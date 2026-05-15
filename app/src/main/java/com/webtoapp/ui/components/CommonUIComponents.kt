package com.webtoapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.design.WtaCard
import com.webtoapp.ui.design.WtaCardTone
import com.webtoapp.ui.design.WtaErrorState
import com.webtoapp.ui.design.WtaFullEmptyState
import com.webtoapp.ui.design.WtaIconTitle
import com.webtoapp.ui.design.WtaInfoChip
import com.webtoapp.ui.design.WtaLoadingState
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaStatItem
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaSwitch

/**
 * Legacy common UI helpers kept as thin forwarders over the Wta design system.
 * Prefer the Wta components directly in new code. These exist to avoid a
 * flag-day migration of every screen at once.
 */

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = WtaSpacing.Medium)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        WtaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = WtaSpacing.Small)
        )
        WtaCard(
            modifier = Modifier.fillMaxWidth(),
            tone = WtaCardTone.Surface,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(WtaSpacing.Large)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(WtaSpacing.Small),
                content = content
            )
        }
    }
}

@Composable
fun IconSwitchCard(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    WtaCard(
        modifier = modifier.fillMaxWidth(),
        tone = WtaCardTone.Surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WtaIconTitle(
                icon = icon,
                title = title,
                subtitle = subtitle,
                enabled = checked,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(WtaSpacing.Small))
            WtaSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun IconSwitchCard(
    title: String,
    iconPainter: Painter,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    // Painter variant: fall back to a plain row without an icon plate so we don't
    // have to duplicate the tinted plate rendering. Legacy callers keep working.
    WtaCard(
        modifier = modifier.fillMaxWidth(),
        tone = WtaCardTone.Surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(WtaSpacing.Small))
            WtaSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun InfoChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    WtaInfoChip(label = label, icon = icon, modifier = modifier)
}

@Composable
fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    WtaStatItem(value = value, label = label, modifier = modifier)
}

@Composable
fun EmptyStatePlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    WtaFullEmptyState(
        title = title,
        message = subtitle,
        icon = icon,
        modifier = modifier,
        action = action
    )
}

@Composable
fun LoadingPlaceholder(
    message: String,
    modifier: Modifier = Modifier
) {
    WtaLoadingState(modifier = modifier, message = message)
}

@Composable
fun ErrorPlaceholder(
    message: String,
    onRetry: () -> Unit,
    retryText: String = "Retry",
    modifier: Modifier = Modifier
) {
    WtaErrorState(
        message = message,
        retryLabel = retryText,
        onRetry = onRetry,
        modifier = modifier
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WtaSpacing.Tiny),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        WtaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun CardContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(WtaSpacing.Large),
        content = content
    )
}

@Composable
fun CardContentWithSpacing(
    modifier: Modifier = Modifier,
    spacing: Dp = WtaSpacing.Large,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(WtaSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
fun IconTitleRow(
    icon: ImageVector,
    title: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    WtaIconTitle(
        icon = icon,
        title = title,
        enabled = enabled,
        modifier = modifier
    )
}

@Composable
fun IconTitleRow(
    iconPainter: Painter,
    title: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Painter variant retained for callers that use vector resource painters.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            painter = iconPainter,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = WtaSpacing.Small)
        )
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun CollapsibleCardHeader(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WtaIconTitle(icon = icon, title = title, enabled = checked)
        WtaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun CollapsibleCardHeader(
    iconPainter: Painter,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTitleRow(iconPainter = iconPainter, title = title, enabled = checked)
        WtaSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun WarningCard(
    message: String,
    modifier: Modifier = Modifier
) {
    WtaStatusBanner(
        message = message,
        tone = WtaStatusTone.Warning,
        modifier = modifier
    )
}
