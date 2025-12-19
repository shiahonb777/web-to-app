package com.webtoapp.ui.components.gallery

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.data.model.GalleryConfig
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import kotlinx.coroutines.delay

/**
 * 画廊查看器 - 支持左右滑动切换的幻灯片组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryViewer(
    config: GalleryConfig,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val items = config.items
    if (items.isEmpty()) return
    
    val pagerState = rememberPagerState(pageCount = { items.size })
    var showControls by remember { mutableStateOf(true) }
    var isAutoPlaying by remember { mutableStateOf(config.autoPlay) }
    
    // 自动播放逻辑
    LaunchedEffect(isAutoPlaying, pagerState.currentPage) {
        if (isAutoPlaying && items.size > 1) {
            delay(config.autoPlayInterval * 1000L)
            val nextPage = if (config.loop) {
                (pagerState.currentPage + 1) % items.size
            } else {
                (pagerState.currentPage + 1).coerceAtMost(items.size - 1)
            }
            if (nextPage != pagerState.currentPage) {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls }
                )
            }
    ) {
        // 主内容 - HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = config.enableSwipe
        ) { page ->
            GalleryPage(
                item = items[page],
                isCurrentPage = page == pagerState.currentPage
            )
        }
        
        // 顶部标题栏
        AnimatedVisibility(
            visible = showControls && config.showTitle,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(top = 48.dp, bottom = 24.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 关闭按钮
                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "关闭",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(Modifier.width(40.dp))
                    }
                    
                    // 标题
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = items[pagerState.currentPage].title.ifBlank { 
                                "项目 ${pagerState.currentPage + 1}" 
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        items[pagerState.currentPage].description?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // 自动播放切换
                    if (items.size > 1) {
                        IconButton(
                            onClick = { isAutoPlaying = !isAutoPlaying },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isAutoPlaying) "暂停自动播放" else "开始自动播放",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(Modifier.width(40.dp))
                    }
                }
            }
        }
        
        // 底部指示器
        AnimatedVisibility(
            visible = showControls && config.showIndicator && items.size > 1,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(bottom = 48.dp, top = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 页码指示器
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(items.size) { index ->
                            val isSelected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (isSelected) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White
                                        else Color.White.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // 页码文字
                    Text(
                        text = "${pagerState.currentPage + 1} / ${items.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 单个画廊页面
 */
@Composable
private fun GalleryPage(
    item: GalleryItem,
    isCurrentPage: Boolean
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (item.type) {
            GalleryItemType.IMAGE -> {
                // 图片显示
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            GalleryItemType.VIDEO -> {
                // 视频播放
                var isPlaying by remember { mutableStateOf(false) }
                
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(item.path))
                            setOnPreparedListener { mp ->
                                mp.isLooping = item.mediaConfig?.loop ?: false
                                if (isCurrentPage && (item.mediaConfig?.autoPlay != false)) {
                                    start()
                                    isPlaying = true
                                }
                            }
                            setOnCompletionListener {
                                isPlaying = false
                            }
                        }
                    },
                    update = { videoView ->
                        if (isCurrentPage) {
                            if (!videoView.isPlaying && (item.mediaConfig?.autoPlay != false)) {
                                videoView.start()
                                isPlaying = true
                            }
                        } else {
                            if (videoView.isPlaying) {
                                videoView.pause()
                                isPlaying = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // 播放/暂停按钮覆盖层
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            "播放",
                            modifier = Modifier.size(72.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            else -> {
                // 其他类型暂不支持
                Text(
                    text = "不支持的媒体类型",
                    color = Color.White
                )
            }
        }
    }
}
