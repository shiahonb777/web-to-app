package com.webtoapp.core.apkbuilder

import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.shell.BgmShellItem
import com.webtoapp.core.shell.LrcShellTheme

data class ApkConfig(
    val meta: MetaBlock,
    val activation: ActivationBlock = ActivationBlock(),
    val adBlock: AdBlockBlock = AdBlockBlock(),
    val announcement: AnnouncementBlock = AnnouncementBlock(),
    val ads: AdsBlock = AdsBlock(),
    val webView: WebViewBlock = WebViewBlock(),
    val webViewBehavior: WebViewBehaviorBlock = WebViewBehaviorBlock(),
    val screenAwake: ScreenAwakeBlock = ScreenAwakeBlock(),
    val statusBar: StatusBarBlock = StatusBarBlock(),
    val floatingWindow: FloatingWindowBlock = FloatingWindowBlock(),
    val proxy: ProxyBlock = ProxyBlock(),
    val dns: DnsBlock = DnsBlock(),
    val errorPage: ErrorPageBlock = ErrorPageBlock(),
    val splash: SplashBlock = SplashBlock(),
    val media: MediaBlock = MediaBlock(),
    val html: HtmlBlock = HtmlBlock(),
    val gallery: GalleryBlock = GalleryBlock(),
    val bgm: BgmBlock = BgmBlock(),
    val translate: TranslateBlock = TranslateBlock(),
    val extension: ExtensionBlock = ExtensionBlock(),
    val autoStart: AutoStartBlock = AutoStartBlock(),
    val optionalServices: OptionalServicesBlock = OptionalServicesBlock(),
    val disguise: DisguiseBlock = DisguiseBlock(),
    val deepLink: DeepLinkBlock = DeepLinkBlock(),
    val wordpress: WordpressBlock = WordpressBlock(),
    val nodejs: NodejsBlock = NodejsBlock(),
    val phpApp: PhpAppBlock = PhpAppBlock(),
    val pythonApp: PythonAppBlock = PythonAppBlock(),
    val goApp: GoAppBlock = GoAppBlock(),
    val multiWeb: MultiWebBlock = MultiWebBlock()
) {

    val appName: String get() = meta.appName
    val packageName: String get() = meta.packageName
    val targetUrl: String get() = meta.targetUrl
    val htmlUsesFileScheme: Boolean get() = meta.htmlUsesFileScheme
    val versionCode: Int get() = meta.versionCode
    val versionName: String get() = meta.versionName
    val iconPath: String? get() = meta.iconPath
    val runtimePermissions: com.webtoapp.data.model.ApkRuntimePermissions get() = meta.runtimePermissions
    val networkTrustConfig: com.webtoapp.data.model.NetworkTrustConfig get() = meta.networkTrustConfig
    val appType: String get() = meta.appType
    val themeType: String get() = meta.themeType
    val darkMode: String get() = meta.darkMode
    val language: String get() = meta.language
    val engineType: String get() = meta.engineType

    val activationEnabled: Boolean get() = activation.enabled
    val activationCodes: List<String> get() = activation.codes
    val activationRequireEveryTime: Boolean get() = activation.requireEveryTime
    val activationDialogTitle: String get() = activation.dialogTitle
    val activationDialogSubtitle: String get() = activation.dialogSubtitle
    val activationDialogInputLabel: String get() = activation.dialogInputLabel
    val activationDialogButtonText: String get() = activation.dialogButtonText
    val activationRemoteEnabled: Boolean get() = activation.remoteEnabled
    val activationRemoteVerifyUrl: String get() = activation.remoteVerifyUrl
    val activationRemotePublicKey: String get() = activation.remotePublicKey
    val activationRemoteOfflinePolicy: String get() = activation.remoteOfflinePolicy

    val adBlockEnabled: Boolean get() = adBlock.enabled
    val adBlockRules: List<String> get() = adBlock.rules

    val announcementEnabled: Boolean get() = announcement.enabled
    val announcementTitle: String get() = announcement.title
    val announcementContent: String get() = announcement.content
    val announcementContentIsHtml: Boolean get() = announcement.contentIsHtml
    val announcementLink: String get() = announcement.link
    val announcementLinkText: String get() = announcement.linkText
    val announcementTemplate: String get() = announcement.template
    val announcementShowEmoji: Boolean get() = announcement.showEmoji
    val announcementAnimationEnabled: Boolean get() = announcement.animationEnabled
    val announcementShowOnce: Boolean get() = announcement.showOnce
    val announcementRequireConfirmation: Boolean get() = announcement.requireConfirmation
    val announcementAllowNeverShow: Boolean get() = announcement.allowNeverShow
    val announcementTriggerOnLaunch: Boolean get() = announcement.triggerOnLaunch
    val announcementTriggerOnNoNetwork: Boolean get() = announcement.triggerOnNoNetwork
    val announcementTriggerIntervalMinutes: Int get() = announcement.triggerIntervalMinutes

    val adsEnabled: Boolean get() = ads.enabled
    val adBannerEnabled: Boolean get() = ads.bannerEnabled
    val adBannerId: String get() = ads.bannerId
    val adInterstitialEnabled: Boolean get() = ads.interstitialEnabled
    val adInterstitialId: String get() = ads.interstitialId
    val adSplashEnabled: Boolean get() = ads.splashEnabled
    val adSplashId: String get() = ads.splashId

    val javaScriptEnabled: Boolean get() = webView.javaScriptEnabled
    val domStorageEnabled: Boolean get() = webView.domStorageEnabled
    val allowFileAccess: Boolean get() = webView.allowFileAccess
    val allowContentAccess: Boolean get() = webView.allowContentAccess
    val cacheEnabled: Boolean get() = webView.cacheEnabled
    val clearBrowsingDataOnLaunch: Boolean get() = webView.clearBrowsingDataOnLaunch
    val zoomEnabled: Boolean get() = webView.zoomEnabled
    val desktopMode: Boolean get() = webView.desktopMode
    val userAgent: String? get() = webView.userAgent
    val userAgentMode: String get() = webView.userAgentMode
    val customUserAgent: String? get() = webView.customUserAgent
    val hideToolbar: Boolean get() = webView.hideToolbar
    val hideBrowserToolbar: Boolean get() = webView.hideBrowserToolbar
    val toolbarShowTitle: Boolean get() = webView.toolbarShowTitle
    val toolbarShowUrl: Boolean get() = webView.toolbarShowUrl
    val toolbarShowBack: Boolean get() = webView.toolbarShowBack
    val toolbarShowForward: Boolean get() = webView.toolbarShowForward
    val toolbarShowRefresh: Boolean get() = webView.toolbarShowRefresh
    val showStatusBarInFullscreen: Boolean get() = webView.showStatusBarInFullscreen
    val showNavigationBarInFullscreen: Boolean get() = webView.showNavigationBarInFullscreen
    val showToolbarInFullscreen: Boolean get() = webView.showToolbarInFullscreen
    val landscapeMode: Boolean get() = webView.landscapeMode
    val orientationMode: String get() = webView.orientationMode
    val injectScripts: List<com.webtoapp.data.model.UserScript> get() = webView.injectScripts
    val longPressMenuEnabled: Boolean get() = webView.longPressMenuEnabled
    val longPressMenuStyle: String get() = webView.longPressMenuStyle
    val adBlockToggleEnabled: Boolean get() = webView.adBlockToggleEnabled
    val popupBlockerEnabled: Boolean get() = webView.popupBlockerEnabled
    val popupBlockerToggleEnabled: Boolean get() = webView.popupBlockerToggleEnabled
    val openExternalLinks: Boolean get() = webView.openExternalLinks
    val showFloatingBackButton: Boolean get() = webView.showFloatingBackButton
    val swipeRefreshEnabled: Boolean get() = webView.swipeRefreshEnabled
    val fullscreenEnabled: Boolean get() = webView.fullscreenEnabled
    val performanceOptimization: Boolean get() = webView.performanceOptimization
    val pwaOfflineEnabled: Boolean get() = webView.pwaOfflineEnabled
    val pwaOfflineStrategy: String get() = webView.pwaOfflineStrategy
    val keyboardAdjustMode: String get() = webView.keyboardAdjustMode

    val initialScale: Int get() = webViewBehavior.initialScale
    val viewportMode: String get() = webViewBehavior.viewportMode
    val customViewportWidth: Int get() = webViewBehavior.customViewportWidth
    val newWindowBehavior: String get() = webViewBehavior.newWindowBehavior
    val enablePaymentSchemes: Boolean get() = webViewBehavior.enablePaymentSchemes
    val enableShareBridge: Boolean get() = webViewBehavior.enableShareBridge
    val enableZoomPolyfill: Boolean get() = webViewBehavior.enableZoomPolyfill
    val enableCrossOriginIsolation: Boolean get() = webViewBehavior.enableCrossOriginIsolation
    val hideUrlPreview: Boolean get() = webViewBehavior.hideUrlPreview
    val decodeBase64DeepLinks: Boolean get() = webViewBehavior.decodeBase64DeepLinks
    val mediaAutoplayEnabled: Boolean get() = webViewBehavior.mediaAutoplayEnabled
    val acceptThirdPartyCookies: Boolean get() = webViewBehavior.acceptThirdPartyCookies
    val enableKernelDisguise: Boolean get() = webViewBehavior.enableKernelDisguise
    val enableImageRepair: Boolean get() = webViewBehavior.enableImageRepair
    val enableScrollMemory: Boolean get() = webViewBehavior.enableScrollMemory
    val followSystemDarkMode: Boolean get() = webViewBehavior.followSystemDarkMode
    val enableClipboardPolyfill: Boolean get() = webViewBehavior.enableClipboardPolyfill
    val enableNotificationPolyfill: Boolean get() = webViewBehavior.enableNotificationPolyfill
    val geolocationEnabled: Boolean get() = webViewBehavior.geolocationEnabled
    val enableOrientationPolyfill: Boolean get() = webViewBehavior.enableOrientationPolyfill
    val enableCompatPolyfills: Boolean get() = webViewBehavior.enableCompatPolyfills
    val enableNativeBridge: Boolean get() = webViewBehavior.enableNativeBridge
    val javaScriptCanOpenWindows: Boolean get() = webViewBehavior.javaScriptCanOpenWindows
    val databaseEnabled: Boolean get() = webViewBehavior.databaseEnabled
    val enableCookiePersistence: Boolean get() = webViewBehavior.enableCookiePersistence
    val enablePrivateNetworkBridge: Boolean get() = webViewBehavior.enablePrivateNetworkBridge
    val allowMixedContent: Boolean get() = webViewBehavior.allowMixedContent
    val enableBlobDownloadInterception: Boolean get() = webViewBehavior.enableBlobDownloadInterception
    val enableCloudflareCompat: Boolean get() = webViewBehavior.enableCloudflareCompat
    val primeUserActivation: Boolean get() = webViewBehavior.primeUserActivation

    val fullscreenVideoOrientation: String get() = webViewBehavior.fullscreenVideoOrientation

    val failoverEnabled: Boolean get() = webViewBehavior.failoverEnabled
    val failoverUrls: List<String> get() = webViewBehavior.failoverUrls
    val failoverTriggerNetworkError: Boolean get() = webViewBehavior.failoverTriggerNetworkError
    val failoverTriggerHttp5xx: Boolean get() = webViewBehavior.failoverTriggerHttp5xx
    val failoverTriggerHttp4xx: Boolean get() = webViewBehavior.failoverTriggerHttp4xx
    val failoverTriggerTimeout: Boolean get() = webViewBehavior.failoverTriggerTimeout
    val failoverTimeoutSeconds: Int get() = webViewBehavior.failoverTimeoutSeconds

    val keepScreenOn: Boolean get() = screenAwake.keepScreenOn
    val screenAwakeMode: String get() = screenAwake.mode
    val screenAwakeTimeoutMinutes: Int get() = screenAwake.timeoutMinutes
    val screenBrightness: Int get() = screenAwake.brightness

    val statusBarColorMode: String get() = statusBar.colorMode
    val statusBarColor: String? get() = statusBar.color
    val statusBarDarkIcons: Boolean? get() = statusBar.darkIcons
    val statusBarBackgroundType: String get() = statusBar.backgroundType
    val statusBarBackgroundImage: String? get() = statusBar.backgroundImage
    val statusBarBackgroundAlpha: Float get() = statusBar.backgroundAlpha
    val statusBarHeightDp: Int get() = statusBar.heightDp
    val statusBarColorModeDark: String get() = statusBar.colorModeDark
    val statusBarColorDark: String? get() = statusBar.colorDark
    val statusBarDarkIconsDark: Boolean? get() = statusBar.darkIconsDark
    val statusBarBackgroundTypeDark: String get() = statusBar.backgroundTypeDark
    val statusBarBackgroundImageDark: String? get() = statusBar.backgroundImageDark
    val statusBarBackgroundAlphaDark: Float get() = statusBar.backgroundAlphaDark

    val floatingWindowEnabled: Boolean get() = floatingWindow.enabled
    val floatingWindowSizePercent: Int get() = floatingWindow.windowSizePercent
    val floatingWindowWidthPercent: Int get() = floatingWindow.widthPercent
    val floatingWindowHeightPercent: Int get() = floatingWindow.heightPercent
    val floatingWindowLockAspectRatio: Boolean get() = floatingWindow.lockAspectRatio
    val floatingWindowAspectRatioMode: String get() = floatingWindow.aspectRatioMode
    val floatingWindowCustomAspectRatioWidth: Int get() = floatingWindow.customAspectRatioWidth
    val floatingWindowCustomAspectRatioHeight: Int get() = floatingWindow.customAspectRatioHeight
    val floatingWindowOpacity: Int get() = floatingWindow.opacity
    val floatingWindowCornerRadius: Int get() = floatingWindow.cornerRadius
    val floatingWindowBorderStyle: String get() = floatingWindow.borderStyle
    val floatingWindowMinimizedIconPath: String? get() = floatingWindow.minimizedIconPath
    val floatingWindowShowTitleBar: Boolean get() = floatingWindow.showTitleBar
    val floatingWindowAutoHideTitleBar: Boolean get() = floatingWindow.autoHideTitleBar
    val floatingWindowStartMinimized: Boolean get() = floatingWindow.startMinimized
    val floatingWindowRememberPosition: Boolean get() = floatingWindow.rememberPosition
    val floatingWindowEdgeSnapping: Boolean get() = floatingWindow.edgeSnapping
    val floatingWindowShowResizeHandle: Boolean get() = floatingWindow.showResizeHandle
    val floatingWindowLockPosition: Boolean get() = floatingWindow.lockPosition

    val proxyMode: String get() = proxy.mode
    val proxyHost: String get() = proxy.host
    val proxyPort: Int get() = proxy.port
    val proxyType: String get() = proxy.type
    val pacUrl: String get() = proxy.pacUrl
    val proxyBypassRules: List<String> get() = proxy.bypassRules
    val proxyUsername: String get() = proxy.username
    val proxyPassword: String get() = proxy.password
    val hostsMappingEnabled: Boolean get() = proxy.hostsMappingEnabled
    val hostsMappings: List<com.webtoapp.data.model.HostMappingEntry> get() = proxy.hostsMappings

    val dnsMode: String get() = dns.mode
    val dnsConfig: DnsApkConfig get() = dns.config

    val errorPageMode: String get() = errorPage.mode
    val errorPageBuiltInStyle: String get() = errorPage.builtInStyle
    val errorPageShowMiniGame: Boolean get() = errorPage.showMiniGame
    val errorPageMiniGameType: String get() = errorPage.miniGameType
    val errorPageAutoRetrySeconds: Int get() = errorPage.autoRetrySeconds
    val errorPageCustomHtml: String get() = errorPage.customHtml
    val errorPageCustomMediaPath: String get() = errorPage.customMediaPath
    val errorPageRetryButtonText: String get() = errorPage.retryButtonText

    val splashEnabled: Boolean get() = splash.enabled
    val splashType: String get() = splash.type
    val splashDuration: Int get() = splash.duration
    val splashClickToSkip: Boolean get() = splash.clickToSkip
    val splashVideoStartMs: Long get() = splash.videoStartMs
    val splashVideoEndMs: Long get() = splash.videoEndMs
    val splashLandscape: Boolean get() = splash.landscape
    val splashFillScreen: Boolean get() = splash.fillScreen
    val splashEnableAudio: Boolean get() = splash.enableAudio

    val mediaEnableAudio: Boolean get() = media.enableAudio
    val mediaLoop: Boolean get() = media.loop
    val mediaAutoPlay: Boolean get() = media.autoPlay
    val mediaFillScreen: Boolean get() = media.fillScreen
    val mediaLandscape: Boolean get() = media.landscape
    val mediaKeepScreenOn: Boolean get() = media.keepScreenOn

    val htmlEntryFile: String get() = html.entryFile
    val htmlEnableJavaScript: Boolean get() = html.enableJavaScript
    val htmlEnableLocalStorage: Boolean get() = html.enableLocalStorage
    val htmlLandscapeMode: Boolean get() = html.landscapeMode

    val galleryItems: List<GalleryShellItemConfig> get() = gallery.items
    val galleryPlayMode: String get() = gallery.playMode
    val galleryImageInterval: Int get() = gallery.imageInterval
    val galleryLoop: Boolean get() = gallery.loop
    val galleryAutoPlay: Boolean get() = gallery.autoPlay
    val galleryBackgroundColor: String get() = gallery.backgroundColor
    val galleryShowThumbnailBar: Boolean get() = gallery.showThumbnailBar
    val galleryShowMediaInfo: Boolean get() = gallery.showMediaInfo
    val galleryOrientation: String get() = gallery.orientation
    val galleryEnableAudio: Boolean get() = gallery.enableAudio
    val galleryVideoAutoNext: Boolean get() = gallery.videoAutoNext
    val galleryShuffleOnLoop: Boolean get() = gallery.shuffleOnLoop
    val galleryDefaultView: String get() = gallery.defaultView
    val galleryGridColumns: Int get() = gallery.gridColumns
    val gallerySortOrder: String get() = gallery.sortOrder
    val galleryRememberPosition: Boolean get() = gallery.rememberPosition

    val bgmEnabled: Boolean get() = bgm.enabled
    val bgmPlaylist: List<BgmShellItem> get() = bgm.playlist
    val bgmPlayMode: String get() = bgm.playMode
    val bgmVolume: Float get() = bgm.volume
    val bgmAutoPlay: Boolean get() = bgm.autoPlay
    val bgmShowLyrics: Boolean get() = bgm.showLyrics
    val bgmLrcTheme: LrcShellTheme? get() = bgm.lrcTheme

    val translateEnabled: Boolean get() = translate.enabled
    val translateTargetLanguage: String get() = translate.targetLanguage
    val translateShowButton: Boolean get() = translate.showButton

    val extensionEnabled: Boolean get() = extension.enabled
    val extensionModuleIds: List<String> get() = extension.moduleIds
    val embeddedExtensionModules: List<EmbeddedExtensionModule> get() = extension.embeddedModules
    val extensionFabIcon: String get() = extension.fabIcon

    val autoStartEnabled: Boolean get() = autoStart.enabled
    val bootStartEnabled: Boolean get() = autoStart.bootStartEnabled
    val scheduledStartEnabled: Boolean get() = autoStart.scheduledStartEnabled
    val scheduledTime: String get() = autoStart.scheduledTime
    val scheduledDays: List<Int> get() = autoStart.scheduledDays

    val forcedRunConfig: ForcedRunConfig? get() = optionalServices.forcedRunConfig
    val isolationEnabled: Boolean get() = optionalServices.isolationEnabled
    val isolationConfig: com.webtoapp.core.privacy.IsolationConfig? get() = optionalServices.isolationConfig
    val backgroundRunEnabled: Boolean get() = optionalServices.backgroundRunEnabled
    val backgroundRunConfig: BackgroundRunConfig? get() = optionalServices.backgroundRunConfig
    val notificationEnabled: Boolean get() = optionalServices.notificationEnabled
    val notificationConfig: NotificationConfig? get() = optionalServices.notificationConfig

    val hardeningEnabled: Boolean get() = optionalServices.hardeningEnabled
    val hardeningThreatResponse: String get() = optionalServices.hardeningThreatResponse

    val blackTechConfig: com.webtoapp.core.actions.DeviceActionsConfig? get() = disguise.blackTechConfig
    val disguiseConfig: com.webtoapp.core.appearance.DisguiseConfig? get() = disguise.disguiseConfig
    val browserDisguiseConfig: com.webtoapp.core.appearance.BrowserDisguiseConfig? get() = disguise.browserDisguiseConfig
    val deviceDisguiseConfig: com.webtoapp.core.appearance.DeviceDisguiseConfig? get() = disguise.deviceDisguiseConfig

    val deepLinkEnabled: Boolean get() = deepLink.enabled
    val deepLinkHosts: List<String> get() = deepLink.hosts

    val wordpressSiteTitle: String get() = wordpress.siteTitle
    val wordpressAdminUser: String get() = wordpress.adminUser
    val wordpressAdminEmail: String get() = wordpress.adminEmail
    val wordpressAdminPassword: String get() = wordpress.adminPassword
    val wordpressThemeName: String get() = wordpress.themeName
    val wordpressPlugins: List<String> get() = wordpress.plugins
    val wordpressActivePlugins: List<String> get() = wordpress.activePlugins
    val wordpressPermalinkStructure: String get() = wordpress.permalinkStructure
    val wordpressSiteLanguage: String get() = wordpress.siteLanguage
    val wordpressAutoInstall: Boolean get() = wordpress.autoInstall
    val wordpressPhpPort: Int get() = wordpress.phpPort
    val wordpressLandscapeMode: Boolean get() = wordpress.landscapeMode

    val nodejsMode: String get() = nodejs.mode
    val nodejsPort: Int get() = nodejs.port
    val nodejsEntryFile: String get() = nodejs.entryFile
    val nodejsEnvVars: Map<String, String> get() = nodejs.envVars
    val nodejsLandscapeMode: Boolean get() = nodejs.landscapeMode

    val phpAppFramework: String get() = phpApp.framework
    val phpAppDocumentRoot: String get() = phpApp.documentRoot
    val phpAppEntryFile: String get() = phpApp.entryFile
    val phpAppPort: Int get() = phpApp.port
    val phpAppEnvVars: Map<String, String> get() = phpApp.envVars
    val phpAppLandscapeMode: Boolean get() = phpApp.landscapeMode

    val pythonAppFramework: String get() = pythonApp.framework
    val pythonAppEntryFile: String get() = pythonApp.entryFile
    val pythonAppEntryModule: String get() = pythonApp.entryModule
    val pythonAppServerType: String get() = pythonApp.serverType
    val pythonAppPort: Int get() = pythonApp.port
    val pythonAppEnvVars: Map<String, String> get() = pythonApp.envVars
    val pythonAppLandscapeMode: Boolean get() = pythonApp.landscapeMode

    val goAppFramework: String get() = goApp.framework
    val goAppBinaryName: String get() = goApp.binaryName
    val goAppTargetArch: String get() = goApp.targetArch
    val goAppPort: Int get() = goApp.port
    val goAppStaticDir: String get() = goApp.staticDir
    val goAppEnvVars: Map<String, String> get() = goApp.envVars
    val goAppLandscapeMode: Boolean get() = goApp.landscapeMode

    val multiWebSites: List<com.webtoapp.core.shell.MultiWebSiteShellConfig> get() = multiWeb.sites
    val multiWebDisplayMode: String get() = multiWeb.displayMode
    val multiWebRefreshInterval: Int get() = multiWeb.refreshInterval
    val multiWebShowSiteIcons: Boolean get() = multiWeb.showSiteIcons
    val multiWebLandscapeMode: Boolean get() = multiWeb.landscapeMode
    val multiWebProjectId: String get() = multiWeb.projectId

    companion object
}

data class MetaBlock(
    val appName: String,
    val packageName: String,
    val targetUrl: String,
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val iconPath: String? = null,
    val runtimePermissions: com.webtoapp.data.model.ApkRuntimePermissions = com.webtoapp.data.model.ApkRuntimePermissions(),
    val networkTrustConfig: com.webtoapp.data.model.NetworkTrustConfig = com.webtoapp.data.model.NetworkTrustConfig(),
    val appType: String = "WEB",
    val themeType: String = "AURORA",
    val darkMode: String = "SYSTEM",
    val language: String = "CHINESE",
    val engineType: String = "SYSTEM_WEBVIEW",
    val htmlUsesFileScheme: Boolean = false,
    val loggingEnabled: Boolean = false
)

data class ActivationBlock(
    val enabled: Boolean = false,
    val codes: List<String> = emptyList(),
    val requireEveryTime: Boolean = false,
    val dialogTitle: String = "",
    val dialogSubtitle: String = "",
    val dialogInputLabel: String = "",
    val dialogButtonText: String = "",
    val remoteEnabled: Boolean = false,
    val remoteVerifyUrl: String = "",
    val remotePublicKey: String = "",
    val remoteOfflinePolicy: String = "ALLOW_CACHED"
)

data class AdBlockBlock(
    val enabled: Boolean = false,
    val rules: List<String> = emptyList()
)

data class AnnouncementBlock(
    val enabled: Boolean = false,
    val title: String = "",
    val content: String = "",
    val contentIsHtml: Boolean = false,
    val link: String = "",
    val linkText: String = "",
    val template: String = "MINIMAL",
    val showEmoji: Boolean = true,
    val animationEnabled: Boolean = true,
    val showOnce: Boolean = true,
    val requireConfirmation: Boolean = false,
    val allowNeverShow: Boolean = false,
    val triggerOnLaunch: Boolean = true,
    val triggerOnNoNetwork: Boolean = false,
    val triggerIntervalMinutes: Int = 0
)

data class AdsBlock(
    val enabled: Boolean = false,
    val bannerEnabled: Boolean = false,
    val bannerId: String = "",
    val interstitialEnabled: Boolean = false,
    val interstitialId: String = "",
    val splashEnabled: Boolean = false,
    val splashId: String = ""
)

data class WebViewBlock(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val allowFileAccess: Boolean = false,
    val allowContentAccess: Boolean = true,
    val cacheEnabled: Boolean = true,
    val clearBrowsingDataOnLaunch: Boolean = false,
    val zoomEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val userAgent: String? = null,
    val userAgentMode: String = "DEFAULT",
    val customUserAgent: String? = null,
    val hideToolbar: Boolean = false,
    val hideBrowserToolbar: Boolean = false,
    val toolbarShowTitle: Boolean = true,
    val toolbarShowUrl: Boolean = true,
    val toolbarShowBack: Boolean = true,
    val toolbarShowForward: Boolean = true,
    val toolbarShowRefresh: Boolean = true,
    val showStatusBarInFullscreen: Boolean = false,
    val showNavigationBarInFullscreen: Boolean = false,
    val showToolbarInFullscreen: Boolean = false,
    val landscapeMode: Boolean = false,
    val orientationMode: String = "PORTRAIT",
    val injectScripts: List<com.webtoapp.data.model.UserScript> = emptyList(),
    val longPressMenuEnabled: Boolean = false,
    val longPressMenuStyle: String = "DISABLED",
    val adBlockToggleEnabled: Boolean = false,
    val popupBlockerEnabled: Boolean = false,
    val popupBlockerToggleEnabled: Boolean = false,
    val openExternalLinks: Boolean = false,
    val showFloatingBackButton: Boolean = false,
    val swipeRefreshEnabled: Boolean = true,
    val fullscreenEnabled: Boolean = true,
    val performanceOptimization: Boolean = false,
    val pwaOfflineEnabled: Boolean = false,
    val pwaOfflineStrategy: String = "NETWORK_FIRST",
    val keyboardAdjustMode: String = "RESIZE"
)

data class WebViewBehaviorBlock(
    val initialScale: Int = 0,
    val viewportMode: String = "DEFAULT",
    val customViewportWidth: Int = 0,
    val newWindowBehavior: String = "SAME_WINDOW",
    val enablePaymentSchemes: Boolean = true,
    val enableShareBridge: Boolean = true,
    val enableZoomPolyfill: Boolean = true,
    val enableCrossOriginIsolation: Boolean = false,
    val hideUrlPreview: Boolean = false,

    val decodeBase64DeepLinks: Boolean = false,
    val decodeBase64Mode: String = "GESTURE_ONLY",
    val javaScriptCanOpenWindows: Boolean = false,
    val jsOpenWindowsPolicy: String = "ALLOW",
    val mediaAutoplayEnabled: Boolean = false,
    val mediaAutoplayScope: String = "VIDEO_ONLY",
    val acceptThirdPartyCookies: Boolean = false,
    val thirdPartyCookieMode: String = "SAME_SITE_LAX",
    val enableKernelDisguise: Boolean = false,
    val kernelDisguiseLevel: String = "STANDARD",
    val kernelFlavor: String = "SYSTEM_DEFAULT",
    val enableImageRepair: Boolean = false,
    val enableScrollMemory: Boolean = false,
    val followSystemDarkMode: Boolean = false,
    val enableClipboardPolyfill: Boolean = false,
    val enableNotificationPolyfill: Boolean = false,
    val geolocationEnabled: Boolean = false,
    val geolocationAccuracy: String = "COARSE",
    val geolocationPolicy: String = "ALWAYS_ASK",
    val enableOrientationPolyfill: Boolean = false,
    val enableCompatPolyfills: Boolean = false,
    val enableNativeBridge: Boolean = false,
    val nativeBridgeClipboard: Boolean = true,
    val nativeBridgeVibration: Boolean = true,
    val nativeBridgeGeolocation: Boolean = true,
    val nativeBridgeBrightness: Boolean = true,
    val nativeBridgeNotification: Boolean = true,
    val nativeBridgeDownload: Boolean = true,
    val nativeBridgePrivateNetwork: Boolean = true,
    val nativeBridgeScreenWake: Boolean = true,
    val databaseEnabled: Boolean = true,
    val enableCookiePersistence: Boolean = true,
    val enablePrivateNetworkBridge: Boolean = false,
    val privateNetworkScope: String = "LOCAL_ONLY",
    val allowMixedContent: Boolean = false,
    val mixedContentMode: String = "COMPATIBILITY",
    val enableBlobDownloadInterception: Boolean = false,
    val blobInterceptScope: String = "ALL",
    val blobInterceptThresholdMb: Int = 5,
    val enableCloudflareCompat: Boolean = true,
    val cloudflareCompatMode: String = "AUTO_DETECT",
    val primeUserActivation: Boolean = false,
    val primeUserActivationMode: String = "SYNTHETIC_TAP",
    val primeUserActivationTiming: String = "ON_PAGE_FINISHED",

    val fullscreenVideoOrientation: String = "AUTO_SENSOR_LANDSCAPE",

    val failoverEnabled: Boolean = false,
    val failoverUrls: List<String> = emptyList(),
    val failoverTriggerNetworkError: Boolean = true,
    val failoverTriggerHttp5xx: Boolean = true,
    val failoverTriggerHttp4xx: Boolean = false,
    val failoverTriggerTimeout: Boolean = false,
    val failoverTimeoutSeconds: Int = 15
)

data class ScreenAwakeBlock(
    val keepScreenOn: Boolean = false,
    val mode: String = "OFF",
    val timeoutMinutes: Int = 30,
    val brightness: Int = -1
)

data class StatusBarBlock(
    val colorMode: String = "THEME",
    val color: String? = null,
    val darkIcons: Boolean? = null,
    val backgroundType: String = "COLOR",
    val backgroundImage: String? = null,
    val backgroundAlpha: Float = 1.0f,
    val heightDp: Int = -1,
    val colorModeDark: String = "THEME",
    val colorDark: String? = null,
    val darkIconsDark: Boolean? = null,
    val backgroundTypeDark: String = "COLOR",
    val backgroundImageDark: String? = null,
    val backgroundAlphaDark: Float = 1.0f
)

data class FloatingWindowBlock(
    val enabled: Boolean = false,
    val windowSizePercent: Int = 80,
    val widthPercent: Int = 80,
    val heightPercent: Int = 80,
    val lockAspectRatio: Boolean = true,
    val aspectRatioMode: String = "SCREEN",
    val customAspectRatioWidth: Int = 16,
    val customAspectRatioHeight: Int = 9,
    val opacity: Int = 100,
    val cornerRadius: Int = 16,
    val borderStyle: String = "SUBTLE",
    val minimizedIconPath: String? = null,
    val minimizedIconSizePercent: Int = 100,
    val minimizedIconEdgeDocking: Boolean = false,
    val showTitleBar: Boolean = true,
    val autoHideTitleBar: Boolean = false,
    val startMinimized: Boolean = false,
    val rememberPosition: Boolean = true,
    val edgeSnapping: Boolean = true,
    val showResizeHandle: Boolean = true,
    val lockPosition: Boolean = false
)

data class ProxyBlock(
    val mode: String = "NONE",
    val host: String = "",
    val port: Int = 0,
    val type: String = "HTTP",
    val pacUrl: String = "",
    val bypassRules: List<String> = emptyList(),
    val username: String = "",
    val password: String = "",
    val hostsMappingEnabled: Boolean = false,
    val hostsMappings: List<com.webtoapp.data.model.HostMappingEntry> = emptyList()
)

data class DnsBlock(
    val mode: String = "SYSTEM",
    val config: DnsApkConfig = DnsApkConfig()
)

data class ErrorPageBlock(
    val mode: String = "BUILTIN_STYLE",
    val builtInStyle: String = "MATERIAL",
    val showMiniGame: Boolean = false,
    val miniGameType: String = "RANDOM",
    val autoRetrySeconds: Int = 15,
    val customHtml: String = "",
    val customMediaPath: String = "",
    val retryButtonText: String = ""
)

data class SplashBlock(
    val enabled: Boolean = false,
    val type: String = "IMAGE",
    val duration: Int = 3,
    val clickToSkip: Boolean = true,
    val videoStartMs: Long = 0,
    val videoEndMs: Long = 5000,
    val landscape: Boolean = false,
    val fillScreen: Boolean = true,
    val enableAudio: Boolean = false
)

data class MediaBlock(
    val enableAudio: Boolean = true,
    val loop: Boolean = true,
    val autoPlay: Boolean = true,
    val fillScreen: Boolean = true,
    val landscape: Boolean = false,
    val keepScreenOn: Boolean = true
)

data class HtmlBlock(
    val entryFile: String = "index.html",
    val enableJavaScript: Boolean = true,
    val enableLocalStorage: Boolean = true,
    val landscapeMode: Boolean = false,
    val loadMode: String = "AUTO"
)

data class GalleryBlock(
    val items: List<GalleryShellItemConfig> = emptyList(),
    val playMode: String = "SEQUENTIAL",
    val imageInterval: Int = 3,
    val loop: Boolean = true,
    val autoPlay: Boolean = false,
    val backgroundColor: String = "#000000",
    val showThumbnailBar: Boolean = true,
    val showMediaInfo: Boolean = true,
    val orientation: String = "PORTRAIT",
    val enableAudio: Boolean = true,
    val videoAutoNext: Boolean = true,
    val shuffleOnLoop: Boolean = false,
    val defaultView: String = "GRID",
    val gridColumns: Int = 3,
    val sortOrder: String = "CUSTOM",
    val rememberPosition: Boolean = true
)

data class BgmBlock(
    val enabled: Boolean = false,
    val playlist: List<BgmShellItem> = emptyList(),
    val playMode: String = "LOOP",
    val volume: Float = 0.5f,
    val autoPlay: Boolean = true,
    val showLyrics: Boolean = true,
    val lrcTheme: LrcShellTheme? = null
)

data class TranslateBlock(
    val enabled: Boolean = false,
    val targetLanguage: String = "zh-CN",
    val showButton: Boolean = true
)

data class ExtensionBlock(
    val enabled: Boolean = false,
    val moduleIds: List<String> = emptyList(),
    val embeddedModules: List<EmbeddedExtensionModule> = emptyList(),
    val fabIcon: String = ""
)

data class AutoStartBlock(
    val enabled: Boolean = false,
    val bootStartEnabled: Boolean = false,
    val scheduledStartEnabled: Boolean = false,
    val scheduledTime: String = "08:00",
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
)

data class OptionalServicesBlock(
    val forcedRunConfig: ForcedRunConfig? = null,
    val isolationEnabled: Boolean = false,
    val isolationConfig: com.webtoapp.core.privacy.IsolationConfig? = null,
    val backgroundRunEnabled: Boolean = false,
    val backgroundRunConfig: BackgroundRunConfig? = null,
    val notificationEnabled: Boolean = false,
    val notificationConfig: NotificationConfig? = null,
    val hardeningEnabled: Boolean = false,
    val hardeningThreatResponse: String = "LOG_ONLY"
)

data class DisguiseBlock(
    val blackTechConfig: com.webtoapp.core.actions.DeviceActionsConfig? = null,
    val disguiseConfig: com.webtoapp.core.appearance.DisguiseConfig? = null,
    val browserDisguiseConfig: com.webtoapp.core.appearance.BrowserDisguiseConfig? = null,
    val deviceDisguiseConfig: com.webtoapp.core.appearance.DeviceDisguiseConfig? = null
)

data class DeepLinkBlock(
    val enabled: Boolean = false,
    val hosts: List<String> = emptyList()
)

data class WordpressBlock(
    val siteTitle: String = "",
    val adminUser: String = "admin",
    val adminEmail: String = "",
    val adminPassword: String = "admin",
    val themeName: String = "",
    val plugins: List<String> = emptyList(),
    val activePlugins: List<String> = emptyList(),
    val permalinkStructure: String = "/%postname%/",
    val siteLanguage: String = "zh_CN",
    val autoInstall: Boolean = true,
    val phpPort: Int = 0,
    val landscapeMode: Boolean = false
)

data class NodejsBlock(
    val mode: String = "STATIC",
    val port: Int = 0,
    val entryFile: String = "",
    val envVars: Map<String, String> = emptyMap(),
    val landscapeMode: Boolean = false
)

data class PhpAppBlock(
    val framework: String = "",
    val documentRoot: String = "",
    val entryFile: String = "index.php",
    val port: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val landscapeMode: Boolean = false
)

data class PythonAppBlock(
    val framework: String = "",
    val entryFile: String = "app.py",
    val entryModule: String = "",
    val serverType: String = "builtin",
    val port: Int = 0,
    val envVars: Map<String, String> = emptyMap(),
    val landscapeMode: Boolean = false
)

data class GoAppBlock(
    val framework: String = "",
    val binaryName: String = "",
    val targetArch: String = "arm64-v8a",
    val port: Int = 0,
    val staticDir: String = "",
    val envVars: Map<String, String> = emptyMap(),
    val landscapeMode: Boolean = false
)

data class MultiWebBlock(
    val sites: List<com.webtoapp.core.shell.MultiWebSiteShellConfig> = emptyList(),
    val displayMode: String = "TABS",
    val refreshInterval: Int = 30,
    val showSiteIcons: Boolean = true,
    val landscapeMode: Boolean = false,
    val projectId: String = ""
)

data class BackgroundRunConfig(
    val notificationTitle: String = "",
    val notificationContent: String = "",
    val showNotification: Boolean = true,
    val keepCpuAwake: Boolean = true
)

data class NotificationConfig(
    val type: String = "none",
    val pollUrl: String = "",
    val pollIntervalMinutes: Int = 15,
    val pollMethod: String = "GET",
    val pollHeaders: String = "",
    val clickUrl: String = ""
)

data class GalleryShellItemConfig(
    val id: String,
    val assetPath: String,
    val type: String,
    val name: String,
    val duration: Long = 0,
    val thumbnailPath: String? = null
)

data class EmbeddedExtensionModule(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "package",
    val category: String = "OTHER",
    val versionName: String = "1.0.0",
    val authorName: String = "",
    val code: String = "",
    val cssCode: String = "",
    val runAt: String = "DOCUMENT_END",
    val sourceType: String = "CUSTOM",
    val runMode: String = "INTERACTIVE",
    val uiConfig: EmbeddedExtensionModuleUiConfig = EmbeddedExtensionModuleUiConfig(),
    val urlMatches: List<EmbeddedUrlMatchRule> = emptyList(),
    val configValues: Map<String, String> = emptyMap(),
    val configItemCount: Int = 0,
    val gmGrants: List<String> = emptyList(),
    val requireUrls: List<String> = emptyList(),
    val requireContents: Map<String, String> = emptyMap(),
    val resources: Map<String, String> = emptyMap(),
    val noframes: Boolean = false,
    val enabled: Boolean = true
)

data class EmbeddedExtensionModuleUiConfig(
    val type: String = "FLOATING_BUTTON",
    val autoHide: Boolean = false,
    val autoHideDelay: Int = 3000,
    val initiallyHidden: Boolean = false,
    val showOnlyOnMatch: Boolean = true
)

data class EmbeddedUrlMatchRule(
    val pattern: String,
    val isRegex: Boolean = false,
    val exclude: Boolean = false
)

data class DnsApkConfig(
    val provider: String = "cloudflare",
    val customDohUrl: String = "",
    val dohMode: String = "automatic",
    val bypassSystemDns: Boolean = false,
    val echEnabled: Boolean = false
)
