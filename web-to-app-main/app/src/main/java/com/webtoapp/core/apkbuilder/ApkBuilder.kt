package com.webtoapp.core.apkbuilder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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

    /**
     * 构建 APK
     * @param webApp WebApp 配置
     * @param onProgress 进度回调 (0-100)
     * @return 构建结果
     */
    suspend fun buildApk(
        webApp: WebApp,
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
            val packageName = webApp.apkExportConfig?.customPackageName?.takeIf { 
                it.isNotBlank() && it.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))
            } ?: generatePackageName(webApp.name)
            val config = webApp.toApkConfig(packageName)
            
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
                templateApk, unsignedApk, config, webApp.iconPath, 
                webApp.getSplashMediaPath(), mediaContentPath,
                bgmPlaylistPaths, bgmLrcDataList, htmlFiles
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
     * 4. 替换/添加图标
     * 5. 嵌入启动画面媒体
     * 6. 嵌入媒体应用内容
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
        onProgress: (Int) -> Unit
    ) {
        val iconBitmap = iconPath?.let { template.loadBitmap(it) }
        var hasConfigFile = false
        val replacedIconPaths = mutableSetOf<String>() // 记录已替换的图标路径
        
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
                        
                        // 修改 AndroidManifest.xml（修改包名）
                        entry.name == "AndroidManifest.xml" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            val modifiedData = axmlEditor.modifyPackageName(originalData, config.packageName)
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
                            // 让 @mipmap/ic_launcher* 从 .xml 改为 .png，便于使用位图图标
                            modifiedData = arscEditor.modifyIconPathsToPng(modifiedData)
                            writeEntryStored(zipOut, entry.name, modifiedData)
                        }
                        
                        // 替换/添加配置文件
                        entry.name == ApkTemplate.CONFIG_PATH -> {
                            hasConfigFile = true
                            writeConfigEntry(zipOut, config)
                        }
                        
                        // 替换图标（如果 APK 中存在 PNG 图标）
                        iconBitmap != null && isIconEntry(entry.name) -> {
                            replaceIconEntry(zipOut, entry.name, iconBitmap)
                            replacedIconPaths.add(entry.name)
                        }
                        
                        // 复制其他文件
                        else -> {
                            copyEntry(zipIn, zipOut, entry)
                        }
                    }
                }
                
                // 如果原 APK 没有配置文件，添加一个
                if (!hasConfigFile) {
                    writeConfigEntry(zipOut, config)
                }
                
                // 如果有图标但 APK 中没有 PNG 图标文件，主动添加
                if (iconBitmap != null && replacedIconPaths.isEmpty()) {
                    addIconsToApk(zipOut, iconBitmap)
                }

                // 为使用 adaptive icon 的模板添加前景 PNG 图标
                // 无条件写入，因为 release 版 APK 的前景图可能被编译到不同路径
                if (iconBitmap != null) {
                    addAdaptiveIconPngs(zipOut, iconBitmap, entryNames)
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
            }
        }
        
        iconBitmap?.recycle()
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
     * 主动添加 PNG 图标到 APK
     * 当原 APK 中没有 PNG 图标时使用
     */
    private fun addIconsToApk(zipOut: ZipOutputStream, bitmap: Bitmap) {
        // 添加所有尺寸的普通图标
        ApkTemplate.ICON_PATHS.forEach { (path, size) ->
            val iconBytes = template.scaleBitmapToPng(bitmap, size)
            writeEntryDeflated(zipOut, path, iconBytes)
        }
        
        // 添加所有尺寸的圆形图标
        ApkTemplate.ROUND_ICON_PATHS.forEach { (path, size) ->
            val iconBytes = template.createRoundIcon(bitmap, size)
            writeEntryDeflated(zipOut, path, iconBytes)
        }
    }

    /**
     * 为 adaptive icon 的前景创建 PNG 版本
     * 在 res/drawable 目录下写入 ic_launcher_foreground.png，
     * 并配合 ArscEditor.modifyIconPathsToPng 将路径从 .xml 切换到 .png
     * 
     * 注意：遵循 Android Adaptive Icon 规范，图标会被放置在安全区域（中间 72dp），
     * 周围保留 18dp 边距，避免被形状遮罩裁剪导致图标看起来被放大
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
        // 108dp * 4 (xxxhdpi) = 432px
        val iconBytes = template.createAdaptiveForegroundIcon(bitmap, 432)

        bases.forEach { base ->
            val pngPath = "${base}.png"
            if (!existingEntryNames.contains(pngPath)) {
                writeEntryDeflated(zipOut, pngPath, iconBytes)
            }
        }
    }
    
    /**
     * 写入条目（使用 DEFLATED 压缩格式）
     */
    private fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.DEFLATED
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }

    /**
     * 写入条目（使用 STORED 未压缩格式）
     * 用于 resources.arsc，满足 Android R+ 对未压缩和 4 字节对齐的要求
     */
    private fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
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
     * 检查是否是图标条目
     * 匹配多种可能的图标路径格式
     */
    private fun isIconEntry(entryName: String): Boolean {
        // 精确匹配预定义路径
        if (ApkTemplate.ICON_PATHS.any { it.first == entryName } ||
            ApkTemplate.ROUND_ICON_PATHS.any { it.first == entryName }) {
            return true
        }
        
        // 模糊匹配：检测所有可能的图标 PNG 文件
        // 支持各种路径格式：mipmap-xxxhdpi-v4, mipmap-xxxhdpi, drawable-xxxhdpi 等
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
     * 替换图标条目
     * 根据路径中的 dpi 信息推断图标尺寸
     */
    private fun replaceIconEntry(zipOut: ZipOutputStream, entryName: String, bitmap: Bitmap) {
        // 优先使用预定义尺寸
        var size = ApkTemplate.ICON_PATHS.find { it.first == entryName }?.second
            ?: ApkTemplate.ROUND_ICON_PATHS.find { it.first == entryName }?.second
        
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
                template.createRoundIcon(bitmap, size)
            }
            // adaptive icon 前景图需要预留 safe zone 边距
            entryName.contains("foreground") -> {
                template.createAdaptiveForegroundIcon(bitmap, size)
            }
            // 普通图标
            else -> {
                template.scaleBitmapToPng(bitmap, size)
            }
        }
        
        writeEntryDeflated(zipOut, entryName, iconBytes)
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
 */
fun WebApp.toApkConfig(packageName: String): ApkConfig {
    return ApkConfig(
        appName = name,
        packageName = packageName,
        targetUrl = url,
        versionCode = apkExportConfig?.customVersionCode ?: 1,
        versionName = apkExportConfig?.customVersionName?.takeIf { it.isNotBlank() } ?: "1.0.0",
        iconPath = iconPath,
        activationEnabled = activationEnabled,
        activationCodes = activationCodes,
        adBlockEnabled = adBlockEnabled,
        adBlockRules = adBlockRules,
        announcementEnabled = announcementEnabled,
        announcementTitle = announcement?.title ?: "",
        announcementContent = announcement?.content ?: "",
        announcementLink = announcement?.linkUrl ?: "",
        javaScriptEnabled = webViewConfig.javaScriptEnabled,
        domStorageEnabled = webViewConfig.domStorageEnabled,
        zoomEnabled = webViewConfig.zoomEnabled,
        desktopMode = webViewConfig.desktopMode,
        userAgent = webViewConfig.userAgent,
        hideToolbar = webViewConfig.hideToolbar,
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
        }
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
