package com.webtoapp.ui.components

import android.media.MediaPlayer
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.data.model.BgmItem
import com.webtoapp.data.model.LrcData
import com.webtoapp.data.model.LrcLine
import com.webtoapp.util.BgmStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Composable parseTime( ) ,
private val LRC_PARSE_TIME_REGEX = Regex("""(\d{1,2}):(\d{2})\.(\d{2})""")

/**
 * LRC edit dialog
 * supportedit text, preview
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
    
    // editin list
    var editingLines by remember { 
        mutableStateOf(lrcData.lines.map { EditableLrcLine(it.startTime, it.text) }) 
    }
    
    // current edit
    var selectedLineIndex by remember { mutableIntStateOf(-1) }
    
    // Play state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // current
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    
    // Liststate
    val listState = rememberLazyListState()
    
    // Yes save
    var hasChanges by remember { mutableStateOf(false) }
    
    // Initialize
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
            AppLogger.e("LrcEditor", "初始化播放器失败", e)
        }
    }
    
    // Update
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPosition = mp.currentPosition.toLong()
                    // Update
                    val newIndex = editingLines.indexOfLast { it.startTime <= currentPosition }
                    if (newIndex != currentLineIndex && newIndex >= 0) {
                        currentLineIndex = newIndex
                        // Autoscroll
                        scope.launch {
                            listState.animateScrollToItem(maxOf(0, newIndex - 2))
                        }
                    }
                }
            }
            delay(50)
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    
    // Note
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(minutes, seconds, millis)
    }
    
    // Parse
    fun parseTime(timeStr: String): Long? {
        val match = LRC_PARSE_TIME_REGEX.find(timeStr) ?: return null
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
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                // Note
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
                
                // Note
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
                            Text(bgm.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${editingLines.size} 行歌词",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // button
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
                
                // Lyricseditlist
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = true)
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
                    
                    // bottom
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
                
                // Play
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Note
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
                                    .weight(weight = 1f, fill = true)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                formatTime(duration),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        // button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 5
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
                            
                            // Play/
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
                                    contentDescription = if (isPlaying) Strings.cdPause else Strings.cdPlay,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // forward5
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
 * edit LRC
 */
data class EditableLrcLine(
    val startTime: Long,
    val text: String
)

/**
 * LRC edit
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
            // Note
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Note
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
                
                // Time( )
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
                
                // Lyricstext( expand edit)
                if (isSelected) {
                    OutlinedTextField(
                        value = line.text,
                        onValueChange = onUpdateText,
                        modifier = Modifier.weight(weight = 1f, fill = true),
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
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                }
                
                // Play
                if (isCurrentPlaying) {
                    Icon(
                        Icons.Default.GraphicEq,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Expand edit
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Time
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
                    
                    // 1
                    FilledTonalIconButton(
                        onClick = { onAdjustTime(-1000) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-1s", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    // 0. 1
                    FilledTonalIconButton(
                        onClick = { onAdjustTime(-100) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-.1", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    // +0. 1
                    FilledTonalIconButton(
                        onClick = { onAdjustTime(100) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+.1", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    // +1
                    FilledTonalIconButton(
                        onClick = { onAdjustTime(1000) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+1s", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                    
                    // current
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
                
                // button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Note
                    if (onMoveUp != null) {
                        OutlinedIconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, "上移", modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    // Note
                    if (onMoveDown != null) {
                        OutlinedIconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "下移", modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                    
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
