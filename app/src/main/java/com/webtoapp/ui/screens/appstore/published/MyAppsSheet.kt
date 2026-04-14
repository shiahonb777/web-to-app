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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyAppsSheet(
    apiClient: CloudApiClient,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var myApps by remember { mutableStateOf<List<AppStoreItem>>(emptyList()) }
    var quota by remember { mutableIntStateOf(0) }
    var tier by remember { mutableStateOf("free") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var appToDelete by remember { mutableStateOf<AppStoreItem?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var managedApp by remember { mutableStateOf<AppStoreItem?>(null) }

    fun loadMyApps(showRefresh: Boolean = false) {
        scope.launch {
            if (showRefresh) isRefreshing = true else isLoading = true
            errorMsg = null
            when (val result = apiClient.listMyApps()) {
                is com.webtoapp.core.auth.AuthResult.Success -> {
                    myApps = result.data.apps
                    quota = result.data.quota
                    tier = result.data.tier
                }
                is com.webtoapp.core.auth.AuthResult.Error -> {
                    errorMsg = result.message
                }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { loadMyApps() }

    val totalDownloads = remember(myApps) { myApps.sumOf { it.downloads } }
    val avgRating = remember(myApps) {
        val rated = myApps.filter { it.ratingCount > 0 }
        if (rated.isEmpty()) 0f else rated.map { it.rating }.average().toFloat()
    }
    val totalLikes = remember(myApps) { myApps.sumOf { it.likeCount } }

    val categoryLabels = mapOf(
        "tools" to Strings.catTools, "social" to Strings.catSocial, "education" to "教育",
        "entertainment" to "娱乐", "productivity" to "效率",
        "lifestyle" to "生活", "business" to "商务",
        "news" to "新闻", "finance" to "金融",
        "health" to "健康", "other" to Strings.catOther
    )

    // delete( )
    appToDelete?.let { app ->
        ItemDeleteConfirmDialog(
            itemName = app.name,
            storeName = "商店",
            actionVerb = "Download this app",
            isDeleting = isDeleting,
            onConfirm = {
                isDeleting = true
                scope.launch {
                    apiClient.deleteMyApp(app.id)
                    myApps = myApps.filterNot { it.id == app.id }
                    appToDelete = null
                    isDeleting = false
                }
            },
            onDismiss = { appToDelete = null }
        )
    }

    // management
    managedApp?.let { app ->
        AppManagementSheet(
            app = app,
            apiClient = apiClient,
            onDismiss = { managedApp = null }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            // Header( )
            MyPublishedItemsHeader(
                title = Strings.storeMyApps,
                isRefreshing = isRefreshing,
                onRefresh = { loadMyApps(showRefresh = true) }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${myApps.size} / $quota",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (tier.lowercase()) {
                        "ultra" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                        "pro" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ) {
                    Text(
                        tier.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (tier.lowercase()) {
                            "ultra" -> Color(0xFFFF9800)
                            "pro" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Note
            if (!isLoading && myApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                StatsOverviewRow(
                    totalDownloads = totalDownloads,
                    avgRating = avgRating,
                    totalLikes = totalLikes,
                    downloadLabel = Strings.storeTotalDownloads
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // contentstate( )
            if (isLoading) {
                PublishedItemLoadingState("Loading my apps...")
            } else if (errorMsg != null) {
                PublishedItemErrorState(errorMsg) { loadMyApps() }
            } else if (myApps.isEmpty()) {
                PublishedItemEmptyState(
                    icon = Icons.Outlined.RocketLaunch,
                    title = Strings.storeNoPublishedApps,
                    subtitle = "将你制作的 APP 发布到商店\n让更多人发现和使用你的作品",
                    onAction = onDismiss
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(myApps, key = { it.id }) { app ->
                        EnhancedElevatedCard(
                            onClick = { managedApp = app },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // App icon
                                    Surface(
                                        shape = RoundedCornerShape(13.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        if (!app.icon.isNullOrBlank()) {
                                            AsyncImage(
                                                model = app.icon,
                                                contentDescription = app.name,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(13.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primaryContainer,
                                                            MaterialTheme.colorScheme.tertiaryContainer
                                                        )
                                                    )
                                                ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Apps, null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                app.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            // label( )
                                            CategoryTag(
                                                label = categoryLabels[app.category] ?: app.category,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        // pills( )
                                        PublishedItemStatsPills(
                                            versionName = app.versionName,
                                            downloads = app.downloads,
                                            rating = app.rating,
                                            ratingCount = app.ratingCount,
                                            likeCount = app.likeCount
                                        )
                                    }
                                    // Delete button
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        IconButton(
                                            onClick = { appToDelete = app },
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete, null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                // Note
                                if (!app.description.isNullOrBlank()) {
                                    Text(
                                        app.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 16.sp
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



// ════════════════════════════════════════════════
// appmanagement( Premium UI)
// ════════════════════════════════════════════════

/** management gradient */
