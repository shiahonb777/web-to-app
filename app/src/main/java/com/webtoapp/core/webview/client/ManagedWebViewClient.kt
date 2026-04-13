package com.webtoapp.core.webview

import android.graphics.Bitmap
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

internal class ManagedWebViewClient(
    private val shouldInterceptRequestHandler: (WebView?, WebResourceRequest?, WebViewClient) -> WebResourceResponse?,
    private val shouldOverrideUrlLoadingHandler: (WebView?, WebResourceRequest?) -> Boolean,
    private val onPageStartedHandler: (WebView?, String?, Bitmap?) -> Unit,
    private val onPageCommitVisibleHandler: (WebView?, String?) -> Unit,
    private val doUpdateVisitedHistoryHandler: (WebView?, String?, Boolean) -> Unit,
    private val onPageFinishedHandler: (WebView?, String?) -> Unit,
    private val onReceivedErrorHandler: (WebView?, WebResourceRequest?, WebResourceError?) -> Unit,
    private val onReceivedHttpErrorHandler: (WebView?, WebResourceRequest?, WebResourceResponse?) -> Unit,
    private val onReceivedSslErrorHandler: (WebView?, SslErrorHandler?, android.net.http.SslError?) -> Unit,
    private val onRenderProcessGoneHandler: (WebView?, RenderProcessGoneDetail?) -> Boolean
) : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? = shouldInterceptRequestHandler(view, request, this)

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean = shouldOverrideUrlLoadingHandler(view, request)

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStartedHandler(view, url, favicon)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        onPageCommitVisibleHandler(view, url)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        doUpdateVisitedHistoryHandler(view, url, isReload)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinishedHandler(view, url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        onReceivedErrorHandler(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        onReceivedHttpErrorHandler(view, request, errorResponse)
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: android.net.http.SslError?
    ) {
        onReceivedSslErrorHandler(view, handler, error)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        return onRenderProcessGoneHandler(view, detail)
    }
}
