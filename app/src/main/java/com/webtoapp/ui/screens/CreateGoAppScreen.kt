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
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.golang.GoSampleManager
import com.webtoapp.data.model.GoAppConfig
import com.webtoapp.ui.components.TypedSampleProjectsCard
import com.webtoapp.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.screens.create.common.CreateScreenState
import com.webtoapp.ui.screens.create.common.resolveDocumentTreeDirectory
import com.webtoapp.ui.screens.create.runtime.GoProjectImportAnalysis
import com.webtoapp.ui.screens.create.runtime.GoProjectImporter

/**
 * create/edit Go app
 * 
 * Note
 * Hero area( Gin=, Fiber=, Echo=, Chi=)
 * go. mod panel( modulepath, Go version, )
 * card( file, , ELF check)
 * select( ARM64, ARM, x86_64)
 * filedirectoryconfig
 * check config
 * listpanel
 * hint
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
    val projectImporter = remember(context) { GoProjectImporter(context) }
    
    // App
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    
    // Go config
    var binaryName by remember { mutableStateOf("") }
    var staticDir by remember { mutableStateOf("") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }
    
    // item
    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    
    // go. mod
    var goModulePath by remember { mutableStateOf<String?>(null) }
    var goVersion by remember { mutableStateOf<String?>(null) }
    var goDeps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showAllDeps by remember { mutableStateOf(false) }
    
    // Note
    var binarySize by remember { mutableStateOf<Long?>(null) }
    var binaryDetected by remember { mutableStateOf(false) }
    
    // Note
    var targetArch by remember { mutableStateOf("arm64") }
    
    // check
    var healthCheckEndpoint by remember { mutableStateOf("/health") }
    
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
        imported: com.webtoapp.ui.screens.create.common.ImportedProject<GoProjectImportAnalysis>,
        appNameOverride: String? = null,
    ) {
        val analysis = imported.analysis
        selectedProjectDir = analysis.projectDir.absolutePath
        detectedFramework = analysis.framework
        binaryName = analysis.binaryName
        binaryDetected = analysis.binaryDetected
        binarySize = analysis.binarySize
        staticDir = analysis.staticDir
        envVars = analysis.envVars
        goModulePath = analysis.modulePath
        goVersion = analysis.goVersion
        goDeps = analysis.dependencies
        targetArch = analysis.targetArch
        healthCheckEndpoint = analysis.healthCheckEndpoint
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
                creationPhase = AppStringsProvider.current().frameworkDetected
                errorMessage = null
                
                try {
                    val projectDir = resolveDocumentTreeDirectory(treeUri)
                    if (!projectDir.exists()) {
                        errorMessage = AppStringsProvider.current().dirNotExists
                        return@launch
                    }
                    creationPhase = AppStringsProvider.current().copyingProjectFiles
                    val imported = projectImporter.importProject(projectDir)
                    applyImportAnalysis(imported)
                    creationPhase = AppStringsProvider.current().goProjectReady
                } catch (e: Exception) {
                    errorMessage = e.message ?: AppStringsProvider.current().projectImportFailed
                } finally {
                    isCreating = false
                }
            }
        }
    }
    
    val canCreate = projectId != null
    
    // Note
    val frameworkColor = remember(detectedFramework) {
        when (detectedFramework?.lowercase()) {
            "gin" -> Color(0xFF0090FF)
            "fiber" -> Color(0xFF8B5CF6)
            "echo" -> Color(0xFF00BCD4)
            "chi" -> AppColors.Error
            "net_http" -> Color(0xFF00ADD8)
            else -> Color(0xFF00ADD8) // Go default
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().createGoApp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
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
                    ) { Text(if (isEdit) AppStringsProvider.current().btnSave else AppStringsProvider.current().btnCreate) }
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
            GoHeroSection(
                detectedFramework = detectedFramework,
                frameworkColor = frameworkColor,
                goVersion = goVersion
            )
            
            // ========== item ==========
            if (selectedProjectDir == null) {
                TypedSampleProjectsCard(
                    title = AppStringsProvider.current().sampleProjects,
                    subtitle = AppStringsProvider.current().sampleGoSubtitle,
                    samples = remember { GoSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = GoSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                isCreating = true
                                creationPhase = AppStringsProvider.current().frameworkDetected
                                try {
                                    creationPhase = AppStringsProvider.current().copyingProjectFiles
                                    val imported = projectImporter.importProject(File(path))
                                    applyImportAnalysis(imported, appNameOverride = sample.name)
                                    creationPhase = AppStringsProvider.current().goProjectReady
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
                        Text(AppStringsProvider.current().njsBasicConfig, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text(AppStringsProvider.current().labelAppName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStringsProvider.current().njsLandscapeMode)
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
                        Text(AppStringsProvider.current().goSelectProject, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = AppStringsProvider.current().goSupportedFrameworks,
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
                                    Text(AppStringsProvider.current().goProjectReady, style = MaterialTheme.typography.bodyMedium, color = frameworkColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selectedProjectDir!!.substringAfterLast("/"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (detectedFramework != null && detectedFramework != "raw") {
                                    Text(
                                        "${AppStringsProvider.current().frameworkDetected}: $detectedFramework",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (binaryName.isNotEmpty()) {
                                    Text(
                                        "${AppStringsProvider.current().goSelectBinary}: $binaryName",
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
                        Text(AppStringsProvider.current().goSelectProject)
                    }
                }
            }
            
            // ========== Cards below appear after project selection ==========
            if (projectId != null) {
                
                // ========== 5. Go Module Info Panel ==========
                if (goModulePath != null) {
                    GoModuleInfoCard(
                        modulePath = goModulePath!!,
                        goVersion = goVersion,
                        depCount = goDeps.size,
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 6. Binary Detection Card ==========
                GoBinaryDetectionCard(
                    binaryDetected = binaryDetected,
                    binaryName = binaryName,
                    binarySize = binarySize,
                    onBinaryNameChange = { binaryName = it },
                    frameworkColor = frameworkColor
                )
                
                // ========== 7. Target Architecture Selector ==========
                GoTargetArchCard(
                    targetArch = targetArch,
                    onArchChange = { targetArch = it },
                    frameworkColor = frameworkColor
                )
                
                // ========== 8. Static File Settings ==========
                GoStaticFilesCard(
                    staticDir = staticDir,
                    onStaticDirChange = { staticDir = it }
                )
                
                // ========== 9. Health Check Endpoint ==========
                GoHealthCheckCard(
                    endpoint = healthCheckEndpoint,
                    onEndpointChange = { healthCheckEndpoint = it }
                )
                
                // ========== 10. Dependencies Panel ==========
                if (goDeps.isNotEmpty()) {
                    GoDepsCard(
                        deps = goDeps,
                        showAll = showAllDeps,
                        onToggleShowAll = { showAllDeps = !showAllDeps },
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 11. Framework Tips ==========
                GoFrameworkTipCard(framework = detectedFramework)
                
                // ========== 12. Environment Variables ==========
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
            
            // Status message
            if (screenState.isBusy) {
                RuntimeLoadingCard(screenState.phase)
            }
            
            screenState.errorMessage?.let { error ->
                RuntimeErrorCard(error = error, onDismiss = { errorMessage = null })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
        }
}

// ==================== Private Composable Components ====================

/**
 * Go Hero area
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
                            "${detectedFramework!!.replaceFirstChar { it.uppercase() }} ${AppStringsProvider.current().goHeroTitle}"
                        else AppStringsProvider.current().goHeroTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = frameworkColor
                    )
                    Text(
                        text = AppStringsProvider.current().goHeroDesc,
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
 * Go module card
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
                Text(AppStringsProvider.current().goModuleInfo, style = MaterialTheme.typography.titleMedium)
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
                        Text(AppStringsProvider.current().goModulePath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(modulePath, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                    }
                    if (goVersion != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStringsProvider.current().goVersion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(goVersion, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStringsProvider.current().goDependencyCount, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$depCount", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = frameworkColor)
                    }
                }
            }
        }
    }
}

/**
 * card
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
                Text(AppStringsProvider.current().goBinaryDetection, style = MaterialTheme.typography.titleMedium)
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
                            if (binaryDetected) AppStringsProvider.current().goBinaryFound else AppStringsProvider.current().goBinaryNotFound,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (binaryDetected) frameworkColor else MaterialTheme.colorScheme.error
                        )
                        if (binaryDetected && binarySize != null) {
                            Text(
                                "${AppStringsProvider.current().goBinarySize}: ${formatFileSize(binarySize)}",
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
                label = { Text(AppStringsProvider.current().goSelectBinary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * select
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
                Text(AppStringsProvider.current().goTargetArch, style = MaterialTheme.typography.titleMedium)
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
 * filedirectoryconfigcard
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
                Text(AppStringsProvider.current().goStaticFiles, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                AppStringsProvider.current().goStaticFilesHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            PremiumTextField(
                value = staticDir,
                onValueChange = onStaticDirChange,
                label = { Text(AppStringsProvider.current().goStaticFiles) },
                placeholder = { Text("static/") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * check card
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
                Text(AppStringsProvider.current().goHealthCheck, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            PremiumTextField(
                value = endpoint,
                onValueChange = onEndpointChange,
                label = { Text(AppStringsProvider.current().goHealthCheckEndpoint) },
                placeholder = { Text("/health") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * panel
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
                Text(AppStringsProvider.current().goDirectDeps, style = MaterialTheme.typography.titleMedium)
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
                    Text(if (showAll) AppStringsProvider.current().close else "${AppStringsProvider.current().more} (${deps.size - 6})")
                }
            }
        }
    }
}

/**
 * hint
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
                        AppStringsProvider.current().phpFrameworkTip,
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
 * file
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
