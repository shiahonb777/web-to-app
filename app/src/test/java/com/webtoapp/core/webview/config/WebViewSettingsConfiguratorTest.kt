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
class WebViewSettingsConfiguratorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val configurator = WebViewSettingsConfigurator()

    @Test
    fun `active chrome extensions auto switch to desktop ua`() {
        val webView = WebView(context)

        configurator.apply(
            webView = webView,
            config = WebViewConfig(),
            effectiveUserAgent = null,
            isDesktopModeRequested = false,
            preferLandscapeEmbeddedViewport = false,
            hasActiveChromeExtension = true,
            desktopUserAgent = "Desktop Test UA"
        )

        assertThat(webView.settings.userAgentString).isEqualTo("Desktop Test UA")
    }

    @Test
    fun `landscape embedded viewport disables overview shrink fit`() {
        val webView = WebView(context)

        configurator.apply(
            webView = webView,
            config = WebViewConfig(),
            effectiveUserAgent = null,
            isDesktopModeRequested = false,
            preferLandscapeEmbeddedViewport = true,
            hasActiveChromeExtension = false,
            desktopUserAgent = "Desktop Test UA"
        )

        assertThat(webView.settings.loadWithOverviewMode).isFalse()
    }
}
