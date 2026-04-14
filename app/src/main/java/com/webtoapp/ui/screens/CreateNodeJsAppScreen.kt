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
import com.webtoapp.core.nodejs.NodeSampleManager
import com.webtoapp.data.model.NodeJsBuildMode
import com.webtoapp.data.model.NodeJsConfig
import com.webtoapp.ui.components.*
import com.webtoapp.ui.components.TypedSampleProjectsCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.screens.create.common.CreateScreenState
import com.webtoapp.ui.screens.create.common.resolveDocumentTreeDirectory
import com.webtoapp.ui.screens.create.runtime.NodeJsProjectImportAnalysis
import com.webtoapp.ui.screens.create.runtime.NodeJsProjectImporter
import java.io.File

/**
 * create Node. js app
 * 
 * Note
 * Hero area( Express=, Fastify=, Koa=, NestJS=, Next. js=, Nuxt=)
 * package. json panel( optional)
 * Note
 * TypeScript indicator
 * manager( npm/yarn/pnpm)
 * with
 * item panel
 * hint
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
    val projectImporter = remember(context) { NodeJsProjectImporter(context) }
    
    // App
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    
    // Node. js config
    var buildMode by remember { mutableStateOf(NodeJsBuildMode.API_BACKEND) }
    var entryFile by remember { mutableStateOf("index.js") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }
    
    // item
    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var detectedEntryFile by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    
    // package. json
    var packageName by remember { mutableStateOf<String?>(null) }
    var packageVersion by remember { mutableStateOf<String?>(null) }
    var packageDescription by remember { mutableStateOf<String?>(null) }
    var npmScripts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedStartScript by remember { mutableStateOf<String?>(null) }
    var dependencies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var devDependencies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showAllDeps by remember { mutableStateOf(false) }
    var showAllDevDeps by remember { mutableStateOf(false) }
    
    // TypeScript
    var hasTypeScript by remember { mutableStateOf(false) }
    
    // TypeScript( Linux)
    var enableTsPreCompile by remember { mutableStateOf(false) }
    val esbuildAvailable = remember { NativeNodeEngine.isAvailable(context) }
    
    // manager
    var packageManager by remember { mutableStateOf("npm") }
    
    // Note
    var detectedPort by remember { mutableStateOf<Int?>(null) }
    var customPort by remember { mutableStateOf("") }
    
    // Node version
    var nodeEngineVersion by remember { mutableStateOf<String?>(null) }
    
    // state
    var isCreating by remember { mutableStateOf(false) }
    var creationPhase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val screenState = remember(isCreating, creationPhase, errorMessage) {
        CreateScreenState(
            isBusy = isCreating,
            phase = creationPhase,
            errorMessage = errorMessage
        )
    }

    fun applyImportAnalysis(
        imported: com.webtoapp.ui.screens.create.common.ImportedProject<NodeJsProjectImportAnalysis>,
        appNameOverride: String? = null,
    ) {
        val analysis = imported.analysis
        selectedProjectDir = analysis.projectDir.absolutePath
        detectedFramework = analysis.framework
        detectedEntryFile = analysis.entryFile
        entryFile = analysis.entryFile ?: entryFile
        buildMode = analysis.buildMode
        packageManager = analysis.packageManager
        packageName = analysis.packageName
        packageVersion = analysis.packageVersion
        packageDescription = analysis.packageDescription
        npmScripts = analysis.scripts
        selectedStartScript = analysis.selectedStartScript
        dependencies = analysis.dependencies
        devDependencies = analysis.devDependencies
        hasTypeScript = analysis.hasTypeScript
        detectedPort = analysis.detectedPort
        customPort = analysis.detectedPort?.toString().orEmpty()
        envVars = analysis.envVars
        nodeEngineVersion = analysis.nodeVersion
        projectId = imported.projectId
        appName = appNameOverride ?: analysis.suggestedAppName ?: appName
    }
    
    // editmode: load
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
    
    // downloadstate
    val downloadState by NodeDependencyManager.downloadState.collectAsStateWithLifecycle()
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    // Note
    val frameworkColor = remember(detectedFramework) {
        when (detectedFramework) {
            "Express" -> Color(0xFF259D3D) // Express
            "Fastify" -> Color(0xFF000000) // Fastify
            "Koa" -> Color(0xFF33333D) // Koa
            "NestJS" -> Color(0xFFE0234E) // NestJS
            "Hapi" -> Color(0xFFFF7B00) // Hapi
            "Next.js" -> Color(0xFF000000) // Next. js
            "Nuxt.js" -> Color(0xFF00DC82) // Nuxt
            else -> Color(0xFF339933) // Node. js default
        }
    }
    
    // fileselect
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
                    val projectDir = resolveDocumentTreeDirectory(treeUri)
                    if (!projectDir.exists() || !File(projectDir, "package.json").exists()) {
                        errorMessage = context.getString(com.webtoapp.R.string.njs_package_json_not_found)
                        return@launch
                    }

                    creationPhase = Strings.copyingProjectFiles
                    val imported = projectImporter.importProject(projectDir) { downloading ->
                        showDownloadDialog = downloading
                    }
                    applyImportAnalysis(imported)
                    creationPhase = Strings.njsProjectReady
                } catch (e: Exception) {
                    errorMessage = e.message ?: "项目导入失败"
                } finally {
                    isCreating = false
                }
            }
        }
    }
    
    // create
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
                                    // TypeScript create
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
            // ========== 1. Hero area ==========
            NodeJsHeroSection(
                detectedFramework = detectedFramework,
                frameworkColor = frameworkColor,
                hasTypeScript = hasTypeScript,
                packageManager = packageManager,
                nodeEngineVersion = nodeEngineVersion
            )
            
            // ========== 2. config ==========
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
                    
                    // app
                    PremiumTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text(Strings.labelAppName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // mode
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
            
            // ========== 3. iconselect ==========
            RuntimeIconPickerCard(
                appIcon = appIcon,
                onSelectIcon = { iconPickerLauncher.launch("image/*") }
            )
            
            // ========== 4. Project Selection ==========
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
            
            // ========== Sample Projects ==========
            if (selectedProjectDir == null && !isCreating) {
                TypedSampleProjectsCard(
                    title = Strings.sampleProjects,
                    subtitle = Strings.sampleNodeSubtitle,
                    samples = remember { NodeSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = NodeSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                isCreating = true
                                creationPhase = Strings.njsProjectDetected
                                try {
                                    creationPhase = Strings.copyingProjectFiles
                                    val imported = projectImporter.importProject(File(path)) { downloading ->
                                        showDownloadDialog = downloading
                                    }
                                    applyImportAnalysis(imported, appNameOverride = sample.name)
                                    creationPhase = Strings.njsProjectReady
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
            
            // ========== Cards below appear after project selection ==========
            if (projectId != null) {
                
                // ========== 5. Project Summary ==========
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
                
                // ========== 6. NPM Scripts Panel ==========
                if (npmScripts.isNotEmpty()) {
                    NodeJsScriptsCard(
                        scripts = npmScripts,
                        selectedScript = selectedStartScript,
                        onSelectScript = { selectedStartScript = it },
                        packageManager = packageManager,
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 7. Build Mode ==========
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
                        
                        // Entry file (non-static mode)
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
                
                // ========== 7.5 TypeScript Precompile (Linux) ==========
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
                
                // ========== 8. Port Settings ==========
                if (buildMode != NodeJsBuildMode.STATIC) {
                    NodeJsPortCard(
                        detectedPort = detectedPort,
                        customPort = customPort,
                        onCustomPortChange = { customPort = it },
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 9. Dependency View ==========
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
                
                // ========== 10. Environment Variables ==========
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
                            
                            // Existing environment variables
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
                            
                            // Add new environment variable
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
                
                // ========== 11. Framework-specific Tips ==========
                if (detectedFramework != null) {
                    NodeJsFrameworkTipsCard(
                        framework = detectedFramework!!,
                        frameworkColor = frameworkColor
                    )
                }
            }
            
            // Status message
            if (screenState.isBusy) {
                RuntimeLoadingCard(screenState.phase)
            }
            
            // Error message
            screenState.errorMessage?.let { error ->
                RuntimeErrorCard(error = error, onDismiss = { errorMessage = null })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Node.js download dialog
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

// ==================== Private Composable Components ====================

/**
 * Node. js Hero area
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
                        // Node versionlabel
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
                        // TypeScript label
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
                        // managerlabel
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
 * item card
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
                    // item @ version
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
                    
                    // Note
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
                    
                    // label
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Note
                        NjsInfoChip(
                            icon = Icons.Outlined.Inventory2,
                            label = "${Strings.njsDependencies}: $depCount",
                            color = frameworkColor
                        )
                        // Note
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
                        // manager
                        NjsInfoChip(
                            icon = Icons.Outlined.Archive,
                            label = "${Strings.njsPackageManager}: $packageManager",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        // Note
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
 * label
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
 * NPM panel
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
                                // label
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
 * configcard
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
            
            // Note
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
            
            // Note
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
 * card
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
            
            // Note
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
            
            // Note
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
 * hintcard
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
