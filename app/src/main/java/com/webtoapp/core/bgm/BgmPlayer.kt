package com.webtoapp.core.bgm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.BgmItem
import com.webtoapp.data.model.BgmPlayMode

/**
 * 背景音乐播放器
 * 支持循环、顺序、随机播放模式
 */
class BgmPlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var config: BgmConfig? = null
    private var currentIndex: Int = 0
    private var shuffledIndices: List<Int> = emptyList()
    private var isReleased = false
    
    /**
     * 初始化播放器
     */
    fun initialize(bgmConfig: BgmConfig) {
        if (bgmConfig.playlist.isEmpty()) return
        
        config = bgmConfig
        currentIndex = 0
        
        // 随机模式时生成打乱的索引
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
                // 设置数据源
                if (bgmItem.path.startsWith("asset:///")) {
                    val assetPath = bgmItem.path.removePrefix("asset:///")
                    val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                } else {
                    setDataSource(bgmItem.path)
                }
                
                // 设置音量
                setVolume(cfg.volume, cfg.volume)
                
                // 设置循环（仅单曲循环模式）
                isLooping = cfg.playMode == BgmPlayMode.LOOP && cfg.playlist.size == 1
                
                // 播放完成回调
                setOnCompletionListener {
                    onTrackCompleted()
                }
                
                // 准备并播放
                prepare()
                start()
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
                // 顺序播放，播放下一首
                if (currentIndex < cfg.playlist.size - 1) {
                    playNext()
                } else {
                    // 列表播放完毕，从头开始
                    currentIndex = 0
                    playCurrentTrack()
                }
            }
            BgmPlayMode.SHUFFLE -> {
                // 随机播放，播放下一首
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
        
        // 随机模式下，如果播放完一轮，重新打乱
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
        }
    }
    
    /**
     * 暂停播放
     */
    fun pause() {
        mediaPlayer?.pause()
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        mediaPlayer?.stop()
        releaseMediaPlayer()
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
     * 释放所有资源
     */
    fun release() {
        isReleased = true
        releaseMediaPlayer()
        config = null
    }
}
