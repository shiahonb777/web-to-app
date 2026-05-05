package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.R
import com.webtoapp.core.cloud.ActivationRecord
import com.webtoapp.core.cloud.RedeemPreview
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.design.WtaActionBar
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.design.WtaBadge
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.FormState
import kotlinx.coroutines.delay

@Composable
fun ActivationCodeScreen(
    cloudViewModel: CloudViewModel,
    onBack: () -> Unit
) {
    val redeemState by cloudViewModel.redeemState.collectAsStateWithLifecycle()
    val previewState by cloudViewModel.previewState.collectAsStateWithLifecycle()
    val redeemPreview by cloudViewModel.redeemPreview.collectAsStateWithLifecycle()
    val history by cloudViewModel.activationHistory.collectAsStateWithLifecycle()
    val message by cloudViewModel.message.collectAsStateWithLifecycle()

    var code by remember { mutableStateOf("") }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showSuccessAnim by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        cloudViewModel.loadActivationHistory()
    }

    LaunchedEffect(redeemState) {
        when (redeemState) {
            is FormState.Success -> {
                showSuccessAnim = true
                cloudViewModel.loadActivationHistory()
                cloudViewModel.resetRedeemState()
                code = ""
                delay(2000)
                showSuccessAnim = false
            }
            is FormState.Error -> {
                cloudViewModel.resetRedeemState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(previewState) {
        when (previewState) {
            is FormState.Success -> showPreviewDialog = true
            is FormState.Error -> cloudViewModel.resetPreviewState()
            else -> Unit
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            cloudViewModel.clearMessage()
        }
    }

    WtaScreen(
        title = Strings.cloudActivationCode,
        snackbarHostState = snackbarHostState,
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = WtaSpacing.ScreenHorizontal,
                top = WtaSpacing.ScreenVertical,
                end = WtaSpacing.ScreenHorizontal,
                bottom = WtaSpacing.ScreenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {
            item {
                WtaSection(
                    title = Strings.cloudRedeemTitle,
                    description = Strings.cloudRedeemDesc
                ) {
                    WtaSettingCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(WtaRadius.Control))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Redeem,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Strings.cloudRedeemTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = Strings.cloudRedeemDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = code,
                                onValueChange = {
                                    code = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }
                                },
                                placeholder = {
                                    Text(
                                        "XXXX-XXXX-XXXX-XXXX",
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            letterSpacing = 2.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(WtaRadius.Control),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Key,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                },
                                trailingIcon = {
                                    if (code.isNotEmpty()) {
                                        IconButton(onClick = { code = "" }) {
                                            Icon(
                                                Icons.Outlined.Clear,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            )

                            AnimatedVisibility(visible = code.isNotEmpty()) {
                                Text(
                                    text = "${code.length} ${Strings.characterCount}",
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (code.length >= 4) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.End
                                )
                            }

                            WtaActionBar {
                                PremiumOutlinedButton(
                                    onClick = { cloudViewModel.previewRedeem(code) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(WtaRadius.Button),
                                    enabled = code.length >= 4 &&
                                        previewState !is FormState.Loading &&
                                        redeemState !is FormState.Loading
                                ) {
                                    if (previewState is FormState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Icon(Icons.Outlined.Preview, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(stringResource(R.string.btn_preview), fontWeight = FontWeight.Medium)
                                }

                                PremiumButton(
                                    onClick = { cloudViewModel.redeemCode(code) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(WtaRadius.Button),
                                    enabled = code.length >= 4 && redeemState !is FormState.Loading
                                ) {
                                    if (redeemState is FormState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Icon(Icons.Filled.Redeem, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (redeemState is FormState.Loading) {
                                            Strings.cloudRedeeming
                                        } else {
                                            Strings.cloudRedeemBtn
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = showSuccessAnim,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    WtaStatusBanner(
                        message = Strings.activationSuccess,
                        tone = WtaStatusTone.Success
                    )
                }
            }

            item {
                WtaSection(title = Strings.cloudProBenefits) {
                    WtaSettingCard {
                        val benefits = listOf(
                            Icons.Filled.Cloud to Strings.cloudBenefitCloud,
                            Icons.Filled.Devices to Strings.cloudBenefitDevices,
                            Icons.Filled.Speed to Strings.cloudBenefitPriority,
                            Icons.Filled.AutoAwesome to Strings.cloudBenefitAnalytics
                        )
                        benefits.forEachIndexed { index, (icon, benefit) ->
                            WtaSettingRow(
                                title = benefit,
                                icon = icon,
                                enabled = false,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            )
                            if (index < benefits.lastIndex) {
                                WtaSectionDivider()
                            }
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                item {
                        WtaSection(
                            title = Strings.cloudRedeemHistory,
                            trailing = {
                                WtaBadge(
                                    text = history.size.toString(),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                        WtaSettingCard {
                            history.forEachIndexed { index, record ->
                                ActivationHistoryRow(record)
                                if (index < history.lastIndex) {
                                    WtaSectionDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPreviewDialog && redeemPreview != null) {
            RedeemPreviewDialog(
                preview = redeemPreview!!,
                onConfirm = {
                    showPreviewDialog = false
                    cloudViewModel.resetPreviewState()
                    cloudViewModel.redeemCode(code)
                },
                onDismiss = {
                    showPreviewDialog = false
                    cloudViewModel.resetPreviewState()
                }
            )
        }
    }
}

@Composable
private fun ActivationHistoryRow(record: ActivationRecord) {
    WtaSettingRow(
        title = record.planType.replaceFirstChar { it.uppercase() },
        subtitle = listOfNotNull(record.createdAt?.take(10), record.note)
            .joinToString(" · ")
            .ifBlank { null },
        icon = Icons.Filled.Verified,
        enabled = false,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        record.proEnd?.let {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Strings.cloudValidUntil,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = it.take(10),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RedeemPreviewDialog(
    preview: RedeemPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(WtaRadius.Card),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (preview.isUpgrade) Icons.AutoMirrored.Filled.TrendingUp else Icons.Filled.SwapHoriz,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = if (preview.isUpgrade) Strings.cloudTierUpgrade else Strings.cloudRedeemPreview,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WtaSettingCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = Strings.cloudActivationCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append("${preview.codeTier.uppercase()} · ${preview.codePlanType}")
                                append(
                                    if (preview.durationDays > 0) {
                                        " · ${preview.durationDays}${Strings.days}"
                                    } else {
                                        " · ${Strings.cloudLifetime}"
                                    }
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WtaSettingCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Strings.cloudCurrentPlan,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preview.currentTier.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (preview.currentIsLifetime) {
                                Text(
                                    text = Strings.cloudLifetime,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                preview.currentExpiresAt?.let {
                                    Text(it.take(10), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    WtaSettingCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Strings.cloudAfterRedeem,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preview.newTier.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (preview.newIsLifetime) {
                                Text(
                                    text = Strings.cloudLifetime,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                preview.newExpiresAt?.let {
                                    Text(it.take(10), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (preview.isUpgrade) {
                    WtaStatusBanner(
                        message = Strings.cloudUpgradeNotice,
                        tone = WtaStatusTone.Warning
                    )
                }
            }
        },
        confirmButton = {
            PremiumButton(onClick = onConfirm) {
                Icon(Icons.Filled.Redeem, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.cloudConfirmRedeem, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
