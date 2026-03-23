package com.webtoapp.util

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * 下载通知管理器
 * 
 * 提供增强的下载通知功能：
 * - 实时下载进度显示
 * - 下载完成通知带打开按钮
 * - 下载失败提示
 * - 支持 Blob/Data URL 下载的通知
 * - 支持媒体保存到相册的通知
 */
class DownloadNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DownloadNotification"
        private const val CHANNEL_ID = "download_channel"
        private const val CHANNEL_NAME = "下载通知"
        private const val PROGRESS_NOTIFICATION_ID = 1001
        private const val COMPLETE_NOTIFICATION_ID_BASE = 2000
        private const val CUSTOM_NOTIFICATION_ID_BASE = 3000
        
        @Volatile
        private var INSTANCE: DownloadNotificationManager? = null
        
        fun getInstance(context: Context): DownloadNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadNotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        /**
         * 释放单例（应用退出时调用）
         */
        fun release() {
            synchronized(this) {
                INSTANCE?.cleanup()
                INSTANCE = null
            }
        }
    }
    
    // Custom通知 ID 计数器
    private val customNotificationIdCounter = AtomicInteger(0)
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val handler = Handler(Looper.getMainLooper())
    
    // 跟踪活跃的下载
    private val activeDownloads = mutableMapOf<Long, DownloadInfo>()
    private var progressRunnable: Runnable? = null
    
    // Download完成广播接收器
    private var downloadReceiver: BroadcastReceiver? = null
    
    init {
        createNotificationChannel()
        registerDownloadReceiver()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示文件下载进度和完成通知"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 注册下载完成广播接收器
     */
    private fun registerDownloadReceiver() {
        if (downloadReceiver != null) return
        
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (downloadId != -1L) {
                        handleDownloadComplete(downloadId)
                    }
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(downloadReceiver, filter)
        }
    }

    /**
     * 开始跟踪下载
     */
    fun trackDownload(downloadId: Long, fileName: String, mimeType: String) {
        activeDownloads[downloadId] = DownloadInfo(
            id = downloadId,
            fileName = fileName,
            mimeType = mimeType,
            progress = 0,
            totalBytes = 0,
            downloadedBytes = 0
        )
        
        // Show初始进度通知
        showProgressNotification()
        
        // Start轮询进度
        startProgressPolling()
    }
    
    /**
     * 开始轮询下载进度
     */
    private fun startProgressPolling() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        
        progressRunnable = object : Runnable {
            override fun run() {
                if (activeDownloads.isEmpty()) {
                    notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
                    return
                }
                
                updateDownloadProgress()
                showProgressNotification()
                
                // 每500ms更新一次
                handler.postDelayed(this, 500)
            }
        }
        
        handler.post(progressRunnable!!)
    }
    
    /**
     * 更新下载进度
     */
    private fun updateDownloadProgress() {
        val idsToRemove = mutableListOf<Long>()
        
        activeDownloads.forEach { (downloadId, info) ->
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = downloadManager.query(query)
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val totalIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val downloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    
                    if (statusIndex >= 0 && totalIndex >= 0 && downloadedIndex >= 0) {
                        val status = it.getInt(statusIndex)
                        val totalBytes = it.getLong(totalIndex)
                        val downloadedBytes = it.getLong(downloadedIndex)
                        
                        info.totalBytes = totalBytes
                        info.downloadedBytes = downloadedBytes
                        info.progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else {
                            0
                        }
                        
                        // Check是否完成或失败
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL,
                            DownloadManager.STATUS_FAILED -> {
                                idsToRemove.add(downloadId)
                            }
                        }
                    }
                } else {
                    // Download记录不存在，移除
                    idsToRemove.add(downloadId)
                }
            }
        }
        
        // 移除已完成的下载
        idsToRemove.forEach { activeDownloads.remove(it) }
    }
    
    /**
     * 显示进度通知
     */
    private fun showProgressNotification() {
        if (activeDownloads.isEmpty()) {
            notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
            return
        }
        
        val totalProgress = activeDownloads.values.sumOf { it.progress }
        val avgProgress = totalProgress / activeDownloads.size
        
        val downloadingCount = activeDownloads.size
        val title = if (downloadingCount == 1) {
            "正在下载: ${activeDownloads.values.first().fileName}"
        } else {
            "正在下载 $downloadingCount 个文件"
        }
        
        // 计算总下载速度信息
        val totalDownloaded = activeDownloads.values.sumOf { it.downloadedBytes }
        val totalSize = activeDownloads.values.sumOf { it.totalBytes }
        val progressText = if (totalSize > 0) {
            "${formatFileSize(totalDownloaded)} / ${formatFileSize(totalSize)}"
        } else {
            "${formatFileSize(totalDownloaded)}"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(progressText)
            .setProgress(100, avgProgress, totalSize <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(PROGRESS_NOTIFICATION_ID, notification)
    }

    /**
     * 处理下载完成
     */
    private fun handleDownloadComplete(downloadId: Long) {
        val info = activeDownloads.remove(downloadId)
        
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)
        
        cursor?.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val localUriIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val titleIndex = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val mimeTypeIndex = it.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                
                if (statusIndex >= 0) {
                    val status = it.getInt(statusIndex)
                    val localUri = if (localUriIndex >= 0) it.getString(localUriIndex) else null
                    val title = if (titleIndex >= 0) it.getString(titleIndex) else info?.fileName ?: "文件"
                    val mimeType = if (mimeTypeIndex >= 0) it.getString(mimeTypeIndex) else info?.mimeType ?: "*/*"
                    
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            showDownloadCompleteNotification(downloadId, title, localUri, mimeType)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            showDownloadFailedNotification(downloadId, title)
                        }
                    }
                }
            }
        }
        
        // 如果没有活跃下载，取消进度通知
        if (activeDownloads.isEmpty()) {
            notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
        }
    }
    
    /**
     * 显示下载完成通知
     */
    private fun showDownloadCompleteNotification(
        downloadId: Long,
        fileName: String,
        localUri: String?,
        mimeType: String
    ) {
        val notificationId = (COMPLETE_NOTIFICATION_ID_BASE + downloadId).toInt()
        
        // Create打开文件的 Intent
        val openIntent = createOpenFileIntent(localUri, mimeType)
        val openPendingIntent = if (openIntent != null) {
            PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        
        // Create分享文件的 Intent
        val shareIntent = createShareFileIntent(localUri, mimeType)
        val sharePendingIntent = if (shareIntent != null) {
            PendingIntent.getActivity(
                context,
                notificationId + 10000,
                Intent.createChooser(shareIntent, "分享文件"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("下载完成")
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        // 添加打开按钮
        if (openPendingIntent != null) {
            builder.setContentIntent(openPendingIntent)
            builder.addAction(
                android.R.drawable.ic_menu_view,
                "Open",
                openPendingIntent
            )
        }
        
        // 添加分享按钮
        if (sharePendingIntent != null) {
            builder.addAction(
                android.R.drawable.ic_menu_share,
                "Share",
                sharePendingIntent
            )
        }
        
        notificationManager.notify(notificationId, builder.build())
        
        Log.d(TAG, "下载完成通知已显示: $fileName")
    }
    
    /**
     * 显示下载失败通知
     */
    private fun showDownloadFailedNotification(downloadId: Long, fileName: String) {
        val notificationId = (COMPLETE_NOTIFICATION_ID_BASE + downloadId).toInt()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("下载失败")
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(notificationId, notification)
        
        Log.d(TAG, "下载失败通知已显示: $fileName")
    }
    
    /**
     * 创建打开文件的 Intent
     */
    private fun createOpenFileIntent(localUri: String?, mimeType: String): Intent? {
        if (localUri.isNullOrBlank()) return null
        
        return try {
            val uri = Uri.parse(localUri)
            val file = when {
                uri.scheme == "file" -> File(uri.path ?: return null)
                uri.scheme == "content" -> {
                    // 对于 content:// URI，直接使用
                    return Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                else -> return null
            }
            
            if (!file.exists()) return null
            
            // 使用 FileProvider 获取安全的 URI
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建打开文件 Intent 失败", e)
            null
        }
    }
    
    /**
     * 创建分享文件的 Intent
     */
    private fun createShareFileIntent(localUri: String?, mimeType: String): Intent? {
        if (localUri.isNullOrBlank()) return null
        
        return try {
            val uri = Uri.parse(localUri)
            val contentUri = when {
                uri.scheme == "file" -> {
                    val file = File(uri.path ?: return null)
                    if (!file.exists()) return null
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                uri.scheme == "content" -> uri
                else -> return null
            }
            
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建分享文件 Intent 失败", e)
            null
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // 忽略
            }
        }
        downloadReceiver = null
        activeDownloads.clear()
    }
    
    /**
     * 下载信息
     */
    private data class DownloadInfo(
        val id: Long,
        val fileName: String,
        val mimeType: String,
        var progress: Int,
        var totalBytes: Long,
        var downloadedBytes: Long
    )
    
    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * 显示不确定进度的下载通知（用于 Blob/Data URL 下载）
     * @return 通知 ID，用于后续更新或取消
     */
    fun showIndeterminateProgress(fileName: String): Int {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "没有通知权限，跳过显示进度通知")
            return -1
        }
        
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在保存")
            .setContentText(fileName)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(notificationId, notification)
        return notificationId
    }
    
    /**
     * 显示确定进度的下载通知
     * @return 通知 ID
     */
    fun showProgress(fileName: String, progress: Int, totalBytes: Long, downloadedBytes: Long): Int {
        if (!hasNotificationPermission()) {
            return -1
        }
        
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        
        val progressText = if (totalBytes > 0) {
            "${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}"
        } else {
            formatFileSize(downloadedBytes)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在下载")
            .setContentText("$fileName - $progressText")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(notificationId, notification)
        return notificationId
    }
    
    /**
     * 更新进度通知
     */
    fun updateProgress(notificationId: Int, fileName: String, progress: Int, totalBytes: Long, downloadedBytes: Long) {
        if (notificationId < 0 || !hasNotificationPermission()) return
        
        val progressText = if (totalBytes > 0) {
            "${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}"
        } else {
            formatFileSize(downloadedBytes)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在下载")
            .setContentText("$fileName - $progressText")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * 取消进度通知
     */
    fun cancelNotification(notificationId: Int) {
        if (notificationId >= 0) {
            notificationManager.cancel(notificationId)
        }
    }
    
    /**
     * 显示文件保存完成通知（通用方法）
     * @param fileName 文件名
     * @param filePath 文件路径（可以是 file:// 或 content:// URI）
     * @param mimeType MIME 类型
     * @param progressNotificationId 之前显示的进度通知 ID，会被取消
     */
    fun showSaveComplete(
        fileName: String,
        filePath: String?,
        mimeType: String,
        progressNotificationId: Int = -1
    ) {
        // Cancel进度通知
        if (progressNotificationId >= 0) {
            notificationManager.cancel(progressNotificationId)
        }
        
        if (!hasNotificationPermission()) {
            Log.w(TAG, "没有通知权限，跳过显示完成通知")
            return
        }
        
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        
        // Create打开文件的 Intent
        val openIntent = createOpenFileIntent(filePath, mimeType)
        val openPendingIntent = if (openIntent != null) {
            PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        
        // Create分享文件的 Intent
        val shareIntent = createShareFileIntent(filePath, mimeType)
        val sharePendingIntent = if (shareIntent != null) {
            PendingIntent.getActivity(
                context,
                notificationId + 10000,
                Intent.createChooser(shareIntent, "分享文件"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Save completed")
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        // 添加打开按钮
        if (openPendingIntent != null) {
            builder.setContentIntent(openPendingIntent)
            builder.addAction(
                android.R.drawable.ic_menu_view,
                "Open",
                openPendingIntent
            )
        }
        
        // 添加分享按钮
        if (sharePendingIntent != null) {
            builder.addAction(
                android.R.drawable.ic_menu_share,
                "Share",
                sharePendingIntent
            )
        }
        
        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "保存完成通知已显示: $fileName")
    }
    
    /**
     * 显示媒体保存到相册完成通知
     */
    fun showMediaSaveComplete(
        fileName: String,
        uri: Uri,
        mimeType: String,
        isImage: Boolean,
        progressNotificationId: Int = -1
    ) {
        // Cancel进度通知
        if (progressNotificationId >= 0) {
            notificationManager.cancel(progressNotificationId)
        }
        
        if (!hasNotificationPermission()) {
            return
        }
        
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        val typeText = if (isImage) "图片" else "视频"
        
        // Create打开文件的 Intent
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create分享 Intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val sharePendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 10000,
            Intent.createChooser(shareIntent, "分享$typeText"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("${typeText}已保存到相册")
            .setContentText(fileName)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "查看", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "${typeText}保存完成通知已显示: $fileName")
    }
    
    /**
     * 显示保存失败通知
     */
    fun showSaveFailed(
        fileName: String,
        errorMessage: String,
        progressNotificationId: Int = -1
    ) {
        // Cancel进度通知
        if (progressNotificationId >= 0) {
            notificationManager.cancel(progressNotificationId)
        }
        
        if (!hasNotificationPermission()) {
            return
        }
        
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Save failed")
            .setContentText("$fileName: $errorMessage")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "保存失败通知已显示: $fileName - $errorMessage")
    }
    
    /**
     * 根据文件路径获取 MIME 类型
     */
    private fun getMimeTypeFromPath(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }
}
