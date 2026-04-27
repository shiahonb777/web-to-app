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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.util.SvgIconMapper

@Composable
fun IconPickerDialog(
    currentIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val icons = listOf(
        "package", "block", "palette", "bolt", "analytics", "mouse", "movie", "lock", "wrench",
        "dark_mode", "list", "clipboard", "image", "fast_forward", "shield", "book", "font_download", "globe",
        "target", "lightbulb", "wrench", "settings", "gaming", "music_note", "phone_android", "computer", "star",
        "fire", "diamond", "gift", "trophy", "festival", "theater_comedy", "palette", "movie", "camera"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().selectIcon) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                icons.chunked(6).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { iconId ->
                            val isSelected = iconId == currentIcon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                                    MaterialTheme.colorScheme.surfaceContainerLow
                                                )
                                            )
                                        }
                                    )
                                    .clickable { onIconSelected(iconId) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    SvgIconMapper.getIcon(iconId),
                                    contentDescription = iconId,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().cancel)
            }
        }
    )
}
