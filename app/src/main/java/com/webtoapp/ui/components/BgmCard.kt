package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.BgmPlayMode
import com.webtoapp.ui.design.*







@Composable
fun BgmCard(
    enabled: Boolean,
    config: BgmConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (BgmConfig) -> Unit
) {
    var showSelectorDialog by remember { mutableStateOf(false) }


    WtaSettingCard {
        Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)) {
            WtaToggleRow(
                title = Strings.bgmTitle,
                subtitle = null,
                icon = Icons.Outlined.MusicNote,
                checked = enabled,
                onCheckedChange = onEnabledChange
            )

            AnimatedVisibility(
                visible = enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column(
                  modifier = Modifier.padding(horizontal = WtaSpacing.RowHorizontal),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                if (config.playlist.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WtaRadius.Card),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {

                            config.playlist.take(3).forEachIndexed { index, bgm ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.15f - index * 0.04f
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        bgm.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (index < minOf(config.playlist.size - 1, 2)) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }


                            if (config.playlist.size > 3) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    Strings.andMoreTracks.format(config.playlist.size - 3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                StatusChip(
                                    icon = when (config.playMode) {
                                        BgmPlayMode.LOOP -> Icons.Outlined.Repeat
                                        BgmPlayMode.SEQUENTIAL -> Icons.AutoMirrored.Outlined.List
                                        BgmPlayMode.SHUFFLE -> Icons.Outlined.Shuffle
                                    },
                                    label = when (config.playMode) {
                                        BgmPlayMode.LOOP -> Strings.loopPlayback
                                        BgmPlayMode.SEQUENTIAL -> Strings.sequentialPlayback
                                        BgmPlayMode.SHUFFLE -> Strings.shufflePlayback
                                    }
                                )


                                StatusChip(
                                    icon = if (config.volume > 0.5f) Icons.AutoMirrored.Outlined.VolumeUp
                                           else Icons.AutoMirrored.Outlined.VolumeDown,
                                    label = "${(config.volume * 100).toInt()}%"
                                )


                                if (config.showLyrics) {
                                    StatusChip(
                                        icon = Icons.Outlined.Subtitles,
                                        label = Strings.showLyrics
                                    )
                                }
                            }
                        }
                    }
                }


                PremiumOutlinedButton(
                    onClick = { showSelectorDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (config.playlist.isEmpty()) Icons.Outlined.Add else Icons.Outlined.Edit,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (config.playlist.isEmpty()) Strings.selectMusic else Strings.modifyConfig)
                }
              }
            }
        }
    }


    if (showSelectorDialog) {
        BgmSelectorDialog(
            currentConfig = config,
            onDismiss = { showSelectorDialog = false },
            onConfirm = { newConfig ->
                onConfigChange(newConfig)
                showSelectorDialog = false
            }
        )
    }
}




@Composable
private fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(WtaRadius.Button),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
