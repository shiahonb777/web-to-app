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
import com.webtoapp.core.i18n.Strings
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
 * 创建/编辑HTML应用页面
 * 支持单个HTML文件、HTML+CSS+JS项目
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateHtmlAppScreen(
    existingAppId: Long? = null,  // 编辑模式时传入已有应用ID
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
    importDir: String? = null,  // 从AI编程导入的目录
    importProjectName: String? = null  // Import的项目名称
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = existingAppId != null
    
    // 导入模式切换: 0=手动选择, 1=ZIP导入
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // 编辑模式时加载已有应用数据
    var existingApp by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = org.koin.java.KoinJavaComponent.get<com.webtoapp.data.repository.WebAppRepository>(com.webtoapp.data.repository.WebAppRepository::class.java)
                .getWebAppById(existingAppId)
                .first()
        }
    }
    val scrollState = rememberScrollState()
    
    // App信息
    var appName by remember { mutableStateOf(importProjectName ?: "") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }
    
    // ==================== ZIP 导入状态 ====================
    var zipAnalysis by remember { mutableStateOf<ZipProjectImporter.ZipProjectAnalysis?>(null) }
    var zipImporting by remember { mutableStateOf(false) }
    var zipError by remember { mutableStateOf<String?>(null) }
    var zipEntryFile by remember { mutableStateOf("") }
    var showEntryFileDialog by remember { mutableStateOf(false) }
    var showFileListDialog by remember { mutableStateOf(false) }
    
    // ==================== 文件夹导入状态 ====================
    var folderAnalysis by remember { mutableStateOf<ZipProjectImporter.ZipProjectAnalysis?>(null) }
    var folderImporting by remember { mutableStateOf(false) }
    var folderError by remember { mutableStateOf<String?>(null) }
    var folderEntryFile by remember { mutableStateOf("") }
    var showFolderEntryFileDialog by remember { mutableStateOf(false) }
    var showFolderFileListDialog by remember { mutableStateOf(false) }
    
    // 单HTML模式 - 三个独立的文件槽位
    var htmlFile by remember { mutableStateOf<HtmlFile?>(null) }
    var cssFile by remember { mutableStateOf<HtmlFile?>(null) }
    var jsFile by remember { mutableStateOf<HtmlFile?>(null) }
    
    // Configure选项（需要在 LaunchedEffect 之前声明）
    var enableJavaScript by remember { mutableStateOf(true) }
    var enableLocalStorage by remember { mutableStateOf(true) }
    var landscapeMode by remember { mutableStateOf(false) }
    
    // Theme配置（需要在 LaunchedEffect 之前声明）
    var themeType by remember { mutableStateOf("AURORA") }
    
    // 代码优化（Linux 环境）
    var enableOptimize by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizeResult by remember { mutableStateOf<HtmlProjectOptimizer.OptimizeResult?>(null) }
    val esbuildAvailable = remember { NativeNodeEngine.isAvailable(context) }
    
    // 编辑模式：加载现有应用数据到UI状态
    LaunchedEffect(existingApp) {
        existingApp?.let { app ->
            // 加载基本信息
            appName = app.name
            appIconPath = app.iconPath
            
            // 加载 HTML 配置
            app.htmlConfig?.let { config ->
                // 尝试从文件列表中恢复文件槽位
                config.files.forEach { file ->
                    when (file.type) {
                        HtmlFileType.HTML -> {
                            // 检查文件是否存在
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
                        else -> { /* 忽略其他类型 */ }
                    }
                }
                
                // 如果文件列表为空但 projectId 存在，尝试从目录中加载
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
                
                // 加载配置选项
                enableJavaScript = config.enableJavaScript
                enableLocalStorage = config.enableLocalStorage
                landscapeMode = config.landscapeMode
            }
            
            // 加载主题
            themeType = app.themeType
        }
    }
    
    // 从AI编程导入文件
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
    
    // 项目分析结果
    var projectAnalysis by remember { mutableStateOf<HtmlProjectProcessor.ProjectAnalysis?>(null) }
    var showAnalysisDialog by remember { mutableStateOf(false) }
    
    // 当文件变化时重新分析
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
    
    // 判断是否可以创建
    val canCreate = when (selectedTabIndex) {
        0 -> htmlFile != null
        1 -> zipAnalysis != null && zipAnalysis!!.htmlFiles.isNotEmpty()
        2 -> folderAnalysis != null && folderAnalysis!!.htmlFiles.isNotEmpty()
        else -> false
    }
    
    // Yes否有问题需要关注
    val hasIssues = projectAnalysis?.issues?.any { 
        it.severity == HtmlProjectProcessor.IssueSeverity.ERROR || 
        it.severity == HtmlProjectProcessor.IssueSeverity.WARNING 
    } == true
    
    // ZIP 文件选择器
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
                        zipError = Strings.zipNoHtmlWarning
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
    
    // 文件夹选择器 (OpenDocumentTree)
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
                        folderError = Strings.folderNoHtmlWarning
                    }
                } catch (e: Exception) {
                    folderError = e.message ?: "Unknown error"
                } finally {
                    folderImporting = false
                }
            }
        }
    }
    
    // HTML文件选择器
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
                // Auto设置应用名
                if (appName.isBlank()) {
                    appName = fileName.substringBeforeLast(".")
                }
            }
        }
    }
    
    // CSS文件选择器
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
    
    // JS文件选择器
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
    
    // Build文件列表
    val htmlFiles = remember(htmlFile, cssFile, jsFile) {
        listOfNotNull(htmlFile, cssFile, jsFile)
    }
    // Verify entryFile：必须有文件名部分（不能只是 .html）
    val entryFile = htmlFile?.name?.takeIf { 
        it.isNotBlank() && it.substringBeforeLast(".").isNotBlank() 
    } ?: "index.html"
    
    // Icon选择器
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
            title = { Text(Strings.unsavedChangesTitle) },
            text = { Text(Strings.unsavedChangesMessage) },
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
                    Text(Strings.discardChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(Strings.keepEditing)
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
                title = { Text(Strings.createHtmlAppTitle) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showExitConfirmDialog = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                            
                            when (selectedTabIndex) {
                                0 -> {
                                    // 手动模式
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
                                                appName.ifBlank { Strings.createHtmlApp },
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
                                            appName.ifBlank { Strings.createHtmlApp },
                                            config,
                                            finalIconUri,
                                            themeType
                                        )
                                    }
                                }
                                1 -> {
                                    // ZIP 导入模式
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
                                    // 文件夹导入模式
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
                            Text(Strings.optimizing)
                        } else {
                            Text(Strings.btnCreate)
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
            // ==================== 导入模式 Tab ====================
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
                                    Text(Strings.manualSelectMode)
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
                                    Text(Strings.zipImportMode)
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
                                    Text(Strings.folderImportMode)
                                }
                            }
                        )
                    }
                }
            }
            
            // ==================== 手动选择模式 ====================
            if (selectedTabIndex == 0) {
                // Select文件
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Strings.selectFiles,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.selectFilesHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // HTML文件槽位（必选）
                        FileSlotWithEditor(
                            label = Strings.htmlFile,
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
                        
                        // CSS文件槽位（可选）
                        FileSlotWithEditor(
                            label = Strings.cssFile,
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
                        
                        // JS文件槽位（可选）
                        FileSlotWithEditor(
                            label = Strings.jsFile,
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
                
                // ==================== 直接编写代码 ====================
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
                                text = Strings.writeCode,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Strings.writeCodeHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // HTML 编写按钮
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
                            // CSS 编写按钮
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
                            // JS 编写按钮
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
            
            // ==================== ZIP 导入模式 ====================
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
            
            // ==================== 文件夹导入模式 ====================
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
            
            // App信息
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.labelAppInfo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // App名称（带随机按钮）
                    AppNameTextFieldSimple(
                        value = appName,
                        onValueChange = { appName = it }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // App图标（带图标库功能）
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
            
            // 高级配置
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.labelAdvancedConfig,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // JavaScript 开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(Strings.enableJavaScript)
                            Text(
                                text = Strings.enableJsHint,
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
                    
                    // Local存储开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(Strings.enableLocalStorage)
                            Text(
                                text = Strings.enableLocalStorageHint,
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
                    
                    // Landscape模式开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(Strings.landscapeModeLabel)
                            Text(
                                text = Strings.landscapeModeHintHtml,
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
                    
                    // 代码优化开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Strings.optimizeCode)
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
                                text = Strings.optimizeCodeHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = enableOptimize,
                            onCheckedChange = { enableOptimize = it }
                        )
                    }
                    
                    // 优化结果展示
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
                                        text = Strings.optimizeComplete,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (result.jsFilesOptimized > 0) {
                                        Text(
                                            text = Strings.optimizeResultJs.replace("%d", result.jsFilesOptimized.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.cssFilesOptimized > 0) {
                                        Text(
                                            text = Strings.optimizeResultCss.replace("%d", result.cssFilesOptimized.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.tsFilesCompiled > 0) {
                                        Text(
                                            text = Strings.optimizeResultTs.replace("%d", result.tsFilesCompiled.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.savedBytes > 0) {
                                        Text(
                                            text = Strings.optimizeResultSaved.replace("%s", formatFileSize(result.savedBytes)),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = Strings.optimizeFailed.replace("%s", result.error ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 项目问题警告卡片
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
                                    text = Strings.projectIssuesDetected,
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
                                        if (errorCount > 0) append(Strings.errorsCount.replace("%d", errorCount.toString()))
                                        if (errorCount > 0 && warningCount > 0) append(", ")
                                        if (warningCount > 0) append(Strings.warningsCount.replace("%d", warningCount.toString()))
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Strings.autoFixHint,
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
                            Text(Strings.viewAnalysisResult)
                        }
                    }
                }
            }
            
            // 提示信息（根据模式显示不同提示）
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
                            0 -> Strings.htmlAppTip
                            1 -> Strings.zipTip
                            2 -> Strings.folderTip
                            else -> Strings.htmlAppTip
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // 功能提示
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
                        text = Strings.featureTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            // Path引用提示（仅手动模式）
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
                                text = Strings.aboutFileReference,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Strings.fileReferenceHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 项目分析结果对话框
    if (showAnalysisDialog && projectAnalysis != null) {
        ProjectAnalysisDialog(
            analysis = projectAnalysis!!,
            onDismiss = { showAnalysisDialog = false }
        )
    }
    
    // ZIP 入口文件选择对话框
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
    
    // ZIP 文件列表对话框
    if (showFileListDialog && zipAnalysis != null) {
        ZipFileListDialog(
            analysis = zipAnalysis!!,
            onDismiss = { showFileListDialog = false }
        )
    }
    
    // 文件夹导入 - 入口文件选择对话框
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
    
    // 文件夹导入 - 文件列表对话框
    if (showFolderFileListDialog && folderAnalysis != null) {
        ZipFileListDialog(
            analysis = folderAnalysis!!,
            onDismiss = { showFolderFileListDialog = false }
        )
    }
        }
}

/**
 * 项目分析结果对话框
 */
@Composable
private fun ProjectAnalysisDialog(
    analysis: HtmlProjectProcessor.ProjectAnalysis,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.projectAnalysisResult)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File信息
                item {
                    Text(
                        text = Strings.fileInfo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        analysis.htmlFiles.forEach { file ->
                            FileInfoRow(file, "HTML")
                        }
                        analysis.cssFiles.forEach { file ->
                            FileInfoRow(file, "CSS")
                        }
                        analysis.jsFiles.forEach { file ->
                            FileInfoRow(file, "JS")
                        }
                    }
                }
                
                // 问题列表
                if (analysis.issues.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.detectedIssues,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    items(analysis.issues) { issue ->
                        IssueCard(issue)
                    }
                }
                
                // 建议
                if (analysis.suggestions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.suggestions,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    items(analysis.suggestions) { suggestion ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Auto修复说明
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.autoProcessHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.gotIt)
            }
        }
    )
}

/**
 * 文件信息行
 */
@Composable
private fun FileInfoRow(file: HtmlProjectProcessor.FileInfo, type: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.shapes.small
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = type,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.shapes.extraSmall
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = Strings.encodingAndSize.format(file.encoding ?: "UTF-8", formatFileSize(file.size)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 问题卡片
 */
@Composable
private fun IssueCard(issue: HtmlProjectProcessor.ProjectIssue) {
    val (icon, containerColor, contentColor) = when (issue.severity) {
        HtmlProjectProcessor.IssueSeverity.ERROR -> Triple(
            Icons.Outlined.Error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        HtmlProjectProcessor.IssueSeverity.WARNING -> Triple(
            Icons.Outlined.Warning,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        HtmlProjectProcessor.IssueSeverity.INFO -> Triple(
            Icons.Outlined.Info,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    
    EnhancedElevatedCard(
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
            if (issue.file != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.fileLabel.format(issue.file),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            if (issue.suggestion != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${Strings.suggestions}: ${issue.suggestion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format(java.util.Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0))
    }
}

/**
 * 文件槽位组件
 */
@Composable
private fun FileSlot(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    file: HtmlFile?,
    required: Boolean,
    onSelect: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (file != null) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
            )
            .border(
                width = 1.dp,
                color = if (file != null)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else if (required)
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (file != null) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (required) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "*",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (file != null) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = Strings.clickToSelectFile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (file != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = Strings.clearFile,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 带编辑按钮的文件槽位组件
 */
@Composable
private fun FileSlotWithEditor(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    file: HtmlFile?,
    required: Boolean,
    onSelect: () -> Unit,
    onClear: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (file != null) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
            )
            .border(
                width = 1.dp,
                color = if (file != null)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else if (required)
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (file != null) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (required) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "*",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (file != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${Strings.orWriteDirectly}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "${Strings.clickToSelectFile} ${Strings.orWriteDirectly}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Edit button
        IconButton(
            onClick = { onEdit() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = Strings.editCode,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        if (file != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = Strings.clearFile,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 全屏代码编辑器对话框
 */
@Composable
private fun CodeEditorDialog(
    fileType: HtmlFileType,
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var codeText by remember { mutableStateOf(initialContent) }
    val isModified = codeText != initialContent
    
    val title = when (fileType) {
        HtmlFileType.HTML -> "HTML"
        HtmlFileType.CSS -> "CSS"
        HtmlFileType.JS -> "JavaScript"
        else -> Strings.codeEditorTitle
    }
    
    val placeholder = when (fileType) {
        HtmlFileType.HTML -> Strings.htmlCodePlaceholder
        HtmlFileType.CSS -> Strings.cssCodePlaceholder
        HtmlFileType.JS -> Strings.jsCodePlaceholder
        else -> ""
    }
    
    // Accent color for syntax label
    val accentColor = when (fileType) {
        HtmlFileType.HTML -> Color(0xFFE44D26)
        HtmlFileType.CSS -> Color(0xFF264DE4)
        HtmlFileType.JS -> Color(0xFFF7DF1E)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Dialog(
        onDismissRequest = {
            if (!isModified) onDismiss()
            // If modified, user must explicitly save or discard
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isModified
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            color = Color(0xFF1E1E1E), // VS Code dark bg
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ==================== Top Bar ====================
                Surface(
                    color = Color(0xFF252526),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isModified) {
                                // Just dismiss without saving when back is pressed
                                // The user clicked X, they probably want to cancel
                            }
                            onDismiss()
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = Strings.close,
                                tint = Color(0xFFCCCCCC)
                            )
                        }
                        
                        // File type badge
                        Surface(
                            color = accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = Strings.codeEditorTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFCCCCCC),
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isModified) {
                            Surface(
                                color = Color(0xFF4EC9B0).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "●",
                                    color = Color(0xFF4EC9B0),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        // Save button
                        TextButton(
                            onClick = { onSave(codeText) },
                            enabled = codeText.isNotBlank()
                        ) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = null,
                                tint = if (codeText.isNotBlank()) Color(0xFF4EC9B0) else Color(0xFF666666),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = Strings.saveFile,
                                color = if (codeText.isNotBlank()) Color(0xFF4EC9B0) else Color(0xFF666666)
                            )
                        }
                    }
                }
                
                // ==================== Code Editor Area ====================
                val scrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Line numbers
                        val lineCount = maxOf(codeText.count { it == '\n' } + 1, 1)
                        Column(
                            modifier = Modifier
                                .width(44.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF1E1E1E))
                                .verticalScroll(scrollState)
                                .padding(end = 8.dp, top = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = Color(0xFF858585)
                                    )
                                )
                            }
                        }
                        
                        // Vertical divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF333333))
                        )
                        
                        // Code input area
                        BasicTextField(
                            value = codeText,
                            onValueChange = { codeText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFFD4D4D4)
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .horizontalScroll(horizontalScrollState)
                                .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (codeText.isEmpty()) {
                                        Text(
                                            text = placeholder,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp,
                                                color = Color(0xFF555555)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
                
                // ==================== Bottom Status Bar ====================
                Surface(
                    color = Color(0xFF007ACC)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically

                    ) {
                        val lineCount = codeText.count { it == '\n' } + 1
                        val charCount = codeText.length
                        Text(
                            text = "$lineCount ${if (lineCount == 1) "line" else "lines"}, $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 从Uri获取文件名
 */
private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result
}

/**
 * 复制Uri内容到临时文件
 */
private fun copyUriToTempFile(
    context: android.content.Context,
    uri: Uri,
    fileName: String?
): File? {
    return try {
        val tempDir = File(context.cacheDir, "html_temp").apply { mkdirs() }
        val targetFile = File(tempDir, fileName ?: "file_${System.currentTimeMillis()}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    } catch (e: Exception) {
        AppLogger.e("CreateHtmlAppScreen", "Operation failed", e)
        null
    }
}

/**
 * 根据文件名获取文件类型
 */
private fun getFileType(fileName: String): HtmlFileType {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "html", "htm" -> HtmlFileType.HTML
        "css" -> HtmlFileType.CSS
        "js" -> HtmlFileType.JS
        "png", "jpg", "jpeg", "gif", "webp", "svg", "ico" -> HtmlFileType.IMAGE
        "ttf", "otf", "woff", "woff2", "eot" -> HtmlFileType.FONT
        else -> HtmlFileType.OTHER
    }
}

// ==================== ZIP 导入相关组件 ====================

/**
 * ZIP 导入区域
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZipImportSection(
    zipAnalysis: ZipProjectImporter.ZipProjectAnalysis?,
    zipImporting: Boolean,
    zipError: String?,
    zipEntryFile: String,
    onSelectZip: () -> Unit,
    onChangeEntry: () -> Unit,
    onShowFileList: () -> Unit,
    onReimport: () -> Unit
) {
    if (zipImporting) {
        // 解压中
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Strings.zipImporting,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else if (zipAnalysis != null) {
        // 分析结果展示
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题 + 重新导入按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.zipProjectAnalysis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onReimport) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.zipReimport, style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 入口文件
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { onChangeEntry() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.zipEntryFile,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = zipEntryFile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (zipAnalysis.htmlFiles.size > 1) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = Strings.zipChangeEntry,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 资源统计
                Text(
                    text = Strings.zipResourceStats,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 资源类型标签
                val stats = zipAnalysis.stats
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.forEach { stat ->
                        AssistChip(
                            onClick = { onShowFileList() },
                            label = {
                                Text(
                                    "${stat.type.icon} ${stat.type.label}: ${stat.count}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 文件总数和大小
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Strings.zipTotalFiles.replace("%d", zipAnalysis.totalFileCount.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Strings.zipTotalSize.replace("%s", zipAnalysis.formattedTotalSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 查看文件列表按钮
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onShowFileList,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Outlined.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.zipFileTreeTitle, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        
        // 警告信息
        if (zipAnalysis.warnings.isNotEmpty()) {
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    zipAnalysis.warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    } else {
        // 初始状态：选择 ZIP
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.selectZipFile,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Strings.selectZipHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumOutlinedButton(
                    onClick = onSelectZip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.selectZipFile)
                }
                
                // 错误信息
                if (zipError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = zipError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ZIP 入口文件选择对话框
 */
@Composable
private fun ZipEntryFileDialog(
    htmlFiles: List<String>,
    currentEntry: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.zipSelectEntryTitle)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(htmlFiles) { file ->
                    val isSelected = file == currentEntry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable { onSelect(file) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSelected) Icons.Filled.RadioButtonChecked
                            else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = file,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.gotIt)
            }
        }
    )
}

/**
 * ZIP 文件列表对话框
 */
@Composable
private fun ZipFileListDialog(
    analysis: ZipProjectImporter.ZipProjectAnalysis,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    Strings.zipFileTreeTitle + " (${analysis.totalFileCount})"
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 按资源类型分组展示
                analysis.stats.forEach { stat ->
                    item {
                        Text(
                            text = "${stat.type.icon} ${stat.type.label} (${stat.count})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    
                    val filesOfType = analysis.allFiles.filter { it.resourceType == stat.type }
                    items(filesOfType) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = file.relativePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(weight = 1f, fill = true),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = file.size.toFileSizeString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.gotIt)
            }
        }
    )
}

/** Helper: format Long as file size */
private fun Long.toFileSizeString(): String = when {
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> "${this / 1024} KB"
    else -> String.format(java.util.Locale.getDefault(), "%.1f MB", this / (1024.0 * 1024.0))
}

// ==================== 文件夹导入相关组件 ====================

/**
 * 文件夹导入区域
 * 复用 ZIP 导入的 UI 模式和数据结构
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderImportSection(
    folderAnalysis: ZipProjectImporter.ZipProjectAnalysis?,
    folderImporting: Boolean,
    folderError: String?,
    folderEntryFile: String,
    onSelectFolder: () -> Unit,
    onChangeEntry: () -> Unit,
    onShowFileList: () -> Unit,
    onReimport: () -> Unit
) {
    if (folderImporting) {
        // 导入中
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Strings.folderImporting,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else if (folderAnalysis != null) {
        // 分析结果展示（复用 ZIP 分析结果的 UI 结构）
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题 + 重新选择按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.zipProjectAnalysis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onReimport) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.zipReimport, style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 入口文件
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { onChangeEntry() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.zipEntryFile,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = folderEntryFile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (folderAnalysis.htmlFiles.size > 1) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = Strings.zipChangeEntry,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 资源统计
                Text(
                    text = Strings.zipResourceStats,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 资源类型标签
                val stats = folderAnalysis.stats
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.forEach { stat ->
                        AssistChip(
                            onClick = { onShowFileList() },
                            label = {
                                Text(
                                    "${stat.type.icon} ${stat.type.label}: ${stat.count}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 文件总数和大小
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Strings.zipTotalFiles.replace("%d", folderAnalysis.totalFileCount.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Strings.zipTotalSize.replace("%s", folderAnalysis.formattedTotalSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 查看文件列表按钮
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onShowFileList,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Outlined.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.zipFileTreeTitle, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        
        // 警告信息
        if (folderAnalysis.warnings.isNotEmpty()) {
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    folderAnalysis.warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    } else {
        // 初始状态：选择文件夹
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.folderSelectFolder,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Strings.folderSelectHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumOutlinedButton(
                    onClick = onSelectFolder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.folderSelectFolder)
                }
                
                // 错误信息
                if (folderError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = folderError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 文件夹导入核心逻辑 ====================

/** 应当跳过的文件/目录 */
private val FOLDER_SKIP_PATTERNS = setOf(
    "__MACOSX", ".DS_Store", "Thumbs.db", ".git", ".svn", ".hg",
    "node_modules", ".idea", ".vscode"
)

/**
 * 从 SAF 文件夹导入 HTML 项目
 * 
 * 使用 DocumentsContract API 递归遍历 SAF 文档树，
 * 将所有文件复制到本地缓存目录，然后分析项目结构。
 */
private fun importFolderFromSaf(
    context: android.content.Context,
    treeUri: Uri
): ZipProjectImporter.ZipProjectAnalysis {
    val tempDir = File(context.cacheDir, "folder_import_${System.currentTimeMillis()}").apply {
        if (exists()) deleteRecursively()
        mkdirs()
    }
    
    try {
        // 递归复制 SAF 文档树到本地目录
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        
        // 获取文件夹名称作为建议的应用名
        var folderName = "HTML Project"
        context.contentResolver.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                folderName = cursor.getString(0) ?: folderName
            }
        }
        
        copyDocumentTree(context, treeUri, docId, tempDir)
        
        // 处理嵌套根目录（和 ZIP 导入一样）
        val projectRoot = unwrapSingleRootDir(tempDir)
        
        // 分析项目
        return analyzeFolder(projectRoot, folderName)
        
    } catch (e: Exception) {
        tempDir.deleteRecursively()
        AppLogger.e("FolderImport", "文件夹导入失败", e)
        throw RuntimeException(Strings.folderImportFailed.replace("%s", e.message ?: "Unknown"), e)
    }
}

/**
 * 递归复制 SAF 文档树到本地目录
 */
private fun copyDocumentTree(
    context: android.content.Context,
    treeUri: Uri,
    parentDocId: String,
    targetDir: File
) {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    
    context.contentResolver.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        ),
        null, null, null
    )?.use { cursor ->
        val docIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        
        while (cursor.moveToNext()) {
            val childDocId = cursor.getString(docIdIndex)
            val childName = cursor.getString(nameIndex) ?: continue
            val mimeType = cursor.getString(mimeIndex) ?: ""
            
            // 跳过不需要的文件/目录
            if (FOLDER_SKIP_PATTERNS.any { childName.equals(it, ignoreCase = true) }) {
                continue
            }
            
            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                // 子目录：递归处理
                val subDir = File(targetDir, childName).apply { mkdirs() }
                copyDocumentTree(context, treeUri, childDocId, subDir)
            } else {
                // 文件：复制到本地
                val targetFile = File(targetDir, childName)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                try {
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("FolderImport", "复制文件失败: $childName", e)
                }
            }
        }
    }
}

/**
 * 如果目录中只有一个子目录，展开它（和 ZIP 导入一致的逻辑）
 */
private fun unwrapSingleRootDir(dir: File): File {
    val children = dir.listFiles() ?: return dir
    return if (children.size == 1 && children[0].isDirectory) {
        children[0]
    } else {
        dir
    }
}

/** 文件分类扩展名集合 */
private val HTML_EXT = setOf("html", "htm", "xhtml")
private val CSS_EXT = setOf("css")
private val JS_EXT = setOf("js", "mjs", "jsx", "ts", "tsx")
private val IMG_EXT = setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "bmp", "avif")
private val FONT_EXT = setOf("ttf", "otf", "woff", "woff2", "eot")
private val AUDIO_EXT = setOf("mp3", "wav", "ogg", "aac", "flac", "m4a")
private val VIDEO_EXT = setOf("mp4", "webm", "mkv", "avi", "mov")
private val DATA_EXT = setOf("json", "xml", "csv", "txt", "md", "yaml", "yml")

/**
 * 分析文件夹项目，复用 ZipProjectAnalysis 数据结构
 */
private fun analyzeFolder(
    projectDir: File,
    folderName: String
): ZipProjectImporter.ZipProjectAnalysis {
    val allFiles = mutableListOf<ZipProjectImporter.ProjectFile>()
    val warnings = mutableListOf<String>()
    
    projectDir.walkTopDown()
        .filter { it.isFile }
        .filter { file -> !FOLDER_SKIP_PATTERNS.any { file.name.equals(it, ignoreCase = true) } }
        .forEach { file ->
            val relativePath = file.relativeTo(projectDir).path
            val resourceType = classifyFileByExt(file.name)
            allFiles.add(
                ZipProjectImporter.ProjectFile(
                    relativePath = relativePath,
                    absolutePath = file.absolutePath,
                    size = file.length(),
                    resourceType = resourceType
                )
            )
        }
    
    val htmlFiles = allFiles.filter { it.resourceType == ZipProjectImporter.ResourceType.HTML }
    
    // 自动识别入口文件
    val entryFile = htmlFiles.find { it.relativePath.equals("index.html", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { it.relativePath.equals("index.htm", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { it.fileName.equals("index.html", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { !it.relativePath.contains('/') }?.relativePath
        ?: htmlFiles.firstOrNull()?.relativePath
    
    if (entryFile == null && htmlFiles.isEmpty()) {
        warnings.add(Strings.folderNoHtmlWarning)
    }
    
    if (allFiles.any { it.size > 50 * 1024 * 1024 }) {
        warnings.add("存在超过 50MB 的大文件，打包 APK 时可能影响安装包大小")
    }
    
    val stats = ZipProjectImporter.ResourceType.entries
        .map { type ->
            val files = allFiles.filter { it.resourceType == type }
            ZipProjectImporter.ResourceStats(
                type = type,
                count = files.size,
                totalSize = files.sumOf { it.size }
            )
        }
        .filter { it.count > 0 }
    
    return ZipProjectImporter.ZipProjectAnalysis(
        extractDir = projectDir.absolutePath,
        allFiles = allFiles,
        entryFile = entryFile ?: htmlFiles.firstOrNull()?.relativePath ?: "index.html",
        htmlFiles = htmlFiles,
        stats = stats,
        totalFileCount = allFiles.size,
        totalSize = allFiles.sumOf { it.size },
        warnings = warnings,
        zipFileName = folderName  // 复用 zipFileName 字段作为文件夹名
    )
}

/** 根据文件扩展名分类资源类型 */
private fun classifyFileByExt(fileName: String): ZipProjectImporter.ResourceType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        in HTML_EXT -> ZipProjectImporter.ResourceType.HTML
        in CSS_EXT -> ZipProjectImporter.ResourceType.CSS
        in JS_EXT -> ZipProjectImporter.ResourceType.JS
        in IMG_EXT -> ZipProjectImporter.ResourceType.IMAGE
        in FONT_EXT -> ZipProjectImporter.ResourceType.FONT
        in AUDIO_EXT -> ZipProjectImporter.ResourceType.AUDIO
        in VIDEO_EXT -> ZipProjectImporter.ResourceType.VIDEO
        in DATA_EXT -> ZipProjectImporter.ResourceType.DATA
        else -> ZipProjectImporter.ResourceType.OTHER
    }
}
