package com.webtoapp.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.AppStoreItem
import com.webtoapp.core.cloud.AppStoreListResponse
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.StoreModuleInfo
import com.webtoapp.core.cloud.ActivationCode
import com.webtoapp.core.cloud.ActivationSettings
import com.webtoapp.core.cloud.Announcement
import com.webtoapp.core.cloud.UpdateConfig
import com.webtoapp.core.cloud.AppUser
import com.webtoapp.core.cloud.GeoDistribution
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.screens.community.Avatar
import com.webtoapp.ui.screens.community.GlassDivider
import com.webtoapp.ui.screens.community.CommunityPhysics
import com.webtoapp.ui.screens.community.LikeBurstEffect
import com.webtoapp.ui.screens.community.AnimatedCounter
import com.webtoapp.ui.screens.community.StaggeredItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.extension.ExtensionManager
import org.koin.compose.koinInject

// ════════════════════════════════════════════════
// ════════════════════════════════════════════════
// downloadmanagement Bottom Sheet( unified: download + downloadapp)
// ════════════════════════════════════════════════

// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadManagerSheet(
    downloadManager: AppDownloadManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tasks by downloadManager.activeTasks.collectAsState()
    val downloadedApps by downloadManager.downloadedApps.collectAsState()

    // Module installation tracking
    val extensionManager: ExtensionManager = koinInject()
    val allUserModules by extensionManager.modules.collectAsState()
    val scope = rememberCoroutineScope()

    // Tab state: 0 = Apps, 1 = Modules
    var selectedTab by remember { mutableIntStateOf(0) }

    // Separate active (downloading/pending) from completed/failed
    val activeTasks = tasks.values.filter {
        it.status == AppDownloadManager.DownloadStatus.DOWNLOADING ||
        it.status == AppDownloadManager.DownloadStatus.PENDING
    }
    val completedTasks = tasks.values.filter {
        it.status == AppDownloadManager.DownloadStatus.COMPLETED ||
        it.status == AppDownloadManager.DownloadStatus.FAILED
    }
    val appsEmpty = activeTasks.isEmpty() && completedTasks.isEmpty() && downloadedApps.isEmpty()

    // Total storage used
    val totalStorageBytes = downloadedApps.sumOf { it.fileSize }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.85f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            // ── Header with gradient background ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            Strings.storeDownloadManager,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        // ── Segmented Tab: Apps / Modules ──
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf(
                                    Strings.marketTabApps to (activeTasks.size + downloadedApps.size),
                                    Strings.marketTabModules to allUserModules.size
                                ).forEachIndexed { index, (label, count) ->
                                    val isSelected = selectedTab == index
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.surface
                                        else Color.Transparent,
                                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedTab = index }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            if (count > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                                ) {
                                                    Text(
                                                        "$count",
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ════════════════════════════════════════
            // Tab Content
            // ════════════════════════════════════════
            when (selectedTab) {
                // ── Apps Tab ──
                0 -> {
                    if (appsEmpty) {
                        // ── Empty state ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                                    )
                                                ),
                                                RoundedCornerShape(22.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Download,
                                            null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Text(
                                    Strings.storeNoDownloadHistory,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    Strings.storeNoDownloadHistoryDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }
                        }
                    } else {
                        // ── App stats bar ──
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            if (activeTasks.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "\${activeTasks.size} " + Strings.inProgress,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (downloadedApps.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        "\${downloadedApps.size} " + Strings.storeModulesInstalled,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (totalStorageBytes > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        downloadManager.formatSize(totalStorageBytes),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            // Clear completed tasks button
                            if (completedTasks.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                    onClick = {
                                        completedTasks.forEach { downloadManager.dismissTask(it.appId) }
                                    }
                                ) {
                                    Text(
                                        Strings.storeClearHistory,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            // ── Section: Active Downloads ──
                            if (activeTasks.isNotEmpty()) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                        Text(
                                            Strings.storeDownloadingLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                "${activeTasks.size}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                items(activeTasks, key = { "active_${it.appId}" }) { task ->
                                    ActiveDownloadCard(task, downloadManager)
                                }
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                            }

                            // ── Section: Failed Downloads ──
                            val failedTasks = completedTasks.filter { it.status == AppDownloadManager.DownloadStatus.FAILED }
                            if (failedTasks.isNotEmpty()) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                        )
                                        Text(
                                            Strings.storeDownloadFailed,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                "${failedTasks.size}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                items(failedTasks, key = { "failed_${it.appId}" }) { task ->
                                    FailedDownloadCard(task, downloadManager)
                                }
                                item { Spacer(modifier = Modifier.height(8.dp)) }
                            }

                            // ── Section: Downloaded Apps ──
                            if (downloadedApps.isNotEmpty()) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                        Text(
                                            "已下载",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                "${downloadedApps.size}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }
                                items(downloadedApps, key = { "downloaded_${it.appId}" }) { app ->
                                    DownloadedAppCard(app, downloadManager)
                                }
                            }
                        }
                    }
                }

                // ── Modules Tab ──
                1 -> {
                    if (allUserModules.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                                    )
                                                ),
                                                RoundedCornerShape(22.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Extension,
                                            null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Text(
                                    Strings.storeNoModuleHistory,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    Strings.storeNoModuleHistoryDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }
                        }
                    } else {
                        // ── Module stats bar ──
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            val enabledCount = allUserModules.count { it.enabled }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "\$enabledCount " + Strings.storeModulesEnabled,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    "\${allUserModules.size} " + Strings.storeModulesInstalled,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(allUserModules, key = { it.id }) { module ->
                                EnhancedElevatedCard(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Module gradient icon
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.Transparent,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.linearGradient(
                                                            if (module.enabled) listOf(
                                                                MaterialTheme.colorScheme.primaryContainer,
                                                                MaterialTheme.colorScheme.tertiaryContainer
                                                            ) else listOf(
                                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Extension, null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (module.enabled) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                module.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (module.enabled) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Category pill
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                                ) {
                                                    Text(
                                                        module.category.getDisplayName(),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                                // Version pill
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                                ) {
                                                    Text(
                                                        "v\${module.version.name}",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                                // Status pill
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = if (module.enabled) Color(0xFF10B981).copy(alpha = 0.1f)
                                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                                ) {
                                                    Text(
                                                        if (module.enabled) Strings.storeModuleEnable else Strings.storeModuleDisable,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (module.enabled) Color(0xFF10B981)
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                        // Toggle + Delete
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (module.enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch { extensionManager.toggleModule(module.id) }
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        if (module.enabled) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                        null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = if (module.enabled) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch { extensionManager.deleteModule(module.id) }
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete, null,
                                                        modifier = Modifier.size(15.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
