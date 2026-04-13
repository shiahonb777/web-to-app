package com.webtoapp.core.webview.intercept

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.engine.shields.BrowserShields
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.webview.WebViewManager
import com.webtoapp.core.webview.WebViewUrlPolicy
import com.webtoapp.data.model.WebViewConfig
import java.io.ByteArrayInputStream

internal class RequestInterceptionCoordinator(
    private val context: Context,
    private val adBlocker: AdBlocker,
    private val urlPolicy: WebViewUrlPolicy,
    private val resourceFallbackLoader: ResourceFallbackLoader
) {
    data class DiagSnapshot(
        val requestCount: Int,
        val blockedCount: Int,
        val errorCount: Int,
        val pageStartTime: Long
    )

    data class InterceptionResult(
        val response: WebResourceResponse?,
        val blocked: Boolean = false
    )

    fun intercept(
        request: WebResourceRequest,
        config: WebViewConfig,
        currentMainFrameUrl: String?,
        shields: BrowserShields?,
        diag: DiagSnapshot,
        shouldBypassAggressiveNetworkHooks: (WebResourceRequest, String) -> Boolean
    ): InterceptionResult {
        val url = request.url?.toString() ?: ""

        if (diag.requestCount % 50 == 0) {
            android.util.Log.w(
                "DIAG",
                "Request progress: total=${diag.requestCount} blocked=${diag.blockedCount} errors=${diag.errorCount} elapsed=${System.currentTimeMillis() - diag.pageStartTime}ms"
            )
        }

        if (com.webtoapp.core.extension.ExtensionResourceInterceptor.isChromeExtensionUrl(url)) {
            return InterceptionResult(
                com.webtoapp.core.extension.ExtensionResourceInterceptor.intercept(context, url)
            )
        }

        val resType = if (url.startsWith("http://") || url.startsWith("https://")) inferResourceType(request) else null
        if (resType != null) {
            if (com.webtoapp.core.extension.WebRequestBridge.shouldBlock(url, resType)) {
                AppLogger.d("WebViewManager", "WebRequest extension blocked: $url")
                return blockedResponse()
            }

            val dnrResult = com.webtoapp.core.extension.DeclarativeNetRequestEngine.evaluate(
                url = url,
                resourceType = resType,
                initiatorDomain = try { Uri.parse(currentMainFrameUrl ?: "").host ?: "" } catch (_: Exception) { "" },
                method = request.method ?: "GET"
            )
            if (dnrResult != null) {
                when (dnrResult.action) {
                    com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.BLOCK -> {
                        AppLogger.d("WebViewManager", "DNR blocked: $url")
                        return blockedResponse()
                    }
                    com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.ALLOW,
                    com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.ALLOW_ALL_REQUESTS -> {
                        return InterceptionResult(null, blocked = false)
                    }
                    com.webtoapp.core.extension.DeclarativeNetRequestEngine.ActionType.REDIRECT -> {
                        AppLogger.d("WebViewManager", "DNR redirect: $url -> ${dnrResult.redirectUrl}")
                    }
                    else -> Unit
                }
            }
        }

        if (url.startsWith("https://localhost/__ext__/")) {
            val extResourcePath = url.removePrefix("https://localhost/__ext__/")
            val chromeExtUrl = "chrome-extension://$extResourcePath"
            return InterceptionResult(
                com.webtoapp.core.extension.ExtensionResourceInterceptor.intercept(context, chromeExtUrl)
            )
        }

        if (url.startsWith("https://localhost/__local__/")) {
            val localPath = url.removePrefix("https://localhost/__local__/")
            AppLogger.d("WebViewManager", "Loading local resource: $localPath")
            return InterceptionResult(resourceFallbackLoader.loadLocalResource(localPath))
        }

        if (url.startsWith("file:///android_asset/")) {
            val assetPath = url.removePrefix("file:///android_asset/")
            return InterceptionResult(resourceFallbackLoader.loadEncryptedAsset(assetPath))
        }

        val bypassAggressiveNetworkHooks = shouldBypassAggressiveNetworkHooks(request, url)
        if (bypassAggressiveNetworkHooks && request.isForMainFrame) {
            AppLogger.d("WebViewManager", "Strict compatibility mode: bypass request interception for $url")
        }

        val urlScheme = com.webtoapp.core.perf.NativePerfEngine.checkUrlScheme(url)
        val isHttpOrHttps = urlScheme == 1 || urlScheme == 2
        val isLocalhost = url.startsWith("https://localhost/__local__/")
        val isThirdParty = if (!bypassAggressiveNetworkHooks && isHttpOrHttps && !isLocalhost) {
            isThirdPartySubResourceRequest(request, currentMainFrameUrl)
        } else {
            false
        }
        val isMapTile = if (isThirdParty) urlPolicy.isMapTileRequest(url) else false

        if (!bypassAggressiveNetworkHooks &&
            !config.disableShields &&
            isThirdParty &&
            !isMapTile &&
            shields != null &&
            shields.isEnabled() &&
            shields.getConfig().trackerBlocking
        ) {
            val trackerCategory = shields.trackerBlocker.checkTracker(url)
            if (trackerCategory != null) {
                shields.stats.recordTrackerBlocked(trackerCategory)
                android.util.Log.w("DIAG", "TRACKER_BLOCKED [$trackerCategory]: ${url.take(120)}")
                return blockedResponse()
            }
        }

        if (!bypassAggressiveNetworkHooks &&
            !isMapTile &&
            adBlocker.isEnabled()
        ) {
            val adResType = resType ?: inferResourceType(request)
            val pageHost = urlPolicy.extractHostFromUrl(currentMainFrameUrl)
            if (adBlocker.shouldBlock(url, pageHost, adResType, isThirdParty)) {
                shields?.stats?.recordAdBlocked()
                android.util.Log.w("DIAG", "AD_BLOCKED [${if (isThirdParty) "3P" else "1P"}/$adResType]: ${url.take(120)}")
                return InterceptionResult(adBlocker.createEmptyResponse(adResType), blocked = true)
            }
        }

        if (!bypassAggressiveNetworkHooks &&
            config.enableCrossOriginIsolation &&
            isHttpOrHttps
        ) {
            android.util.Log.w("DIAG", "CROSS_ORIGIN_PROXY: ${url.take(120)}")
            return InterceptionResult(resourceFallbackLoader.fetchWithCrossOriginHeaders(request))
        }

        if (url.startsWith("http://")) {
            val cleartextResponse = resourceFallbackLoader.fetchCleartextResource(request)
            if (cleartextResponse != null) {
                return InterceptionResult(cleartextResponse)
            }
        }

        return InterceptionResult(null)
    }

    fun inferResourceType(request: WebResourceRequest): String {
        if (request.isForMainFrame) return "main_frame"

        val headers = request.requestHeaders
        val accept = headers.entries.find {
            it.key.equals("Accept", ignoreCase = true)
        }?.value ?: ""

        if (accept.contains("text/html")) return "sub_frame"
        if (accept.contains("text/css")) return "stylesheet"
        if (accept.contains("image/")) return "image"
        if (accept.contains("font/") || accept.contains("application/font")) return "font"

        val url = request.url?.toString() ?: ""
        val ext = url.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()
        return when (ext) {
            "js", "mjs" -> "script"
            "css" -> "stylesheet"
            "png", "jpg", "jpeg", "gif", "webp", "svg", "ico" -> "image"
            "woff", "woff2", "ttf", "otf", "eot" -> "font"
            "html", "htm" -> "sub_frame"
            "json", "xml" -> "xmlhttprequest"
            "mp3", "wav", "ogg", "mp4", "webm" -> "media"
            else -> "other"
        }
    }

    private fun isThirdPartySubResourceRequest(
        request: WebResourceRequest,
        currentMainFrameUrl: String?
    ): Boolean {
        if (request.isForMainFrame) return false

        val requestHost = urlPolicy.extractHostFromUrl(request.url?.toString()) ?: return false
        if (requestHost in WebViewManager.LOCAL_CLEARTEXT_HOSTS) return false

        val topLevelHost = urlPolicy.extractHostFromUrl(currentMainFrameUrl)
            ?: urlPolicy.extractHostFromUrl(request.requestHeaders["Referer"])
            ?: urlPolicy.extractHostFromUrl(request.requestHeaders["referer"])
            ?: return false

        return !urlPolicy.isSameSiteHost(requestHost, topLevelHost)
    }

    private fun blockedResponse(): InterceptionResult {
        return InterceptionResult(
            WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0))),
            blocked = true
        )
    }
}
