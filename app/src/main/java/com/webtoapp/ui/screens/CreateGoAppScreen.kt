package com.webtoapp.ui.screens

import com.webtoapp.ui.theme.AppColors
import com.webtoapp.ui.components.PremiumButton
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.golang.GoRuntime
import com.webtoapp.core.golang.GoSampleManager
import com.webtoapp.data.model.GoAppConfig
import com.webtoapp.ui.components.TypedSampleProjectsCard
import com.webtoapp.ui.components.*
import com.webtoapp.ui.screens.create.WtaCreateFlowScaffold
import com.webtoapp.ui.screens.create.WtaCreateFlowSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File














@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGoAppScreen(
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
    val isEdit = existingAppId > 0L


    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }


    var binaryName by remember { mutableStateOf("") }
    var staticDir by remember { mutableStateOf("") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }


    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }


    var goModulePath by remember { mutableStateOf<String?>(null) }
    var goVersion by remember { mutableStateOf<String?>(null) }
    var goDeps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showAllDeps by remember { mutableStateOf(false) }


    var binarySize by remember { mutableStateOf<Long?>(null) }
    var binaryDetected by remember { mutableStateOf(false) }


    var targetArch by remember { mutableStateOf("arm64") }


    var healthCheckEndpoint by remember { mutableStateOf("/health") }


    var isCreating by remember { mutableStateOf(false) }
    var creationPhase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(existingAppId) {
        if (existingAppId > 0L) {
            val existingApp = org.koin.java.KoinJavaComponent.get<com.webtoapp.data.repository.WebAppRepository>(com.webtoapp.data.repository.WebAppRepository::class.java)
                .getWebAppById(existingAppId).first()
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


                        val detectedBinary = runtime.detectBinary(projectDir)
                        if (detectedBinary != null) {
                            binaryName = detectedBinary
                            binaryDetected = true

                            val searchDirs = listOf(projectDir, File(projectDir, "bin"), File(projectDir, "build"))
                            for (dir in searchDirs) {
                                val binFile = File(dir, detectedBinary)
                                if (binFile.exists()) {
                                    binarySize = binFile.length()
                                    break
                                }
                            }
                        }


                        val detectedStaticDir = runtime.detectStaticDir(projectDir)
                        if (detectedStaticDir.isNotEmpty()) {
                            staticDir = detectedStaticDir
                        }


                        val goMod = File(projectDir, "go.mod")
                        if (goMod.exists()) {
                            try {
                                val content = goMod.readText()
                                val lines = content.lines()


                                lines.firstOrNull { it.startsWith("module ") }?.let { line ->
                                    goModulePath = line.substringAfter("module ").trim()
                                    if (appName.isBlank()) appName = goModulePath!!.substringAfterLast("/")
                                }


                                lines.firstOrNull { it.startsWith("go ") }?.let { line ->
                                    goVersion = line.substringAfter("go ").trim()
                                }


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


    val frameworkColor = remember(detectedFramework) {
        when (detectedFramework?.lowercase()) {
            "gin" -> Color(0xFF0090FF)
            "fiber" -> Color(0xFF8B5CF6)
            "echo" -> Color(0xFF00BCD4)
            "chi" -> AppColors.Error
            "net_http" -> Color(0xFF00ADD8)
            else -> Color(0xFF00ADD8)
        }
    }

    WtaCreateFlowScaffold(
        title = Strings.createGoApp,
        onBack = onBack,
        actions = {
            TextButton(
                onClick = {
                    projectId?.let { pid ->
                        onCreated(
                            appName.ifBlank { Strings.createGoApp },
                            GoAppConfig(
                                projectId = pid,
                                projectName = appName.ifBlank { Strings.createGoApp },
                                framework = detectedFramework ?: "raw",
                                binaryName = binaryName,
                                envVars = envVars,
                                staticDir = staticDir,
                                hasBuildFromSource = binaryName.isEmpty(),
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
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WtaCreateFlowSection(title = Strings.importProject) {

            GoHeroSection(
                detectedFramework = detectedFramework,
                frameworkColor = frameworkColor,
                goVersion = goVersion
            )


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


            if (!isEdit) {
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RuntimeSectionHeader(
                        icon = Icons.Outlined.Settings,
                        title = Strings.njsBasicConfig
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text(Strings.labelAppName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }


            RuntimeIconPickerCard(
                appIcon = appIcon,
                onSelectIcon = { iconPickerLauncher.launch("image/*") }
            )
            }


            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RuntimeSectionHeader(
                        icon = Icons.Outlined.Folder,
                        title = Strings.goSelectProject
                    )
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
            }


            WtaCreateFlowSection(title = Strings.appConfig) {
            if (projectId != null) {


                if (goModulePath != null) {
                    GoModuleInfoCard(
                        modulePath = goModulePath!!,
                        goVersion = goVersion,
                        depCount = goDeps.size,
                        frameworkColor = frameworkColor
                    )
                }


                GoBinaryDetectionCard(
                    binaryDetected = binaryDetected,
                    binaryName = binaryName,
                    binarySize = binarySize,
                    onBinaryNameChange = { binaryName = it },
                    frameworkColor = frameworkColor
                )


                GoTargetArchCard(
                    targetArch = targetArch,
                    onArchChange = { targetArch = it },
                    frameworkColor = frameworkColor
                )


                GoStaticFilesCard(
                    staticDir = staticDir,
                    onStaticDirChange = { staticDir = it }
                )


                GoHealthCheckCard(
                    endpoint = healthCheckEndpoint,
                    onEndpointChange = { healthCheckEndpoint = it }
                )


                if (goDeps.isNotEmpty()) {
                    GoDepsCard(
                        deps = goDeps,
                        showAll = showAllDeps,
                        onToggleShowAll = { showAllDeps = !showAllDeps },
                        frameworkColor = frameworkColor
                    )
                }


                GoFrameworkTipCard(framework = detectedFramework)


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
            }


            WtaCreateFlowSection(title = Strings.preview) {
                if (isCreating) {
                    RuntimeLoadingCard(creationPhase)
                }

                errorMessage?.let { error ->
                    RuntimeErrorCard(error = error, onDismiss = { errorMessage = null })
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}






@Composable
private fun GoHeroSection(
    detectedFramework: String?,
    frameworkColor: Color,
    goVersion: String?
) {
    val title = if (detectedFramework != null && detectedFramework != "raw" && detectedFramework != "net_http")
        "${detectedFramework.replaceFirstChar { it.uppercase() }} ${Strings.goHeroTitle}"
    else Strings.goHeroTitle

    val tags = buildList {
        goVersion?.let { add("Go $it" to frameworkColor) }
    }

    RuntimeHeroSection(
        icon = Icons.Outlined.Code,
        title = title,
        subtitle = Strings.goHeroDesc,
        brandColor = frameworkColor,
        tags = tags
    )
}




@Composable
private fun GoModuleInfoCard(
    modulePath: String,
    goVersion: String?,
    depCount: Int,
    frameworkColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Info,
                title = Strings.goModuleInfo,
                brandColor = frameworkColor
            )
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
            RuntimeSectionHeader(
                icon = Icons.Outlined.Memory,
                title = Strings.goBinaryDetection
            )
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
            RuntimeSectionHeader(
                icon = Icons.Outlined.DeveloperBoard,
                title = Strings.goTargetArch
            )
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




@Composable
private fun GoStaticFilesCard(
    staticDir: String,
    onStaticDirChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Folder,
                title = Strings.goStaticFiles
            )
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
                placeholder = { Text(Strings.goStaticFilesExample) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}




@Composable
private fun GoHealthCheckCard(
    endpoint: String,
    onEndpointChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.MonitorHeart,
                title = Strings.goHealthCheck
            )
            Spacer(modifier = Modifier.height(12.dp))
            PremiumTextField(
                value = endpoint,
                onValueChange = onEndpointChange,
                label = { Text(Strings.goHealthCheckEndpoint) },
                placeholder = { Text(Strings.goHealthCheckEndpointExample) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}




@Composable
private fun GoDepsCard(
    deps: List<Pair<String, String>>,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    frameworkColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Extension,
                title = Strings.goDirectDeps,
                brandColor = frameworkColor
            ) {
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




@Composable
private fun GoFrameworkTipCard(framework: String?) {
    data class Tip(val tip: String, val color: Color)

    val tipText = Strings.goFrameworkTip(framework)
    val tipColor = when (framework?.lowercase()) {
        "gin" -> Color(0xFF0090FF)
        "fiber" -> Color(0xFF8B5CF6)
        "echo" -> Color(0xFF00BCD4)
        "chi" -> AppColors.Error
        "net_http" -> Color(0xFF00ADD8)
        else -> null
    }
    val tipData = if (tipText != null && tipColor != null) Tip(tipText, tipColor) else null

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




private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
