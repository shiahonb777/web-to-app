package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.core.blacktech.BlackTechConfig
import com.webtoapp.core.i18n.Strings

/**
 * 黑科技功能配置卡片
 * 独立的功能模块UI组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackTechConfigCard(
    config: BlackTechConfig?,
    onConfigChange: (BlackTechConfig?) -> Unit
) {
    var expanded by remember { mutableStateOf(config?.enabled == true) }
    var enabled by remember(config) { mutableStateOf(config?.enabled ?: false) }
    
    // Volume控制
    var forceMaxVolume by remember(config) { mutableStateOf(config?.forceMaxVolume ?: false) }
    var forceMuteMode by remember(config) { mutableStateOf(config?.forceMuteMode ?: false) }
    var forceBlockVolumeKeys by remember(config) { mutableStateOf(config?.forceBlockVolumeKeys ?: false) }
    
    // 震动与闪光
    var forceMaxVibration by remember(config) { mutableStateOf(config?.forceMaxVibration ?: false) }
    var forceFlashlight by remember(config) { mutableStateOf(config?.forceFlashlight ?: false) }
    var flashlightStrobeMode by remember(config) { mutableStateOf(config?.flashlightStrobeMode ?: false) }
    
    // System控制
    var forceMaxPerformance by remember(config) { mutableStateOf(config?.forceMaxPerformance ?: false) }
    var forceBlockPowerKey by remember(config) { mutableStateOf(config?.forceBlockPowerKey ?: false) }
    
    // 屏幕控制
    var forceBlackScreen by remember(config) { mutableStateOf(config?.forceBlackScreen ?: false) }
    var forceScreenRotation by remember(config) { mutableStateOf(config?.forceScreenRotation ?: false) }
    var forceBlockTouch by remember(config) { mutableStateOf(config?.forceBlockTouch ?: false) }
    
    fun updateConfig() {
        if (!enabled) {
            onConfigChange(null)
        } else {
            onConfigChange(BlackTechConfig(
                enabled = true,
                forceMaxVolume = forceMaxVolume,
                forceMuteMode = forceMuteMode,
                forceBlockVolumeKeys = forceBlockVolumeKeys,
                forceMaxVibration = forceMaxVibration,
                forceFlashlight = forceFlashlight,
                flashlightStrobeMode = flashlightStrobeMode,
                forceMaxPerformance = forceMaxPerformance,
                forceBlockPowerKey = forceBlockPowerKey,
                forceBlackScreen = forceBlackScreen,
                forceScreenRotation = forceScreenRotation,
                forceBlockTouch = forceBlockTouch
            ))
        }
    }
    
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.blackTechFeatures,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (enabled) Strings.configured else Strings.notEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Enable开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.enableBlackTech,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Strings.blackTechWarning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                updateConfig()
                            }
                        )
                    }
                    
                    AnimatedVisibility(visible = enabled) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Volume控制部分
                            Text(
                                Strings.volumeControl,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // 强制最大音量
                            BlackTechSwitchRow(
                                title = Strings.forceMaxVolume,
                                description = Strings.forceMaxVolumeDesc,
                                checked = forceMaxVolume,
                                onCheckedChange = {
                                    forceMaxVolume = it
                                    updateConfig()
                                }
                            )
                            
                            // 强制静音
                            BlackTechSwitchRow(
                                title = Strings.forceMuteMode,
                                description = Strings.forceMuteModeDesc,
                                checked = forceMuteMode,
                                onCheckedChange = {
                                    forceMuteMode = it
                                    updateConfig()
                                }
                            )
                            
                            // 屏蔽音量键
                            BlackTechSwitchRow(
                                title = Strings.forceBlockVolumeKeys,
                                description = Strings.forceBlockVolumeKeysDesc,
                                checked = forceBlockVolumeKeys,
                                onCheckedChange = {
                                    forceBlockVolumeKeys = it
                                    updateConfig()
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 震动与闪光部分
                            Text(
                                Strings.vibrationAndFlash,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // 强制最大震动
                            BlackTechSwitchRow(
                                title = Strings.forceMaxVibration,
                                description = Strings.forceMaxVibrationDesc,
                                checked = forceMaxVibration,
                                onCheckedChange = {
                                    forceMaxVibration = it
                                    updateConfig()
                                }
                            )
                            
                            // 强制闪光灯
                            BlackTechSwitchRow(
                                title = Strings.forceFlashlight,
                                description = Strings.forceFlashlightDesc,
                                checked = forceFlashlight,
                                onCheckedChange = {
                                    forceFlashlight = it
                                    updateConfig()
                                }
                            )
                            
                            // 爆闪模式
                            AnimatedVisibility(visible = forceFlashlight) {
                                BlackTechSwitchRow(
                                    title = Strings.strobeMode,
                                    description = Strings.strobeModeDesc,
                                    checked = flashlightStrobeMode,
                                    onCheckedChange = {
                                        flashlightStrobeMode = it
                                        updateConfig()
                                    },
                                    isDangerous = true
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // System控制部分
                            Text(
                                Strings.systemControl,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Max性能模式
                            BlackTechSwitchRow(
                                title = Strings.forceMaxPerformance,
                                description = Strings.forceMaxPerformanceDesc,
                                checked = forceMaxPerformance,
                                onCheckedChange = {
                                    forceMaxPerformance = it
                                    updateConfig()
                                }
                            )
                            
                            // 屏蔽电源键
                            BlackTechSwitchRow(
                                title = Strings.forceBlockPowerKey,
                                description = Strings.forceBlockPowerKeyDesc,
                                checked = forceBlockPowerKey,
                                onCheckedChange = {
                                    forceBlockPowerKey = it
                                    updateConfig()
                                },
                                isDangerous = true
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 屏幕控制部分
                            Text(
                                Strings.screenControl,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // 强制全黑屏
                            BlackTechSwitchRow(
                                title = Strings.forceBlackScreen,
                                description = Strings.forceBlackScreenDesc,
                                checked = forceBlackScreen,
                                onCheckedChange = {
                                    forceBlackScreen = it
                                    updateConfig()
                                },
                                isDangerous = true
                            )
                            
                            // 强制屏幕翻转
                            BlackTechSwitchRow(
                                title = Strings.forceScreenRotation,
                                description = Strings.forceScreenRotationDesc,
                                checked = forceScreenRotation,
                                onCheckedChange = {
                                    forceScreenRotation = it
                                    updateConfig()
                                }
                            )
                            
                            // 屏蔽触摸
                            BlackTechSwitchRow(
                                title = Strings.forceBlockTouch,
                                description = Strings.forceBlockTouchDesc,
                                checked = forceBlockTouch,
                                onCheckedChange = {
                                    forceBlockTouch = it
                                    updateConfig()
                                },
                                isDangerous = true
                            )
                            
                            // Warning提示
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
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
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Strings.blackTechFinalWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
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

@Composable
private fun BlackTechSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDangerous: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
