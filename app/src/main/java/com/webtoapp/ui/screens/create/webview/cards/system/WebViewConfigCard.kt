package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.ViewportMode
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.SettingsSwitch
import com.webtoapp.ui.screens.ApkExportSection
import com.webtoapp.ui.screens.UserScriptsSection

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
                    SystemCardFeatureIcon(
                        icon = Icons.Outlined.SettingsApplications,
                        enabled = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = AppStringsProvider.current().advancedSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = AppStringsProvider.current().webViewAdvancedConfig,
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

            SystemCardExpandContent(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AdvancedSettingsSection(
                        title = AppStringsProvider.current().sectionWebEngine,
                        icon = Icons.Outlined.Memory
                    ) {
                        SettingsSwitch(
                            title = "JavaScript",
                            subtitle = AppStringsProvider.current().enableJavaScript,
                            checked = config.javaScriptEnabled,
                            onCheckedChange = { onConfigChange(config.copy(javaScriptEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = AppStringsProvider.current().domStorageSetting,
                            subtitle = AppStringsProvider.current().domStorageSettingHint,
                            checked = config.domStorageEnabled,
                            onCheckedChange = { onConfigChange(config.copy(domStorageEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = AppStringsProvider.current().crossOriginIsolationSetting,
                            subtitle = AppStringsProvider.current().crossOriginIsolationSettingHint,
                            checked = config.enableCrossOriginIsolation,
                            onCheckedChange = { onConfigChange(config.copy(enableCrossOriginIsolation = it)) }
                        )
                    }

                    AdvancedSettingsSection(
                        title = AppStringsProvider.current().sectionContentDisplay,
                        icon = Icons.Outlined.Visibility
                    ) {
                        SettingsSwitch(
                            title = AppStringsProvider.current().zoomSetting,
                            subtitle = AppStringsProvider.current().zoomSettingHint,
                            checked = config.zoomEnabled,
                            onCheckedChange = { onConfigChange(config.copy(zoomEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = AppStringsProvider.current().fullscreenVideoSetting,
                            subtitle = AppStringsProvider.current().fullscreenVideoSettingHint,
                            checked = config.fullscreenEnabled,
                            onCheckedChange = { onConfigChange(config.copy(fullscreenEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = AppStringsProvider.current().hideBrowserToolbarLabel,
                            subtitle = AppStringsProvider.current().hideBrowserToolbarHint,
                            checked = config.hideBrowserToolbar,
                            onCheckedChange = { onConfigChange(config.copy(hideBrowserToolbar = it)) }
                        )
                        ViewportModeSelector(config = config, onConfigChange = onConfigChange)
                    }

                    AdvancedSettingsSection(
                        title = AppStringsProvider.current().sectionNavigation,
                        icon = Icons.Outlined.Navigation
                    ) {
                        SettingsSwitch(
                            title = AppStringsProvider.current().swipeRefreshSetting,
                            subtitle = AppStringsProvider.current().swipeRefreshSettingHint,
                            checked = config.swipeRefreshEnabled,
                            onCheckedChange = { onConfigChange(config.copy(swipeRefreshEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = AppStringsProvider.current().externalLinksSetting,
                            subtitle = AppStringsProvider.current().externalLinksSettingHint,
                            checked = config.openExternalLinks,
                            onCheckedChange = { onConfigChange(config.copy(openExternalLinks = it)) }
                        )
                        SettingsSwitch(
                            title = AppStringsProvider.current().deepLinkSetting,
                            subtitle = AppStringsProvider.current().deepLinkSettingHint,
                            checked = apkExportConfig.deepLinkEnabled,
                            onCheckedChange = { onApkExportConfigChange(apkExportConfig.copy(deepLinkEnabled = it)) }
                        )
                        SystemCardExpandContent(visible = apkExportConfig.deepLinkEnabled) {
                            var customHostsText by remember(apkExportConfig.customDeepLinkHosts) {
                                mutableStateOf(apkExportConfig.customDeepLinkHosts.joinToString("\n"))
                            }
                            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                                Text(
                                    text = AppStringsProvider.current().deepLinkCustomHostsLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = AppStringsProvider.current().deepLinkCustomHostsHint,
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
                            title = AppStringsProvider.current().showFloatingBackButtonLabel,
                            subtitle = AppStringsProvider.current().showFloatingBackButtonHint,
                            checked = config.showFloatingBackButton,
                            onCheckedChange = { onConfigChange(config.copy(showFloatingBackButton = it)) }
                        )
                        SettingsSwitch(
                            title = AppStringsProvider.current().blockSystemNavigationGestureLabel,
                            subtitle = AppStringsProvider.current().blockSystemNavigationGestureHint,
                            checked = config.blockSystemNavigationGesture,
                            onCheckedChange = { onConfigChange(config.copy(blockSystemNavigationGesture = it)) }
                        )
                    }

                    AdvancedSettingsSection(
                        title = AppStringsProvider.current().sectionOfflinePerformance,
                        icon = Icons.Outlined.CloudOff
                    ) {
                        SettingsSwitch(
                            title = AppStringsProvider.current().pwaOfflineTitle,
                            subtitle = AppStringsProvider.current().pwaOfflineSubtitle,
                            checked = config.pwaOfflineEnabled,
                            onCheckedChange = { onConfigChange(config.copy(pwaOfflineEnabled = it)) }
                        )
                        SystemCardExpandContent(visible = config.pwaOfflineEnabled) {
                            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                                Text(
                                    text = AppStringsProvider.current().pwaOfflineStrategyLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                listOf(
                                    "NETWORK_FIRST" to AppStringsProvider.current().pwaStrategyNetworkFirst,
                                    "CACHE_FIRST" to AppStringsProvider.current().pwaStrategyCacheFirst,
                                    "STALE_WHILE_REVALIDATE" to AppStringsProvider.current().pwaStrategyStaleWhileRevalidate
                                ).forEach { (value, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onConfigChange(config.copy(pwaOfflineStrategy = value)) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = config.pwaOfflineStrategy == value,
                                            onClick = { onConfigChange(config.copy(pwaOfflineStrategy = value)) }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(text = label, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        ErrorPageConfigCard(
                            config = config.errorPageConfig,
                            onConfigChange = { onConfigChange(config.copy(errorPageConfig = it)) }
                        )
                    }

                    AdvancedSettingsSection(
                        title = AppStringsProvider.current().sectionDeveloper,
                        icon = Icons.Outlined.Code
                    ) {
                        KeyboardAdjustModeCard(
                            mode = config.keyboardAdjustMode,
                            onModeChange = { onConfigChange(config.copy(keyboardAdjustMode = it)) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        UserScriptsSection(
                            scripts = config.injectScripts,
                            onScriptsChange = { onConfigChange(config.copy(injectScripts = it)) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun AdvancedSettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = com.webtoapp.ui.theme.LocalIsDarkTheme.current
    val sectionBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    Column {
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ViewportModeSelector(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    var viewportExpanded by remember { mutableStateOf(false) }
    val currentModeLabel = when (config.viewportMode) {
        ViewportMode.DEFAULT -> AppStringsProvider.current().viewportModeDefault
        ViewportMode.FIT_SCREEN -> AppStringsProvider.current().viewportModeFitScreen
        ViewportMode.DESKTOP -> AppStringsProvider.current().viewportModeDesktop
        ViewportMode.CUSTOM -> if (config.customViewportWidth in 320..3840) {
            "${AppStringsProvider.current().viewportCustomWidth}: ${config.customViewportWidth}px"
        } else {
            AppStringsProvider.current().viewportModeCustom
        }
    }

    SettingsSwitch(
        title = AppStringsProvider.current().viewportModeTitle,
        subtitle = currentModeLabel,
        checked = viewportExpanded,
        onCheckedChange = { viewportExpanded = it }
    )

    SystemCardExpandContent(visible = viewportExpanded) {
        Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            Text(
                text = AppStringsProvider.current().viewportModeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val viewportOptions = listOf(
                ViewportMode.DEFAULT to Pair(AppStringsProvider.current().viewportModeDefault, Icons.Outlined.Web),
                ViewportMode.FIT_SCREEN to Pair(AppStringsProvider.current().viewportModeFitScreen, Icons.Outlined.Fullscreen),
                ViewportMode.DESKTOP to Pair(AppStringsProvider.current().viewportModeDesktop, Icons.Outlined.DesktopWindows),
                ViewportMode.CUSTOM to Pair(AppStringsProvider.current().viewportModeCustom, Icons.Outlined.Tune)
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
                    text = AppStringsProvider.current().viewportCustomWidthPresets,
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
                            onClick = { onConfigChange(config.copy(customViewportWidth = px)) },
                            label = { Text(text = "$label ($px)", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
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
                            onConfigChange(config.copy(customViewportWidth = width.coerceIn(0, 3840)))
                        },
                        label = { Text(AppStringsProvider.current().viewportCustomWidth) },
                        placeholder = { Text("320-3840") },
                        suffix = { Text("px") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        supportingText = {
                            Text(
                                if (displayWidth in 1..3840) "✓ ${AppStringsProvider.current().viewportCustomWidth}: ${displayWidth}px"
                                else AppStringsProvider.current().viewportCustomWidthHint
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
