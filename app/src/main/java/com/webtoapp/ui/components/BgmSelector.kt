package com.webtoapp.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.util.BgmStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * BGM selector对话框
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BgmSelectorDialog(
    currentConfig: BgmConfig,
    onDismiss: () -> Unit,
    onConfirm: (BgmConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 可用音乐列表
    var availableBgm by remember { mutableStateOf<List<BgmItem>>(emptyList()) }
    
    // 已选播放列表
    var selectedPlaylist by remember { mutableStateOf(currentConfig.playlist) }
    
    // Play模式
    var playMode by remember { mutableStateOf(currentConfig.playMode) }
    
    // Volume
    var volume by remember { mutableFloatStateOf(currentConfig.volume) }
    
    // Auto播放
    var autoPlay by remember { mutableStateOf(currentConfig.autoPlay) }
    
    // Show歌词
    var showLyrics by remember { mutableStateOf(currentConfig.showLyrics) }
    
    // 字幕主题
    var selectedTheme by remember { mutableStateOf(currentConfig.lrcTheme ?: PresetLrcThemes.themes.first()) }
    
    // 当前播放预览的音乐
    var previewingBgm by remember { mutableStateOf<BgmItem?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    // Upload对话框
    var showUploadDialog by remember { mutableStateOf(false) }
    
    // 标签筛选
    var selectedTagFilter by remember { mutableStateOf<BgmTag?>(null) }
    
    // 当前编辑标签的音乐
    var editingTagsBgm by remember { mutableStateOf<BgmItem?>(null) }
    
    // 字幕主题选择对话框
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // LRC 预览对话框
    var showLrcPreviewDialog by remember { mutableStateOf(false) }
    var previewLrcBgm by remember { mutableStateOf<BgmItem?>(null) }
    
    // 手动对齐对话框
    var showManualAlignerDialog by remember { mutableStateOf(false) }
    var manualAlignerBgm by remember { mutableStateOf<BgmItem?>(null) }
    
    // LRC 编辑器对话框
    var showLrcEditorDialog by remember { mutableStateOf(false) }
    var lrcEditorBgm by remember { mutableStateOf<BgmItem?>(null) }
    
    // 拖动排序状态
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var draggedOverItemIndex by remember { mutableIntStateOf(-1) }
    
    // Snackbar 提示状态
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Refresh音乐列表的函数
    val refreshBgmList: () -> Unit = {
        scope.launch {
            withContext(Dispatchers.IO) {
                availableBgm = BgmStorage.scanAllBgm(context)
            }
            // 同步更新已选列表的 LRC 数据
            selectedPlaylist = selectedPlaylist.map { selected ->
                availableBgm.find { it.path == selected.path } ?: selected
            }
        }
    }
    
    // Load音乐列表
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            availableBgm = BgmStorage.scanAllBgm(context)
        }
    }
    
    // Cleanup MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    // 预览播放
    fun previewBgm(bgm: BgmItem) {
        if (previewingBgm == bgm) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            previewingBgm = null
        } else {
            mediaPlayer?.release()
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
                    setOnCompletionListener {
                        previewingBgm = null
                    }
                    prepare()
                    start()
                }
                previewingBgm = bgm
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 标题栏
                    TopAppBar(
                    title = { Text(Strings.selectBgm) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, Strings.close)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                onConfirm(BgmConfig(
                                    playlist = selectedPlaylist.mapIndexed { index, item -> 
                                        item.copy(sortOrder = index) 
                                    },
                                    playMode = playMode,
                                    volume = volume,
                                    autoPlay = autoPlay,
                                    showLyrics = showLyrics,
                                    lrcTheme = if (showLyrics) selectedTheme else null
                                ))
                            }
                        ) {
                            Text(Strings.btnOk)
                        }
                    }
                )
                
                // 内容区
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    // 已选音乐（支持拖动排序）
                    if (selectedPlaylist.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${Strings.selectedMusic} (${selectedPlaylist.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                Strings.clickArrowToReorder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                        ) {
                            itemsIndexed(selectedPlaylist, key = { _, item -> item.id }) { index, bgm ->
                                SelectedBgmItemWithReorder(
                                    bgm = bgm,
                                    index = index,
                                    totalCount = selectedPlaylist.size,
                                    isPlaying = previewingBgm == bgm,
                                    onPlay = { previewBgm(bgm) },
                                    onRemove = {
                                        selectedPlaylist = selectedPlaylist - bgm
                                    },
                                    onEditTags = { editingTagsBgm = bgm },
                                    onGenerateLrc = {
                                        manualAlignerBgm = bgm
                                        showManualAlignerDialog = true
                                    },
                                    onPreviewLrc = if (bgm.lrcData != null) {
                                        {
                                            previewLrcBgm = bgm
                                            showLrcPreviewDialog = true
                                        }
                                    } else null,
                                    onMoveUp = if (index > 0) {
                                        {
                                            val mutableList = selectedPlaylist.toMutableList()
                                            val item = mutableList.removeAt(index)
                                            mutableList.add(index - 1, item)
                                            selectedPlaylist = mutableList
                                        }
                                    } else null,
                                    onMoveDown = if (index < selectedPlaylist.size - 1) {
                                        {
                                            val mutableList = selectedPlaylist.toMutableList()
                                            val item = mutableList.removeAt(index)
                                            mutableList.add(index + 1, item)
                                            selectedPlaylist = mutableList
                                        }
                                    } else null
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // 可用音乐列表
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Strings.availableMusic,
                            style = MaterialTheme.typography.labelMedium
                        )
                        // Upload音乐按钮
                        TextButton(onClick = { showUploadDialog = true }) {
                            Icon(
                                Icons.Outlined.Add,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.uploadMusic)
                        }
                    }
                    
                    // 标签筛选
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTagFilter == null,
                                onClick = { selectedTagFilter = null },
                                label = { Text(Strings.allTag) }
                            )
                        }
                        items(BgmTag.entries.take(10)) { tag ->
                            FilterChip(
                                selected = selectedTagFilter == tag,
                                onClick = { 
                                    selectedTagFilter = if (selectedTagFilter == tag) null else tag 
                                },
                                label = { Text(tag.displayName) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (availableBgm.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.MusicNote,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    Strings.noMusicAvailable,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    Strings.clickToUploadMusic,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // 根据标签筛选
                        val filteredBgm = if (selectedTagFilter != null) {
                            availableBgm.filter { it.tags.contains(selectedTagFilter) }
                        } else {
                            availableBgm
                        }
                        
                        if (filteredBgm.isEmpty() && selectedTagFilter != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    Strings.noMusicWithTag,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(filteredBgm) { bgm ->
                                    val isSelected = selectedPlaylist.any { it.path == bgm.path }
                                    AvailableBgmItem(
                                        bgm = bgm,
                                        isSelected = isSelected,
                                        isPlaying = previewingBgm == bgm,
                                        onPlay = { previewBgm(bgm) },
                                        onSelect = {
                                            if (isSelected) {
                                                selectedPlaylist = selectedPlaylist.filter { it.path != bgm.path }
                                            } else {
                                                selectedPlaylist = selectedPlaylist + bgm
                                            }
                                        },
                                        onEditTags = { editingTagsBgm = bgm },
                                        onGenerateLrc = {
                                            manualAlignerBgm = bgm
                                            showManualAlignerDialog = true
                                        },
                                        onPreviewLrc = if (bgm.lrcData != null) {
                                            {
                                                previewLrcBgm = bgm
                                                showLrcPreviewDialog = true
                                            }
                                        } else null,
                                        onDelete = if (!bgm.isAsset) {
                                            {
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        BgmStorage.deleteBgm(context, bgm)
                                                    }
                                                    availableBgm = availableBgm.filter { it.path != bgm.path }
                                                    selectedPlaylist = selectedPlaylist.filter { it.path != bgm.path }
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 底部设置区（可滚动）
                Divider()
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play模式
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Strings.playMode, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = playMode == BgmPlayMode.LOOP,
                                onClick = { playMode = BgmPlayMode.LOOP },
                                label = { Text(Strings.loopMode, style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = playMode == BgmPlayMode.SEQUENTIAL,
                                onClick = { playMode = BgmPlayMode.SEQUENTIAL },
                                label = { Text(Strings.sequentialMode, style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = playMode == BgmPlayMode.SHUFFLE,
                                onClick = { playMode = BgmPlayMode.SHUFFLE },
                                label = { Text(Strings.shuffleMode, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    
                    // Volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Strings.volume, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                    
                    // Auto播放
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Strings.autoPlay, style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autoPlay, onCheckedChange = { autoPlay = it })
                    }
                    
                    // Show歌词
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Strings.showLyrics, style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showLyrics, onCheckedChange = { showLyrics = it })
                    }
                    
                    // 字幕主题选择
                    if (showLyrics) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(Strings.lyricsTheme, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    selectedTheme.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
                }
                
                // Snackbar 提示（显示在底部）
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    snackbar = { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = if (data.visuals.message.startsWith("✓")) 
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (data.visuals.message.startsWith("✓"))
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                )
            }
        }
    }
    
    // Upload音乐对话框
    if (showUploadDialog) {
        UploadBgmDialog(
            onDismiss = { showUploadDialog = false },
            onUploaded = { newBgm ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        availableBgm = BgmStorage.scanAllBgm(context)
                    }
                }
                showUploadDialog = false
            }
        )
    }
    
    // 编辑标签对话框
    editingTagsBgm?.let { bgm ->
        EditTagsDialog(
            bgm = bgm,
            onDismiss = { editingTagsBgm = null },
            onConfirm = { updatedBgm ->
                availableBgm = availableBgm.map { 
                    if (it.path == updatedBgm.path) updatedBgm else it 
                }
                selectedPlaylist = selectedPlaylist.map {
                    if (it.path == updatedBgm.path) updatedBgm else it
                }
                editingTagsBgm = null
            }
        )
    }
    
    // 字幕主题选择对话框
    if (showThemeDialog) {
        LrcThemeDialog(
            currentTheme = selectedTheme,
            onDismiss = { showThemeDialog = false },
            onSelect = { theme ->
                selectedTheme = theme
                showThemeDialog = false
            }
        )
    }
    
    // LRC 预览对话框
    if (showLrcPreviewDialog && previewLrcBgm != null) {
        LrcPreviewDialog(
            bgm = previewLrcBgm!!,
            onDismiss = {
                showLrcPreviewDialog = false
                previewLrcBgm = null
            },
            onEdit = {
                // 打开 LRC 编辑器
                lrcEditorBgm = previewLrcBgm
                showLrcPreviewDialog = false
                previewLrcBgm = null
                showLrcEditorDialog = true
            }
        )
    }
    
    // 手动对齐对话框
    if (showManualAlignerDialog && manualAlignerBgm != null) {
        ManualLrcAlignerDialog(
            bgm = manualAlignerBgm!!,
            existingLrc = manualAlignerBgm!!.lrcData,
            onDismiss = {
                showManualAlignerDialog = false
                manualAlignerBgm = null
            },
            onSave = { newLrcData ->
                // Update音乐项的 LRC 数据
                val updatedBgm = manualAlignerBgm!!.copy(lrcData = newLrcData)
                availableBgm = availableBgm.map { 
                    if (it.path == updatedBgm.path) updatedBgm else it 
                }
                selectedPlaylist = selectedPlaylist.map {
                    if (it.path == updatedBgm.path) updatedBgm else it
                }
                showManualAlignerDialog = false
                manualAlignerBgm = null
                // Show成功提示
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = Strings.lyricsSaved,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
    
    // LRC 编辑器对话框
    if (showLrcEditorDialog && lrcEditorBgm != null && lrcEditorBgm!!.lrcData != null) {
        LrcEditorDialog(
            bgm = lrcEditorBgm!!,
            lrcData = lrcEditorBgm!!.lrcData!!,
            onDismiss = {
                showLrcEditorDialog = false
                lrcEditorBgm = null
            },
            onSave = { newLrcData ->
                // Update音乐项的 LRC 数据
                val updatedBgm = lrcEditorBgm!!.copy(lrcData = newLrcData)
                availableBgm = availableBgm.map { 
                    if (it.path == updatedBgm.path) updatedBgm else it 
                }
                selectedPlaylist = selectedPlaylist.map {
                    if (it.path == updatedBgm.path) updatedBgm else it
                }
                showLrcEditorDialog = false
                lrcEditorBgm = null
                // Show成功提示
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = Strings.lyricsUpdated,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
}

/**
 * 已选音乐项
 */
@Composable
private fun SelectedBgmItem(
    bgm: BgmItem,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BgmCover(bgm, context, Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                bgm.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) Strings.stop else Strings.play,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = Strings.remove,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 带上下移动按钮的已选音乐项
 */
@Composable
private fun SelectedBgmItemWithReorder(
    bgm: BgmItem,
    index: Int,
    totalCount: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onEditTags: () -> Unit,
    onGenerateLrc: () -> Unit,
    onPreviewLrc: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上下移动按钮
            Column {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = Strings.moveUp,
                        modifier = Modifier.size(20.dp),
                        tint = if (onMoveUp != null) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = Strings.moveDown,
                        modifier = Modifier.size(20.dp),
                        tint = if (onMoveDown != null) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            BgmCoverAdaptive(bgm, context, Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bgm.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (bgm.tags.isNotEmpty()) {
                    Text(
                        bgm.tags.take(2).joinToString(" ") { it.displayName },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            // Lyrics状态指示/预览按钮
            if (bgm.lrcData != null && onPreviewLrc != null) {
                IconButton(onClick = onPreviewLrc, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Subtitles,
                        contentDescription = Strings.previewLyrics,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (bgm.lrcData != null) {
                Icon(
                    Icons.Outlined.Subtitles,
                    contentDescription = Strings.hasLyrics,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onGenerateLrc, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = Strings.aiGenerateLyrics,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onEditTags, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Label,
                    contentDescription = Strings.editTags,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) Strings.stop else Strings.play,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = Strings.remove,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 可用音乐项
 */
@Composable
private fun AvailableBgmItem(
    bgm: BgmItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
    onEditTags: () -> Unit,
    onGenerateLrc: () -> Unit,
    onPreviewLrc: (() -> Unit)? = null,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() },
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelect() }
            )
            BgmCover(bgm, context, Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bgm.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (bgm.isAsset) Strings.presetMusic else Strings.userUploaded,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Lyrics状态指示/预览按钮
            if (bgm.lrcData != null && onPreviewLrc != null) {
                IconButton(onClick = onPreviewLrc, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Subtitles,
                        contentDescription = Strings.previewLyrics,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (bgm.lrcData != null) {
                Icon(
                    Icons.Outlined.Subtitles,
                    contentDescription = Strings.hasLyrics,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onGenerateLrc, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = Strings.aiGenerateLyrics,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) Strings.stop else Strings.preview,
                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = Strings.btnDelete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 封面图组件
 */
@Composable
private fun BgmCover(bgm: BgmItem, context: android.content.Context, modifier: Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (bgm.coverPath != null) {
            val coverData = if (bgm.coverPath.startsWith("asset:///")) {
                "file:///android_asset/${bgm.coverPath.removePrefix("asset:///")}"
            } else {
                File(bgm.coverPath)
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverData)
                    .crossfade(true)
                    .build(),
                contentDescription = "封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.MusicNote,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 自适应封面图组件
 */
@Composable
private fun BgmCoverAdaptive(bgm: BgmItem, context: android.content.Context, modifier: Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (bgm.coverPath != null) {
            val coverData = if (bgm.coverPath.startsWith("asset:///")) {
                "file:///android_asset/${bgm.coverPath.removePrefix("asset:///")}"
            } else {
                File(bgm.coverPath)
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverData)
                    .crossfade(true)
                    .build(),
                contentDescription = "封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.MusicNote,
                    null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 上传音乐对话框
 */
@Composable
private fun UploadBgmDialog(
    onDismiss: () -> Unit,
    onUploaded: (BgmItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var bgmName by remember { mutableStateOf("") }
    var bgmUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var coverUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    
    val bgmPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> 
        uri?.let { 
            bgmUri = it
            if (bgmName.isBlank()) {
                val fileName = uri.lastPathSegment ?: ""
                bgmName = fileName.substringBeforeLast(".").substringAfterLast("/")
            }
        }
    }
    
    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> coverUri = uri }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.uploadMusicTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = bgmName,
                    onValueChange = { bgmName = it },
                    label = { Text(Strings.musicName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { bgmPicker.launch("audio/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.AudioFile, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (bgmUri != null) Strings.selected else Strings.selectMusic)
                    }
                    if (bgmUri != null) {
                        IconButton(onClick = { bgmUri = null }) {
                            Icon(Icons.Default.Close, Strings.clear)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { coverPicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Image, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (coverUri != null) Strings.selected else Strings.selectCoverOptional)
                    }
                    if (coverUri != null) {
                        IconButton(onClick = { coverUri = null }) {
                            Icon(Icons.Default.Close, Strings.clear)
                        }
                    }
                }
                
                Text(
                    Strings.coverTip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val uri = bgmUri ?: return@Button
                    val name = bgmName.ifBlank { "未命名" }
                    
                    isUploading = true
                    scope.launch {
                        try {
                            val savedPath = withContext(Dispatchers.IO) {
                                BgmStorage.saveBgm(context, uri, name)
                            }
                            
                            if (savedPath != null) {
                                val savedCoverPath = coverUri?.let { coverUri ->
                                    withContext(Dispatchers.IO) {
                                        BgmStorage.saveCover(context, coverUri, name)
                                    }
                                }
                                
                                onUploaded(BgmItem(
                                    name = name,
                                    path = savedPath,
                                    coverPath = savedCoverPath,
                                    isAsset = false
                                ))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isUploading = false
                        }
                    }
                },
                enabled = bgmUri != null && !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(Strings.upload)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

/**
 * 标签编辑对话框
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditTagsDialog(
    bgm: BgmItem,
    onDismiss: () -> Unit,
    onConfirm: (BgmItem) -> Unit
) {
    var selectedTags by remember { mutableStateOf(bgm.tags.toSet()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.editTagsTitle) },
        text = {
            Column {
                Text(
                    bgm.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    Strings.selectTagsHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BgmTag.entries.forEach { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = {
                                selectedTags = if (tag in selectedTags) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                            },
                            label = { Text(tag.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(bgm.copy(tags = selectedTags.toList()))
                }
            ) {
                Text(Strings.btnSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

/**
 * 字幕主题选择对话框
 */
@Composable
private fun LrcThemeDialog(
    currentTheme: LrcTheme,
    onDismiss: () -> Unit,
    onSelect: (LrcTheme) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    Strings.selectLyricsTheme,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    Strings.selectLyricsThemeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(PresetLrcThemes.themes) { theme ->
                        LrcThemePreviewCard(
                            theme = theme,
                            isSelected = theme.id == currentTheme.id,
                            onClick = { onSelect(theme) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Strings.btnCancel)
                    }
                }
            }
        }
    }
}

/**
 * 字幕主题预览卡片
 */
@Composable
private fun LrcThemePreviewCard(
    theme: LrcTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(theme.backgroundColor))
    } catch (e: Exception) {
        Color.Black.copy(alpha = 0.5f)
    }
    
    val textColor = try {
        Color(android.graphics.Color.parseColor(theme.textColor))
    } catch (e: Exception) {
        Color.White
    }
    
    val highlightColor = try {
        Color(android.graphics.Color.parseColor(theme.highlightColor))
    } catch (e: Exception) {
        Color.Yellow
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
               else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    theme.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Strings.sampleLyricsText,
                        color = highlightColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Example Lyrics Text",
                        color = textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "动画: ${theme.animationType.displayName} | 位置: ${theme.position.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * LRC 歌词预览对话框
 * 支持播放音乐并同步显示歌词
 */
@Composable
private fun LrcPreviewDialog(
    bgm: BgmItem,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lrcData = bgm.lrcData ?: return onDismiss()
    
    // Play器状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    
    // List滚动状态
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
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
            android.util.Log.e("LrcPreview", "初始化播放器失败", e)
        }
    }
    
    // Update播放进度
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                currentPosition = mp.currentPosition.toLong()
                
                // Find当前歌词行
                val newIndex = lrcData.lines.indexOfLast { it.startTime <= currentPosition }
                if (newIndex != currentLineIndex && newIndex >= 0) {
                    currentLineIndex = newIndex
                    // Auto滚动到当前行
                    scope.launch {
                        listState.animateScrollToItem(
                            index = maxOf(0, newIndex - 2),
                            scrollOffset = 0
                        )
                    }
                }
            }
            delay(100)
        }
    }
    
    // Cleanup播放器
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    
    Dialog(onDismissRequest = {
        mediaPlayer?.release()
        mediaPlayer = null
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            bgm.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${Strings.lyricsPreview} · ${lrcData.lines.size} ${Strings.lines}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 编辑按钮
                    if (onEdit != null) {
                        IconButton(onClick = {
                            mediaPlayer?.release()
                            mediaPlayer = null
                            onEdit()
                        }) {
                            Icon(Icons.Outlined.Edit, Strings.edit)
                        }
                    }
                    
                    IconButton(onClick = {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, Strings.close)
                    }
                }
                
                Divider()
                
                // Lyrics列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.03f)),
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
                
                Divider()
                
                // Play控制
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
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
                            onValueChange = { value ->
                                val newPos = (value * duration).toLong()
                                currentPosition = newPos
                                mediaPlayer?.seekTo(newPos.toInt())
                            },
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
                    
                    // Play按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // 后退 10 秒
                        IconButton(onClick = {
                            mediaPlayer?.let { mp ->
                                val newPos = maxOf(0, mp.currentPosition - 10000)
                                mp.seekTo(newPos)
                                currentPosition = newPos.toLong()
                            }
                        }) {
                            Icon(Icons.Default.Replay10, Strings.backward10s)
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
                                contentDescription = if (isPlaying) Strings.pause else Strings.play,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 前进 10 秒
                        IconButton(onClick = {
                            mediaPlayer?.let { mp ->
                                val newPos = minOf(mp.duration, mp.currentPosition + 10000)
                                mp.seekTo(newPos)
                                currentPosition = newPos.toLong()
                            }
                        }) {
                            Icon(Icons.Default.Forward10, Strings.forward10s)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 格式化时间（毫秒 -> mm:ss）
 */
private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
