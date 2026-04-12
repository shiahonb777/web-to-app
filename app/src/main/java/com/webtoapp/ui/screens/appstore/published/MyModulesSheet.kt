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

// ════════════════════════════════════════════════
// 我的模块 Bottom Sheet
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyModulesSheet(
    apiClient: CloudApiClient,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var myModules by remember { mutableStateOf<List<StoreModuleInfo>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var moduleToDelete by remember { mutableStateOf<StoreModuleInfo?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun loadMyModules(showRefresh: Boolean = false) {
        scope.launch {
            if (showRefresh) isRefreshing = true else isLoading = true
            errorMsg = null
            when (val result = apiClient.getMyPublishedModules()) {
                is com.webtoapp.core.auth.AuthResult.Success -> {
                    myModules = result.data.first
                    totalCount = result.data.second
                }
                is com.webtoapp.core.auth.AuthResult.Error -> {
                    errorMsg = result.message
                }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { loadMyModules() }

    val totalDownloads = remember(myModules) { myModules.sumOf { it.downloads } }
    val avgRating = remember(myModules) {
        val rated = myModules.filter { it.ratingCount > 0 }
        if (rated.isEmpty()) 0f else rated.map { it.rating }.average().toFloat()
    }
    val totalLikes = remember(myModules) { myModules.sumOf { it.likeCount } }

    val moduleCategoryLabels = mapOf(
        "UI_ENHANCE" to Strings.catUiEnhance, "MEDIA" to Strings.catMedia,
        "PRIVACY" to Strings.catPrivacySecurity, "TOOLS" to Strings.catTools,
        "AD_BLOCK" to Strings.catAdBlock, "SOCIAL" to Strings.catSocial,
        "DEVELOPER" to Strings.catDeveloper, "OTHER" to Strings.catOther
    )

    val moduleCategoryColors = mapOf(
        "UI_ENHANCE" to Color(0xFF6366F1),
        "MEDIA" to Color(0xFFEC4899),
        "PRIVACY" to Color(0xFF10B981),
        "TOOLS" to Color(0xFF3B82F6),
        "AD_BLOCK" to Color(0xFFEF4444),
        "SOCIAL" to Color(0xFF8B5CF6),
        "DEVELOPER" to Color(0xFFF59E0B),
        "OTHER" to Color(0xFF6B7280)
    )

    // ── 删除确认（共享组件） ──
    moduleToDelete?.let { module ->
        ItemDeleteConfirmDialog(
            itemName = module.name,
            storeName = "市场",
            actionVerb = "安装此模块",
            isDeleting = isDeleting,
            onConfirm = {
                isDeleting = true
                scope.launch {
                    apiClient.deleteMyModule(module.id)
                    myModules = myModules.filterNot { it.id == module.id }
                    moduleToDelete = null
                    isDeleting = false
                }
            },
            onDismiss = { moduleToDelete = null }
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
            // ── Header（共享组件） ──
            MyPublishedItemsHeader(
                title = Strings.storeMyModules,
                isRefreshing = isRefreshing,
                onRefresh = { loadMyModules(showRefresh = true) }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${myModules.size} 个模块",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── 统计概览（共享组件） ──
            if (!isLoading && myModules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                StatsOverviewRow(
                    totalDownloads = totalDownloads,
                    avgRating = avgRating,
                    totalLikes = totalLikes,
                    downloadLabel = "总安装"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 内容状态（共享组件） ──
            if (isLoading) {
                PublishedItemLoadingState("加载我的模块...")
            } else if (errorMsg != null) {
                PublishedItemErrorState(errorMsg) { loadMyModules() }
            } else if (myModules.isEmpty()) {
                PublishedItemEmptyState(
                    icon = Icons.Outlined.Extension,
                    title = Strings.storeNoPublishedModules,
                    subtitle = "将你开发的扩展模块发布到市场\n让更多用户体验你的创意",
                    onAction = onDismiss
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(myModules, key = { it.id }) { module ->
                        val catColor = moduleCategoryColors[module.category] ?: Color(0xFF6B7280)
                        EnhancedElevatedCard(
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
                                    // Module icon (category colored)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            catColor.copy(alpha = 0.25f),
                                                            catColor.copy(alpha = 0.10f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.Extension, null,
                                                modifier = Modifier.size(22.dp),
                                                tint = catColor
                                            )
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
                                                module.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            // 分类标签（共享组件）
                                            module.category?.let { cat ->
                                                CategoryTag(
                                                    label = moduleCategoryLabels[cat] ?: cat,
                                                    color = catColor
                                                )
                                            }
                                        }
                                        // 信息 pills（共享组件）
                                        PublishedItemStatsPills(
                                            versionName = module.versionName,
                                            downloads = module.downloads,
                                            rating = module.rating,
                                            ratingCount = module.ratingCount,
                                            likeCount = module.likeCount
                                        )
                                    }
                                    // Delete button
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        IconButton(
                                            onClick = { moduleToDelete = module },
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
                                // 描述
                                if (!module.description.isNullOrBlank()) {
                                    Text(
                                        module.description,
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
// 发布模块 Bottom Sheet
// ════════════════════════════════════════════════
