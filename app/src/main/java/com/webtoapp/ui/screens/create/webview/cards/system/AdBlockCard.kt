package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumSwitch
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.viewmodel.EditState

@Composable
fun AdBlockCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onRulesChange: (List<String>) -> Unit,
    onToggleEnabledChange: (Boolean) -> Unit = {}
) {
    var newRule by remember { mutableStateOf("") }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SystemCardFeatureIcon(
                        icon = Icons.Outlined.Shield,
                        enabled = editState.adBlockEnabled
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.adBlocking,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                PremiumSwitch(
                    checked = editState.adBlockEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            SystemCardExpandContent(visible = editState.adBlockEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = Strings.adBlockDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                text = Strings.adBlockToggleEnabled,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = Strings.adBlockToggleDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = editState.webViewConfig.adBlockToggleEnabled,
                            onCheckedChange = onToggleEnabledChange
                        )
                    }

                    Text(
                        text = Strings.customBlockRules,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PremiumTextField(
                            value = newRule,
                            onValueChange = { newRule = it },
                            placeholder = { Text(Strings.adBlockRuleHint) },
                            singleLine = true,
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (newRule.isNotBlank()) {
                                    onRulesChange(editState.adBlockRules + newRule)
                                    newRule = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, Strings.add)
                        }
                    }

                    editState.adBlockRules.forEachIndexed { index, rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rule,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(weight = 1f, fill = true)
                            )
                            IconButton(
                                onClick = {
                                    onRulesChange(editState.adBlockRules.filterIndexed { i, _ -> i != index })
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    Strings.delete,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
