package com.webtoapp.core.errorpage

/**
 * network error.
 */
enum class ErrorPageMode {
    /** default error system. */
    DEFAULT,
    /** usage. */
    BUILTIN_STYLE,
    /** load user. */
    CUSTOM_HTML,
    /** Note. */
    CUSTOM_MEDIA
}

/**
 * error.
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
 * Note.
 */
enum class MiniGameType(val displayName: String) {
    RANDOM("随机"),
    BREAKOUT("弹球消消"),
    MAZE("迷宫行者"),
    INK_ZEN("水墨禅境"),
    STAR_CATCH("星空收集")
}

/**
 * config network error.
 */
data class ErrorPageConfig(
    /** Note. */
    val mode: ErrorPageMode = ErrorPageMode.BUILTIN_STYLE,
    /** BUILTIN_STYLE. */
    val builtInStyle: ErrorPageStyle = ErrorPageStyle.MATERIAL,
    /** user. */
    val customHtml: String? = null,
    /** path media. */
    val customMediaPath: String? = null,
    /** entry. */
    val showMiniGame: Boolean = false,
    /** Note. */
    val miniGameType: MiniGameType = MiniGameType.RANDOM,
    /** usage. */
    val retryButtonText: String = "",
    /** auto. */
    val autoRetrySeconds: Int = 15,
    /** locale. */
    val language: String = "CHINESE"
)