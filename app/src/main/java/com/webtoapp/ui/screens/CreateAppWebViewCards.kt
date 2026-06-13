package com.webtoapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Login
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.design.*
import com.webtoapp.ui.viewmodel.EditState
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import com.webtoapp.util.MediaStorage
import kotlinx.coroutines.launch

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
                                title = Strings.freshSessionModeTitle,
                                subtitle = Strings.freshSessionModeDesc,
                                checked = config.clearBrowsingDataOnLaunch,
                                onCheckedChange = {
                                    onConfigChange(
                                        config.copy(
                                            clearBrowsingDataOnLaunch = it,
                                            pwaOfflineEnabled = if (it) false else config.pwaOfflineEnabled
                                        )
                                    )
                                }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.pwaOfflineTitle,
                                subtitle = Strings.pwaOfflineSubtitle,
                                checked = config.pwaOfflineEnabled,
                                onCheckedChange = {
                                    onConfigChange(
                                        config.copy(
                                            pwaOfflineEnabled = it,
                                            clearBrowsingDataOnLaunch = if (it) false else config.clearBrowsingDataOnLaunch
                                        )
                                    )
                                }
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
    var showOAuthGuide by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)) {

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

            WtaStatusBanner(
                title = Strings.oauthReturnGuideTitle,
                message = Strings.oauthReturnGuideSummary,
                tone = WtaStatusTone.Info,
                actionLabel = Strings.oauthReturnGuideButton,
                onAction = { showOAuthGuide = true }
            )
        }

        ApkExportSection(
            config = config,
            onConfigChange = onConfigChange,
            onOpenPermissionConfig = onOpenPermissionConfig
        )
    }

    if (showOAuthGuide) {
        OAuthReturnGuideDialog(onDismiss = { showOAuthGuide = false })
    }
}

@Composable
private fun OAuthReturnGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null) },
        title = { Text(Strings.oauthReturnGuideTitle) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Strings.oauthReturnGuideIntro,
                    style = MaterialTheme.typography.bodyMedium
                )
                WtaStatusBanner(
                    message = Strings.oauthReturnGuideReason,
                    tone = WtaStatusTone.Info,
                    messageMaxLines = 8
                )
                Text(
                    text = Strings.oauthReturnGuideSteps,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.understood)
            }
        }
    )
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
    webViewConfig: WebViewConfig = WebViewConfig(),
    onEnabledChange: (Boolean) -> Unit,
    onWebViewConfigChange: (WebViewConfig) -> Unit = {}
) {
    var toolbarOptionsExpanded by remember { mutableStateOf(false) }

    WtaSettingCard {
        WtaToggleRow(
            icon = Icons.Outlined.WebAsset,
            title = Strings.hideBrowserToolbarLabel,
            subtitle = Strings.hideBrowserToolbarHint,
            checked = enabled,
            onCheckedChange = onEnabledChange
        )

        AnimatedVisibility(
            visible = !enabled,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            Column {
                WtaSectionDivider()
                WtaChoiceRow(
                    title = Strings.toolbarContentOptionsLabel,
                    subtitle = Strings.toolbarContentOptionsHint,
                    value = if (toolbarOptionsExpanded) Strings.collapse else Strings.expand,
                    isExpanded = toolbarOptionsExpanded,
                    onClick = { toolbarOptionsExpanded = !toolbarOptionsExpanded }
                )
            }
        }

        AnimatedVisibility(
            visible = !enabled && toolbarOptionsExpanded,
            enter = CardExpandTransition,
            exit = CardCollapseTransition
        ) {
            Column {
                WtaSectionDivider()
                WtaToggleRow(
                    title = Strings.toolbarShowTitleLabel,
                    subtitle = Strings.toolbarShowTitleHint,
                    checked = webViewConfig.toolbarShowTitle,
                    onCheckedChange = { onWebViewConfigChange(webViewConfig.copy(toolbarShowTitle = it)) }
                )
                WtaSectionDivider()
                WtaToggleRow(
                    title = Strings.toolbarShowUrlLabel,
                    subtitle = Strings.toolbarShowUrlHint,
                    checked = webViewConfig.toolbarShowUrl,
                    onCheckedChange = { onWebViewConfigChange(webViewConfig.copy(toolbarShowUrl = it)) }
                )
                WtaSectionDivider()
                WtaToggleRow(
                    title = Strings.toolbarShowBackLabel,
                    checked = webViewConfig.toolbarShowBack,
                    onCheckedChange = { onWebViewConfigChange(webViewConfig.copy(toolbarShowBack = it)) }
                )
                WtaSectionDivider()
                WtaToggleRow(
                    title = Strings.toolbarShowForwardLabel,
                    checked = webViewConfig.toolbarShowForward,
                    onCheckedChange = { onWebViewConfigChange(webViewConfig.copy(toolbarShowForward = it)) }
                )
                WtaSectionDivider()
                WtaToggleRow(
                    title = Strings.toolbarShowRefreshLabel,
                    checked = webViewConfig.toolbarShowRefresh,
                    onCheckedChange = { onWebViewConfigChange(webViewConfig.copy(toolbarShowRefresh = it)) }
                )
            }
        }
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
    val modes = listOf(
        com.webtoapp.data.model.KeyboardAdjustMode.RESIZE to Strings.keyboardAdjustResize,
        com.webtoapp.data.model.KeyboardAdjustMode.NOTHING to Strings.keyboardAdjustNothing
    )
    val hintText = when (mode) {
        com.webtoapp.data.model.KeyboardAdjustMode.RESIZE -> Strings.keyboardAdjustResizeHint
        com.webtoapp.data.model.KeyboardAdjustMode.NOTHING -> Strings.keyboardAdjustNothingHint
    }

    WtaSettingCard {
        Column(
            modifier = Modifier.padding(horizontal = WtaSpacing.RowHorizontal, vertical = WtaSpacing.RowVertical),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = Strings.keyboardAdjustModeLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Strings.keyboardAdjustModeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { (m, label) ->
                    PremiumFilterChip(
                        selected = mode == m,
                        onClick = { onModeChange(m) },
                        label = { Text(label) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            CustomHtmlEditorRow(
                                customHtml = config.customHtml,
                                onCustomHtmlChange = { onConfigChange(config.copy(customHtml = it)) }
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
                            CustomMediaPickerRow(
                                customMediaPath = config.customMediaPath,
                                onCustomMediaPathChange = { onConfigChange(config.copy(customMediaPath = it)) }
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

@OptIn(ExperimentalLayoutApi::class)
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

                    WtaSection(
                        title = Strings.specialBasicSectionTitle,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {
                        WtaSettingCard {
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
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.followSystemDarkModeTitle,
                                subtitle = Strings.followSystemDarkModeDesc,
                                icon = Icons.Outlined.DarkMode,
                                checked = config.followSystemDarkMode,
                                onCheckedChange = { onConfigChange(config.copy(followSystemDarkMode = it)) }
                            )
                            WtaSectionDivider()
                            WtaToggleRow(
                                title = Strings.cookiePersistenceTitle,
                                subtitle = Strings.cookiePersistenceDesc,
                                icon = Icons.Outlined.Save,
                                checked = config.enableCookiePersistence,
                                onCheckedChange = { onConfigChange(config.copy(enableCookiePersistence = it)) }
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
                        }
                    }

                    WtaSection(
                        title = Strings.specialAdvancedSectionTitle,
                        headerStyle = WtaSectionHeaderStyle.Quiet
                    ) {

                        SpecialAdvancedRow(
                            title = Strings.decodeBase64DeepLinksTitle,
                            subtitle = Strings.decodeBase64DeepLinksDesc,
                            icon = Icons.Outlined.Link,
                            checked = config.decodeBase64DeepLinks,
                            onCheckedChange = { onConfigChange(config.copy(decodeBase64DeepLinks = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.base64ModeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.Base64DeepLinkMode.GESTURE_ONLY to Strings.base64ModeGesture,
                                    com.webtoapp.data.model.Base64DeepLinkMode.ALWAYS to Strings.base64ModeAlways
                                ),
                                selected = config.decodeBase64Mode,
                                onSelect = { onConfigChange(config.copy(decodeBase64Mode = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.jsCanOpenWindowsTitle,
                            subtitle = Strings.jsCanOpenWindowsDesc,
                            icon = Icons.Outlined.OpenInBrowser,
                            checked = config.javaScriptCanOpenWindows,
                            onCheckedChange = { onConfigChange(config.copy(javaScriptCanOpenWindows = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.jsOpenPolicyLabel,
                                options = listOf(
                                    com.webtoapp.data.model.JsOpenWindowsPolicy.ALLOW to Strings.jsOpenPolicyAllow,
                                    com.webtoapp.data.model.JsOpenWindowsPolicy.BLOCK to Strings.jsOpenPolicyBlock,
                                    com.webtoapp.data.model.JsOpenWindowsPolicy.PROMPT to Strings.jsOpenPolicyPrompt
                                ),
                                selected = config.jsOpenWindowsPolicy,
                                onSelect = { onConfigChange(config.copy(jsOpenWindowsPolicy = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.mediaAutoplayTitle,
                            subtitle = Strings.mediaAutoplayDesc,
                            icon = Icons.Outlined.PlayCircle,
                            checked = config.mediaAutoplayEnabled,
                            onCheckedChange = { onConfigChange(config.copy(mediaAutoplayEnabled = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.mediaAutoplayScopeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.MediaAutoplayScope.VIDEO_ONLY to Strings.mediaAutoplayScopeVideoOnly,
                                    com.webtoapp.data.model.MediaAutoplayScope.AUDIO_ONLY to Strings.mediaAutoplayScopeAudioOnly,
                                    com.webtoapp.data.model.MediaAutoplayScope.BOTH to Strings.mediaAutoplayScopeBoth
                                ),
                                selected = config.mediaAutoplayScope,
                                onSelect = { onConfigChange(config.copy(mediaAutoplayScope = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.kernelDisguiseTitle,
                            subtitle = Strings.kernelDisguiseDesc,
                            icon = Icons.Outlined.Security,
                            checked = config.enableKernelDisguise,
                            onCheckedChange = { onConfigChange(config.copy(enableKernelDisguise = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.kernelDisguiseLevelLabel,
                                options = listOf(
                                    com.webtoapp.data.model.KernelDisguiseLevel.BASIC to Strings.kernelDisguiseLevelBasic,
                                    com.webtoapp.data.model.KernelDisguiseLevel.STANDARD to Strings.kernelDisguiseLevelStandard,
                                    com.webtoapp.data.model.KernelDisguiseLevel.DEEP to Strings.kernelDisguiseLevelDeep
                                ),
                                selected = config.kernelDisguiseLevel,
                                onSelect = { onConfigChange(config.copy(kernelDisguiseLevel = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.kernelFlavorTitle,
                            subtitle = Strings.kernelFlavorDesc,
                            icon = Icons.Outlined.Public,
                            checked = config.kernelFlavor != com.webtoapp.core.kernel.KernelFlavor.SYSTEM_DEFAULT,
                            onCheckedChange = { enabled ->
                                onConfigChange(
                                    config.copy(
                                        kernelFlavor = if (enabled) {
                                            com.webtoapp.core.kernel.KernelFlavor.BLINK_CHROME
                                        } else {
                                            com.webtoapp.core.kernel.KernelFlavor.SYSTEM_DEFAULT
                                        }
                                    )
                                )
                            }
                        ) {
                            ChoiceChipRow(
                                label = Strings.kernelFlavorLabel,
                                options = listOf(
                                    com.webtoapp.core.kernel.KernelFlavor.BLINK_CHROME to Strings.kernelFlavorChrome,
                                    com.webtoapp.core.kernel.KernelFlavor.BLINK_EDGE to Strings.kernelFlavorEdge,
                                    com.webtoapp.core.kernel.KernelFlavor.BLINK_SAMSUNG to Strings.kernelFlavorSamsung,
                                    com.webtoapp.core.kernel.KernelFlavor.GECKO_FIREFOX to Strings.kernelFlavorFirefox,
                                    com.webtoapp.core.kernel.KernelFlavor.WEBKIT_SAFARI to Strings.kernelFlavorSafari
                                ),
                                selected = config.kernelFlavor,
                                onSelect = { onConfigChange(config.copy(kernelFlavor = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.cloudflareCompatTitle,
                            subtitle = Strings.cloudflareCompatDesc,
                            icon = Icons.Outlined.VerifiedUser,
                            checked = config.enableCloudflareCompat,
                            onCheckedChange = { onConfigChange(config.copy(enableCloudflareCompat = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.cloudflareCompatModeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.CloudflareCompatMode.AUTO_DETECT to Strings.cloudflareCompatModeAuto,
                                    com.webtoapp.data.model.CloudflareCompatMode.ALWAYS_ON to Strings.cloudflareCompatModeAlways
                                ),
                                selected = config.cloudflareCompatMode,
                                onSelect = { onConfigChange(config.copy(cloudflareCompatMode = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.mixedContentTitle,
                            subtitle = Strings.mixedContentDesc,
                            icon = Icons.Outlined.Http,
                            checked = config.allowMixedContent,
                            onCheckedChange = { onConfigChange(config.copy(allowMixedContent = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.mixedContentModeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.MixedContentMode.NEVER to Strings.mixedContentModeNever,
                                    com.webtoapp.data.model.MixedContentMode.COMPATIBILITY to Strings.mixedContentModeCompat,
                                    com.webtoapp.data.model.MixedContentMode.ALWAYS to Strings.mixedContentModeAlways
                                ),
                                selected = config.mixedContentMode,
                                onSelect = { onConfigChange(config.copy(mixedContentMode = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.privateNetworkBridgeTitle,
                            subtitle = Strings.privateNetworkBridgeDesc,
                            icon = Icons.Outlined.Lan,
                            checked = config.enablePrivateNetworkBridge,
                            onCheckedChange = { onConfigChange(config.copy(enablePrivateNetworkBridge = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.privateNetworkScopeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.PrivateNetworkScope.LOCAL_ONLY to Strings.privateNetworkScopeLocal,
                                    com.webtoapp.data.model.PrivateNetworkScope.ALL to Strings.privateNetworkScopeAll
                                ),
                                selected = config.privateNetworkScope,
                                onSelect = { onConfigChange(config.copy(privateNetworkScope = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.thirdPartyCookiesTitle,
                            subtitle = Strings.thirdPartyCookiesDesc,
                            icon = Icons.Outlined.Cookie,
                            checked = config.acceptThirdPartyCookies,
                            onCheckedChange = { onConfigChange(config.copy(acceptThirdPartyCookies = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.thirdPartyCookieModeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.ThirdPartyCookieMode.NONE to Strings.thirdPartyCookieModeNone,
                                    com.webtoapp.data.model.ThirdPartyCookieMode.SAME_SITE_LAX to Strings.thirdPartyCookieModeSameSite,
                                    com.webtoapp.data.model.ThirdPartyCookieMode.ALL to Strings.thirdPartyCookieModeAll
                                ),
                                selected = config.thirdPartyCookieMode,
                                onSelect = { onConfigChange(config.copy(thirdPartyCookieMode = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.nativeBridgeTitle,
                            subtitle = Strings.nativeBridgeDesc,
                            icon = Icons.Outlined.Api,
                            checked = config.enableNativeBridge,
                            onCheckedChange = { onConfigChange(config.copy(enableNativeBridge = it)) }
                        ) {
                            val caps = config.nativeBridgeCapabilities
                            Text(
                                text = Strings.nativeBridgeCapabilitiesTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = caps.clipboard,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(clipboard = !caps.clipboard))) },
                                    label = { Text(Strings.nativeBridgeCapsClipboard) }
                                )
                                FilterChip(
                                    selected = caps.vibration,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(vibration = !caps.vibration))) },
                                    label = { Text(Strings.nativeBridgeCapsVibration) }
                                )
                                FilterChip(
                                    selected = caps.geolocation,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(geolocation = !caps.geolocation))) },
                                    label = { Text(Strings.nativeBridgeCapsGeolocation) }
                                )
                                FilterChip(
                                    selected = caps.brightness,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(brightness = !caps.brightness))) },
                                    label = { Text(Strings.nativeBridgeCapsBrightness) }
                                )
                                FilterChip(
                                    selected = caps.notification,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(notification = !caps.notification))) },
                                    label = { Text(Strings.nativeBridgeCapsNotification) }
                                )
                                FilterChip(
                                    selected = caps.notificationScheduled,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(notificationScheduled = !caps.notificationScheduled))) },
                                    label = { Text(Strings.nativeBridgeCapsNotificationScheduled) }
                                )
                                FilterChip(
                                    selected = caps.notificationPersistent,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(notificationPersistent = !caps.notificationPersistent))) },
                                    label = { Text(Strings.nativeBridgeCapsNotificationPersistent) }
                                )
                                FilterChip(
                                    selected = caps.download,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(download = !caps.download))) },
                                    label = { Text(Strings.nativeBridgeCapsDownload) }
                                )
                                FilterChip(
                                    selected = caps.privateNetwork,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(privateNetwork = !caps.privateNetwork))) },
                                    label = { Text(Strings.nativeBridgeCapsPrivateNetwork) }
                                )
                                FilterChip(
                                    selected = caps.screenWake,
                                    onClick = { onConfigChange(config.copy(nativeBridgeCapabilities = caps.copy(screenWake = !caps.screenWake))) },
                                    label = { Text(Strings.nativeBridgeCapsScreenWake) }
                                )
                            }
                        }

                        SpecialAdvancedRow(
                            title = Strings.geolocationTitle,
                            subtitle = Strings.geolocationDesc,
                            icon = Icons.Outlined.LocationOn,
                            checked = config.geolocationEnabled,
                            onCheckedChange = { onConfigChange(config.copy(geolocationEnabled = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.geolocationAccuracyLabel,
                                options = listOf(
                                    com.webtoapp.data.model.GeolocationAccuracy.COARSE to Strings.geolocationAccuracyCoarse,
                                    com.webtoapp.data.model.GeolocationAccuracy.FINE to Strings.geolocationAccuracyFine
                                ),
                                selected = config.geolocationAccuracy,
                                onSelect = { onConfigChange(config.copy(geolocationAccuracy = it)) }
                            )
                            ChoiceChipRow(
                                label = Strings.geolocationPolicyLabel,
                                options = listOf(
                                    com.webtoapp.data.model.GeolocationPolicy.ALWAYS_ASK to Strings.geolocationPolicyAlwaysAsk,
                                    com.webtoapp.data.model.GeolocationPolicy.REMEMBER_PER_HOST to Strings.geolocationPolicyRemember,
                                    com.webtoapp.data.model.GeolocationPolicy.DENY_ALL to Strings.geolocationPolicyDeny
                                ),
                                selected = config.geolocationPolicy,
                                onSelect = { onConfigChange(config.copy(geolocationPolicy = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.blobDownloadTitle,
                            subtitle = Strings.blobDownloadDesc,
                            icon = Icons.Outlined.CloudDownload,
                            checked = config.enableBlobDownloadInterception,
                            onCheckedChange = { onConfigChange(config.copy(enableBlobDownloadInterception = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.blobInterceptScopeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.BlobInterceptScope.ALL to Strings.blobInterceptScopeAll,
                                    com.webtoapp.data.model.BlobInterceptScope.SIZE_OVER_THRESHOLD to Strings.blobInterceptScopeOver
                                ),
                                selected = config.blobInterceptScope,
                                onSelect = { onConfigChange(config.copy(blobInterceptScope = it)) }
                            )
                            if (config.blobInterceptScope == com.webtoapp.data.model.BlobInterceptScope.SIZE_OVER_THRESHOLD) {
                                IntegerField(
                                    label = Strings.blobInterceptThresholdLabel,
                                    value = config.blobInterceptThresholdMb,
                                    minValue = 1,
                                    maxValue = 1024,
                                    onValueChange = { onConfigChange(config.copy(blobInterceptThresholdMb = it)) }
                                )
                            }
                        }

                        SpecialAdvancedRow(
                            title = Strings.primeUserActivationTitle,
                            subtitle = Strings.primeUserActivationDesc,
                            icon = Icons.Outlined.TouchApp,
                            checked = config.primeUserActivation,
                            onCheckedChange = { onConfigChange(config.copy(primeUserActivation = it)) }
                        ) {
                            ChoiceChipRow(
                                label = Strings.primeActivationModeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.PrimeUserActivationMode.SYNTHETIC_TAP to Strings.primeActivationModeTap,
                                    com.webtoapp.data.model.PrimeUserActivationMode.DPAD_OK to Strings.primeActivationModeDpad,
                                    com.webtoapp.data.model.PrimeUserActivationMode.BOTH to Strings.primeActivationModeBoth
                                ),
                                selected = config.primeUserActivationMode,
                                onSelect = { onConfigChange(config.copy(primeUserActivationMode = it)) }
                            )
                            ChoiceChipRow(
                                label = Strings.primeActivationTimingLabel,
                                options = listOf(
                                    com.webtoapp.data.model.PrimeUserActivationTiming.ON_PAGE_FINISHED to Strings.primeActivationTimingFinished,
                                    com.webtoapp.data.model.PrimeUserActivationTiming.ON_FIRST_VISIBLE to Strings.primeActivationTimingVisible
                                ),
                                selected = config.primeUserActivationTiming,
                                onSelect = { onConfigChange(config.copy(primeUserActivationTiming = it)) }
                            )
                        }

                        SpecialAdvancedRow(
                            title = Strings.fullscreenVideoOrientationTitle,
                            subtitle = Strings.fullscreenVideoOrientationDesc,
                            icon = Icons.Outlined.ScreenRotation,
                            checked = config.fullscreenVideoOrientation != com.webtoapp.data.model.FullscreenVideoOrientation.KEEP_CURRENT,
                            onCheckedChange = { enabled ->
                                onConfigChange(
                                    config.copy(
                                        fullscreenVideoOrientation = if (enabled) {
                                            com.webtoapp.data.model.FullscreenVideoOrientation.AUTO_SENSOR_LANDSCAPE
                                        } else {
                                            com.webtoapp.data.model.FullscreenVideoOrientation.KEEP_CURRENT
                                        }
                                    )
                                )
                            }
                        ) {

                            ChoiceChipRow(
                                label = Strings.fullscreenVideoOrientationModeLabel,
                                options = listOf(
                                    com.webtoapp.data.model.FullscreenVideoOrientation.AUTO_SENSOR_LANDSCAPE
                                        to Strings.fullscreenVideoOrientationModeAuto,
                                    com.webtoapp.data.model.FullscreenVideoOrientation.FORCE_LANDSCAPE
                                        to Strings.fullscreenVideoOrientationModeForce
                                ),
                                selected = if (config.fullscreenVideoOrientation == com.webtoapp.data.model.FullscreenVideoOrientation.KEEP_CURRENT) {
                                    com.webtoapp.data.model.FullscreenVideoOrientation.AUTO_SENSOR_LANDSCAPE
                                } else config.fullscreenVideoOrientation,
                                onSelect = { onConfigChange(config.copy(fullscreenVideoOrientation = it)) }
                            )
                        }

                        FailoverAdvancedRow(
                            config = config,
                            onConfigChange = onConfigChange
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialAdvancedRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    WtaSettingCard {
        Column {
            WtaToggleRow(
                title = title,
                subtitle = subtitle,
                icon = icon,
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            AnimatedVisibility(
                visible = checked,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = WtaSpacing.RowHorizontal,
                        end = WtaSpacing.RowHorizontal,
                        top = 4.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChipRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (value, text) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(text) }
                )
            }
        }
    }
}

@Composable
private fun IntegerField(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
) {
    var raw by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = raw,
        onValueChange = { input ->
            raw = input.filter { it.isDigit() }.take(6)
            raw.toIntOrNull()?.coerceIn(minValue, maxValue)?.let { onValueChange(it) }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FailoverAdvancedRow(
    config: com.webtoapp.data.model.WebViewConfig,
    onConfigChange: (com.webtoapp.data.model.WebViewConfig) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    SpecialAdvancedRow(
        title = Strings.failoverTitle,
        subtitle = Strings.failoverDesc,
        icon = Icons.Outlined.SwapHoriz,
        checked = config.failoverEnabled,
        onCheckedChange = { onConfigChange(config.copy(failoverEnabled = it)) }
    ) {
        Text(
            text = Strings.failoverUrlsLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        com.webtoapp.ui.components.WtaReorderableUrlList(
            items = config.failoverUrls,
            onItemsChange = { onConfigChange(config.copy(failoverUrls = it)) },
            onAddRequest = { showAddDialog = true },
            emptyHint = Strings.failoverEmptyHint,
            addButtonText = Strings.failoverAddUrl,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = Strings.failoverTriggersLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val triggers = config.failoverTriggers
            FilterChip(
                selected = triggers.networkError,
                onClick = {
                    onConfigChange(config.copy(failoverTriggers = triggers.copy(networkError = !triggers.networkError)))
                },
                label = { Text(Strings.failoverTriggerNetworkError) }
            )
            FilterChip(
                selected = triggers.http5xx,
                onClick = {
                    onConfigChange(config.copy(failoverTriggers = triggers.copy(http5xx = !triggers.http5xx)))
                },
                label = { Text(Strings.failoverTriggerHttp5xx) }
            )
            FilterChip(
                selected = triggers.http4xx,
                onClick = {
                    onConfigChange(config.copy(failoverTriggers = triggers.copy(http4xx = !triggers.http4xx)))
                },
                label = { Text(Strings.failoverTriggerHttp4xx) }
            )
            FilterChip(
                selected = triggers.timeout,
                onClick = {
                    onConfigChange(config.copy(failoverTriggers = triggers.copy(timeout = !triggers.timeout)))
                },
                label = { Text(Strings.failoverTriggerTimeout) }
            )
        }

        if (config.failoverTriggers.timeout) {
            Spacer(Modifier.height(8.dp))
            IntegerField(
                label = Strings.failoverTimeoutSecondsLabel,
                value = config.failoverTimeoutSeconds,
                minValue = 5,
                maxValue = 60,
                onValueChange = { onConfigChange(config.copy(failoverTimeoutSeconds = it)) }
            )
        }
    }

    if (showAddDialog) {
        FailoverAddUrlDialog(
            existingUrls = config.failoverUrls,
            onDismiss = { showAddDialog = false },
            onConfirm = { url ->
                onConfigChange(config.copy(failoverUrls = config.failoverUrls + url))
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun FailoverAddUrlDialog(
    existingUrls: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    com.webtoapp.ui.design.WtaAlertDialog(
        onDismissRequest = onDismiss,
        title = Strings.failoverAddUrl,
        content = {
            com.webtoapp.ui.design.WtaTextField(
                value = input,
                onValueChange = {
                    input = it
                    errorText = null
                },
                placeholder = Strings.failoverUrlInputHint,
                isError = errorText != null,
                supportingText = errorText,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                    autoCorrect = false
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = input.trim()
                val lower = trimmed.lowercase()
                when {
                    !(lower.startsWith("http://") || lower.startsWith("https://")) -> {
                        errorText = Strings.failoverUrlInvalid
                    }
                    existingUrls.any { it.equals(trimmed, ignoreCase = true) } -> {
                        errorText = Strings.failoverUrlDuplicate
                    }
                    else -> onConfirm(trimmed)
                }
            }) { Text(Strings.confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}

@Composable
private fun CustomHtmlEditorRow(
    customHtml: String?,
    onCustomHtmlChange: (String?) -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }
    val hasContent = !customHtml.isNullOrBlank()
    val lineCount = customHtml?.count { it == '\n' }?.plus(1) ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasContent) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WtaRadius.Card))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(WtaRadius.Card)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(WtaSize.Icon)
                )
                Spacer(Modifier.width(WtaSpacing.Small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Strings.errorPageHtmlSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$lineCount ${if (lineCount == 1) "line" else "lines"}, ${customHtml?.length ?: 0} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onCustomHtmlChange(null) }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = Strings.errorPageClearMedia,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(WtaSize.IconSmall)
                    )
                }
            }
        } else {
            Text(
                text = Strings.errorPageHtmlEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        WtaCard(
            onClick = { showEditor = true },
            tone = WtaCardTone.Surface,
            contentPadding = PaddingValues(vertical = WtaSpacing.Medium, horizontal = WtaSpacing.Large),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(WtaSize.IconSmall)
                )
                Spacer(Modifier.width(WtaSpacing.Small))
                Text(
                    text = if (hasContent) Strings.errorPageEditCodeAgain else Strings.errorPageOpenCodeEditor,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showEditor) {
        WtaCodeEditorDialog(
            language = "HTML",
            initialContent = customHtml ?: "",
            placeholder = Strings.errorPageCustomHtmlHint,

            canSaveEmpty = true,
            onSave = { content ->
                onCustomHtmlChange(content.takeIf { it.isNotBlank() })
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }
}

@Composable
private fun CustomMediaPickerRow(
    customMediaPath: String?,
    onCustomMediaPathChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasContent = !customMediaPath.isNullOrBlank()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {

            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video/")
            val savedPath = MediaStorage.saveMedia(context, uri, isVideo = isVideo)
            if (savedPath != null) {

                if (hasContent) MediaStorage.deleteMedia(customMediaPath)
                onCustomMediaPathChange(savedPath)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasContent) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WtaRadius.Card))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(WtaRadius.Card)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WtaSpacing.Medium)
            ) {
                AsyncImage(
                    model = customMediaPath,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 80.dp, height = 56.dp)
                        .clip(RoundedCornerShape(WtaRadius.Chip)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Strings.errorPageMediaSelected,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = customMediaPath?.substringAfterLast('/') ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { mediaPickerLauncher.launch("*/*") }) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = Strings.errorPageReplaceMedia,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(WtaSize.IconSmall)
                    )
                }
                IconButton(onClick = {
                    MediaStorage.deleteMedia(customMediaPath)
                    onCustomMediaPathChange(null)
                }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = Strings.errorPageClearMedia,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(WtaSize.IconSmall)
                    )
                }
            }
        } else {
            Text(
                text = Strings.errorPageMediaEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            WtaCard(
                onClick = { mediaPickerLauncher.launch("*/*") },
                tone = WtaCardTone.Surface,
                contentPadding = PaddingValues(vertical = WtaSpacing.Medium, horizontal = WtaSpacing.Large),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(WtaSize.IconSmall)
                    )
                    Spacer(Modifier.width(WtaSpacing.Small))
                    Text(
                        text = Strings.errorPagePickMedia,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
