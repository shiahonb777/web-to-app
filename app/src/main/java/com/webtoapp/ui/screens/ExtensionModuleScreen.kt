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
                },
                actions = {
                    IconButton(onClick = onNavigateToMarket) {
                        Icon(Icons.Default.Storefront, contentDescription = Strings.moduleMarketTitle)
                    }
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

                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { it },
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) + slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessHigh)) { it }
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                fabExpanded = false
                                onNavigateToAiDeveloper()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(WtaRadius.Button))
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


                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { it },
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) + slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessHigh)) { it }
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                fabExpanded = false
                                onNavigateToEditor(null)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(WtaRadius.Button))
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


    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Strings.importModule) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {

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
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )


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




private data class FileNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val children: MutableList<FileNode> = mutableListOf()
)







@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionSourceBrowserDialog(
    module: ExtensionModule,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isChromeExt = module.sourceType == ModuleSourceType.CHROME_EXTENSION && module.chromeExtId.isNotEmpty()


    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedFileContent by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }


    val fileTree = remember(module.id) {
        if (isChromeExt) {
            buildExtensionFileTree(context, module)
        } else {
            null
        }
    }


    val expandedDirs = remember { mutableStateMapOf<String, Boolean>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(WtaRadius.Card))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column {

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

                    FileContentView(
                        content = selectedFileContent,
                        fileName = selectedFileName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isChromeExt && fileTree != null) {

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


        Text(
            node.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = true)
        )


        if (!node.isDirectory && node.size > 0) {
            Text(
                formatFileSize(node.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }


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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WtaRadius.Button))
                .background(
                    if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f)
                    else Color.White.copy(alpha = 0.72f)
                )
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




@Composable
private fun getFileIconColor(fileName: String): androidx.compose.ui.graphics.Color {
    return when {
        fileName.endsWith(".js") || fileName.endsWith(".mjs") -> MaterialTheme.colorScheme.tertiary
        fileName.endsWith(".ts") || fileName.endsWith(".tsx") -> MaterialTheme.colorScheme.primary
        fileName.endsWith(".css") || fileName.endsWith(".scss") -> MaterialTheme.colorScheme.secondary
        fileName.endsWith(".html") || fileName.endsWith(".htm") -> MaterialTheme.colorScheme.error
        fileName.endsWith(".json") -> MaterialTheme.colorScheme.primary
        fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".svg") || fileName.endsWith(".gif") -> MaterialTheme.colorScheme.tertiary
        fileName == "manifest.json" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}




private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB"
    }
}




private fun buildExtensionFileTree(context: android.content.Context, module: ExtensionModule): FileNode? {
    val extId = module.chromeExtId
    if (extId.isEmpty()) return null

    return if (module.builtIn) {

        buildAssetFileTree(context, "extensions/$extId", extId)
    } else {

        val extDir = java.io.File(context.filesDir, "extensions/$extId")
        if (extDir.exists() && extDir.isDirectory) {
            buildFileSystemTree(extDir, "")
        } else {
            null
        }
    }
}




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

                    val dirNode = FileNode(name = child, relativePath = relativePath, isDirectory = true)
                    walkAssets(childPath, dirNode)
                    parent.children.add(dirNode)
                } else {

                    val size = try {
                        context.assets.open(childPath).use { it.available().toLong() }
                    } catch (e: Exception) { 0L }
                    parent.children.add(FileNode(name = child, relativePath = relativePath, isDirectory = false, size = size))
                }
            }
        } catch (e: Exception) {

        }
    }

    walkAssets(assetPath, root)
    return root
}




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
