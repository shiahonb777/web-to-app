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
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * 强制运行守护服务（前台服务）
 * 
 * 第二层防护：当辅助功能服务不可用时的备份方案
 * 
 * 原理：
 * 1. 作为前台服务持续运行，不会被系统轻易杀死
 * 2. 使用 UsageStatsManager 定期检查当前前台应用
 * 3. 如果检测到用户离开了目标应用，立即将其拉回
 * 4. 结合 WakeLock 保持 CPU 运行
 * 
 * 注意：需要用户授权"使用情况访问权限"
 */
class ForcedRunGuardService : Service() {
    
    companion object {
        private const val TAG = "ForcedRunGuardService"
        private const val NOTIFICATION_ID = 19527
        private const val CHANNEL_ID = "forced_run_guard"
        
        // Check间隔（毫秒）- 越小越激进，但越耗电
        private const val CHECK_INTERVAL_AGGRESSIVE = 200L  // 激进模式：200ms
        private const val CHECK_INTERVAL_NORMAL = 500L     // 普通模式：500ms
        private const val CHECK_INTERVAL_POWER_SAVE = 1000L // 省电模式：1s
        
        @Volatile
        private var instance: ForcedRunGuardService? = null
        
        @Volatile
        var isRunning = false
            private set
        
        // 强制运行配置
        @Volatile
        var targetPackageName: String? = null
        
        @Volatile
        var targetActivityClass: String? = null
        
        @Volatile
        var checkInterval: Long = CHECK_INTERVAL_AGGRESSIVE
        
        /**
         * 启动守护服务
         */
        fun start(
            context: Context,
            packageName: String,
            activityClass: String,
            aggressive: Boolean = true
        ) {
            targetPackageName = packageName
            targetActivityClass = activityClass
            checkInterval = if (aggressive) CHECK_INTERVAL_AGGRESSIVE else CHECK_INTERVAL_NORMAL
            
            val intent = Intent(context, ForcedRunGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 停止守护服务
         */
        fun stop(context: Context) {
            targetPackageName = null
            targetActivityClass = null
            context.stopService(Intent(context, ForcedRunGuardService::class.java))
        }
        
        /**
         * 检查是否有使用情况访问权限
         */
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
        
        /**
         * 打开使用情况访问权限设置
         */
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
        Log.d(TAG, "守护服务创建")
        
        instance = this
        isRunning = true
        
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        
        createNotificationChannel()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "守护服务启动")
        
        // Start前台服务
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        // Start监控
        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)
        
        // 保证服务被杀后重启
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "守护服务销毁")
        
        instance = null
        isRunning = false
        
        handler.removeCallbacksAndMessages(null)
        releaseWakeLock()
    }
    
    /**
     * 检查当前前台应用并在需要时拉回
     */
    private fun checkAndBringBack() {
        val target = targetPackageName ?: return
        
        val currentForeground = getCurrentForegroundPackage()
        
        if (currentForeground != null && currentForeground != target) {
            // Check是否是系统 UI（允许短暂显示）
            val allowedPrefixes = listOf(
                "com.android.systemui",
                "com.android.launcher"
            )
            
            if (allowedPrefixes.none { currentForeground.startsWith(it) }) {
                Log.d(TAG, "检测到离开应用: $currentForeground, 拉回中...")
                bringAppToFront()
            }
        }
    }
    
    /**
     * 获取当前前台应用包名
     * 
     * 使用多种方法确保准确性：
     * 1. UsageStatsManager (Android 5.1+)
     * 2. ActivityManager.getRunningTasks (旧版本 / 特殊权限)
     */
    private fun getCurrentForegroundPackage(): String? {
        // Method1：使用 UsageStatsManager（推荐）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
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
                Log.w(TAG, "UsageStatsManager 查询失败", e)
            }
        }
        
        // Method2：使用 ActivityManager（需要特殊权限或旧版本）
        try {
            @Suppress("DEPRECATION")
            val tasks = activityManager?.getRunningTasks(1)
            if (!tasks.isNullOrEmpty()) {
                return tasks[0].topActivity?.packageName
            }
        } catch (e: Exception) {
            Log.w(TAG, "ActivityManager 查询失败", e)
        }
        
        return null
    }
    
    /**
     * 将目标应用拉回前台
     */
    private fun bringAppToFront() {
        val pkg = targetPackageName ?: return
        val activity = targetActivityClass
        
        try {
            // Method1：直接启动 Activity
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
            
            // Method2：moveTaskToFront
            try {
                val tasks = activityManager?.appTasks
                tasks?.forEach { task ->
                    if (task.taskInfo?.baseActivity?.packageName == pkg) {
                        task.moveToFront()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "moveTaskToFront 失败", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "拉回失败", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "强制运行守护",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持应用在前台运行"
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
            .setContentTitle("专注模式运行中")
            .setContentText("点击返回应用")
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
            wakeLock?.acquire(24 * 60 * 60 * 1000L) // 最多24小时
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock 获取失败", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock 释放失败", e)
        }
    }
}
