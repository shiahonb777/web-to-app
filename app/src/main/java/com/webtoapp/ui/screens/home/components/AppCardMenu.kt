package com.webtoapp.ui.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AppType

@Composable
fun AppCardMenu(
    expanded: Boolean,
    appType: AppType,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onEditCore: () -> Unit,
    onCreateShortcut: () -> Unit,
    onBuildApk: () -> Unit,
    onShareApk: () -> Unit,
    onExport: () -> Unit,
    onMoveToCategory: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (appType == AppType.WEB) {
            DropdownMenuItem(
                text = { Text(Strings.btnEdit) },
                onClick = {
                    onDismiss()
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) }
            )
        } else {
            DropdownMenuItem(
                text = { Text(Strings.editCoreConfig) },
                onClick = {
                    onDismiss()
                    onEditCore()
                },
                leadingIcon = { Icon(Icons.Outlined.Tune, null) }
            )
            DropdownMenuItem(
                text = { Text(Strings.editCommonConfig) },
                onClick = {
                    onDismiss()
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Outlined.Settings, null) }
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(Strings.btnShortcut) },
            onClick = {
                onDismiss()
                onCreateShortcut()
            },
            leadingIcon = { Icon(Icons.Outlined.AppShortcut, null) }
        )
        DropdownMenuItem(
            text = { Text(Strings.buildDialogTitle) },
            onClick = {
                onDismiss()
                onBuildApk()
            },
            leadingIcon = { Icon(Icons.Outlined.InstallMobile, null) }
        )
        DropdownMenuItem(
            text = { Text(Strings.shareApk) },
            onClick = {
                onDismiss()
                onShareApk()
            },
            leadingIcon = { Icon(Icons.Outlined.Share, null) }
        )
        DropdownMenuItem(
            text = { Text(Strings.btnExport) },
            onClick = {
                onDismiss()
                onExport()
            },
            leadingIcon = { Icon(Icons.Outlined.FileDownload, null) }
        )
        DropdownMenuItem(
            text = { Text(Strings.moveToCategory) },
            onClick = {
                onDismiss()
                onMoveToCategory()
            },
            leadingIcon = { Icon(Icons.Outlined.Folder, null) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(Strings.btnDelete) },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}
