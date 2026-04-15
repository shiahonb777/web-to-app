package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.model.CloudProject
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.CloudAppConfig
import com.webtoapp.ui.viewmodel.CloudViewModel

@Composable
fun CloudAppConfigCard(
    config: CloudAppConfig?,
    cloudViewModel: CloudViewModel,
    onConfigChange: (CloudAppConfig?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val projects by cloudViewModel.projects.collectAsStateWithLifecycle()
    val projectsLoading by cloudViewModel.projectsLoading.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(config?.enabled == true) }
    var enabled by remember(config) { mutableStateOf(config?.enabled == true) }
    var selectedProjectId by remember(config) { mutableStateOf(config?.projectId?.takeIf { it > 0 }) }

    LaunchedEffect(Unit) {
        cloudViewModel.loadProjects()
    }

    fun selectedProject(): CloudProject? = projects.firstOrNull { it.id == selectedProjectId }

    fun pushConfig() {
        if (!enabled) {
            onConfigChange(null)
            return
        }

        val project = selectedProject()
        onConfigChange(
            CloudAppConfig(
                enabled = true,
                projectId = project?.id ?: 0,
                projectKey = project?.projectKey ?: "",
                projectName = project?.name ?: "",
                updateCheckEnabled = config?.updateCheckEnabled ?: true,
                announcementEnabled = config?.announcementEnabled ?: true,
                remoteConfigEnabled = config?.remoteConfigEnabled ?: true,
                activationCodeEnabled = config?.activationCodeEnabled ?: false,
                statsReportEnabled = config?.statsReportEnabled ?: true,
                fcmPushEnabled = config?.fcmPushEnabled ?: false,
                remoteScriptEnabled = config?.remoteScriptEnabled ?: false,
                reportCrashes = config?.reportCrashes ?: true,
                updateCheckInterval = config?.updateCheckInterval ?: 3600,
                forceUpdateEnabled = config?.forceUpdateEnabled ?: false,
                statsReportInterval = config?.statsReportInterval ?: 3600,
            )
        )
    }

    EnhancedElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.CloudDone,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "云项目联动",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = selectedProject()?.name?.ifBlank { "未选择项目" } ?: if (enabled) "未选择项目" else "未启用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        if (!it) selectedProjectId = null
                        pushConfig()
                    }
                )
            }

            AnimatedVisibility(
                visible = expanded && enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "选择一个云项目，把更新检查、公告、远程配置等能力嵌入导出的 APK。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (projectsLoading) {
                        Text(
                            text = "正在加载云项目…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (projects.isEmpty()) {
                        Text(
                            text = "当前没有可用云项目。请先在云端页面创建项目。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        SettingsSection(title = "云项目") {
                            projects.forEach { project ->
                                val selected = selectedProjectId == project.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedProjectId = project.id
                                            pushConfig()
                                        }
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else Color.Transparent
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = project.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        project.description?.takeIf { it.isNotBlank() }?.let {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Key: ${project.projectKey}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (selected) {
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Outlined.Link,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val effectiveConfig = config?.takeIf { enabled } ?: CloudAppConfig(enabled = true)

                    SettingsSection(title = "启用能力") {
                        SettingsSwitch(
                            title = "更新检查",
                            subtitle = "启动后检查云端新版本并提示用户更新",
                            checked = effectiveConfig.updateCheckEnabled,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(updateCheckEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = "公告推送",
                            subtitle = "从云端拉取公告并在应用内展示",
                            checked = effectiveConfig.announcementEnabled,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(announcementEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = "远程配置",
                            subtitle = "允许导出后的应用读取云端远程配置",
                            checked = effectiveConfig.remoteConfigEnabled,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(remoteConfigEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = "在线激活码",
                            subtitle = "通过云端服务进行在线激活验证",
                            checked = effectiveConfig.activationCodeEnabled,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(activationCodeEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = "统计上报",
                            subtitle = "上报安装、打开、活跃等基础统计数据",
                            checked = effectiveConfig.statsReportEnabled,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(statsReportEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = "推送通知",
                            subtitle = "启用 FCM 推送能力",
                            checked = effectiveConfig.fcmPushEnabled,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(fcmPushEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = "远程脚本热更",
                            subtitle = "允许云端下发远程脚本",
                            checked = effectiveConfig.remoteScriptEnabled,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(remoteScriptEnabled = it)) }
                        )
                        SettingsSwitch(
                            title = "崩溃上报",
                            subtitle = "自动上传崩溃信息以便排查问题",
                            checked = effectiveConfig.reportCrashes,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(reportCrashes = it)) }
                        )
                        SettingsSwitch(
                            title = "强制更新",
                            subtitle = "启用后可在云端标记某些版本为必须更新",
                            checked = effectiveConfig.forceUpdateEnabled,
                            onCheckedChange = { onConfigChange(effectiveConfig.copy(forceUpdateEnabled = it)) }
                        )
                    }

                    SettingsSection(title = "当前绑定") {
                        val project = selectedProject()
                        Text(
                            text = "项目：${project?.name ?: effectiveConfig.projectName.ifBlank { "未选择" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Project Key：${project?.projectKey ?: effectiveConfig.projectKey.ifBlank { "未填写" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
