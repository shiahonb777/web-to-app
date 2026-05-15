package com.webtoapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.webtoapp.data.converter.Converters
import androidx.compose.runtime.Stable
import com.webtoapp.util.toFileSizeString

enum class AppType {
    WEB,
    IMAGE,
    VIDEO,
    HTML,
    GALLERY,
    FRONTEND,
    WORDPRESS,
    NODEJS_APP,
    PHP_APP,
    PYTHON_APP,
    GO_APP,
    MULTI_WEB
}

@Entity(
    tableName = "web_apps",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["categoryId"]),
        Index(value = ["isActivated"]),
        Index(value = ["appType", "url"]),
        Index(value = ["appType", "iconPath", "url"])
    ]
)
@TypeConverters(Converters::class)
@Stable
data class WebApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val iconPath: String? = null,
    val packageName: String? = null,
    val appType: AppType = AppType.WEB,
    val mediaConfig: MediaConfig? = null,
    val galleryConfig: GalleryConfig? = null,
    val htmlConfig: HtmlConfig? = null,
    val wordpressConfig: WordPressConfig? = null,
    val nodejsConfig: NodeJsConfig? = null,
    val phpAppConfig: PhpAppConfig? = null,
    val pythonAppConfig: PythonAppConfig? = null,
    val goAppConfig: GoAppConfig? = null,
    val multiWebConfig: MultiWebConfig? = null,


    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    val activationCodeList: List<com.webtoapp.core.activation.ActivationCode> = emptyList(),
    val activationRequireEveryTime: Boolean = false,
    val isActivated: Boolean = false,


    val adsEnabled: Boolean = false,
    val adConfig: AdConfig? = null,


    val announcementEnabled: Boolean = false,
    val announcement: Announcement? = null,


    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),


    val webViewConfig: WebViewConfig = WebViewConfig(),


    val splashEnabled: Boolean = false,
    val splashConfig: SplashConfig? = null,


    val bgmEnabled: Boolean = false,
    val bgmConfig: BgmConfig? = null,


    val apkExportConfig: ApkExportConfig? = null,


    val themeType: String = "AURORA",


    val translateEnabled: Boolean = false,
    val translateConfig: TranslateConfig? = null,


    val extensionEnabled: Boolean = false,
    val extensionModuleIds: List<String> = emptyList(),
    val extensionFabIcon: String? = null,

    val autoStartConfig: AutoStartConfig? = null,
    val forcedRunConfig: com.webtoapp.core.forcedrun.ForcedRunConfig? = null,
    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,
    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null,
    val browserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null,
    val deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null,
    val activationDialogConfig: ActivationDialogConfig? = null,
    val categoryId: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AdConfig(
    val bannerEnabled: Boolean = false,
    val bannerId: String = "",
    val interstitialEnabled: Boolean = false,
    val interstitialId: String = "",
    val splashEnabled: Boolean = false,
    val splashId: String = "",
    val splashDuration: Int = 3
)

enum class AnnouncementTemplateType {
    MINIMAL,
    XIAOHONGSHU,
    GRADIENT,
    GLASSMORPHISM,
    NEON,
    CUTE,
    ELEGANT,
    FESTIVE,
    DARK,
    NATURE
}

enum class AnnouncementTriggerMode {
    ON_LAUNCH,
    ON_INTERVAL,
    ON_NO_NETWORK
}

data class Announcement(
    val title: String = "",
    val content: String = "",
    val linkUrl: String? = null,
    val linkText: String? = null,
    val showOnce: Boolean = true,
    val enabled: Boolean = true,
    val version: Int = 1,
    val template: AnnouncementTemplateType = AnnouncementTemplateType.XIAOHONGSHU,
    val showEmoji: Boolean = true,
    val animationEnabled: Boolean = true,
    val requireConfirmation: Boolean = false,
    val allowNeverShow: Boolean = true,
    val triggerOnLaunch: Boolean = true,
    val triggerOnNoNetwork: Boolean = false,
    val triggerIntervalMinutes: Int = 0,
    val triggerIntervalIncludeLaunch: Boolean = false
)

enum class StatusBarColorMode {
    THEME,
    TRANSPARENT,
    CUSTOM
}

enum class StatusBarBackgroundType {
    COLOR,
    IMAGE
}


enum class LongPressMenuStyle {
    DISABLED,
    SIMPLE,
    FULL,
    IOS,
    FLOATING,
    CONTEXT
}


enum class UserAgentMode(
    val displayName: String,
    val description: String,
    val userAgentString: String?
) {
    DEFAULT(
        "System Default",
        "Use Android WebView default User-Agent",
        null
    ),
    CHROME_MOBILE(
        "Chrome Mobile",
        "Disguise as Chrome Android browser",
        "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + UserAgentVersions.CHROME + ".0.0.0 Mobile Safari/537.36"
    ),
    CHROME_DESKTOP(
        "Chrome Desktop",
        "Disguise as Chrome Windows browser",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + UserAgentVersions.CHROME + ".0.0.0 Safari/537.36"
    ),
    SAFARI_MOBILE(
        "Safari Mobile",
        "Disguise as Safari iOS browser",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/" + UserAgentVersions.SAFARI + ".0 Mobile/15E148 Safari/604.1"
    ),
    SAFARI_DESKTOP(
        "Safari Desktop",
        "Disguise as Safari macOS browser",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/" + UserAgentVersions.SAFARI + ".0 Safari/605.1.15"
    ),
    FIREFOX_MOBILE(
        "Firefox Mobile",
        "Disguise as Firefox Android browser",
        "Mozilla/5.0 (Android 15; Mobile; rv:" + UserAgentVersions.FIREFOX + ".0) Gecko/" + UserAgentVersions.FIREFOX + ".0 Firefox/" + UserAgentVersions.FIREFOX + ".0"
    ),
    FIREFOX_DESKTOP(
        "Firefox Desktop",
        "Disguise as Firefox Windows browser",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:" + UserAgentVersions.FIREFOX + ".0) Gecko/20100101 Firefox/" + UserAgentVersions.FIREFOX + ".0"
    ),
    EDGE_MOBILE(
        "Edge Mobile",
        "Disguise as Edge Android browser",
        "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + UserAgentVersions.CHROME + ".0.0.0 Mobile Safari/537.36 EdgA/" + UserAgentVersions.CHROME + ".0.0.0"
    ),
    EDGE_DESKTOP(
        "Edge Desktop",
        "Disguise as Edge Windows browser",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + UserAgentVersions.CHROME + ".0.0.0 Safari/537.36 Edg/" + UserAgentVersions.CHROME + ".0.0.0"
    ),
    CUSTOM(
        "Custom",
        "Use custom User-Agent string",
        null
    )
}


object UserAgentVersions {
    const val CHROME = "131"
    const val FIREFOX = "133"
    const val SAFARI = "18"
}

@Stable
data class WebViewConfig(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val allowFileAccess: Boolean = false,
    val allowContentAccess: Boolean = true,
    val cacheEnabled: Boolean = true,
    val userAgent: String? = null,
    val userAgentMode: UserAgentMode = UserAgentMode.DEFAULT,
    val customUserAgent: String? = null,
    val desktopMode: Boolean = false,
    val zoomEnabled: Boolean = true,
    val swipeRefreshEnabled: Boolean = true,
    val fullscreenEnabled: Boolean = true,
    val downloadEnabled: Boolean = true,
    val openExternalLinks: Boolean = false,
    val hideBrowserToolbar: Boolean = false,
    val hideToolbar: Boolean = false,
    val showStatusBarInFullscreen: Boolean = false,
    val showNavigationBarInFullscreen: Boolean = false,
    val showToolbarInFullscreen: Boolean = false,
    val landscapeMode: Boolean = false,
    val orientationMode: OrientationMode = OrientationMode.PORTRAIT,
    val injectScripts: List<UserScript> = emptyList(),
    val statusBarColorMode: StatusBarColorMode = StatusBarColorMode.THEME,
    val statusBarColor: String? = null,
    val statusBarDarkIcons: Boolean? = null,
    val statusBarBackgroundType: StatusBarBackgroundType = StatusBarBackgroundType.COLOR,
    val statusBarBackgroundImage: String? = null,
    val statusBarBackgroundAlpha: Float = 1.0f,
    val statusBarHeightDp: Int = 0,

    val statusBarColorModeDark: StatusBarColorMode = StatusBarColorMode.THEME,
    val statusBarColorDark: String? = null,
    val statusBarDarkIconsDark: Boolean = false,
    val statusBarBackgroundTypeDark: StatusBarBackgroundType = StatusBarBackgroundType.COLOR,
    val statusBarBackgroundImageDark: String? = null,
    val statusBarBackgroundAlphaDark: Float = 1.0f,
    val longPressMenuEnabled: Boolean = true,
    val longPressMenuStyle: LongPressMenuStyle = LongPressMenuStyle.FULL,
    val adBlockToggleEnabled: Boolean = false,
    val popupBlockerEnabled: Boolean = false,
    val popupBlockerToggleEnabled: Boolean = false,

    val initialScale: Int = 0,
    val viewportMode: ViewportMode = ViewportMode.DEFAULT,
    val customViewportWidth: Int = 0,
    val newWindowBehavior: NewWindowBehavior = NewWindowBehavior.SAME_WINDOW,
    val enablePaymentSchemes: Boolean = true,
    val enableShareBridge: Boolean = true,
    val enableZoomPolyfill: Boolean = true,
    val enableCrossOriginIsolation: Boolean = false,
    val hideUrlPreview: Boolean = false,
    val disableShields: Boolean = true,

    // 特殊设置
    val decodeBase64DeepLinks: Boolean = false,
    val mediaAutoplayEnabled: Boolean = true,
    val acceptThirdPartyCookies: Boolean = true,
    val enableKernelDisguise: Boolean = true,
    val enableImageRepair: Boolean = true,
    val enableScrollMemory: Boolean = true,
    val enableHttpsUpgrade: Boolean = true,
    val enableOAuthExternalRedirect: Boolean = true,
    val enableClipboardPolyfill: Boolean = true,
    val enableNotificationPolyfill: Boolean = true,
    val safeBrowsingEnabled: Boolean = true,
    val geolocationEnabled: Boolean = true,
    val enableOrientationPolyfill: Boolean = true,
    val enableCompatPolyfills: Boolean = true,
    val enableNativeBridge: Boolean = true,
    val javaScriptCanOpenWindows: Boolean = true,
    val databaseEnabled: Boolean = true,
    val enableCookiePersistence: Boolean = true,
    val enablePrivateNetworkBridge: Boolean = true,
    val allowMixedContent: Boolean = true,
    val enableGpc: Boolean = true,
    val enableCookieConsentBlock: Boolean = true,
    val enableReferrerPolicy: Boolean = true,
    val enableTrackerBlocking: Boolean = true,
    val enableBlobDownloadInterception: Boolean = true,
    val keepScreenOn: Boolean = false,
    val screenAwakeMode: ScreenAwakeMode = ScreenAwakeMode.OFF,
    val screenAwakeTimeoutMinutes: Int = 30,
    val screenBrightness: Int = -1,
    val keyboardAdjustMode: KeyboardAdjustMode = KeyboardAdjustMode.RESIZE,




    val allowFileAccessFromFileURLs: Boolean = false,
    val allowUniversalAccessFromFileURLs: Boolean = false,

    val errorPageConfig: com.webtoapp.core.errorpage.ErrorPageConfig = com.webtoapp.core.errorpage.ErrorPageConfig(),
    val performanceOptimization: Boolean = false,
    val pwaOfflineEnabled: Boolean = false,
    val pwaOfflineStrategy: String = "NETWORK_FIRST",
    val showFloatingBackButton: Boolean = false,
    val floatingWindowConfig: FloatingWindowConfig = FloatingWindowConfig(),
    val proxyMode: String = "NONE",
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val proxyType: String = "HTTP",
    val pacUrl: String = "",
    val proxyBypassRules: List<String> = emptyList(),
    val proxyUsername: String = "",
    val proxyPassword: String = "",
    val hostsMappingEnabled: Boolean = false,
    val hostsMappings: List<HostMappingEntry> = emptyList(),


    val dnsMode: String = "SYSTEM",
    val dnsConfig: DnsConfig = DnsConfig()
)

data class HostMappingEntry(
    val host: String = "",
    val ip: String = ""
)

data class FloatingWindowConfig(
    val enabled: Boolean = false,
    val windowSizePercent: Int = 80,
    val widthPercent: Int = 80,
    val heightPercent: Int = 80,
    val lockAspectRatio: Boolean = true,
    val opacity: Int = 100,
    val cornerRadius: Int = 16,
    val borderStyle: FloatingBorderStyle = FloatingBorderStyle.SUBTLE,
    val showTitleBar: Boolean = true,
    val autoHideTitleBar: Boolean = false,
    val startMinimized: Boolean = false,
    val rememberPosition: Boolean = true,
    val edgeSnapping: Boolean = true,
    val showResizeHandle: Boolean = true,
    val lockPosition: Boolean = false
)

enum class FloatingBorderStyle {
    NONE,
    SUBTLE,
    GLOW,
    ACCENT
}


enum class DnsProvider(val key: String, val dohUrl: String, val displayName: String) {
    @com.google.gson.annotations.SerializedName("cloudflare")
    CLOUDFLARE("cloudflare", "https://cloudflare-dns.com/dns-query", "Cloudflare"),
    @com.google.gson.annotations.SerializedName("google")
    GOOGLE("google", "https://dns.google/dns-query", "Google"),
    @com.google.gson.annotations.SerializedName("adguard")
    ADGUARD("adguard", "https://dns.adguard-dns.com/dns-query", "AdGuard"),
    @com.google.gson.annotations.SerializedName("nextdns")
    NEXTDNS("nextdns", "https://dns.nextdns.io/", "NextDNS"),
    @com.google.gson.annotations.SerializedName("cleanbrowsing")
    CLEANBROWSING("cleanbrowsing", "https://doh.cleanbrowsing.org/doh/family-filter/", "CleanBrowsing"),
    @com.google.gson.annotations.SerializedName("quad9")
    QUAD9("quad9", "https://dns.quad9.net/dns-query", "Quad9"),
    @com.google.gson.annotations.SerializedName("mullvad")
    MULLVAD("mullvad", "https://dns.mullvad.net/dns-query", "Mullvad"),
    @com.google.gson.annotations.SerializedName("custom")
    CUSTOM("custom", "", "Custom");

    companion object {
        fun fromKey(key: String): DnsProvider {
            return entries.find { it.key == key } ?: CLOUDFLARE
        }
    }
}


data class DnsConfig(

    val provider: String = "cloudflare",

    val customDohUrl: String = "",

    val dohMode: String = "automatic",

    val bypassSystemDns: Boolean = false
) {

    val effectiveDohUrl: String
        get() = when (provider) {
            "custom" -> customDohUrl
            else -> DnsProvider.entries.find { it.key == provider }?.dohUrl ?: ""
        }
}

data class UserScript(
    val name: String = "",
    val code: String = "",
    val enabled: Boolean = true,
    val runAt: ScriptRunTime = ScriptRunTime.DOCUMENT_END
)


enum class ScriptRunTime {
    DOCUMENT_START,
    DOCUMENT_END,
    DOCUMENT_IDLE
}


enum class NewWindowBehavior {
    SAME_WINDOW,
    EXTERNAL_BROWSER,
    POPUP_WINDOW,
    BLOCK
}

data class SplashConfig(
    val type: SplashType = SplashType.IMAGE,
    val mediaPath: String? = null,
    val duration: Int = 3,
    val clickToSkip: Boolean = true,
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT,
    val fillScreen: Boolean = true,
    val enableAudio: Boolean = false,
    val videoStartMs: Long = 0,
    val videoEndMs: Long = 5000,
    val videoDurationMs: Long = 0
)

enum class SplashType {
    IMAGE,
    VIDEO
}

enum class SplashOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class KeyboardAdjustMode {
    RESIZE,
    NOTHING
}

enum class OrientationMode {
    PORTRAIT,
    LANDSCAPE,
    REVERSE_PORTRAIT,
    REVERSE_LANDSCAPE,
    SENSOR_PORTRAIT,
    SENSOR_LANDSCAPE,
    AUTO
}

enum class ScreenAwakeMode {
    OFF,
    ALWAYS,
    TIMED
}

enum class ViewportMode {
    DEFAULT,
    FIT_SCREEN,
    DESKTOP,
    CUSTOM
}


data class MediaConfig(
    val mediaPath: String,
    val enableAudio: Boolean = true,
    val loop: Boolean = true,
    val autoPlay: Boolean = true,
    val fillScreen: Boolean = true,
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT,
    val backgroundColor: String = "#000000",
    val keepScreenOn: Boolean = true
)


@Stable
data class GalleryConfig(
    val items: List<GalleryItem> = emptyList(),
    val categories: List<GalleryCategory> = emptyList(),
    val playMode: GalleryPlayMode = GalleryPlayMode.SEQUENTIAL,
    val imageInterval: Int = 3,
    val loop: Boolean = true,
    val autoPlay: Boolean = false,
    val shuffleOnLoop: Boolean = false,
    val defaultView: GalleryViewMode = GalleryViewMode.GRID,
    val gridColumns: Int = 3,
    val sortOrder: GallerySortOrder = GallerySortOrder.CUSTOM,
    val backgroundColor: String = "#000000",
    val showThumbnailBar: Boolean = true,
    val showMediaInfo: Boolean = true,
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT,
    val enableAudio: Boolean = true,
    val videoAutoNext: Boolean = true,
    val rememberPosition: Boolean = false
) {
    fun getItemsByCategory(categoryId: String?): List<GalleryItem> {
        return if (categoryId == null) items
        else items.filter { it.categoryId == categoryId }
    }

    fun getSortedItems(categoryId: String? = null): List<GalleryItem> {
        val filtered = getItemsByCategory(categoryId)
        return when (sortOrder) {
            GallerySortOrder.CUSTOM -> filtered.sortedBy { it.sortIndex }
            GallerySortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            GallerySortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            GallerySortOrder.DATE_ASC -> filtered.sortedBy { it.createdAt }
            GallerySortOrder.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
            GallerySortOrder.TYPE -> filtered.sortedBy { it.type.ordinal }
        }
    }

    val imageCount: Int get() = items.count { it.type == GalleryItemType.IMAGE }
    val videoCount: Int get() = items.count { it.type == GalleryItemType.VIDEO }
    val totalCount: Int get() = items.size
}

data class GalleryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val path: String,
    val type: GalleryItemType,
    val name: String = "",
    val categoryId: String? = null,
    val duration: Long = 0,
    val thumbnailPath: String? = null,
    val sortIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val fileSize: Long = 0
) {
    val formattedDuration: String
        get() {
            if (type != GalleryItemType.VIDEO || duration <= 0) return ""
            val seconds = (duration / 1000) % 60
            val minutes = (duration / 1000 / 60) % 60
            val hours = duration / 1000 / 60 / 60
            return if (hours > 0) {
                String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
            }
        }

    val formattedFileSize: String
        get() = if (fileSize <= 0) "" else fileSize.toFileSizeString()
}

data class GalleryCategory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "folder",
    val color: String = "#6200EE",
    val sortIndex: Int = 0
)

enum class GalleryItemType {
    IMAGE,
    VIDEO
}

enum class GalleryPlayMode {
    SEQUENTIAL,
    SHUFFLE,
    SINGLE_LOOP
}

enum class GalleryViewMode {
    GRID,
    LIST,
    TIMELINE
}

enum class GallerySortOrder {
    CUSTOM,
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    TYPE
}

enum class NodeJsBuildMode {
    STATIC,
    SSR,
    API_BACKEND,
    FULLSTACK
}


data class NodeJsConfig(
    val projectId: String = "",
    val projectName: String = "",
    val sourceProjectPath: String = "",
    val framework: String = "",
    val buildMode: NodeJsBuildMode = NodeJsBuildMode.API_BACKEND,
    val entryFile: String = "index.js",
    val serverPort: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val hasNodeModules: Boolean = false,
    val nodeVersion: String = "",
    val landscapeMode: Boolean = false
)

data class WordPressConfig(
    val projectId: String = "",
    val projectName: String = "",
    val siteTitle: String = "My Site",
    val adminUser: String = "admin",
    val adminEmail: String = "",
    val adminPassword: String = "admin",
    val themeName: String = "",
    val plugins: List<String> = emptyList(),
    val activePlugins: List<String> = emptyList(),
    val permalinkStructure: String = "/%postname%/",
    val siteLanguage: String = "zh_CN",
    val autoInstall: Boolean = true,
    val sourceType: String = "BLANK",
    val sourceProjectId: String = "",
    val phpPort: Int = 0,
    val landscapeMode: Boolean = false
)

data class PhpAppConfig(
    val projectId: String = "",
    val projectName: String = "",
    val framework: String = "",
    val documentRoot: String = "",
    val entryFile: String = "index.php",
    val phpPort: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val hasComposerJson: Boolean = false,
    val landscapeMode: Boolean = false
)

data class PythonAppConfig(
    val projectId: String = "",
    val projectName: String = "",
    val framework: String = "",
    val entryFile: String = "app.py",
    val entryModule: String = "",
    val serverType: String = "builtin",
    val serverPort: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val pythonVersion: String = "",
    val requirementsFile: String = "requirements.txt",
    val hasPipDeps: Boolean = false,
    val landscapeMode: Boolean = false
)

data class GoAppConfig(
    val projectId: String = "",
    val projectName: String = "",
    val framework: String = "",
    val binaryName: String = "",
    val targetArch: String = "arm64",
    val serverPort: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val staticDir: String = "",
    val landscapeMode: Boolean = false
)

data class MultiWebConfig(
    val sites: List<MultiWebSite> = emptyList(),
    val displayMode: String = "TABS",
    val refreshInterval: Int = 30,
    val showSiteIcons: Boolean = true,
    val landscapeMode: Boolean = false,
    val projectId: String = ""
)

data class MultiWebSite(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val type: String = "URL",
    val localFilePath: String = "",
    @Transient val localFileUri: String = "",
    val sourceAppId: Long = 0,
    val sourceProjectId: String = "",
    val iconEmoji: String = "",
    val faviconUrl: String = "",
    val themeColor: String = "",
    val category: String = "",
    val cssSelector: String = "",
    val linkSelector: String = "",
    val enabled: Boolean = true,
    val sortIndex: Int = 0
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

data class HtmlConfig(
    val projectId: String = "",
    val projectDir: String? = null,
    val entryFile: String = "index.html",
    val files: List<HtmlFile> = emptyList(),
    val enableJavaScript: Boolean = true,
    val enableLocalStorage: Boolean = true,
    val allowFileAccess: Boolean = true,
    val backgroundColor: String = "#FFFFFF",
    val landscapeMode: Boolean = false
) {
    fun getValidEntryFile(): String {
        return entryFile.takeIf {
            it.isNotBlank() && it.substringBeforeLast(".").isNotBlank()
        } ?: "index.html"
    }
}

data class HtmlFile(
    val name: String,
    val path: String,
    val type: HtmlFileType = HtmlFileType.OTHER
)

enum class HtmlFileType {
    HTML,
    CSS,
    JS,
    IMAGE,
    FONT,
    OTHER
}

enum class BgmPlayMode {
    LOOP,
    SEQUENTIAL,
    SHUFFLE
}

enum class BgmTag {
    PURE_MUSIC,
    POP,
    ROCK,
    CLASSICAL,
    JAZZ,
    ELECTRONIC,
    FOLK,
    CHINESE_STYLE,
    ANIME,
    GAME,
    MOVIE,
    HEALING,
    EXCITING,
    SAD,
    ROMANTIC,
    RELAXING,
    WORKOUT,
    SLEEP,
    STUDY,
    OTHER;

    val displayName: String get() = when (this) {
        PURE_MUSIC -> com.webtoapp.core.i18n.Strings.bgmTagPureMusic
        POP -> com.webtoapp.core.i18n.Strings.bgmTagPop
        ROCK -> com.webtoapp.core.i18n.Strings.bgmTagRock
        CLASSICAL -> com.webtoapp.core.i18n.Strings.bgmTagClassical
        JAZZ -> com.webtoapp.core.i18n.Strings.bgmTagJazz
        ELECTRONIC -> com.webtoapp.core.i18n.Strings.bgmTagElectronic
        FOLK -> com.webtoapp.core.i18n.Strings.bgmTagFolk
        CHINESE_STYLE -> com.webtoapp.core.i18n.Strings.bgmTagChineseStyle
        ANIME -> com.webtoapp.core.i18n.Strings.bgmTagAnime
        GAME -> com.webtoapp.core.i18n.Strings.bgmTagGame
        MOVIE -> com.webtoapp.core.i18n.Strings.bgmTagMovie
        HEALING -> com.webtoapp.core.i18n.Strings.bgmTagHealing
        EXCITING -> com.webtoapp.core.i18n.Strings.bgmTagExciting
        SAD -> com.webtoapp.core.i18n.Strings.bgmTagSad
        ROMANTIC -> com.webtoapp.core.i18n.Strings.bgmTagRomantic
        RELAXING -> com.webtoapp.core.i18n.Strings.bgmTagRelaxing
        WORKOUT -> com.webtoapp.core.i18n.Strings.bgmTagWorkout
        SLEEP -> com.webtoapp.core.i18n.Strings.bgmTagSleep
        STUDY -> com.webtoapp.core.i18n.Strings.bgmTagStudy
        OTHER -> com.webtoapp.core.i18n.Strings.bgmTagOther
    }
}

data class LrcLine(
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val translation: String? = null
)

data class LrcData(
    val lines: List<LrcLine> = emptyList(),
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val language: String? = null
)

data class LrcTheme(
    val id: String,
    val name: String,
    val fontFamily: String = "default",
    val fontSize: Float = 18f,
    val textColor: String = "#FFFFFF",
    val highlightColor: String = "#FFD700",
    val backgroundColor: String = "#80000000",
    val strokeColor: String? = null,
    val strokeWidth: Float = 0f,
    val shadowEnabled: Boolean = true,
    val animationType: LrcAnimationType = LrcAnimationType.FADE,
    val position: LrcPosition = LrcPosition.BOTTOM,
    val showTranslation: Boolean = true
)

enum class LrcAnimationType {
    NONE, FADE, SLIDE_UP, SLIDE_LEFT, SCALE, TYPEWRITER, KARAOKE;

    val displayName: String get() = when (this) {
        NONE -> com.webtoapp.core.i18n.Strings.lrcAnimNone
        FADE -> com.webtoapp.core.i18n.Strings.lrcAnimFade
        SLIDE_UP -> com.webtoapp.core.i18n.Strings.lrcAnimSlideUp
        SLIDE_LEFT -> com.webtoapp.core.i18n.Strings.lrcAnimSlideLeft
        SCALE -> com.webtoapp.core.i18n.Strings.lrcAnimScale
        TYPEWRITER -> com.webtoapp.core.i18n.Strings.lrcAnimTypewriter
        KARAOKE -> com.webtoapp.core.i18n.Strings.lrcAnimKaraoke
    }
}

enum class LrcPosition {
    TOP, CENTER, BOTTOM;

    val displayName: String get() = when (this) {
        TOP -> com.webtoapp.core.i18n.Strings.lrcPosTop
        CENTER -> com.webtoapp.core.i18n.Strings.lrcPosCenter
        BOTTOM -> com.webtoapp.core.i18n.Strings.lrcPosBottom
    }
}

data class BgmItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val coverPath: String? = null,
    val isAsset: Boolean = false,
    val tags: List<BgmTag> = emptyList(),
    val sortOrder: Int = 0,
    val lrcData: LrcData? = null,
    val lrcPath: String? = null,
    val duration: Long = 0
)

data class BgmConfig(
    val playlist: List<BgmItem> = emptyList(),
    val playMode: BgmPlayMode = BgmPlayMode.LOOP,
    val volume: Float = 0.5f,
    val autoPlay: Boolean = true,
    val showLyrics: Boolean = true,
    val lrcTheme: LrcTheme? = null
)

enum class ApkArchitecture(
    val abiFilters: List<String>
) {
    UNIVERSAL(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")),
    ARM64(listOf("arm64-v8a", "x86_64")),
    ARM32(listOf("armeabi-v7a", "x86"));

    val displayName: String get() = when (this) {
        UNIVERSAL -> com.webtoapp.core.i18n.Strings.archUniversal
        ARM64 -> com.webtoapp.core.i18n.Strings.archArm64
        ARM32 -> com.webtoapp.core.i18n.Strings.archArm32
    }

    val description: String get() = when (this) {
        UNIVERSAL -> com.webtoapp.core.i18n.Strings.archUniversalDesc
        ARM64 -> com.webtoapp.core.i18n.Strings.archArm64Desc
        ARM32 -> com.webtoapp.core.i18n.Strings.archArm32Desc
    }

    companion object {
        fun fromName(name: String): ApkArchitecture {
            return entries.find { it.name == name } ?: UNIVERSAL
        }
    }
}

data class ApkExportConfig(
    val customPackageName: String? = null,
    val customVersionName: String? = null,
    val customVersionCode: Int? = null,
    val architecture: ApkArchitecture = ApkArchitecture.UNIVERSAL,
    val runtimePermissions: ApkRuntimePermissions = ApkRuntimePermissions(),
    val networkTrustConfig: NetworkTrustConfig = NetworkTrustConfig(),
    val encryptionConfig: ApkEncryptionConfig = ApkEncryptionConfig(),
    val hardeningConfig: AppHardeningConfig = AppHardeningConfig(),
    val isolationConfig: com.webtoapp.core.isolation.IsolationConfig = com.webtoapp.core.isolation.IsolationConfig(),
    val backgroundRunEnabled: Boolean = false,
    val backgroundRunConfig: BackgroundRunExportConfig = BackgroundRunExportConfig(),
    val engineType: String = "SYSTEM_WEBVIEW",
    val deepLinkEnabled: Boolean = false,
    val customDeepLinkHosts: List<String> = emptyList(),
    val performanceOptimization: Boolean = false,
    val performanceConfig: PerformanceOptimizationConfig = PerformanceOptimizationConfig(),
    val notificationEnabled: Boolean = false,
    val notificationConfig: NotificationExportConfig = NotificationExportConfig()
)

data class NetworkTrustConfig(
    val trustSystemCa: Boolean = true,
    val trustUserCa: Boolean = true,
    val customCaCertificates: List<CustomCaCertificate> = emptyList(),
    val cleartextTrafficPermitted: Boolean = true
)

data class CustomCaCertificate(
    val id: String,
    val displayName: String,
    val filePath: String,
    val sha256: String,
    val addedAt: Long = System.currentTimeMillis()
)

data class ApkRuntimePermissions(

    val camera: Boolean = false,
    val microphone: Boolean = false,
    val location: Boolean = false,
    val notifications: Boolean = false,


    val readExternalStorage: Boolean = false,
    val writeExternalStorage: Boolean = false,
    val readMediaImages: Boolean = false,
    val readMediaVideo: Boolean = false,
    val readMediaAudio: Boolean = false,


    val bluetooth: Boolean = false,
    val nfc: Boolean = false,
    val wifiState: Boolean = false,


    val bodySensors: Boolean = false,
    val activityRecognition: Boolean = false,


    val readPhoneState: Boolean = false,
    val callPhone: Boolean = false,
    val readContacts: Boolean = false,
    val writeContacts: Boolean = false,
    val readCalendar: Boolean = false,
    val writeCalendar: Boolean = false,
    val readSms: Boolean = false,
    val sendSms: Boolean = false,
    val receiveSms: Boolean = false,
    val readCallLog: Boolean = false,
    val writeCallLog: Boolean = false,
    val processOutgoingCalls: Boolean = false,


    val foregroundService: Boolean = false,
    val wakeLock: Boolean = false,
    val requestIgnoreBatteryOptimizations: Boolean = false,
    val bootCompleted: Boolean = false,
    val vibration: Boolean = false,
    val installPackages: Boolean = false,
    val requestDeletePackages: Boolean = false,
    val systemAlertWindow: Boolean = false
)

fun ApkExportConfig?.isMeaningful(): Boolean {
    if (this == null) return false

    val defaultConfig = ApkExportConfig()
    return this != defaultConfig
}

data class PerformanceOptimizationConfig(
    val compressImages: Boolean = true,
    val imageQuality: Int = 80,
    val convertToWebP: Boolean = true,
    val minifyCode: Boolean = true,
    val minifySvg: Boolean = true,
    val removeUnusedResources: Boolean = true,
    val parallelProcessing: Boolean = true,
    val enableCache: Boolean = true,
    val injectPreloadHints: Boolean = true,
    val injectLazyLoading: Boolean = true,
    val optimizeScripts: Boolean = true,
    val injectDnsPrefetch: Boolean = true,
    val injectPerformanceScript: Boolean = true
) {
    fun toOptimizerConfig(): com.webtoapp.core.linux.PerformanceOptimizer.OptimizeConfig {
        return com.webtoapp.core.linux.PerformanceOptimizer.OptimizeConfig(
            compressImages = compressImages,
            imageQuality = imageQuality,
            convertToWebP = convertToWebP,
            minifyCode = minifyCode,
            minifySvg = minifySvg,
            removeUnusedResources = removeUnusedResources,
            parallelProcessing = parallelProcessing,
            enableCache = enableCache,
            injectPreloadHints = injectPreloadHints,
            injectLazyLoading = injectLazyLoading,
            optimizeScripts = optimizeScripts,
            injectDnsPrefetch = injectDnsPrefetch,
            injectPerformanceScript = injectPerformanceScript
        )
    }
}

data class BackgroundRunExportConfig(
    val notificationTitle: String = "",
    val notificationContent: String = "",
    val showNotification: Boolean = true,
    val keepCpuAwake: Boolean = true
)

data class ApkEncryptionConfig(
    val enabled: Boolean = false,
    val customPassword: String? = null
) {
    companion object {
        val DISABLED = ApkEncryptionConfig(enabled = false)
    }

    fun toEncryptionConfig(): com.webtoapp.core.crypto.EncryptionConfig {
        return if (enabled) com.webtoapp.core.crypto.EncryptionConfig.MAXIMUM.copy(customPassword = customPassword)
        else com.webtoapp.core.crypto.EncryptionConfig.DISABLED
    }
}

    enum class TranslateLanguage(val code: String, val displayName: String) {
    CHINESE("zh-CN", "中文（简体）"),
    CHINESE_TW("zh-TW", "中文（繁體）"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    RUSSIAN("ru", "Русский"),
    ARABIC("ar", "العربية"),
    HINDI("hi", "हिन्दी"),
    THAI("th", "ไทย"),
    VIETNAMESE("vi", "Tiếng Việt"),
    INDONESIAN("id", "Bahasa Indonesia"),
    MALAY("ms", "Bahasa Melayu"),
    TURKISH("tr", "Türkçe"),
    ITALIAN("it", "Italiano"),
    DUTCH("nl", "Nederlands"),
    POLISH("pl", "Polski")
}

enum class TranslateEngine(val displayName: String) {
    AUTO("自动选择"),
    GOOGLE("Google Translate"),
    MYMEMORY("MyMemory"),
    LIBRE("LibreTranslate"),
    LINGVA("Lingva Translate")
}

data class TranslateConfig(
    val targetLanguage: TranslateLanguage = TranslateLanguage.CHINESE,
    val showFloatingButton: Boolean = true,
    val preferredEngine: TranslateEngine = TranslateEngine.AUTO,
    val autoTranslateOnLoad: Boolean = true
)

fun WebApp.getAllActivationCodes(): List<com.webtoapp.core.activation.ActivationCode> {
    return activationCodeList
}

fun WebApp.getActivationCodeStrings(): List<String> {
    return activationCodeList.map { it.toJson() }
}

data class ActivationDialogConfig(
    val title: String = "",
    val subtitle: String = "",
    val inputLabel: String = "",
    val buttonText: String = ""
)

data class AppHardeningConfig(
    val enabled: Boolean = false
) {
    enum class ThreatResponse {
        LOG_ONLY,
        SILENT_EXIT,
        CRASH_RANDOM,
        DATA_WIPE,
        FAKE_DATA;

        val displayName: String get() = when (this) {
            LOG_ONLY -> com.webtoapp.core.i18n.Strings.threatResponseLogOnly
            SILENT_EXIT -> com.webtoapp.core.i18n.Strings.threatResponseSilentExit
            CRASH_RANDOM -> com.webtoapp.core.i18n.Strings.threatResponseCrashRandom
            DATA_WIPE -> com.webtoapp.core.i18n.Strings.threatResponseDataWipe
            FAKE_DATA -> com.webtoapp.core.i18n.Strings.threatResponseFakeData
        }
    }

    companion object {
        val DISABLED = AppHardeningConfig(enabled = false)
        val ENABLED = AppHardeningConfig(enabled = true)
    }
}

data class AutoStartConfig(
    val bootStartEnabled: Boolean = false,
    val scheduledStartEnabled: Boolean = false,
    val scheduledTime: String = "08:00",
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val scheduledRepeat: Boolean = true,
    val bootDelay: Long = 5000L
)




fun WebApp.toManifestJson(): String {
    return com.webtoapp.data.converter.Converters.gson.toJson(this)
}

object ManifestUtils {

    fun fromManifestJson(json: String, overrideId: Long? = null): WebApp? {
        return try {
            val parsed = com.google.gson.JsonParser.parseString(json)
            val defaultWebApp = WebApp(name = "", url = "")
            val defaultJson = com.webtoapp.data.converter.Converters.gson.toJsonTree(defaultWebApp)
            val merged = com.webtoapp.data.converter.Converters.mergeMissingDefaults(defaultJson, parsed)
            val restored = com.webtoapp.data.converter.Converters.gson.fromJson(merged, WebApp::class.java)
            if (overrideId != null) {
                restored?.copy(id = overrideId, updatedAt = System.currentTimeMillis())
            } else {
                restored
            }
        } catch (e: Exception) {
            null
        }
    }
}

enum class NotificationType(val key: String) {
    @com.google.gson.annotations.SerializedName("none")
    NONE("none"),
    @com.google.gson.annotations.SerializedName("web_api")
    WEB_API("web_api"),
    @com.google.gson.annotations.SerializedName("polling")
    POLLING("polling")
}

data class NotificationExportConfig(
    val type: NotificationType = NotificationType.NONE,

    val pollUrl: String = "",

    val pollIntervalMinutes: Int = 15,

    val pollMethod: String = "GET",

    val pollHeaders: String = "",

    val clickUrl: String = ""
)
