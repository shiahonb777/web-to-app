package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.DnsConfig
import com.webtoapp.data.model.DnsProvider

/**
 * DNS 配置卡片
 * 支持系统默认 / DNS-over-HTTPS (DoH) 自定义提供商
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsConfigCard(
    dnsMode: String,
    dnsConfig: DnsConfig,
    onDnsModeChange: (String) -> Unit,
    onDnsConfigChange: (DnsConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = dnsMode != "SYSTEM"

    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Dns,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.dnsConfigTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!enabled) {
                            Text(
                                Strings.dnsModeSystemDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = { onDnsModeChange(if (it) "DOH" else "SYSTEM") }
                )
            }

            // 启用后展示配置
            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // DNS 提供商选择
                    Text(
                        Strings.dnsProviderLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 提供商网格
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DnsProvider.entries.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                row.forEach { provider ->
                                    FilterChip(
                                        selected = dnsConfig.provider == provider.key,
                                        onClick = { onDnsConfigChange(dnsConfig.copy(provider = provider.key)) },
                                        label = { Text(provider.displayName, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // 填充空位
                                repeat(3 - row.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // 自定义 DoH URL
                    if (dnsConfig.provider == "custom") {
                        PremiumTextField(
                            value = dnsConfig.customDohUrl,
                            onValueChange = { onDnsConfigChange(dnsConfig.copy(customDohUrl = it)) },
                            label = { Text(Strings.dnsCustomDohUrl) },
                            placeholder = { Text(Strings.dnsCustomDohUrlPlaceholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // DoH 模式选择
                    Text(
                        Strings.dohModeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DohModeChip(
                            selected = dnsConfig.dohMode == "automatic",
                            label = Strings.dohModeAutomatic,
                            onClick = { onDnsConfigChange(dnsConfig.copy(dohMode = "automatic")) }
                        )
                        DohModeChip(
                            selected = dnsConfig.dohMode == "strict",
                            label = Strings.dohModeStrict,
                            onClick = { onDnsConfigChange(dnsConfig.copy(dohMode = "strict")) }
                        )
                    }

                    // DoH 模式说明
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (dnsConfig.dohMode == "strict") Strings.dohModeStrictDesc else Strings.dohModeAutomaticDesc,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 绕过系统 DNS 开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.dnsBypassSystemDns,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                Strings.dnsBypassSystemDnsDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dnsConfig.bypassSystemDns,
                            onCheckedChange = { onDnsConfigChange(dnsConfig.copy(bypassSystemDns = it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DohModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Outlined.Dns,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}
