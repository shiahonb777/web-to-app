package com.webtoapp.core.webview

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.WebViewConfig
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = WebViewUnitTestApplication::class)
class SpecialUrlHandlerTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val handler = SpecialUrlHandler(application, WebViewUrlPolicy())

    @Before
    fun clearStartedActivities() {
        val shadowApplication = shadowOf(application)
        while (shadowApplication.nextStartedActivity != null) {
            // Drain leftover started activities from previous assertions.
        }
    }

    @Test
    fun `javascript scheme is blocked without launching anything`() {
        val handled = handler.handleSpecialUrl(
            url = "javascript:alert('xss')",
            isUserGesture = true,
            currentMainFrameUrl = "https://example.com",
            currentConfig = WebViewConfig(),
            managedWebViews = emptyList(),
            shouldUseScriptlessMode = { false }
        )

        assertThat(handled).isTrue()
        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    @Test
    fun `payment schemes respect config toggle`() {
        val handled = handler.handleSpecialUrl(
            url = "weixin://wap/pay",
            isUserGesture = true,
            currentMainFrameUrl = "https://example.com",
            currentConfig = WebViewConfig(enablePaymentSchemes = false),
            managedWebViews = emptyList(),
            shouldUseScriptlessMode = { false }
        )

        assertThat(handled).isTrue()
        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    @Test
    fun `system browser launch upgrades insecure http urls`() {
        handler.openInSystemBrowser("http://example.com/path")

        val intent = shadowOf(application).nextStartedActivity
        assertThat(intent).isNotNull()
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.dataString).isEqualTo("https://example.com/path")
    }
}
