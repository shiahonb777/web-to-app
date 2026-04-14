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
import com.webtoapp.core.i18n.Strings
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
            title = Strings.errorPageTitle,
            subtitle = Strings.errorPageSubtitle,
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
                    listOf(
                        ErrorPageMode.BUILTIN_STYLE to Strings.errorPageModeBuiltIn,
                        ErrorPageMode.CUSTOM_HTML to Strings.errorPageModeCustomHtml,
                        ErrorPageMode.CUSTOM_MEDIA to Strings.errorPageModeCustomMedia
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
                            listOf(
                                ErrorPageStyle.MATERIAL to Strings.errorPageStyleMaterial,
                                ErrorPageStyle.SATELLITE to Strings.errorPageStyleSatellite,
                                ErrorPageStyle.OCEAN to Strings.errorPageStyleOcean,
                                ErrorPageStyle.FOREST to Strings.errorPageStyleForest,
                                ErrorPageStyle.MINIMAL to Strings.errorPageStyleMinimal,
                                ErrorPageStyle.NEON to Strings.errorPageStyleNeon
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
                            title = Strings.errorPageMiniGameLabel,
                            subtitle = Strings.errorPageMiniGameDesc,
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
                                        MiniGameType.RANDOM to Strings.errorPageGameRandom,
                                        MiniGameType.BREAKOUT to Strings.errorPageGameBreakout,
                                        MiniGameType.MAZE to Strings.errorPageGameMaze,
                                        MiniGameType.STAR_CATCH to Strings.errorPageGameStarCatch,
                                        MiniGameType.INK_ZEN to Strings.errorPageGameInkZen
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
                            label = { Text(Strings.errorPageModeCustomHtml) },
                            placeholder = { Text(Strings.errorPageCustomHtmlHint) },
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
                            label = { Text(Strings.errorPageModeCustomMedia) },
                            placeholder = { Text(Strings.errorPageCustomMediaHint) },
                            leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Image, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SettingsSwitch(
                    title = Strings.errorPageAutoRetryLabel,
                    subtitle = if (config.autoRetrySeconds > 0) {
                        Strings.errorPageAutoRetryDesc.replace("%d", config.autoRetrySeconds.toString())
                    } else {
                        Strings.errorPageAutoRetryOff
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
                            text = "${config.autoRetrySeconds}${Strings.seconds}",
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
