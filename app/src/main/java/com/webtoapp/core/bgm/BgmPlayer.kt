package com.webtoapp.core.bgm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import com.webtoapp.core.crypto.AssetDecryptor
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.BgmItem
import com.webtoapp.data.model.BgmPlayMode
import java.io.File






class BgmPlayer(private val context: Context) {

    companion object {
        private const val TAG = "BgmPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var config: BgmConfig? = null
    private var currentIndex: Int = 0
    private var shuffledIndices: List<Int> = emptyList()
    private var isReleased = false


    private val assetDecryptor by lazy { AssetDecryptor(context) }


    private val tempFileCache = mutableMapOf<String, File>()


    private var onTrackChangedListener: ((BgmItem?) -> Unit)? = null
    private var onProgressListener: ((Long, Long) -> Unit)? = null
    private var onPlayStateChangedListener: ((Boolean) -> Unit)? = null


    private val progressHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            if (!isReleased && mediaPlayer?.isPlaying == true) {
                try {
                    val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val total = mediaPlayer?.duration?.toLong() ?: 0L
                    onProgressListener?.invoke(current, total)
                } catch (e: Exception) {

                }
                progressHandler.postDelayed(this, 100)
            }
        }
    }




    fun setOnTrackChangedListener(listener: ((BgmItem?) -> Unit)?) {
        onTrackChangedListener = listener
    }




    fun setOnProgressListener(listener: ((Long, Long) -> Unit)?) {
        onProgressListener = listener
    }




    fun setOnPlayStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onPlayStateChangedListener = listener
    }




    fun initialize(bgmConfig: BgmConfig) {
        if (bgmConfig.playlist.isEmpty()) return

        config = bgmConfig
        currentIndex = 0


        if (bgmConfig.playMode == BgmPlayMode.SHUFFLE) {
            shuffledIndices = bgmConfig.playlist.indices.shuffled()
        }

        if (bgmConfig.autoPlay) {
            playCurrentTrack()
        }
    }




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

                if (bgmItem.path.startsWith("asset:///")) {
                    val assetPath = bgmItem.path.removePrefix("asset:///")
                    setAssetDataSource(this, assetPath)
                } else {
                    setDataSource(bgmItem.path)
                }


                setVolume(cfg.volume, cfg.volume)


                isLooping = cfg.playMode == BgmPlayMode.LOOP && cfg.playlist.size == 1


                setOnCompletionListener {
                    onTrackCompleted()
                }


                prepare()
                start()


                onTrackChangedListener?.invoke(bgmItem)
                onPlayStateChangedListener?.invoke(true)


                startProgressUpdates()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Operation failed", e)

            if (cfg.playlist.size > 1) {
                playNext()
            }
        }
    }




    private fun onTrackCompleted() {
        val cfg = config ?: return

        when (cfg.playMode) {
            BgmPlayMode.LOOP -> {
                if (cfg.playlist.size == 1) {

                } else {

                    playNext()
                }
            }
            BgmPlayMode.SEQUENTIAL -> {

                if (currentIndex < cfg.playlist.size - 1) {
                    playNext()
                } else {

                    currentIndex = 0
                    playCurrentTrack()
                }
            }
            BgmPlayMode.SHUFFLE -> {

                playNext()
            }
        }
    }




    fun playNext() {
        val cfg = config ?: return
        if (cfg.playlist.isEmpty()) return

        currentIndex = (currentIndex + 1) % cfg.playlist.size


        if (cfg.playMode == BgmPlayMode.SHUFFLE && currentIndex == 0) {
            shuffledIndices = cfg.playlist.indices.shuffled()
        }

        playCurrentTrack()
    }




    fun playPrevious() {
        val cfg = config ?: return
        if (cfg.playlist.isEmpty()) return

        currentIndex = if (currentIndex > 0) currentIndex - 1 else cfg.playlist.size - 1
        playCurrentTrack()
    }




    fun play() {
        if (mediaPlayer == null && config != null) {
            playCurrentTrack()
        } else {
            mediaPlayer?.start()
            onPlayStateChangedListener?.invoke(true)
            startProgressUpdates()
        }
    }




    fun pause() {
        mediaPlayer?.pause()
        onPlayStateChangedListener?.invoke(false)
        stopProgressUpdates()
    }




    fun stop() {
        mediaPlayer?.stop()
        onPlayStateChangedListener?.invoke(false)
        stopProgressUpdates()
        releaseMediaPlayer()
    }




    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (e: Exception) {

        }
    }




    fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }




    fun getDuration(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }




    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        if (onProgressListener != null) {
            progressHandler.post(progressRunnable)
        }
    }




    private fun stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
    }




    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }




    fun setVolume(volume: Float) {
        config = config?.copy(volume = volume)
        mediaPlayer?.setVolume(volume, volume)
    }




    fun getCurrentTrack(): BgmItem? {
        val cfg = config ?: return null
        val actualIndex = when (cfg.playMode) {
            BgmPlayMode.SHUFFLE -> shuffledIndices.getOrElse(currentIndex) { 0 }
            else -> currentIndex
        }
        return cfg.playlist.getOrNull(actualIndex)
    }




    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {

        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {

        }
        mediaPlayer = null
    }




    private fun setAssetDataSource(player: MediaPlayer, assetPath: String) {

        val encryptedPath = "$assetPath.enc"
        val hasEncrypted = try {
            context.assets.open(encryptedPath).use { true }
        } catch (e: Exception) {
            false
        }

        if (hasEncrypted) {

            AppLogger.d(TAG, "检测到加密BGM: $encryptedPath")


            val cachedFile = tempFileCache[assetPath]
            if (cachedFile != null && cachedFile.exists()) {
                AppLogger.d(TAG, "使用缓存的解密BGM: ${cachedFile.absolutePath}")
                player.setDataSource(cachedFile.absolutePath)
                return
            }

            try {

                val decryptedData = assetDecryptor.loadAsset(assetPath)
                val tempFile = File(context.cacheDir, "bgm_${assetPath.hashCode()}.mp3")
                tempFile.writeBytes(decryptedData)
                tempFileCache[assetPath] = tempFile

                AppLogger.d(TAG, "BGM已解密到临时文件: ${tempFile.absolutePath} (${decryptedData.size} bytes)")
                player.setDataSource(tempFile.absolutePath)
            } catch (e: Exception) {
                AppLogger.e(TAG, "解密BGM失败: $assetPath", e)
                throw e
            }
        } else {

            try {
                val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                AppLogger.d(TAG, "加载非加密BGM: $assetPath")
            } catch (e: Exception) {
                AppLogger.e(TAG, "加载BGM失败: $assetPath", e)
                throw e
            }
        }
    }




    private fun clearTempFiles() {
        tempFileCache.values.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {

            }
        }
        tempFileCache.clear()
    }




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
