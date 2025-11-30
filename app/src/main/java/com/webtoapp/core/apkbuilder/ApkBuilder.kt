package com.webtoapp.core.apkbuilder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
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
    private val originalAppName = "WebToApp"
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
            
            // 生成包名（基于应用名）
            val packageName = generatePackageName(webApp.name)
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
            
            // 修改 APK 内容
            modifyApk(templateApk, unsignedApk, config, webApp.iconPath) { progress ->
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
     */
    private fun modifyApk(
        sourceApk: File,
        outputApk: File,
        config: ApkConfig,
        iconPath: String?,
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

                // 为使用 adaptive icon 的模板添加前景 PNG 图标（drawable/ic_launcher_foreground.png）
                if (iconBitmap != null &&
                    entryNames.contains("res/drawable/ic_launcher_foreground.xml")
                ) {
                    addAdaptiveIconPngs(zipOut, iconBitmap, entryNames)
                }
            }
        }
        
        iconBitmap?.recycle()
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
     */
    private fun addAdaptiveIconPngs(
        zipOut: ZipOutputStream,
        bitmap: Bitmap,
        existingEntryNames: Set<String>
    ) {
        val foregroundPng = "res/drawable/ic_launcher_foreground.png"
        if (!existingEntryNames.contains(foregroundPng)) {
            val iconBytes = template.scaleBitmapToPng(bitmap, 108)
            writeEntryDeflated(zipOut, foregroundPng, iconBytes)
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
        
        val iconBytes = if (entryName.contains("round")) {
            template.createRoundIcon(bitmap, size)
        } else {
            template.scaleBitmapToPng(bitmap, size)
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
        hideToolbar = webViewConfig.hideToolbar
    )
}

/**
 * 构建结果
 */
sealed class BuildResult {
    data class Success(val apkFile: File) : BuildResult()
    data class Error(val message: String) : BuildResult()
}
