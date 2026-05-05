package com.webtoapp.core.webview

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*




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









    fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    )








    fun onLongPress(webView: WebView, x: Float, y: Float): Boolean = false








    fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {}







    fun onUrlChanged(webView: WebView?, url: String?) {}





    fun onNewWindow(resultMsg: android.os.Message?) {}







    fun onRenderProcessGone(didCrash: Boolean) {}
}
