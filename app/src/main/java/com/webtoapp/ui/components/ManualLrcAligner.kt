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
 * LRC dialog
 * user input text, button
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
    
    // state: 1=input, 2=, 3=preview
    var currentStep by remember { mutableIntStateOf(if (existingLrc != null) 2 else 1) }
    
    // Lyricstext( )
    var lyricsText by remember { 
        mutableStateOf(existingLrc?.lines?.joinToString("\n") { it.text } ?: "") 
    }
    
    // Parse
    var lyricLines by remember { 
        mutableStateOf(existingLrc?.lines?.map { it.text } ?: emptyList()) 
    }
    
    // Note
    var timestamps by remember { 
        mutableStateOf(existingLrc?.lines?.map { it.startTime } ?: emptyList<Long>()) 
    }
    
    // ( timestamp > 0, from 0ms)
    var alignedIndices by remember {
        mutableStateOf(
            if (existingLrc != null) (existingLrc.lines.indices).toSet() else emptySet<Int>()
        )
    }
    
    // current
    var currentAlignIndex by remember { mutableIntStateOf(0) }
    
    // statemanagement( /)
    data class AlignState(val timestamps: List<Long>, val currentIndex: Int, val aligned: Set<Int>)
    var historyStack by remember { mutableStateOf(listOf<AlignState>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    
    // Savestate
    fun saveToHistory() {
        val newState = AlignState(timestamps.toList(), currentAlignIndex, alignedIndices.toSet())
        // if, delete
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
    
    // Play state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // Listscrollstate
    val listState = rememberLazyListState()
    
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
            AppLogger.e("ManualLrcAligner", "初始化播放器失败", e)
        }
    }
    
    // Update
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPosition = mp.currentPosition.toLong()
                }
            }
            delay(50) // 50ms update,
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
    
    // Handle
    fun onAlignClick() {
        if (currentAlignIndex < lyricLines.size) {
            // Savecurrentstate( )
            saveToHistory()
            
            val newTimestamps = timestamps.toMutableList()
            // ensurelist
            while (newTimestamps.size <= currentAlignIndex) {
                newTimestamps.add(0L)
            }
            newTimestamps[currentAlignIndex] = currentPosition
            timestamps = newTimestamps
            // current
            alignedIndices = alignedIndices + currentAlignIndex
            
            // Note
            if (currentAlignIndex < lyricLines.size - 1) {
                currentAlignIndex++
                // scroll current
                scope.launch {
                    listState.animateScrollToItem(maxOf(0, currentAlignIndex - 2))
                }
            }
            
            // Save state
            saveToHistory()
        }
    }
    
    // Build LrcData
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
                // button
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
                            // 1: closebutton
                            IconButton(onClick = {
                                mediaPlayer?.release()
                                mediaPlayer = null
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, com.webtoapp.core.i18n.Strings.close)
                            }
                        } else {
                            // 2/3: back
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
                        // indicator
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
                        
                        // /savebutton
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
                
                // contentarea
                when (currentStep) {
                    1 -> {
                        // 1: input
                        LyricsInputStep(
                            lyricsText = lyricsText,
                            onTextChange = { lyricsText = it },
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .padding(horizontal = 16.dp)
                        )
                    }
                    2 -> {
                        // 2
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
                        // 3: preview
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
 * 1: input
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
        
        // Lyricsinput
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
        
        // Note
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
 * 2
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
        // Note
        Text(
            com.webtoapp.core.i18n.Strings.alignmentHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // current( )
        val scope = rememberCoroutineScope()
        var playingLineIndex by remember { mutableIntStateOf(-1) }
        
        LaunchedEffect(currentPosition, alignedIndices) {
            if (alignedIndices.isEmpty()) {
                playingLineIndex = -1
                return@LaunchedEffect
            }
            // , timestamp <= currentPosition
            val newIndex = timestamps.indices
                .filter { it in alignedIndices }
                .lastOrNull { timestamps[it] <= currentPosition }
                ?: -1
            if (newIndex != playingLineIndex && newIndex >= 0) {
                playingLineIndex = newIndex
                // scroll current( user select)
                if (isPlaying) {
                    scope.launch {
                        listState.animateScrollToItem(maxOf(0, newIndex - 2))
                    }
                }
            }
        }
        
        // Lyricslist
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
        
        // Play
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // Note
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
                
                // button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undobutton
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
                    
                    // 3
                    IconButton(onClick = onRewind) {
                        Icon(Icons.Default.Replay, com.webtoapp.core.i18n.Strings.rewind3s)
                    }
                    
                    // Play/
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
                    
                    // button( )
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.1f else 1f,
                        label = "scale"
                    )
                    
                    FilledTonalButton(
                        onClick = {
                            // ensure handle
                            onAlignClick()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 100.dp) // ensure area
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
                    
                    // button- clearcurrent
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
                    
                    // Redobutton
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
                
                // current
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
 * item
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
            // Note
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlayingLine) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            
            // Time
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
            
            // Lyricscontent
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
            
            // stateicon
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
 * 3: preview
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
    
    // current
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    
    // update
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
        
        // Lyricspreview
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
        
        // Play
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
