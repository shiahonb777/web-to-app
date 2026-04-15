package com.webtoapp.core.extension

import android.content.Context
import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.webtoapp.core.network.NetworkModule
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * 扩展文件管理器
 * 
 * 负责：
 * - 从 URI / 文件导入油猴脚本 (.user.js)
 * - 从 URI / 文件导入 Chrome 扩展 (.crx / .zip)
 * - 解压扩展包到独立目录
 * - 管理扩展文件的生命周期（清理等）
 */
class ExtensionFileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ExtensionFileManager"
        private const val EXTENSIONS_DIR = "extensions"
        private const val REQUIRE_CACHE_DIR = "gm_require"
        private const val RESOURCE_CACHE_DIR = "gm_resource"
        private const val TEMP_DIR = "ext_temp"
        private const val MAX_EXTENSION_SIZE = 50 * 1024 * 1024L // 50MB
        private const val MAX_REQUIRE_SIZE = 5 * 1024 * 1024L // 5MB per require file
    }
    
    private val extensionsDir: File by lazy {
        File(context.filesDir, EXTENSIONS_DIR).apply { mkdirs() }
    }
    
    private val tempDir: File by lazy {
        File(context.cacheDir, TEMP_DIR).apply { mkdirs() }
    }
    
    private val requireCacheDir: File by lazy {
        File(context.filesDir, REQUIRE_CACHE_DIR).apply { mkdirs() }
    }
    
    private val resourceCacheDir: File by lazy {
        File(context.filesDir, RESOURCE_CACHE_DIR).apply { mkdirs() }
    }
    
    private val httpClient get() = NetworkModule.defaultClient
    
    /**
     * 导入结果
     */
    sealed class ImportResult {
        data class UserScript(
            val parseResult: UserScriptParser.ParseResult
        ) : ImportResult()
        
        data class ChromeExtension(
            val parseResult: ChromeExtensionParser.ParseResult,
            val extractedDir: File
        ) : ImportResult()
        
        /**
         * JS ZIP 包导入结果 — 多个 JS/CSS 文件打包的扩展
         */
        data class JsPackage(
            val module: ExtensionModule,
            val fileCount: Int,
            val totalSize: Long
        ) : ImportResult()
        
        data class Error(val message: String) : ImportResult()
    }
    
    // ==================== Userscript Import ====================
    
    /**
     * 从 URI 导入油猴脚本
     */
    suspend fun importUserScript(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return@withContext ImportResult.Error("Cannot read file")
            
            val fileName = getFileName(uri) ?: "script.user.js"
            importUserScriptFromText(content, fileName)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to import userscript from URI", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    /**
     * 从文本内容导入油猴脚本
     */
    fun importUserScriptFromText(content: String, fileName: String = ""): ImportResult {
        return try {
            val parseResult = UserScriptParser.parse(content, fileName)
            ImportResult.UserScript(parseResult)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse userscript", e)
            ImportResult.Error("Parse failed: ${e.message}")
        }
    }
    
    // ==================== Chrome Extension Import ====================
    
    /**
     * 从 URI 导入 Chrome 扩展 (.crx 或 .zip)
     */
    suspend fun importChromeExtension(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            // 复制到临时文件
            val tempFile = File(tempDir, "ext_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val copied = input.copyTo(output)
                    if (copied > MAX_EXTENSION_SIZE) {
                        tempFile.delete()
                        return@withContext ImportResult.Error("Extension too large (max 50MB)")
                    }
                }
            } ?: return@withContext ImportResult.Error("Cannot read file")
            
            val result = importChromeExtensionFromFile(tempFile)
            
            // 清理临时文件
            tempFile.delete()
            
            result
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to import Chrome extension from URI", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    /**
     * 从文件导入 Chrome 扩展
     * 如果 ZIP 中没有 manifest.json 但包含 JS 文件，自动转为 JS 包导入
     */
    suspend fun importChromeExtensionFromFile(file: File): ImportResult = withContext(Dispatchers.IO) {
        try {
            val extensionId = UUID.randomUUID().toString().take(12)
            val extractDir = File(extensionsDir, extensionId)
            extractDir.mkdirs()
            
            val isCrx = ChromeExtensionParser.isCrxFile(file)
            
            if (isCrx) {
                // CRX 文件 - 跳过头部，解压 ZIP 数据
                val zipOffset = ChromeExtensionParser.getCrxZipOffset(file)
                extractCrxToDirectory(file, zipOffset, extractDir)
            } else {
                // 普通 ZIP 文件
                extractZipToDirectory(file, extractDir)
            }
            
            // 检查是否有嵌套目录（部分扩展 ZIP 内有一层目录）
            val actualDir = findManifestDirectory(extractDir)
            if (actualDir == null) {
                // 没有 manifest.json — 尝试作为 JS 包导入
                val jsPackageResult = tryImportAsJsPackage(extractDir)
                if (jsPackageResult != null) {
                    return@withContext jsPackageResult
                }
                extractDir.deleteRecursively()
                return@withContext ImportResult.Error("No manifest.json or JS files found in ZIP")
            }
            
            // Pass the top-level UUID directory name as extensionId
            // This ensures chromeExtId matches the filesystem path for resource loading
            val parseResult = ChromeExtensionParser.parseFromDirectory(actualDir, overrideExtensionId = extensionId)
            
            if (!parseResult.isValid) {
                extractDir.deleteRecursively()
                return@withContext ImportResult.Error(
                    "Invalid extension: ${parseResult.warnings.joinToString()}"
                )
            }
            
            ImportResult.ChromeExtension(parseResult, actualDir)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to import Chrome extension from file", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    /**
     * 从 URI 导入 JS ZIP 扩展包
     * 支持包含多个 JS/CSS 文件的 ZIP 包（无需 manifest.json）
     */
    suspend fun importJsZipPackage(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(tempDir, "jszip_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val copied = input.copyTo(output)
                    if (copied > MAX_EXTENSION_SIZE) {
                        tempFile.delete()
                        return@withContext ImportResult.Error("ZIP too large (max 50MB)")
                    }
                }
            } ?: return@withContext ImportResult.Error("Cannot read file")
            
            val extractDir = File(tempDir, "jszip_extract_${System.currentTimeMillis()}")
            extractDir.mkdirs()
            
            try {
                extractZipToDirectory(tempFile, extractDir)
                val result = tryImportAsJsPackage(extractDir)
                if (result != null) {
                    return@withContext result
                }
                return@withContext ImportResult.Error("No JS files found in ZIP")
            } finally {
                tempFile.delete()
                extractDir.deleteRecursively()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to import JS ZIP package", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    /**
     * 尝试将解压后的目录作为 JS 扩展包导入
     * 扫描所有 JS/CSS 文件，按入口优先级排序后拼接为单个模块
     */
    private fun tryImportAsJsPackage(dir: File): ImportResult? {
        val jsFiles = dir.walkTopDown()
            .filter { it.isFile && (it.extension.equals("js", true) || it.extension.equals("mjs", true)) }
            .toList()
        
        if (jsFiles.isEmpty()) return null
        
        val cssFiles = dir.walkTopDown()
            .filter { it.isFile && it.extension.equals("css", true) }
            .toList()
        
        // 构建多文件映射（相对路径 → 文件内容）
        // 保留原始文件结构，不再拼接成单个巨型字符串
        val codeFilesMap = linkedMapOf<String, String>()
        jsFiles.forEach { file ->
            val relativePath = file.relativeTo(dir).path
            codeFilesMap[relativePath] = file.readText()
        }
        
        // CSS 文件同样保留独立结构，拼接为一个 cssCode（CSS 不需要执行顺序）
        val combinedCss = if (cssFiles.isNotEmpty()) {
            buildString {
                cssFiles.forEachIndexed { index, file ->
                    if (index > 0) append("\n\n")
                    append("/* ========== ${file.relativeTo(dir).path} ========== */\n")
                    append(file.readText())
                }
            }
        } else ""
        
        // Auto-detect a name from the directory or first file
        val suggestedName = dir.name.takeIf { it.isNotBlank() && !it.startsWith("jszip_") }
            ?: jsFiles.firstOrNull()?.nameWithoutExtension
            ?: "JS Extension"
        
        val totalSize = (jsFiles + cssFiles).sumOf { it.length() }
        
        val module = ExtensionModule(
            id = UUID.randomUUID().toString(),
            name = suggestedName,
            description = "Imported from ZIP (${jsFiles.size} JS + ${cssFiles.size} CSS files)",
            code = "",  // 不再使用单一 code 字段
            cssCode = combinedCss,
            codeFiles = codeFilesMap,  // 多文件独立存储
            category = ModuleCategory.OTHER,
            sourceType = ModuleSourceType.CUSTOM,
            enabled = true
        )
        
        return ImportResult.JsPackage(
            module = module,
            fileCount = jsFiles.size + cssFiles.size,
            totalSize = totalSize
        )
    }
    
    // ==================== @require / @resource Pre-loading ====================
    
    /**
     * Download and cache all @require URLs for a userscript.
     * Should be called after the module is installed.
     */
    suspend fun preloadRequires(requireUrls: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>() // url -> cached JS content
        requireUrls.forEach { url ->
            try {
                val hash = sha256(url)
                val cacheFile = File(requireCacheDir, hash)
                
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    // Already cached
                    result[url] = cacheFile.readText()
                    return@forEach
                }
                
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.length <= MAX_REQUIRE_SIZE) {
                        cacheFile.writeText(body)
                        result[url] = body
                        AppLogger.d(TAG, "Cached @require: $url (${body.length} bytes)")
                    } else {
                        AppLogger.w(TAG, "@require too large, skipping: $url")
                    }
                } else {
                    AppLogger.w(TAG, "Failed to download @require: $url (${response.code})")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error downloading @require: $url", e)
            }
        }
        result
    }
    
    /**
     * Download and cache all @resource entries for a userscript.
     * @return map of resource name -> cached content (text or data URI)
     */
    suspend fun preloadResources(resources: Map<String, String>): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>() // name -> content
        resources.forEach { (name, url) ->
            try {
                val hash = sha256("$name:$url")
                val cacheFile = File(resourceCacheDir, hash)
                
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    result[name] = cacheFile.readText()
                    return@forEach
                }
                
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type", "text/plain") ?: "text/plain"
                    val bytes = response.body?.bytes() ?: byteArrayOf()
                    
                    if (contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml") || contentType.contains("javascript")) {
                        // Text content — store as-is
                        val text = String(bytes)
                        cacheFile.writeText(text)
                        result[name] = text
                    } else {
                        // Binary content — store as data URI
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val dataUri = "data:$contentType;base64,$base64"
                        cacheFile.writeText(dataUri)
                        result[name] = dataUri
                    }
                    AppLogger.d(TAG, "Cached @resource '$name': $url")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error downloading @resource '$name': $url", e)
            }
        }
        result
    }
    
    /**
     * Get cached @require JS content for a URL.
     * Returns null if not cached.
     */
    fun getCachedRequire(url: String): String? {
        val hash = sha256(url)
        val cacheFile = File(requireCacheDir, hash)
        return if (cacheFile.exists()) cacheFile.readText() else null
    }
    
    /**
     * Get cached @resource content for a name+url.
     * Returns null if not cached.
     */
    fun getCachedResource(name: String, url: String): String? {
        val hash = sha256("$name:$url")
        val cacheFile = File(resourceCacheDir, hash)
        return if (cacheFile.exists()) cacheFile.readText() else null
    }
    
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }
    
    // ==================== File Operations ====================
    
    /**
     * 清理扩展目录
     */
    fun cleanupExtensionDir(dirName: String) {
        try {
            val dir = File(extensionsDir, dirName)
            if (dir.exists()) {
                dir.deleteRecursively()
                AppLogger.d(TAG, "Cleaned up extension dir: $dirName")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cleanup extension dir: $dirName", e)
        }
    }
    
    /**
     * 清理所有临时文件
     */
    fun cleanupTemp() {
        try {
            tempDir.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cleanup temp dir", e)
        }
    }
    
    /**
     * 获取扩展目录总大小
     */
    fun getExtensionsDirSize(): Long {
        return extensionsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    // ==================== Private Helpers ====================
    
    /**
     * 解压 ZIP 文件到目录
     */
    private fun extractZipToDirectory(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            extractZipStream(zis, targetDir)
        }
    }
    
    /**
     * 从 CRX 文件提取 ZIP 数据并解压
     */
    private fun extractCrxToDirectory(crxFile: File, zipOffset: Long, targetDir: File) {
        RandomAccessFile(crxFile, "r").use { raf ->
            raf.seek(zipOffset)
            
            // 读取剩余数据到临时 ZIP 文件
            val tempZip = File(tempDir, "crx_zip_${System.currentTimeMillis()}.zip")
            try {
                FileOutputStream(tempZip).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (raf.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
                extractZipToDirectory(tempZip, targetDir)
            } finally {
                tempZip.delete()
            }
        }
    }
    
    /**
     * 从 ZipInputStream 解压到目录
     */
    private fun extractZipStream(zis: ZipInputStream, targetDir: File) {
        var entry = zis.nextEntry
        while (entry != null) {
            val entryName = entry.name
            
            // 安全检查：防止 Zip Slip 攻击
            val destFile = File(targetDir, entryName).canonicalFile
            if (!destFile.path.startsWith(targetDir.canonicalPath)) {
                AppLogger.w(TAG, "Skipping potentially unsafe zip entry: $entryName")
                zis.closeEntry()
                entry = zis.nextEntry
                continue
            }
            
            // 跳过 macOS 资源文件
            if (entryName.startsWith("__MACOSX/") || entryName.endsWith(".DS_Store")) {
                zis.closeEntry()
                entry = zis.nextEntry
                continue
            }
            
            if (entry.isDirectory) {
                destFile.mkdirs()
            } else {
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { fos ->
                    zis.copyTo(fos)
                }
            }
            
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
    
    /**
     * 在目录中查找 manifest.json（处理可能的嵌套目录）
     */
    private fun findManifestDirectory(dir: File): File? {
        // 直接在根目录查找
        if (dir.resolve("manifest.json").exists()) return dir
        
        // 检查一级子目录
        dir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
            if (subDir.resolve("manifest.json").exists()) return subDir
        }
        
        return null
    }
    
    /**
     * 从 URI 获取文件名
     */
    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
