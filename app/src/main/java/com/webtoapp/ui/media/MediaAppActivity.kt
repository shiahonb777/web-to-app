package com.webtoapp.ui.media

import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.gson.Gson
import com.webtoapp.ui.theme.WebToAppTheme
import java.io.File

/**
 * 媒体应用展示 Activity
 * 用于显示图片或播放视频的独立应用
 */
class MediaAppActivity : AppCompatActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 加载配置
        val config = loadConfig()
        if (config == null) {
            finish()
            return
        }
        
        // 设置全屏
        setupFullscreen()
        
        // 设置屏幕方向
        requestedOrientation = if (config.landscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        setContent {
            WebToAppTheme {
                MediaAppScreen(
                    config = config,
                    onExit = { finish() }
                )
            }
        }
    }
    
    private fun loadConfig(): MediaAppConfig? {
        return try {
            val configJson = assets.open("app_config.json").bufferedReader().readText()
            Gson().fromJson(configJson, MediaAppConfig::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

/**
 * 媒体应用配置（从 JSON 加载）
 */
data class MediaAppConfig(
    val appName: String = "",
    val mediaType: String = "IMAGE",      // "IMAGE" or "VIDEO"
    val mediaPath: String = "",           // assets 中的路径
    val enableAudio: Boolean = true,
    val loop: Boolean = true,
    val autoPlay: Boolean = true,
    val fillScreen: Boolean = true,
    val landscape: Boolean = false,
    val backgroundColor: String = "#000000"
)

/**
 * 媒体应用主界面
 */
@Composable
fun MediaAppScreen(
    config: MediaAppConfig,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val bgColor = try {
        Color(android.graphics.Color.parseColor(config.backgroundColor))
    } catch (e: Exception) {
        Color.Black
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        when (config.mediaType) {
            "IMAGE" -> {
                // 图片展示
                ImageDisplay(
                    context = context,
                    fillScreen = config.fillScreen
                )
            }
            "VIDEO" -> {
                // 视频播放
                VideoPlayer(
                    context = context,
                    enableAudio = config.enableAudio,
                    loop = config.loop,
                    autoPlay = config.autoPlay,
                    fillScreen = config.fillScreen
                )
            }
        }
    }
}

/**
 * 图片展示组件
 */
@Composable
fun ImageDisplay(
    context: android.content.Context,
    fillScreen: Boolean
) {
    var imageLoaded by remember { mutableStateOf(false) }
    
    // 从 assets 加载图片
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data("file:///android_asset/media_content.png")
            .crossfade(true)
            .listener(
                onSuccess = { _, _ -> imageLoaded = true }
            )
            .build()
    )
    
    Image(
        painter = painter,
        contentDescription = "媒体内容",
        modifier = Modifier.fillMaxSize(),
        contentScale = if (fillScreen) ContentScale.Crop else ContentScale.Fit
    )
}

/**
 * 视频播放组件
 */
@Composable
fun VideoPlayer(
    context: android.content.Context,
    enableAudio: Boolean,
    loop: Boolean,
    autoPlay: Boolean,
    fillScreen: Boolean
) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    
    // 自动隐藏控制栏
    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showControls = !showControls
            }
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            try {
                                val afd = ctx.assets.openFd("media_content.mp4")
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                    setSurface(holder.surface)
                                    
                                    val volume = if (enableAudio) 1f else 0f
                                    setVolume(volume, volume)
                                    
                                    isLooping = loop
                                    
                                    setOnPreparedListener {
                                        if (autoPlay) {
                                            start()
                                            isPlaying = true
                                        }
                                    }
                                    
                                    prepareAsync()
                                }
                                afd.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                        
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            mediaPlayer?.release()
                            mediaPlayer = null
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 播放控制（简单的播放/暂停）
        if (showControls && !autoPlay) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                FilledIconButton(
                    onClick = {
                        mediaPlayer?.let { mp ->
                            if (mp.isPlaying) {
                                mp.pause()
                                isPlaying = false
                            } else {
                                mp.start()
                                isPlaying = true
                            }
                        }
                    }
                ) {
                    Text(if (isPlaying) "⏸" else "▶")
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}
