package com.webtoapp.core.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WebViewShellConfigDarkModeTest {





    @Test
    fun `WebViewShellConfig dark mode status bar fields have correct defaults`() {
        val config = WebViewShellConfig()

        assertThat(config.statusBarColorDark).isNull()
        assertThat(config.statusBarBackgroundTypeDark).isEqualTo("COLOR")
    }

    @Test
    fun `WebViewShellConfig dark mode fields are independent from light mode`() {
        val config = WebViewShellConfig(
            statusBarColor = "#FFFFFF",
            statusBarBackgroundType = "IMAGE",
            statusBarColorDark = "#000000",
            statusBarBackgroundTypeDark = "IMAGE"
        )

        assertThat(config.statusBarColor).isEqualTo("#FFFFFF")
        assertThat(config.statusBarBackgroundType).isEqualTo("IMAGE")
        assertThat(config.statusBarColorDark).isEqualTo("#000000")
        assertThat(config.statusBarBackgroundTypeDark).isEqualTo("IMAGE")
    }

    @Test
    fun `WebViewShellConfig copy preserves dark mode fields`() {
        val original = WebViewShellConfig(
            statusBarColorDark = "#333333",
            statusBarBackgroundTypeDark = "IMAGE"
        )

        val copied = original.copy(statusBarColorDark = "#666666")

        assertThat(copied.statusBarColorDark).isEqualTo("#666666")
        assertThat(copied.statusBarBackgroundTypeDark).isEqualTo("IMAGE")
    }
}
