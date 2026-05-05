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
import com.webtoapp.data.model.ApkRuntimePermissions
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.AnnouncementTemplateType
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.getActivationCodeStrings
import com.webtoapp.ui.components.announcement.toUiTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*
import javax.crypto.SecretKey
import com.webtoapp.util.AppConstants
import com.webtoapp.util.TextFileClassifier














class ApkBuilder(private val context: Context) {

    companion object {
        private val SANITIZE_FILENAME_REGEX = AppConstants.SANITIZE_FILENAME_REGEX
        private val PACKAGE_NAME_REGEX = AppConstants.PACKAGE_NAME_REGEX
        private val CHARSET_REGEX = AppConstants.CHARSET_REGEX
    }

    private val template = ApkTemplate(context)
    private val templateProvider = CompositeTemplateProvider.default(context)
    private val signer = JarSigner(context)
    private val axmlEditor = AxmlEditor()
    private val axmlRebuilder = AxmlRebuilder()
    private val arscEditor = ArscEditor()
    private val arscRebuilder = ArscRebuilder()
    private val logger = BuildLogger(context)
    private val encryptedApkBuilder = EncryptedApkBuilder(context)
    private val keyManager = KeyManager.getInstance(context)


    private val outputDir = File(context.getExternalFilesDir(null), "built_apks").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "apk_build_temp").apply { mkdirs() }


    private val originalAppName = "WebToApp"
    private val originalPackageName = "com.webtoapp"





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




    fun getTempDirSize(): Long {
        return tempDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }




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







    suspend fun buildApk(
        webApp: WebApp,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): BuildResult = withContext(Dispatchers.IO) {
        var currentStage = BuildStage.PREPARE
        var currentPackageName: String? = null
        var currentUnsignedApkPath: String? = null
        var currentSignedApkPath: String? = null

        logger.startNewLog(webApp.name)

        try {
            onProgress(0, "Preparing build...")


            val encryptionConfig = webApp.apkExportConfig?.encryptionConfig?.toEncryptionConfig()
                ?: EncryptionConfig.DISABLED


            val hardeningConfig = webApp.apkExportConfig?.hardeningConfig
                ?: com.webtoapp.data.model.AppHardeningConfig()


            val perfOptEnabled = webApp.apkExportConfig?.performanceOptimization == true
            val perfConfig = if (perfOptEnabled) {
                webApp.apkExportConfig?.performanceConfig?.toOptimizerConfig()
                    ?: com.webtoapp.core.linux.PerformanceOptimizer.OptimizeConfig()
            } else null


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
            logger.logKeyValue("hardeningEnabled", hardeningConfig.enabled)

            logger.logKeyValue("performanceOptimization", perfOptEnabled)


            logger.section("APK Export Config")
            logger.logKeyValue("customPackageName", webApp.apkExportConfig?.customPackageName)
            logger.logKeyValue("customVersionCode", webApp.apkExportConfig?.customVersionCode)
            logger.logKeyValue("customVersionName", webApp.apkExportConfig?.customVersionName)


            val architecture = webApp.apkExportConfig?.architecture
                ?: com.webtoapp.data.model.ApkArchitecture.UNIVERSAL
            logger.logKeyValue("architecture", architecture.name)
            logger.logKeyValue("abiFilters", architecture.abiFilters.joinToString(", "))


            logger.section("WebView Config")
            logger.logKeyValue("hideToolbar", webApp.webViewConfig.hideToolbar)
            logger.logKeyValue("javaScriptEnabled", webApp.webViewConfig.javaScriptEnabled)
            logger.logKeyValue("desktopMode", webApp.webViewConfig.desktopMode)
            logger.logKeyValue("landscapeMode", webApp.webViewConfig.landscapeMode)
            logger.logKeyValue("userAgentMode", webApp.webViewConfig.userAgentMode.name)
            logger.logKeyValue("customUserAgent", webApp.webViewConfig.customUserAgent)
            logger.logKeyValue("userAgent(legacy)", webApp.webViewConfig.userAgent)


            logger.section("Media Config")
            logger.logKeyValue("mediaConfig", webApp.mediaConfig)
            logger.logKeyValue("mediaConfig.mediaPath", webApp.mediaConfig?.mediaPath)


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


            logger.section("Splash Screen Config")
            logger.logKeyValue("splashEnabled", webApp.splashEnabled)
            logger.logKeyValue("splashConfig.type", webApp.splashConfig?.type)
            logger.logKeyValue("splashConfig.mediaPath", webApp.splashConfig?.mediaPath)
            logger.logKeyValue("splashMediaPath (getSplashMediaPath)", webApp.getSplashMediaPath())


            logger.section("BGM Config")
            logger.logKeyValue("bgmEnabled", webApp.bgmEnabled)
            logger.logKeyValue("bgmConfig.playlist.size", webApp.bgmConfig?.playlist?.size ?: 0)


            AppLogger.d("ApkBuilder", "Build started - WebApp config:")
            AppLogger.d("ApkBuilder", "  appName=${webApp.name}")
            AppLogger.d("ApkBuilder", "  appType=${webApp.appType}")


            logger.section("Generate Package Name")
            val customPkg = webApp.apkExportConfig?.customPackageName?.takeIf {
                it.isNotBlank() &&
                it.matches(PACKAGE_NAME_REGEX)
            }
            val packageName = customPkg ?: generatePackageName(webApp.name)
            currentPackageName = packageName

            if (webApp.apkExportConfig?.customPackageName?.isNotBlank() == true && customPkg == null) {
                logger.warn("Custom package name format invalid, using auto-generated: $packageName")
            }
            logger.logKeyValue("finalPackageName", packageName)

            val config = webApp.toApkConfigWithModules(packageName, context)
            logger.logKeyValue("versionCode", config.versionCode)
            logger.logKeyValue("versionName", config.versionName)
            logger.logKeyValue("embeddedExtensionModules.size", config.embeddedExtensionModules.size)


            config.embeddedExtensionModules.forEachIndexed { index, module ->
                logger.log("  embeddedModule[$index]: id=${module.id}, name=${module.name}, enabled=${module.enabled}, runAt=${module.runAt}, codeLength=${module.code.length}")
            }

            onProgress(10, "Checking template...")
            logger.section("Parallel Resource Preparation")

            val unsignedApk = File(tempDir, "${packageName}_unsigned.apk")
            val signedApk = File(outputDir, "${sanitizeFileName(webApp.name)}_v${config.versionName}.APK")
            currentUnsignedApkPath = unsignedApk.absolutePath
            currentSignedApkPath = signedApk.absolutePath
            logger.logKeyValue("unsignedApkPath", unsignedApk.absolutePath)
            logger.logKeyValue("signedApkPath", signedApk.absolutePath)

            unsignedApk.delete()
            signedApk.delete()




            val prepStartTime = System.currentTimeMillis()
            currentStage = BuildStage.RESOURCE_PREP

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

                val templateDeferred = async {
                    getOrCreateTemplate(config)
                }


                val encKeyDeferred = async {
                    if (encryptionConfig.enabled) {
                        val signatureHash = signer.getCertificateSignatureHash()
                        keyManager.generateKeyForPackage(
                            packageName, signatureHash,
                            encryptionConfig.customPassword
                        )
                    } else null
                }


                val wpDirDeferred = async {
                    if (webApp.appType == com.webtoapp.data.model.AppType.WORDPRESS) {
                        val projectId = webApp.wordpressConfig?.projectId ?: ""
                        if (projectId.isNotEmpty()) com.webtoapp.core.wordpress.WordPressManager.getProjectDir(context, projectId) else null
                    } else null
                }
                val nodeDirDeferred = async {
                    if (webApp.appType == com.webtoapp.data.model.AppType.NODEJS_APP) {
                        val config = webApp.nodejsConfig
                        val projectId = config?.projectId ?: ""
                        if (projectId.isNotEmpty()) {
                            val runtime = com.webtoapp.core.nodejs.NodeRuntime(context)
                            val internalProjectPath = runtime.getProjectDir(projectId).absolutePath
                            config?.sourceProjectPath
                                ?.takeIf { it.isNotBlank() }
                                ?.let(runtime::resolveSourceProjectDir)
                                ?.takeIf { it.absolutePath != internalProjectPath }
                                ?.let { sourceDir ->
                                    try {
                                        runtime.syncProjectFromSource(projectId, sourceDir)
                                    } catch (e: Exception) {
                                        com.webtoapp.core.logging.AppLogger.w("ApkBuilder", "同步 Node 源项目失败: ${sourceDir.absolutePath}", e)
                                    }
                                }
                            runtime.getProjectDir(projectId)
                        } else null
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


                val mediaContentPath = if (webApp.appType == com.webtoapp.data.model.AppType.IMAGE ||
                                           webApp.appType == com.webtoapp.data.model.AppType.VIDEO) webApp.url else null
                val htmlFiles = if (webApp.appType == com.webtoapp.data.model.AppType.HTML ||
                    webApp.appType == com.webtoapp.data.model.AppType.FRONTEND
                ) webApp.htmlConfig?.files ?: emptyList() else emptyList()
                val bgmPlaylistPaths = if (webApp.bgmEnabled) webApp.bgmConfig?.playlist?.map { it.path } ?: emptyList() else emptyList()
                val bgmLrcDataList = if (webApp.bgmEnabled) webApp.bgmConfig?.playlist?.map { it.lrcData } ?: emptyList() else emptyList()
                val galleryItems = if (webApp.appType == com.webtoapp.data.model.AppType.GALLERY) webApp.galleryConfig?.items ?: emptyList() else emptyList()
                val frontendProjectDir = if (webApp.appType == com.webtoapp.data.model.AppType.FRONTEND) webApp.htmlConfig?.projectDir?.let { File(it) } else null


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


            val templateApk = prepared.templateApk
            if (templateApk == null) {
                return@withContext failBuild(
                    stage = BuildStage.TEMPLATE,
                    cause = BuildFailureCause.TEMPLATE_UNAVAILABLE,
                    message = "Failed to get template APK",
                    details = mapOf(
                        "appName" to webApp.name,
                        "appType" to webApp.appType,
                        "packageName" to packageName,
                        "engineType" to config.engineType
                    )
                )
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

            logger.section("Build Input Preflight")
            currentStage = BuildStage.INPUT_PRECHECK


            val phpBinaryPath = if (config.appType in setOf("PHP_APP", "WORDPRESS")) {
                com.webtoapp.core.wordpress.WordPressDependencyManager.getPhpExecutablePath(context)
            } else null
            val nodeBinaryPath = if (config.appType == "NODEJS_APP") {
                com.webtoapp.core.nodejs.NodeDependencyManager.getNodeLibraryPath(context)
            } else null
            val pythonBinaryPath = if (config.appType == "PYTHON_APP") {
                com.webtoapp.core.python.PythonDependencyManager.getPythonExecutablePath(context)
            } else null
            val muslLinkerPath = if (config.appType == "PYTHON_APP") {
                com.webtoapp.core.python.PythonDependencyManager.getMuslLinkerPath(context)
            } else null

            val preflight = BuildInputPreflight.check(
                BuildInputPreflightRequest(
                    appType = config.appType,
                    htmlEntryFile = config.htmlEntryFile,
                    mediaContentPath = mediaContentPath,
                    htmlFiles = htmlFiles,
                    galleryItems = galleryItems,
                    wordPressProjectDir = wordPressProjectDir,
                    nodejsProjectDir = nodejsProjectDir,
                    phpAppProjectDir = phpAppProjectDir,
                    pythonAppProjectDir = pythonAppProjectDir,
                    goAppProjectDir = goAppProjectDir,
                    frontendProjectDir = frontendProjectDir,
                    networkTrustConfig = config.networkTrustConfig,
                    phpBinaryPath = phpBinaryPath,
                    nodeBinaryPath = nodeBinaryPath,
                    pythonBinaryPath = pythonBinaryPath,
                    muslLinkerPath = muslLinkerPath
                )
            )
            logger.logKeyValue("preflightPassed", preflight.passed)
            logger.logKeyValue("preflightIssueCount", preflight.issues.size)
            preflight.issues.forEachIndexed { index, issue ->
                logger.warn("preflight[$index] ${issue.summary()}")
            }
            if (!preflight.passed) {
                return@withContext failBuild(
                    stage = BuildStage.INPUT_PRECHECK,
                    cause = BuildFailureCause.INPUT_PRECHECK_FAILED,
                    message = "Build input preflight failed: ${preflight.issues.size} issue(s)",
                    details = mapOf(
                        "appName" to webApp.name,
                        "appType" to webApp.appType,
                        "packageName" to packageName,
                        "issueCount" to preflight.issues.size,
                        "issues" to preflight.issues.joinToString(" | ") { it.summary() }
                    )
                )
            }

            logger.section("Modify APK Content")
            currentStage = BuildStage.MODIFY_APK
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


            if (!unsignedApk.exists() || unsignedApk.length() == 0L) {
                return@withContext failBuild(
                    stage = BuildStage.MODIFY_APK,
                    cause = BuildFailureCause.UNSIGNED_OUTPUT_INVALID,
                    message = "Failed to generate unsigned APK",
                    details = mapOf(
                        "unsignedApk" to unsignedApk.absolutePath,
                        "exists" to unsignedApk.exists(),
                        "sizeBytes" to unsignedApk.length()
                    )
                )
            }
            logger.logKeyValue("unsignedApkSize", "${unsignedApk.length() / 1024} KB")

            logger.section("Zip Align APK")
            val zipAligned = ZipAligner.alignInPlace(unsignedApk)
            logger.logKeyValue("zipAlign16kNativeLibs", zipAligned)
            if (!zipAligned) {
                logger.warn("ZipAlign failed; APK signing will continue with the generated artifact")
            } else if (!ZipAligner.verifyNativeLibAlignment(unsignedApk)) {
                logger.warn("One or more native libraries are not 16KB zip-aligned after ZipAlign")
            }

            logger.section("Verify APK Artifact")
            currentStage = BuildStage.ARTIFACT_VERIFY
            val artifactVerification = ApkArtifactVerifier.verify(
                ApkArtifactVerificationRequest(
                    apkFile = unsignedApk,
                    config = config,
                    encryptionEnabled = encryptionConfig.enabled,
                    htmlFiles = htmlFiles,
                    galleryItems = galleryItems,
                    wordPressProjectDir = wordPressProjectDir,
                    nodejsProjectDir = nodejsProjectDir,
                    phpAppProjectDir = phpAppProjectDir,
                    pythonAppProjectDir = pythonAppProjectDir,
                    goAppProjectDir = goAppProjectDir,
                    frontendProjectDir = frontendProjectDir
                )
            )
            logger.logKeyValue("artifactEntryCount", artifactVerification.entryCount)
            logger.logKeyValue("artifactCheckedEntryCount", artifactVerification.checkedEntryCount)
            logger.logKeyValue("artifactVerificationPassed", artifactVerification.passed)
            artifactVerification.issues.forEachIndexed { index, issue ->
                logger.warn("artifact[$index] ${issue.summary()}")
            }
            if (!artifactVerification.passed) {
                return@withContext failBuild(
                    stage = BuildStage.ARTIFACT_VERIFY,
                    cause = BuildFailureCause.ARTIFACT_VERIFICATION_FAILED,
                    message = "APK artifact verification failed: ${artifactVerification.issues.size} issue(s)",
                    details = mapOf(
                        "unsignedApk" to unsignedApk.absolutePath,
                        "appName" to webApp.name,
                        "appType" to webApp.appType,
                        "packageName" to packageName,
                        "issueCount" to artifactVerification.issues.size,
                        "issues" to artifactVerification.issues.joinToString(" | ") { it.summary() }
                    )
                )
            }


            logger.section("Sign APK")
            currentStage = BuildStage.SIGN
            logger.logKeyValue("signerType", signer.getSignerType().name)


            val signSuccess = try {
                signer.sign(unsignedApk, signedApk)
            } catch (e: Exception) {
                return@withContext failBuild(
                    stage = BuildStage.SIGN,
                    cause = BuildFailureCause.SIGNING_EXCEPTION,
                    message = "Signing failed: ${e.message ?: "Unknown error"}",
                    throwable = e,
                    details = mapOf(
                        "unsignedApk" to unsignedApk.absolutePath,
                        "signedApk" to signedApk.absolutePath,
                        "signerType" to signer.getSignerType().name
                    )
                )
            }


            if (!signedApk.exists() || signedApk.length() == 0L) {
                if (signedApk.exists()) signedApk.delete()
                return@withContext failBuild(
                    stage = BuildStage.SIGN,
                    cause = BuildFailureCause.SIGNED_OUTPUT_INVALID,
                    message = "APK signing failed: output file invalid",
                    details = mapOf(
                        "signedApk" to signedApk.absolutePath,
                        "exists" to signedApk.exists(),
                        "sizeBytes" to signedApk.length()
                    )
                )
            }

            logger.logKeyValue("signedApkSize", "${signedApk.length() / 1024} KB")

            if (!signSuccess) {


                logger.warn("ApkVerifier reported issues, but signed APK file is valid (${signedApk.length() / 1024} KB). Continuing build.")
            }

            onProgress(85, "Verifying APK...")
            currentStage = BuildStage.VERIFY
            logger.section("Verify APK")

            val parseResult = debugApkStructure(signedApk)
            logger.logKeyValue("apkPreParseResult", parseResult)
            if (!parseResult) {
                logger.warn("APK pre-parse failed, may not be installable")
            }

            onProgress(90, "Analyzing & cleaning up...")
            currentStage = BuildStage.ANALYZE_CLEANUP


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
            cleanTempFiles()

            failBuild(
                stage = currentStage,
                cause = BuildFailureCause.UNHANDLED_EXCEPTION,
                message = "Build failed: ${e.message ?: "Unknown error"}",
                throwable = e,
                details = mapOf(
                    "appName" to webApp.name,
                    "appType" to webApp.appType,
                    "packageName" to currentPackageName,
                    "unsignedApk" to currentUnsignedApkPath,
                    "signedApk" to currentSignedApkPath
                )
            )
        }
    }

    private fun failBuild(
        stage: BuildStage,
        cause: BuildFailureCause,
        message: String,
        throwable: Throwable? = null,
        details: Map<String, Any?> = emptyMap()
    ): BuildResult.Error {
        val diagnostic = BuildDiagnostic(
            stage = stage,
            cause = cause,
            details = details.filterValues { it != null }
        )

        logger.section("Build Failure Diagnostic")
        logger.logKeyValue("stage", diagnostic.stage)
        logger.logKeyValue("cause", diagnostic.cause)
        diagnostic.details.forEach { (key, value) ->
            logger.logKeyValue("context.$key", value)
        }
        logger.error(message, throwable)
        logger.endLog(false, "${stage.label}: $message")

        return BuildResult.Error(
            message = message,
            logPath = logger.getCurrentLogPath(),
            diagnostic = diagnostic
        )
    }






    private fun getOrCreateTemplate(config: ApkConfig): File? {
        return try {
            val sourceTemplate = templateProvider.getTemplateFor(config) ?: return null
            val sourceName = sourceTemplate.name
            val templateFile = File(tempDir, "base_template_${sourceName.substringBeforeLast('.')}.apk")

            val needsCopy = !templateFile.exists() ||
                templateFile.length() != sourceTemplate.length() ||
                templateFile.lastModified() < sourceTemplate.lastModified()

            if (needsCopy) {
                sourceTemplate.copyTo(templateFile, overwrite = true)
                AppLogger.i("ApkBuilder", "Template APK copied from ${templateProvider.sourceName}")
            } else {
                AppLogger.d("ApkBuilder", "Using cached template APK from ${templateProvider.sourceName}")
            }
            templateFile
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Operation failed", e)
            null
        }
    }












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
        abiFilters: List<String> = emptyList(),
        wordPressProjectDir: File? = null,
        nodejsProjectDir: File? = null,
        frontendProjectDir: File? = null,
        phpAppProjectDir: File? = null,
        pythonAppProjectDir: File? = null,
        goAppProjectDir: File? = null,
        perfConfig: com.webtoapp.core.linux.PerformanceOptimizer.OptimizeConfig? = null,
        onProgress: (Int) -> Unit
    ) {
        logger.log("modifyApk started, encryption=${encryptionConfig.enabled}, abiFilter=${abiFilters.ifEmpty { "all" }}")
        val iconBitmap = iconPath?.let { template.loadBitmap(it) }
            ?: generateDefaultIcon(config.appName, config.themeType)
        var hasConfigFile = false
        var strippedNativeLibSize = 0L
        val replacedIconPaths = mutableSetOf<String>()
        var discoveredOldIconPaths = emptySet<String>()


        val assetEncryptor = if (encryptionConfig.enabled && encryptionKey != null) {
            AssetEncryptor(encryptionKey)
        } else null

        ZipFile(sourceApk).use { zipIn ->
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->

                val entries = zipIn.entries().toList()
                    .sortedWith(compareBy<ZipEntry> { it.name != "resources.arsc" })
                val entryNames = entries.map { it.name }.toSet()

                var processedCount = 0

                entries.forEach { entry ->
                    processedCount++
                    onProgress((processedCount * 100) / entries.size)

                    when {

                        entry.name.startsWith("META-INF/") &&
                        (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") ||
                         entry.name.endsWith(".DSA") || entry.name == "META-INF/MANIFEST.MF") -> {

                        }


                        entry.name.startsWith("assets/splash_media.") -> {
                            AppLogger.d("ApkBuilder", "Skipping old splash media: ${entry.name}")
                        }











                        entry.name == "AndroidManifest.xml" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()

                            val aliasCount = config.disguiseConfig?.getAliasCount() ?: 0

                            val modifiedData = axmlRebuilder.expandAndModifyFull(
                                originalData,
                                originalPackageName,
                                config.packageName,
                                config.versionCode,
                                config.versionName,
                                aliasCount,
                                config.appName,
                                config.deepLinkHosts,
                                buildRequiredPermissions(config)
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





                        entry.name == "resources.arsc" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            val modifiedData = arscRebuilder.rebuildWithNewAppNameAndIcons(
                                originalData,
                                config.appName,
                                replaceIcons = true
                            )

                            discoveredOldIconPaths = arscRebuilder.getLastDiscoveredIconPaths()
                            logger.log("Discovered old icon paths from ARSC: $discoveredOldIconPaths")
                            writeEntryStored(zipOut, entry.name, modifiedData)
                        }


                        entry.name == ApkTemplate.CONFIG_PATH -> {
                            hasConfigFile = true
                            writeConfigEntry(zipOut, config, assetEncryptor, encryptionConfig)
                        }


                        iconBitmap != null && (isIconEntry(entry.name) || discoveredOldIconPaths.contains(entry.name)) -> {




                            val iconBytes = template.createAdaptiveForegroundIcon(iconBitmap, 432)
                            writeEntryDeflated(zipOut, entry.name, iconBytes)
                            replacedIconPaths.add(entry.name)
                            AppLogger.d("ApkBuilder", "Replaced icon entry: ${entry.name} (${iconBytes.size} bytes)")
                        }




                        entry.name.startsWith("lib/") -> {
                            val abi = entry.name.removePrefix("lib/").substringBefore("/")
                            val libName = entry.name.substringAfterLast("/")

                            when {

                                abiFilters.isNotEmpty() && !abiFilters.contains(abi) -> {
                                    AppLogger.d("ApkBuilder", "Skipping architecture: ${entry.name}")
                                }

                                !isRequiredNativeLib(libName, config.appType, config.engineType) -> {
                                    val sizeKb = if (entry.size >= 0) entry.size / 1024 else entry.compressedSize / 1024
                                    AppLogger.d("ApkBuilder", "APK slim: stripped $libName (${sizeKb} KB)")
                                    logger.log("APK slim: stripped $libName (${sizeKb} KB) - not needed for ${config.appType}")
                                    strippedNativeLibSize += if (entry.size >= 0) entry.size else entry.compressedSize
                                }
                                else -> {
                                    ZipUtils.copyEntryPreserveMethod(zipIn, zipOut, entry)
                                }
                            }
                        }


                        entry.name.startsWith("kotlin/") || entry.name == "DebugProbesKt.bin" -> {

                        }


                        isEditorOnlyAsset(entry.name, config.appType, config.engineType) -> {
                            AppLogger.d("ApkBuilder", "APK slim: stripped editor asset: ${entry.name}")
                        }


                        perfConfig != null && perfConfig.removeUnusedResources &&
                        com.webtoapp.core.linux.PerformanceOptimizer.getRemovableEntries(entry.name, config.appType) -> {
                            AppLogger.d("ApkBuilder", "Perf: removed unused resource: ${entry.name}")
                        }


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


                        else -> {
                            copyEntry(zipIn, zipOut, entry)
                        }
                    }
                }


                if (!hasConfigFile) {
                    writeConfigEntry(zipOut, config, assetEncryptor, encryptionConfig)
                }


                ensureRequiredRuntimeAssets(zipOut, config.appType, entryNames)


                if (encryptionConfig.enabled) {

                    val signatureHash = signer.getCertificateSignatureHash()
                    encryptedApkBuilder.writeEncryptionMetadata(zipOut, encryptionConfig, config.packageName, signatureHash)
                    logger.log("Encryption metadata written")
                }


                if (hardeningConfig.enabled) {
                    logger.section("App Hardening")
                    logger.log("Hardening: maximum protection enabled")
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


                if (iconBitmap != null && replacedIconPaths.isEmpty()) {
                    addIconsToApk(zipOut, iconBitmap)
                    logger.log("Added PNG mipmap icons (no existing PNG icons found in template)")
                } else if (iconBitmap != null) {
                    logger.log("Replaced ${replacedIconPaths.size} existing PNG icon entries")
                }



                if (iconBitmap != null) {
                    addAdaptiveIconPngs(zipOut, iconBitmap, entryNames)
                }






                AppLogger.d("ApkBuilder", "Splash config: splashEnabled=${config.splashEnabled}, splashMediaPath=$splashMediaPath, splashType=${config.splashType}")
                if (config.splashEnabled && splashMediaPath != null) {
                    addSplashMediaToAssets(zipOut, splashMediaPath, config.splashType, assetEncryptor, encryptionConfig)
                } else {
                    AppLogger.w("ApkBuilder", "Skipping splash embed: splashEnabled=${config.splashEnabled}, splashMediaPath=$splashMediaPath")
                }


                if (config.statusBarBackgroundType == "IMAGE" && !config.statusBarBackgroundImage.isNullOrEmpty()) {
                    addStatusBarBackgroundToAssets(zipOut, config.statusBarBackgroundImage)
                }


                if (config.bgmEnabled && bgmPlaylistPaths.isNotEmpty()) {
                    logger.log("Embedding BGM: ${bgmPlaylistPaths.size} files")
                    addBgmToAssets(zipOut, bgmPlaylistPaths, bgmLrcDataList, assetEncryptor, encryptionConfig)
                }



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


                if (config.engineType == "GECKOVIEW") {
                    logger.section("Inject GeckoView Native Libraries")
                    injectGeckoViewNativeLibs(zipOut, abiFilters)
                }
            }
        }


        if (strippedNativeLibSize > 0) {
            val savedMb = strippedNativeLibSize / 1024 / 1024
            logger.log("APK slim: total native lib savings: ${savedMb} MB")
            AppLogger.d("ApkBuilder", "APK slim: stripped ${savedMb} MB of unused native libraries")
        }

        iconBitmap?.recycle()
    }












    private fun isRequiredNativeLib(libName: String, appType: String, engineType: String): Boolean {

        if (libName == "libcrypto_engine.so" || libName == "libc++_shared.so") {
            return true
        }




        if (libName == "libapk_optimizer.so" || libName == "libcrypto_optimized.so" || libName == "libperf_engine.so" || libName == "libbrowser_kernel.so") {
            return false
        }









        if (libName == "libphp.so") {
            return appType in setOf("WORDPRESS", "PHP_APP")
        }


        if (libName == "libnode_bridge.so" || libName == "libnode.so") {
            return appType == "NODEJS_APP"
        }


        if (libName == "libpython3.so" || libName == "libmusl-linker.so") {
            return appType == "PYTHON_APP"
        }


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


        return true
    }





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










    private fun addSplashMediaToAssets(
        zipOut: ZipOutputStream,
        mediaPath: String,
        splashType: String,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed splash media: path=$mediaPath, type=$splashType, encrypt=${encryptionConfig.enabled}")

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


        val extension = if (splashType == "VIDEO") "mp4" else "png"
        val assetPath = "splash_media.$extension"
        val isVideo = splashType == "VIDEO"

        try {

            val largeFileThreshold = 10 * 1024 * 1024L

            if (encryptionConfig.enabled && encryptor != null) {

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

                if (isVideo && fileSize > largeFileThreshold) {

                    AppLogger.d("ApkBuilder", "Splash large video streaming write mode: ${fileSize / 1024 / 1024} MB")
                    writeEntryStoredStreaming(zipOut, "assets/$assetPath", mediaFile)
                } else {

                    val mediaBytes = mediaFile.readBytes()
                    writeEntryStoredSimple(zipOut, "assets/$assetPath", mediaBytes)
                    AppLogger.d("ApkBuilder", "Splash media embedded(STORED): assets/$assetPath (${mediaBytes.size} bytes)")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to embed splash media: ${e.message}", e)
        }
    }




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


            writeEntryDeflated(zipOut, "assets/statusbar_background.png", imageBytes)
            AppLogger.d("ApkBuilder", "Status bar background embedded: assets/statusbar_background.png (${imageBytes.size} bytes)")
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to embed status bar background: ${e.message}", e)
        }
    }





    private fun writeEntryStoredSimple(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        ZipUtils.writeEntryStoredSimple(zipOut, name, data)
    }






    private fun writeEntryStoredStreaming(zipOut: ZipOutputStream, name: String, file: File) {
        ZipUtils.writeEntryStoredStreaming(zipOut, name, file)
    }



    private fun ensureAligned16kNativeLib(sourceFile: File, displayName: String): File {
        if (displayName == com.webtoapp.core.nodejs.NodeDependencyManager.NODE_BINARY_NAME) {
            logger.log("ELF 16KB internal patch skipped for $displayName; APK zip entry will still be 16KB aligned")
            return sourceFile
        }

        return try {
            val result = ElfAligner16k.ensureAligned(sourceFile, File(tempDir, "elf16k"))
            when {
                result.alreadyAligned -> logger.log("ELF 16KB already aligned: $displayName")
                result.repacked -> logger.log("ELF 16KB repacked: $displayName (${sourceFile.length() / 1024} KB -> ${result.outputFile.length() / 1024} KB)")
                result.changed -> logger.log("ELF 16KB metadata patched: $displayName")
            }
            result.outputFile
        } catch (e: Exception) {
            logger.warn("ELF 16KB alignment failed for $displayName: ${e.message}; using original binary")
            sourceFile
        }
    }







    private fun addMediaContentToAssets(
        zipOut: ZipOutputStream,
        mediaPath: String,
        isVideo: Boolean,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed media content: path=$mediaPath, isVideo=$isVideo, encrypt=${encryptionConfig.enabled}")

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


        val extension = if (isVideo) "mp4" else "png"
        val assetName = "media_content.$extension"

        try {

            val largeFileThreshold = 10 * 1024 * 1024L

            if (encryptionConfig.enabled && encryptor != null) {

                if (fileSize > largeFileThreshold) {
                    AppLogger.d("ApkBuilder", "Large file encryption mode: ${fileSize / 1024 / 1024} MB")

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

                if (fileSize > largeFileThreshold) {

                    AppLogger.d("ApkBuilder", "Large file streaming write mode: ${fileSize / 1024 / 1024} MB")
                    writeEntryStoredStreaming(zipOut, "assets/$assetName", mediaFile)
                } else {

                    val mediaBytes = mediaFile.readBytes()
                    writeEntryStoredSimple(zipOut, "assets/$assetName", mediaBytes)
                    AppLogger.d("ApkBuilder", "Media content embedded(STORED): assets/$assetName (${mediaBytes.size} bytes)")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ApkBuilder", "Failed to embed media content", e)
        }
    }






    private fun addGalleryItemsToAssets(
        zipOut: ZipOutputStream,
        galleryItems: List<com.webtoapp.data.model.GalleryItem>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed ${galleryItems.size} gallery items, encrypt=${encryptionConfig.enabled}")

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


                if (encryptionConfig.enabled && encryptor != null) {
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


                item.thumbnailPath?.let { thumbPath ->
                    val thumbFile = File(thumbPath)
                    if (thumbFile.exists() && thumbFile.canRead()) {
                        val thumbAssetName = "gallery/thumb_$index.jpg"
                        val thumbBytes = thumbFile.readBytes()
                        if (encryptionConfig.enabled && encryptor != null) {
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






    private fun addWordPressFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {
        AppLogger.d("ApkBuilder", "Embedding WordPress files from: ${projectDir.absolutePath}")

        var fileCount = 0
        var totalSize = 0L


        fun addDirRecursive(dir: File, basePath: String) {
            dir.listFiles()?.forEach { file ->
                val relativePath = "$basePath/${file.name}"
                if (file.isDirectory) {
                    addDirRecursive(file, relativePath)
                } else {
                    try {
                        val assetPath = "assets/wordpress$relativePath"

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




        val phpBinary = resolvePhpBinary()
        if (phpBinary != null && phpBinary.canRead()) {
            try {
                val abi = com.webtoapp.core.wordpress.WordPressDependencyManager.getDeviceAbi()
                val alignedPhpBinary = ensureAligned16kNativeLib(phpBinary, "libphp.so")

                writeEntryStoredStreaming(zipOut, "lib/$abi/libphp.so", alignedPhpBinary)
                logger.log("PHP binary injected as native lib: lib/$abi/libphp.so (${alignedPhpBinary.length() / 1024} KB)")

                writeEntryStoredSimple(zipOut, "assets/php/$abi/php", alignedPhpBinary.readBytes())
                logger.log("PHP binary also embedded as asset: assets/php/$abi/php")
            } catch (e: Exception) {
                logger.error("Failed to embed PHP binary", e)
            }
        } else {
            logger.warn("PHP binary not found")
        }
    }






    private fun addNodeJsFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {

        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.nodeJsConfig(), logger)




        val nodeDir = com.webtoapp.core.nodejs.NodeDependencyManager.getNodeDir(context)
        val nodeBinary = File(nodeDir, com.webtoapp.core.nodejs.NodeDependencyManager.NODE_BINARY_NAME)
        if (nodeBinary.exists() && nodeBinary.canRead()) {
            try {
                val abi = nodeDir.name

                val nodeLibPath = "lib/$abi/${com.webtoapp.core.nodejs.NodeDependencyManager.NODE_BINARY_NAME}"
                val alignedNodeBinary = ensureAligned16kNativeLib(nodeBinary, com.webtoapp.core.nodejs.NodeDependencyManager.NODE_BINARY_NAME)

                writeEntryStoredStreaming(zipOut, nodeLibPath, alignedNodeBinary)
                logger.log("Node.js binary embedded as native lib: $nodeLibPath (${alignedNodeBinary.length() / 1024} KB)")
            } catch (e: Exception) {
                logger.error("Failed to embed Node.js binary", e)
            }
        } else {
            logger.warn("Node.js binary not found in cache: ${nodeBinary.absolutePath}")
        }
    }






    private fun addPhpAppFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {

        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.phpConfig(), logger)




        val phpBinary = resolvePhpBinary()
        if (phpBinary != null && phpBinary.canRead()) {
            try {
                val abi = com.webtoapp.core.wordpress.WordPressDependencyManager.getDeviceAbi()
                val alignedPhpBinary = ensureAligned16kNativeLib(phpBinary, "libphp.so")

                writeEntryStoredStreaming(zipOut, "lib/$abi/libphp.so", alignedPhpBinary)
                logger.log("PHP binary injected as native lib: lib/$abi/libphp.so (${alignedPhpBinary.length() / 1024} KB)")

                writeEntryStoredSimple(zipOut, "assets/php/$abi/php", alignedPhpBinary.readBytes())
                logger.log("PHP binary also embedded as asset: assets/php/$abi/php")
            } catch (e: Exception) {
                logger.error("Failed to embed PHP binary for PHP app", e)
            }
        } else {
            logger.warn("PHP binary not found")
        }
    }




    private fun resolvePhpBinary(): File? {

        val nativePhp = File(context.applicationInfo.nativeLibraryDir, "libphp.so")
        if (nativePhp.exists()) {
            AppLogger.d("ApkBuilder", "Using nativeLibraryDir PHP: ${nativePhp.absolutePath}")
            return nativePhp
        }

        val phpDir = com.webtoapp.core.wordpress.WordPressDependencyManager.getPhpDir(context)
        val downloaded = File(phpDir, "php")
        if (downloaded.exists()) {
            AppLogger.d("ApkBuilder", "Using downloaded PHP: ${downloaded.absolutePath}")
            return downloaded
        }
        AppLogger.w("ApkBuilder", "PHP binary not found in nativeLibraryDir or download cache")
        return null
    }





    private fun addPythonAppFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {




        val reqFile = File(projectDir, "requirements.txt")
        val sitePackages = File(projectDir, ".pypackages")
        if (reqFile.exists() && (!sitePackages.exists() || sitePackages.listFiles().isNullOrEmpty())) {


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


                logger.log("Skipping pre-install: musl linker not executable from current context (SELinux). Runtime will install deps.")
            }
        } else if (sitePackages.exists() && !sitePackages.listFiles().isNullOrEmpty()) {
            logger.log("Python .pypackages already exists (${sitePackages.listFiles()?.size} packages), skipping pre-install")
        }


        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.pythonConfig(), logger)





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


        val pythonHome = com.webtoapp.core.python.PythonDependencyManager.getPythonDir(context)
        var pythonBinary312 = File(pythonHome, "bin/python3.12")
        var pythonBinary3 = File(pythonHome, "bin/python3")
        var pythonBinary = when {
            pythonBinary312.exists() && pythonBinary312.length() > 1024 * 1024 -> pythonBinary312
            pythonBinary3.exists() && pythonBinary3.length() > 1024 * 1024 -> pythonBinary3
            else -> null
        }


        if (pythonBinary == null) {
            logger.warn("Python binary not found locally, attempting auto-download...")
            try {
                val downloadSuccess = kotlinx.coroutines.runBlocking {
                    com.webtoapp.core.python.PythonDependencyManager.downloadPythonRuntime(context)
                }
                if (downloadSuccess) {
                    logger.log("Python runtime downloaded successfully")

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

                val alignedPythonBinary = ensureAligned16kNativeLib(pythonBinary, "libpython3.so")
                writeEntryStoredStreaming(zipOut, "lib/$abi/libpython3.so", alignedPythonBinary)
                logger.log("Python binary embedded as native lib: lib/$abi/libpython3.so (${alignedPythonBinary.length() / 1024} KB, src=${pythonBinary.name})")

                writeEntryStoredSimple(zipOut, "assets/python/$abi/python3", alignedPythonBinary.readBytes())
            } catch (e: Exception) {
                logger.error("Failed to embed Python binary", e)
            }
        } else {
            logger.error("⚠️ CRITICAL: Python binary not available! The exported APK will NOT be able to run Python apps. Please ensure Python runtime is downloaded in WebToApp settings.")
            logger.warn("Python binary not found or too small: python3.12=${pythonBinary312.let { "${it.exists()}/${it.length()}" }}, python3=${pythonBinary3.let { "${it.exists()}/${it.length()}" }}")
        }


        val muslLinkerName = com.webtoapp.core.python.PythonDependencyManager.getMuslLinkerName(abi)
        val muslLinkerFile = File(pythonHome, "lib/$muslLinkerName")
        if (muslLinkerFile.exists() && muslLinkerFile.canRead()) {
            try {
                val alignedMuslLinkerFile = ensureAligned16kNativeLib(muslLinkerFile, "libmusl-linker.so")
                writeEntryStoredStreaming(zipOut, "lib/$abi/libmusl-linker.so", alignedMuslLinkerFile)
                logger.log("musl linker embedded as native lib: lib/$abi/libmusl-linker.so (${alignedMuslLinkerFile.length() / 1024} KB)")
            } catch (e: Exception) {
                logger.error("Failed to embed musl linker", e)
            }
        } else {
            logger.warn("musl linker not found: ${muslLinkerFile.absolutePath} - Python may not execute in exported APK")
        }


        val pythonLibDir = File(pythonHome, "lib")
        RuntimeAssetEmbedder.embedPythonStdlib(zipOut, pythonLibDir, logger)
    }





    private fun addGoAppFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File
    ) {
        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.goConfig(), logger)
    }






    private fun addFrontendFilesToAssets(
        zipOut: ZipOutputStream,
        projectDir: File,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>
    ) {
        RuntimeAssetEmbedder.embedProjectFiles(zipOut, projectDir, RuntimeAssetEmbedder.frontendConfig(), logger)
    }







    private fun encryptLargeFile(file: File, assetName: String, encryptor: AssetEncryptor): ByteArray {
        val fileSize = file.length()
        val maxEncryptSize = 100L * 1024 * 1024
        if (fileSize > maxEncryptSize) {
            AppLogger.w("ApkBuilder", "WARNING: Encrypting very large file ($assetName, ${fileSize / 1024 / 1024}MB). " +
                "May cause high memory usage. Consider disabling encryption for large media files.")
        }
        val mediaBytes = file.readBytes()
        return encryptor.encrypt(mediaBytes, assetName)
    }




    private fun isTextFile(fileName: String): Boolean {
        return TextFileClassifier.isTextFile(fileName)
    }







    private fun addBgmToAssets(
        zipOut: ZipOutputStream,
        bgmPaths: List<String>,
        lrcDataList: List<LrcData?>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed ${bgmPaths.size} BGM files, encrypt=${encryptionConfig.enabled}")

        bgmPaths.forEachIndexed { index, bgmPath ->
            try {
                val assetName = "bgm/bgm_$index.mp3"
                var bgmBytes: ByteArray? = null

                val bgmFile = File(bgmPath)
                if (!bgmFile.exists()) {

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
                    if (encryptionConfig.enabled && encryptor != null) {

                        val encryptedData = encryptor.encrypt(bgmBytes, assetName)
                        writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                        AppLogger.d("ApkBuilder", "BGM encrypted and embedded: assets/${assetName}.enc (${encryptedData.size} bytes)")
                    } else {

                        writeEntryStoredSimple(zipOut, "assets/$assetName", bgmBytes)
                        AppLogger.d("ApkBuilder", "BGM embedded(STORED): assets/$assetName (${bgmBytes.size} bytes)")
                    }
                }


                val lrcData = lrcDataList.getOrNull(index)
                if (lrcData != null && lrcData.lines.isNotEmpty()) {
                    val lrcContent = convertLrcDataToLrcString(lrcData)
                    val lrcAssetName = "bgm/bgm_$index.lrc"
                    val lrcBytes = lrcContent.toByteArray(Charsets.UTF_8)

                    if (encryptionConfig.enabled && encryptor != null) {
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













    private fun addHtmlFilesToAssets(
        zipOut: ZipOutputStream,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ): Int {
        AppLogger.d("ApkBuilder", "Preparing to embed ${htmlFiles.size} HTML project files")


        htmlFiles.forEachIndexed { index, file ->
            AppLogger.d("ApkBuilder", "  [$index] name=${file.name}, path=${file.path}, type=${file.type}")
        }


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


        val isComplexProject = isComplexHtmlProject(htmlFiles, htmlFilesList, jsFilesList, otherFiles)

        if (isComplexProject) {
            AppLogger.i("ApkBuilder", "Complex project detected (React/WASM/ES modules) — using PRESERVE mode (no inlining)")
            return addHtmlFilesPreserveStructure(zipOut, htmlFiles, encryptor, encryptionConfig)
        }


        AppLogger.d("ApkBuilder", "Simple project — using INLINE mode")

        var successCount = 0


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


                val encoding = detectFileEncoding(sourceFile)
                var htmlContent = com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(sourceFile, encoding)

                if (htmlContent.isEmpty()) {
                    AppLogger.w("ApkBuilder", "HTML file content is empty: ${htmlFile.path}")
                    return@forEach
                }


                htmlContent = com.webtoapp.util.HtmlProjectProcessor.processHtmlContent(
                    htmlContent = htmlContent,
                    cssContent = cssContent.takeIf { it.isNotBlank() },
                    jsContent = jsContent.takeIf { it.isNotBlank() },
                    fixPaths = true
                )


                val assetPath = "assets/html/${htmlFile.name}"
                val htmlBytes = htmlContent.toByteArray(Charsets.UTF_8)

                if (encryptionConfig.enabled && encryptor != null) {

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


        otherFiles.forEach { otherFile ->
            try {
                val sourceFile = File(otherFile.path)
                if (sourceFile.exists() && sourceFile.canRead()) {
                    val fileBytes = sourceFile.readBytes()
                    if (fileBytes.isNotEmpty()) {
                        val assetPath = "assets/html/${otherFile.name}"
                        val assetName = "html/${otherFile.name}"


                        if (encryptionConfig.enabled && encryptor != null) {
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













    private fun isComplexHtmlProject(
        allFiles: List<com.webtoapp.data.model.HtmlFile>,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
        jsFiles: List<com.webtoapp.data.model.HtmlFile>,
        @Suppress("UNUSED_PARAMETER") otherFiles: List<com.webtoapp.data.model.HtmlFile>
    ): Boolean {

        val hasWasm = allFiles.any { it.name.endsWith(".wasm", ignoreCase = true) }
        if (hasWasm) {
            AppLogger.d("ApkBuilder", "Complex project indicator: WASM files detected")
            return true
        }


        if (jsFiles.size > 3) {
            AppLogger.d("ApkBuilder", "Complex project indicator: ${jsFiles.size} JS files (>3)")
            return true
        }


        val chunkPattern = Regex("""(chunk|vendor|main|runtime|polyfill)[.\-][a-f0-9]{6,}\.js""", RegexOption.IGNORE_CASE)
        val hasChunkedJs = jsFiles.any { chunkPattern.containsMatchIn(it.name) }
        if (hasChunkedJs) {
            AppLogger.d("ApkBuilder", "Complex project indicator: chunked JS filenames detected")
            return true
        }


        val htmlUsesModules = htmlFiles.any { htmlFile ->
            try {
                val file = File(htmlFile.path)
                if (file.exists() && file.length() < 1024 * 1024) {
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


        val hasSourceMaps = allFiles.any { it.name.endsWith(".map", ignoreCase = true) }


        val hasManifest = allFiles.any {
            it.name.equals("asset-manifest.json", ignoreCase = true) ||
            it.name.equals("manifest.json", ignoreCase = true) ||
            it.name.equals(".vite-manifest.json", ignoreCase = true)
        }
        if (hasManifest) {
            AppLogger.d("ApkBuilder", "Complex project indicator: build manifest detected")
            return true
        }


        if (allFiles.size > 10 && hasSourceMaps) {
            AppLogger.d("ApkBuilder", "Complex project indicator: ${allFiles.size} files with source maps")
            return true
        }


        val jsUsesModules = jsFiles.take(2).any { jsFile ->
            try {
                val file = File(jsFile.path)
                if (file.exists() && file.length() < 512 * 1024) {
                    val content = file.readText(Charsets.UTF_8).take(5000)
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


                val shouldEncrypt = when {
                    isHtml && encryptionConfig.enabled && encryptor != null -> true
                    !isHtml && encryptionConfig.enabled && encryptor != null -> true
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




    private fun detectFileEncoding(file: File): String {
        return try {
            val bytes = file.readBytes().take(1000).toByteArray()


            when {
                bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> "UTF-8"
                bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> "UTF-16BE"
                bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> "UTF-16LE"
                else -> {

                    val content = String(bytes, Charsets.ISO_8859_1)
                    val charsetMatch = CHARSET_REGEX.find(content)
                    charsetMatch?.groupValues?.get(1)?.uppercase() ?: "UTF-8"
                }
            }
        } catch (e: Exception) {
            "UTF-8"
        }
    }




    private fun convertLrcDataToLrcString(lrcData: LrcData): String {
        val sb = StringBuilder()


        lrcData.title?.let { sb.appendLine("[ti:$it]") }
        lrcData.artist?.let { sb.appendLine("[ar:$it]") }
        lrcData.album?.let { sb.appendLine("[al:$it]") }
        sb.appendLine()


        lrcData.lines.forEach { line ->
            val minutes = line.startTime / 60000
            val seconds = (line.startTime % 60000) / 1000
            val centiseconds = (line.startTime % 1000) / 10
            sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, line.text))


            line.translation?.let { translation ->
                sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, translation))
            }
        }

        return sb.toString()
    }





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





    private fun generateDefaultIcon(appName: String, themeType: String = "AURORA"): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)


        val bgColor = getThemePrimaryColor(themeType)


        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val radius = size * 0.22f
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, bgPaint)


        val initial = appName.firstOrNull()?.uppercase() ?: "A"

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




    private fun getThemePrimaryColor(themeType: String): Int = when (themeType) {
        "AURORA"     -> 0xFF7B68EE.toInt()
        "CYBERPUNK"  -> 0xFFFF00FF.toInt()
        "SAKURA"     -> 0xFFFFB7C5.toInt()
        "OCEAN"      -> 0xFF0077B6.toInt()
        "FOREST"     -> 0xFF2D6A4F.toInt()
        "GALAXY"     -> 0xFF5C4D7D.toInt()
        "VOLCANO"    -> 0xFFD32F2F.toInt()
        "FROST"      -> 0xFF4FC3F7.toInt()
        "SUNSET"     -> 0xFFE65100.toInt()
        "MINIMAL"    -> 0xFF212121.toInt()
        "NEON_TOKYO" -> 0xFFE91E63.toInt()
        "LAVENDER"   -> 0xFF7E57C2.toInt()
        else         -> 0xFF7B68EE.toInt()
    }




    private fun getThemeOnPrimaryColor(themeType: String): Int = when (themeType) {
        "CYBERPUNK"  -> 0xFF000000.toInt()
        "SAKURA"     -> 0xFF4A1C2B.toInt()
        "FROST"      -> 0xFF00344A.toInt()
        else         -> 0xFFFFFFFF.toInt()
    }





    private fun addIconsToApk(zipOut: ZipOutputStream, bitmap: Bitmap) {

        ApkTemplate.ICON_PATHS.forEach { (path, size) ->
            val iconBytes = template.scaleBitmapToPng(bitmap, size)
            writeEntryDeflated(zipOut, path, iconBytes)
        }


        ApkTemplate.ROUND_ICON_PATHS.forEach { (path, size) ->
            val iconBytes = template.createRoundIcon(bitmap, size)
            writeEntryDeflated(zipOut, path, iconBytes)
        }
    }









    private fun addAdaptiveIconPngs(
        zipOut: ZipOutputStream,
        bitmap: Bitmap,
        existingEntryNames: Set<String>
    ) {

        val bases = listOf(
            "res/drawable/ic_launcher_foreground",
            "res/drawable/ic_launcher_foreground_new",
            "res/drawable-v24/ic_launcher_foreground",
            "res/drawable-v24/ic_launcher_foreground_new",
            "res/drawable-anydpi-v24/ic_launcher_foreground",
            "res/drawable-anydpi-v24/ic_launcher_foreground_new"
        )



        val iconBytes = template.createAdaptiveForegroundIcon(bitmap, 432)

        bases.forEach { base ->
            val pngPath = "${base}.png"
            if (!existingEntryNames.contains(pngPath)) {
                writeEntryDeflated(zipOut, pngPath, iconBytes)
                AppLogger.d("ApkBuilder", "Added adaptive icon foreground: $pngPath")
            }
        }
    }












    private fun addAdaptiveIconReplacementPngs(zipOut: ZipOutputStream, bitmap: Bitmap) {

        val iconPng = template.scaleBitmapToPng(bitmap, 512)
        writeEntryDeflated(zipOut, "res/mipmap-anydpi-v26/ic_launcher.png", iconPng)
        AppLogger.d("ApkBuilder", "Added replacement icon: res/mipmap-anydpi-v26/ic_launcher.png (512px, ${iconPng.size} bytes)")


        val roundPng = template.createRoundIcon(bitmap, 512)
        writeEntryDeflated(zipOut, "res/mipmap-anydpi-v26/ic_launcher_round.png", roundPng)
        AppLogger.d("ApkBuilder", "Added replacement icon: res/mipmap-anydpi-v26/ic_launcher_round.png (512px, ${roundPng.size} bytes)")

        logger.log("Added PNG icons at mipmap-anydpi-v26 paths (512px, replacing adaptive icon XMLs)")
    }




    private fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        ZipUtils.writeEntryDeflated(zipOut, name, data)
    }





    private fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        ZipUtils.writeEntryStored(zipOut, name, data)
    }




    private fun writeConfigEntry(
        zipOut: ZipOutputStream,
        config: ApkConfig,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Writing config file: splashEnabled=${config.splashEnabled}, splashType=${config.splashType}")
        AppLogger.d("ApkBuilder", "Config userAgentMode=${config.userAgentMode}, customUserAgent=${config.customUserAgent}")

        if (encryptionConfig.enabled && encryptor != null) {

            val configJson = template.createConfigJson(config)
            val encryptedData = encryptor.encryptJson(configJson, "app_config.json")
            writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH + ".enc", encryptedData)



            val stubJson = template.createEncryptedStubJson(config)
            val stubData = stubJson.toByteArray(Charsets.UTF_8)
            writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH, stubData)
            AppLogger.d("ApkBuilder", "Config file encrypted (minimal stub, no sensitive data)")
        } else {

            val configJson = template.createConfigJson(config)
            val data = configJson.toByteArray(Charsets.UTF_8)
            writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH, data)
        }
    }




    private fun isOptimizableAsset(entryName: String): Boolean {
        val ext = entryName.substringAfterLast('.', "").lowercase()
        return ext in setOf("png", "jpg", "jpeg", "js", "css", "svg")
    }





    private fun runtimeAssetsRequiredFor(appType: String): List<String> = when (appType) {
        "PHP_APP", "WORDPRESS" -> listOf("php_router_server.php")
        else -> emptyList()
    }


    private fun ensureRequiredRuntimeAssets(
        zipOut: ZipOutputStream,
        appType: String,
        templateEntries: Set<String>
    ) {
        val required = runtimeAssetsRequiredFor(appType)
        for (assetName in required) {
            val entryName = "assets/$assetName"
            if (entryName in templateEntries) {

                continue
            }
            try {
                val bytes = context.assets.open(assetName).use { it.readBytes() }
                writeEntryDeflated(zipOut, entryName, bytes)
                logger.log("Injected runtime asset from host APK (template missing): $entryName (${bytes.size} bytes)")
                AppLogger.i("ApkBuilder", "Runtime asset injected from host APK: $entryName (${bytes.size} bytes)")
            } catch (e: Exception) {
                logger.error("CRITICAL: required runtime asset missing in BOTH template AND host APK: $assetName", e)
                AppLogger.e("ApkBuilder", "Runtime asset injection FAILED: $assetName", e)
            }
        }
    }


    private fun isEditorOnlyAsset(entryName: String, appType: String, engineType: String): Boolean {

        if (entryName.startsWith("assets/template/")) return true

        if (entryName.startsWith("assets/sample_projects/")) return true

        if (entryName.startsWith("assets/ai/")) return true
        if (entryName == "assets/litellm_model_prices.json") return true

        if (entryName.startsWith("assets/extensions/")) return true

        if (entryName == "assets/omni.ja" && engineType != "GECKOVIEW") return true

        if (entryName == "assets/php_router_server.php" && appType !in setOf("WORDPRESS", "PHP_APP")) return true




        if (entryName.startsWith("assets/python_runtime/") && appType != "PYTHON_APP") return true


        if (entryName.startsWith("assets/go_runtime/") && appType != "GO_APP") return true


        if (entryName.startsWith("assets/help/")) return true


        if (entryName.startsWith("assets/schemas/")) return true


        if (entryName == "assets/default_config.json") return true


        if (entryName.startsWith("assets/frontend_tools/") && appType != "FRONTEND") return true


        if (entryName.startsWith("assets/nodejs_runtime/") && appType != "NODEJS_APP") return true

        return false
    }





    private fun isIconEntry(entryName: String): Boolean {

        if (ApkTemplate.ICON_PATHS.any { it.first == entryName } ||
            ApkTemplate.ROUND_ICON_PATHS.any { it.first == entryName }) {
            return true
        }



        val iconPatterns = listOf(
            "ic_launcher.png",
            "ic_launcher_round.png"


        )
        return iconPatterns.any { pattern ->
            entryName.endsWith(pattern) &&
            (entryName.contains("mipmap") || entryName.contains("drawable"))
        }
    }



















    private fun isAdaptiveIconEntry(entryName: String): Boolean {





        if ((entryName.contains("drawable")) &&
            (entryName.contains("ic_launcher_foreground") || entryName.contains("ic_launcher_foreground_new")) &&
            (entryName.endsWith(".xml") || entryName.endsWith(".jpg") || entryName.endsWith(".png"))) {
            return true
        }
        return false
    }








    private fun isAdaptiveIconDefinition(entryName: String): Boolean {
        return entryName.startsWith("res/mipmap-anydpi") &&
            (entryName.contains("ic_launcher") || entryName.contains("ic_launcher_round")) &&
            entryName.endsWith(".xml")
    }





    private fun replaceIconEntry(zipOut: ZipOutputStream, entryName: String, bitmap: Bitmap) {

        var size = ApkTemplate.ICON_PATHS.find { it.first == entryName }?.second
            ?: ApkTemplate.ROUND_ICON_PATHS.find { it.first == entryName }?.second


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

            entryName.contains("round") -> {
                template.createRoundIcon(bitmap, size)
            }

            entryName.contains("foreground") -> {
                template.createAdaptiveForegroundIcon(bitmap, size)
            }

            else -> {
                template.scaleBitmapToPng(bitmap, size)
            }
        }

        writeEntryDeflated(zipOut, entryName, iconBytes)
    }





    private fun copyEntry(zipIn: ZipFile, zipOut: ZipOutputStream, entry: ZipEntry) {
        ZipUtils.copyEntry(zipIn, zipOut, entry)
    }









    private fun generatePackageName(appName: String): String {

        val raw = appName.hashCode().let {
            if (it < 0) (-it).toString(36) else it.toString(36)
        }.take(4).padStart(4, '0')

        val segment = normalizePackageSegment(raw)

        return "com.w2a.$segment"
    }






    private fun normalizePackageSegment(segment: String): String {
        if (segment.isEmpty()) return "a"

        val chars = segment.lowercase().toCharArray()

        chars[0] = when {
            chars[0] in 'a'..'z' -> chars[0]
            chars[0] in '0'..'9' -> ('a' + (chars[0] - '0'))
            else -> 'a'
        }


        return String(chars)
    }




    private fun sanitizeFileName(name: String): String {
        return name.replace(SANITIZE_FILENAME_REGEX, "_").take(50)
    }

    private fun buildRequiredPermissions(config: ApkConfig): List<String> {
        val permissions = linkedSetOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        )

        val rp = config.runtimePermissions


        if (rp.camera) {
            permissions += "android.permission.CAMERA"
        }
        if (rp.microphone) {
            permissions += "android.permission.RECORD_AUDIO"
            permissions += "android.permission.MODIFY_AUDIO_SETTINGS"
        }
        if (rp.location) {
            permissions += "android.permission.ACCESS_COARSE_LOCATION"
            permissions += "android.permission.ACCESS_FINE_LOCATION"
        }
        if (rp.notifications) {
            permissions += "android.permission.POST_NOTIFICATIONS"
        }


        if (rp.readExternalStorage) {
            permissions += "android.permission.READ_EXTERNAL_STORAGE"
        }
        if (rp.writeExternalStorage) {
            permissions += "android.permission.WRITE_EXTERNAL_STORAGE"
        }
        if (rp.readMediaImages) {
            permissions += "android.permission.READ_MEDIA_IMAGES"
        }
        if (rp.readMediaVideo) {
            permissions += "android.permission.READ_MEDIA_VIDEO"
        }
        if (rp.readMediaAudio) {
            permissions += "android.permission.READ_MEDIA_AUDIO"
        }


        if (rp.bluetooth) {
            permissions += "android.permission.BLUETOOTH"
            permissions += "android.permission.BLUETOOTH_ADMIN"
            permissions += "android.permission.BLUETOOTH_SCAN"
            permissions += "android.permission.BLUETOOTH_CONNECT"
            permissions += "android.permission.BLUETOOTH_ADVERTISE"
        }
        if (rp.nfc) {
            permissions += "android.permission.NFC"
        }
        if (rp.wifiState) {
            permissions += "android.permission.ACCESS_WIFI_STATE"
            permissions += "android.permission.CHANGE_WIFI_STATE"
        }


        if (rp.bodySensors) {
            permissions += "android.permission.BODY_SENSORS"
            permissions += "android.permission.BODY_SENSORS_BACKGROUND"
        }
        if (rp.activityRecognition) {
            permissions += "android.permission.ACTIVITY_RECOGNITION"
        }


        if (rp.readPhoneState) {
            permissions += "android.permission.READ_PHONE_STATE"
        }
        if (rp.callPhone) {
            permissions += "android.permission.CALL_PHONE"
        }
        if (rp.readContacts) {
            permissions += "android.permission.READ_CONTACTS"
        }
        if (rp.writeContacts) {
            permissions += "android.permission.WRITE_CONTACTS"
        }
        if (rp.readCalendar) {
            permissions += "android.permission.READ_CALENDAR"
        }
        if (rp.writeCalendar) {
            permissions += "android.permission.WRITE_CALENDAR"
        }
        if (rp.readSms) {
            permissions += "android.permission.READ_SMS"
        }
        if (rp.sendSms) {
            permissions += "android.permission.SEND_SMS"
        }
        if (rp.receiveSms) {
            permissions += "android.permission.RECEIVE_SMS"
        }
        if (rp.readCallLog) {
            permissions += "android.permission.READ_CALL_LOG"
        }
        if (rp.writeCallLog) {
            permissions += "android.permission.WRITE_CALL_LOG"
        }
        if (rp.processOutgoingCalls) {
            permissions += "android.permission.PROCESS_OUTGOING_CALLS"
        }


        if (rp.foregroundService) {
            permissions += "android.permission.FOREGROUND_SERVICE"
            permissions += "android.permission.FOREGROUND_SERVICE_DATA_SYNC"
            permissions += "android.permission.FOREGROUND_SERVICE_SPECIAL_USE"
        }
        if (rp.wakeLock) {
            permissions += "android.permission.WAKE_LOCK"
        }
        if (rp.requestIgnoreBatteryOptimizations) {
            permissions += "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
        }
        if (rp.bootCompleted) {
            permissions += "android.permission.RECEIVE_BOOT_COMPLETED"
        }
        if (rp.vibration) {
            permissions += "android.permission.VIBRATE"
        }
        if (rp.installPackages) {
            permissions += "android.permission.REQUEST_INSTALL_PACKAGES"
        }
        if (rp.requestDeletePackages) {
            permissions += "android.permission.REQUEST_DELETE_PACKAGES"
        }
        if (rp.systemAlertWindow) {
            permissions += "android.permission.SYSTEM_ALERT_WINDOW"
        }

        return permissions.toList()
    }




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




    fun getBuiltApks(): List<File> {
        return outputDir.listFiles()?.filter { it.extension == "apk" } ?: emptyList()
    }




    fun deleteApk(apkFile: File): Boolean {
        return apkFile.delete()
    }




    fun clearAll() {
        outputDir.listFiles()?.forEach { it.delete() }
        tempDir.listFiles()?.forEach { it.delete() }
    }




    fun getBuildLogs(): List<File> {
        return logger.getAllLogFiles()
    }




    fun getLogDirectory(): String {
        return File(context.getExternalFilesDir(null), "build_logs").absolutePath
    }
}




fun WebApp.toApkConfig(packageName: String, context: android.content.Context? = null): ApkConfig {

    val effectiveTargetUrl = when (appType) {
        com.webtoapp.data.model.AppType.HTML -> {
            val entryFile = htmlConfig?.getValidEntryFile() ?: "index.html"
            "file:///android_asset/html/$entryFile"
        }
        com.webtoapp.data.model.AppType.IMAGE, com.webtoapp.data.model.AppType.VIDEO -> "asset://media_content"
        com.webtoapp.data.model.AppType.GALLERY -> "gallery://content"
        com.webtoapp.data.model.AppType.WORDPRESS -> "wordpress://localhost"
        com.webtoapp.data.model.AppType.NODEJS_APP -> {
            val config = nodejsConfig
            when (config?.buildMode) {
                com.webtoapp.data.model.NodeJsBuildMode.STATIC -> {

                    "file:///android_asset/nodejs_app/dist/index.html"
                }
                com.webtoapp.data.model.NodeJsBuildMode.API_BACKEND,
                com.webtoapp.data.model.NodeJsBuildMode.FULLSTACK -> {

                    "nodejs://localhost"
                }
                else -> "file:///android_asset/nodejs_app/index.html"
            }
        }
        com.webtoapp.data.model.AppType.FRONTEND -> {

            val entryFile = htmlConfig?.getValidEntryFile() ?: "index.html"
            val hasSourceProjectDir = !htmlConfig?.projectDir.isNullOrBlank()
            if (hasSourceProjectDir) {
                "file:///android_asset/frontend_app/$entryFile"
            } else {
                "file:///android_asset/html/$entryFile"
            }
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
        runtimePermissions = (apkExportConfig?.runtimePermissions ?: ApkRuntimePermissions()).let { rp ->
            var result = rp


            if (apkExportConfig?.backgroundRunEnabled == true) {
                result = result.copy(
                    foregroundService = true,
                    wakeLock = true,
                    notifications = true,
                    requestIgnoreBatteryOptimizations = true
                )
            }


            if (apkExportConfig?.notificationEnabled == true) {
                result = result.copy(
                    notifications = true,
                    foregroundService = true
                )
            }


            if (autoStartConfig?.bootStartEnabled == true) {
                result = result.copy(bootCompleted = true)
            }


            if (webViewConfig.floatingWindowConfig.enabled) {
                result = result.copy(systemAlertWindow = true)
            }


            if (forcedRunConfig?.enabled == true) {
                result = result.copy(
                    foregroundService = true,
                    wakeLock = true
                )
            }

            result
        },
        networkTrustConfig = apkExportConfig?.networkTrustConfig ?: com.webtoapp.data.model.NetworkTrustConfig(),
        activationEnabled = activationEnabled,
        activationCodes = getActivationCodeStrings(),
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
        announcementTemplate = announcement?.template?.toUiTemplate()?.type?.name ?: AnnouncementTemplateType.MINIMAL.name,
        announcementShowEmoji = announcement?.showEmoji ?: true,
        announcementAnimationEnabled = announcement?.animationEnabled ?: true,
        announcementShowOnce = announcement?.showOnce ?: true,
        announcementRequireConfirmation = announcement?.requireConfirmation ?: false,
        announcementAllowNeverShow = announcement?.allowNeverShow ?: false,
        javaScriptEnabled = webViewConfig.javaScriptEnabled,
        domStorageEnabled = webViewConfig.domStorageEnabled,
        allowFileAccess = webViewConfig.allowFileAccess,
        allowContentAccess = webViewConfig.allowContentAccess,
        cacheEnabled = webViewConfig.cacheEnabled,
        zoomEnabled = webViewConfig.zoomEnabled,
        desktopMode = webViewConfig.desktopMode,
        userAgent = webViewConfig.userAgent,
        userAgentMode = webViewConfig.userAgentMode.name,
        customUserAgent = webViewConfig.customUserAgent,


        hideToolbar = webViewConfig.hideToolbar,
        hideBrowserToolbar = webViewConfig.hideBrowserToolbar,
        showStatusBarInFullscreen = webViewConfig.showStatusBarInFullscreen,
        showNavigationBarInFullscreen = webViewConfig.showNavigationBarInFullscreen,
        showToolbarInFullscreen = webViewConfig.showToolbarInFullscreen,
        landscapeMode = webViewConfig.landscapeMode,
        orientationMode = webViewConfig.orientationMode.name,



        injectScripts = buildList {

            add(com.webtoapp.data.model.UserScript(
                name = "__kernel__",
                code = com.webtoapp.core.kernel.BrowserKernel.getBuildTimeKernelJs(),
                enabled = true,
                runAt = com.webtoapp.data.model.ScriptRunTime.DOCUMENT_START
            ))

            add(com.webtoapp.data.model.UserScript(
                name = "__perf_start__",
                code = com.webtoapp.core.perf.NativePerfEngine.getPerfJsStart(),
                enabled = true,
                runAt = com.webtoapp.data.model.ScriptRunTime.DOCUMENT_START
            ))

            add(com.webtoapp.data.model.UserScript(
                name = "__perf_end__",
                code = com.webtoapp.core.perf.NativePerfEngine.getPerfJsEnd(),
                enabled = true,
                runAt = com.webtoapp.data.model.ScriptRunTime.DOCUMENT_END
            ))

            val resolvedScripts = if (context != null && webViewConfig.injectScripts.any {
                com.webtoapp.core.script.UserScriptStorage.isFileReference(it.code)
            }) {
                com.webtoapp.core.script.UserScriptStorage.internalizeScripts(context, webViewConfig.injectScripts)
            } else {
                webViewConfig.injectScripts
            }
            addAll(resolvedScripts)
        },

        statusBarColorMode = webViewConfig.statusBarColorMode.name,
        statusBarColor = webViewConfig.statusBarColor,
        statusBarDarkIcons = webViewConfig.statusBarDarkIcons,
        statusBarBackgroundType = webViewConfig.statusBarBackgroundType.name,
        statusBarBackgroundImage = webViewConfig.statusBarBackgroundImage,
        statusBarBackgroundAlpha = webViewConfig.statusBarBackgroundAlpha,
        statusBarHeightDp = webViewConfig.statusBarHeightDp,

        statusBarColorModeDark = webViewConfig.statusBarColorModeDark.name,
        statusBarColorDark = webViewConfig.statusBarColorDark,
        statusBarDarkIconsDark = webViewConfig.statusBarDarkIconsDark,
        statusBarBackgroundTypeDark = webViewConfig.statusBarBackgroundTypeDark.name,
        statusBarBackgroundImageDark = webViewConfig.statusBarBackgroundImageDark,
        statusBarBackgroundAlphaDark = webViewConfig.statusBarBackgroundAlphaDark,
        longPressMenuEnabled = webViewConfig.longPressMenuEnabled,
        longPressMenuStyle = webViewConfig.longPressMenuStyle.name,
        adBlockToggleEnabled = webViewConfig.adBlockToggleEnabled,
        popupBlockerEnabled = webViewConfig.popupBlockerEnabled,
        popupBlockerToggleEnabled = webViewConfig.popupBlockerToggleEnabled,
        openExternalLinks = webViewConfig.openExternalLinks,

        initialScale = webViewConfig.initialScale,
        viewportMode = webViewConfig.viewportMode.name,
        customViewportWidth = webViewConfig.customViewportWidth,
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

        proxyMode = webViewConfig.proxyMode,
        proxyHost = webViewConfig.proxyHost,
        proxyPort = webViewConfig.proxyPort,
        proxyType = webViewConfig.proxyType,
        pacUrl = webViewConfig.pacUrl,
        proxyBypassRules = webViewConfig.proxyBypassRules,
        proxyUsername = webViewConfig.proxyUsername,
        proxyPassword = webViewConfig.proxyPassword,

        dnsMode = webViewConfig.dnsMode,
        dnsConfig = DnsApkConfig(
            provider = webViewConfig.dnsConfig.provider,
            customDohUrl = webViewConfig.dnsConfig.customDohUrl,
            dohMode = webViewConfig.dnsConfig.dohMode,
            bypassSystemDns = webViewConfig.dnsConfig.bypassSystemDns
        ),

        errorPageMode = webViewConfig.errorPageConfig.mode.name,
        errorPageBuiltInStyle = webViewConfig.errorPageConfig.builtInStyle.name,
        errorPageShowMiniGame = webViewConfig.errorPageConfig.showMiniGame,
        errorPageMiniGameType = webViewConfig.errorPageConfig.miniGameType.name,
        errorPageAutoRetrySeconds = webViewConfig.errorPageConfig.autoRetrySeconds,
        errorPageCustomHtml = webViewConfig.errorPageConfig.customHtml ?: "",
        errorPageCustomMediaPath = webViewConfig.errorPageConfig.customMediaPath ?: "",
        errorPageRetryButtonText = webViewConfig.errorPageConfig.retryButtonText,

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

        appType = appType.name,
        mediaEnableAudio = mediaConfig?.enableAudio ?: true,
        mediaLoop = mediaConfig?.loop ?: true,
        mediaAutoPlay = mediaConfig?.autoPlay ?: true,
        mediaFillScreen = mediaConfig?.fillScreen ?: true,
        mediaLandscape = mediaConfig?.orientation == com.webtoapp.data.model.SplashOrientation.LANDSCAPE,
        mediaKeepScreenOn = mediaConfig?.keepScreenOn ?: true,


        htmlEntryFile = htmlConfig?.getValidEntryFile() ?: "index.html",
        htmlEnableJavaScript = htmlConfig?.enableJavaScript ?: true,
        htmlEnableLocalStorage = htmlConfig?.enableLocalStorage ?: true,
        htmlLandscapeMode = htmlConfig?.landscapeMode ?: false,


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


        bgmEnabled = bgmEnabled,
        bgmPlaylist = bgmConfig?.playlist?.mapIndexed { index, item ->
            BgmShellItem(
                id = item.id,
                name = item.name,
                assetPath = "bgm/bgm_$index.mp3",
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

        themeType = themeType,
        darkMode = "SYSTEM",

        translateEnabled = translateEnabled,
        translateTargetLanguage = translateConfig?.targetLanguage?.code ?: "zh-CN",
        translateShowButton = translateConfig?.showFloatingButton ?: true,

        extensionFabIcon = extensionFabIcon ?: "",
        extensionModuleIds = extensionModuleIds,

        bootStartEnabled = autoStartConfig?.bootStartEnabled ?: false,
        scheduledStartEnabled = autoStartConfig?.scheduledStartEnabled ?: false,
        scheduledTime = autoStartConfig?.scheduledTime ?: "08:00",
        scheduledDays = autoStartConfig?.scheduledDays ?: listOf(1, 2, 3, 4, 5, 6, 7),

        forcedRunConfig = forcedRunConfig,

        isolationEnabled = apkExportConfig?.isolationConfig?.enabled ?: false,
        isolationConfig = apkExportConfig?.isolationConfig,

        backgroundRunEnabled = apkExportConfig?.backgroundRunEnabled ?: false,
        backgroundRunConfig = apkExportConfig?.backgroundRunConfig?.let {
            BackgroundRunConfig(
                notificationTitle = it.notificationTitle,
                notificationContent = it.notificationContent,
                showNotification = it.showNotification,
                keepCpuAwake = it.keepCpuAwake
            )
        },

        notificationEnabled = apkExportConfig?.notificationEnabled ?: false,
        notificationConfig = apkExportConfig?.notificationConfig?.let {
            NotificationConfig(
                type = it.type.key,
                pollUrl = it.pollUrl,
                pollIntervalMinutes = it.pollIntervalMinutes,
                pollMethod = it.pollMethod,
                pollHeaders = it.pollHeaders,
                clickUrl = it.clickUrl
            )
        },

        blackTechConfig = blackTechConfig,

        disguiseConfig = disguiseConfig,

        browserDisguiseConfig = browserDisguiseConfig,

        deviceDisguiseConfig = deviceDisguiseConfig,

        language = com.webtoapp.core.i18n.Strings.currentLanguage.value.name,

        engineType = apkExportConfig?.engineType ?: "SYSTEM_WEBVIEW",

        deepLinkEnabled = apkExportConfig?.deepLinkEnabled ?: false,
        deepLinkHosts = buildOAuthReturnHosts(
            url = url,
            customHosts = apkExportConfig?.customDeepLinkHosts ?: emptyList(),
            includeCustomHosts = apkExportConfig?.deepLinkEnabled == true
        ),

        wordpressSiteTitle = wordpressConfig?.siteTitle ?: "",
        wordpressAdminUser = wordpressConfig?.adminUser ?: "admin",
        wordpressAdminEmail = wordpressConfig?.adminEmail ?: "",
        wordpressAdminPassword = wordpressConfig?.adminPassword ?: "admin",
        wordpressThemeName = wordpressConfig?.themeName ?: "",
        wordpressPlugins = wordpressConfig?.plugins ?: emptyList(),
        wordpressActivePlugins = wordpressConfig?.activePlugins ?: emptyList(),
        wordpressPermalinkStructure = wordpressConfig?.permalinkStructure ?: "/%postname%/",
        wordpressSiteLanguage = wordpressConfig?.siteLanguage ?: "zh_CN",
        wordpressAutoInstall = wordpressConfig?.autoInstall ?: true,
        wordpressPhpPort = wordpressConfig?.phpPort ?: 0,
        wordpressLandscapeMode = wordpressConfig?.landscapeMode ?: false,


        nodejsMode = nodejsConfig?.buildMode?.name ?: "STATIC",
        nodejsPort = nodejsConfig?.serverPort ?: 0,
        nodejsEntryFile = nodejsConfig?.entryFile ?: "",
        nodejsEnvVars = nodejsConfig?.envVars ?: emptyMap(),
        nodejsLandscapeMode = nodejsConfig?.landscapeMode ?: false,


        phpAppFramework = phpAppConfig?.framework ?: "",
        phpAppDocumentRoot = phpAppConfig?.documentRoot ?: "",
        phpAppEntryFile = phpAppConfig?.entryFile ?: "index.php",
        phpAppPort = phpAppConfig?.phpPort ?: 0,
        phpAppEnvVars = phpAppConfig?.envVars ?: emptyMap(),
        phpAppLandscapeMode = phpAppConfig?.landscapeMode ?: false,


        pythonAppFramework = pythonAppConfig?.framework ?: "",
        pythonAppEntryFile = pythonAppConfig?.entryFile ?: "app.py",
        pythonAppEntryModule = pythonAppConfig?.entryModule ?: "",
        pythonAppServerType = pythonAppConfig?.serverType ?: "builtin",
        pythonAppPort = pythonAppConfig?.serverPort ?: 0,
        pythonAppEnvVars = pythonAppConfig?.envVars ?: emptyMap(),
        pythonAppLandscapeMode = pythonAppConfig?.landscapeMode ?: false,


        goAppFramework = goAppConfig?.framework ?: "",
        goAppBinaryName = goAppConfig?.binaryName ?: "",
        goAppPort = goAppConfig?.serverPort ?: 0,
        goAppStaticDir = goAppConfig?.staticDir ?: "",
        goAppEnvVars = goAppConfig?.envVars ?: emptyMap(),
        goAppLandscapeMode = goAppConfig?.landscapeMode ?: false,


        multiWebSites = multiWebConfig?.sites?.map { site ->
            com.webtoapp.core.shell.MultiWebSiteShellConfig(
                id = site.id,
                name = site.name,
                url = site.url,
                type = site.type,
                localFilePath = site.localFilePath,
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
        multiWebProjectId = multiWebConfig?.projectId ?: "",


        cloudSdkConfig = cloudConfig?.toCloudSdkConfig() ?: com.webtoapp.core.shell.CloudSdkConfig()
    )
}












private fun extractHostsFromUrl(url: String, customHosts: List<String> = emptyList()): List<String> {

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

            if (host != apex) {
                hosts.add(apex)
            }

            val wwwApex = "www.$apex"
            if (host != wwwApex) {
                hosts.add(wwwApex)
            }
        }
    } catch (_: Exception) { }


    customHosts.forEach { custom ->
        val trimmed = custom.trim().lowercase()
        if (trimmed.isNotBlank() && trimmed.contains(".")) {
            hosts.add(trimmed)
        }
    }

    return hosts.toList()
}

private fun buildOAuthReturnHosts(
    url: String,
    customHosts: List<String> = emptyList(),
    includeCustomHosts: Boolean = false
): List<String> {
    return extractHostsFromUrl(
        url = url,
        customHosts = if (includeCustomHosts) customHosts else emptyList()
    )
}






fun WebApp.toApkConfigWithModules(packageName: String, context: android.content.Context): ApkConfig {
    val baseConfig = toApkConfig(packageName, context)


    val embeddedModules = if (extensionModuleIds.isNotEmpty()) {
        try {
            val extensionManager = com.webtoapp.core.extension.ExtensionManager.getInstance(context)
            extensionManager.getModulesByIds(extensionModuleIds).map { module ->


                val resolvedCode: String
                val resolvedCss: String
                if (module.codeFiles.isNotEmpty()) {
                    val entryNames = setOf("main.js", "index.js", "app.js", "init.js", "bundle.js", "dist.js")
                    val jsFiles = module.codeFiles.entries
                        .filter { it.key.endsWith(".js", true) }
                        .sortedWith(compareByDescending<Map.Entry<String, String>> {
                            it.key.substringAfterLast("/") in entryNames
                        }.thenBy { it.key })
                    val cssFiles = module.codeFiles.entries
                        .filter { it.key.endsWith(".css", true) }
                    resolvedCode = jsFiles.joinToString("\n\n") { (path, content) ->
                        "// === $path ===\n$content"
                    }
                    resolvedCss = if (cssFiles.isNotEmpty()) {
                        val baseCss = module.cssCode
                        val mergedCss = cssFiles.joinToString("\n\n") { (path, content) ->
                            "/* === $path === */\n$content"
                        }
                        if (baseCss.isNotBlank()) "$baseCss\n\n$mergedCss" else mergedCss
                    } else {
                        module.cssCode
                    }
                } else {
                    resolvedCode = module.code
                    resolvedCss = module.cssCode
                }

                EmbeddedExtensionModule(
                    id = module.id,
                    name = module.name,
                    description = module.description,
                    icon = module.icon,
                    category = module.category.name,
                    code = resolvedCode,
                    cssCode = resolvedCss,
                    runAt = module.runAt.name,
                    urlMatches = module.urlMatches.map { rule ->
                        EmbeddedUrlMatchRule(
                            pattern = rule.pattern,
                            isRegex = rule.isRegex,
                            exclude = rule.exclude
                        )
                    },
                    configValues = module.configValues,


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




fun WebApp.getSplashMediaPath(): String? {
    return if (splashEnabled) splashConfig?.mediaPath else null
}


enum class BuildStage(val label: String) {
    PREPARE("Preparing build"),
    RESOURCE_PREP("Preparing resources"),
    INPUT_PRECHECK("Checking build inputs"),
    TEMPLATE("Loading template APK"),
    MODIFY_APK("Modifying APK"),
    ARTIFACT_VERIFY("Verifying APK artifact"),
    SIGN("Signing APK"),
    VERIFY("Verifying APK"),
    ANALYZE_CLEANUP("Analyzing and cleaning up")
}


enum class BuildFailureCause {
    TEMPLATE_UNAVAILABLE,
    INPUT_PRECHECK_FAILED,
    UNSIGNED_OUTPUT_INVALID,
    ARTIFACT_VERIFICATION_FAILED,
    SIGNING_EXCEPTION,
    SIGNED_OUTPUT_INVALID,
    UNHANDLED_EXCEPTION
}


data class BuildDiagnostic(
    val stage: BuildStage,
    val cause: BuildFailureCause,
    val details: Map<String, Any?> = emptyMap()
)




sealed class BuildResult {
    data class Success(
        val apkFile: File,
        val logPath: String? = null,
        val analysisReport: ApkAnalyzer.AnalysisReport? = null
    ) : BuildResult()
    data class Error(
        val message: String,
        val logPath: String? = null,
        val diagnostic: BuildDiagnostic? = null
    ) : BuildResult()
}
