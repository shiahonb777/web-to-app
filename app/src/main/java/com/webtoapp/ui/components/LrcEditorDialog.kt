package com.webtoapp.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
 * LRC 编辑器对话框
 * 支持编辑歌词文本和时间戳，实时预览效果
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LrcEditorDialog(
    bgm: BgmItem,
    lrcData: LrcData,
    onDismiss: () -> Unit,
    onSave: (LrcData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 编辑中的歌词行列表
    var editingLines by remember { 
        mutableStateOf(lrcData.lines.map { EditableLrcLine(it.startTime, it.text) }) 
    }
    
    // 当前选中编辑的行
    var selectedLineIndex by remember { mutableIntStateOf(-1) }
    
    // Play器状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // 当前高亮行
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    
    // List状态
    val listState = rememberLazyListState()
    
    // Yes否有未保存的修改
    var hasChanges by remember { mutableStateOf(false) }
    
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
            android.util.Log.e("LrcEditor", "初始化播放器失败", e)
        }
    }
    
    // Update播放进度和高亮行
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPosition = mp.currentPosition.toLong()
                    // Update高亮行
                    val newIndex = editingLines.indexOfLast { it.startTime <= currentPosition }
                    if (newIndex != currentLineIndex && newIndex >= 0) {
                        currentLineIndex = newIndex
                        // Auto滚动
                        scope.launch {
                            listState.animateScrollToItem(maxOf(0, newIndex - 2))
                        }
                    }
                }
            }
            delay(50)
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
    
    // Parse时间字符串
    fun parseTime(timeStr: String): Long? {
        val regex = Regex("""(\d{1,2}):(\d{2})\.(\d{2})""")
        val match = regex.find(timeStr) ?: return null
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: return null
        val millis = match.groupValues[3].toLongOrNull() ?: return null
        return minutes * 60000 + seconds * 1000 + millis * 10
    }
    
    // Build LrcData
    fun buildLrcData(): LrcData {
        val sortedLines = editingLines.sortedBy { it.startTime }
        val lines = sortedLines.mapIndexed { index, line ->
            val endTime = sortedLines.getOrNull(index + 1)?.startTime ?: (line.startTime + 5000)
            LrcLine(startTime = line.startTime, endTime = endTime, text = line.text)
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
                    title = { Text("编辑 LRC") },
                    navigationIcon = {
                        IconButton(onClick = {
                            mediaPlayer?.release()
                            mediaPlayer = null
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    actions = {
                        if (hasChanges) {
                            TextButton(
                                onClick = {
                                    val newLrcData = buildLrcData()
                                    scope.launch {
                                        BgmStorage.saveLrc(context, bgm.path, newLrcData)
                                    }
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    onSave(newLrcData)
                                }
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("保存")
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
                            Text(bgm.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${editingLines.size} 行歌词",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 添加新行按钮
                        FilledTonalIconButton(
                            onClick = {
                                val newLine = EditableLrcLine(currentPosition, "")
                                editingLines = editingLines + newLine
                                hasChanges = true
                                selectedLineIndex = editingLines.size - 1
                            }
                        ) {
                            Icon(Icons.Default.Add, "添加行")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Lyrics编辑列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(editingLines) { index, line ->
                        val isCurrentPlaying = index == currentLineIndex && isPlaying
                        val isSelected = index == selectedLineIndex
                        
                        LrcLineEditor(
                            index = index,
                            line = line,
                            isCurrentPlaying = isCurrentPlaying,
                            isSelected = isSelected,
                            formatTime = ::formatTime,
                            parseTime = ::parseTime,
                            onSelect = {
                                selectedLineIndex = if (selectedLineIndex == index) -1 else index
                            },
                            onUpdateText = { newText ->
                                val newLines = editingLines.toMutableList()
                                newLines[index] = line.copy(text = newText)
                                editingLines = newLines
                                hasChanges = true
                            },
                            onUpdateTime = { newTime ->
                                val newLines = editingLines.toMutableList()
                                newLines[index] = line.copy(startTime = newTime)
                                editingLines = newLines
                                hasChanges = true
                            },
                            onAdjustTime = { delta ->
                                val newLines = editingLines.toMutableList()
                                val newTime = maxOf(0L, line.startTime + delta)
                                newLines[index] = line.copy(startTime = newTime)
                                editingLines = newLines
                                hasChanges = true
                            },
                            onSetCurrentTime = {
                                val newLines = editingLines.toMutableList()
                                newLines[index] = line.copy(startTime = currentPosition)
                                editingLines = newLines
                                hasChanges = true
                            },
                            onSeekTo = {
                                mediaPlayer?.seekTo(line.startTime.toInt())
                                currentPosition = line.startTime
                            },
                            onDelete = {
                                editingLines = editingLines.filterIndexed { i, _ -> i != index }
                                hasChanges = true
                                if (selectedLineIndex == index) selectedLineIndex = -1
                            },
                            onMoveUp = if (index > 0) {
                                {
                                    val newLines = editingLines.toMutableList()
                                    val temp = newLines[index]
                                    newLines[index] = newLines[index - 1]
                                    newLines[index - 1] = temp
                                    editingLines = newLines
                                    hasChanges = true
                                    selectedLineIndex = index - 1
                                }
                            } else null,
                            onMoveDown = if (index < editingLines.size - 1) {
                                {
                                    val newLines = editingLines.toMutableList()
                                    val temp = newLines[index]
                                    newLines[index] = newLines[index + 1]
                                    newLines[index + 1] = temp
                                    editingLines = newLines
                                    hasChanges = true
                                    selectedLineIndex = index + 1
                                }
                            } else null
                        )
                    }
                    
                    // 底部留白
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
                
                // Play控制区
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp
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
                                fontFamily = FontFamily.Monospace
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                onValueChange = { progress ->
                                    val newPos = (progress * duration).toLong()
                                    mediaPlayer?.seekTo(newPos.toInt())
                                    currentPosition = newPos
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                formatTime(duration),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        // 控制按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 后退5秒
                            IconButton(onClick = {
                                mediaPlayer?.let { mp ->
                                    val newPos = maxOf(0, mp.currentPosition - 5000)
                                    mp.seekTo(newPos)
                                    currentPosition = newPos.toLong()
                                }
                            }) {
                                Icon(Icons.Default.Replay5, "后退5秒")
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Play/暂停
                            FilledIconButton(
                                onClick = {
                                    mediaPlayer?.let { mp ->
                                        if (isPlaying) {
                                            mp.pause()
                                        } else {
                                            mp.start()
                                        }
                                        isPlaying = !isPlaying
                                    }
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "暂停" else "播放",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // 前进5秒
                            IconButton(onClick = {
                                mediaPlayer?.let { mp ->
                                    val newPos = minOf(mp.duration, mp.currentPosition + 5000)
                                    mp.seekTo(newPos)
                                    currentPosition = newPos.toLong()
                                }
                            }) {
                                Icon(Icons.Default.Forward5, "前进5秒")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 可编辑的 LRC 行
 */
data class EditableLrcLine(
    val startTime: Long,
    val text: String
)

/**
 * LRC 行编辑器
 */
@Composable
private fun LrcLineEditor(
    index: Int,
    line: EditableLrcLine,
    isCurrentPlaying: Boolean,
    isSelected: Boolean,
    formatTime: (Long) -> String,
    parseTime: (String) -> Long?,
    onSelect: () -> Unit,
    onUpdateText: (String) -> Unit,
    onUpdateTime: (Long) -> Unit,
    onAdjustTime: (Long) -> Unit,
    onSetCurrentTime: () -> Unit,
    onSeekTo: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            isCurrentPlaying -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        label = "bg"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 基本信息行
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 行号
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
                
                // Time戳（可点击跳转）
                Surface(
                    modifier = Modifier.clickable { onSeekTo() },
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        "[${formatTime(line.startTime)}]",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Lyrics文本（展开时可编辑）
                if (isSelected) {
                    OutlinedTextField(
                        value = line.text,
                        onValueChange = onUpdateText,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                } else {
                    Text(
                        line.text.ifEmpty { "(空行)" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (line.text.isEmpty()) 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Play指示
                if (isCurrentPlaying) {
                    Icon(
                        Icons.Default.GraphicEq,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Expand的编辑选项
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Time调整区
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "时间调整:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // -1秒
                    FilledTonalIconButton(
                        onClick = { onAdjustTime(-1000) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-1s", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    // -0.1秒
                    FilledTonalIconButton(
                        onClick = { onAdjustTime(-100) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-.1", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    // +0.1秒
                    FilledTonalIconButton(
                        onClick = { onAdjustTime(100) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+.1", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    // +1秒
                    FilledTonalIconButton(
                        onClick = { onAdjustTime(1000) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+1s", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 设为当前时间
                    TextButton(
                        onClick = onSetCurrentTime,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("设为当前", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 上移
                    if (onMoveUp != null) {
                        OutlinedIconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, "上移", modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    // 下移
                    if (onMoveDown != null) {
                        OutlinedIconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "下移", modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Delete
                    OutlinedIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.Delete, "Delete", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
