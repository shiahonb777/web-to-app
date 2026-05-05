package com.webtoapp.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WebViewConfigDarkModeTest {





    @Test
    fun `WebViewConfig dark mode status bar fields have correct defaults`() {
        val config = WebViewConfig()

        assertThat(config.statusBarColorModeDark).isEqualTo(StatusBarColorMode.THEME)
        assertThat(config.statusBarColorDark).isNull()
        assertThat(config.statusBarDarkIconsDark).isFalse()
        assertThat(config.statusBarBackgroundTypeDark).isEqualTo(StatusBarBackgroundType.COLOR)
        assertThat(config.statusBarBackgroundImageDark).isNull()
        assertThat(config.statusBarBackgroundAlphaDark).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun `WebViewConfig light mode status bar fields have correct defaults`() {
        val config = WebViewConfig()

        assertThat(config.statusBarColorMode).isEqualTo(StatusBarColorMode.THEME)
        assertThat(config.statusBarColor).isNull()
        assertThat(config.statusBarDarkIcons).isNull()
        assertThat(config.statusBarBackgroundType).isEqualTo(StatusBarBackgroundType.COLOR)
        assertThat(config.statusBarBackgroundImage).isNull()
        assertThat(config.statusBarBackgroundAlpha).isWithin(0.01f).of(1.0f)
    }





    @Test
    fun `WebViewConfig dark mode fields are independent from light mode`() {
        val config = WebViewConfig(
            statusBarColorMode = StatusBarColorMode.CUSTOM,
            statusBarColor = "#FFFFFF",
            statusBarDarkIcons = true,
            statusBarColorModeDark = StatusBarColorMode.TRANSPARENT,
            statusBarColorDark = "#000000",
            statusBarDarkIconsDark = false
        )

        assertThat(config.statusBarColorMode).isEqualTo(StatusBarColorMode.CUSTOM)
        assertThat(config.statusBarColor).isEqualTo("#FFFFFF")
        assertThat(config.statusBarDarkIcons).isTrue()

        assertThat(config.statusBarColorModeDark).isEqualTo(StatusBarColorMode.TRANSPARENT)
        assertThat(config.statusBarColorDark).isEqualTo("#000000")
        assertThat(config.statusBarDarkIconsDark).isFalse()
    }

    @Test
    fun `WebViewConfig copy preserves dark mode fields`() {
        val original = WebViewConfig(
            statusBarColorModeDark = StatusBarColorMode.CUSTOM,
            statusBarColorDark = "#333333",
            statusBarDarkIconsDark = true,
            statusBarBackgroundTypeDark = StatusBarBackgroundType.IMAGE,
            statusBarBackgroundImageDark = "dark_bg.png",
            statusBarBackgroundAlphaDark = 0.8f
        )

        val copied = original.copy(statusBarColorDark = "#666666")

        assertThat(copied.statusBarColorModeDark).isEqualTo(StatusBarColorMode.CUSTOM)
        assertThat(copied.statusBarColorDark).isEqualTo("#666666")
        assertThat(copied.statusBarDarkIconsDark).isTrue()
        assertThat(copied.statusBarBackgroundTypeDark).isEqualTo(StatusBarBackgroundType.IMAGE)
        assertThat(copied.statusBarBackgroundImageDark).isEqualTo("dark_bg.png")
        assertThat(copied.statusBarBackgroundAlphaDark).isWithin(0.01f).of(0.8f)
    }





    @Test
    fun `StatusBarColorMode enum has all expected values`() {
        assertThat(StatusBarColorMode.values().map { it.name }).containsExactly(
            "THEME", "CUSTOM", "TRANSPARENT"
        )
    }

    @Test
    fun `StatusBarColorMode THEME is default`() {
        assertThat(StatusBarColorMode.THEME.name).isEqualTo("THEME")
    }





    @Test
    fun `StatusBarBackgroundType enum has all expected values`() {
        assertThat(StatusBarBackgroundType.values().map { it.name }).containsExactly(
            "COLOR", "IMAGE"
        )
    }

    @Test
    fun `StatusBarBackgroundType COLOR is default`() {
        assertThat(StatusBarBackgroundType.COLOR.name).isEqualTo("COLOR")
    }





    @Test
    fun `OrientationMode enum has AUTO value`() {
        assertThat(OrientationMode.values().map { it.name }).contains("AUTO")
    }

    @Test
    fun `OrientationMode enum has all expected values`() {
        assertThat(OrientationMode.values().map { it.name }).containsExactly(
            "PORTRAIT", "LANDSCAPE", "REVERSE_PORTRAIT", "REVERSE_LANDSCAPE",
            "SENSOR_PORTRAIT", "SENSOR_LANDSCAPE", "AUTO"
        )
    }





    @Test
    fun `WebViewConfig supports IMAGE background type for dark mode`() {
        val config = WebViewConfig(
            statusBarBackgroundTypeDark = StatusBarBackgroundType.IMAGE,
            statusBarBackgroundImageDark = "dark_statusbar.png",
            statusBarBackgroundAlphaDark = 0.7f
        )

        assertThat(config.statusBarBackgroundTypeDark).isEqualTo(StatusBarBackgroundType.IMAGE)
        assertThat(config.statusBarBackgroundImageDark).isEqualTo("dark_statusbar.png")
        assertThat(config.statusBarBackgroundAlphaDark).isWithin(0.01f).of(0.7f)
    }

    @Test
    fun `WebViewConfig supports IMAGE background type for dark mode with null image`() {
        val config = WebViewConfig(
            statusBarBackgroundTypeDark = StatusBarBackgroundType.IMAGE,
            statusBarBackgroundImageDark = null
        )

        assertThat(config.statusBarBackgroundTypeDark).isEqualTo(StatusBarBackgroundType.IMAGE)
        assertThat(config.statusBarBackgroundImageDark).isNull()
    }





    @Test
    fun `WebViewConfig with all dark mode fields set preserves values through copy`() {
        val full = WebViewConfig(
            statusBarColorModeDark = StatusBarColorMode.CUSTOM,
            statusBarColorDark = "#1a1a2e",
            statusBarDarkIconsDark = false,
            statusBarBackgroundTypeDark = StatusBarBackgroundType.IMAGE,
            statusBarBackgroundImageDark = "dark_bg.png",
            statusBarBackgroundAlphaDark = 0.9f
        )

        val roundTripped = full.copy()

        assertThat(roundTripped.statusBarColorModeDark).isEqualTo(full.statusBarColorModeDark)
        assertThat(roundTripped.statusBarColorDark).isEqualTo(full.statusBarColorDark)
        assertThat(roundTripped.statusBarDarkIconsDark).isEqualTo(full.statusBarDarkIconsDark)
        assertThat(roundTripped.statusBarBackgroundTypeDark).isEqualTo(full.statusBarBackgroundTypeDark)
        assertThat(roundTripped.statusBarBackgroundImageDark).isEqualTo(full.statusBarBackgroundImageDark)
        assertThat(roundTripped.statusBarBackgroundAlphaDark).isWithin(0.01f).of(full.statusBarBackgroundAlphaDark)
    }
}
