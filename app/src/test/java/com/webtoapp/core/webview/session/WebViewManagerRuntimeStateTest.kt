package com.webtoapp.core.webview

import android.app.Application
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
class WebViewManagerRuntimeStateTest {

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `runtime state stores current config url and retry metadata`() {
        val state = WebViewManagerRuntimeState()
        val config = WebViewConfig(javaScriptEnabled = false)

        state.currentConfig = config
        state.currentMainFrameUrl = "https://example.com"
        state.fileRetryUrl = "file:///android_asset/index.html"
        state.fileRetryCount = 2

        assertThat(state.currentConfig).isEqualTo(config)
        assertThat(state.currentMainFrameUrl).isEqualTo("https://example.com")
        assertThat(state.fileRetryUrl).isEqualTo("file:///android_asset/index.html")
        assertThat(state.fileRetryCount).isEqualTo(2)
    }

    @Test
    fun `runtime state tracks managed webviews without owning lifecycle`() {
        val state = WebViewManagerRuntimeState()
        val webView = WebView(application)

        state.managedWebViews[webView] = true

        assertThat(state.managedWebViews).containsKey(webView)
    }
}
