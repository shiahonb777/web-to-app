package com.webtoapp.core.shell

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.crypto.AssetDecryptor
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.logging.AppLogger

/** Escape a string for use inside a JS single-quoted string literal. */
private fun String.escapeForJsSingleQuote(): String =
    this.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")

/** Escape a string for use inside a JS template literal (backtick). */
private fun String.escapeForJsTemplate(): String =
    this.replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("\${", "\\\${")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

/**
 * Shell 模式管理器
 * 检测应用是否以 Shell 模式运行（独立 WebApp）
 * 支持加密和非加密配置文件
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
     * 加载配置文件（支持加密和非加密）
     */
    private fun loadConfig(): ShellConfig? {
        if (configLoaded) return cachedConfig

        synchronized(this) {
            if (configLoaded) return cachedConfig

            configLoaded = true
            cachedConfig = try {
            AppLogger.d(TAG, "尝试加载配置文件: $CONFIG_FILE")
            
            // 使用 AssetDecryptor 自动处理加密/非加密配置
            val jsonStr = try {
                assetDecryptor.loadAssetAsString(CONFIG_FILE)
            } catch (e: Exception) {
                AppLogger.e(TAG, "AssetDecryptor 加载失败，尝试直接读取", e)
                // 回退：直接从 assets 读取（非加密模式）
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
                // 调试模式下输出详细配置信息，Release 构建不会泄露敏感配置
                AppLogger.d(TAG, "WebView UA配置: userAgentMode=${config?.webViewConfig?.userAgentMode}")
                AppLogger.d(TAG, "注入脚本: ${config?.webViewConfig?.injectScripts?.size ?: 0} 个")
                AppLogger.d(TAG, "扩展模块: extensionModuleIds=${config?.extensionModuleIds?.size ?: 0}, embeddedExtensionModules=${config?.embeddedExtensionModules?.size ?: 0}")
            }
            // Verify配置有效性
            // HTML/FRONTEND应用不需要targetUrl，使用嵌入的HTML文件
            // Media应用也不需要targetUrl，使用嵌入的媒体文件
            // Gallery应用也不需要targetUrl，使用嵌入的图片/视频列表
            val isValid = when {
                normalizedAppType == "HTML" || normalizedAppType == "FRONTEND" -> {
                    // Verify entryFile 必须有文件名部分（不能只是 .html 或空字符串）
                    val entryFile = config.htmlConfig.entryFile
                    entryFile.isNotBlank() && entryFile.substringBeforeLast(".").isNotBlank()
                }
                normalizedAppType == "IMAGE" || normalizedAppType == "VIDEO" -> true // Media应用
                normalizedAppType == "GALLERY" -> true // Gallery应用（图片/视频画廊）
                normalizedAppType == "WORDPRESS" -> true // WordPress应用（离线PHP+SQLite）
                normalizedAppType == "NODEJS_APP" -> true // Node.js应用（Express/Fastify/Nest等后端）
                normalizedAppType == "PHP_APP" -> true // PHP应用（Laravel/ThinkPHP等）
                normalizedAppType == "PYTHON_APP" -> true // Python应用（Flask/Django/FastAPI等）
                normalizedAppType == "GO_APP" -> true // Go应用（Gin/Fiber/Echo等）
                else -> !config?.targetUrl.isNullOrBlank() // WEB应用需要targetUrl
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
            // 捕获所有 Error（包括 NoClassDefFoundError 等）
            AppLogger.e(TAG, "加载配置文件时发生严重错误", e)
            null
        }

            return cachedConfig
        }
    }
    
    /**
     * 重新加载配置
     */
    fun reload() {
        synchronized(this) {
            configLoaded = false
            cachedConfig = null
            assetDecryptor.clearCache()
        }
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

    // Activation码配置
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

    // Ad拦截配置
    @SerializedName("adBlockEnabled")
    val adBlockEnabled: Boolean = false,

    @SerializedName("adBlockRules")
    val adBlockRules: List<String> = emptyList(),

    // Announcement配置
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

    // WebView 配置
    @SerializedName("webViewConfig")
    val webViewConfig: WebViewShellConfig = WebViewShellConfig(),

    // Start画面配置
    @SerializedName("splashEnabled")
    val splashEnabled: Boolean = false,

    @SerializedName("splashType")
    val splashType: String = "IMAGE",

    @SerializedName("splashDuration")
    val splashDuration: Int = 3,

    @SerializedName("splashClickToSkip")
    val splashClickToSkip: Boolean = true,

    // Video裁剪配置
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
    
    // Media应用配置
    @SerializedName("appType")
    val appType: String = "WEB",
    
    @SerializedName("mediaConfig")
    val mediaConfig: MediaShellConfig = MediaShellConfig(),
    
    // HTML应用配置
    @SerializedName("htmlConfig")
    val htmlConfig: HtmlShellConfig = HtmlShellConfig(),
    
    // Background music配置
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
    
    // Theme配置
    @SerializedName("themeType")
    val themeType: String = "AURORA",
    
    @SerializedName("darkMode")
    val darkMode: String = "SYSTEM",
    
    // Web page自动翻译配置
    @SerializedName("translateEnabled")
    val translateEnabled: Boolean = false,
    
    @SerializedName("translateTargetLanguage")
    val translateTargetLanguage: String = "zh-CN",
    
    @SerializedName("translateShowButton")
    val translateShowButton: Boolean = true,
    
    // 扩展模块配置
    @SerializedName("extensionFabIcon")
    val extensionFabIcon: String = "",
    
    @SerializedName("extensionModuleIds")
    val extensionModuleIds: List<String> = emptyList(),
    
    // 嵌入的扩展模块完整数据（APK导出时嵌入）
    @SerializedName("embeddedExtensionModules")
    val embeddedExtensionModules: List<EmbeddedShellModule> = emptyList(),
    
    // 自启动配置
    @SerializedName("autoStartConfig")
    val autoStartConfig: AutoStartShellConfig? = null,

    // 强制运行配置
    @SerializedName("forcedRunConfig")
    val forcedRunConfig: ForcedRunConfig? = null,
    
    // 独立环境/多开配置
    @SerializedName("isolationEnabled")
    val isolationEnabled: Boolean = false,
    
    @SerializedName("isolationConfig")
    val isolationConfig: IsolationShellConfig? = null,
    
    // 后台运行配置
    @SerializedName("backgroundRunEnabled")
    val backgroundRunEnabled: Boolean = false,
    
    @SerializedName("backgroundRunConfig")
    val backgroundRunConfig: BackgroundRunShellConfig? = null,
    
    // 黑科技功能配置（独立模块）
    @SerializedName("blackTechConfig")
    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,
    
    // App伪装配置（独立模块）
    @SerializedName("disguiseConfig")
    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null,
    
    // 界面语言配置
    @SerializedName("language")
    val language: String = "CHINESE",  // CHINESE, ENGLISH, ARABIC
    
    // Gallery 画廊应用配置
    @SerializedName("galleryConfig")
    val galleryConfig: GalleryShellConfig = GalleryShellConfig(),
    
    // WordPress 应用配置
    @SerializedName("wordpressConfig")
    val wordpressConfig: WordPressShellConfig = WordPressShellConfig(),
    
    // Node.js 应用配置
    @SerializedName("nodejsConfig")
    val nodejsConfig: NodeJsShellConfig = NodeJsShellConfig(),
    
    // Deep link配置
    @SerializedName("deepLinkEnabled")
    val deepLinkEnabled: Boolean = false,
    
    @SerializedName("deepLinkHosts")
    val deepLinkHosts: List<String> = emptyList(),
    
    // PHP 应用配置
    @SerializedName("phpAppConfig")
    val phpAppConfig: PhpAppShellConfig = PhpAppShellConfig(),
    
    // Python 应用配置
    @SerializedName("pythonAppConfig")
    val pythonAppConfig: PythonAppShellConfig = PythonAppShellConfig(),
    
    // Go 应用配置
    @SerializedName("goAppConfig")
    val goAppConfig: GoAppShellConfig = GoAppShellConfig(),
    
    // 多站点聚合应用配置
    @SerializedName("multiWebConfig")
    val multiWebConfig: MultiWebShellConfig = MultiWebShellConfig(),
    
    // 云 SDK 配置（导出 APK 内嵌入的云服务参数）
    @SerializedName("cloudSdkConfig")
    val cloudSdkConfig: CloudSdkConfig = CloudSdkConfig()
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
        /** URL 匹配 Regex 缓存，避免每次重建 */
        private val regexCache = android.util.LruCache<String, Regex>(32)
    }
    
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
                    // 通配符匹配：* 匹配任意字符
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
    
    /**
     * 生成可执行的 JavaScript 代码
     * 结果会缓存，避免每次页面加载时重复序列化
     */
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
 * Gallery 画廊应用 Shell 配置
 */
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

/**
 * Gallery 媒体项 Shell 配置
 */
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
    val landscape: Boolean = false,
    
    @SerializedName("keepScreenOn")
    val keepScreenOn: Boolean = true  // 保持屏幕常亮
)

/**
 * HTML应用 Shell 配置
 */
/**
 * WordPress 应用 Shell 配置
 */
data class WordPressShellConfig(
    @SerializedName("siteTitle")
    val siteTitle: String = "My Site",
    
    @SerializedName("phpPort")
    val phpPort: Int = 0,
    
    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

/**
 * Node.js 应用 Shell 配置
 */
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

/**
 * HTML应用 Shell 配置
 */
/**
 * PHP 应用 Shell 配置
 */
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

/**
 * Python 应用 Shell 配置
 */
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

/**
 * Go 应用 Shell 配置
 */
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

/**
 * 多站点聚合 Shell 配置
 */
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
    /**
     * 获取有效的入口文件名
     * 验证 entryFile 必须有文件名部分（不能只是 .html 或空字符串）
     */
    fun getValidEntryFile(): String {
        return entryFile.takeIf { 
            it.isNotBlank() && it.substringBeforeLast(".").isNotBlank() 
        } ?: "index.html"
    }
}

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
    
    @SerializedName("userAgentMode")
    val userAgentMode: String = "DEFAULT",
    
    @SerializedName("customUserAgent")
    val customUserAgent: String? = null,

    @SerializedName("hideToolbar")
    val hideToolbar: Boolean = false,

    @SerializedName("hideBrowserToolbar")
    val hideBrowserToolbar: Boolean = false,
    
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

    // Status bar深色模式配置
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
    val popupBlockerEnabled: Boolean = true, // 启用弹窗拦截器
    
    @SerializedName("popupBlockerToggleEnabled")
    val popupBlockerToggleEnabled: Boolean = false, // Allow用户在运行时切换弹窗拦截开关
    
    @SerializedName("openExternalLinks")
    val openExternalLinks: Boolean = false, // External链接是否在浏览器打开
    
    // ============ 浏览器兼容性增强配置 ============
    @SerializedName("initialScale")
    val initialScale: Int = 0,
    
    @SerializedName("viewportMode")
    val viewportMode: String = "DEFAULT", // DEFAULT, FIT_SCREEN, DESKTOP
    
    @SerializedName("newWindowBehavior")
    val newWindowBehavior: String = "SAME_WINDOW",
    
    @SerializedName("enablePaymentSchemes")
    val enablePaymentSchemes: Boolean = true,
    
    @SerializedName("enableShareBridge")
    val enableShareBridge: Boolean = true,
    
    @SerializedName("enableZoomPolyfill")
    val enableZoomPolyfill: Boolean = true,
    
    // ============ 高级功能配置 ============
    @SerializedName("enableCrossOriginIsolation")
    val enableCrossOriginIsolation: Boolean = false,
    
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
    
    // ============ 悬浮返回按钮 ============
    @SerializedName("showFloatingBackButton")
    val showFloatingBackButton: Boolean = true, // 全屏模式下是否显示悬浮返回按钮

    // ============ 系统导航手势屏蔽 ============
    @SerializedName("blockSystemNavigationGesture")
    val blockSystemNavigationGesture: Boolean = false, // 是否屏蔽系统导航手势（全屏模式下生效，默认关闭）

    // ============ 键盘调整模式 ============
    @SerializedName("keyboardAdjustMode")
    val keyboardAdjustMode: String = "RESIZE", // RESIZE, NOTHING

    // ============ 下拉刷新 / 视频全屏 ============
    @SerializedName("swipeRefreshEnabled")
    val swipeRefreshEnabled: Boolean = true,
    
    @SerializedName("fullscreenEnabled")
    val fullscreenEnabled: Boolean = true,

    // ============ 性能优化 / PWA 离线 ============
    @SerializedName("performanceOptimization")
    val performanceOptimization: Boolean = false,
    
    @SerializedName("pwaOfflineEnabled")
    val pwaOfflineEnabled: Boolean = false,
    
    @SerializedName("pwaOfflineStrategy")
    val pwaOfflineStrategy: String = "NETWORK_FIRST",

    // ============ 网络错误页配置 ============
    @SerializedName("errorPageConfig")
    val errorPageConfig: ErrorPageShellConfig = ErrorPageShellConfig(),

    // ============ 悬浮小窗配置 ============
    @SerializedName("floatingWindowConfig")
    val floatingWindowConfig: FloatingWindowShellConfig = FloatingWindowShellConfig()
)

/**
 * 悬浮小窗 Shell 配置
 */
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

/**
 * 网络错误页 Shell 配置
 */
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
 * 自启动配置（用于 Shell 配置）
 */
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

/**
 * 独立环境/多开配置（用于 Shell 配置）
 */
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
    /**
     * 转换为 IsolationConfig
     */
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

/**
 * 指纹配置（用于 Shell 配置）
 */
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

/**
 * Header 配置（用于 Shell 配置）
 */
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

/**
 * IP 伪装配置（用于 Shell 配置）
 */
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

/**
 * 后台运行配置（用于 Shell 配置）
 */
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

