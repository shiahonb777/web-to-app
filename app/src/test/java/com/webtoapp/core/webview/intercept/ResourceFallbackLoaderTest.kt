package com.webtoapp.core.webview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.webview.intercept.ResourceFallbackLoader
import java.io.File
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = WebViewUnitTestApplication::class)
class ResourceFallbackLoaderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val loader = ResourceFallbackLoader(context)

    @Test
    fun `fetch cleartext resource returns server payload`() {
        withSingleResponseServer(
            body = "cleartext-ok",
            headers = mapOf("Content-Type" to "text/plain; charset=UTF-8")
        ) { port ->
            val response = loader.fetchCleartextResource(
                FakeWebResourceRequest("http://127.0.0.1:$port/cleartext")
            )

            assertThat(response).isNotNull()
            assertThat(response!!.statusCode).isEqualTo(200)
            assertThat(response.mimeType).isEqualTo("text/plain")
            assertThat(response.readBodyText()).isEqualTo("cleartext-ok")
        }
    }

    @Test
    fun `fetch with cross origin headers injects coop and coep`() {
        withSingleResponseServer(
            body = """{"status":"ok"}""",
            headers = mapOf("Content-Type" to "application/json")
        ) { port ->
            val response = loader.fetchWithCrossOriginHeaders(
                FakeWebResourceRequest("http://127.0.0.1:$port/coop")
            )

            assertThat(response).isNotNull()
            assertThat(response!!.responseHeaders["Cross-Origin-Opener-Policy"]).isEqualTo("same-origin")
            assertThat(response.responseHeaders["Cross-Origin-Embedder-Policy"]).isEqualTo("require-corp")
            assertThat(response.readBodyText()).contains("\"status\":\"ok\"")
        }
    }

    @Test
    fun `load local resource infers mime type from extension`() {
        val localFile = File(context.cacheDir, "resource-loader-${System.nanoTime()}.css")
        localFile.writeText("body { color: red; }")

        val response = loader.loadLocalResource(localFile.absolutePath)

        assertThat(response).isNotNull()
        assertThat(response!!.mimeType).isEqualTo("text/css")
        assertThat(loader.isTextMimeType(response.mimeType)).isTrue()
    }

    private fun withSingleResponseServer(
        body: String,
        headers: Map<String, String>,
        block: (Int) -> Unit
    ) {
        val bodyBytes = body.toByteArray()
        ServerSocket(0).use { server ->
            val worker = thread(start = true, isDaemon = true) {
                try {
                    server.accept().use { socket ->
                        val reader = socket.getInputStream().bufferedReader()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                        }

                        val responseHead = buildString {
                            append("HTTP/1.1 200 OK\r\n")
                            headers.forEach { (name, value) ->
                                append("$name: $value\r\n")
                            }
                            append("Content-Length: ${bodyBytes.size}\r\n")
                            append("Connection: close\r\n")
                            append("\r\n")
                        }.toByteArray()

                        socket.getOutputStream().use { output ->
                            output.write(responseHead)
                            output.write(bodyBytes)
                            output.flush()
                        }
                    }
                } catch (_: SocketException) {
                    // Test cleanup can close the socket before accept completes.
                }
            }

            try {
                block(server.localPort)
            } finally {
                server.close()
                worker.join(2_000)
            }
        }
    }
}
