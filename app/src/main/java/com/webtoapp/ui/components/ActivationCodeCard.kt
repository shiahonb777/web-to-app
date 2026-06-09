package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.design.WtaSwitch
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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

private data class CodeTypeTheme(
    val icon: ImageVector,
    val color: Color,
    val labelBg: Color
)

@Composable
private fun getCodeTypeTheme(type: ActivationCodeType): CodeTypeTheme {
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme

    return when (type) {
        ActivationCodeType.PERMANENT -> CodeTypeTheme(
            icon = Icons.Outlined.AllInclusive,
            color = scheme.primary,
            labelBg = scheme.primary.copy(alpha = 0.10f)
        )
        ActivationCodeType.TIME_LIMITED -> CodeTypeTheme(
            icon = Icons.Outlined.Timer,
            color = scheme.secondary,
            labelBg = scheme.secondary.copy(alpha = 0.10f)
        )
        ActivationCodeType.USAGE_LIMITED -> CodeTypeTheme(
            icon = Icons.Outlined.ConfirmationNumber,
            color = scheme.tertiary,
            labelBg = scheme.tertiary.copy(alpha = 0.10f)
        )
        ActivationCodeType.DEVICE_BOUND -> CodeTypeTheme(
            icon = Icons.Outlined.PhonelinkLock,
            color = com.webtoapp.ui.design.WtaColors.semantic.warning,
            labelBg = com.webtoapp.ui.design.WtaColors.semantic.warningContainer
        )
        ActivationCodeType.COMBINED -> CodeTypeTheme(
            icon = Icons.Outlined.Layers,
            color = com.webtoapp.ui.design.WtaColors.semantic.info,
            labelBg = com.webtoapp.ui.design.WtaColors.semantic.infoContainer
        )
    }
}

@Composable
fun ActivationCodeCard(
    enabled: Boolean,
    activationCodes: List<ActivationCode>,
    requireEveryTime: Boolean = false,
    dialogConfig: com.webtoapp.data.model.ActivationDialogConfig = com.webtoapp.data.model.ActivationDialogConfig(),
    remoteConfig: com.webtoapp.data.model.RemoteActivationConfig = com.webtoapp.data.model.RemoteActivationConfig(),
    onEnabledChange: (Boolean) -> Unit,
    onCodesChange: (List<ActivationCode>) -> Unit,
    onRequireEveryTimeChange: (Boolean) -> Unit = {},
    onDialogConfigChange: (com.webtoapp.data.model.ActivationDialogConfig) -> Unit = {},
    onRemoteConfigChange: (com.webtoapp.data.model.RemoteActivationConfig) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showRemoteGuideDialog by remember { mutableStateOf(false) }
    var showCustomTextSection by remember { mutableStateOf(
        dialogConfig.title.isNotBlank() || dialogConfig.subtitle.isNotBlank() ||
        dialogConfig.inputLabel.isNotBlank() || dialogConfig.buttonText.isNotBlank()
    ) }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(showCopiedSnackbar) {
        if (showCopiedSnackbar) {
            snackbarHostState.showSnackbar(
                message = Strings.copiedToClipboard,
                duration = SnackbarDuration.Short
            )
            showCopiedSnackbar = false
        }
    }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

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
                    Text(
                        text = Strings.activationCodeVerify,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                WtaSwitch(
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
                    WtaSwitch(
                        checked = requireEveryTime,
                        onCheckedChange = onRequireEveryTimeChange
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                RemoteActivationSection(
                    remoteConfig = remoteConfig,
                    onRemoteConfigChange = onRemoteConfigChange,
                    onShowGuide = { showRemoteGuideDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    PremiumButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.addActivationCode, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    PremiumOutlinedButton(
                        onClick = { showBatchDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.batchGenerate, maxLines = 1)
                    }
                }

                PremiumOutlinedButton(
                    onClick = { showBatchImportDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.PostAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.batchImport, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

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

                    EmptyActivationCodesState()
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
              }
            }
        }
    }

    if (showAddDialog) {
        AddActivationCodeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { code ->
                onCodesChange(activationCodes + code)
                showAddDialog = false
            }
        )
    }

    if (showBatchDialog) {
        BatchGenerateDialog(
            onDismiss = { showBatchDialog = false },
            onConfirm = { codes ->
                onCodesChange(activationCodes + codes)
                showBatchDialog = false
            }
        )
    }

    if (showBatchImportDialog) {
        BatchImportDialog(
            existingCodes = activationCodes,
            onDismiss = { showBatchImportDialog = false },
            onConfirm = { codes ->
                onCodesChange(activationCodes + codes)
                showBatchImportDialog = false
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
            title = { Text(Strings.deleteAllCodes, fontWeight = FontWeight.SemiBold) },
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

    if (showRemoteGuideDialog) {
        RemoteActivationGuideDialog(
            onDismiss = { showRemoteGuideDialog = false },
            onCopy = {
                clipboardManager.setText(AnnotatedString(it))
                showCopiedSnackbar = true
            }
        )
    }
}

@Composable
private fun RemoteActivationSection(
    remoteConfig: com.webtoapp.data.model.RemoteActivationConfig,
    onRemoteConfigChange: (com.webtoapp.data.model.RemoteActivationConfig) -> Unit,
    onShowGuide: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    text = Strings.remoteActivationTitle,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = Strings.remoteActivationHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            WtaSwitch(
                checked = remoteConfig.enabled,
                onCheckedChange = { onRemoteConfigChange(remoteConfig.copy(enabled = it)) }
            )
        }

        TextButton(
            onClick = onShowGuide,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(Strings.remoteActivationGuideButton)
        }

        AnimatedVisibility(
            visible = remoteConfig.enabled,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.DataObject,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Strings.remoteActivationProtocolSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                PremiumTextField(
                    value = remoteConfig.verifyUrl,
                    onValueChange = { onRemoteConfigChange(remoteConfig.copy(verifyUrl = it.trim())) },
                    label = { Text(Strings.remoteActivationUrlLabel) },
                    placeholder = { Text("https://") },
                    supportingText = { Text(Strings.remoteActivationUrlSupporting) },
                    singleLine = true,
                    isError = remoteConfig.verifyUrl.isNotBlank() &&
                        !remoteConfig.verifyUrl.startsWith("https://", ignoreCase = true),
                    modifier = Modifier.fillMaxWidth()
                )
                PremiumTextField(
                    value = remoteConfig.publicKeyBase64,
                    onValueChange = { onRemoteConfigChange(remoteConfig.copy(publicKeyBase64 = it.trim())) },
                    label = { Text(Strings.remoteActivationPublicKeyLabel) },
                    supportingText = { Text(Strings.remoteActivationPublicKeySupporting) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = Strings.remoteActivationOfflineLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val policies = listOf(
                    com.webtoapp.data.model.RemoteActivationOfflinePolicy.ALLOW_CACHED to Strings.remoteActivationOfflineAllowCached,
                    com.webtoapp.data.model.RemoteActivationOfflinePolicy.DENY to Strings.remoteActivationOfflineDeny,
                    com.webtoapp.data.model.RemoteActivationOfflinePolicy.ALLOW to Strings.remoteActivationOfflineAllow
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    policies.forEach { (policy, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRemoteConfigChange(remoteConfig.copy(offlinePolicy = policy)) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = remoteConfig.offlinePolicy == policy,
                                onClick = { onRemoteConfigChange(remoteConfig.copy(offlinePolicy = policy)) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Text(
                    text = Strings.remoteActivationPrivacyNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = com.webtoapp.ui.design.WtaColors.semantic.warning
                )
            }
        }
    }
}

@Composable
private fun RemoteActivationGuideDialog(
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    var copiedCode by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.close)
            }
        },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.IntegrationInstructions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(Strings.remoteActivationGuideTitle)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = Strings.remoteActivationGuideIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RemoteActivationGuideSection(
                    title = Strings.remoteActivationGuideRequestTitle,
                    body = Strings.remoteActivationGuideRequestBody
                )
                RemoteActivationCodeBlock(
                    title = Strings.remoteActivationGuideRequestExampleTitle,
                    code = remoteActivationRequestExample,
                    copied = copiedCode == remoteActivationRequestExample,
                    onCopy = {
                        copiedCode = remoteActivationRequestExample
                        onCopy(remoteActivationRequestExample)
                    }
                )
                RemoteActivationGuideSection(
                    title = Strings.remoteActivationGuideResponseTitle,
                    body = Strings.remoteActivationGuideResponseBody
                )
                RemoteActivationCodeBlock(
                    title = Strings.remoteActivationGuideResponseExampleTitle,
                    code = remoteActivationResponseExample,
                    copied = copiedCode == remoteActivationResponseExample,
                    onCopy = {
                        copiedCode = remoteActivationResponseExample
                        onCopy(remoteActivationResponseExample)
                    }
                )
                RemoteActivationGuideSection(
                    title = Strings.remoteActivationGuideSignatureTitle,
                    body = Strings.remoteActivationGuideSignatureBody
                )
                RemoteActivationCodeBlock(
                    title = Strings.remoteActivationGuideSignatureExampleTitle,
                    code = remoteActivationSignedPayloadExample,
                    copied = copiedCode == remoteActivationSignedPayloadExample,
                    onCopy = {
                        copiedCode = remoteActivationSignedPayloadExample
                        onCopy(remoteActivationSignedPayloadExample)
                    }
                )
                RemoteActivationGuideSection(
                    title = Strings.remoteActivationGuideKeysTitle,
                    body = Strings.remoteActivationGuideKeysBody
                )
                RemoteActivationCodeBlock(
                    title = Strings.remoteActivationGuidePhpExampleTitle,
                    code = remoteActivationPhpExample,
                    copied = copiedCode == remoteActivationPhpExample,
                    onCopy = {
                        copiedCode = remoteActivationPhpExample
                        onCopy(remoteActivationPhpExample)
                    }
                )
                RemoteActivationGuideSection(
                    title = Strings.remoteActivationGuideDeployTitle,
                    body = Strings.remoteActivationGuideDeployBody
                )
            }
        }
    )
}

@Composable
private fun RemoteActivationGuideSection(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RemoteActivationCodeBlock(
    title: String,
    code: String,
    copied: Boolean,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = onCopy,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (copied) Strings.copied else Strings.copy)
                }
            }
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}

private val remoteActivationRequestExample = """
POST /activation/verify HTTP/1.1
Content-Type: application/json

{
  "code": "D2CD-9BA0-F334",
  "deviceId": "device-id-from-app",
  "packageName": "com.example.app",
  "nonce": "client-random-nonce",
  "ts": 1780995251386
}
""".trimIndent()

private val remoteActivationResponseExample = """
{
  "ok": true,
  "message": "OK",
  "expiresAt": null,
  "remainingUses": null,
  "nonce": "client-random-nonce",
  "sig": "base64-ecdsa-signature"
}
""".trimIndent()

private val remoteActivationSignedPayloadExample = """
{"ok":true,"expiresAt":0,"remainingUses":-1,"nonce":"client-random-nonce"}
""".trimIndent()

private val remoteActivationPhpExample = """
<?php
header('Content-Type: application/json; charset=utf-8');

${'$'}privateKey = openssl_pkey_get_private(file_get_contents(__DIR__ . '/private.pem'));
${'$'}input = json_decode(file_get_contents('php://input'), true) ?: [];

${'$'}codes = [
    'D2CD-9BA0-F334' => ['expiresAt' => null, 'remainingUses' => null],
];
${'$'}allowedPackages = ['com.example.app'];

${'$'}code = strtoupper(trim(${'$'}input['code'] ?? ''));
${'$'}nonce = (string)(${'$'}input['nonce'] ?? '');
${'$'}packageName = (string)(${'$'}input['packageName'] ?? '');
${'$'}record = ${'$'}codes[${'$'}code] ?? null;

${'$'}ok = ${'$'}record !== null &&
    ${'$'}nonce !== '' &&
    in_array(${'$'}packageName, ${'$'}allowedPackages, true);
${'$'}expiresAt = ${'$'}record['expiresAt'] ?? null;
${'$'}remainingUses = ${'$'}record['remainingUses'] ?? null;

if (${'$'}expiresAt !== null && ${'$'}expiresAt <= (int)(microtime(true) * 1000)) {
    ${'$'}ok = false;
}

${'$'}signedPayload = json_encode([
    'ok' => ${'$'}ok,
    'expiresAt' => ${'$'}expiresAt ?? 0,
    'remainingUses' => ${'$'}remainingUses ?? -1,
    'nonce' => ${'$'}nonce,
], JSON_UNESCAPED_SLASHES);

openssl_sign(${'$'}signedPayload, ${'$'}signature, ${'$'}privateKey, OPENSSL_ALGO_SHA256);

echo json_encode([
    'ok' => ${'$'}ok,
    'message' => ${'$'}ok ? 'OK' : 'Invalid or expired code',
    'expiresAt' => ${'$'}expiresAt,
    'remainingUses' => ${'$'}remainingUses,
    'nonce' => ${'$'}nonce,
    'sig' => base64_encode(${'$'}signature),
], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
""".trimIndent()

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
                            fontWeight = FontWeight.SemiBold,
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

@Composable
private fun AddActivationCodeDialog(
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
                Text(Strings.addActivationCode, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

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
                            return@PremiumButton
                        }
                        ActivationCode(
                            code = trimmed,
                            type = codeType,
                            timeLimitMs = timeLimitMs,
                            usageLimit = usageLimitInt,
                            note = note.takeIf { it.isNotBlank() }
                        )
                    } else {
                        val activationManager = com.webtoapp.WebToAppApplication.getInstance()
                            .activationManager
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

@Composable
private fun BatchGenerateDialog(
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
        title = { Text(Strings.batchGenerate, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

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

                    val activationManager = com.webtoapp.WebToAppApplication.getInstance()
                        .activationManager
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

private fun parseBatchImportCodes(
    raw: String,
    existingCodes: List<ActivationCode>
): List<ActivationCode> {
    val minLen = com.webtoapp.core.activation.ActivationManager.MIN_CODE_LENGTH
    val maxLen = com.webtoapp.core.activation.ActivationManager.MAX_CODE_LENGTH

    fun normalize(code: String): String =
        code.replace("-", "").replace(" ", "").uppercase().trim()

    val seen = existingCodes.map { normalize(it.code) }.toMutableSet()
    val result = mutableListOf<ActivationCode>()

    raw.split("\n").forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach
        if (trimmed.length < minLen || trimmed.length > maxLen) return@forEach
        val key = normalize(trimmed)
        if (key.isEmpty() || key in seen) return@forEach
        seen.add(key)
        result.add(
            ActivationCode(
                code = trimmed,
                type = ActivationCodeType.PERMANENT,
                note = Strings.batchImportNote
            )
        )
    }

    return result
}

@Composable
private fun BatchImportDialog(
    existingCodes: List<ActivationCode>,
    onDismiss: () -> Unit,
    onConfirm: (List<ActivationCode>) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val nonEmptyLineCount = input.split("\n").count { it.isBlank().not() }
    val validCount = parseBatchImportCodes(input, existingCodes).size

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                Icons.Outlined.PostAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(Strings.batchImportCodes, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Strings.batchImportCodesHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                PremiumTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        errorText = null
                    },
                    label = { Text(Strings.batchImportInputLabel) },
                    placeholder = { Text("VIPCODE01\nVIPCODE02\nVIPCODE03") },
                    isError = errorText != null,
                    supportingText = {
                        if (errorText != null) {
                            Text(errorText!!, color = MaterialTheme.colorScheme.error)
                        } else if (nonEmptyLineCount > 0) {
                            Text(
                                Strings.batchImportResult(validCount, nonEmptyLineCount - validCount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    },
                    minLines = 5,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    val codes = parseBatchImportCodes(input, existingCodes)
                    if (codes.isEmpty()) {
                        errorText = Strings.batchImportEmpty
                        return@PremiumButton
                    }
                    onConfirm(codes)
                }
            ) {
                Icon(Icons.Outlined.PostAdd, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.batchImport, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

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
