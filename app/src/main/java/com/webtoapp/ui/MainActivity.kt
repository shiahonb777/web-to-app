package com.webtoapp.ui

import android.Manifest
import android.content.Intent
import com.webtoapp.core.auth.GoogleSignInHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.WindowCompat
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.i18n.LanguageManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.components.FirstLaunchLanguageScreen
import com.webtoapp.ui.navigation.AppNavigation
import com.webtoapp.ui.shell.ShellActivity
import com.webtoapp.ui.theme.CircularRevealOverlay
import com.webtoapp.ui.theme.LocalThemeRevealState
import com.webtoapp.ui.theme.WebToAppTheme
import com.webtoapp.ui.theme.rememberThemeRevealState

/**
 * 主Activity - 应用入口
 */
class MainActivity : ComponentActivity() {

    // 首次启动语言选择状态
    private var showLanguageSelection by mutableStateOf(true)
    
    // 快捷方式权限对话框状态
    private var showShortcutPermissionDialog by mutableStateOf(false)
    private var shortcutPermissionMessage by mutableStateOf("")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permission结果目前无需特殊处理，失败时相关功能会在使用时再报错
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.lifecycle("MainActivity", "onCreate", "savedInstanceState=${savedInstanceState != null}")
        
        // 处理 Google OAuth 回调（冷启动时）
        handleGoogleOAuthIfNeeded(intent)
        
        // Check是否为 Shell 模式（添加异常保护）
        val isShell = try {
            val shellManager = WebToAppApplication.shellMode
            val result = shellManager.isShellMode()
            AppLogger.d("MainActivity", "Shell mode check: isShellMode=$result")
            result
        } catch (e: Exception) {
            AppLogger.e("MainActivity", "Shell mode check failed", e)
            false
        } catch (e: Error) {
            AppLogger.e("MainActivity", "Shell mode check critical error", Error(e))
            false
        }
        
        if (isShell) {
            // Shell 模式：直接跳转到 ShellActivity
            AppLogger.i("MainActivity", "Entering shell mode, redirecting to ShellActivity")
            try {
                startActivity(Intent(this, ShellActivity::class.java))
                finish()
                return
            } catch (e: Exception) {
                AppLogger.e("MainActivity", "Failed to start ShellActivity", e)
                // Resume显示主界面
            }
        }
        AppLogger.i("MainActivity", "Normal mode, showing main UI")

        // Enable边到边显示（Android 15+ 兼容）
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            AppLogger.w("MainActivity", "enableEdgeToEdge failed", e)
        }

        requestNecessaryPermissions()
        
        // Check快捷方式权限
        checkShortcutPermission()

        // Set窗口装饰以支持边到边显示
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (e: Exception) {
            AppLogger.w("MainActivity", "setDecorFitsSystemWindows failed", e)
        }
        
        setContent {
            // 圆形揭示动画状态（在主题之外创建，以便截图包含当前主题）
            val themeRevealState = rememberThemeRevealState()
            
            WebToAppTheme { isDarkTheme ->
                // 根据主题设置状态栏颜色（跟随主题色）
                val themeColors = MaterialTheme.colorScheme
                LaunchedEffect(isDarkTheme, themeColors.background) {
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    
                    // 状态栏透明 — 沉浸式
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                    
                    // 导航栏保持透明
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
                }
                
                val context = LocalContext.current
                val languageManager = remember { LanguageManager.getInstance(context) }
                val hasSelectedLanguage by languageManager.hasSelectedLanguageFlow.collectAsState(initial = true)
                
                // 提供 ThemeRevealState 给子组件（HomeScreen 的切换按钮）
                CompositionLocalProvider(
                    LocalThemeRevealState provides themeRevealState
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 主内容
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (!hasSelectedLanguage && showLanguageSelection) {
                                FirstLaunchLanguageScreen(
                                    onLanguageSelected = {
                                        showLanguageSelection = false
                                    }
                                )
                            } else {
                                AppNavigation()
                            }
                        }
                        
                        // 圆形揭示动画叠层（在所有内容之上）
                        CircularRevealOverlay(revealState = themeRevealState)
                    }
                }
                
                // 快捷方式权限提示对话框
                if (showShortcutPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showShortcutPermissionDialog = false },
                        title = { Text(Strings.shortcutPermissionTitle) },
                        text = { Text(shortcutPermissionMessage) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showShortcutPermissionDialog = false
                                    openAppSettings()
                                }
                            ) {
                                Text(Strings.shortcutPermissionGoToSettings)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showShortcutPermissionDialog = false }
                            ) {
                                Text(Strings.shortcutPermissionLater)
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * 检查快捷方式权限
     */
    private fun checkShortcutPermission() {
        // 只在 Android 8.0+ 检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Check是否支持固定快捷方式
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
                // Get厂商特定的提示信息
                shortcutPermissionMessage = buildShortcutPermissionMessage()
                showShortcutPermissionDialog = true
            }
        }
    }

    /**
     * 构建厂商特定的快捷方式权限提示
     */
    private fun buildShortcutPermissionMessage(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                Strings.shortcutPermissionXiaomi
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                Strings.shortcutPermissionHuawei
            }
            manufacturer.contains("oppo") -> {
                Strings.shortcutPermissionOppo
            }
            manufacturer.contains("vivo") -> {
                Strings.shortcutPermissionVivo
            }
            manufacturer.contains("meizu") -> {
                Strings.shortcutPermissionMeizu
            }
            manufacturer.contains("samsung") -> {
                Strings.shortcutPermissionSamsung
            }
            else -> {
                Strings.shortcutPermissionGeneric
            }
        }
    }

    /**
     * 打开应用设置页面
     */
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开应用设置，尝试打开通用设置
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e2: Exception) {
                // 忽略
            }
        }
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val needRequest = permissions.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }

        if (needRequest.isNotEmpty()) {
            AppLogger.d("MainActivity", "Requesting permissions: ${needRequest.joinToString()}")
            permissionLauncher.launch(needRequest.toTypedArray())
        }
    }
    
    override fun onStart() {
        super.onStart()
        AppLogger.lifecycle("MainActivity", "onStart")
    }
    
    override fun onResume() {
        super.onResume()
        AppLogger.lifecycle("MainActivity", "onResume")
    }
    
    override fun onPause() {
        AppLogger.lifecycle("MainActivity", "onPause")
        super.onPause()
    }
    
    override fun onStop() {
        AppLogger.lifecycle("MainActivity", "onStop")
        super.onStop()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppLogger.lifecycle("MainActivity", "onNewIntent")
        // 处理 Google OAuth 回调（从浏览器返回时）
        handleGoogleOAuthIfNeeded(intent)
    }
    
    /**
     * 检查并处理 Google OAuth 回调
     */
    private fun handleGoogleOAuthIfNeeded(intent: Intent?) {
        if (GoogleSignInHelper.isOAuthCallback(intent)) {
            val uri = intent?.data ?: return
            AppLogger.i("MainActivity", "Handling Google OAuth callback: ${uri.scheme}://${uri.host}")
            CoroutineScope(Dispatchers.Main).launch {
                GoogleSignInHelper.handleOAuthCallback(uri)
            }
        }
    }
    
    override fun onDestroy() {
        AppLogger.lifecycle("MainActivity", "onDestroy")
        super.onDestroy()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        AppLogger.lifecycle("MainActivity", "onSaveInstanceState")
        super.onSaveInstanceState(outState)
    }
}
