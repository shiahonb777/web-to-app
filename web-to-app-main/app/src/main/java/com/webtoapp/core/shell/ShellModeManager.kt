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
                    if (config?.targetUrl.isNullOrBlank()) {
                        android.util.Log.w("ShellModeManager", "配置无效: targetUrl 为空")
                        null
                    } else {
                        android.util.Log.d("ShellModeManager", "配置有效，进入 Shell 模式")
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
    val bgmLrcTheme: LrcShellTheme? = null
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
    val backgroundColor: String = "#FFFFFF"
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
