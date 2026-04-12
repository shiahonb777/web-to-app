package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.webtoapp.core.linux.HtmlProjectOptimizer
import com.webtoapp.core.linux.NativeNodeEngine
import com.webtoapp.core.nodejs.NodeDependencyManager
import com.webtoapp.core.nodejs.NodeRuntime
import com.webtoapp.core.nodejs.NodeSampleManager
import com.webtoapp.data.model.NodeJsBuildMode
import com.webtoapp.data.model.NodeJsConfig
import com.webtoapp.ui.components.*
import com.webtoapp.ui.components.TypedSampleProjectsCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox

/**
 * 创建 Node.js 应用页面
 * 
 * 增强功能：
 * - 框架品牌化 Hero 区域（Express=绿, Fastify=黑, Koa=灰, NestJS=红, Next.js=黑, Nuxt=绿）
 * - package.json 脚本面板（可选启动脚本）
 * - 依赖可视化（生产依赖 / 开发依赖分组）
 * - TypeScript 检测指示器
 * - 包管理器检测（npm/yarn/pnpm）
 * - 端口检测与自定义端口
 * - 项目信息摘要面板
 * - 框架特定提示
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateNodeJsAppScreen(
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    existingAppId: Long = 0L,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        nodejsConfig: NodeJsConfig,
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
    
    // Node.js 配置
    var buildMode by remember { mutableStateOf(NodeJsBuildMode.API_BACKEND) }
    var entryFile by remember { mutableStateOf("index.js") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }
    
    // 项目检测
    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var detectedEntryFile by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    
    // 增强：package.json 信息
    var packageName by remember { mutableStateOf<String?>(null) }
    var packageVersion by remember { mutableStateOf<String?>(null) }
    var packageDescription by remember { mutableStateOf<String?>(null) }
    var npmScripts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedStartScript by remember { mutableStateOf<String?>(null) }
    var dependencies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var devDependencies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showAllDeps by remember { mutableStateOf(false) }
    var showAllDevDeps by remember { mutableStateOf(false) }
    
    // 增强：TypeScript 检测
    var hasTypeScript by remember { mutableStateOf(false) }
    
    // TypeScript 预编译（Linux 环境）
    var enableTsPreCompile by remember { mutableStateOf(false) }
    val esbuildAvailable = remember { NativeNodeEngine.isAvailable(context) }
    
    // 增强：包管理器检测
    var packageManager by remember { mutableStateOf("npm") }
    
    // 增强：端口检测
    var detectedPort by remember { mutableStateOf<Int?>(null) }
    var customPort by remember { mutableStateOf("") }
    
    // 增强：Node 版本
    var nodeEngineVersion by remember { mutableStateOf<String?>(null) }
    
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
                app.nodejsConfig?.let { config ->
                    buildMode = config.buildMode
                    entryFile = config.entryFile
                    landscapeMode = config.landscapeMode
                    envVars = config.envVars.toMutableMap()
                    detectedFramework = config.framework
                    projectId = config.projectId
                    selectedProjectDir = config.projectName
                    packageName = config.projectName
                    if (config.serverPort > 0) {
                        detectedPort = config.serverPort
                        customPort = config.serverPort.toString()
                    }
                    if (config.nodeVersion.isNotEmpty()) {
                        nodeEngineVersion = config.nodeVersion
                    }
                }
            }
        }
    }
    
    // 依赖下载状态
    val downloadState by NodeDependencyManager.downloadState.collectAsStateWithLifecycle()
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    // 框架品牌色
    val frameworkColor = remember(detectedFramework) {
        when (detectedFramework) {
            "Express" -> Color(0xFF259D3D) // Express 绿
            "Fastify" -> Color(0xFF000000) // Fastify 黑
            "Koa" -> Color(0xFF33333D) // Koa 灰黑
            "NestJS" -> Color(0xFFE0234E) // NestJS 红
            "Hapi" -> Color(0xFFFF7B00) // Hapi 橙
            "Next.js" -> Color(0xFF000000) // Next.js 黑
            "Nuxt.js" -> Color(0xFF00DC82) // Nuxt 绿
            else -> Color(0xFF339933) // Node.js 默认绿
        }
    }
    
    // 文件选择器
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { treeUri ->
            scope.launch {
                isCreating = true
                creationPhase = Strings.njsProjectDetected
                errorMessage = null
                
                try {
                    withContext(Dispatchers.IO) {
                        // 解析 SAF URI 到文件路径
                        val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                        val path = docId.substringAfter(":")
                        val storageRoot = if (docId.startsWith("primary:")) {
                            android.os.Environment.getExternalStorageDirectory().absolutePath
                        } else {
                            "/storage/${docId.substringBefore(":")}"
                        }
                        val projectPath = "$storageRoot/$path"
                        val projectDir = File(projectPath)
                        
                        if (!projectDir.exists() || !File(projectDir, "package.json").exists()) {
                            errorMessage = context.getString(com.webtoapp.R.string.njs_package_json_not_found)
                            isCreating = false
                            return@withContext
                        }
                        
                        selectedProjectDir = projectPath
                        
                        // 检测项目信息
                        val runtime = NodeRuntime(context)
                        val detected = runtime.detectEntryFile(projectDir)
                        if (detected != null) {
                            detectedEntryFile = detected
                            entryFile = detected
                        }
                        
                        // 增强：检测包管理器
                        packageManager = when {
                            File(projectDir, "pnpm-lock.yaml").exists() -> "pnpm"
                            File(projectDir, "yarn.lock").exists() -> "yarn"
                            File(projectDir, "bun.lockb").exists() -> "bun"
                            else -> "npm"
                        }
                        
                        // 增强：检测 TypeScript
                        hasTypeScript = File(projectDir, "tsconfig.json").exists()
                        
                        // 读取 package.json 获取项目名称和框架
                        val packageJson = File(projectDir, "package.json")
                        if (packageJson.exists()) {
                            try {
                                val content = packageJson.readText()
                                val gson = com.google.gson.Gson()
                                val json = gson.fromJson(content, com.google.gson.JsonObject::class.java)
                                
                                // 项目名称
                                json.get("name")?.asString?.let { name ->
                                    packageName = name
                                    if (appName.isBlank()) appName = name
                                }
                                
                                // 项目版本
                                json.get("version")?.asString?.let { packageVersion = it }
                                
                                // 项目描述
                                json.get("description")?.asString?.let { packageDescription = it }
                                
                                // 增强：NPM 脚本
                                json.getAsJsonObject("scripts")?.let { scripts ->
                                    val scriptMap = mutableMapOf<String, String>()
                                    scripts.keySet().forEach { key ->
                                        scriptMap[key] = scripts.get(key).asString
                                    }
                                    npmScripts = scriptMap
                                    // 自动选择启动脚本
                                    selectedStartScript = when {
                                        "start" in scriptMap -> "start"
                                        "dev" in scriptMap -> "dev"
                                        "serve" in scriptMap -> "serve"
                                        else -> scriptMap.keys.firstOrNull()
                                    }
                                }
                                
                                // 增强：Node 引擎版本
                                json.getAsJsonObject("engines")?.get("node")?.asString?.let {
                                    nodeEngineVersion = it
                                }
                                
                                // 依赖
                                val deps = json.getAsJsonObject("dependencies")
                                val devDeps = json.getAsJsonObject("devDependencies")
                                val allDeps = mutableSetOf<String>()
                                
                                // 增强：保存完整依赖映射
                                deps?.let { d ->
                                    val depMap = mutableMapOf<String, String>()
                                    d.keySet().forEach { key -> depMap[key] = d.get(key).asString }
                                    dependencies = depMap
                                    allDeps.addAll(d.keySet())
                                }
                                devDeps?.let { dd ->
                                    val devDepMap = mutableMapOf<String, String>()
                                    dd.keySet().forEach { key -> devDepMap[key] = dd.get(key).asString }
                                    devDependencies = devDepMap
                                    allDeps.addAll(dd.keySet())
                                }
                                
                                // 增强：TypeScript 从依赖检测
                                if (!hasTypeScript && ("typescript" in allDeps || "ts-node" in allDeps)) {
                                    hasTypeScript = true
                                }
                                
                                detectedFramework = when {
                                    "express" in allDeps -> "Express"
                                    "fastify" in allDeps -> "Fastify"
                                    "koa" in allDeps -> "Koa"
                                    "@nestjs/core" in allDeps -> "NestJS"
                                    "@hapi/hapi" in allDeps -> "Hapi"
                                    "next" in allDeps -> "Next.js"
                                    "nuxt" in allDeps -> "Nuxt.js"
                                    else -> null
                                }
                                
                                // 自动推断构建模式
                                buildMode = when {
                                    "next" in allDeps || "nuxt" in allDeps -> NodeJsBuildMode.FULLSTACK
                                    "express" in allDeps || "fastify" in allDeps || "koa" in allDeps ||
                                    "@nestjs/core" in allDeps || "@hapi/hapi" in allDeps -> NodeJsBuildMode.API_BACKEND
                                    else -> NodeJsBuildMode.STATIC
                                }
                                
                                // 增强：端口检测 - 从脚本或环境变量推断
                                npmScripts["start"]?.let { startScript ->
                                    val portMatch = Regex("(?:PORT=|--port[= ])(\\d{4,5})").find(startScript)
                                    portMatch?.groupValues?.get(1)?.toIntOrNull()?.let { detectedPort = it }
                                }
                                if (detectedPort == null) {
                                    // 尝试从 .env 读取
                                    val envFile = File(projectDir, ".env")
                                    if (envFile.exists()) {
                                        envFile.readLines().firstOrNull { it.trimStart().startsWith("PORT=") }
                                            ?.substringAfter("=")?.trim()?.toIntOrNull()?.let { detectedPort = it }
                                    }
                                }
                                if (detectedPort == null) {
                                    // 从入口文件尝试检测 listen 端口
                                    val entryF = File(projectDir, entryFile)
                                    if (entryF.exists()) {
                                        val entryContent = entryF.readText()
                                        val listenMatch = Regex("\\.listen\\((\\d{4,5})").find(entryContent)
                                        listenMatch?.groupValues?.get(1)?.toIntOrNull()?.let { detectedPort = it }
                                    }
                                }
                                
                                // 检测 .env.example 获取环境变量
                                val envExample = File(projectDir, ".env.example")
                                if (envExample.exists()) {
                                    envExample.readLines().forEach { line ->
                                        val trimmed = line.trim()
                                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                                            val key = trimmed.substringBefore("=").trim()
                                            val value = trimmed.substringAfter("=").trim()
                                            if (key.isNotEmpty()) {
                                                envVars = envVars.toMutableMap().apply { put(key, value) }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // 解析失败不影响主流程
                            }
                        }
                        
                        // 检查 Node.js 依赖并下载
                        if (buildMode != NodeJsBuildMode.STATIC && !NodeDependencyManager.isNodeReady(context)) {
                            showDownloadDialog = true
                            val success = NodeDependencyManager.downloadNodeRuntime(context)
                            showDownloadDialog = false
                            if (!success) {
                                errorMessage = Strings.njsDownloadFailed
                                isCreating = false
                                return@withContext
                            }
                        }
                        
                        // 复制项目文件到内部存储
                        creationPhase = "正在复制项目文件..."
                        val newProjectId = java.util.UUID.randomUUID().toString()
                        runtime.createProject(newProjectId, projectDir)
                        projectId = newProjectId
                        creationPhase = Strings.njsProjectReady
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "项目导入失败"
                } finally {
                    isCreating = false
                }
            }
        }
    }
    
    // 判断是否可以创建
    val canCreate = projectId != null
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.njsCreateTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            projectId?.let { pid ->
                                val finalPort = customPort.toIntOrNull() ?: detectedPort
                                
                                if (enableTsPreCompile && hasTypeScript && esbuildAvailable) {
                                    // 先预编译 TypeScript 再创建
                                    isCreating = true
                                    creationPhase = Strings.tsPreCompile
                                    scope.launch {
                                        val projectDir = File(context.filesDir, "nodejs_projects/$pid")
                                        if (projectDir.exists()) {
                                            HtmlProjectOptimizer.optimizeDirectory(
                                                context = context,
                                                projectDir = projectDir.absolutePath
                                            )
                                        }
                                        isCreating = false
                                        onCreated(
                                            appName.ifBlank { "Node.js App" },
                                            NodeJsConfig(
                                                projectId = pid,
                                                projectName = appName.ifBlank { "Node.js App" },
                                                framework = detectedFramework ?: "",
                                                buildMode = buildMode,
                                                entryFile = entryFile,
                                                serverPort = finalPort ?: 3000,
                                                envVars = envVars,
                                                hasNodeModules = dependencies.isNotEmpty(),
                                                nodeVersion = nodeEngineVersion ?: "",
                                                landscapeMode = landscapeMode
                                            ),
                                            appIcon,
                                            "AURORA"
                                        )
                                    }
                                } else {
                                    onCreated(
                                        appName.ifBlank { "Node.js App" },
                                        NodeJsConfig(
                                            projectId = pid,
                                            projectName = appName.ifBlank { "Node.js App" },
                                            framework = detectedFramework ?: "",
                                            buildMode = buildMode,
                                            entryFile = entryFile,
                                            serverPort = finalPort ?: 3000,
                                            envVars = envVars,
                                            hasNodeModules = dependencies.isNotEmpty(),
                                            nodeVersion = nodeEngineVersion ?: "",
                                            landscapeMode = landscapeMode
                                        ),
                                        appIcon,
                                        "AURORA"
                                    )
                                }
                            }
                        },
                        enabled = canCreate && !isCreating
                    ) {
                        Text(if (isEdit) Strings.btnSave else Strings.btnCreate)
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
            // ========== 1. 框架品牌化 Hero 区域 ==========
            NodeJsHeroSection(
                detectedFramework = detectedFramework,
                frameworkColor = frameworkColor,
                hasTypeScript = hasTypeScript,
                packageManager = packageManager,
                nodeEngineVersion = nodeEngineVersion
            )
            
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
                    
                    // 应用名称
                    PremiumTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text(Strings.labelAppName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 横屏模式
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
                        Text(Strings.njsSelectProjectFolder, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.njsSelectProjectDesc,
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
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint = frameworkColor, modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Strings.njsProjectDetected,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = frameworkColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedProjectDir!!.substringAfterLast("/"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (detectedFramework != null) {
                                    Text(
                                        text = "${Strings.njsFramework}: $detectedFramework",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (detectedEntryFile != null) {
                                    Text(
                                        text = "${Strings.njsEntryFile}: $detectedEntryFile",
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
                        Text(Strings.njsSelectProjectFolder)
                    }
                }
            }
            
            // ========== 示例项目 ==========
            if (selectedProjectDir == null && !isCreating) {
                TypedSampleProjectsCard(
                    title = Strings.sampleProjects,
                    subtitle = Strings.sampleNodeSubtitle,
                    samples = remember { NodeSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = NodeSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                selectedProjectDir = path
                                isCreating = true
                                creationPhase = Strings.njsProjectDetected
                                try {
                                    withContext(Dispatchers.IO) {
                                        val projectDir = File(path)
                                        val runtime = NodeRuntime(context)
                                        
                                        // 检测入口文件
                                        val detected = runtime.detectEntryFile(projectDir)
                                        if (detected != null) {
                                            detectedEntryFile = detected
                                            entryFile = detected
                                        }
                                        
                                        // 检测包管理器
                                        packageManager = when {
                                            File(projectDir, "pnpm-lock.yaml").exists() -> "pnpm"
                                            File(projectDir, "yarn.lock").exists() -> "yarn"
                                            else -> "npm"
                                        }
                                        
                                        // 读取 package.json
                                        val packageJson = File(projectDir, "package.json")
                                        if (packageJson.exists()) {
                                            try {
                                                val content = packageJson.readText()
                                                val gson = com.google.gson.Gson()
                                                val json = gson.fromJson(content, com.google.gson.JsonObject::class.java)
                                                
                                                json.get("name")?.asString?.let { name ->
                                                    packageName = name
                                                }
                                                json.get("version")?.asString?.let { packageVersion = it }
                                                json.get("description")?.asString?.let { packageDescription = it }
                                                
                                                // NPM 脚本
                                                json.getAsJsonObject("scripts")?.let { scripts ->
                                                    val scriptMap = mutableMapOf<String, String>()
                                                    scripts.keySet().forEach { key ->
                                                        scriptMap[key] = scripts.get(key).asString
                                                    }
                                                    npmScripts = scriptMap
                                                    selectedStartScript = when {
                                                        "start" in scriptMap -> "start"
                                                        "dev" in scriptMap -> "dev"
                                                        else -> scriptMap.keys.firstOrNull()
                                                    }
                                                }
                                                
                                                // 依赖
                                                val deps = json.getAsJsonObject("dependencies")
                                                val allDeps = mutableSetOf<String>()
                                                deps?.let { d ->
                                                    val depMap = mutableMapOf<String, String>()
                                                    d.keySet().forEach { key -> depMap[key] = d.get(key).asString }
                                                    dependencies = depMap
                                                    allDeps.addAll(d.keySet())
                                                }
                                                
                                                // 检测框架
                                                detectedFramework = when {
                                                    "express" in allDeps -> "Express"
                                                    "fastify" in allDeps -> "Fastify"
                                                    "koa" in allDeps -> "Koa"
                                                    else -> null
                                                }
                                                
                                                buildMode = NodeJsBuildMode.API_BACKEND
                                            } catch (e: Exception) { android.util.Log.w("CreateNodeJsApp", "Failed to parse package.json", e) }
                                        }
                                        
                                        appName = sample.name
                                        
                                        // 检查 Node.js 依赖
                                        if (!NodeDependencyManager.isNodeReady(context)) {
                                            showDownloadDialog = true
                                            val success = NodeDependencyManager.downloadNodeRuntime(context)
                                            showDownloadDialog = false
                                            if (!success) {
                                                errorMessage = Strings.njsDownloadFailed
                                                isCreating = false
                                                return@withContext
                                            }
                                        }
                                        
                                        // 复制项目文件
                                        creationPhase = Strings.copyingProjectFiles
                                        val newProjectId = java.util.UUID.randomUUID().toString()
                                        runtime.createProject(newProjectId, projectDir)
                                        projectId = newProjectId
                                        creationPhase = Strings.njsProjectReady
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
            
            // ========== 以下卡片仅在项目选择后显示 ==========
            if (projectId != null) {
                
                // ========== 5. 项目信息摘要 ==========
                if (packageName != null) {
                    NodeJsProjectInfoCard(
                        packageName = packageName!!,
                        packageVersion = packageVersion,
                        packageDescription = packageDescription,
                        depCount = dependencies.size,
                        devDepCount = devDependencies.size,
                        hasTypeScript = hasTypeScript,
                        packageManager = packageManager,
                        detectedPort = detectedPort,
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 6. NPM 脚本面板 ==========
                if (npmScripts.isNotEmpty()) {
                    NodeJsScriptsCard(
                        scripts = npmScripts,
                        selectedScript = selectedStartScript,
                        onSelectScript = { selectedStartScript = it },
                        packageManager = packageManager,
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 7. 构建模式 ==========
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Outlined.Build, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(Strings.njsBuildMode, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        listOf(
                            Triple(NodeJsBuildMode.STATIC, Strings.njsModeStatic, Strings.njsModeStaticDesc),
                            Triple(NodeJsBuildMode.API_BACKEND, Strings.njsModeBackend, Strings.njsModeBackendDesc),
                            Triple(NodeJsBuildMode.FULLSTACK, Strings.njsModeFullstack, Strings.njsModeFullstackDesc)
                        ).forEach { (mode, label, desc) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (buildMode == mode) frameworkColor.copy(alpha = 0.08f) else Color.Transparent,
                                onClick = { buildMode = mode }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = buildMode == mode,
                                        onClick = { buildMode = mode },
                                        colors = RadioButtonDefaults.colors(selectedColor = frameworkColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (buildMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 入口文件（非静态模式）
                        if (buildMode != NodeJsBuildMode.STATIC) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PremiumTextField(
                                value = entryFile,
                                onValueChange = { entryFile = it },
                                label = { Text(Strings.njsEntryFile) },
                                placeholder = { Text(Strings.njsEntryFileHint) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    if (hasTypeScript) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF3178C6).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "TS",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF3178C6),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                
                // ========== 7.5 TypeScript 预编译（Linux 环境） ==========
                if (hasTypeScript && esbuildAvailable) {
                    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF3178C6).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Outlined.Speed, null, tint = Color(0xFF3178C6), modifier = Modifier.size(22.dp)) }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(Strings.tsPreCompile, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF3178C6).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "esbuild",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF3178C6),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(
                                        text = Strings.tsPreCompileHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PremiumSwitch(
                                    checked = enableTsPreCompile,
                                    onCheckedChange = { enableTsPreCompile = it }
                                )
                            }
                        }
                    }
                }
                
                // ========== 8. 端口配置 ==========
                if (buildMode != NodeJsBuildMode.STATIC) {
                    NodeJsPortCard(
                        detectedPort = detectedPort,
                        customPort = customPort,
                        onCustomPortChange = { customPort = it },
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 9. 依赖可视化 ==========
                if (dependencies.isNotEmpty() || devDependencies.isNotEmpty()) {
                    NodeJsDependenciesCard(
                        dependencies = dependencies,
                        devDependencies = devDependencies,
                        showAllDeps = showAllDeps,
                        onToggleDeps = { showAllDeps = !showAllDeps },
                        showAllDevDeps = showAllDevDeps,
                        onToggleDevDeps = { showAllDevDeps = !showAllDevDeps },
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 10. 环境变量 ==========
                if (buildMode != NodeJsBuildMode.STATIC) {
                    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(Strings.njsEnvVars, style = MaterialTheme.typography.titleMedium)
                                if (envVars.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = frameworkColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            "${envVars.size}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = frameworkColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 已有环境变量列表
                            envVars.forEach { (key, value) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = frameworkColor
                                        )
                                        Text(
                                            text = " = ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(weight = 1f, fill = true),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                            onClick = { envVars = envVars.toMutableMap().apply { remove(key) } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            
                            // 添加新环境变量
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PremiumTextField(
                                    value = newEnvKey,
                                    onValueChange = { newEnvKey = it },
                                    label = { Text("Key") },
                                    modifier = Modifier.weight(weight = 1f, fill = true),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                PremiumTextField(
                                    value = newEnvValue,
                                    onValueChange = { newEnvValue = it },
                                    label = { Text("Value") },
                                    modifier = Modifier.weight(weight = 1f, fill = true),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newEnvKey.isNotBlank()) {
                                            envVars = envVars.toMutableMap().apply {
                                                put(newEnvKey.trim(), newEnvValue.trim())
                                            }
                                            newEnvKey = ""
                                            newEnvValue = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, Strings.njsAddEnvVar)
                                }
                            }
                        }
                    }
                }
                
                // ========== 11. 框架特定提示 ==========
                if (detectedFramework != null) {
                    NodeJsFrameworkTipsCard(
                        framework = detectedFramework!!,
                        frameworkColor = frameworkColor
                    )
                }
            }
            
            // 状态提示
            if (isCreating) {
                RuntimeLoadingCard(creationPhase)
            }
            
            // 错误提示
            errorMessage?.let { error ->
                RuntimeErrorCard(error = error, onDismiss = { errorMessage = null })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Node.js 下载对话框
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(Strings.njsDownloadDeps) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    when (val state = downloadState) {
                        is NodeDependencyManager.DownloadState.Downloading -> {
                            Text(Strings.njsDownloading)
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${(state.bytesDownloaded / 1024 / 1024)}MB / ${(state.totalBytes / 1024 / 1024)}MB",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is NodeDependencyManager.DownloadState.Extracting -> {
                            Text("正在解压 ${state.fileName}...")
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        is NodeDependencyManager.DownloadState.Verifying -> {
                            Text("正在验证 ${state.fileName}...")
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        is NodeDependencyManager.DownloadState.Complete -> {
                            Text(Strings.njsDownloadComplete)
                        }
                        is NodeDependencyManager.DownloadState.Error -> {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {
                            Text(Strings.njsDownloading)
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
        }
}

// ==================== 私有 Composable 组件 ====================

/**
 * Node.js 框架品牌化 Hero 区域
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeJsHeroSection(
    detectedFramework: String?,
    frameworkColor: Color,
    hasTypeScript: Boolean,
    packageManager: String,
    nodeEngineVersion: String?
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
                        text = if (detectedFramework != null) "$detectedFramework ${Strings.njsHeroTitle}"
                        else Strings.njsHeroTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = frameworkColor
                    )
                    Text(
                        text = Strings.njsHeroDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Node 版本标签
                        nodeEngineVersion?.let { ver ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = frameworkColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Node $ver",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = frameworkColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        // TypeScript 标签
                        if (hasTypeScript) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF3178C6).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "TypeScript",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF3178C6),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // 包管理器标签
                        val pmColor = when (packageManager) {
                            "yarn" -> Color(0xFF2C8EBB)
                            "pnpm" -> Color(0xFFF69220)
                            "bun" -> Color(0xFFF9F1E1)
                            else -> Color(0xFFCB3837)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = pmColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = packageManager,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (packageManager == "bun") Color(0xFFB89B00) else pmColor,
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
 * 项目信息摘要卡片
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeJsProjectInfoCard(
    packageName: String,
    packageVersion: String?,
    packageDescription: String?,
    depCount: Int,
    devDepCount: Int,
    hasTypeScript: Boolean,
    packageManager: String,
    detectedPort: Int?,
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
                Text(Strings.njsProjectInfo, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = frameworkColor.copy(alpha = 0.06f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 项目名 @ 版本
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = frameworkColor
                        )
                        packageVersion?.let {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = frameworkColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "v$it",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = frameworkColor
                                )
                            }
                        }
                    }
                    
                    // 描述
                    packageDescription?.let { desc ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = frameworkColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // 统计标签行
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 依赖数
                        NjsInfoChip(
                            icon = Icons.Outlined.Inventory2,
                            label = "${Strings.njsDependencies}: $depCount",
                            color = frameworkColor
                        )
                        // 开发依赖数
                        if (devDepCount > 0) {
                            NjsInfoChip(
                                icon = Icons.Outlined.Build,
                                label = "${Strings.njsDevDependencies}: $devDepCount",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        // TypeScript
                        if (hasTypeScript) {
                            NjsInfoChip(
                                icon = Icons.Outlined.Code,
                                label = "TypeScript",
                                color = Color(0xFF3178C6)
                            )
                        }
                        // 包管理器
                        NjsInfoChip(
                            icon = Icons.Outlined.Archive,
                            label = "${Strings.njsPackageManager}: $packageManager",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        // 检测到端口
                        detectedPort?.let {
                            NjsInfoChip(
                                icon = Icons.Outlined.Lan,
                                label = "${Strings.njsDetectedPort}: $it",
                                color = Color(0xFFFF6B35)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 信息标签组件
 */
@Composable
private fun NjsInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * NPM 脚本面板
 */
@Composable
private fun NodeJsScriptsCard(
    scripts: Map<String, String>,
    selectedScript: String?,
    onSelectScript: (String) -> Unit,
    packageManager: String,
    frameworkColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(frameworkColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Terminal, null, tint = frameworkColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.njsScripts, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = frameworkColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        "${scripts.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = frameworkColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Strings.njsStartupScript,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            scripts.forEach { (name, command) ->
                val isSelected = name == selectedScript
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) frameworkColor.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    onClick = { onSelectScript(name) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectScript(name) },
                            modifier = Modifier.size(20.dp),
                            colors = RadioButtonDefaults.colors(selectedColor = frameworkColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) frameworkColor else MaterialTheme.colorScheme.onSurface
                                )
                                // 为常用脚本加上推荐标签
                                if (name == "start" || name == "dev") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFFC107).copy(alpha = 0.2f)
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp).size(14.dp),
                                            tint = Color(0xFFFF8F00)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "$packageManager run $name → $command",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 端口配置卡片
 */
@Composable
private fun NodeJsPortCard(
    detectedPort: Int?,
    customPort: String,
    onCustomPortChange: (String) -> Unit,
    frameworkColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(frameworkColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Lan, null, tint = frameworkColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.njsDetectedPort, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // 检测到的端口
            if (detectedPort != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = frameworkColor.copy(alpha = 0.06f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle, null,
                            tint = frameworkColor, modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${Strings.njsDetectedPort}: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$detectedPort",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = frameworkColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            
            // 自定义端口
            PremiumTextField(
                value = customPort,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                        onCustomPortChange(newValue)
                    }
                },
                label = { Text(Strings.njsPortOverride) },
                placeholder = { Text(detectedPort?.toString() ?: "3000") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    if (customPort.isBlank() && detectedPort != null) {
                        Text(
                            "${Strings.njsDetectedPort}: $detectedPort",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }
    }
}

/**
 * 依赖可视化卡片
 */
@Composable
private fun NodeJsDependenciesCard(
    dependencies: Map<String, String>,
    devDependencies: Map<String, String>,
    showAllDeps: Boolean,
    onToggleDeps: () -> Unit,
    showAllDevDeps: Boolean,
    onToggleDevDeps: () -> Unit,
    frameworkColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(frameworkColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Inventory2, null, tint = frameworkColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.njsDependencies, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // 生产依赖
            if (dependencies.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "dependencies",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = frameworkColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = frameworkColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${dependencies.size}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = frameworkColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                val visibleDeps = if (showAllDeps) dependencies.entries.toList()
                else dependencies.entries.take(5).toList()
                
                visibleDeps.forEach { (name, version) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(weight = 1f, fill = true),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = version,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (dependencies.size > 5) {
                    TextButton(onClick = onToggleDeps, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (showAllDeps) Strings.collapse else "${Strings.expandAll} (${dependencies.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = frameworkColor
                        )
                    }
                }
            }
            
            // 开发依赖
            if (devDependencies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "devDependencies",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${devDependencies.size}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                val visibleDevDeps = if (showAllDevDeps) devDependencies.entries.toList()
                else devDependencies.entries.take(3).toList()
                
                visibleDevDeps.forEach { (name, version) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(weight = 1f, fill = true),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = version,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                if (devDependencies.size > 3) {
                    TextButton(onClick = onToggleDevDeps, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (showAllDevDeps) Strings.collapse else "${Strings.expandAll} (${devDependencies.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 框架特定提示卡片
 */
@Composable
private fun NodeJsFrameworkTipsCard(
    framework: String,
    frameworkColor: Color
) {
    val tips = when (framework) {
        "Express" -> listOf(
            "[Tip] 确保 app.listen() 绑定到 0.0.0.0 而非 localhost",
            "[Package] 静态文件使用 express.static() 中间件",
            "[Config] 生产模式设置 NODE_ENV=production 以优化性能"
        )
        "Fastify" -> listOf(
            "[Tip] 使用 host: '0.0.0.0' 配置监听地址",
            "[Perf] Fastify 原生支持 JSON Schema 验证",
            "[Note] 推荐使用 @fastify/static 插件服务静态文件"
        )
        "Koa" -> listOf(
            "[Tip] Koa 需要显式安装路由中间件 (koa-router)",
            "[Config] 使用 koa-static 服务静态资源",
            "[Package] Koa 的洋葱模型适合复杂中间件编排"
        )
        "NestJS" -> listOf(
            "[Tip] NestJS 入口文件通常为 dist/main.js",
            "[Build] 确保先运行 nest build 生成 dist 目录",
            "[Config] 在 main.ts 中设置 app.listen(port, '0.0.0.0')"
        )
        "Next.js" -> listOf(
            "[Tip] Next.js 使用 next start 启动生产服务器",
            "[Package] 确保已运行 next build 生成 .next 目录",
            "[API] API 路由位于 pages/api 或 app/api 目录"
        )
        "Nuxt.js" -> listOf(
            "[Tip] Nuxt 3 使用 .output 目录作为生产输出",
            "[Config] 运行 nuxi build 生成服务端产物",
            "[API] Nitro 服务引擎自动处理 API 路由"
        )
        "Hapi" -> listOf(
            "[Tip] Hapi 使用 server.start() 启动服务",
            "[Config] 配置 host: '0.0.0.0' 以允许外部访问",
            "[Package] 使用 @hapi/inert 插件服务静态文件"
        )
        else -> listOf(
            "[Tip] 确保服务器监听 0.0.0.0 而非 127.0.0.1",
            "[Config] 使用 PORT 环境变量配置端口号",
            "[Package] 生产模式请设置 NODE_ENV=production"
        )
    }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFC107).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Lightbulb, null, tint = Color(0xFFFFC107), modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$framework Tips",
                    style = MaterialTheme.typography.titleMedium,
                    color = frameworkColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            tips.forEach { tip ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = frameworkColor.copy(alpha = 0.04f)
                ) {
                    Text(
                        text = tip,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
