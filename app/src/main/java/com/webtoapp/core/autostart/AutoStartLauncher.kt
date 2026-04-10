package com.webtoapp.core.autostart

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.shell.ShellActivity
import com.webtoapp.ui.webview.WebViewActivity

/**
 * 自启动应用启动器
 * 集中管理 BootReceiver 和 ScheduledStartReceiver 共用的启动逻辑，消除重复代码
 */
object AutoStartLauncher {

    private const val TAG = "AutoStartLauncher"

    /**
     * 检测当前运行模式并启动相应的应用
     *
     * @param context 上下文
     * @param source 调用来源（用于日志）
     * @param appId 主应用模式下要启动的应用 ID（Shell 模式下忽略）
     * @param delayMs 延迟启动毫秒数（0 = 立即启动）
     */
    fun launch(
        context: Context,
        source: String,
        appId: Long = -1,
        delayMs: Long = 0
    ) {
        val action = Runnable {
            val isShellMode = try {
                WebToAppApplication.shellMode.isShellMode()
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
     * 启动 Shell 模式应用
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
     * 启动主应用中的 WebView 应用
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
