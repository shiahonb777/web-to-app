package com.webtoapp.ui.theme.enhanced

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.theme.*

/**
 * 是否启用强化背景的 CompositionLocal
 * 子组件可以根据这个值决定是否使用透明背景
 */
val LocalEnhancedBackgroundEnabled = staticCompositionLocalOf { false }

/**
 * 强化模式触摸事件数据类
 */
data class EnhancedTouchEvent(
    val position: Offset,
    val type: TouchType,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class TouchType {
        TAP,           // 单击
        LONG_PRESS,    // 长按
        DRAG,          // 拖拽
        DRAG_END       // 拖拽结束
    }
}

/**
 * 触摸事件的 CompositionLocal
 * 背景组件可以监听这个值来响应触摸事件
 */
val LocalEnhancedTouchEvent = compositionLocalOf<State<EnhancedTouchEvent?>> { 
    mutableStateOf(null) 
}

/**
 * 强化模式下的零阴影 Elevation
 * 用于消除 ElevatedCard 等组件的灰色边框
 */
@Composable
fun enhancedCardElevation() = CardDefaults.elevatedCardElevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
    draggedElevation = 0.dp,
    disabledElevation = 0.dp
)

/**
 * 强化版主题包装器
 * 根据当前主题和 UI 模式自动应用对应的背景效果
 */
@Composable
fun EnhancedThemeWrapper(
    modifier: Modifier = Modifier,
    enableSound: Boolean = true,
    onInteraction: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }
    val theme = LocalAppTheme.current
    val uiMode by themeManager.uiModeFlow.collectAsState()
    val animSettings = LocalAnimationSettings.current
    
    // 触摸事件状态 - 用于传递给背景组件
    val touchEvent = remember { mutableStateOf<EnhancedTouchEvent?>(null) }
    
    // 强化模式判断：
    // 1. UI 模式为 ENHANCED（从 ThemeManager 的 StateFlow）
    // 2. 或者 animSettings.enhancedMode 为 true（从 Theme.kt 同步）
    // 3. 动画必须启用
    val isEnhanced = remember(uiMode, animSettings) {
        val result = (uiMode == ThemeManager.UiMode.ENHANCED || animSettings.enhancedMode) && animSettings.enabled
        android.util.Log.d("EnhancedThemeWrapper", "isEnhanced=$result, uiMode=$uiMode, enhancedMode=${animSettings.enhancedMode}, animEnabled=${animSettings.enabled}")
        result
    }
    
    // Listen uiMode 变化
    LaunchedEffect(uiMode) {
        android.util.Log.d("EnhancedThemeWrapper", "uiMode changed to: $uiMode")
    }
    
    CompositionLocalProvider(
        LocalEnhancedBackgroundEnabled provides isEnhanced,
        LocalEnhancedTouchEvent provides touchEvent
    ) {
        // 在外层 Box 上监听触摸事件，使用 Initial pass 不阻止子组件接收事件
        val touchModifier = if (isEnhanced) {
            Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val position = event.changes.firstOrNull()?.position ?: continue
                        
                        when (event.type) {
                            PointerEventType.Press -> {
                                touchEvent.value = EnhancedTouchEvent(
                                    position = position,
                                    type = EnhancedTouchEvent.TouchType.TAP
                                )
                                onInteraction()
                            }
                            PointerEventType.Move -> {
                                if (event.changes.any { it.pressed }) {
                                    touchEvent.value = EnhancedTouchEvent(
                                        position = position,
                                        type = EnhancedTouchEvent.TouchType.DRAG
                                    )
                                }
                            }
                            PointerEventType.Release -> {
                                touchEvent.value = EnhancedTouchEvent(
                                    position = position,
                                    type = EnhancedTouchEvent.TouchType.DRAG_END
                                )
                            }
                            else -> {}
                        }
                        // 不调用 consume()，让事件继续传递到子组件
                    }
                }
            }
        } else Modifier
        
        Box(modifier = modifier.fillMaxSize().then(touchModifier)) {
            // 根据 UI 模式决定是否显示强化背景
            if (isEnhanced) {
                // 根据主题类型显示对应的强化背景
                when (theme.type) {
                    AppThemeType.AURORA -> AuroraEnhancedBackground(
                        theme = theme
                    )
                    AppThemeType.SAKURA -> SakuraEnhancedBackground(
                        theme = theme,
                        enableSound = enableSound,
                        onInteraction = onInteraction
                    )
                    AppThemeType.OCEAN -> OceanPhysicsBackground(
                        theme = theme,
                        onInteraction = onInteraction
                    )
                    AppThemeType.GALAXY -> GalaxyPhysicsBackground(
                        theme = theme,
                        onInteraction = onInteraction
                    )
                    AppThemeType.CYBERPUNK -> CyberpunkEnhancedBackground(
                        theme = theme,
                        onInteraction = onInteraction
                    )
                    AppThemeType.VOLCANO -> VolcanoEnhancedBackground(
                        theme = theme,
                        onInteraction = onInteraction
                    )
                    AppThemeType.FROST -> FrostEnhancedBackground(
                        theme = theme,
                        onInteraction = onInteraction
                    )
                    // 其他主题使用默认粒子背景
                    else -> DefaultEnhancedBackground(
                        theme = theme,
                        onInteraction = onInteraction
                    )
                }
            }
            
            // 内容层 - 使用修改后的配色方案
            if (isEnhanced) {
                // 强化模式：使用半透明背景的配色方案
                // 注意：保留 outline 和 outlineVariant 颜色，否则 Switch、Checkbox 等组件的边框会消失
                val transparentColorScheme = MaterialTheme.colorScheme.copy(
                    background = MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                    surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    surfaceVariant = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    // 完全透明的 surfaceTint 消除 elevation 色调叠加
                    surfaceTint = Color.Transparent,
                    // 保留 outline 颜色用于 Switch、Checkbox 等组件的边框
                    // outline = 保持原值
                    // outlineVariant = 保持原值
                    // 阴影颜色设为透明（用于某些组件的阴影）
                    scrim = Color.Transparent
                )
                MaterialTheme(
                    colorScheme = transparentColorScheme,
                    typography = MaterialTheme.typography,
                    shapes = MaterialTheme.shapes
                ) {
                    content()
                }
            } else {
                // Default模式：正常显示
                content()
            }
        }
    }
}


/**
 * 默认强化背景
 * 用于没有专属强化效果的主题
 */
@Composable
fun DefaultEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    // 使用高级物理引擎驱动的主题
    when (theme.type) {
        // 使用新的专业级物理引擎主题
        AppThemeType.FOREST -> ForestEcosystemBackground(theme = theme, onInteraction = onInteraction)
        
        // 其他主题使用原有实现
        AppThemeType.SUNSET -> SunsetEnhancedBackground(theme = theme, onInteraction = onInteraction)
        AppThemeType.MINIMAL -> MinimalEnhancedBackground(theme = theme, onInteraction = onInteraction)
        AppThemeType.NEON_TOKYO -> NeonTokyoEnhancedBackground(theme = theme, onInteraction = onInteraction)
        AppThemeType.LAVENDER -> LavenderEnhancedBackground(theme = theme, onInteraction = onInteraction)
        else -> {
            // 通用粒子背景
            if (theme.effects.enableParticles) {
                com.webtoapp.ui.theme.ParticleBackground(
                    color = theme.effects.particleColor,
                    particleCount = 50,
                    modifier = modifier
                )
            }
        }
    }
}
