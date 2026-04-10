package com.webtoapp.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.MultiWebConfig
import com.webtoapp.data.model.MultiWebSite
import com.webtoapp.ui.components.PremiumSwitch
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.RuntimeIconPickerCard
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.util.UrlMetadataFetcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 创建/编辑多站点聚合应用页面 — Multi-Site Hub
 *
 * 设计要点（对标 mockup）：
 * 1. 深紫渐变 Hero 卡片 — dramatic, full-bleed indigo→purple
 * 2. 水平 pill 模式选择器 — 独立排列，不包在卡片里
 * 3. 站点列表 — 每个站点独立玻璃态卡片，左侧彩色渐变条
 * 4. Quick-Add 栏 — 底部固定，暗色圆角输入 + "+" 按钮
 * 5. 配置区域折叠在底部 — 不占用主视觉
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMultiWebAppScreen(
    existingAppId: Long = 0L,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        multiWebConfig: MultiWebConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val scrollState = rememberScrollState()
    val isEdit = existingAppId > 0L

    // App 信息
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var landscapeMode by remember { mutableStateOf(false) }

    // 站点列表
    var sites by remember { mutableStateOf<List<MultiWebSite>>(emptyList()) }

    // 显示模式
    var displayMode by remember { mutableStateOf("TABS") }

    // Feed 模式刷新间隔
    var refreshInterval by remember { mutableStateOf(30) }

    // Dialog 状态
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var editingSite by remember { mutableStateOf<MultiWebSite?>(null) }
    var showBatchImport by remember { mutableStateOf(false) }
    var showSettingsSection by remember { mutableStateOf(false) }

    // Quick-Add URL
    var quickAddUrl by remember { mutableStateOf("") }

    // 用于 AddSiteDialog 自动填充
    var prefilledUrl by remember { mutableStateOf("") }
    var prefilledName by remember { mutableStateOf("") }
    var prefilledFavicon by remember { mutableStateOf("") }
    var prefilledColor by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // 编辑模式：加载已有数据
    LaunchedEffect(existingAppId) {
        if (existingAppId > 0L) {
            val existingApp = org.koin.java.KoinJavaComponent
                .get<com.webtoapp.data.repository.WebAppRepository>(
                    com.webtoapp.data.repository.WebAppRepository::class.java
                ).getWebApp(existingAppId)
            existingApp?.let { app ->
                appName = app.name
                app.iconPath?.let { appIcon = android.net.Uri.parse(it) }
                app.multiWebConfig?.let { config ->
                    sites = config.sites
                    displayMode = config.displayMode
                    refreshInterval = config.refreshInterval
                    landscapeMode = config.landscapeMode
                }
            }
        }
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }

    val canCreate = sites.isNotEmpty()

    // 品牌色系
    val brandIndigo = Color(0xFF6366F1)
    val brandViolet = Color(0xFF8B5CF6)
    val brandPurple = Color(0xFFA855F7)

    // 站点卡片渐变配色
    val siteCardColors = remember {
        listOf(
            Color(0xFF6366F1) to Color(0xFF818CF8), // Indigo
            Color(0xFF14B8A6) to Color(0xFF2DD4BF), // Teal
            Color(0xFFEC4899) to Color(0xFFF472B6), // Pink
            Color(0xFFF59E0B) to Color(0xFFFBBF24), // Amber
            Color(0xFF8B5CF6) to Color(0xFFA78BFA), // Violet
            Color(0xFF06B6D4) to Color(0xFF22D3EE), // Cyan
            Color(0xFFEF4444) to Color(0xFFF87171), // Red
            Color(0xFF10B981) to Color(0xFF34D399), // Emerald
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    // 设置按钮
                    IconButton(onClick = { showSettingsSection = !showSettingsSection }) {
                        Icon(
                            Icons.Outlined.Settings, null,
                            tint = if (showSettingsSection) brandIndigo
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 批量导入
                    IconButton(onClick = { showBatchImport = true }) {
                        Icon(Icons.Outlined.PlaylistAdd, null)
                    }
                    // 创建/保存
                    TextButton(
                        onClick = {
                            onCreated(
                                appName.ifBlank { "Multi-Site App" },
                                MultiWebConfig(
                                    sites = sites,
                                    displayMode = displayMode,
                                    refreshInterval = refreshInterval,
                                    showSiteIcons = true,
                                    landscapeMode = landscapeMode
                                ),
                                appIcon, "AURORA"
                            )
                        },
                        enabled = canCreate
                    ) {
                        Text(
                            if (isEdit) Strings.btnSave else Strings.btnCreate,
                            fontWeight = FontWeight.Bold,
                            color = if (canCreate) brandIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 可滚动主内容
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ═══════════════════════ 1. HERO CARD ═══════════════════════
                    // 深紫色渐变 hero — dramatic, matching mockup
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        brandIndigo,
                                        brandViolet,
                                        brandPurple
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Multi-Site Hub",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    Strings.multiWebHeroDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )
                                if (sites.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "${sites.size} sites",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            // 发光地球 icon
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🌐", fontSize = 32.sp)
                            }
                        }
                    }

                    // ═══════════════════════ 2. MODE SELECTOR ═══════════════════════
                    // 独立 pill 按钮 — 不包在卡片里，直接排列
                    val modes = listOf(
                        Triple("TABS", Strings.multiWebModeTabs, "📑"),
                        Triple("CARDS", Strings.multiWebModeCards, "🃏"),
                        Triple("FEED", Strings.multiWebModeFeed, "📰"),
                        Triple("DRAWER", Strings.multiWebModeDrawer, "☰")
                    )
                    val modeDescriptions = mapOf(
                        "TABS" to Strings.multiWebModeTabsDesc,
                        "CARDS" to Strings.multiWebModeCardsDesc,
                        "FEED" to Strings.multiWebModeFeedDesc,
                        "DRAWER" to Strings.multiWebModeDrawerDesc
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modes.forEach { (mode, title, icon) ->
                            val isSelected = displayMode == mode
                            val bgColor by animateColorAsState(
                                if (isSelected) brandIndigo else Color.Transparent,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "pillBg"
                            )
                            val borderAlpha by animateFloatAsState(
                                if (isSelected) 0f else 1f,
                                label = "borderAlpha"
                            )

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .then(
                                        if (!isSelected) Modifier.border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha),
                                            shape = RoundedCornerShape(50)
                                        ) else Modifier
                                    )
                                    .clickable { displayMode = mode },
                                shape = RoundedCornerShape(50),
                                color = bgColor,
                                shadowElevation = if (isSelected) 4.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(icon, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 当前模式描述（小字）
                    Text(
                        modeDescriptions[displayMode] ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // Feed 模式提示
                    AnimatedVisibility(
                        visible = displayMode == "FEED",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = brandIndigo.copy(alpha = 0.08f)
                        ) {
                            Text(
                                Strings.multiWebFeedTip,
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = brandIndigo,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // ═══════════════════════ 3. SITE LIST ═══════════════════════
                    // 标题行
                    if (sites.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                Strings.multiWebSiteList,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                Strings.multiWebSiteCount.replace("%d", sites.size.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 站点卡片 — 每个独立的玻璃态卡片，左侧彩色渐变条
                    if (sites.isEmpty()) {
                        // 空状态 — 更优雅
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    brandIndigo.copy(alpha = 0.1f),
                                                    brandPurple.copy(alpha = 0.05f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Language, null,
                                        modifier = Modifier.size(36.dp),
                                        tint = brandIndigo.copy(alpha = 0.4f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    Strings.multiWebNoSites,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    Strings.multiWebQuickAddHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sites.forEachIndexed { index, site ->
                                val (colorStart, colorEnd) = siteCardColors[index % siteCardColors.size]
                                SiteCard(
                                    site = site,
                                    accentColorStart = colorStart,
                                    accentColorEnd = colorEnd,
                                    showFeedConfig = displayMode == "FEED",
                                    onEdit = {
                                        editingSite = site
                                        prefilledUrl = ""
                                        prefilledName = ""
                                        prefilledFavicon = ""
                                        prefilledColor = ""
                                        showAddSiteDialog = true
                                    },
                                    onDelete = {
                                        sites = sites.toMutableList().also { it.removeAt(index) }
                                    },
                                    onToggleEnabled = { enabled ->
                                        sites = sites.toMutableList().also {
                                            it[index] = site.copy(enabled = enabled)
                                        }
                                    },
                                    onMoveUp = if (index > 0) {
                                        {
                                            sites = sites.toMutableList().also {
                                                val item = it.removeAt(index)
                                                it.add(index - 1, item)
                                            }
                                        }
                                    } else null,
                                    onMoveDown = if (index < sites.size - 1) {
                                        {
                                            sites = sites.toMutableList().also {
                                                val item = it.removeAt(index)
                                                it.add(index + 1, item)
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                    }

                    // ═══════════════════════ 4. SETTINGS (collapsible) ═══════════════════════
                    AnimatedVisibility(
                        visible = showSettingsSection,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // App name + icon
                            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Settings, null,
                                            tint = brandIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            Strings.njsBasicConfig,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    PremiumTextField(
                                        value = appName,
                                        onValueChange = { appName = it },
                                        label = { Text(Strings.labelAppName) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            Strings.njsLandscapeMode,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        PremiumSwitch(
                                            checked = landscapeMode,
                                            onCheckedChange = { landscapeMode = it }
                                        )
                                    }
                                }
                            }

                            // Icon picker
                            RuntimeIconPickerCard(
                                appIcon = appIcon,
                                onSelectIcon = { iconPickerLauncher.launch("image/*") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ═══════════════════════ 5. BOTTOM QUICK-ADD BAR ═══════════════════════
                // 固定底部 — 暗色圆角输入框 + "+" 按钮（matching mockup）
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // URL 输入框
                        OutlinedTextField(
                            value = quickAddUrl,
                            onValueChange = { quickAddUrl = it },
                            placeholder = {
                                Text(
                                    Strings.multiWebQuickAddHint,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Link, null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (quickAddUrl.isBlank()) {
                                    // 粘贴按钮
                                    IconButton(
                                        onClick = {
                                            val clipText = getClipboardText(context)
                                            if (clipText != null) quickAddUrl = clipText
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ContentPaste, null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { quickAddUrl = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Clear, null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isValidUrl(quickAddUrl)) {
                                        focusManager.clearFocus()
                                        handleQuickAdd(quickAddUrl.trim()) { url, name, favicon, color ->
                                            prefilledUrl = url
                                            prefilledName = name
                                            prefilledFavicon = favicon
                                            prefilledColor = color
                                            editingSite = null
                                            showAddSiteDialog = true
                                        }
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandIndigo,
                                cursorColor = brandIndigo
                            )
                        )

                        // "+" 添加按钮 — 发光 indigo
                        FilledIconButton(
                            onClick = {
                                if (quickAddUrl.isNotBlank() && isValidUrl(quickAddUrl)) {
                                    focusManager.clearFocus()
                                    prefilledUrl = quickAddUrl.trim()
                                    prefilledName = ""
                                    prefilledFavicon = ""
                                    prefilledColor = ""
                                    editingSite = null
                                    showAddSiteDialog = true
                                } else {
                                    // 空白时打开空白 dialog
                                    prefilledUrl = ""
                                    prefilledName = ""
                                    prefilledFavicon = ""
                                    prefilledColor = ""
                                    editingSite = null
                                    showAddSiteDialog = true
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = brandIndigo
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Add, null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════ DIALOGS ═══════════════════════
    if (showAddSiteDialog) {
        AddSiteDialog(
            editingSite = editingSite,
            showFeedFields = displayMode == "FEED",
            newSortIndex = sites.size,
            prefilledUrl = prefilledUrl,
            prefilledName = prefilledName,
            prefilledFavicon = prefilledFavicon,
            prefilledColor = prefilledColor,
            onDismiss = {
                showAddSiteDialog = false
                editingSite = null
                prefilledUrl = ""
                prefilledName = ""
                prefilledFavicon = ""
                prefilledColor = ""
            },
            onSave = { site ->
                if (editingSite != null) {
                    sites = sites.map { if (it.id == editingSite!!.id) site else it }
                } else {
                    sites = sites + site
                }
                showAddSiteDialog = false
                editingSite = null
                prefilledUrl = ""
                prefilledName = ""
                prefilledFavicon = ""
                prefilledColor = ""
                quickAddUrl = ""
            }
        )
    }

    if (showBatchImport) {
        BatchImportDialog(
            startSortIndex = sites.size,
            onDismiss = { showBatchImport = false },
            onImport = { importedSites ->
                sites = sites + importedSites
                showBatchImport = false
            }
        )
    }
}

// ══════════════════════════════════════════════════════
// SITE CARD — 独立玻璃态卡片，左侧彩色渐变条
// ══════════════════════════════════════════════════════

@Composable
private fun SiteCard(
    site: MultiWebSite,
    accentColorStart: Color,
    accentColorEnd: Color,
    showFeedConfig: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val disabledAlpha by animateFloatAsState(
        if (site.enabled) 1f else 0.5f,
        label = "disabledAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = disabledAlpha }
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // ★ 左侧彩色渐变条 — 视觉亮点
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColorStart, accentColorEnd)
                        )
                    )
            )

            // 拖拽手柄
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.Default.DragHandle, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 主内容
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favicon / emoji 图标
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColorStart.copy(alpha = 0.15f),
                                    accentColorEnd.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (site.faviconUrl.isNotBlank()) {
                        val painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current).apply {
                                data(site.faviconUrl)
                                crossfade(true)
                            }.build()
                        )
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else if (site.iconEmoji.isNotBlank()) {
                        Text(site.iconEmoji, fontSize = 22.sp)
                    } else {
                        Icon(
                            Icons.Outlined.Language, null,
                            tint = accentColorStart,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 文本信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        site.name.ifBlank { site.url },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        site.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Feed CSS selector
                    if (showFeedConfig && site.cssSelector.isNotBlank()) {
                        Text(
                            "CSS: ${site.cssSelector}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = accentColorStart.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 分类标签
                    if (site.category.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColorStart.copy(alpha = 0.1f)
                        ) {
                            Text(
                                site.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = accentColorStart
                            )
                        }
                    }
                }
            }

            // 3-dot menu
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.multiWebEditSite) },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (site.enabled) Strings.multiWebDisableSite else Strings.multiWebEnableSite)
                        },
                        onClick = { showMenu = false; onToggleEnabled(!site.enabled) },
                        leadingIcon = {
                            Icon(
                                if (site.enabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    if (onMoveUp != null || onMoveDown != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        DropdownMenuItem(
                            text = { Text(Strings.multiWebMoveUp) },
                            onClick = { showMenu = false; onMoveUp?.invoke() },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(18.dp)) },
                            enabled = onMoveUp != null
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.multiWebMoveDown) },
                            onClick = { showMenu = false; onMoveDown?.invoke() },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(18.dp)) },
                            enabled = onMoveDown != null
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    DropdownMenuItem(
                        text = { Text(Strings.multiWebDeleteSite, color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// ADD / EDIT SITE DIALOG
// ══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSiteDialog(
    editingSite: MultiWebSite?,
    showFeedFields: Boolean,
    newSortIndex: Int = 0,
    prefilledUrl: String = "",
    prefilledName: String = "",
    prefilledFavicon: String = "",
    prefilledColor: String = "",
    onDismiss: () -> Unit,
    onSave: (MultiWebSite) -> Unit
) {
    var name by remember { mutableStateOf(editingSite?.name ?: prefilledName) }
    var url by remember { mutableStateOf(editingSite?.url ?: prefilledUrl) }
    var emoji by remember { mutableStateOf(editingSite?.iconEmoji ?: "") }
    var category by remember { mutableStateOf(editingSite?.category ?: "") }
    var cssSelector by remember { mutableStateOf(editingSite?.cssSelector ?: "") }
    var linkSelector by remember { mutableStateOf(editingSite?.linkSelector ?: "") }
    var faviconUrl by remember { mutableStateOf(editingSite?.faviconUrl ?: prefilledFavicon) }
    var themeColor by remember { mutableStateOf(editingSite?.themeColor ?: prefilledColor) }

    var isFetchingMetadata by remember { mutableStateOf(false) }
    var hasAutoFilled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val brandColor = Color(0xFF6366F1)
    val isValid = url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))

    // Auto-fetch when URL is valid (debounced)
    LaunchedEffect(url) {
        if (editingSite != null) return@LaunchedEffect
        if (!isValid || hasAutoFilled) return@LaunchedEffect
        if (prefilledName.isNotBlank()) { hasAutoFilled = true; return@LaunchedEffect }

        delay(800)
        if (!isValid || hasAutoFilled) return@LaunchedEffect

        isFetchingMetadata = true
        val metadata = UrlMetadataFetcher.fetch(url)
        isFetchingMetadata = false

        if (metadata.title.isNotBlank()) { name = metadata.title; hasAutoFilled = true }
        if (metadata.faviconUrl.isNotBlank()) faviconUrl = metadata.faviconUrl
        if (metadata.themeColor.isNotBlank()) themeColor = metadata.themeColor
    }

    LaunchedEffect(editingSite) { hasAutoFilled = editingSite != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (editingSite != null) Icons.Outlined.Edit else Icons.Outlined.AddCircleOutline,
                null, tint = brandColor, modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                if (editingSite != null) Strings.multiWebEditSite else Strings.multiWebAddSite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // URL with auto-fetch indicator
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; hasAutoFilled = false },
                    label = { Text(Strings.multiWebSiteUrl) },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = url.isNotBlank() && !isValid,
                    trailingIcon = {
                        when {
                            isFetchingMetadata -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                            )
                            isValid && hasAutoFilled && name.isNotBlank() -> Icon(
                                Icons.Outlined.CheckCircle, null,
                                modifier = Modifier.size(20.dp), tint = Color(0xFF4CAF50)
                            )
                            isValid && !hasAutoFilled -> IconButton(
                                onClick = {
                                    scope.launch {
                                        isFetchingMetadata = true
                                        val m = UrlMetadataFetcher.fetch(url)
                                        isFetchingMetadata = false
                                        if (m.title.isNotBlank()) { name = m.title; hasAutoFilled = true }
                                        if (m.faviconUrl.isNotBlank()) faviconUrl = m.faviconUrl
                                        if (m.themeColor.isNotBlank()) themeColor = m.themeColor
                                    }
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(18.dp), tint = brandColor)
                            }
                        }
                    }
                )

                // Fetching hint
                AnimatedVisibility(visible = isFetchingMetadata, enter = fadeIn(), exit = fadeOut()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CloudDownload, null,
                            modifier = Modifier.size(14.dp),
                            tint = brandColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            Strings.multiWebFetchingTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = brandColor.copy(alpha = 0.6f)
                        )
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.multiWebSiteName) },
                    placeholder = { Text("My Site") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Emoji + Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        label = { Text("Emoji") },
                        placeholder = { Text("🌐") },
                        modifier = Modifier.weight(0.4f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(Strings.multiWebSiteCategory) },
                        placeholder = { Text("News") },
                        modifier = Modifier.weight(0.6f),
                        singleLine = true
                    )
                }

                // Feed mode fields
                if (showFeedFields) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Feed Extraction",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = brandColor
                    )
                    OutlinedTextField(
                        value = cssSelector,
                        onValueChange = { cssSelector = it },
                        label = { Text(Strings.multiWebCssSelector) },
                        placeholder = { Text(Strings.multiWebCssSelectorHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        MultiWebSite(
                            id = editingSite?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            url = url.trim(),
                            iconEmoji = emoji.trim(),
                            faviconUrl = faviconUrl.trim(),
                            themeColor = themeColor.trim(),
                            category = category.trim(),
                            cssSelector = cssSelector.trim(),
                            linkSelector = linkSelector.trim(),
                            enabled = editingSite?.enabled ?: true,
                            sortIndex = editingSite?.sortIndex ?: newSortIndex
                        )
                    )
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = brandColor)
            ) { Text(Strings.btnSave) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}

// ══════════════════════════════════════════════════════
// BATCH IMPORT DIALOG
// ══════════════════════════════════════════════════════

@Composable
private fun BatchImportDialog(
    startSortIndex: Int = 0,
    onDismiss: () -> Unit,
    onImport: (List<MultiWebSite>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var previewSites by remember { mutableStateOf<List<MultiWebSite>>(emptyList()) }
    var showPreview by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val brandColor = Color(0xFF6366F1)

    fun parseTextToSites(input: String): List<MultiWebSite> {
        return input.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                // 支持格式: URL、名称|URL、emoji|名称|URL
                val parts = line.split("|").map { it.trim() }
                val url: String
                val name: String
                val emoji: String

                when {
                    parts.size >= 3 && isValidUrl(parts.last()) -> {
                        emoji = parts[0]
                        name = parts[1]
                        url = parts.last()
                    }
                    parts.size == 2 && isValidUrl(parts[1]) -> {
                        emoji = ""
                        name = parts[0]
                        url = parts[1]
                    }
                    parts.size == 1 && isValidUrl(parts[0]) -> {
                        emoji = ""
                        url = parts[0]
                        name = try {
                            java.net.URI(url).host?.removePrefix("www.")
                                ?.substringBefore(".")
                                ?.replaceFirstChar { it.uppercase() }
                                ?: url
                        } catch (_: Exception) { url }
                    }
                    else -> return@mapNotNull null
                }

                MultiWebSite(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    url = url,
                    iconEmoji = emoji,
                    enabled = true,
                    sortIndex = startSortIndex
                )
            }
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Outlined.PlaylistAdd, null, tint = brandColor, modifier = Modifier.size(32.dp))
        },
        title = { Text(Strings.multiWebBatchImport, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!showPreview) {
                    Text(
                        Strings.multiWebBatchHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = { Text("https://example.com\nNews|https://news.site.com") },
                        maxLines = 10
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            val clip = getClipboardText(context)
                            if (clip != null) text = clip
                        }) {
                            Icon(Icons.Outlined.ContentPaste, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.multiWebPaste)
                        }
                    }
                } else {
                    Text(
                        String.format(Strings.multiWebImportCount, previewSites.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = brandColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        previewSites.forEach { site ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(site.iconEmoji.ifBlank { "🌐" }, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            site.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            site.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { showPreview = false; previewSites = emptyList() }) {
                        Text(Strings.multiWebEditList)
                    }
                }
            }
        },
        confirmButton = {
            if (!showPreview) {
                Button(
                    onClick = {
                        previewSites = parseTextToSites(text)
                        showPreview = true
                    },
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) { Text(Strings.multiWebPreview) }
            } else {
                Button(
                    onClick = { onImport(previewSites) },
                    enabled = previewSites.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) { Text(String.format(Strings.multiWebImportSites, previewSites.size)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}

// ══════════════════════════════════════════════════════
// Helper Functions
// ══════════════════════════════════════════════════════

private fun isValidUrl(url: String): Boolean {
    return url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
}

private fun getClipboardText(context: Context): String? {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).text?.toString()?.trim()
        } else null
    } catch (_: Exception) { null }
}

private fun handleQuickAdd(
    url: String, 
    onReady: (url: String, name: String, favicon: String, color: String) -> Unit
) {
    onReady(url, "", "", "")
}
