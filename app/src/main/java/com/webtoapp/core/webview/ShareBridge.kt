package com.webtoapp.core.webview

import android.content.Context
import android.webkit.JavascriptInterface
import com.webtoapp.core.logging.AppLogger

/**
 * navigator.share bridge class
 * Provide system share functionality
 */
class ShareBridge(private val context: Context) {
    
    /**
     * Share text
     */
    @JavascriptInterface
    fun shareText(title: String?, text: String?, url: String?) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                if (!title.isNullOrEmpty()) {
                    putExtra(android.content.Intent.EXTRA_SUBJECT, title)
                }
                val shareText = buildString {
                    if (!text.isNullOrEmpty()) append(text)
                    if (!url.isNullOrEmpty()) {
                        if (isNotEmpty()) append("\n")
                        append(url)
                    }
                }
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }
            val chooser = android.content.Intent.createChooser(intent, title ?: "Share")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            AppLogger.e("ShareBridge", "Share failed", e)
        }
    }
    
    /**
     * Check if sharing is supported
     */
    @JavascriptInterface
    fun canShare(): Boolean = true
}
