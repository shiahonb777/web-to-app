package com.webtoapp.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android TV 检测工具
 */
object TvUtils {

    /**
     * 检测当前设备是否为 Android TV
     * 通过 UiModeManager 和 Leanback feature 双重检测
     */
    fun isTv(context: Context): Boolean {
        // 方式1：通过 UiModeManager 检测
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true
        }

        // 方式2：通过 Leanback feature 检测（部分 TV 盒子可能不设置 UI_MODE）
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            context.packageManager.hasSystemFeature("android.software.leanback")) {
            return true
        }

        // 方式3：通过 FEATURE_TELEVISION 检测（旧版 API）
        @Suppress("DEPRECATION")
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
            return true
        }

        return false
    }
}

/**
 * Composable 版本的 TV 检测
 * 在 UI 层方便使用
 */
@Composable
fun isRunningOnTv(): Boolean {
    val context = LocalContext.current
    return remember { TvUtils.isTv(context) }
}
