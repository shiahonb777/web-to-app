package com.webtoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 主题设置界面
 * 展示所有主题并支持切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeManager = remember { ThemeManager.getInstance(context) }
    
    // 状态
    val currentThemeType by themeManager.themeTypeFlow.collectAsState(initial = AppThemeType.AURORA)
    val darkMode by themeManager.darkModeFlow.collectAsState(initial = ThemeManager.DarkModeSettings.SYSTEM)
    val uiMode by themeManager.uiModeFlow.collectAsState(initial = ThemeManager.UiMode.DEFAULT)
    val enableAnimations by themeManager.enableAnimationsFlow.collectAsState(initial = true)
    val enableParticles by themeManager.enableParticlesFlow.collectAsState(initial = true)
    val enableHaptics by themeManager.enableHapticsFlow.collectAsState(initial = true)
    val enableSound by themeManager.enableSoundFlow.collectAsState(initial = true)
    val animationSpeed by themeManager.animationSpeedFlow.collectAsState(initial = ThemeManager.AnimationSpeed.NORMAL)
    
    var selectedTab by remember { mutableStateOf(0) }
    var showThemePreview by remember { mutableStateOf<AppThemeType?>(null) }
    
    val currentTheme = AppThemes.getTheme(currentThemeType)
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(Strings.themeSettings)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                currentTheme.type.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiMode == ThemeManager.UiMode.ENHANCED) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        Strings.enhancedMode,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, Strings.back)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 标签栏
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(Strings.theme) },
                    icon = { Icon(Icons.Outlined.Palette, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(Strings.appearance) },
                    icon = { Icon(Icons.Outlined.Tune, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(Strings.effects) },
                    icon = { Icon(Icons.Outlined.AutoAwesome, null) }
                )
            }
            
            when (selectedTab) {
                0 -> ThemeSelectionTab(
                    currentThemeType = currentThemeType,
                    uiMode = uiMode,
                    onThemeSelect = { type ->
                        scope.launch { themeManager.setThemeType(type) }
                    },
                    onPreviewTheme = { showThemePreview = it },
                    onUiModeChange = { mode ->
                        scope.launch { themeManager.setUiMode(mode) }
                    }
                )
                1 -> AppearanceTab(
                    darkMode = darkMode,
                    onDarkModeChange = { mode ->
                        scope.launch { themeManager.setDarkMode(mode) }
                    }
                )
                2 -> AnimationTab(
                    enableAnimations = enableAnimations,
                    enableParticles = enableParticles,
                    enableHaptics = enableHaptics,
                    enableSound = enableSound,
                    animationSpeed = animationSpeed,
                    currentTheme = currentTheme,
                    uiMode = uiMode,
                    onEnableAnimationsChange = { scope.launch { themeManager.setEnableAnimations(it) } },
                    onEnableParticlesChange = { scope.launch { themeManager.setEnableParticles(it) } },
                    onEnableHapticsChange = { scope.launch { themeManager.setEnableHaptics(it) } },
                    onEnableSoundChange = { scope.launch { themeManager.setEnableSound(it) } },
                    onAnimationSpeedChange = { scope.launch { themeManager.setAnimationSpeed(it) } }
                )
            }
        }
    }
    
    // Theme预览对话框
    showThemePreview?.let { themeType ->
        ThemePreviewDialog(
            theme = AppThemes.getTheme(themeType),
            uiMode = uiMode,
            onDismiss = { showThemePreview = null },
            onApply = {
                scope.launch {
                    themeManager.setThemeType(themeType)
                    showThemePreview = null
                }
            }
        )
    }
}

/**
 * 主题选择标签页
 */
@Composable
private fun ThemeSelectionTab(
    currentThemeType: AppThemeType,
    uiMode: ThemeManager.UiMode,
    onThemeSelect: (AppThemeType) -> Unit,
    onPreviewTheme: (AppThemeType) -> Unit,
    onUiModeChange: (ThemeManager.UiMode) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // UI 模式选择卡片
        item(span = { GridItemSpan(2) }) {
            UiModeSelector(
                currentMode = uiMode,
                onModeChange = onUiModeChange
            )
        }
        
        items(AppThemes.allThemes) { theme ->
            ThemeCard(
                theme = theme,
                isSelected = theme.type == currentThemeType,
                isEnhanced = uiMode == ThemeManager.UiMode.ENHANCED,
                onClick = { onThemeSelect(theme.type) },
                onLongClick = { onPreviewTheme(theme.type) }
            )
        }
    }
}

/**
 * UI 模式选择器
 */
@Composable
private fun UiModeSelector(
    currentMode: ThemeManager.UiMode,
    onModeChange: (ThemeManager.UiMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                Strings.uiMode,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                Strings.selectUiStyleHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeManager.UiMode.entries.forEach { mode ->
                    UiModeCard(
                        mode = mode,
                        isSelected = currentMode == mode,
                        onClick = { onModeChange(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun UiModeCard(
    mode: ThemeManager.UiMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "scale"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surface,
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else 
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (mode == ThemeManager.UiMode.ENHANCED)
                            Brush.linearGradient(theme.gradients.primary)
                        else
                            Brush.linearGradient(listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            ))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (mode == ThemeManager.UiMode.ENHANCED)
                        Icons.Default.AutoAwesome
                    else
                        Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = if (mode == ThemeManager.UiMode.ENHANCED)
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                mode.getDisplayName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            Text(
                mode.getDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 主题卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    isEnhanced: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .scale(scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(theme.shapes.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 渐变预览
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        brush = Brush.linearGradient(theme.gradients.primary),
                        shape = RoundedCornerShape(
                            topStart = theme.shapes.cardRadius,
                            topEnd = theme.shapes.cardRadius
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Color圆点预览
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        theme.darkColors.primary,
                        theme.darkColors.secondary,
                        theme.darkColors.tertiary
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
                
                // 选中标记
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Theme信息
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = theme.type.getDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = theme.type.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
                Spacer(modifier = Modifier.height(6.dp))
                // 特性标签
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FeatureChip(theme.animationStyle.getDisplayName())
                    if (theme.effects.enableGlow) {
                        FeatureChip(Strings.glow)
                    }
                    if (theme.effects.enableParticles) {
                        FeatureChip(Strings.particle)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * 外观设置标签页
 */
@Composable
private fun AppearanceTab(
    darkMode: ThemeManager.DarkModeSettings,
    onDarkModeChange: (ThemeManager.DarkModeSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 暗色模式
        Text(Strings.darkMode, style = MaterialTheme.typography.titleMedium)
        
        ThemeManager.DarkModeSettings.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDarkModeChange(mode) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = darkMode == mode,
                    onClick = { onDarkModeChange(mode) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(mode.getDisplayName(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when (mode) {
                            ThemeManager.DarkModeSettings.SYSTEM -> Strings.autoSwitchBySystem
                            ThemeManager.DarkModeSettings.LIGHT -> Strings.alwaysUseLightTheme
                            ThemeManager.DarkModeSettings.DARK -> Strings.alwaysUseDarkTheme
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Divider()
        
        // 预览卡片
        Text(Strings.previewEffect, style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 浅色预览
            PreviewCard(
                title = Strings.lightModePreview,
                isDark = false,
                modifier = Modifier.weight(1f)
            )
            // 深色预览
            PreviewCard(
                title = Strings.darkModePreview,
                isDark = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PreviewCard(
    title: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
    val contentColor = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    val primaryColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // 模拟卡片
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = contentColor.copy(alpha = 0.1f)
            ) {}
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 模拟按钮
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                shape = RoundedCornerShape(14.dp),
                color = primaryColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        Strings.button,
                        color = if (isDark) Color.Black else Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * 动画设置标签页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimationTab(
    enableAnimations: Boolean,
    enableParticles: Boolean,
    enableHaptics: Boolean,
    enableSound: Boolean,
    animationSpeed: ThemeManager.AnimationSpeed,
    currentTheme: AppTheme,
    uiMode: ThemeManager.UiMode,
    onEnableAnimationsChange: (Boolean) -> Unit,
    onEnableParticlesChange: (Boolean) -> Unit,
    onEnableHapticsChange: (Boolean) -> Unit,
    onEnableSoundChange: (Boolean) -> Unit,
    onAnimationSpeedChange: (ThemeManager.AnimationSpeed) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 强化模式提示
        if (uiMode == ThemeManager.UiMode.ENHANCED) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.enhancedModeEnabled,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            Strings.enjoyImmersiveExperience.format(currentTheme.type.getDisplayName()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // 动画预览
        AnimationPreviewCard(
            theme = currentTheme,
            enabled = enableAnimations,
            showParticles = enableParticles
        )
        
        Divider()
        
        // 动画开关
        SwitchSettingItem(
            title = Strings.enableAnimations,
            description = Strings.enableAnimationsHint,
            checked = enableAnimations,
            onCheckedChange = onEnableAnimationsChange
        )
        
        SwitchSettingItem(
            title = Strings.particleEffects,
            description = if (uiMode == ThemeManager.UiMode.ENHANCED) 
                Strings.particleEffectsEnhancedHint 
            else 
                Strings.particleEffectsHint,
            checked = enableParticles,
            onCheckedChange = onEnableParticlesChange,
            enabled = enableAnimations && currentTheme.effects.enableParticles
        )
        
        SwitchSettingItem(
            title = Strings.hapticFeedback,
            description = Strings.hapticFeedbackHint,
            checked = enableHaptics,
            onCheckedChange = onEnableHapticsChange
        )
        
        SwitchSettingItem(
            title = Strings.soundFeedback,
            description = Strings.soundFeedbackHint,
            checked = enableSound,
            onCheckedChange = onEnableSoundChange,
            enabled = uiMode == ThemeManager.UiMode.ENHANCED
        )
        
        Divider()
        
        // 动画速度
        Text(Strings.animationSpeed, style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeManager.AnimationSpeed.entries.forEach { speed ->
                FilterChip(
                    selected = animationSpeed == speed,
                    onClick = { onAnimationSpeedChange(speed) },
                    label = { Text(speed.getDisplayName()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Divider()
        
        // 当前主题动画风格
        Text(Strings.currentThemeAnimStyle, style = MaterialTheme.typography.titleMedium)
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Animation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        currentTheme.animationStyle.getDisplayName(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TouchApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Strings.interactionStyleLabel.format(currentTheme.interactionStyle.getDisplayName()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * 动画预览卡片 - 根据主题动画风格展示不同效果
 */
@Composable
private fun AnimationPreviewCard(
    theme: AppTheme,
    enabled: Boolean,
    showParticles: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "preview")
    
    // 根据主题动画风格获取不同的动画参数
    val (duration, easing, dampingRatio) = remember(theme.animationStyle) {
        when (theme.animationStyle) {
            AnimationStyle.SMOOTH -> Triple(2500, FastOutSlowInEasing, 0.8f)
            AnimationStyle.BOUNCY -> Triple(1500, LinearOutSlowInEasing, 0.4f)
            AnimationStyle.SNAPPY -> Triple(800, LinearOutSlowInEasing, 0.9f)
            AnimationStyle.ELEGANT -> Triple(3500, FastOutSlowInEasing, 0.85f)
            AnimationStyle.PLAYFUL -> Triple(1200, FastOutSlowInEasing, 0.3f)
            AnimationStyle.DRAMATIC -> Triple(2000, FastOutSlowInEasing, 0.6f)
        }
    }
    
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = easing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    // 弹跳动画 - 用于 BOUNCY 和 PLAYFUL 风格
    val bounceAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (theme.animationStyle == AnimationStyle.BOUNCY || 
                         theme.animationStyle == AnimationStyle.PLAYFUL) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (800 / (1f - dampingRatio + 0.1f)).toInt().coerceIn(400, 1500),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (theme.animationStyle) {
                    AnimationStyle.SNAPPY -> 4000
                    AnimationStyle.ELEGANT -> 12000
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    
    // 脉冲动画 - 用于 DRAMATIC 风格
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(theme.shapes.cardRadius),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(theme.gradients.background))
        ) {
            // 粒子效果
            if (enabled && showParticles && theme.effects.enableParticles) {
                when (theme.type) {
                    AppThemeType.SAKURA -> SakuraPetalsBackground(
                        petalColor = theme.effects.particleColor,
                        petalCount = 15
                    )
                    AppThemeType.GALAXY -> StarfieldBackground(
                        starColor = theme.effects.particleColor,
                        starCount = 50
                    )
                    else -> ParticleBackground(
                        color = theme.effects.particleColor,
                        particleCount = 30
                    )
                }
            }
            
            // 动画演示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 根据动画风格展示不同的动画效果
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer {
                            if (enabled) {
                                when (theme.animationStyle) {
                                    AnimationStyle.SMOOTH -> {
                                        translationY = floatAnim * 16f - 8f
                                        alpha = 0.8f + floatAnim * 0.2f
                                    }
                                    AnimationStyle.BOUNCY -> {
                                        scaleX = bounceAnim
                                        scaleY = bounceAnim
                                        translationY = (1f - bounceAnim) * 30f
                                    }
                                    AnimationStyle.SNAPPY -> {
                                        translationX = (floatAnim - 0.5f) * 40f
                                        rotationZ = (floatAnim - 0.5f) * 20f
                                    }
                                    AnimationStyle.ELEGANT -> {
                                        translationY = floatAnim * 12f - 6f
                                        rotationZ = rotateAnim * 0.05f
                                        alpha = 0.85f + floatAnim * 0.15f
                                    }
                                    AnimationStyle.PLAYFUL -> {
                                        scaleX = bounceAnim
                                        scaleY = 2f - bounceAnim
                                        rotationZ = sin(floatAnim * PI.toFloat() * 2) * 15f
                                    }
                                    AnimationStyle.DRAMATIC -> {
                                        scaleX = pulseAnim
                                        scaleY = pulseAnim
                                        rotationZ = rotateAnim * 0.2f
                                    }
                                }
                            }
                        }
                        .clip(CircleShape)
                        .background(Brush.linearGradient(theme.gradients.primary))
                        .then(
                            if (enabled && theme.effects.enableGlow) {
                                Modifier.drawBehind {
                                    val glowAlpha = when (theme.animationStyle) {
                                        AnimationStyle.DRAMATIC -> pulseAnim * 0.5f
                                        AnimationStyle.BOUNCY -> (bounceAnim - 1f) * 3f + 0.3f
                                        else -> 0.3f + floatAnim * 0.2f
                                    }
                                    drawCircle(
                                        color = theme.effects.glowColor.copy(alpha = glowAlpha),
                                        radius = size.minDimension / 2 + theme.effects.glowRadius.toPx()
                                    )
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 动画风格名称
                Text(
                    theme.animationStyle.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    if (enabled) Strings.clickButtonToExperience else Strings.animationDisabled,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 交互演示按钮
                if (enabled) {
                    AnimatedDemoButton(theme = theme)
                }
            }
        }
    }
}

/**
 * 动画演示按钮 - 展示主题的交互动画效果
 */
@Composable
private fun AnimatedDemoButton(theme: AppTheme) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) {
            when (theme.animationStyle) {
                AnimationStyle.BOUNCY -> 0.85f
                AnimationStyle.SNAPPY -> 0.92f
                AnimationStyle.PLAYFUL -> 0.8f
                else -> 0.95f
            }
        } else 1f,
        animationSpec = getSpringSpec(theme.animationStyle, 1f),
        label = "buttonScale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isPressed && theme.animationStyle == AnimationStyle.PLAYFUL) -5f else 0f,
        animationSpec = spring(dampingRatio = 0.3f),
        label = "buttonRotation"
    )
    
    Surface(
        modifier = Modifier
            .scale(scale)
            .graphicsLayer { rotationZ = rotation },
        shape = RoundedCornerShape(theme.shapes.buttonRadius),
        color = MaterialTheme.colorScheme.primary,
        onClick = { }
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(theme.gradients.accent))
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                Strings.holdToExperience,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * 主题预览对话框
 */
@Composable
private fun ThemePreviewDialog(
    theme: AppTheme,
    uiMode: ThemeManager.UiMode,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(theme.gradients.primary))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(theme.type.getDisplayName())
                        if (uiMode == ThemeManager.UiMode.ENHANCED) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    Strings.enhancedVersion,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        theme.type.getDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Color预览
                Text(Strings.colorScheme, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Strings.primaryColor to theme.darkColors.primary,
                        Strings.secondaryColor to theme.darkColors.secondary,
                        Strings.accentColor to theme.darkColors.tertiary
                    ).forEach { (name, color) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                Divider()
                
                // 特性
                Text(Strings.themeFeatures, style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FeatureRow(Strings.animationStyle, theme.animationStyle.getDisplayName())
                    FeatureRow(Strings.interactionMethod, theme.interactionStyle.getDisplayName())
                    FeatureRow(Strings.cornerRadius, "${theme.shapes.cornerRadius}")
                    FeatureRow(Strings.glowEffect, if (theme.effects.enableGlow) "✓" else "✗")
                    FeatureRow(Strings.particleEffect, if (theme.effects.enableParticles) "✓" else "✗")
                    FeatureRow(Strings.glassmorphism, if (theme.effects.enableGlassmorphism) "✓" else "✗")
                }
            }
        },
        confirmButton = {
            Button(onClick = onApply) {
                Text(Strings.applyTheme)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}

@Composable
private fun FeatureRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
