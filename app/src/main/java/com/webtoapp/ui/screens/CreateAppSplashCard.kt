package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.viewmodel.EditState


/**
 * 检查媒体文件是否存在
 */
fun checkMediaExists(context: android.content.Context, uri: android.net.Uri?, savedPath: String?): Boolean {
    // 优先检查保存的路径
    if (!savedPath.isNullOrEmpty()) {
        return java.io.File(savedPath).exists()
    }
    // Check URI
    if (uri != null) {
        return try {
            context.contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    return false
}

/**
 * 启动画面设置卡片
 */
@Composable
fun SplashScreenCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onSelectImage: () -> Unit,
    onSelectVideo: () -> Unit,
    onDurationChange: (Int) -> Unit,
    onClickToSkipChange: (Boolean) -> Unit,
    onOrientationChange: (SplashOrientation) -> Unit,
    onFillScreenChange: (Boolean) -> Unit,
    onEnableAudioChange: (Boolean) -> Unit,
    onVideoTrimChange: (startMs: Long, endMs: Long, totalDurationMs: Long) -> Unit,
    onClearMedia: () -> Unit
) {
    val context = LocalContext.current
    
    // Check媒体文件是否存在
    val mediaExists = remember(editState.splashMediaUri, editState.savedSplashPath) {
        checkMediaExists(context, editState.splashMediaUri, editState.savedSplashPath)
    }
    
    // 如果媒体不存在但 URI 非空，自动清除
    LaunchedEffect(mediaExists, editState.splashMediaUri) {
        if (!mediaExists && editState.splashMediaUri != null) {
            onClearMedia()
        }
    }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题和开关
            CollapsibleCardHeader(
                icon = Icons.Outlined.Wallpaper,
                title = Strings.splashScreen,
                checked = editState.splashEnabled,
                onCheckedChange = onEnabledChange
            )

            AnimatedVisibility(
                visible = editState.splashEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = Strings.splashHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Media预览区域
                if (editState.splashMediaUri != null && mediaExists) {
                    if (editState.splashConfig.type == SplashType.VIDEO) {
                        // Video裁剪器
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    Strings.videoCrop,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                TextButton(onClick = onClearMedia) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(Strings.remove, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            
                            VideoTrimmer(
                                videoPath = editState.savedSplashPath ?: editState.splashMediaUri.toString(),
                                startMs = editState.splashConfig.videoStartMs,
                                endMs = editState.splashConfig.videoEndMs,
                                videoDurationMs = editState.splashConfig.videoDurationMs,
                                onTrimChange = onVideoTrimChange
                            )
                        }
                    } else {
                        // Image预览
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(editState.splashMediaUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = Strings.splashPreview,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Delete按钮
                                IconButton(
                                    onClick = onClearMedia,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        Strings.remove,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty状态 - 选择媒体
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.medium
                            ),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Strings.clickToSelectImageOrVideo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Select按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumOutlinedButton(
                        onClick = onSelectImage,
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    ) {
                        Icon(Icons.Outlined.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.selectImage)
                    }
                    PremiumOutlinedButton(
                        onClick = onSelectVideo,
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    ) {
                        Icon(Icons.Outlined.VideoFile, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.selectVideo)
                    }
                }

                // 以下设置仅在上传媒体后显示
                if (editState.splashMediaUri != null && mediaExists) {
                    // Show时长设置（仅图片显示，视频使用裁剪范围）
                    if (editState.splashConfig.type == SplashType.IMAGE) {
                        Column {
                            Text(
                                text = Strings.displayDurationSeconds.replace("%d", editState.splashConfig.duration.toString()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = editState.splashConfig.duration.toFloat(),
                                onValueChange = { onDurationChange(it.toInt()) },
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 点击跳过设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(Strings.allowSkip, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Strings.allowSkipHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = editState.splashConfig.clickToSkip,
                            onCheckedChange = onClickToSkipChange
                        )
                    }
                    
                    // Show方向设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(Strings.landscapeDisplay, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Strings.landscapeDisplayHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = editState.splashConfig.orientation == SplashOrientation.LANDSCAPE,
                            onCheckedChange = { isLandscape ->
                                onOrientationChange(
                                    if (isLandscape) SplashOrientation.LANDSCAPE 
                                    else SplashOrientation.PORTRAIT
                                )
                            }
                        )
                    }
                    
                    // 铺满屏幕设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(Strings.fillScreen, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Strings.fillScreenHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = editState.splashConfig.fillScreen,
                            onCheckedChange = onFillScreenChange
                        )
                    }
                    
                    // Enable音频设置（仅视频类型显示）
                    if (editState.splashConfig.type == SplashType.VIDEO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(Strings.enableAudio, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    Strings.enableAudioHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PremiumSwitch(
                                checked = editState.splashConfig.enableAudio,
                                onCheckedChange = onEnableAudioChange
                            )
                        }
                    }
                }
              }
            }
        }
    }
}
