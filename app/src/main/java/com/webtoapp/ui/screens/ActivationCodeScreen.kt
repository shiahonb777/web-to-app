package com.webtoapp.ui.screens

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.ActivationRecord
import com.webtoapp.core.cloud.RedeemPreview
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.FormState
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import com.webtoapp.R

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(Unit) { cloudViewModel.loadActivationHistory() }

    LaunchedEffect(redeemState) {
        when (redeemState) {
            is FormState.Success -> {
                showSuccessAnim = true
                snackbarHostState.showSnackbar((redeemState as FormState.Success).message)
                cloudViewModel.loadActivationHistory()
                cloudViewModel.resetRedeemState()
                code = ""
                kotlinx.coroutines.delay(2000)
                showSuccessAnim = false
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((redeemState as FormState.Error).message)
                cloudViewModel.resetRedeemState()
            }
            else -> {}
        }
    }

    // Preview result handling
    LaunchedEffect(previewState) {
        when (previewState) {
            is FormState.Success -> showPreviewDialog = true
            is FormState.Error -> {
                snackbarHostState.showSnackbar((previewState as FormState.Error).message)
                cloudViewModel.resetPreviewState()
            }
            else -> {}
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            cloudViewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().cloudActivationCode) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Input Section ──
            item {
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with gradient icon
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
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
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = AppStringsProvider.current().cloudRedeemTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = AppStringsProvider.current().cloudRedeemDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Code input field
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' } },
                            placeholder = {
                                Text(
                                    "XXXX-XXXX-XXXX-XXXX",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        letterSpacing = 2.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
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

                        // Character count hint
                        AnimatedVisibility(visible = code.isNotEmpty()) {
                            Text(
                                text = "${code.length} ${AppStringsProvider.current().characterCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (code.length >= 4) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }

                        // Preview + Redeem buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumOutlinedButton(
                                onClick = { cloudViewModel.previewRedeem(code) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = code.length >= 4 && previewState !is FormState.Loading && redeemState !is FormState.Loading
                            ) {
                                if (previewState is FormState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Outlined.Preview, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(stringResource(R.string.btn_preview), fontWeight = FontWeight.Medium)
                            }

                            PremiumButton(
                                onClick = { cloudViewModel.redeemCode(code) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
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
                                    text = if (redeemState is FormState.Loading)
                                        AppStringsProvider.current().cloudRedeeming else AppStringsProvider.current().cloudRedeemBtn,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── Success animation overlay ──
            if (showSuccessAnim) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1B5E20).copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = AppStringsProvider.current().activationSuccess,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            // ── Pro Benefits ──
            item {
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Diamond,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pro ${AppStringsProvider.current().cloudProBenefits}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val benefits = listOf(
                            Pair(Icons.Outlined.Cloud, AppStringsProvider.current().cloudBenefitCloud),
                            Pair(Icons.Outlined.Speed, AppStringsProvider.current().cloudBenefitPriority),
                            Pair(Icons.Outlined.Devices, AppStringsProvider.current().cloudBenefitDevices),
                            Pair(Icons.Outlined.Analytics, AppStringsProvider.current().cloudBenefitAnalytics)
                        )
                        benefits.forEach { (icon, benefit) ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = benefit,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Redemption History ──
            if (history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = AppStringsProvider.current().cloudRedeemHistory,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${history.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                history.forEach { record ->
                    item {
                        EnhancedHistoryRow(record)
                    }
                }
            }
        }
        }

        // ── Preview Dialog ──
        if (showPreviewDialog && redeemPreview != null) {
            EnhancedRedeemPreviewDialog(
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

// ═══════════════════════════════════════════
// Enhanced History Row
// ═══════════════════════════════════════════

@Composable
private fun EnhancedHistoryRow(record: ActivationRecord) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    text = record.planType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    record.createdAt?.let {
                        Text(
                            it.take(10),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    record.note?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            record.proEnd?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = AppStringsProvider.current().cloudValidUntil,
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
    }
}

// ═══════════════════════════════════════════
// Enhanced Redeem Preview Dialog
// ═══════════════════════════════════════════

@Composable
private fun EnhancedRedeemPreviewDialog(
    preview: RedeemPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val upgradeColor = Color(0xFF2E7D32)
    val normalColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (preview.isUpgrade) upgradeColor.copy(alpha = 0.1f)
                        else normalColor.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (preview.isUpgrade) Icons.AutoMirrored.Filled.TrendingUp else Icons.Filled.SwapHoriz,
                    null,
                    tint = if (preview.isUpgrade) upgradeColor else normalColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                if (preview.isUpgrade) "🚀 ${AppStringsProvider.current().cloudTierUpgrade}" else AppStringsProvider.current().cloudRedeemPreview,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Code info card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            AppStringsProvider.current().cloudActivationCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${preview.codeTier.uppercase()} · ${preview.codePlanType}" +
                                if (preview.durationDays > 0) " · ${preview.durationDays}${AppStringsProvider.current().days}" else " · ${AppStringsProvider.current().cloudLifetime}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Current → New state comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current state
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                AppStringsProvider.current().cloudCurrentPlan,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                preview.currentTier.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (preview.currentIsLifetime) {
                                Text(AppStringsProvider.current().cloudLifetime, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700))
                            } else {
                                preview.currentExpiresAt?.let {
                                    Text(
                                        it.take(10),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        modifier = Modifier.padding(horizontal = 8.dp).size(24.dp),
                        tint = if (preview.isUpgrade) upgradeColor else normalColor
                    )

                    // New state
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (preview.isUpgrade) upgradeColor else normalColor).copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                AppStringsProvider.current().cloudAfterRedeem,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (preview.isUpgrade) upgradeColor else normalColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                preview.newTier.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (preview.isUpgrade) upgradeColor else normalColor
                            )
                            if (preview.newIsLifetime) {
                                Text(AppStringsProvider.current().cloudLifetime, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700))
                            } else {
                                preview.newExpiresAt?.let {
                                    Text(
                                        it.take(10),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (preview.isUpgrade) upgradeColor else normalColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Upgrade badge
                if (preview.isUpgrade) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = upgradeColor.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                null,
                                tint = upgradeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                AppStringsProvider.current().cloudUpgradeNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = upgradeColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            PremiumButton(onClick = onConfirm) {
                Icon(Icons.Filled.Redeem, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStringsProvider.current().cloudConfirmRedeem, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )
}
