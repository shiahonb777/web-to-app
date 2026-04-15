package com.webtoapp.core.floatingwindow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.webkit.WebView
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.FloatingWindowConfig

/**
 * 悬浮小窗前台服务
 * 管理悬浮窗的生命周期，确保窗口不会被系统回收
 *
 * 使用前台服务是因为：
 * 1. 悬浮窗需要持续显示，不会因后台限制被杀
 * 2. Android 8.0+ 要求后台服务必须有前台通知
 * 3. 通知可以提供快捷操作（恢复/关闭）
 */
class FloatingWindowService : Service() {

    companion object {
        private const val TAG = "FloatingWindowService"
        private const val NOTIFICATION_CHANNEL_ID = "floating_window_channel"
        private const val NOTIFICATION_ID = 2024
        
        // Intent extras
        const val EXTRA_CONFIG = "extra_floating_window_config"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_ACTION = "extra_action"
        
        // Actions
        const val ACTION_SHOW = "action_show"
        const val ACTION_DISMISS = "action_dismiss"
        const val ACTION_MINIMIZE = "action_minimize"
        const val ACTION_RESTORE = "action_restore"
        
        @Volatile
        private var instance: FloatingWindowService? = null
        
        fun getInstance(): FloatingWindowService? = instance
        
        /**
         * 检查是否有悬浮窗权限
         */
        fun canDrawOverlays(context: android.content.Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
        
        /**
         * 请求悬浮窗权限
         */
        fun requestOverlayPermission(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
    
    private lateinit var floatingWindowManager: FloatingWindowManager
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        floatingWindowManager = FloatingWindowManager(this)
        createNotificationChannel()
        AppLogger.i(TAG, "FloatingWindowService 已创建")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra(EXTRA_ACTION) ?: ACTION_SHOW
        
        when (action) {
            ACTION_SHOW -> {
                val configJson = intent?.getStringExtra(EXTRA_CONFIG)
                val url = intent?.getStringExtra(EXTRA_URL) ?: ""
                val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: ""
                
                val config = if (configJson != null) {
                    try {
                        com.webtoapp.util.GsonProvider.gson.fromJson(
                            configJson, FloatingWindowConfig::class.java
                        )
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "解析悬浮窗配置失败", e)
                        FloatingWindowConfig(enabled = true)
                    }
                } else {
                    FloatingWindowConfig(enabled = true)
                }
                
                // 启动前台通知
                startForeground(NOTIFICATION_ID, createNotification(appName))
                
                // 创建悬浮窗
                floatingWindowManager.onDismiss = {
                    stopSelf()
                }
                floatingWindowManager.show(config, appName, url)
                
                AppLogger.i(TAG, "悬浮窗已启动: url=$url, size=${config.windowSizePercent}%, opacity=${config.opacity}%")
            }
            
            ACTION_DISMISS -> {
                floatingWindowManager.dismiss()
                stopSelf()
            }
            
            ACTION_MINIMIZE -> {
                floatingWindowManager.minimize()
            }
            
            ACTION_RESTORE -> {
                floatingWindowManager.restore()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        floatingWindowManager.dismiss()
        instance = null
        AppLogger.i(TAG, "FloatingWindowService 已销毁")
        super.onDestroy()
    }
    
    /**
     * 获取悬浮窗管理器
     */
    fun getManager(): FloatingWindowManager = floatingWindowManager
    
    /**
     * 获取悬浮窗中的 WebView
     */
    fun getWebView(): WebView? = floatingWindowManager.getWebView()
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                AppStringsProvider.current().floatingWindowNotificationChannel,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = AppStringsProvider.current().floatingWindowNotificationChannelDesc
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建前台通知
     */
    private fun createNotification(appName: String = ""): Notification {
        val title = AppStringsProvider.current().floatingWindowNotificationTitle
        val content = if (appName.isNotBlank()) {
            String.format(AppStringsProvider.current().floatingWindowNotificationContent, appName)
        } else {
            AppStringsProvider.current().floatingWindowNotificationContentDefault
        }
        
        // 关闭悬浮窗的 PendingIntent
        val dismissIntent = Intent(this, FloatingWindowService::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_DISMISS)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        
        return builder
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                AppStringsProvider.current().floatingWindowClose,
                dismissPendingIntent
            )
            .build()
    }
}
