package com.webtoapp.core.webview

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = WebViewUnitTestApplication::class)
class WebViewUrlPolicyTest {

    private val policy = WebViewUrlPolicy()

    @Test
    fun `local cleartext hosts stay out of conservative modes`() {
        assertThat(policy.shouldUseConservativeScriptMode("http://127.0.0.1:8080/index.html")).isFalse()
        assertThat(policy.shouldUseScriptlessMode("http://127.0.0.1:8080/index.html")).isFalse()
    }

    @Test
    fun `strict compat hosts enter scriptless mode`() {
        val url = "https://www.tiktok.com/@webtoapp"

        assertThat(policy.shouldUseConservativeScriptMode(url)).isTrue()
        assertThat(policy.shouldUseScriptlessMode(url)).isTrue()
    }

    @Test
    fun `registrable domain and map tile detection stay stable`() {
        assertThat(policy.getRegistrableDomain("static.demo.co.uk")).isEqualTo("demo.co.uk")
        assertThat(policy.isSameSiteHost("api.example.com", "cdn.example.com")).isTrue()
        assertThat(policy.isMapTileRequest("https://tile.openstreetmap.org/1/2/3.png")).isTrue()
    }
}
