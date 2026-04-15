package com.webtoapp.core.extension

import com.google.gson.annotations.SerializedName
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.util.GsonProvider
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Max time (ms) allowed for a single user-supplied regex match. */
private const val REGEX_TIMEOUT_MS = 200L

/** Shared single-thread executor for regex matching (avoids creating a new thread per call). */
private val regexExecutor by lazy {
    Executors.newSingleThreadExecutor { r ->
        Thread(r, "SafeRegexMatcher").apply { isDaemon = true }
    }
}

/** LRU-style cache for compiled Regex patterns (max 64 entries). */
private val regexCache = object : LinkedHashMap<String, Regex>(32, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Regex>?) = size > 64
}

/** Run a regex match with a timeout to prevent ReDoS. */
private fun safeRegexMatch(pattern: String, input: String): Boolean {
    return try {
        val compiledRegex = synchronized(regexCache) {
            regexCache.getOrPut(pattern) { Regex(pattern) }
        }
        val future = regexExecutor.submit<Boolean> { compiledRegex.containsMatchIn(input) }
        future.get(REGEX_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    } catch (e: TimeoutException) {
        false
    } catch (e: Exception) {
        false
    }
}

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

enum class ModuleCategory(val icon: String) {
    CONTENT_FILTER("block"),
    CONTENT_ENHANCE("auto_awesome"),
    STYLE_MODIFIER("palette"),
    THEME("rainbow"),
    FUNCTION_ENHANCE("bolt"),
    AUTOMATION("smart_toy"),
    NAVIGATION("explore"),
    DATA_EXTRACT("analytics"),
    DATA_SAVE("save"),
    INTERACTION("mouse"),
    ACCESSIBILITY("accessibility"),
    MEDIA("movie"),
    VIDEO("videocam"),
    IMAGE("image"),
    AUDIO("music_note"),
    SECURITY("lock"),
    ANTI_TRACKING("person_search"),
    SOCIAL("chat"),
    SHOPPING("shopping_cart"),
    READING("book"),
    TRANSLATE("globe"),
    DEVELOPER("wrench"),
    OTHER("package");
    
    fun getDisplayName(): String = when (this) {
        CONTENT_FILTER -> AppStringsProvider.current().catContentFilter
        CONTENT_ENHANCE -> AppStringsProvider.current().catContentEnhance
        STYLE_MODIFIER -> AppStringsProvider.current().catStyleModifier
        THEME -> AppStringsProvider.current().catTheme
        FUNCTION_ENHANCE -> AppStringsProvider.current().catFunctionEnhance
        AUTOMATION -> AppStringsProvider.current().catAutomation
        NAVIGATION -> AppStringsProvider.current().catNavigation
        DATA_EXTRACT -> AppStringsProvider.current().catDataExtract
        DATA_SAVE -> AppStringsProvider.current().catDataSave
        INTERACTION -> AppStringsProvider.current().catInteraction
        ACCESSIBILITY -> AppStringsProvider.current().catAccessibility
        MEDIA -> AppStringsProvider.current().catMedia
        VIDEO -> AppStringsProvider.current().catVideo
        IMAGE -> AppStringsProvider.current().catImage
        AUDIO -> AppStringsProvider.current().catAudio
        SECURITY -> AppStringsProvider.current().catSecurity
        ANTI_TRACKING -> AppStringsProvider.current().catAntiTracking
        SOCIAL -> AppStringsProvider.current().catSocial
        SHOPPING -> AppStringsProvider.current().catShopping
        READING -> AppStringsProvider.current().catReading
        TRANSLATE -> AppStringsProvider.current().catTranslate
        DEVELOPER -> AppStringsProvider.current().catDeveloper
        OTHER -> AppStringsProvider.current().catOther
    }
    
    fun getDescription(): String = when (this) {
        CONTENT_FILTER -> AppStringsProvider.current().catContentFilterDesc
        CONTENT_ENHANCE -> AppStringsProvider.current().catContentEnhanceDesc
        STYLE_MODIFIER -> AppStringsProvider.current().catStyleModifierDesc
        THEME -> AppStringsProvider.current().catThemeDesc
        FUNCTION_ENHANCE -> AppStringsProvider.current().catFunctionEnhanceDesc
        AUTOMATION -> AppStringsProvider.current().catAutomationDesc
        NAVIGATION -> AppStringsProvider.current().catNavigationDesc
        DATA_EXTRACT -> AppStringsProvider.current().catDataExtractDesc
        DATA_SAVE -> AppStringsProvider.current().catDataSaveDesc
        INTERACTION -> AppStringsProvider.current().catInteractionDesc
        ACCESSIBILITY -> AppStringsProvider.current().catAccessibilityDesc
        MEDIA -> AppStringsProvider.current().catMediaDesc
        VIDEO -> AppStringsProvider.current().catVideoDesc
        IMAGE -> AppStringsProvider.current().catImageDesc
        AUDIO -> AppStringsProvider.current().catAudioDesc
        SECURITY -> AppStringsProvider.current().catSecurityDesc
        ANTI_TRACKING -> AppStringsProvider.current().catAntiTrackingDesc
        SOCIAL -> AppStringsProvider.current().catSocialDesc
        SHOPPING -> AppStringsProvider.current().catShoppingDesc
        READING -> AppStringsProvider.current().catReadingDesc
        TRANSLATE -> AppStringsProvider.current().catTranslateDesc
        DEVELOPER -> AppStringsProvider.current().catDeveloperDesc
        OTHER -> AppStringsProvider.current().catOtherDesc
    }
}

enum class ModuleRunTime(val jsEvent: String) {
    DOCUMENT_START(""),
    DOCUMENT_END("DOMContentLoaded"),
    DOCUMENT_IDLE("load"),
    CONTEXT_MENU("contextmenu"),
    BEFORE_UNLOAD("beforeunload");
    
    fun getDisplayName(): String = when (this) {
        DOCUMENT_START -> AppStringsProvider.current().runTimeDocStart
        DOCUMENT_END -> AppStringsProvider.current().runTimeDocEnd
        DOCUMENT_IDLE -> AppStringsProvider.current().runTimeDocIdle
        CONTEXT_MENU -> AppStringsProvider.current().runTimeContextMenu
        BEFORE_UNLOAD -> AppStringsProvider.current().runTimeBeforeUnload
    }
    
    fun getDescription(): String = when (this) {
        DOCUMENT_START -> AppStringsProvider.current().runTimeDocStartDesc
        DOCUMENT_END -> AppStringsProvider.current().runTimeDocEndDesc
        DOCUMENT_IDLE -> AppStringsProvider.current().runTimeDocIdleDesc
        CONTEXT_MENU -> AppStringsProvider.current().runTimeContextMenuDesc
        BEFORE_UNLOAD -> AppStringsProvider.current().runTimeBeforeUnloadDesc
    }
}

enum class ModuleTrigger {
    AUTO,
    MANUAL,
    INTERVAL,
    MUTATION,
    SCROLL,
    CLICK,
    HOVER,
    FOCUS,
    INPUT,
    VISIBILITY;
    
    val displayName: String get() = when (this) {
        AUTO -> AppStringsProvider.current().triggerAuto
        MANUAL -> AppStringsProvider.current().triggerManual
        INTERVAL -> AppStringsProvider.current().triggerInterval
        MUTATION -> AppStringsProvider.current().triggerMutation
        SCROLL -> AppStringsProvider.current().triggerScroll
        CLICK -> AppStringsProvider.current().triggerClick
        HOVER -> AppStringsProvider.current().triggerHover
        FOCUS -> AppStringsProvider.current().triggerFocus
        INPUT -> AppStringsProvider.current().triggerInput
        VISIBILITY -> AppStringsProvider.current().triggerVisibility
    }
    
    val description: String get() = when (this) {
        AUTO -> AppStringsProvider.current().triggerAutoDesc
        MANUAL -> AppStringsProvider.current().triggerManualDesc
        INTERVAL -> AppStringsProvider.current().triggerIntervalDesc
        MUTATION -> AppStringsProvider.current().triggerMutationDesc
        SCROLL -> AppStringsProvider.current().triggerScrollDesc
        CLICK -> AppStringsProvider.current().triggerClickDesc
        HOVER -> AppStringsProvider.current().triggerHoverDesc
        FOCUS -> AppStringsProvider.current().triggerFocusDesc
        INPUT -> AppStringsProvider.current().triggerInputDesc
        VISIBILITY -> AppStringsProvider.current().triggerVisibilityDesc
    }
}

enum class ModulePermission(val dangerous: Boolean = false) {
    DOM_ACCESS,
    DOM_OBSERVE,
    CSS_INJECT,
    STORAGE,
    COOKIE(true),
    INDEXED_DB(true),
    CACHE,
    NETWORK(true),
    WEBSOCKET(true),
    FETCH_INTERCEPT(true),
    CLIPBOARD(true),
    NOTIFICATION,
    ALERT,
    KEYBOARD,
    MOUSE,
    TOUCH,
    LOCATION(true),
    CAMERA(true),
    MICROPHONE(true),
    DEVICE_INFO,
    MEDIA,
    FULLSCREEN,
    PICTURE_IN_PICTURE,
    SCREEN_CAPTURE(true),
    DOWNLOAD,
    FILE_ACCESS(true),
    EVAL(true),
    IFRAME(true),
    WINDOW_OPEN,
    HISTORY,
    NAVIGATION;
    
    val displayName: String get() = when (this) {
        DOM_ACCESS -> AppStringsProvider.current().permDomAccess
        DOM_OBSERVE -> AppStringsProvider.current().permDomObserve
        CSS_INJECT -> AppStringsProvider.current().permCssInject
        STORAGE -> AppStringsProvider.current().permStorage
        COOKIE -> AppStringsProvider.current().permCookie
        INDEXED_DB -> AppStringsProvider.current().permIndexedDb
        CACHE -> AppStringsProvider.current().permCache
        NETWORK -> AppStringsProvider.current().permNetwork
        WEBSOCKET -> AppStringsProvider.current().permWebsocket
        FETCH_INTERCEPT -> AppStringsProvider.current().permFetchIntercept
        CLIPBOARD -> AppStringsProvider.current().permClipboard
        NOTIFICATION -> AppStringsProvider.current().permNotification
        ALERT -> AppStringsProvider.current().permAlert
        KEYBOARD -> AppStringsProvider.current().permKeyboard
        MOUSE -> AppStringsProvider.current().permMouse
        TOUCH -> AppStringsProvider.current().permTouch
        LOCATION -> AppStringsProvider.current().permLocation
        CAMERA -> AppStringsProvider.current().permCamera
        MICROPHONE -> AppStringsProvider.current().permMicrophone
        DEVICE_INFO -> AppStringsProvider.current().permDeviceInfo
        MEDIA -> AppStringsProvider.current().permMedia
        FULLSCREEN -> AppStringsProvider.current().permFullscreen
        PICTURE_IN_PICTURE -> AppStringsProvider.current().permPip
        SCREEN_CAPTURE -> AppStringsProvider.current().permScreenCapture
        DOWNLOAD -> AppStringsProvider.current().permDownload
        FILE_ACCESS -> AppStringsProvider.current().permFileAccess
        EVAL -> AppStringsProvider.current().permEval
        IFRAME -> AppStringsProvider.current().permIframe
        WINDOW_OPEN -> AppStringsProvider.current().permWindowOpen
        HISTORY -> AppStringsProvider.current().permHistory
        NAVIGATION -> AppStringsProvider.current().permNavigation
    }
    
    val description: String get() = when (this) {
        DOM_ACCESS -> AppStringsProvider.current().permDomAccessDesc
        DOM_OBSERVE -> AppStringsProvider.current().permDomObserveDesc
        CSS_INJECT -> AppStringsProvider.current().permCssInjectDesc
        STORAGE -> AppStringsProvider.current().permStorageDesc
        COOKIE -> AppStringsProvider.current().permCookieDesc
        INDEXED_DB -> AppStringsProvider.current().permIndexedDbDesc
        CACHE -> AppStringsProvider.current().permCacheDesc
        NETWORK -> AppStringsProvider.current().permNetworkDesc
        WEBSOCKET -> AppStringsProvider.current().permWebsocketDesc
        FETCH_INTERCEPT -> AppStringsProvider.current().permFetchInterceptDesc
        CLIPBOARD -> AppStringsProvider.current().permClipboardDesc
        NOTIFICATION -> AppStringsProvider.current().permNotificationDesc
        ALERT -> AppStringsProvider.current().permAlertDesc
        KEYBOARD -> AppStringsProvider.current().permKeyboardDesc
        MOUSE -> AppStringsProvider.current().permMouseDesc
        TOUCH -> AppStringsProvider.current().permTouchDesc
        LOCATION -> AppStringsProvider.current().permLocationDesc
        CAMERA -> AppStringsProvider.current().permCameraDesc
        MICROPHONE -> AppStringsProvider.current().permMicrophoneDesc
        DEVICE_INFO -> AppStringsProvider.current().permDeviceInfoDesc
        MEDIA -> AppStringsProvider.current().permMediaDesc
        FULLSCREEN -> AppStringsProvider.current().permFullscreenDesc
        PICTURE_IN_PICTURE -> AppStringsProvider.current().permPipDesc
        SCREEN_CAPTURE -> AppStringsProvider.current().permScreenCaptureDesc
        DOWNLOAD -> AppStringsProvider.current().permDownloadDesc
        FILE_ACCESS -> AppStringsProvider.current().permFileAccessDesc
        EVAL -> AppStringsProvider.current().permEvalDesc
        IFRAME -> AppStringsProvider.current().permIframeDesc
        WINDOW_OPEN -> AppStringsProvider.current().permWindowOpenDesc
        HISTORY -> AppStringsProvider.current().permHistoryDesc
        NAVIGATION -> AppStringsProvider.current().permNavigationDesc
    }
}

enum class ConfigItemType {
    TEXT, TEXTAREA, NUMBER, BOOLEAN,
    SELECT, MULTI_SELECT, RADIO, CHECKBOX,
    COLOR, URL, EMAIL, PASSWORD,
    REGEX, CSS_SELECTOR, JAVASCRIPT, JSON,
    RANGE, DATE, TIME, DATETIME,
    FILE, IMAGE;
    
    val displayName: String get() = when (this) {
        TEXT -> AppStringsProvider.current().configTypeText
        TEXTAREA -> AppStringsProvider.current().configTypeTextarea
        NUMBER -> AppStringsProvider.current().configTypeNumber
        BOOLEAN -> AppStringsProvider.current().configTypeBoolean
        SELECT -> AppStringsProvider.current().configTypeSelect
        MULTI_SELECT -> AppStringsProvider.current().configTypeMultiSelect
        RADIO -> AppStringsProvider.current().configTypeRadio
        CHECKBOX -> AppStringsProvider.current().configTypeCheckbox
        COLOR -> AppStringsProvider.current().configTypeColor
        URL -> AppStringsProvider.current().configTypeUrl
        EMAIL -> AppStringsProvider.current().configTypeEmail
        PASSWORD -> AppStringsProvider.current().configTypePassword
        REGEX -> AppStringsProvider.current().configTypeRegex
        CSS_SELECTOR -> AppStringsProvider.current().configTypeCssSelector
        JAVASCRIPT -> AppStringsProvider.current().configTypeJavascript
        JSON -> AppStringsProvider.current().configTypeJson
        RANGE -> AppStringsProvider.current().configTypeRange
        DATE -> AppStringsProvider.current().configTypeDate
        TIME -> AppStringsProvider.current().configTypeTime
        DATETIME -> AppStringsProvider.current().configTypeDatetime
        FILE -> AppStringsProvider.current().configTypeFile
        IMAGE -> AppStringsProvider.current().configTypeImage
    }
    
    val description: String get() = when (this) {
        TEXT -> AppStringsProvider.current().configTypeTextDesc
        TEXTAREA -> AppStringsProvider.current().configTypeTextareaDesc
        NUMBER -> AppStringsProvider.current().configTypeNumberDesc
        BOOLEAN -> AppStringsProvider.current().configTypeBooleanDesc
        SELECT -> AppStringsProvider.current().configTypeSelectDesc
        MULTI_SELECT -> AppStringsProvider.current().configTypeMultiSelectDesc
        RADIO -> AppStringsProvider.current().configTypeRadioDesc
        CHECKBOX -> AppStringsProvider.current().configTypeCheckboxDesc
        COLOR -> AppStringsProvider.current().configTypeColorDesc
        URL -> AppStringsProvider.current().configTypeUrlDesc
        EMAIL -> AppStringsProvider.current().configTypeEmailDesc
        PASSWORD -> AppStringsProvider.current().configTypePasswordDesc
        REGEX -> AppStringsProvider.current().configTypeRegexDesc
        CSS_SELECTOR -> AppStringsProvider.current().configTypeCssSelectorDesc
        JAVASCRIPT -> AppStringsProvider.current().configTypeJavascriptDesc
        JSON -> AppStringsProvider.current().configTypeJsonDesc
        RANGE -> AppStringsProvider.current().configTypeRangeDesc
        DATE -> AppStringsProvider.current().configTypeDateDesc
        TIME -> AppStringsProvider.current().configTypeTimeDesc
        DATETIME -> AppStringsProvider.current().configTypeDatetimeDesc
        FILE -> AppStringsProvider.current().configTypeFileDesc
        IMAGE -> AppStringsProvider.current().configTypeImageDesc
    }
}

data class ModuleConfigItem(
    @SerializedName("key")
    val key: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("type")
    val type: ConfigItemType = ConfigItemType.TEXT,
    @SerializedName("defaultValue")
    val defaultValue: String = "",
    @SerializedName("options")
    val options: List<String> = emptyList(),
    @SerializedName("required")
    val required: Boolean = false,
    @SerializedName("placeholder")
    val placeholder: String = "",
    @SerializedName("validation")
    val validation: String? = null
)

enum class ModuleUiType {
    FLOATING_BUTTON,
    FLOATING_TOOLBAR,
    SIDEBAR,
    BOTTOM_BAR,
    FLOATING_PANEL,
    MINI_BUTTON,
    CUSTOM;
    
    fun getDisplayName(): String = when (this) {
        FLOATING_BUTTON -> AppStringsProvider.current().uiTypeFloatingButton
        FLOATING_TOOLBAR -> AppStringsProvider.current().uiTypeFloatingToolbar
        SIDEBAR -> AppStringsProvider.current().uiTypeSidebar
        BOTTOM_BAR -> AppStringsProvider.current().uiTypeBottomBar
        FLOATING_PANEL -> AppStringsProvider.current().uiTypeFloatingPanel
        MINI_BUTTON -> AppStringsProvider.current().uiTypeMiniButton
        CUSTOM -> AppStringsProvider.current().uiTypeCustom
    }
    
    fun getDescription(): String = when (this) {
        FLOATING_BUTTON -> AppStringsProvider.current().uiTypeFloatingButtonDesc
        FLOATING_TOOLBAR -> AppStringsProvider.current().uiTypeFloatingToolbarDesc
        SIDEBAR -> AppStringsProvider.current().uiTypeSidebarDesc
        BOTTOM_BAR -> AppStringsProvider.current().uiTypeBottomBarDesc
        FLOATING_PANEL -> AppStringsProvider.current().uiTypeFloatingPanelDesc
        MINI_BUTTON -> AppStringsProvider.current().uiTypeMiniButtonDesc
        CUSTOM -> AppStringsProvider.current().uiTypeCustomDesc
    }
    
    fun getIcon(): String = when (this) {
        FLOATING_BUTTON -> "radio_button"
        FLOATING_TOOLBAR -> "wrench"
        SIDEBAR -> "side_navigation"
        BOTTOM_BAR -> "dock_to_bottom"
        FLOATING_PANEL -> "picture_in_picture"
        MINI_BUTTON -> "radio_button"
        CUSTOM -> "palette"
    }
}

enum class UiPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;
    
    fun toCssPosition(): String = when (this) {
        TOP_LEFT -> "top:80px;left:16px"
        TOP_CENTER -> "top:80px;left:50%;transform:translateX(-50%)"
        TOP_RIGHT -> "top:80px;right:16px"
        MIDDLE_LEFT -> "top:50%;left:16px;transform:translateY(-50%)"
        MIDDLE_CENTER -> "top:50%;left:50%;transform:translate(-50%,-50%)"
        MIDDLE_RIGHT -> "top:50%;right:16px;transform:translateY(-50%)"
        BOTTOM_LEFT -> "bottom:80px;left:16px"
        BOTTOM_CENTER -> "bottom:80px;left:50%;transform:translateX(-50%)"
        BOTTOM_RIGHT -> "bottom:80px;right:16px"
    }
    
    fun getDisplayName(): String = when (this) {
        TOP_LEFT -> AppStringsProvider.current().posTopLeft
        TOP_CENTER -> AppStringsProvider.current().posTopCenter
        TOP_RIGHT -> AppStringsProvider.current().posTopRight
        MIDDLE_LEFT -> AppStringsProvider.current().posMiddleLeft
        MIDDLE_CENTER -> AppStringsProvider.current().posMiddleCenter
        MIDDLE_RIGHT -> AppStringsProvider.current().posMiddleRight
        BOTTOM_LEFT -> AppStringsProvider.current().posBottomLeft
        BOTTOM_CENTER -> AppStringsProvider.current().posBottomCenter
        BOTTOM_RIGHT -> AppStringsProvider.current().posBottomRight
    }
}

enum class ToolbarOrientation {
    HORIZONTAL,
    VERTICAL;
    
    fun getDisplayName(): String = when (this) {
        HORIZONTAL -> AppStringsProvider.current().orientationHorizontal
        VERTICAL -> AppStringsProvider.current().orientationVertical
    }
}

enum class SidebarPosition {
    LEFT,
    RIGHT;
    
    fun getDisplayName(): String = when (this) {
        LEFT -> AppStringsProvider.current().sidebarLeft
        RIGHT -> AppStringsProvider.current().sidebarRight
    }
}

data class ToolbarItem(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("icon")
    val icon: String,
    @SerializedName("label")
    val label: String = "",
    @SerializedName("tooltip")
    val tooltip: String = "",
    @SerializedName("action")
    val action: String,
    @SerializedName("showLabel")
    val showLabel: Boolean = false,
    @SerializedName("badge")
    val badge: String = ""
)

data class ModuleUiConfig(
    @SerializedName("type")
    val type: ModuleUiType = ModuleUiType.FLOATING_BUTTON,
    @SerializedName("position")
    val position: UiPosition = UiPosition.BOTTOM_RIGHT,
    @SerializedName("draggable")
    val draggable: Boolean = false,
    @SerializedName("autoHide")
    val autoHide: Boolean = false,
    @SerializedName("autoHideDelay")
    val autoHideDelay: Int = 3000,
    @SerializedName("initiallyHidden")
    val initiallyHidden: Boolean = false,
    @SerializedName("showOnlyOnMatch")
    val showOnlyOnMatch: Boolean = true,
    @SerializedName("buttonSize")
    val buttonSize: Int = 56,
    @SerializedName("buttonColor")
    val buttonColor: String = "",
    @SerializedName("toolbarItems")
    val toolbarItems: List<ToolbarItem> = emptyList(),
    @SerializedName("toolbarOrientation")
    val toolbarOrientation: ToolbarOrientation = ToolbarOrientation.HORIZONTAL,
    @SerializedName("toolbarCollapsible")
    val toolbarCollapsible: Boolean = true,
    @SerializedName("toolbarCollapsed")
    val toolbarCollapsed: Boolean = false,
    @SerializedName("sidebarPosition")
    val sidebarPosition: SidebarPosition = SidebarPosition.RIGHT,
    @SerializedName("sidebarWidth")
    val sidebarWidth: Int = 300,
    @SerializedName("sidebarCollapsed")
    val sidebarCollapsed: Boolean = true,
    @SerializedName("bottomBarHeight")
    val bottomBarHeight: Int = 56,
    @SerializedName("bottomBarTransparent")
    val bottomBarTransparent: Boolean = false,
    @SerializedName("panelWidth")
    val panelWidth: Int = 320,
    @SerializedName("panelHeight")
    val panelHeight: Int = 400,
    @SerializedName("panelResizable")
    val panelResizable: Boolean = true,
    @SerializedName("panelMinimizable")
    val panelMinimizable: Boolean = true,
    @SerializedName("customHtml")
    val customHtml: String = "",
    @SerializedName("customCss")
    val customCss: String = ""
) {
    companion object {
        val DEFAULT = ModuleUiConfig()
        
        val VIDEO_TOOLBAR = ModuleUiConfig(
            type = ModuleUiType.FLOATING_TOOLBAR,
            position = UiPosition.BOTTOM_CENTER,
            toolbarCollapsible = true
        )
        
        val SIDEBAR_DEFAULT = ModuleUiConfig(
            type = ModuleUiType.SIDEBAR,
            sidebarPosition = SidebarPosition.RIGHT,
            sidebarWidth = 300
        )
        
        val BOTTOM_BAR_DEFAULT = ModuleUiConfig(
            type = ModuleUiType.BOTTOM_BAR,
            bottomBarHeight = 56
        )
        
        val MINI_BUTTON_DEFAULT = ModuleUiConfig(
            type = ModuleUiType.MINI_BUTTON,
            position = UiPosition.BOTTOM_RIGHT,
            buttonSize = 40
        )
    }
}

data class ModuleAuthor(
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("qq")
    val qq: String? = null
)

data class ModuleVersion(
    @SerializedName("code")
    val code: Int = 1,
    @SerializedName("name")
    val name: String = "1.0.0",
    @SerializedName("changelog")
    val changelog: String = ""
)

data class UrlMatchRule(
    @SerializedName("pattern")
    val pattern: String,
    @SerializedName("isRegex")
    val isRegex: Boolean = false,
    @SerializedName("exclude")
    val exclude: Boolean = false
)


enum class ModuleSourceType {
    CUSTOM,
    USERSCRIPT,
    CHROME_EXTENSION
}

enum class ModuleRunMode {
    INTERACTIVE,
    AUTO;
    
    fun getDisplayName(): String = when (this) {
        INTERACTIVE -> AppStringsProvider.current().runModeInteractive
        AUTO -> AppStringsProvider.current().runModeAuto
    }
    
    fun getDescription(): String = when (this) {
        INTERACTIVE -> AppStringsProvider.current().runModeInteractiveDesc
        AUTO -> AppStringsProvider.current().runModeAutoDesc
    }
    
    fun getIcon(): String = when (this) {
        INTERACTIVE -> "desktop_windows"
        AUTO -> "bolt"
    }
}

data class ExtensionModule(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("icon")
    val icon: String = "package",
    @SerializedName("category")
    val category: ModuleCategory = ModuleCategory.OTHER,
    @SerializedName("tags")
    val tags: List<String> = emptyList(),
    @SerializedName("version")
    val version: ModuleVersion = ModuleVersion(),
    @SerializedName("author")
    val author: ModuleAuthor? = null,
    @SerializedName("code")
    val code: String = "",
    @SerializedName("cssCode")
    val cssCode: String = "",
    @SerializedName("runAt")
    val runAt: ModuleRunTime = ModuleRunTime.DOCUMENT_END,
    @SerializedName("urlMatches")
    val urlMatches: List<UrlMatchRule> = emptyList(),
    @SerializedName("permissions")
    val permissions: List<ModulePermission> = emptyList(),
    @SerializedName("configItems")
    val configItems: List<ModuleConfigItem> = emptyList(),
    @SerializedName("configValues")
    val configValues: Map<String, String> = emptyMap(),
    @SerializedName("dependencies")
    val dependencies: List<String> = emptyList(),
    @SerializedName("enabled")
    val enabled: Boolean = true,
    @SerializedName("builtIn")
    val builtIn: Boolean = false,
    @SerializedName("uiConfig")
    val uiConfig: ModuleUiConfig = ModuleUiConfig.DEFAULT,
    @SerializedName("runMode")
    val runMode: ModuleRunMode = ModuleRunMode.INTERACTIVE,
    @SerializedName("sourceType")
    val sourceType: ModuleSourceType = ModuleSourceType.CUSTOM,
    @SerializedName("chromeExtId")
    val chromeExtId: String = "",
    @SerializedName("world")
    val world: String = "ISOLATED",
    @SerializedName("backgroundScript")
    val backgroundScript: String = "",
    @SerializedName("gmGrants")
    val gmGrants: List<String> = emptyList(),
    @SerializedName("requireUrls")
    val requireUrls: List<String> = emptyList(),
    @SerializedName("resources")
    val resources: Map<String, String> = emptyMap(),
    @SerializedName("noframes")
    val noframes: Boolean = false,
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson get() = GsonProvider.gson
        private const val SHARE_CODE_PREFIX_V1 = "WTA1:"
        private const val SHARE_CODE_PREFIX_V0 = ""

        fun fromJson(json: String): ExtensionModule? {
            return try {
                gson.fromJson(json, ExtensionModule::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        fun fromShareCode(shareCode: String): ExtensionModule? {
            return try {
                val json = when {
                    shareCode.startsWith(SHARE_CODE_PREFIX_V1) -> {
                        val compressed = android.util.Base64.decode(
                            shareCode.removePrefix(SHARE_CODE_PREFIX_V1), 
                            android.util.Base64.DEFAULT
                        )
                        decompressGzip(compressed)
                    }
                    else -> {
                        String(android.util.Base64.decode(shareCode, android.util.Base64.DEFAULT))
                    }
                }
                fromJson(json)
            } catch (e: Exception) {
                null
            }
        }
        
        private fun decompressGzip(compressed: ByteArray): String {
            java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(compressed)).use { gzip ->
                return gzip.bufferedReader().readText()
            }
        }
        
        private fun compressGzip(data: String): ByteArray {
            val bos = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPOutputStream(bos).use { gzip ->
                gzip.write(data.toByteArray())
            }
            return bos.toByteArray()
        }
    }
    
    fun toJson(): String = gson.toJson(this)
    
    fun toShareCode(): String {
        val compressed = compressGzip(toJson())
        return SHARE_CODE_PREFIX_V1 + android.util.Base64.encodeToString(compressed, android.util.Base64.NO_WRAP)
    }
    
    fun toShareCodeLegacy(): String {
        return android.util.Base64.encodeToString(toJson().toByteArray(), android.util.Base64.NO_WRAP)
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
    
    private fun matchRule(url: String, rule: UrlMatchRule): Boolean {
        return if (rule.isRegex) {
            safeRegexMatch(rule.pattern, url)
        } else {
            // Chrome match pattern: scheme://host/path
            // Special: <all_urls> matches everything, "*" alone matches everything
            val pattern = rule.pattern
            if (pattern == "*" || pattern == "<all_urls>") return true
            
            // Convert Chrome match pattern to regex properly:
            // 1. Escape regex special chars EXCEPT * which is our wildcard
            // 2. Handle *:// scheme (matches http:// and https://)
            // 3. Handle * in host (matches any subdomain)
            // 4. Handle * in path (matches any path)
            val regexPattern = buildString {
                append("^")
                var i = 0
                while (i < pattern.length) {
                    val c = pattern[i]
                    when {
                        // Handle *:// (https?|ftp|file)://
                        c == '*' && pattern.startsWith("*://", i) -> {
                            append("(https?|ftp|file)://")
                            i += 4 // skip *://
                        }
                        // Regular wildcard * .*
                        c == '*' -> {
                            append(".*")
                            i++
                        }
                        // Escape regex special chars
                        c in ".+?^\${}()|[]\\/" -> {
                            append("\\")
                            append(c)
                            i++
                        }
                        else -> {
                            append(c)
                            i++
                        }
                    }
                }
                append("$")
            }
            try {
                Regex(regexPattern, RegexOption.IGNORE_CASE).matches(url)
            } catch (e: Exception) {
                // Fallback: simple contains check
                url.contains(pattern, ignoreCase = true)
            }
        }
    }
    
    @Transient
    @Volatile
    private var _cachedExecutableCode: String? = null
    
    /**
     * when Gson.
     */
    fun generateExecutableCode(): String {
        _cachedExecutableCode?.let { return it }
        val configJson = gson.toJson(configValues)
        val uiConfigJson = gson.toJson(mapOf(
            "type" to uiConfig.type.name,
            "position" to uiConfig.position.name,
            "draggable" to uiConfig.draggable,
            "autoHide" to uiConfig.autoHide,
            "autoHideDelay" to uiConfig.autoHideDelay,
            "initiallyHidden" to uiConfig.initiallyHidden,
            "showOnlyOnMatch" to uiConfig.showOnlyOnMatch,
            "buttonSize" to uiConfig.buttonSize,
            "buttonColor" to uiConfig.buttonColor,
            "toolbarItems" to uiConfig.toolbarItems.map { item ->
                mapOf(
                    "id" to item.id,
                    "icon" to item.icon,
                    "label" to item.label,
                    "tooltip" to item.tooltip,
                    "action" to item.action,
                    "showLabel" to item.showLabel,
                    "badge" to item.badge
                )
            },
            "toolbarOrientation" to uiConfig.toolbarOrientation.name,
            "toolbarCollapsible" to uiConfig.toolbarCollapsible,
            "toolbarCollapsed" to uiConfig.toolbarCollapsed,
            "sidebarPosition" to uiConfig.sidebarPosition.name,
            "sidebarWidth" to uiConfig.sidebarWidth,
            "sidebarCollapsed" to uiConfig.sidebarCollapsed,
            "bottomBarHeight" to uiConfig.bottomBarHeight,
            "bottomBarTransparent" to uiConfig.bottomBarTransparent,
            "panelWidth" to uiConfig.panelWidth,
            "panelHeight" to uiConfig.panelHeight,
            "panelResizable" to uiConfig.panelResizable,
            "panelMinimizable" to uiConfig.panelMinimizable,
            "customHtml" to uiConfig.customHtml,
            "customCss" to uiConfig.customCss
        ))
        val runModeStr = runMode.name
        return """
            (function() {
                'use strict';
                // Moduleconfig.
                const __MODULE_CONFIG__ = $configJson;
                const __MODULE_UI_CONFIG__ = $uiConfigJson;
                const __MODULE_RUN_MODE__ = '$runModeStr';
                const __MODULE_INFO__ = {
                    id: '${id.escapeForJsSingleQuote()}',
                    name: '${name.escapeForJsSingleQuote()}',
                    icon: '${icon.escapeForJsSingleQuote()}',
                    version: '${version.name}',
                    uiConfig: __MODULE_UI_CONFIG__,
                    runMode: __MODULE_RUN_MODE__
                };
                
                // Configure.
                function getConfig(key, defaultValue) {
                    return __MODULE_CONFIG__[key] !== undefined ? __MODULE_CONFIG__[key] : defaultValue;
                }
                
                // CSS.
                ${if (cssCode.isNotBlank()) """
                (function() {
                    const style = document.createElement('style');
                    style.id = 'ext-module-${id}';
                    style.textContent = `${cssCode.escapeForJsTemplate()}`;
                    (document.head || document.documentElement).appendChild(style);
                })();
                """ else ""}
                
                // User.
                try {
                    $code
                } catch(e) {
                    console.error('[ExtModule: ${name.escapeForJsSingleQuote()}] Error:', e);
                }
                
                // to.
                // use use register.
                // in use uiConfig when.
                (function __autoRegister__() {
                    if (typeof __WTA_MODULE_UI__ === 'undefined') {
                        setTimeout(__autoRegister__, 100);
                        return;
                    }
                    // etc after Check when as " ".
                    var panel = window.__WTA_PANEL__;
                    if (!panel || !panel._initialized) {
                        setTimeout(__autoRegister__, 100);
                        return;
                    }
                    // Check use is use uiConfig.
                    if (panel.modules) {
                        var existing = panel.modules.find(function(m) { return m.id === __MODULE_INFO__.id; });
                        if (existing && existing.uiConfig && existing.uiConfig.type) {
                            // use uiConfig.
                            return;
                        }
                    }
                    // use or uiConfig.
                    __WTA_MODULE_UI__.register({
                        id: __MODULE_INFO__.id,
                        name: __MODULE_INFO__.name,
                        icon: __MODULE_INFO__.icon,
                        uiConfig: __MODULE_UI_CONFIG__,
                        runMode: __MODULE_RUN_MODE__
                    });
                })();
            })();
        """.trimIndent().also { _cachedExecutableCode = it }
    }
    
    /**
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) errors.add(AppStringsProvider.current().validateNameEmpty)
        if (code.isBlank() && cssCode.isBlank()) errors.add(AppStringsProvider.current().validateCodeEmpty)
        configItems.forEach { item ->
            if (item.required && configValues[item.key].isNullOrBlank()) {
                errors.add(AppStringsProvider.current().validateConfigRequired.replace("%s", item.name))
            }
        }
        
        return errors
    }
}

data class ModulePackage(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("author")
    val author: ModuleAuthor? = null,
    @SerializedName("modules")
    val modules: List<ExtensionModule>,
    @SerializedName("version")
    val version: String = "1.0.0",
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson get() = GsonProvider.gson
        
        fun fromJson(json: String): ModulePackage? {
            return try {
                gson.fromJson(json, ModulePackage::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun toJson(): String = gson.toJson(this)
}

object ModuleCategoryGroups {
    val groups = listOf(
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupContent,
            icon = "edit_note",
            categories = listOf(
                ModuleCategory.CONTENT_FILTER,
                ModuleCategory.CONTENT_ENHANCE,
                ModuleCategory.READING
            )
        ),
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupAppearance,
            icon = "palette",
            categories = listOf(
                ModuleCategory.STYLE_MODIFIER,
                ModuleCategory.THEME
            )
        ),
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupFunction,
            icon = "bolt",
            categories = listOf(
                ModuleCategory.FUNCTION_ENHANCE,
                ModuleCategory.AUTOMATION,
                ModuleCategory.NAVIGATION,
                ModuleCategory.INTERACTION
            )
        ),
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupData,
            icon = "analytics",
            categories = listOf(
                ModuleCategory.DATA_EXTRACT,
                ModuleCategory.DATA_SAVE,
                ModuleCategory.TRANSLATE
            )
        ),
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupMedia,
            icon = "movie",
            categories = listOf(
                ModuleCategory.MEDIA,
                ModuleCategory.VIDEO,
                ModuleCategory.IMAGE,
                ModuleCategory.AUDIO
            )
        ),
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupSecurity,
            icon = "lock",
            categories = listOf(
                ModuleCategory.SECURITY,
                ModuleCategory.ANTI_TRACKING
            )
        ),
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupLife,
            icon = "build",
            categories = listOf(
                ModuleCategory.SOCIAL,
                ModuleCategory.SHOPPING,
                ModuleCategory.ACCESSIBILITY
            )
        ),
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupDeveloper,
            icon = "computer",
            categories = listOf(
                ModuleCategory.DEVELOPER
            )
        ),
        CategoryGroup(
            name = AppStringsProvider.current().categoryGroupOther,
            icon = "package",
            categories = listOf(
                ModuleCategory.OTHER
            )
        )
    )
    fun getGroupForCategory(category: ModuleCategory): CategoryGroup? {
        return groups.find { it.categories.contains(category) }
    }
}

data class CategoryGroup(
    val name: String,
    val icon: String,
    val categories: List<ModuleCategory>
)

object ModulePermissionGroups {
    
    val groups = listOf(
        PermissionGroup(
            name = AppStringsProvider.current().permGroupBasic,
            permissions = listOf(
                ModulePermission.DOM_ACCESS,
                ModulePermission.DOM_OBSERVE,
                ModulePermission.CSS_INJECT
            )
        ),
        PermissionGroup(
            name = AppStringsProvider.current().permGroupStorage,
            permissions = listOf(
                ModulePermission.STORAGE,
                ModulePermission.COOKIE,
                ModulePermission.INDEXED_DB,
                ModulePermission.CACHE
            )
        ),
        PermissionGroup(
            name = AppStringsProvider.current().permGroupNetwork,
            permissions = listOf(
                ModulePermission.NETWORK,
                ModulePermission.WEBSOCKET,
                ModulePermission.FETCH_INTERCEPT
            )
        ),
        PermissionGroup(
            name = AppStringsProvider.current().permGroupInteraction,
            permissions = listOf(
                ModulePermission.CLIPBOARD,
                ModulePermission.NOTIFICATION,
                ModulePermission.ALERT,
                ModulePermission.KEYBOARD,
                ModulePermission.MOUSE,
                ModulePermission.TOUCH
            )
        ),
        PermissionGroup(
            name = AppStringsProvider.current().permGroupDevice,
            permissions = listOf(
                ModulePermission.LOCATION,
                ModulePermission.CAMERA,
                ModulePermission.MICROPHONE,
                ModulePermission.DEVICE_INFO
            )
        ),
        PermissionGroup(
            name = AppStringsProvider.current().permGroupMediaPerm,
            permissions = listOf(
                ModulePermission.MEDIA,
                ModulePermission.FULLSCREEN,
                ModulePermission.PICTURE_IN_PICTURE,
                ModulePermission.SCREEN_CAPTURE
            )
        ),
        PermissionGroup(
            name = AppStringsProvider.current().permGroupFile,
            permissions = listOf(
                ModulePermission.DOWNLOAD,
                ModulePermission.FILE_ACCESS
            )
        ),
        PermissionGroup(
            name = AppStringsProvider.current().permGroupAdvanced,
            permissions = listOf(
                ModulePermission.EVAL,
                ModulePermission.IFRAME,
                ModulePermission.WINDOW_OPEN,
                ModulePermission.HISTORY,
                ModulePermission.NAVIGATION
            )
        )
    )
}

data class PermissionGroup(
    val name: String,
    val permissions: List<ModulePermission>
)

object ModulePresets {
    fun createElementBlocker(
        name: String,
        selectors: List<String>,
        description: String = AppStringsProvider.current().presetBlockElements
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "block",
            category = ModuleCategory.CONTENT_FILTER,
            tags = listOf(AppStringsProvider.current().tagBlock, AppStringsProvider.current().tagHideElement),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOM_OBSERVE),
            code = """
                const selectors = ${selectors.joinToString(",", "[", "]") { "\"$it\"" }};
                function hide() {
                    selectors.forEach(s => {
                        document.querySelectorAll(s).forEach(el => el.style.display = 'none');
                    });
                }
                hide();
                new MutationObserver(hide).observe(document.body, { childList: true, subtree: true });
            """.trimIndent()
        )
    }
    
    fun createStyleInjector(
        name: String,
        cssCode: String,
        description: String = AppStringsProvider.current().presetInjectStyle
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "palette",
            category = ModuleCategory.STYLE_MODIFIER,
            tags = listOf(AppStringsProvider.current().tagStyleCss, "CSS"),
            runAt = ModuleRunTime.DOCUMENT_START,
            permissions = listOf(ModulePermission.CSS_INJECT),
            cssCode = cssCode
        )
    }
    
    fun createAutoClicker(
        name: String,
        selector: String,
        delay: Int = 1000,
        description: String = AppStringsProvider.current().presetAutoClick
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "mouse",
            category = ModuleCategory.AUTOMATION,
            tags = listOf(AppStringsProvider.current().tagAuto, AppStringsProvider.current().tagClickAction),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            code = """
                setTimeout(() => {
                    const el = document.querySelector('$selector');
                    if (el) el.click();
                }, $delay);
            """.trimIndent()
        )
    }
    
    fun createFloatingButton(
        name: String,
        buttonText: String,
        onClick: String,
        position: String = "bottom-right",
        description: String = AppStringsProvider.current().presetFloatingButton
    ): ExtensionModule {
        val positionStyle = when (position) {
            "bottom-left" -> "bottom: 80px; left: 20px;"
            "top-right" -> "top: 80px; right: 20px;"
            "top-left" -> "top: 80px; left: 20px;"
            else -> "bottom: 80px; right: 20px;"
        }
        
        return ExtensionModule(
            name = name,
            description = description,
            icon = "radio_button",
            category = ModuleCategory.FUNCTION_ENHANCE,
            tags = listOf(AppStringsProvider.current().tagButton, AppStringsProvider.current().tagFloatingWidget),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            code = """
                const btn = document.createElement('div');
                btn.textContent = '$buttonText';
                btn.style.cssText = 'position:fixed;$positionStyle;z-index:99999;padding:12px 20px;background:rgba(0,0,0,0.8);color:white;border-radius:25px;cursor:pointer;font-size:14px;box-shadow:0 2px 10px rgba(0,0,0,0.3);';
                btn.onclick = () => { $onClick };
                document.body.appendChild(btn);
            """.trimIndent()
        )
    }
}
