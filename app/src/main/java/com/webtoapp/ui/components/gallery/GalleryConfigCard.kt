package com.webtoapp.ui.components.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.data.model.GalleryConfig
import com.webtoapp.data.model.GalleryTransition

/**
 * 画廊配置卡片 - 设置幻灯片播放选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryConfigCard(
    config: GalleryConfig,
    onConfigChange: (GalleryConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Slideshow,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "幻灯片设置",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Divider()
            
            // 显示标题
            SettingsSwitchRow(
                title = "显示标题栏",
                subtitle = "在顶部显示当前项目的名称",
                checked = config.showTitle,
                onCheckedChange = { onConfigChange(config.copy(showTitle = it)) }
            )
            
            // 显示指示器
            SettingsSwitchRow(
                title = "显示页面指示器",
                subtitle = "在底部显示当前页码和总页数",
                checked = config.showIndicator,
                onCheckedChange = { onConfigChange(config.copy(showIndicator = it)) }
            )
            
            // 允许滑动
            SettingsSwitchRow(
                title = "允许滑动切换",
                subtitle = "左右滑动切换到上一个/下一个",
                checked = config.enableSwipe,
                onCheckedChange = { onConfigChange(config.copy(enableSwipe = it)) }
            )
            
            // 循环播放
            SettingsSwitchRow(
                title = "循环播放",
                subtitle = "到达最后一项后回到第一项",
                checked = config.loop,
                onCheckedChange = { onConfigChange(config.copy(loop = it)) }
            )
            
            Divider()
            
            // 自动播放
            SettingsSwitchRow(
                title = "自动播放",
                subtitle = "自动切换到下一个项目",
                checked = config.autoPlay,
                onCheckedChange = { onConfigChange(config.copy(autoPlay = it)) }
            )
            
            // 自动播放间隔
            if (config.autoPlay) {
                Column {
                    Text(
                        text = "切换间隔: ${config.autoPlayInterval} 秒",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = config.autoPlayInterval.toFloat(),
                        onValueChange = { 
                            onConfigChange(config.copy(autoPlayInterval = it.toInt()))
                        },
                        valueRange = 2f..30f,
                        steps = 27,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Divider()
            
            // 切换动画
            Text(
                text = "切换动画",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GalleryTransition.entries.take(3).forEach { transition ->
                    FilterChip(
                        selected = config.transitionType == transition,
                        onClick = { onConfigChange(config.copy(transitionType = transition)) },
                        label = { Text(transition.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GalleryTransition.entries.drop(3).forEach { transition ->
                    FilterChip(
                        selected = config.transitionType == transition,
                        onClick = { onConfigChange(config.copy(transitionType = transition)) },
                        label = { Text(transition.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 设置开关行
 */
@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
