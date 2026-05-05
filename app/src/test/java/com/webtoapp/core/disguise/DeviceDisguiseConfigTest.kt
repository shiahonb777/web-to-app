package com.webtoapp.core.disguise

import com.webtoapp.data.model.UserAgentMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeviceDisguiseConfigTest {





    @Test
    fun `toUserAgentMode returns DEFAULT when disabled`() {
        val config = DeviceDisguiseConfig(enabled = false)
        assertThat(config.toUserAgentMode()).isEqualTo(UserAgentMode.DEFAULT)
    }

    @Test
    fun `toUserAgentMode returns CUSTOM when customUserAgent set`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            customUserAgent = "MyCustomUA/1.0"
        )
        assertThat(config.toUserAgentMode()).isEqualTo(UserAgentMode.CUSTOM)
    }

    @Test
    fun `toUserAgentMode returns CHROME_MOBILE for Android phone`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.PHONE,
            deviceOS = DeviceOS.ANDROID
        )
        assertThat(config.toUserAgentMode()).isEqualTo(UserAgentMode.CHROME_MOBILE)
    }

    @Test
    fun `toUserAgentMode returns SAFARI_MOBILE for iOS phone`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.PHONE,
            deviceOS = DeviceOS.IOS
        )
        assertThat(config.toUserAgentMode()).isEqualTo(UserAgentMode.SAFARI_MOBILE)
    }

    @Test
    fun `toUserAgentMode returns CHROME_DESKTOP for Windows desktop`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.DESKTOP,
            deviceOS = DeviceOS.WINDOWS
        )
        assertThat(config.toUserAgentMode()).isEqualTo(UserAgentMode.CHROME_DESKTOP)
    }

    @Test
    fun `toUserAgentMode returns SAFARI_DESKTOP for macOS desktop`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.DESKTOP,
            deviceOS = DeviceOS.MACOS
        )
        assertThat(config.toUserAgentMode()).isEqualTo(UserAgentMode.SAFARI_DESKTOP)
    }

    @Test
    fun `toUserAgentMode returns FIREFOX_DESKTOP for Linux desktop`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.DESKTOP,
            deviceOS = DeviceOS.LINUX
        )
        assertThat(config.toUserAgentMode()).isEqualTo(UserAgentMode.FIREFOX_DESKTOP)
    }





    @Test
    fun `generateUserAgent returns empty string when disabled`() {
        val config = DeviceDisguiseConfig(enabled = false)
        assertThat(config.generateUserAgent()).isEmpty()
    }

    @Test
    fun `generateUserAgent returns custom UA when set`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            customUserAgent = "CustomAgent/2.0"
        )
        assertThat(config.generateUserAgent()).isEqualTo("CustomAgent/2.0")
    }

    @Test
    fun `generateUserAgent for Android phone contains Chrome`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.PHONE,
            deviceOS = DeviceOS.ANDROID,
            deviceModel = "Pixel10"
        )
        assertThat(config.generateUserAgent()).contains("Chrome")
        assertThat(config.generateUserAgent()).contains("Android")
    }

    @Test
    fun `generateUserAgent for iOS phone contains Safari`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.PHONE,
            deviceOS = DeviceOS.IOS
        )
        assertThat(config.generateUserAgent()).contains("Safari")
        assertThat(config.generateUserAgent()).contains("iPhone")
    }

    @Test
    fun `generateUserAgent for Windows desktop contains Windows NT`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.DESKTOP,
            deviceOS = DeviceOS.WINDOWS
        )
        assertThat(config.generateUserAgent()).contains("Windows NT")
    }





    @Test
    fun `requiresDesktopViewport returns false when disabled`() {
        val config = DeviceDisguiseConfig(enabled = false)
        assertThat(config.requiresDesktopViewport()).isFalse()
    }

    @Test
    fun `requiresDesktopViewport returns true for DESKTOP type`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.DESKTOP
        )
        assertThat(config.requiresDesktopViewport()).isTrue()
    }

    @Test
    fun `requiresDesktopViewport returns true for LAPTOP type`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.LAPTOP
        )
        assertThat(config.requiresDesktopViewport()).isTrue()
    }

    @Test
    fun `requiresDesktopViewport returns false for PHONE type`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.PHONE
        )
        assertThat(config.requiresDesktopViewport()).isFalse()
    }

    @Test
    fun `requiresDesktopViewport returns true when isDesktopViewport flag set`() {
        val config = DeviceDisguiseConfig(
            enabled = true,
            deviceType = DeviceType.PHONE,
            isDesktopViewport = true
        )
        assertThat(config.requiresDesktopViewport()).isTrue()
    }





    @Test
    fun `getBrandsForType PHONE returns phone brands`() {
        val brands = DeviceBrand.getBrandsForType(DeviceType.PHONE)
        assertThat(brands).isNotEmpty()
        assertThat(brands.map { it }).contains(DeviceBrand.SAMSUNG)
        assertThat(brands.map { it }).contains(DeviceBrand.APPLE)
    }

    @Test
    fun `getBrandsForType DESKTOP returns desktop brands`() {
        val brands = DeviceBrand.getBrandsForType(DeviceType.DESKTOP)
        assertThat(brands).isNotEmpty()
        assertThat(brands.map { it }).contains(DeviceBrand.GENERIC_WINDOWS)
        assertThat(brands.map { it }).contains(DeviceBrand.GENERIC_MAC)
    }

    @Test
    fun `getBrandsForType WATCH returns watch brands`() {
        val brands = DeviceBrand.getBrandsForType(DeviceType.WATCH)
        assertThat(brands).isNotEmpty()
        assertThat(brands.map { it }).contains(DeviceBrand.APPLE_WATCH)
    }





    @Test
    fun `DeviceType has all expected values`() {
        assertThat(DeviceType.values()).asList().containsExactly(
            DeviceType.PHONE,
            DeviceType.TABLET,
            DeviceType.DESKTOP,
            DeviceType.LAPTOP,
            DeviceType.WATCH,
            DeviceType.TV
        )
    }





    @Test
    fun `DeviceOS has all expected values`() {
        assertThat(DeviceOS.values()).asList().containsExactly(
            DeviceOS.ANDROID,
            DeviceOS.IOS,
            DeviceOS.HARMONYOS,
            DeviceOS.WINDOWS,
            DeviceOS.MACOS,
            DeviceOS.LINUX,
            DeviceOS.CHROMEOS,
            DeviceOS.WATCHOS,
            DeviceOS.WEAROS,
            DeviceOS.TVOS
        )
    }

    @Test
    fun `each DeviceOS has non-blank displayName`() {
        for (os in DeviceOS.values()) {
            assertThat(os.displayName).isNotEmpty()
        }
    }
}
