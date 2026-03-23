package com.webtoapp.ui.components.aimodule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 流式消息气泡组件
 * 
 * 用于实时显示 AI 生成的内容，支持流式文本显示和打字光标动画
 * 
 * @param content 消息内容
 * @param isStreaming 是否正在流式输出
 * @param thinkingContent 思考内容（如果有）
 * @param isUser 是否为用户消息
 * @param modifier Modifier
 * 
 * Requirements: 2.1, 3.4
 */
@Composable
fun StreamingMessageBubble(
    content: String,
    isStreaming: Boolean,
    thinkingContent: String? = null,
    isUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // 消息头部（角色标识）
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isUser) {
                Text(
                    "你",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("👤", fontSize = 12.sp)
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🤖", fontSize = 12.sp)
                    }
                }
                Text(
                    "AI 助手",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (isStreaming) {
                    StreamingIndicator()
                }
            }
        }
        
        // 思考内容块（如果有）
        if (!thinkingContent.isNullOrBlank() && !isUser) {
            ThinkingBlock(
                content = thinkingContent,
                isStreaming = isStreaming,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .padding(bottom = 8.dp)
            )
        }
        
        // 消息气泡
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                if (content.isBlank() && isStreaming) {
                    // 等待内容时显示打字指示器
                    TypingIndicator()
                } else {
                    // Show内容和可选的打字光标
                    Row(verticalAlignment = Alignment.Bottom) {
                        val textColor = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        
                        SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
                            // 检测是否包含代码内容（代码块或HTML标签）
                            // If it is代码内容，使用纯文本显示，避免Markdown渲染破坏代码
                            val isCodeContent = content.contains("```") || 
                                content.contains("<!DOCTYPE") || 
                                content.contains("<html") ||
                                content.contains("<style>") ||
                                content.contains("<script>") ||
                                content.contains("function ") ||
                                content.contains("const ") ||
                                content.contains("let ") ||
                                content.contains("var ")
                            
                            if (isCodeContent) {
                                // 代码内容使用纯文本，保留所有字符
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = textColor,
                                    lineHeight = 22.sp
                                )
                            } else {
                                // 普通文本使用 Markdown 渲染
                                MarkdownStyledText(
                                    text = content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        
                        // 流式输出时显示打字光标
                        if (isStreaming && !isUser) {
                            TypingCursor()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 流式输出指示器
 * 显示在消息头部，表示正在接收流式内容
 */
@Composable
private fun StreamingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.2f)
    ) {
        Text(
            "生成中",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * 打字光标动画
 * 在流式输出时显示闪烁的光标
 */
@Composable
fun TypingCursor(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    
    Box(
        modifier = modifier
            .padding(start = 2.dp)
            .width(2.dp)
            .height(16.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                RoundedCornerShape(1.dp)
            )
    )
}

/**
 * 打字指示器
 * 在等待内容时显示三个跳动的点
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * 用户消息气泡
 * 简化版本，用于显示用户输入的消息
 */
@Composable
fun UserMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    StreamingMessageBubble(
        content = content,
        isStreaming = false,
        isUser = true,
        modifier = modifier
    )
}

/**
 * AI 消息气泡
 * 简化版本，用于显示 AI 的响应消息
 */
@Composable
fun AssistantMessageBubble(
    content: String,
    isStreaming: Boolean = false,
    thinkingContent: String? = null,
    modifier: Modifier = Modifier
) {
    StreamingMessageBubble(
        content = content,
        isStreaming = isStreaming,
        thinkingContent = thinkingContent,
        isUser = false,
        modifier = modifier
    )
}

/**
 * Markdown 样式文本组件
 * 
 * 支持基本的 Markdown 语法渲染：
 * - **粗体**
 * - *斜体* 或 _斜体_
 * - `代码`
 * - [链接](url)
 * 
 * @param text 要渲染的文本
 * @param style 文本样式
 * @param color 基础文本颜色
 * @param lineHeight 行高
 */
@Composable
fun MarkdownStyledText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    lineHeight: androidx.compose.ui.unit.TextUnit = 22.sp,
    modifier: Modifier = Modifier
) {
    val annotatedString = parseMarkdownToAnnotatedString(text, color)
    Text(
        text = annotatedString,
        style = style,
        lineHeight = lineHeight,
        modifier = modifier
    )
}

/**
 * 解析 Markdown 文本为 AnnotatedString
 * 
 * 支持的语法：
 * - **粗体**
 * - *斜体* 或 _斜体_
 * - `内联代码`
 * - [链接文本](url)
 */
@Composable
private fun parseMarkdownToAnnotatedString(text: String, baseColor: Color): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // 粗体 **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 斜体 *text* 或 _text_ (但不是 **)
                (text.startsWith("*", i) && !text.startsWith("**", i)) || text.startsWith("_", i) -> {
                    val marker = text[i]
                    val end = text.indexOf(marker, i + 1)
                    if (end != -1 && end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 内联代码 `code` (但不是 ```)
                text.startsWith("`", i) && !text.startsWith("```", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBackground,
                                color = primaryColor
                            )
                        ) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 链接 [text](url)
                text.startsWith("[", i) -> {
                    val textEnd = text.indexOf("]", i)
                    val urlStart = if (textEnd != -1) text.indexOf("(", textEnd) else -1
                    val urlEnd = if (urlStart != -1) text.indexOf(")", urlStart) else -1
                    if (textEnd != -1 && urlStart == textEnd + 1 && urlEnd != -1) {
                        withStyle(
                            SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(text.substring(i + 1, textEnd))
                        }
                        i = urlEnd + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 普通字符
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
