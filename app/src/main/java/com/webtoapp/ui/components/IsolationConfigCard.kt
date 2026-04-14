package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.isolation.*
import androidx.compose.ui.graphics.Color

/**
 * app / configcard
 * 
 * for appconfig
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IsolationConfigCard(
    config: IsolationConfig,
    onConfigChange: (IsolationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    
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
                            imageVector = if (config.enabled) Icons.Default.Security else Icons.Outlined.Security,
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
                            text = AppStringsProvider.current().isolatedEnvironment,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (config.enabled) AppStringsProvider.current().antiDetectionEnabled else AppStringsProvider.current().notEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                PremiumSwitch(
                    checked = config.enabled,
                    onCheckedChange = { enabled ->
                        onConfigChange(if (enabled) IsolationConfig.STANDARD else IsolationConfig.DISABLED)
                    }
                )
            }
            
            // Expand config
            AnimatedVisibility(visible = config.enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Note
                    Text(
                        text = AppStringsProvider.current().isolationLevel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFilterChip(
                            selected = isBasicConfig(config),
                            onClick = { onConfigChange(IsolationConfig.BASIC) },
                            label = { Text(AppStringsProvider.current().basic) },
                            leadingIcon = if (isBasicConfig(config)) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        PremiumFilterChip(
                            selected = isStandardConfig(config),
                            onClick = { onConfigChange(IsolationConfig.STANDARD) },
                            label = { Text(AppStringsProvider.current().standard) },
                            leadingIcon = if (isStandardConfig(config)) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        PremiumFilterChip(
                            selected = isMaximumConfig(config),
                            onClick = { onConfigChange(IsolationConfig.MAXIMUM) },
                            label = { Text(AppStringsProvider.current().maximum) },
                            leadingIcon = if (isMaximumConfig(config)) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        PremiumFilterChip(
                            selected = expanded,
                            onClick = { expanded = !expanded },
                            label = { Text(AppStringsProvider.current().custom) },
                            leadingIcon = {
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                    
                    // Customconfig
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Note
                            Text(
                                text = AppStringsProvider.current().fingerprintProtection,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            IsolationOption(
                                title = AppStringsProvider.current().randomFingerprint,
                                description = AppStringsProvider.current().randomFingerprintHint,
                                icon = Icons.Outlined.Fingerprint,
                                checked = config.fingerprintConfig.randomize,
                                onCheckedChange = {
                                    onConfigChange(config.copy(
                                        fingerprintConfig = config.fingerprintConfig.copy(randomize = it)
                                    ))
                                }
                            )
                            
                            IsolationOption(
                                title = AppStringsProvider.current().canvasProtection,
                                description = AppStringsProvider.current().canvasProtectionHint,
                                icon = Icons.Outlined.Palette,
                                checked = config.protectCanvas,
                                onCheckedChange = { onConfigChange(config.copy(protectCanvas = it)) }
                            )
                            
                            IsolationOption(
                                title = AppStringsProvider.current().webglProtection,
                                description = AppStringsProvider.current().webglProtectionHint,
                                icon = Icons.Outlined.Brush,
                                checked = config.protectWebGL,
                                onCheckedChange = { onConfigChange(config.copy(protectWebGL = it)) }
                            )
                            
                            IsolationOption(
                                title = AppStringsProvider.current().audioProtection,
                                description = AppStringsProvider.current().audioProtectionHint,
                                icon = Icons.Outlined.VolumeUp,
                                checked = config.protectAudio,
                                onCheckedChange = { onConfigChange(config.copy(protectAudio = it)) }
                            )
                            
                            IsolationOption(
                                title = AppStringsProvider.current().fontProtection,
                                description = AppStringsProvider.current().fontProtectionHint,
                                icon = Icons.Outlined.FontDownload,
                                checked = config.protectFonts,
                                onCheckedChange = { onConfigChange(config.copy(protectFonts = it)) }
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Network
                            Text(
                                text = AppStringsProvider.current().networkProtection,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            IsolationOption(
                                title = AppStringsProvider.current().webrtcProtection,
                                description = AppStringsProvider.current().webrtcProtectionHint,
                                icon = Icons.Outlined.Wifi,
                                checked = config.blockWebRTC,
                                onCheckedChange = { onConfigChange(config.copy(blockWebRTC = it)) }
                            )
                            
                            IsolationOption(
                                title = AppStringsProvider.current().headerSpoofing,
                                description = AppStringsProvider.current().headerSpoofingHint,
                                icon = Icons.Outlined.Http,
                                checked = config.headerConfig.enabled,
                                onCheckedChange = {
                                    onConfigChange(config.copy(
                                        headerConfig = config.headerConfig.copy(enabled = it)
                                    ))
                                }
                            )
                            
                            IsolationOption(
                                title = AppStringsProvider.current().ipSpoofing,
                                description = AppStringsProvider.current().ipSpoofingHint,
                                icon = Icons.Outlined.VpnKey,
                                checked = config.ipSpoofConfig.enabled,
                                onCheckedChange = {
                                    onConfigChange(config.copy(
                                        ipSpoofConfig = config.ipSpoofConfig.copy(enabled = it)
                                    ))
                                }
                            )
                            
                            // IP select
                            AnimatedVisibility(visible = config.ipSpoofConfig.enabled) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                ) {
                                    Text(
                                        text = AppStringsProvider.current().ipRegion,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IpRange.entries.forEach { range ->
                                            PremiumFilterChip(
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
                                    
                                    // Searchinput( only select" "display)
                                    AnimatedVisibility(visible = config.ipSpoofConfig.randomIpRange == IpRange.SEARCH) {
                                        Column(
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            PremiumTextField(
                                                value = config.ipSpoofConfig.searchKeyword ?: "",
                                                onValueChange = { keyword ->
                                                    onConfigChange(config.copy(
                                                        ipSpoofConfig = config.ipSpoofConfig.copy(searchKeyword = keyword)
                                                    ))
                                                },
                                                label = { Text(AppStringsProvider.current().countryRegion) },
                                                placeholder = { Text(AppStringsProvider.current().countryRegionHint) },
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
                                                text = AppStringsProvider.current().supportedCountriesHint,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // advanced
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStringsProvider.current().advancedOptions,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                TextButton(
                                    onClick = { showAdvanced = !showAdvanced }
                                ) {
                                    Text(if (showAdvanced) AppStringsProvider.current().collapse else AppStringsProvider.current().expand)
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
                                        title = AppStringsProvider.current().storageIsolation,
                                        description = AppStringsProvider.current().storageIsolationHint,
                                        icon = Icons.Outlined.Storage,
                                        checked = config.storageIsolation,
                                        onCheckedChange = { onConfigChange(config.copy(storageIsolation = it)) }
                                    )
                                    
                                    IsolationOption(
                                        title = AppStringsProvider.current().timezoneSpoofing,
                                        description = AppStringsProvider.current().timezoneSpoofingHint,
                                        icon = Icons.Outlined.Schedule,
                                        checked = config.spoofTimezone,
                                        onCheckedChange = { onConfigChange(config.copy(spoofTimezone = it)) }
                                    )
                                    
                                    IsolationOption(
                                        title = AppStringsProvider.current().languageSpoofing,
                                        description = AppStringsProvider.current().languageSpoofingHint,
                                        icon = Icons.Outlined.Language,
                                        checked = config.spoofLanguage,
                                        onCheckedChange = { onConfigChange(config.copy(spoofLanguage = it)) }
                                    )
                                    
                                    IsolationOption(
                                        title = AppStringsProvider.current().resolutionSpoofing,
                                        description = AppStringsProvider.current().resolutionSpoofingHint,
                                        icon = Icons.Outlined.AspectRatio,
                                        checked = config.spoofScreen,
                                        onCheckedChange = { onConfigChange(config.copy(spoofScreen = it)) }
                                    )
                                    
                                    IsolationOption(
                                        title = AppStringsProvider.current().regenerateOnLaunch,
                                        description = AppStringsProvider.current().regenerateOnLaunchHint,
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
                    
                    // Note
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
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
                                text = AppStringsProvider.current().isolationDescription,
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
            modifier = Modifier.weight(weight = 1f, fill = true),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (checked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        else if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (checked)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
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

// config
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
