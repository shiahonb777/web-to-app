package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.AppHardeningConfig
import androidx.compose.ui.graphics.Color

/**
 * configcard
 * , app config
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardeningConfigCard(
    config: AppHardeningConfig,
    onConfigChange: (AppHardeningConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvanced by remember { mutableStateOf(false) }
    var showDexOptions by remember { mutableStateOf(false) }
    var showSoOptions by remember { mutableStateOf(false) }
    var showAntiReverseOptions by remember { mutableStateOf(false) }
    var showEnvOptions by remember { mutableStateOf(false) }
    var showCodeObfOptions by remember { mutableStateOf(false) }
    var showRaspOptions by remember { mutableStateOf(false) }
    var showAntiTamperOptions by remember { mutableStateOf(false) }
    var showThreatOptions by remember { mutableStateOf(false) }
    
    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (config.enabled) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (config.enabled) Icons.Default.Shield else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (config.enabled)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = AppStringsProvider.current().appHardening,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (config.enabled) AppStringsProvider.current().hardeningEnabled else AppStringsProvider.current().hardeningDisabled,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                PremiumSwitch(
                    checked = config.enabled,
                    onCheckedChange = { enabled ->
                        onConfigChange(config.copy(enabled = enabled))
                    }
                )
            }
            
            // expand config
            AnimatedVisibility(visible = config.enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Note
                    Text(
                        text = AppStringsProvider.current().hardeningLevel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // select
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HardeningLevelChip(
                            label = AppStringsProvider.current().hardeningLevelBasic,
                            selected = config.hardeningLevel == com.webtoapp.data.model.webapp.config.AppHardeningConfig.HardeningLevel.BASIC,
                            onClick = { onConfigChange(AppHardeningConfig.BASIC) }
                        )
                        HardeningLevelChip(
                            label = AppStringsProvider.current().hardeningLevelStandard,
                            selected = config.hardeningLevel == com.webtoapp.data.model.webapp.config.AppHardeningConfig.HardeningLevel.STANDARD,
                            onClick = { onConfigChange(AppHardeningConfig.STANDARD) }
                        )
                        HardeningLevelChip(
                            label = AppStringsProvider.current().hardeningLevelAdvanced,
                            selected = config.hardeningLevel == com.webtoapp.data.model.webapp.config.AppHardeningConfig.HardeningLevel.ADVANCED,
                            onClick = { onConfigChange(AppHardeningConfig.ADVANCED) }
                        )
                        HardeningLevelChip(
                            label = AppStringsProvider.current().hardeningLevelFortress,
                            selected = config.hardeningLevel == com.webtoapp.data.model.webapp.config.AppHardeningConfig.HardeningLevel.FORTRESS,
                            onClick = { onConfigChange(AppHardeningConfig.FORTRESS) }
                        )
                    }
                    
                    // Note
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = config.hardeningLevel.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Note
                    val layerCount = countProtectionLayers(config)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${AppStringsProvider.current().hardeningProtectionLayers}: $layerCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        TextButton(
                            onClick = { showAdvanced = !showAdvanced }
                        ) {
                            Text(
                                if (showAdvanced) AppStringsProvider.current().collapse else AppStringsProvider.current().advancedSettings,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Icon(
                                if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // ==================== advancedsettingsexpand ====================
                    AnimatedVisibility(visible = showAdvanced) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // ===== DEX =====
                            HardeningSectionHeader(
                                title = AppStringsProvider.current().dexProtection,
                                icon = Icons.Default.Code,
                                expanded = showDexOptions,
                                onToggle = { showDexOptions = !showDexOptions }
                            )
                            AnimatedVisibility(visible = showDexOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    HardeningOption(
                                        title = AppStringsProvider.current().dexEncryption,
                                        description = AppStringsProvider.current().dexEncryptionHint,
                                        checked = config.dexEncryption,
                                        onCheckedChange = { onConfigChange(config.copy(dexEncryption = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().dexSplitting,
                                        description = AppStringsProvider.current().dexSplittingHint,
                                        checked = config.dexSplitting,
                                        onCheckedChange = { onConfigChange(config.copy(dexSplitting = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().dexVmp,
                                        description = AppStringsProvider.current().dexVmpHint,
                                        checked = config.dexVmp,
                                        onCheckedChange = { onConfigChange(config.copy(dexVmp = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().dexControlFlow,
                                        description = AppStringsProvider.current().dexControlFlowHint,
                                        checked = config.dexControlFlowFlattening,
                                        onCheckedChange = { onConfigChange(config.copy(dexControlFlowFlattening = it)) }
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // ===== Native SO =====
                            HardeningSectionHeader(
                                title = AppStringsProvider.current().soProtection,
                                icon = Icons.Default.Memory,
                                expanded = showSoOptions,
                                onToggle = { showSoOptions = !showSoOptions }
                            )
                            AnimatedVisibility(visible = showSoOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    HardeningOption(
                                        title = AppStringsProvider.current().soEncryption,
                                        description = AppStringsProvider.current().soEncryptionHint,
                                        checked = config.soEncryption,
                                        onCheckedChange = { onConfigChange(config.copy(soEncryption = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().soElfObfuscation,
                                        description = AppStringsProvider.current().soElfObfuscationHint,
                                        checked = config.soElfObfuscation,
                                        onCheckedChange = { onConfigChange(config.copy(soElfObfuscation = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().soSymbolStrip,
                                        description = AppStringsProvider.current().soSymbolStripHint,
                                        checked = config.soSymbolStrip,
                                        onCheckedChange = { onConfigChange(config.copy(soSymbolStrip = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().soAntiDump,
                                        description = AppStringsProvider.current().soAntiDumpHint,
                                        checked = config.soAntiDump,
                                        onCheckedChange = { onConfigChange(config.copy(soAntiDump = it)) }
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // Note
                            HardeningSectionHeader(
                                title = AppStringsProvider.current().antiReverse,
                                icon = Icons.Default.Security,
                                expanded = showAntiReverseOptions,
                                onToggle = { showAntiReverseOptions = !showAntiReverseOptions }
                            )
                            AnimatedVisibility(visible = showAntiReverseOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    HardeningOption(
                                        title = AppStringsProvider.current().antiDebugMultiLayer,
                                        description = AppStringsProvider.current().antiDebugMultiLayerHint,
                                        checked = config.antiDebugMultiLayer,
                                        onCheckedChange = { onConfigChange(config.copy(antiDebugMultiLayer = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().antiFridaAdvanced,
                                        description = AppStringsProvider.current().antiFridaAdvancedHint,
                                        checked = config.antiFridaAdvanced,
                                        onCheckedChange = { onConfigChange(config.copy(antiFridaAdvanced = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().antiXposedDeep,
                                        description = AppStringsProvider.current().antiXposedDeepHint,
                                        checked = config.antiXposedDeep,
                                        onCheckedChange = { onConfigChange(config.copy(antiXposedDeep = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().antiMagiskDetect,
                                        description = AppStringsProvider.current().antiMagiskDetectHint,
                                        checked = config.antiMagiskDetect,
                                        onCheckedChange = { onConfigChange(config.copy(antiMagiskDetect = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().antiMemoryDump,
                                        description = AppStringsProvider.current().antiMemoryDumpHint,
                                        checked = config.antiMemoryDump,
                                        onCheckedChange = { onConfigChange(config.copy(antiMemoryDump = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().antiScreenCapture,
                                        description = AppStringsProvider.current().antiScreenCaptureHint,
                                        checked = config.antiScreenCapture,
                                        onCheckedChange = { onConfigChange(config.copy(antiScreenCapture = it)) }
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // Note
                            HardeningSectionHeader(
                                title = AppStringsProvider.current().environmentDetection,
                                icon = Icons.Default.PhoneAndroid,
                                expanded = showEnvOptions,
                                onToggle = { showEnvOptions = !showEnvOptions }
                            )
                            AnimatedVisibility(visible = showEnvOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    HardeningOption(
                                        title = AppStringsProvider.current().detectEmulatorAdvanced,
                                        description = AppStringsProvider.current().detectEmulatorAdvancedHint,
                                        checked = config.detectEmulatorAdvanced,
                                        onCheckedChange = { onConfigChange(config.copy(detectEmulatorAdvanced = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().detectVirtualApp,
                                        description = AppStringsProvider.current().detectVirtualAppHint,
                                        checked = config.detectVirtualApp,
                                        onCheckedChange = { onConfigChange(config.copy(detectVirtualApp = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().detectUSBDebugging,
                                        description = AppStringsProvider.current().detectUSBDebuggingHint,
                                        checked = config.detectUSBDebugging,
                                        onCheckedChange = { onConfigChange(config.copy(detectUSBDebugging = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().detectVPN,
                                        description = AppStringsProvider.current().detectVPNHint,
                                        checked = config.detectVPN,
                                        onCheckedChange = { onConfigChange(config.copy(detectVPN = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().detectDeveloperOptions,
                                        description = AppStringsProvider.current().detectDeveloperOptionsHint,
                                        checked = config.detectDeveloperOptions,
                                        onCheckedChange = { onConfigChange(config.copy(detectDeveloperOptions = it)) }
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // ===== code =====
                            HardeningSectionHeader(
                                title = AppStringsProvider.current().codeObfuscation,
                                icon = Icons.Default.VisibilityOff,
                                expanded = showCodeObfOptions,
                                onToggle = { showCodeObfOptions = !showCodeObfOptions }
                            )
                            AnimatedVisibility(visible = showCodeObfOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    HardeningOption(
                                        title = AppStringsProvider.current().stringEncryption,
                                        description = AppStringsProvider.current().stringEncryptionHint,
                                        checked = config.stringEncryption,
                                        onCheckedChange = { onConfigChange(config.copy(stringEncryption = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().classNameObfuscation,
                                        description = AppStringsProvider.current().classNameObfuscationHint,
                                        checked = config.classNameObfuscation,
                                        onCheckedChange = { onConfigChange(config.copy(classNameObfuscation = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().callIndirection,
                                        description = AppStringsProvider.current().callIndirectionHint,
                                        checked = config.callIndirection,
                                        onCheckedChange = { onConfigChange(config.copy(callIndirection = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().opaquePredicates,
                                        description = AppStringsProvider.current().opaquePredicatesHint,
                                        checked = config.opaquePredicates,
                                        onCheckedChange = { onConfigChange(config.copy(opaquePredicates = it)) }
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // ===== run( RASP) =====
                            HardeningSectionHeader(
                                title = AppStringsProvider.current().raspProtection,
                                icon = Icons.Default.VerifiedUser,
                                expanded = showRaspOptions,
                                onToggle = { showRaspOptions = !showRaspOptions }
                            )
                            AnimatedVisibility(visible = showRaspOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    HardeningOption(
                                        title = AppStringsProvider.current().dexCrcVerify,
                                        description = AppStringsProvider.current().dexCrcVerifyHint,
                                        checked = config.dexCrcVerify,
                                        onCheckedChange = { onConfigChange(config.copy(dexCrcVerify = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().memoryIntegrity,
                                        description = AppStringsProvider.current().memoryIntegrityHint,
                                        checked = config.memoryIntegrity,
                                        onCheckedChange = { onConfigChange(config.copy(memoryIntegrity = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().jniCallValidation,
                                        description = AppStringsProvider.current().jniCallValidationHint,
                                        checked = config.jniCallValidation,
                                        onCheckedChange = { onConfigChange(config.copy(jniCallValidation = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().timingCheck,
                                        description = AppStringsProvider.current().timingCheckHint,
                                        checked = config.timingCheck,
                                        onCheckedChange = { onConfigChange(config.copy(timingCheck = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().stackTraceFilter,
                                        description = AppStringsProvider.current().stackTraceFilterHint,
                                        checked = config.stackTraceFilter,
                                        onCheckedChange = { onConfigChange(config.copy(stackTraceFilter = it)) }
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // Note
                            HardeningSectionHeader(
                                title = AppStringsProvider.current().antiTamper,
                                icon = Icons.Default.Fingerprint,
                                expanded = showAntiTamperOptions,
                                onToggle = { showAntiTamperOptions = !showAntiTamperOptions }
                            )
                            AnimatedVisibility(visible = showAntiTamperOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    HardeningOption(
                                        title = AppStringsProvider.current().multiPointSignature,
                                        description = AppStringsProvider.current().multiPointSignatureHint,
                                        checked = config.multiPointSignatureVerify,
                                        onCheckedChange = { onConfigChange(config.copy(multiPointSignatureVerify = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().apkChecksum,
                                        description = AppStringsProvider.current().apkChecksumHint,
                                        checked = config.apkChecksumValidation,
                                        onCheckedChange = { onConfigChange(config.copy(apkChecksumValidation = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().resourceIntegrity,
                                        description = AppStringsProvider.current().resourceIntegrityHint,
                                        checked = config.resourceIntegrity,
                                        onCheckedChange = { onConfigChange(config.copy(resourceIntegrity = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().certificatePinning,
                                        description = AppStringsProvider.current().certificatePinningHint,
                                        checked = config.certificatePinning,
                                        onCheckedChange = { onConfigChange(config.copy(certificatePinning = it)) }
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // Note
                            HardeningSectionHeader(
                                title = AppStringsProvider.current().threatResponse,
                                icon = Icons.Default.Warning,
                                expanded = showThreatOptions,
                                onToggle = { showThreatOptions = !showThreatOptions }
                            )
                            AnimatedVisibility(visible = showThreatOptions) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // select
                                    Text(
                                        text = AppStringsProvider.current().threatResponse,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    
                                    ThreatResponseSelector(
                                        selectedResponse = config.responseStrategy,
                                        onResponseChange = { onConfigChange(config.copy(responseStrategy = it)) }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Note
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                            Text(
                                                text = AppStringsProvider.current().responseDelay,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = AppStringsProvider.current().responseDelayHint,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${config.responseDelay}s",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Slider(
                                        value = config.responseDelay.toFloat(),
                                        onValueChange = { onConfigChange(config.copy(responseDelay = it.toInt())) },
                                        valueRange = 0f..30f,
                                        steps = 5
                                    )
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    HardeningOption(
                                        title = AppStringsProvider.current().enableHoneypot,
                                        description = AppStringsProvider.current().enableHoneypotHint,
                                        checked = config.enableHoneypot,
                                        onCheckedChange = { onConfigChange(config.copy(enableHoneypot = it)) }
                                    )
                                    HardeningOption(
                                        title = AppStringsProvider.current().enableSelfDestruct,
                                        description = AppStringsProvider.current().enableSelfDestructHint,
                                        checked = config.enableSelfDestruct,
                                        onCheckedChange = { onConfigChange(config.copy(enableSelfDestruct = it)) },
                                        isDangerous = true
                                    )
                                }
                            }
                        }
                    }
                    
                    // Note
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = AppStringsProvider.current().appHardeningDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * select
 */
@Composable
private fun HardeningLevelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    PremiumFilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
        } else null
    )
}

/**
 * Note
 */
@Composable
private fun HardeningSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Note
 */
@Composable
private fun HardeningOption(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDangerous: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = if (isDangerous) CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.error,
                checkmarkColor = MaterialTheme.colorScheme.onError
            ) else CheckboxDefaults.colors()
        )
    }
}

/**
 * select
 */
@Composable
private fun ThreatResponseSelector(
    selectedResponse: com.webtoapp.data.model.webapp.config.AppHardeningConfig.ThreatResponse,
    onResponseChange: (com.webtoapp.data.model.webapp.config.AppHardeningConfig.ThreatResponse) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        com.webtoapp.data.model.webapp.config.AppHardeningConfig.ThreatResponse.entries.forEach { response ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedResponse == response,
                    onClick = { onResponseChange(response) }
                )
                Text(
                    text = response.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp),
                    color = if (response == com.webtoapp.data.model.webapp.config.AppHardeningConfig.ThreatResponse.DATA_WIPE ||
                        response == com.webtoapp.data.model.webapp.config.AppHardeningConfig.ThreatResponse.CRASH_RANDOM)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * current
 */
private fun countProtectionLayers(config: AppHardeningConfig): Int {
    var count = 0
    // DEX
    if (config.dexEncryption) count++
    if (config.dexSplitting) count++
    if (config.dexVmp) count++
    if (config.dexControlFlowFlattening) count++
    // SO
    if (config.soEncryption) count++
    if (config.soElfObfuscation) count++
    if (config.soSymbolStrip) count++
    if (config.soAntiDump) count++
    // Anti-reverse
    if (config.antiDebugMultiLayer) count++
    if (config.antiFridaAdvanced) count++
    if (config.antiXposedDeep) count++
    if (config.antiMagiskDetect) count++
    if (config.antiMemoryDump) count++
    if (config.antiScreenCapture) count++
    // Environment
    if (config.detectEmulatorAdvanced) count++
    if (config.detectVirtualApp) count++
    if (config.detectUSBDebugging) count++
    if (config.detectVPN) count++
    if (config.detectDeveloperOptions) count++
    // Code obfuscation
    if (config.stringEncryption) count++
    if (config.classNameObfuscation) count++
    if (config.callIndirection) count++
    if (config.opaquePredicates) count++
    // RASP
    if (config.dexCrcVerify) count++
    if (config.memoryIntegrity) count++
    if (config.jniCallValidation) count++
    if (config.timingCheck) count++
    if (config.stackTraceFilter) count++
    // Anti-tamper
    if (config.multiPointSignatureVerify) count++
    if (config.apkChecksumValidation) count++
    if (config.resourceIntegrity) count++
    if (config.certificatePinning) count++
    // Threat response extras
    if (config.enableHoneypot) count++
    if (config.enableSelfDestruct) count++
    return count
}
