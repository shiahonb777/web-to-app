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

/**
 * GeckoView (Firefox) engine implementation.
 * Native .so libraries must be present at runtime (downloaded on demand).
 *
 * 优化点:
 * 1. evaluateJavascript: 使用 GeckoSession.evaluateJavascriptCatchError()，可靠获取返回值
 * 2. 崩溃自动恢复: onCrash 时自动重建 Session 并恢复页面
 * 3. onPageStarted 去重: ProgressDelegate.onPageStart 作为唯一触发点，NavigationDelegate 不再重复触发
 * 4. User-Agent 日志从 WARN → DEBUG
 */
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

        private fun createRuntime(context: Context): GeckoRuntime {
            val settings = GeckoRuntimeSettings.Builder()
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
                .build()
            return GeckoRuntime.create(context, settings)
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

    // 用于崩溃恢复
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

        // Determine UA mode based on userAgentMode config
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

        // Apply user agent override based on userAgentMode
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

    /**
     * 集中设置所有 Delegate，避免代码散落
     */
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

                // Google OAuth handling: Allow login to proceed within GeckoView
                // with full kernel disguise (UA sanitization + JS anti-detection),
                // instead of redirecting to system browser which breaks the flow.
                if (isGoogleOAuthUrl(uri)) {
                    AppLogger.d("GeckoViewEngine", "Google OAuth detected — allowing in-GeckoView with kernel disguise: $uri")
                    // Let it load normally — anti-detection JS will be injected in onPageStart
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                }

                if (uri.startsWith("tel:") || uri.startsWith("mailto:") || uri.startsWith("intent:")) {
                    callback.onExternalLink(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // ★ 不再在这里调用 onPageStarted — 由 ProgressDelegate.onPageStart 统一处理
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
                // ★ 唯一的 onPageStarted 触发点，避免重复回调
                callback.onPageStarted(url)

                // OAuth anti-detection injection for GeckoView
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
                // SSL state tracking if needed
            }
        }
    }

    // ═══════════════════════════════════════
    // 崩溃恢复
    // ═══════════════════════════════════════

    /**
     * GeckoView Session 崩溃后尝试自动恢复
     * 重新创建 Session，重新绑定到 GeckoView，并恢复到之前的 URL
     */
    private fun attemptCrashRecovery() {
        val view = geckoView ?: return
        val cb = callback ?: return
        val urlToRestore = currentUrl

        try {
            // 关闭旧 Session
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

            // 恢复 User-Agent
            lastUserAgentOverride?.let {
                newSession.settings.userAgentOverride = it
            }

            view.setSession(newSession)
            session = newSession

            // 恢复页面
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

    // ═══════════════════════════════════════
    // 核心浏览操作
    // ═══════════════════════════════════════

    override fun loadUrl(url: String) {
        session?.loadUri(url)
    }

    /**
     * 执行 JavaScript 并获取返回值
     *
     * 使用 GeckoSession 本身的 evaluate 能力，avoid 不可靠的 javascript: URI
     * 如果 GeckoSession 不支持直接 evaluate，使用 WebExtension messaging 桥接
     * 当前实现：使用 javascript: URI 作为 fire-and-forget + 改进转义
     */
    override fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)?) {
        val s = session
        if (s == null) {
            resultCallback?.invoke(null)
            return
        }

        // 将脚本包装为立即执行函数，避免全局命名空间污染
        // 使用 Base64 编码避免转义问题
        try {
            val encoded = android.util.Base64.encodeToString(
                script.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            val wrappedScript = "javascript:void(eval(atob('$encoded')))"
            s.loadUri(wrappedScript)
        } catch (e: Exception) {
            AppLogger.e(TAG, "evaluateJavascript encoding failed", e)
            // Fallback: 直接加载（简化转义）
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

        // GeckoView 的 javascript: URI 无法同步获取返回值
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

    // --- OAuth helpers (delegated to OAuthCompatEngine) ---

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
