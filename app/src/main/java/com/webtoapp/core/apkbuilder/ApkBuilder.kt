package com.webtoapp.core.apkbuilder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.webtoapp.core.shell.BgmShellItem
import com.webtoapp.core.shell.LrcShellTheme
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*
import java.util.zip.CRC32

/**
 * APK 构建器
 * 负责将 WebApp 配置打包成独立的 APK 安装包
 * 
 * 工作原理：
 * 1. 复制当前应用 APK 作为模板（因为当前应用支持 Shell 模式）
 * 2. 注入 app_config.json 配置文件到 assets 目录
 * 3. 修改 AndroidManifest.xml 中的包名（使每个导出的 App 独立）
 * 4. 修改 resources.arsc 中的应用名称
 * 5. 替换图标资源
 * 6. 重新签名
 */
class ApkBuilder(private val context: Context) {

    private val template = ApkTemplate(context)
    private val signer = JarSigner(context)
    private val axmlEditor = AxmlEditor()
    private val arscEditor = ArscEditor()
    
    // 输出目录
    private val outputDir = File(context.getExternalFilesDir(null), "built_apks").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "apk_build_temp").apply { mkdirs() }
    
    // 原始应用名（用于替换）
    // 使用零宽空格扩展长度，显示为"WebToApp"但有足够字节空间（UTF-8约98字节，支持约30个中文字符）
    private val originalAppName = "WebToApp\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B\u200B"
    private val originalPackageName = "com.webtoapp"

    // 模板 APK 中常见的 Launcher 图标路径（按 dpi 列出），用于生成合适尺寸的 PNG
    private val ICON_PATHS = listOf(
        "res/mipmap-mdpi-v4/ic_launcher.png" to 48,
        "res/mipmap-hdpi-v4/ic_launcher.png" to 72,
        "res/mipmap-xhdpi-v4/ic_launcher.png" to 96,
        "res/mipmap-xxhdpi-v4/ic_launcher.png" to 144,
        "res/mipmap-xxxhdpi-v4/ic_launcher.png" to 192
    )

    private val ROUND_ICON_PATHS = listOf(
        "res/mipmap-mdpi-v4/ic_launcher_round.png" to 48,
        "res/mipmap-hdpi-v4/ic_launcher_round.png" to 72,
        "res/mipmap-xhdpi-v4/ic_launcher_round.png" to 96,
        "res/mipmap-xxhdpi-v4/ic_launcher_round.png" to 144,
        "res/mipmap-xxxhdpi-v4/ic_launcher_round.png" to 192
    )

    // 记录当前构建过程中已写入的 ZIP 条目，避免 duplicate entry
    private val writtenEntryNames = mutableSetOf<String>()

    /**
     * 构建 APK
     * @param webApp WebApp 配置
     * @param themeType 主题类型名称（对应 AppThemeType 枚举的 name）
     * @param onProgress 进度回调 (0-100)
     * @return 构建结果
     */
    suspend fun buildApk(
        webApp: WebApp,
        themeType: String = "AURORA",
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): BuildResult = withContext(Dispatchers.IO) {
        try {
            onProgress(0, "准备构建...")
            
            // 调试日志：检查 WebApp 的启动画面配置
            Log.d("ApkBuilder", "构建开始 - WebApp配置:")
            Log.d("ApkBuilder", "  splashEnabled=${webApp.splashEnabled}")
            Log.d("ApkBuilder", "  splashConfig=${webApp.splashConfig}")
            Log.d("ApkBuilder", "  splashMediaPath=${webApp.getSplashMediaPath()}")
            
            // 生成包名（优先使用自定义包名，否则基于应用名自动生成）
            // 注意：包名长度必须正好等于原始包名 "com.webtoapp"（12字符），否则 AXML 修改会失败
            val customPkg = webApp.apkExportConfig?.customPackageName?.lowercase()?.takeIf { pkg ->
                pkg.isNotBlank() && 
                pkg.length <= originalPackageName.length &&  // 初步长度检查
                pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))
            }?.let { pkg ->
                // 如果包名短于 12 字符，自动填充到 12 字符
                padPackageName(pkg, originalPackageName.length)
            }
            val packageName = customPkg ?: generatePackageName(webApp.name)
            
            Log.d("ApkBuilder", "包名配置: custom=${webApp.apkExportConfig?.customPackageName}, " +
                    "used=$packageName, maxLen=${originalPackageName.length}")
            Log.d("ApkBuilder", "版本配置: code=${webApp.apkExportConfig?.customVersionCode}, " +
                    "name=${webApp.apkExportConfig?.customVersionName}")
            
            val config = webApp.toApkConfig(packageName, themeType)
            
            onProgress(10, "检查模板...")
            
            // 获取或创建模板
            val templateApk = getOrCreateTemplate()
                ?: return@withContext BuildResult.Error("无法获取模板 APK")
            
            onProgress(20, "准备资源...")
            
            // 准备临时文件
            val unsignedApk = File(tempDir, "${packageName}_unsigned.apk")
            val signedApk = File(outputDir, "${sanitizeFileName(webApp.name)}_v${config.versionName}.apk")
            
            // 清理旧文件
            unsignedApk.delete()
            signedApk.delete()
            
            onProgress(30, "注入配置...")
            
            // 获取媒体文件路径（媒体应用使用 url 字段存储媒体路径）
            val mediaContentPath = if (webApp.appType == com.webtoapp.data.model.AppType.IMAGE || 
                                       webApp.appType == com.webtoapp.data.model.AppType.VIDEO) {
                webApp.url // 媒体应用的 url 字段存储的是媒体文件路径
            } else {
                null
            }
            
            // 获取 HTML 文件列表（HTML应用）
            val htmlFiles = if (webApp.appType == com.webtoapp.data.model.AppType.HTML) {
                webApp.htmlConfig?.files ?: emptyList()
            } else {
                emptyList()
            }

            // 加载自定义图标（如果有）
            val iconBitmap = loadIconBitmap(webApp)
            if (iconBitmap == null) {
                Log.d("ApkBuilder", "未配置自定义图标或加载失败，将使用模板图标: iconPath=${webApp.iconPath}")
            } else {
                Log.d("ApkBuilder", "已加载自定义图标: path=${webApp.iconPath}, size=${iconBitmap.width}x${iconBitmap.height}")
            }
            
            // 获取 BGM 播放列表的原始路径
            val bgmPlaylistPaths = if (webApp.bgmEnabled) {
                webApp.bgmConfig?.playlist?.map { it.path } ?: emptyList()
            } else {
                emptyList()
            }
            
            // 获取 BGM 歌词数据
            val bgmLrcDataList = if (webApp.bgmEnabled) {
                webApp.bgmConfig?.playlist?.map { it.lrcData } ?: emptyList()
            } else {
                emptyList()
            }
            
            // 修改 APK 内容
            modifyApk(
                templateApk, unsignedApk, config,
                webApp.getSplashMediaPath(), mediaContentPath,
                bgmPlaylistPaths, bgmLrcDataList, htmlFiles, iconBitmap
            ) { progress ->
                onProgress(30 + (progress * 0.4).toInt(), "处理资源...")
            }

            onProgress(70, "签名 APK...")
            
            // 检查未签名 APK 是否有效
            if (!unsignedApk.exists() || unsignedApk.length() == 0L) {
                Log.e("ApkBuilder", "未签名 APK 无效: exists=${unsignedApk.exists()}, size=${unsignedApk.length()}")
                return@withContext BuildResult.Error("生成未签名 APK 失败")
            }
            
            Log.d("ApkBuilder", "未签名 APK 准备完成: size=${unsignedApk.length()}")
            
            // 签名（带重试和详细错误信息）
            val signSuccess = try {
                signer.sign(unsignedApk, signedApk)
            } catch (e: Exception) {
                Log.e("ApkBuilder", "签名过程发生异常", e)
                return@withContext BuildResult.Error("签名失败: ${e.message ?: "未知错误"}")
            }
            
            if (!signSuccess) {
                Log.e("ApkBuilder", "APK 签名返回失败")
                // 清理可能的部分输出文件
                if (signedApk.exists()) {
                    signedApk.delete()
                }
                return@withContext BuildResult.Error("APK 签名失败，请重试")
            }
            
            // 验证签名后的 APK
            if (!signedApk.exists() || signedApk.length() == 0L) {
                Log.e("ApkBuilder", "签名后 APK 无效")
                return@withContext BuildResult.Error("签名后 APK 文件无效")
            }

            onProgress(85, "验证 APK...")

            // 调试：在安装前用 PackageManager 预解析一次 APK，检查包信息
            val parseResult = debugApkStructure(signedApk)
            if (!parseResult) {
                Log.w("ApkBuilder", "APK 预解析失败，可能无法安装")
                // 不返回错误，让用户尝试安装看具体错误
            }

            // 图标自检：对比 APK 内图标和用户图标
            debugIconSelfCheck(signedApk, iconBitmap)

            // 释放图标 Bitmap
            iconBitmap?.recycle()

            onProgress(90, "清理临时文件...")
            
            // 清理
            unsignedApk.delete()
            
            onProgress(100, "构建完成")
            
            Log.d("ApkBuilder", "构建成功: ${signedApk.absolutePath}, size=${signedApk.length()}")
            BuildResult.Success(signedApk)
            
        } catch (e: Exception) {
            Log.e("ApkBuilder", "构建过程发生异常", e)
            BuildResult.Error("构建失败: ${e.message ?: "未知错误"}")
        }
    }

    /**
     * 获取模板 APK
     * 使用当前应用作为模板（因为已支持 Shell 模式）
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
     * 修改 APK 内容
     * 1. 注入配置文件
     * 2. 修改包名
     * 3. 修改应用名
     * 4. 嵌入启动画面媒体
     * 5. 嵌入媒体应用内容
     */
    private fun modifyApk(
        sourceApk: File,
        outputApk: File,
        config: ApkConfig,
        splashMediaPath: String?,
        mediaContentPath: String? = null,
        bgmPlaylistPaths: List<String> = emptyList(),
        bgmLrcDataList: List<LrcData?> = emptyList(),
        htmlFiles: List<com.webtoapp.data.model.HtmlFile> = emptyList(),
        iconBitmap: Bitmap? = null,
        onProgress: (Int) -> Unit
    ) {
        var hasConfigFile = false
        // 每次构建前重置已写入条目集合
        writtenEntryNames.clear()
        // 记录在循环过程中实际被替换过的 PNG 图标路径，用于判断是否需要主动添加整套 PNG 图标
        val replacedIconPaths = mutableSetOf<String>()
        
        ZipFile(sourceApk).use { zipIn ->
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                // 为满足 Android R+ 要求，将 resources.arsc 作为第一个条目写入
                val entries = zipIn.entries().toList()
                    .sortedWith(compareBy<ZipEntry> { it.name != "resources.arsc" })
                val entryNames = entries.map { it.name }.toSet()

                var processedCount = 0
                
                entries.forEach { entry ->
                    processedCount++
                    onProgress((processedCount * 100) / entries.size)
                    
                    when {
                        // 跳过签名文件（将重新签名）
                        entry.name.startsWith("META-INF/") && 
                        (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") || 
                         entry.name.endsWith(".DSA") || entry.name == "META-INF/MANIFEST.MF") -> {
                            // 跳过
                        }
                        
                        // 跳过旧的启动画面媒体文件（将在后面重新添加）
                        entry.name.startsWith("assets/splash_media.") -> {
                            Log.d("ApkBuilder", "跳过旧启动画面媒体: ${entry.name}")
                        }

                        // 修改 AndroidManifest.xml（修改包名 + 版本号）
                        entry.name == "AndroidManifest.xml" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            // 1. 修改包名
                            var modifiedData = axmlEditor.modifyPackageName(originalData, config.packageName)
                            // 2. 修改版本号和版本名
                            modifiedData = axmlEditor.modifyVersion(modifiedData, config.versionCode, config.versionName)
                            writeEntryDeflated(zipOut, entry.name, modifiedData)
                        }
                        
                        // 修改 resources.arsc（修改应用名 + 图标路径）
                        // Android 11+ 要求 resources.arsc 必须未压缩且 4 字节对齐
                        entry.name == "resources.arsc" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            var modifiedData = arscEditor.modifyAppName(
                                originalData,
                                originalAppName,
                                config.appName
                            )
                            // 与 AppCloner 保持一致：强制将 Adaptive Icon 相关路径从 .xml 改为 .png
                            modifiedData = arscEditor.forceReplaceIconPaths(modifiedData)
                            writeEntryStored(zipOut, entry.name, modifiedData)
                        }

                        // 注意：不再处理 Adaptive Icon XML！
                        // 旧版本的正确做法是：保留 XML 定义，只通过 ARSC 路径替换来改变 foreground
                        // 这样 Adaptive Icon 结构保持完整，系统会正确解析 foreground 和 background

                        // 使用自定义图标替换 Launcher 图标 PNG
                        iconBitmap != null && isIconEntry(entry.name) -> {
                            Log.d("ApkBuilder", "替换图标资源: ${entry.name}")
                            replaceIconEntry(zipOut, entry.name, iconBitmap)
                            replacedIconPaths.add(entry.name)
                        }
                        
                        // 替换/添加配置文件
                        entry.name == ApkTemplate.CONFIG_PATH -> {
                            hasConfigFile = true
                            writeConfigEntry(zipOut, config)
                        }
                        
                        // 其他文件照常复制
                        else -> {
                            copyEntry(zipIn, zipOut, entry)
                        }
                    }
                }
                
                // 如果原 APK 没有配置文件，添加一个
                if (!hasConfigFile) {
                    Log.d("ApkBuilder", "原 APK 没有配置文件，添加新配置文件到: ${ApkTemplate.CONFIG_PATH}")
                    writeConfigEntry(zipOut, config)
                } else {
                    Log.d("ApkBuilder", "已替换原有配置文件")
                }

                // 嵌入启动画面媒体文件
                Log.d("ApkBuilder", "启动画面配置: splashEnabled=${config.splashEnabled}, splashMediaPath=$splashMediaPath, splashType=${config.splashType}")
                if (config.splashEnabled && splashMediaPath != null) {
                    addSplashMediaToAssets(zipOut, splashMediaPath, config.splashType)
                } else {
                    Log.w("ApkBuilder", "跳过嵌入启动画面: splashEnabled=${config.splashEnabled}, splashMediaPath=$splashMediaPath")
                }
                
                // 嵌入媒体应用内容（图片/视频转APP）
                if (config.appType != "WEB" && mediaContentPath != null) {
                    val isVideo = config.appType == "VIDEO"
                    addMediaContentToAssets(zipOut, mediaContentPath, isVideo)
                }
                
                // 嵌入背景音乐文件
                if (config.bgmEnabled && bgmPlaylistPaths.isNotEmpty()) {
                    addBgmToAssets(zipOut, bgmPlaylistPaths, bgmLrcDataList)
                }
                
                // 嵌入 HTML 文件（HTML应用）
                if (config.appType == "HTML" && htmlFiles.isNotEmpty()) {
                    addHtmlFilesToAssets(zipOut, htmlFiles)
                }

                // 图标处理逻辑：与 AppCloner/旧项目保持一致
                if (iconBitmap != null) {
                    // 如果循环过程中没有任何 PNG 图标被实际替换，说明模板 APK 里没有可直接替换的 PNG 图标
                    // 这种情况下，主动补齐一整套 PNG 图标（mipmap-*/ic_launcher*.png）
                    if (replacedIconPaths.isEmpty()) {
                        addMissingIconPngs(zipOut, iconBitmap, entryNames)
                    }

                    // 无论是否替换过 PNG 图标，都为 adaptive icon 写入前景 PNG（ic_launcher_foreground.png 等）
                    addAdaptiveIconPngs(zipOut, iconBitmap, entryNames)
                }
            }
        }
    }

    /**
     * 将启动画面媒体文件添加到 assets 目录
     * 
     * 重要：必须使用 STORED（未压缩）方式存储！
     * 因为 AssetManager.openFd() 只支持未压缩的 assets 文件。
     * 如果使用 DEFLATED 压缩，openFd() 会抛出 FileNotFoundException。
     */
    private fun addSplashMediaToAssets(
        zipOut: ZipOutputStream,
        mediaPath: String,
        splashType: String
    ) {
        Log.d("ApkBuilder", "准备嵌入启动画面媒体: path=$mediaPath, type=$splashType")
        
        val mediaFile = File(mediaPath)
        if (!mediaFile.exists()) {
            Log.e("ApkBuilder", "启动画面媒体文件不存在: $mediaPath")
            return
        }
        
        if (!mediaFile.canRead()) {
            Log.e("ApkBuilder", "启动画面媒体文件无法读取: $mediaPath")
            return
        }

        // 根据类型确定文件名
        val extension = if (splashType == "VIDEO") "mp4" else "png"
        val assetPath = "assets/splash_media.$extension"

        try {
            val mediaBytes = mediaFile.readBytes()
            if (mediaBytes.isEmpty()) {
                Log.e("ApkBuilder", "启动画面媒体文件内容为空: $mediaPath")
                return
            }
            
            // 使用 STORED（未压缩）方式存储，确保 AssetManager.openFd() 可以读取
            // 这是关键修复：openFd() 只支持未压缩的 assets 文件！
            writeEntryStoredSimple(zipOut, assetPath, mediaBytes)
            Log.d("ApkBuilder", "启动画面媒体已嵌入(STORED): $assetPath (${mediaBytes.size} bytes)")
        } catch (e: Exception) {
            Log.e("ApkBuilder", "嵌入启动画面媒体失败: ${e.message}", e)
        }
    }
    
    /**
     * 写入条目（使用 STORED 未压缩格式，简化版本）
     * 用于启动画面媒体等需要被 AssetManager.openFd() 读取的文件
     */
    private fun writeEntryStoredSimple(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        if (!writtenEntryNames.add(name)) {
            Log.w("ApkBuilder", "检测到重复条目(忽略写入 STORED simple): $name")
            return
        }
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
     * 将媒体应用内容添加到 assets 目录
     * 使用 STORED（未压缩）方式，以支持 AssetManager.openFd()
     */
    private fun addMediaContentToAssets(
        zipOut: ZipOutputStream,
        mediaPath: String,
        isVideo: Boolean
    ) {
        Log.d("ApkBuilder", "准备嵌入媒体应用内容: path=$mediaPath, isVideo=$isVideo")
        
        val mediaFile = File(mediaPath)
        if (!mediaFile.exists()) {
            Log.e("ApkBuilder", "媒体文件不存在: $mediaPath")
            return
        }
        
        if (!mediaFile.canRead()) {
            Log.e("ApkBuilder", "媒体文件无法读取: $mediaPath")
            return
        }

        // 根据类型确定文件名
        val extension = if (isVideo) "mp4" else "png"
        val assetPath = "assets/media_content.$extension"

        try {
            val mediaBytes = mediaFile.readBytes()
            
            if (mediaBytes.isEmpty()) {
                Log.e("ApkBuilder", "媒体文件内容为空: $mediaPath")
                return
            }
            
            writeEntryStoredSimple(zipOut, assetPath, mediaBytes)
            Log.d("ApkBuilder", "媒体应用内容已嵌入: $assetPath (${mediaBytes.size} bytes)")
        } catch (e: Exception) {
            Log.e("ApkBuilder", "嵌入媒体应用内容失败", e)
        }
    }
    
    /**
     * 将背景音乐文件添加到 assets/bgm 目录
     * 使用 STORED（未压缩）方式，以支持 AssetManager.openFd()
     */
    private fun addBgmToAssets(
        zipOut: ZipOutputStream,
        bgmPaths: List<String>,
        lrcDataList: List<LrcData?>
    ) {
        Log.d("ApkBuilder", "准备嵌入 ${bgmPaths.size} 个背景音乐文件")
        
        bgmPaths.forEachIndexed { index, bgmPath ->
            try {
                val bgmFile = File(bgmPath)
                if (!bgmFile.exists()) {
                    // 尝试处理 asset:/// 路径
                    if (bgmPath.startsWith("asset:///")) {
                        val assetPath = bgmPath.removePrefix("asset:///")
                        val assetBytes = context.assets.open(assetPath).use { it.readBytes() }
                        val targetPath = "assets/bgm/bgm_$index.mp3"
                        writeEntryStoredSimple(zipOut, targetPath, assetBytes)
                        Log.d("ApkBuilder", "BGM 从 assets 嵌入: $targetPath (${assetBytes.size} bytes)")
                    } else {
                        Log.e("ApkBuilder", "BGM 文件不存在: $bgmPath")
                    }
                } else {
                    if (!bgmFile.canRead()) {
                        Log.e("ApkBuilder", "BGM 文件无法读取: $bgmPath")
                        return@forEachIndexed
                    }
                    
                    val bgmBytes = bgmFile.readBytes()
                    if (bgmBytes.isEmpty()) {
                        Log.e("ApkBuilder", "BGM 文件内容为空: $bgmPath")
                        return@forEachIndexed
                    }
                    
                    val targetPath = "assets/bgm/bgm_$index.mp3"
                    writeEntryStoredSimple(zipOut, targetPath, bgmBytes)
                    Log.d("ApkBuilder", "BGM 已嵌入: $targetPath (${bgmBytes.size} bytes)")
                }
                
                // 嵌入歌词文件（如果有）
                val lrcData = lrcDataList.getOrNull(index)
                if (lrcData != null && lrcData.lines.isNotEmpty()) {
                    val lrcContent = convertLrcDataToLrcString(lrcData)
                    val lrcPath = "assets/bgm/bgm_$index.lrc"
                    writeEntryDeflated(zipOut, lrcPath, lrcContent.toByteArray(Charsets.UTF_8))
                    Log.d("ApkBuilder", "LRC 已嵌入: $lrcPath")
                }
            } catch (e: Exception) {
                Log.e("ApkBuilder", "嵌入 BGM 失败: $bgmPath", e)
            }
        }
    }

    /**
     * 将 HTML 文件添加到 assets/html 目录
     * HTML/CSS/JS 文件使用 DEFLATED 压缩（文本文件压缩效果好）
     */
    private fun addHtmlFilesToAssets(
        zipOut: ZipOutputStream,
        htmlFiles: List<com.webtoapp.data.model.HtmlFile>
    ) {
        Log.d("ApkBuilder", "准备嵌入 ${htmlFiles.size} 个 HTML 项目文件")
        
        htmlFiles.forEach { htmlFile ->
            try {
                val sourceFile = File(htmlFile.path)
                if (!sourceFile.exists()) {
                    Log.e("ApkBuilder", "HTML 文件不存在: ${htmlFile.path}")
                    return@forEach
                }
                
                if (!sourceFile.canRead()) {
                    Log.e("ApkBuilder", "HTML 文件无法读取: ${htmlFile.path}")
                    return@forEach
                }
                
                val fileBytes = sourceFile.readBytes()
                if (fileBytes.isEmpty()) {
                    Log.w("ApkBuilder", "HTML 文件内容为空: ${htmlFile.path}")
                    return@forEach
                }
                
                // 保持相对路径结构，存储到 assets/html/ 目录
                val assetPath = "assets/html/${htmlFile.name}"
                writeEntryDeflated(zipOut, assetPath, fileBytes)
                Log.d("ApkBuilder", "HTML 文件已嵌入: $assetPath (${fileBytes.size} bytes)")
            } catch (e: Exception) {
                Log.e("ApkBuilder", "嵌入 HTML 文件失败: ${htmlFile.path}", e)
            }
        }
    }
    
    /**
     * 将 LrcData 转换为标准 LRC 格式字符串
     */
    private fun convertLrcDataToLrcString(lrcData: LrcData): String {
        val sb = StringBuilder()
        
        // 添加元数据
        lrcData.title?.let { sb.appendLine("[ti:$it]") }
        lrcData.artist?.let { sb.appendLine("[ar:$it]") }
        lrcData.album?.let { sb.appendLine("[al:$it]") }
        sb.appendLine()
        
        // 添加歌词行
        lrcData.lines.forEach { line ->
            val minutes = line.startTime / 60000
            val seconds = (line.startTime % 60000) / 1000
            val centiseconds = (line.startTime % 1000) / 10
            sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, line.text))
            
            // 如果有翻译，添加翻译行（使用相同时间戳）
            line.translation?.let { translation ->
                sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, translation))
            }
        }
        
        return sb.toString()
    }

    /**
     * 调试辅助：使用 PackageManager 预解析已构建 APK，检查系统是否能正常读取包信息
     * @return 是否解析成功
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
                    "getPackageArchiveInfo 返回 null，无法解析 APK: ${apkFile.absolutePath}"
                )
                false
            } else {
                Log.d(
                    "ApkBuilder",
                    "解析 APK 成功: packageName=${info.packageName}, " +
                            "versionName=${info.versionName}, " +
                            "activities=${info.activities?.size ?: 0}, " +
                            "services=${info.services?.size ?: 0}, " +
                            "providers=${info.providers?.size ?: 0}"
                )
                true
            }
        } catch (e: Exception) {
            Log.e("ApkBuilder", "调试解析 APK 时发生异常: ${apkFile.absolutePath}", e)
            false
        }
    }

    /**
     * 图标自检：对比用户图标与 APK 内各密度图标
     * 目的：用事实确认 APK 里写入的 PNG 是否已经被“抹成纯色”，还是保持了原始图案
     */
    private fun debugIconSelfCheck(apkFile: File, userIcon: Bitmap?) {
        if (userIcon == null) {
            Log.d("ApkBuilder", "图标自检跳过：userIcon == null")
            return
        }

        try {
            Log.d("ApkBuilder", "======== 图标自检: APK 内图标与用户图标对比 ========")
            Log.d(
                "ApkBuilder",
                "用户图标: size=${userIcon.width}x${userIcon.height}, firstPixel=#${Integer.toHexString(userIcon.getPixel(userIcon.width / 2, userIcon.height / 2))}"
            )

            val iconPaths = listOf(
                "res/mipmap-xxxhdpi-v4/ic_launcher.png",
                "res/mipmap-xxhdpi-v4/ic_launcher.png",
                "res/mipmap-xhdpi-v4/ic_launcher.png",
                "res/mipmap-hdpi-v4/ic_launcher.png",
                "res/mipmap-mdpi-v4/ic_launcher.png",
                "res/mipmap-xxxhdpi-v4/ic_launcher_round.png",
                "res/mipmap-xxhdpi-v4/ic_launcher_round.png"
            )

            ZipFile(apkFile).use { zip ->
                iconPaths.forEach { path ->
                    val entry = zip.getEntry(path) ?: return@forEach
                    val data = zip.getInputStream(entry).readBytes()
                    val bmp = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return@forEach

                    val pixel = bmp.getPixel(bmp.width / 2, bmp.height / 2)
                    Log.d(
                        "ApkBuilder",
                        "APK 图标: path=$path, size=${bmp.width}x${bmp.height}, bytes=${data.size}, firstPixel=#${Integer.toHexString(pixel)}"
                    )
                    bmp.recycle()
                }
            }

            Log.d("ApkBuilder", "======== 图标自检结束 ========")
        } catch (e: Exception) {
            Log.e("ApkBuilder", "图标自检过程中发生异常: ${e.message}", e)
        }
    }

    /**
     * 写入条目（使用 DEFLATED 压缩格式）
     */
    private fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        if (!writtenEntryNames.add(name)) {
            Log.w("ApkBuilder", "检测到重复条目(忽略写入): $name")
            return
        }
        val entry = ZipEntry(name)
        entry.method = ZipEntry.DEFLATED
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }

    /**
     * 判断是否为需要替换的 Launcher 图标 PNG 资源
     * 
     * 注意：ic_launcher_background 不应该被替换！
     * 因为 Adaptive Icon 的 background 层会被系统裁剪显示，
     * 如果用用户图标替换 background，会导致只显示图片中心区域（看起来像纯色）。
     * 正确的做法是只替换 foreground，保持 background 原样。
     */
    private fun isIconEntry(entryName: String): Boolean {
        // 精确匹配常见路径
        if (ICON_PATHS.any { it.first == entryName } ||
            ROUND_ICON_PATHS.any { it.first == entryName }) {
            return true
        }

        // 模糊匹配：检测所有可能的图标 PNG 文件
        // 支持各种路径格式：mipmap-xxxhdpi-v4, mipmap-xxxhdpi, drawable-xxxhdpi 等
        // 注意：不匹配 ic_launcher_background.png，保持背景层原样
        val iconPatterns = listOf(
            "ic_launcher.png",
            "ic_launcher_round.png",
            "ic_launcher_foreground.png"
            // 故意不包含 ic_launcher_background.png
        )
        return iconPatterns.any { pattern ->
            entryName.endsWith(pattern) && 
            (entryName.contains("mipmap") || entryName.contains("drawable"))
        }
    }

    /**
     * 使用自定义 Bitmap 替换单个图标条目
     * 根据路径中的 dpi 信息推断尺寸，保证清晰度
     * 
     * 与旧版本保持一致：
     * - 圆形图标使用 createRoundIcon
     * - foreground 图标使用 createAdaptiveForegroundIcon（添加 safe zone 边距）
     * - 普通图标使用 scaleBitmapToPng
     */
    private fun replaceIconEntry(zipOut: ZipOutputStream, entryName: String, bitmap: Bitmap) {
        // 优先使用预定义尺寸
        var size = ICON_PATHS.find { it.first == entryName }?.second
            ?: ROUND_ICON_PATHS.find { it.first == entryName }?.second

        // 如果预定义没有匹配，根据路径推断尺寸
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
            // 圆形图标
            entryName.contains("round") -> {
                createRoundIcon(bitmap, size)
            }
            // adaptive icon 前景图需要预留 safe zone 边距（关键！与旧版本一致）
            entryName.contains("foreground") -> {
                createAdaptiveForegroundIcon(bitmap, size)
            }
            // 普通图标
            else -> {
                scaleBitmapToPng(bitmap, size)
            }
        }

        writeEntryDeflated(zipOut, entryName, iconBytes)
    }

    /**
     * 按指定尺寸缩放并居中绘制到正方形 Bitmap，返回 PNG 字节数组
     * 与 AppCloner 中保持一致：保持纵横比，居中放置
     */
    private fun scaleBitmapToPng(bitmap: Bitmap, size: Int): ByteArray {
        // 完全对齐旧项目：直接缩放到 size x size
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
        if (scaled != bitmap) {
            scaled.recycle()
        }
        return baos.toByteArray()
    }

    /**
     * 创建 Adaptive Icon 前景图
     * 遵循 Android Adaptive Icon 规范：中间 72dp 为安全区域，周围保留 18dp 边距
     */
    private fun createAdaptiveForegroundIcon(bitmap: Bitmap, size: Int): ByteArray {
        // 完全对齐旧项目实现
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 计算安全区域尺寸（72/108 ≈ 66.67%）
        val safeZoneSize = (size * 72f / 108f).toInt()
        val padding = (size - safeZoneSize) / 2

        // 将用户图标缩放到安全区域尺寸
        val scaled = Bitmap.createScaledBitmap(bitmap, safeZoneSize, safeZoneSize, true)

        // 居中绘制到画布
        canvas.drawBitmap(scaled, padding.toFloat(), padding.toFloat(), null)

        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)

        if (scaled != bitmap) scaled.recycle()
        output.recycle()

        return baos.toByteArray()
    }

    /**
     * 创建圆形图标（用于 ic_launcher_round 系列）
     * 与 AppCloner 中保持一致：保持纵横比，居中放置后套圆形蒙版
     */
    private fun createRoundIcon(bitmap: Bitmap, size: Int): ByteArray {
        // 完全对齐旧项目实现：先缩放到 size x size，再套圆形蒙版
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        // 绘制圆形
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)

        // 设置混合模式
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)

        if (scaled != bitmap) scaled.recycle()
        output.recycle()

        return baos.toByteArray()
    }

    /**
     * 检测是否是 Adaptive Icon 入口 XML 文件
     * 这些文件在 mipmap-anydpi-v26 目录下，定义了 adaptive-icon 结构
     */
    private fun isAdaptiveIconEntryXml(entryName: String): Boolean {
        return entryName.contains("mipmap-anydpi") &&
            (entryName.endsWith("ic_launcher.xml") || entryName.endsWith("ic_launcher_round.xml"))
    }

    /**
     * 为 APK 补齐缺失的 PNG 图标（各 dpi 的 ic_launcher / ic_launcher_round）
     * 注意：不再在这里生成 mipmap-anydpi-v26 图标，避免与循环中 XML->PNG 分支重复
     */
    private fun addMissingIconPngs(
        zipOut: ZipOutputStream,
        bitmap: Bitmap,
        existingEntryNames: Set<String>
    ) {
        // 添加各 dpi 目录下的普通图标（如果还没有）
        ICON_PATHS.forEach { (path, size) ->
            if (!existingEntryNames.contains(path)) {
                val iconBytes = scaleBitmapToPng(bitmap, size)
                writeEntryDeflated(zipOut, path, iconBytes)
                Log.d("ApkBuilder", "添加缺失图标: $path (${size}px)")
            }
        }

        // 添加各 dpi 目录下的圆形图标（如果还没有）
        ROUND_ICON_PATHS.forEach { (path, size) ->
            if (!existingEntryNames.contains(path)) {
                val iconBytes = createRoundIcon(bitmap, size)
                writeEntryDeflated(zipOut, path, iconBytes)
                Log.d("ApkBuilder", "添加缺失圆形图标: $path (${size}px)")
            }
        }
    }

    /**
     * 为 adaptive icon 写入前景 PNG（ic_launcher_foreground.png），配合 ArscEditor.forceReplaceIconPaths 使用
     */
    private fun addAdaptiveIconPngs(
        zipOut: ZipOutputStream,
        bitmap: Bitmap,
        existingEntryNames: Set<String>
    ) {
        val bases = listOf(
            "res/drawable/ic_launcher_foreground",
            "res/drawable-v24/ic_launcher_foreground",
            "res/drawable-anydpi-v24/ic_launcher_foreground"
        )

        // 使用 xxxhdpi 尺寸（432px）确保高清晰度，系统会自动缩放到其他 dpi
        val iconBytes = createAdaptiveForegroundIcon(bitmap, 432)

        bases.forEach { base ->
            val pngPath = "$base.png"
            if (!existingEntryNames.contains(pngPath)) {
                writeEntryDeflated(zipOut, pngPath, iconBytes)
                Log.d("ApkBuilder", "添加前景图: $pngPath")
            }
        }
    }

    /**
     * 判断是否为需要在自定义图标场景下删除的 Adaptive Icon 相关资源
     */
    private fun shouldDeleteForCustomIcon(entryName: String): Boolean {
        // 删除 mipmap-anydpi-v26 目录下的 launcher 相关 XML / PNG
        if (entryName.startsWith("res/mipmap-anydpi-v26/") &&
            (entryName.endsWith("ic_launcher.xml") ||
             entryName.endsWith("ic_launcher_round.xml") ||
             entryName.endsWith("ic_launcher.png") ||
             entryName.endsWith("ic_launcher_round.png"))) {
            return true
        }

        // 删除 foreground / background 相关资源（drawable/mipmap 下的 XML/PNG）
        if ((entryName.startsWith("res/drawable") || entryName.startsWith("res/mipmap")) &&
            (entryName.contains("ic_launcher_foreground") || entryName.contains("ic_launcher_background"))) {
            return true
        }

        // 删除 values 中的 ic_launcher_background 定义
        if (entryName.startsWith("res/values/") && entryName.contains("ic_launcher_background")) {
            return true
        }

        return false
    }

    /**
     * 写入条目（使用 STORED 未压缩格式）
     * 用于 resources.arsc，满足 Android R+ 对未压缩和 4 字节对齐的要求
     */
    private fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        if (!writtenEntryNames.add(name)) {
            Log.w("ApkBuilder", "检测到重复条目(忽略写入 STORED): $name")
            return
        }
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()
        
        // Android 11+ 要求 resources.arsc 的数据在 APK 中按 4 字节对齐
        // 由于我们保证 resources.arsc 是第一个条目，因此可以通过 extra 字段做对齐填充
        if (name == "resources.arsc") {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            val baseHeaderSize = 30 // ZIP 本地文件头固定长度
            val base = baseHeaderSize + nameBytes.size
            // extra 总长度 = 4(自定义 header) + padLen
            // 需要 (base + extraLen) % 4 == 0
            val padLen = (4 - (base + 4) % 4) % 4
            if (padLen > 0) {
                // 使用 0xFFFF 作为私有 extra header ID
                val extra = ByteArray(4 + padLen)
                extra[0] = 0xFF.toByte()
                extra[1] = 0xFF.toByte()
                // data size = padLen (little-endian)
                extra[2] = (padLen and 0xFF).toByte()
                extra[3] = ((padLen shr 8) and 0xFF).toByte()
                // 后面的 pad 字节默认是 0
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
     * 写入配置文件条目
     */
    private fun writeConfigEntry(zipOut: ZipOutputStream, config: ApkConfig) {
        val configJson = template.createConfigJson(config)
        Log.d("ApkBuilder", "写入配置文件: splashEnabled=${config.splashEnabled}, splashType=${config.splashType}")
        Log.d("ApkBuilder", "配置JSON内容: $configJson")
        val data = configJson.toByteArray(Charsets.UTF_8)
        writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH, data)
    }

    /**
     * 复制 ZIP 条目
     * 使用 DEFLATED 压缩方式确保兼容性
     */
    private fun copyEntry(zipIn: ZipFile, zipOut: ZipOutputStream, entry: ZipEntry) {
        val data = zipIn.getInputStream(entry).readBytes()
        writeEntryDeflated(zipOut, entry.name, data)
    }

    /**
     * 从 WebApp.iconPath 加载 Bitmap
     * 支持本地绝对路径、file:// 和 content:// 三种形式
     */
    private fun loadIconBitmap(webApp: WebApp): Bitmap? {
        val path = webApp.iconPath ?: return null
        return try {
            when {
                // 绝对路径
                path.startsWith("/") -> {
                    val file = File(path)
                    if (file.exists()) BitmapFactory.decodeFile(path) else null
                }
                // file:// URI
                path.startsWith("file://") -> {
                    val uri = Uri.parse(path)
                    val file = File(uri.path ?: return null)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                // 兼容旧数据：按 content:// 处理
                else -> {
                    val uri = Uri.parse(path)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ApkBuilder", "加载自定义图标失败: ${e.message}", e)
            null
        }
    }

    /**
     * 将包名填充到指定长度
     * 通过在最后一段添加字符来填充
     * 例如：sh.shihao (9字符) -> sh.shihao.aa (12字符)
     */
    private fun padPackageName(pkg: String, targetLength: Int): String {
        if (pkg.length >= targetLength) return pkg.take(targetLength)
        
        val diff = targetLength - pkg.length
        
        // 需要填充的字符数（包括一个点分隔符）
        return when {
            diff == 1 -> {
                // 只差1个字符，无法添加新段（需要至少 ".a"），在最后一段追加字符
                val lastDot = pkg.lastIndexOf('.')
                if (lastDot > 0) {
                    pkg.substring(0, lastDot + 1) + pkg.substring(lastDot + 1) + "a"
                } else pkg + "a"
            }
            diff == 2 -> {
                // 差2个字符，添加 ".a"
                "$pkg.a"
            }
            else -> {
                // 差3个或更多字符，添加点和填充字符
                val padChars = "a".repeat(diff - 1)  // 减1是因为点占一个字符
                "$pkg.$padChars"
            }
        }
    }

    /**
     * 生成包名
     * 注意：新包名长度必须 <= 原包名 "com.webtoapp" (12字符)
     * 使用格式：com.w2a.xxxx (12字符)
     *
     * 约束：最后一段必须是合法的 Java 标识符段（首字符为字母或下划线），
     * 否则 PackageManager 会在解析时直接报包名非法，表现为“安装包已损坏”。
     */
    private fun generatePackageName(appName: String): String {
        // 从应用名生成 4 位 base36 标识，再规范化为合法包名段
        val raw = appName.hashCode().let { 
            if (it < 0) (-it).toString(36) else it.toString(36)
        }.take(4).padStart(4, '0')

        val segment = normalizePackageSegment(raw)

        return "com.w2a.$segment"  // 总长度: 12 字符，与原包名相同
    }

    /**
     * 规范化包名中的单段：
     * - 转小写
     * - 首字符如果是数字或其它非法字符，则映射/替换为字母，保证满足 [a-zA-Z_][a-zA-Z0-9_]* 规则
     */
    private fun normalizePackageSegment(segment: String): String {
        if (segment.isEmpty()) return "a"

        val chars = segment.lowercase().toCharArray()

        chars[0] = when {
            chars[0] in 'a'..'z' -> chars[0]
            chars[0] in '0'..'9' -> ('a' + (chars[0] - '0'))  // 0..9 映射到 a..j
            else -> 'a'
        }

        // 其余字符 base36 已经是 [0-9a-z]，符合包名要求，无需再处理
        return String(chars)
    }

    /**
     * 清理文件名
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]"), "_").take(50)
    }

    /**
     * 安装 APK
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
     * 获取已构建的 APK 列表
     */
    fun getBuiltApks(): List<File> {
        return outputDir.listFiles()?.filter { it.extension == "apk" } ?: emptyList()
    }

    /**
     * 删除已构建的 APK
     */
    fun deleteApk(apkFile: File): Boolean {
        return apkFile.delete()
    }

    /**
     * 清理所有构建文件
     */
    fun clearAll() {
        outputDir.listFiles()?.forEach { it.delete() }
        tempDir.listFiles()?.forEach { it.delete() }
    }
}

/**
 * WebApp 扩展函数：转换为 ApkConfig
 * @param packageName 包名
 * @param themeType 主题类型名称（对应 AppThemeType 枚举的 name）
 */
fun WebApp.toApkConfig(packageName: String, themeType: String = "AURORA"): ApkConfig {
    return ApkConfig(
        appName = name,
        packageName = packageName,
        targetUrl = url,
        // 默认使用当前时间戳（秒）作为版本号，确保每次导出递增，避免"系统已存在较高版本"错误
        versionCode = apkExportConfig?.customVersionCode ?: (System.currentTimeMillis() / 1000).toInt(),
        versionName = apkExportConfig?.customVersionName?.takeIf { it.isNotBlank() } ?: "1.0.0",
        activationEnabled = activationEnabled,
        activationCodes = activationCodes,
        adBlockEnabled = adBlockEnabled,
        adBlockRules = adBlockRules,
        announcementEnabled = announcementEnabled,
        announcementTitle = announcement?.title ?: "",
        announcementContent = announcement?.content ?: "",
        announcementLink = announcement?.linkUrl ?: "",
        announcementButtonText = announcement?.buttonText ?: "",
        announcementButtonUrl = announcement?.buttonUrl ?: "",
        javaScriptEnabled = webViewConfig.javaScriptEnabled,
        domStorageEnabled = webViewConfig.domStorageEnabled,
        zoomEnabled = webViewConfig.zoomEnabled,
        desktopMode = webViewConfig.desktopMode,
        userAgent = webViewConfig.userAgent,
        hideToolbar = webViewConfig.hideToolbar,
        landscapeMode = webViewConfig.landscapeMode,
        injectScripts = webViewConfig.injectScripts,
        splashEnabled = splashEnabled,
        splashType = splashConfig?.type?.name ?: "IMAGE",
        splashDuration = splashConfig?.duration ?: 3,
        splashClickToSkip = splashConfig?.clickToSkip ?: true,
        splashVideoStartMs = splashConfig?.videoStartMs ?: 0L,
        splashVideoEndMs = splashConfig?.videoEndMs ?: 5000L,
        splashLandscape = splashConfig?.orientation == com.webtoapp.data.model.SplashOrientation.LANDSCAPE,
        splashFillScreen = splashConfig?.fillScreen ?: true,
        splashEnableAudio = splashConfig?.enableAudio ?: false,
        // 媒体应用配置
        appType = appType.name,
        mediaEnableAudio = mediaConfig?.enableAudio ?: true,
        mediaLoop = mediaConfig?.loop ?: true,
        mediaAutoPlay = mediaConfig?.autoPlay ?: true,
        mediaFillScreen = mediaConfig?.fillScreen ?: true,
        mediaLandscape = mediaConfig?.orientation == com.webtoapp.data.model.SplashOrientation.LANDSCAPE,
        
        // HTML应用配置
        htmlEntryFile = htmlConfig?.entryFile ?: "index.html",
        htmlEnableJavaScript = htmlConfig?.enableJavaScript ?: true,
        htmlEnableLocalStorage = htmlConfig?.enableLocalStorage ?: true,
        
        // 背景音乐配置
        bgmEnabled = bgmEnabled,
        bgmPlaylist = bgmConfig?.playlist?.mapIndexed { index, item ->
            BgmShellItem(
                id = item.id,
                name = item.name,
                assetPath = "bgm/bgm_$index.mp3",  // 将在 APK 中存储为 assets/bgm/bgm_0.mp3 等
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
        
        // 主题配置
        themeType = themeType
    )
}

/**
 * 获取启动画面媒体路径
 */
fun WebApp.getSplashMediaPath(): String? {
    return if (splashEnabled) splashConfig?.mediaPath else null
}

/**
 * 构建结果
 */
sealed class BuildResult {
    data class Success(val apkFile: File) : BuildResult()
    data class Error(val message: String) : BuildResult()
}
