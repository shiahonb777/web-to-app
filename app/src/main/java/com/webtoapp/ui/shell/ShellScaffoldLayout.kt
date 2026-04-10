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
 * Shell 主布局：Scaffold + TopAppBar + 内容区域
 *
 * 包含：进度条、激活/阻止状态显示、内容路由、所有覆盖层组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.ShellScaffoldLayout(
    config: ShellConfig,
    appType: String,
    hideToolbar: Boolean,
    // 状态
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
    // 下拉刷新
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    // 回调
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onShowActivationDialog: () -> Unit,
    onErrorDismiss: () -> Unit,
    onActivityFinish: () -> Unit,
    // Status bar配置
    statusBarHeightDp: Int
) {
    val context = LocalContext.current

    // 是否显示顶部导航栏：非全屏模式或全屏模式下用户选择显示
    val showToolbar = !hideToolbar || config.webViewConfig.showToolbarInFullscreen

    // 读取键盘调整模式
    val keyboardAdjustMode = remember {
        try {
            KeyboardAdjustMode.valueOf(config.webViewConfig.keyboardAdjustMode)
        } catch (e: Exception) {
            KeyboardAdjustMode.RESIZE
        }
    }

    Scaffold(
        // 根据键盘调整模式设置 contentWindowInsets
        // RESIZE 模式且全屏：保留 IME insets 以便 Compose 响应键盘
        // blockSystemNavigationGesture=true 且全屏：屏蔽系统导航手势（consume gesture area）
        // 全屏且未屏蔽导航手势：使用 ScaffoldDefaults 以保留系统手势区域
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
        // 计算内容的 padding
        // Fullscreen模式 + 显示状态栏时，需要给内容添加状态栏高度的 padding，避免被遮挡
        val density = LocalDensity.current

        val topInsetPx = WindowInsets.statusBars.getTop(density)
        val systemStatusBarHeightDp = if (topInsetPx > 0) {
            with(density) { topInsetPx.toDp() }
        } else {
            24.dp
        }

        // 计算实际需要的状态栏 padding（使用自定义高度或系统默认高度）
        val actualStatusBarPadding = if (statusBarHeightDp > 0) statusBarHeightDp.dp else systemStatusBarHeightDp

        val contentModifier = when {
            hideToolbar && showToolbar -> {
                // Fullscreen模式但显示toolbar：使用 Scaffold 的 padding（toolbar高度）
                Modifier.fillMaxSize().padding(padding)
            }
            hideToolbar && config.webViewConfig.showStatusBarInFullscreen -> {
                // Fullscreen模式但显示状态栏：内容需要在状态栏下方
                // 使用自定义高度或系统默认高度作为顶部 padding
                Modifier.fillMaxSize().padding(top = actualStatusBarPadding)
            }
            hideToolbar -> {
                // 完全全屏模式：内容铺满整个屏幕
                Modifier.fillMaxSize()
            }
            else -> {
                // 非全屏模式：使用 Scaffold 的 padding
                Modifier.fillMaxSize().padding(padding)
            }
        }

        Box(modifier = contentModifier) {
            // 进度条
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

            // 使用 key 包裹：当渲染进程被杀死后 webViewRecreationKey 自增，
            // 强制整个内容子树重建，自动重新加载页面
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

            // Lyrics显示（逻辑已提取到 ShellOverlays.kt）
            ShellLyricsOverlay(config = config, bgmState = bgmState)

            // 强制运行倒计时（逻辑已提取到 ShellOverlays.kt）
            ShellForcedRunOverlay(
                config = config,
                forcedRunActive = forcedRunActive,
                forcedRunRemainingMs = forcedRunRemainingMs
            )

            // 悬浮返回按钮（逻辑已提取到 ShellOverlays.kt）
            ShellFloatingBackButton(
                hideToolbar = hideToolbar,
                showToolbar = showToolbar,
                canGoBack = canGoBack,
                forcedRunActive = forcedRunActive,
                showFloatingBackButton = config.webViewConfig.showFloatingBackButton,
                actualStatusBarPadding = actualStatusBarPadding,
                webViewRef = webViewRef
            )

            // Error提示（逻辑已提取到 ShellOverlays.kt）
            ShellErrorCard(
                errorMessage = errorMessage,
                forcedRunActive = forcedRunActive,
                onDismiss = onErrorDismiss
            )

            // 虚拟导航栏（逻辑已提取到 ShellOverlays.kt）
            ShellVirtualNavBar(
                appType = appType,
                config = config,
                forcedRunActive = forcedRunActive,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                webViewRef = webViewRef
            )

            // Ad拦截切换按钮（逻辑已提取到 ShellOverlays.kt）
            ShellAdBlockToggle(
                config = config,
                forcedRunActive = forcedRunActive,
                webViewRef = webViewRef
            )
        }
    }
}

/**
 * Shell TopAppBar（标题 + 导航按钮）
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
 * 内容区域：根据激活状态和强制运行状态显示不同内容
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
    // 下拉刷新
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onShowActivationDialog: () -> Unit,
    onActivityFinish: () -> Unit
) {
    // Activation检查中，显示加载状态
    if (!isActivationChecked) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    // 未激活提示
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
        // 内容路由（逻辑已提取到 ShellContentRouter.kt）
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
