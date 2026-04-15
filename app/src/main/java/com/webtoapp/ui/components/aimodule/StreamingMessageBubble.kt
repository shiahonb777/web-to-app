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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * message
 * 
 * for display AI content, support textdisplay animation
 * 
 * @param content messagecontent
 * @param isStreaming output
 * @param thinkingContent content( if)
 * @param isUser usermessage
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
        // messageheader( )
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
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
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
        
        // content( if)
        if (!thinkingContent.isNullOrBlank() && !isUser) {
            ThinkingBlock(
                content = thinkingContent,
                isStreaming = isStreaming,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .padding(bottom = 8.dp)
            )
        }
        
        // message
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
                    // contentdisplay indicator
                    TypingIndicator()
                } else {
                    // Showcontent optional
                    Row(verticalAlignment = Alignment.Bottom) {
                        val textColor = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        
                        SelectionContainer(modifier = Modifier.weight(weight = 1f, fill = false)) {
                            // codecontent( code orHTMLlabel)
                            // If it iscodecontent, textdisplay, Markdown code
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
                                // codecontent text,
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = textColor,
                                    lineHeight = 22.sp
                                )
                            } else {
                                // text Markdown
                                MarkdownStyledText(
                                    text = content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        
                        // outputdisplay
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
 * outputindicator
 * display messageheader, content
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
 * animation
 * outputdisplay
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
 * indicator
 * contentdisplay
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
 * usermessage
 * version, showuserinput message
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
 * AI message
 * version, show AI message
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
 * Markdown text
 * 
 * support Markdown
 * Note
 * * * or _ _
 * `code`
 * [ ] ( url)
 * 
 * @param text text
 * @param style text
 * @param color textcolor
 * @param lineHeight
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
 * Markdown text AnnotatedString
 * 
 * support
 * Note
 * * * or _ _
 * ` code`
 * [ text] ( url)
 */
@Composable
private fun parseMarkdownToAnnotatedString(text: String, baseColor: Color): AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // **text**
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
                // *text* or _text_( **)
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
                // code `code`( ```)
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
                // [ text] ( url)
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
                // Note
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
