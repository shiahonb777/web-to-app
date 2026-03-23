package com.webtoapp.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.StatusBarBackgroundType
import com.webtoapp.data.model.StatusBarColorMode
import com.webtoapp.data.model.WebViewConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusBarConfigCard(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val systemStatusBarHeight = remember {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            with(density) { context.resources.getDimensionPixelSize(resourceId).toDp().value.toInt() }
        } else 24
    }

    val currentHeightDp = if (config.statusBarHeightDp > 0) config.statusBarHeightDp else systemStatusBarHeight
    
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropper by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingImageUri = it
            showCropper = true
        }
    }
    
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = config.statusBarColor,
            onColorSelected = { color ->
                onConfigChange(config.copy(
                    statusBarColorMode = StatusBarColorMode.CUSTOM,
                    statusBarBackgroundType = StatusBarBackgroundType.COLOR,
                    statusBarColor = color
                ))
            },
            onDismiss = { showColorPicker = false }
        )
    }
    
    if (showCropper && pendingImageUri != null) {
        StatusBarImageCropper(
            imageUri = pendingImageUri!!,
            statusBarHeightDp = currentHeightDp,
            onCropComplete = { croppedPath ->
                onConfigChange(config.copy(
                    statusBarBackgroundType = StatusBarBackgroundType.IMAGE,
                    statusBarBackgroundImage = croppedPath
                ))
                showCropper = false
                pendingImageUri = null
            },
            onDismiss = {
                showCropper = false
                pendingImageUri = null
            }
        )
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusBarPreviewBox(
            heightDp = currentHeightDp,
            backgroundType = config.statusBarBackgroundType,
            backgroundColor = config.statusBarColor,
            backgroundImage = config.statusBarBackgroundImage,
            alpha = config.statusBarBackgroundAlpha
        )
        
        HeightSlider(
            currentHeight = currentHeightDp,
            systemDefaultHeight = systemStatusBarHeight,
            onHeightChange = { onConfigChange(config.copy(statusBarHeightDp = it)) }
        )
        
        Divider()
        
        Text(Strings.backgroundType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = config.statusBarBackgroundType == StatusBarBackgroundType.COLOR,
                onClick = { onConfigChange(config.copy(statusBarBackgroundType = StatusBarBackgroundType.COLOR)) },
                label = { Text(Strings.solidColor) },
                leadingIcon = if (config.statusBarBackgroundType == StatusBarBackgroundType.COLOR) {
                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                } else { { Icon(Icons.Outlined.Palette, null, Modifier.size(18.dp)) } }
            )
            FilterChip(
                selected = config.statusBarBackgroundType == StatusBarBackgroundType.IMAGE,
                onClick = { onConfigChange(config.copy(statusBarBackgroundType = StatusBarBackgroundType.IMAGE)) },
                label = { Text(Strings.image) },
                leadingIcon = if (config.statusBarBackgroundType == StatusBarBackgroundType.IMAGE) {
                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                } else { { Icon(Icons.Outlined.Image, null, Modifier.size(18.dp)) } }
            )
        }

        when (config.statusBarBackgroundType) {
            StatusBarBackgroundType.COLOR -> {
                ColorSelectionRow(currentColor = config.statusBarColor, onColorClick = { showColorPicker = true })
            }
            StatusBarBackgroundType.IMAGE -> {
                ImageSelectionRow(
                    currentImagePath = config.statusBarBackgroundImage,
                    onSelectImage = { imagePickerLauncher.launch("image/*") },
                    onClearImage = { onConfigChange(config.copy(statusBarBackgroundImage = null, statusBarBackgroundType = StatusBarBackgroundType.COLOR)) }
                )
            }
        }
        
        Divider()
        
        AlphaSlider(alpha = config.statusBarBackgroundAlpha, onAlphaChange = { onConfigChange(config.copy(statusBarBackgroundAlpha = it)) })
    }
}
@Composable
private fun StatusBarPreviewBox(
    heightDp: Int,
    backgroundType: StatusBarBackgroundType,
    backgroundColor: String?,
    backgroundImage: String?,
    alpha: Float
) {
    val bgColor = remember(backgroundColor) { backgroundColor?.let { parseColor(it) } ?: Color.Black }
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Strings.statusBarPreview, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Box(
            modifier = Modifier.fillMaxWidth().height(heightDp.dp).clip(RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
        ) {
            when (backgroundType) {
                StatusBarBackgroundType.COLOR -> {
                    Box(modifier = Modifier.fillMaxSize().background(bgColor.copy(alpha = alpha)))
                }
                StatusBarBackgroundType.IMAGE -> {
                    if (backgroundImage != null) {
                        AsyncImage(model = backgroundImage, contentDescription = null,
                            modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
                            contentScale = ContentScale.Crop)
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center) {
                            Text(Strings.noImageSelected, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("12:00", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SignalCellularAlt, null, Modifier.size(12.dp), tint = Color.White)
                    Icon(Icons.Default.Wifi, null, Modifier.size(12.dp), tint = Color.White)
                    Icon(Icons.Default.BatteryFull, null, Modifier.size(12.dp), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HeightSlider(currentHeight: Int, systemDefaultHeight: Int, onHeightChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(Strings.statusBarHeight, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${currentHeight}dp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = currentHeight.toFloat(), onValueChange = { onHeightChange(it.toInt()) }, valueRange = 16f..48f, steps = 31, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("16dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onHeightChange(systemDefaultHeight) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("${Strings.restoreDefault} (${systemDefaultHeight}dp)", fontSize = 12.sp)
            }
            Text("48dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColorSelectionRow(currentColor: String?, onColorClick: () -> Unit) {
    val color = remember(currentColor) { currentColor?.let { parseColor(it) } ?: Color.Black }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onColorClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(color).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(Strings.backgroundColor, style = MaterialTheme.typography.bodyMedium)
            Text(currentColor?.uppercase() ?: "#000000", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.Edit, contentDescription = Strings.selectColor, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ImageSelectionRow(currentImagePath: String?, onSelectImage: () -> Unit, onClearImage: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (currentImagePath != null) {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = currentImagePath, contentDescription = null, modifier = Modifier.width(80.dp).height(32.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.weight(1f)) {
                    Text(Strings.imageSelected, style = MaterialTheme.typography.bodyMedium)
                    Text(Strings.clickToChangeOrClear, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSelectImage) { Icon(Icons.Default.Edit, Strings.changeImage) }
                IconButton(onClick = onClearImage) { Icon(Icons.Default.Delete, Strings.clearImage, tint = MaterialTheme.colorScheme.error) }
            }
        } else {
            OutlinedButton(onClick = onSelectImage, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(Strings.selectBackgroundImage)
            }
        }
    }
}

@Composable
private fun AlphaSlider(alpha: Float, onAlphaChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(Strings.backgroundAlpha, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(alpha * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = alpha, onValueChange = onAlphaChange, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Strings.transparent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Strings.opaque, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
