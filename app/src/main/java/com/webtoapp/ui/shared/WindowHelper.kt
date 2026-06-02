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
import androidx.core.view.WindowInsetsAnimationCompat
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
        isDarkTheme: Boolean,
        backgroundAlpha: Float = 1f
    ) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        val alpha = backgroundAlpha.coerceIn(0f, 1f)

        when (colorMode) {
            "TRANSPARENT" -> {
                activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
                val useDarkIcons = darkIcons ?: !isDarkTheme
                controller.isAppearanceLightStatusBars = useDarkIcons
            }
            "CUSTOM" -> {
                val baseColor = try {
                    android.graphics.Color.parseColor(customColor ?: "#FFFFFF")
                } catch (e: Exception) {
                    android.graphics.Color.WHITE
                }
                activity.window.statusBarColor = applyAlpha(baseColor, alpha)
                val useDarkIcons = darkIcons ?: isColorLight(baseColor)
                controller.isAppearanceLightStatusBars = useDarkIcons
            }
            else -> {
                if (isDarkTheme) {
                    val base = android.graphics.Color.parseColor("#1C1B1F")
                    activity.window.statusBarColor = applyAlpha(base, alpha)
                    controller.isAppearanceLightStatusBars = false
                } else {
                    val base = android.graphics.Color.parseColor("#FFFBFE")
                    activity.window.statusBarColor = applyAlpha(base, alpha)
                    controller.isAppearanceLightStatusBars = true
                }
            }
        }

        controller.isAppearanceLightNavigationBars = controller.isAppearanceLightStatusBars
    }

    private fun applyAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return android.graphics.Color.argb(
            a,
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color)
        )
    }

    fun isColorLight(color: Int): Boolean {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return luminance > 0.5
    }

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

                    val needsCustomBackground = statusBarBgType == "IMAGE" ||
                        statusBarColorMode == "CUSTOM" ||
                        statusBarColorMode == "TRANSPARENT"

                    if (needsCustomBackground) {

                        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                        controller.show(WindowInsetsCompat.Type.systemBars())
                        activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT

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
                            }
                        }
                        controller.isAppearanceLightNavigationBars = controller.isAppearanceLightStatusBars
                    } else {

                        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                        controller.show(WindowInsetsCompat.Type.systemBars())
                        activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        applyStatusBarColor(activity, statusBarColorMode, statusBarCustomColor, statusBarDarkIcons, isDarkTheme)
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(tag, "applyImmersiveFullscreen failed", e)
        }
    }

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

    private fun applyKeyboardMode(
        activity: Activity,
        keyboardAdjustMode: KeyboardAdjustMode?,
        tag: String
    ) {
        val contentView = activity.findViewById<View>(android.R.id.content) ?: return

        val mode = keyboardAdjustMode ?: KeyboardAdjustMode.RESIZE

        when (mode) {
            KeyboardAdjustMode.RESIZE -> {

                @Suppress("DEPRECATION")
                activity.window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                )

                var imeAnimating = false

                fun applyImeBottomPadding(bottom: Int) {
                    if (contentView.paddingBottom != bottom) {
                        contentView.setPadding(
                            contentView.paddingLeft,
                            contentView.paddingTop,
                            contentView.paddingRight,
                            bottom
                        )
                    }
                }

                ViewCompat.setWindowInsetsAnimationCallback(
                    contentView,
                    object : WindowInsetsAnimationCompat.Callback(
                        WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP
                    ) {
                        override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                            if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                                imeAnimating = true
                            }
                        }

                        override fun onProgress(
                            insets: WindowInsetsCompat,
                            runningAnimations: MutableList<WindowInsetsAnimationCompat>
                        ): WindowInsetsCompat {

                            applyImeBottomPadding(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
                            return insets
                        }

                        override fun onEnd(animation: WindowInsetsAnimationCompat) {
                            if (animation.typeMask and WindowInsetsCompat.Type.ime() == 0) return
                            imeAnimating = false

                            val rootInsets = ViewCompat.getRootWindowInsets(contentView)
                            val imeVisible = rootInsets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
                            val imeBottom = rootInsets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                            applyImeBottomPadding(if (imeVisible) imeBottom else 0)

                            if (imeVisible) {
                                contentView.postDelayed({
                                    checkAndScrollWebViewToFocusedInput(activity)
                                }, 100)
                            }
                        }
                    }
                )

                ViewCompat.setOnApplyWindowInsetsListener(contentView) { _, windowInsets ->

                    if (!imeAnimating) {
                        val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
                        val imeBottom = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                        applyImeBottomPadding(if (imeVisible) imeBottom else 0)
                    }

                    windowInsets
                }

                ViewCompat.requestApplyInsets(contentView)
                AppLogger.d(tag, "键盘模式: RESIZE (IME 动画同步)")
            }

            KeyboardAdjustMode.NOTHING -> {

                @Suppress("DEPRECATION")
                activity.window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                )

                ViewCompat.setWindowInsetsAnimationCallback(contentView, null)
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

    private fun checkAndScrollWebViewToFocusedInput(activity: Activity) {
        try {
            val webView = findWebViewInHierarchy(activity.window.decorView)
            webView?.evaluateJavascript("""
                (function() {
                    var el = document.activeElement;
                    if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA' && !el.isContentEditable)) {
                        return 'no_input';
                    }
                    var rect = el.getBoundingClientRect();
                    var viewportHeight = window.visualViewport ? window.visualViewport.height : window.innerHeight;
                    var viewportTop = window.visualViewport ? window.visualViewport.offsetTop : 0;
                    // 如果元素已经在可视区域内，说明网页自己已经处理了上推
                    if (rect.top >= viewportTop && rect.bottom <= (viewportTop + viewportHeight)) {
                        return 'already_visible';
                    }
                    // 元素不在可视区域，执行滚动
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    return 'scrolled';
                })();
            """.trimIndent(), null)
        } catch (e: Exception) {
            AppLogger.w("WindowHelper", "checkAndScrollWebViewToFocusedInput failed", e)
        }
    }

    private fun scrollWebViewToFocusedInput(activity: Activity) {
        try {

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

    fun showCustomView(
        activity: Activity,
        view: View,
        fullscreenOrientation: com.webtoapp.data.model.FullscreenVideoOrientation =
            com.webtoapp.data.model.FullscreenVideoOrientation.AUTO_SENSOR_LANDSCAPE
    ): Int {
        val originalOrientation = activity.requestedOrientation

        when (fullscreenOrientation) {
            com.webtoapp.data.model.FullscreenVideoOrientation.AUTO_SENSOR_LANDSCAPE -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                AppLogger.d("WindowHelper", "Fullscreen: SENSOR_LANDSCAPE (auto-rotate with device)")
            }
            com.webtoapp.data.model.FullscreenVideoOrientation.FORCE_LANDSCAPE -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                AppLogger.d("WindowHelper", "Fullscreen: LANDSCAPE (forced)")
            }
            com.webtoapp.data.model.FullscreenVideoOrientation.KEEP_CURRENT -> {
                AppLogger.d("WindowHelper", "Fullscreen: keeping current orientation ($originalOrientation)")
            }
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
