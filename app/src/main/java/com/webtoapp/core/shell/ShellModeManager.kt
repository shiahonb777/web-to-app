package com.webtoapp.core.shell

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.crypto.AssetDecryptor
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.logging.AppLogger


private fun String.escapeForJsSingleQuote(): String =
    this.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")


private fun String.escapeForJsTemplate(): String =
    this.replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("\${", "\\\${")
        .replace("\n", "\\n")
        .replace("\r", "\\r")




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
                normalizedAppType in listOf("IMAGE", "VIDEO", "GALLERY", "WORDPRESS", "NODEJS_APP", "PHP_APP", "PYTHON_APP", "GO_APP", "MULTI_WEB") -> true
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





    fun setCustomPassword(password: String?) {
        assetDecryptor.setCustomPassword(password)

        synchronized(this) {
            configLoaded = false
            cachedConfig = null
        }
    }




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


    @SerializedName("adBlockEnabled")
    val adBlockEnabled: Boolean = false,

    @SerializedName("adBlockRules")
    val adBlockRules: List<String> = emptyList(),


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

    @SerializedName("announcementTriggerOnLaunch")
    val announcementTriggerOnLaunch: Boolean = true,

    @SerializedName("announcementTriggerOnNoNetwork")
    val announcementTriggerOnNoNetwork: Boolean = false,

    @SerializedName("announcementTriggerIntervalMinutes")
    val announcementTriggerIntervalMinutes: Int = 0,

    @SerializedName("adsEnabled")
    val adsEnabled: Boolean = false,

    @SerializedName("adBannerEnabled")
    val adBannerEnabled: Boolean = false,

    @SerializedName("adBannerId")
    val adBannerId: String = "",

    @SerializedName("adInterstitialEnabled")
    val adInterstitialEnabled: Boolean = false,

    @SerializedName("adInterstitialId")
    val adInterstitialId: String = "",

    @SerializedName("adSplashEnabled")
    val adSplashEnabled: Boolean = false,

    @SerializedName("adSplashId")
    val adSplashId: String = "",


    @SerializedName("webViewConfig")
    val webViewConfig: WebViewShellConfig = WebViewShellConfig(),


    @SerializedName("splashEnabled")
    val splashEnabled: Boolean = false,

    @SerializedName("splashType")
    val splashType: String = "IMAGE",

    @SerializedName("splashDuration")
    val splashDuration: Int = 3,

    @SerializedName("splashClickToSkip")
    val splashClickToSkip: Boolean = true,


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


    @SerializedName("appType")
    val appType: String = "WEB",

    @SerializedName("mediaConfig")
    val mediaConfig: MediaShellConfig = MediaShellConfig(),


    @SerializedName("htmlConfig")
    val htmlConfig: HtmlShellConfig = HtmlShellConfig(),


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


    @SerializedName("themeType")
    val themeType: String = "AURORA",

    @SerializedName("darkMode")
    val darkMode: String = "SYSTEM",


    @SerializedName("translateEnabled")
    val translateEnabled: Boolean = false,

    @SerializedName("translateTargetLanguage")
    val translateTargetLanguage: String = "zh-CN",

    @SerializedName("translateShowButton")
    val translateShowButton: Boolean = true,


    @SerializedName("extensionEnabled")
    val extensionEnabled: Boolean = false,

    @SerializedName("extensionFabIcon")
    val extensionFabIcon: String = "",

    @SerializedName("extensionModuleIds")
    val extensionModuleIds: List<String> = emptyList(),


    @SerializedName("embeddedExtensionModules")
    val embeddedExtensionModules: List<EmbeddedShellModule> = emptyList(),


    @SerializedName("autoStartConfig")
    val autoStartConfig: AutoStartShellConfig? = null,


    @SerializedName("forcedRunConfig")
    val forcedRunConfig: ForcedRunConfig? = null,


    @SerializedName("isolationEnabled")
    val isolationEnabled: Boolean = false,

    @SerializedName("isolationConfig")
    val isolationConfig: IsolationShellConfig? = null,


    @SerializedName("backgroundRunEnabled")
    val backgroundRunEnabled: Boolean = false,

    @SerializedName("backgroundRunConfig")
    val backgroundRunConfig: BackgroundRunShellConfig? = null,


    @SerializedName("notificationEnabled")
    val notificationEnabled: Boolean = false,

    @SerializedName("notificationConfig")
    val notificationConfig: NotificationShellConfig? = null,


    @SerializedName("blackTechConfig")
    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,


    @SerializedName("disguiseConfig")
    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null,


    @SerializedName("browserDisguiseConfig")
    val browserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null,


    @SerializedName("deviceDisguiseConfig")
    val deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null,


    @SerializedName("language")
    val language: String = "CHINESE",


    @SerializedName("galleryConfig")
    val galleryConfig: GalleryShellConfig = GalleryShellConfig(),


    @SerializedName("wordpressConfig")
    val wordpressConfig: WordPressShellConfig = WordPressShellConfig(),


    @SerializedName("nodejsConfig")
    val nodejsConfig: NodeJsShellConfig = NodeJsShellConfig(),


    @SerializedName("deepLinkEnabled")
    val deepLinkEnabled: Boolean = false,

    @SerializedName("deepLinkHosts")
    val deepLinkHosts: List<String> = emptyList(),


    @SerializedName("phpAppConfig")
    val phpAppConfig: PhpAppShellConfig = PhpAppShellConfig(),


    @SerializedName("pythonAppConfig")
    val pythonAppConfig: PythonAppShellConfig = PythonAppShellConfig(),


    @SerializedName("goAppConfig")
    val goAppConfig: GoAppShellConfig = GoAppShellConfig(),


    @SerializedName("multiWebConfig")
    val multiWebConfig: MultiWebShellConfig = MultiWebShellConfig()
)


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

    @SerializedName("versionName")
    val versionName: String = "1.0.0",

    @SerializedName("authorName")
    val authorName: String = "",

    @SerializedName("code")
    val code: String = "",

    @SerializedName("cssCode")
    val cssCode: String = "",

    @SerializedName("runAt")
    val runAt: String = "DOCUMENT_END",

    @SerializedName("sourceType")
    val sourceType: String = "CUSTOM",

    @SerializedName("runMode")
    val runMode: String = "INTERACTIVE",

    @SerializedName("uiConfig")
    val uiConfig: EmbeddedShellModuleUiConfig = EmbeddedShellModuleUiConfig(),

    @SerializedName("urlMatches")
    val urlMatches: List<EmbeddedUrlMatch> = emptyList(),

    @SerializedName("configValues")
    val configValues: Map<String, String> = emptyMap(),

    @SerializedName("configItemCount")
    val configItemCount: Int = 0,

    @SerializedName("gmGrants")
    val gmGrants: List<String> = emptyList(),

    @SerializedName("requireUrls")
    val requireUrls: List<String> = emptyList(),

    @SerializedName("requireContents")
    val requireContents: Map<String, String> = emptyMap(),

    @SerializedName("resources")
    val resources: Map<String, String> = emptyMap(),

    @SerializedName("noframes")
    val noframes: Boolean = false,

    @SerializedName("enabled")
    val enabled: Boolean = true
) {
    companion object {
        private val GSON = com.webtoapp.util.GsonProvider.gson

        private val regexCache = android.util.LruCache<String, Regex>(32)
    }

    fun matchesUrl(url: String): Boolean {
        if (urlMatches.isEmpty()) return true

        val includeRules = urlMatches.filter { !it.exclude }
        val excludeRules = urlMatches.filter { it.exclude }


        for (rule in excludeRules) {
            if (matchRule(url, rule)) return false
        }


        if (includeRules.isEmpty()) return true


        return includeRules.any { matchRule(url, it) }
    }

    private fun matchRule(url: String, rule: EmbeddedUrlMatch): Boolean {
        return try {
            val cacheKey = if (rule.isRegex) rule.pattern else "glob:${rule.pattern}"
            val regex = regexCache.get(cacheKey) ?: run {
                val r = if (rule.isRegex) {
                    Regex(rule.pattern)
                } else {

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

    fun isUserscript(): Boolean = sourceType == "USERSCRIPT"

    fun shouldRegisterInPanel(): Boolean {
        return !(isUserscript() && configItemCount == 0)
    }

    fun generateExecutableCode(): String {
        _cachedCode?.let { return it }
        val configJson = GSON.toJson(configValues)
        val uiConfigJson = GSON.toJson(
            mapOf(
                "type" to uiConfig.type,
                "autoHide" to uiConfig.autoHide,
                "autoHideDelay" to uiConfig.autoHideDelay,
                "initiallyHidden" to uiConfig.initiallyHidden,
                "showOnlyOnMatch" to uiConfig.showOnlyOnMatch
            )
        )
        return """
            (function() {
                'use strict';
                // Module配置
                const __MODULE_CONFIG__ = $configJson;
                const __MODULE_UI_CONFIG__ = $uiConfigJson;
                const __MODULE_RUN_MODE__ = '${runMode.escapeForJsSingleQuote()}';
                const __MODULE_INFO__ = {
                    id: '${id.escapeForJsSingleQuote()}',
                    name: '${name.escapeForJsSingleQuote()}',
                    icon: '${icon.escapeForJsSingleQuote()}',
                    version: '${versionName.escapeForJsSingleQuote()}',
                    uiConfig: __MODULE_UI_CONFIG__,
                    runMode: __MODULE_RUN_MODE__
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

                ${if (shouldRegisterInPanel()) """
                (function __autoRegister__() {
                    if (typeof __WTA_MODULE_UI__ === 'undefined') {
                        setTimeout(__autoRegister__, 100);
                        return;
                    }
                    var panel = window.__WTA_PANEL__;
                    if (!panel || !panel._initialized) {
                        setTimeout(__autoRegister__, 100);
                        return;
                    }
                    if (panel.modules) {
                        var existing = panel.modules.find(function(m) { return m.id === __MODULE_INFO__.id; });
                        if (existing && existing.uiConfig && existing.uiConfig.type) {
                            return;
                        }
                    }
                    __WTA_MODULE_UI__.register({
                        id: __MODULE_INFO__.id,
                        name: __MODULE_INFO__.name,
                        icon: __MODULE_INFO__.icon,
                        uiConfig: __MODULE_UI_CONFIG__,
                        runMode: __MODULE_RUN_MODE__
                    });
                })();
                """ else ""}
            })();
        """.trimIndent().also { _cachedCode = it }
    }
}

data class EmbeddedShellModuleUiConfig(
    @SerializedName("type")
    val type: String = "FLOATING_BUTTON",

    @SerializedName("autoHide")
    val autoHide: Boolean = false,

    @SerializedName("autoHideDelay")
    val autoHideDelay: Int = 3000,

    @SerializedName("initiallyHidden")
    val initiallyHidden: Boolean = false,

    @SerializedName("showOnlyOnMatch")
    val showOnlyOnMatch: Boolean = true
)

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
    val playMode: String = "SEQUENTIAL",

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
    val orientation: String = "PORTRAIT",

    @SerializedName("enableAudio")
    val enableAudio: Boolean = true,

    @SerializedName("videoAutoNext")
    val videoAutoNext: Boolean = true,

    @SerializedName("shuffleOnLoop")
    val shuffleOnLoop: Boolean = false,

    @SerializedName("defaultView")
    val defaultView: String = "GRID",

    @SerializedName("gridColumns")
    val gridColumns: Int = 3,

    @SerializedName("sortOrder")
    val sortOrder: String = "CUSTOM",

    @SerializedName("rememberPosition")
    val rememberPosition: Boolean = true
)

data class GalleryShellItem(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("assetPath")
    val assetPath: String = "",

    @SerializedName("type")
    val type: String = "IMAGE",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("duration")
    val duration: Long = 0,

    @SerializedName("thumbnailPath")
    val thumbnailPath: String? = null
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
    val keepScreenOn: Boolean = true
)

data class WordPressShellConfig(
    @SerializedName("siteTitle")
    val siteTitle: String = "My Site",

    @SerializedName("adminUser")
    val adminUser: String = "admin",

    @SerializedName("adminEmail")
    val adminEmail: String = "",

    @SerializedName("adminPassword")
    val adminPassword: String = "admin",

    @SerializedName("themeName")
    val themeName: String = "",

    @SerializedName("plugins")
    val plugins: List<String> = emptyList(),

    @SerializedName("activePlugins")
    val activePlugins: List<String> = emptyList(),

    @SerializedName("permalinkStructure")
    val permalinkStructure: String = "/%postname%/",

    @SerializedName("siteLanguage")
    val siteLanguage: String = "zh_CN",

    @SerializedName("autoInstall")
    val autoInstall: Boolean = true,

    @SerializedName("phpPort")
    val phpPort: Int = 0,

    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false
)

data class NodeJsShellConfig(
    @SerializedName("mode")
    val mode: String = "STATIC",

    @SerializedName("port")
    val port: Int = 0,

    @SerializedName("entryFile")
    val entryFile: String = "",

    @SerializedName("envVars")
    val envVars: Map<String, String> = emptyMap(),

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

    @SerializedName("targetArch")
    val targetArch: String = "arm64-v8a",

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
    val landscapeMode: Boolean = false,

    @SerializedName("projectId")
    val projectId: String = ""
)

data class MultiWebSiteShellConfig(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("url")
    val url: String = "",

    @SerializedName("type")
    val type: String = "URL",

    @SerializedName("localFilePath")
    val localFilePath: String = "",

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
) {
    fun getEffectiveUrl(localBaseUrl: String = ""): String {
        return if ((type == "LOCAL" || (type == "EXISTING" && localFilePath.isNotBlank())) && localFilePath.isNotBlank()) {
            val base = localBaseUrl.trimEnd('/')
            val path = localFilePath.trimStart('/')
            "$base/$path"
        } else {
            url
        }
    }
}

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
    val dohMode: String = "automatic",

    @SerializedName("bypassSystemDns")
    val bypassSystemDns: Boolean = false
)

data class HostMappingShellEntry(
    @SerializedName("host")
    val host: String = "",

    @SerializedName("ip")
    val ip: String = ""
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
    val hideBrowserToolbar: Boolean = false,

    @SerializedName("showStatusBarInFullscreen")
    val showStatusBarInFullscreen: Boolean = false,

    @SerializedName("showNavigationBarInFullscreen")
    val showNavigationBarInFullscreen: Boolean = false,

    @SerializedName("showToolbarInFullscreen")
    val showToolbarInFullscreen: Boolean = false,

    @SerializedName("landscapeMode")
    val landscapeMode: Boolean = false,

    @SerializedName("orientationMode")
    val orientationMode: String = "PORTRAIT",

    @SerializedName("injectScripts")
    val injectScripts: List<ShellUserScript> = emptyList(),

    @SerializedName("statusBarColorMode")
    val statusBarColorMode: String = "THEME",

    @SerializedName("statusBarColor")
    val statusBarColor: String? = null,

    @SerializedName("statusBarDarkIcons")
    val statusBarDarkIcons: Boolean? = null,

    @SerializedName("statusBarBackgroundType")
    val statusBarBackgroundType: String = "COLOR",

    @SerializedName("statusBarBackgroundImage")
    val statusBarBackgroundImage: String? = null,

    @SerializedName("statusBarBackgroundAlpha")
    val statusBarBackgroundAlpha: Float = 1.0f,

    @SerializedName("statusBarHeightDp")
    val statusBarHeightDp: Int = 0,


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
    val longPressMenuEnabled: Boolean = true,

    @SerializedName("longPressMenuStyle")
    val longPressMenuStyle: String = "FULL",

    @SerializedName("adBlockToggleEnabled")
    val adBlockToggleEnabled: Boolean = false,

    @SerializedName("popupBlockerEnabled")
    val popupBlockerEnabled: Boolean = false,

    @SerializedName("popupBlockerToggleEnabled")
    val popupBlockerToggleEnabled: Boolean = false,

    @SerializedName("openExternalLinks")
    val openExternalLinks: Boolean = false,


    @SerializedName("initialScale")
    val initialScale: Int = 0,

    @SerializedName("viewportMode")
    val viewportMode: String = "DEFAULT",

    @SerializedName("customViewportWidth")
    val customViewportWidth: Int = 0,

    @SerializedName("newWindowBehavior")
    val newWindowBehavior: String = "SAME_WINDOW",

    @SerializedName("enablePaymentSchemes")
    val enablePaymentSchemes: Boolean = true,

    @SerializedName("enableShareBridge")
    val enableShareBridge: Boolean = true,

    @SerializedName("enableZoomPolyfill")
    val enableZoomPolyfill: Boolean = true,


    @SerializedName("enableCrossOriginIsolation")
    val enableCrossOriginIsolation: Boolean = false,

    @SerializedName("hideUrlPreview")
    val hideUrlPreview: Boolean = false,

    @SerializedName("disableShields")
    val disableShields: Boolean = true,

    @SerializedName("decodeBase64DeepLinks")
    val decodeBase64DeepLinks: Boolean = false,

    @SerializedName("mediaAutoplayEnabled")
    val mediaAutoplayEnabled: Boolean = true,

    @SerializedName("acceptThirdPartyCookies")
    val acceptThirdPartyCookies: Boolean = true,

    @SerializedName("enableKernelDisguise")
    val enableKernelDisguise: Boolean = true,

    @SerializedName("enableImageRepair")
    val enableImageRepair: Boolean = true,

    @SerializedName("enableScrollMemory")
    val enableScrollMemory: Boolean = true,

    @SerializedName("enableHttpsUpgrade")
    val enableHttpsUpgrade: Boolean = true,

    @SerializedName("enableOAuthExternalRedirect")
    val enableOAuthExternalRedirect: Boolean = true,

    @SerializedName("enableClipboardPolyfill")
    val enableClipboardPolyfill: Boolean = true,

    @SerializedName("enableNotificationPolyfill")
    val enableNotificationPolyfill: Boolean = true,

    @SerializedName("safeBrowsingEnabled")
    val safeBrowsingEnabled: Boolean = true,

    @SerializedName("geolocationEnabled")
    val geolocationEnabled: Boolean = true,

    @SerializedName("enableOrientationPolyfill")
    val enableOrientationPolyfill: Boolean = true,

    @SerializedName("enableCompatPolyfills")
    val enableCompatPolyfills: Boolean = true,

    @SerializedName("enableNativeBridge")
    val enableNativeBridge: Boolean = true,

    @SerializedName("javaScriptCanOpenWindows")
    val javaScriptCanOpenWindows: Boolean = true,

    @SerializedName("databaseEnabled")
    val databaseEnabled: Boolean = true,

    @SerializedName("enableCookiePersistence")
    val enableCookiePersistence: Boolean = true,

    @SerializedName("enablePrivateNetworkBridge")
    val enablePrivateNetworkBridge: Boolean = true,

    @SerializedName("allowMixedContent")
    val allowMixedContent: Boolean = true,

    @SerializedName("enableGpc")
    val enableGpc: Boolean = true,

    @SerializedName("enableCookieConsentBlock")
    val enableCookieConsentBlock: Boolean = true,

    @SerializedName("enableReferrerPolicy")
    val enableReferrerPolicy: Boolean = true,

    @SerializedName("enableTrackerBlocking")
    val enableTrackerBlocking: Boolean = true,

    @SerializedName("enableBlobDownloadInterception")
    val enableBlobDownloadInterception: Boolean = true,

    @SerializedName("keepScreenOn")
    val keepScreenOn: Boolean = false,

    @SerializedName("screenAwakeMode")
    val screenAwakeMode: String = "OFF",

    @SerializedName("screenAwakeTimeoutMinutes")
    val screenAwakeTimeoutMinutes: Int = 30,

    @SerializedName("screenBrightness")
    val screenBrightness: Int = -1,


    @SerializedName("showFloatingBackButton")
    val showFloatingBackButton: Boolean = false,


    @SerializedName("keyboardAdjustMode")
    val keyboardAdjustMode: String = "RESIZE",


    @SerializedName("swipeRefreshEnabled")
    val swipeRefreshEnabled: Boolean = true,

    @SerializedName("fullscreenEnabled")
    val fullscreenEnabled: Boolean = true,


    @SerializedName("performanceOptimization")
    val performanceOptimization: Boolean = false,

    @SerializedName("pwaOfflineEnabled")
    val pwaOfflineEnabled: Boolean = false,

    @SerializedName("pwaOfflineStrategy")
    val pwaOfflineStrategy: String = "NETWORK_FIRST",


    @SerializedName("errorPageConfig")
    val errorPageConfig: ErrorPageShellConfig = ErrorPageShellConfig(),


    @SerializedName("floatingWindowConfig")
    val floatingWindowConfig: FloatingWindowShellConfig = FloatingWindowShellConfig(),


    @SerializedName("proxyMode")
    val proxyMode: String = "NONE",

    @SerializedName("proxyHost")
    val proxyHost: String = "",

    @SerializedName("proxyPort")
    val proxyPort: Int = 0,

    @SerializedName("proxyType")
    val proxyType: String = "HTTP",

    @SerializedName("pacUrl")
    val pacUrl: String = "",

    @SerializedName("proxyBypassRules")
    val proxyBypassRules: List<String> = emptyList(),

    @SerializedName("proxyUsername")
    val proxyUsername: String = "",

    @SerializedName("proxyPassword")
    val proxyPassword: String = "",

    @SerializedName("hostsMappingEnabled")
    val hostsMappingEnabled: Boolean = false,

    @SerializedName("hostsMappings")
    val hostsMappings: List<HostMappingShellEntry> = emptyList(),


    @SerializedName("dnsMode")
    val dnsMode: String = "SYSTEM",

    @SerializedName("dnsConfig")
    val dnsConfig: DnsShellConfig = DnsShellConfig()
)

data class FloatingWindowShellConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,

    @SerializedName("windowSizePercent")
    val windowSizePercent: Int = 80,

    @SerializedName("widthPercent")
    val widthPercent: Int = 80,

    @SerializedName("heightPercent")
    val heightPercent: Int = 80,

    @SerializedName("lockAspectRatio")
    val lockAspectRatio: Boolean = true,

    @SerializedName("opacity")
    val opacity: Int = 100,

    @SerializedName("cornerRadius")
    val cornerRadius: Int = 16,

    @SerializedName("borderStyle")
    val borderStyle: String = "SUBTLE",

    @SerializedName("showTitleBar")
    val showTitleBar: Boolean = true,

    @SerializedName("autoHideTitleBar")
    val autoHideTitleBar: Boolean = false,

    @SerializedName("startMinimized")
    val startMinimized: Boolean = false,

    @SerializedName("rememberPosition")
    val rememberPosition: Boolean = true,

    @SerializedName("edgeSnapping")
    val edgeSnapping: Boolean = true,

    @SerializedName("showResizeHandle")
    val showResizeHandle: Boolean = true,

    @SerializedName("lockPosition")
    val lockPosition: Boolean = false
)

data class ErrorPageShellConfig(
    @SerializedName("mode")
    val mode: String = "BUILTIN_STYLE",

    @SerializedName("builtInStyle")
    val builtInStyle: String = "MATERIAL",

    @SerializedName("showMiniGame")
    val showMiniGame: Boolean = false,

    @SerializedName("miniGameType")
    val miniGameType: String = "RANDOM",

    @SerializedName("autoRetrySeconds")
    val autoRetrySeconds: Int = 15,

    @SerializedName("customHtml")
    val customHtml: String = "",

    @SerializedName("customMediaPath")
    val customMediaPath: String = "",

    @SerializedName("retryButtonText")
    val retryButtonText: String = ""
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
    val type: String = "none",

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
