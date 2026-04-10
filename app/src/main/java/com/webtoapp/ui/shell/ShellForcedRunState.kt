package com.webtoapp.ui.shell
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.Context
import androidx.compose.runtime.*
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.forcedrun.ForcedRunMode
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.delay

/**
 * 强制运行状态持有类
 */
class ForcedRunState(
    val forcedRunManager: ForcedRunManager,
    forcedRunActiveState: State<Boolean>,
    forcedRunRemainingMsState: State<Long>,
    blockedState: MutableState<Boolean>,
    blockedMessageState: MutableState<String>,
    showPermissionDialogState: MutableState<Boolean>,
    permissionCheckedState: MutableState<Boolean>
) {
    val forcedRunActive by forcedRunActiveState
    val forcedRunRemainingMs by forcedRunRemainingMsState
    var forcedRunBlocked by blockedState
    var forcedRunBlockedMessage by blockedMessageState
    var showForcedRunPermissionDialog by showPermissionDialogState
    var forcedRunPermissionChecked by permissionCheckedState
}

/**
 * 创建并记住强制运行状态
 */
@Composable
fun rememberForcedRunState(context: Context): ForcedRunState {
    val forcedRunManager = remember { ForcedRunManager.getInstance(context) }
    val forcedRunActiveState = forcedRunManager.isInForcedRunMode.collectAsStateWithLifecycle()
    val forcedRunRemainingMsState = forcedRunManager.remainingTimeMs.collectAsStateWithLifecycle()
    val blockedState = remember { mutableStateOf(false) }
    val blockedMessageState = remember { mutableStateOf("") }
    val showPermissionDialogState = remember { mutableStateOf(false) }
    val permissionCheckedState = remember { mutableStateOf(false) }

    return remember {
        ForcedRunState(
            forcedRunManager = forcedRunManager,
            forcedRunActiveState = forcedRunActiveState,
            forcedRunRemainingMsState = forcedRunRemainingMsState,
            blockedState = blockedState,
            blockedMessageState = blockedMessageState,
            showPermissionDialogState = showPermissionDialogState,
            permissionCheckedState = permissionCheckedState
        )
    }
}

/**
 * 格式化持续时间（毫秒 -> HH:MM:SS 或 MM:SS）
 */
fun formatDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

/**
 * 更新强制运行状态
 */
fun updateForcedRunState(
    state: ForcedRunState,
    config: ForcedRunConfig?,
    isActivated: Boolean
) {
    if (config?.enabled != true || !isActivated) {
        state.forcedRunBlocked = false
        if (state.forcedRunActive) {
            state.forcedRunManager.stopForcedRunMode()
        }
        return
    }

    if (!state.forcedRunManager.canEnterApp(config)) {
        val waitMs = state.forcedRunManager.getTimeUntilNextAccess(config)
        val waitText = if (waitMs > 0) formatDuration(waitMs) else ""
        state.forcedRunBlockedMessage = if (waitText.isNotEmpty()) {
            "当前不在允许进入时间，请稍后再试（剩余 $waitText）。"
        } else {
            "当前不在允许进入时间，请稍后再试。"
        }
        state.forcedRunBlocked = true
        if (state.forcedRunActive) {
            state.forcedRunManager.stopForcedRunMode()
        }
        return
    }

    state.forcedRunBlocked = false
    val shouldStart = when (config.mode) {
        ForcedRunMode.COUNTDOWN -> true
        else -> state.forcedRunManager.isInForcedRunPeriod(config)
    }

    if (shouldStart && !state.forcedRunActive) {
        state.forcedRunManager.startForcedRunMode(config, -1L)
    } else if (!shouldStart && state.forcedRunActive) {
        state.forcedRunManager.stopForcedRunMode()
    }
}

/**
 * 强制运行副作用管理（LaunchedEffect + DisposableEffect）
 */
@Composable
fun ForcedRunEffects(
    state: ForcedRunState,
    config: ForcedRunConfig?,
    isActivated: Boolean,
    context: Context,
    onForcedRunStateChanged: (Boolean, ForcedRunConfig?) -> Unit
) {
    // 检查强制运行权限
    LaunchedEffect(Unit) {
        if (config?.enabled == true && !state.forcedRunPermissionChecked) {
            val protectionLevel = config.protectionLevel
            val permissionStatus = ForcedRunManager.checkProtectionPermissions(context, protectionLevel)

            AppLogger.d("ShellActivity", "强制运行权限检查: level=$protectionLevel, " +
                "hasAccessibility=${permissionStatus.hasAccessibility}, " +
                "hasUsageStats=${permissionStatus.hasUsageStats}, " +
                "isFullyGranted=${permissionStatus.isFullyGranted}")

            if (!permissionStatus.isFullyGranted) {
                state.showForcedRunPermissionDialog = true
            }
            state.forcedRunPermissionChecked = true
        }
    }

    // 定期更新强制运行状态
    LaunchedEffect(isActivated, config) {
        while (true) {
            updateForcedRunState(state, config, isActivated)
            delay(60_000L)
        }
    }

    // 通知强制运行状态变化
    LaunchedEffect(state.forcedRunActive, config) {
        onForcedRunStateChanged(state.forcedRunActive, config)
    }

    // 清理
    DisposableEffect(Unit) {
        onDispose {
            if (state.forcedRunActive) {
                state.forcedRunManager.stopForcedRunMode()
            }
        }
    }
}
