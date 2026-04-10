package com.webtoapp.core.shell

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.webtoapp.core.logging.AppLogger
import java.io.File

/**
 * 应用内更新管理器
 *
 * 使用 Android DownloadManager 下载 APK 并自动安装，替代原有的跳转浏览器方式。
 * 
 * 特点:
 * 1. 后台下载，不阻塞 UI
 * 2. 系统通知栏显示下载进度
 * 3. 下载完成自动弹出安装界面
 * 4. 支持 Notification Channel (Android 8.0+)
 * 5. 使用 FileProvider 兼容 Android 7.0+
 */
class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val NOTIFICATION_CHANNEL_ID = "app_update_channel"
        private const val APK_FILE_NAME = "app_update.apk"
        private const val NOTIFICATION_ID = 20001
    }

    private var currentDownloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null
    private val downloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    init {
        createNotificationChannel()
    }

    /**
     * 下载并安装 APK
     * 
     * @param downloadUrl APK 下载 URL
     * @param versionName 版本名称（用于通知显示）
     * @param showNotification 是否在通知栏显示进度
     * @param autoInstall 下载完成后是否自动弹出安装
     */
    fun downloadAndInstall(
        downloadUrl: String,
        versionName: String = "",
        showNotification: Boolean = true,
        autoInstall: Boolean = true
    ) {
        try {
            // 清理旧的下载
            cancelCurrentDownload()
            deleteOldApk()

            val uri = Uri.parse(downloadUrl)
            val fileName = "${context.packageName}_update_${System.currentTimeMillis()}.apk"

            val request = DownloadManager.Request(uri).apply {
                setTitle("${getAppName()} 更新")
                setDescription(if (versionName.isNotBlank()) "正在下载 v$versionName..." else "正在下载更新...")
                
                // 下载到应用的外部缓存目录（无需外部存储权限）
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                
                // 允许在移动网络和 WiFi 下载
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                
                // 通知栏显示
                if (showNotification) {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                } else {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                }
                
                setMimeType("application/vnd.android.package-archive")
            }

            currentDownloadId = downloadManager.enqueue(request)
            AppLogger.i(TAG, "APK download started: downloadId=$currentDownloadId, url=$downloadUrl")

            // 注册下载完成广播
            if (autoInstall) {
                registerDownloadReceiver()
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start APK download", e)
        }
    }

    /**
     * 获取当前下载进度
     * @return Triple<bytesDownloaded, totalBytes, status> or null if no download
     */
    fun getDownloadProgress(): Triple<Long, Long, Int>? {
        if (currentDownloadId < 0) return null

        val query = DownloadManager.Query().setFilterById(currentDownloadId)
        downloadManager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )
                val totalBytes = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
                val status = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                )
                return Triple(bytesDownloaded, totalBytes, status)
            }
        }
        return null
    }

    /**
     * 取消当前下载
     */
    fun cancelCurrentDownload() {
        if (currentDownloadId >= 0) {
            try {
                downloadManager.remove(currentDownloadId)
                AppLogger.d(TAG, "Download cancelled: $currentDownloadId")
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to cancel download", e)
            }
            currentDownloadId = -1
        }
        unregisterDownloadReceiver()
    }

    /**
     * 安装 APK
     */
    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                AppLogger.e(TAG, "APK file not found: ${apkFile.absolutePath}")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                // Android 7.0+ 使用 FileProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val authority = "${context.packageName}.fileprovider"
                    val contentUri = FileProvider.getUriForFile(context, authority, apkFile)
                    setDataAndType(contentUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    setDataAndType(
                        Uri.fromFile(apkFile),
                        "application/vnd.android.package-archive"
                    )
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            AppLogger.i(TAG, "Install intent sent for: ${apkFile.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to install APK", e)
            // Fallback: 尝试使用系统安装器
            try {
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = Uri.fromFile(apkFile)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                AppLogger.e(TAG, "Fallback install also failed", e2)
            }
        }
    }

    /**
     * 注册下载完成广播接收器
     */
    private fun registerDownloadReceiver() {
        unregisterDownloadReceiver()

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (downloadId != currentDownloadId) return

                AppLogger.i(TAG, "Download complete: $downloadId")
                
                // 查询下载的文件路径
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                        )
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val localUri = cursor.getString(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                            )
                            val apkFile = File(Uri.parse(localUri).path ?: return)
                            installApk(apkFile)
                        } else {
                            AppLogger.w(TAG, "Download failed with status: $status")
                            showDownloadFailedNotification()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(downloadReceiver, filter)
        }
    }

    /**
     * 注销广播接收器
     */
    private fun unregisterDownloadReceiver() {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
        downloadReceiver = null
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "应用更新",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示应用更新下载进度和安装通知"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * 显示下载失败通知
     */
    private fun showDownloadFailedNotification() {
        try {
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("更新下载失败")
                .setContentText("请检查网络连接后重试")
                .setAutoCancel(true)
                .build()

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to show notification", e)
        }
    }

    /**
     * 删除旧的 APK 下载文件
     */
    private fun deleteOldApk() {
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadDir?.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk")) {
                    file.delete()
                    AppLogger.d(TAG, "Deleted old APK: ${file.name}")
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to clean old APKs", e)
        }
    }

    /**
     * 获取应用名称
     */
    private fun getAppName(): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(context.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "App"
        }
    }

    /**
     * 释放资源
     */
    fun destroy() {
        unregisterDownloadReceiver()
    }
}
