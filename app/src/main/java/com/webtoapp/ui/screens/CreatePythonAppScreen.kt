package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import com.webtoapp.core.python.PythonSampleManager
import com.webtoapp.data.model.PythonAppConfig
import com.webtoapp.ui.components.TypedSampleProjectsCard
import com.webtoapp.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.screens.create.common.CreateScreenState
import com.webtoapp.ui.screens.create.common.resolveDocumentTreeDirectory
import com.webtoapp.ui.screens.create.runtime.PythonProjectImportAnalysis
import com.webtoapp.ui.screens.create.runtime.PythonProjectImporter

/**
 * create/edit Python app
 * 
 * Note
 * Hero area( Flask=, Django=, FastAPI=, Tornado=)
 * requirements. txt / pyproject. toml panel
 * type select( Builtin / Gunicorn WSGI / Uvicorn ASGI)
 * WSGI/ASGI moduleconfig
 * indicator
 * Django configpanel( settings module, directory, ALLOWED_HOSTS)
 * FastAPI configpanel( API, ASGI)
 * hint
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePythonAppScreen(
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    existingAppId: Long = 0L,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        pythonAppConfig: PythonAppConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isEdit = existingAppId > 0L
    val projectImporter = remember(context) { PythonProjectImporter(context) }
    
    // App
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    
    // Python config
    var entryFile by remember { mutableStateOf("app.py") }
    var entryModule by remember { mutableStateOf("") }
    var serverType by remember { mutableStateOf("builtin") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }
    
    // item
    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    
    // list
    var requirements by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var requirementsSource by remember { mutableStateOf("") }
    var showAllDeps by remember { mutableStateOf(false) }
    
    // Note
    var venvDetected by remember { mutableStateOf(false) }
    var venvPath by remember { mutableStateOf<String?>(null) }
    
    // Python version
    var pythonVersion by remember { mutableStateOf<String?>(null) }
    
    // Django
    var djangoSettingsModule by remember { mutableStateOf("") }
    var djangoStaticDir by remember { mutableStateOf("static") }
    
    // FastAPI
    var fastapiDocsEnabled by remember { mutableStateOf(true) }
    
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
        imported: com.webtoapp.ui.screens.create.common.ImportedProject<PythonProjectImportAnalysis>,
        appNameOverride: String? = null,
    ) {
        val analysis = imported.analysis
        selectedProjectDir = analysis.projectDir.absolutePath
        detectedFramework = analysis.framework
        entryFile = analysis.entryFile ?: entryFile
        entryModule = analysis.entryModule
        serverType = analysis.serverType
        envVars = analysis.envVars
        requirements = analysis.requirements
        requirementsSource = analysis.requirementsSource
        venvDetected = analysis.venvDetected
        venvPath = analysis.venvPath
        pythonVersion = analysis.pythonVersion
        djangoSettingsModule = analysis.djangoSettingsModule
        djangoStaticDir = analysis.djangoStaticDir
        fastapiDocsEnabled = analysis.fastapiDocsEnabled
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
                app.pythonAppConfig?.let { config ->
                    entryFile = config.entryFile
                    entryModule = config.entryModule
                    serverType = config.serverType
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
                    creationPhase = AppStringsProvider.current().pyProjectReady
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
            "flask" -> Color(0xFF333333)
            "django" -> Color(0xFF0C4B33)
            "fastapi" -> Color(0xFF009688)
            "tornado" -> Color(0xFF4285F4)
            else -> Color(0xFF3776AB) // Python default
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().createPythonApp) },
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
                                    appName.ifBlank { "Python App" },
                                    PythonAppConfig(
                                        projectId = pid,
                                        projectName = appName.ifBlank { "Python App" },
                                        framework = detectedFramework ?: "raw",
                                        entryFile = entryFile,
                                        entryModule = entryModule,
                                        serverType = serverType,
                                        envVars = envVars,
                                        requirementsFile = if (selectedProjectDir?.let { File(it, "requirements.txt").exists() } == true) "requirements.txt" else "",
                                        hasPipDeps = selectedProjectDir?.let { File(it, "requirements.txt").exists() } ?: false,
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
            PythonHeroSection(
                detectedFramework = detectedFramework,
                frameworkColor = frameworkColor,
                pythonVersion = pythonVersion
            )
            
            // ========== item ==========
            if (selectedProjectDir == null) {
                TypedSampleProjectsCard(
                    title = AppStringsProvider.current().sampleProjects,
                    subtitle = AppStringsProvider.current().samplePythonSubtitle,
                    samples = remember { PythonSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = PythonSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                isCreating = true
                                creationPhase = AppStringsProvider.current().frameworkDetected
                                try {
                                    creationPhase = AppStringsProvider.current().copyingProjectFiles
                                    val imported = projectImporter.importProject(File(path))
                                    applyImportAnalysis(imported, appNameOverride = sample.name)
                                    creationPhase = AppStringsProvider.current().pyProjectReady
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
                        Text(AppStringsProvider.current().pySelectProject, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = AppStringsProvider.current().pySupportedFrameworks,
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
                                    Text(AppStringsProvider.current().pyProjectReady, style = MaterialTheme.typography.bodyMedium, color = frameworkColor)
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
                                Text(
                                    "${AppStringsProvider.current().entryFile}: $entryFile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (pythonVersion != null) {
                                    Text(
                                        "Python $pythonVersion",
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
                        Text(AppStringsProvider.current().pySelectProject)
                    }
                }
            }
            
            // ========== Cards below appear after project selection ==========
            if (projectId != null) {
                
                // ========== 5. Virtual Env Indicator ==========
                PythonVenvIndicator(
                    venvDetected = venvDetected,
                    venvPath = venvPath,
                    frameworkColor = frameworkColor
                )
                
                // ========== 6. Dependencies Panel ==========
                if (requirements.isNotEmpty()) {
                    PythonRequirementsCard(
                        requirements = requirements,
                        source = requirementsSource,
                        showAll = showAllDeps,
                        onToggleShowAll = { showAllDeps = !showAllDeps },
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 7. Server Type Selector ==========
                PythonServerTypeCard(
                    serverType = serverType,
                    detectedFramework = detectedFramework,
                    onServerTypeChange = { serverType = it },
                    frameworkColor = frameworkColor
                )
                
                // ========== 8. WSGI/ASGI Module Settings ==========
                PythonModuleConfigCard(
                    entryFile = entryFile,
                    onEntryFileChange = { entryFile = it },
                    entryModule = entryModule,
                    onEntryModuleChange = { entryModule = it },
                    serverType = serverType
                )
                
                // ========== 9. Django Panel ==========
                if (detectedFramework == "django") {
                    PythonDjangoCard(
                        settingsModule = djangoSettingsModule,
                        onSettingsModuleChange = { djangoSettingsModule = it },
                        staticDir = djangoStaticDir,
                        onStaticDirChange = { djangoStaticDir = it }
                    )
                }
                
                // ========== 10. FastAPI Panel ==========
                if (detectedFramework == "fastapi") {
                    PythonFastapiCard(
                        docsEnabled = fastapiDocsEnabled,
                        onDocsEnabledChange = { fastapiDocsEnabled = it }
                    )
                }
                
                // ========== 11. Framework Tips ==========
                PythonFrameworkTipCard(framework = detectedFramework)
                
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
 * Python Hero area
 */
@Composable
private fun PythonHeroSection(
    detectedFramework: String?,
    frameworkColor: Color,
    pythonVersion: String?
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
                        text = if (detectedFramework != null && detectedFramework != "raw")
                            "${detectedFramework!!.replaceFirstChar { it.uppercase() }} ${AppStringsProvider.current().pyHeroTitle}"
                        else AppStringsProvider.current().pyHeroTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = frameworkColor
                    )
                    Text(
                        text = AppStringsProvider.current().pyHeroDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pythonVersion != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = frameworkColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Python $pythonVersion",
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
 * indicator
 */
@Composable
private fun PythonVenvIndicator(
    venvDetected: Boolean,
    venvPath: String?,
    frameworkColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (venvDetected) frameworkColor.copy(alpha = 0.08f)
        else if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (venvDetected) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                null,
                tint = if (venvDetected) frameworkColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    text = AppStringsProvider.current().pyVenvDetected,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (venvDetected) frameworkColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (venvDetected) "${AppStringsProvider.current().pyVenvFound} ($venvPath/)"
                    else AppStringsProvider.current().pyVenvNotFound,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (venvDetected) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = frameworkColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "venv",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = frameworkColor
                    )
                }
            }
        }
    }
}

/**
 * listpanel
 */
@Composable
private fun PythonRequirementsCard(
    requirements: List<Pair<String, String>>,
    source: String,
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
                Text(AppStringsProvider.current().pyRequirements, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = frameworkColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${requirements.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = frameworkColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${AppStringsProvider.current().pyRequirementsFile}: $source",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            val visibleDeps = if (showAll) requirements else requirements.take(8)
            visibleDeps.forEach { (name, version) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (version.isNotBlank()) {
                        Text(
                            version,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (requirements.size > 8) {
                TextButton(onClick = onToggleShowAll, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showAll) AppStringsProvider.current().close else "${AppStringsProvider.current().more} (${requirements.size - 8})")
                }
            }
        }
    }
}

/**
 * type select
 */
@Composable
private fun PythonServerTypeCard(
    serverType: String,
    detectedFramework: String?,
    onServerTypeChange: (String) -> Unit,
    frameworkColor: Color
) {
    val recommendedServer = when (detectedFramework?.lowercase()) {
        "fastapi" -> "uvicorn"
        "django" -> "gunicorn"
        "flask" -> "gunicorn"
        else -> null
    }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Dns, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().pyServerType, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // Builtin
            PythonServerOption(
                title = AppStringsProvider.current().pyServerBuiltin,
                description = AppStringsProvider.current().pyServerBuiltinDesc,
                selected = serverType == "builtin",
                isRecommended = recommendedServer == null,
                onClick = { onServerTypeChange("builtin") },
                accentColor = frameworkColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Gunicorn (WSGI)
            PythonServerOption(
                title = AppStringsProvider.current().pyServerGunicorn,
                description = AppStringsProvider.current().pyServerGunicornDesc,
                selected = serverType == "gunicorn",
                isRecommended = recommendedServer == "gunicorn",
                onClick = { onServerTypeChange("gunicorn") },
                accentColor = frameworkColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Uvicorn (ASGI)
            PythonServerOption(
                title = AppStringsProvider.current().pyServerUvicorn,
                description = AppStringsProvider.current().pyServerUvicornDesc,
                selected = serverType == "uvicorn",
                isRecommended = recommendedServer == "uvicorn",
                onClick = { onServerTypeChange("uvicorn") },
                accentColor = frameworkColor
            )
        }
    }
}

/**
 * card
 */
@Composable
private fun PythonServerOption(
    title: String,
    description: String,
    selected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accentColor.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                AppStringsProvider.current().pyRecommended,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }
                    }
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * WSGI/ASGI modulewith fileconfigcard
 */
@Composable
private fun PythonModuleConfigCard(
    entryFile: String,
    onEntryFileChange: (String) -> Unit,
    entryModule: String,
    onEntryModuleChange: (String) -> Unit,
    serverType: String
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().phpAdvancedConfig, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            PremiumTextField(
                value = entryFile,
                onValueChange = onEntryFileChange,
                label = { Text(AppStringsProvider.current().entryFile) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            AnimatedVisibility(visible = serverType != "builtin") {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumTextField(
                        value = entryModule,
                        onValueChange = onEntryModuleChange,
                        label = { Text(AppStringsProvider.current().pyWsgiModule) },
                        placeholder = { Text(AppStringsProvider.current().pyWsgiModuleHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text(
                                if (serverType == "gunicorn") "WSGI: module.wsgi:application"
                                else "ASGI: module:app",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Django configpanel
 */
@Composable
private fun PythonDjangoCard(
    settingsModule: String,
    onSettingsModuleChange: (String) -> Unit,
    staticDir: String,
    onStaticDirChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C4B33).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Settings, null, tint = Color(0xFF0C4B33), modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().pyDjangoSettings, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            PremiumTextField(
                value = settingsModule,
                onValueChange = onSettingsModuleChange,
                label = { Text(AppStringsProvider.current().pyDjangoSettingsModule) },
                placeholder = { Text("myproject.settings") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            PremiumTextField(
                value = staticDir,
                onValueChange = onStaticDirChange,
                label = { Text(AppStringsProvider.current().pyDjangoStaticDir) },
                placeholder = { Text("static") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0C4B33).copy(alpha = 0.06f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = Color(0xFF0C4B33), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        AppStringsProvider.current().pyDjangoAllowedHosts,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0C4B33)
                    )
                }
            }
        }
    }
}

/**
 * FastAPI configpanel
 */
@Composable
private fun PythonFastapiCard(
    docsEnabled: Boolean,
    onDocsEnabledChange: (Boolean) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF009688).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Api, null, tint = Color(0xFF009688), modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(AppStringsProvider.current().pyFastapiConfig, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF009688).copy(alpha = 0.06f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        AppStringsProvider.current().pyFastapiDocsEndpoint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF009688)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        AppStringsProvider.current().pyFastapiAsgiHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * hintcard
 */
@Composable
private fun PythonFrameworkTipCard(framework: String?) {
    data class FrameworkTip(val tip: String, val color: Color)
    
    val tipData = when (framework?.lowercase()) {
        "flask" -> FrameworkTip(
            tip = "Flask: Debug mode auto-disabled for production. Static files served from static/ folder.",
            color = Color(0xFF333333)
        )
        "django" -> null // Django has its own dedicated card
        "fastapi" -> null // FastAPI has its own dedicated card
        "tornado" -> FrameworkTip(
            tip = "Tornado: IOLoop auto-configured. WebSocket support available. Non-blocking mode enabled.",
            color = Color(0xFF4285F4)
        )
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
