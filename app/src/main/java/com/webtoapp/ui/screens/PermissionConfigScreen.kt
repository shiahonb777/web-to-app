package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.ApkRuntimePermissions
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.SettingsSwitch
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.util.PermissionPresetStorage
import com.webtoapp.util.SavedPermissionPreset


private val DANGEROUS_PERMISSION_KEYS = setOf(
    "readSms", "sendSms", "receiveSms",
    "callPhone", "processOutgoingCalls",
    "readContacts", "writeContacts",
    "readCallLog", "writeCallLog",
    "readPhoneState",
    "systemAlertWindow", "installPackages"
)


private data class PermissionPreset(
    val label: () -> String,
    val icon: ImageVector,
    val apply: (ApkRuntimePermissions) -> ApkRuntimePermissions,
    val match: (ApkRuntimePermissions) -> Boolean
)

private data class PermissionItem(
    val key: String,
    val title: () -> String,
    val subtitle: () -> String,
    val checked: (ApkRuntimePermissions) -> Boolean,
    val update: (ApkRuntimePermissions, Boolean) -> ApkRuntimePermissions
)

private data class PermissionCategorySpec(
    val title: () -> String,
    val icon: ImageVector,
    val items: List<PermissionItem>
)

private fun defaultExpandedCategories(): Set<String> = setOf(
    Strings.permissionCategoryBasic,
    Strings.permissionCategoryStorage
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

private val PERMISSION_CATEGORIES = listOf(
    PermissionCategorySpec(
        title = { Strings.permissionCategoryBasic },
        icon = Icons.Outlined.Security,
        items = listOf(
            PermissionItem("camera", { Strings.permissionCamera }, { Strings.permissionCameraDesc }, { it.camera }) { p, v -> p.copy(camera = v) },
            PermissionItem("microphone", { Strings.permissionMicrophone }, { Strings.permissionMicrophoneDesc }, { it.microphone }) { p, v -> p.copy(microphone = v) },
            PermissionItem("location", { Strings.permissionLocation }, { Strings.permissionLocationDesc }, { it.location }) { p, v -> p.copy(location = v) },
            PermissionItem("notifications", { Strings.permissionNotifications }, { Strings.permissionNotificationsDesc }, { it.notifications }) { p, v -> p.copy(notifications = v) }
        )
    ),
    PermissionCategorySpec(
        title = { Strings.permissionCategoryStorage },
        icon = Icons.Outlined.Folder,
        items = listOf(
            PermissionItem("readExternalStorage", { Strings.permissionReadExternalStorage }, { Strings.permissionReadExternalStorageDesc }, { it.readExternalStorage }) { p, v -> p.copy(readExternalStorage = v) },
            PermissionItem("writeExternalStorage", { Strings.permissionWriteExternalStorage }, { Strings.permissionWriteExternalStorageDesc }, { it.writeExternalStorage }) { p, v -> p.copy(writeExternalStorage = v) },
            PermissionItem("readMediaImages", { Strings.permissionReadMediaImages }, { Strings.permissionReadMediaImagesDesc }, { it.readMediaImages }) { p, v -> p.copy(readMediaImages = v) },
            PermissionItem("readMediaVideo", { Strings.permissionReadMediaVideo }, { Strings.permissionReadMediaVideoDesc }, { it.readMediaVideo }) { p, v -> p.copy(readMediaVideo = v) },
            PermissionItem("readMediaAudio", { Strings.permissionReadMediaAudio }, { Strings.permissionReadMediaAudioDesc }, { it.readMediaAudio }) { p, v -> p.copy(readMediaAudio = v) }
        )
    ),
    PermissionCategorySpec(
        title = { Strings.permissionCategoryConnectivity },
        icon = Icons.Outlined.Bluetooth,
        items = listOf(
            PermissionItem("bluetooth", { Strings.permissionBluetooth }, { Strings.permissionBluetoothDesc }, { it.bluetooth }) { p, v -> p.copy(bluetooth = v) },
            PermissionItem("nfc", { Strings.permissionNfc }, { Strings.permissionNfcDesc }, { it.nfc }) { p, v -> p.copy(nfc = v) },
            PermissionItem("wifiState", { Strings.permissionWifiState }, { Strings.permissionWifiStateDesc }, { it.wifiState }) { p, v -> p.copy(wifiState = v) }
        )
    ),
    PermissionCategorySpec(
        title = { Strings.permissionCategorySensors },
        icon = Icons.AutoMirrored.Outlined.DirectionsRun,
        items = listOf(
            PermissionItem("bodySensors", { Strings.permissionBodySensors }, { Strings.permissionBodySensorsDesc }, { it.bodySensors }) { p, v -> p.copy(bodySensors = v) },
            PermissionItem("activityRecognition", { Strings.permissionActivityRecognition }, { Strings.permissionActivityRecognitionDesc }, { it.activityRecognition }) { p, v -> p.copy(activityRecognition = v) }
        )
    ),
    PermissionCategorySpec(
        title = { Strings.permissionCategorySystem },
        icon = Icons.Outlined.Phone,
        items = listOf(
            PermissionItem("readPhoneState", { Strings.permissionReadPhoneState }, { Strings.permissionReadPhoneStateDesc }, { it.readPhoneState }) { p, v -> p.copy(readPhoneState = v) },
            PermissionItem("callPhone", { Strings.permissionCallPhone }, { Strings.permissionCallPhoneDesc }, { it.callPhone }) { p, v -> p.copy(callPhone = v) },
            PermissionItem("readContacts", { Strings.permissionReadContacts }, { Strings.permissionReadContactsDesc }, { it.readContacts }) { p, v -> p.copy(readContacts = v) },
            PermissionItem("writeContacts", { Strings.permissionWriteContacts }, { Strings.permissionWriteContactsDesc }, { it.writeContacts }) { p, v -> p.copy(writeContacts = v) },
            PermissionItem("readCalendar", { Strings.permissionReadCalendar }, { Strings.permissionReadCalendarDesc }, { it.readCalendar }) { p, v -> p.copy(readCalendar = v) },
            PermissionItem("writeCalendar", { Strings.permissionWriteCalendar }, { Strings.permissionWriteCalendarDesc }, { it.writeCalendar }) { p, v -> p.copy(writeCalendar = v) },
            PermissionItem("readSms", { Strings.permissionReadSms }, { Strings.permissionReadSmsDesc }, { it.readSms }) { p, v -> p.copy(readSms = v) },
            PermissionItem("sendSms", { Strings.permissionSendSms }, { Strings.permissionSendSmsDesc }, { it.sendSms }) { p, v -> p.copy(sendSms = v) },
            PermissionItem("receiveSms", { Strings.permissionReceiveSms }, { Strings.permissionReceiveSmsDesc }, { it.receiveSms }) { p, v -> p.copy(receiveSms = v) },
            PermissionItem("readCallLog", { Strings.permissionReadCallLog }, { Strings.permissionReadCallLogDesc }, { it.readCallLog }) { p, v -> p.copy(readCallLog = v) },
            PermissionItem("writeCallLog", { Strings.permissionWriteCallLog }, { Strings.permissionWriteCallLogDesc }, { it.writeCallLog }) { p, v -> p.copy(writeCallLog = v) },
            PermissionItem("processOutgoingCalls", { Strings.permissionProcessOutgoingCalls }, { Strings.permissionProcessOutgoingCallsDesc }, { it.processOutgoingCalls }) { p, v -> p.copy(processOutgoingCalls = v) }
        )
    ),
    PermissionCategorySpec(
        title = { Strings.permissionCategoryBackground },
        icon = Icons.Outlined.History,
        items = listOf(
            PermissionItem("foregroundService", { Strings.permissionForegroundService }, { Strings.permissionForegroundServiceDesc }, { it.foregroundService }) { p, v -> p.copy(foregroundService = v) },
            PermissionItem("wakeLock", { Strings.permissionWakeLock }, { Strings.permissionWakeLockDesc }, { it.wakeLock }) { p, v -> p.copy(wakeLock = v) },
            PermissionItem("requestIgnoreBatteryOptimizations", { Strings.permissionRequestIgnoreBatteryOptimizations }, { Strings.permissionRequestIgnoreBatteryOptimizationsDesc }, { it.requestIgnoreBatteryOptimizations }) { p, v -> p.copy(requestIgnoreBatteryOptimizations = v) },
            PermissionItem("bootCompleted", { Strings.permissionBootCompleted }, { Strings.permissionBootCompletedDesc }, { it.bootCompleted }) { p, v -> p.copy(bootCompleted = v) },
            PermissionItem("vibration", { Strings.permissionVibration }, { Strings.permissionVibrationDesc }, { it.vibration }) { p, v -> p.copy(vibration = v) },
            PermissionItem("installPackages", { Strings.permissionInstallPackages }, { Strings.permissionInstallPackagesDesc }, { it.installPackages }) { p, v -> p.copy(installPackages = v) },
            PermissionItem("requestDeletePackages", { Strings.permissionRequestDeletePackages }, { Strings.permissionRequestDeletePackagesDesc }, { it.requestDeletePackages }) { p, v -> p.copy(requestDeletePackages = v) },
            PermissionItem("systemAlertWindow", { Strings.permissionSystemAlertWindow }, { Strings.permissionSystemAlertWindowDesc }, { it.systemAlertWindow }) { p, v -> p.copy(systemAlertWindow = v) }
        )
    )
)





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionConfigScreen(
    permissions: ApkRuntimePermissions,
    onPermissionsChange: (ApkRuntimePermissions) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                PermissionConfigPanel(
                    permissions = permissions,
                    onPermissionsChange = onPermissionsChange
                )
            }
        }
    }
}

@Composable
fun PermissionConfigPanel(
    permissions: ApkRuntimePermissions,
    onPermissionsChange: (ApkRuntimePermissions) -> Unit,
    modifier: Modifier = Modifier,
    showDescription: Boolean = true
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var schemeName by remember { mutableStateOf("") }
    var savedPresets by remember { mutableStateOf(PermissionPresetStorage.load(context)) }
    var expandedCategories by remember { mutableStateOf(defaultExpandedCategories()) }
    val enabledCount = countEnabledPermissions(permissions)
    val conflicts = detectConflicts(permissions)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showDescription) {
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
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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

        PermissionSchemeCard(
            savedPresets = savedPresets,
            schemeName = schemeName,
            onSchemeNameChange = { schemeName = it },
            onSaveScheme = {
                savedPresets = PermissionPresetStorage.save(context, schemeName, permissions)
                schemeName = ""
            },
            onApplyScheme = { onPermissionsChange(it.permissions) },
            onDeleteScheme = {
                savedPresets = PermissionPresetStorage.delete(context, it.id)
            }
        )

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
                    TextButton(onClick = { onPermissionsChange(ApkRuntimePermissions()) }) {
                        Text(Strings.permissionClearAll, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        PremiumTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(Strings.permissionSearchHint) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        PERMISSION_CATEGORIES.forEach { category ->
            val visibleItems = category.items.filter { it.matches(searchQuery) }
            if (visibleItems.isNotEmpty()) {
                val title = category.title()
                val isExpanded = searchQuery.isNotBlank() || title in expandedCategories
                PermissionCategoryCard(
                    title = title,
                    icon = category.icon,
                    enabledCount = visibleItems.count { it.checked(permissions) },
                    totalCount = visibleItems.size,
                    expanded = isExpanded,
                    onExpandedChange = { expanded ->
                        expandedCategories = if (expanded) {
                            expandedCategories + title
                        } else {
                            expandedCategories - title
                        }
                    }
                ) {
                    visibleItems.forEach { item ->
                        PermissionSwitch(
                            key = item.key,
                            title = item.title(),
                            subtitle = item.subtitle(),
                            checked = item.checked(permissions),
                            onCheckedChange = { checked ->
                                onPermissionsChange(item.update(permissions, checked))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionSummaryCard(
    permissions: ApkRuntimePermissions,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val enabledCount = countEnabledPermissions(permissions)
    val dangerousEnabledCount = countDangerousEnabledPermissions(permissions)
    val summary = if (enabledCount == 0) {
        Strings.notEnabled
    } else {
        buildList {
            add(Strings.permissionEnabledCount.format(enabledCount))
            if (dangerousEnabledCount > 0) {
                add("Sensitive $dangerousEnabledCount")
            }
        }.joinToString(" · ")
    }

    WtaSettingCard(modifier = modifier) {
        WtaSettingRow(
            title = Strings.permissionConfigButton,
            subtitle = summary,
            icon = Icons.Outlined.AdminPanelSettings,
            onClick = onClick
        ) {
            if (enabledCount > 0) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = enabledCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = Strings.details,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (enabledCount > 0) {
            WtaSectionDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionSummaryPill(
                    label = Strings.configured,
                    value = enabledCount.toString(),
                    tint = MaterialTheme.colorScheme.primary
                )
                if (dangerousEnabledCount > 0) {
                    PermissionSummaryPill(
                        label = "Sensitive",
                        value = dangerousEnabledCount.toString(),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionSchemeCard(
    savedPresets: List<SavedPermissionPreset>,
    schemeName: String,
    onSchemeNameChange: (String) -> Unit,
    onSaveScheme: () -> Unit,
    onApplyScheme: (SavedPermissionPreset) -> Unit,
    onDeleteScheme: (SavedPermissionPreset) -> Unit
) {
    EnhancedElevatedCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Bookmarks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Strings.saveAsSchemeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumTextField(
                    value = schemeName,
                    onValueChange = onSchemeNameChange,
                    label = { Text(Strings.schemeName) },
                    placeholder = { Text(Strings.inputSchemeName) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                PremiumButton(
                    onClick = onSaveScheme,
                    enabled = schemeName.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(Strings.save)
                }
            }

            if (savedPresets.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    savedPresets.forEach { preset ->
                        ElevatedFilterChip(
                            selected = false,
                            onClick = { onApplyScheme(preset) },
                            label = { Text(preset.name) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onDeleteScheme(preset) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = Strings.btnDelete,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun PermissionItem.matches(searchQuery: String): Boolean {
    return searchQuery.isBlank() ||
        title().contains(searchQuery, ignoreCase = true) ||
        subtitle().contains(searchQuery, ignoreCase = true) ||
        key.contains(searchQuery, ignoreCase = true)
}

@Composable
private fun PermissionCategoryCard(
    title: String,
    icon: ImageVector,
    enabledCount: Int,
    totalCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    EnhancedElevatedCard {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) }
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
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$enabledCount/$totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabledCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        content = content
                    )
                }
            }
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

private fun countDangerousEnabledPermissions(permissions: ApkRuntimePermissions): Int {
    return listOf(
        permissions.readSms,
        permissions.sendSms,
        permissions.receiveSms,
        permissions.callPhone,
        permissions.processOutgoingCalls,
        permissions.readContacts,
        permissions.writeContacts,
        permissions.readCallLog,
        permissions.writeCallLog,
        permissions.readPhoneState,
        permissions.systemAlertWindow,
        permissions.installPackages
    ).count { it }
}

@Composable
private fun PermissionSummaryPill(
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}

private fun detectConflicts(permissions: ApkRuntimePermissions): List<String> {
    val conflicts = mutableListOf<String>()


    val hasMediaPerm = permissions.readMediaImages || permissions.readMediaVideo || permissions.readMediaAudio
    val hasLegacyStorage = permissions.readExternalStorage || permissions.writeExternalStorage
    if (hasMediaPerm && hasLegacyStorage) {
        conflicts.add(Strings.permissionConflictMediaVsLegacy)
    }


    val hasSmsPerm = permissions.readSms || permissions.sendSms || permissions.receiveSms
    if (hasSmsPerm) {
        conflicts.add(Strings.permissionConflictSmsRisk)
    }


    val hasCallPerm = permissions.callPhone || permissions.processOutgoingCalls
    if (hasCallPerm) {
        conflicts.add(Strings.permissionConflictCallRisk)
    }

    return conflicts
}
