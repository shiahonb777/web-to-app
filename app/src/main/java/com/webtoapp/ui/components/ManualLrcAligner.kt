package com.webtoapp.ui.components

import android.media.MediaPlayer
import com.webtoapp.core.logging.AppLogger
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.data.model.BgmItem
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.LrcLine
import com.webtoapp.util.BgmStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 手动 LRC 对齐工具对话框
 * 用户可以输入歌词文本，然后播放音频时点击按钮进行逐句时间对齐
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLrcAlignerDialog(
    bgm: BgmItem,
    existingLrc: LrcData? = null,
    onDismiss: () -> Unit,
    onSave: (LrcData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 步骤状态：1=输入歌词, 2=对齐时间, 3=预览确认
    var currentStep by remember { mutableIntStateOf(if (existingLrc != null) 2 else 1) }
    
    // Lyrics文本（每行一句）
    var lyricsText by remember { 
        mutableStateOf(existingLrc?.lines?.joinToString("\n") { it.text } ?: "") 
    }
    
    // Parse后的歌词行
    var lyricLines by remember { 
        mutableStateOf(existingLrc?.lines?.map { it.text } ?: emptyList()) 
    }
    
    // 每行的时间戳（毫秒）
    var timestamps by remember { 
        mutableStateOf(existingLrc?.lines?.map { it.startTime } ?: emptyList<Long>()) 
    }
    
    // 跟踪哪些行已经对齐了（不能用 timestamp > 0 判断，因为歌词可能从 0ms 开始）
    var alignedIndices by remember {
        mutableStateOf(
            if (existingLrc != null) (existingLrc.lines.indices).toSet() else emptySet<Int>()
        )
    }
    
    // 当前正在对齐的行索引
    var currentAlignIndex by remember { mutableIntStateOf(0) }
    
    // 历史状态管理（撤销/重做）
    data class AlignState(val timestamps: List<Long>, val currentIndex: Int, val aligned: Set<Int>)
    var historyStack by remember { mutableStateOf(listOf<AlignState>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    
    // Save状态到历史
    fun saveToHistory() {
        val newState = AlignState(timestamps.toList(), currentAlignIndex, alignedIndices.toSet())
        // 如果在历史中间位置，删除后面的记录
        val trimmedHistory = if (historyIndex < historyStack.size - 1) {
            historyStack.take(historyIndex + 1)
        } else {
            historyStack
        }
        historyStack = trimmedHistory + newState
        historyIndex = historyStack.size - 1
    }
    
    // Undo
    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val state = historyStack[historyIndex]
            timestamps = state.timestamps
            currentAlignIndex = state.currentIndex
            alignedIndices = state.aligned
        }
    }
    
    // Redo
    fun redo() {
        if (historyIndex < historyStack.size - 1) {
            historyIndex++
            val state = historyStack[historyIndex]
            timestamps = state.timestamps
            currentAlignIndex = state.currentIndex
            alignedIndices = state.aligned
        }
    }
    
    // Play器状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // List滚动状态
    val listState = rememberLazyListState()
    
    // Initialize播放器
    LaunchedEffect(bgm.path) {
        try {
            mediaPlayer = MediaPlayer().apply {
                if (bgm.path.startsWith("asset:///")) {
                    val assetPath = bgm.path.removePrefix("asset:///")
                    val afd = context.assets.openFd(assetPath)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                } else {
                    setDataSource(bgm.path)
                }
                prepare()
                duration = this.duration.toLong()
            }
        } catch (e: Exception) {
            AppLogger.e("ManualLrcAligner", "初始化播放器失败", e)
        }
    }
    
    // Update播放进度
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPosition = mp.currentPosition.toLong()
                }
            }
            delay(50) // 50ms 更新一次，更精确
        }
    }
    
    // Cleanup播放器
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    
    // 格式化时间
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(minutes, seconds, millis)
    }
    
    // Handle对齐点击
    fun onAlignClick() {
        if (currentAlignIndex < lyricLines.size) {
            // Save当前状态到历史（打点前）
            saveToHistory()
            
            val newTimestamps = timestamps.toMutableList()
            // 确保列表足够长
            while (newTimestamps.size <= currentAlignIndex) {
                newTimestamps.add(0L)
            }
            newTimestamps[currentAlignIndex] = currentPosition
            timestamps = newTimestamps
            // 标记当前行已对齐
            alignedIndices = alignedIndices + currentAlignIndex
            
            // 移动到下一行
            if (currentAlignIndex < lyricLines.size - 1) {
                currentAlignIndex++
                // 滚动到当前行
                scope.launch {
                    listState.animateScrollToItem(maxOf(0, currentAlignIndex - 2))
                }
            }
            
            // Save打点后的状态
            saveToHistory()
        }
    }
    
    // Build最终的 LrcData
    fun buildLrcData(): LrcData {
        val lines = lyricLines.mapIndexed { index, text ->
            val startTime = timestamps.getOrElse(index) { 0L }
            val endTime = timestamps.getOrElse(index + 1) { startTime + 5000L }
            LrcLine(startTime = startTime, endTime = endTime, text = text)
        }
        return LrcData(lines = lines)
    }
    
    Dialog(
        onDismissRequest = {
            mediaPlayer?.release()
            mediaPlayer = null
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(bottom = 64.dp)) {
                // 标题栏 - 包含步骤导航按钮
                TopAppBar(
                    title = { 
                        Text(when (currentStep) {
                            1 -> com.webtoapp.core.i18n.Strings.inputLyrics
                            2 -> com.webtoapp.core.i18n.Strings.timeAlignment
                            else -> com.webtoapp.core.i18n.Strings.previewConfirm
                        })
                    },
                    navigationIcon = {
                        if (currentStep == 1) {
                            // 步骤1：关闭按钮
                            IconButton(onClick = {
                                mediaPlayer?.release()
                                mediaPlayer = null
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, com.webtoapp.core.i18n.Strings.close)
                            }
                        } else {
                            // 步骤2/3：返回上一步
                            IconButton(onClick = {
                                when (currentStep) {
                                    2 -> { currentStep = 1 }
                                    3 -> { currentStep = 2 }
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, com.webtoapp.core.i18n.Strings.previousStep)
                            }
                        }
                    },
                    actions = {
                        // 步骤指示器
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { step ->
                                Box(
                                    modifier = Modifier
                                        .size(if (step + 1 == currentStep) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (step + 1 <= currentStep)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outlineVariant
                                        )
                                )
                            }
                        }
                        
                        // 步骤导航：下一步/保存按钮
                        when (currentStep) {
                            1 -> {
                                val lineCount = lyricsText.lines().filter { it.trim().isNotEmpty() }.size
                                TextButton(
                                    onClick = {
                                        lyricLines = lyricsText.lines()
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                        timestamps = List(lyricLines.size) { 0L }
                                        alignedIndices = emptySet()
                                        currentAlignIndex = 0
                                        currentStep = 2
                                    },
                                    enabled = lineCount > 0
                                ) {
                                    Text(com.webtoapp.core.i18n.Strings.nextStepTimeAlign)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                                }
                            }
                            2 -> {
                                TextButton(
                                    onClick = {
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                        currentStep = 3
                                    },
                                    enabled = lyricLines.indices.all { it in alignedIndices }
                                ) {
                                    Text(com.webtoapp.core.i18n.Strings.nextStep)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                                }
                            }
                            3 -> {
                                TextButton(
                                    onClick = {
                                        val lrcData = buildLrcData()
                                        scope.launch {
                                            BgmStorage.saveLrc(context, bgm.path, lrcData)
                                        }
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        onSave(lrcData)
                                    }
                                ) {
                                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(com.webtoapp.core.i18n.Strings.saveLrc)
                                }
                            }
                        }
                    }
                )
                
                // 音乐信息
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                bgm.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "${com.webtoapp.core.i18n.Strings.duration}: ${formatTime(duration)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 内容区域
                when (currentStep) {
                    1 -> {
                        // 步骤1：输入歌词
                        LyricsInputStep(
                            lyricsText = lyricsText,
                            onTextChange = { lyricsText = it },
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .padding(horizontal = 16.dp)
                        )
                    }
                    2 -> {
                        // 步骤2：时间对齐
                        AlignmentStep(
                            lyricLines = lyricLines,
                            timestamps = timestamps,
                            alignedIndices = alignedIndices,
                            currentAlignIndex = currentAlignIndex,
                            currentPosition = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            listState = listState,
                            formatTime = ::formatTime,
                            onAlignClick = ::onAlignClick,
                            onPlay = {
                                mediaPlayer?.let { mp ->
                                    if (isPlaying) {
                                        mp.pause()
                                    } else {
                                        mp.start()
                                    }
                                    isPlaying = !isPlaying
                                }
                            },
                            onSeek = { progress ->
                                val newPos = (progress * duration).toLong()
                                mediaPlayer?.seekTo(newPos.toInt())
                                currentPosition = newPos
                            },
                            onRewind = {
                                mediaPlayer?.let { mp ->
                                    val newPos = maxOf(0, mp.currentPosition - 3000)
                                    mp.seekTo(newPos)
                                    currentPosition = newPos.toLong()
                                }
                            },
                            onEditTimestamp = { index, newTime ->
                                saveToHistory()
                                val newTimestamps = timestamps.toMutableList()
                                newTimestamps[index] = newTime
                                timestamps = newTimestamps
                                if (newTime > 0L || index in alignedIndices) {
                                    alignedIndices = alignedIndices + index
                                }
                                if (newTime == 0L) {
                                    alignedIndices = alignedIndices - index
                                }
                                saveToHistory()
                            },
                            onSelectLine = { index ->
                                currentAlignIndex = index
                            },
                            canUndo = historyIndex > 0,
                            canRedo = historyIndex < historyStack.size - 1,
                            onUndo = ::undo,
                            onRedo = ::redo,
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .padding(horizontal = 16.dp)
                        )
                    }
                    3 -> {
                        // 步骤3：预览确认
                        PreviewStep(
                            lrcData = buildLrcData(),
                            mediaPlayer = mediaPlayer,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            formatTime = ::formatTime,
                            onPlay = {
                                mediaPlayer?.let { mp ->
                                    if (isPlaying) {
                                        mp.pause()
                                    } else {
                                        mp.start()
                                    }
                                    isPlaying = !isPlaying
                                }
                            },
                            onSeek = { progress ->
                                val newPos = (progress * duration).toLong()
                                mediaPlayer?.seekTo(newPos.toInt())
                                currentPosition = newPos
                            },
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 步骤1：歌词输入
 */
@Composable
private fun LyricsInputStep(
    lyricsText: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            com.webtoapp.core.i18n.Strings.inputLyricsHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Lyrics输入框
        OutlinedTextField(
            value = lyricsText,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = true),
            placeholder = { 
                Text(
                    com.webtoapp.core.i18n.Strings.lyricsPlaceholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ) 
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 统计信息
        val lineCount = lyricsText.lines().filter { it.trim().isNotEmpty() }.size
        Text(
            com.webtoapp.core.i18n.Strings.totalLinesCount.format(lineCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 步骤2：时间对齐
 */
@Composable
private fun AlignmentStep(
    lyricLines: List<String>,
    timestamps: List<Long>,
    alignedIndices: Set<Int>,
    currentAlignIndex: Int,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    formatTime: (Long) -> String,
    onAlignClick: () -> Unit,
    onPlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onRewind: () -> Unit,
    onEditTimestamp: (Int, Long) -> Unit,
    onSelectLine: (Int) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 说明
        Text(
            com.webtoapp.core.i18n.Strings.alignmentHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 根据播放位置自动追踪当前播放行（基于已对齐的时间戳）
        val scope = rememberCoroutineScope()
        var playingLineIndex by remember { mutableIntStateOf(-1) }
        
        LaunchedEffect(currentPosition, alignedIndices) {
            if (alignedIndices.isEmpty()) {
                playingLineIndex = -1
                return@LaunchedEffect
            }
            // 在已对齐的行中，找到最后一个 timestamp <= currentPosition 的行
            val newIndex = timestamps.indices
                .filter { it in alignedIndices }
                .lastOrNull { timestamps[it] <= currentPosition }
                ?: -1
            if (newIndex != playingLineIndex && newIndex >= 0) {
                playingLineIndex = newIndex
                // 自动滚动到当前播放行（但不干扰用户手动选择的对齐行）
                if (isPlaying) {
                    scope.launch {
                        listState.animateScrollToItem(maxOf(0, newIndex - 2))
                    }
                }
            }
        }
        
        // Lyrics列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(lyricLines) { index, line ->
                val isAligned = index in timestamps.indices && index in alignedIndices
                val isCurrent = index == currentAlignIndex
                val isPlayingLine = index == playingLineIndex && isPlaying
                val timestamp = timestamps.getOrElse(index) { 0L }
                
                AlignmentLineItem(
                    index = index,
                    line = line,
                    timestamp = timestamp,
                    isAligned = isAligned,
                    isCurrent = isCurrent,
                    isPlayingLine = isPlayingLine,
                    formatTime = formatTime,
                    onClick = { onSelectLine(index) },
                    onEditTime = { newTime -> onEditTimestamp(index, newTime) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Play控制区
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // 进度条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = onSeek,
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 控制按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo按钮
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            com.webtoapp.core.i18n.Strings.undo,
                            tint = if (canUndo) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                    
                    // 后退3秒
                    IconButton(onClick = onRewind) {
                        Icon(Icons.Default.Replay, com.webtoapp.core.i18n.Strings.rewind3s)
                    }
                    
                    // Play/暂停
                    FilledIconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) com.webtoapp.core.i18n.Strings.pause else com.webtoapp.core.i18n.Strings.play,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // 打点按钮（核心功能）
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.1f else 1f,
                        label = "scale"
                    )
                    
                    FilledTonalButton(
                        onClick = {
                            // 确保点击事件被正确处理
                            onAlignClick()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 100.dp) // 确保有足够的点击区域
                            .then(
                                if (buttonScale != 1f) {
                                    Modifier.scale(buttonScale)
                                } else {
                                    Modifier
                                }
                            ),
                        enabled = true,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.TouchApp, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(com.webtoapp.core.i18n.Strings.tap, fontWeight = FontWeight.Bold)
                    }
                    
                    // 重新打点按钮 - 清除当前行时间戳
                    val currentTimestamp = timestamps.getOrElse(currentAlignIndex) { 0L }
                    IconButton(
                        onClick = { onEditTimestamp(currentAlignIndex, 0L) },
                        enabled = currentTimestamp > 0
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            com.webtoapp.core.i18n.Strings.reTap,
                            tint = if (currentTimestamp > 0) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                    
                    // Redo按钮
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            com.webtoapp.core.i18n.Strings.redo,
                            tint = if (canRedo) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
                
                // 当前对齐进度
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (currentAlignIndex + 1).toFloat() / lyricLines.size },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${com.webtoapp.core.i18n.Strings.progress}: ${currentAlignIndex + 1}/${lyricLines.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 对齐行项目
 */
@Composable
private fun AlignmentLineItem(
    index: Int,
    line: String,
    timestamp: Long,
    isAligned: Boolean,
    isCurrent: Boolean,
    isPlayingLine: Boolean = false,
    formatTime: (Long) -> String,
    onClick: () -> Unit,
    onEditTime: (Long) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPlayingLine -> MaterialTheme.colorScheme.secondaryContainer
            isCurrent -> MaterialTheme.colorScheme.primaryContainer
            isAligned -> if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
            else -> Color.Transparent
        },
        label = "bg"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 行号
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlayingLine) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            
            // Time戳
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isAligned) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    if (isAligned) "[${formatTime(timestamp)}]" else "[--:--.--]",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAligned) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Lyrics内容
            Text(
                line,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent || isPlayingLine) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isPlayingLine -> MaterialTheme.colorScheme.onSecondaryContainer
                    isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(weight = 1f, fill = true)
            )
            
            // 状态图标
            when {
                isPlayingLine -> {
                    Icon(
                        Icons.Outlined.MusicNote,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                isAligned -> {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                isCurrent -> {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 步骤3：预览确认
 */
@Composable
private fun PreviewStep(
    lrcData: LrcData,
    mediaPlayer: MediaPlayer?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    formatTime: (Long) -> String,
    onPlay: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 当前高亮的行
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    
    // 根据播放位置更新高亮行
    LaunchedEffect(currentPosition) {
        val newIndex = lrcData.lines.indexOfLast { it.startTime <= currentPosition }
        if (newIndex != currentLineIndex && newIndex >= 0) {
            currentLineIndex = newIndex
            scope.launch {
                listState.animateScrollToItem(maxOf(0, newIndex - 2))
            }
        }
    }
    
    Column(modifier = modifier) {
        Text(
            com.webtoapp.core.i18n.Strings.previewLrcHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Lyrics预览
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = true)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.05f)),
            contentPadding = PaddingValues(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(lrcData.lines) { index, line ->
                val isCurrent = index == currentLineIndex
                Text(
                    text = line.text,
                    style = if (isCurrent) 
                        MaterialTheme.typography.titleMedium 
                    else 
                        MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Play控制
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatTime(currentPosition),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = onSeek,
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                FilledIconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) com.webtoapp.core.i18n.Strings.pause else com.webtoapp.core.i18n.Strings.play,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}
