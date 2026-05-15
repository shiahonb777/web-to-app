package com.webtoapp.ui.design

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

/**
 * Drop-down menu wrapper. The 2024.02 Compose BOM used here does not expose
 * [shape] or [containerColor] on DropdownMenu, so we cannot fully restyle
 * the container - but we can still give every consumer identical offset,
 * haptics and typography by funnelling through this helper.
 *
 * Use [WtaDropdownMenuItem] for the body so items stay consistent across
 * screens.
 */
@Composable
fun WtaDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 2.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        properties = properties,
        content = content
    )
}

@Composable
fun WtaDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val tint = when {
        !enabled -> colors.onSurface.copy(alpha = WtaAlpha.Disabled)
        destructive -> colors.error
        else -> colors.onSurface
    }
    val hapticClick = rememberHapticClick(onClick)
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = tint
            )
        },
        onClick = hapticClick,
        modifier = modifier,
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint
                )
            }
        },
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        colors = MenuDefaults.itemColors(
            textColor = tint,
            leadingIconColor = tint
        )
    )
}
