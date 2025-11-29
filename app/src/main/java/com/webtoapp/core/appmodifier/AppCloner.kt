package com.webtoapp.core.appmodifier

import android.content.Context
import android.content.Intent
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
import com.webtoapp.core.apkbuilder.JarSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val arscEditor = ArscEditor()
    private val signer = JarSigner(context)
    
    // 输出目录
    private val outputDir = File(context.getExternalFilesDir(null), "cloned_apks").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "clone_temp").apply { mkdirs() }
    
    // 图标路径
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
        try {
            onProgress(10, "准备图标...")
            
            // 准备图标
            val iconBitmap = prepareIcon(config)
            val icon = IconCompat.createWithBitmap(iconBitmap)
            
            onProgress(50, "创建快捷方式...")
            
            // 直接启动原应用
            val launchIntent = context.packageManager.getLaunchIntentForPackage(config.originalApp.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            } ?: return@withContext AppModifyResult.Error("无法获取应用启动 Intent")

            // 检查是否支持固定快捷方式
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return@withContext AppModifyResult.Error("当前启动器不支持创建快捷方式")
            }

            // 创建快捷方式
            val shortcutId = "modified_${config.originalApp.packageName}_${System.currentTimeMillis()}"
            val shortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(config.newAppName.take(10))
                .setLongLabel(config.newAppName.take(25))
                .setIcon(icon)
                .setIntent(launchIntent)
                .build()

            onProgress(80, "请求创建...")
            
            val result = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
            
            iconBitmap.recycle()
            
            if (result) {
                onProgress(100, "完成")
                AppModifyResult.ShortcutSuccess
            } else {
                AppModifyResult.Error("创建快捷方式失败，请检查权限设置")
            }
            
        } catch (e: Exception) {
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
        
        // 否则使用原应用图标
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
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        
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
        try {
            onProgress(0, "准备克隆...")
            
            val sourceApk = File(config.originalApp.apkPath)
            if (!sourceApk.exists()) {
                return@withContext AppModifyResult.Error("无法访问原应用 APK")
            }
            
            // 生成新包名（在原包名基础上添加后缀）
            val newPackageName = generateClonePackageName(config.originalApp.packageName)
            
            onProgress(10, "复制 APK...")
            
            // 准备临时文件
            val unsignedApk = File(tempDir, "clone_unsigned.apk")
            val signedApk = File(outputDir, "${sanitizeFileName(config.newAppName)}_clone.apk")
            
            unsignedApk.delete()
            signedApk.delete()
            
            onProgress(20, "修改包名和应用名...")
            
            // 修改 APK
            val iconBitmap = config.newIconPath?.let { loadBitmapFromPath(it) }
                ?: config.originalApp.icon?.let { drawableToBitmap(it) }
            
            modifyApk(
                sourceApk = sourceApk,
                outputApk = unsignedApk,
                originalPackageName = config.originalApp.packageName,
                newPackageName = newPackageName,
                originalAppName = config.originalApp.appName,
                newAppName = config.newAppName,
                iconBitmap = iconBitmap
            ) { progress ->
                onProgress(20 + (progress * 0.5).toInt(), "处理资源...")
            }
            
            iconBitmap?.recycle()
            
            onProgress(70, "签名 APK...")
            
            // 签名
            val signSuccess = signer.sign(unsignedApk, signedApk)
            if (!signSuccess) {
                unsignedApk.delete()
                return@withContext AppModifyResult.Error("APK 签名失败")
            }
            
            onProgress(90, "准备安装...")
            
            // 清理临时文件
            unsignedApk.delete()
            
            // 安装 APK
            installApk(signedApk)
            
            onProgress(100, "完成")
            AppModifyResult.CloneSuccess(signedApk.absolutePath)
            
        } catch (e: Exception) {
            e.printStackTrace()
            AppModifyResult.Error(e.message ?: "克隆失败")
        }
    }

    /**
     * 生成克隆包名
     * 原包名长度限制了新包名的长度，需要巧妙处理
     */
    private fun generateClonePackageName(originalPackageName: String): String {
        // 使用 "c.<hash>" 形式生成一个全新的包名
        // 保证新包名长度 <= 原包名长度，便于在二进制 XML 中安全替换
        val maxLen = originalPackageName.length.coerceAtMost(128)
        val hash = kotlin.math.abs(originalPackageName.hashCode()).toString(36)

        // 预留 "c." 两个字符给前缀
        val suffixLen = (maxLen - 2).coerceAtLeast(1)
        val suffix = if (hash.length >= suffixLen) {
            hash.take(suffixLen)
        } else {
            hash.padEnd(suffixLen, '0')
        }

        return "c.$suffix"
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
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            val modifiedData = modifyManifestPackageName(
                                originalData, originalPackageName, newPackageName
                            )
                            writeEntryDeflated(zipOut, entry.name, modifiedData)
                        }

                        // 修改 resources.arsc
                        entry.name == "resources.arsc" -> {
                            val originalData = zipIn.getInputStream(entry).readBytes()
                            var modifiedData = arscEditor.modifyAppName(
                                originalData, originalAppName, newAppName
                            )
                            modifiedData = arscEditor.modifyIconPathsToPng(modifiedData)
                            writeEntryStored(zipOut, entry.name, modifiedData)
                        }

                        // 替换图标
                        iconBitmap != null && isIconEntry(entry.name) -> {
                            replaceIconEntry(zipOut, entry.name, iconBitmap)
                        }

                        // 复制其他文件
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
        
        val iconBytes = if (entryName.contains("round")) {
            createRoundIcon(bitmap, size)
        } else {
            scaleBitmapToPng(bitmap, size)
        }
        
        writeEntryStored(zipOut, entryName, iconBytes)
    }

    /**
     * 缩放并转换为 PNG
     */
    private fun scaleBitmapToPng(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
        if (scaled != bitmap) scaled.recycle()
        return baos.toByteArray()
    }

    /**
     * 创建圆形图标
     */
    private fun createRoundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)
        
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        
        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)
        
        if (scaled != bitmap) scaled.recycle()
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
