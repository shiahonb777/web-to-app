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
import com.webtoapp.core.i18n.LanguageManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellModeManager
import com.webtoapp.ui.components.FirstLaunchLanguageScreen
import com.webtoapp.ui.navigation.AppNavigation
import com.webtoapp.ui.shell.ShellActivity
import com.webtoapp.ui.theme.CircularRevealOverlay
import com.webtoapp.ui.theme.LocalThemeRevealState
import com.webtoapp.ui.theme.WebToAppTheme
import com.webtoapp.ui.theme.rememberThemeRevealState
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

/**
 * Activity -
 */
class MainActivity : ComponentActivity() {

    private val shellModeManager: ShellModeManager by inject()

    // Comment
    private var showLanguageSelection by mutableStateOf(true)
    
    // Comment
    private var showShortcutPermissionDialog by mutableStateOf(false)
    private var shortcutPermissionMessage by mutableStateOf("")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permission，
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.lifecycle("MainActivity", "onCreate", "savedInstanceState=${savedInstanceState != null}")
        
        // Google OAuth （）
        handleGoogleOAuthIfNeeded(intent)
        
        // Check Shell （）
        val isShell = try {
            val result = shellModeManager.isShellMode()
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
            // Shell ： ShellActivity
            AppLogger.i("MainActivity", "Entering shell mode, redirecting to ShellActivity")
            try {
                startActivity(Intent(this, ShellActivity::class.java))
                finish()
                return
            } catch (e: Exception) {
                AppLogger.e("MainActivity", "Failed to start ShellActivity", e)
                // Resume
            }
        }
        AppLogger.i("MainActivity", "Normal mode, showing main UI")

        // Enable（Android 15+ ）
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            AppLogger.w("MainActivity", "enableEdgeToEdge failed", e)
        }

        requestNecessaryPermissions()
        
        // Check
        checkShortcutPermission()

        // Set
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } catch (e: Exception) {
            AppLogger.w("MainActivity", "setDecorFitsSystemWindows failed", e)
        }
        
        setContent {
            // （，）
            val themeRevealState = rememberThemeRevealState()
            
            WebToAppTheme { isDarkTheme ->
                // （）
                val themeColors = MaterialTheme.colorScheme
                LaunchedEffect(isDarkTheme, themeColors.background) {
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    
                    // —
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                    
                    // Comment
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
                }
                
                val languageManager: LanguageManager = koinInject()
                val hasSelectedLanguage by languageManager.hasSelectedLanguageFlow.collectAsState(initial = true)
                
                // ThemeRevealState （HomeScreen ）
                CompositionLocalProvider(
                    LocalThemeRevealState provides themeRevealState
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Comment
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
                        
                        // （）
                        CircularRevealOverlay(revealState = themeRevealState)
                    }
                }
                
                // Comment
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
     * Comment
     */
    private fun checkShortcutPermission() {
        // Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Check
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
                // Get
                shortcutPermissionMessage = buildShortcutPermissionMessage()
                showShortcutPermissionDialog = true
            }
        }
    }

    /**
     * Comment
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
     * Comment
     */
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // ，
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e2: Exception) {
                // Comment
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
        // Google OAuth （）
        handleGoogleOAuthIfNeeded(intent)
    }
    
    /**
     * Google OAuth
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
