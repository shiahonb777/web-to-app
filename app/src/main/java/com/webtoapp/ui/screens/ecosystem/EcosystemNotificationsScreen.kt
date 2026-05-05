package com.webtoapp.ui.screens.ecosystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.cloud.EcosystemNotification
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.design.WtaEmptyState
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.viewmodel.EcosystemViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EcosystemNotificationsScreen(
    viewModel: EcosystemViewModel = koinViewModel(),
    onBack: () -> Unit,
    onNavigateToItem: (String, Int) -> Unit,
    onNavigateToUser: (Int) -> Unit
) {
    val state by viewModel.notificationsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    WtaScreen(
        title = Strings.ecosystemNotifications,
        subtitle = if (state.unreadCount > 0) Strings.ecosystemUnreadCount.format(state.unreadCount) else Strings.ecosystemNotificationsSubtitle,
        onBack = onBack,
        actions = {
            if (state.unreadCount > 0) {
                IconButton(onClick = viewModel::markAllNotificationsRead) {
                    Icon(Icons.Outlined.DoneAll, contentDescription = Strings.ecosystemMarkAllRead)
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WtaSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)
        ) {
            state.error?.let {
                WtaStatusBanner(
                    title = Strings.ecosystemLoadFailed,
                    message = it,
                    tone = WtaStatusTone.Error,
                    actionLabel = Strings.ecosystemRetry,
                    onAction = viewModel::loadNotifications
                )
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.notifications.isEmpty() -> WtaEmptyState(
                    title = Strings.ecosystemNoNotificationsTitle,
                    message = Strings.ecosystemNoNotificationsMessage,
                    icon = Icons.Outlined.Notifications
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        EcosystemNotificationRow(
                            notification = notification,
                            onClick = {
                                viewModel.markNotificationRead(notification.id) {
                                    when (notification.refType) {
                                        "post", "app", "module" -> notification.refId?.let {
                                            onNavigateToItem(notification.refType, it)
                                        }
                                        "user" -> notification.refId?.let(onNavigateToUser)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EcosystemNotificationRow(
    notification: EcosystemNotification,
    onClick: () -> Unit
) {
    WtaSettingCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(7.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Icon(
                notificationIcon(notification.type),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    notification.title ?: Strings.ecosystemNotifications,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                notification.content?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.actor.displayName ?: notification.actor.username,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    notification.createdAt?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

private fun notificationIcon(type: String): ImageVector = when (type) {
    "comment" -> Icons.Outlined.ChatBubbleOutline
    "like" -> Icons.Outlined.ThumbUp
    "follow" -> Icons.Outlined.PersonAdd
    "system" -> Icons.Outlined.Campaign
    else -> Icons.Outlined.Notifications
}
