package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.data.model.ApkEncryptionConfig

/**
 * 加密配置卡片
 * 用于在创建应用时配置 APK 加密选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptionConfigCard(
    config: ApkEncryptionConfig,
    onConfigChange: (ApkEncryptionConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.enabled) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (config.enabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (config.enabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = "资源加密",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (config.enabled) "已启用加密保护" else "未启用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { enabled ->
                        onConfigChange(config.copy(enabled = enabled))
                    }
                )
            }
            
            // 展开详细配置
            AnimatedVisibility(visible = config.enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 快捷预设
                    Text(
                        text = "加密级别",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = config == ApkEncryptionConfig.BASIC.copy(enabled = true),
                            onClick = {
                                onConfigChange(ApkEncryptionConfig.BASIC)
                            },
                            label = { Text("基础") },
                            leadingIcon = if (config == ApkEncryptionConfig.BASIC.copy(enabled = true)) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        FilterChip(
                            selected = config == ApkEncryptionConfig.FULL,
                            onClick = {
                                onConfigChange(ApkEncryptionConfig.FULL)
                            },
                            label = { Text("完全") },
                            leadingIcon = if (config == ApkEncryptionConfig.FULL) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        
                        FilterChip(
                            selected = expanded,
                            onClick = { expanded = !expanded },
                            label = { Text("自定义") },
                            leadingIcon = {
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null,
                                    Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                    
                    // 自定义配置
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            EncryptionOption(
                                title = "配置文件",
                                description = "加密 app_config.json",
                                checked = config.encryptConfig,
                                onCheckedChange = { onConfigChange(config.copy(encryptConfig = it)) }
                            )
                            
                            EncryptionOption(
                                title = "HTML/CSS/JS",
                                description = "加密网页代码文件",
                                checked = config.encryptHtml,
                                onCheckedChange = { onConfigChange(config.copy(encryptHtml = it)) }
                            )
                            
                            EncryptionOption(
                                title = "媒体文件",
                                description = "加密图片和视频",
                                checked = config.encryptMedia,
                                onCheckedChange = { onConfigChange(config.copy(encryptMedia = it)) }
                            )
                            
                            EncryptionOption(
                                title = "启动画面",
                                description = "加密启动画面资源",
                                checked = config.encryptSplash,
                                onCheckedChange = { onConfigChange(config.copy(encryptSplash = it)) }
                            )
                            
                            EncryptionOption(
                                title = "背景音乐",
                                description = "加密 BGM 文件",
                                checked = config.encryptBgm,
                                onCheckedChange = { onConfigChange(config.copy(encryptBgm = it)) }
                            )
                        }
                    }
                    
                    // 加密说明
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "加密后的资源无法被直接查看或提取，可有效保护您的代码和内容。加密基于 AES-256-GCM 算法，密钥与应用签名绑定。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncryptionOption(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
