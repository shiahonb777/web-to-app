package com.webtoapp.core.autostart

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.shell.ShellActivity

/**
 * Shell 专用自启动启动器 — 仅启动 ShellActivity，不引用 WebViewActivity
 */
object AutoStartLauncher {

    private const val TAG = "AutoStartLauncher"

    fun launch(
        context: Context,
        source: String,
        appId: Long = -1,
        delayMs: Long = 0
    ) {
        val action = Runnable {
            try {
                val intent = Intent(context, ShellActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                AppLogger.d(TAG, "[$source] Shell 应用已启动")
            } catch (e: Exception) {
                AppLogger.e(TAG, "[$source] 启动 Shell 应用失败", e)
            }
        }

        if (delayMs > 0) {
            AppLogger.d(TAG, "[$source] 将在 ${delayMs}ms 后启动")
            Handler(Looper.getMainLooper()).postDelayed(action, delayMs)
        } else {
            action.run()
        }
    }
}
