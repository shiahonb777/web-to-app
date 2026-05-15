package com.webtoapp.ui.screens
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.php.PhpAppRuntime
import com.webtoapp.core.php.PhpSampleManager
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.ui.components.TypedSampleProjectsCard
import com.webtoapp.data.model.PhpAppConfig
import com.webtoapp.ui.components.*
import com.webtoapp.ui.screens.create.WtaCreateFlowScaffold
import com.webtoapp.ui.screens.create.WtaCreateFlowSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.zip.ZipInputStream












@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePhpAppScreen(
    existingAppId: Long = 0L,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        phpAppConfig: PhpAppConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit,
    onOpenLinuxEnv: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEdit = existingAppId > 0L


    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }


    var documentRoot by remember { mutableStateOf("") }
    var entryFile by remember { mutableStateOf("index.php") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }


    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }


    var composerDeps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var composerDevDeps by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showAllDeps by remember { mutableStateOf(false) }
    var showAllDevDeps by remember { mutableStateOf(false) }


    var detectedWebDirs by remember { mutableStateOf<List<String>>(emptyList()) }
    var useCustomDocRoot by remember { mutableStateOf(false) }


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


    var detectedDbFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var sqlitePath by remember { mutableStateOf("") }


    var frameworkVersion by remember { mutableStateOf<String?>(null) }


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


    val downloadState by WordPressDependencyManager.downloadState.collectAsStateWithLifecycle()
    var showDownloadDialog by remember { mutableStateOf(false) }


    val processProjectDir: suspend (File) -> Unit = processProject@{ inputDir ->

        val projectDir = resolvePhpProjectRoot(inputDir)
        selectedProjectDir = projectDir.absolutePath

        val runtime = PhpAppRuntime(context)
        val framework = runtime.detectFramework(projectDir)
        detectedFramework = framework

        val detectedDocRoot = runtime.detectDocumentRoot(projectDir, framework)
        if (detectedDocRoot.isNotEmpty()) {
            documentRoot = detectedDocRoot
        }

        val detected = runtime.detectEntryFile(projectDir, detectedDocRoot)
        entryFile = detected


        val possibleDirs = listOf("public", "www", "htdocs", "web", "webroot", "html")
        detectedWebDirs = possibleDirs.filter { File(projectDir, it).isDirectory }


        val dbExtensions = listOf(".db", ".sqlite", ".sqlite3")
        detectedDbFiles = projectDir.walk().maxDepth(3)
            .filter { f -> dbExtensions.any { f.name.endsWith(it) } }
            .map { it.relativeTo(projectDir).path }
            .toList()
        if (detectedDbFiles.isNotEmpty()) {
            sqlitePath = detectedDbFiles.first()
        }


        val composerJson = File(projectDir, "composer.json")
        if (composerJson.exists()) {
            try {
                val content = composerJson.readText()
                val gson = com.google.gson.Gson()
                val json = gson.fromJson(content, com.google.gson.JsonObject::class.java)
                json.get("name")?.asString?.let { name ->
                    if (appName.isBlank()) appName = name.substringAfterLast("/")
                }


                json.getAsJsonObject("require")?.let { req ->
                    composerDeps = req.entrySet()
                        .filter { it.key != "php" }
                        .associate { it.key to it.value.asString }
                }
                json.getAsJsonObject("require-dev")?.let { dev ->
                    composerDevDeps = dev.entrySet()
                        .associate { it.key to it.value.asString }
                }


                json.getAsJsonObject("require")?.get("php")?.asString?.let {
                    frameworkVersion = it
                }


                val allDepKeys = (composerDeps.keys + composerDevDeps.keys).toSet()
                if (allDepKeys.any { it.contains("gd") || it.contains("image") || it.contains("intervention") }) {
                    phpExtensions = phpExtensions.toMutableMap().apply { put("gd", true) }
                }
                if (allDepKeys.any { it.contains("zip") || it.contains("archive") }) {
                    phpExtensions = phpExtensions.toMutableMap().apply { put("zip", true) }
                }
                if (allDepKeys.any { it.contains("xml") || it.contains("soap") }) {
                    phpExtensions = phpExtensions.toMutableMap().apply { put("xml", true) }
                }
                if (allDepKeys.any { it.contains("curl") || it.contains("guzzle") || it.contains("http") }) {
                    phpExtensions = phpExtensions.toMutableMap().apply { put("curl", true) }
                }
            } catch (e: Exception) { android.util.Log.w("CreatePhpApp", "Failed to parse composer.json dependencies", e) }
        }


        if (!WordPressDependencyManager.isPhpReady(context)) {
            showDownloadDialog = true
            val success = WordPressDependencyManager.downloadAllDependencies(context)
            showDownloadDialog = false
            if (!success) {
                errorMessage = Strings.wpDownloadFailed
                isCreating = false
                return@processProject
            }
        }


        creationPhase = Strings.copyingProjectFiles
        val newProjectId = java.util.UUID.randomUUID().toString()
        runtime.createProject(newProjectId, projectDir)
        projectId = newProjectId
        creationPhase = Strings.phpProjectReady
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
                creationPhase = Strings.copyingProjectFiles
                errorMessage = null

                try {
                    withContext(Dispatchers.IO) {

                        val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
                        if (treeDoc == null || !treeDoc.exists()) {
                            errorMessage = Strings.dirNotExists
                            isCreating = false
                            return@withContext
                        }

                        val tempDir = File(context.cacheDir, "php_saf_import_${System.currentTimeMillis()}")
                        tempDir.mkdirs()
                        copyDocumentTreeToLocal(context, treeDoc, tempDir)

                        if (tempDir.listFiles().isNullOrEmpty()) {
                            errorMessage = Strings.dirNotExists
                            tempDir.deleteRecursively()
                            isCreating = false
                            return@withContext
                        }

                        creationPhase = Strings.phpFrameworkDetected
                        processProjectDir(tempDir)


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


    val zipPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { zipUri ->
            scope.launch {
                isCreating = true
                creationPhase = Strings.phpExtractingZip
                errorMessage = null

                try {
                    withContext(Dispatchers.IO) {

                        val extractDir = File(context.cacheDir, "php_zip_extract_${System.currentTimeMillis()}")
                        extractDir.mkdirs()

                        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                            ZipInputStream(inputStream).use { zis ->
                                var entry = zis.nextEntry
                                while (entry != null) {

                                    val name = entry.name
                                    if (!entry.isDirectory && !name.startsWith("__MACOSX/") && !name.substringAfterLast("/").startsWith("._")) {
                                        val outFile = File(extractDir, name)
                                        outFile.parentFile?.mkdirs()
                                        outFile.outputStream().use { out ->
                                            zis.copyTo(out)
                                        }
                                    }
                                    zis.closeEntry()
                                    entry = zis.nextEntry
                                }
                            }
                        } ?: run {
                            errorMessage = Strings.phpZipExtractFailed
                            isCreating = false
                            return@withContext
                        }


                        val children = extractDir.listFiles()
                        val projectDir = if (children != null && children.size == 1 && children[0].isDirectory) {
                            children[0]
                        } else {
                            extractDir
                        }


                        val hasPhpFiles = projectDir.walk().maxDepth(3).any { it.extension == "php" }
                        if (!hasPhpFiles) {
                            errorMessage = Strings.phpZipNoPhpFiles
                            extractDir.deleteRecursively()
                            isCreating = false
                            return@withContext
                        }

                        creationPhase = Strings.phpFrameworkDetected
                        processProjectDir(projectDir)


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


    val accentColor = MaterialTheme.colorScheme.onSurface

    WtaCreateFlowScaffold(
        title = Strings.createPhpApp,
        onBack = onBack,
        actions = {
            TextButton(
                onClick = {
                    projectId?.let { pid ->
                        onCreated(
                            appName.ifBlank { Strings.createPhpApp },
                            PhpAppConfig(
                                projectId = pid,
                                projectName = appName.ifBlank { Strings.createPhpApp },
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
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            WtaCreateFlowSection(title = Strings.importProject) {
                PhpHeroSection(
                    detectedFramework = detectedFramework,
                    accentColor = accentColor,
                    frameworkVersion = frameworkVersion
                )


            if (selectedProjectDir == null) {
                TypedSampleProjectsCard(
                    title = Strings.sampleProjects,
                    subtitle = Strings.samplePhpSubtitle,
                    samples = remember { PhpSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = PhpSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                selectedProjectDir = path
                                isCreating = true
                                creationPhase = Strings.phpFrameworkDetected
                                try {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val projectDir = java.io.File(path)
                                        val runtime = PhpAppRuntime(context)
                                        val framework = runtime.detectFramework(projectDir)
                                        detectedFramework = framework
                                        val detectedDocRoot = runtime.detectDocumentRoot(projectDir, framework)
                                        if (detectedDocRoot.isNotEmpty()) documentRoot = detectedDocRoot
                                        entryFile = runtime.detectEntryFile(projectDir, detectedDocRoot)
                                        appName = sample.name
                                        creationPhase = Strings.copyingProjectFiles
                                        val newProjectId = java.util.UUID.randomUUID().toString()
                                        runtime.createProject(newProjectId, projectDir)
                                        projectId = newProjectId
                                        creationPhase = Strings.phpProjectReady
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
                        title = Strings.phpSelectProject
                    )
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
                            color = accentColor.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = accentColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Strings.phpProjectReady, style = MaterialTheme.typography.bodyMedium, color = accentColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(selectedProjectDir!!.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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


            WtaCreateFlowSection(title = Strings.appConfig) {
            if (projectId != null) {


                if (composerDeps.isNotEmpty() || composerDevDeps.isNotEmpty()) {
                    PhpComposerDepsCard(
                        deps = composerDeps,
                        devDeps = composerDevDeps,
                        showAllDeps = showAllDeps,
                        showAllDevDeps = showAllDevDeps,
                        onToggleDeps = { showAllDeps = !showAllDeps },
                        onToggleDevDeps = { showAllDevDeps = !showAllDevDeps },
                        accentColor = accentColor
                    )
                }

                // 在 App 内一键 composer install——只在项目有 composer.json 时显示
                if (selectedProjectDir != null && File(selectedProjectDir!!, "composer.json").exists()) {
                    InstallProjectDepsCard(
                        kind = DepsKind.PHP,
                        projectDir = selectedProjectDir,
                        accentColor = accentColor,
                        onOpenBuildEnvScreen = onOpenLinuxEnv,
                    )
                }


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


                PhpExtensionsCard(
                    extensions = phpExtensions,
                    onToggle = { ext, enabled ->
                        phpExtensions = phpExtensions.toMutableMap().apply { put(ext, enabled) }
                    }
                )


                if (detectedDbFiles.isNotEmpty()) {
                    PhpDatabaseCard(
                        detectedDbFiles = detectedDbFiles,
                        sqlitePath = sqlitePath,
                        onPathChange = { sqlitePath = it }
                    )
                }


                PhpFrameworkTipCard(framework = detectedFramework)


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






@Composable
private fun PhpHeroSection(
    detectedFramework: String?,
    accentColor: Color,
    frameworkVersion: String?
) {
    val title = if (detectedFramework != null && detectedFramework != "raw")
        "$detectedFramework ${Strings.phpHeroTitle}"
    else Strings.phpHeroTitle

    val tags = buildList {
        frameworkVersion?.let { add("PHP $it" to accentColor) }
    }

    RuntimeHeroSection(
        icon = Icons.Outlined.Code,
        title = title,
        subtitle = Strings.phpHeroDesc,
        brandColor = accentColor,
        tags = tags
    )
}




@Composable
private fun PhpComposerDepsCard(
    deps: Map<String, String>,
    devDeps: Map<String, String>,
    showAllDeps: Boolean,
    showAllDevDeps: Boolean,
    onToggleDeps: () -> Unit,
    onToggleDevDeps: () -> Unit,
    accentColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Extension,
                title = Strings.phpComposerDeps,
                brandColor = accentColor
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${deps.size + devDeps.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
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
            RuntimeSectionHeader(
                icon = Icons.Outlined.FolderOpen,
                title = Strings.phpDocRootSelect
            )
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
                        placeholder = { Text(Strings.phpDocRootExample) },
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




@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PhpExtensionsCard(
    extensions: Map<String, Boolean>,
    onToggle: (String, Boolean) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Extension,
                title = Strings.phpExtensions,
                brandColor = MaterialTheme.colorScheme.onSurface
            )
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




@Composable
private fun PhpDatabaseCard(
    detectedDbFiles: List<String>,
    sqlitePath: String,
    onPathChange: (String) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Storage,
                title = Strings.phpDatabaseConfig,
                brandColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
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
        val tipColor = MaterialTheme.colorScheme.onSurface

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





private fun copyDocumentTreeToLocal(context: Context, docDir: DocumentFile, destDir: File) {
    docDir.listFiles().forEach { child ->
        val name = child.name ?: return@forEach

        if (name == "__MACOSX" || name.startsWith("._")) return@forEach

        if (child.isDirectory) {
            val subDir = File(destDir, name)
            subDir.mkdirs()
            copyDocumentTreeToLocal(context, child, subDir)
        } else if (child.isFile) {
            val destFile = File(destDir, name)
            try {
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) { android.util.Log.w("CreatePhpApp", "Failed to copy file: $name", e) }
        }
    }
}





private fun resolvePhpProjectRoot(dir: File): File {

    if (dir.listFiles()?.any { it.isFile && it.extension == "php" } == true) return dir


    val phpSubDir = dir.listFiles()
        ?.filter { it.isDirectory && it.name != "__MACOSX" && !it.name.startsWith("._") }
        ?.firstOrNull { sub -> sub.listFiles()?.any { it.isFile && it.extension == "php" } == true }

    return phpSubDir ?: dir
}
