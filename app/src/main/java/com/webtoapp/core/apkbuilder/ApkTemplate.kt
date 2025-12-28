 package com.webtoapp.core.apkbuilder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.webtoapp.core.shell.BgmShellItem
import com.webtoapp.core.shell.LrcShellTheme
import com.webtoapp.data.model.UserScript
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
            "activationRequireEveryTime": ${config.activationRequireEveryTime},
            "adBlockEnabled": ${config.adBlockEnabled},
            "adBlockRules": [${config.adBlockRules.joinToString(",") { "\"${escapeJson(it)}\"" }}],
            "announcementEnabled": ${config.announcementEnabled},
            "announcementTitle": "${escapeJson(config.announcementTitle)}",
            "announcementContent": "${escapeJson(config.announcementContent)}",
            "announcementLink": "${escapeJson(config.announcementLink)}",
            "announcementLinkText": "${escapeJson(config.announcementLinkText)}",
            "announcementTemplate": "${config.announcementTemplate}",
            "announcementShowEmoji": ${config.announcementShowEmoji},
            "announcementAnimationEnabled": ${config.announcementAnimationEnabled},
            "announcementShowOnce": ${config.announcementShowOnce},
            "splashEnabled": ${config.splashEnabled},
            "splashType": "${config.splashType}",
            "splashDuration": ${config.splashDuration},
            "splashClickToSkip": ${config.splashClickToSkip},
            "splashVideoStartMs": ${config.splashVideoStartMs},
            "splashVideoEndMs": ${config.splashVideoEndMs},
            "splashLandscape": ${config.splashLandscape},
            "splashFillScreen": ${config.splashFillScreen},
            "splashEnableAudio": ${config.splashEnableAudio},
            "webViewConfig": {
                "javaScriptEnabled": ${config.javaScriptEnabled},
                "domStorageEnabled": ${config.domStorageEnabled},
                "zoomEnabled": ${config.zoomEnabled},
                "desktopMode": ${config.desktopMode},
                "userAgent": ${config.userAgent?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "hideToolbar": ${config.hideToolbar},
                "showStatusBarInFullscreen": ${config.showStatusBarInFullscreen},
                "landscapeMode": ${config.landscapeMode},
                "injectScripts": [${config.injectScripts.joinToString(",") { script ->
                    """{"name":"${escapeJson(script.name)}","code":"${escapeJson(script.code)}","enabled":${script.enabled},"runAt":"${script.runAt.name}"}"""
                }}]
            },
            "appType": "${config.appType}",
            "mediaConfig": {
                "enableAudio": ${config.mediaEnableAudio},
                "loop": ${config.mediaLoop},
                "autoPlay": ${config.mediaAutoPlay},
                "fillScreen": ${config.mediaFillScreen},
                "landscape": ${config.mediaLandscape}
            },
            "htmlConfig": {
                "entryFile": "${escapeJson(config.htmlEntryFile)}",
                "enableJavaScript": ${config.htmlEnableJavaScript},
                "enableLocalStorage": ${config.htmlEnableLocalStorage},
                "landscapeMode": ${config.htmlLandscapeMode}
            },
            "bgmEnabled": ${config.bgmEnabled},
            "bgmPlaylist": [${config.bgmPlaylist.joinToString(",") { item ->
                """{"id":"${escapeJson(item.id)}","name":"${escapeJson(item.name)}","assetPath":"${escapeJson(item.assetPath)}","lrcAssetPath":${item.lrcAssetPath?.let { "\"${escapeJson(it)}\"" } ?: "null"},"sortOrder":${item.sortOrder}}"""
            }}],
            "bgmPlayMode": "${config.bgmPlayMode}",
            "bgmVolume": ${config.bgmVolume},
            "bgmAutoPlay": ${config.bgmAutoPlay},
            "bgmShowLyrics": ${config.bgmShowLyrics},
            "bgmLrcTheme": ${config.bgmLrcTheme?.let { theme ->
                """{"id":"${escapeJson(theme.id)}","name":"${escapeJson(theme.name)}","fontSize":${theme.fontSize},"textColor":"${escapeJson(theme.textColor)}","highlightColor":"${escapeJson(theme.highlightColor)}","backgroundColor":"${escapeJson(theme.backgroundColor)}","animationType":"${theme.animationType}","position":"${theme.position}"}"""
            } ?: "null"},
            "themeType": "${config.themeType}",
            "darkMode": "${config.darkMode}",
            "translateEnabled": ${config.translateEnabled},
            "translateTargetLanguage": "${config.translateTargetLanguage}",
            "translateShowButton": ${config.translateShowButton},
            "extensionModuleIds": [${config.extensionModuleIds.joinToString(",") { "\"${escapeJson(it)}\"" }}],
            "embeddedExtensionModules": [${config.embeddedExtensionModules.joinToString(",") { module ->
                """{"id":"${escapeJson(module.id)}","name":"${escapeJson(module.name)}","description":"${escapeJson(module.description)}","icon":"${escapeJson(module.icon)}","category":"${module.category}","code":"${escapeJson(module.code)}","cssCode":"${escapeJson(module.cssCode)}","runAt":"${module.runAt}","urlMatches":[${module.urlMatches.joinToString(",") { rule ->
                    """{"pattern":"${escapeJson(rule.pattern)}","isRegex":${rule.isRegex},"exclude":${rule.exclude}}"""
                }}],"configValues":{${module.configValues.entries.joinToString(",") { (k, v) ->
                    "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
                }}},"enabled":${module.enabled}}"""
            }}],
            "autoStartConfig": ${if (config.bootStartEnabled || config.scheduledStartEnabled) {
                """{"bootStartEnabled":${config.bootStartEnabled},"scheduledStartEnabled":${config.scheduledStartEnabled},"scheduledTime":"${config.scheduledTime}","scheduledDays":[${config.scheduledDays.joinToString(",")}]}"""
            } else "null"}
        }
        """.trimIndent()
    }

    /**
     * 转义 JSON 字符串
     * 完整处理所有JSON特殊字符，确保JavaScript代码可以正确嵌入
     */
    private fun escapeJson(str: String): String {
        val sb = StringBuilder()
        for (char in str) {
            when (char) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f") // form feed
                else -> {
                    // 控制字符使用Unicode转义
                    if (char.code < 32) {
                        sb.append("\\u${char.code.toString(16).padStart(4, '0')}")
                    } else {
                        sb.append(char)
                    }
                }
            }
        }
        return sb.toString()
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
     * 创建 Adaptive Icon 前景图
     * 遵循 Android Adaptive Icon 规范：
     * - 前景层总尺寸 108dp
     * - 安全区域（完整显示）为中间 72dp（66.67%）
     * - 外围 18dp 作为 safe zone 边距
     *
     * @param bitmap 用户上传的图标
     * @param size 输出尺寸（像素）
     * @return PNG 格式字节数组
     */
    fun createAdaptiveForegroundIcon(bitmap: Bitmap, size: Int): ByteArray {
        // 创建透明画布
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        
        // 计算安全区域尺寸（72/108 ≈ 66.67%）
        val safeZoneSize = (size * 72f / 108f).toInt()
        val padding = (size - safeZoneSize) / 2
        
        // 将用户图标缩放到安全区域尺寸
        val scaled = Bitmap.createScaledBitmap(bitmap, safeZoneSize, safeZoneSize, true)
        
        // 居中绘制到画布
        canvas.drawBitmap(scaled, padding.toFloat(), padding.toFloat(), null)
        
        // 转换为 PNG
        val baos = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, baos)
        
        if (scaled != bitmap) scaled.recycle()
        output.recycle()
        
        return baos.toByteArray()
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
    val activationRequireEveryTime: Boolean = false,
    
    // 广告拦截
    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),
    
    // 公告
    val announcementEnabled: Boolean = false,
    val announcementTitle: String = "",
    val announcementContent: String = "",
    val announcementLink: String = "",
    val announcementLinkText: String = "",
    val announcementTemplate: String = "XIAOHONGSHU",
    val announcementShowEmoji: Boolean = true,
    val announcementAnimationEnabled: Boolean = true,
    val announcementShowOnce: Boolean = true,
    
    // WebView 配置
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val zoomEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val userAgent: String? = null,
    val hideToolbar: Boolean = false,
    val showStatusBarInFullscreen: Boolean = false,  // 全屏模式下是否显示状态栏
    val landscapeMode: Boolean = false, // 横屏模式
    val injectScripts: List<UserScript> = emptyList(), // 用户注入脚本
    
    // 启动画面配置
    val splashEnabled: Boolean = false,
    val splashType: String = "IMAGE",      // "IMAGE" or "VIDEO"
    val splashDuration: Int = 3,           // 显示时长（秒）
    val splashClickToSkip: Boolean = true, // 是否允许点击跳过
    val splashVideoStartMs: Long = 0,      // 视频裁剪起始（毫秒）
    val splashVideoEndMs: Long = 5000,     // 视频裁剪结束（毫秒）
    val splashLandscape: Boolean = false,  // 是否横屏显示
    val splashFillScreen: Boolean = true,  // 是否自动放大铺满屏幕
    val splashEnableAudio: Boolean = false, // 是否启用视频音频
    
    // 媒体应用配置（图片/视频转APP）
    val appType: String = "WEB",           // "WEB", "IMAGE", "VIDEO", "HTML"
    val mediaEnableAudio: Boolean = true,  // 视频是否启用音频
    val mediaLoop: Boolean = true,         // 是否循环播放
    val mediaAutoPlay: Boolean = true,     // 是否自动播放
    val mediaFillScreen: Boolean = true,   // 是否铺满屏幕
    val mediaLandscape: Boolean = false,   // 是否横屏显示
    
    // HTML应用配置
    val htmlEntryFile: String = "index.html",  // HTML入口文件名
    val htmlEnableJavaScript: Boolean = true,  // 是否启用JavaScript
    val htmlEnableLocalStorage: Boolean = true, // 是否启用本地存储
    val htmlLandscapeMode: Boolean = false,    // HTML应用横屏模式
    
    // 背景音乐配置
    val bgmEnabled: Boolean = false,       // 是否启用背景音乐
    val bgmPlaylist: List<BgmShellItem> = emptyList(), // 播放列表
    val bgmPlayMode: String = "LOOP",      // 播放模式: LOOP, SEQUENTIAL, SHUFFLE
    val bgmVolume: Float = 0.5f,           // 音量 (0.0-1.0)
    val bgmAutoPlay: Boolean = true,       // 是否自动播放
    val bgmShowLyrics: Boolean = true,     // 是否显示歌词
    val bgmLrcTheme: LrcShellTheme? = null, // 歌词主题
    
    // 主题配置
    val themeType: String = "AURORA",      // 主题类型
    val darkMode: String = "SYSTEM",       // 深色模式: SYSTEM, LIGHT, DARK
    
    // 网页自动翻译配置
    val translateEnabled: Boolean = false,        // 是否启用自动翻译
    val translateTargetLanguage: String = "zh-CN", // 目标语言: zh-CN, en, ja
    val translateShowButton: Boolean = true,      // 是否显示翻译按钮
    
    // 扩展模块配置
    val extensionModuleIds: List<String> = emptyList(), // 启用的扩展模块ID列表
    val embeddedExtensionModules: List<EmbeddedExtensionModule> = emptyList(), // 嵌入的扩展模块完整数据
    
    // 自启动配置
    val autoStartEnabled: Boolean = false,
    val bootStartEnabled: Boolean = false,
    val scheduledStartEnabled: Boolean = false,
    val scheduledTime: String = "08:00",
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
)

/**
 * 嵌入到 APK 中的扩展模块数据
 * 包含模块执行所需的所有信息
 */
data class EmbeddedExtensionModule(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "📦",
    val category: String = "OTHER",
    val code: String = "",
    val cssCode: String = "",
    val runAt: String = "DOCUMENT_END",
    val urlMatches: List<EmbeddedUrlMatchRule> = emptyList(),
    val configValues: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)

/**
 * 嵌入的 URL 匹配规则
 */
data class EmbeddedUrlMatchRule(
    val pattern: String,
    val isRegex: Boolean = false,
    val exclude: Boolean = false
)
