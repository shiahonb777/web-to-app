package com.webtoapp.core.webview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.webview.intercept.RequestInterceptionCoordinator
import com.webtoapp.core.webview.intercept.ResourceFallbackLoader
import com.webtoapp.data.model.WebViewConfig
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = WebViewUnitTestApplication::class)
class RequestInterceptionCoordinatorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val coordinator = RequestInterceptionCoordinator(
        context = context,
        adBlocker = AdBlocker(),
        urlPolicy = WebViewUrlPolicy(),
        resourceFallbackLoader = ResourceFallbackLoader(context)
    )

    @Test
    fun `intercept serves localhost local resources through fallback loader`() {
        val localFile = File(context.cacheDir, "request-intercept-${System.nanoTime()}.json")
        localFile.writeText("""{"ok":true}""")

        val result = coordinator.intercept(
            request = FakeWebResourceRequest(localFile.asLocalhostResourceUrl()),
            config = WebViewConfig(),
            currentMainFrameUrl = "https://example.com",
            shields = null,
            diag = RequestInterceptionCoordinator.DiagSnapshot(
                requestCount = 1,
                blockedCount = 0,
                errorCount = 0,
                pageStartTime = 0L
            ),
            shouldBypassAggressiveNetworkHooks = { _, _ -> false }
        )

        assertThat(result.blocked).isFalse()
        assertThat(result.response).isNotNull()
        val response = result.response ?: error("Expected localhost resource response")
        assertThat(response.mimeType).isEqualTo("application/json")
        assertThat(response.readBodyText()).contains("\"ok\":true")
    }

    @Test
    fun `infer resource type uses accept header before extension guessing`() {
        val request = FakeWebResourceRequest(
            rawUrl = "https://cdn.example.com/assets/no-extension",
            headers = mapOf("Accept" to "text/css,*/*;q=0.1")
        )

        assertThat(coordinator.inferResourceType(request)).isEqualTo("stylesheet")
    }
}
