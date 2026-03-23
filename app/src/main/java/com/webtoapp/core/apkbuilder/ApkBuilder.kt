package com.webtoapp.core.apkbuilder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
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
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*
import java.util.zip.CRC32
import javax.crypto.SecretKey

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

    private val template = ApkTemplate(context)
    private val signer = JarSigner(context)
    private val axmlEditor = AxmlEditor()
    private val axmlRebuilder = AxmlRebuilder()
    private val arscEditor = ArscEditor()
    private val arscRebuilder = ArscRebuilder()  // For unlimited app name length
    private val logger = BuildLogger(context)
    private val encryptedApkBuilder = EncryptedApkBuilder(context)
    private val keyManager = KeyManager(context)
    
    // Output directory
    private val outputDir = File(context.getExternalFilesDir(null), "built_apks").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "apk_build_temp").apply { mkdirs() }
    
    // Original app name (for replacement)
    // ArscRebuilder can handle any length, so we use simple name
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
            Log.d("ApkBuilder", "Temp files cleaned")
        } catch (e: Exception) {
            Log.e("ApkBuilder", "Failed to clean temp files", e)
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
                    Log.d("ApkBuilder", "Deleted old build: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("ApkBuilder", "Failed to clean old builds", e)
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
            Log.d("ApkBuilder", "Build started - WebApp config:")
            Log.d("ApkBuilder", "  appName=${webApp.name}")
            Log.d("ApkBuilder", "  appType=${webApp.appType}")
            
            // Generate package name
            logger.section("Generate Package Name")
            val customPkg = webApp.apkExportConfig?.customPackageName?.takeIf { 
                it.isNotBlank() && 
                it.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))
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
            logger.section("Get Template")
            
            val templateApk = getOrCreateTemplate()
            if (templateApk == null) {
                logger.error("Failed to get template APK")
                logger.endLog(false, "Failed to get template APK")
                return@withContext BuildResult.Error("Failed to get template APK")
            }
            logger.logKeyValue("templatePath", templateApk.absolutePath)
            logger.logKeyValue("templateSize", "${templateApk.length() / 1024} KB")
            
            onProgress(20, "Preparing resources...")
            logger.section("Prepare Files")
            
            val unsignedApk = File(tempDir, "${packageName}_unsigned.apk")
            val signedApk = File(outputDir, "${sanitizeFileName(webApp.name)}_v${config.versionName}.APK")
            logger.logKeyValue("unsignedApkPath", unsignedApk.absolutePath)
            logger.logKeyValue("signedApkPath", signedApk.absolutePath)
            
            unsignedApk.delete()
            signedApk.delete()
            
            onProgress(30, "Injecting config...")
            logger.section("Prepare Embedded Resources")
            
            // Get media file path
            val mediaContentPath = if (webApp.appType == com.webtoapp.data.model.AppType.IMAGE || 
                                       webApp.appType == com.webtoapp.data.model.AppType.VIDEO) {
                webApp.url
            } else {
                null
            }
            logger.logKeyValue("mediaContentPath", mediaContentPath)
            if (mediaContentPath != null) {
                val mediaFile = File(mediaContentPath)
                logger.logKeyValue("mediaFile.exists", mediaFile.exists())
                logger.logKeyValue("mediaFile.size", if (mediaFile.exists()) "${mediaFile.length() / 1024} KB" else "N/A")
            }
            
            // Get HTML file list
            val htmlFiles = if (webApp.appType == com.webtoapp.data.model.AppType.HTML) {
                webApp.htmlConfig?.files ?: emptyList()
            } else {
                emptyList()
            }
            logger.logKeyValue("htmlFiles.size", htmlFiles.size)
            htmlFiles.forEachIndexed { index, file ->
                val exists = File(file.path).exists()
                logger.log("  html[$index]: name=${file.name}, path=${file.path}, exists=$exists")
            }
            
            // Get BGM playlist original paths
            val bgmPlaylistPaths = if (webApp.bgmEnabled) {
                webApp.bgmConfig?.playlist?.map { it.path } ?: emptyList()
            } else {
                emptyList()
            }
            logger.logKeyValue("bgmPlaylistPaths.size", bgmPlaylistPaths.size)
            
            // Get BGM lyrics data
            val bgmLrcDataList = if (webApp.bgmEnabled) {
                webApp.bgmConfig?.playlist?.map { it.lrcData } ?: emptyList()
            } else {
                emptyList()
            }
            
            // Get gallery items (for GALLERY app type)
            val galleryItems = if (webApp.appType == com.webtoapp.data.model.AppType.GALLERY) {
                webApp.galleryConfig?.items ?: emptyList()
            } else {
                emptyList()
            }
            logger.logKeyValue("galleryItems.size", galleryItems.size)
            
            // Generate encryption key (if encryption enabled)
            // Important: Use JarSigner's certificate signature hash to ensure same signature for packaging and runtime
            val encryptionKey: SecretKey? = if (encryptionConfig.enabled) {
                logger.section("Generate Encryption Key")
                val signatureHash = signer.getCertificateSignatureHash()
                logger.logKeyValue("signatureHash", signatureHash.toHexString().take(32) + "...")
                keyManager.generateKeyForPackage(packageName, signatureHash).also {
                    logger.log("Encryption key generated (using target signature)")
                }
            } else null
            
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
                architecture.abiFilters
            ) { progress ->
                val msg = if (encryptionConfig.enabled) "Encrypting and processing resources..." else "Processing resources..."
                onProgress(30 + (progress * 0.4).toInt(), msg)
            }
            
            onProgress(70, "Signing APK...")
            logger.section("Sign APK")
            
            // Check if unsigned APK is valid
            if (!unsignedApk.exists() || unsignedApk.length() == 0L) {
                logger.error("Unsigned APK invalid: exists=${unsignedApk.exists()}, size=${unsignedApk.length()}")
                logger.endLog(false, "Failed to generate unsigned APK")
                return@withContext BuildResult.Error("Failed to generate unsigned APK")
            }
            logger.logKeyValue("unsignedApkSize", "${unsignedApk.length() / 1024} KB")
            
            // Log signer status
            logger.logKeyValue("signerReady", signer.isReady())
            logger.logKeyValue("signerType", signer.getSignerType().name)
            try {
                val certInfo = signer.getCertificateInfo()
                logger.log("Certificate info: $certInfo")
            } catch (e: Exception) {
                logger.warn("Failed to get certificate info: ${e.message}")
            }
            
            // Sign to temp directory first, avoid external storage permission issues
            val tempSignedApk = File(tempDir, "${packageName}_signed.apk")
            tempSignedApk.delete()
            
            // Signature
            val signSuccess = try {
                logger.log("Calling signer.sign()...")
                logger.log("Input: ${unsignedApk.absolutePath}")
                logger.log("Output(temp): ${tempSignedApk.absolutePath}")
                val result = signer.sign(unsignedApk, tempSignedApk)
                logger.log("signer.sign() returned: $result")
                result
            } catch (e: Exception) {
                logger.error("Exception during signing", e)
                logger.endLog(false, "Signing failed: ${e.message}")
                return@withContext BuildResult.Error("Signing failed: ${e.message ?: "Unknown error"}")
            }
            
            // Check signing result
            // If file exists and has content, consider success even if ApkVerifier validation fails
            // (Let Android system do final validation during installation)
            val fileExists = tempSignedApk.exists() && tempSignedApk.length() > 0
            logger.log("Post-signing file status: exists=${tempSignedApk.exists()}, size=${if (tempSignedApk.exists()) tempSignedApk.length() else 0} bytes")
            
            if (!fileExists) {
                logger.error("APK signing failed: output file doesn't exist or is empty")
                
                // Try to reset keys and retry once
                logger.log("Attempting to reset signing keys and retry...")
                if (signer.resetKeys()) {
                    logger.log("Key reset successful, retrying signing...")
                    logger.logKeyValue("newSignerType", signer.getSignerType().name)
                    
                    try {
                        signer.sign(unsignedApk, tempSignedApk)
                    } catch (e: Exception) {
                        logger.error("Retry signing failed", e)
                    }
                    
                    // Check file again
                    if (tempSignedApk.exists() && tempSignedApk.length() > 0) {
                        logger.log("Retry signing successful! File size: ${tempSignedApk.length()} bytes")
                    } else {
                        logger.error("Retry signing still failed")
                        logger.endLog(false, "APK signing failed")
                        return@withContext BuildResult.Error("APK signing failed, please try clearing app data and retry")
                    }
                } else {
                    logger.error("Key reset failed")
                    logger.endLog(false, "APK signing failed")
                    return@withContext BuildResult.Error("APK signing failed, please retry")
                }
            } else {
                if (!signSuccess) {
                    logger.warn("ApkVerifier validation failed, but file generated successfully, continuing build")
                } else {
                    logger.log("Signing successful and verified")
                }
            }
            
            // Copy to final directory
            logger.log("Copying signed APK to final directory...")
            try {
                tempSignedApk.copyTo(signedApk, overwrite = true)
                tempSignedApk.delete()
                logger.log("Copy successful: ${signedApk.absolutePath}")
            } catch (e: Exception) {
                logger.error("Failed to copy to final directory", e)
                // If copy fails, try using temp file as result
                logger.log("Trying to use temp file as result...")
                return@withContext BuildResult.Success(tempSignedApk, logger.getCurrentLogPath())
            }
            
            // Verify signed APK
            logger.logKeyValue("signedApkSize", "${signedApk.length() / 1024} KB")
            if (!signedApk.exists() || signedApk.length() == 0L) {
                logger.error("Signed APK invalid")
                logger.endLog(false, "Signed APK file invalid")
                return@withContext BuildResult.Error("Signed APK file invalid")
            }

            onProgress(85, "Verifying APK...")
            logger.section("Verify APK")
            
            val parseResult = debugApkStructure(signedApk)
            logger.logKeyValue("apkPreParseResult", parseResult)
            if (!parseResult) {
                logger.warn("APK pre-parse failed, may not be installable")
            }
            
            onProgress(90, "Cleaning temp files...")
            unsignedApk.delete()
            
            onProgress(100, "Build complete")
            
            logger.logKeyValue("finalApkPath", signedApk.absolutePath)
            logger.logKeyValue("finalApkSize", "${signedApk.length() / 1024} KB")
            logger.endLog(true, "Build successful")
            
            // Clean temp files after successful build
            cleanTempFiles()
            
            BuildResult.Success(signedApk, logger.getCurrentLogPath())
            
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
     */
    private fun getOrCreateTemplate(): File? {
        return try {
            val currentApk = File(context.applicationInfo.sourceDir)
            val templateFile = File(tempDir, "base_template.apk")
            currentApk.copyTo(templateFile, overwrite = true)
            templateFile
        } catch (e: Exception) {
            e.printStackTrace()
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
    private fun modifyApk(
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
        abiFilters: List<String> = emptyList(),  // Architecture filter, empty means no filter
        onProgress: (Int) -> Unit
    ) {
        logger.log("modifyApk started, encryption=${encryptionConfig.enabled}, abiFilter=${abiFilters.ifEmpty { "all" }}")
        val iconBitmap = iconPath?.let { template.loadBitmap(it) }
        var hasConfigFile = false
        val replacedIconPaths = mutableSetOf<String>() // Track replaced icon paths
        
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
                            Log.d("ApkBuilder", "Skipping old splash media: ${entry.name}")
                        }
                        
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
                                config.appName
                            )
                            writeEntryDeflated(zipOut, entry.name, modifiedData)
                            if (aliasCount > 0) {
                                logger.log("Added $aliasCount activity-alias (multi desktop icons)")
                            }
                        }
                        
                        // Modify resources.arsc (modify app name + icon paths)
                        // Android 11+ requires resources.arsc to be uncompressed and 4-byte aligned
                        entry.name == "resources.arsc" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            
                            // Use ArscRebuilder for unlimited app name length
                            // This rebuilds the entire string pool to accommodate any name length
                            var modifiedData = arscRebuilder.rebuildWithNewAppName(
                                originalData,
                                config.appName
                            )
                            
                            // Change @mipmap/ic_launcher* from .xml to .png for bitmap icons
                            modifiedData = arscEditor.modifyIconPathsToPng(modifiedData)
                            writeEntryStored(zipOut, entry.name, modifiedData)
                        }
                        
                        // Replace/add config file
                        entry.name == ApkTemplate.CONFIG_PATH -> {
                            hasConfigFile = true
                            writeConfigEntry(zipOut, config, assetEncryptor, encryptionConfig)
                        }
                        
                        // Replace icon (if PNG icon exists in APK)
                        iconBitmap != null && isIconEntry(entry.name) -> {
                            replaceIconEntry(zipOut, entry.name, iconBitmap)
                            replacedIconPaths.add(entry.name)
                        }
                        
                        // Filter native libraries (based on architecture config)
                        entry.name.startsWith("lib/") && abiFilters.isNotEmpty() -> {
                            // Check if architecture should be kept
                            val abi = entry.name.removePrefix("lib/").substringBefore("/")
                            if (abiFilters.contains(abi)) {
                                copyEntry(zipIn, zipOut, entry)
                            } else {
                                // Skip unwanted architecture
                                Log.d("ApkBuilder", "Skipping architecture: ${entry.name}")
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
                
                // If have icon but no PNG icon files in APK, add them
                if (iconBitmap != null && replacedIconPaths.isEmpty()) {
                    addIconsToApk(zipOut, iconBitmap)
                }

                // Add foreground PNG icons for templates using adaptive icons
                // Write unconditionally, because release APK's foreground may be compiled to different paths
                if (iconBitmap != null) {
                    addAdaptiveIconPngs(zipOut, iconBitmap, entryNames)
                }

                // Embed splash media files
                Log.d("ApkBuilder", "Splash config: splashEnabled=${config.splashEnabled}, splashMediaPath=$splashMediaPath, splashType=${config.splashType}")
                if (config.splashEnabled && splashMediaPath != null) {
                    addSplashMediaToAssets(zipOut, splashMediaPath, config.splashType, assetEncryptor, encryptionConfig)
                } else {
                    Log.w("ApkBuilder", "Skipping splash embed: splashEnabled=${config.splashEnabled}, splashMediaPath=$splashMediaPath")
                }
                
                // Embed status bar background image (if image background configured)
                if (config.statusBarBackgroundType == "IMAGE" && !config.statusBarBackgroundImage.isNullOrEmpty()) {
                    addStatusBarBackgroundToAssets(zipOut, config.statusBarBackgroundImage)
                }
                
                // Embed media app content (single media mode: image/video to APP)
                if (config.appType != "WEB" && mediaContentPath != null) {
                    logger.log("Embedding single media content: $mediaContentPath")
                    val isVideo = config.appType == "VIDEO"
                    addMediaContentToAssets(zipOut, mediaContentPath, isVideo, assetEncryptor, encryptionConfig)
                }
                
                // Embed background music files
                if (config.bgmEnabled && bgmPlaylistPaths.isNotEmpty()) {
                    logger.log("Embedding BGM: ${bgmPlaylistPaths.size} files")
                    addBgmToAssets(zipOut, bgmPlaylistPaths, bgmLrcDataList, assetEncryptor, encryptionConfig)
                }
                
                // Embed HTML files (HTML app)
                if (config.appType == "HTML" && htmlFiles.isNotEmpty()) {
                    logger.section("Embed HTML Files")
                    val embeddedCount = addHtmlFilesToAssets(zipOut, htmlFiles, assetEncryptor, encryptionConfig)
                    logger.logKeyValue("htmlFilesEmbeddedCount", embeddedCount)
                    if (embeddedCount == 0) {
                        logger.warn("HTML app failed to embed any files!")
                    }
                } else if (config.appType == "HTML") {
                    logger.warn("HTML app but htmlFiles is empty! htmlConfig=${config.htmlEntryFile}")
                }
                
                // Embed gallery items (Gallery app)
                if (config.appType == "GALLERY" && galleryItems.isNotEmpty()) {
                    logger.section("Embed Gallery Items")
                    addGalleryItemsToAssets(zipOut, galleryItems, assetEncryptor, encryptionConfig)
                    logger.logKeyValue("galleryItemsEmbeddedCount", galleryItems.size)
                } else if (config.appType == "GALLERY") {
                    logger.warn("Gallery app but galleryItems is empty!")
                }
            }
        }
        
        iconBitmap?.recycle()
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
        Log.d("ApkBuilder", "Preparing to embed splash media: path=$mediaPath, type=$splashType, encrypt=${encryptionConfig.encryptSplash}")
        
        val mediaFile = File(mediaPath)
        if (!mediaFile.exists()) {
            Log.e("ApkBuilder", "Splash media file does not exist: $mediaPath")
            return
        }
        
        if (!mediaFile.canRead()) {
            Log.e("ApkBuilder", "Splash media file cannot be read: $mediaPath")
            return
        }
        
        val fileSize = mediaFile.length()
        if (fileSize == 0L) {
            Log.e("ApkBuilder", "Splash media file is empty: $mediaPath")
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
                    Log.d("ApkBuilder", "Splash large video encryption mode: ${fileSize / 1024 / 1024} MB")
                    val encryptedData = encryptLargeFile(mediaFile, assetPath, encryptor)
                    writeEntryDeflated(zipOut, "assets/${assetPath}.enc", encryptedData)
                    Log.d("ApkBuilder", "Splash media encrypted and embedded: assets/${assetPath}.enc (${encryptedData.size} bytes)")
                } else {
                    val mediaBytes = mediaFile.readBytes()
                    val encryptedData = encryptor.encrypt(mediaBytes, assetPath)
                    writeEntryDeflated(zipOut, "assets/${assetPath}.enc", encryptedData)
                    Log.d("ApkBuilder", "Splash media encrypted and embedded: assets/${assetPath}.enc (${encryptedData.size} bytes)")
                }
            } else {
                // Non-encrypted mode
                if (isVideo && fileSize > largeFileThreshold) {
                    // Large video: use streaming write to avoid OOM
                    Log.d("ApkBuilder", "Splash large video streaming write mode: ${fileSize / 1024 / 1024} MB")
                    writeEntryStoredStreaming(zipOut, "assets/$assetPath", mediaFile)
                } else {
                    // Small file or image: normal read
                    val mediaBytes = mediaFile.readBytes()
                    writeEntryStoredSimple(zipOut, "assets/$assetPath", mediaBytes)
                    Log.d("ApkBuilder", "Splash media embedded(STORED): assets/$assetPath (${mediaBytes.size} bytes)")
                }
            }
        } catch (e: Exception) {
            Log.e("ApkBuilder", "Failed to embed splash media: ${e.message}", e)
        }
    }
    
    /**
     * Add status bar background image to assets directory
     */
    private fun addStatusBarBackgroundToAssets(
        zipOut: ZipOutputStream,
        imagePath: String
    ) {
        Log.d("ApkBuilder", "Preparing to embed status bar background: path=$imagePath")
        
        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            Log.e("ApkBuilder", "Status bar background image does not exist: $imagePath")
            return
        }
        
        if (!imageFile.canRead()) {
            Log.e("ApkBuilder", "Status bar background image cannot be read: $imagePath")
            return
        }
        
        try {
            val imageBytes = imageFile.readBytes()
            if (imageBytes.isEmpty()) {
                Log.e("ApkBuilder", "Status bar background image is empty: $imagePath")
                return
            }
            
            // Use DEFLATED compression (image doesn't need openFd)
            writeEntryDeflated(zipOut, "assets/statusbar_background.png", imageBytes)
            Log.d("ApkBuilder", "Status bar background embedded: assets/statusbar_background.png (${imageBytes.size} bytes)")
        } catch (e: Exception) {
            Log.e("ApkBuilder", "Failed to embed status bar background: ${e.message}", e)
        }
    }
    
    /**
     * Write entry (using STORED uncompressed format, simplified version)
     * For splash media etc. that need to be read by AssetManager.openFd()
     */
    private fun writeEntryStoredSimple(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()
        
        val crc = CRC32()
        crc.update(data)
        entry.crc = crc.value

        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }
    
    /**
     * Streaming write large file (using STORED uncompressed format)
     * For large video files, avoid OOM
     * Two steps: first calculate CRC, then write data
     */
    private fun writeEntryStoredStreaming(zipOut: ZipOutputStream, name: String, file: File) {
        val fileSize = file.length()
        
        // First pass: calculate CRC32 (streaming read, don't load to memory)
        val crc = CRC32()
        val buffer = ByteArray(8192)
        file.inputStream().buffered().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                crc.update(buffer, 0, bytesRead)
            }
        }
        
        // Create ZIP entry
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = fileSize
        entry.compressedSize = fileSize
        entry.crc = crc.value
        
        // Second pass: write data (streaming write)
        zipOut.putNextEntry(entry)
        file.inputStream().buffered().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                zipOut.write(buffer, 0, bytesRead)
            }
        }
        zipOut.closeEntry()
        
        Log.d("ApkBuilder", "Large file streaming embedded(STORED): $name (${fileSize / 1024} KB)")
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
        Log.d("ApkBuilder", "Preparing to embed media content: path=$mediaPath, isVideo=$isVideo, encrypt=${encryptionConfig.encryptMedia}")
        
        val mediaFile = File(mediaPath)
        if (!mediaFile.exists()) {
            Log.e("ApkBuilder", "Media file does not exist: $mediaPath")
            return
        }
        
        if (!mediaFile.canRead()) {
            Log.e("ApkBuilder", "Media file cannot be read: $mediaPath")
            return
        }
        
        val fileSize = mediaFile.length()
        if (fileSize == 0L) {
            Log.e("ApkBuilder", "Media file is empty: $mediaPath")
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
                    Log.d("ApkBuilder", "Large file encryption mode: ${fileSize / 1024 / 1024} MB")
                    // Large file: chunked read and encrypt
                    val encryptedData = encryptLargeFile(mediaFile, assetName, encryptor)
                    writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                    Log.d("ApkBuilder", "Media content encrypted and embedded: assets/${assetName}.enc (${encryptedData.size} bytes)")
                } else {
                    val mediaBytes = mediaFile.readBytes()
                    val encryptedData = encryptor.encrypt(mediaBytes, assetName)
                    writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                    Log.d("ApkBuilder", "Media content encrypted and embedded: assets/${assetName}.enc (${encryptedData.size} bytes)")
                }
            } else {
                // Non-encrypted mode
                if (fileSize > largeFileThreshold) {
                    // Large file: use streaming write to avoid OOM
                    Log.d("ApkBuilder", "Large file streaming write mode: ${fileSize / 1024 / 1024} MB")
                    writeEntryStoredStreaming(zipOut, "assets/$assetName", mediaFile)
                } else {
                    // Small file: normal read
                    val mediaBytes = mediaFile.readBytes()
                    writeEntryStoredSimple(zipOut, "assets/$assetName", mediaBytes)
                    Log.d("ApkBuilder", "Media content embedded(STORED): assets/$assetName (${mediaBytes.size} bytes)")
                }
            }
        } catch (e: Exception) {
            Log.e("ApkBuilder", "Failed to embed media content", e)
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
        Log.d("ApkBuilder", "Preparing to embed ${galleryItems.size} gallery items, encrypt=${encryptionConfig.encryptMedia}")
        
        galleryItems.forEachIndexed { index, item ->
            try {
                val mediaFile = File(item.path)
                if (!mediaFile.exists()) {
                    Log.w("ApkBuilder", "Gallery item file not found: ${item.path}")
                    return@forEachIndexed
                }
                if (!mediaFile.canRead()) {
                    Log.w("ApkBuilder", "Gallery item file cannot be read: ${item.path}")
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
                    Log.d("ApkBuilder", "Gallery item encrypted and embedded: assets/${assetName}.enc")
                } else {
                    if (isVideo && fileSize > largeFileThreshold) {
                        writeEntryStoredStreaming(zipOut, "assets/$assetName", mediaFile)
                    } else {
                        writeEntryStoredSimple(zipOut, "assets/$assetName", mediaFile.readBytes())
                    }
                    Log.d("ApkBuilder", "Gallery item embedded(STORED): assets/$assetName (${fileSize / 1024} KB)")
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
                        Log.d("ApkBuilder", "Gallery thumbnail embedded: assets/$thumbAssetName")
                    }
                }
            } catch (e: Exception) {
                Log.e("ApkBuilder", "Failed to embed gallery item ${item.path}", e)
            }
        }
    }
    
    /**
     * Encrypt large file (chunked read to avoid OOM)
     */
    private fun encryptLargeFile(file: File, assetName: String, encryptor: AssetEncryptor): ByteArray {
        // For large files, we still need to read all content for encryption
        // But use buffered stream to reduce memory peak
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024 * 1024) // 1MB buffer
        
        file.inputStream().buffered().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
        
        val mediaBytes = outputStream.toByteArray()
        return encryptor.encrypt(mediaBytes, assetName)
    }
    
    /**
     * Check if text file (can be compressed)
     */
    private fun isTextFile(fileName: String): Boolean {
        val textExtensions = setOf("html", "htm", "css", "js", "json", "xml", "txt", "svg", "md")
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in textExtensions
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
        Log.d("ApkBuilder", "Preparing to embed ${bgmPaths.size} BGM files, encrypt=${encryptionConfig.encryptBgm}")
        
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
                        Log.e("ApkBuilder", "BGM file does not exist: $bgmPath")
                        return@forEachIndexed
                    }
                } else {
                    if (!bgmFile.canRead()) {
                        Log.e("ApkBuilder", "BGM file cannot be read: $bgmPath")
                        return@forEachIndexed
                    }
                    
                    bgmBytes = bgmFile.readBytes()
                    if (bgmBytes.isEmpty()) {
                        Log.e("ApkBuilder", "BGM file is empty: $bgmPath")
                        return@forEachIndexed
                    }
                }
                
                if (bgmBytes != null) {
                    if (encryptionConfig.encryptBgm && encryptor != null) {
                        // Encrypt BGM
                        val encryptedData = encryptor.encrypt(bgmBytes, assetName)
                        writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                        Log.d("ApkBuilder", "BGM encrypted and embedded: assets/${assetName}.enc (${encryptedData.size} bytes)")
                    } else {
                        // Use STORED (uncompressed) format
                        writeEntryStoredSimple(zipOut, "assets/$assetName", bgmBytes)
                        Log.d("ApkBuilder", "BGM embedded(STORED): assets/$assetName (${bgmBytes.size} bytes)")
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
                        Log.d("ApkBuilder", "LRC encrypted and embedded: assets/${lrcAssetName}.enc")
                    } else {
                        writeEntryDeflated(zipOut, "assets/$lrcAssetName", lrcBytes)
                        Log.d("ApkBuilder", "LRC embedded: assets/$lrcAssetName")
                    }
                }
            } catch (e: Exception) {
                Log.e("ApkBuilder", "Failed to embed BGM: $bgmPath", e)
            }
        }
    }
    
    /**
     * Add HTML files to assets/html directory
     * 
     * Important fix: Inline CSS and JS into HTML files instead of as separate files
     * This avoids path reference issues when WebView loads local files
     * 
     * Enhanced features:
     * 1. Auto detect and fix resource path references
     * 2. Correctly handle file encoding
     * 3. Safely wrap JS code to ensure execution after DOM load
     * 4. Support encrypting HTML/CSS/JS files
     * 
     * @return Number of successfully embedded files
     */
    private fun addHtmlFilesToAssets(
        zipOut: ZipOutputStream,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ): Int {
        Log.d("ApkBuilder", "Preparing to embed ${htmlFiles.size} HTML project files")
        
        // Print all file paths for debugging
        htmlFiles.forEachIndexed { index, file ->
            Log.d("ApkBuilder", "  [$index] name=${file.name}, path=${file.path}, type=${file.type}")
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
            it.name.endsWith(".js", ignoreCase = true)
        }
        val otherFiles = htmlFiles.filter { file ->
            file !in htmlFilesList && file !in cssFilesList && file !in jsFilesList
        }
        
        Log.d("ApkBuilder", "File categories: HTML=${htmlFilesList.size}, CSS=${cssFilesList.size}, JS=${jsFilesList.size}, Other=${otherFiles.size}")
        
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
                Log.e("ApkBuilder", "Failed to read CSS file: ${cssFile.path}", e)
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
                Log.e("ApkBuilder", "Failed to read JS file: ${jsFile.path}", e)
                null
            }
        }.joinToString("\n\n")
        
        Log.d("ApkBuilder", "CSS content length: ${cssContent.length}, JS content length: ${jsContent.length}")
        
        // Handle HTML files using HtmlProjectProcessor
        htmlFilesList.forEach { htmlFile ->
            try {
                val sourceFile = File(htmlFile.path)
                Log.d("ApkBuilder", "Processing HTML file: ${htmlFile.path}")
                
                if (!sourceFile.exists()) {
                    Log.e("ApkBuilder", "HTML file does not exist: ${htmlFile.path}")
                    return@forEach
                }
                
                if (!sourceFile.canRead()) {
                    Log.e("ApkBuilder", "HTML file cannot be read: ${htmlFile.path}")
                    return@forEach
                }
                
                // Read HTML with correct encoding
                val encoding = detectFileEncoding(sourceFile)
                var htmlContent = com.webtoapp.util.HtmlProjectProcessor.readFileWithEncoding(sourceFile, encoding)
                
                if (htmlContent.isEmpty()) {
                    Log.w("ApkBuilder", "HTML file content is empty: ${htmlFile.path}")
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
                    Log.d("ApkBuilder", "HTML file encrypted and embedded: ${assetPath}.enc (${encryptedData.size} bytes)")
                } else {
                    writeEntryDeflated(zipOut, assetPath, htmlBytes)
                    Log.d("ApkBuilder", "HTML file embedded(inline CSS/JS): $assetPath (${htmlContent.length} bytes)")
                }
                successCount++
            } catch (e: Exception) {
                Log.e("ApkBuilder", "Failed to embed HTML file: ${htmlFile.path}", e)
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
                            Log.d("ApkBuilder", "Other file encrypted and embedded: ${assetPath}.enc (${encryptedData.size} bytes)")
                        } else {
                            writeEntryDeflated(zipOut, assetPath, fileBytes)
                            Log.d("ApkBuilder", "Other file embedded: $assetPath (${fileBytes.size} bytes)")
                        }
                        successCount++
                    }
                }
            } catch (e: Exception) {
                Log.e("ApkBuilder", "Failed to embed other file: ${otherFile.path}", e)
            }
        }
        
        Log.d("ApkBuilder", "HTML files embedding complete: $successCount/${htmlFiles.size} successful")
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
                    val charsetMatch = Regex("""charset=["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE).find(content)
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
                Log.e(
                    "ApkBuilder",
                    "getPackageArchiveInfo returned null, cannot parse APK: ${apkFile.absolutePath}"
                )
                false
            } else {
                Log.d(
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
            Log.e("ApkBuilder", "Exception while debug parsing APK: ${apkFile.absolutePath}", e)
            false
        }
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
     * and work with ArscEditor.modifyIconPathsToPng to switch paths from .xml/.jpg to .png
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
                Log.d("ApkBuilder", "Added adaptive icon foreground: $pngPath")
            }
        }
    }
    
    /**
     * Write entry (using DEFLATED compression format)
     */
    private fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.DEFLATED
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }

    /**
     * Write entry (using STORED uncompressed format)
     * For resources.arsc, to satisfy Android R+ uncompressed and 4-byte alignment requirements
     */
    private fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()
        
        // Android 11+ requires resources.arsc data to be 4-byte aligned in APK
        // Since we ensure resources.arsc is the first entry, we can use extra field for alignment padding
        if (name == "resources.arsc") {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            val baseHeaderSize = 30 // ZIP local file header fixed length
            val base = baseHeaderSize + nameBytes.size
            // extra total length = 4(custom header) + padLen
            // Need (base + extraLen) % 4 == 0
            val padLen = (4 - (base + 4) % 4) % 4
            if (padLen > 0) {
                // Use 0xFFFF as private extra header ID
                val extra = ByteArray(4 + padLen)
                extra[0] = 0xFF.toByte()
                extra[1] = 0xFF.toByte()
                // data size = padLen (little-endian)
                extra[2] = (padLen and 0xFF).toByte()
                extra[3] = ((padLen shr 8) and 0xFF).toByte()
                // Remaining pad bytes default to 0
                entry.extra = extra
            }
        }

        val crc = CRC32()
        crc.update(data)
        entry.crc = crc.value

        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
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
        Log.d("ApkBuilder", "Writing config file: splashEnabled=${config.splashEnabled}, splashType=${config.splashType}")
        Log.d("ApkBuilder", "Config JSON content: $configJson")
        
        if (encryptionConfig.encryptConfig && encryptor != null) {
            // Encrypt config file
            val encryptedData = encryptor.encryptJson(configJson, "app_config.json")
            writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH + ".enc", encryptedData)
            Log.d("ApkBuilder", "Config file encrypted")
        } else {
            // Write plaintext
            val data = configJson.toByteArray(Charsets.UTF_8)
            writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH, data)
        }
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
            "ic_launcher_round.png",
            "ic_launcher_foreground.png",
            "ic_launcher_background.png"
        )
        return iconPatterns.any { pattern ->
            entryName.endsWith(pattern) && 
            (entryName.contains("mipmap") || entryName.contains("drawable"))
        }
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
        val data = zipIn.getInputStream(entry).readBytes()
        writeEntryDeflated(zipOut, entry.name, data)
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
        return name.replace(Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]"), "_").take(50)
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
            e.printStackTrace()
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
        com.webtoapp.data.model.AppType.GALLERY -> "gallery://content"  // Gallery应用不需要targetUrl
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
        landscapeMode = webViewConfig.landscapeMode,
        injectScripts = webViewConfig.injectScripts,
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
        language = com.webtoapp.core.i18n.Strings.currentLanguage.value.name
    )
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
            android.util.Log.e("ApkBuilder", "Failed to get extension module data", e)
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
    data class Success(val apkFile: File, val logPath: String? = null) : BuildResult()
    data class Error(val message: String) : BuildResult()
}
