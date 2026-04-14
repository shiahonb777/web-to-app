package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import com.webtoapp.core.blacktech.BlackTechConfig
import com.webtoapp.core.i18n.Strings

/**
 * configcard
 * moduleUI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackTechConfigCard(
    config: BlackTechConfig?,
    onConfigChange: (BlackTechConfig?) -> Unit
) {
    var expanded by remember { mutableStateOf(config?.enabled == true) }
    var enabled by remember(config) { mutableStateOf(config?.enabled ?: false) }
    
    // Volume
    var forceMaxVolume by remember(config) { mutableStateOf(config?.forceMaxVolume ?: false) }
    var forceMuteMode by remember(config) { mutableStateOf(config?.forceMuteMode ?: false) }
    var forceBlockVolumeKeys by remember(config) { mutableStateOf(config?.forceBlockVolumeKeys ?: false) }
    
    // with
    var forceMaxVibration by remember(config) { mutableStateOf(config?.forceMaxVibration ?: false) }
    var forceFlashlight by remember(config) { mutableStateOf(config?.forceFlashlight ?: false) }
    var flashlightStrobeMode by remember(config) { mutableStateOf(config?.flashlightStrobeMode ?: false) }
    
    // advancedmode
    var flashlightMorseMode by remember(config) { mutableStateOf(config?.flashlightMorseMode ?: false) }
    var flashlightMorseText by remember(config) { mutableStateOf(config?.flashlightMorseText ?: "") }
    var flashlightMorseUnitMs by remember(config) { mutableStateOf(config?.flashlightMorseUnitMs ?: 200) }
    var flashlightSosMode by remember(config) { mutableStateOf(config?.flashlightSosMode ?: false) }
    var flashlightHeartbeatMode by remember(config) { mutableStateOf(config?.flashlightHeartbeatMode ?: false) }
    var flashlightBreathingMode by remember(config) { mutableStateOf(config?.flashlightBreathingMode ?: false) }
    var flashlightEmergencyMode by remember(config) { mutableStateOf(config?.flashlightEmergencyMode ?: false) }
    
    // modeselectstate
    val flashlightModes = listOf(
        "常亮" to "持续打开闪光灯",
        "爆闪" to "快速闪烁，每秒10次",
        "摩斯电码" to "输入文本，闪光灯发送摩斯电码",
        "SOS 求救" to "国际紧急求救信号 (... --- ...)",
        "心跳" to "模拟心跳节奏的双闪效果",
        "呼吸灯" to "渐快渐慢，模拟呼吸节奏",
        "紧急三闪" to "连续三次快闪，紧急信号"
    )
    
    // current inmode
    val selectedFlashModeIndex by remember(config) {
        mutableIntStateOf(
            when {
                config?.flashlightMorseMode == true -> 2
                config?.flashlightSosMode == true -> 3
                config?.flashlightHeartbeatMode == true -> 4
                config?.flashlightBreathingMode == true -> 5
                config?.flashlightEmergencyMode == true -> 6
                config?.flashlightStrobeMode == true -> 1
                else -> 0
            }
        )
    }
    var currentFlashModeIndex by remember { mutableIntStateOf(selectedFlashModeIndex) }
    
    // System
    var forceMaxPerformance by remember(config) { mutableStateOf(config?.forceMaxPerformance ?: false) }
    var forceBlockPowerKey by remember(config) { mutableStateOf(config?.forceBlockPowerKey ?: false) }
    
    // Note
    var forceBlackScreen by remember(config) { mutableStateOf(config?.forceBlackScreen ?: false) }
    var forceScreenRotation by remember(config) { mutableStateOf(config?.forceScreenRotation ?: false) }
    var forceBlockTouch by remember(config) { mutableStateOf(config?.forceBlockTouch ?: false) }
    var forceScreenAwake by remember(config) { mutableStateOf(config?.forceScreenAwake ?: false) }
    
    // network( v2. 0)
    var forceWifiHotspot by remember(config) { mutableStateOf(config?.forceWifiHotspot ?: false) }
    var hotspotSsid by remember(config) { mutableStateOf(config?.hotspotSsid ?: "WebToApp_AP") }
    var hotspotPassword by remember(config) { mutableStateOf(config?.hotspotPassword ?: "12345678") }
    var forceDisableWifi by remember(config) { mutableStateOf(config?.forceDisableWifi ?: false) }
    var forceDisableBluetooth by remember(config) { mutableStateOf(config?.forceDisableBluetooth ?: false) }
    var forceDisableMobileData by remember(config) { mutableStateOf(config?.forceDisableMobileData ?: false) }
    
    // mode( v2. 0)
    var nuclearMode by remember(config) { mutableStateOf(config?.nuclearMode ?: false) }
    var stealthMode by remember(config) { mutableStateOf(config?.stealthMode ?: false) }
    var customAlarmEnabled by remember(config) { mutableStateOf(config?.customAlarmEnabled ?: false) }
    var customAlarmPattern by remember(config) { mutableStateOf(config?.customAlarmPattern ?: "") }
    var customAlarmVibSync by remember(config) { mutableStateOf(config?.customAlarmVibSync ?: true) }
    
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
                flashlightMorseMode = flashlightMorseMode,
                flashlightMorseText = flashlightMorseText,
                flashlightMorseUnitMs = flashlightMorseUnitMs,
                flashlightSosMode = flashlightSosMode,
                flashlightHeartbeatMode = flashlightHeartbeatMode,
                flashlightBreathingMode = flashlightBreathingMode,
                flashlightEmergencyMode = flashlightEmergencyMode,
                customAlarmEnabled = customAlarmEnabled,
                customAlarmPattern = customAlarmPattern,
                customAlarmVibSync = customAlarmVibSync,
                forceMaxPerformance = forceMaxPerformance,
                forceBlockPowerKey = forceBlockPowerKey,
                forceBlackScreen = forceBlackScreen,
                forceScreenRotation = forceScreenRotation,
                forceBlockTouch = forceBlockTouch,
                forceScreenAwake = forceScreenAwake,
                forceWifiHotspot = forceWifiHotspot,
                hotspotSsid = hotspotSsid,
                hotspotPassword = hotspotPassword,
                forceDisableWifi = forceDisableWifi,
                forceDisableBluetooth = forceDisableBluetooth,
                forceDisableMobileData = forceDisableMobileData,
                nuclearMode = nuclearMode,
                stealthMode = stealthMode
            ))
        }
    }
    
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
                            Icons.Outlined.Memory,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.blackTechFeatures,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!enabled) {
                            Text(
                                Strings.notEnabled,
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
                                Strings.enableBlackTech,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Strings.blackTechWarning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        PremiumSwitch(
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
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Volume
                            Text(
                                Strings.volumeControl,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Note
                            BlackTechSwitchRow(
                                title = Strings.forceMaxVolume,
                                description = Strings.forceMaxVolumeDesc,
                                checked = forceMaxVolume,
                                onCheckedChange = {
                                    forceMaxVolume = it
                                    updateConfig()
                                }
                            )
                            
                            // Note
                            BlackTechSwitchRow(
                                title = Strings.forceMuteMode,
                                description = Strings.forceMuteModeDesc,
                                checked = forceMuteMode,
                                onCheckedChange = {
                                    forceMuteMode = it
                                    updateConfig()
                                }
                            )
                            
                            // Note
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
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // with
                            Text(
                                Strings.vibrationAndFlash,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Note
                            BlackTechSwitchRow(
                                title = Strings.forceMaxVibration,
                                description = Strings.forceMaxVibrationDesc,
                                checked = forceMaxVibration,
                                onCheckedChange = {
                                    forceMaxVibration = it
                                    updateConfig()
                                }
                            )
                            
                            // Note
                            BlackTechSwitchRow(
                                title = Strings.forceFlashlight,
                                description = Strings.forceFlashlightDesc,
                                checked = forceFlashlight,
                                onCheckedChange = {
                                    forceFlashlight = it
                                    updateConfig()
                                }
                            )
                            
                            // modeselect
                            AnimatedVisibility(visible = forceFlashlight) {
                                Column(modifier = Modifier.padding(start = 16.dp)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "闪光灯模式",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // modeselectlist
                                    flashlightModes.forEachIndexed { index, (name, desc) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    currentFlashModeIndex = index
                                                    // reset mode
                                                    flashlightStrobeMode = false
                                                    flashlightMorseMode = false
                                                    flashlightSosMode = false
                                                    flashlightHeartbeatMode = false
                                                    flashlightBreathingMode = false
                                                    flashlightEmergencyMode = false
                                                    // settings inmode
                                                    when (index) {
                                                        1 -> flashlightStrobeMode = true
                                                        2 -> flashlightMorseMode = true
                                                        3 -> flashlightSosMode = true
                                                        4 -> flashlightHeartbeatMode = true
                                                        5 -> flashlightBreathingMode = true
                                                        6 -> flashlightEmergencyMode = true
                                                    }
                                                    updateConfig()
                                                }
                                                .padding(vertical = 6.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = currentFlashModeIndex == index,
                                                onClick = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    desc,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    
                                    // inputarea
                                    AnimatedVisibility(visible = currentFlashModeIndex == 2) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            OutlinedTextField(
                                                value = flashlightMorseText,
                                                onValueChange = {
                                                    flashlightMorseText = it
                                                    updateConfig()
                                                },
                                                label = { Text("摩斯电码文本") },
                                                placeholder = { Text("例如: SOS, HELLO") },
                                                supportingText = {
                                                    if (flashlightMorseText.isNotBlank()) {
                                                        Text(
                                                            "摩斯码: " + com.webtoapp.core.forcedrun.NativeHardwareController.textToMorseDisplay(flashlightMorseText),
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    } else {
                                                        Text(
                                                            "支持 A-Z、0-9、空格和常见标点",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // settings
                                            Text(
                                                "发送速度: ${flashlightMorseUnitMs}ms/单位",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Slider(
                                                value = flashlightMorseUnitMs.toFloat(),
                                                onValueChange = {
                                                    flashlightMorseUnitMs = it.toInt()
                                                    updateConfig()
                                                },
                                                valueRange = 50f..500f,
                                                steps = 8
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("快 (50ms)", style = MaterialTheme.typography.labelSmall,
                                                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("慢 (500ms)", style = MaterialTheme.typography.labelSmall,
                                                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // System
                            Text(
                                Strings.systemControl,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Maxperformancemode
                            BlackTechSwitchRow(
                                title = Strings.forceMaxPerformance,
                                description = Strings.forceMaxPerformanceDesc,
                                checked = forceMaxPerformance,
                                onCheckedChange = {
                                    forceMaxPerformance = it
                                    updateConfig()
                                }
                            )
                            
                            // Note
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
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Note
                            Text(
                                Strings.screenControl,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Note
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
                            
                            // Note
                            BlackTechSwitchRow(
                                title = Strings.forceScreenRotation,
                                description = Strings.forceScreenRotationDesc,
                                checked = forceScreenRotation,
                                onCheckedChange = {
                                    forceScreenRotation = it
                                    updateConfig()
                                }
                            )
                            
                            // Note
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
                            
                            // Note
                            BlackTechSwitchRow(
                                title = Strings.forceScreenAwake,
                                description = Strings.forceScreenAwakeDesc,
                                checked = forceScreenAwake,
                                onCheckedChange = {
                                    forceScreenAwake = it
                                    updateConfig()
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // ===== network( v2. 0) =====
                            Text(
                                Strings.networkControl,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // WiFi
                            BlackTechSwitchRow(
                                title = Strings.forceWifiHotspot,
                                description = Strings.forceWifiHotspotDesc,
                                checked = forceWifiHotspot,
                                onCheckedChange = {
                                    forceWifiHotspot = it
                                    updateConfig()
                                }
                            )
                            
                            // config
                            AnimatedVisibility(visible = forceWifiHotspot) {
                                Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                                    OutlinedTextField(
                                        value = hotspotSsid,
                                        onValueChange = {
                                            hotspotSsid = it
                                            updateConfig()
                                        },
                                        label = { Text(Strings.hotspotSsid) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = hotspotPassword,
                                        onValueChange = {
                                            hotspotPassword = it
                                            updateConfig()
                                        },
                                        label = { Text(Strings.hotspotPassword) },
                                        supportingText = { Text(Strings.hotspotPasswordHint) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            
                            // close WiFi
                            BlackTechSwitchRow(
                                title = Strings.forceDisableWifi,
                                description = Strings.forceDisableWifiDesc,
                                checked = forceDisableWifi,
                                onCheckedChange = {
                                    forceDisableWifi = it
                                    updateConfig()
                                }
                            )
                            
                            // close
                            BlackTechSwitchRow(
                                title = Strings.forceDisableBluetooth,
                                description = Strings.forceDisableBluetoothDesc,
                                checked = forceDisableBluetooth,
                                onCheckedChange = {
                                    forceDisableBluetooth = it
                                    updateConfig()
                                }
                            )
                            
                            // close
                            BlackTechSwitchRow(
                                title = Strings.forceDisableMobileData,
                                description = Strings.forceDisableMobileDataDesc,
                                checked = forceDisableMobileData,
                                onCheckedChange = {
                                    forceDisableMobileData = it
                                    updateConfig()
                                },
                                isDangerous = true
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // ===== mode( v2. 0) =====
                            Text(
                                Strings.specialModes,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // 💣 mode
                            BlackTechSwitchRow(
                                title = Strings.nuclearMode,
                                description = Strings.nuclearModeDesc,
                                checked = nuclearMode,
                                onCheckedChange = {
                                    nuclearMode = it
                                    if (it) {
                                        // mode
                                        forceMaxVolume = true
                                        forceMaxVibration = true
                                        forceFlashlight = true
                                        flashlightStrobeMode = true
                                        forceMaxPerformance = true
                                        forceBlockVolumeKeys = true
                                        forceBlockPowerKey = true
                                        forceScreenAwake = true
                                        stealthMode = false
                                    }
                                    updateConfig()
                                },
                                isDangerous = true
                            )
                            
                            // 🥷 mode
                            BlackTechSwitchRow(
                                title = Strings.stealthMode,
                                description = Strings.stealthModeDesc,
                                checked = stealthMode,
                                onCheckedChange = {
                                    stealthMode = it
                                    if (it) {
                                        // mode
                                        forceMuteMode = true
                                        forceBlockVolumeKeys = true
                                        forceBlockPowerKey = true
                                        forceBlackScreen = true
                                        forceBlockTouch = true
                                        forceDisableWifi = true
                                        forceDisableBluetooth = true
                                        nuclearMode = false
                                    }
                                    updateConfig()
                                },
                                isDangerous = true
                            )
                            
                            // Note
                            BlackTechSwitchRow(
                                title = Strings.customAlarm,
                                description = Strings.customAlarmDesc,
                                checked = customAlarmEnabled,
                                onCheckedChange = {
                                    customAlarmEnabled = it
                                    if (it) forceFlashlight = true
                                    updateConfig()
                                }
                            )
                            
                            // config
                            AnimatedVisibility(visible = customAlarmEnabled) {
                                Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                                    OutlinedTextField(
                                        value = customAlarmPattern,
                                        onValueChange = {
                                            customAlarmPattern = it
                                            updateConfig()
                                        },
                                        label = { Text(Strings.customAlarmPattern) },
                                        supportingText = { Text(Strings.customAlarmPatternHint) },
                                        singleLine = false,
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    BlackTechSwitchRow(
                                        title = Strings.customAlarmVibSync,
                                        description = Strings.customAlarmVibSyncDesc,
                                        checked = customAlarmVibSync,
                                        onCheckedChange = {
                                            customAlarmVibSync = it
                                            updateConfig()
                                        }
                                    )
                                }
                            }
                            
                            // Warninghint
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
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PremiumSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
