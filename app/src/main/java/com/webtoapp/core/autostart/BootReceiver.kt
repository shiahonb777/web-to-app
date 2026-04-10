package com.webtoapp.core.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.WebToAppApplication

/**
 * 开机 / 时间变更 广播接收器
 *
 * 监听以下系统事件：
 * - BOOT_COMPLETED       — 设备开机完成后自动启动配置的应用
 * - QUICKBOOT_POWERON    — HTC/Samsung 快速启动完成（补充覆盖）
 * - LOCKED_BOOT_COMPLETED — Direct Boot 模式支持（Android 7+）
 * - TIME_SET             — 用户手动修改系统时间后重新调度闹钟
 * - TIMEZONE_CHANGED     — 用户切换时区后重新调度闹钟
 * - MY_PACKAGE_REPLACED  — 应用更新后恢复闹钟调度（系统会清除 PendingIntent）
 *
 * 优化点（v2）：
 * 1. 使用 goAsync() 获取额外处理时间，避免 10s ANR 限制
 * 2. 持有 WakeLock 确保 CPU 不会在 Handler.postDelayed 期间休眠
 * 3. 支持 HTC/Samsung 的 QUICKBOOT_POWERON 广播
 * 4. 应用更新后自动恢复闹钟调度
 * 5. 开机延迟使用可配置的 bootDelay 参数
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val WAKELOCK_TAG = "WebToApp:BootReceiver"
        private const val WAKELOCK_TIMEOUT_MS = 60_000L // 最长持锁 60s
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

    /**
     * 处理开机完成事件
     */
    private fun handleBootCompleted(context: Context) {
        AppLogger.d(TAG, "收到开机完成广播")

        // goAsync() 获取额外处理时间（从 10s 延长到约 30s）
        val pendingResult = goAsync()

        val autoStartManager = AutoStartManager(context)
        val bootDelay = autoStartManager.getBootDelay()

        // 获取 WakeLock 确保延迟启动期间 CPU 不休眠
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            acquire(WAKELOCK_TIMEOUT_MS)
        }

        try {
            // ① 开机自启动
            val isShellMode = try {
                WebToAppApplication.shellMode.isShellMode()
            } catch (e: Exception) {
                false
            }

            if (isShellMode) {
                val config = try {
                    WebToAppApplication.shellMode.getConfig()
                } catch (e: Exception) {
                    null
                }
                if (config?.autoStartConfig?.bootStartEnabled == true) {
                    AppLogger.d(TAG, "Shell 模式：延迟 ${bootDelay}ms 后启动开机自启动应用")
                    AutoStartLauncher.launch(
                        context = context,
                        source = "BootReceiver/Shell",
                        delayMs = bootDelay
                    )
                }
            } else {
                val bootStartAppId = autoStartManager.getBootStartAppId()
                if (bootStartAppId > 0) {
                    AppLogger.d(TAG, "主应用模式：延迟 ${bootDelay}ms 后启动应用 $bootStartAppId")
                    AutoStartLauncher.launch(
                        context = context,
                        source = "BootReceiver/Main",
                        appId = bootStartAppId,
                        delayMs = bootDelay
                    )
                }
            }

            // ② 重新设置定时闹钟（开机后系统闹钟会丢失）
            autoStartManager.rescheduleAlarmIfNeeded()
        } catch (e: Exception) {
            AppLogger.e(TAG, "开机自启动处理异常", e)
        } finally {
            // 释放 WakeLock + 通知系统本次广播处理完成
            try {
                if (wakeLock.isHeld) wakeLock.release()
            } catch (_: Exception) {}
            try {
                pendingResult.finish()
            } catch (_: Exception) {}
        }
    }

    /**
     * 处理应用更新事件
     * 应用更新后，之前注册的 PendingIntent 会被系统清除，需要重新调度闹钟
     */
    private fun handlePackageReplaced(context: Context) {
        AppLogger.d(TAG, "收到应用更新广播，恢复闹钟调度")
        try {
            val autoStartManager = AutoStartManager(context)
            autoStartManager.rescheduleAlarmIfNeeded()
        } catch (e: Exception) {
            AppLogger.e(TAG, "应用更新后恢复闹钟失败", e)
        }
    }

    /**
     * 处理时间/时区变更事件
     * 系统时间改变后，之前调度的精确闹钟可能不再准确，需要重新调度
     */
    private fun handleTimeChanged(context: Context, action: String) {
        AppLogger.d(TAG, "收到时间变更广播: $action")

        try {
            val autoStartManager = AutoStartManager(context)
            autoStartManager.rescheduleAlarmIfNeeded()
        } catch (e: Exception) {
            AppLogger.e(TAG, "时间变更后重新调度闹钟失败", e)
        }
    }
}
