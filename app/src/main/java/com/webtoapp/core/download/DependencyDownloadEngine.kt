package com.webtoapp.core.download

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.webtoapp.core.network.NetworkModule
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean











object DependencyDownloadEngine {

    private const val TAG = "DependencyDownloadEngine"


    private const val SPEED_WINDOW_MS = 3000L


    private const val THROTTLE_MS = 500L


    private const val PAUSE_CHECK_MS = 200L



    sealed class State {
        object Idle : State()

        data class Downloading(

            val url: String,

            val displayName: String,

            val fileName: String,

            val bytesDownloaded: Long,

            val totalBytes: Long,

            val progress: Float,

            val speedBytesPerSec: Long,

            val etaSeconds: Long,

            val startTimeMillis: Long,

            val isPaused: Boolean
        ) : State()

        data class Extracting(val displayName: String) : State()
        data class Verifying(val displayName: String) : State()
        object Complete : State()
        data class Error(val message: String, val retryable: Boolean = true) : State()


        data class Paused(
            val url: String,
            val displayName: String,
            val fileName: String,
            val bytesDownloaded: Long,
            val totalBytes: Long,
            val progress: Float,
            val startTimeMillis: Long
        ) : State()
    }

    val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state


    private val _paused = AtomicBoolean(false)


    val isActive: Boolean get() = _state.value is State.Downloading || _state.value is State.Paused




    private const val USER_AGENT = "WebToApp/1.0 (Android; DependencyDownloadEngine)"

    private val httpClient: OkHttpClient by lazy {
        NetworkModule.customClient {
            addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                )
            }
        }
    }




    private val speedSamples = mutableListOf<Pair<Long, Long>>()

    private fun recordSample(totalDownloaded: Long) {
        val now = System.currentTimeMillis()
        speedSamples.add(now to totalDownloaded)

        speedSamples.removeAll { now - it.first > SPEED_WINDOW_MS }
    }

    private fun calculateSpeed(): Long {
        if (speedSamples.size < 2) return 0L
        val oldest = speedSamples.first()
        val newest = speedSamples.last()
        val timeDelta = newest.first - oldest.first
        if (timeDelta <= 0) return 0L
        val bytesDelta = newest.second - oldest.second
        return (bytesDelta * 1000 / timeDelta).coerceAtLeast(0)
    }

    private fun calculateEta(remaining: Long, speed: Long): Long {
        if (speed <= 0 || remaining <= 0) return -1
        return remaining / speed
    }






    fun pause() {
        if (_state.value is State.Downloading) {
            _paused.set(true)
            val dl = _state.value as? State.Downloading ?: return
            _state.value = State.Paused(
                url = dl.url,
                displayName = dl.displayName,
                fileName = dl.fileName,
                bytesDownloaded = dl.bytesDownloaded,
                totalBytes = dl.totalBytes,
                progress = dl.progress,
                startTimeMillis = dl.startTimeMillis
            )
            AppLogger.i(TAG, "下载已暂停: ${dl.displayName}")
        }
    }




    fun resume() {
        _paused.set(false)
        AppLogger.i(TAG, "下载已继续")
    }




    fun reset() {
        _paused.set(false)
        speedSamples.clear()
        _state.value = State.Idle
    }













    suspend fun downloadFile(
        url: String,
        destFile: File,
        displayName: String,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val fileName = url.substringAfterLast("/")
        val tempFile = File(destFile.parentFile, "${destFile.name}.tmp")
        var downloadedBytes = 0L
        val startTime = System.currentTimeMillis()

        speedSamples.clear()
        _paused.set(false)

        try {

            if (tempFile.exists()) {
                downloadedBytes = tempFile.length()
            }

            val requestBuilder = Request.Builder().url(url)
            if (downloadedBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
                AppLogger.i(TAG, "断点续传: 从 $downloadedBytes 字节继续 ($displayName)")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 206) {
                AppLogger.e(TAG, "下载失败: HTTP ${response.code} - $url")
                _state.value = State.Error(Strings.downloadFailedHttp.replace("%d", response.code.toString()))
                response.close()
                return@withContext false
            }

            val body = response.body ?: run {
                _state.value = State.Error(Strings.downloadReturnedEmpty)
                response.close()
                return@withContext false
            }

            val contentLength = body.contentLength()
            val totalBytes = if (response.code == 206) {
                downloadedBytes + contentLength
            } else {
                contentLength
            }

            val outputStream = if (response.code == 206) {
                FileOutputStream(tempFile, true)
            } else {
                downloadedBytes = 0
                FileOutputStream(tempFile)
            }

            var lastThrottleTime = 0L

            outputStream.use { fos ->
                val buffer = ByteArray(8192)
                val inputStream = body.byteStream()
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {

                    while (_paused.get()) {
                        delay(PAUSE_CHECK_MS)
                        if (!isActive) return@withContext false
                    }

                    fos.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead


                    recordSample(downloadedBytes)


                    val now = System.currentTimeMillis()
                    if (now - lastThrottleTime >= THROTTLE_MS) {
                        lastThrottleTime = now

                        val speed = calculateSpeed()
                        val remaining = if (totalBytes > 0) totalBytes - downloadedBytes else -1
                        val eta = calculateEta(remaining, speed)
                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else 0f

                        _state.value = State.Downloading(
                            url = url,
                            displayName = displayName,
                            fileName = fileName,
                            bytesDownloaded = downloadedBytes,
                            totalBytes = totalBytes,
                            progress = progress,
                            speedBytesPerSec = speed,
                            etaSeconds = eta,
                            startTimeMillis = startTime,
                            isPaused = false
                        )
                    }
                }
            }


            tempFile.renameTo(destFile)
            AppLogger.i(TAG, "$displayName 下载完成: ${destFile.length()} 字节")
            true

        } catch (e: Exception) {
            AppLogger.e(TAG, "下载 $displayName 失败", e)
            _state.value = State.Error(Strings.downloadNameFailed.replaceFirst("%s", displayName).replaceFirst("%s", e.message ?: ""))
            false
        }
    }



    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec B/s"
            bytesPerSec < 1024 * 1024 -> "${bytesPerSec / 1024} KB/s"
            else -> String.format(java.util.Locale.getDefault(), "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
        }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 0 -> Strings.sizeUnknown
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024L * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(java.util.Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun formatEta(seconds: Long): String {
        if (seconds < 0) return "--:--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        } else {
            String.format(java.util.Locale.getDefault(), "%d:%02d", m, s)
        }
    }

    fun formatTime(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}
