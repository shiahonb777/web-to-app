package com.webtoapp.core.webview

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebViewConfig

internal class ManagedWebChromeClient(
    private val config: WebViewConfig,
    private val callbacks: WebViewCallbacks,
    private val specialUrlHandler: SpecialUrlHandler
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
        return specialUrlHandler.handleNewWindowRequest(
            view = view,
            resultMsg = resultMsg,
            behavior = config.newWindowBehavior,
            href = view.hitTestResult.extra,
            onPopupWindow = callbacks::onNewWindow
        )
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        AppLogger.d("WebViewManager", "onCloseWindow")
    }
}
