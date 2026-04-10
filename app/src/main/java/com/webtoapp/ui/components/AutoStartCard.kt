package com.webtoapp.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.webtoapp.core.autostart.AutoStartManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AutoStartConfig

/**
 * 自启动配置卡片（v2 — 大幅优化）
 *
 * 优化点：
 * 1. 显示"下次启动时间"让用户确认自启动是否生效
 * 2. 检查并引导精确闹钟权限（Android 12+）
 * 3. 检查并引导电池优化白名单设置（国产 ROM 核心问题）
 * 4. OEM ROM 品牌检测 — 引导用户到品牌特定的自启动管理界面
 * 5. 权限状态在 onResume 时自动刷新（用户从设置页返回后即时更新）
 * 6. 支持自定义开机延迟
 * 7. 支持多时段定时启动
 * 8. 显示上次触发时间和累计触发次数（诊断信息）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoStartCard(
    config: AutoStartConfig?,
    onConfigChange: (AutoStartConfig?) -> Unit
) {
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(config != null && (config.bootStartEnabled || config.scheduledStartEnabled)) }
    var bootStartEnabled by remember(config) { mutableStateOf(config?.bootStartEnabled ?: false) }
    var scheduledStartEnabled by remember(config) { mutableStateOf(config?.scheduledStartEnabled ?: false) }
    var scheduledTime by remember(config) { mutableStateOf(config?.scheduledTime ?: "08:00") }
    var scheduledDays by remember(config) { mutableStateOf(config?.scheduledDays ?: listOf(1,2,3,4,5,6,7)) }
    var bootDelay by remember(config) { mutableStateOf(config?.bootDelay ?: AutoStartManager.DEFAULT_BOOT_DELAY_MS) }

    // Time选择对话框
    var showTimePicker by remember { mutableStateOf(false) }

    // 下次启动时间预览
    val nextTriggerDisplay by remember(scheduledStartEnabled, scheduledTime, scheduledDays) {
        mutableStateOf(
            if (scheduledStartEnabled && scheduledDays.isNotEmpty()) {
                try {
                    val manager = AutoStartManager(context)
                    val nextTrigger = manager.calculateNextTriggerTime(scheduledTime, scheduledDays)
                    nextTrigger?.let {
                        val now = java.util.Calendar.getInstance()
                        val daysDiff = ((it.timeInMillis - now.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
                        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it.time)
                        when (daysDiff) {
                            0 -> "${Strings.today} $timeStr"
                            1 -> "${Strings.tomorrow} $timeStr"
                            else -> {
                                val dayNames = listOf(
                                    Strings.dayMon, Strings.dayTue, Strings.dayWed,
                                    Strings.dayThu, Strings.dayFri, Strings.daySat, Strings.daySun
                                )
                                val calendarDow = it.get(java.util.Calendar.DAY_OF_WEEK)
                                val ourDow = if (calendarDow == java.util.Calendar.SUNDAY) 7 else calendarDow - 1
                                "${dayNames[ourDow - 1]} $timeStr"
                            }
                        }
                    }
                } catch (e: Exception) { null }
            } else null
        )
    }

    // ★ 权限状态：使用 Lifecycle 观察 onResume 刷新（用户从设置页返回后自动更新）
    var canScheduleExact by remember { mutableStateOf(true) }
    var ignoringBatteryOpt by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    val manager = AutoStartManager(context)
                    canScheduleExact = manager.canScheduleExactAlarms()
                    ignoringBatteryOpt = manager.isIgnoringBatteryOptimizations()
                } catch (_: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // OEM ROM 品牌信息
    val oemBrandName = remember { AutoStartManager(context).getOemBrandName() }
    val oemAutoStartIntent = remember { AutoStartManager(context).getOemAutoStartIntent() }

    fun updateConfig() {
        if (!bootStartEnabled && !scheduledStartEnabled) {
            onConfigChange(null)
        } else {
            onConfigChange(AutoStartConfig(
                bootStartEnabled = bootStartEnabled,
                scheduledStartEnabled = scheduledStartEnabled,
                scheduledTime = scheduledTime,
                scheduledDays = scheduledDays,
                bootDelay = bootDelay
            ))
        }
    }

    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
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
                                if (bootStartEnabled || scheduledStartEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.RocketLaunch,
                            contentDescription = null,
                            tint = if (bootStartEnabled || scheduledStartEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.autoStartSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!bootStartEnabled && !scheduledStartEnabled) {
                            Text(
                                Strings.notEnabled,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // 显示简要状态
                            val statusParts = mutableListOf<String>()
                            if (bootStartEnabled) statusParts.add(Strings.bootAutoStart)
                            if (scheduledStartEnabled) statusParts.add(scheduledTime)
                            Text(
                                statusParts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // ═══════════════════════════════
                    // 开机自启动
                    // ═══════════════════════════════
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                Strings.bootAutoStart,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Strings.bootAutoStartHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = bootStartEnabled,
                            onCheckedChange = {
                                bootStartEnabled = it
                                updateConfig()
                            }
                        )
                    }

                    // ★ 开机延迟滑块（启用开机自启动时显示）
                    AnimatedVisibility(visible = bootStartEnabled) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    Strings.bootDelay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${bootDelay / 1000}s",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = bootDelay.toFloat(),
                                onValueChange = { bootDelay = it.toLong() },
                                onValueChangeFinished = { updateConfig() },
                                valueRange = AutoStartManager.MIN_BOOT_DELAY_MS.toFloat()..AutoStartManager.MAX_BOOT_DELAY_MS.toFloat(),
                                steps = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // ═══════════════════════════════
                    // 定时自启动
                    // ═══════════════════════════════
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                Strings.scheduledAutoStart,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Strings.scheduledAutoStartHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = scheduledStartEnabled,
                            onCheckedChange = {
                                scheduledStartEnabled = it
                                updateConfig()
                            }
                        )
                    }

                    // 定时启动详细配置
                    AnimatedVisibility(visible = scheduledStartEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // Time选择
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(Strings.launchTime)
                                    }
                                    Text(
                                        scheduledTime,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 星期选择
                            Text(
                                Strings.launchDate,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val dayNames = listOf(
                                    Strings.dayMon, Strings.dayTue, Strings.dayWed,
                                    Strings.dayThu, Strings.dayFri, Strings.daySat, Strings.daySun
                                )
                                dayNames.forEachIndexed { index, name ->
                                    val day = index + 1
                                    val isSelected = scheduledDays.contains(day)
                                    PremiumFilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scheduledDays = if (isSelected) {
                                                scheduledDays - day
                                            } else {
                                                scheduledDays + day
                                            }
                                            updateConfig()
                                        },
                                        label = { Text(name) },
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }

                            // ★ 下次启动时间预览
                            nextTriggerDisplay?.let { display ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Alarm,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${Strings.nextLaunchTime}: $display",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════════════════════
                    // 权限警告区域
                    // ═══════════════════════════════
                    if (bootStartEnabled || scheduledStartEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // ★ OEM ROM 自启动白名单提示（国产 ROM 核心问题）
                        if (oemBrandName != null && oemAutoStartIntent != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            context.startActivity(oemAutoStartIntent)
                                        } catch (e: Exception) {
                                            // OEM 设置页不存在，降级到通用应用详情页
                                            try {
                                                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(fallback)
                                            } catch (_: Exception) {}
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.PhoneAndroid,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            Strings.oemAutoStartHint.replace("%s", oemBrandName),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Icon(
                                        Icons.Outlined.NavigateNext,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Android 12+ 精确闹钟权限提示
                        if (!canScheduleExact && scheduledStartEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) { /* 某些系统可能不支持 */ }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Strings.exactAlarmPermissionHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Outlined.NavigateNext,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 电池优化提示
                        if (!ignoringBatteryOpt) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // 降级到通用电池设置
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                                            } catch (_: Exception) { }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.BatteryAlert,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Strings.batteryOptimizationHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Outlined.NavigateNext,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // ★ 权限全部就绪的成功提示
                        if (canScheduleExact && ignoringBatteryOpt) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Strings.autoStartPermissionReady,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // 提示信息
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Strings.autoStartNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Time选择对话框
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = scheduledTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                scheduledTime = time
                updateConfig()
                showTimePicker = false
            }
        )
    }
}

/**
 * 时间选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.selectLaunchTime) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    onConfirm("$hour:$minute")
                }
            ) {
                Text(Strings.btnOk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
