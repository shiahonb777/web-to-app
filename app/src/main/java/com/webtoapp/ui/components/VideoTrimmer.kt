package com.webtoapp.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 视频裁剪组件
 * 使用缩略图预览，避免 VideoView 黑框问题
 * 时长不限制
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmer(
    videoPath: String,
    startMs: Long,
    endMs: Long,
    videoDurationMs: Long,
    onTrimChange: (startMs: Long, endMs: Long, totalDurationMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Check视频文件是否存在
    val videoExists = remember(videoPath) {
        when {
            videoPath.startsWith("/") -> File(videoPath).exists()
            videoPath.startsWith("content://") -> {
                try {
                    context.contentResolver.openInputStream(Uri.parse(videoPath))?.close()
                    true
                } catch (e: Exception) {
                    false
                }
            }
            else -> false
        }
    }
    
    // 如果文件不存在，显示提示
    if (!videoExists) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.videoFileNotExist,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    // Get视频信息和缩略图
    var totalDuration by remember { mutableLongStateOf(videoDurationMs) }
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPreviewMs by remember { mutableLongStateOf(startMs) }
    
    // MediaMetadataRetriever 实例（用于实时预览）
    val retriever = remember { MediaMetadataRetriever() }
    var retrieverReady by remember { mutableStateOf(false) }
    
    // Initialize retriever 和加载视频信息
    LaunchedEffect(videoPath) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                when {
                    videoPath.startsWith("/") -> retriever.setDataSource(videoPath)
                    videoPath.startsWith("content://") -> retriever.setDataSource(context, Uri.parse(videoPath))
                    else -> retriever.setDataSource(videoPath)
                }
                retrieverReady = true
                
                // Get时长
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                
                // Get初始缩略图
                val frame = retriever.getFrameAtTime(startMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                
                totalDuration = duration
                thumbnail = frame
                currentPreviewMs = startMs
                
                // Initialize裁剪范围（使用整个视频）
                if (videoDurationMs == 0L && duration > 0) {
                    onTrimChange(0L, duration, duration)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }
    
    // 当起始位置变化时更新预览帧（带防抖处理，避免频繁调用导致卡顿）
    var previewJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(currentPreviewMs) {
        // Cancel之前的任务
        previewJob?.cancel()
        
        if (retrieverReady && currentPreviewMs >= 0) {
            // 防抖延迟 150ms
            previewJob = scope.launch {
                kotlinx.coroutines.delay(150)
                withContext(Dispatchers.IO) {
                    try {
                        val frame = retriever.getFrameAtTime(
                            currentPreviewMs * 1000, 
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        if (frame != null) {
                            thumbnail = frame
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    // Cleanup retriever
    DisposableEffect(Unit) {
        onDispose {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // 计算当前选择的时长
    val selectedDuration = endMs - startMs
    val selectedSeconds = selectedDuration / 1000f
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Video缩略图预览区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = Strings.videoPreview,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Video图标指示
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Duration信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Strings.selectedDuration.format(selectedSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = Strings.totalDuration.format(totalDuration / 1000f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 裁剪范围滑块
        if (totalDuration > 0) {
            Column {
                Text(
                    text = Strings.trimRangeHint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 使用 RangeSlider 选择起止时间
                RangeSlider(
                    value = startMs.toFloat()..endMs.toFloat(),
                    onValueChange = { range ->
                        val newStart = range.start.toLong()
                        var newEnd = range.endInclusive.toLong()
                        
                        // 确保至少有 1 秒
                        if (newEnd - newStart < 1000) {
                            newEnd = minOf(newStart + 1000, totalDuration)
                        }
                        
                        // Update预览帧位置（优先显示起始位置）
                        currentPreviewMs = newStart
                        
                        onTrimChange(newStart, newEnd, totalDuration)
                    },
                    valueRange = 0f..totalDuration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Time标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(startMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatTime(endMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间显示
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return if (minutes > 0) {
        "%d:%02d.%d".format(minutes, seconds, millis)
    } else {
        "%d.%d 秒".format(seconds, millis)
    }
}
