package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.disguise.DeviceDisguiseConfig
import com.webtoapp.core.disguise.DeviceType
import com.webtoapp.core.disguise.DeviceOS
import com.webtoapp.core.disguise.DeviceBrand
import com.webtoapp.core.disguise.DevicePresets
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumSwitch
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.SettingsSwitch

/**
 * 设备伪装卡片 — 独立一级功能卡片 v2.0
 *
 * 设计哲学: 极客美学 — 仅分类使用 emoji，品牌和型号纯文字显示
 *
 * 功能:
 * - 设备类型切换 (手机/平板/桌面/笔记本/手表/电视)
 * - 热门设备一键选择 (30+ 预设，全部 2025-2026 最新型号)
 * - 自定义设备配置 (品牌/型号/分辨率 完全可控)
 * - 自动生成 User-Agent
 * - 自定义 UA 覆盖
 * - 强制桌面视口选项
 * - 当前伪装状态实时预览
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceDisguiseCard(
    config: DeviceDisguiseConfig,
    onConfigChange: (DeviceDisguiseConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomUA by remember { mutableStateOf(false) }
    var showCustomDevice by remember { mutableStateOf(false) }

    // 自定义设备的临时编辑状态
    var customModelName by remember { mutableStateOf("") }
    var customModelId by remember { mutableStateOf("") }
    var customScreenW by remember { mutableStateOf("") }
    var customScreenH by remember { mutableStateOf("") }
    var customDensity by remember { mutableStateOf("") }

    val isEnabled = config.enabled
    val accentColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ═══════ 卡片头部 ═══════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.DevicesOther,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.deviceDisguiseTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isEnabled && config.deviceModelName.isNotBlank())
                                "${Strings.deviceDisguiseActive} ${config.deviceModelName}"
                            else Strings.notEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }

            // ═══════ 展开内容 ═══════
            AnimatedVisibility(
                visible = expanded,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // ── 总开关 ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.deviceDisguiseTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isEnabled) Strings.deviceDisguiseActive else Strings.deviceDisguiseOff,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = isEnabled,
                            onCheckedChange = { onConfigChange(config.copy(enabled = it)) }
                        )
                    }

                    AnimatedVisibility(
                        visible = isEnabled,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            // ── 提示信息 ──
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Strings.deviceDisguiseHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── 设备类型选择 (仅此处使用 emoji) ──
                            Text(
                                text = Strings.deviceQuickSelect,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val deviceTypes = listOf(
                                DeviceType.PHONE to Strings.deviceTypePhone,
                                DeviceType.TABLET to Strings.deviceTypeTablet,
                                DeviceType.DESKTOP to Strings.deviceTypeDesktop,
                                DeviceType.LAPTOP to Strings.deviceTypeLaptop,
                                DeviceType.WATCH to Strings.deviceTypeWatch
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                deviceTypes.forEach { (type, label) ->
                                    val isSelected = config.deviceType == type
                                    val bgColor by animateColorAsState(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        label = "typeBg"
                                    )
                                    val contentColor by animateColorAsState(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "typeContent"
                                    )

                                    Surface(
                                        onClick = {
                                            // 切换设备类型时，仅更新 deviceType，不自动选到第一个预设
                                            // 用户需要手动选择具体型号
                                            val presets = DevicePresets.getPresetsForType(type)
                                            if (presets.isNotEmpty()) {
                                                onConfigChange(presets.first().toConfig())
                                            } else {
                                                onConfigChange(config.copy(deviceType = type))
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = bgColor,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                            1.5.dp, MaterialTheme.colorScheme.primary
                                        ) else null
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            // 设备类型分类 emoji
                                            Text(
                                                text = type.emoji,
                                                fontSize = 20.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = contentColor,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // ── 热门设备预设列表 (纯文字，无 emoji) ──
                            Text(
                                text = Strings.devicePopularPresets,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val presets = DevicePresets.getPresetsForType(config.deviceType)

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presets.forEach { preset ->
                                    // 使用唯一 model ID 进行匹配，避免空 model 导致的多重选中
                                    val isSelected = config.deviceModel == preset.model &&
                                            config.deviceBrand == preset.brand
                                    val chipBg by animateColorAsState(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                        label = "presetBg"
                                    )

                                    Surface(
                                        onClick = {
                                            // 选择预设时保持当前 deviceType（不跟随预设切换分类）
                                            onConfigChange(preset.toConfig().copy(
                                                deviceType = config.deviceType
                                            ))
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = chipBg,
                                        tonalElevation = if (isSelected) 0.dp else 2.dp,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                            1.5.dp, MaterialTheme.colorScheme.primary
                                        ) else androidx.compose.foundation.BorderStroke(
                                            0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 纯文字：品牌·型号·分辨率
                                            Column {
                                                Text(
                                                    text = preset.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${preset.os.displayName} · ${preset.screenWidth}×${preset.screenHeight}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Outlined.CheckCircle,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // ── 当前伪装信息预览 ──
                            if (config.deviceModelName.isNotBlank()) {
                                Text(
                                    text = Strings.deviceCurrentDisguise,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // 仅设备类型使用 emoji
                                            Text(
                                                text = config.deviceType.emoji,
                                                fontSize = 24.sp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = config.deviceModelName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${config.deviceBrand.displayName} · ${config.deviceOS.displayName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // UA 预览
                                        val ua = config.generateUserAgent()
                                        if (ua.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = Strings.deviceGeneratedUA,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = ua,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                        lineHeight = 15.sp,
                                                        maxLines = 4,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // 屏幕尺寸 (纯文字标签)
                                        if (config.screenWidth > 0 && config.screenHeight > 0) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                InfoChip("${config.screenWidth}×${config.screenHeight}")
                                                if (config.pixelDensity > 0) {
                                                    InfoChip("${config.pixelDensity}x DPI")
                                                }
                                                if (config.requiresDesktopViewport()) {
                                                    InfoChip("Desktop VP")
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // ── 强制桌面视口 ──
                            if (config.deviceType !in listOf(DeviceType.DESKTOP, DeviceType.LAPTOP)) {
                                SettingsSwitch(
                                    title = Strings.deviceDesktopViewport,
                                    subtitle = Strings.deviceDesktopViewportHint,
                                    checked = config.isDesktopViewport,
                                    onCheckedChange = {
                                        onConfigChange(config.copy(isDesktopViewport = it))
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // ── 自定义设备区域 ──
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))

                            SettingsSwitch(
                                title = Strings.deviceCustomDevice,
                                subtitle = Strings.deviceCustomDeviceHint,
                                checked = showCustomDevice || config.isCustomDevice,
                                onCheckedChange = {
                                    showCustomDevice = it
                                    if (it && config.deviceModelName.isNotBlank()) {
                                        // 预填充当前值
                                        customModelName = config.deviceModelName
                                        customModelId = config.deviceModel
                                        customScreenW = if (config.screenWidth > 0) config.screenWidth.toString() else ""
                                        customScreenH = if (config.screenHeight > 0) config.screenHeight.toString() else ""
                                        customDensity = if (config.pixelDensity > 0) config.pixelDensity.toString() else ""
                                    }
                                }
                            )

                            AnimatedVisibility(
                                visible = showCustomDevice || config.isCustomDevice,
                                enter = CardExpandTransition,
                                exit = CardCollapseTransition
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    // 设备名称
                                    PremiumTextField(
                                        value = customModelName,
                                        onValueChange = { customModelName = it },
                                        label = { Text(Strings.deviceCustomName) },
                                        placeholder = { Text("Galaxy S26 Ultra") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 设备型号标识
                                    PremiumTextField(
                                        value = customModelId,
                                        onValueChange = { customModelId = it },
                                        label = { Text(Strings.deviceCustomModelId) },
                                        placeholder = { Text("SM-S938B") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 屏幕分辨率
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PremiumTextField(
                                            value = customScreenW,
                                            onValueChange = { customScreenW = it.filter { c -> c.isDigit() } },
                                            label = { Text(Strings.deviceCustomWidth) },
                                            placeholder = { Text("1920") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        PremiumTextField(
                                            value = customScreenH,
                                            onValueChange = { customScreenH = it.filter { c -> c.isDigit() } },
                                            label = { Text(Strings.deviceCustomHeight) },
                                            placeholder = { Text("1080") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 像素密度
                                    PremiumTextField(
                                        value = customDensity,
                                        onValueChange = { customDensity = it },
                                        label = { Text(Strings.deviceCustomDensity) },
                                        placeholder = { Text("2.0") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 应用自定义设备按钮
                                    FilledTonalButton(
                                        onClick = {
                                            onConfigChange(config.copy(
                                                deviceModelName = customModelName.ifBlank { "Custom Device" },
                                                deviceModel = customModelId.ifBlank { "CUSTOM-${System.currentTimeMillis()}" },
                                                screenWidth = customScreenW.toIntOrNull() ?: 0,
                                                screenHeight = customScreenH.toIntOrNull() ?: 0,
                                                pixelDensity = customDensity.toFloatOrNull() ?: 0f,
                                                isCustomDevice = true
                                            ))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = customModelName.isNotBlank()
                                    ) {
                                        Icon(
                                            Icons.Outlined.Save,
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(Strings.deviceCustomApply)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // ── 自定义 UA 区域 ──
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))

                            SettingsSwitch(
                                title = Strings.deviceCustomUA,
                                subtitle = Strings.deviceCustomUAHint,
                                checked = showCustomUA || !config.customUserAgent.isNullOrBlank(),
                                onCheckedChange = {
                                    showCustomUA = it
                                    if (!it) onConfigChange(config.copy(customUserAgent = null))
                                }
                            )

                            AnimatedVisibility(
                                visible = showCustomUA || !config.customUserAgent.isNullOrBlank(),
                                enter = CardExpandTransition,
                                exit = CardCollapseTransition
                            ) {
                                PremiumTextField(
                                    value = config.customUserAgent ?: "",
                                    onValueChange = {
                                        onConfigChange(config.copy(customUserAgent = it.ifBlank { null }))
                                    },
                                    label = { Text("User-Agent") },
                                    placeholder = { Text("Mozilla/5.0 ...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    singleLine = false,
                                    minLines = 2,
                                    maxLines = 4
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
