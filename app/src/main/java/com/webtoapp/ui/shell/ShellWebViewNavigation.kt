package com.webtoapp.ui.shell

import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

object ShellWebViewNavigation {

    fun goBackOrFinish(activity: AppCompatActivity, webView: WebView?) {
        val wv = webView ?: run {
            activity.finish()
            return
        }

        val list = wv.copyBackForwardList()
        val currentIndex = list.currentIndex
        val currentItem = list.getItemAtIndex(currentIndex)
        val previousItem = if (currentIndex > 0) list.getItemAtIndex(currentIndex - 1) else null
        val currentUrls = listOf(
            currentItem?.url.orEmpty(),
            currentItem?.originalUrl.orEmpty(),
            wv.url.orEmpty(),
            wv.originalUrl.orEmpty()
        )
        val previousUrls = listOf(
            previousItem?.url.orEmpty(),
            previousItem?.originalUrl.orEmpty()
        )

        if (!wv.canGoBack() || currentIndex <= 0 || shouldFinishInsteadOfBack(currentUrls, previousUrls)) {
            activity.finish()
            return
        }

        wv.goBack()
    }

    internal fun shouldFinishInsteadOfBack(currentUrl: String, previousUrl: String): Boolean {
        return shouldFinishInsteadOfBack(listOf(currentUrl), listOf(previousUrl))
    }

    private fun shouldFinishInsteadOfBack(currentUrls: List<String>, previousUrls: List<String>): Boolean {
        val currentHasBlank = currentUrls.any(::isBlankPage)
        val currentHasError = currentUrls.any(::isGeneratedErrorPage)
        val currentHasLocal = currentUrls.any(::isLocalRuntimeUrl)
        val previousHasBlank = previousUrls.any(::isBlankPage)
        val previousHasError = previousUrls.any(::isGeneratedErrorPage)
        val previousHasLocal = previousUrls.any(::isLocalRuntimeUrl)

        return previousHasBlank ||
            previousHasError ||
            currentHasBlank && (previousHasLocal || previousHasError) ||
            currentHasError && (previousHasLocal || previousHasBlank) ||
            currentHasLocal && previousHasError
    }

    private fun isBlankPage(url: String): Boolean {
        return url.isBlank() || url == "about:blank"
    }

    private fun isGeneratedErrorPage(url: String): Boolean {
        return url.startsWith("data:text/html", ignoreCase = true) ||
            url.startsWith("data:text/plain", ignoreCase = true)
    }

    private fun isLocalRuntimeUrl(url: String): Boolean {
        return url.startsWith("http://127.0.0.1:", ignoreCase = true) ||
            url.startsWith("http://localhost:", ignoreCase = true)
    }
}
