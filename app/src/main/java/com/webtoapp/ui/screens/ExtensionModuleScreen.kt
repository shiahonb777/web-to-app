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
import com.webtoapp.ui.screens.extensionmodule.ExtensionModulesTabContent
import com.webtoapp.ui.screens.extensionmodule.UserScriptsTabContent
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.webtoapp.R
import org.koin.compose.koinInject

/**
 * modulemanagement
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExtensionModuleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,  // null new
    onNavigateToAiDeveloper: () -> Unit = {},  // AI

) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensionManager: ExtensionManager = koinInject()
    
    val modules by extensionManager.modules.collectAsStateWithLifecycle()
    val builtInModules by extensionManager.builtInModules.collectAsStateWithLifecycle()
    
    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    
    // /Chrome importrelatedstate
    val extensionFileManager = remember { ExtensionFileManager(context) }
    var showUserScriptPreview by remember { mutableStateOf<UserScriptParser.ParseResult?>(null) }
    var showChromeExtPreview by remember { mutableStateOf<ChromeExtensionParser.ParseResult?>(null) }
    var pendingChromeExtDir by remember { mutableStateOf<java.io.File?>(null) }
    
    // Fileselect( . wtamod)
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
    
    // fileselect( . user. js /. js)
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
    
    // Chrome fileselect( . crx /. zip)
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
    
    // select
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
    
    // Filtermodule- remember, ensure StateFlow update UI
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
                // AI button
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
                
                // createbutton
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
                
                // FAB- Apple- style spring rotation
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
            // Search- MD3 SearchBar
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
            
            // Tab: module /
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
                    // Tab 0: module
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
                    // Tab 1
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
    
    // Import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Strings.importModule) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Userscript import
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
                    
                    // Chrome import
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
                    
                    // .wtamod file import
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
                    
                    // import
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
    
    // Userscript preview install dialog
    showUserScriptPreview?.let { parseResult ->
        AlertDialog(
            onDismissRequest = { showUserScriptPreview = null },
            title = { Text(Strings.installUserScript) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Script info
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
                    
                    // URL match rules
                    if (parseResult.module.urlMatches.isNotEmpty()) {
                        Text(
                        "${Strings.matchingSites}: ${parseResult.module.urlMatches.size} ${Strings.matchRules}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // GM API permissions
                    if (parseResult.module.gmGrants.isNotEmpty()) {
                        Text(
                            "${Strings.requiredApis}: ${parseResult.module.gmGrants.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    // Warning
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
    
    // Chrome extension preview install dialog
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
                    
                    // Warning
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








