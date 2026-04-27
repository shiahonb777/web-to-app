package com.webtoapp.ui.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger

internal fun scheduleStrictHostFallbackProbe(
    context: Context,
    url: String?,
    source: String,
    delayMs: Long,
    webViewProvider: () -> WebView?,
    isTriggered: () -> Boolean,
    markTriggered: () -> Unit
) {
    if (!STRICT_HOST_AUTO_EXTERNAL_FALLBACK_ENABLED) return
    if (isTriggered() || !shouldSkipLongPressEnhancer(url)) return
    val expectedUrl = url

    webViewProvider()?.postDelayed({
        val activeWebView = webViewProvider() ?: return@postDelayed
        if (isTriggered()) return@postDelayed

        val current = activeWebView.url
        if (expectedUrl != null && expectedUrl != current) return@postDelayed

        activeWebView.evaluateJavascript(buildStrictHostProbeScript()) { raw ->
            if (isTriggered()) return@evaluateJavascript
            val decoded = decodeEvaluateJavascriptString(raw)
            if (!shouldFallbackToExternalForStrictHost(decoded)) return@evaluateJavascript
            markTriggered()
            AppLogger.w(
                "WebViewActivity",
                "Strict host blank-page probe ($source) triggered external fallback: $expectedUrl metrics=$decoded"
            )
            val fallbackUrl = expectedUrl ?: activeWebView.url.orEmpty()
            if (fallbackUrl.isNotBlank()) {
                val safeUrl = normalizeExternalUrlForIntent(fallbackUrl)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }, delayMs)
}

private fun buildStrictHostProbeScript(): String {
    return """
        (function() {
            try {
                var body = document.body;
                var root = document.documentElement;
                if (!body) return JSON.stringify({blank:true, reason:'no-body'});
                var text = (body.innerText || '').replace(/\s+/g, '');
                var textLength = text.length;
                var height = Math.max(body.scrollHeight || 0, root ? (root.scrollHeight || 0) : 0);
                var nodeCount = body.querySelectorAll('*').length;
                var videoCount = document.querySelectorAll('video').length;
                var imgCount = document.images ? document.images.length : 0;
                var blank = height < 900 && textLength < 80 && nodeCount < 120 && videoCount === 0 && imgCount < 5;
                return JSON.stringify({
                    blank: blank,
                    height: height,
                    textLength: textLength,
                    nodeCount: nodeCount,
                    videoCount: videoCount,
                    imgCount: imgCount
                });
            } catch (e) {
                return JSON.stringify({blank:false, error:String(e)});
            }
        })();
    """.trimIndent()
}
