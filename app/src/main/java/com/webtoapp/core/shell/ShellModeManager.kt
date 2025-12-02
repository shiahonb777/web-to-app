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
            context.assets.open(CONFIG_FILE).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val config = Gson().fromJson(reader, ShellConfig::class.java)
                    // 验证配置有效性
                    if (config?.targetUrl.isNullOrBlank()) null else config
                }
            }
        } catch (e: Exception) {
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
    val splashLandscape: Boolean = false
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
    val hideToolbar: Boolean = false
)
