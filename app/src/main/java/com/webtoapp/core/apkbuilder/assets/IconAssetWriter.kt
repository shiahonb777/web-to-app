package com.webtoapp.core.apkbuilder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.webtoapp.core.logging.AppLogger
import java.util.zip.ZipOutputStream

internal class IconAssetWriter(
    private val template: ApkTemplate,
    private val logger: BuildLogger
) {

    fun generateDefaultIcon(appName: String, themeType: String = "AURORA"): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgColor = getThemePrimaryColor(themeType)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val radius = size * 0.22f
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, bgPaint)

        val initial = appName.firstOrNull()?.uppercase() ?: "A"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = getThemeOnPrimaryColor(themeType)
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

    fun addIconsToApk(zipOut: ZipOutputStream, bitmap: Bitmap) {
        ApkTemplate.ICON_PATHS.forEach { (path, size) ->
            ZipUtils.writeEntryDeflated(zipOut, path, template.scaleBitmapToPng(bitmap, size))
        }
        ApkTemplate.ROUND_ICON_PATHS.forEach { (path, size) ->
            ZipUtils.writeEntryDeflated(zipOut, path, template.createRoundIcon(bitmap, size))
        }
    }

    fun addAdaptiveIconPngs(
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
            val pngPath = "$base.png"
            if (!existingEntryNames.contains(pngPath)) {
                ZipUtils.writeEntryDeflated(zipOut, pngPath, iconBytes)
                AppLogger.d("ApkBuilder", "Added adaptive icon foreground: $pngPath")
            }
        }
    }

    fun replaceIconEntry(zipOut: ZipOutputStream, entryName: String, bitmap: Bitmap) {
        val size = ApkTemplate.ICON_PATHS.find { it.first == entryName }?.second
            ?: ApkTemplate.ROUND_ICON_PATHS.find { it.first == entryName }?.second
            ?: when {
                entryName.contains("xxxhdpi") -> 192
                entryName.contains("xxhdpi") -> 144
                entryName.contains("xhdpi") -> 96
                entryName.contains("hdpi") -> 72
                entryName.contains("mdpi") -> 48
                entryName.contains("ldpi") -> 36
                else -> 96
            }

        val iconBytes = when {
            entryName.contains("round") -> template.createRoundIcon(bitmap, size)
            entryName.contains("foreground") -> template.createAdaptiveForegroundIcon(bitmap, size)
            else -> template.scaleBitmapToPng(bitmap, size)
        }
        ZipUtils.writeEntryDeflated(zipOut, entryName, iconBytes)
    }

    private fun getThemePrimaryColor(themeType: String): Int = when (themeType) {
        "AURORA" -> 0xFF7B68EE.toInt()
        "CYBERPUNK" -> 0xFFFF00FF.toInt()
        "SAKURA" -> 0xFFFFB7C5.toInt()
        "OCEAN" -> 0xFF0077B6.toInt()
        "FOREST" -> 0xFF2D6A4F.toInt()
        "GALAXY" -> 0xFF5C4D7D.toInt()
        "VOLCANO" -> 0xFFD32F2F.toInt()
        "FROST" -> 0xFF4FC3F7.toInt()
        "SUNSET" -> 0xFFE65100.toInt()
        "MINIMAL" -> 0xFF212121.toInt()
        "NEON_TOKYO" -> 0xFFE91E63.toInt()
        "LAVENDER" -> 0xFF7E57C2.toInt()
        else -> 0xFF7B68EE.toInt()
    }

    private fun getThemeOnPrimaryColor(themeType: String): Int = when (themeType) {
        "CYBERPUNK" -> 0xFF000000.toInt()
        "SAKURA" -> 0xFF4A1C2B.toInt()
        "FROST" -> 0xFF00344A.toInt()
        else -> 0xFFFFFFFF.toInt()
    }
}
