package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton

import android.net.Uri
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
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.php.PhpSampleManager
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.ui.components.TypedSampleProjectsCard
import com.webtoapp.data.model.PhpAppConfig
import com.webtoapp.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.screens.create.common.CreateScreenState
import com.webtoapp.ui.screens.create.runtime.PhpProjectImportAnalysis
import com.webtoapp.ui.screens.create.runtime.PhpProjectImporter
import com.webtoapp.ui.screens.create.runtime.PhpProjectSourceLoader

/**
 * create/edit PHP app
 * 
 * Note
 * Hero area( Laravel=, ThinkPHP=, CodeIgniter=, Slim=)
 * Composer panel( composer. json)
 * Web directory select
 * PHP expand panel
 * config
 * hint
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePhpAppScreen(
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    existingAppId: Long = 0L,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        phpAppConfig: PhpAppConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isEdit = existingAppId > 0L
    val projectImporter = remember(context) { PhpProjectImporter(context) }
    val sourceLoader = remember { PhpProjectSourceLoader() }
    
    // App
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    
    // PHP config
    var documentRoot by remember { mutableStateOf("") }
    var entryFile by remember { mutableStateOf("index.php") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }
    
    // item
    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    
    // Composer
    var composerDeps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var composerDevDeps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showAllDeps by remember { mutableStateOf(false) }
    var showAllDevDeps by remember { mutableStateOf(false) }
    
    // Web directory
    var detectedWebDirs by remember { mutableStateOf<List<String>>(emptyList()) }
    var useCustomDocRoot by remember { mutableStateOf(false) }
    
    // PHP
    var phpExtensions by remember { mutableStateOf<Map<String, Boolean>>(mapOf(
        "pdo_sqlite" to true,
        "json" to true,
        "mbstring" to true,
        "openssl" to true,
        "curl" to false,
        "gd" to false,
        "zip" to false,
        "xml" to false
    )) }
    
    // Note
    var detectedDbFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var sqlitePath by remember { mutableStateOf("") }
    
    // version
    var frameworkVersion by remember { mutableStateOf<String?>(null) }
    
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
        imported: com.webtoapp.ui.screens.create.common.ImportedProject<PhpProjectImportAnalysis>,
        appNameOverride: String? = null,
    ) {
        val analysis = imported.analysis
        selectedProjectDir = analysis.projectDir.absolutePath
        detectedFramework = analysis.framework
        documentRoot = analysis.documentRoot
        entryFile = analysis.entryFile ?: entryFile
        envVars = analysis.envVars
        composerDeps = analysis.composerDependencies
        composerDevDeps = analysis.composerDevDependencies
        detectedWebDirs = analysis.detectedWebDirs
        phpExtensions = analysis.phpExtensions
        detectedDbFiles = analysis.detectedDatabaseFiles
        sqlitePath = analysis.sqlitePath
        frameworkVersion = analysis.frameworkVersion
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
                app.phpAppConfig?.let { config ->
                    documentRoot = config.documentRoot
                    entryFile = config.entryFile
                    landscapeMode = config.landscapeMode
                    envVars = config.envVars.toMutableMap()
                    detectedFramework = config.framework
                    projectId = config.projectId
                    selectedProjectDir = config.projectName
                }
            }
        }
    }
    
    // downloadstate
    val downloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    var showDownloadDialog by remember { mutableStateOf(false) }
    
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
                creationPhase = Strings.copyingProjectFiles
                errorMessage = null
                
                try {
                    val tempDir = File(context.cacheDir, "php_saf_import_${System.currentTimeMillis()}").apply { mkdirs() }
                    try {
                        val stagedDir = sourceLoader.copyDocumentTreeToTempDir(context, treeUri, tempDir)
                        creationPhase = Strings.phpFrameworkDetected
                        val imported = projectImporter.importProject(stagedDir) { downloading ->
                            showDownloadDialog = downloading
                        }
                        applyImportAnalysis(imported)
                        creationPhase = Strings.phpProjectReady
                    } finally {
                        tempDir.deleteRecursively()
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: Strings.projectImportFailed
                } finally {
                    isCreating = false
                }
            }
        }
    }
    
    // ZIP fileselect
    val zipPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { zipUri ->
            scope.launch {
                isCreating = true
                creationPhase = Strings.phpExtractingZip
                errorMessage = null
                
                try {
                    val extractDir = File(context.cacheDir, "php_zip_extract_${System.currentTimeMillis()}").apply { mkdirs() }
                    try {
                        val stagedDir = sourceLoader.extractZipToTempDir(context, zipUri, extractDir)
                        creationPhase = Strings.phpFrameworkDetected
                        val imported = projectImporter.importProject(stagedDir) { downloading ->
                            showDownloadDialog = downloading
                        }
                        applyImportAnalysis(imported)
                        creationPhase = Strings.phpProjectReady
                    } finally {
                        extractDir.deleteRecursively()
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: Strings.phpZipExtractFailed
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
            "laravel" -> Color(0xFFFF2D20)
            "thinkphp" -> Color(0xFF6190E8)
            "codeigniter" -> Color(0xFFDD4814)
            "slim" -> Color(0xFF74B72E)
            else -> Color(0xFF777BB4) // PHPdefault
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.createPhpApp) },
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
                                    appName.ifBlank { "PHP App" },
                                    PhpAppConfig(
                                        projectId = pid,
                                        projectName = appName.ifBlank { "PHP App" },
                                        framework = detectedFramework ?: "raw",
                                        documentRoot = documentRoot,
                                        entryFile = entryFile,
                                        envVars = envVars,
                                        hasComposerJson = selectedProjectDir?.let { File(it, "composer.json").exists() } ?: false,
                                        landscapeMode = landscapeMode
                                    ),
                                    appIcon,
                                    "AURORA"
                                )
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
            PhpHeroSection(
                detectedFramework = detectedFramework,
                frameworkColor = frameworkColor,
                frameworkVersion = frameworkVersion
            )
            
            // ========== item ==========
            if (selectedProjectDir == null) {
                TypedSampleProjectsCard(
                    title = Strings.sampleProjects,
                    subtitle = Strings.samplePhpSubtitle,
                    samples = remember { PhpSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = PhpSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                isCreating = true
                                creationPhase = Strings.phpFrameworkDetected
                                try {
                                    creationPhase = Strings.copyingProjectFiles
                                    val imported = projectImporter.importProject(File(path)) { downloading ->
                                        showDownloadDialog = downloading
                                    }
                                    applyImportAnalysis(imported, appNameOverride = sample.name)
                                    creationPhase = Strings.phpProjectReady
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
                        Text(Strings.phpSelectProject, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.phpSupportedFrameworks,
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
                                    Text(Strings.phpProjectReady, style = MaterialTheme.typography.bodyMedium, color = frameworkColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(selectedProjectDir!!.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                if (detectedFramework != null && detectedFramework != "raw") {
                                    Text("${Strings.phpFrameworkDetected}: $detectedFramework", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${Strings.phpDocumentRoot}: ${documentRoot.ifBlank { "/" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${Strings.phpEntryFile}: $entryFile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (frameworkVersion != null) {
                                    Text("${Strings.phpVersion}: $frameworkVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(Strings.phpSelectProject)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumOutlinedButton(
                        onClick = { zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                        enabled = !isCreating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Archive, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.phpImportZip)
                    }
                }
            }
            
            // ========== Cards below appear after project selection ==========
            if (projectId != null) {
                
                // ========== 5. Composer Dependencies Panel ==========
                if (composerDeps.isNotEmpty() || composerDevDeps.isNotEmpty()) {
                    PhpComposerDepsCard(
                        deps = composerDeps,
                        devDeps = composerDevDeps,
                        showAllDeps = showAllDeps,
                        showAllDevDeps = showAllDevDeps,
                        onToggleDeps = { showAllDeps = !showAllDeps },
                        onToggleDevDeps = { showAllDevDeps = !showAllDevDeps },
                        frameworkColor = frameworkColor
                    )
                }
                
                // ========== 6. Web Root Selector ==========
                PhpDocRootCard(
                    detectedWebDirs = detectedWebDirs,
                    currentDocRoot = documentRoot,
                    useCustom = useCustomDocRoot,
                    onSelectDir = { dir ->
                        documentRoot = dir
                        useCustomDocRoot = false
                    },
                    onToggleCustom = { useCustomDocRoot = it },
                    onCustomPathChange = { documentRoot = it },
                    entryFile = entryFile,
                    onEntryFileChange = { entryFile = it }
                )
                
                // ========== 7. PHP Extensions Panel ==========
                PhpExtensionsCard(
                    extensions = phpExtensions,
                    onToggle = { ext, enabled ->
                        phpExtensions = phpExtensions.toMutableMap().apply { put(ext, enabled) }
                    }
                )
                
                // ========== 8. Database Settings ==========
                if (detectedDbFiles.isNotEmpty()) {
                    PhpDatabaseCard(
                        detectedDbFiles = detectedDbFiles,
                        sqlitePath = sqlitePath,
                        onPathChange = { sqlitePath = it }
                    )
                }
                
                // ========== 9. Framework Tips ==========
                PhpFrameworkTipCard(framework = detectedFramework)
                
                // ========== 10. Environment Variables ==========
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
    
    // PHP download dialog
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(Strings.wpDownloadDeps) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    when (val state = downloadState) {
                        is WordPressDependencyManager.DownloadState.Downloading -> {
                            Text(Strings.wpDownloading)
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        }
                        is WordPressDependencyManager.DownloadState.Extracting -> {
                            Text("${Strings.wpExtracting} ${state.fileName}...")
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        is WordPressDependencyManager.DownloadState.Complete -> {
                            Text(Strings.wpDepsReady)
                        }
                        is WordPressDependencyManager.DownloadState.Error -> {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {
                            Text(Strings.wpDownloading)
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
 * PHP Hero area
 */
@Composable
private fun PhpHeroSection(
    detectedFramework: String?,
    frameworkColor: Color,
    frameworkVersion: String?
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
                            "$detectedFramework ${Strings.phpHeroTitle}"
                        else Strings.phpHeroTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = frameworkColor
                    )
                    Text(
                        text = Strings.phpHeroDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (frameworkVersion != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = frameworkColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "PHP $frameworkVersion",
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
 * Composer panel
 */
@Composable
private fun PhpComposerDepsCard(
    deps: Map<String, String>,
    devDeps: Map<String, String>,
    showAllDeps: Boolean,
    showAllDevDeps: Boolean,
    onToggleDeps: () -> Unit,
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
                ) { Icon(Icons.Outlined.Extension, null, tint = frameworkColor, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.phpComposerDeps, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = frameworkColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${deps.size + devDeps.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = frameworkColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (deps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("${Strings.phpRequireDeps} (${deps.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                
                val visibleDeps = if (showAllDeps) deps.entries.toList() else deps.entries.take(5).toList()
                visibleDeps.forEach { (name, version) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(weight = 1f, fill = true), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(version, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (deps.size > 5) {
                    TextButton(onClick = onToggleDeps, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showAllDeps) Strings.close else "${Strings.more} (${deps.size - 5})")
                    }
                }
            }
            
            if (devDeps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("${Strings.phpRequireDevDeps} (${devDeps.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                
                val visibleDevDeps = if (showAllDevDeps) devDeps.entries.toList() else devDeps.entries.take(3).toList()
                visibleDevDeps.forEach { (name, version) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(weight = 1f, fill = true), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(version, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (devDeps.size > 3) {
                    TextButton(onClick = onToggleDevDeps, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showAllDevDeps) Strings.close else "${Strings.more} (${devDeps.size - 3})")
                    }
                }
            }
        }
    }
}

/**
 * Web directoryselectcard
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PhpDocRootCard(
    detectedWebDirs: List<String>,
    currentDocRoot: String,
    useCustom: Boolean,
    onSelectDir: (String) -> Unit,
    onToggleCustom: (Boolean) -> Unit,
    onCustomPathChange: (String) -> Unit,
    entryFile: String,
    onEntryFileChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.phpDocRootSelect, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(Strings.phpDocRootHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (detectedWebDirs.isNotEmpty()) {
                Text(Strings.phpDetectedDirs, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumFilterChip(
                        selected = currentDocRoot.isBlank() && !useCustom,
                        onClick = { onSelectDir("") },
                        label = { Text("/ (${Strings.phpProjectRoot})") },
                        leadingIcon = if (currentDocRoot.isBlank() && !useCustom) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    detectedWebDirs.forEach { dir ->
                        PremiumFilterChip(
                            selected = currentDocRoot == dir && !useCustom,
                            onClick = { onSelectDir(dir) },
                            label = { Text("$dir/") },
                            leadingIcon = if (currentDocRoot == dir && !useCustom) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    PremiumFilterChip(
                        selected = useCustom,
                        onClick = { onToggleCustom(true) },
                        label = { Text(Strings.phpCustomPath) },
                        leadingIcon = if (useCustom) {
                            { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
            
            AnimatedVisibility(visible = useCustom || detectedWebDirs.isEmpty()) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumTextField(
                        value = currentDocRoot,
                        onValueChange = onCustomPathChange,
                        label = { Text(Strings.phpDocumentRoot) },
                        placeholder = { Text("public") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            PremiumTextField(
                value = entryFile,
                onValueChange = onEntryFileChange,
                label = { Text(Strings.phpEntryFile) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * PHP panel
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PhpExtensionsCard(
    extensions: Map<String, Boolean>,
    onToggle: (String, Boolean) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Extension, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.phpExtensions, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(Strings.phpExtensionsHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                extensions.forEach { (ext, enabled) ->
                    PremiumFilterChip(
                        selected = enabled,
                        onClick = { onToggle(ext, !enabled) },
                        label = { Text(ext, fontFamily = FontFamily.Monospace) },
                        leadingIcon = if (enabled) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * configcard
 */
@Composable
private fun PhpDatabaseCard(
    detectedDbFiles: List<String>,
    sqlitePath: String,
    onPathChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.phpDatabaseConfig, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("${Strings.phpDbDetected} (${detectedDbFiles.size})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        detectedDbFiles.take(3).forEach { path ->
                            Text(path, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            PremiumTextField(
                value = sqlitePath,
                onValueChange = onPathChange,
                label = { Text(Strings.phpSqlitePath) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

/**
 * hintcard
 */
@Composable
private fun PhpFrameworkTipCard(framework: String?) {
    val tip = when (framework?.lowercase()) {
        "laravel" -> Strings.phpLaravelTip
        "thinkphp" -> Strings.phpThinkPhpTip
        "codeigniter" -> Strings.phpCodeIgniterTip
        "slim" -> Strings.phpSlimTip
        else -> null
    }
    
    if (tip != null) {
        val tipColor = when (framework?.lowercase()) {
            "laravel" -> Color(0xFFFF2D20)
            "thinkphp" -> Color(0xFF6190E8)
            "codeigniter" -> Color(0xFFDD4814)
            "slim" -> Color(0xFF74B72E)
            else -> Color(0xFF777BB4)
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = tipColor.copy(alpha = 0.08f)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Lightbulb, null, tint = tipColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(Strings.phpFrameworkTip, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = tipColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
