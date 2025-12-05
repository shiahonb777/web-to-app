package com.webtoapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.webtoapp.data.converter.Converters

/**
 * 应用类型
 */
enum class AppType {
    WEB,    // 网页应用（默认）
    IMAGE,  // 图片展示应用
    VIDEO,  // 视频播放应用
    HTML    // 本地HTML应用（支持HTML+CSS+JS）
}

/**
 * WebApp实体类 - 存储用户创建的应用配置
 */
@Entity(tableName = "web_apps")
@TypeConverters(Converters::class)
data class WebApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 基本信息
    val name: String,
    val url: String,                           // WEB类型为URL，IMAGE/VIDEO类型为媒体文件路径
    val iconPath: String? = null,
    val packageName: String? = null,
    val appType: AppType = AppType.WEB,        // 应用类型
    
    // 媒体应用配置（仅 IMAGE/VIDEO 类型）
    val mediaConfig: MediaConfig? = null,
    
    // HTML应用配置（仅 HTML 类型）
    val htmlConfig: HtmlConfig? = null,

    // 激活码配置
    val activationEnabled: Boolean = false,
    val activationCodes: List<String> = emptyList(),
    val isActivated: Boolean = false,

    // 广告配置
    val adsEnabled: Boolean = false,
    val adConfig: AdConfig? = null,

    // 公告配置
    val announcementEnabled: Boolean = false,
    val announcement: Announcement? = null,

    // 广告拦截配置
    val adBlockEnabled: Boolean = false,
    val adBlockRules: List<String> = emptyList(),

    // WebView配置
    val webViewConfig: WebViewConfig = WebViewConfig(),

    // 启动画面配置
    val splashEnabled: Boolean = false,
    val splashConfig: SplashConfig? = null,

    // 背景音乐配置
    val bgmEnabled: Boolean = false,
    val bgmConfig: BgmConfig? = null,
    
    // APK 导出配置（仅打包APK时生效）
    val apkExportConfig: ApkExportConfig? = null,

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
 * 公告配置
 */
data class Announcement(
    val title: String = "",
    val content: String = "",
    val linkUrl: String? = null,
    val linkText: String? = null,
    val showOnce: Boolean = true,
    val enabled: Boolean = true,
    val version: Int = 1 // 用于判断是否显示过
)

/**
 * WebView配置
 */
data class WebViewConfig(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val allowFileAccess: Boolean = false,
    val allowContentAccess: Boolean = true,
    val cacheEnabled: Boolean = true,
    val userAgent: String? = null,
    val desktopMode: Boolean = false,
    val zoomEnabled: Boolean = true,
    val swipeRefreshEnabled: Boolean = true,
    val fullscreenEnabled: Boolean = true,
    val downloadEnabled: Boolean = true,
    val openExternalLinks: Boolean = false, // 外部链接是否在浏览器打开
    val hideToolbar: Boolean = false, // 隐藏工具栏（全屏模式，无浏览器特征）
    val injectScripts: List<UserScript> = emptyList() // 用户自定义注入脚本
)

/**
 * 用户自定义脚本（油猴风格）
 */
data class UserScript(
    val name: String = "",           // 脚本名称
    val code: String = "",           // JavaScript 代码
    val enabled: Boolean = true,     // 是否启用
    val runAt: ScriptRunTime = ScriptRunTime.DOCUMENT_END // 运行时机
)

/**
 * 脚本运行时机
 */
enum class ScriptRunTime {
    DOCUMENT_START, // 页面开始加载时（DOM 未就绪）
    DOCUMENT_END,   // DOM 就绪后（推荐）
    DOCUMENT_IDLE   // 页面完全加载后
}

/**
 * 启动画面配置
 */
data class SplashConfig(
    val type: SplashType = SplashType.IMAGE,  // 类型：图片或视频
    val mediaPath: String? = null,             // 媒体文件路径
    val duration: Int = 3,                     // 图片显示时长（秒，1-5秒）
    val clickToSkip: Boolean = true,           // 是否允许点击跳过
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT, // 显示方向
    val fillScreen: Boolean = true,            // 是否自动放大铺满屏幕
    val enableAudio: Boolean = false,          // 是否启用视频音频
    
    // 视频裁剪配置
    val videoStartMs: Long = 0,                // 视频裁剪起始时间（毫秒）
    val videoEndMs: Long = 5000,               // 视频裁剪结束时间（毫秒）
    val videoDurationMs: Long = 0              // 原视频总时长（毫秒）
)

/**
 * 启动画面类型
 */
enum class SplashType {
    IMAGE,  // 图片
    VIDEO   // 视频
}

/**
 * 启动画面显示方向
 */
enum class SplashOrientation {
    PORTRAIT,   // 竖屏
    LANDSCAPE   // 横屏
}

/**
 * 媒体应用配置（图片/视频转APP）
 */
data class MediaConfig(
    val mediaPath: String,                         // 媒体文件路径
    val enableAudio: Boolean = true,               // 视频是否启用音频
    val loop: Boolean = true,                      // 是否循环播放（视频）
    val autoPlay: Boolean = true,                  // 是否自动播放（视频）
    val fillScreen: Boolean = true,                // 是否铺满屏幕
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT, // 显示方向
    val backgroundColor: String = "#000000"        // 背景颜色
)

/**
 * HTML应用配置（本地HTML+CSS+JS转APP）
 */
data class HtmlConfig(
    val projectId: String = "",                    // 项目ID（用于定位文件目录）
    val entryFile: String = "index.html",          // 入口HTML文件名
    val files: List<HtmlFile> = emptyList(),       // 所有文件列表（HTML/CSS/JS等）
    val enableJavaScript: Boolean = true,          // 是否启用JavaScript
    val enableLocalStorage: Boolean = true,        // 是否启用本地存储
    val allowFileAccess: Boolean = true,           // 是否允许文件访问
    val backgroundColor: String = "#FFFFFF"        // 背景颜色
)

/**
 * HTML项目中的单个文件
 */
data class HtmlFile(
    val name: String,                              // 文件名（含相对路径，如 "css/style.css"）
    val path: String,                              // 本地绝对路径
    val type: HtmlFileType = HtmlFileType.OTHER    // 文件类型
)

/**
 * HTML文件类型
 */
enum class HtmlFileType {
    HTML,   // HTML文件
    CSS,    // CSS样式文件
    JS,     // JavaScript文件
    IMAGE,  // 图片资源
    FONT,   // 字体文件
    OTHER   // 其他文件
}

/**
 * 背景音乐播放模式
 */
enum class BgmPlayMode {
    LOOP,       // 单曲循环
    SEQUENTIAL, // 顺序播放
    SHUFFLE     // 随机播放
}

/**
 * 音乐标签 - 用于分类
 */
enum class BgmTag(val displayName: String) {
    PURE_MUSIC("纯音乐"),
    POP("流行"),
    ROCK("摇滚"),
    CLASSICAL("古典"),
    JAZZ("爵士"),
    ELECTRONIC("电子"),
    FOLK("民谣"),
    CHINESE_STYLE("古风"),
    ANIME("动漫"),
    GAME("游戏"),
    MOVIE("影视"),
    HEALING("治愈"),
    EXCITING("激昂"),
    SAD("伤感"),
    ROMANTIC("浪漫"),
    RELAXING("轻松"),
    WORKOUT("运动"),
    SLEEP("助眠"),
    STUDY("学习"),
    OTHER("其他")
}

/**
 * LRC 字幕元素
 */
data class LrcLine(
    val startTime: Long,    // 开始时间（毫秒）
    val endTime: Long,      // 结束时间（毫秒）
    val text: String,       // 歌词文本
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
enum class LrcAnimationType(val displayName: String) {
    NONE("无动画"),
    FADE("淡入淡出"),
    SLIDE_UP("向上滑动"),
    SLIDE_LEFT("向左滑动"),
    SCALE("缩放"),
    TYPEWRITER("打字机"),
    KARAOKE("卡拉OK高亮")
}

/**
 * 字幕位置
 */
enum class LrcPosition(val displayName: String) {
    TOP("顶部"),
    CENTER("居中"),
    BOTTOM("底部")
}

/**
 * 背景音乐项
 */
data class BgmItem(
    val id: String = java.util.UUID.randomUUID().toString(),  // 唯一ID
    val name: String,           // 音乐名称
    val path: String,           // 音乐文件路径
    val coverPath: String? = null, // 封面图片路径（可选）
    val isAsset: Boolean = false,  // 是否为预置资源
    val tags: List<BgmTag> = emptyList(),  // 标签
    val sortOrder: Int = 0,     // 排序顺序
    val lrcData: LrcData? = null,  // LRC 字幕数据
    val lrcPath: String? = null,   // LRC 文件路径
    val duration: Long = 0      // 音乐时长（毫秒）
)

/**
 * 背景音乐配置
 */
data class BgmConfig(
    val playlist: List<BgmItem> = emptyList(),  // 播放列表
    val playMode: BgmPlayMode = BgmPlayMode.LOOP, // 播放模式
    val volume: Float = 0.5f,                    // 音量 (0.0-1.0)
    val autoPlay: Boolean = true,                // 是否自动播放
    val showLyrics: Boolean = true,              // 是否显示歌词
    val lrcTheme: LrcTheme? = null               // 字幕主题
)

/**
 * APK 导出配置（仅打包APK时生效）
 */
data class ApkExportConfig(
    val customPackageName: String? = null,       // 自定义包名（如 com.example.myapp）
    val customVersionName: String? = null,       // 自定义版本名（如 1.0.0）
    val customVersionCode: Int? = null           // 自定义版本号（如 1）
)
