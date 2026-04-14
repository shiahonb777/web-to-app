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
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * Base color
 */
data class PresetColor(
    val hex: String,
    val name: String
)

val baseColors = listOf(
    PresetColor("#F44336", AppStringsProvider.current().colorRed),
    PresetColor("#E91E63", AppStringsProvider.current().colorPink),
    PresetColor("#9C27B0", AppStringsProvider.current().colorPurple),
    PresetColor("#673AB7", AppStringsProvider.current().colorDeepPurple),
    PresetColor("#3F51B5", AppStringsProvider.current().colorIndigo),
    PresetColor("#2196F3", AppStringsProvider.current().colorBlue),
    PresetColor("#03A9F4", AppStringsProvider.current().colorLightBlue),
    PresetColor("#00BCD4", AppStringsProvider.current().colorCyan),
    PresetColor("#009688", AppStringsProvider.current().colorTeal),
    PresetColor("#4CAF50", AppStringsProvider.current().colorGreen),
    PresetColor("#8BC34A", AppStringsProvider.current().colorLightGreen),
    PresetColor("#CDDC39", AppStringsProvider.current().colorLime),
    PresetColor("#FFEB3B", AppStringsProvider.current().colorYellow),
    PresetColor("#FFC107", AppStringsProvider.current().colorAmber),
    PresetColor("#FF9800", AppStringsProvider.current().colorOrange),
    PresetColor("#FF5722", AppStringsProvider.current().colorDeepOrange),
    PresetColor("#795548", AppStringsProvider.current().colorBrown),
    PresetColor("#9E9E9E", AppStringsProvider.current().colorGrey),
    PresetColor("#607D8B", AppStringsProvider.current().colorBlueGrey),
    PresetColor("#000000", AppStringsProvider.current().colorBlack),
    PresetColor("#FFFFFF", AppStringsProvider.current().colorWhite),
    PresetColor("#1C1B1F", AppStringsProvider.current().colorDarkTheme),
    PresetColor("#FFFBFE", AppStringsProvider.current().colorLightTheme),
    PresetColor("#00000000", AppStringsProvider.current().colorTransparent)
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
        title = { Text(AppStringsProvider.current().selectColor) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // current colorpreview
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
                            text = AppStringsProvider.current().currentSelection,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedColor.uppercase(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                HorizontalDivider()
                
                // color
                Text(
                    text = AppStringsProvider.current().presetColors,
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
                
                HorizontalDivider()
                
                // Customcolorinput
                Text(
                    text = AppStringsProvider.current().customColor,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = customColorInput,
                    onValueChange = { input ->
                        // input
                        val filtered = input.filter { it in "0123456789ABCDEFabcdef" }.take(8)
                        customColorInput = filtered
                        // If it is color, update color
                        if (filtered.length == 6 || filtered.length == 8) {
                            selectedColor = "#$filtered"
                        }
                    },
                    label = { Text(AppStringsProvider.current().hexColor) },
                    placeholder = { Text(AppStringsProvider.current().hexColorHint) },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    supportingText = {
                        Text(AppStringsProvider.current().hexColorFormat)
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
                Text(AppStringsProvider.current().btnOk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().btnCancel)
            }
        }
    )
}

/**
 * color
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
                contentDescription = AppStringsProvider.current().colorSelected,
                modifier = Modifier.size(20.dp),
                tint = if (isColorLight(parsedColor)) Color.Black else Color.White
            )
        }
    }
}

/**
 * color Color
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
 * color
 */
fun isColorLight(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5
}
