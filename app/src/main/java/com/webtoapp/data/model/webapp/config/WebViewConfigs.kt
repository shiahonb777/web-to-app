package com.webtoapp.data.model.webapp.config

import androidx.compose.runtime.Stable

enum class StatusBarColorMode {
    THEME,      // Follow the theme color
    TRANSPARENT,// Fully transparent
    CUSTOM      // Custom color
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
    val hideToolbar: Boolean = false,
    val hideBrowserToolbar: Boolean = false,
    val showStatusBarInFullscreen: Boolean = false,
    val showNavigationBarInFullscreen: Boolean = false,
    val showToolbarInFullscreen: Boolean = false,
    val landscapeMode: Boolean = false,
    val orientationMode: OrientationMode = if (landscapeMode) OrientationMode.LANDSCAPE else OrientationMode.PORTRAIT,
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
    val statusBarDarkIconsDark: Boolean? = null,
    val statusBarBackgroundTypeDark: StatusBarBackgroundType = StatusBarBackgroundType.COLOR,
    val statusBarBackgroundImageDark: String? = null,
    val statusBarBackgroundAlphaDark: Float = 1.0f,
    val longPressMenuEnabled: Boolean = true,
    val longPressMenuStyle: LongPressMenuStyle = LongPressMenuStyle.FULL,
    val adBlockToggleEnabled: Boolean = false,
    val popupBlockerEnabled: Boolean = true,
    val popupBlockerToggleEnabled: Boolean = false,
    val initialScale: Int = 0,
    val viewportMode: ViewportMode = ViewportMode.DEFAULT,
    val customViewportWidth: Int = 0,
    val newWindowBehavior: NewWindowBehavior = NewWindowBehavior.SAME_WINDOW,
    val enablePaymentSchemes: Boolean = true,
    val enableShareBridge: Boolean = true,
    val enableZoomPolyfill: Boolean = true,
    val enableCrossOriginIsolation: Boolean = false,
    val disableShields: Boolean = false,
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
    val showFloatingBackButton: Boolean = true,
    val blockSystemNavigationGesture: Boolean = false,
    val floatingWindowConfig: FloatingWindowConfig = FloatingWindowConfig()
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

/**
 * Controls how the page reacts when the soft keyboard opens.
 */
enum class KeyboardAdjustMode {
    RESIZE,      // Resize the page so inputs stay visible
    NOTHING      // Let the keyboard overlay the page
}

/**
 * Supported screen orientation modes.
 */
enum class OrientationMode {
    PORTRAIT,            // Lock to portrait
    LANDSCAPE,           // Lock to landscape
    REVERSE_PORTRAIT,    // Lock to reverse portrait
    REVERSE_LANDSCAPE,   // Lock to reverse landscape
    SENSOR_PORTRAIT,     // Auto-switch within portrait orientations
    SENSOR_LANDSCAPE,    // Auto-switch within landscape orientations
    AUTO                 // Fully sensor-driven rotation
}

/**
 * Screen awake behavior.
 */
enum class ScreenAwakeMode {
    OFF,       // Follow the system timeout
    ALWAYS,    // Keep the screen on until the app exits
    TIMED      // Keep the screen on for a limited time
}

/**
 * Controls how WebView scales the page viewport.
 */
enum class ViewportMode {
    DEFAULT,       // Standard behavior for most pages
    FIT_SCREEN,    // Force content to fit the screen
    DESKTOP,       // Use a desktop-style viewport
    CUSTOM         // Use a custom viewport width
}

/**
 * Media app configuration kept for legacy single-media flows.
 */
