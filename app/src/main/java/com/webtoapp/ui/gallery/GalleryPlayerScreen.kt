package com.webtoapp.ui.gallery

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * 画廊播放器主界面
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryPlayerScreen(
    config: GalleryConfig,
    startIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get排序后的媒体列表
    val items = remember(config) {
        when (config.playMode) {
            GalleryPlayMode.SHUFFLE -> config.getSortedItems().shuffled()
            else -> config.getSortedItems()
        }
    }
    
    // Pager 状态
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { items.size }
    )
    
    // 当前项索引 - 使用 settledPage 确保页面完全稳定后才更新
    val currentIndex by remember { derivedStateOf { pagerState.settledPage } }
    val currentItem = items.getOrNull(currentIndex)
    
    // 控制 UI 显示状态
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(config.autoPlay) }
    
    // Image自动播放计时器
    LaunchedEffect(currentIndex, isPlaying) {
        // 只在页面完全停止后才开始计时
        if (isPlaying && currentItem?.type == GalleryItemType.IMAGE && !pagerState.isScrollInProgress) {
            delay(config.imageInterval * 1000L)
            // Play下一个
            if (currentIndex < items.size - 1) {
                pagerState.animateScrollToPage(currentIndex + 1)
            } else if (config.loop) {
                pagerState.animateScrollToPage(0)
            } else {
                isPlaying = false
            }
        }
    }
    
    // Auto隐藏控制 UI
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }
    
    // 背景颜色
    val bgColor = remember(config.backgroundColor) {
        try {
            Color(android.graphics.Color.parseColor(config.backgroundColor))
        } catch (e: Exception) {
            Color.Black
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        // 主内容 - HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items.getOrNull(page)
            if (item != null) {
                when (item.type) {
                    GalleryItemType.IMAGE -> {
                        GalleryImageViewer(
                            item = item,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    GalleryItemType.VIDEO -> {
                        GalleryVideoPlayer(
                            item = item,
                            isCurrentPage = page == currentIndex,
                            isPlaying = isPlaying && page == currentIndex,
                            enableAudio = config.enableAudio,
                            showControls = showControls,
                            onPlayStateChange = { playing -> isPlaying = playing },
                            onVideoEnded = {
                                if (config.videoAutoNext) {
                                    scope.launch {
                                        if (currentIndex < items.size - 1) {
                                            pagerState.animateScrollToPage(currentIndex + 1)
                                        } else if (config.loop) {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                }
                            },
                            onToggleControls = { showControls = !showControls },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        
        // 顶部信息栏
        AnimatedVisibility(
            visible = showControls && config.showMediaInfo,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopInfoBar(
                currentItem = currentItem,
                currentIndex = currentIndex,
                totalCount = items.size,
                onBack = onBack
            )
        }
        
        // 底部缩略图栏
        if (config.showThumbnailBar) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ThumbnailBar(
                    items = items,
                    currentIndex = currentIndex,
                    onItemClick = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
        
        // Play/暂停按钮（仅图片模式）
        if (currentItem?.type == GalleryItemType.IMAGE) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) Strings.galleryPlayerPause else Strings.galleryPlayerPlay,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        // 左右导航箭头
        AnimatedVisibility(
            visible = showControls && currentIndex > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(currentIndex - 1)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = Strings.galleryPlayerPrevious,
                    tint = Color.White
                )
            }
        }
        
        AnimatedVisibility(
            visible = showControls && currentIndex < items.size - 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(currentIndex + 1)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = Strings.galleryPlayerNext,
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 顶部信息栏
 */
@Composable
private fun TopInfoBar(
    currentItem: GalleryItem?,
    currentIndex: Int,
    totalCount: Int,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = Strings.back,
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                currentItem?.let { item ->
                    Text(
                        text = item.name.ifBlank { "Media ${currentIndex + 1}" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Media类型图标
            currentItem?.let { item ->
                Icon(
                    if (item.type == GalleryItemType.VIDEO) Icons.Outlined.Videocam 
                    else Icons.Outlined.Image,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 底部缩略图栏
 */
@Composable
private fun ThumbnailBar(
    items: List<GalleryItem>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    // Auto滚动到当前项
    LaunchedEffect(currentIndex) {
        if (currentIndex in items.indices) {
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -100 // 偏移以居中显示
            )
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == currentIndex
                
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 64.dp else 56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onItemClick(index) }
                ) {
                    val imagePath = item.thumbnailPath ?: item.path
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(imagePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Video标记
                    if (item.type == GalleryItemType.VIDEO) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.Center),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图片查看器
 */
@Composable
fun GalleryImageViewer(
    item: GalleryItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(item.path))
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 视频播放器
 */
@Composable
fun GalleryVideoPlayer(
    item: GalleryItem,
    isCurrentPage: Boolean,
    isPlaying: Boolean,
    enableAudio: Boolean,
    showControls: Boolean,
    onPlayStateChange: (Boolean) -> Unit,
    onVideoEnded: () -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isFastForwarding by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    
    // Create和释放 MediaPlayer
    DisposableEffect(item.path) {
        val mediaPlayer = android.media.MediaPlayer().apply {
            try {
                setDataSource(item.path)
                setOnPreparedListener { mp ->
                    isPrepared = true
                    duration = mp.duration.toLong()
                    surfaceHolder?.let { mp.setDisplay(it) }
                    if (isPlaying) mp.start()
                }
                setOnCompletionListener {
                    onVideoEnded()
                }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            } catch (e: Exception) {
                // Handle error
            }
        }
        player = mediaPlayer
        
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {
                // ignore
            }
            player = null
            isPrepared = false
        }
    }
    
    // Handle播放状态变化
    LaunchedEffect(isPlaying, isPrepared, isCurrentPage) {
        player?.let { mp ->
            if (isPrepared) {
                if (isPlaying && isCurrentPage) {
                    if (!mp.isPlaying) mp.start()
                } else {
                    if (mp.isPlaying) mp.pause()
                }
            }
        }
    }
    
    // Handle音量
    LaunchedEffect(enableAudio, isPrepared) {
        player?.let { mp ->
            if (isPrepared) {
                mp.setVolume(
                    if (enableAudio) 1f else 0f,
                    if (enableAudio) 1f else 0f
                )
            }
        }
    }
    
    // Update播放进度
    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            player?.let { mp ->
                try {
                    currentPosition = mp.currentPosition.toLong()
                } catch (e: Exception) {
                    // ignore
                }
            }
            delay(100)
        }
    }
    
    // 长按快进
    LaunchedEffect(isFastForwarding) {
        if (isFastForwarding) {
            player?.let { mp ->
                try {
                    // 使用 2x 速度播放
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        mp.playbackParams = mp.playbackParams.setSpeed(2f)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        } else {
            player?.let { mp ->
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val width = size.width
                        val seekAmount = 10000 // 10 seconds
                        player?.let { mp ->
                            if (isPrepared) {
                                val newPosition = if (offset.x < width / 2) {
                                    // 双击左侧，后退
                                    (mp.currentPosition - seekAmount).coerceAtLeast(0)
                                } else {
                                    // 双击右侧，快进
                                    (mp.currentPosition + seekAmount).coerceAtMost(mp.duration)
                                }
                                mp.seekTo(newPosition)
                            }
                        }
                    },
                    onLongPress = {
                        isFastForwarding = true
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) {
                            isFastForwarding = false
                        }
                    }
                }
            }
    ) {
        // SurfaceView for video
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            surfaceHolder = holder
                            player?.setDisplay(holder)
                        }
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            surfaceHolder = null
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Video控制层
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            VideoControlBar(
                currentPosition = currentPosition,
                duration = duration,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                onPlayPause = { onPlayStateChange(!isPlaying) },
                onSeek = { position ->
                    player?.seekTo(position.toInt())
                    currentPosition = position
                },
                onSpeedChange = { speed ->
                    playbackSpeed = speed
                    player?.let { mp ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            try {
                                mp.playbackParams = mp.playbackParams.setSpeed(speed)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                }
            )
        }
        
        // 快进指示
        AnimatedVisibility(
            visible = isFastForwarding,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("2x", color = Color.White)
                }
            }
        }
    }
}

/**
 * 视频控制栏
 */
@Composable
private fun VideoControlBar(
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // 进度条
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { onSeek((it * duration).toLong()) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            
            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 倍速按钮
                Box {
                    TextButton(onClick = { showSpeedMenu = true }) {
                        Text(
                            text = "${playbackSpeed}x",
                            color = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x") },
                                onClick = {
                                    onSpeedChange(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }
                
                // 后退 10 秒
                IconButton(onClick = { onSeek((currentPosition - 10000).coerceAtLeast(0)) }) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = Strings.galleryPlayerSeekBack,
                        tint = Color.White
                    )
                }
                
                // Play/暂停
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) Strings.galleryPlayerPause else Strings.galleryPlayerPlay,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
                
                // 快进 10 秒
                IconButton(onClick = { onSeek((currentPosition + 10000).coerceAtMost(duration)) }) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = Strings.galleryPlayerSeekForward,
                        tint = Color.White
                    )
                }
                
                // 占位
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

/**
 * 格式化时间
 */
private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    val hours = ms / 1000 / 60 / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
