package com.webtoapp.ui.components.htmlcoding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.webtoapp.core.i18n.Strings
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.ai.htmlcoding.*
import com.webtoapp.ui.components.aimodule.ModelSelector
import com.webtoapp.ui.components.aimodule.ProviderIcon
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Theme-adaptive code block colors
@Composable
fun codeBlockColors(): CodeBlockColors {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    return if (isDark) {
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
            operator = Color(0xFF89DCEB)
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
            operator = Color(0xFF04A5E5)
        )
    }
}

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
    val operator: Color
)

// Simple Markdown text rendering - 对代码内容禁用Markdown
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    // 检测是否包含代码内容
    val isCodeContent = text.contains("```") || 
        text.contains("<!DOCTYPE") || 
        text.contains("<html") ||
        text.contains("<style>") ||
        text.contains("<script>") ||
        text.contains("function ") ||
        text.contains("const ") ||
        text.contains("let ") ||
        text.contains("var ")
    
    SelectionContainer {
        if (isCodeContent) {
            // 代码内容使用纯文本，保留所有字符
            Text(text = text, style = style, color = color, modifier = modifier)
        } else {
            // 普通文本使用 Markdown 渲染
            val annotatedString = parseMarkdown(text, color)
            Text(text = annotatedString, style = style, modifier = modifier)
        }
    }
}

@Composable
private fun parseMarkdown(text: String, baseColor: Color): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                (text.startsWith("*", i) && !text.startsWith("**", i)) || text.startsWith("_", i) -> {
                    val marker = text[i]
                    val end = text.indexOf(marker, i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                text.startsWith("`", i) && !text.startsWith("```", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground, color = primaryColor)) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                text.startsWith("[", i) -> {
                    val textEnd = text.indexOf("]", i)
                    val urlStart = text.indexOf("(", textEnd)
                    val urlEnd = text.indexOf(")", urlStart)
                    if (textEnd != -1 && urlStart == textEnd + 1 && urlEnd != -1) {
                        withStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline)) {
                            append(text.substring(i + 1, textEnd))
                        }
                        i = urlEnd + 1
                    } else { append(text[i]); i++ }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }
}


// Message bubble component
@Composable
fun MessageBubble(
    message: HtmlCodingMessage,
    onEditClick: () -> Unit = {},
    onPreviewCode: (CodeBlock) -> Unit = {},
    onCopyCode: (String) -> Unit = {},
    onDownloadCode: ((CodeBlock) -> Unit)? = null,
    onExportToProject: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 2.dp) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f, fill = false), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            if (!isUser && message.thinking != null) {
                ThinkingBlock(thinking = message.thinking!!)
                Spacer(Modifier.height(10.dp))
            }
            Surface(
                shape = RoundedCornerShape(topStart = if (isUser) 20.dp else 4.dp, topEnd = if (isUser) 4.dp else 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 500.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    if (isUser && message.images.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                            items(message.images) { imagePath ->
                                Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                                    AsyncImage(model = File(imagePath), contentDescription = null, modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                }
                            }
                        }
                    }
                    if (message.content.isNotBlank()) {
                        if (isUser) {
                            SelectionContainer { Text(message.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary) }
                        } else {
                            MarkdownText(text = message.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (message.isEdited) {
                        Text("(edited)", style = MaterialTheme.typography.labelSmall, color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (!isUser && message.codeBlocks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                CodeBlocksTabContainer(codeBlocks = message.codeBlocks, onPreview = onPreviewCode, onCopy = onCopyCode, onDownload = onDownloadCode, onExportToProject = onExportToProject)
            }
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isUser) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Edit, "Edit", Modifier.size(16.dp), MaterialTheme.colorScheme.outline)
                    }
                }
                Text(formatTime(message.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        if (isUser) {
            Spacer(Modifier.width(12.dp))
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondary, shadowElevation = 2.dp) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, null, Modifier.size(22.dp), MaterialTheme.colorScheme.onSecondary)
                }
            }
        }
    }
}


// Code blocks tab container
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeBlocksTabContainer(
    codeBlocks: List<CodeBlock>,
    onPreview: (CodeBlock) -> Unit,
    onCopy: (String) -> Unit,
    onDownload: ((CodeBlock) -> Unit)? = null,
    onExportToProject: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = codeBlockColors()
    val pagerState = rememberPagerState(pageCount = { codeBlocks.size })
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var showMoreMenu by remember { mutableStateOf(false) }
    
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = colors.background, shadowElevation = 4.dp) {
        Column {
            Surface(color = colors.headerBackground, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ScrollableTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            modifier = Modifier.weight(1f),
                            containerColor = Color.Transparent,
                            contentColor = colors.text,
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                if (tabPositions.isNotEmpty() && pagerState.currentPage < tabPositions.size) {
                                    Box(Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]).height(3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                                }
                            },
                            divider = {}
                        ) {
                            codeBlocks.forEachIndexed { index, block ->
                                Tab(selected = pagerState.currentPage == index, onClick = { scope.launch { pagerState.animateScrollToPage(index) } }, modifier = Modifier.height(40.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = getLanguageColor(block.language)) {
                                            Text(block.language.uppercase().take(3), style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp)
                                        }
                                        Text(block.filename?.takeIf { it.isNotBlank() } ?: getDefaultFilename(block.language), style = MaterialTheme.typography.labelMedium, color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else colors.text.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(start = 8.dp)) {
                            val currentBlock = codeBlocks.getOrNull(pagerState.currentPage)
                            if (currentBlock?.language in listOf("html", "htm", "css", "js")) {
                                IconButton(onClick = { currentBlock?.let { onPreview(it) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.PlayArrow, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(onClick = { currentBlock?.let { clipboardManager.setText(AnnotatedString(it.content)); onCopy(it.content) } }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = colors.text.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                            if (onDownload != null || onExportToProject != null) {
                                Box {
                                    IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = colors.text.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }
                                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                        onDownload?.let { download -> DropdownMenuItem(text = { Text(Strings.download) }, onClick = { showMoreMenu = false; currentBlock?.let { download(it) } }, leadingIcon = { Icon(Icons.Outlined.Download, null) }) }
                                        onExportToProject?.let { DropdownMenuItem(text = { Text(Strings.exportAll) }, onClick = { showMoreMenu = false; it() }, leadingIcon = { Icon(Icons.Outlined.FolderOpen, null) }) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                CodeContentView(code = codeBlocks[page].content, language = codeBlocks[page].language, colors = colors)
            }
            Surface(color = colors.headerBackground.copy(alpha = 0.5f), shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.fileCountFormat.format(pagerState.currentPage + 1, codeBlocks.size), style = MaterialTheme.typography.labelSmall, color = colors.text.copy(alpha = 0.5f))
                    codeBlocks.getOrNull(pagerState.currentPage)?.let { Text(Strings.linesCount.format(it.content.lines().size), style = MaterialTheme.typography.labelSmall, color = colors.text.copy(alpha = 0.5f)) }
                }
            }
        }
    }
}


// Code content view with syntax highlighting
@Composable
internal fun CodeContentView(code: String, language: String, colors: CodeBlockColors, modifier: Modifier = Modifier) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val lines = code.lines()
    SelectionContainer {
        Row(modifier = modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(verticalScrollState)) {
            Column(modifier = Modifier.background(colors.headerBackground.copy(alpha = 0.3f)).padding(horizontal = 12.dp, vertical = 12.dp)) {
                lines.forEachIndexed { index, _ ->
                    Text("${index + 1}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 20.sp), color = colors.lineNumber)
                }
            }
            Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState).padding(12.dp)) {
                Column {
                    lines.forEach { line ->
                        Text(highlightSyntax(line, language, colors), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 20.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun highlightSyntax(line: String, language: String, colors: CodeBlockColors): AnnotatedString {
    return buildAnnotatedString {
        when (language.lowercase()) {
            "html", "htm", "xml", "svg" -> highlightHtml(line, colors)
            "css" -> highlightCss(line, colors)
            "javascript", "js" -> highlightJs(line, colors)
            "json" -> highlightJson(line, colors)
            else -> withStyle(SpanStyle(color = colors.text)) { append(line) }
        }
    }
}

private fun AnnotatedString.Builder.highlightHtml(line: String, colors: CodeBlockColors) {
    var i = 0
    while (i < line.length) {
        when {
            line.startsWith("<!--", i) -> {
                val end = line.indexOf("-->", i).let { if (it != -1) it + 3 else line.length }
                withStyle(SpanStyle(color = colors.comment)) { append(line.substring(i, end)) }
                i = end
            }
            line[i] == '<' -> {
                val tagEnd = line.indexOf('>', i)
                if (tagEnd != -1) {
                    highlightHtmlTag(line.substring(i, tagEnd + 1), colors)
                    i = tagEnd + 1
                } else { withStyle(SpanStyle(color = colors.text)) { append(line[i]) }; i++ }
            }
            else -> { withStyle(SpanStyle(color = colors.text)) { append(line[i]) }; i++ }
        }
    }
}

private fun AnnotatedString.Builder.highlightHtmlTag(tag: String, colors: CodeBlockColors) {
    var i = 0
    while (i < tag.length) {
        when {
            tag[i] in "<>/" -> { withStyle(SpanStyle(color = colors.operator)) { append(tag[i]) }; i++ }
            tag[i] == '"' || tag[i] == '\'' -> {
                val quote = tag[i]
                val end = tag.indexOf(quote, i + 1)
                if (end != -1) { withStyle(SpanStyle(color = colors.string)) { append(tag.substring(i, end + 1)) }; i = end + 1 }
                else { withStyle(SpanStyle(color = colors.string)) { append(tag[i]) }; i++ }
            }
            tag[i] == '=' -> { withStyle(SpanStyle(color = colors.operator)) { append(tag[i]) }; i++ }
            tag[i].isLetter() || tag[i] == '-' || tag[i] == '_' -> {
                val start = i
                while (i < tag.length && (tag[i].isLetterOrDigit() || tag[i] == '-' || tag[i] == '_')) i++
                val word = tag.substring(start, i)
                val isTagName = start <= 1 || tag[start - 1] == '/' || tag[start - 1] == '<'
                withStyle(SpanStyle(color = if (isTagName) colors.tag else colors.attribute)) { append(word) }
            }
            else -> { withStyle(SpanStyle(color = colors.text)) { append(tag[i]) }; i++ }
        }
    }
}

private fun AnnotatedString.Builder.highlightCss(line: String, colors: CodeBlockColors) {
    var i = 0
    while (i < line.length) {
        when {
            line.startsWith("/*", i) -> {
                val end = line.indexOf("*/", i).let { if (it != -1) it + 2 else line.length }
                withStyle(SpanStyle(color = colors.comment)) { append(line.substring(i, end)) }
                i = end
            }
            line[i] == '"' || line[i] == '\'' -> {
                val quote = line[i]
                val end = line.indexOf(quote, i + 1)
                if (end != -1) { withStyle(SpanStyle(color = colors.string)) { append(line.substring(i, end + 1)) }; i = end + 1 }
                else { withStyle(SpanStyle(color = colors.string)) { append(line[i]) }; i++ }
            }
            line[i].isDigit() || (line[i] == '.' && i + 1 < line.length && line[i + 1].isDigit()) -> {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i].isLetter() || line[i] == '%')) i++
                withStyle(SpanStyle(color = colors.number)) { append(line.substring(start, i)) }
            }
            line[i] == '#' && i + 1 < line.length && line[i + 1].isLetterOrDigit() -> {
                val start = i; i++
                while (i < line.length && line[i].isLetterOrDigit()) i++
                withStyle(SpanStyle(color = colors.number)) { append(line.substring(start, i)) }
            }
            else -> { withStyle(SpanStyle(color = colors.text)) { append(line[i]) }; i++ }
        }
    }
}

private fun AnnotatedString.Builder.highlightJs(line: String, colors: CodeBlockColors) {
    val jsKeywords = setOf("const", "let", "var", "function", "return", "if", "else", "for", "while", "class", "extends", "new", "this", "import", "export", "from", "async", "await", "try", "catch", "throw", "finally", "switch", "case", "break", "continue", "default", "true", "false", "null", "undefined", "typeof", "instanceof")
    var i = 0
    while (i < line.length) {
        when {
            line.startsWith("//", i) -> { withStyle(SpanStyle(color = colors.comment)) { append(line.substring(i)) }; return }
            line.startsWith("/*", i) -> {
                val end = line.indexOf("*/", i).let { if (it != -1) it + 2 else line.length }
                withStyle(SpanStyle(color = colors.comment)) { append(line.substring(i, end)) }
                i = end
            }
            line[i] == '"' || line[i] == '\'' || line[i] == '`' -> {
                val quote = line[i]
                var end = i + 1
                while (end < line.length && line[end] != quote) { if (line[end] == '\\' && end + 1 < line.length) end++; end++ }
                if (end < line.length) end++
                withStyle(SpanStyle(color = colors.string)) { append(line.substring(i, end)) }
                i = end
            }
            line[i].isDigit() -> {
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.')) i++
                withStyle(SpanStyle(color = colors.number)) { append(line.substring(start, i)) }
            }
            line[i].isLetter() || line[i] == '_' || line[i] == '$' -> {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '$')) i++
                val word = line.substring(start, i)
                val color = when { word in jsKeywords -> colors.keyword; i < line.length && line[i] == '(' -> colors.function; else -> colors.text }
                withStyle(SpanStyle(color = color)) { append(word) }
            }
            else -> { withStyle(SpanStyle(color = colors.text)) { append(line[i]) }; i++ }
        }
    }
}

private fun AnnotatedString.Builder.highlightJson(line: String, colors: CodeBlockColors) {
    var i = 0
    while (i < line.length) {
        when {
            line[i] == '"' -> {
                val end = line.indexOf('"', i + 1)
                if (end != -1) {
                    val isKey = line.indexOf(':', end) != -1 && line.substring(end, minOf(line.indexOf(':', end) + 1, line.length)).all { it.isWhitespace() || it == ':' }
                    withStyle(SpanStyle(color = if (isKey) colors.attribute else colors.string)) { append(line.substring(i, end + 1)) }
                    i = end + 1
                } else { withStyle(SpanStyle(color = colors.string)) { append(line[i]) }; i++ }
            }
            line[i].isDigit() || (line[i] == '-' && i + 1 < line.length && line[i + 1].isDigit()) -> {
                val start = i; if (line[i] == '-') i++
                while (i < line.length && (line[i].isDigit() || line[i] == '.')) i++
                withStyle(SpanStyle(color = colors.number)) { append(line.substring(start, i)) }
            }
            line.startsWith("true", i) || line.startsWith("false", i) || line.startsWith("null", i) -> {
                val word = when { line.startsWith("true", i) -> "true"; line.startsWith("false", i) -> "false"; else -> "null" }
                withStyle(SpanStyle(color = colors.keyword)) { append(word) }
                i += word.length
            }
            else -> { withStyle(SpanStyle(color = colors.text)) { append(line[i]) }; i++ }
        }
    }
}


// Thinking block component
@Composable
fun ThinkingBlock(thinking: String, modifier: Modifier = Modifier, defaultExpanded: Boolean = true) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))) {
        Column(Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Psychology, null, Modifier.size(18.dp), MaterialTheme.colorScheme.tertiary) }
                    }
                    Text(Strings.thinking, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp), MaterialTheme.colorScheme.tertiary) }
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(tween(300)) + fadeIn(), exit = shrinkVertically(tween(300)) + fadeOut()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        SelectionContainer { Text(thinking, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)) }
                    }
                }
            }
        }
    }
}

// Streaming message bubble
@Composable
fun StreamingMessageBubble(thinking: String, content: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Start) {
        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 2.dp) {
            Box(contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "avatar")
                val scale by infiniteTransition.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "scale")
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size((22 * scale).dp), MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f, fill = false), horizontalAlignment = Alignment.Start) {
            if (thinking.isNotEmpty()) {
                StreamingThinkingBlock(thinking)
                Spacer(Modifier.height(10.dp))
            }
            if (content.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 1.dp, modifier = Modifier.widthIn(max = 500.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Box(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                            MarkdownText(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                        val cursorAlpha by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "cursorAlpha")
                        Text("|", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha))
                    }
                }
            } else if (thinking.isEmpty()) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.widthIn(max = 500.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { index ->
                            val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
                            val offsetY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = -8f, animationSpec = infiniteRepeatable(tween(400, delayMillis = index * 100), RepeatMode.Reverse), label = "offsetY")
                            Box(Modifier.offset(y = offsetY.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingThinkingBlock(thinking: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)), modifier = modifier.widthIn(max = 500.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val infiniteTransition = rememberInfiniteTransition(label = "thinking")
                val alpha by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Psychology, null, Modifier.size(18.dp), MaterialTheme.colorScheme.tertiary.copy(alpha = alpha)) }
                }
                Text(Strings.thinkingDots, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
                Text(thinking, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f))
            }
        }
    }
}


// Chat input area
@Composable
fun ChatInputArea(value: String, onValueChange: (String) -> Unit, images: List<String>, onAddImage: () -> Unit, onRemoveImage: (Int) -> Unit, onSend: () -> Unit, isLoading: Boolean, maxImages: Int = 3, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            AnimatedVisibility(visible = images.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    itemsIndexed(images) { index, imagePath ->
                        Box {
                            Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                                AsyncImage(model = File(imagePath), contentDescription = null, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            }
                            Surface(modifier = Modifier.size(24.dp).align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).clickable { onRemoveImage(index) }, shape = CircleShape, color = MaterialTheme.colorScheme.error, shadowElevation = 2.dp) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, contentDescription = Strings.remove, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(14.dp)) }
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = if (images.size < maxImages && !isLoading) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp).clickable(enabled = images.size < maxImages && !isLoading) { onAddImage() }) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Image, contentDescription = Strings.addImage, tint = if (images.size < maxImages && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp)) }
                }
                OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f), placeholder = { Text(Strings.describeHtmlPage, color = MaterialTheme.colorScheme.outline) }, maxLines = 5, enabled = !isLoading, shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                Surface(shape = CircleShape, color = if (value.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(44.dp).clickable(enabled = value.isNotBlank() && !isLoading) { onSend() }, shadowElevation = if (value.isNotBlank() && !isLoading) 4.dp else 0.dp) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        else Icon(Icons.Default.Send, contentDescription = Strings.btnSend, tint = if (value.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

// Session list item
@Composable
fun SessionListItem(session: HtmlCodingSession, isSelected: Boolean, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Chat, null, Modifier.size(20.dp), if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(Strings.messagesCount.format(session.messages.size) + " - " + formatDate(session.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Delete, Strings.btnDelete, Modifier.size(18.dp), MaterialTheme.colorScheme.outline) }
        }
    }
}

// Checkpoint list item
@Composable
fun CheckpointListItem(checkpoint: ProjectCheckpoint, isCurrent: Boolean, onRestore: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant, border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(if (isCurrent) Icons.Filled.CheckCircle else Icons.Outlined.History, null, Modifier.size(18.dp), if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(checkpoint.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal)
                Spacer(Modifier.height(2.dp))
                Text(Strings.filesCountShort.format(checkpoint.files.size) + " - " + formatTime(checkpoint.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            if (!isCurrent) { FilledTonalButton(onClick = onRestore, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text(Strings.btnRestore, style = MaterialTheme.typography.labelMedium) }; Spacer(Modifier.width(8.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Delete, Strings.btnDelete, Modifier.size(18.dp), MaterialTheme.colorScheme.outline) }
        }
    }
}


// Style template card
@Composable
fun StyleTemplateCard(template: StyleTemplate, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = template.colorScheme?.let { parseColor(it.background) } ?: MaterialTheme.colorScheme.surface
    val textColor = template.colorScheme?.let { parseColor(it.text) } ?: MaterialTheme.colorScheme.onSurface
    Surface(modifier = modifier.width(150.dp).height(190.dp).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), shadowElevation = if (isSelected) 12.dp else 4.dp, border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), color = bgColor) {
        Box {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                template.colorScheme?.let { colors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(colors.primary, colors.secondary, colors.accent).forEach { color ->
                            Surface(Modifier.size(20.dp), CircleShape, parseColor(color), shadowElevation = 2.dp, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))) {}
                        }
                    }
                }
                Column {
                    Text(template.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(Modifier.height(4.dp))
                    Text(template.description, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                }
            }
            if (isSelected) {
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}

// Style reference card
@Composable
fun StyleReferenceCard(style: StyleReference, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface, shadowElevation = if (isSelected) 4.dp else 1.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isSelected) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp)) } } }
                    Text(style.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) { Text(style.category.getDisplayName(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(style.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                style.keywords.take(4).forEach { keyword -> Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text(keyword, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }
    }
}


// Config panel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigPanel(
    config: SessionConfig, 
    onConfigChange: (SessionConfig) -> Unit, 
    textModels: List<com.webtoapp.data.model.SavedModel>, 
    imageModels: List<com.webtoapp.data.model.SavedModel>, 
    rulesTemplates: List<RulesTemplate>, 
    onNavigateToAiSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(Strings.sessionConfig, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        // Text model - 使用 ModelSelector 组件
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Column(Modifier.padding(16.dp)) {
                Text(Strings.textModel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                ModelSelector(
                    selectedModel = textModels.find { it.id == config.textModelId },
                    availableModels = textModels,
                    onModelSelected = { model -> onConfigChange(config.copy(textModelId = model.id)) },
                    onConfigureClick = onNavigateToAiSettings
                )
            }
        }
        
        // Image model - 使用自定义的图像模型选择器
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Column(Modifier.padding(16.dp)) {
                Text(Strings.imageModelOptional, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                ImageModelSelector(
                    selectedModel = imageModels.find { it.id == config.imageModelId },
                    availableModels = imageModels,
                    onModelSelected = { model -> onConfigChange(config.copy(imageModelId = model?.id)) },
                    onConfigureClick = onNavigateToAiSettings
                )
            }
        }
        
        // Temperature
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.temperature, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) { Text(String.format("%.1f", config.temperature), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                Slider(value = config.temperature, onValueChange = { onConfigChange(config.copy(temperature = it)) }, valueRange = 0f..2f, steps = 19)
                Text(Strings.temperatureHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        
        Divider()
        
        // 工具包配置
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.toolbox, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        Strings.nEnabled.format(config.enabledTools.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    Strings.toolboxHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(12.dp))
                
                HtmlToolType.entries.forEach { toolType ->
                    val isEnabled = toolType in config.enabledTools
                    val isRequired = toolType == HtmlToolType.WRITE_HTML
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                               else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                toolType.icon,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        toolType.getDisplayName(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (isRequired) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                Strings.required,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    // 需要图像模型的工具显示提示
                                    if (toolType.requiresImageModel && config.imageModelId.isNullOrBlank()) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                Strings.requiresImageModel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    toolType.getDescription(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            // 需要图像模型但未配置时禁用开关
                            val canEnable = !toolType.requiresImageModel || !config.imageModelId.isNullOrBlank()
                            Switch(
                                checked = isEnabled && canEnable,
                                onCheckedChange = { checked ->
                                    if ((!isRequired || checked) && canEnable) {
                                        val newTools = if (checked) {
                                            config.enabledTools + toolType
                                        } else {
                                            config.enabledTools - toolType
                                        }
                                        onConfigChange(config.copy(enabledTools = newTools))
                                    }
                                },
                                enabled = !isRequired && canEnable
                            )
                        }
                    }
                }
            }
        }
        
        Divider()
        
        // Rules
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Column(Modifier.padding(16.dp)) {
                Text(Strings.rules, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                var showRulesTemplates by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showRulesTemplates = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Outlined.LibraryBooks, null); Spacer(Modifier.width(8.dp)); Text(Strings.selectFromTemplate) }
                if (showRulesTemplates) {
                    AlertDialog(onDismissRequest = { showRulesTemplates = false }, title = { Text(Strings.selectRuleTemplate) }, text = {
                        LazyColumn { items(rulesTemplates) { template -> ListItem(headlineContent = { Text(template.name) }, supportingContent = { Text(template.description) }, modifier = Modifier.clickable { onConfigChange(config.copy(rules = template.rules)); showRulesTemplates = false }) } }
                    }, confirmButton = { TextButton(onClick = { showRulesTemplates = false }) { Text(Strings.btnCancel) } })
                }
                Spacer(Modifier.height(12.dp))
                val effectiveRules = config.getEffectiveRules()
                effectiveRules.forEachIndexed { index, rule ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(rule, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { 
                                // If it is默认规则（空列表），则先转为实际列表再删除
                                val newRules = if (config.rules.isEmpty()) {
                                    effectiveRules.toMutableList().apply { removeAt(index) }
                                } else {
                                    config.rules.toMutableList().apply { removeAt(index) }
                                }
                                onConfigChange(config.copy(rules = newRules))
                            }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, contentDescription = Strings.btnDelete, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                var newRule by remember { mutableStateOf("") }
                OutlinedTextField(value = newRule, onValueChange = { newRule = it }, placeholder = { Text(Strings.addNewRule) }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { if (newRule.isNotBlank()) { onConfigChange(config.copy(rules = config.rules + newRule)); newRule = "" } }, enabled = newRule.isNotBlank()) { Icon(Icons.Default.Add, contentDescription = Strings.add, tint = if (newRule.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) } }, singleLine = true, shape = RoundedCornerShape(12.dp))
            }
        }
    }
}

/**
 * 图像模型选择器（支持"不使用"选项）
 */
@Composable
private fun ImageModelSelector(
    selectedModel: com.webtoapp.data.model.SavedModel?,
    availableModels: List<com.webtoapp.data.model.SavedModel>,
    onModelSelected: (com.webtoapp.data.model.SavedModel?) -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        // Select器按钮
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 供应商图标
                ProviderIcon(
                    provider = selectedModel?.model?.provider,
                    modifier = Modifier.size(28.dp)
                )
                
                // 模型信息
                Column(modifier = Modifier.weight(1f)) {
                    if (selectedModel != null) {
                        Text(
                            selectedModel.alias ?: selectedModel.model.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            selectedModel.model.provider.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            Strings.noImageModel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 下拉箭头
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = Strings.expand,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 下拉菜单
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 280.dp, max = 360.dp).heightIn(max = 400.dp)
        ) {
            // 不使用选项
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚫", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(Strings.noImageModel)
                    }
                },
                onClick = {
                    onModelSelected(null)
                    expanded = false
                },
                modifier = Modifier.background(
                    if (selectedModel == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                )
            )
            
            if (availableModels.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    Strings.selectImageModel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ProviderIcon(
                                    provider = model.model.provider,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        model.alias ?: model.model.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedModel?.id == model.id) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        model.model.provider.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selectedModel?.id == model.id) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = Strings.selected,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onModelSelected(model)
                            expanded = false
                        },
                        modifier = Modifier.background(
                            if (selectedModel?.id == model.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Configure更多模型
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            Strings.configureMoreModels,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onConfigureClick()
                }
            )
        }
    }
}


// Helper functions
private fun formatTime(timestamp: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
private fun formatDate(timestamp: Long): String = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun getLanguageColor(language: String): Color = when (language.lowercase()) {
    "html", "htm" -> Color(0xFFE34C26)
    "css" -> Color(0xFF264DE4)
    "javascript", "js" -> Color(0xFFF0DB4F)
    "svg" -> Color(0xFFFFB13B)
    "json" -> Color(0xFF292929)
    "xml" -> Color(0xFF0060AC)
    "typescript", "ts" -> Color(0xFF3178C6)
    else -> Color(0xFF6B7280)
}

private fun getDefaultFilename(language: String): String = when (language.lowercase()) {
    "html", "htm" -> "index.html"
    "css" -> "style.css"
    "javascript", "js" -> "script.js"
    "svg" -> "image.svg"
    "json" -> "data.json"
    else -> "file.$language"
}

private fun parseColor(colorString: String): Color = try {
    if (colorString.startsWith("#")) Color(android.graphics.Color.parseColor(colorString))
    else if (colorString.startsWith("linear-gradient")) Regex("#[0-9A-Fa-f]{6}").find(colorString)?.let { Color(android.graphics.Color.parseColor(it.value)) } ?: Color.Gray
    else Color.Gray
} catch (e: Exception) { Color.Gray }

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue


// ==================== 项目文件面板组件 ====================

/**
 * 项目文件面板 - 显示会话的项目文件夹内容
 */
@Composable
fun ProjectFilesPanel(
    files: List<ProjectFileInfo>,
    selectedFile: ProjectFileInfo?,
    onFileClick: (ProjectFileInfo) -> Unit,
    onPreviewClick: (ProjectFileInfo) -> Unit,
    onRefresh: () -> Unit,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column {
            // 标题栏
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!isExpanded) },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            Strings.projectFiles,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "${files.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = Strings.refresh,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isExpanded) Strings.collapse else Strings.expand,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // File列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                Strings.noFilesYet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                Strings.aiCodeSavedHere,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // 按基础文件名分组
                    val groupedFiles = files.groupBy { it.getBaseName() }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        groupedFiles.forEach { (baseName, versions) ->
                            val sortedVersions = versions.sortedByDescending { it.version }
                            val latestVersion = sortedVersions.first()
                            
                            item(key = baseName) {
                                FileGroupItem(
                                    baseName = baseName,
                                    latestFile = latestVersion,
                                    versions = sortedVersions,
                                    isSelected = selectedFile?.getBaseName() == baseName,
                                    onFileClick = onFileClick,
                                    onPreviewClick = onPreviewClick
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
 * 文件组项目 - 显示一个文件的所有版本
 */
@Composable
private fun FileGroupItem(
    baseName: String,
    latestFile: ProjectFileInfo,
    versions: List<ProjectFileInfo>,
    isSelected: Boolean,
    onFileClick: (ProjectFileInfo) -> Unit,
    onPreviewClick: (ProjectFileInfo) -> Unit
) {
    var showVersions by remember { mutableStateOf(false) }
    
    Column {
        // 主文件项（最新版本）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFileClick(latestFile) },
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surface,
            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // File图标
                FileTypeIcon(type = latestFile.type, modifier = Modifier.size(32.dp))
                
                // File信息
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            latestFile.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (latestFile.version > 1) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "v${latestFile.version}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "${latestFile.formatSize()} · ${latestFile.formatTime()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                // Version历史按钮
                if (versions.size > 1) {
                    IconButton(
                        onClick = { showVersions = !showVersions },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (showVersions) Icons.Default.ExpandLess else Icons.Default.History,
                            contentDescription = Strings.versionHistory,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                // 预览按钮
                if (latestFile.type == ProjectFileType.HTML) {
                    IconButton(
                        onClick = { onPreviewClick(latestFile) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = "预览",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Version历史列表
        AnimatedVisibility(
            visible = showVersions && versions.size > 1,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 42.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                versions.drop(1).forEach { version ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFileClick(version) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                version.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                version.formatTime(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (version.type == ProjectFileType.HTML) {
                                IconButton(
                                    onClick = { onPreviewClick(version) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = "预览",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 文件类型图标
 */
@Composable
fun FileTypeIcon(type: ProjectFileType, modifier: Modifier = Modifier) {
    val (icon, color) = when (type) {
        ProjectFileType.HTML -> Icons.Outlined.Code to Color(0xFFE34C26)
        ProjectFileType.CSS -> Icons.Outlined.Palette to Color(0xFF264DE4)
        ProjectFileType.JS -> Icons.Outlined.Javascript to Color(0xFFF0DB4F)
        ProjectFileType.JSON -> Icons.Outlined.DataObject to Color(0xFF292929)
        ProjectFileType.SVG -> Icons.Outlined.Image to Color(0xFFFFB13B)
        ProjectFileType.IMAGE -> Icons.Outlined.Image to Color(0xFF4CAF50)
        ProjectFileType.OTHER -> Icons.Outlined.InsertDriveFile to Color(0xFF6B7280)
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                icon,
                contentDescription = type.name,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 文件预览面板
 */
@Composable
fun FilePreviewPanel(
    file: ProjectFileInfo?,
    content: String?,
    onClose: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (file == null || content == null) return
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FileTypeIcon(type = file.type, modifier = Modifier.size(28.dp))
                    Column {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${content.lines().size} 行 · ${file.formatSize()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (file.type == ProjectFileType.HTML) {
                        FilledTonalButton(
                            onClick = onPreview,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("预览")
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
            
            // 代码内容
            val colors = codeBlockColors()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                color = colors.background
            ) {
                CodeContentView(
                    code = content,
                    language = file.getExtension(),
                    colors = colors
                )
            }
        }
    }
}
