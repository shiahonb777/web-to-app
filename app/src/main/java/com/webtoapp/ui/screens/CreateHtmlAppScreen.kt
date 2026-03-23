package com.webtoapp.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.ui.components.*
import com.webtoapp.util.HtmlProjectProcessor
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * 创建/编辑HTML应用页面
 * 支持单个HTML文件、HTML+CSS+JS项目
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    importDir: String? = null,  // 从AI编程导入的目录
    importProjectName: String? = null  // Import的项目名称
) {
    val context = LocalContext.current
    val isEditMode = existingAppId != null
    
    // 编辑模式时加载已有应用数据
    var existingApp by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = com.webtoapp.WebToAppApplication.repository
                .getWebAppById(existingAppId)
                .first()
        }
    }
    val scrollState = rememberScrollState()
    
    // App信息
    var appName by remember { mutableStateOf(importProjectName ?: "") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }
    
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
            projectAnalysis = HtmlProjectProcessor.analyzeProject(
                htmlFilePath = htmlFile?.path,
                cssFilePath = cssFile?.path,
                jsFilePath = jsFile?.path
            )
        } else {
            projectAnalysis = null
        }
    }
    
    // 判断是否可以创建
    val canCreate = htmlFile != null
    
    // Yes否有问题需要关注
    val hasIssues = projectAnalysis?.issues?.any { 
        it.severity == HtmlProjectProcessor.IssueSeverity.ERROR || 
        it.severity == HtmlProjectProcessor.IssueSeverity.WARNING 
    } == true
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.createHtmlAppTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                            
                            // 单HTML模式
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
                        },
                        enabled = canCreate
                    ) {
                        Text(Strings.btnCreate)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    FileSlot(
                        label = Strings.htmlFile,
                        icon = Icons.Outlined.Code,
                        file = htmlFile,
                        required = true,
                        onSelect = { htmlPickerLauncher.launch("text/html") },
                        onClear = { htmlFile = null }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // CSS文件槽位（可选）
                    FileSlot(
                        label = Strings.cssFile,
                        icon = Icons.Outlined.Palette,
                        file = cssFile,
                        required = false,
                        onSelect = { cssPickerLauncher.launch("text/css") },
                        onClear = { cssFile = null }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // JS文件槽位（可选）
                    FileSlot(
                        label = Strings.jsFile,
                        icon = Icons.Outlined.Javascript,
                        file = jsFile,
                        required = false,
                        onSelect = { jsPickerLauncher.launch("*/*") },
                        onClear = { jsFile = null }
                    )
                }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Strings.enableJavaScript)
                            Text(
                                text = Strings.enableJsHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableJavaScript,
                            onCheckedChange = { enableJavaScript = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Local存储开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Strings.enableLocalStorage)
                            Text(
                                text = Strings.enableLocalStorageHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableLocalStorage,
                            onCheckedChange = { enableLocalStorage = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Landscape模式开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Strings.landscapeModeLabel)
                            Text(
                                text = Strings.landscapeModeHintHtml,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = landscapeMode,
                            onCheckedChange = { landscapeMode = it }
                        )
                    }
                }
            }
            
            // 项目问题警告卡片
            if (hasIssues && projectAnalysis != null) {
                Card(
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
                            Column(modifier = Modifier.weight(1f)) {
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
            
            // 提示信息
            Card(
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
                        text = Strings.htmlAppTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // 功能提示
            Card(
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
            
            // Path引用提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
    
    // 项目分析结果对话框
    if (showAnalysisDialog && projectAnalysis != null) {
        ProjectAnalysisDialog(
            analysis = projectAnalysis!!,
            onDismiss = { showAnalysisDialog = false }
        )
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
                Icon(
                    Icons.Outlined.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
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
                    Card(
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
        Column(modifier = Modifier.weight(1f)) {
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
    
    Card(
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
                    text = "💡 ${issue.suggestion}",
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
        else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
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
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
        Column(modifier = Modifier.weight(1f)) {
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
        e.printStackTrace()
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
