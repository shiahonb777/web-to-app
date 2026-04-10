package com.webtoapp.ui.screens.community

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.cloud.CommunityModuleDetail
import com.webtoapp.core.cloud.CommunityPostItem
import com.webtoapp.core.cloud.TeamWorkItem
import com.webtoapp.core.cloud.UserActivityInfo
import com.webtoapp.ui.viewmodel.CommunityViewModel
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.UserTitleBadges
import androidx.compose.ui.graphics.Color

/**
 * 用户主页 — 全面对接所有 API 数据，精致 UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Int,
    communityViewModel: CommunityViewModel,
    onBack: () -> Unit,
    onModuleClick: (Int) -> Unit,
    onPostClick: (Int) -> Unit = {},
    onNavigateToUser: (Int) -> Unit = {}
) {
    val profile by communityViewModel.userProfile.collectAsStateWithLifecycle()
    val modules by communityViewModel.userModules.collectAsStateWithLifecycle()
    val teamWorks by communityViewModel.userTeamWorks.collectAsStateWithLifecycle()
    val userPosts by communityViewModel.userPosts.collectAsStateWithLifecycle()
    val userActivity by communityViewModel.userActivity.collectAsStateWithLifecycle()
    val loading by communityViewModel.userProfileLoading.collectAsStateWithLifecycle()
    val message by communityViewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }
    var showFollowersSheet by remember { mutableStateOf(false) }
    var showFollowingSheet by remember { mutableStateOf(false) }
    val followersList by communityViewModel.followersList.collectAsStateWithLifecycle()
    val followingList by communityViewModel.followingList.collectAsStateWithLifecycle()

    LaunchedEffect(userId) { communityViewModel.loadUserProfile(userId) }
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); communityViewModel.clearMessage() } }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    profile?.let { u ->
                        Text(u.displayName ?: u.username, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp)) }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (loading) {
                ProfileShimmer(Modifier)
            } else {
                profile?.let { user ->
                    LazyColumn(Modifier.fillMaxSize()) {
                        // ════════════════════════════════════
                        // HEADER — Avatar + Info + Follow
                        // ════════════════════════════════════
                        item {
                            StaggeredItem(index = 0) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        // Avatar — larger with optional CoilAsync
                                        Box {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(72.dp)
                                            ) {
                                                if (user.avatarUrl != null) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(user.avatarUrl).crossfade(true).build(),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Text(
                                                            (user.displayName ?: user.username).take(1).uppercase(),
                                                            fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                            // Online dot
                                            userActivity?.let { act ->
                                                if (act.isOnline) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFF4CAF50),
                                                        modifier = Modifier.size(14.dp)
                                                            .align(Alignment.BottomEnd),
                                                        shadowElevation = 2.dp
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.surface,
                                                            modifier = Modifier.padding(2.dp)
                                                        ) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = Color(0xFF4CAF50),
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {}
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        SpringFollowButton(
                                            isFollowing = user.isFollowing,
                                            onClick = { communityViewModel.toggleFollow(userId) }
                                        )
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    // Display name + badges
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            user.displayName ?: user.username,
                                            fontWeight = FontWeight.Bold, fontSize = 22.sp
                                        )
                                        if (user.isDeveloper) {
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.Filled.Verified, null, Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    // @username + online/offline status
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "@${user.username}", fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        userActivity?.let { act ->
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (act.isOnline) Color(0xFF4CAF50).copy(alpha = 0.1f)
                                                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    if (act.isOnline) Strings.communityOnline else {
                                                        act.lastSeenAt?.let { String.format(Strings.communityLastSeen, formatTimeAgo(it)) }
                                                            ?: Strings.communityOffline
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                                    color = if (act.isOnline) Color(0xFF4CAF50)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                )
                                            }
                                        }
                                    }

                                    // Team badges
                                    if (user.teamBadges.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        UserTitleBadges(
                                            isDeveloper = user.isDeveloper,
                                            teamBadges = user.teamBadges
                                        )
                                    }

                                    // Bio
                                    user.bio?.let {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            it, fontSize = 15.sp, lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                    }

                                    // Join date
                                    user.createdAt?.let {
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                String.format(Strings.communityJoined, formatDate(it)),
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }

                                    // Stats row
                                    Spacer(Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                        StatLabel(
                                            user.followingCount, Strings.communityFollowing,
                                            onClick = {
                                                communityViewModel.loadUserFollowing(userId)
                                                showFollowingSheet = true
                                            }
                                        )
                                        StatLabel(
                                            user.followerCount, Strings.communityFollowers,
                                            onClick = {
                                                communityViewModel.loadUserFollowers(userId)
                                                showFollowersSheet = true
                                            }
                                        )
                                        StatLabel(user.appCount, Strings.communityApps)
                                        StatLabel(user.moduleCount, Strings.communityModules)
                                    }
                                }
                            }
                            GlassDivider()
                        }

                        // ════════════════════════════════════
                        // ONLINE TIME STATS CARD
                        // ════════════════════════════════════
                        userActivity?.let { act ->
                            if (act.todaySeconds > 0 || act.monthSeconds > 0 || act.yearSeconds > 0) {
                                item {
                                    StaggeredItem(index = 1) {
                                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OnlineTimeCard(
                                                    icon = Icons.Outlined.Today,
                                                    label = Strings.communityTodayOnline,
                                                    value = formatDuration(act.todaySeconds),
                                                    accent = Color(0xFF42A5F5),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OnlineTimeCard(
                                                    icon = Icons.Outlined.DateRange,
                                                    label = Strings.communityMonthOnline,
                                                    value = formatDuration(act.monthSeconds),
                                                    accent = Color(0xFF66BB6A),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OnlineTimeCard(
                                                    icon = Icons.Outlined.CalendarMonth,
                                                    label = Strings.communityYearOnline,
                                                    value = formatDuration(act.yearSeconds),
                                                    accent = Color(0xFFFFB74D),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                    GlassDivider()
                                }
                            }
                        }

                        // ════════════════════════════════════
                        // TAB BAR
                        // ════════════════════════════════════
                        item {
                            val tabIndex = if (userActivity != null) 2 else 1
                            StaggeredItem(index = tabIndex) {
                                val tabs = listOf(
                                    Triple(Icons.Outlined.Extension, Strings.communityModules, modules.size),
                                    Triple(Icons.Outlined.Groups, Strings.communityTeamWorks, teamWorks.size),
                                    Triple(Icons.Outlined.Forum, Strings.communityPosts, userPosts.size),
                                    Triple(Icons.Outlined.Timeline, Strings.communityActivity, -1)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    tabs.forEachIndexed { index, (icon, label, count) ->
                                        val isSelected = selectedTab == index
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedTab = index }
                                                .padding(vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    icon, null, Modifier.size(15.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                )
                                                Text(
                                                    label, fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    maxLines = 1
                                                )
                                                if (count > 0) {
                                                    CountBadge(count)
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(2.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                modifier = Modifier.width(32.dp).height(2.5.dp)
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }

                        // ════════════════════════════════════
                        // TAB CONTENT
                        // ════════════════════════════════════
                        when (selectedTab) {
                            0 -> {
                                // ── Modules ──
                                if (modules.isEmpty()) {
                                    item { EmptyState(Icons.Outlined.Extension, Strings.communityNoModulesYet, Strings.communityNoModulesHint) }
                                }
                                itemsIndexed(modules, key = { _, m -> m.id }) { index, module ->
                                    StaggeredItem(index = index + 3) {
                                        ModuleRow(module, onClick = { onModuleClick(module.id) })
                                    }
                                    GlassDivider()
                                }
                            }
                            1 -> {
                                // ── Team Works ──
                                if (teamWorks.isEmpty()) {
                                    item { EmptyState(Icons.Outlined.Groups, Strings.communityNoTeamWorksYet, Strings.communityNoTeamWorksHint) }
                                }
                                itemsIndexed(teamWorks, key = { _, w -> "tw_${w.id}" }) { index, work ->
                                    StaggeredItem(index = index + 3) {
                                        TeamWorkRow(work, onClick = { onModuleClick(work.id) })
                                    }
                                    GlassDivider()
                                }
                            }
                            2 -> {
                                // ── Posts ──
                                if (userPosts.isEmpty()) {
                                    item { EmptyState(Icons.Outlined.Forum, Strings.communityNoPosts, null) }
                                }
                                itemsIndexed(userPosts, key = { _, p -> "post_${p.id}" }) { index, post ->
                                    StaggeredItem(index = index + 3) {
                                        PostRow(post, onClick = { onPostClick(post.id) })
                                    }
                                    GlassDivider()
                                }
                            }
                            3 -> {
                                // ── Activity ──
                                item {
                                    StaggeredItem(index = 3) {
                                        userActivity?.let { act ->
                                            ActivityCard(act, user.displayName ?: user.username)
                                        } ?: EmptyState(Icons.Outlined.Timeline, Strings.communityNoActivityData, null)
                                    }
                                }
                            }
                        }

                        // Bottom spacer
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }

    // ─── Followers Sheet ───
    if (showFollowersSheet) {
        FollowerListSheet(
            title = Strings.communityFollowersList,
            users = followersList,
            emptyText = Strings.communityNoFollowers,
            onDismiss = { showFollowersSheet = false },
            onUserClick = { uid ->
                showFollowersSheet = false
                onNavigateToUser(uid)
            }
        )
    }

    // ─── Following Sheet ───
    if (showFollowingSheet) {
        FollowerListSheet(
            title = Strings.communityFollowingList,
            users = followingList,
            emptyText = Strings.communityNoFollowing,
            onDismiss = { showFollowingSheet = false },
            onUserClick = { uid ->
                showFollowingSheet = false
                onNavigateToUser(uid)
            }
        )
    }
}

// ═══ Count Badge ═══

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    ) {
        Text(
            "$count",
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ═══ Empty State ═══

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?
) {
    Box(Modifier.fillMaxWidth().padding(vertical = 56.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            }
        }
    }
}

// ═══ Online Time Card ═══

@Composable
private fun OnlineTimeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = accent.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(
                value, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = accent
            )
            Text(
                label, fontSize = 10.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ═══ Activity Card ═══

@Composable
private fun ActivityCard(act: UserActivityInfo, userName: String) {
    Column(Modifier.padding(16.dp)) {
        // Online status card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (act.isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (act.isOnline) "$userName ${Strings.communityOnline}"
                        else "$userName ${Strings.communityOffline}",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
                act.lastSeenAt?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${String.format(Strings.communityLastSeen, formatTimeAgo(it))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OnlineTimeCard(
                        icon = Icons.Outlined.Today,
                        label = Strings.communityTodayOnline,
                        value = formatDuration(act.todaySeconds),
                        accent = Color(0xFF42A5F5),
                        modifier = Modifier.weight(1f)
                    )
                    OnlineTimeCard(
                        icon = Icons.Outlined.DateRange,
                        label = Strings.communityMonthOnline,
                        value = formatDuration(act.monthSeconds),
                        accent = Color(0xFF66BB6A),
                        modifier = Modifier.weight(1f)
                    )
                    OnlineTimeCard(
                        icon = Icons.Outlined.CalendarMonth,
                        label = Strings.communityYearOnline,
                        value = formatDuration(act.yearSeconds),
                        accent = Color(0xFFFFB74D),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ═══ Spring Follow Button ═══

@Composable
private fun SpringFollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        CommunityPhysics.MorphButton,
        label = "followScale",
        finishedListener = { pressed = false }
    )

    if (isFollowing) {
        PremiumOutlinedButton(
            onClick = { pressed = true; onClick() },
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            modifier = Modifier.scale(scale)
        ) { Text(Strings.communityFollowing, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
    } else {
        PremiumButton(
            onClick = { pressed = true; onClick() },
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            modifier = Modifier.scale(scale),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) { Text(Strings.communityFollow, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}

// ═══ Stat Label ═══

@Composable
private fun StatLabel(count: Int, label: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedCounter(
            count = count,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }
}

// ═══ Module Row ═══

@Composable
private fun ModuleRow(module: CommunityModuleDetail, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Module icon
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            if (module.icon != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(module.icon).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Outlined.Extension, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(module.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                module.versionName?.let {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text("v$it", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
            if (!module.description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(module.description, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            }
            // Tags
            if (module.tags.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    module.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tagColor(tag).copy(alpha = 0.1f)
                        ) {
                            Text("#$tag", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = tagColor(tag))
                        }
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Download, null, Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    Spacer(Modifier.width(2.dp))
                    Text("${module.downloads}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★", fontSize = 12.sp, color = Color(0xFFFFB300).copy(alpha = 0.8f))
                    Spacer(Modifier.width(2.dp))
                    Text(String.format("%.1f", module.rating), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text(" (${module.ratingCount})", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
                if (module.isFeatured) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFB300).copy(alpha = 0.12f)
                    ) {
                        Text(Strings.communityFeatured, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    }
                }
            }
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp).align(Alignment.CenterVertically),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
    }
}

// ═══ Team Work Row ═══

@Composable
private fun TeamWorkRow(work: TeamWorkItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (work.moduleType == "app")
                MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            if (work.icon != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(work.icon).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        if (work.moduleType == "app") Icons.Outlined.Apps else Icons.Outlined.Extension,
                        null, Modifier.size(20.dp),
                        tint = if (work.moduleType == "app") MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(work.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Role badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (work.contributorRole == "lead")
                        Color(0xFFFFB300).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        if (work.contributorRole == "lead") Strings.communityLead else Strings.communityMember,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (work.contributorRole == "lead") Color(0xFFFFB300)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                if (work.contributionPoints > 0) {
                    Text(String.format(Strings.communityPoints, work.contributionPoints), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
                work.teamName?.let {
                    Text("·", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            work.contributionDescription?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Download, null, Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    Spacer(Modifier.width(2.dp))
                    Text("${work.downloads}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★", fontSize = 12.sp, color = Color(0xFFFFB300).copy(alpha = 0.8f))
                    Spacer(Modifier.width(2.dp))
                    Text(String.format("%.1f", work.rating), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                }
            }
        }
    }
}

// ═══ Post Row ═══

@Composable
private fun PostRow(post: CommunityPostItem, onClick: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Content preview
        Text(
            post.content, maxLines = 3, overflow = TextOverflow.Ellipsis,
            fontSize = 14.5.sp, lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
        )
        // Media preview (show first image if any)
        if (post.media.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val m = post.media[0]
            val url = m.urlGitee ?: m.urlGithub
            if (url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        // Tags
        if (post.tags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                post.tags.take(4).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tagColor(tag).copy(alpha = 0.12f)
                    ) {
                        Text("#$tag", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = tagColor(tag))
                    }
                }
            }
        }
        // Interaction stats + time
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        null, Modifier.size(14.dp),
                        tint = if (post.isLiked) Color(0xFFE91E63)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text("${post.likeCount}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    Spacer(Modifier.width(2.dp))
                    Text("${post.commentCount}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.RemoveRedEye, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                    Spacer(Modifier.width(2.dp))
                    Text("${post.viewCount}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                formatTimeAgo(post.createdAt), fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

// ═══ Shimmer ═══

@Composable
private fun ProfileShimmer(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Row {
            ShimmerBlock(72.dp, 72.dp, CircleShape)
            Spacer(Modifier.weight(1f))
            ShimmerBlock(88.dp, 36.dp, RoundedCornerShape(20.dp))
        }
        Spacer(Modifier.height(14.dp))
        ShimmerBlock(160.dp, 20.dp)
        Spacer(Modifier.height(6.dp))
        ShimmerBlock(120.dp, 14.dp)
        Spacer(Modifier.height(14.dp))
        ShimmerBlock(280.dp, 14.dp)
        Spacer(Modifier.height(6.dp))
        ShimmerBlock(200.dp, 14.dp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            repeat(3) { ShimmerBlock(70.dp, 14.dp) }
        }
        Spacer(Modifier.height(20.dp))
        GlassDivider()
        repeat(4) {
            Spacer(Modifier.height(12.dp))
            Row {
                ShimmerBlock(40.dp, 40.dp, RoundedCornerShape(10.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    ShimmerBlock(160.dp, 14.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBlock(240.dp, 13.dp)
                }
            }
        }
    }
}

// ═══ Date formatter ═══

internal fun formatDate(isoDate: String?): String {
    if (isoDate == null) return ""
    return try {
        val parts = isoDate.split("T")[0].split("-")
        if (parts.size >= 3) "${parts[1]}/${parts[2]}/${parts[0]}"
        else isoDate
    } catch (_: Exception) { isoDate }
}

// ═══ Online Time Badge (kept for backward compat) ═══

@Composable
private fun OnlineTimeBadge(label: String, value: String, modifier: Modifier = Modifier) {
    OnlineTimeCard(
        icon = Icons.Outlined.AccessTime,
        label = label,
        value = value,
        accent = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
