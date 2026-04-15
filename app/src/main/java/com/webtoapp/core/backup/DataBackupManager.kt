package com.webtoapp.core.backup

import android.content.Context
import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.util.threadLocalCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

/**
 * Note: brief English comment.
 * Note: brief English comment.
 */
class DataBackupManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DataBackupManager"
        private const val BACKUP_VERSION = 2  // Version升级：支持更多资源类型
        private const val APPS_JSON = "apps.json"
        private const val RESOURCES_DIR = "resources/"
        private const val ICONS_DIR = "resources/icons/"
        private const val SPLASH_DIR = "resources/splash/"
        private const val BGM_DIR = "resources/bgm/"
        private const val BGM_LRC_DIR = "resources/bgm_lrc/"      // BGM 歌词文件
        private const val BGM_COVER_DIR = "resources/bgm_cover/"  // BGM 封面图片
        private const val HTML_DIR = "resources/html/"
        private const val MEDIA_DIR = "resources/media/"
        private const val STATUSBAR_DIR = "resources/statusbar/" // 状态栌背景图
        private const val EXTENSION_DIR = "extensions/"
        private const val EXTENSION_MODULES_FILE = "${EXTENSION_DIR}modules.json"
        private const val EXTENSION_BUILTIN_STATES_FILE = "${EXTENSION_DIR}builtin_states.json"
        
        // Note: brief English comment.
        private const val BUFFER_SIZE = 8192
        
        // Note: brief English comment.
        private val backupDateFormat = threadLocalCompat {
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        }
        
        // Note: brief English comment.
        private val gson: Gson by lazy {
            GsonBuilder()
                .setPrettyPrinting()
                .create()
        }
    }
    
    /**
     * Note: brief English comment.
     */
    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val exportTime: Long = System.currentTimeMillis(),
        val appCount: Int = 0,
        val apps: List<WebApp> = emptyList()
    )
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    suspend fun exportAllData(
        repository: WebAppRepository,
        outputUri: Uri,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            onProgress(0, 100, "正在读取应用数据...")
            coroutineContext.ensureActive() // Support取消
            
            // Note: brief English comment.
            val apps = repository.allWebApps.first()
            if (apps.isEmpty()) {
                return@withContext Result.failure(Exception("没有可导出的应用数据"))
            }
            
            AppLogger.i(TAG, "准备导出 ${apps.size} 个应用")
            
            // Note: brief English comment.
            val resourceFiles = mutableMapOf<String, String>() // zipPath -> localPath
            
            apps.forEachIndexed { index, app ->
                coroutineContext.ensureActive()
                onProgress(10 + (index * 30 / apps.size), 100, "正在收集资源: ${app.name}")
                collectAppResources(app, resourceFiles)
            }
            
            AppLogger.i(TAG, "收集到 ${resourceFiles.size} 个资源文件")
            
            // Note: brief English comment.
            val appsWithRelativePaths = apps.map { app ->
                updateAppPathsToRelative(app, resourceFiles)
            }
            
            val backupData = BackupData(
                version = BACKUP_VERSION,
                exportTime = System.currentTimeMillis(),
                appCount = apps.size,
                apps = appsWithRelativePaths
            )
            val extensionFiles = collectExtensionBackupFiles()
            
            onProgress(50, 100, "正在创建备份文件...")
            coroutineContext.ensureActive()
            
            // Note: brief English comment.
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream, BUFFER_SIZE)).use { zipOut ->
                    // Note: brief English comment.
                    val jsonBytes = gson.toJson(backupData).toByteArray(Charsets.UTF_8)
                    zipOut.putNextEntry(ZipEntry(APPS_JSON))
                    zipOut.write(jsonBytes)
                    zipOut.closeEntry()
                    
                    // Note: brief English comment.
                    var processedFiles = 0
                    resourceFiles.forEach { (zipPath, localPath) ->
                        coroutineContext.ensureActive()
                        processedFiles++
                        onProgress(
                            50 + (processedFiles * 45 / resourceFiles.size),
                            100,
                            "正在打包资源文件..."
                        )
                        
                        try {
                            val file = File(localPath)
                            if (file.exists() && file.canRead()) {
                                zipOut.putNextEntry(ZipEntry(zipPath))
                                file.inputStream().buffered(BUFFER_SIZE).use { input ->
                                    input.copyTo(zipOut, BUFFER_SIZE)
                                }
                                zipOut.closeEntry()
                            }
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "无法打包资源文件: $localPath", e)
                        }
                    }
                    
                    // Note: brief English comment.
                    extensionFiles.forEach { (zipPath, file) ->
                        coroutineContext.ensureActive()
                        try {
                            if (file.exists() && file.canRead()) {
                                zipOut.putNextEntry(ZipEntry(zipPath))
                                file.inputStream().buffered(BUFFER_SIZE).use { input ->
                                    input.copyTo(zipOut, BUFFER_SIZE)
                                }
                                zipOut.closeEntry()
                            }
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "无法打包扩展配置: ${file.absolutePath}", e)
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("无法创建输出文件"))
            
            onProgress(100, 100, "导出完成")
            
            Result.success(ExportResult(
                appCount = apps.size,
                resourceCount = resourceFiles.size + extensionFiles.size
            ))
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Export failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    suspend fun importAllData(
        repository: WebAppRepository,
        inputUri: Uri,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        val extractedFiles = mutableListOf<File>() // 用于失败时清理
        
        try {
            onProgress(0, 100, "正在读取备份文件...")
            coroutineContext.ensureActive()
            
            var backupData: BackupData? = null
            val extractedResources = mutableMapOf<String, String>() // zipPath -> extractedLocalPath
            var modulesJsonBytes: ByteArray? = null
            var builtInStatesJsonBytes: ByteArray? = null
            
            // Note: brief English comment.
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream, BUFFER_SIZE)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    var totalEntries = 0
                    
                    while (entry != null) {
                        coroutineContext.ensureActive()
                        totalEntries++
                        onProgress(10 + (totalEntries % 40), 100, "正在解压: ${entry.name}")
                        
                        when {
                            entry.name == APPS_JSON -> {
                                // Note: brief English comment.
                                val jsonBytes = zipIn.readBytes()
                                val jsonStr = String(jsonBytes, Charsets.UTF_8)
                                backupData = parseBackupData(jsonStr)
                                AppLogger.i(TAG, "读取到 ${backupData?.appCount} 个应用")
                            }
                            entry.name == EXTENSION_MODULES_FILE -> {
                                modulesJsonBytes = zipIn.readBytes()
                            }
                            entry.name == EXTENSION_BUILTIN_STATES_FILE -> {
                                builtInStatesJsonBytes = zipIn.readBytes()
                            }
                            entry.name.startsWith(RESOURCES_DIR) && !entry.isDirectory -> {
                                // Note: brief English comment.
                                val extractedPath = extractResourceFile(entry.name, zipIn)
                                if (extractedPath != null) {
                                    extractedResources[entry.name] = extractedPath
                                    extractedFiles.add(File(extractedPath))
                                }
                            }
                        }
                        
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(Exception("无法读取备份文件"))
            
            val data = backupData
            if (data == null) {
                cleanupExtractedFiles(extractedFiles)
                return@withContext Result.failure(Exception("备份文件格式无效"))
            }
            
            onProgress(60, 100, "正在导入应用数据...")
            coroutineContext.ensureActive()
            
            // Note: brief English comment.
            var importedCount = 0
            var skippedCount = 0
            
            data.apps.forEachIndexed { index, app ->
                coroutineContext.ensureActive()
                onProgress(
                    60 + (index * 35 / data.apps.size),
                    100,
                    "正在导入: ${app.name}"
                )
                
                try {
                    // Note: brief English comment.
                    val appWithLocalPaths = updateAppPathsToLocal(app, extractedResources)
                    
                    // Note: brief English comment.
                    val newApp = appWithLocalPaths.copy(
                        id = 0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    repository.createWebApp(newApp)
                    importedCount++
                } catch (e: Exception) {
                    AppLogger.w(TAG, "导入应用失败: ${app.name}", e)
                    skippedCount++
                }
            }
            
            // Note: brief English comment.
            restoreExtensionFiles(modulesJsonBytes, builtInStatesJsonBytes)
            
            onProgress(100, 100, "导入完成")
            
            Result.success(ImportResult(
                totalCount = data.appCount,
                importedCount = importedCount,
                skippedCount = skippedCount
            ))
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入失败", e)
            cleanupExtractedFiles(extractedFiles)
            Result.failure(e)
        }
    }

    /**
     * Note: brief English comment.
     */
    private fun parseBackupData(jsonStr: String): BackupData {
        val root = JsonParser.parseString(jsonStr).asJsonObject
        val version = root.get("version")?.asInt ?: 1
        val exportTime = root.get("exportTime")?.asLong ?: System.currentTimeMillis()
        val appCount = root.get("appCount")?.asInt ?: 0
        val appsArray = root.getAsJsonArray("apps")

        val apps = appsArray?.mapNotNull { element ->
            runCatching {
                parseWebAppWithDefaults(element.asJsonObject)
            }.getOrElse {
                AppLogger.w(TAG, "解析应用配置失败，已跳过一项", it)
                null
            }
        } ?: emptyList()

        return BackupData(
            version = version,
            exportTime = exportTime,
            appCount = if (appCount > 0) appCount else apps.size,
            apps = apps
        )
    }

    private fun parseWebAppWithDefaults(appObject: JsonObject): WebApp {
        val app = gson.fromJson(appObject, WebApp::class.java)
        val mergedWebViewConfig = parseWebViewConfigWithDefaults(appObject.get("webViewConfig"))
        return app.copy(webViewConfig = mergedWebViewConfig)
    }

    private fun parseWebViewConfigWithDefaults(rawValue: JsonElement?): WebViewConfig {
        val defaults = gson.toJsonTree(WebViewConfig())
        val merged = mergeMissingDefaults(defaults, rawValue)
        return runCatching {
            gson.fromJson(merged, WebViewConfig::class.java)
        }.getOrDefault(WebViewConfig())
    }

    private fun mergeMissingDefaults(defaults: JsonElement, current: JsonElement?): JsonElement {
        if (!defaults.isJsonObject) {
            return current?.deepCopy() ?: defaults.deepCopy()
        }

        val merged = JsonObject()
        val currentObj = if (current != null && current.isJsonObject) current.asJsonObject else JsonObject()
        currentObj.entrySet().forEach { (key, value) ->
            merged.add(key, value)
        }

        defaults.asJsonObject.entrySet().forEach { (key, defaultValue) ->
            val currentValue = if (merged.has(key)) merged.get(key) else null
            if (currentValue == null || currentValue.isJsonNull) {
                merged.add(key, defaultValue.deepCopy())
            } else {
                merged.add(key, mergeMissingDefaults(defaultValue, currentValue))
            }
        }
        return merged
    }

    private fun restoreExtensionFiles(modulesJsonBytes: ByteArray?, builtInStatesJsonBytes: ByteArray?) {
        if (modulesJsonBytes == null && builtInStatesJsonBytes == null) return

        runCatching {
            val extensionDir = File(context.filesDir, "extension_modules").apply { mkdirs() }
            modulesJsonBytes?.let { File(extensionDir, "modules.json").writeBytes(it) }
            builtInStatesJsonBytes?.let { File(extensionDir, "builtin_states.json").writeBytes(it) }

            // Force reload extension modules from restored files
            com.webtoapp.core.extension.ExtensionManager.release()
            com.webtoapp.core.extension.ExtensionManager.getInstance(context)
            AppLogger.i(TAG, "扩展模块配置已恢复")
        }.onFailure { e ->
            AppLogger.w(TAG, "恢复扩展模块配置失败", e)
        }
    }
    
    /**
     * Note: brief English comment.
     */
    private fun cleanupExtractedFiles(files: List<File>) {
        files.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "清理文件失败: ${file.absolutePath}", e)
            }
        }
    }
    
    /**
     * Note: brief English comment.
     */
    fun cleanupBackupTempFiles() {
        val backupDirs = listOf(
            "backup_icons", "backup_splash", "backup_bgm", 
            "backup_bgm_lrc", "backup_bgm_cover",
            "backup_html", "backup_media", "backup_statusbar", "backup_other"
        )
        
        backupDirs.forEach { dirName ->
            try {
                val dir = File(context.filesDir, dirName)
                if (dir.exists() && dir.isDirectory) {
                    dir.deleteRecursively()
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "清理备份目录失败: $dirName", e)
            }
        }
    }
    
    /**
     * Note: brief English comment.
     */
    fun getBackupTempSize(): Long {
        val backupDirs = listOf(
            "backup_icons", "backup_splash", "backup_bgm",
            "backup_bgm_lrc", "backup_bgm_cover",
            "backup_html", "backup_media", "backup_statusbar", "backup_other"
        )
        
        return backupDirs.sumOf { dirName ->
            val dir = File(context.filesDir, dirName)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else 0L
        }
    }
    
    /**
     * Note: brief English comment.
     */
    private fun collectAppResources(app: WebApp, resources: MutableMap<String, String>) {
        val appId = app.id.toString()
        
        // Icon
        app.iconPath?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${ICONS_DIR}${appId}_icon.$ext"] = path
            }
        }
        
        // Note: brief English comment.
        app.splashConfig?.mediaPath?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${SPLASH_DIR}${appId}_splash.$ext"] = path
            }
        }
        
        // Note: brief English comment.
        app.bgmConfig?.playlist?.forEachIndexed { index, bgmItem ->
            // Note: brief English comment.
            if (!bgmItem.isAsset && File(bgmItem.path).exists()) {
                val ext = bgmItem.path.substringAfterLast('.', "mp3")
                resources["${BGM_DIR}${appId}_bgm_$index.$ext"] = bgmItem.path
            }
            // Note: brief English comment.
            bgmItem.lrcPath?.let { lrcPath ->
                if (File(lrcPath).exists()) {
                    resources["${BGM_LRC_DIR}${appId}_bgm_$index.lrc"] = lrcPath
                }
            }
            // Note: brief English comment.
            bgmItem.coverPath?.let { coverPath ->
                if (File(coverPath).exists()) {
                    val ext = coverPath.substringAfterLast('.', "jpg")
                    resources["${BGM_COVER_DIR}${appId}_bgm_cover_$index.$ext"] = coverPath
                }
            }
        }
        
        // Note: brief English comment.
        app.webViewConfig.statusBarBackgroundImage?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${STATUSBAR_DIR}${appId}_statusbar.$ext"] = path
            }
        }
        
        // Note: brief English comment.
        app.htmlConfig?.files?.forEach { htmlFile ->
            if (File(htmlFile.path).exists()) {
                resources["${HTML_DIR}${appId}/${htmlFile.name}"] = htmlFile.path
            }
        }
        
        // Note: brief English comment.
        if (app.appType == com.webtoapp.data.model.AppType.IMAGE || 
            app.appType == com.webtoapp.data.model.AppType.VIDEO) {
            val mediaPath = app.url
            if (mediaPath.isNotBlank() && File(mediaPath).exists()) {
                val ext = mediaPath.substringAfterLast('.', "mp4")
                resources["${MEDIA_DIR}${appId}_media.$ext"] = mediaPath
            }
        }
    }

    /**
     * Note: brief English comment.
     */
    private fun collectExtensionBackupFiles(): Map<String, File> {
        val extensionDir = File(context.filesDir, "extension_modules")
        if (!extensionDir.exists() || !extensionDir.isDirectory) return emptyMap()

        val files = mutableMapOf<String, File>()
        val modulesFile = File(extensionDir, "modules.json")
        val builtInStatesFile = File(extensionDir, "builtin_states.json")

        if (modulesFile.exists() && modulesFile.isFile) {
            files[EXTENSION_MODULES_FILE] = modulesFile
        }
        if (builtInStatesFile.exists() && builtInStatesFile.isFile) {
            files[EXTENSION_BUILTIN_STATES_FILE] = builtInStatesFile
        }
        return files
    }
    
    /**
     * Note: brief English comment.
     */
    private fun updateAppPathsToRelative(
        app: WebApp,
        resources: Map<String, String>
    ): WebApp {
        val appId = app.id.toString()
        
        // Note: brief English comment.
        fun findZipPath(localPath: String?): String? {
            if (localPath == null) return null
            return resources.entries.find { it.value == localPath }?.key
        }
        
        return app.copy(
            iconPath = findZipPath(app.iconPath),
            splashConfig = app.splashConfig?.copy(
                mediaPath = findZipPath(app.splashConfig?.mediaPath)
            ),
            bgmConfig = app.bgmConfig?.copy(
                playlist = app.bgmConfig?.playlist?.mapIndexed { index, item ->
                    if (!item.isAsset) {
                        item.copy(
                            path = findZipPath(item.path) ?: item.path,
                            lrcPath = findZipPath(item.lrcPath),
                            coverPath = findZipPath(item.coverPath)
                        )
                    } else item
                } ?: emptyList()
            ),
            htmlConfig = app.htmlConfig?.copy(
                files = app.htmlConfig?.files?.map { file ->
                    file.copy(path = "${HTML_DIR}${appId}/${file.name}")
                } ?: emptyList()
            ),
            webViewConfig = app.webViewConfig.copy(
                statusBarBackgroundImage = findZipPath(app.webViewConfig.statusBarBackgroundImage)
            ),
            url = if (app.appType == com.webtoapp.data.model.AppType.IMAGE ||
                      app.appType == com.webtoapp.data.model.AppType.VIDEO) {
                findZipPath(app.url) ?: app.url
            } else app.url
        )
    }
    
    /**
     * Note: brief English comment.
     */
    private fun updateAppPathsToLocal(
        app: WebApp,
        extractedResources: Map<String, String>
    ): WebApp {
        return app.copy(
            iconPath = extractedResources[app.iconPath] ?: app.iconPath,
            splashConfig = app.splashConfig?.copy(
                mediaPath = extractedResources[app.splashConfig?.mediaPath] ?: app.splashConfig?.mediaPath
            ),
            bgmConfig = app.bgmConfig?.copy(
                playlist = app.bgmConfig?.playlist?.map { item ->
                    if (!item.isAsset) {
                        item.copy(
                            path = extractedResources[item.path] ?: item.path,
                            lrcPath = item.lrcPath?.let { extractedResources[it] ?: it },
                            coverPath = item.coverPath?.let { extractedResources[it] ?: it }
                        )
                    } else item
                } ?: emptyList()
            ),
            htmlConfig = app.htmlConfig?.copy(
                files = app.htmlConfig?.files?.map { file ->
                    file.copy(path = extractedResources[file.path] ?: file.path)
                } ?: emptyList()
            ),
            webViewConfig = app.webViewConfig.copy(
                statusBarBackgroundImage = app.webViewConfig.statusBarBackgroundImage?.let {
                    extractedResources[it] ?: it
                }
            ),
            url = if (app.appType == com.webtoapp.data.model.AppType.IMAGE ||
                      app.appType == com.webtoapp.data.model.AppType.VIDEO) {
                extractedResources[app.url] ?: app.url
            } else app.url
        )
    }
    
    /**
     * Note: brief English comment.
     */
    private fun extractResourceFile(zipPath: String, zipIn: ZipInputStream): String? {
        return try {
            val targetDir = when {
                zipPath.startsWith(ICONS_DIR) -> File(context.filesDir, "backup_icons")
                zipPath.startsWith(SPLASH_DIR) -> File(context.filesDir, "backup_splash")
                zipPath.startsWith(BGM_LRC_DIR) -> File(context.filesDir, "backup_bgm_lrc")
                zipPath.startsWith(BGM_COVER_DIR) -> File(context.filesDir, "backup_bgm_cover")
                zipPath.startsWith(BGM_DIR) -> File(context.filesDir, "backup_bgm")
                zipPath.startsWith(HTML_DIR) -> File(context.filesDir, "backup_html")
                zipPath.startsWith(MEDIA_DIR) -> File(context.filesDir, "backup_media")
                zipPath.startsWith(STATUSBAR_DIR) -> File(context.filesDir, "backup_statusbar")
                else -> File(context.filesDir, "backup_other")
            }
            
            targetDir.mkdirs()
            
            val fileName = zipPath.substringAfterLast('/')
            val targetFile = File(targetDir, fileName)
            
            FileOutputStream(targetFile).use { output ->
                zipIn.copyTo(output)
            }
            
            targetFile.absolutePath
        } catch (e: Exception) {
            AppLogger.w(TAG, "解压资源文件失败: $zipPath", e)
            null
        }
    }
    
    /**
     * Note: brief English comment.
     */
    fun generateBackupFileName(): String {
        return "WebToApp_Backup_${backupDateFormat.get()!!.format(Date())}.zip"
    }
}

/**
 * Note: brief English comment.
 */
data class ExportResult(
    val appCount: Int,
    val resourceCount: Int
)

/**
 * Note: brief English comment.
 */
data class ImportResult(
    val totalCount: Int,
    val importedCount: Int,
    val skippedCount: Int
)
