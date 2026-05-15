package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import com.webtoapp.core.python.PythonRuntime
import com.webtoapp.core.python.PythonSampleManager
import com.webtoapp.data.model.PythonAppConfig
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
fun CreatePythonAppScreen(
    existingAppId: Long = 0L,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        pythonAppConfig: PythonAppConfig,
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


    var entryFile by remember { mutableStateOf("app.py") }
    var entryModule by remember { mutableStateOf("") }
    var serverType by remember { mutableStateOf("builtin") }
    var landscapeMode by remember { mutableStateOf(false) }
    var envVars by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }


    var selectedProjectDir by remember { mutableStateOf<String?>(null) }
    var detectedFramework by remember { mutableStateOf<String?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }


    var requirements by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var requirementsSource by remember { mutableStateOf("") }
    var showAllDeps by remember { mutableStateOf(false) }


    var venvDetected by remember { mutableStateOf(false) }
    var venvPath by remember { mutableStateOf<String?>(null) }


    var pythonVersion by remember { mutableStateOf<String?>(null) }


    var djangoSettingsModule by remember { mutableStateOf("") }
    var djangoStaticDir by remember { mutableStateOf("static") }


    var fastapiDocsEnabled by remember { mutableStateOf(true) }


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
                creationPhase = Strings.frameworkDetected
                errorMessage = null

                try {
                    withContext(Dispatchers.IO) {
                        val runtime = PythonRuntime(context)



                        creationPhase = Strings.copyingProjectFiles
                        val newProjectId = java.util.UUID.randomUUID().toString()


                        val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                        val path = docId.substringAfter(":")
                        val storageRoot = if (docId.startsWith("primary:")) {
                            android.os.Environment.getExternalStorageDirectory().absolutePath
                        } else {
                            "/storage/${docId.substringBefore(":")}"
                        }
                        val projectPath = "$storageRoot/$path"
                        val projectFileDir = File(projectPath)


                        var copiedDir: File
                        if (projectFileDir.exists() && projectFileDir.canRead()) {
                            copiedDir = runtime.createProject(newProjectId, projectFileDir)
                        } else {
                            copiedDir = runtime.getProjectDir(newProjectId)
                            copiedDir.mkdirs()
                        }


                        val copiedFileCount = copiedDir.walkTopDown().filter { it.isFile }.count()
                        if (copiedFileCount == 0) {


                            android.util.Log.w("CreatePythonApp", "File API 复制了 0 个文件，回退 SAF API: $treeUri")
                            copiedDir = runtime.createProjectFromUri(newProjectId, treeUri, context)
                            val safCopiedCount = copiedDir.walkTopDown().filter { it.isFile }.count()
                            android.util.Log.i("CreatePythonApp", "SAF API 复制了 $safCopiedCount 个文件")
                            if (safCopiedCount == 0) {
                                errorMessage = Strings.dirNotExists
                                isCreating = false
                                return@withContext
                            }
                        }

                        selectedProjectDir = projectPath
                        projectId = newProjectId



                        val framework = runtime.detectFramework(copiedDir)
                        detectedFramework = framework

                        val detected = runtime.detectEntryFile(copiedDir, framework)
                        entryFile = detected


                        serverType = when (framework) {
                            "fastapi" -> "uvicorn"
                            "django" -> "gunicorn"
                            else -> "builtin"
                        }


                        val venvDirs = listOf("venv", ".venv", "env", ".env")
                        for (vDir in venvDirs) {
                            val venvDir = File(copiedDir, vDir)
                            if (venvDir.isDirectory && File(venvDir, "bin/python").exists()) {
                                venvDetected = true
                                venvPath = vDir
                                break
                            }

                            if (venvDir.isDirectory && File(venvDir, "Scripts/python.exe").exists()) {
                                venvDetected = true
                                venvPath = vDir
                                break
                            }
                        }


                        val reqFile = File(copiedDir, "requirements.txt")
                        val pipfileFile = File(copiedDir, "Pipfile")
                        val pyprojectFile = File(copiedDir, "pyproject.toml")

                        if (reqFile.exists()) {
                            requirementsSource = "requirements.txt"
                            try {
                                requirements = reqFile.readLines()
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("-") }
                                    .map { line ->
                                        val parts = line.split(Regex("[>=<~!]+"), 2)
                                        val name = parts[0].trim()
                                        val version = if (parts.size > 1) {
                                            line.substring(name.length).trim()
                                        } else ""
                                        name to version
                                    }
                            } catch (e: Exception) { android.util.Log.w("CreatePythonApp", "Failed to parse requirements.txt", e) }
                        } else if (pipfileFile.exists()) {
                            requirementsSource = "Pipfile"
                            try {
                                val content = pipfileFile.readText()
                                val packagesSection = content.substringAfter("[packages]", "")
                                    .substringBefore("[", "")
                                requirements = packagesSection.lines()
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() && it.contains("=") }
                                    .map { line ->
                                        val key = line.substringBefore("=").trim()
                                        val value = line.substringAfter("=").trim().removeSurrounding("\"")
                                        key to value
                                    }
                            } catch (e: Exception) { android.util.Log.w("CreatePythonApp", "Failed to parse Pipfile", e) }
                        }


                        if (pyprojectFile.exists()) {
                            try {
                                val content = pyprojectFile.readText()
                                val nameMatch = Regex("""name\s*=\s*"([^"]+)"""").find(content)
                                nameMatch?.groupValues?.get(1)?.let { if (appName.isBlank()) appName = it }

                                val pyVerMatch = Regex("""requires-python\s*=\s*"([^"]+)"""").find(content)
                                pyVerMatch?.groupValues?.get(1)?.let { pythonVersion = it }

                                // 如果没有 requirements.txt，尝试从 pyproject.toml 解析依赖
                                if (requirements.isEmpty()) {
                                    requirementsSource = "pyproject.toml"
                                    val depsBlock = content.substringAfter("dependencies", "")
                                        .substringAfter("[", "").substringBefore("]", "")
                                    if (depsBlock.isNotBlank()) {
                                        requirements = depsBlock.lines()
                                            .map { it.trim().removeSurrounding("\"").removeSurrounding(",").trim() }
                                            .filter { it.isNotEmpty() && !it.startsWith("#") }
                                            .map { line ->
                                                val parts = line.split(Regex("[>=<~!]+"), 2)
                                                val name = parts[0].trim()
                                                val version = if (parts.size > 1) line.substring(name.length).trim() else ""
                                                name to version
                                            }
                                    }
                                }
                            } catch (e: Exception) { android.util.Log.w("CreatePythonApp", "Failed to parse pyproject.toml", e) }
                        }

                        // 读取项目名称 (setup.py fallback)
                        val setupPy = File(copiedDir, "setup.py")
                        if (setupPy.exists() && appName.isBlank()) {
                            try {
                                val content = setupPy.readText()
                                val nameMatch = Regex("""name\s*=\s*['"]([^'"]+)['"]""").find(content)
                                nameMatch?.groupValues?.get(1)?.let { appName = it }
                            } catch (e: Exception) { android.util.Log.w("CreatePythonApp", "Failed to parse setup.py", e) }
                        }

                        // 增强：Django 专属检测
                        if (framework == "django") {
                            // 检测 settings module
                            val managePy = File(copiedDir, "manage.py")
                            if (managePy.exists()) {
                                try {
                                    val content = managePy.readText()
                                    val settingsMatch = Regex("""DJANGO_SETTINGS_MODULE.*?['"]([^'"]+)['"]""").find(content)
                                    settingsMatch?.groupValues?.get(1)?.let {
                                        djangoSettingsModule = it
                                        entryModule = "$it.wsgi:application"
                                    }
                                } catch (e: Exception) { android.util.Log.w("CreatePythonApp", "Failed to parse manage.py", e) }
                            }
                            // 检测 wsgi.py
                            copiedDir.walk().maxDepth(2).filter { it.name == "wsgi.py" }.firstOrNull()?.let {
                                val modulePath = it.relativeTo(copiedDir).path
                                    .removeSuffix(".py").replace(File.separator, ".")
                                if (entryModule.isBlank()) entryModule = "$modulePath:application"
                            }
                        }

                        // 增强：FastAPI 专属检测
                        if (framework == "fastapi") {
                            // 推断 ASGI module
                            val mainPy = File(copiedDir, entryFile)
                            if (mainPy.exists()) {
                                try {
                                    val content = mainPy.readText()
                                    val appVarMatch = Regex("""(\w+)\s*=\s*FastAPI\(""").find(content)
                                    val appVar = appVarMatch?.groupValues?.get(1) ?: "app"
                                    val moduleName = entryFile.removeSuffix(".py")
                                    if (entryModule.isBlank()) entryModule = "$moduleName:$appVar"
                                } catch (_: Exception) {
                                    if (entryModule.isBlank()) entryModule = "main:app"
                                }
                            }
                        }

                        // 读取 .env 文件
                        val envFile = File(copiedDir, ".env")
                        if (envFile.exists()) {
                            envFile.readLines().forEach { line ->
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

                        creationPhase = Strings.pyProjectReady
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

    val accentColor = MaterialTheme.colorScheme.onSurface

    WtaCreateFlowScaffold(
        title = Strings.createPythonApp,
        onBack = onBack,
        actions = {
            TextButton(
                onClick = {
                    projectId?.let { pid ->
                        onCreated(
                            appName.ifBlank { Strings.createPythonApp },
                            PythonAppConfig(
                                projectId = pid,
                                projectName = appName.ifBlank { Strings.createPythonApp },
                                framework = detectedFramework ?: "raw",
                                entryFile = entryFile,
                                entryModule = entryModule,
                                serverType = serverType,
                                envVars = envVars,
                                requirementsFile = if (selectedProjectDir?.let { File(it, "requirements.txt").exists() } == true) "requirements.txt" else "",
                                hasPipDeps = selectedProjectDir?.let { File(it, "requirements.txt").exists() } ?: false,
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
            // ========== 1. 框架品牌化 Hero 区域 ==========
            PythonHeroSection(
                detectedFramework = detectedFramework,
                accentColor = accentColor,
                pythonVersion = pythonVersion
            )

            // ========== 示例项目 ==========
            if (selectedProjectDir == null) {
                TypedSampleProjectsCard(
                    title = Strings.sampleProjects,
                    subtitle = Strings.samplePythonSubtitle,
                    samples = remember { PythonSampleManager.getSampleProjects() },
                    onSelectSample = { sample ->
                        scope.launch {
                            val result = PythonSampleManager.extractSampleProject(context, sample.id)
                            result.onSuccess { path ->
                                selectedProjectDir = path
                                isCreating = true
                                creationPhase = Strings.frameworkDetected
                                try {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        val projectDir = java.io.File(path)
                                        val runtime = PythonRuntime(context)
                                        val framework = runtime.detectFramework(projectDir)
                                        detectedFramework = framework
                                        entryFile = runtime.detectEntryFile(projectDir, framework)
                                        serverType = when (framework) {
                                            "fastapi" -> "uvicorn"
                                            "django" -> "gunicorn"
                                            else -> "builtin"
                                        }
                                        appName = sample.name
                                        creationPhase = Strings.copyingProjectFiles
                                        val newProjectId = java.util.UUID.randomUUID().toString()
                                        runtime.createProject(newProjectId, projectDir)
                                        projectId = newProjectId
                                        creationPhase = Strings.pyProjectReady
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

            // ========== 2. 基本配置（仅新建时显示，编辑时在通用配置中设置） ==========
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

            // ========== 3. 图标选择 ==========
            RuntimeIconPickerCard(
                appIcon = appIcon,
                onSelectIcon = { iconPickerLauncher.launch("image/*") }
            )
            }

            // ========== 4. 项目选择 ==========
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RuntimeSectionHeader(
                        icon = Icons.Outlined.Folder,
                        title = Strings.pySelectProject
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings.pySupportedFrameworks,
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
                                    Text(Strings.pyProjectReady, style = MaterialTheme.typography.bodyMedium, color = accentColor)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selectedProjectDir!!.substringAfterLast("/"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (detectedFramework != null && detectedFramework != "raw") {
                                    Text(
                                        "${Strings.frameworkDetected}: $detectedFramework",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${Strings.entryFile}: $entryFile",
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
                        Text(Strings.pySelectProject)
                    }
                }
            }
            }

            // ========== 以下卡片仅在项目选择后显示 ==========
            WtaCreateFlowSection(title = Strings.appConfig) {
            if (projectId != null) {

                // ========== 5. 虚拟环境指示器 ==========
                PythonVenvIndicator(
                    venvDetected = venvDetected,
                    venvPath = venvPath,
                    accentColor = accentColor
                )

                // ========== 6. 依赖面板 ==========
                if (requirements.isNotEmpty()) {
                    PythonRequirementsCard(
                        requirements = requirements,
                        source = requirementsSource,
                        showAll = showAllDeps,
                        onToggleShowAll = { showAllDeps = !showAllDeps },
                        accentColor = accentColor
                    )
                }

                // 在 App 内一键 pip install -r requirements.txt
                if (selectedProjectDir != null && File(selectedProjectDir!!, "requirements.txt").exists()) {
                    InstallProjectDepsCard(
                        kind = DepsKind.PYTHON,
                        projectDir = selectedProjectDir,
                        accentColor = accentColor,
                        onOpenBuildEnvScreen = onOpenLinuxEnv,
                    )
                }

                // ========== 7. 服务器类型选择器 ==========
                PythonServerTypeCard(
                    serverType = serverType,
                    detectedFramework = detectedFramework,
                    onServerTypeChange = { serverType = it },
                    accentColor = accentColor
                )

                // ========== 8. WSGI/ASGI 模块配置 ==========
                PythonModuleConfigCard(
                    entryFile = entryFile,
                    onEntryFileChange = { entryFile = it },
                    entryModule = entryModule,
                    onEntryModuleChange = { entryModule = it },
                    serverType = serverType
                )

                // ========== 9. Django 专属面板 ==========
                if (detectedFramework == "django") {
                    PythonDjangoCard(
                        settingsModule = djangoSettingsModule,
                        onSettingsModuleChange = { djangoSettingsModule = it },
                        staticDir = djangoStaticDir,
                        onStaticDirChange = { djangoStaticDir = it }
                    )
                }

                // ========== 10. FastAPI 专属面板 ==========
                if (detectedFramework == "fastapi") {
                    PythonFastapiCard(
                        docsEnabled = fastapiDocsEnabled,
                        onDocsEnabledChange = { fastapiDocsEnabled = it }
                    )
                }

                // ========== 11. 框架提示 ==========
                PythonFrameworkTipCard(framework = detectedFramework)

                // ========== 12. 环境变量 ==========
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

            // 状态提示
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

// ==================== 私有 Composable 组件 ====================

/**
 * Python 框架品牌化 Hero 区域
 */
@Composable
private fun PythonHeroSection(
    detectedFramework: String?,
    accentColor: Color,
    pythonVersion: String?
) {
    val title = if (detectedFramework != null && detectedFramework != "raw")
        "${detectedFramework.replaceFirstChar { it.uppercase() }} ${Strings.pyHeroTitle}"
    else Strings.pyHeroTitle

    val tags = buildList {
        pythonVersion?.let { add("Python $it" to accentColor) }
    }

    RuntimeHeroSection(
        icon = Icons.Outlined.Code,
        title = title,
        subtitle = Strings.pyHeroDesc,
        brandColor = accentColor,
        tags = tags
    )
}

/**
 * 虚拟环境检测指示器
 */
@Composable
private fun PythonVenvIndicator(
    venvDetected: Boolean,
    venvPath: String?,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (venvDetected) accentColor.copy(alpha = 0.08f)
        else if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (venvDetected) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                null,
                tint = if (venvDetected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Text(
                    text = Strings.pyVenvDetected,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (venvDetected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (venvDetected) "${Strings.pyVenvFound} ($venvPath/)"
                    else Strings.pyVenvNotFound,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (venvDetected) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "venv",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    )
                }
            }
        }
    }
}

/**
 * 依赖列表面板
 */
@Composable
private fun PythonRequirementsCard(
    requirements: List<Pair<String, String>>,
    source: String,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    accentColor: Color
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Extension,
                title = Strings.pyRequirements,
                brandColor = accentColor
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${requirements.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${Strings.pyRequirementsFile}: $source",
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
                    Text(if (showAll) Strings.close else "${Strings.more} (${requirements.size - 8})")
                }
            }
        }
    }
}

/**
 * 服务器类型可视化选择器
 */
@Composable
private fun PythonServerTypeCard(
    serverType: String,
    detectedFramework: String?,
    onServerTypeChange: (String) -> Unit,
    accentColor: Color
) {
    val recommendedServer = when (detectedFramework?.lowercase()) {
        "fastapi" -> "uvicorn"
        "django" -> "gunicorn"
        "flask" -> "gunicorn"
        else -> null
    }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Dns,
                title = Strings.pyServerType
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Builtin
            PythonServerOption(
                title = Strings.pyServerBuiltin,
                description = Strings.pyServerBuiltinDesc,
                selected = serverType == "builtin",
                isRecommended = recommendedServer == null,
                onClick = { onServerTypeChange("builtin") },
                accentColor = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Gunicorn (WSGI)
            PythonServerOption(
                title = Strings.pyServerGunicorn,
                description = Strings.pyServerGunicornDesc,
                selected = serverType == "gunicorn",
                isRecommended = recommendedServer == "gunicorn",
                onClick = { onServerTypeChange("gunicorn") },
                accentColor = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Uvicorn (ASGI)
            PythonServerOption(
                title = Strings.pyServerUvicorn,
                description = Strings.pyServerUvicornDesc,
                selected = serverType == "uvicorn",
                isRecommended = recommendedServer == "uvicorn",
                onClick = { onServerTypeChange("uvicorn") },
                accentColor = accentColor
            )
        }
    }
}

/**
 * 单个服务器选项卡片
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
                                Strings.pyRecommended,
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
 * WSGI/ASGI 模块与入口文件配置卡片
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
            RuntimeSectionHeader(
                icon = Icons.Outlined.Code,
                title = Strings.phpAdvancedConfig
            )
            Spacer(modifier = Modifier.height(12.dp))

            PremiumTextField(
                value = entryFile,
                onValueChange = onEntryFileChange,
                label = { Text(Strings.entryFile) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            AnimatedVisibility(visible = serverType != "builtin") {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumTextField(
                        value = entryModule,
                        onValueChange = onEntryModuleChange,
                        label = { Text(Strings.pyWsgiModule) },
                        placeholder = { Text(Strings.pyWsgiModuleHint) },
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
 * Django 专属配置面板
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
            RuntimeSectionHeader(
                icon = Icons.Outlined.Settings,
                title = Strings.pyDjangoSettings,
                brandColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            PremiumTextField(
                value = settingsModule,
                onValueChange = onSettingsModuleChange,
                label = { Text(Strings.pyDjangoSettingsModule) },
                placeholder = { Text(Strings.pyDjangoSettingsModuleExample) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            PremiumTextField(
                value = staticDir,
                onValueChange = onStaticDirChange,
                label = { Text(Strings.pyDjangoStaticDir) },
                placeholder = { Text(Strings.pyDjangoStaticDirExample) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Strings.pyDjangoAllowedHosts,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * FastAPI 专属配置面板
 */
@Composable
private fun PythonFastapiCard(
    docsEnabled: Boolean,
    onDocsEnabledChange: (Boolean) -> Unit
) {
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            RuntimeSectionHeader(
                icon = Icons.Outlined.Api,
                title = Strings.pyFastapiConfig,
                brandColor = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        Strings.pyFastapiDocsEndpoint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        Strings.pyFastapiAsgiHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 框架特定提示卡片
 */
@Composable
private fun PythonFrameworkTipCard(framework: String?) {
    data class FrameworkTip(val tip: String, val color: Color)

    val tipText = Strings.pyFrameworkTip(framework)
    val tipColor = when (framework?.lowercase()) {
        "flask", "tornado" -> MaterialTheme.colorScheme.onSurface
        else -> null
    }
    val tipData = if (tipText != null && tipColor != null) FrameworkTip(tipText, tipColor) else null

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
