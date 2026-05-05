package com.webtoapp.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext




object TvUtils {





    fun isTv(context: Context): Boolean {

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true
        }


        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            context.packageManager.hasSystemFeature("android.software.leanback")) {
            return true
        }


        @Suppress("DEPRECATION")
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
            return true
        }

        return false
    }
}





@Composable
fun isRunningOnTv(): Boolean {
    val context = LocalContext.current
    return remember { TvUtils.isTv(context) }
}
