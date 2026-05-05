package com.webtoapp.core.appmodifier

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.splash.SplashLauncherActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import com.webtoapp.core.apkbuilder.ZipUtils
import com.webtoapp.util.AppConstants





class AppCloner(private val context: Context) {

    companion object {
        private val SANITIZE_FILENAME_REGEX = AppConstants.SANITIZE_FILENAME_REGEX
    }

    private val axmlEditor = AxmlEditor()
    private val axmlRebuilder = AxmlRebuilder()
    private val arscEditor = ArscEditor()
    private val signer = JarSigner(context)


    private val outputDir = File(context.getExternalFilesDir(null), "cloned_apks").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "clone_temp").apply { mkdirs() }


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





    suspend fun createModifiedShortcut(
        config: AppModifyConfig,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): AppModifyResult = withContext(Dispatchers.IO) {

        val safeProgress: suspend (Int, String) -> Unit = { p, t ->
            withContext(Dispatchers.Main) {
                try {
                    onProgress(p, t)
                } catch (e: Exception) {
                    AppLogger.e("AppCloner", "Progress callback error: ${e.message}")
                }
            }
        }

        try {
            safeProgress(10, "准备图标...")


            val iconBitmap = prepareIcon(config)
            val icon = IconCompat.createWithBitmap(iconBitmap)

            safeProgress(50, "创建快捷方式...")



            val hasSplash = config.splashEnabled &&
                           !config.splashPath.isNullOrBlank() &&
                           java.io.File(config.splashPath).exists()

            val hasActivation = config.activationEnabled && config.activationCodes.isNotEmpty()

            val hasAnnouncement = config.announcementEnabled && config.announcementTitle.isNotBlank()

            val needsSplashLauncher = hasSplash || hasActivation || hasAnnouncement

            AppLogger.d("AppCloner", "快捷方式配置: hasSplash=$hasSplash, hasActivation=$hasActivation, hasAnnouncement=$hasAnnouncement, needsSplashLauncher=$needsSplashLauncher")


            val launchIntent = if (needsSplashLauncher) {

                Intent(context, SplashLauncherActivity::class.java).apply {

                    action = Intent.ACTION_VIEW
                    putExtra(SplashLauncherActivity.EXTRA_TARGET_PACKAGE, config.originalApp.packageName)

                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_TYPE, config.splashType)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_PATH, config.splashPath)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_DURATION, config.splashDuration)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_CLICK_SKIP, config.splashClickToSkip)
                    putExtra(SplashLauncherActivity.EXTRA_VIDEO_START_MS, config.splashVideoStartMs)
                    putExtra(SplashLauncherActivity.EXTRA_VIDEO_END_MS, config.splashVideoEndMs)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_LANDSCAPE, config.splashLandscape)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_FILL_SCREEN, config.splashFillScreen)
                    putExtra(SplashLauncherActivity.EXTRA_SPLASH_ENABLE_AUDIO, config.splashEnableAudio)

                    putExtra(SplashLauncherActivity.EXTRA_ACTIVATION_ENABLED, config.activationEnabled)
                    putExtra(SplashLauncherActivity.EXTRA_ACTIVATION_CODES, config.activationCodes.joinToString(","))
                    putExtra(SplashLauncherActivity.EXTRA_ACTIVATION_REQUIRE_EVERY_TIME, config.activationRequireEveryTime)

                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_ENABLED, config.announcementEnabled)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_TITLE, config.announcementTitle)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_CONTENT, config.announcementContent)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_LINK, config.announcementLink)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_TEMPLATE, config.announcementTemplate)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_SHOW_EMOJI, config.announcementShowEmoji)
                    putExtra(SplashLauncherActivity.EXTRA_ANNOUNCEMENT_ANIMATION, config.announcementAnimationEnabled)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            } else {

                context.packageManager.getLaunchIntentForPackage(config.originalApp.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                } ?: return@withContext AppModifyResult.Error("无法获取应用启动 Intent")
            }


            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return@withContext AppModifyResult.Error("当前启动器不支持创建快捷方式")
            }


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
            AppLogger.e("AppCloner", "创建快捷方式失败", e)
            AppLogger.e("AppCloner", "Operation failed", e)
            AppModifyResult.Error(e.message ?: "创建快捷方式失败")
        }
    }




    private fun prepareIcon(config: AppModifyConfig): Bitmap {

        config.newIconPath?.let { path ->
            loadBitmapFromPath(path)?.let { return it }
        }


        config.originalApp.icon?.let { drawable ->
            return drawableToBitmap(drawable)
        }


        return createDefaultIcon()
    }




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






    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }




    private fun createDefaultIcon(): Bitmap {
        val size = 192
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            canvas.drawColor(0xFF6200EE.toInt())
        }
    }





    suspend fun cloneAndInstall(
        config: AppModifyConfig,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): AppModifyResult = withContext(Dispatchers.IO) {

        val safeProgress: suspend (Int, String) -> Unit = { p, t ->
            withContext(Dispatchers.Main) {
                try {
                    onProgress(p, t)
                } catch (e: Exception) {
                    AppLogger.e("AppCloner", "Progress callback error: ${e.message}")
                }
            }
        }

        try {
            AppLogger.d("AppCloner", "开始克隆: ${config.originalApp.packageName}")
            safeProgress(0, "准备克隆...")

            val sourceApk = File(config.originalApp.apkPath)
            if (!sourceApk.exists()) {
                AppLogger.e("AppCloner", "源 APK 不存在: ${config.originalApp.apkPath}")
                return@withContext AppModifyResult.Error("无法访问原应用 APK")
            }

            AppLogger.d("AppCloner", "源 APK 大小: ${sourceApk.length()} bytes")


            val newPackageName = generateClonePackageName(config.originalApp.packageName)
            AppLogger.d("AppCloner", "新包名: $newPackageName")

            safeProgress(10, "复制 APK...")


            val unsignedApk = File(tempDir, "clone_unsigned.apk")
            val signedApk = File(outputDir, "${sanitizeFileName(config.newAppName)}_clone.APK")

            unsignedApk.delete()
            signedApk.delete()

            safeProgress(20, "修改包名和应用名...")


            var iconBitmap: Bitmap? = null
            try {
                iconBitmap = config.newIconPath?.let { loadBitmapFromPath(it) }
                    ?: config.originalApp.icon?.let { drawableToBitmap(it) }
            } catch (e: Exception) {
                AppLogger.e("AppCloner", "加载图标失败: ${e.message}")

            }

            AppLogger.d("AppCloner", "开始修改 APK...")
            modifyApk(
                sourceApk = sourceApk,
                outputApk = unsignedApk,
                originalPackageName = config.originalApp.packageName,
                newPackageName = newPackageName,
                originalAppName = config.originalApp.appName,
                newAppName = config.newAppName,
                iconBitmap = iconBitmap
            ) { progress ->

            }

            AppLogger.d("AppCloner", "APK 修改完成，大小: ${unsignedApk.length()} bytes")

            iconBitmap?.recycle()

            safeProgress(70, "签名 APK...")


            AppLogger.d("AppCloner", "开始签名...")
            val signSuccess = signer.sign(unsignedApk, signedApk)
            if (!signSuccess) {
                AppLogger.e("AppCloner", "Signing failed")
                unsignedApk.delete()
                return@withContext AppModifyResult.Error("APK 签名失败")
            }

            AppLogger.d("AppCloner", "签名完成，最终大小: ${signedApk.length()} bytes")


            debugApkStructure(signedApk)

            safeProgress(90, "准备安装...")


            unsignedApk.delete()


            withContext(Dispatchers.Main) {
                installApk(signedApk)
            }

            safeProgress(100, "Done")
            AppModifyResult.CloneSuccess(signedApk.absolutePath)

        } catch (e: Exception) {
            AppLogger.e("AppCloner", "克隆失败", e)
            AppLogger.e("AppCloner", "Operation failed", e)
            AppModifyResult.Error(e.message ?: "克隆失败: ${e.javaClass.simpleName}")
        }
    }






    private fun generateClonePackageName(originalPackageName: String): String {


        val maxLen = originalPackageName.length.coerceAtMost(128)


        val timestamp = System.currentTimeMillis()
        val random = (0..9999).random()
        val uniqueId = ((timestamp xor random.toLong()) and 0x7FFFFFFF).toString(36)


        val suffixLen = (maxLen - 2).coerceAtLeast(1)
        val rawSuffix = if (uniqueId.length >= suffixLen) {
            uniqueId.take(suffixLen)
        } else {
            uniqueId.padEnd(suffixLen, '0')
        }

        val normalizedSuffix = normalizePackageSegment(rawSuffix)

        return "c.$normalizedSuffix"
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

                        entry.name.startsWith("META-INF/") &&
                        (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") ||
                         entry.name.endsWith(".DSA") || entry.name == "META-INF/MANIFEST.MF") -> {
                        }


                        entry.name == "AndroidManifest.xml" -> {
                            try {
                                val originalData = zipIn.getInputStream(entry).readBytes()
                                AppLogger.d("AppCloner", "AndroidManifest.xml 原始大小: ${originalData.size} bytes")



                                val modifiedData = if (originalPackageName == "com.webtoapp") {
                                    axmlEditor.modifyPackageName(originalData, newPackageName)
                                } else {

                                    axmlRebuilder.expandAndModify(
                                        originalData,
                                        originalPackageName,
                                        newPackageName
                                    )
                                }

                                AppLogger.d("AppCloner", "AndroidManifest.xml 修改后大小: ${modifiedData.size} bytes")
                                writeEntryDeflated(zipOut, entry.name, modifiedData)
                            } catch (e: Exception) {
                                AppLogger.e("AppCloner", "修改 AndroidManifest.xml 失败: ${e.message}", e)

                                copyEntry(zipIn, zipOut, entry)
                            }
                        }


                        entry.name == "resources.arsc" -> {
                            try {
                                val originalData = zipIn.getInputStream(entry).readBytes()
                                AppLogger.d("AppCloner", "resources.arsc 原始大小: ${originalData.size} bytes")

                                var modifiedData = arscEditor.modifyAppName(
                                    originalData, originalAppName, newAppName
                                )
                                modifiedData = arscEditor.modifyIconPathsToPng(modifiedData)

                                AppLogger.d("AppCloner", "resources.arsc 修改后大小: ${modifiedData.size} bytes")
                                writeEntryStored(zipOut, entry.name, modifiedData)
                            } catch (e: Exception) {
                                AppLogger.e("AppCloner", "修改 resources.arsc 失败: ${e.message}", e)

                                copyEntry(zipIn, zipOut, entry)
                            }
                        }


                        iconBitmap != null && isIconEntry(entry.name) -> {
                            replaceIconEntry(zipOut, entry.name, iconBitmap)
                        }


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





    private fun modifyManifestPackageName(
        data: ByteArray,
        oldPackage: String,
        newPackage: String
    ): ByteArray {
        val result = data.copyOf()


        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_8)


        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_16LE)

        return result
    }




    private fun replacePackageBytes(data: ByteArray, oldPkg: String, newPkg: String, charset: java.nio.charset.Charset) {
        val oldBytes = oldPkg.toByteArray(charset)
        val newBytes = newPkg.toByteArray(charset)



        val replacement = if (newBytes.size <= oldBytes.size) {
            newBytes + ByteArray(oldBytes.size - newBytes.size)
        } else {


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





    private fun isIconEntry(entryName: String): Boolean {

        if (ICON_PATHS.any { it.first == entryName } ||
            ROUND_ICON_PATHS.any { it.first == entryName }) {
            return true
        }


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





    private fun replaceIconEntry(zipOut: ZipOutputStream, entryName: String, bitmap: Bitmap) {

        var size = ICON_PATHS.find { it.first == entryName }?.second
            ?: ROUND_ICON_PATHS.find { it.first == entryName }?.second


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
                createRoundIcon(bitmap, size)
            }

            entryName.contains("foreground") -> {
                createAdaptiveForegroundIcon(bitmap, size)
            }

            else -> {
                scaleBitmapToPng(bitmap, size)
            }
        }

        writeEntryStored(zipOut, entryName, iconBytes)
    }








    private fun createAdaptiveForegroundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)


        val safeZoneSize = (size * 72f / 108f).toInt()
        val padding = (size - safeZoneSize) / 2f


        val scale = Math.min(safeZoneSize.toFloat() / bitmap.width, safeZoneSize.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()


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




    private fun scaleBitmapToPng(bitmap: Bitmap, size: Int): ByteArray {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)


        val scale = Math.min(size.toFloat() / bitmap.width, size.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()


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




    private fun createRoundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }


        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)

        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)


        val scale = Math.min(size.toFloat() / bitmap.width, size.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()


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
        ZipUtils.writeEntryDeflated(zipOut, name, data)
    }

    private fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        ZipUtils.writeEntryStored(zipOut, name, data)
    }

    private fun copyEntry(zipIn: ZipFile, zipOut: ZipOutputStream, entry: ZipEntry) {
        ZipUtils.copyEntryPreserveMethod(zipIn, zipOut, entry)
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




    private fun sanitizeFileName(name: String): String {
        return name.replace(SANITIZE_FILENAME_REGEX, "_").take(50)
    }




    private fun debugApkStructure(apkFile: File) {
        try {
            val pm = context.packageManager
            val flags = PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_PROVIDERS

            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)

            if (info == null) {
                AppLogger.e("AppCloner", "getPackageArchiveInfo 返回 null，无法解析 APK: ${apkFile.absolutePath}")
            } else {
                val appInfo = info.applicationInfo
                val flagsApp = appInfo?.flags ?: 0
                val isDebuggable = (flagsApp and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                val isTestOnly = (flagsApp and ApplicationInfo.FLAG_TEST_ONLY) != 0

                AppLogger.d("AppCloner", "解析克隆 APK 成功: packageName=${info.packageName}, " +
                        "versionName=${info.versionName}, " +
                        "activities=${info.activities?.size ?: 0}, " +
                        "services=${info.services?.size ?: 0}, " +
                        "providers=${info.providers?.size ?: 0}, " +
                        "debuggable=$isDebuggable, testOnly=$isTestOnly, appFlags=0x${flagsApp.toString(16)}")
            }
        } catch (e: Exception) {
            AppLogger.e("AppCloner", "调试解析克隆 APK 时发生异常: ${apkFile.absolutePath}", e)
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
            AppLogger.e("AppCloner", "Operation failed", e)
            false
        }
    }
}
