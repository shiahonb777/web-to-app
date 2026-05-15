package com.webtoapp.ui.webview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import com.webtoapp.core.logging.AppLogger
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.webtoapp.ui.components.EdgeSwipeRefreshLayout
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.bgm.BgmPlayer
import com.webtoapp.core.webview.LocalHttpServer
import com.webtoapp.core.webview.LongPressHandler
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.core.webview.WebViewManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.KeyboardAdjustMode
import com.webtoapp.data.model.LongPressMenuStyle
import com.webtoapp.data.model.SplashOrientation
import com.webtoapp.data.model.SplashType
import com.webtoapp.data.model.WebApp
import android.content.pm.ActivityInfo
import com.webtoapp.ui.theme.WebToAppTheme
import com.webtoapp.util.DownloadHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.webtoapp.ui.shared.WindowHelper
import com.webtoapp.ui.shell.ShellWebViewNavigation
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.core.wordpress.WordPressPhpRuntime
import com.webtoapp.core.wordpress.WordPressManager
import com.webtoapp.data.model.WordPressConfig
import com.webtoapp.core.php.PhpAppRuntime
import com.webtoapp.core.stats.AppUsageTracker
import androidx.compose.ui.text.style.TextOverflow
import com.webtoapp.ui.components.announcement.toUiTemplate




class WebViewActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_APP_ID = "app_id"
        private const val EXTRA_URL = "url"
        private const val EXTRA_TEST_URL = "test_url"
        private const val EXTRA_TEST_MODULE_IDS = "test_module_ids"
        private const val EXTRA_PREVIEW_APP_JSON = "preview_app_json"

        fun start(context: Context, appId: Long) {
            context.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_APP_ID, appId)
            })
        }

        fun startWithUrl(context: Context, url: String) {
            context.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            })
        }





        fun startPreview(context: Context, webAppJson: String) {
            context.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_PREVIEW_APP_JSON, webAppJson)
            })
        }




        fun startForTest(context: Context, testUrl: String, moduleIds: List<String>) {
            context.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_TEST_URL, testUrl)
                putStringArrayListExtra(EXTRA_TEST_MODULE_IDS, ArrayList(moduleIds))
            })
        }
    }

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null


    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissions.Callback? = null

    private var immersiveFullscreenEnabled: Boolean = false
    private var showStatusBarInFullscreen: Boolean = false
    internal var showNavigationBarInFullscreen: Boolean = false


    private var originalOrientationBeforeFullscreen: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED


    private var statusBarColorMode: com.webtoapp.data.model.StatusBarColorMode = com.webtoapp.data.model.StatusBarColorMode.THEME
    private var statusBarCustomColor: String? = null
    private var statusBarDarkIcons: Boolean? = null
    private var statusBarBackgroundType: com.webtoapp.data.model.StatusBarBackgroundType = com.webtoapp.data.model.StatusBarBackgroundType.COLOR

    private var statusBarColorModeDark: com.webtoapp.data.model.StatusBarColorMode = com.webtoapp.data.model.StatusBarColorMode.THEME
    private var statusBarCustomColorDark: String? = null
    private var statusBarDarkIconsDark: Boolean = false
    private var statusBarBackgroundTypeDark: com.webtoapp.data.model.StatusBarBackgroundType = com.webtoapp.data.model.StatusBarBackgroundType.COLOR
    internal var keyboardAdjustMode: KeyboardAdjustMode = KeyboardAdjustMode.RESIZE

    private fun applyStatusBarColor(
        colorMode: com.webtoapp.data.model.StatusBarColorMode,
        customColor: String?,
        darkIcons: Boolean?,
        isDarkTheme: Boolean
    ) = WindowHelper.applyStatusBarColor(this, colorMode.name, customColor, darkIcons, isDarkTheme)

    private fun applyImmersiveFullscreen(enabled: Boolean, hideNavBar: Boolean? = null, isDarkTheme: Boolean = false) {
        val shouldHideNavBar = hideNavBar ?: !showNavigationBarInFullscreen
        WindowHelper.applyImmersiveFullscreen(
            activity = this,
            enabled = enabled,
            hideNavBar = shouldHideNavBar,
            isDarkTheme = isDarkTheme,
            showStatusBar = showStatusBarInFullscreen,
            statusBarColorMode = statusBarColorMode.name,
            statusBarCustomColor = statusBarCustomColor,
            statusBarDarkIcons = statusBarDarkIcons,
            statusBarBgType = statusBarBackgroundType.name,
            keyboardAdjustMode = keyboardAdjustMode,
            tag = "WebViewActivity"
        )
    }


    private var cameraPhotoUri: android.net.Uri? = null

    private val fileChooserActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback
        if (callback == null) return@registerForActivityResult

        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUris = mutableListOf<android.net.Uri>()
            val data = result.data
            if (data == null || (data.data == null && data.clipData == null)) {
                cameraPhotoUri?.let { resultUris.add(it) }
            } else {
                data.data?.let { resultUris.add(it) }
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let { resultUris.add(it) }
                    }
                }
            }
            callback.onReceiveValue(resultUris.toTypedArray())
        } else {
            callback.onReceiveValue(null)
        }
        filePathCallback = null
        cameraPhotoUri = null
    }


    private var pendingFileChooserParams: android.webkit.WebChromeClient.FileChooserParams? = null
    private val cameraForChooserPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        launchFileChooserIntent(pendingFileChooserParams)
        pendingFileChooserParams = null
    }

    private fun handleFileChooser(
        callback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
        params: android.webkit.WebChromeClient.FileChooserParams?
    ): Boolean {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        if (callback == null) return false

        val needsCamera = isCameraRequiredForChooser(params)
        val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (needsCamera && !hasCam) {
            pendingFileChooserParams = params
            cameraForChooserPermLauncher.launch(android.Manifest.permission.CAMERA)
        } else {
            launchFileChooserIntent(params)
        }
        return true
    }




    private fun isCameraRequiredForChooser(params: android.webkit.WebChromeClient.FileChooserParams?): Boolean {
        if (params == null) return false
        if (params.isCaptureEnabled) return true

        val acceptTypes = params.acceptTypes
        if (acceptTypes == null || acceptTypes.isEmpty() || (acceptTypes.size == 1 && acceptTypes[0].isNullOrBlank())) {
            return true
        }

        for (type in acceptTypes) {
            if (type.isNullOrBlank()) continue
            val lower = type.lowercase()
            if (lower.startsWith("image/") || lower.startsWith("video/")) return true
            if (lower in setOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".heif",
                    ".bmp", ".svg", ".mp4", ".mov", ".avi", ".mkv", ".webm", ".3gp")) return true
        }

        return false
    }




    private fun extensionToMimeTypeForChooser(ext: String): String {
        return when (ext) {
            ".json" -> "application/json"
            ".xml" -> "application/xml"
            ".csv" -> "text/csv"
            ".txt" -> "text/plain"
            ".pdf" -> "application/pdf"
            ".doc" -> "application/msword"
            ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ".xls" -> "application/vnd.ms-excel"
            ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ".ppt" -> "application/vnd.ms-powerpoint"
            ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ".html", ".htm" -> "text/html"
            ".css" -> "text/css"
            ".js" -> "application/javascript"
            ".py" -> "text/x-python"
            ".yaml", ".yml" -> "application/x-yaml"
            ".md" -> "text/markdown"
            ".jpg", ".jpeg" -> "image/jpeg"
            ".png" -> "image/png"
            ".gif" -> "image/gif"
            ".webp" -> "image/webp"
            ".svg" -> "image/svg+xml"
            ".bmp" -> "image/bmp"
            ".mp3" -> "audio/mpeg"
            ".wav" -> "audio/wav"
            ".mp4" -> "video/mp4"
            ".webm" -> "video/webm"
            ".zip" -> "application/zip"
            ".gz", ".gzip" -> "application/gzip"
            ".rar" -> "application/vnd.rar"
            ".7z" -> "application/x-7z-compressed"
            ".apk" -> "application/vnd.android.package-archive"
            ".sql" -> "application/sql"
            else -> {
                val extWithoutDot = ext.removePrefix(".")
                android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extWithoutDot)
                    ?: "application/octet-stream"
            }
        }
    }

    private fun launchFileChooserIntent(params: android.webkit.WebChromeClient.FileChooserParams?) {
        try {
            val needsCamera = isCameraRequiredForChooser(params)
            val hasCam = needsCamera && androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val extraIntents = mutableListOf<android.content.Intent>()
            if (hasCam) {
                try {
                    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                    val dir = java.io.File(cacheDir, "camera_photos").apply { mkdirs() }
                    val photoFile = java.io.File.createTempFile("IMG_${ts}_", ".jpg", dir)
                    cameraPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                        this, "${packageName}.fileprovider", photoFile
                    )
                    val camIntent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
                        addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    if (camIntent.resolveActivity(packageManager) != null) extraIntents.add(camIntent)
                    val vidIntent = android.content.Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
                    if (vidIntent.resolveActivity(packageManager) != null) extraIntents.add(vidIntent)
                } catch (e: Exception) {
                    AppLogger.e("WebViewActivity", "Camera intent failed", e)
                }
            }


            val rawAcceptTypes = params?.acceptTypes ?: arrayOf("*/*")
            val resolvedMimeTypes = rawAcceptTypes
                .filter { !it.isNullOrBlank() }
                .map { type ->
                    if (type.startsWith(".")) extensionToMimeTypeForChooser(type.lowercase()) else type
                }
                .distinct()

            val mimeType = when {
                resolvedMimeTypes.isEmpty() -> "*/*"
                resolvedMimeTypes.size == 1 -> resolvedMimeTypes[0]
                else -> "*/*"
            }

            val contentIntent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = mimeType
                if (params?.mode == android.webkit.WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                    putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                if (resolvedMimeTypes.size > 1) {
                    putExtra(android.content.Intent.EXTRA_MIME_TYPES, resolvedMimeTypes.toTypedArray())
                    type = "*/*"
                }
            }
            val chooser = android.content.Intent.createChooser(contentIntent, null).apply {
                if (extraIntents.isNotEmpty()) putExtra(android.content.Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
            }
            fileChooserActivityLauncher.launch(chooser)
        } catch (e: Exception) {
            AppLogger.e("WebViewActivity", "File chooser launch failed", e)
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        pendingPermissionRequest?.let { request ->
            if (allGranted) {
                request.grant(request.resources)
            } else {
                request.deny()
            }
            pendingPermissionRequest = null
        }
    }


    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        pendingGeolocationCallback?.invoke(pendingGeolocationOrigin, granted, false)
        pendingGeolocationOrigin = null
        pendingGeolocationCallback = null
    }


    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AppLogger.d("WebViewActivity", "Notification permission granted")
        } else {
            AppLogger.d("WebViewActivity", "Notification permission denied")
        }
    }




    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }








    fun handlePermissionRequest(request: PermissionRequest) {
        val resources = request.resources
        val androidPermissions = mutableListOf<String>()

        resources.forEach { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    androidPermissions.add(android.Manifest.permission.CAMERA)
                }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {


                    androidPermissions.add(android.Manifest.permission.RECORD_AUDIO)
                }
                PermissionRequest.RESOURCE_MIDI_SYSEX -> {

                }
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {

                }
            }
        }

        val uniquePermissions = androidPermissions.distinct()

        if (uniquePermissions.isEmpty()) {

            request.grant(resources)
            return
        }


        val notGranted = uniquePermissions.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, it
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {

            request.grant(resources)
        } else {

            pendingPermissionRequest = request
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }




    fun handleGeolocationPermission(origin: String?, callback: GeolocationPermissions.Callback?) {
        pendingGeolocationOrigin = origin
        pendingGeolocationCallback = callback
        locationPermissionLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }


    private var usageTracker: AppUsageTracker? = null
    private var trackedAppId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {

        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            AppLogger.w("WebViewActivity", "enableEdgeToEdge failed", e)
        }

        super.onCreate(savedInstanceState)


        requestNotificationPermissionIfNeeded()



        immersiveFullscreenEnabled = false
        applyImmersiveFullscreen(immersiveFullscreenEnabled)

        val appId = intent.getLongExtra(EXTRA_APP_ID, -1)


        if (appId > 0) {
            trackedAppId = appId
            try {
                usageTracker = org.koin.java.KoinJavaComponent.get(AppUsageTracker::class.java)
                usageTracker?.trackLaunch(appId)
            } catch (e: Exception) {
                AppLogger.w("WebViewActivity", "Usage tracker init failed: ${e.message}")
            }
        }
        val directUrl = intent.getStringExtra(EXTRA_URL)


        val testUrl = intent.getStringExtra(EXTRA_TEST_URL)
        val testModuleIds = intent.getStringArrayListExtra(EXTRA_TEST_MODULE_IDS)


        val previewAppJson = intent.getStringExtra(EXTRA_PREVIEW_APP_JSON)
        val previewApp: com.webtoapp.data.model.WebApp? = if (!previewAppJson.isNullOrBlank()) {
            try {

                com.webtoapp.data.converter.Converters.gson.fromJson(
                    previewAppJson, com.webtoapp.data.model.WebApp::class.java
                )
            } catch (e: Exception) {
                AppLogger.w("WebViewActivity", "Failed to parse preview WebApp JSON: ${e.message}")
                null
            }
        } else null

        setContent {
            WebToAppTheme { isDarkTheme ->

                LaunchedEffect(isDarkTheme, statusBarColorMode) {
                    if (!immersiveFullscreenEnabled) {
                        applyStatusBarColor(statusBarColorMode, statusBarCustomColor, statusBarDarkIcons, isDarkTheme)
                    }
                }

                WebViewScreen(
                    appId = appId,
                    directUrl = directUrl,
                    previewApp = previewApp,
                    testUrl = testUrl,
                    testModuleIds = testModuleIds,
                    onStatusBarConfigChanged = { colorMode, customColor, darkIcons, showStatusBar, backgroundType, colorModeDark, customColorDark, darkIconsDark, backgroundTypeDark ->

                        statusBarColorMode = colorMode
                        statusBarCustomColor = customColor
                        statusBarDarkIcons = darkIcons
                        showStatusBarInFullscreen = showStatusBar
                        statusBarBackgroundType = backgroundType

                        statusBarColorModeDark = colorModeDark
                        statusBarCustomColorDark = customColorDark
                        statusBarDarkIconsDark = darkIconsDark
                        statusBarBackgroundTypeDark = backgroundTypeDark
                    },
                    onWebViewCreated = { wv ->
                        webView = wv


                        wv.onResume()
                        wv.resumeTimers()

                        val downloadBridge = com.webtoapp.core.webview.DownloadBridge(this@WebViewActivity, lifecycleScope)
                        wv.addJavascriptInterface(downloadBridge, com.webtoapp.core.webview.DownloadBridge.JS_INTERFACE_NAME)

                        val nativeBridge = com.webtoapp.core.webview.NativeBridge(this@WebViewActivity, lifecycleScope) { wv }
                        wv.addJavascriptInterface(nativeBridge, com.webtoapp.core.webview.NativeBridge.JS_INTERFACE_NAME)
                    },
                    onFileChooser = { callback, params ->
                        handleFileChooser(callback, params)
                    },
                    onShowCustomView = { view, callback ->
                        customView = view
                        customViewCallback = callback
                        showCustomView(view)
                    },
                    onHideCustomView = {
                        hideCustomView()
                    },
                    onFullscreenModeChanged = { enabled ->
                        immersiveFullscreenEnabled = enabled
                        if (customView == null) {
                            applyImmersiveFullscreen(enabled)
                        }
                    }
                )
            }
        }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    customView != null -> hideCustomView()
                    else -> {

                        val wv = webView
                        if (wv != null) {
                            wv.evaluateJavascript("""
                                (function() {
                                    var evt = new KeyboardEvent('keydown', {
                                        key: 'Escape', code: 'Escape',
                                        keyCode: 27, which: 27,
                                        bubbles: true, cancelable: true
                                    });
                                    return !document.dispatchEvent(evt);
                                })();
                            """.trimIndent()) { result ->
                                if (result == "true") {

                                    return@evaluateJavascript
                                }




                                ShellWebViewNavigation.goBackOrFinish(this@WebViewActivity, wv)
                            }
                        } else {
                            finish()
                        }
                    }
                }
            }
        })
    }

    private fun showCustomView(view: View) {
        originalOrientationBeforeFullscreen = WindowHelper.showCustomView(this, view)
        applyImmersiveFullscreen(true)
    }

    private fun hideCustomView() {
        customView?.let { view ->
            WindowHelper.hideCustomView(this, view, customViewCallback, originalOrientationBeforeFullscreen)
            customView = null
            customViewCallback = null
            originalOrientationBeforeFullscreen = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            applyImmersiveFullscreen(immersiveFullscreenEnabled)
        }
    }

    private fun shouldForwardKeyToWebView(event: KeyEvent): Boolean {
        if (event.isSystem) {
            return false
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_POWER -> false
            else -> true
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (shouldForwardKeyToWebView(event) && webView?.dispatchKeyEvent(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen(customView != null || immersiveFullscreenEnabled)
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        webView?.resumeTimers()
        if (trackedAppId > 0) usageTracker?.trackResume(trackedAppId)
    }

    override fun onPause() {
        if (trackedAppId > 0) usageTracker?.trackPause(trackedAppId)
        webView?.onPause()
        android.webkit.CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)




        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            com.webtoapp.core.logging.AppLogger.w("WebViewActivity", "Memory pressure (level=$level), skipped manual GC")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()



        com.webtoapp.core.logging.AppLogger.w("WebViewActivity", "Low memory, skipped manual GC")
    }

    override fun onDestroy() {

        if (trackedAppId > 0) usageTracker?.trackClose(trackedAppId)


        android.webkit.CookieManager.getInstance().flush()
        webView?.let { wv ->
            wv.stopLoading()



            wv.onPause()
            wv.webChromeClient = null
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
        webView = null


        try {
            com.webtoapp.core.webview.PacProxyManager(this).clearProxy()
        } catch (_: Exception) {}
        com.webtoapp.core.engine.GeckoViewEngine.applyProxyConfig(
            com.webtoapp.core.engine.ProxyConfig(mode = "NONE")
        )

        super.onDestroy()
    }
}

private class WtaScrollBridge(
    private val onScrollChanged: (Int) -> Unit
) {
    @JavascriptInterface
    fun onScroll(y: Int) {
        onScrollChanged(y)
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    appId: Long,
    directUrl: String?,
    previewApp: com.webtoapp.data.model.WebApp? = null,
    testUrl: String? = null,
    testModuleIds: List<String>? = null,
    onStatusBarConfigChanged: ((com.webtoapp.data.model.StatusBarColorMode, String?, Boolean?, Boolean, com.webtoapp.data.model.StatusBarBackgroundType, com.webtoapp.data.model.StatusBarColorMode, String?, Boolean, com.webtoapp.data.model.StatusBarBackgroundType) -> Unit)? = null,
    onWebViewCreated: (WebView) -> Unit,
    onFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onFullscreenModeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val repository = WebToAppApplication.repository
    val activation = WebToAppApplication.activation
    val announcement = WebToAppApplication.announcement
    val adBlocker = WebToAppApplication.adBlock


    val isTestMode = !testUrl.isNullOrBlank()


    var webApp by remember { mutableStateOf<WebApp?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    var isActivated by remember { mutableStateOf(false) }

    var isActivationChecked by remember { mutableStateOf(false) }

    var webViewRecreationKey by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var adCapabilityNoticeShown by remember { mutableStateOf(false) }
    var strictHostFallbackTriggered by remember { mutableStateOf(false) }


    var showSplash by remember { mutableStateOf(false) }
    var splashCountdown by remember { mutableIntStateOf(0) }
    var originalOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }


    val bgmPlayer = remember { BgmPlayer(context) }


    var webViewRef by remember { mutableStateOf<WebView?>(null) }


    val jsScrollTop = remember { AtomicInteger(0) }
    val scrollBridge: WtaScrollBridge = remember { WtaScrollBridge { y -> jsScrollTop.set(y) } }


    var showLongPressMenu by remember { mutableStateOf(false) }
    var longPressResult by remember { mutableStateOf<LongPressHandler.LongPressResult?>(null) }
    var longPressTouchX by remember { mutableFloatStateOf(0f) }
    var longPressTouchY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val longPressHandler = remember { LongPressHandler(context, scope) }


    var showConsole by remember { mutableStateOf(false) }
    var consoleMessages by remember { mutableStateOf<List<ConsoleLogEntry>>(emptyList()) }


    var statusBarBackgroundType by remember { mutableStateOf("COLOR") }
    var statusBarBackgroundColor by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundImage by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundAlpha by remember { mutableFloatStateOf(1.0f) }
    var statusBarHeightDp by remember { mutableIntStateOf(0) }

    var statusBarBackgroundTypeDarkLocal by remember { mutableStateOf("COLOR") }
    var statusBarBackgroundColorDark by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundImageDark by remember { mutableStateOf<String?>(null) }
    var statusBarBackgroundAlphaDark by remember { mutableFloatStateOf(1.0f) }


    var wordPressPreviewState by remember { mutableStateOf<WordPressPreviewState>(WordPressPreviewState.Idle) }
    val phpRuntime = remember { WordPressPhpRuntime(context) }
    val wpDownloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    var wpRetryTrigger by remember { mutableIntStateOf(0) }


    var phpAppPreviewState by remember { mutableStateOf<PhpAppPreviewState>(PhpAppPreviewState.Idle) }
    val phpAppRuntime = remember { PhpAppRuntime(context) }
    val phpAppDownloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    var phpAppRetryTrigger by remember { mutableIntStateOf(0) }


    var pythonAppPreviewState by remember { mutableStateOf<PythonAppPreviewState>(PythonAppPreviewState.Idle) }
    val pythonRuntime = remember { com.webtoapp.core.python.PythonRuntime(context) }
    val pythonHttpServer = remember { com.webtoapp.core.webview.LocalHttpServer(context) }
    var pythonAppRetryTrigger by remember { mutableIntStateOf(0) }


    var nodeJsAppPreviewState by remember { mutableStateOf<NodeJsAppPreviewState>(NodeJsAppPreviewState.Idle) }
    val nodeRuntime = remember { com.webtoapp.core.nodejs.NodeRuntime(context) }
    val nodeHttpServer = remember { com.webtoapp.core.webview.LocalHttpServer(context) }
    var nodeJsAppRetryTrigger by remember { mutableIntStateOf(0) }


    var goAppPreviewState by remember { mutableStateOf<GoAppPreviewState>(GoAppPreviewState.Idle) }
    val goRuntime = remember { com.webtoapp.core.golang.GoRuntime(context) }
    val goHttpServer = remember { com.webtoapp.core.webview.LocalHttpServer(context) }
    var goAppRetryTrigger by remember { mutableIntStateOf(0) }


    LaunchedEffect(webApp) {
        webApp?.let { app ->
            onStatusBarConfigChanged?.invoke(
                app.webViewConfig.statusBarColorMode,
                app.webViewConfig.statusBarColor,
                app.webViewConfig.statusBarDarkIcons,
                app.webViewConfig.showStatusBarInFullscreen,
                app.webViewConfig.statusBarBackgroundType,
                app.webViewConfig.statusBarColorModeDark,
                app.webViewConfig.statusBarColorDark,
                app.webViewConfig.statusBarDarkIconsDark,
                app.webViewConfig.statusBarBackgroundTypeDark
            )

            statusBarBackgroundType = app.webViewConfig.statusBarBackgroundType.name
            statusBarBackgroundColor = app.webViewConfig.statusBarColor
            statusBarBackgroundImage = app.webViewConfig.statusBarBackgroundImage
            statusBarBackgroundAlpha = app.webViewConfig.statusBarBackgroundAlpha
            statusBarHeightDp = app.webViewConfig.statusBarHeightDp

            statusBarBackgroundTypeDarkLocal = app.webViewConfig.statusBarBackgroundTypeDark.name
            statusBarBackgroundColorDark = app.webViewConfig.statusBarColorDark
            statusBarBackgroundImageDark = app.webViewConfig.statusBarBackgroundImageDark
            statusBarBackgroundAlphaDark = app.webViewConfig.statusBarBackgroundAlphaDark

            (context as? WebViewActivity)?.let { activity ->
                activity.showNavigationBarInFullscreen = app.webViewConfig.showNavigationBarInFullscreen
                activity.keyboardAdjustMode = app.webViewConfig.keyboardAdjustMode

                WindowHelper.applyKeyboardModeOnly(
                    activity = activity,
                    keyboardAdjustMode = app.webViewConfig.keyboardAdjustMode,
                    tag = "WebViewActivity"
                )
            }

            if (!adCapabilityNoticeShown && hasConfiguredAds(app)) {
                adCapabilityNoticeShown = true
                AppLogger.w(
                    "WebViewActivity",
                    "Ad config detected for appId=${app.id}, but AdManager is placeholder-only and no ad SDK is integrated"
                )
                Toast.makeText(context, Strings.adSdkNotIntegrated, Toast.LENGTH_LONG).show()
            }
        }
    }


    LaunchedEffect(appId, directUrl, testUrl, previewApp) {

        if (isTestMode) {
            isActivated = true
            isActivationChecked = true
            return@LaunchedEffect
        }



        if (previewApp != null) {
            webApp = previewApp
            isActivated = true
            isActivationChecked = true


            if (previewApp.adBlockEnabled) {
                adBlocker.initialize(previewApp.adBlockRules, useDefaultRules = true)
                adBlocker.setEnabled(true)
            }


            when (previewApp.webViewConfig.orientationMode) {
                com.webtoapp.data.model.OrientationMode.LANDSCAPE -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                }
                com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
                com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
                com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
                com.webtoapp.data.model.OrientationMode.AUTO -> {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                }
                com.webtoapp.data.model.OrientationMode.PORTRAIT -> {
                    if (com.webtoapp.util.TvUtils.isTv(context)) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    } else {
                        @android.annotation.SuppressLint("SourceLockedOrientationActivity")
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
            }


            if (previewApp.webViewConfig.screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.ALWAYS) {
                activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            return@LaunchedEffect
        }


        if (!directUrl.isNullOrBlank()) {
            isActivated = true
            isActivationChecked = true
            return@LaunchedEffect
        }

        if (appId > 0) {
            val app = repository.getWebApp(appId)
            webApp = app
            if (app != null) {

                if (app.adBlockEnabled) {
                    adBlocker.initialize(app.adBlockRules, useDefaultRules = true)
                    adBlocker.setEnabled(true)
                }


                if (app.activationEnabled) {

                    if (app.activationRequireEveryTime) {
                        activation.resetActivation(appId)
                        isActivated = false
                        isActivationChecked = true
                        showActivationDialog = true
                    } else {
                        val activated = activation.isActivated(appId).first()
                        isActivated = activated
                        isActivationChecked = true
                        if (!activated) {
                            showActivationDialog = true
                        }
                    }
                } else {

                    isActivated = true
                    isActivationChecked = true
                }


                if (app.announcementEnabled && isActivated && app.announcement?.triggerOnLaunch == true) {
                    val shouldShow = announcement.shouldShowAnnouncementForTrigger(
                        appId,
                        app.announcement,
                        isLaunch = true
                    )
                    showAnnouncementDialog = shouldShow
                }


                if (app.splashEnabled && app.splashConfig != null && isActivated) {
                    val mediaPath = app.splashConfig.mediaPath
                    if (mediaPath != null && File(mediaPath).exists()) {
                        showSplash = true
                        splashCountdown = app.splashConfig.duration


                        if (app.splashConfig.orientation == SplashOrientation.LANDSCAPE) {
                            originalOrientation = activity.requestedOrientation
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }
                }


                if (app.bgmEnabled && app.bgmConfig != null && isActivated) {
                    bgmPlayer.initialize(app.bgmConfig)
                }


                when (app.webViewConfig.orientationMode) {
                    com.webtoapp.data.model.OrientationMode.LANDSCAPE -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    }
                    com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    }
                    com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                    com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE -> {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                    com.webtoapp.data.model.OrientationMode.AUTO -> {

                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    }
                    com.webtoapp.data.model.OrientationMode.PORTRAIT -> {
                        if (com.webtoapp.util.TvUtils.isTv(context)) {

                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {

                            @android.annotation.SuppressLint("SourceLockedOrientationActivity")
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                }


                val awakeMode = app.webViewConfig.screenAwakeMode
                when (awakeMode) {
                    com.webtoapp.data.model.ScreenAwakeMode.ALWAYS -> {
                        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    com.webtoapp.data.model.ScreenAwakeMode.TIMED -> {
                        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                        val timeoutMs = app.webViewConfig.screenAwakeTimeoutMinutes * 60 * 1000L
                        kotlinx.coroutines.MainScope().launch {
                            kotlinx.coroutines.delay(timeoutMs)
                            activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                    com.webtoapp.data.model.ScreenAwakeMode.OFF -> {

                        if (app.webViewConfig.keepScreenOn) {
                            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }


                val brightness = app.webViewConfig.screenBrightness
                if (brightness in 0..100) {
                    val lp = activity.window.attributes
                    lp.screenBrightness = brightness / 100f
                    activity.window.attributes = lp
                }
            } else {

                isActivated = true
                isActivationChecked = true
            }
        } else {

            isActivated = true
            isActivationChecked = true
        }
    }


    DisposableEffect(Unit) {

        if (webApp?.announcementEnabled == true && webApp?.announcement?.triggerOnNoNetwork == true) {
            announcement.startNetworkMonitoring()
        }

        onDispose {
            bgmPlayer.release()
            announcement.stopNetworkMonitoring()
        }
    }


    val networkAvailable by announcement.isNetworkAvailable.collectAsStateWithLifecycle()
    var lastNetworkState by remember { mutableStateOf(true) }

    LaunchedEffect(networkAvailable, webApp, isActivated) {

        if (lastNetworkState && !networkAvailable && isActivated) {
            val app = webApp
            if (app != null && app.announcementEnabled && app.announcement?.triggerOnNoNetwork == true) {
                val shouldShow = announcement.shouldShowAnnouncementForTrigger(
                    appId,
                    app.announcement,
                    isNoNetwork = true
                )
                if (shouldShow && !showAnnouncementDialog) {
                    showAnnouncementDialog = true
                }
            }
        }
        lastNetworkState = networkAvailable
    }


    LaunchedEffect(webApp, isActivated) {
        val app = webApp ?: return@LaunchedEffect
        if (!isActivated) return@LaunchedEffect

        val intervalMinutes = app.announcement?.triggerIntervalMinutes ?: 0
        if (!app.announcementEnabled || intervalMinutes <= 0) return@LaunchedEffect


        if (app.announcement?.triggerIntervalIncludeLaunch == true) {
            announcement.resetIntervalTrigger(appId)
        }

        while (true) {
            val nextDelay = announcement.getMillisUntilNextIntervalAnnouncement(appId, app.announcement)
            delay(nextDelay.coerceIn(1_000L, intervalMinutes * 60 * 1000L))

            if (!isActivated || webApp != app) break

            if (announcement.shouldTriggerIntervalAnnouncement(appId, app.announcement)) {
                val shouldShow = announcement.shouldShowAnnouncementForTrigger(
                    appId,
                    app.announcement,
                    isInterval = true
                )
                if (shouldShow && !showAnnouncementDialog) {
                    showAnnouncementDialog = true
                    announcement.markIntervalTrigger(appId)
                }
            }
        }
    }


    LaunchedEffect(showSplash, splashCountdown) {

        if (webApp?.splashConfig?.type == SplashType.VIDEO) return@LaunchedEffect

        if (showSplash && splashCountdown > 0) {
            delay(1000L)
            splashCountdown--
        } else if (showSplash && splashCountdown <= 0) {
            showSplash = false

            if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                activity.requestedOrientation = originalOrientation
                originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }


    LaunchedEffect(webApp, isActivated, isActivationChecked, wpRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.WORDPRESS) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        wordPressPreviewState = WordPressPreviewState.CheckingDeps


        if (!WordPressDependencyManager.isAllReady(context)) {
            wordPressPreviewState = WordPressPreviewState.Downloading
            val success = WordPressDependencyManager.downloadAllDependencies(context)
            if (!success) {
                wordPressPreviewState = WordPressPreviewState.Error(Strings.wpDownloadFailed)
                return@LaunchedEffect
            }
        }


        var projectId = app.wordpressConfig?.projectId ?: ""
        val projectDir = if (projectId.isNotEmpty()) {
            WordPressManager.getProjectDir(context, projectId)
        } else null

        if (projectDir == null || !projectDir.exists() || !File(projectDir, "wp-includes/version.php").exists()) {
            wordPressPreviewState = WordPressPreviewState.CreatingProject
            val newId = WordPressManager.createProject(
                context = context,
                siteTitle = app.wordpressConfig?.siteTitle ?: "My Site",
                adminUser = app.wordpressConfig?.adminUser ?: "admin",
                adminEmail = app.wordpressConfig?.adminEmail ?: ""
            )
            if (newId == null) {
                wordPressPreviewState = WordPressPreviewState.Error(Strings.wpProjectCreateFailed)
                return@LaunchedEffect
            }
            projectId = newId

            val updatedConfig = (app.wordpressConfig ?: WordPressConfig()).copy(projectId = newId)
            repository.updateWebApp(app.copy(wordpressConfig = updatedConfig))
            webApp = app.copy(wordpressConfig = updatedConfig)
        }


        wordPressPreviewState = WordPressPreviewState.StartingServer
        val wpDir = WordPressManager.getProjectDir(context, projectId)
        WordPressManager.ensureDbPhpExists(context, wpDir)
        val port = phpRuntime.startServer(wpDir.absolutePath, app.wordpressConfig?.phpPort ?: 0)

        if (port > 0) {
            val url = "http://127.0.0.1:$port/"

            WordPressManager.autoInstallIfNeeded(
                baseUrl = "http://127.0.0.1:$port",
                siteTitle = app.wordpressConfig?.siteTitle?.takeIf { it.isNotBlank() } ?: "My Site",
                adminUser = app.wordpressConfig?.adminUser?.takeIf { it.isNotBlank() } ?: "admin",
                adminPassword = app.wordpressConfig?.adminPassword?.takeIf { it.isNotBlank() } ?: "admin",
                adminEmail = app.wordpressConfig?.adminEmail?.takeIf { it.isNotBlank() } ?: "admin@localhost.local",
                siteLanguage = app.wordpressConfig?.siteLanguage?.takeIf { it.isNotBlank() } ?: "en_US"
            )
            WordPressManager.applyRuntimeConfig(
                phpBinary = phpRuntime.getPhpBinaryPath(),
                projectDir = wpDir,
                siteTitle = app.wordpressConfig?.siteTitle?.takeIf { it.isNotBlank() } ?: "My Site",
                permalinkStructure = app.wordpressConfig?.permalinkStructure ?: "/%postname%/",
                siteLanguage = app.wordpressConfig?.siteLanguage?.takeIf { it.isNotBlank() } ?: "en_US",
                themeName = app.wordpressConfig?.themeName ?: "",
                activePlugins = app.wordpressConfig?.activePlugins ?: emptyList()
            )
            wordPressPreviewState = WordPressPreviewState.Ready(url)
            delay(200)
            webViewRef?.loadUrl(url)
        } else {
            wordPressPreviewState = WordPressPreviewState.Error(Strings.wpServerError)
        }
    }


    DisposableEffect(phpRuntime) {
        onDispose {
            phpRuntime.stopServer()
        }
    }


    LaunchedEffect(webApp, isActivated, isActivationChecked, phpAppRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.PHP_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        AppLogger.i("PhpAppPreview", "开始 PHP 应用预览流程, appId=$appId, phpAppConfig=${app.phpAppConfig}")

        val config = app.phpAppConfig
        if (config == null) {
            AppLogger.e("PhpAppPreview", "phpAppConfig 为 null，无法启动预览")
            phpAppPreviewState = PhpAppPreviewState.Error(Strings.phpAppProjectNotFound)
            return@LaunchedEffect
        }

        phpAppPreviewState = PhpAppPreviewState.CheckingDeps
        AppLogger.i("PhpAppPreview", "检查 PHP 依赖, isPhpReady=${WordPressDependencyManager.isPhpReady(context)}")


        if (!WordPressDependencyManager.isPhpReady(context)) {
            phpAppPreviewState = PhpAppPreviewState.Downloading
            val success = WordPressDependencyManager.downloadPhpDependency(context)
            if (!success) {
                phpAppPreviewState = PhpAppPreviewState.Error(Strings.phpAppDownloadFailed)
                return@LaunchedEffect
            }
        }


        val projectId = config.projectId
        AppLogger.i("PhpAppPreview", "projectId='$projectId', docRoot='${config.documentRoot}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("PhpAppPreview", "projectId 为空")
            phpAppPreviewState = PhpAppPreviewState.Error(Strings.phpAppProjectNotFound)
            return@LaunchedEffect
        }
        val projectDir = phpAppRuntime.getProjectDir(projectId)
        AppLogger.i("PhpAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            phpAppPreviewState = PhpAppPreviewState.Error(Strings.phpAppProjectNotFound)
            return@LaunchedEffect
        }


        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("PhpAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }


        var actualDocRoot = config.documentRoot
        var actualEntryFile = config.entryFile


        var actualProjectDir = projectDir
        val docRootDir = if (actualDocRoot.isNotBlank()) File(projectDir, actualDocRoot) else projectDir
        if (!File(docRootDir, actualEntryFile).exists()) {
            AppLogger.i("PhpAppPreview", "入口文件不存在，尝试自动检测框架...")


            var detectedFramework = phpAppRuntime.detectFramework(projectDir)
            var detectedDocRoot = phpAppRuntime.detectDocumentRoot(projectDir, detectedFramework)
            var detectedEntry = phpAppRuntime.detectEntryFile(projectDir, detectedDocRoot)


            val detectedDocRootDir = if (detectedDocRoot.isNotBlank()) File(projectDir, detectedDocRoot) else projectDir
            if (!File(detectedDocRootDir, detectedEntry).exists()) {
                AppLogger.i("PhpAppPreview", "根目录未找到入口文件，扫描子目录...")
                val phpSubDir = projectDir.listFiles()
                    ?.filter { it.isDirectory && it.name != "__MACOSX" && !it.name.startsWith("._") }
                    ?.firstOrNull { sub -> sub.listFiles()?.any { it.isFile && it.extension == "php" } == true }

                if (phpSubDir != null) {
                    AppLogger.i("PhpAppPreview", "找到 PHP 子目录: ${phpSubDir.name}")
                    actualProjectDir = phpSubDir
                    detectedFramework = phpAppRuntime.detectFramework(phpSubDir)
                    detectedDocRoot = phpAppRuntime.detectDocumentRoot(phpSubDir, detectedFramework)
                    detectedEntry = phpAppRuntime.detectEntryFile(phpSubDir, detectedDocRoot)
                }
            }

            AppLogger.i("PhpAppPreview", "自动检测: framework=$detectedFramework, docRoot='$detectedDocRoot', entry='$detectedEntry', projectDir=${actualProjectDir.name}")
            actualDocRoot = detectedDocRoot
            actualEntryFile = detectedEntry
        }


        phpAppPreviewState = PhpAppPreviewState.StartingServer
        AppLogger.i("PhpAppPreview", "启动 PHP 服务器: docRoot='$actualDocRoot', entry='$actualEntryFile'")
        val port = phpAppRuntime.startServer(
            projectDir = actualProjectDir.absolutePath,
            documentRoot = actualDocRoot,
            entryFile = actualEntryFile,
            port = config.phpPort,
            envVars = config.envVars
        )

        if (port > 0) {
            val url = "http://127.0.0.1:$port/"
            AppLogger.i("PhpAppPreview", "PHP 服务器已启动: $url")
            phpAppPreviewState = PhpAppPreviewState.Ready(url)
            delay(200)
            webViewRef?.loadUrl(url)
        } else {
            AppLogger.e("PhpAppPreview", "PHP 服务器启动失败, port=$port, serverState=${phpAppRuntime.serverState.value}")
            val errorDetail = when (val state = phpAppRuntime.serverState.value) {
                is PhpAppRuntime.ServerState.Error -> state.message
                else -> Strings.phpAppServerError
            }
            phpAppPreviewState = PhpAppPreviewState.Error(errorDetail)
        }
    }


    DisposableEffect(phpAppRuntime) {
        onDispose {
            phpAppRuntime.stopServer()
        }
    }


    LaunchedEffect(webApp, isActivated, isActivationChecked, pythonAppRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.PYTHON_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        val config = app.pythonAppConfig
        if (config == null) {
            AppLogger.e("PythonAppPreview", "pythonAppConfig 为 null")
            pythonAppPreviewState = PythonAppPreviewState.Error(Strings.pyProjectNotFound)
            return@LaunchedEffect
        }

        AppLogger.i("PythonAppPreview", "开始 Python 应用预览流程, appId=$appId, config=$config")
        pythonAppPreviewState = PythonAppPreviewState.Starting


        val projectId = config.projectId
        AppLogger.i("PythonAppPreview", "projectId='$projectId', framework='${config.framework}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("PythonAppPreview", "projectId 为空")
            pythonAppPreviewState = PythonAppPreviewState.Error(Strings.pyProjectNotFound)
            return@LaunchedEffect
        }
        val projectDir = pythonRuntime.getProjectDir(projectId)
        AppLogger.i("PythonAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            pythonAppPreviewState = PythonAppPreviewState.Error(Strings.pyProjectNotFound)
            return@LaunchedEffect
        }


        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("PythonAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }


        var actualProjectDir = projectDir
        var actualEntryFile = config.entryFile.ifBlank { "app.py" }
        var actualFramework = config.framework.ifBlank { "raw" }

        if (!File(actualProjectDir, actualEntryFile).exists()) {
            AppLogger.i("PythonAppPreview", "入口文件不存在: $actualEntryFile，尝试自动检测...")


            val detectedFramework = pythonRuntime.detectFramework(projectDir)
            val detectedEntry = pythonRuntime.detectEntryFile(projectDir, detectedFramework)

            if (File(projectDir, detectedEntry).exists()) {
                AppLogger.i("PythonAppPreview", "自动检测到: framework=$detectedFramework, entry=$detectedEntry")
                actualFramework = detectedFramework
                actualEntryFile = detectedEntry
            } else {

                AppLogger.i("PythonAppPreview", "根目录未找到入口文件，扫描子目录...")
                val pySubDir = projectDir.listFiles()
                    ?.filter { it.isDirectory && it.name != "__MACOSX" && it.name != "__pycache__" && !it.name.startsWith("._") && it.name != "venv" && it.name != ".venv" && it.name != ".git" }
                    ?.firstOrNull { sub ->
                        sub.listFiles()?.any { it.isFile && it.extension == "py" } == true
                    }

                if (pySubDir != null) {
                    AppLogger.i("PythonAppPreview", "找到 Python 子目录: ${pySubDir.name}")
                    actualProjectDir = pySubDir
                    actualFramework = pythonRuntime.detectFramework(pySubDir)
                    actualEntryFile = pythonRuntime.detectEntryFile(pySubDir, actualFramework)
                    AppLogger.i("PythonAppPreview", "子目录检测: framework=$actualFramework, entry=$actualEntryFile")
                }
            }
        }

        AppLogger.i("PythonAppPreview", "最终配置: projectDir=${actualProjectDir.absolutePath}, framework=$actualFramework, entry=$actualEntryFile")


        try {
            val candidates = listOf("dist", "build", "public", "static", "www", "templates", "")
            var docRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) actualProjectDir else File(actualProjectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("PythonAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    docRoot = candidate
                    AppLogger.i("PythonAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }

            if (docRoot != null) {
                val url = pythonHttpServer.start(docRoot)
                AppLogger.i("PythonAppPreview", "LocalHttpServer 已启动: $url")
                pythonAppPreviewState = PythonAppPreviewState.Ready(url)
                delay(200)
                webViewRef?.loadUrl(url)
            } else if (pythonRuntime.isPythonAvailable()) {

                AppLogger.i("PythonAppPreview", "Python 运行时可用，启动后端服务器")
                pythonAppPreviewState = PythonAppPreviewState.StartingServer

                val serverPort = pythonRuntime.startServer(
                    projectDir = actualProjectDir.absolutePath,
                    entryFile = actualEntryFile,
                    framework = actualFramework,
                    port = config.serverPort,
                    envVars = config.envVars,
                    installDeps = config.hasPipDeps
                )

                if (serverPort > 0) {
                    val serverUrl = "http://127.0.0.1:$serverPort"
                    AppLogger.i("PythonAppPreview", "Python 服务器已启动: $serverUrl")
                    pythonAppPreviewState = PythonAppPreviewState.Ready(serverUrl)
                    delay(200)
                    webViewRef?.loadUrl(serverUrl)
                } else {
                    AppLogger.e("PythonAppPreview", "Python 服务器启动失败，回退到预览模式")

                    val url = pythonHttpServer.start(actualProjectDir)
                    File(actualProjectDir, "_preview_.html").delete()
                    val previewHtml = pythonRuntime.generatePreviewHtml(
                        projectDir = actualProjectDir,
                        framework = actualFramework,
                        entryFile = actualEntryFile
                    )
                    val previewFile = File(actualProjectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    pythonAppPreviewState = PythonAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            } else {

                AppLogger.w("PythonAppPreview", "Python 运行时不可用，生成项目预览页面")
                val url = pythonHttpServer.start(actualProjectDir)
                File(actualProjectDir, "_preview_.html").delete()

                val htmlFiles = actualProjectDir.walkTopDown().filter { it.extension == "html" && it.name != "_preview_.html" }.take(1).toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(actualProjectDir).path
                    val targetUrl = "$url/$relPath"
                    pythonAppPreviewState = PythonAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                } else {
                    val previewHtml = pythonRuntime.generatePreviewHtml(
                        projectDir = actualProjectDir,
                        framework = actualFramework,
                        entryFile = actualEntryFile
                    )
                    val previewFile = File(actualProjectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    pythonAppPreviewState = PythonAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("PythonAppPreview", "启动预览失败", e)
            pythonAppPreviewState = PythonAppPreviewState.Error(e.message ?: Strings.pyPreviewFailed)
        }
    }


    DisposableEffect(pythonHttpServer) {
        onDispose {
            pythonHttpServer.stop()
            pythonRuntime.stopServer()
        }
    }


    LaunchedEffect(webApp, isActivated, isActivationChecked, nodeJsAppRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.NODEJS_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        val config = app.nodejsConfig
        if (config == null) {
            AppLogger.e("NodeJsAppPreview", "nodejsConfig 为 null")
            nodeJsAppPreviewState = NodeJsAppPreviewState.Error(Strings.nodeProjectNotFound)
            return@LaunchedEffect
        }

        AppLogger.i("NodeJsAppPreview", "开始 Node.js 应用预览流程, appId=$appId, config=$config")
        nodeJsAppPreviewState = NodeJsAppPreviewState.Starting


        val projectId = config.projectId
        AppLogger.i("NodeJsAppPreview", "projectId='$projectId', framework='${config.framework}', entry='${config.entryFile}'")
        if (projectId.isBlank()) {
            AppLogger.e("NodeJsAppPreview", "projectId 为空")
            nodeJsAppPreviewState = NodeJsAppPreviewState.Error(Strings.nodeProjectNotFound)
            return@LaunchedEffect
        }
        val internalProjectPath = nodeRuntime.getProjectDir(projectId).absolutePath
        config.sourceProjectPath
            .takeIf { it.isNotBlank() }
            ?.let(nodeRuntime::resolveSourceProjectDir)
            ?.takeIf { it.absolutePath != internalProjectPath }
            ?.let { sourceDir ->
                try {
                    nodeRuntime.syncProjectFromSource(projectId, sourceDir)
                    AppLogger.i("NodeJsAppPreview", "已从源目录同步 Node 项目: ${sourceDir.absolutePath}")
                } catch (e: Exception) {
                    AppLogger.w("NodeJsAppPreview", "同步源项目失败: ${sourceDir.absolutePath}", e)
                }
            }

        val projectDir = nodeRuntime.getProjectDir(projectId)
        AppLogger.i("NodeJsAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            nodeJsAppPreviewState = NodeJsAppPreviewState.Error(Strings.nodeProjectNotFound)
            return@LaunchedEffect
        }


        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("NodeJsAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }


        try {
            val candidates = listOf("dist", "build", "public", "static", "www", "")
            var foundDocRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) projectDir else File(projectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("NodeJsAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    foundDocRoot = candidate
                    AppLogger.i("NodeJsAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }

            val docRoot = foundDocRoot
            if (docRoot != null) {
                val url = nodeHttpServer.start(docRoot)
                AppLogger.i("NodeJsAppPreview", "LocalHttpServer 已启动: $url")
                nodeJsAppPreviewState = NodeJsAppPreviewState.Ready(url)
                delay(200)
                webViewRef?.loadUrl(url)
            } else {

                AppLogger.w("NodeJsAppPreview", "未找到 index.html，尝试在项目根启动 HTTP 服务器")
                val url = nodeHttpServer.start(projectDir)
                AppLogger.i("NodeJsAppPreview", "LocalHttpServer 在项目根启动: $url")


                File(projectDir, "_preview_.html").delete()


                val htmlFiles = projectDir.walkTopDown().filter { it.extension == "html" && it.name != "_preview_.html" }.take(1).toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(projectDir).path
                    val targetUrl = "$url/$relPath"
                    AppLogger.i("NodeJsAppPreview", "找到 HTML 文件: $relPath, URL=$targetUrl")
                    nodeJsAppPreviewState = NodeJsAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                } else {

                    AppLogger.i("NodeJsAppPreview", "无静态 HTML，生成项目预览页面")
                    val previewHtml = nodeRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        entryFile = config.entryFile
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    AppLogger.i("NodeJsAppPreview", "预览页面已生成: $targetUrl")
                    nodeJsAppPreviewState = NodeJsAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("NodeJsAppPreview", "启动预览失败", e)
            nodeJsAppPreviewState = NodeJsAppPreviewState.Error(e.message ?: Strings.nodePreviewFailed)
        }
    }


    DisposableEffect(nodeHttpServer) {
        onDispose {
            nodeHttpServer.stop()
        }
    }


    LaunchedEffect(webApp, isActivated, isActivationChecked, goAppRetryTrigger) {
        val app = webApp ?: return@LaunchedEffect
        if (app.appType != com.webtoapp.data.model.AppType.GO_APP) return@LaunchedEffect
        if (!isActivated || !isActivationChecked) return@LaunchedEffect

        val config = app.goAppConfig
        if (config == null) {
            AppLogger.e("GoAppPreview", "goAppConfig 为 null")
            goAppPreviewState = GoAppPreviewState.Error(Strings.goProjectNotFound)
            return@LaunchedEffect
        }

        AppLogger.i("GoAppPreview", "开始 Go 应用预览流程, appId=$appId, config=$config")
        goAppPreviewState = GoAppPreviewState.Starting


        val projectId = config.projectId
        AppLogger.i("GoAppPreview", "projectId='$projectId', framework='${config.framework}', binary='${config.binaryName}'")
        if (projectId.isBlank()) {
            AppLogger.e("GoAppPreview", "projectId 为空")
            goAppPreviewState = GoAppPreviewState.Error(Strings.goProjectNotFound)
            return@LaunchedEffect
        }
        val projectDir = goRuntime.getProjectDir(projectId)
        AppLogger.i("GoAppPreview", "项目目录: ${projectDir.absolutePath}, exists=${projectDir.exists()}")
        if (!projectDir.exists()) {
            goAppPreviewState = GoAppPreviewState.Error(Strings.goProjectNotFound)
            return@LaunchedEffect
        }


        projectDir.listFiles()?.take(20)?.forEach { file ->
            AppLogger.d("GoAppPreview", "  - ${file.name} (${if (file.isDirectory) "dir" else "${file.length()} bytes"})")
        }


        try {
            val candidates = listOf("dist", "build", "public", "static", "web", "www", "")
            var foundDocRoot: File? = null
            for (dir in candidates) {
                val candidate = if (dir.isEmpty()) projectDir else File(projectDir, dir)
                val hasIndex = File(candidate, "index.html").exists()
                AppLogger.d("GoAppPreview", "检查候选: '$dir' -> ${candidate.absolutePath}, isDir=${candidate.isDirectory}, hasIndex=$hasIndex")
                if (candidate.isDirectory && hasIndex) {
                    foundDocRoot = candidate
                    AppLogger.i("GoAppPreview", "找到 docRoot: ${candidate.absolutePath}")
                    break
                }
            }

            val docRoot = foundDocRoot
            if (docRoot != null) {
                val url = goHttpServer.start(docRoot)
                AppLogger.i("GoAppPreview", "LocalHttpServer 已启动: $url")
                goAppPreviewState = GoAppPreviewState.Ready(url)
                delay(200)
                webViewRef?.loadUrl(url)
            } else if (config.binaryName.isNotBlank() || goRuntime.detectBinary(projectDir) != null) {

                AppLogger.i("GoAppPreview", "启动 Go 后端服务器（仅使用预编译二进制）")
                goAppPreviewState = GoAppPreviewState.StartingServer

                val serverPort = goRuntime.startServer(
                    projectDir = projectDir.absolutePath,
                    binaryName = config.binaryName,
                    port = config.serverPort,
                    envVars = config.envVars
                )

                if (serverPort > 0) {
                    val serverUrl = "http://127.0.0.1:$serverPort"
                    AppLogger.i("GoAppPreview", "Go 服务器已启动: $serverUrl")
                    goAppPreviewState = GoAppPreviewState.Ready(serverUrl)
                    delay(200)
                    webViewRef?.loadUrl(serverUrl)
                } else {
                    AppLogger.e("GoAppPreview", "Go 服务器启动失败，回退到预览模式")
                    val url = goHttpServer.start(projectDir)
                    File(projectDir, "_preview_.html").delete()
                    val previewHtml = goRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        binaryName = config.binaryName
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    goAppPreviewState = GoAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            } else {

                AppLogger.w("GoAppPreview", "无可执行二进制，生成项目预览页面")
                val url = goHttpServer.start(projectDir)
                File(projectDir, "_preview_.html").delete()

                val htmlFiles = projectDir.walkTopDown().filter { it.extension == "html" && it.name != "_preview_.html" }.take(1).toList()
                if (htmlFiles.isNotEmpty()) {
                    val relPath = htmlFiles.first().relativeTo(projectDir).path
                    val targetUrl = "$url/$relPath"
                    goAppPreviewState = GoAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                } else {
                    val previewHtml = goRuntime.generatePreviewHtml(
                        projectDir = projectDir,
                        framework = config.framework,
                        binaryName = config.binaryName
                    )
                    val previewFile = File(projectDir, "_preview_.html")
                    previewFile.writeText(previewHtml)
                    val targetUrl = "$url/_preview_.html"
                    goAppPreviewState = GoAppPreviewState.Ready(targetUrl)
                    delay(200)
                    webViewRef?.loadUrl(targetUrl)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("GoAppPreview", "启动预览失败", e)
            goAppPreviewState = GoAppPreviewState.Error(e.message ?: Strings.goPreviewFailed)
        }
    }


    DisposableEffect(goHttpServer) {
        onDispose {
            goHttpServer.stop()
            goRuntime.stopServer()
        }
    }

    fun scheduleStrictHostFallbackProbe(url: String?, source: String, delayMs: Long) {
        if (!STRICT_HOST_AUTO_EXTERNAL_FALLBACK_ENABLED) return
        if (strictHostFallbackTriggered || !shouldSkipLongPressEnhancer(url)) return
        val expectedUrl = url

        webViewRef?.postDelayed({
            val activeWebView = webViewRef ?: return@postDelayed
            if (strictHostFallbackTriggered) return@postDelayed

            val current = activeWebView.url
            if (expectedUrl != null && expectedUrl != current) return@postDelayed

            val probeScript = """
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

            activeWebView.evaluateJavascript(probeScript) { raw ->
                if (strictHostFallbackTriggered) return@evaluateJavascript
                val decoded = decodeEvaluateJavascriptString(raw)
                if (!shouldFallbackToExternalForStrictHost(decoded)) return@evaluateJavascript
                strictHostFallbackTriggered = true
                AppLogger.w("WebViewActivity", "Strict host blank-page probe ($source) triggered external fallback: $expectedUrl metrics=$decoded")
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


    val webViewCallbacks = remember {
        object : WebViewCallbacks {
            override fun onPageStarted(url: String?) {


                if (url == "about:blank") return
                isLoading = true
                currentUrl = url ?: ""
                jsScrollTop.set(0)
                if (!shouldSkipLongPressEnhancer(url)) {
                    strictHostFallbackTriggered = false
                } else {
                    scheduleStrictHostFallbackProbe(url, "page_started", 5500L)
                }
            }

            override fun onPageCommitVisible(url: String?) {
                scheduleStrictHostFallbackProbe(url, "commit_visible", 2200L)
            }

            override fun onUrlChanged(webView: WebView?, url: String?) {

                webView?.let {
                    canGoBack = it.canGoBack()
                    canGoForward = it.canGoForward()
                }
                if (url != null) currentUrl = url
            }

            override fun onPageFinished(url: String?) {
                if (url == "about:blank") return
                isLoading = false
                isRefreshing = false
                currentUrl = url ?: ""
                webViewRef?.let {
                    canGoBack = it.canGoBack()
                    canGoForward = it.canGoForward()


                    it.evaluateJavascript("""
                        (function(){
                            if(window._wtaScrollTrackerInstalled) return;
                            window._wtaScrollTrackerInstalled = true;
                            function report(){
                                var y = Math.round(Math.max(
                                    window.pageYOffset || 0,
                                    document.documentElement ? document.documentElement.scrollTop : 0,
                                    document.body ? document.body.scrollTop : 0
                                ));
                                try { _wtaScrollBridge.onScroll(y); } catch(e) { /* bridge unavailable */ }
                            }
                            window.addEventListener('scroll', report, {passive:true, capture:true});
                            document.addEventListener('scroll', report, {passive:true, capture:true});
                            report();
                        })();
                    """.trimIndent(), null)


                    if (!shouldSkipLongPressEnhancer(url)) {
                        longPressHandler.injectLongPressEnhancer(it)
                    } else {
                        AppLogger.d("WebViewActivity", "Skip long-press enhancer for strict compatibility host: $url")
                    }
                }
                scheduleStrictHostFallbackProbe(url, "page_finished", 1200L)
            }

            override fun onProgressChanged(progress: Int) {
                loadProgress = progress
            }

            override fun onTitleChanged(title: String?) {
                if (title == "about:blank" || title.isNullOrBlank()) return
                pageTitle = title
            }

            override fun onIconReceived(icon: Bitmap?) {}

            override fun onError(errorCode: Int, description: String) {
                errorMessage = description
                isLoading = false
                isRefreshing = false
            }

            override fun onSslError(error: String) {
                errorMessage = context.getString(com.webtoapp.R.string.webview_ssl_error)
            }

            override fun onExternalLink(url: String) {
                try {
                    val safeUrl = normalizeExternalUrlForIntent(url)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    AppLogger.w("WebViewActivity", "No app to handle external link: $url", e)
                    android.widget.Toast.makeText(
                        context,
                        context.getString(com.webtoapp.R.string.webview_cannot_open_link),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                view?.let { onShowCustomView(it, callback) }
            }

            override fun onHideCustomView() {
                onHideCustomView()
            }

            override fun onGeolocationPermission(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {

                (activity as? WebViewActivity)?.handleGeolocationPermission(origin, callback)
                    ?: callback?.invoke(origin, true, false)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {

                request?.let { req ->
                    (activity as? WebViewActivity)?.handlePermissionRequest(req)
                        ?: req.grant(req.resources)
                }
            }

            override fun onShowFileChooser(
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                return onFileChooser(filePathCallback, fileChooserParams)
            }

            override fun onDownloadStart(
                url: String,
                userAgent: String,
                contentDisposition: String,
                mimeType: String,
                contentLength: Long
            ) {


                DownloadHelper.handleDownload(
                    context = context,
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                    scope = scope,
                    onBlobDownload = { blobUrl, filename ->
                        val safeBlobUrl = org.json.JSONObject.quote(blobUrl)
                        val safeFilename = org.json.JSONObject.quote(filename)


                        webViewRef?.evaluateJavascript("""
                            (function() {
                                try {
                                    const blobUrl = $safeBlobUrl;
                                    const filename = $safeFilename;
                                    const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024;
                                    const CHUNK_SIZE = 512 * 1024;

                                    function uint8ToBase64(u8) {
                                        const S = 8192; const p = [];
                                        for (let i = 0; i < u8.length; i += S) p.push(String.fromCharCode.apply(null, u8.subarray(i, i + S)));
                                        return btoa(p.join(''));
                                    }

                                    function processChunked(blob, fname) {
                                        const mimeType = blob.type || 'application/octet-stream';
                                        if (!window.AndroidDownload || !window.AndroidDownload.startChunkedDownload) {
                                            processSmall(blob, fname); return;
                                        }
                                        const did = window.AndroidDownload.startChunkedDownload(fname, mimeType, blob.size);
                                        let off = 0, ci = 0; const tc = Math.ceil(blob.size / CHUNK_SIZE);
                                        function next() {
                                            if (off >= blob.size) { window.AndroidDownload.finishChunkedDownload(did); return; }
                                            blob.slice(off, off + CHUNK_SIZE).arrayBuffer().then(function(ab) {
                                                window.AndroidDownload.appendChunk(did, uint8ToBase64(new Uint8Array(ab)), ci, tc);
                                                off += CHUNK_SIZE; ci++;
                                                setTimeout(next, 0);
                                            });
                                        }
                                        next();
                                    }

                                    function processSmall(blob, fname) {
                                        const reader = new FileReader();
                                        reader.onloadend = function() {
                                            const base64Data = reader.result.split(',')[1];
                                            const mimeType = blob.type || 'application/octet-stream';
                                            if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                                window.AndroidDownload.saveBase64File(base64Data, fname, mimeType);
                                            }
                                        };
                                        reader.readAsDataURL(blob);
                                    }

                                    if (blobUrl.startsWith('data:')) {
                                        const parts = blobUrl.split(',');
                                        const meta = parts[0];
                                        const base64Data = parts[1];
                                        const mimeMatch = meta.match(/data:([^;]+)/);
                                        const mimeType = mimeMatch ? mimeMatch[1] : 'application/octet-stream';
                                        if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                            window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                                        }
                                    } else if (blobUrl.startsWith('blob:')) {
                                        // 优先从 DownloadBridge 的缓存里拿 Blob（页面可能已同步 revoke URL）
                                        const cachedBlob = window.__wtaBlobMap && window.__wtaBlobMap.get(blobUrl);
                                        function dispatch(blob) {
                                            if (blob.size > LARGE_FILE_THRESHOLD) {
                                                processChunked(blob, filename);
                                            } else {
                                                processSmall(blob, filename);
                                            }
                                        }
                                        if (cachedBlob) {
                                            dispatch(cachedBlob);
                                        } else {
                                            fetch(blobUrl)
                                                .then(function(r) { return r.blob(); })
                                                .then(dispatch)
                                                .catch(function(err) {
                                                    console.error('[DownloadHelper] Blob fetch failed:', err);
                                                    if (window.AndroidDownload && window.AndroidDownload.showToast) {
                                                        window.AndroidDownload.showToast('${Strings.downloadFailedWithReason}' + err.message);
                                                    }
                                                });
                                        }
                                    }
                                } catch(e) {
                                    console.error('[DownloadHelper] Error:', e);
                                }
                            })();
                        """.trimIndent(), null)
                    }
                )
            }

            override fun onLongPress(webView: WebView, x: Float, y: Float): Boolean {


                val hitResult = webView.hitTestResult
                val hitType = hitResult.type
                val isLink = hitType == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                             hitType == WebView.HitTestResult.ANCHOR_TYPE


                val menuEnabled = webApp?.webViewConfig?.longPressMenuEnabled ?: true
                if (!menuEnabled) {
                    return isLink
                }


                if (hitType == WebView.HitTestResult.EDIT_TEXT_TYPE ||
                    hitType == WebView.HitTestResult.UNKNOWN_TYPE) {
                    return false
                }


                longPressHandler.getLongPressDetails(webView, x, y) { result ->
                    when (result) {
                        is LongPressHandler.LongPressResult.Image,
                        is LongPressHandler.LongPressResult.Video,
                        is LongPressHandler.LongPressResult.Link,
                        is LongPressHandler.LongPressResult.ImageLink -> {
                            longPressResult = result
                            longPressTouchX = x
                            longPressTouchY = y
                            showLongPressMenu = true
                        }
                        is LongPressHandler.LongPressResult.Text,
                        is LongPressHandler.LongPressResult.None -> {



                        }
                    }
                }


                return when (hitType) {
                    WebView.HitTestResult.IMAGE_TYPE,
                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                    WebView.HitTestResult.SRC_ANCHOR_TYPE,
                    WebView.HitTestResult.ANCHOR_TYPE -> true
                    else -> false
                }
            }

            override fun onConsoleMessage(level: Int, message: String, sourceId: String, lineNumber: Int) {
                val consoleLevel = when (level) {
                    0 -> ConsoleLevel.DEBUG
                    1 -> ConsoleLevel.LOG
                    2 -> ConsoleLevel.INFO
                    3 -> ConsoleLevel.WARNING
                    4 -> ConsoleLevel.ERROR
                    else -> ConsoleLevel.LOG
                }
                consoleMessages = consoleMessages + ConsoleLogEntry(
                    level = consoleLevel,
                    message = message,
                    source = sourceId,
                    lineNumber = lineNumber,
                    timestamp = System.currentTimeMillis()
                )
            }

            override fun onRenderProcessGone(didCrash: Boolean) {
                AppLogger.w("WebViewActivity", "Render process gone (crash=$didCrash), triggering WebView recreation")
                webViewRef = null
                errorMessage = null

                webViewRecreationKey++



                val app = webApp
                when (app?.appType) {
                    com.webtoapp.data.model.AppType.PHP_APP -> phpAppRetryTrigger++
                    com.webtoapp.data.model.AppType.NODEJS_APP -> nodeJsAppRetryTrigger++
                    com.webtoapp.data.model.AppType.PYTHON_APP -> pythonAppRetryTrigger++
                    com.webtoapp.data.model.AppType.GO_APP -> goAppRetryTrigger++
                    com.webtoapp.data.model.AppType.WORDPRESS -> wpRetryTrigger++
                    else -> {  }
                }
            }
        }
    }

    val webViewManager = remember { WebViewManager(context, adBlocker) }


    val localHttpServer = remember { LocalHttpServer.getInstance(context) }


    val targetUrl = remember(directUrl, webApp, testUrl) {
        val app = webApp
        when {

            !testUrl.isNullOrBlank() -> normalizeWebUrlForSecurity(testUrl)
            !directUrl.isNullOrBlank() -> normalizeWebUrlForSecurity(directUrl)
            app?.appType == com.webtoapp.data.model.AppType.WORDPRESS -> {

                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.PHP_APP -> {

                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.PYTHON_APP -> {

                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.NODEJS_APP -> {

                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.GO_APP -> {

                "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.MULTI_WEB -> {

                val firstSite = app.multiWebConfig?.sites?.firstOrNull { it.enabled && (it.url.isNotBlank() || it.localFilePath.isNotBlank()) }
                firstSite?.getEffectiveUrl() ?: "about:blank"
            }
            app?.appType == com.webtoapp.data.model.AppType.HTML ||
            app?.appType == com.webtoapp.data.model.AppType.FRONTEND -> {

                val projectId = app.htmlConfig?.projectId ?: ""
                val entryFile = app.htmlConfig?.getValidEntryFile() ?: "index.html"
                val htmlDir = File(context.filesDir, "html_projects/$projectId")


                AppLogger.d("WebViewActivity", "========== HTML App Debug Info ==========")
                AppLogger.d("WebViewActivity", "projectId: '$projectId'")
                AppLogger.d("WebViewActivity", "entryFile: '$entryFile'")
                AppLogger.d("WebViewActivity", "htmlDir: ${htmlDir.absolutePath}")
                AppLogger.d("WebViewActivity", "htmlDir.exists(): ${htmlDir.exists()}")
                AppLogger.d("WebViewActivity", "htmlConfig: ${app.htmlConfig}")
                AppLogger.d("WebViewActivity", "htmlConfig.files: ${app.htmlConfig?.files}")


                if (htmlDir.exists()) {
                    val files = htmlDir.listFiles()
                    AppLogger.d("WebViewActivity", "目录文件列表 (${files?.size ?: 0} 个):")
                    files?.forEach { file ->
                        AppLogger.d("WebViewActivity", "  - ${file.name} (${file.length()} bytes)")
                    }


                    val entryFilePath = File(htmlDir, entryFile)
                    AppLogger.d("WebViewActivity", "入口文件路径: ${entryFilePath.absolutePath}")
                    AppLogger.d("WebViewActivity", "入口文件存在: ${entryFilePath.exists()}")
                }
                AppLogger.d("WebViewActivity", "=========================================")

                if (htmlDir.exists()) {
                    try {

                        val enableLocalIsolation = app.webViewConfig.enableCrossOriginIsolation ||
                            LocalHttpServer.shouldEnableCrossOriginIsolation(htmlDir)
                        val baseUrl = localHttpServer.start(
                            htmlDir,
                            enableCrossOriginIsolation = enableLocalIsolation
                        )
                        val targetUrl = "$baseUrl/$entryFile"
                        AppLogger.d("WebViewActivity", "目标 URL: $targetUrl, crossOriginIsolation=$enableLocalIsolation")
                        targetUrl
                    } catch (e: Exception) {
                        AppLogger.e("WebViewActivity", "启动本地服务器失败", e)

                        "file://${htmlDir.absolutePath}/$entryFile"
                    }
                } else {
                    AppLogger.w("WebViewActivity", "HTML项目目录不存在: ${htmlDir.absolutePath}")
                    ""
                }
            }
            else -> normalizeWebUrlForSecurity(app?.url)
        }
    }


    DisposableEffect(Unit) {
        onDispose {


        }
    }


    val hideToolbar = !isTestMode && webApp?.webViewConfig?.hideToolbar == true

    val hideBrowserToolbar = !isTestMode && webApp?.webViewConfig?.hideBrowserToolbar == true

    val showToolbarInPreview = !hideToolbar || webApp?.webViewConfig?.showToolbarInFullscreen == true

    val shouldShowTopBar = showToolbarInPreview && !hideBrowserToolbar

    LaunchedEffect(hideToolbar) {

        onFullscreenModeChanged(hideToolbar)
    }


    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(


        contentWindowInsets = if (hideToolbar && !showToolbarInPreview) WindowInsets(0) else if (hideToolbar && showToolbarInPreview) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        modifier = if (hideToolbar && !showToolbarInPreview) Modifier.fillMaxSize() else if (hideToolbar) Modifier.fillMaxSize() else Modifier,
        topBar = {
            if (shouldShowTopBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isTestMode) Strings.moduleTestMode else pageTitle.ifEmpty { webApp?.name ?: "WebApp" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isTestMode && !testModuleIds.isNullOrEmpty()) {
                                Text(
                                    text = Strings.testingModules.format(testModuleIds.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            } else if (currentUrl.isNotEmpty()) {
                                Text(
                                    text = currentUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (canGoBack) {
                            IconButton(onClick = {
                                (context as? AppCompatActivity)?.let { activity ->
                                    ShellWebViewNavigation.goBackOrFinish(activity, webViewRef)
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        } else {
                            IconButton(onClick = { (context as? AppCompatActivity)?.finish() }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                    },
                    actions = {

                        IconButton(
                            onClick = { showConsole = !showConsole },
                            modifier = Modifier.size(48.dp).offset(x = (-4).dp)
                        ) {
                            Box(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                                BadgedBox(
                                    badge = {
                                        val errorCount = consoleMessages.count { it.level == ConsoleLevel.ERROR }
                                        if (errorCount > 0) {
                                            Badge { Text("$errorCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (showConsole) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                                        Strings.console
                                    )
                                }
                            }
                        }

                        Box {
                            var showToolbarMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showToolbarMenu = true }) {
                                Icon(Icons.Default.MoreVert, "更多")
                            }
                            DropdownMenu(
                                expanded = showToolbarMenu,
                                onDismissRequest = { showToolbarMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(Strings.goBack) },
                                    onClick = {
                                        showToolbarMenu = false
                                        (context as? AppCompatActivity)?.let { activity ->
                                            ShellWebViewNavigation.goBackOrFinish(activity, webViewRef)
                                        }
                                    },
                                    enabled = canGoBack,
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(Strings.goForward) },
                                    onClick = {
                                        showToolbarMenu = false
                                        webViewRef?.goForward()
                                    },
                                    enabled = canGoForward,
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(Strings.refresh) },
                                    onClick = {
                                        showToolbarMenu = false
                                        webViewRef?.reload()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, null)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { padding ->


        val density = LocalDensity.current

        val topInsetPx = WindowInsets.statusBars.getTop(density)
        val systemStatusBarHeightDp = if (topInsetPx > 0) {
            with(density) { topInsetPx.toDp() }
        } else {
            24.dp
        }


        val actualStatusBarPadding = if (statusBarHeightDp > 0) statusBarHeightDp.dp else systemStatusBarHeightDp

        val contentModifier = when {
            hideToolbar && showToolbarInPreview -> {

                Modifier.fillMaxSize().padding(padding)
            }
            hideToolbar && webApp?.webViewConfig?.showStatusBarInFullscreen == true -> {


                Modifier.fillMaxSize().padding(top = actualStatusBarPadding)
            }
            hideToolbar -> {

                Modifier.fillMaxSize()
            }
            else -> {

                Modifier.fillMaxSize().padding(padding)
            }
        }

        Box(modifier = contentModifier) {

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }


            if (!isActivationChecked && webApp?.activationEnabled == true) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else if (!isActivated && webApp?.activationEnabled == true) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(Strings.pleaseActivateApp)
                        Spacer(modifier = Modifier.height(16.dp))
                        PremiumButton(onClick = { showActivationDialog = true }) {
                            Text(Strings.enterActivationCode)
                        }
                    }
                }
            } else if (webApp?.appType == com.webtoapp.data.model.AppType.MULTI_WEB && isActivationChecked) {

                val mwApp = webApp
                val multiWebConfig = mwApp?.multiWebConfig
                if (mwApp != null && multiWebConfig != null && multiWebConfig.sites.isNotEmpty()) {
                    val shellConfig = com.webtoapp.core.shell.ShellConfig(
                        appName = mwApp.name,
                        appType = "MULTI_WEB",
                        multiWebConfig = com.webtoapp.core.shell.MultiWebShellConfig(
                            sites = multiWebConfig.sites.map { site ->
                                com.webtoapp.core.shell.MultiWebSiteShellConfig(
                                    id = site.id,
                                    name = site.name,
                                    url = site.url,
                                    type = site.type,
                                    localFilePath = site.localFilePath,
                                    iconEmoji = site.iconEmoji,
                                    category = site.category,
                                    cssSelector = site.cssSelector,
                                    linkSelector = site.linkSelector,
                                    enabled = site.enabled
                                )
                            },
                            displayMode = multiWebConfig.displayMode,
                            refreshInterval = multiWebConfig.refreshInterval,
                            showSiteIcons = multiWebConfig.showSiteIcons,
                            landscapeMode = multiWebConfig.landscapeMode,
                            projectId = multiWebConfig.projectId
                        ),
                        extensionModuleIds = mwApp.extensionModuleIds,
                        extensionFabIcon = mwApp.extensionFabIcon.orEmpty(),
                        browserDisguiseConfig = mwApp.browserDisguiseConfig,
                        deviceDisguiseConfig = mwApp.deviceDisguiseConfig
                    )
                    com.webtoapp.ui.shell.MultiWebShellMode(
                        config = shellConfig,
                        webViewConfig = mwApp.webViewConfig,
                        webViewCallbacks = webViewCallbacks,
                        webViewManager = webViewManager,
                        onWebViewCreated = { wv ->
                            webViewRef = wv
                            onWebViewCreated(wv)
                        },
                        swipeRefreshEnabled = mwApp.webViewConfig.swipeRefreshEnabled,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            webViewRef?.reload()
                        }
                    )
                } else {

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Language, null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                Strings.multiWebNoSites,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (targetUrl.isNotEmpty() && isActivationChecked) {


                key(webViewRecreationKey) {

                var isConsoleExpanded by remember { mutableStateOf(false) }
                val swipeRefreshEnabled = webApp?.webViewConfig?.swipeRefreshEnabled != false

                Column(modifier = Modifier.fillMaxSize()) {

                    AndroidView(
                        factory = { ctx ->
                            EdgeSwipeRefreshLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setColorSchemeColors(
                                    android.graphics.Color.parseColor("#6750A4"),
                                    android.graphics.Color.parseColor("#7F67BE")
                                )
                                isEnabled = swipeRefreshEnabled
                                setOnRefreshListener {
                                    isRefreshing = true
                                    webViewRef?.reload()
                                }

                                var swipeChildWebView: WebView? = null
                                setOnChildScrollUpCallback { _, _ ->
                                    val wv = swipeChildWebView ?: return@setOnChildScrollUpCallback false
                                    wv.scrollY > 0 || jsScrollTop.get() > 0
                                }

                                val createdWebView = WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )

                                    val moduleIds = if (isTestMode && !testModuleIds.isNullOrEmpty()) {
                                        testModuleIds
                                    } else {
                                        webApp?.extensionModuleIds ?: emptyList()
                                    }
                                    webViewManager.configureWebView(
                                        this,
                                        webApp?.webViewConfig ?: com.webtoapp.data.model.WebViewConfig(),
                                        webViewCallbacks,
                                        moduleIds,
                                        emptyList(),
                                        webApp?.extensionFabIcon.orEmpty(),
                                        allowGlobalModuleFallback = false,
                                        browserDisguiseConfig = webApp?.browserDisguiseConfig,
                                        deviceDisguiseConfig = webApp?.deviceDisguiseConfig
                                    )

                                    val currentApp = webApp
                                    if (currentApp?.appType == com.webtoapp.data.model.AppType.HTML) {
                                        settings.apply {
                                            allowFileAccess = true
                                            allowContentAccess = true
                                            @Suppress("DEPRECATION")
                                            allowFileAccessFromFileURLs = true
                                            @Suppress("DEPRECATION")
                                            allowUniversalAccessFromFileURLs = true
                                            javaScriptEnabled = currentApp.htmlConfig?.enableJavaScript ?: true
                                            domStorageEnabled = currentApp.htmlConfig?.enableLocalStorage ?: true
                                        }
                                    }



                                    var lastTouchX = 0f
                                    var lastTouchY = 0f
                                    setOnTouchListener { view, event ->
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN,
                                            MotionEvent.ACTION_MOVE -> {
                                                lastTouchX = event.x
                                                lastTouchY = event.y
                                            }
                                            MotionEvent.ACTION_UP -> view.performClick()
                                        }
                                        false
                                    }
                                    setOnLongClickListener {
                                        webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                                    }

                                    addJavascriptInterface(scrollBridge, "_wtaScrollBridge")
                                    onWebViewCreated(this)

                                    if (shouldSkipLongPressEnhancer(targetUrl)) {
                                        webViewManager.applyPreloadPolicyForUrl(this, targetUrl)
                                        AppLogger.d("WebViewActivity", "Strict host pre-load policy applied for $targetUrl")
                                    }

                                    webViewRef = this




                                    loadUrl(targetUrl)
                                }

                                swipeChildWebView = createdWebView
                                addView(createdWebView)
                            }
                        },
                        update = { swipeLayout ->
                            swipeLayout.isEnabled = swipeRefreshEnabled
                            if (swipeLayout.isRefreshing != isRefreshing) {
                                swipeLayout.isRefreshing = isRefreshing
                            }
                        },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )


                    AnimatedVisibility(
                        visible = showConsole,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        ConsolePanel(
                            consoleMessages = consoleMessages,
                            isExpanded = isConsoleExpanded,
                            onExpandToggle = { isConsoleExpanded = !isConsoleExpanded },
                            onClear = { consoleMessages = emptyList() },
                            onRunScript = { script ->
                                webViewRef?.evaluateJavascript(script) { result ->
                                    consoleMessages = consoleMessages + ConsoleLogEntry(
                                        level = ConsoleLevel.LOG,
                                        message = "=> $result",
                                        source = "eval",
                                        lineNumber = 0,
                                        timestamp = System.currentTimeMillis()
                                    )
                                }
                            },
                            onClose = { showConsole = false }
                        )
                    }
                }
                }
            } else if (webApp == null) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }


            val isWordPressLoading = webApp?.appType == com.webtoapp.data.model.AppType.WORDPRESS &&
                wordPressPreviewState !is WordPressPreviewState.Ready &&
                wordPressPreviewState !is WordPressPreviewState.Idle
            if (isWordPressLoading) {
                WordPressLoadingOverlay(
                    state = wordPressPreviewState,
                    downloadState = wpDownloadState,
                    onRetry = { wpRetryTrigger++ }
                )
            }


            val isPhpAppLoading = webApp?.appType == com.webtoapp.data.model.AppType.PHP_APP &&
                phpAppPreviewState !is PhpAppPreviewState.Ready &&
                phpAppPreviewState !is PhpAppPreviewState.Idle
            if (isPhpAppLoading) {
                PhpAppLoadingOverlay(
                    state = phpAppPreviewState,
                    downloadState = phpAppDownloadState,
                    onRetry = { phpAppRetryTrigger++ }
                )
            }


            val isPythonAppLoading = webApp?.appType == com.webtoapp.data.model.AppType.PYTHON_APP &&
                pythonAppPreviewState !is PythonAppPreviewState.Ready &&
                pythonAppPreviewState !is PythonAppPreviewState.Idle
            if (isPythonAppLoading) {
                PythonAppLoadingOverlay(
                    state = pythonAppPreviewState,
                    onRetry = { pythonAppRetryTrigger++ }
                )
            }


            val isGoAppLoading = webApp?.appType == com.webtoapp.data.model.AppType.GO_APP &&
                goAppPreviewState !is GoAppPreviewState.Ready &&
                goAppPreviewState !is GoAppPreviewState.Idle
            if (isGoAppLoading) {
                SimpleAppLoadingOverlay(
                    isStarting = goAppPreviewState is GoAppPreviewState.Starting || goAppPreviewState is GoAppPreviewState.StartingServer,
                    startingText = Strings.goStartingPreview,
                    errorMessage = (goAppPreviewState as? GoAppPreviewState.Error)?.message,
                    onRetry = { goAppRetryTrigger++ }
                )
            }




            if (webApp?.webViewConfig?.showFloatingBackButton == true &&
                ((hideToolbar && !showToolbarInPreview) || hideBrowserToolbar) &&
                canGoBack
            ) {
                var fabAlpha by remember { mutableFloatStateOf(0.9f) }
                var fadeKey by remember { mutableIntStateOf(0) }

                LaunchedEffect(canGoBack, fadeKey) {
                    fabAlpha = 0.9f
                    delay(3000L)

                    val steps = 20
                    val stepDelay = 30L
                    for (i in 1..steps) {
                        fabAlpha = 0.9f - (0.65f * i / steps)
                        delay(stepDelay)
                    }
                }

                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = {
                        fadeKey++
                        (context as? AppCompatActivity)?.let { activity ->
                            ShellWebViewNavigation.goBackOrFinish(activity, webViewRef)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = actualStatusBarPadding + 8.dp)
                        .graphicsLayer { alpha = fabAlpha },
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.cdBack)
                }
            }


            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(error, modifier = Modifier.weight(weight = 1f, fill = true))
                        TextButton(onClick = { errorMessage = null }) {
                            Text(Strings.close)
                        }
                    }
                }
            }


        }
    }



    if (hideToolbar && webApp?.webViewConfig?.showStatusBarInFullscreen == true) {
        com.webtoapp.ui.components.StatusBarOverlay(
            show = true,
            backgroundType = statusBarBackgroundType,
            backgroundColor = statusBarBackgroundColor,
            backgroundImagePath = statusBarBackgroundImage,
            alpha = statusBarBackgroundAlpha,
            heightDp = statusBarHeightDp,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
    }


    if (showActivationDialog) {
        val activationStatus by androidx.compose.runtime.produceState<com.webtoapp.core.activation.ActivationStatus?>(initialValue = null) {
            value = try {
                activation.getActivationStatus(appId)
            } catch (e: Exception) {
                null
            }
        }

        com.webtoapp.ui.components.EnhancedActivationDialog(
            onDismiss = { showActivationDialog = false },
            onActivate = { code ->
                val allCodes = webApp?.activationCodeList ?: emptyList()
                return@EnhancedActivationDialog activation.verifyActivationCodeWithObjects(appId, code, allCodes)
            },
            activationStatus = activationStatus,
            customTitle = webApp?.activationDialogConfig?.title ?: "",
            customSubtitle = webApp?.activationDialogConfig?.subtitle ?: "",
            customInputLabel = webApp?.activationDialogConfig?.inputLabel ?: "",
            customButtonText = webApp?.activationDialogConfig?.buttonText ?: ""
        )


        LaunchedEffect(Unit) {
            activation.isActivated(appId).collect { activated ->
                if (activated) {
                    isActivated = true
                    showActivationDialog = false

                    if (webApp?.announcementEnabled == true) {
                        val shouldShow = announcement.shouldShowAnnouncement(appId, webApp?.announcement)
                        showAnnouncementDialog = shouldShow
                    }
                }
            }
        }
    }


    if (showAnnouncementDialog && webApp?.announcement != null) {
        val ann = webApp!!.announcement!!
com.webtoapp.ui.components.announcement.AnnouncementDialog(
            config = com.webtoapp.ui.components.announcement.AnnouncementConfig(
                announcement = ann,
                template = ann.template.toUiTemplate(),
                showEmoji = ann.showEmoji,
                animationEnabled = ann.animationEnabled
            ),
            onDismiss = {
                showAnnouncementDialog = false
                val scope = (context as? AppCompatActivity)?.lifecycleScope
                scope?.launch {
                    announcement.markAnnouncementShown(appId, ann.version)
                }
            },
            onLinkClick = { url ->
                try {
                    val safeUrl = normalizeExternalUrlForIntent(url)
                    if (safeUrl.isBlank()) {
                        Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
                }
            },
            onNeverShowChecked = { checked ->
                if (checked) {
                    val scope = (context as? AppCompatActivity)?.lifecycleScope
                    scope?.launch {
                        announcement.markNeverShow(appId)
                    }
                }
            }
        )
    }


    val closeSplash = {
        showSplash = false

        if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity.requestedOrientation = originalOrientation
            originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }


    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        webApp?.splashConfig?.let { splashConfig ->
            SplashOverlay(
                splashConfig = splashConfig,
                countdown = splashCountdown,

                onSkip = if (splashConfig.clickToSkip) { closeSplash } else null,

                onComplete = closeSplash
            )
        }
    }


    if (showLongPressMenu && longPressResult != null) {
        WebViewLongPressMenu(
            menuStyle = webApp?.webViewConfig?.longPressMenuStyle ?: LongPressMenuStyle.FULL,
            result = longPressResult!!,
            touchX = longPressTouchX,
            touchY = longPressTouchY,
            longPressHandler = longPressHandler,
            onDismiss = {
                showLongPressMenu = false
                longPressResult = null
            }
        )
    }
}
