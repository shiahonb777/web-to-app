package com.webtoapp.ui.screens.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Sort
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
import com.webtoapp.core.cloud.CommunityUserProfile
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.viewmodel.CommunityViewModel

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
    var activeTab by remember { mutableStateOf("users") }
    var selectedPostType by remember { mutableStateOf<String?>(null) }
    var selectedSortBy by remember { mutableStateOf("relevance") }

    fun dispatchSearch(text: String) {
        query = text
        when (activeTab) {
            "users" -> communityViewModel.searchUsers(text)
            "posts" -> communityViewModel.searchPostsFiltered(text, selectedPostType, selectedSortBy)
        }
    }

    fun onFilterChanged() {
        if (query.isNotBlank()) {
            communityViewModel.searchPostsFiltered(query, selectedPostType, selectedSortBy)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)
        ) {
            Text(AppStringsProvider.current().communitySearchAll, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("users" to AppStringsProvider.current().communityTabUsers, "posts" to AppStringsProvider.current().communityTabPostsSearch).forEach { (key, label) ->
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
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
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
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { dispatchSearch(it) },
                placeholder = {
                    Text(
                        if (activeTab == "users") AppStringsProvider.current().communitySearchHint else AppStringsProvider.current().communitySearchPostsHint,
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

            AnimatedVisibility(visible = activeTab == "posts") {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val postTypeFilters = listOf(
                            null to AppStringsProvider.current().communityAllTags,
                            "discussion" to AppStringsProvider.current().communityPostTypeDiscussion,
                            "showcase" to AppStringsProvider.current().communityPostTypeShowcase,
                            "tutorial" to AppStringsProvider.current().communityPostTypeTutorial,
                            "question" to AppStringsProvider.current().communityPostTypeQuestion,
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Sort, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
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
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
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
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (activeTab) {
                "users" -> {
                    if (searchLoading) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (query.isNotBlank() && searchResults.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.PersonOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(Modifier.height(8.dp))
                                Text(AppStringsProvider.current().communityNoUsersFound, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
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
                                Icon(Icons.Outlined.SearchOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(Modifier.height(8.dp))
                                Text(AppStringsProvider.current().communityNoPostsFound, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
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

@Composable
private fun SearchPostRow(
    post: CommunityPostItem,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        val (typeIcon, typeColor) = when (post.postType) {
            "showcase" -> Icons.Filled.Palette to Color(0xFF6C5CE7)
            "tutorial" -> Icons.AutoMirrored.Filled.MenuBook to Color(0xFF4CAF50)
            "question" -> Icons.AutoMirrored.Filled.HelpOutline to Color(0xFFFF9800)
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
            Text(
                post.title ?: post.content.take(80),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(post.authorDisplayName ?: post.authorUsername, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Text(formatTimeAgo(post.createdAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Favorite, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.width(2.dp))
                    Text("${post.likeCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.width(2.dp))
                    Text("${post.commentCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowerListSheet(
    title: String,
    users: List<CommunityUserProfile>,
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
                        Icon(Icons.Outlined.People, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text(emptyText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
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
                            subtitle = if (user.followerCount > 0) "${user.followerCount} ${AppStringsProvider.current().communityFollowersList}" else null,
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
                    model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        displayName.take(1).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
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
                    Icon(Icons.Filled.Verified, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("@$username", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            subtitle?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            }
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
    }
}
