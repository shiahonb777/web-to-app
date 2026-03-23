package com.webtoapp.core.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.repository.WebAppRepository
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
 * 数据备份管理器
 * 支持一键导出和导入所有应用数据
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
        
        // 缓冲区大小
        private const val BUFFER_SIZE = 8192
        
        // 单例 Gson
        private val gson: Gson by lazy {
            GsonBuilder()
                .setPrettyPrinting()
                .create()
        }
    }
    
    /**
     * 备份数据结构
     */
    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val exportTime: Long = System.currentTimeMillis(),
        val appCount: Int = 0,
        val apps: List<WebApp> = emptyList()
    )
    
    /**
     * 导出所有数据到 ZIP 文件
     * @param repository 数据仓库
     * @param outputUri 输出文件 URI
     * @param onProgress 进度回调 (current, total, message)
     * @return 导出结果
     */
    suspend fun exportAllData(
        repository: WebAppRepository,
        outputUri: Uri,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            onProgress(0, 100, "正在读取应用数据...")
            coroutineContext.ensureActive() // Support取消
            
            // Get所有应用
            val apps = repository.allWebApps.first()
            if (apps.isEmpty()) {
                return@withContext Result.failure(Exception("没有可导出的应用数据"))
            }
            
            Log.d(TAG, "准备导出 ${apps.size} 个应用")
            
            // 收集所有资源文件
            val resourceFiles = mutableMapOf<String, String>() // zipPath -> localPath
            
            apps.forEachIndexed { index, app ->
                coroutineContext.ensureActive()
                onProgress(10 + (index * 30 / apps.size), 100, "正在收集资源: ${app.name}")
                collectAppResources(app, resourceFiles)
            }
            
            Log.d(TAG, "收集到 ${resourceFiles.size} 个资源文件")
            
            // Create备份数据（更新资源路径为相对路径）
            val appsWithRelativePaths = apps.map { app ->
                updateAppPathsToRelative(app, resourceFiles)
            }
            
            val backupData = BackupData(
                version = BACKUP_VERSION,
                exportTime = System.currentTimeMillis(),
                appCount = apps.size,
                apps = appsWithRelativePaths
            )
            
            onProgress(50, 100, "正在创建备份文件...")
            coroutineContext.ensureActive()
            
            // 写入 ZIP 文件
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream, BUFFER_SIZE)).use { zipOut ->
                    // 写入 apps.json
                    val jsonBytes = gson.toJson(backupData).toByteArray(Charsets.UTF_8)
                    zipOut.putNextEntry(ZipEntry(APPS_JSON))
                    zipOut.write(jsonBytes)
                    zipOut.closeEntry()
                    
                    // 写入资源文件
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
                            Log.w(TAG, "无法打包资源文件: $localPath", e)
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("无法创建输出文件"))
            
            onProgress(100, 100, "导出完成")
            
            Result.success(ExportResult(
                appCount = apps.size,
                resourceCount = resourceFiles.size
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从 ZIP 文件导入所有数据
     * @param repository 数据仓库
     * @param inputUri 输入文件 URI
     * @param onProgress 进度回调
     * @return 导入结果
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
            
            // 读取 ZIP 文件
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
                                // 读取应用数据
                                val jsonBytes = zipIn.readBytes()
                                val jsonStr = String(jsonBytes, Charsets.UTF_8)
                                backupData = gson.fromJson(jsonStr, BackupData::class.java)
                                Log.d(TAG, "读取到 ${backupData?.appCount} 个应用")
                            }
                            entry.name.startsWith(RESOURCES_DIR) && !entry.isDirectory -> {
                                // Decompression资源文件
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
            
            if (backupData == null) {
                cleanupExtractedFiles(extractedFiles)
                return@withContext Result.failure(Exception("备份文件格式无效"))
            }
            
            onProgress(60, 100, "正在导入应用数据...")
            coroutineContext.ensureActive()
            
            // Import应用
            var importedCount = 0
            var skippedCount = 0
            
            backupData!!.apps.forEachIndexed { index, app ->
                coroutineContext.ensureActive()
                onProgress(
                    60 + (index * 35 / backupData!!.apps.size),
                    100,
                    "正在导入: ${app.name}"
                )
                
                try {
                    // Update资源路径为本地路径
                    val appWithLocalPaths = updateAppPathsToLocal(app, extractedResources)
                    
                    // Create新应用（重置 ID 以避免冲突）
                    val newApp = appWithLocalPaths.copy(
                        id = 0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    repository.createWebApp(newApp)
                    importedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "导入应用失败: ${app.name}", e)
                    skippedCount++
                }
            }
            
            onProgress(100, 100, "导入完成")
            
            Result.success(ImportResult(
                totalCount = backupData!!.appCount,
                importedCount = importedCount,
                skippedCount = skippedCount
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "导入失败", e)
            cleanupExtractedFiles(extractedFiles)
            Result.failure(e)
        }
    }
    
    /**
     * 清理已解压的文件（导入失败时调用）
     */
    private fun cleanupExtractedFiles(files: List<File>) {
        files.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "清理文件失败: ${file.absolutePath}", e)
            }
        }
    }
    
    /**
     * 清理所有备份临时文件
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
                Log.w(TAG, "清理备份目录失败: $dirName", e)
            }
        }
    }
    
    /**
     * 获取备份临时文件大小
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
     * 收集应用的所有资源文件
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
        
        // Start画面
        app.splashConfig?.mediaPath?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${SPLASH_DIR}${appId}_splash.$ext"] = path
            }
        }
        
        // BGM 文件
        app.bgmConfig?.playlist?.forEachIndexed { index, bgmItem ->
            // BGM 音频文件
            if (!bgmItem.isAsset && File(bgmItem.path).exists()) {
                val ext = bgmItem.path.substringAfterLast('.', "mp3")
                resources["${BGM_DIR}${appId}_bgm_$index.$ext"] = bgmItem.path
            }
            // BGM 歌词文件
            bgmItem.lrcPath?.let { lrcPath ->
                if (File(lrcPath).exists()) {
                    resources["${BGM_LRC_DIR}${appId}_bgm_$index.lrc"] = lrcPath
                }
            }
            // BGM 封面图片
            bgmItem.coverPath?.let { coverPath ->
                if (File(coverPath).exists()) {
                    val ext = coverPath.substringAfterLast('.', "jpg")
                    resources["${BGM_COVER_DIR}${appId}_bgm_cover_$index.$ext"] = coverPath
                }
            }
        }
        
        // 状态栌背景图片
        app.webViewConfig.statusBarBackgroundImage?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${STATUSBAR_DIR}${appId}_statusbar.$ext"] = path
            }
        }
        
        // HTML 文件
        app.htmlConfig?.files?.forEach { htmlFile ->
            if (File(htmlFile.path).exists()) {
                resources["${HTML_DIR}${appId}/${htmlFile.name}"] = htmlFile.path
            }
        }
        
        // Media应用内容
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
     * Update app路径为相对路径（用于导出）
     */
    private fun updateAppPathsToRelative(
        app: WebApp,
        resources: Map<String, String>
    ): WebApp {
        val appId = app.id.toString()
        
        // Find对应的 ZIP 路径
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
     * Update app路径为本地路径（用于导入）
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
     * 解压资源文件到本地
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
            Log.w(TAG, "解压资源文件失败: $zipPath", e)
            null
        }
    }
    
    /**
     * 生成备份文件名
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "WebToApp_Backup_${dateFormat.format(Date())}.zip"
    }
}

/**
 * 导出结果
 */
data class ExportResult(
    val appCount: Int,
    val resourceCount: Int
)

/**
 * 导入结果
 */
data class ImportResult(
    val totalCount: Int,
    val importedCount: Int,
    val skippedCount: Int
)
