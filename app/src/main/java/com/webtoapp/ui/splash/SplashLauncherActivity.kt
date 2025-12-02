package com.webtoapp.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.webtoapp.ui.theme.WebToAppTheme
import kotlinx.coroutines.delay
import java.io.File

/**
 * 启动画面中转 Activity
 * 用于本地应用快捷方式，先显示启动画面再启动目标应用
 */
class SplashLauncherActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_SPLASH_TYPE = "splash_type"       // "IMAGE" or "VIDEO"
        const val EXTRA_SPLASH_PATH = "splash_path"
        const val EXTRA_SPLASH_DURATION = "splash_duration"  // 秒
        const val EXTRA_SPLASH_CLICK_SKIP = "splash_click_skip"
        const val EXTRA_VIDEO_START_MS = "video_start_ms"
        const val EXTRA_VIDEO_END_MS = "video_end_ms"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        val splashType = intent.getStringExtra(EXTRA_SPLASH_TYPE) ?: "IMAGE"
        val splashPath = intent.getStringExtra(EXTRA_SPLASH_PATH)
        val splashDuration = intent.getIntExtra(EXTRA_SPLASH_DURATION, 3)
        val clickToSkip = intent.getBooleanExtra(EXTRA_SPLASH_CLICK_SKIP, true)
        val videoStartMs = intent.getLongExtra(EXTRA_VIDEO_START_MS, 0L)
        val videoEndMs = intent.getLongExtra(EXTRA_VIDEO_END_MS, 5000L)

        // 验证参数
        if (targetPackage.isNullOrBlank()) {
            finish()
            return
        }

        // 验证启动画面媒体是否存在
        val hasValidSplash = splashPath != null && File(splashPath).exists()

        setContent {
            WebToAppTheme {
                SplashLauncherScreen(
                    targetPackage = targetPackage,
                    hasValidSplash = hasValidSplash,
                    splashType = splashType,
                    splashPath = splashPath,
                    splashDuration = splashDuration,
                    clickToSkip = clickToSkip,
                    videoStartMs = videoStartMs,
                    videoEndMs = videoEndMs,
                    onLaunchTarget = { launchTargetApp(targetPackage) }
                )
            }
        }
    }

    private fun launchTargetApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }
}

/**
 * 启动画面中转屏幕
 */
@Composable
fun SplashLauncherScreen(
    targetPackage: String,
    hasValidSplash: Boolean,
    splashType: String,
    splashPath: String?,
    splashDuration: Int,
    clickToSkip: Boolean,
    videoStartMs: Long,
    videoEndMs: Long,
    onLaunchTarget: () -> Unit
) {
    // 如果没有有效的启动画面，直接启动目标应用
    LaunchedEffect(hasValidSplash) {
        if (!hasValidSplash) {
            onLaunchTarget()
        }
    }

    if (!hasValidSplash || splashPath == null) {
        // 显示加载中
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // 倒计时状态
    var countdown by remember { mutableIntStateOf(
        if (splashType == "VIDEO") ((videoEndMs - videoStartMs) / 1000).toInt() else splashDuration
    ) }
    var showSplash by remember { mutableStateOf(true) }

    // 倒计时逻辑（仅对图片）
    if (splashType == "IMAGE") {
        LaunchedEffect(countdown) {
            if (countdown > 0) {
                delay(1000L)
                countdown--
            } else {
                showSplash = false
                onLaunchTarget()
            }
        }
    }

    // 启动画面 UI
    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        SplashContent(
            splashType = splashType,
            splashPath = splashPath,
            countdown = countdown,
            clickToSkip = clickToSkip,
            videoStartMs = videoStartMs,
            videoEndMs = videoEndMs,
            onSkip = {
                showSplash = false
                onLaunchTarget()
            }
        )
    }
}

/**
 * 启动画面内容
 */
@Composable
fun SplashContent(
    splashType: String,
    splashPath: String,
    countdown: Int,
    clickToSkip: Boolean,
    videoStartMs: Long,
    videoEndMs: Long,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val videoDurationMs = videoEndMs - videoStartMs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (clickToSkip) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSkip() }
                } else {
                    Modifier
                }
            )
    ) {
        when (splashType) {
            "IMAGE" -> {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(File(splashPath))
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "启动画面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            "VIDEO" -> {
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                
                // 监控播放进度
                LaunchedEffect(mediaPlayer) {
                    mediaPlayer?.let { mp ->
                        while (mp.isPlaying) {
                            if (mp.currentPosition >= videoEndMs) {
                                mp.pause()
                                onSkip()
                                break
                            }
                            delay(100)
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
                                            setDataSource(splashPath)
                                            setSurface(holder.surface)
                                            setVolume(0f, 0f)
                                            isLooping = false
                                            setOnPreparedListener {
                                                seekTo(videoStartMs.toInt())
                                                start()
                                            }
                                            setOnCompletionListener { onSkip() }
                                            prepareAsync()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        onSkip()
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
                    }
                }
            }
        }

        // 倒计时/跳过提示
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
                val displayTime = if (splashType == "VIDEO") (videoDurationMs / 1000).toInt() else countdown
                if (displayTime > 0) {
                    Text(
                        text = "${displayTime}s",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (clickToSkip) {
                    if (displayTime > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("|", color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "跳过",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
