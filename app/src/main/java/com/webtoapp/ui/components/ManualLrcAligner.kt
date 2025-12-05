package com.webtoapp.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
    
    // 歌词文本（每行一句）
    var lyricsText by remember { 
        mutableStateOf(existingLrc?.lines?.joinToString("\n") { it.text } ?: "") 
    }
    
    // 解析后的歌词行
    var lyricLines by remember { 
        mutableStateOf(existingLrc?.lines?.map { it.text } ?: emptyList()) 
    }
    
    // 每行的时间戳（毫秒）
    var timestamps by remember { 
        mutableStateOf(existingLrc?.lines?.map { it.startTime } ?: emptyList<Long>()) 
    }
    
    // 当前正在对齐的行索引
    var currentAlignIndex by remember { mutableIntStateOf(0) }
    
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
    
    // 处理对齐点击
    fun onAlignClick() {
        if (currentAlignIndex < lyricLines.size) {
            val newTimestamps = timestamps.toMutableList()
            // 确保列表足够长
            while (newTimestamps.size <= currentAlignIndex) {
                newTimestamps.add(0L)
            }
            newTimestamps[currentAlignIndex] = currentPosition
            timestamps = newTimestamps
            
            // 移动到下一行
            if (currentAlignIndex < lyricLines.size - 1) {
                currentAlignIndex++
                // 滚动到当前行
                scope.launch {
                    listState.animateScrollToItem(maxOf(0, currentAlignIndex - 2))
                }
            }
        }
    }
    
    // 构建最终的 LrcData
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
                                // 初始化时间戳
                                timestamps = List(lyricLines.size) { 0L }
                                currentAlignIndex = 0
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
                            timestamps = timestamps,
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
                                val newTimestamps = timestamps.toMutableList()
                                newTimestamps[index] = newTime
                                timestamps = newTimestamps
                            },
                            onSelectLine = { index ->
                                currentAlignIndex = index
                            },
                            onBack = { currentStep = 1 },
                            onNext = {
                                mediaPlayer?.pause()
                                isPlaying = false
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
 * 步骤2：时间对齐
 */
@Composable
private fun AlignmentStep(
    lyricLines: List<String>,
    timestamps: List<Long>,
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
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 说明
        Text(
            "播放音频，在听到每句歌词开始时点击「打点」按钮",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 歌词列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(lyricLines) { index, line ->
                val isAligned = timestamps.getOrElse(index) { 0L } > 0
                val isCurrent = index == currentAlignIndex
                val timestamp = timestamps.getOrElse(index) { 0L }
                
                AlignmentLineItem(
                    index = index,
                    line = line,
                    timestamp = timestamp,
                    isAligned = isAligned,
                    isCurrent = isCurrent,
                    formatTime = formatTime,
                    onClick = { onSelectLine(index) },
                    onEditTime = { newTime -> onEditTimestamp(index, newTime) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 播放控制区
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 控制按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 后退3秒
                    IconButton(onClick = onRewind) {
                        Icon(Icons.Default.Replay, "后退3秒")
                    }
                    
                    // 播放/暂停
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
                    
                    // 打点按钮（核心功能）
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.1f else 1f,
                        label = "scale"
                    )
                    
                    FilledTonalButton(
                        onClick = onAlignClick,
                        modifier = Modifier
                            .scale(buttonScale)
                            .height(48.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.TouchApp, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打点", fontWeight = FontWeight.Bold)
                    }
                }
                
                // 当前对齐进度
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (currentAlignIndex + 1).toFloat() / lyricLines.size,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "进度: ${currentAlignIndex + 1}/${lyricLines.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                Spacer(modifier = Modifier.width(8.dp))
                Text("上一步")
            }
            
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = timestamps.all { it > 0 }
            ) {
                Text("下一步")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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
    formatTime: (Long) -> String,
    onClick: () -> Unit,
    onEditTime: (Long) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCurrent -> MaterialTheme.colorScheme.primaryContainer
            isAligned -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            
            // 时间戳
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
            
            // 歌词内容
            Text(
                line,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) 
                    MaterialTheme.colorScheme.onPrimaryContainer
                else 
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // 状态图标
            if (isAligned) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isCurrent) {
                Icon(
                    Icons.Default.ArrowForward,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
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
    onBack: () -> Unit,
    onSave: () -> Unit,
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
            "预览生成的 LRC 效果，确认无误后保存",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 歌词预览
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
        
        // 播放控制
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                Spacer(modifier = Modifier.width(8.dp))
                Text("返回修改")
            }
            
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存 LRC")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
