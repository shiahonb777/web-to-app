package com.webtoapp.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.ui.components.BgmCard
import com.webtoapp.ui.components.IconPickerWithLibrary
import java.io.File

/**
 * 创建HTML应用页面
 * 支持单个HTML文件或HTML+CSS+JS项目
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHtmlAppScreen(
    onBack: () -> Unit,
    onCreated: (
        name: String,
        htmlConfig: HtmlConfig,
        iconUri: Uri?,
        activationEnabled: Boolean,
        activationCodes: List<String>,
        bgmEnabled: Boolean,
        bgmConfig: BgmConfig
    ) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // 应用信息
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }  // 图标库选择的路径
    
    // HTML文件列表
    var htmlFiles by remember { mutableStateOf<List<HtmlFile>>(emptyList()) }
    var entryFile by remember { mutableStateOf("index.html") }
    
    // 配置选项
    var enableJavaScript by remember { mutableStateOf(true) }
    var enableLocalStorage by remember { mutableStateOf(true) }
    
    // 激活码
    var activationEnabled by remember { mutableStateOf(false) }
    var activationCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 背景音乐
    var bgmEnabled by remember { mutableStateOf(false) }
    var bgmConfig by remember { mutableStateOf(BgmConfig()) }
    
    // 文件选择器 - 选择多个文件
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uri ->
                val fileName = getFileName(context, uri)
                val tempFile = copyUriToTempFile(context, uri, fileName)
                if (tempFile != null && fileName != null) {
                    HtmlFile(
                        name = fileName,
                        path = tempFile.absolutePath,
                        type = getFileType(fileName)
                    )
                } else null
            }
            htmlFiles = files
            
            // 自动选择入口文件
            val indexHtml = files.find { it.name.equals("index.html", ignoreCase = true) }
            if (indexHtml != null) {
                entryFile = indexHtml.name
            } else {
                val firstHtml = files.find { it.type == HtmlFileType.HTML }
                if (firstHtml != null) {
                    entryFile = firstHtml.name
                }
            }
            
            // 自动设置应用名
            if (appName.isBlank() && files.isNotEmpty()) {
                appName = files.first().name.substringBeforeLast(".")
            }
        }
    }
    
    // 图标选择器
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建HTML应用") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val config = HtmlConfig(
                                entryFile = entryFile,
                                files = htmlFiles,
                                enableJavaScript = enableJavaScript,
                                enableLocalStorage = enableLocalStorage
                            )
                            // 处理图标：优先使用图标库路径，否则使用相册选择的Uri
                            val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                            onCreated(
                                appName.ifBlank { "HTML应用" },
                                config,
                                finalIconUri,
                                activationEnabled,
                                activationCodes,
                                bgmEnabled,
                                bgmConfig
                            )
                        },
                        enabled = htmlFiles.isNotEmpty()
                    ) {
                        Text("创建")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 文件选择区域
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "选择HTML文件",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "支持选择单个HTML文件或多个文件（HTML+CSS+JS）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 选择文件按钮
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf(
                                "text/html",
                                "text/css",
                                "application/javascript",
                                "text/javascript",
                                "image/*",
                                "font/*",
                                "*/*"
                            ))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.FileOpen, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (htmlFiles.isEmpty()) "选择文件" else "重新选择")
                    }
                    
                    // 已选择的文件列表
                    if (htmlFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "已选择 ${htmlFiles.size} 个文件",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        htmlFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (file.type) {
                                        HtmlFileType.HTML -> Icons.Outlined.Code
                                        HtmlFileType.CSS -> Icons.Outlined.Palette
                                        HtmlFileType.JS -> Icons.Outlined.Javascript
                                        HtmlFileType.IMAGE -> Icons.Outlined.Image
                                        else -> Icons.Outlined.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (file.name == entryFile) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("入口") },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 入口文件选择（如果有多个HTML文件）
            val htmlOnlyFiles = htmlFiles.filter { it.type == HtmlFileType.HTML }
            if (htmlOnlyFiles.size > 1) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "选择入口文件",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        htmlOnlyFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { entryFile = file.name }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = file.name == entryFile,
                                    onClick = { entryFile = file.name }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(file.name)
                            }
                        }
                    }
                }
            }
            
            // 应用信息
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "应用信息",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 应用名称
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("应用名称") },
                        placeholder = { Text("输入应用名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 应用图标（带图标库功能）
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
            
            // 高级配置
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "高级配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // JavaScript 开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用 JavaScript")
                            Text(
                                text = "允许HTML中的JavaScript代码执行",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableJavaScript,
                            onCheckedChange = { enableJavaScript = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 本地存储开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用本地存储")
                            Text(
                                text = "允许使用 localStorage 保存数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableLocalStorage,
                            onCheckedChange = { enableLocalStorage = it }
                        )
                    }
                }
            }
            
            // 激活码设置
            MediaActivationCard(
                enabled = activationEnabled,
                codes = activationCodes,
                onEnabledChange = { activationEnabled = it },
                onCodesChange = { activationCodes = it }
            )
            
            // 背景音乐
            BgmCard(
                enabled = bgmEnabled,
                config = bgmConfig,
                onEnabledChange = { bgmEnabled = it },
                onConfigChange = { bgmConfig = it }
            )
            
            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "提示：如果你的HTML项目包含多个文件（CSS、JS、图片等），请同时选择所有相关文件。文件之间的相对路径引用将被保留。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * 从Uri获取文件名
 */
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

/**
 * 复制Uri内容到临时文件
 */
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
        e.printStackTrace()
        null
    }
}

/**
 * 根据文件名获取文件类型
 */
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
