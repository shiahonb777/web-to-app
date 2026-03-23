package com.webtoapp.ui.components.aimodule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 代码预览面板组件
 * 
 * 用于显示和编辑生成的代码，支持语法高亮、行号显示、Tab 切换
 * 
 * @param jsCode JavaScript 代码
 * @param cssCode CSS 代码
 * @param onJsCodeChange JavaScript 代码变更回调
 * @param onCssCodeChange CSS 代码变更回调
 * @param onCopy 复制代码回调
 * @param onValidate 验证代码回调
 * @param onSave 保存代码回调
 * @param isEditable 是否可编辑
 * @param modifier Modifier
 * 
 * Requirements: 4.3, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7
 */
@Composable
fun CodePreviewPanel(
    jsCode: String,
    cssCode: String,
    onJsCodeChange: (String) -> Unit,
    onCssCodeChange: (String) -> Unit,
    onCopy: (String) -> Unit,
    onValidate: () -> Unit,
    onSave: () -> Unit,
    isEditable: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CodeTab.JAVASCRIPT) }
    val colors = rememberCodeBlockColors()
    val clipboardManager = LocalClipboardManager.current
    
    val currentCode = when (selectedTab) {
        CodeTab.JAVASCRIPT -> jsCode
        CodeTab.CSS -> cssCode
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.background,
        shadowElevation = 4.dp
    ) {
        Column {
            // 头部：Tab 切换和操作按钮
            CodePreviewHeader(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(currentCode))
                    onCopy(currentCode)
                },
                onValidate = onValidate,
                onSave = onSave,
                isEditable = isEditable,
                colors = colors
            )
            
            // 代码内容区域
            CodeContentArea(
                code = currentCode,
                language = when (selectedTab) {
                    CodeTab.JAVASCRIPT -> "javascript"
                    CodeTab.CSS -> "css"
                },
                onCodeChange = { newCode ->
                    when (selectedTab) {
                        CodeTab.JAVASCRIPT -> onJsCodeChange(newCode)
                        CodeTab.CSS -> onCssCodeChange(newCode)
                    }
                },
                isEditable = isEditable,
                colors = colors
            )
            
            // 底部状态栏
            CodePreviewFooter(
                lineCount = currentCode.lines().size,
                language = when (selectedTab) {
                    CodeTab.JAVASCRIPT -> "JavaScript"
                    CodeTab.CSS -> "CSS"
                },
                colors = colors
            )
        }
    }
}

/**
 * 代码 Tab 类型
 */
enum class CodeTab(val displayName: String, val icon: String) {
    JAVASCRIPT("JavaScript", "JS"),
    CSS("CSS", "CSS")
}

/**
 * 代码块颜色配置
 */
data class CodeBlockColors(
    val background: Color,
    val headerBackground: Color,
    val text: Color,
    val lineNumber: Color,
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val function: Color,
    val tag: Color,
    val attribute: Color,
    val number: Color,
    val operator: Color,
    val selection: Color
)

/**
 * 获取主题自适应的代码块颜色
 */
@Composable
fun rememberCodeBlockColors(): CodeBlockColors {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    return remember(isDark) {
        if (isDark) {
            CodeBlockColors(
                background = Color(0xFF1E1E2E),
                headerBackground = Color(0xFF313244),
                text = Color(0xFFCDD6F4),
                lineNumber = Color(0xFF6C7086),
                keyword = Color(0xFFCBA6F7),
                string = Color(0xFFA6E3A1),
                comment = Color(0xFF6C7086),
                function = Color(0xFF89B4FA),
                tag = Color(0xFFF38BA8),
                attribute = Color(0xFFFAB387),
                number = Color(0xFFFAB387),
                operator = Color(0xFF89DCEB),
                selection = Color(0xFF45475A)
            )
        } else {
            CodeBlockColors(
                background = Color(0xFFF8F9FC),
                headerBackground = Color(0xFFE8EAF0),
                text = Color(0xFF1E1E2E),
                lineNumber = Color(0xFF9CA0B0),
                keyword = Color(0xFF8839EF),
                string = Color(0xFF40A02B),
                comment = Color(0xFF9CA0B0),
                function = Color(0xFF1E66F5),
                tag = Color(0xFFD20F39),
                attribute = Color(0xFFFE640B),
                number = Color(0xFFFE640B),
                operator = Color(0xFF04A5E5),
                selection = Color(0xFFDCE0E8)
            )
        }
    }
}

/**
 * 代码预览头部
 */
@Composable
private fun CodePreviewHeader(
    selectedTab: CodeTab,
    onTabSelected: (CodeTab) -> Unit,
    onCopy: () -> Unit,
    onValidate: () -> Unit,
    onSave: () -> Unit,
    isEditable: Boolean,
    colors: CodeBlockColors
) {
    Surface(
        color = colors.headerBackground,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 切换
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CodeTab.entries.forEach { tab ->
                    CodeTabButton(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        colors = colors
                    )
                }
            }
            
            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Copy按钮
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制",
                        tint = colors.text.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Verify按钮（仅在可编辑时显示）
                if (isEditable) {
                    IconButton(
                        onClick = onValidate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "验证",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // Save按钮
                FilledTonalButton(
                    onClick = onSave,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "保存",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}


/**
 * 代码 Tab 按钮
 */
@Composable
private fun CodeTabButton(
    tab: CodeTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: CodeBlockColors
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        colors.text.copy(alpha = 0.7f)
    }
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 语言标签
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = getLanguageColor(tab)
            ) {
                Text(
                    text = tab.icon,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = tab.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * 获取语言对应的颜色
 */
private fun getLanguageColor(tab: CodeTab): Color {
    return when (tab) {
        CodeTab.JAVASCRIPT -> Color(0xFFF7DF1E) // JavaScript 黄色
        CodeTab.CSS -> Color(0xFF264DE4) // CSS 蓝色
    }
}

/**
 * 代码内容区域
 */
@Composable
private fun CodeContentArea(
    code: String,
    language: String,
    onCodeChange: (String) -> Unit,
    isEditable: Boolean,
    colors: CodeBlockColors
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    
    var textFieldValue by remember(code) { 
        mutableStateOf(TextFieldValue(code)) 
    }
    
    // 同步外部代码变化
    LaunchedEffect(code) {
        if (textFieldValue.text != code) {
            textFieldValue = TextFieldValue(code)
        }
    }
    
    // 根据模式选择显示的内容源
    // 可编辑模式：使用 textFieldValue.text（实时编辑内容）
    // Read-only模式：使用 code 参数
    val displayText = if (isEditable) textFieldValue.text else code
    val displayLines = displayText.lines()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 400.dp)
            .verticalScroll(verticalScrollState)
    ) {
        // 行号列 - 使用 displayLines 确保行号与内容同步
        LineNumberColumn(
            lineCount = displayLines.size,
            colors = colors
        )
        
        // 代码内容
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
                .padding(12.dp)
        ) {
            if (isEditable) {
                // 可编辑模式 - 使用透明文本 + 语法高亮装饰层
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        onCodeChange(newValue.text)
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = Color.Transparent // 输入层文本透明，只显示光标
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) { innerTextField ->
                    // Show语法高亮的装饰器
                    Box {
                        // 语法高亮层 - 使用 displayLines 确保与输入同步
                        Column {
                            displayLines.forEach { line ->
                                Text(
                                    text = highlightSyntax(line, language, colors),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }
                        // 实际输入层（透明文本，只显示光标和处理输入）
                        Box(modifier = Modifier.matchParentSize()) {
                            innerTextField()
                        }
                    }
                }
            } else {
                // Read-only模式 - 使用 displayLines（已从 code 计算）
                SelectionContainer {
                    Column {
                        displayLines.forEach { line ->
                            Text(
                                text = highlightSyntax(line, language, colors),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 行号列
 */
@Composable
private fun LineNumberColumn(
    lineCount: Int,
    colors: CodeBlockColors
) {
    Column(
        modifier = Modifier
            .background(colors.headerBackground.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        repeat(lineCount) { index ->
            Text(
                text = "${index + 1}",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 20.sp
                ),
                color = colors.lineNumber
            )
        }
    }
}

/**
 * 代码预览底部状态栏
 */
@Composable
private fun CodePreviewFooter(
    lineCount: Int,
    language: String,
    colors: CodeBlockColors
) {
    Surface(
        color = colors.headerBackground.copy(alpha = 0.5f),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = colors.text.copy(alpha = 0.5f)
            )
            
            Text(
                text = "$lineCount 行",
                style = MaterialTheme.typography.labelSmall,
                color = colors.text.copy(alpha = 0.5f)
            )
        }
    }
}


/**
 * 语法高亮
 */
@Composable
private fun highlightSyntax(line: String, language: String, colors: CodeBlockColors): AnnotatedString {
    return buildAnnotatedString {
        when (language.lowercase()) {
            "javascript", "js" -> highlightJavaScript(line, colors)
            "css" -> highlightCss(line, colors)
            else -> withStyle(SpanStyle(color = colors.text)) { append(line) }
        }
    }
}

/**
 * JavaScript 语法高亮
 */
private fun AnnotatedString.Builder.highlightJavaScript(line: String, colors: CodeBlockColors) {
    val jsKeywords = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while",
        "class", "extends", "new", "this", "import", "export", "from", "async",
        "await", "try", "catch", "throw", "finally", "switch", "case", "break",
        "continue", "default", "true", "false", "null", "undefined", "typeof",
        "instanceof", "in", "of", "delete", "void", "yield", "static", "get", "set"
    )
    
    var i = 0
    while (i < line.length) {
        when {
            // 单行注释
            line.startsWith("//", i) -> {
                withStyle(SpanStyle(color = colors.comment)) {
                    append(line.substring(i))
                }
                return
            }
            // 多行注释
            line.startsWith("/*", i) -> {
                val end = line.indexOf("*/", i).let { if (it != -1) it + 2 else line.length }
                withStyle(SpanStyle(color = colors.comment)) {
                    append(line.substring(i, end))
                }
                i = end
            }
            // 字符串（双引号、单引号、模板字符串）
            line[i] == '"' || line[i] == '\'' || line[i] == '`' -> {
                val quote = line[i]
                var end = i + 1
                while (end < line.length && line[end] != quote) {
                    if (line[end] == '\\' && end + 1 < line.length) end++
                    end++
                }
                if (end < line.length) end++
                withStyle(SpanStyle(color = colors.string)) {
                    append(line.substring(i, end))
                }
                i = end
            }
            // 数字
            line[i].isDigit() -> {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'x' || line[i] == 'X')) i++
                withStyle(SpanStyle(color = colors.number)) {
                    append(line.substring(start, i))
                }
            }
            // 标识符和关键字
            line[i].isLetter() || line[i] == '_' || line[i] == '$' -> {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '$')) i++
                val word = line.substring(start, i)
                val color = when {
                    word in jsKeywords -> colors.keyword
                    i < line.length && line[i] == '(' -> colors.function
                    else -> colors.text
                }
                withStyle(SpanStyle(color = color)) {
                    append(word)
                }
            }
            // 运算符
            line[i] in "+-*/%=<>!&|^~?:" -> {
                withStyle(SpanStyle(color = colors.operator)) {
                    append(line[i])
                }
                i++
            }
            // 其他字符
            else -> {
                withStyle(SpanStyle(color = colors.text)) {
                    append(line[i])
                }
                i++
            }
        }
    }
}

/**
 * CSS 语法高亮
 */
private fun AnnotatedString.Builder.highlightCss(line: String, colors: CodeBlockColors) {
    var i = 0
    while (i < line.length) {
        when {
            // 注释
            line.startsWith("/*", i) -> {
                val end = line.indexOf("*/", i).let { if (it != -1) it + 2 else line.length }
                withStyle(SpanStyle(color = colors.comment)) {
                    append(line.substring(i, end))
                }
                i = end
            }
            // 字符串
            line[i] == '"' || line[i] == '\'' -> {
                val quote = line[i]
                val end = line.indexOf(quote, i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(color = colors.string)) {
                        append(line.substring(i, end + 1))
                    }
                    i = end + 1
                } else {
                    withStyle(SpanStyle(color = colors.string)) {
                        append(line[i])
                    }
                    i++
                }
            }
            // 数字和单位
            line[i].isDigit() || (line[i] == '.' && i + 1 < line.length && line[i + 1].isDigit()) -> {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i].isLetter() || line[i] == '%')) i++
                withStyle(SpanStyle(color = colors.number)) {
                    append(line.substring(start, i))
                }
            }
            // Color值 #xxx
            line[i] == '#' && i + 1 < line.length && line[i + 1].isLetterOrDigit() -> {
                val start = i
                i++
                while (i < line.length && line[i].isLetterOrDigit()) i++
                withStyle(SpanStyle(color = colors.number)) {
                    append(line.substring(start, i))
                }
            }
            // Properties名（在冒号前）
            line[i].isLetter() || line[i] == '-' -> {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '-' || line[i] == '_')) i++
                val word = line.substring(start, i)
                // Check是否是属性名（后面跟着冒号）
                val isProperty = line.indexOf(':', i).let { colonIndex ->
                    colonIndex != -1 && line.substring(i, colonIndex).all { it.isWhitespace() }
                }
                val color = if (isProperty) colors.attribute else colors.text
                withStyle(SpanStyle(color = color)) {
                    append(word)
                }
            }
            // Select器符号
            line[i] in ".#@:[]{}()," -> {
                withStyle(SpanStyle(color = colors.operator)) {
                    append(line[i])
                }
                i++
            }
            // 其他字符
            else -> {
                withStyle(SpanStyle(color = colors.text)) {
                    append(line[i])
                }
                i++
            }
        }
    }
}

/**
 * 简化版代码预览组件
 * 用于只读显示代码，不支持编辑
 */
@Composable
fun SimpleCodePreview(
    code: String,
    language: String,
    onCopy: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = rememberCodeBlockColors()
    val clipboardManager = LocalClipboardManager.current
    val lines = code.lines()
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.background,
        shadowElevation = 2.dp
    ) {
        Column {
            // 头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.headerBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (language.lowercase()) {
                            "javascript", "js" -> Color(0xFFF7DF1E)
                            "css" -> Color(0xFF264DE4)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Text(
                            text = language.uppercase().take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "${lines.size} 行",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.text.copy(alpha = 0.6f)
                    )
                }
                
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        onCopy(code)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制",
                        tint = colors.text.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // 代码内容
            val horizontalScrollState = rememberScrollState()
            val verticalScrollState = rememberScrollState()
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(verticalScrollState)
            ) {
                // 行号
                Column(
                    modifier = Modifier
                        .background(colors.headerBackground.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            text = "${index + 1}",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 18.sp
                            ),
                            color = colors.lineNumber
                        )
                    }
                }
                
                // 代码
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                        .padding(8.dp)
                ) {
                    SelectionContainer {
                        Column {
                            lines.forEach { line ->
                                Text(
                                    text = highlightSyntax(line, language, colors),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Color luminance extension
 */
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
