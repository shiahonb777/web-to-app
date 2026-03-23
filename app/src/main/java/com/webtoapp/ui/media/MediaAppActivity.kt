package com.webtoapp.ui.media

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
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
import com.webtoapp.core.i18n.Strings
import com.google.gson.Gson
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.theme.WebToAppTheme

/**
 * 媒体应用展示 Activity
 * 用于显示图片或播放视频的独立应用
 */
class MediaAppActivity : AppCompatActivity() {
    
    companion object {
        private const val EXTRA_WEB_APP = "extra_web_app"
        
        /**
         * 启动预览模式（从 WebApp 数据加载）
         */
        fun startForPreview(context: Context, webApp: WebApp) {
            context.startActivity(Intent(context, MediaAppActivity::class.java).apply {
                putExtra(EXTRA_WEB_APP, Gson().toJson(webApp))
            })
        }
    }
    
    private var mediaPlayer: MediaPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 尝试从 Intent 加载 WebApp（预览模式）
        val webAppJson = intent.getStringExtra(EXTRA_WEB_APP)
        val config = if (webAppJson != null) {
            // 预览模式：从 WebApp 数据创建配置
            val webApp = Gson().fromJson(webAppJson, WebApp::class.java)
            createConfigFromWebApp(webApp)
        } else {
            // 独立应用模式：从 assets 加载配置
            loadConfig()
        }
        
        if (config == null) {
            finish()
            return
        }
        
        // Set任务列表中显示的应用名称（修复双重名称显示问题）
        setTaskDescription(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.app.ActivityManager.TaskDescription.Builder()
                    .setLabel(config.appName)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.app.ActivityManager.TaskDescription(config.appName)
            }
        )
        
        // Set全屏
        setupFullscreen()
        
        // Set屏幕方向
        requestedOrientation = if (config.landscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        setContent {
            WebToAppTheme { _ ->
                MediaAppScreen(
                    config = config,
                    onExit = { finish() }
                )
            }
        }
    }
    
    /**
     * 从 WebApp 数据创建 MediaAppConfig（预览模式）
     */
    private fun createConfigFromWebApp(webApp: WebApp): MediaAppConfig? {
        val mediaConfig = webApp.mediaConfig ?: return null
        return MediaAppConfig(
            appName = webApp.name,
            mediaType = when (webApp.appType) {
                com.webtoapp.data.model.AppType.IMAGE -> "IMAGE"
                com.webtoapp.data.model.AppType.VIDEO -> "VIDEO"
                else -> return null
            },
            mediaPath = mediaConfig.mediaPath,
            enableAudio = mediaConfig.enableAudio,
            loop = mediaConfig.loop,
            autoPlay = mediaConfig.autoPlay,
            fillScreen = mediaConfig.fillScreen,
            landscape = mediaConfig.orientation == com.webtoapp.data.model.SplashOrientation.LANDSCAPE,
            backgroundColor = mediaConfig.backgroundColor
        )
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
        // 让内容延伸到系统栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set状态栏和导航栏为完全透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Support刘海屏/挖孔屏，内容延伸到刘海区域
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // Hide系统栏（状态栏 + 导航栏）
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // 从边缘滑动时临时显示系统栏
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    /**
     * 窗口获得焦点时重新应用沉浸式模式
     * 防止用户从边缘滑出系统栏后不自动隐藏
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupFullscreen()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

/**
 * Media app configuration（从 JSON 加载）
 */
data class MediaAppConfig(
    val appName: String = "",
    val mediaType: String = "IMAGE",      // "IMAGE" or "VIDEO"
    val mediaPath: String = "",
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
                ImageDisplay(
                    context = context,
                    fillScreen = config.fillScreen,
                    mediaPath = config.mediaPath.takeIf { it.isNotEmpty() }
                )
            }
            "VIDEO" -> {
                VideoPlayer(
                    context = context,
                    enableAudio = config.enableAudio,
                    loop = config.loop,
                    autoPlay = config.autoPlay,
                    fillScreen = config.fillScreen,
                    mediaPath = config.mediaPath.takeIf { it.isNotEmpty() }
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
    fillScreen: Boolean,
    mediaPath: String? = null
) {
    val imageData = mediaPath ?: "file:///android_asset/media_content.png"
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(imageData)
            .crossfade(true)
            .build()
    )
    
    Image(
        painter = painter,
        contentDescription = Strings.mediaContent,
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
    fillScreen: Boolean,
    mediaPath: String? = null
) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    
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
                                val videoPath = mediaPath ?: "media_content.mp4"
                                mediaPlayer = MediaPlayer().apply {
                                    if (videoPath.startsWith("asset:///")) {
                                        val assetPath = videoPath.removePrefix("asset:///")
                                        val afd = ctx.assets.openFd(assetPath)
                                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                        afd.close()
                                    } else {
                                        setDataSource(videoPath)
                                    }
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
