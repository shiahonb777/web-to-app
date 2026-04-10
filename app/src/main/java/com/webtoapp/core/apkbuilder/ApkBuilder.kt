package com.webtoapp.core.apkbuilder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import androidx.core.content.FileProvider
import com.webtoapp.core.crypto.AssetEncryptor
import com.webtoapp.core.crypto.EncryptedApkBuilder
import com.webtoapp.core.crypto.EncryptionConfig
import com.webtoapp.core.crypto.KeyManager
import com.webtoapp.core.crypto.toHexString
import com.webtoapp.core.shell.BgmShellItem
import com.webtoapp.core.shell.LrcShellTheme
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*
import javax.crypto.SecretKey
import com.webtoapp.util.AppConstants
import com.webtoapp.util.TextFileClassifier

/**
 * APK Builder
 * Responsible for packaging WebApp configuration into standalone APK installer
 * 
 * How it works:
 * 1. Copy current app APK as template (because current app supports Shell mode)
 * 2. Inject app_config.json config file into assets directory
 * 3. Modify package name in AndroidManifest.xml (make each exported app independent)
 * 4. Modify app name in resources.arsc
 * 5. Replace icon resources
 * 6. Optional: encrypt resource files
 * 7. Re-sign
 */
class ApkBuilder(private val context: Context) {

    companion object {
        private val SANITIZE_FILENAME_REGEX = AppConstants.SANITIZE_FILENAME_REGEX
        private val PACKAGE_NAME_REGEX = AppConstants.PACKAGE_NAME_REGEX
        private val CHARSET_REGEX = AppConstants.CHARSET_REGEX
    }

    private val template = ApkTemplate(context)
    private val signer = JarSigner(context)
    private val axmlEditor = AxmlEditor()
    private val axmlRebuilder = AxmlRebuilder()
    private val arscEditor = ArscEditor()
    private val arscRebuilder = ArscRebuilder()  // 重建 string pool，支持任意长度应用名
    private val logger = BuildLogger(context)
    private val encryptedApkBuilder = EncryptedApkBuilder(context)
    private val keyManager = KeyManager.getInstance(context)
    
    // Output directory
    private val outputDir = File(context.getExternalFilesDir(null), "built_apks").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "apk_build_temp").apply { mkdirs() }
    
    // 原始应用名（ArscRebuilder 可处理任意长度，无需预填充）
    private val originalAppName = "WebToApp"
    private val originalPackageName = "com.webtoapp"
    
    /**
     * Clean temp directory
     * Delete all temporary build files, release storage space
     */
    fun cleanTempFiles() {
        try {
            tempDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
            AppLogger.d("ApkBuilder", "Temp files cleaned")
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to clean temp files", e)
        }
    }
    
    /**
     * Get temp directory size (bytes)
     */
    fun getTempDirSize(): Long {
        return tempDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
    
    /**
     * Clean old build artifacts (keep most recent N)
     */
    fun cleanOldBuilds(keepCount: Int = 5) {
        try {
            val apkFiles = outputDir.listFiles { file -> file.extension == "apk" }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            
            if (apkFiles.size > keepCount) {
                apkFiles.drop(keepCount).forEach { file ->
                    file.delete()
                    AppLogger.d("ApkBuilder", "Deleted old build: ${file.name}")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to clean old builds", e)
        }
    }

    /**
     * Build APK
     * @param webApp WebApp configuration
     * @param onProgress Progress callback (0-100)
     * @return Build result
     */
    suspend fun buildApk(
        webApp: WebApp,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): BuildResult = withContext(Dispatchers.IO) {
        // Start logging
        val logFile = logger.startNewLog(webApp.name)
        
        try {
            onProgress(0, "Preparing build...")
            
            // Get encryption config
            val encryptionConfig = webApp.apkExportConfig?.encryptionConfig?.toEncryptionConfig()
                ?: EncryptionConfig.DISABLED
            
            // Get hardening config
            val hardeningConfig = webApp.apkExportConfig?.hardeningConfig
                ?: com.webtoapp.data.model.AppHardeningConfig()
            
            // Get performance optimization config
            val perfOptEnabled = webApp.apkExportConfig?.performanceOptimization == true
            val perfConfig = if (perfOptEnabled) {
                webApp.apkExportConfig?.performanceConfig?.toOptimizerConfig()
                    ?: com.webtoapp.core.linux.PerformanceOptimizer.OptimizeConfig()
            } else null
            
            // Log complete WebApp config
            logger.section("WebApp Config")
            logger.logKeyValue("appName", webApp.name)
            logger.logKeyValue("appType", webApp.appType)
            logger.logKeyValue("url", webApp.url)
            logger.logKeyValue("iconPath", webApp.iconPath)
            logger.logKeyValue("splashEnabled", webApp.splashEnabled)
            logger.logKeyValue("bgmEnabled", webApp.bgmEnabled)
            logger.logKeyValue("activationEnabled", webApp.activationEnabled)
            logger.logKeyValue("adBlockEnabled", webApp.adBlockEnabled)
            logger.logKeyValue("translateEnabled", webApp.translateEnabled)
            logger.logKeyValue("encryptionEnabled", encryptionConfig.enabled)
            if (encryptionConfig.enabled) {
                logger.logKeyValue("encryptConfig", encryptionConfig.encryptConfig)
                logger.logKeyValue("encryptHtml", encryptionConfig.encryptHtml)
                logger.logKeyValue("encryptMedia", encryptionConfig.encryptMedia)
            }
            logger.logKeyValue("hardeningEnabled", hardeningConfig.enabled)
            if (hardeningConfig.enabled) {
                logger.logKeyValue("hardeningLevel", hardeningConfig.hardeningLevel.name)
                logger.logKeyValue("dexEncryption", hardeningConfig.dexEncryption)
                logger.logKeyValue("soEncryption", hardeningConfig.soEncryption)
                logger.logKeyValue("antiDebugMultiLayer", hardeningConfig.antiDebugMultiLayer)
                logger.logKeyValue("responseStrategy", hardeningConfig.responseStrategy.name)
            }
            
            logger.logKeyValue("performanceOptimization", perfOptEnabled)
            
            // APK export config
            logger.section("APK Export Config")
            logger.logKeyValue("customPackageName", webApp.apkExportConfig?.customPackageName)
            logger.logKeyValue("customVersionCode", webApp.apkExportConfig?.customVersionCode)
            logger.logKeyValue("customVersionName", webApp.apkExportConfig?.customVersionName)
            
            // Architecture config
            val architecture = webApp.apkExportConfig?.architecture 
                ?: com.webtoapp.data.model.ApkArchitecture.UNIVERSAL
            logger.logKeyValue("architecture", architecture.name)
            logger.logKeyValue("abiFilters", architecture.abiFilters.joinToString(", "))
            
            // WebView config
            logger.section("WebView Config")
            logger.logKeyValue("hideToolbar", webApp.webViewConfig.hideToolbar)
            logger.logKeyValue("javaScriptEnabled", webApp.webViewConfig.javaScriptEnabled)
            logger.logKeyValue("desktopMode", webApp.webViewConfig.desktopMode)
            logger.logKeyValue("landscapeMode", webApp.webViewConfig.landscapeMode)
            logger.logKeyValue("userAgentMode", webApp.webViewConfig.userAgentMode.name)
            logger.logKeyValue("customUserAgent", webApp.webViewConfig.customUserAgent)
            logger.logKeyValue("userAgent(legacy)", webApp.webViewConfig.userAgent)
            
            // Media config
            logger.section("Media Config")
            logger.logKeyValue("mediaConfig", webApp.mediaConfig)
            logger.logKeyValue("mediaConfig.mediaPath", webApp.mediaConfig?.mediaPath)
            
            // HTML config
            if (webApp.appType == com.webtoapp.data.model.AppType.HTML) {
                logger.section("HTML Config")
                logger.logKeyValue("htmlConfig.projectId", webApp.htmlConfig?.projectId)
                logger.logKeyValue("htmlConfig.entryFile", webApp.htmlConfig?.entryFile)
                logger.logKeyValue("htmlConfig.files.size", webApp.htmlConfig?.files?.size ?: 0)
                webApp.htmlConfig?.files?.forEachIndexed { index, file ->
                    val exists = File(file.path).exists()
                    logger.log("  file[$index]: name=${file.name}, path=${file.path}, exists=$exists")
                }
            }
            
            // Splash screen config
            logger.section("Splash Screen Config")
            logger.logKeyValue("splashEnabled", webApp.splashEnabled)
            logger.logKeyValue("splashConfig.type", webApp.splashConfig?.type)
            logger.logKeyValue("splashConfig.mediaPath", webApp.splashConfig?.mediaPath)
            logger.logKeyValue("splashMediaPath (getSplashMediaPath)", webApp.getSplashMediaPath())
            
            // BGM config
            logger.section("BGM Config")
            logger.logKeyValue("bgmEnabled", webApp.bgmEnabled)
            logger.logKeyValue("bgmConfig.playlist.size", webApp.bgmConfig?.playlist?.size ?: 0)
            
            // Also keep original Logcat logs
            AppLogger.d("ApkBuilder", "Build started - WebApp config:")
            AppLogger.d("ApkBuilder", "  appName=${webApp.name}")
            AppLogger.d("ApkBuilder", "  appType=${webApp.appType}")
            
            // Generate package name
            logger.section("Generate Package Name")
            val customPkg = webApp.apkExportConfig?.customPackageName?.takeIf { 
                it.isNotBlank() && 
                it.matches(PACKAGE_NAME_REGEX)
            }
            val packageName = customPkg ?: generatePackageName(webApp.name)
            
            if (webApp.apkExportConfig?.customPackageName?.isNotBlank() == true && customPkg == null) {
                logger.warn("Custom package name format invalid, using auto-generated: $packageName")
            }
            logger.logKeyValue("finalPackageName", packageName)
            
            val config = webApp.toApkConfigWithModules(packageName, context)
            logger.logKeyValue("versionCode", config.versionCode)
            logger.logKeyValue("versionName", config.versionName)
            logger.logKeyValue("embeddedExtensionModules.size", config.embeddedExtensionModules.size)
            
            // Log each embedded extension module in detail
            config.embeddedExtensionModules.forEachIndexed { index, module ->
                logger.log("  embeddedModule[$index]: id=${module.id}, name=${module.name}, enabled=${module.enabled}, runAt=${module.runAt}, codeLength=${module.code.length}")
            }
            
            onProgress(10, "Checking template...")
            logger.section("Parallel Resource Preparation")
            
            val unsignedApk = File(tempDir, "${packageName}_unsigned.apk")
            val signedApk = File(outputDir, "${sanitizeFileName(webApp.name)}_v${config.versionName}.APK")
            logger.logKeyValue("unsignedApkPath", unsignedApk.absolutePath)
            logger.logKeyValue("signedApkPath", signedApk.absolutePath)
            
            unsignedApk.delete()
            signedApk.delete()
            
            // === Parallel resource preparation ===
            // Template APK, project directories, and encryption key are all independent I/O
            // operations that can run concurrently to reduce total prep time.
            val prepStartTime = System.currentTimeMillis()
            
            data class PreparedResources(
                val templateApk: File?,
                val mediaContentPath: String?,
                val htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
                val bgmPlaylistPaths: List<String>,
                val bgmLrcDataList: List<LrcData?>,
                val galleryItems: List<com.webtoapp.data.model.GalleryItem>,
                val wordPressProjectDir: File?,
                val nodejsProjectDir: File?,
                val phpAppProjectDir: File?,
                val pythonAppProjectDir: File?,
                val goAppProjectDir: File?,
                val frontendProjectDir: File?,
                val encryptionKey: SecretKey?
            )
            
            val prepared = coroutineScope {
                // Async: Template APK (potentially slow — copies ~50MB)
                val templateDeferred = async {
                    getOrCreateTemplate()
                }
                
                // Async: Encryption key generation
                val encKeyDeferred = async {
                    if (encryptionConfig.enabled) {
                        val signatureHash = signer.getCertificateSignatureHash()
                        keyManager.generateKeyForPackage(packageName, signatureHash)
                    } else null
                }
                
                // Async: Project directory lookups (all independent filesystem checks)
                val wpDirDeferred = async {
                    if (webApp.appType == com.webtoapp.data.model.AppType.WORDPRESS) {
                        val projectId = webApp.wordpressConfig?.projectId ?: ""
                        if (projectId.isNotEmpty()) com.webtoapp.core.wordpress.WordPressManager.getProjectDir(context, projectId) else null
                    } else null
                }
                val nodeDirDeferred = async {
                    if (webApp.appType == com.webtoapp.data.model.AppType.NODEJS_APP) {
                        val projectId = webApp.nodejsConfig?.projectId ?: ""
                        if (projectId.isNotEmpty()) com.webtoapp.core.nodejs.NodeRuntime(context).getProjectDir(projectId) else null
                    } else null
                }
                val phpDirDeferred = async {
                    if (webApp.appType == com.webtoapp.data.model.AppType.PHP_APP) {
                        val projectId = webApp.phpAppConfig?.projectId ?: ""
                        if (projectId.isNotEmpty()) com.webtoapp.core.php.PhpAppRuntime(context).getProjectDir(projectId) else null
                    } else null
                }
                val pythonDirDeferred = async {
                    if (webApp.appType == com.webtoapp.data.model.AppType.PYTHON_APP) {
                        val projectId = webApp.pythonAppConfig?.projectId ?: ""
                        if (projectId.isNotEmpty()) File(context.filesDir, "python_projects/$projectId") else null
                    } else null
                }
                val goDirDeferred = async {
                    if (webApp.appType == com.webtoapp.data.model.AppType.GO_APP) {
                        val projectId = webApp.goAppConfig?.projectId ?: ""
                        if (projectId.isNotEmpty()) File(context.filesDir, "go_projects/$projectId") else null
                    } else null
                }
                
                // Synchronous (fast, in-memory): media paths, HTML files, BGM, gallery
                val mediaContentPath = if (webApp.appType == com.webtoapp.data.model.AppType.IMAGE || 
                                           webApp.appType == com.webtoapp.data.model.AppType.VIDEO) webApp.url else null
                val htmlFiles = if (webApp.appType == com.webtoapp.data.model.AppType.HTML) webApp.htmlConfig?.files ?: emptyList() else emptyList()
                val bgmPlaylistPaths = if (webApp.bgmEnabled) webApp.bgmConfig?.playlist?.map { it.path } ?: emptyList() else emptyList()
                val bgmLrcDataList = if (webApp.bgmEnabled) webApp.bgmConfig?.playlist?.map { it.lrcData } ?: emptyList() else emptyList()
                val galleryItems = if (webApp.appType == com.webtoapp.data.model.AppType.GALLERY) webApp.galleryConfig?.items ?: emptyList() else emptyList()
                val frontendProjectDir = if (webApp.appType == com.webtoapp.data.model.AppType.FRONTEND) webApp.htmlConfig?.projectDir?.let { File(it) } else null
                
                // Await all parallel results
                PreparedResources(
                    templateApk = templateDeferred.await(),
                    mediaContentPath = mediaContentPath,
                    htmlFiles = htmlFiles,
                    bgmPlaylistPaths = bgmPlaylistPaths,
                    bgmLrcDataList = bgmLrcDataList,
                    galleryItems = galleryItems,
                    wordPressProjectDir = wpDirDeferred.await(),
                    nodejsProjectDir = nodeDirDeferred.await(),
                    phpAppProjectDir = phpDirDeferred.await(),
                    pythonAppProjectDir = pythonDirDeferred.await(),
                    goAppProjectDir = goDirDeferred.await(),
                    frontendProjectDir = frontendProjectDir,
                    encryptionKey = encKeyDeferred.await()
                )
            }
            
            val prepElapsed = System.currentTimeMillis() - prepStartTime
            logger.log("Parallel resource preparation completed in ${prepElapsed}ms")
            
            // Unpack prepared resources
            val templateApk = prepared.templateApk
            if (templateApk == null) {
                logger.error("Failed to get template APK")
                logger.endLog(false, "Failed to get template APK")
                return@withContext BuildResult.Error("Failed to get template APK")
            }
            logger.logKeyValue("templatePath", templateApk.absolutePath)
            logger.logKeyValue("templateSize", "${templateApk.length() / 1024} KB")
            
            val mediaContentPath = prepared.mediaContentPath
            val htmlFiles = prepared.htmlFiles
            val bgmPlaylistPaths = prepared.bgmPlaylistPaths
            val bgmLrcDataList = prepared.bgmLrcDataList
            val galleryItems = prepared.galleryItems
            val wordPressProjectDir = prepared.wordPressProjectDir
            val nodejsProjectDir = prepared.nodejsProjectDir
            val phpAppProjectDir = prepared.phpAppProjectDir
            val pythonAppProjectDir = prepared.pythonAppProjectDir
            val goAppProjectDir = prepared.goAppProjectDir
            val frontendProjectDir = prepared.frontendProjectDir
            val encryptionKey = prepared.encryptionKey
            
            // Log resource details
            logger.section("Prepared Resources")
            logger.logKeyValue("mediaContentPath", mediaContentPath)
            if (mediaContentPath != null) {
                val mediaFile = File(mediaContentPath)
                logger.logKeyValue("mediaFile.exists", mediaFile.exists())
                logger.logKeyValue("mediaFile.size", if (mediaFile.exists()) "${mediaFile.length() / 1024} KB" else "N/A")
            }
            logger.logKeyValue("htmlFiles.size", htmlFiles.size)
            htmlFiles.forEachIndexed { index, file ->
                val exists = File(file.path).exists()
                logger.log("  html[$index]: name=${file.name}, path=${file.path}, exists=$exists")
            }
            logger.logKeyValue("bgmPlaylistPaths.size", bgmPlaylistPaths.size)
            logger.logKeyValue("galleryItems.size", galleryItems.size)
            logger.logKeyValue("wordPressProjectDir", wordPressProjectDir?.absolutePath)
            logger.logKeyValue("wordPressProjectDir.exists", wordPressProjectDir?.exists())
            logger.logKeyValue("nodejsProjectDir", nodejsProjectDir?.absolutePath)
            logger.logKeyValue("nodejsProjectDir.exists", nodejsProjectDir?.exists())
            logger.logKeyValue("phpAppProjectDir", phpAppProjectDir?.absolutePath)
            logger.logKeyValue("pythonAppProjectDir", pythonAppProjectDir?.absolutePath)
            logger.logKeyValue("goAppProjectDir", goAppProjectDir?.absolutePath)
            logger.logKeyValue("frontendProjectDir", frontendProjectDir?.absolutePath)
            logger.logKeyValue("frontendProjectDir.exists", frontendProjectDir?.exists())
            if (encryptionConfig.enabled) {
                logger.section("Encryption Key")
                logger.log("Encryption key generated (using target signature)")
            }
            
            onProgress(20, "Preparing resources...")
            
            // Modify APK content
            logger.section("Modify APK Content")
            if (encryptionConfig.enabled) {
                onProgress(30, "Encrypting resources...")
                logger.log("Encryption mode enabled")
            }
            modifyApk(
                templateApk, unsignedApk, config, webApp.iconPath, 
                webApp.getSplashMediaPath(), mediaContentPath,
                bgmPlaylistPaths, bgmLrcDataList, htmlFiles, galleryItems,
                encryptionConfig, encryptionKey,
                hardeningConfig,
                architecture.abiFilters,
                wordPressProjectDir,
                nodejsProjectDir,
                frontendProjectDir,
                phpAppProjectDir,
                pythonAppProjectDir,
                goAppProjectDir,
                perfConfig
            ) { progress ->
                val msg = when {
                    perfOptEnabled && encryptionConfig.enabled -> "Optimizing & encrypting resources..."
                    perfOptEnabled -> "Optimizing resources..."
                    encryptionConfig.enabled -> "Encrypting and processing resources..."
                    else -> "Processing resources..."
                }
                onProgress(30 + (progress * 0.4).toInt(), msg)
            }
            
            onProgress(70, "Signing APK...")
            
            // Check if unsigned APK is valid
            if (!unsignedApk.exists() || unsignedApk.length() == 0L) {
                logger.error("Unsigned APK invalid: exists=${unsignedApk.exists()}, size=${unsignedApk.length()}")
                logger.endLog(false, "Failed to generate unsigned APK")
                return@withContext BuildResult.Error("Failed to generate unsigned APK")
            }
            logger.logKeyValue("unsignedApkSize", "${unsignedApk.length() / 1024} KB")
            
            // 【复刻 1.8.5】直接签名，不做任何优化/对齐
            // 1.8.5 没有 NativeApkOptimizer、没有 ZipAligner，签名完全稳定
            logger.section("Sign APK")
            logger.logKeyValue("signerType", signer.getSignerType().name)
            
            // Signature
            val signSuccess = try {
                signer.sign(unsignedApk, signedApk)
            } catch (e: Exception) {
                logger.error("Signing exception", e)
                logger.endLog(false, "Signing failed: ${e.message}")
                return@withContext BuildResult.Error("Signing failed: ${e.message ?: "Unknown error"}")
            }
            
            // 检查签名后文件是否有效（唯一的硬性条件）
            if (!signedApk.exists() || signedApk.length() == 0L) {
                logger.error("Signed APK file missing or empty after sign()")
                logger.endLog(false, "Signed APK file invalid")
                if (signedApk.exists()) signedApk.delete()
                return@withContext BuildResult.Error("APK signing failed: output file invalid")
            }
            
            logger.logKeyValue("signedApkSize", "${signedApk.length() / 1024} KB")
            
            if (!signSuccess) {
                // sign() 返回 false 表示 ApkVerifier 校验有警告/错误，但文件已存在且非空
                // 大多数情况下 APK 仍可正常安装，仅记录警告
                logger.warn("ApkVerifier reported issues, but signed APK file is valid (${signedApk.length() / 1024} KB). Continuing build.")
            }

            onProgress(85, "Verifying APK...")
            logger.section("Verify APK")
            
            val parseResult = debugApkStructure(signedApk)
            logger.logKeyValue("apkPreParseResult", parseResult)
            if (!parseResult) {
                logger.warn("APK pre-parse failed, may not be installable")
            }
            
            onProgress(90, "Analyzing & cleaning up...")
            
            // Parallel post-build: analysis + cleanup run concurrently
            val analysisReport = coroutineScope {
                val analysisDeferred = async {
                    try {
                        val report = ApkAnalyzer.analyze(signedApk)
                        logger.section("APK Analysis")
                        logger.log(ApkAnalyzer.formatReport(report))
                        report
                    } catch (e: Exception) {
                        AppLogger.e("ApkBuilder", "APK analysis failed (non-fatal)", e)
                        null
                    }
                }
                val cleanupDeferred = async {
                    unsignedApk.delete()
                    cleanTempFiles()
                }
                
                cleanupDeferred.await()
                analysisDeferred.await()
            }
            
            onProgress(100, "Build complete")
            
            logger.logKeyValue("finalApkPath", signedApk.absolutePath)
            logger.logKeyValue("finalApkSize", "${signedApk.length() / 1024} KB")
            logger.endLog(true, "Build successful")
            
            BuildResult.Success(signedApk, logger.getCurrentLogPath(), analysisReport)
            
        } catch (e: Exception) {
            logger.error("Exception during build", e)
            logger.endLog(false, "Build failed: ${e.message}")
            
            // Clean temp files even on build failure
            cleanTempFiles()
            
            BuildResult.Error("Build failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Get template APK
     * Use current app as template (because it supports Shell mode)
     * Caches the template and only re-copies when the source APK changes.
     */
    private fun getOrCreateTemplate(): File? {
        return try {
            val currentApk = File(context.applicationInfo.sourceDir)
            val templateFile = File(tempDir, "base_template.apk")
            
            // Only copy if template doesn't exist or source APK has changed
            val needsCopy = !templateFile.exists() ||
                templateFile.length() != currentApk.length() ||
                templateFile.lastModified() < currentApk.lastModified()
            
            if (needsCopy) {
                currentApk.copyTo(templateFile, overwrite = true)
                AppLogger.d("ApkBuilder", "Template APK copied (source changed)")
            } else {
                AppLogger.d("ApkBuilder", "Using cached template APK")
            }
            templateFile
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Operation failed", e)
            null
        }
    }

    /**
     * Modify APK content
     * 1. Inject config file (optional encryption)
     * 2. Modify package name
     * 3. Modify app name
     * 4. Replace/add icon
     * 5. Embed splash media (optional encryption)
     * 6. Embed media app content (optional encryption)
     * 7. Embed HTML files (optional encryption)
     * 8. Filter unnecessary resource files (reduce APK size)
     */
    private suspend fun modifyApk(
        sourceApk: File,
        outputApk: File,
        config: ApkConfig,
        iconPath: String?,
        splashMediaPath: String?,
        mediaContentPath: String? = null,
        bgmPlaylistPaths: List<String> = emptyList(),
        bgmLrcDataList: List<LrcData?> = emptyList(),
        htmlFiles: List<com.webtoapp.data.model.HtmlFile> = emptyList(),
        galleryItems: List<com.webtoapp.data.model.GalleryItem> = emptyList(),
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED,
        encryptionKey: SecretKey? = null,
        hardeningConfig: com.webtoapp.data.model.AppHardeningConfig = com.webtoapp.data.model.AppHardeningConfig(),
        abiFilters: List<String> = emptyList(),  // Architecture filter, empty means no filter
        wordPressProjectDir: File? = null,  // WordPress project directory
        nodejsProjectDir: File? = null,     // Node.js project directory
        frontendProjectDir: File? = null,   // Frontend project directory
        phpAppProjectDir: File? = null,     // PHP app project directory
        pythonAppProjectDir: File? = null,  // Python app project directory
        goAppProjectDir: File? = null,      // Go app project directory
        perfConfig: com.webtoapp.core.linux.PerformanceOptimizer.OptimizeConfig? = null,  // Performance optimization config
        onProgress: (Int) -> Unit
    ) {
        logger.log("modifyApk started, encryption=${encryptionConfig.enabled}, abiFilter=${abiFilters.ifEmpty { "all" }}")
        val iconBitmap = iconPath?.let { template.loadBitmap(it) }
            ?: generateDefaultIcon(config.appName, config.themeType)
        var hasConfigFile = false
        var strippedNativeLibSize = 0L // Track total stripped native lib size
        val replacedIconPaths = mutableSetOf<String>() // Track replaced icon paths
        var discoveredOldIconPaths = emptySet<String>() // Old icon paths discovered from ARSC (may be R8-obfuscated)
        
        // Create encryptor (if encryption enabled)
        val assetEncryptor = if (encryptionConfig.enabled && encryptionKey != null) {
            AssetEncryptor(encryptionKey)
        } else null
        
        ZipFile(sourceApk).use { zipIn ->
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                // To satisfy Android R+ requirements, write resources.arsc as first entry
                val entries = zipIn.entries().toList()
                    .sortedWith(compareBy<ZipEntry> { it.name != "resources.arsc" })
                val entryNames = entries.map { it.name }.toSet()

                var processedCount = 0
                
                entries.forEach { entry ->
                    processedCount++
                    onProgress((processedCount * 100) / entries.size)
                    
                    when {
                        // Skip signature files (will re-sign)
                        entry.name.startsWith("META-INF/") && 
                        (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") || 
                         entry.name.endsWith(".DSA") || entry.name == "META-INF/MANIFEST.MF") -> {
                            // Skip
                        }
                        
                        // Skip old splash media files (will re-add later)
                        entry.name.startsWith("assets/splash_media.") -> {
                            AppLogger.d("ApkBuilder", "Skipping old splash media: ${entry.name}")
                        }
                        
                        // Keep adaptive icon definition XMLs (mipmap-anydpi-v26/ic_launcher.xml)!
                        // These XMLs reference @drawable/ic_launcher_foreground, which we replace
                        // at line 843. The XML → foreground pipeline ensures the launcher renders
                        // the icon correctly as an AdaptiveIconDrawable on Android 8+.
                        // 
                        // Previously we stripped these XMLs and replaced with PNGs, but launchers
                        // expect AdaptiveIconDrawable from mipmap resources on API 26+,
                        // causing "Failure retrieving resources" and default icon fallback.
                        
                        // Modify AndroidManifest.xml (modify package name, version, add multi-icons)
                        entry.name == "AndroidManifest.xml" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            // Calculate activity-alias count (multi desktop icons)
                            val aliasCount = config.disguiseConfig?.getAliasCount() ?: 0
                            // Use full AXML modification method
                            val modifiedData = axmlRebuilder.expandAndModifyFull(
                                originalData, 
                                originalPackageName, 
                                config.packageName,
                                config.versionCode,
                                config.versionName,
                                aliasCount,
                                config.appName,
                                config.deepLinkHosts
                            )
                            writeEntryDeflated(zipOut, entry.name, modifiedData)
                            if (aliasCount > 0) {
                                logger.log("Added $aliasCount activity-alias (multi desktop icons)")
                                if (aliasCount >= 100) {
                                    val overheadKb = (aliasCount * 520L) / 1024
                                    val impactLevel = com.webtoapp.core.disguise.DisguiseConfig.assessImpactLevel(aliasCount + 1)
                                    logger.log("⚡ Icon Storm mode: $aliasCount aliases, ~${overheadKb}KB manifest overhead, impact level $impactLevel")
                                }
                            }
                        }
                        
                        // 修改 resources.arsc (修改应用名 + 替换图标路径)
                        // 使用 ArscRebuilder 重建 string pool，支持任意长度应用名
                        // replaceIcons=true 会解析 ARSC 层级结构，定位 R8 混淆后的实际图标路径
                        // Android 11+ requires resources.arsc to be uncompressed and 4-byte aligned
                        entry.name == "resources.arsc" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            val modifiedData = arscRebuilder.rebuildWithNewAppNameAndIcons(
                                originalData,
                                config.appName,
                                replaceIcons = true
                            )
                            // 获取 R8 混淆后的旧图标路径（如 res/BW.xml, res/qx.png 等）
                            discoveredOldIconPaths = arscRebuilder.getLastDiscoveredIconPaths()
                            logger.log("Discovered old icon paths from ARSC: $discoveredOldIconPaths")
                            writeEntryStored(zipOut, entry.name, modifiedData)
                        }
                        
                        // Replace/add config file
                        entry.name == ApkTemplate.CONFIG_PATH -> {
                            hasConfigFile = true
                            writeConfigEntry(zipOut, config, assetEncryptor, encryptionConfig)
                        }
                        
                        // 替换图标 - 同时支持原始路径和 R8 混淆后的路径
                        iconBitmap != null && (isIconEntry(entry.name) || discoveredOldIconPaths.contains(entry.name)) -> {
                            // 使用 createAdaptiveForegroundIcon 遵循 Android Adaptive Icon 规范：
                            // 前景层 108dp，安全区域（完整显示）为中间 72dp（66.67%），
                            // 外围 18dp 作为系统形状遮罩裁剪区域
                            // 432px = 108dp * 4 (xxxhdpi)
                            val iconBytes = template.createAdaptiveForegroundIcon(iconBitmap, 432)
                            writeEntryDeflated(zipOut, entry.name, iconBytes)
                            replacedIconPaths.add(entry.name)
                            AppLogger.d("ApkBuilder", "Replaced icon entry: ${entry.name} (${iconBytes.size} bytes)")
                        }
                        
                        // Filter native libraries (by architecture AND app type)
                        // This is the biggest APK size optimization: strips unused native libs
                        // e.g., libphp.so (28MB) is only needed for WordPress/PHP apps
                        entry.name.startsWith("lib/") -> {
                            val abi = entry.name.removePrefix("lib/").substringBefore("/")
                            val libName = entry.name.substringAfterLast("/")
                            
                            when {
                                // Skip unwanted architecture
                                abiFilters.isNotEmpty() && !abiFilters.contains(abi) -> {
                                    AppLogger.d("ApkBuilder", "Skipping architecture: ${entry.name}")
                                }
                                // Skip native libs not needed for this app type
                                !isRequiredNativeLib(libName, config.appType, config.engineType) -> {
                                    val sizeKb = if (entry.size >= 0) entry.size / 1024 else entry.compressedSize / 1024
                                    AppLogger.d("ApkBuilder", "APK slim: stripped $libName (${sizeKb} KB)")
                                    logger.log("APK slim: stripped $libName (${sizeKb} KB) - not needed for ${config.appType}")
                                    strippedNativeLibSize += if (entry.size >= 0) entry.size else entry.compressedSize
                                }
                                else -> {
                                    copyEntry(zipIn, zipOut, entry)
                                }
                            }
                        }
                        
                        // Strip Kotlin reflection metadata (not needed at runtime)
                        entry.name.startsWith("kotlin/") || entry.name == "DebugProbesKt.bin" -> {
                            // Skip - saves ~50KB total
                        }
                        
                        // Strip editor-only assets (not needed in Shell mode)
                        isEditorOnlyAsset(entry.name, config.appType, config.engineType) -> {
                            AppLogger.d("ApkBuilder", "APK slim: stripped editor asset: ${entry.name}")
                        }
                        
                        // Performance optimization: remove unused resources
                        perfConfig != null && perfConfig.removeUnusedResources &&
                        com.webtoapp.core.linux.PerformanceOptimizer.getRemovableEntries(entry.name, config.appType) -> {
                            AppLogger.d("ApkBuilder", "Perf: removed unused resource: ${entry.name}")
                        }
                        
                        // Performance optimization: optimize assets on-the-fly
                        perfConfig != null && entry.name.startsWith("assets/") && isOptimizableAsset(entry.name) -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            val optimizedData = com.webtoapp.core.linux.PerformanceOptimizer.optimizeBytesForApk(
                                context, entry.name.substringAfterLast("/"), originalData, perfConfig
                            )
                            writeEntryDeflated(zipOut, entry.name, optimizedData)
                            if (optimizedData.size < originalData.size) {
                                AppLogger.d("ApkBuilder", "Perf: optimized ${entry.name}: ${originalData.size} -> ${optimizedData.size}")
                            }
                        }
                        
                        // Copy other files
                        else -> {
                            copyEntry(zipIn, zipOut, entry)
                        }
                    }
                }
                
                // If original APK has no config file, add one
                if (!hasConfigFile) {
                    writeConfigEntry(zipOut, config, assetEncryptor, encryptionConfig)
                }
                
                // Write encryption metadata (if encryption enabled)
                if (encryptionConfig.enabled) {
                    // Use JarSigner's certificate signature hash to ensure same signature for encryption key derivation
                    val signatureHash = signer.getCertificateSignatureHash()
                    encryptedApkBuilder.writeEncryptionMetadata(zipOut, encryptionConfig, config.packageName, signatureHash)
                    logger.log("Encryption metadata written")
                }
                
                // Perform app hardening (if hardening enabled)
                if (hardeningConfig.enabled) {
                    logger.section("App Hardening")
                    logger.log("Hardening level: ${hardeningConfig.hardeningLevel.name}")
                    val hardeningEngine = com.webtoapp.core.hardening.AppHardeningEngine(context)
                    val signatureHash = signer.getCertificateSignatureHash()
                    val hardeningResult = hardeningEngine.performHardening(
                        config = hardeningConfig,
                        zipOut = zipOut,
                        packageName = config.packageName,
                        signatureHash = signatureHash
                    ) { _, hardenText ->
                        logger.log("Hardening: $hardenText")
                    }
                    logger.logKeyValue("hardeningSuccess", hardeningResult.success)
                    logger.logKeyValue("protectionLayers", hardeningResult.stats.totalProtectionLayers)
                    logger.logKeyValue("hardeningTimeMs", hardeningResult.stats.hardeningTimeMs)
                    if (hardeningResult.warnings.isNotEmpty()) {
                        hardeningResult.warnings.forEach { logger.warn("Hardening: $it") }
                    }
                    if (hardeningResult.errors.isNotEmpty()) {
                        hardeningResult.errors.forEach { logger.error("Hardening: $it") }
                    }
                    logger.log("App hardening completed: ${hardeningResult.protectedFeatures.size} features protected")
                }
                
                // Performance optimization: inject performance script into assets
                if (perfConfig != null && perfConfig.injectPerformanceScript) {
                    logger.section("Performance Optimization")
                    val perfScript = com.webtoapp.core.linux.PerformanceOptimizer.generatePerformanceScript()
                    val scriptData = perfScript.toByteArray(Charsets.UTF_8)
                    writeEntryDeflated(zipOut, "assets/wta_perf_optimize.js", scriptData)
                    logger.log("Performance script injected (${scriptData.size} bytes)")
                    logger.log("Perf features: images=${perfConfig.compressImages}, code=${perfConfig.minifyCode}, " +
                        "webp=${perfConfig.convertToWebP}, preload=${perfConfig.injectPreloadHints}, " +
                        "lazy=${perfConfig.injectLazyLoading}, scripts=${perfConfig.optimizeScripts}")
                }
                
                // If have icon but no PNG icon files in APK, add them
                if (iconBitmap != null && replacedIconPaths.isEmpty()) {
                    addIconsToApk(zipOut, iconBitmap)
                    logger.log("Added PNG mipmap icons (no existing PNG icons found in template)")
                } else if (iconBitmap != null) {
                    logger.log("Replaced ${replacedIconPaths.size} existing PNG icon entries")
                }

                // Add foreground PNG icons for templates using adaptive icons
                // Write unconditionally, because release APK's foreground may be compiled to different paths
                if (iconBitmap != null) {
                    addAdaptiveIconPngs(zipOut, iconBitmap, entryNames)
                }
                
                // No longer writing PNG replacements at mipmap-anydpi-v26 paths.
                // The adaptive icon XMLs are kept intact and reference the foreground drawable,
                // which is replaced with the user's icon during the ZIP copy loop above.

                // Embed splash media files
                AppLogger.d("ApkBuilder", "Splash config: splashEnabled=${config.splashEnabled}, splashMediaPath=$splashMediaPath, splashType=${config.splashType}")
                if (config.splashEnabled && splashMediaPath != null) {
                    addSplashMediaToAssets(zipOut, splashMediaPath, config.splashType, assetEncryptor, encryptionConfig)
                } else {
                    AppLogger.w("ApkBuilder", "Skipping splash embed: splashEnabled=${config.splashEnabled}, splashMediaPath=$splashMediaPath")
                }
                
                // Embed status bar background image (if image background configured)
                if (config.statusBarBackgroundType == "IMAGE" && !config.statusBarBackgroundImage.isNullOrEmpty()) {
                    addStatusBarBackgroundToAssets(zipOut, config.statusBarBackgroundImage)
                }
                
                // Embed background music files (common to all types)
                if (config.bgmEnabled && bgmPlaylistPaths.isNotEmpty()) {
                    logger.log("Embedding BGM: ${bgmPlaylistPaths.size} files")
                    addBgmToAssets(zipOut, bgmPlaylistPaths, bgmLrcDataList, assetEncryptor, encryptionConfig)
                }
                
                // === Strategy pattern: app-type-specific content embedding ===
                // Resolve project directory for the current app type
                val projectDir = when (config.appType) {
                    "WORDPRESS" -> wordPressProjectDir
                    "NODEJS_APP" -> nodejsProjectDir
                    "PHP_APP" -> phpAppProjectDir
                    "PYTHON_APP" -> pythonAppProjectDir
                    "GO_APP" -> goAppProjectDir
                    "FRONTEND" -> frontendProjectDir
                    else -> null
                }
                
                val embedder = AppContentEmbedderFactory.create(config.appType)
                if (embedder != null) {
                    val embedCtx = EmbedContext(
                        config = config,
                        logger = logger,
                        encryptor = assetEncryptor,
                        encryptionConfig = encryptionConfig,
                        mediaContentPath = mediaContentPath,
                        htmlFiles = htmlFiles,
                        galleryItems = galleryItems,
                        projectDir = projectDir,
                        fnAddMediaContent = ::addMediaContentToAssets,
                        fnAddHtmlFiles = ::addHtmlFilesToAssets,
                        fnAddGalleryItems = ::addGalleryItemsToAssets,
                        fnAddWordPressFiles = ::addWordPressFilesToAssets,
                        fnAddNodeJsFiles = ::addNodeJsFilesToAssets,
                        fnAddFrontendFiles = ::addFrontendFilesToAssets,
                        fnAddPhpAppFiles = ::addPhpAppFilesToAssets,
                        fnAddPythonAppFiles = ::addPythonAppFilesToAssets,
                        fnAddGoAppFiles = ::addGoAppFilesToAssets
                    )
                    val result = embedder.embed(zipOut, embedCtx)
                    logger.log("Content embedding [${config.appType}]: ${result.message}")
                }
                
                // Inject GeckoView native .so files (when engine type is GECKOVIEW)
                if (config.engineType == "GECKOVIEW") {
                    logger.section("Inject GeckoView Native Libraries")
                    injectGeckoViewNativeLibs(zipOut, abiFilters)
                }
            }
        }
        
        // Log native library stripping summary
        if (strippedNativeLibSize > 0) {
            val savedMb = strippedNativeLibSize / 1024 / 1024
            logger.log("APK slim: total native lib savings: ${savedMb} MB")
            AppLogger.d("ApkBuilder", "APK slim: stripped ${savedMb} MB of unused native libraries")
        }
        
        iconBitmap?.recycle()
    }
    
    /**
     * Check if a native library is required for the given app type and engine type.
     * Used to strip unnecessary native libraries from generated APKs.
     * 
     * Size impact examples:
     * - libphp.so: ~28MB (only for WORDPRESS/PHP_APP)
     * - GeckoView companion libs: ~12MB total (only for GECKOVIEW engine)
     * - libnode_bridge.so: ~11KB (only for NODEJS_APP)
     * 
     * For a basic WEB app, this saves ~40MB from the APK.
     */
    private fun isRequiredNativeLib(libName: String, appType: String, engineType: String): Boolean {
        // Core libraries always needed
        if (libName == "libcrypto_engine.so" || libName == "libc++_shared.so") {
            return true
        }
        
        // APK optimizer — 仅编辑器使用，Shell APK 不需要
        // Crypto optimized — 编辑器打包时用于加速加密，Shell 使用 Java 解密回退
        // Perf engine — 构建时生成性能 JS 并嵌入配置，Shell 不需要原生库
        if (libName == "libapk_optimizer.so" || libName == "libcrypto_optimized.so" || libName == "libperf_engine.so" || libName == "libbrowser_kernel.so") {
            return false
        }
        
        // sys_optimizer — Shell APK 保留: 运行时进行系统级优化 (CPU亲和性, 线程优先级等)
        
        // Hardware control — 仅黑科技功能需要，普通 Shell 不需要
        // 注意: 如果 WebApp 启用了黑科技功能，应该保留
        // 但因为 isRequiredNativeLib 没有 config 上下文，保守保留
        // (C 优化器层会做二次检查)
        
        // PHP binary - only for WordPress and PHP apps
        if (libName == "libphp.so") {
            return appType in setOf("WORDPRESS", "PHP_APP")
        }
        
        // Node.js bridge and runtime - only for Node.js apps
        if (libName == "libnode_bridge.so" || libName == "libnode.so") {
            return appType == "NODEJS_APP"
        }
        
        // Python binary and musl linker - only for Python apps
        if (libName == "libpython3.so" || libName == "libmusl-linker.so") {
            return appType == "PYTHON_APP"
        }
        
        // GeckoView companion libraries - only when using GeckoView engine
        val geckoViewLibs = setOf(
            "libgkcodecs.so",
            "libminidump_analyzer.so",
            "libnss3.so",
            "libfreebl3.so",
            "libsoftokn3.so",
            "liblgpllibs.so"
        )
        if (libName in geckoViewLibs) {
            return engineType == "GECKOVIEW"
        }
        
        // Unknown libraries - keep them to be safe
        return true
    }

    /**
     * Inject GeckoView native .so files into APK
     * Reads cached .so files from EngineFileManager and writes them to lib/{abi}/ in the APK
     */
    private fun injectGeckoViewNativeLibs(
        zipOut: ZipOutputStream,
        abiFilters: List<String>
    ) {
        try {
            val engineFileManager = com.webtoapp.core.engine.download.EngineFileManager(context)
            val nativeLibs = engineFileManager.listEngineNativeLibs(com.webtoapp.core.engine.EngineType.GECKOVIEW)
            
            if (nativeLibs.isEmpty()) {
                logger.warn("GeckoView engine selected but no native libs found! Make sure engine is downloaded.")
                return
            }
            
            var totalInjected = 0
            nativeLibs.forEach { (abi, soFiles) ->
                // Skip ABIs not in the filter (if filter is set)
                if (abiFilters.isNotEmpty() && !abiFilters.contains(abi)) {
                    logger.log("Skipping GeckoView ABI: $abi (not in abiFilters)")
                    return@forEach
                }
                
                soFiles.forEach { soFile ->
                    val entryPath = "lib/$abi/" + soFile.name
                    logger.log("Injecting: $entryPath (" + (soFile.length() / 1024) + " KB)")
                    writeEntryStoredStreaming(zipOut, entryPath, soFile)
                    totalInjected++
                }
            }
            
            logger.logKeyValue("geckoNativeLibsInjected", totalInjected)
        } catch (e: Exception) {
            logger.error("Failed to inject GeckoView native libs", e)
        }
    }
    
    /**
     * Add splash media file to assets directory
     * 
     * Important: Must use STORED (uncompressed) storage!
     * Because AssetManager.openFd() only supports uncompressed asset files.
     * If using DEFLATED compression, openFd() will throw FileNotFoundException.
     * 
     * Note: If encryption enabled, cannot use openFd(), need to decrypt to temp file first
     */
    private fun addSplashMediaToAssets(
        zipOut: ZipOutputStream,
        mediaPath: String,
        splashType: String,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed splash media: path=$mediaPath, type=$splashType, encrypt=${encryptionConfig.encryptSplash}")
        
        val mediaFile = File(mediaPath)
        if (!mediaFile.exists()) {
            AppLogger.e("ApkBuilder", "Splash media file does not exist: $mediaPath")
            return
        }
        
        if (!mediaFile.canRead()) {
            AppLogger.e("ApkBuilder", "Splash media file cannot be read: $mediaPath")
            return
        }
        
        val fileSize = mediaFile.length()
        if (fileSize == 0L) {
            AppLogger.e("ApkBuilder", "Splash media file is empty: $mediaPath")
            return
        }

        // Determine filename based on type
        val extension = if (splashType == "VIDEO") "mp4" else "png"
        val assetPath = "splash_media.$extension"
        val isVideo = splashType == "VIDEO"

        try {
            // Large file threshold: 10MB, use streaming write to avoid OOM
            val largeFileThreshold = 10 * 1024 * 1024L
            
            if (encryptionConfig.encryptSplash && encryptor != null) {
                // Encrypt splash screen
                if (isVideo && fileSize > largeFileThreshold) {
                    AppLogger.d("ApkBuilder", "Splash large video encryption mode: ${fileSize / 1024 / 1024} MB")
                    val encryptedData = encryptLargeFile(mediaFile, assetPath, encryptor)
                    writeEntryDeflated(zipOut, "assets/${assetPath}.enc", encryptedData)
                    AppLogger.d("ApkBuilder", "Splash media encrypted and embedded: assets/${assetPath}.enc (${encryptedData.size} bytes)")
                } else {
                    val mediaBytes = mediaFile.readBytes()
                    val encryptedData = encryptor.encrypt(mediaBytes, assetPath)
                    writeEntryDeflated(zipOut, "assets/${assetPath}.enc", encryptedData)
                    AppLogger.d("ApkBuilder", "Splash media encrypted and embedded: assets/${assetPath}.enc (${encryptedData.size} bytes)")
                }
            } else {
                // Non-encrypted mode
                if (isVideo && fileSize > largeFileThreshold) {
                    // Large video: use streaming write to avoid OOM
                    AppLogger.d("ApkBuilder", "Splash large video streaming write mode: ${fileSize / 1024 / 1024} MB")
                    writeEntryStoredStreaming(zipOut, "assets/$assetPath", mediaFile)
                } else {
                    // Small file or image: normal read
                    val mediaBytes = mediaFile.readBytes()
                    writeEntryStoredSimple(zipOut, "assets/$assetPath", mediaBytes)
                    AppLogger.d("ApkBuilder", "Splash media embedded(STORED): assets/$assetPath (${mediaBytes.size} bytes)")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to embed splash media: ${e.message}", e)
        }
    }
    
    /**
     * Add status bar background image to assets directory
     */
    private fun addStatusBarBackgroundToAssets(
        zipOut: ZipOutputStream,
        imagePath: String
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed status bar background: path=$imagePath")
        
        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            AppLogger.e("ApkBuilder", "Status bar background image does not exist: $imagePath")
            return
        }
        
        if (!imageFile.canRead()) {
            AppLogger.e("ApkBuilder", "Status bar background image cannot be read: $imagePath")
            return
        }
        
        try {
            val imageBytes = imageFile.readBytes()
            if (imageBytes.isEmpty()) {
                AppLogger.e("ApkBuilder", "Status bar background image is empty: $imagePath")
                return
            }
            
            // Use DEFLATED compression (image doesn't need openFd)
            writeEntryDeflated(zipOut, "assets/statusbar_background.png", imageBytes)
            AppLogger.d("ApkBuilder", "Status bar background embedded: assets/statusbar_background.png (${imageBytes.size} bytes)")
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to embed status bar background: ${e.message}", e)
        }
    }
    
    /**
     * Write entry (using STORED uncompressed format, simplified version)
     * For splash media etc. that need to be read by AssetManager.openFd()
     */
    private fun writeEntryStoredSimple(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        ZipUtils.writeEntryStoredSimple(zipOut, name, data)
    }
    
    /**
     * Streaming write large file (using STORED uncompressed format)
     * For large video files, avoid OOM
     * Two steps: first calculate CRC, then write data
     */
    private fun writeEntryStoredStreaming(zipOut: ZipOutputStream, name: String, file: File) {
        ZipUtils.writeEntryStoredStreaming(zipOut, name, file)
    }
    
    /**
     * Add media app content to assets directory
     * Use STORED (uncompressed) format to support AssetManager.openFd()
     * 
     * Note: If encryption enabled, cannot use openFd(), need to decrypt to temp file first
     */
    private fun addMediaContentToAssets(
        zipOut: ZipOutputStream,
        mediaPath: String,
        isVideo: Boolean,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed media content: path=$mediaPath, isVideo=$isVideo, encrypt=${encryptionConfig.encryptMedia}")
        
        val mediaFile = File(mediaPath)
        if (!mediaFile.exists()) {
            AppLogger.e("ApkBuilder", "Media file does not exist: $mediaPath")
            return
        }
        
        if (!mediaFile.canRead()) {
            AppLogger.e("ApkBuilder", "Media file cannot be read: $mediaPath")
            return
        }
        
        val fileSize = mediaFile.length()
        if (fileSize == 0L) {
            AppLogger.e("ApkBuilder", "Media file is empty: $mediaPath")
            return
        }

        // Determine filename based on type
        val extension = if (isVideo) "mp4" else "png"
        val assetName = "media_content.$extension"

        try {
            // Large file threshold: 10MB, use streaming write to avoid OOM
            val largeFileThreshold = 10 * 1024 * 1024L
            
            if (encryptionConfig.encryptMedia && encryptor != null) {
                // Encrypt media content (large file chunked encryption)
                if (fileSize > largeFileThreshold) {
                    AppLogger.d("ApkBuilder", "Large file encryption mode: ${fileSize / 1024 / 1024} MB")
                    // Large file: chunked read and encrypt
                    val encryptedData = encryptLargeFile(mediaFile, assetName, encryptor)
                    writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                    AppLogger.d("ApkBuilder", "Media content encrypted and embedded: assets/${assetName}.enc (${encryptedData.size} bytes)")
                } else {
                    val mediaBytes = mediaFile.readBytes()
                    val encryptedData = encryptor.encrypt(mediaBytes, assetName)
                    writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                    AppLogger.d("ApkBuilder", "Media content encrypted and embedded: assets/${assetName}.enc (${encryptedData.size} bytes)")
                }
            } else {
                // Non-encrypted mode
                if (fileSize > largeFileThreshold) {
                    // Large file: use streaming write to avoid OOM
                    AppLogger.d("ApkBuilder", "Large file streaming write mode: ${fileSize / 1024 / 1024} MB")
                    writeEntryStoredStreaming(zipOut, "assets/$assetName", mediaFile)
                } else {
                    // Small file: normal read
                    val mediaBytes = mediaFile.readBytes()
                    writeEntryStoredSimple(zipOut, "assets/$assetName", mediaBytes)
                    AppLogger.d("ApkBuilder", "Media content embedded(STORED): assets/$assetName (${mediaBytes.size} bytes)")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to embed media content", e)
        }
    }
    
    /**
     * Add gallery media items to assets/gallery directory
     * Each item is saved as gallery/item_X.{png|mp4}
     * Thumbnails are saved as gallery/thumb_X.jpg
     */
    private fun addGalleryItemsToAssets(
        zipOut: ZipOutputStream,
        galleryItems: List<com.webtoapp.data.model.GalleryItem>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed ${galleryItems.size} gallery items, encrypt=${encryptionConfig.encryptMedia}")
        
        galleryItems.forEachIndexed { index, item ->
            try {
                val mediaFile = File(item.path)
                if (!mediaFile.exists()) {
                    AppLogger.w("ApkBuilder", "Gallery item file not found: ${item.path}")
                    return@forEachIndexed
                }
                if (!mediaFile.canRead()) {
                    AppLogger.w("ApkBuilder", "Gallery item file cannot be read: ${item.path}")
                    return@forEachIndexed
                }
                
                val ext = if (item.type == com.webtoapp.data.model.GalleryItemType.VIDEO) "mp4" else "png"
                val assetName = "gallery/item_$index.$ext"
                val isVideo = item.type == com.webtoapp.data.model.GalleryItemType.VIDEO
                val fileSize = mediaFile.length()
                val largeFileThreshold = 10 * 1024 * 1024L
                
                // Embed media file
                if (encryptionConfig.encryptMedia && encryptor != null) {
                    if (isVideo && fileSize > largeFileThreshold) {
                        val encryptedData = encryptLargeFile(mediaFile, assetName, encryptor)
                        writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                    } else {
                        val data = mediaFile.readBytes()
                        val encrypted = encryptor.encrypt(data, assetName)
                        writeEntryDeflated(zipOut, "assets/${assetName}.enc", encrypted)
                    }
                    AppLogger.d("ApkBuilder", "Gallery item encrypted and embedded: assets/${assetName}.enc")
                } else {
                    if (isVideo && fileSize > largeFileThreshold) {
                        writeEntryStoredStreaming(zipOut, "assets/$assetName", mediaFile)
                    } else {
                        writeEntryStoredSimple(zipOut, "assets/$assetName", mediaFile.readBytes())
                    }
                    AppLogger.d("ApkBuilder", "Gallery item embedded(STORED): assets/$assetName (${fileSize / 1024} KB)")
                }
                
                // Embed thumbnail (if exists)
                item.thumbnailPath?.let { thumbPath ->
                    val thumbFile = File(thumbPath)
                    if (thumbFile.exists() && thumbFile.canRead()) {
                        val thumbAssetName = "gallery/thumb_$index.jpg"
                        val thumbBytes = thumbFile.readBytes()
                        if (encryptionConfig.encryptMedia && encryptor != null) {
                            val encryptedThumb = encryptor.encrypt(thumbBytes, thumbAssetName)
                            writeEntryDeflated(zipOut, "assets/${thumbAssetName}.enc", encryptedThumb)
                        } else {
                            writeEntryDeflated(zipOut, "assets/$thumbAssetName", thumbBytes)
                        }
                        AppLogger.d("ApkBuilder", "Gallery thumbnail embedded: assets/$thumbAssetName")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("ApkBuilder", "Failed to embed gallery item ${item.path}", e)
            }
        }
    }
    
    /**
     * Add WordPress project files + PHP binary to assets
     * - WordPress files go to assets/wordpress/
     * - PHP binary goes to assets/php/{abi}/php
     */
    private fun addWordPressFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {
        AppLogger.d("ApkBuilder", "Embedding WordPress files from: ${projectDir.absolutePath}")
        
        var fileCount = 0
        var totalSize = 0L
        
        // 1. Embed WordPress project files recursively
        fun addDirRecursive(dir: File, basePath: String) {
            dir.listFiles()?.forEach { file ->
                val relativePath = "$basePath/${file.name}"
                if (file.isDirectory) {
                    addDirRecursive(file, relativePath)
                } else {
                    try {
                        val assetPath = "assets/wordpress$relativePath"
                        // Use DEFLATED for text files, STORED for binary
                        if (isTextFile(file.name)) {
                            writeEntryDeflated(zipOut, assetPath, file.readBytes())
                        } else {
                            writeEntryStoredSimple(zipOut, assetPath, file.readBytes())
                        }
                        fileCount++
                        totalSize += file.length()
                    } catch (e: Exception) {
                        AppLogger.w("ApkBuilder", "Failed to embed WordPress file: ${file.absolutePath}", e)
                    }
                }
            }
        }
        addDirRecursive(projectDir, "")
        logger.logKeyValue("wordpressFilesEmbedded", fileCount)
        logger.logKeyValue("wordpressTotalSize", "${totalSize / 1024} KB")
        
        // 2. Embed PHP binary
        // Write to both lib/{abi}/libphp.so (native library, SELinux apk_data_file) and
        // assets/php/{abi}/php (backward compat for pre-Android 15 shells)
        val phpBinary = resolvePhpBinary()
        if (phpBinary != null && phpBinary.canRead()) {
            try {
                val abi = com.webtoapp.core.wordpress.WordPressDependencyManager.getDeviceAbi()
                // Native library path — extracted to nativeLibraryDir on install (Android 15+ safe)
                writeEntryStoredStreaming(zipOut, "lib/$abi/libphp.so", phpBinary)
                logger.log("PHP binary injected as native lib: lib/$abi/libphp.so (${phpBinary.length() / 1024} KB)")
                // Legacy assets path — backward compat
                writeEntryStoredSimple(zipOut, "assets/php/$abi/php", phpBinary.readBytes())
                logger.log("PHP binary also embedded as asset: assets/php/$abi/php")
            } catch (e: Exception) {
                logger.error("Failed to embed PHP binary", e)
            }
        } else {
            logger.warn("PHP binary not found")
        }
    }
    
    /**
     * Add Node.js project files + Node binary to assets
     * - Project files go to assets/nodejs_app/
     * - Node binary goes to assets/node/{abi}/node
     */
    private fun addNodeJsFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {
        // 1. Embed project files recursively
        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.nodeJsConfig(), logger)
        
        // 2. Embed Node.js binary as native library (shared library for JNI dlopen)
        // Placed in lib/{abi}/libnode.so so Android extracts it to nativeLibraryDir on install.
        // NodeBridge (libnode_bridge.so) will dlopen this and call node::Start() via JNI.
        val nodeDir = com.webtoapp.core.nodejs.NodeDependencyManager.getNodeDir(context)
        val nodeBinary = File(nodeDir, com.webtoapp.core.nodejs.NodeDependencyManager.NODE_BINARY_NAME)
        if (nodeBinary.exists() && nodeBinary.canRead()) {
            try {
                val abi = nodeDir.name // e.g. "arm64-v8a"
                // Native library path — extracted to nativeLibraryDir on install (Android 15+ safe)
                val nodeLibPath = "lib/$abi/${com.webtoapp.core.nodejs.NodeDependencyManager.NODE_BINARY_NAME}"
                // Node binary is large (~40MB), use streaming write
                writeEntryStoredStreaming(zipOut, nodeLibPath, nodeBinary)
                logger.log("Node.js binary embedded as native lib: $nodeLibPath (${nodeBinary.length() / 1024} KB)")
            } catch (e: Exception) {
                logger.error("Failed to embed Node.js binary", e)
            }
        } else {
            logger.warn("Node.js binary not found in cache: ${nodeBinary.absolutePath}")
        }
    }
    
    /**
     * Add PHP app project files + PHP binary to assets
     * - Project files go to assets/php_app/
     * - PHP binary goes to assets/php/{abi}/php (reuse WordPress PHP)
     */
    private fun addPhpAppFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {
        // Embed project files recursively
        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.phpConfig(), logger)
        
        // Embed PHP binary (reuse WordPress PHP)
        // Write to both lib/{abi}/libphp.so (native library, SELinux apk_data_file) and
        // assets/php/{abi}/php (backward compat for pre-Android 15 shells)
        val phpBinary = resolvePhpBinary()
        if (phpBinary != null && phpBinary.canRead()) {
            try {
                val abi = com.webtoapp.core.wordpress.WordPressDependencyManager.getDeviceAbi()
                // Native library path — extracted to nativeLibraryDir on install (Android 15+ safe)
                writeEntryStoredStreaming(zipOut, "lib/$abi/libphp.so", phpBinary)
                logger.log("PHP binary injected as native lib: lib/$abi/libphp.so (${phpBinary.length() / 1024} KB)")
                // Legacy assets path — backward compat for shells built before this change
                writeEntryStoredSimple(zipOut, "assets/php/$abi/php", phpBinary.readBytes())
                logger.log("PHP binary also embedded as asset: assets/php/$abi/php")
            } catch (e: Exception) {
                logger.error("Failed to embed PHP binary for PHP app", e)
            }
        } else {
            logger.warn("PHP binary not found")
        }
    }
    
    /**
     * Resolve the PHP binary file: prefer nativeLibraryDir (bundled via jniLibs), fallback to download cache.
     */
    private fun resolvePhpBinary(): File? {
        // 1. nativeLibraryDir (bundled as libphp.so in the builder app itself)
        val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        if (nativePhp.exists()) {
            AppLogger.d("ApkBuilder", "Using nativeLibraryDir PHP: ${nativePhp.absolutePath}")
            return nativePhp
        }
        // 2. Download cache
        val phpDir = com.webtoapp.core.wordpress.WordPressDependencyManager.getPhpDir(context)
        val downloaded = File(phpDir, "php")
        if (downloaded.exists()) {
            AppLogger.d("ApkBuilder", "Using downloaded PHP: ${downloaded.absolutePath}")
            return downloaded
        }
        AppLogger.w("ApkBuilder", "PHP binary not found in nativeLibraryDir or download cache")
        return null
    }
    
    
    /**
     * Add Python app project files + Python binary to assets
     */
    private fun addPythonAppFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {
        // ★ 预安装 Python 依赖到 .pypackages（构建时安装，避免运行时 pip install 失败）
        // 在 Android 上 pip install 经常因 musl linker 问题失败，
        // 所以在构建机（也是 Android，但有完整的 Python 运行时）上预装依赖，
        // 然后把 .pypackages 一起打包进 APK assets 中。
        val reqFile = File(projectDir, "requirements.txt")
        val sitePackages = File(projectDir, ".pypackages")
        if (reqFile.exists() && (!sitePackages.exists() || sitePackages.listFiles().isNullOrEmpty())) {
            // 检查 musl linker 是否在 nativeLibraryDir（SELinux 允许执行）
            // 在 app data 目录的 musl linker 会被 SELinux 阻止执行 (execute_no_trans denied)
            val nativeMuslLinker = File(context.applicationInfo.nativeLibraryDir, "libmusl-linker.so")
            if (nativeMuslLinker.exists() && nativeMuslLinker.canExecute()) {
                logger.log("Pre-installing Python dependencies for APK bundling...")
                try {
                    val installed = kotlinx.coroutines.runBlocking {
                        com.webtoapp.core.python.PythonDependencyManager.installRequirements(context, projectDir) { line ->
                            AppLogger.d("ApkBuilder", "[pip-preinstall] $line")
                        }
                    }
                    if (installed) {
                        val pkgCount = sitePackages.listFiles()?.size ?: 0
                        logger.log("Python dependencies pre-installed: $pkgCount packages in .pypackages")
                    } else {
                        logger.warn("Python dependency pre-install failed - APK will attempt pip install at runtime")
                    }
                } catch (e: Exception) {
                    logger.warn("Python dependency pre-install exception: ${e.message}")
                }
            } else {
                // musl linker 不在 nativeLibraryDir，无法在构建时预安装
                // 导出的 APK 会在运行时自行安装依赖（musl linker 在 APK 的 nativeLibraryDir 中可执行）
                logger.log("Skipping pre-install: musl linker not executable from current context (SELinux). Runtime will install deps.")
            }
        } else if (sitePackages.exists() && !sitePackages.listFiles().isNullOrEmpty()) {
            logger.log("Python .pypackages already exists (${sitePackages.listFiles()?.size} packages), skipping pre-install")
        }
        
        // Embed project files recursively (including .pypackages if it exists)
        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.pythonConfig(), logger)
        
        // ★ 嵌入 sitecustomize.py — Python 启动时自动加载的修复脚本
        // 修复两个关键的 Android 运行时问题:
        // 1. importlib.metadata.PackageNotFoundError - --target 安装的包缺少 .dist-info
        // 2. Flask 端口硬编码 - app.py 中 app.run(port=5000) 与 WebToApp 分配的端口不匹配
        val sitecustomizeContent = """
import os, sys, builtins

# === 1. Patch importlib.metadata for --target installed packages ===
try:
    import importlib.metadata
    _orig_version = importlib.metadata.version
    def _patched_version(name):
        try:
            return _orig_version(name)
        except importlib.metadata.PackageNotFoundError:
            try:
                mod = __import__(name.replace('-', '_'))
                if hasattr(mod, '__version__'):
                    return mod.__version__
            except (ImportError, Exception):
                pass
            return "0.0.0"
    importlib.metadata.version = _patched_version
except Exception:
    pass

# === 2. Patch Flask to use PORT env var ===
_w2a_port = int(os.environ.get('PORT', '5000'))
_orig_builtins_import = builtins.__import__
_flask_patched = False

def _w2a_import(name, *args, **kwargs):
    global _flask_patched
    result = _orig_builtins_import(name, *args, **kwargs)
    if name == 'flask' and not _flask_patched:
        _flask_patched = True
        try:
            _orig_run = result.Flask.run
            def _new_run(self, host=None, port=None, **kw):
                kw.pop('debug', None)
                _orig_run(self, host='127.0.0.1', port=_w2a_port, debug=False, **kw)
            result.Flask.run = _new_run
        except Exception:
            pass
    return result

builtins.__import__ = _w2a_import
""".trimIndent()
        try {
            ZipUtils.writeEntryDeflated(zipOut, "assets/python_app/sitecustomize.py", sitecustomizeContent.toByteArray())
            logger.log("Embedded sitecustomize.py for Android runtime fixes (metadata + port)")
        } catch (e: Exception) {
            logger.warn("Failed to embed sitecustomize.py: ${e.message}")
        }
        
        // Embed Python binary (from cache) — prefer python3.12 (real binary) over python3 (symlink)
        val pythonHome = com.webtoapp.core.python.PythonDependencyManager.getPythonDir(context)
        var pythonBinary312 = File(pythonHome, "bin/python3.12")
        var pythonBinary3 = File(pythonHome, "bin/python3")
        var pythonBinary = when {
            pythonBinary312.exists() && pythonBinary312.length() > 1024 * 1024 -> pythonBinary312
            pythonBinary3.exists() && pythonBinary3.length() > 1024 * 1024 -> pythonBinary3
            else -> null
        }
        
        // ★ 如果 Python 二进制不存在，自动下载 Python 运行时
        if (pythonBinary == null) {
            logger.warn("Python binary not found locally, attempting auto-download...")
            try {
                val downloadSuccess = kotlinx.coroutines.runBlocking {
                    com.webtoapp.core.python.PythonDependencyManager.downloadPythonRuntime(context)
                }
                if (downloadSuccess) {
                    logger.log("Python runtime downloaded successfully")
                    // 重新检查
                    pythonBinary312 = File(pythonHome, "bin/python3.12")
                    pythonBinary3 = File(pythonHome, "bin/python3")
                    pythonBinary = when {
                        pythonBinary312.exists() && pythonBinary312.length() > 1024 * 1024 -> pythonBinary312
                        pythonBinary3.exists() && pythonBinary3.length() > 1024 * 1024 -> pythonBinary3
                        else -> null
                    }
                } else {
                    logger.error("Python runtime download failed - exported APK will not have Python interpreter!")
                }
            } catch (e: Exception) {
                logger.error("Failed to auto-download Python runtime: ${e.message}", e)
            }
        }
        
        val abi = com.webtoapp.core.wordpress.WordPressDependencyManager.getDeviceAbi()
        if (pythonBinary != null && pythonBinary.canRead()) {
            try {
                // Native library path — auto-extracted to nativeLibraryDir (Android 15+ SELinux safe)
                writeEntryStoredStreaming(zipOut, "lib/$abi/libpython3.so", pythonBinary)
                logger.log("Python binary embedded as native lib: lib/$abi/libpython3.so (${pythonBinary.length() / 1024} KB, src=${pythonBinary.name})")
                // Legacy assets path — backward compat
                writeEntryStoredSimple(zipOut, "assets/python/$abi/python3", pythonBinary.readBytes())
            } catch (e: Exception) {
                logger.error("Failed to embed Python binary", e)
            }
        } else {
            logger.error("⚠️ CRITICAL: Python binary not available! The exported APK will NOT be able to run Python apps. Please ensure Python runtime is downloaded in WebToApp settings.")
            logger.warn("Python binary not found or too small: python3.12=${pythonBinary312.let { "${it.exists()}/${it.length()}" }}, python3=${pythonBinary3.let { "${it.exists()}/${it.length()}" }}")
        }
        
        // Embed musl dynamic linker (required to execute musl-linked Python on Android)
        val muslLinkerName = com.webtoapp.core.python.PythonDependencyManager.getMuslLinkerName(abi)
        val muslLinkerFile = File(pythonHome, "lib/$muslLinkerName")
        if (muslLinkerFile.exists() && muslLinkerFile.canRead()) {
            try {
                writeEntryStoredStreaming(zipOut, "lib/$abi/libmusl-linker.so", muslLinkerFile)
                logger.log("musl linker embedded as native lib: lib/$abi/libmusl-linker.so (${muslLinkerFile.length() / 1024} KB)")
            } catch (e: Exception) {
                logger.error("Failed to embed musl linker", e)
            }
        } else {
            logger.warn("musl linker not found: ${muslLinkerFile.absolutePath} - Python may not execute in exported APK")
        }
        
        // Embed Python standard library into assets (required for Python to function)
        val pythonLibDir = File(pythonHome, "lib")
        RuntimeAssetEmbedder.embedPythonStdlib(zipOut, pythonLibDir, logger)
    }
    
    
    /**
     * Add Go app binary + static files to assets
     */
    private fun addGoAppFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {
        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.goConfig(), logger)
    }
    
    
    /**
     * Add frontend project files to assets/frontend_app/
     * Embeds entire project directory recursively, excluding build artifacts
     */
    private fun addFrontendFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>
    ) {
        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.frontendConfig(), logger)
    }
    
    /**
     * Encrypt large file.
     * AES-GCM requires full plaintext for authentication tag, so true streaming
     * is not possible. We read the file once to minimize peak memory (single copy
     * instead of the previous double-copy via ByteArrayOutputStream).
     */
    private fun encryptLargeFile(file: File, assetName: String, encryptor: AssetEncryptor): ByteArray {
        val fileSize = file.length()
        val maxEncryptSize = 100L * 1024 * 1024 // 100MB
        if (fileSize > maxEncryptSize) {
            AppLogger.w("ApkBuilder", "WARNING: Encrypting very large file ($assetName, ${fileSize / 1024 / 1024}MB). " +
                "May cause high memory usage. Consider disabling encryption for large media files.")
        }
        val mediaBytes = file.readBytes()
        return encryptor.encrypt(mediaBytes, assetName)
    }
    
    /**
     * Check if text file (can be compressed)
     */
    private fun isTextFile(fileName: String): Boolean {
        return TextFileClassifier.isTextFile(fileName)
    }
    
    /**
     * Add background music files to assets/bgm directory
     * Use STORED (uncompressed) format to support AssetManager.openFd()
     * 
     * Note: If encryption enabled, cannot use openFd(), need to decrypt to temp file first
     */
    private fun addBgmToAssets(
        zipOut: ZipOutputStream,
        bgmPaths: List<String>,
        lrcDataList: List<LrcData?>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed ${bgmPaths.size} BGM files, encrypt=${encryptionConfig.encryptBgm}")
        
        bgmPaths.forEachIndexed { index, bgmPath ->
            try {
                val assetName = "bgm/bgm_$index.mp3"
                var bgmBytes: ByteArray? = null
                
                val bgmFile = File(bgmPath)
                if (!bgmFile.exists()) {
                    // Try to handle asset:/// path
                    if (bgmPath.startsWith("asset:///")) {
                        val assetPath = bgmPath.removePrefix("asset:///")
                        bgmBytes = context.assets.open(assetPath).use { it.readBytes() }
                    } else {
                        AppLogger.e("ApkBuilder", "BGM file does not exist: $bgmPath")
                        return@forEachIndexed
                    }
                } else {
                    if (!bgmFile.canRead()) {
                        AppLogger.e("ApkBuilder", "BGM file cannot be read: $bgmPath")
                        return@forEachIndexed
                    }
                    
                    bgmBytes = bgmFile.readBytes()
                    if (bgmBytes.isEmpty()) {
                        AppLogger.e("ApkBuilder", "BGM file is empty: $bgmPath")
                        return@forEachIndexed
                    }
                }
                
                if (bgmBytes != null) {
                    if (encryptionConfig.encryptBgm && encryptor != null) {
                        // Encrypt BGM
                        val encryptedData = encryptor.encrypt(bgmBytes, assetName)
                        writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                        AppLogger.d("ApkBuilder", "BGM encrypted and embedded: assets/${assetName}.enc (${encryptedData.size} bytes)")
                    } else {
                        // Use STORED (uncompressed) format
                        writeEntryStoredSimple(zipOut, "assets/$assetName", bgmBytes)
                        AppLogger.d("ApkBuilder", "BGM embedded(STORED): assets/$assetName (${bgmBytes.size} bytes)")
                    }
                }
                
                // Embed lyrics file (if exists) - lyrics files are small, can be encrypted
                val lrcData = lrcDataList.getOrNull(index)
                if (lrcData != null && lrcData.lines.isNotEmpty()) {
                    val lrcContent = convertLrcDataToLrcString(lrcData)
                    val lrcAssetName = "bgm/bgm_$index.lrc"
                    val lrcBytes = lrcContent.toByteArray(Charsets.UTF_8)
                    
                    if (encryptionConfig.encryptBgm && encryptor != null) {
                        val encryptedLrc = encryptor.encrypt(lrcBytes, lrcAssetName)
                        writeEntryDeflated(zipOut, "assets/${lrcAssetName}.enc", encryptedLrc)
                        AppLogger.d("ApkBuilder", "LRC encrypted and embedded: assets/${lrcAssetName}.enc")
                    } else {
                        writeEntryDeflated(zipOut, "assets/$lrcAssetName", lrcBytes)
                        AppLogger.d("ApkBuilder", "LRC embedded: assets/$lrcAssetName")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("ApkBuilder", "Failed to embed BGM: $bgmPath", e)
            }
        }
    }
    
    /**
     * Add HTML files to assets/html directory
     * 
     * Two modes:
     * 1. SIMPLE mode (legacy): For simple projects (1 HTML + ≤1 CSS + ≤1 JS, no WASM/modules),
     *    inline CSS and JS into the HTML file for simplicity.
     * 2. PRESERVE mode (new): For complex projects (React, Vue, WASM, ES modules, multiple JS chunks),
     *    embed all files as separate assets preserving their relative directory structure.
     *    This prevents breaking module imports, dynamic imports, WASM loading, and chunk references.
     * 
     * @return Number of successfully embedded files
     */
    private fun addHtmlFilesToAssets(
        zipOut: ZipOutputStream,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ): Int {
        AppLogger.d("ApkBuilder", "Preparing to embed ${htmlFiles.size} HTML project files")
        
        // Print all file paths for debugging
        htmlFiles.forEachIndexed { index, file ->
            AppLogger.d("ApkBuilder", "  [$index] name=${file.name}, path=${file.path}, type=${file.type}")
        }
        
        // Categorize files
        val htmlFilesList = htmlFiles.filter { 
            it.type == com.webtoapp.data.model.HtmlFileType.HTML || 
            it.name.endsWith(".html", ignoreCase = true) || 
            it.name.endsWith(".htm", ignoreCase = true)
        }
        val cssFilesList = htmlFiles.filter { 
            it.type == com.webtoapp.data.model.HtmlFileType.CSS || 
            it.name.endsWith(".css", ignoreCase = true)
        }
        val jsFilesList = htmlFiles.filter { 
            it.type == com.webtoapp.data.model.HtmlFileType.JS || 
            it.name.endsWith(".js", ignoreCase = true) ||
            it.name.endsWith(".mjs", ignoreCase = true)
        }
        val otherFiles = htmlFiles.filter { file ->
            file !in htmlFilesList && file !in cssFilesList && file !in jsFilesList
        }
        
        AppLogger.d("ApkBuilder", "File categories: HTML=${htmlFilesList.size}, CSS=${cssFilesList.size}, JS=${jsFilesList.size}, Other=${otherFiles.size}")
        
        // ★ Detect complex project — if so, skip inlining and preserve file structure
        val isComplexProject = isComplexHtmlProject(htmlFiles, htmlFilesList, jsFilesList, otherFiles)
        
        if (isComplexProject) {
            AppLogger.i("ApkBuilder", "Complex project detected (React/WASM/ES modules) — using PRESERVE mode (no inlining)")
            return addHtmlFilesPreserveStructure(zipOut, htmlFiles, encryptor, encryptionConfig)
        }
        
        // ── SIMPLE mode: inline CSS/JS into HTML (legacy behavior for simple projects) ──
        AppLogger.d("ApkBuilder", "Simple project — using INLINE mode")
        
        var successCount = 0
        
        // Read CSS content (with correct encoding)
        val cssContent = cssFilesList.mapNotNull { cssFile ->
            try {
                val file = File(cssFile.path)
                if (file.exists() && file.canRead()) {
                    val encoding = detectFileEncoding(file)
                    com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(file, encoding)
                } else null
            } catch (e: Exception) {
                AppLogger.e("ApkBuilder", "Failed to read CSS file: ${cssFile.path}", e)
                null
            }
        }.joinToString("\n\n")
        
        // Read JS content (with correct encoding)
        val jsContent = jsFilesList.mapNotNull { jsFile ->
            try {
                val file = File(jsFile.path)
                if (file.exists() && file.canRead()) {
                    val encoding = detectFileEncoding(file)
                    com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(file, encoding)
                } else null
            } catch (e: Exception) {
                AppLogger.e("ApkBuilder", "Failed to read JS file: ${jsFile.path}", e)
                null
            }
        }.joinToString("\n\n")
        
        AppLogger.d("ApkBuilder", "CSS content length: ${cssContent.length}, JS content length: ${jsContent.length}")
        
        // Handle HTML files using HtmlProjectProcessor
        htmlFilesList.forEach { htmlFile ->
            try {
                val sourceFile = File(htmlFile.path)
                AppLogger.d("ApkBuilder", "Processing HTML file: ${htmlFile.path}")
                
                if (!sourceFile.exists()) {
                    AppLogger.e("ApkBuilder", "HTML file does not exist: ${htmlFile.path}")
                    return@forEach
                }
                
                if (!sourceFile.canRead()) {
                    AppLogger.e("ApkBuilder", "HTML file cannot be read: ${htmlFile.path}")
                    return@forEach
                }
                
                // Read HTML with correct encoding
                val encoding = detectFileEncoding(sourceFile)
                var htmlContent = com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(sourceFile, encoding)
                
                if (htmlContent.isEmpty()) {
                    AppLogger.w("ApkBuilder", "HTML file content is empty: ${htmlFile.path}")
                    return@forEach
                }
                
                // Process HTML content using HtmlProjectProcessor
                htmlContent = com.webtoapp.util.HtmlProjectProcessor.processHtmlContent(
                    htmlContent = htmlContent,
                    cssContent = cssContent.takeIf { it.isNotBlank() },
                    jsContent = jsContent.takeIf { it.isNotBlank() },
                    fixPaths = true
                )
                
                // Save to assets/html/ directory
                val assetPath = "assets/html/${htmlFile.name}"
                val htmlBytes = htmlContent.toByteArray(Charsets.UTF_8)
                
                if (encryptionConfig.encryptHtml && encryptor != null) {
                    // Encrypt HTML file
                    val encryptedData = encryptor.encrypt(htmlBytes, "html/${htmlFile.name}")
                    writeEntryDeflated(zipOut, "${assetPath}.enc", encryptedData)
                    AppLogger.d("ApkBuilder", "HTML file encrypted and embedded: ${assetPath}.enc (${encryptedData.size} bytes)")
                } else {
                    writeEntryDeflated(zipOut, assetPath, htmlBytes)
                    AppLogger.d("ApkBuilder", "HTML file embedded(inline CSS/JS): $assetPath (${htmlContent.length} bytes)")
                }
                successCount++
            } catch (e: Exception) {
                AppLogger.e("ApkBuilder", "Failed to embed HTML file: ${htmlFile.path}", e)
            }
        }
        
        // Handle other files (images, fonts, etc.)
        otherFiles.forEach { otherFile ->
            try {
                val sourceFile = File(otherFile.path)
                if (sourceFile.exists() && sourceFile.canRead()) {
                    val fileBytes = sourceFile.readBytes()
                    if (fileBytes.isNotEmpty()) {
                        val assetPath = "assets/html/${otherFile.name}"
                        val assetName = "html/${otherFile.name}"
                        
                        // Other files (like images) encryption based on encryptMedia config
                        if (encryptionConfig.encryptMedia && encryptor != null) {
                            val encryptedData = encryptor.encrypt(fileBytes, assetName)
                            writeEntryDeflated(zipOut, "${assetPath}.enc", encryptedData)
                            AppLogger.d("ApkBuilder", "Other file encrypted and embedded: ${assetPath}.enc (${encryptedData.size} bytes)")
                        } else {
                            writeEntryDeflated(zipOut, assetPath, fileBytes)
                            AppLogger.d("ApkBuilder", "Other file embedded: $assetPath (${fileBytes.size} bytes)")
                        }
                        successCount++
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("ApkBuilder", "Failed to embed other file: ${otherFile.path}", e)
            }
        }
        
        AppLogger.d("ApkBuilder", "HTML files embedding complete: $successCount/${htmlFiles.size} successful")
        return successCount
    }
    
    /**
     * Detect whether an HTML project is "complex" and should NOT use inlining.
     * 
     * A project is considered complex if any of the following are true:
     * 1. Contains WASM files (.wasm) — cannot be inlined
     * 2. Has more than 3 JS files — likely a bundled/chunked build (React, Vue, etc.)
     * 3. Has JS files with chunk-like naming patterns (e.g., chunk-abc123.js, [hash].js)
     * 4. HTML references ES modules (type="module") — requires separate files
     * 5. Contains source maps (.map) — indicates a build tool output
     * 6. Contains JSON manifest files (asset-manifest.json, manifest.json)
     * 7. Has a nested directory structure (files with path separators)
     */
    private fun isComplexHtmlProject(
        allFiles: List<com.webtoapp.data.model.HtmlFile>,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
        jsFiles: List<com.webtoapp.data.model.HtmlFile>,
        @Suppress("UNUSED_PARAMETER") otherFiles: List<com.webtoapp.data.model.HtmlFile>
    ): Boolean {
        // 1. WASM files present
        val hasWasm = allFiles.any { it.name.endsWith(".wasm", ignoreCase = true) }
        if (hasWasm) {
            AppLogger.d("ApkBuilder", "Complex project indicator: WASM files detected")
            return true
        }
        
        // 2. Many JS files (chunked build output)
        if (jsFiles.size > 3) {
            AppLogger.d("ApkBuilder", "Complex project indicator: ${jsFiles.size} JS files (>3)")
            return true
        }
        
        // 3. Chunk-like JS naming patterns (React/Webpack/Vite output)
        val chunkPattern = Regex("""(chunk|vendor|main|runtime|polyfill)[.\-][a-f0-9]{6,}\.js""", RegexOption.IGNORE_CASE)
        val hasChunkedJs = jsFiles.any { chunkPattern.containsMatchIn(it.name) }
        if (hasChunkedJs) {
            AppLogger.d("ApkBuilder", "Complex project indicator: chunked JS filenames detected")
            return true
        }
        
        // 4. HTML uses ES modules (type="module")
        val htmlUsesModules = htmlFiles.any { htmlFile ->
            try {
                val file = File(htmlFile.path)
                if (file.exists() && file.length() < 1024 * 1024) { // <1MB to avoid reading huge files
                    val content = file.readText(Charsets.UTF_8)
                    content.contains("type=\"module\"", ignoreCase = true) ||
                    content.contains("type='module'", ignoreCase = true)
                } else false
            } catch (_: Exception) { false }
        }
        if (htmlUsesModules) {
            AppLogger.d("ApkBuilder", "Complex project indicator: ES module (type=\"module\") detected in HTML")
            return true
        }
        
        // 5. Source maps present (build tool output)
        val hasSourceMaps = allFiles.any { it.name.endsWith(".map", ignoreCase = true) }
        
        // 6. Asset manifest files (React CRA / Vite / Webpack)
        val hasManifest = allFiles.any { 
            it.name.equals("asset-manifest.json", ignoreCase = true) ||
            it.name.equals("manifest.json", ignoreCase = true) ||
            it.name.equals(".vite-manifest.json", ignoreCase = true)
        }
        if (hasManifest) {
            AppLogger.d("ApkBuilder", "Complex project indicator: build manifest detected")
            return true
        }
        
        // 7. Total file count is high (>10 files suggests a build output)
        if (allFiles.size > 10 && hasSourceMaps) {
            AppLogger.d("ApkBuilder", "Complex project indicator: ${allFiles.size} files with source maps")
            return true
        }
        
        // 8. JS files use import/export (ES module syntax) — sample check first JS file
        val jsUsesModules = jsFiles.take(2).any { jsFile ->
            try {
                val file = File(jsFile.path)
                if (file.exists() && file.length() < 512 * 1024) { // <512KB
                    val content = file.readText(Charsets.UTF_8).take(5000) // check first 5KB
                    content.contains("import ", ignoreCase = false) &&
                    (content.contains(" from ", ignoreCase = false) || content.contains("import(", ignoreCase = false))
                } else false
            } catch (_: Exception) { false }
        }
        if (jsUsesModules) {
            AppLogger.d("ApkBuilder", "Complex project indicator: ES import/export syntax in JS files")
            return true
        }
        
        return false
    }
    
    /**
     * PRESERVE mode: Embed all HTML project files as separate assets,
     * preserving their relative directory structure.
     * 
     * This is used for complex projects (React, Vue, WASM, etc.) where
     * inlining JS/CSS would break module imports and dynamic loading.
     * 
     * Files are placed under assets/html/ with their original names.
     * The WebView loads index.html via file:///android_asset/html/index.html
     * and all relative references (./static/js/main.js, ./chunk.wasm) work correctly.
     */
    private fun addHtmlFilesPreserveStructure(
        zipOut: ZipOutputStream,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ): Int {
        var successCount = 0
        
        htmlFiles.forEach { htmlFile ->
            try {
                val sourceFile = File(htmlFile.path)
                if (!sourceFile.exists() || !sourceFile.canRead()) {
                    AppLogger.w("ApkBuilder", "File not accessible: ${htmlFile.path}")
                    return@forEach
                }
                
                val fileBytes = sourceFile.readBytes()
                if (fileBytes.isEmpty()) {
                    AppLogger.w("ApkBuilder", "File is empty: ${htmlFile.path}")
                    return@forEach
                }
                
                val assetPath = "assets/html/${htmlFile.name}"
                val assetName = "html/${htmlFile.name}"
                val isHtml = htmlFile.name.endsWith(".html", ignoreCase = true) || 
                             htmlFile.name.endsWith(".htm", ignoreCase = true)
                val isText = isHtml || 
                             htmlFile.name.endsWith(".js", ignoreCase = true) ||
                             htmlFile.name.endsWith(".mjs", ignoreCase = true) ||
                             htmlFile.name.endsWith(".css", ignoreCase = true) ||
                             htmlFile.name.endsWith(".json", ignoreCase = true) ||
                             htmlFile.name.endsWith(".svg", ignoreCase = true) ||
                             htmlFile.name.endsWith(".xml", ignoreCase = true) ||
                             htmlFile.name.endsWith(".map", ignoreCase = true) ||
                             htmlFile.name.endsWith(".txt", ignoreCase = true)
                
                // For HTML files, only add viewport meta if missing (no inlining)
                val finalBytes = if (isHtml) {
                    var content = String(fileBytes, Charsets.UTF_8)
                    if (!content.contains("viewport", ignoreCase = true)) {
                        content = com.webtoapp.util.HtmlProjectProcessor.processHtmlContent(
                            htmlContent = content,
                            cssContent = null,
                            jsContent = null,
                            fixPaths = false,
                            removeLocalRefs = false
                        )
                    }
                    content.toByteArray(Charsets.UTF_8)
                } else {
                    fileBytes
                }
                
                // Determine if encryption applies
                val shouldEncrypt = when {
                    isHtml && encryptionConfig.encryptHtml && encryptor != null -> true
                    !isHtml && encryptionConfig.encryptMedia && encryptor != null -> true
                    else -> false
                }
                
                if (shouldEncrypt && encryptor != null) {
                    val encryptedData = encryptor.encrypt(finalBytes, assetName)
                    writeEntryDeflated(zipOut, "${assetPath}.enc", encryptedData)
                    AppLogger.d("ApkBuilder", "File encrypted: ${assetPath}.enc (${encryptedData.size} bytes)")
                } else if (isText) {
                    writeEntryDeflated(zipOut, assetPath, finalBytes)
                    AppLogger.d("ApkBuilder", "Text file preserved: $assetPath (${finalBytes.size} bytes)")
                } else {
                    // Binary files (WASM, images, fonts) — stored without compression
                    writeEntryStored(zipOut, assetPath, finalBytes)
                    AppLogger.d("ApkBuilder", "Binary file preserved: $assetPath (${finalBytes.size} bytes)")
                }
                
                successCount++
            } catch (e: Exception) {
                AppLogger.e("ApkBuilder", "Failed to embed file: ${htmlFile.path}", e)
            }
        }
        
        AppLogger.d("ApkBuilder", "PRESERVE mode complete: $successCount/${htmlFiles.size} files embedded")
        return successCount
    }
    
    /**
     * Detect file encoding
     */
    private fun detectFileEncoding(file: File): String {
        return try {
            val bytes = file.readBytes().take(1000).toByteArray()
            
            // Check BOM
            when {
                bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> "UTF-8"
                bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> "UTF-16BE"
                bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> "UTF-16LE"
                else -> {
                    // Try to detect charset declaration
                    val content = String(bytes, Charsets.ISO_8859_1)
                    val charsetMatch = CHARSET_REGEX.find(content)
                    charsetMatch?.groupValues?.get(1)?.uppercase() ?: "UTF-8"
                }
            }
        } catch (e: Exception) {
            "UTF-8"
        }
    }
    
    /**
     * Convert LrcData to standard LRC format string
     */
    private fun convertLrcDataToLrcString(lrcData: LrcData): String {
        val sb = StringBuilder()
        
        // Add metadata
        lrcData.title?.let { sb.appendLine("[ti:$it]") }
        lrcData.artist?.let { sb.appendLine("[ar:$it]") }
        lrcData.album?.let { sb.appendLine("[al:$it]") }
        sb.appendLine()
        
        // Add lyrics lines
        lrcData.lines.forEach { line ->
            val minutes = line.startTime / 60000
            val seconds = (line.startTime % 60000) / 1000
            val centiseconds = (line.startTime % 1000) / 10
            sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, line.text))
            
            // If has translation, add translation line (using same timestamp)
            line.translation?.let { translation ->
                sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, translation))
            }
        }
        
        return sb.toString()
    }

    /**
     * Debug helper: Use PackageManager to pre-parse built APK, check if system can read package info
     * @return Whether parsing succeeded
     */
    private fun debugApkStructure(apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            val flags = PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_PROVIDERS

            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)

            if (info == null) {
                AppLogger.e(
                    "ApkBuilder",
                    "getPackageArchiveInfo returned null, cannot parse APK: ${apkFile.absolutePath}"
                )
                false
            } else {
                AppLogger.d(
                    "ApkBuilder",
                    "APK parsed successfully: packageName=${info.packageName}, " +
                            "versionName=${info.versionName}, " +
                            "activities=${info.activities?.size ?: 0}, " +
                            "services=${info.services?.size ?: 0}, " +
                            "providers=${info.providers?.size ?: 0}"
                )
                true
            }
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Exception while debug parsing APK: ${apkFile.absolutePath}", e)
            false
        }
    }
    
    /**
     * Generate a default icon when no custom icon is provided.
     * Creates a 512x512 bitmap with the app name's first character on a themed background.
     */
    private fun generateDefaultIcon(appName: String, themeType: String = "AURORA"): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Use theme primary color as background
        val bgColor = getThemePrimaryColor(themeType)

        // Draw rounded-rect background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val radius = size * 0.22f
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, bgPaint)

        // Draw first character
        val initial = appName.firstOrNull()?.uppercase() ?: "A"
        // Choose text color: use white for most themes, dark for light-colored themes
        val textColor = getThemeOnPrimaryColor(themeType)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = size * 0.45f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val textX = size / 2f
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initial, textX, textY, textPaint)

        logger.log("Generated default icon for '$appName' (initial='$initial', theme=$themeType, color=#${Integer.toHexString(bgColor)})")
        return bitmap
    }

    /**
     * Map theme type name to its light-mode primary color (matching AppThemes definitions).
     */
    private fun getThemePrimaryColor(themeType: String): Int = when (themeType) {
        "AURORA"     -> 0xFF7B68EE.toInt()  // Medium Slate Blue
        "CYBERPUNK"  -> 0xFFFF00FF.toInt()  // Magenta
        "SAKURA"     -> 0xFFFFB7C5.toInt()  // Pink
        "OCEAN"      -> 0xFF0077B6.toInt()  // Blue
        "FOREST"     -> 0xFF2D6A4F.toInt()  // Green
        "GALAXY"     -> 0xFF5C4D7D.toInt()  // Purple
        "VOLCANO"    -> 0xFFD32F2F.toInt()  // Red
        "FROST"      -> 0xFF4FC3F7.toInt()  // Light Blue
        "SUNSET"     -> 0xFFE65100.toInt()  // Deep Orange
        "MINIMAL"    -> 0xFF212121.toInt()  // Dark Grey
        "NEON_TOKYO" -> 0xFFE91E63.toInt()  // Pink Red
        "LAVENDER"   -> 0xFF7E57C2.toInt()  // Deep Purple
        else         -> 0xFF7B68EE.toInt()  // Fallback to Aurora
    }

    /**
     * Map theme type name to its onPrimary color for text contrast.
     */
    private fun getThemeOnPrimaryColor(themeType: String): Int = when (themeType) {
        "CYBERPUNK"  -> 0xFF000000.toInt()  // Black on magenta
        "SAKURA"     -> 0xFF4A1C2B.toInt()  // Dark on pink
        "FROST"      -> 0xFF00344A.toInt()  // Dark on light blue
        else         -> 0xFFFFFFFF.toInt()  // White for most themes
    }

    /**
     * Actively add PNG icons to APK
     * Used when original APK has no PNG icons
     */
    private fun addIconsToApk(zipOut: ZipOutputStream, bitmap: Bitmap) {
        // Add all sizes of normal icons
        ApkTemplate.ICON_PATHS.forEach { (path, size) ->
            val iconBytes = template.scaleBitmapToPng(bitmap, size)
            writeEntryDeflated(zipOut, path, iconBytes)
        }
        
        // Add all sizes of round icons
        ApkTemplate.ROUND_ICON_PATHS.forEach { (path, size) ->
            val iconBytes = template.createRoundIcon(bitmap, size)
            writeEntryDeflated(zipOut, path, iconBytes)
        }
    }

    /**
     * Create PNG version for adaptive icon foreground
     * Write ic_launcher_foreground.png and ic_launcher_foreground_new.png in res/drawable directory,
     * and work with ArscRebuilder to switch paths from .xml/.jpg to .png
     * 
     * Note: Following Android Adaptive Icon spec, icon is placed in safe zone (center 72dp),
     * with 18dp margin around to avoid being clipped by shape mask making icon look enlarged
     */
    private fun addAdaptiveIconPngs(
        zipOut: ZipOutputStream,
        bitmap: Bitmap,
        existingEntryNames: Set<String>
    ) {
        // Support both ic_launcher_foreground and ic_launcher_foreground_new
        val bases = listOf(
            "res/drawable/ic_launcher_foreground",
            "res/drawable/ic_launcher_foreground_new",
            "res/drawable-v24/ic_launcher_foreground",
            "res/drawable-v24/ic_launcher_foreground_new",
            "res/drawable-anydpi-v24/ic_launcher_foreground",
            "res/drawable-anydpi-v24/ic_launcher_foreground_new"
        )

        // Use xxxhdpi size (432px) for high resolution, system will auto scale to other dpi
        // 108dp * 4 (xxxhdpi) = 432px
        val iconBytes = template.createAdaptiveForegroundIcon(bitmap, 432)

        bases.forEach { base ->
            val pngPath = "${base}.png"
            if (!existingEntryNames.contains(pngPath)) {
                writeEntryDeflated(zipOut, pngPath, iconBytes)
                AppLogger.d("ApkBuilder", "Added adaptive icon foreground: $pngPath")
            }
        }
    }

    /**
     * Write PNG icons at mipmap-anydpi-v26 paths, replacing the stripped adaptive icon definition XMLs.
     * ArscRebuilder replaces ARSC icon paths to point to our known PNG paths,
     * so we write actual PNG files at these paths for the resource references to be valid.
     *
     * IMPORTANT: Uses 512px size (NOT 192px) to produce a different CRC32 from the
     * density-specific PNGs (which use 192px for xxxhdpi). If the CRC matches,
     * NativeApkOptimizer's dedup will remove THIS file and keep the density-specific one,
     * but ARSC only references the anydpi-v26 path — causing "Failure retrieving resources"
     * because the file no longer exists in the ZIP.
     */
    private fun addAdaptiveIconReplacementPngs(zipOut: ZipOutputStream, bitmap: Bitmap) {
        // Normal icon — 512px to avoid CRC dedup with xxxhdpi (192px)
        val iconPng = template.scaleBitmapToPng(bitmap, 512)
        writeEntryDeflated(zipOut, "res/mipmap-anydpi-v26/ic_launcher.png", iconPng)
        AppLogger.d("ApkBuilder", "Added replacement icon: res/mipmap-anydpi-v26/ic_launcher.png (512px, ${iconPng.size} bytes)")

        // Round icon — 512px to avoid CRC dedup
        val roundPng = template.createRoundIcon(bitmap, 512)
        writeEntryDeflated(zipOut, "res/mipmap-anydpi-v26/ic_launcher_round.png", roundPng)
        AppLogger.d("ApkBuilder", "Added replacement icon: res/mipmap-anydpi-v26/ic_launcher_round.png (512px, ${roundPng.size} bytes)")
        
        logger.log("Added PNG icons at mipmap-anydpi-v26 paths (512px, replacing adaptive icon XMLs)")
    }
    
    /**
     * Write entry (using DEFLATED compression format)
     */
    private fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        ZipUtils.writeEntryDeflated(zipOut, name, data)
    }

    /**
     * Write entry (using STORED uncompressed format)
     * For resources.arsc, to satisfy Android R+ uncompressed and 4-byte alignment requirements
     */
    private fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        ZipUtils.writeEntryStored(zipOut, name, data)
    }

    /**
     * Write config file entry (supports encryption)
     */
    private fun writeConfigEntry(
        zipOut: ZipOutputStream, 
        config: ApkConfig,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        val configJson = template.createConfigJson(config)
        AppLogger.d("ApkBuilder", "Writing config file: splashEnabled=${config.splashEnabled}, splashType=${config.splashType}")
        AppLogger.d("ApkBuilder", "Config userAgentMode=${config.userAgentMode}, customUserAgent=${config.customUserAgent}")
        AppLogger.d("ApkBuilder", "Config JSON content: $configJson")
        
        if (encryptionConfig.encryptConfig && encryptor != null) {
            // Encrypt config file
            val encryptedData = encryptor.encryptJson(configJson, "app_config.json")
            writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH + ".enc", encryptedData)
            // 同时写入明文配置作为解密失败时的安全降级
            // 确保即使解密环节出问题，App 也不会变回原版 WebToApp
            val data = configJson.toByteArray(Charsets.UTF_8)
            writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH, data)
            AppLogger.d("ApkBuilder", "Config file encrypted (with plaintext fallback)")
        } else {
            // Write plaintext
            val data = configJson.toByteArray(Charsets.UTF_8)
            writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH, data)
        }
    }

    /**
     * Check if an asset entry is optimizable (image/JS/CSS/SVG)
     */
    private fun isOptimizableAsset(entryName: String): Boolean {
        val ext = entryName.substringAfterLast('.', "").lowercase()
        return ext in setOf("png", "jpg", "jpeg", "js", "css", "svg")
    }
    
    /**
     * Check if an entry is editor-only and not needed in Shell mode APKs.
     * Used to strip unnecessary template assets from generated APKs.
     */
    private fun isEditorOnlyAsset(entryName: String, appType: String, engineType: String): Boolean {
        // Editor template files
        if (entryName.startsWith("assets/template/")) return true
        // Sample projects (editor-only demos)
        if (entryName.startsWith("assets/sample_projects/")) return true
        // AI model lists (editor AI coding feature only)
        if (entryName.startsWith("assets/ai/")) return true
        if (entryName == "assets/litellm_model_prices.json") return true
        // Built-in browser extensions (editor manages these, Shell mode loads from config)
        if (entryName.startsWith("assets/extensions/")) return true
        // GeckoView omni.ja (12.6MB) — only needed if engine is GeckoView
        if (entryName == "assets/omni.ja" && engineType != "GECKOVIEW") return true
        // PHP router script — only needed for WordPress/PHP apps
        if (entryName == "assets/php_router_server.php" && appType !in setOf("WORDPRESS", "PHP_APP")) return true
        
        // === 额外瘦身规则 (C 优化器协同) ===
        
        // Python 运行时 — 仅 Python 应用需要
        if (entryName.startsWith("assets/python_runtime/") && appType != "PYTHON_APP") return true
        
        // Go 运行时 — 仅 Go 应用需要
        if (entryName.startsWith("assets/go_runtime/") && appType != "GO_APP") return true
        
        // 编辑器专用文档和帮助文件
        if (entryName.startsWith("assets/docs/")) return true
        if (entryName.startsWith("assets/help/")) return true
        
        // Room 数据库 schema (仅编辑器开发调试用)
        if (entryName.startsWith("assets/schemas/")) return true
        
        // 编辑器专用的大型 JSON 配置
        if (entryName == "assets/default_config.json") return true
        
        // 前端构建工具配置 — 仅前端项目需要
        if (entryName.startsWith("assets/frontend_tools/") && appType != "FRONTEND") return true
        
        // Node.js modules — 仅 Node.js 应用需要
        if (entryName.startsWith("assets/nodejs_runtime/") && appType != "NODEJS_APP") return true
        
        return false
    }
    
    /**
     * Check if is icon entry
     * Match multiple possible icon path formats
     */
    private fun isIconEntry(entryName: String): Boolean {
        // Exact match predefined paths
        if (ApkTemplate.ICON_PATHS.any { it.first == entryName } ||
            ApkTemplate.ROUND_ICON_PATHS.any { it.first == entryName }) {
            return true
        }
        
        // Fuzzy match: detect all possible icon PNG files
        // Support various path formats: mipmap-xxxhdpi-v4, mipmap-xxxhdpi, drawable-xxxhdpi etc.
        val iconPatterns = listOf(
            "ic_launcher.png",
            "ic_launcher_round.png"
            // Note: ic_launcher_foreground.png and ic_launcher_background.png are adaptive icon
            // components, NOT standalone launcher icons. They are handled by isAdaptiveIconEntry().
        )
        return iconPatterns.any { pattern ->
            entryName.endsWith(pattern) && 
            (entryName.contains("mipmap") || entryName.contains("drawable"))
        }
    }

    /**
     * Check if entry is an adaptive icon foreground drawable that should be replaced
     * with the user's custom icon.
     * 
     * Strategy: We keep the adaptive icon definition XMLs (mipmap-anydpi-v26/ic_launcher.xml)
     * in the output because the ARSC references them. These XMLs reference
     * @drawable/ic_launcher_foreground, so by replacing the foreground drawable with the
     * user's icon, the adaptive icon system automatically displays the correct icon on Android 8+.
     * 
     * Matched entries (replaced with user's icon):
     * - drawable/ic_launcher_foreground.png (PNG foreground image)
     * - drawable/ic_launcher_foreground.xml (compiled vector foreground)
     * - drawable/ic_launcher_foreground_new.jpg (foreground image variant)
     * 
     * NOT matched (kept as-is, copied via else branch):
     * - mipmap-anydpi-v26/ic_launcher.xml (adaptive icon definition — references the foreground we replace)
     * - mipmap-anydpi-v26/ic_launcher_round.xml (adaptive round icon definition)
     */
    private fun isAdaptiveIconEntry(entryName: String): Boolean {
        // Only match foreground image files that need to be replaced with the user's icon.
        // Note: Adaptive icon definition XMLs (mipmap-anydpi-v26/ic_launcher.xml) are now
        // stripped separately by isAdaptiveIconDefinition() to force PNG icon fallback,
        // because R8 obfuscation makes the foreground path unpredictable.
        // Support all formats: XML (compiled vector), JPG, and PNG.
        if ((entryName.contains("drawable")) &&
            (entryName.contains("ic_launcher_foreground") || entryName.contains("ic_launcher_foreground_new")) &&
            (entryName.endsWith(".xml") || entryName.endsWith(".jpg") || entryName.endsWith(".png"))) {
            return true
        }
        return false
    }

    /**
     * Check if entry is an adaptive icon definition XML (mipmap-anydpi-v26/ic_launcher*.xml).
     * These have the highest resource priority on Android 8+ and override density-specific PNG icons.
     * In release builds with R8, the foreground drawable they reference gets renamed to obfuscated
     * paths (e.g., res/Pb.jpg), making it impossible to reliably replace via isAdaptiveIconEntry().
     * Stripping these definition XMLs forces Android to fall back to our injected PNG icons.
     */
    private fun isAdaptiveIconDefinition(entryName: String): Boolean {
        return entryName.startsWith("res/mipmap-anydpi") &&
            (entryName.contains("ic_launcher") || entryName.contains("ic_launcher_round")) &&
            entryName.endsWith(".xml")
    }

    /**
     * Replace icon entry
     * Infer icon size from dpi info in path
     */
    private fun replaceIconEntry(zipOut: ZipOutputStream, entryName: String, bitmap: Bitmap) {
        // Prioritize predefined sizes
        var size = ApkTemplate.ICON_PATHS.find { it.first == entryName }?.second
            ?: ApkTemplate.ROUND_ICON_PATHS.find { it.first == entryName }?.second
        
        // If no predefined match, infer size from path
        if (size == null) {
            size = when {
                entryName.contains("xxxhdpi") -> 192
                entryName.contains("xxhdpi") -> 144
                entryName.contains("xhdpi") -> 96
                entryName.contains("hdpi") -> 72
                entryName.contains("mdpi") -> 48
                entryName.contains("ldpi") -> 36
                else -> 96
            }
        }
        
        val iconBytes = when {
            // Round icon
            entryName.contains("round") -> {
                template.createRoundIcon(bitmap, size)
            }
            // Adaptive icon foreground needs safe zone margin
            entryName.contains("foreground") -> {
                template.createAdaptiveForegroundIcon(bitmap, size)
            }
            // Normal icon
            else -> {
                template.scaleBitmapToPng(bitmap, size)
            }
        }
        
        writeEntryDeflated(zipOut, entryName, iconBytes)
    }

    /**
     * Copy ZIP entry
     * Use DEFLATED compression for compatibility
     */
    private fun copyEntry(zipIn: ZipFile, zipOut: ZipOutputStream, entry: ZipEntry) {
        ZipUtils.copyEntry(zipIn, zipOut, entry)
    }

    /**
     * Generate package name
     * Note: New package name length must be <= original "com.webtoapp" (12 chars)
     * Format: com.w2a.xxxx (12 chars)
     *
     * Constraint: Last segment must be valid Java identifier (first char is letter or underscore),
     * otherwise PackageManager will report invalid package name during parsing, showing "installation package corrupted".
     */
    private fun generatePackageName(appName: String): String {
        // Generate 4-digit base36 identifier from app name, then normalize to valid package segment
        val raw = appName.hashCode().let { 
            if (it < 0) (-it).toString(36) else it.toString(36)
        }.take(4).padStart(4, '0')

        val segment = normalizePackageSegment(raw)

        return "com.w2a.$segment"  // Total length: 12 chars, same as original package name
    }

    /**
     * Normalize single segment in package name:
     * - Convert to lowercase
     * - If first char is digit or other illegal char, map/replace to letter, ensuring [a-zA-Z_][a-zA-Z0-9_]* rule
     */
    private fun normalizePackageSegment(segment: String): String {
        if (segment.isEmpty()) return "a"

        val chars = segment.lowercase().toCharArray()

        chars[0] = when {
            chars[0] in 'a'..'z' -> chars[0]
            chars[0] in '0'..'9' -> ('a' + (chars[0] - '0'))  // 0..9 maps to a..j
            else -> 'a'
        }

        // Remaining chars are already [0-9a-z] from base36, meeting package name requirements
        return String(chars)
    }

    /**
     * Sanitize file name
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(SANITIZE_FILENAME_REGEX, "_").take(50)
    }

    /**
     * Install APK
     */
    fun installApk(apkFile: File): Boolean {
        return try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Operation failed", e)
            false
        }
    }

    /**
     * Get list of built APKs
     */
    fun getBuiltApks(): List<File> {
        return outputDir.listFiles()?.filter { it.extension == "apk" } ?: emptyList()
    }

    /**
     * Delete built APK
     */
    fun deleteApk(apkFile: File): Boolean {
        return apkFile.delete()
    }

    /**
     * Clear all build files
     */
    fun clearAll() {
        outputDir.listFiles()?.forEach { it.delete() }
        tempDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * Get all build log files
     */
    fun getBuildLogs(): List<File> {
        return logger.getAllLogFiles()
    }
    
    /**
     * Get log directory path
     */
    fun getLogDirectory(): String {
        return File(context.getExternalFilesDir(null), "build_logs").absolutePath
    }
}

/**
 * WebApp extension function: Convert to ApkConfig
 */
fun WebApp.toApkConfig(packageName: String): ApkConfig {
    // HTML and media apps don't use targetUrl, set placeholder to avoid config validation failure
    val effectiveTargetUrl = when (appType) {
        com.webtoapp.data.model.AppType.HTML -> {
            val entryFile = htmlConfig?.getValidEntryFile() ?: "index.html"
            "file:///android_asset/html/$entryFile"
        }
        com.webtoapp.data.model.AppType.IMAGE, com.webtoapp.data.model.AppType.VIDEO -> "asset://media_content"
        com.webtoapp.data.model.AppType.GALLERY -> "gallery://content"
        com.webtoapp.data.model.AppType.WORDPRESS -> "wordpress://localhost"  // WordPress应用使用本地PHP服务器
        com.webtoapp.data.model.AppType.NODEJS_APP -> {
            val config = nodejsConfig
            when (config?.buildMode) {
                com.webtoapp.data.model.NodeJsBuildMode.STATIC -> {
                    // 静态模式：指向打包后的静态文件
                    "file:///android_asset/nodejs_app/dist/index.html"
                }
                com.webtoapp.data.model.NodeJsBuildMode.API_BACKEND,
                com.webtoapp.data.model.NodeJsBuildMode.FULLSTACK -> {
                    // 后端/全栈模式：本地 Node.js 服务器
                    "nodejs://localhost"
                }
                else -> "file:///android_asset/nodejs_app/index.html"
            }
        }
        com.webtoapp.data.model.AppType.FRONTEND -> {
            // 前端项目：指向打包后的文件
            val entryFile = htmlConfig?.getValidEntryFile() ?: "index.html"
            "file:///android_asset/frontend_app/$entryFile"
        }
        com.webtoapp.data.model.AppType.PHP_APP -> "phpapp://localhost"
        com.webtoapp.data.model.AppType.PYTHON_APP -> "pythonapp://localhost"
        com.webtoapp.data.model.AppType.GO_APP -> "goapp://localhost"
        com.webtoapp.data.model.AppType.MULTI_WEB -> "multiweb://localhost"
        else -> url
    }
    
    return ApkConfig(
        appName = name,
        packageName = packageName,
        targetUrl = effectiveTargetUrl,
        versionCode = apkExportConfig?.customVersionCode ?: 1,
        versionName = apkExportConfig?.customVersionName?.takeIf { it.isNotBlank() } ?: "1.0.0",
        iconPath = iconPath,
        activationEnabled = activationEnabled,
        activationCodes = activationCodes,
        activationRequireEveryTime = activationRequireEveryTime,
        activationDialogTitle = activationDialogConfig?.title ?: "",
        activationDialogSubtitle = activationDialogConfig?.subtitle ?: "",
        activationDialogInputLabel = activationDialogConfig?.inputLabel ?: "",
        activationDialogButtonText = activationDialogConfig?.buttonText ?: "",
        adBlockEnabled = adBlockEnabled,
        adBlockRules = adBlockRules,
        announcementEnabled = announcementEnabled,
        announcementTitle = announcement?.title ?: "",
        announcementContent = announcement?.content ?: "",
        announcementLink = announcement?.linkUrl ?: "",
        announcementLinkText = announcement?.linkText ?: "",
        announcementTemplate = announcement?.template?.name ?: "XIAOHONGSHU",
        announcementShowEmoji = announcement?.showEmoji ?: true,
        announcementAnimationEnabled = announcement?.animationEnabled ?: true,
        announcementShowOnce = announcement?.showOnce ?: true,
        announcementRequireConfirmation = announcement?.requireConfirmation ?: false,
        announcementAllowNeverShow = announcement?.allowNeverShow ?: false,
        javaScriptEnabled = webViewConfig.javaScriptEnabled,
        domStorageEnabled = webViewConfig.domStorageEnabled,
        zoomEnabled = webViewConfig.zoomEnabled,
        desktopMode = webViewConfig.desktopMode,
        userAgent = webViewConfig.userAgent,
        userAgentMode = webViewConfig.userAgentMode.name,
        customUserAgent = webViewConfig.customUserAgent,
        // Use user-configured hideToolbar setting, no longer force HTML/media apps to hide toolbar
        // User can choose whether to enable fullscreen mode when creating app
        hideToolbar = webViewConfig.hideToolbar,
        showStatusBarInFullscreen = webViewConfig.showStatusBarInFullscreen,
        showNavigationBarInFullscreen = webViewConfig.showNavigationBarInFullscreen,
        showToolbarInFullscreen = webViewConfig.showToolbarInFullscreen,
        landscapeMode = webViewConfig.landscapeMode,
        orientationMode = webViewConfig.orientationMode.name,
        // 构建时注入 C 级性能优化脚本:
        // 被动事件监听 (消除滚动卡顿) + 图片懒加载 + DNS 预连接 + 掉帧自适应降级
        // 优化脚本排在用户脚本之前，确保性能优化最先生效
        injectScripts = buildList {
            // 浏览器内核伪装 JS — 最先注入, 确保在任何页面脚本之前覆盖检测向量
            add(com.webtoapp.data.model.UserScript(
                name = "__kernel__",
                code = com.webtoapp.core.kernel.BrowserKernel.getBuildTimeKernelJs(),
                enabled = true,
                runAt = com.webtoapp.data.model.ScriptRunTime.DOCUMENT_START
            ))
            // C 级性能优化 - DOCUMENT_START (被动事件, 可见性回收)
            add(com.webtoapp.data.model.UserScript(
                name = "__perf_start__",
                code = com.webtoapp.core.perf.NativePerfEngine.getPerfJsStart(),
                enabled = true,
                runAt = com.webtoapp.data.model.ScriptRunTime.DOCUMENT_START
            ))
            // C 级性能优化 - DOCUMENT_END (懒加载, content-visibility, 预连接, 掉帧检测)
            add(com.webtoapp.data.model.UserScript(
                name = "__perf_end__",
                code = com.webtoapp.core.perf.NativePerfEngine.getPerfJsEnd(),
                enabled = true,
                runAt = com.webtoapp.data.model.ScriptRunTime.DOCUMENT_END
            ))
            // 用户自定义脚本
            addAll(webViewConfig.injectScripts)
        },
        // Status bar config
        statusBarColorMode = webViewConfig.statusBarColorMode.name,
        statusBarColor = webViewConfig.statusBarColor,
        statusBarDarkIcons = webViewConfig.statusBarDarkIcons,
        statusBarBackgroundType = webViewConfig.statusBarBackgroundType.name,
        statusBarBackgroundImage = webViewConfig.statusBarBackgroundImage,
        statusBarBackgroundAlpha = webViewConfig.statusBarBackgroundAlpha,
        statusBarHeightDp = webViewConfig.statusBarHeightDp,
        longPressMenuEnabled = webViewConfig.longPressMenuEnabled,
        longPressMenuStyle = webViewConfig.longPressMenuStyle.name,
        adBlockToggleEnabled = webViewConfig.adBlockToggleEnabled,
        popupBlockerEnabled = webViewConfig.popupBlockerEnabled,
        popupBlockerToggleEnabled = webViewConfig.popupBlockerToggleEnabled,
        openExternalLinks = webViewConfig.openExternalLinks,
        // 浏览器兼容性增强配置
        initialScale = webViewConfig.initialScale,
        viewportMode = webViewConfig.viewportMode.name,
        newWindowBehavior = webViewConfig.newWindowBehavior.name,
        enablePaymentSchemes = webViewConfig.enablePaymentSchemes,
        enableShareBridge = webViewConfig.enableShareBridge,
        enableZoomPolyfill = webViewConfig.enableZoomPolyfill,
        enableCrossOriginIsolation = webViewConfig.enableCrossOriginIsolation,
        disableShields = webViewConfig.disableShields,
        keepScreenOn = webViewConfig.keepScreenOn,
        screenAwakeMode = webViewConfig.screenAwakeMode.name,
        screenAwakeTimeoutMinutes = webViewConfig.screenAwakeTimeoutMinutes,
        screenBrightness = webViewConfig.screenBrightness,
        keyboardAdjustMode = webViewConfig.keyboardAdjustMode.name,
        showFloatingBackButton = webViewConfig.showFloatingBackButton,
        swipeRefreshEnabled = webViewConfig.swipeRefreshEnabled,
        fullscreenEnabled = webViewConfig.fullscreenEnabled,
        performanceOptimization = webViewConfig.performanceOptimization,
        pwaOfflineEnabled = webViewConfig.pwaOfflineEnabled,
        pwaOfflineStrategy = webViewConfig.pwaOfflineStrategy,
        // 网络错误页配置
        errorPageMode = webViewConfig.errorPageConfig.mode.name,
        errorPageBuiltInStyle = webViewConfig.errorPageConfig.builtInStyle.name,
        errorPageShowMiniGame = webViewConfig.errorPageConfig.showMiniGame,
        errorPageMiniGameType = webViewConfig.errorPageConfig.miniGameType.name,
        errorPageAutoRetrySeconds = webViewConfig.errorPageConfig.autoRetrySeconds,
        // 悬浮小窗配置
        floatingWindowEnabled = webViewConfig.floatingWindowConfig.enabled,
        floatingWindowSizePercent = webViewConfig.floatingWindowConfig.windowSizePercent,
        floatingWindowWidthPercent = webViewConfig.floatingWindowConfig.widthPercent,
        floatingWindowHeightPercent = webViewConfig.floatingWindowConfig.heightPercent,
        floatingWindowLockAspectRatio = webViewConfig.floatingWindowConfig.lockAspectRatio,
        floatingWindowOpacity = webViewConfig.floatingWindowConfig.opacity,
        floatingWindowCornerRadius = webViewConfig.floatingWindowConfig.cornerRadius,
        floatingWindowBorderStyle = webViewConfig.floatingWindowConfig.borderStyle.name,
        floatingWindowShowTitleBar = webViewConfig.floatingWindowConfig.showTitleBar,
        floatingWindowAutoHideTitleBar = webViewConfig.floatingWindowConfig.autoHideTitleBar,
        floatingWindowStartMinimized = webViewConfig.floatingWindowConfig.startMinimized,
        floatingWindowRememberPosition = webViewConfig.floatingWindowConfig.rememberPosition,
        floatingWindowEdgeSnapping = webViewConfig.floatingWindowConfig.edgeSnapping,
        floatingWindowShowResizeHandle = webViewConfig.floatingWindowConfig.showResizeHandle,
        floatingWindowLockPosition = webViewConfig.floatingWindowConfig.lockPosition,
        splashEnabled = splashEnabled,
        splashType = splashConfig?.type?.name ?: "IMAGE",
        splashDuration = splashConfig?.duration ?: 3,
        splashClickToSkip = splashConfig?.clickToSkip ?: true,
        splashVideoStartMs = splashConfig?.videoStartMs ?: 0L,
        splashVideoEndMs = splashConfig?.videoEndMs ?: 5000L,
        splashLandscape = splashConfig?.orientation == com.webtoapp.data.model.SplashOrientation.LANDSCAPE,
        splashFillScreen = splashConfig?.fillScreen ?: true,
        splashEnableAudio = splashConfig?.enableAudio ?: false,
        // Media app config
        appType = appType.name,
        mediaEnableAudio = mediaConfig?.enableAudio ?: true,
        mediaLoop = mediaConfig?.loop ?: true,
        mediaAutoPlay = mediaConfig?.autoPlay ?: true,
        mediaFillScreen = mediaConfig?.fillScreen ?: true,
        mediaLandscape = mediaConfig?.orientation == com.webtoapp.data.model.SplashOrientation.LANDSCAPE,
        mediaKeepScreenOn = mediaConfig?.keepScreenOn ?: true,
        
        // HTML app config
        htmlEntryFile = htmlConfig?.getValidEntryFile() ?: "index.html",
        htmlEnableJavaScript = htmlConfig?.enableJavaScript ?: true,
        htmlEnableLocalStorage = htmlConfig?.enableLocalStorage ?: true,
        htmlLandscapeMode = htmlConfig?.landscapeMode ?: false,
        
        // Gallery app config
        galleryItems = galleryConfig?.items?.mapIndexed { index, item ->
            val ext = if (item.type == com.webtoapp.data.model.GalleryItemType.VIDEO) "mp4" else "png"
            GalleryShellItemConfig(
                id = item.id,
                assetPath = "gallery/item_$index.$ext",
                type = item.type.name,
                name = item.name,
                duration = item.duration,
                thumbnailPath = if (item.thumbnailPath != null) "gallery/thumb_$index.jpg" else null
            )
        } ?: emptyList(),
        galleryPlayMode = galleryConfig?.playMode?.name ?: "SEQUENTIAL",
        galleryImageInterval = galleryConfig?.imageInterval ?: 3,
        galleryLoop = galleryConfig?.loop ?: true,
        galleryAutoPlay = galleryConfig?.autoPlay ?: false,
        galleryBackgroundColor = galleryConfig?.backgroundColor ?: "#000000",
        galleryShowThumbnailBar = galleryConfig?.showThumbnailBar ?: true,
        galleryShowMediaInfo = galleryConfig?.showMediaInfo ?: true,
        galleryOrientation = galleryConfig?.orientation?.name ?: "PORTRAIT",
        galleryEnableAudio = galleryConfig?.enableAudio ?: true,
        galleryVideoAutoNext = galleryConfig?.videoAutoNext ?: true,
        
        // Background music config
        bgmEnabled = bgmEnabled,
        bgmPlaylist = bgmConfig?.playlist?.mapIndexed { index, item ->
            BgmShellItem(
                id = item.id,
                name = item.name,
                assetPath = "bgm/bgm_$index.mp3",  // Will be stored as assets/bgm/bgm_0.mp3 etc. in APK
                lrcAssetPath = if (item.lrcData != null) "bgm/bgm_$index.lrc" else null,
                sortOrder = item.sortOrder
            )
        } ?: emptyList(),
        bgmPlayMode = bgmConfig?.playMode?.name ?: "LOOP",
        bgmVolume = bgmConfig?.volume ?: 0.5f,
        bgmAutoPlay = bgmConfig?.autoPlay ?: true,
        bgmShowLyrics = bgmConfig?.showLyrics ?: true,
        bgmLrcTheme = bgmConfig?.lrcTheme?.let { theme ->
            LrcShellTheme(
                id = theme.id,
                name = theme.name,
                fontSize = theme.fontSize,
                textColor = theme.textColor,
                highlightColor = theme.highlightColor,
                backgroundColor = theme.backgroundColor,
                animationType = theme.animationType.name,
                position = theme.position.name
            )
        },
        // Theme config
        themeType = themeType,
        darkMode = "SYSTEM",
        // Translation config
        translateEnabled = translateEnabled,
        translateTargetLanguage = translateConfig?.targetLanguage?.code ?: "zh-CN",
        translateShowButton = translateConfig?.showFloatingButton ?: true,
        // Extension module config
        extensionFabIcon = extensionFabIcon ?: "",
        extensionModuleIds = extensionModuleIds,
        // Auto start config
        bootStartEnabled = autoStartConfig?.bootStartEnabled ?: false,
        scheduledStartEnabled = autoStartConfig?.scheduledStartEnabled ?: false,
        scheduledTime = autoStartConfig?.scheduledTime ?: "08:00",
        scheduledDays = autoStartConfig?.scheduledDays ?: listOf(1, 2, 3, 4, 5, 6, 7),
        // Forced run config
        forcedRunConfig = forcedRunConfig,
        // Isolation/multi-instance config
        isolationEnabled = apkExportConfig?.isolationConfig?.enabled ?: false,
        isolationConfig = apkExportConfig?.isolationConfig,
        // Background run config
        backgroundRunEnabled = apkExportConfig?.backgroundRunEnabled ?: false,
        backgroundRunConfig = apkExportConfig?.backgroundRunConfig?.let {
            BackgroundRunConfig(
                notificationTitle = it.notificationTitle,
                notificationContent = it.notificationContent,
                showNotification = it.showNotification,
                keepCpuAwake = it.keepCpuAwake
            )
        },
        // Black tech feature config (independent module)
        blackTechConfig = blackTechConfig,
        // App disguise config (independent module)
        disguiseConfig = disguiseConfig,
        // UI language config - use current app language
        language = com.webtoapp.core.i18n.Strings.currentLanguage.value.name,
        // Browser engine config
        engineType = apkExportConfig?.engineType ?: "SYSTEM_WEBVIEW",
        // Deep link config
        deepLinkEnabled = apkExportConfig?.deepLinkEnabled ?: false,
        deepLinkHosts = if (apkExportConfig?.deepLinkEnabled == true) {
            extractHostsFromUrl(url, apkExportConfig.customDeepLinkHosts)
        } else {
            emptyList()
        },
        // WordPress config
        wordpressSiteTitle = wordpressConfig?.siteTitle ?: "",
        wordpressPhpPort = wordpressConfig?.phpPort ?: 0,
        wordpressLandscapeMode = wordpressConfig?.landscapeMode ?: false,
        
        // Node.js config
        nodejsMode = nodejsConfig?.buildMode?.name ?: "STATIC",
        nodejsPort = nodejsConfig?.serverPort ?: 0,
        nodejsEntryFile = nodejsConfig?.entryFile ?: "",
        nodejsEnvVars = nodejsConfig?.envVars ?: emptyMap(),
        nodejsLandscapeMode = nodejsConfig?.landscapeMode ?: false,
        
        // PHP 通用应用 config
        phpAppFramework = phpAppConfig?.framework ?: "",
        phpAppDocumentRoot = phpAppConfig?.documentRoot ?: "",
        phpAppEntryFile = phpAppConfig?.entryFile ?: "index.php",
        phpAppPort = phpAppConfig?.phpPort ?: 0,
        phpAppEnvVars = phpAppConfig?.envVars ?: emptyMap(),
        phpAppLandscapeMode = phpAppConfig?.landscapeMode ?: false,
        
        // Python Web 应用 config
        pythonAppFramework = pythonAppConfig?.framework ?: "",
        pythonAppEntryFile = pythonAppConfig?.entryFile ?: "app.py",
        pythonAppEntryModule = pythonAppConfig?.entryModule ?: "",
        pythonAppServerType = pythonAppConfig?.serverType ?: "builtin",
        pythonAppPort = pythonAppConfig?.serverPort ?: 0,
        pythonAppEnvVars = pythonAppConfig?.envVars ?: emptyMap(),
        pythonAppLandscapeMode = pythonAppConfig?.landscapeMode ?: false,
        
        // Go Web 服务 config
        goAppFramework = goAppConfig?.framework ?: "",
        goAppBinaryName = goAppConfig?.binaryName ?: "",
        goAppPort = goAppConfig?.serverPort ?: 0,
        goAppStaticDir = goAppConfig?.staticDir ?: "",
        goAppEnvVars = goAppConfig?.envVars ?: emptyMap(),
        goAppLandscapeMode = goAppConfig?.landscapeMode ?: false,
        
        // 多站点聚合 config
        multiWebSites = multiWebConfig?.sites?.map { site ->
            com.webtoapp.core.shell.MultiWebSiteShellConfig(
                id = site.id,
                name = site.name,
                url = site.url,
                iconEmoji = site.iconEmoji,
                category = site.category,
                cssSelector = site.cssSelector,
                linkSelector = site.linkSelector,
                enabled = site.enabled
            )
        } ?: emptyList(),
        multiWebDisplayMode = multiWebConfig?.displayMode ?: "TABS",
        multiWebRefreshInterval = multiWebConfig?.refreshInterval ?: 30,
        multiWebShowSiteIcons = multiWebConfig?.showSiteIcons ?: true,
        multiWebLandscapeMode = multiWebConfig?.landscapeMode ?: false,
        
        // 云 SDK 配置（构建时嵌入）
        cloudSdkConfig = cloudConfig?.toCloudSdkConfig() ?: com.webtoapp.core.shell.CloudSdkConfig()
    )
}

/**
 * 智能提取 Deep Link 域名列表
 *
 * 自动识别并生成完整的域名匹配集合：
 * 1. 原始域名（如 www.example.com）
 * 2. 去掉 www 的裸域名（如 example.com）
 * 3. 如果输入是裸域名，则追加 www 前缀
 * 4. 合并用户自定义的额外域名
 *
 * 这样用户只需开启开关，无需手动配置域名列表。
 */
private fun extractHostsFromUrl(url: String, customHosts: List<String> = emptyList()): List<String> {
    // Known second-level TLDs (country-code domains like co.uk, com.au)
    val secondLevelTlds = setOf(
        "co.uk", "org.uk", "ac.uk", "gov.uk",
        "com.au", "net.au", "org.au", "edu.au",
        "co.jp", "or.jp", "ne.jp", "ac.jp",
        "com.cn", "net.cn", "org.cn", "edu.cn",
        "com.br", "org.br", "net.br",
        "co.kr", "or.kr", "ne.kr",
        "co.in", "net.in", "org.in",
        "com.tw", "org.tw", "net.tw",
        "co.nz", "org.nz", "net.nz",
        "com.hk", "org.hk", "net.hk",
        "com.sg", "org.sg", "net.sg",
        "co.za", "org.za", "net.za",
        "com.mx", "org.mx", "net.mx",
        "com.ar", "org.ar", "net.ar",
        "co.id", "or.id", "web.id",
        "com.my", "org.my", "net.my",
        "co.th", "or.th", "in.th"
    )
    
    fun getApexDomain(host: String): String {
        val parts = host.split(".")
        if (parts.size <= 2) return host
        val lastTwo = parts.takeLast(2).joinToString(".")
        return if (lastTwo in secondLevelTlds && parts.size > 2) {
            parts.takeLast(3).joinToString(".")
        } else {
            lastTwo
        }
    }
    
    val hosts = mutableSetOf<String>()
    try {
        val uri = android.net.Uri.parse(url)
        val host = uri.host?.lowercase()
        if (!host.isNullOrBlank() && host != "localhost" && !host.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))) {
            hosts.add(host)
            val apex = getApexDomain(host)
            // Auto-add apex domain (strips www. or other subdomains)
            if (host != apex) {
                hosts.add(apex)
            }
            // Auto-add www variant of apex
            val wwwApex = "www.$apex"
            if (host != wwwApex) {
                hosts.add(wwwApex)
            }
        }
    } catch (_: Exception) { }

    // Merge user-specified custom hosts
    customHosts.forEach { custom ->
        val trimmed = custom.trim().lowercase()
        if (trimmed.isNotBlank() && trimmed.contains(".")) {
            hosts.add(trimmed)
        }
    }

    return hosts.toList()
}

/**
 * WebApp extension function: Convert to ApkConfig (with embedded module data)
 * @param packageName Package name
 * @param context Context, for getting extension module manager
 */
fun WebApp.toApkConfigWithModules(packageName: String, context: android.content.Context): ApkConfig {
    val baseConfig = toApkConfig(packageName)
    
    // Get and embed extension module data
    val embeddedModules = if (extensionModuleIds.isNotEmpty()) {
        try {
            val extensionManager = com.webtoapp.core.extension.ExtensionManager.getInstance(context)
            extensionManager.getModulesByIds(extensionModuleIds).map { module ->
                EmbeddedExtensionModule(
                    id = module.id,
                    name = module.name,
                    description = module.description,
                    icon = module.icon,
                    category = module.category.name,
                    code = module.code,
                    cssCode = module.cssCode,
                    runAt = module.runAt.name,
                    urlMatches = module.urlMatches.map { rule ->
                        EmbeddedUrlMatchRule(
                            pattern = rule.pattern,
                            isRegex = rule.isRegex,
                            exclude = rule.exclude
                        )
                    },
                    configValues = module.configValues,
                    // Important fix: User selecting module means they want to enable it
                    // Built-in modules default enabled=false, but should be true when embedded in APK
                    enabled = true
                )
            }
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to get extension module data", e)
            emptyList()
        }
    } else {
        emptyList()
    }
    
    return baseConfig.copy(
        embeddedExtensionModules = embeddedModules
    )
}

/**
 * Get splash media path
 */
fun WebApp.getSplashMediaPath(): String? {
    return if (splashEnabled) splashConfig?.mediaPath else null
}

/**
 * Build result
 */
sealed class BuildResult {
    data class Success(
        val apkFile: File, 
        val logPath: String? = null,
        val analysisReport: ApkAnalyzer.AnalysisReport? = null
    ) : BuildResult()
    data class Error(val message: String) : BuildResult()
}
