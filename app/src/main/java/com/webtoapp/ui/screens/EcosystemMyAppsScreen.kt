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
import com.webtoapp.ui.components.ApkExportPreflightPanel
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.screens.ecosystem.AnimatedCounter
import com.webtoapp.ui.screens.ecosystem.Avatar
import com.webtoapp.ui.screens.ecosystem.EcosystemMotion
import com.webtoapp.ui.screens.ecosystem.GlassDivider
import com.webtoapp.ui.screens.ecosystem.LikeBurstEffect
import com.webtoapp.ui.screens.ecosystem.ModuleCard
import com.webtoapp.ui.screens.ecosystem.StaggeredItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EcosystemMyAppsSheet(
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

    appToDelete?.let { app ->
        ItemDeleteConfirmDialog(
            itemName = app.name,
            itemKind = Strings.ecosystemPublishApp,
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


    managedApp?.let { app ->
        EcosystemAppManagementSheet(
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

            MyPublishedItemsHeader(
                title = Strings.ecosystemMyApps,
                isRefreshing = isRefreshing,
                onRefresh = { loadMyApps(showRefresh = true) }
            ) {
                Surface(
                    shape = RoundedCornerShape(WtaRadius.Button),
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
                    shape = RoundedCornerShape(WtaRadius.Button),
                    color = when (tier.lowercase()) {
                        "ultra" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
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
                            "ultra" -> MaterialTheme.colorScheme.tertiary
                            "pro" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        letterSpacing = 0.5.sp
                    )
                }
            }


            if (!isLoading && myApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                StatsOverviewRow(
                    totalDownloads = totalDownloads,
                    avgRating = avgRating,
                    totalLikes = totalLikes,
                    downloadLabel = Strings.ecosystemDownload
                )
            }

            Spacer(modifier = Modifier.height(12.dp))


            if (isLoading) {
                PublishedItemLoadingState(Strings.myApps)
            } else if (errorMsg != null) {
                PublishedItemErrorState(errorMsg) { loadMyApps() }
            } else if (myApps.isEmpty()) {
                PublishedItemEmptyState(
                    icon = Icons.Outlined.RocketLaunch,
                    title = Strings.ecosystemMyApps,
                    subtitle = Strings.ecosystemPublishAppSubtitle,
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
                            shape = RoundedCornerShape(WtaRadius.Card),
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

                                    Surface(
                                        shape = RoundedCornerShape(WtaRadius.Control),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        if (!app.icon.isNullOrBlank()) {
                                            AsyncImage(
                                                model = app.icon,
                                                contentDescription = app.name,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(WtaRadius.Control)),
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

                                            CategoryTag(
                                                label = categoryLabels[app.category] ?: app.category,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }

                                        PublishedItemStatsPills(
                                            versionName = app.versionName,
                                            downloads = app.downloads,
                                            rating = app.rating,
                                            ratingCount = app.ratingCount,
                                            likeCount = app.likeCount
                                        )
                                    }

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









data class MgmtGradients(
    val blue: List<Color>,
    val green: List<Color>,
    val orange: List<Color>,
    val red: List<Color>,
    val purple: List<Color>
)

@Composable
internal fun rememberMgmtGradients(): MgmtGradients {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        MgmtGradients(
            blue = listOf(scheme.primary, scheme.tertiary),
            green = listOf(scheme.tertiary, scheme.primary),
            orange = listOf(scheme.secondary, scheme.tertiary),
            red = listOf(scheme.error, scheme.secondary),
            purple = listOf(scheme.primaryContainer, scheme.secondaryContainer)
        )
    }
}

@Composable
private fun rememberModuleCategoryColors(): Map<String, Color> {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        mapOf(
            "UI_ENHANCE" to scheme.primary,
            "MEDIA" to scheme.secondary,
            "PRIVACY" to scheme.tertiary,
            "TOOLS" to scheme.primary,
            "AD_BLOCK" to scheme.error,
            "SOCIAL" to scheme.secondary,
            "DEVELOPER" to scheme.tertiary,
            "OTHER" to scheme.onSurfaceVariant
        )
    }
}



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
    var actionFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }
    var managedModule by remember { mutableStateOf<StoreModuleInfo?>(null) }

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

    val moduleCategoryColors = rememberModuleCategoryColors()


    moduleToDelete?.let { module ->
        ItemDeleteConfirmDialog(
            itemName = module.name,
            itemKind = Strings.ecosystemPublishModule,
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

    managedModule?.let { module ->
        EcosystemModuleManagementSheet(
            module = module,
            apiClient = apiClient,
            onDismiss = { managedModule = null }
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

            MyPublishedItemsHeader(
                title = Strings.ecosystemMyModules,
                isRefreshing = isRefreshing,
                onRefresh = { loadMyModules(showRefresh = true) }
            ) {
                Surface(
                    shape = RoundedCornerShape(WtaRadius.Button),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        Strings.extensionsCount.format(myModules.size),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }


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


            if (isLoading) {
                PublishedItemLoadingState(Strings.loadingMyModules)
            } else if (errorMsg != null) {
                PublishedItemErrorState(errorMsg) { loadMyModules() }
            } else if (myModules.isEmpty()) {
                PublishedItemEmptyState(
                    icon = Icons.Outlined.Extension,
                    title = Strings.ecosystemNoPublishedModules,
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
                        val catColor = moduleCategoryColors[module.category] ?: MaterialTheme.colorScheme.onSurfaceVariant
                        EnhancedElevatedCard(
                            shape = RoundedCornerShape(WtaRadius.Card),
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

                                    Surface(
                                        shape = RoundedCornerShape(WtaRadius.Control),
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

                                            module.category?.let { cat ->
                                                CategoryTag(
                                                    label = moduleCategoryLabels[cat] ?: cat,
                                                    color = catColor
                                                )
                                            }
                                        }

                                        PublishedItemStatsPills(
                                            versionName = module.versionName,
                                            downloads = module.downloads,
                                            rating = module.rating,
                                            ratingCount = module.ratingCount,
                                            likeCount = module.likeCount
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        IconButton(
                                            onClick = { managedModule = module },
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                Icons.Outlined.Settings, null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

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
