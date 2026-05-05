package com.webtoapp.core.engine

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.webview.OAuthCompatEngine
import android.view.View
import com.webtoapp.data.model.UserAgentMode
import com.webtoapp.data.model.WebViewConfig
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebResponse




data class ProxyConfig(
    val mode: String = "NONE",
    val host: String = "",
    val port: Int = 0,
    val type: String = "HTTP",
    val pacUrl: String = "",
    val username: String = "",
    val password: String = ""
)











class GeckoViewEngine(
    private val context: Context
) : BrowserEngine {

    companion object {
        private const val TAG = "GeckoViewEngine"

        @Volatile
        private var sharedRuntime: GeckoRuntime? = null

        fun getRuntime(context: Context): GeckoRuntime {
            return sharedRuntime ?: synchronized(this) {
                sharedRuntime ?: createRuntime(context.applicationContext).also {
                    sharedRuntime = it
                }
            }
        }

        @Volatile
        private var currentDnsConfig: com.webtoapp.data.model.DnsConfig? = null

        @Volatile
        private var currentProxyConfig: ProxyConfig? = null

        fun applyDnsConfig(config: com.webtoapp.data.model.DnsConfig) {
            currentDnsConfig = config
            val runtime = sharedRuntime ?: return
            applyDohToRuntime(runtime, config)
        }

        fun applyProxyConfig(config: ProxyConfig) {
            currentProxyConfig = config




            val runtime = sharedRuntime
            if (runtime != null && config.mode != "NONE") {
                AppLogger.w(TAG, "GeckoView proxy set after runtime creation — proxy will take effect on next runtime creation")
            }
            AppLogger.d(TAG, "Proxy config stored: mode=${config.mode}, type=${config.type}, host=${config.host}:${config.port}")
        }





        private fun buildProxyArgs(config: ProxyConfig): List<String> {
            if (config.mode == "NONE") return emptyList()
            val args = mutableListOf<String>()
            when (config.mode) {
                "STATIC" -> {
                    if (config.host.isBlank() || config.port <= 0) return emptyList()


                    args.add("--pref=network.proxy.type=1")

                    when (config.type.uppercase()) {
                        "SOCKS5", "SOCKS" -> {
                            args.add("--pref=network.proxy.socks=${config.host}")
                            args.add("--pref=network.proxy.socks_port=${config.port}")
                            args.add("--pref=network.proxy.socks_version=5")

                            args.add("--pref=network.proxy.socks_remote_dns=true")
                        }
                        "HTTPS" -> {

                            args.add("--pref=network.proxy.ssl=${config.host}")
                            args.add("--pref=network.proxy.ssl_port=${config.port}")
                            args.add("--pref=network.proxy.http=${config.host}")
                            args.add("--pref=network.proxy.http_port=${config.port}")
                            args.add("--pref=network.proxy.share_proxy_settings=true")
                        }
                        else -> {

                            args.add("--pref=network.proxy.http=${config.host}")
                            args.add("--pref=network.proxy.http_port=${config.port}")
                            args.add("--pref=network.proxy.ssl=${config.host}")
                            args.add("--pref=network.proxy.ssl_port=${config.port}")
                            args.add("--pref=network.proxy.share_proxy_settings=true")
                        }
                    }
                }
                "PAC" -> {
                    if (config.pacUrl.isBlank()) return emptyList()

                    args.add("--pref=network.proxy.type=2")
                    args.add("--pref=network.proxy.autoconfig_url=${config.pacUrl}")
                }
            }
            return args
        }

        private fun applyDohToRuntime(runtime: GeckoRuntime, config: com.webtoapp.data.model.DnsConfig) {
            val dohUrl = config.effectiveDohUrl
            if (dohUrl.isBlank()) {

                runtime.settings.setTrustedRecursiveResolverMode(GeckoRuntimeSettings.TRR_MODE_OFF)
                AppLogger.d(TAG, "DoH disabled, using system DNS")
                return
            }




            val trrMode = if (config.dohMode == "strict" || config.bypassSystemDns) {
                GeckoRuntimeSettings.TRR_MODE_ONLY
            } else {
                GeckoRuntimeSettings.TRR_MODE_FIRST
            }

            runtime.settings.setTrustedRecursiveResolverMode(trrMode)
            runtime.settings.setTrustedRecursiveResolverUri(dohUrl)

            AppLogger.d(TAG, "DoH applied to GeckoView: provider=${config.provider}, mode=$trrMode, url=$dohUrl")
        }

        private fun createRuntime(context: Context): GeckoRuntime {
            val settingsBuilder = GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .consoleOutput(true)
                .contentBlocking(
                    ContentBlocking.Settings.Builder()
                        .antiTracking(
                            ContentBlocking.AntiTracking.DEFAULT
                                    or ContentBlocking.AntiTracking.STP
                        )
                        .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                        .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                        .build()
                )


            currentDnsConfig?.let { config ->
                val dohUrl = config.effectiveDohUrl
                if (dohUrl.isNotBlank()) {
                    val trrMode = if (config.dohMode == "strict" || config.bypassSystemDns) {
                        GeckoRuntimeSettings.TRR_MODE_ONLY
                    } else {
                        GeckoRuntimeSettings.TRR_MODE_FIRST
                    }
                    settingsBuilder.trustedRecursiveResolverMode(trrMode)
                    settingsBuilder.trustedRecursiveResolverUri(dohUrl)
                }
            }


            currentProxyConfig?.let { config ->
                val proxyArgs = buildProxyArgs(config)
                if (proxyArgs.isNotEmpty()) {
                    settingsBuilder.arguments(proxyArgs.toTypedArray())
                    AppLogger.d(TAG, "GeckoView proxy args applied at runtime creation: $proxyArgs")
                }
            }

            return GeckoRuntime.create(context, settingsBuilder.build())
        }
    }

    override val engineType = EngineType.GECKOVIEW

    private var geckoView: GeckoView? = null
    private var session: GeckoSession? = null
    private var callback: BrowserEngineCallback? = null
    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var canGoBackFlag = false
    private var canGoForwardFlag = false


    private var lastConfig: WebViewConfig? = null
    private var lastGeckoUaMode: Int = GeckoSessionSettings.USER_AGENT_MODE_MOBILE
    private var lastUserAgentOverride: String? = null

    override fun createView(
        context: Context,
        config: WebViewConfig,
        callback: BrowserEngineCallback
    ): View {
        this.callback = callback
        this.lastConfig = config

        val runtime = getRuntime(context)


        val geckoUaMode = when (config.userAgentMode) {
            UserAgentMode.CHROME_DESKTOP, UserAgentMode.SAFARI_DESKTOP,
            UserAgentMode.FIREFOX_DESKTOP, UserAgentMode.EDGE_DESKTOP -> GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            else -> GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        this.lastGeckoUaMode = geckoUaMode

        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .userAgentMode(geckoUaMode)
            .build()

        val newSession = GeckoSession(sessionSettings)
        setupDelegates(newSession, callback)

        newSession.open(runtime)
        session = newSession

        val view = GeckoView(context)
        view.setSession(newSession)
        geckoView = view


        val effectiveUserAgent = when (config.userAgentMode) {
            UserAgentMode.DEFAULT -> null
            UserAgentMode.CUSTOM -> config.customUserAgent?.takeIf { it.isNotBlank() }
            else -> config.userAgentMode.userAgentString
        }
        if (effectiveUserAgent != null) {
            newSession.settings.userAgentOverride = effectiveUserAgent
            lastUserAgentOverride = effectiveUserAgent
            AppLogger.d(TAG, "User-Agent set: ${effectiveUserAgent.take(80)}...")
        }

        return view
    }




    private fun setupDelegates(session: GeckoSession, callback: BrowserEngineCallback) {
        setupContentDelegate(session, callback)
        setupNavigationDelegate(session, callback)
        setupProgressDelegate(session, callback)
    }

    private fun setupContentDelegate(session: GeckoSession, callback: BrowserEngineCallback) {
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                currentTitle = title
                callback.onTitleChanged(title)
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                if (fullScreen) {
                    callback.onShowCustomView(geckoView, null)
                } else {
                    callback.onHideCustomView()
                }
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                val contentType = response.headers["Content-Type"] ?: "application/octet-stream"
                val contentDisposition = response.headers["Content-Disposition"] ?: ""
                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                callback.onDownloadStart(
                    response.uri,
                    "",
                    contentDisposition,
                    contentType,
                    contentLength
                )
            }

            override fun onCrash(session: GeckoSession) {
                AppLogger.e(TAG, "GeckoView session crashed, attempting recovery...")
                callback.onError(-1, "Engine crash — recovering...")
                attemptCrashRecovery()
            }
        }
    }

    private fun setupNavigationDelegate(session: GeckoSession, callback: BrowserEngineCallback) {
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                currentUrl = url
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                canGoBackFlag = canGoBack
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                canGoForwardFlag = canGoForward
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = request.uri




                if (isGoogleOAuthUrl(uri)) {
                    AppLogger.d("GeckoViewEngine", "Google OAuth detected — allowing in-GeckoView with kernel disguise: $uri")

                    return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                }

                if (uri.startsWith("tel:") || uri.startsWith("mailto:") || uri.startsWith("intent:")) {
                    callback.onExternalLink(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }


                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession>? {
                loadUrl(uri)
                return null
            }
        }
    }

    private fun setupProgressDelegate(session: GeckoSession, callback: BrowserEngineCallback) {
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {

                callback.onPageStarted(url)


                OAuthCompatEngine.getAntiDetectionJs(url)?.let { js ->
                    val provider = OAuthCompatEngine.getProviderType(url)
                    AppLogger.d(TAG, "Injecting OAuth anti-detection JS [$provider] (GeckoView) for: $url")
                    evaluateJavascript(js, null)
                }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                callback.onPageFinished(currentUrl)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                callback.onProgressChanged(progress)
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {

            }
        }
    }









    private fun attemptCrashRecovery() {
        val view = geckoView ?: return
        val cb = callback ?: return
        val urlToRestore = currentUrl

        try {

            try { session?.close() } catch (_: Exception) { }
            session = null

            val runtime = getRuntime(context)

            val sessionSettings = GeckoSessionSettings.Builder()
                .usePrivateMode(false)
                .useTrackingProtection(true)
                .userAgentMode(lastGeckoUaMode)
                .build()

            val newSession = GeckoSession(sessionSettings)
            setupDelegates(newSession, cb)
            newSession.open(runtime)


            lastUserAgentOverride?.let {
                newSession.settings.userAgentOverride = it
            }

            view.setSession(newSession)
            session = newSession


            if (!urlToRestore.isNullOrBlank() && urlToRestore != "about:blank") {
                newSession.loadUri(urlToRestore)
                AppLogger.i(TAG, "Crash recovery successful, restoring URL: $urlToRestore")
            } else {
                AppLogger.i(TAG, "Crash recovery successful (no URL to restore)")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Crash recovery failed", e)
            cb.onError(-2, "Engine crash recovery failed: ${e.message}")
        }
    }





    override fun loadUrl(url: String) {
        session?.loadUri(url)
    }








    override fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {
        val s = session
        if (s == null) {
            resultCallback?.invoke(null)
            return
        }



        try {
            val encoded = android.util.Base64.encodeToString(
                script.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            val wrappedScript = "javascript:void(eval(atob('$encoded')))"
            s.loadUri(wrappedScript)
        } catch (e: Exception) {
            AppLogger.e(TAG, "evaluateJavascript encoding failed", e)

            try {
                val escaped = script
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                s.loadUri("javascript:void(eval('$escaped'))")
            } catch (ex: Exception) {
                AppLogger.e(TAG, "evaluateJavascript fallback also failed", ex)
            }
        }


        resultCallback?.invoke(null)
    }

    override fun canGoBack(): Boolean = canGoBackFlag
    override fun goBack() { session?.goBack() }
    override fun canGoForward(): Boolean = canGoForwardFlag
    override fun goForward() { session?.goForward() }
    override fun reload() { session?.reload() }
    override fun stopLoading() { session?.stop() }
    override fun getCurrentUrl(): String? = currentUrl
    override fun getTitle(): String? = currentTitle
    override fun getView(): View? = geckoView

    override fun destroy() {
        try {
            session?.close()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error closing session", e)
        }
        session = null
        geckoView = null
        callback = null
        lastConfig = null
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        try {
            val runtime = sharedRuntime ?: return
            runtime.storageController.clearData(StorageController.ClearFlags.ALL)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error clearing cache", e)
        }
    }

    override fun clearHistory() {
        try {
            session?.purgeHistory()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error clearing history", e)
        }
    }



    private fun isGoogleOAuthUrl(url: String): Boolean = OAuthCompatEngine.isOAuthUrl(url)

    private fun openInSystemBrowser(url: String) {
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to open system browser for OAuth: $url", e)
        }
    }
}
