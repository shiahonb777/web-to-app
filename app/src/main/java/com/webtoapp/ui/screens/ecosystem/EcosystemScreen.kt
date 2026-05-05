package com.webtoapp.ui.screens.ecosystem

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.net.Uri
import coil.compose.AsyncImage
import com.webtoapp.core.cloud.CloudApiClient
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun EcosystemScreen(
    viewModel: EcosystemViewModel = koinViewModel(),
    initialType: String = "all",
    screenTitle: String = Strings.ecosystemTitle,
    screenSubtitle: String = Strings.ecosystemSubtitle,
    onNavigateToItem: (String, Int) -> Unit,
    onNavigateToUser: (Int) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onPublishApp: () -> Unit,
    onPublishModule: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val composerState by viewModel.composerState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient: CloudApiClient = koinInject()
    val listState = rememberLazyListState()
    var showPublishMenu by remember { mutableStateOf(false) }
    var showPostDialog by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<List<EcosystemPreviewImage>>(emptyList()) }
    var previewIndex by remember { mutableStateOf(0) }

    LaunchedEffect(initialType) {
        if (viewModel.uiState.value.selectedType != initialType) {
            viewModel.setType(initialType)
        } else if (viewModel.uiState.value.items.isEmpty()) {
            viewModel.refresh()
        }
    }

    val lastVisible by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(lastVisible, state.items.size) {
        if (lastVisible >= state.items.size - 3) viewModel.loadMore()
    }

    WtaScreen(
        title = screenTitle,
        subtitle = screenSubtitle,
        actions = {
            IconButton(onClick = { showSearch = !showSearch }) {
                Icon(Icons.Outlined.Search, contentDescription = Strings.ecosystemSearch)
            }
            IconButton(onClick = onNavigateToNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = Strings.ecosystemNotifications)
            }
            IconButton(onClick = onNavigateToFavorites) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = Strings.ecosystemBookmarks)
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showPublishMenu = true },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = Strings.ecosystemPublish)
                }
                DropdownMenu(expanded = showPublishMenu, onDismissRequest = { showPublishMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(Strings.ecosystemCreatePost) },
                        leadingIcon = { Icon(Icons.Outlined.Forum, null) },
                        onClick = { showPublishMenu = false; showPostDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.ecosystemPublishApp) },
                        leadingIcon = { Icon(Icons.Outlined.Apps, null) },
                        onClick = { showPublishMenu = false; onPublishApp() }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.ecosystemPublishModule) },
                        leadingIcon = { Icon(Icons.Outlined.Code, null) },
                        onClick = { showPublishMenu = false; onPublishModule() }
                    )
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
            if (showSearch) {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = viewModel::setSearch,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(Strings.ecosystemSearchPlaceholder) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(WtaRadius.Control)
                )
            }

            EcosystemTypeTabs(
                selected = state.selectedType,
                onSelected = viewModel::setType
            )

            state.error?.let {
                WtaStatusBanner(
                    title = Strings.ecosystemLoadFailed,
                    message = it,
                    tone = WtaStatusTone.Error,
                    actionLabel = Strings.ecosystemRetry,
                    onAction = viewModel::refresh
                )
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.items.isEmpty()) {
                WtaEmptyState(
                    title = Strings.ecosystemEmptyTitle,
                    message = Strings.ecosystemEmptyMessage,
                    icon = Icons.Outlined.Forum
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)
                ) {
                    items(state.items, key = { "${it.type}-${it.id}" }) { item ->
                        EcosystemItemCard(
                            item = item,
                            onClick = { onNavigateToItem(item.type, item.id) },
                            onAuthorClick = { onNavigateToUser(item.author.id) },
                            onLike = { viewModel.toggleLike(item.type, item.id) },
                            onBookmark = { viewModel.toggleBookmark(item.type, item.id) },
                            onPreviewImages = { images, index ->
                                previewImages = images
                                previewIndex = index
                            }
                        )
                    }
                    if (state.loadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPostDialog) {
        CreatePostDialog(
            composerState = composerState,
            onDismiss = { showPostDialog = false },
            onImageSelected = viewModel::initializeComposerUploads,
            onRemoveImage = viewModel::removeComposerUpload,
            onRetryUploads = {
                scope.launch {
                    val uploadResult = viewModel.retryFailedUploads(
                        context = context,
                        uploadAsset = apiClient::uploadAsset
                    )
                    if (uploadResult is com.webtoapp.core.auth.AuthResult.Error) {
                        viewModel.showMessage(uploadResult.message)
                    }
                }
            },
            onPublish = { title, content, tags ->
                scope.launch {
                    val uploadResult = viewModel.retryFailedUploads(
                        context = context,
                        uploadAsset = apiClient::uploadAsset
                    )
                    when (uploadResult) {
                        is com.webtoapp.core.auth.AuthResult.Success -> {
                            viewModel.createPost(
                                content = content,
                                title = title,
                                tags = tags,
                                media = uploadResult.data
                            ) { success ->
                                if (success) showPostDialog = false
                            }
                        }
                        is com.webtoapp.core.auth.AuthResult.Error -> {
                            viewModel.showMessage(uploadResult.message)
                        }
                    }
                }
            }
        )
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
fun EcosystemTypeTabs(selected: String, onSelected: (String) -> Unit) {
    val tabs: List<Pair<String, String>> = listOf(
        "all" to Strings.ecosystemTabAll,
        "post" to Strings.ecosystemTabPosts,
        "app" to Strings.ecosystemTabApps,
        "module" to Strings.ecosystemTabModules
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { (key, label) ->
            AssistChip(
                onClick = { onSelected(key) },
                label = { Text(label) },
                leadingIcon = if (selected == key) {
                    { Icon(iconForType(key), null, Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
fun EcosystemItemCard(
    item: EcosystemItem,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onPreviewImages: (List<EcosystemPreviewImage>, Int) -> Unit
) {
    val mediaPreviewUrls = remember(item.media) {
        item.media.mapNotNull { media ->
            val original = media.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val preview = media.thumbnailUrl?.takeIf { it.isNotBlank() } ?: original
            EcosystemPreviewImage(originalUrl = original, previewUrl = preview)
        }
    }
    WtaSettingCard(onClick = onClick) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val publishTime = ecosystemPublishTimeLabel(item.createdAt)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconForType(item.type), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    typeLabel(item.type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (publishTime != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        publishTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        width = 44.dp,
                        height = 44.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(WtaRadius.IconPlate))
                            .clickable {
                                onPreviewImages(
                                    listOf(EcosystemPreviewImage(originalUrl = item.icon, previewUrl = item.icon)),
                                    0
                                )
                            }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.summary.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (mediaPreviewUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        mediaPreviewUrls.take(4).size,
                        key = { index -> "${item.type}-${item.id}-media-$index-${mediaPreviewUrls[index].originalUrl}" }
                    ) { index ->
                        CommunityImage(
                            url = mediaPreviewUrls[index].previewUrl,
                            width = 108.dp,
                            height = 108.dp,
                            modifier = Modifier
                                .size(108.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(WtaRadius.Card))
                                .clickable { onPreviewImages(mediaPreviewUrls, index) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    name = item.author.username,
                    avatarUrl = item.author.avatarUrl,
                    size = 26,
                    modifier = Modifier.clickable(onClick = onAuthorClick)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.author.displayName ?: item.author.username,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onLike) {
                    Icon(Icons.Outlined.ThumbUp, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${item.stats.likes}")
                }
                TextButton(onClick = onBookmark) {
                    Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(16.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SmallStat(Icons.Outlined.ChatBubbleOutline, "${item.stats.comments}")
                if (item.type != "post") SmallStat(Icons.Outlined.CloudDownload, "${item.stats.downloads}")
                if (item.stats.rating > 0f) Text(Strings.ecosystemRatingLabel.format(item.stats.rating), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SmallStat(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CreatePostDialog(
    composerState: com.webtoapp.ui.viewmodel.EcosystemPostComposerState,
    onDismiss: () -> Unit,
    onImageSelected: (List<Uri>) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onRetryUploads: () -> Unit,
    onPublish: (String?, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) onImageSelected(uris)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.ecosystemCreatePost) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(Strings.ecosystemPostTitleLabel) }, singleLine = true)
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text(Strings.ecosystemPostContentLabel) }, minLines = 4)
                OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text(Strings.ecosystemPostTagsLabel) }, singleLine = true)
                FilledTonalButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(Strings.uploadPostImage)
                }
                composerState.uploadMessage?.let { message ->
                    WtaStatusBanner(
                        message = message,
                        tone = WtaStatusTone.Error,
                        actionLabel = Strings.retry,
                        onAction = onRetryUploads
                    )
                }
                if (composerState.uploads.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(composerState.uploads, key = { it.uri.toString() }) { upload ->
                            Column(
                                modifier = Modifier.width(84.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AsyncImage(
                                    model = upload.uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(WtaRadius.Card))
                                        .clickable(enabled = !upload.uploading) { onRemoveImage(upload.uri) },
                                    contentScale = ContentScale.Crop
                                )
                                when {
                                    upload.uploading -> {
                                        LinearProgressIndicator(
                                            progress = { upload.progress.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    upload.error != null -> {
                                        Text(
                                            upload.error,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    upload.uploadedUrl != null -> {
                                        Text(
                                            "${(upload.progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                enabled = content.isNotBlank() && !composerState.uploading,
                onClick = {
                    onPublish(
                        title.takeIf { it.isNotBlank() },
                        content,
                        tags.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }.take(5)
                    )
                }
            ) { Text(Strings.ecosystemPublish) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.ecosystemCancel) } }
    )
}

fun iconForType(type: String): ImageVector = when (type) {
    "app" -> Icons.Outlined.Apps
    "module" -> Icons.Outlined.Code
    else -> Icons.Outlined.Forum
}

fun typeLabel(type: String): String = when (type) {
    "app" -> Strings.ecosystemTabApps
    "module" -> Strings.ecosystemTabModules
    "post" -> Strings.ecosystemTabPosts
    else -> Strings.ecosystemTabAll
}
