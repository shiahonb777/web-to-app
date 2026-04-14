package com.webtoapp.ui.shell

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.LongPressHandler
import com.webtoapp.core.webview.WebViewCallbacks

/**
 * create Shell mode WebView
 *
 * update, handle, notify with WebViewCallbacks override
 */
fun createShellWebViewCallbacks(
    context: android.content.Context,
    config: ShellConfig,
    webViewRefProvider: () -> WebView?,
    currentUrlProvider: () -> String,
    longPressHandler: LongPressHandler,
    handleShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    handleHideCustomView: () -> Unit,
    handleFileChooser: (android.webkit.ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    updateLoading: (Boolean) -> Unit,
    updateUrl: (String) -> Unit,
    updateTitle: (String) -> Unit,
    updateProgress: (Int) -> Unit,
    updateError: (String?) -> Unit,
    updateNavigation: (canBack: Boolean, canForward: Boolean) -> Unit,
    updateWebViewRef: (WebView?) -> Unit,
    notifyRecreationKeyIncrement: () -> Unit,
    notifyLongPressMenu: (LongPressHandler.LongPressResult, Float, Float) -> Unit
): WebViewCallbacks {
    return object : WebViewCallbacks {
        override fun onPageStarted(url: String?) {
            if (url == "about:blank") return
            updateLoading(true)
            updateUrl(url ?: "")
            com.webtoapp.core.shell.ShellLogger.logWebView("开始加载", url ?: "")
            
            // Inject document_start remote( )
            webViewRefProvider()?.let { wv ->
                (context as? ShellActivity)?.cloudSdkManager?.let { sdkManager ->
                    val code = sdkManager.getRemoteScriptCode("document_start", url ?: "")
                    if (code.isNotBlank()) {
                        wv.evaluateJavascript(code, null)
                        AppLogger.d("ShellActivity", "Injected document_start scripts for: $url")
                    }
                }
            }
        }

        override fun onUrlChanged(webView: WebView?, url: String?) {
            // SPA navigation (pushState/replaceState) — update nav state in real time
            webView?.let {
                updateNavigation(it.canGoBack(), it.canGoForward())
            }
            if (url != null) updateUrl(url)
        }

        override fun onPageFinished(url: String?) {
            if (url == "about:blank") return
            updateLoading(false)
            updateUrl(url ?: "")
            com.webtoapp.core.shell.ShellLogger.logWebView("Loading complete", url ?: "")
            webViewRefProvider()?.let {
                updateNavigation(it.canGoBack(), it.canGoForward())
                
                // Inject
                if (config.translateEnabled) {
                    injectTranslateScript(it, config.translateTargetLanguage, config.translateShowButton)
                }
                
                // Injectlong- press( long- press)
                longPressHandler.injectLongPressEnhancer(it)
                
                // Injectremote
                (context as? ShellActivity)?.cloudSdkManager?.let { sdkManager ->
                    val endCode = sdkManager.getRemoteScriptCode("document_end", url ?: "")
                    if (endCode.isNotBlank()) {
                        it.evaluateJavascript(endCode, null)
                        AppLogger.d("ShellActivity", "Injected document_end scripts for: $url")
                    }
                    
                    // document_idle: ( )
                    val idleCode = sdkManager.getRemoteScriptCode("document_idle", url ?: "")
                    if (idleCode.isNotBlank()) {
                        it.postDelayed({
                            it.evaluateJavascript(idleCode, null)
                            AppLogger.d("ShellActivity", "Injected document_idle scripts for: $url")
                        }, 300)
                    }
                }
            }
        }

        override fun onProgressChanged(progress: Int) {
            updateProgress(progress)
        }

        override fun onTitleChanged(title: String?) {
            if (title == "about:blank" || title.isNullOrBlank()) return
            updateTitle(title)
        }

        override fun onIconReceived(icon: Bitmap?) {}

        override fun onError(errorCode: Int, description: String) {
            updateError(description)
            updateLoading(false)
            com.webtoapp.core.shell.ShellLogger.logWebView("加载错误", currentUrlProvider(), "errorCode=$errorCode, description=$description")
        }

        override fun onSslError(error: String) {
            updateError("SSL安全错误")
            com.webtoapp.core.shell.ShellLogger.logWebView("SSL错误", currentUrlProvider(), error)
        }

        override fun onExternalLink(url: String) {
            try {
                val safeUrl = normalizeExternalUrlForIntent(url)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                AppLogger.w("ShellActivity", "No app to handle external link: $url", e)
                Toast.makeText(
                    context,
                    Strings.cannotOpenLink,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
            view?.let { handleShowCustomView(it, callback) }
        }

        override fun onHideCustomView() {
            handleHideCustomView()
        }

        override fun onGeolocationPermission(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            // Activity Android
            (context as? ShellActivity)?.handleGeolocationPermission(origin, callback)
                ?: callback?.invoke(origin, true, false)
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            // Activity Androidsystem( , )
            AppLogger.d("ShellActivity", "WebViewCallbacks.onPermissionRequest called, request: ${request?.resources?.joinToString()}")
            request?.let { req ->
                val shellActivity = context as? ShellActivity
                AppLogger.d("ShellActivity", "ShellActivity cast result: ${shellActivity != null}")
                if (shellActivity != null) {
                    shellActivity.handlePermissionRequest(req)
                } else {
                    AppLogger.w("ShellActivity", "Context is not ShellActivity, granting directly")
                    req.grant(req.resources)
                }
            } ?: AppLogger.w("ShellActivity", "Permission request is null")
        }

        override fun onShowFileChooser(
            filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean {
            return handleFileChooser(filePathCallback, fileChooserParams)
        }
        
        override fun onDownloadStart(
            url: String,
            userAgent: String,
            contentDisposition: String,
            mimeType: String,
            contentLength: Long
        ) {
            // Checkand download
            (context as? ShellActivity)?.handleDownloadWithPermission(
                url, userAgent, contentDisposition, mimeType, contentLength
            )
        }
        
        override fun onLongPress(webView: WebView, x: Float, y: Float): Boolean {
            // long- press, check long- press
            // if, alwaysintercept hidesystemdefault previewdialog
            val hitResult = webView.hitTestResult
            val hitType = hitResult.type
            val isLink = hitType == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                         hitType == WebView.HitTestResult.ANCHOR_TYPE
            
            // Checklong- press
            if (!config.webViewConfig.longPressMenuEnabled) {
                return isLink // long- pressalwaysintercept hidepreviewdialog
            }
            
            // If it isedit or type, intercept, WebView handledefault select
            if (hitType == WebView.HitTestResult.EDIT_TEXT_TYPE ||
                hitType == WebView.HitTestResult.UNKNOWN_TYPE) {
                return false
            }
            
            // JS long- press
            longPressHandler.getLongPressDetails(webView, x, y) { result ->
                when (result) {
                    is LongPressHandler.LongPressResult.Image,
                    is LongPressHandler.LongPressResult.Video,
                    is LongPressHandler.LongPressResult.Link,
                    is LongPressHandler.LongPressResult.ImageLink -> {
                        notifyLongPressMenu(result, x, y)
                    }
                    is LongPressHandler.LongPressResult.Text,
                    is LongPressHandler.LongPressResult.None -> {
                        // or area, display
                    }
                }
            }
            
            // , type, intercept display
            return when (hitType) {
                WebView.HitTestResult.IMAGE_TYPE,
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                WebView.HitTestResult.SRC_ANCHOR_TYPE,
                WebView.HitTestResult.ANCHOR_TYPE -> true
                else -> false  // intercept, default select
            }
        }
        
        override fun onRenderProcessGone(didCrash: Boolean) {
            AppLogger.w("ShellActivity", "Render process gone (crash=$didCrash), triggering WebView recreation")
            updateWebViewRef(null)
            updateError(null)
            notifyRecreationKeyIncrement()
        }
    }
}
