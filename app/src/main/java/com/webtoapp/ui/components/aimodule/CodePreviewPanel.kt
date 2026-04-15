package com.webtoapp.ui.components.aimodule

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
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
 * codepreviewpanel
 * 
 * show edit code, support, display, Tab switch
 * 
 * @param jsCode JavaScript code
 * @param cssCode CSS code
 * @param onJsCodeChange JavaScript code
 * @param onCssCodeChange CSS code
 * @param onCopy code
 * @param onValidate verifycode
 * @param onSave savecode
 * @param isEditable edit
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
            // header: Tab switch button
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
            
            // codecontentarea
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
            
            // bottomstatus bar
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
 * code Tab type
 */
enum class CodeTab(val displayName: String, val icon: String) {
    JAVASCRIPT("JavaScript", "JS"),
    CSS("CSS", "CSS")
}

/**
 * code colorconfig
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
 * code color
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
 * codepreviewheader
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
            // Tab switch
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
            
            // button
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Copybutton
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
                
                // Verifybutton( only editdisplay)
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
                
                // Savebutton
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
 * code Tab button
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
            // label
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
 * color
 */
private fun getLanguageColor(tab: CodeTab): Color {
    return when (tab) {
        CodeTab.JAVASCRIPT -> Color(0xFFF7DF1E) // JavaScript
        CodeTab.CSS -> Color(0xFF264DE4) // CSS
    }
}

/**
 * codecontentarea
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
    
    // sync code
    LaunchedEffect(code) {
        if (textFieldValue.text != code) {
            textFieldValue = TextFieldValue(code)
        }
    }
    
    // modeselectdisplay content
    // editmode: textFieldValue. text( editcontent)
    // Read- onlymode: code
    val displayText = if (isEditable) textFieldValue.text else code
    val displayLines = displayText.lines()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 400.dp)
            .verticalScroll(verticalScrollState)
    ) {
        // displayLines ensure withcontentsync
        LineNumberColumn(
            lineCount = displayLines.size,
            colors = colors
        )
        
        // codecontent
        Box(
            modifier = Modifier
                .weight(weight = 1f, fill = true)
                .horizontalScroll(horizontalScrollState)
                .padding(12.dp)
        ) {
            if (isEditable) {
                // editmode- text +
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
                        color = Color.Transparent // input text, display
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) { innerTextField ->
                    // Show
                    Box {
                        // displayLines ensurewithinputsync
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
                        // input( text, display handleinput)
                        Box(modifier = Modifier.matchParentSize()) {
                            innerTextField()
                        }
                    }
                }
            } else {
                // Read- onlymode- displayLines( from code)
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
 * Note
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
 * codepreviewbottomstatus bar
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
 * Note
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
 * JavaScript
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
            // Note
            line.startsWith("//", i) -> {
                withStyle(SpanStyle(color = colors.comment)) {
                    append(line.substring(i))
                }
                return
            }
            // Note
            line.startsWith("/*", i) -> {
                val end = line.indexOf("*/", i).let { if (it != -1) it + 2 else line.length }
                withStyle(SpanStyle(color = colors.comment)) {
                    append(line.substring(i, end))
                }
                i = end
            }
            // Note
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
            // Note
            line[i].isDigit() -> {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'x' || line[i] == 'X')) i++
                withStyle(SpanStyle(color = colors.number)) {
                    append(line.substring(start, i))
                }
            }
            // Note
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
            // Note
            line[i] in "+-*/%=<>!&|^~?:" -> {
                withStyle(SpanStyle(color = colors.operator)) {
                    append(line[i])
                }
                i++
            }
            // Note
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
 * CSS
 */
private fun AnnotatedString.Builder.highlightCss(line: String, colors: CodeBlockColors) {
    var i = 0
    while (i < line.length) {
        when {
            // Note
            line.startsWith("/*", i) -> {
                val end = line.indexOf("*/", i).let { if (it != -1) it + 2 else line.length }
                withStyle(SpanStyle(color = colors.comment)) {
                    append(line.substring(i, end))
                }
                i = end
            }
            // Note
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
            // Note
            line[i].isDigit() || (line[i] == '.' && i + 1 < line.length && line[i + 1].isDigit()) -> {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i].isLetter() || line[i] == '%')) i++
                withStyle(SpanStyle(color = colors.number)) {
                    append(line.substring(start, i))
                }
            }
            // Color #xxx
            line[i] == '#' && i + 1 < line.length && line[i + 1].isLetterOrDigit() -> {
                val start = i
                i++
                while (i < line.length && line[i].isLetterOrDigit()) i++
                withStyle(SpanStyle(color = colors.number)) {
                    append(line.substring(start, i))
                }
            }
            // Properties( )
            line[i].isLetter() || line[i] == '-' -> {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '-' || line[i] == '_')) i++
                val word = line.substring(start, i)
                // Check( )
                val isProperty = line.indexOf(':', i).let { colonIndex ->
                    colonIndex != -1 && line.substring(i, colonIndex).all { it.isWhitespace() }
                }
                val color = if (isProperty) colors.attribute else colors.text
                withStyle(SpanStyle(color = color)) {
                    append(word)
                }
            }
            // Select
            line[i] in ".#@:[]{}()," -> {
                withStyle(SpanStyle(color = colors.operator)) {
                    append(line[i])
                }
                i++
            }
            // Note
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
 * codepreview
 * for displaycode, supportedit
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
            // header
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
            
            // codecontent
            val horizontalScrollState = rememberScrollState()
            val verticalScrollState = rememberScrollState()
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(verticalScrollState)
            ) {
                // Note
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
                
                // code
                Box(
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
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
