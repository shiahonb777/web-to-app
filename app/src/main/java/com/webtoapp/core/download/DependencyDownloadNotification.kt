package com.webtoapp.core.download

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.webtoapp.core.i18n.AppStringsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 依赖下载专用通知管理器
 * 
 * 通知栏实时显示：
 * - 文件名 + 进度百分比
 * - 已下载 / 总大小
 * - 下载速度
 * - ETA 倒计时
 * - 下载 URL
 * - 暂停/继续 Action 按钮
 */
@SuppressLint("StaticFieldLeak")
class DependencyDownloadNotification private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "DependencyDownloadNotification"
        private const val CHANNEL_ID = "dep_download_channel"
        private val CHANNEL_NAME get() = AppStringsProvider.current().runtimeDownloadChannel
        private const val NOTIFICATION_ID = 8001
        
        const val ACTION_PAUSE = "com.webtoapp.DEP_DOWNLOAD_PAUSE"
        const val ACTION_RESUME = "com.webtoapp.DEP_DOWNLOAD_RESUME"
        
        @Volatile
        private var INSTANCE: DependencyDownloadNotification? = null
        
        fun getInstance(context: Context): DependencyDownloadNotification {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DependencyDownloadNotification(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var receiver: BroadcastReceiver? = null
    
    init {
        createChannel()
        registerReceiver()
        observeEngine()
    }
    
    // ==================== 初始化 ====================
    
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = AppStringsProvider.current().runtimeDownloadChannelDesc
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun registerReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PAUSE -> DependencyDownloadEngine.pause()
                    ACTION_RESUME -> DependencyDownloadEngine.resume()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
    
    // ==================== 状态监听 ====================
    
    private fun observeEngine() {
        scope.launch {
            DependencyDownloadEngine.state.collectLatest { state ->
                when (state) {
                    is DependencyDownloadEngine.State.Downloading -> showProgress(state)
                    is DependencyDownloadEngine.State.Paused -> showPaused(state)
                    is DependencyDownloadEngine.State.Extracting -> showExtracting(state.displayName)
                    is DependencyDownloadEngine.State.Complete -> showComplete()
                    is DependencyDownloadEngine.State.Error -> showError(state.message)
                    else -> dismiss()
                }
            }
        }
    }
    
    // ==================== 通知构建 ====================
    
    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    
    private fun showProgress(dl: DependencyDownloadEngine.State.Downloading) {
        if (!hasPermission()) return
        
        val percent = (dl.progress * 100).toInt()
        val sizeText = "${DependencyDownloadEngine.formatSize(dl.bytesDownloaded)} / ${DependencyDownloadEngine.formatSize(dl.totalBytes)}"
        val speedText = DependencyDownloadEngine.formatSpeed(dl.speedBytesPerSec)
        val etaText = DependencyDownloadEngine.formatEta(dl.etaSeconds)
        val startText = DependencyDownloadEngine.formatTime(dl.startTimeMillis)
        
        // BigText 详情
        val details = buildString {
            append("$sizeText · $speedText · ${AppStringsProvider.current().depDownloadRemaining} $etaText\n")
            append("${AppStringsProvider.current().depDownloadStarted}: $startText\n")
            append(dl.url)
        }
        
        val pauseIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("${dl.displayName}  $percent%")
            .setContentText("$sizeText · $speedText · ${AppStringsProvider.current().depDownloadRemaining} $etaText")
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setProgress(100, percent, dl.totalBytes <= 0)
            .addAction(android.R.drawable.ic_media_pause, AppStringsProvider.current().depDownloadPause, pauseIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showPaused(paused: DependencyDownloadEngine.State.Paused) {
        if (!hasPermission()) return
        
        val percent = (paused.progress * 100).toInt()
        val sizeText = "${DependencyDownloadEngine.formatSize(paused.bytesDownloaded)} / ${DependencyDownloadEngine.formatSize(paused.totalBytes)}"
        
        val resumeIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_RESUME),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val details = buildString {
            append("${AppStringsProvider.current().depDownloadPaused} · $sizeText\n")
            append("${AppStringsProvider.current().depDownloadStarted}: ${DependencyDownloadEngine.formatTime(paused.startTimeMillis)}\n")
            append(paused.url)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentTitle("${paused.displayName}  ${AppStringsProvider.current().depDownloadPaused} $percent%")
            .setContentText("${AppStringsProvider.current().depDownloadPaused} · $sizeText")
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setProgress(100, percent, false)
            .addAction(android.R.drawable.ic_media_play, AppStringsProvider.current().depDownloadResume, resumeIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showExtracting(displayName: String) {
        if (!hasPermission()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("${AppStringsProvider.current().depDownloadExtracting} $displayName")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showComplete() {
        if (!hasPermission()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(AppStringsProvider.current().depDownloadComplete)
            .setContentText(AppStringsProvider.current().depDownloadAllReady)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showError(message: String) {
        if (!hasPermission()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(AppStringsProvider.current().downloadFailed)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
