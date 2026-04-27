package com.webtoapp.core.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CloudSdkConfigTest {

    // ═══════════════════════════════════════════
    // isValid
    // ═══════════════════════════════════════════

    @Test
    fun `isValid returns false when disabled`() {
        val config = CloudSdkConfig(enabled = false, projectKey = "abc")
        assertThat(config.isValid()).isFalse()
    }

    @Test
    fun `isValid returns false when enabled but blank projectKey`() {
        val config = CloudSdkConfig(enabled = true, projectKey = "")
        assertThat(config.isValid()).isFalse()
    }

    @Test
    fun `isValid returns false when enabled but blank projectKey whitespace`() {
        val config = CloudSdkConfig(enabled = true, projectKey = "   ")
        assertThat(config.isValid()).isFalse()
    }

    @Test
    fun `isValid returns true when enabled and projectKey present`() {
        val config = CloudSdkConfig(enabled = true, projectKey = "proj-123")
        assertThat(config.isValid()).isTrue()
    }

    // ═══════════════════════════════════════════
    // DISABLED preset
    // ═══════════════════════════════════════════

    @Test
    fun `DISABLED preset is not valid`() {
        assertThat(CloudSdkConfig.DISABLED.isValid()).isFalse()
    }

    @Test
    fun `DISABLED preset has default apiBaseUrl`() {
        assertThat(CloudSdkConfig.DISABLED.apiBaseUrl).isEqualTo(CloudSdkConfig.DEFAULT_API_BASE_URL)
    }

    // ═══════════════════════════════════════════
    // getSdkApiUrl
    // ═══════════════════════════════════════════

    @Test
    fun `getSdkApiUrl constructs correct path`() {
        val config = CloudSdkConfig(projectKey = "abc-123")
        val url = config.getSdkApiUrl("check-update")
        assertThat(url).isEqualTo("https://api.shiaho.sbs/api/v1/sdk/abc-123/check-update")
    }

    @Test
    fun `getSdkApiUrl handles custom apiBaseUrl with trailing slash`() {
        val config = CloudSdkConfig(
            projectKey = "my-proj",
            apiBaseUrl = "https://custom.api.com/"
        )
        val url = config.getSdkApiUrl("announcements")
        assertThat(url).isEqualTo("https://custom.api.com/api/v1/sdk/my-proj/announcements")
    }

    @Test
    fun `getSdkApiUrl handles custom apiBaseUrl without trailing slash`() {
        val config = CloudSdkConfig(
            projectKey = "my-proj",
            apiBaseUrl = "https://custom.api.com"
        )
        val url = config.getSdkApiUrl("config")
        assertThat(url).isEqualTo("https://custom.api.com/api/v1/sdk/my-proj/config")
    }

    // ═══════════════════════════════════════════
    // Default values
    // ═══════════════════════════════════════════

    @Test
    fun `default config has sensible defaults`() {
        val config = CloudSdkConfig()
        assertThat(config.enabled).isFalse()
        assertThat(config.updateCheckEnabled).isTrue()
        assertThat(config.announcementEnabled).isTrue()
        assertThat(config.remoteConfigEnabled).isTrue()
        assertThat(config.activationCodeEnabled).isFalse()
        assertThat(config.statsReportEnabled).isTrue()
        assertThat(config.fcmPushEnabled).isFalse()
        assertThat(config.updateCheckInterval).isEqualTo(3600)
        assertThat(config.statsReportInterval).isEqualTo(3600)
        assertThat(config.activationBindDevice).isTrue()
        assertThat(config.inAppDownload).isTrue()
    }
}
