package com.webtoapp.core.webview

import android.app.Application
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.File

internal class WebViewUnitTestApplication : Application()

internal class FakeWebResourceRequest(
    private val rawUrl: String,
    private val isMainFrame: Boolean = false,
    private val methodValue: String = "GET",
    private val headers: Map<String, String> = emptyMap(),
    private val hasUserGesture: Boolean = true,
    private val redirect: Boolean = false
) : WebResourceRequest {
    override fun getUrl(): Uri = Uri.parse(rawUrl)

    override fun isForMainFrame(): Boolean = isMainFrame

    override fun isRedirect(): Boolean = redirect

    override fun hasGesture(): Boolean = hasUserGesture

    override fun getMethod(): String = methodValue

    override fun getRequestHeaders(): MutableMap<String, String> = headers.toMutableMap()
}

internal fun WebResourceResponse.readBodyText(): String {
    return data?.bufferedReader()?.use { it.readText() }.orEmpty()
}

internal fun File.asLocalhostResourceUrl(): String {
    return "https://localhost/__local__/${absolutePath.replace('\\', '/')}"
}
