package com.webtoapp.core.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.logging.AppLogger
import java.util.Calendar

/**
 * Shell 专用定时启动广播接收器 — 不引用 WebViewActivity
 */
class ScheduledStartReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScheduledStartReceiver"
        private const val WAKELOCK_TAG = "WebToApp:ScheduledStart"
        private const val WAKELOCK_TIMEOUT_MS = 30_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AutoStartManager.ACTION_SCHEDULED_START) return

        AppLogger.d(TAG, "收到定时启动广播")
        val pendingResult = goAsync()

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
            acquire(WAKELOCK_TIMEOUT_MS)
        }

        try {
            val autoStartManager = AutoStartManager(context)

            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (today == Calendar.SUNDAY) 7 else today - 1

            val config = try {
                WebToAppApplication.shellMode.getConfig()
            } catch (e: Exception) { null }

            val autoStartConfig = config?.autoStartConfig
            if (autoStartConfig?.scheduledStartEnabled == true) {
                if (autoStartConfig.scheduledDays.contains(dayOfWeek)) {
                    AppLogger.d(TAG, "Shell 模式：今天(周$dayOfWeek)在启动日期列表中，启动应用")
                    AutoStartLauncher.launch(
                        context = context,
                        source = "ScheduledStart/Shell"
                    )
                } else {
                    AppLogger.d(TAG, "Shell 模式：今天(周$dayOfWeek)不在启动日期列表中，跳过")
                }
            }

            autoStartManager.rescheduleAfterTrigger()
        } catch (e: Exception) {
            AppLogger.e(TAG, "定时启动处理异常", e)
        } finally {
            try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Exception) {}
            try { pendingResult.finish() } catch (_: Exception) {}
        }
    }
}
