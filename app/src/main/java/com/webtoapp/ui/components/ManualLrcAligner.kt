package com.webtoapp.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import com.webtoapp.data.model.LrcTheme
import com.webtoapp.data.model.PresetLrcThemes
import com.webtoapp.util.BgmStorage
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

/**
 * 每行的时间戳（开始和结束）
 */
data class LineTimestamp(val startTime: Long = 0L, val endTime: Long = 0L)

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
    
    // 歌词文本（每行一句）
    var lyricsText by remember { 
        mutableStateOf(existingLrc?.lines?.joinToString("\n") { it.text } ?: "") 
    }
    
    // 解析后的歌词行
    var lyricLines by remember { 
        mutableStateOf(existingLrc?.lines?.map { it.text } ?: emptyList()) 
    }
    
    // 每行的时间戳
    var lineTimestamps by remember { 
        mutableStateOf(
            existingLrc?.lines?.map { LineTimestamp(it.startTime, it.endTime) } 
                ?: emptyList<LineTimestamp>()
        ) 
    }
    
    // 当前正在对齐的行索引
    var currentAlignIndex by remember { mutableIntStateOf(0) }
    
    // 打点模式：true = 打开始点，false = 打结束点
    var isMarkingStart by remember { mutableStateOf(true) }
    
    // 历史记录用于撤销/重做
    data class AlignState(
        val lineTimestamps: List<LineTimestamp>, 
        val currentIndex: Int,
        val isMarkingStart: Boolean
    )
    val historyStack = remember { mutableStateListOf<AlignState>() }
    var historyIndex by remember { mutableIntStateOf(-1) }
    
    // 播放器状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // 列表滚动状态
    val listState = rememberLazyListState()
    
    // 初始化播放器
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
            android.util.Log.e("ManualLrcAligner", "初始化播放器失败", e)
        }
    }
    
    // 更新播放进度
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
    
    // 清理播放器
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
    
    // 保存当前状态到历史记录
    fun saveToHistory() {
        while (historyStack.size > historyIndex + 1) {
            historyStack.removeAt(historyStack.size - 1)
        }
        historyStack.add(AlignState(lineTimestamps.toList(), currentAlignIndex, isMarkingStart))
        historyIndex = historyStack.size - 1
    }
    
    // 撤销
    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val state = historyStack[historyIndex]
            lineTimestamps = state.lineTimestamps
            currentAlignIndex = state.currentIndex
            isMarkingStart = state.isMarkingStart
            scope.launch {
                listState.animateScrollToItem(maxOf(0, currentAlignIndex - 2))
            }
        }
    }
    
    // 重做
    fun redo() {
        if (historyIndex < historyStack.size - 1) {
            historyIndex++
            val state = historyStack[historyIndex]
            lineTimestamps = state.lineTimestamps
            currentAlignIndex = state.currentIndex
            isMarkingStart = state.isMarkingStart
            scope.launch {
                listState.animateScrollToItem(maxOf(0, currentAlignIndex - 2))
            }
        }
    }
    
    // 打开始点
    fun markStart() {
        if (currentAlignIndex < lyricLines.size) {
            val newTimestamps = lineTimestamps.toMutableList()
            while (newTimestamps.size <= currentAlignIndex) {
                newTimestamps.add(LineTimestamp())
            }
            newTimestamps[currentAlignIndex] = newTimestamps[currentAlignIndex].copy(startTime = currentPosition)
            lineTimestamps = newTimestamps
            isMarkingStart = false  // 切换到打结束点模式
            saveToHistory()
        }
    }
    
    // 打结束点
    fun markEnd() {
        if (currentAlignIndex < lyricLines.size) {
            val newTimestamps = lineTimestamps.toMutableList()
            while (newTimestamps.size <= currentAlignIndex) {
                newTimestamps.add(LineTimestamp())
            }
            newTimestamps[currentAlignIndex] = newTimestamps[currentAlignIndex].copy(endTime = currentPosition)
            lineTimestamps = newTimestamps
            saveToHistory()
            
            // 移动到下一行，并切换回打开始点模式
            if (currentAlignIndex < lyricLines.size - 1) {
                currentAlignIndex++
                isMarkingStart = true
                scope.launch {
                    listState.animateScrollToItem(maxOf(0, currentAlignIndex - 2))
                }
            }
        }
    }
    
    // 重新打开始点
    fun realignStart() {
        if (currentAlignIndex < lyricLines.size) {
            val newTimestamps = lineTimestamps.toMutableList()
            while (newTimestamps.size <= currentAlignIndex) {
                newTimestamps.add(LineTimestamp())
            }
            newTimestamps[currentAlignIndex] = newTimestamps[currentAlignIndex].copy(startTime = currentPosition)
            lineTimestamps = newTimestamps
            saveToHistory()
        }
    }
    
    // 重新打结束点
    fun realignEnd() {
        if (currentAlignIndex < lyricLines.size) {
            val newTimestamps = lineTimestamps.toMutableList()
            while (newTimestamps.size <= currentAlignIndex) {
                newTimestamps.add(LineTimestamp())
            }
            newTimestamps[currentAlignIndex] = newTimestamps[currentAlignIndex].copy(endTime = currentPosition)
            lineTimestamps = newTimestamps
            saveToHistory()
        }
    }
    
    // 构建最终的 LrcData
    fun buildLrcData(): LrcData {
        val lines = lyricLines.mapIndexed { index, text ->
            val ts = lineTimestamps.getOrElse(index) { LineTimestamp() }
            // 如果结束时间未设置，使用下一行的开始时间或当前开始+5秒
            val endTime = if (ts.endTime > 0) ts.endTime else {
                lineTimestamps.getOrNull(index + 1)?.startTime?.takeIf { it > 0 } 
                    ?: (ts.startTime + 5000L)
            }
            LrcLine(startTime = ts.startTime, endTime = endTime, text = text)
        }
        return LrcData(lines = lines)
    }
    
    Dialog(
        onDismissRequest = {
            mediaPlayer?.release()
            mediaPlayer = null
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏
                TopAppBar(
                    title = { 
                        Text(when (currentStep) {
                            1 -> "输入歌词"
                            2 -> "时间对齐"
                            else -> "预览确认"
                        })
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            mediaPlayer?.release()
                            mediaPlayer = null
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, "关闭")
                        }
                    },
                    actions = {
                        // 步骤指示器
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 16.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                bgm.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "时长: ${formatTime(duration)}",
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
                            onNext = {
                                // 解析歌词行
                                lyricLines = lyricsText.lines()
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                // 初始化时间戳（每行都有开始和结束）
                                lineTimestamps = List(lyricLines.size) { LineTimestamp() }
                                currentAlignIndex = 0
                                isMarkingStart = true
                                currentStep = 2
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        )
                    }
                    2 -> {
                        // 步骤2：时间对齐
                        AlignmentStep(
                            lyricLines = lyricLines,
                            lineTimestamps = lineTimestamps,
                            currentAlignIndex = currentAlignIndex,
                            isMarkingStart = isMarkingStart,
                            currentPosition = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            listState = listState,
                            formatTime = ::formatTime,
                            onMarkStart = ::markStart,
                            onMarkEnd = ::markEnd,
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
                            onSelectLine = { index ->
                                currentAlignIndex = index
                                isMarkingStart = true  // 选中新行时重置为打开始点
                            },
                            onUndo = ::undo,
                            onRedo = ::redo,
                            onRealignStart = ::realignStart,
                            onRealignEnd = ::realignEnd,
                            canUndo = historyIndex > 0,
                            canRedo = historyIndex < historyStack.size - 1,
                            onBack = { currentStep = 1 },
                            onNext = {
                                mediaPlayer?.pause()
                                isPlaying = false
                                // 重置播放位置到开头
                                mediaPlayer?.seekTo(0)
                                currentPosition = 0L
                                currentStep = 3
                            },
                            modifier = Modifier
                                .weight(1f)
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
                            onBack = { currentStep = 2 },
                            onSave = {
                                val lrcData = buildLrcData()
                                // 保存到文件
                                scope.launch {
                                    BgmStorage.saveLrc(context, bgm.path, lrcData)
                                }
                                mediaPlayer?.release()
                                mediaPlayer = null
                                onSave(lrcData)
                            },
                            modifier = Modifier
                                .weight(1f)
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
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "请输入歌词文本，每行一句：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 歌词输入框
        OutlinedTextField(
            value = lyricsText,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { 
                Text(
                    "在这里粘贴或输入歌词...\n\n示例：\n♪ 前奏\n第一句歌词\n第二句歌词\n♪ 间奏\n继续歌词...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ) 
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 统计信息
        val lineCount = lyricsText.lines().filter { it.trim().isNotEmpty() }.size
        Text(
            "共 $lineCount 行歌词",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 下一步按钮
        Button(
            onClick = onNext,
            enabled = lineCount > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("下一步：时间对齐")
            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.padding(start = 8.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 步骤2：时间对齐（优化版交互）
 */
@Composable
private fun AlignmentStep(
    lyricLines: List<String>,
    lineTimestamps: List<LineTimestamp>,
    currentAlignIndex: Int,
    isMarkingStart: Boolean,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    formatTime: (Long) -> String,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onPlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onRewind: () -> Unit,
    onSelectLine: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRealignStart: () -> Unit,
    onRealignEnd: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedLines = lineTimestamps.count { it.startTime > 0 && it.endTime > 0 }
    val allComplete = lineTimestamps.size >= lyricLines.size && 
                      lineTimestamps.all { it.startTime > 0 && it.endTime > 0 }
    
    Column(modifier = modifier) {
        // 顶部状态栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 进度
                Text(
                    "$completedLines/${lyricLines.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = completedLines.toFloat() / lyricLines.size.coerceAtLeast(1),
                    modifier = Modifier.weight(1f).height(6.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                // 撤销/重做
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Undo, "撤销", modifier = Modifier.size(18.dp),
                        tint = if (canUndo) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
                IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Redo, "重做", modifier = Modifier.size(18.dp),
                        tint = if (canRedo) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 歌词列表
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(lyricLines) { index, line ->
                val ts = lineTimestamps.getOrNull(index)
                val hasStart = (ts?.startTime ?: 0L) > 0
                val hasEnd = (ts?.endTime ?: 0L) > 0
                val isCurrent = index == currentAlignIndex
                
                LyricLineCard(
                    index = index,
                    line = line,
                    startTime = ts?.startTime ?: 0L,
                    endTime = ts?.endTime ?: 0L,
                    hasStart = hasStart,
                    hasEnd = hasEnd,
                    isCurrent = isCurrent,
                    isMarkingStart = isMarkingStart,
                    formatTime = formatTime,
                    onClick = { onSelectLine(index) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // 当前行提示卡片
        val currentTs = lineTimestamps.getOrNull(currentAlignIndex)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isMarkingStart) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "第${currentAlignIndex + 1}行",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        lyricLines.getOrElse(currentAlignIndex) { "" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 开始时间显示
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "开始: ${if ((currentTs?.startTime ?: 0L) > 0) formatTime(currentTs!!.startTime) else "待打"}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 结束时间显示
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "结束: ${if ((currentTs?.endTime ?: 0L) > 0) formatTime(currentTs!!.endTime) else "待打"}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 播放控制区
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 进度条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = onSeek,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
                }
                
                // 播放控制 + 打点按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 后退
                    IconButton(onClick = onRewind) {
                        Icon(Icons.Default.Replay, "后退3秒")
                    }
                    
                    // 播放/暂停
                    FilledIconButton(onClick = onPlay, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // 智能打点按钮 - 根据模式自动切换
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.08f else 1f,
                        label = "scale"
                    )
                    val buttonColor = if (isMarkingStart) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.tertiary
                    
                    Button(
                        onClick = { if (isMarkingStart) onMarkStart() else onMarkEnd() },
                        modifier = Modifier.scale(buttonScale).height(48.dp).width(120.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    ) {
                        Icon(
                            if (isMarkingStart) Icons.Default.FirstPage else Icons.Default.LastPage,
                            null, modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isMarkingStart) "打开始" else "打结束",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 辅助操作行
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 手动打开始
                    TextButton(
                        onClick = onMarkStart,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.FirstPage, null, modifier = Modifier.size(16.dp))
                        Text("开始", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // 重打开始
                    TextButton(onClick = onRealignStart) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Text("重开始", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // 重打结束
                    TextButton(onClick = onRealignEnd) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Text("重结束", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // 手动打结束
                    TextButton(
                        onClick = onMarkEnd,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.LastPage, null, modifier = Modifier.size(16.dp))
                        Text("结束", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 底部按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("返回")
            }
            
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = allComplete
            ) {
                Text(if (allComplete) "预览" else "未完成")
                if (allComplete) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 歌词行卡片（优化版）
 */
@Composable
private fun LyricLineCard(
    index: Int,
    line: String,
    startTime: Long,
    endTime: Long,
    hasStart: Boolean,
    hasEnd: Boolean,
    isCurrent: Boolean,
    isMarkingStart: Boolean,
    formatTime: (Long) -> String,
    onClick: () -> Unit
) {
    val isComplete = hasStart && hasEnd
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCurrent && isMarkingStart -> MaterialTheme.colorScheme.primaryContainer
            isCurrent && !isMarkingStart -> MaterialTheme.colorScheme.tertiaryContainer
            isComplete -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            hasStart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        label = "bg"
    )
    
    val borderColor = when {
        isCurrent -> if (isMarkingStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
        else -> Color.Transparent
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCurrent) Modifier.border(2.dp, borderColor, RoundedCornerShape(10.dp)) else Modifier)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号 + 状态
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = when {
                            isComplete -> MaterialTheme.colorScheme.primary
                            hasStart -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            isCurrent -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isComplete) {
                    Icon(Icons.Default.Check, null, 
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent || hasStart) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // 歌词文本
            Text(
                line,
                style = if (isCurrent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCurrent -> if (isMarkingStart) MaterialTheme.colorScheme.onPrimaryContainer 
                                 else MaterialTheme.colorScheme.onTertiaryContainer
                    isComplete -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // 时间标签
            if (hasStart || hasEnd) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasStart) {
                        Text(
                            formatTime(startTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (hasStart && hasEnd) {
                        Text(" - ", style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.outline)
                    }
                    if (hasEnd) {
                        Text(
                            formatTime(endTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else if (isCurrent) {
                Text(
                    if (isMarkingStart) "⏱ 待开始" else "⏱ 待结束",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * 步骤3：预览确认（支持字幕主题选择）
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 当前高亮的行
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    
    // 字幕主题状态
    var selectedTheme by remember { mutableStateOf(PresetLrcThemes.themes.first()) }
    var showThemeSelector by remember { mutableStateOf(false) }
    
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
    
    // 解析颜色
    fun parseColor(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color.White
        }
    }
    
    Column(modifier = modifier) {
        // 主题选择器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "预览 LRC 效果",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 字幕主题下拉菜单
            ExposedDropdownMenuBox(
                expanded = showThemeSelector,
                onExpandedChange = { showThemeSelector = it }
            ) {
                Surface(
                    modifier = Modifier
                        .menuAnchor()
                        .clickable { showThemeSelector = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Palette,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            selectedTheme.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                ExposedDropdownMenu(
                    expanded = showThemeSelector,
                    onDismissRequest = { showThemeSelector = false }
                ) {
                    PresetLrcThemes.themes.forEach { theme ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 主题预览色块
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                parseColor(theme.highlightColor),
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(theme.name)
                                    if (theme.id == selectedTheme.id) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedTheme = theme
                                showThemeSelector = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 歌词预览（带丰富动画的主题样式）
        val bgColor = parseColor(selectedTheme.backgroundColor)
        val textColor = parseColor(selectedTheme.textColor)
        val highlightColor = parseColor(selectedTheme.highlightColor)
        val strokeColor = selectedTheme.strokeColor?.let { parseColor(it) }
        
        // 无限循环动画控制器
        val infiniteTransition = rememberInfiniteTransition(label = "lrc_anim")
        
        // 全局脉冲动画 (用于发光效果)
        val pulseAnim by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        
        // 扫光动画 (霓虹、卡拉OK)
        val sweepAnim by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sweep"
        )
        
        // 呼吸动画 (月光)
        val breatheAnim by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathe"
        )
        
        // 火焰跳动 (烈焰)
        val flameAnim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flame"
        )
        
        // 星星闪烁 (星河)
        val starAnim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "star"
        )
        
        // 根据主题生成特色背景渐变
        val themeGradient = when (selectedTheme.id) {
            "galaxy" -> listOf(Color(0xFF0D1B2D), Color(0xFF1A0A2E), Color(0xFF16213E))
            "karaoke" -> listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F0F1A))
            "cyberpunk" -> listOf(Color(0xFF0A0A14), Color(0xFF120520), Color(0xFF050510))
            "moonlight" -> listOf(Color(0xFF101820), Color(0xFF1C2833), Color(0xFF0D1117))
            "golden" -> listOf(Color(0xFF1A1208), Color(0xFF2D1F0A), Color(0xFF0F0A04))
            "ocean" -> listOf(Color(0xFF001A28), Color(0xFF00252E), Color(0xFF001018))
            "sakura" -> listOf(Color(0xFF180810), Color(0xFF2A1520), Color(0xFF100508))
            "inferno" -> listOf(Color(0xFF100800), Color(0xFF1A0A00), Color(0xFF0A0400))
            else -> listOf(Color.Black.copy(alpha = 0.95f), bgColor, Color.Black.copy(alpha = 0.95f))
        }
        
        // 主预览区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(colors = themeGradient))
        ) {
            // ========== 背景粒子特效 ==========
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val w = size.width
                val h = size.height
                
                when (selectedTheme.id) {
                    // 星河 - 星星闪烁
                    "galaxy" -> {
                        repeat(30) { i ->
                            val x = (i * 37 + starAnim * 2) % w
                            val y = (i * 53 + sin(starAnim * 0.1f + i) * 20) % h
                            val starSize = (3 + sin(starAnim * 0.05f + i * 0.5f) * 2).toFloat()
                            val alpha = (0.3f + sin(starAnim * 0.1f + i) * 0.3f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = starSize,
                                center = Offset(x, y)
                            )
                        }
                    }
                    // 樱花 - 花瓣飘落
                    "sakura" -> {
                        repeat(15) { i ->
                            val x = (i * 47 + starAnim * 0.5f + sin(starAnim * 0.02f + i) * 30) % w
                            val y = (starAnim * 1.5f + i * 60) % (h + 50) - 25
                            val alpha = (0.4f + sin(starAnim * 0.05f + i) * 0.2f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color(0xFFFFB7C5).copy(alpha = alpha),
                                radius = 4f + sin(i.toFloat()) * 2f,
                                center = Offset(x, y)
                            )
                        }
                    }
                    // 深海 - 气泡上升
                    "ocean" -> {
                        repeat(12) { i ->
                            val x = (i * 67) % w
                            val y = h - ((starAnim * 2f + i * 80) % (h + 40))
                            val bubbleSize = 3f + (i % 3) * 2f
                            val alpha = (0.3f + sin(starAnim * 0.1f + i) * 0.15f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color(0xFF4DD0E1).copy(alpha = alpha),
                                radius = bubbleSize,
                                center = Offset(x, y)
                            )
                        }
                    }
                    // 烈焰 - 火星飞舞
                    "inferno" -> {
                        repeat(20) { i ->
                            val x = w / 2 + sin(starAnim * 0.05f + i * 0.8f) * (w * 0.4f)
                            val y = h - ((starAnim * 3f + i * 50) % (h * 0.8f))
                            val sparkSize = 2f + flameAnim * 2f + (i % 3)
                            val alpha = (0.5f + flameAnim * 0.3f - (h - y) / h * 0.5f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color(0xFFFF6D00).copy(alpha = alpha),
                                radius = sparkSize,
                                center = Offset(x, y)
                            )
                        }
                    }
                    // 金曲 - 金色光点
                    "golden" -> {
                        repeat(15) { i ->
                            val x = (i * 57 + sin(starAnim * 0.03f + i) * 20) % w
                            val y = (i * 43 + cos(starAnim * 0.03f + i) * 20) % h
                            val alpha = (0.2f + sin(starAnim * 0.08f + i * 0.7f) * 0.25f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = alpha),
                                radius = 3f + sin(starAnim * 0.05f + i) * 1.5f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
            
            // ========== 动态光效层 ==========
            if (selectedTheme.shadowEnabled) {
                // 脉冲光晕
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(pulseAnim * 0.15f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(highlightColor, Color.Transparent),
                                center = Offset(Float.POSITIVE_INFINITY / 2, Float.POSITIVE_INFINITY / 2.5f),
                                radius = 600f
                            )
                        )
                )
            }
            
            // ========== 歌词列表 ==========
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lrcData.lines) { index, line ->
                    // 动画化的当前行检测
                    val isCurrent = index == currentLineIndex
                    val distance = kotlin.math.abs(index - currentLineIndex)
                    
                    // ===== 丝滑动画 =====
                    // 缩放动画
                    val scale by animateFloatAsState(
                        targetValue = when {
                            isCurrent -> 1.12f
                            distance == 1 -> 1.02f
                            else -> 0.95f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "scale_$index"
                    )
                    
                    // 透明度动画
                    val alpha by animateFloatAsState(
                        targetValue = when {
                            isCurrent -> 1f
                            distance == 1 -> 0.65f
                            distance == 2 -> 0.35f
                            else -> 0.15f
                        },
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        label = "alpha_$index"
                    )
                    
                    // 颜色动画
                    val animatedColor by animateColorAsState(
                        targetValue = if (isCurrent) highlightColor else textColor.copy(alpha = alpha),
                        animationSpec = tween(300),
                        label = "color_$index"
                    )
                    
                    // 位移动画 (当前行微微上浮)
                    val offsetY by animateFloatAsState(
                        targetValue = if (isCurrent) -8f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "offset_$index"
                    )
                    
                    // 字体大小
                    val fontSize = when {
                        isCurrent -> (selectedTheme.fontSize + 8).sp
                        distance == 1 -> (selectedTheme.fontSize + 2).sp
                        else -> (selectedTheme.fontSize - 1).sp
                    }
                    
                    // 当前行容器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = offsetY
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // ===== 当前行特效 =====
                        if (isCurrent) {
                            // 外层脉冲光晕
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .scale(pulseAnim * 0.3f + 0.85f)
                                    .alpha(pulseAnim * 0.5f + 0.3f)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                highlightColor.copy(alpha = 0.4f),
                                                highlightColor.copy(alpha = 0.1f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            
                            // 扫光效果 (卡拉OK / 霓虹夜)
                            if (selectedTheme.id in listOf("karaoke", "cyberpunk", "golden")) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .graphicsLayer { 
                                            translationX = (sweepAnim - 0.5f) * size.width 
                                        }
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    highlightColor.copy(alpha = 0.6f),
                                                    Color.White.copy(alpha = 0.8f),
                                                    highlightColor.copy(alpha = 0.6f),
                                                    Color.Transparent
                                                ),
                                                startX = 0f,
                                                endX = 200f
                                            )
                                        )
                                )
                            }
                            
                            // 月光呼吸 (月光主题)
                            if (selectedTheme.id == "moonlight") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .scale(breatheAnim)
                                        .alpha(0.3f)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.5f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }
                            
                            // 火焰效果 (烈焰主题)
                            if (selectedTheme.id == "inferno") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .graphicsLayer {
                                            scaleY = 1f + flameAnim * 0.15f
                                            translationY = -flameAnim * 5f
                                        }
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFFFF6D00).copy(alpha = 0.3f * pulseAnim),
                                                    Color(0xFFFF3D00).copy(alpha = 0.5f * pulseAnim),
                                                    Color(0xFFDD2C00).copy(alpha = 0.2f)
                                                )
                                            )
                                        )
                                )
                            }
                        }
                        
                        // ===== 歌词文字 =====
                        Text(
                            text = line.text,
                            fontSize = fontSize,
                            fontWeight = when {
                                isCurrent -> FontWeight.Black
                                distance == 1 -> FontWeight.SemiBold
                                else -> FontWeight.Normal
                            },
                            color = animatedColor,
                            textAlign = TextAlign.Center,
                            letterSpacing = if (isCurrent) 3.sp else 0.5.sp,
                            lineHeight = fontSize * 1.5f,
                            style = TextStyle(
                                shadow = when {
                                    isCurrent -> Shadow(
                                        color = highlightColor.copy(alpha = 0.9f),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 25f + pulseAnim * 10f
                                    )
                                    selectedTheme.shadowEnabled -> Shadow(
                                        color = Color.Black.copy(alpha = 0.9f),
                                        offset = Offset(2f, 3f),
                                        blurRadius = 8f
                                    )
                                    else -> null
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // ========== 顶部渐变遮罩 ==========
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                themeGradient.first(),
                                themeGradient.first().copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // ========== 底部渐变遮罩 ==========
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                themeGradient.last().copy(alpha = 0.6f),
                                themeGradient.last()
                            )
                        )
                    )
            )
            
            // ========== 动态边框 ==========
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                highlightColor.copy(alpha = 0.5f * pulseAnim),
                                highlightColor.copy(alpha = 0.1f),
                                highlightColor.copy(alpha = 0.3f),
                                highlightColor.copy(alpha = 0.1f),
                                highlightColor.copy(alpha = 0.5f * pulseAnim)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 播放控制
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledIconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 底部按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("返回修改")
            }
            
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存 LRC")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
