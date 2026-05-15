package com.webtoapp.core.errorpage

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ErrorPageManagerTest {

    @Test
    fun `default mode does not intercept unrecognized errors`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(mode = ErrorPageMode.DEFAULT)
        )

        // errorCode 0 + description with no errno marker → no diagnostic → system page preserved
        val html = manager.generateErrorPage(0, "generic failure", "https://example.com")

        assertThat(html).isNull()
    }

    @Test
    fun `default mode renders diagnostic fallback for EADDRNOTAVAIL`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(mode = ErrorPageMode.DEFAULT, language = "CHINESE")
        )

        val html = manager.generateErrorPage(
            -1,
            "bind failed: EADDRNOTAVAIL (Cannot assign requested address)",
            "https://example.com"
        )

        assertThat(html).isNotNull()
        assertThat(html).contains("无法绑定本机网络地址")
        assertThat(html).contains("EADDRNOTAVAIL")
        // No mini-game or auto retry countdown in the minimal fallback
        assertThat(html).doesNotContain("gameCanvas")
        assertThat(html).doesNotContain("秒后重试")
    }

    @Test
    fun `default mode localizes fallback to english`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(mode = ErrorPageMode.DEFAULT, language = "ENGLISH")
        )

        val html = manager.generateErrorPage(
            -1,
            "bind failed: EADDRNOTAVAIL",
            "https://example.com"
        )

        assertThat(html).isNotNull()
        assertThat(html).contains("Can&#39;t bind a local network address")
    }

    @Test
    fun `builtin mode includes diagnostic card for ECONNREFUSED to loopback`() {
        val manager = ErrorPageManager(
            ErrorPageConfig(
                mode = ErrorPageMode.BUILTIN_STYLE,
                showMiniGame = false,
                autoRetrySeconds = 0
            )
        )

        val html = manager.generateErrorPage(
            -6,
            "ECONNREFUSED",
            "http://127.0.0.1:3000"
        )

        assertThat(html).contains("本地服务未启动")
        assertThat(html).contains("diag-card")
        assertThat(html).contains("LOCAL_CONN_REFUSED")
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
        assertThat(html).contains("秒后重试")
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

        assertThat(html).doesNotContain("秒后重试")
        assertThat(html).doesNotContain("showGame()")
        assertThat(html).doesNotContain("gameOverlay")
    }
}
