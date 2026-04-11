package com.webtoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.viewmodel.EditState
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition

/**
 * 长按菜单设置卡片 - 精简优雅版 + iOS 丝滑动画
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LongPressMenuCard(
    style: LongPressMenuStyle,
    onStyleChange: (LongPressMenuStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // 箭头旋转动画 — iOS 风格弹簧
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "arrowRotation"
    )

    // 展开内容透明度动画 — 渐显效果
    val contentAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (expanded) 400 else 200,
            easing = FastOutSlowInEasing
        ),
        label = "contentAlpha"
    )

    // 样式选项配置
    data class StyleOption(
        val style: LongPressMenuStyle,
        val name: String,
        val desc: String,
        val icon: ImageVector,
        val accentColor: Color
    )

    val styleOptions = listOf(
        StyleOption(LongPressMenuStyle.FULL, Strings.longPressMenuStyleFull, Strings.longPressMenuStyleFullDesc, Icons.Outlined.ViewList, Color(0xFF6366F1)),
        StyleOption(LongPressMenuStyle.SIMPLE, Strings.longPressMenuStyleSimple, Strings.longPressMenuStyleSimpleDesc, Icons.Outlined.ViewAgenda, Color(0xFF22C55E)),
        StyleOption(LongPressMenuStyle.IOS, Strings.longPressMenuStyleIos, Strings.longPressMenuStyleIosDesc, Icons.Outlined.PhoneIphone, Color(0xFF3B82F6)),
        StyleOption(LongPressMenuStyle.FLOATING, Strings.longPressMenuStyleFloating, Strings.longPressMenuStyleFloatingDesc, Icons.Outlined.BubbleChart, Color(0xFFF97316)),
        StyleOption(LongPressMenuStyle.CONTEXT, Strings.longPressMenuStyleContext, Strings.longPressMenuStyleContextDesc, Icons.Outlined.Mouse, Color(0xFF8B5CF6)),
        StyleOption(LongPressMenuStyle.DISABLED, Strings.longPressMenuStyleDisabled, Strings.longPressMenuStyleDisabledDesc, Icons.Outlined.Block, Color(0xFF9CA3AF))
    )

    val selectedOption = styleOptions.find { it.style == style } ?: styleOptions[0]
    val isEnabled = style != LongPressMenuStyle.DISABLED

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(weight = 1f, fill = true)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ListAlt,
                            null,
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.longPressMenuSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        AnimatedContent(
                            targetState = selectedOption,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                            },
                            label = "styleLabelCrossfade"
                        ) { option ->
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = option.accentColor
                            )
                        }
                    }
                }
                // 旋转箭头
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = arrowRotation
                    }
                )
            }

            // 展开内容 — iOS 弹簧物理动画
            AnimatedVisibility(
                visible = expanded,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 样式选择 - FlowRow 紧凑布局
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styleOptions.forEach { option ->
                            val isSelected = option.style == style
                            PremiumFilterChip(
                                selected = isSelected,
                                onClick = { onStyleChange(option.style) },
                                label = { Text(option.name) },
                                leadingIcon = {
                                    Icon(
                                        option.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) option.accentColor
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    // 选中样式的描述 + 预览 — 带 crossfade 切换
                    Crossfade(
                        targetState = selectedOption,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        ),
                        label = "styleDetailCrossfade"
                    ) { currentOption ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current)
                                Color.White.copy(alpha = 0.06f)
                            else
                                currentOption.accentColor.copy(alpha = 0.04f),
                            border = BorderStroke(
                                1.dp,
                                currentOption.accentColor.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 描述文字
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = currentOption.accentColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            currentOption.icon,
                                            contentDescription = null,
                                            tint = currentOption.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                        Text(
                                            text = currentOption.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = currentOption.accentColor
                                        )
                                    }
                                }

                                // 预览（非禁用状态时显示）
                                if (currentOption.style != LongPressMenuStyle.DISABLED) {
                                    LongPressMenuStylePreview(
                                        style = currentOption.style,
                                        accentColor = currentOption.accentColor
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

/**
 * 长按菜单样式预览组件 —— 紧凑精美版
 */
@Composable
private fun LongPressMenuStylePreview(
    style: LongPressMenuStyle,
    accentColor: Color
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current)
                    Color.White.copy(alpha = 0.05f)
                else
                    Color.White.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LongPressMenuStyle.FULL, LongPressMenuStyle.SIMPLE -> {
                // BottomSheet 预览
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(3.dp)
                                    .background(
                                        color = onSurfaceColor.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            repeat(if (style == LongPressMenuStyle.FULL) 3 else 2) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(accentColor.copy(alpha = 0.2f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(weight = 1f, fill = true)
                                            .height(10.dp)
                                            .background(
                                                onSurfaceColor.copy(alpha = 0.12f),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LongPressMenuStyle.IOS -> {
                // iOS 风格预览
                Surface(
                    modifier = Modifier.width(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 6.dp
                ) {
                    Column {
                        repeat(3) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(10.dp)
                                        .background(
                                            onSurfaceColor.copy(alpha = 0.15f),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            accentColor.copy(alpha = 0.25f),
                                            CircleShape
                                        )
                                )
                            }
                            if (index < 2) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 14.dp),
                                    color = onSurfaceColor.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }

            LongPressMenuStyle.FLOATING -> {
                // 悬浮气泡风格预览
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = CircleShape,
                        color = accentColor,
                        tonalElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val positions = listOf(
                        Alignment.TopCenter, Alignment.CenterStart,
                        Alignment.CenterEnd, Alignment.BottomCenter
                    )
                    val icons = listOf(
                        Icons.Default.Download, Icons.Default.ContentCopy,
                        Icons.Default.Share, Icons.Default.OpenInBrowser
                    )
                    positions.forEachIndexed { idx, align ->
                        Surface(
                            modifier = Modifier
                                .align(align)
                                .padding(
                                    when (align) {
                                        Alignment.TopCenter -> PaddingValues(top = 4.dp)
                                        Alignment.BottomCenter -> PaddingValues(bottom = 4.dp)
                                        Alignment.CenterStart -> PaddingValues(start = 16.dp)
                                        Alignment.CenterEnd -> PaddingValues(end = 16.dp)
                                        else -> PaddingValues(0.dp)
                                    }
                                ),
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.65f - idx * 0.08f),
                            tonalElevation = 3.dp
                        ) {
                            Box(
                                modifier = Modifier.size(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icons[idx], null, tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            LongPressMenuStyle.CONTEXT -> {
                // 右键菜单风格预览
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 12.dp)
                            .width(120.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            repeat(4) { index ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index == 0) accentColor.copy(alpha = 0.08f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                if (index == 0) accentColor
                                                else onSurfaceColor.copy(alpha = 0.25f),
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(weight = 1f, fill = true)
                                            .height(8.dp)
                                            .background(
                                                onSurfaceColor.copy(alpha = 0.15f),
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LongPressMenuStyle.DISABLED -> {
                // 不显示预览
            }
        }
    }
}

/**
 * 广告拦截卡片
 */
@Composable
fun AdBlockCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onRulesChange: (List<String>) -> Unit,
    onToggleEnabledChange: (Boolean) -> Unit = {}
) {
    var newRule by remember { mutableStateOf("") }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (editState.adBlockEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            null,
                            tint = if (editState.adBlockEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.adBlocking,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                PremiumSwitch(
                    checked = editState.adBlockEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(
                visible = editState.adBlockEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = Strings.adBlockDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Allow用户切换广告拦截
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.adBlockToggleEnabled,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Strings.adBlockToggleDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PremiumSwitch(
                        checked = editState.webViewConfig.adBlockToggleEnabled,
                        onCheckedChange = onToggleEnabledChange
                    )
                }

                Text(
                    text = Strings.customBlockRules,
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PremiumTextField(
                        value = newRule,
                        onValueChange = { newRule = it },
                        placeholder = { Text(Strings.adBlockRuleHint) },
                        singleLine = true,
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (newRule.isNotBlank()) {
                                onRulesChange(editState.adBlockRules + newRule)
                                newRule = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, Strings.add)
                    }
                }

                editState.adBlockRules.forEachIndexed { index, rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        )
                        IconButton(
                            onClick = {
                                onRulesChange(editState.adBlockRules.filterIndexed { i, _ -> i != index })
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                Strings.delete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
              }
            }
        }
    }
}

/**
 * WebView configuration卡片
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun WebViewConfigCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit,
    apkExportConfig: ApkExportConfig = ApkExportConfig(),
    onApkExportConfigChange: (ApkExportConfig) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.SettingsApplications,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.advancedSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = Strings.webViewAdvancedConfig,
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
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ═══════════════════════════════════════════
                    // § 1 — Engine
                    // ═══════════════════════════════════════════
                    AdvancedSettingsSection(
                        title = Strings.sectionWebEngine,
                        icon = Icons.Outlined.Memory
                    ) {
                        SettingsSwitch(
                            title = "JavaScript",
                            subtitle = Strings.enableJavaScript,
                            checked = config.javaScriptEnabled,
                            onCheckedChange = { onConfigChange(config.copy(javaScriptEnabled = it)) }
                        )

                        SettingsSwitch(
                            title = Strings.domStorageSetting,
                            subtitle = Strings.domStorageSettingHint,
                            checked = config.domStorageEnabled,
                            onCheckedChange = { onConfigChange(config.copy(domStorageEnabled = it)) }
                        )

                        SettingsSwitch(
                            title = Strings.crossOriginIsolationSetting,
                            subtitle = Strings.crossOriginIsolationSettingHint,
                            checked = config.enableCrossOriginIsolation,
                            onCheckedChange = { onConfigChange(config.copy(enableCrossOriginIsolation = it)) }
                        )
                    }

                    // ═══════════════════════════════════════════
                    // § 2 — Content & Display
                    // ═══════════════════════════════════════════
                    AdvancedSettingsSection(
                        title = Strings.sectionContentDisplay,
                        icon = Icons.Outlined.Visibility
                    ) {
                        SettingsSwitch(
                            title = Strings.zoomSetting,
                            subtitle = Strings.zoomSettingHint,
                            checked = config.zoomEnabled,
                            onCheckedChange = { onConfigChange(config.copy(zoomEnabled = it)) }
                        )

                        SettingsSwitch(
                            title = Strings.fullscreenVideoSetting,
                            subtitle = Strings.fullscreenVideoSettingHint,
                            checked = config.fullscreenEnabled,
                            onCheckedChange = { onConfigChange(config.copy(fullscreenEnabled = it)) }
                        )

                        SettingsSwitch(
                            title = Strings.hideBrowserToolbarLabel,
                            subtitle = Strings.hideBrowserToolbarHint,
                            checked = config.hideBrowserToolbar,
                            onCheckedChange = { onConfigChange(config.copy(hideBrowserToolbar = it)) }
                        )

                        // 视口适配模式 (inline)
                        ViewportModeSelector(config = config, onConfigChange = onConfigChange)
                    }

                    // ═══════════════════════════════════════════
                    // § 3 — Navigation & Interaction
                    // ═══════════════════════════════════════════
                    AdvancedSettingsSection(
                        title = Strings.sectionNavigation,
                        icon = Icons.Outlined.Navigation
                    ) {
                        SettingsSwitch(
                            title = Strings.swipeRefreshSetting,
                            subtitle = Strings.swipeRefreshSettingHint,
                            checked = config.swipeRefreshEnabled,
                            onCheckedChange = { onConfigChange(config.copy(swipeRefreshEnabled = it)) }
                        )

                        SettingsSwitch(
                            title = Strings.externalLinksSetting,
                            subtitle = Strings.externalLinksSettingHint,
                            checked = config.openExternalLinks,
                            onCheckedChange = { onConfigChange(config.copy(openExternalLinks = it)) }
                        )

                        SettingsSwitch(
                            title = Strings.deepLinkSetting,
                            subtitle = Strings.deepLinkSettingHint,
                            checked = apkExportConfig.deepLinkEnabled,
                            onCheckedChange = { onApkExportConfigChange(apkExportConfig.copy(deepLinkEnabled = it)) }
                        )

                        AnimatedVisibility(
                            visible = apkExportConfig.deepLinkEnabled,
                            enter = CardExpandTransition,
                            exit = CardCollapseTransition
                        ) {
                            var customHostsText by remember(apkExportConfig.customDeepLinkHosts) {
                                mutableStateOf(apkExportConfig.customDeepLinkHosts.joinToString("\n"))
                            }
                            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                                Text(
                                    text = Strings.deepLinkCustomHostsLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = Strings.deepLinkCustomHostsHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                PremiumTextField(
                                    value = customHostsText,
                                    onValueChange = { newText ->
                                        customHostsText = newText
                                        val hosts = newText.split("\n", ",", " ")
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                        onApkExportConfigChange(apkExportConfig.copy(customDeepLinkHosts = hosts))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("api.example.com\ncdn.example.com") },
                                    minLines = 2,
                                    maxLines = 4,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        SettingsSwitch(
                            title = Strings.showFloatingBackButtonLabel,
                            subtitle = Strings.showFloatingBackButtonHint,
                            checked = config.showFloatingBackButton,
                            onCheckedChange = { onConfigChange(config.copy(showFloatingBackButton = it)) }
                        )

                        SettingsSwitch(
                            title = Strings.blockSystemNavigationGestureLabel,
                            subtitle = Strings.blockSystemNavigationGestureHint,
                            checked = config.blockSystemNavigationGesture,
                            onCheckedChange = { onConfigChange(config.copy(blockSystemNavigationGesture = it)) }
                        )
                    }

                    // ═══════════════════════════════════════════
                    // § 4 — Offline & Performance
                    // ═══════════════════════════════════════════
                    AdvancedSettingsSection(
                        title = Strings.sectionOfflinePerformance,
                        icon = Icons.Outlined.CloudOff
                    ) {
                        SettingsSwitch(
                            title = Strings.pwaOfflineTitle,
                            subtitle = Strings.pwaOfflineSubtitle,
                            checked = config.pwaOfflineEnabled,
                            onCheckedChange = { onConfigChange(config.copy(pwaOfflineEnabled = it)) }
                        )

                        AnimatedVisibility(
                            visible = config.pwaOfflineEnabled,
                            enter = CardExpandTransition,
                            exit = CardCollapseTransition
                        ) {
                            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                                Text(
                                    text = Strings.pwaOfflineStrategyLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                val strategies = listOf(
                                    "NETWORK_FIRST" to Strings.pwaStrategyNetworkFirst,
                                    "CACHE_FIRST" to Strings.pwaStrategyCacheFirst,
                                    "STALE_WHILE_REVALIDATE" to Strings.pwaStrategyStaleWhileRevalidate
                                )

                                strategies.forEach { (value, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable { onConfigChange(config.copy(pwaOfflineStrategy = value)) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = config.pwaOfflineStrategy == value,
                                            onClick = { onConfigChange(config.copy(pwaOfflineStrategy = value)) }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // 自定义无网络页面
                        ErrorPageConfigCard(
                            config = config.errorPageConfig,
                            onConfigChange = { onConfigChange(config.copy(errorPageConfig = it)) }
                        )
                    }

                    // ═══════════════════════════════════════════
                    // § 5 — Developer Tools
                    // ═══════════════════════════════════════════
                    AdvancedSettingsSection(
                        title = Strings.sectionDeveloper,
                        icon = Icons.Outlined.Code
                    ) {
                        // 键盘调整模式
                        KeyboardAdjustModeCard(
                            mode = config.keyboardAdjustMode,
                            onModeChange = { onConfigChange(config.copy(keyboardAdjustMode = it)) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 用户脚本
                        UserScriptsSection(
                            scripts = config.injectScripts,
                            onScriptsChange = { onConfigChange(config.copy(injectScripts = it)) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // APK 导出
                        ApkExportSection(
                            config = apkExportConfig,
                            onConfigChange = onApkExportConfigChange
                        )
                    }
                }
            }
        }
    }
}

// ApkExportSection and CustomSigningSection moved to CreateAppApkSection.kt

// ═══════════════════════════════════════════
// Section Group Component for Advanced Settings
// ═══════════════════════════════════════════

/**
 * 高级设置分组容器 — 参考 iOS Settings 的分组美学
 * 每个 section 使用微妙的调色背景 + 小型图标标签头部
 */
@Composable
private fun AdvancedSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = com.webtoapp.ui.theme.LocalIsDarkTheme.current
    val sectionBg = if (isDark)
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Column {
        // Section header — minimal, with micro-accent
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }

        // Section body
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = sectionBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                content = content
            )
        }
    }
}

// ═══════════════════════════════════════════
// Viewport Mode Selector (extracted)
// ═══════════════════════════════════════════

/**
 * 视口适配模式选择器 — 从 WebViewConfigCard 中提取
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewportModeSelector(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    var viewportExpanded by remember { mutableStateOf(false) }
    val currentModeLabel = when (config.viewportMode) {
        ViewportMode.DEFAULT -> Strings.viewportModeDefault
        ViewportMode.FIT_SCREEN -> Strings.viewportModeFitScreen
        ViewportMode.DESKTOP -> Strings.viewportModeDesktop
        ViewportMode.CUSTOM -> if (config.customViewportWidth in 320..3840)
            "${Strings.viewportCustomWidth}: ${config.customViewportWidth}px"
        else Strings.viewportModeCustom
    }

    SettingsSwitch(
        title = Strings.viewportModeTitle,
        subtitle = currentModeLabel,
        checked = viewportExpanded,
        onCheckedChange = { viewportExpanded = it }
    )

    AnimatedVisibility(
        visible = viewportExpanded,
        enter = CardExpandTransition,
        exit = CardCollapseTransition
    ) {
        Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            Text(
                text = Strings.viewportModeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 模式选择 FilterChip
            val viewportOptions = listOf(
                ViewportMode.DEFAULT to Pair(Strings.viewportModeDefault, Icons.Outlined.Web),
                ViewportMode.FIT_SCREEN to Pair(Strings.viewportModeFitScreen, Icons.Outlined.Fullscreen),
                ViewportMode.DESKTOP to Pair(Strings.viewportModeDesktop, Icons.Outlined.DesktopWindows),
                ViewportMode.CUSTOM to Pair(Strings.viewportModeCustom, Icons.Outlined.Tune)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewportOptions.forEach { (mode, pair) ->
                    val (label, icon) = pair
                    val selected = config.viewportMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onConfigChange(config.copy(viewportMode = mode)) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // CUSTOM 模式：预设宽度 + 自定义输入
            if (config.viewportMode == ViewportMode.CUSTOM) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Strings.viewportCustomWidthPresets,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val presets = listOf(
                    320 to "Mobile S",
                    375 to "Mobile",
                    414 to "Mobile L",
                    768 to "Tablet",
                    1024 to "iPad Pro",
                    1280 to "Laptop",
                    1920 to "Desktop"
                )
                val currentWidth = config.customViewportWidth.coerceIn(0, 3840)
                val displayWidth = if (currentWidth == 0) 0 else currentWidth

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (px, label) ->
                        val isSelected = displayWidth == px
                        SuggestionChip(
                            onClick = {
                                onConfigChange(config.copy(customViewportWidth = px))
                            },
                            label = {
                                Text(
                                    text = "$label ($px)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isSelected) BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary
                            ) else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = if (displayWidth == 0) "" else displayWidth.toString(),
                        onValueChange = { input ->
                            val width = input.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
                            val clamped = width.coerceIn(0, 3840)
                            onConfigChange(config.copy(customViewportWidth = clamped))
                        },
                        label = { Text(Strings.viewportCustomWidth) },
                        placeholder = { Text("320-3840") },
                        suffix = { Text("px") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        supportingText = {
                            Text(
                                if (displayWidth in 1..3840) "✓ ${Strings.viewportCustomWidth}: ${displayWidth}px"
                                else Strings.viewportCustomWidthHint
                            )
                        },
                        isError = displayWidth > 0 && (displayWidth < 320 || displayWidth > 3840),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    }
}

/**
 * 浏览器伪装卡片（User-Agent 配置）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserAgentCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isEnabled = config.userAgentMode != UserAgentMode.DEFAULT
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 卡片头部
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
                                if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Language,
                            null,
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.userAgentMode,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isEnabled) config.userAgentMode.displayName else Strings.userAgentDefault,
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
            
            // Expand内容 - 使用 AnimatedVisibility 实现平滑动画
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // 提示文字
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.bypassWebViewDetection,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 浏览器选择
                Text(
                    text = Strings.mobileVersion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 移动版浏览器
                    listOf(
                        UserAgentMode.DEFAULT to Strings.userAgentDefault,
                        UserAgentMode.CHROME_MOBILE to "Chrome",
                        UserAgentMode.SAFARI_MOBILE to "Safari",
                        UserAgentMode.FIREFOX_MOBILE to "Firefox",
                        UserAgentMode.EDGE_MOBILE to "Edge"
                    ).forEach { (mode, name) ->
                        PremiumFilterChip(
                            selected = config.userAgentMode == mode,
                            onClick = { onConfigChange(config.copy(userAgentMode = mode)) },
                            label = { Text(name) },
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = Strings.desktopVersion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 桌面版浏览器
                    listOf(
                        UserAgentMode.CHROME_DESKTOP to "Chrome",
                        UserAgentMode.SAFARI_DESKTOP to "Safari",
                        UserAgentMode.FIREFOX_DESKTOP to "Firefox",
                        UserAgentMode.EDGE_DESKTOP to "Edge"
                    ).forEach { (mode, name) ->
                        PremiumFilterChip(
                            selected = config.userAgentMode == mode,
                            onClick = { onConfigChange(config.copy(userAgentMode = mode)) },
                            label = { Text(name) },
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom选项
                PremiumFilterChip(
                    selected = config.userAgentMode == UserAgentMode.CUSTOM,
                    onClick = { onConfigChange(config.copy(userAgentMode = UserAgentMode.CUSTOM)) },
                    label = { Text(Strings.userAgentCustom) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Edit,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                )
                
                // Custom输入框
                if (config.userAgentMode == UserAgentMode.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumTextField(
                        value = config.customUserAgent ?: "",
                        onValueChange = { onConfigChange(config.copy(customUserAgent = it.ifBlank { null })) },
                        label = { Text("User-Agent") },
                        placeholder = { Text(Strings.userAgentCustomHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )
                }
                
                    // Show当前 User-Agent
                    if (config.userAgentMode != UserAgentMode.DEFAULT && config.userAgentMode != UserAgentMode.CUSTOM) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Strings.currentUserAgent,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = config.userAgentMode.userAgentString ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Status Bar Style Config card — standalone, works without fullscreen mode
 */
@Composable
fun StatusBarStyleCard(
    webViewConfig: WebViewConfig,
    onWebViewConfigChange: (WebViewConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Strings.statusBarStyleConfigLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                val arrowRotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                    label = "statusBarArrow"
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Light / Dark mode tab selector
                    var selectedTab by remember { mutableIntStateOf(0) }
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(Strings.statusBarLightModeLabel) },
                            icon = { Icon(Icons.Outlined.LightMode, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(Strings.statusBarDarkModeLabel) },
                            icon = { Icon(Icons.Outlined.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedTab == 0) {
                        // Light mode - use standard fields directly
                        StatusBarConfigCard(
                            config = webViewConfig,
                            onConfigChange = onWebViewConfigChange
                        )
                    } else {
                        // Dark mode - map dark fields to standard fields for the component
                        val darkMappedConfig = webViewConfig.copy(
                            statusBarColorMode = webViewConfig.statusBarColorModeDark,
                            statusBarColor = webViewConfig.statusBarColorDark,
                            statusBarDarkIcons = webViewConfig.statusBarDarkIconsDark,
                            statusBarBackgroundType = webViewConfig.statusBarBackgroundTypeDark,
                            statusBarBackgroundImage = webViewConfig.statusBarBackgroundImageDark,
                            statusBarBackgroundAlpha = webViewConfig.statusBarBackgroundAlphaDark,
                        )
                        StatusBarConfigCard(
                            config = darkMappedConfig,
                            onConfigChange = { changedConfig ->
                                // Map changed standard fields back to dark fields
                                onWebViewConfigChange(webViewConfig.copy(
                                    statusBarColorModeDark = changedConfig.statusBarColorMode,
                                    statusBarColorDark = changedConfig.statusBarColor,
                                    statusBarDarkIconsDark = changedConfig.statusBarDarkIcons,
                                    statusBarBackgroundTypeDark = changedConfig.statusBarBackgroundType,
                                    statusBarBackgroundImageDark = changedConfig.statusBarBackgroundImage,
                                    statusBarBackgroundAlphaDark = changedConfig.statusBarBackgroundAlpha,
                                ))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 全屏模式卡片
 */
@Composable
fun FullscreenModeCard(
    enabled: Boolean,
    showStatusBar: Boolean = false,
    showNavigationBar: Boolean = false,
    showToolbar: Boolean = false,
    onEnabledChange: (Boolean) -> Unit,
    onShowStatusBarChange: (Boolean) -> Unit = {},
    onShowNavigationBarChange: (Boolean) -> Unit = {},
    onShowToolbarChange: (Boolean) -> Unit = {},
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CollapsibleCardHeader(
                icon = Icons.Outlined.Fullscreen,
                title = Strings.fullscreenMode,
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            
            // Fullscreen模式下显示状态栏选项
            AnimatedVisibility(
                visible = enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.showStatusBar,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Strings.showStatusBarHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumSwitch(
                        checked = showStatusBar,
                        onCheckedChange = onShowStatusBarChange
                    )
                }
                
                // 显示导航栏选项
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.showNavigationBar,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Strings.showNavigationBarHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumSwitch(
                        checked = showNavigationBar,
                        onCheckedChange = onShowNavigationBarChange
                    )
                }
                
                // 显示顶部导航栏选项
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.showToolbar,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = Strings.showToolbarHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    PremiumSwitch(
                        checked = showToolbar,
                        onCheckedChange = onShowToolbarChange
                    )
                }
                
              }
            }
        }
    }
}

/**
 * 屏幕方向模式卡片 — 支持 7 种方向模式的完整选择器
 *
 * 设计理念：
 * - 基础模式（竖屏 / 横屏 / 自动旋转）直接展示
 * - 高级模式（反向竖屏 / 反向横屏 / 感应竖屏 / 感应横屏）折叠展示
 * - 每个模式带图标 + 描述，用户一目了然
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandscapeModeCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    orientationMode: com.webtoapp.data.model.OrientationMode = if (enabled) com.webtoapp.data.model.OrientationMode.LANDSCAPE else com.webtoapp.data.model.OrientationMode.PORTRAIT,
    onOrientationModeChange: (com.webtoapp.data.model.OrientationMode) -> Unit = { mode ->
        onEnabledChange(mode == com.webtoapp.data.model.OrientationMode.LANDSCAPE)
    }
) {
    // 是否非默认竖屏 — 用于控制开关状态
    val isCustomOrientation = orientationMode != com.webtoapp.data.model.OrientationMode.PORTRAIT
    // 高级选项展开状态
    var advancedExpanded by remember { mutableStateOf(
        // 如果当前已选中高级模式，默认展开
        orientationMode in listOf(
            com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT,
            com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE,
            com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT,
            com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE
        )
    ) }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CollapsibleCardHeader(
                icon = Icons.Outlined.ScreenRotation,
                title = Strings.orientationModeLabel,
                checked = isCustomOrientation,
                onCheckedChange = { checked ->
                    if (checked) {
                        onOrientationModeChange(com.webtoapp.data.model.OrientationMode.LANDSCAPE)
                    } else {
                        onOrientationModeChange(com.webtoapp.data.model.OrientationMode.PORTRAIT)
                    }
                }
            )

            AnimatedVisibility(
                visible = isCustomOrientation,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // 提示文字
                    Text(
                        text = Strings.orientationModeHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── 基础模式 ──
                    Text(
                        text = Strings.orientationBasicLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 竖屏 / 横屏 / 自动旋转 — 三个可视化选择卡
                    val basicModes = listOf(
                        Triple(com.webtoapp.data.model.OrientationMode.LANDSCAPE, Icons.Outlined.StayCurrentLandscape, Strings.orientationLandscape),
                        Triple(com.webtoapp.data.model.OrientationMode.AUTO, Icons.Outlined.ScreenRotation, Strings.orientationAuto)
                    )

                    basicModes.forEach { (mode, icon, label) ->
                        OrientationModeItem(
                            icon = icon,
                            title = label,
                            subtitle = when (mode) {
                                com.webtoapp.data.model.OrientationMode.LANDSCAPE -> Strings.orientationLandscapeDesc
                                com.webtoapp.data.model.OrientationMode.AUTO -> Strings.orientationAutoDesc
                                else -> ""
                            },
                            selected = orientationMode == mode,
                            onClick = { onOrientationModeChange(mode) }
                        )
                        if (mode != basicModes.last().first) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // ── 高级模式展开按钮 ──
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { advancedExpanded = !advancedExpanded },
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = Strings.orientationAdvancedLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            val advancedArrowRotation by animateFloatAsState(
                                targetValue = if (advancedExpanded) 180f else 0f,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                                label = "advancedOrientationArrow"
                            )
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.graphicsLayer { rotationZ = advancedArrowRotation }
                            )
                        }
                    }

                    // ── 高级模式内容 ──
                    AnimatedVisibility(
                        visible = advancedExpanded,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))

                            // 反向模式
                            Text(
                                text = Strings.orientationReversedLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentPortrait,
                                title = Strings.orientationReversePortrait,
                                subtitle = Strings.orientationReversePortraitDesc,
                                selected = orientationMode == com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT,
                                onClick = { onOrientationModeChange(com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT) },
                                iconRotation = 180f
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentLandscape,
                                title = Strings.orientationReverseLandscape,
                                subtitle = Strings.orientationReverseLandscapeDesc,
                                selected = orientationMode == com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE,
                                onClick = { onOrientationModeChange(com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE) },
                                iconRotation = 180f
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 感应模式
                            Text(
                                text = Strings.orientationSensorLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentPortrait,
                                title = Strings.orientationSensorPortrait,
                                subtitle = Strings.orientationSensorPortraitDesc,
                                selected = orientationMode == com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT,
                                onClick = { onOrientationModeChange(com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentLandscape,
                                title = Strings.orientationSensorLandscape,
                                subtitle = Strings.orientationSensorLandscapeDesc,
                                selected = orientationMode == com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE,
                                onClick = { onOrientationModeChange(com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE) }
                            )
                        }
                    }

                    // ── 当前模式提示信息 ──
                    val currentModeHint = when (orientationMode) {
                        com.webtoapp.data.model.OrientationMode.AUTO -> Strings.orientationAutoHint
                        com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT -> Strings.orientationSensorPortraitHint
                        com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE -> Strings.orientationSensorLandscapeHint
                        else -> null
                    }

                    AnimatedVisibility(
                        visible = currentModeHint != null,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        if (currentModeHint != null) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = currentModeHint,
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
}

/**
 * 方向模式选择项 — 可复用的单行卡片选择器
 * 支持图标旋转以直观表达方向含义
 */
@Composable
private fun OrientationModeItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconRotation: Float = 0f
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary 
                      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "orientationBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) 
                      else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "orientationBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.75.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = iconRotation }
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // 标题 + 副标
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // 选中指示器
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 保持屏幕常亮卡片 — 高级版
 * 支持 OFF / ALWAYS / TIMED 三种模式
 * 包含超时时间滑块、亮度控制、电量影响提示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepScreenOnCard(
    screenAwakeMode: com.webtoapp.data.model.ScreenAwakeMode,
    onScreenAwakeModeChange: (com.webtoapp.data.model.ScreenAwakeMode) -> Unit,
    screenAwakeTimeoutMinutes: Int,
    onScreenAwakeTimeoutChange: (Int) -> Unit,
    screenBrightness: Int,
    onScreenBrightnessChange: (Int) -> Unit
) {
    val isEnabled = screenAwakeMode != com.webtoapp.data.model.ScreenAwakeMode.OFF
    val primary = MaterialTheme.colorScheme.primary
    
    // 模式选项定义
    data class AwakeModeOption(
        val mode: com.webtoapp.data.model.ScreenAwakeMode,
        val icon: ImageVector,
        val title: String,
        val subtitle: String
    )
    
    val modeOptions = listOf(
        AwakeModeOption(
            mode = com.webtoapp.data.model.ScreenAwakeMode.OFF,
            icon = Icons.Outlined.BedtimeOff,
            title = Strings.screenAwakeOff,
            subtitle = Strings.screenAwakeOffDesc
        ),
        AwakeModeOption(
            mode = com.webtoapp.data.model.ScreenAwakeMode.ALWAYS,
            icon = Icons.Outlined.LightMode,
            title = Strings.screenAwakeAlways,
            subtitle = Strings.screenAwakeAlwaysDesc
        ),
        AwakeModeOption(
            mode = com.webtoapp.data.model.ScreenAwakeMode.TIMED,
            icon = Icons.Outlined.Timer,
            title = Strings.screenAwakeTimed,
            subtitle = Strings.screenAwakeTimedDesc
        )
    )
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                                if (isEnabled) primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = if (isEnabled) primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.keepScreenOnLabel,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                PremiumSwitch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onScreenAwakeModeChange(com.webtoapp.data.model.ScreenAwakeMode.ALWAYS)
                        } else {
                            onScreenAwakeModeChange(com.webtoapp.data.model.ScreenAwakeMode.OFF)
                        }
                    }
                )
            }
            
            // ── 展开面板（仅启用时显示）──
            AnimatedVisibility(
                visible = isEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // ── 模式选择器 ──
                    Text(
                        text = Strings.screenAwakeModeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        modeOptions.filter { it.mode != com.webtoapp.data.model.ScreenAwakeMode.OFF }.forEach { option ->
                            val isSelected = screenAwakeMode == option.mode
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onScreenAwakeModeChange(option.mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected)
                                    primary.copy(alpha = 0.08f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected)
                                    BorderStroke(1.5.dp, primary.copy(alpha = 0.3f))
                                else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 图标
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) primary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // 文本
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) primary
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = option.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // 选中指示器
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // ── 定时模式：超时时间滑块 ──
                    AnimatedVisibility(
                        visible = screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.TIMED,
                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(300))
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.screenAwakeTimeoutLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = Strings.screenAwakeTimeoutValue(screenAwakeTimeoutMinutes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Slider(
                                value = screenAwakeTimeoutMinutes.toFloat(),
                                onValueChange = { onScreenAwakeTimeoutChange(it.toInt()) },
                                valueRange = 5f..120f,
                                steps = 22, // 5, 10, 15, 20, 25, 30 ... 120
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // 预设快捷按钮
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(10, 30, 60, 120).forEach { minutes ->
                                    val isPresetSelected = screenAwakeTimeoutMinutes == minutes
                                    FilterChip(
                                        selected = isPresetSelected,
                                        onClick = { onScreenAwakeTimeoutChange(minutes) },
                                        label = {
                                            Text(
                                                text = Strings.screenAwakeTimeoutValue(minutes),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // ── 亮度控制 ──
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.BrightnessLow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.screenBrightnessLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        Text(
                            text = if (screenBrightness < 0) Strings.screenBrightnessAuto
                                   else "${screenBrightness}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 自动 / 手动切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAuto = screenBrightness < 0
                        FilterChip(
                            selected = isAuto,
                            onClick = { onScreenBrightnessChange(-1) },
                            label = { Text(Strings.screenBrightnessAuto) },
                            leadingIcon = if (isAuto) {
                                {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isAuto,
                            onClick = { if (isAuto) onScreenBrightnessChange(80) },
                            label = { Text(Strings.screenBrightnessManual) },
                            leadingIcon = if (!isAuto) {
                                {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // 手动亮度滑块
                    AnimatedVisibility(
                        visible = screenBrightness >= 0,
                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(300))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BrightnessLow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Slider(
                                value = (if (screenBrightness < 0) 80 else screenBrightness).toFloat(),
                                onValueChange = { onScreenBrightnessChange(it.toInt()) },
                                valueRange = 5f..100f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Icon(
                                imageVector = Icons.Outlined.BrightnessHigh,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // ── 电量影响提示 ──
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.ALWAYS)
                            Color(0xFFFFF3E0)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.ALWAYS)
                                    Icons.Outlined.BatteryAlert
                                else
                                    Icons.Outlined.Info,
                                contentDescription = null,
                                tint = if (screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.ALWAYS)
                                    Color(0xFFE65100)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (screenAwakeMode) {
                                    com.webtoapp.data.model.ScreenAwakeMode.ALWAYS -> Strings.screenAwakeBatteryWarning
                                    com.webtoapp.data.model.ScreenAwakeMode.TIMED -> Strings.screenAwakeTimedHint
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.ALWAYS)
                                    Color(0xFFE65100)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 键盘调整模式 — 内嵌在高级设置中
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardAdjustModeCard(
    mode: com.webtoapp.data.model.KeyboardAdjustMode,
    onModeChange: (com.webtoapp.data.model.KeyboardAdjustMode) -> Unit
) {
    val isCustomized = mode != com.webtoapp.data.model.KeyboardAdjustMode.RESIZE

    Column {
        SettingsSwitch(
            title = Strings.keyboardAdjustModeLabel,
            subtitle = Strings.keyboardAdjustModeHint,
            checked = isCustomized,
            onCheckedChange = { checked ->
                if (checked) {
                    onModeChange(com.webtoapp.data.model.KeyboardAdjustMode.NOTHING)
                } else {
                    onModeChange(com.webtoapp.data.model.KeyboardAdjustMode.RESIZE)
                }
            }
        )

        AnimatedVisibility(
            visible = isCustomized,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        com.webtoapp.data.model.KeyboardAdjustMode.NOTHING to Strings.keyboardAdjustNothing,
                        com.webtoapp.data.model.KeyboardAdjustMode.RESIZE to Strings.keyboardAdjustResize
                    )
                    modes.forEach { (m, label) ->
                        PremiumFilterChip(
                            selected = mode == m,
                            onClick = { onModeChange(m) },
                            label = { Text(label) }
                        )
                    }
                }

                // 当前选中模式的提示
                val hintText = when (mode) {
                    com.webtoapp.data.model.KeyboardAdjustMode.RESIZE -> Strings.keyboardAdjustResizeHint
                    com.webtoapp.data.model.KeyboardAdjustMode.NOTHING -> Strings.keyboardAdjustNothingHint
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = hintText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ==================== Error Page Configuration Card ====================

/**
 * 自定义无网络页面配置卡片
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ErrorPageConfigCard(
    config: com.webtoapp.core.errorpage.ErrorPageConfig,
    onConfigChange: (com.webtoapp.core.errorpage.ErrorPageConfig) -> Unit
) {
    val isCustomized = config.mode != com.webtoapp.core.errorpage.ErrorPageMode.DEFAULT

    Column {
        SettingsSwitch(
            title = Strings.errorPageTitle,
            subtitle = Strings.errorPageSubtitle,
            checked = isCustomized,
            onCheckedChange = { checked ->
                if (checked) {
                    onConfigChange(config.copy(mode = com.webtoapp.core.errorpage.ErrorPageMode.BUILTIN_STYLE))
                } else {
                    onConfigChange(config.copy(mode = com.webtoapp.core.errorpage.ErrorPageMode.DEFAULT))
                }
            }
        )

        AnimatedVisibility(
            visible = isCustomized,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {

                    Text(
                        text = Strings.errorPageSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── 页面模式选择 ──
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            com.webtoapp.core.errorpage.ErrorPageMode.BUILTIN_STYLE to Strings.errorPageModeBuiltIn,
                            com.webtoapp.core.errorpage.ErrorPageMode.CUSTOM_HTML to Strings.errorPageModeCustomHtml,
                            com.webtoapp.core.errorpage.ErrorPageMode.CUSTOM_MEDIA to Strings.errorPageModeCustomMedia
                        )
                        modes.forEach { (mode, label) ->
                            PremiumFilterChip(
                                selected = config.mode == mode,
                                onClick = { onConfigChange(config.copy(mode = mode)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    // ── 内置风格选择 (仅 BUILTIN_STYLE 模式) ──
                    AnimatedVisibility(
                        visible = config.mode == com.webtoapp.core.errorpage.ErrorPageMode.BUILTIN_STYLE,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = Strings.errorPageStyleLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val styles = listOf(
                                    com.webtoapp.core.errorpage.ErrorPageStyle.MATERIAL to Strings.errorPageStyleMaterial,
                                    com.webtoapp.core.errorpage.ErrorPageStyle.SATELLITE to Strings.errorPageStyleSatellite,
                                    com.webtoapp.core.errorpage.ErrorPageStyle.OCEAN to Strings.errorPageStyleOcean,
                                    com.webtoapp.core.errorpage.ErrorPageStyle.FOREST to Strings.errorPageStyleForest,
                                    com.webtoapp.core.errorpage.ErrorPageStyle.MINIMAL to Strings.errorPageStyleMinimal,
                                    com.webtoapp.core.errorpage.ErrorPageStyle.NEON to Strings.errorPageStyleNeon
                                )
                                styles.forEach { (style, label) ->
                                    PremiumFilterChip(
                                        selected = config.builtInStyle == style,
                                        onClick = { onConfigChange(config.copy(builtInStyle = style)) },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            // ── 小游戏开关 ──
                            Spacer(modifier = Modifier.height(12.dp))

                            SettingsSwitch(
                                title = Strings.errorPageMiniGameLabel,
                                subtitle = Strings.errorPageMiniGameDesc,
                                checked = config.showMiniGame,
                                onCheckedChange = { onConfigChange(config.copy(showMiniGame = it)) }
                            )

                            // ── 小游戏类型选择 ──
                            AnimatedVisibility(
                                visible = config.showMiniGame,
                                enter = CardExpandTransition,
                                exit = CardCollapseTransition
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val games = listOf(
                                            com.webtoapp.core.errorpage.MiniGameType.RANDOM to Strings.errorPageGameRandom,
                                            com.webtoapp.core.errorpage.MiniGameType.BREAKOUT to Strings.errorPageGameBreakout,
                                            com.webtoapp.core.errorpage.MiniGameType.MAZE to Strings.errorPageGameMaze,
                                            com.webtoapp.core.errorpage.MiniGameType.STAR_CATCH to Strings.errorPageGameStarCatch,
                                            com.webtoapp.core.errorpage.MiniGameType.INK_ZEN to Strings.errorPageGameInkZen
                                        )
                                        games.forEach { (type, label) ->
                                            PremiumFilterChip(
                                                selected = config.miniGameType == type,
                                                onClick = { onConfigChange(config.copy(miniGameType = type)) },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── 自定义 HTML 输入 (仅 CUSTOM_HTML 模式) ──
                    AnimatedVisibility(
                        visible = config.mode == com.webtoapp.core.errorpage.ErrorPageMode.CUSTOM_HTML,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            PremiumTextField(
                                value = config.customHtml ?: "",
                                onValueChange = { onConfigChange(config.copy(customHtml = it)) },
                                label = { Text(Strings.errorPageModeCustomHtml) },
                                placeholder = { Text(Strings.errorPageCustomHtmlHint) },
                                leadingIcon = { Icon(Icons.Outlined.Code, null) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8,
                                singleLine = false
                            )
                        }
                    }

                    // ── 自定义媒体输入 (仅 CUSTOM_MEDIA 模式) ──
                    AnimatedVisibility(
                        visible = config.mode == com.webtoapp.core.errorpage.ErrorPageMode.CUSTOM_MEDIA,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            PremiumTextField(
                                value = config.customMediaPath ?: "",
                                onValueChange = { onConfigChange(config.copy(customMediaPath = it)) },
                                label = { Text(Strings.errorPageModeCustomMedia) },
                                placeholder = { Text(Strings.errorPageCustomMediaHint) },
                                leadingIcon = { Icon(Icons.Outlined.Image, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // ── 自动重试 ──
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsSwitch(
                        title = Strings.errorPageAutoRetryLabel,
                        subtitle = if (config.autoRetrySeconds > 0)
                            Strings.errorPageAutoRetryDesc.replace("%d", config.autoRetrySeconds.toString())
                        else Strings.errorPageAutoRetryOff,
                        checked = config.autoRetrySeconds > 0,
                        onCheckedChange = { checked ->
                            onConfigChange(config.copy(autoRetrySeconds = if (checked) 15 else 0))
                        }
                    )

                    AnimatedVisibility(
                        visible = config.autoRetrySeconds > 0,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${config.autoRetrySeconds}${Strings.seconds}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Slider(
                                value = config.autoRetrySeconds.toFloat(),
                                onValueChange = {
                                    onConfigChange(config.copy(autoRetrySeconds = it.toInt()))
                                },
                                valueRange = 5f..60f,
                                steps = 10,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
            }
        }
    }
}

