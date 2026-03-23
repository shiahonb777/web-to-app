package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.forcedrun.ForcedRunMode
import com.webtoapp.core.i18n.Strings

/**
 * Forced run config card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForcedRunConfigCard(
    config: ForcedRunConfig?,
    onConfigChange: (ForcedRunConfig?) -> Unit
) {
    var expanded by remember { mutableStateOf(config?.enabled == true) }
    var enabled by remember(config) { mutableStateOf(config?.enabled ?: false) }
    var mode by remember(config) { mutableStateOf(config?.mode ?: ForcedRunMode.FIXED_TIME) }
    
    // 固定时间模式
    var startTime by remember(config) { mutableStateOf(config?.startTime ?: "08:00") }
    var endTime by remember(config) { mutableStateOf(config?.endTime ?: "12:00") }
    var activeDays by remember(config) { mutableStateOf(config?.activeDays ?: listOf(1, 2, 3, 4, 5, 6, 7)) }
    
    // 倒计时模式
    var countdownMinutes by remember(config) { mutableStateOf(config?.countdownMinutes ?: 60) }
    
    // 限时模式
    var accessStartTime by remember(config) { mutableStateOf(config?.accessStartTime ?: "08:00") }
    var accessEndTime by remember(config) { mutableStateOf(config?.accessEndTime ?: "22:00") }
    var accessDays by remember(config) { mutableStateOf(config?.accessDays ?: listOf(1, 2, 3, 4, 5, 6, 7)) }
    
    // 通用配置
    var blockSystemUI by remember(config) { mutableStateOf(config?.blockSystemUI ?: true) }
    var blockBackButton by remember(config) { mutableStateOf(config?.blockBackButton ?: true) }
    var blockHomeButton by remember(config) { mutableStateOf(config?.blockHomeButton ?: true) }
    var showCountdown by remember(config) { mutableStateOf(config?.showCountdown ?: true) }
    var allowEmergencyExit by remember(config) { mutableStateOf(config?.allowEmergencyExit ?: false) }
    var emergencyPassword by remember(config) { mutableStateOf(config?.emergencyPassword ?: "") }
    
    // Time选择对话框
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showAccessStartTimePicker by remember { mutableStateOf(false) }
    var showAccessEndTimePicker by remember { mutableStateOf(false) }
    
    fun updateConfig() {
        if (!enabled) {
            onConfigChange(null)
        } else {
            onConfigChange(ForcedRunConfig(
                enabled = true,
                mode = mode,
                startTime = startTime,
                endTime = endTime,
                activeDays = activeDays,
                countdownMinutes = countdownMinutes,
                accessStartTime = accessStartTime,
                accessEndTime = accessEndTime,
                accessDays = accessDays,
                blockSystemUI = blockSystemUI,
                blockBackButton = blockBackButton,
                blockHomeButton = blockHomeButton,
                showCountdown = showCountdown,
                allowEmergencyExit = allowEmergencyExit,
                emergencyPassword = emergencyPassword.takeIf { it.isNotBlank() }
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
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.forcedRunSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (enabled) Strings.configured else Strings.notEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Enable开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.enableForcedRun,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Strings.forcedRunHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                updateConfig()
                            }
                        )
                    }
                    
                    AnimatedVisibility(visible = enabled) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 运行模式选择
                            Text(
                                Strings.forcedRunMode,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = mode == ForcedRunMode.FIXED_TIME,
                                    onClick = {
                                        mode = ForcedRunMode.FIXED_TIME
                                        updateConfig()
                                    },
                                    label = { Text(Strings.fixedTimeMode) },
                                    leadingIcon = if (mode == ForcedRunMode.FIXED_TIME) {
                                        { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = mode == ForcedRunMode.COUNTDOWN,
                                    onClick = {
                                        mode = ForcedRunMode.COUNTDOWN
                                        updateConfig()
                                    },
                                    label = { Text(Strings.countdownMode) },
                                    leadingIcon = if (mode == ForcedRunMode.COUNTDOWN) {
                                        { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = mode == ForcedRunMode.DURATION,
                                    onClick = {
                                        mode = ForcedRunMode.DURATION
                                        updateConfig()
                                    },
                                    label = { Text(Strings.durationMode) },
                                    leadingIcon = if (mode == ForcedRunMode.DURATION) {
                                        { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 根据模式显示不同配置
                            when (mode) {
                                ForcedRunMode.FIXED_TIME -> {
                                    // 固定时间段配置
                                    Text(
                                        Strings.fixedTimeModeHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Start时间
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showStartTimePicker = true }
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
                                                    Icons.Outlined.PlayArrow,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(Strings.forcedRunStartTime)
                                            }
                                            Text(
                                                startTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // End时间
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showEndTimePicker = true }
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
                                                    Icons.Outlined.Stop,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(Strings.forcedRunEndTime)
                                            }
                                            Text(
                                                endTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 生效日期
                                    Text(
                                        Strings.activeDays,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    DaySelector(
                                        selectedDays = activeDays,
                                        onDaysChange = {
                                            activeDays = it
                                            updateConfig()
                                        }
                                    )
                                }
                                
                                ForcedRunMode.COUNTDOWN -> {
                                    // 倒计时模式配置
                                    Text(
                                        Strings.countdownModeHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    OutlinedTextField(
                                        value = countdownMinutes.toString(),
                                        onValueChange = { value ->
                                            value.toIntOrNull()?.let {
                                                countdownMinutes = it.coerceIn(1, 480)
                                                updateConfig()
                                            }
                                        },
                                        label = { Text(Strings.countdownDuration) },
                                        suffix = { Text(Strings.minutes) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // 快捷预设
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(25, 45, 60, 90, 120).forEach { minutes ->
                                            AssistChip(
                                                onClick = {
                                                    countdownMinutes = minutes
                                                    updateConfig()
                                                },
                                                label = { Text("${minutes}${Strings.minutesShort}") }
                                            )
                                        }
                                    }
                                }
                                
                                ForcedRunMode.DURATION -> {
                                    // 限时模式配置
                                    Text(
                                        Strings.durationModeHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 可进入开始时间
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showAccessStartTimePicker = true }
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
                                                    Icons.Outlined.LockOpen,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(Strings.accessStartTime)
                                            }
                                            Text(
                                                accessStartTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // 可进入结束时间
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showAccessEndTimePicker = true }
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
                                                    Icons.Outlined.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(Strings.accessEndTime)
                                            }
                                            Text(
                                                accessEndTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 可进入日期
                                    Text(
                                        Strings.accessDays,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    DaySelector(
                                        selectedDays = accessDays,
                                        onDaysChange = {
                                            accessDays = it
                                            updateConfig()
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 高级选项
                            Text(
                                Strings.advancedOptions,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // 屏蔽系统UI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(Strings.blockSystemUI, style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = blockSystemUI,
                                    onCheckedChange = {
                                        blockSystemUI = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            // 屏蔽返回键
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(Strings.blockBackButton, style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = blockBackButton,
                                    onCheckedChange = {
                                        blockBackButton = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            // 屏蔽Home键
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(Strings.blockHomeButton, style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = blockHomeButton,
                                    onCheckedChange = {
                                        blockHomeButton = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            // Show倒计时
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(Strings.showCountdownTimer, style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = showCountdown,
                                    onCheckedChange = {
                                        showCountdown = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 紧急退出
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(Strings.allowEmergencyExit, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        Strings.emergencyExitHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = allowEmergencyExit,
                                    onCheckedChange = {
                                        allowEmergencyExit = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            AnimatedVisibility(visible = allowEmergencyExit) {
                                OutlinedTextField(
                                    value = emergencyPassword,
                                    onValueChange = {
                                        emergencyPassword = it
                                        updateConfig()
                                    },
                                    label = { Text(Strings.emergencyPassword) },
                                    placeholder = { Text(Strings.emergencyPasswordHint) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                            
                        }
                    }
                }
            }
        }
    }
    
    // Time选择对话框
    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { time ->
                startTime = time
                updateConfig()
                showStartTimePicker = false
            }
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { time ->
                endTime = time
                updateConfig()
                showEndTimePicker = false
            }
        )
    }
    
    if (showAccessStartTimePicker) {
        TimePickerDialog(
            initialTime = accessStartTime,
            onDismiss = { showAccessStartTimePicker = false },
            onConfirm = { time ->
                accessStartTime = time
                updateConfig()
                showAccessStartTimePicker = false
            }
        )
    }
    
    if (showAccessEndTimePicker) {
        TimePickerDialog(
            initialTime = accessEndTime,
            onDismiss = { showAccessEndTimePicker = false },
            onConfirm = { time ->
                accessEndTime = time
                updateConfig()
                showAccessEndTimePicker = false
            }
        )
    }
}

/**
 * 星期选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySelector(
    selectedDays: List<Int>,
    onDaysChange: (List<Int>) -> Unit
) {
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
            val isSelected = selectedDays.contains(day)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newDays = if (isSelected) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                    onDaysChange(newDays)
                },
                label = { Text(name) },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
