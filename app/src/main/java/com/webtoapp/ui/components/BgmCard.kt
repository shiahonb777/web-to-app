package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.BgmPlayMode

/**
 * settingscard( )
 * listpreview
 * mode/ /
 * animation
 */
@Composable
fun BgmCard(
    enabled: Boolean,
    config: BgmConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (BgmConfig) -> Unit
) {
    var showSelectorDialog by remember { mutableStateOf(false) }

    // icon animation
    val iconScale by animateFloatAsState(
        targetValue = if (enabled) 1.1f else 1f,
        animationSpec = tween(300),
        label = "bgmIconScale"
    )

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(iconScale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote,
                            null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = AppStringsProvider.current().bgmTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (enabled && config.playlist.isNotEmpty()) {
                            Text(
                                text = "${config.playlist.size} ${AppStringsProvider.current().selectedMusic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            AnimatedVisibility(
                visible = enabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = AppStringsProvider.current().bgmDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // currentconfig( enhanced)
                if (config.playlist.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // listpreview( display 3)
                            config.playlist.take(3).forEachIndexed { index, bgm ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Note
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
                                            fontWeight = FontWeight.Bold,
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
                            
                            // " N. . . " hint
                            if (config.playlist.size > 3) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    AppStringsProvider.current().andMoreTracks.format(config.playlist.size - 3),
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
                            
                            // statelabel
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // mode
                                StatusChip(
                                    icon = when (config.playMode) {
                                        BgmPlayMode.LOOP -> Icons.Outlined.Repeat
                                        BgmPlayMode.SEQUENTIAL -> Icons.AutoMirrored.Outlined.List
                                        BgmPlayMode.SHUFFLE -> Icons.Outlined.Shuffle
                                    },
                                    label = when (config.playMode) {
                                        BgmPlayMode.LOOP -> AppStringsProvider.current().loopPlayback
                                        BgmPlayMode.SEQUENTIAL -> AppStringsProvider.current().sequentialPlayback
                                        BgmPlayMode.SHUFFLE -> AppStringsProvider.current().shufflePlayback
                                    }
                                )
                                
                                // Note
                                StatusChip(
                                    icon = if (config.volume > 0.5f) Icons.AutoMirrored.Outlined.VolumeUp
                                           else Icons.AutoMirrored.Outlined.VolumeDown,
                                    label = "${(config.volume * 100).toInt()}%"
                                )
                                
                                // Note
                                if (config.showLyrics) {
                                    StatusChip(
                                        icon = Icons.Outlined.Subtitles,
                                        label = AppStringsProvider.current().showLyrics
                                    )
                                }
                            }
                        }
                    }
                }
                
                // select/ button
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
                    Text(if (config.playlist.isEmpty()) AppStringsProvider.current().selectMusic else AppStringsProvider.current().modifyConfig)
                }
              }
            }
        }
    }
    
    // selectdialog
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

/**
 * statelabel( mode/ /)
 */
@Composable
private fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
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
