package com.webtoapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.activation.ActivationResult
import com.webtoapp.core.activation.ActivationCodeType
import com.webtoapp.core.activation.ActivationStatus
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.util.threadLocalCompat
import java.text.SimpleDateFormat
import java.util.*
/**
 * enhancedactivation codedialog- advanced, state
 */
@Composable
fun EnhancedActivationDialog(
    onDismiss: () -> Unit,
    onActivate: suspend (String) -> ActivationResult,
    activationStatus: ActivationStatus? = null,
    customTitle: String = "",
    customSubtitle: String = "",
    customInputLabel: String = "",
    customButtonText: String = ""
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var activationResult by remember { mutableStateOf<ActivationResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Header with icon ──
                ActivationDialogHeader(
                    title = customTitle.ifBlank { AppStringsProvider.current().activateApp },
                    subtitle = customSubtitle.ifBlank { AppStringsProvider.current().enterActivationCodeToContinue },
                    isLoading = isLoading,
                    result = activationResult
                )

                // ── Current activation status card ──
                activationStatus?.let { status ->
                    if (status.isActivated) {
                        EnhancedActivationStatusCard(status = status)
                    }
                }

                // ── Input field ──
                AnimatedVisibility(
                    visible = activationResult !is ActivationResult.Success,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    PremiumTextField(
                        value = code,
                        onValueChange = {
                            code = it.filter { c -> c.isLetterOrDigit() }
                                .take(com.webtoapp.core.activation.ActivationManager.MAX_CODE_LENGTH)
                            error = null
                            activationResult = null
                        },
                        label = { Text(customInputLabel.ifBlank { AppStringsProvider.current().inputActivationCode }) },
                        placeholder = { Text(AppStringsProvider.current().activationCodeExample) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        isError = error != null,
                        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }

                // ── Result feedback ──
                activationResult?.let { result ->
                    ActivationResultCard(result = result)
                }
            }
        },
        confirmButton = {
            AnimatedVisibility(
                visible = activationResult !is ActivationResult.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PremiumButton(
                    onClick = {
                        if (code.isBlank()) {
                            error = AppStringsProvider.current().pleaseEnterActivationCode
                        } else {
                            isLoading = true
                            error = null
                            activationResult = null
                        }
                    },
                    enabled = !isLoading && code.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        if (isLoading) Icons.Outlined.HourglassTop else Icons.Outlined.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        customButtonText.ifBlank { AppStringsProvider.current().activate },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )

    // Handle activation logic
    LaunchedEffect(isLoading) {
        if (isLoading && code.isNotBlank()) {
            val result = onActivate(code)
            activationResult = result
            isLoading = false

            if (result is ActivationResult.Success) {
                kotlinx.coroutines.delay(1500)
                onDismiss()
            }
        }
    }
}

// ═══════════════════════════════════════════
// Dialog Header
// ═══════════════════════════════════════════

@Composable
private fun ActivationDialogHeader(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    result: ActivationResult?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val iconBgColor = when (result) {
        is ActivationResult.Success -> Color(0xFF2E7D32)
        is ActivationResult.Invalid, is ActivationResult.DeviceMismatch,
        is ActivationResult.Expired, is ActivationResult.UsageExceeded -> MaterialTheme.colorScheme.error
        is ActivationResult.AlreadyActivated -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    val icon: ImageVector = when (result) {
        is ActivationResult.Success -> Icons.Filled.CheckCircle
        is ActivationResult.Invalid -> Icons.Filled.Cancel
        is ActivationResult.DeviceMismatch -> Icons.Filled.PhonelinkErase
        is ActivationResult.Expired -> Icons.Filled.TimerOff
        is ActivationResult.UsageExceeded -> Icons.Filled.Block
        is ActivationResult.AlreadyActivated -> Icons.Filled.Verified
        else -> Icons.Filled.Shield
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(if (isLoading) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            iconBgColor.copy(alpha = 0.15f),
                            iconBgColor.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = iconBgColor,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = iconBgColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = when (result) {
                is ActivationResult.Success -> AppStringsProvider.current().activationSuccess
                is ActivationResult.AlreadyActivated -> AppStringsProvider.current().appAlreadyActivated
                else -> title
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle
        Text(
            text = when (result) {
                is ActivationResult.Success -> AppStringsProvider.current().activationSuccessHint
                is ActivationResult.AlreadyActivated -> AppStringsProvider.current().appAlreadyActivatedHint
                else -> subtitle
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ═══════════════════════════════════════════
// Result Feedback Card
// ═══════════════════════════════════════════

@Composable
private fun ActivationResultCard(result: ActivationResult) {
    val (icon, title, message, containerColor, contentColor, suggestion) = when (result) {
        is ActivationResult.Success -> ResultCardData(
            icon = Icons.Filled.CheckCircle,
            title = AppStringsProvider.current().activationSuccess,
            message = AppStringsProvider.current().activationSuccessDetail,
            containerColor = Color(0xFF1B5E20).copy(alpha = 0.1f),
            contentColor = Color(0xFF2E7D32),
            suggestion = null
        )
        is ActivationResult.Invalid -> ResultCardData(
            icon = Icons.Filled.Cancel,
            title = AppStringsProvider.current().invalidActivationCode,
            message = result.message.ifBlank { AppStringsProvider.current().invalidActivationCode },
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.error,
            suggestion = AppStringsProvider.current().invalidCodeSuggestion
        )
        is ActivationResult.DeviceMismatch -> ResultCardData(
            icon = Icons.Filled.PhonelinkErase,
            title = AppStringsProvider.current().activationCodeBoundToOtherDevice,
            message = AppStringsProvider.current().deviceMismatchDetail,
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.error,
            suggestion = AppStringsProvider.current().deviceMismatchSuggestion
        )
        is ActivationResult.Expired -> ResultCardData(
            icon = Icons.Filled.TimerOff,
            title = AppStringsProvider.current().activationCodeExpired,
            message = AppStringsProvider.current().expiredDetail,
            containerColor = Color(0xFFF57F17).copy(alpha = 0.1f),
            contentColor = Color(0xFFE65100),
            suggestion = AppStringsProvider.current().expiredSuggestion
        )
        is ActivationResult.UsageExceeded -> ResultCardData(
            icon = Icons.Filled.Block,
            title = AppStringsProvider.current().activationCodeUsageExceeded,
            message = AppStringsProvider.current().usageExceededDetail,
            containerColor = Color(0xFFF57F17).copy(alpha = 0.1f),
            contentColor = Color(0xFFE65100),
            suggestion = AppStringsProvider.current().usageExceededSuggestion
        )
        is ActivationResult.AlreadyActivated -> ResultCardData(
            icon = Icons.Filled.Verified,
            title = AppStringsProvider.current().appAlreadyActivated,
            message = AppStringsProvider.current().alreadyActivatedDetail,
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
            suggestion = null
        )
        is ActivationResult.Empty -> return
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(350)) + expandVertically(
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )

                // Actionable suggestion
                suggestion?.let { sug ->
                    HorizontalDivider(
                        color = contentColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = sug,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Enhanced Activation Status Card
// ═══════════════════════════════════════════

@Composable
private fun EnhancedActivationStatusCard(status: ActivationStatus) {
    val isValid = status.isValid
    val primaryColor = if (isValid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val bgColor = if (isValid) Color(0xFF1B5E20).copy(alpha = 0.08f)
                  else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isValid) Icons.Filled.VerifiedUser else Icons.Filled.GppBad,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isValid) AppStringsProvider.current().activated else AppStringsProvider.current().activationExpired,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                // Type badge
                status.codeType?.let { type ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = primaryColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = getStatusTypeName(type),
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Activation time
            status.activatedTime?.let { time ->
                StatusInfoRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = AppStringsProvider.current().activationTime,
                    value = formatTime(time),
                    color = primaryColor
                )
            }

            // Time remaining with progress bar
            status.expireTime?.let {
                val remaining = status.remainingTimeMs
                if (remaining != null && remaining > 0) {
                    val days = remaining / (24 * 60 * 60 * 1000)
                    val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                    StatusInfoRow(
                        icon = Icons.Outlined.Timer,
                        label = AppStringsProvider.current().remainingTime,
                        value = "${days}${AppStringsProvider.current().days} ${hours}${AppStringsProvider.current().hours}",
                        color = primaryColor
                    )
                    // Time progress bar
                    status.activatedTime?.let { activatedTime ->
                        val totalDuration = it - activatedTime
                        if (totalDuration > 0) {
                            val elapsed = System.currentTimeMillis() - activatedTime
                            val progress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { 1f - progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (progress > 0.8f) Color(0xFFE65100) else primaryColor,
                                trackColor = primaryColor.copy(alpha = 0.1f)
                            )
                        }
                    }
                } else {
                    StatusInfoRow(
                        icon = Icons.Outlined.EventBusy,
                        label = AppStringsProvider.current().expireTime,
                        value = formatTime(it),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Usage count with progress bar
            status.usageLimit?.let { limit ->
                val remaining = status.remainingUsage ?: 0
                StatusInfoRow(
                    icon = Icons.Outlined.ConfirmationNumber,
                    label = AppStringsProvider.current().remainingUsage,
                    value = "$remaining / $limit",
                    color = primaryColor
                )
                val progress = if (limit > 0) status.usageCount.toFloat() / limit else 0f
                LinearProgressIndicator(
                    progress = { 1f - progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (progress > 0.8f) Color(0xFFE65100) else primaryColor,
                    trackColor = primaryColor.copy(alpha = 0.1f)
                )
            }

            // Device binding
            status.deviceId?.let {
                StatusInfoRow(
                    icon = Icons.Outlined.PhonelinkLock,
                    label = AppStringsProvider.current().deviceBound,
                    value = "",
                    color = primaryColor
                )
            }
        }
    }
}

@Composable
private fun StatusInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.7f)
        )
        if (value.isNotBlank()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

// ═══════════════════════════════════════════
// Data classes & Utilities
// ═══════════════════════════════════════════

private data class ResultCardData(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val containerColor: Color,
    val contentColor: Color,
    val suggestion: String?
)

private fun getStatusTypeName(type: ActivationCodeType): String = when (type) {
    ActivationCodeType.PERMANENT -> AppStringsProvider.current().activationTypePermanent
    ActivationCodeType.TIME_LIMITED -> AppStringsProvider.current().activationTypeTimeLimited
    ActivationCodeType.USAGE_LIMITED -> AppStringsProvider.current().activationTypeUsageLimited
    ActivationCodeType.DEVICE_BOUND -> AppStringsProvider.current().activationTypeDeviceBound
    ActivationCodeType.COMBINED -> AppStringsProvider.current().activationTypeCombined
}

private val ACTIVATION_DATE_FORMAT = threadLocalCompat {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
}

private fun formatTime(timestamp: Long): String {
    return ACTIVATION_DATE_FORMAT.get()!!.format(Date(timestamp))
}
