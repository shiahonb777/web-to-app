package com.webtoapp.ui.webview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.webview.WebViewManager

@Composable
internal fun rememberWebViewManager(
    context: Context,
    adBlocker: AdBlocker
): WebViewManager {
    return remember(context, adBlocker) {
        WebViewManager(context, adBlocker)
    }
}
