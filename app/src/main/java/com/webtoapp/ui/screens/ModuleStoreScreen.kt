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
import com.webtoapp.ui.screens.community.ModuleCard

/**
 * 模块市场页面 — 社区扩展模块的发现、搜索与安装
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleStoreScreen(
    cloudViewModel: CloudViewModel,
    onBack: () -> Unit,
    onInstallModule: (String) -> Unit,   // share_code -> install locally
    onNavigateToModule: (Int) -> Unit = {}  // moduleId -> ModuleDetailScreen
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
                            Strings.moduleMarketTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            Strings.moduleCount.format(total),
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
                            placeholder = { Text(Strings.searchModules, fontSize = 14.sp) },
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
                            label = { Text(Strings.allCategories, fontSize = 12.sp) }
                        )
                    }
                    val categories = listOf(
                        "UI_ENHANCE" to Strings.catUiEnhance,
                        "MEDIA" to Strings.catMediaLabel,
                        "PRIVACY" to Strings.catPrivacyLabel,
                        "TOOLS" to Strings.catToolsLabel,
                        "AD_BLOCK" to Strings.catAdBlockLabel,
                        "SOCIAL" to Strings.catSocialLabel,
                        "DEVELOPER" to Strings.catDeveloperLabel,
                        "OTHER" to Strings.catOtherLabel
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
                    val sorts = listOf("downloads" to Strings.sortMostDownloads, "rating" to Strings.sortHighestRating, "created_at" to Strings.sortLatestPublish)
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
                ModuleCard(
                    module = module,
                    onClick = { onNavigateToModule(module.id) },
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
                            if (searchQuery.isNotBlank()) Strings.noMatchingModules else Strings.noModulesYet,
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

