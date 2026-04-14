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
// appmanagement( Premium UI)
// ════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PublishAppSheet(
    apiClient: CloudApiClient,
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    val allProjects by webAppRepository.allWebApps.collectAsStateWithLifecycle(initialValue = emptyList())

    // Selected project
    var selectedProject by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    var showProjectPicker by remember { mutableStateOf(false) }

    // Auto-filled from project, but editable
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("other") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCode by remember { mutableStateOf("1") }
    var packageName by remember { mutableStateOf("") }
    var iconUrl by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var screenshotUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var contactEmail by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var privacyPolicyUrl by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }

    // activation codeconfig
    var enableActivation by remember { mutableStateOf(false) }
    var enableDeviceBinding by remember { mutableStateOf(false) }
    var activationCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCodeTemplate by remember { mutableStateOf<String?>(null) }
    var customCodeInput by remember { mutableStateOf("") }


    var myTeams by remember { mutableStateOf<List<com.webtoapp.core.cloud.TeamItem>>(emptyList()) }
    var selectedTeamId by remember { mutableStateOf<Int?>(null) }
    var selectedTeamMembers by remember { mutableStateOf<List<com.webtoapp.core.cloud.TeamMemberItem>>(emptyList()) }
    data class ContribEntry(val userId: Int, val username: String, val displayName: String?, var role: String = "member", var points: Int = 0, var desc: String = "")
    var contributorEntries by remember { mutableStateOf<List<ContribEntry>>(emptyList()) }
    var isLoadingTeams by remember { mutableStateOf(false) }

    // Load user's teams on mount
    LaunchedEffect(Unit) {
        isLoadingTeams = true
        when (val result = apiClient.listTeams()) {
            is com.webtoapp.core.auth.AuthResult.Success -> myTeams = result.data.teams
            else -> {}
        }
        isLoadingTeams = false
    }

    // Load members when team is selected
    LaunchedEffect(selectedTeamId) {
        selectedTeamId?.let { teamId ->
            when (val result = apiClient.getTeamMembers(teamId)) {
                is com.webtoapp.core.auth.AuthResult.Success -> {
                    selectedTeamMembers = result.data
                    // Auto-populate contributor entries from members
                    contributorEntries = result.data.map { m ->
                        ContribEntry(m.userId, m.username, m.displayName, if (m.role == "owner") "lead" else "member", 0, "")
                    }
                }
                else -> {}
            }
        } ?: run {
            selectedTeamMembers = emptyList()
            contributorEntries = emptyList()
        }
    }

    // The built APK file from the selected project
    var selectedApkFile by remember { mutableStateOf<java.io.File?>(null) }

    // Inline build state
    var isBuilding by remember { mutableStateOf(false) }
    var buildProgress by remember { mutableIntStateOf(0) }
    var buildProgressText by remember { mutableStateOf("") }
    var buildError by remember { mutableStateOf<String?>(null) }

    // Screenshot picker (multi-select)
    var screenshotUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        screenshotUris = screenshotUris + uris
    }

    // Icon picker
    var iconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { iconUri = it }
    }

    val categories = listOf(
        "tools" to Strings.catTools, "social" to Strings.catSocial, "education" to "教育",
        "entertainment" to "娱乐", "productivity" to "效率",
        "lifestyle" to "生活", "business" to "商务",
        "news" to "新闻", "finance" to "金融",
        "health" to "健康", "other" to Strings.catOther
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                    Strings.storePublishApp,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    "选择您已创建的应用发布到商店",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // selectapp
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("选择应用", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "选择已创建的应用项目",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    Surface(
                        onClick = { showProjectPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedProject != null)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedProject != null) {
                                // Show selected project info
                                if (selectedProject!!.iconPath != null) {
                                    AsyncImage(
                                        model = selectedProject!!.iconPath,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
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
                                            Icon(
                                                Icons.Outlined.Android, null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedProject!!.name, fontWeight = FontWeight.SemiBold)
                                    Row {
                                        Text(
                                            selectedProject!!.appType.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (selectedApkFile != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "✅ APK 已就绪",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.Outlined.SwapHoriz, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Icon(Icons.Outlined.Apps, null, modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "点击选择要发布的应用",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // APK build section
                    if (selectedProject != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        if (selectedApkFile != null && !isBuilding) {
                            // APK found — show info
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
                                        Text(selectedApkFile!!.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${selectedApkFile!!.length() / 1024} KB · APK 已就绪",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981).copy(alpha = 0.8f))
                                    }
                                    // Rebuild button
                                    TextButton(onClick = {
                                        buildError = null
                                        isBuilding = true
                                        buildProgress = 0
                                        buildProgressText = "准备构建..."
                                        scope.launch {
                                            val apkBuilder = com.webtoapp.core.apkbuilder.ApkBuilder(context)
                                            val result = apkBuilder.buildApk(selectedProject!!) { p, t ->
                                                buildProgress = p
                                                buildProgressText = t
                                            }
                                            when (result) {
                                                is com.webtoapp.core.apkbuilder.BuildResult.Success -> {
                                                    selectedApkFile = result.apkFile
                                                    buildError = null
                                                }
                                                is com.webtoapp.core.apkbuilder.BuildResult.Error -> {
                                                    buildError = result.message
                                                }
                                            }
                                            isBuilding = false
                                        }
                                    }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("重新构建", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        } else if (isBuilding) {
                            // Building in progress
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text("正在构建 APK...",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("$buildProgress%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(2.5.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(buildProgress / 100f)
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.tertiary
                                                        )
                                                    ),
                                                    RoundedCornerShape(2.5.dp)
                                                )
                                        )
                                    }
                                    Text(buildProgressText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        } else {
                            // No APK — show build button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Build, null, modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("需要先构建 APK 才能发布",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            buildError = null
                                            isBuilding = true
                                            buildProgress = 0
                                            buildProgressText = "准备构建..."
                                            scope.launch {
                                                val apkBuilder = com.webtoapp.core.apkbuilder.ApkBuilder(context)
                                                val result = apkBuilder.buildApk(selectedProject!!) { p, t ->
                                                    buildProgress = p
                                                    buildProgressText = t
                                                }
                                                when (result) {
                                                    is com.webtoapp.core.apkbuilder.BuildResult.Success -> {
                                                        selectedApkFile = result.apkFile
                                                        buildError = null
                                                    }
                                                    is com.webtoapp.core.apkbuilder.BuildResult.Error -> {
                                                        buildError = result.message
                                                    }
                                                }
                                                isBuilding = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Icon(Icons.Outlined.Build, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("一键构建 APK", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Build error
                            if (buildError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Outlined.Error, null, modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("构建失败: $buildError",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                    }
                }

                // Note
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("基本信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "应用名称、图标和版本信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // app
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("应用名称 *") },
                        placeholder = { Text("如：我的工具箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.Apps, null, modifier = Modifier.size(20.dp)) }
                    )
                }

                // icon
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Icon preview
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
                                        Icons.Outlined.Image, null,
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
                            Text("从相册选择应用图标",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Category
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "选择应用所属分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { (key, label) ->
                            PremiumFilterChip(
                                selected = selectedCategory == key,
                                onClick = { selectedCategory = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Version
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
                            value = versionCode,
                            onValueChange = { versionCode = it.filter { c -> c.isDigit() } },
                            label = { Text("版本号") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Package name
                item {
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("包名 (可选)") },
                        placeholder = { Text("com.example.myapp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── Description and tags ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("描述和标签", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "详细描述应用功能，添加标签方便搜索",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("应用描述 * (支持 Markdown)") },
                        placeholder = { Text("描述应用的功能和用途...") },
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
                        placeholder = { Text("工具,效率,开源") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── Screenshots (multiple) ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("截图 * (至少一张)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "添加截图让用户了解应用界面",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Added screenshot previews
                if (screenshotUris.isNotEmpty() || screenshotUrls.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Already uploaded URLs
                            items(screenshotUrls.size) { index ->
                                Box(modifier = Modifier.size(85.dp, 150.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxSize(),
                                        shadowElevation = 2.dp
                                    ) {
                                        AsyncImage(
                                            model = screenshotUrls[index],
                                            contentDescription = "截图 ${index + 1}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            screenshotUrls = screenshotUrls.toMutableList().also { it.removeAt(index) }
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
                            // Newly picked local URIs (not yet uploaded)
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
                                            contentDescription = "新截图 ${index + 1}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
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

                // Add screenshot (from gallery)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "从相册添加截图",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "已添加 ${screenshotUrls.size + screenshotUris.size} 张截图",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }




                // ── Contact information ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("联系信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "方便用户联系开发者",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("邮箱") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Outlined.Email, null, modifier = Modifier.size(18.dp)) }
                        )
                        OutlinedTextField(
                            value = websiteUrl,
                            onValueChange = { websiteUrl = it },
                            label = { Text("网站") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Outlined.Language, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = privacyPolicyUrl,
                        onValueChange = { privacyPolicyUrl = it },
                        label = { Text("隐私政策 URL (可选)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.PrivacyTip, null, modifier = Modifier.size(20.dp)) }
                    )
                }

                // ── Activation code settings ──
                item {
                    EnhancedElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.VpnKey, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("激活码配置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Switch(checked = enableActivation, onCheckedChange = { enableActivation = it })
                            }

                            if (enableActivation) {
                                Text(
                                    "用户安装后需要输入激活码才能使用应用",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )

                                // Device binding
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("设备绑定", style = MaterialTheme.typography.bodyMedium)
                                        Text("每个激活码只能在一台设备使用", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                    Switch(checked = enableDeviceBinding, onCheckedChange = { enableDeviceBinding = it })
                                }

                                HorizontalDivider()

                                // Quick template generation
                                Text("快速生成", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "numeric6" to "数字码 ×5",
                                        "standard" to "标准码 ×5",
                                        "uuid" to "UUID ×5"
                                    ).forEach { (id, label) ->
                                        Surface(
                                            onClick = {
                                                selectedCodeTemplate = id
                                                val gen = when (id) {
                                                    "numeric6" -> (1..5).map { String.format("%06d", (100000..999999).random()) }
                                                    "standard" -> {
                                                        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                                                        (1..5).map { (1..3).joinToString("-") { (1..4).map { chars.random() }.joinToString("") } }
                                                    }
                                                    else -> (1..5).map { java.util.UUID.randomUUID().toString() }
                                                }
                                                activationCodes = activationCodes + gen
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selectedCodeTemplate == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                                        ) {
                                            Text(
                                                label,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedCodeTemplate == id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedCodeTemplate == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Custom input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customCodeInput,
                                        onValueChange = { customCodeInput = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("输入自定义激活码", fontSize = 13.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    )
                                    Button(
                                        onClick = {
                                            if (customCodeInput.isNotBlank()) {
                                                activationCodes = activationCodes + customCodeInput.trim()
                                                customCodeInput = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        enabled = customCodeInput.isNotBlank()
                                    ) {
                                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Added activation code list
                                if (activationCodes.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("已添加 ${activationCodes.size} 个激活码", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        TextButton(onClick = { activationCodes = emptyList(); selectedCodeTemplate = null }) {
                                            Text("清空", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        activationCodes.forEachIndexed { idx, code ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        code,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    IconButton(
                                                        onClick = { activationCodes = activationCodes.toMutableList().also { it.removeAt(idx) } },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Outlined.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            uploadStatus.ifBlank { "上传中..." },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        "${(uploadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(uploadProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    )
                                                ),
                                                RoundedCornerShape(2.5.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Linked team (optional) ──
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
                                Text(
                                    "可选",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            "关联团队后，团队和成员贡献信息将在应用详情及成员主页展示",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    // Team selector
                    item {
                        var teamDropdownExpanded by remember { mutableStateOf(false) }
                        val selectedTeam = myTeams.find { it.id == selectedTeamId }

                        ExposedDropdownMenuBox(
                            expanded = teamDropdownExpanded,
                            onExpandedChange = { teamDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedTeam?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(Strings.teamSelectTeam) },
                                placeholder = { Text("点击选择团队") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Outlined.Groups, null, modifier = Modifier.size(20.dp)) }
                            )
                            ExposedDropdownMenu(
                                expanded = teamDropdownExpanded,
                                onDismissRequest = { teamDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("不关联团队", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    leadingIcon = { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp)) },
                                    onClick = {
                                        selectedTeamId = null
                                        teamDropdownExpanded = false
                                    }
                                )
                                HorizontalDivider()
                                myTeams.forEach { team ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(team.name, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "${team.memberCount} 成员",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Groups, null, Modifier.size(18.dp)) },
                                        onClick = {
                                            selectedTeamId = team.id
                                            teamDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Contributor entries (when team selected)
                    if (selectedTeamId != null && contributorEntries.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "设置贡献者角色与贡献点",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        contributorEntries.forEachIndexed { index, entry ->
                            item(key = "contrib_${entry.userId}") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                    border = if (entry.role == "lead")
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    else null
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(32.dp),
                                                shape = CircleShape,
                                                color = if (entry.role == "lead")
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHighest
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Text(
                                                        (entry.displayName ?: entry.username).take(1).uppercase(),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (entry.role == "lead")
                                                            MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    entry.displayName ?: entry.username,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "@${entry.username}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilterChip(
                                                    selected = entry.role == "lead",
                                                    onClick = {
                                                        contributorEntries = contributorEntries.toMutableList().also {
                                                            it[index] = entry.copy(role = "lead")
                                                        }
                                                    },
                                                    label = { Text(Strings.teamLead, fontSize = 11.sp) },
                                                    modifier = Modifier.height(28.dp)
                                                )
                                                FilterChip(
                                                    selected = entry.role == "member",
                                                    onClick = {
                                                        contributorEntries = contributorEntries.toMutableList().also {
                                                            it[index] = entry.copy(role = "member")
                                                        }
                                                    },
                                                    label = { Text(Strings.teamMemberRole, fontSize = 11.sp) },
                                                    modifier = Modifier.height(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = entry.points.toString(),
                                                onValueChange = { value ->
                                                    val p = value.toIntOrNull() ?: 0
                                                    contributorEntries = contributorEntries.toMutableList().also {
                                                        it[index] = entry.copy(points = p)
                                                    }
                                                },
                                                label = { Text(Strings.teamContributionPoints, fontSize = 11.sp) },
                                                modifier = Modifier.width(100.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                            OutlinedTextField(
                                                value = entry.desc,
                                                onValueChange = { value ->
                                                    contributorEntries = contributorEntries.toMutableList().also {
                                                        it[index] = entry.copy(desc = value)
                                                    }
                                                },
                                                label = { Text(Strings.teamContributionDesc, fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                placeholder = { Text("如：UI设计、后端开发", fontSize = 12.sp) },
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Publish button ──
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        enabled = !isPublishing && !isBuilding,
                        onClick = {
                            if (selectedProject == null) {
                                scope.launch { snackbarHostState.showSnackbar("请先选择要发布的应用") }
                                return@Button
                            }
                            if (name.isBlank() || description.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.storeFillRequired) }
                                return@Button
                            }
                            if (screenshotUrls.isEmpty() && screenshotUris.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.storeAddScreenshot) }
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

                                // Step 2: Upload local screenshots
                                val allScreenshotUrls = screenshotUrls.toMutableList()
                                if (screenshotUris.isNotEmpty()) {
                                    val total = screenshotUris.size
                                    for ((idx, uri) in screenshotUris.withIndex()) {
                                        uploadStatus = "正在上传截图 ${idx + 1}/$total..."
                                        uploadProgress = (idx.toFloat()) / (total + 2)
                                        val scrFile = uriToTempFile(uri, "screenshot_$idx", "png")
                                        if (scrFile != null) {
                                            when (val r = apiClient.uploadAsset(scrFile, "image/png")) {
                                                is com.webtoapp.core.auth.AuthResult.Success -> allScreenshotUrls.add(r.data)
                                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                                    uploadStatus = "截图 ${idx + 1} 上传失败"
                                                }
                                            }
                                            scrFile.delete()
                                        }
                                    }
                                }

                                // Step 3: Upload APK from build output via asset upload
                                var apkUrlGithub: String? = null
                                if (selectedApkFile != null && selectedApkFile!!.exists()) {
                                    try {
                                        uploadStatus = "正在上传 APK..."
                                        when (val r = apiClient.uploadAsset(
                                            selectedApkFile!!,
                                            "application/vnd.android.package-archive"
                                        ) { progress -> uploadProgress = 0.5f + progress * 0.4f }) {
                                            is com.webtoapp.core.auth.AuthResult.Success -> apkUrlGithub = r.data
                                            is com.webtoapp.core.auth.AuthResult.Error -> uploadStatus = "APK 上传失败: ${r.message}"
                                        }
                                    } catch (e: Exception) {
                                        uploadStatus = "APK 上传失败: ${e.message}"
                                    }
                                } else {
                                    // Warn: no APK selected — app won't be downloadable
                                    scope.launch {
                                        snackbarHostState.showSnackbar("⚠️ 未选择 APK 文件，应用发布后将无法被下载安装")
                                    }
                                }

                                // Step 4: Publish app info to store
                                uploadStatus = "正在发布应用信息..."
                                uploadProgress = 0.95f
                                val result = apiClient.publishApp(
                                    name = name,
                                    description = description,
                                    category = selectedCategory,
                                    versionName = versionName,
                                    versionCode = versionCode.toIntOrNull() ?: 1,
                                    packageName = packageName.ifBlank { null },
                                    icon = finalIconUrl,
                                    tags = tags.ifBlank { null },
                                    screenshots = allScreenshotUrls,
                                    apkUrlGithub = apkUrlGithub,
                                    apkUrlGitee = null,
                                    contactEmail = contactEmail.ifBlank { null },
                                    websiteUrl = websiteUrl.ifBlank { null },
                                    privacyPolicyUrl = privacyPolicyUrl.ifBlank { null }
                                )
                                isPublishing = false
                                uploadProgress = 0f
                                uploadStatus = ""
                                when (result) {
                                    is com.webtoapp.core.auth.AuthResult.Success -> {
                                        // If team is selected, associate it now
                                        val publishedAppId = result.data.id
                                        if (selectedTeamId != null && contributorEntries.isNotEmpty()) {
                                            val hasLead = contributorEntries.any { it.role == "lead" }
                                            if (hasLead) {
                                                val contribs = contributorEntries.map { e ->
                                                    com.webtoapp.core.cloud.ContributorInput(
                                                        userId = e.userId,
                                                        contributorRole = e.role,
                                                        contributionPoints = e.points,
                                                        description = e.desc.ifBlank { null }
                                                    )
                                                }
                                                val teamResult = apiClient.associateModuleTeam(
                                                    moduleId = publishedAppId,
                                                    teamId = selectedTeamId!!,
                                                    contributors = contribs
                                                )
                                                when (teamResult) {
                                                    is com.webtoapp.core.auth.AuthResult.Success ->
                                                        snackbarHostState.showSnackbar("${Strings.storePublishSuccess} · 团队已关联")
                                                    is com.webtoapp.core.auth.AuthResult.Error ->
                                                        snackbarHostState.showSnackbar("${Strings.storePublishSuccess} · 团队关联失败: ${teamResult.message}")
                                                }
                                            } else {
                                                snackbarHostState.showSnackbar("${Strings.storePublishSuccess} · 团队关联需至少一位主负责人")
                                            }
                                        } else {
                                            snackbarHostState.showSnackbar(Strings.storePublishSuccess)
                                        }
                                        onPublished()
                                    }
                                    is com.webtoapp.core.auth.AuthResult.Error -> {
                                        snackbarHostState.showSnackbar("${Strings.storePublishFailed}: ${result.message}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("发布中...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Outlined.Publish, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.storePublishApp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Project picker dialog
    if (showProjectPicker) {
        AlertDialog(
            onDismissRequest = { showProjectPicker = false },
            title = {
                Column {
                    Text("选择要发布的应用", fontWeight = FontWeight.Bold)
                    Text(
                        "${allProjects.size} 个本地应用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (allProjects.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
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
                                        Icon(Icons.Outlined.Apps, null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                    }
                                }
                                Text("您还没有创建任何应用，请先创建应用",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                    items(allProjects.size) { index ->
                        val project = allProjects[index]
                        Surface(
                            onClick = {
                                selectedProject = project
                                // Auto-fill fields from project
                                name = project.name
                                val exportConfig = project.apkExportConfig
                                versionName = exportConfig?.customVersionName ?: "1.0.0"
                                versionCode = (exportConfig?.customVersionCode ?: 1).toString()
                                packageName = exportConfig?.customPackageName ?: project.packageName ?: ""
                                // Set icon from project icon path
                                if (project.iconPath != null) {
                                    iconUri = android.net.Uri.fromFile(java.io.File(project.iconPath!!))
                                }
                                // Find latest built APK for this project
                                val apkBuilder = com.webtoapp.core.apkbuilder.ApkBuilder(context)
                                val builtApks = apkBuilder.getBuiltApks()
                                val sanitizedName = project.name.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5._-]"), "_")
                                selectedApkFile = builtApks
                                    .filter { it.name.contains(sanitizedName, ignoreCase = true) }
                                    .maxByOrNull { it.lastModified() }
                                    ?: builtApks.maxByOrNull { it.lastModified() } // fallback: latest APK

                                showProjectPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedProject?.id == project.id)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth(),
                            border = if (selectedProject?.id == project.id)
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (project.iconPath != null) {
                                    AsyncImage(
                                        model = project.iconPath,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
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
                                            Icon(Icons.Outlined.Android, null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(project.name,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    Text(project.appType.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selectedProject?.id == project.id) {
                                    Icon(Icons.Filled.CheckCircle, null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectPicker = false }) {
                    Text(Strings.storeReviewCancel)
                }
            }
        )
    }
}
