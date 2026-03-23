package com.webtoapp.core.appmodifier

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.webtoapp.core.apkbuilder.ArscEditor
import com.webtoapp.core.apkbuilder.AxmlEditor
import com.webtoapp.core.apkbuilder.AxmlRebuilder
import com.webtoapp.core.apkbuilder.JarSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.webtoapp.ui.splash.SplashLauncherActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 应用克隆器
 * 支持创建自定义图标和名称的应用快捷方式或克隆安装
 */
class AppCloner(private val context: Context) {

    private val axmlEditor = AxmlEditor()
    private val axmlRebuilder = AxmlRebuilder()
    private val arscEditor = ArscEditor()
    private val signer = JarSigner(context)
    
    // 输出目录
    private val outputDir = File(context.getExternalFilesDir(null), "cloned_apks").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "clone_temp").apply { mkdirs() }
    
    // Icon路径
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

    /**
     * 创建修改后的快捷方式
     * 使用新图标和名称，但启动原应用
     */
    suspend fun createModifiedShortcut(
        config: AppModifyConfig,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): AppModifyResult = withContext(Dispatchers.IO) {
        // 包装进度回调，确保在主线程调用
        val safeProgress: suspend (Int, String) -> Unit = { p, t ->
            withContext(Dispatchers.Main) {
                try {
                    onProgress(p, t)
                } catch (e: Exception) {
                    Log.e("AppCloner", "Progress callback error: ${e.message}")
                }
            }
        }
        
        try {
            safeProgress(10, "准备图标...")
            
            // 准备图标
            val iconBitmap = prepareIcon(config)
            val icon = IconCompat.createWithBitmap(iconBitmap)
            
            safeProgress(50, "创建快捷方式...")
            
            // 判断是否需要使用 SplashLauncherActivity（有启动画面/激活码/公告任一功能真正生效时）
            // Start画面：必须启用且有有效的媒体路径
            val hasSplash = config.splashEnabled && 
                           !config.splashPath.isNullOrBlank() && 
                           java.io.File(config.splashPath).exists()
            // Activation码：必须启用且有至少一个激活码
            val hasActivation = config.activationEnabled && config.activationCodes.isNotEmpty()
            // Announcement：必须启用且有标题
            val hasAnnouncement = config.announcementEnabled && config.announcementTitle.isNotBlank()
            
            val needsSplashLauncher = hasSplash || hasActivation || hasAnnouncement
            
            Log.d("AppCloner", "快捷方式配置: hasSplash=$hasSplash, hasActivation=$hasActivation, hasAnnouncement=$hasAnnouncement, needsSplashLauncher=$needsSplashLauncher")
            
            // Create启动 Intent
            val launchIntent = if (needsSplashLauncher) {
                // 使用 SplashLauncherActivity 处理启动画面/激活码/公告
                Intent(context, SplashLauncherActivity::class.java).apply {
                    // 必须设置 action，否则 ShortcutManager 会报错
                    action = Intent.ACTION_VIEW
                    putExtra(SplashLauncherActivity.EXTRA_TARGET_PACKAGE, config.originalApp.packageName)
                    // Start画面配置
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_TYPE, config.splashType)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_PATH, config.splashPath)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_DURATION, config.splashDuration)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_CLICK_SKIP, config.splashClickToSkip)
                    putExtra(SplashLauncherActivity.EXTRA_VIDEO_START_MS, config.splashVideoStartMs)
                    putExtra(SplashLauncherActivity.EXTRA_VIDEO_END_MS, config.splashVideoEndMs)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_LANDSCAPE, config.splashLandscape)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_FILL_SCREEN, config.splashFillScreen)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_ENABLE_AUDIO, config.splashEnableAudio)
                    // Activation码配置（使用逗号分隔的字符串，因为 PersistableBundle 不支持 ArrayList）
                    putExtra(SplashLauncherActivity.EXTRA_ACTIVATION_ENABLED, config.activationEnabled)
                    putExtra(SplashLauncherActivity.EXTRA_ACTIVATION_CODES, config.activationCodes.joinToString(","))
                    putExtra(SplashLauncherActivity.EXTRA_ACTIVATION_REQUIRE_EVERY_TIME, config.activationRequireEveryTime)
                    // Announcement配置
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_ENABLED, config.announcementEnabled)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_TITLE, config.announcementTitle)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_CONTENT, config.announcementContent)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_LINK, config.announcementLink)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            } else {
                // 直接启动原应用
                context.packageManager.getLaunchIntentForPackage(config.originalApp.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                } ?: return@withContext AppModifyResult.Error("无法获取应用启动 Intent")
            }

            // Check是否支持固定快捷方式
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return@withContext AppModifyResult.Error("当前启动器不支持创建快捷方式")
            }

            // Create快捷方式
            val shortcutId = "modified_${config.originalApp.packageName}_${System.currentTimeMillis()}"
            val shortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(config.newAppName.take(10))
                .setLongLabel(config.newAppName.take(25))
                .setIcon(icon)
                .setIntent(launchIntent)
                .build()

            safeProgress(80, "请求创建...")
            
            val result = withContext(Dispatchers.Main) {
                ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            }
            
            iconBitmap.recycle()
            
            if (result) {
                safeProgress(100, "Done")
                AppModifyResult.ShortcutSuccess
            } else {
                AppModifyResult.Error("创建快捷方式失败，请检查权限设置")
            }
            
        } catch (e: Exception) {
            Log.e("AppCloner", "创建快捷方式失败", e)
            e.printStackTrace()
            AppModifyResult.Error(e.message ?: "创建快捷方式失败")
        }
    }

    /**
     * 准备图标 Bitmap
     */
    private fun prepareIcon(config: AppModifyConfig): Bitmap {
        // 如果有新图标路径，使用新图标
        config.newIconPath?.let { path ->
            loadBitmapFromPath(path)?.let { return it }
        }
        
        // No则使用原应用图标
        config.originalApp.icon?.let { drawable ->
            return drawableToBitmap(drawable)
        }
        
        // 兜底：创建默认图标
        return createDefaultIcon()
    }

    /**
     * 从路径加载 Bitmap
     */
    private fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            when {
                path.startsWith("/") -> BitmapFactory.decodeFile(path)
                path.startsWith("content://") || path.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(path))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                else -> BitmapFactory.decodeFile(path)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Drawable 转 Bitmap
     * 注意：总是创建新的 Bitmap 副本，避免直接返回系统管理的 Bitmap 引用
     * 这样后续 recycle() 不会影响系统缓存
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        
        return bitmap
    }

    /**
     * 创建默认图标
     */
    private fun createDefaultIcon(): Bitmap {
        val size = 192
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            canvas.drawColor(0xFF6200EE.toInt()) // Material Purple
        }
    }

    /**
     * 克隆并安装应用
     * 修改包名、应用名、图标，然后安装为新应用
     */
    suspend fun cloneAndInstall(
        config: AppModifyConfig,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): AppModifyResult = withContext(Dispatchers.IO) {
        // 包装进度回调，确保在主线程调用
        val safeProgress: suspend (Int, String) -> Unit = { p, t ->
            withContext(Dispatchers.Main) {
                try {
                    onProgress(p, t)
                } catch (e: Exception) {
                    Log.e("AppCloner", "Progress callback error: ${e.message}")
                }
            }
        }
        
        try {
            Log.d("AppCloner", "开始克隆: ${config.originalApp.packageName}")
            safeProgress(0, "准备克隆...")
            
            val sourceApk = File(config.originalApp.apkPath)
            if (!sourceApk.exists()) {
                Log.e("AppCloner", "源 APK 不存在: ${config.originalApp.apkPath}")
                return@withContext AppModifyResult.Error("无法访问原应用 APK")
            }
            
            Log.d("AppCloner", "源 APK 大小: ${sourceApk.length()} bytes")
            
            // Generate新包名（在原包名基础上添加后缀）
            val newPackageName = generateClonePackageName(config.originalApp.packageName)
            Log.d("AppCloner", "新包名: $newPackageName")
            
            safeProgress(10, "复制 APK...")
            
            // 准备临时文件
            val unsignedApk = File(tempDir, "clone_unsigned.apk")
            val signedApk = File(outputDir, "${sanitizeFileName(config.newAppName)}_clone.APK")
            
            unsignedApk.delete()
            signedApk.delete()
            
            safeProgress(20, "修改包名和应用名...")
            
            // 修改 APK
            var iconBitmap: Bitmap? = null
            try {
                iconBitmap = config.newIconPath?.let { loadBitmapFromPath(it) }
                    ?: config.originalApp.icon?.let { drawableToBitmap(it) }
            } catch (e: Exception) {
                Log.e("AppCloner", "加载图标失败: ${e.message}")
                // Resume处理，不使用自定义图标
            }
            
            Log.d("AppCloner", "开始修改 APK...")
            modifyApk(
                sourceApk = sourceApk,
                outputApk = unsignedApk,
                originalPackageName = config.originalApp.packageName,
                newPackageName = newPackageName,
                originalAppName = config.originalApp.appName,
                newAppName = config.newAppName,
                iconBitmap = iconBitmap
            ) { progress ->
                // 同步回调，不需要 suspend
            }
            
            Log.d("AppCloner", "APK 修改完成，大小: ${unsignedApk.length()} bytes")
            
            iconBitmap?.recycle()
            
            safeProgress(70, "签名 APK...")
            
            // Signature
            Log.d("AppCloner", "开始签名...")
            val signSuccess = signer.sign(unsignedApk, signedApk)
            if (!signSuccess) {
                Log.e("AppCloner", "Signing failed")
                unsignedApk.delete()
                return@withContext AppModifyResult.Error("APK 签名失败")
            }
            
            Log.d("AppCloner", "签名完成，最终大小: ${signedApk.length()} bytes")

            // 在安装前用 PackageManager 预解析一次，方便发现结构性问题
            debugApkStructure(signedApk)

            safeProgress(90, "准备安装...")
            
            // Cleanup temp files
            unsignedApk.delete()
            
            // 安装 APK
            withContext(Dispatchers.Main) {
                installApk(signedApk)
            }
            
            safeProgress(100, "Done")
            AppModifyResult.CloneSuccess(signedApk.absolutePath)
            
        } catch (e: Exception) {
            Log.e("AppCloner", "克隆失败", e)
            e.printStackTrace()
            AppModifyResult.Error(e.message ?: "克隆失败: ${e.javaClass.simpleName}")
        }
    }

    /**
     * 生成克隆包名
     * 原包名长度限制了新包名的长度，需要巧妙处理
     * 每次生成唯一的包名，支持同一应用多次克隆
     */
    private fun generateClonePackageName(originalPackageName: String): String {
        // 使用 "c.<unique>" 形式生成唯一包名
        // 保证新包名长度 <= 原包名长度，便于在二进制 XML 中安全替换
        val maxLen = originalPackageName.length.coerceAtMost(128)
        
        // 结合时间戳和随机数生成唯一标识
        val timestamp = System.currentTimeMillis()
        val random = (0..9999).random()
        val uniqueId = ((timestamp xor random.toLong()) and 0x7FFFFFFF).toString(36)

        // 预留 "c." 两个字符给前缀
        val suffixLen = (maxLen - 2).coerceAtLeast(1)
        val rawSuffix = if (uniqueId.length >= suffixLen) {
            uniqueId.take(suffixLen)
        } else {
            uniqueId.padEnd(suffixLen, '0')
        }

        val normalizedSuffix = normalizePackageSegment(rawSuffix)

        return "c.$normalizedSuffix"
    }

    /**
     * Java 包名的每段必须以字母或下划线开头，且只能包含字母、数字、下划线。
     * 这里简单地将第一位强制替换为字母，其余保持原样（若为大写则转小写）。
     */
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

    /**
     * 修改 APK 内容
     */
    private fun modifyApk(
        sourceApk: File,
        outputApk: File,
        originalPackageName: String,
        newPackageName: String,
        originalAppName: String,
        newAppName: String,
        iconBitmap: Bitmap?,
        onProgress: (Int) -> Unit = {}
    ) {
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
                        // 跳过签名文件
                        entry.name.startsWith("META-INF/") &&
                        (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") ||
                         entry.name.endsWith(".DSA") || entry.name == "META-INF/MANIFEST.MF") -> {
                        }

                        // 修改 AndroidManifest.xml
                        entry.name == "AndroidManifest.xml" -> {
                            try {
                                val originalData = zipIn.getInputStream(entry).readBytes()
                                Log.d("AppCloner", "AndroidManifest.xml 原始大小: ${originalData.size} bytes")

                                // 克隆自身（com.webtoapp）时，复用 ApkBuilder 中已经验证过的 AxmlEditor 逻辑
                                // 克隆第三方应用时，使用 AxmlRebuilder 展开相对路径类名，避免 ClassNotFoundException
                                val modifiedData = if (originalPackageName == "com.webtoapp") {
                                    axmlEditor.modifyPackageName(originalData, newPackageName)
                                } else {
                                    // 使用 AXML 重构器：展开相对路径类名并修改包名
                                    axmlRebuilder.expandAndModify(
                                        originalData,
                                        originalPackageName,
                                        newPackageName
                                    )
                                }

                                Log.d("AppCloner", "AndroidManifest.xml 修改后大小: ${modifiedData.size} bytes")
                                writeEntryDeflated(zipOut, entry.name, modifiedData)
                            } catch (e: Exception) {
                                Log.e("AppCloner", "修改 AndroidManifest.xml 失败: ${e.message}", e)
                                // 如果修改失败，直接复制原始文件（但包名不变可能导致冲突）
                                copyEntry(zipIn, zipOut, entry)
                            }
                        }

                        // 修改 resources.arsc
                        entry.name == "resources.arsc" -> {
                            try {
                                val originalData = zipIn.getInputStream(entry).readBytes()
                                Log.d("AppCloner", "resources.arsc 原始大小: ${originalData.size} bytes")
                                
                                var modifiedData = arscEditor.modifyAppName(
                                    originalData, originalAppName, newAppName
                                )
                                modifiedData = arscEditor.modifyIconPathsToPng(modifiedData)
                                
                                Log.d("AppCloner", "resources.arsc 修改后大小: ${modifiedData.size} bytes")
                                writeEntryStored(zipOut, entry.name, modifiedData)
                            } catch (e: Exception) {
                                Log.e("AppCloner", "修改 resources.arsc 失败: ${e.message}", e)
                                // 如果修改失败，直接复制原始文件
                                copyEntry(zipIn, zipOut, entry)
                            }
                        }

                        // Replace图标
                        iconBitmap != null && isIconEntry(entry.name) -> {
                            replaceIconEntry(zipOut, entry.name, iconBitmap)
                        }

                        // Copy其他文件
                        else -> {
                            copyEntry(zipIn, zipOut, entry)
                        }
                    }
                }

                if (iconBitmap != null &&
                    entryNames.contains("res/drawable/ic_launcher_foreground.xml")
                ) {
                    addAdaptiveIconPngs(zipOut, iconBitmap, entryNames)
                }
            }
        }
    }

    /**
     * 修改 Manifest 中的包名
     * 直接在二进制数据中查找并替换
     */
    private fun modifyManifestPackageName(
        data: ByteArray,
        oldPackage: String,
        newPackage: String
    ): ByteArray {
        val result = data.copyOf()
        
        // UTF-8 格式替换
        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_8)
        
        // UTF-16LE 格式替换
        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_16LE)
        
        return result
    }

    /**
     * 替换包名字节
     */
    private fun replacePackageBytes(data: ByteArray, oldPkg: String, newPkg: String, charset: java.nio.charset.Charset) {
        val oldBytes = oldPkg.toByteArray(charset)
        val newBytes = newPkg.toByteArray(charset)
        
        // 新包名可能比原包名长，需要特殊处理
        // 这里采用的策略是：如果新包名更长，保持原长度（会导致包名被截断）
        val replacement = if (newBytes.size <= oldBytes.size) {
            newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
        } else {
            // 新包名更长时的处理：由于克隆时新包名是基于原包名生成的，理论上不会出现这种情况
            // 但为了安全，这里截断新包名
            newBytes.copyOf(oldBytes.size)
        }
        
        var i = 0
        while (i <= data.size - oldBytes.size) {
            var match = true
            for (j in oldBytes.indices) {
                if (data[i + j] != oldBytes[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                System.arraycopy(replacement, 0, data, i, replacement.size)
                i += oldBytes.size
            } else {
                i++
            }
        }
    }

    /**
     * 检查是否是图标条目
     * 匹配多种可能的图标路径格式
     */
    private fun isIconEntry(entryName: String): Boolean {
        // 精确匹配预定义路径
        if (ICON_PATHS.any { it.first == entryName } ||
            ROUND_ICON_PATHS.any { it.first == entryName }) {
            return true
        }
        
        // 模糊匹配：检测所有可能的图标 PNG 文件
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
            // adaptive icon 前景图需要预留 safe zone 边距
            entryName.contains("foreground") -> {
                createAdaptiveForegroundIcon(bitmap, size)
            }
            // 普通图标
            else -> {
                scaleBitmapToPng(bitmap, size)
            }
        }
        
        writeEntryStored(zipOut, entryName, iconBytes)
    }

    /**
     * 创建 Adaptive Icon 前景图
     * 遵循 Android Adaptive Icon 规范：
     * - 前景层总尺寸 108dp
     * - 安全区域（完整显示）为中间 72dp（66.67%）
     * - 外围 18dp 作为 safe zone 边距
     */
    private fun createAdaptiveForegroundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // 计算安全区域尺寸（72/108 ≈ 66.67%）
        val safeZoneSize = (size * 72f / 108f).toInt()
        val padding = (size - safeZoneSize) / 2f
        
        // 计算缩放比例，保持纵横比
        val scale = Math.min(safeZoneSize.toFloat() / bitmap.width, safeZoneSize.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        
        // 计算居中位置（在安全区域内居中）
        val left = padding + (safeZoneSize - scaledWidth) / 2f
        val top = padding + (safeZoneSize - scaledHeight) / 2f
        
        val destRect = android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(bitmap, null, destRect, paint)
        
        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)
        output.recycle()
        return baos.toByteArray()
    }

    /**
     * 缩放并转换为 PNG (保持纵横比，居中)
     */
    private fun scaleBitmapToPng(bitmap: Bitmap, size: Int): ByteArray {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // 计算缩放比例，保持纵横比
        val scale = Math.min(size.toFloat() / bitmap.width, size.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        
        // 计算居中位置
        val left = (size - scaledWidth) / 2f
        val top = (size - scaledHeight) / 2f
        
        val destRect = android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight)
        val paint = android.graphics.Paint().apply { 
            isAntiAlias = true 
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(bitmap, null, destRect, paint)
        
        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)
        output.recycle()
        return baos.toByteArray()
    }

    /**
     * 创建圆形图标 (保持纵横比，居中)
     */
    private fun createRoundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // 绘制圆形蒙版
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)
        
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        
        // 计算缩放比例，保持纵横比
        val scale = Math.min(size.toFloat() / bitmap.width, size.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        
        // 计算居中位置
        val left = (size - scaledWidth) / 2f
        val top = (size - scaledHeight) / 2f
        
        val destRect = android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(bitmap, null, destRect, paint)
        
        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)
        output.recycle()
        return baos.toByteArray()
    }

    private fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.DEFLATED
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }

    private fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()

        if (name == "resources.arsc") {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            val baseHeaderSize = 30
            val base = baseHeaderSize + nameBytes.size
            val padLen = (4 - (base + 4) % 4) % 4
            if (padLen > 0) {
                val extra = ByteArray(4 + padLen)
                extra[0] = 0xFF.toByte()
                extra[1] = 0xFF.toByte()
                extra[2] = (padLen and 0xFF).toByte()
                extra[3] = ((padLen shr 8) and 0xFF).toByte()
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

    private fun copyEntry(zipIn: ZipFile, zipOut: ZipOutputStream, entry: ZipEntry) {
        val newEntry = ZipEntry(entry.name)
        if (entry.method == ZipEntry.STORED) {
            newEntry.method = ZipEntry.STORED
            newEntry.size = entry.size
            newEntry.crc = entry.crc
        }
        zipOut.putNextEntry(newEntry)
        zipIn.getInputStream(entry).copyTo(zipOut)
        zipOut.closeEntry()
    }

    private fun addAdaptiveIconPngs(
        zipOut: ZipOutputStream,
        bitmap: Bitmap,
        existingEntryNames: Set<String>
    ) {
        val foregroundPng = "res/drawable/ic_launcher_foreground.png"
        if (!existingEntryNames.contains(foregroundPng)) {
            val iconBytes = scaleBitmapToPng(bitmap, 108)
            writeEntryDeflated(zipOut, foregroundPng, iconBytes)
        }
    }

    /**
     * 清理文件名
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]"), "_").take(50)
    }

    /**
     * 调试辅助：使用 PackageManager 预解析已克隆 APK，检查系统是否能正常读取包信息
     */
    private fun debugApkStructure(apkFile: File) {
        try {
            val pm = context.packageManager
            val flags = PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_PROVIDERS

            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)

            if (info == null) {
                Log.e(
                    "AppCloner",
                    "getPackageArchiveInfo 返回 null，无法解析 APK: ${apkFile.absolutePath}"
                )
            } else {
                val appInfo = info.applicationInfo
                val flagsApp = appInfo?.flags ?: 0
                val isDebuggable = (flagsApp and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                val isTestOnly = (flagsApp and ApplicationInfo.FLAG_TEST_ONLY) != 0

                Log.d(
                    "AppCloner",
                    "解析克隆 APK 成功: packageName=${info.packageName}, " +
                            "versionName=${info.versionName}, " +
                            "activities=${info.activities?.size ?: 0}, " +
                            "services=${info.services?.size ?: 0}, " +
                            "providers=${info.providers?.size ?: 0}, " +
                            "debuggable=$isDebuggable, testOnly=$isTestOnly, appFlags=0x${flagsApp.toString(16)}"
                )
            }
        } catch (e: Exception) {
            Log.e("AppCloner", "调试解析克隆 APK 时发生异常: ${apkFile.absolutePath}", e)
        }
    }

    private fun installApk(apkFile: File): Boolean {
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
}
