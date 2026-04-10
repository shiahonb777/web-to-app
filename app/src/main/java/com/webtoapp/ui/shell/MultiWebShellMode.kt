package com.webtoapp.ui.shell

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.MultiWebShellConfig
import com.webtoapp.core.shell.MultiWebSiteShellConfig
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.core.webview.WebViewCallbacks
import com.webtoapp.ui.components.EdgeSwipeRefreshLayout
import com.webtoapp.data.model.WebViewConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Multi-Web Shell Mode — 多站点聚合应用运行时
 * 支持四种显示模式：
 * - TABS：底部标签页，每个标签对应一个站点
 * - CARDS：卡片首页，点击卡片打开全屏 WebView
 * - FEED：聚合信息流，从所有站点提取文章标题和链接
 * - DRAWER：侧边抽屉，左侧抽屉显示站点列表，右侧显示选中站点的 WebView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiWebShellMode(
    config: ShellConfig,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val multiWebConfig = config.multiWebConfig
    val sites = multiWebConfig.sites.filter { it.enabled && it.url.isNotBlank() }

    if (sites.isEmpty()) {
        // No sites configured
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Language, null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No sites configured",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    when (multiWebConfig.displayMode.uppercase()) {
        "TABS" -> TabsMode(config, multiWebConfig, sites, webViewConfig, webViewCallbacks, webViewManager, onWebViewCreated, swipeRefreshEnabled, isRefreshing, onRefresh)
        "CARDS" -> CardsMode(config, multiWebConfig, sites, webViewConfig, webViewCallbacks, webViewManager, onWebViewCreated, swipeRefreshEnabled, isRefreshing, onRefresh)
        "FEED" -> FeedMode(config, multiWebConfig, sites, webViewConfig, webViewCallbacks, webViewManager, onWebViewCreated, swipeRefreshEnabled, isRefreshing, onRefresh)
        "DRAWER" -> DrawerMode(config, multiWebConfig, sites, webViewConfig, webViewCallbacks, webViewManager, onWebViewCreated, swipeRefreshEnabled, isRefreshing, onRefresh)
        else -> TabsMode(config, multiWebConfig, sites, webViewConfig, webViewCallbacks, webViewManager, onWebViewCreated, swipeRefreshEnabled, isRefreshing, onRefresh)
    }
}

// ============================================================
// TABS MODE — Bottom tab bar with WebView per tab
// ============================================================

@Composable
private fun TabsMode(
    config: ShellConfig,
    multiWebConfig: MultiWebShellConfig,
    sites: List<MultiWebSiteShellConfig>,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val webViews = remember { mutableStateMapOf<Int, WebView>() }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (sites.size > 1) {
                NavigationBar(
                    tonalElevation = 2.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    sites.forEachIndexed { index, site ->
                        NavigationBarItem(
                            icon = {
                                if (site.iconEmoji.isNotBlank()) {
                                    Text(site.iconEmoji, fontSize = 20.sp)
                                } else {
                                    Icon(Icons.Outlined.Language, contentDescription = null)
                                }
                            },
                            label = {
                                Text(
                                    site.name.ifBlank { extractDomain(site.url) },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                            },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Show all WebViews but only make the selected one visible
            sites.forEachIndexed { index, site ->
                val isVisible = index == selectedTab
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (!isVisible) Modifier.size(0.dp) else Modifier)
                ) {
                    if (isVisible || webViews.containsKey(index)) {
                        var swipeChildWebView: WebView? = null
                        AndroidView(
                            factory = { ctx ->
                                EdgeSwipeRefreshLayout(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setColorSchemeColors(
                                        android.graphics.Color.parseColor("#6750A4"),
                                        android.graphics.Color.parseColor("#7F67BE")
                                    )
                                    isEnabled = swipeRefreshEnabled
                                    setOnRefreshListener {
                                        swipeChildWebView?.reload()
                                    }
                                    setOnChildScrollUpCallback { _, child ->
                                        val wv = child as? WebView ?: return@setOnChildScrollUpCallback false
                                        wv.scrollY > 0
                                    }
                                    val createdWebView = webViews.getOrPut(index) {
                                        WebView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            webViewManager.configureWebView(
                                                this, webViewConfig, webViewCallbacks,
                                                config.extensionModuleIds, config.embeddedExtensionModules,
                                                config.extensionFabIcon, allowGlobalModuleFallback = false
                                            )
                                            var lastTouchX = 0f; var lastTouchY = 0f
                                            setOnTouchListener { view, event ->
                                                when (event.action) {
                                                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                                        lastTouchX = event.x; lastTouchY = event.y
                                                    }
                                                    MotionEvent.ACTION_UP -> view.performClick()
                                                }
                                                false
                                            }
                                            setOnLongClickListener {
                                                webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                                            }
                                            if (index == 0) onWebViewCreated(this)
                                            loadUrl(site.url)
                                        }
                                    }
                                    swipeChildWebView = createdWebView
                                    addView(createdWebView)
                                }
                            },
                            update = { swipeLayout ->
                                swipeLayout.isEnabled = swipeRefreshEnabled
                                if (swipeLayout.isRefreshing != isRefreshing) {
                                    swipeLayout.isRefreshing = isRefreshing
                                }
                                if (!isRefreshing && swipeLayout.isRefreshing) {
                                    swipeLayout.isRefreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// CARDS MODE — Home grid, tap to open full-screen WebView
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardsMode(
    config: ShellConfig,
    multiWebConfig: MultiWebShellConfig,
    sites: List<MultiWebSiteShellConfig>,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    var openSite by remember { mutableStateOf<MultiWebSiteShellConfig?>(null) }

    if (openSite != null) {
        // Full-screen WebView for selected site
        val site = openSite!!
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(site.name.ifBlank { extractDomain(site.url) }) },
                    navigationIcon = {
                        IconButton(onClick = { openSite = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            var swipeChildWebView: WebView? = null
            AndroidView(
                factory = { ctx ->
                    EdgeSwipeRefreshLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setColorSchemeColors(
                            android.graphics.Color.parseColor("#6750A4"),
                            android.graphics.Color.parseColor("#7F67BE")
                        )
                        isEnabled = swipeRefreshEnabled
                        setOnRefreshListener {
                            swipeChildWebView?.reload()
                        }
                        setOnChildScrollUpCallback { _, child ->
                            val wv = child as? WebView ?: return@setOnChildScrollUpCallback false
                            wv.scrollY > 0
                        }
                        val createdWebView = WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewManager.configureWebView(
                                this, webViewConfig, webViewCallbacks,
                                config.extensionModuleIds, config.embeddedExtensionModules,
                                config.extensionFabIcon, allowGlobalModuleFallback = false
                            )
                            var lastTouchX = 0f; var lastTouchY = 0f
                            setOnTouchListener { view, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                        lastTouchX = event.x; lastTouchY = event.y
                                    }
                                    MotionEvent.ACTION_UP -> view.performClick()
                                }
                                false
                            }
                            setOnLongClickListener {
                                webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                            }
                            onWebViewCreated(this)
                            loadUrl(site.url)
                        }
                        swipeChildWebView = createdWebView
                        addView(createdWebView)
                    }
                },
                update = { swipeLayout ->
                    swipeLayout.isEnabled = swipeRefreshEnabled
                    if (swipeLayout.isRefreshing != isRefreshing) {
                        swipeLayout.isRefreshing = isRefreshing
                    }
                    if (!isRefreshing && swipeLayout.isRefreshing) {
                        swipeLayout.isRefreshing = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    } else {
        // Card grid home
        CardsHomeGrid(
            sites = sites,
            showIcons = multiWebConfig.showSiteIcons,
            onSiteClicked = { openSite = it }
        )
    }
}

@Composable
private fun CardsHomeGrid(
    sites: List<MultiWebSiteShellConfig>,
    showIcons: Boolean,
    onSiteClicked: (MultiWebSiteShellConfig) -> Unit
) {
    // Group by category
    val grouped = remember(sites) {
        val categorized = sites.groupBy { it.category.ifBlank { "" } }
        categorized.entries.sortedBy { if (it.key.isBlank()) "zzz" else it.key }
    }

    val cardColors = remember {
        listOf(
            Color(0xFF6366F1) to Color(0xFF818CF8), // Indigo
            Color(0xFFEC4899) to Color(0xFFF472B6), // Pink
            Color(0xFF14B8A6) to Color(0xFF2DD4BF), // Teal
            Color(0xFFF59E0B) to Color(0xFFFBBF24), // Amber
            Color(0xFF8B5CF6) to Color(0xFFA78BFA), // Violet
            Color(0xFF06B6D4) to Color(0xFF22D3EE), // Cyan
            Color(0xFFEF4444) to Color(0xFFF87171), // Red
            Color(0xFF10B981) to Color(0xFF34D399), // Emerald
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "🌐",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "My Sites",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${sites.size} sites",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        grouped.forEach { (category, categorySites) ->
            if (category.isNotBlank()) {
                item {
                    Text(
                        category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
            }

            // 2-column grid using Row pairs
            val chunked = categorySites.chunked(2)
            items(chunked) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEachIndexed { i, site ->
                        val colorIndex = (sites.indexOf(site)) % cardColors.size
                        val (colorStart, colorEnd) = cardColors[colorIndex]

                        SiteCard(
                            site = site,
                            colorStart = colorStart,
                            colorEnd = colorEnd,
                            showIcon = showIcons,
                            onClick = { onSiteClicked(site) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if odd number
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SiteCard(
    site: MultiWebSiteShellConfig,
    colorStart: Color,
    colorEnd: Color,
    showIcon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(colorStart, colorEnd)
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon / emoji
                if (showIcon) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (site.iconEmoji.isNotBlank()) {
                            Text(site.iconEmoji, fontSize = 22.sp)
                        } else {
                            Icon(
                                Icons.Outlined.Language, null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        site.name.ifBlank { extractDomain(site.url) },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        extractDomain(site.url),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================
// FEED MODE — Aggregated article feed from all sites
// ============================================================

data class FeedItem(
    val title: String,
    val url: String,
    val siteName: String,
    val siteEmoji: String,
    val siteUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedMode(
    config: ShellConfig,
    multiWebConfig: MultiWebShellConfig,
    sites: List<MultiWebSiteShellConfig>,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var feedItems by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var openUrl by remember { mutableStateOf<String?>(null) }
    var openTitle by remember { mutableStateOf("") }

    // Fetch feed items
    LaunchedEffect(sites) {
        isLoading = true
        feedItems = withContext(Dispatchers.IO) {
            fetchFeedItems(sites)
        }
        isLoading = false
    }

    if (openUrl != null) {
        // Full-screen WebView for article
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(openTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { openUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { padding ->
            var swipeChildWebView: WebView? = null
            AndroidView(
                factory = { ctx ->
                    EdgeSwipeRefreshLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setColorSchemeColors(
                            android.graphics.Color.parseColor("#6750A4"),
                            android.graphics.Color.parseColor("#7F67BE")
                        )
                        isEnabled = swipeRefreshEnabled
                        setOnRefreshListener {
                            swipeChildWebView?.reload()
                        }
                        setOnChildScrollUpCallback { _, child ->
                            val wv = child as? WebView ?: return@setOnChildScrollUpCallback false
                            wv.scrollY > 0
                        }
                        val createdWebView = WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewManager.configureWebView(
                                this, webViewConfig, webViewCallbacks,
                                config.extensionModuleIds, config.embeddedExtensionModules,
                                config.extensionFabIcon, allowGlobalModuleFallback = false
                            )
                            var lastTouchX = 0f; var lastTouchY = 0f
                            setOnTouchListener { view, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                        lastTouchX = event.x; lastTouchY = event.y
                                    }
                                    MotionEvent.ACTION_UP -> view.performClick()
                                }
                                false
                            }
                            setOnLongClickListener {
                                webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                            }
                            onWebViewCreated(this)
                            loadUrl(openUrl!!)
                        }
                        swipeChildWebView = createdWebView
                        addView(createdWebView)
                    }
                },
                update = { swipeLayout ->
                    swipeLayout.isEnabled = swipeRefreshEnabled
                    if (swipeLayout.isRefreshing != isRefreshing) {
                        swipeLayout.isRefreshing = isRefreshing
                    }
                    if (!isRefreshing && swipeLayout.isRefreshing) {
                        swipeLayout.isRefreshing = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    } else {
        // Feed list
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text("Feed", fontWeight = FontWeight.Bold)
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    feedItems = withContext(Dispatchers.IO) {
                                        fetchFeedItems(sites)
                                    }
                                    isLoading = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            if (isLoading && feedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching articles...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (feedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.RssFeed, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No articles found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Configure CSS selectors to extract articles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "${feedItems.size} articles from ${sites.size} sites",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(feedItems) { item ->
                        FeedItemCard(
                            item = item,
                            onClick = {
                                openUrl = item.url
                                openTitle = item.title
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FeedItemCard(
    item: FeedItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Site badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.siteEmoji.isNotBlank()) {
                    Text(item.siteEmoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    item.siteName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Domain
            Text(
                extractDomain(item.url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ============================================================
// DRAWER MODE — Left drawer with site list, main content = selected site WebView
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerMode(
    config: ShellConfig,
    multiWebConfig: MultiWebShellConfig,
    sites: List<MultiWebSiteShellConfig>,
    webViewConfig: WebViewConfig,
    webViewCallbacks: WebViewCallbacks,
    webViewManager: com.webtoapp.core.webview.WebViewManager,
    onWebViewCreated: (WebView) -> Unit,
    swipeRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    var selectedSite by remember { mutableStateOf(sites.firstOrNull()) }
    var drawerVisible by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(drawerVisible) {
        if (drawerVisible) {
            scope.launch { drawerState.open() }
        } else {
            scope.launch { drawerState.close() }
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        if (!drawerVisible && drawerState.currentValue == DrawerValue.Open) {
            drawerVisible = true
        } else if (drawerVisible && drawerState.currentValue == DrawerValue.Closed) {
            drawerVisible = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    Text(
                        "My Sites",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${sites.size} sites",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Site list in drawer
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sites) { site ->
                        val isSelected = selectedSite?.id == site.id
                        DrawerSiteItem(
                            site = site,
                            isSelected = isSelected,
                            onClick = {
                                selectedSite = site
                                drawerVisible = false
                            }
                        )
                    }
                }
            }
        },
        gesturesEnabled = true
    ) {
        // Main content: WebView of selected site with hamburger button
        val currentSite = selectedSite ?: sites.firstOrNull()

        // Keep all WebViews alive in a map, show only the selected one
        val webViews = remember { mutableStateMapOf<String, WebView>() }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            currentSite?.name?.ifBlank { currentSite?.url?.let { extractDomain(it) } ?: "" } ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            drawerVisible = !drawerVisible
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                sites.forEach { site ->
                    val isVisible = currentSite?.id == site.id
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (!isVisible) Modifier.size(0.dp) else Modifier)
                    ) {
                        if (isVisible || webViews.containsKey(site.id)) {
                            var swipeChildWebView: WebView? = null
                            AndroidView(
                                factory = { ctx ->
                                    EdgeSwipeRefreshLayout(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        setColorSchemeColors(
                                            android.graphics.Color.parseColor("#6750A4"),
                                            android.graphics.Color.parseColor("#7F67BE")
                                        )
                                        isEnabled = swipeRefreshEnabled
                                        setOnRefreshListener {
                                            swipeChildWebView?.reload()
                                        }
                                        setOnChildScrollUpCallback { _, child ->
                                            val wv = child as? WebView ?: return@setOnChildScrollUpCallback false
                                            wv.scrollY > 0
                                        }
                                        val createdWebView = webViews.getOrPut(site.id) {
                                            WebView(ctx).apply {
                                                layoutParams = ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                webViewManager.configureWebView(
                                                    this, webViewConfig, webViewCallbacks,
                                                    config.extensionModuleIds, config.embeddedExtensionModules,
                                                    config.extensionFabIcon, allowGlobalModuleFallback = false
                                                )
                                                var lastTouchX = 0f; var lastTouchY = 0f
                                                setOnTouchListener { view, event ->
                                                    when (event.action) {
                                                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                                            lastTouchX = event.x; lastTouchY = event.y
                                                        }
                                                        MotionEvent.ACTION_UP -> view.performClick()
                                                    }
                                                    false
                                                }
                                                setOnLongClickListener {
                                                    webViewCallbacks.onLongPress(this, lastTouchX, lastTouchY)
                                                }
                                                if (site == sites.first()) onWebViewCreated(this)
                                                loadUrl(site.url)
                                            }
                                        }
                                        swipeChildWebView = createdWebView
                                        addView(createdWebView)
                                    }
                                },
                                update = { swipeLayout ->
                                    swipeLayout.isEnabled = swipeRefreshEnabled
                                    if (swipeLayout.isRefreshing != isRefreshing) {
                                        swipeLayout.isRefreshing = isRefreshing
                                    }
                                    if (!isRefreshing && swipeLayout.isRefreshing) {
                                        swipeLayout.isRefreshing = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (currentSite == null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Language, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No site selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSiteItem(
    site: MultiWebSiteShellConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "drawerItemBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Site icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (site.iconEmoji.isNotBlank()) {
                Text(site.iconEmoji, fontSize = 18.sp)
            } else {
                Icon(
                    Icons.Outlined.Language, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                site.name.ifBlank { extractDomain(site.url) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                extractDomain(site.url),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            Icon(
                Icons.Default.Check, null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================
// Utilities
// ============================================================

private fun extractDomain(url: String): String {
    return try {
        URL(url).host.removePrefix("www.")
    } catch (e: Exception) {
        url.removePrefix("https://").removePrefix("http://").substringBefore("/")
    }
}

/**
 * Fetch feed items from configured sites using CSS selectors.
 * Uses Jsoup-like HTML parsing via regex fallback (since Jsoup may not be available).
 */
private fun fetchFeedItems(sites: List<MultiWebSiteShellConfig>): List<FeedItem> {
    val allItems = mutableListOf<FeedItem>()

    for (site in sites) {
        if (site.url.isBlank()) continue
        val siteName = site.name.ifBlank { extractDomain(site.url) }

        try {
            val connection = URL(site.url).openConnection()
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            val html = connection.getInputStream().bufferedReader().readText()

            if (site.cssSelector.isNotBlank()) {
                // Simple CSS selector-based extraction
                // Supports: "tag.class", "tag#id", ".class", "#id", "tag"
                val elements = extractElementsByCss(html, site.cssSelector)
                elements.forEach { element ->
                    val text = extractTextFromHtml(element).trim()
                    val href = extractHrefFromHtml(element)
                    if (text.isNotBlank() && text.length > 5) {
                        val fullUrl = resolveUrl(site.url, href ?: "")
                        allItems.add(
                            FeedItem(
                                title = text,
                                url = fullUrl.ifBlank { site.url },
                                siteName = siteName,
                                siteEmoji = site.iconEmoji,
                                siteUrl = site.url
                            )
                        )
                    }
                }
            } else {
                // Fallback: extract all <a> tags with substantial text
                val linkPattern = Regex("""<a\s[^>]*href\s*=\s*["']([^"']*)["'][^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
                linkPattern.findAll(html).forEach { match ->
                    val href = match.groupValues[1]
                    val text = extractTextFromHtml(match.groupValues[2]).trim()
                    if (text.length in 10..200 && !text.contains("<") && href.isNotBlank()) {
                        val fullUrl = resolveUrl(site.url, href)
                        allItems.add(
                            FeedItem(
                                title = text,
                                url = fullUrl,
                                siteName = siteName,
                                siteEmoji = site.iconEmoji,
                                siteUrl = site.url
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w("MultiWebFeed", "Failed to fetch ${site.url}: ${e.message}")
        }
    }

    return allItems.distinctBy { it.title }.take(100)
}

/**
 * Simple CSS selector element extraction from raw HTML.
 * Supports selectors like: "h2 a", ".post-title a", "article h2", "#content a"
 */
private fun extractElementsByCss(html: String, selector: String): List<String> {
    // Parse the last element in a compound selector (e.g. "article h2 a" → "a")
    val parts = selector.trim().split(Regex("\\s+"))
    val targetSelector = parts.last()
    
    val tag: String?
    val className: String?
    val idName: String?
    
    when {
        targetSelector.contains(".") -> {
            val split = targetSelector.split(".", limit = 2)
            tag = split[0].ifBlank { null }
            className = split[1]
            idName = null
        }
        targetSelector.contains("#") -> {
            val split = targetSelector.split("#", limit = 2)
            tag = split[0].ifBlank { null }
            idName = split[1]
            className = null
        }
        else -> {
            tag = targetSelector
            className = null
            idName = null
        }
    }

    val results = mutableListOf<String>()
    
    // Build regex based on selector
    val tagPattern = tag ?: "[a-zA-Z][a-zA-Z0-9]*"
    val attrPattern = buildString {
        append("""<$tagPattern\s""")
        if (className != null) {
            append("""[^>]*class\s*=\s*["'][^"']*\b${Regex.escape(className)}\b[^"']*["']""")
        } else if (idName != null) {
            append("""[^>]*id\s*=\s*["']${Regex.escape(idName)}["']""")
        } else {
            append("[^>]*")
        }
        append(""">.*?</$tagPattern>""")
    }

    try {
        val regex = Regex(attrPattern, setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        regex.findAll(html).take(50).forEach { match ->
            results.add(match.value)
        }
    } catch (e: Exception) {
        AppLogger.w("MultiWebFeed", "CSS selector regex failed: ${e.message}")
    }

    return results
}

private fun extractTextFromHtml(html: String): String {
    return html.replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun extractHrefFromHtml(html: String): String? {
    val match = Regex("""href\s*=\s*["']([^"']*)["']""").find(html)
    return match?.groupValues?.get(1)
}

private fun resolveUrl(baseUrl: String, href: String): String {
    if (href.isBlank()) return baseUrl
    if (href.startsWith("http://") || href.startsWith("https://")) return href
    if (href.startsWith("//")) return "https:$href"
    return try {
        val base = URL(baseUrl)
        if (href.startsWith("/")) {
            "${base.protocol}://${base.host}$href"
        } else {
            val basePath = baseUrl.substringBeforeLast("/")
            "$basePath/$href"
        }
    } catch (e: Exception) {
        href
    }
}
