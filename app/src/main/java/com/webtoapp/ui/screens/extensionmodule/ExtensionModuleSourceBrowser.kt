package com.webtoapp.ui.screens.extensionmodule

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.extension.ExtensionModule
import com.webtoapp.core.extension.ModuleSourceType
import com.webtoapp.core.i18n.AppStringsProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserScriptsTabContent(
    filteredUserScripts: List<ExtensionModule>,
    extensionManager: ExtensionManager,
    searchQuery: String,
    onImportUserScript: () -> Unit,
    onClearSearch: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredUserScripts, key = { it.id }) { module ->
            UserScriptCard(
                module = module,
                onDelete = {
                    scope.launch {
                        extensionManager.deleteModule(module.id)
                        Toast.makeText(context, AppStringsProvider.current().deleted, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (filteredUserScripts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFFF7DF1E).copy(alpha = 0.10f),
                                            Color(0xFFF7DF1E).copy(alpha = 0.02f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                if (searchQuery.isNotBlank()) AppStringsProvider.current().noMatchingScripts else AppStringsProvider.current().noUserScripts,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (searchQuery.isNotBlank()) AppStringsProvider.current().tryDifferentSearch else AppStringsProvider.current().noUserScriptsHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (searchQuery.isBlank()) {
                            com.webtoapp.ui.components.PremiumButton(
                                onClick = onImportUserScript,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(AppStringsProvider.current().importUserScript, style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            TextButton(onClick = onClearSearch) {
                                Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStringsProvider.current().clearSearch, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserScriptCard(
    module: ExtensionModule,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }

    val isChromeExt = module.sourceType == ModuleSourceType.CHROME_EXTENSION
    val typeIcon = if (isChromeExt) "🧩" else "🐵"
    val typeLabel = if (isChromeExt) "Chrome" else "UserScript"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    com.webtoapp.ui.components.ModuleIcon(
                        iconId = module.icon.ifBlank { typeIcon },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            module.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(weight = 1f, fill = false)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isChromeExt) Color(0xFF4285F4).copy(alpha = 0.08f)
                                    else Color(0xFFF7DF1E).copy(alpha = 0.10f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isChromeExt) Color(0xFF4285F4) else Color(0xFFD4A017),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "v${module.version.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        module.author?.let { author ->
                            Text(
                                "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                author.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = AppStringsProvider.current().more)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(AppStringsProvider.current().viewSourceCode) },
                            onClick = { showMenu = false; showSourceDialog = true },
                            leadingIcon = { Icon(Icons.Outlined.Code, null) }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                        DropdownMenuItem(
                            text = { Text(AppStringsProvider.current().btnDelete, color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            if (module.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            val hasUrlMatches = module.urlMatches.isNotEmpty()
            val hasGmGrants = module.gmGrants.isNotEmpty()

            if (hasUrlMatches || hasGmGrants) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (hasUrlMatches) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Language,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                AppStringsProvider.current().onlyEffectiveOnMatchingSites.format(module.urlMatches.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (hasGmGrants) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Api,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "${module.gmGrants.size} APIs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSourceDialog) {
        ExtensionSourceBrowserDialog(
            module = module,
            onDismiss = { showSourceDialog = false }
        )
    }
}

private data class FileNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val children: MutableList<FileNode> = mutableListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionSourceBrowserDialog(
    module: ExtensionModule,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isChromeExt = module.sourceType == ModuleSourceType.CHROME_EXTENSION && module.chromeExtId.isNotEmpty()

    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedFileContent by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }

    val fileTree = remember(module.id) {
        if (isChromeExt) {
            buildExtensionFileTree(context, module)
        } else {
            null
        }
    }

    val expandedDirs = remember { mutableStateMapOf<String, Boolean>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                if (selectedFilePath != null) selectedFileName else module.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (selectedFilePath != null) {
                                Text(
                                    selectedFilePath ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (selectedFilePath != null) {
                                selectedFilePath = null
                            } else {
                                onDismiss()
                            }
                        }) {
                            Icon(
                                if (selectedFilePath != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                                contentDescription = null
                            )
                        }
                    }
                )

                if (selectedFilePath != null) {
                    FileContentView(
                        content = selectedFileContent,
                        fileName = selectedFileName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isChromeExt && fileTree != null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        fileTree.children.sortedWith(compareBy({ !it.isDirectory }, { it.name })).forEach { node ->
                            fileTreeItems(
                                node = node,
                                depth = 0,
                                expandedDirs = expandedDirs,
                                onFileClick = { path, name ->
                                    val content = readExtensionFile(context, module, path)
                                    selectedFileContent = content ?: AppStringsProvider.current().cannotReadFile
                                    selectedFileName = name
                                    selectedFilePath = path
                                }
                            )
                        }
                    }
                } else {
                    FileContentView(
                        content = module.code,
                        fileName = if (module.sourceType == ModuleSourceType.CHROME_EXTENSION) "content.js" else "${module.name}.user.js",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun LazyListScope.fileTreeItems(
    node: FileNode,
    depth: Int,
    expandedDirs: MutableMap<String, Boolean>,
    onFileClick: (path: String, name: String) -> Unit
) {
    val isExpanded = expandedDirs[node.relativePath] ?: (depth == 0)

    item(key = node.relativePath) {
        FileTreeRow(
            node = node,
            depth = depth,
            isExpanded = isExpanded,
            onClick = {
                if (node.isDirectory) {
                    expandedDirs[node.relativePath] = !isExpanded
                } else {
                    onFileClick(node.relativePath, node.name)
                }
            }
        )
    }

    if (node.isDirectory && isExpanded) {
        node.children.sortedWith(compareBy({ !it.isDirectory }, { it.name })).forEach { child ->
            fileTreeItems(child, depth + 1, expandedDirs, onFileClick)
        }
    }
}

@Composable
private fun FileTreeRow(
    node: FileNode,
    depth: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = (16 + depth * 20).dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            Icon(
                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                getFileIcon(node.name),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = getFileIconColor(node.name)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            node.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = true)
        )

        if (!node.isDirectory && node.size > 0) {
            Text(
                formatFileSize(node.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (node.isDirectory) {
            Icon(
                if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FileContentView(
    content: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isBinary = content.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }
    val isImage = fileName.lowercase().let {
        it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") ||
            it.endsWith(".gif") || it.endsWith(".svg") || it.endsWith(".webp") || it.endsWith(".ico")
    }

    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    getFileIcon(fileName),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = getFileIconColor(fileName)
                )
                Text(
                    fileName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                Text(
                    "${content.length} chars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (isImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🖼️ ${AppStringsProvider.current().imageFile}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isBinary) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    AppStringsProvider.current().binaryFile,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val lines = content.lines()
            val lineNumWidth = lines.size.toString().length

            Text(
                buildAnnotatedString(lines, lineNumWidth),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            )
        }
    }
}

private fun buildAnnotatedString(lines: List<String>, lineNumWidth: Int): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val maxLines = 10000
        lines.take(maxLines).forEachIndexed { index, line ->
            val lineNum = (index + 1).toString().padStart(lineNumWidth)
            pushStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            )
            append("$lineNum  ")
            pop()
            append(line)
            if (index < lines.size - 1) append("\n")
        }
        if (lines.size > maxLines) {
            append("\n\n... (${lines.size} lines total)")
        }
    }
}

@Composable
private fun getFileIcon(fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        fileName.endsWith(".js") || fileName.endsWith(".mjs") -> Icons.Outlined.Code
        fileName.endsWith(".ts") || fileName.endsWith(".tsx") -> Icons.Outlined.Code
        fileName.endsWith(".css") || fileName.endsWith(".scss") -> Icons.Outlined.Palette
        fileName.endsWith(".html") || fileName.endsWith(".htm") -> Icons.Outlined.Language
        fileName.endsWith(".json") -> Icons.Outlined.DataObject
        fileName.endsWith(".md") || fileName.endsWith(".txt") -> Icons.Outlined.Description
        fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".svg") || fileName.endsWith(".gif") || fileName.endsWith(".webp") || fileName.endsWith(".ico") -> Icons.Outlined.Image
        fileName.endsWith(".woff") || fileName.endsWith(".woff2") || fileName.endsWith(".ttf") -> Icons.Outlined.FontDownload
        fileName == "manifest.json" -> Icons.Outlined.Settings
        fileName == "LICENSE" || fileName.startsWith("LICENSE") -> Icons.Outlined.Gavel
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}

@Composable
private fun getFileIconColor(fileName: String): androidx.compose.ui.graphics.Color {
    return when {
        fileName.endsWith(".js") || fileName.endsWith(".mjs") -> androidx.compose.ui.graphics.Color(0xFFF7DF1E)
        fileName.endsWith(".ts") || fileName.endsWith(".tsx") -> androidx.compose.ui.graphics.Color(0xFF3178C6)
        fileName.endsWith(".css") || fileName.endsWith(".scss") -> androidx.compose.ui.graphics.Color(0xFF1572B6)
        fileName.endsWith(".html") || fileName.endsWith(".htm") -> androidx.compose.ui.graphics.Color(0xFFE44D26)
        fileName.endsWith(".json") -> androidx.compose.ui.graphics.Color(0xFF5B9BD5)
        fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".svg") || fileName.endsWith(".gif") -> MaterialTheme.colorScheme.tertiary
        fileName == "manifest.json" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB"
    }
}

private fun buildExtensionFileTree(context: android.content.Context, module: ExtensionModule): FileNode? {
    val extId = module.chromeExtId
    if (extId.isEmpty()) return null

    return if (module.builtIn) {
        buildAssetFileTree(context, "extensions/$extId", extId)
    } else {
        val extDir = java.io.File(context.filesDir, "extensions/$extId")
        if (extDir.exists() && extDir.isDirectory) {
            buildFileSystemTree(extDir, "")
        } else {
            null
        }
    }
}

private fun buildAssetFileTree(context: android.content.Context, assetPath: String, name: String): FileNode {
    val root = FileNode(name = name, relativePath = "", isDirectory = true)

    fun walkAssets(currentPath: String, parent: FileNode) {
        try {
            val children = context.assets.list(currentPath) ?: return
            for (child in children) {
                val childPath = "$currentPath/$child"
                val relativePath = childPath.removePrefix("extensions/$name/")
                val subChildren = context.assets.list(childPath)

                if (subChildren != null && subChildren.isNotEmpty()) {
                    val dirNode = FileNode(name = child, relativePath = relativePath, isDirectory = true)
                    walkAssets(childPath, dirNode)
                    parent.children.add(dirNode)
                } else {
                    val size = try {
                        context.assets.open(childPath).use { it.available().toLong() }
                    } catch (_: Exception) {
                        0L
                    }
                    parent.children.add(FileNode(name = child, relativePath = relativePath, isDirectory = false, size = size))
                }
            }
        } catch (_: Exception) {
        }
    }

    walkAssets(assetPath, root)
    return root
}

private fun buildFileSystemTree(dir: java.io.File, relativePath: String): FileNode {
    val root = FileNode(name = dir.name, relativePath = relativePath, isDirectory = true)

    dir.listFiles()?.forEach { file ->
        val childRelative = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
        if (file.isDirectory) {
            root.children.add(buildFileSystemTree(file, childRelative))
        } else {
            root.children.add(FileNode(name = file.name, relativePath = childRelative, isDirectory = false, size = file.length()))
        }
    }

    return root
}

private fun readExtensionFile(context: android.content.Context, module: ExtensionModule, relativePath: String): String? {
    val extId = module.chromeExtId

    return try {
        if (module.builtIn) {
            context.assets.open("extensions/$extId/$relativePath").bufferedReader().use { it.readText() }
        } else {
            val file = java.io.File(context.filesDir, "extensions/$extId/$relativePath")
            if (file.exists()) file.readText() else null
        }
    } catch (_: Exception) {
        null
    }
}
