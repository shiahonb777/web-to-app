package com.webtoapp.core.webview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.NewWindowBehavior
import com.webtoapp.data.model.WebViewConfig

internal class ManagedWebChromeClient(
    private val context: Context,
    private val config: WebViewConfig,
    private val callbacks: WebViewCallbacks,
    private val normalizeHttpUrlForSecurity: (String) -> String
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        callbacks.onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        callbacks.onTitleChanged(title)
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        callbacks.onIconReceived(icon)
    }

    override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        callbacks.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        callbacks.onHideCustomView()
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        callbacks.onGeolocationPermission(origin, callback)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        AppLogger.d("WebViewManager", "onPermissionRequest called: ${request?.resources?.joinToString()}")
        if (request != null) {
            callbacks.onPermissionRequest(request)
        } else {
            AppLogger.w("WebViewManager", "onPermissionRequest: request is null!")
        }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            val level = when (it.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> 4
                ConsoleMessage.MessageLevel.WARNING -> 3
                ConsoleMessage.MessageLevel.LOG -> 1
                ConsoleMessage.MessageLevel.DEBUG -> 0
                else -> 2
            }
            callbacks.onConsoleMessage(level, it.message() ?: "", it.sourceId() ?: "unknown", it.lineNumber())
        }
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return callbacks.onShowFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
    ): Boolean {
        if (view == null) return false

        val href = view.hitTestResult.extra
        AppLogger.d("WebViewManager", "onCreateWindow: href=$href, behavior=${config.newWindowBehavior}")

        val originalWebView = view
        return when (config.newWindowBehavior) {
            NewWindowBehavior.SAME_WINDOW -> {
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                if (transport != null) {
                    val tempWebView = WebView(context)
                    tempWebView.webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString()
                            if (url != null) {
                                val safeUrl = normalizeHttpUrlForSecurity(url)
                                originalWebView.loadUrl(safeUrl)
                                tempView?.destroy()
                            }
                            return true
                        }
                    }
                    transport.webView = tempWebView
                    resultMsg.sendToTarget()
                }
                true
            }
            NewWindowBehavior.EXTERNAL_BROWSER -> {
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                if (transport != null) {
                    val tempWebView = WebView(context)
                    tempWebView.webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldOverrideUrlLoading(tempView: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString()
                            if (url != null) {
                                try {
                                    val safeUrl = normalizeHttpUrlForSecurity(url)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(safeUrl))
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    AppLogger.e("WebViewManager", "Cannot open external browser: $url", e)
                                }
                                tempView?.destroy()
                            }
                            return true
                        }
                    }
                    transport.webView = tempWebView
                    resultMsg.sendToTarget()
                }
                true
            }
            NewWindowBehavior.POPUP_WINDOW -> {
                callbacks.onNewWindow(resultMsg)
                true
            }
            NewWindowBehavior.BLOCK -> false
        }
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        AppLogger.d("WebViewManager", "onCloseWindow")
    }
}
