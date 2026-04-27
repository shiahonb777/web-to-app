package com.webtoapp.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.logging.AppLogger
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
import com.webtoapp.ui.screens.community.ModuleCard
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 统一市场页面 — 应用商店 + 模块市场
 * 使用顶部 Tab 切换「应用」和「模块」两个子页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppStoreScreen(
    cloudViewModel: CloudViewModel,
    onInstallModule: (String) -> Unit,
    onNavigateToModule: (Int) -> Unit = {},
    downloadManager: AppDownloadManager? = null
) {
    val apiClient: CloudApiClient = koinInject()
    val scope = rememberCoroutineScope()

    // ── Pager 状态 ──
    val pagerState = rememberPagerState(pageCount = { 2 })

    // ── 搜索状态 ──
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // ── 右上角菜单 ──
    var showMenu by remember { mutableStateOf(false) }
    var showDownloadManager by remember { mutableStateOf(false) }
    var showMyApps by remember { mutableStateOf(false) }
    var showPublishApp by remember { mutableStateOf(false) }
    // Module-specific dialogs
    var showMyModules by remember { mutableStateOf(false) }
    var showPublishModule by remember { mutableStateOf(false) }

    // 活跃下载数 badge
    val emptyTasks = remember { mutableStateOf<Map<Int, AppDownloadManager.DownloadTask>>(emptyMap()) }
    val activeTasks by (downloadManager?.activeTasks?.collectAsState() ?: emptyTasks)
    val activeCount = activeTasks.count {
        it.value.status == AppDownloadManager.DownloadStatus.DOWNLOADING ||
        it.value.status == AppDownloadManager.DownloadStatus.PENDING
    }
    val emptyDownloaded = remember { mutableStateOf<List<AppDownloadManager.DownloadedApp>>(emptyList()) }
    val downloadedAppsList by (downloadManager?.downloadedApps?.collectAsState() ?: emptyDownloaded)
    val downloadedCount = downloadedAppsList.size
    // 当前 Tab 名称
    val tabTitles = listOf(Strings.marketTabApps, Strings.marketTabModules)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (isSearchActive) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = {
                                isSearchActive = false
                            },
                            expanded = false,
                            onExpandedChange = { },
                            placeholder = {
                                Text(
                                    if (pagerState.currentPage == 0) Strings.storeSearchPlaceholder
                                    else Strings.moduleStoreSearchPlaceholder
                                )
                            },
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = null)
                                    }
                                }
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth()
                ) {}
            } else {
                TopAppBar(
                    title = {
                        // 内嵌 pill-style Tab 切换器 — 不占额外空间
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                tabTitles.forEachIndexed { index, title ->
                                    val isSelected = pagerState.currentPage == index
                                    Surface(
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { scope.launch { pagerState.animateScrollToPage(index) } },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Outlined.Search, contentDescription = null, Modifier.size(21.dp))
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = null, Modifier.size(21.dp))
                                // Badge for active downloads
                                if (activeCount > 0) {
                                    Badge(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-4).dp, y = 4.dp),
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text("$activeCount")
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (pagerState.currentPage == 0) {
                                    // ── Apps Tab Menu ──
                                    DropdownMenuItem(
                                        text = { Text(Strings.storeDownloadManager) },
                                        onClick = { showMenu = false; showDownloadManager = true },
                                        leadingIcon = { Icon(Icons.Outlined.Download, null) },
                                        trailingIcon = if (activeCount > 0 || downloadedCount > 0) {{
                                            Badge(containerColor = if (activeCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
                                                Text("${activeCount + downloadedCount}")
                                            }
                                        }} else null
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(Strings.storeMyApps) },
                                        onClick = { showMenu = false; showMyApps = true },
                                        leadingIcon = { Icon(Icons.Outlined.Apps, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(Strings.storePublishApp) },
                                        onClick = { showMenu = false; showPublishApp = true },
                                        leadingIcon = { Icon(Icons.Outlined.Publish, null) }
                                    )
                                } else {
                                    // ── Modules Tab Menu ──
                                    DropdownMenuItem(
                                        text = { Text(Strings.storeDownloadManager) },
                                        onClick = { showMenu = false; showDownloadManager = true },
                                        leadingIcon = { Icon(Icons.Outlined.Download, null) },
                                        trailingIcon = if (activeCount > 0 || downloadedCount > 0) {{
                                            Badge(containerColor = if (activeCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) {
                                                Text("${activeCount + downloadedCount}")
                                            }
                                        }} else null
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(Strings.storeMyModules) },
                                        onClick = { showMenu = false; showMyModules = true },
                                        leadingIcon = { Icon(Icons.Outlined.Extension, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(Strings.storePublishModule) },
                                        onClick = { showMenu = false; showPublishModule = true },
                                        leadingIcon = { Icon(Icons.Outlined.Publish, null) }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> AppsTabContent(
                    apiClient = apiClient,
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    downloadManager = downloadManager
                )
                1 -> ModulesTabContent(
                    cloudViewModel = cloudViewModel,
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    onInstallModule = onInstallModule,
                    onNavigateToModule = onNavigateToModule
                )
            }
        }
    }

    // ── Bottom Sheets ──
    if (showDownloadManager && downloadManager != null) {
        DownloadManagerSheet(
            downloadManager = downloadManager,
            onDismiss = { showDownloadManager = false }
        )
    }

    if (showMyApps) {
        MyAppsSheet(
            apiClient = apiClient,
            onDismiss = { showMyApps = false }
        )
    }

    if (showPublishApp) {
        PublishAppSheet(
            apiClient = apiClient,
            onDismiss = { showPublishApp = false },
            onPublished = {
                showPublishApp = false
            }
        )
    }

    // ── Module Bottom Sheets ──
    if (showMyModules) {
        MyModulesSheet(
            apiClient = apiClient,
            onDismiss = { showMyModules = false }
        )
    }

    if (showPublishModule) {
        PublishModuleSheet(
            apiClient = apiClient,
            onDismiss = { showPublishModule = false },
            onPublished = { showPublishModule = false }
        )
    }
}


// ════════════════════════════════════════════════
// Tab 1: 应用市场
// ════════════════════════════════════════════════

@Composable
private fun AppsTabContent(
    apiClient: CloudApiClient,
    searchQuery: String,
    isSearchActive: Boolean,
    downloadManager: AppDownloadManager? = null
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSort by rememberSaveable { mutableStateOf("downloads") }
    var sortOrder by rememberSaveable { mutableStateOf("desc") }
    var apps by remember { mutableStateOf<List<AppStoreItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalApps by remember { mutableIntStateOf(0) }
    var currentPage by rememberSaveable { mutableIntStateOf(1) }
    var selectedApp by remember { mutableStateOf<AppStoreItem?>(null) }

    val categoryLabels = mapOf(
        "tools" to Strings.storeCatTools,
        "social" to Strings.storeCatSocial,
        "education" to Strings.storeCatEducation,
        "entertainment" to Strings.storeCatEntertainment,
        "productivity" to Strings.storeCatProductivity,
        "lifestyle" to Strings.storeCatLifestyle,
        "business" to Strings.storeCatBusiness,
        "news" to Strings.storeCatNews,
        "finance" to Strings.storeCatFinance,
        "health" to Strings.storeCatHealth,
        "other" to Strings.storeCatOther,
    )

    val categoryIcons = mapOf(
        "tools" to Icons.Outlined.Build,
        "social" to Icons.Outlined.Forum,
        "education" to Icons.Outlined.School,
        "entertainment" to Icons.Outlined.SportsEsports,
        "productivity" to Icons.Outlined.Bolt,
        "lifestyle" to Icons.Outlined.Spa,
        "business" to Icons.Outlined.BusinessCenter,
        "news" to Icons.Outlined.Newspaper,
        "finance" to Icons.Outlined.AccountBalance,
        "health" to Icons.Outlined.FavoriteBorder,
        "other" to Icons.Outlined.Category,
    )

    val sortOptions = listOf(
        "downloads" to Strings.storeSortDownloads,
        "rating" to Strings.storeSortRating,
        "created_at" to Strings.storeSortNewest,
        "like_count" to Strings.storeSortLikes,
    )

    fun loadApps(page: Int = 1) {
        scope.launch {
            isLoading = true
            val result = apiClient.listStoreApps(
                category = selectedCategory,
                search = searchQuery.ifBlank { null },
                sort = selectedSort,
                order = sortOrder,
                page = page
            )
            result.onSuccess { resp ->
                apps = if (page == 1) resp.apps else apps + resp.apps
                categories = resp.categories
                totalApps = resp.total
                currentPage = page
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadApps() }
    LaunchedEffect(selectedCategory, selectedSort, sortOrder) { loadApps() }
    // 当搜索完成（不再 active 且 query 变化过）时重新加载
    LaunchedEffect(searchQuery, isSearchActive) {
        if (!isSearchActive) loadApps()
    }

    // ── Landscape-adaptive: detect orientation for multi-column grid ──
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1)

    LazyVerticalGrid(
        columns = gridColumns,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        horizontalArrangement = if (isLandscape) Arrangement.spacedBy(8.dp) else Arrangement.spacedBy(0.dp)
    ) {
        // ── 分类筛选 (full-width) ──
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            ) {
                item {
                    PremiumFilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(Strings.storeAllCategories, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Apps,
                                null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                items(categories) { cat ->
                    val icon = categoryIcons[cat] ?: Icons.Outlined.Category
                    PremiumFilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(categoryLabels[cat] ?: cat, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                icon,
                                null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }

        // ── 排序 (full-width) ──
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${Strings.storeAppsCount}: $totalApps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    sortOptions.forEach { (key, label) ->
                        val isSelected = selectedSort == key
                        PremiumFilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    sortOrder = if (sortOrder == "desc") "asc" else "desc"
                                } else {
                                    selectedSort = key
                                    sortOrder = "desc"
                                }
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(label, fontSize = 11.sp)
                                    if (isSelected) {
                                        Icon(
                                            if (sortOrder == "desc") Icons.Filled.KeyboardArrowDown
                                            else Icons.Filled.KeyboardArrowUp,
                                            contentDescription = if (sortOrder == "desc") Strings.sortDesc else Strings.sortAsc,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── Loading (full-width) ──
        if (isLoading && apps.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            Strings.storeLoadingApps,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // ── Empty state (full-width) ──
        if (!isLoading && apps.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                        )
                                    ),
                                    RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Store,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        Strings.storeEmpty,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        Strings.storeNoContentTryAgain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // ── 应用列表 — cards auto-fill grid columns ──
        items(apps, key = { it.id }) { app ->
            AppListCard(
                app = app,
                onClick = { selectedApp = app },
                downloadManager = downloadManager
            )
        }

        // ── Load more (full-width) ──
        if (apps.size < totalApps && !isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { loadApps(currentPage + 1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                )
                            )
                        )
                    ) {
                        Text(
                            Strings.storeLoadMore,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "(${apps.size}/$totalApps)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (isLoading && apps.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }

    // ── 应用详情底部弹窗 ──
    selectedApp?.let { app ->
        AppDetailSheet(
            app = app,
            apiClient = apiClient,
            downloadManager = downloadManager,
            onDismiss = { selectedApp = null }
        )
    }
}


// ════════════════════════════════════════════════
// Tab 2: 模块市场
// ════════════════════════════════════════════════

@Composable
private fun ModulesTabContent(
    cloudViewModel: CloudViewModel,
    searchQuery: String,
    isSearchActive: Boolean,
    onInstallModule: (String) -> Unit,
    onNavigateToModule: (Int) -> Unit = {}) {
    val modules by cloudViewModel.storeModules.collectAsStateWithLifecycle()
    val loading by cloudViewModel.storeLoading.collectAsStateWithLifecycle()
    val total by cloudViewModel.storeTotal.collectAsStateWithLifecycle()
    val featured by cloudViewModel.featuredModules.collectAsStateWithLifecycle()
    val updatableIds by cloudViewModel.updatableModuleIds.collectAsStateWithLifecycle()

    // 跟踪是否已完成首次加载——避免空列表瞬间闪过
    var initialLoaded by remember { mutableStateOf(false) }

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSort by rememberSaveable { mutableStateOf("downloads") }
    var sortOrder by rememberSaveable { mutableStateOf("desc") }

    val moduleCategories = listOf(
        "UI_ENHANCE" to Strings.catUiEnhance,
        "MEDIA" to Strings.catMedia,
        "PRIVACY" to Strings.catPrivacySecurity,
        "TOOLS" to Strings.catTools,
        "AD_BLOCK" to Strings.catAdBlock,
        "SOCIAL" to Strings.catSocial,
        "DEVELOPER" to Strings.catDeveloper,
        "OTHER" to Strings.catOther
    )

    val moduleSorts = listOf(
        "downloads" to Strings.moduleStoreSortDownloads,
        "rating" to Strings.moduleStoreSortRating,
        "created_at" to Strings.moduleStoreSortNewest,
        "like_count" to Strings.moduleStoreSortLikes,
    )

    fun loadModules() {
        cloudViewModel.loadStoreModules(
            selectedCategory,
            searchQuery.ifBlank { null },
            selectedSort,
            sortOrder
        )
    }

    // 首次进入触发加载
    LaunchedEffect(Unit) {
        loadModules()
        cloudViewModel.loadFeaturedModules()
        cloudViewModel.checkModuleUpdates()
    }
    LaunchedEffect(selectedCategory, selectedSort, sortOrder) { loadModules() }
    LaunchedEffect(searchQuery, isSearchActive) {
        if (!isSearchActive) loadModules()
    }

    // 当 loading 变为 false 且 modules 加载过后，标记首次加载完成
    LaunchedEffect(loading) {
        if (!loading && !initialLoaded) {
            initialLoaded = true
        }
    }

    // ── 首次加载：全屏 loading（覆盖 loading 尚未被 ViewModel 设为 true 的那一帧）──
    if (!initialLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    Strings.storeLoadingModules,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    // ── Landscape-adaptive: detect orientation for multi-column grid ──
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1)

    LazyVerticalGrid(
        columns = gridColumns,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = if (isLandscape) Arrangement.spacedBy(12.dp) else Arrangement.spacedBy(0.dp)
    ) {
        // ── 精选推荐 Banner (full-width) ──
        if (featured.isNotEmpty() && selectedCategory == null && searchQuery.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedModuleBanner(
                    modules = featured,
                    onModuleClick = { onNavigateToModule(it.id) },
                    onInstall = { module ->
                        cloudViewModel.downloadStoreModule(
                            moduleId = module.id,
                            onResult = { shareCode -> onInstallModule(shareCode) }
                        )
                    }
                )
            }
        }

        // ── 分类过滤 (full-width) ──
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PremiumFilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(Strings.moduleStoreCatAll, fontSize = 12.sp) }
                    )
                }
                items(moduleCategories) { (key, name) ->
                    PremiumFilterChip(
                        selected = selectedCategory == key,
                        onClick = {
                            selectedCategory = if (selectedCategory == key) null else key
                        },
                        label = { Text(name, fontSize = 12.sp) }
                    )
                }
            }
        }

        // ── 排序 + 统计 (full-width) ──
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Strings.moduleCount.format(total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    moduleSorts.forEach { (key, label) ->
                        val isSelected = selectedSort == key
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    sortOrder = if (sortOrder == "desc") "asc" else "desc"
                                } else {
                                    selectedSort = key
                                    sortOrder = "desc"
                                }
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            trailingIcon = if (isSelected) {{
                                Icon(
                                    if (sortOrder == "desc") Icons.Filled.KeyboardArrowDown
                                    else Icons.Filled.KeyboardArrowUp,
                                    contentDescription = if (sortOrder == "desc") Strings.sortDesc else Strings.sortAsc,
                                    modifier = Modifier.size(16.dp)
                                )
                            }} else null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        // ── 切换分类/排序后的加载中 (full-width) ──
        if (loading && initialLoaded) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }

        // ── 模块列表 — cards auto-fill grid columns ──
        if (!loading) {
            items(modules, key = { it.id }) { module ->
                ModuleCard(
                    module = module,
                    onClick = { onNavigateToModule(module.id) },
                    onInstall = {
                        cloudViewModel.downloadStoreModule(
                            moduleId = module.id,
                            onResult = { shareCode -> onInstallModule(shareCode) }
                        )
                    },
                    hasUpdate = module.id in updatableIds
                )
            }
        }

        // ── 空状态 (full-width) ──
        if (!loading && modules.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                        )
                                    ),
                                    RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        if (searchQuery.isNotBlank()) Strings.moduleStoreEmptySearch else Strings.moduleStoreEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        Strings.storeNoContentForModules,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }

}




// ════════════════════════════════════════════════
// 精选推荐 Banner — App Store style auto-scroll
// ════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedModuleBanner(
    modules: List<StoreModuleInfo>,
    onModuleClick: (StoreModuleInfo) -> Unit,
    onInstall: (StoreModuleInfo) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { modules.size })
    val installedTracker = koinInject<com.webtoapp.core.cloud.InstalledItemsTracker>()

    // Auto-scroll
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % modules.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            val module = modules[page]
            val isInstalled = installedTracker.isInstalled(module.id)

            EnhancedElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxSize().clickable { onModuleClick(module) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Gradient background
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                )
                            )
                        )
                    )

                    // Content
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: info
                        Column(modifier = Modifier.weight(1f)) {
                            // Featured badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFA726).copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Filled.Star, null, modifier = Modifier.size(11.dp),
                                        tint = Color(0xFFFFA726))
                                    Text(Strings.featured, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFA726))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(module.name, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(3.dp))
                            Text("by ${module.authorName}", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            if (!module.description.isNullOrBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(module.description, fontSize = 13.sp, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Downloads
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Download, null, Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Spacer(Modifier.width(2.dp))
                                    Text("${module.downloads}", fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                                // Rating
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = Color(0xFFFFC107))
                                    Spacer(Modifier.width(2.dp))
                                    Text(if (module.ratingCount > 0) String.format("%.1f", module.rating) else "-",
                                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFC107))
                                }
                            }
                        }

                        // Right: icon + install
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Module icon
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Outlined.Extension, null, Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            if (isInstalled) {
                                Surface(shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(13.dp),
                                            tint = Color(0xFF4CAF50))
                                        Text(Strings.installed, fontSize = 10.sp,
                                            color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                                    }
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { onInstall(module) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Download, null, Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(Strings.install, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Page indicator dots
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(modules.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }
}


// ════════════════════════════════════════════════
// 应用列表卡片 — Premium Design
// ════════════════════════════════════════════════

@Composable
private fun AppListCard(
    app: AppStoreItem,
    onClick: () -> Unit,
    downloadManager: AppDownloadManager? = null
) {
    val ratingColor = when {
        app.rating >= 4.0f -> Color(0xFF10B981)
        app.rating >= 3.0f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    val accentColor = when {
        app.isFeatured -> Color(0xFFF59E0B)
        app.rating >= 4.5f -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.primary
    }

    // Check download state
    val tasks = downloadManager?.activeTasks?.collectAsState()?.value ?: emptyMap()
    val downloadedList = downloadManager?.downloadedApps?.collectAsState()?.value ?: emptyList()
    val currentTask = tasks[app.id]
    val isDownloaded = downloadedList.any { it.appId == app.id }
    val isActiveDownload = currentTask?.status == AppDownloadManager.DownloadStatus.DOWNLOADING
    val downloadProgress = currentTask?.progress ?: 0f

    EnhancedElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // ─ Accent strip on left edge ─
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(80.dp)
                    .align(Alignment.CenterVertically)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.0f),
                                accentColor.copy(alpha = 0.8f),
                                accentColor.copy(alpha = 0.0f)
                            )
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─ App icon with subtle shadow ─
                Box {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 2.dp
                    ) {
                        if (!app.icon.isNullOrBlank()) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = app.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(15.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
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
                                    Icons.Outlined.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // ─ Info column ─
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title row
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
                        if (app.isFeatured) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        modifier = Modifier.size(10.dp),
                                        tint = Color(0xFFF59E0B)
                                    )
                                    Text(
                                        Strings.selected,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF59E0B),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // Author + version
                    Text(
                        "${app.authorName} · v${app.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        letterSpacing = 0.2.sp
                    )

                    // Stats pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rating pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ratingColor.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    null,
                                    modifier = Modifier.size(11.dp),
                                    tint = ratingColor
                                )
                                Text(
                                    String.format("%.1f", app.rating),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ratingColor
                                )
                            }
                        }

                        // Download count pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Download,
                                    null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    formatDownloads(app.downloads),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // ─ Download / Installed button ─
                when {
                    isDownloaded -> {
                        // Already downloaded — green check
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color(0xFF4CAF50))
                                Text(Strings.storeModulesInstalled, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50))
                            }
                        }
                    }
                    isActiveDownload -> {
                        // Downloading — progress ring
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Text(
                                "${(downloadProgress * 100).toInt()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    else -> {
                        // Not downloaded — download button (opens detail)
                        FilledTonalButton(
                            onClick = onClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Outlined.Download, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.storeGet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}


// ════════════════════════════════════════════════
// 应用详情底部弹窗
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailSheet(
    app: AppStoreItem,
    apiClient: CloudApiClient,
    downloadManager: AppDownloadManager? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var detail by remember { mutableStateOf(app) }
    var isLoadingDetail by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var actionFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }

    // Like state
    var isLiked by remember { mutableStateOf(false) }
    var currentLikeCount by remember { mutableIntStateOf(app.likeCount) }
    var isLiking by remember { mutableStateOf(false) }

    // Review state
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviews by remember { mutableStateOf<List<com.webtoapp.core.cloud.AppReviewItem>>(emptyList()) }
    var reviewsTotal by remember { mutableIntStateOf(0) }
    var isLoadingReviews by remember { mutableStateOf(false) }

    LaunchedEffect(app.id) {
        val result = apiClient.getStoreAppDetail(app.id)
        result.onSuccess { detail = it; currentLikeCount = it.likeCount }
        isLoadingDetail = false

        // Fetch like status
        when (val likeResult = apiClient.getLikeStatus(app.id)) {
            is com.webtoapp.core.auth.AuthResult.Success -> isLiked = likeResult.data
            else -> {} // Not logged in or error — no liked state
        }

        // Fetch reviews
        isLoadingReviews = true
        val reviewResult = apiClient.getAppReviews(app.id, page = 1, size = 5)
        reviewResult.onSuccess { resp ->
            reviews = resp.reviews
            reviewsTotal = resp.total
            android.util.Log.d("AppStore", "Loaded ${resp.reviews.size} reviews, total=${resp.total}")
        }
        reviewResult.onFailure { e ->
            android.util.Log.e("AppStore", "Failed to load reviews: ${e.message}", e)
        }
        isLoadingReviews = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(16.dp)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header with gradient hero ──
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Icon with shadow
                            Surface(
                                modifier = Modifier.size(76.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shadowElevation = 4.dp
                            ) {
                                if (!detail.icon.isNullOrBlank()) {
                                    AsyncImage(
                                        model = detail.icon,
                                        contentDescription = detail.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
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
                                            Icons.Outlined.Apps,
                                            contentDescription = null,
                                            modifier = Modifier.size(34.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    detail.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    "${detail.authorName} · v${detail.versionName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                detail.packageName?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Stats bar — frosted capsules ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill(Icons.Filled.Star, String.format("%.1f", detail.rating), "${detail.ratingCount} ${Strings.storeReviews}", modifier = Modifier.weight(1f))
                    StatPill(Icons.Outlined.Download, formatDownloads(detail.downloads), Strings.storeDownloads, modifier = Modifier.weight(1f))
                    StatPill(Icons.Outlined.ThumbUp, "$currentLikeCount", Strings.storeLikes, modifier = Modifier.weight(1f))
                }
            }

            // ── Download button with real-time progress ──
            item {
                // Observe download manager state for this app
                val tasks = if (downloadManager != null) {
                    downloadManager.activeTasks.collectAsState().value
                } else emptyMap()
                val downloadedList = if (downloadManager != null) {
                    downloadManager.downloadedApps.collectAsState().value
                } else emptyList()
                val currentTask = tasks[detail.id]
                val downloadedApp = downloadedList.find { it.appId == detail.id }

                // Determine button state
                val isCompleted = currentTask?.status == AppDownloadManager.DownloadStatus.COMPLETED || downloadedApp != null
                val isActiveDownload = currentTask?.status == AppDownloadManager.DownloadStatus.DOWNLOADING
                val isFailed = currentTask?.status == AppDownloadManager.DownloadStatus.FAILED
                val isPending = currentTask?.status == AppDownloadManager.DownloadStatus.PENDING

                when {
                    // ── State: Downloading — premium progress card ──
                    isActiveDownload || isPending -> {
                        val progress = currentTask?.progress ?: 0f
                        val speed = currentTask?.speed ?: 0
                        val downloaded = currentTask?.downloadedBytes ?: 0
                        val total = currentTask?.totalBytes ?: -1L

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                if (isPending) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                } else {
                                                    Text(
                                                        "${(progress * 100).toInt()}%",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            if (isPending) Strings.storePreparingDownload else Strings.storeDownloadingLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (speed > 0) {
                                        Text(
                                            downloadManager?.formatSpeed(speed) ?: "",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Progress bar with gradient track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (total > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.tertiary
                                                        )
                                                    ),
                                                    RoundedCornerShape(3.dp)
                                                )
                                        )
                                    } else {
                                        // Indeterminate — use composable indicator
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxSize(),
                                            trackColor = Color.Transparent
                                        )
                                    }
                                }

                                // Size info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        downloadManager?.formatSize(downloaded) ?: "0 B",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    if (total > 0) {
                                        Text(
                                            downloadManager?.formatSize(total) ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Cancel button
                                OutlinedButton(
                                    onClick = { downloadManager?.cancelDownload(detail.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Strings.storeCancelDownload, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // ── State: Completed — gradient install button ──
                    isCompleted -> {
                        val filePath = currentTask?.filePath ?: downloadedApp?.filePath
                        Button(
                            onClick = {
                                if (filePath != null) {
                                    downloadManager?.installApk(filePath)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.InstallMobile, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.storeInstallApp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }

                    // ── State: Failed — retry with error card ──
                    isFailed -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.ErrorOutline, null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        currentTask?.error ?: Strings.storeDownloadFailed,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            downloadManager?.dismissTask(detail.id)
                                            isDownloading = true
                                            try {
                                                val result = apiClient.downloadStoreApp(detail.id)
                                                result.onSuccess { urls ->
                                                    val url = urls["apk_url_github"] ?: urls["apk_url_gitee"]
                                                    if (url != null && downloadManager != null) {
                                                        downloadManager.startDownload(detail.id, detail.name, url)
                                                    } else if (url != null) {
                                                        uriHandler.openUri(url)
                                                    } else {
                                                        actionFailureReport = buildSheetFailureReport(
                                                            title = Strings.appDownloadFailed,
                                                            stage = Strings.getDownloadLinkStage,
                                                            summary = Strings.storeNoDownloadLink,
                                                            contextLines = listOf(
                                                                "appId=${detail.id}",
                                                                "appName=${detail.name}",
                                                                "retry=true"
                                                            )
                                                        )
                                                    }
                                                }
                                                result.onFailure { e ->
                                                    actionFailureReport = buildSheetFailureReport(
                                                        title = Strings.appDownloadFailed,
                                                        stage = Strings.getDownloadLinkStage,
                                                        summary = context.getString(
                                                            com.webtoapp.R.string.store_get_download_link_failed,
                                                            (e.message ?: "")
                                                        ),
                                                        contextLines = listOf(
                                                            "appId=${detail.id}",
                                                            "appName=${detail.name}",
                                                            "retry=true"
                                                        ),
                                                        throwable = e
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                actionFailureReport = buildSheetFailureReport(
                                                    title = Strings.appDownloadFailed,
                                                    stage = Strings.requestDownloadApiStage,
                                                    summary = context.getString(
                                                        com.webtoapp.R.string.store_network_error,
                                                        (e.message ?: "")
                                                    ),
                                                    contextLines = listOf(
                                                        "appId=${detail.id}",
                                                        "appName=${detail.name}",
                                                        "retry=true"
                                                    ),
                                                    throwable = e
                                                )
                                            } finally {
                                                isDownloading = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Strings.storeRedownload, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // ── State: Idle — premium download button ──
                    else -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    isDownloading = true
                                    try {
                                        val result = apiClient.downloadStoreApp(detail.id)
                                        result.onSuccess { urls ->
                                            val url = urls["apk_url_github"] ?: urls["apk_url_gitee"]
                                            if (url != null && downloadManager != null) {
                                                downloadManager.startDownload(detail.id, detail.name, url)
                                            } else if (url != null) {
                                                uriHandler.openUri(url)
                                            } else {
                                                actionFailureReport = buildSheetFailureReport(
                                                    title = Strings.appDownloadFailed,
                                                    stage = Strings.getDownloadLinkStage,
                                                    summary = Strings.storeNoDownloadLink,
                                                    contextLines = listOf(
                                                        "appId=${detail.id}",
                                                        "appName=${detail.name}",
                                                        "retry=false"
                                                    )
                                                )
                                            }
                                        }
                                        result.onFailure { e ->
                                            actionFailureReport = buildSheetFailureReport(
                                                title = Strings.appDownloadFailed,
                                                stage = Strings.getDownloadLinkStage,
                                                summary = context.getString(
                                                    com.webtoapp.R.string.store_get_download_link_failed,
                                                    (e.message ?: "")
                                                ),
                                                contextLines = listOf(
                                                    "appId=${detail.id}",
                                                    "appName=${detail.name}",
                                                    "retry=false"
                                                ),
                                                throwable = e
                                            )
                                        }
                                    } catch (e: Exception) {
                                        actionFailureReport = buildSheetFailureReport(
                                            title = Strings.appDownloadFailed,
                                            stage = Strings.requestDownloadApiStage,
                                            summary = context.getString(
                                                com.webtoapp.R.string.store_network_error,
                                                (e.message ?: "")
                                            ),
                                            contextLines = listOf(
                                                "appId=${detail.id}",
                                                "appName=${detail.name}",
                                                "retry=false"
                                            ),
                                            throwable = e
                                        )
                                    } finally {
                                        isDownloading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isDownloading
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Strings.storeGetDownloadLink, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Filled.Download, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Strings.storeDownloadBtn, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                            }
                        }
                    }
                }
            }

            // ── Screenshots ──
            if (detail.screenshots.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            Strings.storeScreenshots,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                "${detail.screenshots.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(detail.screenshots) { url ->
                            Surface(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(360.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shadowElevation = 2.dp
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = Strings.storeScreenshot,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // ── Description (Markdown) ──
            detail.description?.let { desc ->
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            Strings.storeDescription,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
                item {
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 22.sp
                    )
                }
            }

            // ── Developer info ──
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        Strings.storeDeveloperInfo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                }
            }
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    detail.contactEmail?.let {
                        DevInfoRow(Icons.Outlined.Email, Strings.storeEmail, it)
                    }
                    detail.websiteUrl?.let {
                        DevInfoRow(Icons.Outlined.Language, Strings.storeWebsite, it)
                    }
                    detail.groupChatUrl?.let {
                        DevInfoRow(Icons.Outlined.Groups, Strings.storeGroupChat, it)
                    }
                    detail.privacyPolicyUrl?.let {
                        DevInfoRow(Icons.Outlined.Security, Strings.storePrivacyPolicy, it)
                    }
                    detail.contactPhone?.let {
                        DevInfoRow(Icons.Outlined.Phone, Strings.storePhone, it)
                    }
                }
            }

            // ── Twitter/X style action bar with physics ──
            item {
                GlassDivider()
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Comment count
                    AppPhysicsActionButton(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        activeIcon = Icons.Outlined.ChatBubbleOutline,
                        count = reviewsTotal, isActive = false,
                        activeColor = MaterialTheme.colorScheme.primary,
                        onClick = { showReviewDialog = true }
                    )
                    // Like with toggle
                    AppPhysicsActionButton(
                        icon = Icons.Outlined.ThumbUp,
                        activeIcon = Icons.Filled.ThumbUp,
                        count = currentLikeCount,
                        isActive = isLiked,
                        activeColor = Color(0xFF4CAF50),
                        onClick = {
                            if (!isLiking) {
                                isLiking = true
                                scope.launch {
                                    when (val result = apiClient.likeStoreApp(detail.id)) {
                                        is com.webtoapp.core.auth.AuthResult.Success -> {
                                            isLiked = result.data.liked
                                            currentLikeCount = result.data.likeCount
                                        }
                                        is com.webtoapp.core.auth.AuthResult.Error -> {
                                            isLiking = false
                                            actionFailureReport = buildSheetFailureReport(
                                                title = Strings.appActionFailed,
                                                stage = Strings.likeAppStage,
                                                summary = result.message,
                                                contextLines = listOf(
                                                    "appId=${detail.id}",
                                                    "appName=${detail.name}",
                                                    "action=likeStoreApp"
                                                )
                                            )
                                            return@launch
                                        }
                                    }
                                    isLiking = false
                                }
                            }
                        }
                    )
                    // Share
                    AppPhysicsActionButton(
                        icon = Icons.Outlined.Share,
                        activeIcon = Icons.Filled.Share,
                        count = null, isActive = false,
                        activeColor = MaterialTheme.colorScheme.primary,
                        onClick = { /* share intent */ }
                    )
                    // Report
                    AppPhysicsActionButton(
                        icon = Icons.Outlined.Flag,
                        activeIcon = Icons.Filled.Flag,
                        count = null, isActive = false,
                        activeColor = Color(0xFFEF5350),
                        onClick = { showReportDialog = true }
                    )
                }
            }
            item {
                GlassDivider()
            }

            // ── Twitter/X style reviews section ──
            if (reviews.isEmpty() && !isLoadingReviews) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Strings.storeNoReviewsYet, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(Strings.storeBeFirstToReview, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                        }
                    }
                }
            }

            items(reviews.size) { index ->
                val review = reviews[index]
                StaggeredItem(index = index) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Avatar(name = review.authorName, size = 36)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(weight = 1f, fill = true)) {
                            // Name row
                            Text(review.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                            // Meta row: device · IP · time
                            val metaParts = mutableListOf<String>()
                            review.deviceModel?.let { metaParts.add(it) }
                            review.ipAddress?.let { metaParts.add("IP $it") }
                            review.createdAt?.let {
                                // Show "YYYY-MM-DD HH:mm"
                                metaParts.add(it.take(16).replace("T", " "))
                            }
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    metaParts.joinToString("  ·  "),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                            // Star rating row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                repeat(5) { i ->
                                    Icon(
                                        if (i < review.rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        null, modifier = Modifier.size(13.dp),
                                        tint = if (i < review.rating) Color(0xFFFFC107)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                    )
                                }
                            }
                            if (!review.comment.isNullOrBlank()) {
                                Text(review.comment, fontSize = 15.sp, lineHeight = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
                GlassDivider(Modifier.padding(start = 62.dp))
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        } // Scaffold
    }

    // Report dialog
    if (showReportDialog) {
        ReportDialog(
            appName = detail.name,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, description ->
                scope.launch {
                    val result = apiClient.reportStoreApp(detail.id, reason, description)
                    when (result) {
                        is com.webtoapp.core.auth.AuthResult.Success -> {
                            snackbarHostState.showSnackbar(Strings.storeReportSuccess)
                        }
                        is com.webtoapp.core.auth.AuthResult.Error -> {
                            actionFailureReport = buildSheetFailureReport(
                                title = Strings.appReportFailed,
                                stage = Strings.reportAppStage,
                                summary = context.getString(
                                    com.webtoapp.R.string.store_report_failed,
                                    result.message
                                ),
                                contextLines = listOf(
                                    "appId=${detail.id}",
                                    "appName=${detail.name}",
                                    "reason=$reason",
                                    "description=${description ?: ""}"
                                )
                            )
                        }
                    }
                    showReportDialog = false
                }
            }
        )
    }

    // Review dialog
    if (showReviewDialog) {
        var reviewRating by remember { mutableIntStateOf(5) }
        var reviewComment by remember { mutableStateOf("") }
        var isSubmittingReview by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmittingReview) showReviewDialog = false },
            icon = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    )
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.RateReview, null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            title = { Text("\"" + Strings.storeReviewSubmitTitle + " \"" + detail.name + "\"", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Star rating
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.storeReviewRatingLabel, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { reviewRating = star },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        if (star <= reviewRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        "Star $star",
                                        modifier = Modifier.size(32.dp),
                                        tint = if (star <= reviewRating) Color(0xFFFFC107)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    // Comment input
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text(Strings.storeReviewCommentLabel) },
                        placeholder = { Text(Strings.storeReviewPlaceholder) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingReview = true
                        scope.launch {
                            when (val result = apiClient.reviewStoreApp(
                                detail.id, reviewRating,
                                reviewComment.ifBlank { null }
                            )) {
                                is com.webtoapp.core.auth.AuthResult.Success -> {
                                    // Close dialog and stop spinner FIRST (before blocking snackbar)
                                    isSubmittingReview = false
                                    showReviewDialog = false

                                    // Refresh reviews and detail in background
                                    val reviewResult = apiClient.getAppReviews(detail.id, page = 1, size = 5)
                                    reviewResult.onSuccess { resp ->
                                        reviews = resp.reviews
                                        reviewsTotal = resp.total
                                        android.util.Log.d("AppStore", "Refreshed ${resp.reviews.size} reviews, total=${resp.total}")
                                    }
                                    reviewResult.onFailure { e ->
                                        android.util.Log.e("AppStore", "Failed to refresh reviews: ${e.message}", e)
                                    }
                                    val detailResult = apiClient.getStoreAppDetail(detail.id)
                                    detailResult.onSuccess { detail = it }

                                    // Show toast LAST (this suspends until dismissed)
                                    snackbarHostState.showSnackbar(Strings.storeReviewSuccess)
                                }
                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                    isSubmittingReview = false
                                    actionFailureReport = buildSheetFailureReport(
                                        title = Strings.appReviewSubmitFailed,
                                        stage = Strings.submitReviewStage,
                                        summary = Strings.storeReviewFailed + ": ${result.message}",
                                        contextLines = listOf(
                                            "appId=${detail.id}",
                                            "appName=${detail.name}",
                                            "rating=$reviewRating",
                                            "comment=$reviewComment"
                                        )
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isSubmittingReview,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSubmittingReview) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(Strings.storeReviewSubmit)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReviewDialog = false },
                    enabled = !isSubmittingReview
                ) { Text(Strings.storeReviewCancel) }
            }
        )
    }

    actionFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { actionFailureReport = null }
        )
    }
}

/**
 * 举报对话框
 */
@Composable
private fun ReportDialog(
    appName: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, description: String?) -> Unit
) {
    val reasons = listOf(
        "spam" to Strings.storeReportReasonSpam,
        "malicious" to Strings.storeReportReasonMalicious,
        "inappropriate" to Strings.storeReportReasonInappropriate,
        "copyright" to Strings.storeReportReasonCopyright,
        "other" to Strings.storeReportReasonOther
    )
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        icon = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Flag, null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        title = { Text(Strings.reportAppTitle.format(appName), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Strings.storeReportSelectReason,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                reasons.forEach { (value, label) ->
                    val isSelected = selectedReason == value
                    Surface(
                        onClick = { selectedReason = value },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
                        border = if (isSelected) BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        ) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = value },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.error
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    label = { Text(Strings.storeReportDescOptional) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    onSubmit(selectedReason, description.ifBlank { null })
                },
                enabled = selectedReason.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(Strings.storeReportSubmit)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(Strings.storeReviewCancel)
            }
        }
    )
}

/**
 * Twitter/X 物理弹簧操作按钮 — 应用商店版
 */
@Composable
private fun AppPhysicsActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int?, isActive: Boolean,
    activeColor: Color, onClick: () -> Unit
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val currentColor by animateColorAsState(
        if (isActive) activeColor else inactiveColor,
        tween(280), label = "appBtnClr"
    )

    // Particle burst trigger
    var burstKey by remember { mutableIntStateOf(0) }
    var showBurst by remember { mutableStateOf(false) }

    // Spring bounce
    var bouncing by remember { mutableStateOf(false) }
    val scaleVal by animateFloatAsState(
        if (bouncing) 1.35f else 1f,
        CommunityPhysics.LikeBounce,
        label = "appBtnScale",
        finishedListener = { bouncing = false }
    )

    Box(contentAlignment = Alignment.Center) {
        // Particle layer
        LikeBurstEffect(
            trigger = showBurst,
            color = activeColor,
            modifier = Modifier.size(40.dp)
        )

        TextButton(
            onClick = {
                bouncing = true
                if (!isActive) { showBurst = true; burstKey++ }
                onClick()
            },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Icon(
                if (isActive) activeIcon else icon, null,
                Modifier.size(20.dp).scale(scaleVal),
                tint = currentColor
            )
            count?.let {
                Spacer(Modifier.width(3.dp))
                AnimatedCounter(
                    count = it,
                    color = currentColor,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }

    // Reset particles
    LaunchedEffect(burstKey) {
        if (showBurst) {
            kotlinx.coroutines.delay(500)
            showBurst = false
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    icon, null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DevInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    icon, null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 0.3.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


private fun formatDownloads(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}


// ════════════════════════════════════════════════
// ════════════════════════════════════════════════
// 下载管理 Bottom Sheet (统一：活跃下载 + 已下载应用)
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadManagerSheet(
    downloadManager: AppDownloadManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tasks by downloadManager.activeTasks.collectAsState()
    val downloadedApps by downloadManager.downloadedApps.collectAsState()

    // Module installation tracking
    val context = androidx.compose.ui.platform.LocalContext.current
    val extensionManager = remember { com.webtoapp.core.extension.ExtensionManager.getInstance(context) }
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
                                            Strings.downloaded,
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

@Composable
private fun ActiveDownloadCard(
    task: AppDownloadManager.DownloadTask,
    downloadManager: AppDownloadManager
) {
    EnhancedElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            if (task.status == AppDownloadManager.DownloadStatus.PENDING) Strings.preparing
                            else "${downloadManager.formatSize(task.downloadedBytes)} / ${
                                if (task.totalBytes > 0) downloadManager.formatSize(task.totalBytes) else "?"
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (task.speed > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    downloadManager.formatSpeed(task.speed),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(32.dp)
                ) {
                    IconButton(
                        onClick = { downloadManager.cancelDownload(task.appId) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(Icons.Filled.Close, Strings.storeReviewCancel,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Gradient progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(task.progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            RoundedCornerShape(2.5.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${(task.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FailedDownloadCard(
    task: AppDownloadManager.DownloadTask,
    downloadManager: AppDownloadManager
) {
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
                                listOf(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline, null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    task.appName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    task.error ?: Strings.storeDownloadFailed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(32.dp)
            ) {
                IconButton(
                    onClick = { downloadManager.dismissTask(task.appId) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Outlined.Close, Strings.remove,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedAppCard(
    app: AppDownloadManager.DownloadedApp,
    downloadManager: AppDownloadManager
) {
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
                                listOf(
                                    Color(0xFF10B981).copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Android, null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF10B981)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Size pill
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            downloadManager.formatSize(app.fileSize),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    // Time pill
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            buildString {
                                val elapsed = System.currentTimeMillis() - app.downloadedAt
                                val mins = elapsed / 60_000
                                val hours = mins / 60
                                val days = hours / 24
                                append(when {
                                    days > 0 -> "\${days} " + Strings.daysAgo
                                    hours > 0 -> "\${hours} " + Strings.hoursAgo
                                    mins > 1 -> "\${mins} " + Strings.minutesAgo
                                    else -> Strings.justNow
                                })
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            // Install button
            Button(
                onClick = { downloadManager.installApk(app.filePath) },
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Outlined.InstallMobile, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Strings.installApp, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.width(4.dp))
            // Delete button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(32.dp)
            ) {
                IconButton(
                    onClick = { downloadManager.deleteDownloadedApp(app.appId) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Outlined.Delete, Strings.delete,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


// ════════════════════════════════════════════════
// 我的发布 — 共享组件
// ════════════════════════════════════════════════

/**
 * 统一 Header：渐变背景 + 标题 + 刷新按钮 + 右侧自定义 pills
 */
@Composable
private fun MyPublishedItemsHeader(
    title: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    pills: @Composable RowScope.() -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Refresh button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh, null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    pills()
                }
            }
        }
    }
}

/**
 * 统一统计概览行：三个等宽卡片 (下载/评分/点赞)
 */
@Composable
private fun StatsOverviewRow(
    totalDownloads: Int,
    avgRating: Float,
    totalLikes: Int,
    downloadLabel: String = Strings.storeTotalDownloads
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Downloads
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Download,
            iconColor = MaterialTheme.colorScheme.primary,
            value = formatDownloads(totalDownloads),
            label = downloadLabel
        )
        // Rating
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Star,
            iconColor = Color(0xFFFFC107),
            value = if (avgRating > 0f) String.format("%.1f", avgRating) else "-",
            label = Strings.storeAverageRating
        )
        // Likes
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.ThumbUp,
            iconColor = Color(0xFFE91E63),
            value = formatDownloads(totalLikes),
            label = Strings.storeTotalLikes
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = iconColor)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 统一删除确认对话框（带 loading 状态）
 */
@Composable
private fun ItemDeleteConfirmDialog(
    itemName: String,
    storeName: String,       // "商店" or "市场"
    actionVerb: String,      // "下载" or Strings.installApp
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Warning, null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        title = { Text(Strings.storeConfirmDelistTitle, fontWeight = FontWeight.Bold) },
        text = {
            Text(Strings.confirmDelistMessage.format(itemName, storeName, actionVerb))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (isDeleting) Strings.storeConfirmDelisting else Strings.storeConfirmDelistTitle)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(Strings.storeReviewCancel)
            }
        }
    )
}

/** 统一加载状态 */
@Composable
private fun PublishedItemLoadingState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/** 统一错误状态 */
@Composable
private fun PublishedItemErrorState(errorMsg: String?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline, null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                errorMsg ?: "Loading failed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry")
            }
        }
    }
}

/** 统一空状态 */
@Composable
private fun PublishedItemEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                modifier = Modifier.size(80.dp)
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
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Outlined.Publish, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Publish", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** 统一 stat pill（版本/下载量/评分/点赞） */
@Composable
private fun StatPillVersion(text: String) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StatPillWithIcon(
    icon: ImageVector,
    text: String,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    bgColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Surface(shape = RoundedCornerShape(5.dp), color = bgColor) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(10.dp), tint = iconColor)
            Text(text, fontSize = 10.sp, fontWeight = fontWeight, color = textColor)
        }
    }
}

/** 统一卡片信息行：版本 + 下载量 + 评分 + 点赞 */
@Composable
private fun PublishedItemStatsPills(
    versionName: String?,
    downloads: Int,
    rating: Float,
    ratingCount: Int,
    likeCount: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        versionName?.let { StatPillVersion("v$it") }
        StatPillWithIcon(
            icon = Icons.Outlined.Download,
            text = formatDownloads(downloads)
        )
        StatPillWithIcon(
            icon = Icons.Filled.Star,
            text = if (ratingCount > 0) String.format("%.1f", rating) else "-",
            iconColor = Color(0xFFFFC107),
            textColor = Color(0xFFFFC107),
            bgColor = Color(0xFFFFC107).copy(alpha = 0.1f),
            fontWeight = FontWeight.SemiBold
        )
        if (likeCount > 0) {
            StatPillWithIcon(
                icon = Icons.Outlined.ThumbUp,
                text = "$likeCount",
                iconColor = Color(0xFFE91E63).copy(alpha = 0.7f),
                textColor = Color(0xFFE91E63).copy(alpha = 0.7f),
                bgColor = Color(0xFFE91E63).copy(alpha = 0.08f)
            )
        }
    }
}

/** 统一分类标签 */
@Composable
private fun CategoryTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}


// ════════════════════════════════════════════════
// 我的应用 Bottom Sheet
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyAppsSheet(
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
    var actionFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }

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
        "tools" to Strings.catTools, "social" to Strings.catSocial, "education" to Strings.catEducation,
        "entertainment" to Strings.catEntertainment, "productivity" to Strings.catProductivity,
        "lifestyle" to Strings.catLifestyle, "business" to Strings.catBusiness,
        "news" to Strings.catNews, "finance" to Strings.catFinance,
        "health" to Strings.catHealth, "other" to Strings.catOther
    )

    // ── 删除确认（共享组件） ──
    appToDelete?.let { app ->
        ItemDeleteConfirmDialog(
            itemName = app.name,
            storeName = Strings.storeName,
            actionVerb = "Download this app",
            isDeleting = isDeleting,
            onConfirm = {
                isDeleting = true
                scope.launch {
                    when (val result = apiClient.deleteMyApp(app.id)) {
                        is com.webtoapp.core.auth.AuthResult.Success -> {
                            myApps = myApps.filterNot { it.id == app.id }
                            appToDelete = null
                        }
                        is com.webtoapp.core.auth.AuthResult.Error -> {
                            actionFailureReport = buildSheetFailureReport(
                                title = Strings.appUnpublishFailed,
                                stage = Strings.unpublishPublishedAppStage,
                                summary = result.message,
                                contextLines = listOf(
                                    "appId=${app.id}",
                                    "appName=${app.name}",
                                    "versionName=${app.versionName}"
                                )
                            )
                        }
                    }
                    isDeleting = false
                }
            },
            onDismiss = { appToDelete = null }
        )
    }

    // ── 管理控制台 ──
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
            // ── Header（共享组件） ──
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

            // ── 统计概览（共享组件） ──
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

            // ── 内容状态（共享组件） ──
            if (isLoading) {
                PublishedItemLoadingState("Loading my apps...")
            } else if (errorMsg != null) {
                PublishedItemErrorState(errorMsg) { loadMyApps() }
            } else if (myApps.isEmpty()) {
                PublishedItemEmptyState(
                    icon = Icons.Outlined.RocketLaunch,
                    title = Strings.storeNoPublishedApps,
                    subtitle = Strings.publishAppSubtitle,
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
                                            // 分类标签（共享组件）
                                            CategoryTag(
                                                label = categoryLabels[app.category] ?: app.category,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        // 信息 pills（共享组件）
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
                                // 描述
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

    actionFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { actionFailureReport = null }
        )
    }
}



// ════════════════════════════════════════════════
// 应用管理控制台 (Premium UI)
// ════════════════════════════════════════════════

/** 管理控制台用渐变色组 */
private val mgmtGradientBlue = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val mgmtGradientGreen = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
private val mgmtGradientOrange = listOf(Color(0xFFF7971E), Color(0xFFFFD200))
private val mgmtGradientRed = listOf(Color(0xFFEB3349), Color(0xFFF45C43))
private val mgmtGradientPurple = listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppManagementSheet(
    app: AppStoreItem,
    apiClient: CloudApiClient,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    data class MgmtTab(val icon: ImageVector, val label: String, val gradient: List<Color>)
    val tabs = listOf(
        MgmtTab(Icons.Outlined.Dashboard, "Overview", mgmtGradientBlue),
        MgmtTab(Icons.Outlined.VpnKey, "Activation Code", mgmtGradientPurple),
        MgmtTab(Icons.Outlined.Campaign, "Announcements", mgmtGradientOrange),
        MgmtTab(Icons.Outlined.SystemUpdate, "Updates", mgmtGradientGreen),
        MgmtTab(Icons.Outlined.People, "Users", mgmtGradientRed)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.95f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ══ Premium Header ══
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2))))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp),
                        shadowElevation = 8.dp
                    ) {
                        if (!app.icon.isNullOrBlank()) {
                            AsyncImage(
                                model = app.icon, contentDescription = app.name,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Apps, null, Modifier.size(24.dp), tint = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                                Text("v${app.versionName}", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                                Text("Management Console", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ══ Premium Pill-Style Tab Bar ══
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, tab ->
                    val isSelected = selectedTab == idx
                    Surface(
                        onClick = { selectedTab = idx },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        modifier = Modifier.height(42.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Brush.linearGradient(tab.gradient)
                                    else Brush.linearGradient(listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    )),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(tab.icon, null, Modifier.size(16.dp), tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text(tab.label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> ManagementOverviewTab(app, apiClient, scope, onAppUpdated = { /* refresh */ }, onAppDeleted = { /* close sheet */ })
                1 -> ManagementActivationTab(app, apiClient, scope)
                2 -> ManagementAnnouncementTab(app, apiClient, scope)
                3 -> ManagementUpdateTab(app, apiClient, scope)
                4 -> ManagementUsersTab(app, apiClient, scope)
            }
        }
    }
}

/** 渐变统计迷你卡 */
@Composable
private fun GradientMiniStat(gradient: List<Color>, icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(gradient)).padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.85f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp)
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
        }
    }
}

/** 信息行（带左色条） */
@Composable
private fun OverviewInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

// ── 概览 Tab ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManagementOverviewTab(app: AppStoreItem, apiClient: CloudApiClient? = null, scope: kotlinx.coroutines.CoroutineScope? = null, onAppUpdated: (() -> Unit)? = null, onAppDeleted: (() -> Unit)? = null) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editName by remember(app) { mutableStateOf(app.name) }
    var editDescription by remember(app) { mutableStateOf(app.description ?: "") }
    var editCategory by remember(app) { mutableStateOf(app.category) }
    var isUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var actionFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }

    val categories = listOf(
        "tools" to Strings.catTools, "social" to Strings.catSocial, "education" to Strings.catEducation,
        "entertainment" to Strings.catEntertainment, "productivity" to Strings.catProductivity,
        "lifestyle" to Strings.catLifestyle, "business" to Strings.catBusiness,
        "news" to Strings.catNews, "finance" to Strings.catFinance,
        "health" to Strings.catHealth, "other" to Strings.catOther
    )

    // 编辑对话框
    if (showEditDialog && apiClient != null && scope != null) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientBlue)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text("Edit app info", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("App name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("App description") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { (key, label) ->
                            FilterChip(
                                selected = editCategory == key,
                                onClick = { editCategory = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                    updateError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpdating = true
                        updateError = null
                        scope.launch {
                            when (val result = apiClient.updateStoreApp(app.id, editName, editDescription, editCategory)) {
                                is com.webtoapp.core.auth.AuthResult.Success -> {
                                    showEditDialog = false
                                    onAppUpdated?.invoke()
                                }
                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                    updateError = result.message
                                }
                            }
                            isUpdating = false
                        }
                    },
                    enabled = !isUpdating && editName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isUpdating) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }
                    Text(if (isUpdating) "Saving…" else "Save")
                }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }, enabled = !isUpdating) { Text(Strings.storeReviewCancel) } }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog && apiClient != null && scope != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientRed)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.DeleteForever, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text("Delete app", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Strings.confirmDeleteAppMessage.format(app.name), style = MaterialTheme.typography.bodyMedium)
                    Text(Strings.deleteAppWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            when (val result = apiClient.deleteStoreApp(app.id)) {
                                is com.webtoapp.core.auth.AuthResult.Success -> {
                                    showDeleteDialog = false
                                    onAppDeleted?.invoke()
                                }
                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                    actionFailureReport = buildSheetFailureReport(
                                        title = Strings.appDeleteFailed,
                                        stage = Strings.deleteAppStage,
                                        summary = result.message,
                                        contextLines = listOf(
                                            "appId=${app.id}",
                                            "appName=${app.name}",
                                            "versionName=${app.versionName}"
                                        )
                                    )
                                    isDeleting = false
                                }
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isDeleting) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(6.dp)) }
                    Text(if (isDeleting) Strings.deleting else Strings.confirmDelete, color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) { Text(Strings.storeReviewCancel) } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        // 渐变三格统计
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientMiniStat(mgmtGradientBlue, Icons.Outlined.Download, "${app.downloads}", Strings.downloads, Modifier.weight(1f))
                GradientMiniStat(mgmtGradientOrange, Icons.Filled.Star, String.format("%.1f", app.rating), Strings.storeReviewRatingLabel, Modifier.weight(1f))
                GradientMiniStat(mgmtGradientGreen, Icons.Outlined.ThumbUp, "${app.likeCount}", Strings.likes, Modifier.weight(1f))
            }
        }
        // 应用信息卡
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), shadowElevation = 1.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientBlue)))
                        Text(Strings.appInfo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (apiClient != null) {
                            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Edit, Strings.editLabel, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    OverviewInfoRow(Strings.appId, "#${app.id}")
                    OverviewInfoRow(Strings.version, "v${app.versionName}")
                    OverviewInfoRow("Category", app.category)
                    OverviewInfoRow(Strings.packageName, app.packageName ?: "—")
                    OverviewInfoRow(Strings.publisher, app.authorName)
                    app.createdAt?.let { OverviewInfoRow(Strings.publishTime, it.take(10)) }
                }
            }
        }
        // 操作区
        if (apiClient != null && scope != null) {
            item {
                // 编辑按钮
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(mgmtGradientBlue)).clickable { showEditDialog = true }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = Color.White)
                        Text("Edit app info", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                // 删除按钮（危险区域）
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Text(Strings.dangerZone, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Text(Strings.deleteAppDataWarning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.DeleteForever, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Strings.deleteThisApp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    actionFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { actionFailureReport = null }
        )
    }
}

// ── 激活码 Tab ──
@Composable
private fun ManagementActivationTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var settings by remember { mutableStateOf<ActivationSettings?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCodes by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    fun loadSettings() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getActivationSettings(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> settings = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { loadSettings() }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientPurple)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.VpnKey, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text(Strings.addActivationCode, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Strings.selectTemplateQuickGenerate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("numeric6" to "🔢 数字码", "standard" to "📋 标准码", "uuid" to "🔗 UUID").forEach { (id, label) ->
                            Surface(
                                onClick = {
                                    selectedTemplate = id
                                    newCodes = when (id) {
                                        "numeric6" -> (1..5).map { String.format("%06d", (100000..999999).random()) }
                                        "standard" -> { val c = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; (1..5).map { (1..3).joinToString("-") { (1..4).map { c.random() }.joinToString("") } } }
                                        else -> (1..5).map { java.util.UUID.randomUUID().toString() }
                                    }.joinToString("\n")
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedTemplate == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                            ) { Text(label, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 12.sp, fontWeight = if (selectedTemplate == id) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Text(Strings.orCustomPerLine, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = newCodes, onValueChange = { newCodes = it; selectedTemplate = null }, modifier = Modifier.fillMaxWidth().height(150.dp), placeholder = { Text(Strings.enterActivationCode, fontSize = 13.sp) }, shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) {
                        Text(Strings.totalCodes.format(newCodes.lines().filter { it.isNotBlank() }.size), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { val cl = newCodes.lines().filter { it.isNotBlank() }.map { it.trim() }; if (cl.isNotEmpty()) { isAdding = true; scope.launch { apiClient.createActivationCodes(app.id, cl); loadSettings(); showAddDialog = false; isAdding = false; newCodes = "" } } }, enabled = !isAdding && newCodes.lines().any { it.isNotBlank() }, shape = RoundedCornerShape(10.dp)) {
                    if (isAdding) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }
                    Text(if (isAdding) Strings.adding else Strings.addLabel)
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }, enabled = !isAdding) { Text(Strings.storeReviewCancel) } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        if (isLoading) { item { PublishedItemLoadingState(Strings.loadingActivationCodes) } }
        else if (errorMsg != null) { item { PublishedItemErrorState(errorMsg) { loadSettings() } } }
        else {
            val s = settings ?: return@LazyColumn
            // 渐变设置卡
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), shadowElevation = 1.dp) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientPurple)))
                            Text(Strings.activationCodeSettings, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column { Text(Strings.enableActivationVerification, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text(Strings.userNeedActivationCode, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                            Switch(checked = s.enabled, onCheckedChange = { scope.launch { apiClient.updateActivationSettings(app.id, it, s.deviceBindingEnabled, s.maxDevicesPerCode); loadSettings() } })
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column { Text(Strings.enableDeviceBinding, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text(Strings.oneCodeOneDevice, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                            Switch(checked = s.deviceBindingEnabled, onCheckedChange = { scope.launch { apiClient.updateActivationSettings(app.id, s.enabled, it, s.maxDevicesPerCode); loadSettings() } })
                        }
                    }
                }
            }
            // 渐变统计
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientMiniStat(mgmtGradientPurple, Icons.Outlined.VpnKey, "${s.totalCodes}", Strings.totalActivationCodesShort, Modifier.weight(1f))
                    GradientMiniStat(mgmtGradientGreen, Icons.Outlined.CheckCircle, "${s.usedCodes}", Strings.usedCodes, Modifier.weight(1f))
                    GradientMiniStat(mgmtGradientOrange, Icons.Outlined.Pending, "${s.totalCodes - s.usedCodes}", Strings.unusedCodes, Modifier.weight(1f))
                }
            }
            // 渐变添加按钮
            item {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(mgmtGradientPurple)).clickable { showAddDialog = true }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = Color.White)
                        Text(Strings.addActivationCode, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            // 激活码列表
            items(s.codes, key = { it.id }) { code ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (code.isUsed) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(if (code.isUsed) Icons.Outlined.CheckCircle else Icons.Outlined.Pending, null, Modifier.size(18.dp), tint = if (code.isUsed) Color(0xFF10B981) else Color(0xFFF59E0B))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(code.code, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CategoryTag(if (code.isUsed) Strings.usedCode else Strings.unusedCode, if (code.isUsed) Color(0xFF10B981) else Color(0xFFF59E0B))
                                code.usedByDeviceId?.let { Text("📱 ${it.take(8)}…", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                            }
                        }
                        IconButton(onClick = { scope.launch { apiClient.deleteActivationCode(app.id, code.id); loadSettings() } }, Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

// ── 公告 Tab ──
@Composable
private fun ManagementAnnouncementTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var annoTitle by remember { mutableStateOf("") }
    var annoContent by remember { mutableStateOf("") }
    var annoType by remember { mutableStateOf("info") }
    var annoPinned by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getAnnouncements(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> announcements = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    data class ATemplate(val id: String, val name: String, val emoji: String, val title: String, val content: String, val type: String)
    val templates = listOf(
        ATemplate("maintenance", Strings.templateMaintenance, "🔧", Strings.templateMaintenanceTitle, Strings.templateMaintenanceContent, "warning"),
        ATemplate("feature", Strings.templateFeature, "🎉", Strings.templateFeatureTitle, Strings.templateFeatureContent, "info"),
        ATemplate("security", Strings.templateSecurity, "🔒", Strings.templateSecurityTitle, Strings.templateSecurityContent, "warning"),
        ATemplate("event", Strings.templateEvent, "🎁", Strings.templateEventTitle, Strings.templateEventContent, "event")
    )

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreating) showCreateDialog = false },
            title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientOrange)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Campaign, null, Modifier.size(16.dp), tint = Color.White) }; Text(Strings.publishAnnouncement, fontWeight = FontWeight.Bold) } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(Strings.selectTemplate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("info" to Strings.infoNotice, "warning" to Strings.warningNotice, "event" to Strings.eventNotice).forEach { (type, label) -> FilterChip(selected = annoType == type, onClick = { annoType = type }, label = { Text(label, fontSize = 12.sp) }) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    OutlinedTextField(value = annoTitle, onValueChange = { annoTitle = it; selectedTemplateId = null }, label = { Text(Strings.titleLabel) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = annoContent, onValueChange = { annoContent = it; selectedTemplateId = null }, label = { Text(Strings.contentLabel) }, modifier = Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("info" to Strings.infoNotice, "warning" to Strings.warningNotice, "event" to Strings.eventNotice).forEach { (type, label) -> FilterChip(selected = annoType == type, onClick = { annoType = type }, label = { Text(label, fontSize = 12.sp) }) } }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(Strings.pinnedLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Switch(checked = annoPinned, onCheckedChange = { annoPinned = it }) }
                }
            },
            confirmButton = { Button(onClick = { isCreating = true; scope.launch { apiClient.createAnnouncement(app.id, annoTitle, annoContent, annoType, annoPinned); load(); showCreateDialog = false; isCreating = false; annoTitle = ""; annoContent = ""; selectedTemplateId = null } }, enabled = !isCreating && annoTitle.isNotBlank() && annoContent.isNotBlank(), shape = RoundedCornerShape(10.dp)) { if (isCreating) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }; Text(if (isCreating) Strings.publishing else Strings.publishButton) } },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }, enabled = !isCreating) { Text(Strings.storeReviewCancel) } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(mgmtGradientOrange)).clickable { showCreateDialog = true }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Outlined.Campaign, null, Modifier.size(18.dp), tint = Color.White); Text(Strings.publishNewAnnouncement, fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
        if (isLoading) { item { PublishedItemLoadingState(Strings.loadingAnnouncements) } }
        else if (errorMsg != null) { item { PublishedItemErrorState(errorMsg) { load() } } }
        else if (announcements.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.Campaign, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)); Text(Strings.noAnnouncements, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } } }
        } else {
            items(announcements, key = { it.id }) { anno ->
                val typeGrad = when (anno.type) { "warning" -> mgmtGradientOrange; "event" -> mgmtGradientPurple; else -> mgmtGradientBlue }
                val typeLabel = when (anno.type) { "warning" -> Strings.warningNotice; "event" -> Strings.eventNotice; else -> Strings.infoNotice }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(typeGrad)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                                if (anno.isPinned) Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFEF4444).copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text(Strings.pinnedBadge, fontSize = 10.sp, color = Color(0xFFEF4444)) }
                            }
                            IconButton(onClick = { scope.launch { apiClient.deleteAnnouncement(app.id, anno.id); load() } }, Modifier.size(28.dp)) { Icon(Icons.Outlined.Delete, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) }
                        }
                        Text(anno.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(anno.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("👁 ${anno.viewCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)); anno.createdAt?.let { Text(it.take(10), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)) } }
                    }
                }
            }
        }
    }
}

// ── 更新 Tab ──
@Composable
private fun ManagementUpdateTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var config by remember { mutableStateOf<UpdateConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showPushDialog by remember { mutableStateOf(false) }
    var pushVersionName by remember { mutableStateOf("") }; var pushVersionCode by remember { mutableStateOf("") }
    var pushTitle by remember { mutableStateOf("") }; var pushContent by remember { mutableStateOf("") }
    var pushForce by remember { mutableStateOf(false) }
    var useR2 by remember { mutableStateOf(false) }
    var pushTemplateId by remember { mutableStateOf("simple") }; var isPushing by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    // App selector state
    var myApps by remember { mutableStateOf<List<AppStoreItem>>(emptyList()) }
    var selectedSourceApp by remember { mutableStateOf<AppStoreItem?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var isLoadingApps by remember { mutableStateOf(false) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getUpdateConfig(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> config = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    // Load my apps list when dialog opened
    LaunchedEffect(showPushDialog) {
        if (showPushDialog && myApps.isEmpty()) {
            isLoadingApps = true
            when (val r = apiClient.listMyApps()) {
                is com.webtoapp.core.auth.AuthResult.Success -> {
                    myApps = r.data.apps.filter { it.id != app.id }
                }
                is com.webtoapp.core.auth.AuthResult.Error -> { /* ignore */ }
            }
            isLoadingApps = false
        }
    }

    data class UTemplate(val id: String, val name: String, val desc: String, val t: String, val c: String)
    val templates = listOf(
        UTemplate("simple", Strings.templateSimple, Strings.templateSimpleDesc, Strings.templateSimpleTitle, Strings.templateSimpleContent),
        UTemplate("dialog", Strings.templateDialog, Strings.templateDialogDesc, Strings.templateDialogTitle, Strings.templateDialogContent),
        UTemplate("fullscreen", Strings.templateFullscreen, Strings.templateFullscreenDesc, Strings.templateFullscreenTitle, Strings.templateFullscreenContent)
    )

    if (showPushDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPushing) showPushDialog = false },
            title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientGreen)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(16.dp), tint = Color.White) }; Text(Strings.pushUpdate, fontWeight = FontWeight.Bold) } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(Strings.updateTemplate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        templates.forEach { t -> Surface(onClick = { selectedTemplateId = t.id; pushTemplateId = t.id; pushTitle = t.t; pushContent = t.c }, shape = RoundedCornerShape(10.dp), color = if (selectedTemplateId == t.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) { Column(Modifier.padding(8.dp)) { Text(t.name, fontSize = 11.sp, fontWeight = if (selectedTemplateId == t.id) FontWeight.Bold else FontWeight.Normal); Text(t.desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) } } }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pushVersionName, onValueChange = { pushVersionName = it }, label = { Text(Strings.versionNoLabel, fontSize = 12.sp) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("1.2.0", fontSize = 12.sp) })
                        OutlinedTextField(value = pushVersionCode, onValueChange = { pushVersionCode = it }, label = { Text(Strings.versionCodeShortLabel, fontSize = 12.sp) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("2", fontSize = 12.sp) })
                    }
                    OutlinedTextField(value = pushTitle, onValueChange = { pushTitle = it; selectedTemplateId = null }, label = { Text(Strings.updateTitleLabel) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = pushContent, onValueChange = { pushContent = it; selectedTemplateId = null }, label = { Text(Strings.updateContentLabel) }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))

                    // ── App Selector (replaces APK URL input) ──
                    Text(Strings.linkUpdateApp, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        onClick = { showAppPicker = !showAppPicker },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedSourceApp != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (selectedSourceApp != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (selectedSourceApp != null) {
                                val sa = selectedSourceApp!!
                                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(mgmtGradientGreen)), contentAlignment = Alignment.Center) {
                                    if (sa.icon != null) AsyncImage(sa.icon, null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    else Icon(Icons.Filled.Apps, null, Modifier.size(16.dp), tint = Color.White)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(sa.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("v${sa.versionName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Outlined.Apps, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text(Strings.selectPublishedAppAsSource, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Icon(if (showAppPicker) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }

                    // Expandable app list
                    androidx.compose.animation.AnimatedVisibility(visible = showAppPicker) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))) {
                            Column(Modifier.fillMaxWidth().padding(6.dp).heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                                if (isLoadingApps) {
                                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
                                } else if (myApps.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) { Text(Strings.noOtherPublishedApps, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                                } else {
                                    myApps.forEach { item ->
                                        val isSelected = selectedSourceApp?.id == item.id
                                        Surface(
                                            onClick = { selectedSourceApp = item; showAppPicker = false; if (pushVersionName.isBlank()) pushVersionName = item.versionName },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                        ) {
                                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                                    if (item.icon != null) AsyncImage(item.icon, null, Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                                    else Text(item.name.take(1), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Column(Modifier.weight(1f)) {
                                                    Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("v${item.versionName} · " + Strings.storeDownloadsCount.format(item.downloads), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                }
                                                if (isSelected) Icon(Icons.Filled.RadioButtonChecked, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                else Icon(Icons.Filled.RadioButtonUnchecked, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (pushForce) Color(0xFFEF4444).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)) {
                        Row(Modifier.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column { Text(if (pushForce) Strings.forceUpdate else Strings.optionalUpdate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text(if (pushForce) Strings.forceUpdateDesc else Strings.optionalUpdateDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                            Switch(checked = pushForce, onCheckedChange = { pushForce = it })
                        }
                    }

                    // R2 云存储加速
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (useR2) Color(0xFF3B82F6).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)) {
                        Row(Modifier.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (useR2) Strings.r2CdnAccelerate else Strings.r2CloudStorage, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(if (useR2) Strings.r2CdnDesc else Strings.r2StorageDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            Switch(checked = useR2, onCheckedChange = { useR2 = it })
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { isPushing = true; scope.launch { apiClient.pushUpdate(app.id, pushVersionName, pushVersionCode.toIntOrNull() ?: 1, pushTitle, pushContent, selectedSourceApp?.id, pushForce, 0, pushTemplateId); load(); showPushDialog = false; isPushing = false } }, enabled = !isPushing && pushVersionName.isNotBlank() && pushTitle.isNotBlank(), shape = RoundedCornerShape(10.dp)) { if (isPushing) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }; Text(if (isPushing) Strings.pushingUpdate else Strings.pushUpdateButton) } },
            dismissButton = { TextButton(onClick = { showPushDialog = false }, enabled = !isPushing) { Text(Strings.storeReviewCancel) } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(mgmtGradientGreen)).clickable { showPushDialog = true }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(18.dp), tint = Color.White); Text(Strings.pushNewVersion, fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
        if (isLoading) { item { PublishedItemLoadingState(Strings.loadingUpdateConfig) } }
        else if (errorMsg != null) { item { PublishedItemErrorState(errorMsg) { load() } } }
        else {
            val c = config ?: return@LazyColumn
            if (c.isActive) {
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), shadowElevation = 1.dp) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientGreen)))
                                Text(Strings.currentUpdateConfig, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(if (c.isForceUpdate) mgmtGradientRed else mgmtGradientGreen)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(if (c.isForceUpdate) Strings.forceUpdate else Strings.optionalUpdate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            OverviewInfoRow(Strings.targetVersion, "v${c.latestVersionName} (${c.latestVersionCode})")
                            OverviewInfoRow(Strings.updateTitleLabel2, c.updateTitle)
                            OverviewInfoRow(Strings.templateLabel, c.templateId)
                            c.sourceAppName?.let { OverviewInfoRow(Strings.sourceApp, it) } ?: c.apkUrl?.let { OverviewInfoRow("APK", it.take(35) + "…") }
                            Text(c.updateContent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                        }
                    }
                }
            } else {
                item { Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)); Text(Strings.noUpdateConfig, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } } }
            }
        }
    }
}

// ── 用户 Tab ──
@Composable
private fun ManagementUsersTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var geoData by remember { mutableStateOf<List<GeoDistribution>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showGeo by remember { mutableStateOf(false) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getAppUsers(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> users = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; when (val r = apiClient.getUserGeoDistribution(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> geoData = r.data; is com.webtoapp.core.auth.AuthResult.Error -> {} }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        if (isLoading) { item { PublishedItemLoadingState(Strings.loadingUsers) } }
        else if (errorMsg != null) { item { PublishedItemErrorState(errorMsg) { load() } } }
        else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientMiniStat(mgmtGradientBlue, Icons.Outlined.People, "${users.size}", Strings.totalUsers, Modifier.weight(1f))
                    GradientMiniStat(mgmtGradientGreen, Icons.Outlined.FiberManualRecord, "${users.count { it.isActive }}", Strings.activeUsers, Modifier.weight(1f))
                    GradientMiniStat(mgmtGradientPurple, Icons.Outlined.Public, "${geoData.size}", Strings.countriesRegions, Modifier.weight(1f))
                }
            }
            // 地理分布
            item {
                Surface(Modifier.fillMaxWidth().clickable { showGeo = !showGeo }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientRed))); Text(Strings.geoDistribution, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                        Icon(if (showGeo) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
            if (showGeo && geoData.isNotEmpty()) {
                items(geoData) { geo ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Text(countryFlag(geo.countryCode), fontSize = 20.sp); Text(geo.country, fontWeight = FontWeight.SemiBold) }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("${geo.count}", fontWeight = FontWeight.Bold); Text("${String.format("%.1f", geo.percentage)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                            }
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                                Box(Modifier.fillMaxWidth(geo.percentage / 100f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brush.linearGradient(mgmtGradientBlue)))
                            }
                            geo.regions.take(3).forEach { r -> Row(Modifier.fillMaxWidth().padding(start = 28.dp), Arrangement.SpaceBetween) { Text(r.region, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)); Text("${r.count}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } }
                        }
                    }
                }
            }
            // 用户列表
            item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientBlue))); Text(Strings.userList, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) } }
            if (users.isEmpty()) { item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { Text(Strings.noUserData, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } } }
            else {
                items(users, key = { it.id }) { user ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(mgmtGradientBlue)), contentAlignment = Alignment.Center) {
                                Text(user.id.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${user.id.take(12)}…", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                    if (user.isActive) Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    user.deviceModel?.let { Text("📱 $it", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                                    user.country?.let { Text("${countryFlag(it)} $it", fontSize = 10.sp) }
                                    user.appVersion?.let { Text("v$it", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                                }
                            }
                            user.activationCode?.let { Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(mgmtGradientGreen.map { c -> c.copy(alpha = 0.15f) })).padding(horizontal = 7.dp, vertical = 3.dp)) { Text(Strings.storeActivated, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981)) } }
                        }
                    }
                }
            }
        }
    }
}

/** 国家代码转 emoji 旗帜 */
private fun countryFlag(countryCode: String): String {
    if (countryCode.length != 2) return "🌍"
    val first = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

private data class SheetFailureReport(
    val title: String,
    val summary: String,
    val details: String
)

private fun buildSheetFailureReport(
    title: String,
    stage: String,
    summary: String,
    contextLines: List<String>,
    throwable: Throwable? = null
): SheetFailureReport {
    throwable?.let { AppLogger.e("AppStoreScreen", "$title failed at $stage", it) }
    val details = buildString {
        appendLine(title)
        appendLine("stage: $stage")
        appendLine("summary: $summary")
        if (contextLines.isNotEmpty()) {
            appendLine()
            appendLine("context:")
            contextLines.forEach { appendLine(it) }
        }
        if (throwable != null) {
            appendLine()
            appendLine("exception:")
            appendLine(android.util.Log.getStackTraceString(throwable))
        }
        appendLine()
        appendLine("recent_logs:")
        append(AppLogger.getRecentLogTail())
    }
    return SheetFailureReport(title = title, summary = summary, details = details)
}

private fun buildApkBuildFailureReport(
    context: android.content.Context,
    project: com.webtoapp.data.model.WebApp,
    error: com.webtoapp.core.apkbuilder.BuildResult.Error
): SheetFailureReport {
    val buildLog = com.webtoapp.core.apkbuilder.BuildLogger(context).readLogContent(error.logPath)

    val details = buildString {
        appendLine("APK 构建失败")
        appendLine("stage: apk_build")
        appendLine("summary: ${error.message}")
        appendLine()
        appendLine("project:")
        appendLine("name=${project.name}")
        appendLine("appType=${project.appType}")
        appendLine("source=${project.url}")
        appendLine()
        appendLine("log_path:")
        appendLine(error.logPath ?: "<unavailable>")
        appendLine()
        appendLine("build_log:")
        appendLine(buildLog ?: "<build log unavailable>")
        appendLine()
        appendLine("recent_logs:")
        append(AppLogger.getRecentLogTail())
    }

    return SheetFailureReport(
        title = Strings.apkBuildFailed,
        summary = error.message,
        details = details
    )
}

@Composable
private fun SheetFailureReportDialog(
    report: SheetFailureReport,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(report.title)
                Text(
                    report.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = report.details,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .padding(bottom = 48.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )

                    FilledTonalButton(
                        onClick = { clipboardManager.setText(AnnotatedString(report.details)) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.copy)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.close)
            }
        }
    )
}

// ════════════════════════════════════════════════
// 发布应用 Bottom Sheet
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishAppSheet(
    apiClient: CloudApiClient,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Load local projects from database
    val db = remember { com.webtoapp.data.database.AppDatabase.getInstance(context) }
    val allProjects by db.webAppDao().getAllWebApps().collectAsStateWithLifecycle(initialValue = emptyList())

    // Selected project
    var selectedProject by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    var showProjectPicker by remember { mutableStateOf(false) }

    // Auto-filled from project, but editable
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("other") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCode by remember { mutableStateOf("1") }
    var packageName by remember { mutableStateOf("") }
    var iconUrl by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var screenshotUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var contactEmail by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var privacyPolicyUrl by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }
    var publishFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }

    // ── 激活码配置 ──
    var enableActivation by remember { mutableStateOf(false) }
    var enableDeviceBinding by remember { mutableStateOf(false) }
    var activationCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCodeTemplate by remember { mutableStateOf<String?>(null) }
    var customCodeInput by remember { mutableStateOf("") }


    // The built APK file from the selected project
    var selectedApkFile by remember { mutableStateOf<java.io.File?>(null) }

    // Inline build state
    var isBuilding by remember { mutableStateOf(false) }
    var buildProgress by remember { mutableIntStateOf(0) }
    var buildProgressText by remember { mutableStateOf("") }
    var buildFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }
    var showBuildFailureDialog by remember { mutableStateOf(false) }

    // Screenshot picker (multi-select)
    var screenshotUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        screenshotUris = screenshotUris + uris
    }

    // Icon picker
    var iconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { iconUri = it }
    }

    val categories = listOf(
        "tools" to Strings.catTools, "social" to Strings.catSocial, "education" to Strings.catEducation,
        "entertainment" to Strings.catEntertainment, "productivity" to Strings.catProductivity,
        "lifestyle" to Strings.catLifestyle, "business" to Strings.catBusiness,
        "news" to Strings.catNews, "finance" to Strings.catFinance,
        "health" to Strings.catHealth, "other" to Strings.catOther
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
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
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    Strings.storePublishApp,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    Strings.selectAppToPublish,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // ── 选择应用 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.selectApp, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.selectCreatedApp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    Surface(
                        onClick = { showProjectPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedProject != null)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedProject != null) {
                                // Show selected project info
                                if (selectedProject!!.iconPath != null) {
                                    AsyncImage(
                                        model = selectedProject!!.iconPath,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primaryContainer,
                                                            MaterialTheme.colorScheme.tertiaryContainer
                                                        )
                                                    ),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.Android, null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedProject!!.name, fontWeight = FontWeight.SemiBold)
                                    Row {
                                        Text(
                                            selectedProject!!.appType.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (selectedApkFile != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "✅ APK 已就绪",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.Outlined.SwapHoriz, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Icon(Icons.Outlined.Apps, null, modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    Strings.tapToSelectApp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // APK build section
                    if (selectedProject != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        if (selectedApkFile != null && !isBuilding) {
                            // APK found — show info
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp),
                                                tint = Color(0xFF10B981))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(selectedApkFile!!.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${selectedApkFile!!.length() / 1024} KB · " + Strings.storeApkReady,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981).copy(alpha = 0.8f))
                                    }
                                    // Rebuild button
                                    TextButton(onClick = {
                                        buildFailureReport = null
                                        showBuildFailureDialog = false
                                        isBuilding = true
                                        buildProgress = 0
                                        buildProgressText = Strings.preparingBuild
                                        scope.launch {
                                            val apkBuilder = com.webtoapp.core.apkbuilder.ApkBuilder(context)
                                    val result = apkBuilder.buildApk(selectedProject!!) { p, t ->
                                                buildProgress = p
                                                buildProgressText = t
                                            }
                                            when (result) {
                                                is com.webtoapp.core.apkbuilder.BuildResult.Success -> {
                                                    selectedApkFile = result.apkFile
                                                    buildFailureReport = null
                                                    showBuildFailureDialog = false
                                                }
                                                is com.webtoapp.core.apkbuilder.BuildResult.Error -> {
                                                    buildFailureReport = buildApkBuildFailureReport(
                                                        context = context,
                                                        project = selectedProject!!,
                                                        error = result
                                                    )
                                                    showBuildFailureDialog = true
                                                }
                                            }
                                            isBuilding = false
                                        }
                                    }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Strings.rebuild, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        } else if (isBuilding) {
                            // Building in progress
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(Strings.buildingApk,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("$buildProgress%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(2.5.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(buildProgress / 100f)
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.tertiary
                                                        )
                                                    ),
                                                    RoundedCornerShape(2.5.dp)
                                                )
                                        )
                                    }
                                    Text(buildProgressText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        } else {
                            // No APK — show build button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Build, null, modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(Strings.needBuildBeforePublish,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            buildFailureReport = null
                                            showBuildFailureDialog = false
                                            isBuilding = true
                                            buildProgress = 0
                                            buildProgressText = Strings.preparingBuild
                                            scope.launch {
                                                val apkBuilder = com.webtoapp.core.apkbuilder.ApkBuilder(context)
                                                val result = apkBuilder.buildApk(selectedProject!!) { p, t ->
                                                    buildProgress = p
                                                    buildProgressText = t
                                                }
                                                when (result) {
                                                    is com.webtoapp.core.apkbuilder.BuildResult.Success -> {
                                                        selectedApkFile = result.apkFile
                                                        buildFailureReport = null
                                                        showBuildFailureDialog = false
                                                    }
                                                    is com.webtoapp.core.apkbuilder.BuildResult.Error -> {
                                                        buildFailureReport = buildApkBuildFailureReport(
                                                            context = context,
                                                            project = selectedProject!!,
                                                            error = result
                                                        )
                                                        showBuildFailureDialog = true
                                                    }
                                                }
                                                isBuilding = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Icon(Icons.Outlined.Build, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(Strings.oneClickBuildApk, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                        }

                        if (buildFailureReport != null && !isBuilding) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            Icons.Outlined.ErrorOutline,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                Strings.buildFailed,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                buildFailureReport!!.summary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showBuildFailureDialog = true },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Outlined.Article, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(Strings.viewFullError)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 基本信息 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.basicInfo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.appNameIconVersion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // 应用名称
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.appNameLabel) },
                        placeholder = { Text(Strings.appNamePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.Apps, null, modifier = Modifier.size(20.dp)) }
                    )
                }

                // 图标
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Icon preview
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            if (iconUri != null) {
                                AsyncImage(
                                    model = iconUri,
                                    contentDescription = Strings.iconPreview,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (iconUrl.isNotBlank()) {
                                AsyncImage(
                                    model = iconUrl,
                                    contentDescription = Strings.iconPreview,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                )
                                            ),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Image, null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { iconPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (iconUri != null) Strings.changeIcon else Strings.selectIcon)
                            }
                            Text(Strings.selectIconFromGallery,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 分类
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.selectAppCategory,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { (key, label) ->
                            PremiumFilterChip(
                                selected = selectedCategory == key,
                                onClick = { selectedCategory = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // 版本
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            label = { Text(Strings.versionNameLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = versionCode,
                            onValueChange = { versionCode = it.filter { c -> c.isDigit() } },
                            label = { Text(Strings.versionCodeLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // 包名
                item {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text(Strings.packageNameLabel) },
                        placeholder = { Text("com.example.myapp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── 描述和标签 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.descriptionAndTags, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.describeAppFeatures,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(Strings.appDescLabel) },
                        placeholder = { Text(Strings.appDescPlaceholder) },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text(Strings.tagsLabel) },
                        placeholder = { Text(Strings.tagsPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── 截图 (多张) ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.screenshotsRequired, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.addScreenshots,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // 已添加的截图预览
                if (screenshotUris.isNotEmpty() || screenshotUrls.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Already uploaded URLs
                            items(screenshotUrls.size) { index ->
                                Box(modifier = Modifier.size(85.dp, 150.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxSize(),
                                        shadowElevation = 2.dp
                                    ) {
                                        AsyncImage(
                                            model = screenshotUrls[index],
                                            contentDescription = Strings.screenshotLabel.format(index + 1),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            screenshotUrls = screenshotUrls.toMutableList().also { it.removeAt(index) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(22.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            // Newly picked local URIs (not yet uploaded)
                            items(screenshotUris.size) { index ->
                                Box(modifier = Modifier.size(85.dp, 150.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxSize(),
                                        shadowElevation = 2.dp
                                    ) {
                                        AsyncImage(
                                            model = screenshotUris[index],
                                            contentDescription = Strings.newScreenshotLabel.format(index + 1),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            screenshotUris = screenshotUris.toMutableList().also { it.removeAt(index) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(22.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 添加截图（从相册选择）
                item {
                    Surface(
                        onClick = { screenshotPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Strings.addScreenshotsFromAlbum,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        Strings.screenshotsAdded.format(screenshotUrls.size + screenshotUris.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }




                // ── 联系信息 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.contactInfo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.contactInfoHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text(Strings.emailLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Outlined.Email, null, modifier = Modifier.size(18.dp)) }
                        )
                        OutlinedTextField(
                            value = websiteUrl,
                            onValueChange = { websiteUrl = it },
                            label = { Text(Strings.websiteLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Outlined.Language, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = privacyPolicyUrl,
                        onValueChange = { privacyPolicyUrl = it },
                        label = { Text(Strings.privacyPolicyUrlLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.PrivacyTip, null, modifier = Modifier.size(20.dp)) }
                    )
                }

                // ── 激活码配置区 ──
                item {
                    EnhancedElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.VpnKey, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(Strings.activationCodeConfig, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Switch(checked = enableActivation, onCheckedChange = { enableActivation = it })
                            }

                            if (enableActivation) {
                                Text(
                                    Strings.userActivationCodeHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )

                                // 设备绑定
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(Strings.deviceBindingLabel, style = MaterialTheme.typography.bodyMedium)
                                        Text(Strings.oneCodeOneDeviceAlt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                    Switch(checked = enableDeviceBinding, onCheckedChange = { enableDeviceBinding = it })
                                }

                                HorizontalDivider()

                                // 模板快速生成
                                Text(Strings.quickGenerate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "numeric6" to Strings.numericCodeLabel,
                                        "standard" to Strings.standardCodeLabel,
                                        "uuid" to Strings.uuidCodeLabel
                                    ).forEach { (id, label) ->
                                        Surface(
                                            onClick = {
                                                selectedCodeTemplate = id
                                                val gen = when (id) {
                                                    "numeric6" -> (1..5).map { String.format("%06d", (100000..999999).random()) }
                                                    "standard" -> {
                                                        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                                                        (1..5).map { (1..3).joinToString("-") { (1..4).map { chars.random() }.joinToString("") } }
                                                    }
                                                    else -> (1..5).map { java.util.UUID.randomUUID().toString() }
                                                }
                                                activationCodes = activationCodes + gen
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selectedCodeTemplate == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                                        ) {
                                            Text(
                                                label,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedCodeTemplate == id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedCodeTemplate == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // 自定义输入
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customCodeInput,
                                        onValueChange = { customCodeInput = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text(Strings.enterCustomActivationCode, fontSize = 13.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    )
                                    Button(
                                        onClick = {
                                            if (customCodeInput.isNotBlank()) {
                                                activationCodes = activationCodes + customCodeInput.trim()
                                                customCodeInput = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        enabled = customCodeInput.isNotBlank()
                                    ) {
                                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // 已添加的激活码列表
                                if (activationCodes.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(Strings.addedCodesCount.format(activationCodes.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        TextButton(onClick = { activationCodes = emptyList(); selectedCodeTemplate = null }) {
                                            Text(Strings.clearAll, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        activationCodes.forEachIndexed { idx, code ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        code,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    IconButton(
                                                        onClick = { activationCodes = activationCodes.toMutableList().also { it.removeAt(idx) } },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
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

                // ── 上传进度 ──
                if (isPublishing && uploadProgress > 0f) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            uploadStatus.ifBlank { Strings.uploading },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        "${(uploadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(uploadProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    )
                                                ),
                                                RoundedCornerShape(2.5.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 发布按钮 ──
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        enabled = !isPublishing && !isBuilding,
                        onClick = {
                            if (selectedProject == null) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.selectAppToPublish) }
                                return@Button
                            }
                            if (name.isBlank() || description.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.storeFillRequired) }
                                return@Button
                            }
                            if (screenshotUrls.isEmpty() && screenshotUris.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.storeAddScreenshot) }
                                return@Button
                            }
                            scope.launch {
                                isPublishing = true
                                uploadProgress = 0f

                                // Helper: convert content URI to temp file
                                fun uriToTempFile(uri: android.net.Uri, prefix: String, ext: String): java.io.File? {
                                    return try {
                                        val input = context.contentResolver.openInputStream(uri) ?: return null
                                        val tempFile = java.io.File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
                                        tempFile.outputStream().use { out -> input.copyTo(out) }
                                        input.close()
                                        tempFile
                                    } catch (e: Exception) { null }
                                }

                                // Step 1: Upload icon if selected locally
                                var finalIconUrl = iconUrl.ifBlank { null }
                                if (iconUri != null) {
                                    uploadStatus = Strings.uploadingIconAlready
                                    val iconFile = uriToTempFile(iconUri!!, "icon", "png")
                                    if (iconFile != null) {
                                        when (val r = apiClient.uploadAsset(iconFile, "image/png")) {
                                            is com.webtoapp.core.auth.AuthResult.Success -> finalIconUrl = r.data
                                            is com.webtoapp.core.auth.AuthResult.Error -> {
                                                val summary = Strings.iconUploadFailed.format(r.message)
                                                publishFailureReport = buildSheetFailureReport(
                                                    title = Strings.appPublishFailed,
                                                    stage = Strings.uploadIconStage,
                                                    summary = summary,
                                                    contextLines = listOf(
                                                        "project=${selectedProject?.name ?: "unknown"}",
                                                        "iconUri=${iconUri}",
                                                        "name=$name",
                                                        "versionName=$versionName",
                                                        "versionCode=$versionCode"
                                                    )
                                                )
                                                iconFile.delete()
                                                isPublishing = false
                                                uploadProgress = 0f
                                                uploadStatus = summary
                                                return@launch
                                            }
                                        }
                                        iconFile.delete()
                                    } else {
                                        val summary = Strings.iconReadFailed
                                        publishFailureReport = buildSheetFailureReport(
                                            title = Strings.appPublishFailed,
                                            stage = Strings.readIconStage,
                                            summary = summary,
                                            contextLines = listOf(
                                                "project=${selectedProject?.name ?: "unknown"}",
                                                "iconUri=${iconUri}",
                                                "name=$name"
                                            )
                                        )
                                        isPublishing = false
                                        uploadProgress = 0f
                                        uploadStatus = summary
                                        return@launch
                                    }
                                }

                                // Step 2: Upload local screenshots
                                val allScreenshotUrls = screenshotUrls.toMutableList()
                                if (screenshotUris.isNotEmpty()) {
                                    val total = screenshotUris.size
                                    for ((idx, uri) in screenshotUris.withIndex()) {
                                        uploadStatus = Strings.uploadingScreenshot.format(idx + 1, total)
                                        uploadProgress = (idx.toFloat()) / (total + 2)
                                        val scrFile = uriToTempFile(uri, "screenshot_$idx", "png")
                                        if (scrFile != null) {
                                            when (val r = apiClient.uploadAsset(scrFile, "image/png")) {
                                                is com.webtoapp.core.auth.AuthResult.Success -> allScreenshotUrls.add(r.data)
                                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                                    val summary = Strings.screenshotUploadFailed.format(idx + 1, r.message)
                                                    publishFailureReport = buildSheetFailureReport(
                                                        title = Strings.appPublishFailed,
                                                        stage = Strings.uploadScreenshotStage,
                                                        summary = summary,
                                                        contextLines = listOf(
                                                            "project=${selectedProject?.name ?: "unknown"}",
                                                            "screenshotIndex=${idx + 1}",
                                                            "screenshotUri=$uri",
                                                            "name=$name"
                                                        )
                                                    )
                                                    scrFile.delete()
                                                    isPublishing = false
                                                    uploadProgress = 0f
                                                    uploadStatus = summary
                                                    return@launch
                                                }
                                            }
                                            scrFile.delete()
                                        } else {
                                            val summary = Strings.screenshotReadFailed.format(idx + 1)
                                            publishFailureReport = buildSheetFailureReport(
                                                title = Strings.appPublishFailed,
                                                stage = Strings.readScreenshotStage,
                                                summary = summary,
                                                contextLines = listOf(
                                                    "project=${selectedProject?.name ?: "unknown"}",
                                                    "screenshotIndex=${idx + 1}",
                                                    "screenshotUri=$uri"
                                                )
                                            )
                                            isPublishing = false
                                            uploadProgress = 0f
                                            uploadStatus = summary
                                            return@launch
                                        }
                                    }
                                }

                                // Step 3: Upload APK from build output via asset upload
                                var apkUrlGithub: String? = null
                                if (selectedApkFile != null && selectedApkFile!!.exists()) {
                                    try {
                                        uploadStatus = Strings.uploadingApk
                                        when (val r = apiClient.uploadAsset(
                                            selectedApkFile!!,
                                            "application/vnd.android.package-archive"
                                        ) { progress -> uploadProgress = 0.5f + progress * 0.4f }) {
                                            is com.webtoapp.core.auth.AuthResult.Success -> apkUrlGithub = r.data
                                            is com.webtoapp.core.auth.AuthResult.Error -> {
                                                val summary = Strings.apkUploadFailed.format(r.message)
                                                publishFailureReport = buildSheetFailureReport(
                                                    title = Strings.appPublishFailed,
                                                    stage = Strings.uploadApkStage,
                                                    summary = summary,
                                                    contextLines = listOf(
                                                        "project=${selectedProject?.name ?: "unknown"}",
                                                        "apk=${selectedApkFile!!.absolutePath}",
                                                        "apkSize=${selectedApkFile!!.length()}",
                                                        "name=$name",
                                                        "versionName=$versionName",
                                                        "versionCode=$versionCode"
                                                    )
                                                )
                                                isPublishing = false
                                                uploadProgress = 0f
                                                uploadStatus = summary
                                                return@launch
                                            }
                                        }
                                    } catch (e: Exception) {
                                        val summary = Strings.apkUploadFailed.format(e.message)
                                        publishFailureReport = buildSheetFailureReport(
                                            title = Strings.appPublishFailed,
                                            stage = Strings.uploadApkStage,
                                            summary = summary,
                                            contextLines = listOf(
                                                "project=${selectedProject?.name ?: "unknown"}",
                                                "apk=${selectedApkFile!!.absolutePath}",
                                                "apkSize=${selectedApkFile!!.length()}",
                                                "name=$name"
                                            ),
                                            throwable = e
                                        )
                                        isPublishing = false
                                        uploadProgress = 0f
                                        uploadStatus = summary
                                        return@launch
                                    }
                                } else {
                                    // Warn: no APK selected — app won't be downloadable
                                    scope.launch {
                                        snackbarHostState.showSnackbar(Strings.noApkWarning)
                                    }
                                }

                                // Step 4: Publish app info to store
                                uploadStatus = Strings.publishingAppInfo
                                uploadProgress = 0.95f
                                val result = apiClient.publishApp(
                                    name = name,
                                    description = description,
                                    category = selectedCategory,
                                    versionName = versionName,
                                    versionCode = versionCode.toIntOrNull() ?: 1,
                                    packageName = packageName.ifBlank { null },
                                    icon = finalIconUrl,
                                    tags = tags.ifBlank { null },
                                    screenshots = allScreenshotUrls,
                                    apkUrlGithub = apkUrlGithub,
                                    apkUrlGitee = null,
                                    contactEmail = contactEmail.ifBlank { null },
                                    websiteUrl = websiteUrl.ifBlank { null },
                                    privacyPolicyUrl = privacyPolicyUrl.ifBlank { null }
                                )
                                isPublishing = false
                                uploadProgress = 0f
                                uploadStatus = ""
                                when (result) {
                                    is com.webtoapp.core.auth.AuthResult.Success -> {
                                        snackbarHostState.showSnackbar(Strings.storePublishSuccess)
                                        onPublished()
                                    }
                                    is com.webtoapp.core.auth.AuthResult.Error -> {
                                        val summary = "${Strings.storePublishFailed}: ${result.message}"
                                        publishFailureReport = buildSheetFailureReport(
                                            title = Strings.appPublishFailed,
                                            stage = Strings.submitAppInfoStage,
                                            summary = summary,
                                            contextLines = listOf(
                                                "project=${selectedProject?.name ?: "unknown"}",
                                                "name=$name",
                                                "versionName=$versionName",
                                                "versionCode=$versionCode",
                                                "apkUrlGithub=${apkUrlGithub ?: "null"}"
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.publishing, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Outlined.Publish, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.storePublishApp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Project picker dialog
    if (showProjectPicker) {
        AlertDialog(
            onDismissRequest = { showProjectPicker = false },
            title = {
                Column {
                    Text(Strings.selectAppToPublish, fontWeight = FontWeight.Bold)
                    Text(
                        Strings.localAppsCount.format(allProjects.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (allProjects.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.size(56.dp)
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
                                                RoundedCornerShape(18.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Apps, null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                    }
                                }
                                Text(Strings.noAppsCreateFirst,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                    items(allProjects.size) { index ->
                        val project = allProjects[index]
                        Surface(
                            onClick = {
                                selectedProject = project
                                // Auto-fill fields from project
                                name = project.name
                                val exportConfig = project.apkExportConfig
                                versionName = exportConfig?.customVersionName ?: "1.0.0"
                                versionCode = (exportConfig?.customVersionCode ?: 1).toString()
                                packageName = exportConfig?.customPackageName ?: project.packageName ?: ""
                                // Set icon from project icon path
                                if (project.iconPath != null) {
                                    iconUri = android.net.Uri.fromFile(java.io.File(project.iconPath!!))
                                }
                                // Find latest built APK for this project
                                val apkBuilder = com.webtoapp.core.apkbuilder.ApkBuilder(context)
                                val builtApks = apkBuilder.getBuiltApks()
                                val sanitizedName = project.name.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5._-]"), "_")
                                selectedApkFile = builtApks
                                    .filter { it.name.contains(sanitizedName, ignoreCase = true) }
                                    .maxByOrNull { it.lastModified() }

                                showProjectPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedProject?.id == project.id)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth(),
                            border = if (selectedProject?.id == project.id)
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (project.iconPath != null) {
                                    AsyncImage(
                                        model = project.iconPath,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primaryContainer,
                                                            MaterialTheme.colorScheme.tertiaryContainer
                                                        )
                                                    ),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Android, null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(project.name,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    Text(project.appType.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selectedProject?.id == project.id) {
                                    Icon(Icons.Filled.CheckCircle, null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectPicker = false }) {
                    Text(Strings.storeReviewCancel)
                }
            }
        )
    }

    publishFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { publishFailureReport = null }
        )
    }

    if (showBuildFailureDialog && buildFailureReport != null) {
        SheetFailureReportDialog(
            report = buildFailureReport!!,
            onDismiss = { showBuildFailureDialog = false }
        )
    }
}


// ════════════════════════════════════════════════
// 我的模块 Bottom Sheet
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyModulesSheet(
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
    var actionFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }

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
            storeName = Strings.marketName,
            actionVerb = Strings.installModule,
            isDeleting = isDeleting,
            onConfirm = {
                isDeleting = true
                scope.launch {
                    when (val result = apiClient.deleteMyModule(module.id)) {
                        is com.webtoapp.core.auth.AuthResult.Success -> {
                            myModules = myModules.filterNot { it.id == module.id }
                            moduleToDelete = null
                        }
                        is com.webtoapp.core.auth.AuthResult.Error -> {
                            actionFailureReport = buildSheetFailureReport(
                                title = Strings.moduleUnpublishFailed,
                                stage = Strings.unpublishPublishedModuleStage,
                                summary = result.message,
                                contextLines = listOf(
                                    "moduleId=${module.id}",
                                    "moduleName=${module.name}",
                                    "versionName=${module.versionName ?: ""}"
                                )
                            )
                        }
                    }
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
                    downloadLabel = Strings.totalInstalls
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 内容状态（共享组件） ──
            if (isLoading) {
                PublishedItemLoadingState(Strings.loadingMyModules)
            } else if (errorMsg != null) {
                PublishedItemErrorState(errorMsg) { loadMyModules() }
            } else if (myModules.isEmpty()) {
                PublishedItemEmptyState(
                    icon = Icons.Outlined.Extension,
                    title = Strings.storeNoPublishedModules,
                    subtitle = Strings.publishModuleSubtitle,
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

    actionFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { actionFailureReport = null }
        )
    }
}


// ════════════════════════════════════════════════
// 发布模块 Bottom Sheet
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishModuleSheet(
    apiClient: CloudApiClient,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Load local modules from ExtensionManager
    val extensionManager = remember { com.webtoapp.core.extension.ExtensionManager.getInstance(context) }
    val localModules by extensionManager.modules.collectAsState()

    // Selected module
    var selectedModule by remember { mutableStateOf<com.webtoapp.core.extension.ExtensionModule?>(null) }
    var showModulePicker by remember { mutableStateOf(false) }

    // Auto-filled from module, but editable
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var shareCode by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("tools") }
    var tags by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCode by remember { mutableIntStateOf(1) }
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }
    var publishFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }

    // Icon picker
    var iconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var iconUrl by remember { mutableStateOf("") }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { iconUri = it } }

    // Screenshot picker (multi-select)
    var screenshotUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris -> screenshotUris = screenshotUris + uris }

    val moduleCategories = listOf(
        "tools" to Strings.catTools, "ui" to Strings.catUi, "media" to Strings.catMedia,
        "social" to Strings.catSocial, "productivity" to Strings.catProductivity,
        "education" to Strings.catEducation, "entertainment" to Strings.catEntertainment,
        "developer" to Strings.catDeveloper, "other" to Strings.catOther
    )

    // Map ExtensionModule category to store category string
    fun mapCategory(cat: com.webtoapp.core.extension.ModuleCategory): String = when (cat) {
        com.webtoapp.core.extension.ModuleCategory.CONTENT_FILTER,
        com.webtoapp.core.extension.ModuleCategory.CONTENT_ENHANCE -> "tools"
        com.webtoapp.core.extension.ModuleCategory.STYLE_MODIFIER,
        com.webtoapp.core.extension.ModuleCategory.THEME -> "ui"
        com.webtoapp.core.extension.ModuleCategory.MEDIA,
        com.webtoapp.core.extension.ModuleCategory.VIDEO,
        com.webtoapp.core.extension.ModuleCategory.IMAGE,
        com.webtoapp.core.extension.ModuleCategory.AUDIO -> "media"
        com.webtoapp.core.extension.ModuleCategory.SOCIAL -> "social"
        com.webtoapp.core.extension.ModuleCategory.FUNCTION_ENHANCE,
        com.webtoapp.core.extension.ModuleCategory.AUTOMATION,
        com.webtoapp.core.extension.ModuleCategory.DATA_EXTRACT,
        com.webtoapp.core.extension.ModuleCategory.DATA_SAVE -> "productivity"
        com.webtoapp.core.extension.ModuleCategory.READING,
        com.webtoapp.core.extension.ModuleCategory.TRANSLATE -> "education"
        com.webtoapp.core.extension.ModuleCategory.SHOPPING,
        com.webtoapp.core.extension.ModuleCategory.NAVIGATION,
        com.webtoapp.core.extension.ModuleCategory.INTERACTION -> "entertainment"
        com.webtoapp.core.extension.ModuleCategory.DEVELOPER,
        com.webtoapp.core.extension.ModuleCategory.SECURITY,
        com.webtoapp.core.extension.ModuleCategory.ANTI_TRACKING -> "developer"
        com.webtoapp.core.extension.ModuleCategory.ACCESSIBILITY,
        com.webtoapp.core.extension.ModuleCategory.OTHER -> "other"
    }

    // Module picker dialog
    if (showModulePicker) {
        AlertDialog(
            onDismissRequest = { showModulePicker = false },
            title = {
                Column {
                    Text(Strings.selectModuleToPublish, fontWeight = FontWeight.Bold)
                    Text(
                        Strings.localModulesCount.format(localModules.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            },
            text = {
                if (localModules.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Transparent,
                                modifier = Modifier.size(56.dp)
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
                                            RoundedCornerShape(18.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Extension, null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                }
                            }
                            Text(Strings.noLocalModules,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center)
                            Text(Strings.createModuleFirst,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(localModules, key = { it.id }) { module ->
                            val isSelected = selectedModule?.id == module.id
                            Surface(
                                onClick = {
                                    selectedModule = module
                                    // Auto-fill all fields from module metadata
                                    name = module.name
                                    description = module.description
                                    selectedCategory = mapCategory(module.category)
                                    tags = module.tags.joinToString(",")
                                    versionName = module.version.name
                                    versionCode = module.version.code
                                    // Generate share code (need to load code from file first)
                                    val loadedModule = extensionManager.ensureCodeLoaded(module)
                                    shareCode = loadedModule.toShareCode()
                                    showModulePicker = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primaryContainer,
                                                            MaterialTheme.colorScheme.tertiaryContainer
                                                        )
                                                    ),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Extension, null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(module.name,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                module.category.getDisplayName(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                "v\${module.version.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Filled.CheckCircle, null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModulePicker = false }) {
                    Text(Strings.storeReviewCancel)
                }
            }
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
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Header ──
                item {
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
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    Strings.storePublishModule,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    Strings.selectModuleToPublish,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // ── 选择模块 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.selectModule, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.selectCreatedLocalModule,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    Surface(
                        onClick = { showModulePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedModule != null)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedModule != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        MaterialTheme.colorScheme.tertiaryContainer
                                                    )
                                                ),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Extension, null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedModule!!.name, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            selectedModule!!.category.getDisplayName(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "v${selectedModule!!.version.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(Icons.Outlined.SwapHoriz, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    Strings.tapToSelectModule,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Share code auto-generated indicator
                    if (selectedModule != null && shareCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp),
                                            tint = Color(0xFF10B981))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(Strings.shareCodeAutoGenerated,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(Strings.charCount.format(shareCode.length),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF10B981).copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }

                // ── 基本信息 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.basicInfo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.moduleNameIconVersion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // 模块名称
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.moduleNameLabel) },
                        placeholder = { Text(Strings.moduleNamePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(20.dp)) }
                    )
                }

                // 图标
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            if (iconUri != null) {
                                AsyncImage(
                                    model = iconUri,
                                    contentDescription = Strings.iconPreview,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (iconUrl.isNotBlank()) {
                                AsyncImage(
                                    model = iconUrl,
                                    contentDescription = Strings.iconPreview,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                )
                                            ),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Extension, null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { iconPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (iconUri != null) Strings.changeIcon else Strings.selectIcon)
                            }
                            Text(Strings.selectModuleIconFromGallery,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── 分类和标签 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.selectModuleCategory,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(moduleCategories) { (key, label) ->
                            PremiumFilterChip(
                                selected = selectedCategory == key,
                                onClick = { selectedCategory = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // 版本
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            label = { Text(Strings.versionNameLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = versionCode.toString(),
                            onValueChange = { versionCode = it.toIntOrNull() ?: 1 },
                            label = { Text(Strings.versionCodeLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // ── 描述和标签 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.descriptionAndTags, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.describeModuleFeatures,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(Strings.moduleDescLabel) },
                        placeholder = { Text(Strings.moduleDescPlaceholder) },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text(Strings.tagsLabel) },
                        placeholder = { Text(Strings.moduleTagsPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── 截图 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.screenshotsOptional, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.addScreenshotsPreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                if (screenshotUris.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(screenshotUris.size) { index ->
                                Box(modifier = Modifier.size(85.dp, 150.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxSize(),
                                        shadowElevation = 2.dp
                                    ) {
                                        AsyncImage(
                                            model = screenshotUris[index],
                                            contentDescription = Strings.screenshotLabel.format(index + 1),
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            screenshotUris = screenshotUris.toMutableList().also { it.removeAt(index) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(22.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { screenshotPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.addScreenshotFromGallery,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (screenshotUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(Strings.addedScreenshotsCount.format(screenshotUris.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }

                // ── 手动分享码输入 (高级) ──
                if (selectedModule == null) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                            Text(Strings.manualShareCodeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(Strings.noShareCodeLocalModule,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }

                    item {
                        OutlinedTextField(
                            value = shareCode,
                            onValueChange = { shareCode = it },
                            label = { Text(Strings.moduleShareCodeLabel) },
                            placeholder = { Text(Strings.pasteModuleShareCode) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            supportingText = { Text(Strings.shareCodeExportHint) },
                            minLines = 3,
                            maxLines = 6
                        )
                    }
                }

                // ── Upload progress ──
                if (isPublishing && uploadProgress > 0f) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text(uploadStatus.ifBlank { Strings.uploading },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium)
                                    }
                                    Text("${(uploadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(uploadProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                                ),
                                                RoundedCornerShape(2.5.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 发布按钮 ──
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        enabled = !isPublishing,
                        onClick = {
                            if (name.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.enterModuleName) }
                                return@Button
                            }
                            if (shareCode.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.selectModuleOrShareCode) }
                                return@Button
                            }
                            scope.launch {
                                isPublishing = true
                                uploadProgress = 0f

                                // Helper: convert content URI to temp file
                                fun uriToTempFile(uri: android.net.Uri, prefix: String, ext: String): java.io.File? {
                                    return try {
                                        val input = context.contentResolver.openInputStream(uri) ?: return null
                                        val tempFile = java.io.File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
                                        tempFile.outputStream().use { out -> input.copyTo(out) }
                                        input.close()
                                        tempFile
                                    } catch (e: Exception) { null }
                                }

                                // Step 1: Upload icon if selected locally
                                var finalIconUrl = iconUrl.ifBlank { null }
                                if (iconUri != null) {
                                    uploadStatus = Strings.uploadingIconAlready
                                    uploadProgress = 0.1f
                                    val iconFile = uriToTempFile(iconUri!!, "icon", "png")
                                    if (iconFile != null) {
                                        when (val r = apiClient.uploadAsset(iconFile, "image/png")) {
                                            is com.webtoapp.core.auth.AuthResult.Success -> finalIconUrl = r.data
                                            is com.webtoapp.core.auth.AuthResult.Error -> {
                                                val summary = Strings.iconUploadFailed.format(r.message)
                                                publishFailureReport = buildSheetFailureReport(
                                                    title = Strings.modulePublishFailed,
                                                    stage = Strings.uploadIconStage,
                                                    summary = summary,
                                                    contextLines = listOf(
                                                        "module=${selectedModule?.name ?: "manual"}",
                                                        "iconUri=${iconUri}",
                                                        "name=$name",
                                                        "versionName=$versionName",
                                                        "versionCode=$versionCode"
                                                    )
                                                )
                                                iconFile.delete()
                                                isPublishing = false
                                                uploadProgress = 0f
                                                uploadStatus = summary
                                                return@launch
                                            }
                                        }
                                        iconFile.delete()
                                    } else {
                                        val summary = Strings.iconReadFailed
                                        publishFailureReport = buildSheetFailureReport(
                                            title = Strings.modulePublishFailed,
                                            stage = Strings.readIconStage,
                                            summary = summary,
                                            contextLines = listOf(
                                                "module=${selectedModule?.name ?: "manual"}",
                                                "iconUri=${iconUri}",
                                                "name=$name"
                                            )
                                        )
                                        isPublishing = false
                                        uploadProgress = 0f
                                        uploadStatus = summary
                                        return@launch
                                    }
                                }

                                // Step 2: Publish module info
                                uploadStatus = Strings.publishingModule
                                uploadProgress = 0.8f

                                try {
                                    val result = apiClient.publishModule(
                                        name = name,
                                        description = description,
                                        icon = finalIconUrl,
                                        category = selectedCategory,
                                        tags = tags.ifBlank { null },
                                        versionName = versionName.ifBlank { null },
                                        versionCode = versionCode,
                                        shareCode = shareCode
                                    )
                                    uploadProgress = 1f
                                    when (result) {
                                        is com.webtoapp.core.auth.AuthResult.Success -> {
                                            snackbarHostState.showSnackbar(Strings.storeModulePublishSuccess)
                                            onPublished()
                                        }
                                        is com.webtoapp.core.auth.AuthResult.Error -> {
                                            val summary = Strings.publishFailed.format(result.message)
                                            publishFailureReport = buildSheetFailureReport(
                                                title = Strings.modulePublishFailed,
                                                stage = Strings.submitModuleInfoStage,
                                                summary = summary,
                                                contextLines = listOf(
                                                    "module=${selectedModule?.name ?: "manual"}",
                                                    "name=$name",
                                                    "versionName=$versionName",
                                                    "versionCode=$versionCode",
                                                    "selectedCategory=$selectedCategory"
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    val summary = Strings.networkError.format(e.message)
                                    publishFailureReport = buildSheetFailureReport(
                                        title = Strings.modulePublishFailed,
                                        stage = Strings.requestPublishStage,
                                        summary = summary,
                                        contextLines = listOf(
                                            "module=${selectedModule?.name ?: "manual"}",
                                            "name=$name",
                                            "versionName=$versionName",
                                            "versionCode=$versionCode"
                                        ),
                                        throwable = e
                                    )
                                } finally {
                                    isPublishing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.publishing, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Outlined.Publish, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.storePublishModule, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    publishFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { publishFailureReport = null }
        )
    }
}
