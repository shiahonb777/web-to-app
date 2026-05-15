package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.design.WtaSwitch
import com.webtoapp.ui.components.PremiumOutlinedButton
import android.provider.DocumentsContract
import com.webtoapp.core.logging.AppLogger
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.linux.HtmlProjectOptimizer
import com.webtoapp.core.linux.NativeNodeEngine
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.ui.components.*
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.screens.create.WtaCreateFlowScaffold
import com.webtoapp.ui.screens.create.WtaCreateFlowSection
import com.webtoapp.util.HtmlProjectProcessor
import com.webtoapp.util.ZipProjectImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.graphics.Color
import com.webtoapp.ui.components.EnhancedElevatedCard





private data class HtmlEditorStateSnapshot(
    val appName: String = "",
    val manualFiles: List<HtmlFile> = emptyList(),
    val appIcon: Uri? = null,
    val enableJavaScript: Boolean = true,
    val enableLocalStorage: Boolean = true,
    val landscapeMode: Boolean = false
)





@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateHtmlAppScreen(
    existingAppId: Long? = null,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        htmlConfig: HtmlConfig?,
        iconUri: Uri?,
        themeType: String
    ) -> Unit,
    onZipCreated: (
        name: String,
        extractedDir: String,
        entryFile: String,
        iconUri: Uri?,
        enableJavaScript: Boolean,
        enableLocalStorage: Boolean,
        landscapeMode: Boolean
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    importDir: String? = null,
    importProjectName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = existingAppId != null


    var selectedTabIndex by remember { mutableIntStateOf(0) }


    var existingApp by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = org.koin.java.KoinJavaComponent.get<com.webtoapp.data.repository.WebAppRepository>(com.webtoapp.data.repository.WebAppRepository::class.java)
                .getWebAppById(existingAppId)
                .first()
        }
    }
    var appName by remember { mutableStateOf(importProjectName ?: "") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }


    var zipAnalysis by remember { mutableStateOf<ZipProjectImporter.ZipProjectAnalysis?>(null) }
    var zipImporting by remember { mutableStateOf(false) }
    var zipError by remember { mutableStateOf<String?>(null) }
    var zipEntryFile by remember { mutableStateOf("") }
    var showEntryFileDialog by remember { mutableStateOf(false) }
    var showFileListDialog by remember { mutableStateOf(false) }


    var folderAnalysis by remember { mutableStateOf<ZipProjectImporter.ZipProjectAnalysis?>(null) }
    var folderImporting by remember { mutableStateOf(false) }
    var folderError by remember { mutableStateOf<String?>(null) }
    var folderEntryFile by remember { mutableStateOf("") }
    var showFolderEntryFileDialog by remember { mutableStateOf(false) }
    var showFolderFileListDialog by remember { mutableStateOf(false) }


    var manualFiles by remember { mutableStateOf<List<HtmlFile>>(emptyList()) }


    var enableJavaScript by remember { mutableStateOf(true) }
    var enableLocalStorage by remember { mutableStateOf(true) }
    var landscapeMode by remember { mutableStateOf(false) }


    var themeType by remember { mutableStateOf("AURORA") }


    var enableOptimize by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizeResult by remember { mutableStateOf<HtmlProjectOptimizer.OptimizeResult?>(null) }
    val esbuildAvailable = remember { NativeNodeEngine.isAvailable(context) }


    LaunchedEffect(existingApp) {
        existingApp?.let { app ->

            appName = app.name
            appIconPath = app.iconPath


            app.htmlConfig?.let { config ->

                val restoredFiles = config.files.filter { java.io.File(it.path).exists() }.toMutableList()


                if (config.projectId.isNotBlank()) {
                    val projectDir = java.io.File(context.filesDir, "html_projects/${config.projectId}")
                    if (projectDir.exists()) {
                        val restoredNames = restoredFiles.map { it.name }.toSet()
                        projectDir.listFiles()?.forEach { file ->
                            if (file.isFile && file.name !in restoredNames) {
                                val type = getFileType(file.name)
                                restoredFiles.add(HtmlFile(
                                    name = file.name,
                                    path = file.absolutePath,
                                    type = type
                                ))
                            }
                        }
                    }
                }
                manualFiles = restoredFiles


                enableJavaScript = config.enableJavaScript
                enableLocalStorage = config.enableLocalStorage
                landscapeMode = config.landscapeMode
            }


            themeType = app.themeType
        }
    }


    LaunchedEffect(importDir) {
        if (importDir != null) {
            val dir = java.io.File(importDir)
            if (dir.exists() && dir.isDirectory) {
                val importedFiles = dir.listFiles()?.mapNotNull { file ->
                    if (file.isFile) {
                        HtmlFile(
                            name = file.name,
                            path = file.absolutePath,
                            type = getFileType(file.name)
                        )
                    } else null
                } ?: emptyList()
                manualFiles = importedFiles

                if (appName.isBlank()) {
                    appName = importProjectName ?: importedFiles.firstOrNull {
                        it.type == HtmlFileType.HTML
                    }?.name?.substringBeforeLast(".") ?: ""
                }
            }
        }
    }


    var projectAnalysis by remember { mutableStateOf<HtmlProjectProcessor.ProjectAnalysis?>(null) }
    var showAnalysisDialog by remember { mutableStateOf(false) }


    LaunchedEffect(manualFiles) {
        val htmlFile = manualFiles.firstOrNull { it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) || it.name.endsWith(".htm", ignoreCase = true) }
        if (htmlFile != null) {
            projectAnalysis = withContext(Dispatchers.IO) {
                HtmlProjectProcessor.analyzeProject(
                    htmlFilePath = htmlFile.path,
                    cssFilePath = manualFiles.firstOrNull { it.type == HtmlFileType.CSS }?.path,
                    jsFilePath = manualFiles.firstOrNull { it.type == HtmlFileType.JS }?.path
                )
            }
        } else {
            projectAnalysis = null
        }
    }


    val canCreate = when (selectedTabIndex) {
        0 -> manualFiles.any { it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) || it.name.endsWith(".htm", ignoreCase = true) }
        1 -> zipAnalysis != null && zipAnalysis!!.htmlFiles.isNotEmpty()
        2 -> folderAnalysis != null && folderAnalysis!!.htmlFiles.isNotEmpty()
        else -> false
    }


    val hasIssues = projectAnalysis?.issues?.any {
        it.severity == HtmlProjectProcessor.IssueSeverity.ERROR ||
        it.severity == HtmlProjectProcessor.IssueSeverity.WARNING
    } == true


    val zipPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { zipUri ->
            zipImporting = true
            zipError = null
            zipAnalysis = null
            scope.launch {
                try {
                    val analysis = withContext(Dispatchers.IO) {
                        ZipProjectImporter.importZip(context, zipUri)
                    }
                    zipAnalysis = analysis
                    zipEntryFile = analysis.entryFile
                    if (appName.isBlank()) {
                        appName = analysis.suggestedAppName
                    }
                    if (analysis.htmlFiles.isEmpty()) {
                        zipError = Strings.zipNoHtmlWarning
                    }
                } catch (e: ZipProjectImporter.ZipImportException) {
                    zipError = e.message
                } catch (e: Exception) {
                    zipError = e.message ?: "Unknown error"
                } finally {
                    zipImporting = false
                }
            }
        }
    }


    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { treeUri ->
            folderImporting = true
            folderError = null
            folderAnalysis = null
            scope.launch {
                try {
                    val analysis = withContext(Dispatchers.IO) {
                        importFolderFromSaf(context, treeUri)
                    }
                    folderAnalysis = analysis
                    folderEntryFile = analysis.entryFile
                    if (appName.isBlank()) {
                        appName = analysis.suggestedAppName
                    }
                    if (analysis.htmlFiles.isEmpty()) {
                        folderError = Strings.folderNoHtmlWarning
                    }
                } catch (e: Exception) {
                    folderError = e.message ?: "Unknown error"
                } finally {
                    folderImporting = false
                }
            }
        }
    }


    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = getFileName(context, it)
            val tempFile = copyUriToTempFile(context, it, fileName)
            if (tempFile != null && fileName != null) {
                val fileType = getFileType(fileName)
                val newFile = HtmlFile(
                    name = fileName,
                    path = tempFile.absolutePath,
                    type = fileType
                )
                manualFiles = manualFiles + newFile

                if (appName.isBlank() && (fileType == HtmlFileType.HTML || fileType == HtmlFileType.OTHER && fileName.endsWith(".html", ignoreCase = true))) {
                    appName = fileName.substringBeforeLast(".")
                }
            }
        }
    }


    val entryFile = manualFiles.firstOrNull {
        it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) || it.name.endsWith(".htm", ignoreCase = true)
    }?.name?.takeIf { it.isNotBlank() && it.substringBeforeLast(".").isNotBlank() } ?: "index.html"


    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }



    var baselineSnapshot by remember { mutableStateOf(HtmlEditorStateSnapshot(appName = importProjectName ?: "")) }


    LaunchedEffect(existingApp) {
        existingApp?.let {

            kotlinx.coroutines.delay(100)
            baselineSnapshot = HtmlEditorStateSnapshot(
                appName = appName,
                manualFiles = manualFiles,
                appIcon = appIcon,
                enableJavaScript = enableJavaScript,
                enableLocalStorage = enableLocalStorage,
                landscapeMode = landscapeMode
            )
        }
    }


    LaunchedEffect(importDir) {
        if (importDir != null) {
            kotlinx.coroutines.delay(100)
            baselineSnapshot = HtmlEditorStateSnapshot(
                appName = appName,
                manualFiles = manualFiles,
                appIcon = appIcon,
                enableJavaScript = enableJavaScript,
                enableLocalStorage = enableLocalStorage,
                landscapeMode = landscapeMode
            )
        }
    }

    val hasUnsavedChanges = remember(appName, manualFiles, appIcon, enableJavaScript, enableLocalStorage, landscapeMode, baselineSnapshot) {
        appName != baselineSnapshot.appName ||
        manualFiles != baselineSnapshot.manualFiles ||
        appIcon != baselineSnapshot.appIcon ||
        enableJavaScript != baselineSnapshot.enableJavaScript ||
        enableLocalStorage != baselineSnapshot.enableLocalStorage ||
        landscapeMode != baselineSnapshot.landscapeMode
    }
    var showExitConfirmDialog by remember { mutableStateOf(false) }


    var showCodeEditorDialog by remember { mutableStateOf(false) }
    var codeEditorType by remember { mutableStateOf(HtmlFileType.HTML) }
    var codeEditorContent by remember { mutableStateOf("") }


    BackHandler(enabled = hasUnsavedChanges) {
        showExitConfirmDialog = true
    }


    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(Strings.unsavedChangesTitle) },
            text = { Text(Strings.unsavedChangesMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(Strings.discardChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(Strings.keepEditing)
                }
            }
        )
    }


    if (showCodeEditorDialog) {
        CodeEditorDialog(
            fileType = codeEditorType,
            initialContent = codeEditorContent,
            onSave = { content ->
                val fileName = when (codeEditorType) {
                    HtmlFileType.HTML -> "index.html"
                    HtmlFileType.CSS -> "style.css"
                    HtmlFileType.JS -> "script.js"
                    else -> "file.txt"
                }

                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        val tempDir = File(context.cacheDir, "html_temp").apply { mkdirs() }
                        val targetFile = File(tempDir, fileName)
                        targetFile.writeText(content)
                        targetFile
                    }
                    val htmlFileObj = HtmlFile(
                        name = fileName,
                        path = file.absolutePath,
                        type = codeEditorType
                    )

                    val existingIndex = manualFiles.indexOfFirst { it.name == fileName }
                    if (existingIndex >= 0) {
                        manualFiles = manualFiles.toMutableList().also { it[existingIndex] = htmlFileObj }
                    } else {
                        manualFiles = manualFiles + htmlFileObj
                    }
                    if (appName.isBlank() && codeEditorType == HtmlFileType.HTML) appName = "index"
                }
                showCodeEditorDialog = false
            },
            onDismiss = { showCodeEditorDialog = false }
        )
    }

    WtaCreateFlowScaffold(
        title = Strings.createHtmlAppTitle,
        onBack = {
            if (hasUnsavedChanges) showExitConfirmDialog = true else onBack()
        },
        actions = {
            TextButton(
                onClick = {
                    val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon

                    when (selectedTabIndex) {
                        0 -> {

                            if (enableOptimize && !isOptimizing) {
                                isOptimizing = true
                                scope.launch {
                                    val result = HtmlProjectOptimizer.optimizeFiles(
                                        context = context,
                                        jsFilePath = manualFiles.firstOrNull { it.type == HtmlFileType.JS }?.path,
                                        cssFilePath = manualFiles.firstOrNull { it.type == HtmlFileType.CSS }?.path
                                    )
                                    optimizeResult = result
                                    isOptimizing = false

                                    val config = HtmlConfig(
                                        entryFile = entryFile,
                                        files = manualFiles,
                                        enableJavaScript = enableJavaScript,
                                        enableLocalStorage = enableLocalStorage,
                                        landscapeMode = landscapeMode
                                    )
                                    onCreated(
                                        appName.ifBlank { Strings.createHtmlApp },
                                        config,
                                        finalIconUri,
                                        themeType
                                    )
                                }
                            } else {
                                val config = HtmlConfig(
                                    entryFile = entryFile,
                                    files = manualFiles,
                                    enableJavaScript = enableJavaScript,
                                    enableLocalStorage = enableLocalStorage,
                                    landscapeMode = landscapeMode
                                )
                                onCreated(
                                    appName.ifBlank { Strings.createHtmlApp },
                                    config,
                                    finalIconUri,
                                    themeType
                                )
                            }
                        }
                        1 -> {

                            zipAnalysis?.let { analysis ->
                                if (enableOptimize && !isOptimizing) {
                                    isOptimizing = true
                                    scope.launch {
                                        val result = HtmlProjectOptimizer.optimizeDirectory(
                                            context = context,
                                            projectDir = analysis.extractDir
                                        )
                                        optimizeResult = result
                                        isOptimizing = false

                                        onZipCreated(
                                            appName.ifBlank { analysis.suggestedAppName },
                                            analysis.extractDir,
                                            zipEntryFile,
                                            finalIconUri,
                                            enableJavaScript,
                                            enableLocalStorage,
                                            landscapeMode
                                        )
                                    }
                                } else {
                                    onZipCreated(
                                        appName.ifBlank { analysis.suggestedAppName },
                                        analysis.extractDir,
                                        zipEntryFile,
                                        finalIconUri,
                                        enableJavaScript,
                                        enableLocalStorage,
                                        landscapeMode
                                    )
                                }
                            }
                        }
                        2 -> {

                            folderAnalysis?.let { analysis ->
                                if (enableOptimize && !isOptimizing) {
                                    isOptimizing = true
                                    scope.launch {
                                        val result = HtmlProjectOptimizer.optimizeDirectory(
                                            context = context,
                                            projectDir = analysis.extractDir
                                        )
                                        optimizeResult = result
                                        isOptimizing = false

                                        onZipCreated(
                                            appName.ifBlank { analysis.suggestedAppName },
                                            analysis.extractDir,
                                            folderEntryFile,
                                            finalIconUri,
                                            enableJavaScript,
                                            enableLocalStorage,
                                            landscapeMode
                                        )
                                    }
                                } else {
                                    onZipCreated(
                                        appName.ifBlank { analysis.suggestedAppName },
                                        analysis.extractDir,
                                        folderEntryFile,
                                        finalIconUri,
                                        enableJavaScript,
                                        enableLocalStorage,
                                        landscapeMode
                                    )
                                }
                            }
                        }
                    }
                },
                enabled = canCreate && !isOptimizing
            ) {
                if (isOptimizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.optimizing)
                } else {
                    Text(Strings.btnCreate)
                }
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            WtaCreateFlowSection(title = Strings.importProject) {
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium),
                        edgePadding = 0.dp
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.TouchApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Strings.manualSelectMode)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.FolderZip,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Strings.zipImportMode)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Strings.folderImportMode)
                                }
                            }
                        )
                    }
                }
            }


            if (selectedTabIndex == 0) {

                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Strings.selectFiles,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.selectFilesHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))


                        manualFiles.forEachIndexed { index, file ->
                            val (icon, label) = when (file.type) {
                                HtmlFileType.HTML -> Icons.Outlined.Code to Strings.htmlFile
                                HtmlFileType.CSS -> Icons.Outlined.Palette to Strings.cssFile
                                HtmlFileType.JS -> Icons.Outlined.Javascript to Strings.jsFile
                                HtmlFileType.IMAGE -> Icons.Outlined.Image to "Image"
                                HtmlFileType.FONT -> Icons.Outlined.FontDownload to "Font"
                                HtmlFileType.OTHER -> Icons.AutoMirrored.Outlined.InsertDriveFile to file.name.substringAfterLast(".", "File")
                            }
                            HtmlFileSlot(
                                label = label,
                                icon = icon,
                                file = file,
                                required = file.type == HtmlFileType.HTML,
                                onSelect = { filePickerLauncher.launch("*/*") },
                                onClear = { manualFiles = manualFiles.toMutableList().also { it.removeAt(index) } },
                                onEdit = {
                                    codeEditorType = file.type.takeIf { it == HtmlFileType.HTML || it == HtmlFileType.CSS || it == HtmlFileType.JS } ?: HtmlFileType.OTHER
                                    codeEditorContent = try { File(file.path).readText() } catch (e: Exception) { "" }
                                    showCodeEditorDialog = true
                                }
                            )
                            if (index < manualFiles.lastIndex) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }


                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.htmlAddFiles)
                        }
                    }
                }


                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = Strings.writeCode,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Strings.writeCodeHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            PremiumOutlinedButton(
                                onClick = {
                                    codeEditorType = HtmlFileType.HTML
                                    val existingHtml = manualFiles.firstOrNull { it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) }
                                    codeEditorContent = if (existingHtml != null) {
                                        try { File(existingHtml.path).readText() } catch (e: Exception) { "" }
                                    } else "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>My App</title>\n</head>\n<body>\n    <h1>Hello World</h1>\n</body>\n</html>"
                                    showCodeEditorDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Code, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("HTML", maxLines = 1)
                            }

                            PremiumOutlinedButton(
                                onClick = {
                                    codeEditorType = HtmlFileType.CSS
                                    val existingCss = manualFiles.firstOrNull { it.type == HtmlFileType.CSS }
                                    codeEditorContent = if (existingCss != null) {
                                        try { File(existingCss.path).readText() } catch (e: Exception) { "" }
                                    } else "/* Styles */\nbody {\n    margin: 0;\n    padding: 0;\n    font-family: sans-serif;\n}\n"
                                    showCodeEditorDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Palette, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CSS", maxLines = 1)
                            }

                            PremiumOutlinedButton(
                                onClick = {
                                    codeEditorType = HtmlFileType.JS
                                    val existingJs = manualFiles.firstOrNull { it.type == HtmlFileType.JS }
                                    codeEditorContent = if (existingJs != null) {
                                        try { File(existingJs.path).readText() } catch (e: Exception) { "" }
                                    } else "// JavaScript\ndocument.addEventListener('DOMContentLoaded', () => {\n    console.log('App loaded');\n});\n"
                                    showCodeEditorDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Javascript, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("JS", maxLines = 1)
                            }
                        }
                    }
                }
            }


            if (selectedTabIndex == 1) {
                ZipImportSection(
                    zipAnalysis = zipAnalysis,
                    zipImporting = zipImporting,
                    zipError = zipError,
                    zipEntryFile = zipEntryFile,
                    onSelectZip = { zipPickerLauncher.launch("application/zip") },
                    onChangeEntry = { showEntryFileDialog = true },
                    onShowFileList = { showFileListDialog = true },
                    onReimport = {
                        zipAnalysis = null
                        zipError = null
                        zipPickerLauncher.launch("application/zip")
                    }
                )
            }


            if (selectedTabIndex == 2) {
                FolderImportSection(
                    folderAnalysis = folderAnalysis,
                    folderImporting = folderImporting,
                    folderError = folderError,
                    folderEntryFile = folderEntryFile,
                    onSelectFolder = { folderPickerLauncher.launch(null) },
                    onChangeEntry = { showFolderEntryFileDialog = true },
                    onShowFileList = { showFolderFileListDialog = true },
                    onReimport = {
                        folderAnalysis = null
                        folderError = null
                        folderPickerLauncher.launch(null)
                    }
                )
            }
            }


            WtaCreateFlowSection(title = Strings.appConfig) {
            if (!isEditMode) {
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.labelAppInfo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))


                    AppNameTextFieldSimple(
                        value = appName,
                        onValueChange = { appName = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))


                    IconPickerWithLibrary(
                        iconUri = appIcon,
                        iconPath = appIconPath,
                        onSelectFromGallery = { iconPickerLauncher.launch("image/*") },
                        onSelectFromLibrary = { path ->
                            appIconPath = path
                            appIcon = null
                        }
                    )
                }
            }
            }


            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Strings.labelAdvancedConfig,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(Strings.enableJavaScript)
                            Text(
                                text = Strings.enableJsHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        WtaSwitch(
                            checked = enableJavaScript,
                            onCheckedChange = { enableJavaScript = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(Strings.enableLocalStorage)
                            Text(
                                text = Strings.enableLocalStorageHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        WtaSwitch(
                            checked = enableLocalStorage,
                            onCheckedChange = { enableLocalStorage = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))


                    if (!isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(Strings.landscapeModeLabel)
                            Text(
                                text = Strings.landscapeModeHintHtml,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        WtaSwitch(
                            checked = landscapeMode,
                            onCheckedChange = { landscapeMode = it }
                        )
                    }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Strings.optimizeCode)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (esbuildAvailable) "esbuild" else "built-in",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (esbuildAvailable)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .background(
                                            if (esbuildAvailable)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.shapes.extraSmall
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = Strings.optimizeCodeHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        WtaSwitch(
                            checked = enableOptimize,
                            onCheckedChange = { enableOptimize = it }
                        )
                    }


                    if (optimizeResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val result = optimizeResult!!
                        EnhancedElevatedCard(
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.success)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (result.success) {
                                    Text(
                                        text = Strings.optimizeComplete,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (result.jsFilesOptimized > 0) {
                                        Text(
                                            text = Strings.optimizeResultJs.replace("%d", result.jsFilesOptimized.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.cssFilesOptimized > 0) {
                                        Text(
                                            text = Strings.optimizeResultCss.replace("%d", result.cssFilesOptimized.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.tsFilesCompiled > 0) {
                                        Text(
                                            text = Strings.optimizeResultTs.replace("%d", result.tsFilesCompiled.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (result.savedBytes > 0) {
                                        Text(
                                            text = Strings.optimizeResultSaved.replace("%s", formatFileSize(result.savedBytes)),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = Strings.optimizeFailed.replace("%s", result.error ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }


            WtaCreateFlowSection(title = Strings.preview) {
                if (hasIssues && projectAnalysis != null) {
                    val errorCount = projectAnalysis!!.issues.count {
                        it.severity == HtmlProjectProcessor.IssueSeverity.ERROR
                    }
                    val warningCount = projectAnalysis!!.issues.count {
                        it.severity == HtmlProjectProcessor.IssueSeverity.WARNING
                    }
                    WtaStatusBanner(
                        title = Strings.projectIssuesDetected,
                        message = buildString {
                            if (errorCount > 0) append(Strings.errorsCount.replace("%d", errorCount.toString()))
                            if (errorCount > 0 && warningCount > 0) append(", ")
                            if (warningCount > 0) append(Strings.warningsCount.replace("%d", warningCount.toString()))
                            if (isNotEmpty()) append("\n")
                            append(Strings.autoFixHint)
                        },
                        tone = WtaStatusTone.Error,
                        actionLabel = Strings.viewAnalysisResult,
                        onAction = { showAnalysisDialog = true },
                        messageMaxLines = 4
                    )
                }


                WtaStatusBanner(
                    message = when (selectedTabIndex) {
                            0 -> Strings.htmlAppTip
                            1 -> Strings.zipTip
                            2 -> Strings.folderTip
                            else -> Strings.htmlAppTip
                    },
                    tone = WtaStatusTone.Info,
                    messageMaxLines = 4
                )


                WtaStatusBanner(
                    message = Strings.featureTip,
                    tone = WtaStatusTone.Warning,
                    messageMaxLines = 4
                )


            if (selectedTabIndex == 0) {
                EnhancedElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = Strings.aboutFileReference,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Strings.fileReferenceHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            }
        }
    }


    if (showAnalysisDialog && projectAnalysis != null) {
        ProjectAnalysisDialog(
            analysis = projectAnalysis!!,
            onDismiss = { showAnalysisDialog = false }
        )
    }


    if (showEntryFileDialog && zipAnalysis != null) {
        ZipEntryFileDialog(
            htmlFiles = zipAnalysis!!.htmlFiles.map { it.relativePath },
            currentEntry = zipEntryFile,
            onSelect = { selected ->
                zipEntryFile = selected
                showEntryFileDialog = false
            },
            onDismiss = { showEntryFileDialog = false }
        )
    }


    if (showFileListDialog && zipAnalysis != null) {
        ZipFileListDialog(
            analysis = zipAnalysis!!,
            onDismiss = { showFileListDialog = false }
        )
    }


    if (showFolderEntryFileDialog && folderAnalysis != null) {
        ZipEntryFileDialog(
            htmlFiles = folderAnalysis!!.htmlFiles.map { it.relativePath },
            currentEntry = folderEntryFile,
            onSelect = { selected ->
                folderEntryFile = selected
                showFolderEntryFileDialog = false
            },
            onDismiss = { showFolderEntryFileDialog = false }
        )
    }


    if (showFolderFileListDialog && folderAnalysis != null) {
        ZipFileListDialog(
            analysis = folderAnalysis!!,
            onDismiss = { showFolderFileListDialog = false }
        )
    }
}




@Composable
private fun ProjectAnalysisDialog(
    analysis: HtmlProjectProcessor.ProjectAnalysis,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.projectAnalysisResult)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Text(
                        text = Strings.fileInfo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        analysis.htmlFiles.forEach { file ->
                            FileInfoRow(file, "HTML")
                        }
                        analysis.cssFiles.forEach { file ->
                            FileInfoRow(file, "CSS")
                        }
                        analysis.jsFiles.forEach { file ->
                            FileInfoRow(file, "JS")
                        }
                    }
                }


                if (analysis.issues.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.detectedIssues,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(analysis.issues) { issue ->
                        IssueCard(issue)
                    }
                }


                if (analysis.suggestions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Strings.suggestions,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(analysis.suggestions) { suggestion ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }


                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.autoProcessHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.gotIt)
            }
        }
    )
}




@Composable
private fun FileInfoRow(file: HtmlProjectProcessor.FileInfo, type: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.shapes.small
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = type,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.shapes.extraSmall
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = Strings.encodingAndSize.format(file.encoding ?: "UTF-8", formatFileSize(file.size)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}




@Composable
private fun IssueCard(issue: HtmlProjectProcessor.ProjectIssue) {
    val (icon, containerColor, contentColor) = when (issue.severity) {
        HtmlProjectProcessor.IssueSeverity.ERROR -> Triple(
            Icons.Outlined.Error,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.onSurface
        )
        HtmlProjectProcessor.IssueSeverity.WARNING -> Triple(
            Icons.Outlined.Warning,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.onSurface
        )
        HtmlProjectProcessor.IssueSeverity.INFO -> Triple(
            Icons.Outlined.Info,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.onSurface
        )
    }

    EnhancedElevatedCard(
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
            if (issue.file != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.fileLabel.format(issue.file),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            if (issue.suggestion != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${Strings.suggestions}: ${issue.suggestion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}




private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format(java.util.Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0))
    }
}




@Composable
private fun CodeEditorDialog(
    fileType: HtmlFileType,
    initialContent: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var codeText by remember { mutableStateOf(initialContent) }
    val isModified = codeText != initialContent

    val title = when (fileType) {
        HtmlFileType.HTML -> "HTML"
        HtmlFileType.CSS -> "CSS"
        HtmlFileType.JS -> "JavaScript"
        else -> Strings.codeEditorTitle
    }

    val placeholder = when (fileType) {
        HtmlFileType.HTML -> Strings.htmlCodePlaceholder
        HtmlFileType.CSS -> Strings.cssCodePlaceholder
        HtmlFileType.JS -> Strings.jsCodePlaceholder
        else -> ""
    }


    val accentColor = MaterialTheme.colorScheme.onSurface

    Dialog(
        onDismissRequest = {
            if (!isModified) onDismiss()

        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isModified
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            color = com.webtoapp.ui.theme.AppColors.EditorDark.copy(alpha = 0.98f),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Surface(
                    color = com.webtoapp.ui.theme.AppColors.EditorDark,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isModified) {


                            }
                            onDismiss()
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = Strings.close,
                                tint = com.webtoapp.ui.theme.AppColors.CodeForeground
                            )
                        }


                        Surface(
                            color = accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = Strings.codeEditorTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = com.webtoapp.ui.theme.AppColors.CodeForeground,
                            modifier = Modifier.weight(1f)
                        )

                        if (isModified) {
                            Surface(
                                color = com.webtoapp.ui.theme.AppColors.CodeForeground.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "●",
                                    color = com.webtoapp.ui.theme.AppColors.CodeForeground,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }


                        TextButton(
                            onClick = { onSave(codeText) },
                            enabled = codeText.isNotBlank()
                        ) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = null,
                                tint = if (codeText.isNotBlank()) com.webtoapp.ui.theme.AppColors.CodeForeground else com.webtoapp.ui.theme.AppColors.CodeMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = Strings.saveFile,
                                color = if (codeText.isNotBlank()) com.webtoapp.ui.theme.AppColors.CodeForeground else com.webtoapp.ui.theme.AppColors.CodeMuted
                            )
                        }
                    }
                }


                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // 关键：把 verticalScroll 唯一地放在外层 Row 上。
                    // 行号 Column 与 BasicTextField 都不再各自套 scroll、也不再 fillMaxHeight/fillMaxSize，
                    // 子组件在垂直方向自然按内容撑开，由父 Row 统一滚动。
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                    ) {

                        val lineCount = maxOf(codeText.count { it == '\n' } + 1, 1)
                        Column(
                            modifier = Modifier
                                .width(44.dp)
                                .background(com.webtoapp.ui.theme.AppColors.EditorDark)
                                .padding(end = 8.dp, top = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = com.webtoapp.ui.theme.AppColors.CodeGutter
                                    )
                                )
                            }
                        }

                        // 1dp 分隔条改为 BasicTextField 左侧 padding 模拟，
                        // 避免在无界高度的 verticalScroll 容器中 fillMaxHeight 塌缩。

                        BasicTextField(
                            value = codeText,
                            onValueChange = { codeText = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = com.webtoapp.ui.theme.AppColors.CodeForeground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                                .padding(start = 9.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (codeText.isEmpty()) {
                                        Text(
                                            text = placeholder,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp,
                                                color = com.webtoapp.ui.theme.AppColors.CodeMuted
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }


                Surface(
                    color = com.webtoapp.ui.theme.AppColors.EditorDarkAlt
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically

                    ) {
                        val lineCount = codeText.count { it == '\n' } + 1
                        val charCount = codeText.length
                        Text(
                            text = "$lineCount ${if (lineCount == 1) "line" else "lines"}, $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.webtoapp.ui.theme.AppColors.CodeForeground.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}




private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result
}




private fun copyUriToTempFile(
    context: android.content.Context,
    uri: Uri,
    fileName: String?
): File? {
    return try {
        val tempDir = File(context.cacheDir, "html_temp").apply { mkdirs() }
        val targetFile = File(tempDir, fileName ?: "file_${System.currentTimeMillis()}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    } catch (e: Exception) {
        AppLogger.e("CreateHtmlAppScreen", "Operation failed", e)
        null
    }
}




private fun getFileType(fileName: String): HtmlFileType {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "html", "htm" -> HtmlFileType.HTML
        "css" -> HtmlFileType.CSS
        "js" -> HtmlFileType.JS
        "png", "jpg", "jpeg", "gif", "webp", "svg", "ico" -> HtmlFileType.IMAGE
        "ttf", "otf", "woff", "woff2", "eot" -> HtmlFileType.FONT
        else -> HtmlFileType.OTHER
    }
}






@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZipImportSection(
    zipAnalysis: ZipProjectImporter.ZipProjectAnalysis?,
    zipImporting: Boolean,
    zipError: String?,
    zipEntryFile: String,
    onSelectZip: () -> Unit,
    onChangeEntry: () -> Unit,
    onShowFileList: () -> Unit,
    onReimport: () -> Unit
) {
    if (zipImporting) {

        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Strings.zipImporting,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else if (zipAnalysis != null) {

        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.zipProjectAnalysis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onReimport) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.zipReimport, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { onChangeEntry() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.zipEntryFile,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = zipEntryFile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (zipAnalysis.htmlFiles.size > 1) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = Strings.zipChangeEntry,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))


                Text(
                    text = Strings.zipResourceStats,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))


                val stats = zipAnalysis.stats
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.forEach { stat ->
                        AssistChip(
                            onClick = { onShowFileList() },
                            label = {
                                Text(
                                    "${stat.type.icon} ${stat.type.label}: ${stat.count}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Strings.zipTotalFiles.replace("%d", zipAnalysis.totalFileCount.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Strings.zipTotalSize.replace("%s", zipAnalysis.formattedTotalSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onShowFileList,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.zipFileTreeTitle, style = MaterialTheme.typography.labelMedium)
                }
            }
        }


        if (zipAnalysis.warnings.isNotEmpty()) {
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    zipAnalysis.warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    } else {

        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.selectZipFile,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Strings.selectZipHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                PremiumOutlinedButton(
                    onClick = onSelectZip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.selectZipFile)
                }


                if (zipError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = zipError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}




@Composable
private fun ZipEntryFileDialog(
    htmlFiles: List<String>,
    currentEntry: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.zipSelectEntryTitle)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(htmlFiles) { file ->
                    val isSelected = file == currentEntry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .clickable { onSelect(file) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSelected) Icons.Filled.RadioButtonChecked
                            else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = file,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.gotIt)
            }
        }
    )
}




@Composable
private fun ZipFileListDialog(
    analysis: ZipProjectImporter.ZipProjectAnalysis,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    Strings.zipFileTreeTitle + " (${analysis.totalFileCount})"
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                analysis.stats.forEach { stat ->
                    item {
                        Text(
                            text = "${stat.type.icon} ${stat.type.label} (${stat.count})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    val filesOfType = analysis.allFiles.filter { it.resourceType == stat.type }
                    items(filesOfType) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = file.relativePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(weight = 1f, fill = true),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = file.size.toFileSizeString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.gotIt)
            }
        }
    )
}


private fun Long.toFileSizeString(): String = when {
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> "${this / 1024} KB"
    else -> String.format(java.util.Locale.getDefault(), "%.1f MB", this / (1024.0 * 1024.0))
}







@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderImportSection(
    folderAnalysis: ZipProjectImporter.ZipProjectAnalysis?,
    folderImporting: Boolean,
    folderError: String?,
    folderEntryFile: String,
    onSelectFolder: () -> Unit,
    onChangeEntry: () -> Unit,
    onShowFileList: () -> Unit,
    onReimport: () -> Unit
) {
    if (folderImporting) {

        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Strings.folderImporting,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else if (folderAnalysis != null) {

        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.zipProjectAnalysis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onReimport) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.zipReimport, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { onChangeEntry() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(
                            text = Strings.zipEntryFile,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = folderEntryFile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (folderAnalysis.htmlFiles.size > 1) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = Strings.zipChangeEntry,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))


                Text(
                    text = Strings.zipResourceStats,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))


                val stats = folderAnalysis.stats
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.forEach { stat ->
                        AssistChip(
                            onClick = { onShowFileList() },
                            label = {
                                Text(
                                    "${stat.type.icon} ${stat.type.label}: ${stat.count}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Strings.zipTotalFiles.replace("%d", folderAnalysis.totalFileCount.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Strings.zipTotalSize.replace("%s", folderAnalysis.formattedTotalSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onShowFileList,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.zipFileTreeTitle, style = MaterialTheme.typography.labelMedium)
                }
            }
        }


        if (folderAnalysis.warnings.isNotEmpty()) {
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    folderAnalysis.warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    } else {

        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Strings.folderSelectFolder,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Strings.folderSelectHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                PremiumOutlinedButton(
                    onClick = onSelectFolder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.folderSelectFolder)
                }


                if (folderError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    EnhancedElevatedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = folderError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}




private val FOLDER_SKIP_PATTERNS = setOf(
    "__MACOSX", ".DS_Store", "Thumbs.db", ".git", ".svn", ".hg",
    "node_modules", ".idea", ".vscode"
)







private fun importFolderFromSaf(
    context: android.content.Context,
    treeUri: Uri
): ZipProjectImporter.ZipProjectAnalysis {
    val tempDir = File(context.cacheDir, "folder_import_${System.currentTimeMillis()}").apply {
        if (exists()) deleteRecursively()
        mkdirs()
    }

    try {

        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)


        var folderName = "HTML Project"
        context.contentResolver.query(
            docUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                folderName = cursor.getString(0) ?: folderName
            }
        }

        copyDocumentTree(context, treeUri, docId, tempDir)


        val projectRoot = unwrapSingleRootDir(tempDir)


        return analyzeFolder(projectRoot, folderName)

    } catch (e: Exception) {
        tempDir.deleteRecursively()
        AppLogger.e("FolderImport", "文件夹导入失败", e)
        throw RuntimeException(Strings.folderImportFailed.replace("%s", e.message ?: "Unknown"), e)
    }
}




private fun copyDocumentTree(
    context: android.content.Context,
    treeUri: Uri,
    parentDocId: String,
    targetDir: File
) {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

    context.contentResolver.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        ),
        null, null, null
    )?.use { cursor ->
        val docIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

        while (cursor.moveToNext()) {
            val childDocId = cursor.getString(docIdIndex)
            val childName = cursor.getString(nameIndex) ?: continue
            val mimeType = cursor.getString(mimeIndex) ?: ""


            if (FOLDER_SKIP_PATTERNS.any { childName.equals(it, ignoreCase = true) }) {
                continue
            }

            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {

                val subDir = File(targetDir, childName).apply { mkdirs() }
                copyDocumentTree(context, treeUri, childDocId, subDir)
            } else {

                val targetFile = File(targetDir, childName)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                try {
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("FolderImport", "复制文件失败: $childName", e)
                }
            }
        }
    }
}




private fun unwrapSingleRootDir(dir: File): File {
    val children = dir.listFiles() ?: return dir
    return if (children.size == 1 && children[0].isDirectory) {
        children[0]
    } else {
        dir
    }
}


private val HTML_EXT = setOf("html", "htm", "xhtml")
private val CSS_EXT = setOf("css")
private val JS_EXT = setOf("js", "mjs", "jsx", "ts", "tsx")
private val IMG_EXT = setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "bmp", "avif")
private val FONT_EXT = setOf("ttf", "otf", "woff", "woff2", "eot")
private val AUDIO_EXT = setOf("mp3", "wav", "ogg", "aac", "flac", "m4a")
private val VIDEO_EXT = setOf("mp4", "webm", "mkv", "avi", "mov")
private val DATA_EXT = setOf("json", "xml", "csv", "txt", "md", "yaml", "yml")




private fun analyzeFolder(
    projectDir: File,
    folderName: String
): ZipProjectImporter.ZipProjectAnalysis {
    val allFiles = mutableListOf<ZipProjectImporter.ProjectFile>()
    val warnings = mutableListOf<String>()

    projectDir.walkTopDown()
        .filter { it.isFile }
        .filter { file -> !FOLDER_SKIP_PATTERNS.any { file.name.equals(it, ignoreCase = true) } }
        .forEach { file ->
            val relativePath = file.relativeTo(projectDir).path
            val resourceType = classifyFileByExt(file.name)
            allFiles.add(
                ZipProjectImporter.ProjectFile(
                    relativePath = relativePath,
                    absolutePath = file.absolutePath,
                    size = file.length(),
                    resourceType = resourceType
                )
            )
        }

    val htmlFiles = allFiles.filter { it.resourceType == ZipProjectImporter.ResourceType.HTML }


    val entryFile = htmlFiles.find { it.relativePath.equals("index.html", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { it.relativePath.equals("index.htm", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { it.fileName.equals("index.html", ignoreCase = true) }?.relativePath
        ?: htmlFiles.find { !it.relativePath.contains('/') }?.relativePath
        ?: htmlFiles.firstOrNull()?.relativePath

    if (entryFile == null && htmlFiles.isEmpty()) {
        warnings.add(Strings.folderNoHtmlWarning)
    }

    if (allFiles.any { it.size > 50 * 1024 * 1024 }) {
        warnings.add(Strings.largeFileWarning)
    }

    val stats = ZipProjectImporter.ResourceType.entries
        .map { type ->
            val files = allFiles.filter { it.resourceType == type }
            ZipProjectImporter.ResourceStats(
                type = type,
                count = files.size,
                totalSize = files.sumOf { it.size }
            )
        }
        .filter { it.count > 0 }

    return ZipProjectImporter.ZipProjectAnalysis(
        extractDir = projectDir.absolutePath,
        allFiles = allFiles,
        entryFile = entryFile ?: htmlFiles.firstOrNull()?.relativePath ?: "index.html",
        htmlFiles = htmlFiles,
        stats = stats,
        totalFileCount = allFiles.size,
        totalSize = allFiles.sumOf { it.size },
        warnings = warnings,
        zipFileName = folderName
    )
}


private fun classifyFileByExt(fileName: String): ZipProjectImporter.ResourceType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        in HTML_EXT -> ZipProjectImporter.ResourceType.HTML
        in CSS_EXT -> ZipProjectImporter.ResourceType.CSS
        in JS_EXT -> ZipProjectImporter.ResourceType.JS
        in IMG_EXT -> ZipProjectImporter.ResourceType.IMAGE
        in FONT_EXT -> ZipProjectImporter.ResourceType.FONT
        in AUDIO_EXT -> ZipProjectImporter.ResourceType.AUDIO
        in VIDEO_EXT -> ZipProjectImporter.ResourceType.VIDEO
        in DATA_EXT -> ZipProjectImporter.ResourceType.DATA
        else -> ZipProjectImporter.ResourceType.OTHER
    }
}
