package com.webtoapp.ui.screens.community

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.ModuleComment
import com.webtoapp.ui.viewmodel.CommunityViewModel
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * module- Jobs- style frosted glass + spring physics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    moduleId: Int,
    communityViewModel: CommunityViewModel,
    onBack: () -> Unit,
    onNavigateToUser: (Int) -> Unit,
    onInstallModule: (String) -> Unit,
) {
    val module by communityViewModel.moduleDetail.collectAsStateWithLifecycle()
    val loading by communityViewModel.moduleDetailLoading.collectAsStateWithLifecycle()
    val comments by communityViewModel.comments.collectAsStateWithLifecycle()
    val commentsLoading by communityViewModel.commentsLoading.collectAsStateWithLifecycle()
    val message by communityViewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var commentText by remember { mutableStateOf("") }
    var showMoreSheet by remember { mutableStateOf(false) }

    LaunchedEffect(moduleId) {
        communityViewModel.loadModuleDetail(moduleId)
        communityViewModel.loadComments(moduleId)
    }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); communityViewModel.clearMessage() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().communityPost, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp))
                    }
                }
            )
        },
        // bottominput
        bottomBar = {
            FrostedBottomBar {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                             Text(AppStringsProvider.current().communityPostYourReply, fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        },
                        modifier = Modifier.weight(weight = 1f, fill = true).heightIn(max = 96.dp),
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
                    val sendEnabled = commentText.isNotBlank()
                    val sendAlpha by animateFloatAsState(
                        if (sendEnabled) 1f else 0.35f, tween(200), label = "sendA"
                    )
                    FilledIconButton(
                        onClick = {
                            if (sendEnabled) {
                                communityViewModel.addComment(moduleId, commentText.trim())
                                commentText = ""
                            }
                        },
                        enabled = sendEnabled,
                        modifier = Modifier.size(40.dp).graphicsLayer { alpha = sendAlpha },
                        shape = CircleShape
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        if (loading) {
            DetailShimmer(Modifier)
        } else {
            module?.let { mod ->
                LazyColumn(Modifier) {
                    // Note
                    item {
                        StaggeredItem(index = 0) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                // Note
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onNavigateToUser(mod.authorId) }
                                ) {
                                    Avatar(name = mod.authorName, size = 42)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(weight = 1f, fill = true)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(mod.authorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            if (mod.isFeatured) {
                                                Spacer(Modifier.width(3.dp))
                                                Icon(Icons.Filled.Verified, null, Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Text(stringResource(com.webtoapp.R.string.community_developer_role), fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    }
                                    IconButton(onClick = { showMoreSheet = true }) {
                                        Icon(Icons.Outlined.MoreHoriz, null, Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                                    }
                                }

                                Spacer(Modifier.height(14.dp))
                                Text(mod.name, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 28.sp)

                                if (!mod.description.isNullOrBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(mod.description, fontSize = 15.sp, lineHeight = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f))
                                }

                                if (mod.tags.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        buildAnnotatedString {
                                            mod.tags.take(4).forEachIndexed { i, tag ->
                                                if (i > 0) append("  ")
                                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                                                    append("#$tag")
                                                }
                                            }
                                        }, fontSize = 14.sp
                                    )
                                }

                                Spacer(Modifier.height(14.dp))
                                Text(
                                    buildAnnotatedString {
                                        mod.versionName?.let { append("v$it  ·  ") }
                                        append("${String.format(AppStringsProvider.current().communityDownloads, mod.downloads)}  ·  ${String.format(AppStringsProvider.current().communityRatings, mod.ratingCount)}")
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                        GlassDivider()
                    }

                    // Note
                    item {
                        StaggeredItem(index = 1) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                PhysicsActionButton(
                                    icon = Icons.Outlined.ChatBubbleOutline,
                                    activeIcon = Icons.Outlined.ChatBubbleOutline,
                                    count = comments.size, isActive = false,
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    onClick = {}
                                )
                                PhysicsActionButton(
                                    icon = Icons.Outlined.ThumbUp,
                                    activeIcon = Icons.Filled.ThumbUp,
                                    count = mod.rating.toInt(),
                                    isActive = mod.userVote == "up",
                                    activeColor = Color(0xFF4CAF50),
                                    onClick = { communityViewModel.voteModule(moduleId, "up") }
                                )
                                PhysicsActionButton(
                                    icon = Icons.Outlined.ThumbDown,
                                    activeIcon = Icons.Filled.ThumbDown,
                                    count = null, isActive = mod.userVote == "down",
                                    activeColor = Color(0xFFEF5350),
                                    onClick = { communityViewModel.voteModule(moduleId, "down") }
                                )
                                PhysicsActionButton(
                                    icon = Icons.Outlined.BookmarkBorder,
                                    activeIcon = Icons.Filled.Bookmark,
                                    count = null, isActive = mod.isFavorited,
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    onClick = { communityViewModel.toggleFavorite(moduleId) }
                                )
                                PhysicsActionButton(
                                    icon = Icons.Outlined.Download,
                                    activeIcon = Icons.Filled.Download,
                                    count = null, isActive = false,
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    onClick = { mod.shareCode?.let(onInstallModule) }
                                )
                            }
                        }
                        GlassDivider()
                    }

                    // Note
                    if (comments.isEmpty() && !commentsLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(AppStringsProvider.current().communityNoRepliesYet, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(2.dp))
                                    Text(AppStringsProvider.current().communityBeFirstReply, fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                                }
                            }
                        }
                    }

                    itemsIndexed(comments, key = { _, c -> c.id }) { index, comment ->
                        StaggeredItem(index = index + 2) {
                            CommentRow(comment, onUserClick = { onNavigateToUser(comment.userId) })
                        }
                        GlassDivider(Modifier.padding(start = 62.dp))
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    if (showMoreSheet) {
        ReportSheet(
            onDismiss = { showMoreSheet = false },
            onReport = { reason ->
                communityViewModel.reportModule(moduleId, reason, null)
                showMoreSheet = false
            }
        )
    }
        }
}

// ═══════════════════════════════════════════
// spring button
// ═══════════════════════════════════════════

@Composable
private fun PhysicsActionButton(
    icon: ImageVector, activeIcon: ImageVector,
    count: Int?, isActive: Boolean,
    activeColor: Color, onClick: () -> Unit
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val currentColor by animateColorAsState(
        if (isActive) activeColor else inactiveColor,
        tween(280), label = "btnClr"
    )

    // Note
    var burstKey by remember { mutableIntStateOf(0) }
    var showBurst by remember { mutableStateOf(false) }

    // Note
    var bouncing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (bouncing) 1.35f else 1f,
        CommunityPhysics.LikeBounce,
        label = "btnScale",
        finishedListener = { bouncing = false }
    )

    Box(contentAlignment = Alignment.Center) {
        // Note
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
                Modifier.size(20.dp).scale(scale),
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

    // reset
    LaunchedEffect(burstKey) {
        if (showBurst) {
            kotlinx.coroutines.delay(500)
            showBurst = false
        }
    }
}

// ═══════════════════════════════════════════
// Note
// ═══════════════════════════════════════════

@Composable
private fun CommentRow(comment: ModuleComment, onUserClick: () -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Avatar(
            name = comment.userName, avatarUrl = comment.userAvatar, size = 36,
            modifier = Modifier.clickable(onClick = onUserClick)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(weight = 1f, fill = true)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onUserClick))
                comment.createdAt?.let {
                    Text("  ·  ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(comment.content, fontSize = 15.sp, lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))

            if (comment.replies.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                comment.replies.take(3).forEach { reply ->
                    Row(Modifier.padding(vertical = 1.dp)) {
                        Text(reply.userName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Text(" ${reply.content}", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (comment.replies.size > 3) {
                    Text(AppStringsProvider.current().communityShowMoreReplies, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Shimmer
// ═══════════════════════════════════════════

@Composable
private fun DetailShimmer(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBlock(42.dp, 42.dp, CircleShape)
            Spacer(Modifier.width(10.dp))
            Column {
                ShimmerBlock(120.dp, 14.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBlock(80.dp, 12.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        ShimmerBlock(220.dp, 20.dp)
        Spacer(Modifier.height(12.dp))
        ShimmerBlock(300.dp, 14.dp)
        Spacer(Modifier.height(6.dp))
        ShimmerBlock(260.dp, 14.dp)
        Spacer(Modifier.height(6.dp))
        ShimmerBlock(180.dp, 14.dp)
        Spacer(Modifier.height(20.dp))
        GlassDivider()
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(5) { ShimmerBlock(28.dp, 20.dp) }
        }
        Spacer(Modifier.height(12.dp))
        GlassDivider()
        repeat(3) {
            Spacer(Modifier.height(14.dp))
            Row {
                ShimmerBlock(36.dp, 36.dp, CircleShape)
                Spacer(Modifier.width(10.dp))
                Column {
                    ShimmerBlock(100.dp, 13.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBlock(240.dp, 13.dp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Sheet
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(onDismiss: () -> Unit, onReport: (String) -> Unit) {
    var selected by remember { mutableStateOf("") }
    val reasons = listOf(
        "spam" to AppStringsProvider.current().communityReportSpam, "inappropriate" to AppStringsProvider.current().communityReportInappropriate,
        "malicious" to AppStringsProvider.current().communityReportMalicious, "copyright" to AppStringsProvider.current().communityReportCopyright, "other" to AppStringsProvider.current().communityReportOther
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(AppStringsProvider.current().communityReportTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(AppStringsProvider.current().communityReportWhy, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
            Spacer(Modifier.height(16.dp))
            reasons.forEach { (key, label) ->
                Row(
                    Modifier.fillMaxWidth().clickable { selected = key }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, fontSize = 15.sp, modifier = Modifier.weight(weight = 1f, fill = true))
                    RadioButton(selected = selected == key, onClick = { selected = key })
                }
                GlassDivider()
            }
            Spacer(Modifier.height(12.dp))
            PremiumButton(
                onClick = { onReport(selected) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = selected.isNotBlank()
            ) { Text(AppStringsProvider.current().communityReportSubmit, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(28.dp))
        }
    }
}
