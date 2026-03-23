package com.webtoapp.core.forcedrun

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 强制运行权限引导组件
 * 
 * 引导用户授权必要的权限以启用强制运行功能
 */
@Composable
fun ForcedRunPermissionGuide(
    protectionLevel: ProtectionLevel,
    onAllPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Permission状态
    var hasAccessibility by remember { mutableStateOf(false) }
    var hasUsageStats by remember { mutableStateOf(false) }
    
    // Refresh权限状态
    fun refreshPermissions() {
        hasAccessibility = ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
        hasUsageStats = ForcedRunGuardService.hasUsageStatsPermission(context)
    }
    
    // 初始检查和定期刷新
    LaunchedEffect(Unit) {
        refreshPermissions()
    }
    
    // 每次 resume 时刷新
    DisposableEffect(Unit) {
        onDispose { }
    }
    
    // Check是否所有需要的权限都已授权
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
            // 标题
            Text(
                text = "强制运行权限设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "为了确保强制运行功能有效，请授权以下权限：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            // 辅助功能权限
            if (protectionLevel != ProtectionLevel.BASIC) {
                PermissionItem(
                    title = "辅助功能服务",
                    description = "监控窗口变化，防止用户切换应用",
                    isGranted = hasAccessibility,
                    onRequestPermission = {
                        ForcedRunAccessibilityService.openAccessibilitySettings(context)
                    },
                    onRefresh = { refreshPermissions() }
                )
            }
            
            // 使用情况访问权限
            if (protectionLevel == ProtectionLevel.MAXIMUM) {
                PermissionItem(
                    title = "使用情况访问",
                    description = "检测当前前台应用，提供双重防护",
                    isGranted = hasUsageStats,
                    onRequestPermission = {
                        ForcedRunGuardService.openUsageAccessSettings(context)
                    },
                    onRefresh = { refreshPermissions() }
                )
            }
            
            // 防护级别说明
            Divider()
            
            ProtectionLevelInfo(protectionLevel)
            
            // Refresh按钮
            OutlinedButton(
                onClick = { refreshPermissions() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("刷新权限状态")
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
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 状态图标
        Icon(
            imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
            modifier = Modifier.size(28.dp)
        )
        
        // Permission信息
        Column(modifier = Modifier.weight(1f)) {
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
        
        // 操作按钮
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
                Text("授权", fontSize = 14.sp)
            }
        } else {
            Text(
                text = "已授权",
                color = Color(0xFF4CAF50),
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
            "基础防护",
            "仅拦截返回键，防护效果有限",
            Color(0xFF9E9E9E)
        )
        ProtectionLevel.STANDARD -> Triple(
            "标准防护",
            "通过辅助功能监控窗口，有效阻止应用切换",
            Color(0xFF2196F3)
        )
        ProtectionLevel.MAXIMUM -> Triple(
            "最强防护",
            "辅助功能 + 后台守护服务，双重防护确保万无一失",
            Color(0xFF4CAF50)
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

/**
 * 权限检查对话框
 */
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
                text = if (permissionStatus.isFullyGranted) "权限已就绪" else "需要授权",
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
                    Text("开始")
                }
            } else {
                TextButton(onClick = onContinueAnyway) {
                    Text("跳过（降级防护）")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
