package com.webtoapp.ui.components.htmlcoding

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Markdown 渲染组件
 * 支持基础 Markdown 语法和 Mermaid 图表
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val elements = remember(text) { parseMarkdown(text) }
    
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        elements.forEach { element ->
            when (element) {
                is MarkdownElement.Heading -> HeadingText(element, color)
                is MarkdownElement.Paragraph -> ParagraphText(element.content, color)
                is MarkdownElement.BulletList -> BulletListView(element.items, color)
                is MarkdownElement.NumberedList -> NumberedListView(element.items, color)
                is MarkdownElement.Quote -> QuoteView(element.content, color)
                is MarkdownElement.CodeBlock -> CodeBlockView(element)
                is MarkdownElement.MermaidChart -> MermaidChartView(element.code)
                is MarkdownElement.Table -> TableView(element)
                is MarkdownElement.HorizontalRule -> HorizontalRuleView()
            }
        }
    }
}

/**
 * 标题渲染
 */
@Composable
private fun HeadingText(heading: MarkdownElement.Heading, color: Color) {
    val style = when (heading.level) {
        1 -> MaterialTheme.typography.headlineMedium
        2 -> MaterialTheme.typography.headlineSmall
        3 -> MaterialTheme.typography.titleLarge
        4 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    Text(
        text = heading.content,
        style = style,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

/**
 * 段落渲染（支持行内格式）
 */
@Composable
private fun ParagraphText(content: String, color: Color) {
    SelectionContainer {
        Text(
            text = parseInlineStyles(content),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

/**
 * 无序列表
 */
@Composable
private fun BulletListView(items: List<String>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row {
                Text("• ", color = color, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = parseInlineStyles(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
        }
    }
}

/**
 * 有序列表
 */
@Composable
private fun NumberedListView(items: List<String>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            Row {
                Text("${index + 1}. ", color = color, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = parseInlineStyles(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
        }
    }
}

/**
 * 引用块
 */
@Composable
private fun QuoteView(content: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(IntrinsicSize.Max)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = parseInlineStyles(content),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 代码块（非 Mermaid）
 */
@Composable
private fun CodeBlockView(codeBlock: MarkdownElement.CodeBlock) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Column {
            // 语言标签
            if (codeBlock.language.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = codeBlock.language.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 代码内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = codeBlock.code,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

/**
 * Mermaid 图表渲染（使用 WebView）
 */
@Composable
private fun MermaidChartView(code: String) {
    val context = LocalContext.current
    val bgColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    val html = remember(code, isDark) {
        buildMermaidHtml(code, isDark, bgColor.toArgb(), textColor.toArgb())
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 400.dp),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    setBackgroundColor(bgColor.toArgb())
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 表格渲染
 */
@Composable
private fun TableView(table: MarkdownElement.Table) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val headerBg = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Column {
            // 表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                table.headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // 数据行
            table.rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 水平分隔线
 */
@Composable
private fun HorizontalRuleView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// ===== 解析逻辑 =====

sealed class MarkdownElement {
    data class Heading(val level: Int, val content: String) : MarkdownElement()
    data class Paragraph(val content: String) : MarkdownElement()
    data class BulletList(val items: List<String>) : MarkdownElement()
    data class NumberedList(val items: List<String>) : MarkdownElement()
    data class Quote(val content: String) : MarkdownElement()
    data class CodeBlock(val language: String, val code: String) : MarkdownElement()
    data class MermaidChart(val code: String) : MarkdownElement()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownElement()
    object HorizontalRule : MarkdownElement()
}

/**
 * 解析 Markdown 文本为元素列表
 */
private fun parseMarkdown(text: String): List<MarkdownElement> {
    val elements = mutableListOf<MarkdownElement>()
    val lines = text.lines()
    var i = 0
    
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        
        when {
            // 代码块
            trimmed.startsWith("```") -> {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                val code = codeLines.joinToString("\n")
                if (lang.equals("mermaid", ignoreCase = true)) {
                    elements.add(MarkdownElement.MermaidChart(code))
                } else {
                    elements.add(MarkdownElement.CodeBlock(lang, code))
                }
                i++
            }
            // 标题
            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length
                val content = trimmed.drop(level).trim()
                elements.add(MarkdownElement.Heading(level.coerceIn(1, 6), content))
                i++
            }
            // 水平线
            trimmed.matches(Regex("^[-*_]{3,}$")) -> {
                elements.add(MarkdownElement.HorizontalRule)
                i++
            }
            // 无序列表
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val l = lines[i].trim()
                    if (l.startsWith("- ") || l.startsWith("* ")) {
                        items.add(l.drop(2))
                        i++
                    } else break
                }
                elements.add(MarkdownElement.BulletList(items))
            }
            // 有序列表
            trimmed.matches(Regex("^\\d+\\. .+")) -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val l = lines[i].trim()
                    if (l.matches(Regex("^\\d+\\. .+"))) {
                        items.add(l.substringAfter(". "))
                        i++
                    } else break
                }
                elements.add(MarkdownElement.NumberedList(items))
            }
            // 引用
            trimmed.startsWith("> ") -> {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("> ")) {
                    quoteLines.add(lines[i].trim().removePrefix("> "))
                    i++
                }
                elements.add(MarkdownElement.Quote(quoteLines.joinToString("\n")))
            }
            // 表格
            trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().let { it.startsWith("|") && it.endsWith("|") }) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                if (tableLines.size >= 2) {
                    val headers = tableLines[0].split("|").filter { it.isNotBlank() }.map { it.trim() }
                    val rows = tableLines.drop(2).map { row ->
                        row.split("|").filter { it.isNotBlank() }.map { it.trim() }
                    }
                    elements.add(MarkdownElement.Table(headers, rows))
                }
            }
            // 普通段落
            trimmed.isNotBlank() -> {
                val paragraphLines = mutableListOf<String>()
                while (i < lines.size) {
                    val l = lines[i].trim()
                    if (l.isBlank() || l.startsWith("#") || l.startsWith("```") || 
                        l.startsWith("- ") || l.startsWith("* ") || l.startsWith("> ") ||
                        l.matches(Regex("^\\d+\\. .+")) || l.matches(Regex("^[-*_]{3,}$"))) {
                        break
                    }
                    paragraphLines.add(l)
                    i++
                }
                elements.add(MarkdownElement.Paragraph(paragraphLines.joinToString(" ")))
            }
            else -> i++
        }
    }
    
    return elements
}

/**
 * 解析行内样式（加粗、斜体、代码、链接等）
 */
private fun parseInlineStyles(text: String): AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        
        while (remaining.isNotEmpty()) {
            when {
                // 加粗 **text**
                remaining.startsWith("**") -> {
                    val end = remaining.indexOf("**", 2)
                    if (end > 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(remaining.substring(2, end))
                        }
                        remaining = remaining.substring(end + 2)
                    } else {
                        append("**")
                        remaining = remaining.substring(2)
                    }
                }
                // 斜体 *text*
                remaining.startsWith("*") && !remaining.startsWith("**") -> {
                    val end = remaining.indexOf("*", 1)
                    if (end > 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(remaining.substring(1, end))
                        }
                        remaining = remaining.substring(end + 1)
                    } else {
                        append("*")
                        remaining = remaining.substring(1)
                    }
                }
                // 删除线 ~~text~~
                remaining.startsWith("~~") -> {
                    val end = remaining.indexOf("~~", 2)
                    if (end > 2) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(remaining.substring(2, end))
                        }
                        remaining = remaining.substring(end + 2)
                    } else {
                        append("~~")
                        remaining = remaining.substring(2)
                    }
                }
                // 行内代码 `code`
                remaining.startsWith("`") && !remaining.startsWith("```") -> {
                    val end = remaining.indexOf("`", 1)
                    if (end > 1) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x20808080)
                        )) {
                            append(remaining.substring(1, end))
                        }
                        remaining = remaining.substring(end + 1)
                    } else {
                        append("`")
                        remaining = remaining.substring(1)
                    }
                }
                else -> {
                    append(remaining.first())
                    remaining = remaining.drop(1)
                }
            }
        }
    }
}

/**
 * 构建 Mermaid 图表的 HTML
 */
private fun buildMermaidHtml(code: String, isDark: Boolean, bgColor: Int, textColor: Int): String {
    val theme = if (isDark) "dark" else "default"
    val bgHex = String.format("#%06X", 0xFFFFFF and bgColor)
    
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            background: $bgHex; 
            display: flex; 
            justify-content: center; 
            align-items: center;
            min-height: 100vh;
            padding: 16px;
        }
        .mermaid { 
            max-width: 100%; 
            overflow-x: auto;
        }
    </style>
</head>
<body>
    <div class="mermaid">
$code
    </div>
    <script>
        mermaid.initialize({ 
            startOnLoad: true, 
            theme: '$theme',
            securityLevel: 'loose'
        });
    </script>
</body>
</html>
    """.trimIndent()
}

/**
 * 计算颜色亮度
 */
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
