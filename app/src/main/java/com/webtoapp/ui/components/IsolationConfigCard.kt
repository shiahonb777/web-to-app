package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.isolation.*

/**
 * 应用隔离/多开配置卡片
 * 
 * 用于在构建应用时配置独立环境选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsolationConfigCard(
    config: IsolationConfig,
    onConfigChange: (IsolationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.enabled) 
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
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
                        imageVector = if (config.enabled) Icons.Default.Security else Icons.Outlined.Security,
                        contentDescription = null,
                        tint = if (config.enabled) 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = Strings.isolatedEnvironment,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (config.enabled) Strings.antiDetectionEnabled else Strings.notEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { enabled ->
                        onConfigChange(if (enabled) IsolationConfig.STANDARD else IsolationConfig.DISABLED)
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
                        text = Strings.isolationLevel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isBasicConfig(config),
                            onClick = { onConfigChange(IsolationConfig.BASIC) },
                            label = { Text(Strings.basic) },
                            leadingIcon = if (isBasicConfig(config)) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        FilterChip(
                            selected = isStandardConfig(config),
                            onClick = { onConfigChange(IsolationConfig.STANDARD) },
                            label = { Text(Strings.standard) },
                            leadingIcon = if (isStandardConfig(config)) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        FilterChip(
                            selected = isMaximumConfig(config),
                            onClick = { onConfigChange(IsolationConfig.MAXIMUM) },
                            label = { Text(Strings.maximum) },
                            leadingIcon = if (isMaximumConfig(config)) {
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
                            // 指纹防护
                            Text(
                                text = Strings.fingerprintProtection,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            IsolationOption(
                                title = Strings.randomFingerprint,
                                description = Strings.randomFingerprintHint,
                                icon = Icons.Outlined.Fingerprint,
                                checked = config.fingerprintConfig.randomize,
                                onCheckedChange = {
                                    onConfigChange(config.copy(
                                        fingerprintConfig = config.fingerprintConfig.copy(randomize = it)
                                    ))
                                }
                            )
                            
                            IsolationOption(
                                title = Strings.canvasProtection,
                                description = Strings.canvasProtectionHint,
                                icon = Icons.Outlined.Palette,
                                checked = config.protectCanvas,
                                onCheckedChange = { onConfigChange(config.copy(protectCanvas = it)) }
                            )
                            
                            IsolationOption(
                                title = Strings.webglProtection,
                                description = Strings.webglProtectionHint,
                                icon = Icons.Outlined.Brush,
                                checked = config.protectWebGL,
                                onCheckedChange = { onConfigChange(config.copy(protectWebGL = it)) }
                            )
                            
                            IsolationOption(
                                title = Strings.audioProtection,
                                description = Strings.audioProtectionHint,
                                icon = Icons.Outlined.VolumeUp,
                                checked = config.protectAudio,
                                onCheckedChange = { onConfigChange(config.copy(protectAudio = it)) }
                            )
                            
                            IsolationOption(
                                title = Strings.fontProtection,
                                description = Strings.fontProtectionHint,
                                icon = Icons.Outlined.FontDownload,
                                checked = config.protectFonts,
                                onCheckedChange = { onConfigChange(config.copy(protectFonts = it)) }
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Network防护
                            Text(
                                text = Strings.networkProtection,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            IsolationOption(
                                title = Strings.webrtcProtection,
                                description = Strings.webrtcProtectionHint,
                                icon = Icons.Outlined.Wifi,
                                checked = config.blockWebRTC,
                                onCheckedChange = { onConfigChange(config.copy(blockWebRTC = it)) }
                            )
                            
                            IsolationOption(
                                title = Strings.headerSpoofing,
                                description = Strings.headerSpoofingHint,
                                icon = Icons.Outlined.Http,
                                checked = config.headerConfig.enabled,
                                onCheckedChange = {
                                    onConfigChange(config.copy(
                                        headerConfig = config.headerConfig.copy(enabled = it)
                                    ))
                                }
                            )
                            
                            IsolationOption(
                                title = Strings.ipSpoofing,
                                description = Strings.ipSpoofingHint,
                                icon = Icons.Outlined.VpnKey,
                                checked = config.ipSpoofConfig.enabled,
                                onCheckedChange = {
                                    onConfigChange(config.copy(
                                        ipSpoofConfig = config.ipSpoofConfig.copy(enabled = it)
                                    ))
                                }
                            )
                            
                            // IP 范围选择
                            AnimatedVisibility(visible = config.ipSpoofConfig.enabled) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                ) {
                                    Text(
                                        text = Strings.ipRegion,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IpRange.entries.forEach { range ->
                                            FilterChip(
                                                selected = config.ipSpoofConfig.randomIpRange == range,
                                                onClick = {
                                                    onConfigChange(config.copy(
                                                        ipSpoofConfig = config.ipSpoofConfig.copy(randomIpRange = range)
                                                    ))
                                                },
                                                label = { Text(range.displayName, style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                    }
                                    
                                    // Search输入框（仅在选择"搜索"时显示）
                                    AnimatedVisibility(visible = config.ipSpoofConfig.randomIpRange == IpRange.SEARCH) {
                                        Column(
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = config.ipSpoofConfig.searchKeyword ?: "",
                                                onValueChange = { keyword ->
                                                    onConfigChange(config.copy(
                                                        ipSpoofConfig = config.ipSpoofConfig.copy(searchKeyword = keyword)
                                                    ))
                                                },
                                                label = { Text(Strings.countryRegion) },
                                                placeholder = { Text(Strings.countryRegionHint) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Search,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            )
                                            Text(
                                                text = Strings.supportedCountriesHint,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // 高级选项
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.advancedOptions,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                TextButton(
                                    onClick = { showAdvanced = !showAdvanced }
                                ) {
                                    Text(if (showAdvanced) Strings.collapse else Strings.expand)
                                    Icon(
                                        if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            AnimatedVisibility(visible = showAdvanced) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IsolationOption(
                                        title = Strings.storageIsolation,
                                        description = Strings.storageIsolationHint,
                                        icon = Icons.Outlined.Storage,
                                        checked = config.storageIsolation,
                                        onCheckedChange = { onConfigChange(config.copy(storageIsolation = it)) }
                                    )
                                    
                                    IsolationOption(
                                        title = Strings.timezoneSpoofing,
                                        description = Strings.timezoneSpoofingHint,
                                        icon = Icons.Outlined.Schedule,
                                        checked = config.spoofTimezone,
                                        onCheckedChange = { onConfigChange(config.copy(spoofTimezone = it)) }
                                    )
                                    
                                    IsolationOption(
                                        title = Strings.languageSpoofing,
                                        description = Strings.languageSpoofingHint,
                                        icon = Icons.Outlined.Language,
                                        checked = config.spoofLanguage,
                                        onCheckedChange = { onConfigChange(config.copy(spoofLanguage = it)) }
                                    )
                                    
                                    IsolationOption(
                                        title = Strings.resolutionSpoofing,
                                        description = Strings.resolutionSpoofingHint,
                                        icon = Icons.Outlined.AspectRatio,
                                        checked = config.spoofScreen,
                                        onCheckedChange = { onConfigChange(config.copy(spoofScreen = it)) }
                                    )
                                    
                                    IsolationOption(
                                        title = Strings.regenerateOnLaunch,
                                        description = Strings.regenerateOnLaunchHint,
                                        icon = Icons.Outlined.Refresh,
                                        checked = config.fingerprintConfig.regenerateOnLaunch,
                                        onCheckedChange = {
                                            onConfigChange(config.copy(
                                                fingerprintConfig = config.fingerprintConfig.copy(regenerateOnLaunch = it)
                                            ))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // 功能说明
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
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = Strings.isolationDescription,
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

@Composable
private fun IsolationOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (checked) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Column {
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
        }
        
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// 辅助函数：判断是否为预设配置
private fun isBasicConfig(config: IsolationConfig): Boolean {
    return config.enabled &&
            config.storageIsolation &&
            config.blockWebRTC &&
            config.protectCanvas &&
            !config.protectAudio &&
            !config.protectWebGL &&
            !config.protectFonts &&
            !config.headerConfig.enabled &&
            !config.ipSpoofConfig.enabled
}

private fun isStandardConfig(config: IsolationConfig): Boolean {
    return config.enabled &&
            config.fingerprintConfig.randomize &&
            config.headerConfig.enabled &&
            config.storageIsolation &&
            config.blockWebRTC &&
            config.protectCanvas &&
            config.protectAudio &&
            config.protectWebGL &&
            !config.protectFonts &&
            !config.ipSpoofConfig.enabled
}

private fun isMaximumConfig(config: IsolationConfig): Boolean {
    return config.enabled &&
            config.fingerprintConfig.randomize &&
            config.fingerprintConfig.regenerateOnLaunch &&
            config.headerConfig.enabled &&
            config.ipSpoofConfig.enabled &&
            config.storageIsolation &&
            config.blockWebRTC &&
            config.protectCanvas &&
            config.protectAudio &&
            config.protectWebGL &&
            config.protectFonts
}
