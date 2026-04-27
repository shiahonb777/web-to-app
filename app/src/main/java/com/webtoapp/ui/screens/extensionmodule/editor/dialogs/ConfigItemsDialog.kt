package com.webtoapp.ui.screens.extensionmodule.editor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.extension.ConfigItemType
import com.webtoapp.core.extension.ModuleConfigItem
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumTextField

@Composable
fun ConfigItemsDialog(
    configItems: List<ModuleConfigItem>,
    onConfigItemsChange: (List<ModuleConfigItem>) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().userConfigItems) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (configItems.isEmpty()) {
                    Text(
                        AppStringsProvider.current().noConfigItemsHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    configItems.forEachIndexed { index, item ->
                        val itemColor = MaterialTheme.colorScheme.secondary
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    itemColor.copy(alpha = 0.12f),
                                                    itemColor.copy(alpha = 0.04f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = itemColor
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${item.type.name} · ${item.key}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = { onConfigItemsChange(configItems.filterIndexed { i, _ -> i != index }) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = AppStringsProvider.current().delete,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(AppStringsProvider.current().add)
                }
                TextButton(onClick = onDismiss) {
                    Text(AppStringsProvider.current().done)
                }
            }
        }
    )

    if (showAddDialog) {
        AddConfigItemDialog(
            onAdd = { item ->
                onConfigItemsChange(configItems + item)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConfigItemDialog(
    onAdd: (ModuleConfigItem) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ConfigItemType.TEXT) }
    var defaultValue by remember { mutableStateOf("") }
    var required by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().addConfigItem) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(AppStringsProvider.current().keyNameRequired) },
                    placeholder = { Text(AppStringsProvider.current().keyNamePlaceholder) },
                    singleLine = true
                )

                PremiumTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(AppStringsProvider.current().displayNameRequired) },
                    placeholder = { Text(AppStringsProvider.current().displayNamePlaceholder) },
                    singleLine = true
                )

                PremiumTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(AppStringsProvider.current().explanationLabel) },
                    placeholder = { Text(AppStringsProvider.current().configExplanationPlaceholder) },
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    PremiumTextField(
                        value = type.name,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        label = { Text(AppStringsProvider.current().typeLabel) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ConfigItemType.values().forEach { itemType ->
                            DropdownMenuItem(
                                text = { Text(itemType.name) },
                                onClick = {
                                    type = itemType
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                PremiumTextField(
                    value = defaultValue,
                    onValueChange = { defaultValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(AppStringsProvider.current().defaultValueLabel) },
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = required, onCheckedChange = { required = it })
                    Text(AppStringsProvider.current().requiredField)
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    onAdd(
                        ModuleConfigItem(
                            key = key,
                            name = name,
                            description = description,
                            type = type,
                            defaultValue = defaultValue,
                            required = required
                        )
                    )
                },
                enabled = key.isNotBlank() && name.isNotBlank()
            ) {
                Text(AppStringsProvider.current().add)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().cancel)
            }
        }
    )
}
