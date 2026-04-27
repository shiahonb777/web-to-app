package com.webtoapp.core.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.logging.AppLogger

/**
 * Shell 专用开机广播接收器 — 不引用 WebViewActivity
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val WAKELOCK_TAG = "WebToApp:BootReceiver"
        private const val WAKELOCK_TIMEOUT_MS = 60_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                handleBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                handlePackageReplaced(context)
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                handleTimeChanged(context, intent.action ?: "")
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        AppLogger.d(TAG, "收到开机完成广播")
        val pendingResult = goAsync()
        val autoStartManager = AutoStartManager(context)
        val bootDelay = autoStartManager.getBootDelay()

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
            acquire(WAKELOCK_TIMEOUT_MS)
        }

        try {
            val config = try {
                WebToAppApplication.shellMode.getConfig()
            } catch (e: Exception) { null }

            if (config?.autoStartConfig?.bootStartEnabled == true) {
                AppLogger.d(TAG, "Shell 模式：延迟 ${bootDelay}ms 后启动开机自启动应用")
                AutoStartLauncher.launch(
                    context = context,
                    source = "BootReceiver/Shell",
                    delayMs = bootDelay
                )
            }
            autoStartManager.rescheduleAlarmIfNeeded()
        } catch (e: Exception) {
            AppLogger.e(TAG, "开机自启动处理异常", e)
        } finally {
            try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Exception) {}
            try { pendingResult.finish() } catch (_: Exception) {}
        }
    }

    private fun handlePackageReplaced(context: Context) {
        AppLogger.d(TAG, "收到应用更新广播，恢复闹钟调度")
        try {
            AutoStartManager(context).rescheduleAlarmIfNeeded()
        } catch (e: Exception) {
            AppLogger.e(TAG, "应用更新后恢复闹钟失败", e)
        }
    }

    private fun handleTimeChanged(context: Context, action: String) {
        AppLogger.d(TAG, "收到时间变更广播: $action")
        try {
            AutoStartManager(context).rescheduleAlarmIfNeeded()
        } catch (e: Exception) {
            AppLogger.e(TAG, "时间变更后重新调度闹钟失败", e)
        }
    }
}
