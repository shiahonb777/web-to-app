package com.webtoapp.ui.components

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.io.File

/**
 * 视频裁剪组件
 * 允许用户选择视频的一个片段（最长5秒）
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
    val maxTrimDuration = 5000L // 最大裁剪时长 5 秒
    
    // 获取视频总时长
    var totalDuration by remember { mutableLongStateOf(videoDurationMs) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(startMs) }
    
    // 初始化获取视频时长
    LaunchedEffect(videoPath) {
        if (totalDuration == 0L) {
            try {
                val retriever = MediaMetadataRetriever()
                when {
                    videoPath.startsWith("/") -> retriever.setDataSource(videoPath)
                    videoPath.startsWith("content://") -> retriever.setDataSource(context, Uri.parse(videoPath))
                    else -> retriever.setDataSource(videoPath)
                }
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                totalDuration = durationStr?.toLongOrNull() ?: 0L
                retriever.release()
                
                // 初始化裁剪范围
                val initialEnd = minOf(maxTrimDuration, totalDuration)
                onTrimChange(0L, initialEnd, totalDuration)
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
        // 视频预览区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // 简化的视频预览（使用 VideoView）
            AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        when {
                            videoPath.startsWith("/") -> setVideoPath(videoPath)
                            else -> setVideoURI(Uri.parse(videoPath))
                        }
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            seekTo(startMs.toInt())
                        }
                    }
                },
                update = { videoView ->
                    if (isPlaying) {
                        videoView.start()
                    } else {
                        videoView.pause()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 播放/暂停按钮
            FloatingActionButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.size(48.dp),
                containerColor = Color.White.copy(alpha = 0.8f)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.Black
                )
            }
        }
        
        // 时长信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "已选择: %.1f 秒".format(selectedSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedDuration > maxTrimDuration) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "总时长: %.1f 秒".format(totalDuration / 1000f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 裁剪范围滑块
        if (totalDuration > 0) {
            Column {
                Text(
                    text = "裁剪范围（拖动选择 1-5 秒片段）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 使用 RangeSlider 选择起止时间
                RangeSlider(
                    value = startMs.toFloat()..endMs.toFloat(),
                    onValueChange = { range ->
                        var newStart = range.start.toLong()
                        var newEnd = range.endInclusive.toLong()
                        
                        // 限制最大时长为 5 秒
                        if (newEnd - newStart > maxTrimDuration) {
                            // 根据哪个端点在移动来调整
                            if (newStart != startMs) {
                                // 起始点在移动，调整结束点
                                newEnd = minOf(newStart + maxTrimDuration, totalDuration)
                            } else {
                                // 结束点在移动，调整起始点
                                newStart = maxOf(0, newEnd - maxTrimDuration)
                            }
                        }
                        
                        // 确保至少有 1 秒
                        if (newEnd - newStart < 1000) {
                            newEnd = minOf(newStart + 1000, totalDuration)
                        }
                        
                        onTrimChange(newStart, newEnd, totalDuration)
                    },
                    valueRange = 0f..totalDuration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 时间标签
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
        
        // 提示信息
        if (selectedDuration > maxTrimDuration) {
            Text(
                text = "⚠️ 裁剪片段不能超过 5 秒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
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
