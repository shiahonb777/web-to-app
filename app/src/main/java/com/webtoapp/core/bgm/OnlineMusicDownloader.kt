package com.webtoapp.core.bgm

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.BgmItem
import com.webtoapp.util.BgmStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.webtoapp.core.network.NetworkModule
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Note
 * Note
 */
object OnlineMusicDownloader {
    
    private const val TAG = "OnlineMusicDownloader"
    
    // Pre-compiled regex for safe file names
    private val SAFE_NAME_REGEX = Regex("[^a-zA-Z0-9\u4e00-\u9fa5_-]")
    
    private val client get() = NetworkModule.downloadClient
    
    /**
 * Note
     * @param context Context
 * Note
 * Note
 * Note
     */
    suspend fun downloadMusic(
        context: Context,
        track: OnlineMusicTrack,
        onProgress: ((Float) -> Unit)? = null
    ): BgmItem? {
        return withContext(Dispatchers.IO) {
            try {
                val bgmDir = BgmStorage.getBgmDir(context)
                val safeName = generateSafeFileName(track.name, track.id)
                
 Note
                val playUrl = track.playUrl
                if (playUrl.isNullOrBlank()) {
                    AppLogger.e(TAG, "无播放链接")
                    return@withContext null
                }
                
 Note
                val ext = detectExtension(playUrl)
                val musicFile = File(bgmDir, "$safeName.$ext")
                
 Note
                if (musicFile.exists() && musicFile.length() > 0) {
                    AppLogger.i(TAG, "音乐文件已存在: ${musicFile.absolutePath}")
                    return@withContext createBgmItem(track, musicFile, bgmDir, safeName)
                }

                
 Note
                AppLogger.i(TAG, "开始下载音乐: $playUrl")
                val downloadSuccess = downloadFile(playUrl, musicFile) { progress ->
                    onProgress?.invoke(progress * 0.8f)
                }
                
                if (!downloadSuccess) {
                    AppLogger.e(TAG, "音乐下载失败")
                    return@withContext null
                }
                
 Note
                var coverFile: File? = null
                if (!track.coverUrl.isNullOrBlank()) {
                    onProgress?.invoke(0.85f)
                    coverFile = File(bgmDir, "$safeName.jpg")
                    val coverSuccess = downloadFile(track.coverUrl, coverFile) { progress ->
                        onProgress?.invoke(0.8f + progress * 0.2f)
                    }
                    if (!coverSuccess) {
                        AppLogger.w(TAG, "封面下载失败，继续使用无封面")
                        coverFile = null
                    }
                }
                
 Note
                if (!track.lrcText.isNullOrBlank()) {
                    try {
                        val lrcFile = File(bgmDir, "$safeName.lrc")
                        lrcFile.writeText(track.lrcText)
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "保存歌词失败", e)
                    }
                }
                
                onProgress?.invoke(1.0f)
                createBgmItem(track, musicFile, bgmDir, safeName, coverFile)
                
            } catch (e: Exception) {
                AppLogger.e(TAG, "下载音乐异常", e)
                null
            }
        }
    }
    
    /**
 * Note
     */
    private fun downloadFile(
        url: String,
        destFile: File,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                AppLogger.e(TAG, "下载失败: ${response.code}")
                return false
            }
            
            val body = response.body ?: return false
            val contentLength = body.contentLength()
            
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        if (contentLength > 0) {
                            onProgress?.invoke(totalBytesRead.toFloat() / contentLength)
                        }
                    }
                }
            }
            
            destFile.exists() && destFile.length() > 0
        } catch (e: Exception) {
            AppLogger.e(TAG, "下载文件异常: $url", e)
            false
        }
    }
    
    /**
 * Note
     */
    private fun generateSafeFileName(name: String, id: String): String {
        val safeName = name.replace(SAFE_NAME_REGEX, "_").take(50)
        val safeId = id.replace(SAFE_NAME_REGEX, "_").take(20)
        return "${safeName}_$safeId"
    }
    
    /**
 * Note
     */
    private fun createBgmItem(
        track: OnlineMusicTrack,
        musicFile: File,
        bgmDir: File,
        safeName: String,
        coverFile: File? = null
    ): BgmItem {
        val actualCoverFile = coverFile ?: File(bgmDir, "$safeName.jpg").takeIf { it.exists() }
        
        return BgmItem(
            name = "${track.name} - ${track.artist}",
            path = musicFile.absolutePath,
            coverPath = actualCoverFile?.absolutePath,
            isAsset = false,
            tags = emptyList(),
            sortOrder = 0,
            lrcData = null,
            lrcPath = null,
            duration = track.duration
        )
    }
    
    /**
 * Note
     */
    private fun detectExtension(url: String): String {
        val path = try {
            java.net.URL(url).path
        } catch (_: Exception) {
            url
        }
        return when {
            path.contains(".m4a", ignoreCase = true) -> "m4a"
            path.contains(".aac", ignoreCase = true) -> "aac"
            path.contains(".ogg", ignoreCase = true) -> "ogg"
            path.contains(".flac", ignoreCase = true) -> "flac"
            path.contains(".wav", ignoreCase = true) -> "wav"
            else -> "mp3"
        }
    }
    
    private val MUSIC_EXTENSIONS = listOf("mp3", "m4a", "aac", "ogg", "flac", "wav")
    
    /**
 * Note
     */
    fun isMusicDownloaded(context: Context, track: OnlineMusicTrack): Boolean {
        val bgmDir = BgmStorage.getBgmDir(context)
        val safeName = generateSafeFileName(track.name, track.id)
        return MUSIC_EXTENSIONS.any { ext ->
            val file = File(bgmDir, "$safeName.$ext")
            file.exists() && file.length() > 0
        }
    }
    
    /**
 * Note
     */
    fun getDownloadedMusicPath(context: Context, track: OnlineMusicTrack): String? {
        val bgmDir = BgmStorage.getBgmDir(context)
        val safeName = generateSafeFileName(track.name, track.id)
        for (ext in MUSIC_EXTENSIONS) {
            val file = File(bgmDir, "$safeName.$ext")
            if (file.exists()) return file.absolutePath
        }
        return null
    }
}