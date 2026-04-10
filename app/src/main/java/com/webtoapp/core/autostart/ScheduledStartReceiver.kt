package com.webtoapp.core.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.WebToAppApplication
import java.util.Calendar

/**
 * 定时启动广播接收器
 *
 * 接收精确闹钟触发的广播，启动配置的应用，并立即调度下一次闹钟。
 *
 * 优化点（v2）：
 * 1. 使用 "单次精确闹钟 + 触发后 reschedule" 模式替换 setRepeating
 * 2. 只在匹配日才真正启动（双重保障，与 Manager 的精确计算互补）
 * 3. 使用 AutoStartLauncher 集中处理启动逻辑，消除重复代码
 * 4. 触发后自动调度下一次，保证连续运行
 * 5. goAsync() + WakeLock 保证可靠执行
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

        // goAsync() 获取额外处理时间
        val pendingResult = goAsync()

        // WakeLock 防止 CPU 在处理期间休眠
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            acquire(WAKELOCK_TIMEOUT_MS)
        }

        try {
            val autoStartManager = AutoStartManager(context)

            // 检测运行模式
            val isShellMode = try {
                WebToAppApplication.shellMode.isShellMode()
            } catch (e: Exception) {
                false
            }

            // 获取今天是星期几（转换为 1=周一 .. 7=周日）
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (today == Calendar.SUNDAY) 7 else today - 1

            if (isShellMode) {
                handleShellMode(context, dayOfWeek)
            } else {
                handleMainAppMode(context, autoStartManager, dayOfWeek)
            }

            // ★ 核心：触发后立即调度下一次闹钟
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
     * Shell 模式下的定时启动处理
     */
    private fun handleShellMode(context: Context, dayOfWeek: Int) {
        val config = try {
            WebToAppApplication.shellMode.getConfig()
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
     * 主应用模式下的定时启动处理
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
