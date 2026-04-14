package com.webtoapp.ui.shell

import android.webkit.WebView
import com.webtoapp.ui.components.PremiumButton
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.data.model.KeyboardAdjustMode
import com.webtoapp.data.model.WebViewConfig

/**
 * Shell: Scaffold + TopAppBar + contentarea
 *
 * , / statedisplay, content,
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.ShellScaffoldLayout(
    config: ShellConfig,
    appType: String,
    hideToolbar: Boolean,
    hideBrowserToolbar: Boolean = false,
    // state
    isLoading: Boolean,
    loadProgress: Int,
    pageTitle: String,
    currentUrl: String,
    errorMessage: String?,
    isActivationChecked: Boolean,
    isActivated: Boolean,
    forcedRunActive: Boolean,
    forcedRunBlocked: Boolean,
    forcedRunBlockedMessage: String,
    forcedRunRemainingMs: Long,
    canGoBack: Boolean,
    canGoForward: Boolean,
    webViewRecreationKey: Int,
    // WebView
    webViewRef: WebView?,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    deepLinkUrl: String?,
    bgmState: BgmPlayerState,
    // pull- to- refresh
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    // Note
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onShowActivationDialog: () -> Unit,
    onErrorDismiss: () -> Unit,
    onActivityFinish: () -> Unit,
    // Status barconfig
    statusBarHeightDp: Int
) {
    val context = LocalContext.current

    // displaytop: modeor mode userselectdisplay
    val showToolbar = when {
        hideBrowserToolbar -> false
        hideToolbar -> config.webViewConfig.showToolbarInFullscreen
        else -> true
    }

    // keyboard mode
    val keyboardAdjustMode = remember {
        try {
            KeyboardAdjustMode.valueOf(config.webViewConfig.keyboardAdjustMode)
        } catch (e: Exception) {
            KeyboardAdjustMode.RESIZE
        }
    }

    Scaffold(
        // keyboard modesettings contentWindowInsets
        // RESIZE mode: IME insets Compose keyboard
        // blockSystemNavigationGesture=true: system( consume gesture area)
        // ScaffoldDefaults system area
        contentWindowInsets = when {
            keyboardAdjustMode == KeyboardAdjustMode.RESIZE && hideToolbar -> WindowInsets.ime
            hideToolbar && webViewConfig.blockSystemNavigationGesture -> WindowInsets(0)
            else -> ScaffoldDefaults.contentWindowInsets
        },
        modifier = Modifier,
        topBar = {
            if (showToolbar) {
                ShellTopAppBar(
                    pageTitle = pageTitle,
                    appName = config.appName,
                    currentUrl = currentUrl,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    webViewRef = webViewRef
                )
            }
        }
    ) { padding ->
        // content padding
        // Fullscreenmode + displaystatus bar, content status bar padding,
        val density = LocalDensity.current

        val topInsetPx = WindowInsets.statusBars.getTop(density)
        val systemStatusBarHeightDp = if (topInsetPx > 0) {
            with(density) { topInsetPx.toDp() }
        } else {
            24.dp
        }

        // status bar padding( orsystemdefault)
        val actualStatusBarPadding = if (statusBarHeightDp > 0) statusBarHeightDp.dp else systemStatusBarHeightDp

        val contentModifier = when {
            hideToolbar && showToolbar -> {
                // Fullscreenmode displaytoolbar: Scaffold padding( toolbar)
                Modifier.fillMaxSize().padding(padding)
            }
            hideToolbar && config.webViewConfig.showStatusBarInFullscreen -> {
                // Fullscreenmode displaystatus bar: content status bar
                // orsystemdefault top padding
                Modifier.fillMaxSize().padding(top = actualStatusBarPadding)
            }
            hideToolbar -> {
                // mode: content
                Modifier.fillMaxSize()
            }
            else -> {
                // mode: Scaffold padding
                Modifier.fillMaxSize().padding(padding)
            }
        }

        Box(modifier = contentModifier) {
            // Note
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // key: when webViewRecreationKey,
            // content, load
            key(webViewRecreationKey) {
                ShellContentArea(
                    config = config,
                    appType = appType,
                    isActivationChecked = isActivationChecked,
                    isActivated = isActivated,
                    forcedRunBlocked = forcedRunBlocked,
                    forcedRunBlockedMessage = forcedRunBlockedMessage,
                    webViewConfig = webViewConfig,
                    webViewCallbacks = webViewCallbacks,
                    webViewManager = webViewManager,
                    deepLinkUrl = deepLinkUrl,
                    swipeRefreshEnabled = swipeRefreshEnabled,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onWebViewCreated = onWebViewCreated,
                    onWebViewRefUpdated = onWebViewRefUpdated,
                    onShowActivationDialog = onShowActivationDialog,
                    onActivityFinish = onActivityFinish
                )
            }

            // Lyricsdisplay( ShellOverlays. kt)
            ShellLyricsOverlay(config = config, bgmState = bgmState)

            // force- run( ShellOverlays. kt)
            ShellForcedRunOverlay(
                config = config,
                forcedRunActive = forcedRunActive,
                forcedRunRemainingMs = forcedRunRemainingMs
            )

            // backbutton( ShellOverlays. kt)
            ShellFloatingBackButton(
                hideToolbar = hideToolbar || hideBrowserToolbar,
                showToolbar = showToolbar,
                canGoBack = canGoBack,
                forcedRunActive = forcedRunActive,
                showFloatingBackButton = config.webViewConfig.showFloatingBackButton,
                actualStatusBarPadding = actualStatusBarPadding,
                webViewRef = webViewRef
            )

            // Errorhint( ShellOverlays. kt)
            ShellErrorCard(
                errorMessage = errorMessage,
                forcedRunActive = forcedRunActive,
                onDismiss = onErrorDismiss
            )

            // ( ShellOverlays. kt)
            ShellVirtualNavBar(
                appType = appType,
                config = config,
                forcedRunActive = forcedRunActive,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                webViewRef = webViewRef
            )

            // Adinterceptswitchbutton( ShellOverlays. kt)
            ShellAdBlockToggle(
                config = config,
                forcedRunActive = forcedRunActive,
                webViewRef = webViewRef
            )
        }
    }
}

/**
 * Shell TopAppBar( + button)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopAppBar(
    pageTitle: String,
    appName: String,
    currentUrl: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    webViewRef: WebView?
) {
    val context = LocalContext.current

    TopAppBar(
        title = {
            Column {
                Text(
                    text = pageTitle.ifEmpty { appName },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (currentUrl.isNotEmpty()) {
                    Text(
                        text = currentUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    webViewRef?.let { wv ->
                        val list = wv.copyBackForwardList()
                        val prev = list.getItemAtIndex(list.currentIndex - 1)?.url
                        if (prev == "about:blank") {
                            (context as? AppCompatActivity)?.finish()
                        } else {
                            wv.goBack()
                        }
                    }
                },
                enabled = canGoBack
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            IconButton(
                onClick = { webViewRef?.goForward() },
                enabled = canGoForward
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Forward")
            }
            IconButton(onClick = { webViewRef?.reload() }) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }
    )
}

/**
 * contentarea: state force- runstatedisplay content
 */
@Composable
private fun ShellContentArea(
    config: ShellConfig,
    appType: String,
    isActivationChecked: Boolean,
    isActivated: Boolean,
    forcedRunBlocked: Boolean,
    forcedRunBlockedMessage: String,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    deepLinkUrl: String?,
    // pull- to- refresh
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onShowActivationDialog: () -> Unit,
    onActivityFinish: () -> Unit
) {
    // Activationcheck, displayloadstate
    if (!isActivationChecked) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    // hint
    else if (!isActivated && config.activationEnabled) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("请先激活应用")
                Spacer(modifier = Modifier.height(16.dp))
                PremiumButton(onClick = onShowActivationDialog) {
                    Text("输入激活码")
                }
            }
        }
    } else if (forcedRunBlocked) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(forcedRunBlockedMessage)
            }
        }
    } else {
        // content( ShellContentRouter. kt)
        ShellContentRouter(
            appType = appType,
            config = config,
            webViewConfig = webViewConfig,
            webViewCallbacks = webViewCallbacks,
            webViewManager = webViewManager,
            deepLinkUrl = deepLinkUrl,
            swipeRefreshEnabled = swipeRefreshEnabled,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onWebViewCreated = onWebViewCreated,
            onWebViewRefUpdated = onWebViewRefUpdated,
            onActivityFinish = onActivityFinish
        )
    }
}
