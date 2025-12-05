package com.webtoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    val enableAnimations by themeManager.enableAnimationsFlow.collectAsState(initial = true)
    val enableParticles by themeManager.enableParticlesFlow.collectAsState(initial = true)
    val enableHaptics by themeManager.enableHapticsFlow.collectAsState(initial = true)
    val animationSpeed by themeManager.animationSpeedFlow.collectAsState(initial = ThemeManager.AnimationSpeed.NORMAL)
    
    var selectedTab by remember { mutableStateOf(0) }
    var showThemePreview by remember { mutableStateOf<AppThemeType?>(null) }
    
    val currentTheme = AppThemes.getTheme(currentThemeType)
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("主题设置")
                        Text(
                            currentTheme.type.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
                    text = { Text("主题") },
                    icon = { Icon(Icons.Outlined.Palette, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("外观") },
                    icon = { Icon(Icons.Outlined.Tune, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("动画") },
                    icon = { Icon(Icons.Outlined.Animation, null) }
                )
            }
            
            when (selectedTab) {
                0 -> ThemeSelectionTab(
                    currentThemeType = currentThemeType,
                    onThemeSelect = { type ->
                        scope.launch { themeManager.setThemeType(type) }
                    },
                    onPreviewTheme = { showThemePreview = it }
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
                    animationSpeed = animationSpeed,
                    currentTheme = currentTheme,
                    onEnableAnimationsChange = { scope.launch { themeManager.setEnableAnimations(it) } },
                    onEnableParticlesChange = { scope.launch { themeManager.setEnableParticles(it) } },
                    onEnableHapticsChange = { scope.launch { themeManager.setEnableHaptics(it) } },
                    onAnimationSpeedChange = { scope.launch { themeManager.setAnimationSpeed(it) } }
                )
            }
        }
    }
    
    // 主题预览对话框
    showThemePreview?.let { themeType ->
        ThemePreviewDialog(
            theme = AppThemes.getTheme(themeType),
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
    onThemeSelect: (AppThemeType) -> Unit,
    onPreviewTheme: (AppThemeType) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(AppThemes.allThemes) { theme ->
            ThemeCard(
                theme = theme,
                isSelected = theme.type == currentThemeType,
                onClick = { onThemeSelect(theme.type) },
                onLongClick = { onPreviewTheme(theme.type) }
            )
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
                // 颜色圆点预览
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
            
            // 主题信息
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = theme.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = theme.type.description,
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
                    FeatureChip(theme.animationStyle.displayName)
                    if (theme.effects.enableGlow) {
                        FeatureChip("发光")
                    }
                    if (theme.effects.enableParticles) {
                        FeatureChip("粒子")
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
        Text("深色模式", style = MaterialTheme.typography.titleMedium)
        
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
                    Text(mode.displayName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when (mode) {
                            ThemeManager.DarkModeSettings.SYSTEM -> "根据系统设置自动切换"
                            ThemeManager.DarkModeSettings.LIGHT -> "始终使用浅色主题"
                            ThemeManager.DarkModeSettings.DARK -> "始终使用深色主题"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Divider()
        
        // 预览卡片
        Text("预览效果", style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 浅色预览
            PreviewCard(
                title = "浅色模式",
                isDark = false,
                modifier = Modifier.weight(1f)
            )
            // 深色预览
            PreviewCard(
                title = "深色模式",
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
                        "按钮",
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
    animationSpeed: ThemeManager.AnimationSpeed,
    currentTheme: AppTheme,
    onEnableAnimationsChange: (Boolean) -> Unit,
    onEnableParticlesChange: (Boolean) -> Unit,
    onEnableHapticsChange: (Boolean) -> Unit,
    onAnimationSpeedChange: (ThemeManager.AnimationSpeed) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 动画预览
        AnimationPreviewCard(
            theme = currentTheme,
            enabled = enableAnimations,
            showParticles = enableParticles
        )
        
        Divider()
        
        // 动画开关
        SwitchSettingItem(
            title = "启用动画",
            description = "开启界面过渡动画和交互反馈",
            checked = enableAnimations,
            onCheckedChange = onEnableAnimationsChange
        )
        
        SwitchSettingItem(
            title = "粒子效果",
            description = "显示主题特有的背景粒子动画",
            checked = enableParticles,
            onCheckedChange = onEnableParticlesChange,
            enabled = enableAnimations && currentTheme.effects.enableParticles
        )
        
        SwitchSettingItem(
            title = "触觉反馈",
            description = "交互时提供震动反馈",
            checked = enableHaptics,
            onCheckedChange = onEnableHapticsChange
        )
        
        Divider()
        
        // 动画速度
        Text("动画速度", style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeManager.AnimationSpeed.entries.forEach { speed ->
                FilterChip(
                    selected = animationSpeed == speed,
                    onClick = { onAnimationSpeedChange(speed) },
                    label = { Text(speed.displayName) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Divider()
        
        // 当前主题动画风格
        Text("当前主题动画风格", style = MaterialTheme.typography.titleMedium)
        
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
                        currentTheme.animationStyle.displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TouchApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "交互风格: ${currentTheme.interactionStyle.displayName}",
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
 * 动画预览卡片
 */
@Composable
private fun AnimationPreviewCard(
    theme: AppTheme,
    enabled: Boolean,
    showParticles: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "preview")
    
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
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
                // 浮动圆形
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer {
                            if (enabled) {
                                translationY = floatAnim * 20f - 10f
                                rotationZ = rotateAnim * 0.1f
                            }
                        }
                        .clip(CircleShape)
                        .background(Brush.linearGradient(theme.gradients.primary))
                        .then(
                            if (enabled && theme.effects.enableGlow) {
                                Modifier.drawBehind {
                                    drawCircle(
                                        color = theme.effects.glowColor.copy(alpha = 0.4f * floatAnim),
                                        radius = size.minDimension / 2 + 16.dp.toPx()
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    if (enabled) "动画已启用" else "动画已禁用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 主题预览对话框
 */
@Composable
private fun ThemePreviewDialog(
    theme: AppTheme,
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
                    Text(theme.type.displayName)
                    Text(
                        theme.type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 颜色预览
                Text("配色方案", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "主色" to theme.darkColors.primary,
                        "次色" to theme.darkColors.secondary,
                        "强调" to theme.darkColors.tertiary
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
                Text("主题特性", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FeatureRow("动画风格", theme.animationStyle.displayName)
                    FeatureRow("交互方式", theme.interactionStyle.displayName)
                    FeatureRow("圆角大小", "${theme.shapes.cornerRadius}")
                    FeatureRow("发光效果", if (theme.effects.enableGlow) "✓" else "✗")
                    FeatureRow("粒子效果", if (theme.effects.enableParticles) "✓" else "✗")
                    FeatureRow("玻璃拟态", if (theme.effects.enableGlassmorphism) "✓" else "✗")
                }
            }
        },
        confirmButton = {
            Button(onClick = onApply) {
                Text("应用主题")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
