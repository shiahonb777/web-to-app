package com.webtoapp.core.autostart

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellRuntimeServices
import com.webtoapp.ui.shell.ShellActivity
import com.webtoapp.ui.webview.WebViewActivity

/**
 * Note.
 * BootReceiver ScheduledStartReceiver ，
 */
object AutoStartLauncher {

    private const val TAG = "AutoStartLauncher"

    /**
     * Note.
     *
     * @param context parameter
     * @param source parameter
     * @param appId parameter
     * @param delayMs parameter
     */
    fun launch(
        context: Context,
        source: String,
        appId: Long = -1,
        delayMs: Long = 0
    ) {
        val action = Runnable {
            val isShellMode = try {
                ShellRuntimeServices.shellMode.isShellMode()
            } catch (e: Exception) {
                false
            }

            if (isShellMode) {
                launchShellApp(context, source)
            } else if (appId > 0) {
                launchWebViewApp(context, source, appId)
            } else {
                AppLogger.w(TAG, "[$source] 无有效的 appId，跳过启动")
            }
        }

        if (delayMs > 0) {
            AppLogger.d(TAG, "[$source] 将在 ${delayMs}ms 后启动")
            Handler(Looper.getMainLooper()).postDelayed(action, delayMs)
        } else {
            action.run()
        }
    }

    /**
     * Shell
     */
    private fun launchShellApp(context: Context, source: String) {
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

    /**
     * WebView
     */
    private fun launchWebViewApp(context: Context, source: String, appId: Long) {
        try {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra("app_id", appId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppLogger.d(TAG, "[$source] WebView 应用已启动, appId=$appId")
        } catch (e: Exception) {
            AppLogger.e(TAG, "[$source] 启动 WebView 应用失败, appId=$appId", e)
        }
    }
}
