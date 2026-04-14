package com.webtoapp.ui.screens.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import com.webtoapp.core.cloud.CommunityUserProfile
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.CommunityPostItem
import com.webtoapp.core.cloud.PostCommentItem
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Premium Twitter/X Style
 * content, , app, ( spring) , list,
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    communityViewModel: com.webtoapp.ui.viewmodel.CommunityViewModel,
    onBack: () -> Unit,
    onNavigateToUser: (Int) -> Unit
) {
    val apiClient: CloudApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var post by remember { mutableStateOf<CommunityPostItem?>(null) }
    var comments by remember { mutableStateOf<List<PostCommentItem>>(emptyList()) }
    var commentTotal by remember { mutableIntStateOf(0) }   // CLI-06: total from server
    var commentPage by remember { mutableIntStateOf(1) }     // CLI-12: pagination
    var hasMoreComments by remember { mutableStateOf(false) }
    var loadingMoreComments by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<PostCommentItem?>(null) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf("") }
    val commentFocusRequester = remember { FocusRequester() }

    // @mention state
    var showMentionPopup by remember { mutableStateOf(false) }
    var mentionFilter by remember { mutableStateOf("") }
    val mentionResults by communityViewModel.mentionResults.collectAsState()
    val mentionLoading by communityViewModel.mentionLoading.collectAsState()

    // Load post detail + comments
    LaunchedEffect(postId) {
        loading = true
        val postResult = apiClient.getCommunityPost(postId)
        if (postResult is AuthResult.Success) {
            post = postResult.data
        }
        val commentsResult = apiClient.listPostComments(postId, page = 1)
        if (commentsResult is AuthResult.Success) {
            val resp = commentsResult.data
            comments = resp.comments
            commentTotal = resp.total
            commentPage = 1
            hasMoreComments = resp.comments.size < resp.total
        }
        loading = false
    }

    // Refresh helper
    fun refreshComments() {
        scope.launch {
            val refreshed = apiClient.listPostComments(postId, page = 1)
            if (refreshed is AuthResult.Success) {
                comments = refreshed.data.comments
                commentTotal = refreshed.data.total
                commentPage = 1
                hasMoreComments = refreshed.data.comments.size < refreshed.data.total
            }
            // Also refresh the post to get latest counts
            val refreshedPost = apiClient.getCommunityPost(postId)
            if (refreshedPost is AuthResult.Success) post = refreshedPost.data
        }
    }

    // CLI-12: Load more comments
    fun loadMoreComments() {
        if (loadingMoreComments || !hasMoreComments) return
        loadingMoreComments = true
        scope.launch {
            val nextPage = commentPage + 1
            val result = apiClient.listPostComments(postId, page = nextPage)
            if (result is AuthResult.Success) {
                comments = comments + result.data.comments
                commentPage = nextPage
                hasMoreComments = comments.size < result.data.total
            }
            loadingMoreComments = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(Strings.communityPost, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { showMoreSheet = true }) {
                        Icon(Icons.Outlined.MoreHoriz, null, Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            )
        },
        bottomBar = {
            FrostedBottomBar {
                Column {
                    // Reply indicator
                    AnimatedVisibility(
                        visible = replyingTo != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        replyingTo?.let { reply ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.SubdirectoryArrowRight, null, Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                                append("@${reply.authorDisplayName ?: reply.authorUsername}")
                                            }
                                            append(" ")
                                            append(reply.content.take(40))
                                        },
                                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Filled.Close, null, Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    // @mention popup overlay
                    AnimatedVisibility(
                        visible = showMentionPopup,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.AlternateEmail, null, Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        Strings.communityMentionSelectUser,
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            showMentionPopup = false
                                            communityViewModel.clearMentionResults()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, null, Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                }
                                if (mentionLoading) {
                                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    }
                                } else if (mentionResults.isEmpty() && mentionFilter.isNotBlank()) {
                                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                        Text(Strings.communityNoUsersFound, fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    }
                                } else {
                                    mentionResults.forEach { user ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable {
                                                    // Insert @username into commentText
                                                    val atIdx = commentText.lastIndexOf('@')
                                                    if (atIdx >= 0) {
                                                        commentText = commentText.substring(0, atIdx) + "@${user.username} "
                                                    } else {
                                                        commentText += "@${user.username} "
                                                    }
                                                    showMentionPopup = false
                                                    communityViewModel.clearMentionResults()
                                                }
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(28.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer
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
                                                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    user.displayName ?: user.username,
                                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "@${user.username}", fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                            if (user.isDeveloper) {
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Filled.Verified, null, Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { newText ->
                                commentText = newText
                                // Detect @mention trigger
                                val atIdx = newText.lastIndexOf('@')
                                if (atIdx >= 0 && (atIdx == 0 || newText[atIdx - 1] == ' ' || newText[atIdx - 1] == '\n')) {
                                    val afterAt = newText.substring(atIdx + 1)
                                    // Only trigger if the text after @ doesn't contain spaces (still typing username)
                                    if (!afterAt.contains(' ') && afterAt.length <= 20) {
                                        showMentionPopup = true
                                        mentionFilter = afterAt
                                        if (afterAt.isNotBlank()) {
                                            communityViewModel.searchMentions(afterAt)
                                        }
                                    } else {
                                        showMentionPopup = false
                                    }
                                } else {
                                    showMentionPopup = false
                                }
                            },
                            placeholder = {
                                Text(
                                    if (replyingTo != null) "${Strings.communityComment}..." else Strings.communityPostYourReply,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(max = 96.dp)
                                .focusRequester(commentFocusRequester),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        val sendEnabled = commentText.isNotBlank() && !submitting
                        val sendAlpha by animateFloatAsState(
                            if (sendEnabled) 1f else 0.35f, tween(200), label = "sendA"
                        )
                        FilledIconButton(
                            onClick = {
                                if (sendEnabled) {
                                    submitting = true
                                    scope.launch {
                                        val parentId = replyingTo?.id
                                        val result = apiClient.addPostComment(postId, commentText.trim(), parentId)
                                        if (result is AuthResult.Success) {
                                            commentText = ""
                                            replyingTo = null
                                            refreshComments()
                                        } else if (result is AuthResult.Error) {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                        submitting = false
                                    }
                                }
                            },
                            enabled = sendEnabled,
                            modifier = Modifier.size(40.dp).graphicsLayer { alpha = sendAlpha },
                            shape = CircleShape
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (loading) {
                DetailShimmer()
            } else {
                post?.let { p ->
                    LazyColumn {
                        // ═══ Post Body ═══
                        item {
                            StaggeredItem(index = 0) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    // ── Author header ──
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { onNavigateToUser(p.authorId) }
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(46.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            if (p.authorAvatarUrl != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(p.authorAvatarUrl).crossfade(true).build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        (p.authorDisplayName ?: p.authorUsername).take(1).uppercase(),
                                                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(p.authorDisplayName ?: p.authorUsername,
                                                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                if (p.authorIsDeveloper) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Verified, null, Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Text(
                                                "@${p.authorUsername} · ${formatTimeAgo(p.createdAt)}",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            )
                                        }
                                        // CLI-09: Follow button (only for others' posts)
                                        if (!p.isOwnPost) {
                                            val isFollowing = p.authorIsFollowing
                                            Surface(
                                                modifier = Modifier.clickable {
                                                    communityViewModel.toggleFollow(p.authorId)
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isFollowing) Color.Transparent
                                                    else MaterialTheme.colorScheme.primary,
                                                border = if (isFollowing) BorderStroke(1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null
                                            ) {
                                                Text(
                                                    if (isFollowing) Strings.communityFollowing else Strings.communityFollow,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                                    color = if (isFollowing) MaterialTheme.colorScheme.onSurface
                                                        else MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }

                                    // ── Phase 1 v2: Type badge + Title ──
                                    if (p.postType != "discussion") {
                                        Spacer(Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val (typeIcon, typeLabel, typeColor) = when (p.postType) {
                                                "showcase" -> Triple(
                                                    Icons.Outlined.Palette,
                                                    stringResource(com.webtoapp.R.string.community_post_type_showcase),
                                                    Color(0xFF6C5CE7)
                                                )
                                                "tutorial" -> Triple(
                                                    Icons.Outlined.MenuBook,
                                                    stringResource(com.webtoapp.R.string.community_post_type_tutorial),
                                                    Color(0xFF4CAF50)
                                                )
                                                "question" -> Triple(
                                                    Icons.Outlined.HelpOutline,
                                                    stringResource(com.webtoapp.R.string.community_post_type_question),
                                                    Color(0xFFFF9800)
                                                )
                                                else -> Triple(
                                                    Icons.Outlined.ChatBubble,
                                                    stringResource(com.webtoapp.R.string.community_post_type_update),
                                                    Color(0xFF9E9E9E)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = typeColor.copy(alpha = 0.1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(typeIcon, null, Modifier.size(14.dp), tint = typeColor)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(typeLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = typeColor)
                                                }
                                            }
                                            // Difficulty badge for tutorials
                                            if (p.postType == "tutorial" && p.difficulty != null) {
                                                Spacer(Modifier.width(8.dp))
                                                val (diffLabel, diffColor) = when (p.difficulty) {
                                                    "beginner" -> stringResource(com.webtoapp.R.string.community_difficulty_beginner) to Color(0xFF4CAF50)
                                                    "intermediate" -> stringResource(com.webtoapp.R.string.community_difficulty_intermediate) to Color(0xFFFF9800)
                                                    "advanced" -> stringResource(com.webtoapp.R.string.community_difficulty_advanced) to Color(0xFFE91E63)
                                                    else -> p.difficulty!! to Color(0xFF9E9E9E)
                                                }
                                                Surface(shape = RoundedCornerShape(6.dp), color = diffColor.copy(alpha = 0.1f)) {
                                                    Text(diffLabel,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = diffColor)
                                                }
                                            }
                                            // Resolved badge for questions
                                            if (p.postType == "question" && p.isResolved == true) {
                                                Spacer(Modifier.width(8.dp))
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50).copy(alpha = 0.1f)) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(12.dp), tint = Color(0xFF4CAF50))
                                                        Spacer(Modifier.width(3.dp))
                                                        Text(
                                                            stringResource(com.webtoapp.R.string.community_resolved),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF4CAF50)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // ── Phase 1 v2: Title (Tutorial/Question) ──
                                    if (p.title != null && p.postType in listOf("tutorial", "question")) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            p.title!!, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                            lineHeight = 28.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // ── Phase 1 v2: Showcase info card ──
                                    if (p.postType == "showcase") {
                                        Spacer(Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF6C5CE7).copy(alpha = 0.06f),
                                            border = BorderStroke(0.5.dp, Color(0xFF6C5CE7).copy(alpha = 0.15f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(Modifier.padding(14.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    // App icon
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = Color(0xFF6C5CE7).copy(alpha = 0.12f),
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        if (p.appIconUrl != null) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(p.appIconUrl).crossfade(true).build(),
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize()
                                                                    .clip(RoundedCornerShape(12.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                Icon(Icons.Outlined.Apps, null, Modifier.size(28.dp),
                                                                    tint = Color(0xFF6C5CE7))
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            p.appName ?: stringResource(com.webtoapp.R.string.community_work_default),
                                                            fontWeight = FontWeight.Bold, fontSize = 17.sp
                                                        )
                                                        if (p.sourceType != null) {
                                                            val sourceLabel = when (p.sourceType) {
                                                                "website" -> stringResource(com.webtoapp.R.string.community_source_website)
                                                                "html" -> stringResource(com.webtoapp.R.string.community_source_html)
                                                                "media" -> stringResource(com.webtoapp.R.string.community_source_media)
                                                                "frontend" -> stringResource(com.webtoapp.R.string.community_source_frontend)
                                                                "server" -> stringResource(com.webtoapp.R.string.community_source_server)
                                                                else -> p.sourceType!!
                                                            }
                                                            Text(
                                                                stringResource(com.webtoapp.R.string.community_source_format, sourceLabel),
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                    }
                                                }
                                                // Recipe import button
                                                if (p.hasRecipe) {
                                                    Spacer(Modifier.height(12.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val recipeImportedMsg = stringResource(com.webtoapp.R.string.community_recipe_imported)
                                                        Button(
                                                            onClick = {
                                                                scope.launch {
                                                                    communityViewModel.importRecipe(postId) { _ ->
                                                                        scope.launch {
                                                                            snackbarHostState.showSnackbar(
                                                                                recipeImportedMsg
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF6C5CE7)
                                                            ),
                                                            modifier = Modifier.weight(1f).height(38.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                stringResource(com.webtoapp.R.string.community_import_recipe_one_tap),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                        if (p.recipeImportCount > 0) {
                                                            Spacer(Modifier.width(10.dp))
                                                            Text(
                                                                stringResource(
                                                                    com.webtoapp.R.string.community_import_count,
                                                                    p.recipeImportCount
                                                                ),
                                                                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                                                color = Color(0xFF6C5CE7).copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // ── Content (with @mention highlighting) ──
                                    Spacer(Modifier.height(16.dp))
                                    MentionableText(
                                        text = p.content,
                                        fontSize = 16.sp,
                                        lineHeight = 25.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.93f),
                                        mentionColor = MaterialTheme.colorScheme.primary,
                                        onMentionClick = { username ->
                                            // Resolve @mention username to userId and navigate
                                            communityViewModel.resolveUserByUsername(username) { userId ->
                                                onNavigateToUser(userId)
                                            }
                                        }
                                    )

                                    // ── Tags ──
                                    if (p.tags.isNotEmpty()) {
                                        Spacer(Modifier.height(14.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            p.tags.forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = tagColor(tag).copy(alpha = 0.12f)
                                                ) {
                                                    Text("#$tag",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                                        color = tagColor(tag))
                                                }
                                            }
                                        }
                                    }

                                    // ── Media ──
                                    if (p.media.isNotEmpty()) {
                                        Spacer(Modifier.height(14.dp))
                                        if (p.media.size == 1) {
                                            val m = p.media[0]
                                            val url = m.urlGitee ?: m.urlGithub
                                            if (url != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(url).crossfade(true).build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxWidth()
                                                        .heightIn(max = 320.dp)
                                                        .clip(RoundedCornerShape(14.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            // CLI-13: Multi-image with dot indicators
                                            val mediaListState = rememberLazyListState()
                                            val currentImageIndex by remember {
                                                derivedStateOf {
                                                    mediaListState.firstVisibleItemIndex
                                                }
                                            }
                                            Column {
                                                LazyRow(
                                                    state = mediaListState,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(p.media) { m ->
                                                        val url = m.urlGitee ?: m.urlGithub
                                                        if (url != null) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(url).crossfade(true).build(),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(180.dp)
                                                                    .clip(RoundedCornerShape(12.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                                // Dot indicators
                                                if (p.media.size > 1) {
                                                    Spacer(Modifier.height(8.dp))
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        p.media.forEachIndexed { idx, _ ->
                                                            val isActive = idx == currentImageIndex
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(horizontal = 3.dp)
                                                                    .size(if (isActive) 7.dp else 5.dp)
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
                                    if (p.appLinks.isNotEmpty()) {
                                        Spacer(Modifier.height(14.dp))
                                        p.appLinks.forEach { appLink ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                                border = BorderStroke(0.5.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        if (appLink.appIcon != null) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(appLink.appIcon).crossfade(true).build(),
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize()
                                                                    .clip(RoundedCornerShape(10.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    if (appLink.storeModuleType == "app") Icons.Outlined.Apps
                                                                    else Icons.Outlined.Extension,
                                                                    null, Modifier.size(22.dp),
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.width(10.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            appLink.appName ?: Strings.communityApplication,
                                                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                                        )
                                                        appLink.appDescription?.let {
                                                            Text(it, fontSize = 11.sp, maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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

                                    // ── Timestamp (full) ──
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        formatTimeAgo(p.createdAt),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                            GlassDivider()
                        }

                        // ═══ Stats Row ═══
                        item {
                            StaggeredItem(index = 1) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatPill("${p.likeCount}", Strings.communityLike)
                                    StatPill("${p.commentCount}", Strings.communityComment)
                                    StatPill("${p.shareCount}", Strings.communityShare)
                                    StatPill("${p.viewCount}", Strings.communityViews)
                                    if (p.postType == "showcase" && p.hasRecipe) {
                                        StatPill(
                                            "${p.recipeImportCount}",
                                            stringResource(com.webtoapp.R.string.community_import)
                                        )
                                    }
                                }
                            }
                            GlassDivider()
                        }

                        // ═══ Action Bar (physics-based) ═══
                        item {
                            StaggeredItem(index = 2) {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    DetailActionButton(
                                        icon = Icons.Outlined.FavoriteBorder,
                                        activeIcon = Icons.Filled.Favorite,
                                        label = Strings.communityLike,
                                        isActive = p.isLiked,
                                        activeColor = Color(0xFFE91E63),
                                        onClick = {
                                            scope.launch {
                                                val result = apiClient.togglePostLike(postId)
                                                if (result is AuthResult.Success) {
                                                    post = p.copy(
                                                        isLiked = result.data.liked,
                                                        likeCount = result.data.likeCount
                                                    )
                                                } else if (result is AuthResult.Error) {
                                                    snackbarHostState.showSnackbar(result.message)
                                                }
                                            }
                                        }
                                    )
                                    DetailActionButton(
                                        icon = Icons.Outlined.ChatBubbleOutline,
                                        activeIcon = Icons.Outlined.ChatBubbleOutline,
                                        label = Strings.communityComment,
                                        isActive = false,
                                        activeColor = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            // CLI-14: Focus the comment input
                                            scope.launch {
                                                try { commentFocusRequester.requestFocus() } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                    DetailActionButton(
                                        icon = Icons.Outlined.Repeat,
                                        activeIcon = Icons.Filled.Repeat,
                                        label = Strings.communityShare,
                                        isActive = false,
                                        activeColor = Color(0xFF4CAF50),
                                        onClick = {
                                            scope.launch {
                                                val result = apiClient.sharePost(postId)
                                                if (result is AuthResult.Success) {
                                                    post = p.copy(shareCount = p.shareCount + 1)
                                                } else if (result is AuthResult.Error) {
                                                    snackbarHostState.showSnackbar(result.message)
                                                }
                                            }
                                        }
                                    )
                                    // CLI-05: Hide bookmark until API exists — show view count instead
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Icon(Icons.Outlined.RemoveRedEye, null, Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${p.viewCount}", fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                    }
                                }
                            }
                            GlassDivider()
                        }

                        // ═══ Comments Section ═══
                        item {
                            StaggeredItem(index = 3) {
                                Text(
                                    "${Strings.communityComment} ($commentTotal)",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                                )
                            }
                        }

                        if (comments.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.Forum, null,
                                            Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(Strings.communityNoRepliesYet,
                                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(2.dp))
                                        Text(Strings.communityBeFirstReply, fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                                    }
                                }
                            }
                        }

                        itemsIndexed(comments, key = { _, c -> c.id }) { index, comment ->
                            StaggeredItem(index = index + 4) {
                                PostCommentRow(
                                    comment = comment,
                                    onUserClick = { onNavigateToUser(comment.authorId) },
                                    onReply = { replyingTo = comment },
                                    onLike = {
                                        // Comment like — future feature
                                    },
                                    onMentionClick = { username ->
                                        communityViewModel.resolveUserByUsername(username) { userId ->
                                            onNavigateToUser(userId)
                                        }
                                    }
                                )
                            }
                            GlassDivider(Modifier.padding(start = 62.dp))
                        }

                        // CLI-12: Load more comments button
                        if (hasMoreComments) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (loadingMoreComments) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        TextButton(onClick = { loadMoreComments() }) {
                                            Text(
                                                Strings.communityShowMoreReplies,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.SearchOff, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text(Strings.communityPostNotFound, fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // ═══ More Sheet ═══
    if (showMoreSheet) {
        ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                // CLI-02: Edit (only for own posts)
                post?.let { p ->
                    if (p.isOwnPost) {
                        Surface(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    showMoreSheet = false
                                    editContent = p.content
                                    showEditSheet = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Edit, null, Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(14.dp))
                                Text(Strings.communityEditPost, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // CLI-01: Delete (only for own posts)
                        Surface(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    showMoreSheet = false
                                    showDeleteConfirm = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Delete, null, Modifier.size(22.dp),
                                    tint = Color(0xFFE57373))
                                Spacer(Modifier.width(14.dp))
                                Text(Strings.communityDeletePost, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium, color = Color(0xFFE57373))
                            }
                        }
                    }
                }

                // Report
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            showMoreSheet = false
                            showReportSheet = true
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Flag, null, Modifier.size(22.dp),
                            tint = Color(0xFFE57373))
                        Spacer(Modifier.width(14.dp))
                        Text(Strings.communityReportTitle, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, color = Color(0xFFE57373))
                    }
                }

                // Share link
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            showMoreSheet = false
                            scope.launch {
                                snackbarHostState.showSnackbar(Strings.communityShare)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Share, null, Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(Modifier.width(14.dp))
                        Text(Strings.communityShare, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // CLI-01: Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    communityViewModel.deletePost(postId, onSuccess = { onBack() })
                }) {
                    Text(Strings.communityConfirmDelete, color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(Strings.communityCancel)
                }
            },
            title = { Text(Strings.communityDeletePost, fontWeight = FontWeight.Bold) },
            text = { Text(Strings.communityDeletePostConfirmMsg) }
        )
    }

    // CLI-02: Edit post sheet
    if (showEditSheet) {
        ModalBottomSheet(onDismissRequest = { showEditSheet = false }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(Strings.communityEditPost, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 10
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ){
                    TextButton(onClick = { showEditSheet = false }) {
                        Text(Strings.communityCancel)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            communityViewModel.editPost(postId, editContent.trim()) { updated ->
                                post = updated
                            }
                            showEditSheet = false
                        },
                        enabled = editContent.isNotBlank()
                    ) {
                        Text(Strings.communitySave)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // CLI-15: Report reason selection
    if (showReportSheet) {
        ReportReasonSheet(
            onDismiss = { showReportSheet = false },
            onReasonSelected = { reason ->
                showReportSheet = false
                communityViewModel.reportPost(postId, reason)
            }
        )
    }
}

// ═══════════════════════════════════════════
// Stats Pill — "42 Likes" inline display
// ═══════════════════════════════════════════

@Composable
private fun StatPill(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(count, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (label.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(label, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// ═══════════════════════════════════════════
// Physics-based action button
// ═══════════════════════════════════════════

@Composable
private fun DetailActionButton(
    icon: ImageVector, activeIcon: ImageVector,
    label: String = "",
    isActive: Boolean, activeColor: Color,
    onClick: () -> Unit
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val currentColor by animateColorAsState(
        if (isActive) activeColor else inactiveColor,
        tween(280), label = "actionClr"
    )

    var bouncing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (bouncing) 1.3f else 1f,
        spring(dampingRatio = 0.35f, stiffness = 600f),
        label = "actionScale",
        finishedListener = { bouncing = false }
    )

    var showBurst by remember { mutableStateOf(false) }
    var burstKey by remember { mutableIntStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            bouncing = true
            if (!isActive) { showBurst = true; burstKey++ }
            onClick()
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            LikeBurstEffect(
                trigger = showBurst,
                color = activeColor,
                modifier = Modifier.size(36.dp)
            )
            Icon(
                if (isActive) activeIcon else icon, null,
                Modifier.size(22.dp).scale(scale), tint = currentColor
            )
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                label, fontSize = 11.sp,
                color = currentColor,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }

    LaunchedEffect(burstKey) {
        if (showBurst) {
            kotlinx.coroutines.delay(500)
            showBurst = false
        }
    }
}

// ═══════════════════════════════════════════
// Comment Row — with reply button
// ═══════════════════════════════════════════

@Composable
private fun PostCommentRow(
    comment: PostCommentItem,
    onUserClick: () -> Unit,
    onReply: () -> Unit,
    onLike: () -> Unit,
    onMentionClick: (String) -> Unit = {}
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row {
            Surface(
                modifier = Modifier.size(36.dp).clickable(onClick = onUserClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (comment.authorAvatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(comment.authorAvatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            (comment.authorDisplayName ?: comment.authorUsername).take(1).uppercase(),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        comment.authorDisplayName ?: comment.authorUsername,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onUserClick)
                    )
                    if (comment.authorIsDeveloper) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Filled.Verified, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    comment.createdAt?.let {
                        Text("  ·  ", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Text(formatTimeAgo(it), fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    }
                }
                Spacer(Modifier.height(3.dp))
                MentionableText(
                    text = comment.content,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    mentionColor = MaterialTheme.colorScheme.primary,
                    onMentionClick = onMentionClick
                )

                // Action row
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reply button
                    Row(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onReply
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SubdirectoryArrowRight, null, Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.width(3.dp))
                        Text(Strings.communityComment, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                    // Like count
                    if (comment.likeCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Spacer(Modifier.width(3.dp))
                            Text("${comment.likeCount}", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                }

                // Nested replies
                if (comment.replies.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        comment.replies.take(3).forEach { reply ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            (reply.authorDisplayName ?: reply.authorUsername).take(1).uppercase(),
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            reply.authorDisplayName ?: reply.authorUsername,
                                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        reply.createdAt?.let {
                                            Text(" · ${formatTimeAgo(it)}", fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                        }
                                    }
                                    Text(
                                        reply.content, fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 3, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (comment.replies.size > 3) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                Strings.communityShowMoreReplies,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// CLI-15: Report reason selection sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportReasonSheet(
    onDismiss: () -> Unit,
    onReasonSelected: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(Strings.communityReportWhy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))

            val reasons = listOf(
                "spam" to Strings.communityReportSpam,
                "inappropriate" to Strings.communityReportInappropriate,
                "malicious" to Strings.communityReportMalicious,
                "copyright" to Strings.communityReportCopyright,
                "other" to Strings.communityReportOther,
            )
            reasons.forEach { (key, label) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReasonSelected(key) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (key) {
                                "spam" -> Icons.Outlined.Report
                                "inappropriate" -> Icons.Outlined.Block
                                "malicious" -> Icons.Outlined.BugReport
                                "copyright" -> Icons.Outlined.Copyright
                                else -> Icons.Outlined.MoreHoriz
                            },
                            null, Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(label, fontSize = 15.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ═══════════════════════════════════════════
// Shimmer skeleton
// ═══════════════════════════════════════════

@Composable
private fun DetailShimmer() {
    Column(Modifier.padding(16.dp)) {
        // Author
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBlock(46.dp, 46.dp, CircleShape)
            Spacer(Modifier.width(10.dp))
            Column {
                ShimmerBlock(140.dp, 15.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBlock(100.dp, 12.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        // Content
        ShimmerBlock(300.dp, 16.dp)
        Spacer(Modifier.height(8.dp))
        ShimmerBlock(280.dp, 16.dp)
        Spacer(Modifier.height(8.dp))
        ShimmerBlock(200.dp, 16.dp)
        Spacer(Modifier.height(16.dp))
        // Tags
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { ShimmerBlock(60.dp, 22.dp, RoundedCornerShape(6.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        // Stats
        GlassDivider()
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { ShimmerBlock(50.dp, 14.dp) }
        }
        Spacer(Modifier.height(10.dp))
        GlassDivider()
        Spacer(Modifier.height(12.dp))
        // Action bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(4) { ShimmerBlock(28.dp, 22.dp) }
        }
        Spacer(Modifier.height(12.dp))
        GlassDivider()
        // Comments
        repeat(3) {
            Spacer(Modifier.height(14.dp))
            Row {
                ShimmerBlock(36.dp, 36.dp, CircleShape)
                Spacer(Modifier.width(10.dp))
                Column {
                    ShimmerBlock(120.dp, 13.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBlock(260.dp, 14.dp)
                    Spacer(Modifier.height(4.dp))
                    ShimmerBlock(180.dp, 14.dp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// MentionableText — @mention highlighting
// ═══════════════════════════════════════════

@Composable
fun MentionableText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 20.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    mentionColor: Color = MaterialTheme.colorScheme.primary,
    onMentionClick: (String) -> Unit = {}
) {
    val annotated = buildAnnotatedString {
        val regex = Regex("@(\\w{2,20})")
        var lastEnd = 0
        regex.findAll(text).forEach { match ->
            // Append text before the match
            append(text.substring(lastEnd, match.range.first))
            // Push annotation for clickable @mention
            pushStringAnnotation("mention", match.groupValues[1])
            withStyle(SpanStyle(color = mentionColor, fontWeight = FontWeight.SemiBold)) {
                append(match.value)
            }
            pop()
            lastEnd = match.range.last + 1
        }
        if (lastEnd < text.length) append(text.substring(lastEnd))
    }

    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = color
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("mention", offset, offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick(annotation.item)
                }
        }
    )
}
