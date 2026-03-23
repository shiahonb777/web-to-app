package com.webtoapp.ui.components.aimodule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 思考块组件
 * 
 * 用于显示 AI 的思考过程，具有独特的视觉样式，与普通消息区分开来
 * 
 * @param content 思考内容
 * @param isStreaming 是否正在流式输出
 * @param isExpanded 是否展开显示完整内容
 * @param onExpandToggle 展开/折叠切换回调
 * @param modifier Modifier
 * 
 * Requirements: 2.2
 */
@Composable
fun ThinkingBlock(
    content: String,
    isStreaming: Boolean = false,
    isExpanded: Boolean = true,
    onExpandToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    
    // 思考块的渐变背景色
    val gradientColors = listOf(
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    )
    
    // 边框颜色动画（流式输出时）
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )
    
    val borderColor = if (isStreaming) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = borderAlpha)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (onExpandToggle != null) {
                    Modifier.clickable { 
                        expanded = !expanded
                        onExpandToggle()
                    }
                } else {
                    Modifier.clickable { expanded = !expanded }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .padding(12.dp)
        ) {
            Column {
                // 头部
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 思考图标（带动画）
                        ThinkingIcon(isAnimating = isStreaming)
                        
                        Text(
                            "思考中...",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        
                        if (isStreaming) {
                            ThinkingDots()
                        }
                    }
                    
                    // Expand/折叠按钮
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "折叠" else "展开",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                // 内容区域
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 分隔线
                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 思考内容
                        if (content.isBlank() && isStreaming) {
                            // 等待内容时显示占位符
                            Text(
                                "正在分析...",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                
                                // 流式输出时显示光标
                                if (isStreaming) {
                                    TypingCursor(
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Collapse时显示预览
                if (!expanded && content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 思考图标（带旋转动画）
 */
@Composable
private fun ThinkingIcon(
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingIcon")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
        modifier = modifier.size(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "🤔",
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 思考中的动态点
 */
@Composable
private fun ThinkingDots(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 200
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = delay,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = alpha))
            )
        }
    }
}

/**
 * 简化版思考块
 * 用于在消息列表中显示思考过程的简洁版本
 */
@Composable
fun CompactThinkingBlock(
    content: String,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🤔", fontSize = 14.sp)
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "思考",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = content.ifBlank { "分析中..." },
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                if (isStreaming) {
                    TypingCursor(modifier = Modifier.padding(start = 2.dp))
                }
            }
        }
    }
}

/**
 * 思考步骤列表项
 * 用于显示多个思考步骤
 */
@Composable
fun ThinkingStepItem(
    step: Int,
    title: String,
    content: String,
    isActive: Boolean = false,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 步骤编号
        Surface(
            shape = CircleShape,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                isActive -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "$step",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
        
        // 内容
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    isActive -> MaterialTheme.colorScheme.tertiary
                    isCompleted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            if (content.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }
        }
        
        // 活动指示器
        if (isActive) {
            ThinkingDots()
        }
    }
}
