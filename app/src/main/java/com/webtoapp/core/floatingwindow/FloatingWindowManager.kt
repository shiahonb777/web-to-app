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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.FloatingBorderStyle
import com.webtoapp.data.model.FloatingWindowConfig

/**
 * 悬浮小窗管理器 V2
 * 管理悬浮窗的创建、拖拽、大小调整和透明度控制
 *
 * V2 新功能：
 * - 独立宽高控制（widthPercent / heightPercent）
 * - 边缘吸附（拖拽到屏幕边缘自动贴合）
 * - 右下角缩放手柄（手势拖拽调整窗口大小）
 * - 自定义圆角半径与边框样式
 * - 自动隐藏标题栏（3秒无操作后隐藏，触摸时恢复）
 * - 锁定位置模式
 * - 弹性动画（最小化/恢复时有 overshoot 弹跳效果）
 */
class FloatingWindowManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingWindowManager"
        private const val PREFS_NAME = "floating_window_prefs"
        private const val KEY_POS_X = "position_x"
        private const val KEY_POS_Y = "position_y"

        // 标题栏高度 dp
        private const val TITLE_BAR_HEIGHT_DP = 40
        // 最小化按钮大小 dp
        private const val MINI_BUTTON_SIZE_DP = 56
        // 缩放手柄大小 dp
        private const val RESIZE_HANDLE_SIZE_DP = 28
        // 边缘吸附阈值 px
        private const val EDGE_SNAP_THRESHOLD_PX = 60
        // 自动隐藏标题栏延迟 ms
        private const val AUTO_HIDE_TITLE_DELAY_MS = 3000L
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    // 悬浮窗视图
    private var floatingView: View? = null
    // 最小化按钮
    private var miniButton: View? = null
    // 悬浮窗中的 WebView
    private var webView: WebView? = null
    // 标题栏引用
    private var titleBarView: View? = null
    // 当前配置
    private var config: FloatingWindowConfig = FloatingWindowConfig()
    // 窗口参数
    private var windowParams: WindowManager.LayoutParams? = null
    // 最小化按钮参数
    private var miniParams: WindowManager.LayoutParams? = null
    // 状态
    private var isMinimized: Boolean = false
    private var isShowing: Boolean = false
    private var isTitleBarHidden: Boolean = false

    // 自动隐藏标题栏 Runnable
    private val autoHideTitleRunnable = Runnable {
        if (config.autoHideTitleBar && config.showTitleBar && !isTitleBarHidden) {
            titleBarView?.animate()?.alpha(0f)?.translationY(-titleBarView!!.height.toFloat())
                ?.setDuration(250)?.withEndAction {
                    isTitleBarHidden = true
                }?.start()
        }
    }

    // 回调
    var onWebViewCreated: ((WebView) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    /**
     * 创建并显示悬浮窗
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show(config: FloatingWindowConfig, appName: String = "", url: String = "") {
        if (isShowing) return

        this.config = config

        // 计算窗口尺寸（使用独立宽高，向后兼容 windowSizePercent）
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val effectiveWidth = if (config.widthPercent != config.windowSizePercent) config.widthPercent else config.windowSizePercent
        val effectiveHeight = if (config.heightPercent != config.windowSizePercent) config.heightPercent else config.windowSizePercent
        val width = (screenWidth * effectiveWidth / 100)
        val height = (screenHeight * effectiveHeight / 100)

        // 创建窗口参数
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
            // 恢复上次位置或居中
            if (config.rememberPosition) {
                x = prefs.getInt(KEY_POS_X, (screenWidth - width) / 2)
                y = prefs.getInt(KEY_POS_Y, (screenHeight - height) / 2)
            } else {
                x = (screenWidth - width) / 2
                y = (screenHeight - height) / 2
            }
            alpha = config.opacity / 100f
        }

        // 创建悬浮窗布局
        floatingView = createFloatingLayout(appName)

        // 添加到 WindowManager
        try {
            windowManager.addView(floatingView, windowParams)
            isShowing = true
            isMinimized = false

            // 加载 URL
            if (url.isNotBlank()) {
                webView?.loadUrl(url)
            }

            // 如果配置了启动时最小化
            if (config.startMinimized) {
                minimize()
            }

            // 启动自动隐藏标题栏计时
            scheduleAutoHideTitleBar()

            AppLogger.i(TAG, "悬浮窗已显示: width=${effectiveWidth}%, height=${effectiveHeight}%, opacity=${config.opacity}%")
        } catch (e: Exception) {
            AppLogger.e(TAG, "显示悬浮窗失败", e)
        }
    }

    /**
     * 获取内部 WebView 实例
     */
    fun getWebView(): WebView? = webView

    /**
     * 创建悬浮窗布局
     */
    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun createFloatingLayout(appName: String): View {
        val density = context.resources.displayMetrics.density
        val titleBarHeight = (TITLE_BAR_HEIGHT_DP * density).toInt()
        val cornerRadius = (config.cornerRadius * density)

        // 根容器
        val rootLayout = FrameLayout(context).apply {
            background = createStyledBackground(cornerRadius)
            clipToOutline = true
            elevation = 8 * density
        }

        // 内容容器（垂直排列标题栏 + WebView）
        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 标题栏（用于拖拽）
        if (config.showTitleBar) {
            val titleBar = createTitleBar(appName, titleBarHeight, density)
            titleBarView = titleBar
            contentLayout.addView(titleBar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                titleBarHeight
            ))
        }

        // WebView 容器
        val webViewContainer = FrameLayout(context)
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            // 触摸时恢复标题栏
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    restoreTitleBarIfHidden()
                    scheduleAutoHideTitleBar()
                }
                false
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

        // 缩放手柄（右下角）
        if (config.showResizeHandle) {
            val resizeHandle = createResizeHandle(density)
            val handleSize = (RESIZE_HANDLE_SIZE_DP * density).toInt()
            val handleLp = FrameLayout.LayoutParams(handleSize, handleSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, (4 * density).toInt(), (4 * density).toInt())
            }
            rootLayout.addView(resizeHandle, handleLp)
        }

        // 通知回调
        webView?.let { onWebViewCreated?.invoke(it) }

        return rootLayout
    }

    /**
     * 创建标题栏
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createTitleBar(appName: String, height: Int, density: Float): View {
        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1E1E2E.toInt()) // 深色标题栏
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), 0, (4 * density).toInt(), 0)
        }

        // 拖拽指示器
        val dragIndicator = TextView(context).apply {
            text = "⠿"
            setTextColor(0x99AAAACC.toInt())
            textSize = 16f
            setPadding(0, 0, (6 * density).toInt(), 0)
        }
        titleBar.addView(dragIndicator)

        // 标题文本
        val titleText = TextView(context).apply {
            text = appName.ifBlank { "WebToApp" }
            setTextColor(0xFFD0D0E0.toInt())
            textSize = 13f
            setSingleLine(true)
        }
        titleBar.addView(titleText, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ))

        // 最小化按钮
        val minimizeBtn = createTitleButton("─", 0xFFD0D0E0.toInt(), density) { minimize() }
        titleBar.addView(minimizeBtn)

        // 关闭按钮
        val closeBtn = createTitleButton("✕", 0xFFFF6B6B.toInt(), density) { dismiss() }
        titleBar.addView(closeBtn)

        // 拖拽逻辑（仅非锁定时）
        if (!config.lockPosition) {
            setupDragHandler(titleBar)
        }

        return titleBar
    }

    /**
     * 创建标题栏按钮
     */
    private fun createTitleButton(symbol: String, color: Int, density: Float, onClick: () -> Unit): View {
        return TextView(context).apply {
            text = symbol
            setTextColor(color)
            textSize = 14f
            gravity = Gravity.CENTER
            val size = (34 * density).toInt()
            minimumWidth = size
            minimumHeight = size
            setPadding((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            setOnClickListener { onClick() }
        }
    }

    /**
     * 创建缩放手柄
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createResizeHandle(density: Float): View {
        val handle = TextView(context).apply {
            text = "◢"
            textSize = 14f
            setTextColor(0x88AAAACC.toInt())
            gravity = Gravity.CENTER
            // 圆角背景
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
                    // 更新 config 中的百分比
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

    /**
     * 设置拖拽处理（含边缘吸附）
     */
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
                    // 恢复标题栏
                    restoreTitleBarIfHidden()
                    scheduleAutoHideTitleBar()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    // 超过阈值才开始拖拽（避免误触）
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
                        // 边缘吸附
                        if (config.edgeSnapping) {
                            performEdgeSnap()
                        }
                        // 保存位置
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

    /**
     * 边缘吸附逻辑
     */
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

        // 左边缘吸附
        if (params.x < EDGE_SNAP_THRESHOLD_PX) {
            snapX = 0
            didSnap = true
        }
        // 右边缘吸附
        if (params.x + winW > screenW - EDGE_SNAP_THRESHOLD_PX) {
            snapX = screenW - winW
            didSnap = true
        }
        // 顶部吸附
        if (params.y < EDGE_SNAP_THRESHOLD_PX) {
            snapY = 0
            didSnap = true
        }
        // 底部吸附
        if (params.y + winH > screenH - EDGE_SNAP_THRESHOLD_PX) {
            snapY = screenH - winH
            didSnap = true
        }

        if (didSnap) {
            // 动画吸附到边缘
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

    /**
     * 保存窗口位置
     */
    private fun savePosition() {
        prefs.edit()
            .putInt(KEY_POS_X, windowParams?.x ?: 0)
            .putInt(KEY_POS_Y, windowParams?.y ?: 0)
            .apply()
    }

    /**
     * 调度自动隐藏标题栏
     */
    private fun scheduleAutoHideTitleBar() {
        handler.removeCallbacks(autoHideTitleRunnable)
        if (config.autoHideTitleBar && config.showTitleBar) {
            handler.postDelayed(autoHideTitleRunnable, AUTO_HIDE_TITLE_DELAY_MS)
        }
    }

    /**
     * 恢复被隐藏的标题栏
     */
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

    /**
     * 最小化为悬浮按钮
     */
    @SuppressLint("ClickableViewAccessibility")
    fun minimize() {
        if (isMinimized || !isShowing) return

        // 隐藏悬浮窗（带缩放动画）
        floatingView?.animate()?.scaleX(0.1f)?.scaleY(0.1f)?.alpha(0f)
            ?.setDuration(200)?.withEndAction {
                floatingView?.visibility = View.GONE
                floatingView?.scaleX = 1f
                floatingView?.scaleY = 1f
                floatingView?.alpha = 1f
            }?.start()

        val density = context.resources.displayMetrics.density
        val buttonSize = (MINI_BUTTON_SIZE_DP * density).toInt()

        // 创建窗口参数
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

            // 弹出动画
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

    /**
     * 创建最小化按钮
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createMiniButton(size: Int, density: Float): View {
        val button = FrameLayout(context).apply {
            background = createCircleBackground()
            elevation = 12 * density
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

        // 点击恢复
        button.setOnClickListener { restore() }

        // 拖拽最小化按钮
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
                    false // 不消费，让 onClick 有机会触发
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
                        // 同步位置到窗口参数
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

    /**
     * 从最小化恢复
     */
    fun restore() {
        if (!isMinimized || !isShowing) return

        // 移除最小化按钮（带缩放动画）
        miniButton?.animate()?.scaleX(0f)?.scaleY(0f)?.alpha(0f)
            ?.setDuration(150)?.withEndAction {
                try {
                    miniButton?.let { windowManager.removeView(it) }
                } catch (_: Exception) {}
                miniButton = null
            }?.start()

        // 恢复悬浮窗位置到最小化按钮位置
        windowParams?.x = miniParams?.x ?: windowParams?.x ?: 0
        windowParams?.y = miniParams?.y ?: windowParams?.y ?: 0

        // 显示悬浮窗（带弹出动画）
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

    /**
     * 更新窗口大小
     */
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

    /**
     * 更新透明度
     */
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

    /**
     * 关闭悬浮窗
     */
    fun dismiss() {
        if (!isShowing) return

        // 取消自动隐藏定时器
        handler.removeCallbacks(autoHideTitleRunnable)

        try {
            // 保存位置
            if (config.rememberPosition) {
                savePosition()
            }

            // 清理 WebView
            webView?.let { wv ->
                wv.stopLoading()
                wv.onPause()
                wv.pauseTimers()
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.removeAllViews()
                wv.destroy()
            }
            webView = null
            titleBarView = null

            // 移除最小化按钮
            miniButton?.let { windowManager.removeView(it) }
            miniButton = null

            // 移除悬浮窗
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

    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean = isShowing

    /**
     * 是否已最小化
     */
    fun isMinimized(): Boolean = isMinimized

    // ==================== 辅助方法 ====================

    /**
     * 创建带样式的圆角矩形背景（根据 borderStyle 配置）
     */
    private fun createStyledBackground(cornerRadius: Float): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(0xFF1A1A2E.toInt()) // 深色背景
            this.cornerRadius = cornerRadius
            // 根据边框样式设置
            when (config.borderStyle) {
                FloatingBorderStyle.NONE -> { /* 不设置边框 */ }
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

    /**
     * 创建圆形背景
     */
    private fun createCircleBackground(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(0xFF2A2A4E.toInt())
            setStroke(2, 0xFF6666CC.toInt())
        }
    }
}
