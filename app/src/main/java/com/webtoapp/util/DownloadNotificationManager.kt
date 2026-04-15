package com.webtoapp.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.webtoapp.core.logging.AppLogger
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.webtoapp.core.i18n.AppStringsProvider
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Download notification manager
 * 
 * ：
 * -
 * -
 * -
 * - Blob/Data URL
 * -
 */
@SuppressLint("StaticFieldLeak")
class DownloadNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DownloadNotification"
        private const val CHANNEL_ID = "download_channel"
        private val CHANNEL_NAME get() = AppStringsProvider.current().notifDownloadChannel
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
         * （）
         */
        fun release() {
            synchronized(this) {
                INSTANCE?.cleanup()
                INSTANCE = null
            }
        }
    }
    
    // Custom notification ID counter
    private val customNotificationIdCounter = AtomicInteger(0)
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val handler = Handler(Looper.getMainLooper())
    
    // Track active downloads
    private val activeDownloads = mutableMapOf<Long, DownloadInfo>()
    private var progressRunnable: Runnable? = null
    
    // Download complete broadcast receiver
    private var downloadReceiver: BroadcastReceiver? = null
    
    init {
        createNotificationChannel()
        registerDownloadReceiver()
    }
    
    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = AppStringsProvider.current().notifDownloadChannelDesc
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Register download complete receiver
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
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Start download tracking
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
        
        // Show
        showProgressNotification()
        
        // Start
        startProgressPolling()
    }
    
    /**
     * Start polling download progress
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
                
                // Update every 500ms
                handler.postDelayed(this, 500)
            }
        }
        
        handler.post(progressRunnable!!)
    }
    
    /**
     * Update download progress
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
                        
                        // Check
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL,
                            DownloadManager.STATUS_FAILED -> {
                                idsToRemove.add(downloadId)
                            }
                        }
                    }
                } else {
                    // Download，
                    idsToRemove.add(downloadId)
                }
            }
        }
        
        // Note.
        idsToRemove.forEach { activeDownloads.remove(it) }
    }
    
    /**
     * Show progress notification
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
            "Downloading: ${activeDownloads.values.first().fileName}"
        } else {
            "Downloading $downloadingCount files"
        }
        
        // Note.
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
     * Handle download completion
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
                    val title = if (titleIndex >= 0) it.getString(titleIndex) else info?.fileName ?: "File"
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
        
        // Cancel progress notification if no active downloads
        if (activeDownloads.isEmpty()) {
            notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
        }
    }
    
    /**
     * Show download complete notification
     */
    private fun showDownloadCompleteNotification(
        downloadId: Long,
        fileName: String,
        localUri: String?,
        mimeType: String
    ) {
        val notificationId = (COMPLETE_NOTIFICATION_ID_BASE + downloadId).toInt()
        
        // Create Intent
        val openIntent = createOpenFileIntent(localUri, mimeType)
        val openPendingIntent = if (openIntent != null) {
            PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        
        // Create Intent
        val shareIntent = createShareFileIntent(localUri, mimeType)
        val sharePendingIntent = if (shareIntent != null) {
            PendingIntent.getActivity(
                context,
                notificationId + 10000,
                Intent.createChooser(shareIntent, AppStringsProvider.current().notifShare),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(AppStringsProvider.current().notifDownloadComplete)
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        // Add open action
        if (openPendingIntent != null) {
            builder.setContentIntent(openPendingIntent)
            builder.addAction(
                android.R.drawable.ic_menu_view,
                AppStringsProvider.current().notifOpen,
                openPendingIntent
            )
        }
        
        // Add share action
        if (sharePendingIntent != null) {
            builder.addAction(
                android.R.drawable.ic_menu_share,
                AppStringsProvider.current().notifShare,
                sharePendingIntent
            )
        }
        
        notificationManager.notify(notificationId, builder.build())
    }
    
    /**
     * Show download failure notification
     */
    private fun showDownloadFailedNotification(downloadId: Long, fileName: String) {
        val notificationId = (COMPLETE_NOTIFICATION_ID_BASE + downloadId).toInt()
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(AppStringsProvider.current().notifDownloadFailed)
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        notificationManager.notify(notificationId, builder.build())
    }
    
    /**
     * Create open-file intent
     */
    private fun createOpenFileIntent(localUri: String?, mimeType: String): Intent? {
        if (localUri == null) return null
        return try {
            val uri = Uri.parse(localUri)
            val fileUri = if (uri.scheme == "file") {
                val file = File(uri.path ?: return null)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                uri
            }
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to create open intent", e)
            null
        }
    }
    
    /**
     * Create share intent
     */
    private fun createShareFileIntent(localUri: String?, mimeType: String): Intent? {
        if (localUri == null) return null
        return try {
            val uri = Uri.parse(localUri)
            val fileUri = if (uri.scheme == "file") {
                val file = File(uri.path ?: return null)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                uri
            }
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to create share intent", e)
            null
        }
    }
    
    /**
     * Show indeterminate progress notification
     * @return result
     */
    fun showIndeterminateProgress(fileName: String): Int {
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(AppStringsProvider.current().notifDownloading)
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
     * Update download progress
     */
    fun updateProgress(notificationId: Int, fileName: String, progress: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName)
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Show media save complete notification
     */
    fun showMediaSaveComplete(
        fileName: String,
        uri: android.net.Uri,
        mimeType: String,
        isImage: Boolean,
        progressNotificationId: Int
    ) {
        // Note.
        notificationManager.cancel(progressNotificationId)
        
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        val typeText = if (isImage) AppStringsProvider.current().image else AppStringsProvider.current().video
        
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(AppStringsProvider.current().savedToGallery.replace("%s", typeText))
            .setContentText(fileName)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Show file save complete notification
     */
    fun showSaveComplete(
        fileName: String,
        filePath: String,
        mimeType: String,
        progressNotificationId: Int
    ) {
        notificationManager.cancel(progressNotificationId)
        
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        
        val openIntent = createOpenFileIntent("file://$filePath", mimeType)
        val pendingIntent = if (openIntent != null) {
            PendingIntent.getActivity(
                context, notificationId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(AppStringsProvider.current().notifDownloadComplete)
            .setContentText(fileName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        
        notificationManager.notify(notificationId, builder.build())
    }
    
    /**
     * Show save failure notification
     */
    fun showSaveFailed(fileName: String, reason: String, progressNotificationId: Int) {
        notificationManager.cancel(progressNotificationId)
        
        val notificationId = CUSTOM_NOTIFICATION_ID_BASE + customNotificationIdCounter.incrementAndGet()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(AppStringsProvider.current().notifDownloadFailed)
            .setContentText("$fileName: $reason")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Format file size
     */
    private fun formatFileSize(bytes: Long): String {
        return android.text.format.Formatter.formatFileSize(context, bytes)
    }
    
    /**
     * Clean up resources
     */
    private fun cleanup() {
        try {
            progressRunnable?.let { handler.removeCallbacks(it) }
            progressRunnable = null
            downloadReceiver?.let {
                context.unregisterReceiver(it)
            }
            downloadReceiver = null
            activeDownloads.clear()
            notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cleanup failed", e)
        }
    }
    
    /**
     * Download info
     */
    data class DownloadInfo(
        val id: Long,
        val fileName: String,
        val mimeType: String,
        var progress: Int = 0,
        var totalBytes: Long = 0,
        var downloadedBytes: Long = 0
    )
}
