package com.webtoapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.webtoapp.ui.data.converter.Converters
import androidx.compose.runtime.Stable
import com.webtoapp.util.toFileSizeString

/**
 * 应用类型
 */
enum class AppType {
    WEB,        // Web page应用（默认）
    IMAGE,      // Image展示应用（单图片，兼容旧版）
    VIDEO,      // Video播放应用（单视频，兼容旧版）
    HTML,       // LocalHTML应用（支持HTML+CSS+JS）
    GALLERY,    // Media画廊应用（多图片/视频、分类、排序、连续播放）
    FRONTEND,   // 前端项目应用（Vue/React/Vite 等构建产物）
    WORDPRESS,  // WordPress 离线应用（PHP + SQLite）
    NODEJS_APP, // Node.js 后端应用（Express/Fastify/Nest.js 等）
    PHP_APP,    // 通用 PHP 应用（Laravel/ThinkPHP/CodeIgniter 等）
    PYTHON_APP, // Python Web 应用（Flask/Django/FastAPI 等）
    GO_APP,     // Go Web 服务（Gin/Fiber/Echo 等）
    MULTI_WEB   // 多站点聚合应用（多链接合并为一个 APP）
}

/**
 * WebApp实体类 - 存储用户创建的应用配置
 */
@Entity(
    tableName = "web_apps",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["categoryId"]),
        Index(value = ["isActivated"])
    ]
)
@TypeConverters(Converters::class)
@Stable
data class WebApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 基本信息
    val name: String,
    val url: String,                           // WEB类型为URL，IMAGE/VIDEO类型为媒体文件路径
    val iconPath: String? = null,
    val packageName: String? = null,
    val appType: AppType = AppType.WEB,        // App类型
    
    // Media应用配置（仅 IMAGE/VIDEO 类型，兼容旧版）
    val mediaConfig: MediaConfig? = null,
    
    // Media画廊配置（仅 GALLERY 类型，支持多媒体）
    val galleryConfig: GalleryConfig? = null,
    
    // HTML应用配置（仅 HTML 类型）
    val htmlConfig: HtmlConfig? = null,
    
    // WordPress应用配置（仅 WORDPRESS 类型）
    val wordpressConfig: WordPressConfig? = null,
    
    // Node.js应用配置（仅 NODEJS_APP 类型）
    val nodejsConfig: NodeJsConfig? = null,
    
    // 通用 PHP 应用配置（仅 PHP_APP 类型）
    val phpAppConfig: PhpAppConfig? = null,
    
    // Python Web 应用配置（仅 PYTHON_APP 类型）
    val pythonAppConfig: PythonAppConfig? = null,
    
    // Go Web 服务配置（仅 GO_APP 类型）
    val goAppConfig: GoAppConfig? = null,
    
    // 多站点聚合配置（仅 MULTI_WEB 类型）
    val multiWebConfig: MultiWebConfig? = null,

    // Activation码配置
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),  // 旧格式（兼容性）
    val activationCodeList: List<com.webtoapp.core.activation.ActivationCode> = emptyList(),  // 新格式
    val activationRequireEveryTime: Boolean = false,  // Yes否每次启动都需要验证
    val isActivated: Boolean = false,

    // Ad配置
    val adsEnabled: Boolean = false,
    val adConfig: AdConfig? = null,

    // Announcement配置
    val announcementEnabled: Boolean = false,
    val announcement: Announcement? = null,

    // Ad拦截配置
    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),

    // WebView配置
    val webViewConfig: WebViewConfig = WebViewConfig(),

    // Start画面配置
    val splashEnabled: Boolean = false,
    val splashConfig: SplashConfig? = null,

    // Background music配置
    val bgmEnabled: Boolean = false,
    val bgmConfig: BgmConfig? = null,
    
    // APK 导出配置（仅打包APK时生效）
    val apkExportConfig: ApkExportConfig? = null,
    
    // Theme配置（用于导出的应用 UI 风格）
    val themeType: String = "AURORA",
    
    // Web page自动翻译配置
    val translateEnabled: Boolean = false,
    val translateConfig: TranslateConfig? = null,
    
    // 扩展模块配置
    val extensionModuleIds: List<String> = emptyList(),  // Enable的扩展模块ID列表
    val extensionFabIcon: String? = null,  // 扩展模块悬浮按钮自定义图标（emoji，空=默认🧩）
    
    // 自启动配置
    val autoStartConfig: AutoStartConfig? = null,
    
    // 强制运行配置
    val forcedRunConfig: com.webtoapp.core.forcedrun.ForcedRunConfig? = null,
    
    // 黑科技功能配置（独立模块）
    val blackTechConfig: com.webtoapp.core.blacktech.BlackTechConfig? = null,
    
    // App伪装配置（独立模块）
    val disguiseConfig: com.webtoapp.core.disguise.DisguiseConfig? = null,
    
    // 浏览器伪装配置（反指纹引擎）
    val browserDisguiseConfig: com.webtoapp.core.disguise.BrowserDisguiseConfig? = null,
    
    // 设备伪装配置（设备类型/品牌/型号 UA 伪装）
    val deviceDisguiseConfig: com.webtoapp.core.disguise.DeviceDisguiseConfig? = null,
    
    // 激活码对话框自定义文本
    val activationDialogConfig: ActivationDialogConfig? = null,
    
    // 分类ID（关联 AppCategory）
    val categoryId: Long? = null,
    
    // 云 SDK 配置（关联云项目，启用更新检查/公告/远程配置等）
    val cloudConfig: CloudAppConfig? = null,

    // 元数据
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 广告配置
 */
data class AdConfig(
    val bannerEnabled: Boolean = false,
    val bannerId: String = "",
    val interstitialEnabled: Boolean = false,
    val interstitialId: String = "",
    val splashEnabled: Boolean = false,
    val splashId: String = "",
    val splashDuration: Int = 3 // 秒
)

/**
 * 公告模板类型
 */
enum class AnnouncementTemplateType {
    MINIMAL,        // 极简风格
    XIAOHONGSHU,    // 小红书风格
    GRADIENT,       // 渐变风格
    GLASSMORPHISM,  // 毛玻璃风格
    NEON,           // 霓虹风格
    CUTE,           // 可爱风格
    ELEGANT,        // 优雅风格
    FESTIVE,        // 节日风格
    DARK,           // 暗黑风格
    NATURE          // 自然风格
}

/**
 * 公告触发模式
 */
enum class AnnouncementTriggerMode {
    ON_LAUNCH,      // Start时触发
    ON_INTERVAL,    // 定时间隔触发
    ON_NO_NETWORK   // 无网络时触发
}

/**
 * 公告配置
 */
data class Announcement(
    val title: String = "",
    val content: String = "",
    val linkUrl: String? = null,
    val linkText: String? = null,
    val showOnce: Boolean = true,
    val enabled: Boolean = true,
    val version: Int = 1, // 用于判断是否显示过
    val template: AnnouncementTemplateType = AnnouncementTemplateType.XIAOHONGSHU, // Announcement模板
    val showEmoji: Boolean = true, // Yes否显示表情
    val animationEnabled: Boolean = true, // Yes否启用动画
    // 新增：需要勾选同意/已阅读才能关闭
    val requireConfirmation: Boolean = false,
    // 新增：允许用户勾选不再显示
    val allowNeverShow: Boolean = true,
    
    // ==================== 触发机制 ====================
    // Start时触发（默认开启，保持backward compatible）
    val triggerOnLaunch: Boolean = true,
    // 无网络时触发
    val triggerOnNoNetwork: Boolean = false,
    // 定时间隔触发（分钟，0=禁用）
    val triggerIntervalMinutes: Int = 0,
    // 定时触发是否在启动时也立即触发一次
    val triggerIntervalIncludeLaunch: Boolean = false
)

/**
 * 状态栏颜色模式
 */
enum class StatusBarColorMode {
    THEME,      // 跟随主题色（默认）
    TRANSPARENT,// 完全透明
    CUSTOM      // Custom颜色
}

/**
 * Status bar background类型
 */
enum class StatusBarBackgroundType {
    COLOR,  // 纯色背景（使用 statusBarColor）
    IMAGE   // Image背景
}


/**
 * 长按菜单样式
 */
enum class LongPressMenuStyle {
    DISABLED,       // Disable长按菜单
    SIMPLE,         // 简洁模式：仅保存图片、复制链接
    FULL,           // 完整模式：所有功能
    IOS,            // iOS 风格：类似 iPhone 的模糊背景菜单
    FLOATING,       // 悬浮气泡：在点击位置显示小气泡
    CONTEXT         // 右键菜单：类似桌面端右键菜单
}

/**
 * User-Agent 模式
 * 用于伪装浏览器身份，绕过网站对 WebView 的检测
 */
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

/**
 * 浏览器版本号集中管理
 * 更新时只需修改这里，所有 UA 字符串自动同步
 */
object UserAgentVersions {
    const val CHROME = "131"
    const val FIREFOX = "133"
    const val SAFARI = "18"
}

/**
 * WebView configuration
 */
@Stable
data class WebViewConfig(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val allowFileAccess: Boolean = false,
    val allowContentAccess: Boolean = true,
    val cacheEnabled: Boolean = true,
    val userAgent: String? = null,
    val userAgentMode: UserAgentMode = UserAgentMode.DEFAULT, // User-Agent 模式
    val customUserAgent: String? = null, // Custom User-Agent（仅 CUSTOM 模式使用）
    val desktopMode: Boolean = false, // Keep forbackward compatible
    val zoomEnabled: Boolean = true,
    val swipeRefreshEnabled: Boolean = true,
    val fullscreenEnabled: Boolean = true,
    val downloadEnabled: Boolean = true,
    val openExternalLinks: Boolean = false, // External链接是否在浏览器打开
    val hideToolbar: Boolean = false, // Hide工具栏（全屏模式，无浏览器特征）
    val showStatusBarInFullscreen: Boolean = false, // Fullscreen模式下是否显示状态栏
    val showNavigationBarInFullscreen: Boolean = false, // Fullscreen模式下是否显示导航栏
    val showToolbarInFullscreen: Boolean = false, // Fullscreen模式下是否显示顶部导航栏
    val landscapeMode: Boolean = false, // [已废弃] 向后兼容，使用 orientationMode 代替
    val orientationMode: OrientationMode = if (landscapeMode) OrientationMode.LANDSCAPE else OrientationMode.PORTRAIT, // 屏幕方向模式
    val injectScripts: List<UserScript> = emptyList(), // User自定义注入脚本
    val statusBarColorMode: StatusBarColorMode = StatusBarColorMode.THEME, // Status bar颜色模式
    val statusBarColor: String? = null, // Custom状态栏颜色（仅 CUSTOM 模式生效，如 "#FF5722"）
    val statusBarDarkIcons: Boolean? = null, // Status bar图标颜色：true=深色图标，false=浅色图标，null=自动
    // Status bar背景配置（新增）
    val statusBarBackgroundType: StatusBarBackgroundType = StatusBarBackgroundType.COLOR, // Background type
    val statusBarBackgroundImage: String? = null, // Cropped image path
    val statusBarBackgroundAlpha: Float = 1.0f, // Alpha 0.0-1.0
    val statusBarHeightDp: Int = 0, // Custom高度dp（0=系统默认）
    val longPressMenuEnabled: Boolean = true, // Yes否启用长按菜单
    val longPressMenuStyle: LongPressMenuStyle = LongPressMenuStyle.FULL, // Long press menu style
    val adBlockToggleEnabled: Boolean = false, // Allow用户在运行时切换广告拦截开关
    val popupBlockerEnabled: Boolean = true, // 启用弹窗拦截器（拦截 window.open 等弹窗广告）
    val popupBlockerToggleEnabled: Boolean = false, // Allow用户在运行时切换弹窗拦截开关
    
    // ============ 浏览器兼容性增强配置 ============
    val initialScale: Int = 0, // Initial scale (0-200, 0=自动)，解决 CSS zoom 不生效问题
    val viewportMode: ViewportMode = ViewportMode.DEFAULT, // 视口模式（解决 Unity/Canvas 游戏放大裁切问题）
    val customViewportWidth: Int = 0, // 自定义视口宽度（像素），0=自动。仅 CUSTOM 模式生效。有效范围：320-3840
    val newWindowBehavior: NewWindowBehavior = NewWindowBehavior.SAME_WINDOW, // window.open / target="_blank" 行为
    val enablePaymentSchemes: Boolean = true, // Enable支付宝、微信等支付 scheme 拦截
    val enableShareBridge: Boolean = true, // Enable navigator.share 桥接
    val enableZoomPolyfill: Boolean = true, // Enable CSS zoom polyfill（自动转换为 transform）
    
    // ============ 高级功能配置 ============
    val enableCrossOriginIsolation: Boolean = false, // 启用跨域隔离（SharedArrayBuffer/FFmpeg.wasm 支持）
    val disableShields: Boolean = false, // 禁用 BrowserShields（允许第三方 Cookie、跳过跟踪器拦截等，适用于游戏/需要完整网络功能的应用）
    val keepScreenOn: Boolean = false, // [向后兼容] 保持屏幕常亮
    val screenAwakeMode: ScreenAwakeMode = ScreenAwakeMode.OFF, // 屏幕常亮模式：OFF / ALWAYS / TIMED
    val screenAwakeTimeoutMinutes: Int = 30, // 定时常亮时长（分钟），仅 TIMED 模式有效
    val screenBrightness: Int = -1, // 屏幕亮度：-1=跟随系统, 0-100=自定义百分比
    val keyboardAdjustMode: KeyboardAdjustMode = KeyboardAdjustMode.RESIZE, // 键盘调整模式（RESIZE=推起内容，NOTHING=覆盖页面）
    
    // ============ 安全配置 ============
    // SECURITY: These should only be true for local HTML/FRONTEND apps.
    // For WEB apps loading remote URLs, these MUST be false to prevent
    // malicious web pages from reading local files via file:// URLs.
    val allowFileAccessFromFileURLs: Boolean = false,
    val allowUniversalAccessFromFileURLs: Boolean = false,
    
    // ============ 网络错误页配置 ============
    val errorPageConfig: com.webtoapp.core.errorpage.ErrorPageConfig = com.webtoapp.core.errorpage.ErrorPageConfig(),
    
    // ============ 性能优化 ============
    val performanceOptimization: Boolean = false,  // 运行时性能优化脚本注入（懒加载/预连接/滚动优化/内存管理）
    
    // ============ PWA 离线支持 ============
    val pwaOfflineEnabled: Boolean = false,  // 启用 Service Worker 离线缓存
    val pwaOfflineStrategy: String = "NETWORK_FIRST",  // 缓存策略: CACHE_FIRST / NETWORK_FIRST / STALE_WHILE_REVALIDATE
    
    // ============ 悬浮返回按钮 ============
    val showFloatingBackButton: Boolean = true, // 全屏模式下是否显示悬浮返回按钮

    // ============ 系统导航手势屏蔽 ============
    val blockSystemNavigationGesture: Boolean = false, // 是否屏蔽系统导航手势（全屏模式下生效，默认关闭）

    // ============ 悬浮小窗配置 ============
    val floatingWindowConfig: FloatingWindowConfig = FloatingWindowConfig()
)

/**
 * 悬浮小窗配置
 * 支持以悬浮窗模式显示应用，可自由调整大小和透明度
 */
data class FloatingWindowConfig(
    val enabled: Boolean = false,              // 是否启用悬浮窗模式
    val windowSizePercent: Int = 80,            // [向后兼容] 窗口大小百分比 (50-100)
    val widthPercent: Int = 80,                 // 窗口宽度百分比 (30-100)
    val heightPercent: Int = 80,                // 窗口高度百分比 (30-100)
    val lockAspectRatio: Boolean = true,        // 锁定宽高比（同步调整宽高）
    val opacity: Int = 100,                     // 透明度百分比 (30-100)
    val cornerRadius: Int = 16,                 // 圆角半径 dp (0-32)
    val borderStyle: FloatingBorderStyle = FloatingBorderStyle.SUBTLE, // 边框样式
    val showTitleBar: Boolean = true,           // 是否显示标题栏（用于拖拽移动）
    val autoHideTitleBar: Boolean = false,       // 空闲后自动隐藏标题栏
    val startMinimized: Boolean = false,        // 启动时最小化为悬浮按钮
    val rememberPosition: Boolean = true,       // 记住上次窗口位置
    val edgeSnapping: Boolean = true,           // 边缘吸附（拖拽到屏幕边缘自动贴边）
    val showResizeHandle: Boolean = true,       // 显示右下角缩放手柄
    val lockPosition: Boolean = false           // 锁定位置（禁止拖拽）
)

/**
 * 悬浮窗边框样式
 */
enum class FloatingBorderStyle {
    NONE,      // 无边框
    SUBTLE,    // 细微边框（默认）
    GLOW,      // 发光边框
    ACCENT     // 主题色边框
}

/**
 * User custom script (Tampermonkey style)
 */
data class UserScript(
    val name: String = "",           // Script名称
    val code: String = "",           // JavaScript 代码
    val enabled: Boolean = true,     // Yes否启用
    val runAt: ScriptRunTime = ScriptRunTime.DOCUMENT_END // 运行时机
)

/**
 * Script run timing
 */
enum class ScriptRunTime {
    DOCUMENT_START, // Page开始加载时（DOM 未就绪）
    DOCUMENT_END,   // DOM 就绪后（推荐）
    DOCUMENT_IDLE   // Page完全加载后
}

/**
 * New window open behavior（window.open / target="_blank"）
 */
enum class NewWindowBehavior {
    SAME_WINDOW,    // 在当前窗口打开（默认）
    EXTERNAL_BROWSER, // Open in external browser
    POPUP_WINDOW,   // 弹出新窗口（需要处理）
    BLOCK           // Block opening
}

/**
 * Splash screen configuration
 */
data class SplashConfig(
    val type: SplashType = SplashType.IMAGE,  // Class型：图片或视频
    val mediaPath: String? = null,             // Media文件路径
    val duration: Int = 3,                     // Image显示时长（秒，1-5秒）
    val clickToSkip: Boolean = true,           // Yes否允许点击跳过
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT, // Show方向
    val fillScreen: Boolean = true,            // Yes否自动放大铺满屏幕
    val enableAudio: Boolean = false,          // Yes否启用视频音频
    
    // Video裁剪配置
    val videoStartMs: Long = 0,                // Video裁剪起始时间（毫秒）
    val videoEndMs: Long = 5000,               // Video裁剪结束时间（毫秒）
    val videoDurationMs: Long = 0              // 原视频总时长（毫秒）
)

/**
 * 启动画面类型
 */
enum class SplashType {
    IMAGE,  // Image
    VIDEO   // Video
}

/**
 * 启动画面显示方向
 */
enum class SplashOrientation {
    PORTRAIT,   // Portrait
    LANDSCAPE   // Landscape
}

/**
 * 键盘调整模式 — 控制软键盘弹出时的页面行为
 *
 * - RESIZE: 页面自动调整大小，键盘会推起内容（确保输入框可见，可能有轻微卡顿）
 * - NOTHING: 键盘覆盖页面，不调整布局（更流畅，但可能遮挡输入框）
 */
enum class KeyboardAdjustMode {
    RESIZE,      // 页面调整大小（键盘推起内容）
    NOTHING      // 键盘覆盖页面（无布局调整）
}

/**
 * 屏幕方向模式 — 用于 WebApp / 各类应用配置
 *
 * 支持七种模式：
 * - PORTRAIT: 锁定竖屏（正向）
 * - LANDSCAPE: 锁定横屏（正向）
 * - REVERSE_PORTRAIT: 锁定反向竖屏（倒置）
 * - REVERSE_LANDSCAPE: 锁定反向横屏
 * - SENSOR_PORTRAIT: 竖屏 + 重力感应（允许正向/反向竖屏切换）
 * - SENSOR_LANDSCAPE: 横屏 + 重力感应（允许正向/反向横屏切换）
 * - AUTO: 全方向自动旋转（跟随重力感应，平板友好）
 */
enum class OrientationMode {
    PORTRAIT,            // 锁定竖屏（正向）
    LANDSCAPE,           // 锁定横屏（正向）
    REVERSE_PORTRAIT,    // 锁定反向竖屏（倒置）
    REVERSE_LANDSCAPE,   // 锁定反向横屏
    SENSOR_PORTRAIT,     // 竖屏 + 重力感应（正向/反向竖屏自动切换）
    SENSOR_LANDSCAPE,    // 横屏 + 重力感应（正向/反向横屏自动切换）
    AUTO                 // 全方向自动旋转（重力感应）
}

/**
 * 屏幕常亮模式
 */
enum class ScreenAwakeMode {
    OFF,       // 关闭：跟随系统超时
    ALWAYS,    // 始终常亮：适用于 code-server、数字相框等
    TIMED      // 定时常亮：在指定时间后恢复系统超时（节省电量）
}

/**
 * 视口适配模式 — 控制 WebView 如何处理页面的视口缩放
 *
 * 问题背景：Unity WebGL 游戏、Canvas 应用等使用固定尺寸渲染，
 * Android WebView 默认的 DPI 缩放会导致内容放大，UI 元素被裁切到屏幕外。
 *
 * - DEFAULT: 标准行为，适合大多数网页
 * - FIT_SCREEN: 强制内容适配屏幕（注入 viewport meta + CSS 缩放），解决 Unity/Canvas 放大裁切问题
 * - DESKTOP: 桌面视口（980px 宽），适合桌面端网页
 * - CUSTOM: 用户自定义视口宽度
 */
enum class ViewportMode {
    DEFAULT,       // 标准模式（适合大多数网页）
    FIT_SCREEN,    // 适配屏幕（强制缩放至可见范围，适合 Unity/Canvas/游戏）
    DESKTOP,       // 桌面视口（980px 宽度，适合桌面端网页）
    CUSTOM         // 自定义视口宽度
}

/**
 * Media app configuration（图片/视频转APP）- 兼容旧版单媒体模式
 */
data class MediaConfig(
    val mediaPath: String,                         // Media文件路径
    val enableAudio: Boolean = true,               // Video是否启用音频
    val loop: Boolean = true,                      // Yes否循环播放（视频）
    val autoPlay: Boolean = true,                  // Yes否自动播放（视频）
    val fillScreen: Boolean = true,                // Yes否铺满屏幕
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT, // Show方向
    val backgroundColor: String = "#000000",       // 背景颜色
    val keepScreenOn: Boolean = true               // 保持屏幕常亮
)

// ==================== 媒体画廊配置（新版多媒体支持）====================

/**
 * Media gallery configuration - 支持多图片/视频、分类、排序、连续播放
 */
@Stable
data class GalleryConfig(
    val items: List<GalleryItem> = emptyList(),                      // Media项列表
    val categories: List<GalleryCategory> = emptyList(),             // 分类列表
    val playMode: GalleryPlayMode = GalleryPlayMode.SEQUENTIAL,      // Play模式
    val imageInterval: Int = 3,                                      // Image播放间隔（秒，1-60）
    val loop: Boolean = true,                                        // Yes否循环播放
    val autoPlay: Boolean = false,                                   // 进入后是否自动播放
    val shuffleOnLoop: Boolean = false,                              // Loop时是否打乱顺序
    val defaultView: GalleryViewMode = GalleryViewMode.GRID,         // Default视图模式
    val gridColumns: Int = 3,                                        // 网格列数（2-5）
    val sortOrder: GallerySortOrder = GallerySortOrder.CUSTOM,       // Sort方式
    val backgroundColor: String = "#000000",                         // Play器背景颜色
    val showThumbnailBar: Boolean = true,                            // Play时显示底部缩略图栏
    val showMediaInfo: Boolean = true,                               // Show媒体信息（名称、索引等）
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT, // 屏幕方向
    val enableAudio: Boolean = true,                                 // Video是否启用音频
    val videoAutoNext: Boolean = true,                               // Video播放完自动下一个
    val rememberPosition: Boolean = false                            // 记住上次播放位置
) {
    /**
     * Get media items by category
     */
    fun getItemsByCategory(categoryId: String?): List<GalleryItem> {
        return if (categoryId == null) {
            items
        } else {
            items.filter { it.categoryId == categoryId }
        }
    }
    
    /**
     * Get sorted media items
     */
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
    
    /**
     * Statistics
     */
    val imageCount: Int get() = items.count { it.type == GalleryItemType.IMAGE }
    val videoCount: Int get() = items.count { it.type == GalleryItemType.VIDEO }
    val totalCount: Int get() = items.size
}

/**
 * Gallery media item
 */
data class GalleryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val path: String,                                // Media文件路径
    val type: GalleryItemType,                       // Media类型
    val name: String = "",                           // Show名称
    val categoryId: String? = null,                  // 所属分类ID
    val duration: Long = 0,                          // Video时长（毫秒）
    val thumbnailPath: String? = null,               // 缩略图路径
    val sortIndex: Int = 0,                          // 手动排序索引
    val createdAt: Long = System.currentTimeMillis(),// 添加时间
    val width: Int = 0,                              // Media宽度
    val height: Int = 0,                             // Media高度
    val fileSize: Long = 0                           // File大小（字节）
) {
    /**
     * 格式化的时长显示（视频）
     */
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
    
    /**
     * 格式化的文件大小显示
     * 复用 Extensions.kt 中的 Long.toFileSizeString()
     */
    val formattedFileSize: String
        get() = if (fileSize <= 0) "" else fileSize.toFileSizeString()
}

/**
 * 画廊分类
 */
data class GalleryCategory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,                                // 分类名称
    val icon: String = "folder",                        // 分类图标（icon ID）
    val color: String = "#6200EE",                   // 分类颜色
    val sortIndex: Int = 0                           // Sort索引
)

/**
 * 媒体项类型
 */
enum class GalleryItemType {
    IMAGE,  // Image
    VIDEO   // Video
}

/**
 * 画廊播放模式
 */
enum class GalleryPlayMode {
    SEQUENTIAL,   // Sequential播放
    SHUFFLE,      // Shuffle播放
    SINGLE_LOOP   // 单个循环
}

/**
 * 画廊视图模式
 */
enum class GalleryViewMode {
    GRID,         // 网格视图
    LIST,         // List视图
    TIMELINE      // Time线视图
}

/**
 * 画廊排序方式
 */
enum class GallerySortOrder {
    CUSTOM,       // Custom排序（手动拖拽）
    NAME_ASC,     // Name升序
    NAME_DESC,    // Name降序
    DATE_ASC,     // Date升序（最早在前）
    DATE_DESC,    // Date降序（最新在前）
    TYPE          // 按类型分组（图片在前/视频在前）
}

/**
 * Node.js 应用构建模式
 */
enum class NodeJsBuildMode {
    STATIC,         // 纯静态前端（dist 目录）
    SSR,            // 服务端渲染（Next.js/Nuxt.js）
    API_BACKEND,    // API 后端（Express/Fastify/Koa）
    FULLSTACK       // 全栈应用（前端 + API）
}

/**
 * Node.js 应用配置（Node.js 后端项目转 APP）
 */
data class NodeJsConfig(
    val projectId: String = "",                      // 项目ID
    val projectName: String = "",                     // 项目名称
    val framework: String = "",                       // 框架名称（express, fastify, koa, nest 等）
    val buildMode: NodeJsBuildMode = NodeJsBuildMode.API_BACKEND,  // 构建模式
    val entryFile: String = "index.js",              // 入口文件（如 server.js, index.js, app.js）
    val serverPort: Int = 0,                         // 服务器端口（0=自动分配）
    val envVars: Map<String, String> = emptyMap(),   // 环境变量
    val hasNodeModules: Boolean = false,             // 是否包含 node_modules
    val nodeVersion: String = "",                    // Node.js 版本要求
    val landscapeMode: Boolean = false               // 横屏模式
)

/**
 * WordPress 应用配置（离线 WordPress 站点转 APP）
 */
data class WordPressConfig(
    val projectId: String = "",                    // 项目ID（用于定位 WordPress 文件目录）
    val siteTitle: String = "My Site",              // 站点标题
    val adminUser: String = "admin",                // 管理员用户名
    val adminEmail: String = "",                    // 管理员邮箱
    val themeName: String = "",                     // 主题目录名
    val plugins: List<String> = emptyList(),        // 已安装的插件列表
    val phpPort: Int = 0,                           // PHP 服务器端口（0=自动分配）
    val landscapeMode: Boolean = false              // 横屏模式
)

/**
 * 通用 PHP 应用配置（Laravel/ThinkPHP/CodeIgniter 等转 APP）
 */
data class PhpAppConfig(
    val projectId: String = "",                      // 项目ID
    val projectName: String = "",                     // 项目名称
    val framework: String = "",                       // 框架名称（laravel, thinkphp, codeigniter, slim, raw）
    val documentRoot: String = "",                    // 相对于项目根的 Web 根目录（如 public/）
    val entryFile: String = "index.php",             // 入口文件
    val phpPort: Int = 0,                            // PHP 服务器端口（0=自动分配）
    val envVars: Map<String, String> = emptyMap(),   // 环境变量
    val hasComposerJson: Boolean = false,            // 是否有 composer.json
    val landscapeMode: Boolean = false               // 横屏模式
)

/**
 * Python Web 应用配置（Flask/Django/FastAPI 等转 APP）
 */
data class PythonAppConfig(
    val projectId: String = "",                      // 项目ID
    val projectName: String = "",                     // 项目名称
    val framework: String = "",                       // 框架名称（flask, django, fastapi, tornado, raw）
    val entryFile: String = "app.py",               // 入口文件
    val entryModule: String = "",                    // WSGI/ASGI module（如 "myapp.wsgi:application"）
    val serverType: String = "builtin",             // 服务器类型（builtin, gunicorn, uvicorn）
    val serverPort: Int = 0,                         // 服务器端口（0=自动分配）
    val envVars: Map<String, String> = emptyMap(),   // 环境变量
    val pythonVersion: String = "",                  // Python 版本要求
    val requirementsFile: String = "requirements.txt", // 依赖文件
    val hasPipDeps: Boolean = false,                 // 是否有 pip 依赖
    val landscapeMode: Boolean = false               // 横屏模式
)

/**
 * Go Web 服务配置（Gin/Fiber/Echo 等转 APP）
 */
data class GoAppConfig(
    val projectId: String = "",                      // 项目ID
    val projectName: String = "",                     // 项目名称
    val framework: String = "",                       // 框架名称（gin, fiber, echo, chi, net_http, raw）
    val binaryName: String = "",                     // 预编译二进制文件名
    val serverPort: Int = 0,                         // 服务器端口（0=自动分配）
    val envVars: Map<String, String> = emptyMap(),   // 环境变量
    val staticDir: String = "",                      // 静态文件目录（如 static/, public/）
    val hasBuildFromSource: Boolean = false,         // 是否从源码构建
    val landscapeMode: Boolean = false               // 横屏模式
)

/**
 * 多站点聚合应用配置（多链接合并为一个 APP）
 * 支持三种显示模式：Tabs（底部标签页）、Cards（卡片首页）、Feed（聚合信息流）
 */
data class MultiWebConfig(
    val sites: List<MultiWebSite> = emptyList(),      // 站点列表
    val displayMode: String = "TABS",                  // 显示模式: TABS, CARDS, FEED, DRAWER
    val refreshInterval: Int = 30,                     // 自动刷新间隔（分钟，仅 FEED 模式）
    val showSiteIcons: Boolean = true,                 // 是否显示站点图标
    val landscapeMode: Boolean = false                 // 横屏模式
)

/**
 * 多站点聚合中的单个站点配置
 */
data class MultiWebSite(
    val id: String = "",                               // 站点唯一ID
    val name: String = "",                             // 站点名称
    val url: String = "",                              // 站点URL
    val iconEmoji: String = "",                        // 站点图标（Emoji）
    val faviconUrl: String = "",                       // Favicon URL（自动获取）
    val themeColor: String = "",                       // 主题色（自动获取）
    val category: String = "",                         // 分类标签
    val cssSelector: String = "",                      // CSS 选择器（Feed 模式用于提取文章）
    val linkSelector: String = "",                     // 链接选择器（Feed 模式用于提取链接）
    val enabled: Boolean = true,                       // 是否启用
    val sortIndex: Int = 0                             // 排序索引（用于拖拽重排序）
)

/**
 * HTML应用配置（本地HTML+CSS+JS转APP）
 */
data class HtmlConfig(
    val projectId: String = "",                    // 项目ID（用于定位文件目录）
    val projectDir: String? = null,                // 项目目录路径（用于遍历嵌入）
    val entryFile: String = "index.html",          // 入口HTML文件名
    val files: List<HtmlFile> = emptyList(),       // 所有文件列表（HTML/CSS/JS等）
    val enableJavaScript: Boolean = true,          // Yes否启用JavaScript
    val enableLocalStorage: Boolean = true,        // Yes否启用本地存储
    val allowFileAccess: Boolean = true,           // Yes否允许文件访问
    val backgroundColor: String = "#FFFFFF",       // 背景颜色
    val landscapeMode: Boolean = false             // Landscape模式
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
 * HTML项目中的单个文件
 */
data class HtmlFile(
    val name: String,                              // File名（含相对路径，如 "css/style.css"）
    val path: String,                              // Local绝对路径
    val type: HtmlFileType = HtmlFileType.OTHER    // File类型
)

/**
 * HTML文件类型
 */
enum class HtmlFileType {
    HTML,   // HTML文件
    CSS,    // CSS样式文件
    JS,     // JavaScript文件
    IMAGE,  // Image资源
    FONT,   // 字体文件
    OTHER   // 其他文件
}

/**
 * 背景音乐播放模式
 */
enum class BgmPlayMode {
    LOOP,       // 单曲循环
    SEQUENTIAL, // Sequential播放
    SHUFFLE     // Shuffle播放
}

/**
 * 音乐标签 - 用于分类
 */
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

/**
 * LRC 字幕元素
 */
data class LrcLine(
    val startTime: Long,    // Start时间（毫秒）
    val endTime: Long,      // End时间（毫秒）
    val text: String,       // Lyrics文本
    val translation: String? = null  // 翻译（可选）
)

/**
 * LRC 字幕数据
 */
data class LrcData(
    val lines: List<LrcLine> = emptyList(),
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val language: String? = null
)

/**
 * 字幕主题样式
 */
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

/**
 * 字幕动画类型
 */
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

/**
 * 字幕位置
 */
enum class LrcPosition {
    TOP, CENTER, BOTTOM;
    
    val displayName: String get() = when (this) {
        TOP -> com.webtoapp.core.i18n.Strings.lrcPosTop
        CENTER -> com.webtoapp.core.i18n.Strings.lrcPosCenter
        BOTTOM -> com.webtoapp.core.i18n.Strings.lrcPosBottom
    }
}

/**
 * 背景音乐项
 */
data class BgmItem(
    val id: String = java.util.UUID.randomUUID().toString(),  // 唯一ID
    val name: String,           // 音乐名称
    val path: String,           // 音乐文件路径
    val coverPath: String? = null, // 封面图片路径（可选）
    val isAsset: Boolean = false,  // Yes否为预置资源
    val tags: List<BgmTag> = emptyList(),  // 标签
    val sortOrder: Int = 0,     // Sort顺序
    val lrcData: LrcData? = null,  // LRC 字幕数据
    val lrcPath: String? = null,   // LRC 文件路径
    val duration: Long = 0      // 音乐时长（毫秒）
)

/**
 * 背景音乐配置
 */
data class BgmConfig(
    val playlist: List<BgmItem> = emptyList(),  // Play列表
    val playMode: BgmPlayMode = BgmPlayMode.LOOP, // Play模式
    val volume: Float = 0.5f,                    // Volume (0.0-1.0)
    val autoPlay: Boolean = true,                // Yes否自动播放
    val showLyrics: Boolean = true,              // Yes否显示歌词
    val lrcTheme: LrcTheme? = null               // 字幕主题
)

/**
 * APK 架构选择
 */
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

/**
 * APK 导出配置（仅打包APK时生效）
 */
data class ApkExportConfig(
    val customPackageName: String? = null,       // Custom包名（如 com.example.myapp）
    val customVersionName: String? = null,       // Custom版本名（如 1.0.0）
    val customVersionCode: Int? = null,          // Custom版本号（如 1）
    val architecture: ApkArchitecture = ApkArchitecture.UNIVERSAL,  // APK架构
    val encryptionConfig: ApkEncryptionConfig = ApkEncryptionConfig(),  // Encryption配置
    val hardeningConfig: AppHardeningConfig = AppHardeningConfig(),  // 软件加固配置
    val isolationConfig: com.webtoapp.core.isolation.IsolationConfig = com.webtoapp.core.isolation.IsolationConfig(),  // 独立环境/多开配置
    val backgroundRunEnabled: Boolean = false,   // Yes否启用后台运行
    val backgroundRunConfig: BackgroundRunExportConfig = BackgroundRunExportConfig(),  // 后台运行配置
    val engineType: String = "SYSTEM_WEBVIEW",   // 浏览器引擎类型: SYSTEM_WEBVIEW, GECKOVIEW
    val deepLinkEnabled: Boolean = false,         // 是否启用Deep Link（链接打开）
    val customDeepLinkHosts: List<String> = emptyList(), // 用户自定义的额外 Deep Link 域名
    val performanceOptimization: Boolean = false,  // 性能优化（资源压缩/构建加速/加载提速）
    val performanceConfig: PerformanceOptimizationConfig = PerformanceOptimizationConfig()  // 性能优化详细配置
)

/**
 * 性能优化配置
 */
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

/**
 * 后台运行导出配置
 */
data class BackgroundRunExportConfig(
    val notificationTitle: String = "",          // 通知标题
    val notificationContent: String = "",        // 通知内容
    val showNotification: Boolean = true,        // Yes否显示通知
    val keepCpuAwake: Boolean = true             // Yes否保持CPU唤醒
)

/**
 * APK 加密配置
 */
data class ApkEncryptionConfig(
    val enabled: Boolean = false,                // Yes否启用加密
    val encryptConfig: Boolean = true,           // Encryption配置文件
    val encryptHtml: Boolean = true,             // Encryption HTML/CSS/JS
    val encryptMedia: Boolean = false,           // Encryption媒体文件（图片/视频）
    val encryptSplash: Boolean = false,          // Encryption启动画面
    val encryptBgm: Boolean = false,             // Encryption背景音乐
    val customPassword: String? = null,          // Custom密码（可选，增强安全性）
    val enableIntegrityCheck: Boolean = true,    // Enable完整性检查
    val enableAntiDebug: Boolean = true,         // Enable反调试保护
    val enableAntiTamper: Boolean = true,        // Enable防篡改保护
    val obfuscateStrings: Boolean = false,       // 混淆字符串（实验性）
    val encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD  // Encryption强度
) {
    /**
     * 加密强度级别
     */
    enum class EncryptionLevel(val iterations: Int) {
        FAST(5000),
        STANDARD(10000),
        HIGH(50000),
        PARANOID(100000);
        
        val description: String get() = when (this) {
            FAST -> com.webtoapp.core.i18n.Strings.encryptLevelFast
            STANDARD -> com.webtoapp.core.i18n.Strings.encryptLevelStandard
            HIGH -> com.webtoapp.core.i18n.Strings.encryptLevelHigh
            PARANOID -> com.webtoapp.core.i18n.Strings.encryptLevelParanoid
        }
    }
    
    companion object {
        /** 不加密 */
        val DISABLED = ApkEncryptionConfig(enabled = false)
        
        /** 基础加密（仅加密代码和配置） */
        val BASIC = ApkEncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = false,
            enableIntegrityCheck = true,
            enableAntiDebug = false,
            encryptionLevel = EncryptionLevel.STANDARD
        )
        
        /** 完全加密（加密所有资源） */
        val FULL = ApkEncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = true,
            encryptSplash = true,
            encryptBgm = true,
            enableIntegrityCheck = true,
            enableAntiDebug = true,
            enableAntiTamper = true,
            encryptionLevel = EncryptionLevel.HIGH
        )
        
        /** 最高安全级别 */
        val MAXIMUM = ApkEncryptionConfig(
            enabled = true,
            encryptConfig = true,
            encryptHtml = true,
            encryptMedia = true,
            encryptSplash = true,
            encryptBgm = true,
            enableIntegrityCheck = true,
            enableAntiDebug = true,
            enableAntiTamper = true,
            obfuscateStrings = true,
            encryptionLevel = EncryptionLevel.PARANOID
        )
    }
    
    /** 转换为内部加密配置 */
    fun toEncryptionConfig(): com.webtoapp.core.crypto.EncryptionConfig {
        return com.webtoapp.core.crypto.EncryptionConfig(
            enabled = enabled,
            encryptConfig = encryptConfig,
            encryptHtml = encryptHtml,
            encryptMedia = encryptMedia,
            encryptSplash = encryptSplash,
            encryptBgm = encryptBgm,
            customPassword = customPassword,
            enableIntegrityCheck = enableIntegrityCheck,
            enableAntiDebug = enableAntiDebug,
            enableAntiTamper = enableAntiTamper,
            enableRootDetection = false,
            enableEmulatorDetection = false,
            obfuscateStrings = obfuscateStrings,
            encryptionLevel = when (encryptionLevel) {
                EncryptionLevel.FAST -> com.webtoapp.core.crypto.EncryptionLevel.FAST
                EncryptionLevel.STANDARD -> com.webtoapp.core.crypto.EncryptionLevel.STANDARD
                EncryptionLevel.HIGH -> com.webtoapp.core.crypto.EncryptionLevel.HIGH
                EncryptionLevel.PARANOID -> com.webtoapp.core.crypto.EncryptionLevel.PARANOID
            },
            enableRuntimeProtection = enableIntegrityCheck || enableAntiDebug || enableAntiTamper,
            blockOnThreat = false
        )
    }
}

/**
 * 翻译目标语言 — 支持 20 种世界主要语言
 */
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

/**
 * 翻译引擎 — 支持多引擎自动降级
 *
 * 优先级：GOOGLE → MYMEMORY → LIBRE → LINGVA
 */
enum class TranslateEngine(val displayName: String) {
    /** Google Translate API（默认，最稳定） */
    AUTO("自动选择"),
    /** Google Translate */
    GOOGLE("Google Translate"),
    /** MyMemory — 开源翻译记忆库 */
    MYMEMORY("MyMemory"),
    /** LibreTranslate — 自托管开源翻译引擎 */
    LIBRE("LibreTranslate"),
    /** Lingva — 开源 Google 前端代理 */
    LINGVA("Lingva Translate")
}

/**
 * 网页自动翻译配置
 */
data class TranslateConfig(
    val targetLanguage: TranslateLanguage = TranslateLanguage.CHINESE,  // 目标翻译语言
    val showFloatingButton: Boolean = true,  // 是否显示翻译悬浮按钮
    val preferredEngine: TranslateEngine = TranslateEngine.AUTO,  // 首选翻译引擎
    val autoTranslateOnLoad: Boolean = true  // 页面加载完成后自动翻译
)

/**
 * WebApp 扩展函数 - 获取所有激活码（兼容新旧格式）
 */
fun WebApp.getAllActivationCodes(): List<com.webtoapp.core.activation.ActivationCode> {
    val codes = mutableListOf<com.webtoapp.core.activation.ActivationCode>()
    
    // 添加新格式激活码
    codes.addAll(activationCodeList)
    
    // 添加旧格式激活码（转换为新格式）
    activationCodes.forEach { codeStr ->
        // 尝试解析为新格式
        val code = com.webtoapp.core.activation.ActivationCode.fromJson(codeStr)
        if (code != null) {
            codes.add(code)
        } else {
            // 旧格式，转换为永久激活码
            codes.add(com.webtoapp.core.activation.ActivationCode.fromLegacyString(codeStr))
        }
    }
    
    return codes
}

/**
 * WebApp 扩展函数 - 获取激活码字符串列表（用于兼容旧代码）
 */
fun WebApp.getActivationCodeStrings(): List<String> {
    val strings = mutableListOf<String>()
    
    // 添加新格式激活码的 JSON 字符串
    activationCodeList.forEach { code ->
        strings.add(code.toJson())
    }
    
    // 添加旧格式激活码
    activationCodes.forEach { codeStr ->
        // If not JSON 格式，直接添加
        if (!codeStr.trimStart().startsWith("{")) {
            strings.add(codeStr)
        }
    }
    
    return strings
}

/**
 * 激活码对话框自定义文本配置
 */
data class ActivationDialogConfig(
    val title: String = "",       // 自定义标题（空=使用默认「激活应用」）
    val subtitle: String = "",    // 自定义副标题（空=使用默认「请输入激活码以继续使用」）
    val inputLabel: String = "",  // 自定义输入框标签（空=使用默认「激活码」）
    val buttonText: String = ""   // 自定义按钮文字（空=使用默认「激活」）
)

/**
 * 软件加固配置
 * 独立于加密功能，提供企业级应用加固保护
 */
data class AppHardeningConfig(
    val enabled: Boolean = false,                    // 是否启用加固
    val hardeningLevel: HardeningLevel = HardeningLevel.STANDARD,  // 加固等级
    
    // ==================== DEX 保护 ====================
    val dexEncryption: Boolean = true,               // DEX 文件加密（壳保护）
    val dexSplitting: Boolean = false,               // DEX 分片动态加载
    val dexVmp: Boolean = false,                     // VMP 虚拟机保护（将关键代码转为自定义指令集）
    val dexControlFlowFlattening: Boolean = false,   // 控制流平坦化
    
    // ==================== Native SO 保护 ====================
    val soEncryption: Boolean = true,                // SO 文件 section 加密
    val soElfObfuscation: Boolean = false,           // ELF 头混淆（抗 IDA 分析）
    val soSymbolStrip: Boolean = true,               // 符号表剥离 + 假符号注入
    val soAntiDump: Boolean = false,                 // 反内存 dump 保护
    
    // ==================== 反逆向工程 ====================
    val antiDebugMultiLayer: Boolean = true,         // 多层反调试（ptrace/timing/signal/thread）
    val antiFridaAdvanced: Boolean = true,           // 高级 Frida 检测（内存扫描/线程名/inline hook）
    val antiXposedDeep: Boolean = true,              // 深度 Xposed/LSPosed 检测（ART hook 检测）
    val antiMagiskDetect: Boolean = false,           // Magisk/Shamiko 隐藏检测
    val antiMemoryDump: Boolean = false,             // 反内存 dump（mprotect + inotify）
    val antiScreenCapture: Boolean = false,          // 反截屏/录屏保护（FLAG_SECURE）
    
    // ==================== 环境检测 ====================
    val detectEmulatorAdvanced: Boolean = false,     // 高级模拟器检测（硬件指纹/传感器/温度）
    val detectVirtualApp: Boolean = true,            // 虚拟化环境检测（VirtualXposed/太极/Parallel Space）
    val detectUSBDebugging: Boolean = false,         // USB 调试状态检测
    val detectVPN: Boolean = false,                  // VPN/代理检测
    val detectDeveloperOptions: Boolean = false,     // 开发者选项检测
    
    // ==================== 代码混淆 ====================
    val stringEncryption: Boolean = true,            // 字符串加密（多层编码：AES + Base64 + XOR）
    val classNameObfuscation: Boolean = false,       // 类名混淆（重命名为无意义字符）
    val callIndirection: Boolean = false,            // 方法调用间接化（反射 + 动态代理）
    val opaquePredicates: Boolean = false,           // 不透明谓词注入（干扰静态分析）
    
    // ==================== 运行时自保护 (RASP) ====================
    val dexCrcVerify: Boolean = true,                // DEX CRC 自校验（检测运行时篡改）
    val memoryIntegrity: Boolean = false,            // 内存完整性监控（关键数据区域）
    val jniCallValidation: Boolean = false,          // JNI 调用链验证（防止伪造调用）
    val timingCheck: Boolean = false,                // 时序检测（反加速/减速攻击）
    val stackTraceFilter: Boolean = true,            // 堆栈轨迹清洗（隐藏内部实现）
    
    // ==================== 防篡改 ====================
    val multiPointSignatureVerify: Boolean = true,   // 多点签名验证（Native + Java + 延迟）
    val apkChecksumValidation: Boolean = true,       // APK 校验和验证（DEX + 资源 + Manifest）
    val resourceIntegrity: Boolean = false,          // 资源文件完整性校验
    val certificatePinning: Boolean = false,         // 证书锁定（防中间人攻击）
    
    // ==================== 威胁响应策略 ====================
    val responseStrategy: ThreatResponse = ThreatResponse.SILENT_EXIT,  // 检测到威胁时的响应
    val responseDelay: Int = 0,                      // 响应延迟（秒，0=立即；延迟退出更隐蔽）
    val enableHoneypot: Boolean = false,             // 蜜罐陷阱（注入假数据迷惑逆向者）
    val enableSelfDestruct: Boolean = false          // 自毁机制（严重威胁时清除敏感数据）
) {
    /**
     * 加固等级
     */
    enum class HardeningLevel {
        BASIC,        // 基础加固：DEX 加密 + 反调试 + 签名校验
        STANDARD,     // 标准加固：+ SO 保护 + 字符串加密 + 环境检测
        ADVANCED,     // 高级加固：+ VMP + 控制流混淆 + RASP + 内存保护
        FORTRESS;     // 堡垒级：全部开启，极致保护
        
        val displayName: String get() = when (this) {
            BASIC -> com.webtoapp.core.i18n.Strings.hardeningLevelBasic
            STANDARD -> com.webtoapp.core.i18n.Strings.hardeningLevelStandard
            ADVANCED -> com.webtoapp.core.i18n.Strings.hardeningLevelAdvanced
            FORTRESS -> com.webtoapp.core.i18n.Strings.hardeningLevelFortress
        }
        
        val description: String get() = when (this) {
            BASIC -> com.webtoapp.core.i18n.Strings.hardeningLevelBasicDesc
            STANDARD -> com.webtoapp.core.i18n.Strings.hardeningLevelStandardDesc
            ADVANCED -> com.webtoapp.core.i18n.Strings.hardeningLevelAdvancedDesc
            FORTRESS -> com.webtoapp.core.i18n.Strings.hardeningLevelFortressDesc
        }
    }
    
    /**
     * 威胁响应策略
     */
    enum class ThreatResponse {
        LOG_ONLY,        // 仅记录日志（最宽松）
        SILENT_EXIT,     // 静默退出（延迟随机时间后退出，不提示原因）
        CRASH_RANDOM,    // 随机崩溃（伪装成正常 bug，迷惑攻击者）
        DATA_WIPE,       // 数据擦除（清除应用内敏感数据后退出）
        FAKE_DATA;       // 假数据注入（返回伪造数据，蜜罐模式）
        
        val displayName: String get() = when (this) {
            LOG_ONLY -> com.webtoapp.core.i18n.Strings.threatResponseLogOnly
            SILENT_EXIT -> com.webtoapp.core.i18n.Strings.threatResponseSilentExit
            CRASH_RANDOM -> com.webtoapp.core.i18n.Strings.threatResponseCrashRandom
            DATA_WIPE -> com.webtoapp.core.i18n.Strings.threatResponseDataWipe
            FAKE_DATA -> com.webtoapp.core.i18n.Strings.threatResponseFakeData
        }
    }
    
    companion object {
        /** 不加固 */
        val DISABLED = AppHardeningConfig(enabled = false)
        
        /** 基础加固 */
        val BASIC = AppHardeningConfig(
            enabled = true,
            hardeningLevel = HardeningLevel.BASIC,
            dexEncryption = true,
            soEncryption = false,
            antiDebugMultiLayer = true,
            antiFridaAdvanced = true,
            antiXposedDeep = false,
            stringEncryption = false,
            dexCrcVerify = true,
            multiPointSignatureVerify = true,
            apkChecksumValidation = true
        )
        
        /** 标准加固 */
        val STANDARD = AppHardeningConfig(
            enabled = true,
            hardeningLevel = HardeningLevel.STANDARD,
            dexEncryption = true,
            soEncryption = true,
            soSymbolStrip = true,
            antiDebugMultiLayer = true,
            antiFridaAdvanced = true,
            antiXposedDeep = true,
            detectVirtualApp = true,
            stringEncryption = true,
            dexCrcVerify = true,
            stackTraceFilter = true,
            multiPointSignatureVerify = true,
            apkChecksumValidation = true
        )
        
        /** 高级加固 */
        val ADVANCED = AppHardeningConfig(
            enabled = true,
            hardeningLevel = HardeningLevel.ADVANCED,
            dexEncryption = true,
            dexSplitting = true,
            dexVmp = true,
            dexControlFlowFlattening = true,
            soEncryption = true,
            soElfObfuscation = true,
            soSymbolStrip = true,
            soAntiDump = true,
            antiDebugMultiLayer = true,
            antiFridaAdvanced = true,
            antiXposedDeep = true,
            antiMagiskDetect = true,
            antiMemoryDump = true,
            detectVirtualApp = true,
            detectUSBDebugging = true,
            stringEncryption = true,
            classNameObfuscation = true,
            callIndirection = true,
            opaquePredicates = true,
            dexCrcVerify = true,
            memoryIntegrity = true,
            jniCallValidation = true,
            timingCheck = true,
            stackTraceFilter = true,
            multiPointSignatureVerify = true,
            apkChecksumValidation = true,
            resourceIntegrity = true,
            responseStrategy = ThreatResponse.SILENT_EXIT,
            responseDelay = 3
        )
        
        /** 堡垒级 - 全部开启 */
        val FORTRESS = AppHardeningConfig(
            enabled = true,
            hardeningLevel = HardeningLevel.FORTRESS,
            dexEncryption = true,
            dexSplitting = true,
            dexVmp = true,
            dexControlFlowFlattening = true,
            soEncryption = true,
            soElfObfuscation = true,
            soSymbolStrip = true,
            soAntiDump = true,
            antiDebugMultiLayer = true,
            antiFridaAdvanced = true,
            antiXposedDeep = true,
            antiMagiskDetect = true,
            antiMemoryDump = true,
            antiScreenCapture = true,
            detectEmulatorAdvanced = true,
            detectVirtualApp = true,
            detectUSBDebugging = true,
            detectVPN = true,
            detectDeveloperOptions = true,
            stringEncryption = true,
            classNameObfuscation = true,
            callIndirection = true,
            opaquePredicates = true,
            dexCrcVerify = true,
            memoryIntegrity = true,
            jniCallValidation = true,
            timingCheck = true,
            stackTraceFilter = true,
            multiPointSignatureVerify = true,
            apkChecksumValidation = true,
            resourceIntegrity = true,
            certificatePinning = true,
            responseStrategy = ThreatResponse.CRASH_RANDOM,
            responseDelay = 5,
            enableHoneypot = true,
            enableSelfDestruct = true
        )
    }
}

/**
 * 自启动配置
 */
data class AutoStartConfig(
    val bootStartEnabled: Boolean = false,      // 开机自启动
    val scheduledStartEnabled: Boolean = false, // 定时自启动
    val scheduledTime: String = "08:00",        // 定时启动时间（HH:mm 格式）
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 启动日期（1-7 代表周一到周日）
    val scheduledRepeat: Boolean = true,        // 是否重复（每天/每周）
    val bootDelay: Long = 5000L                 // 开机延迟（毫秒，1000-30000）
)

/**
 * 云 SDK 应用配置 — 关联云项目后嵌入导出的 APK
 *
 * 用户在创建/编辑应用时选择一个云项目来关联，
 * 构建 APK 时会把此配置转换为 CloudSdkConfig 写入 app_config.json，
 * 导出的 APP 启动时 CloudSdkManager 会读取配置并初始化：
 * - 更新检查 → 弹出更新对话框
 * - 公告展示 → 弹出公告对话框
 * - 远程配置 → 缓存到 SharedPreferences
 * - 激活码验证 → 在线验证
 * - 统计上报 → 设备/使用/崩溃数据
 * - FCM 推送 → 实时推送（Ultra 专属）
 */
data class CloudAppConfig(
    /** 是否启用云 SDK */
    val enabled: Boolean = false,
    /** 关联的云项目 ID */
    val projectId: Int = 0,
    /** 云项目 Key（UUID） */
    val projectKey: String = "",
    /** 云项目名称（用于 UI 显示） */
    val projectName: String = "",
    
    // ─── 功能开关 ───
    /** 启用更新检查 */
    val updateCheckEnabled: Boolean = true,
    /** 启用公告 */
    val announcementEnabled: Boolean = true,
    /** 启用远程配置 */
    val remoteConfigEnabled: Boolean = true,
    /** 启用在线激活码验证（替代离线激活码） */
    val activationCodeEnabled: Boolean = false,
    /** 启用统计上报 */
    val statsReportEnabled: Boolean = true,
    /** 启用 FCM 推送 */
    val fcmPushEnabled: Boolean = false,
    /** 启用远程脚本热更 */
    val remoteScriptEnabled: Boolean = false,
    /** 启用崩溃上报 */
    val reportCrashes: Boolean = true,
    
    // ─── 更新配置 ───
    /** 更新检查间隔（秒） */
    val updateCheckInterval: Int = 3600,
    /** 支持强制更新 */
    val forceUpdateEnabled: Boolean = false,
    
    // ─── 统计配置 ───
    /** 统计上报间隔（秒） */
    val statsReportInterval: Int = 3600
) {
    /**
     * 转换为 CloudSdkConfig（用于 APK 构建时嵌入）
     */
    fun toCloudSdkConfig(): com.webtoapp.core.shell.CloudSdkConfig {
        return com.webtoapp.core.shell.CloudSdkConfig(
            enabled = enabled && projectKey.isNotBlank(),
            projectKey = projectKey,
            updateCheckEnabled = updateCheckEnabled,
            announcementEnabled = announcementEnabled,
            remoteConfigEnabled = remoteConfigEnabled,
            activationCodeEnabled = activationCodeEnabled,
            statsReportEnabled = statsReportEnabled,
            fcmPushEnabled = fcmPushEnabled,
            remoteScriptEnabled = remoteScriptEnabled,
            updateCheckInterval = updateCheckInterval,
            forceUpdateEnabled = forceUpdateEnabled,
            statsReportInterval = statsReportInterval,
            reportCrashes = reportCrashes
        )
    }
}

// ═══════════════════════════════════════════
// Manifest 序列化 / 反序列化（云端同步用）
// ═══════════════════════════════════════════

/**
 * 将 WebApp 序列化为 Manifest JSON 字符串。
 * 用于云端同步、备份、跨设备迁移。
 */
fun WebApp.toManifestJson(): String {
    return com.webtoapp.ui.data.converter.Converters.gson.toJson(this)
}

/**
 * Manifest 工具类
 */
object ManifestUtils {
    /**
     * 从 Manifest JSON 字符串恢复 WebApp。
     * 使用 mergeMissingDefaults 保证向前/向后兼容。
     */
    fun fromManifestJson(json: String, overrideId: Long? = null): WebApp? {
        return try {
            val parsed = com.google.gson.JsonParser.parseString(json)
            val defaultWebApp = WebApp(name = "", url = "")
            val defaultJson = com.webtoapp.ui.data.converter.Converters.gson.toJsonTree(defaultWebApp)
            val merged = com.webtoapp.ui.data.converter.Converters.mergeMissingDefaults(defaultJson, parsed)
            val restored = com.webtoapp.ui.data.converter.Converters.gson.fromJson(merged, WebApp::class.java)
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

