package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.KeyboardAdjustMode
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.SettingsSwitch

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun KeyboardAdjustModeCard(
    mode: KeyboardAdjustMode,
    onModeChange: (KeyboardAdjustMode) -> Unit
) {
    val isCustomized = mode != KeyboardAdjustMode.RESIZE

    Column {
        SettingsSwitch(
            title = Strings.keyboardAdjustModeLabel,
            subtitle = Strings.keyboardAdjustModeHint,
            checked = isCustomized,
            onCheckedChange = { checked ->
                onModeChange(if (checked) KeyboardAdjustMode.NOTHING else KeyboardAdjustMode.RESIZE)
            }
        )

        SystemCardExpandContent(visible = isCustomized) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        KeyboardAdjustMode.NOTHING to Strings.keyboardAdjustNothing,
                        KeyboardAdjustMode.RESIZE to Strings.keyboardAdjustResize
                    ).forEach { (value, label) ->
                        PremiumFilterChip(
                            selected = mode == value,
                            onClick = { onModeChange(value) },
                            label = { Text(label) }
                        )
                    }
                }

                val hintText = when (mode) {
                    KeyboardAdjustMode.RESIZE -> Strings.keyboardAdjustResizeHint
                    KeyboardAdjustMode.NOTHING -> Strings.keyboardAdjustNothingHint
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
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
                            text = hintText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
