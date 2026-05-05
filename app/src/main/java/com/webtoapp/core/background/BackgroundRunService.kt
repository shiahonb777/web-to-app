package com.webtoapp.core.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import androidx.core.app.NotificationCompat
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings






class BackgroundRunService : Service() {

    companion object {
        private const val TAG = "BackgroundRunService"
        private const val CHANNEL_ID = "background_run_channel"
        private const val NOTIFICATION_ID = 10001
        private const val WAKELOCK_TIMEOUT_MS = 4 * 60 * 60 * 1000L
        private const val WAKELOCK_RENEWAL_INTERVAL_MS = 3 * 60 * 60 * 1000L
        private const val ACTION_STOP = "com.webtoapp.action.STOP_BACKGROUND_RUN"

        private const val EXTRA_APP_NAME = "app_name"
        private const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        private const val EXTRA_NOTIFICATION_CONTENT = "notification_content"
        private const val EXTRA_SHOW_NOTIFICATION = "show_notification"
        private const val EXTRA_KEEP_CPU_AWAKE = "keep_cpu_awake"

        private var isRunning = false




        fun start(
            context: Context,
            appName: String = "",
            notificationTitle: String? = null,
            notificationContent: String? = null,
            showNotification: Boolean = true,
            keepCpuAwake: Boolean = true
        ) {
            if (isRunning) {
                AppLogger.w(TAG, "服务已在运行")
                return
            }

            val intent = Intent(context, BackgroundRunService::class.java).apply {
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_NOTIFICATION_TITLE, notificationTitle)
                putExtra(EXTRA_NOTIFICATION_CONTENT, notificationContent)
                putExtra(EXTRA_SHOW_NOTIFICATION, showNotification)
                putExtra(EXTRA_KEEP_CPU_AWAKE, keepCpuAwake)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                AppLogger.w(TAG, "后台运行服务启动请求已发送")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start background service", e)
            }
        }




        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, BackgroundRunService::class.java))
                AppLogger.w(TAG, "后台运行服务停止请求已发送")
            } catch (e: Exception) {
                AppLogger.e(TAG, "停止后台运行服务失败", e)
            }
        }




        fun isServiceRunning(): Boolean = isRunning





        fun requestIgnoreBatteryOptimizations(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == false) {
                        val intent = Intent(
                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        ).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        AppLogger.i(TAG, "已请求忽略电池优化")
                    } else {
                        AppLogger.i(TAG, "已在电池优化白名单中")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "请求忽略电池优化失败", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var appName: String = ""
    private var keepCpuAwake: Boolean = true
    private val wakeLockRenewalHandler = Handler(Looper.getMainLooper())
    private val wakeLockRenewalRunnable = object : Runnable {
        override fun run() {
            renewWakeLock()
            wakeLockRenewalHandler.postDelayed(this, WAKELOCK_RENEWAL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.w(TAG, "后台运行服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        AppLogger.w(TAG, "后台运行服务启动")

        appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: ""
        val notificationTitle = intent?.getStringExtra(EXTRA_NOTIFICATION_TITLE)
        val notificationContent = intent?.getStringExtra(EXTRA_NOTIFICATION_CONTENT)
        val showNotification = intent?.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true) ?: true
        keepCpuAwake = intent?.getBooleanExtra(EXTRA_KEEP_CPU_AWAKE, true) ?: true


        try {
            val notification = createNotification(
                title = notificationTitle ?: (if (appName.isNotEmpty()) appName else "App") + " ${Strings.appRunningInBackground}",
                content = notificationContent ?: Strings.tapToReturnToApp
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

                startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isRunning = true
        } catch (e: Exception) {
            AppLogger.e(TAG, "startForeground 失败，服务无法启动", e)
            stopSelf()
            return START_NOT_STICKY
        }


        if (keepCpuAwake) {
            acquireWakeLock()

            wakeLockRenewalHandler.removeCallbacks(wakeLockRenewalRunnable)
            wakeLockRenewalHandler.postDelayed(wakeLockRenewalRunnable, WAKELOCK_RENEWAL_INTERVAL_MS)
        }

        AppLogger.w(TAG, "Background service started: appName=$appName, keepCpuAwake=$keepCpuAwake")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.w(TAG, "后台运行服务销毁")

        isRunning = false
        wakeLockRenewalHandler.removeCallbacks(wakeLockRenewalRunnable)
        releaseWakeLock()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Run",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keep app running in background"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null


        val stopIntent = Intent(this, BackgroundRunService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconResId = applicationInfo.icon.takeIf { it != 0 } ?: R.mipmap.ic_launcher

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(iconResId)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                }
            }
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                Strings.backgroundRunStop,
                stopPendingIntent
            )
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WebToApp:BackgroundRunWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
        }

        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(WAKELOCK_TIMEOUT_MS)
                AppLogger.w(TAG, "WakeLock 已获取 (超时=${WAKELOCK_TIMEOUT_MS}ms)")
            }
        }
    }




    private fun renewWakeLock() {
        try {
            releaseWakeLock()
            acquireWakeLock()
            AppLogger.d(TAG, "WakeLock 已续期")
        } catch (e: Exception) {
            AppLogger.e(TAG, "WakeLock 续期失败", e)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                    AppLogger.w(TAG, "WakeLock 已释放")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "WakeLock 释放异常", e)
                }
            }
        }
        wakeLock = null
    }
}
