package com.webtoapp.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════
// 全局动画工具库
// ═══════════════════════════════════════════════════════════

// ==================== 1. Stagger 交错入场 ====================

/**
 * 列表项交错入场动画
 * 从下方滑入 + 淡入，每项间隔 [staggerDelayMs]
 *
 * 用法：在 LazyColumn 的 items 中用 StaggeredAnimatedItem 包裹每个 item
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    staggerDelayMs: Long = 50L,
    slideOffsetDp: Int = 40,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * staggerDelayMs)
        visible = true
    }

    val density = LocalDensity.current
    val slideOffsetPx = with(density) { slideOffsetDp.dp.roundToPx() }

    // ★ 使用 spring 而非 tween，打断时保留速度
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + slideInVertically(
            initialOffsetY = { slideOffsetPx },
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        modifier = modifier
    ) {
        content()
    }
}

// ==================== 2. 空状态呼吸浮动 ====================

/**
 * 呼吸浮动动画修饰符
 * 缓慢上下浮动 + 微旋转，让静态元素有生命力
 */
@Composable
fun Modifier.breathingFloat(
    floatAmountDp: Float = 6f,
    rotationDegrees: Float = 2f,
    durationMs: Int = 3000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    val translationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = floatAmountDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -rotationDegrees,
        targetValue = rotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween((durationMs * 1.3f).toInt(), easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    return this.graphicsLayer {
        this.translationY = -translationY * density
        this.rotationZ = rotation
        this.scaleX = scale
        this.scaleY = scale
    }
}

// ==================== 3. 对话框弹簧缩放 ====================

/**
 * 对话框入场缩放动画
 * scale 0.85→1.0 + alpha 0→1 弹簧效果
 */
@Composable
fun AnimatedDialogContent(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dialogAlpha"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}

// ==================== 4. Snackbar 弹簧滑入 ====================

/**
 * Snackbar 入场/退场动画规格
 */
val SnackbarEnterTransition: EnterTransition = slideInVertically(
    initialOffsetY = { it },
    animationSpec = spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow
    )
) + fadeIn(
    animationSpec = tween(200)
)

val SnackbarExitTransition: ExitTransition = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(250, easing = FastOutSlowInEasing)
) + fadeOut(
    animationSpec = tween(200)
)

// ==================== 5. Tab 切换滑动方向 ====================

/**
 * 计算 Tab 切换动画方向
 * @return 正值=向左滑（目标在右侧），负值=向右滑（目标在左侧）
 */
fun tabSlideDirection(previousTab: Int, currentTab: Int): Int {
    return if (currentTab > previousTab) 1 else -1
}

/**
 * Tab 页面内容的入场动画
 */
fun tabEnterTransition(direction: Int): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> direction * fullWidth / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
}

/**
 * Tab 页面内容的退场动画
 */
fun tabExitTransition(direction: Int): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -direction * fullWidth / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(250, easing = FastOutSlowInEasing)
    )
}

// ==================== 6. 卡片折叠展开 ====================

/**
 * 可折叠卡片的内容展开/收起动画规格 (Apple/物理世界风格)
 * 去除所有的渐显渐隐(fadeIn/fadeOut)，完全依赖纯物理的边界剪裁(Cliping)与弹簧位移，
 * 实现如同物理拉伸般的空间展开体验。
 */
val CardExpandTransition: EnterTransition = expandVertically(
    animationSpec = spring(
        dampingRatio = 0.82f, // 极为轻微的阻尼，保留"Q弹"但不会乱晃
        stiffness = 350f      // 适中的刚度，确保响应迅速 (iOS默认约相似范围)
    ),
    expandFrom = androidx.compose.ui.Alignment.Top,
    clip = true
)

val CardCollapseTransition: ExitTransition = shrinkVertically(
    // 采用拟物化的贝塞尔曲线来模拟物理降落，强制插值到 0，解决物理弹簧计算因底层0点反弹导致遗留像素的硬切。
    animationSpec = tween(
        durationMillis = 280,
        easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f) // 一种模拟物理急停和无阻尼缓冲的缓动
    ),
    shrinkTowards = androidx.compose.ui.Alignment.Top,
    clip = true
)

// ==================== 7. 开关涟漪效果 ====================

/**
 * 涟漪效果的动画参数
 */
data class RippleAnimState(
    val isActive: Boolean = false,
    val progress: Float = 0f
)

@Composable
fun rememberRippleAnimatable(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}
