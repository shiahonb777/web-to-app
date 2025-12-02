package com.webtoapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.webtoapp.data.converter.Converters

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
    val url: String,
    val iconPath: String? = null,
    val packageName: String? = null,

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
    val hideToolbar: Boolean = false // 隐藏工具栏（全屏模式，无浏览器特征）
)

/**
 * 启动画面配置
 */
data class SplashConfig(
    val type: SplashType = SplashType.IMAGE,  // 类型：图片或视频
    val mediaPath: String? = null,             // 媒体文件路径
    val duration: Int = 3,                     // 图片显示时长（秒，1-5秒）
    val clickToSkip: Boolean = true,           // 是否允许点击跳过
    
    // 视频裁剪配置
    val videoStartMs: Long = 0,                // 视频裁剪起始时间（毫秒）
    val videoEndMs: Long = 5000,               // 视频裁剪结束时间（毫秒，最大5秒）
    val videoDurationMs: Long = 0              // 原视频总时长（毫秒）
)

/**
 * 启动画面类型
 */
enum class SplashType {
    IMAGE,  // 图片
    VIDEO   // 视频（裁剪后5秒以内）
}
