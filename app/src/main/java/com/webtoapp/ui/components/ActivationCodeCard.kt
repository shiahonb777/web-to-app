package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.activation.ActivationCode
import com.webtoapp.core.activation.ActivationCodeType
import com.webtoapp.core.i18n.Strings
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════
// Color & icon mapping for each activation code type
// ═══════════════════════════════════════════

private data class CodeTypeTheme(
    val icon: ImageVector,
    val color: Color,
    val labelBg: Color
)

@Composable
private fun getCodeTypeTheme(type: ActivationCodeType): CodeTypeTheme = when (type) {
    ActivationCodeType.PERMANENT -> CodeTypeTheme(
        icon = Icons.Outlined.AllInclusive,
        color = Color(0xFF2E7D32),
        labelBg = Color(0xFF2E7D32).copy(alpha = 0.1f)
    )
    ActivationCodeType.TIME_LIMITED -> CodeTypeTheme(
        icon = Icons.Outlined.Timer,
        color = Color(0xFF1565C0),
        labelBg = Color(0xFF1565C0).copy(alpha = 0.1f)
    )
    ActivationCodeType.USAGE_LIMITED -> CodeTypeTheme(
        icon = Icons.Outlined.ConfirmationNumber,
        color = Color(0xFF6A1B9A),
        labelBg = Color(0xFF6A1B9A).copy(alpha = 0.1f)
    )
    ActivationCodeType.DEVICE_BOUND -> CodeTypeTheme(
        icon = Icons.Outlined.PhonelinkLock,
        color = Color(0xFFE65100),
        labelBg = Color(0xFFE65100).copy(alpha = 0.1f)
    )
    ActivationCodeType.COMBINED -> CodeTypeTheme(
        icon = Icons.Outlined.Layers,
        color = Color(0xFF00838F),
        labelBg = Color(0xFF00838F).copy(alpha = 0.1f)
    )
}

// ═══════════════════════════════════════════
// Main Card
// ═══════════════════════════════════════════

/**
 * 激活码配置卡片（增强版 - 支持多种类型 + 批量操作）
 */
@Composable
fun ActivationCodeCard(
    enabled: Boolean,
    activationCodes: List<ActivationCode>,
    requireEveryTime: Boolean = false,
    dialogConfig: com.webtoapp.data.model.ActivationDialogConfig = com.webtoapp.data.model.ActivationDialogConfig(),
    activationManager: com.webtoapp.core.activation.ActivationManager,
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<ActivationCode>) -> Unit,
    onRequireEveryTimeChange: (Boolean) -> Unit = {},
    onDialogConfigChange: (com.webtoapp.data.model.ActivationDialogConfig) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showCustomTextSection by remember { mutableStateOf(
        dialogConfig.title.isNotBlank() || dialogConfig.subtitle.isNotBlank() ||
        dialogConfig.inputLabel.isNotBlank() || dialogConfig.buttonText.isNotBlank()
    ) }
    val clipboardManager = LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header with icon + switch ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.VpnKey,
                            null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.activationCodeVerify,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (enabled && activationCodes.isNotEmpty()) {
                            Text(
                                text = Strings.totalCodes.replace("%d", activationCodes.size.toString()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = Strings.activationCodeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // ── Require Every Launch option ──
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.requireEveryLaunch,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (requireEveryTime) Strings.requireEveryLaunchHintOn else Strings.requireEveryLaunchHintOff,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PremiumSwitch(
                        checked = requireEveryTime,
                        onCheckedChange = onRequireEveryTimeChange
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Custom Dialog Text Section ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomTextSection = !showCustomTextSection },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.customDialogText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Strings.customDialogTextHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val customArrowRotation by animateFloatAsState(
                        targetValue = if (showCustomTextSection) 180f else 0f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                        label = "customArrowRotation"
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationZ = customArrowRotation }
                    )
                }
                
                AnimatedVisibility(
                    visible = showCustomTextSection,
                    enter = CardExpandTransition,
                    exit = CardCollapseTransition
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumTextField(
                            value = dialogConfig.title,
                            onValueChange = { onDialogConfigChange(dialogConfig.copy(title = it)) },
                            label = { Text(Strings.dialogTitle) },
                            placeholder = { Text(Strings.dialogTitleHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PremiumTextField(
                            value = dialogConfig.subtitle,
                            onValueChange = { onDialogConfigChange(dialogConfig.copy(subtitle = it)) },
                            label = { Text(Strings.dialogSubtitle) },
                            placeholder = { Text(Strings.dialogSubtitleHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PremiumTextField(
                            value = dialogConfig.inputLabel,
                            onValueChange = { onDialogConfigChange(dialogConfig.copy(inputLabel = it)) },
                            label = { Text(Strings.dialogInputLabel) },
                            placeholder = { Text(Strings.dialogInputLabelHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PremiumTextField(
                            value = dialogConfig.buttonText,
                            onValueChange = { onDialogConfigChange(dialogConfig.copy(buttonText = it)) },
                            label = { Text(Strings.dialogButtonText) },
                            placeholder = { Text(Strings.dialogButtonTextHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Action buttons: Add + Batch + More ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add single code
                    PremiumButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.addActivationCode, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    // Batch generate
                    PremiumOutlinedButton(
                        onClick = { showBatchDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.batchGenerate, maxLines = 1)
                    }
                }

                // ── Toolbar: Copy All + Delete All (only when codes exist) ──
                if (activationCodes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val allCodes = activationCodes.joinToString("\n") { it.code }
                                clipboardManager.setText(AnnotatedString(allCodes))
                                showCopiedSnackbar = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.CopyAll, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.copyAllCodes, style = MaterialTheme.typography.labelSmall)
                        }

                        TextButton(
                            onClick = { showDeleteAllDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.deleteAllCodes, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // ── Activation code list ──
                if (activationCodes.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activationCodes.forEachIndexed { index, code ->
                            EnhancedActivationCodeItem(
                                code = code,
                                onDelete = {
                                    onCodesChange(activationCodes.filterIndexed { i, _ -> i != index })
                                }
                            )
                        }
                    }
                } else {
                    // Empty state
                    EmptyActivationCodesState()
                }
              }
            }
        }
    }

    // ── Dialogs ──
    if (showAddDialog) {
        AddActivationCodeDialog(
            activationManager = activationManager,
            onDismiss = { showAddDialog = false },
            onConfirm = { code ->
                onCodesChange(activationCodes + code)
                showAddDialog = false
            }
        )
    }

    if (showBatchDialog) {
        BatchGenerateDialog(
            activationManager = activationManager,
            onDismiss = { showBatchDialog = false },
            onConfirm = { codes ->
                onCodesChange(activationCodes + codes)
                showBatchDialog = false
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(Strings.deleteAllCodes, fontWeight = FontWeight.Bold) },
            text = { Text(Strings.deleteAllCodesConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        onCodesChange(emptyList())
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.btnDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════
// Empty State
// ═══════════════════════════════════════════

@Composable
private fun EmptyActivationCodesState() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Key,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = Strings.noActivationCodes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ═══════════════════════════════════════════
// Enhanced Activation Code Item
// ═══════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedActivationCodeItem(
    code: ActivationCode,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopiedToast by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val theme = getCodeTypeTheme(code.type)

    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            snackbarHostState.showSnackbar(
                message = Strings.activationCodeCopied,
                duration = SnackbarDuration.Short
            )
            showCopiedToast = false
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    // Type badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = theme.labelBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                theme.icon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = theme.color
                            )
                            Text(
                                text = getActivationTypeName(code.type),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.color
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Activation code text (clickable to copy)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code.code))
                                showCopiedToast = true
                            },
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(code.code))
                                showCopiedToast = true
                            }
                        )
                    ) {
                        Text(
                            text = code.code,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(weight = 1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = Strings.copyActivationCode,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        Strings.btnDelete,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Restriction info chips ──
            val infoChips = buildList {
                when (code.type) {
                    ActivationCodeType.TIME_LIMITED -> {
                        code.timeLimitMs?.let { timeLimit ->
                            val days = TimeUnit.MILLISECONDS.toDays(timeLimit)
                            val hours = TimeUnit.MILLISECONDS.toHours(timeLimit) % 24
                            add("⏱ ${Strings.validityPeriod}：${days}${Strings.days}${if (hours > 0) " ${hours}${Strings.hours}" else ""}")
                        }
                    }
                    ActivationCodeType.USAGE_LIMITED -> {
                        code.usageLimit?.let { limit ->
                            add("🔢 ${Strings.usageCount}：$limit ${Strings.times}")
                        }
                    }
                    ActivationCodeType.COMBINED -> {
                        code.timeLimitMs?.let { timeLimit ->
                            val days = TimeUnit.MILLISECONDS.toDays(timeLimit)
                            add("⏱ ${days}${Strings.days}")
                        }
                        code.usageLimit?.let { limit ->
                            add("🔢 $limit ${Strings.times}")
                        }
                    }
                    ActivationCodeType.DEVICE_BOUND -> {
                        add("🔒 ${Strings.deviceBound}")
                    }
                    ActivationCodeType.PERMANENT -> {
                        add("♾️ ${Strings.permanentValid}")
                    }
                }

                code.note?.takeIf { it.isNotBlank() }?.let { note ->
                    add("📝 $note")
                }
            }

            if (infoChips.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    infoChips.forEach { chip ->
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Add Activation Code Dialog
// ═══════════════════════════════════════════

@Composable
private fun AddActivationCodeDialog(
    activationManager: com.webtoapp.core.activation.ActivationManager,
    onDismiss: () -> Unit,
    onConfirm: (ActivationCode) -> Unit
) {
    var codeType by remember { mutableStateOf(ActivationCodeType.PERMANENT) }
    var timeLimitDays by remember { mutableStateOf("7") }
    var usageLimit by remember { mutableStateOf("100") }
    var note by remember { mutableStateOf("") }
    var customCode by remember { mutableStateOf("") }
    var useCustomCode by remember { mutableStateOf(false) }
    var codeLength by remember { mutableStateOf(com.webtoapp.core.activation.ActivationManager.DEFAULT_CODE_LENGTH.toFloat()) }
    var codeLengthError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(Strings.addActivationCode, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Activation code type selector using chips
                Text(
                    text = Strings.activationCodeType,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActivationCodeType.values().forEach { type ->
                        val theme = getCodeTypeTheme(type)
                        val isSelected = codeType == type
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) theme.labelBg else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { codeType = type }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { codeType = type },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = theme.color
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    theme.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) theme.color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(
                                        text = getActivationTypeName(type),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) theme.color else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = getActivationTypeDesc(type),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Custom activation code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useCustomCode = !useCustomCode },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useCustomCode,
                        onCheckedChange = { useCustomCode = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.useCustomCode, style = MaterialTheme.typography.bodyMedium)
                }

                AnimatedVisibility(visible = useCustomCode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumTextField(
                            value = customCode,
                            onValueChange = {
                                // Allow letters, digits only, max 16 chars
                                val filtered = it.filter { c -> c.isLetterOrDigit() }.take(com.webtoapp.core.activation.ActivationManager.MAX_CODE_LENGTH)
                                customCode = filtered
                                codeLengthError = when {
                                    filtered.length in 1 until com.webtoapp.core.activation.ActivationManager.MIN_CODE_LENGTH ->
                                        Strings.codeTooShort
                                    else -> null
                                }
                            },
                            label = { Text(Strings.inputActivationCode) },
                            placeholder = { Text(Strings.activationCodeExample) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                            },
                            isError = codeLengthError != null,
                            supportingText = codeLengthError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                                ?: { Text("${customCode.length}/${com.webtoapp.core.activation.ActivationManager.MAX_CODE_LENGTH}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Code length selector (only for auto-generated codes)
                AnimatedVisibility(visible = !useCustomCode) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Strings.codeLength,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${codeLength.toInt()} ${Strings.chars}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = codeLength,
                            onValueChange = { codeLength = it },
                            valueRange = com.webtoapp.core.activation.ActivationManager.MIN_CODE_LENGTH.toFloat()..com.webtoapp.core.activation.ActivationManager.MAX_CODE_LENGTH.toFloat(),
                            steps = com.webtoapp.core.activation.ActivationManager.MAX_CODE_LENGTH - com.webtoapp.core.activation.ActivationManager.MIN_CODE_LENGTH - 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Time limit configuration
                AnimatedVisibility(
                    visible = codeType == ActivationCodeType.TIME_LIMITED || 
                        codeType == ActivationCodeType.COMBINED
                ) {
                    PremiumTextField(
                        value = timeLimitDays,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                timeLimitDays = it
                            }
                        },
                        label = { Text(Strings.validityDays) },
                        placeholder = { Text("7") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Timer, null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // Usage limit configuration
                AnimatedVisibility(
                    visible = codeType == ActivationCodeType.USAGE_LIMITED || 
                        codeType == ActivationCodeType.COMBINED
                ) {
                    PremiumTextField(
                        value = usageLimit,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                usageLimit = it
                            }
                        },
                        label = { Text(Strings.usageCount) },
                        placeholder = { Text("100") },
                        leadingIcon = {
                            Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // Note
                PremiumTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Strings.noteOptional) },
                    placeholder = { Text(Strings.vipUserOnly) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Outlined.Notes, null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    val timeLimitMs = when (codeType) {
                        ActivationCodeType.TIME_LIMITED, ActivationCodeType.COMBINED -> {
                            timeLimitDays.toLongOrNull()?.let { 
                                TimeUnit.DAYS.toMillis(it) 
                            }
                        }
                        else -> null
                    }

                    val usageLimitInt = when (codeType) {
                        ActivationCodeType.USAGE_LIMITED, ActivationCodeType.COMBINED -> {
                            usageLimit.toIntOrNull()
                        }
                        else -> null
                    }

                    val code = if (useCustomCode && customCode.isNotBlank()) {
                        val trimmed = customCode.trim()
                        if (trimmed.length < com.webtoapp.core.activation.ActivationManager.MIN_CODE_LENGTH) {
                            codeLengthError = Strings.codeTooShort
                            return@PremiumButton  // abort, code too short
                        }
                        ActivationCode(
                            code = trimmed,
                            type = codeType,
                            timeLimitMs = timeLimitMs,
                            usageLimit = usageLimitInt,
                            note = note.takeIf { it.isNotBlank() }
                        )
                    } else {
                        activationManager.generateActivationCode(
                            type = codeType,
                            timeLimitMs = timeLimitMs,
                            usageLimit = usageLimitInt,
                            note = note.takeIf { it.isNotBlank() },
                            length = codeLength.toInt()
                        )
                    }

                    onConfirm(code)
                }
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.btnOk, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

// ═══════════════════════════════════════════
// Batch Generate Dialog
// ═══════════════════════════════════════════

@Composable
private fun BatchGenerateDialog(
    activationManager: com.webtoapp.core.activation.ActivationManager,
    onDismiss: () -> Unit,
    onConfirm: (List<ActivationCode>) -> Unit
) {
    var codeType by remember { mutableStateOf(ActivationCodeType.PERMANENT) }
    var batchCount by remember { mutableStateOf("5") }
    var timeLimitDays by remember { mutableStateOf("7") }
    var usageLimit by remember { mutableStateOf("100") }
    var codeLength by remember { mutableStateOf(com.webtoapp.core.activation.ActivationManager.DEFAULT_CODE_LENGTH.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(Strings.batchGenerate, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Count
                PremiumTextField(
                    value = batchCount,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) batchCount = it },
                    label = { Text(Strings.batchCount) },
                    placeholder = { Text("5") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Tag, null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                // Type selection (simplified for batch)
                Text(
                    text = Strings.activationCodeType,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActivationCodeType.values().take(3).forEach { type ->
                        val theme = getCodeTypeTheme(type)
                        val isSelected = codeType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { codeType = type },
                            label = {
                                Text(
                                    getActivationTypeName(type),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(theme.icon, null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.labelBg,
                                selectedLabelColor = theme.color,
                                selectedLeadingIconColor = theme.color
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActivationCodeType.values().drop(3).forEach { type ->
                        val theme = getCodeTypeTheme(type)
                        val isSelected = codeType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { codeType = type },
                            label = {
                                Text(
                                    getActivationTypeName(type),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(theme.icon, null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.labelBg,
                                selectedLabelColor = theme.color,
                                selectedLeadingIconColor = theme.color
                            )
                        )
                    }
                }

                // Code length selector
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.codeLength,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${codeLength.toInt()} ${Strings.chars}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = codeLength,
                        onValueChange = { codeLength = it },
                        valueRange = com.webtoapp.core.activation.ActivationManager.MIN_CODE_LENGTH.toFloat()..com.webtoapp.core.activation.ActivationManager.MAX_CODE_LENGTH.toFloat(),
                        steps = com.webtoapp.core.activation.ActivationManager.MAX_CODE_LENGTH - com.webtoapp.core.activation.ActivationManager.MIN_CODE_LENGTH - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Time limit
                AnimatedVisibility(
                    visible = codeType == ActivationCodeType.TIME_LIMITED || codeType == ActivationCodeType.COMBINED
                ) {
                    PremiumTextField(
                        value = timeLimitDays,
                        onValueChange = { if (it.all { c -> c.isDigit() }) timeLimitDays = it },
                        label = { Text(Strings.validityDays) },
                        leadingIcon = { Icon(Icons.Outlined.Timer, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // Usage limit
                AnimatedVisibility(
                    visible = codeType == ActivationCodeType.USAGE_LIMITED || codeType == ActivationCodeType.COMBINED
                ) {
                    PremiumTextField(
                        value = usageLimit,
                        onValueChange = { if (it.all { c -> c.isDigit() }) usageLimit = it },
                        label = { Text(Strings.usageCount) },
                        leadingIcon = { Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    val count = batchCount.toIntOrNull()?.coerceIn(1, 100) ?: 5
                    val timeLimitMs = when (codeType) {
                        ActivationCodeType.TIME_LIMITED, ActivationCodeType.COMBINED ->
                            timeLimitDays.toLongOrNull()?.let { TimeUnit.DAYS.toMillis(it) }
                        else -> null
                    }
                    val usageLimitInt = when (codeType) {
                        ActivationCodeType.USAGE_LIMITED, ActivationCodeType.COMBINED ->
                            usageLimit.toIntOrNull()
                        else -> null
                    }

                    val codes = activationManager.generateActivationCodes(
                        count = count,
                        type = codeType,
                        timeLimitMs = timeLimitMs,
                        usageLimit = usageLimitInt,
                        length = codeLength.toInt()
                    )
                    onConfirm(codes)
                }
            ) {
                Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.batchGenerate, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

// ═══════════════════════════════════════════
// Utilities
// ═══════════════════════════════════════════

private fun getActivationTypeName(type: ActivationCodeType): String = when (type) {
    ActivationCodeType.PERMANENT -> Strings.activationTypePermanent
    ActivationCodeType.TIME_LIMITED -> Strings.activationTypeTimeLimited
    ActivationCodeType.USAGE_LIMITED -> Strings.activationTypeUsageLimited
    ActivationCodeType.DEVICE_BOUND -> Strings.activationTypeDeviceBound
    ActivationCodeType.COMBINED -> Strings.activationTypeCombined
}

private fun getActivationTypeDesc(type: ActivationCodeType): String = when (type) {
    ActivationCodeType.PERMANENT -> Strings.activationTypePermanentDesc
    ActivationCodeType.TIME_LIMITED -> Strings.activationTypeTimeLimitedDesc
    ActivationCodeType.USAGE_LIMITED -> Strings.activationTypeUsageLimitedDesc
    ActivationCodeType.DEVICE_BOUND -> Strings.activationTypeDeviceBoundDesc
    ActivationCodeType.COMBINED -> Strings.activationTypeCombinedDesc
}
