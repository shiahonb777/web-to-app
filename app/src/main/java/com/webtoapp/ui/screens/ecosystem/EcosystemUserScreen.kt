package com.webtoapp.ui.screens.ecosystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.webtoapp.core.cloud.EcosystemItem
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaEmptyState
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.WtaSettingRow
import com.webtoapp.ui.viewmodel.EcosystemViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EcosystemUserScreen(
    userId: Int,
    viewModel: EcosystemViewModel = koinViewModel(),
    onBack: () -> Unit,
    onNavigateToItem: (String, Int) -> Unit
) {
    val state by viewModel.profileState.collectAsState()
    var selected by remember { mutableStateOf("post") }

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    WtaScreen(title = Strings.ecosystemProfileTitle, onBack = onBack) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.content == null -> WtaEmptyState(
                title = Strings.ecosystemUserMissingTitle,
                message = state.error,
                icon = Icons.Outlined.Person,
                modifier = Modifier.padding(WtaSpacing.ScreenHorizontal)
            )
            else -> {
                val content = state.content!!
                val list = when (selected) {
                    "app" -> content.apps
                    "module" -> content.modules
                    else -> content.posts
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = WtaSpacing.ScreenHorizontal),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)
                ) {
                    item {
                        WtaSettingCard {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    content.profile.displayName ?: content.profile.username,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!content.profile.bio.isNullOrBlank()) {
                                    Text(content.profile.bio, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ProfileStat(Strings.ecosystemTabPosts, content.posts.size)
                                    ProfileStat(Strings.ecosystemTabApps, content.apps.size)
                                    ProfileStat(Strings.ecosystemTabModules, content.modules.size)
                                    ProfileStat(Strings.ecosystemFollowers, content.profile.followerCount)
                                }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileTab("post", Strings.ecosystemTabPosts, selected) { selected = it }
                            ProfileTab("app", Strings.ecosystemTabApps, selected) { selected = it }
                            ProfileTab("module", Strings.ecosystemTabModules, selected) { selected = it }
                        }
                    }
                    if (list.isEmpty()) {
                        item {
                            WtaEmptyState(title = Strings.ecosystemNoProfileContent.format(profileTabLabel(selected)))
                        }
                    } else {
                        items(list, key = { "${it.type}-${it.id}" }) { item ->
                            ProfileContentRow(item = item, onClick = { onNavigateToItem(item.type, item.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(key: String, label: String, selected: String, onSelected: (String) -> Unit) {
    AssistChip(
        onClick = { onSelected(key) },
        label = { Text(label) },
        leadingIcon = if (key == selected) {
            { androidx.compose.material3.Icon(iconForType(key), null) }
        } else null
    )
}

@Composable
private fun ProfileStat(label: String, value: Int) {
    WtaSettingCard(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun profileTabLabel(key: String): String = when (key) {
    "app" -> Strings.ecosystemTabApps
    "module" -> Strings.ecosystemTabModules
    else -> Strings.ecosystemTabPosts
}

@Composable
private fun ProfileContentRow(item: EcosystemItem, onClick: () -> Unit) {
    WtaSettingCard(onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (item.summary.isNotBlank()) {
                Text(item.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ecosystemPublishTimeLabel(item.createdAt)?.let { publishTime ->
                Text(
                    publishTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${Strings.ecosystemLike} ${item.stats.likes}  ${Strings.ecosystemComment} ${item.stats.comments}" +
                    if (item.type != "post") "  ${Strings.ecosystemDownload} ${item.stats.downloads}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
