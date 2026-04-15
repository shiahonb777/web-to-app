package com.webtoapp.ui.screens.extensionmodule.editor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.webtoapp.core.extension.ModuleUiConfig
import com.webtoapp.core.extension.ModuleUiType
import com.webtoapp.core.extension.SidebarPosition
import com.webtoapp.core.extension.ToolbarOrientation
import com.webtoapp.core.extension.UiPosition
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.PremiumSwitch
import com.webtoapp.util.SvgIconMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiTypeSelectionDialog(
    currentUiConfig: ModuleUiConfig,
    onUiConfigChange: (ModuleUiConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentUiConfig.type) }
    var position by remember { mutableStateOf(currentUiConfig.position) }
    var draggable by remember { mutableStateOf(currentUiConfig.draggable) }
    var toolbarOrientation by remember { mutableStateOf(currentUiConfig.toolbarOrientation) }
    var sidebarPosition by remember { mutableStateOf(currentUiConfig.sidebarPosition) }
    var showPositionSelector by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().uiTypeConfig) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    AppStringsProvider.current().selectUiType,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                ModuleUiType.values().forEach { uiType ->
                    val isSelected = selectedType == uiType
                    val tintColor = MaterialTheme.colorScheme.primary
                    Surface(
                        onClick = { selectedType = uiType },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            tintColor.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                tintColor.copy(alpha = if (isSelected) 0.15f else 0.10f),
                                                tintColor.copy(alpha = if (isSelected) 0.06f else 0.03f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    SvgIconMapper.getIcon(uiType.getIcon()),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) {
                                        tintColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiType.getDisplayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                                Text(
                                    uiType.getDescription(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = tintColor
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )

                Text(
                    AppStringsProvider.current().commonConfig,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                if (selectedType != ModuleUiType.SIDEBAR && selectedType != ModuleUiType.BOTTOM_BAR) {
                    Surface(
                        onClick = { showPositionSelector = !showPositionSelector },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    AppStringsProvider.current().uiPosition,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    position.getDisplayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                if (showPositionSelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (showPositionSelector) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                UiPosition.values().forEach { pos ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                position = pos
                                                showPositionSelector = false
                                            }
                                            .then(
                                                if (position == pos) {
                                                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = position == pos,
                                            onClick = {
                                                position = pos
                                                showPositionSelector = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            pos.getDisplayName(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (position == pos) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStringsProvider.current().draggableSwitch)
                    PremiumSwitch(checked = draggable, onCheckedChange = { draggable = it })
                }

                if (selectedType == ModuleUiType.FLOATING_TOOLBAR) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                    Text(
                        AppStringsProvider.current().toolbarConfig,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFilterChip(
                            selected = toolbarOrientation == ToolbarOrientation.HORIZONTAL,
                            onClick = { toolbarOrientation = ToolbarOrientation.HORIZONTAL },
                            label = { Text(ToolbarOrientation.HORIZONTAL.getDisplayName()) }
                        )
                        PremiumFilterChip(
                            selected = toolbarOrientation == ToolbarOrientation.VERTICAL,
                            onClick = { toolbarOrientation = ToolbarOrientation.VERTICAL },
                            label = { Text(ToolbarOrientation.VERTICAL.getDisplayName()) }
                        )
                    }
                }

                if (selectedType == ModuleUiType.SIDEBAR) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                    Text(
                        AppStringsProvider.current().sidebarConfig,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFilterChip(
                            selected = sidebarPosition == SidebarPosition.LEFT,
                            onClick = { sidebarPosition = SidebarPosition.LEFT },
                            label = { Text(SidebarPosition.LEFT.getDisplayName()) }
                        )
                        PremiumFilterChip(
                            selected = sidebarPosition == SidebarPosition.RIGHT,
                            onClick = { sidebarPosition = SidebarPosition.RIGHT },
                            label = { Text(SidebarPosition.RIGHT.getDisplayName()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    onUiConfigChange(
                        currentUiConfig.copy(
                            type = selectedType,
                            position = position,
                            draggable = draggable,
                            toolbarOrientation = toolbarOrientation,
                            sidebarPosition = sidebarPosition
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(AppStringsProvider.current().confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().cancel)
            }
        }
    )
}
