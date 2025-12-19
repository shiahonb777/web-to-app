package com.webtoapp.data.model

/**
 * 画廊/幻灯片配置 - 支持多媒体项目
 * 可用于：媒体应用、HTML应用、网页应用
 */
data class GalleryConfig(
    val items: List<GalleryItem> = emptyList(),      // 媒体项列表
    val autoPlay: Boolean = false,                    // 是否自动播放幻灯片
    val autoPlayInterval: Int = 5,                    // 自动播放间隔（秒）
    val showIndicator: Boolean = true,                // 是否显示页面指示器
    val showTitle: Boolean = true,                    // 是否显示标题栏
    val enableSwipe: Boolean = true,                  // 是否允许滑动切换
    val loop: Boolean = true,                         // 是否循环播放
    val transitionType: GalleryTransition = GalleryTransition.SLIDE  // 切换动画类型
)

/**
 * 画廊项目 - 单个媒体/页面
 */
data class GalleryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",                           // 显示标题
    val type: GalleryItemType,                        // 项目类型
    val path: String,                                 // 文件路径或URL
    val thumbnailPath: String? = null,                // 缩略图路径（可选）
    val description: String? = null,                  // 描述（可选）
    val sortOrder: Int = 0,                           // 排序顺序
    
    // 媒体特有配置
    val mediaConfig: MediaItemConfig? = null,
    
    // HTML特有配置
    val htmlConfig: HtmlItemConfig? = null,
    
    // 网页特有配置
    val webConfig: WebItemConfig? = null
)

/**
 * 画廊项目类型
 */
enum class GalleryItemType {
    IMAGE,      // 图片
    VIDEO,      // 视频
    HTML,       // HTML页面
    WEB         // 网页URL
}

/**
 * 切换动画类型
 */
enum class GalleryTransition(val displayName: String) {
    NONE("无动画"),
    SLIDE("滑动"),
    FADE("淡入淡出"),
    ZOOM("缩放"),
    CUBE("立方体"),
    FLIP("翻转")
}

/**
 * 媒体项配置
 */
data class MediaItemConfig(
    val enableAudio: Boolean = true,                  // 视频是否启用音频
    val loop: Boolean = false,                        // 单个视频是否循环
    val autoPlay: Boolean = true,                     // 是否自动播放
    val fillScreen: Boolean = true,                   // 是否铺满屏幕
    val duration: Long = 0                            // 媒体时长（毫秒，用于视频）
)

/**
 * HTML项配置
 */
data class HtmlItemConfig(
    val entryFile: String = "index.html",             // 入口文件
    val enableJavaScript: Boolean = true,             // 是否启用JS
    val enableLocalStorage: Boolean = true            // 是否启用本地存储
)

/**
 * 网页项配置
 */
data class WebItemConfig(
    val desktopMode: Boolean = false,                 // 桌面模式
    val enableJavaScript: Boolean = true,             // 是否启用JS
    val customUserAgent: String? = null               // 自定义UA
)
