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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.webtoapp.core.i18n.AppStringsProvider
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

/**
 * unifiedmarket- appstore + modulemarket
 * Top tab switch: "app" and "module"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppStoreScreen(
    cloudViewModel: CloudViewModel,
    apiClient: CloudApiClient,
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    installedTracker: com.webtoapp.core.cloud.InstalledItemsTracker,
    onInstallModule: (String) -> Unit,
    downloadManager: AppDownloadManager? = null
) {
    val scope = rememberCoroutineScope()

    // Pager state
    val pagerState = rememberPagerState(pageCount = { 2 })

    // state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Note
    var showMenu by remember { mutableStateOf(false) }
    var showDownloadManager by remember { mutableStateOf(false) }
    var showMyApps by remember { mutableStateOf(false) }
    var showPublishApp by remember { mutableStateOf(false) }
    // Module-specific dialogs
    var showMyModules by remember { mutableStateOf(false) }
    var showPublishModule by remember { mutableStateOf(false) }

    // download badge
    val emptyTasks = remember { mutableStateOf<Map<Int, AppDownloadManager.DownloadTask>>(emptyMap()) }
    val activeTasks by (downloadManager?.activeTasks?.collectAsState() ?: emptyTasks)
    val activeCount = activeTasks.count {
        it.value.status == AppDownloadManager.DownloadStatus.DOWNLOADING ||
        it.value.status == AppDownloadManager.DownloadStatus.PENDING
    }
    val emptyDownloaded = remember { mutableStateOf<List<AppDownloadManager.DownloadedApp>>(emptyList()) }
    val downloadedAppsList by (downloadManager?.downloadedApps?.collectAsState() ?: emptyDownloaded)
    val downloadedCount = downloadedAppsList.size
    // current Tab
    val tabTitles = listOf(AppStringsProvider.current().marketTabApps, AppStringsProvider.current().marketTabModules)

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
                                    if (pagerState.currentPage == 0) AppStringsProvider.current().storeSearchPlaceholder
                                    else AppStringsProvider.current().moduleStoreSearchPlaceholder
                                )
                            },
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                        // pill- style Tab switch
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
                                        text = { Text(AppStringsProvider.current().storeDownloadManager) },
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
                                        text = { Text(AppStringsProvider.current().storeMyApps) },
                                        onClick = { showMenu = false; showMyApps = true },
                                        leadingIcon = { Icon(Icons.Outlined.Apps, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(AppStringsProvider.current().storePublishApp) },
                                        onClick = { showMenu = false; showPublishApp = true },
                                        leadingIcon = { Icon(Icons.Outlined.Publish, null) }
                                    )
                                } else {
                                    // ── Modules Tab Menu ──
                                    DropdownMenuItem(
                                        text = { Text(AppStringsProvider.current().storeDownloadManager) },
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
                                        text = { Text(AppStringsProvider.current().storeMyModules) },
                                        onClick = { showMenu = false; showMyModules = true },
                                        leadingIcon = { Icon(Icons.Outlined.Extension, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(AppStringsProvider.current().storePublishModule) },
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
                    apiClient = apiClient,
                    installedTracker = installedTracker,
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    onInstallModule = onInstallModule
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
            webAppRepository = webAppRepository,
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
// Tab 1: appmarket
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
        "tools" to AppStringsProvider.current().storeCatTools,
        "social" to AppStringsProvider.current().storeCatSocial,
        "education" to AppStringsProvider.current().storeCatEducation,
        "entertainment" to AppStringsProvider.current().storeCatEntertainment,
        "productivity" to AppStringsProvider.current().storeCatProductivity,
        "lifestyle" to AppStringsProvider.current().storeCatLifestyle,
        "business" to AppStringsProvider.current().storeCatBusiness,
        "news" to AppStringsProvider.current().storeCatNews,
        "finance" to AppStringsProvider.current().storeCatFinance,
        "health" to AppStringsProvider.current().storeCatHealth,
        "other" to AppStringsProvider.current().storeCatOther,
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
        "downloads" to AppStringsProvider.current().storeSortDownloads,
        "rating" to AppStringsProvider.current().storeSortRating,
        "created_at" to AppStringsProvider.current().storeSortNewest,
        "like_count" to AppStringsProvider.current().storeSortLikes,
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
    // when( active query) load
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
        // filter( full- width)
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
                        label = { Text(AppStringsProvider.current().storeAllCategories, fontSize = 12.sp) },
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

        // ( full- width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${AppStringsProvider.current().storeAppsCount}: $totalApps",
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
                                            contentDescription = if (sortOrder == "desc") AppStringsProvider.current().sortDesc else AppStringsProvider.current().sortAsc,
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
                            AppStringsProvider.current().storeLoadingApps,
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
                        AppStringsProvider.current().storeEmpty,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        AppStringsProvider.current().storeNoContentTryAgain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // applist- cards auto- fill grid columns
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
                            AppStringsProvider.current().storeLoadMore,
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

    // app bottomdialog
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
// Tab 2: modulemarket
// ════════════════════════════════════════════════

@Composable
private fun ModulesTabContent(
    cloudViewModel: CloudViewModel,
    apiClient: CloudApiClient,
    installedTracker: com.webtoapp.core.cloud.InstalledItemsTracker,
    searchQuery: String,
    isSearchActive: Boolean,
    onInstallModule: (String) -> Unit
) {
    val modules by cloudViewModel.storeModules.collectAsStateWithLifecycle()
    val loading by cloudViewModel.storeLoading.collectAsStateWithLifecycle()
    val total by cloudViewModel.storeTotal.collectAsStateWithLifecycle()

    // load- - list
    var initialLoaded by remember { mutableStateOf(false) }
    var selectedStoreModule by remember { mutableStateOf<StoreModuleInfo?>(null) }

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSort by rememberSaveable { mutableStateOf("downloads") }
    var sortOrder by rememberSaveable { mutableStateOf("desc") }

    val moduleCategories = listOf(
        "UI_ENHANCE" to AppStringsProvider.current().catUiEnhance,
        "MEDIA" to AppStringsProvider.current().catMedia,
        "PRIVACY" to AppStringsProvider.current().catPrivacySecurity,
        "TOOLS" to AppStringsProvider.current().catTools,
        "AD_BLOCK" to AppStringsProvider.current().catAdBlock,
        "SOCIAL" to AppStringsProvider.current().catSocial,
        "DEVELOPER" to AppStringsProvider.current().catDeveloper,
        "OTHER" to AppStringsProvider.current().catOther
    )

    val moduleSorts = listOf(
        "downloads" to AppStringsProvider.current().moduleStoreSortDownloads,
        "rating" to AppStringsProvider.current().moduleStoreSortRating,
        "created_at" to AppStringsProvider.current().moduleStoreSortNewest,
        "like_count" to AppStringsProvider.current().moduleStoreSortLikes,
    )

    fun loadModules() {
        cloudViewModel.loadStoreModules(
            selectedCategory,
            searchQuery.ifBlank { null },
            selectedSort,
            sortOrder
        )
    }

    // load
    LaunchedEffect(Unit) { loadModules() }
    LaunchedEffect(selectedCategory, selectedSort, sortOrder) { loadModules() }
    LaunchedEffect(searchQuery, isSearchActive) {
        if (!isSearchActive) loadModules()
    }

    // when loading false modules load, load
    LaunchedEffect(loading) {
        if (!loading && !initialLoaded) {
            initialLoaded = true
        }
    }

    // load: loading( loading ViewModel true)
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
                    AppStringsProvider.current().storeLoadingModules,
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
        // ( full- width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PremiumFilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(AppStringsProvider.current().moduleStoreCatAll, fontSize = 12.sp) }
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

        // +( full- width)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$total 个模块",
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
                                    contentDescription = if (sortOrder == "desc") AppStringsProvider.current().sortDesc else AppStringsProvider.current().sortAsc,
                                    modifier = Modifier.size(16.dp)
                                )
                            }} else null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        // switch / load( full- width)
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

        // modulelist- cards auto- fill grid columns
        if (!loading) {
            items(modules, key = { it.id }) { module ->
                ModuleStoreCard(
                    module = module,
                    installedTracker = installedTracker,
                    onClick = { selectedStoreModule = module },
                    onInstall = { selectedStoreModule = module }
                )
            }
        }

        // state( full- width)
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
                        if (searchQuery.isNotBlank()) AppStringsProvider.current().moduleStoreEmptySearch else AppStringsProvider.current().moduleStoreEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        AppStringsProvider.current().storeNoContentForModules,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }

    // ── Module detail sheet ──
    selectedStoreModule?.let { module ->
        ModuleStoreDetailSheet(
            module = module,
            apiClient = apiClient,
            installedTracker = installedTracker,
            onDismiss = { selectedStoreModule = null },
            onInstallWithCallback = { onComplete ->
                cloudViewModel.downloadStoreModule(
                    moduleId = module.id,
                    onResult = { shareCode ->
                        try {
                            onInstallModule(shareCode)
                            onComplete(true, AppStringsProvider.current().storeInstallSuccess)
                        } catch (e: Exception) {
                            onComplete(false, e.message ?: AppStringsProvider.current().storeInstallFailed)
                        }
                    },
                    onError = { errorMsg ->
                        onComplete(false, errorMsg)
                    }
                )
            }
        )
    }
}
