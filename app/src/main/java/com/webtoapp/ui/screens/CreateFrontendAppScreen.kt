package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton

import com.webtoapp.ui.theme.AppColors
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.frontend.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.linux.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.screens.create.WtaCreateFlowScaffold
import com.webtoapp.ui.screens.create.WtaCreateFlowSection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File








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
    @Suppress("UNUSED_PARAMETER")
    onNavigateToLinuxEnv: () -> Unit = {},
    existingAppId: Long? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = existingAppId != null


    val linuxEnv = remember { LinuxEnvironmentManager.getInstance(context) }
    val linuxState by linuxEnv.state.collectAsStateWithLifecycle()


    var buildMode by remember { mutableStateOf(BuildMode.IMPORT_DIST) }


    var projectPath by remember { mutableStateOf<String?>(null) }
    var projectName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var existingApp by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }


    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = org.koin.java.KoinJavaComponent.get<com.webtoapp.data.repository.WebAppRepository>(com.webtoapp.data.repository.WebAppRepository::class.java)
                .getWebAppById(existingAppId)
                .first()
            existingApp?.let { app ->
                projectName = app.name
                app.iconPath?.let { path -> appIcon = Uri.parse(path) }

                app.htmlConfig?.files?.firstOrNull()?.path?.let { firstFilePath ->
                    val projectDir = File(firstFilePath).parentFile?.absolutePath
                    if (projectDir != null) {
                        projectPath = projectDir
                    }
                }
            }
        }
    }


    var detectionResult by remember { mutableStateOf<ProjectDetectionResult?>(null) }
    var isDetecting by remember { mutableStateOf(false) }


    val importBuilder = remember { FrontendProjectBuilder(context) }
    val nodeBuilder = remember { NodeProjectBuilder(context) }
    val githubFetcher = remember { GitHubRepoFetcher(context) }

    val importState by importBuilder.buildState.collectAsStateWithLifecycle()
    val importLogs by importBuilder.buildLogs.collectAsStateWithLifecycle()

    val nodeBuildState by nodeBuilder.buildState.collectAsStateWithLifecycle()
    val nodeBuildLogs by nodeBuilder.buildLogs.collectAsStateWithLifecycle()


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


    var showLogsDialog by remember { mutableStateOf(false) }
    var showErrorReportDialog by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        linuxEnv.checkEnvironment()
    }


    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }


    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(context, it)
            if (path != null) {
                projectPath = path
                projectName = File(path).name


                scope.launch {
                    isDetecting = true
                    detectionResult = ProjectDetector.detectProject(path)
                    isDetecting = false
                }
            }
        }
    }


    val canImport = projectPath != null &&
                   detectionResult != null &&
                   detectionResult?.issues?.none { it.severity == IssueSeverity.ERROR } == true &&
                   currentBuildState is BuildState.Idle

    val canBuild = projectPath != null &&
                  detectionResult != null &&
                  linuxState is EnvironmentState.Ready &&
                  currentBuildState is BuildState.Idle

    WtaCreateFlowScaffold(
        title = if (isEditMode) Strings.editFrontendApp else Strings.createFrontendApp,
        onBack = onBack,
        actions = {
            if (currentLogs.isNotEmpty()) {
                IconButton(onClick = { showLogsDialog = true }) {
                    Icon(Icons.Outlined.Terminal, Strings.logs)
                }
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            WtaCreateFlowSection(title = Strings.importProject) {
                BuildModeSelector(
                )



                GitHubImportCard(
                    fetcher = githubFetcher,
                    onFetched = { result ->
                        projectPath = result.localPath
                        projectName = result.subPath
                            ?.substringAfterLast('/')
                            ?.takeIf { it.isNotBlank() }
                            ?: result.repo
                        scope.launch {
                            isDetecting = true
                            val detection = ProjectDetector.detectProject(result.localPath)
                            detectionResult = detection
                            if (detection.issues.any { it.type == IssueType.NO_DIST_FOLDER }) {
                                buildMode = BuildMode.FULL_BUILD
                            }
                            isDetecting = false
                        }
                    }
                )


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


                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RuntimeSectionHeader(
                            icon = Icons.Outlined.Folder,
                            title = Strings.selectProject
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (projectPath == null) {
                            PremiumOutlinedButton(
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
                                color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
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
                                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
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
            }


            WtaCreateFlowSection(title = Strings.appConfig) {
                AnimatedVisibility(visible = isDetecting || detectionResult != null) {
                    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            RuntimeSectionHeader(
                                icon = Icons.Outlined.Analytics,
                                title = Strings.projectAnalysis,
                                brandColor = MaterialTheme.colorScheme.secondary
                            ) {
                                if (isDetecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            if (detectionResult != null) {
                                Spacer(modifier = Modifier.height(16.dp))


                                DetectionInfoRow(
                                    icon = Icons.Outlined.Code,
                                    label = Strings.frameworkLabel,
                                    value = getFrameworkDisplayName(detectionResult!!.framework),
                                    valueColor = getFrameworkColor(detectionResult!!.framework)
                                )

                                DetectionInfoRow(
                                    icon = Icons.Outlined.Inventory,
                                    label = Strings.packageManagerLabel,
                                    value = detectionResult!!.packageManager.name
                                )

                                if (detectionResult!!.frameworkVersion != null) {
                                    DetectionInfoRow(
                                        icon = Icons.Outlined.Tag,
                                        label = Strings.versionLabel,
                                        value = detectionResult!!.frameworkVersion!!
                                    )
                                }

                                if (detectionResult!!.hasTypeScript) {
                                    DetectionInfoRow(
                                        icon = Icons.Outlined.Code,
                                        label = "TypeScript",
                                        value = Strings.enabled,
                                        valueColor = Color(0xFF3178C6)
                                    )
                                }


                                val totalDeps = detectionResult!!.dependencies.size +
                                               detectionResult!!.devDependencies.size
                                if (totalDeps > 0) {
                                    DetectionInfoRow(
                                        icon = Icons.Outlined.Extension,
                                        label = Strings.dependencyCountLabel,
                                        value = Strings.dependencyCountValue.format(totalDeps)
                                    )
                                }


                                DetectionInfoRow(
                                    icon = Icons.Outlined.FolderOpen,
                                    label = Strings.outputDirLabel,
                                    value = File(detectionResult!!.outputDir).name
                                )



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


                if (!isEditMode) {
                    AnimatedVisibility(visible = detectionResult != null &&
                        detectionResult?.issues?.none { it.severity == IssueSeverity.ERROR } == true) {
                        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                RuntimeSectionHeader(
                                    icon = Icons.Outlined.Settings,
                                    title = Strings.appConfig,
                                    brandColor = MaterialTheme.colorScheme.tertiary
                                )

                                Spacer(modifier = Modifier.height(16.dp))


                                AppNameTextFieldSimple(
                                    value = projectName,
                                    onValueChange = { projectName = it }
                                )

                                Spacer(modifier = Modifier.height(12.dp))


                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        Strings.labelIcon,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
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
                }
            }


            AnimatedVisibility(visible = currentBuildState !is BuildState.Idle) {
                WtaCreateFlowSection(title = Strings.preview) {
                    BuildStatusCard(currentBuildState, currentLogs.size) {
                        showLogsDialog = true
                    }
                }
            }


            Spacer(modifier = Modifier.height(8.dp))


            WtaCreateFlowSection(title = Strings.save) {
                if (isEditMode && currentBuildState is BuildState.Idle) {
                    PremiumButton(
                        onClick = {

                            onCreated(
                                projectName,
                                "",
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
                            PremiumButton(
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
                            PremiumButton(
                                onClick = {
                                    scope.launch {
                                        val result = nodeBuilder.buildProject(
                                            projectPath!!,
                                            NodeBuildConfig(allowBuiltinPackagerFallback = false)
                                        )
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
                        PremiumButton(
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
                            PremiumOutlinedButton(
                                onClick = { showErrorReportDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.BugReport, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Strings.viewFullError)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            PremiumButton(
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
                        PremiumOutlinedButton(
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
            }

            Spacer(modifier = Modifier.height(32.dp))
        }


    if (showLogsDialog) {
        BuildLogsDialog(
            logs = currentLogs,
            onDismiss = { showLogsDialog = false }
        )
    }

    if (showErrorReportDialog && currentBuildState is BuildState.Error) {
        FullErrorReportDialog(
            title = Strings.fullErrorReport,
            summary = currentBuildState.message,
            report = buildFrontendErrorReport(
                mode = buildMode,
                projectPath = projectPath,
                detectionResult = detectionResult,
                logs = currentLogs,
                summary = currentBuildState.message
            ),
            onDismiss = { showErrorReportDialog = false }
        )
    }
    }
}





@Composable
private fun BuildModeSelector(
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Rocket,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
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


            WtaStatusBanner(
                title = Strings.usageSteps,
                message = Strings.usageStepsContent,
                tone = WtaStatusTone.Info
            )


            Spacer(modifier = Modifier.height(12.dp))

            WtaStatusBanner(
                message = Strings.builtInEngineReady,
                tone = WtaStatusTone.Success
            )
        }
    }
}




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




@Composable
private fun IssueItem(issue: ProjectIssue) {
    val (icon, color) = when (issue.severity) {
        IssueSeverity.ERROR -> Icons.Filled.Error to AppColors.Error
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




@Composable
private fun BuildStatusCard(
    state: BuildState,
    logCount: Int,
    onViewLogs: () -> Unit
) {
    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = when (state) {
            is BuildState.Success -> AppColors.Success.copy(alpha = 0.1f)
            is BuildState.Error -> AppColors.Error.copy(alpha = 0.1f)
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
                            progress = { state.progress },
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
                            progress = { state.progress },
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
                            progress = { state.progress },
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
                            progress = { state.progress },
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
                            tint = AppColors.Success,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(Strings.completed, color = AppColors.Success)
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
                            tint = AppColors.Error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(Strings.failed, color = AppColors.Error)
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

                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))

                TextButton(onClick = onViewLogs) {
                    Text("${Strings.logs} ($logCount)")
                }
            }
        }
    }
}





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
                        LogLevel.ERROR -> AppColors.Error
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

private fun buildFrontendErrorReport(
    mode: BuildMode,
    projectPath: String?,
    detectionResult: ProjectDetectionResult?,
    logs: List<BuildLogEntry>,
    summary: String
): String {
    return buildString {
        appendLine("WebToApp Frontend Build Failure")
        appendLine("mode: ${mode.name}")
        appendLine("projectPath: ${projectPath ?: "null"}")
        appendLine("framework: ${detectionResult?.framework ?: "unknown"}")
        appendLine("outputDir: ${detectionResult?.outputDir ?: "unknown"}")
        appendLine("summary: $summary")
        appendLine()
        appendLine("issues:")
        if (detectionResult?.issues.isNullOrEmpty()) {
            appendLine("none")
        } else {
            detectionResult!!.issues.forEach { issue ->
                appendLine("- ${issue.severity}: ${issue.message}")
                issue.suggestion?.let { appendLine("  suggestion: $it") }
            }
        }
        appendLine()
        appendLine("logs:")
        if (logs.isEmpty()) {
            appendLine("no logs")
        } else {
            logs.forEach { entry ->
                appendLine("[${entry.level}] ${entry.message}")
            }
        }
    }
}

@Composable
private fun FullErrorReportDialog(
    title: String,
    summary: String,
    report: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = report,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .padding(bottom = 48.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    )

                    FilledTonalButton(
                        onClick = { clipboardManager.setText(AnnotatedString(report)) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Strings.copy)
                    }
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




private fun getFrameworkDisplayName(framework: FrontendFramework): String {
    return when (framework) {
        FrontendFramework.VUE -> "Vue.js"
        FrontendFramework.REACT -> "React"
        FrontendFramework.NEXT -> "Next.js"
        FrontendFramework.NUXT -> "Nuxt.js"
        FrontendFramework.ANGULAR -> "Angular"
        FrontendFramework.SVELTE -> "Svelte"
        FrontendFramework.VITE -> "Vite"
        FrontendFramework.UNKNOWN -> Strings.staticWebsite
    }
}





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




private fun getPathFromUri(@Suppress("UNUSED_PARAMETER") context: android.content.Context, uri: Uri): String? {
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




@Composable
private fun GitHubImportCard(
    fetcher: GitHubRepoFetcher,
    onFetched: (GitHubFetchResult) -> Unit
) {
    val scope = rememberCoroutineScope()
    val fetchState by fetcher.state.collectAsStateWithLifecycle()
    val fetchLogs by fetcher.logs.collectAsStateWithLifecycle()

    var url by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var subPath by remember { mutableStateOf("") }
    var showLogs by remember { mutableStateOf(false) }

    val isFetching = fetchState is GitHubFetchState.Resolving ||
        fetchState is GitHubFetchState.Downloading ||
        fetchState is GitHubFetchState.Extracting
    val canFetch = url.isNotBlank() && !isFetching

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.CloudDownload,
                title = Strings.githubImportTitle,
                brandColor = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                Strings.githubImportSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(Strings.githubUrlLabel) },
                placeholder = { Text(Strings.githubUrlPlaceholder) },
                leadingIcon = { Icon(Icons.Outlined.Link, null) },
                singleLine = true,
                enabled = !isFetching,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text(Strings.githubBranchLabel) },
                    placeholder = { Text(Strings.githubBranchPlaceholder) },
                    singleLine = true,
                    enabled = !isFetching,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = subPath,
                    onValueChange = { subPath = it },
                    label = { Text(Strings.githubSubPathLabel) },
                    placeholder = { Text(Strings.githubSubPathPlaceholder) },
                    singleLine = true,
                    enabled = !isFetching,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = fetchState) {
                is GitHubFetchState.Resolving -> GitHubStatusRow(
                    text = Strings.githubResolving,
                    indeterminate = true
                )
                is GitHubFetchState.Downloading -> GitHubStatusRow(
                    text = Strings.githubDownloading,
                    progress = state.progress,
                    indeterminate = state.progress <= 0f
                )
                is GitHubFetchState.Extracting -> GitHubStatusRow(
                    text = Strings.githubExtracting,
                    indeterminate = true
                )
                is GitHubFetchState.Success -> {
                    Text(
                        Strings.githubFetchSuccess,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Success
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is GitHubFetchState.Error -> {
                    Text(
                        Strings.githubFetchFailed.format(state.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {}
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PremiumButton(
                    onClick = {
                        scope.launch {
                            val result = fetcher.fetch(
                                input = url.trim(),
                                explicitBranch = branch.trim().takeIf { it.isNotBlank() },
                                explicitSubPath = subPath.trim().takeIf { it.isNotBlank() }
                            )
                            result.onSuccess(onFetched)
                        }
                    },
                    enabled = canFetch,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.CloudDownload, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.githubFetchAndAnalyze)
                }
                if (fetchLogs.isNotEmpty()) {
                    PremiumOutlinedButton(
                        onClick = { showLogs = true }
                    ) {
                        Icon(Icons.Outlined.Terminal, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${fetchLogs.size}")
                    }
                }
            }
        }
    }

    if (showLogs) {
        AlertDialog(
            onDismissRequest = { showLogs = false },
            title = { Text(Strings.logs) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(fetchLogs) { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogs = false }) { Text(Strings.close) }
            }
        )
    }
}

@Composable
private fun GitHubStatusRow(
    text: String,
    progress: Float = 0f,
    indeterminate: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (indeterminate) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
