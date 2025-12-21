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
                    val isValid = when {
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
    
    @SerializedName("announcementShowOnce")
    val announcementShowOnce: Boolean = true,

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
    
    // 嵌入的扩展模块完整数据（APK导出时嵌入）
    @SerializedName("embeddedExtensionModules")
    val embeddedExtensionModules: List<EmbeddedShellModule> = emptyList()
)

/**
 * 嵌入到 Shell APK 中的扩展模块数据
 */
data class EmbeddedShellModule(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("description")
    val description: String = "",
    
    @SerializedName("icon")
    val icon: String = "📦",
    
    @SerializedName("category")
    val category: String = "OTHER",
    
    @SerializedName("code")
    val code: String = "",
    
    @SerializedName("cssCode")
    val cssCode: String = "",
    
    @SerializedName("runAt")
    val runAt: String = "DOCUMENT_END",
    
    @SerializedName("urlMatches")
    val urlMatches: List<EmbeddedUrlMatch> = emptyList(),
    
    @SerializedName("configValues")
    val configValues: Map<String, String> = emptyMap(),
    
    @SerializedName("enabled")
    val enabled: Boolean = true
) {
    /**
     * 检查 URL 是否匹配此模块
     */
    fun matchesUrl(url: String): Boolean {
        if (urlMatches.isEmpty()) return true
        
        val includeRules = urlMatches.filter { !it.exclude }
        val excludeRules = urlMatches.filter { it.exclude }
        
        // 先检查排除规则
        for (rule in excludeRules) {
            if (matchRule(url, rule)) return false
        }
        
        // 如果没有包含规则，默认匹配
        if (includeRules.isEmpty()) return true
        
        // 检查包含规则
        return includeRules.any { matchRule(url, it) }
    }
    
    private fun matchRule(url: String, rule: EmbeddedUrlMatch): Boolean {
        return if (rule.isRegex) {
            try {
                Regex(rule.pattern).containsMatchIn(url)
            } catch (e: Exception) {
                false
            }
        } else {
            // 通配符匹配：* 匹配任意字符
            val regexPattern = rule.pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
            try {
                Regex(regexPattern, RegexOption.IGNORE_CASE).containsMatchIn(url)
            } catch (e: Exception) {
                url.contains(rule.pattern, ignoreCase = true)
            }
        }
    }
    
    /**
     * 生成可执行的 JavaScript 代码
     */
    fun generateExecutableCode(): String {
        val configJson = com.google.gson.Gson().toJson(configValues)
        return """
            (function() {
                'use strict';
                // 模块配置
                const __MODULE_CONFIG__ = $configJson;
                const __MODULE_INFO__ = {
                    id: '${id}',
                    name: '${name.replace("'", "\\'")}',
                    version: '1.0.0'
                };
                
                // 配置访问函数
                function getConfig(key, defaultValue) {
                    return __MODULE_CONFIG__[key] !== undefined ? __MODULE_CONFIG__[key] : defaultValue;
                }
                
                // CSS 注入
                ${if (cssCode.isNotBlank()) """
                (function() {
                    const style = document.createElement('style');
                    style.id = 'ext-module-${id}';
                    style.textContent = `${cssCode.replace("`", "\\`")}`;
                    (document.head || document.documentElement).appendChild(style);
                })();
                """ else ""}
                
                // 用户代码
                try {
                    $code
                } catch(e) {
                    console.error('[ExtModule: ${name}] Error:', e);
                }
            })();
        """.trimIndent()
    }
}

/**
 * 嵌入的 URL 匹配规则
 */
data class EmbeddedUrlMatch(
    @SerializedName("pattern")
    val pattern: String = "",
    
    @SerializedName("isRegex")
    val isRegex: Boolean = false,
    
    @SerializedName("exclude")
    val exclude: Boolean = false
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
