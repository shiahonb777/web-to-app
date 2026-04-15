package com.webtoapp.ui.screens.create.webview.cards.display

import androidx.compose.runtime.Composable
import com.webtoapp.data.model.OrientationMode
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.screens.create.webview.cards.system.FullscreenModeCard as SystemFullscreenModeCard
import com.webtoapp.ui.screens.create.webview.cards.system.LandscapeModeCard as SystemLandscapeModeCard
import com.webtoapp.ui.screens.create.webview.cards.system.StatusBarStyleCard as SystemStatusBarStyleCard
import com.webtoapp.ui.screens.create.webview.cards.system.UserAgentCard as SystemUserAgentCard

@Composable
fun UserAgentCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    SystemUserAgentCard(config = config, onConfigChange = onConfigChange)
}

@Composable
fun StatusBarStyleCard(
    webViewConfig: WebViewConfig,
    onWebViewConfigChange: (WebViewConfig) -> Unit
) {
    SystemStatusBarStyleCard(
        webViewConfig = webViewConfig,
        onWebViewConfigChange = onWebViewConfigChange
    )
}

@Composable
fun FullscreenModeCard(
    enabled: Boolean,
    showStatusBar: Boolean = false,
    showNavigationBar: Boolean = false,
    showToolbar: Boolean = false,
    onEnabledChange: (Boolean) -> Unit,
    onShowStatusBarChange: (Boolean) -> Unit = {},
    onShowNavigationBarChange: (Boolean) -> Unit = {},
    onShowToolbarChange: (Boolean) -> Unit = {}
) {
    SystemFullscreenModeCard(
        enabled = enabled,
        showStatusBar = showStatusBar,
        showNavigationBar = showNavigationBar,
        showToolbar = showToolbar,
        onEnabledChange = onEnabledChange,
        onShowStatusBarChange = onShowStatusBarChange,
        onShowNavigationBarChange = onShowNavigationBarChange,
        onShowToolbarChange = onShowToolbarChange
    )
}

@Composable
fun LandscapeModeCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    orientationMode: OrientationMode = if (enabled) OrientationMode.LANDSCAPE else OrientationMode.PORTRAIT,
    onOrientationModeChange: (OrientationMode) -> Unit = { mode ->
        onEnabledChange(mode == OrientationMode.LANDSCAPE)
    }
) {
    SystemLandscapeModeCard(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        orientationMode = orientationMode,
        onOrientationModeChange = onOrientationModeChange
    )
}
