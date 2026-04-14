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
 * extension manager.
 *
 * .
 * - from URI / (.user.js)
 * - from URI / Chrome extension (.crx /.zip)
 * - extension to.
 * - manageextension.
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
     */
    sealed class ImportResult {
        data class UserScript(
            val parseResult: UserScriptParser.ParseResult
        ) : ImportResult()
        
        data class ChromeExtension(
            val parseResult: ChromeExtensionParser.ParseResult,
            val extractedDir: File
        ) : ImportResult()
        
        data class Error(val message: String) : ImportResult()
    }
    
    // ==================== Userscript Import ====================
    
    /**
     * from URI.
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
     * from.
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
     * from URI Chrome extension.
     */
    suspend fun importChromeExtension(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            // to when.
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
            
            // when.
            tempFile.delete()
            
            result
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to import Chrome extension from URI", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    /**
     * from Chrome extension.
     */
    suspend fun importChromeExtensionFromFile(file: File): ImportResult = withContext(Dispatchers.IO) {
        try {
            val extensionId = UUID.randomUUID().toString().take(12)
            val extractDir = File(extensionsDir, extensionId)
            extractDir.mkdirs()
            
            val isCrx = ChromeExtensionParser.isCrxFile(file)
            
            if (isCrx) {
                // CRX - ZIP.
                val zipOffset = ChromeExtensionParser.getCrxZipOffset(file)
                extractCrxToDirectory(file, zipOffset, extractDir)
            } else {
                // ZIP.
                extractZipToDirectory(file, extractDir)
            }
            
            // Check is.
            val actualDir = findManifestDirectory(extractDir)
            if (actualDir == null) {
                extractDir.deleteRecursively()
                return@withContext ImportResult.Error("No manifest.json found in extension")
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
                        // Text content store as-is
                        val text = String(bytes)
                        cacheFile.writeText(text)
                        result[name] = text
                    } else {
                        // Binary content store as data URI
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
     * extension.
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
     * when.
     */
    fun cleanupTemp() {
        try {
            tempDir.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cleanup temp dir", e)
        }
    }
    
    /**
     * Getextension large small.
     */
    fun getExtensionsDirSize(): Long {
        return extensionsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    // ==================== Private Helpers ====================
    
    /**
     * ZIP to.
     */
    private fun extractZipToDirectory(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            extractZipStream(zis, targetDir)
        }
    }
    
    /**
     * from CRX Extract ZIP and.
     */
    private fun extractCrxToDirectory(crxFile: File, zipOffset: Long, targetDir: File) {
        RandomAccessFile(crxFile, "r").use { raf ->
            raf.seek(zipOffset)
            
            // to when ZIP.
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
     * from ZipInputStream to.
     */
    private fun extractZipStream(zis: ZipInputStream, targetDir: File) {
        var entry = zis.nextEntry
        while (entry != null) {
            val entryName = entry.name
            
            // Check Zip Slip.
            val destFile = File(targetDir, entryName).canonicalFile
            if (!destFile.path.startsWith(targetDir.canonicalPath)) {
                AppLogger.w(TAG, "Skipping potentially unsafe zip entry: $entryName")
                zis.closeEntry()
                entry = zis.nextEntry
                continue
            }
            
            // macOS.
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
     * in in manifest.json.
     */
    private fun findManifestDirectory(dir: File): File? {
        // in.
        if (dir.resolve("manifest.json").exists()) return dir
        
        // Check level.
        dir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
            if (subDir.resolve("manifest.json").exists()) return subDir
        }
        
        return null
    }
    
    /**
     * from URI Get.
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
