package com.webtoapp.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.design.WtaSwitch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.FloatingBorderStyle
import com.webtoapp.data.model.FloatingWindowAspectRatioMode
import com.webtoapp.data.model.FloatingWindowConfig
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import com.webtoapp.util.IconStorage
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FloatingWindowConfigCard(
    config: FloatingWindowConfig,
    onConfigChange: (FloatingWindowConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvanced by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val aspectRatioMode = if (!config.lockAspectRatio && config.aspectRatioMode == FloatingWindowAspectRatioMode.SCREEN) {
        FloatingWindowAspectRatioMode.FREE
    } else {
        config.aspectRatioMode
    }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val oldPath = config.minimizedIconPath
        IconStorage.saveIconFromUri(context, uri)?.let { path ->
            if (oldPath != path) {
                IconStorage.deleteIcon(oldPath)
            }
            onConfigChange(config.copy(minimizedIconPath = path))
        }
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (showAdvanced) 180f else 0f,
        animationSpec = com.webtoapp.ui.design.WtaMotion.settleSpring(),
        label = "arrowRotation"
    )

    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

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
                WtaSwitch(
                    checked = config.enabled,
                    onCheckedChange = { onConfigChange(config.copy(enabled = it)) }
                )
            }

            AnimatedVisibility(
                visible = config.enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    SectionHeader(
                        icon = Icons.Outlined.Straighten,
                        title = Strings.fwSectionSize
                    )

                    SliderWithLabel(
                        label = Strings.fwWidthLabel,
                        value = config.widthPercent,
                        valueRange = 30f..100f,
                        steps = 13,
                        onValueChange = { newWidth ->
                            onConfigChange(config.copy(
                                widthPercent = newWidth,
                                windowSizePercent = newWidth
                            ))
                        }
                    )

                    AnimatedVisibility(
                        visible = aspectRatioMode == FloatingWindowAspectRatioMode.FREE,
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

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = Strings.fwAspectRatio,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data class AspectOption(
                            val mode: FloatingWindowAspectRatioMode,
                            val label: String
                        )
                        val aspectOptions = listOf(
                            AspectOption(FloatingWindowAspectRatioMode.SCREEN, Strings.fwAspectScreen),
                            AspectOption(FloatingWindowAspectRatioMode.FREE, Strings.fwAspectFree),
                            AspectOption(FloatingWindowAspectRatioMode.RATIO_16_9, "16:9"),
                            AspectOption(FloatingWindowAspectRatioMode.RATIO_9_16, "9:16"),
                            AspectOption(FloatingWindowAspectRatioMode.RATIO_4_3, "4:3"),
                            AspectOption(FloatingWindowAspectRatioMode.SQUARE, "1:1"),
                            AspectOption(FloatingWindowAspectRatioMode.CUSTOM, Strings.fwAspectCustom)
                        )
                        aspectOptions.forEach { option ->
                            FilterChip(
                                selected = aspectRatioMode == option.mode,
                                onClick = {
                                    onConfigChange(config.copy(
                                        aspectRatioMode = option.mode,
                                        lockAspectRatio = option.mode != FloatingWindowAspectRatioMode.FREE
                                    ))
                                },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (aspectRatioMode == option.mode) {
                                    {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = aspectRatioMode == FloatingWindowAspectRatioMode.CUSTOM,
                        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                SliderWithLabel(
                                    label = Strings.fwAspectWidth,
                                    value = config.customAspectRatioWidth.coerceIn(1, 32),
                                    valueRange = 1f..32f,
                                    steps = 30,
                                    suffix = "",
                                    onValueChange = {
                                        onConfigChange(config.copy(customAspectRatioWidth = it.coerceIn(1, 32)))
                                    }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                SliderWithLabel(
                                    label = Strings.fwAspectHeight,
                                    value = config.customAspectRatioHeight.coerceIn(1, 32),
                                    valueRange = 1f..32f,
                                    steps = 30,
                                    suffix = "",
                                    onValueChange = {
                                        onConfigChange(config.copy(customAspectRatioHeight = it.coerceIn(1, 32)))
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    SectionHeader(
                        icon = Icons.Outlined.Palette,
                        title = Strings.fwSectionAppearance
                    )

                    SliderWithLabel(
                        label = Strings.floatingWindowOpacity,
                        value = config.opacity,
                        valueRange = 30f..100f,
                        steps = 6,
                        onValueChange = { onConfigChange(config.copy(opacity = it)) }
                    )

                    SliderWithLabel(
                        label = Strings.fwCornerRadius,
                        value = config.cornerRadius,
                        valueRange = 0f..32f,
                        steps = 7,
                        suffix = "dp",
                        onValueChange = { onConfigChange(config.copy(cornerRadius = it)) }
                    )

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

                    Spacer(Modifier.height(12.dp))
                    FloatingWindowMinimizedIconPicker(
                        iconPath = config.minimizedIconPath,
                        onSelect = { iconPickerLauncher.launch("image/*") },
                        onClear = {
                            IconStorage.deleteIcon(config.minimizedIconPath)
                            onConfigChange(config.copy(minimizedIconPath = null))
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    SectionHeader(
                        icon = Icons.Outlined.Tune,
                        title = Strings.fwSectionBehavior
                    )

                    ToggleRow(
                        title = Strings.floatingWindowShowTitleBar,
                        subtitle = Strings.floatingWindowShowTitleBarDesc,
                        checked = config.showTitleBar,
                        onCheckedChange = { onConfigChange(config.copy(showTitleBar = it)) }
                    )

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

                    ToggleRow(
                        title = Strings.fwEdgeSnapping,
                        subtitle = Strings.fwEdgeSnappingDesc,
                        checked = config.edgeSnapping,
                        onCheckedChange = { onConfigChange(config.copy(edgeSnapping = it)) }
                    )

                    ToggleRow(
                        title = Strings.fwResizeHandle,
                        subtitle = Strings.fwResizeHandleDesc,
                        checked = config.showResizeHandle,
                        onCheckedChange = { onConfigChange(config.copy(showResizeHandle = it)) }
                    )

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

                    AnimatedVisibility(
                        visible = showAdvanced,
                        enter = fadeIn(tween(200)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {

                            ToggleRow(
                                title = Strings.floatingWindowStartMinimized,
                                subtitle = Strings.floatingWindowStartMinimizedDesc,
                                checked = config.startMinimized,
                                onCheckedChange = { onConfigChange(config.copy(startMinimized = it)) }
                            )

                            ToggleRow(
                                title = Strings.floatingWindowRememberPosition,
                                subtitle = Strings.floatingWindowRememberPositionDesc,
                                checked = config.rememberPosition,
                                onCheckedChange = { onConfigChange(config.copy(rememberPosition = it)) }
                            )

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

@Composable
private fun FloatingWindowMinimizedIconPicker(
    iconPath: String?,
    onSelect: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val hasIcon = !iconPath.isNullOrBlank() && File(iconPath).exists()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onSelect() },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (hasIcon) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(iconPath!!))
                        .crossfade(true)
                        .build(),
                    contentDescription = Strings.fwMinimizedIcon,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = Strings.fwDefaultMinimizedIcon,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = Strings.fwMinimizedIcon,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (hasIcon) Strings.fwCustomMinimizedIcon else Strings.fwDefaultMinimizedIcon,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onSelect,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(Strings.fwSelectMinimizedIcon, style = MaterialTheme.typography.labelMedium)
                }
                AnimatedVisibility(
                    visible = hasIcon,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150))
                ) {
                    TextButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(Strings.fwClearMinimizedIcon, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

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
        WtaSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

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
