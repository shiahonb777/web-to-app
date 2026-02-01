package com.webtoapp.ui.components.aimodule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.screens.aimodule.ErrorInfo

/**
 * 错误恢复操作类型
 *
 * Requirements: 8.4
 */
sealed class RecoveryAction(
    val icon: ImageVector
) {
    abstract val title: String
    abstract val description: String

    object Retry : RecoveryAction(Icons.Default.Refresh) {
        override val title: String get() = Strings.retryAction
        override val description: String get() = Strings.retryActionHint
    }

    object RetryWithDifferentModel : RecoveryAction(Icons.Default.SwapHoriz) {
        override val title: String get() = Strings.retryWithDifferentModel
        override val description: String get() = Strings.retryWithDifferentModelHint
    }

    object ShowRawResponse : RecoveryAction(Icons.Default.Code) {
        override val title: String get() = Strings.showRawResponse
        override val description: String get() = Strings.showRawResponseHint
    }

    object GoToSettings : RecoveryAction(Icons.Default.Settings) {
        override val title: String get() = Strings.goToSettings
        override val description: String get() = Strings.goToSettingsHint
    }

    object ManualEdit : RecoveryAction(Icons.Default.Edit) {
        override val title: String get() = Strings.manualEdit
        override val description: String get() = Strings.manualEditHint
    }

    object Dismiss : RecoveryAction(Icons.Default.Close) {
        override val title: String get() = Strings.dismissAction
        override val description: String get() = Strings.dismissActionHint
    }
}


/**
 * 根据错误信息获取可用的恢复操作
 *
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
fun getRecoveryActions(error: ErrorInfo): List<RecoveryAction> {
    return when {
        // 认证错误 - 引导用户检查 API Key
        error.code == "401" || error.code == "403" -> listOf(
            RecoveryAction.GoToSettings,
            RecoveryAction.RetryWithDifferentModel
        )

        // 限流错误 - 可以重试
        error.code == "429" -> listOf(
            RecoveryAction.Retry,
            RecoveryAction.RetryWithDifferentModel
        )

        // 网络错误 - 可以重试
        error.code == "NETWORK_ERROR" || error.message.contains("网络", ignoreCase = true) -> listOf(
            RecoveryAction.Retry
        )

        // 解析错误 - 显示原始响应
        error.rawResponse != null -> listOf(
            RecoveryAction.ShowRawResponse,
            RecoveryAction.Retry,
            RecoveryAction.ManualEdit
        )

        // 可恢复错误 - 提供重试选项
        error.recoverable -> listOf(
            RecoveryAction.Retry,
            RecoveryAction.RetryWithDifferentModel
        )

        // 不可恢复错误 - 只能手动处理
        else -> listOf(
            RecoveryAction.ManualEdit,
            RecoveryAction.Dismiss
        )
    }
}

/**
 * 格式化错误消息
 *
 * 将错误信息格式化为用户友好的消息，包含错误码（如果有）
 *
 * Requirements: 8.1
 */
fun formatErrorMessage(error: ErrorInfo): String {
    val codePrefix = error.code?.let { "[$it] " } ?: ""
    return "$codePrefix${error.message}"
}

/**
 * 获取错误类型描述
 */
private fun getErrorTypeDescription(error: ErrorInfo): String {
    return when {
        error.code == "401" || error.code == "403" -> Strings.errorTypeAuth
        error.code == "429" -> Strings.errorTypeRateLimit
        error.code == "500" || error.code == "502" || error.code == "503" -> Strings.errorTypeServer
        error.code == "NETWORK_ERROR" -> Strings.errorTypeNetwork
        error.code == "PARSE_ERROR" -> Strings.errorTypeParse
        error.code == "TIMEOUT" -> Strings.errorTypeTimeout
        error.rawResponse != null -> Strings.errorTypeResponseParse
        !error.recoverable -> Strings.errorTypeFatal
        else -> Strings.errorTypeOperationFailed
    }
}

/**
 * 获取错误图标
 */
private fun getErrorIcon(error: ErrorInfo): ImageVector {
    return when {
        error.code == "401" || error.code == "403" -> Icons.Default.Lock
        error.code == "429" -> Icons.Default.Timer
        error.code == "NETWORK_ERROR" -> Icons.Default.WifiOff
        error.code == "TIMEOUT" -> Icons.Default.HourglassEmpty
        else -> Icons.Default.Error
    }
}

/**
 * 错误卡片组件
 *
 * 显示错误信息、错误码和恢复选项
 *
 * @param error 错误信息
 * @param onRetry 重试回调
 * @param onRetryWithDifferentModel 换模型重试回调
 * @param onShowRawResponse 显示原始响应回调
 * @param onGoToSettings 前往设置回调
 * @param onManualEdit 手动编辑回调
 * @param onDismiss 关闭回调
 * @param modifier Modifier
 *
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
@Composable
fun ErrorCard(
    error: ErrorInfo,
    onRetry: () -> Unit = {},
    onRetryWithDifferentModel: () -> Unit = {},
    onShowRawResponse: () -> Unit = {},
    onGoToSettings: () -> Unit = {},
    onManualEdit: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showRawResponse by remember { mutableStateOf(false) }
    val recoveryActions = remember(error) { getRecoveryActions(error) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 错误头部
            ErrorHeader(error = error)

            // 错误消息
            ErrorMessage(error = error)

            // 原始响应（可展开）
            if (error.rawResponse != null) {
                RawResponseSection(
                    rawResponse = error.rawResponse,
                    isExpanded = showRawResponse,
                    onToggle = { showRawResponse = !showRawResponse }
                )
            }

            // 恢复操作按钮
            if (recoveryActions.isNotEmpty()) {
                Divider(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )

                RecoveryActionsRow(
                    actions = recoveryActions,
                    onActionClick = { action ->
                        when (action) {
                            RecoveryAction.Retry -> onRetry()
                            RecoveryAction.RetryWithDifferentModel -> onRetryWithDifferentModel()
                            RecoveryAction.ShowRawResponse -> showRawResponse = !showRawResponse
                            RecoveryAction.GoToSettings -> onGoToSettings()
                            RecoveryAction.ManualEdit -> onManualEdit()
                            RecoveryAction.Dismiss -> onDismiss()
                        }
                    }
                )
            }
        }
    }
}


/**
 * 错误头部
 */
@Composable
private fun ErrorHeader(error: ErrorInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 错误图标
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getErrorIcon(error),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // 错误类型
            Text(
                text = getErrorTypeDescription(error),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )

            // 错误码（如果有）
            if (error.code != null) {
                Text(
                    text = Strings.errorCodeFormat.format(error.code),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }

        // 可恢复标识
        if (error.recoverable) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = Strings.recoverable,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 错误消息
 */
@Composable
private fun ErrorMessage(error: ErrorInfo) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
            lineHeight = 20.sp
        )
    }
}

/**
 * 原始响应区域
 */
@Composable
private fun RawResponseSection(
    rawResponse: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        // 展开/折叠按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) Strings.collapse else Strings.expand,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = if (isExpanded) Strings.hideRawResponse else Strings.showRawResponse,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // 原始响应内容
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = rawResponse.take(1000) + if (rawResponse.length > 1000) "\n..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * 恢复操作按钮行
 */
@Composable
private fun RecoveryActionsRow(
    actions: List<RecoveryAction>,
    onActionClick: (RecoveryAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            RecoveryActionButton(
                action = action,
                onClick = { onActionClick(action) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 恢复操作按钮
 */
@Composable
private fun RecoveryActionButton(
    action: RecoveryAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPrimary = action == RecoveryAction.Retry

    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}


/**
 * 简化版错误卡片
 * 用于在消息列表中显示错误的简洁版本
 *
 * @param error 错误信息
 * @param onRetry 重试回调
 * @param modifier Modifier
 */
@Composable
fun CompactErrorCard(
    error: ErrorInfo,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatErrorMessage(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (error.recoverable) {
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = Strings.btnRetry,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 网络离线指示器
 *
 * Requirements: 8.2
 */
@Composable
fun OfflineIndicator(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.offlineTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Strings.offlineHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(Strings.retry)
            }
        }
    }
}

/**
 * API 限流提示
 *
 * Requirements: 8.2
 */
@Composable
fun RateLimitIndicator(
    retryAfterSeconds: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember(retryAfterSeconds) { mutableStateOf(retryAfterSeconds) }

    // 倒计时
    LaunchedEffect(retryAfterSeconds) {
        while (remainingSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            remainingSeconds--
        }
        // 倒计时结束自动重试
        if (remainingSeconds == 0) {
            onRetry()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.rateLimitTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = if (remainingSeconds > 0) {
                        Strings.retryAfterSeconds.format(remainingSeconds)
                    } else {
                        Strings.retrying
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }

            if (remainingSeconds > 0) {
                // 进度指示器
                CircularProgressIndicator(
                    progress = remainingSeconds.toFloat() / retryAfterSeconds,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
