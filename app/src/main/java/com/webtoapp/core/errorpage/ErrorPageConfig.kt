package com.webtoapp.core.errorpage




enum class ErrorPageMode {

    DEFAULT,

    BUILTIN_STYLE,

    CUSTOM_HTML,

    CUSTOM_MEDIA
}




enum class ErrorPageStyle(val displayName: String) {
    MATERIAL("Material Design"),
    SATELLITE("深空卫星"),
    OCEAN("深海世界"),
    FOREST("萤火森林"),
    MINIMAL("极简线条"),
    NEON("赛博霓虹")
}




enum class MiniGameType(val displayName: String) {
    RANDOM("随机"),
    BREAKOUT("弹球消消"),
    MAZE("迷宫行者"),
    INK_ZEN("水墨禅境"),
    STAR_CATCH("星空收集")
}




data class ErrorPageConfig(

    val mode: ErrorPageMode = ErrorPageMode.BUILTIN_STYLE,

    val builtInStyle: ErrorPageStyle = ErrorPageStyle.MATERIAL,

    val customHtml: String? = null,

    val customMediaPath: String? = null,

    val showMiniGame: Boolean = false,

    val miniGameType: MiniGameType = MiniGameType.RANDOM,

    val retryButtonText: String = "",

    val autoRetrySeconds: Int = 15,

    val language: String = "CHINESE"
)
