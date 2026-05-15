package com.webtoapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.market.MarketInstallState
import com.webtoapp.core.market.MarketModuleView
import com.webtoapp.core.market.MarketState
import com.webtoapp.core.market.ModuleMarketEntry
import com.webtoapp.core.market.ModuleMarketRepository
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.design.WtaBackground
import com.webtoapp.ui.design.WtaButton
import com.webtoapp.ui.design.WtaButtonSize
import com.webtoapp.ui.design.WtaButtonVariant
import com.webtoapp.ui.design.WtaCard
import com.webtoapp.ui.design.WtaCardTone
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaSpacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Module Market — pulls modules straight from this project's GitHub repository
 * and lets the user install them with one tap. See `modules/README.md` in the
 * repo for the contributing flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleMarketScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    val repo = remember { ModuleMarketRepository.getInstance(context, extensionManager) }

    val state by repo.state.collectAsState()
    var views by remember { mutableStateOf<List<MarketModuleView>>(emptyList()) }

    LaunchedEffect(repo) {
        repo.views.collectLatest { views = it }
    }

    LaunchedEffect(repo) {
        // Kick off an initial fetch — uses cache when fresh, network otherwise.
        repo.refresh(force = false)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    var installingId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(views, searchQuery, selectedCategory) {
        views.filter { v ->
            val matchesCategory = selectedCategory == null ||
                runCatching { ModuleCategory.valueOf(v.entry.category) }.getOrNull() == selectedCategory
            val q = searchQuery.trim()
            val matchesSearch = q.isBlank() ||
                v.entry.name.contains(q, ignoreCase = true) ||
                v.entry.description.contains(q, ignoreCase = true) ||
                v.entry.tags.any { it.contains(q, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(Strings.moduleMarketTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.contributingUrl)))
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = Strings.moduleMarketContribute)
                    }
                    IconButton(onClick = {
                        scope.launch { repo.refresh(force = true) }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = Strings.refresh)
                    }
                }
            )
        }
    ) { padding ->
        WtaBackground(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Search field
                PremiumTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(Strings.moduleMarketSearchHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = Strings.clear)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(WtaRadius.Button)
                )

                // Category chips. We show a fixed set of the most relevant
                // categories so the row stays shallow on small screens.
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        PremiumFilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(Strings.moduleMarketAll) }
                        )
                    }
                    items(MarketCategoryHighlights) { category ->
                        PremiumFilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category.getDisplayName()) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                when (val s = state) {
                    is MarketState.Idle, is MarketState.Loading -> {
                        if (views.isEmpty()) {
                            LoadingPlaceholder()
                        } else {
                            ModuleListContent(
                                items = filtered,
                                installingId = installingId,
                                onInstall = { entry ->
                                    installingId = entry.id
                                    scope.launch {
                                        installModule(entry, repo, snackbarHostState, context.applicationContext)
                                        installingId = null
                                    }
                                },
                                onOpenSource = { entry ->
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(repo.githubUrl(entry)))
                                    )
                                }
                            )
                        }
                    }
                    is MarketState.Loaded -> {
                        if (filtered.isEmpty()) {
                            EmptyState(searchQuery)
                        } else {
                            ModuleListContent(
                                items = filtered,
                                installingId = installingId,
                                onInstall = { entry ->
                                    installingId = entry.id
                                    scope.launch {
                                        installModule(entry, repo, snackbarHostState, context.applicationContext)
                                        installingId = null
                                    }
                                },
                                onOpenSource = { entry ->
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(repo.githubUrl(entry)))
                                    )
                                }
                            )
                        }
                    }
                    is MarketState.Error -> {
                        ErrorState(message = s.message, onRetry = {
                            scope.launch { repo.refresh(force = true) }
                        })
                    }
                }
            }
        }
    }
}

private suspend fun installModule(
    entry: ModuleMarketEntry,
    repo: ModuleMarketRepository,
    snackbar: SnackbarHostState,
    appContext: android.content.Context
) {
    val result = repo.install(entry)
    result.onSuccess {
        Toast.makeText(appContext, Strings.moduleMarketInstalled.replace("%s", entry.name), Toast.LENGTH_SHORT).show()
    }.onFailure { e ->
        snackbar.showSnackbar(Strings.moduleMarketInstallFailed.replace("%s", e.message ?: "unknown"))
    }
}

@Composable
private fun ModuleListContent(
    items: List<MarketModuleView>,
    installingId: String?,
    onInstall: (ModuleMarketEntry) -> Unit,
    onOpenSource: (ModuleMarketEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.entry.id }) { view ->
            MarketModuleCard(
                view = view,
                isInstalling = installingId == view.entry.id,
                onInstall = { onInstall(view.entry) },
                onOpenSource = { onOpenSource(view.entry) }
            )
        }
    }
}

@Composable
private fun MarketModuleCard(
    view: MarketModuleView,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onOpenSource: () -> Unit
) {
    WtaCard(
        modifier = Modifier.fillMaxWidth(),
        tone = WtaCardTone.Surface,
        contentPadding = PaddingValues(WtaSpacing.Large)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon — use first letter of name as a stand-in. Custom material
            // icon names are stored in `entry.icon`; full mapping is left for
            // a future iteration.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = view.entry.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        view.entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "v${view.entry.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (view.entry.description.isNotBlank()) {
                    Text(
                        view.entry.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                view.entry.author?.let { a ->
                    Text(
                        Strings.moduleMarketAuthor.replace("%s", a.name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenSource, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.OpenInNew, contentDescription = Strings.moduleMarketViewSource)
            }
            Spacer(Modifier.weight(1f))
            InstallButton(
                state = view.state,
                isInstalling = isInstalling,
                onClick = onInstall
            )
        }
    }
}

@Composable
private fun InstallButton(
    state: MarketInstallState,
    isInstalling: Boolean,
    onClick: () -> Unit
) {
    if (isInstalling) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(Strings.moduleMarketInstalling, style = MaterialTheme.typography.labelMedium)
        }
        return
    }
    when (state) {
        MarketInstallState.NotInstalled -> WtaButton(
            onClick = onClick,
            text = Strings.moduleMarketInstall,
            variant = WtaButtonVariant.Primary,
            size = WtaButtonSize.Small,
            leadingIcon = Icons.Default.CloudDownload
        )
        MarketInstallState.UpdateAvailable -> WtaButton(
            onClick = onClick,
            text = Strings.moduleMarketUpdate,
            variant = WtaButtonVariant.Tonal,
            size = WtaButtonSize.Small,
            leadingIcon = Icons.Default.SystemUpdate
        )
        MarketInstallState.UpToDate -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                Strings.moduleMarketInstalled.replace("%s", "").trim(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                Strings.moduleMarketLoading,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(searchQuery: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            if (searchQuery.isBlank()) Strings.moduleMarketEmpty
            else Strings.moduleMarketNoResults,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            WtaButton(
                onClick = onRetry,
                text = Strings.retry,
                variant = WtaButtonVariant.Tonal,
                size = WtaButtonSize.Small
            )
        }
    }
}

/** Categories shown as filter chips in the market header. */
private val MarketCategoryHighlights = listOf(
    ModuleCategory.CONTENT_FILTER,
    ModuleCategory.STYLE_MODIFIER,
    ModuleCategory.FUNCTION_ENHANCE,
    ModuleCategory.MEDIA,
    ModuleCategory.SECURITY,
    ModuleCategory.TRANSLATE,
    ModuleCategory.DEVELOPER,
    ModuleCategory.OTHER
)
