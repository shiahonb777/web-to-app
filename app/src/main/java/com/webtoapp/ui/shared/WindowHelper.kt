package com.webtoapp.ui.shared

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.KeyboardAdjustMode

object WindowHelper {

    fun applyStatusBarColor(
        activity: Activity,
        colorMode: String,
        customColor: String?,
        darkIcons: Boolean?,
        isDarkTheme: Boolean
    ) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)

        when (colorMode) {
            "TRANSPARENT" -> {
                activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                val useDarkIcons = darkIcons ?: !isDarkTheme
                controller.isAppearanceLightStatusBars = useDarkIcons
            }
            "CUSTOM" -> {
                val color = try {
                    android.graphics.Color.parseColor(customColor ?: "#FFFFFF")
                } catch (e: Exception) {
                    android.graphics.Color.WHITE
                }
                activity.window.statusBarColor = color
                val useDarkIcons = darkIcons ?: isColorLight(color)
                controller.isAppearanceLightStatusBars = useDarkIcons
            }
            else -> {
                if (isDarkTheme) {
                    activity.window.statusBarColor = android.graphics.Color.parseColor("#1C1B1F")
                    controller.isAppearanceLightStatusBars = false
                } else {
                    activity.window.statusBarColor = android.graphics.Color.parseColor("#FFFBFE")
                    controller.isAppearanceLightStatusBars = true
                }
            }
        }

        controller.isAppearanceLightNavigationBars = controller.isAppearanceLightStatusBars
    }

    fun isColorLight(color: Int): Boolean {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return luminance > 0.5
    }

    /**
     * @param keyboardAdjustMode RESIZE = push content up, NOTHING = overlay content.
     * If null, uses legacy behavior: NOTHING when enabled, RESIZE when disabled.
     * Android 11+ deprecated ADJUST_RESIZE; this method uses WindowInsets IME listener instead.
     */
    fun applyImmersiveFullscreen(
        activity: Activity,
        enabled: Boolean,
        hideNavBar: Boolean = true,
        isDarkTheme: Boolean = false,
        showStatusBar: Boolean = false,
        forceHideSystemUi: Boolean = false,
        statusBarColorMode: String = "THEME",
        statusBarCustomColor: String? = null,
        statusBarDarkIcons: Boolean? = null,
        statusBarBgType: String = "COLOR",
        keyboardAdjustMode: KeyboardAdjustMode? = null,
        tag: String = "WindowHelper"
    ) {
        try {
            // Support notch / punch-hole displays
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                activity.window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            applyKeyboardMode(activity, keyboardAdjustMode, tag)

            WindowInsetsControllerCompat(activity.window, activity.window.decorView).let { controller ->
                if (enabled) {
                    activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    val shouldShowStatusBar = if (forceHideSystemUi) false else showStatusBar

                    if (shouldShowStatusBar) {
                        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                        controller.show(WindowInsetsCompat.Type.statusBars())

                        if (statusBarBgType == "IMAGE") {
                            activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                            val useDarkIcons = statusBarDarkIcons ?: !isDarkTheme
                            controller.isAppearanceLightStatusBars = useDarkIcons
                        } else {
                            when (statusBarColorMode) {
                                "CUSTOM" -> {
                                    val color = try {
                                        android.graphics.Color.parseColor(statusBarCustomColor ?: "#000000")
                                    } catch (e: Exception) {
                                        android.graphics.Color.BLACK
                                    }
                                    activity.window.statusBarColor = color
                                    val useDarkIcons = statusBarDarkIcons ?: isColorLight(color)
                                    controller.isAppearanceLightStatusBars = useDarkIcons
                                }
                                "TRANSPARENT" -> {
                                    activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                                    val useDarkIcons = statusBarDarkIcons ?: !isDarkTheme
                                    controller.isAppearanceLightStatusBars = useDarkIcons
                                }
                                else -> {
                                    if (isDarkTheme) {
                                        activity.window.statusBarColor = android.graphics.Color.parseColor("#1C1B1F")
                                        controller.isAppearanceLightStatusBars = false
                                    } else {
                                        activity.window.statusBarColor = android.graphics.Color.parseColor("#FFFBFE")
                                        controller.isAppearanceLightStatusBars = true
                                    }
                                }
                            }
                        }
                    } else {
                        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                        activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                        controller.hide(WindowInsetsCompat.Type.statusBars())
                    }

                    if (hideNavBar || forceHideSystemUi) {
                        controller.hide(WindowInsetsCompat.Type.navigationBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        controller.show(WindowInsetsCompat.Type.navigationBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                    activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    applyStatusBarColor(activity, statusBarColorMode, statusBarCustomColor, statusBarDarkIcons, isDarkTheme)
                }
            }
        } catch (e: Exception) {
            AppLogger.w(tag, "applyImmersiveFullscreen failed", e)
        }
    }

    /**
     * 仅更新键盘行为，不触发全屏模式变更。
     * 可在配置加载后独立调用，无需重新走完整的 applyImmersiveFullscreen 流程。
     */
    fun applyKeyboardModeOnly(
        activity: Activity,
        keyboardAdjustMode: KeyboardAdjustMode?,
        tag: String = "WindowHelper"
    ) {
        try {
            applyKeyboardMode(activity, keyboardAdjustMode, tag)
        } catch (e: Exception) {
            AppLogger.w(tag, "applyKeyboardModeOnly failed", e)
        }
    }

    /**
     * 配置键盘弹出时的内容调整行为。
     * RESIZE: 监听 IME insets，给 contentView 设置 bottom padding 推内容上去。
     * NOTHING: 不监听 IME，键盘直接覆盖内容。
     * 使用 contentView 而非 decorView 设置 padding，避免影响系统 insets 处理。
     */
    private fun applyKeyboardMode(
        activity: Activity,
        keyboardAdjustMode: KeyboardAdjustMode?,
        tag: String
    ) {
        val decorView = activity.window.decorView
        val contentView = activity.findViewById<View>(android.R.id.content) ?: return

        val mode = keyboardAdjustMode ?: KeyboardAdjustMode.RESIZE
        
        when (mode) {
            KeyboardAdjustMode.RESIZE -> {
                // ADJUST_NOTHING + WindowInsets IME 监听，手动给 contentView 设置 bottom padding
                @Suppress("DEPRECATION")
                activity.window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                )
                
                ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, windowInsets ->
                    val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
                    val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
                    
                    if (imeVisible) {
                        // 键盘弹出：设置 bottom padding = IME 高度
                        // IME insets 已包含导航栏高度（IME bottom = 键盘高度 + 导航栏高度）
                        view.setPadding(
                            view.paddingLeft,
                            view.paddingTop,
                            view.paddingRight,
                            imeInsets.bottom
                        )
                        
                        AppLogger.d(tag, "键盘弹出: IME bottom=${imeInsets.bottom}px")
                        
                        // 触发 WebView 滚动到焦点输入框
                        // 使用延迟确保布局已更新
                        view.postDelayed({
                            scrollWebViewToFocusedInput(activity)
                        }, 100)
                    } else {
                        // 键盘隐藏：重置 padding
                        view.setPadding(
                            view.paddingLeft,
                            view.paddingTop,
                            view.paddingRight,
                            0
                        )
                    }
                    
                    windowInsets
                }
                
                // 触发一次 insets 重新计算
                ViewCompat.requestApplyInsets(contentView)
                AppLogger.d(tag, "键盘模式: RESIZE (WindowInsets 监听)")
            }
            
            KeyboardAdjustMode.NOTHING -> {
                // NOTHING 模式：键盘覆盖内容，不调整布局
                @Suppress("DEPRECATION")
                activity.window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                )
                
                // 移除之前的 insets 监听器，重置 padding
                ViewCompat.setOnApplyWindowInsetsListener(contentView, null)
                contentView.setPadding(
                    contentView.paddingLeft,
                    contentView.paddingTop,
                    contentView.paddingRight,
                    0
                )
                
                AppLogger.d(tag, "键盘模式: NOTHING (覆盖)")
            }
        }
    }
    
    /**
     * 在 WebView 中滚动到当前焦点输入框
     * 
     * 当键盘弹出且内容区域被 padding 压缩后，WebView 内部的焦点输入框
     * 可能仍然在可视区域之外。注入 JS 调用 scrollIntoView 确保可见。
     */
    private fun scrollWebViewToFocusedInput(activity: Activity) {
        try {
            // 在 View 树中寻找 WebView
            val webView = findWebViewInHierarchy(activity.window.decorView)
            webView?.evaluateJavascript("""
                (function() {
                    var el = document.activeElement;
                    if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) {
                        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }
                })();
            """.trimIndent(), null)
        } catch (e: Exception) {
            AppLogger.w("WindowHelper", "scrollWebViewToFocusedInput failed", e)
        }
    }
    
    /**
     * 在 View 层级中递归查找 WebView 实例
     */
    private fun findWebViewInHierarchy(view: View): android.webkit.WebView? {
        if (view is android.webkit.WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findWebViewInHierarchy(view.getChildAt(i))
                if (result != null) return result
            }
        }
        return null
    }

    // ==================== Video Custom View ====================

    /**
     * Show a video custom view (enters landscape fullscreen).
     *
     * @return the original orientation before fullscreen (caller should store this)
     */
    fun showCustomView(
        activity: Activity,
        view: View
    ): Int {
        val originalOrientation = activity.requestedOrientation
        
        // 仅当 custom view 是视频播放器时切换到横屏
        // 游戏/Canvas 等全屏请求不应改变方向
        // 视频播放器通常是 SurfaceView 或 TextureView
        val isVideoView = view is android.view.SurfaceView ||
                          view is android.view.TextureView ||
                          (view is ViewGroup && hasVideoChildView(view))
        
        if (isVideoView) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            AppLogger.d("WindowHelper", "Video fullscreen: switching to SENSOR_LANDSCAPE")
        } else {
            // 非视频内容（游戏、Canvas 等）：保持当前方向
            AppLogger.d("WindowHelper", "Non-video fullscreen: keeping current orientation ($originalOrientation)")
        }

        val decorView = activity.window.decorView as FrameLayout
        decorView.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        return originalOrientation
    }
    
    /**
     * 递归检查 ViewGroup 中是否包含视频播放 View (SurfaceView/TextureView)
     * 用于判断 onShowCustomView 传入的是视频全屏还是其他全屏内容
     */
    private fun hasVideoChildView(parent: ViewGroup): Boolean {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is android.view.SurfaceView || child is android.view.TextureView) {
                return true
            }
            if (child is ViewGroup && hasVideoChildView(child)) {
                return true
            }
        }
        return false
    }

    /**
     * Hide the video custom view (restores previous orientation).
     */
    fun hideCustomView(
        activity: Activity,
        view: View,
        callback: WebChromeClient.CustomViewCallback?,
        originalOrientation: Int
    ) {
        val decorView = activity.window.decorView as FrameLayout
        decorView.removeView(view)
        callback?.onCustomViewHidden()
        activity.requestedOrientation = originalOrientation
    }
}
