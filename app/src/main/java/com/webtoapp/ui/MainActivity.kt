package com.webtoapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.WindowCompat
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.i18n.LanguageManager
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.ui.components.FirstLaunchLanguageScreen
import com.webtoapp.ui.navigation.AppNavigation
import com.webtoapp.ui.shell.ShellActivity
import com.webtoapp.ui.theme.ThemeManager
import com.webtoapp.ui.theme.WebToAppTheme
import com.webtoapp.ui.theme.enhanced.EnhancedThemeWrapper

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
        
        // Check是否为 Shell 模式（添加异常保护）
        val isShell = try {
            val shellManager = WebToAppApplication.shellMode
            val result = shellManager.isShellMode()
            AppLogger.d("MainActivity", "检查 Shell 模式: isShellMode=$result")
            result
        } catch (e: Exception) {
            AppLogger.e("MainActivity", "检查 Shell 模式失败", e)
            false
        } catch (e: Error) {
            AppLogger.e("MainActivity", "检查 Shell 模式时发生严重错误", Error(e))
            false
        }
        
        if (isShell) {
            // Shell 模式：直接跳转到 ShellActivity
            AppLogger.i("MainActivity", "进入 Shell 模式，跳转到 ShellActivity")
            try {
                startActivity(Intent(this, ShellActivity::class.java))
                finish()
                return
            } catch (e: Exception) {
                AppLogger.e("MainActivity", "跳转到 ShellActivity 失败", e)
                // Resume显示主界面
            }
        }
        AppLogger.i("MainActivity", "普通模式，显示主界面")

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
            WebToAppTheme { isDarkTheme ->
                // 根据主题设置状态栏颜色（跟随主题色）
                LaunchedEffect(isDarkTheme) {
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    
                    // 主应用使用主题色状态栏
                    if (isDarkTheme) {
                        // 深色主题：深色背景 + 浅色图标
                        window.statusBarColor = android.graphics.Color.parseColor("#1C1B1F")
                        windowInsetsController.isAppearanceLightStatusBars = false
                    } else {
                        // 浅色主题：浅色背景 + 深色图标
                        window.statusBarColor = android.graphics.Color.parseColor("#FFFBFE")
                        windowInsetsController.isAppearanceLightStatusBars = true
                    }
                    
                    // 导航栏保持透明
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
                }
                
                // 强化版主题包装器 - 根据 UI 模式自动显示增强背景
                EnhancedThemeWrapper {
                    val context = LocalContext.current
                    val languageManager = remember { LanguageManager.getInstance(context) }
                    val hasSelectedLanguage by languageManager.hasSelectedLanguageFlow.collectAsState(initial = true)
                    
                    // 首次启动显示语言选择，否则显示主内容
                    if (!hasSelectedLanguage && showLanguageSelection) {
                        FirstLaunchLanguageScreen(
                            onLanguageSelected = {
                                showLanguageSelection = false
                            }
                        )
                    } else {
                        // 主内容
                        AppNavigation()
                    }
                    
                    // 快捷方式权限提示对话框
                    if (showShortcutPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showShortcutPermissionDialog = false },
                            title = { Text("需要快捷方式权限") },
                            text = { Text(shortcutPermissionMessage) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showShortcutPermissionDialog = false
                                        openAppSettings()
                                    }
                                ) {
                                    Text("去设置")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showShortcutPermissionDialog = false }
                                ) {
                                    Text("稍后再说")
                                }
                            }
                        )
                    }
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
                "检测到您使用的是小米/红米手机，需要开启「桌面快捷方式」权限才能创建应用快捷方式。\n\n" +
                "请前往：设置 > 应用设置 > 应用管理 > WebToApp > 权限管理 > 桌面快捷方式"
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                "检测到您使用的是华为/荣耀手机，需要开启「创建桌面快捷方式」权限。\n\n" +
                "请前往：设置 > 应用 > 应用管理 > WebToApp > 权限 > 创建桌面快捷方式"
            }
            manufacturer.contains("oppo") -> {
                "检测到您使用的是 OPPO 手机，需要开启「桌面快捷方式」权限。\n\n" +
                "请前往：设置 > 应用管理 > WebToApp > 权限 > 桌面快捷方式"
            }
            manufacturer.contains("vivo") -> {
                "检测到您使用的是 vivo 手机，需要开启「桌面快捷方式」权限。\n\n" +
                "请前往：i管家 > 应用管理 > 权限管理 > WebToApp > 桌面快捷方式"
            }
            manufacturer.contains("meizu") -> {
                "检测到您使用的是魅族手机，需要开启「桌面快捷方式」权限。\n\n" +
                "请前往：手机管家 > 权限管理 > WebToApp > 桌面快捷方式"
            }
            manufacturer.contains("samsung") -> {
                "检测到您使用的是三星手机，请确认桌面已解锁编辑状态。\n\n" +
                "您也可以长按应用图标，选择「添加到主屏幕」来创建快捷方式。"
            }
            else -> {
                "当前启动器可能不支持创建快捷方式，请检查桌面设置或应用权限。\n\n" +
                "点击「去设置」打开应用详情页，查看是否有相关权限选项。"
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
            AppLogger.d("MainActivity", "请求权限: ${needRequest.joinToString()}")
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
    
    override fun onDestroy() {
        AppLogger.lifecycle("MainActivity", "onDestroy")
        super.onDestroy()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        AppLogger.lifecycle("MainActivity", "onSaveInstanceState")
        super.onSaveInstanceState(outState)
    }
}
