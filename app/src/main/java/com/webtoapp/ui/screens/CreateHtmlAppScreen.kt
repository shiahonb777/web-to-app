package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumOutlinedButton
import android.provider.DocumentsContract
import com.webtoapp.core.logging.AppLogger
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.linux.HtmlProjectOptimizer
import com.webtoapp.core.linux.NativeNodeEngine
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.ui.components.*
import com.webtoapp.util.HtmlProjectProcessor
import com.webtoapp.util.ZipProjectImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import androidx.compose.ui.graphics.Color
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * create/editHTMLapp
 * support HTMLfile, HTML+CSS+JSitem
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateHtmlAppScreen(
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    existingAppId: Long? = null,  // editmode appID
    onBack: () -> Unit,
    onCreated: (
        name: String,
        htmlConfig: HtmlConfig?,
        iconUri: Uri?,
        themeType: String
    ) -> Unit,
    onZipCreated: (
        name: String,
        extractedDir: String,
        entryFile: String,
        iconUri: Uri?,
        enableJavaScript: Boolean,
        enableLocalStorage: Boolean,
        landscapeMode: Boolean
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    importDir: String? = null,  // fromAI import directory
    importProjectName: String? = null  // Import item
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = existingAppId != null
    
    // importmodeswitch: 0= select, 1=ZIPimport
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // editmodeload app
    var existingApp by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = webAppRepository.getWebAppById(existingAppId).first()
        }
    }
    val scrollState = rememberScrollState()
    
    // App
    var appName by remember { mutableStateOf(importProjectName ?: "") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }
    
    // ==================== ZIP importstate ====================
    var zipAnalysis by remember { mutableStateOf<ZipProjectImporter.ZipProjectAnalysis?>(null) }
    var zipImporting by remember { mutableStateOf(false) }
    var zipError by remember { mutableStateOf<String?>(null) }
    var zipEntryFile by remember { mutableStateOf("") }
    var showEntryFileDialog by remember { mutableStateOf(false) }
    var showFileListDialog by remember { mutableStateOf(false) }
    
    // ==================== file importstate ====================
    var folderAnalysis by remember { mutableStateOf<ZipProjectImporter.ZipProjectAnalysis?>(null) }
    var folderImporting by remember { mutableStateOf(false) }
    var folderError by remember { mutableStateOf<String?>(null) }
    var folderEntryFile by remember { mutableStateOf("") }
    var showFolderEntryFileDialog by remember { mutableStateOf(false) }
    var showFolderFileListDialog by remember { mutableStateOf(false) }
    
    // HTMLmode- file
    var htmlFile by remember { mutableStateOf<HtmlFile?>(null) }
    var cssFile by remember { mutableStateOf<HtmlFile?>(null) }
    var jsFile by remember { mutableStateOf<HtmlFile?>(null) }
    
    // Configure( LaunchedEffect)
    var enableJavaScript by remember { mutableStateOf(true) }
    var enableLocalStorage by remember { mutableStateOf(true) }
    var landscapeMode by remember { mutableStateOf(false) }
    
    // Themeconfig( LaunchedEffect)
    var themeType by remember { mutableStateOf("AURORA") }
    
    // code( Linux)
    var enableOptimize by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizeResult by remember { mutableStateOf<HtmlProjectOptimizer.OptimizeResult?>(null) }
    val esbuildAvailable = remember { NativeNodeEngine.isAvailable(context) }
    
    // editmode: load app UIstate
    LaunchedEffect(existingApp) {
        existingApp?.let { app ->
            // load
            appName = app.name
            appIconPath = app.iconPath
            
            // load HTML config
            app.htmlConfig?.let { config ->
                // fromfilelist file
                config.files.forEach { file ->
                    when (file.type) {
                        HtmlFileType.HTML -> {
                            // checkfile
                            if (java.io.File(file.path).exists()) {
                                htmlFile = file
                            }
                        }
                        HtmlFileType.CSS -> {
                            if (java.io.File(file.path).exists()) {
                                cssFile = file
                            }
                        }
                        HtmlFileType.JS -> {
                            if (java.io.File(file.path).exists()) {
                                jsFile = file
                            }
                        }
                        else -> { /* type */ }
                    }
                }
                
                // iffilelist projectId, fromdirectory load
                if (htmlFile == null && config.projectId.isNotBlank()) {
                    val projectDir = java.io.File(context.filesDir, "html_projects/${config.projectId}")
                    if (projectDir.exists()) {
                        projectDir.listFiles()?.forEach { file ->
                            when {
                                file.name.endsWith(".html", ignoreCase = true) ||
                                file.name.endsWith(".htm", ignoreCase = true) -> {
                                    htmlFile = HtmlFile(
                                        name = file.name,
                                        path = file.absolutePath,
                                        type = HtmlFileType.HTML
                                    )
                                }
                                file.name.endsWith(".css", ignoreCase = true) -> {
                                    cssFile = HtmlFile(
                                        name = file.name,
                                        path = file.absolutePath,
                                        type = HtmlFileType.CSS
                                    )
                                }
                                file.name.endsWith(".js", ignoreCase = true) -> {
                                    jsFile = HtmlFile(
                                        name = file.name,
                                        path = file.absolutePath,
                                        type = HtmlFileType.JS
                                    )
                                }
                            }
                        }
                    }
                }
                
                // loadconfig
                enableJavaScript = config.enableJavaScript
                enableLocalStorage = config.enableLocalStorage
                landscapeMode = config.landscapeMode
            }
            
            // load
            themeType = app.themeType
        }
    }
    
    // fromAI importfile
    LaunchedEffect(importDir) {
        if (importDir != null) {
            val dir = java.io.File(importDir)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    when {
                        file.name.endsWith(".html", ignoreCase = true) || 
                        file.name.endsWith(".htm", ignoreCase = true) -> {
                            htmlFile = HtmlFile(
                                name = file.name,
                                path = file.absolutePath,
                                type = HtmlFileType.HTML
                            )
                        }
                        file.name.endsWith(".css", ignoreCase = true) -> {
                            cssFile = HtmlFile(
                                name = file.name,
                                path = file.absolutePath,
                                type = HtmlFileType.CSS
                            )
                        }
                        file.name.endsWith(".js", ignoreCase = true) -> {
                            jsFile = HtmlFile(
                                name = file.name,
                                path = file.absolutePath,
                                type = HtmlFileType.JS
                            )
                        }
                    }
                }
            }
        }
    }
    
    // item
    var projectAnalysis by remember { mutableStateOf<HtmlProjectProcessor.ProjectAnalysis?>(null) }
    var showAnalysisDialog by remember { mutableStateOf(false) }
    
    // whenfile
    LaunchedEffect(htmlFile, cssFile, jsFile) {
        if (htmlFile != null) {
            projectAnalysis = withContext(Dispatchers.IO) {
                HtmlProjectProcessor.analyzeProject(
                    htmlFilePath = htmlFile?.path,
                    cssFilePath = cssFile?.path,
                    jsFilePath = jsFile?.path
                )
            }
        } else {
            projectAnalysis = null
        }
    }
    
    // create
    val canCreate = when (selectedTabIndex) {
        0 -> htmlFile != null
        1 -> zipAnalysis != null && zipAnalysis!!.htmlFiles.isNotEmpty()
        2 -> folderAnalysis != null && folderAnalysis!!.htmlFiles.isNotEmpty()
        else -> false
    }
    
    // Yes
    val hasIssues = projectAnalysis?.issues?.any { 
        it.severity == HtmlProjectProcessor.IssueSeverity.ERROR || 
        it.severity == HtmlProjectProcessor.IssueSeverity.WARNING 
    } == true
    
    // ZIP fileselect
    val zipPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { zipUri ->
            zipImporting = true
            zipError = null
            zipAnalysis = null
            scope.launch {
                try {
                    val analysis = withContext(Dispatchers.IO) {
                        ZipProjectImporter.importZip(context, zipUri)
                    }
                    zipAnalysis = analysis
                    zipEntryFile = analysis.entryFile
                    if (appName.isBlank()) {
                        appName = analysis.suggestedAppName
                    }
                    if (analysis.htmlFiles.isEmpty()) {
                        zipError = AppStringsProvider.current().zipNoHtmlWarning
                    }
                } catch (e: ZipProjectImporter.ZipImportException) {
                    zipError = e.message
                } catch (e: Exception) {
                    zipError = e.message ?: "Unknown error"
                } finally {
                    zipImporting = false
                }
            }
        }
    }
    
    // file select( OpenDocumentTree)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { treeUri ->
            folderImporting = true
            folderError = null
            folderAnalysis = null
            scope.launch {
                try {
                    val analysis = withContext(Dispatchers.IO) {
                        importFolderFromSaf(context, treeUri)
                    }
                    folderAnalysis = analysis
                    folderEntryFile = analysis.entryFile
                    if (appName.isBlank()) {
                        appName = analysis.suggestedAppName
                    }
                    if (analysis.htmlFiles.isEmpty()) {
                        folderError = AppStringsProvider.current().folderNoHtmlWarning
                    }
                } catch (e: Exception) {
                    folderError = e.message ?: "Unknown error"
                } finally {
                    folderImporting = false
                }
            }
        }
    }
    
    // HTMLfileselect
    val htmlPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, it)
            val tempFile = copyUriToTempFile(context, it, fileName)
            if (tempFile != null && fileName != null) {
                htmlFile = HtmlFile(
                    name = fileName,
                    path = tempFile.absolutePath,
                    type = HtmlFileType.HTML
                )
                // Autosettingsapp
                if (appName.isBlank()) {
                    appName = fileName.substringBeforeLast(".")
                }
            }
        }
    }
    
    // CSSfileselect
    val cssPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, it)
            val tempFile = copyUriToTempFile(context, it, fileName)
            if (tempFile != null && fileName != null) {
                cssFile = HtmlFile(
                    name = fileName,
                    path = tempFile.absolutePath,
                    type = HtmlFileType.CSS
                )
            }
        }
    }
    
    // JSfileselect
    val jsPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, it)
            val tempFile = copyUriToTempFile(context, it, fileName)
            if (tempFile != null && fileName != null) {
                jsFile = HtmlFile(
                    name = fileName,
                    path = tempFile.absolutePath,
                    type = HtmlFileType.JS
                )
            }
        }
    }
    
    // Buildfilelist
    val htmlFiles = remember(htmlFile, cssFile, jsFile) {
        listOfNotNull(htmlFile, cssFile, jsFile)
    }
    // Verify entryFile: file( . html)
    val entryFile = htmlFile?.name?.takeIf { 
        it.isNotBlank() && it.substringBeforeLast(".").isNotBlank() 
    } ?: "index.html"
    
    // Iconselect
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    // ==================== Dirty State & Back Confirmation ====================
    // Track initial values to detect changes
    val initialAppName = remember { importProjectName ?: "" }
    val hasUnsavedChanges = remember(appName, htmlFile, cssFile, jsFile, appIcon, enableJavaScript, enableLocalStorage, landscapeMode) {
        appName != initialAppName || htmlFile != null || cssFile != null || jsFile != null || 
        appIcon != null || !enableJavaScript || !enableLocalStorage || landscapeMode
    }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    
    // ==================== Inline Code Editor State ====================
    var showCodeEditorDialog by remember { mutableStateOf(false) }
    var codeEditorType by remember { mutableStateOf(HtmlFileType.HTML) }
    var codeEditorContent by remember { mutableStateOf("") }
    
    // BackHandler — intercept back when there are unsaved changes
    BackHandler(enabled = hasUnsavedChanges) {
        showExitConfirmDialog = true
    }
    
    // Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(AppStringsProvider.current().unsavedChangesTitle) },
            text = { Text(AppStringsProvider.current().unsavedChangesMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(AppStringsProvider.current().discardChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(AppStringsProvider.current().keepEditing)
                }
            }
        )
    }
    
    // Code Editor Full-screen Dialog
    if (showCodeEditorDialog) {
        CodeEditorDialog(
            fileType = codeEditorType,
            initialContent = codeEditorContent,
            onSave = { content ->
                val fileName = when (codeEditorType) {
                    HtmlFileType.HTML -> "index.html"
                    HtmlFileType.CSS -> "style.css"
                    HtmlFileType.JS -> "script.js"
                    else -> "file.txt"
                }
                // Write content to a temp file
                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        val tempDir = File(context.cacheDir, "html_temp").apply { mkdirs() }
                        val targetFile = File(tempDir, fileName)
                        targetFile.writeText(content)
                        targetFile
                    }
                    val htmlFileObj = HtmlFile(
                        name = fileName,
                        path = file.absolutePath,
                        type = codeEditorType
                    )
                    when (codeEditorType) {
                        HtmlFileType.HTML -> {
                            htmlFile = htmlFileObj
                            if (appName.isBlank()) appName = "index"
                        }
                        HtmlFileType.CSS -> cssFile = htmlFileObj
                        HtmlFileType.JS -> jsFile = htmlFileObj
                        else -> {}
                    }
                }
                showCodeEditorDialog = false
            },
            onDismiss = { showCodeEditorDialog = false }
        )
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().createHtmlAppTitle) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showExitConfirmDialog = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                            
                            when (selectedTabIndex) {
                                0 -> {
                                    // mode
                                    if (enableOptimize && !isOptimizing) {
                                        isOptimizing = true
                                        scope.launch {
                                            val result = HtmlProjectOptimizer.optimizeFiles(
                                                context = context,
                                                jsFilePath = jsFile?.path,
                                                cssFilePath = cssFile?.path
                                            )
                                            optimizeResult = result
                                            isOptimizing = false
                                            
                                            val config = HtmlConfig(
                                                entryFile = entryFile,
                                                files = htmlFiles,
                                                enableJavaScript = enableJavaScript,
                                                enableLocalStorage = enableLocalStorage,
                                                landscapeMode = landscapeMode
                                            )
                                            onCreated(
                                                appName.ifBlank { AppStringsProvider.current().createHtmlApp },
                                                config,
                                                finalIconUri,
                                                themeType
                                            )
                                        }
                                    } else {
                                        val config = HtmlConfig(
                                            entryFile = entryFile,
                                            files = htmlFiles,
                                            enableJavaScript = enableJavaScript,
                                            enableLocalStorage = enableLocalStorage,
                                            landscapeMode = landscapeMode
                                        )
                                        onCreated(
                                            appName.ifBlank { AppStringsProvider.current().createHtmlApp },
                                            config,
                                            finalIconUri,
                                            themeType
                                        )
                                    }
                                }
                                1 -> {
                                    // ZIP importmode
                                    zipAnalysis?.let { analysis ->
                                        if (enableOptimize && !isOptimizing) {
                                            isOptimizing = true
                                            scope.launch {
                                                val result = HtmlProjectOptimizer.optimizeDirectory(
                                                    context = context,
                                                    projectDir = analysis.extractDir
                                                )
                                                optimizeResult = result
                                                isOptimizing = false
                                                
                                                onZipCreated(
                                                    appName.ifBlank { analysis.suggestedAppName },
                                                    analysis.extractDir,
                                                    zipEntryFile,
                                                    finalIconUri,
                                                    enableJavaScript,
                                                    enableLocalStorage,
                                                    landscapeMode
                                                )
                                            }
                                        } else {
                                            onZipCreated(
                                                appName.ifBlank { analysis.suggestedAppName },
                                                analysis.extractDir,
                                                zipEntryFile,
                                                finalIconUri,
                                                enableJavaScript,
                                                enableLocalStorage,
                                                landscapeMode
                                            )
                                        }
                                    }
                                }
                                2 -> {
                                    // file importmode
                                    folderAnalysis?.let { analysis ->
                                        if (enableOptimize && !isOptimizing) {
                                            isOptimizing = true
                                            scope.launch {
                                                val result = HtmlProjectOptimizer.optimizeDirectory(
                                                    context = context,
                                                    projectDir = analysis.extractDir
                                                )
                                                optimizeResult = result
                                                isOptimizing = false
                                                
                                                onZipCreated(
                                                    appName.ifBlank { analysis.suggestedAppName },
                                                    analysis.extractDir,
                                                    folderEntryFile,
                                                    finalIconUri,
                                                    enableJavaScript,
                                                    enableLocalStorage,
                                                    landscapeMode
                                                )
                                            }
                                        } else {
                                            onZipCreated(
                                                appName.ifBlank { analysis.suggestedAppName },
                                                analysis.extractDir,
                                                folderEntryFile,
                                                finalIconUri,
                                                enableJavaScript,
                                                enableLocalStorage,
                                                landscapeMode
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        enabled = canCreate && !isOptimizing
                    ) {
                        if (isOptimizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStringsProvider.current().optimizing)
                        } else {
                            Text(AppStringsProvider.current().btnCreate)
                        }
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
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== importmode Tab ====================
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium),
                        edgePadding = 0.dp
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.TouchApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStringsProvider.current().manualSelectMode)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.FolderZip,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStringsProvider.current().zipImportMode)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStringsProvider.current().folderImportMode)
                                }
                            }
                        )
                    }
                }
            }
            
            // ==================== selectmode ====================
            if (selectedTabIndex == 0) {
                // Selectfile
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = AppStringsProvider.current().selectFiles,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = AppStringsProvider.current().selectFilesHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // HTMLfile( )
                        FileSlotWithEditor(
                            label = AppStringsProvider.current().htmlFile,
                            icon = Icons.Outlined.Code,
                            file = htmlFile,
                            required = true,
                            onSelect = { htmlPickerLauncher.launch("text/html") },
                            onClear = { htmlFile = null },
                            onEdit = {
                                codeEditorType = HtmlFileType.HTML
                                codeEditorContent = if (htmlFile != null) {
                                    try { File(htmlFile!!.path).readText() } catch (e: Exception) { "" }
                                } else ""
                                showCodeEditorDialog = true
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // CSSfile( optional)
                        FileSlotWithEditor(
                            label = AppStringsProvider.current().cssFile,
                            icon = Icons.Outlined.Palette,
                            file = cssFile,
                            required = false,
                            onSelect = { cssPickerLauncher.launch("text/css") },
                            onClear = { cssFile = null },
                            onEdit = {
                                codeEditorType = HtmlFileType.CSS
                                codeEditorContent = if (cssFile != null) {
                                    try { File(cssFile!!.path).readText() } catch (e: Exception) { "" }
                                } else ""
                                showCodeEditorDialog = true
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // JSfile( optional)
                        FileSlotWithEditor(
                            label = AppStringsProvider.current().jsFile,
                            icon = Icons.Outlined.Javascript,
                            file = jsFile,
                            required = false,
                            onSelect = { jsPickerLauncher.launch("*/*") },
                            onClear = { jsFile = null },
                            onEdit = {
                                codeEditorType = HtmlFileType.JS
                                codeEditorContent = if (jsFile != null) {
                                    try { File(jsFile!!.path).readText() } catch (e: Exception) { "" }
                                } else ""
                                showCodeEditorDialog = true
                            }
                        )
                    }
                }
                
                // ==================== Direct Code Editing ====================
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = AppStringsProvider.current().writeCode,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AppStringsProvider.current().writeCodeHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // HTML edit button
                            PremiumOutlinedButton(
                                onClick = {
                                    codeEditorType = HtmlFileType.HTML
                                    codeEditorContent = if (htmlFile != null) {
                                        try { File(htmlFile!!.path).readText() } catch (e: Exception) { "" }
                                    } else "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>My App</title>\n</head>\n<body>\n    <h1>Hello World</h1>\n</body>\n</html>"
                                    showCodeEditorDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Code, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("HTML", maxLines = 1)
                            }
                            // CSS edit button
                            PremiumOutlinedButton(
                                onClick = {
                                    codeEditorType = HtmlFileType.CSS
                                    codeEditorContent = if (cssFile != null) {
                                        try { File(cssFile!!.path).readText() } catch (e: Exception) { "" }
                                    } else "/* Styles */\nbody {\n    margin: 0;\n    padding: 0;\n    font-family: sans-serif;\n}\n"
                                    showCodeEditorDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Palette, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CSS", maxLines = 1)
                            }
                            // JS button
                            PremiumOutlinedButton(
                                onClick = {
                                    codeEditorType = HtmlFileType.JS
                                    codeEditorContent = if (jsFile != null) {
                                        try { File(jsFile!!.path).readText() } catch (e: Exception) { "" }
                                    } else "// JavaScript\ndocument.addEventListener('DOMContentLoaded', () => {\n    console.log('App loaded');\n});\n"
                                    showCodeEditorDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Javascript, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("JS", maxLines = 1)
                            }
                        }
                    }
                }
            }
            
            // ==================== ZIP importmode ====================
            if (selectedTabIndex == 1) {
                ZipImportSection(
                    zipAnalysis = zipAnalysis,
                    zipImporting = zipImporting,
                    zipError = zipError,
                    zipEntryFile = zipEntryFile,
                    onSelectZip = { zipPickerLauncher.launch("application/zip") },
                    onChangeEntry = { showEntryFileDialog = true },
                    onShowFileList = { showFileListDialog = true },
                    onReimport = {
                        zipAnalysis = null
                        zipError = null
                        zipPickerLauncher.launch("application/zip")
                    }
                )
            }
            
            // ==================== file importmode ====================
            if (selectedTabIndex == 2) {
                FolderImportSection(
                    folderAnalysis = folderAnalysis,
                    folderImporting = folderImporting,
                    folderError = folderError,
                    folderEntryFile = folderEntryFile,
                    onSelectFolder = { folderPickerLauncher.launch(null) },
                    onChangeEntry = { showFolderEntryFileDialog = true },
                    onShowFileList = { showFolderFileListDialog = true },
                    onReimport = {
                        folderAnalysis = null
                        folderError = null
                        folderPickerLauncher.launch(null)
                    }
                )
            }
            
            // App
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStringsProvider.current().labelAppInfo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // App( button)
                    AppNameTextFieldSimple(
                        value = appName,
                        onValueChange = { appName = it }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Appicon( icon)
                    IconPickerWithLibrary(
                        iconUri = appIcon,
                        iconPath = appIconPath,
                        onSelectFromGallery = { iconPickerLauncher.launch("image/*") },
                        onSelectFromLibrary = { path -> 
                            appIconPath = path 
                            appIcon = null
                        }
                    )
                }
            }
            
            // Advanced settings
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStringsProvider.current().labelAdvancedConfig,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // JavaScript toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(AppStringsProvider.current().enableJavaScript)
                            Text(
                                text = AppStringsProvider.current().enableJsHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = enableJavaScript,
                            onCheckedChange = { enableJavaScript = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Local storage toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(AppStringsProvider.current().enableLocalStorage)
                            Text(
                                text = AppStringsProvider.current().enableLocalStorageHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = enableLocalStorage,
                            onCheckedChange = { enableLocalStorage = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Landscape mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(AppStringsProvider.current().landscapeModeLabel)
                            Text(
                                text = AppStringsProvider.current().landscapeModeHintHtml,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = landscapeMode,
                            onCheckedChange = { landscapeMode = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Code optimization toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(AppStringsProvider.current().optimizeCode)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (esbuildAvailable) "esbuild" else "built-in",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (esbuildAvailable) 
                                        MaterialTheme.colorScheme.primary
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            if (esbuildAvailable)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.shapes.extraSmall
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = AppStringsProvider.current().optimizeCodeHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = enableOptimize,
                            onCheckedChange = { enableOptimize = it }
                        )
                    }
                    
                    // Optimization result
                    if (optimizeResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val result = optimizeResult!!
                        EnhancedElevatedCard(
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.success) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else 
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (result.success) {
                                    Text(
                                        text = AppStringsProvider.current().optimizeComplete,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (result.jsFilesOptimized > 0) {
                                        Text(
                                            text = AppStringsProvider.current().optimizeResultJs.replace("%d", result.jsFilesOptimized.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.cssFilesOptimized > 0) {
                                        Text(
                                            text = AppStringsProvider.current().optimizeResultCss.replace("%d", result.cssFilesOptimized.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.tsFilesCompiled > 0) {
                                        Text(
                                            text = AppStringsProvider.current().optimizeResultTs.replace("%d", result.tsFilesCompiled.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.savedBytes > 0) {
                                        Text(
                                            text = AppStringsProvider.current().optimizeResultSaved.replace("%s", formatHtmlProjectFileSize(result.savedBytes)),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = AppStringsProvider.current().optimizeFailed.replace("%s", result.error ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Project issue warning card
            if (hasIssues && projectAnalysis != null) {
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                Text(
                                    text = AppStringsProvider.current().projectIssuesDetected,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                val errorCount = projectAnalysis!!.issues.count { 
                                    it.severity == HtmlProjectProcessor.IssueSeverity.ERROR 
                                }
                                val warningCount = projectAnalysis!!.issues.count { 
                                    it.severity == HtmlProjectProcessor.IssueSeverity.WARNING 
                                }
                                Text(
                                    text = buildString {
                                        if (errorCount > 0) append(AppStringsProvider.current().errorsCount.replace("%d", errorCount.toString()))
                                        if (errorCount > 0 && warningCount > 0) append(", ")
                                        if (warningCount > 0) append(AppStringsProvider.current().warningsCount.replace("%d", warningCount.toString()))
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = AppStringsProvider.current().autoFixHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showAnalysisDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStringsProvider.current().viewAnalysisResult)
                        }
                    }
                }
            }
            
            // Mode-based hint
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (selectedTabIndex) {
                            0 -> AppStringsProvider.current().htmlAppTip
                            1 -> AppStringsProvider.current().zipTip
                            2 -> AppStringsProvider.current().folderTip
                            else -> AppStringsProvider.current().htmlAppTip
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Feature hint
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = AppStringsProvider.current().featureTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            // Path reference hint (manual mode only)
            if (selectedTabIndex == 0) {
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = AppStringsProvider.current().aboutFileReference,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = AppStringsProvider.current().fileReferenceHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Project analysis dialog
    if (showAnalysisDialog && projectAnalysis != null) {
        ProjectAnalysisDialog(
            analysis = projectAnalysis!!,
            onDismiss = { showAnalysisDialog = false }
        )
    }
    
    // ZIP entry file dialog
    if (showEntryFileDialog && zipAnalysis != null) {
        ZipEntryFileDialog(
            htmlFiles = zipAnalysis!!.htmlFiles.map { it.relativePath },
            currentEntry = zipEntryFile,
            onSelect = { selected ->
                zipEntryFile = selected
                showEntryFileDialog = false
            },
            onDismiss = { showEntryFileDialog = false }
        )
    }
    
    // ZIP file list dialog
    if (showFileListDialog && zipAnalysis != null) {
        ZipFileListDialog(
            analysis = zipAnalysis!!,
            onDismiss = { showFileListDialog = false }
        )
    }
    
    // Folder import - entry file dialog
    if (showFolderEntryFileDialog && folderAnalysis != null) {
        ZipEntryFileDialog(
            htmlFiles = folderAnalysis!!.htmlFiles.map { it.relativePath },
            currentEntry = folderEntryFile,
            onSelect = { selected ->
                folderEntryFile = selected
                showFolderEntryFileDialog = false
            },
            onDismiss = { showFolderEntryFileDialog = false }
        )
    }
    
    // Folder import - file list dialog
    if (showFolderFileListDialog && folderAnalysis != null) {
        ZipFileListDialog(
            analysis = folderAnalysis!!,
            onDismiss = { showFolderFileListDialog = false }
        )
    }
        }
}
