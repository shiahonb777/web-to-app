package com.webtoapp.ui.shared

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.KeyboardAdjustMode

/**
 * Shared window/system-bar helper used by both WebViewActivity and ShellActivity.
 *
 * Centralises:
 * - Status-bar colour application
 * - Immersive fullscreen toggling
 * - Video custom-view show/hide
 * - Colour-luminance helper
 */
object WindowHelper {

    // ==================== Status Bar ====================

    /**
     * Apply status-bar colour based on [colorMode].
     *
     * @param colorMode "THEME", "TRANSPARENT", or "CUSTOM"
     */
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
                // THEME mode
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

    // ==================== Colour Helpers ====================

    fun isColorLight(color: Int): Boolean {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return luminance > 0.5
    }

    // ==================== Immersive Fullscreen ====================

    /**
     * Toggle immersive fullscreen.
     *
     * @param statusBarColorMode   Current colour mode string ("THEME"/"TRANSPARENT"/"CUSTOM")
     * @param statusBarCustomColor Custom colour hex (used when mode is "CUSTOM")
     * @param statusBarDarkIcons   Force dark icons, or null for auto
     * @param statusBarBgType      "COLOR" or "IMAGE"
     * @param showStatusBar        Whether to keep the status bar visible in fullscreen
     * @param forceHideSystemUi    If true, hide both status bar and nav bar regardless of other settings
     * @param keyboardAdjustMode   Keyboard adjust mode (RESIZE = push content up, NOTHING = overlay content)
     *                              If null, uses legacy behavior: NOTHING when enabled, RESIZE when disabled
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

            // Determine soft input mode based on keyboardAdjustMode preference
            // If keyboardAdjustMode is null (not specified), use legacy behavior for backward compatibility
            val softInputMode = when (keyboardAdjustMode) {
                KeyboardAdjustMode.RESIZE -> WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                KeyboardAdjustMode.NOTHING -> WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                null -> if (enabled) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                        else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            @Suppress("DEPRECATION")
            activity.window.setSoftInputMode(softInputMode)

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
                        // User wants to keep navigation bar visible in fullscreen
                        controller.show(WindowInsetsCompat.Type.navigationBars())
                        // Only apply transient swipe to status bars (if hidden),
                        // keep nav bar permanently visible
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
        
        // Switch to landscape only when custom view is a video player
        // Full-screen requests from games/canvas should not change orientation
        // Video players are usually SurfaceView or TextureView
        val isVideoView = view is android.view.SurfaceView ||
                          view is android.view.TextureView ||
                          (view is ViewGroup && hasVideoChildView(view))
        
        if (isVideoView) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            AppLogger.d("WindowHelper", "Video fullscreen: switching to SENSOR_LANDSCAPE")
        } else {
            // Non-video content (game/canvas, etc.): keep current orientation
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
     * Recursively checks whether a ViewGroup contains video views (SurfaceView/TextureView).
     * Used to distinguish video full-screen from other full-screen custom content in onShowCustomView.
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
