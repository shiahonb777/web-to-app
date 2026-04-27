package com.webtoapp.ui.screens.extensionmodule.editor.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.extension.ModuleConfigItem
import com.webtoapp.core.extension.ModulePermission
import com.webtoapp.core.extension.ModuleRunMode
import com.webtoapp.core.extension.ModuleRunTime
import com.webtoapp.core.extension.ModuleUiConfig
import com.webtoapp.core.extension.UrlMatchRule
import com.webtoapp.core.i18n.AppStringsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTab(
    runAt: ModuleRunTime,
    onRunAtClick: () -> Unit,
    runMode: ModuleRunMode,
    onRunModeClick: () -> Unit,
    permissions: Set<ModulePermission>,
    onPermissionsClick: () -> Unit,
    urlMatches: List<UrlMatchRule>,
    onUrlMatchesClick: () -> Unit,
    configItems: List<ModuleConfigItem>,
    onConfigItemsClick: () -> Unit,
    uiConfig: ModuleUiConfig,
    onUiTypeClick: () -> Unit
) {
    @Composable
    fun AdvancedOptionCard(
        title: String,
        subtitle: String,
        icon: @Composable () -> Unit,
        iconTint: Color,
        onClick: () -> Unit
    ) {
        Surface(
            onClick = onClick,
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    iconTint.copy(alpha = 0.12f),
                                    iconTint.copy(alpha = 0.04f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val secondaryColor = MaterialTheme.colorScheme.secondary

        AdvancedOptionCard(
            title = AppStringsProvider.current().uiTypeConfig,
            subtitle = uiConfig.type.getDisplayName(),
            icon = {
                Icon(
                    Icons.Default.Widgets,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tertiaryColor
                )
            },
            iconTint = tertiaryColor,
            onClick = onUiTypeClick
        )

        AdvancedOptionCard(
            title = AppStringsProvider.current().runModeLabel,
            subtitle = "${runMode.getDisplayName()} · ${runMode.getDescription()}",
            icon = {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = primaryColor
                )
            },
            iconTint = primaryColor,
            onClick = onRunModeClick
        )

        AdvancedOptionCard(
            title = AppStringsProvider.current().runTime,
            subtitle = runAt.getDisplayName(),
            icon = {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = secondaryColor
                )
            },
            iconTint = secondaryColor,
            onClick = onRunAtClick
        )

        AdvancedOptionCard(
            title = AppStringsProvider.current().requiredPermissions,
            subtitle = if (permissions.isEmpty()) {
                AppStringsProvider.current().noSpecialPermissions
            } else {
                permissions.joinToString { it.displayName }
            },
            icon = {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            },
            iconTint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            onClick = onPermissionsClick
        )

        AdvancedOptionCard(
            title = AppStringsProvider.current().urlMatchRules,
            subtitle = if (urlMatches.isEmpty()) {
                AppStringsProvider.current().matchAllWebsites
            } else {
                AppStringsProvider.current().rulesCount.replace("%d", urlMatches.size.toString())
            },
            icon = {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = primaryColor
                )
            },
            iconTint = primaryColor,
            onClick = onUrlMatchesClick
        )

        AdvancedOptionCard(
            title = AppStringsProvider.current().userConfigItems,
            subtitle = if (configItems.isEmpty()) {
                AppStringsProvider.current().noConfigItems
            } else {
                AppStringsProvider.current().configItemsCount.replace("%d", configItems.size.toString())
            },
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = secondaryColor
                )
            },
            iconTint = secondaryColor,
            onClick = onConfigItemsClick
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.10f),
                                    primaryColor.copy(alpha = 0.03f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = primaryColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        AppStringsProvider.current().developerGuide,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        AppStringsProvider.current().developerGuideContent,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
