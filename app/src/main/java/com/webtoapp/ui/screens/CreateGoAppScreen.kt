package com.webtoapp.ui.screens

import com.webtoapp.ui.theme.AppColors
import com.webtoapp.ui.components.PremiumButton
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.golang.GoRuntime
import com.webtoapp.core.golang.GoSampleManager
import com.webtoapp.data.model.GoAppConfig
import com.webtoapp.ui.components.TypedSampleProjectsCard
import com.webtoapp.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox

/**
 * 创建/编辑 Go 服务应用页面
 * 
 * 增强功能：
 * - 框架品牌化 Hero 区域（Gin=蓝, Fiber=紫, Echo=青, Chi=红）
 * - go.mod 信息面板（模块路径、Go 版本、依赖数量）
 * - 预编译二进制检测卡片（文件名、大小、ELF 检查）
 * - 目标架构可视化选择器（ARM64, ARM, x86_64）
 * - 静态文件目录配置
 * - 健康检查端点配置
 * - 依赖列表面板
 * - 框架特定提示
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGoAppScreen(
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    existingAppId: Long = 0L,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        goAppConfig: GoAppConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isEdit = existingAppId > 0L
    
    // App 信息
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    
    // Go 配置
    var binaryName by remember { mutableStateOf("") }
    var staticDir by remember { mutableStateOf("") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }
    
    // 项目检测
    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    
    // 增强：go.mod 信息
    var goModulePath by remember { mutableStateOf<String?>(null) }
    var goVersion by remember { mutableStateOf<String?>(null) }
    var goDeps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showAllDeps by remember { mutableStateOf(false) }
    
    // 增强：二进制检测
    var binarySize by remember { mutableStateOf<Long?>(null) }
    var binaryDetected by remember { mutableStateOf(false) }
    
    // 增强：目标架构
    var targetArch by remember { mutableStateOf("arm64") }
    
    // 增强：健康检查
    var healthCheckEndpoint by remember { mutableStateOf("/health") }
    
    // 状态
    var isCreating by remember { mutableStateOf(false) }
    var creationPhase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 编辑模式：加载已有数据
    LaunchedEffect(existingAppId) {
        if (existingAppId > 0L) {
            val existingApp = webAppRepository.getWebAppById(existingAppId).first()
            existingApp?.let { app ->
                appName = app.name
                app.iconPath?.let { appIcon = android.net.Uri.parse(it) }
                app.goAppConfig?.let { config ->
                    binaryName = config.binaryName
                    staticDir = config.staticDir
                    landscapeMode = config.landscapeMode
                    envVars = config.envVars.toMutableMap()
                    detectedFramework = config.framework
                    projectId = config.projectId
                    selectedProjectDir = config.projectName
                }
            }
        }
    }
    
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { treeUri ->
            scope.launch {
                isCreating = true
                creationPhase = Strings.frameworkDetected
                errorMessage = null
                
                try {
                    withContext(Dispatchers.IO) {
                        val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                        val path = docId.substringAfter(":")
                        val storageRoot = if (docId.startsWith("primary:")) {
                            android.os.Environment.getExternalStorageDirectory().absolutePath
                        } else {
                            "/storage/${docId.substringBefore(":")}"
                        }
                        val projectPath = "$storageRoot/$path"
                        val projectDir = File(projectPath)
                        
                        if (!projectDir.exists()) {
                            errorMessage = Strings.dirNotExists
                            isCreating = false
                            return@withContext
                        }
                        
                        selectedProjectDir = projectPath
                        
                        val runtime = GoRuntime(context)
                        val framework = runtime.detectFramework(projectDir)
                        detectedFramework = framework
                        
                        // 检测预编译二进制
                        val detectedBinary = runtime.detectBinary(projectDir)
                        if (detectedBinary != null) {
                            binaryName = detectedBinary
                            binaryDetected = true
                            // 获取文件大小
                            val searchDirs = listOf(projectDir, File(projectDir, "bin"), File(projectDir, "build"))
                            for (dir in searchDirs) {
                                val binFile = File(dir, detectedBinary)
                                if (binFile.exists()) {
                                    binarySize = binFile.length()
                                    break
                                }
                            }
                        }
                        
                        // 检测静态文件目录
                        val detectedStaticDir = runtime.detectStaticDir(projectDir)
                        if (detectedStaticDir.isNotEmpty()) {
                            staticDir = detectedStaticDir
                        }
                        
                        // 增强：解析 go.mod
                        val goMod = File(projectDir, "go.mod")
                        if (goMod.exists()) {
                            try {
                                val content = goMod.readText()
                                val lines = content.lines()
                                
                                // 模块路径
                                lines.firstOrNull { it.startsWith("module ") }?.let { line ->
                                    goModulePath = line.substringAfter("module ").trim()
                                    if (appName.isBlank()) appName = goModulePath!!.substringAfterLast("/")
                                }
                                
                                // Go 版本
                                lines.firstOrNull { it.startsWith("go ") }?.let { line ->
                                    goVersion = line.substringAfter("go ").trim()
                                }
                                
                                // 解析直接依赖
                                var inRequire = false
                                val deps = mutableListOf<Pair<String, String>>()
                                for (line in lines) {
                                    val trimmed = line.trim()
                                    if (trimmed == "require (") {
                                        inRequire = true
                                        continue
                                    }
                                    if (trimmed == ")") {
                                        inRequire = false
                                        continue
                                    }
                                    if (inRequire && trimmed.isNotEmpty() && !trimmed.startsWith("//")) {
                                        if (!trimmed.contains("// indirect")) {
                                            val parts = trimmed.split(" ", limit = 2)
                                            if (parts.size == 2) {
                                                deps.add(parts[0].trim() to parts[1].trim())
                                            }
                                        }
                                    }
                                    // 单行 require
                                    if (trimmed.startsWith("require ") && !trimmed.contains("(")) {
                                        val reqParts = trimmed.removePrefix("require ").trim().split(" ", limit = 2)
                                        if (reqParts.size == 2) {
                                            deps.add(reqParts[0].trim() to reqParts[1].trim())
                                        }
                                    }
                                }
                                goDeps = deps
                            } catch (e: Exception) { android.util.Log.w("CreateGoApp", "Failed to parse go.mod", e) }
                        }
                        
                        // 复制项目文件
                        creationPhase = Strings.copyingProjectFiles
                        val newProjectId = java.util.UUID.randomUUID().toString()
                        runtime.createProject(newProjectId, projectDir)
                        projectId = newProjectId
                        creationPhase = Strings.goProjectReady
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: Strings.projectImportFailed
                } finally {
                    isCreating = false
                }
            }
        }
    }
    
    val canCreate = projectId != null
    
    // 获取框架品牌色
    val frameworkColor = remember(detectedFramework) {
        when (detectedFramework?.lowercase()) {
            "gin" -> Color(0xFF0090FF)
            "fiber" -> Color(0xFF8B5CF6)
            "echo" -> Color(0xFF00BCD4)
            "chi" -> AppColors.Error
            "net_http" -> Color(0xFF00ADD8)
            else -> Color(0xFF00ADD8) // Go 默认蓝
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.createGoApp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            projectId?.let { pid ->
                                onCreated(
                                    appName.ifBlank { "Go Service" },
                                    GoAppConfig(
                                        projectId = pid,
                                        projectName = appName.ifBlank { "Go Service" },
                                        framework = detectedFramework ?: "raw",
                                        binaryName = binaryName,
                                        envVars = envVars,
                                        staticDir = staticDir,
                                        hasBuildFromSource = binaryName.isEmpty(),
                                        landscapeMode = landscapeMode
                                    ),
                                    appIcon, "AURORA"
                                )
                            }
                        },
                        enabled = canCreate && !isCreating
                    ) { Text(if (isEdit) Strings.btnSave else Strings.btnCreate) }
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
            // ========== 1. 框架品牌化 Hero 区域 ==========
            GoHeroSection(
                detectedFramework = detectedFramework,
                frameworkColor = frameworkColor,
                goVersion = goVersion
            )
            
            // ========== 示例项目 ==========
            if (selectedProjectDir == null) {
                TypedSampleProjectsCard(
                    title = Strings.sampleProjects,
                    subtitle = Strings.sampleGoSubtitle,
                    samples = remember { GoSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = GoSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                selectedProjectDir = path
                                isCreating = true
                                creationPhase = Strings.frameworkDetected
                                try {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val projectDir = java.io.File(path)
                                        val runtime = GoRuntime(context)
                                        val framework = runtime.detectFramework(projectDir)
                                        detectedFramework = framework
                                        val detectedBinary = runtime.detectBinary(projectDir)
                                        if (detectedBinary != null) { binaryName = detectedBinary; binaryDetected = true }
                                        val detectedStaticDir = runtime.detectStaticDir(projectDir)
                                        if (detectedStaticDir.isNotEmpty()) staticDir = detectedStaticDir
                                        appName = sample.name
                                        creationPhase = Strings.copyingProjectFiles
                                        val newProjectId = java.util.UUID.randomUUID().toString()
                                        runtime.createProject(newProjectId, projectDir)
                                        projectId = newProjectId
                                        creationPhase = Strings.goProjectReady
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isCreating = false
                                }
                            }
                        }
                    }
                )
            }
            
            // ========== 2. 基本配置 ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.njsBasicConfig, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text(Strings.labelAppName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Strings.njsLandscapeMode)
                        PremiumSwitch(checked = landscapeMode, onCheckedChange = { landscapeMode = it })
                    }
                }
            }
            
            // ========== 3. 图标选择 ==========
            RuntimeIconPickerCard(
                appIcon = appIcon,
                onSelectIcon = { iconPickerLauncher.launch("image/*") }
            )
            
            // ========== 4. 项目选择 ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.goSelectProject, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.goSupportedFrameworks,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (selectedProjectDir != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = frameworkColor.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = frameworkColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Strings.goProjectReady, style = MaterialTheme.typography.bodyMedium, color = frameworkColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selectedProjectDir!!.substringAfterLast("/"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (detectedFramework != null && detectedFramework != "raw") {
                                    Text(
                                        "${Strings.frameworkDetected}: $detectedFramework",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (binaryName.isNotEmpty()) {
                                    Text(
                                        "${Strings.goSelectBinary}: $binaryName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    PremiumButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        enabled = !isCreating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.goSelectProject)
                    }
                }
            }
            
            // ========== 以下卡片仅在项目选择后显示 ==========
            if (projectId != null) {
                
                // ========== 5. Go 模块信息面板 ==========
                if (goModulePath != null) {
                    GoModuleInfoCard(
                        modulePath = goModulePath!!,
                        goVersion = goVersion,
                        depCount = goDeps.size,
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 6. 二进制检测卡片 ==========
                GoBinaryDetectionCard(
                    binaryDetected = binaryDetected,
                    binaryName = binaryName,
                    binarySize = binarySize,
                    onBinaryNameChange = { binaryName = it },
                    frameworkColor = frameworkColor
                )
                
                // ========== 7. 目标架构选择器 ==========
                GoTargetArchCard(
                    targetArch = targetArch,
                    onArchChange = { targetArch = it },
                    frameworkColor = frameworkColor
                )
                
                // ========== 8. 静态文件配置 ==========
                GoStaticFilesCard(
                    staticDir = staticDir,
                    onStaticDirChange = { staticDir = it }
                )
                
                // ========== 9. 健康检查端点 ==========
                GoHealthCheckCard(
                    endpoint = healthCheckEndpoint,
                    onEndpointChange = { healthCheckEndpoint = it }
                )
                
                // ========== 10. 依赖面板 ==========
                if (goDeps.isNotEmpty()) {
                    GoDepsCard(
                        deps = goDeps,
                        showAll = showAllDeps,
                        onToggleShowAll = { showAllDeps = !showAllDeps },
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 11. 框架提示 ==========
                GoFrameworkTipCard(framework = detectedFramework)
                
                // ========== 12. 环境变量 ==========
                RuntimeEnvVarsCard(
                    envVars = envVars,
                    newEnvKey = newEnvKey,
                    newEnvValue = newEnvValue,
                    onNewKeyChange = { newEnvKey = it },
                    onNewValueChange = { newEnvValue = it },
                    onAdd = {
                        if (newEnvKey.isNotBlank()) {
                            envVars = envVars.toMutableMap().apply { put(newEnvKey.trim(), newEnvValue.trim()) }
                            newEnvKey = ""; newEnvValue = ""
                        }
                    },
                    onRemove = { key -> envVars = envVars.toMutableMap().apply { remove(key) } }
                )
            }
            
            // 状态提示
            if (isCreating) {
                RuntimeLoadingCard(creationPhase)
            }
            
            errorMessage?.let { error ->
                RuntimeErrorCard(error = error, onDismiss = { errorMessage = null })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
        }
}

// ==================== 私有 Composable 组件 ====================

/**
 * Go 框架品牌化 Hero 区域
 */
@Composable
private fun GoHeroSection(
    detectedFramework: String?,
    frameworkColor: Color,
    goVersion: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(frameworkColor.copy(alpha = 0.15f), frameworkColor.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = frameworkColor.copy(alpha = 0.15f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Code, null, modifier = Modifier.size(32.dp), tint = frameworkColor)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        text = if (detectedFramework != null && detectedFramework != "raw" && detectedFramework != "net_http")
                            "${detectedFramework!!.replaceFirstChar { it.uppercase() }} ${Strings.goHeroTitle}"
                        else Strings.goHeroTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = frameworkColor
                    )
                    Text(
                        text = Strings.goHeroDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (goVersion != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = frameworkColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Go $goVersion",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = frameworkColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Go 模块信息卡片
 */
@Composable
private fun GoModuleInfoCard(
    modulePath: String,
    goVersion: String?,
    depCount: Int,
    frameworkColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(frameworkColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Info, null, tint = frameworkColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.goModuleInfo, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = frameworkColor.copy(alpha = 0.06f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Strings.goModulePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(modulePath, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                    }
                    if (goVersion != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(Strings.goVersion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(goVersion, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Strings.goDependencyCount, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$depCount", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = frameworkColor)
                    }
                }
            }
        }
    }
}

/**
 * 预编译二进制检测卡片
 */
@Composable
private fun GoBinaryDetectionCard(
    binaryDetected: Boolean,
    binaryName: String,
    binarySize: Long?,
    onBinaryNameChange: (String) -> Unit,
    frameworkColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Memory, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.goBinaryDetection, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (binaryDetected) frameworkColor.copy(alpha = 0.06f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (binaryDetected) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        null,
                        tint = if (binaryDetected) frameworkColor else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            if (binaryDetected) Strings.goBinaryFound else Strings.goBinaryNotFound,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (binaryDetected) frameworkColor else MaterialTheme.colorScheme.error
                        )
                        if (binaryDetected && binarySize != null) {
                            Text(
                                "${Strings.goBinarySize}: ${formatFileSize(binarySize)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            PremiumTextField(
                value = binaryName,
                onValueChange = onBinaryNameChange,
                label = { Text(Strings.goSelectBinary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * 目标架构选择器
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GoTargetArchCard(
    targetArch: String,
    onArchChange: (String) -> Unit,
    frameworkColor: Color
) {
    val architectures = listOf(
        "arm64" to "ARM64 (aarch64)",
        "arm" to "ARM (armv7a)",
        "x86_64" to "x86_64"
    )
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.DeveloperBoard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.goTargetArch, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                architectures.forEach { (arch, label) ->
                    PremiumFilterChip(
                        selected = targetArch == arch,
                        onClick = { onArchChange(arch) },
                        label = { Text(label, fontFamily = FontFamily.Monospace) },
                        leadingIcon = if (targetArch == arch) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * 静态文件目录配置卡片
 */
@Composable
private fun GoStaticFilesCard(
    staticDir: String,
    onStaticDirChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.goStaticFiles, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                Strings.goStaticFilesHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            PremiumTextField(
                value = staticDir,
                onValueChange = onStaticDirChange,
                label = { Text(Strings.goStaticFiles) },
                placeholder = { Text("static/") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * 健康检查端点卡片
 */
@Composable
private fun GoHealthCheckCard(
    endpoint: String,
    onEndpointChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.MonitorHeart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.goHealthCheck, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            PremiumTextField(
                value = endpoint,
                onValueChange = onEndpointChange,
                label = { Text(Strings.goHealthCheckEndpoint) },
                placeholder = { Text("/health") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * 依赖面板
 */
@Composable
private fun GoDepsCard(
    deps: List<Pair<String, String>>,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    frameworkColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(frameworkColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Extension, null, tint = frameworkColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.goDirectDeps, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = frameworkColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${deps.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = frameworkColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            val visibleDeps = if (showAll) deps else deps.take(6)
            visibleDeps.forEach { (name, version) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        version,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (deps.size > 6) {
                TextButton(onClick = onToggleShowAll, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showAll) Strings.close else "${Strings.more} (${deps.size - 6})")
                }
            }
        }
    }
}

/**
 * 框架特定提示
 */
@Composable
private fun GoFrameworkTipCard(framework: String?) {
    data class Tip(val tip: String, val color: Color)
    
    val tipData = when (framework?.lowercase()) {
        "gin" -> Tip("Gin: GIN_MODE auto-set to release. Router optimized for production.", Color(0xFF0090FF))
        "fiber" -> Tip("Fiber: Prefork disabled for Android. Fasthttp engine active.", Color(0xFF8B5CF6))
        "echo" -> Tip("Echo: Logger & recover middleware auto-configured.", Color(0xFF00BCD4))
        "chi" -> Tip("Chi: Lightweight stdlib-compatible router. Middleware stack ready.", AppColors.Error)
        "net_http" -> Tip("net/http: Standard library server. Graceful shutdown enabled.", Color(0xFF00ADD8))
        else -> null
    }
    
    if (tipData != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = tipData.color.copy(alpha = 0.08f)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Lightbulb, null, tint = tipData.color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        Strings.phpFrameworkTip,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = tipData.color
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        tipData.tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
