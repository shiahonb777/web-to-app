package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.design.WtaSwitch
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webtoapp.core.disguise.BrowserDisguiseConfig
import com.webtoapp.core.disguise.BrowserDisguisePreset
import com.webtoapp.core.disguise.WebGLRenderer
import com.webtoapp.core.disguise.ScreenProfile
import com.webtoapp.core.i18n.Strings










@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserDisguiseConfigCard(
    config: BrowserDisguiseConfig?,
    onConfigChange: (BrowserDisguiseConfig?) -> Unit
) {
    var expanded by remember { mutableStateOf(config?.enabled == true) }
    var showAdvanced by remember { mutableStateOf(false) }


    val currentConfig = config ?: BrowserDisguiseConfig.DISABLED
    var enabled by remember(config) { mutableStateOf(currentConfig.enabled) }
    var preset by remember(config) { mutableStateOf(currentConfig.preset) }


    var canvasNoise by remember(config) { mutableStateOf(currentConfig.canvasNoise) }
    var webglSpoof by remember(config) { mutableStateOf(currentConfig.webglSpoof) }
    var webglRenderer by remember(config) { mutableStateOf(currentConfig.webglRenderer) }
    var audioNoise by remember(config) { mutableStateOf(currentConfig.audioNoise) }
    var screenSpoof by remember(config) { mutableStateOf(currentConfig.screenSpoof) }
    var screenProfile by remember(config) { mutableStateOf(currentConfig.screenProfile) }
    var clientRectsNoise by remember(config) { mutableStateOf(currentConfig.clientRectsNoise) }
    var timezoneSpoof by remember(config) { mutableStateOf(currentConfig.timezoneSpoof) }
    var languageSpoof by remember(config) { mutableStateOf(currentConfig.languageSpoof) }
    var platformSpoof by remember(config) { mutableStateOf(currentConfig.platformSpoof) }
    var hardwareConcurrencySpoof by remember(config) { mutableStateOf(currentConfig.hardwareConcurrencySpoof) }
    var deviceMemorySpoof by remember(config) { mutableStateOf(currentConfig.deviceMemorySpoof) }
    var mediaDevicesSpoof by remember(config) { mutableStateOf(currentConfig.mediaDevicesSpoof) }
    var webrtcIpShield by remember(config) { mutableStateOf(currentConfig.webrtcIpShield) }
    var fontEnumerationBlock by remember(config) { mutableStateOf(currentConfig.fontEnumerationBlock) }
    var batteryShield by remember(config) { mutableStateOf(currentConfig.batteryShield) }
    var nativeToStringProtection by remember(config) { mutableStateOf(currentConfig.nativeToStringProtection) }
    var iframeDisguisePropagation by remember(config) { mutableStateOf(currentConfig.iframeDisguisePropagation) }

    fun buildConfig(): BrowserDisguiseConfig? {
        if (!enabled) return null
        return BrowserDisguiseConfig(
            enabled = true,
            preset = preset,
            removeXRequestedWith = true,
            sanitizeUserAgent = true,
            hideWebdriver = true,
            emulateWindowChrome = true,
            fakePlugins = true,
            fakeVendor = true,
            canvasNoise = canvasNoise,
            webglSpoof = webglSpoof,
            webglRenderer = webglRenderer,
            audioNoise = audioNoise,
            screenSpoof = screenSpoof,
            screenProfile = screenProfile,
            clientRectsNoise = clientRectsNoise,
            timezoneSpoof = timezoneSpoof,
            languageSpoof = languageSpoof,
            platformSpoof = platformSpoof,
            hardwareConcurrencySpoof = hardwareConcurrencySpoof,
            deviceMemorySpoof = deviceMemorySpoof,
            mediaDevicesSpoof = mediaDevicesSpoof,
            webrtcIpShield = webrtcIpShield,
            fontEnumerationBlock = fontEnumerationBlock,
            batteryShield = batteryShield,
            nativeToStringProtection = nativeToStringProtection,
            iframeDisguisePropagation = iframeDisguisePropagation,
            errorStackCleaning = true
        )
    }

    fun updateConfig() {
        onConfigChange(buildConfig())
    }

    fun applyPreset(p: BrowserDisguisePreset) {
        preset = p
        if (p == BrowserDisguisePreset.OFF) {
            enabled = false
            onConfigChange(null)
            return
        }
        enabled = true
        val c = BrowserDisguiseConfig.fromPreset(p)
        canvasNoise = c.canvasNoise
        webglSpoof = c.webglSpoof
        webglRenderer = c.webglRenderer
        audioNoise = c.audioNoise
        screenSpoof = c.screenSpoof
        screenProfile = c.screenProfile
        clientRectsNoise = c.clientRectsNoise
        timezoneSpoof = c.timezoneSpoof
        languageSpoof = c.languageSpoof
        platformSpoof = c.platformSpoof
        hardwareConcurrencySpoof = c.hardwareConcurrencySpoof
        deviceMemorySpoof = c.deviceMemorySpoof
        mediaDevicesSpoof = c.mediaDevicesSpoof
        webrtcIpShield = c.webrtcIpShield
        fontEnumerationBlock = c.fontEnumerationBlock
        batteryShield = c.batteryShield
        nativeToStringProtection = c.nativeToStringProtection
        iframeDisguisePropagation = c.iframeDisguisePropagation
        updateConfig()
    }


    val coverage = BrowserDisguiseConfig.calculateCoverage(buildConfig() ?: BrowserDisguiseConfig.DISABLED)
    val level = BrowserDisguiseConfig.getDisguiseLevel(coverage)

    val levelColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant
            coverage < 0.3f -> com.webtoapp.ui.design.WtaColors.semantic.success
            coverage < 0.5f -> com.webtoapp.ui.design.WtaColors.semantic.info
            coverage < 0.75f -> MaterialTheme.colorScheme.tertiary
            coverage < 0.95f -> com.webtoapp.ui.design.WtaColors.semantic.warning
            else -> com.webtoapp.ui.design.WtaColors.semantic.error
        },
        label = "levelColor"
    )

    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

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
                                if (enabled) Brush.linearGradient(listOf(
                                    levelColor.copy(alpha = 0.2f),
                                    levelColor.copy(alpha = 0.05f)
                                ))
                                else Brush.linearGradient(listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                ))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = if (enabled) levelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.browserDisguiseTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (enabled) {
                            Text(
                                "${level} · ${"%.0f".format(coverage * 100)}% ${Strings.browserDisguiseCoverage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = levelColor
                            )
                        } else {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(Strings.browserDisguiseEnable, style = MaterialTheme.typography.bodyLarge)
                            Text(Strings.browserDisguiseEnableDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        WtaSwitch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                if (it && preset == BrowserDisguisePreset.OFF) {
                                    applyPreset(BrowserDisguisePreset.STEALTH)
                                } else if (!it) {
                                    onConfigChange(null)
                                } else {
                                    updateConfig()
                                }
                            }
                        )
                    }

                    AnimatedVisibility(visible = enabled) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))


                            Text(Strings.browserDisguisePreset, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))

                            val presets = BrowserDisguisePreset.entries.filter { it != BrowserDisguisePreset.OFF }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.forEach { p ->
                                    val isSelected = preset == p
                                    val chipColor = when (p.level) {
                                        1 -> com.webtoapp.ui.design.WtaColors.semantic.success
                                        2 -> com.webtoapp.ui.design.WtaColors.semantic.info
                                        3 -> MaterialTheme.colorScheme.tertiary
                                        4 -> com.webtoapp.ui.design.WtaColors.semantic.error
                                        else -> MaterialTheme.colorScheme.primary
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { applyPreset(p) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) chipColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, chipColor.copy(alpha = 0.6f)) else null
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                p.displayName.split(" ").first(),
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                p.displayName.split(" ").getOrElse(1) { "" },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center,
                                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))


                            CoverageDashboard(
                                coverage = coverage,
                                level = level,
                                levelColor = levelColor,
                                activeVectors = countActiveVectors(buildConfig() ?: BrowserDisguiseConfig.DISABLED)
                            )

                            Spacer(modifier = Modifier.height(12.dp))


                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAdvanced = !showAdvanced },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            Strings.browserDisguiseAdvanced,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Icon(
                                        if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showAdvanced) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {

                                    SectionHeader(Strings.browserDisguiseL2Title)

                                    VectorSwitch(Strings.browserDisguiseCanvasNoise, Strings.browserDisguiseCanvasNoiseDesc, canvasNoise) {
                                        canvasNoise = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseWebGL, Strings.browserDisguiseWebGLDesc, webglSpoof) {
                                        webglSpoof = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseAudio, Strings.browserDisguiseAudioDesc, audioNoise) {
                                        audioNoise = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseScreen, Strings.browserDisguiseScreenDesc, screenSpoof) {
                                        screenSpoof = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseClientRects, Strings.browserDisguiseClientRectsDesc, clientRectsNoise) {
                                        clientRectsNoise = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))


                                    SectionHeader(Strings.browserDisguiseL3Title)

                                    VectorSwitch(Strings.browserDisguiseTimezone, Strings.browserDisguiseTimezoneDesc, timezoneSpoof) {
                                        timezoneSpoof = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseLanguage, Strings.browserDisguiseLanguageDesc, languageSpoof) {
                                        languageSpoof = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguisePlatform, Strings.browserDisguisePlatformDesc, platformSpoof) {
                                        platformSpoof = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseHardware, Strings.browserDisguiseHardwareDesc, hardwareConcurrencySpoof) {
                                        hardwareConcurrencySpoof = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseMemory, Strings.browserDisguiseMemoryDesc, deviceMemorySpoof) {
                                        deviceMemorySpoof = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))


                                    SectionHeader(Strings.browserDisguiseL4Title)

                                    VectorSwitch(Strings.browserDisguiseMediaDevices, Strings.browserDisguiseMediaDevicesDesc, mediaDevicesSpoof) {
                                        mediaDevicesSpoof = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseWebRTC, Strings.browserDisguiseWebRTCDesc, webrtcIpShield) {
                                        webrtcIpShield = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseFonts, Strings.browserDisguiseFontsDesc, fontEnumerationBlock) {
                                        fontEnumerationBlock = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseBattery, Strings.browserDisguiseBatteryDesc, batteryShield) {
                                        batteryShield = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))


                                    SectionHeader(Strings.browserDisguiseL5Title)

                                    VectorSwitch(Strings.browserDisguisePrototype, Strings.browserDisguisePrototypeDesc, nativeToStringProtection) {
                                        nativeToStringProtection = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                    VectorSwitch(Strings.browserDisguiseIframe, Strings.browserDisguiseIframeDesc, iframeDisguisePropagation) {
                                        iframeDisguisePropagation = it; preset = BrowserDisguisePreset.CUSTOM; updateConfig()
                                    }
                                }
                            }


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
                                        Strings.browserDisguiseTip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun VectorSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        WtaSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun CoverageDashboard(
    coverage: Float,
    level: String,
    levelColor: Color,
    activeVectors: Int
) {
    Surface(
        color = levelColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Strings.browserDisguiseCoverageTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = levelColor
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = levelColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        level,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = levelColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))


            LinearProgressIndicator(
                progress = { coverage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = levelColor,
                trackColor = levelColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        Strings.browserDisguiseActiveVectors,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$activeVectors / 22",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = levelColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Strings.browserDisguiseCoverage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${"%.0f".format(coverage * 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = levelColor
                    )
                }
            }
        }
    }
}

private fun countActiveVectors(config: BrowserDisguiseConfig): Int {
    if (!config.enabled) return 0
    var count = 0
    if (config.removeXRequestedWith) count++
    if (config.sanitizeUserAgent) count++
    if (config.hideWebdriver) count++
    if (config.emulateWindowChrome) count++
    if (config.fakePlugins) count++
    if (config.fakeVendor) count++
    if (config.canvasNoise) count++
    if (config.webglSpoof) count++
    if (config.audioNoise) count++
    if (config.screenSpoof) count++
    if (config.clientRectsNoise) count++
    if (config.timezoneSpoof) count++
    if (config.languageSpoof) count++
    if (config.platformSpoof) count++
    if (config.hardwareConcurrencySpoof) count++
    if (config.deviceMemorySpoof) count++
    if (config.mediaDevicesSpoof) count++
    if (config.webrtcIpShield) count++
    if (config.fontEnumerationBlock) count++
    if (config.batteryShield) count++
    if (config.nativeToStringProtection) count++
    if (config.iframeDisguisePropagation) count++
    return count
}
