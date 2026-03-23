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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.extension.agent.ToolCallInfo
import com.webtoapp.core.extension.agent.ToolStatus

/**
 * 工具调用卡片组件
 * 
 * 用于显示 Agent 工具调用的详细信息，包括工具名称、图标、状态、参数和结果
 * 支持折叠/展开详情
 * 
 * @param toolCall 工具调用信息
 * @param isExpanded 是否展开显示详情
 * @param onExpandToggle 展开/折叠切换回调
 * @param modifier Modifier
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6
 */
@Composable
fun ToolCallCard(
    toolCall: ToolCallInfo,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember(isExpanded) { mutableStateOf(isExpanded) }
    
    // 根据状态确定颜色
    val (borderColor, backgroundColor) = getToolCallColors(toolCall.status)
    
    // Execute中动画
    val infiniteTransition = rememberInfiniteTransition(label = "toolCall")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )
    
    val animatedBorderColor = if (toolCall.status == ToolStatus.EXECUTING) {
        borderColor.copy(alpha = borderAlpha)
    } else {
        borderColor
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable {
                expanded = !expanded
                onExpandToggle()
            },
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 头部：工具名称、图标、状态
            ToolCallHeader(
                toolCall = toolCall,
                isExpanded = expanded
            )
            
            // Expand的详情区域
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Parameters显示
                    if (toolCall.parameters.isNotEmpty()) {
                        ToolCallParameters(parameters = toolCall.parameters)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // 结果显示
                    if (toolCall.status == ToolStatus.SUCCESS || toolCall.status == ToolStatus.FAILED) {
                        ToolCallResult(toolCall = toolCall)
                    }
                }
            }
            
            // Collapse时显示简要信息
            if (!expanded && toolCall.status == ToolStatus.SUCCESS) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getResultSummary(toolCall),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 工具调用头部
 */
@Composable
private fun ToolCallHeader(
    toolCall: ToolCallInfo,
    isExpanded: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 工具图标
        ToolIcon(
            icon = toolCall.toolIcon,
            status = toolCall.status
        )
        
        // 工具名称和状态
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getToolDisplayName(toolCall.toolName),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 状态标签
                ToolStatusBadge(status = toolCall.status)
                
                // Execute时间（如果已完成）
                if (toolCall.executionTimeMs > 0) {
                    Text(
                        text = formatExecutionTime(toolCall.executionTimeMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        // Expand/折叠按钮
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "折叠" else "展开",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * 工具图标
 */
@Composable
private fun ToolIcon(
    icon: String,
    status: ToolStatus,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        ToolStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
        ToolStatus.EXECUTING -> MaterialTheme.colorScheme.primaryContainer
        ToolStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ToolStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
    }
    
    // Execute中旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "iconRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (status == ToolStatus.EXECUTING) {
                    Modifier.rotate(rotation)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
    }
}

/**
 * 工具状态标签
 */
@Composable
private fun ToolStatusBadge(status: ToolStatus) {
    val (text, color, backgroundColor) = when (status) {
        ToolStatus.PENDING -> Triple("等待中", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        ToolStatus.EXECUTING -> Triple("执行中", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ToolStatus.SUCCESS -> Triple("成功", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ToolStatus.FAILED -> Triple("失败", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Execute中显示动画点
            if (status == ToolStatus.EXECUTING) {
                ExecutingDots()
            } else {
                // 状态图标
                val icon = when (status) {
                    ToolStatus.PENDING -> Icons.Outlined.Schedule
                    ToolStatus.SUCCESS -> Icons.Default.Check
                    ToolStatus.FAILED -> Icons.Default.Close
                    else -> null
                }
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = color
                    )
                }
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/**
 * 执行中动画点
 */
@Composable
private fun ExecutingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "executingDots")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 150
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 450,
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

/**
 * 工具调用参数显示
 */
@Composable
private fun ToolCallParameters(parameters: Map<String, Any?>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "参数",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                parameters.forEach { (key, value) ->
                    ParameterRow(key = key, value = value)
                }
            }
        }
    }
}

/**
 * 参数行
 */
@Composable
private fun ParameterRow(key: String, value: Any?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 60.dp)
        )
        Text(
            text = formatParameterValue(value),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 工具调用结果显示
 */
@Composable
private fun ToolCallResult(toolCall: ToolCallInfo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (toolCall.status == ToolStatus.SUCCESS) "结果" else "Error",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (toolCall.status == ToolStatus.SUCCESS) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            }
        )
        
        val backgroundColor = if (toolCall.status == ToolStatus.SUCCESS) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        }
        
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = backgroundColor
        ) {
            val content = if (toolCall.status == ToolStatus.SUCCESS) {
                formatResultValue(toolCall.result)
            } else {
                toolCall.error ?: "未知错误"
            }
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (toolCall.status == ToolStatus.SUCCESS) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(8.dp),
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * 获取工具调用颜色
 */
@Composable
private fun getToolCallColors(status: ToolStatus): Pair<Color, Color> {
    return when (status) {
        ToolStatus.PENDING -> Pair(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
        ToolStatus.EXECUTING -> Pair(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        )
        ToolStatus.SUCCESS -> Pair(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
        ToolStatus.FAILED -> Pair(
            MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        )
    }
}

/**
 * 获取工具显示名称
 */
fun getToolDisplayName(toolName: String): String {
    return when (toolName.lowercase()) {
        "syntax_check", "syntaxcheck" -> "语法检查"
        "security_scan", "securityscan" -> "安全扫描"
        "code_format", "codeformat" -> "代码格式化"
        "code_fix", "codefix" -> "代码修复"
        "module_save", "modulesave" -> "保存模块"
        "module_test", "moduletest" -> "测试模块"
        "web_search", "websearch" -> "网络搜索"
        "read_file", "readfile" -> "读取文件"
        "write_file", "writefile" -> "写入文件"
        else -> toolName.replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}

/**
 * 格式化执行时间
 */
fun formatExecutionTime(timeMs: Long): String {
    return when {
        timeMs < 1000 -> "${timeMs}ms"
        timeMs < 60000 -> String.format("%.1fs", timeMs / 1000.0)
        else -> String.format("%.1fm", timeMs / 60000.0)
    }
}

/**
 * 格式化参数值
 */
private fun formatParameterValue(value: Any?): String {
    return when (value) {
        null -> "null"
        is String -> if (value.length > 100) "${value.take(100)}..." else value
        is List<*> -> "[${value.size} items]"
        is Map<*, *> -> "{${value.size} entries}"
        else -> value.toString()
    }
}

/**
 * 格式化结果值
 */
private fun formatResultValue(result: Any?): String {
    return when (result) {
        null -> "无返回值"
        is String -> if (result.length > 500) "${result.take(500)}..." else result
        is Boolean -> if (result) "✓ 通过" else "✗ 未通过"
        is Map<*, *> -> {
            val map = result as Map<String, Any?>
            map.entries.joinToString("\n") { (k, v) -> "$k: $v" }
        }
        is List<*> -> result.joinToString("\n")
        else -> result.toString()
    }
}

/**
 * 获取结果摘要
 */
private fun getResultSummary(toolCall: ToolCallInfo): String {
    return when {
        toolCall.result is Boolean -> if (toolCall.result) "✓ 检查通过" else "✗ 检查未通过"
        toolCall.result is String -> {
            val str = toolCall.result as String
            if (str.length > 50) "${str.take(50)}..." else str
        }
        toolCall.result is Map<*, *> -> {
            val map = toolCall.result as Map<*, *>
            "${map.size} 项结果"
        }
        else -> "执行完成 (${formatExecutionTime(toolCall.executionTimeMs)})"
    }
}

/**
 * 简化版工具调用卡片
 * 用于在消息列表中显示工具调用的简洁版本
 */
@Composable
fun CompactToolCallCard(
    toolCall: ToolCallInfo,
    modifier: Modifier = Modifier
) {
    val (_, backgroundColor) = getToolCallColors(toolCall.status)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(toolCall.toolIcon, fontSize = 14.sp)
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getToolDisplayName(toolCall.toolName),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            
            if (toolCall.status == ToolStatus.SUCCESS || toolCall.status == ToolStatus.FAILED) {
                Text(
                    text = getResultSummary(toolCall),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        ToolStatusBadge(status = toolCall.status)
    }
}

/**
 * 工具调用列表
 * 用于显示多个相关的工具调用（如语法检查后的修复）
 */
@Composable
fun ToolCallGroup(
    toolCalls: List<ToolCallInfo>,
    modifier: Modifier = Modifier
) {
    if (toolCalls.isEmpty()) return
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        toolCalls.forEachIndexed { index, toolCall ->
            var isExpanded by remember { mutableStateOf(false) }
            
            ToolCallCard(
                toolCall = toolCall,
                isExpanded = isExpanded,
                onExpandToggle = { isExpanded = !isExpanded }
            )
            
            // 连接线（除了最后一个）
            if (index < toolCalls.size - 1) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .width(2.dp)
                        .height(8.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
