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
        val decorView = activity.window.decorView
        val contentView = activity.findViewById<View>(android.R.id.content) ?: return

        val mode = keyboardAdjustMode ?: KeyboardAdjustMode.RESIZE

        when (mode) {
            KeyboardAdjustMode.RESIZE -> {

                @Suppress("DEPRECATION")
                activity.window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                )

                ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, windowInsets ->
                    val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
                    val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

                    if (imeVisible) {


                        view.setPadding(
                            view.paddingLeft,
                            view.paddingTop,
                            view.paddingRight,
                            imeInsets.bottom
                        )

                        AppLogger.d(tag, "键盘弹出: IME bottom=${imeInsets.bottom}px")



                        view.postDelayed({
                            scrollWebViewToFocusedInput(activity)
                        }, 100)
                    } else {

                        view.setPadding(
                            view.paddingLeft,
                            view.paddingTop,
                            view.paddingRight,
                            0
                        )
                    }

                    windowInsets
                }


                ViewCompat.requestApplyInsets(contentView)
                AppLogger.d(tag, "键盘模式: RESIZE (WindowInsets 监听)")
            }

            KeyboardAdjustMode.NOTHING -> {

                @Suppress("DEPRECATION")
                activity.window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                )


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
        view: View
    ): Int {
        val originalOrientation = activity.requestedOrientation




        val isVideoView = view is android.view.SurfaceView ||
                          view is android.view.TextureView ||
                          (view is ViewGroup && hasVideoChildView(view))

        if (isVideoView) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            AppLogger.d("WindowHelper", "Video fullscreen: switching to SENSOR_LANDSCAPE")
        } else {

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
