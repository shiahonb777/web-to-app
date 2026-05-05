package com.webtoapp.ui.screens.ecosystem

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.cloud.EcosystemComment
import com.webtoapp.core.cloud.EcosystemItem
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.image.CommunityImage
import com.webtoapp.ui.design.WtaEmptyState
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.viewmodel.EcosystemViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EcosystemDetailScreen(
    type: String,
    id: Int,
    viewModel: EcosystemViewModel = koinViewModel(),
    onBack: () -> Unit,
    onNavigateToUser: (Int) -> Unit,
    onDownloadApp: suspend (EcosystemItem, String) -> Unit,
    onInstallModule: suspend (String) -> Result<Unit>
) {
    val state by viewModel.detailState.collectAsState()
    var comment by remember { mutableStateOf("") }
    var previewImages by remember { mutableStateOf<List<EcosystemPreviewImage>>(emptyList()) }
    var previewIndex by remember { mutableStateOf(0) }

    LaunchedEffect(type, id) {
        viewModel.loadDetail(type, id)
    }

    WtaScreen(
        title = typeLabel(type),
        onBack = onBack,
        actions = {
            state.item?.let { item ->
                IconButton(onClick = { viewModel.toggleLike(item.type, item.id) }) {
                    Icon(Icons.Outlined.ThumbUp, contentDescription = Strings.ecosystemLike)
                }
                IconButton(onClick = { viewModel.toggleBookmark(item.type, item.id) }) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = Strings.ecosystemBookmarks)
                }
            }
        }
    ) {
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@WtaScreen
        }

        val item = state.item
        if (item == null) {
            WtaEmptyState(
                title = Strings.ecosystemContentMissingTitle,
                message = state.error ?: Strings.ecosystemContentMissingMessage,
                modifier = Modifier.padding(WtaSpacing.ScreenHorizontal)
            )
            return@WtaScreen
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WtaSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)
        ) {
            item {
                EcosystemDetailHeader(
                    item = item,
                    actionLoading = state.actionLoading,
                    onNavigateToUser = onNavigateToUser,
                    onPreviewImages = { images, index ->
                        previewImages = images
                        previewIndex = index
                    },
                    onDownloadApp = {
                        viewModel.downloadApp(item) { url -> onDownloadApp(item, url) }
                    },
                    onInstallModule = {
                        viewModel.installModule(item, onInstallModule)
                    }
                )
            }
            state.error?.let { error ->
                item {
                    WtaStatusBanner(message = error, tone = WtaStatusTone.Error)
                }
            }
            state.message?.let { message ->
                item {
                    WtaStatusBanner(message = message, tone = WtaStatusTone.Success)
                }
            }
            item {
                WtaSettingCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(Strings.ecosystemDiscussion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(Strings.ecosystemCommentPlaceholder) },
                                minLines = 1,
                                maxLines = 4,
                                shape = RoundedCornerShape(WtaRadius.Control)
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                enabled = comment.isNotBlank() && !state.commentLoading,
                                onClick = {
                                    viewModel.addComment(item.type, item.id, comment)
                                    comment = ""
                                },
                                shape = RoundedCornerShape(WtaRadius.Button)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            if (state.comments.isEmpty()) {
                item {
                    WtaEmptyState(
                        title = Strings.ecosystemNoCommentsTitle,
                        message = Strings.ecosystemNoCommentsMessage,
                        icon = Icons.Outlined.ChatBubbleOutline
                    )
                }
            } else {
                items(state.comments, key = { it.id }) { commentItem ->
                    EcosystemCommentRow(commentItem)
                }
            }
        }
    }

    if (previewImages.isNotEmpty()) {
        EcosystemImagePreviewDialog(
            images = previewImages,
            initialIndex = previewIndex,
            onDismiss = {
                previewImages = emptyList()
                previewIndex = 0
            }
        )
    }
}

@Composable
private fun EcosystemDetailHeader(
    item: EcosystemItem,
    actionLoading: Boolean,
    onNavigateToUser: (Int) -> Unit,
    onPreviewImages: (List<EcosystemPreviewImage>, Int) -> Unit,
    onDownloadApp: () -> Unit,
    onInstallModule: () -> Unit
) {
    val mediaPreviewUrls = remember(item.media) {
        item.media.mapNotNull { media ->
            val original = media.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val preview = media.thumbnailUrl?.takeIf { it.isNotBlank() } ?: original
            EcosystemPreviewImage(originalUrl = original, previewUrl = preview)
        }
    }
    WtaSettingCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val publishTime = ecosystemPublishTimeLabel(item.createdAt)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconForType(item.type), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(typeLabel(item.type), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Text(item.category, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                if (publishTime != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        publishTime,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!item.icon.isNullOrBlank()) {
                    CommunityImage(
                        url = item.icon,
                        width = 58.dp,
                        height = 58.dp,
                        modifier = Modifier
                            .size(58.dp)
                            .clickable {
                                onPreviewImages(
                                    listOf(EcosystemPreviewImage(originalUrl = item.icon, previewUrl = item.icon)),
                                    0
                                )
                            }
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onNavigateToUser(item.author.id) }, contentPadding = PaddingValues(0.dp)) {
                        Text(item.author.displayName ?: item.author.username, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (mediaPreviewUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(
                        mediaPreviewUrls.size,
                        key = { index -> "${item.type}-${item.id}-media-$index-${mediaPreviewUrls[index].originalUrl}" }
                    ) { index ->
                        CommunityImage(
                            url = mediaPreviewUrls[index].previewUrl,
                            width = 180.dp,
                            height = 180.dp,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(WtaRadius.Card))
                                .clickable { onPreviewImages(mediaPreviewUrls, index) }
                        )
                    }
                }
            }

            if (item.content.isNotBlank()) {
                Text(item.content, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("${Strings.ecosystemLike} ${item.stats.likes}")
                StatChip("${Strings.ecosystemComment} ${item.stats.comments}")
                if (item.type != "post") StatChip("${Strings.ecosystemDownload} ${item.stats.downloads}")
            }

            if (item.type == "app") {
                MetaLine(Strings.ecosystemPackageName, item.meta["package_name"])
                MetaLine(Strings.ecosystemVersion, item.meta["version_name"])
            }
            if (item.type == "module") {
                MetaLine(Strings.ecosystemModuleType, item.meta["module_type"])
                MetaLine(Strings.ecosystemVersion, item.meta["version_name"])
            }

            EcosystemDetailAction(
                item = item,
                loading = actionLoading,
                onDownloadApp = onDownloadApp,
                onInstallModule = onInstallModule
            )
        }
    }
}

@Composable
private fun StatChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(WtaRadius.Button))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EcosystemDetailAction(
    item: EcosystemItem,
    loading: Boolean,
    onDownloadApp: () -> Unit,
    onInstallModule: () -> Unit
) {
    when (item.type) {
        "app" -> FilledTonalButton(
            enabled = !loading,
            onClick = onDownloadApp,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(WtaRadius.Button)
        ) {
            Icon(Icons.Outlined.CloudDownload, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (loading) Strings.ecosystemPreparingDownload else Strings.ecosystemDownloadApp)
        }
        "module" -> FilledTonalButton(
            enabled = !loading,
            onClick = onInstallModule,
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(WtaRadius.Button)
        ) {
            Icon(Icons.Outlined.Code, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (loading) Strings.ecosystemInstallingModule else Strings.ecosystemInstallModule)
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row {
        Text("$label：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EcosystemCommentRow(comment: EcosystemComment) {
    WtaSettingCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.author.displayName ?: comment.author.username,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                formatEcosystemRelativeTime(comment.createdAt)?.let { time ->
                    Text(
                        time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
            if (comment.likeCount > 0) {
                Text("${Strings.ecosystemLike} ${comment.likeCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            comment.replies.forEach { reply ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "${reply.author.displayName ?: reply.author.username}：${reply.content}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
