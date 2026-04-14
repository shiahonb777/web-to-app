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
internal fun ModuleStoreDetailSheet(
    module: StoreModuleInfo,
    apiClient: CloudApiClient,
    installedTracker: com.webtoapp.core.cloud.InstalledItemsTracker,
    onDismiss: () -> Unit,
    onInstallWithCallback: (onComplete: (success: Boolean, message: String) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Like state
    var isLiked by remember { mutableStateOf(false) }
    var currentLikeCount by remember { mutableIntStateOf(module.likeCount) }
    var isLiking by remember { mutableStateOf(false) }

    // Review state
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviews by remember { mutableStateOf<List<com.webtoapp.core.cloud.AppReviewItem>>(emptyList()) }
    var reviewsTotal by remember { mutableIntStateOf(0) }
    var isLoadingReviews by remember { mutableStateOf(false) }

    // Report state
    var showReportDialog by remember { mutableStateOf(false) }

    // Download / Install state
    var isDownloading by remember { mutableStateOf(false) }
    var isInstalled by remember { mutableStateOf(installedTracker.isInstalled(module.id)) }

    LaunchedEffect(module.id) {
        when (val likeResult = apiClient.getModuleLikeStatus(module.id)) {
            is com.webtoapp.core.auth.AuthResult.Success -> isLiked = likeResult.data
            else -> {}
        }
        isLoadingReviews = true
        apiClient.getModuleReviews(module.id, page = 1, size = 5).onSuccess { resp ->
            reviews = resp.reviews; reviewsTotal = resp.total
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
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Header ──
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.Transparent, modifier = Modifier.size(56.dp)) {
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Extension, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(module.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                if (module.isFeatured) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.Verified, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                            }
                            Text("by ${module.authorName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (!module.description.isNullOrBlank()) { Text(module.description, fontSize = 15.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)); Spacer(Modifier.height(10.dp)) }
                    if (module.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            module.tags.take(4).forEach { tag -> Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) { Text("#$tag", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) } }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(buildString { module.versionName?.let { append("v$it  ·  ") }; append("${module.downloads} downloads  ·  ${module.ratingCount} ratings") }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (isDownloading || isInstalled) return@Button
                            isDownloading = true
                            onInstallWithCallback { success, msg ->
                                isDownloading = false
                                if (success) {
                                    isInstalled = true
                                    installedTracker.markInstalled(module.id)
                                    scope.launch { snackbarHostState.showSnackbar("✅ " + Strings.storeModuleInstallSuccess) }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("❌ " + msg) }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isDownloading,
                        colors = if (isInstalled) ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ) else ButtonDefaults.buttonColors()
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.storeDownloading, fontWeight = FontWeight.SemiBold)
                        } else if (isInstalled) {
                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Strings.storeInstalled, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(Strings.moduleStoreInstall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Physics action bar ──
            item { Spacer(Modifier.height(8.dp)) }
            item { GlassDivider() }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AppPhysicsActionButton(icon = Icons.Outlined.ChatBubbleOutline, activeIcon = Icons.Filled.ChatBubble, count = reviewsTotal, isActive = false, activeColor = MaterialTheme.colorScheme.primary, onClick = { showReviewDialog = true })
                    AppPhysicsActionButton(icon = Icons.Outlined.ThumbUp, activeIcon = Icons.Filled.ThumbUp, count = currentLikeCount, isActive = isLiked, activeColor = Color(0xFF4CAF50), onClick = {
                        if (isLiking) return@AppPhysicsActionButton; isLiking = true
                        scope.launch {
                            when (val r = apiClient.likeStoreModule(module.id)) { is com.webtoapp.core.auth.AuthResult.Success -> { isLiked = r.data.liked; currentLikeCount = r.data.likeCount }; is com.webtoapp.core.auth.AuthResult.Error -> snackbarHostState.showSnackbar(r.message) }
                            isLiking = false
                        }
                    })
                    AppPhysicsActionButton(icon = Icons.Outlined.Share, activeIcon = Icons.Filled.Share, count = null, isActive = false, activeColor = MaterialTheme.colorScheme.primary, onClick = { })
                    AppPhysicsActionButton(icon = Icons.Outlined.Flag, activeIcon = Icons.Filled.Flag, count = null, isActive = false, activeColor = Color(0xFFEF5350), onClick = { showReportDialog = true })
                }
            }
            item { GlassDivider() }

            // ── Reviews ──
            if (reviews.isEmpty() && !isLoadingReviews) {
                item { Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(Strings.storeNoReviewsYet, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(2.dp)); Text(Strings.storeBeFirstToReview, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) } } }
            }
            items(reviews.size) { index ->
                val review = reviews[index]
                StaggeredItem(index = index) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Avatar(name = review.authorName, size = 36)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f, true)) {
                            Text(review.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            val metaParts = mutableListOf<String>()
                            review.deviceModel?.let { metaParts.add(it) }; review.ipAddress?.let { metaParts.add("IP $it") }; review.createdAt?.let { metaParts.add(it.take(16).replace("T", " ")) }
                            if (metaParts.isNotEmpty()) { Text(metaParts.joinToString("  ·  "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp)) }
                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.padding(vertical = 2.dp)) { repeat(5) { i -> Icon(if (i < review.rating) Icons.Filled.Star else Icons.Outlined.StarBorder, null, Modifier.size(13.dp), tint = if (i < review.rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)) } }
                            if (!review.comment.isNullOrBlank()) { Text(review.comment, fontSize = 15.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)) }
                        }
                    }
                }
                GlassDivider(Modifier.padding(start = 62.dp))
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
        } // Scaffold
    }

    // Report dialog
    if (showReportDialog) {
        ReportDialog(appName = Strings.storeReportAppTitle + " \"" + module.name + "\"", onDismiss = { showReportDialog = false }, onSubmit = { reason, _ ->
            scope.launch {
                when (val r = apiClient.reportStoreModule(module.id, reason)) { is com.webtoapp.core.auth.AuthResult.Success -> snackbarHostState.showSnackbar(Strings.storeReportSuccess); is com.webtoapp.core.auth.AuthResult.Error -> snackbarHostState.showSnackbar(Strings.storeReportFailed + ": ${r.message}") }
                showReportDialog = false
            }
        })
    }

    // Review dialog
    if (showReviewDialog) {
        var reviewRating by remember { mutableIntStateOf(5) }
        var reviewComment by remember { mutableStateOf("") }
        var isSubmittingReview by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isSubmittingReview) showReviewDialog = false },
            icon = { Surface(shape = RoundedCornerShape(16.dp), color = Color.Transparent, modifier = Modifier.size(48.dp)) { Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.RateReview, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.tertiary) } } },
            title = { Text("\"" + Strings.storeReviewSubmitTitle + " \"" + module.name + "\"", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.storeReviewRatingLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star -> IconButton(onClick = { reviewRating = star }, modifier = Modifier.size(36.dp)) { Icon(if (star <= reviewRating) Icons.Filled.Star else Icons.Outlined.StarBorder, "Star $star", tint = if (star <= reviewRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(28.dp)) } }
                        }
                    }
                    OutlinedTextField(value = reviewComment, onValueChange = { reviewComment = it }, label = { Text(Strings.storeReviewCommentLabel) }, placeholder = { Text(Strings.storeReviewPlaceholder) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    isSubmittingReview = true
                    scope.launch {
                        when (val result = apiClient.reviewStoreModule(module.id, reviewRating, reviewComment.ifBlank { null })) {
                            is com.webtoapp.core.auth.AuthResult.Success -> { isSubmittingReview = false; showReviewDialog = false; apiClient.getModuleReviews(module.id, page = 1, size = 5).onSuccess { reviews = it.reviews; reviewsTotal = it.total }; snackbarHostState.showSnackbar(Strings.storeReviewSuccess) }
                            is com.webtoapp.core.auth.AuthResult.Error -> { isSubmittingReview = false; snackbarHostState.showSnackbar(Strings.storeReviewFailed + ": \${result.message}") }
                        }
                    }
                }, enabled = !isSubmittingReview, shape = RoundedCornerShape(10.dp)) {
                    if (isSubmittingReview) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(8.dp)) }
                    Text(Strings.storeReviewSubmit)
                }
            },
            dismissButton = { TextButton(onClick = { showReviewDialog = false }, enabled = !isSubmittingReview) { Text(Strings.storeReviewCancel) } }
        )
    }
}


// ════════════════════════════════════════════════
// modulelistcard
// ════════════════════════════════════════════════
