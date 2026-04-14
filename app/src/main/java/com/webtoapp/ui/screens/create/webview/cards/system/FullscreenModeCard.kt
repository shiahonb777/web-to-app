package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.CollapsibleCardHeader
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumSwitch

@Composable
fun FullscreenModeCard(
    enabled: Boolean,
    showStatusBar: Boolean = false,
    showNavigationBar: Boolean = false,
    showToolbar: Boolean = false,
    onEnabledChange: (Boolean) -> Unit,
    onShowStatusBarChange: (Boolean) -> Unit = {},
    onShowNavigationBarChange: (Boolean) -> Unit = {},
    onShowToolbarChange: (Boolean) -> Unit = {}
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CollapsibleCardHeader(
                icon = Icons.Outlined.Fullscreen,
                title = Strings.fullscreenMode,
                checked = enabled,
                onCheckedChange = onEnabledChange
            )

            SystemCardExpandContent(visible = enabled) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    FullscreenToggleRow(
                        title = Strings.showStatusBar,
                        subtitle = Strings.showStatusBarHint,
                        checked = showStatusBar,
                        onCheckedChange = onShowStatusBarChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FullscreenToggleRow(
                        title = Strings.showNavigationBar,
                        subtitle = Strings.showNavigationBarHint,
                        checked = showNavigationBar,
                        onCheckedChange = onShowNavigationBarChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FullscreenToggleRow(
                        title = Strings.showToolbar,
                        subtitle = Strings.showToolbarHint,
                        checked = showToolbar,
                        onCheckedChange = onShowToolbarChange
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        PremiumSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
