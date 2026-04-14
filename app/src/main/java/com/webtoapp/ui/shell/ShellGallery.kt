package com.webtoapp.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gallery( Shell mode)
 * from APK assets/gallery/ directoryload file
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ShellGalleryPlayer(
    galleryConfig: com.webtoapp.core.shell.GalleryShellConfig,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val assetDecryptor = remember { com.webtoapp.core.crypto.AssetDecryptor(context) }
    
    // list
    val items = remember(galleryConfig) {
        when (galleryConfig.playMode) {
            "SHUFFLE" -> galleryConfig.items.shuffled()
            else -> galleryConfig.items
        }
    }
    var effectiveItems by remember { mutableStateOf(items) }
    
    // If config items are empty, try to derive from assets (for compatibility)
    LaunchedEffect(items) {
        effectiveItems = if (items.isNotEmpty()) {
            items
        } else {
            val derived = deriveGalleryItemsFromAssets(context)
            if (derived.isNotEmpty()) derived else items
        }
    }
    
    if (effectiveItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("没有媒体文件", color = Color.White)
        }
        return
    }
    
    // Pager state
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { effectiveItems.size }
    )
    
    // current
    val currentIndex by remember { derivedStateOf { pagerState.settledPage } }
    val currentItem = effectiveItems.getOrNull(currentIndex)
    
    // UI displaystate
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(galleryConfig.autoPlay) }
    
    // Note
    LaunchedEffect(currentIndex, isPlaying) {
        if (isPlaying && currentItem?.type == "IMAGE" && !pagerState.isScrollInProgress) {
            kotlinx.coroutines.delay(galleryConfig.imageInterval * 1000L)
            if (currentIndex < items.size - 1) {
                pagerState.animateScrollToPage(currentIndex + 1)
            } else if (galleryConfig.loop) {
                pagerState.animateScrollToPage(0)
            } else {
                isPlaying = false
            }
        }
    }
    
    // hide UI
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
    
    // color
    val bgColor = remember(galleryConfig.backgroundColor) {
        try {
            Color(android.graphics.Color.parseColor(galleryConfig.backgroundColor))
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
        // content- HorizontalPager
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = effectiveItems.getOrNull(page)
            if (item != null) {
                when (item.type) {
                    "IMAGE" -> {
                        ShellGalleryImageViewer(
                            item = item,
                            assetDecryptor = assetDecryptor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "VIDEO" -> {
                        ShellGalleryVideoPlayer(
                            item = item,
                            assetDecryptor = assetDecryptor,
                            isCurrentPage = page == currentIndex,
                            isPlaying = isPlaying && page == currentIndex,
                            enableAudio = galleryConfig.enableAudio,
                            showControls = showControls,
                            onPlayStateChange = { playing -> isPlaying = playing },
                            onVideoEnded = {
                                if (galleryConfig.videoAutoNext) {
                                    scope.launch {
                                        if (currentIndex < effectiveItems.size - 1) {
                                            pagerState.animateScrollToPage(currentIndex + 1)
                                        } else if (galleryConfig.loop) {
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
        
        // top
        AnimatedVisibility(
            visible = showControls && galleryConfig.showMediaInfo,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
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
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Strings.cdBack,
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        currentItem?.let { item ->
                            Text(
                                text = item.name.ifBlank { "Media ${currentIndex + 1}" },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${currentIndex + 1} / ${effectiveItems.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    // typeicon
                    currentItem?.let { item ->
                        Icon(
                            if (item.type == "VIDEO") Icons.Outlined.Videocam 
                            else Icons.Outlined.Image,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // / button( only mode)
        if (currentItem?.type == "IMAGE") {
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
                            androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) Strings.cdPause else Strings.cdPlay,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        // Note
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
                    scope.launch { pagerState.animateScrollToPage(currentIndex - 1) }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = Strings.cdPrevious,
                    tint = Color.White
                )
            }
        }
        
        AnimatedVisibility(
            visible = showControls && currentIndex < effectiveItems.size - 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(currentIndex + 1) }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = Strings.cdNext,
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Derive gallery items from embedded assets when config items are empty.
 * Looks for assets/gallery/item_*.{png|jpg|mp4|...} (and optional .enc).
 */
private fun deriveGalleryItemsFromAssets(
    context: android.content.Context
): List<com.webtoapp.core.shell.GalleryShellItem> {
    return try {
        val assetEntries = context.assets.list("gallery")?.toList().orEmpty()
        if (assetEntries.isEmpty()) return emptyList()
        
        val entrySet = assetEntries.toSet()
        val normalized = assetEntries.map { name ->
            if (name.endsWith(".enc")) name.removeSuffix(".enc") else name
        }.toSet()
        
        val videoExts = setOf("mp4", "webm", "mkv", "avi", "mov", "3gp", "m4v")
        val imageExts = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif")
        
        normalized
            .filter { it.startsWith("item_") }
            .mapNotNull { name ->
                val ext = name.substringAfterLast('.', "").lowercase()
                val type = when {
                    ext in videoExts -> "VIDEO"
                    ext in imageExts -> "IMAGE"
                    else -> null
                } ?: return@mapNotNull null
                
                val index = name.substringAfter("item_").substringBefore(".").toIntOrNull()
                val thumbName = index?.let { "thumb_$it.jpg" }
                val thumbExists = thumbName?.let { tn -> tn in normalized || "${tn}.enc" in entrySet } == true
                
                val displayName = index?.let { "Media ${it + 1}" } ?: name
                val sortKey = index ?: Int.MAX_VALUE
                
                com.webtoapp.core.shell.GalleryShellItem(
                    id = name,
                    assetPath = "gallery/$name",
                    type = type,
                    name = displayName,
                    duration = 0,
                    thumbnailPath = if (thumbExists) "gallery/$thumbName" else null
                ) to sortKey
            }
            .sortedBy { it.second }
            .map { it.first }
    } catch (e: Exception) {
        AppLogger.w("ShellGallery", "Failed to derive gallery assets", e)
        emptyList()
    }
}

/**
 * Gallery( from assets load)
 */
@Composable
fun ShellGalleryImageViewer(
    item: com.webtoapp.core.shell.GalleryShellItem,
    assetDecryptor: com.webtoapp.core.crypto.AssetDecryptor,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // load
    LaunchedEffect(item.assetPath) {
        isLoading = true
        try {
            // load version
            val imageBytes = try {
                assetDecryptor.loadAsset(item.assetPath)
            } catch (e: Exception) {
                // fallback version
                context.assets.open(item.assetPath).use { it.readBytes() }
            }
            bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            AppLogger.e("ShellGallery", "Failed to load image: ${item.assetPath}", e)
        }
        isLoading = false
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Gallery( from assets load)
 */
@Composable
fun ShellGalleryVideoPlayer(
    item: com.webtoapp.core.shell.GalleryShellItem,
    assetDecryptor: com.webtoapp.core.crypto.AssetDecryptor,
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
    val scope = rememberCoroutineScope()
    
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var surfaceHolder by remember { mutableStateOf<android.view.SurfaceHolder?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var tempVideoFile by remember { mutableStateOf<java.io.File?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val isEncrypted = remember(item.assetPath) { assetDecryptor.isEncrypted(item.assetPath) }
    
    // create MediaPlayer
    DisposableEffect(item.assetPath, isEncrypted) {
        val mediaPlayer = android.media.MediaPlayer()
        var assetFd: android.content.res.AssetFileDescriptor? = null
        
        val job = scope.launch(Dispatchers.IO) {
            try {
                // Set listeners on main thread
                withContext(Dispatchers.Main) {
                    mediaPlayer.setOnPreparedListener { mp ->
                        isPrepared = true
                        duration = mp.duration.toLong()
                        surfaceHolder?.let { mp.setDisplay(it) }
                        if (isPlaying) mp.start()
                    }
                    mediaPlayer.setOnCompletionListener {
                        onVideoEnded()
                    }
                    mediaPlayer.setOnErrorListener { _, _, _ -> true }
                }
                
                if (!isEncrypted) {
                    // prefer AssetFileDescriptor( file)
                    try {
                        assetFd = context.assets.openFd(item.assetPath)
                        withContext(Dispatchers.Main) {
                            mediaPlayer.setDataSource(
                                assetFd!!.fileDescriptor,
                                assetFd!!.startOffset,
                                assetFd!!.length
                            )
                            mediaPlayer.prepareAsync()
                        }
                        return@launch
                    } catch (e: Exception) {
                        AppLogger.w("ShellGallery", "openFd failed, fallback to stream copy: ${item.assetPath}", e)
                    }
                    
                    // fallback: file
                    val ext = item.assetPath.substringAfterLast('.', "mp4")
                    val tempFile = java.io.File(context.cacheDir, "gallery_video_${System.currentTimeMillis()}.$ext")
                    context.assets.open(item.assetPath).use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    tempVideoFile = tempFile
                    withContext(Dispatchers.Main) {
                        mediaPlayer.setDataSource(tempFile.absolutePath)
                        mediaPlayer.prepareAsync()
                    }
                } else {
                    // file
                    val videoBytes = assetDecryptor.loadAsset(item.assetPath)
                    val ext = item.assetPath.substringAfterLast('.', "mp4")
                    val tempFile = java.io.File(context.cacheDir, "gallery_video_${System.currentTimeMillis()}.$ext")
                    tempFile.writeBytes(videoBytes)
                    tempVideoFile = tempFile
                    withContext(Dispatchers.Main) {
                        mediaPlayer.setDataSource(tempFile.absolutePath)
                        mediaPlayer.prepareAsync()
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("ShellGallery", "Failed to load video: ${item.assetPath}", e)
            }
        }
        
        player = mediaPlayer
        
        onDispose {
            job.cancel()
            try {
                assetFd?.close()
            } catch (e: Exception) {
                // ignore
            }
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {
                // ignore
            }
            player = null
            isPrepared = false
            // file
            tempVideoFile?.delete()
            tempVideoFile = null
        }
    }
    
    // handle state
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
    
    // handle
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
    
    // update
    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            player?.let { mp ->
                try {
                    currentPosition = mp.currentPosition.toLong()
                } catch (e: Exception) {
                    // ignore
                }
            }
            kotlinx.coroutines.delay(100)
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
                                    (mp.currentPosition - seekAmount).coerceAtLeast(0)
                                } else {
                                    (mp.currentPosition + seekAmount).coerceAtMost(mp.duration)
                                }
                                mp.seekTo(newPosition)
                            }
                        }
                    }
                )
            }
    ) {
        // SurfaceView for video
        AndroidView(
            factory = { ctx ->
                android.view.SurfaceView(ctx).apply {
                    holder.addCallback(object : android.view.SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            surfaceHolder = holder
                            player?.setDisplay(holder)
                        }
                        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                            surfaceHolder = null
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Note
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
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
                    // Note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimeMs(currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            onValueChange = { 
                                player?.seekTo((it * duration).toInt())
                                currentPosition = (it * duration).toLong()
                            },
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        
                        Text(
                            text = formatTimeMs(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    
                    // button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 10
                        IconButton(onClick = { 
                            player?.let { mp ->
                                if (isPrepared) mp.seekTo((mp.currentPosition - 10000).coerceAtLeast(0))
                            }
                        }) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = Strings.cdSeekBack,
                                tint = Color.White
                            )
                        }
                        
                        // Note
                        IconButton(
                            onClick = { onPlayStateChange(!isPlaying) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) Strings.cdPause else Strings.cdPlay,
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                        
                        // 10
                        IconButton(onClick = { 
                            player?.let { mp ->
                                if (isPrepared) mp.seekTo((mp.currentPosition + 10000).coerceAtMost(mp.duration))
                            }
                        }) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = Strings.cdSeekForward,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
