package com.webtoapp.core.webview

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = WebViewUnitTestApplication::class)
class WebViewNavigationHandlerTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val handler = WebViewNavigationHandler(
        urlPolicy = WebViewUrlPolicy(),
        strictHostRuntimePolicy = StrictHostRuntimePolicy(application, WebViewUrlPolicy()),
        specialUrlHandler = SpecialUrlHandler(application, WebViewUrlPolicy()),
        getCurrentMainFrameUrl = { "https://example.com" },
        getCurrentConfig = { null },
        getManagedWebViews = { emptyList() },
        getShields = { null }
    )

    @Test
    fun `same-site hosts are not treated as external`() {
        assertThat(
            handler.isExternalUrl(
                targetUrl = "https://cdn.example.com/assets/app.js",
                currentUrl = "https://example.com/home"
            )
        ).isFalse()
    }

    @Test
    fun `different hosts are treated as external`() {
        assertThat(
            handler.isExternalUrl(
                targetUrl = "https://accounts.google.com/signin",
                currentUrl = "https://example.com/home"
            )
        ).isTrue()
    }
}
