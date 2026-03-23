package com.webtoapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.frontend.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.linux.*
import com.webtoapp.ui.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * 创建/编辑前端项目应用页面
 * 
 * 支持两种模式：
 * 1. 导入已构建的 dist 目录
 * 2. 完整构建（使用内置 Linux 环境）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFrontendAppScreen(
    onBack: () -> Unit,
    onCreated: (
        name: String,
        outputPath: String,
        iconUri: Uri?,
        framework: FrontendFramework
    ) -> Unit,
    onNavigateToLinuxEnv: () -> Unit = {},
    existingAppId: Long? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // 编辑模式
    val isEditMode = existingAppId != null
    
    // Linux 环境
    val linuxEnv = remember { LinuxEnvironmentManager.getInstance(context) }
    val linuxState by linuxEnv.state.collectAsState()
    
    // Build模式
    var buildMode by remember { mutableStateOf(BuildMode.IMPORT_DIST) }
    
    // 项目信息
    var projectPath by remember { mutableStateOf<String?>(null) }
    var projectName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var existingApp by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    
    // Load现有应用数据（编辑模式）
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = com.webtoapp.WebToAppApplication.repository
                .getWebAppById(existingAppId)
                .first()
            existingApp?.let { app ->
                projectName = app.name
                app.iconPath?.let { path -> appIcon = Uri.parse(path) }
                // FRONTEND 应用的文件存储在 htmlConfig 中
                app.htmlConfig?.files?.firstOrNull()?.path?.let { firstFilePath ->
                    val projectDir = File(firstFilePath).parentFile?.absolutePath
                    if (projectDir != null) {
                        projectPath = projectDir
                    }
                }
            }
        }
    }
    
    // 检测结果
    var detectionResult by remember { mutableStateOf<ProjectDetectionResult?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    
    // Build器
    val importBuilder = remember { FrontendProjectBuilder(context) }
    val nodeBuilder = remember { NodeProjectBuilder(context) }
    
    val importState by importBuilder.buildState.collectAsState()
    val importLogs by importBuilder.buildLogs.collectAsState()
    
    val nodeBuildState by nodeBuilder.buildState.collectAsState()
    val nodeBuildLogs by nodeBuilder.buildLogs.collectAsState()
    
    // 当前使用的状态和日志
    val currentBuildState = if (buildMode == BuildMode.FULL_BUILD) {
        when (val state = nodeBuildState) {
            is NodeBuildState.Idle -> BuildState.Idle
            is NodeBuildState.Analyzing -> BuildState.Scanning
            is NodeBuildState.CopyingFiles -> BuildState.CopyingProject(state.progress)
            is NodeBuildState.InstallingDeps -> BuildState.InstallingDependencies(state.progress, state.currentPackage)
            is NodeBuildState.Building -> BuildState.BuildingProject(state.progress, state.stage)
            is NodeBuildState.Processing -> BuildState.ProcessingOutput
            is NodeBuildState.Success -> BuildState.Success(state.outputPath, 0)
            is NodeBuildState.Error -> BuildState.Error(state.message)
        }
    } else {
        importState
    }
    
    val currentLogs = if (buildMode == BuildMode.FULL_BUILD) nodeBuildLogs else importLogs
    
    // Show日志对话框
    var showLogsDialog by remember { mutableStateOf(false) }
    
    // Check Linux 环境
    LaunchedEffect(Unit) {
        linuxEnv.checkEnvironment()
    }
    
    // Icon选择器
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    // File夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(context, it)
            if (path != null) {
                projectPath = path
                projectName = File(path).name
                
                // Auto检测项目
                scope.launch {
                    isDetecting = true
                    detectionResult = ProjectDetector.detectProject(path)
                    isDetecting = false
                }
            }
        }
    }
    
    // 判断是否可以操作
    val canImport = projectPath != null && 
                   detectionResult != null && 
                   detectionResult?.issues?.none { it.severity == IssueSeverity.ERROR } == true &&
                   currentBuildState is BuildState.Idle
    
    val canBuild = projectPath != null &&
                  detectionResult != null &&
                  linuxState is EnvironmentState.Ready &&
                  currentBuildState is BuildState.Idle
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) Strings.editFrontendApp else Strings.createFrontendApp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    if (currentLogs.isNotEmpty()) {
                        IconButton(onClick = { showLogsDialog = true }) {
                            Icon(Icons.Outlined.Terminal, Strings.logs)
                        }
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
            // ========== 构建模式选择 ==========
            BuildModeSelector(
                selectedMode = buildMode,
                onModeSelected = { buildMode = it },
                linuxState = linuxState,
                onSetupLinux = onNavigateToLinuxEnv
            )


            // ========== 示例项目 ==========
            SampleProjectsCard(
                onSelectSample = { sample ->
                    scope.launch {
                        val result = SampleProjectManager.extractSampleProject(context, sample.id)
                        result.onSuccess { path ->
                            projectPath = path
                            projectName = sample.name
                            isDetecting = true
                            detectionResult = ProjectDetector.detectProject(path)
                            isDetecting = false
                        }
                    }
                }
            )

            // ========== 选择项目 ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Folder,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            Strings.selectProject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (projectPath == null) {
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.FolderOpen, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.selectProjectFolder)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            Strings.selectProjectHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Folder,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        projectName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        projectPath ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = {
                                    projectPath = null
                                    detectionResult = null
                                    importBuilder.reset()
                                    nodeBuilder.reset()
                                }) {
                                    Icon(Icons.Default.Close, Strings.remove)
                                }
                            }
                        }
                    }
                }
            }
            
            // ========== 项目检测结果 ==========
            AnimatedVisibility(visible = isDetecting || detectionResult != null) {
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Analytics,
                                null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Strings.projectAnalysis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            if (isDetecting) {
                                Spacer(modifier = Modifier.width(12.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        
                        if (detectionResult != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 框架信息
                            DetectionInfoRow(
                                icon = Icons.Outlined.Code,
                                label = Strings.frameworkLabel,
                                value = getFrameworkDisplayName(detectionResult!!.framework),
                                valueColor = getFrameworkColor(detectionResult!!.framework)
                            )
                            
                            if (detectionResult!!.frameworkVersion != null) {
                                DetectionInfoRow(
                                    icon = Icons.Outlined.Tag,
                                    label = Strings.versionLabel,
                                    value = detectionResult!!.frameworkVersion!!
                                )
                            }
                            
                            DetectionInfoRow(
                                icon = Icons.Outlined.Inventory,
                                label = Strings.packageManagerLabel,
                                value = detectionResult!!.packageManager.name
                            )
                            
                            if (detectionResult!!.hasTypeScript) {
                                DetectionInfoRow(
                                    icon = Icons.Outlined.Code,
                                    label = "TypeScript",
                                    value = Strings.enabled,
                                    valueColor = Color(0xFF3178C6)
                                )
                            }
                            
                            // 依赖统计
                            val totalDeps = detectionResult!!.dependencies.size + 
                                           detectionResult!!.devDependencies.size
                            if (totalDeps > 0) {
                                DetectionInfoRow(
                                    icon = Icons.Outlined.Extension,
                                    label = Strings.dependencyCountLabel,
                                    value = Strings.dependencyCountValue.format(totalDeps)
                                )
                            }
                            
                            // 输出目录
                            DetectionInfoRow(
                                icon = Icons.Outlined.FolderOpen,
                                label = Strings.outputDirLabel,
                                value = File(detectionResult!!.outputDir).name
                            )

                            
                            // 问题和建议
                            if (detectionResult!!.issues.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                detectionResult!!.issues.forEach { issue ->
                                    IssueItem(issue)
                                }
                            }
                            
                            if (detectionResult!!.suggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                detectionResult!!.suggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            suggestion,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // ========== 应用配置 ==========
            AnimatedVisibility(visible = detectionResult != null && 
                detectionResult?.issues?.none { it.severity == IssueSeverity.ERROR } == true) {
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Settings,
                                null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Strings.appConfig,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // App名称（带随机按钮）
                        AppNameTextFieldSimple(
                            value = projectName,
                            onValueChange = { projectName = it }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // App图标
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                Strings.labelIcon,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconPickerWithLibrary(
                                iconUri = appIcon,
                                onSelectFromGallery = { iconPickerLauncher.launch("image/*") },
                                onSelectFromLibrary = { path -> 
                                    appIcon = Uri.parse(path)
                                }
                            )
                        }
                    }
                }
            }
            
            // ========== 构建状态 ==========
            AnimatedVisibility(visible = currentBuildState !is BuildState.Idle) {
                BuildStatusCard(currentBuildState, currentLogs.size) {
                    showLogsDialog = true
                }
            }
            
            // ========== 操作按钮 ==========
            Spacer(modifier = Modifier.height(8.dp))
            
            // 编辑模式：显示保存按钮（仅更新名称和图标）
            if (isEditMode && currentBuildState is BuildState.Idle) {
                Button(
                    onClick = {
                        // 传递 null 作为 outputPath，表示不更新文件
                        onCreated(
                            projectName,
                            "", // Empty字符串表示不更新文件
                            appIcon,
                            existingApp?.let { FrontendFramework.UNKNOWN } ?: FrontendFramework.UNKNOWN
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = projectName.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.btnSave)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            when (val state = currentBuildState) {
                is BuildState.Idle -> {
                    if (buildMode == BuildMode.IMPORT_DIST) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = importBuilder.importProject(projectPath!!)
                                    result.onSuccess { importResult ->
                                        onCreated(
                                            projectName,
                                            importResult.outputPath,
                                            appIcon,
                                            importResult.framework
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canImport
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEditMode) Strings.reimportProject else Strings.importProject)
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = nodeBuilder.buildProject(projectPath!!)
                                    result.onSuccess { buildResult ->
                                        onCreated(
                                            projectName,
                                            buildResult.outputPath,
                                            appIcon,
                                            buildResult.framework
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canBuild
                        ) {
                            Icon(Icons.Default.Build, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEditMode) Strings.rebuildProject else Strings.buildProject)
                        }
                    }
                }
                is BuildState.Success -> {
                    Button(
                        onClick = {
                            onCreated(
                                projectName,
                                state.outputPath,
                                appIcon,
                                detectionResult!!.framework
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEditMode) Strings.btnSave else Strings.btnCreate)
                    }
                }
                is BuildState.Error -> {
                    Column {
                        Button(
                            onClick = { 
                                importBuilder.reset()
                                nodeBuilder.reset()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.btnRetry)
                        }
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = { 
                            importBuilder.reset()
                            nodeBuilder.reset()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.btnCancel)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Log对话框
    if (showLogsDialog) {
        BuildLogsDialog(
            logs = currentLogs,
            onDismiss = { showLogsDialog = false }
        )
    }
}


/**
 * 构建模式选择器
 */
@Composable
private fun BuildModeSelector(
    selectedMode: BuildMode,
    onModeSelected: (BuildMode) -> Unit,
    linuxState: EnvironmentState,
    onSetupLinux: () -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Rocket,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        Strings.importFrontendProject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        Strings.supportVueReactVite,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 使用说明
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        Strings.usageSteps,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        Strings.usageStepsContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Built-in构建引擎说明
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Strings.builtInEngineReady,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 检测信息行
 */
@Composable
private fun DetectionInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * 问题项
 */
@Composable
private fun IssueItem(issue: ProjectIssue) {
    val (icon, color) = when (issue.severity) {
        IssueSeverity.ERROR -> Icons.Filled.Error to Color(0xFFE53935)
        IssueSeverity.WARNING -> Icons.Filled.Warning to Color(0xFFFFA726)
        IssueSeverity.INFO -> Icons.Filled.Info to Color(0xFF42A5F5)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                issue.message,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
            issue.suggestion?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 构建状态卡片
 */
@Composable
private fun BuildStatusCard(
    state: BuildState,
    logCount: Int,
    onViewLogs: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = when (state) {
            is BuildState.Success -> Color(0xFF4CAF50).copy(alpha = 0.1f)
            is BuildState.Error -> Color(0xFFE53935).copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state) {
                    is BuildState.Scanning -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.scanningProject)
                    }
                    is BuildState.Importing -> {
                        CircularProgressIndicator(
                            progress = state.progress,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(Strings.importing)
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is BuildState.CheckingEnvironment -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.checkingEnv)
                    }
                    is BuildState.CopyingProject -> {
                        CircularProgressIndicator(
                            progress = state.progress,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(Strings.copyingProjectFiles)
                            Text(
                                "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is BuildState.InstallingDependencies -> {
                        CircularProgressIndicator(
                            progress = state.progress,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(Strings.installingDeps)
                            if (state.currentPackage.isNotEmpty()) {
                                Text(
                                    state.currentPackage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    is BuildState.BuildingProject -> {
                        CircularProgressIndicator(
                            progress = state.progress,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(Strings.building)
                            Text(
                                state.stage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is BuildState.ProcessingOutput -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.processingOutput)
                    }
                    is BuildState.Success -> {
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(Strings.completed, color = Color(0xFF4CAF50))
                            Text(
                                Strings.totalFiles.replace("%d", state.fileCount.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is BuildState.Error -> {
                        Icon(
                            Icons.Filled.Error,
                            null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Strings.failed, color = Color(0xFFE53935))
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    else -> {}
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(onClick = onViewLogs) {
                    Text("${Strings.logs} ($logCount)")
                }
            }
        }
    }
}


/**
 * 构建日志对话框
 */
@Composable
private fun BuildLogsDialog(
    logs: List<BuildLogEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.importLogs) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(logs) { entry ->
                    val color = when (entry.level) {
                        LogLevel.ERROR -> Color(0xFFE53935)
                        LogLevel.WARNING -> Color(0xFFFFA726)
                        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.close)
            }
        }
    )
}

/**
 * 获取框架显示名称
 */
private fun getFrameworkDisplayName(framework: FrontendFramework): String {
    return when (framework) {
        FrontendFramework.VUE -> "Vue.js"
        FrontendFramework.REACT -> "React"
        FrontendFramework.NEXT -> "Next.js"
        FrontendFramework.NUXT -> "Nuxt.js"
        FrontendFramework.ANGULAR -> "Angular"
        FrontendFramework.SVELTE -> "Svelte"
        FrontendFramework.VITE -> "Vite"
        FrontendFramework.UNKNOWN -> "静态网站"
    }
}



/**
 * 获取框架颜色
 */
private fun getFrameworkColor(framework: FrontendFramework): Color {
    return when (framework) {
        FrontendFramework.VUE -> Color(0xFF42B883)
        FrontendFramework.REACT -> Color(0xFF61DAFB)
        FrontendFramework.NEXT -> Color(0xFF000000)
        FrontendFramework.NUXT -> Color(0xFF00DC82)
        FrontendFramework.ANGULAR -> Color(0xFFDD0031)
        FrontendFramework.SVELTE -> Color(0xFFFF3E00)
        FrontendFramework.VITE -> Color(0xFF646CFF)
        FrontendFramework.UNKNOWN -> Color.Gray
    }
}

/**
 * 从 Uri 获取真实路径
 */
private fun getPathFromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        if (split.size >= 2) {
            val type = split[0]
            val path = split[1]
            when (type) {
                "primary" -> "/storage/emulated/0/$path"
                else -> "/storage/$type/$path"
            }
        } else {
            uri.path
        }
    } catch (e: Exception) {
        uri.path
    }
}
