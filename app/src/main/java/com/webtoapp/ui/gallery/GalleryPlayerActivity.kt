package com.webtoapp.ui.gallery

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.gson.Gson
import com.webtoapp.data.model.GalleryConfig
import com.webtoapp.data.model.SplashOrientation

/**
 * 媒体画廊播放器 Activity
 * 支持全屏沉浸式播放图片和视频
 */
class GalleryPlayerActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_CONFIG = "gallery_config"
        private const val EXTRA_START_INDEX = "start_index"
        
        /**
         * 启动画廊播放器
         */
        fun launch(
            context: Context, 
            config: GalleryConfig,
            startIndex: Int = 0
        ) {
            val intent = Intent(context, GalleryPlayerActivity::class.java).apply {
                putExtra(EXTRA_CONFIG, Gson().toJson(config))
                putExtra(EXTRA_START_INDEX, startIndex)
            }
            context.startActivity(intent)
        }
    }
    
    private var config: GalleryConfig? = null
    private var startIndex: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Parse配置
        val configJson = intent.getStringExtra(EXTRA_CONFIG)
        config = configJson?.let {
            try {
                Gson().fromJson(it, GalleryConfig::class.java)
            } catch (e: Exception) {
                null
            }
        }
        startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
        
        val galleryConfig = config
        if (galleryConfig == null || galleryConfig.items.isEmpty()) {
            finish()
            return
        }
        
        // Set屏幕方向
        requestedOrientation = when (galleryConfig.orientation) {
            SplashOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            SplashOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        // Set全屏沉浸模式
        setupImmersiveMode()
        
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            val isDark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
            ) {
                LaunchedEffect(Unit) {
                    hideSystemBars()
                }
                
                GalleryPlayerScreen(
                    config = galleryConfig,
                    startIndex = startIndex.coerceIn(0, galleryConfig.items.size - 1),
                    onBack = { finish() }
                )
            }
        }
    }
    
    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
    
    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
