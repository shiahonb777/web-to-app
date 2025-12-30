package com.webtoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * 预设的 Base 颜色
 */
data class PresetColor(
    val hex: String,
    val name: String
)

val baseColors = listOf(
    PresetColor("#F44336", "红色"),
    PresetColor("#E91E63", "粉色"),
    PresetColor("#9C27B0", "紫色"),
    PresetColor("#673AB7", "深紫"),
    PresetColor("#3F51B5", "靛蓝"),
    PresetColor("#2196F3", "蓝色"),
    PresetColor("#03A9F4", "浅蓝"),
    PresetColor("#00BCD4", "青色"),
    PresetColor("#009688", "蓝绿"),
    PresetColor("#4CAF50", "绿色"),
    PresetColor("#8BC34A", "浅绿"),
    PresetColor("#CDDC39", "黄绿"),
    PresetColor("#FFEB3B", "黄色"),
    PresetColor("#FFC107", "琥珀"),
    PresetColor("#FF9800", "橙色"),
    PresetColor("#FF5722", "深橙"),
    PresetColor("#795548", "棕色"),
    PresetColor("#9E9E9E", "灰色"),
    PresetColor("#607D8B", "蓝灰"),
    PresetColor("#000000", "黑色"),
    PresetColor("#FFFFFF", "白色"),
    PresetColor("#1C1B1F", "深色主题"),
    PresetColor("#FFFBFE", "浅色主题"),
    PresetColor("#00000000", "透明")
)

/**
 * 颜色选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    currentColor: String?,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customColorInput by remember { mutableStateOf(currentColor?.removePrefix("#") ?: "") }
    var selectedColor by remember { mutableStateOf(currentColor ?: "#2196F3") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 当前选中颜色预览
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(parseColor(selectedColor))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = "当前选择",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedColor.uppercase(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Divider()
                
                // 预设颜色网格
                Text(
                    text = "预设颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(baseColors) { presetColor ->
                        ColorItem(
                            color = presetColor.hex,
                            isSelected = selectedColor.equals(presetColor.hex, ignoreCase = true),
                            onClick = {
                                selectedColor = presetColor.hex
                                customColorInput = presetColor.hex.removePrefix("#")
                            }
                        )
                    }
                }
                
                Divider()
                
                // 自定义颜色输入
                Text(
                    text = "自定义颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = customColorInput,
                    onValueChange = { input ->
                        // 只允许输入有效的十六进制字符
                        val filtered = input.filter { it in "0123456789ABCDEFabcdef" }.take(8)
                        customColorInput = filtered
                        // 如果是有效的颜色值，更新选中颜色
                        if (filtered.length == 6 || filtered.length == 8) {
                            selectedColor = "#$filtered"
                        }
                    },
                    label = { Text("十六进制颜色") },
                    placeholder = { Text("如: FF5722 或 80FF5722") },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    supportingText = {
                        Text("6位(RGB)或8位(ARGB)十六进制")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(selectedColor)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 单个颜色项
 */
@Composable
private fun ColorItem(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val parsedColor = parseColor(color)
    val isTransparent = color.equals("#00000000", ignoreCase = true)
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (isTransparent) {
                    Modifier.background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color.White, Color.LightGray)
                        )
                    )
                } else {
                    Modifier.background(parsedColor)
                }
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选择",
                modifier = Modifier.size(20.dp),
                tint = if (isColorLight(parsedColor)) Color.Black else Color.White
            )
        }
    }
}

/**
 * 解析颜色字符串为 Color
 */
fun parseColor(colorString: String): Color {
    return try {
        val hex = colorString.removePrefix("#")
        when (hex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$hex"))
            8 -> Color(android.graphics.Color.parseColor("#$hex"))
            else -> Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}

/**
 * 判断颜色是否为浅色
 */
fun isColorLight(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5
}
