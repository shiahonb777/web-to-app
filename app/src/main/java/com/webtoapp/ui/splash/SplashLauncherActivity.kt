package com.webtoapp.ui.splash

import android.content.Intent
import android.content.pm.ActivityInfo
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
        const val EXTRA_SPLASH_LANDSCAPE = "splash_landscape"
        const val EXTRA_SPLASH_FILL_SCREEN = "splash_fill_screen"
        const val EXTRA_SPLASH_ENABLE_AUDIO = "splash_enable_audio"
        // 激活码配置
        const val EXTRA_ACTIVATION_ENABLED = "activation_enabled"
        const val EXTRA_ACTIVATION_CODES = "activation_codes"
        // 公告配置
        const val EXTRA_ANNOUNCEMENT_ENABLED = "announcement_enabled"
        const val EXTRA_ANNOUNCEMENT_TITLE = "announcement_title"
        const val EXTRA_ANNOUNCEMENT_CONTENT = "announcement_content"
        const val EXTRA_ANNOUNCEMENT_LINK = "announcement_link"
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
        val isLandscape = intent.getBooleanExtra(EXTRA_SPLASH_LANDSCAPE, false)
        val fillScreen = intent.getBooleanExtra(EXTRA_SPLASH_FILL_SCREEN, true)
        val enableAudio = intent.getBooleanExtra(EXTRA_SPLASH_ENABLE_AUDIO, false)
        // 激活码配置（从逗号分隔的字符串解析）
        val activationEnabled = intent.getBooleanExtra(EXTRA_ACTIVATION_ENABLED, false)
        val activationCodesStr = intent.getStringExtra(EXTRA_ACTIVATION_CODES) ?: ""
        val activationCodes = if (activationCodesStr.isNotBlank()) {
            activationCodesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        // 公告配置
        val announcementEnabled = intent.getBooleanExtra(EXTRA_ANNOUNCEMENT_ENABLED, false)
        val announcementTitle = intent.getStringExtra(EXTRA_ANNOUNCEMENT_TITLE) ?: ""
        val announcementContent = intent.getStringExtra(EXTRA_ANNOUNCEMENT_CONTENT) ?: ""
        val announcementLink = intent.getStringExtra(EXTRA_ANNOUNCEMENT_LINK)

        // 验证参数
        if (targetPackage.isNullOrBlank()) {
            finish()
            return
        }

        // 验证启动画面媒体是否存在
        val hasValidSplash = splashPath != null && File(splashPath).exists()
        
        // 处理横屏显示
        if (hasValidSplash && isLandscape) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

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
                    fillScreen = fillScreen,
                    enableAudio = enableAudio,
                    activationEnabled = activationEnabled,
                    activationCodes = activationCodes,
                    announcementEnabled = announcementEnabled,
                    announcementTitle = announcementTitle,
                    announcementContent = announcementContent,
                    announcementLink = announcementLink,
                    onLaunchTarget = { launchTargetApp(targetPackage) }
                )
            }
        }
    }

    private fun launchTargetApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // 使用 finishAndRemoveTask 彻底移除当前任务
        // 这样在最近应用中不会留下 SplashLauncherActivity 的任务
        finishAndRemoveTask()
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
    fillScreen: Boolean,
    enableAudio: Boolean = false,
    activationEnabled: Boolean = false,
    activationCodes: List<String> = emptyList(),
    announcementEnabled: Boolean = false,
    announcementTitle: String = "",
    announcementContent: String = "",
    announcementLink: String? = null,
    onLaunchTarget: () -> Unit
) {
    val context = LocalContext.current
    
    // 激活状态
    var isActivated by remember { mutableStateOf(!activationEnabled) }
    var showActivationDialog by remember { mutableStateOf(activationEnabled) }
    
    // 公告状态
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    
    // 启动画面状态
    var showSplash by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(
        if (splashType == "VIDEO") ((videoEndMs - videoStartMs) / 1000).toInt() else splashDuration
    ) }
    
    // 激活成功后检查公告和启动画面
    LaunchedEffect(isActivated) {
        if (isActivated) {
            // 检查公告
            if (announcementEnabled && announcementTitle.isNotEmpty()) {
                showAnnouncementDialog = true
            } else {
                // 检查启动画面
                if (hasValidSplash && splashPath != null) {
                    showSplash = true
                } else {
                    onLaunchTarget()
                }
            }
        }
    }
    
    // 公告关闭后显示启动画面
    fun onAnnouncementDismiss() {
        showAnnouncementDialog = false
        if (hasValidSplash && splashPath != null) {
            showSplash = true
        } else {
            onLaunchTarget()
        }
    }
    
    // 如果不需要激活也不需要公告也不需要启动画面，直接启动目标应用
    LaunchedEffect(Unit) {
        if (!activationEnabled && !announcementEnabled && !hasValidSplash) {
            onLaunchTarget()
        }
    }

    // 倒计时逻辑（仅对图片）
    if (showSplash && splashType == "IMAGE") {
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
    
    // 主UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 未激活状态显示激活界面
        if (!isActivated) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "应用需要激活",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showActivationDialog = true }) {
                    Text("输入激活码")
                }
            }
        }

        // 启动画面
        AnimatedVisibility(
            visible = showSplash && splashPath != null,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            SplashContent(
                splashType = splashType,
                splashPath = splashPath!!,
                countdown = countdown,
                clickToSkip = clickToSkip,
                videoStartMs = videoStartMs,
                videoEndMs = videoEndMs,
                fillScreen = fillScreen,
                enableAudio = enableAudio,
                onSkip = {
                    showSplash = false
                    onLaunchTarget()
                }
            )
        }
        
        // 加载指示器（非激活非启动画面状态）
        if (!showActivationDialog && !showAnnouncementDialog && !showSplash && isActivated) {
            CircularProgressIndicator(color = Color.White)
        }
    }
    
    // 激活码对话框
    if (showActivationDialog) {
        ActivationDialog(
            onDismiss = { showActivationDialog = false },
            onActivate = { code ->
                if (activationCodes.contains(code)) {
                    isActivated = true
                    showActivationDialog = false
                }
            }
        )
    }
    
    // 公告对话框
    if (showAnnouncementDialog) {
        AnnouncementDialog(
            title = announcementTitle,
            content = announcementContent,
            linkUrl = announcementLink,
            onDismiss = { onAnnouncementDismiss() },
            onLinkClick = { url ->
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            }
        )
    }
}

/**
 * 激活码对话框
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
        title = { Text("激活应用") },
        text = {
            Column {
                Text("请输入激活码以继续使用")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        error = null
                    },
                    label = { Text("激活码") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank()) {
                        error = "请输入激活码"
                    } else {
                        onActivate(code)
                    }
                }
            ) {
                Text("激活")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 公告对话框
 */
@Composable
fun AnnouncementDialog(
    title: String,
    content: String,
    linkUrl: String?,
    onDismiss: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(content)
                if (!linkUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onLinkClick(linkUrl) }) {
                        Text("查看详情")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("我知道了")
            }
        }
    )
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
    fillScreen: Boolean,
    enableAudio: Boolean = false,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val videoDurationMs = videoEndMs - videoStartMs
    val contentScaleMode = if (fillScreen) ContentScale.Crop else ContentScale.Fit
    
    // 视频剩余时间（用于动态倒计时显示）
    var videoRemainingMs by remember { mutableLongStateOf(videoDurationMs) }

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
            ),
        contentAlignment = Alignment.Center
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
                    contentScale = contentScaleMode
                )
            }
            "VIDEO" -> {
                var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }
                
                // 监控播放进度
                // 仅在播放器准备就绪后开始监控
                LaunchedEffect(isPlayerReady) {
                    if (!isPlayerReady) return@LaunchedEffect
                    mediaPlayer?.let { mp ->
                        // 等待播放器真正开始播放
                        while (!mp.isPlaying) {
                            delay(50)
                            if (mediaPlayer == null) return@LaunchedEffect
                        }
                        // 监控播放进度并更新剩余时间
                        while (mp.isPlaying) {
                            val currentPos = mp.currentPosition
                            // 更新剩余时间用于倒计时显示
                            videoRemainingMs = (videoEndMs - currentPos).coerceAtLeast(0L)
                            if (currentPos >= videoEndMs) {
                                mp.pause()
                                onSkip()
                                break
                            }
                            delay(100) // 100ms 更新一次倒计时显示
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
                                            // 根据配置决定是否启用音频
                                            val volume = if (enableAudio) 1f else 0f
                                            setVolume(volume, volume)
                                            isLooping = false
                                            setOnPreparedListener {
                                                seekTo(videoStartMs.toInt())
                                                start()
                                                isPlayerReady = true
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
                // 视频使用动态剩余时间，图片使用传入的 countdown
                val displayTime = if (splashType == "VIDEO") ((videoRemainingMs + 999) / 1000).toInt() else countdown
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
