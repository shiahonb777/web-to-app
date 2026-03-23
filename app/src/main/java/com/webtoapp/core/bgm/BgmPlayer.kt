package com.webtoapp.core.bgm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.util.Log
import com.webtoapp.core.crypto.AssetDecryptor
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.BgmItem
import com.webtoapp.data.model.BgmPlayMode
import java.io.File

/**
 * 背景音乐播放器
 * 支持循环、顺序、随机播放模式
 * 支持播放进度回调和歌词同步
 */
class BgmPlayer(private val context: Context) {
    
    companion object {
        private const val TAG = "BgmPlayer"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var config: BgmConfig? = null
    private var currentIndex: Int = 0
    private var shuffledIndices: List<Int> = emptyList()
    private var isReleased = false
    
    // Encryption资源解密器
    private val assetDecryptor by lazy { AssetDecryptor(context) }
    
    // 临时文件缓存（用于加密BGM播放）
    private val tempFileCache = mutableMapOf<String, File>()
    
    // Play状态回调
    private var onTrackChangedListener: ((BgmItem?) -> Unit)? = null
    private var onProgressListener: ((Long, Long) -> Unit)? = null  // (currentMs, totalMs)
    private var onPlayStateChangedListener: ((Boolean) -> Unit)? = null  // isPlaying
    
    // 进度更新 Handler
    private val progressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            if (!isReleased && mediaPlayer?.isPlaying == true) {
                try {
                    val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val total = mediaPlayer?.duration?.toLong() ?: 0L
                    onProgressListener?.invoke(current, total)
                } catch (e: Exception) {
                    // ignore
                }
                progressHandler.postDelayed(this, 100)  // 每 100ms 更新一次
            }
        }
    }
    
    /**
     * 设置曲目切换监听器
     */
    fun setOnTrackChangedListener(listener: ((BgmItem?) -> Unit)?) {
        onTrackChangedListener = listener
    }
    
    /**
     * 设置播放进度监听器（用于歌词同步）
     */
    fun setOnProgressListener(listener: ((Long, Long) -> Unit)?) {
        onProgressListener = listener
    }
    
    /**
     * 设置播放状态监听器
     */
    fun setOnPlayStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onPlayStateChangedListener = listener
    }
    
    /**
     * 初始化播放器
     */
    fun initialize(bgmConfig: BgmConfig) {
        if (bgmConfig.playlist.isEmpty()) return
        
        config = bgmConfig
        currentIndex = 0
        
        // Shuffle模式时生成打乱的索引
        if (bgmConfig.playMode == BgmPlayMode.SHUFFLE) {
            shuffledIndices = bgmConfig.playlist.indices.shuffled()
        }
        
        if (bgmConfig.autoPlay) {
            playCurrentTrack()
        }
    }
    
    /**
     * 播放当前曲目
     */
    private fun playCurrentTrack() {
        val cfg = config ?: return
        if (cfg.playlist.isEmpty()) return
        
        val actualIndex = when (cfg.playMode) {
            BgmPlayMode.SHUFFLE -> shuffledIndices.getOrElse(currentIndex) { 0 }
            else -> currentIndex
        }
        
        val bgmItem = cfg.playlist.getOrNull(actualIndex) ?: return
        
        try {
            releaseMediaPlayer()
            
            mediaPlayer = MediaPlayer().apply {
                // Set数据源
                if (bgmItem.path.startsWith("asset:///")) {
                    val assetPath = bgmItem.path.removePrefix("asset:///")
                    setAssetDataSource(this, assetPath)
                } else {
                    setDataSource(bgmItem.path)
                }
                
                // Set音量
                setVolume(cfg.volume, cfg.volume)
                
                // Set循环（仅单曲循环模式）
                isLooping = cfg.playMode == BgmPlayMode.LOOP && cfg.playlist.size == 1
                
                // Play完成回调
                setOnCompletionListener {
                    onTrackCompleted()
                }
                
                // 准备并播放
                prepare()
                start()
                
                // 通知曲目切换
                onTrackChangedListener?.invoke(bgmItem)
                onPlayStateChangedListener?.invoke(true)
                
                // Start进度更新
                startProgressUpdates()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 尝试播放下一首
            if (cfg.playlist.size > 1) {
                playNext()
            }
        }
    }
    
    /**
     * 曲目播放完成
     */
    private fun onTrackCompleted() {
        val cfg = config ?: return
        
        when (cfg.playMode) {
            BgmPlayMode.LOOP -> {
                if (cfg.playlist.size == 1) {
                    // 单曲循环，由 isLooping 处理
                } else {
                    // 多首歌时循环播放整个列表
                    playNext()
                }
            }
            BgmPlayMode.SEQUENTIAL -> {
                // Sequential播放，播放下一首
                if (currentIndex < cfg.playlist.size - 1) {
                    playNext()
                } else {
                    // List播放完毕，从头开始
                    currentIndex = 0
                    playCurrentTrack()
                }
            }
            BgmPlayMode.SHUFFLE -> {
                // Shuffle播放，播放下一首
                playNext()
            }
        }
    }
    
    /**
     * 播放下一首
     */
    fun playNext() {
        val cfg = config ?: return
        if (cfg.playlist.isEmpty()) return
        
        currentIndex = (currentIndex + 1) % cfg.playlist.size
        
        // Shuffle模式下，如果播放完一轮，重新打乱
        if (cfg.playMode == BgmPlayMode.SHUFFLE && currentIndex == 0) {
            shuffledIndices = cfg.playlist.indices.shuffled()
        }
        
        playCurrentTrack()
    }
    
    /**
     * 播放上一首
     */
    fun playPrevious() {
        val cfg = config ?: return
        if (cfg.playlist.isEmpty()) return
        
        currentIndex = if (currentIndex > 0) currentIndex - 1 else cfg.playlist.size - 1
        playCurrentTrack()
    }
    
    /**
     * 开始播放
     */
    fun play() {
        if (mediaPlayer == null && config != null) {
            playCurrentTrack()
        } else {
            mediaPlayer?.start()
            onPlayStateChangedListener?.invoke(true)
            startProgressUpdates()
        }
    }
    
    /**
     * 暂停播放
     */
    fun pause() {
        mediaPlayer?.pause()
        onPlayStateChangedListener?.invoke(false)
        stopProgressUpdates()
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        mediaPlayer?.stop()
        onPlayStateChangedListener?.invoke(false)
        stopProgressUpdates()
        releaseMediaPlayer()
    }
    
    /**
     * 跳转到指定位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (e: Exception) {
            // ignore
        }
    }
    
    /**
     * 获取当前播放位置（毫秒）
     */
    fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 获取总时长（毫秒）
     */
    fun getDuration(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 开始进度更新
     */
    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        if (onProgressListener != null) {
            progressHandler.post(progressRunnable)
        }
    }
    
    /**
     * 停止进度更新
     */
    private fun stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
    }
    
    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 设置音量
     */
    fun setVolume(volume: Float) {
        config = config?.copy(volume = volume)
        mediaPlayer?.setVolume(volume, volume)
    }
    
    /**
     * 获取当前播放的曲目
     */
    fun getCurrentTrack(): BgmItem? {
        val cfg = config ?: return null
        val actualIndex = when (cfg.playMode) {
            BgmPlayMode.SHUFFLE -> shuffledIndices.getOrElse(currentIndex) { 0 }
            else -> currentIndex
        }
        return cfg.playlist.getOrNull(actualIndex)
    }
    
    /**
     * 释放 MediaPlayer
     */
    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
    }
    
    /**
     * 设置 Asset 数据源（支持加密和非加密）
     */
    private fun setAssetDataSource(player: MediaPlayer, assetPath: String) {
        // 首先检查是否存在加密版本
        val encryptedPath = "$assetPath.enc"
        val hasEncrypted = try {
            context.assets.open(encryptedPath).use { true }
        } catch (e: Exception) {
            false
        }
        
        if (hasEncrypted) {
            // Encryption版本存在，需要解密到临时文件
            Log.d(TAG, "检测到加密BGM: $encryptedPath")
            
            // Check缓存
            val cachedFile = tempFileCache[assetPath]
            if (cachedFile != null && cachedFile.exists()) {
                Log.d(TAG, "使用缓存的解密BGM: ${cachedFile.absolutePath}")
                player.setDataSource(cachedFile.absolutePath)
                return
            }
            
            try {
                // Decryption到临时文件
                val decryptedData = assetDecryptor.loadAsset(assetPath)
                val tempFile = File(context.cacheDir, "bgm_${assetPath.hashCode()}.mp3")
                tempFile.writeBytes(decryptedData)
                tempFileCache[assetPath] = tempFile
                
                Log.d(TAG, "BGM已解密到临时文件: ${tempFile.absolutePath} (${decryptedData.size} bytes)")
                player.setDataSource(tempFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "解密BGM失败: $assetPath", e)
                throw e
            }
        } else {
            // 非加密版本，使用 openFd
            try {
                val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                Log.d(TAG, "加载非加密BGM: $assetPath")
            } catch (e: Exception) {
                Log.e(TAG, "加载BGM失败: $assetPath", e)
                throw e
            }
        }
    }
    
    /**
     * 清理临时文件缓存
     */
    private fun clearTempFiles() {
        tempFileCache.values.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        tempFileCache.clear()
    }
    
    /**
     * 释放所有资源
     */
    fun release() {
        isReleased = true
        stopProgressUpdates()
        releaseMediaPlayer()
        clearTempFiles()
        config = null
        onTrackChangedListener = null
        onProgressListener = null
        onPlayStateChangedListener = null
    }
}
