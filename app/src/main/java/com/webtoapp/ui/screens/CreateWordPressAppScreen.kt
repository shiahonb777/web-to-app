package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton

import com.webtoapp.ui.theme.AppColors
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.webtoapp.ui.screens.create.WtaCreateFlowScaffold
import com.webtoapp.ui.screens.create.WtaCreateFlowSection
import com.webtoapp.ui.design.WtaRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.webtoapp.ui.components.EnhancedElevatedCard














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


    val wpBlue = AppColors.WordPress


    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }


    var siteTitle by remember { mutableStateOf(Strings.wpDefaultSiteTitle) }
    var adminUser by remember { mutableStateOf("admin") }
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("admin") }
    var landscapeMode by remember { mutableStateOf(false) }
    var sourceType by remember { mutableStateOf("BLANK") }


    var permalink by remember { mutableStateOf("postname") }


    var siteLanguage by remember { mutableStateOf(Strings.wpDefaultSiteLanguageCode) }


    var detectedThemes by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeTheme by remember { mutableStateOf<String?>(null) }
    var detectedPlugins by remember { mutableStateOf<List<String>>(emptyList()) }
    var activePlugins by remember { mutableStateOf<Set<String>>(emptySet()) }
    var wpVersion by remember { mutableStateOf<String?>(null) }
    var isImportMode by remember { mutableStateOf(false) }


    var isCreating by remember { mutableStateOf(false) }
    var creationPhase by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    val downloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    var showDownloadDialog by remember { mutableStateOf(false) }


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


                    creationPhase = Strings.wpCreatingProject
                    val newProjectId = WordPressManager.createProject(context, siteTitle, adminUser, adminEmail)

                    if (newProjectId != null) {

                        val importSuccess = WordPressManager.importFullProject(context, newProjectId, zipUri, siteTitle)
                        if (importSuccess) {
                            projectId = newProjectId
                            sourceType = "ZIP"
                            creationPhase = Strings.wpProjectReady


                            val metadata = withContext(Dispatchers.IO) { WordPressManager.inspectProject(context, newProjectId) }
                            detectedThemes = metadata.themes
                            detectedPlugins = metadata.plugins
                            activePlugins = metadata.plugins.toSet()
                            activeTheme = metadata.themes.firstOrNull()
                            wpVersion = metadata.version
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


    fun createNewSite() {
        scope.launch {
            isCreating = true
            creationPhase = Strings.wpCheckingDeps
            errorMessage = null
            isImportMode = false

            try {

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


                creationPhase = Strings.wpCreatingProject
                val newProjectId = WordPressManager.createProject(context, siteTitle, adminUser, adminEmail)

                if (newProjectId != null) {
                    projectId = newProjectId
                    sourceType = "BLANK"
                    creationPhase = Strings.wpProjectReady
                    val metadata = withContext(Dispatchers.IO) { WordPressManager.inspectProject(context, newProjectId) }
                    detectedThemes = metadata.themes
                    detectedPlugins = metadata.plugins
                    activePlugins = metadata.plugins.toSet()
                    activeTheme = metadata.themes.firstOrNull()
                    wpVersion = metadata.version
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


    val canCreate = projectId != null

    WtaCreateFlowScaffold(
        title = Strings.wpCreateTitle,
        onBack = onBack,
        actions = {
            TextButton(
                onClick = {
                    projectId?.let { pid ->
                        onCreated(
                            appName.ifBlank { siteTitle },
                            WordPressConfig(
                                projectId = pid,
                                projectName = appName.ifBlank { siteTitle },
                                siteTitle = siteTitle,
                                adminUser = adminUser,
                                adminEmail = adminEmail,
                                adminPassword = adminPassword,
                                themeName = activeTheme ?: "",
                                plugins = detectedPlugins,
                                activePlugins = activePlugins.toList(),
                                permalinkStructure = permalink,
                                siteLanguage = siteLanguage,
                                autoInstall = true,
                                sourceType = sourceType,
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
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WtaCreateFlowSection(title = Strings.importProject) {

                    WpHeroSection(
                        wpBlue = wpBlue,
                        wpVersion = wpVersion
                    )


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
                                    val newProjectId = WordPressManager.importProjectDirectory(context, java.io.File(path), sample.name)
                                    if (newProjectId != null) {
                                        projectId = newProjectId
                                        appName = sample.name
                                        siteTitle = sample.name
                                        sourceType = "SAMPLE"


                                        withContext(Dispatchers.IO) {
                                            try {
                                                val metadata = WordPressManager.inspectProject(context, newProjectId)
                                                detectedThemes = metadata.themes
                                                detectedPlugins = metadata.plugins
                                                activePlugins = metadata.plugins.toSet()
                                                activeTheme = metadata.themes.firstOrNull()
                                                wpVersion = metadata.version
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


            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RuntimeSectionHeader(
                        icon = Icons.Outlined.Settings,
                        title = Strings.wpBasicConfig,
                        brandColor = wpBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))


                    PremiumTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text(Strings.labelAppName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))


                    PremiumTextField(
                        value = siteTitle,
                        onValueChange = { siteTitle = it },
                        label = { Text(Strings.wpSiteTitle) },
                        placeholder = { Text(Strings.wpSiteTitleHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))


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


            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RuntimeSectionHeader(
                        icon = Icons.Outlined.Image,
                        title = Strings.labelIcon,
                        brandColor = wpBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(WtaRadius.Control))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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


            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RuntimeSectionHeader(
                        icon = Icons.Outlined.FolderOpen,
                        title = Strings.wpImportProject,
                        brandColor = wpBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.wpImportProjectDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))


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
            }


            WtaCreateFlowSection(title = Strings.appConfig) {
            if (projectId != null && !isCreating) {


                WpAdminConfigCard(
                    adminUser = adminUser,
                    onAdminUserChange = { adminUser = it },
                    adminEmail = adminEmail,
                    onAdminEmailChange = { adminEmail = it },
                    adminPassword = adminPassword,
                    onAdminPasswordChange = { adminPassword = it },
                    wpBlue = wpBlue
                )


                WpThemeCard(
                    themes = detectedThemes,
                    activeTheme = activeTheme,
                    onActiveThemeChange = { activeTheme = it },
                    wpBlue = wpBlue
                )


                WpPluginCard(
                    plugins = detectedPlugins,
                    activePlugins = activePlugins,
                    onPluginToggled = { plugin ->
                        activePlugins = if (plugin in activePlugins) {
                            activePlugins - plugin
                        } else {
                            activePlugins + plugin
                        }
                    },
                    wpBlue = wpBlue
                )


                WpPermalinkCard(
                    selected = permalink,
                    onSelect = { permalink = it },
                    wpBlue = wpBlue
                )


                WpLanguageCard(
                    selected = siteLanguage,
                    onSelect = { siteLanguage = it },
                    wpBlue = wpBlue
                )


                WpDbInfoCard(wpBlue = wpBlue)
            }
            }


            WtaCreateFlowSection(title = Strings.preview) {
                if (isCreating) {
                    RuntimeBrandedLoadingCard(creationPhase = creationPhase, brandColor = wpBlue)
                }


                errorMessage?.let { error ->
                    RuntimeBrandedErrorCard(error = error, onDismiss = { errorMessage = null })
                }


                if (projectId != null && !isCreating) {
                    RuntimeSuccessCard(
                        title = Strings.wpProjectReady,
                        subtitle = "ID: $projectId",
                        brandColor = wpBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }


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






@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WpHeroSection(
    wpBlue: Color,
    wpVersion: String?
) {
    val tags = buildList {
        wpVersion?.let { add("WP $it" to wpBlue) }
        add("PHP" to AppColors.Php)
        add("SQLite" to AppColors.SQLite)
    }

    RuntimeHeroSection(
        icon = Icons.Outlined.Language,
        title = Strings.wpHeroTitle,
        subtitle = Strings.wpHeroDesc,
        brandColor = wpBlue,
        tags = tags
    )
}




@Composable
private fun WpAdminConfigCard(
    adminUser: String,
    onAdminUserChange: (String) -> Unit,
    adminEmail: String,
    onAdminEmailChange: (String) -> Unit,
    adminPassword: String,
    onAdminPasswordChange: (String) -> Unit,
    wpBlue: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.AdminPanelSettings,
                title = Strings.wpAdminConfig,
                brandColor = wpBlue
            )
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
                placeholder = { Text(Strings.wpAdminEmailHint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Email, null, modifier = Modifier.size(20.dp)) }
            )
            Spacer(modifier = Modifier.height(10.dp))

            PremiumTextField(
                value = adminPassword,
                onValueChange = onAdminPasswordChange,
                label = { Text(Strings.wpAdminPassword) },
                placeholder = { Text(Strings.wpAdminPasswordHint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(20.dp)) }
            )
        }
    }
}




@Composable
private fun WpThemeCard(
    themes: List<String>,
    activeTheme: String?,
    onActiveThemeChange: (String) -> Unit,
    wpBlue: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Palette,
                title = Strings.wpThemePanel,
                brandColor = wpBlue
            ) {
                if (themes.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(WtaRadius.Button))
                            .background(wpBlue.copy(alpha = 0.12f))
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

                activeTheme?.let { theme ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Button))
                            .background(wpBlue.copy(alpha = 0.06f))
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(WtaRadius.Button))
                            .background(
                                if (isActive) wpBlue.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable { onActiveThemeChange(theme) }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WtaRadius.Button))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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




@Composable
private fun WpPluginCard(
    plugins: List<String>,
    activePlugins: Set<String>,
    onPluginToggled: (String) -> Unit,
    wpBlue: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Extension,
                title = Strings.wpPluginPanel,
                brandColor = wpBlue
            ) {
                if (plugins.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(WtaRadius.Button))
                            .background(wpBlue.copy(alpha = 0.12f))
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
                    val isActive = plugin in activePlugins
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(WtaRadius.Button))
                            .background(if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f))
                            .clickable { onPluginToggled(plugin) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isActive,
                                onCheckedChange = { onPluginToggled(plugin) },
                                colors = CheckboxDefaults.colors(checkedColor = wpBlue)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = plugin.replace("-", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(weight = 1f, fill = true),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (isActive) Strings.wpPluginActive else Strings.wpPluginInactive,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) AppColors.Success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WtaRadius.Button))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
            RuntimeSectionHeader(
                icon = Icons.Outlined.Link,
                title = Strings.wpPermalink,
                brandColor = wpBlue
            )
            Spacer(modifier = Modifier.height(12.dp))

            options.forEach { (value, label, icon) ->
                val isSelected = selected == value
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(WtaRadius.Button))
                        .background(if (isSelected) wpBlue.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onSelect(value) }
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




@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WpLanguageCard(
    selected: String,
    onSelect: (String) -> Unit,
    wpBlue: Color
) {
    val languages = listOf(
        "zh_CN" to Strings.langChineseSimplified,
        "en_US" to Strings.langEnglish,
        "zh_TW" to Strings.langChineseTraditional,
        "ja" to Strings.langJapanese,
        "ko_KR" to Strings.langKorean,
        "ar" to Strings.langArabic,
        "es_ES" to Strings.langSpanish,
        "fr_FR" to Strings.langFrench,
        "de_DE" to Strings.langGerman,
        "pt_BR" to Strings.langPortugueseBrazil
    )

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Translate,
                title = Strings.wpSiteLanguage,
                brandColor = wpBlue
            )
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




@Composable
private fun WpDbInfoCard(wpBlue: Color) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Storage,
                title = Strings.wpDbInfo,
                brandColor = wpBlue
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WtaRadius.Button))
                    .background(AppColors.SQLite.copy(alpha = 0.06f))
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WtaRadius.Button))
                            .background(AppColors.SQLite.copy(alpha = 0.06f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    Strings.wpDbEngine,
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
                                    Strings.wpDbMode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    Strings.wpDbOfflineMode,
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
                                    Strings.wpDbPlugin,
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
