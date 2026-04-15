package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.core.errorpage.ErrorPageConfig
import com.webtoapp.core.errorpage.ErrorPageMode
import com.webtoapp.core.errorpage.ErrorPageStyle
import com.webtoapp.core.errorpage.MiniGameType
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.SettingsSwitch

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ErrorPageConfigCard(
    config: ErrorPageConfig,
    onConfigChange: (ErrorPageConfig) -> Unit
) {
    val isCustomized = config.mode != ErrorPageMode.DEFAULT

    Column {
        SettingsSwitch(
            title = AppStringsProvider.current().errorPageTitle,
            subtitle = AppStringsProvider.current().errorPageSubtitle,
            checked = isCustomized,
            onCheckedChange = { checked ->
                onConfigChange(
                    config.copy(
                        mode = if (checked) ErrorPageMode.BUILTIN_STYLE else ErrorPageMode.DEFAULT
                    )
                )
            }
        )

        SystemCardExpandContent(visible = isCustomized) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
                Text(
                    text = AppStringsProvider.current().errorPageSubtitle,
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
                    listOf(
                        ErrorPageMode.BUILTIN_STYLE to AppStringsProvider.current().errorPageModeBuiltIn,
                        ErrorPageMode.CUSTOM_HTML to AppStringsProvider.current().errorPageModeCustomHtml,
                        ErrorPageMode.CUSTOM_MEDIA to AppStringsProvider.current().errorPageModeCustomMedia
                    ).forEach { (mode, label) ->
                        PremiumFilterChip(
                            selected = config.mode == mode,
                            onClick = { onConfigChange(config.copy(mode = mode)) },
                            label = { Text(label) }
                        )
                    }
                }

                SystemCardExpandContent(visible = config.mode == ErrorPageMode.BUILTIN_STYLE) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = AppStringsProvider.current().errorPageStyleLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ErrorPageStyle.MATERIAL to AppStringsProvider.current().errorPageStyleMaterial,
                                ErrorPageStyle.SATELLITE to AppStringsProvider.current().errorPageStyleSatellite,
                                ErrorPageStyle.OCEAN to AppStringsProvider.current().errorPageStyleOcean,
                                ErrorPageStyle.FOREST to AppStringsProvider.current().errorPageStyleForest,
                                ErrorPageStyle.MINIMAL to AppStringsProvider.current().errorPageStyleMinimal,
                                ErrorPageStyle.NEON to AppStringsProvider.current().errorPageStyleNeon
                            ).forEach { (style, label) ->
                                PremiumFilterChip(
                                    selected = config.builtInStyle == style,
                                    onClick = { onConfigChange(config.copy(builtInStyle = style)) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsSwitch(
                            title = AppStringsProvider.current().errorPageMiniGameLabel,
                            subtitle = AppStringsProvider.current().errorPageMiniGameDesc,
                            checked = config.showMiniGame,
                            onCheckedChange = { onConfigChange(config.copy(showMiniGame = it)) }
                        )

                        SystemCardExpandContent(visible = config.showMiniGame) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        MiniGameType.RANDOM to AppStringsProvider.current().errorPageGameRandom,
                                        MiniGameType.BREAKOUT to AppStringsProvider.current().errorPageGameBreakout,
                                        MiniGameType.MAZE to AppStringsProvider.current().errorPageGameMaze,
                                        MiniGameType.STAR_CATCH to AppStringsProvider.current().errorPageGameStarCatch,
                                        MiniGameType.INK_ZEN to AppStringsProvider.current().errorPageGameInkZen
                                    ).forEach { (type, label) ->
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

                SystemCardExpandContent(visible = config.mode == ErrorPageMode.CUSTOM_HTML) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        PremiumTextField(
                            value = config.customHtml ?: "",
                            onValueChange = { onConfigChange(config.copy(customHtml = it)) },
                            label = { Text(AppStringsProvider.current().errorPageModeCustomHtml) },
                            placeholder = { Text(AppStringsProvider.current().errorPageCustomHtmlHint) },
                            leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Code, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            singleLine = false
                        )
                    }
                }

                SystemCardExpandContent(visible = config.mode == ErrorPageMode.CUSTOM_MEDIA) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        PremiumTextField(
                            value = config.customMediaPath ?: "",
                            onValueChange = { onConfigChange(config.copy(customMediaPath = it)) },
                            label = { Text(AppStringsProvider.current().errorPageModeCustomMedia) },
                            placeholder = { Text(AppStringsProvider.current().errorPageCustomMediaHint) },
                            leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Image, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SettingsSwitch(
                    title = AppStringsProvider.current().errorPageAutoRetryLabel,
                    subtitle = if (config.autoRetrySeconds > 0) {
                        AppStringsProvider.current().errorPageAutoRetryDesc.replace("%d", config.autoRetrySeconds.toString())
                    } else {
                        AppStringsProvider.current().errorPageAutoRetryOff
                    },
                    checked = config.autoRetrySeconds > 0,
                    onCheckedChange = { checked ->
                        onConfigChange(config.copy(autoRetrySeconds = if (checked) 15 else 0))
                    }
                )

                SystemCardExpandContent(visible = config.autoRetrySeconds > 0) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${config.autoRetrySeconds}${AppStringsProvider.current().seconds}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = config.autoRetrySeconds.toFloat(),
                            onValueChange = { onConfigChange(config.copy(autoRetrySeconds = it.toInt())) },
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
