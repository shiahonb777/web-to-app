package com.webtoapp.core.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellRuntimeServices
import java.util.Calendar

/**
 * Handles scheduled auto-start broadcast.
 *
 * Uses one-shot exact alarms plus rescheduling after each trigger.
 * Uses goAsync() and WakeLock for more reliable execution.
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

        // goAsync()
        val pendingResult = goAsync()

        // WakeLock CPU
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            acquire(WAKELOCK_TIMEOUT_MS)
        }

        try {
            val autoStartManager = AutoStartManager(context)

            // Note.
            val isShellMode = try {
                ShellRuntimeServices.shellMode.isShellMode()
            } catch (e: Exception) {
                false
            }

            // （ 1= .. 7=）
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (today == Calendar.SUNDAY) 7 else today - 1

            if (isShellMode) {
                handleShellMode(context, dayOfWeek)
            } else {
                handleMainAppMode(context, autoStartManager, dayOfWeek)
            }

            // ★ ：
            autoStartManager.rescheduleAfterTrigger()
        } catch (e: Exception) {
            AppLogger.e(TAG, "定时启动处理异常", e)
        } finally {
            try {
                if (wakeLock.isHeld) wakeLock.release()
            } catch (_: Exception) {}
            try {
                pendingResult.finish()
            } catch (_: Exception) {}
        }
    }

    /**
     * Shell
     */
    private fun handleShellMode(context: Context, dayOfWeek: Int) {
        val config = try {
            ShellRuntimeServices.shellMode.getConfig()
        } catch (e: Exception) {
            AppLogger.e(TAG, "获取 Shell 配置失败", e)
            return
        }

        val autoStartConfig = config?.autoStartConfig
        if (autoStartConfig?.scheduledStartEnabled != true) {
            AppLogger.d(TAG, "Shell 模式：定时自启动未启用，跳过")
            return
        }

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

    /**
     * Note.
     */
    private fun handleMainAppMode(context: Context, autoStartManager: AutoStartManager, dayOfWeek: Int) {
        val config = autoStartManager.getScheduledStartConfig() ?: run {
            AppLogger.d(TAG, "主应用模式：无定时配置，跳过")
            return
        }

        if (config.days.contains(dayOfWeek)) {
            AppLogger.d(TAG, "主应用模式：今天(周$dayOfWeek)在启动日期列表中，启动应用 ${config.appId}")
            AutoStartLauncher.launch(
                context = context,
                source = "ScheduledStart/Main",
                appId = config.appId
            )
        } else {
            AppLogger.d(TAG, "主应用模式：今天(周$dayOfWeek)不在启动日期列表中，跳过")
        }
    }
}
