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

/**
 * 统一依赖下载引擎
 * 
 * 提供：
 * - 暂停/继续（AtomicBoolean 标志位软暂停 + 断点续传硬恢复）
 * - 实时速度计算（3 秒滑动窗口采样）
 * - ETA 倒计时
 * - 丰富的下载状态（URL、文件名、大小、速度、ETA、开始时间）
 * - 系统通知栏实时进度
 */
object DependencyDownloadEngine {
    
    private const val TAG = "DependencyDownloadEngine"
    
    /** 速度采样窗口（毫秒） */
    private const val SPEED_WINDOW_MS = 3000L
    
    /** 通知 / StateFlow 更新节流间隔（毫秒） */
    private const val THROTTLE_MS = 500L
    
    /** 暂停循环时的检查间隔（毫秒） */
    private const val PAUSE_CHECK_MS = 200L
    
    // ==================== 下载状态 ====================
    
    sealed class State {
        object Idle : State()
        
        data class Downloading(
            /** 当前下载 URL */
            val url: String,
            /** 显示名称（如 "PHP 8.4 (arm64-v8a)"） */
            val displayName: String,
            /** 原始文件名 */
            val fileName: String,
            /** 已下载字节数 */
            val bytesDownloaded: Long,
            /** 文件总字节数（-1 表示未知） */
            val totalBytes: Long,
            /** 百分比进度 0..1 */
            val progress: Float,
            /** 实时下载速度（字节/秒） */
            val speedBytesPerSec: Long,
            /** 预计剩余秒数（-1 表示未知） */
            val etaSeconds: Long,
            /** 下载开始时间戳（System.currentTimeMillis） */
            val startTimeMillis: Long,
            /** 是否暂停中 */
            val isPaused: Boolean
        ) : State()
        
        data class Extracting(val displayName: String) : State()
        data class Verifying(val displayName: String) : State()
        object Complete : State()
        data class Error(val message: String, val retryable: Boolean = true) : State()
        
        /** 暂停态 — 保留暂停前的快照 */
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
    
    /** 暂停标志位 */
    private val _paused = AtomicBoolean(false)
    
    /** 当前是否有下载在进行 */
    val isActive: Boolean get() = _state.value is State.Downloading || _state.value is State.Paused
    
    // ==================== OkHttp 客户端 ====================
    
    /** 下载用 User-Agent（wordpress.org 等服务器会拦截 OkHttp 默认 UA） */
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
    
    // ==================== 速度计算 ====================
    
    /** 采样点：(timestamp, cumulative bytes) */
    private val speedSamples = mutableListOf<Pair<Long, Long>>()
    
    private fun recordSample(totalDownloaded: Long) {
        val now = System.currentTimeMillis()
        speedSamples.add(now to totalDownloaded)
        // 清理超过窗口外的旧采样
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
    
    // ==================== 公开 API ====================
    
    /**
     * 暂停当前下载
     */
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
    
    /**
     * 继续下载（取消暂停标志，下载循环会自动恢复）
     */
    fun resume() {
        _paused.set(false)
        AppLogger.i(TAG, "下载已继续")
    }
    
    /**
     * 重置为空闲状态
     */
    fun reset() {
        _paused.set(false)
        speedSamples.clear()
        _state.value = State.Idle
    }
    
    /**
     * 下载文件（核心方法）
     * 
     * 支持断点续传、暂停/继续、实时速度/ETA。
     * 通知栏更新由调用方通过 collect [state] 后转发给 DependencyDownloadNotification。
     * 
     * @param url 下载地址
     * @param destFile 最终保存文件
     * @param displayName 界面显示名
     * @param context 用于通知（可选）
     * @return true 下载成功
     */
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
            // 检查已有的临时文件（断点续传）
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
                FileOutputStream(tempFile, true) // 追加
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
                    // 暂停检查 — 循环等待直到取消暂停
                    while (_paused.get()) {
                        delay(PAUSE_CHECK_MS)
                        if (!isActive) return@withContext false
                    }
                    
                    fos.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    
                    // 记录采样
                    recordSample(downloadedBytes)
                    
                    // 节流更新状态
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
            
            // 下载完成，重命名临时文件
            tempFile.renameTo(destFile)
            AppLogger.i(TAG, "$displayName 下载完成: ${destFile.length()} 字节")
            true
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载 $displayName 失败", e)
            _state.value = State.Error(Strings.downloadNameFailed.replaceFirst("%s", displayName).replaceFirst("%s", e.message ?: ""))
            false
        }
    }
    
    // ==================== 格式化工具 ====================
    
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
