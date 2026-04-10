package com.webtoapp.ui.webview

import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.WebApp
import com.webtoapp.util.ensureWebUrlScheme
import com.webtoapp.util.normalizeExternalIntentUrl
import com.webtoapp.util.upgradeRemoteHttpToHttps

internal fun normalizeWebUrlForSecurity(rawUrl: String?): String {
    val trimmed = rawUrl?.trim().orEmpty()
    val withScheme = ensureWebUrlScheme(trimmed)
    val upgraded = upgradeRemoteHttpToHttps(withScheme)
    if (upgraded != trimmed) {
        AppLogger.w("WebViewActivity", "Blocked insecure HTTP target, auto-upgraded to HTTPS: $trimmed -> $upgraded")
    }
    return upgraded
}

internal fun normalizeExternalUrlForIntent(rawUrl: String): String {
    val safeUrl = normalizeExternalIntentUrl(rawUrl)
    if (safeUrl.isEmpty()) {
        AppLogger.w("WebViewActivity", "Blocked invalid or dangerous external URL: $rawUrl")
        return ""
    }
    return normalizeWebUrlForSecurity(safeUrl)
}

internal fun hasConfiguredAds(app: WebApp): Boolean {
    val config = app.adConfig
    return app.adsEnabled ||
        config?.bannerId?.isNotBlank() == true ||
        config?.interstitialId?.isNotBlank() == true ||
        config?.splashId?.isNotBlank() == true
}

internal val STRICT_COMPAT_HOST_SUFFIXES = setOf(
    "douyin.com",
    "iesdouyin.com",
    "tiktok.com",
    "tiktokv.com",
    "byteoversea.com",
    "byteimg.com"
)

// Keep strict-host fallback probe logic for diagnostics, but do not auto-jump to external browser.
internal const val STRICT_HOST_AUTO_EXTERNAL_FALLBACK_ENABLED = false

internal fun shouldSkipLongPressEnhancer(url: String?): Boolean {
    val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
    return STRICT_COMPAT_HOST_SUFFIXES.any { suffix ->
        host == suffix || host.endsWith(".$suffix")
    }
}

internal fun decodeEvaluateJavascriptString(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty() || value == "null") return null
    return runCatching {
        org.json.JSONArray("[$value]").optString(0, null)
    }.getOrNull() ?: value
}

internal fun shouldFallbackToExternalForStrictHost(metricsJson: String?): Boolean {
    val json = metricsJson ?: return false
    return runCatching {
        val obj = org.json.JSONObject(json)
        val blank = obj.optBoolean("blank", false)
        val height = obj.optInt("height", 0)
        val textLength = obj.optInt("textLength", 0)
        val nodeCount = obj.optInt("nodeCount", 0)
        blank || (height < 900 && textLength < 80 && nodeCount < 120)
    }.getOrDefault(false)
}
