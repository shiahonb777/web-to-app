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
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ViewList
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.design.*
import com.webtoapp.ui.viewmodel.EditState
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LongPressMenuCard(
    style: LongPressMenuStyle,
    onStyleChange: (LongPressMenuStyle) -> Unit
) {
    val enabled = style != LongPressMenuStyle.DISABLED

    data class StyleOption(
        val style: LongPressMenuStyle,
        val name: String,
        val desc: String,
        val icon: ImageVector,
        val accentColor: Color
    )

    val styleOptions = listOf(
        StyleOption(LongPressMenuStyle.FULL, Strings.longPressMenuStyleFull, Strings.longPressMenuStyleFullDesc, Icons.AutoMirrored.Outlined.ViewList, com.webtoapp.ui.theme.AppColors.NeutralAccent),
        StyleOption(LongPressMenuStyle.SIMPLE, Strings.longPressMenuStyleSimple, Strings.longPressMenuStyleSimpleDesc, Icons.Outlined.ViewAgenda, com.webtoapp.ui.theme.AppColors.NeutralAccent),
        StyleOption(LongPressMenuStyle.IOS, Strings.longPressMenuStyleIos, Strings.longPressMenuStyleIosDesc, Icons.Outlined.PhoneIphone, com.webtoapp.ui.theme.AppColors.NeutralAccent),
        StyleOption(LongPressMenuStyle.FLOATING, Strings.longPressMenuStyleFloating, Strings.longPressMenuStyleFloatingDesc, Icons.Outlined.BubbleChart, com.webtoapp.ui.theme.AppColors.NeutralAccent),
        StyleOption(LongPressMenuStyle.CONTEXT, Strings.longPressMenuStyleContext, Strings.longPressMenuStyleContextDesc, Icons.Outlined.Mouse, com.webtoapp.ui.theme.AppColors.NeutralAccent)
    )

    val selectedOption = styleOptions.find { it.style == style } ?: styleOptions[0]

    WtaSettingCard {
            WtaToggleRow(
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                title = Strings.longPressMenuSettings,
                checked = enabled,
                onCheckedChange = { checked ->
                    onStyleChange(
                        if (checked) LongPressMenuStyle.FULL
                        else LongPressMenuStyle.DISABLED
                    )
                }
            )

            AnimatedVisibility(
                visible = enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WtaSectionDivider()

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = WtaSpacing.RowHorizontal),
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

            LongPressMenuStyle.DISABLED -> {}
        }
    }
}

@Composable
fun AdBlockCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onRulesChange: (List<String>) -> Unit,
    onToggleEnabledChange: (Boolean) -> Unit = {}
) {
    var newRule by remember { mutableStateOf("") }

    WtaSettingCard {
            WtaToggleRow(
                title = Strings.adBlocking,
                icon = Icons.Outlined.Shield,
                checked = editState.adBlockEnabled,
                onCheckedChange = onEnabledChange
            )

            AnimatedVisibility(
                visible = editState.adBlockEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column {
                WtaSectionDivider()
                WtaStatusBanner(
                    message = Strings.adBlockDescription,
                    tone = WtaStatusTone.Info,
                    modifier = Modifier.padding(
                        horizontal = WtaSpacing.RowHorizontal,
                        vertical = WtaSpacing.ContentGap
                    )
                )

                WtaSectionDivider()
                WtaToggleRow(
                    title = Strings.adBlockToggleEnabled,
                    subtitle = Strings.adBlockToggleDescription,
                    checked = editState.webViewConfig.adBlockToggleEnabled,
                    onCheckedChange = onToggleEnabledChange
                )

                Text(
                    text = Strings.customBlockRules,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        start = WtaSpacing.RowHorizontal,
                        top = WtaSpacing.ContentGap,
                        bottom = WtaSpacing.ContentGap
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WtaSpacing.RowHorizontal),
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
                    WtaSectionDivider()
                    WtaSettingRow(
                        title = rule
                    ) {
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun BrowserAdvancedConfigCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    WtaSettingCard {
        Column {
            WtaChoiceRow(
                title = Strings.advancedSettings,
                subtitle = null,
                icon = Icons.Outlined.SettingsApplications,
                value = "",
                isExpanded = expanded,
                onClick = { expanded = !expanded }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = WtaSpacing.RowHorizontal,
                        vertical = WtaSpacing.ContentGap
                    ),
                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
                ) {

                    WtaSection(
                        title = Strings.sectionWebEngine,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = "JavaScript",
                                subtitle = Strings.enableJavaScript,
                                checked = config.javaScriptEnabled,
                                onCheckedChange = { onConfigChange(config.copy(javaScriptEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.domStorageSetting,
                                subtitle = Strings.domStorageSettingHint,
                                checked = config.domStorageEnabled,
                                onCheckedChange = { onConfigChange(config.copy(domStorageEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.crossOriginIsolationSetting,
                                subtitle = Strings.crossOriginIsolationSettingHint,
                                checked = config.enableCrossOriginIsolation,
                                onCheckedChange = { onConfigChange(config.copy(enableCrossOriginIsolation = it)) }
                            )
                        }
                    }

                    WtaSection(
                        title = Strings.sectionContentDisplay,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.zoomSetting,
                                subtitle = Strings.zoomSettingHint,
                                checked = config.zoomEnabled,
                                onCheckedChange = { onConfigChange(config.copy(zoomEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.fullscreenVideoSetting,
                                subtitle = Strings.fullscreenVideoSettingHint,
                                checked = config.fullscreenEnabled,
                                onCheckedChange = { onConfigChange(config.copy(fullscreenEnabled = it)) }
                            )
                        }
                        ViewportModeSelector(config = config, onConfigChange = onConfigChange)
                    }

                    WtaSection(
                        title = Strings.sectionNavigation,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.swipeRefreshSetting,
                                subtitle = Strings.swipeRefreshSettingHint,
                                checked = config.swipeRefreshEnabled,
                                onCheckedChange = { onConfigChange(config.copy(swipeRefreshEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.externalLinksSetting,
                                subtitle = Strings.externalLinksSettingHint,
                                checked = config.openExternalLinks,
                                onCheckedChange = { onConfigChange(config.copy(openExternalLinks = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.popupBlockerSetting,
                                subtitle = Strings.popupBlockerSettingHint,
                                checked = config.popupBlockerEnabled,
                                onCheckedChange = { onConfigChange(config.copy(popupBlockerEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.showFloatingBackButtonLabel,
                                subtitle = Strings.showFloatingBackButtonHint,
                                checked = config.showFloatingBackButton,
                                onCheckedChange = { onConfigChange(config.copy(showFloatingBackButton = it)) }
                            )
                        }
                    }

                    WtaSection(
                        title = Strings.sectionOfflinePerformance,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
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
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = WtaSpacing.RowHorizontal,
                                        vertical = WtaSpacing.ContentGap
                                    )
                                ) {
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
                        }

                        ErrorPageConfigCard(
                            config = config.errorPageConfig,
                            onConfigChange = { onConfigChange(config.copy(errorPageConfig = it)) }
                        )
                    }

                    WtaSection(
                        title = Strings.sectionDeveloper,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        KeyboardAdjustModeCard(
                            mode = config.keyboardAdjustMode,
                            onModeChange = { onConfigChange(config.copy(keyboardAdjustMode = it)) }
                        )

                        UserScriptsSection(
                            scripts = config.injectScripts,
                            onScriptsChange = { onConfigChange(config.copy(injectScripts = it)) }
                        )
                    }


                    WtaSection(
                        title = Strings.proxySectionTitle,
                        description = Strings.proxySectionSubtitle,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        // 代理模式选择
                        WtaSettingCard {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = WtaSpacing.RowHorizontal,
                                    vertical = WtaSpacing.ContentGap
                                )
                            ) {
                                Text(
                                    text = Strings.proxyModeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val proxyModes = listOf(
                                        "NONE" to Strings.proxyModeNone,
                                        "STATIC" to Strings.proxyModeStatic,
                                        "PAC" to Strings.proxyModePac
                                    )
                                    proxyModes.forEach { (mode, label) ->
                                        FilterChip(
                                            selected = config.proxyMode == mode,
                                            onClick = { onConfigChange(config.copy(proxyMode = mode)) },
                                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                            leadingIcon = if (config.proxyMode == mode) {{
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }} else null
                                        )
                                    }
                                }
                            }
                        }

                        // 静态代理配置
                        AnimatedVisibility(
                            visible = config.proxyMode == "STATIC",
                            enter = CardExpandTransition,
                            exit = CardCollapseTransition
                        ) {
                            WtaSettingCard {
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = WtaSpacing.RowHorizontal,
                                        vertical = WtaSpacing.ContentGap
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
                                ) {
                                    Text(
                                        text = Strings.proxyTypeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("HTTP", "HTTPS", "SOCKS5").forEach { type ->
                                            FilterChip(
                                                selected = config.proxyType == type,
                                                onClick = { onConfigChange(config.copy(proxyType = type)) },
                                                label = { Text(type, style = MaterialTheme.typography.bodySmall) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PremiumTextField(
                                            value = config.proxyHost,
                                            onValueChange = { onConfigChange(config.copy(proxyHost = it.trim())) },
                                            modifier = Modifier.weight(1f),
                                            label = { Text(Strings.proxyHostLabel) },
                                            placeholder = { Text(Strings.proxyHostHint) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )
                                        PremiumTextField(
                                            value = if (config.proxyPort > 0) config.proxyPort.toString() else "",
                                            onValueChange = { input ->
                                                val port = input.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0
                                                onConfigChange(config.copy(proxyPort = port))
                                            },
                                            modifier = Modifier.width(100.dp),
                                            label = { Text(Strings.proxyPortLabel) },
                                            placeholder = { Text(Strings.proxyPortHint) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = config.proxyType == "SOCKS5" || config.proxyType == "HTTPS",
                                        enter = CardExpandTransition,
                                        exit = CardCollapseTransition
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)) {
                                            Text(
                                                text = Strings.proxyAuthLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                PremiumTextField(
                                                    value = config.proxyUsername,
                                                    onValueChange = { onConfigChange(config.copy(proxyUsername = it.trim())) },
                                                    modifier = Modifier.weight(1f),
                                                    label = { Text(Strings.proxyUsernameLabel) },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodySmall
                                                )
                                                PremiumTextField(
                                                    value = config.proxyPassword,
                                                    onValueChange = { onConfigChange(config.copy(proxyPassword = it)) },
                                                    modifier = Modifier.weight(1f),
                                                    label = { Text(Strings.proxyPasswordLabel) },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }

                                    var bypassText by remember(config.proxyBypassRules) {
                                        mutableStateOf(config.proxyBypassRules.joinToString("\n"))
                                    }
                                    Text(
                                        text = Strings.proxyBypassLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = Strings.proxyBypassHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    PremiumTextField(
                                        value = bypassText,
                                        onValueChange = { newText ->
                                            bypassText = newText
                                            val rules = newText.split("\n")
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                            onConfigChange(config.copy(proxyBypassRules = rules))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("*.local\n192.168.0.0/16") },
                                        minLines = 2,
                                        maxLines = 4,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // PAC 代理配置
                        AnimatedVisibility(
                            visible = config.proxyMode == "PAC",
                            enter = CardExpandTransition,
                            exit = CardCollapseTransition
                        ) {
                            WtaSettingCard {
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = WtaSpacing.RowHorizontal,
                                        vertical = WtaSpacing.ContentGap
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
                                ) {
                                    PremiumTextField(
                                        value = config.pacUrl,
                                        onValueChange = { onConfigChange(config.copy(pacUrl = it.trim())) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(Strings.pacUrlLabel) },
                                        placeholder = { Text(Strings.pacUrlHint) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )

                                    var pacBypassText by remember(config.proxyBypassRules) {
                                        mutableStateOf(config.proxyBypassRules.joinToString("\n"))
                                    }
                                    Text(
                                        text = Strings.proxyBypassLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = Strings.proxyBypassHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    PremiumTextField(
                                        value = pacBypassText,
                                        onValueChange = { newText ->
                                            pacBypassText = newText
                                            val rules = newText.split("\n")
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                            onConfigChange(config.copy(proxyBypassRules = rules))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("*.local\n192.168.0.0/16") },
                                        minLines = 2,
                                        maxLines = 4,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Hosts 映射
                        var hostsMappingsText by remember(config.hostsMappings) {
                            mutableStateOf(
                                config.hostsMappings.joinToString("\n") { "${it.ip} ${it.host}" }
                            )
                        }
                        val parsedHostsMappings = remember(hostsMappingsText) {
                            parseHostsMappingsInput(hostsMappingsText)
                        }

                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.hostsMappingTitle,
                                subtitle = when {
                                    config.proxyMode != "NONE" -> Strings.hostsMappingProxyConflict
                                    parsedHostsMappings.isNotEmpty() -> Strings.hostsMappingParsedCount(parsedHostsMappings.size)
                                    else -> Strings.hostsMappingSubtitle
                                },
                                icon = Icons.Outlined.Route,
                                checked = config.hostsMappingEnabled,
                                enabled = config.proxyMode == "NONE",
                                onCheckedChange = { enabled ->
                                    onConfigChange(config.copy(hostsMappingEnabled = enabled))
                                }
                            )

                            AnimatedVisibility(
                                visible = config.hostsMappingEnabled || hostsMappingsText.isNotBlank(),
                                enter = CardExpandTransition,
                                exit = CardCollapseTransition
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = WtaSpacing.RowHorizontal,
                                        vertical = WtaSpacing.ContentGap
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)
                                ) {
                                    Text(
                                        text = Strings.hostsMappingDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Strings.hostsMappingHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    PremiumTextField(
                                        value = hostsMappingsText,
                                        onValueChange = { newText ->
                                            hostsMappingsText = newText
                                            onConfigChange(config.copy(hostsMappings = parseHostsMappingsInput(newText)))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("1.2.3.4 example.com\n203.0.113.10 api.example.com") },
                                        minLines = 4,
                                        maxLines = 8,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = Strings.hostsMappingWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
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

private fun parseHostsMappingsInput(text: String): List<HostMappingEntry> {
    val mappings = linkedMapOf<String, String>()
    text.lineSequence().forEach { rawLine ->
        val line = rawLine.substringBefore('#').trim()
        if (line.isBlank()) return@forEach
        val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return@forEach

        val first = normalizeIpv4(parts.first())
        val last = normalizeIpv4(parts.last())
        val pair = when {
            first != null -> parts.drop(1).firstOrNull()?.let { host -> host to first }
            last != null -> parts.firstOrNull()?.let { host -> host to last }
            else -> null
        } ?: return@forEach

        val host = normalizeHostMappingHost(pair.first) ?: return@forEach
        mappings[host] = pair.second
    }
    return mappings.map { HostMappingEntry(host = it.key, ip = it.value) }
}

private fun normalizeHostMappingHost(raw: String): String? {
    val host = raw.trim().trim('.').lowercase()
    if (host.isBlank()) return null
    if (!host.contains('.')) return null
    if (host.any { it.isWhitespace() || it == ':' || it == '/' }) return null
    return host
}

private fun normalizeIpv4(raw: String): String? {
    val parts = raw.trim().split('.')
    if (parts.size != 4) return null
    val normalized = parts.map { it.toIntOrNull() ?: return null }
    if (normalized.any { it !in 0..255 }) return null
    return normalized.joinToString(".")
}


@Composable
fun ApkExportSettingsCard(
    config: ApkExportConfig,
    onConfigChange: (ApkExportConfig) -> Unit,
    onOpenPermissionConfig: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)) {
        // ── 深度链接 ──────────────────────────────────────────
        WtaSection(
            title = Strings.sectionNavigation,
            headerStyle = WtaSectionHeaderStyle.Quiet
        ) {
            WtaSettingCard {
                WtaToggleRow(
                    title = Strings.deepLinkSetting,
                    subtitle = Strings.deepLinkSettingHint,
                    icon = Icons.Outlined.Link,
                    checked = config.deepLinkEnabled,
                    onCheckedChange = { onConfigChange(config.copy(deepLinkEnabled = it)) }
                )

                AnimatedVisibility(
                    visible = config.deepLinkEnabled,
                    enter = CardExpandTransition,
                    exit = CardCollapseTransition
                ) {
                    var customHostsText by remember(config.customDeepLinkHosts) {
                        mutableStateOf(config.customDeepLinkHosts.joinToString("\n"))
                    }
                    Column(
                        modifier = Modifier.padding(
                            horizontal = WtaSpacing.RowHorizontal,
                            vertical = WtaSpacing.ContentGap
                        )
                    ) {
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
                                onConfigChange(config.copy(customDeepLinkHosts = hosts))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("api.example.com\ncdn.example.com") },
                            minLines = 2,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ── APK 导出核心配置 ──────────────────────────────────────────
        ApkExportSection(
            config = config,
            onConfigChange = onConfigChange,
            onOpenPermissionConfig = onOpenPermissionConfig
        )
    }
}




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

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
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


@Composable
fun HideBrowserToolbarCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    WtaSettingCard {
        WtaToggleRow(
            icon = Icons.Outlined.WebAsset,
            title = Strings.hideBrowserToolbarLabel,
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
    }
}


@Composable
fun FullscreenModeCard(
    enabled: Boolean,
    showStatusBar: Boolean = false,
    showNavigationBar: Boolean = false,
    hideBrowserToolbarInFullscreen: Boolean = true,
    webViewConfig: WebViewConfig = WebViewConfig(),
    onEnabledChange: (Boolean) -> Unit,
    onShowStatusBarChange: (Boolean) -> Unit = {},
    onShowNavigationBarChange: (Boolean) -> Unit = {},
    onHideBrowserToolbarInFullscreenChange: (Boolean) -> Unit = {},
    onWebViewConfigChange: (WebViewConfig) -> Unit = {}
) {
    var statusBarConfigExpanded by remember { mutableStateOf(false) }

    WtaSettingCard {
        WtaToggleRow(
                icon = Icons.Outlined.Fullscreen,
                title = Strings.fullscreenMode,
                checked = enabled,
                onCheckedChange = onEnabledChange
        )

            AnimatedVisibility(
                visible = enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column {
                WtaSectionDivider()
                WtaToggleRow(
                    title = Strings.showStatusBar,
                    subtitle = Strings.showStatusBarHint,
                    checked = showStatusBar,
                    onCheckedChange = onShowStatusBarChange
                )
                WtaSectionDivider()
                WtaToggleRow(
                    title = Strings.showNavigationBar,
                    subtitle = Strings.showNavigationBarHint,
                    checked = showNavigationBar,
                    onCheckedChange = onShowNavigationBarChange
                )

                if (showStatusBar) {
                    WtaSectionDivider()

                    WtaChoiceRow(
                        title = Strings.statusBarStyleConfigLabel,
                        icon = Icons.Outlined.Tune,
                        value = if (statusBarConfigExpanded) Strings.collapse else Strings.expand,
                        onClick = { statusBarConfigExpanded = !statusBarConfigExpanded }
                    )

                    AnimatedVisibility(
                        visible = statusBarConfigExpanded,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            var statusBarModeTab by remember { mutableStateOf(0) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                FilterChip(
                                    selected = statusBarModeTab == 0,
                                    onClick = { statusBarModeTab = 0 },
                                    label = { Text(Strings.statusBarLightModeLabel) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = statusBarModeTab == 1,
                                    onClick = { statusBarModeTab = 1 },
                                    label = { Text(Strings.statusBarDarkModeLabel) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (statusBarModeTab == 0) {
                                StatusBarConfigCard(
                                    config = webViewConfig,
                                    onConfigChange = onWebViewConfigChange
                                )
                            } else {

                                StatusBarConfigCard(
                                    config = webViewConfig.copy(
                                        statusBarColorMode = webViewConfig.statusBarColorModeDark,
                                        statusBarColor = webViewConfig.statusBarColorDark,
                                        statusBarDarkIcons = webViewConfig.statusBarDarkIconsDark,
                                        statusBarBackgroundType = webViewConfig.statusBarBackgroundTypeDark,
                                        statusBarBackgroundImage = webViewConfig.statusBarBackgroundImageDark,
                                        statusBarBackgroundAlpha = webViewConfig.statusBarBackgroundAlphaDark
                                    ),
                                    onConfigChange = { newConfig ->
                                        onWebViewConfigChange(
                                            webViewConfig.copy(
                                                statusBarColorModeDark = newConfig.statusBarColorMode,
                                                statusBarColorDark = newConfig.statusBarColor,
                                                statusBarDarkIconsDark = newConfig.statusBarDarkIcons ?: false,
                                                statusBarBackgroundTypeDark = newConfig.statusBarBackgroundType,
                                                statusBarBackgroundImageDark = newConfig.statusBarBackgroundImage,
                                                statusBarBackgroundAlphaDark = newConfig.statusBarBackgroundAlpha
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
              }
            }
    }
}




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
    val isCustomOrientation = orientationMode != com.webtoapp.data.model.OrientationMode.PORTRAIT
    var advancedExpanded by remember { mutableStateOf(

        orientationMode in listOf(
            com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT,
            com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE,
            com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT,
            com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE
        )
    ) }

    WtaSettingCard {
        WtaToggleRow(
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
                    WtaSectionDivider()
                    WtaStatusBanner(
                        message = Strings.orientationModeHint,
                        tone = WtaStatusTone.Info,
                        modifier = Modifier.padding(
                            horizontal = WtaSpacing.RowHorizontal,
                            vertical = WtaSpacing.ContentGap
                        )
                    )

                    Text(
                        text = Strings.orientationBasicLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = WtaSpacing.RowHorizontal,
                            top = WtaSpacing.ContentGap,
                            bottom = WtaSpacing.ContentGap
                        )
                    )

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
                            WtaSectionDivider()
                        }
                    }

                    WtaSectionDivider()
                    WtaChoiceRow(
                        title = Strings.orientationAdvancedLabel,
                        icon = Icons.Outlined.Tune,
                        value = if (advancedExpanded) Strings.collapse else Strings.expand,
                        onClick = { advancedExpanded = !advancedExpanded }
                    )

                    AnimatedVisibility(
                        visible = advancedExpanded,
                        enter = CardExpandTransition,
                        exit = CardCollapseTransition
                    ) {
                        Column {
                            Text(
                                text = Strings.orientationReversedLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(
                                    start = WtaSpacing.RowHorizontal,
                                    top = WtaSpacing.ContentGap,
                                    bottom = WtaSpacing.ContentGap
                                )
                            )

                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentPortrait,
                                title = Strings.orientationReversePortrait,
                                subtitle = Strings.orientationReversePortraitDesc,
                                selected = orientationMode == com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT,
                                onClick = { onOrientationModeChange(com.webtoapp.data.model.OrientationMode.REVERSE_PORTRAIT) }
                            )
                            WtaSectionDivider()
                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentLandscape,
                                title = Strings.orientationReverseLandscape,
                                subtitle = Strings.orientationReverseLandscapeDesc,
                                selected = orientationMode == com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE,
                                onClick = { onOrientationModeChange(com.webtoapp.data.model.OrientationMode.REVERSE_LANDSCAPE) }
                            )

                            WtaSectionDivider()
                            Text(
                                text = Strings.orientationSensorLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(
                                    start = WtaSpacing.RowHorizontal,
                                    top = WtaSpacing.ContentGap,
                                    bottom = WtaSpacing.ContentGap
                                )
                            )

                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentPortrait,
                                title = Strings.orientationSensorPortrait,
                                subtitle = Strings.orientationSensorPortraitDesc,
                                selected = orientationMode == com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT,
                                onClick = { onOrientationModeChange(com.webtoapp.data.model.OrientationMode.SENSOR_PORTRAIT) }
                            )
                            WtaSectionDivider()
                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentLandscape,
                                title = Strings.orientationSensorLandscape,
                                subtitle = Strings.orientationSensorLandscapeDesc,
                                selected = orientationMode == com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE,
                                onClick = { onOrientationModeChange(com.webtoapp.data.model.OrientationMode.SENSOR_LANDSCAPE) }
                            )
                        }
                    }

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
                                WtaSectionDivider()
                                WtaStatusBanner(
                                    message = currentModeHint,
                                    tone = WtaStatusTone.Info,
                                    modifier = Modifier.padding(
                                        horizontal = WtaSpacing.RowHorizontal,
                                        vertical = WtaSpacing.ContentGap
                                    )
                                )
                            }
                        }
                    }
                }
            }
    }
}


@Composable
private fun OrientationModeItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    WtaSettingRow(
        title = title,
        subtitle = subtitle.takeIf { it.isNotEmpty() },
        icon = icon,
        onClick = onClick
    ) {
            if (selected) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
    }
}

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

    WtaSettingCard {
        WtaToggleRow(
            title = Strings.keepScreenOnLabel,
            icon = Icons.Outlined.Lightbulb,
            checked = isEnabled,
            onCheckedChange = { checked ->
                        if (checked) {
                            onScreenAwakeModeChange(com.webtoapp.data.model.ScreenAwakeMode.ALWAYS)
                        } else {
                            onScreenAwakeModeChange(com.webtoapp.data.model.ScreenAwakeMode.OFF)
                        }
            }
        )

            AnimatedVisibility(
                visible = isEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column {
                    WtaSectionDivider()
                    Text(
                        text = Strings.screenAwakeModeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        modifier = Modifier.padding(
                            start = WtaSpacing.RowHorizontal,
                            top = WtaSpacing.ContentGap,
                            bottom = WtaSpacing.ContentGap
                        )
                    )

                    Column {
                        modeOptions.filter { it.mode != com.webtoapp.data.model.ScreenAwakeMode.OFF }.forEach { option ->
                            val isSelected = screenAwakeMode == option.mode
                            WtaSettingRow(
                                title = option.title,
                                subtitle = option.subtitle,
                                icon = option.icon,
                                onClick = { onScreenAwakeModeChange(option.mode) }
                            ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                            }
                            if (option.mode != com.webtoapp.data.model.ScreenAwakeMode.TIMED) {
                                WtaSectionDivider()
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.TIMED,
                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(300))
                    ) {
                        Column {
                            WtaSectionDivider()
                            WtaSliderRow(
                                title = Strings.screenAwakeTimeoutLabel,
                                value = screenAwakeTimeoutMinutes.toFloat(),
                                onValueChange = { onScreenAwakeTimeoutChange(it.toInt()) },
                                valueLabel = Strings.screenAwakeTimeoutValue(screenAwakeTimeoutMinutes),
                                valueRange = 5f..120f,
                            )

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

                    WtaSectionDivider()
                    WtaSettingRow(
                        title = Strings.screenBrightnessLabel,
                        icon = Icons.Outlined.BrightnessLow
                    ) {
                        Text(
                            text = if (screenBrightness < 0) Strings.screenBrightnessAuto else "${screenBrightness}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }


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


                    AnimatedVisibility(
                        visible = screenBrightness >= 0,
                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(300))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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


                    WtaStatusBanner(
                        message = when (screenAwakeMode) {
                            com.webtoapp.data.model.ScreenAwakeMode.ALWAYS -> Strings.screenAwakeBatteryWarning
                            com.webtoapp.data.model.ScreenAwakeMode.TIMED -> Strings.screenAwakeTimedHint
                            else -> ""
                        },
                        tone = if (screenAwakeMode == com.webtoapp.data.model.ScreenAwakeMode.ALWAYS)
                            WtaStatusTone.Warning
                        else
                            WtaStatusTone.Info,
                        modifier = Modifier.padding(
                            horizontal = WtaSpacing.RowHorizontal,
                            vertical = WtaSpacing.ContentGap
                        )
                    )
                }
            }
    }
}




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


                            Spacer(modifier = Modifier.height(12.dp))

                            SettingsSwitch(
                                title = Strings.errorPageMiniGameLabel,
                                subtitle = Strings.errorPageMiniGameDesc,
                                checked = config.showMiniGame,
                                onCheckedChange = { onConfigChange(config.copy(showMiniGame = it)) }
                            )


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


/**
 * 特殊设置卡片（抽屉式）：
 * - 默认收起，只显示标题行 + 图标
 * - 点击展开后显示 25 个开关，按逻辑分组
 * - 与 BrowserAdvancedConfigCard 的交互模式完全一致
 */
@Composable
fun SpecialSettingsCard(
    config: com.webtoapp.data.model.WebViewConfig,
    onConfigChange: (com.webtoapp.data.model.WebViewConfig) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    WtaSettingCard {
        Column {
            WtaChoiceRow(
                title = Strings.specialSettingsTitle,
                subtitle = null,
                icon = Icons.Outlined.Science,
                value = "",
                isExpanded = expanded,
                onClick = { expanded = !expanded }
            )

            AnimatedVisibility(
                visible = expanded,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = WtaSpacing.RowHorizontal,
                        vertical = WtaSpacing.ContentGap
                    ),
                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
                ) {

                    // ── 链接与导航 ──────────────────────────────────────────
                    WtaSection(
                        title = Strings.specialSectionLinksNav,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.decodeBase64DeepLinksTitle,
                                subtitle = Strings.decodeBase64DeepLinksDesc,
                                icon = Icons.Outlined.Link,
                                checked = config.decodeBase64DeepLinks,
                                onCheckedChange = { onConfigChange(config.copy(decodeBase64DeepLinks = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.oauthExternalTitle,
                                subtitle = Strings.oauthExternalDesc,
                                icon = Icons.Outlined.OpenInNew,
                                checked = config.enableOAuthExternalRedirect,
                                onCheckedChange = { onConfigChange(config.copy(enableOAuthExternalRedirect = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.jsCanOpenWindowsTitle,
                                subtitle = Strings.jsCanOpenWindowsDesc,
                                icon = Icons.Outlined.OpenInBrowser,
                                checked = config.javaScriptCanOpenWindows,
                                onCheckedChange = { onConfigChange(config.copy(javaScriptCanOpenWindows = it)) }
                            )
                        }
                    }

                    // ── 媒体与内容 ──────────────────────────────────────────
                    WtaSection(
                        title = Strings.specialSectionMedia,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.mediaAutoplayTitle,
                                subtitle = Strings.mediaAutoplayDesc,
                                icon = Icons.Outlined.PlayCircle,
                                checked = config.mediaAutoplayEnabled,
                                onCheckedChange = { onConfigChange(config.copy(mediaAutoplayEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.imageRepairTitle,
                                subtitle = Strings.imageRepairDesc,
                                icon = Icons.Outlined.Image,
                                checked = config.enableImageRepair,
                                onCheckedChange = { onConfigChange(config.copy(enableImageRepair = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.scrollMemoryTitle,
                                subtitle = Strings.scrollMemoryDesc,
                                icon = Icons.Outlined.BookmarkBorder,
                                checked = config.enableScrollMemory,
                                onCheckedChange = { onConfigChange(config.copy(enableScrollMemory = it)) }
                            )
                        }
                    }

                    // ── 安全与网络 ──────────────────────────────────────────
                    WtaSection(
                        title = Strings.specialSectionSecurity,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.kernelDisguiseTitle,
                                subtitle = Strings.kernelDisguiseDesc,
                                icon = Icons.Outlined.Security,
                                checked = config.enableKernelDisguise,
                                onCheckedChange = { onConfigChange(config.copy(enableKernelDisguise = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.httpsUpgradeTitle,
                                subtitle = Strings.httpsUpgradeDesc,
                                icon = Icons.Outlined.Lock,
                                checked = config.enableHttpsUpgrade,
                                onCheckedChange = { onConfigChange(config.copy(enableHttpsUpgrade = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.safeBrowsingTitle,
                                subtitle = Strings.safeBrowsingDesc,
                                icon = Icons.Outlined.Shield,
                                checked = config.safeBrowsingEnabled,
                                onCheckedChange = { onConfigChange(config.copy(safeBrowsingEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.mixedContentTitle,
                                subtitle = Strings.mixedContentDesc,
                                icon = Icons.Outlined.Http,
                                checked = config.allowMixedContent,
                                onCheckedChange = { onConfigChange(config.copy(allowMixedContent = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.privateNetworkBridgeTitle,
                                subtitle = Strings.privateNetworkBridgeDesc,
                                icon = Icons.Outlined.Lan,
                                checked = config.enablePrivateNetworkBridge,
                                onCheckedChange = { onConfigChange(config.copy(enablePrivateNetworkBridge = it)) }
                            )
                        }
                    }

                    // ── 隐私保护 ──────────────────────────────────────────
                    WtaSection(
                        title = Strings.specialSectionPrivacy,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.gpcTitle,
                                subtitle = Strings.gpcDesc,
                                icon = Icons.Outlined.PrivacyTip,
                                checked = config.enableGpc,
                                onCheckedChange = { onConfigChange(config.copy(enableGpc = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.cookieConsentBlockTitle,
                                subtitle = Strings.cookieConsentBlockDesc,
                                icon = Icons.Outlined.DoNotDisturb,
                                checked = config.enableCookieConsentBlock,
                                onCheckedChange = { onConfigChange(config.copy(enableCookieConsentBlock = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.referrerPolicyTitle,
                                subtitle = Strings.referrerPolicyDesc,
                                icon = Icons.Outlined.Policy,
                                checked = config.enableReferrerPolicy,
                                onCheckedChange = { onConfigChange(config.copy(enableReferrerPolicy = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.trackerBlockingTitle,
                                subtitle = Strings.trackerBlockingDesc,
                                icon = Icons.Outlined.RemoveCircleOutline,
                                checked = config.enableTrackerBlocking,
                                onCheckedChange = { onConfigChange(config.copy(enableTrackerBlocking = it)) }
                            )
                        }
                    }

                    // ── Cookie 与存储 ──────────────────────────────────────────
                    WtaSection(
                        title = Strings.specialSectionStorage,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.thirdPartyCookiesTitle,
                                subtitle = Strings.thirdPartyCookiesDesc,
                                icon = Icons.Outlined.Cookie,
                                checked = config.acceptThirdPartyCookies,
                                onCheckedChange = { onConfigChange(config.copy(acceptThirdPartyCookies = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.databaseStorageTitle,
                                subtitle = Strings.databaseStorageDesc,
                                icon = Icons.Outlined.Storage,
                                checked = config.databaseEnabled,
                                onCheckedChange = { onConfigChange(config.copy(databaseEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.cookiePersistenceTitle,
                                subtitle = Strings.cookiePersistenceDesc,
                                icon = Icons.Outlined.Save,
                                checked = config.enableCookiePersistence,
                                onCheckedChange = { onConfigChange(config.copy(enableCookiePersistence = it)) }
                            )
                        }
                    }

                    // ── 兼容性与补丁 ──────────────────────────────────────────
                    WtaSection(
                        title = Strings.specialSectionPolyfills,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.clipboardPolyfillTitle,
                                subtitle = Strings.clipboardPolyfillDesc,
                                icon = Icons.Outlined.ContentPaste,
                                checked = config.enableClipboardPolyfill,
                                onCheckedChange = { onConfigChange(config.copy(enableClipboardPolyfill = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.notificationPolyfillTitle,
                                subtitle = Strings.notificationPolyfillDesc,
                                icon = Icons.Outlined.Notifications,
                                checked = config.enableNotificationPolyfill,
                                onCheckedChange = { onConfigChange(config.copy(enableNotificationPolyfill = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.orientationPolyfillTitle,
                                subtitle = Strings.orientationPolyfillDesc,
                                icon = Icons.Outlined.ScreenRotation,
                                checked = config.enableOrientationPolyfill,
                                onCheckedChange = { onConfigChange(config.copy(enableOrientationPolyfill = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.compatPolyfillsTitle,
                                subtitle = Strings.compatPolyfillsDesc,
                                icon = Icons.Outlined.Build,
                                checked = config.enableCompatPolyfills,
                                onCheckedChange = { onConfigChange(config.copy(enableCompatPolyfills = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.nativeBridgeTitle,
                                subtitle = Strings.nativeBridgeDesc,
                                icon = Icons.Outlined.Api,
                                checked = config.enableNativeBridge,
                                onCheckedChange = { onConfigChange(config.copy(enableNativeBridge = it)) }
                            )
                        }
                    }

                    // ── 其他 ──────────────────────────────────────────
                    WtaSection(
                        title = Strings.specialSectionMisc,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
                            WtaToggleRow(
                                title = Strings.geolocationTitle,
                                subtitle = Strings.geolocationDesc,
                                icon = Icons.Outlined.LocationOn,
                                checked = config.geolocationEnabled,
                                onCheckedChange = { onConfigChange(config.copy(geolocationEnabled = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.blobDownloadTitle,
                                subtitle = Strings.blobDownloadDesc,
                                icon = Icons.Outlined.CloudDownload,
                                checked = config.enableBlobDownloadInterception,
                                onCheckedChange = { onConfigChange(config.copy(enableBlobDownloadInterception = it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
