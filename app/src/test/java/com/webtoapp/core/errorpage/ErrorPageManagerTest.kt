package com.webtoapp.core.errorpage

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ErrorPageManagerTest {

    @Test
    fun `default mode does not intercept system error page`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(mode = ErrorPageMode.DEFAULT)
        )

        val html = manager.generateErrorPage(-2, "dns failed", "https://example.com")

        assertThat(html).isNull()
    }

    @Test
    fun `custom html mode returns configured html directly`() {
        val customHtml = "<html><body>My Custom Error</body></html>"
        val manager = ErrorPageManager(
            ErrorPageConfig(
                mode = ErrorPageMode.CUSTOM_HTML,
                customHtml = customHtml
            )
        )

        val html = manager.generateErrorPage(-2, "failed", "https://example.com")

        assertThat(html).isEqualTo(customHtml)
    }

    @Test
    fun `custom media mode renders video when path is mp4`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(
                mode = ErrorPageMode.CUSTOM_MEDIA,
                customMediaPath = "/storage/error.mp4",
                retryButtonText = "重试加载"
            )
        )

        val html = manager.generateErrorPage(-6, "timeout", "https://example.com")

        assertThat(html).contains("<video")
        assertThat(html).contains("重试加载")
        assertThat(html).contains("/storage/error.mp4")
    }

    @Test
    fun `custom media mode renders image when path is not video`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(
                mode = ErrorPageMode.CUSTOM_MEDIA,
                customMediaPath = "/storage/error.png"
            )
        )

        val html = manager.generateErrorPage(-6, "timeout", "https://example.com")

        assertThat(html).contains("<img")
        assertThat(html).contains("/storage/error.png")
    }

    @Test
    fun `builtin mode includes retry auto retry and game sections based on config`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(
                mode = ErrorPageMode.BUILTIN_STYLE,
                builtInStyle = ErrorPageStyle.NEON,
                showMiniGame = true,
                miniGameType = MiniGameType.MAZE,
                retryButtonText = "再试一次",
                autoRetrySeconds = 5
            )
        )

        val html = manager.generateErrorPage(
            -2,
            "dns",
            "https://example.com/path?x='1'&y=\"2\""
        )

        assertThat(html).contains("再试一次")
        assertThat(html).contains("自动重试中")
        assertThat(html).contains("showGame()")
        assertThat(html).contains("gameCanvas")
        assertThat(html).contains("\\'1\\'")
        assertThat(html).contains("&quot;2&quot;")
    }

    @Test
    fun `builtin mode hides auto retry and game entry when disabled`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(
                mode = ErrorPageMode.BUILTIN_STYLE,
                showMiniGame = false,
                autoRetrySeconds = 0
            )
        )

        val html = manager.generateErrorPage(-2, "dns", "https://example.com")

        assertThat(html).doesNotContain("自动重试中")
        assertThat(html).doesNotContain("showGame()")
        assertThat(html).doesNotContain("gameOverlay")
    }
}
