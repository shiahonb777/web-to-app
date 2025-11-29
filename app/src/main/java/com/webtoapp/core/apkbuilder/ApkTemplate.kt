package com.webtoapp.core.apkbuilder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.*
import java.util.zip.*

/**
 * APK 模板管理器
 * 管理预编译的 WebView Shell APK 模板
 */
class ApkTemplate(private val context: Context) {

    companion object {
        // 模板 APK 在 assets 中的路径
        private const val TEMPLATE_APK = "template/webview_shell.apk"
        
        // 配置文件路径（在 APK 内）
        const val CONFIG_PATH = "assets/app_config.json"
        
        // 图标资源路径
        val ICON_PATHS = listOf(
            "res/mipmap-mdpi-v4/ic_launcher.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher.png" to 192
        )
        
        // 圆形图标资源路径
        val ROUND_ICON_PATHS = listOf(
            "res/mipmap-mdpi-v4/ic_launcher_round.png" to 48,
            "res/mipmap-hdpi-v4/ic_launcher_round.png" to 72,
            "res/mipmap-xhdpi-v4/ic_launcher_round.png" to 96,
            "res/mipmap-xxhdpi-v4/ic_launcher_round.png" to 144,
            "res/mipmap-xxxhdpi-v4/ic_launcher_round.png" to 192
        )
    }

    // 模板缓存目录
    private val templateDir = File(context.cacheDir, "apk_templates")

    init {
        templateDir.mkdirs()
    }

    /**
     * 获取模板 APK 文件
     * 如果不存在则从 assets 解压
     */
    fun getTemplateApk(): File? {
        val templateFile = File(templateDir, "webview_shell.apk")
        
        // 检查模板是否已存在
        if (templateFile.exists()) {
            return templateFile
        }

        // 从 assets 复制（如果存在）
        return try {
            context.assets.open(TEMPLATE_APK).use { input ->
                FileOutputStream(templateFile).use { output ->
                    input.copyTo(output)
                }
            }
            templateFile
        } catch (e: Exception) {
            // 模板不存在，需要动态创建
            null
        }
    }

    /**
     * 检查是否有可用的模板
     */
    fun hasTemplate(): Boolean {
        return try {
            context.assets.open(TEMPLATE_APK).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 创建配置 JSON
     */
    fun createConfigJson(config: ApkConfig): String {
        return """
        {
            "appName": "${escapeJson(config.appName)}",
            "packageName": "${escapeJson(config.packageName)}",
            "targetUrl": "${escapeJson(config.targetUrl)}",
            "versionCode": ${config.versionCode},
            "versionName": "${escapeJson(config.versionName)}",
            "activationEnabled": ${config.activationEnabled},
            "activationCodes": [${config.activationCodes.joinToString(",") { "\"${escapeJson(it)}\"" }}],
            "adBlockEnabled": ${config.adBlockEnabled},
            "adBlockRules": [${config.adBlockRules.joinToString(",") { "\"${escapeJson(it)}\"" }}],
            "announcementEnabled": ${config.announcementEnabled},
            "announcementTitle": "${escapeJson(config.announcementTitle)}",
            "announcementContent": "${escapeJson(config.announcementContent)}",
            "announcementLink": "${escapeJson(config.announcementLink)}",
            "webViewConfig": {
                "javaScriptEnabled": ${config.javaScriptEnabled},
                "domStorageEnabled": ${config.domStorageEnabled},
                "zoomEnabled": ${config.zoomEnabled},
                "desktopMode": ${config.desktopMode},
                "userAgent": ${config.userAgent?.let { "\"${escapeJson(it)}\"" } ?: "null"}
            }
        }
        """.trimIndent()
    }

    /**
     * 转义 JSON 字符串
     */
    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * 将 Bitmap 缩放到指定尺寸并压缩为 PNG
     */
    fun scaleBitmapToPng(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
        if (scaled != bitmap) {
            scaled.recycle()
        }
        return baos.toByteArray()
    }

    /**
     * 从文件加载 Bitmap
     */
    fun loadBitmap(iconPath: String): Bitmap? {
        return try {
            if (iconPath.startsWith("/")) {
                BitmapFactory.decodeFile(iconPath)
            } else if (iconPath.startsWith("content://")) {
                context.contentResolver.openInputStream(android.net.Uri.parse(iconPath))?.use {
                    BitmapFactory.decodeStream(it)
                }
            } else {
                BitmapFactory.decodeFile(iconPath)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建圆形图标
     */
    fun createRoundIcon(bitmap: Bitmap, size: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // 绘制圆形
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rect, paint)
        
        // 设置混合模式
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        
        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)
        
        if (scaled != bitmap) scaled.recycle()
        output.recycle()
        
        return baos.toByteArray()
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        templateDir.listFiles()?.forEach { it.delete() }
    }
}

/**
 * APK 配置数据类
 */
data class ApkConfig(
    val appName: String,
    val packageName: String,
    val targetUrl: String,
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val iconPath: String? = null,
    
    // 激活码
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    
    // 广告拦截
    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),
    
    // 公告
    val announcementEnabled: Boolean = false,
    val announcementTitle: String = "",
    val announcementContent: String = "",
    val announcementLink: String = "",
    
    // WebView 配置
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val zoomEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val userAgent: String? = null
)
