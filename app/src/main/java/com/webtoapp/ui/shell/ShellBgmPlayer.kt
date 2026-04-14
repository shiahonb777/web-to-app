package com.webtoapp.ui.shell

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import androidx.compose.runtime.*
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.LrcLine
import kotlinx.coroutines.delay

// Composable parseLrcText( ) , call
private val BGM_LRC_TIME_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

/**
 * BGM state( from ShellActivity)
 */
class BgmPlayerState internal constructor(
    private val _player: MutableState<MediaPlayer?>,
    private val _currentIndex: MutableIntState,
    private val _isPlaying: MutableState<Boolean>,
    private val _currentLrcData: MutableState<LrcData?>,
    private val _currentLrcLineIndex: MutableIntState,
    private val _currentPosition: MutableLongState
) {
    var player: MediaPlayer? by _player
    var currentIndex: Int by _currentIndex
    var isPlaying: Boolean by _isPlaying
    var currentLrcData: LrcData? by _currentLrcData
    var currentLrcLineIndex: Int by _currentLrcLineIndex
    var currentPosition: Long by _currentPosition
}

/**
 * Parse LRC text
 */
internal fun parseLrcText(text: String): LrcData? {
    val lines = mutableListOf<LrcLine>()
    
    text.lines().forEach { line ->
        BGM_LRC_TIME_REGEX.find(line)?.let { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0
            val seconds = match.groupValues[2].toLongOrNull() ?: 0
            val millis = match.groupValues[3].let {
                if (it.length == 2) it.toLong() * 10 else it.toLong()
            }
            val lyricText = match.groupValues[4].trim()
            
            if (lyricText.isNotEmpty()) {
                val startTime = minutes * 60000 + seconds * 1000 + millis
                lines.add(LrcLine(startTime = startTime, endTime = startTime + 5000, text = lyricText))
            }
        }
    }
    
    // Note
    for (i in 0 until lines.size - 1) {
        lines[i] = lines[i].copy(endTime = lines[i + 1].startTime)
    }
    
    return if (lines.isNotEmpty()) LrcData(lines = lines) else null
}

/**
 * createandmanagement BGM state
 * from ShellActivity BGM( lines 1430- 1592)
 */
@Composable
fun rememberBgmPlayerState(
    context: Context,
    config: ShellConfig
): BgmPlayerState {
    // Note
    val bgmPlayerState = remember { mutableStateOf<MediaPlayer?>(null) }
    var bgmPlayer by bgmPlayerState
    val currentBgmIndexState = remember { mutableIntStateOf(0) }
    var currentBgmIndex by currentBgmIndexState
    val isBgmPlayingState = remember { mutableStateOf(false) }
    var isBgmPlaying by isBgmPlayingState
    
    // ===== display =====
    val currentLrcDataState = remember { mutableStateOf<LrcData?>(null) }
    var currentLrcData by currentLrcDataState
    val currentLrcLineIndexState = remember { mutableIntStateOf(-1) }
    var currentLrcLineIndex by currentLrcLineIndexState
    val bgmCurrentPositionState = remember { mutableLongStateOf(0L) }
    var bgmCurrentPosition by bgmCurrentPositionState
    
    // Loadcurrent BGM LRC
    fun loadLrcForCurrentBgm(bgmIndex: Int) {
        if (!config.bgmShowLyrics) {
            currentLrcData = null
            return
        }
        
        val bgmItem = config.bgmPlaylist.getOrNull(bgmIndex) ?: return
        val lrcPath = bgmItem.lrcAssetPath ?: return
        
        try {
            val lrcAssetPath = lrcPath.removePrefix("assets/")
            val lrcText = context.assets.open(lrcAssetPath).bufferedReader().readText()
            currentLrcData = parseLrcText(lrcText)
            currentLrcLineIndex = -1
            AppLogger.d("ShellActivity", "LRC 加载成功: $lrcPath, ${currentLrcData?.lines?.size} 行")
        } catch (e: Exception) {
            AppLogger.e("ShellActivity", "加载 LRC 失败: $lrcPath", e)
            currentLrcData = null
        }
    }
    
    // Initializeand BGM
    LaunchedEffect(config.bgmEnabled) {
        if (config.bgmEnabled && config.bgmPlaylist.isNotEmpty()) {
            try {
                // Create
                val player = MediaPlayer()
                val firstItem = config.bgmPlaylist.first()
                val assetPath = firstItem.assetPath.removePrefix("assets/")
                
                val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                
                player.setVolume(config.bgmVolume, config.bgmVolume)
                player.isLooping = config.bgmPlayMode == "LOOP" && config.bgmPlaylist.size == 1
                
                player.setOnCompletionListener {
                    // Play
                    val nextIndex = when (config.bgmPlayMode) {
                        "SHUFFLE" -> (0 until config.bgmPlaylist.size).random()
                        "SEQUENTIAL" -> if (currentBgmIndex + 1 < config.bgmPlaylist.size) currentBgmIndex + 1 else -1
                        else -> (currentBgmIndex + 1) % config.bgmPlaylist.size // LOOP
                    }
                    
                    if (nextIndex >= 0 && nextIndex < config.bgmPlaylist.size) {
                        currentBgmIndex = nextIndex
                        try {
                            player.reset()
                            val nextItem = config.bgmPlaylist[nextIndex]
                            val nextAssetPath = nextItem.assetPath.removePrefix("assets/")
                            val nextAfd = context.assets.openFd(nextAssetPath)
                            player.setDataSource(nextAfd.fileDescriptor, nextAfd.startOffset, nextAfd.length)
                            nextAfd.close()
                            player.prepare()
                            player.start()
                            
                            // Load
                            loadLrcForCurrentBgm(nextIndex)
                        } catch (e: Exception) {
                            AppLogger.e("ShellActivity", "播放下一首 BGM 失败", e)
                        }
                    }
                }
                
                player.prepare()
                
                // Auto
                if (config.bgmAutoPlay) {
                    player.start()
                    isBgmPlaying = true
                }
                
                bgmPlayer = player
                
                // Load
                loadLrcForCurrentBgm(0)
                
                AppLogger.d("ShellActivity", "BGM 播放器初始化成功: ${firstItem.name}")
            } catch (e: Exception) {
                AppLogger.e("ShellActivity", "初始化 BGM 播放器失败", e)
            }
        }
    }
    
    // Update display( )
    LaunchedEffect(isBgmPlaying, currentLrcData) {
        if (!isBgmPlaying || currentLrcData == null) return@LaunchedEffect
        
        while (isBgmPlaying && currentLrcData != null) {
            bgmPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        bgmCurrentPosition = mp.currentPosition.toLong()
                        
                        // Findcurrent display
                        val lrcData = currentLrcData
                        if (lrcData != null) {
                            val newIndex = lrcData.lines.indexOfLast { it.startTime <= bgmCurrentPosition }
                            if (newIndex != currentLrcLineIndex) {
                                currentLrcLineIndex = newIndex
                            }
                        }
                    }
                } catch (e: Exception) {
                    // state
                }
            }
            delay(100)
        }
    }
    
    // Cleanup BGM
    DisposableEffect(Unit) {
        onDispose {
            bgmPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            bgmPlayer = null
        }
    }
    
    return BgmPlayerState(
        _player = bgmPlayerState,
        _currentIndex = currentBgmIndexState,
        _isPlaying = isBgmPlayingState,
        _currentLrcData = currentLrcDataState,
        _currentLrcLineIndex = currentLrcLineIndexState,
        _currentPosition = bgmCurrentPositionState
    )
}
