package com.webtoapp.core.apkbuilder

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
            "announcementButtonText": "${escapeJson(config.announcementButtonText)}",
            "announcementButtonUrl": "${escapeJson(config.announcementButtonUrl)}",
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
                "landscapeMode": ${config.landscapeMode},
                "injectScripts": ${serializeScripts(config.injectScripts)}
            },
            "appType": "${config.appType}",
            "mediaConfig": {
                "enableAudio": ${config.mediaEnableAudio},
                "loop": ${config.mediaLoop},
                "autoPlay": ${config.mediaAutoPlay},
                "fillScreen": ${config.mediaFillScreen},
                "landscape": ${config.mediaLandscape}
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
            "themeType": "${escapeJson(config.themeType)}"
        }
        """.trimIndent()
    }

    // Gson 实例用于安全的 JSON 序列化
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    
    /**
     * 转义 JSON 字符串 - 完整的 JSON 规范实现
     * 处理所有 JSON 特殊字符和控制字符
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
                '\u000C' -> sb.append("\\f")  // form feed
                else -> {
                    // 转义所有控制字符 (0x00 - 0x1F)
                    if (char.code < 0x20) {
                        sb.append(String.format("\\u%04x", char.code))
                    } else {
                        sb.append(char)
                    }
                }
            }
        }
        return sb.toString()
    }
    
    /**
     * 使用 Gson 安全序列化脚本数组
     * 避免手动拼接 JSON 导致的格式错误
     */
    private fun serializeScripts(scripts: List<UserScript>): String {
        val scriptList = scripts.map { script ->
            mapOf(
                "name" to script.name,
                "code" to script.code,
                "enabled" to script.enabled,
                "runAt" to script.runAt.name
            )
        }
        return gson.toJson(scriptList)
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
    val announcementButtonText: String = "",  // 左侧按钮文本
    val announcementButtonUrl: String = "",   // 左侧按钮链接
    
    // WebView 配置
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val zoomEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val userAgent: String? = null,
    val hideToolbar: Boolean = false,
    val landscapeMode: Boolean = false, // 强制横屏显示
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
    
    // 背景音乐配置
    val bgmEnabled: Boolean = false,       // 是否启用背景音乐
    val bgmPlaylist: List<BgmShellItem> = emptyList(), // 播放列表
    val bgmPlayMode: String = "LOOP",      // 播放模式: LOOP, SEQUENTIAL, SHUFFLE
    val bgmVolume: Float = 0.5f,           // 音量 (0.0-1.0)
    val bgmAutoPlay: Boolean = true,       // 是否自动播放
    val bgmShowLyrics: Boolean = true,     // 是否显示歌词
    val bgmLrcTheme: LrcShellTheme? = null, // 歌词主题
    
    // 主题配置
    val themeType: String = "AURORA"        // 主题类型名称，对应 AppThemeType 枚举
)
