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
import com.webtoapp.ui.components.ExtensionSourceBrowserDialog
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
import com.webtoapp.ui.design.WtaRadius
import kotlinx.coroutines.launch
import com.webtoapp.ui.design.WtaBackground
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.webtoapp.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExtensionModuleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToAiDeveloper: () -> Unit = {},
    onNavigateToMarket: () -> Unit = {},

) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensionManager = remember { ExtensionManager.getInstance(context) }

    val modules by extensionManager.modules.collectAsStateWithLifecycle()
    val builtInModules by extensionManager.builtInModules.collectAsStateWithLifecycle()
    val isModulesLoading by extensionManager.isLoading.collectAsStateWithLifecycle()
    val loadError by extensionManager.loadError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var showStoreDialog by remember { mutableStateOf(false) }

    val extensionFileManager = remember { ExtensionFileManager(context) }
    var showUserScriptPreview by remember { mutableStateOf<UserScriptParser.ParseResult?>(null) }
    var showChromeExtPreview by remember { mutableStateOf<ChromeExtensionParser.ParseResult?>(null) }
    var pendingChromeExtDir by remember { mutableStateOf<java.io.File?>(null) }

    var isImporting by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isImporting = true
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
                } finally {
                    isImporting = false
                }
            }
        }
    }

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

    var showJsPackagePreview by remember { mutableStateOf<ExtensionFileManager.ImportResult.JsPackage?>(null) }

    val chromeExtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isImporting = true
            scope.launch {
                val result = extensionFileManager.importChromeExtension(it)
                isImporting = false
                when (result) {
                    is ExtensionFileManager.ImportResult.ChromeExtension -> {
                        showChromeExtPreview = result.parseResult
                        pendingChromeExtDir = result.extractedDir
                    }
                    is ExtensionFileManager.ImportResult.JsPackage -> {
                        showJsPackagePreview = result
                    }
                    is ExtensionFileManager.ImportResult.Error -> {
                        Toast.makeText(context, context.getString(R.string.msg_import_failed, result.message), Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    val jsZipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isImporting = true
            scope.launch {
                val result = extensionFileManager.importJsZipPackage(it)
                isImporting = false
                when (result) {
                    is ExtensionFileManager.ImportResult.JsPackage -> {
                        showJsPackagePreview = result
                    }
                    is ExtensionFileManager.ImportResult.Error -> {
                        Toast.makeText(context, context.getString(R.string.msg_import_failed, result.message), Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

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

    val allModules = builtInModules + modules
    val extensionModules = allModules.filter { it.sourceType == ModuleSourceType.CUSTOM }
    val userScriptModules = allModules.filter { it.sourceType != ModuleSourceType.CUSTOM }

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
    }.let { list ->

        val seenExtIds = HashSet<String>()
        list.filter { module ->
            if (module.sourceType == ModuleSourceType.CHROME_EXTENSION && module.chromeExtId.isNotEmpty()) {
                seenExtIds.add(module.chromeExtId)
            } else {
                true
            }
        }
    }

    LaunchedEffect(loadError) {
        val error = loadError ?: return@LaunchedEffect
        val message = when (error) {
            is ExtensionLoadError.ParsingFailed -> {
                val backup = error.backupFileName
                if (backup != null) {
                    "${Strings.loadFailed}: modules.json corrupted, backup: $backup"
                } else {
                    "${Strings.loadFailed}: modules.json corrupted"
                }
            }
            is ExtensionLoadError.IoFailure -> {
                "${Strings.loadFailed}: ${error.message}"
            }
        }
        snackbarHostState.showSnackbar(message)
        extensionManager.clearLoadError()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(Strings.extensionModule) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                }

            )
        },
        floatingActionButton = {

            FloatingActionButton(
                onClick = { showImportDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 2.dp,
                    hoveredElevation = 3.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = Strings.addModule)
            }
        }
    ) { padding ->
        WtaBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

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
                shape = RoundedCornerShape(WtaRadius.Button)
            )

            val pagerState = rememberPagerState(pageCount = { 2 })
            val tabTitles = listOf(
                Strings.extensionModulesTab,
                Strings.userScriptsTab
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(WtaRadius.Control))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
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

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(WtaRadius.Button))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface
                                    else Color.Transparent
                                )
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                                            )
                                    ) {
                                        Text(
                                            "$count",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
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

                    0 -> ExtensionModulesTabContent(
                        filteredModules = filteredModules,
                        isLoading = isModulesLoading,
                        extensionManager = extensionManager,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        onCategoryChange = { selectedCategory = it },
                        onNavigateToEditor = onNavigateToEditor,
                        onNavigateToAiDeveloper = onNavigateToAiDeveloper,
                        onClearSearch = { searchQuery = "" }
                    )

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

    if (isImporting) {
        Dialog(onDismissRequest = {  }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(WtaRadius.Card))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                    Text(
                        Strings.importing,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showStoreDialog) {
        BrowserExtensionStoreDialog(
            extensionFileManager = extensionFileManager,
            extensionManager = extensionManager,
            onDismiss = { showStoreDialog = false }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },

            title = { Text(Strings.addModule) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text(
                        Strings.addEntrySectionInstall,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                    )

                    AddEntryRow(
                        icon = Icons.Default.Storefront,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = Strings.moduleMarketTitle,
                        subtitle = Strings.addEntryFromMarketDesc,
                        onClick = {
                            showImportDialog = false
                            onNavigateToMarket()
                        }
                    )

                    AddEntryRow(
                        icon = Icons.Default.Extension,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = Strings.browserExtStoreTitle,
                        subtitle = Strings.browserExtStoreDesc,
                        onClick = {
                            showImportDialog = false
                            showStoreDialog = true
                        }
                    )

                    AddEntryRow(
                        icon = Icons.Default.AutoAwesome,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = Strings.aiDevelop,
                        subtitle = Strings.addEntryAiDevelopDesc,
                        onClick = {
                            showImportDialog = false
                            onNavigateToAiDeveloper()
                        }
                    )

                    AddEntryRow(
                        icon = Icons.Default.Code,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = Strings.manualCreate,
                        subtitle = Strings.addEntryManualDesc,
                        onClick = {
                            showImportDialog = false
                            onNavigateToEditor(null)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )

                    Text(
                        Strings.addEntrySectionImport,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                showImportDialog = false
                                userScriptPickerLauncher.launch("*/*")
                            }
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
                                Icon(
                                    Icons.Outlined.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                showImportDialog = false
                                chromeExtPickerLauncher.launch("*/*")
                            }
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
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Extension,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                showImportDialog = false
                                jsZipPickerLauncher.launch("*/*")
                            }
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
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.FolderZip,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(Strings.importJsPackage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Text(
                                    Strings.importJsPackageHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                showImportDialog = false
                                filePickerLauncher.launch("*/*")
                            }
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                showImportDialog = false
                                qrCodeImagePickerLauncher.launch("image/*")
                            }
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

    showUserScriptPreview?.let { parseResult ->
        AlertDialog(
            onDismissRequest = { showUserScriptPreview = null },
            title = { Text(Strings.installUserScript) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(WtaRadius.Control))
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
                                Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                parseResult.module.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
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

                    if (parseResult.module.urlMatches.isNotEmpty()) {
                        Text(
                        "${Strings.matchingSites}: ${parseResult.module.urlMatches.size} ${Strings.matchRules}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (parseResult.module.gmGrants.isNotEmpty()) {
                        Text(
                            "${Strings.requiredApis}: ${parseResult.module.gmGrants.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

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

    showJsPackagePreview?.let { jsPackage ->
        var editableName by remember(jsPackage) { mutableStateOf(jsPackage.module.name) }

        AlertDialog(
            onDismissRequest = { showJsPackagePreview = null },
            title = { Text(Strings.installJsPackage) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(WtaRadius.Control))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.FolderZip,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "${jsPackage.fileCount} ${Strings.filesDetected}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${formatFileSize(jsPackage.totalSize)} ${Strings.totalSize}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (jsPackage.module.codeFiles.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WtaRadius.Button))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    Strings.multiFileStorageHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    PremiumTextField(
                        value = editableName,
                        onValueChange = { editableName = it },
                        label = { Text(Strings.extensionName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (jsPackage.module.codeFiles.isNotEmpty()) {
                        Text(
                            Strings.includedFiles,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WtaRadius.Control))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                val filePaths = jsPackage.module.codeFiles.keys.toList()
                                val displayCount = minOf(filePaths.size, 15)
                                filePaths.take(displayCount).forEach { path ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            if (path.endsWith(".css", true)) Icons.Outlined.Palette
                                            else Icons.Outlined.Javascript,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (path.endsWith(".css", true)) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.tertiary
                                        )
                                        Text(
                                            path,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (filePaths.size > displayCount) {
                                    Text(
                                        "... +${filePaths.size - displayCount}",
                                        modifier = Modifier.padding(start = 24.dp, top = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        jsPackage.module.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                PremiumButton(onClick = {
                    scope.launch {
                        val finalModule = jsPackage.module.copy(
                            name = editableName.ifBlank { jsPackage.module.name }
                        )
                        extensionManager.addModule(finalModule).onSuccess { module ->
                            Toast.makeText(context, context.getString(R.string.msg_import_success, module.name), Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(context, context.getString(R.string.msg_import_failed, e.message ?: "Unknown error"), Toast.LENGTH_SHORT).show()
                        }
                    }
                    showJsPackagePreview = null
                }) {
                    Text(Strings.install)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsPackagePreview = null }) {
                    Text(Strings.btnCancel)
                }
            }
        )
    }

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
                                .clip(RoundedCornerShape(WtaRadius.Control))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                parseResult.extensionName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
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

                    if (parseResult.supportedPermissions.isNotEmpty()) {
                        Text(
                            "${Strings.requiredApis}: ${parseResult.supportedPermissions.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    if (parseResult.unsupportedPermissions.isNotEmpty()) {
                        Text(
                            "⚠️ ${Strings.unsupportedApis}: ${parseResult.unsupportedPermissions.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCard(
    module: ExtensionModule,
    extensionManager: ExtensionManager,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WtaRadius.Card))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

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

                        if (module.builtIn) {
                            Box(
                                modifier = Modifier
                            .clip(RoundedCornerShape(WtaRadius.Button))
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

            if (module.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(module.tags.take(5)) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WtaRadius.Button))
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
        val fullModule = remember(module) { extensionManager.ensureCodeLoaded(module) }
        QrCodeShareDialog(
            module = fullModule,
            shareCode = fullModule.toShareCode(),
            onDismiss = { showQrCodeDialog = false }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(Strings.exportModule) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
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
                            }
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                showExportDialog = false
                                val fileName = extensionManager.getModuleExportFileName(module.id) ?: "module.wtamod"
                                createFileLauncher.launch(fileName)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                .clip(RoundedCornerShape(WtaRadius.Control))
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

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(WtaRadius.Card))
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
            fontWeight = FontWeight.SemiBold,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionModulesTabContent(
    filteredModules: List<ExtensionModule>,
    isLoading: Boolean,
    extensionManager: ExtensionManager,
    selectedCategory: ModuleCategory?,
    searchQuery: String,
    onCategoryChange: (ModuleCategory?) -> Unit,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToAiDeveloper: () -> Unit,
    onClearSearch: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                )
            }

            if (filteredModules.isEmpty() && isLoading && searchQuery.isBlank()) {
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                Strings.loading,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (filteredModules.isEmpty()) {
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
                                        shape = RoundedCornerShape(WtaRadius.Button),
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
                                        shape = RoundedCornerShape(WtaRadius.Button)
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
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.02f)
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
                                shape = RoundedCornerShape(WtaRadius.Button)
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WtaRadius.Card))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

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

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isChromeExt) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isChromeExt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

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

    if (showSourceDialog) {
        ExtensionSourceBrowserDialog(
            module = module,
            onDismiss = { showSourceDialog = false }
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB"
}

@Composable
private fun AddEntryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WtaRadius.Card))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
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
                                iconTint.copy(alpha = 0.15f),
                                iconTint.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconTint)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserExtensionStoreDialog(
    extensionFileManager: ExtensionFileManager,
    extensionManager: ExtensionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userModules by extensionManager.modules.collectAsStateWithLifecycle()

    val installedNames = remember(userModules) {
        userModules
            .filter { it.sourceType == ModuleSourceType.CHROME_EXTENSION }
            .map { it.name.trim() }
            .toSet()
    }

    var installingId by remember { mutableStateOf<String?>(null) }
    var customInput by remember { mutableStateOf("") }

    fun install(storeId: String, displayName: String) {
        if (installingId != null) return
        installingId = storeId
        scope.launch {
            val result = extensionFileManager.installChromeExtensionFromStore(storeId)
            when (result) {
                is ExtensionFileManager.ImportResult.ChromeExtension -> {
                    var ok = 0
                    result.parseResult.modules.forEach { module ->
                        extensionManager.addModule(module).onSuccess { ok++ }
                    }
                    if (ok > 0) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_import_success, result.parseResult.extensionName),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.msg_import_failed, context.getString(R.string.unknown_error)), Toast.LENGTH_SHORT).show()
                    }
                }
                is ExtensionFileManager.ImportResult.Error -> {
                    Toast.makeText(context, context.getString(R.string.msg_import_failed, result.message), Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(context, context.getString(R.string.msg_import_failed, context.getString(R.string.unknown_error)), Toast.LENGTH_SHORT).show()
                }
            }
            installingId = null
        }
    }

    Dialog(
        onDismissRequest = { if (installingId == null) onDismiss() },
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
                TopAppBar(
                    title = { Text(Strings.browserExtStoreTitle) },
                    navigationIcon = {
                        IconButton(onClick = { if (installingId == null) onDismiss() }) {
                            Icon(Icons.Default.Close, Strings.btnCancel)
                        }
                    }
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            Strings.browserExtStoreInstallByIdHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PremiumTextField(
                                value = customInput,
                                onValueChange = { customInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("chromewebstore.google.com/... or id") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            PremiumButton(
                                onClick = {
                                    val id = com.webtoapp.core.extension.BrowserExtensionStore.extractStoreId(customInput)
                                    if (id == null) {
                                        Toast.makeText(context, context.getString(R.string.msg_import_failed, "id"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        install(id, id)
                                    }
                                },
                                enabled = installingId == null,
                                shape = RoundedCornerShape(WtaRadius.Button)
                            ) {
                                Text(Strings.install, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                    }

                    items(com.webtoapp.core.extension.BrowserExtensionStore.catalog, key = { it.storeId }) { entry ->
                        val isInstalled = entry.name.trim() in installedNames
                        val isBusy = installingId == entry.storeId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Extension,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    entry.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            when {
                                isBusy -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                isInstalled -> Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                else -> PremiumButton(
                                    onClick = { install(entry.storeId, entry.name) },
                                    enabled = installingId == null,
                                    shape = RoundedCornerShape(WtaRadius.Button)
                                ) {
                                    Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Strings.install, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
