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
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.LrcGenerationService
import com.webtoapp.data.model.*
import com.webtoapp.util.BgmStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 背景音乐选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BgmSelectorDialog(
    currentConfig: BgmConfig,
    onDismiss: () -> Unit,
    onConfirm: (BgmConfig) -> Unit,
    onNavigateToTaskManager: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 可用音乐列表
    var availableBgm by remember { mutableStateOf<List<BgmItem>>(emptyList()) }
    
    // 已选播放列表
    var selectedPlaylist by remember { mutableStateOf(currentConfig.playlist) }
    
    // 播放模式
    var playMode by remember { mutableStateOf(currentConfig.playMode) }
    
    // 音量
    var volume by remember { mutableFloatStateOf(currentConfig.volume) }
    
    // 自动播放
    var autoPlay by remember { mutableStateOf(currentConfig.autoPlay) }
    
    // 显示歌词
    var showLyrics by remember { mutableStateOf(currentConfig.showLyrics) }
    
    // 字幕主题
    var selectedTheme by remember { mutableStateOf(currentConfig.lrcTheme ?: PresetLrcThemes.themes.first()) }
    
    // 当前播放预览的音乐
    var previewingBgm by remember { mutableStateOf<BgmItem?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    // 上传对话框
    var showUploadDialog by remember { mutableStateOf(false) }
    
    // 标签筛选
    var selectedTagFilter by remember { mutableStateOf<BgmTag?>(null) }
    
    // 当前编辑标签的音乐
    var editingTagsBgm by remember { mutableStateOf<BgmItem?>(null) }
    
    // 字幕主题选择对话框
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // AI生成歌词对话框
    var showGenerateLrcDialog by remember { mutableStateOf(false) }
    var generateLrcBgm by remember { mutableStateOf<BgmItem?>(null) }
    
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
    
    // 刷新音乐列表的函数
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
    
    // 加载音乐列表
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            availableBgm = BgmStorage.scanAllBgm(context)
        }
    }
    
    // 注册广播接收器监听 LRC 生成完成
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == LrcGenerationService.ACTION_LRC_TASK_COMPLETED) {
                    val bgmPath = intent.getStringExtra(LrcGenerationService.EXTRA_BGM_PATH)
                    val bgmName = intent.getStringExtra(LrcGenerationService.EXTRA_BGM_NAME)
                        ?: availableBgm.find { it.path == bgmPath }?.name 
                        ?: bgmPath?.substringAfterLast("/")?.substringBeforeLast(".") 
                        ?: "歌曲"
                    val success = intent.getBooleanExtra(LrcGenerationService.EXTRA_SUCCESS, false)
                    android.util.Log.d("BgmSelector", "收到 LRC 完成广播: path=$bgmPath, name=$bgmName, success=$success")
                    
                    // 显示 Snackbar 提示
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = if (success) "✓ 「$bgmName」歌词生成完成" else "✗ 「$bgmName」歌词生成失败",
                            duration = SnackbarDuration.Short
                        )
                    }
                    
                    if (success) {
                        // 刷新数据
                        refreshBgmList()
                    }
                }
            }
        }
        
        val filter = IntentFilter(LrcGenerationService.ACTION_LRC_TASK_COMPLETED)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }
    
    // 清理 MediaPlayer
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
                    title = { Text("选择背景音乐") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "关闭")
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
                            Text("确定")
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
                                "已选音乐 (${selectedPlaylist.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "长按拖动排序",
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
                                DraggableSelectedBgmItem(
                                    bgm = bgm,
                                    index = index,
                                    isPlaying = previewingBgm == bgm,
                                    isDragging = draggedItemIndex == index,
                                    isDraggedOver = draggedOverItemIndex == index,
                                    onPlay = { previewBgm(bgm) },
                                    onRemove = {
                                        selectedPlaylist = selectedPlaylist - bgm
                                    },
                                    onEditTags = { editingTagsBgm = bgm },
                                    onGenerateLrc = {
                                        generateLrcBgm = bgm
                                        showGenerateLrcDialog = true
                                    },
                                    onPreviewLrc = if (bgm.lrcData != null) {
                                        {
                                            previewLrcBgm = bgm
                                            showLrcPreviewDialog = true
                                        }
                                    } else null,
                                    onDragStart = { draggedItemIndex = index },
                                    onDragEnd = {
                                        if (draggedItemIndex >= 0 && draggedOverItemIndex >= 0 && 
                                            draggedItemIndex != draggedOverItemIndex) {
                                            val mutableList = selectedPlaylist.toMutableList()
                                            val item = mutableList.removeAt(draggedItemIndex)
                                            mutableList.add(draggedOverItemIndex, item)
                                            selectedPlaylist = mutableList
                                        }
                                        draggedItemIndex = -1
                                        draggedOverItemIndex = -1
                                    },
                                    onDragOver = { draggedOverItemIndex = index }
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
                            "可用音乐",
                            style = MaterialTheme.typography.labelMedium
                        )
                        TextButton(onClick = { showUploadDialog = true }) {
                            Icon(
                                Icons.Outlined.Add,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("上传音乐")
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
                                label = { Text("全部") }
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
                                    "暂无音乐",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "点击上方按钮上传音乐",
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
                                    "没有此标签的音乐",
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
                                            generateLrcBgm = bgm
                                            showGenerateLrcDialog = true
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
                
                // 底部设置区
                Divider()
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 播放模式
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("播放模式", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = playMode == BgmPlayMode.LOOP,
                                onClick = { playMode = BgmPlayMode.LOOP },
                                label = { Text("循环", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = playMode == BgmPlayMode.SEQUENTIAL,
                                onClick = { playMode = BgmPlayMode.SEQUENTIAL },
                                label = { Text("顺序", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = playMode == BgmPlayMode.SHUFFLE,
                                onClick = { playMode = BgmPlayMode.SHUFFLE },
                                label = { Text("随机", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    
                    // 音量
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("音量", style = MaterialTheme.typography.bodyMedium)
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
                    
                    // 自动播放
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自动播放", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autoPlay, onCheckedChange = { autoPlay = it })
                    }
                    
                    // 显示歌词
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("显示歌词", style = MaterialTheme.typography.bodyMedium)
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
                                Text("字幕主题", style = MaterialTheme.typography.bodyMedium)
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
    
    // 上传音乐对话框
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
    
    // AI生成歌词对话框
    if (showGenerateLrcDialog && generateLrcBgm != null) {
        GenerateLrcDialog(
            bgm = generateLrcBgm!!,
            onDismiss = { 
                showGenerateLrcDialog = false
                generateLrcBgm = null
            },
            onTaskStarted = { updatedBgm ->
                // 更新列表中的音乐项状态
                availableBgm = availableBgm.map { 
                    if (it.path == updatedBgm.path) updatedBgm else it 
                }
                selectedPlaylist = selectedPlaylist.map {
                    if (it.path == updatedBgm.path) updatedBgm else it
                }
                showGenerateLrcDialog = false
                generateLrcBgm = null
            },
            onNavigateToTaskManager = onNavigateToTaskManager,
            onManualAlign = {
                // 打开手动对齐对话框
                manualAlignerBgm = generateLrcBgm
                showGenerateLrcDialog = false
                generateLrcBgm = null
                showManualAlignerDialog = true
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
                // 更新音乐项的 LRC 数据
                val updatedBgm = manualAlignerBgm!!.copy(lrcData = newLrcData)
                availableBgm = availableBgm.map { 
                    if (it.path == updatedBgm.path) updatedBgm else it 
                }
                selectedPlaylist = selectedPlaylist.map {
                    if (it.path == updatedBgm.path) updatedBgm else it
                }
                showManualAlignerDialog = false
                manualAlignerBgm = null
                // 显示成功提示
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "✓ 歌词已保存",
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
                // 更新音乐项的 LRC 数据
                val updatedBgm = lrcEditorBgm!!.copy(lrcData = newLrcData)
                availableBgm = availableBgm.map { 
                    if (it.path == updatedBgm.path) updatedBgm else it 
                }
                selectedPlaylist = selectedPlaylist.map {
                    if (it.path == updatedBgm.path) updatedBgm else it
                }
                showLrcEditorDialog = false
                lrcEditorBgm = null
                // 显示成功提示
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "✓ 歌词已更新",
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
                    contentDescription = if (isPlaying) "停止" else "播放",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 可拖动的已选音乐项
 */
@Composable
private fun DraggableSelectedBgmItem(
    bgm: BgmItem,
    index: Int,
    isPlaying: Boolean,
    isDragging: Boolean,
    isDraggedOver: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onEditTags: () -> Unit,
    onGenerateLrc: () -> Unit,
    onPreviewLrc: (() -> Unit)? = null,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragOver: () -> Unit
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (isDragging) Modifier
                    .shadow(8.dp, RoundedCornerShape(8.dp))
                    .zIndex(1f)
                else if (isDraggedOver) Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                else Modifier
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDrag = { _, _ -> },
                    onDragCancel = { onDragEnd() }
                )
            },
        shape = MaterialTheme.shapes.small,
        color = if (isDragging) MaterialTheme.colorScheme.primaryContainer 
               else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
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
            // 歌词状态指示/预览按钮
            if (bgm.lrcData != null && onPreviewLrc != null) {
                IconButton(onClick = onPreviewLrc, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Subtitles,
                        contentDescription = "预览歌词",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (bgm.lrcData != null) {
                Icon(
                    Icons.Outlined.Subtitles,
                    contentDescription = "已有歌词",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onGenerateLrc, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = "AI生成歌词",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onEditTags, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Label,
                    contentDescription = "编辑标签",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "停止" else "播放",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除",
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
                    if (bgm.isAsset) "预置音乐" else "用户上传",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 歌词状态指示/预览按钮
            if (bgm.lrcData != null && onPreviewLrc != null) {
                IconButton(onClick = onPreviewLrc, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Subtitles,
                        contentDescription = "预览歌词",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (bgm.lrcData != null) {
                Icon(
                    Icons.Outlined.Subtitles,
                    contentDescription = "已有歌词",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onGenerateLrc, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = "AI生成歌词",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "停止" else "预览",
                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "删除",
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
        title = { Text("上传音乐") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = bgmName,
                    onValueChange = { bgmName = it },
                    label = { Text("音乐名称") },
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
                        Text(if (bgmUri != null) "已选择" else "选择音乐")
                    }
                    if (bgmUri != null) {
                        IconButton(onClick = { bgmUri = null }) {
                            Icon(Icons.Default.Close, "清除")
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
                        Text(if (coverUri != null) "已选择" else "选择封面(可选)")
                    }
                    if (coverUri != null) {
                        IconButton(onClick = { coverUri = null }) {
                            Icon(Icons.Default.Close, "清除")
                        }
                    }
                }
                
                Text(
                    "提示: 封面图片用于在选择界面展示",
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
                    Text("上传")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
        title = { Text("编辑标签") },
        text = {
            Column {
                Text(
                    bgm.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "选择适合的标签(可多选)",
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
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
                    "选择字幕主题",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "选择歌词显示的视觉风格",
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
                        Text("取消")
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
                        "示例歌词文本",
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
 * AI生成歌词对话框
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GenerateLrcDialog(
    bgm: BgmItem,
    onDismiss: () -> Unit,
    onTaskStarted: (BgmItem) -> Unit,
    onNavigateToTaskManager: (() -> Unit)? = null,
    onManualAlign: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configManager = remember { AiConfigManager(context) }
    
    // 获取已配置的模型（筛选具有音频能力的模型）
    val savedModels by configManager.savedModelsFlow.collectAsState(initial = emptyList())
    val apiKeys by configManager.apiKeysFlow.collectAsState(initial = emptyList())
    
    // 筛选出具有音频能力的模型
    val audioModels = savedModels.filter { model ->
        model.capabilities.contains(ModelCapability.AUDIO)
    }
    
    var selectedModel by remember { mutableStateOf<SavedModel?>(null) }
    var isStarting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 自动选择第一个音频模型
    LaunchedEffect(audioModels) {
        if (selectedModel == null && audioModels.isNotEmpty()) {
            selectedModel = audioModels.first()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("AI生成歌词")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 音乐信息
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                bgm.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (bgm.lrcData != null) {
                                Text(
                                    "已有歌词，重新生成将覆盖",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                // 模型选择
                if (audioModels.isEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "未找到支持音频的模型",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "请先在「AI设置」中添加模型，并为其标记「音频理解」能力",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        "选择模型",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    // 模型列表
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        audioModels.forEach { model ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedModel = model },
                                shape = MaterialTheme.shapes.small,
                                color = if (selectedModel == model) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedModel == model,
                                        onClick = { selectedModel = model }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.alias ?: model.model.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            model.model.provider.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 错误信息
                errorMessage?.let { error ->
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // 提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "任务将在后台运行，完成后会发送通知",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (onNavigateToTaskManager != null) {
                        TextButton(
                            onClick = {
                                onDismiss()
                                onNavigateToTaskManager()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("查看任务", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                // 手动对齐入口
                if (onManualAlign != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onManualAlign()
                            },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.TouchApp,
                                null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "手动对齐歌词",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "输入歌词文本，播放时点击打点进行时间对齐",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val model = selectedModel ?: return@Button
                    val apiKey = apiKeys.find { it.id == model.apiKeyId }
                    
                    if (apiKey == null) {
                        errorMessage = "未找到关联的API密钥"
                        return@Button
                    }
                    
                    isStarting = true
                    
                    // 创建任务
                    val task = LrcTask(
                        bgmItemId = bgm.id,
                        bgmName = bgm.name,
                        bgmPath = bgm.path,
                        modelId = model.id
                    )
                    
                    // 启动后台服务
                    LrcGenerationService.startTask(context, task, apiKey, model)
                    
                    // 通知任务已开始
                    onTaskStarted(bgm)
                },
                enabled = selectedModel != null && !isStarting
            ) {
                if (isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("开始生成")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
    
    // 播放器状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    
    // 列表滚动状态
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
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
            android.util.Log.e("LrcPreview", "初始化播放器失败", e)
        }
    }
    
    // 更新播放进度
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                currentPosition = mp.currentPosition.toLong()
                
                // 查找当前歌词行
                val newIndex = lrcData.lines.indexOfLast { it.startTime <= currentPosition }
                if (newIndex != currentLineIndex && newIndex >= 0) {
                    currentLineIndex = newIndex
                    // 自动滚动到当前行
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
    
    // 清理播放器
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
                            "歌词预览 · ${lrcData.lines.size} 行",
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
                            Icon(Icons.Outlined.Edit, "编辑")
                        }
                    }
                    
                    IconButton(onClick = {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, "关闭")
                    }
                }
                
                Divider()
                
                // 歌词列表
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
                
                // 播放控制
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
                    
                    // 播放按钮
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
                            Icon(Icons.Default.Replay10, "后退10秒")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 播放/暂停
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
                        
                        // 前进 10 秒
                        IconButton(onClick = {
                            mediaPlayer?.let { mp ->
                                val newPos = minOf(mp.duration, mp.currentPosition + 10000)
                                mp.seekTo(newPos)
                                currentPosition = newPos.toLong()
                            }
                        }) {
                            Icon(Icons.Default.Forward10, "前进10秒")
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
