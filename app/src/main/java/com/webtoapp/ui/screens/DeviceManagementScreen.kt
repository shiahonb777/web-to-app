package com.webtoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.DeviceInfo
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    cloudViewModel: CloudViewModel,
    onBack: () -> Unit
) {
    val devices by cloudViewModel.devices.collectAsStateWithLifecycle()
    val loading by cloudViewModel.devicesLoading.collectAsStateWithLifecycle()
    val message by cloudViewModel.message.collectAsStateWithLifecycle()

    var showRemoveDialog by remember { mutableStateOf<DeviceInfo?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { cloudViewModel.loadDevices() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            cloudViewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.cloudDeviceManagement) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { cloudViewModel.loadDevices() }) {
                        Icon(Icons.Outlined.Refresh, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        when {
            loading && devices.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }
            devices.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.DevicesOther, null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            Strings.cloudNoDevices,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Note
                    item {
                        Text(
                            text = "${Strings.cloudDeviceCount} · ${devices.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    items(devices) { device ->
                        DeviceRow(
                            device = device,
                            onRemove = { showRemoveDialog = device }
                        )
                        if (device != devices.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        showRemoveDialog?.let { device ->
            AlertDialog(
                onDismissRequest = { showRemoveDialog = null },
                title = { Text(Strings.cloudRemoveDevice) },
                text = {
                    Text("${Strings.cloudRemoveDeviceConfirm}\n\n${device.deviceName} · ${device.deviceOs}")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            cloudViewModel.removeDevice(device.id)
                            showRemoveDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text(Strings.cloudRemoveDevice) }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = null }) {
                        Text(Strings.cancel)
                    }
                }
            )
        }
    }
        }
}

@Composable
private fun DeviceRow(
    device: DeviceInfo,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (device.deviceOs.contains("Android", true))
                Icons.Outlined.PhoneAndroid else Icons.Outlined.Laptop,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = device.deviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (device.isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.cloudCurrentDevice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = buildString {
                    append(device.deviceOs)
                    device.appVersion?.let { append(" · v$it") }
                    device.lastActiveAt?.let { append(" · ${it.take(10)}") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!device.isCurrent) {
            TextButton(
                onClick = onRemove,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    Strings.cloudRemoveDevice,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
