package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
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
import com.webtoapp.ui.design.*
import com.webtoapp.ui.viewmodel.EditState





fun checkMediaExists(context: android.content.Context, uri: android.net.Uri?, savedPath: String?): Boolean {

    if (!savedPath.isNullOrEmpty()) {
        return java.io.File(savedPath).exists()
    }

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


    val mediaExists = remember(editState.splashMediaUri, editState.savedSplashPath) {
        checkMediaExists(context, editState.splashMediaUri, editState.savedSplashPath)
    }


    LaunchedEffect(mediaExists, editState.splashMediaUri) {
        if (!mediaExists && editState.splashMediaUri != null) {
            onClearMedia()
        }
    }

    WtaSettingCard {
        Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)) {

            WtaToggleRow(
                icon = Icons.Outlined.Wallpaper,
                title = Strings.splashScreen,
                subtitle = null,
                checked = editState.splashEnabled,
                onCheckedChange = onEnabledChange
            )

            AnimatedVisibility(
                visible = editState.splashEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column(
                  modifier = Modifier.padding(horizontal = WtaSpacing.RowHorizontal),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                if (editState.splashMediaUri != null && mediaExists) {
                    if (editState.splashConfig.type == SplashType.VIDEO) {

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


                if (editState.splashMediaUri != null && mediaExists) {

                    if (editState.splashConfig.type == SplashType.IMAGE) {
                        WtaSliderRow(
                            title = Strings.displayDuration,
                            subtitle = Strings.displayDurationSeconds.replace("%d", editState.splashConfig.duration.toString()),
                            value = editState.splashConfig.duration.toFloat(),
                            onValueChange = { onDurationChange(it.toInt()) },
                            valueLabel = "${editState.splashConfig.duration}s",
                            valueRange = 1f..5f
                        )
                    }


                    WtaToggleRow(
                        title = Strings.allowSkip,
                        subtitle = Strings.allowSkipHint,
                        icon = Icons.Outlined.TouchApp,
                        checked = editState.splashConfig.clickToSkip,
                        onCheckedChange = onClickToSkipChange
                    )


                    WtaToggleRow(
                        title = Strings.landscapeDisplay,
                        subtitle = Strings.landscapeDisplayHint,
                        icon = Icons.Outlined.ScreenRotation,
                        checked = editState.splashConfig.orientation == SplashOrientation.LANDSCAPE,
                        onCheckedChange = { isLandscape ->
                            onOrientationChange(
                                if (isLandscape) SplashOrientation.LANDSCAPE
                                else SplashOrientation.PORTRAIT
                            )
                        }
                    )


                    WtaToggleRow(
                        title = Strings.fillScreen,
                        subtitle = Strings.fillScreenHint,
                        icon = Icons.Outlined.AspectRatio,
                        checked = editState.splashConfig.fillScreen,
                        onCheckedChange = onFillScreenChange
                    )


                    if (editState.splashConfig.type == SplashType.VIDEO) {
                        WtaToggleRow(
                            title = Strings.enableAudio,
                            subtitle = Strings.enableAudioHint,
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
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
