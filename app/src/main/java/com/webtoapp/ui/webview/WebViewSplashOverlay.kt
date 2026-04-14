package com.webtoapp.ui.webview

import androidx.compose.foundation.Image
import com.webtoapp.ui.components.PremiumButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.SplashConfig
import com.webtoapp.data.model.SplashType
import kotlinx.coroutines.delay
import java.io.File

/**
 * animation
 * support( )
 */
@Composable
fun SplashOverlay(
    splashConfig: SplashConfig,
    countdown: Int,
    onSkip: (() -> Unit)?,           // Note
    onComplete: (() -> Unit)? = null // Play
) {
    val context = LocalContext.current
    val mediaPath = splashConfig.mediaPath ?: return

    // Video related
    val videoStartMs = splashConfig.videoStartMs
    val videoEndMs = splashConfig.videoEndMs
    val videoDurationMs = videoEndMs - videoStartMs
    val contentScaleMode = if (splashConfig.fillScreen) ContentScale.Crop else ContentScale.Fit
    
    // Video( for display)
    var videoRemainingMs by remember { mutableLongStateOf(videoDurationMs) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (onSkip != null) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSkip() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (splashConfig.type) {
            SplashType.IMAGE -> {
                // Image animation
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(File(mediaPath))
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = Strings.cdSplashScreen,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScaleMode
                )
            }
            SplashType.VIDEO -> {
                // Video animation- support
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }
                
                // Note
                // only
                LaunchedEffect(isPlayerReady) {
                    if (!isPlayerReady) return@LaunchedEffect
                    mediaPlayer?.let { mp ->
                        // Note
                        while (!mp.isPlaying) {
                            delay(50)
                            // if
                            if (mediaPlayer == null) return@LaunchedEffect
                        }
                        // andupdate
                        while (mp.isPlaying) {
                            val currentPos = mp.currentPosition
                            // Update for display
                            videoRemainingMs = (videoEndMs - currentPos).coerceAtLeast(0L)
                            if (currentPos >= videoEndMs) {
                                mp.pause()
                                // onComplete
                                onComplete?.invoke()
                                break
                            }
                            delay(100) // 100ms update display
                        }
                    }
                }
                
                AndroidView(
                    factory = { ctx ->
                        android.view.SurfaceView(ctx).apply {
                            holder.addCallback(object : android.view.SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                    try {
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(mediaPath)
                                            setSurface(holder.surface)
                                            // config
                                            val volume = if (splashConfig.enableAudio) 1f else 0f
                                            setVolume(volume, volume)
                                            isLooping = false
                                            setOnPreparedListener { 
                                                // Note
                                                seekTo(videoStartMs.toInt())
                                                start()
                                                isPlayerReady = true
                                            }
                                            setOnCompletionListener { onComplete?.invoke() }
                                            prepareAsync()
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("WebViewActivity", "Operation failed", e)
                                        onComplete?.invoke()
                                    }
                                }
                                override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, ht: Int) {}
                                override fun surfaceDestroyed(h: android.view.SurfaceHolder) {
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // MediaPlayer
                DisposableEffect(Unit) {
                    onDispose {
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }
            }
        }

        // / hint
        // Video, countdown
        val displayTime = if (splashConfig.type == SplashType.VIDEO) {
            ((videoRemainingMs + 999) / 1000).toInt()
        } else {
            countdown
        }
        
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = MaterialTheme.shapes.small,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (displayTime > 0) {
                    Text(
                        text = "${displayTime}s",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (onSkip != null) {
                    if (displayTime > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "|",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Skip",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * activation codedialog
 */
@Composable
fun ActivationDialog(
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.activateApp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(Strings.enterCodeToContinue)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                    },
                    label = { Text(Strings.activationCode) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    if (code.isBlank()) {
                        error = Strings.enterActivationCode
                    } else {
                        onActivate(code)
                    }
                }
            ) {
                Text(Strings.activate)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
