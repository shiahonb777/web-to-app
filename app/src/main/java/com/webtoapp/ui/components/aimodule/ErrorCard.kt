package com.webtoapp.ui.components.aimodule

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.screens.aimodule.ErrorInfo
import androidx.compose.ui.graphics.Color

/**
 * error type
 * 
 * Requirements: 8.4
 */
sealed class RecoveryAction(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    object Retry : RecoveryAction(
        title = "Retry",
        icon = Icons.Default.Refresh,
        description = "重新尝试上一次操作"
    )
    
    object RetryWithDifferentModel : RecoveryAction(
        title = "换个模型重试",
        icon = Icons.Default.SwapHoriz,
        description = "使用其他 AI 模型重试"
    )
    
    object ShowRawResponse : RecoveryAction(
        title = "查看原始响应",
        icon = Icons.Default.Code,
        description = "显示 AI 返回的原始内容"
    )
    
    object GoToSettings : RecoveryAction(
        title = "前往设置",
        icon = Icons.Default.Settings,
        description = "检查 API Key 配置"
    )
    
    object ManualEdit : RecoveryAction(
        title = "手动编辑",
        icon = Icons.Default.Edit,
        description = "手动修改代码"
    )
    
    object Dismiss : RecoveryAction(
        title = "Close",
        icon = Icons.Default.Close,
        description = "关闭错误提示"
    )
}


/**
 * error
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
fun getRecoveryActions(error: ErrorInfo): List<RecoveryAction> {
    return when {
        // error- usercheck API Key
        error.code == "401" || error.code == "403" -> listOf(
            RecoveryAction.GoToSettings,
            RecoveryAction.RetryWithDifferentModel
        )
        
        // error
        error.code == "429" -> listOf(
            RecoveryAction.Retry,
            RecoveryAction.RetryWithDifferentModel
        )
        
        // Networkerror
        error.code == "NETWORK_ERROR" || error.message.contains("网络", ignoreCase = true) -> listOf(
            RecoveryAction.Retry
        )
        
        // Parseerror- display
        error.rawResponse != null -> listOf(
            RecoveryAction.ShowRawResponse,
            RecoveryAction.Retry,
            RecoveryAction.ManualEdit
        )
        
        // error
        error.recoverable -> listOf(
            RecoveryAction.Retry,
            RecoveryAction.RetryWithDifferentModel
        )
        
        // error- handle
        else -> listOf(
            RecoveryAction.ManualEdit,
            RecoveryAction.Dismiss
        )
    }
}

/**
 * errormessage
 * 
 * maperror user message, error( if)
 * 
 * Requirements: 8.1
 */
fun formatErrorMessage(error: ErrorInfo): String {
    val codePrefix = error.code?.let { "[$it] " } ?: ""
    return "$codePrefix${error.message}"
}

/**
 * errortype
 */
private fun getErrorTypeDescription(error: ErrorInfo): String {
    return when {
        error.code == "401" || error.code == "403" -> "认证错误"
        error.code == "429" -> "请求限流"
        error.code == "500" || error.code == "502" || error.code == "503" -> "服务器错误"
        error.code == "NETWORK_ERROR" -> "网络错误"
        error.code == "PARSE_ERROR" -> "解析错误"
        error.code == "TIMEOUT" -> "请求超时"
        error.rawResponse != null -> "响应解析失败"
        !error.recoverable -> "严重错误"
        else -> "Operation failed"
    }
}

/**
 * erroricon
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
 * errorcard
 * 
 * displayerror, error
 * 
 * @param error error
 * @param onRetry
 * @param onRetryWithDifferentModel
 * @param onShowRawResponse display
 * @param onGoToSettings settings
 * @param onManualEdit edit
 * @param onDismiss close
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
            // Errorheader
            ErrorHeader(error = error)
            
            // Errormessage
            ErrorMessage(error = error)
            
            // ( expand)
            if (error.rawResponse != null) {
                RawResponseSection(
                    rawResponse = error.rawResponse,
                    isExpanded = showRawResponse,
                    onToggle = { showRawResponse = !showRawResponse }
                )
            }
            
            // button
            if (recoveryActions.isNotEmpty()) {
                HorizontalDivider(
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
 * errorheader
 */
@Composable
private fun ErrorHeader(error: ErrorInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Erroricon
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
        
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            // Errortype
            Text(
                text = getErrorTypeDescription(error),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            
            // Error( if)
            if (error.code != null) {
                Text(
                    text = "错误码: ${error.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Note
        if (error.recoverable) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "可恢复",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * errormessage
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
 * area
 */
@Composable
private fun RawResponseSection(
    rawResponse: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column {
        // Expand/collapsebutton
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
                contentDescription = if (isExpanded) AppStringsProvider.current().cdCollapse else AppStringsProvider.current().cdExpand,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = if (isExpanded) AppStringsProvider.current().hideRawResponse else AppStringsProvider.current().viewRawResponse,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        // content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
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
 * button
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
                modifier = Modifier.weight(weight = 1f, fill = true)
            )
        }
    }
}

/**
 * button
 */
@Composable
private fun RecoveryActionButton(
    action: RecoveryAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPrimary = action == RecoveryAction.Retry
    
    if (isPrimary) {
        PremiumButton(
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
        PremiumOutlinedButton(
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
 * errorcard
 * formessagelist displayerror version
 * 
 * @param error error
 * @param onRetry
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
        
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
                    contentDescription = AppStringsProvider.current().cdRetry,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * networkofflineindicator
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
            
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    text = "网络连接不可用",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "请检查网络后重试",
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
                Text(AppStringsProvider.current().retry)
            }
        }
    }
}

/**
 * API hint
 * 
 * Requirements: 8.2
 */
@Composable
fun RateLimitIndicator(
    retryAfterSeconds: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember(retryAfterSeconds) { mutableIntStateOf(retryAfterSeconds) }
    
    // Note
    LaunchedEffect(retryAfterSeconds) {
        while (remainingSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            remainingSeconds--
        }
        // Note
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
            
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    text = "请求过于频繁",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = if (remainingSeconds > 0) "${remainingSeconds}秒后自动重试" else "正在重试...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            
            if (remainingSeconds > 0) {
                // indicator
                CircularProgressIndicator(
                    progress = { remainingSeconds.toFloat() / retryAfterSeconds },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
