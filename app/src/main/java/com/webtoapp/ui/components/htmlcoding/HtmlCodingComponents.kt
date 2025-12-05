package com.webtoapp.ui.components.htmlcoding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.ai.htmlcoding.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 消息气泡组件
 */
@Composable
fun MessageBubble(
    message: HtmlCodingMessage,
    onEditClick: () -> Unit = {},
    onPreviewCode: (CodeBlock) -> Unit = {},
    onCopyCode: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI头像
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Filled.Code,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // 消息内容卡片
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isUser) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 用户消息的图片
                    if (isUser && message.images.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(message.images) { imagePath ->
                                AsyncImage(
                                    model = File(imagePath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    
                    // 文本内容
                    if (message.content.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 编辑标记
                    if (message.isEdited) {
                        Text(
                            text = "(已编辑)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // 思考内容（AI消息）
            if (!isUser && message.thinking != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ThinkingBlock(thinking = message.thinking!!)
            }
            
            // 代码块（AI消息）
            if (!isUser && message.codeBlocks.isNotEmpty()) {
                message.codeBlocks.forEach { codeBlock ->
                    Spacer(modifier = Modifier.height(8.dp))
                    CodeBlockCard(
                        codeBlock = codeBlock,
                        onPreview = { onPreviewCode(codeBlock) },
                        onCopy = { onCopyCode(codeBlock.content) }
                    )
                }
            }
            
            // 操作按钮
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isUser) {
                    // 用户消息：编辑按钮
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                // 时间
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // 用户头像
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * 思考内容展示组件
 */
@Composable
fun ThinkingBlock(
    thinking: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "思考过程",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SelectionContainer {
                    Text(
                        text = thinking,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 代码块卡片组件
 */
@Composable
fun CodeBlockCard(
    codeBlock: CodeBlock,
    onPreview: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Column {
            // 头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 语言标签
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getLanguageColor(codeBlock.language)
                    ) {
                        Text(
                            text = codeBlock.language.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    // 文件名
                    codeBlock.filename?.let { filename ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = filename,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
                
                Row {
                    // 预览按钮（仅HTML）
                    if (codeBlock.language.lowercase() in listOf("html", "htm")) {
                        IconButton(
                            onClick = onPreview,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = "预览",
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // 复制按钮
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(codeBlock.content))
                            onCopy()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "复制",
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    // 展开/收起
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // 代码内容
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val scrollState = rememberScrollState()
                SelectionContainer {
                    Text(
                        text = codeBlock.content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        color = Color(0xFFD4D4D4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .horizontalScroll(scrollState)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * 输入区域组件
 */
@Composable
fun ChatInputArea(
    value: String,
    onValueChange: (String) -> Unit,
    images: List<String>,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    maxImages: Int = 3,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 图片预览
            if (images.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    itemsIndexed(images) { index, imagePath ->
                        Box {
                            AsyncImage(
                                model = File(imagePath),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onRemoveImage(index) },
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移除",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // 输入行
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 添加图片按钮
                IconButton(
                    onClick = onAddImage,
                    enabled = images.size < maxImages && !isLoading
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = "添加图片",
                        tint = if (images.size < maxImages) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline
                    )
                }
                
                // 输入框
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("描述你想要的HTML页面...") },
                    maxLines = 5,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp)
                )
                
                // 发送按钮
                FilledIconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }
            }
        }
    }
}

/**
 * 会话列表项
 */
@Composable
fun SessionListItem(
    session: HtmlCodingSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Chat,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${session.messages.size} 条消息 · ${formatDate(session.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * 检查点列表项
 */
@Composable
fun CheckpointListItem(
    checkpoint: ProjectCheckpoint,
    isCurrent: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isCurrent) 
            MaterialTheme.colorScheme.secondaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isCurrent) Icons.Filled.CheckCircle else Icons.Outlined.History,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isCurrent) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = checkpoint.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    text = "${checkpoint.files.size} 个文件 · ${formatTime(checkpoint.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            if (!isCurrent) {
                TextButton(onClick = onRestore) {
                    Text("恢复", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * 模板卡片组件
 */
@Composable
fun StyleTemplateCard(
    template: StyleTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else 
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // 颜色预览
            template.colorScheme?.let { colors ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(parseColor(colors.primary))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(parseColor(colors.secondary))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(parseColor(colors.accent))
                    )
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = template.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 风格参考卡片组件
 */
@Composable
fun StyleReferenceCard(
    style: StyleReference,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else 
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
        else 
            MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = style.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = style.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = style.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 关键词标签
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(style.keywords.take(4)) { keyword ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = keyword,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 配置面板组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(
    config: SessionConfig,
    onConfigChange: (SessionConfig) -> Unit,
    textModels: List<com.webtoapp.data.model.SavedModel>,
    imageModels: List<com.webtoapp.data.model.SavedModel>,
    rulesTemplates: List<RulesTemplate>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 文本模型选择
        Text("文本模型", style = MaterialTheme.typography.titleSmall)
        
        var textModelExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = textModelExpanded,
            onExpandedChange = { textModelExpanded = it }
        ) {
            OutlinedTextField(
                value = textModels.find { it.id == config.textModelId }?.let { 
                    it.alias ?: it.model.name 
                } ?: "请选择模型",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(textModelExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = textModelExpanded,
                onDismissRequest = { textModelExpanded = false }
            ) {
                textModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.alias ?: model.model.name) },
                        onClick = {
                            onConfigChange(config.copy(textModelId = model.id))
                            textModelExpanded = false
                        }
                    )
                }
            }
        }
        
        // 图像模型选择（可选）
        Text("图像模型（可选）", style = MaterialTheme.typography.titleSmall)
        
        var imageModelExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = imageModelExpanded,
            onExpandedChange = { imageModelExpanded = it }
        ) {
            OutlinedTextField(
                value = imageModels.find { it.id == config.imageModelId }?.let { 
                    it.alias ?: it.model.name 
                } ?: "不使用图像模型",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(imageModelExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = imageModelExpanded,
                onDismissRequest = { imageModelExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("不使用图像模型") },
                    onClick = {
                        onConfigChange(config.copy(imageModelId = null))
                        imageModelExpanded = false
                    }
                )
                imageModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.alias ?: model.model.name) },
                        onClick = {
                            onConfigChange(config.copy(imageModelId = model.id))
                            imageModelExpanded = false
                        }
                    )
                }
            }
        }
        
        // 温度滑块
        Text("温度: ${String.format("%.1f", config.temperature)}", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = config.temperature,
            onValueChange = { onConfigChange(config.copy(temperature = it)) },
            valueRange = 0f..2f,
            steps = 19
        )
        Text(
            text = "低温度(0): 更确定性、更保守\n高温度(2): 更随机、更创意",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        
        Divider()
        
        // Rules
        Text("对话规则", style = MaterialTheme.typography.titleSmall)
        
        // Rules模板选择
        var showRulesTemplates by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showRulesTemplates = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.LibraryBooks, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("从模板选择")
        }
        
        if (showRulesTemplates) {
            AlertDialog(
                onDismissRequest = { showRulesTemplates = false },
                title = { Text("选择规则模板") },
                text = {
                    LazyColumn {
                        items(rulesTemplates) { template ->
                            ListItem(
                                headlineContent = { Text(template.name) },
                                supportingContent = { Text(template.description) },
                                modifier = Modifier.clickable {
                                    onConfigChange(config.copy(rules = template.rules))
                                    showRulesTemplates = false
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRulesTemplates = false }) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 当前规则列表
        config.rules.forEachIndexed { index, rule ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}. $rule",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val newRules = config.rules.toMutableList()
                        newRules.removeAt(index)
                        onConfigChange(config.copy(rules = newRules))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        // 添加新规则
        var newRule by remember { mutableStateOf("") }
        OutlinedTextField(
            value = newRule,
            onValueChange = { newRule = it },
            placeholder = { Text("添加新规则...") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (newRule.isNotBlank()) {
                            onConfigChange(config.copy(rules = config.rules + newRule))
                            newRule = ""
                        }
                    },
                    enabled = newRule.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            },
            singleLine = true
        )
    }
}

// ==================== 辅助函数 ====================

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getLanguageColor(language: String): Color {
    return when (language.lowercase()) {
        "html", "htm" -> Color(0xFFE34C26)
        "css" -> Color(0xFF264DE4)
        "javascript", "js" -> Color(0xFFF7DF1E)
        "svg" -> Color(0xFFFFB13B)
        "json" -> Color(0xFF000000)
        else -> Color(0xFF6B7280)
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        if (colorString.startsWith("#")) {
            Color(android.graphics.Color.parseColor(colorString))
        } else if (colorString.startsWith("linear-gradient")) {
            // 简单处理渐变，取第一个颜色
            val colorMatch = Regex("#[0-9A-Fa-f]{6}").find(colorString)
            colorMatch?.let { Color(android.graphics.Color.parseColor(it.value)) } 
                ?: Color.Gray
        } else {
            Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}
