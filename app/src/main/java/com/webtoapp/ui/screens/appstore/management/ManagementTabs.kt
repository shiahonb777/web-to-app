package com.webtoapp.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.AppStoreItem
import com.webtoapp.core.cloud.AppStoreListResponse
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.StoreModuleInfo
import com.webtoapp.core.cloud.ActivationCode
import com.webtoapp.core.cloud.ActivationSettings
import com.webtoapp.core.cloud.Announcement
import com.webtoapp.core.cloud.UpdateConfig
import com.webtoapp.core.cloud.AppUser
import com.webtoapp.core.cloud.GeoDistribution
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.screens.community.Avatar
import com.webtoapp.ui.screens.community.GlassDivider
import com.webtoapp.ui.screens.community.CommunityPhysics
import com.webtoapp.ui.screens.community.LikeBurstEffect
import com.webtoapp.ui.screens.community.AnimatedCounter
import com.webtoapp.ui.screens.community.StaggeredItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ════════════════════════════════════════════════
// appmanagement( Premium UI)
// ════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ManagementOverviewTab(app: AppStoreItem, apiClient: CloudApiClient? = null, scope: kotlinx.coroutines.CoroutineScope? = null, onAppUpdated: (() -> Unit)? = null, onAppDeleted: (() -> Unit)? = null) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editName by remember(app) { mutableStateOf(app.name) }
    var editDescription by remember(app) { mutableStateOf(app.description ?: "") }
    var editCategory by remember(app) { mutableStateOf(app.category) }
    var isUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "tools" to AppStringsProvider.current().catTools, "social" to AppStringsProvider.current().catSocial, "education" to "教育",
        "entertainment" to "娱乐", "productivity" to "效率",
        "lifestyle" to "生活", "business" to "商务",
        "news" to "新闻", "finance" to "金融",
        "health" to "健康", "other" to AppStringsProvider.current().catOther
    )

    // editdialog
    if (showEditDialog && apiClient != null && scope != null) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientBlue)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text("Edit app info", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("App name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("App description") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { (key, label) ->
                            FilterChip(
                                selected = editCategory == key,
                                onClick = { editCategory = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                    updateError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpdating = true
                        updateError = null
                        scope.launch {
                            when (val result = apiClient.updateStoreApp(app.id, editName, editDescription, editCategory)) {
                                is com.webtoapp.core.auth.AuthResult.Success -> {
                                    showEditDialog = false
                                    onAppUpdated?.invoke()
                                }
                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                    updateError = result.message
                                }
                            }
                            isUpdating = false
                        }
                    },
                    enabled = !isUpdating && editName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isUpdating) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }
                    Text(if (isUpdating) "Saving…" else "Save")
                }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }, enabled = !isUpdating) { Text(AppStringsProvider.current().storeReviewCancel) } }
        )
    }

    // delete dialog
    if (showDeleteDialog && apiClient != null && scope != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientRed)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.DeleteForever, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text("Delete app", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("确定要删除「${app.name}」吗？", style = MaterialTheme.typography.bodyMedium)
                    Text("此操作不可撤销，应用的所有数据（激活码、公告、用户数据等）都将被永久删除。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            when (apiClient.deleteStoreApp(app.id)) {
                                is com.webtoapp.core.auth.AuthResult.Success -> {
                                    showDeleteDialog = false
                                    onAppDeleted?.invoke()
                                }
                                is com.webtoapp.core.auth.AuthResult.Error -> { isDeleting = false }
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isDeleting) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(6.dp)) }
                    Text(if (isDeleting) "删除中…" else "确认删除", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) { Text(AppStringsProvider.current().storeReviewCancel) } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        // gradient
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientMiniStat(mgmtGradientBlue, Icons.Outlined.Download, "${app.downloads}", "下载", Modifier.weight(1f))
                GradientMiniStat(mgmtGradientOrange, Icons.Filled.Star, String.format("%.1f", app.rating), AppStringsProvider.current().storeReviewRatingLabel, Modifier.weight(1f))
                GradientMiniStat(mgmtGradientGreen, Icons.Outlined.ThumbUp, "${app.likeCount}", "点赞", Modifier.weight(1f))
            }
        }
        // app card
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), shadowElevation = 1.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientBlue)))
                        Text("应用信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (apiClient != null) {
                            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Edit, "编辑", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    OverviewInfoRow("应用 ID", "#${app.id}")
                    OverviewInfoRow("版本", "v${app.versionName}")
                    OverviewInfoRow("Category", app.category)
                    OverviewInfoRow("包名", app.packageName ?: "—")
                    OverviewInfoRow("发布者", app.authorName)
                    app.createdAt?.let { OverviewInfoRow("发布时间", it.take(10)) }
                }
            }
        }
        // Note
        if (apiClient != null && scope != null) {
            item {
                // editbutton
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(mgmtGradientBlue)).clickable { showEditDialog = true }.padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = Color.White)
                        Text("Edit app info", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                // deletebutton( area)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Text("危险区域", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Text("删除应用后所有数据将不可恢复", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.DeleteForever, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("删除此应用", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// activation code Tab
@Composable
internal fun ManagementActivationTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var settings by remember { mutableStateOf<ActivationSettings?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCodes by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    fun loadSettings() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getActivationSettings(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> settings = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { loadSettings() }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientPurple)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.VpnKey, null, Modifier.size(16.dp), tint = Color.White)
                    }
                    Text("添加激活码", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("选择模板快速生成", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("numeric6" to "🔢 数字码", "standard" to "📋 标准码", "uuid" to "🔗 UUID").forEach { (id, label) ->
                            Surface(
                                onClick = {
                                    selectedTemplate = id
                                    newCodes = when (id) {
                                        "numeric6" -> (1..5).map { String.format("%06d", (100000..999999).random()) }
                                        "standard" -> { val c = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"; (1..5).map { (1..3).joinToString("-") { (1..4).map { c.random() }.joinToString("") } } }
                                        else -> (1..5).map { java.util.UUID.randomUUID().toString() }
                                    }.joinToString("\n")
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedTemplate == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                            ) { Text(label, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 12.sp, fontWeight = if (selectedTemplate == id) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Text("或自定义（每行一个）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = newCodes, onValueChange = { newCodes = it; selectedTemplate = null }, modifier = Modifier.fillMaxWidth().height(150.dp), placeholder = { Text("输入激活码…", fontSize = 13.sp) }, shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) {
                        Text("共 ${newCodes.lines().filter { it.isNotBlank() }.size} 个", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { val cl = newCodes.lines().filter { it.isNotBlank() }.map { it.trim() }; if (cl.isNotEmpty()) { isAdding = true; scope.launch { apiClient.createActivationCodes(app.id, cl); loadSettings(); showAddDialog = false; isAdding = false; newCodes = "" } } }, enabled = !isAdding && newCodes.lines().any { it.isNotBlank() }, shape = RoundedCornerShape(10.dp)) {
                    if (isAdding) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }
                    Text(if (isAdding) "添加中…" else "添加")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }, enabled = !isAdding) { Text(AppStringsProvider.current().storeReviewCancel) } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        if (isLoading) { item { PublishedItemLoadingState("加载激活码…") } }
        else if (errorMsg != null) { item { PublishedItemErrorState(errorMsg) { loadSettings() } } }
        else {
            val s = settings ?: return@LazyColumn
            // gradientsettingscard
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), shadowElevation = 1.dp) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientPurple)))
                            Text("激活码设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column { Text("启用激活码验证", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text("用户需输入激活码才能使用", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                            Switch(checked = s.enabled, onCheckedChange = { scope.launch { apiClient.updateActivationSettings(app.id, it, s.deviceBindingEnabled, s.maxDevicesPerCode); loadSettings() } })
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column { Text("设备绑定", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text("每个激活码绑定一台设备", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                            Switch(checked = s.deviceBindingEnabled, onCheckedChange = { scope.launch { apiClient.updateActivationSettings(app.id, s.enabled, it, s.maxDevicesPerCode); loadSettings() } })
                        }
                    }
                }
            }
            // gradient
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientMiniStat(mgmtGradientPurple, Icons.Outlined.VpnKey, "${s.totalCodes}", "总激活码", Modifier.weight(1f))
                    GradientMiniStat(mgmtGradientGreen, Icons.Outlined.CheckCircle, "${s.usedCodes}", "已使用", Modifier.weight(1f))
                    GradientMiniStat(mgmtGradientOrange, Icons.Outlined.Pending, "${s.totalCodes - s.usedCodes}", "未使用", Modifier.weight(1f))
                }
            }
            // gradient button
            item {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(mgmtGradientPurple)).clickable { showAddDialog = true }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = Color.White)
                        Text("添加激活码", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            // activation codelist
            items(s.codes, key = { it.id }) { code ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (code.isUsed) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(if (code.isUsed) Icons.Outlined.CheckCircle else Icons.Outlined.Pending, null, Modifier.size(18.dp), tint = if (code.isUsed) Color(0xFF10B981) else Color(0xFFF59E0B))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(code.code, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CategoryTag(if (code.isUsed) "已使用" else "未使用", if (code.isUsed) Color(0xFF10B981) else Color(0xFFF59E0B))
                                code.usedByDeviceId?.let { Text("📱 ${it.take(8)}…", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                            }
                        }
                        IconButton(onClick = { scope.launch { apiClient.deleteActivationCode(app.id, code.id); loadSettings() } }, Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

// announcement Tab
@Composable
internal fun ManagementAnnouncementTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var annoTitle by remember { mutableStateOf("") }
    var annoContent by remember { mutableStateOf("") }
    var annoType by remember { mutableStateOf("info") }
    var annoPinned by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getAnnouncements(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> announcements = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    data class ATemplate(val id: String, val name: String, val emoji: String, val title: String, val content: String, val type: String)
    val templates = listOf(
        ATemplate("maintenance", "系统维护", "🔧", "系统维护通知", "尊敬的用户，我们将于 [时间] 进行系统维护升级，预计维护时长 [X] 小时。维护期间可能无法正常使用，敬请谅解。", "warning"),
        ATemplate("feature", "功能更新", "🎉", "新功能上线", "好消息！我们推出了全新的 [功能名称]：\n\n• 新增 [功能1]\n• 优化 [功能2]\n• 修复 [问题]", "info"),
        ATemplate("security", "安全提醒", "🔒", "安全公告", "请注意：\n\n• 请勿分享激活码\n• 定期更新应用\n• 发现异常请联系我们", "warning"),
        ATemplate("event", "活动通知", "🎁", "限时活动", "🎊 [活动名称] 现已开启！\n\n活动时间：[开始] - [结束]\n\n快来参与吧！", "event")
    )

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreating) showCreateDialog = false },
            title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientOrange)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Campaign, null, Modifier.size(16.dp), tint = Color.White) }; Text("发布公告", fontWeight = FontWeight.Bold) } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("选择模板", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        templates.forEach { t -> Surface(onClick = { selectedTemplateId = t.id; annoTitle = t.title; annoContent = t.content; annoType = t.type }, shape = RoundedCornerShape(10.dp), color = if (selectedTemplateId == t.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) { Text("${t.emoji} ${t.name}", Modifier.padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = if (selectedTemplateId == t.id) FontWeight.Bold else FontWeight.Normal) } }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    OutlinedTextField(value = annoTitle, onValueChange = { annoTitle = it; selectedTemplateId = null }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = annoContent, onValueChange = { annoContent = it; selectedTemplateId = null }, label = { Text("内容") }, modifier = Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("info" to "ℹ️ 通知", "warning" to "⚠️ 警告", "event" to "🎁 活动").forEach { (type, label) -> FilterChip(selected = annoType == type, onClick = { annoType = type }, label = { Text(label, fontSize = 12.sp) }) } }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("📌 置顶", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Switch(checked = annoPinned, onCheckedChange = { annoPinned = it }) }
                }
            },
            confirmButton = { Button(onClick = { isCreating = true; scope.launch { apiClient.createAnnouncement(app.id, annoTitle, annoContent, annoType, annoPinned); load(); showCreateDialog = false; isCreating = false; annoTitle = ""; annoContent = ""; selectedTemplateId = null } }, enabled = !isCreating && annoTitle.isNotBlank() && annoContent.isNotBlank(), shape = RoundedCornerShape(10.dp)) { if (isCreating) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }; Text(if (isCreating) "发布中…" else "发布") } },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }, enabled = !isCreating) { Text(AppStringsProvider.current().storeReviewCancel) } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(mgmtGradientOrange)).clickable { showCreateDialog = true }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Outlined.Campaign, null, Modifier.size(18.dp), tint = Color.White); Text("发布新公告", fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
        if (isLoading) { item { PublishedItemLoadingState("加载公告…") } }
        else if (errorMsg != null) { item { PublishedItemErrorState(errorMsg) { load() } } }
        else if (announcements.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.Campaign, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)); Text("暂无公告", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } } }
        } else {
            items(announcements, key = { it.id }) { anno ->
                val typeGrad = when (anno.type) { "warning" -> mgmtGradientOrange; "event" -> mgmtGradientPurple; else -> mgmtGradientBlue }
                val typeLabel = when (anno.type) { "warning" -> "⚠️ 警告"; "event" -> "🎁 活动"; else -> "ℹ️ 通知" }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(typeGrad)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                                if (anno.isPinned) Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFEF4444).copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text("📌 置顶", fontSize = 10.sp, color = Color(0xFFEF4444)) }
                            }
                            IconButton(onClick = { scope.launch { apiClient.deleteAnnouncement(app.id, anno.id); load() } }, Modifier.size(28.dp)) { Icon(Icons.Outlined.Delete, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) }
                        }
                        Text(anno.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(anno.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("👁 ${anno.viewCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)); anno.createdAt?.let { Text(it.take(10), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)) } }
                    }
                }
            }
        }
    }
}

// update Tab
@Composable
internal fun ManagementUpdateTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var config by remember { mutableStateOf<UpdateConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showPushDialog by remember { mutableStateOf(false) }
    var pushVersionName by remember { mutableStateOf("") }; var pushVersionCode by remember { mutableStateOf("") }
    var pushTitle by remember { mutableStateOf("") }; var pushContent by remember { mutableStateOf("") }
    var pushForce by remember { mutableStateOf(false) }
    var useR2 by remember { mutableStateOf(false) }
    var pushTemplateId by remember { mutableStateOf("simple") }; var isPushing by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    // App selector state
    var myApps by remember { mutableStateOf<List<AppStoreItem>>(emptyList()) }
    var selectedSourceApp by remember { mutableStateOf<AppStoreItem?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var isLoadingApps by remember { mutableStateOf(false) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getUpdateConfig(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> config = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    // Load my apps list when dialog opened
    LaunchedEffect(showPushDialog) {
        if (showPushDialog && myApps.isEmpty()) {
            isLoadingApps = true
            when (val r = apiClient.listMyApps()) {
                is com.webtoapp.core.auth.AuthResult.Success -> {
                    myApps = r.data.apps.filter { it.id != app.id }
                }
                is com.webtoapp.core.auth.AuthResult.Error -> { /* ignore */ }
            }
            isLoadingApps = false
        }
    }

    data class UTemplate(val id: String, val name: String, val desc: String, val t: String, val c: String)
    val templates = listOf(
        UTemplate("simple", "📝 简约", "列表式更新日志", "发现新版本 v[版本号]", "🔸 新增 [功能1]\n🔸 优化 [功能2]\n🔸 修复 [问题]"),
        UTemplate("dialog", "💬 弹窗", "半屏弹窗 + 柔和动效", "有新版本可用", "请更新到最新版本以获得更好的体验。"),
        UTemplate("fullscreen", "🖥 全屏", "全屏卡片 + 大图", "重大更新！", "🎉 全新版本！\n\n✨ 全新界面\n⚡ 性能提升\n🔒 安全增强")
    )

    if (showPushDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPushing) showPushDialog = false },
            title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(mgmtGradientGreen)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(16.dp), tint = Color.White) }; Text("推送更新", fontWeight = FontWeight.Bold) } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("更新模板", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        templates.forEach { t -> Surface(onClick = { selectedTemplateId = t.id; pushTemplateId = t.id; pushTitle = t.t; pushContent = t.c }, shape = RoundedCornerShape(10.dp), color = if (selectedTemplateId == t.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) { Column(Modifier.padding(8.dp)) { Text(t.name, fontSize = 11.sp, fontWeight = if (selectedTemplateId == t.id) FontWeight.Bold else FontWeight.Normal); Text(t.desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) } } }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pushVersionName, onValueChange = { pushVersionName = it }, label = { Text("版本号", fontSize = 12.sp) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("1.2.0", fontSize = 12.sp) })
                        OutlinedTextField(value = pushVersionCode, onValueChange = { pushVersionCode = it }, label = { Text("版本代码", fontSize = 12.sp) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("2", fontSize = 12.sp) })
                    }
                    OutlinedTextField(value = pushTitle, onValueChange = { pushTitle = it; selectedTemplateId = null }, label = { Text("更新标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = pushContent, onValueChange = { pushContent = it; selectedTemplateId = null }, label = { Text("更新内容") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))

                    // ── App Selector (replaces APK URL input) ──
                    Text("关联更新应用", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        onClick = { showAppPicker = !showAppPicker },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedSourceApp != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (selectedSourceApp != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (selectedSourceApp != null) {
                                val sa = selectedSourceApp!!
                                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(mgmtGradientGreen)), contentAlignment = Alignment.Center) {
                                    if (sa.icon != null) AsyncImage(sa.icon, null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    else Icon(Icons.Filled.Apps, null, Modifier.size(16.dp), tint = Color.White)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(sa.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("v${sa.versionName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Outlined.Apps, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text("选择一个已发布的应用作为更新源", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Icon(if (showAppPicker) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }

                    // Expandable app list
                    androidx.compose.animation.AnimatedVisibility(visible = showAppPicker) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))) {
                            Column(Modifier.fillMaxWidth().padding(6.dp).heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                                if (isLoadingApps) {
                                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
                                } else if (myApps.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) { Text("暂无其他已发布的应用", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                                } else {
                                    myApps.forEach { item ->
                                        val isSelected = selectedSourceApp?.id == item.id
                                        Surface(
                                            onClick = { selectedSourceApp = item; showAppPicker = false; if (pushVersionName.isBlank()) pushVersionName = item.versionName },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                        ) {
                                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                                    if (item.icon != null) AsyncImage(item.icon, null, Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                                    else Text(item.name.take(1), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Column(Modifier.weight(1f)) {
                                                    Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("v${item.versionName} · ${item.downloads} 次下载", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                }
                                                if (isSelected) Icon(Icons.Filled.RadioButtonChecked, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                else Icon(Icons.Filled.RadioButtonUnchecked, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (pushForce) Color(0xFFEF4444).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)) {
                        Row(Modifier.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column { Text(if (pushForce) "⚠️ 强制更新" else "可选更新", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text(if (pushForce) "用户必须更新才能继续使用" else "用户可以选择稍后更新", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                            Switch(checked = pushForce, onCheckedChange = { pushForce = it })
                        }
                    }

                    // R2
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (useR2) Color(0xFF3B82F6).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)) {
                        Row(Modifier.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (useR2) "🚀 R2 CDN 加速" else "☁️ R2 云存储", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(if (useR2) "APK 将通过 Cloudflare R2 CDN 全球加速分发" else "启用 R2 存储以获得更快的下载速度", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            Switch(checked = useR2, onCheckedChange = { useR2 = it })
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { isPushing = true; scope.launch { apiClient.pushUpdate(app.id, pushVersionName, pushVersionCode.toIntOrNull() ?: 1, pushTitle, pushContent, selectedSourceApp?.id, pushForce, 0, pushTemplateId); load(); showPushDialog = false; isPushing = false } }, enabled = !isPushing && pushVersionName.isNotBlank() && pushTitle.isNotBlank(), shape = RoundedCornerShape(10.dp)) { if (isPushing) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)) }; Text(if (isPushing) "推送中…" else "推送更新") } },
            dismissButton = { TextButton(onClick = { showPushDialog = false }, enabled = !isPushing) { Text(AppStringsProvider.current().storeReviewCancel) } }
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(mgmtGradientGreen)).clickable { showPushDialog = true }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(18.dp), tint = Color.White); Text("推送新版本", fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
        if (isLoading) { item { PublishedItemLoadingState("加载更新配置…") } }
        else if (errorMsg != null) { item { PublishedItemErrorState(errorMsg) { load() } } }
        else {
            val c = config ?: return@LazyColumn
            if (c.isActive) {
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f), shadowElevation = 1.dp) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientGreen)))
                                Text("当前更新配置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(if (c.isForceUpdate) mgmtGradientRed else mgmtGradientGreen)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(if (c.isForceUpdate) "强制更新" else "可选更新", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            OverviewInfoRow("目标版本", "v${c.latestVersionName} (${c.latestVersionCode})")
                            OverviewInfoRow("更新标题", c.updateTitle)
                            OverviewInfoRow("模板", c.templateId)
                            c.sourceAppName?.let { OverviewInfoRow("更新源应用", it) } ?: c.apkUrl?.let { OverviewInfoRow("APK", it.take(35) + "…") }
                            Text(c.updateContent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                        }
                    }
                }
            } else {
                item { Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.SystemUpdate, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)); Text("暂未配置更新推送", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } } }
            }
        }
    }
}

// user Tab
@Composable
internal fun ManagementUsersTab(app: AppStoreItem, apiClient: CloudApiClient, scope: kotlinx.coroutines.CoroutineScope) {
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var geoData by remember { mutableStateOf<List<GeoDistribution>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showGeo by remember { mutableStateOf(false) }

    fun load() { scope.launch { isLoading = true; errorMsg = null; when (val r = apiClient.getAppUsers(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> users = r.data; is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = r.message }; when (val r = apiClient.getUserGeoDistribution(app.id)) { is com.webtoapp.core.auth.AuthResult.Success -> geoData = r.data; is com.webtoapp.core.auth.AuthResult.Error -> {} }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        if (isLoading) { item { PublishedItemLoadingState("加载用户…") } }
        else if (errorMsg != null) { item { PublishedItemErrorState(errorMsg) { load() } } }
        else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientMiniStat(mgmtGradientBlue, Icons.Outlined.People, "${users.size}", "总用户", Modifier.weight(1f))
                    GradientMiniStat(mgmtGradientGreen, Icons.Outlined.FiberManualRecord, "${users.count { it.isActive }}", "活跃", Modifier.weight(1f))
                    GradientMiniStat(mgmtGradientPurple, Icons.Outlined.Public, "${geoData.size}", "国家/地区", Modifier.weight(1f))
                }
            }
            // geo distribution
            item {
                Surface(Modifier.fillMaxWidth().clickable { showGeo = !showGeo }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientRed))); Text("🌍 地理分布", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                        Icon(if (showGeo) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
            if (showGeo && geoData.isNotEmpty()) {
                items(geoData) { geo ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Text(countryFlag(geo.countryCode), fontSize = 20.sp); Text(geo.country, fontWeight = FontWeight.SemiBold) }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("${geo.count}", fontWeight = FontWeight.Bold); Text("${String.format("%.1f", geo.percentage)}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                            }
                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                                Box(Modifier.fillMaxWidth(geo.percentage / 100f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brush.linearGradient(mgmtGradientBlue)))
                            }
                            geo.regions.take(3).forEach { r -> Row(Modifier.fillMaxWidth().padding(start = 28.dp), Arrangement.SpaceBetween) { Text(r.region, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)); Text("${r.count}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } }
                        }
                    }
                }
            }
            // userlist
            item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(4.dp, 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(mgmtGradientBlue))); Text("👤 用户列表", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) } }
            if (users.isEmpty()) { item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { Text("暂无用户数据", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } } }
            else {
                items(users, key = { it.id }) { user ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(mgmtGradientBlue)), contentAlignment = Alignment.Center) {
                                Text(user.id.take(2).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${user.id.take(12)}…", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                    if (user.isActive) Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    user.deviceModel?.let { Text("📱 $it", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                                    user.country?.let { Text("${countryFlag(it)} $it", fontSize = 10.sp) }
                                    user.appVersion?.let { Text("v$it", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                                }
                            }
                            user.activationCode?.let { Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Brush.linearGradient(mgmtGradientGreen.map { c -> c.copy(alpha = 0.15f) })).padding(horizontal = 7.dp, vertical = 3.dp)) { Text("✓ 已激活", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981)) } }
                        }
                    }
                }
            }
        }
    }
}

/** country code emoji */
internal fun countryFlag(countryCode: String): String {
    if (countryCode.length != 2) return "🌍"
    val first = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

// ════════════════════════════════════════════════
// app Bottom Sheet
// ════════════════════════════════════════════════
