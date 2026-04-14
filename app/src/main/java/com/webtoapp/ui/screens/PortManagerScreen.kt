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
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.core.port.PortManager
import com.webtoapp.core.port.ProcessPortScanner
import com.webtoapp.core.port.ProcessPortScanner.RunningService
import com.webtoapp.core.port.ProcessPortScanner.ServiceType
import com.webtoapp.util.openUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * management
 * 
 * Note
 * anddisplay runin( , run)
 * support /
 * support open
 * Note
 * optional refresh( 5s)
 * with
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isScanning by remember { mutableStateOf(false) }
    var services by remember { mutableStateOf<List<RunningService>>(emptyList()) }
    var showKillAllDialog by remember { mutableStateOf(false) }
    var showKillDialog by remember { mutableStateOf<RunningService?>(null) }
    var autoRefresh by remember { mutableStateOf(false) }
    var showRangeStats by remember { mutableStateOf(false) }
    
    // Note
    suspend fun doScan() {
        isScanning = true
        services = ProcessPortScanner.scanAllPorts(context)
        isScanning = false
    }
    
    // Note
    LaunchedEffect(Unit) { doScan() }
    
    // refresh
    LaunchedEffect(autoRefresh) {
        if (autoRefresh) {
            while (true) {
                delay(5000)
                doScan()
            }
        }
    }
    
    // refresh
    fun refresh() {
        scope.launch { doScan() }
    }
    
    // Note
    fun killService(service: RunningService) {
        scope.launch {
            val success = ProcessPortScanner.killProcess(service.port)
            if (success) {
                snackbarHostState.showSnackbar(
                    AppStringsProvider.current().portManagerServiceKilled.format(service.port)
                )
            } else {
                snackbarHostState.showSnackbar(AppStringsProvider.current().portManagerKillFailed)
            }
            delay(300)
            doScan()
        }
    }
    
    // Note
    fun killAllServices() {
        scope.launch {
            val count = ProcessPortScanner.killAllProcesses(context)
            snackbarHostState.showSnackbar(AppStringsProvider.current().portManagerAllKilled.format(count))
            delay(300)
            doScan()
        }
    }
    
    // open
    fun openInBrowser(url: String) {
        try {
            context.openUrl(url)
        } catch (_: Exception) {
            scope.launch { snackbarHostState.showSnackbar(AppStringsProvider.current().portManagerKillFailed) }
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().portManagerTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                },
                actions = {
                    // refresh
                    IconButton(onClick = { autoRefresh = !autoRefresh }) {
                        Icon(
                            if (autoRefresh) Icons.Default.SyncDisabled else Icons.Default.Sync,
                            AppStringsProvider.current().portManagerAutoRefresh,
                            tint = if (autoRefresh) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // refreshbutton
                    IconButton(
                        onClick = { refresh() },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, AppStringsProvider.current().portManagerAutoRefresh)
                        }
                    }
                    // button
                    if (services.isNotEmpty()) {
                        IconButton(onClick = { showKillAllDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                AppStringsProvider.current().portManagerKillAll,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // card
            PortStatsCard(
                services = services,
                showRangeStats = showRangeStats,
                onToggleRangeStats = { showRangeStats = !showRangeStats }
            )
            
            // list
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
                            onKill = { showKillDialog = service },
                            onOpen = { openInBrowser(service.url) }
                        )
                    }
                }
            }
        }
    }
    
    // dialog
    if (showKillAllDialog) {
        AlertDialog(
            onDismissRequest = { showKillAllDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(AppStringsProvider.current().portManagerKillAll) },
            text = { Text(AppStringsProvider.current().portManagerKillAllConfirm) },
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
                    Text(AppStringsProvider.current().portManagerKillAll)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillAllDialog = false }) {
                    Text(AppStringsProvider.current().btnCancel)
                }
            }
        )
    }
    
    // dialog
    showKillDialog?.let { service ->
        AlertDialog(
            onDismissRequest = { showKillDialog = null },
            icon = { Icon(Icons.Default.Stop, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(AppStringsProvider.current().portManagerKillService) },
            text = { 
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(AppStringsProvider.current().portManagerKillConfirmSingle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${AppStringsProvider.current().portManagerPort}: ${service.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "${AppStringsProvider.current().portManagerType}: ${service.type.label}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${AppStringsProvider.current().portManagerProject}: ${service.owner}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (service.allocatedAt > 0) {
                        Text(
                            "${AppStringsProvider.current().portManagerUptime}: ${PortManager.formatDuration(System.currentTimeMillis() - service.allocatedAt)}",
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
                    Text(AppStringsProvider.current().portManagerKill)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillDialog = null }) {
                    Text(AppStringsProvider.current().btnCancel)
                }
            }
        )
    }
        }
}

/**
 * card( expand panel)
 */
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
                    AppStringsProvider.current().portManagerRunningServices,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${services.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (services.isNotEmpty()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // expand
                    IconButton(
                        onClick = onToggleRangeStats,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            AppStringsProvider.current().portManagerPortRanges,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // type
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
            
            // Note
            AnimatedVisibility(visible = showRangeStats) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        AppStringsProvider.current().portManagerPortRanges,
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

/**
 * typelabel
 */
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

/**
 * card
 */
@Composable
private fun ServiceCard(
    service: RunningService,
    onKill: () -> Unit,
    onOpen: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // updaterun
    var uptimeText by remember { mutableStateOf("") }
    LaunchedEffect(service.allocatedAt) {
        if (service.allocatedAt > 0) {
            while (true) {
                uptimeText = PortManager.formatDuration(System.currentTimeMillis() - service.allocatedAt)
                delay(1000)
            }
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
                // left: typeindicator +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(weight = 1f, fill = true)
                ) {
                    // typecolorindicator
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
                            fontWeight = FontWeight.Bold,
                            color = Color(service.type.color)
                        )
                    }
                    
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "${AppStringsProvider.current().portManagerPort} ${service.port}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            // label
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
                
                // right: stateindicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // state
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
                    
                    // expand/ icon
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
            
            // expand
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Note
                    DetailRow("URL", service.url)
                    DetailRow("PID", if (service.pid > 0) service.pid.toString() else AppStringsProvider.current().portManagerUnknown)
                    DetailRow(AppStringsProvider.current().portManagerProcess, service.processName.ifEmpty { AppStringsProvider.current().portManagerUnknown })
                    DetailRow(AppStringsProvider.current().portManagerStatus, if (service.isResponding) AppStringsProvider.current().portManagerResponding else AppStringsProvider.current().portManagerNotResponding)
                    if (service.responseTimeMs >= 0) {
                        DetailRow(AppStringsProvider.current().portManagerLatency, "${service.responseTimeMs}ms")
                    }
                    if (uptimeText.isNotEmpty()) {
                        DetailRow(AppStringsProvider.current().portManagerUptime, uptimeText)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // openbutton
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
                            Text(AppStringsProvider.current().portManagerOpen)
                        }
                        
                        // button
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
                            Text(AppStringsProvider.current().portManagerKill)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Note
 */
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

/**
 * state
 */
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
                AppStringsProvider.current().portManagerNoServices,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                AppStringsProvider.current().portManagerAllReleased,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
