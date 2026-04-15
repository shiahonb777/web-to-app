package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webtoapp.core.disguise.DisguiseConfig
import com.webtoapp.core.disguise.DisguiseConfig.IconStormMode
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * app configcard v2. 0
 * 
 * ICON STORM
 * support icon
 * modeselect
 * Note
 * mode/
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisguiseConfigCard(
    config: DisguiseConfig?,
    onConfigChange: (DisguiseConfig?) -> Unit
) {
    var expanded by remember { mutableStateOf(config?.enabled == true) }
    var enabled by remember(config) { mutableStateOf(config?.enabled ?: false) }
    
    // iconconfig
    var multiLauncherIcons by remember(config) { mutableIntStateOf(config?.multiLauncherIcons ?: 1) }
    var iconStormMode by remember(config) { mutableStateOf(config?.iconStormMode ?: IconStormMode.NORMAL) }
    var randomizeNames by remember(config) { mutableStateOf(config?.randomizeNames ?: false) }
    var customNamePrefix by remember(config) { mutableStateOf(config?.customNamePrefix ?: "") }
    
    fun updateConfig() {
        if (!enabled) {
            onConfigChange(null)
        } else {
            onConfigChange(DisguiseConfig(
                enabled = enabled,
                multiLauncherIcons = if (enabled) multiLauncherIcons else 1,
                iconStormMode = iconStormMode,
                randomizeNames = randomizeNames,
                customNamePrefix = customNamePrefix
            ))
        }
    }
    
    // color
    val impactLevel = DisguiseConfig.assessImpactLevel(multiLauncherIcons)
    val impactColor by animateColorAsState(
        targetValue = when (impactLevel) {
            0 -> MaterialTheme.colorScheme.primary
            1 -> Color(0xFF4CAF50)
            2 -> Color(0xFFFFA726)
            3 -> Color(0xFFFF7043)
            4 -> Color(0xFFE53935)
            else -> Color(0xFFD500F9)
        },
        label = "impactColor"
    )
    
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
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
                                Icons.Outlined.AppShortcut,
                                contentDescription = null,
                                tint = if (enabled) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                        Text(
                            AppStringsProvider.current().disguiseMultiIconTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (enabled && multiLauncherIcons > 1) {
                            Text(
                                "${multiLauncherIcons} ${AppStringsProvider.current().iconStormIcons} · ${AppStringsProvider.current().iconStormImpactPrefix}${
                                    when (impactLevel) {
                                        0 -> AppStringsProvider.current().iconStormImpactNone
                                        1 -> AppStringsProvider.current().iconStormImpactLight
                                        2 -> AppStringsProvider.current().iconStormImpactMedium
                                        3 -> AppStringsProvider.current().iconStormImpactHeavy
                                        4 -> AppStringsProvider.current().iconStormImpactExtreme
                                        else -> AppStringsProvider.current().iconStormImpactDangerous
                                    }
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = impactColor
                            )
                        } else if (!enabled) {
                            Text(
                                AppStringsProvider.current().notEnabled,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Enable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                AppStringsProvider.current().disguiseEnableMultiIcon,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                AppStringsProvider.current().disguiseEnableMultiIconDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                if (it && multiLauncherIcons < 2) {
                                    multiLauncherIcons = 2
                                }
                                updateConfig()
                            }
                        )
                    }
                    
                    AnimatedVisibility(visible = enabled) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // ===== ICON STORM modeselect =====
                            Text(
                                AppStringsProvider.current().iconStormMode,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // mode
                            val modes = IconStormMode.entries.toList()
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Normal, Subtle, Flood
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    modes.take(3).forEach { mode ->
                                        IconStormModeChip(
                                            mode = mode,
                                            isSelected = iconStormMode == mode,
                                            impactColor = when (DisguiseConfig.assessImpactLevel(mode.suggestedCount.coerceAtLeast(2))) {
                                                0 -> MaterialTheme.colorScheme.primary
                                                1 -> Color(0xFF4CAF50)
                                                2 -> Color(0xFFFFA726)
                                                3 -> Color(0xFFFF7043)
                                                4 -> Color(0xFFE53935)
                                                else -> Color(0xFFD500F9)
                                            },
                                            onClick = {
                                                iconStormMode = mode
                                                multiLauncherIcons = if (mode == IconStormMode.NORMAL) 2 else mode.suggestedCount
                                                updateConfig()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                // Storm, Extreme, Research, Custom
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    modes.drop(3).forEach { mode ->
                                        IconStormModeChip(
                                            mode = mode,
                                            isSelected = iconStormMode == mode,
                                            impactColor = when (DisguiseConfig.assessImpactLevel(
                                                if (mode == IconStormMode.CUSTOM) multiLauncherIcons
                                                else mode.suggestedCount.coerceAtLeast(2)
                                            )) {
                                                0 -> MaterialTheme.colorScheme.primary
                                                1 -> Color(0xFF4CAF50)
                                                2 -> Color(0xFFFFA726)
                                                3 -> Color(0xFFFF7043)
                                                4 -> Color(0xFFE53935)
                                                else -> Color(0xFFD500F9)
                                            },
                                            onClick = {
                                                iconStormMode = mode
                                                if (mode != IconStormMode.CUSTOM) {
                                                    multiLauncherIcons = if (mode == IconStormMode.NORMAL) 2 else mode.suggestedCount
                                                }
                                                updateConfig()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // ===== input( only Custom modeoradvancedmodedisplay) =====
                            Text(
                                AppStringsProvider.current().disguiseIconCountTitle,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                AppStringsProvider.current().iconStormNoLimit,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    AppStringsProvider.current().disguiseCountLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(60.dp)
                                )
                                PremiumTextField(
                                    value = multiLauncherIcons.toString(),
                                    onValueChange = { value ->
                                        val num = value.filter { it.isDigit() }.toIntOrNull() ?: 2
                                        // 🔥 v2. 0
                                        multiLauncherIcons = num.coerceAtLeast(2)
                                        iconStormMode = when {
                                            num <= 10 -> IconStormMode.NORMAL
                                            num <= 50 -> IconStormMode.SUBTLE
                                            num <= 200 -> IconStormMode.FLOOD
                                            num <= 500 -> IconStormMode.STORM
                                            num <= 2000 -> IconStormMode.EXTREME
                                            num <= 5000 -> IconStormMode.RESEARCH
                                            else -> IconStormMode.CUSTOM
                                        }
                                        updateConfig()
                                    },
                                    modifier = Modifier.weight(weight = 1f, fill = true),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    supportingText = { Text(AppStringsProvider.current().iconStormUnlimited) }
                                )
                            }
                            
                            // Note
                            Spacer(modifier = Modifier.height(12.dp))
                            ImpactDashboard(
                                count = multiLauncherIcons,
                                impactLevel = impactLevel,
                                impactColor = impactColor
                            )
                            
                            // ===== advanced =====
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Note
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(
                                        AppStringsProvider.current().iconStormRandomNames,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        AppStringsProvider.current().iconStormRandomNamesDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PremiumSwitch(
                                    checked = randomizeNames,
                                    onCheckedChange = {
                                        randomizeNames = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            // ( when)
                            AnimatedVisibility(visible = !randomizeNames) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    PremiumTextField(
                                        value = customNamePrefix,
                                        onValueChange = {
                                            customNamePrefix = it
                                            updateConfig()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(AppStringsProvider.current().iconStormNamePrefix) },
                                        supportingText = { Text(AppStringsProvider.current().iconStormNamePrefixHint) },
                                        singleLine = true
                                    )
                                }
                            }
                            
                            // Note
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Outlined.Lightbulb,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        AppStringsProvider.current().iconStormTip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            
                            // ===== warning( >= 200 icondisplay) =====
                            AnimatedVisibility(visible = impactLevel >= 3) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0x33E53935),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.Outlined.Warning,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(0xFFE53935)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                AppStringsProvider.current().iconStormWarning,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFE53935)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Icon Storm modeselect
 */
@Composable
private fun IconStormModeChip(
    mode: IconStormMode,
    isSelected: Boolean,
    impactColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        impactColor.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, impactColor.copy(alpha = 0.6f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                mode.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) impactColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (mode.suggestedCount > 0) {
                Text(
                    "${mode.suggestedCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) impactColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Note
 */
@Composable
private fun ImpactDashboard(
    count: Int,
    impactLevel: Int,
    impactColor: Color
) {
    val estimatedOverhead = DisguiseConfig.estimateManifestOverhead(count)
    val overheadStr = when {
        estimatedOverhead < 1024 -> "${estimatedOverhead} B"
        estimatedOverhead < 1024 * 1024 -> "${estimatedOverhead / 1024} KB"
        else -> "${"%.1f".format(estimatedOverhead.toDouble() / 1024 / 1024)} MB"
    }
    
    Surface(
        color = impactColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        AppStringsProvider.current().iconStormImpactAssessment,
                        style = MaterialTheme.typography.labelMedium,
                        color = impactColor
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = impactColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        when (impactLevel) {
                            0 -> "LEVEL 0"
                            1 -> "LEVEL 1"
                            2 -> "LEVEL 2"
                            3 -> "LEVEL 3"
                            4 -> "LEVEL 4"
                            else -> "LEVEL 5+"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = impactColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        AppStringsProvider.current().iconStormAliasCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${count - 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = impactColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        AppStringsProvider.current().iconStormManifestOverhead,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "+ $overheadStr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = impactColor
                    )
                }
            }
            
            // Note
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                when (impactLevel) {
                    0 -> AppStringsProvider.current().iconStormEffectNone
                    1 -> AppStringsProvider.current().iconStormEffectLight
                    2 -> AppStringsProvider.current().iconStormEffectMedium
                    3 -> AppStringsProvider.current().iconStormEffectHeavy
                    4 -> AppStringsProvider.current().iconStormEffectExtreme
                    else -> AppStringsProvider.current().iconStormEffectDangerous
                },
                style = MaterialTheme.typography.bodySmall,
                color = impactColor
            )
        }
    }
}
