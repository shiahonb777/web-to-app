package com.webtoapp.core.extension

import com.google.gson.annotations.SerializedName
import com.webtoapp.core.i18n.Strings
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

/**
 * 扩展模块分类
 */
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
        CONTENT_FILTER -> Strings.catContentFilter
        CONTENT_ENHANCE -> Strings.catContentEnhance
        STYLE_MODIFIER -> Strings.catStyleModifier
        THEME -> Strings.catTheme
        FUNCTION_ENHANCE -> Strings.catFunctionEnhance
        AUTOMATION -> Strings.catAutomation
        NAVIGATION -> Strings.catNavigation
        DATA_EXTRACT -> Strings.catDataExtract
        DATA_SAVE -> Strings.catDataSave
        INTERACTION -> Strings.catInteraction
        ACCESSIBILITY -> Strings.catAccessibility
        MEDIA -> Strings.catMedia
        VIDEO -> Strings.catVideo
        IMAGE -> Strings.catImage
        AUDIO -> Strings.catAudio
        SECURITY -> Strings.catSecurity
        ANTI_TRACKING -> Strings.catAntiTracking
        SOCIAL -> Strings.catSocial
        SHOPPING -> Strings.catShopping
        READING -> Strings.catReading
        TRANSLATE -> Strings.catTranslate
        DEVELOPER -> Strings.catDeveloper
        OTHER -> Strings.catOther
    }
    
    fun getDescription(): String = when (this) {
        CONTENT_FILTER -> Strings.catContentFilterDesc
        CONTENT_ENHANCE -> Strings.catContentEnhanceDesc
        STYLE_MODIFIER -> Strings.catStyleModifierDesc
        THEME -> Strings.catThemeDesc
        FUNCTION_ENHANCE -> Strings.catFunctionEnhanceDesc
        AUTOMATION -> Strings.catAutomationDesc
        NAVIGATION -> Strings.catNavigationDesc
        DATA_EXTRACT -> Strings.catDataExtractDesc
        DATA_SAVE -> Strings.catDataSaveDesc
        INTERACTION -> Strings.catInteractionDesc
        ACCESSIBILITY -> Strings.catAccessibilityDesc
        MEDIA -> Strings.catMediaDesc
        VIDEO -> Strings.catVideoDesc
        IMAGE -> Strings.catImageDesc
        AUDIO -> Strings.catAudioDesc
        SECURITY -> Strings.catSecurityDesc
        ANTI_TRACKING -> Strings.catAntiTrackingDesc
        SOCIAL -> Strings.catSocialDesc
        SHOPPING -> Strings.catShoppingDesc
        READING -> Strings.catReadingDesc
        TRANSLATE -> Strings.catTranslateDesc
        DEVELOPER -> Strings.catDeveloperDesc
        OTHER -> Strings.catOtherDesc
    }
}

/**
 * 模块执行时机
 */
enum class ModuleRunTime(val jsEvent: String) {
    DOCUMENT_START(""),
    DOCUMENT_END("DOMContentLoaded"),
    DOCUMENT_IDLE("load"),
    CONTEXT_MENU("contextmenu"),
    BEFORE_UNLOAD("beforeunload");
    
    fun getDisplayName(): String = when (this) {
        DOCUMENT_START -> Strings.runTimeDocStart
        DOCUMENT_END -> Strings.runTimeDocEnd
        DOCUMENT_IDLE -> Strings.runTimeDocIdle
        CONTEXT_MENU -> Strings.runTimeContextMenu
        BEFORE_UNLOAD -> Strings.runTimeBeforeUnload
    }
    
    fun getDescription(): String = when (this) {
        DOCUMENT_START -> Strings.runTimeDocStartDesc
        DOCUMENT_END -> Strings.runTimeDocEndDesc
        DOCUMENT_IDLE -> Strings.runTimeDocIdleDesc
        CONTEXT_MENU -> Strings.runTimeContextMenuDesc
        BEFORE_UNLOAD -> Strings.runTimeBeforeUnloadDesc
    }
}

/**
 * 模块触发条件
 */
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
        AUTO -> Strings.triggerAuto
        MANUAL -> Strings.triggerManual
        INTERVAL -> Strings.triggerInterval
        MUTATION -> Strings.triggerMutation
        SCROLL -> Strings.triggerScroll
        CLICK -> Strings.triggerClick
        HOVER -> Strings.triggerHover
        FOCUS -> Strings.triggerFocus
        INPUT -> Strings.triggerInput
        VISIBILITY -> Strings.triggerVisibility
    }
    
    val description: String get() = when (this) {
        AUTO -> Strings.triggerAutoDesc
        MANUAL -> Strings.triggerManualDesc
        INTERVAL -> Strings.triggerIntervalDesc
        MUTATION -> Strings.triggerMutationDesc
        SCROLL -> Strings.triggerScrollDesc
        CLICK -> Strings.triggerClickDesc
        HOVER -> Strings.triggerHoverDesc
        FOCUS -> Strings.triggerFocusDesc
        INPUT -> Strings.triggerInputDesc
        VISIBILITY -> Strings.triggerVisibilityDesc
    }
}

/**
 * 模块权限
 */
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
        DOM_ACCESS -> Strings.permDomAccess
        DOM_OBSERVE -> Strings.permDomObserve
        CSS_INJECT -> Strings.permCssInject
        STORAGE -> Strings.permStorage
        COOKIE -> Strings.permCookie
        INDEXED_DB -> Strings.permIndexedDb
        CACHE -> Strings.permCache
        NETWORK -> Strings.permNetwork
        WEBSOCKET -> Strings.permWebsocket
        FETCH_INTERCEPT -> Strings.permFetchIntercept
        CLIPBOARD -> Strings.permClipboard
        NOTIFICATION -> Strings.permNotification
        ALERT -> Strings.permAlert
        KEYBOARD -> Strings.permKeyboard
        MOUSE -> Strings.permMouse
        TOUCH -> Strings.permTouch
        LOCATION -> Strings.permLocation
        CAMERA -> Strings.permCamera
        MICROPHONE -> Strings.permMicrophone
        DEVICE_INFO -> Strings.permDeviceInfo
        MEDIA -> Strings.permMedia
        FULLSCREEN -> Strings.permFullscreen
        PICTURE_IN_PICTURE -> Strings.permPip
        SCREEN_CAPTURE -> Strings.permScreenCapture
        DOWNLOAD -> Strings.permDownload
        FILE_ACCESS -> Strings.permFileAccess
        EVAL -> Strings.permEval
        IFRAME -> Strings.permIframe
        WINDOW_OPEN -> Strings.permWindowOpen
        HISTORY -> Strings.permHistory
        NAVIGATION -> Strings.permNavigation
    }
    
    val description: String get() = when (this) {
        DOM_ACCESS -> Strings.permDomAccessDesc
        DOM_OBSERVE -> Strings.permDomObserveDesc
        CSS_INJECT -> Strings.permCssInjectDesc
        STORAGE -> Strings.permStorageDesc
        COOKIE -> Strings.permCookieDesc
        INDEXED_DB -> Strings.permIndexedDbDesc
        CACHE -> Strings.permCacheDesc
        NETWORK -> Strings.permNetworkDesc
        WEBSOCKET -> Strings.permWebsocketDesc
        FETCH_INTERCEPT -> Strings.permFetchInterceptDesc
        CLIPBOARD -> Strings.permClipboardDesc
        NOTIFICATION -> Strings.permNotificationDesc
        ALERT -> Strings.permAlertDesc
        KEYBOARD -> Strings.permKeyboardDesc
        MOUSE -> Strings.permMouseDesc
        TOUCH -> Strings.permTouchDesc
        LOCATION -> Strings.permLocationDesc
        CAMERA -> Strings.permCameraDesc
        MICROPHONE -> Strings.permMicrophoneDesc
        DEVICE_INFO -> Strings.permDeviceInfoDesc
        MEDIA -> Strings.permMediaDesc
        FULLSCREEN -> Strings.permFullscreenDesc
        PICTURE_IN_PICTURE -> Strings.permPipDesc
        SCREEN_CAPTURE -> Strings.permScreenCaptureDesc
        DOWNLOAD -> Strings.permDownloadDesc
        FILE_ACCESS -> Strings.permFileAccessDesc
        EVAL -> Strings.permEvalDesc
        IFRAME -> Strings.permIframeDesc
        WINDOW_OPEN -> Strings.permWindowOpenDesc
        HISTORY -> Strings.permHistoryDesc
        NAVIGATION -> Strings.permNavigationDesc
    }
}

/**
 * 模块配置项类型
 */
enum class ConfigItemType {
    TEXT, TEXTAREA, NUMBER, BOOLEAN,
    SELECT, MULTI_SELECT, RADIO, CHECKBOX,
    COLOR, URL, EMAIL, PASSWORD,
    REGEX, CSS_SELECTOR, JAVASCRIPT, JSON,
    RANGE, DATE, TIME, DATETIME,
    FILE, IMAGE;
    
    val displayName: String get() = when (this) {
        TEXT -> Strings.configTypeText
        TEXTAREA -> Strings.configTypeTextarea
        NUMBER -> Strings.configTypeNumber
        BOOLEAN -> Strings.configTypeBoolean
        SELECT -> Strings.configTypeSelect
        MULTI_SELECT -> Strings.configTypeMultiSelect
        RADIO -> Strings.configTypeRadio
        CHECKBOX -> Strings.configTypeCheckbox
        COLOR -> Strings.configTypeColor
        URL -> Strings.configTypeUrl
        EMAIL -> Strings.configTypeEmail
        PASSWORD -> Strings.configTypePassword
        REGEX -> Strings.configTypeRegex
        CSS_SELECTOR -> Strings.configTypeCssSelector
        JAVASCRIPT -> Strings.configTypeJavascript
        JSON -> Strings.configTypeJson
        RANGE -> Strings.configTypeRange
        DATE -> Strings.configTypeDate
        TIME -> Strings.configTypeTime
        DATETIME -> Strings.configTypeDatetime
        FILE -> Strings.configTypeFile
        IMAGE -> Strings.configTypeImage
    }
    
    val description: String get() = when (this) {
        TEXT -> Strings.configTypeTextDesc
        TEXTAREA -> Strings.configTypeTextareaDesc
        NUMBER -> Strings.configTypeNumberDesc
        BOOLEAN -> Strings.configTypeBooleanDesc
        SELECT -> Strings.configTypeSelectDesc
        MULTI_SELECT -> Strings.configTypeMultiSelectDesc
        RADIO -> Strings.configTypeRadioDesc
        CHECKBOX -> Strings.configTypeCheckboxDesc
        COLOR -> Strings.configTypeColorDesc
        URL -> Strings.configTypeUrlDesc
        EMAIL -> Strings.configTypeEmailDesc
        PASSWORD -> Strings.configTypePasswordDesc
        REGEX -> Strings.configTypeRegexDesc
        CSS_SELECTOR -> Strings.configTypeCssSelectorDesc
        JAVASCRIPT -> Strings.configTypeJavascriptDesc
        JSON -> Strings.configTypeJsonDesc
        RANGE -> Strings.configTypeRangeDesc
        DATE -> Strings.configTypeDateDesc
        TIME -> Strings.configTypeTimeDesc
        DATETIME -> Strings.configTypeDatetimeDesc
        FILE -> Strings.configTypeFileDesc
        IMAGE -> Strings.configTypeImageDesc
    }
}

/**
 * 模块配置项定义
 */
data class ModuleConfigItem(
    @SerializedName("key")
    val key: String,                              // Configure键名
    @SerializedName("name")
    val name: String,                             // Show名称
    @SerializedName("description")
    val description: String = "",                 // Configure说明
    @SerializedName("type")
    val type: ConfigItemType = ConfigItemType.TEXT, // Configure类型
    @SerializedName("defaultValue")
    val defaultValue: String = "",                // Default值
    @SerializedName("options")
    val options: List<String> = emptyList(),      // Options列表（SELECT/MULTI_SELECT 类型）
    @SerializedName("required")
    val required: Boolean = false,                // Yes否必填
    @SerializedName("placeholder")
    val placeholder: String = "",                 // 占位提示
    @SerializedName("validation")
    val validation: String? = null                // Verify正则（可选）
)

/**
 * 模块 UI 类型
 * 定义模块在网页中的展示形式
 */
enum class ModuleUiType {
    FLOATING_BUTTON,    // 悬浮按钮（默认，点击显示面板）
    FLOATING_TOOLBAR,   // 悬浮工具栏（多个按钮水平/垂直排列）
    SIDEBAR,            // 侧边栏（可展开/收起的面板）
    BOTTOM_BAR,         // 底部操作栏（固定在底部）
    FLOATING_PANEL,     // 可拖动悬浮面板
    MINI_BUTTON,        // 迷你悬浮按钮（更小，不显示模块数量）
    CUSTOM;             // 完全自定义（使用 customHtml）
    
    fun getDisplayName(): String = when (this) {
        FLOATING_BUTTON -> Strings.uiTypeFloatingButton
        FLOATING_TOOLBAR -> Strings.uiTypeFloatingToolbar
        SIDEBAR -> Strings.uiTypeSidebar
        BOTTOM_BAR -> Strings.uiTypeBottomBar
        FLOATING_PANEL -> Strings.uiTypeFloatingPanel
        MINI_BUTTON -> Strings.uiTypeMiniButton
        CUSTOM -> Strings.uiTypeCustom
    }
    
    fun getDescription(): String = when (this) {
        FLOATING_BUTTON -> Strings.uiTypeFloatingButtonDesc
        FLOATING_TOOLBAR -> Strings.uiTypeFloatingToolbarDesc
        SIDEBAR -> Strings.uiTypeSidebarDesc
        BOTTOM_BAR -> Strings.uiTypeBottomBarDesc
        FLOATING_PANEL -> Strings.uiTypeFloatingPanelDesc
        MINI_BUTTON -> Strings.uiTypeMiniButtonDesc
        CUSTOM -> Strings.uiTypeCustomDesc
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

/**
 * UI 位置
 */
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
        TOP_LEFT -> Strings.posTopLeft
        TOP_CENTER -> Strings.posTopCenter
        TOP_RIGHT -> Strings.posTopRight
        MIDDLE_LEFT -> Strings.posMiddleLeft
        MIDDLE_CENTER -> Strings.posMiddleCenter
        MIDDLE_RIGHT -> Strings.posMiddleRight
        BOTTOM_LEFT -> Strings.posBottomLeft
        BOTTOM_CENTER -> Strings.posBottomCenter
        BOTTOM_RIGHT -> Strings.posBottomRight
    }
}

/**
 * 工具栏方向
 */
enum class ToolbarOrientation {
    HORIZONTAL,  // 水平排列
    VERTICAL;    // 垂直排列
    
    fun getDisplayName(): String = when (this) {
        HORIZONTAL -> Strings.orientationHorizontal
        VERTICAL -> Strings.orientationVertical
    }
}

/**
 * 侧边栏位置
 */
enum class SidebarPosition {
    LEFT,   // 左侧
    RIGHT;  // 右侧
    
    fun getDisplayName(): String = when (this) {
        LEFT -> Strings.sidebarLeft
        RIGHT -> Strings.sidebarRight
    }
}

/**
 * 工具栏项 - 定义工具栏中的单个按钮
 */
data class ToolbarItem(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),  // 唯一ID
    @SerializedName("icon")
    val icon: String,                               // Icon（emoji）
    @SerializedName("label")
    val label: String = "",                         // 标签文字（可选）
    @SerializedName("tooltip")
    val tooltip: String = "",                       // 悬停提示
    @SerializedName("action")
    val action: String,                             // 点击时执行的 JS 代码或函数名
    @SerializedName("showLabel")
    val showLabel: Boolean = false,                 // Yes否显示标签
    @SerializedName("badge")
    val badge: String = ""                          // 徽章文字（如数量）
)

/**
 * 模块 UI 配置
 * 定义模块的 UI 样式和行为
 */
data class ModuleUiConfig(
    @SerializedName("type")
    val type: ModuleUiType = ModuleUiType.FLOATING_BUTTON,
    
    // 通用配置
    @SerializedName("position")
    val position: UiPosition = UiPosition.BOTTOM_RIGHT,  // UI 位置
    @SerializedName("draggable")
    val draggable: Boolean = false,                      // Yes否可拖动
    @SerializedName("autoHide")
    val autoHide: Boolean = false,                       // 滚动时自动隐藏
    @SerializedName("autoHideDelay")
    val autoHideDelay: Int = 3000,                       // Auto隐藏延迟（毫秒）
    @SerializedName("initiallyHidden")
    val initiallyHidden: Boolean = false,                // 初始是否隐藏
    @SerializedName("showOnlyOnMatch")
    val showOnlyOnMatch: Boolean = true,                 // 仅在匹配 URL 时显示
    
    // 悬浮按钮配置
    @SerializedName("buttonSize")
    val buttonSize: Int = 56,                            // 按钮大小（dp）
    @SerializedName("buttonColor")
    val buttonColor: String = "",                        // 按钮颜色（空则使用主题色）
    
    // Toolbar配置
    @SerializedName("toolbarItems")
    val toolbarItems: List<ToolbarItem> = emptyList(),   // Toolbar按钮列表
    @SerializedName("toolbarOrientation")
    val toolbarOrientation: ToolbarOrientation = ToolbarOrientation.HORIZONTAL,
    @SerializedName("toolbarCollapsible")
    val toolbarCollapsible: Boolean = true,              // Yes否可折叠
    @SerializedName("toolbarCollapsed")
    val toolbarCollapsed: Boolean = false,               // 初始是否折叠
    
    // 侧边栏配置
    @SerializedName("sidebarPosition")
    val sidebarPosition: SidebarPosition = SidebarPosition.RIGHT,
    @SerializedName("sidebarWidth")
    val sidebarWidth: Int = 300,                         // 侧边栏宽度（px）
    @SerializedName("sidebarCollapsed")
    val sidebarCollapsed: Boolean = true,                // 初始是否折叠
    
    // 底部栏配置
    @SerializedName("bottomBarHeight")
    val bottomBarHeight: Int = 56,                       // 底部栏高度（px）
    @SerializedName("bottomBarTransparent")
    val bottomBarTransparent: Boolean = false,           // Yes否半透明背景
    
    // 悬浮面板配置
    @SerializedName("panelWidth")
    val panelWidth: Int = 320,                           // 面板宽度（px）
    @SerializedName("panelHeight")
    val panelHeight: Int = 400,                          // 面板高度（px）
    @SerializedName("panelResizable")
    val panelResizable: Boolean = true,                  // Yes否可调整大小
    @SerializedName("panelMinimizable")
    val panelMinimizable: Boolean = true,                // Yes否可最小化
    
    // Custom HTML（仅 CUSTOM 类型）
    @SerializedName("customHtml")
    val customHtml: String = "",                         // Custom HTML 模板
    @SerializedName("customCss")
    val customCss: String = ""                           // Custom CSS
) {
    companion object {
        /** 默认悬浮按钮配置 */
        val DEFAULT = ModuleUiConfig()
        
        /** 视频增强工具栏预设 */
        val VIDEO_TOOLBAR = ModuleUiConfig(
            type = ModuleUiType.FLOATING_TOOLBAR,
            position = UiPosition.BOTTOM_CENTER,
            toolbarCollapsible = true
        )
        
        /** 侧边栏预设 */
        val SIDEBAR_DEFAULT = ModuleUiConfig(
            type = ModuleUiType.SIDEBAR,
            sidebarPosition = SidebarPosition.RIGHT,
            sidebarWidth = 300
        )
        
        /** 底部栏预设 */
        val BOTTOM_BAR_DEFAULT = ModuleUiConfig(
            type = ModuleUiType.BOTTOM_BAR,
            bottomBarHeight = 56
        )
        
        /** 迷你按钮预设 */
        val MINI_BUTTON_DEFAULT = ModuleUiConfig(
            type = ModuleUiType.MINI_BUTTON,
            position = UiPosition.BOTTOM_RIGHT,
            buttonSize = 40
        )
    }
}

/**
 * 模块作者信息
 */
data class ModuleAuthor(
    @SerializedName("name")
    val name: String,                             // 作者名称
    @SerializedName("email")
    val email: String? = null,                    // 邮箱
    @SerializedName("url")
    val url: String? = null,                      // 主页
    @SerializedName("qq")
    val qq: String? = null                        // QQ
)

/**
 * 模块版本信息
 */
data class ModuleVersion(
    @SerializedName("code")
    val code: Int = 1,                            // Version号
    @SerializedName("name")
    val name: String = "1.0.0",                   // Version名
    @SerializedName("changelog")
    val changelog: String = ""                    // Update日志
)

/**
 * URL 匹配规则
 */
data class UrlMatchRule(
    @SerializedName("pattern")
    val pattern: String,                          // 匹配模式（支持通配符和正则）
    @SerializedName("isRegex")
    val isRegex: Boolean = false,                 // Yes否为正则表达式
    @SerializedName("exclude")
    val exclude: Boolean = false                  // Yes否为排除规则
)


/**
 * 模块来源类型
 */
enum class ModuleSourceType {
    CUSTOM,              // 用户自建 / AI 生成
    USERSCRIPT,          // 油猴脚本 (.user.js)
    CHROME_EXTENSION     // Chrome 扩展 (manifest.json)
}

/**
 * 模块运行方式
 * 定义模块是否需要 UI 交互
 */
enum class ModuleRunMode {
    INTERACTIVE,         // 交互模式：可在管理面板中操作简单UI，也可启动独立窗口使用完整UI
    AUTO;                // 自动模式：自动加载运行，无UI操作界面，不可交互
    
    fun getDisplayName(): String = when (this) {
        INTERACTIVE -> Strings.runModeInteractive
        AUTO -> Strings.runModeAuto
    }
    
    fun getDescription(): String = when (this) {
        INTERACTIVE -> Strings.runModeInteractiveDesc
        AUTO -> Strings.runModeAutoDesc
    }
    
    fun getIcon(): String = when (this) {
        INTERACTIVE -> "desktop_windows"
        AUTO -> "bolt"
    }
}

/**
 * 扩展模块 - 核心数据模型
 * 
 * 这是整个扩展系统的核心，定义了一个可复用、可分享的功能模块
 */
data class ExtensionModule(
    // 基本标识
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),  // 唯一ID
    @SerializedName("name")
    val name: String,                               // Module名称
    @SerializedName("description")
    val description: String = "",                   // Module描述
    @SerializedName("icon")
    val icon: String = "package",                        // Module图标（icon ID）
    
    // 分类与标签
    @SerializedName("category")
    val category: ModuleCategory = ModuleCategory.OTHER, // Module分类
    @SerializedName("tags")
    val tags: List<String> = emptyList(),           // 标签列表
    
    // Version与作者
    @SerializedName("version")
    val version: ModuleVersion = ModuleVersion(),   // Version信息
    @SerializedName("author")
    val author: ModuleAuthor? = null,               // 作者信息
    
    // 代码内容
    @SerializedName("code")
    val code: String = "",                          // JavaScript 代码
    @SerializedName("cssCode")
    val cssCode: String = "",                       // CSS 代码（可选）
    
    // Execute配置
    @SerializedName("runAt")
    val runAt: ModuleRunTime = ModuleRunTime.DOCUMENT_END, // Execute时机
    @SerializedName("urlMatches")
    val urlMatches: List<UrlMatchRule> = emptyList(), // URL 匹配规则（空则匹配所有）
    
    // Permission声明
    @SerializedName("permissions")
    val permissions: List<ModulePermission> = emptyList(), // 所需权限
    
    // Configure项定义
    @SerializedName("configItems")
    val configItems: List<ModuleConfigItem> = emptyList(), // User可配置项
    @SerializedName("configValues")
    val configValues: Map<String, String> = emptyMap(),    // User配置值
    
    // 依赖关系
    @SerializedName("dependencies")
    val dependencies: List<String> = emptyList(),   // 依赖的其他模块ID
    
    // 状态
    @SerializedName("enabled")
    val enabled: Boolean = true,                    // Yes否启用
    @SerializedName("builtIn")
    val builtIn: Boolean = false,                   // Yes否为内置模块
    
    // UI 配置
    @SerializedName("uiConfig")
    val uiConfig: ModuleUiConfig = ModuleUiConfig.DEFAULT, // UI 类型和配置
    
    // 运行方式（必填）
    @SerializedName("runMode")
    val runMode: ModuleRunMode = ModuleRunMode.INTERACTIVE, // 运行方式：交互模式或自动模式
    
    // 模块来源
    @SerializedName("sourceType")
    val sourceType: ModuleSourceType = ModuleSourceType.CUSTOM,
    
    // Chrome 扩展特有字段
    @SerializedName("chromeExtId")
    val chromeExtId: String = "",                        // Chrome 扩展标识符（用于 storage 命名空间隔离和 runtime.id）
    @SerializedName("world")
    val world: String = "ISOLATED",                      // 内容脚本执行世界: "ISOLATED" 或 "MAIN"
    @SerializedName("backgroundScript")
    val backgroundScript: String = "",                   // Background script 相对路径（如 "background/index.js"）
    
    // 油猴脚本特有字段
    @SerializedName("gmGrants")
    val gmGrants: List<String> = emptyList(),          // @grant 声明的 GM_* API 列表
    @SerializedName("requireUrls")
    val requireUrls: List<String> = emptyList(),        // @require 外部依赖 URL
    @SerializedName("resources")
    val resources: Map<String, String> = emptyMap(),    // @resource 命名资源 (name -> url)
    @SerializedName("noframes")
    val noframes: Boolean = false,                      // @noframes 仅主框架执行
    
    // 元数据
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson get() = GsonProvider.gson
        
        // 分享码前缀标识
        private const val SHARE_CODE_PREFIX_V1 = "WTA1:"  // Compression版
        private const val SHARE_CODE_PREFIX_V0 = ""       // 旧版（无前缀）
        
        /**
         * 从 JSON 字符串解析模块
         */
        fun fromJson(json: String): ExtensionModule? {
            return try {
                gson.fromJson(json, ExtensionModule::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * 从分享码解析模块
         * 支持新版压缩格式和旧版未压缩格式
         */
        fun fromShareCode(shareCode: String): ExtensionModule? {
            return try {
                val json = when {
                    // V1: 压缩版（GZIP + Base64）
                    shareCode.startsWith(SHARE_CODE_PREFIX_V1) -> {
                        val compressed = android.util.Base64.decode(
                            shareCode.removePrefix(SHARE_CODE_PREFIX_V1), 
                            android.util.Base64.DEFAULT
                        )
                        decompressGzip(compressed)
                    }
                    // V0: 旧版未压缩（纯 Base64）
                    else -> {
                        String(android.util.Base64.decode(shareCode, android.util.Base64.DEFAULT))
                    }
                }
                fromJson(json)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * GZIP 解压缩
         */
        private fun decompressGzip(compressed: ByteArray): String {
            java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(compressed)).use { gzip ->
                return gzip.bufferedReader().readText()
            }
        }
        
        /**
         * GZIP 压缩
         */
        private fun compressGzip(data: String): ByteArray {
            val bos = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPOutputStream(bos).use { gzip ->
                gzip.write(data.toByteArray())
            }
            return bos.toByteArray()
        }
    }
    
    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String = gson.toJson(this)
    
    /**
     * 生成分享码（压缩版）
     * 使用 GZIP 压缩后 Base64 编码，通常可缩短 50-70%
     */
    fun toShareCode(): String {
        val compressed = compressGzip(toJson())
        return SHARE_CODE_PREFIX_V1 + android.util.Base64.encodeToString(compressed, android.util.Base64.NO_WRAP)
    }
    
    /**
     * 生成旧版分享码（未压缩，用于兼容性）
     */
    fun toShareCodeLegacy(): String {
        return android.util.Base64.encodeToString(toJson().toByteArray(), android.util.Base64.NO_WRAP)
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
                        // Handle *:// → (https?|ftp|file)://
                        c == '*' && pattern.startsWith("*://", i) -> {
                            append("(https?|ftp|file)://")
                            i += 4 // skip *://
                        }
                        // Regular wildcard * → .*
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
    
    // Cache generated executable code per module instance (data class is immutable)
    @Transient
    @Volatile
    private var _cachedExecutableCode: String? = null
    
    /**
     * 生成最终执行的代码（注入配置值）
     * 结果会缓存，避免每次页面加载时重复 Gson 序列化和字符串拼接
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
                // Module配置
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
                
                // 自动注册模块到面板系统（使用配置的 uiConfig）
                // 如果用户代码已经调用了 register，面板系统会更新已有注册
                // 注意：只在用户代码未传递 uiConfig 时才补充注册
                (function __autoRegister__() {
                    if (typeof __WTA_MODULE_UI__ === 'undefined') {
                        setTimeout(__autoRegister__, 100);
                        return;
                    }
                    // 等待面板完全初始化后再检查，避免面板未就绪时误判为"未注册"
                    var panel = window.__WTA_PANEL__;
                    if (!panel || !panel._initialized) {
                        setTimeout(__autoRegister__, 100);
                        return;
                    }
                    // 检查用户代码是否已经用正确的 uiConfig 注册过
                    if (panel.modules) {
                        var existing = panel.modules.find(function(m) { return m.id === __MODULE_INFO__.id; });
                        if (existing && existing.uiConfig && existing.uiConfig.type) {
                            // 用户代码已注册且包含 uiConfig，跳过自动注册
                            return;
                        }
                    }
                    // 用户代码未注册或未传递 uiConfig，补充注册
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
     * 验证模块完整性
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) errors.add(Strings.validateNameEmpty)
        if (code.isBlank() && cssCode.isBlank()) errors.add(Strings.validateCodeEmpty)
        
        // Verify配置项
        configItems.forEach { item ->
            if (item.required && configValues[item.key].isNullOrBlank()) {
                errors.add(Strings.validateConfigRequired.replace("%s", item.name))
            }
        }
        
        return errors
    }
}

/**
 * 模块包 - 用于导出/导入多个模块
 */
data class ModulePackage(
    @SerializedName("name")
    val name: String,                               // Package name称
    @SerializedName("description")
    val description: String = "",                   // 包描述
    @SerializedName("author")
    val author: ModuleAuthor? = null,               // 作者
    @SerializedName("modules")
    val modules: List<ExtensionModule>,             // Module列表
    @SerializedName("version")
    val version: String = "1.0.0",                  // 包版本
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

/**
 * 模块分类分组 - 用于 UI 展示
 */
object ModuleCategoryGroups {
    
    /**
     * 分类分组定义
     */
    val groups = listOf(
        CategoryGroup(
            name = Strings.categoryGroupContent,
            icon = "edit_note",
            categories = listOf(
                ModuleCategory.CONTENT_FILTER,
                ModuleCategory.CONTENT_ENHANCE,
                ModuleCategory.READING
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupAppearance,
            icon = "palette",
            categories = listOf(
                ModuleCategory.STYLE_MODIFIER,
                ModuleCategory.THEME
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupFunction,
            icon = "bolt",
            categories = listOf(
                ModuleCategory.FUNCTION_ENHANCE,
                ModuleCategory.AUTOMATION,
                ModuleCategory.NAVIGATION,
                ModuleCategory.INTERACTION
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupData,
            icon = "analytics",
            categories = listOf(
                ModuleCategory.DATA_EXTRACT,
                ModuleCategory.DATA_SAVE,
                ModuleCategory.TRANSLATE
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupMedia,
            icon = "movie",
            categories = listOf(
                ModuleCategory.MEDIA,
                ModuleCategory.VIDEO,
                ModuleCategory.IMAGE,
                ModuleCategory.AUDIO
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupSecurity,
            icon = "lock",
            categories = listOf(
                ModuleCategory.SECURITY,
                ModuleCategory.ANTI_TRACKING
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupLife,
            icon = "build",
            categories = listOf(
                ModuleCategory.SOCIAL,
                ModuleCategory.SHOPPING,
                ModuleCategory.ACCESSIBILITY
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupDeveloper,
            icon = "computer",
            categories = listOf(
                ModuleCategory.DEVELOPER
            )
        ),
        CategoryGroup(
            name = Strings.categoryGroupOther,
            icon = "package",
            categories = listOf(
                ModuleCategory.OTHER
            )
        )
    )
    
    /**
     * 获取分类所属的分组
     */
    fun getGroupForCategory(category: ModuleCategory): CategoryGroup? {
        return groups.find { it.categories.contains(category) }
    }
}

/**
 * 分类分组
 */
data class CategoryGroup(
    val name: String,
    val icon: String,
    val categories: List<ModuleCategory>
)

/**
 * 权限分组 - 用于 UI 展示
 */
object ModulePermissionGroups {
    
    val groups = listOf(
        PermissionGroup(
            name = Strings.permGroupBasic,
            permissions = listOf(
                ModulePermission.DOM_ACCESS,
                ModulePermission.DOM_OBSERVE,
                ModulePermission.CSS_INJECT
            )
        ),
        PermissionGroup(
            name = Strings.permGroupStorage,
            permissions = listOf(
                ModulePermission.STORAGE,
                ModulePermission.COOKIE,
                ModulePermission.INDEXED_DB,
                ModulePermission.CACHE
            )
        ),
        PermissionGroup(
            name = Strings.permGroupNetwork,
            permissions = listOf(
                ModulePermission.NETWORK,
                ModulePermission.WEBSOCKET,
                ModulePermission.FETCH_INTERCEPT
            )
        ),
        PermissionGroup(
            name = Strings.permGroupInteraction,
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
            name = Strings.permGroupDevice,
            permissions = listOf(
                ModulePermission.LOCATION,
                ModulePermission.CAMERA,
                ModulePermission.MICROPHONE,
                ModulePermission.DEVICE_INFO
            )
        ),
        PermissionGroup(
            name = Strings.permGroupMediaPerm,
            permissions = listOf(
                ModulePermission.MEDIA,
                ModulePermission.FULLSCREEN,
                ModulePermission.PICTURE_IN_PICTURE,
                ModulePermission.SCREEN_CAPTURE
            )
        ),
        PermissionGroup(
            name = Strings.permGroupFile,
            permissions = listOf(
                ModulePermission.DOWNLOAD,
                ModulePermission.FILE_ACCESS
            )
        ),
        PermissionGroup(
            name = Strings.permGroupAdvanced,
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

/**
 * 权限分组
 */
data class PermissionGroup(
    val name: String,
    val permissions: List<ModulePermission>
)

/**
 * 常用模块预设 - 快速创建常见类型模块
 */
object ModulePresets {
    
    /**
     * 创建元素屏蔽模块
     */
    fun createElementBlocker(
        name: String,
        selectors: List<String>,
        description: String = Strings.presetBlockElements
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "block",
            category = ModuleCategory.CONTENT_FILTER,
            tags = listOf(Strings.tagBlock, Strings.tagHideElement),
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
    
    /**
     * 创建样式注入模块
     */
    fun createStyleInjector(
        name: String,
        cssCode: String,
        description: String = Strings.presetInjectStyle
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "palette",
            category = ModuleCategory.STYLE_MODIFIER,
            tags = listOf(Strings.tagStyleCss, "CSS"),
            runAt = ModuleRunTime.DOCUMENT_START,
            permissions = listOf(ModulePermission.CSS_INJECT),
            cssCode = cssCode
        )
    }
    
    /**
     * 创建自动点击模块
     */
    fun createAutoClicker(
        name: String,
        selector: String,
        delay: Int = 1000,
        description: String = Strings.presetAutoClick
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "mouse",
            category = ModuleCategory.AUTOMATION,
            tags = listOf(Strings.tagAuto, Strings.tagClickAction),
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
    
    /**
     * 创建悬浮按钮模块
     */
    fun createFloatingButton(
        name: String,
        buttonText: String,
        onClick: String,
        position: String = "bottom-right",
        description: String = Strings.presetFloatingButton
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
            tags = listOf(Strings.tagButton, Strings.tagFloatingWidget),
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
