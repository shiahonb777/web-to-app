package com.webtoapp.core.shell

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

/**
 * Shell 模式管理器
 * 检测应用是否以 Shell 模式运行（独立 WebApp）
 */
class ShellModeManager(private val context: Context) {

    companion object {
        private const val CONFIG_FILE = "app_config.json"
    }

    private var cachedConfig: ShellConfig? = null
    private var configLoaded = false

    /**
     * 检查是否为 Shell 模式（存在有效的配置文件）
     */
    fun isShellMode(): Boolean {
        return loadConfig() != null
    }

    /**
     * 获取 Shell 配置
     */
    fun getConfig(): ShellConfig? {
        return loadConfig()
    }

    /**
     * 加载配置文件
     */
    private fun loadConfig(): ShellConfig? {
        if (configLoaded) return cachedConfig

        configLoaded = true
        cachedConfig = try {
            android.util.Log.d("ShellModeManager", "尝试加载配置文件: $CONFIG_FILE")
            context.assets.open(CONFIG_FILE).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val jsonStr = reader.readText()
                    android.util.Log.d("ShellModeManager", "配置文件内容: $jsonStr")
                    val config = Gson().fromJson(jsonStr, ShellConfig::class.java)
                    android.util.Log.d("ShellModeManager", "解析结果: targetUrl=${config?.targetUrl}, splashEnabled=${config?.splashEnabled}")
                    // 验证配置有效性
                    // HTML应用不需要targetUrl，使用嵌入的HTML文件
                    // 媒体应用也不需要targetUrl，使用嵌入的媒体文件
                    // 画廊模式使用嵌入的画廊媒体文件
                    val isValid = when {
                        config?.galleryEnabled == true && config.galleryItems.isNotEmpty() -> true // 画廊模式
                        config?.appType == "HTML" -> config.htmlConfig.entryFile.isNotBlank()
                        config?.appType == "IMAGE" || config?.appType == "VIDEO" -> true // 媒体应用
                        else -> !config?.targetUrl.isNullOrBlank() // WEB应用需要targetUrl
                    }
                    if (!isValid) {
                        android.util.Log.w("ShellModeManager", "配置无效: appType=${config?.appType}, targetUrl=${config?.targetUrl}")
                        null
                    } else {
                        android.util.Log.d("ShellModeManager", "配置有效，进入 Shell 模式, appType=${config?.appType}")
                        config
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ShellModeManager", "加载配置文件失败", e)
            null
        }

        return cachedConfig
    }
}

/**
 * Shell 模式配置数据类
 */
data class ShellConfig(
    @SerializedName("appName")
    val appName: String = "",

    @SerializedName("packageName")
    val packageName: String = "",

    @SerializedName("targetUrl")
    val targetUrl: String = "",

    @SerializedName("versionCode")
    val versionCode: Int = 1,

    @SerializedName("versionName")
    val versionName: String = "1.0.0",

    // 激活码配置
    @SerializedName("activationEnabled")
    val activationEnabled: Boolean = false,

    @SerializedName("activationCodes")
    val activationCodes: List<String> = emptyList(),

    // 广告拦截配置
    @SerializedName("adBlockEnabled")
    val adBlockEnabled: Boolean = false,

    @SerializedName("adBlockRules")
    val adBlockRules: List<String> = emptyList(),

    // 公告配置
    @SerializedName("announcementEnabled")
    val announcementEnabled: Boolean = false,

    @SerializedName("announcementTitle")
    val announcementTitle: String = "",

    @SerializedName("announcementContent")
    val announcementContent: String = "",

    @SerializedName("announcementLink")
    val announcementLink: String = "",
    
    @SerializedName("announcementLinkText")
    val announcementLinkText: String = "",
    
    @SerializedName("announcementTemplate")
    val announcementTemplate: String = "XIAOHONGSHU",
    
    @SerializedName("announcementShowEmoji")
    val announcementShowEmoji: Boolean = true,
    
    @SerializedName("announcementAnimationEnabled")
    val announcementAnimationEnabled: Boolean = true,

    // WebView 配置
    @SerializedName("webViewConfig")
    val webViewConfig: WebViewShellConfig = WebViewShellConfig(),

    // 启动画面配置
    @SerializedName("splashEnabled")
    val splashEnabled: Boolean = false,

    @SerializedName("splashType")
    val splashType: String = "IMAGE",

    @SerializedName("splashDuration")
    val splashDuration: Int = 3,

    @SerializedName("splashClickToSkip")
    val splashClickToSkip: Boolean = true,

    // 视频裁剪配置
    @SerializedName("splashVideoStartMs")
    val splashVideoStartMs: Long = 0,

    @SerializedName("splashVideoEndMs")
    val splashVideoEndMs: Long = 5000,
    
    @SerializedName("splashLandscape")
    val splashLandscape: Boolean = false,
    
    @SerializedName("splashFillScreen")
    val splashFillScreen: Boolean = true,
    
    @SerializedName("splashEnableAudio")
    val splashEnableAudio: Boolean = false,
    
    // 媒体应用配置
    @SerializedName("appType")
    val appType: String = "WEB",
    
    @SerializedName("mediaConfig")
    val mediaConfig: MediaShellConfig = MediaShellConfig(),
    
    // HTML应用配置
    @SerializedName("htmlConfig")
    val htmlConfig: HtmlShellConfig = HtmlShellConfig(),
    
    // 背景音乐配置
    @SerializedName("bgmEnabled")
    val bgmEnabled: Boolean = false,
    
    @SerializedName("bgmPlaylist")
    val bgmPlaylist: List<BgmShellItem> = emptyList(),
    
    @SerializedName("bgmPlayMode")
    val bgmPlayMode: String = "LOOP",
    
    @SerializedName("bgmVolume")
    val bgmVolume: Float = 0.5f,
    
    @SerializedName("bgmAutoPlay")
    val bgmAutoPlay: Boolean = true,
    
    @SerializedName("bgmShowLyrics")
    val bgmShowLyrics: Boolean = true,
    
    @SerializedName("bgmLrcTheme")
    val bgmLrcTheme: LrcShellTheme? = null,
    
    // 主题配置
    @SerializedName("themeType")
    val themeType: String = "AURORA",
    
    @SerializedName("darkMode")
    val darkMode: String = "SYSTEM",
    
    // 网页自动翻译配置
    @SerializedName("translateEnabled")
    val translateEnabled: Boolean = false,
    
    @SerializedName("translateTargetLanguage")
    val translateTargetLanguage: String = "zh-CN",
    
    @SerializedName("translateShowButton")
    val translateShowButton: Boolean = true,
    
    // 扩展模块配置
    @SerializedName("extensionModuleIds")
    val extensionModuleIds: List<String> = emptyList(),
    
    // 画廊配置
    @SerializedName("galleryEnabled")
    val galleryEnabled: Boolean = false,
    
    @SerializedName("galleryItems")
    val galleryItems: List<GalleryShellItem> = emptyList(),
    
    @SerializedName("galleryAutoPlay")
    val galleryAutoPlay: Boolean = false,
    
    @SerializedName("galleryAutoPlayInterval")
    val galleryAutoPlayInterval: Int = 5,
    
    @SerializedName("galleryShowIndicator")
    val galleryShowIndicator: Boolean = true,
    
    @SerializedName("galleryLoop")
    val galleryLoop: Boolean = true,
    
    @SerializedName("galleryTransition")
    val galleryTransition: String = "SLIDE"
)

/**
 * 媒体应用 Shell 配置
 */
data class MediaShellConfig(
    @SerializedName("enableAudio")
    val enableAudio: Boolean = true,
    
    @SerializedName("loop")
    val loop: Boolean = true,
    
    @SerializedName("autoPlay")
    val autoPlay: Boolean = true,
    
    @SerializedName("fillScreen")
    val fillScreen: Boolean = true,
    
    @SerializedName("landscape")
    val landscape: Boolean = false
)

/**
 * HTML应用 Shell 配置
 */
data class HtmlShellConfig(
    @SerializedName("entryFile")
    val entryFile: String = "index.html",
    
    @SerializedName("enableJavaScript")
    val enableJavaScript: Boolean = true,
    
    @SerializedName("enableLocalStorage")
    val enableLocalStorage: Boolean = true,
    
    @SerializedName("backgroundColor")
    val backgroundColor: String = "#FFFFFF",
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

/**
 * WebView Shell 配置
 */
data class WebViewShellConfig(
    @SerializedName("javaScriptEnabled")
    val javaScriptEnabled: Boolean = true,

    @SerializedName("domStorageEnabled")
    val domStorageEnabled: Boolean = true,

    @SerializedName("zoomEnabled")
    val zoomEnabled: Boolean = true,

    @SerializedName("desktopMode")
    val desktopMode: Boolean = false,

    @SerializedName("userAgent")
    val userAgent: String? = null,

    @SerializedName("hideToolbar")
    val hideToolbar: Boolean = false,
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false,
    
    @SerializedName("injectScripts")
    val injectScripts: List<ShellUserScript> = emptyList()
)

/**
 * Shell 模式用户脚本配置
 */
data class ShellUserScript(
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("code")
    val code: String = "",
    
    @SerializedName("enabled")
    val enabled: Boolean = true,
    
    @SerializedName("runAt")
    val runAt: String = "DOCUMENT_END"
)

/**
 * BGM 项（用于 Shell 配置）
 */
data class BgmShellItem(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("assetPath")
    val assetPath: String = "",
    
    @SerializedName("lrcAssetPath")
    val lrcAssetPath: String? = null,
    
    @SerializedName("sortOrder")
    val sortOrder: Int = 0
)

/**
 * 歌词主题（用于 Shell 配置）
 */
data class LrcShellTheme(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("fontSize")
    val fontSize: Float = 18f,
    
    @SerializedName("textColor")
    val textColor: String = "#FFFFFF",
    
    @SerializedName("highlightColor")
    val highlightColor: String = "#FFD700",
    
    @SerializedName("backgroundColor")
    val backgroundColor: String = "#80000000",
    
    @SerializedName("animationType")
    val animationType: String = "FADE",
    
    @SerializedName("position")
    val position: String = "BOTTOM"
)

/**
 * 画廊项（用于 Shell 配置）
 */
data class GalleryShellItem(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("type")
    val type: String = "IMAGE",  // "IMAGE", "VIDEO", "HTML", "WEB"
    
    @SerializedName("assetPath")
    val assetPath: String = "",  // 在 APK assets 中的路径
    
    @SerializedName("sortOrder")
    val sortOrder: Int = 0,
    
    @SerializedName("enableAudio")
    val enableAudio: Boolean = true,
    
    @SerializedName("loop")
    val loop: Boolean = false,
    
    @SerializedName("autoPlay")
    val autoPlay: Boolean = true,
    
    @SerializedName("fillScreen")
    val fillScreen: Boolean = true
)
