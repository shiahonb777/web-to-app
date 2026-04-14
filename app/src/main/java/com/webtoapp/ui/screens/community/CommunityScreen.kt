package com.webtoapp.ui.screens.community

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.*
import com.webtoapp.core.auth.TokenManager
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.UserTitleBadges
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import com.webtoapp.ui.viewmodel.CommunityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToUser: (Int) -> Unit,
    onNavigateToModule: (Int) -> Unit,
    onNavigateToPost: (Int) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    isTabVisible: Boolean = true
) {
    val apiClient: CloudApiClient = koinInject()
    val tokenManager: TokenManager = koinInject()
    val communityViewModel: CommunityViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Feed state — from ViewModel (survives config changes)
    val posts by communityViewModel.posts.collectAsState()
    val isLoading by communityViewModel.feedLoading.collectAsState()
    val isLoadingMore by communityViewModel.feedLoadingMore.collectAsState()
    val isRefreshing by communityViewModel.feedRefreshing.collectAsState()
    val selectedTag by communityViewModel.selectedTag.collectAsState()
    val unreadCount by communityViewModel.unreadCount.collectAsState()
    val selectedTab by communityViewModel.selectedTab.collectAsState()
    val discoverData by communityViewModel.discoverData.collectAsState()
    val discoverLoading by communityViewModel.discoverLoading.collectAsState()
    val followingPosts by communityViewModel.followingPosts.collectAsState()
    val followingLoading by communityViewModel.followingLoading.collectAsState()
    var showCreatePost by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val discoverListState = rememberLazyListState()
    val availableTags = remember { listOf(
        "html", "css", "javascript", "typescript", "vue", "react", "angular",
        "svelte", "nextjs", "nuxtjs", "nodejs", "python", "php", "go",
        "webtoapp", "pwa", "responsive", "animation", "game", "tool",
        "新闻阅读", "视频播放", "社交工具", "办公效率", "学习教育",
        "游戏娱乐", "生活服务", "购物比价", "隐私安全", "系统工具",
        "广告拦截", "暗黑模式", "离线使用", "视频下载", "自定义图标",
        "启动画面", "后台运行", "多语言", "脱壳包装", "模块扩展",
    ) }

    // Phase 1 v2: 3 tab definitions
    val tabs = remember { listOf(
        "discover" to AppStringsProvider.current().communityTabDiscover,
        "following" to AppStringsProvider.current().communityTabFollowing,
        "feed" to AppStringsProvider.current().communityTabFeed,
    ) }

    // ViewModel message → snackbar
    val vmMessage by communityViewModel.message.collectAsState()
    LaunchedEffect(vmMessage) {
        vmMessage?.let {
            snackbarHostState.showSnackbar(it)
            communityViewModel.clearMessage()
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        communityViewModel.loadDiscover()
        communityViewModel.loadUnreadCount()
    }

    // P3 #16: Detect scroll to bottom → load more
    val lastVisibleIndex by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(lastVisibleIndex) {
        if (lastVisibleIndex >= posts.size - 3 && !isLoadingMore && posts.isNotEmpty()) {
            communityViewModel.loadMorePosts()
        }
    }

    // P3 #17: Periodic heartbeat (every 2 minutes) — only when tab is visible
    LaunchedEffect(isTabVisible) {
        if (!isTabVisible) return@LaunchedEffect
        while (true) {
            try { apiClient.sendHeartbeat() } catch (_: Exception) {}
            kotlinx.coroutines.delay(120_000L)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                // ─── Top row: title + actions ───
                TopAppBar(
                    title = {
                        Text(
                            AppStringsProvider.current().tabCommunity,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSearchSheet = true }) {
                            Icon(Icons.Outlined.Search, AppStringsProvider.current().communitySearch, Modifier.size(21.dp))
                        }
                        IconButton(onClick = { onNavigateToNotifications() }) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text(if (unreadCount > 99) "99+" else "$unreadCount", fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.Notifications, AppStringsProvider.current().communityNotifications, Modifier.size(21.dp))
                            }
                        }
                        IconButton(onClick = { onNavigateToFavorites() }) {
                            Icon(Icons.Outlined.BookmarkBorder, AppStringsProvider.current().communityBookmarks, Modifier.size(21.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                )
                // ─── Phase 1 v2: 3-Tab Bar ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tabs.forEach { (key, label) ->
                        val isSelected = selectedTab == key
                        Surface(
                            onClick = { communityViewModel.setSelectedTab(key) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                            border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (tokenManager.isLoggedIn()) {
                        showCreatePost = true
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(AppStringsProvider.current().communityLoginToPost) }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Outlined.Edit, AppStringsProvider.current().communityCreatePost)
            }
        }
    ) { padding ->
        // ── Landscape-adaptive: constrain content max-width on wide screens ──
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val contentModifier = if (isLandscape) {
            Modifier.fillMaxSize().padding(padding)
        } else {
            Modifier.fillMaxSize().padding(padding)
        }
        // Wrapper for centering content in landscape with max-width
        val lazyColumnModifier = if (isLandscape) {
            Modifier.widthIn(max = 640.dp).fillMaxHeight()
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = contentModifier,
            contentAlignment = if (isLandscape) Alignment.TopCenter else Alignment.TopStart
        ) {
        Column(
            modifier = lazyColumnModifier
        ) {
            // ═══ Tab Content ═══
            when (selectedTab) {
                "discover" -> {
                    // ─── Discover Tab: Sectioned Content ───
                    if (discoverLoading && discoverData == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = discoverListState
                            ) {
                                // ── Section 1: Featured Showcases (horizontal scroll) ──
                                val showcases = discoverData?.featuredShowcases ?: emptyList()
                                if (showcases.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            icon = Icons.Filled.AutoAwesome,
                                            title = AppStringsProvider.current().communitySectionFeatured,
                                            color = Color(0xFFFFB300)
                                        )
                                    }
                                    item {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(showcases, key = { it.id }) { post ->
                                                ShowcaseCard(
                                                    post = post,
                                                    onClick = { onNavigateToPost(post.id) },
                                                    onImportRecipe = {
                                                        communityViewModel.importRecipe(post.id) { /* recipe result */ }
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                // ── Section 2: Trending ──
                                val trending = discoverData?.trending ?: emptyList()
                                if (trending.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                                            title = AppStringsProvider.current().communitySectionHot,
                                            color = Color(0xFFE91E63)
                                        )
                                    }
                                    itemsIndexed(trending, key = { _, p -> "t_${p.id}" }) { index, post ->
                                        PostCard(
                                            post = post,
                                            onPostClick = { onNavigateToPost(post.id) },
                                            onLike = { communityViewModel.togglePostLike(post.id) },
                                            onShare = { communityViewModel.sharePost(post.id) },
                                            onComment = { onNavigateToPost(post.id) },
                                            onReport = { communityViewModel.reportPost(post.id) },
                                            onAuthorClick = { onNavigateToUser(post.authorId) },
                                            onAppLinkClick = { moduleId -> onNavigateToModule(moduleId) },
                                            onMentionClick = { username ->
                                                scope.launch {
                                                    communityViewModel.resolveUserByUsername(username) { userId ->
                                                        onNavigateToUser(userId)
                                                    }
                                                }
                                            },
                                            onImportRecipe = if (post.hasRecipe) {
                                                { communityViewModel.importRecipe(post.id) { } }
                                            } else null,
                                        )
                                        if (index < trending.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }

                                // ── Section 3: Latest Tutorials ──
                                val tutorials = discoverData?.latestTutorials ?: emptyList()
                                if (tutorials.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            icon = Icons.AutoMirrored.Filled.MenuBook,
                                            title = AppStringsProvider.current().communitySectionTutorials,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                    items(tutorials, key = { "tut_${it.id}" }) { post ->
                                        TutorialCard(
                                            post = post,
                                            onClick = { onNavigateToPost(post.id) }
                                        )
                                    }
                                }

                                // ── Section 4: Unanswered Questions ──
                                val questions = discoverData?.unansweredQuestions ?: emptyList()
                                if (questions.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                                            title = AppStringsProvider.current().communitySectionQuestions,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                    items(questions, key = { "q_${it.id}" }) { post ->
                                        QuestionCard(
                                            post = post,
                                            onClick = { onNavigateToPost(post.id) }
                                        )
                                    }
                                }

                                // Empty state
                                if (showcases.isEmpty() && trending.isEmpty() && tutorials.isEmpty() && questions.isEmpty()) {
                                    item {
                                        Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Outlined.Explore, null, Modifier.size(56.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                                Spacer(Modifier.height(12.dp))
                                                Text(AppStringsProvider.current().communityEmptyDiscover, fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }

                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                    }
                }

                "following" -> {
                    // ─── Following Tab: Posts from followed users ───
                    if (!tokenManager.isLoggedIn()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.PersonAdd, null, Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(Modifier.height(12.dp))
                                Text(AppStringsProvider.current().communityEmptyFollowing, fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    } else if (followingLoading && followingPosts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
                        }
                    } else if (followingPosts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Explore, null, Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(Modifier.height(12.dp))
                                Text(AppStringsProvider.current().communityEmptyFollowingNotFollowing, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(AppStringsProvider.current().communityEmptyFollowingSuggestion, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(16.dp))
                                FilledTonalButton(onClick = { communityViewModel.setSelectedTab("discover") }) {
                                    Text(AppStringsProvider.current().communityGoDiscover)
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(followingPosts, key = { _, p -> "fol_${p.id}" }) { index, post ->
                                    PostCard(
                                        post = post,
                                        onPostClick = { onNavigateToPost(post.id) },
                                        onLike = { communityViewModel.togglePostLike(post.id) },
                                        onShare = { communityViewModel.sharePost(post.id) },
                                        onComment = { onNavigateToPost(post.id) },
                                        onReport = { communityViewModel.reportPost(post.id) },
                                        onAuthorClick = { onNavigateToUser(post.authorId) },
                                        onAppLinkClick = { moduleId -> onNavigateToModule(moduleId) },
                                        onMentionClick = { username ->
                                            scope.launch {
                                                communityViewModel.resolveUserByUsername(username) { userId ->
                                                    onNavigateToUser(userId)
                                                }
                                            }
                                        },
                                        onImportRecipe = if (post.hasRecipe) {
                                            { communityViewModel.importRecipe(post.id) { } }
                                        } else null,
                                    )
                                    if (index < followingPosts.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }                        }
                    }
                }

                else -> {
                    // Feed Tab( )
                    // Tag Filter Bar
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { communityViewModel.setSelectedTag(null) },
                                label = { Text(AppStringsProvider.current().communityAllTags, fontSize = 12.sp) },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        items(availableTags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { communityViewModel.setSelectedTag(if (selectedTag == tag) null else tag) },
                                label = { Text(tag, fontSize = 12.sp) },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp),
                                leadingIcon = if (selectedTag == tag) {
                                    { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    // Feed content
                    if (isLoading && posts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
                        }
                    } else if (posts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Forum, null, Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(Modifier.height(12.dp))
                                Text(AppStringsProvider.current().communityNoPosts, fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState
                            ) {
                                itemsIndexed(posts, key = { _, p -> p.id }, contentType = { _, _ -> "post" }) { index, post ->
                                    PostCard(
                                        post = post,
                                        onPostClick = { onNavigateToPost(post.id) },
                                        onLike = { communityViewModel.togglePostLike(post.id) },
                                        onShare = { communityViewModel.sharePost(post.id) },
                                        onComment = { onNavigateToPost(post.id) },
                                        onReport = { communityViewModel.reportPost(post.id) },
                                        onAuthorClick = { onNavigateToUser(post.authorId) },
                                        onAppLinkClick = { moduleId -> onNavigateToModule(moduleId) },
                                        onMentionClick = { username ->
                                            scope.launch {
                                                communityViewModel.resolveUserByUsername(username) { userId ->
                                                    onNavigateToUser(userId)
                                                }
                                            }
                                        },
                                        onImportRecipe = if (post.hasRecipe) {
                                            { communityViewModel.importRecipe(post.id) { } }
                                        } else null,
                                        modifier = Modifier.animateItem()
                                    )
                                    if (index < posts.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                                if (isLoadingMore) {
                                    item {
                                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }
        }
        } // Box wrapper
    }

    // ─── Create Post Sheet ───
    if (showCreatePost) {
        CreatePostSheet(
            apiClient = apiClient,
            availableTags = availableTags,
            onDismiss = { showCreatePost = false },
            onPosted = {
                showCreatePost = false
                communityViewModel.refreshPosts()
                scope.launch { snackbarHostState.showSnackbar(AppStringsProvider.current().communityPostSuccess) }
            }
        )
    }

    // ─── Search Sheet (Users + Posts) ───
    if (showSearchSheet) {
        SearchUsersSheet(
            communityViewModel = communityViewModel,
            onDismiss = { showSearchSheet = false },
            onUserClick = { userId ->
                showSearchSheet = false
                onNavigateToUser(userId)
            },
            onPostClick = { postId ->
                showSearchSheet = false
                onNavigateToPost(postId)
            }
        )
    }
}
// ═══════════════════════════════════════════
// Create Post Sheet (P3 #18: Media upload)
// ═══════════════════════════════════════════

data class PendingMedia(
    val uri: android.net.Uri,
    val uploading: Boolean = false,
    val progress: Float = 0f,
    val uploadedUrl: String? = null,
    val error: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostSheet(
    apiClient: CloudApiClient,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onPosted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isPublishing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Phase 1 v2: Post type state
    var selectedPostType by remember { mutableStateOf("discussion") }
    var appName by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("website") }
    var projectRecipe by remember { mutableStateOf("") }
    var tutorialTitle by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("beginner") }
    var questionTitle by remember { mutableStateOf("") }

    // Store apps for linking
    var storeApps by remember { mutableStateOf<List<StoreModuleInfo>>(emptyList()) }
    var selectedAppLinks by remember { mutableStateOf<List<PostAppLinkInput>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }

    // P3 #18: Media state
    var pendingMedia by remember { mutableStateOf<List<PendingMedia>>(emptyList()) }

    // Image picker launcher
    val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        val newMedia = uris.map { PendingMedia(uri = it) }
        pendingMedia = (pendingMedia + newMedia).take(9)
        // Auto-upload each
        newMedia.forEach { media ->
            scope.launch {
                pendingMedia = pendingMedia.map { if (it.uri == media.uri) it.copy(uploading = true) else it }
                try {
                    val tempFile = java.io.File.createTempFile("post_media_", ".jpg", context.cacheDir)
                    context.contentResolver.openInputStream(media.uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val result = apiClient.uploadAsset(
                        file = tempFile, contentType = "image/jpeg",
                        onProgress = { p ->
                            pendingMedia = pendingMedia.map {
                                if (it.uri == media.uri) it.copy(progress = p) else it
                            }
                        }
                    )
                    when (result) {
                        is AuthResult.Success -> pendingMedia = pendingMedia.map {
                            if (it.uri == media.uri) it.copy(uploading = false, uploadedUrl = result.data, progress = 1f) else it
                        }
                        is AuthResult.Error -> pendingMedia = pendingMedia.map {
                            if (it.uri == media.uri) it.copy(uploading = false, error = true) else it
                        }
                    }
                    tempFile.delete()
                } catch (_: Exception) {
                    pendingMedia = pendingMedia.map {
                        if (it.uri == media.uri) it.copy(uploading = false, error = true) else it
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        when (val result = apiClient.listStoreModules(page = 1, size = 100)) {
            is AuthResult.Success -> storeApps = result.data.first
            is AuthResult.Error -> { /* silent */ }
        }
    }

    // Post type definitions
    val postTypes = remember { listOf(
        Triple("discussion", AppStringsProvider.current().communityPostTypeDiscussion, Color(0xFF9E9E9E)),
        Triple("showcase", AppStringsProvider.current().communityPostTypeShowcase, Color(0xFF6C5CE7)),
        Triple("tutorial", AppStringsProvider.current().communityPostTypeTutorial, Color(0xFF4CAF50)),
        Triple("question", AppStringsProvider.current().communityPostTypeQuestion, Color(0xFFFF9800)),
    ) }
    val sourceTypes = remember { listOf(
        "website" to AppStringsProvider.current().communitySourceTypeWebsite, "html" to "HTML", "media" to AppStringsProvider.current().communitySourceTypeMedia,
        "frontend" to AppStringsProvider.current().communitySourceTypeFrontend, "server" to AppStringsProvider.current().communitySourceTypeServer,
    ) }
    val difficulties = remember { listOf(
        "beginner" to "🟢 " + AppStringsProvider.current().communityDifficultyBeginner, "intermediate" to "🟡 " + AppStringsProvider.current().communityDifficultyIntermediate, "advanced" to "🔴 " + AppStringsProvider.current().communityDifficultyAdvanced,
    ) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetMaxWidth = 640.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title + Publish
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(AppStringsProvider.current().communityCreatePost, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (content.isBlank()) return@Button
                        if (selectedTags.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar(AppStringsProvider.current().communityTagRequired) }
                            return@Button
                        }
                        if (selectedPostType == "showcase" && appName.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(AppStringsProvider.current().communityEnterAppName) }
                            return@Button
                        }
                        if (selectedPostType == "tutorial" && tutorialTitle.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(AppStringsProvider.current().communityEnterTutorialTitle) }
                            return@Button
                        }
                        if (selectedPostType == "question" && questionTitle.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(AppStringsProvider.current().communityEnterQuestionTitle) }
                            return@Button
                        }
                        val mediaInputs = pendingMedia
                            .filter { it.uploadedUrl != null }
                            .map { PostMediaInput(mediaType = "image", urlGithub = it.uploadedUrl) }
                        scope.launch {
                            isPublishing = true
                            val result = apiClient.createCommunityPost(
                                content = content, tags = selectedTags.toList(),
                                appLinks = selectedAppLinks, media = mediaInputs,
                                postType = selectedPostType,
                                appName = if (selectedPostType == "showcase") appName else null,
                                sourceType = if (selectedPostType == "showcase") sourceType else null,
                                projectRecipe = if (selectedPostType == "showcase" && projectRecipe.isNotBlank()) projectRecipe else null,
                                title = when (selectedPostType) {
                                    "tutorial" -> tutorialTitle
                                    "question" -> questionTitle
                                    else -> null
                                },
                                difficulty = if (selectedPostType == "tutorial") difficulty else null,
                            )
                            when (result) {
                                is AuthResult.Success -> onPosted()
                                is AuthResult.Error -> snackbarHostState.showSnackbar(AppStringsProvider.current().communityPublishFailed)
                            }
                            isPublishing = false
                        }
                    },
                    enabled = content.isNotBlank() && selectedTags.isNotEmpty() && !isPublishing && pendingMedia.none { it.uploading },
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.height(36.dp)
                ) {
                    if (isPublishing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(AppStringsProvider.current().communityPublish, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Phase 1 v2: Post Type Selector ───
            Text(AppStringsProvider.current().communityPostTypeLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                postTypes.forEach { (key, label, color) ->
                    val isSelected = selectedPostType == key
                    Surface(
                        onClick = { selectedPostType = key },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 0.5.dp,
                            if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                            Text(label, fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Type-specific fields ───
            when (selectedPostType) {
                "showcase" -> {
                    OutlinedTextField(
                        value = appName, onValueChange = { appName = it },
                        placeholder = { Text(AppStringsProvider.current().communityAppNamePlaceholder, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Apps, null, Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(AppStringsProvider.current().communitySourceTypeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sourceTypes.forEach { (key, label) ->
                            FilterChip(
                                selected = sourceType == key,
                                onClick = { sourceType = key },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectRecipe, onValueChange = { projectRecipe = it },
                        placeholder = { Text(AppStringsProvider.current().communityRecipeJsonPlaceholder, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(10.dp), maxLines = 4,
                        supportingText = {
                            Text(
                                stringResource(com.webtoapp.R.string.community_recipe_json_supporting),
                                fontSize = 10.sp
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                "tutorial" -> {
                    OutlinedTextField(
                        value = tutorialTitle, onValueChange = { tutorialTitle = it },
                        placeholder = {
                            Text(
                                stringResource(com.webtoapp.R.string.community_tutorial_title_placeholder),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Title, null, Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(com.webtoapp.R.string.community_difficulty_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        difficulties.forEach { (key, label) ->
                            FilterChip(
                                selected = difficulty == key,
                                onClick = { difficulty = key },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                "question" -> {
                    OutlinedTextField(
                        value = questionTitle, onValueChange = { questionTitle = it },
                        placeholder = {
                            Text(
                                stringResource(com.webtoapp.R.string.community_question_title_placeholder),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Content input
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                placeholder = { Text(
                    when (selectedPostType) {
                        "showcase" -> stringResource(com.webtoapp.R.string.community_showcase_content_placeholder)
                        "tutorial" -> stringResource(com.webtoapp.R.string.community_tutorial_content_placeholder)
                        "question" -> stringResource(com.webtoapp.R.string.community_question_content_placeholder)
                        else -> AppStringsProvider.current().communityWhatsNew
                    }, fontSize = 14.sp
                ) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp), maxLines = 12
            )

            // P3 #18: Media preview strip
            if (pendingMedia.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(pendingMedia.size) { index ->
                        val media = pendingMedia[index]
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(media.uri).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (media.uploading) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(progress = { media.progress }, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp, color = Color.White)
                                }
                            }
                            if (media.error) {
                                Box(Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.ErrorOutline, null, Modifier.size(24.dp), tint = Color.White)
                                }
                            }
                            if (media.uploadedUrl != null && !media.uploading) {
                                Surface(shape = CircleShape, color = Color(0xFF4CAF50), modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(16.dp)) {
                                    Icon(Icons.Filled.Check, null, Modifier.padding(2.dp), tint = Color.White)
                                }
                            }
                            IconButton(onClick = { pendingMedia = pendingMedia.filterIndexed { i, _ -> i != index } },
                                modifier = Modifier.align(Alignment.TopEnd).size(20.dp)) {
                                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) {
                                    Icon(Icons.Filled.Close, null, Modifier.padding(2.dp), tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tags — continuous scrollable row
            val maxTags = 3
            Text(
                "${AppStringsProvider.current().communitySelectTags} (${selectedTags.size}/$maxTags)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedTags.size >= maxTags) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(availableTags) { tag ->
                    val isSelected = tag in selectedTags
                    val isDisabled = !isSelected && selectedTags.size >= maxTags
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selectedTags = selectedTags - tag
                            else if (selectedTags.size < maxTags) selectedTags = selectedTags + tag
                            else scope.launch { snackbarHostState.showSnackbar(AppStringsProvider.current().communityTagMaxLimit) }
                        },
                        label = { Text(tag, fontSize = 11.sp,
                            color = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            else Color.Unspecified) },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isDisabled,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tagColor(tag).copy(alpha = 0.15f),
                            selectedLabelColor = tagColor(tag),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        ),
                        leadingIcon = if (isSelected) { { Icon(Icons.Filled.Check, null, Modifier.size(12.dp)) } } else null
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons: Add media + Link app
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { mediaPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(
                        mediaType = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.height(36.dp), enabled = pendingMedia.size < 9
                ) {
                    Icon(Icons.Outlined.Image, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(AppStringsProvider.current().communityAddMedia, fontSize = 12.sp)
                    if (pendingMedia.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("${pendingMedia.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { showAppPicker = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(36.dp)) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(AppStringsProvider.current().communityLinkApp, fontSize = 12.sp)
                    if (selectedAppLinks.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("${selectedAppLinks.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Linked apps preview
            if (selectedAppLinks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                selectedAppLinks.forEach { link ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Apps, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(link.appName ?: "App", fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { selectedAppLinks = selectedAppLinks.filter { it != link } }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Outlined.Close, null, Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SnackbarHost(snackbarHostState)
        }
    }

    // App picker dialog
    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text(AppStringsProvider.current().communityLinkApp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(storeApps) { app ->
                        val isSelected = selectedAppLinks.any { it.storeModuleId == app.id }
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            selectedAppLinks = if (isSelected) selectedAppLinks.filter { it.storeModuleId != app.id }
                            else selectedAppLinks + PostAppLinkInput(linkType = "store", storeModuleId = app.id, appName = app.name, appIcon = app.icon, appDescription = app.description)
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.description ?: "", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    if (storeApps.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(AppStringsProvider.current().communityNoAppsToLink, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        } }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAppPicker = false }) { Text(AppStringsProvider.current().communityConfirm) } }
        )
    }
}

