package com.webtoapp.core.engine

import android.graphics.Bitmap
import android.view.View

/**
 * Note.
 * SystemWebView GeckoView.
 */
interface BrowserEngineCallback {

    /** load page. */
    fun onPageStarted(url: String?)

    /** load page. */
    fun onPageFinished(url: String?)

    /** load. */
    fun onProgressChanged(progress: Int)

    /** page. */
    fun onTitleChanged(title: String?)

    /** page. */
    fun onIconReceived(icon: Bitmap?)

    /** load page error. */
    fun onError(errorCode: Int, description: String)

    /** error. */
    fun onSslError(error: String)

    /** Note. */
    fun onExternalLink(url: String)

    /** Note. */
    fun onShowCustomView(view: View?, callback: Any?)

    /** Note. */
    fun onHideCustomView()

    /** download. */
    fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    )

    /** Note. */
    fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {}

    /** Note. */
    fun onNewWindow(resultMsg: android.os.Message?) {}
}