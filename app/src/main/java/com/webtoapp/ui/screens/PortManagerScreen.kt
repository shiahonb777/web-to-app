package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.port.ProcessPortScanner
import com.webtoapp.core.port.ProcessPortScanner.RunningService
import com.webtoapp.core.port.ProcessPortScanner.ServiceType
import com.webtoapp.core.port.WtaAppPortDiscovery
import com.webtoapp.core.port.WtaAppPortDiscovery.WtaAppPortReport
import com.webtoapp.core.port.WtaAppPortDiscovery.RemoteAllocation
import com.webtoapp.util.openUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.webtoapp.ui.design.WtaBackground
import com.webtoapp.ui.components.EnhancedElevatedCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll












@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }

    var isScanning by remember { mutableStateOf(false) }
    var services by remember { mutableStateOf<List<RunningService>>(emptyList()) }
    var showKillAllDialog by remember { mutableStateOf(false) }
    var showKillDialog by remember { mutableStateOf<RunningService?>(null) }
    var autoRefresh by remember { mutableStateOf(false) }
    var showRangeStats by remember { mutableStateOf(false) }

    var isScanningWtaApps by remember { mutableStateOf(false) }
    var wtaReports by remember { mutableStateOf<List<WtaAppPortReport>>(emptyList()) }

    val nowMs by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }


    suspend fun doScan() {
        isScanning = true
        services = ProcessPortScanner.scanAllPorts(context)
        isScanning = false
    }

    suspend fun doScanWtaApps() {
        isScanningWtaApps = true
        try {
            wtaReports = WtaAppPortDiscovery.queryAllApps(context)
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("${Strings.portManagerScanFailed}: ${e.message ?: ""}")
        } finally {
            isScanningWtaApps = false
        }
    }


    LaunchedEffect(Unit) { doScan() }


    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && wtaReports.isEmpty() && !isScanningWtaApps) {
            doScanWtaApps()
        }
    }


    LaunchedEffect(autoRefresh, selectedTab) {
        if (autoRefresh) {
            while (true) {
                delay(5000)
                if (selectedTab == 0) doScan() else doScanWtaApps()
            }
        }
    }


    fun refresh() {
        scope.launch {
            if (selectedTab == 0) doScan() else doScanWtaApps()
        }
    }


    fun killService(service: RunningService) {
        scope.launch {
            val success = ProcessPortScanner.killProcess(service.port)
            if (success) {
                snackbarHostState.showSnackbar(
                    Strings.portManagerServiceKilled.format(service.port)
                )
            } else {
                snackbarHostState.showSnackbar(Strings.portManagerKillFailed)
            }
            delay(300)
            doScan()
        }
    }


    fun killAllServices() {
        scope.launch {
            val count = ProcessPortScanner.killAllProcesses(context)
            snackbarHostState.showSnackbar(Strings.portManagerAllKilled.format(count))
            delay(300)
            doScan()
        }
    }


    fun releaseRemotePort(report: WtaAppPortReport, alloc: RemoteAllocation) {
        scope.launch {
            val ok = WtaAppPortDiscovery.releaseRemotePort(
                context, report.app.packageName, alloc.port
            )
            if (ok) {
                snackbarHostState.showSnackbar(
                    Strings.portManagerReleaseRemoteSuccess
                        .format(report.app.displayName, alloc.port)
                )
                delay(300)
                doScanWtaApps()
            } else {
                snackbarHostState.showSnackbar(Strings.portManagerReleaseRemoteFailed)
            }
        }
    }


    fun openInBrowser(url: String) {
        try {
            context.openUrl(url)
        } catch (_: Exception) {
            scope.launch { snackbarHostState.showSnackbar(Strings.portManagerKillFailed) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.portManagerTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {

                    IconButton(onClick = { autoRefresh = !autoRefresh }) {
                        Icon(
                            if (autoRefresh) Icons.Default.SyncDisabled else Icons.Default.Sync,
                            Strings.portManagerAutoRefresh,
                            tint = if (autoRefresh) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { refresh() },
                        enabled = !isScanning && !isScanningWtaApps
                    ) {
                        if (isScanning || isScanningWtaApps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, Strings.portManagerAutoRefresh)
                        }
                    }

                    if (selectedTab == 0 && services.isNotEmpty()) {
                        IconButton(onClick = { showKillAllDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                Strings.portManagerKillAll,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        WtaBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(Strings.portManagerTabThisApp) },
                    icon = { Icon(Icons.Outlined.SettingsEthernet, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(Strings.portManagerTabAllApps) },
                    icon = { Icon(Icons.Outlined.Apps, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> ThisAppTabContent(
                    services = services,
                    isScanning = isScanning,
                    showRangeStats = showRangeStats,
                    onToggleRangeStats = { showRangeStats = !showRangeStats },
                    nowMs = nowMs,
                    onKillRequest = { showKillDialog = it },
                    onOpenInBrowser = ::openInBrowser
                )
                else -> AllAppsTabContent(
                    reports = wtaReports,
                    isScanning = isScanningWtaApps,
                    nowMs = nowMs,
                    onReleaseRequest = ::releaseRemotePort
                )
            }
        }
    }


    if (showKillAllDialog) {
        AlertDialog(
            onDismissRequest = { showKillAllDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(Strings.portManagerKillAll) },
            text = { Text(Strings.portManagerKillAllConfirm) },
            confirmButton = {
                PremiumButton(
                    onClick = {
                        showKillAllDialog = false
                        killAllServices()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.portManagerKillAll)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillAllDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }


    showKillDialog?.let { service ->
        AlertDialog(
            onDismissRequest = { showKillDialog = null },
            icon = { Icon(Icons.Default.Stop, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(Strings.portManagerKillService) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(Strings.portManagerKillConfirmSingle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${Strings.portManagerPort}: ${service.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "${Strings.portManagerType}: ${service.type.label}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${Strings.portManagerProject}: ${service.owner}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (service.allocatedAt > 0) {
                        Text(
                            "${Strings.portManagerUptime}: ${PortManager.formatDuration(System.currentTimeMillis() - service.allocatedAt)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                PremiumButton(
                    onClick = {
                        val s = service
                        showKillDialog = null
                        killService(s)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.portManagerKill)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillDialog = null }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
        }
}




@Composable
private fun PortStatsCard(
    services: List<RunningService>,
    showRangeStats: Boolean,
    onToggleRangeStats: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Strings.portManagerRunningServices,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${services.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (services.isNotEmpty())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = onToggleRangeStats,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            Strings.portManagerPortRanges,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))


                val grouped = services.groupBy { it.type }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (type, list) ->
                        ServiceTypeChip(type = type, count = list.size)
                    }
                }
            }


            AnimatedVisibility(visible = showRangeStats) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        Strings.portManagerPortRanges,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val rangeStats = remember(services) { PortManager.getRangeStats() }
                    rangeStats.forEach { stat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                stat.range.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(76.dp)
                            )
                            LinearProgressIndicator(
                                progress = { stat.usagePercent },
                                modifier = Modifier
                                    .weight(weight = 1f, fill = true)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when {
                                    stat.usagePercent > 0.8f -> MaterialTheme.colorScheme.error
                                    stat.usagePercent > 0.5f -> AppColors.Warning
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Text(
                                "${stat.allocated}/${stat.total}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}




@Composable
private fun ServiceTypeChip(type: ServiceType, count: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(type.color).copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(type.color))
            )
            Text(
                "${type.label}: $count",
                style = MaterialTheme.typography.labelMedium,
                color = Color(type.color)
            )
        }
    }
}




@Composable
private fun ServiceCard(
    service: RunningService,
    nowMs: Long,
    onKill: () -> Unit,
    onOpen: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val uptimeText = remember(nowMs, service.allocatedAt) {
        if (service.allocatedAt > 0) {
            PortManager.formatDuration(nowMs - service.allocatedAt)
        } else {
            ""
        }
    }

    EnhancedElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(weight = 1f, fill = true)
                ) {

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(service.type.color).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            service.type.label.take(2),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(service.type.color)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "${Strings.portManagerPort} ${service.port}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )

                            if (service.responseTimeMs >= 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when {
                                        service.responseTimeMs < 100 -> AppColors.Success.copy(alpha = 0.15f)
                                        service.responseTimeMs < 500 -> AppColors.Warning.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        "${service.responseTimeMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        color = when {
                                            service.responseTimeMs < 100 -> AppColors.Success
                                            service.responseTimeMs < 500 -> AppColors.Warning
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                service.owner,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(weight = 1f, fill = false)
                            )
                            if (uptimeText.isNotEmpty()) {
                                Text(
                                    "• $uptimeText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    val statusColor by animateColorAsState(
                        if (service.isResponding) AppColors.Success else AppColors.Warning,
                        label = "statusColor"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )


                    val rotation by animateFloatAsState(
                        if (expanded) 180f else 0f,
                        label = "rotation"
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = rotation },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))


                    DetailRow("URL", service.url)
                    DetailRow("PID", if (service.pid > 0) service.pid.toString() else Strings.portManagerUnknown)
                    DetailRow(Strings.portManagerProcess, service.processName.ifEmpty { Strings.portManagerUnknown })
                    DetailRow(Strings.portManagerStatus, if (service.isResponding) Strings.portManagerResponding else Strings.portManagerNotResponding)
                    if (service.responseTimeMs >= 0) {
                        DetailRow(Strings.portManagerLatency, "${service.responseTimeMs}ms")
                    }
                    if (uptimeText.isNotEmpty()) {
                        DetailRow(Strings.portManagerUptime, uptimeText)
                    }

                    Spacer(modifier = Modifier.height(12.dp))


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        PremiumOutlinedButton(
                            onClick = onOpen,
                            modifier = Modifier.weight(weight = 1f, fill = true),
                            enabled = service.isResponding
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.portManagerOpen)
                        }


                        PremiumButton(
                            onClick = onKill,
                            modifier = Modifier.weight(weight = 1f, fill = true),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.portManagerKill)
                        }
                    }
                }
            }
        }
    }
}




@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}




@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                Strings.portManagerNoServices,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                Strings.portManagerAllReleased,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}



// ─── Tab content: 本应用 ───────────────────────────────────────────

@Composable
private fun ColumnScope.ThisAppTabContent(
    services: List<RunningService>,
    isScanning: Boolean,
    showRangeStats: Boolean,
    onToggleRangeStats: () -> Unit,
    nowMs: Long,
    onKillRequest: (RunningService) -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    PortStatsCard(
        services = services,
        showRangeStats = showRangeStats,
        onToggleRangeStats = onToggleRangeStats
    )

    if (services.isEmpty() && !isScanning) {
        EmptyState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(services, key = { it.port }) { service ->
                ServiceCard(
                    service = service,
                    nowMs = nowMs,
                    onKill = { onKillRequest(service) },
                    onOpen = { onOpenInBrowser(service.url) }
                )
            }
        }
    }
}


// ─── Tab content: 全部 Web2App 应用 ────────────────────────────────

@Composable
private fun ColumnScope.AllAppsTabContent(
    reports: List<WtaAppPortReport>,
    isScanning: Boolean,
    nowMs: Long,
    onReleaseRequest: (WtaAppPortReport, RemoteAllocation) -> Unit
) {
    if (reports.isEmpty() && !isScanning) {
        WtaAppsEmptyState()
        return
    }

    if (reports.isEmpty() && isScanning) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports, key = { it.app.packageName }) { report ->
            WtaAppPortReportCard(
                report = report,
                nowMs = nowMs,
                onRelease = { alloc -> onReleaseRequest(report, alloc) }
            )
        }
    }
}


@Composable
private fun WtaAppPortReportCard(
    report: WtaAppPortReport,
    nowMs: Long,
    onRelease: (RemoteAllocation) -> Unit
) {
    var expanded by remember { mutableStateOf(report.allocations.isNotEmpty()) }

    EnhancedElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        text = report.app.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = report.app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val statusText = when {
                    !report.responded -> Strings.portManagerWtaAppOffline
                    report.allocations.isEmpty() -> Strings.portManagerWtaAppNoPorts
                    else -> Strings.portManagerWtaAppPortsCount.format(report.allocations.size)
                }
                val statusColor = when {
                    !report.responded -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    report.allocations.isEmpty() -> AppColors.Success
                    else -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        maxLines = 2
                    )
                }
            }

            AnimatedVisibility(visible = expanded && report.allocations.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    report.allocations.forEach { alloc ->
                        RemoteAllocationRow(
                            allocation = alloc,
                            nowMs = nowMs,
                            onRelease = { onRelease(alloc) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun RemoteAllocationRow(
    allocation: RemoteAllocation,
    nowMs: Long,
    onRelease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (allocation.alive) AppColors.Success else AppColors.Warning)
        )

        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = allocation.port.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = allocation.range,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = allocation.owner,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (allocation.allocatedAt > 0) {
                Text(
                    text = PortManager.formatDuration((nowMs - allocation.allocatedAt).coerceAtLeast(0)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        TextButton(
            onClick = onRelease,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(Strings.portManagerKill, style = MaterialTheme.typography.labelMedium)
        }
    }
}


@Composable
private fun WtaAppsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Apps,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                Strings.portManagerNoWtaApps,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                Strings.portManagerNoWtaAppsHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
