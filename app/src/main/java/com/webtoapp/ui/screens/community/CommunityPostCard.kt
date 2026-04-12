package com.webtoapp.ui.screens.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.cloud.CommunityPostItem
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.UserTitleBadges

@Composable
internal fun PostCard(
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp).clickable { onAuthorClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (post.authorAvatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(post.authorAvatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            (post.authorDisplayName ?: post.authorUsername).take(1).uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
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
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.MoreHoriz,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
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
                        Text(
                            badgeLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }
                }
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
                        Text(
                            diffLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
                        )
                    }
                }
                if (post.postType == "question" && post.isResolved == true) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50).copy(alpha = 0.1f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(11.dp), tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(2.dp))
                            Text(Strings.communityResolvedLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        MentionableText(
            text = post.content,
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            mentionColor = MaterialTheme.colorScheme.primary,
            onMentionClick = onMentionClick
        )

        if (post.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(post.tags) { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tagColor(tag).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "#$tag",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tagColor(tag)
                        )
                    }
                }
            }
        }

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
                                        null,
                                        Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                appLink.appName ?: Strings.communityApplication,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            appLink.appDescription?.let {
                                Text(
                                    it,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                appLink.storeModuleDownloads?.let {
                                    Text("${it} ↓", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                                appLink.storeModuleRating?.let {
                                    Text("★ ${String.format("%.1f", it)}", fontSize = 10.sp, color = Color(0xFFFFB300).copy(alpha = 0.8f))
                                }
                            }
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

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
                        Text(Strings.communityRecipeDesc, fontSize = 11.sp, color = Color(0xFF6C5CE7).copy(alpha = 0.6f))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6C5CE7).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${post.recipeImportCount}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6C5CE7)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onLike) {
                Icon(
                    if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    null,
                    Modifier.size(18.dp),
                    tint = if (post.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${post.likeCount}",
                    fontSize = 12.sp,
                    color = if (post.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            TextButton(onClick = onComment) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Text("${post.commentCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            TextButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Text("${post.shareCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.RemoveRedEye, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                Spacer(Modifier.width(3.dp))
                Text("${post.viewCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
            }
        }
    }
}

internal fun tagColor(tag: String): Color = when (tag) {
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

internal fun formatTimeAgo(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        val tzRegex = Regex("([+-])(\\d{2}):(\\d{2})$")
        val tzMatch = tzRegex.find(isoString)
        val endsWithZ = isoString.endsWith("Z")

        val offsetMs = when {
            tzMatch != null -> {
                val sign = if (tzMatch.groupValues[1] == "+") 1 else -1
                val hh = tzMatch.groupValues[2].toInt()
                val mm = tzMatch.groupValues[3].toInt()
                sign * (hh * 3600_000L + mm * 60_000L)
            }
            endsWithZ -> 0L
            else -> 0L
        }

        val cleaned = isoString
            .replace(Regex("\\.[0-9]+"), "")
            .replace(Regex("[+-]\\d{2}:\\d{2}$"), "")
            .replace(Regex("Z$"), "")

        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val parsed = format.parse(cleaned) ?: return isoString
        val epochMs = parsed.time - offsetMs

        val now = System.currentTimeMillis()
        val diff = now - epochMs
        if (diff < 0) return Strings.timeJustNow
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
    } catch (_: Exception) {
        isoString
    }
}

internal fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> String.format(Strings.durationHourMinute, hours, minutes)
        minutes > 0 -> String.format(Strings.durationMinute, minutes)
        else -> Strings.durationLessThanMinute
    }
}
