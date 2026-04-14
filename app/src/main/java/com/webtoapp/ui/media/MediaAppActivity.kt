package com.webtoapp.ui.media

import android.content.Context
import com.webtoapp.core.logging.AppLogger
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.theme.WebToAppTheme

/**
 * Media app display Activity.
 * Standalone app for image display and video playback.
 */
class MediaAppActivity : AppCompatActivity() {
    
    companion object {
        private const val EXTRA_WEB_APP = "extra_web_app"
        private val gson = com.webtoapp.util.GsonProvider.gson
        
        /**
         * Launch preview mode (load from WebApp data).
         */
        fun startForPreview(context: Context, webApp: WebApp) {
            context.startActivity(Intent(context, MediaAppActivity::class.java).apply {
                putExtra(EXTRA_WEB_APP, gson.toJson(webApp))
            })
        }
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentConfig: MediaAppConfig? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Try loading WebApp from Intent (preview mode)
        val webAppJson = intent.getStringExtra(EXTRA_WEB_APP)
        val config = if (webAppJson != null) {
            // Preview mode: build config from WebApp data
            val webApp = gson.fromJson(webAppJson, WebApp::class.java)
            createConfigFromWebApp(webApp)
        } else {
            // Standalone mode: load config from assets
            loadConfig()
        }
        
        if (config == null) {
            finish()
            return
        }
        
        // Set app label shown in recents (fix duplicate title issue)
        setTaskDescription(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.app.ActivityManager.TaskDescription.Builder()
                    .setLabel(config.appName)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.app.ActivityManager.TaskDescription(config.appName)
            }
        )
        
        currentConfig = config
        
        // Set full-screen
        setupFullscreen()
        
        // Set screen orientation
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
     * Build MediaAppConfig from WebApp data (preview mode).
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
            backgroundColor = mediaConfig.backgroundColor,
            keepScreenOn = mediaConfig.keepScreenOn
        )
    }
    
    private fun loadConfig(): MediaAppConfig? {
        return try {
            val configJson = assets.open("app_config.json").bufferedReader().readText()
            gson.fromJson(configJson, MediaAppConfig::class.java)
        } catch (e: Exception) {
            AppLogger.e("MediaAppActivity", "Operation failed", e)
            null
        }
    }
    
    private fun setupFullscreen() {
        // Extend content behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set status and navigation bars fully transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Keep screen on (by config)
        if (currentConfig?.keepScreenOn == true) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        // Support cutout/punch-hole areas and extend content into them
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // Hide system bars (status + navigation)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Temporarily show system bars on edge swipe
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    /**
     * Re-apply immersive mode when window gains focus.
     * Prevent bars from staying visible after edge swipe.
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
 * Media app configuration (loaded from JSON).
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
    val backgroundColor: String = "#000000",
    val keepScreenOn: Boolean = true       // Comment
)

/**
 * Media app main screen.
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
 * Image display component.
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
 * Video player component.
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
                                AppLogger.e("MediaAppActivity", "Operation failed", e)
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
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
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
