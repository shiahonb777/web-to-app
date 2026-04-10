package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumFilterChip

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.extension.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.QrCodeShareDialog
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.webtoapp.R

/**
 * 扩展模块管理页面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExtensionModuleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,  // null 表示新建
    onNavigateToAiDeveloper: () -> Unit = {},  // AI 开发器入口

) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    
    val modules by extensionManager.modules.collectAsStateWithLifecycle()
    val builtInModules by extensionManager.builtInModules.collectAsStateWithLifecycle()
    
    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    
    // 油猴脚本/Chrome 扩展导入相关状态
    val extensionFileManager = remember { ExtensionFileManager(context) }
    var showUserScriptPreview by remember { mutableStateOf<UserScriptParser.ParseResult?>(null) }
    var showChromeExtPreview by remember { mutableStateOf<ChromeExtensionParser.ParseResult?>(null) }
    var pendingChromeExtDir by remember { mutableStateOf<java.io.File?>(null) }
    
    // File选择器 (.wtamod)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val result = extensionManager.importModule(stream)
                        result.onSuccess { module ->
                            Toast.makeText(context, context.getString(R.string.msg_import_success, module.name), Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(context, context.getString(R.string.msg_import_failed, e.message ?: context.getString(R.string.unknown_error)), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.msg_import_failed, e.message ?: context.getString(R.string.unknown_error)), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 油猴脚本文件选择器 (.user.js / .js)
    val userScriptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = extensionFileManager.importUserScript(it)
                when (result) {
                    is ExtensionFileManager.ImportResult.UserScript -> {
                        showUserScriptPreview = result.parseResult
                    }
                    is ExtensionFileManager.ImportResult.Error -> {
                        Toast.makeText(context, context.getString(R.string.msg_import_failed, result.message), Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
    
    // Chrome 扩展文件选择器 (.crx / .zip)
    val chromeExtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = extensionFileManager.importChromeExtension(it)
                when (result) {
                    is ExtensionFileManager.ImportResult.ChromeExtension -> {
                        showChromeExtPreview = result.parseResult
                        pendingChromeExtDir = result.extractedDir
                    }
                    is ExtensionFileManager.ImportResult.Error -> {
                        Toast.makeText(context, context.getString(R.string.msg_import_failed, result.message), Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
    
    // 二维码图片选择器
    val qrCodeImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val qrContent = QrCodeUtils.decodeQrCode(bitmap)
                            if (qrContent != null) {
                                extensionManager.importFromShareCode(qrContent).onSuccess { module ->
                                    Toast.makeText(context, context.getString(R.string.msg_import_success, module.name), Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, context.getString(R.string.msg_import_failed, e.message ?: "Unknown error"), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, Strings.qrCodeNotFound, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, Strings.imageLoadFailed, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.msg_import_failed, e.message ?: "Unknown error"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Separate modules by source type
    val allModules = builtInModules + modules
    val extensionModules = allModules.filter { it.sourceType == ModuleSourceType.CUSTOM }
    val userScriptModules = allModules.filter { it.sourceType != ModuleSourceType.CUSTOM }
    
    // Filter模块 - 直接计算而非使用 remember，确保 StateFlow 更新时 UI 正确响应
    val filteredModules = extensionModules.filter { module ->
        val matchesCategory = selectedCategory == null || module.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
            module.name.contains(searchQuery, ignoreCase = true) ||
            module.description.contains(searchQuery, ignoreCase = true) ||
            module.tags.any { it.contains(searchQuery, ignoreCase = true) }
        matchesCategory && matchesSearch
    }
    
    val filteredUserScripts = userScriptModules.filter { module ->
        searchQuery.isBlank() ||
            module.name.contains(searchQuery, ignoreCase = true) ||
            module.description.contains(searchQuery, ignoreCase = true)
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.extensionModule) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = Strings.btnImport)
                    }
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = Strings.add)
                    }
                }
            )
        },
        floatingActionButton = {
            var fabExpanded by remember { mutableStateOf(false) }
            
            Column(horizontalAlignment = Alignment.End) {
                // AI 开发按钮
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { it },
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) + slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessHigh)) { it }
                ) {
                    Surface(
                        onClick = {
                            fabExpanded = false
                            onNavigateToAiDeveloper()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                Strings.aiDevelop,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // 手动创建按钮
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { it },
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) + slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessHigh)) { it }
                ) {
                    Surface(
                        onClick = {
                            fabExpanded = false
                            onNavigateToEditor(null)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Text(
                                Strings.manualCreate,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // 主 FAB — Apple-style spring rotation
                val fabRotation by animateFloatAsState(
                    targetValue = if (fabExpanded) 135f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "fabRotation"
                )
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 2.dp,
                        hoveredElevation = 3.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = Strings.createModule,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = fabRotation
                        }
                    )
                }
            }
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
            // Search栏 - MD3 SearchBar 风格
            PremiumTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(Strings.searchModules) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = Strings.clear)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )
            
            // Tab 分页: 扩展模块 / 油猴脚本
            val pagerState = rememberPagerState(pageCount = { 2 })
            val tabTitles = listOf(
                Strings.extensionModulesTab,
                Strings.userScriptsTab
            )
            
            // Apple-style Segmented Control
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        val count = if (index == 0) extensionModules.size else userScriptModules.size
                        
                        Surface(
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.surface
                                else Color.Transparent,
                            shadowElevation = if (isSelected) 1.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            "$count",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    // Tab 0: 扩展模块
                    0 -> ExtensionModulesTabContent(
                        filteredModules = filteredModules,
                        extensionManager = extensionManager,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        onCategoryChange = { selectedCategory = it },
                        onNavigateToEditor = onNavigateToEditor,
                        onNavigateToAiDeveloper = onNavigateToAiDeveloper,
                        onClearSearch = { searchQuery = "" }
                    )
                    // Tab 1: 油猴脚本
                    1 -> UserScriptsTabContent(
                        filteredUserScripts = filteredUserScripts,
                        extensionManager = extensionManager,
                        searchQuery = searchQuery,
                        onImportUserScript = {
                            userScriptPickerLauncher.launch("*/*")
                        },
                        onClearSearch = { searchQuery = "" }
                    )
                }
            }
        }
    }
    
    // Import对话框
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Strings.importModule) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 油猴脚本导入
                    Surface(
                        onClick = {
                            showImportDialog = false
                            userScriptPickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFF7DF1E).copy(alpha = 0.15f),
                                                Color(0xFFF7DF1E).copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFD4A017)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(Strings.importUserScript, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.importUserScriptHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                    
                    // Chrome 扩展导入
                    Surface(
                        onClick = {
                            showImportDialog = false
                            chromeExtPickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF4285F4).copy(alpha = 0.15f),
                                                Color(0xFF4285F4).copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Extension,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF4285F4)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(Strings.importChromeExtension, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.importChromeExtensionHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                    
                    // Hairline separator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                    
                    // .wtamod 文件导入
                    Surface(
                        onClick = {
                            showImportDialog = false
                            filePickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
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
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(Strings.importFromFile, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.selectWtamodFile,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                    
                    // 二维码导入
                    Surface(
                        onClick = {
                            showImportDialog = false
                            qrCodeImagePickerLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(Strings.importFromQrImage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.selectQrImageHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                    
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
    
    // 油猴脚本预览安装对话框
    showUserScriptPreview?.let { parseResult ->
        AlertDialog(
            onDismissRequest = { showUserScriptPreview = null },
            title = { Text(Strings.installUserScript) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 脚本信息
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFF7DF1E).copy(alpha = 0.15f),
                                            Color(0xFFF7DF1E).copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color(0xFFD4A017)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                parseResult.module.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "v${parseResult.module.version.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (parseResult.module.description.isNotBlank()) {
                        Text(
                            parseResult.module.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    parseResult.module.author?.let { author ->
                        Text(
                        "${Strings.scriptAuthor}: ${author.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // URL 匹配规则
                    if (parseResult.module.urlMatches.isNotEmpty()) {
                        Text(
                        "${Strings.matchingSites}: ${parseResult.module.urlMatches.size} ${Strings.matchRules}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // GM API 权限
                    if (parseResult.module.gmGrants.isNotEmpty()) {
                        Text(
                            "${Strings.requiredApis}: ${parseResult.module.gmGrants.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    // 警告
                    parseResult.warnings.forEach { warning ->
                        Text(
                            "⚠️ $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                PremiumButton(onClick = {
                    scope.launch {
                        extensionManager.addModule(parseResult.module).onSuccess { module ->
                            Toast.makeText(context, "${Strings.msgImportSuccess}: ${module.name}", Toast.LENGTH_SHORT).show()
                            // Pre-load @require and @resource in background
                            val fileManager = com.webtoapp.core.extension.ExtensionFileManager(context)
                            if (module.requireUrls.isNotEmpty()) {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    fileManager.preloadRequires(module.requireUrls)
                                }
                            }
                            if (module.resources.isNotEmpty()) {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    fileManager.preloadResources(module.resources)
                                }
                            }
                        }.onFailure { e ->
                            Toast.makeText(context, context.getString(R.string.msg_import_failed, e.message ?: "Unknown error"), Toast.LENGTH_SHORT).show()
                        }
                        showUserScriptPreview = null
                    }
                }) {
                    Text(Strings.install)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserScriptPreview = null }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
    
    // Chrome 扩展预览安装对话框
    showChromeExtPreview?.let { parseResult ->
        AlertDialog(
            onDismissRequest = {
                showChromeExtPreview = null
                pendingChromeExtDir = null
            },
            title = { Text(Strings.installChromeExtension) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF4285F4).copy(alpha = 0.15f),
                                            Color(0xFF4285F4).copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color(0xFF4285F4)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                parseResult.extensionName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "v${parseResult.extensionVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (parseResult.extensionDescription.isNotBlank()) {
                        Text(
                            parseResult.extensionDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Text(
                        "${Strings.contentScripts}: ${parseResult.modules.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Supported permissions
                    if (parseResult.supportedPermissions.isNotEmpty()) {
                        Text(
                            "${Strings.requiredApis}: ${parseResult.supportedPermissions.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    // Unsupported permissions
                    if (parseResult.unsupportedPermissions.isNotEmpty()) {
                        Text(
                            "⚠️ ${Strings.unsupportedApis}: ${parseResult.unsupportedPermissions.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    // 警告
                    parseResult.warnings.filter { !it.startsWith("Unsupported permissions") }.forEach { warning ->
                        Text(
                            "⚠️ $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                PremiumButton(onClick = {
                    scope.launch {
                        var successCount = 0
                        parseResult.modules.forEach { module ->
                            extensionManager.addModule(module).onSuccess { successCount++ }
                        }
                        if (successCount > 0) {
                            Toast.makeText(
                                context,
                                "${context.getString(R.string.msg_import_success, parseResult.extensionName)} ($successCount ${Strings.contentScripts})",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.msg_import_failed, context.getString(R.string.unknown_error)), Toast.LENGTH_SHORT).show()
                        }
                        showChromeExtPreview = null
                        pendingChromeExtDir = null
                    }
                }) {
                    Text(Strings.install)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChromeExtPreview = null
                    pendingChromeExtDir = null
                }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
    
        }
}


/**
 * 模块卡片组件 - MD3 原生风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(
    module: ExtensionModule,
    extensionManager: ExtensionManager,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPublish: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showMenu by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    // SAF 文件创建器
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                extensionManager.exportModuleToUri(module.id, it).onSuccess {
                    Toast.makeText(context, Strings.exportSuccess, Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Storage权限请求器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission已授予，执行导出
            scope.launch {
                extensionManager.exportModuleToDownloads(module.id).onSuccess { path ->
                    Toast.makeText(context, "${Strings.exportSuccess}: $path", Toast.LENGTH_LONG).show()
                }.onFailure { e ->
                    Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, Strings.storagePermissionRequiredForExport, Toast.LENGTH_SHORT).show()
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部：图标、名称、徽章、菜单
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon — Apple-style gradient tinted
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(13.dp))
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
                    com.webtoapp.ui.components.ModuleIcon(
                        iconId = module.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    // Name行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            module.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(weight = 1f, fill = false)
                        )
                        
                        // Built-in标签 — Apple-style pill
                        if (module.builtIn) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    Strings.builtIn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 分类和版本 — with dot separator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            module.category.getDisplayName(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            "v${module.version.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // 菜单按钮
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = Strings.more)
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.btnEdit) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.export) },
                            onClick = { showMenu = false; showExportDialog = true },
                            leadingIcon = { Icon(Icons.Outlined.FileUpload, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.share) },
                            onClick = { showMenu = false; showQrCodeDialog = true },
                            leadingIcon = { Icon(Icons.Outlined.Share, null) }
                        )
                        // 发布到模块市场
                        onPublish?.let { publish ->
                            DropdownMenuItem(
                                text = { Text("发布到市场") },
                                onClick = { showMenu = false; publish() },
                                leadingIcon = { Icon(Icons.Outlined.CloudUpload, null) }
                            )
                        }
                        if (!module.builtIn) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                            DropdownMenuItem(
                                text = { Text(Strings.btnDelete, color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
            
            // Description
            if (module.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
            
            // 标签 — Apple-style inline pills
            if (module.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(module.tags.take(5)) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // 底部信息
            val hasUrlMatches = module.urlMatches.isNotEmpty()
            val dangerousPermissions = module.permissions.filter { it.dangerous }
            
            if (hasUrlMatches || dangerousPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (hasUrlMatches) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Language,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                Strings.onlyEffectiveOnMatchingSites.format(module.urlMatches.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (dangerousPermissions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Shield,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                Strings.requiresSensitivePermissions.format(
                                    dangerousPermissions.joinToString { it.displayName }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showQrCodeDialog) {
        QrCodeShareDialog(
            module = module,
            shareCode = module.toShareCode(),
            onDismiss = { showQrCodeDialog = false }
        )
    }
    
    // Export选项对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(Strings.exportModule) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Save到 Downloads
                    Surface(
                        onClick = {
                            showExportDialog = false
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                    scope.launch {
                                        extensionManager.exportModuleToDownloads(module.id).onSuccess { path ->
                                            Toast.makeText(context, "${Strings.exportSuccess}\n$path", Toast.LENGTH_LONG).show()
                                        }.onFailure { e ->
                                            Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    permissionLauncher.launch(permission)
                                }
                            } else {
                                scope.launch {
                                    extensionManager.exportModuleToDownloads(module.id).onSuccess { path ->
                                        Toast.makeText(context, "${Strings.exportSuccess}\n$path", Toast.LENGTH_LONG).show()
                                    }.onFailure { e ->
                                        Toast.makeText(context, "${Strings.exportFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
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
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(Strings.exportToDownloads, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.exportToDownloadsHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                    
                    // Custom存储路径
                    Surface(
                        onClick = {
                            showExportDialog = false
                            val fileName = extensionManager.getModuleExportFileName(module.id) ?: "module.wtamod"
                            createFileLauncher.launch(fileName)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(Strings.exportToCustomPath, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.exportToCustomPathHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }
}

/**
 * Statistics项 - MD3 简洁风格
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        primary.copy(alpha = 0.08f),
                        primary.copy(alpha = 0.03f)
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Tab 0: 扩展模块列表（CUSTOM + CHROME_EXTENSION）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionModulesTabContent(
    filteredModules: List<ExtensionModule>,
    extensionManager: ExtensionManager,
    selectedCategory: ModuleCategory?,
    searchQuery: String,
    onCategoryChange: (ModuleCategory?) -> Unit,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToAiDeveloper: () -> Unit,
    onClearSearch: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 分类筛选器
        LazyRow(
            modifier = Modifier.padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PremiumFilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text(Strings.all) },
                    leadingIcon = if (selectedCategory == null) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
            items(ModuleCategory.values().toList()) { category ->
                PremiumFilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(if (selectedCategory == category) null else category) },
                    label = { Text(category.getDisplayName()) },
                    leadingIcon = if (selectedCategory == category) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
        
        // 统计信息
        val stats = extensionManager.getStatistics()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Outlined.Extension,
                value = stats.totalCount.toString(),
                label = Strings.totalModulesLabel
            )
            StatItem(
                icon = Icons.Outlined.Verified,
                value = stats.builtInCount.toString(),
                label = Strings.builtInLabel
            )
            StatItem(
                icon = Icons.Outlined.Build,
                value = stats.userCount.toString(),
                label = Strings.customLabel
            )
        }
        
        // Refined separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Module列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredModules, key = { it.id }) { module ->
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                ModuleCard(
                    module = module,
                    extensionManager = extensionManager,
                    onEdit = { onNavigateToEditor(module.id) },
                    onDelete = {
                        scope.launch {
                            extensionManager.deleteModule(module.id)
                            Toast.makeText(context, Strings.deleted, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPublish = if (!module.builtIn) {
                        {
                            val cloudVm: com.webtoapp.ui.viewmodel.CloudViewModel = org.koin.java.KoinJavaComponent.get(com.webtoapp.ui.viewmodel.CloudViewModel::class.java)
                            cloudVm.publishModule(module)
                        }
                    } else null
                )
            }
            
            // 空状态
            if (filteredModules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Apple-style gradient circle icon
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (searchQuery.isNotBlank()) Icons.Outlined.Search else Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    if (searchQuery.isNotBlank()) Strings.noModulesFound else Strings.noModulesYet,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (searchQuery.isNotBlank()) 
                                        Strings.tryDifferentSearch 
                                    else 
                                        Strings.createModuleHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (searchQuery.isBlank()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { onNavigateToAiDeveloper() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(Strings.aiDevelop, style = MaterialTheme.typography.labelMedium)
                                    }
                                    
                                    PremiumButton(
                                        onClick = { onNavigateToEditor(null) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(Strings.createFirstModule, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            } else {
                                TextButton(onClick = onClearSearch) {
                                    Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Strings.clearSearch, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/**
 * Tab 1: 油猴脚本列表 (USERSCRIPT)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserScriptsTabContent(
    filteredUserScripts: List<ExtensionModule>,
    extensionManager: ExtensionManager,
    searchQuery: String,
    onImportUserScript: () -> Unit,
    onClearSearch: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 脚本卡片列表
        items(filteredUserScripts, key = { it.id }) { module ->
            UserScriptCard(
                module = module,
                onDelete = {
                    scope.launch {
                        extensionManager.deleteModule(module.id)
                        Toast.makeText(context, Strings.deleted, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        
        // 空状态
        if (filteredUserScripts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFFF7DF1E).copy(alpha = 0.10f),
                                            Color(0xFFF7DF1E).copy(alpha = 0.02f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                if (searchQuery.isNotBlank()) Strings.noMatchingScripts else Strings.noUserScripts,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (searchQuery.isNotBlank()) Strings.tryDifferentSearch else Strings.noUserScriptsHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (searchQuery.isBlank()) {
                            PremiumButton(
                                onClick = onImportUserScript,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.importUserScript, style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            TextButton(onClick = onClearSearch) {
                                Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Strings.clearSearch, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * 油猴脚本/Chrome扩展卡片 — 与 ModuleCard 统一设计风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserScriptCard(
    module: ExtensionModule,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    
    val isChromeExt = module.sourceType == ModuleSourceType.CHROME_EXTENSION
    val typeIcon = if (isChromeExt) "🧩" else "🐵"
    val typeLabel = if (isChromeExt) "Chrome" else "UserScript"
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 头部：图标、名称、徽章、菜单
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 图标 — Apple-style gradient tinted
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(13.dp))
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
                    com.webtoapp.ui.components.ModuleIcon(
                        iconId = module.icon.ifBlank { typeIcon },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    // 名称行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            module.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(weight = 1f, fill = false)
                        )
                        
                        // 类型标签 — Apple-style pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isChromeExt) Color(0xFF4285F4).copy(alpha = 0.08f)
                                    else Color(0xFFF7DF1E).copy(alpha = 0.10f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isChromeExt) Color(0xFF4285F4) else Color(0xFFD4A017),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 版本和作者 — with dot separator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "v${module.version.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        module.author?.let { author ->
                            Text(
                                "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                author.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                // 菜单按钮
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = Strings.more)
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.viewSourceCode) },
                            onClick = { showMenu = false; showSourceDialog = true },
                            leadingIcon = { Icon(Icons.Outlined.Code, null) }
                        )
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                        DropdownMenuItem(
                            text = { Text(Strings.btnDelete, color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
            
            // 描述
            if (module.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
            
            // 底部信息：URL匹配 + GM权限
            val hasUrlMatches = module.urlMatches.isNotEmpty()
            val hasGmGrants = module.gmGrants.isNotEmpty()
            
            if (hasUrlMatches || hasGmGrants) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (hasUrlMatches) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Language,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                Strings.onlyEffectiveOnMatchingSites.format(module.urlMatches.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (hasGmGrants) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Api,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "${module.gmGrants.size} APIs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 源码/项目浏览器对话框
    if (showSourceDialog) {
        ExtensionSourceBrowserDialog(
            module = module,
            onDismiss = { showSourceDialog = false }
        )
    }
}

/**
 * 文件树节点
 */
private data class FileNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val children: MutableList<FileNode> = mutableListOf()
)

/**
 * 扩展源码/项目浏览器对话框
 * 
 * - Chrome 扩展：显示完整项目目录树，点击文件查看内容
 * - 油猴脚本：显示脚本代码
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionSourceBrowserDialog(
    module: ExtensionModule,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isChromeExt = module.sourceType == ModuleSourceType.CHROME_EXTENSION && module.chromeExtId.isNotEmpty()
    
    // 当前查看的文件路径（null = 目录树视图）
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedFileContent by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }
    
    // 构建文件树
    val fileTree = remember(module.id) {
        if (isChromeExt) {
            buildExtensionFileTree(context, module)
        } else {
            null
        }
    }
    
    // 展开状态
    val expandedDirs = remember { mutableStateMapOf<String, Boolean>() }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // 顶部栏
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                if (selectedFilePath != null) selectedFileName else module.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (selectedFilePath != null) {
                                Text(
                                    selectedFilePath ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (selectedFilePath != null) {
                                selectedFilePath = null
                            } else {
                                onDismiss()
                            }
                        }) {
                            Icon(
                                if (selectedFilePath != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                                contentDescription = null
                            )
                        }
                    }
                )
                
                if (selectedFilePath != null) {
                    // 文件内容视图
                    FileContentView(
                        content = selectedFileContent,
                        fileName = selectedFileName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isChromeExt && fileTree != null) {
                    // 项目目录树视图
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        fileTree.children.sortedWith(compareBy({ !it.isDirectory }, { it.name })).forEach { node ->
                            fileTreeItems(
                                node = node,
                                depth = 0,
                                expandedDirs = expandedDirs,
                                onFileClick = { path, name ->
                                    val content = readExtensionFile(context, module, path)
                                    selectedFileContent = content ?: Strings.cannotReadFile
                                    selectedFileName = name
                                    selectedFilePath = path
                                }
                            )
                        }
                    }
                } else {
                    // 纯脚本代码视图
                    FileContentView(
                        content = module.code,
                        fileName = if (module.sourceType == ModuleSourceType.CHROME_EXTENSION) "content.js" else "${module.name}.user.js",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * 递归添加文件树节点到 LazyColumn
 */
private fun LazyListScope.fileTreeItems(
    node: FileNode,
    depth: Int,
    expandedDirs: MutableMap<String, Boolean>,
    onFileClick: (path: String, name: String) -> Unit
) {
    val isExpanded = expandedDirs[node.relativePath] ?: (depth == 0)
    
    item(key = node.relativePath) {
        FileTreeRow(
            node = node,
            depth = depth,
            isExpanded = isExpanded,
            onClick = {
                if (node.isDirectory) {
                    expandedDirs[node.relativePath] = !isExpanded
                } else {
                    onFileClick(node.relativePath, node.name)
                }
            }
        )
    }
    
    if (node.isDirectory && isExpanded) {
        node.children.sortedWith(compareBy({ !it.isDirectory }, { it.name })).forEach { child ->
            fileTreeItems(child, depth + 1, expandedDirs, onFileClick)
        }
    }
}

/**
 * 文件树行
 */
@Composable
private fun FileTreeRow(
    node: FileNode,
    depth: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = (16 + depth * 20).dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        if (node.isDirectory) {
            Icon(
                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                getFileIcon(node.name),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = getFileIconColor(node.name)
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // 文件名
        Text(
            node.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = true)
        )
        
        // 文件大小
        if (!node.isDirectory && node.size > 0) {
            Text(
                formatFileSize(node.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        
        // 目录展开指示器
        if (node.isDirectory) {
            Icon(
                if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 文件内容查看视图
 */
@Composable
private fun FileContentView(
    content: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isBinary = content.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }
    val isImage = fileName.lowercase().let {
        it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") ||
        it.endsWith(".gif") || it.endsWith(".svg") || it.endsWith(".webp") || it.endsWith(".ico")
    }
    
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        // 文件信息栏
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    getFileIcon(fileName),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = getFileIconColor(fileName)
                )
                Text(
                    fileName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                Text(
                    "${content.length} chars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        // 文件内容
        if (isImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🖼️ ${Strings.imageFile}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isBinary) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    Strings.binaryFile,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // 代码/文本内容 - 等宽字体 + 行号
            val lines = content.lines()
            val lineNumWidth = lines.size.toString().length
            
            Text(
                buildAnnotatedString(lines, lineNumWidth),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            )
        }
    }
}

/**
 * 构建带行号的文本
 */
private fun buildAnnotatedString(lines: List<String>, lineNumWidth: Int): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val maxLines = 10000
        lines.take(maxLines).forEachIndexed { index, line ->
            val lineNum = (index + 1).toString().padStart(lineNumWidth)
            pushStyle(androidx.compose.ui.text.SpanStyle(
                color = androidx.compose.ui.graphics.Color.Gray
            ))
            append("$lineNum  ")
            pop()
            append(line)
            if (index < lines.size - 1) append("\n")
        }
        if (lines.size > maxLines) {
            append("\n\n... (${lines.size} lines total)")
        }
    }
}

/**
 * 根据文件扩展名返回图标
 */
@Composable
private fun getFileIcon(fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        fileName.endsWith(".js") || fileName.endsWith(".mjs") -> Icons.Outlined.Code
        fileName.endsWith(".ts") || fileName.endsWith(".tsx") -> Icons.Outlined.Code
        fileName.endsWith(".css") || fileName.endsWith(".scss") -> Icons.Outlined.Palette
        fileName.endsWith(".html") || fileName.endsWith(".htm") -> Icons.Outlined.Language
        fileName.endsWith(".json") -> Icons.Outlined.DataObject
        fileName.endsWith(".md") || fileName.endsWith(".txt") -> Icons.Outlined.Description
        fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".svg") || fileName.endsWith(".gif") || fileName.endsWith(".webp") || fileName.endsWith(".ico") -> Icons.Outlined.Image
        fileName.endsWith(".woff") || fileName.endsWith(".woff2") || fileName.endsWith(".ttf") -> Icons.Outlined.FontDownload
        fileName == "manifest.json" -> Icons.Outlined.Settings
        fileName == "LICENSE" || fileName.startsWith("LICENSE") -> Icons.Outlined.Gavel
        else -> Icons.Outlined.InsertDriveFile
    }
}

/**
 * 根据文件扩展名返回图标颜色
 */
@Composable
private fun getFileIconColor(fileName: String): androidx.compose.ui.graphics.Color {
    return when {
        fileName.endsWith(".js") || fileName.endsWith(".mjs") -> androidx.compose.ui.graphics.Color(0xFFF7DF1E)
        fileName.endsWith(".ts") || fileName.endsWith(".tsx") -> androidx.compose.ui.graphics.Color(0xFF3178C6)
        fileName.endsWith(".css") || fileName.endsWith(".scss") -> androidx.compose.ui.graphics.Color(0xFF1572B6)
        fileName.endsWith(".html") || fileName.endsWith(".htm") -> androidx.compose.ui.graphics.Color(0xFFE44D26)
        fileName.endsWith(".json") -> androidx.compose.ui.graphics.Color(0xFF5B9BD5)
        fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".svg") || fileName.endsWith(".gif") -> MaterialTheme.colorScheme.tertiary
        fileName == "manifest.json" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB"
    }
}

/**
 * 构建扩展项目文件树
 */
private fun buildExtensionFileTree(context: android.content.Context, module: ExtensionModule): FileNode? {
    val extId = module.chromeExtId
    if (extId.isEmpty()) return null
    
    return if (module.builtIn) {
        // 内置扩展：从 assets 读取
        buildAssetFileTree(context, "extensions/$extId", extId)
    } else {
        // 用户导入扩展：从 filesDir 读取
        val extDir = java.io.File(context.filesDir, "extensions/$extId")
        if (extDir.exists() && extDir.isDirectory) {
            buildFileSystemTree(extDir, "")
        } else {
            null
        }
    }
}

/**
 * 从 assets 构建文件树
 */
private fun buildAssetFileTree(context: android.content.Context, assetPath: String, name: String): FileNode {
    val root = FileNode(name = name, relativePath = "", isDirectory = true)
    
    fun walkAssets(currentPath: String, parent: FileNode) {
        try {
            val children = context.assets.list(currentPath) ?: return
            for (child in children) {
                val childPath = "$currentPath/$child"
                val relativePath = childPath.removePrefix("extensions/$name/")
                val subChildren = context.assets.list(childPath)
                
                if (subChildren != null && subChildren.isNotEmpty()) {
                    // 目录
                    val dirNode = FileNode(name = child, relativePath = relativePath, isDirectory = true)
                    walkAssets(childPath, dirNode)
                    parent.children.add(dirNode)
                } else {
                    // 文件
                    val size = try {
                        context.assets.open(childPath).use { it.available().toLong() }
                    } catch (e: Exception) { 0L }
                    parent.children.add(FileNode(name = child, relativePath = relativePath, isDirectory = false, size = size))
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    
    walkAssets(assetPath, root)
    return root
}

/**
 * 从文件系统构建文件树
 */
private fun buildFileSystemTree(dir: java.io.File, relativePath: String): FileNode {
    val root = FileNode(name = dir.name, relativePath = relativePath, isDirectory = true)
    
    dir.listFiles()?.forEach { file ->
        val childRelative = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
        if (file.isDirectory) {
            root.children.add(buildFileSystemTree(file, childRelative))
        } else {
            root.children.add(FileNode(name = file.name, relativePath = childRelative, isDirectory = false, size = file.length()))
        }
    }
    
    return root
}

/**
 * 读取扩展文件内容
 */
private fun readExtensionFile(context: android.content.Context, module: ExtensionModule, relativePath: String): String? {
    val extId = module.chromeExtId
    
    return try {
        if (module.builtIn) {
            context.assets.open("extensions/$extId/$relativePath").bufferedReader().use { it.readText() }
        } else {
            val file = java.io.File(context.filesDir, "extensions/$extId/$relativePath")
            if (file.exists()) file.readText() else null
        }
    } catch (e: Exception) {
        null
    }
}
