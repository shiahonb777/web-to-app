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
import com.webtoapp.core.i18n.Strings
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
// 我的模块 Bottom Sheet
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PublishModuleSheet(
    apiClient: CloudApiClient,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Load local modules from ExtensionManager
    val extensionManager = remember { com.webtoapp.core.extension.ExtensionManager.getInstance(context) }
    val localModules by extensionManager.modules.collectAsState()

    // Selected module
    var selectedModule by remember { mutableStateOf<com.webtoapp.core.extension.ExtensionModule?>(null) }
    var showModulePicker by remember { mutableStateOf(false) }

    // Auto-filled from module, but editable
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var shareCode by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("tools") }
    var tags by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCode by remember { mutableIntStateOf(1) }
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }

    // ── Team association state ──
    var myTeams by remember { mutableStateOf<List<com.webtoapp.core.cloud.TeamItem>>(emptyList()) }
    var selectedTeamId by remember { mutableStateOf<Int?>(null) }
    var selectedTeamMembers by remember { mutableStateOf<List<com.webtoapp.core.cloud.TeamMemberItem>>(emptyList()) }
    data class ModContribEntry(val userId: Int, val username: String, val displayName: String?, var role: String = "member", var points: Int = 0, var desc: String = "")
    var contributorEntries by remember { mutableStateOf<List<ModContribEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        when (val result = apiClient.listTeams()) {
            is com.webtoapp.core.auth.AuthResult.Success -> myTeams = result.data.teams
            else -> {}
        }
    }

    LaunchedEffect(selectedTeamId) {
        selectedTeamId?.let { teamId ->
            when (val result = apiClient.getTeamMembers(teamId)) {
                is com.webtoapp.core.auth.AuthResult.Success -> {
                    selectedTeamMembers = result.data
                    contributorEntries = result.data.map { m ->
                        ModContribEntry(m.userId, m.username, m.displayName, if (m.role == "owner") "lead" else "member", 0, "")
                    }
                }
                else -> {}
            }
        } ?: run {
            selectedTeamMembers = emptyList()
            contributorEntries = emptyList()
        }
    }

    // Icon picker
    var iconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var iconUrl by remember { mutableStateOf("") }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { iconUri = it } }

    // Screenshot picker (multi-select)
    var screenshotUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris -> screenshotUris = screenshotUris + uris }

    val moduleCategories = listOf(
        "tools" to Strings.catTools, "ui" to "界面", "media" to Strings.catMedia,
        "social" to Strings.catSocial, "productivity" to "效率",
        "education" to "教育", "entertainment" to "娱乐",
        "developer" to "开发", "other" to Strings.catOther
    )

    // Map ExtensionModule category to store category string
    fun mapCategory(cat: com.webtoapp.core.extension.ModuleCategory): String = when (cat) {
        com.webtoapp.core.extension.ModuleCategory.CONTENT_FILTER,
        com.webtoapp.core.extension.ModuleCategory.CONTENT_ENHANCE -> "tools"
        com.webtoapp.core.extension.ModuleCategory.STYLE_MODIFIER,
        com.webtoapp.core.extension.ModuleCategory.THEME -> "ui"
        com.webtoapp.core.extension.ModuleCategory.MEDIA,
        com.webtoapp.core.extension.ModuleCategory.VIDEO,
        com.webtoapp.core.extension.ModuleCategory.IMAGE,
        com.webtoapp.core.extension.ModuleCategory.AUDIO -> "media"
        com.webtoapp.core.extension.ModuleCategory.SOCIAL -> "social"
        com.webtoapp.core.extension.ModuleCategory.FUNCTION_ENHANCE,
        com.webtoapp.core.extension.ModuleCategory.AUTOMATION,
        com.webtoapp.core.extension.ModuleCategory.DATA_EXTRACT,
        com.webtoapp.core.extension.ModuleCategory.DATA_SAVE -> "productivity"
        com.webtoapp.core.extension.ModuleCategory.READING,
        com.webtoapp.core.extension.ModuleCategory.TRANSLATE -> "education"
        com.webtoapp.core.extension.ModuleCategory.SHOPPING,
        com.webtoapp.core.extension.ModuleCategory.NAVIGATION,
        com.webtoapp.core.extension.ModuleCategory.INTERACTION -> "entertainment"
        com.webtoapp.core.extension.ModuleCategory.DEVELOPER,
        com.webtoapp.core.extension.ModuleCategory.SECURITY,
        com.webtoapp.core.extension.ModuleCategory.ANTI_TRACKING -> "developer"
        com.webtoapp.core.extension.ModuleCategory.ACCESSIBILITY,
        com.webtoapp.core.extension.ModuleCategory.OTHER -> "other"
    }

    // Module picker dialog
    if (showModulePicker) {
        AlertDialog(
            onDismissRequest = { showModulePicker = false },
            title = {
                Column {
                    Text("选择要发布的模块", fontWeight = FontWeight.Bold)
                    Text(
                        "${localModules.size} 个本地模块",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            },
            text = {
                if (localModules.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Transparent,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                                )
                                            ),
                                            RoundedCornerShape(18.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Extension, null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                }
                            }
                            Text("暂无本地模块",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center)
                            Text("请先在模块编辑器中创建模块",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(localModules, key = { it.id }) { module ->
                            val isSelected = selectedModule?.id == module.id
                            Surface(
                                onClick = {
                                    selectedModule = module
                                    // Auto-fill all fields from module metadata
                                    name = module.name
                                    description = module.description
                                    selectedCategory = mapCategory(module.category)
                                    tags = module.tags.joinToString(",")
                                    versionName = module.version.name
                                    versionCode = module.version.code
                                    // Generate share code
                                    shareCode = module.toShareCode()
                                    showModulePicker = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primaryContainer,
                                                            MaterialTheme.colorScheme.tertiaryContainer
                                                        )
                                                    ),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Extension, null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(module.name,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                module.category.getDisplayName(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                "v\${module.version.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Filled.CheckCircle, null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModulePicker = false }) {
                    Text(Strings.storeReviewCancel)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Header ──
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f),
                                            Color.Transparent
                                        )
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    Strings.storePublishModule,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    "选择您已创建的模块发布到市场",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // ── 选择模块 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("选择模块", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "选择已创建的本地模块",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    Surface(
                        onClick = { showModulePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedModule != null)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedModule != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        MaterialTheme.colorScheme.tertiaryContainer
                                                    )
                                                ),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Extension, null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedModule!!.name, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            selectedModule!!.category.getDisplayName(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "v${selectedModule!!.version.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(Icons.Outlined.SwapHoriz, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "点击选择要发布的模块",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Share code auto-generated indicator
                    if (selectedModule != null && shareCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp),
                                            tint = Color(0xFF10B981))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("分享码已自动生成",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("${shareCode.length} 字符",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF10B981).copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }

                // ── 基本信息 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("基本信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "模块名称、图标和版本信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // 模块名称
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("模块名称 *") },
                        placeholder = { Text("如：天气小组件") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(20.dp)) }
                    )
                }

                // 图标
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            if (iconUri != null) {
                                AsyncImage(
                                    model = iconUri,
                                    contentDescription = "图标预览",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (iconUrl.isNotBlank()) {
                                AsyncImage(
                                    model = iconUrl,
                                    contentDescription = "图标预览",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                )
                                            ),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Extension, null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { iconPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (iconUri != null) "更换图标" else "选择图标")
                            }
                            Text("从相册选择模块图标 (可选)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── 分类和标签 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "选择模块所属分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(moduleCategories) { (key, label) ->
                            PremiumFilterChip(
                                selected = selectedCategory == key,
                                onClick = { selectedCategory = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // 版本
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            label = { Text("版本名") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = versionCode.toString(),
                            onValueChange = { versionCode = it.toIntOrNull() ?: 1 },
                            label = { Text("版本号") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // ── 描述和标签 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("描述和标签", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "详细描述模块功能，添加标签方便搜索",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("模块描述 (支持 Markdown)") },
                        placeholder = { Text("描述模块的功能和用途...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("标签 (逗号分隔)") },
                        placeholder = { Text("天气,工具,小组件") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── 截图 ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("截图 (可选)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "添加截图让用户提前预览效果",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                if (screenshotUris.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(screenshotUris.size) { index ->
                                Box(modifier = Modifier.size(85.dp, 150.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxSize(),
                                        shadowElevation = 2.dp
                                    ) {
                                        AsyncImage(
                                            model = screenshotUris[index],
                                            contentDescription = "截图 ${index + 1}",
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            screenshotUris = screenshotUris.toMutableList().also { it.removeAt(index) }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(22.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { screenshotPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从相册添加截图",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (screenshotUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("已添加 ${screenshotUris.size} 张截图",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }

                // ── 手动分享码输入 (高级) ──
                if (selectedModule == null) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                            Text("手动输入分享码", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("如果不选择本地模块，也可以直接粘贴分享码",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }

                    item {
                        OutlinedTextField(
                            value = shareCode,
                            onValueChange = { shareCode = it },
                            label = { Text("模块分享码 *") },
                            placeholder = { Text("粘贴模块的分享码") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            supportingText = { Text("在模块编辑器中导出获得的分享码") },
                            minLines = 3,
                            maxLines = 6
                        )
                    }
                }

                // ── Upload progress ──
                if (isPublishing && uploadProgress > 0f) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text(uploadStatus.ifBlank { "上传中..." },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium)
                                    }
                                    Text("${(uploadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(uploadProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                                ),
                                                RoundedCornerShape(2.5.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 关联团队 (可选) ──
                if (myTeams.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                            Text(Strings.teamAssociate, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text("可选", modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontSize = 10.sp)
                            }
                        }
                        Text("关联团队后，团队和成员贡献信息将在模块详情及成员主页展示",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }

                    item {
                        var teamDropdownExpanded by remember { mutableStateOf(false) }
                        val selectedTeam = myTeams.find { it.id == selectedTeamId }
                        ExposedDropdownMenuBox(expanded = teamDropdownExpanded, onExpandedChange = { teamDropdownExpanded = it }) {
                            OutlinedTextField(
                                value = selectedTeam?.name ?: "", onValueChange = {}, readOnly = true,
                                label = { Text(Strings.teamSelectTeam) }, placeholder = { Text("点击选择团队") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Outlined.Groups, null, modifier = Modifier.size(20.dp)) }
                            )
                            ExposedDropdownMenu(expanded = teamDropdownExpanded, onDismissRequest = { teamDropdownExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("不关联团队", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    leadingIcon = { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp)) },
                                    onClick = { selectedTeamId = null; teamDropdownExpanded = false }
                                )
                                HorizontalDivider()
                                myTeams.forEach { team ->
                                    DropdownMenuItem(
                                        text = { Column { Text(team.name, fontWeight = FontWeight.Medium); Text("${team.memberCount} 成员", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                                        leadingIcon = { Icon(Icons.Outlined.Groups, null, Modifier.size(18.dp)) },
                                        onClick = { selectedTeamId = team.id; teamDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedTeamId != null && contributorEntries.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("设置贡献者角色与贡献点", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        contributorEntries.forEachIndexed { index, entry ->
                            item(key = "mcontrib_${entry.userId}") {
                                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                    border = if (entry.role == "lead") BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(modifier = Modifier.size(32.dp), shape = CircleShape,
                                                color = if (entry.role == "lead") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Text((entry.displayName ?: entry.username).take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                                        color = if (entry.role == "lead") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(entry.displayName ?: entry.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text("@${entry.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilterChip(selected = entry.role == "lead", onClick = { contributorEntries = contributorEntries.toMutableList().also { it[index] = entry.copy(role = "lead") } },
                                                    label = { Text(Strings.teamLead, fontSize = 11.sp) }, modifier = Modifier.height(28.dp))
                                                FilterChip(selected = entry.role == "member", onClick = { contributorEntries = contributorEntries.toMutableList().also { it[index] = entry.copy(role = "member") } },
                                                    label = { Text(Strings.teamMemberRole, fontSize = 11.sp) }, modifier = Modifier.height(28.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = entry.points.toString(), onValueChange = { v -> val p = v.toIntOrNull() ?: 0; contributorEntries = contributorEntries.toMutableList().also { it[index] = entry.copy(points = p) } },
                                                label = { Text(Strings.teamContributionPoints, fontSize = 11.sp) }, modifier = Modifier.width(100.dp), shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                                            OutlinedTextField(value = entry.desc, onValueChange = { v -> contributorEntries = contributorEntries.toMutableList().also { it[index] = entry.copy(desc = v) } },
                                                label = { Text(Strings.teamContributionDesc, fontSize = 11.sp) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), singleLine = true,
                                                placeholder = { Text("如：功能开发、测试", fontSize = 12.sp) }, textStyle = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 发布按钮 ──
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        enabled = !isPublishing,
                        onClick = {
                            if (name.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("请输入模块名称") }
                                return@Button
                            }
                            if (shareCode.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("请选择模块或输入分享码") }
                                return@Button
                            }
                            scope.launch {
                                isPublishing = true
                                uploadProgress = 0f

                                // Helper: convert content URI to temp file
                                fun uriToTempFile(uri: android.net.Uri, prefix: String, ext: String): java.io.File? {
                                    return try {
                                        val input = context.contentResolver.openInputStream(uri) ?: return null
                                        val tempFile = java.io.File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
                                        tempFile.outputStream().use { out -> input.copyTo(out) }
                                        input.close()
                                        tempFile
                                    } catch (e: Exception) { null }
                                }

                                // Step 1: Upload icon if selected locally
                                var finalIconUrl = iconUrl.ifBlank { null }
                                if (iconUri != null) {
                                    uploadStatus = "正在上传图标..."
                                    uploadProgress = 0.1f
                                    val iconFile = uriToTempFile(iconUri!!, "icon", "png")
                                    if (iconFile != null) {
                                        when (val r = apiClient.uploadAsset(iconFile, "image/png")) {
                                            is com.webtoapp.core.auth.AuthResult.Success -> finalIconUrl = r.data
                                            is com.webtoapp.core.auth.AuthResult.Error -> {
                                                uploadStatus = "图标上传失败: ${r.message}"
                                            }
                                        }
                                        iconFile.delete()
                                    }
                                }

                                // Step 2: Publish module info
                                uploadStatus = "正在发布模块..."
                                uploadProgress = 0.8f

                                try {
                                    val result = apiClient.publishModule(
                                        name = name,
                                        description = description,
                                        icon = finalIconUrl,
                                        category = selectedCategory,
                                        tags = tags.ifBlank { null },
                                        versionName = versionName.ifBlank { null },
                                        versionCode = versionCode,
                                        shareCode = shareCode
                                    )
                                    uploadProgress = 1f
                                    when (result) {
                                        is com.webtoapp.core.auth.AuthResult.Success -> {
                                            // Associate team if selected
                                            val publishedModuleId = result.data.id
                                            if (selectedTeamId != null && contributorEntries.isNotEmpty()) {
                                                val hasLead = contributorEntries.any { it.role == "lead" }
                                                if (hasLead) {
                                                    val contribs = contributorEntries.map { e ->
                                                        com.webtoapp.core.cloud.ContributorInput(
                                                            userId = e.userId, contributorRole = e.role,
                                                            contributionPoints = e.points, description = e.desc.ifBlank { null }
                                                        )
                                                    }
                                                    val teamResult = apiClient.associateModuleTeam(moduleId = publishedModuleId, teamId = selectedTeamId!!, contributors = contribs)
                                                    when (teamResult) {
                                                        is com.webtoapp.core.auth.AuthResult.Success ->
                                                            snackbarHostState.showSnackbar("${Strings.storeModulePublishSuccess} · 团队已关联")
                                                        is com.webtoapp.core.auth.AuthResult.Error ->
                                                            snackbarHostState.showSnackbar("${Strings.storeModulePublishSuccess} · 团队关联失败: ${teamResult.message}")
                                                    }
                                                } else {
                                                    snackbarHostState.showSnackbar("${Strings.storeModulePublishSuccess} · 团队关联需至少一位主负责人")
                                                }
                                            } else {
                                                snackbarHostState.showSnackbar(Strings.storeModulePublishSuccess)
                                            }
                                            onPublished()
                                        }
                                        is com.webtoapp.core.auth.AuthResult.Error -> {
                                            snackbarHostState.showSnackbar("发布失败: ${result.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("网络错误: ${e.message}")
                                } finally {
                                    isPublishing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("发布中...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Outlined.Publish, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.storePublishModule, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
