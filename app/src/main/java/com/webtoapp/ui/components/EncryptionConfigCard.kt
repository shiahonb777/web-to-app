package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.ApkEncryptionConfig

/**
 * Encryption config card
 * 用于在创建应用时配置 APK 加密选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptionConfigCard(
    config: ApkEncryptionConfig,
    onConfigChange: (ApkEncryptionConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showSecurityOptions by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.enabled) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (config.enabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (config.enabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = Strings.resourceEncryption,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (config.enabled) Strings.encryptionEnabled else Strings.notEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { enabled ->
                        onConfigChange(config.copy(enabled = enabled))
                    }
                )
            }
            
            // Expand详细配置
            AnimatedVisibility(visible = config.enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 快捷预设
                    Text(
                        text = Strings.encryptionLevel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = config == ApkEncryptionConfig.BASIC.copy(enabled = true),
                            onClick = {
                                onConfigChange(ApkEncryptionConfig.BASIC)
                            },
                            label = { Text(Strings.basic) },
                            leadingIcon = if (config == ApkEncryptionConfig.BASIC.copy(enabled = true)) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        FilterChip(
                            selected = config == ApkEncryptionConfig.FULL,
                            onClick = {
                                onConfigChange(ApkEncryptionConfig.FULL)
                            },
                            label = { Text(Strings.full) },
                            leadingIcon = if (config == ApkEncryptionConfig.FULL) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        FilterChip(
                            selected = config == ApkEncryptionConfig.MAXIMUM,
                            onClick = {
                                onConfigChange(ApkEncryptionConfig.MAXIMUM)
                            },
                            label = { Text(Strings.maximum) },
                            leadingIcon = if (config == ApkEncryptionConfig.MAXIMUM) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        FilterChip(
                            selected = expanded,
                            onClick = { expanded = !expanded },
                            label = { Text(Strings.custom) },
                            leadingIcon = {
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                    
                    // Custom配置
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Resource加密选项
                            Text(
                                text = Strings.resourceEncryption,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            EncryptionOption(
                                title = Strings.configFileEncryption,
                                description = Strings.configFileEncryptionHint,
                                checked = config.encryptConfig,
                                onCheckedChange = { onConfigChange(config.copy(encryptConfig = it)) }
                            )
                            
                            EncryptionOption(
                                title = Strings.htmlCssJsEncryption,
                                description = Strings.htmlCssJsEncryptionHint,
                                checked = config.encryptHtml,
                                onCheckedChange = { onConfigChange(config.copy(encryptHtml = it)) }
                            )
                            
                            EncryptionOption(
                                title = Strings.mediaFileEncryption,
                                description = Strings.mediaFileEncryptionHint,
                                checked = config.encryptMedia,
                                onCheckedChange = { onConfigChange(config.copy(encryptMedia = it)) }
                            )
                            
                            EncryptionOption(
                                title = Strings.splashEncryption,
                                description = Strings.splashEncryptionHint,
                                checked = config.encryptSplash,
                                onCheckedChange = { onConfigChange(config.copy(encryptSplash = it)) }
                            )
                            
                            EncryptionOption(
                                title = Strings.bgmEncryption,
                                description = Strings.bgmEncryptionHint,
                                checked = config.encryptBgm,
                                onCheckedChange = { onConfigChange(config.copy(encryptBgm = it)) }
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Encryption强度选择
                            Text(
                                text = Strings.encryptionStrength,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            EncryptionLevelSelector(
                                selectedLevel = config.encryptionLevel,
                                onLevelChange = { onConfigChange(config.copy(encryptionLevel = it)) }
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Security保护选项
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.securityProtection,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { showSecurityOptions = !showSecurityOptions }
                                ) {
                                    Text(if (showSecurityOptions) Strings.collapse else Strings.expand)
                                    Icon(
                                        if (showSecurityOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            AnimatedVisibility(visible = showSecurityOptions) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    EncryptionOption(
                                        title = Strings.integrityCheck,
                                        description = Strings.integrityCheckHint,
                                        checked = config.enableIntegrityCheck,
                                        onCheckedChange = { onConfigChange(config.copy(enableIntegrityCheck = it)) }
                                    )
                                    
                                    EncryptionOption(
                                        title = Strings.antiDebugProtection,
                                        description = Strings.antiDebugProtectionHint,
                                        checked = config.enableAntiDebug,
                                        onCheckedChange = { onConfigChange(config.copy(enableAntiDebug = it)) }
                                    )
                                    
                                    EncryptionOption(
                                        title = Strings.antiTamperProtection,
                                        description = Strings.antiTamperProtectionHint,
                                        checked = config.enableAntiTamper,
                                        onCheckedChange = { onConfigChange(config.copy(enableAntiTamper = it)) }
                                    )
                                    
                                    EncryptionOption(
                                        title = Strings.stringObfuscation,
                                        description = Strings.stringObfuscationHint,
                                        checked = config.obfuscateStrings,
                                        onCheckedChange = { onConfigChange(config.copy(obfuscateStrings = it)) }
                                    )
                                    
                                    // Security保护说明
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = Strings.securityWarning,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Encryption说明
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = Strings.encryptionDescription,
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
 * 加密强度选择器
 */
@Composable
private fun EncryptionLevelSelector(
    selectedLevel: ApkEncryptionConfig.EncryptionLevel,
    onLevelChange: (ApkEncryptionConfig.EncryptionLevel) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ApkEncryptionConfig.EncryptionLevel.entries.forEach { level ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedLevel == level,
                    onClick = { onLevelChange(level) }
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = level.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${Strings.pbkdf2Iterations}: ${level.iterations}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EncryptionOption(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
