package com.webtoapp.ui.screens

import androidx.compose.runtime.Composable
import com.webtoapp.core.errorpage.ErrorPageConfig
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.KeyboardAdjustMode
import com.webtoapp.data.model.LongPressMenuStyle
import com.webtoapp.data.model.OrientationMode
import com.webtoapp.data.model.ScreenAwakeMode
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.screens.create.webview.cards.display.FullscreenModeCard as DisplayFullscreenModeCard
import com.webtoapp.ui.screens.create.webview.cards.display.LandscapeModeCard as DisplayLandscapeModeCard
import com.webtoapp.ui.screens.create.webview.cards.display.StatusBarStyleCard as DisplayStatusBarStyleCard
import com.webtoapp.ui.screens.create.webview.cards.display.UserAgentCard as DisplayUserAgentCard
import com.webtoapp.ui.screens.create.webview.cards.interaction.AdBlockCard as InteractionAdBlockCard
import com.webtoapp.ui.screens.create.webview.cards.interaction.ErrorPageConfigCard as InteractionErrorPageConfigCard
import com.webtoapp.ui.screens.create.webview.cards.interaction.LongPressMenuCard as InteractionLongPressMenuCard
import com.webtoapp.ui.screens.create.webview.cards.system.KeepScreenOnCard as SystemKeepScreenOnCard
import com.webtoapp.ui.screens.create.webview.cards.system.KeyboardAdjustModeCard as SystemKeyboardAdjustModeCard
import com.webtoapp.ui.screens.create.webview.cards.system.WebViewConfigCard as SystemWebViewConfigCard
import com.webtoapp.ui.viewmodel.EditState

@Composable
fun LongPressMenuCard(
    style: LongPressMenuStyle,
    onStyleChange: (LongPressMenuStyle) -> Unit
) {
    InteractionLongPressMenuCard(style = style, onStyleChange = onStyleChange)
}

@Composable
fun AdBlockCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onRulesChange: (List<String>) -> Unit,
    onToggleEnabledChange: (Boolean) -> Unit = {}
) {
    InteractionAdBlockCard(
        editState = editState,
        onEnabledChange = onEnabledChange,
        onRulesChange = onRulesChange,
        onToggleEnabledChange = onToggleEnabledChange
    )
}

@Composable
fun WebViewConfigCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit,
    apkExportConfig: ApkExportConfig = ApkExportConfig(),
    onApkExportConfigChange: (ApkExportConfig) -> Unit = {}
) {
    SystemWebViewConfigCard(
        config = config,
        onConfigChange = onConfigChange,
        apkExportConfig = apkExportConfig,
        onApkExportConfigChange = onApkExportConfigChange
    )
}

@Composable
fun UserAgentCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    DisplayUserAgentCard(config = config, onConfigChange = onConfigChange)
}

@Composable
fun StatusBarStyleCard(
    webViewConfig: WebViewConfig,
    onWebViewConfigChange: (WebViewConfig) -> Unit
) {
    DisplayStatusBarStyleCard(
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
    DisplayFullscreenModeCard(
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
    DisplayLandscapeModeCard(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        orientationMode = orientationMode,
        onOrientationModeChange = onOrientationModeChange
    )
}

@Composable
fun KeepScreenOnCard(
    screenAwakeMode: ScreenAwakeMode,
    onScreenAwakeModeChange: (ScreenAwakeMode) -> Unit,
    screenAwakeTimeoutMinutes: Int,
    onScreenAwakeTimeoutChange: (Int) -> Unit,
    screenBrightness: Int,
    onScreenBrightnessChange: (Int) -> Unit
) {
    SystemKeepScreenOnCard(
        screenAwakeMode = screenAwakeMode,
        onScreenAwakeModeChange = onScreenAwakeModeChange,
        screenAwakeTimeoutMinutes = screenAwakeTimeoutMinutes,
        onScreenAwakeTimeoutChange = onScreenAwakeTimeoutChange,
        screenBrightness = screenBrightness,
        onScreenBrightnessChange = onScreenBrightnessChange
    )
}

@Composable
fun KeyboardAdjustModeCard(
    mode: KeyboardAdjustMode,
    onModeChange: (KeyboardAdjustMode) -> Unit
) {
    SystemKeyboardAdjustModeCard(mode = mode, onModeChange = onModeChange)
}

@Composable
fun ErrorPageConfigCard(
    config: ErrorPageConfig,
    onConfigChange: (ErrorPageConfig) -> Unit
) {
    InteractionErrorPageConfigCard(config = config, onConfigChange = onConfigChange)
}
