package com.webtoapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.AppColors
import com.webtoapp.core.stats.*
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * 使用统计仪表盘页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    apps: List<WebApp>,
    allStats: List<AppUsageStats>,
    healthRecords: List<AppHealthRecord>,
    overallStats: OverallStats,
    onBack: () -> Unit,
    onCheckHealth: (WebApp) -> Unit = {},
    onCheckAllHealth: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(Strings.statsTitle, Strings.healthTitle)
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.statsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = onCheckAllHealth) {
                            Icon(Icons.Outlined.Refresh, contentDescription = Strings.healthCheckNow)
                        }
                    }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab 切换
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                            icon = {
                                Icon(
                                    if (index == 0) Icons.Outlined.BarChart else Icons.Outlined.MonitorHeart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
                
                when (selectedTab) {
                    0 -> UsageStatsTab(apps, allStats, overallStats)
                    1 -> HealthMonitorTab(apps, healthRecords, onCheckHealth)
                }
            }
        }
    }
}

/**
 * 使用统计 Tab
 */
@Composable
private fun UsageStatsTab(
    apps: List<WebApp>,
    allStats: List<AppUsageStats>,
    overallStats: OverallStats
) {
    val sortedByLaunches = remember(allStats) {
        allStats.filter { it.launchCount > 0 }.sortedByDescending { it.launchCount }
    }
    val sortedByTime = remember(allStats) {
        allStats.filter { it.totalUsageMs > 0 }.sortedByDescending { it.totalUsageMs }
    }
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 汇总卡片
        item {
            OverallStatsCard(overallStats)
        }
        
        // 最常使用排行
        item {
            Text(
                Strings.statsMostUsed,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        if (sortedByLaunches.isEmpty()) {
            item {
                EmptyStatsCard()
            }
        } else {
            items(sortedByLaunches.take(10), key = { "launches_${it.appId}" }) { stats ->
                val app = apps.find { it.id == stats.appId }
                if (app != null) {
                    UsageStatsCard(
                        app = app,
                        stats = stats,
                        rank = sortedByLaunches.indexOf(stats) + 1
                    )
                }
            }
        }
        
        // 使用时间排行
        item {
            Text(
                Strings.statsMostTime,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        if (sortedByTime.isEmpty()) {
            item {
                EmptyStatsCard()
            }
        } else {
            items(sortedByTime.take(10), key = { "time_${it.appId}" }) { stats ->
                val app = apps.find { it.id == stats.appId }
                if (app != null) {
                    UsageTimeCard(
                        app = app,
                        stats = stats,
                        maxMs = sortedByTime.first().totalUsageMs
                    )
                }
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/**
 * 汇总统计卡片
 */
@Composable
private fun OverallStatsCard(stats: OverallStats) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Outlined.TouchApp,
                value = stats.totalLaunchCount.toString(),
                label = Strings.statsTotalLaunches,
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                icon = Icons.Outlined.Timer,
                value = stats.formattedTotalUsage,
                label = Strings.statsTotalUsage,
                color = MaterialTheme.colorScheme.tertiary
            )
            StatItem(
                icon = Icons.Outlined.Apps,
                value = stats.activeAppCount.toString(),
                label = Strings.statsActiveApps,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                icon, null,
                modifier = Modifier.padding(12.dp),
                tint = color
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 使用统计卡片（按启动次数）
 */
@Composable
private fun UsageStatsCard(
    app: WebApp,
    stats: AppUsageStats,
    rank: Int
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Surface(
                shape = CircleShape,
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // App 图标
            AppIconSmall(app)
            
            Spacer(Modifier.width(12.dp))
            
            // 信息
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    app.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stats.formattedLastUsed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 启动次数
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${stats.launchCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    Strings.statsLaunches,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 使用时间卡片（带进度条）
 */
@Composable
private fun UsageTimeCard(
    app: WebApp,
    stats: AppUsageStats,
    maxMs: Long
) {
    val fraction = if (maxMs > 0) (stats.totalUsageMs.toFloat() / maxMs).coerceIn(0f, 1f) else 0f
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconSmall(app)
                Spacer(Modifier.width(12.dp))
                Text(
                    app.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stats.formattedTotalUsage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * 健康监控 Tab
 */
@Composable
private fun HealthMonitorTab(
    apps: List<WebApp>,
    healthRecords: List<AppHealthRecord>,
    onCheckHealth: (WebApp) -> Unit
) {
    val recordMap = remember(healthRecords) { healthRecords.associateBy { it.appId } }
    val webApps = remember(apps) { apps.filter { it.appType == AppType.WEB && it.url.startsWith("http") } }
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 健康概览
        item {
            HealthOverviewCard(webApps, recordMap)
        }
        
        // 每个 WEB 应用的健康状态
        items(webApps) { app ->
            val record = recordMap[app.id]
            HealthStatusCard(app, record, onCheckHealth)
        }
        
        if (webApps.isEmpty()) {
            item {
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            Strings.statsNoData,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/**
 * 健康概览卡片
 */
@Composable
private fun HealthOverviewCard(
    apps: List<WebApp>,
    recordMap: Map<Long, AppHealthRecord>
) {
    val online = apps.count { recordMap[it.id]?.status == HealthStatus.ONLINE }
    val slow = apps.count { recordMap[it.id]?.status == HealthStatus.SLOW }
    val offline = apps.count { recordMap[it.id]?.status == HealthStatus.OFFLINE }
    val unknown = apps.size - online - slow - offline
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HealthStatItem(online, Strings.healthOnline, AppColors.Success)
            HealthStatItem(slow, Strings.healthSlow, Color(0xFFFFC107))
            HealthStatItem(offline, Strings.healthOffline, Color(0xFFF44336))
            HealthStatItem(unknown, Strings.healthUnknown, Color(0xFF9E9E9E))
        }
    }
}

@Composable
private fun HealthStatItem(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 单个应用健康状态卡片
 */
@Composable
private fun HealthStatusCard(
    app: WebApp,
    record: AppHealthRecord?,
    onCheckHealth: (WebApp) -> Unit
) {
    val statusColor = when (record?.status) {
        HealthStatus.ONLINE -> AppColors.Success
        HealthStatus.SLOW -> Color(0xFFFFC107)
        HealthStatus.OFFLINE -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    val statusText = when (record?.status) {
        HealthStatus.ONLINE -> Strings.healthOnline
        HealthStatus.SLOW -> Strings.healthSlow
        HealthStatus.OFFLINE -> Strings.healthOffline
        else -> Strings.healthUnknown
    }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 状态指示灯
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                
                Spacer(Modifier.width(10.dp))
                
                AppIconSmall(app)
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        app.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        app.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 状态标签
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 详细信息
            if (record != null && record.status != HealthStatus.UNKNOWN) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (record.responseTimeMs > 0) {
                        Text(
                            "${Strings.healthResponseTime}: ${record.responseTimeMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (record.httpStatusCode > 0) {
                        Text(
                            "HTTP ${record.httpStatusCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (record.errorMessage != null) {
                    Text(
                        record.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 空统计状态
 */
@Composable
private fun EmptyStatsCard() {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.BarChart,
                    null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    Strings.statsNoData,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 小型 App 图标
 */
@Composable
private fun AppIconSmall(app: WebApp) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (app.iconPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(app.iconPath)
                    .crossfade(true)
                    .build(),
                contentDescription = app.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            val defaultIconRes = when (app.appType) {
                AppType.WEB -> R.drawable.ic_type_web
                AppType.IMAGE -> R.drawable.ic_type_media
                AppType.VIDEO -> R.drawable.ic_type_media
                AppType.HTML -> R.drawable.ic_type_html
                AppType.GALLERY -> R.drawable.ic_type_gallery
                AppType.FRONTEND -> R.drawable.ic_type_frontend
                AppType.WORDPRESS -> R.drawable.ic_type_wordpress
                AppType.NODEJS_APP -> R.drawable.ic_type_nodejs
                AppType.PHP_APP -> R.drawable.ic_type_php
                AppType.PYTHON_APP -> R.drawable.ic_type_python
                AppType.GO_APP -> R.drawable.ic_type_go
                AppType.MULTI_WEB -> R.drawable.ic_type_web
            }
            Icon(
                painterResource(defaultIconRes), null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
