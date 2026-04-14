package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton

import com.webtoapp.ui.theme.AppColors
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.core.wordpress.WordPressManager
import com.webtoapp.core.wordpress.WordPressSampleManager
import com.webtoapp.data.model.WordPressConfig
import com.webtoapp.ui.components.*
import com.webtoapp.ui.components.TypedSampleProjectsCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * create WordPress app
 * 
 * Note
 * WordPress Hero area( WP gradient)
 * managementpanel( , current)
 * listpanel( )
 * management config( , )
 * panel( SQLite offlinemode)
 * select
 * select
 * WordPress version
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateWordPressAppScreen(
    onBack: () -> Unit,
    onCreated: (
        name: String,
        wordpressConfig: WordPressConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // WordPress
    val wpBlue = Color(0xFF21759B)
    val wpDarkBlue = Color(0xFF0073AA)
    val wpGray = Color(0xFF464646)
    
    // App
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    
    // WordPress config
    var siteTitle by remember { mutableStateOf("My Site") }
    var adminUser by remember { mutableStateOf("admin") }
    var adminEmail by remember { mutableStateOf("") }
    var landscapeMode by remember { mutableStateOf(false) }
    
    // Note
    var permalink by remember { mutableStateOf("postname") }
    
    // Note
    var siteLanguage by remember { mutableStateOf("zh_CN") }
    
    // /( importmode)
    var detectedThemes by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeTheme by remember { mutableStateOf<String?>(null) }
    var detectedPlugins by remember { mutableStateOf<List<String>>(emptyList()) }
    var wpVersion by remember { mutableStateOf<String?>(null) }
    var isImportMode by remember { mutableStateOf(false) }
    
    // state
    var isCreating by remember { mutableStateOf(false) }
    var creationPhase by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // downloadstate
    val downloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    // fileselect
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    val zipPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { zipUri ->
            scope.launch {
                isCreating = true
                creationPhase = Strings.wpCheckingDeps
                errorMessage = null
                isImportMode = true
                
                try {
                    // checkanddownload
                    if (!WordPressDependencyManager.isAllReady(context)) {
                        showDownloadDialog = true
                        val success = WordPressDependencyManager.downloadAllDependencies(context)
                        showDownloadDialog = false
                        if (!success) {
                            errorMessage = Strings.wpDownloadFailed
                            isCreating = false
                            return@launch
                        }
                    }
                    
                    // createitem
                    creationPhase = Strings.wpCreatingProject
                    val newProjectId = WordPressManager.createProject(context, siteTitle, adminUser)
                    
                    if (newProjectId != null) {
                        // import WordPress
                        val importSuccess = WordPressManager.importFullProject(context, newProjectId, zipUri)
                        if (importSuccess) {
                            projectId = newProjectId
                            creationPhase = Strings.wpProjectReady
                            
                            // importitem
                            withContext(Dispatchers.IO) {
                                try {
                                    val projectDir = WordPressManager.getProjectDir(context, newProjectId)
                                    if (projectDir != null) {
                                        // Note
                                        val themesDir = File(projectDir, "wp-content/themes")
                                        if (themesDir.exists() && themesDir.isDirectory) {
                                            detectedThemes = themesDir.listFiles()
                                                ?.filter { it.isDirectory }
                                                ?.map { it.name }
                                                ?: emptyList()
                                            activeTheme = detectedThemes.firstOrNull { it != "index.php" }
                                        }
                                        
                                        // Note
                                        val pluginsDir = File(projectDir, "wp-content/plugins")
                                        if (pluginsDir.exists() && pluginsDir.isDirectory) {
                                            detectedPlugins = pluginsDir.listFiles()
                                                ?.filter { it.isDirectory && it.name != "index.php" }
                                                ?.map { it.name }
                                                ?: emptyList()
                                        }
                                        
                                        // WP version
                                        val versionFile = File(projectDir, "wp-includes/version.php")
                                        if (versionFile.exists()) {
                                            val content = versionFile.readText()
                                            val versionMatch = Regex("""\${'$'}wp_version\s*=\s*'([^']+)'""").find(content)
                                            wpVersion = versionMatch?.groupValues?.get(1)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // failed
                                }
                            }
                        } else {
                            errorMessage = Strings.wpProjectCreateFailed
                        }
                    } else {
                        errorMessage = Strings.wpProjectCreateFailed
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: Strings.wpProjectCreateFailed
                } finally {
                    isCreating = false
                }
            }
        }
    }
    
    // create
    fun createNewSite() {
        scope.launch {
            isCreating = true
            creationPhase = Strings.wpCheckingDeps
            errorMessage = null
            isImportMode = false
            
            try {
                // checkanddownload
                if (!WordPressDependencyManager.isAllReady(context)) {
                    showDownloadDialog = true
                    val success = WordPressDependencyManager.downloadAllDependencies(context)
                    showDownloadDialog = false
                    if (!success) {
                        errorMessage = Strings.wpDownloadFailed
                        isCreating = false
                        return@launch
                    }
                }
                
                // createitem
                creationPhase = Strings.wpCreatingProject
                val newProjectId = WordPressManager.createProject(context, siteTitle, adminUser)
                
                if (newProjectId != null) {
                    projectId = newProjectId
                    creationPhase = Strings.wpProjectReady
                    wpVersion = "6.4"  // WP version
                } else {
                    errorMessage = Strings.wpProjectCreateFailed
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: Strings.wpProjectCreateFailed
            } finally {
                isCreating = false
            }
        }
    }
    
    // create
    val canCreate = projectId != null
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.wpCreateTitle) },
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
                                    appName.ifBlank { siteTitle },
                                    WordPressConfig(
                                        projectId = pid,
                                        siteTitle = siteTitle,
                                        adminUser = adminUser,
                                        adminEmail = adminEmail,
                                        themeName = activeTheme ?: "",
                                        plugins = detectedPlugins,
                                        landscapeMode = landscapeMode
                                    ),
                                    appIcon,
                                    "AURORA"
                                )
                            }
                        },
                        enabled = canCreate && !isCreating
                    ) {
                        Text(Strings.btnCreate)
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
            // ========== 1. WordPress Hero area ==========
            WpHeroSection(
                wpBlue = wpBlue,
                wpVersion = wpVersion,
                isImportMode = isImportMode && projectId != null
            )
            
            // ========== item ==========
            if (projectId == null && !isCreating) {
                TypedSampleProjectsCard(
                    title = Strings.sampleProjects,
                    subtitle = Strings.sampleWpSubtitle,
                    samples = remember { WordPressSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = WordPressSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                isCreating = true
                                creationPhase = Strings.wpCheckingDeps
                                errorMessage = null
                                isImportMode = false
                                try {
                                    // checkanddownload
                                    if (!WordPressDependencyManager.isAllReady(context)) {
                                        showDownloadDialog = true
                                        val success = WordPressDependencyManager.downloadAllDependencies(context)
                                        showDownloadDialog = false
                                        if (!success) {
                                            errorMessage = Strings.wpDownloadFailed
                                            isCreating = false
                                            return@onSuccess
                                        }
                                    }
                                    
                                    creationPhase = Strings.wpCreatingProject
                                    val newProjectId = WordPressManager.createProject(context, sample.name, "admin")
                                    if (newProjectId != null) {
                                        projectId = newProjectId
                                        appName = sample.name
                                        wpVersion = "6.4"
                                        
                                        // itemin
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val sampleDir = java.io.File(path)
                                                val themesDir = java.io.File(sampleDir, "wp-content/themes")
                                                if (themesDir.exists() && themesDir.isDirectory) {
                                                    detectedThemes = themesDir.listFiles()
                                                        ?.filter { it.isDirectory }
                                                        ?.map { it.name }
                                                        ?: emptyList()
                                                    activeTheme = detectedThemes.firstOrNull()
                                                }
                                                Unit
                                            } catch (e: Exception) { android.util.Log.w("CreateWPApp", "Failed to detect themes", e) }
                                        }
                                        
                                        creationPhase = Strings.wpProjectReady
                                    } else {
                                        errorMessage = Strings.wpProjectCreateFailed
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: Strings.wpProjectCreateFailed
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
                                .background(wpBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.Settings, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.wpBasicConfig, style = MaterialTheme.typography.titleMedium)
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
                    
                    // Note
                    PremiumTextField(
                        value = siteTitle,
                        onValueChange = { siteTitle = it },
                        label = { Text(Strings.wpSiteTitle) },
                        placeholder = { Text(Strings.wpSiteTitleHint) },
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
                        Text(Strings.wpLandscapeMode)
                        PremiumSwitch(checked = landscapeMode, onCheckedChange = { landscapeMode = it })
                    }
                }
            }
            
            // ========== 3. iconselect ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(wpBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.Image, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.labelIcon, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            if (appIcon != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(appIcon).crossfade(true).build(),
                                    contentDescription = null, modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Language, null, tint = wpBlue)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        PremiumOutlinedButton(onClick = { iconPickerLauncher.launch("image/*") }) {
                            Text(Strings.selectIcon)
                        }
                    }
                }
            }
            
            // ========== 4. Project Create/Import ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(wpBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.FolderOpen, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(Strings.wpImportProject, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.wpImportProjectDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Import WordPress archive button
                    PremiumOutlinedButton(
                        onClick = { zipPickerLauncher.launch("application/zip") },
                        enabled = !isCreating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.FileUpload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.btnImport)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(weight = 1f, fill = true))
                        Text(
                            text = Strings.wpOrCreateNew,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        HorizontalDivider(modifier = Modifier.weight(weight = 1f, fill = true))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Create new site button
                    PremiumButton(
                        onClick = { createNewSite() },
                        enabled = !isCreating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = wpBlue)
                    ) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.wpCreateNewSite)
                    }
                    
                    Text(
                        text = Strings.wpCreateNewSiteDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // ========== Cards below appear after project creation ==========
            if (projectId != null && !isCreating) {
                
                // ========== 5. Admin Settings ==========
                WpAdminConfigCard(
                    adminUser = adminUser,
                    onAdminUserChange = { adminUser = it },
                    adminEmail = adminEmail,
                    onAdminEmailChange = { adminEmail = it },
                    wpBlue = wpBlue
                )
                
                // ========== 6. Theme Management ==========
                WpThemeCard(
                    themes = detectedThemes,
                    activeTheme = activeTheme,
                    onActiveThemeChange = { activeTheme = it },
                    isImportMode = isImportMode,
                    wpBlue = wpBlue
                )
                
                // ========== 7. Plugin Management ==========
                WpPluginCard(
                    plugins = detectedPlugins,
                    isImportMode = isImportMode,
                    wpBlue = wpBlue
                )
                
                // ========== 8. Permalink Structure ==========
                WpPermalinkCard(
                    selected = permalink,
                    onSelect = { permalink = it },
                    wpBlue = wpBlue
                )
                
                // ========== 9. Site Language ==========
                WpLanguageCard(
                    selected = siteLanguage,
                    onSelect = { siteLanguage = it },
                    wpBlue = wpBlue
                )
                
                // ========== 10. Database Info ==========
                WpDbInfoCard(wpBlue = wpBlue)
            }
            
            // Status display
            if (isCreating) {
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = wpBlue.copy(alpha = 0.1f)
                    )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = wpBlue)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(creationPhase, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            // Error message
            errorMessage?.let { error ->
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(error, modifier = Modifier.weight(weight = 1f, fill = true), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = { errorMessage = null }) { Text(Strings.btnCancel) }
                    }
                }
            }
            
            // Project ready state
            if (projectId != null && !isCreating) {
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = wpBlue.copy(alpha = 0.08f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = wpBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = Strings.wpProjectReady,
                                style = MaterialTheme.typography.bodyMedium,
                                color = wpBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ID: $projectId",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Download progress dialog
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(Strings.wpDownloadDeps) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    when (val state = downloadState) {
                        is WordPressDependencyManager.DownloadState.Downloading -> {
                            Text("${Strings.wpDownloading}: ${state.currentFile}")
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = wpBlue
                            )
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        is WordPressDependencyManager.DownloadState.Extracting -> {
                            Text("${Strings.wpExtracting}: ${state.fileName}")
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = wpBlue)
                        }
                        is WordPressDependencyManager.DownloadState.Verifying -> {
                            Text(Strings.verifyingFile.replaceFirst("%s", state.fileName))
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = wpBlue)
                        }
                        else -> {
                            Text(Strings.wpCheckingDeps)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = wpBlue)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${Strings.wpMirrorSource}: ${
                            if (WordPressDependencyManager.getMirrorRegion() == WordPressDependencyManager.MirrorRegion.CN)
                                Strings.wpMirrorCN
                            else
                                Strings.wpMirrorGlobal
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }
        }
}

// ==================== Private Composable Components ====================

/**
 * WordPress Hero area
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WpHeroSection(
    wpBlue: Color,
    wpVersion: String?,
    isImportMode: Boolean
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
                        colors = listOf(wpBlue.copy(alpha = 0.15f), Color(0xFF464646).copy(alpha = 0.08f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = wpBlue.copy(alpha = 0.15f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Language, null, modifier = Modifier.size(32.dp), tint = wpBlue)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        text = Strings.wpHeroTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = wpBlue
                    )
                    Text(
                        text = Strings.wpHeroDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // WP version
                        wpVersion?.let { ver ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = wpBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "WP $ver",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = wpBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // PHP label
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF777BB4).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "PHP",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF777BB4),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // SQLite label
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF003B57).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "SQLite",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF003B57),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        // import/newlabel
                        if (isImportMode) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFF6B35).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Imported",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF6B35),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * management configcard
 */
@Composable
private fun WpAdminConfigCard(
    adminUser: String,
    onAdminUserChange: (String) -> Unit,
    adminEmail: String,
    onAdminEmailChange: (String) -> Unit,
    wpBlue: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(wpBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.AdminPanelSettings, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.wpAdminConfig, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            PremiumTextField(
                value = adminUser,
                onValueChange = onAdminUserChange,
                label = { Text(Strings.wpAdminUser) },
                placeholder = { Text(Strings.wpAdminUserHint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Person, null, modifier = Modifier.size(20.dp)) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            PremiumTextField(
                value = adminEmail,
                onValueChange = onAdminEmailChange,
                label = { Text(Strings.wpAdminEmail) },
                placeholder = { Text("admin@example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Email, null, modifier = Modifier.size(20.dp)) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFC107).copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Info, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF8F00))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.wpAdminPassword,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ": admin",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * managementcard
 */
@Composable
private fun WpThemeCard(
    themes: List<String>,
    activeTheme: String?,
    onActiveThemeChange: (String) -> Unit,
    isImportMode: Boolean,
    wpBlue: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(wpBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Palette, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.wpThemePanel, style = MaterialTheme.typography.titleMedium)
                if (themes.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = wpBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${themes.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = wpBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (themes.isNotEmpty()) {
                // current
                activeTheme?.let { theme ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = wpBlue.copy(alpha = 0.06f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = wpBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    Strings.wpActiveTheme,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    theme,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = wpBlue
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    Strings.wpInstalledThemes,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                themes.forEach { theme ->
                    val isActive = theme == activeTheme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isActive) wpBlue.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        onClick = { onActiveThemeChange(theme) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isActive,
                                onClick = { onActiveThemeChange(theme) },
                                modifier = Modifier.size(20.dp),
                                colors = RadioButtonDefaults.colors(selectedColor = wpBlue)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = theme,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Palette, null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            Strings.wpNoThemes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * managementcard
 */
@Composable
private fun WpPluginCard(
    plugins: List<String>,
    isImportMode: Boolean,
    wpBlue: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(wpBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Extension, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.wpPluginPanel, style = MaterialTheme.typography.titleMedium)
                if (plugins.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = wpBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${plugins.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = wpBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (plugins.isNotEmpty()) {
                plugins.forEach { plugin ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Extension, null,
                                modifier = Modifier.size(18.dp),
                                tint = wpBlue
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = plugin.replace("-", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(weight = 1f, fill = true),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AppColors.Success.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Active",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Extension, null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            Strings.wpNoPlugins,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * selectcard
 */
@Composable
private fun WpPermalinkCard(
    selected: String,
    onSelect: (String) -> Unit,
    wpBlue: Color
) {
    val options = listOf(
        Triple("plain", Strings.wpPermalinkPlain, Icons.Outlined.Tag),
        Triple("postname", Strings.wpPermalinkPostName, Icons.Outlined.Link),
        Triple("numeric", Strings.wpPermalinkNumeric, Icons.Outlined.Numbers)
    )
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(wpBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Link, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.wpPermalink, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            options.forEach { (value, label, icon) ->
                val isSelected = selected == value
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) wpBlue.copy(alpha = 0.08f) else Color.Transparent,
                    onClick = { onSelect(value) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = wpBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isSelected) wpBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * selectcard
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WpLanguageCard(
    selected: String,
    onSelect: (String) -> Unit,
    wpBlue: Color
) {
    val languages = listOf(
        "zh_CN" to "中文（简体）",
        "en_US" to "English",
        "zh_TW" to "中文（繁體）",
        "ja" to "日本語",
        "ko_KR" to "한국어",
        "ar" to "العربية",
        "es_ES" to "Español",
        "fr_FR" to "Français",
        "de_DE" to "Deutsch",
        "pt_BR" to "Português (BR)"
    )
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(wpBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Translate, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.wpSiteLanguage, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { (code, label) ->
                    val isSelected = selected == code
                    PremiumFilterChip(
                        selected = isSelected,
                        onClick = { onSelect(code) },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
        }
    }
}

/**
 * card
 */
@Composable
private fun WpDbInfoCard(wpBlue: Color) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(wpBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Storage, null, tint = wpBlue, modifier = Modifier.size(22.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.wpDbInfo, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF003B57).copy(alpha = 0.06f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CheckCircle, null,
                            modifier = Modifier.size(18.dp),
                            tint = AppColors.Success
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            Strings.wpDbType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF003B57).copy(alpha = 0.06f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Engine",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "SQLite 3",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Offline / On-device",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = AppColors.Success
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Plugin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "wp-sqlite-db",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
