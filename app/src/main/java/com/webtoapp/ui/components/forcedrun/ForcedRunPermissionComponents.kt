package com.webtoapp.ui.components.forcedrun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.forcedrun.ForcedRunAccessibilityService
import com.webtoapp.core.forcedrun.ForcedRunGuardService
import com.webtoapp.core.forcedrun.ForcedRunManager
import com.webtoapp.core.forcedrun.ProtectionLevel
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.AppColors

@Composable
fun ForcedRunPermissionGuide(
    protectionLevel: ProtectionLevel,
    onAllPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAccessibility by remember { mutableStateOf(false) }
    var hasUsageStats by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        hasAccessibility = ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
        hasUsageStats = ForcedRunGuardService.hasUsageStatsPermission(context)
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    DisposableEffect(Unit) {
        onDispose { }
    }

    LaunchedEffect(hasAccessibility, hasUsageStats) {
        val allGranted = when (protectionLevel) {
            ProtectionLevel.BASIC -> true
            ProtectionLevel.STANDARD -> hasAccessibility
            ProtectionLevel.MAXIMUM -> hasAccessibility && hasUsageStats
        }
        if (allGranted) {
            onAllPermissionsGranted()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = Strings.forcedRunPermissionTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = Strings.forcedRunPermissionDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            if (protectionLevel != ProtectionLevel.BASIC) {
                PermissionItem(
                    title = Strings.accessibilityService,
                    description = Strings.accessibilityServiceDesc,
                    isGranted = hasAccessibility,
                    onRequestPermission = {
                        ForcedRunAccessibilityService.openAccessibilitySettings(context)
                    }
                )
            }

            if (protectionLevel == ProtectionLevel.MAXIMUM) {
                PermissionItem(
                    title = Strings.usageAccess,
                    description = Strings.usageAccessDesc,
                    isGranted = hasUsageStats,
                    onRequestPermission = {
                        ForcedRunGuardService.openUsageAccessSettings(context)
                    }
                )
            }

            HorizontalDivider()
            ProtectionLevelInfo(protectionLevel)

            OutlinedButton(
                onClick = { refreshPermissions() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.refreshPermissionStatus)
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (isGranted) AppColors.Success else AppColors.Warning,
            modifier = Modifier.size(28.dp)
        )

        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isGranted) {
            FilledTonalButton(
                onClick = onRequestPermission,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(Strings.grant, fontSize = 14.sp)
            }
        } else {
            Text(
                text = Strings.granted,
                color = AppColors.Success,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ProtectionLevelInfo(level: ProtectionLevel) {
    val (levelName, levelDescription, levelColor) = when (level) {
        ProtectionLevel.BASIC -> Triple(
            Strings.protectionBasic,
            Strings.protectionBasicDesc,
            Color(0xFF9E9E9E)
        )
        ProtectionLevel.STANDARD -> Triple(
            Strings.protectionStandard,
            Strings.protectionStandardDesc,
            Color(0xFF2196F3)
        )
        ProtectionLevel.MAXIMUM -> Triple(
            Strings.protectionMaximum,
            Strings.protectionMaximumDesc,
            AppColors.Success
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = levelColor.copy(alpha = 0.2f)
        ) {
            Text(
                text = levelName,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = levelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Text(
            text = levelDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ForcedRunPermissionDialog(
    protectionLevel: ProtectionLevel,
    onDismiss: () -> Unit,
    onContinueAnyway: () -> Unit,
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionStatus = remember(protectionLevel) {
        ForcedRunManager.checkProtectionPermissions(context, protectionLevel)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (permissionStatus.isFullyGranted) Strings.permissionsReady else Strings.permissionsNeeded,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            ForcedRunPermissionGuide(
                protectionLevel = protectionLevel,
                onAllPermissionsGranted = onAllPermissionsGranted
            )
        },
        confirmButton = {
            if (permissionStatus.isFullyGranted) {
                Button(onClick = onAllPermissionsGranted) {
                    Text(Strings.start)
                }
            } else {
                TextButton(onClick = onContinueAnyway) {
                    Text(Strings.skipDegradedProtection)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
