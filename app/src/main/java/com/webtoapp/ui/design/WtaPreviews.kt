package com.webtoapp.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.webtoapp.ui.theme.WebToAppThemeSimple

@Preview(name = "WTA Settings Light", showBackground = true, widthDp = 390)
@Preview(
    name = "WTA Settings Dark",
    showBackground = true,
    widthDp = 390,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "WTA Settings RTL",
    showBackground = true,
    widthDp = 390,
    locale = "ar"
)
@Composable
private fun WtaSettingsPreview() {
    WebToAppThemeSimple {
        WtaSettingsPreviewContent()
    }
}

@Composable
private fun WtaSettingsPreviewContent() {
    var enabled by remember { mutableStateOf(true) }
    var sliderValue by remember { mutableFloatStateOf(0.64f) }
    var textValue by remember { mutableStateOf("https://example.internal") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WtaSpacing.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
    ) {
        WtaStatusBanner(
            title = "Certificate warning",
            message = "This long status message verifies wrapping, truncation, icon alignment, and action sizing across compact layouts.",
            tone = WtaStatusTone.Warning,
            actionLabel = "Review",
            onAction = {}
        )

        WtaSection(
            title = "Common",
            description = "Stable rows for the primary create/edit flow.",
            level = WtaCapabilityLevel.Common
        ) {
            WtaSettingCard {
                WtaToggleRow(
                    title = "Use installed certificate authority",
                    subtitle = "Allow exported apps to trust user-installed certificates when the shell supports it.",
                    icon = Icons.Outlined.Lock,
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )
                WtaSectionDivider()
                WtaChoiceRow(
                    title = "Browser mode",
                    subtitle = "A deliberately long subtitle checks that setting rows keep a stable height and do not push trailing content off screen.",
                    icon = Icons.Outlined.OpenInBrowser,
                    value = "System WebView",
                    onClick = {}
                )
                WtaSectionDivider()
                WtaTextFieldRow(
                    title = "Start URL",
                    subtitle = "Input row smoke test",
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = "https://example.com"
                )
            }
        }

        WtaSection(
            title = "Advanced",
            description = "Collapsed by default in production screens.",
            level = WtaCapabilityLevel.Advanced,
            initiallyExpanded = true
        ) {
            WtaSettingCard {
                WtaSliderRow(
                    title = "Screen brightness",
                    subtitle = "Slider row smoke test",
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueLabel = "${(sliderValue * 100).toInt()}%"
                )
                WtaSectionDivider()
                WtaToggleRow(
                    title = "Disabled option",
                    subtitle = "Disabled rows should remain legible without inviting interaction.",
                    icon = Icons.Outlined.Tune,
                    checked = false,
                    onCheckedChange = {},
                    enabled = false
                )
            }
        }

        WtaSection(
            title = "Lab",
            description = "High-risk capabilities are visually separated from common settings.",
            level = WtaCapabilityLevel.Lab,
            initiallyExpanded = true
        ) {
            WtaSettingCard {
                WtaDangerRow(
                    title = "Reset experimental profile",
                    subtitle = "Danger row smoke test with constrained trailing content.",
                    icon = Icons.Outlined.Delete,
                    onClick = {}
                )
                WtaSectionDivider()
                WtaSettingRow(
                    title = "Locale preview",
                    subtitle = "RTL and long text should be contained inside the row.",
                    icon = Icons.Outlined.Language
                ) {
                    Text(
                        text = "Arabic",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
