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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.CommunityModuleDetail
import com.webtoapp.ui.viewmodel.CommunityViewModel
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color
import com.webtoapp.core.i18n.Strings

/**
 * Favorites screen with state-driven UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    communityViewModel: CommunityViewModel,
    onBack: () -> Unit,
    onModuleClick: (Int) -> Unit
) {
    val favorites by communityViewModel.favorites.collectAsStateWithLifecycle()
    val loading by communityViewModel.favoritesLoading.collectAsStateWithLifecycle()
    val message by communityViewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { communityViewModel.loadFavorites() }
    LaunchedEffect(message) { message?.let { snackbarHostState.showSnackbar(it); communityViewModel.clearMessage() } }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Strings.communityBookmarks, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        if (favorites.isNotEmpty()) {
                            Text("@me", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp)) }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        if (loading) {
            BookmarkShimmer(Modifier)
        } else if (favorites.isEmpty()) {
            // Jobs- style state
            Box(Modifier, contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    Text(Strings.communitySaveForLater, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Strings.communitySaveForLaterHint,
                        fontSize = 14.sp, lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                itemsIndexed(favorites, key = { _, m -> m.id }) { index, module ->
                    StaggeredItem(index = index) {
                        BookmarkRow(module, onClick = { onModuleClick(module.id) })
                    }
                    GlassDivider()
                }
            }
        }
    }
        }
}

@Composable
private fun BookmarkRow(module: CommunityModuleDetail, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(name = module.authorName, size = 40)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(weight = 1f, fill = true)) {
            // Note
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(module.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (module.isFeatured) {
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
                module.category?.let {
                    Text("  ·  $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                }
            }
            // module
            Text(module.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Note
            if (!module.description.isNullOrBlank()) {
                Text(module.description, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
            // bottom
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ThumbUp, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    Spacer(Modifier.width(3.dp))
                    Text("${module.rating.toInt()}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Outlined.Download, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    Spacer(Modifier.width(3.dp))
                    Text("${module.downloads}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                }
                Icon(Icons.Filled.Bookmark, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ═══ Shimmer ═══

@Composable
private fun BookmarkShimmer(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        repeat(5) {
            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
                ShimmerBlock(40.dp, 40.dp, CircleShape)
                Spacer(Modifier.width(10.dp))
                Column {
                    ShimmerBlock(120.dp, 13.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBlock(180.dp, 14.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBlock(260.dp, 13.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBlock(160.dp, 13.dp)
                }
            }
            GlassDivider()
        }
    }
}
