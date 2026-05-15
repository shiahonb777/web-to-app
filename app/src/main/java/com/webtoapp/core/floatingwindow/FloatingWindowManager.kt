package com.webtoapp.core.floatingwindow

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.FloatingBorderStyle
import com.webtoapp.data.model.FloatingWindowConfig














class FloatingWindowManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingWindowManager"
        private const val PREFS_NAME = "floating_window_prefs"
        private const val KEY_POS_X = "position_x"
        private const val KEY_POS_Y = "position_y"


        private const val TITLE_BAR_HEIGHT_DP = 40

        private const val MINI_BUTTON_SIZE_DP = 56

        private const val RESIZE_HANDLE_SIZE_DP = 28

        private const val EDGE_SNAP_THRESHOLD_PX = 60

        private const val AUTO_HIDE_TITLE_DELAY_MS = 3000L
    }

    private data class WindowBounds(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())


    private var floatingView: View? = null

    private var miniButton: View? = null

    private var webView: WebView? = null

    private var titleBarView: View? = null
    private var backButtonView: TextView? = null
    private var forwardButtonView: TextView? = null
    private var fullscreenButtonView: TextView? = null
    private var savedWindowBounds: WindowBounds? = null
    private var isFullscreen: Boolean = false

    private var config: FloatingWindowConfig = FloatingWindowConfig()

    private var windowParams: WindowManager.LayoutParams? = null

    private var miniParams: WindowManager.LayoutParams? = null

    private var isMinimized: Boolean = false
    private var isShowing: Boolean = false
    private var isTitleBarHidden: Boolean = false


    private val autoHideTitleRunnable = Runnable {
        if (config.autoHideTitleBar && config.showTitleBar && !isTitleBarHidden) {
            titleBarView?.animate()?.alpha(0f)?.translationY(-titleBarView!!.height.toFloat())
                ?.setDuration(250)?.withEndAction {
                    isTitleBarHidden = true
                }?.start()
        }
    }


    var onWebViewCreated: ((WebView) -> Unit)? = null
    var onWebViewPageFinished: ((WebView, String?) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null




    @SuppressLint("ClickableViewAccessibility")
    fun show(config: FloatingWindowConfig, appName: String = "", url: String = "") {
        if (isShowing) return

        this.config = config
        isFullscreen = false
        savedWindowBounds = null


        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val effectiveWidth = if (config.widthPercent != config.windowSizePercent) config.widthPercent else config.windowSizePercent
        val effectiveHeight = if (config.heightPercent != config.windowSizePercent) config.heightPercent else config.windowSizePercent
        val width = (screenWidth * effectiveWidth / 100)
        val height = (screenHeight * effectiveHeight / 100)


        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            width,
            height,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START

            if (config.rememberPosition) {
                x = prefs.getInt(KEY_POS_X, (screenWidth - width) / 2)
                y = prefs.getInt(KEY_POS_Y, (screenHeight - height) / 2)
            } else {
                x = (screenWidth - width) / 2
                y = (screenHeight - height) / 2
            }
            alpha = config.opacity / 100f
        }


        floatingView = createFloatingLayout(appName)


        try {
            windowManager.addView(floatingView, windowParams)
            isShowing = true
            isMinimized = false


            if (url.isNotBlank()) {
                webView?.loadUrl(url)
            }


            if (config.startMinimized) {
                minimize()
            }


            scheduleAutoHideTitleBar()

            AppLogger.i(TAG, "悬浮窗已显示: width=${effectiveWidth}%, height=${effectiveHeight}%, opacity=${config.opacity}%")
        } catch (e: Exception) {
            AppLogger.e(TAG, "显示悬浮窗失败", e)
        }
    }




    fun getWebView(): WebView? = webView




    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun createFloatingLayout(appName: String): View {
        val density = context.resources.displayMetrics.density
        val titleBarHeight = (TITLE_BAR_HEIGHT_DP * density).toInt()
        val cornerRadius = (config.cornerRadius * density)


        val rootLayout = FrameLayout(context).apply {
            background = createStyledBackground(cornerRadius)
            clipToOutline = true
            elevation = 8 * density
        }


        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }


        if (config.showTitleBar) {
            val titleBar = createTitleBar(appName, titleBarHeight, density)
            titleBarView = titleBar
            contentLayout.addView(titleBar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                titleBarHeight
            ))
        }


        val webViewContainer = FrameLayout(context)
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    updateNavigationButtons()
                    view?.let { onWebViewPageFinished?.invoke(it, url) }
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    updateNavigationButtons()
                }
            }

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    restoreTitleBarIfHidden()
                    scheduleAutoHideTitleBar()
                }
                false
            }
            post {
                updateNavigationButtons()
                updateFullscreenButton()
            }
        }
        webViewContainer.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        contentLayout.addView(webViewContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        rootLayout.addView(contentLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))


        if (config.showResizeHandle) {
            val resizeHandle = createResizeHandle(density)
            val handleSize = (RESIZE_HANDLE_SIZE_DP * density).toInt()
            val handleLp = FrameLayout.LayoutParams(handleSize, handleSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, (4 * density).toInt(), (4 * density).toInt())
            }
            rootLayout.addView(resizeHandle, handleLp)
        }


        webView?.let { onWebViewCreated?.invoke(it) }

        return rootLayout
    }




    @SuppressLint("ClickableViewAccessibility")
    private fun createTitleBar(appName: String, height: Int, density: Float): View {
        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1E1E2E.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), 0, (4 * density).toInt(), 0)
        }


        val trafficLightContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, (10 * density).toInt(), 0)
        }


        val closeBtn = createTrafficLightButton("✕", 0xFFFF5F57.toInt(), 0x99000000.toInt(), density, Strings.close) {
            dismiss()
        }
        trafficLightContainer.addView(closeBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, (8 * density).toInt(), 0) })


        val minimizeBtn = createTrafficLightButton("─", 0xFFFFBD2E.toInt(), 0x99000000.toInt(), density, Strings.floatingWindowMinimize) {
            minimize()
        }
        trafficLightContainer.addView(minimizeBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, (8 * density).toInt(), 0) })


        fullscreenButtonView = createTrafficLightButton("↗", 0xFF28C840.toInt(), 0x99000000.toInt(), density, Strings.floatingWindowEnterFullscreen) {
            toggleFullscreen()
        }
        trafficLightContainer.addView(fullscreenButtonView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        titleBar.addView(trafficLightContainer)


        val dragIndicator = TextView(context).apply {
            text = "⠿"
            setTextColor(0x99AAAACC.toInt())
            textSize = 16f
            setPadding(0, 0, (6 * density).toInt(), 0)
        }
        titleBar.addView(dragIndicator)


        val titleText = TextView(context).apply {
            text = appName.ifBlank { "WebToApp" }
            setTextColor(0xFFD0D0E0.toInt())
            textSize = 13f
            setSingleLine(true)
        }
        titleBar.addView(titleText, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ))

        backButtonView = createTitleButton("<", 0xFFD0D0E0.toInt(), density, Strings.goBack) {
            navigateBack()
        }
        titleBar.addView(backButtonView)

        forwardButtonView = createTitleButton(">", 0xFFD0D0E0.toInt(), density, Strings.goForward) {
            navigateForward()
        }
        titleBar.addView(forwardButtonView)

        updateNavigationButtons()
        updateFullscreenButton()


        if (!config.lockPosition) {
            setupDragHandler(titleBar)
        }

        return titleBar
    }




    private fun createTitleButton(
        symbol: String,
        color: Int,
        density: Float,
        contentDescriptionText: String,
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = symbol
            contentDescription = contentDescriptionText
            setTextColor(color)
            textSize = 13f
            gravity = Gravity.CENTER
            val size = (32 * density).toInt()
            minimumWidth = size
            minimumHeight = size
            setPadding((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            setOnClickListener { onClick() }
        }
    }





    @SuppressLint("ClickableViewAccessibility")
    private fun createTrafficLightButton(
        symbol: String,
        bgColor: Int,
        symbolColor: Int,
        density: Float,
        contentDescriptionText: String,
        onClick: () -> Unit
    ): TextView {
        val dotSize = (14 * density).toInt()
        val padding = (2 * density).toInt()
        return TextView(context).apply {
            text = symbol
            contentDescription = contentDescriptionText
            setTextColor(symbolColor)
            textSize = 8f
            gravity = Gravity.CENTER
            minWidth = dotSize
            minHeight = dotSize
            minimumWidth = dotSize
            minimumHeight = dotSize
            setPadding(padding, padding, padding, padding)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(bgColor)
            }
            setOnClickListener { onClick() }
        }
    }




    @SuppressLint("ClickableViewAccessibility")
    private fun createResizeHandle(density: Float): View {
        val handle = TextView(context).apply {
            text = "◢"
            textSize = 14f
            setTextColor(0x88AAAACC.toInt())
            gravity = Gravity.CENTER

            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(0x44333355.toInt())
                cornerRadius = 6 * density
            }
        }

        var initialWidth = 0
        var initialHeight = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialWidth = windowParams?.width ?: 0
                    initialHeight = windowParams?.height ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    val dm = context.resources.displayMetrics
                    val minW = (dm.widthPixels * 0.25f).toInt()
                    val minH = (dm.heightPixels * 0.2f).toInt()
                    val newW = (initialWidth + dx).coerceIn(minW, dm.widthPixels)
                    val newH = (initialHeight + dy).coerceIn(minH, dm.heightPixels)
                    windowParams?.width = newW
                    windowParams?.height = newH
                    try {
                        windowManager.updateViewLayout(floatingView, windowParams)
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "缩放窗口失败", e)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {

                    val dm = context.resources.displayMetrics
                    val wPercent = ((windowParams?.width ?: dm.widthPixels) * 100 / dm.widthPixels).coerceIn(30, 100)
                    val hPercent = ((windowParams?.height ?: dm.heightPixels) * 100 / dm.heightPixels).coerceIn(30, 100)
                    config = config.copy(widthPercent = wPercent, heightPercent = hPercent)
                    true
                }
                else -> false
            }
        }

        return handle
    }




    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragHandler(dragView: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        dragView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams?.x ?: 0
                    initialY = windowParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false

                    restoreTitleBarIfHidden()
                    scheduleAutoHideTitleBar()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY


                    if (!isDragging && (dx * dx + dy * dy > 100)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        windowParams?.x = initialX + dx.toInt()
                        windowParams?.y = initialY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(floatingView, windowParams)
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "更新窗口位置失败", e)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {

                        if (config.edgeSnapping) {
                            performEdgeSnap()
                        }

                        if (config.rememberPosition) {
                            savePosition()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }




    private fun performEdgeSnap() {
        val params = windowParams ?: return
        val dm = context.resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels
        val winW = params.width
        val winH = params.height

        var snapX = params.x
        var snapY = params.y
        var didSnap = false


        if (params.x < EDGE_SNAP_THRESHOLD_PX) {
            snapX = 0
            didSnap = true
        }

        if (params.x + winW > screenW - EDGE_SNAP_THRESHOLD_PX) {
            snapX = screenW - winW
            didSnap = true
        }

        if (params.y < EDGE_SNAP_THRESHOLD_PX) {
            snapY = 0
            didSnap = true
        }

        if (params.y + winH > screenH - EDGE_SNAP_THRESHOLD_PX) {
            snapY = screenH - winH
            didSnap = true
        }

        if (didSnap) {

            val startX = params.x
            val startY = params.y
            val finalX = snapX
            val finalY = snapY

            val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 200
                interpolator = OvershootInterpolator(0.8f)
                addUpdateListener { anim ->
                    val fraction = anim.animatedValue as Float
                    params.x = (startX + (finalX - startX) * fraction).toInt()
                    params.y = (startY + (finalY - startY) * fraction).toInt()
                    try {
                        windowManager.updateViewLayout(floatingView, params)
                    } catch (_: Exception) {}
                }
            }
            animator.start()
        }
    }




    private fun savePosition() {
        prefs.edit()
            .putInt(KEY_POS_X, windowParams?.x ?: 0)
            .putInt(KEY_POS_Y, windowParams?.y ?: 0)
            .apply()
    }




    private fun scheduleAutoHideTitleBar() {
        handler.removeCallbacks(autoHideTitleRunnable)
        if (config.autoHideTitleBar && config.showTitleBar) {
            handler.postDelayed(autoHideTitleRunnable, AUTO_HIDE_TITLE_DELAY_MS)
        }
    }




    private fun restoreTitleBarIfHidden() {
        if (isTitleBarHidden && titleBarView != null) {
            titleBarView!!.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .withEndAction {
                    isTitleBarHidden = false
                }
                .start()
        }
    }

    private fun navigateBack() {
        val currentWebView = webView ?: return
        if (!currentWebView.canGoBack()) return
        currentWebView.goBack()
        currentWebView.postDelayed({ updateNavigationButtons() }, 120)
    }

    private fun navigateForward() {
        val currentWebView = webView ?: return
        if (!currentWebView.canGoForward()) return
        currentWebView.goForward()
        currentWebView.postDelayed({ updateNavigationButtons() }, 120)
    }

    private fun updateNavigationButtons() {
        val currentWebView = webView
        val canGoBack = currentWebView?.canGoBack() == true
        val canGoForward = currentWebView?.canGoForward() == true

        backButtonView?.apply {
            isEnabled = canGoBack
            alpha = if (canGoBack) 1f else 0.35f
        }
        forwardButtonView?.apply {
            isEnabled = canGoForward
            alpha = if (canGoForward) 1f else 0.35f
        }
    }

    private fun updateFullscreenButton() {
        fullscreenButtonView?.apply {
            text = if (isFullscreen) "↙" else "↗"
            contentDescription = if (isFullscreen) {
                Strings.floatingWindowExitFullscreen
            } else {
                Strings.floatingWindowEnterFullscreen
            }
        }
    }

    private fun toggleFullscreen() {
        val params = windowParams ?: return
        val density = context.resources.displayMetrics
        if (!isFullscreen) {
            savedWindowBounds = WindowBounds(
                x = params.x,
                y = params.y,
                width = params.width,
                height = params.height
            )
            params.x = 0
            params.y = 0
            params.width = density.widthPixels
            params.height = density.heightPixels
            isFullscreen = true
        } else {
            savedWindowBounds?.let { bounds ->
                params.x = bounds.x
                params.y = bounds.y
                params.width = bounds.width
                params.height = bounds.height
            }
            savedWindowBounds = null
            isFullscreen = false
        }

        try {
            windowManager.updateViewLayout(floatingView, params)
            updateFullscreenButton()
        } catch (e: Exception) {
            AppLogger.w(TAG, "切换悬浮窗全屏失败", e)
        }
    }




    @SuppressLint("ClickableViewAccessibility")
    fun minimize() {
        if (isMinimized || !isShowing) return


        floatingView?.animate()?.scaleX(0.1f)?.scaleY(0.1f)?.alpha(0f)
            ?.setDuration(200)?.withEndAction {
                floatingView?.visibility = View.GONE
                floatingView?.scaleX = 1f
                floatingView?.scaleY = 1f
                floatingView?.alpha = 1f
            }?.start()

        val density = context.resources.displayMetrics.density
        val buttonSize = (MINI_BUTTON_SIZE_DP * density).toInt()


        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        miniParams = WindowManager.LayoutParams(
            buttonSize,
            buttonSize,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowParams?.x ?: 0
            y = windowParams?.y ?: 0
            alpha = 0.9f
        }

        miniButton = createMiniButton(buttonSize, density)

        try {
            windowManager.addView(miniButton, miniParams)
            isMinimized = true


            miniButton?.scaleX = 0f
            miniButton?.scaleY = 0f
            miniButton?.animate()?.scaleX(1f)?.scaleY(1f)
                ?.setDuration(300)?.setInterpolator(OvershootInterpolator(1.2f))
                ?.start()

            AppLogger.d(TAG, "悬浮窗已最小化")
        } catch (e: Exception) {
            AppLogger.e(TAG, "最小化失败", e)
        }
    }




    @SuppressLint("ClickableViewAccessibility")
    private fun createMiniButton(size: Int, density: Float): View {
        val button = FrameLayout(context).apply {
            background = createCircleBackground()
            elevation = 12 * density
            contentDescription = Strings.floatingWindowRestoreWindow
        }

        val icon = TextView(context).apply {
            text = "🌐"
            textSize = 24f
            gravity = Gravity.CENTER
        }
        button.addView(icon, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))


        button.setOnClickListener { restore() }


        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = miniParams?.x ?: 0
                    initialY = miniParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (dx * dx + dy * dy > 100)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        miniParams?.x = initialX + dx.toInt()
                        miniParams?.y = initialY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(miniButton, miniParams)
                        } catch (_: Exception) {}
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {

                        windowParams?.x = miniParams?.x ?: 0
                        windowParams?.y = miniParams?.y ?: 0
                        if (config.rememberPosition) {
                            savePosition()
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        return button
    }




    fun restore() {
        if (!isMinimized || !isShowing) return


        miniButton?.animate()?.scaleX(0f)?.scaleY(0f)?.alpha(0f)
            ?.setDuration(150)?.withEndAction {
                try {
                    miniButton?.let { windowManager.removeView(it) }
                } catch (_: Exception) {}
                miniButton = null
            }?.start()


        windowParams?.x = miniParams?.x ?: windowParams?.x ?: 0
        windowParams?.y = miniParams?.y ?: windowParams?.y ?: 0


        floatingView?.visibility = View.VISIBLE
        floatingView?.scaleX = 0.1f
        floatingView?.scaleY = 0.1f
        floatingView?.alpha = 0f
        floatingView?.animate()?.scaleX(1f)?.scaleY(1f)?.alpha(config.opacity / 100f)
            ?.setDuration(300)?.setInterpolator(OvershootInterpolator(1.0f))
            ?.start()

        try {
            windowManager.updateViewLayout(floatingView, windowParams)
        } catch (e: Exception) {
            AppLogger.w(TAG, "恢复窗口位置失败", e)
        }

        isMinimized = false
        scheduleAutoHideTitleBar()
        AppLogger.d(TAG, "悬浮窗已恢复")
    }




    fun updateSize(percent: Int) {
        val clamped = percent.coerceIn(30, 100)
        config = config.copy(widthPercent = clamped, heightPercent = clamped, windowSizePercent = clamped)

        val displayMetrics = context.resources.displayMetrics
        windowParams?.width = (displayMetrics.widthPixels * clamped / 100)
        windowParams?.height = (displayMetrics.heightPixels * clamped / 100)

        try {
            if (!isMinimized) {
                windowManager.updateViewLayout(floatingView, windowParams)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "更新窗口大小失败", e)
        }
    }




    fun updateOpacity(opacity: Int) {
        val clamped = opacity.coerceIn(30, 100)
        config = config.copy(opacity = clamped)

        windowParams?.alpha = clamped / 100f

        try {
            if (!isMinimized) {
                windowManager.updateViewLayout(floatingView, windowParams)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "更新透明度失败", e)
        }
    }




    fun dismiss() {
        if (!isShowing) return


        handler.removeCallbacks(autoHideTitleRunnable)

        try {

            if (config.rememberPosition) {
                savePosition()
            }


            webView?.let { wv ->
                wv.stopLoading()
                wv.onPause()
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.removeAllViews()
                wv.destroy()
            }
            webView = null
            titleBarView = null
            backButtonView = null
            forwardButtonView = null
            fullscreenButtonView = null
            savedWindowBounds = null
            isFullscreen = false


            miniButton?.let { windowManager.removeView(it) }
            miniButton = null


            floatingView?.let { windowManager.removeView(it) }
            floatingView = null

            isShowing = false
            isMinimized = false
            isTitleBarHidden = false

            onDismiss?.invoke()
            AppLogger.i(TAG, "悬浮窗已关闭")
        } catch (e: Exception) {
            AppLogger.e(TAG, "关闭悬浮窗失败", e)
        }
    }




    fun isShowing(): Boolean = isShowing




    fun isMinimized(): Boolean = isMinimized






    private fun createStyledBackground(cornerRadius: Float): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(0xFF1A1A2E.toInt())
            this.cornerRadius = cornerRadius

            when (config.borderStyle) {
                FloatingBorderStyle.NONE -> {  }
                FloatingBorderStyle.SUBTLE -> {
                    setStroke(2, 0xFF333355.toInt())
                }
                FloatingBorderStyle.GLOW -> {
                    setStroke(3, 0xFF6666FF.toInt())
                }
                FloatingBorderStyle.ACCENT -> {
                    setStroke(3, 0xFF8B5CF6.toInt())
                }
            }
        }
    }




    private fun createCircleBackground(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(0xFF2A2A4E.toInt())
            setStroke(2, 0xFF6666CC.toInt())
        }
    }
}
