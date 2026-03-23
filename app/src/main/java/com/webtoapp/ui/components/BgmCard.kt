package com.webtoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.BgmPlayMode

/**
 * 背景音乐设置卡片
 */
@Composable
fun BgmCard(
    enabled: Boolean,
    config: BgmConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (BgmConfig) -> Unit
) {
    var showSelectorDialog by remember { mutableStateOf(false) }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.MusicNote,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.bgmTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            if (enabled) {
                Text(
                    text = Strings.bgmDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 当前配置概览
                if (config.playlist.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    Strings.selectedMusicCount.format(config.playlist.size),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    when (config.playMode) {
                                        BgmPlayMode.LOOP -> Strings.loopPlayback
                                        BgmPlayMode.SEQUENTIAL -> Strings.sequentialPlayback
                                        BgmPlayMode.SHUFFLE -> Strings.shufflePlayback
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Show音乐名称
                            Text(
                                config.playlist.joinToString("、") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                Strings.volumePercent.format((config.volume * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Select/修改按钮
                OutlinedButton(
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
    
    // 音乐选择对话框
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
