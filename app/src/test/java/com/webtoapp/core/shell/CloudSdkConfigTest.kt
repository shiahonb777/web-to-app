package com.webtoapp.core.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CloudSdkConfigTest {





    @Test
    fun `isValid returns false when disabled`() {
        val config = CloudSdkConfig(enabled = false, runtimeKey = "rt-abc")
        assertThat(config.isValid()).isFalse()
    }

    @Test
    fun `isValid returns false when enabled but blank runtimeKey`() {
        val config = CloudSdkConfig(enabled = true, runtimeKey = "")
        assertThat(config.isValid()).isFalse()
    }

    @Test
    fun `isValid returns false when enabled but blank runtimeKey whitespace`() {
        val config = CloudSdkConfig(enabled = true, runtimeKey = "   ")
        assertThat(config.isValid()).isFalse()
    }

    @Test
    fun `isValid returns true when enabled and runtimeKey present`() {
        val config = CloudSdkConfig(enabled = true, runtimeKey = "rt-123")
        assertThat(config.isValid()).isTrue()
    }

    @Test
    fun `isValid returns true when runtimeKey present`() {
        val config = CloudSdkConfig(enabled = true, runtimeKey = "rt-123")
        assertThat(config.isValid()).isTrue()
    }





    @Test
    fun `DISABLED preset is not valid`() {
        assertThat(CloudSdkConfig.DISABLED.isValid()).isFalse()
    }

    @Test
    fun `DISABLED preset has default apiBaseUrl`() {
        assertThat(CloudSdkConfig.DISABLED.apiBaseUrl).isEqualTo(CloudSdkConfig.DEFAULT_API_BASE_URL)
    }





    @Test
    fun `getSdkApiUrl constructs correct path`() {
        val config = CloudSdkConfig(runtimeKey = "rt-abc-123")
        val url = config.getSdkApiUrl("check-update")
        assertThat(url).isEqualTo("https://api.shiaho.sbs/api/v1/sdk/rt-abc-123/check-update")
    }

    @Test
    fun `getSdkApiUrl handles custom apiBaseUrl with trailing slash`() {
        val config = CloudSdkConfig(
            runtimeKey = "rt-my-proj",
            apiBaseUrl = "https://custom.api.com/"
        )
        val url = config.getSdkApiUrl("announcements")
        assertThat(url).isEqualTo("https://custom.api.com/api/v1/sdk/rt-my-proj/announcements")
    }

    @Test
    fun `getSdkApiUrl handles custom apiBaseUrl without trailing slash`() {
        val config = CloudSdkConfig(
            runtimeKey = "rt-my-proj",
            apiBaseUrl = "https://custom.api.com"
        )
        val url = config.getSdkApiUrl("config")
        assertThat(url).isEqualTo("https://custom.api.com/api/v1/sdk/rt-my-proj/config")
    }





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
