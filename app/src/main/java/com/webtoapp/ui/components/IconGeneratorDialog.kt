package com.webtoapp.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AiFeature
import com.webtoapp.data.model.ModelCapability
import com.webtoapp.data.model.SavedModel
import com.webtoapp.util.IconLibraryStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI 图标生成对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconGeneratorDialog(
    onDismiss: () -> Unit,
    onIconGenerated: (String) -> Unit // Icon文件路径（已保存到图标库）
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configManager = remember { AiConfigManager(context) }
    val aiClient = remember { AiApiClient(context) }
    
    // 模型和密钥
    val savedModels by configManager.savedModelsFlow.collectAsState(initial = emptyList())
    val apiKeys by configManager.apiKeysFlow.collectAsState(initial = emptyList())
    
    // 筛选支持图标生成功能的模型
    val imageGenModels = savedModels.filter { model ->
        model.supportsFeature(AiFeature.ICON_GENERATION)
    }
    
    // 状态
    var selectedModel by remember { mutableStateOf<SavedModel?>(null) }
    var prompt by remember { mutableStateOf("") }
    var referenceImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedIcon by remember { mutableStateOf<String?>(null) }  // Base64 数据
    var savedIconPath by remember { mutableStateOf<String?>(null) }  // Save后的文件路径
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Auto选择第一个模型
    LaunchedEffect(imageGenModels) {
        if (selectedModel == null && imageGenModels.isNotEmpty()) {
            selectedModel = imageGenModels.first()
        }
    }
    
    // Image选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            referenceImages = (referenceImages + uris).take(3)
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(Strings.aiGenerateIcon)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 模型选择
                if (imageGenModels.isEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                Strings.noImageGenModel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                Strings.addImageGenModelHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(Strings.selectModel, style = MaterialTheme.typography.labelMedium)
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedModel?.alias ?: selectedModel?.model?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            imageGenModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.alias ?: model.model.name) },
                                    onClick = {
                                        selectedModel = model
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // 提示词输入
                Text(Strings.describeIcon, style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(Strings.iconDescriptionExample) },
                    minLines = 2,
                    maxLines = 4
                )
                
                // 参考图片
                Text(
                    Strings.referenceImages,
                    style = MaterialTheme.typography.labelMedium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(referenceImages) { uri ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { referenceImages = referenceImages - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    if (referenceImages.size < 3) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    Strings.addImage,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Generate结果预览
                generatedIcon?.let { base64 ->
                    Text(Strings.generationResult, style = MaterialTheme.typography.labelMedium)
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                            .align(Alignment.CenterHorizontally)
                    ) {
                        val bitmap = remember(base64) {
                            try {
                                val bytes = Base64.decode(base64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = Strings.generatedIcon,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                // Error信息
                errorMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // Generate中提示
                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
        },
        confirmButton = {
            if (generatedIcon != null && savedIconPath != null) {
                Button(onClick = { onIconGenerated(savedIconPath!!) }) {
                    Text(Strings.useThisIcon)
                }
            } else if (generatedIcon != null) {
                // 正在保存中
                Button(enabled = false, onClick = {}) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.saving)
                }
            } else {
                Button(
                    onClick = {
                        val model = selectedModel ?: return@Button
                        val apiKey = apiKeys.find { it.id == model.apiKeyId } ?: return@Button
                        
                        isGenerating = true
                        errorMessage = null
                        
                        scope.launch {
                            val imagePaths = referenceImages.map { it.toString() }
                            val result = aiClient.generateAppIcon(
                                context, prompt, imagePaths, apiKey, model
                            )
                            
                            withContext(Dispatchers.Main) {
                                isGenerating = false
                                result.fold(
                                    onSuccess = { base64 ->
                                        generatedIcon = base64
                                        // Auto保存到图标库
                                        scope.launch {
                                            val item = IconLibraryStorage.saveFromBase64(
                                                context, base64, 
                                                prompt.take(20).ifBlank { "AI图标" }
                                            )
                                            savedIconPath = item?.path
                                        }
                                    },
                                    onFailure = { errorMessage = it.message }
                                )
                            }
                        }
                    },
                    enabled = selectedModel != null && prompt.isNotBlank() && !isGenerating
                ) {
                    Text(Strings.generateIcon)
                }
            }
        },
        dismissButton = {
            if (generatedIcon != null) {
                TextButton(onClick = { 
                    generatedIcon = null 
                    savedIconPath = null
                }) {
                    Text(Strings.regenerate)
                }
            } else {
                TextButton(onClick = onDismiss, enabled = !isGenerating) {
                    Text(Strings.btnCancel)
                }
            }
        }
    )
}
