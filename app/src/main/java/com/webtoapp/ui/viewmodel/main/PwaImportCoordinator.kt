package com.webtoapp.ui.viewmodel.main

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.pwa.PwaAnalyzer
import com.webtoapp.core.pwa.PwaAnalysisResult
import com.webtoapp.core.pwa.PwaDataSource
import com.webtoapp.ui.viewmodel.EditState
import com.webtoapp.data.model.OrientationMode
import com.webtoapp.data.model.SplashType
import com.webtoapp.data.model.StatusBarColorMode
import com.webtoapp.util.IconStorage
import com.webtoapp.util.SplashStorage
import java.net.HttpURLConnection
import java.net.URL

class PwaImportCoordinator {

    fun applyResult(state: EditState, result: PwaAnalysisResult): EditState {
        var next = state
        result.suggestedName?.takeIf { it.isNotBlank() && state.name.isBlank() }?.let { next = next.copy(name = it) }
        result.suggestedThemeColor?.let { color ->
            next = next.copy(
                webViewConfig = next.webViewConfig.copy(
                    statusBarColorMode = StatusBarColorMode.CUSTOM,
                    statusBarColor = color
                )
            )
        }
        result.suggestedOrientation?.let { orientation ->
            parseOrientation(orientation)?.let {
                next = next.copy(webViewConfig = next.webViewConfig.copy(orientationMode = it))
            }
        }
        result.suggestedDisplay?.let { display ->
            next = applyDisplayMode(next, display)
        }
        if (result.source == PwaDataSource.MANIFEST) {
            next = next.copy(
                webViewConfig = next.webViewConfig.copy(
                    pwaOfflineEnabled = true
                )
            )
        }
        val hosts = PwaAnalyzer.suggestDeepLinkHosts(result, next.url)
        if (hosts.isNotEmpty()) {
            next = next.copy(
                apkExportConfig = next.apkExportConfig.copy(
                    deepLinkEnabled = true,
                    customDeepLinkHosts = hosts
                )
            )
        }
        result.startUrl?.let { startUrl ->
            val currentUrl = next.url.trim()
            if (startUrl.isNotBlank() && startUrl != currentUrl) {
                val currentHost = PwaAnalyzer.extractHost(currentUrl)
                val startHost = PwaAnalyzer.extractHost(startUrl)
                if (currentHost == startHost) {
                    next = next.copy(url = startUrl)
                }
            }
        }
        return next
    }

    suspend fun downloadIcon(context: Context, iconUrl: String): String? {
        return try {
            val conn = URL(iconUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36"
            )
            try {
                if (conn.responseCode !in 200..299) return null
                val bitmap = BitmapFactory.decodeStream(conn.inputStream) ?: return null
                IconStorage.saveIconFromBitmap(context, bitmap)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            AppLogger.e("PwaImportCoordinator", "Icon download failed: ${e.message}")
            null
        }
    }

    suspend fun saveIconFromUri(context: Context, state: EditState, uri: Uri): String? {
        val oldPath = state.savedIconPath
        val path = IconStorage.saveIconFromUri(context, uri)
        if (path != null && oldPath != null && oldPath != path) {
            IconStorage.deleteIcon(oldPath)
        }
        return path
    }

    suspend fun saveSplashFromUri(
        context: Context,
        state: EditState,
        uri: Uri,
        isVideo: Boolean
    ): Pair<String, SplashType>? {
        val oldPath = state.savedSplashPath
        val path = SplashStorage.saveMediaFromUri(context, uri, isVideo)
        if (path != null && oldPath != null && oldPath != path) {
            SplashStorage.deleteMedia(oldPath)
        }
        val type = if (isVideo) SplashType.VIDEO else SplashType.IMAGE
        return path?.let { it to type }
    }

    fun clearSplashMedia(state: EditState): EditState {
        return state.copy(
            splashMediaUri = null,
            savedSplashPath = null
        )
    }

    private fun applyDisplayMode(state: EditState, display: String): EditState {
        return when (display.lowercase()) {
            "fullscreen" -> state.copy(
                webViewConfig = state.webViewConfig.copy(
                    hideToolbar = true,
                    fullscreenEnabled = true
                )
            )
            "standalone" -> state.copy(
                webViewConfig = state.webViewConfig.copy(
                    hideToolbar = true
                )
            )
            else -> state
        }
    }

    private fun parseOrientation(orientation: String): OrientationMode? {
        return when (orientation.lowercase()) {
            "portrait", "portrait-primary", "portrait-secondary" -> OrientationMode.PORTRAIT
            "landscape", "landscape-primary", "landscape-secondary" -> OrientationMode.LANDSCAPE
            "any", "natural" -> OrientationMode.AUTO
            else -> null
        }
    }
}
