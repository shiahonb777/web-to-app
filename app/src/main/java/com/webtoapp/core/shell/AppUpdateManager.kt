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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import java.io.File













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









    fun downloadAndInstall(
        downloadUrl: String,
        versionName: String = "",
        showNotification: Boolean = true,
        autoInstall: Boolean = true
    ) {
        try {

            cancelCurrentDownload()
            deleteOldApk()

            val uri = Uri.parse(downloadUrl)
            val fileName = "${context.packageName}_update_${System.currentTimeMillis()}.apk"

            val request = DownloadManager.Request(uri).apply {
                setTitle("${getAppName()} 更新")
                setDescription(if (versionName.isNotBlank()) "正在下载 v$versionName..." else "正在下载更新...")


                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)


                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )


                if (showNotification) {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                } else {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                }

                setMimeType("application/vnd.android.package-archive")
            }

            currentDownloadId = downloadManager.enqueue(request)
            AppLogger.i(TAG, "APK download started: downloadId=$currentDownloadId, url=$downloadUrl")


            if (autoInstall) {
                registerDownloadReceiver()
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start APK download", e)
        }
    }





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




    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                AppLogger.e(TAG, "APK file not found: ${apkFile.absolutePath}")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {

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




    private fun registerDownloadReceiver() {
        unregisterDownloadReceiver()

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (downloadId != currentDownloadId) return

                AppLogger.i(TAG, "Download complete: $downloadId")


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
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }




    private fun unregisterDownloadReceiver() {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {

            }
        }
        downloadReceiver = null
    }




    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                Strings.notifUpdateChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = Strings.notifUpdateChannelDesc
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }




    private fun showDownloadFailedNotification() {
        try {
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(Strings.notifUpdateDownloadFailed)
                .setContentText(Strings.errorCheckNetworkRetry)
                .setAutoCancel(true)
                .build()

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to show notification", e)
        }
    }




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




    private fun getAppName(): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(context.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "App"
        }
    }




    fun destroy() {
        unregisterDownloadReceiver()
    }
}
