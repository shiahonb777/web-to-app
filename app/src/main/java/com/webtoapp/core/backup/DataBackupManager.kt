package com.webtoapp.core.backup

import android.content.Context
import android.net.Uri
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.data.database.AppDatabase
import com.webtoapp.data.model.AppCategory
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





class DataBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "DataBackupManager"
        private const val BACKUP_VERSION = 3
        private const val APPS_JSON = "apps.json"
        private const val RESOURCES_DIR = "resources/"
        private const val ICONS_DIR = "resources/icons/"
        private const val SPLASH_DIR = "resources/splash/"
        private const val BGM_DIR = "resources/bgm/"
        private const val BGM_LRC_DIR = "resources/bgm_lrc/"
        private const val BGM_COVER_DIR = "resources/bgm_cover/"
        private const val HTML_DIR = "resources/html/"
        private const val MEDIA_DIR = "resources/media/"
        private const val STATUSBAR_DIR = "resources/statusbar/"
        private const val GALLERY_DIR = "resources/gallery/"
        private const val CERTS_DIR = "resources/certs/"
        private const val MULTI_WEB_DIR = "resources/multi_web/"
        private const val EXTENSION_DIR = "extensions/"
        private const val EXTENSION_MODULES_FILE = "${EXTENSION_DIR}modules.json"
        private const val EXTENSION_BUILTIN_STATES_FILE = "${EXTENSION_DIR}builtin_states.json"
        private const val LOCAL_FILES_DIR = "local/files/"
        private const val LOCAL_EXTERNAL_FILES_DIR = "local/external_files/"
        private const val DATASTORE_DIR = "local/datastore/"
        private const val SHARED_PREFS_DIR = "local/shared_prefs/"

        private val MANAGED_FILES_DIRS = listOf(
            "extension_modules",
            "html_projects",
            "splash_media",
            "website_icons",
            "custom_ca"
        )

        private val MANAGED_EXTERNAL_DIRS = listOf(
            "AiCoding"
        )

        private val SAFE_DATASTORE_NAMES = listOf(
            "ai_coding",
            "language_settings",
            "theme_settings",
            "announcement"
        )

        private val SAFE_SHARED_PREF_NAMES = listOf(
            "installed_store_items"
        )


        private const val BUFFER_SIZE = 8192


        private val backupDateFormat = threadLocalCompat {
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        }


        private val gson: Gson by lazy {
            GsonBuilder()
                .setPrettyPrinting()
                .create()
        }
    }




    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val exportTime: Long = System.currentTimeMillis(),
        val appCount: Int = 0,
        val apps: List<WebApp> = emptyList(),
        val categories: List<AppCategory> = emptyList()
    )








    suspend fun exportAllData(
        repository: WebAppRepository,
        outputUri: Uri,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            onProgress(0, 100, Strings.backupReadingData)
            coroutineContext.ensureActive()


            val apps = repository.allWebApps.first()
            if (apps.isEmpty()) {
                return@withContext Result.failure(Exception("没有可导出的应用数据"))
            }

            AppLogger.i(TAG, "准备导出 ${apps.size} 个应用")

            val categories = AppDatabase.getInstance(context).appCategoryDao().getAllCategories().first()

            val resourceFiles = mutableMapOf<String, String>()

            apps.forEachIndexed { index, app ->
                coroutineContext.ensureActive()
                onProgress(10 + (index * 30 / apps.size), 100, Strings.backupCollectingResources.format(app.name))
                collectAppResources(app, resourceFiles)
            }

            AppLogger.i(TAG, "收集到 ${resourceFiles.size} 个资源文件")


            val appsWithRelativePaths = apps.map { app ->
                updateAppPathsToRelative(app, resourceFiles)
            }

            val backupData = BackupData(
                version = BACKUP_VERSION,
                exportTime = System.currentTimeMillis(),
                appCount = apps.size,
                apps = appsWithRelativePaths,
                categories = categories
            )
            val localFiles = collectLocalBackupFiles()

            onProgress(50, 100, Strings.backupCreatingFile)
            coroutineContext.ensureActive()


            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream, BUFFER_SIZE)).use { zipOut ->

                    val jsonBytes = gson.toJson(backupData).toByteArray(Charsets.UTF_8)
                    zipOut.putNextEntry(ZipEntry(APPS_JSON))
                    zipOut.write(jsonBytes)
                    zipOut.closeEntry()


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


                    localFiles.forEach { (zipPath, file) ->
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
                            AppLogger.w(TAG, "无法打包本地数据: ${file.absolutePath}", e)
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("无法创建输出文件"))

            onProgress(100, 100, Strings.backupExportComplete)

            Result.success(ExportResult(
                appCount = apps.size,
                resourceCount = resourceFiles.size + localFiles.size
            ))

        } catch (e: Exception) {
            AppLogger.e(TAG, "Export failed", e)
            Result.failure(e)
        }
    }








    suspend fun importAllData(
        repository: WebAppRepository,
        inputUri: Uri,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        val extractedFiles = mutableListOf<File>()

        try {
            onProgress(0, 100, Strings.backupReadingBackup)
            coroutineContext.ensureActive()

            var backupData: BackupData? = null
            val extractedResources = mutableMapOf<String, String>()
            var modulesJsonBytes: ByteArray? = null
            var builtInStatesJsonBytes: ByteArray? = null


            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream, BUFFER_SIZE)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    var totalEntries = 0

                    while (entry != null) {
                        coroutineContext.ensureActive()
                        totalEntries++
                        onProgress(10 + (totalEntries % 40), 100, Strings.backupExtracting.format(entry.name))

                        when {
                            entry.name == APPS_JSON -> {

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

                                val extractedPath = extractResourceFile(entry.name, zipIn)
                                if (extractedPath != null) {
                                    extractedResources[entry.name] = extractedPath
                                    extractedFiles.add(File(extractedPath))
                                }
                            }
                            isLocalBackupEntry(entry.name) && !entry.isDirectory -> {
                                restoreLocalBackupEntry(entry.name, zipIn)
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

            onProgress(60, 100, Strings.backupImportingData)
            coroutineContext.ensureActive()


            var importedCount = 0
            var skippedCount = 0
            val categoryIdMap = restoreCategories(data.categories)

            data.apps.forEachIndexed { index, app ->
                coroutineContext.ensureActive()
                onProgress(
                    60 + (index * 35 / data.apps.size),
                    100,
                    "正在导入: ${app.name}"
                )

                try {

                    val appWithLocalPaths = updateAppPathsToLocal(app, extractedResources)


                    val newApp = appWithLocalPaths.copy(
                        id = 0,
                        categoryId = remapCategoryId(appWithLocalPaths.categoryId, categoryIdMap),
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


            restoreExtensionFiles(modulesJsonBytes, builtInStatesJsonBytes)

            onProgress(100, 100, Strings.backupImportComplete)

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

        val categories = root.getAsJsonArray("categories")?.mapNotNull { element ->
            runCatching {
                gson.fromJson(element, AppCategory::class.java)
            }.getOrElse {
                AppLogger.w(TAG, "解析分类失败，已跳过一项", it)
                null
            }
        } ?: emptyList()

        return BackupData(
            version = version,
            exportTime = exportTime,
            appCount = if (appCount > 0) appCount else apps.size,
            apps = apps,
            categories = categories
        )
    }

    private suspend fun restoreCategories(categories: List<AppCategory>): Map<Long, Long> {
        if (categories.isEmpty()) return emptyMap()
        val dao = AppDatabase.getInstance(context).appCategoryDao()
        val idMap = mutableMapOf<Long, Long>()
        categories.forEach { category ->
            runCatching {
                val newId = dao.insert(category.copy(id = 0))
                idMap[category.id] = newId
            }.onFailure { e ->
                AppLogger.w(TAG, "导入分类失败: ${category.name}", e)
            }
        }
        return idMap
    }

    private fun remapCategoryId(categoryId: Long?, categoryIdMap: Map<Long, Long>): Long? {
        if (categoryId == null) return null
        return categoryIdMap[categoryId] ?: categoryId
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


            com.webtoapp.core.extension.ExtensionManager.release()
            com.webtoapp.core.extension.ExtensionManager.getInstance(context)
            AppLogger.i(TAG, "扩展模块配置已恢复")
        }.onFailure { e ->
            AppLogger.w(TAG, "恢复扩展模块配置失败", e)
        }
    }




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




    fun cleanupBackupTempFiles() {
        val backupDirs = listOf(
            "backup_icons", "backup_splash", "backup_bgm",
            "backup_bgm_lrc", "backup_bgm_cover",
            "backup_html", "backup_media", "backup_statusbar",
            "backup_gallery", "backup_certs", "backup_multi_web", "backup_other"
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




    fun getBackupTempSize(): Long {
        val backupDirs = listOf(
            "backup_icons", "backup_splash", "backup_bgm",
            "backup_bgm_lrc", "backup_bgm_cover",
            "backup_html", "backup_media", "backup_statusbar",
            "backup_gallery", "backup_certs", "backup_multi_web", "backup_other"
        )

        return backupDirs.sumOf { dirName ->
            val dir = File(context.filesDir, dirName)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else 0L
        }
    }




    private fun collectAppResources(app: WebApp, resources: MutableMap<String, String>) {
        val appId = app.id.toString()


        app.iconPath?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${ICONS_DIR}${appId}_icon.$ext"] = path
            }
        }


        app.splashConfig?.mediaPath?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${SPLASH_DIR}${appId}_splash.$ext"] = path
            }
        }


        app.bgmConfig?.playlist?.forEachIndexed { index, bgmItem ->

            if (!bgmItem.isAsset && File(bgmItem.path).exists()) {
                val ext = bgmItem.path.substringAfterLast('.', "mp3")
                resources["${BGM_DIR}${appId}_bgm_$index.$ext"] = bgmItem.path
            }

            bgmItem.lrcPath?.let { lrcPath ->
                if (File(lrcPath).exists()) {
                    resources["${BGM_LRC_DIR}${appId}_bgm_$index.lrc"] = lrcPath
                }
            }

            bgmItem.coverPath?.let { coverPath ->
                if (File(coverPath).exists()) {
                    val ext = coverPath.substringAfterLast('.', "jpg")
                    resources["${BGM_COVER_DIR}${appId}_bgm_cover_$index.$ext"] = coverPath
                }
            }
        }


        app.webViewConfig.statusBarBackgroundImage?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${STATUSBAR_DIR}${appId}_statusbar.$ext"] = path
            }
        }

        app.webViewConfig.statusBarBackgroundImageDark?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "png")
                resources["${STATUSBAR_DIR}${appId}_statusbar_dark.$ext"] = path
            }
        }


        app.htmlConfig?.files?.forEach { htmlFile ->
            if (File(htmlFile.path).exists()) {
                resources["${HTML_DIR}${appId}/${htmlFile.name}"] = htmlFile.path
            }
        }


        if (app.appType == com.webtoapp.data.model.AppType.IMAGE ||
            app.appType == com.webtoapp.data.model.AppType.VIDEO) {
            val mediaPath = app.url
            if (mediaPath.isNotBlank() && File(mediaPath).exists()) {
                val ext = mediaPath.substringAfterLast('.', "mp4")
                resources["${MEDIA_DIR}${appId}_media.$ext"] = mediaPath
            }
        }

        app.mediaConfig?.mediaPath?.let { path ->
            if (File(path).exists()) {
                val ext = path.substringAfterLast('.', "mp4")
                resources["${MEDIA_DIR}${appId}_media_config.$ext"] = path
            }
        }

        app.galleryConfig?.items?.forEachIndexed { index, item ->
            if (File(item.path).exists()) {
                val ext = item.path.substringAfterLast('.', "jpg")
                resources["${GALLERY_DIR}${appId}_gallery_$index.$ext"] = item.path
            }
            item.thumbnailPath?.let { path ->
                if (File(path).exists()) {
                    val ext = path.substringAfterLast('.', "jpg")
                    resources["${GALLERY_DIR}${appId}_gallery_thumb_$index.$ext"] = path
                }
            }
        }

        app.apkExportConfig?.networkTrustConfig?.customCaCertificates?.forEachIndexed { index, cert ->
            if (File(cert.filePath).exists()) {
                val ext = cert.filePath.substringAfterLast('.', "cer")
                resources["${CERTS_DIR}${appId}_custom_ca_$index.$ext"] = cert.filePath
            }
        }

        app.multiWebConfig?.sites?.forEachIndexed { index, site ->
            val path = site.localFilePath
            if (path.isNotBlank() && File(path).exists()) {
                val ext = path.substringAfterLast('.', "html")
                resources["${MULTI_WEB_DIR}${appId}_site_$index.$ext"] = path
            }
        }
    }




    private fun collectLocalBackupFiles(): Map<String, File> {
        val files = linkedMapOf<String, File>()

        MANAGED_FILES_DIRS.forEach { dirName ->
            collectDirectoryFiles(
                sourceDir = File(context.filesDir, dirName),
                zipRoot = "$LOCAL_FILES_DIR$dirName/",
                files = files
            )
        }

        val externalFilesDir = context.getExternalFilesDir(null)
        if (externalFilesDir != null) {
            MANAGED_EXTERNAL_DIRS.forEach { dirName ->
                collectDirectoryFiles(
                    sourceDir = File(externalFilesDir, dirName),
                    zipRoot = "$LOCAL_EXTERNAL_FILES_DIR$dirName/",
                    files = files
                )
            }
        }

        SAFE_DATASTORE_NAMES.forEach { name ->
            val file = File(context.filesDir, "datastore/$name.preferences_pb")
            if (file.exists() && file.isFile && file.canRead()) {
                files["$DATASTORE_DIR${file.name}"] = file
            }
        }

        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        SAFE_SHARED_PREF_NAMES.forEach { name ->
            val file = File(sharedPrefsDir, "$name.xml")
            if (file.exists() && file.isFile && file.canRead()) {
                files["$SHARED_PREFS_DIR${file.name}"] = file
            }
        }

        return files
    }

    private fun collectDirectoryFiles(
        sourceDir: File,
        zipRoot: String,
        files: MutableMap<String, File>
    ) {
        if (!sourceDir.exists() || !sourceDir.isDirectory) return
        sourceDir.walkTopDown()
            .filter { it.isFile && it.canRead() }
            .forEach { file ->
                val relativePath = file.relativeTo(sourceDir).invariantSeparatorsPath
                files["$zipRoot$relativePath"] = file
            }
    }




    private fun updateAppPathsToRelative(
        app: WebApp,
        resources: Map<String, String>
    ): WebApp {
        val appId = app.id.toString()


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
            mediaConfig = app.mediaConfig?.copy(
                mediaPath = findZipPath(app.mediaConfig.mediaPath) ?: app.mediaConfig.mediaPath
            ),
            galleryConfig = app.galleryConfig?.copy(
                items = app.galleryConfig.items.map { item ->
                    item.copy(
                        path = findZipPath(item.path) ?: item.path,
                        thumbnailPath = item.thumbnailPath?.let { findZipPath(it) ?: it }
                    )
                }
            ),
            apkExportConfig = app.apkExportConfig?.copy(
                networkTrustConfig = app.apkExportConfig.networkTrustConfig.copy(
                    customCaCertificates = app.apkExportConfig.networkTrustConfig.customCaCertificates.map { cert ->
                        cert.copy(filePath = findZipPath(cert.filePath) ?: cert.filePath)
                    }
                )
            ),
            multiWebConfig = app.multiWebConfig?.copy(
                sites = app.multiWebConfig.sites.map { site ->
                    site.copy(
                        localFilePath = findZipPath(site.localFilePath) ?: site.localFilePath
                    )
                }
            ),
            webViewConfig = app.webViewConfig.copy(
                statusBarBackgroundImage = findZipPath(app.webViewConfig.statusBarBackgroundImage),
                statusBarBackgroundImageDark = findZipPath(app.webViewConfig.statusBarBackgroundImageDark)
            ),
            url = if (app.appType == com.webtoapp.data.model.AppType.IMAGE ||
                      app.appType == com.webtoapp.data.model.AppType.VIDEO) {
                findZipPath(app.url) ?: app.url
            } else app.url
        )
    }




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
            mediaConfig = app.mediaConfig?.copy(
                mediaPath = extractedResources[app.mediaConfig.mediaPath] ?: app.mediaConfig.mediaPath
            ),
            galleryConfig = app.galleryConfig?.copy(
                items = app.galleryConfig.items.map { item ->
                    item.copy(
                        path = extractedResources[item.path] ?: item.path,
                        thumbnailPath = item.thumbnailPath?.let { extractedResources[it] ?: it }
                    )
                }
            ),
            apkExportConfig = app.apkExportConfig?.copy(
                networkTrustConfig = app.apkExportConfig.networkTrustConfig.copy(
                    customCaCertificates = app.apkExportConfig.networkTrustConfig.customCaCertificates.map { cert ->
                        cert.copy(filePath = extractedResources[cert.filePath] ?: cert.filePath)
                    }
                )
            ),
            multiWebConfig = app.multiWebConfig?.copy(
                sites = app.multiWebConfig.sites.map { site ->
                    site.copy(
                        localFilePath = extractedResources[site.localFilePath] ?: site.localFilePath
                    )
                }
            ),
            webViewConfig = app.webViewConfig.copy(
                statusBarBackgroundImage = app.webViewConfig.statusBarBackgroundImage?.let {
                    extractedResources[it] ?: it
                },
                statusBarBackgroundImageDark = app.webViewConfig.statusBarBackgroundImageDark?.let {
                    extractedResources[it] ?: it
                }
            ),
            url = if (app.appType == com.webtoapp.data.model.AppType.IMAGE ||
                      app.appType == com.webtoapp.data.model.AppType.VIDEO) {
                extractedResources[app.url] ?: app.url
            } else app.url
        )
    }




    private fun extractResourceFile(zipPath: String, zipIn: ZipInputStream): String? {
        return try {
            val (targetDir, prefix) = when {
                zipPath.startsWith(ICONS_DIR) -> File(context.filesDir, "backup_icons") to ICONS_DIR
                zipPath.startsWith(SPLASH_DIR) -> File(context.filesDir, "backup_splash") to SPLASH_DIR
                zipPath.startsWith(BGM_LRC_DIR) -> File(context.filesDir, "backup_bgm_lrc") to BGM_LRC_DIR
                zipPath.startsWith(BGM_COVER_DIR) -> File(context.filesDir, "backup_bgm_cover") to BGM_COVER_DIR
                zipPath.startsWith(BGM_DIR) -> File(context.filesDir, "backup_bgm") to BGM_DIR
                zipPath.startsWith(HTML_DIR) -> File(context.filesDir, "backup_html") to HTML_DIR
                zipPath.startsWith(MEDIA_DIR) -> File(context.filesDir, "backup_media") to MEDIA_DIR
                zipPath.startsWith(STATUSBAR_DIR) -> File(context.filesDir, "backup_statusbar") to STATUSBAR_DIR
                zipPath.startsWith(GALLERY_DIR) -> File(context.filesDir, "backup_gallery") to GALLERY_DIR
                zipPath.startsWith(CERTS_DIR) -> File(context.filesDir, "backup_certs") to CERTS_DIR
                zipPath.startsWith(MULTI_WEB_DIR) -> File(context.filesDir, "backup_multi_web") to MULTI_WEB_DIR
                else -> File(context.filesDir, "backup_other") to RESOURCES_DIR
            }

            targetDir.mkdirs()

            val relativePath = zipPath.removePrefix(prefix).ifBlank { zipPath.substringAfterLast('/') }
            val targetFile = resolveSafeChild(targetDir, relativePath) ?: return null
            targetFile.parentFile?.mkdirs()

            FileOutputStream(targetFile).use { output ->
                zipIn.copyTo(output)
            }

            targetFile.absolutePath
        } catch (e: Exception) {
            AppLogger.w(TAG, "解压资源文件失败: $zipPath", e)
            null
        }
    }

    private fun isLocalBackupEntry(zipPath: String): Boolean {
        return zipPath.startsWith(LOCAL_FILES_DIR) ||
            zipPath.startsWith(LOCAL_EXTERNAL_FILES_DIR) ||
            zipPath.startsWith(DATASTORE_DIR) ||
            zipPath.startsWith(SHARED_PREFS_DIR)
    }

    private fun restoreLocalBackupEntry(zipPath: String, zipIn: ZipInputStream) {
        val targetFile = when {
            zipPath.startsWith(LOCAL_FILES_DIR) -> {
                val relativePath = zipPath.removePrefix(LOCAL_FILES_DIR)
                resolveSafeChild(context.filesDir, relativePath)
            }
            zipPath.startsWith(LOCAL_EXTERNAL_FILES_DIR) -> {
                val externalFilesDir = context.getExternalFilesDir(null) ?: return
                val relativePath = zipPath.removePrefix(LOCAL_EXTERNAL_FILES_DIR)
                resolveSafeChild(externalFilesDir, relativePath)
            }
            zipPath.startsWith(DATASTORE_DIR) -> {
                val fileName = zipPath.removePrefix(DATASTORE_DIR)
                val allowed = SAFE_DATASTORE_NAMES.map { "$it.preferences_pb" }.contains(fileName)
                if (allowed) File(context.filesDir, "datastore/$fileName") else null
            }
            zipPath.startsWith(SHARED_PREFS_DIR) -> {
                val fileName = zipPath.removePrefix(SHARED_PREFS_DIR)
                val allowed = SAFE_SHARED_PREF_NAMES.map { "$it.xml" }.contains(fileName)
                if (allowed) File(File(context.applicationInfo.dataDir, "shared_prefs"), fileName) else null
            }
            else -> null
        } ?: return

        runCatching {
            targetFile.parentFile?.mkdirs()
            FileOutputStream(targetFile).use { output ->
                zipIn.copyTo(output, BUFFER_SIZE)
            }
        }.onFailure { e ->
            AppLogger.w(TAG, "恢复本地数据失败: $zipPath", e)
        }
    }

    private fun resolveSafeChild(baseDir: File, relativePath: String): File? {
        val targetFile = File(baseDir, relativePath)
        val baseCanonical = baseDir.canonicalFile
        val targetCanonical = targetFile.canonicalFile
        return if (targetCanonical.path.startsWith(baseCanonical.path + File.separator)) {
            targetCanonical
        } else {
            AppLogger.w(TAG, "跳过不安全的备份路径: $relativePath")
            null
        }
    }




    fun generateBackupFileName(): String {
        return "WebToApp_Backup_${backupDateFormat.get()!!.format(Date())}.zip"
    }
}




data class ExportResult(
    val appCount: Int,
    val resourceCount: Int
)




data class ImportResult(
    val totalCount: Int,
    val importedCount: Int,
    val skippedCount: Int
)
