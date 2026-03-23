package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.webtoapp.core.disguise.DisguiseConfig
import com.webtoapp.core.i18n.Strings

/**
 * 应用伪装功能配置卡片
 * 
 * 核心功能：多桌面图标
 * 通过 AndroidManifest 的 activity-alias 实现，安装后自动显示多个图标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisguiseConfigCard(
    config: DisguiseConfig?,
    onConfigChange: (DisguiseConfig?) -> Unit
) {
    var expanded by remember { mutableStateOf(config?.enabled == true) }
    var enabled by remember(config) { mutableStateOf(config?.enabled ?: false) }
    
    // 多图标配置
    var multiLauncherIcons by remember(config) { mutableStateOf(config?.multiLauncherIcons ?: 1) }
    
    fun updateConfig() {
        if (!enabled) {
            onConfigChange(null)
        } else {
            onConfigChange(DisguiseConfig(
                enabled = enabled,
                multiLauncherIcons = if (enabled) multiLauncherIcons else 1
            ))
        }
    }
    
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                Strings.disguiseMultiIconTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (enabled && multiLauncherIcons > 1) 
                                    String.format(Strings.disguiseIconCountFormat, multiLauncherIcons)
                                else 
                                    Strings.disguiseNotEnabled,
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
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Enable开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.disguiseEnableMultiIcon,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Strings.disguiseEnableMultiIconDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                if (it && multiLauncherIcons < 2) {
                                    multiLauncherIcons = 2
                                }
                                updateConfig()
                            }
                        )
                    }
                    
                    AnimatedVisibility(visible = enabled) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Icon数量
                            Text(
                                Strings.disguiseIconCountTitle,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                Strings.disguiseIconCountDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Icon数量输入
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    Strings.disguiseCountLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(60.dp)
                                )
                                OutlinedTextField(
                                    value = multiLauncherIcons.toString(),
                                    onValueChange = { value ->
                                        val num = value.filter { it.isDigit() }.toIntOrNull() ?: 2
                                        multiLauncherIcons = if (num < 2) 2 else if (num > 10) 10 else num
                                        updateConfig()
                                    },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    supportingText = { Text(Strings.disguiseCountHint) }
                                )
                            }
                            
                            // 说明提示
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Outlined.Lightbulb,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Strings.disguiseMultiIconTip,
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
