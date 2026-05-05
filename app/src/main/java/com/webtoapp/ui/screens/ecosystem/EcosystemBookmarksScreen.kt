package com.webtoapp.ui.screens.ecosystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.design.WtaEmptyState
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.viewmodel.EcosystemViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EcosystemBookmarksScreen(
    viewModel: EcosystemViewModel = koinViewModel(),
    onBack: () -> Unit,
    onNavigateToItem: (String, Int) -> Unit,
    onNavigateToUser: (Int) -> Unit
) {
    val state by viewModel.bookmarksState.collectAsState()
    var previewImages by remember { mutableStateOf<List<EcosystemPreviewImage>>(emptyList()) }
    var previewIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadBookmarks()
    }

    WtaScreen(
        title = Strings.ecosystemBookmarks,
        subtitle = Strings.ecosystemSubtitle,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WtaSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)
        ) {
            EcosystemTypeTabs(
                selected = state.selectedType,
                onSelected = viewModel::setBookmarksType
            )

            state.error?.let {
                WtaStatusBanner(
                    title = Strings.ecosystemLoadFailed,
                    message = it,
                    tone = WtaStatusTone.Error,
                    actionLabel = Strings.ecosystemRetry,
                    onAction = { viewModel.loadBookmarks() }
                )
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.items.isEmpty() -> WtaEmptyState(
                    title = Strings.ecosystemNoBookmarksTitle,
                    message = Strings.ecosystemNoBookmarksMessage,
                    icon = Icons.Outlined.BookmarkBorder
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
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
