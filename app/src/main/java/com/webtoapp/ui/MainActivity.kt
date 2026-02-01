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
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.i18n.InitializeLanguage
import com.webtoapp.core.i18n.LanguageManager
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
        // 权限结果目前无需特殊处理，失败时相关功能会在使用时再报错
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查是否为 Shell 模式（添加异常保护）
        val isShell = try {
            val shellManager = WebToAppApplication.shellMode
            val result = shellManager.isShellMode()
            android.util.Log.d("MainActivity", "Check shell mode: isShellMode=$result")
            result
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to check shell mode", e)
            false
        } catch (e: Error) {
            android.util.Log.e("MainActivity", "Critical error while checking shell mode", e)
            false
        }

        if (isShell) {
            // Shell 模式：直接跳转到 ShellActivity
            android.util.Log.d("MainActivity", "Shell mode detected, launching ShellActivity")
            try {
                startActivity(Intent(this, ShellActivity::class.java))
                finish()
                return
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to launch ShellActivity", e)
                // 继续显示主界面
            }
        }
        android.util.Log.d("MainActivity", "Normal mode, showing main UI")

        // 启用边到边显示（Android 15+ 兼容）
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "enableEdgeToEdge failed", e)
        }

        requestNecessaryPermissions()

        // 检查快捷方式权限
        checkShortcutPermission()

        // 设置窗口装饰以支持边到边显示
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "setDecorFitsSystemWindows failed", e)
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
                    InitializeLanguage()
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
                            title = { Text(Strings.shortcutPermissionTitle) },
                            text = { Text(shortcutPermissionMessage) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showShortcutPermissionDialog = false
                                        openAppSettings()
                                    }
                                ) {
                                    Text(Strings.openSettings)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showShortcutPermissionDialog = false }
                                ) {
                                    Text(Strings.notNow)
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
            // 检查是否支持固定快捷方式
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
                // 获取厂商特定的提示信息
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
                Strings.shortcutPermissionMessageXiaomi
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                Strings.shortcutPermissionMessageHuawei
            }
            manufacturer.contains("oppo") -> {
                Strings.shortcutPermissionMessageOppo
            }
            manufacturer.contains("vivo") -> {
                Strings.shortcutPermissionMessageVivo
            }
            manufacturer.contains("meizu") -> {
                Strings.shortcutPermissionMessageMeizu
            }
            manufacturer.contains("samsung") -> {
                Strings.shortcutPermissionMessageSamsung
            }
            else -> {
                Strings.shortcutPermissionMessageGeneric
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
            permissionLauncher.launch(needRequest.toTypedArray())
        }
    }
}
