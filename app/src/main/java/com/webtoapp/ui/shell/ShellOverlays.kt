package com.webtoapp.ui.shell

import android.webkit.WebView
import com.webtoapp.ui.components.PremiumButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.WebToAppApplication
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ForcedRunCountdownOverlay
import com.webtoapp.ui.components.VirtualNavigationBar
import com.webtoapp.core.forcedrun.ForcedRunManager
import kotlinx.coroutines.delay

/**
 * BGM 歌词显示覆盖层
 */
@Composable
fun BoxScope.ShellLyricsOverlay(
    config: ShellConfig,
    bgmState: BgmPlayerState
) {
    if (config.bgmShowLyrics && bgmState.currentLrcData != null && bgmState.currentLrcLineIndex >= 0) {
        val lrcTheme = config.bgmLrcTheme
        val bgColor = try {
            Color(android.graphics.Color.parseColor(lrcTheme?.backgroundColor ?: "#80000000"))
        } catch (e: Exception) {
            Color.Black.copy(alpha = 0.5f)
        }
        val textColor = try {
            Color(android.graphics.Color.parseColor(lrcTheme?.highlightColor ?: "#FFD700"))
        } catch (e: Exception) {
            Color.Yellow
        }

        Box(
            modifier = Modifier
                .align(
                    when (lrcTheme?.position) {
                        "TOP" -> Alignment.TopCenter
                        "CENTER" -> Alignment.Center
                        else -> Alignment.BottomCenter
                    }
                )
                .padding(16.dp)
                .background(bgColor, shape = MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bgmState.currentLrcData!!.lines[bgmState.currentLrcLineIndex].text,
                color = textColor,
                fontSize = (lrcTheme?.fontSize ?: 16f).sp
            )
        }
    }
}

/**
 * 强制运行倒计时覆盖层
 */
@Composable
fun BoxScope.ShellForcedRunOverlay(
    config: ShellConfig,
    forcedRunActive: Boolean,
    forcedRunRemainingMs: Long
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val forcedRunManager = remember { ForcedRunManager.getInstance(context) }

    if (forcedRunActive && config.forcedRunConfig?.showCountdown == true) {
        ForcedRunCountdownOverlay(
            remainingMs = forcedRunRemainingMs,
            allowEmergencyExit = config.forcedRunConfig?.allowEmergencyExit == true,
            emergencyPassword = config.forcedRunConfig?.emergencyPassword,
            onEmergencyExit = {
                forcedRunManager.stopForcedRunMode()
                activity.finish()
            }
        )
    }
}

/**
 * 全屏模式下的悬浮返回按钮（自动淡出）
 */
@Composable
fun BoxScope.ShellFloatingBackButton(
    hideToolbar: Boolean,
    showToolbar: Boolean = false,
    canGoBack: Boolean,
    forcedRunActive: Boolean,
    showFloatingBackButton: Boolean = true,
    actualStatusBarPadding: Dp,
    webViewRef: WebView?
) {
    val context = LocalContext.current

    // 仅在全屏模式下toolbar未显示时才显示悬浮返回按钮（且用户未禁用）
    if (showFloatingBackButton && hideToolbar && !showToolbar && canGoBack && !forcedRunActive) {
        var fabAlpha by remember { mutableFloatStateOf(0.9f) }
        var fadeKey by remember { mutableIntStateOf(0) }

        LaunchedEffect(canGoBack, fadeKey) {
            fabAlpha = 0.9f
            delay(3000L)
            val steps = 20
            val stepDelay = 30L
            for (i in 1..steps) {
                fabAlpha = 0.9f - (0.65f * i / steps)
                delay(stepDelay)
            }
        }

        SmallFloatingActionButton(
            onClick = {
                fadeKey++
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
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = actualStatusBarPadding + 8.dp)
                .graphicsLayer { alpha = fabAlpha },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            ),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.cdBack)
        }
    }
}

/**
 * 错误提示卡片
 */
@Composable
fun BoxScope.ShellErrorCard(
    errorMessage: String?,
    forcedRunActive: Boolean,
    onDismiss: () -> Unit
) {
    errorMessage?.let { error ->
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .padding(bottom = if (forcedRunActive) 56.dp else 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(error, modifier = Modifier.weight(weight = 1f, fill = true))
                TextButton(onClick = onDismiss) {
                    Text(Strings.cdClose)
                }
            }
        }
    }
}

/**
 * 虚拟导航栏（强制运行模式下显示）
 */
@Composable
fun BoxScope.ShellVirtualNavBar(
    appType: String,
    config: ShellConfig,
    forcedRunActive: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    webViewRef: WebView?
) {
    val context = LocalContext.current

    VirtualNavigationBar(
        visible = forcedRunActive,
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        onBack = {
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
        onForward = { webViewRef?.goForward() },
        onRefresh = { webViewRef?.reload() },
        onHome = {
            // 返回主页
            val homeUrl = when {
                appType == "HTML" || appType == "FRONTEND" -> "file:///android_asset/html/${config.htmlConfig.getValidEntryFile()}"
                else -> config.targetUrl
            }
            webViewRef?.loadUrl(homeUrl)
        },
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}

/**
 * 广告拦截切换按钮
 */
@Composable
fun BoxScope.ShellAdBlockToggle(
    config: ShellConfig,
    forcedRunActive: Boolean,
    webViewRef: WebView?
) {
    val context = LocalContext.current
    val adBlocker = WebToAppApplication.adBlock

    if (config.adBlockEnabled && config.webViewConfig.adBlockToggleEnabled) {
        var adBlockCurrentlyEnabled by remember { mutableStateOf(config.adBlockEnabled) }

        FloatingActionButton(
            onClick = {
                adBlockCurrentlyEnabled = !adBlockCurrentlyEnabled
                adBlocker.setEnabled(adBlockCurrentlyEnabled)
                webViewRef?.reload()
                val message = if (adBlockCurrentlyEnabled)
                    Strings.adBlockEnabled
                else
                    Strings.adBlockDisabled
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (forcedRunActive) 72.dp else 16.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            ),
            containerColor = if (adBlockCurrentlyEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = if (adBlockCurrentlyEnabled)
                    Icons.Default.Shield
                else
                    Icons.Outlined.Shield,
                contentDescription = if (adBlockCurrentlyEnabled)
                    Strings.adBlockEnabled
                else
                    Strings.adBlockDisabled,
                tint = if (adBlockCurrentlyEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
