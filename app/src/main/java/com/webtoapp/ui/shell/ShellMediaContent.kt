package com.webtoapp.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.delay

/**
 * Shell mode animation( from assets load, support)
 */
@Composable
fun ShellSplashOverlay(
    splashType: String,
    countdown: Int,
    videoStartMs: Long = 0,
    videoEndMs: Long = 5000,
    fillScreen: Boolean = true,
    enableAudio: Boolean = false,    // Yes
    onSkip: (() -> Unit)?,           // Note
    onComplete: (() -> Unit)? = null // Play
) {
    val context = LocalContext.current
    val extension = if (splashType == "VIDEO") "mp4" else "png"
    val assetPath = "splash_media.$extension"
    val videoDurationMs = videoEndMs - videoStartMs
    val contentScaleMode = if (fillScreen) ContentScale.Crop else ContentScale.Fit
    
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
        when (splashType) {
            "IMAGE" -> {
                // Image animation( from assets load)
                // file: ///android_asset/ load assets in
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data("file:///android_asset/$assetPath")
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = Strings.cdSplashScreen,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScaleMode
                )
            }
            "VIDEO" -> {
                // Video animation( support, support)
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }
                var tempVideoFile by remember { mutableStateOf<java.io.File?>(null) }
                
                // Note
                // only
                LaunchedEffect(isPlayerReady) {
                    if (!isPlayerReady) return@LaunchedEffect
                    mediaPlayer?.let { mp ->
                        // Note
                        while (!mp.isPlaying) {
                            delay(50)
                            if (mediaPlayer == null) return@LaunchedEffect
                        }
                        // andupdate
                        while (mp.isPlaying) {
                            val currentPos = mp.currentPosition
                            // Update for display
                            videoRemainingMs = (videoEndMs - currentPos).coerceAtLeast(0L)
                            if (currentPos >= videoEndMs) {
                                mp.pause()
                                // onComplete,
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
                                        // Check version
                                        val encryptedPath = "$assetPath.enc"
                                        val hasEncrypted = try {
                                            ctx.assets.open(encryptedPath).use { true }
                                        } catch (e: Exception) { false }
                                        
                                        if (hasEncrypted) {
                                            // Encryption: file
                                            AppLogger.d("ShellSplash", "检测到加密启动画面视频")
                                            val decryptor = com.webtoapp.core.crypto.AssetDecryptor(ctx)
                                            val decryptedData = decryptor.loadAsset(assetPath)
                                            val tempFile = java.io.File(ctx.cacheDir, "splash_video_${System.currentTimeMillis()}.mp4")
                                            tempFile.writeBytes(decryptedData)
                                            tempVideoFile = tempFile
                                            
                                            mediaPlayer = android.media.MediaPlayer().apply {
                                                setDataSource(tempFile.absolutePath)
                                                setSurface(holder.surface)
                                                val volume = if (enableAudio) 1f else 0f
                                                setVolume(volume, volume)
                                                isLooping = false
                                                setOnPreparedListener {
                                                    seekTo(videoStartMs.toInt())
                                                    start()
                                                    isPlayerReady = true
                                                }
                                                setOnCompletionListener { onComplete?.invoke() }
                                                prepareAsync()
                                            }
                                        } else {
                                            // openFd
                                            val afd = ctx.assets.openFd(assetPath)
                                            mediaPlayer = android.media.MediaPlayer().apply {
                                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                                setSurface(holder.surface)
                                                val volume = if (enableAudio) 1f else 0f
                                                setVolume(volume, volume)
                                                isLooping = false
                                                setOnPreparedListener {
                                                    seekTo(videoStartMs.toInt())
                                                    start()
                                                    isPlayerReady = true
                                                }
                                                setOnCompletionListener { onComplete?.invoke() }
                                                prepareAsync()
                                            }
                                            afd.close()
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("ShellActivity", "Operation failed", e)
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
                
                DisposableEffect(Unit) {
                    onDispose {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        // Cleanup temp files
                        tempVideoFile?.delete()
                        tempVideoFile = null
                    }
                }
            }
        }

        // / hint
        // Video, countdown
        val displayTime = if (splashType == "VIDEO") ((videoRemainingMs + 999) / 1000).toInt() else countdown
        
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
 * contentdisplay( Shell mode /)
 */
@Composable
fun MediaContentDisplay(
    isVideo: Boolean,
    mediaConfig: com.webtoapp.core.shell.MediaShellConfig
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            // Video( support)
            var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
            var tempVideoFile by remember { mutableStateOf<java.io.File?>(null) }
            val assetPath = "media_content.mp4"
            
            AndroidView(
                factory = { ctx ->
                    android.view.SurfaceView(ctx).apply {
                        holder.addCallback(object : android.view.SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                try {
                                    // Check version
                                    val encryptedPath = "$assetPath.enc"
                                    val hasEncrypted = try {
                                        ctx.assets.open(encryptedPath).use { true }
                                    } catch (e: Exception) { false }
                                    
                                    if (hasEncrypted) {
                                        // Encryption: file
                                        AppLogger.d("MediaContent", "检测到加密媒体视频")
                                        val decryptor = com.webtoapp.core.crypto.AssetDecryptor(ctx)
                                        val decryptedData = decryptor.loadAsset(assetPath)
                                        val tempFile = java.io.File(ctx.cacheDir, "media_video_${System.currentTimeMillis()}.mp4")
                                        tempFile.writeBytes(decryptedData)
                                        tempVideoFile = tempFile
                                        
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(tempFile.absolutePath)
                                            setSurface(holder.surface)
                                            val volume = if (mediaConfig.enableAudio) 1f else 0f
                                            setVolume(volume, volume)
                                            isLooping = mediaConfig.loop
                                            setOnPreparedListener {
                                                if (mediaConfig.autoPlay) start()
                                            }
                                            prepareAsync()
                                        }
                                    } else {
                                        // openFd
                                        val afd = ctx.assets.openFd(assetPath)
                                        mediaPlayer = android.media.MediaPlayer().apply {
                                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                            setSurface(holder.surface)
                                            val volume = if (mediaConfig.enableAudio) 1f else 0f
                                            setVolume(volume, volume)
                                            isLooping = mediaConfig.loop
                                            setOnPreparedListener {
                                                if (mediaConfig.autoPlay) start()
                                            }
                                            prepareAsync()
                                        }
                                        afd.close()
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("ShellActivity", "Operation failed", e)
                                }
                            }
                            
                            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                            
                            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                mediaPlayer?.release()
                                mediaPlayer = null
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            DisposableEffect(Unit) {
                onDispose {
                    mediaPlayer?.release()
                    mediaPlayer = null
                    // Cleanup temp files
                    tempVideoFile?.delete()
                    tempVideoFile = null
                }
            }
        } else {
            // Imagedisplay
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data("file:///android_asset/media_content.png")
                    .crossfade(true)
                    .build()
            )
            
            Image(
                painter = painter,
                contentDescription = Strings.cdMediaContent,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (mediaConfig.fillScreen) 
                    ContentScale.Crop 
                else 
                    ContentScale.Fit
            )
        }
    }
}
