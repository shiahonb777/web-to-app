package com.webtoapp.core.webview

import android.content.Context
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.WebViewConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = WebViewUnitTestApplication::class)
class StrictHostRuntimePolicyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val policy = StrictHostRuntimePolicy(context, WebViewUrlPolicy())

    @Test
    fun `build strict host origins keeps http https and www variants`() {
        val origins = policy.buildStrictHostOrigins("https://www.tiktok.com/@webtoapp")

        assertThat(origins).containsExactly(
            "https://tiktok.com",
            "http://tiktok.com",
            "https://www.tiktok.com",
            "http://www.tiktok.com"
        )
    }

    @Test
    fun `subresource requests inherit strict mode from referer`() {
        val request = FakeWebResourceRequest(
            rawUrl = "https://cdn.example.com/app.js",
            headers = mapOf("Referer" to "https://www.tiktok.com/@webtoapp")
        )

        val bypass = policy.shouldBypassAggressiveNetworkHooks(
            request = request,
            requestUrl = request.url.toString(),
            currentMainFrameUrl = null
        )

        assertThat(bypass).isTrue()
    }

    @Test
    fun `apply preload policy forces strict mobile ua for strict hosts`() {
        val webView = WebView(context)
        webView.settings.userAgentString = "custom-test-ua"

        policy.applyPreloadPolicyForUrl(
            webView = webView,
            pageUrl = "https://www.tiktok.com/@webtoapp",
            currentConfig = WebViewConfig(),
            currentDeviceDisguiseConfig = null
        )

        assertThat(webView.settings.userAgentString).isEqualTo(
            WebViewManager.STRICT_COMPAT_MOBILE_USER_AGENT
                ?: WebViewManager.STRICT_COMPAT_MOBILE_UA_FALLBACK
        )
    }
}
