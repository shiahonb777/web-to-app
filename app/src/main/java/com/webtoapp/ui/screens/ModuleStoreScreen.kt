package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.components.PremiumFilterChip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import org.koin.compose.koinInject
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.StoreModuleInfo
import com.webtoapp.core.extension.ExtensionModule
import com.webtoapp.core.extension.ModuleCategory
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * 模块市场页面 — 社区扩展模块的发现、搜索与安装
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleStoreScreen(
    cloudViewModel: CloudViewModel,
    onBack: () -> Unit,
    onInstallModule: (String) -> Unit   // share_code -> install locally
) {
    val modules by cloudViewModel.storeModules.collectAsStateWithLifecycle()
    val loading by cloudViewModel.storeLoading.collectAsStateWithLifecycle()
    val total by cloudViewModel.storeTotal.collectAsStateWithLifecycle()
    val message by cloudViewModel.message.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf("downloads") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        cloudViewModel.loadStoreModules()
    }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message!!)
            cloudViewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "模块市场",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${total} 个模块",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 搜索
                    var showSearch by remember { mutableStateOf(false) }
                    if (showSearch) {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                cloudViewModel.loadStoreModules(selectedCategory, it.ifBlank { null }, selectedSort)
                            },
                            placeholder = { Text("搜索模块...", fontSize = 14.sp) },
                            modifier = Modifier.width(200.dp).height(48.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            )
                        )
                    }
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch && searchQuery.isNotBlank()) {
                            searchQuery = ""
                            cloudViewModel.loadStoreModules(selectedCategory, null, selectedSort)
                        }
                    }) {
                        Icon(if (showSearch) Icons.Filled.Close else Icons.Outlined.Search, "搜索")
                    }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 分类过滤
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        PremiumFilterChip(
                            selected = selectedCategory == null,
                            onClick = {
                                selectedCategory = null
                                cloudViewModel.loadStoreModules(null, searchQuery.ifBlank { null }, selectedSort)
                            },
                            label = { Text("全部", fontSize = 12.sp) }
                        )
                    }
                    val categories = listOf(
                        "UI_ENHANCE" to "界面增强",
                        "MEDIA" to "媒体",
                        "PRIVACY" to "隐私安全",
                        "TOOLS" to "工具",
                        "AD_BLOCK" to "广告拦截",
                        "SOCIAL" to "社交",
                        "DEVELOPER" to "开发者",
                        "OTHER" to "其他"
                    )
                    items(categories) { (key, name) ->
                        PremiumFilterChip(
                            selected = selectedCategory == key,
                            onClick = {
                                selectedCategory = if (selectedCategory == key) null else key
                                cloudViewModel.loadStoreModules(selectedCategory, searchQuery.ifBlank { null }, selectedSort)
                            },
                            label = { Text(name, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // 排序
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sorts = listOf("downloads" to "最多下载", "rating" to "最高评分", "created_at" to "最新发布")
                    sorts.forEach { (key, name) ->
                        PremiumFilterChip(
                            selected = selectedSort == key,
                            onClick = {
                                selectedSort = key
                                cloudViewModel.loadStoreModules(selectedCategory, searchQuery.ifBlank { null }, key)
                            },
                            label = { Text(name, fontSize = 11.sp) },
                        )
                    }
                }
            }

            // 加载中
            if (loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }

            // 模块列表
            items(modules, key = { it.id }) { module ->
                ModuleStoreCard(
                    module = module,
                    onInstall = {
                        cloudViewModel.downloadStoreModule(
                            moduleId = module.id,
                            onResult = { shareCode ->
                                onInstallModule(shareCode)
                            },
                            onError = null
                        )
                    }
                )
            }

            // 空状态
            if (!loading && modules.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Extension,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "没有找到匹配的模块" else "暂无模块，成为第一个发布者吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
        }
}

@Composable
private fun ModuleStoreCard(module: StoreModuleInfo, onInstall: () -> Unit) {
    val installedTracker = koinInject<com.webtoapp.core.cloud.InstalledItemsTracker>()
    val isInstalled = installedTracker.isInstalled(module.id)
    EnhancedElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon — Apple-style gradient tinted
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Extension,
                        null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(module.name, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(weight = 1f, fill = false))
                        if (module.isFeatured) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = Color(0xFFFFA726).copy(alpha = 0.12f)
                            ) {
                                Text("精选", fontSize = 10.sp, color = Color(0xFFFFA726),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("by ${module.authorName}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            if (!module.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(module.description, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }

            // Tags
            if (module.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    module.tags.take(3).forEach { tag ->
                        Surface(shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text(tag, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom bar: stats + install button
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Downloads
                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(3.dp))
                Text("${module.downloads}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.width(12.dp))

                // Rating
                Icon(Icons.Outlined.Star, null, modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFFC107))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    if (module.ratingCount > 0) "${module.rating}" else "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Version
                module.versionName?.let {
                    Text("v$it", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }

                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))

                // Install button — shows "已安装" if already installed
                if (isInstalled) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(15.dp),
                                tint = Color(0xFF4CAF50))
                            Text("已安装", fontSize = 12.sp, color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    FilledTonalButton(
                        onClick = onInstall,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("安装", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
