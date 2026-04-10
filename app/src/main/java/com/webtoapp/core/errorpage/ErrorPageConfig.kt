package com.webtoapp.core.errorpage

/**
 * 网络错误页显示模式
 */
enum class ErrorPageMode {
    /** 系统默认错误页（不拦截） */
    DEFAULT,
    /** 使用内置精美风格 */
    BUILTIN_STYLE,
    /** 加载用户自定义 HTML */
    CUSTOM_HTML,
    /** 显示自定义图片或视频 */
    CUSTOM_MEDIA
}

/**
 * 内置错误页视觉风格
 */
enum class ErrorPageStyle(val displayName: String) {
    MATERIAL("Material Design"),
    SATELLITE("深空卫星"),
    OCEAN("深海世界"),
    FOREST("萤火森林"),
    MINIMAL("极简线条"),
    NEON("赛博霓虹")
}

/**
 * 内嵌小游戏类型
 */
enum class MiniGameType(val displayName: String) {
    RANDOM("随机"),
    BREAKOUT("弹球消消"),
    MAZE("迷宫行者"),
    INK_ZEN("水墨禅境"),
    STAR_CATCH("星空收集")
}

/**
 * 网络错误页配置
 */
data class ErrorPageConfig(
    /** 显示模式 */
    val mode: ErrorPageMode = ErrorPageMode.BUILTIN_STYLE,
    /** 内置风格（仅 BUILTIN_STYLE 模式） */
    val builtInStyle: ErrorPageStyle = ErrorPageStyle.MATERIAL,
    /** 用户自定义 HTML（仅 CUSTOM_HTML 模式） */
    val customHtml: String? = null,
    /** 自定义媒体路径（仅 CUSTOM_MEDIA 模式，图片或视频） */
    val customMediaPath: String? = null,
    /** 是否显示小游戏入口 */
    val showMiniGame: Boolean = false,
    /** 小游戏类型 */
    val miniGameType: MiniGameType = MiniGameType.RANDOM,
    /** 重试按钮文字（会被 i18n 覆盖，仅自定义场景使用） */
    val retryButtonText: String = "",
    /** 自动重试间隔秒数（0=不自动重试） */
    val autoRetrySeconds: Int = 15,
    /** 界面语言: CHINESE, ENGLISH, ARABIC */
    val language: String = "CHINESE"
)
