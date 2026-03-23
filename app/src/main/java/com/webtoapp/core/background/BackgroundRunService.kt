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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.webtoapp.R

/**
 * 后台运行服务
 * 
 * 使用前台服务保持应用在后台持续运行，即使用户退出应用界面
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
         * 启动后台运行服务
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
                Log.d(TAG, "服务已在运行")
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
                Log.d(TAG, "后台运行服务启动请求已发送")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start background service", e)
            }
        }
        
        /**
         * 停止后台运行服务
         */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, BackgroundRunService::class.java))
                Log.d(TAG, "后台运行服务停止请求已发送")
            } catch (e: Exception) {
                Log.e(TAG, "停止后台运行服务失败", e)
            }
        }
        
        /**
         * 检查服务是否正在运行
         */
        fun isServiceRunning(): Boolean = isRunning
    }
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var appName: String = ""
    private var keepCpuAwake: Boolean = true
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "后台运行服务创建")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "后台运行服务启动")
        
        appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: ""
        val notificationTitle = intent?.getStringExtra(EXTRA_NOTIFICATION_TITLE)
        val notificationContent = intent?.getStringExtra(EXTRA_NOTIFICATION_CONTENT)
        val showNotification = intent?.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true) ?: true
        keepCpuAwake = intent?.getBooleanExtra(EXTRA_KEEP_CPU_AWAKE, true) ?: true
        
        // Start前台服务
        val notification = createNotification(
            title = notificationTitle ?: (if (appName.isNotEmpty()) appName else "应用") + "正在后台运行",
            content = notificationContent ?: "点击返回应用"
        )
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true
        
        // Get WakeLock 保持 CPU 运行
        if (keepCpuAwake) {
            acquireWakeLock()
        }
        
        Log.d(TAG, "Background service started: appName=$appName, keepCpuAwake=$keepCpuAwake")
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "后台运行服务销毁")
        
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
        
        // 尝试使用应用图标，如果失败则使用系统图标
        val iconResId = try {
            // 尝试使用 ic_notification 图标（如果存在）
            val notifIconId = resources.getIdentifier("ic_notification", "drawable", packageName)
            if (notifIconId != 0) {
                notifIconId
            } else {
                // 回退到 ic_launcher 图标
                val launcherIconId = resources.getIdentifier("ic_launcher", "mipmap", packageName)
                if (launcherIconId != 0) launcherIconId else android.R.drawable.ic_dialog_info
            }
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }
        
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
                Log.d(TAG, "WakeLock 已获取")
            }
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock 已释放")
            }
        }
        wakeLock = null
    }
}
