package com.webtoapp.core.webview

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*

/**
 * WebView callback interface
 */
interface WebViewCallbacks {
    fun onPageStarted(url: String?)
    fun onPageCommitVisible(url: String?) {}
    fun onPageFinished(url: String?)
    fun onProgressChanged(progress: Int)
    fun onTitleChanged(title: String?)
    fun onIconReceived(icon: Bitmap?)
    fun onError(errorCode: Int, description: String)
    fun onSslError(error: String)
    fun onExternalLink(url: String)
    fun onShowCustomView(view: android.view.View?, callback: WebChromeClient.CustomViewCallback?)
    fun onHideCustomView()
    fun onGeolocationPermission(origin: String?, callback: GeolocationPermissions.Callback?)
    fun onPermissionRequest(request: PermissionRequest?)
    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean
    
    /**
     * Download request callback
     * @param url Download URL
     * @param userAgent User-Agent
     * @param contentDisposition Content-Disposition header
     * @param mimeType MIME type
     * @param contentLength File size
     */
    fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    )
    
    /**
     * Long press event callback
     * @param webView WebView instance
     * @param x Long press X coordinate
     * @param y Long press Y coordinate
     * @return Whether this event is consumed
     */
    fun onLongPress(webView: WebView, x: Float, y: Float): Boolean = false
    
    /**
     * Console message callback
     * @param level Log level
     * @param message Message content
     * @param sourceId Source file
     * @param lineNumber Line number
     */
    fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {}
    
    /**
     * URL changed callback (triggered by SPA navigation via pushState/replaceState)
     * Use this to update canGoBack/canGoForward state in real time
     * @param webView The WebView instance
     * @param url The new URL
     */
    fun onUrlChanged(webView: WebView?, url: String?) {}
    
    /**
     * New window request callback (window.open / target="_blank")
     * @param resultMsg Message for passing to new window
     */
    fun onNewWindow(resultMsg: android.os.Message?) {}
    
    /**
     * WebView render process gone callback.
     * Called when the renderer crashes or is killed by the system (e.g. memory pressure).
     * Implementations should recreate the WebView and reload the page.
     * @param didCrash true if the renderer crashed, false if killed by system
     */
    fun onRenderProcessGone(didCrash: Boolean) {}
}
