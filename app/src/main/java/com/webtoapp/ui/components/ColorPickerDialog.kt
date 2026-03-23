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
import com.webtoapp.core.i18n.Strings

/**
 * 预设的 Base 颜色
 */
data class PresetColor(
    val hex: String,
    val name: String
)

val baseColors = listOf(
    PresetColor("#F44336", Strings.colorRed),
    PresetColor("#E91E63", Strings.colorPink),
    PresetColor("#9C27B0", Strings.colorPurple),
    PresetColor("#673AB7", Strings.colorDeepPurple),
    PresetColor("#3F51B5", Strings.colorIndigo),
    PresetColor("#2196F3", Strings.colorBlue),
    PresetColor("#03A9F4", Strings.colorLightBlue),
    PresetColor("#00BCD4", Strings.colorCyan),
    PresetColor("#009688", Strings.colorTeal),
    PresetColor("#4CAF50", Strings.colorGreen),
    PresetColor("#8BC34A", Strings.colorLightGreen),
    PresetColor("#CDDC39", Strings.colorLime),
    PresetColor("#FFEB3B", Strings.colorYellow),
    PresetColor("#FFC107", Strings.colorAmber),
    PresetColor("#FF9800", Strings.colorOrange),
    PresetColor("#FF5722", Strings.colorDeepOrange),
    PresetColor("#795548", Strings.colorBrown),
    PresetColor("#9E9E9E", Strings.colorGrey),
    PresetColor("#607D8B", Strings.colorBlueGrey),
    PresetColor("#000000", Strings.colorBlack),
    PresetColor("#FFFFFF", Strings.colorWhite),
    PresetColor("#1C1B1F", Strings.colorDarkTheme),
    PresetColor("#FFFBFE", Strings.colorLightTheme),
    PresetColor("#00000000", Strings.colorTransparent)
)

/**
 * Color picker dialog
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
        title = { Text(Strings.selectColor) },
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
                            text = Strings.currentSelection,
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
                    text = Strings.presetColors,
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
                
                // Custom颜色输入
                Text(
                    text = Strings.customColor,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = customColorInput,
                    onValueChange = { input ->
                        // 只允许输入有效的十六进制字符
                        val filtered = input.filter { it in "0123456789ABCDEFabcdef" }.take(8)
                        customColorInput = filtered
                        // If it is有效的颜色值，更新选中颜色
                        if (filtered.length == 6 || filtered.length == 8) {
                            selectedColor = "#$filtered"
                        }
                    },
                    label = { Text(Strings.hexColor) },
                    placeholder = { Text(Strings.hexColorHint) },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    supportingText = {
                        Text(Strings.hexColorFormat)
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
                Text(Strings.btnOk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
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
                contentDescription = Strings.colorSelected,
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
