package com.webtoapp.core.cloud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit







class AppDownloadManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppDownloadManager"
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_BASE_ID = 50000

        @Volatile
        private var instance: AppDownloadManager? = null

        fun getInstance(context: Context): AppDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: AppDownloadManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    data class DownloadTask(
        val appId: Int,
        val appName: String,
        val url: String,
        val progress: Float = 0f,
        val downloadedBytes: Long = 0,
        val totalBytes: Long = -1,
        val speed: Long = 0,
        val status: DownloadStatus = DownloadStatus.PENDING,
        val filePath: String? = null,
        val error: String? = null
    )

    enum class DownloadStatus {
        PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
    }

    data class DownloadedApp(
        val appId: Int,
        val appName: String,
        val filePath: String,
        val fileSize: Long,
        val downloadedAt: Long
    )

    private val _activeTasks = MutableStateFlow<Map<Int, DownloadTask>>(emptyMap())
    val activeTasks: StateFlow<Map<Int, DownloadTask>> = _activeTasks.asStateFlow()

    private val _downloadedApps = MutableStateFlow<List<DownloadedApp>>(emptyList())
    val downloadedApps: StateFlow<List<DownloadedApp>> = _downloadedApps.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "store_apks").also {
            it.mkdirs()
        }
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannel()
        scanDownloadedApps()
    }


    val activeDownloadCount: Int
        get() = _activeTasks.value.count {
            it.value.status == DownloadStatus.DOWNLOADING || it.value.status == DownloadStatus.PENDING
        }


    suspend fun startDownload(appId: Int, appName: String, url: String) {

        val existing = _activeTasks.value[appId]
        if (existing != null && existing.status == DownloadStatus.DOWNLOADING) return

        val task = DownloadTask(
            appId = appId,
            appName = appName,
            url = url,
            status = DownloadStatus.DOWNLOADING
        )
        _activeTasks.update { it + (appId to task) }


        showProgressNotification(appId, appName, 0, true)

        withContext(Dispatchers.IO) {
            try {
                val fileName = "${appName.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fff]"), "_")}_${appId}.apk"
                val file = File(downloadDir, fileName)

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    updateTask(appId) { copy(status = DownloadStatus.FAILED, error = "HTTP ${response.code}") }
                    showFailedNotification(appId, appName)
                    return@withContext
                }

                val body = response.body ?: run {
                    updateTask(appId) { copy(status = DownloadStatus.FAILED, error = "Empty response") }
                    showFailedNotification(appId, appName)
                    return@withContext
                }

                val totalBytes = body.contentLength()
                updateTask(appId) { copy(totalBytes = totalBytes) }

                var downloadedBytes = 0L
                var lastTime = System.currentTimeMillis()
                var lastBytes = 0L
                var lastNotifyTime = 0L

                FileOutputStream(file).use { fos ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {

                            val currentTask = _activeTasks.value[appId]
                            if (currentTask == null) {

                                file.delete()
                                cancelNotification(appId)
                                return@withContext
                            }

                            fos.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead


                            val now = System.currentTimeMillis()
                            val elapsed = now - lastTime
                            val speed = if (elapsed > 500) {
                                val s = ((downloadedBytes - lastBytes) * 1000) / elapsed
                                lastTime = now
                                lastBytes = downloadedBytes
                                s
                            } else {
                                _activeTasks.value[appId]?.speed ?: 0
                            }

                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                            updateTask(appId) {
                                copy(
                                    progress = progress,
                                    downloadedBytes = downloadedBytes,
                                    speed = speed
                                )
                            }


                            if (now - lastNotifyTime > 500) {
                                showProgressNotification(appId, appName, (progress * 100).toInt(), totalBytes <= 0)
                                lastNotifyTime = now
                            }
                        }
                    }
                }


                updateTask(appId) {
                    copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 1f,
                        filePath = file.absolutePath,
                        downloadedBytes = downloadedBytes
                    )
                }


                _downloadedApps.update { list ->
                    list.filterNot { it.appId == appId } + DownloadedApp(
                        appId = appId,
                        appName = appName,
                        filePath = file.absolutePath,
                        fileSize = file.length(),
                        downloadedAt = System.currentTimeMillis()
                    )
                }

                showCompleteNotification(appId, appName, file.absolutePath)
                AppLogger.i(TAG, "Download complete: $appName → ${file.absolutePath}")

            } catch (e: Exception) {
                AppLogger.e(TAG, "Download failed: $appName", e)
                updateTask(appId) {
                    copy(status = DownloadStatus.FAILED, error = e.message ?: "未知错误")
                }
                showFailedNotification(appId, appName)
            }
        }
    }


    fun cancelDownload(appId: Int) {
        _activeTasks.update { it - appId }
        cancelNotification(appId)
    }


    fun dismissTask(appId: Int) {
        _activeTasks.update { it - appId }
        cancelNotification(appId)
    }


    fun deleteDownloadedApp(appId: Int) {
        val app = _downloadedApps.value.find { it.appId == appId } ?: return
        File(app.filePath).delete()
        _downloadedApps.update { it.filterNot { a -> a.appId == appId } }
    }


    fun installApk(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Install APK failed", e)
        }
    }





    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                Strings.notifDownloadChannelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = Strings.notifDownloadChannelDesc
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(appId: Int, appName: String, progress: Int, indeterminate: Boolean) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(Strings.notifDownloading.format(appName))
            .setContentText(if (indeterminate) Strings.notifPreparing else "$progress%")
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(NOTIFICATION_BASE_ID + appId, notification)
    }

    private fun showCompleteNotification(appId: Int, appName: String, filePath: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(Strings.notifDownloadComplete.format(appName))
            .setContentText(Strings.notifClickToInstall)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(NOTIFICATION_BASE_ID + appId, notification)
    }

    private fun showFailedNotification(appId: Int, appName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(Strings.notifDownloadFailed.format(appName))
            .setContentText(Strings.notifRetry)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(NOTIFICATION_BASE_ID + appId, notification)
    }

    private fun cancelNotification(appId: Int) {
        notificationManager.cancel(NOTIFICATION_BASE_ID + appId)
    }






    private fun scanDownloadedApps() {
        val files = downloadDir.listFiles { f -> f.extension == "apk" } ?: return
        val apps = files.map { file ->
            val name = file.nameWithoutExtension
            val idStr = name.substringAfterLast("_", "0")
            DownloadedApp(
                appId = idStr.toIntOrNull() ?: 0,
                appName = name.substringBeforeLast("_"),
                filePath = file.absolutePath,
                fileSize = file.length(),
                downloadedAt = file.lastModified()
            )
        }
        _downloadedApps.value = apps.sortedByDescending { it.downloadedAt }
    }

    private fun updateTask(appId: Int, update: DownloadTask.() -> DownloadTask) {
        _activeTasks.update { map ->
            val current = map[appId] ?: return@update map
            map + (appId to current.update())
        }
    }

    fun formatSpeed(bytesPerSec: Long): String = when {
        bytesPerSec >= 1_048_576 -> String.format("%.1f MB/s", bytesPerSec / 1_048_576.0)
        bytesPerSec >= 1_024 -> String.format("%.0f KB/s", bytesPerSec / 1_024.0)
        else -> "$bytesPerSec B/s"
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.0f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}
