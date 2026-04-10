package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.FloatingWindowConfig
import com.webtoapp.data.model.FloatingBorderStyle
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import kotlin.math.roundToInt

/**
 * 悬浮小窗配置卡片 — 高级版
 * 分为三个区域：窗口尺寸、外观样式、行为控制
 * 包含可视预览、分组展开、边框样式选择器
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FloatingWindowConfigCard(
    config: FloatingWindowConfig,
    onConfigChange: (FloatingWindowConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvanced by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    
    // 箭头旋转动画
    val arrowRotation by animateFloatAsState(
        targetValue = if (showAdvanced) 180f else 0f,
        animationSpec = tween(300),
        label = "arrowRotation"
    )

    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ── 头部：图标 + 标题 + 开关 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                if (config.enabled) primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PictureInPicture,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (config.enabled) primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.floatingWindowTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                PremiumSwitch(
                    checked = config.enabled,
                    onCheckedChange = { onConfigChange(config.copy(enabled = it)) }
                )
            }

            // ── 展开面板（仅启用时显示）──
            AnimatedVisibility(
                visible = config.enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    // ════════════════════════════════════════
                    // 区域 1: 窗口尺寸
                    // ════════════════════════════════════════
                    SectionHeader(
                        icon = Icons.Outlined.Straighten,
                        title = Strings.fwSectionSize
                    )

                    // 宽度滑块
                    SliderWithLabel(
                        label = Strings.fwWidthLabel,
                        value = config.widthPercent,
                        valueRange = 30f..100f,
                        steps = 13,
                        onValueChange = { newWidth ->
                            if (config.lockAspectRatio) {
                                onConfigChange(config.copy(
                                    widthPercent = newWidth,
                                    heightPercent = newWidth,
                                    windowSizePercent = newWidth
                                ))
                            } else {
                                onConfigChange(config.copy(
                                    widthPercent = newWidth,
                                    windowSizePercent = newWidth
                                ))
                            }
                        }
                    )

                    // 高度滑块（仅非锁定比例时独立控制）
                    AnimatedVisibility(
                        visible = !config.lockAspectRatio,
                        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        SliderWithLabel(
                            label = Strings.fwHeightLabel,
                            value = config.heightPercent,
                            valueRange = 30f..100f,
                            steps = 13,
                            onValueChange = { newHeight ->
                                onConfigChange(config.copy(heightPercent = newHeight))
                            }
                        )
                    }

                    // 锁定宽高比开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AspectRatio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                Strings.fwLockAspectRatio,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        PremiumSwitch(
                            checked = config.lockAspectRatio,
                            onCheckedChange = { locked ->
                                if (locked) {
                                    // 锁定时同步高度到宽度
                                    onConfigChange(config.copy(
                                        lockAspectRatio = true,
                                        heightPercent = config.widthPercent
                                    ))
                                } else {
                                    onConfigChange(config.copy(lockAspectRatio = false))
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    // ════════════════════════════════════════
                    // 区域 2: 外观样式
                    // ════════════════════════════════════════
                    SectionHeader(
                        icon = Icons.Outlined.Palette,
                        title = Strings.fwSectionAppearance
                    )

                    // 透明度滑块
                    SliderWithLabel(
                        label = Strings.floatingWindowOpacity,
                        value = config.opacity,
                        valueRange = 30f..100f,
                        steps = 6,
                        onValueChange = { onConfigChange(config.copy(opacity = it)) }
                    )

                    // 圆角半径滑块
                    SliderWithLabel(
                        label = Strings.fwCornerRadius,
                        value = config.cornerRadius,
                        valueRange = 0f..32f,
                        steps = 7,
                        suffix = "dp",
                        onValueChange = { onConfigChange(config.copy(cornerRadius = it)) }
                    )

                    // 边框样式选择器
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = Strings.fwBorderStyle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data class BorderOption(
                            val style: FloatingBorderStyle,
                            val icon: ImageVector,
                            val label: String
                        )
                        val borderOptions = listOf(
                            BorderOption(FloatingBorderStyle.NONE, Icons.Outlined.DoNotDisturb, Strings.fwBorderNone),
                            BorderOption(FloatingBorderStyle.SUBTLE, Icons.Outlined.CropSquare, Strings.fwBorderSubtle),
                            BorderOption(FloatingBorderStyle.GLOW, Icons.Outlined.AutoAwesome, Strings.fwBorderGlow),
                            BorderOption(FloatingBorderStyle.ACCENT, Icons.Outlined.Palette, Strings.fwBorderAccent)
                        )
                        borderOptions.forEach { option ->
                            val isSelected = config.borderStyle == option.style
                            FilterChip(
                                selected = isSelected,
                                onClick = { onConfigChange(config.copy(borderStyle = option.style)) },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        option.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    // ════════════════════════════════════════
                    // 区域 3: 行为控制
                    // ════════════════════════════════════════
                    SectionHeader(
                        icon = Icons.Outlined.Tune,
                        title = Strings.fwSectionBehavior
                    )

                    // 显示标题栏
                    ToggleRow(
                        title = Strings.floatingWindowShowTitleBar,
                        subtitle = Strings.floatingWindowShowTitleBarDesc,
                        checked = config.showTitleBar,
                        onCheckedChange = { onConfigChange(config.copy(showTitleBar = it)) }
                    )

                    // 自动隐藏标题栏
                    AnimatedVisibility(
                        visible = config.showTitleBar,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        ToggleRow(
                            title = Strings.fwAutoHideTitleBar,
                            subtitle = Strings.fwAutoHideTitleBarDesc,
                            checked = config.autoHideTitleBar,
                            onCheckedChange = { onConfigChange(config.copy(autoHideTitleBar = it)) },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // 边缘吸附
                    ToggleRow(
                        title = Strings.fwEdgeSnapping,
                        subtitle = Strings.fwEdgeSnappingDesc,
                        checked = config.edgeSnapping,
                        onCheckedChange = { onConfigChange(config.copy(edgeSnapping = it)) }
                    )

                    // 缩放手柄
                    ToggleRow(
                        title = Strings.fwResizeHandle,
                        subtitle = Strings.fwResizeHandleDesc,
                        checked = config.showResizeHandle,
                        onCheckedChange = { onConfigChange(config.copy(showResizeHandle = it)) }
                    )

                    // ── 展开更多 ──
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showAdvanced = !showAdvanced },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showAdvanced) Strings.hideAdvanced else Strings.showAdvanced,
                                style = MaterialTheme.typography.labelMedium,
                                color = primary
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(arrowRotation)
                            )
                        }
                    }

                    // 高级设置
                    AnimatedVisibility(
                        visible = showAdvanced,
                        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 启动时最小化
                            ToggleRow(
                                title = Strings.floatingWindowStartMinimized,
                                subtitle = Strings.floatingWindowStartMinimizedDesc,
                                checked = config.startMinimized,
                                onCheckedChange = { onConfigChange(config.copy(startMinimized = it)) }
                            )

                            // 记住位置
                            ToggleRow(
                                title = Strings.floatingWindowRememberPosition,
                                subtitle = Strings.floatingWindowRememberPositionDesc,
                                checked = config.rememberPosition,
                                onCheckedChange = { onConfigChange(config.copy(rememberPosition = it)) }
                            )

                            // 锁定位置
                            ToggleRow(
                                title = Strings.fwLockPosition,
                                subtitle = Strings.fwLockPositionDesc,
                                checked = config.lockPosition,
                                onCheckedChange = { onConfigChange(config.copy(lockPosition = it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════
// 内部辅助组件
// ══════════════════════════════════════════

/**
 * 带标签和数值显示的滑块
 */
@Composable
private fun SliderWithLabel(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Int) -> Unit,
    suffix: String = "%"
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = primary.copy(alpha = 0.1f)
            ) {
                Text(
                    "${value}${suffix}",
                    style = MaterialTheme.typography.labelMedium,
                    color = primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 开关行组件
 */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(1.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PremiumSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 区域标题组件（图标 + 文字，纯色单色风格）
 */
@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = primary
        )
    }
}
