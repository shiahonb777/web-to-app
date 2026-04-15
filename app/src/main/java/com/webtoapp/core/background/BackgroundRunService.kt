package com.webtoapp.core.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import androidx.core.app.NotificationCompat
import com.webtoapp.R
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * Note.
 * 
 * ，
 */
class BackgroundRunService : Service() {
    
    companion object {
        private const val TAG = "BackgroundRunService"
        private const val CHANNEL_ID = "background_run_channel"
        private const val NOTIFICATION_ID = 10001
        
        private const val EXTRA_APP_NAME = "app_name"
        private const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        private const val EXTRA_NOTIFICATION_CONTENT = "notification_content"
        private const val EXTRA_SHOW_NOTIFICATION = "show_notification"
        private const val EXTRA_KEEP_CPU_AWAKE = "keep_cpu_awake"
        
        private var isRunning = false
        
        /**
         * Note.
         */
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
        
        /**
         * Note.
         */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, BackgroundRunService::class.java))
                AppLogger.w(TAG, "后台运行服务停止请求已发送")
            } catch (e: Exception) {
                AppLogger.e(TAG, "停止后台运行服务失败", e)
            }
        }
        
        /**
         * Note.
         */
        fun isServiceRunning(): Boolean = isRunning
    }
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var appName: String = ""
    private var keepCpuAwake: Boolean = true
    
    override fun onCreate() {
        super.onCreate()
        AppLogger.w(TAG, "后台运行服务创建")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.w(TAG, "后台运行服务启动")
        
        appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: ""
        val notificationTitle = intent?.getStringExtra(EXTRA_NOTIFICATION_TITLE)
        val notificationContent = intent?.getStringExtra(EXTRA_NOTIFICATION_CONTENT)
        val showNotification = intent?.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true) ?: true
        keepCpuAwake = intent?.getBooleanExtra(EXTRA_KEEP_CPU_AWAKE, true) ?: true
        
        // Start
        val notification = createNotification(
            title = notificationTitle ?: (if (appName.isNotEmpty()) appName else "App") + " ${AppStringsProvider.current().appRunningInBackground}",
            content = notificationContent ?: AppStringsProvider.current().tapToReturnToApp
        )
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true
        
        // Get WakeLock CPU
        if (keepCpuAwake) {
            acquireWakeLock()
        }
        
        AppLogger.w(TAG, "Background service started: appName=$appName, keepCpuAwake=$keepCpuAwake")
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        AppLogger.w(TAG, "后台运行服务销毁")
        
        isRunning = false
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
            .build()
    }
    
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WebToApp:BackgroundRunWakeLock"
            )
        }
        
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(24 * 60 * 60 * 1000L)
                AppLogger.w(TAG, "WakeLock 已获取")
            }
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                AppLogger.w(TAG, "WakeLock 已释放")
            }
        }
        wakeLock = null
    }
}
