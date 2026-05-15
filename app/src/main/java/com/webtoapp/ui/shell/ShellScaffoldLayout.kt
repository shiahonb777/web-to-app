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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.data.model.WebViewConfig






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.ShellScaffoldLayout(
    config: ShellConfig,
    appType: String,
    hideToolbar: Boolean,
    hideBrowserToolbar: Boolean = false,

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

    webViewRef: WebView?,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    deepLinkUrl: String?,
    bgmState: BgmPlayerState,

    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,

    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onShowActivationDialog: () -> Unit,
    onErrorDismiss: () -> Unit,
    onActivityFinish: () -> Unit,

    statusBarHeightDp: Int
) {
    val context = LocalContext.current


    val showToolbar = (!hideToolbar || config.webViewConfig.showToolbarInFullscreen) && !hideBrowserToolbar

    Scaffold(

        // 键盘适配由 WindowHelper.applyKeyboardMode 统一处理（通过 contentView padding），
        // 此处不再重复设置 WindowInsets.ime，避免与 WindowHelper 以及网页自身的键盘适配代码
        // 产生叠加，导致输入框被推到键盘两倍高度的位置。
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
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


        val density = LocalDensity.current

        val topInsetPx = WindowInsets.statusBars.getTop(density)
        val systemStatusBarHeightDp = if (topInsetPx > 0) {
            with(density) { topInsetPx.toDp() }
        } else {
            24.dp
        }


        val actualStatusBarPadding = if (statusBarHeightDp > 0) statusBarHeightDp.dp else systemStatusBarHeightDp

        val contentModifier = when {
            hideToolbar && showToolbar -> {

                Modifier.fillMaxSize().padding(padding)
            }
            hideToolbar && config.webViewConfig.showStatusBarInFullscreen -> {


                Modifier.fillMaxSize().padding(top = actualStatusBarPadding)
            }
            hideToolbar -> {

                Modifier.fillMaxSize()
            }
            else -> {

                Modifier.fillMaxSize().padding(padding)
            }
        }

        Box(modifier = contentModifier) {

            // Thin Safari-style top progress. Sits flush at the top of the
            // content area, grows with the page load, then fades out once
            // loading finishes rather than abruptly disappearing.
            WebViewLoadingBar(
                visible = isLoading,
                progress = loadProgress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )



            ShellContentArea(
                config = config,
                appType = appType,
                isActivationChecked = isActivationChecked,
                isActivated = isActivated,
                forcedRunBlocked = forcedRunBlocked,
                forcedRunBlockedMessage = forcedRunBlockedMessage,
                webViewRecreationKey = webViewRecreationKey,
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


            ShellLyricsOverlay(config = config, bgmState = bgmState)


            ShellForcedRunOverlay(
                config = config,
                forcedRunActive = forcedRunActive,
                forcedRunRemainingMs = forcedRunRemainingMs
            )


            ShellErrorCard(
                errorMessage = errorMessage,
                forcedRunActive = forcedRunActive,
                onDismiss = onErrorDismiss
            )


            ShellVirtualNavBar(
                appType = appType,
                config = config,
                forcedRunActive = forcedRunActive,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                webViewRef = webViewRef
            )


            ShellAdBlockToggle(
                config = config,
                forcedRunActive = forcedRunActive,
                webViewRef = webViewRef
            )
        }
    }
}




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
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (currentUrl.isNotEmpty()) {
                    Text(
                        text = currentUrl.shortenForShellToolbar(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            com.webtoapp.ui.design.WtaIconButton(
                onClick = {
                    (context as? AppCompatActivity)?.let { activity ->
                        ShellWebViewNavigation.goBackOrFinish(activity, webViewRef)
                    }
                },
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                enabled = canGoBack
            )
            com.webtoapp.ui.design.WtaIconButton(
                onClick = { webViewRef?.goForward() },
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Forward",
                enabled = canGoForward
            )
            com.webtoapp.ui.design.WtaIconButton(
                onClick = { webViewRef?.reload() },
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Hide the scheme + common tracking noise from the toolbar url label and
 * truncate each segment softly so the URL stays readable.
 */
private fun String.shortenForShellToolbar(): String {
    val withoutScheme = when {
        startsWith("https://") -> substring(8)
        startsWith("http://") -> substring(7)
        else -> this
    }
    // Strip querystring for visual clarity; most URLs still identify well
    // from the path alone.
    return withoutScheme.substringBefore('?').substringBefore('#')
}




@Composable
private fun ShellContentArea(
    config: ShellConfig,
    appType: String,
    isActivationChecked: Boolean,
    isActivated: Boolean,
    forcedRunBlocked: Boolean,
    forcedRunBlockedMessage: String,
    webViewRecreationKey: Int,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    deepLinkUrl: String?,

    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewRefUpdated: (WebView) -> Unit,
    onShowActivationDialog: () -> Unit,
    onActivityFinish: () -> Unit
) {

    if (!isActivationChecked) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

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
                Text(Strings.pleaseActivateApp)
                Spacer(modifier = Modifier.height(16.dp))
                PremiumButton(onClick = onShowActivationDialog) {
                    Text(Strings.enterActivationCode)
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

        ShellContentRouter(
            appType = appType,
            config = config,
            webViewRecreationKey = webViewRecreationKey,
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


/**
 * Safari-style thin loading indicator.
 *
 * Behaviour:
 *  - Animates from 0 to the latest reported `progress` with the settle spring
 *    so the fill always feels physical rather than snapping.
 *  - When visibility flips off, the bar first finishes to full, then fades
 *    over ~240ms. This avoids the "bar disappears halfway" feel of the raw
 *    LinearProgressIndicator that was previously used.
 *  - Height is 2dp, no trailing track (we draw only the fill), so it reads as
 *    a restrained affordance rather than a loud control.
 */
@Composable
private fun WebViewLoadingBar(
    visible: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) progress.coerceIn(0f, 1f) else 1f,
        animationSpec = com.webtoapp.ui.design.WtaMotion.settleSpring(),
        label = "webviewProgress"
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = com.webtoapp.ui.design.WtaMotion.exitTween(
            durationMillis = com.webtoapp.ui.design.WtaMotion.DurationMedium
        ),
        label = "webviewProgressAlpha"
    )
    if (alpha <= 0f) return

    val primary = MaterialTheme.colorScheme.primary
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .height(2.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        val fillWidth = size.width * animatedProgress
        drawRect(
            color = primary,
            size = Size(fillWidth, size.height)
        )
    }
}
