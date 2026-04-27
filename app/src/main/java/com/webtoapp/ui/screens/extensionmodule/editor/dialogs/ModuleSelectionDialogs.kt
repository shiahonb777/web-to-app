package com.webtoapp.ui.screens.extensionmodule.editor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.extension.ModulePermission
import com.webtoapp.core.extension.ModuleRunMode
import com.webtoapp.core.extension.ModuleRunTime
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.util.SvgIconMapper

@Composable
fun ModuleCategoryDialog(
    selectedCategory: ModuleCategory,
    onCategorySelected: (ModuleCategory) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().selectCategory) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ModuleCategory.values().toList()) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = { onCategorySelected(cat) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
                                                MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.15f else 0.10f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.06f else 0.03f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    SvgIconMapper.getIcon(cat.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cat.getDisplayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                                Text(
                                    cat.getDescription(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ModuleRunAtDialog(
    selectedRunAt: ModuleRunTime,
    onRunAtSelected: (ModuleRunTime) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().runTime) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModuleRunTime.values().forEach { time ->
                    val isSelected = selectedRunAt == time
                    val timeIcon = when (time) {
                        ModuleRunTime.DOCUMENT_START -> "play_arrow"
                        ModuleRunTime.DOCUMENT_END -> "description"
                        ModuleRunTime.DOCUMENT_IDLE -> "hourglass_top"
                        ModuleRunTime.CONTEXT_MENU -> "menu"
                        ModuleRunTime.BEFORE_UNLOAD -> "exit_to_app"
                    }
                    Surface(
                        onClick = { onRunAtSelected(time) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
                                                MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.15f else 0.10f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.06f else 0.03f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    SvgIconMapper.getIcon(timeIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    time.getDisplayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                                Text(
                                    time.getDescription(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ModuleRunModeDialog(
    selectedRunMode: ModuleRunMode,
    onRunModeSelected: (ModuleRunMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().runModeLabel) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModuleRunMode.values().forEach { mode ->
                    val isSelected = selectedRunMode == mode
                    Surface(
                        onClick = { onRunModeSelected(mode) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
                                                MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.15f else 0.10f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.06f else 0.03f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    SvgIconMapper.getIcon(mode.getIcon()),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    mode.getDisplayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                                Text(
                                    mode.getDescription(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ModulePermissionsDialog(
    permissions: Set<ModulePermission>,
    onPermissionsChange: (Set<ModulePermission>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().requiredPermissions) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ModulePermission.values().toList()) { perm ->
                    val isChecked = perm in permissions
                    val permIcon = when (perm) {
                        ModulePermission.DOM_ACCESS, ModulePermission.DOM_OBSERVE -> "code"
                        ModulePermission.CSS_INJECT -> "palette"
                        ModulePermission.STORAGE, ModulePermission.CACHE -> "save"
                        ModulePermission.COOKIE, ModulePermission.INDEXED_DB -> "cookie"
                        ModulePermission.NETWORK, ModulePermission.WEBSOCKET, ModulePermission.FETCH_INTERCEPT -> "cloud"
                        ModulePermission.CLIPBOARD -> "content_paste"
                        ModulePermission.NOTIFICATION, ModulePermission.ALERT -> "notifications"
                        ModulePermission.KEYBOARD, ModulePermission.MOUSE, ModulePermission.TOUCH -> "mouse"
                        ModulePermission.LOCATION -> "location_on"
                        ModulePermission.CAMERA -> "camera"
                        ModulePermission.MICROPHONE -> "mic"
                        ModulePermission.DEVICE_INFO -> "phone_android"
                        ModulePermission.MEDIA, ModulePermission.FULLSCREEN, ModulePermission.PICTURE_IN_PICTURE -> "movie"
                        ModulePermission.SCREEN_CAPTURE -> "screenshot"
                        ModulePermission.DOWNLOAD, ModulePermission.FILE_ACCESS -> "folder"
                        ModulePermission.EVAL -> "terminal"
                        ModulePermission.IFRAME, ModulePermission.WINDOW_OPEN -> "open_in_new"
                        ModulePermission.HISTORY, ModulePermission.NAVIGATION -> "explore"
                    }
                    val tintColor = when {
                        perm.dangerous -> MaterialTheme.colorScheme.error
                        isChecked -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                    Surface(
                        onClick = {
                            onPermissionsChange(if (isChecked) permissions - perm else permissions + perm)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isChecked) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                tintColor.copy(alpha = if (isChecked) 0.15f else 0.10f),
                                                tintColor.copy(alpha = if (isChecked) 0.06f else 0.03f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    SvgIconMapper.getIcon(permIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = tintColor
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        perm.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                    if (perm.dangerous) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                AppStringsProvider.current().sensitive,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                Text(
                                    perm.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 2
                                )
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    onPermissionsChange(if (it) permissions + perm else permissions - perm)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().confirm)
            }
        }
    )
}
