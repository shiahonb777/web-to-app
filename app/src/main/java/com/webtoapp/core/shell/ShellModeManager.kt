package com.webtoapp.core.shell

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.crypto.AssetDecryptor
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.logging.AppLogger

/** Escape for JS single-quoted string literal. */
private fun String.escapeForJsSingleQuote(): String =
    this.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")

/** Escape for JS template literal (backtick). */
private fun String.escapeForJsTemplate(): String =
    this.replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("\${", "\\\${")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

/**
 * Shell 模式管理器 — 检测应用是否以独立 WebApp 模式运行
 */
class ShellModeManager(private val context: Context) {

    companion object {
        private const val TAG = "ShellModeManager"
        private const val CONFIG_FILE = "app_config.json"
    }

    private val gson = com.webtoapp.util.GsonProvider.gson
    @Volatile
    private var cachedConfig: ShellConfig? = null
    @Volatile
    private var configLoaded = false
    private val assetDecryptor = AssetDecryptor(context)

    fun isShellMode(): Boolean = loadConfig() != null

    fun getConfig(): ShellConfig? = loadConfig()

    private fun loadConfig(): ShellConfig? {
        if (configLoaded) return cachedConfig

        synchronized(this) {
            if (configLoaded) return cachedConfig

            configLoaded = true
            cachedConfig = try {
            AppLogger.d(TAG, "尝试加载配置文件: $CONFIG_FILE")

            val jsonStr = try {
                assetDecryptor.loadAssetAsString(CONFIG_FILE)
            } catch (e: Exception) {
                AppLogger.e(TAG, "AssetDecryptor 加载失败，尝试直接读取", e)
                try {
                    context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
                } catch (e2: Exception) {
                    AppLogger.e(TAG, "直接读取也失败", e2)
                    throw e2
                }
            }

            AppLogger.d(TAG, "配置文件内容长度: ${jsonStr.length}")
            val config = gson.fromJson(jsonStr, ShellConfig::class.java)
            val normalizedAppType = config?.appType?.trim()?.uppercase() ?: ""
            AppLogger.d(TAG, "解析结果: appType=${config?.appType} (normalized=$normalizedAppType)")
            val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable) {
                AppLogger.d(TAG, "WebView UA配置: userAgentMode=${config?.webViewConfig?.userAgentMode}")
                AppLogger.d(TAG, "注入脚本: ${config?.webViewConfig?.injectScripts?.size ?: 0} 个")
                AppLogger.d(TAG, "扩展模块: extensionModuleIds=${config?.extensionModuleIds?.size ?: 0}, embeddedExtensionModules=${config?.embeddedExtensionModules?.size ?: 0}")
            }
            val isValid = when {
                normalizedAppType == "HTML" || normalizedAppType == "FRONTEND" -> {
                    val entryFile = config.htmlConfig.entryFile
                    entryFile.isNotBlank() && entryFile.substringBeforeLast(".").isNotBlank()
                }
                normalizedAppType in listOf("IMAGE", "VIDEO", "GALLERY", "WORDPRESS", "NODEJS_APP", "PHP_APP", "PYTHON_APP", "GO_APP") -> true
                else -> !config?.targetUrl.isNullOrBlank()
            }
            if (!isValid) {
                AppLogger.w(TAG, "配置无效: appType=${config?.appType}, targetUrl=${config?.targetUrl}")
                null
            } else {
                AppLogger.d(TAG, "配置有效，进入 Shell 模式, appType=${config?.appType}")
                config
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载配置文件失败", e)
            null
        } catch (e: Error) {
            AppLogger.e(TAG, "加载配置文件时发生严重错误", e)
            null
        }

            return cachedConfig
        }
    }

    fun reload() {
        synchronized(this) {
            configLoaded = false
            cachedConfig = null
            assetDecryptor.clearCache()
        }
    }
    
    /**
     * 设置自定义密码（用于密钥派生）
     * 当 APK 使用自定义密码加密时，需要在加载配置前调用此方法
     */
    fun setCustomPassword(password: String?) {
        assetDecryptor.setCustomPassword(password)
        // 清除配置缓存以触发重新加载
        synchronized(this) {
            configLoaded = false
            cachedConfig = null
        }
    }
    
    /**
     * 检查是否需要自定义密码才能解密
     */
    fun requiresCustomPassword(): Boolean {
        return assetDecryptor.requiresCustomPassword()
    }
}

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

    // Activation
    @SerializedName("activationEnabled")
    val activationEnabled: Boolean = false,

    @SerializedName("activationCodes")
    val activationCodes: List<String> = emptyList(),
    
    @SerializedName("activationRequireEveryTime")
    val activationRequireEveryTime: Boolean = false,

    @SerializedName("activationDialogTitle")
    val activationDialogTitle: String = "",

    @SerializedName("activationDialogSubtitle")
    val activationDialogSubtitle: String = "",

    @SerializedName("activationDialogInputLabel")
    val activationDialogInputLabel: String = "",

    @SerializedName("activationDialogButtonText")
    val activationDialogButtonText: String = "",

    // Ad block
    @SerializedName("adBlockEnabled")
    val adBlockEnabled: Boolean = false,

    @SerializedName("adBlockRules")
    val adBlockRules: List<String> = emptyList(),

    // Announcement
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
    
    @SerializedName("announcementRequireConfirmation")
    val announcementRequireConfirmation: Boolean = false,
    
    @SerializedName("announcementAllowNeverShow")
    val announcementAllowNeverShow: Boolean = false,

    // WebView
    @SerializedName("webViewConfig")
    val webViewConfig: WebViewShellConfig = WebViewShellConfig(),

    // Splash
    @SerializedName("splashEnabled")
    val splashEnabled: Boolean = false,

    @SerializedName("splashType")
    val splashType: String = "IMAGE",

    @SerializedName("splashDuration")
    val splashDuration: Int = 3,

    @SerializedName("splashClickToSkip")
    val splashClickToSkip: Boolean = true,

    // Video crop
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
    
    // App type
    @SerializedName("appType")
    val appType: String = "WEB",
    
    @SerializedName("mediaConfig")
    val mediaConfig: MediaShellConfig = MediaShellConfig(),
    
    // HTML
    @SerializedName("htmlConfig")
    val htmlConfig: HtmlShellConfig = HtmlShellConfig(),
    
    // BGM
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
    
    // Theme
    @SerializedName("themeType")
    val themeType: String = "AURORA",
    
    @SerializedName("darkMode")
    val darkMode: String = "SYSTEM",
    
    // Translate
    @SerializedName("translateEnabled")
    val translateEnabled: Boolean = false,
    
    @SerializedName("translateTargetLanguage")
    val translateTargetLanguage: String = "zh-CN",
    
    @SerializedName("translateShowButton")
    val translateShowButton: Boolean = true,
    
    // Extension
    @SerializedName("extensionFabIcon")
    val extensionFabIcon: String = "",
    
    @SerializedName("extensionModuleIds")
    val extensionModuleIds: List<String> = emptyList(),
    
    // 嵌入的扩展模块完整数据（APK导出时嵌入）
    @SerializedName("embeddedExtensionModules")
    val embeddedExtensionModules: List<EmbeddedShellModule> = emptyList(),
    
    // Auto start
    @SerializedName("autoStartConfig")
    val autoStartConfig: AutoStartShellConfig? = null,

    // Forced run
    @SerializedName("forcedRunConfig")
    val forcedRunConfig: ForcedRunConfig? = null,
    
    // Isolation
    @SerializedName("isolationEnabled")
    val isolationEnabled: Boolean = false,
    
    @SerializedName("isolationConfig")
    val isolationConfig: IsolationShellConfig? = null,
    
    // Background run
    @SerializedName("backgroundRunEnabled")
    val backgroundRunEnabled: Boolean = false,
    
    @SerializedName("backgroundRunConfig")
    val backgroundRunConfig: BackgroundRunShellConfig? = null,
    
    // Notification
    @SerializedName("notificationEnabled")
    val notificationEnabled: Boolean = false,
    
    @SerializedName("notificationConfig")
    val notificationConfig: NotificationShellConfig? = null,
    
    // Black tech
    @SerializedName("blackTechConfig")
    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,
    
    // Disguise
    @SerializedName("disguiseConfig")
    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null,
    
    // Browser disguise
    @SerializedName("browserDisguiseConfig")
    val browserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null,
    
    // Device disguise
    @SerializedName("deviceDisguiseConfig")
    val deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null,
    
    // Language
    @SerializedName("language")
    val language: String = "CHINESE",  // CHINESE, ENGLISH, ARABIC
    
    // Gallery
    @SerializedName("galleryConfig")
    val galleryConfig: GalleryShellConfig = GalleryShellConfig(),
    
    // WordPress
    @SerializedName("wordpressConfig")
    val wordpressConfig: WordPressShellConfig = WordPressShellConfig(),
    
    // Node.js
    @SerializedName("nodejsConfig")
    val nodejsConfig: NodeJsShellConfig = NodeJsShellConfig(),
    
    // Deep link
    @SerializedName("deepLinkEnabled")
    val deepLinkEnabled: Boolean = false,
    
    @SerializedName("deepLinkHosts")
    val deepLinkHosts: List<String> = emptyList(),
    
    // PHP
    @SerializedName("phpAppConfig")
    val phpAppConfig: PhpAppShellConfig = PhpAppShellConfig(),
    
    // Python
    @SerializedName("pythonAppConfig")
    val pythonAppConfig: PythonAppShellConfig = PythonAppShellConfig(),
    
    // Go
    @SerializedName("goAppConfig")
    val goAppConfig: GoAppShellConfig = GoAppShellConfig(),
    
    // Multi-web
    @SerializedName("multiWebConfig")
    val multiWebConfig: MultiWebShellConfig = MultiWebShellConfig(),
    
    // Cloud SDK
    @SerializedName("cloudSdkConfig")
    val cloudSdkConfig: CloudSdkConfig = CloudSdkConfig()
)

/** Shell APK 中嵌入的扩展模块 */
data class EmbeddedShellModule(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("description")
    val description: String = "",
    
    @SerializedName("icon")
    val icon: String = "package",
    
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
    companion object {
        private val GSON = com.webtoapp.util.GsonProvider.gson
        /** URL 匹配 Regex 缓存 */
        private val regexCache = android.util.LruCache<String, Regex>(32)
    }
    
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
        
        // Check包含规则
        return includeRules.any { matchRule(url, it) }
    }
    
    private fun matchRule(url: String, rule: EmbeddedUrlMatch): Boolean {
        return try {
            val cacheKey = if (rule.isRegex) rule.pattern else "glob:${rule.pattern}"
            val regex = regexCache.get(cacheKey) ?: run {
                val r = if (rule.isRegex) {
                    Regex(rule.pattern)
                } else {
                    // Wildcard: * matches any
                    val regexPattern = rule.pattern
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .replace("?", ".")
                    Regex(regexPattern, RegexOption.IGNORE_CASE)
                }
                regexCache.put(cacheKey, r)
                r
            }
            regex.containsMatchIn(url)
        } catch (e: Exception) {
            url.contains(rule.pattern, ignoreCase = true)
        }
    }
    
    @Transient
    @Volatile
    private var _cachedCode: String? = null
    
    /** Generate executable JS code (cached). */
    fun generateExecutableCode(): String {
        _cachedCode?.let { return it }
        val configJson = GSON.toJson(configValues)
        return """
            (function() {
                'use strict';
                // Module配置
                const __MODULE_CONFIG__ = $configJson;
                const __MODULE_INFO__ = {
                    id: '${id.escapeForJsSingleQuote()}',
                    name: '${name.escapeForJsSingleQuote()}',
                    version: '1.0.0'
                };
                
                // Configure访问函数
                function getConfig(key, defaultValue) {
                    return __MODULE_CONFIG__[key] !== undefined ? __MODULE_CONFIG__[key] : defaultValue;
                }
                
                // CSS 注入
                ${if (cssCode.isNotBlank()) """
                (function() {
                    const style = document.createElement('style');
                    style.id = 'ext-module-${id}';
                    style.textContent = `${cssCode.escapeForJsTemplate()}`;
                    (document.head || document.documentElement).appendChild(style);
                })();
                """ else ""}
                
                // User代码
                try {
                    $code
                } catch(e) {
                    console.error('[ExtModule: ${name.escapeForJsSingleQuote()}] Error:', e);
                }
            })();
        """.trimIndent().also { _cachedCode = it }
    }
}

data class EmbeddedUrlMatch(
    @SerializedName("pattern")
    val pattern: String = "",
    
    @SerializedName("isRegex")
    val isRegex: Boolean = false,
    
    @SerializedName("exclude")
    val exclude: Boolean = false
)

data class GalleryShellConfig(
    @SerializedName("items")
    val items: List<GalleryShellItem> = emptyList(),
    
    @SerializedName("playMode")
    val playMode: String = "SEQUENTIAL",  // SEQUENTIAL, SHUFFLE, SINGLE_LOOP
    
    @SerializedName("imageInterval")
    val imageInterval: Int = 3,
    
    @SerializedName("loop")
    val loop: Boolean = true,
    
    @SerializedName("autoPlay")
    val autoPlay: Boolean = false,
    
    @SerializedName("backgroundColor")
    val backgroundColor: String = "#000000",
    
    @SerializedName("showThumbnailBar")
    val showThumbnailBar: Boolean = true,
    
    @SerializedName("showMediaInfo")
    val showMediaInfo: Boolean = true,
    
    @SerializedName("orientation")
    val orientation: String = "PORTRAIT",  // PORTRAIT, LANDSCAPE
    
    @SerializedName("enableAudio")
    val enableAudio: Boolean = true,
    
    @SerializedName("videoAutoNext")
    val videoAutoNext: Boolean = true
)

data class GalleryShellItem(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("assetPath")
    val assetPath: String = "",  // assets/gallery/item_0.{png|mp4}
    
    @SerializedName("type")
    val type: String = "IMAGE",  // IMAGE or VIDEO
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("duration")
    val duration: Long = 0,
    
    @SerializedName("thumbnailPath")
    val thumbnailPath: String? = null  // assets/gallery/thumb_0.jpg
)

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
    val landscape: Boolean = false,
    
    @SerializedName("keepScreenOn")
    val keepScreenOn: Boolean = true  // 保持屏幕常亮
)

data class WordPressShellConfig(
    @SerializedName("siteTitle")
    val siteTitle: String = "My Site",
    
    @SerializedName("phpPort")
    val phpPort: Int = 0,
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

data class NodeJsShellConfig(
    @SerializedName("mode")
    val mode: String = "STATIC",  // STATIC, BACKEND, FULLSTACK
    
    @SerializedName("port")
    val port: Int = 0,  // Node.js服务器端口（0=自动分配）
    
    @SerializedName("entryFile")
    val entryFile: String = "",  // 入口文件（backend/fullstack模式需要）
    
    @SerializedName("envVars")
    val envVars: Map<String, String> = emptyMap(),  // 环境变量
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

data class PhpAppShellConfig(
    @SerializedName("framework")
    val framework: String = "",
    
    @SerializedName("documentRoot")
    val documentRoot: String = "",
    
    @SerializedName("entryFile")
    val entryFile: String = "index.php",
    
    @SerializedName("port")
    val port: Int = 0,
    
    @SerializedName("envVars")
    val envVars: Map<String, String> = emptyMap(),
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

data class PythonAppShellConfig(
    @SerializedName("framework")
    val framework: String = "",
    
    @SerializedName("entryFile")
    val entryFile: String = "app.py",
    
    @SerializedName("entryModule")
    val entryModule: String = "",
    
    @SerializedName("serverType")
    val serverType: String = "builtin",
    
    @SerializedName("port")
    val port: Int = 0,
    
    @SerializedName("envVars")
    val envVars: Map<String, String> = emptyMap(),
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

data class GoAppShellConfig(
    @SerializedName("framework")
    val framework: String = "",
    
    @SerializedName("binaryName")
    val binaryName: String = "",
    
    @SerializedName("port")
    val port: Int = 0,
    
    @SerializedName("staticDir")
    val staticDir: String = "",
    
    @SerializedName("envVars")
    val envVars: Map<String, String> = emptyMap(),
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

data class MultiWebShellConfig(
    @SerializedName("sites")
    val sites: List<MultiWebSiteShellConfig> = emptyList(),
    
    @SerializedName("displayMode")
    val displayMode: String = "TABS",
    
    @SerializedName("refreshInterval")
    val refreshInterval: Int = 30,
    
    @SerializedName("showSiteIcons")
    val showSiteIcons: Boolean = true,
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

data class MultiWebSiteShellConfig(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("url")
    val url: String = "",
    
    @SerializedName("iconEmoji")
    val iconEmoji: String = "",
    
    @SerializedName("category")
    val category: String = "",
    
    @SerializedName("cssSelector")
    val cssSelector: String = "",
    
    @SerializedName("linkSelector")
    val linkSelector: String = "",
    
    @SerializedName("enabled")
    val enabled: Boolean = true
)

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
) {
    /** Validate entryFile has a non-empty name part. */
    fun getValidEntryFile(): String {
        return entryFile.takeIf { 
            it.isNotBlank() && it.substringBeforeLast(".").isNotBlank() 
        } ?: "index.html"
    }
}

data class DnsShellConfig(
    @SerializedName("provider")
    val provider: String = "cloudflare",

    @SerializedName("customDohUrl")
    val customDohUrl: String = "",

    @SerializedName("dohMode")
    val dohMode: String = "automatic", // automatic, strict

    @SerializedName("bypassSystemDns")
    val bypassSystemDns: Boolean = false
)

data class WebViewShellConfig(
    @SerializedName("javaScriptEnabled")
    val javaScriptEnabled: Boolean = true,

    @SerializedName("domStorageEnabled")
    val domStorageEnabled: Boolean = true,

    @SerializedName("allowFileAccess")
    val allowFileAccess: Boolean = false,

    @SerializedName("allowContentAccess")
    val allowContentAccess: Boolean = true,

    @SerializedName("cacheEnabled")
    val cacheEnabled: Boolean = true,

    @SerializedName("zoomEnabled")
    val zoomEnabled: Boolean = true,

    @SerializedName("desktopMode")
    val desktopMode: Boolean = false,

    @SerializedName("userAgent")
    val userAgent: String? = null,
    
    @SerializedName("userAgentMode")
    val userAgentMode: String = "DEFAULT",
    
    @SerializedName("customUserAgent")
    val customUserAgent: String? = null,

    @SerializedName("hideToolbar")
    val hideToolbar: Boolean = false,
    
    @SerializedName("hideBrowserToolbar")
    val hideBrowserToolbar: Boolean = false,  // 仅隐藏浏览器工具栏（不触发沉浸式，保留系统状态栏和导航栏）
    
    @SerializedName("showStatusBarInFullscreen")
    val showStatusBarInFullscreen: Boolean = false,  // Fullscreen模式下是否显示状态栏
    
    @SerializedName("showNavigationBarInFullscreen")
    val showNavigationBarInFullscreen: Boolean = false,  // Fullscreen模式下是否显示导航栏
    
    @SerializedName("showToolbarInFullscreen")
    val showToolbarInFullscreen: Boolean = false,  // Fullscreen模式下是否显示顶部导航栏
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false,
    
    @SerializedName("orientationMode")
    val orientationMode: String = "PORTRAIT", // PORTRAIT, LANDSCAPE, REVERSE_PORTRAIT, REVERSE_LANDSCAPE, SENSOR_PORTRAIT, SENSOR_LANDSCAPE, AUTO
    
    @SerializedName("injectScripts")
    val injectScripts: List<ShellUserScript> = emptyList(),
    
    @SerializedName("statusBarColorMode")
    val statusBarColorMode: String = "THEME", // THEME, TRANSPARENT, CUSTOM
    
    @SerializedName("statusBarColor")
    val statusBarColor: String? = null, // Custom状态栏颜色（仅 CUSTOM 模式生效）
    
    @SerializedName("statusBarDarkIcons")
    val statusBarDarkIcons: Boolean? = null, // Status bar图标颜色：true=深色图标，false=浅色图标，null=自动
    
    @SerializedName("statusBarBackgroundType")
    val statusBarBackgroundType: String = "COLOR", // COLOR, IMAGE
    
    @SerializedName("statusBarBackgroundImage")
    val statusBarBackgroundImage: String? = null, // Cropped image path（assets中的路径）
    
    @SerializedName("statusBarBackgroundAlpha")
    val statusBarBackgroundAlpha: Float = 1.0f, // Alpha 0.0-1.0
    
    @SerializedName("statusBarHeightDp")
    val statusBarHeightDp: Int = 0, // Custom高度dp（0=系统默认）

    // Status bar dark mode config
    @SerializedName("statusBarColorModeDark")
    val statusBarColorModeDark: String = "THEME",

    @SerializedName("statusBarColorDark")
    val statusBarColorDark: String? = null,

    @SerializedName("statusBarDarkIconsDark")
    val statusBarDarkIconsDark: Boolean? = null,

    @SerializedName("statusBarBackgroundTypeDark")
    val statusBarBackgroundTypeDark: String = "COLOR",

    @SerializedName("statusBarBackgroundImageDark")
    val statusBarBackgroundImageDark: String? = null,

    @SerializedName("statusBarBackgroundAlphaDark")
    val statusBarBackgroundAlphaDark: Float = 1.0f,
    
    @SerializedName("longPressMenuEnabled")
    val longPressMenuEnabled: Boolean = true, // Yes否启用长按菜单
    
    @SerializedName("longPressMenuStyle")
    val longPressMenuStyle: String = "FULL", // DISABLED, SIMPLE, FULL
    
    @SerializedName("adBlockToggleEnabled")
    val adBlockToggleEnabled: Boolean = false, // Allow用户在运行时切换广告拦截开关
    
    @SerializedName("popupBlockerEnabled")
    val popupBlockerEnabled: Boolean = false, // 默认关闭，避免拦截合法的 window.open 导致按钮/菜单/搜索失效
    
    @SerializedName("popupBlockerToggleEnabled")
    val popupBlockerToggleEnabled: Boolean = false, // Allow用户在运行时切换弹窗拦截开关
    
    @SerializedName("openExternalLinks")
    val openExternalLinks: Boolean = false, // External链接是否在浏览器打开
    
    // Browser compatibility
    @SerializedName("initialScale")
    val initialScale: Int = 0,
    
    @SerializedName("viewportMode")
    val viewportMode: String = "DEFAULT", // DEFAULT, FIT_SCREEN, DESKTOP
    
    @SerializedName("customViewportWidth")
    val customViewportWidth: Int = 0, // 自定义视口宽度（0=自动）
    
    @SerializedName("newWindowBehavior")
    val newWindowBehavior: String = "SAME_WINDOW",
    
    @SerializedName("enablePaymentSchemes")
    val enablePaymentSchemes: Boolean = true,
    
    @SerializedName("enableShareBridge")
    val enableShareBridge: Boolean = true,
    
    @SerializedName("enableZoomPolyfill")
    val enableZoomPolyfill: Boolean = true,
    
    // Advanced
    @SerializedName("enableCrossOriginIsolation")
    val enableCrossOriginIsolation: Boolean = false,
    
    @SerializedName("hideUrlPreview")
    val hideUrlPreview: Boolean = false,
    
    @SerializedName("disableShields")
    val disableShields: Boolean = true, // 默认禁用 BrowserShields - 避免误杀关键服务 (OAuth/CAPTCHA/错误监控等)
    
    @SerializedName("keepScreenOn")
    val keepScreenOn: Boolean = false, // [向后兼容] 保持屏幕常亮
    
    @SerializedName("screenAwakeMode")
    val screenAwakeMode: String = "OFF", // OFF, ALWAYS, TIMED
    
    @SerializedName("screenAwakeTimeoutMinutes")
    val screenAwakeTimeoutMinutes: Int = 30, // 定时常亮时长（分钟），仅 TIMED 模式
    
    @SerializedName("screenBrightness")
    val screenBrightness: Int = -1, // 屏幕亮度：-1=跟随系统, 0-100=自定义百分比
    
    // Floating back button
    @SerializedName("showFloatingBackButton")
    val showFloatingBackButton: Boolean = true, // 全屏模式下是否显示悬浮返回按钮

    // Keyboard
    @SerializedName("keyboardAdjustMode")
    val keyboardAdjustMode: String = "RESIZE", // RESIZE, NOTHING

    // Pull refresh / Video fullscreen
    @SerializedName("swipeRefreshEnabled")
    val swipeRefreshEnabled: Boolean = true,
    
    @SerializedName("fullscreenEnabled")
    val fullscreenEnabled: Boolean = true,

    // Performance / PWA offline
    @SerializedName("performanceOptimization")
    val performanceOptimization: Boolean = false,
    
    @SerializedName("pwaOfflineEnabled")
    val pwaOfflineEnabled: Boolean = false,
    
    @SerializedName("pwaOfflineStrategy")
    val pwaOfflineStrategy: String = "NETWORK_FIRST",

    // Error page
    @SerializedName("errorPageConfig")
    val errorPageConfig: ErrorPageShellConfig = ErrorPageShellConfig(),

    // Floating window
    @SerializedName("floatingWindowConfig")
    val floatingWindowConfig: FloatingWindowShellConfig = FloatingWindowShellConfig(),

    // Proxy
    @SerializedName("proxyMode")
    val proxyMode: String = "NONE",  // NONE, STATIC, PAC
    
    @SerializedName("proxyHost")
    val proxyHost: String = "",      // 固定代理主机（STATIC 模式）
    
    @SerializedName("proxyPort")
    val proxyPort: Int = 0,          // 固定代理端口（STATIC 模式）
    
    @SerializedName("proxyType")
    val proxyType: String = "HTTP",  // HTTP, HTTPS, SOCKS5（STATIC 模式）
    
    @SerializedName("pacUrl")
    val pacUrl: String = "",         // PAC 脚本 URL（PAC 模式）
    
    @SerializedName("proxyBypassRules")
    val proxyBypassRules: List<String> = emptyList(), // 代理绕过规则

    // DNS
    @SerializedName("dnsMode")
    val dnsMode: String = "SYSTEM",  // SYSTEM, DOH

    @SerializedName("dnsConfig")
    val dnsConfig: DnsShellConfig = DnsShellConfig()
)

data class FloatingWindowShellConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,
    
    @SerializedName("windowSizePercent")
    val windowSizePercent: Int = 80,       // [向后兼容] 窗口大小百分比 50-100
    
    @SerializedName("widthPercent")
    val widthPercent: Int = 80,            // 独立宽度百分比 30-100
    
    @SerializedName("heightPercent")
    val heightPercent: Int = 80,           // 独立高度百分比 30-100
    
    @SerializedName("lockAspectRatio")
    val lockAspectRatio: Boolean = true,   // 锁定宽高比
    
    @SerializedName("opacity")
    val opacity: Int = 100,                 // 透明度百分比 30-100
    
    @SerializedName("cornerRadius")
    val cornerRadius: Int = 16,             // 圆角半径 dp (0-32)
    
    @SerializedName("borderStyle")
    val borderStyle: String = "SUBTLE",     // NONE, SUBTLE, GLOW, ACCENT
    
    @SerializedName("showTitleBar")
    val showTitleBar: Boolean = true,       // 显示标题栏
    
    @SerializedName("autoHideTitleBar")
    val autoHideTitleBar: Boolean = false,  // 自动隐藏标题栏
    
    @SerializedName("startMinimized")
    val startMinimized: Boolean = false,    // 启动时最小化
    
    @SerializedName("rememberPosition")
    val rememberPosition: Boolean = true,   // 记住位置
    
    @SerializedName("edgeSnapping")
    val edgeSnapping: Boolean = true,       // 边缘吸附
    
    @SerializedName("showResizeHandle")
    val showResizeHandle: Boolean = true,   // 显示缩放手柄
    
    @SerializedName("lockPosition")
    val lockPosition: Boolean = false       // 锁定位置
)

data class ErrorPageShellConfig(
    @SerializedName("mode")
    val mode: String = "BUILTIN_STYLE", // DEFAULT, BUILTIN_STYLE, CUSTOM_HTML, CUSTOM_MEDIA
    
    @SerializedName("builtInStyle")
    val builtInStyle: String = "MATERIAL",
    
    @SerializedName("showMiniGame")
    val showMiniGame: Boolean = false,
    
    @SerializedName("miniGameType")
    val miniGameType: String = "RANDOM",
    
    @SerializedName("autoRetrySeconds")
    val autoRetrySeconds: Int = 15
)

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

data class AutoStartShellConfig(
    @SerializedName("bootStartEnabled")
    val bootStartEnabled: Boolean = false,
    
    @SerializedName("scheduledStartEnabled")
    val scheduledStartEnabled: Boolean = false,
    
    @SerializedName("scheduledTime")
    val scheduledTime: String = "08:00",
    
    @SerializedName("scheduledDays")
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
)

data class IsolationShellConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,
    
    @SerializedName("fingerprintConfig")
    val fingerprintConfig: FingerprintShellConfig = FingerprintShellConfig(),
    
    @SerializedName("headerConfig")
    val headerConfig: HeaderShellConfig = HeaderShellConfig(),
    
    @SerializedName("ipSpoofConfig")
    val ipSpoofConfig: IpSpoofShellConfig = IpSpoofShellConfig(),
    
    @SerializedName("storageIsolation")
    val storageIsolation: Boolean = true,
    
    @SerializedName("blockWebRTC")
    val blockWebRTC: Boolean = true,
    
    @SerializedName("protectCanvas")
    val protectCanvas: Boolean = true,
    
    @SerializedName("protectAudio")
    val protectAudio: Boolean = true,
    
    @SerializedName("protectWebGL")
    val protectWebGL: Boolean = true,
    
    @SerializedName("protectFonts")
    val protectFonts: Boolean = false,
    
    @SerializedName("spoofTimezone")
    val spoofTimezone: Boolean = false,
    
    @SerializedName("customTimezone")
    val customTimezone: String? = null,
    
    @SerializedName("spoofLanguage")
    val spoofLanguage: Boolean = false,
    
    @SerializedName("customLanguage")
    val customLanguage: String? = null,
    
    @SerializedName("spoofScreen")
    val spoofScreen: Boolean = false,
    
    @SerializedName("customScreenWidth")
    val customScreenWidth: Int? = null,
    
    @SerializedName("customScreenHeight")
    val customScreenHeight: Int? = null
) {
    fun toIsolationConfig(): com.webtoapp.core.isolation.IsolationConfig {
        return com.webtoapp.core.isolation.IsolationConfig(
            enabled = enabled,
            fingerprintConfig = com.webtoapp.core.isolation.FingerprintConfig(
                randomize = fingerprintConfig.randomize,
                regenerateOnLaunch = fingerprintConfig.regenerateOnLaunch,
                customUserAgent = fingerprintConfig.customUserAgent,
                randomUserAgent = fingerprintConfig.randomUserAgent,
                fingerprintId = fingerprintConfig.fingerprintId
            ),
            headerConfig = com.webtoapp.core.isolation.HeaderConfig(
                enabled = headerConfig.enabled,
                randomizeOnRequest = headerConfig.randomizeOnRequest,
                dnt = headerConfig.dnt,
                spoofClientHints = headerConfig.spoofClientHints,
                refererPolicy = try {
                    com.webtoapp.core.isolation.RefererPolicy.valueOf(headerConfig.refererPolicy)
                } catch (e: Exception) {
                    com.webtoapp.core.isolation.RefererPolicy.STRICT_ORIGIN
                }
            ),
            ipSpoofConfig = com.webtoapp.core.isolation.IpSpoofConfig(
                enabled = ipSpoofConfig.enabled,
                spoofMethod = try {
                    com.webtoapp.core.isolation.IpSpoofMethod.valueOf(ipSpoofConfig.spoofMethod)
                } catch (e: Exception) {
                    com.webtoapp.core.isolation.IpSpoofMethod.HEADER
                },
                customIp = ipSpoofConfig.customIp,
                randomIpRange = try {
                    com.webtoapp.core.isolation.IpRange.valueOf(ipSpoofConfig.randomIpRange)
                } catch (e: Exception) {
                    com.webtoapp.core.isolation.IpRange.GLOBAL
                },
                searchKeyword = ipSpoofConfig.searchKeyword,
                xForwardedFor = ipSpoofConfig.xForwardedFor,
                xRealIp = ipSpoofConfig.xRealIp,
                clientIp = ipSpoofConfig.clientIp
            ),
            storageIsolation = storageIsolation,
            blockWebRTC = blockWebRTC,
            protectCanvas = protectCanvas,
            protectAudio = protectAudio,
            protectWebGL = protectWebGL,
            protectFonts = protectFonts,
            spoofTimezone = spoofTimezone,
            customTimezone = customTimezone,
            spoofLanguage = spoofLanguage,
            customLanguage = customLanguage,
            spoofScreen = spoofScreen,
            customScreenWidth = customScreenWidth,
            customScreenHeight = customScreenHeight
        )
    }
}

data class FingerprintShellConfig(
    @SerializedName("randomize")
    val randomize: Boolean = true,
    
    @SerializedName("regenerateOnLaunch")
    val regenerateOnLaunch: Boolean = false,
    
    @SerializedName("customUserAgent")
    val customUserAgent: String? = null,
    
    @SerializedName("randomUserAgent")
    val randomUserAgent: Boolean = true,
    
    @SerializedName("fingerprintId")
    val fingerprintId: String = java.util.UUID.randomUUID().toString()
)

data class HeaderShellConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,
    
    @SerializedName("randomizeOnRequest")
    val randomizeOnRequest: Boolean = false,
    
    @SerializedName("dnt")
    val dnt: Boolean = true,
    
    @SerializedName("spoofClientHints")
    val spoofClientHints: Boolean = true,
    
    @SerializedName("refererPolicy")
    val refererPolicy: String = "STRICT_ORIGIN"
)

data class IpSpoofShellConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,
    
    @SerializedName("spoofMethod")
    val spoofMethod: String = "HEADER",
    
    @SerializedName("customIp")
    val customIp: String? = null,
    
    @SerializedName("randomIpRange")
    val randomIpRange: String = "GLOBAL",
    
    @SerializedName("searchKeyword")
    val searchKeyword: String? = null,
    
    @SerializedName("xForwardedFor")
    val xForwardedFor: Boolean = true,
    
    @SerializedName("xRealIp")
    val xRealIp: Boolean = true,
    
    @SerializedName("clientIp")
    val clientIp: Boolean = true
)

data class BackgroundRunShellConfig(
    @SerializedName("notificationTitle")
    val notificationTitle: String = "",
    
    @SerializedName("notificationContent")
    val notificationContent: String = "",
    
    @SerializedName("showNotification")
    val showNotification: Boolean = true,
    
    @SerializedName("keepCpuAwake")
    val keepCpuAwake: Boolean = true
)

data class NotificationShellConfig(
    @SerializedName("type")
    val type: String = "none", // none, web_api, polling

    @SerializedName("pollUrl")
    val pollUrl: String = "",

    @SerializedName("pollIntervalMinutes")
    val pollIntervalMinutes: Int = 15,

    @SerializedName("pollMethod")
    val pollMethod: String = "GET",

    @SerializedName("pollHeaders")
    val pollHeaders: String = "",

    @SerializedName("clickUrl")
    val clickUrl: String = ""
)

