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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDetailSheet(
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
                    StatPill(Icons.Filled.Star, String.format("%.1f", detail.rating), "${detail.ratingCount} ${AppStringsProvider.current().storeReviews}", modifier = Modifier.weight(1f))
                    StatPill(Icons.Outlined.Download, formatDownloads(detail.downloads), AppStringsProvider.current().storeDownloads, modifier = Modifier.weight(1f))
                    StatPill(Icons.Outlined.ThumbUp, "$currentLikeCount", AppStringsProvider.current().storeLikes, modifier = Modifier.weight(1f))
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
                                            if (isPending) AppStringsProvider.current().storePreparingDownload else AppStringsProvider.current().storeDownloadingLabel,
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
                                    Text(AppStringsProvider.current().storeCancelDownload, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                            Text(AppStringsProvider.current().storeInstallApp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
                                        currentTask?.error ?: AppStringsProvider.current().storeDownloadFailed,
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
                                                    }
                                                }
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
                                    Text(AppStringsProvider.current().storeRedownload, fontWeight = FontWeight.SemiBold)
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
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(AppStringsProvider.current().storeNoDownloadLink)
                                                }
                                            }
                                        }
                                        result.onFailure { e ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(
                                                        com.webtoapp.R.string.store_get_download_link_failed,
                                                        (e.message ?: "")
                                                    )
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                context.getString(
                                                    com.webtoapp.R.string.store_network_error,
                                                    (e.message ?: "")
                                                )
                                            )
                                        }
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
                                Text(AppStringsProvider.current().storeGetDownloadLink, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Filled.Download, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStringsProvider.current().storeDownloadBtn, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
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
                            AppStringsProvider.current().storeScreenshots,
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
                                    contentDescription = AppStringsProvider.current().storeScreenshot,
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
                            AppStringsProvider.current().storeDescription,
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
                        AppStringsProvider.current().storeDeveloperInfo,
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
                        DevInfoRow(Icons.Outlined.Email, AppStringsProvider.current().storeEmail, it)
                    }
                    detail.websiteUrl?.let {
                        DevInfoRow(Icons.Outlined.Language, AppStringsProvider.current().storeWebsite, it)
                    }
                    detail.groupChatUrl?.let {
                        DevInfoRow(Icons.Outlined.Groups, AppStringsProvider.current().storeGroupChat, it)
                    }
                    detail.privacyPolicyUrl?.let {
                        DevInfoRow(Icons.Outlined.Security, AppStringsProvider.current().storePrivacyPolicy, it)
                    }
                    detail.contactPhone?.let {
                        DevInfoRow(Icons.Outlined.Phone, AppStringsProvider.current().storePhone, it)
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
                                            snackbarHostState.showSnackbar(result.message)
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
                            Text(AppStringsProvider.current().storeNoReviewsYet, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text(AppStringsProvider.current().storeBeFirstToReview, fontSize = 14.sp,
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
                            snackbarHostState.showSnackbar(AppStringsProvider.current().storeReportSuccess)
                        }
                        is com.webtoapp.core.auth.AuthResult.Error -> {
                            snackbarHostState.showSnackbar(
                                context.getString(
                                    com.webtoapp.R.string.store_report_failed,
                                    result.message
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
            title = { Text("\"" + AppStringsProvider.current().storeReviewSubmitTitle + " \"" + detail.name + "\"", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Star rating
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(AppStringsProvider.current().storeReviewRatingLabel, style = MaterialTheme.typography.labelMedium,
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
                        label = { Text(AppStringsProvider.current().storeReviewCommentLabel) },
                        placeholder = { Text(AppStringsProvider.current().storeReviewPlaceholder) },
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
                                    snackbarHostState.showSnackbar(AppStringsProvider.current().storeReviewSuccess)
                                }
                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                    isSubmittingReview = false
                                    snackbarHostState.showSnackbar(AppStringsProvider.current().storeReviewFailed + ": \${result.message}")
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
                    Text(AppStringsProvider.current().storeReviewSubmit)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReviewDialog = false },
                    enabled = !isSubmittingReview
                ) { Text(AppStringsProvider.current().storeReviewCancel) }
            }
        )
    }
}

/**
 * dialog
 */
@Composable
internal fun ReportDialog(
    appName: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, description: String?) -> Unit
) {
    val reasons = listOf(
        "spam" to AppStringsProvider.current().storeReportReasonSpam,
        "malicious" to AppStringsProvider.current().storeReportReasonMalicious,
        "inappropriate" to AppStringsProvider.current().storeReportReasonInappropriate,
        "copyright" to AppStringsProvider.current().storeReportReasonCopyright,
        "other" to AppStringsProvider.current().storeReportReasonOther
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
        title = { Text("举报「$appName」", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStringsProvider.current().storeReportSelectReason,
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
                    label = { Text(AppStringsProvider.current().storeReportDescOptional) },
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
                Text(AppStringsProvider.current().storeReportSubmit)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(AppStringsProvider.current().storeReviewCancel)
            }
        }
    )
}

/**
 * Twitter/X spring button- appstore
 */
@Composable
internal fun AppPhysicsActionButton(
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
internal fun StatPill(
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
internal fun DevInfoRow(
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


internal fun formatDownloads(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}


// ════════════════════════════════════════════════
// ════════════════════════════════════════════════
// downloadmanagement Bottom Sheet( unified: download + downloadapp)
// ════════════════════════════════════════════════
