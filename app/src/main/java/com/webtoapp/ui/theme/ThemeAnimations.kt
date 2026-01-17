package com.webtoapp.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * 主题动画系统
 * 提供各种动画效果和交互反馈
 */

// ==================== 动画参数获取 ====================

/**
 * 根据动画风格获取弹簧规格
 */
fun getSpringSpec(style: AnimationStyle, speedMultiplier: Float = 1f): SpringSpec<Float> {
    val (dampingRatio, stiffness) = when (style) {
        AnimationStyle.SMOOTH -> 0.8f to Spring.StiffnessLow
        AnimationStyle.BOUNCY -> 0.5f to Spring.StiffnessMedium
        AnimationStyle.SNAPPY -> 0.9f to Spring.StiffnessHigh
        AnimationStyle.ELEGANT -> 0.85f to Spring.StiffnessVeryLow
        AnimationStyle.PLAYFUL -> 0.4f to Spring.StiffnessMediumLow
        AnimationStyle.DRAMATIC -> 0.7f to Spring.StiffnessMedium
    }
    return spring(dampingRatio = dampingRatio, stiffness = stiffness / speedMultiplier)
}

/**
 * 根据动画风格获取 Dp 类型的弹簧规格
 */
fun getSpringSpecDp(style: AnimationStyle, speedMultiplier: Float = 1f): SpringSpec<Dp> {
    val (dampingRatio, stiffness) = when (style) {
        AnimationStyle.SMOOTH -> 0.8f to Spring.StiffnessLow
        AnimationStyle.BOUNCY -> 0.5f to Spring.StiffnessMedium
        AnimationStyle.SNAPPY -> 0.9f to Spring.StiffnessHigh
        AnimationStyle.ELEGANT -> 0.85f to Spring.StiffnessVeryLow
        AnimationStyle.PLAYFUL -> 0.4f to Spring.StiffnessMediumLow
        AnimationStyle.DRAMATIC -> 0.7f to Spring.StiffnessMedium
    }
    return spring(dampingRatio = dampingRatio, stiffness = stiffness / speedMultiplier)
}

/**
 * 根据动画风格获取 tween 动画规格
 */
fun getTweenSpec(style: AnimationStyle, speedMultiplier: Float = 1f): TweenSpec<Float> {
    val (duration, easing) = when (style) {
        AnimationStyle.SMOOTH -> 350 to FastOutSlowInEasing
        AnimationStyle.BOUNCY -> 250 to LinearOutSlowInEasing
        AnimationStyle.SNAPPY -> 150 to LinearOutSlowInEasing
        AnimationStyle.ELEGANT -> 500 to CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        AnimationStyle.PLAYFUL -> 300 to FastOutSlowInEasing
        AnimationStyle.DRAMATIC -> 400 to CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
    }
    return tween(durationMillis = (duration * speedMultiplier).toInt(), easing = easing)
}

/**
 * 根据动画风格获取按压时的缩放值
 */
fun getPressedScale(style: AnimationStyle): Float {
    return when (style) {
        AnimationStyle.SMOOTH -> 0.96f
        AnimationStyle.BOUNCY -> 0.88f
        AnimationStyle.SNAPPY -> 0.94f
        AnimationStyle.ELEGANT -> 0.97f
        AnimationStyle.PLAYFUL -> 0.85f
        AnimationStyle.DRAMATIC -> 0.90f
    }
}

/**
 * 根据动画风格获取缓动曲线
 */
fun getEasing(style: AnimationStyle): Easing {
    return when (style) {
        AnimationStyle.SMOOTH -> FastOutSlowInEasing
        AnimationStyle.BOUNCY -> EaseOutBounce
        AnimationStyle.SNAPPY -> LinearOutSlowInEasing
        AnimationStyle.ELEGANT -> CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        AnimationStyle.PLAYFUL -> EaseOutBack
        AnimationStyle.DRAMATIC -> CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
    }
}

/**
 * 自定义缓动曲线
 */
val EaseOutBounce = Easing { fraction ->
    val n1 = 7.5625f
    val d1 = 2.75f
    var t = fraction
    when {
        t < 1f / d1 -> n1 * t * t
        t < 2f / d1 -> {
            t -= 1.5f / d1
            n1 * t * t + 0.75f
        }
        t < 2.5f / d1 -> {
            t -= 2.25f / d1
            n1 * t * t + 0.9375f
        }
        else -> {
            t -= 2.625f / d1
            n1 * t * t + 0.984375f
        }
    }
}

val EaseOutBack = Easing { fraction ->
    val c1 = 1.70158f
    val c3 = c1 + 1f
    1f + c3 * (fraction - 1f).pow(3) + c1 * (fraction - 1f).pow(2)
}

// ==================== Modifier扩展 ====================

/**
 * 脉冲发光效果
 */
fun Modifier.pulseGlow(
    color: Color,
    enabled: Boolean = true,
    radius: Dp = 20.dp
): Modifier = composed {
    if (!enabled) return@composed this
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    this.drawBehind {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius.toPx(),
            center = center
        )
    }
}

/**
 * 闪烁效果
 */
fun Modifier.shimmer(
    colors: List<Color>,
    enabled: Boolean = true,
    durationMillis: Int = 2000
): Modifier = composed {
    if (!enabled || colors.isEmpty()) return@composed this
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    this.drawWithContent {
        drawContent()
        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(size.width * translateAnim - size.width, 0f),
            end = Offset(size.width * translateAnim, size.height)
        )
        drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
    }
}

/**
 * 悬浮效果（垂直漂浮动画）
 */
fun Modifier.floatingAnimation(
    enabled: Boolean = true,
    offsetY: Dp = 8.dp
): Modifier = composed {
    if (!enabled) return@composed this
    
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    
    val density = LocalDensity.current
    val offsetPx = with(density) { offsetY.toPx() }
    
    this.offset(y = (offset * offsetPx / density.density).dp)
}

/**
 * 呼吸缩放效果
 */
fun Modifier.breathingScale(
    enabled: Boolean = true,
    minScale: Float = 0.98f,
    maxScale: Float = 1.02f
): Modifier = composed {
    if (!enabled) return@composed this
    
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    
    this.scale(scale)
}

/**
 * 旋转光晕效果
 */
fun Modifier.rotatingGlow(
    colors: List<Color>,
    enabled: Boolean = true,
    radius: Dp = 100.dp
): Modifier = composed {
    if (!enabled || colors.size < 2) return@composed this
    
    val infiniteTransition = rememberInfiniteTransition(label = "rotatingGlow")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    this.drawBehind {
        rotate(rotation) {
            drawCircle(
                brush = Brush.sweepGradient(colors),
                radius = radius.toPx(),
                center = center,
                alpha = 0.3f
            )
        }
    }
}

/**
 * 波纹扩散背景
 */
@Composable
fun RippleBackground(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    
    val ripples = remember {
        listOf(0, 1000, 2000).map { delay ->
            delay to Random.nextFloat() * 0.3f + 0.2f
        }
    }
    
    Box(modifier = modifier) {
        ripples.forEachIndexed { index, (delayMs, alpha) ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, delayMillis = delayMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleScale$index"
            )
            val alphaAnim by infiniteTransition.animateFloat(
                initialValue = alpha,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, delayMillis = delayMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleAlpha$index"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .background(
                        color.copy(alpha = alphaAnim),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

// ==================== 粒子系统 ====================

/**
 * 粒子数据
 */
data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var radius: Float,
    var alpha: Float,
    var life: Float,
    var maxLife: Float
)

/**
 * 粒子效果背景
 */
@Composable
fun ParticleBackground(
    color: Color,
    particleCount: Int = 50,
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    var size by remember { mutableStateOf(Size.Zero) }
    
    LaunchedEffect(size, particleCount) {
        if (size.width > 0 && size.height > 0) {
            particles = List(particleCount) {
                createParticle(size.width, size.height)
            }
            
            while (true) {
                delay(16) // ~60fps
                particles = particles.map { p ->
                    updateParticle(p, size.width, size.height)
                }
            }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { size = this.size }
    ) {
        particles.forEach { particle ->
            drawCircle(
                color = color.copy(alpha = particle.alpha),
                radius = particle.radius,
                center = Offset(particle.x, particle.y)
            )
        }
    }
}

private fun createParticle(width: Float, height: Float): Particle {
    return Particle(
        x = Random.nextFloat() * width,
        y = Random.nextFloat() * height,
        velocityX = (Random.nextFloat() - 0.5f) * 0.5f,
        velocityY = (Random.nextFloat() - 0.5f) * 0.5f - 0.3f,
        radius = Random.nextFloat() * 3f + 1f,
        alpha = Random.nextFloat() * 0.5f + 0.2f,
        life = 0f,
        maxLife = Random.nextFloat() * 200f + 100f
    )
}

private fun updateParticle(particle: Particle, width: Float, height: Float): Particle {
    var newParticle = particle.copy(
        x = particle.x + particle.velocityX,
        y = particle.y + particle.velocityY,
        life = particle.life + 1f
    )
    
    // 边界检查和重生
    if (newParticle.life > newParticle.maxLife ||
        newParticle.x < 0 || newParticle.x > width ||
        newParticle.y < 0 || newParticle.y > height) {
        newParticle = createParticle(width, height)
    }
    
    // 渐变透明度
    val lifeRatio = newParticle.life / newParticle.maxLife
    newParticle = newParticle.copy(
        alpha = (1f - lifeRatio) * 0.5f
    )
    
    return newParticle
}

/**
 * 星星粒子效果（星空主题专用）
 */
@Composable
fun StarfieldBackground(
    starColor: Color = Color.White,
    starCount: Int = 100,
    modifier: Modifier = Modifier
) {
    data class Star(
        val x: Float,
        val y: Float,
        val size: Float,
        val twinkleOffset: Float
    )
    
    var stars by remember { mutableStateOf(listOf<Star>()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )
    
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            stars = List(starCount) {
                Star(
                    x = Random.nextFloat() * canvasSize.width,
                    y = Random.nextFloat() * canvasSize.height,
                    size = Random.nextFloat() * 2f + 0.5f,
                    twinkleOffset = Random.nextFloat()
                )
            }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { canvasSize = size }
    ) {
        stars.forEach { star ->
            val alpha = ((sin((twinkle + star.twinkleOffset) * PI * 2) + 1) / 2 * 0.5 + 0.3).toFloat()
            drawCircle(
                color = starColor.copy(alpha = alpha),
                radius = star.size,
                center = Offset(star.x, star.y)
            )
        }
    }
}

/**
 * 樱花飘落效果（樱花主题专用）
 */
@Composable
fun SakuraPetalsBackground(
    petalColor: Color = Color(0xFFFFB7C5),
    petalCount: Int = 30,
    modifier: Modifier = Modifier
) {
    data class Petal(
        var x: Float,
        var y: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var fallSpeed: Float,
        var swayOffset: Float,
        var swaySpeed: Float,
        var size: Float
    )
    
    var petals by remember { mutableStateOf(listOf<Petal>()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableStateOf(0f) }
    
    LaunchedEffect(canvasSize, petalCount) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            petals = List(petalCount) {
                Petal(
                    x = Random.nextFloat() * canvasSize.width,
                    y = Random.nextFloat() * canvasSize.height - canvasSize.height,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 2f,
                    fallSpeed = Random.nextFloat() * 1f + 0.5f,
                    swayOffset = Random.nextFloat() * PI.toFloat() * 2,
                    swaySpeed = Random.nextFloat() * 0.02f + 0.01f,
                    size = Random.nextFloat() * 8f + 6f
                )
            }
            
            while (true) {
                delay(16)
                frame += 1f
                petals = petals.map { p ->
                    var newX = p.x + sin(frame * p.swaySpeed + p.swayOffset).toFloat() * 0.5f
                    var newY = p.y + p.fallSpeed
                    val newRotation = p.rotation + p.rotationSpeed
                    
                    if (newY > canvasSize.height + 20) {
                        newY = -20f
                        newX = Random.nextFloat() * canvasSize.width
                    }
                    
                    p.copy(x = newX, y = newY, rotation = newRotation)
                }
            }
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { canvasSize = size }
    ) {
        petals.forEach { petal ->
            rotate(petal.rotation, pivot = Offset(petal.x, petal.y)) {
                // 绘制花瓣形状
                drawOval(
                    color = petalColor.copy(alpha = 0.7f),
                    topLeft = Offset(petal.x - petal.size / 2, petal.y - petal.size / 4),
                    size = Size(petal.size, petal.size / 2)
                )
            }
        }
    }
}

// ==================== 页面过渡动画 ====================

/**
 * 根据主题风格获取页面进入动画
 */
fun getEnterTransition(style: AnimationStyle): EnterTransition {
    return when (style) {
        AnimationStyle.SMOOTH -> fadeIn(tween(300)) + slideInHorizontally { it / 4 }
        AnimationStyle.BOUNCY -> fadeIn(tween(250)) + scaleIn(
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
            initialScale = 0.8f
        )
        AnimationStyle.SNAPPY -> fadeIn(tween(150)) + slideInHorizontally(tween(150)) { it }
        AnimationStyle.ELEGANT -> fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }
        AnimationStyle.PLAYFUL -> fadeIn(tween(300)) + scaleIn(
            animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
            initialScale = 0.6f
        )
        AnimationStyle.DRAMATIC -> fadeIn(tween(400)) + expandIn(
            animationSpec = tween(400, easing = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f))
        )
    }
}

/**
 * 根据主题风格获取页面退出动画
 */
fun getExitTransition(style: AnimationStyle): ExitTransition {
    return when (style) {
        AnimationStyle.SMOOTH -> fadeOut(tween(300)) + slideOutHorizontally { -it / 4 }
        AnimationStyle.BOUNCY -> fadeOut(tween(250)) + scaleOut(targetScale = 0.8f)
        AnimationStyle.SNAPPY -> fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it }
        AnimationStyle.ELEGANT -> fadeOut(tween(500)) + slideOutVertically(tween(500)) { -it / 6 }
        AnimationStyle.PLAYFUL -> fadeOut(tween(300)) + scaleOut(targetScale = 0.6f)
        AnimationStyle.DRAMATIC -> fadeOut(tween(400)) + shrinkOut(
            animationSpec = tween(400, easing = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f))
        )
    }
}
