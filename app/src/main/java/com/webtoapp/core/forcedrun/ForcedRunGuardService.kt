package com.webtoapp.core.forcedrun

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import com.webtoapp.core.i18n.Strings
import android.provider.Settings
import com.webtoapp.core.logging.AppLogger
import androidx.core.app.NotificationCompat














class ForcedRunGuardService : Service() {

    companion object {
        private const val TAG = "ForcedRunGuardService"
        private const val NOTIFICATION_ID = 19527
        private const val CHANNEL_ID = "forced_run_guard"


        private const val CHECK_INTERVAL_AGGRESSIVE = 200L
        private const val CHECK_INTERVAL_NORMAL = 500L
        private const val CHECK_INTERVAL_POWER_SAVE = 1000L

        @Volatile
        private var instance: ForcedRunGuardService? = null

        @Volatile
        var isRunning = false
            private set


        @Volatile
        var targetPackageName: String? = null

        @Volatile
        var targetActivityClass: String? = null

        @Volatile
        var checkInterval: Long = CHECK_INTERVAL_AGGRESSIVE


        @Volatile
        var remainingTimeMs: Long = 0L







        fun start(
            context: Context,
            packageName: String,
            activityClass: String,
            aggressive: Boolean = true,
            remainingMs: Long = 0L
        ) {
            targetPackageName = packageName
            targetActivityClass = activityClass
            checkInterval = if (aggressive) CHECK_INTERVAL_AGGRESSIVE else CHECK_INTERVAL_NORMAL
            remainingTimeMs = remainingMs

            val intent = Intent(context, ForcedRunGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }




        fun stop(context: Context) {
            targetPackageName = null
            targetActivityClass = null
            context.stopService(Intent(context, ForcedRunGuardService::class.java))
        }




        fun hasUsageStatsPermission(context: Context): Boolean {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return false

            val now = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 1000 * 60,
                now
            )

            return stats != null && stats.isNotEmpty()
        }




        fun openUsageAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var usageStatsManager: UsageStatsManager? = null
    private var activityManager: ActivityManager? = null

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (targetPackageName != null) {
                checkAndBringBack()
                handler.postDelayed(this, checkInterval)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(TAG, "守护服务创建")

        instance = this
        isRunning = true

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.i(TAG, "守护服务启动")


        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }


        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)


        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.i(TAG, "守护服务销毁")

        instance = null
        isRunning = false

        handler.removeCallbacksAndMessages(null)
        releaseWakeLock()
    }




    private fun checkAndBringBack() {
        val target = targetPackageName ?: return

        val currentForeground = getCurrentForegroundPackage()

        if (currentForeground != null && currentForeground != target) {

            val allowedPrefixes = listOf(
                "com.android.systemui",
                "com.android.launcher"
            )

            if (allowedPrefixes.none { currentForeground.startsWith(it) }) {
                AppLogger.i(TAG, "检测到离开应用: $currentForeground, 拉回中...")
                bringAppToFront()
            }
        }
    }








    private fun getCurrentForegroundPackage(): String? {

        try {
            val now = System.currentTimeMillis()
            val events = usageStatsManager?.queryEvents(now - 10000, now)

            if (events != null) {
                var lastPackage: String? = null
                val event = UsageEvents.Event()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                        event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastPackage = event.packageName
                    }
                }

                if (lastPackage != null) {
                    return lastPackage
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "UsageStatsManager 查询失败", e)
        }


        try {
            @Suppress("DEPRECATION")
            val tasks = activityManager?.getRunningTasks(1)
            if (!tasks.isNullOrEmpty()) {
                return tasks[0].topActivity?.packageName
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "ActivityManager 查询失败", e)
        }

        return null
    }




    private fun bringAppToFront() {
        val pkg = targetPackageName ?: return
        val activity = targetActivityClass

        try {

            val intent = if (activity != null) {
                Intent().apply {
                    component = ComponentName(pkg, activity)
                }
            } else {
                packageManager.getLaunchIntentForPackage(pkg)
            }

            intent?.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            intent?.let { startActivity(it) }


            try {
                val tasks = activityManager?.appTasks
                tasks?.forEach { task ->
                    if (task.taskInfo?.baseActivity?.packageName == pkg) {
                        task.moveToFront()
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "moveTaskToFront 失败", e)
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "拉回失败", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                Strings.notifFocusModeChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = Strings.notifFocusModeChannelDesc
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(targetPackageName ?: packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(Strings.notifFocusModeRunning)
            .setContentText(Strings.notifClickToReturn)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }







    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WebToApp:ForcedRunGuard"
            )

            val bufferMs = 5 * 60 * 1000L
            val defaultDurationMs = 4 * 60 * 60 * 1000L
            val maxDurationMs = 24 * 60 * 60 * 1000L

            val holdDuration = if (remainingTimeMs > 0) {
                (remainingTimeMs + bufferMs).coerceAtMost(maxDurationMs)
            } else {
                defaultDurationMs
            }

            wakeLock?.acquire(holdDuration)
            AppLogger.i(TAG, "WakeLock acquired: duration=${holdDuration / 1000}s " +
                    "(remaining=${remainingTimeMs / 1000}s + buffer=${bufferMs / 1000}s)")
        } catch (e: Exception) {
            AppLogger.e(TAG, "WakeLock 获取失败", e)
        }
    }






    fun refreshWakeLock() {
        releaseWakeLock()
        acquireWakeLock()
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    AppLogger.i(TAG, "WakeLock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            AppLogger.e(TAG, "WakeLock 释放失败", e)
        }
    }
}
