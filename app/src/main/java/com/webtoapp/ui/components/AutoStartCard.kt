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
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.webtoapp.core.autostart.AutoStartManager
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.AutoStartConfig

/**
 * configcard( v2- )
 *
 * Note
 * 1. display" " user
 * 2. checkand( Android 12+)
 * 3. checkand settings( ROM)
 * 4. OEM ROM- user management
 * 5. state onResume refresh( userfromsettings back update)
 * 6. support
 * 7. support
 * 8. display( )
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

    // Timeselectdialog
    var showTimePicker by remember { mutableStateOf(false) }

    // preview
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
                            0 -> "${AppStringsProvider.current().today} $timeStr"
                            1 -> "${AppStringsProvider.current().tomorrow} $timeStr"
                            else -> {
                                val dayNames = listOf(
                                    AppStringsProvider.current().dayMon, AppStringsProvider.current().dayTue, AppStringsProvider.current().dayWed,
                                    AppStringsProvider.current().dayThu, AppStringsProvider.current().dayFri, AppStringsProvider.current().daySat, AppStringsProvider.current().daySun
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

    // ★ state: Lifecycle onResume refresh( userfromsettings back update)
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

    // OEM ROM
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
            // Note
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
                            AppStringsProvider.current().autoStartSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!bootStartEnabled && !scheduledStartEnabled) {
                            Text(
                                AppStringsProvider.current().notEnabled,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // display state
                            val statusParts = mutableListOf<String>()
                            if (bootStartEnabled) statusParts.add(AppStringsProvider.current().bootAutoStart)
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
                    // Note
                    // ═══════════════════════════════
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                AppStringsProvider.current().bootAutoStart,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                AppStringsProvider.current().bootAutoStartHint,
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

                    // ★( display)
                    AnimatedVisibility(visible = bootStartEnabled) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    AppStringsProvider.current().bootDelay,
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
                    // Note
                    // ═══════════════════════════════
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                AppStringsProvider.current().scheduledAutoStart,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                AppStringsProvider.current().scheduledAutoStartHint,
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

                    // config
                    AnimatedVisibility(visible = scheduledStartEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // Timeselect
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
                                        Text(AppStringsProvider.current().launchTime)
                                    }
                                    Text(
                                        scheduledTime,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // select
                            Text(
                                AppStringsProvider.current().launchDate,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val dayNames = listOf(
                                    AppStringsProvider.current().dayMon, AppStringsProvider.current().dayTue, AppStringsProvider.current().dayWed,
                                    AppStringsProvider.current().dayThu, AppStringsProvider.current().dayFri, AppStringsProvider.current().daySat, AppStringsProvider.current().daySun
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

                            // ★ preview
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
                                            "${AppStringsProvider.current().nextLaunchTime}: $display",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ═══════════════════════════════
                    // warningarea
                    // ═══════════════════════════════
                    if (bootStartEnabled || scheduledStartEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // ★ OEM ROM hint( ROM)
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
                                            // OEM settings, app
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
                                            AppStringsProvider.current().oemAutoStartHint.replace("%s", oemBrandName),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Outlined.NavigateNext,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Android 12+ hint
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
                                        } catch (e: Exception) { /* system support */ }
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
                                        AppStringsProvider.current().exactAlarmPermissionHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.AutoMirrored.Outlined.NavigateNext,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // hint
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
                                            // settings
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
                                        AppStringsProvider.current().batteryOptimizationHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.AutoMirrored.Outlined.NavigateNext,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // ★ all successhint
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
                                        AppStringsProvider.current().autoStartPermissionReady,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // hint
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
                                AppStringsProvider.current().autoStartNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Timeselectdialog
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
 * selectdialog
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
        title = { Text(AppStringsProvider.current().selectLaunchTime) },
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
                Text(AppStringsProvider.current().btnOk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )
}
