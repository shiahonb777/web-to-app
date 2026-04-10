package com.webtoapp.ui.screens.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.FeedItem
import com.webtoapp.core.cloud.NotificationItem
import com.webtoapp.ui.viewmodel.CommunityViewModel
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.core.i18n.Strings

/**
 * 通知与动态 — Jobs-style: 毛玻璃 Tab + 弹簧物理滑动指示器 + 精致入场动画
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    communityViewModel: CommunityViewModel,
    onBack: () -> Unit,
    onNavigateToModule: (Int) -> Unit,
    onNavigateToUser: (Int) -> Unit
) {
    val notifications by communityViewModel.notifications.collectAsStateWithLifecycle()
    val notificationsLoading by communityViewModel.notificationsLoading.collectAsStateWithLifecycle()
    val feed by communityViewModel.activityFeed.collectAsStateWithLifecycle()
    val feedLoading by communityViewModel.activityFeedLoading.collectAsStateWithLifecycle()
    val unreadCount by communityViewModel.unreadCount.collectAsStateWithLifecycle()
    val message by communityViewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        communityViewModel.loadNotifications()
        communityViewModel.loadUnreadCount()
        communityViewModel.loadFeed()
    }
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); communityViewModel.clearMessage() } }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(Strings.communityNotifications, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp)) }
                },
                actions = {
                    if (selectedTab == 0 && unreadCount > 0) {
                        IconButton(onClick = { communityViewModel.markAllNotificationsRead() }) {
                            Icon(Icons.Outlined.DoneAll, null, Modifier.size(22.dp))
                        }
                    }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(Modifier) {
            // === 弹簧物理 Tab ===
            SpringTab(
                tabs = listOf(Strings.communityTabAll, Strings.communityTabActivity),
                selectedIndex = selectedTab,
                badge = if (unreadCount > 0) unreadCount else null,
                onSelect = { selectedTab = it }
            )
            GlassDivider()

            // === 内容区 Crossfade 平滑过渡 ===
            Crossfade(targetState = selectedTab, animationSpec = tween(280), label = "tabCross") { tab ->
                when (tab) {
                    0 -> NotificationsContent(notifications, notificationsLoading, communityViewModel, onNavigateToModule, onNavigateToUser)
                    1 -> FeedContent(feed, feedLoading, onNavigateToModule)
                }
            }
        }
    }
        }
}

// ═══ 弹簧 Tab ═══

@Composable
private fun SpringTab(
    tabs: List<String>,
    selectedIndex: Int,
    badge: Int?,
    onSelect: (Int) -> Unit
) {
    // 指示器位置用弹簧物理
    val indicatorOffset by animateDpAsState(
        targetValue = (selectedIndex * 180).dp, // 近似
        CommunityPhysics.TabIndicator,
        label = "tabOffset"
    )

    Row(Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, label ->
            Box(
                Modifier.weight(weight = 1f, fill = true).clickable { onSelect(index) }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (index == selectedIndex) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        if (index == 0 && badge != null) {
                            Spacer(Modifier.width(4.dp))
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("$badge", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // 弹簧动画指示器
                    val indicatorAlpha by animateFloatAsState(
                        if (index == selectedIndex) 1f else 0f,
                        CommunityPhysics.ItemEntrance, label = "indAlpha"
                    )
                    val indicatorScale by animateFloatAsState(
                        if (index == selectedIndex) 1f else 0.3f,
                        CommunityPhysics.MorphButton, label = "indScale"
                    )
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(36.dp).height(3.dp)
                            .graphicsLayer { alpha = indicatorAlpha; scaleX = indicatorScale }
                    ) {}
                }
            }
        }
    }
}

// ═══ 通知列表 ═══

@Composable
private fun NotificationsContent(
    notifications: List<NotificationItem>, loading: Boolean,
    viewModel: CommunityViewModel,
    onModule: (Int) -> Unit, onUser: (Int) -> Unit
) {
    if (loading) {
        ListShimmer()
    } else if (notifications.isEmpty()) {
        EmptyState(Strings.communityNothingYet, Strings.communityNothingYetHint)
    } else {
        LazyColumn {
            itemsIndexed(notifications, key = { _, n -> n.id }) { index, notification ->
                StaggeredItem(index = index) {
                    NotificationRow(notification) {
                        viewModel.markNotificationRead(notification.id)
                        when (notification.refType) {
                            "module" -> notification.refId?.let { onModule(it) }
                            "user" -> notification.actorId?.let { onUser(it) }
                        }
                    }
                }
                GlassDivider()
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationItem, onClick: () -> Unit) {
    val icon = when (notification.type) {
        "comment" -> Icons.Outlined.ChatBubbleOutline
        "vote" -> Icons.Outlined.ThumbUp
        "follow" -> Icons.Outlined.PersonAdd
        "system" -> Icons.Outlined.Campaign
        else -> Icons.Outlined.Notifications
    }
    val iconColor = when (notification.type) {
        "comment" -> MaterialTheme.colorScheme.primary
        "vote" -> Color(0xFF4CAF50)
        "follow" -> Color(0xFF1D9BF0)
        "system" -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!notification.isRead) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(6.dp).offset(y = 7.dp)) {}
            Spacer(Modifier.width(10.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Icon(icon, null, Modifier.size(18.dp), tint = iconColor)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(weight = 1f, fill = true)) {
            notification.title?.let {
                Text(it, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            notification.content?.let {
                Text(it, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
            }
            notification.createdAt?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f))
            }
        }
    }
}

// ═══ 动态 Feed ═══

@Composable
private fun FeedContent(feed: List<FeedItem>, loading: Boolean, onModule: (Int) -> Unit) {
    if (loading) {
        ListShimmer()
    } else if (feed.isEmpty()) {
        EmptyState(Strings.communityNoFeedYet, Strings.communityNoFeedYetHint)
    } else {
        LazyColumn {
            itemsIndexed(feed, key = { _, f -> f.id }) { index, item ->
                StaggeredItem(index = index) {
                    FeedRow(item) { item.targetId?.let { onModule(it) } }
                }
                GlassDivider()
            }
        }
    }
}

@Composable
private fun FeedRow(item: FeedItem, onTargetClick: () -> Unit) {
    val action = when (item.type) {
        "publish" -> Strings.communityActionPublished; "vote" -> Strings.communityActionLiked; "comment" -> Strings.communityActionReplied
        "favorite" -> Strings.communityActionBookmarked; "follow" -> Strings.communityActionFollowed; else -> Strings.communityActionInteracted
    }

    Row(
        Modifier.fillMaxWidth().clickable(onClick = onTargetClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(name = item.actorName, avatarUrl = item.actorAvatar, size = 36)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(weight = 1f, fill = true)) {
            Text("${item.actorName} $action ${item.targetName ?: ""}", fontSize = 14.sp, lineHeight = 19.sp)
            item.createdAt?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f))
            }
        }
    }
}

// ═══ 通用 ═══

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(48.dp)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 14.sp, lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f))
        }
    }
}

@Composable
private fun ListShimmer() {
    Column(Modifier.padding(16.dp)) {
        repeat(6) {
            Row(Modifier.padding(vertical = 10.dp)) {
                ShimmerBlock(18.dp, 18.dp, CircleShape)
                Spacer(Modifier.width(14.dp))
                Column {
                    ShimmerBlock(180.dp, 13.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBlock(260.dp, 13.dp)
                }
            }
            GlassDivider()
        }
    }
}
