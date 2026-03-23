package com.webtoapp.core.bgm

import android.content.Context
import android.util.Log
import com.webtoapp.data.model.BgmItem
import com.webtoapp.util.BgmStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 在线音乐下载器
 * 负责下载音乐文件和封面图片到本地
 */
object OnlineMusicDownloader {
    
    private const val TAG = "OnlineMusicDownloader"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    /**
     * 下载在线音乐到本地
     * @param context Context
     * @param musicData 在线音乐数据
     * @param onProgress 下载进度回调 (0.0 - 1.0)
     * @return 下载后的 BgmItem，失败返回 null
     */
    suspend fun downloadMusic(
        context: Context,
        musicData: OnlineMusicData,
        onProgress: ((Float) -> Unit)? = null
    ): BgmItem? {
        return withContext(Dispatchers.IO) {
            try {
                val bgmDir = BgmStorage.getBgmDir(context)
                
                // Generate安全的文件名
                val safeName = generateSafeFileName(musicData.name, musicData.id)
                val musicFile = File(bgmDir, "$safeName.mp3")
                
                // 如果文件已存在，直接返回
                if (musicFile.exists() && musicFile.length() > 0) {
                    Log.d(TAG, "音乐文件已存在: ${musicFile.absolutePath}")
                    return@withContext createBgmItem(musicData, musicFile, bgmDir, safeName)
                }
                
                // Download音乐文件
                Log.d(TAG, "开始下载音乐: ${musicData.url}")
                val downloadSuccess = downloadFile(musicData.url, musicFile) { progress ->
                    onProgress?.invoke(progress * 0.8f) // 音乐下载占 80%
                }
                
                if (!downloadSuccess) {
                    Log.e(TAG, "音乐下载失败")
                    return@withContext null
                }
                
                // Download封面图片（如果有）
                var coverFile: File? = null
                if (!musicData.coverUrl.isNullOrBlank()) {
                    onProgress?.invoke(0.85f)
                    coverFile = File(bgmDir, "$safeName.jpg")
                    val coverSuccess = downloadFile(musicData.coverUrl, coverFile) { progress ->
                        onProgress?.invoke(0.8f + progress * 0.2f) // 封面下载占 20%
                    }
                    if (!coverSuccess) {
                        Log.w(TAG, "封面下载失败，继续使用无封面")
                        coverFile = null
                    }
                }
                
                onProgress?.invoke(1.0f)
                createBgmItem(musicData, musicFile, bgmDir, safeName, coverFile)
                
            } catch (e: Exception) {
                Log.e(TAG, "下载音乐异常", e)
                null
            }
        }
    }
    
    /**
     * 下载文件
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
                Log.e(TAG, "下载失败: ${response.code}")
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
            Log.e(TAG, "下载文件异常: $url", e)
            false
        }
    }
    
    /**
     * 生成安全的文件名
     */
    private fun generateSafeFileName(name: String, id: Long): String {
        // 移除特殊字符，保留中文、英文、数字
        val safeName = name.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5_-]"), "_")
            .take(50) // 限制长度
        return "${safeName}_$id"
    }
    
    /**
     * 创建 BgmItem
     */
    private fun createBgmItem(
        musicData: OnlineMusicData,
        musicFile: File,
        bgmDir: File,
        safeName: String,
        coverFile: File? = null
    ): BgmItem {
        // Check封面文件
        val actualCoverFile = coverFile ?: File(bgmDir, "$safeName.jpg").takeIf { it.exists() }
        
        // 歌手名称
        val artistName = musicData.singers?.joinToString("、") { it.name } ?: "未知歌手"
        
        return BgmItem(
            name = musicData.name,
            path = musicFile.absolutePath,
            coverPath = actualCoverFile?.absolutePath,
            isAsset = false,
            tags = emptyList(),
            sortOrder = 0,
            lrcData = null,
            lrcPath = null,
            duration = 0
        )
    }
    
    /**
     * 检查音乐是否已下载
     */
    fun isMusicDownloaded(context: Context, musicData: OnlineMusicData): Boolean {
        val bgmDir = BgmStorage.getBgmDir(context)
        val safeName = generateSafeFileName(musicData.name, musicData.id)
        val musicFile = File(bgmDir, "$safeName.mp3")
        return musicFile.exists() && musicFile.length() > 0
    }
    
    /**
     * 获取已下载的音乐文件路径
     */
    fun getDownloadedMusicPath(context: Context, musicData: OnlineMusicData): String? {
        val bgmDir = BgmStorage.getBgmDir(context)
        val safeName = generateSafeFileName(musicData.name, musicData.id)
        val musicFile = File(bgmDir, "$safeName.mp3")
        return if (musicFile.exists()) musicFile.absolutePath else null
    }
}
