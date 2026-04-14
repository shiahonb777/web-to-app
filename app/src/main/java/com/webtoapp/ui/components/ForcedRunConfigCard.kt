package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import com.webtoapp.core.forcedrun.ForcedRunConfig
import com.webtoapp.core.forcedrun.ForcedRunMode
import com.webtoapp.core.i18n.AppStringsProvider

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
    
    // mode
    var startTime by remember(config) { mutableStateOf(config?.startTime ?: "08:00") }
    var endTime by remember(config) { mutableStateOf(config?.endTime ?: "12:00") }
    var activeDays by remember(config) { mutableStateOf(config?.activeDays ?: listOf(1, 2, 3, 4, 5, 6, 7)) }
    
    // mode
    var countdownMinutes by remember(config) { mutableIntStateOf(config?.countdownMinutes ?: 60) }
    
    // mode
    var accessStartTime by remember(config) { mutableStateOf(config?.accessStartTime ?: "08:00") }
    var accessEndTime by remember(config) { mutableStateOf(config?.accessEndTime ?: "22:00") }
    var accessDays by remember(config) { mutableStateOf(config?.accessDays ?: listOf(1, 2, 3, 4, 5, 6, 7)) }
    
    // config
    var blockSystemUI by remember(config) { mutableStateOf(config?.blockSystemUI ?: true) }
    var blockBackButton by remember(config) { mutableStateOf(config?.blockBackButton ?: true) }
    var blockHomeButton by remember(config) { mutableStateOf(config?.blockHomeButton ?: true) }
    var showCountdown by remember(config) { mutableStateOf(config?.showCountdown ?: true) }
    var allowEmergencyExit by remember(config) { mutableStateOf(config?.allowEmergencyExit ?: false) }
    var emergencyPassword by remember(config) { mutableStateOf(config?.emergencyPassword ?: "") }
    
    // Timeselectdialog
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
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.DirectionsRun,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            AppStringsProvider.current().forcedRunSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!enabled) {
                            Text(
                                AppStringsProvider.current().notEnabled,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    // Enable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                AppStringsProvider.current().enableForcedRun,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                AppStringsProvider.current().forcedRunHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
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
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // runmodeselect
                            Text(
                                AppStringsProvider.current().forcedRunMode,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PremiumFilterChip(
                                    selected = mode == ForcedRunMode.FIXED_TIME,
                                    onClick = {
                                        mode = ForcedRunMode.FIXED_TIME
                                        updateConfig()
                                    },
                                    label = { Text(AppStringsProvider.current().fixedTimeMode) },
                                    leadingIcon = if (mode == ForcedRunMode.FIXED_TIME) {
                                        { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                                    } else null
                                )
                                PremiumFilterChip(
                                    selected = mode == ForcedRunMode.COUNTDOWN,
                                    onClick = {
                                        mode = ForcedRunMode.COUNTDOWN
                                        updateConfig()
                                    },
                                    label = { Text(AppStringsProvider.current().countdownMode) },
                                    leadingIcon = if (mode == ForcedRunMode.COUNTDOWN) {
                                        { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                                    } else null
                                )
                                PremiumFilterChip(
                                    selected = mode == ForcedRunMode.DURATION,
                                    onClick = {
                                        mode = ForcedRunMode.DURATION
                                        updateConfig()
                                    },
                                    label = { Text(AppStringsProvider.current().durationMode) },
                                    leadingIcon = if (mode == ForcedRunMode.DURATION) {
                                        { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // modedisplay config
                            when (mode) {
                                ForcedRunMode.FIXED_TIME -> {
                                    // config
                                    Text(
                                        AppStringsProvider.current().fixedTimeModeHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Start
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
                                                Text(AppStringsProvider.current().forcedRunStartTime)
                                            }
                                            Text(
                                                startTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // End
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
                                                Text(AppStringsProvider.current().forcedRunEndTime)
                                            }
                                            Text(
                                                endTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Note
                                    Text(
                                        AppStringsProvider.current().activeDays,
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
                                    // modeconfig
                                    Text(
                                        AppStringsProvider.current().countdownModeHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    PremiumTextField(
                                        value = countdownMinutes.toString(),
                                        onValueChange = { value ->
                                            value.toIntOrNull()?.let {
                                                countdownMinutes = it.coerceIn(1, 480)
                                                updateConfig()
                                            }
                                        },
                                        label = { Text(AppStringsProvider.current().countdownDuration) },
                                        suffix = { Text(AppStringsProvider.current().minutes) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Note
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
                                                label = { Text("${minutes}${AppStringsProvider.current().minutesShort}") }
                                            )
                                        }
                                    }
                                }
                                
                                ForcedRunMode.DURATION -> {
                                    // modeconfig
                                    Text(
                                        AppStringsProvider.current().durationModeHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Note
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
                                                Text(AppStringsProvider.current().accessStartTime)
                                            }
                                            Text(
                                                accessStartTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Note
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
                                                Text(AppStringsProvider.current().accessEndTime)
                                            }
                                            Text(
                                                accessEndTime,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Note
                                    Text(
                                        AppStringsProvider.current().accessDays,
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
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // advanced
                            Text(
                                AppStringsProvider.current().advancedOptions,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // systemUI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(AppStringsProvider.current().blockSystemUI, style = MaterialTheme.typography.bodyMedium)
                                PremiumSwitch(
                                    checked = blockSystemUI,
                                    onCheckedChange = {
                                        blockSystemUI = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            // back
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(AppStringsProvider.current().blockBackButton, style = MaterialTheme.typography.bodyMedium)
                                PremiumSwitch(
                                    checked = blockBackButton,
                                    onCheckedChange = {
                                        blockBackButton = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            // Home
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(AppStringsProvider.current().blockHomeButton, style = MaterialTheme.typography.bodyMedium)
                                PremiumSwitch(
                                    checked = blockHomeButton,
                                    onCheckedChange = {
                                        blockHomeButton = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            // Show
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(AppStringsProvider.current().showCountdownTimer, style = MaterialTheme.typography.bodyMedium)
                                PremiumSwitch(
                                    checked = showCountdown,
                                    onCheckedChange = {
                                        showCountdown = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Note
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(AppStringsProvider.current().allowEmergencyExit, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        AppStringsProvider.current().emergencyExitHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PremiumSwitch(
                                    checked = allowEmergencyExit,
                                    onCheckedChange = {
                                        allowEmergencyExit = it
                                        updateConfig()
                                    }
                                )
                            }
                            
                            AnimatedVisibility(visible = allowEmergencyExit) {
                                PremiumTextField(
                                    value = emergencyPassword,
                                    onValueChange = {
                                        emergencyPassword = it
                                        updateConfig()
                                    },
                                    label = { Text(AppStringsProvider.current().emergencyPassword) },
                                    placeholder = { Text(AppStringsProvider.current().emergencyPasswordHint) },
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
    
    // Timeselectdialog
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
 * select
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
            AppStringsProvider.current().dayMon, AppStringsProvider.current().dayTue, AppStringsProvider.current().dayWed,
            AppStringsProvider.current().dayThu, AppStringsProvider.current().dayFri, AppStringsProvider.current().daySat, AppStringsProvider.current().daySun
        )
        dayNames.forEachIndexed { index, name ->
            val day = index + 1
            val isSelected = selectedDays.contains(day)
            PremiumFilterChip(
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
