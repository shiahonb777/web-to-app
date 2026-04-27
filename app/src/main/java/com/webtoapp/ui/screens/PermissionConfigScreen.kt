package com.webtoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.ApkRuntimePermissions
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.SettingsSwitch
import com.webtoapp.ui.components.ThemedBackgroundBox

// ── 危险权限集合 ──
private val DANGEROUS_PERMISSION_KEYS = setOf(
    "readSms", "sendSms", "receiveSms",
    "callPhone", "processOutgoingCalls",
    "readContacts", "writeContacts",
    "readCallLog", "writeCallLog",
    "readPhoneState",
    "systemAlertWindow", "installPackages"
)

// ── 权限预设模板 ──
private data class PermissionPreset(
    val label: () -> String,
    val icon: ImageVector,
    val apply: (ApkRuntimePermissions) -> ApkRuntimePermissions,
    val match: (ApkRuntimePermissions) -> Boolean
)

private val PERMISSION_PRESETS = listOf(
    PermissionPreset(
        label = { Strings.permissionPresetNone },
        icon = Icons.Outlined.Block,
        apply = { ApkRuntimePermissions() },
        match = { it == ApkRuntimePermissions() }
    ),
    PermissionPreset(
        label = { Strings.permissionPresetMinimal },
        icon = Icons.Outlined.Language,
        apply = { ApkRuntimePermissions(notifications = true) },
        match = { it == ApkRuntimePermissions(notifications = true) }
    ),
    PermissionPreset(
        label = { Strings.permissionPresetStandard },
        icon = Icons.Outlined.Verified,
        apply = { ApkRuntimePermissions(
            camera = true, location = true, notifications = true,
            readMediaImages = true, readMediaVideo = true
        ) },
        match = { it == ApkRuntimePermissions(
            camera = true, location = true, notifications = true,
            readMediaImages = true, readMediaVideo = true
        ) }
    ),
    PermissionPreset(
        label = { Strings.permissionPresetFull },
        icon = Icons.Outlined.SelectAll,
        apply = { ApkRuntimePermissions(
            camera = true, microphone = true, location = true, notifications = true,
            readMediaImages = true, readMediaVideo = true, readMediaAudio = true,
            bluetooth = true, nfc = true, bodySensors = true
        ) },
        match = { it == ApkRuntimePermissions(
            camera = true, microphone = true, location = true, notifications = true,
            readMediaImages = true, readMediaVideo = true, readMediaAudio = true,
            bluetooth = true, nfc = true, bodySensors = true
        ) }
    )
)

/**
 * APK 权限配置独立页面
 * 从高级设置中独立出来，提供完整的权限选择功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionConfigScreen(
    permissions: ApkRuntimePermissions,
    onPermissionsChange: (ApkRuntimePermissions) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Strings.permissionConfigTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        ThemedBackgroundBox {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 说明卡片
                EnhancedElevatedCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = Strings.permissionConfigDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── 预设快捷选择 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PERMISSION_PRESETS.forEach { preset ->
                        val selected = preset.match(permissions)
                        FilterChip(
                            selected = selected,
                            onClick = { onPermissionsChange(preset.apply(permissions)) },
                            label = { Text(preset.label()) },
                            leadingIcon = {
                                Icon(
                                    preset.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // ── 冲突检测提示 ──
                val conflicts = detectConflicts(permissions)
                if (conflicts.isNotEmpty()) {
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Strings.permissionConflictTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                conflicts.forEach { conflict ->
                                    Text(
                                        text = "• $conflict",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // 已选权限计数
                val enabledCount = countEnabledPermissions(permissions)
                Surface(
                    color = if (enabledCount > 0)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (enabledCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.permissionEnabledCount.format(enabledCount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (enabledCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (enabledCount > 0) {
                            TextButton(onClick = {
                                onPermissionsChange(ApkRuntimePermissions())
                            }) {
                                Text(Strings.permissionClearAll, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // ── 搜索框 ──
                PremiumTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(Strings.permissionSearchHint) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── 基础权限 ──
                PermissionCategoryCard(
                    title = Strings.permissionCategoryBasic,
                    icon = Icons.Outlined.Security,
                    searchQuery = searchQuery
                ) {
                    PermissionSwitch(
                        key = "camera",
                        title = Strings.permissionCamera,
                        subtitle = Strings.permissionCameraDesc,
                        checked = permissions.camera,
                        onCheckedChange = { onPermissionsChange(permissions.copy(camera = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "microphone",
                        title = Strings.permissionMicrophone,
                        subtitle = Strings.permissionMicrophoneDesc,
                        checked = permissions.microphone,
                        onCheckedChange = { onPermissionsChange(permissions.copy(microphone = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "location",
                        title = Strings.permissionLocation,
                        subtitle = Strings.permissionLocationDesc,
                        checked = permissions.location,
                        onCheckedChange = { onPermissionsChange(permissions.copy(location = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "notifications",
                        title = Strings.permissionNotifications,
                        subtitle = Strings.permissionNotificationsDesc,
                        checked = permissions.notifications,
                        onCheckedChange = { onPermissionsChange(permissions.copy(notifications = it)) },
                        searchQuery = searchQuery
                    )
                }

                // ── 存储权限 ──
                PermissionCategoryCard(
                    title = Strings.permissionCategoryStorage,
                    icon = Icons.Outlined.Folder,
                    searchQuery = searchQuery
                ) {
                    PermissionSwitch(
                        key = "readExternalStorage",
                        title = Strings.permissionReadExternalStorage,
                        subtitle = Strings.permissionReadExternalStorageDesc,
                        checked = permissions.readExternalStorage,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readExternalStorage = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "writeExternalStorage",
                        title = Strings.permissionWriteExternalStorage,
                        subtitle = Strings.permissionWriteExternalStorageDesc,
                        checked = permissions.writeExternalStorage,
                        onCheckedChange = { onPermissionsChange(permissions.copy(writeExternalStorage = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "readMediaImages",
                        title = Strings.permissionReadMediaImages,
                        subtitle = Strings.permissionReadMediaImagesDesc,
                        checked = permissions.readMediaImages,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readMediaImages = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "readMediaVideo",
                        title = Strings.permissionReadMediaVideo,
                        subtitle = Strings.permissionReadMediaVideoDesc,
                        checked = permissions.readMediaVideo,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readMediaVideo = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "readMediaAudio",
                        title = Strings.permissionReadMediaAudio,
                        subtitle = Strings.permissionReadMediaAudioDesc,
                        checked = permissions.readMediaAudio,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readMediaAudio = it)) },
                        searchQuery = searchQuery
                    )
                }

                // ── 连接权限 ──
                PermissionCategoryCard(
                    title = Strings.permissionCategoryConnectivity,
                    icon = Icons.Outlined.Bluetooth,
                    searchQuery = searchQuery
                ) {
                    PermissionSwitch(
                        key = "bluetooth",
                        title = Strings.permissionBluetooth,
                        subtitle = Strings.permissionBluetoothDesc,
                        checked = permissions.bluetooth,
                        onCheckedChange = { onPermissionsChange(permissions.copy(bluetooth = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "nfc",
                        title = Strings.permissionNfc,
                        subtitle = Strings.permissionNfcDesc,
                        checked = permissions.nfc,
                        onCheckedChange = { onPermissionsChange(permissions.copy(nfc = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "wifiState",
                        title = Strings.permissionWifiState,
                        subtitle = Strings.permissionWifiStateDesc,
                        checked = permissions.wifiState,
                        onCheckedChange = { onPermissionsChange(permissions.copy(wifiState = it)) },
                        searchQuery = searchQuery
                    )
                }

                // ── 传感器权限 ──
                PermissionCategoryCard(
                    title = Strings.permissionCategorySensors,
                    icon = Icons.Outlined.DirectionsRun,
                    searchQuery = searchQuery
                ) {
                    PermissionSwitch(
                        key = "bodySensors",
                        title = Strings.permissionBodySensors,
                        subtitle = Strings.permissionBodySensorsDesc,
                        checked = permissions.bodySensors,
                        onCheckedChange = { onPermissionsChange(permissions.copy(bodySensors = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "activityRecognition",
                        title = Strings.permissionActivityRecognition,
                        subtitle = Strings.permissionActivityRecognitionDesc,
                        checked = permissions.activityRecognition,
                        onCheckedChange = { onPermissionsChange(permissions.copy(activityRecognition = it)) },
                        searchQuery = searchQuery
                    )
                }

                // ── 系统权限 ──
                PermissionCategoryCard(
                    title = Strings.permissionCategorySystem,
                    icon = Icons.Outlined.Phone,
                    searchQuery = searchQuery
                ) {
                    PermissionSwitch(
                        key = "readPhoneState",
                        title = Strings.permissionReadPhoneState,
                        subtitle = Strings.permissionReadPhoneStateDesc,
                        checked = permissions.readPhoneState,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readPhoneState = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "callPhone",
                        title = Strings.permissionCallPhone,
                        subtitle = Strings.permissionCallPhoneDesc,
                        checked = permissions.callPhone,
                        onCheckedChange = { onPermissionsChange(permissions.copy(callPhone = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "readContacts",
                        title = Strings.permissionReadContacts,
                        subtitle = Strings.permissionReadContactsDesc,
                        checked = permissions.readContacts,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readContacts = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "writeContacts",
                        title = Strings.permissionWriteContacts,
                        subtitle = Strings.permissionWriteContactsDesc,
                        checked = permissions.writeContacts,
                        onCheckedChange = { onPermissionsChange(permissions.copy(writeContacts = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "readCalendar",
                        title = Strings.permissionReadCalendar,
                        subtitle = Strings.permissionReadCalendarDesc,
                        checked = permissions.readCalendar,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readCalendar = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "writeCalendar",
                        title = Strings.permissionWriteCalendar,
                        subtitle = Strings.permissionWriteCalendarDesc,
                        checked = permissions.writeCalendar,
                        onCheckedChange = { onPermissionsChange(permissions.copy(writeCalendar = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "readSms",
                        title = Strings.permissionReadSms,
                        subtitle = Strings.permissionReadSmsDesc,
                        checked = permissions.readSms,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readSms = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "sendSms",
                        title = Strings.permissionSendSms,
                        subtitle = Strings.permissionSendSmsDesc,
                        checked = permissions.sendSms,
                        onCheckedChange = { onPermissionsChange(permissions.copy(sendSms = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "receiveSms",
                        title = Strings.permissionReceiveSms,
                        subtitle = Strings.permissionReceiveSmsDesc,
                        checked = permissions.receiveSms,
                        onCheckedChange = { onPermissionsChange(permissions.copy(receiveSms = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "readCallLog",
                        title = Strings.permissionReadCallLog,
                        subtitle = Strings.permissionReadCallLogDesc,
                        checked = permissions.readCallLog,
                        onCheckedChange = { onPermissionsChange(permissions.copy(readCallLog = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "writeCallLog",
                        title = Strings.permissionWriteCallLog,
                        subtitle = Strings.permissionWriteCallLogDesc,
                        checked = permissions.writeCallLog,
                        onCheckedChange = { onPermissionsChange(permissions.copy(writeCallLog = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "processOutgoingCalls",
                        title = Strings.permissionProcessOutgoingCalls,
                        subtitle = Strings.permissionProcessOutgoingCallsDesc,
                        checked = permissions.processOutgoingCalls,
                        onCheckedChange = { onPermissionsChange(permissions.copy(processOutgoingCalls = it)) },
                        searchQuery = searchQuery
                    )
                }

                // ── 后台/高级系统权限 ──
                PermissionCategoryCard(
                    title = Strings.permissionCategoryBackground,
                    icon = Icons.Outlined.History,
                    searchQuery = searchQuery
                ) {
                    PermissionSwitch(
                        key = "foregroundService",
                        title = Strings.permissionForegroundService,
                        subtitle = Strings.permissionForegroundServiceDesc,
                        checked = permissions.foregroundService,
                        onCheckedChange = { onPermissionsChange(permissions.copy(foregroundService = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "wakeLock",
                        title = Strings.permissionWakeLock,
                        subtitle = Strings.permissionWakeLockDesc,
                        checked = permissions.wakeLock,
                        onCheckedChange = { onPermissionsChange(permissions.copy(wakeLock = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "requestIgnoreBatteryOptimizations",
                        title = Strings.permissionRequestIgnoreBatteryOptimizations,
                        subtitle = Strings.permissionRequestIgnoreBatteryOptimizationsDesc,
                        checked = permissions.requestIgnoreBatteryOptimizations,
                        onCheckedChange = { onPermissionsChange(permissions.copy(requestIgnoreBatteryOptimizations = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "bootCompleted",
                        title = Strings.permissionBootCompleted,
                        subtitle = Strings.permissionBootCompletedDesc,
                        checked = permissions.bootCompleted,
                        onCheckedChange = { onPermissionsChange(permissions.copy(bootCompleted = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "vibration",
                        title = Strings.permissionVibration,
                        subtitle = Strings.permissionVibrationDesc,
                        checked = permissions.vibration,
                        onCheckedChange = { onPermissionsChange(permissions.copy(vibration = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "installPackages",
                        title = Strings.permissionInstallPackages,
                        subtitle = Strings.permissionInstallPackagesDesc,
                        checked = permissions.installPackages,
                        onCheckedChange = { onPermissionsChange(permissions.copy(installPackages = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "requestDeletePackages",
                        title = Strings.permissionRequestDeletePackages,
                        subtitle = Strings.permissionRequestDeletePackagesDesc,
                        checked = permissions.requestDeletePackages,
                        onCheckedChange = { onPermissionsChange(permissions.copy(requestDeletePackages = it)) },
                        searchQuery = searchQuery
                    )
                    PermissionSwitch(
                        key = "systemAlertWindow",
                        title = Strings.permissionSystemAlertWindow,
                        subtitle = Strings.permissionSystemAlertWindowDesc,
                        checked = permissions.systemAlertWindow,
                        onCheckedChange = { onPermissionsChange(permissions.copy(systemAlertWindow = it)) },
                        searchQuery = searchQuery
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PermissionCategoryCard(
    title: String,
    icon: ImageVector,
    searchQuery: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    // 当有搜索词时，始终显示分类（子项会自行过滤）
    EnhancedElevatedCard {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PermissionSwitch(
    key: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    searchQuery: String = ""
) {
    val isDangerous = key in DANGEROUS_PERMISSION_KEYS
    val matchesSearch = searchQuery.isBlank() ||
        title.contains(searchQuery, ignoreCase = true) ||
        subtitle.contains(searchQuery, ignoreCase = true)

    if (!matchesSearch) return

    Column {
        SettingsSwitch(
            title = buildString {
                append(title)
                if (isDangerous) append(" ⚠")
            },
            subtitle = if (isDangerous) "$subtitle\n${Strings.permissionDangerTag}" else subtitle,
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        if (isDangerous && checked) {
            Text(
                text = Strings.permissionDangerWarning,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )
        }
    }
}

private fun countEnabledPermissions(permissions: ApkRuntimePermissions): Int {
    return listOf(
        permissions.camera, permissions.microphone, permissions.location, permissions.notifications,
        permissions.readExternalStorage, permissions.writeExternalStorage,
        permissions.readMediaImages, permissions.readMediaVideo, permissions.readMediaAudio,
        permissions.bluetooth, permissions.nfc, permissions.wifiState,
        permissions.bodySensors, permissions.activityRecognition,
        permissions.readPhoneState, permissions.callPhone,
        permissions.readContacts, permissions.writeContacts,
        permissions.readCalendar, permissions.writeCalendar,
        permissions.readSms, permissions.sendSms, permissions.receiveSms,
        permissions.readCallLog, permissions.writeCallLog, permissions.processOutgoingCalls,
        permissions.foregroundService, permissions.wakeLock,
        permissions.requestIgnoreBatteryOptimizations, permissions.bootCompleted,
        permissions.vibration, permissions.installPackages,
        permissions.requestDeletePackages, permissions.systemAlertWindow
    ).count { it }
}

private fun detectConflicts(permissions: ApkRuntimePermissions): List<String> {
    val conflicts = mutableListOf<String>()

    // Android 13+ 媒体权限与旧存储权限重叠
    val hasMediaPerm = permissions.readMediaImages || permissions.readMediaVideo || permissions.readMediaAudio
    val hasLegacyStorage = permissions.readExternalStorage || permissions.writeExternalStorage
    if (hasMediaPerm && hasLegacyStorage) {
        conflicts.add(Strings.permissionConflictMediaVsLegacy)
    }

    // SMS 权限风险提示
    val hasSmsPerm = permissions.readSms || permissions.sendSms || permissions.receiveSms
    if (hasSmsPerm) {
        conflicts.add(Strings.permissionConflictSmsRisk)
    }

    // 通话相关权限风险
    val hasCallPerm = permissions.callPhone || permissions.processOutgoingCalls
    if (hasCallPerm) {
        conflicts.add(Strings.permissionConflictCallRisk)
    }

    return conflicts
}
