package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.UserAgentMode
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.PremiumTextField

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun UserAgentCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isEnabled = config.userAgentMode != UserAgentMode.DEFAULT

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
                        icon = Icons.Outlined.Language,
                        enabled = isEnabled
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.userAgentMode,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isEnabled) config.userAgentMode.displayName else Strings.userAgentDefault,
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
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.bypassWebViewDetection,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = Strings.mobileVersion,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            UserAgentMode.DEFAULT to Strings.userAgentDefault,
                            UserAgentMode.CHROME_MOBILE to "Chrome",
                            UserAgentMode.SAFARI_MOBILE to "Safari",
                            UserAgentMode.FIREFOX_MOBILE to "Firefox",
                            UserAgentMode.EDGE_MOBILE to "Edge"
                        ).forEach { (mode, name) ->
                            PremiumFilterChip(
                                selected = config.userAgentMode == mode,
                                onClick = { onConfigChange(config.copy(userAgentMode = mode)) },
                                label = { Text(name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = Strings.desktopVersion,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            UserAgentMode.CHROME_DESKTOP to "Chrome",
                            UserAgentMode.SAFARI_DESKTOP to "Safari",
                            UserAgentMode.FIREFOX_DESKTOP to "Firefox",
                            UserAgentMode.EDGE_DESKTOP to "Edge"
                        ).forEach { (mode, name) ->
                            PremiumFilterChip(
                                selected = config.userAgentMode == mode,
                                onClick = { onConfigChange(config.copy(userAgentMode = mode)) },
                                label = { Text(name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    PremiumFilterChip(
                        selected = config.userAgentMode == UserAgentMode.CUSTOM,
                        onClick = { onConfigChange(config.copy(userAgentMode = UserAgentMode.CUSTOM)) },
                        label = { Text(Strings.userAgentCustom) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Edit,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    if (config.userAgentMode == UserAgentMode.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PremiumTextField(
                            value = config.customUserAgent ?: "",
                            onValueChange = { onConfigChange(config.copy(customUserAgent = it.ifBlank { null })) },
                            label = { Text("User-Agent") },
                            placeholder = { Text(Strings.userAgentCustomHint) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4
                        )
                    }

                    if (config.userAgentMode != UserAgentMode.DEFAULT && config.userAgentMode != UserAgentMode.CUSTOM) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Strings.currentUserAgent,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = config.userAgentMode.userAgentString ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
