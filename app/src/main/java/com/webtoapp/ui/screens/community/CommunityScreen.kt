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
import com.webtoapp.core.i18n.Strings
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
        "discover" to Strings.communityTabDiscover,
        "following" to Strings.communityTabFollowing,
        "feed" to Strings.communityTabFeed,
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
                            Strings.tabCommunity,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSearchSheet = true }) {
                            Icon(Icons.Outlined.Search, Strings.communitySearch, Modifier.size(21.dp))
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
                                Icon(Icons.Outlined.Notifications, Strings.communityNotifications, Modifier.size(21.dp))
                            }
                        }
                        IconButton(onClick = { onNavigateToFavorites() }) {
                            Icon(Icons.Outlined.BookmarkBorder, Strings.communityBookmarks, Modifier.size(21.dp))
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
                        scope.launch { snackbarHostState.showSnackbar(Strings.communityLoginToPost) }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Outlined.Edit, Strings.communityCreatePost)
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
                                            title = Strings.communitySectionFeatured,
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
                                            icon = Icons.Filled.TrendingUp,
                                            title = Strings.communitySectionHot,
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
                                            icon = Icons.Filled.MenuBook,
                                            title = Strings.communitySectionTutorials,
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
                                            icon = Icons.Filled.HelpOutline,
                                            title = Strings.communitySectionQuestions,
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
                                                Text(Strings.communityEmptyDiscover, fontSize = 14.sp,
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
                                Text(Strings.communityEmptyFollowing, fontSize = 15.sp,
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
                                Text(Strings.communityEmptyFollowingNotFollowing, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(Strings.communityEmptyFollowingSuggestion, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(16.dp))
                                FilledTonalButton(onClick = { communityViewModel.setSelectedTab("discover") }) {
                                    Text(Strings.communityGoDiscover)
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
                    // ─── Feed Tab (广场) ───
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
                                label = { Text(Strings.communityAllTags, fontSize = 12.sp) },
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
                                Text(Strings.communityNoPosts, fontSize = 15.sp,
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
                scope.launch { snackbarHostState.showSnackbar(Strings.communityPostSuccess) }
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
// Post Card
// ═══════════════════════════════════════════

@Composable
private fun PostCard(
    post: CommunityPostItem,
    onPostClick: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit,
    onReport: () -> Unit,
    onAuthorClick: () -> Unit,
    onAppLinkClick: (Int) -> Unit,
    onMentionClick: (String) -> Unit = {},
    onImportRecipe: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().clickable { onPostClick() }.padding(horizontal = 16.dp, vertical = 12.dp)) {
        // ── Author Header ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp).clickable { onAuthorClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (post.authorAvatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(post.authorAvatarUrl).crossfade(true).build(),
                        contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            (post.authorDisplayName ?: post.authorUsername).take(1).uppercase(),
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).clickable { onAuthorClick() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        post.authorDisplayName ?: post.authorUsername,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    UserTitleBadges(
                        isDeveloper = post.authorIsDeveloper,
                        teamBadges = post.authorTeamBadges
                    )
                }
                Text(
                    "@${post.authorUsername} · ${formatTimeAgo(post.createdAt)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            // More menu
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.MoreHoriz, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(Strings.communityReport) },
                        onClick = { showMenu = false; onReport() },
                        leadingIcon = { Icon(Icons.Outlined.Flag, null, Modifier.size(18.dp)) }
                    )
                }
            }
        }

        // ── Phase 1 v2: Post Type Badge ──
        if (post.postType != "discussion") {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val (badgeIcon, badgeLabel, badgeColor) = when (post.postType) {
                    "showcase" -> Triple(Icons.Filled.Palette, post.appName ?: Strings.communityTypeShowcase, Color(0xFF6C5CE7))
                    "tutorial" -> Triple(Icons.Filled.MenuBook, post.title ?: Strings.communityTypeTutorial, Color(0xFF4CAF50))
                    "question" -> Triple(Icons.Filled.HelpOutline, post.title ?: Strings.communityTypeQuestion, Color(0xFFFF9800))
                    else -> Triple(Icons.Filled.ChatBubble, Strings.communityTypeDiscussion, Color(0xFF9E9E9E))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(badgeIcon, null, Modifier.size(13.dp), tint = badgeColor)
                        Spacer(Modifier.width(4.dp))
                        Text(badgeLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = badgeColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp))
                    }
                }
                // Difficulty badge for tutorials
                if (post.postType == "tutorial" && post.difficulty != null) {
                    val diffColor = when (post.difficulty) {
                        "beginner" -> Color(0xFF4CAF50)
                        "intermediate" -> Color(0xFFFF9800)
                        "advanced" -> Color(0xFFE91E63)
                        else -> Color(0xFF9E9E9E)
                    }
                    val diffLabel = when (post.difficulty) {
                        "beginner" -> Strings.communityDifficultyBeginner
                        "intermediate" -> Strings.communityDifficultyIntermediate
                        "advanced" -> Strings.communityDifficultyAdvanced
                        else -> post.difficulty
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = diffColor.copy(alpha = 0.1f)) {
                        Text(diffLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = diffColor)
                    }
                }
                // Resolved badge for questions
                if (post.postType == "question" && post.isResolved == true) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50).copy(alpha = 0.1f)) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(11.dp), tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(2.dp))
                            Text(Strings.communityResolvedLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }

        // ── Content (with @mention highlighting) ──
        Spacer(Modifier.height(8.dp))
        MentionableText(
            text = post.content,
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            mentionColor = MaterialTheme.colorScheme.primary,
            onMentionClick = onMentionClick
        )

        // ── Tags ──
        if (post.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(post.tags) { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tagColor(tag).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "#$tag", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = tagColor(tag)
                        )
                    }
                }
            }
        }

        // ── Media (images/videos) ──
        if (post.media.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            if (post.media.size == 1) {
                val m = post.media[0]
                val url = m.urlGitee ?: m.urlGithub
                if (url != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                // CLI-13: Multi-image with dot indicators
                val mediaListState = rememberLazyListState()
                val currentImageIndex by remember {
                    derivedStateOf { mediaListState.firstVisibleItemIndex }
                }
                Column {
                    LazyRow(
                        state = mediaListState,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(post.media) { m ->
                            val url = m.urlGitee ?: m.urlGithub
                            if (url != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(160.dp).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    if (post.media.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            post.media.forEachIndexed { idx, _ ->
                                val isActive = idx == currentImageIndex
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.5.dp)
                                        .size(if (isActive) 6.dp else 4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── App Links ──
        if (post.appLinks.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            post.appLinks.forEach { appLink ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        appLink.storeModuleId?.let { onAppLinkClick(it) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App icon
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            if (appLink.appIcon != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(appLink.appIcon).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        if (appLink.storeModuleType == "app") Icons.Outlined.Apps else Icons.Outlined.Extension,
                                        null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                appLink.appName ?: Strings.communityApplication,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            appLink.appDescription?.let {
                                Text(
                                    it, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                appLink.storeModuleDownloads?.let {
                                    Text("${it} ↓", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                                appLink.storeModuleRating?.let {
                                    Text("★ ${String.format("%.1f", it)}", fontSize = 10.sp,
                                        color = Color(0xFFFFB300).copy(alpha = 0.8f))
                                }
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // ── Phase 1 v2: Recipe Import Button ──
        if (post.hasRecipe && onImportRecipe != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = onImportRecipe,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF6C5CE7).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFF6C5CE7).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Download, null, Modifier.size(20.dp), tint = Color(0xFF6C5CE7))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Strings.communityUseRecipe, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF6C5CE7))
                        Text(Strings.communityRecipeDesc, fontSize = 11.sp,
                            color = Color(0xFF6C5CE7).copy(alpha = 0.6f))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6C5CE7).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${post.recipeImportCount}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF6C5CE7)
                        )
                    }
                }
            }
        }

        // ── Action Bar ──
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Like
            TextButton(onClick = onLike) {
                Icon(
                    if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    null, Modifier.size(18.dp),
                    tint = if (post.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${post.likeCount}", fontSize = 12.sp,
                    color = if (post.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            // Comment
            TextButton(onClick = onComment) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Text("${post.commentCount}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            // Share
            TextButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Text("${post.shareCount}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            // View count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.RemoveRedEye, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                Spacer(Modifier.width(3.dp))
                Text("${post.viewCount}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
            }
        }
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
        Triple("discussion", Strings.communityPostTypeDiscussion, Color(0xFF9E9E9E)),
        Triple("showcase", Strings.communityPostTypeShowcase, Color(0xFF6C5CE7)),
        Triple("tutorial", Strings.communityPostTypeTutorial, Color(0xFF4CAF50)),
        Triple("question", Strings.communityPostTypeQuestion, Color(0xFFFF9800)),
    ) }
    val sourceTypes = remember { listOf(
        "website" to Strings.communitySourceTypeWebsite, "html" to "HTML", "media" to Strings.communitySourceTypeMedia,
        "frontend" to Strings.communitySourceTypeFrontend, "server" to Strings.communitySourceTypeServer,
    ) }
    val difficulties = remember { listOf(
        "beginner" to "🟢 " + Strings.communityDifficultyBeginner, "intermediate" to "🟡 " + Strings.communityDifficultyIntermediate, "advanced" to "🔴 " + Strings.communityDifficultyAdvanced,
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
                Text(Strings.communityCreatePost, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (content.isBlank()) return@Button
                        if (selectedTags.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar(Strings.communityTagRequired) }
                            return@Button
                        }
                        if (selectedPostType == "showcase" && appName.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(Strings.communityEnterAppName) }
                            return@Button
                        }
                        if (selectedPostType == "tutorial" && tutorialTitle.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(Strings.communityEnterTutorialTitle) }
                            return@Button
                        }
                        if (selectedPostType == "question" && questionTitle.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(Strings.communityEnterQuestionTitle) }
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
                                is AuthResult.Error -> snackbarHostState.showSnackbar(Strings.communityPublishFailed)
                            }
                            isPublishing = false
                        }
                    },
                    enabled = content.isNotBlank() && selectedTags.isNotEmpty() && !isPublishing && pendingMedia.none { it.uploading },
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.height(36.dp)
                ) {
                    if (isPublishing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(Strings.communityPublish, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Phase 1 v2: Post Type Selector ───
            Text(Strings.communityPostTypeLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                        placeholder = { Text(Strings.communityAppNamePlaceholder, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Apps, null, Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(Strings.communitySourceTypeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        placeholder = { Text(Strings.communityRecipeJsonPlaceholder, fontSize = 12.sp) },
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
                        leadingIcon = { Icon(Icons.Outlined.HelpOutline, null, Modifier.size(18.dp)) }
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
                        else -> Strings.communityWhatsNew
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
                "${Strings.communitySelectTags} (${selectedTags.size}/$maxTags)",
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
                            else scope.launch { snackbarHostState.showSnackbar(Strings.communityTagMaxLimit) }
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
                    Text(Strings.communityAddMedia, fontSize = 12.sp)
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
                    Text(Strings.communityLinkApp, fontSize = 12.sp)
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
            title = { Text(Strings.communityLinkApp, fontWeight = FontWeight.Bold) },
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
                            Text(Strings.communityNoAppsToLink, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        } }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAppPicker = false }) { Text(Strings.communityConfirm) } }
        )
    }
}

// ═══════════════════════════════════════════
// Phase 1 v2: Discover Composables
// ═══════════════════════════════════════════

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = color)
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Showcase card for horizontal scroll in Discover tab */
@Composable
private fun ShowcaseCard(
    post: CommunityPostItem,
    onClick: () -> Unit,
    onImportRecipe: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.width(260.dp)
    ) {
        Column {
            // Screenshot area with gradient overlay
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val screenshot = post.media.firstOrNull()
                val imgUrl = screenshot?.urlGitee ?: screenshot?.urlGithub
                if (imgUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(imgUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6C5CE7).copy(alpha = 0.3f), Color(0xFFA29BFE).copy(alpha = 0.15f))
                            ),
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Apps, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    }
                }
                // Gradient overlay at bottom
                Box(
                    modifier = Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))
                        )
                )
                // App name overlay
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.appIconUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(post.appIconUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        post.appName ?: Strings.communityTypeShowcase, fontWeight = FontWeight.Bold,
                        fontSize = 14.sp, color = Color.White, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Import count badge
                if (post.recipeImportCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6C5CE7).copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Download, null, Modifier.size(11.dp), tint = Color.White)
                            Spacer(Modifier.width(2.dp))
                            Text("${post.recipeImportCount}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // Info footer
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    post.content, fontSize = 12.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Author
                    Text(
                        post.authorDisplayName ?: post.authorUsername,
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.weight(1f))
                    // Recipe button
                    if (post.hasRecipe) {
                        Surface(
                            onClick = onImportRecipe,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6C5CE7).copy(alpha = 0.1f)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Download, null, Modifier.size(12.dp), tint = Color(0xFF6C5CE7))
                                Spacer(Modifier.width(3.dp))
                                Text("导入", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C5CE7))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Compact tutorial list item for Discover tab */
@Composable
private fun TutorialCard(
    post: CommunityPostItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or icon
            val thumb = post.media.firstOrNull()
            val thumbUrl = thumb?.urlGitee ?: thumb?.urlGithub
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                modifier = Modifier.size(50.dp)
            ) {
                if (thumbUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(thumbUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.MenuBook, null, Modifier.size(24.dp), tint = Color(0xFF4CAF50))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.title ?: post.content.take(50),
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        post.authorDisplayName ?: post.authorUsername,
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    // Difficulty
                    post.difficulty?.let { diff ->
                        val (diffLabel, diffColor) = when (diff) {
                            "beginner" -> Strings.communityDifficultyBeginner to Color(0xFF4CAF50)
                            "intermediate" -> Strings.communityDifficultyIntermediate to Color(0xFFFF9800)
                            "advanced" -> Strings.communityDifficultyAdvanced to Color(0xFFE91E63)
                            else -> diff to Color(0xFF9E9E9E)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = diffColor.copy(alpha = 0.1f)) {
                            Text(diffLabel, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp, fontWeight = FontWeight.Bold, color = diffColor)
                        }
                    }
                }
            }
            // Stats
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.RemoveRedEye, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.width(2.dp))
                    Text("${post.viewCount}", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Favorite, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.width(2.dp))
                    Text("${post.likeCount}", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

/** Question item for Discover tab */
@Composable
private fun QuestionCard(
    post: CommunityPostItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Q icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFF9800).copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Q", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFFFF9800))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.title ?: post.content.take(60),
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        post.authorDisplayName ?: post.authorUsername,
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.width(2.dp))
                        Text("${post.commentCount} 回答", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                    if (post.isResolved == true) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF4CAF50).copy(alpha = 0.1f)) {
                            Text(Strings.communityResolvedLabel, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════

fun tagColor(tag: String): Color = when (tag) {
    "html" -> Color(0xFFE44D26)
    "css" -> Color(0xFF264DE4)
    "javascript", "typescript" -> Color(0xFFF7DF1E)
    "vue" -> Color(0xFF42B883)
    "react" -> Color(0xFF61DAFB)
    "angular" -> Color(0xFFDD0031)
    "svelte" -> Color(0xFFFF3E00)
    "nextjs" -> Color(0xFF888888)
    "nuxtjs" -> Color(0xFF00DC82)
    "nodejs" -> Color(0xFF339933)
    "python" -> Color(0xFF3776AB)
    "php" -> Color(0xFF777BB4)
    "go" -> Color(0xFF00ADD8)
    "webtoapp" -> Color(0xFF6C5CE7)
    "pwa" -> Color(0xFF5A0FC8)
    "responsive" -> Color(0xFF00BCD4)
    "animation" -> Color(0xFFFF5722)
    "game" -> Color(0xFFE91E63)
    "tool" -> Color(0xFF607D8B)
    "education" -> Color(0xFF4CAF50)
    // Chinese use_case tags
    "新闻阅读" -> Color(0xFF2196F3)
    "视频播放" -> Color(0xFFE91E63)
    "社交工具" -> Color(0xFF9C27B0)
    "办公效率" -> Color(0xFF3F51B5)
    "学习教育" -> Color(0xFF4CAF50)
    "游戏娱乐" -> Color(0xFFFF5722)
    "生活服务" -> Color(0xFF009688)
    "购物比价" -> Color(0xFFFF9800)
    "隐私安全" -> Color(0xFF795548)
    "系统工具" -> Color(0xFF607D8B)
    // Chinese feature tags
    "广告拦截" -> Color(0xFFD32F2F)
    "暗黑模式" -> Color(0xFF424242)
    "离线使用" -> Color(0xFF00897B)
    "视频下载" -> Color(0xFF1565C0)
    "自定义图标" -> Color(0xFF6A1B9A)
    "启动画面" -> Color(0xFFF4511E)
    "后台运行" -> Color(0xFF00838F)
    "多语言" -> Color(0xFF558B2F)
    "脱壳包装" -> Color(0xFF37474F)
    "模块扩展" -> Color(0xFF6C5CE7)
    else -> Color(0xFF9E9E9E)
}

fun formatTimeAgo(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        // ── 1. Extract timezone offset from ISO string ──
        // Server may return: "2026-03-24T22:30:15+00:00", "...+08:00", "...Z", or bare
        val tzRegex = Regex("([+-])(\\d{2}):(\\d{2})$")
        val tzMatch = tzRegex.find(isoString)
        val endsWithZ = isoString.endsWith("Z")

        // Calculate the offset in milliseconds
        val offsetMs = when {
            tzMatch != null -> {
                val sign = if (tzMatch.groupValues[1] == "+") 1 else -1
                val hh = tzMatch.groupValues[2].toInt()
                val mm = tzMatch.groupValues[3].toInt()
                sign * (hh * 3600_000L + mm * 60_000L)
            }
            endsWithZ -> 0L
            else -> 0L   // no timezone info → assume UTC (server should always send +00:00)
        }

        // ── 2. Strip fractional seconds AND timezone suffix ──
        val cleaned = isoString
            .replace(Regex("\\.[0-9]+"), "")          // remove .123456
            .replace(Regex("[+-]\\d{2}:\\d{2}$"), "")  // remove +08:00
            .replace(Regex("Z$"), "")                  // remove trailing Z

        // ── 3. Parse as local-of-offset, then convert to epoch millis ──
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val parsed = format.parse(cleaned) ?: return isoString
        // parsed.time = epoch millis assuming the string is UTC.
        // Subtract the offset to get the actual UTC epoch millis.
        val epochMs = parsed.time - offsetMs

        // ── 4. Relative time ──
        val now = System.currentTimeMillis()
        val diff = now - epochMs
        if (diff < 0) return Strings.timeJustNow // future time safety
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        when {
            minutes < 1 -> Strings.timeJustNow
            minutes < 60 -> String.format(Strings.timeMinutesAgo, minutes)
            hours < 24 -> String.format(Strings.timeHoursAgo, hours)
            days < 7 -> String.format(Strings.timeDaysAgo, days)
            days < 30 -> String.format(Strings.timeWeeksAgo, days / 7)
            else -> String.format(Strings.timeMonthsAgo, days / 30)
        }
    } catch (_: Exception) { isoString }
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> String.format(Strings.durationHourMinute, hours, minutes)
        minutes > 0 -> String.format(Strings.durationMinute, minutes)
        else -> Strings.durationLessThanMinute
    }
}


// ═══════════════════════════════════════════
// Unified Search Sheet (Users + Posts tabs)
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUsersSheet(
    communityViewModel: CommunityViewModel,
    onDismiss: () -> Unit,
    onUserClick: (Int) -> Unit,
    onPostClick: (Int) -> Unit = {}
) {
    val searchResults by communityViewModel.searchResults.collectAsState()
    val searchLoading by communityViewModel.searchLoading.collectAsState()
    val postSearchResults by communityViewModel.postSearchResults.collectAsState()
    val postSearchLoading by communityViewModel.postSearchLoading.collectAsState()
    var query by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("users") } // "users" | "posts"

    // Advanced search filters for posts tab
    var selectedPostType by remember { mutableStateOf<String?>(null) }
    var selectedSortBy by remember { mutableStateOf("relevance") }

    // Dispatch search to current tab
    fun dispatchSearch(text: String) {
        query = text
        when (activeTab) {
            "users" -> communityViewModel.searchUsers(text)
            "posts" -> communityViewModel.searchPostsFiltered(text, selectedPostType, selectedSortBy)
        }
    }

    // Re-dispatch when filters change
    fun onFilterChanged() {
        if (query.isNotBlank()) {
            communityViewModel.searchPostsFiltered(query, selectedPostType, selectedSortBy)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)
        ) {
            Text(Strings.communitySearchAll, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            // ── Tab Bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("users" to Strings.communityTabUsers, "posts" to Strings.communityTabPostsSearch).forEach { (key, label) ->
                    val isSelected = activeTab == key
                    Surface(
                        onClick = {
                            if (activeTab != key) {
                                activeTab = key
                                if (query.isNotBlank()) {
                                    when (key) {
                                        "users" -> communityViewModel.searchUsers(query)
                                        "posts" -> communityViewModel.searchPostsFiltered(query, selectedPostType, selectedSortBy)
                                    }
                                }
                            }
                        },
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

            Spacer(Modifier.height(12.dp))

            // ── Search Input ──
            OutlinedTextField(
                value = query,
                onValueChange = { dispatchSearch(it) },
                placeholder = {
                    Text(
                        if (activeTab == "users") Strings.communitySearchHint else Strings.communitySearchPostsHint,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { dispatchSearch("") }) {
                            Icon(Icons.Filled.Close, null, Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )

            // ── Advanced Filters (Posts tab only) ──
            AnimatedVisibility(visible = activeTab == "posts") {
                Column {
                    Spacer(Modifier.height(10.dp))
                    // Post type filter
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val postTypeFilters = listOf(
                            null to Strings.communityAllTags,
                            "discussion" to Strings.communityPostTypeDiscussion,
                            "showcase" to Strings.communityPostTypeShowcase,
                            "tutorial" to Strings.communityPostTypeTutorial,
                            "question" to Strings.communityPostTypeQuestion,
                        )
                        postTypeFilters.forEach { (type, label) ->
                            val isSelected = selectedPostType == type
                            val chipColor = when (type) {
                                "showcase" -> Color(0xFF6C5CE7)
                                "tutorial" -> Color(0xFF4CAF50)
                                "question" -> Color(0xFFFF9800)
                                "discussion" -> Color(0xFF9E9E9E)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPostType = type
                                    onFilterChanged()
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(alpha = 0.15f),
                                    selectedLabelColor = chipColor
                                ),
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Filled.Check, null, Modifier.size(12.dp), tint = chipColor) }
                                } else null
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Sort options
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Sort, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        val sortOptions = listOf(
                            "relevance" to "相关度",
                            "newest" to "最新",
                            "likes" to "最多点赞",
                        )
                        sortOptions.forEach { (key, label) ->
                            val isSelected = selectedSortBy == key
                            Surface(
                                onClick = {
                                    selectedSortBy = key
                                    onFilterChanged()
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                modifier = Modifier.height(26.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Results ──
            when (activeTab) {
                "users" -> {
                    if (searchLoading) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (query.isNotBlank() && searchResults.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.PersonOff, null, Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(Modifier.height(8.dp))
                                Text(Strings.communityNoUsersFound, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp)
                        ) {
                            items(searchResults, key = { it.id }) { user ->
                                UserListRow(
                                    displayName = user.displayName ?: user.username,
                                    username = user.username,
                                    avatarUrl = user.avatarUrl,
                                    isDeveloper = user.isDeveloper,
                                    onClick = { onUserClick(user.id) }
                                )
                                if (user != searchResults.last()) {
                                    HorizontalDivider(
                                        Modifier.padding(start = 56.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }

                "posts" -> {
                    if (postSearchLoading) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (query.isNotBlank() && postSearchResults.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.SearchOff, null, Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(Modifier.height(8.dp))
                                Text(Strings.communityNoPostsFound, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            items(postSearchResults, key = { "search_${it.id}" }) { post ->
                                SearchPostRow(
                                    post = post,
                                    onClick = {
                                        onDismiss()
                                        onPostClick(post.id)
                                    }
                                )
                                if (post != postSearchResults.last()) {
                                    HorizontalDivider(
                                        Modifier.padding(start = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
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

/** Compact post row for search results */
@Composable
private fun SearchPostRow(
    post: CommunityPostItem,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Post type icon
        val (typeIcon, typeColor) = when (post.postType) {
            "showcase" -> Icons.Filled.Palette to Color(0xFF6C5CE7)
            "tutorial" -> Icons.Filled.MenuBook to Color(0xFF4CAF50)
            "question" -> Icons.Filled.HelpOutline to Color(0xFFFF9800)
            else -> Icons.Filled.ChatBubble to Color(0xFF9E9E9E)
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = typeColor.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(typeIcon, null, Modifier.size(20.dp), tint = typeColor)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // Title or content preview
            Text(
                post.title ?: post.content.take(80),
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    post.authorDisplayName ?: post.authorUsername,
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Text(
                    formatTimeAgo(post.createdAt),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            // Stats row
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Favorite, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.width(2.dp))
                    Text("${post.likeCount}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.width(2.dp))
                    Text("${post.commentCount}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}


// ═══════════════════════════════════════════
// Follower / Following List Sheet
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowerListSheet(
    title: String,
    users: List<com.webtoapp.core.cloud.CommunityUserProfile>,
    emptyText: String,
    onDismiss: () -> Unit,
    onUserClick: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            if (users.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.People, null, Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text(emptyText, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(users, key = { it.id }) { user ->
                        UserListRow(
                            displayName = user.displayName ?: user.username,
                            username = user.username,
                            avatarUrl = user.avatarUrl,
                            isDeveloper = user.isDeveloper,
                            subtitle = if (user.followerCount > 0) "${user.followerCount} ${Strings.communityFollowersList}" else null,
                            onClick = { onUserClick(user.id) }
                        )
                        if (user != users.last()) {
                            HorizontalDivider(
                                Modifier.padding(start = 56.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════
// Reusable User Row (for search / follower list)
// ═══════════════════════════════════════════

@Composable
private fun UserListRow(
    displayName: String,
    username: String,
    avatarUrl: String?,
    isDeveloper: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        displayName.take(1).uppercase(),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (isDeveloper) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Verified, null, Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("@$username", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            subtitle?.let {
                Text(it, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            }
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
    }
}
