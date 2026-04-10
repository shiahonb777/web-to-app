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
import com.webtoapp.core.i18n.Strings
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
 * 端口管理页面
 * 
 * 功能：
 * - 扫描并显示所有运行中的服务（含响应延迟、运行时长）
 * - 支持终止单个服务 / 一键终止所有服务
 * - 支持在浏览器中打开服务
 * - 端口范围使用率仪表盘
 * - 可选自动刷新（5s 间隔）
 * - 僵尸端口自动检测与清理
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
    
    // 扫描函数
    suspend fun doScan() {
        isScanning = true
        services = ProcessPortScanner.scanAllPorts(context)
        isScanning = false
    }
    
    // 初始扫描
    LaunchedEffect(Unit) { doScan() }
    
    // 自动刷新
    LaunchedEffect(autoRefresh) {
        if (autoRefresh) {
            while (true) {
                delay(5000)
                doScan()
            }
        }
    }
    
    // 刷新函数
    fun refresh() {
        scope.launch { doScan() }
    }
    
    // 终止单个服务
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
    
    // 终止所有服务
    fun killAllServices() {
        scope.launch {
            val count = ProcessPortScanner.killAllProcesses(context)
            snackbarHostState.showSnackbar(Strings.portManagerAllKilled.format(count))
            delay(300)
            doScan()
        }
    }
    
    // 在浏览器中打开
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
                    // 自动刷新开关
                    IconButton(onClick = { autoRefresh = !autoRefresh }) {
                        Icon(
                            if (autoRefresh) Icons.Default.SyncDisabled else Icons.Default.Sync,
                            Strings.portManagerAutoRefresh,
                            tint = if (autoRefresh) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 手动刷新按钮
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
                            Icon(Icons.Default.Refresh, Strings.portManagerAutoRefresh)
                        }
                    }
                    // 终止所有按钮
                    if (services.isNotEmpty()) {
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
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 统计卡片
            PortStatsCard(
                services = services,
                showRangeStats = showRangeStats,
                onToggleRangeStats = { showRangeStats = !showRangeStats }
            )
            
            // 服务列表
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
    
    // 终止所有确认对话框
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
    
    // 终止单个确认对话框
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

/**
 * 端口统计卡片（含可展开的端口范围使用率面板）
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
                    Strings.portManagerRunningServices,
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
                    // 展开端口范围统计
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
                
                // 按类型分组统计
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
            
            // 端口范围使用率仪表盘
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

/**
 * 服务类型标签
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
 * 服务卡片
 */
@Composable
private fun ServiceCard(
    service: RunningService,
    onKill: () -> Unit,
    onOpen: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // 实时更新运行时长
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
                // 左侧：类型指示器 + 端口
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(weight = 1f, fill = true)
                ) {
                    // 类型颜色指示器
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
                                "${Strings.portManagerPort} ${service.port}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            // 响应延迟标签
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
                
                // 右侧：状态指示器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 响应状态
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
                    
                    // 展开/收起图标
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
            
            // 展开详情
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 详细信息
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
                    
                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 打开按钮
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
                        
                        // 终止按钮
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

/**
 * 详情行
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
 * 空状态
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
