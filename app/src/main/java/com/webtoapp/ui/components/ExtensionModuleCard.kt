package com.webtoapp.ui.components
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.extension.*
import com.webtoapp.core.i18n.Strings
import java.io.ByteArrayOutputStream
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

// 预设图标数据（id 与 ExtensionPanelScript.ICON_SVG_MAP 的 key 对应）
private data class FabPresetIcon(val id: String, val icon: ImageVector)

// 方案图标选择器的预设 emoji
private val PRESET_ICONS = listOf(
    "package", "book", "shield", "movie", "wrench", "dark_mode", "bolt", "target",
    "fire", "diamond", "rocket", "palette", "globe", "lock", "lightbulb", "music_note",
    "camera", "extension", "star", "heart", "rainbow", "eco", "gaming", "antenna"
)

private val PRESET_FAB_ICONS = listOf(
    FabPresetIcon("star", Icons.Filled.Star),
    FabPresetIcon("bolt", Icons.Filled.FlashOn),
    FabPresetIcon("shield", Icons.Filled.Shield),
    FabPresetIcon("heart", Icons.Filled.Favorite),
    FabPresetIcon("play", Icons.Filled.PlayArrow),
    FabPresetIcon("gear", Icons.Filled.Settings),
    FabPresetIcon("globe", Icons.Filled.Language),
    FabPresetIcon("lock", Icons.Filled.Lock),
    FabPresetIcon("moon", Icons.Filled.DarkMode),
    FabPresetIcon("palette", Icons.Filled.Palette),
    FabPresetIcon("bulb", Icons.Filled.Lightbulb),
    FabPresetIcon("fire", Icons.Filled.LocalFireDepartment),
    FabPresetIcon("diamond", Icons.Filled.Diamond),
    FabPresetIcon("rocket", Icons.Filled.RocketLaunch),
    FabPresetIcon("eye", Icons.Filled.Visibility),
    FabPresetIcon("pin", Icons.Filled.LocationOn),
    FabPresetIcon("code", Icons.Filled.Code),
    FabPresetIcon("music", Icons.Filled.MusicNote),
    FabPresetIcon("camera", Icons.Filled.CameraAlt),
    FabPresetIcon("puzzle", Icons.Filled.Extension),
)

/**
 * 扩展模块设置卡片
 * 
 * 类似于激活码、公告、广告拦截等功能卡片的设计风格
 * 支持开关控制和模块选择
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionModuleCard(
    enabled: Boolean,
    selectedModuleIds: Set<String>,
    extensionFabIcon: String,
    onEnabledChange: (Boolean) -> Unit,
    onModuleIdsChange: (Set<String>) -> Unit,
    onFabIconChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    val userModules by extensionManager.modules.collectAsStateWithLifecycle()
    val builtInModules by extensionManager.builtInModules.collectAsStateWithLifecycle()
    
    // 所有可用模块（不再过滤全局启用状态，因为模块的启用现在由每个应用配置控制）
    val allModules = builtInModules + userModules
    val availableModules = allModules // 所有模块都可以被选择
    
    // 已选择的模块
    val selectedModules = allModules.filter { it.id in selectedModuleIds }
    
    var showModuleSelector by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    
    // 方案管理器
    val presetManager = remember { ModulePresetManager.getInstance(context) }
    var showPresetSelector by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    
    EnhancedElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Extension,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.extensionModule,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (enabled && selectedModuleIds.isNotEmpty()) {
                            Text(
                                text = Strings.selectedCountFormat.format(selectedModuleIds.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                PremiumSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            // Expand内容
            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = Strings.addCustomFeatures,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 快捷方案选择
                    PresetQuickSelect(
                        presetManager = presetManager,
                        selectedModuleIds = selectedModuleIds,
                        onApplyPreset = { preset ->
                            onModuleIdsChange(preset.moduleIds.toSet())
                            Toast.makeText(context, "${Strings.appliedPreset}: ${preset.name}", Toast.LENGTH_SHORT).show()
                        },
                        onShowAllPresets = { showPresetSelector = true }
                    )
                    
                    HorizontalDivider()
                    
                    // 快速选择常用模块
                    if (availableModules.isNotEmpty()) {
                        Text(
                            text = Strings.quickSelect,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        QuickModuleChips(
                            allModules = availableModules.take(6),
                            selectedIds = selectedModuleIds,
                            onToggle = { moduleId ->
                                onModuleIdsChange(
                                    if (moduleId in selectedModuleIds) {
                                        selectedModuleIds - moduleId
                                    } else {
                                        selectedModuleIds + moduleId
                                    }
                                )
                            }
                        )
                    }
                    
                    // 已选模块列表
                    if (selectedModules.isNotEmpty()) {
                        HorizontalDivider()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Strings.selectedModulesCount.format(selectedModules.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                // Save为方案
                                TextButton(
                                    onClick = { showSavePresetDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.Save, null, Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Strings.saveAsScheme, style = MaterialTheme.typography.labelSmall)
                                }
                                // 清空按钮
                                TextButton(
                                    onClick = { onModuleIdsChange(emptySet()) },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(Strings.clearAll, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        
                        selectedModules.forEach { module ->
                            SelectedModuleItem(
                                module = module,
                                onRemove = {
                                    onModuleIdsChange(selectedModuleIds - module.id)
                                }
                            )
                        }
                    }
                    
                    // 自定义悬浮按钮图标
                    HorizontalDivider()
                    FabIconSelector(
                        selectedIcon = extensionFabIcon,
                        onIconChange = onFabIconChange
                    )
                    
                    // 操作按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 浏览全部模块按钮
                        PremiumOutlinedButton(
                            onClick = { showModuleSelector = true },
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        ) {
                            Icon(
                                Icons.Outlined.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Strings.selectModules)
                        }
                        
                        // 方案管理按钮
                        PremiumOutlinedButton(
                            onClick = { showPresetSelector = true }
                        ) {
                            Icon(
                                Icons.Outlined.Bookmarks,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        // 测试按钮
                        if (selectedModuleIds.isNotEmpty()) {
                            PremiumOutlinedButton(
                                onClick = { showTestDialog = true }
                            ) {
                                Icon(
                                    Icons.Outlined.Science,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 方案选择对话框
    if (showPresetSelector) {
        PresetSelectorDialog(
            presetManager = presetManager,
            extensionManager = extensionManager,
            currentSelection = selectedModuleIds,
            onApplyPreset = { preset ->
                onModuleIdsChange(preset.moduleIds.toSet())
                showPresetSelector = false
            },
            onDismiss = { showPresetSelector = false }
        )
    }
    
    // Save方案对话框
    if (showSavePresetDialog) {
        SavePresetDialog(
            moduleIds = selectedModuleIds,
            presetManager = presetManager,
            onSaved = { 
                showSavePresetDialog = false
                Toast.makeText(context, Strings.presetSaved, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSavePresetDialog = false }
        )
    }
    
    // Module选择对话框
    if (showModuleSelector) {
        ExtensionModuleSelectorDialog(
            allModules = allModules,
            selectedIds = selectedModuleIds,
            onSelectionChange = onModuleIdsChange,
            onDismiss = { showModuleSelector = false }
        )
    }
    
    // 测试对话框
    if (showTestDialog) {
        ModuleTestDialog(
            selectedModules = selectedModules,
            onDismiss = { showTestDialog = false }
        )
    }
}

/**
 * 快速选择模块芯片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickModuleChips(
    allModules: List<ExtensionModule>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allModules, key = { it.id }) { module ->
            val isSelected = module.id in selectedIds
            PremiumFilterChip(
                selected = isSelected,
                onClick = { onToggle(module.id) },
                label = { 
                    Text(
                        module.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                } else {
                    { ModuleIcon(iconId = module.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface) }
                }
            )
        }
    }
}

/**
 * 已选模块项
 * 
 * 紧凑两行布局：
 *   行1: [图标] 模块名称（溢出省略）  [删除按钮]
 *   行2: 标签行（内置 · UI类型 · 运行模式 · 分类）— 使用 FlowRow 自动换行
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedModuleItem(
    module: ExtensionModule,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            ModuleIcon(
                iconId = module.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            
            // 内容列 — 名称 + 标签
            Column(modifier = Modifier.weight(1f)) {
                // 行1: 名称
                Text(
                    module.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                // 行2: 标签 — FlowRow 自动换行，避免横向溢出
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // 内置标签
                    if (module.builtIn) {
                        CompactBadge(
                            text = Strings.builtIn,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    // UI 类型（仅非默认时显示）
                    if (module.uiConfig.type != ModuleUiType.FLOATING_BUTTON) {
                        CompactBadge(
                            text = module.uiConfig.type.getDisplayName(),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    // 运行模式
                    CompactBadge(
                        text = module.runMode.getDisplayName(),
                        containerColor = if (module.runMode == ModuleRunMode.AUTO)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        contentColor = if (module.runMode == ModuleRunMode.AUTO)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    // 分类
                    CompactBadge(
                        text = module.category.getDisplayName(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 删除按钮
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = Strings.removeModule,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 紧凑标签 — 极小圆角胶囊
 */
@Composable
private fun CompactBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}

/**
 * 悬浮按钮图标选择器
 * 支持预设 SVG 图标 + 相册自定义图片（带裁剪预览）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FabIconSelector(
    selectedIcon: String,
    onIconChange: (String) -> Unit
) {
    val context = LocalContext.current
    
    // 图片预览确认对话框状态
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingBase64 by remember { mutableStateOf<String?>(null) }
    
    // 相册图片选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            // 缩放到 96x96 — 高清分辨率，确保 2x/3x 屏幕也清晰
            val size = minOf(original.width, original.height)
            val xOffset = (original.width - size) / 2
            val yOffset = (original.height - size) / 2
            val cropped = Bitmap.createBitmap(original, xOffset, yOffset, size, size)
            val scaled = Bitmap.createScaledBitmap(cropped, 96, 96, true)
            if (cropped !== original) original.recycle()
            if (scaled !== cropped) cropped.recycle()
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            // 显示预览确认对话框
            pendingBitmap = scaled
            pendingBase64 = base64
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 解码已保存的自定义图片（用于选择器预览）
    val customBitmap = remember(selectedIcon) {
        if (selectedIcon.startsWith("custom:")) {
            try {
                val bytes = Base64.decode(selectedIcon.removePrefix("custom:"), Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) { null }
        } else null
    }
    
    val isCustomSelected = selectedIcon.startsWith("custom:")
    
    // ======== 预览确认对话框 ========
    if (pendingBitmap != null && pendingBase64 != null) {
        AlertDialog(
            onDismissRequest = {
                pendingBitmap?.recycle()
                pendingBitmap = null
                pendingBase64 = null
            },
            icon = {
                // 圆形预览 — 模拟实际 FAB 效果
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pendingBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            },
            title = {
                Text(
                    Strings.fabIconPreviewTitle,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    Strings.fabIconPreviewDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onIconChange("custom:${pendingBase64!!}")
                    pendingBitmap = null
                    pendingBase64 = null
                }) {
                    Text(Strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // 重新选择
                    pendingBitmap?.recycle()
                    pendingBitmap = null
                    pendingBase64 = null
                    galleryLauncher.launch("image/*")
                }) {
                    Text(Strings.reselect)
                }
            }
        )
    }
    
    Text(
        text = Strings.extensionFabIconLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Spacer(modifier = Modifier.height(6.dp))
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ---- 自定义图片按钮 ----
        item {
            Surface(
                onClick = { galleryLauncher.launch("image/*") },
                shape = RoundedCornerShape(10.dp),
                color = if (isCustomSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                },
                border = if (isCustomSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customBitmap != null) {
                            Image(
                                bitmap = customBitmap.asImageBitmap(),
                                contentDescription = Strings.fabIconFromGallery,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = Strings.fabIconFromGallery,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (isCustomSelected) Strings.fabIconChangeImage else Strings.fabIconCustom,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // ---- 分隔线 ----
        item {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
        }
        
        // ---- 预设图标列表 ----
        items(PRESET_FAB_ICONS) { preset ->
            val isSelected = selectedIcon == preset.id
            Surface(
                onClick = { onIconChange(if (isSelected) "" else preset.id) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                },
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else null
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        preset.icon,
                        contentDescription = preset.id,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
    
    // 已选状态提示
    if (selectedIcon.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isCustomSelected) Strings.fabIconSelected
                       else PRESET_FAB_ICONS.find { it.id == selectedIcon }?.id ?: selectedIcon,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            // 清除按钮
            Surface(
                onClick = { onIconChange("") },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 扩展模块选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionModuleSelectorDialog(
    allModules: List<ExtensionModule>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    
    // 直接计算过滤后的模块列表，确保列表更新时 UI 正确响应
    val filteredModules = allModules.filter { module ->
        val matchesSearch = searchQuery.isBlank() ||
            module.name.contains(searchQuery, ignoreCase = true) ||
            module.description.contains(searchQuery, ignoreCase = true) ||
            module.tags.any { it.contains(searchQuery, ignoreCase = true) }
        val matchesCategory = selectedCategory == null || module.category == selectedCategory
        matchesSearch && matchesCategory
    }
    
    // 按分类分组
    val groupedModules = filteredModules.groupBy { it.category }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // 标题栏
                TopAppBar(
                    title = { Text(Strings.selectExtensionModules) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, Strings.btnCancel)
                        }
                    },
                    actions = {
                        TextButton(onClick = onDismiss) {
                            Text(Strings.doneWithCount.format(selectedIds.size))
                        }
                    }
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Search框
                    PremiumTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(Strings.searchModulesHint) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, Strings.clear)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 分类筛选
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            PremiumFilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text(Strings.all) },
                                leadingIcon = if (selectedCategory == null) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                        items(ModuleCategory.values().toList()) { category ->
                            PremiumFilterChip(
                                selected = selectedCategory == category,
                                onClick = { 
                                    selectedCategory = if (selectedCategory == category) null else category
                                },
                                label = { Text(category.getDisplayName()) },
                                leadingIcon = if (selectedCategory == category) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Module列表
                    LazyColumn(
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedCategory == null && searchQuery.isBlank()) {
                            // 按分类显示
                            groupedModules.forEach { (category, modules) ->
                                item {
                                    Text(
                                        category.getDisplayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(modules, key = { it.id }) { module ->
                                    ModuleSelectItem(
                                        module = module,
                                        isSelected = module.id in selectedIds,
                                        onToggle = {
                                            onSelectionChange(
                                                if (module.id in selectedIds) {
                                                    selectedIds - module.id
                                                } else {
                                                    selectedIds + module.id
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            // 平铺显示搜索/筛选结果
                            items(filteredModules, key = { it.id }) { module ->
                                ModuleSelectItem(
                                    module = module,
                                    isSelected = module.id in selectedIds,
                                    onToggle = {
                                        onSelectionChange(
                                            if (module.id in selectedIds) {
                                                selectedIds - module.id
                                            } else {
                                                selectedIds + module.id
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        
                        if (filteredModules.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.SearchOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            Strings.noMatchingModules,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

/**
 * 模块选择项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleSelectItem(
    module: ExtensionModule,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ModuleIcon(
                        iconId = module.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                // 名称（溢出省略）
                Text(
                    module.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                // 标签行 — FlowRow 自动换行
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (module.builtIn) {
                        CompactBadge(
                            text = Strings.builtIn,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    CompactBadge(
                        text = module.runMode.getDisplayName(),
                        containerColor = if (module.runMode == ModuleRunMode.AUTO)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        contentColor = if (module.runMode == ModuleRunMode.AUTO)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                if (module.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        module.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 标签
                if (module.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        module.tags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    tag,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // 选中状态
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

/**
 * 模块测试对话框
 * 提供测试页面让用户预览模块效果
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleTestDialog(
    selectedModules: List<ExtensionModule>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val testPages = remember { DebugTestPages.getAll() }
    var selectedTestPage by remember { mutableStateOf(testPages.firstOrNull()) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // 标题栏
                TopAppBar(
                    title = { Text(Strings.testModule) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, Strings.close)
                        }
                    }
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 已选模块信息
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                Text(
                                    Strings.willTestModulesFormat.format(selectedModules.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    selectedModules.joinToString { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    // 测试页面选择
                    Text(
                        Strings.selectTestPageTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(testPages) { page ->
                            PremiumFilterChip(
                                selected = selectedTestPage?.id == page.id,
                                onClick = { selectedTestPage = page },
                                label = { Text("${page.icon} ${page.name}") },
                                leadingIcon = if (selectedTestPage?.id == page.id) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                    
                    // 测试页面描述
                    selectedTestPage?.let { page ->
                        Text(
                            page.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                    
                    // Start测试按钮
                    PremiumButton(
                        onClick = {
                            selectedTestPage?.let { page ->
                                // Start测试 WebView
                                val intent = Intent(context, com.webtoapp.ui.webview.WebViewActivity::class.java).apply {
                                    putExtra("test_url", page.toDataUrl())
                                    putStringArrayListExtra("test_module_ids", ArrayList(selectedModules.map { it.id }))
                                }
                                context.startActivity(intent)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedTestPage != null
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.startTestBtn)
                    }
                    
                    // 提示
                    Text(
                        Strings.testPageHintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 模块详情预览对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailDialog(
    module: ExtensionModule,
    onDismiss: () -> Unit,
    onSelect: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // 标题栏
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ModuleIcon(iconId = module.icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(module.name)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, Strings.close)
                        }
                    }
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 基本信息
                    if (module.description.isNotBlank()) {
                        Text(
                            module.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // 元信息
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoChip(module.category.getDisplayName())
                        InfoChip("v${module.version.name}")
                        if (module.builtIn) {
                            InfoChip(Strings.builtInModule)
                        }
                    }
                    
                    // 标签
                    if (module.tags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(module.tags) { tag ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "#$tag",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Configure项
                    if (module.configItems.isNotEmpty()) {
                        Text(
                            Strings.configurableItems.format(module.configItems.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        module.configItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.name, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    item.type.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Permission
                    if (module.permissions.isNotEmpty()) {
                        Text(
                            Strings.requiredPermissions,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(module.permissions) { perm ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (perm.dangerous) 
                                        MaterialTheme.colorScheme.errorContainer 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        perm.displayName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (perm.dangerous)
                                            MaterialTheme.colorScheme.onErrorContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                    
                    // Select按钮
                    PremiumButton(
                        onClick = {
                            onSelect()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.addThisModule)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * 快捷方案选择
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetQuickSelect(
    presetManager: ModulePresetManager,
    selectedModuleIds: Set<String>,
    onApplyPreset: (ModulePreset) -> Unit,
    onShowAllPresets: () -> Unit
) {
    val presets = remember { presetManager.getBuiltInPresets().take(4) }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.quickSchemes,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onShowAllPresets,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(Strings.allSchemesBtn, style = MaterialTheme.typography.labelSmall)
                Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp))
            }
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets) { preset ->
                val isApplied = preset.moduleIds.toSet() == selectedModuleIds
                PremiumFilterChip(
                    selected = isApplied,
                    onClick = { onApplyPreset(preset) },
                    label = { Text("${preset.icon} ${preset.name}") },
                    leadingIcon = if (isApplied) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

/**
 * 方案选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSelectorDialog(
    presetManager: ModulePresetManager,
    extensionManager: ExtensionManager,
    currentSelection: Set<String>,
    onApplyPreset: (ModulePreset) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allPresets = remember { presetManager.getAllPresets() }
    val builtInPresets = allPresets.filter { it.builtIn }
    val userPresets = allPresets.filter { !it.builtIn }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                TopAppBar(
                    title = { Text(Strings.moduleSchemes) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, Strings.close)
                        }
                    }
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Built-in方案
                    item {
                        Text(
                            Strings.builtInSchemes,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(builtInPresets) { preset ->
                        PresetItem(
                            preset = preset,
                            extensionManager = extensionManager,
                            isApplied = preset.moduleIds.toSet() == currentSelection,
                            onApply = { onApplyPreset(preset) },
                            onDelete = null
                        )
                    }
                    
                    // User方案
                    if (userPresets.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                Strings.mySchemes,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(userPresets) { preset ->
                            PresetItem(
                                preset = preset,
                                extensionManager = extensionManager,
                                isApplied = preset.moduleIds.toSet() == currentSelection,
                                onApply = { onApplyPreset(preset) },
                                onDelete = {
                                    presetManager.deletePreset(preset.id)
                                    Toast.makeText(context, Strings.deleted, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            Strings.schemeTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 方案项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetItem(
    preset: ModulePreset,
    extensionManager: ExtensionManager,
    isApplied: Boolean,
    onApply: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val modules = remember(preset.moduleIds) {
        extensionManager.getModulesByIds(preset.moduleIds)
    }
    
    Surface(
        onClick = onApply,
        shape = RoundedCornerShape(12.dp),
        color = if (isApplied) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        border = if (isApplied) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    preset.icon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        preset.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (preset.builtIn) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                Strings.builtIn,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                if (preset.description.isNotBlank()) {
                    Text(
                        preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 包含的模块
                Text(
                    "${Strings.containsModules.format(modules.size)}: ${modules.joinToString { it.name }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 操作
            if (isApplied) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = Strings.applied,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = Strings.btnDelete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 保存方案对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePresetDialog(
    moduleIds: Set<String>,
    presetManager: ModulePresetManager,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("package") }
    var showIconPicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.saveAsSchemeTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Icon选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { showIconPicker = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            selectedIcon,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    PremiumTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.schemeNameLabel) },
                        placeholder = { Text(Strings.enterSchemeNameHint) },
                        singleLine = true,
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                }
                
                PremiumTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Strings.descriptionOptionalLabel) },
                    placeholder = { Text(Strings.briefDescribeSchemeHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    Strings.willSaveModules.format(moduleIds.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Icon选择器
                if (showIconPicker) {
                    HorizontalDivider()
                    Text(Strings.selectIconTitle, style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PRESET_ICONS) { icon ->
                            Surface(
                                onClick = {
                                    selectedIcon = icon
                                    showIconPicker = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (icon == selectedIcon) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Icon(
                                    com.webtoapp.util.SvgIconMapper.getIcon(icon),
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp).size(24.dp),
                                    tint = if (icon == selectedIcon)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    if (name.isNotBlank()) {
                        presetManager.createPresetFromSelection(
                            name = name,
                            description = description,
                            icon = selectedIcon,
                            moduleIds = moduleIds
                        )
                        onSaved()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(Strings.btnSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
