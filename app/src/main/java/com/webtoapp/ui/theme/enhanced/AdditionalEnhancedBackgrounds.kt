package com.webtoapp.ui.theme.enhanced

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.webtoapp.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

/**
 * ============================================================
 * 额外高端主题背景 - Midnight, Emerald, RoseGold
 * ============================================================
 */

// ==================== 午夜星空 - Midnight ====================

/**
 * 午夜主题 - 终极版
 * 特性：
 * - 深邃星空（多层星星）
 * - 流星雨（物理轨迹）
 * - 月亮光晕（脉冲呼吸）
 * - 星云效果（柏林噪声）
 * - 萤火虫光点
 * - 触摸产生星星爆发
 * - 陀螺仪视差
 */
@Composable
fun MidnightEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "midnight")
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }

    // 粒子系统
    var stars by remember { mutableStateOf(listOf<MidnightStar>()) }
    var shootingStars by remember { mutableStateOf(listOf<MidnightShootingStar>()) }
    var glowParticles by remember { mutableStateOf(listOf<GlowParticle>()) }
    var touchBursts by remember { mutableStateOf(listOf<StarBurst>()) }
    
    // 传感器
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    val moonPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "moonPulse"
    )
    
    val nebulaPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "nebula"
    )
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = tiltX * 0.9f + event.values[0] * 0.1f
                tiltY = tiltY * 0.9f + event.values[1] * 0.1f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            stars = List(60) { createMidnightStar(canvasSize, it) }
            glowParticles = List(15) { createGlowParticle(canvasSize) }
            
            while (true) {
                frame++
                
                // Update星星闪烁
                stars = stars.map { star ->
                    star.copy(twinklePhase = star.twinklePhase + star.twinkleSpeed)
                }

                // Shuffle产生流星
                if (Random.nextFloat() < 0.008f && shootingStars.size < 5) {
                    shootingStars = shootingStars + createShootingStar(canvasSize)
                }
                
                // Update流星
                shootingStars = shootingStars.mapNotNull { star ->
                    val newStar = star.copy(
                        x = star.x + star.vx,
                        y = star.y + star.vy,
                        life = star.life - 0.02f,
                        trail = (star.trail + Offset(star.x, star.y)).takeLast(20)
                    )
                    if (newStar.life > 0 && newStar.y < canvasSize.height) newStar else null
                }
                
                // Update光点
                glowParticles = glowParticles.map { updateGlowParticle(it, canvasSize, frame) }
                
                // Update触摸爆发
                touchBursts = touchBursts.mapNotNull { burst ->
                    val newBurst = burst.copy(
                        radius = burst.radius + 4f,
                        alpha = burst.alpha - 0.025f
                    )
                    if (newBurst.alpha > 0) newBurst else null
                }
                
                delay(16)
            }
        }
    }
    
    // Response增强触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                val offset = event.position
                haptic.performHaptic(HapticType.SPARKLE)
                onInteraction()
                
                // 星星爆发
                touchBursts = touchBursts + StarBurst(offset.x, offset.y, 0f, 1f)
                
                // 添加新星星
                val newStars = List(4) {
                    MidnightStar(
                        x = offset.x + (Random.nextFloat() - 0.5f) * 100f,
                        y = offset.y + (Random.nextFloat() - 0.5f) * 100f,
                        size = Random.nextFloat() * 4f + 2f,
                        brightness = 1f,
                        twinklePhase = Random.nextFloat() * PI.toFloat() * 2,
                        twinkleSpeed = Random.nextFloat() * 0.1f + 0.02f,
                        layer = 2,
                        color = midnightStarColors.random()
                    )
                }
                stars = (stars + newStars).takeLast(200)
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height

        // 深邃夜空渐变
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A0A1A),
                    Color(0xFF0D0D25),
                    Color(0xFF101035),
                    Color(0xFF151545)
                )
            )
        )
        
        // 星云效果
        drawMidnightNebula(width, height, nebulaPhase, tiltX, tiltY)
        
        // 月亮
        val moonX = width * 0.8f + tiltX * 15f
        val moonY = height * 0.15f + tiltY * 10f
        drawMoonWithGlow(moonX, moonY, min(width, height) * 0.08f, moonPulse)
        
        // 星星（按层次绘制）
        stars.sortedBy { it.layer }.forEach { star ->
            drawMidnightStarEnhanced(star, tiltX, tiltY, frame)
        }
        
        // 流星
        shootingStars.forEach { star ->
            drawShootingStarEnhanced(star)
        }
        
        // 光点
        glowParticles.forEach { particle ->
            drawGlowParticleEnhanced(particle, frame)
        }
        
        // 触摸爆发
        touchBursts.forEach { burst ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE6E6FA).copy(alpha = burst.alpha * 0.5f),
                        Color(0xFF9370DB).copy(alpha = burst.alpha * 0.2f),
                        Color.Transparent
                    )
                ),
                radius = burst.radius,
                center = Offset(burst.x, burst.y)
            )
        }
        
        // 底部渐变
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFF0A0A1A).copy(alpha = 0.5f)),
                startY = height * 0.8f,
                endY = height
            )
        )
    }
}

// Midnight 数据类
data class MidnightStar(
    val x: Float, val y: Float,
    val size: Float, val brightness: Float,
    var twinklePhase: Float, val twinkleSpeed: Float,
    val layer: Int, val color: Color
)

data class MidnightShootingStar(
    var x: Float, var y: Float,
    val vx: Float, val vy: Float,
    var life: Float, var trail: List<Offset>
)

data class GlowParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var phase: Float,
    var alpha: Float, var color: Color
)

data class StarBurst(val x: Float, val y: Float, var radius: Float, var alpha: Float)


private val midnightStarColors = listOf(
    Color(0xFFFFFFFF), Color(0xFFE6E6FA), Color(0xFFB0C4DE),
    Color(0xFF87CEEB), Color(0xFFADD8E6), Color(0xFFF0F8FF)
)

private fun createMidnightStar(canvasSize: Size, index: Int): MidnightStar {
    val layer = index % 3
    val layerScale = 1f - layer * 0.25f
    return MidnightStar(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height * 0.85f,
        size = (Random.nextFloat() * 3f + 1f) * layerScale,
        brightness = Random.nextFloat() * 0.5f + 0.5f,
        twinklePhase = Random.nextFloat() * PI.toFloat() * 2,
        twinkleSpeed = Random.nextFloat() * 0.08f + 0.02f,
        layer = layer,
        color = midnightStarColors.random()
    )
}

private fun createShootingStar(canvasSize: Size): MidnightShootingStar {
    val startX = Random.nextFloat() * canvasSize.width * 0.7f + canvasSize.width * 0.15f
    val angle = Random.nextFloat() * 0.5f + 0.3f
    val speed = Random.nextFloat() * 15f + 10f
    return MidnightShootingStar(
        x = startX, y = -20f,
        vx = cos(angle) * speed,
        vy = sin(angle) * speed,
        life = 1f,
        trail = emptyList()
    )
}

private fun createGlowParticle(canvasSize: Size): GlowParticle {
    return GlowParticle(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        vx = (Random.nextFloat() - 0.5f) * 5f,
        vy = -Random.nextFloat() * 3f - 1f,
        size = Random.nextFloat() * 15f + 8f,
        phase = Random.nextFloat() * PI.toFloat() * 2,
        alpha = Random.nextFloat() * 0.2f + 0.1f,
        color = listOf(Color(0xFF9370DB), Color(0xFF6A5ACD), Color(0xFF483D8B)).random()
    )
}

private fun updateGlowParticle(particle: GlowParticle, canvasSize: Size, frame: Long): GlowParticle {
    var newParticle = particle.copy(
        x = particle.x + particle.vx * 0.016f + sin(frame * 0.01f + particle.phase) * 0.3f,
        y = particle.y + particle.vy * 0.016f,
        phase = particle.phase + 0.02f,
        alpha = (particle.alpha + sin(frame * 0.02f + particle.phase) * 0.01f).coerceIn(0.08f, 0.3f)
    )
    
    if (newParticle.y < -30 || newParticle.x < -30 || newParticle.x > canvasSize.width + 30) {
        newParticle = createGlowParticle(canvasSize).copy(y = canvasSize.height + 20)
    }
    
    return newParticle
}


// Midnight 绘制函数
private fun DrawScope.drawMidnightNebula(
    width: Float, height: Float, phase: Float,
    tiltX: Float, tiltY: Float
) {
    val nebulaColors = listOf(
        Color(0xFF4B0082).copy(alpha = 0.15f),
        Color(0xFF6A5ACD).copy(alpha = 0.1f),
        Color(0xFF483D8B).copy(alpha = 0.12f)
    )
    
    nebulaColors.forEachIndexed { i, color ->
        val offsetX = sin(phase * 2 * PI.toFloat() + i) * 50f + tiltX * 20f
        val offsetY = cos(phase * 2 * PI.toFloat() + i * 0.5f) * 30f + tiltY * 15f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = Offset(width * (0.3f + i * 0.2f) + offsetX, height * (0.25f + i * 0.15f) + offsetY),
                radius = min(width, height) * (0.4f + i * 0.1f)
            ),
            radius = min(width, height) * (0.5f + i * 0.15f),
            center = Offset(width * (0.3f + i * 0.2f) + offsetX, height * (0.25f + i * 0.15f) + offsetY)
        )
    }
}

private fun DrawScope.drawMoonWithGlow(x: Float, y: Float, radius: Float, pulse: Float) {
    val pulseScale = 1f + sin(pulse) * 0.03f
    
    // 外层光晕
    for (i in 5 downTo 1) {
        val layerRadius = radius * (1.5f + i * 0.8f) * pulseScale
        val layerAlpha = 0.06f / i
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFACD).copy(alpha = layerAlpha),
                    Color(0xFFE6E6FA).copy(alpha = layerAlpha * 0.5f),
                    Color.Transparent
                )
            ),
            radius = layerRadius,
            center = Offset(x, y)
        )
    }
    
    // 月亮主体
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFF0),
                Color(0xFFFFFACD),
                Color(0xFFE6E6FA)
            )
        ),
        radius = radius * pulseScale,
        center = Offset(x, y)
    )
    
    // 月亮纹理（陨石坑）
    val craters = listOf(
        Triple(0.2f, -0.15f, 0.12f),
        Triple(-0.25f, 0.2f, 0.08f),
        Triple(0.1f, 0.25f, 0.1f)
    )
    craters.forEach { (dx, dy, size) ->
        drawCircle(
            color = Color(0xFFD3D3D3).copy(alpha = 0.3f),
            radius = radius * size,
            center = Offset(x + radius * dx, y + radius * dy)
        )
    }
}


private fun DrawScope.drawMidnightStarEnhanced(
    star: MidnightStar, tiltX: Float, tiltY: Float, frame: Long
) {
    val parallax = (star.layer + 1) * 0.3f
    val adjustedX = star.x + tiltX * parallax * 8f
    val adjustedY = star.y + tiltY * parallax * 6f
    
    val twinkle = (sin(star.twinklePhase) + 1) / 2
    val alpha = (star.brightness * (0.5f + twinkle * 0.5f)).coerceIn(0f, 1f)
    val currentSize = star.size * (0.8f + twinkle * 0.4f)
    
    // 星星光晕
    if (star.size > 2f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    star.color.copy(alpha = alpha * 0.3f),
                    Color.Transparent
                )
            ),
            radius = currentSize * 4f,
            center = Offset(adjustedX, adjustedY)
        )
    }
    
    // 十字光芒（大星星）
    if (star.size > 2.5f && twinkle > 0.6f) {
        val rayLength = currentSize * 3f
        val rayAlpha = alpha * 0.4f
        
        drawLine(
            color = star.color.copy(alpha = rayAlpha),
            start = Offset(adjustedX - rayLength, adjustedY),
            end = Offset(adjustedX + rayLength, adjustedY),
            strokeWidth = 1f
        )
        drawLine(
            color = star.color.copy(alpha = rayAlpha),
            start = Offset(adjustedX, adjustedY - rayLength),
            end = Offset(adjustedX, adjustedY + rayLength),
            strokeWidth = 1f
        )
    }
    
    // 星星核心
    drawCircle(
        color = star.color.copy(alpha = alpha),
        radius = currentSize,
        center = Offset(adjustedX, adjustedY)
    )
}

private fun DrawScope.drawShootingStarEnhanced(star: MidnightShootingStar) {
    // 轨迹
    if (star.trail.size > 1) {
        for (i in 1 until star.trail.size) {
            val alpha = (i.toFloat() / star.trail.size) * star.life * 0.6f
            val width = (i.toFloat() / star.trail.size) * 3f
            
            drawLine(
                color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
                start = star.trail[i - 1],
                end = star.trail[i],
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }
    
    // 流星头部
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = star.life),
                Color(0xFF87CEEB).copy(alpha = star.life * 0.5f),
                Color.Transparent
            )
        ),
        radius = 8f,
        center = Offset(star.x, star.y)
    )
}

private fun DrawScope.drawGlowParticleEnhanced(particle: GlowParticle, frame: Long) {
    val pulse = (sin(particle.phase) + 1) / 2
    val currentAlpha = (particle.alpha * (0.7f + pulse * 0.3f)).coerceIn(0f, 1f)
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                particle.color.copy(alpha = currentAlpha),
                particle.color.copy(alpha = currentAlpha * 0.3f),
                Color.Transparent
            )
        ),
        radius = particle.size * (0.9f + pulse * 0.2f),
        center = Offset(particle.x, particle.y)
    )
}



// ==================== 翡翠森林 - Emerald ====================

/**
 * 翡翠主题 - 终极版
 * 特性：
 * - 宝石光泽效果
 * - 水晶粒子
 * - 光线折射
 * - 翡翠色渐变
 * - 闪烁光点
 * - 触摸产生宝石碎片
 */
@Composable
fun EmeraldEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "emerald")
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    
    var crystals by remember { mutableStateOf(listOf<EmeraldCrystal>()) }
    var sparkles by remember { mutableStateOf(listOf<EmeraldSparkle>()) }
    var lightRays by remember { mutableStateOf(listOf<LightRay>()) }
    var touchEffects by remember { mutableStateOf(listOf<CrystalBurst>()) }
    
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = tiltX * 0.9f + event.values[0] * 0.1f
                tiltY = tiltY * 0.9f + event.values[1] * 0.1f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            crystals = List(12) { createEmeraldCrystal(canvasSize) }
            sparkles = List(25) { createEmeraldSparkle(canvasSize) }
            lightRays = List(5) { createLightRay(canvasSize, it) }
            
            while (true) {
                frame++
                
                crystals = crystals.map { updateEmeraldCrystal(it, tiltX, tiltY, frame) }
                sparkles = sparkles.map { updateEmeraldSparkle(it, canvasSize, frame) }
                
                touchEffects = touchEffects.mapNotNull { effect ->
                    val newEffect = effect.copy(
                        radius = effect.radius + 5f,
                        alpha = effect.alpha - 0.03f,
                        rotation = effect.rotation + 2f
                    )
                    if (newEffect.alpha > 0) newEffect else null
                }
                
                delay(16)
            }
        }
    }
    
    // Response增强触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                val offset = event.position
                haptic.performHaptic(HapticType.IMPACT_LIGHT)
                onInteraction()
                
                touchEffects = touchEffects + CrystalBurst(offset.x, offset.y, 0f, 1f, 0f)
                
                // 添加新闪光
                val newSparkles = List(8) {
                    EmeraldSparkle(
                        x = offset.x + (Random.nextFloat() - 0.5f) * 80f,
                        y = offset.y + (Random.nextFloat() - 0.5f) * 80f,
                        size = Random.nextFloat() * 6f + 3f,
                        phase = Random.nextFloat() * PI.toFloat() * 2,
                        speed = Random.nextFloat() * 0.1f + 0.05f,
                        alpha = 1f,
                        color = emeraldColors.random()
                    )
                }
                sparkles = (sparkles + newSparkles).takeLast(100)
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 翡翠渐变背景
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF004D40),
                    Color(0xFF00695C),
                    Color(0xFF00897B),
                    Color(0xFF26A69A)
                )
            )
        )
        
        // 宝石纹理
        drawEmeraldTexture(width, height, shimmer, tiltX, tiltY)
        
        // 光线折射
        lightRays.forEach { ray ->
            drawLightRayEnhanced(ray, shimmer, tiltX)
        }
        
        // 水晶
        crystals.forEach { crystal ->
            drawEmeraldCrystalEnhanced(crystal, shimmer, pulse)
        }
        
        // 闪光点
        sparkles.forEach { sparkle ->
            drawEmeraldSparkleEnhanced(sparkle, frame)
        }
        
        // 触摸效果
        touchEffects.forEach { effect ->
            drawCrystalBurstEffect(effect)
        }
        
        // 顶部高光
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.1f + sin(shimmer) * 0.05f),
                    Color.Transparent
                ),
                endY = height * 0.3f
            )
        )
    }
}


// Emerald 数据类
data class EmeraldCrystal(
    var x: Float, var y: Float,
    var size: Float, var rotation: Float,
    var rotationSpeed: Float, var alpha: Float,
    var facets: Int, var color: Color
)

data class EmeraldSparkle(
    var x: Float, var y: Float,
    var size: Float, var phase: Float,
    var speed: Float, var alpha: Float,
    var color: Color
)

data class LightRay(
    val startX: Float, val startY: Float,
    val angle: Float, val width: Float,
    val length: Float, val color: Color
)

data class CrystalBurst(
    val x: Float, val y: Float,
    var radius: Float, var alpha: Float,
    var rotation: Float
)

private val emeraldColors = listOf(
    Color(0xFF00E676), Color(0xFF69F0AE), Color(0xFFA5D6A7),
    Color(0xFF80CBC4), Color(0xFF4DB6AC), Color(0xFF26A69A)
)

private fun createEmeraldCrystal(canvasSize: Size): EmeraldCrystal {
    return EmeraldCrystal(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        size = Random.nextFloat() * 40f + 20f,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 20f,
        alpha = Random.nextFloat() * 0.3f + 0.1f,
        facets = Random.nextInt(4) + 4,
        color = emeraldColors.random()
    )
}

private fun createEmeraldSparkle(canvasSize: Size): EmeraldSparkle {
    return EmeraldSparkle(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        size = Random.nextFloat() * 5f + 2f,
        phase = Random.nextFloat() * PI.toFloat() * 2,
        speed = Random.nextFloat() * 0.08f + 0.02f,
        alpha = Random.nextFloat() * 0.6f + 0.3f,
        color = emeraldColors.random()
    )
}

private fun createLightRay(canvasSize: Size, index: Int): LightRay {
    return LightRay(
        startX = canvasSize.width * (0.1f + index * 0.1f),
        startY = -50f,
        angle = PI.toFloat() / 4 + (Random.nextFloat() - 0.5f) * 0.3f,
        width = Random.nextFloat() * 80f + 40f,
        length = canvasSize.height * 1.5f,
        color = emeraldColors.random()
    )
}

private fun updateEmeraldCrystal(
    crystal: EmeraldCrystal, tiltX: Float, tiltY: Float, frame: Long
): EmeraldCrystal {
    return crystal.copy(
        rotation = crystal.rotation + crystal.rotationSpeed * 0.016f,
        alpha = (crystal.alpha + sin(frame * 0.02f + crystal.x * 0.01f) * 0.02f).coerceIn(0.08f, 0.4f)
    )
}

private fun updateEmeraldSparkle(
    sparkle: EmeraldSparkle, canvasSize: Size, frame: Long
): EmeraldSparkle {
    var newSparkle = sparkle.copy(
        phase = sparkle.phase + sparkle.speed,
        alpha = (sparkle.alpha + sin(frame * 0.03f + sparkle.phase) * 0.03f).coerceIn(0.2f, 0.9f)
    )
    return newSparkle
}


// Emerald 绘制函数
private fun DrawScope.drawEmeraldTexture(
    width: Float, height: Float, shimmer: Float,
    tiltX: Float, tiltY: Float
) {
    // 宝石内部纹理
    val lineCount = 15
    for (i in 0 until lineCount) {
        val baseY = height * i / lineCount
        val offset = sin(shimmer + i * 0.3f) * 20f + tiltX * 5f
        val alpha = 0.05f + sin(shimmer * 2 + i) * 0.02f
        
        drawLine(
            color = Color.White.copy(alpha = alpha.coerceIn(0f, 0.1f)),
            start = Offset(-50f + offset, baseY),
            end = Offset(width + 50f + offset, baseY + height * 0.1f),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawLightRayEnhanced(ray: LightRay, shimmer: Float, tiltX: Float) {
    val adjustedAngle = ray.angle + tiltX * 0.02f
    val shimmerAlpha = 0.08f + sin(shimmer + ray.startX * 0.01f) * 0.03f
    
    val path = Path().apply {
        moveTo(ray.startX, ray.startY)
        lineTo(ray.startX + cos(adjustedAngle - 0.05f) * ray.length, 
               ray.startY + sin(adjustedAngle - 0.05f) * ray.length)
        lineTo(ray.startX + ray.width + cos(adjustedAngle + 0.05f) * ray.length,
               ray.startY + sin(adjustedAngle + 0.05f) * ray.length)
        lineTo(ray.startX + ray.width, ray.startY)
        close()
    }
    
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(
                ray.color.copy(alpha = shimmerAlpha.coerceIn(0f, 0.15f)),
                ray.color.copy(alpha = shimmerAlpha * 0.5f),
                Color.Transparent
            ),
            start = Offset(ray.startX, ray.startY),
            end = Offset(ray.startX + cos(adjustedAngle) * ray.length,
                        ray.startY + sin(adjustedAngle) * ray.length)
        )
    )
}

private fun DrawScope.drawEmeraldCrystalEnhanced(
    crystal: EmeraldCrystal, shimmer: Float, pulse: Float
) {
    rotate(crystal.rotation, pivot = Offset(crystal.x, crystal.y)) {
        val currentAlpha = (crystal.alpha * (0.8f + pulse * 0.4f)).coerceIn(0f, 1f)
        
        // 水晶外发光
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    crystal.color.copy(alpha = currentAlpha * 0.3f),
                    Color.Transparent
                )
            ),
            radius = crystal.size * 1.5f,
            center = Offset(crystal.x, crystal.y)
        )
        
        // 水晶多边形
        val path = Path().apply {
            for (i in 0 until crystal.facets) {
                val angle = (i.toFloat() / crystal.facets) * 2 * PI.toFloat()
                val radius = crystal.size * (0.8f + sin(shimmer + i) * 0.2f)
                val px = crystal.x + cos(angle) * radius
                val py = crystal.y + sin(angle) * radius
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    crystal.color.copy(alpha = currentAlpha),
                    crystal.color.copy(alpha = currentAlpha * 0.5f)
                ),
                center = Offset(crystal.x, crystal.y)
            )
        )
        
        drawPath(
            path = path,
            color = Color.White.copy(alpha = currentAlpha * 0.3f),
            style = Stroke(width = 1.5f)
        )
        
        // 高光点
        drawCircle(
            color = Color.White.copy(alpha = currentAlpha * 0.5f),
            radius = crystal.size * 0.15f,
            center = Offset(crystal.x - crystal.size * 0.2f, crystal.y - crystal.size * 0.2f)
        )
    }
}


private fun DrawScope.drawEmeraldSparkleEnhanced(sparkle: EmeraldSparkle, frame: Long) {
    val twinkle = (sin(sparkle.phase) + 1) / 2
    val currentAlpha = (sparkle.alpha * twinkle).coerceIn(0f, 1f)
    val currentSize = sparkle.size * (0.6f + twinkle * 0.8f)
    
    // 光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                sparkle.color.copy(alpha = currentAlpha * 0.5f),
                Color.Transparent
            )
        ),
        radius = currentSize * 3f,
        center = Offset(sparkle.x, sparkle.y)
    )
    
    // 十字光芒
    if (twinkle > 0.5f) {
        val rayLength = currentSize * 2.5f
        drawLine(
            color = Color.White.copy(alpha = currentAlpha * 0.6f),
            start = Offset(sparkle.x - rayLength, sparkle.y),
            end = Offset(sparkle.x + rayLength, sparkle.y),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.White.copy(alpha = currentAlpha * 0.6f),
            start = Offset(sparkle.x, sparkle.y - rayLength),
            end = Offset(sparkle.x, sparkle.y + rayLength),
            strokeWidth = 1f
        )
    }
    
    // 核心
    drawCircle(
        color = Color.White.copy(alpha = currentAlpha),
        radius = currentSize * 0.5f,
        center = Offset(sparkle.x, sparkle.y)
    )
}

private fun DrawScope.drawCrystalBurstEffect(effect: CrystalBurst) {
    rotate(effect.rotation, pivot = Offset(effect.x, effect.y)) {
        // 六边形扩散
        val path = Path().apply {
            for (i in 0 until 6) {
                val angle = (i.toFloat() / 6) * 2 * PI.toFloat()
                val px = effect.x + cos(angle) * effect.radius
                val py = effect.y + sin(angle) * effect.radius
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        
        drawPath(
            path = path,
            color = Color(0xFF00E676).copy(alpha = effect.alpha * 0.4f),
            style = Stroke(width = 3f)
        )
        
        // 内层
        val innerPath = Path().apply {
            for (i in 0 until 6) {
                val angle = (i.toFloat() / 6) * 2 * PI.toFloat() + PI.toFloat() / 6
                val px = effect.x + cos(angle) * effect.radius * 0.6f
                val py = effect.y + sin(angle) * effect.radius * 0.6f
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        
        drawPath(
            path = innerPath,
            color = Color.White.copy(alpha = effect.alpha * 0.3f),
            style = Stroke(width = 2f)
        )
    }
}



// ==================== 玫瑰金 - RoseGold ====================

/**
 * 玫瑰金主题 - 终极版
 * 特性：
 * - 金属光泽渐变
 * - 漂浮金箔粒子
 * - 柔光效果
 * - 优雅波纹
 * - 闪烁星点
 * - 触摸产生金色涟漪
 */
@Composable
fun RoseGoldEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "roseGold")
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    
    var goldFlakes by remember { mutableStateOf(listOf<GoldFlake>()) }
    var softGlows by remember { mutableStateOf(listOf<SoftGlow>()) }
    var elegantRipples by remember { mutableStateOf(listOf<ElegantRipple>()) }
    var starPoints by remember { mutableStateOf(listOf<StarPoint>()) }
    
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )
    
    val wave by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = tiltX * 0.9f + event.values[0] * 0.1f
                tiltY = tiltY * 0.9f + event.values[1] * 0.1f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            goldFlakes = List(35) { createGoldFlake(canvasSize) }
            softGlows = List(8) { createSoftGlow(canvasSize) }
            starPoints = List(20) { createStarPoint(canvasSize) }
            
            while (true) {
                frame++
                
                goldFlakes = goldFlakes.mapNotNull { updateGoldFlake(it, canvasSize, tiltX, tiltY) }
                softGlows = softGlows.map { updateSoftGlow(it, canvasSize, frame) }
                starPoints = starPoints.map { updateStarPoint(it, frame) }
                
                // 补充金箔
                if (goldFlakes.size < 30 && Random.nextFloat() < 0.05f) {
                    goldFlakes = goldFlakes + createGoldFlake(canvasSize)
                }
                
                elegantRipples = elegantRipples.mapNotNull { ripple ->
                    val newRipple = ripple.copy(
                        radius = ripple.radius + 3f,
                        alpha = ripple.alpha - 0.012f
                    )
                    if (newRipple.alpha > 0) newRipple else null
                }
                
                delay(16)
            }
        }
    }
    
    // Response增强触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                val offset = event.position
                haptic.performHaptic(HapticType.SOFT_LANDING)
                onInteraction()
                
                // 优雅涟漪
                elegantRipples = elegantRipples + listOf(
                    ElegantRipple(offset.x, offset.y, 0f, 0.8f),
                    ElegantRipple(offset.x, offset.y, 0f, 0.6f),
                    ElegantRipple(offset.x, offset.y, 0f, 0.4f)
                )
                
                // 金箔爆发
                val newFlakes = List(10) {
                    val angle = Random.nextFloat() * 2 * PI.toFloat()
                    val speed = Random.nextFloat() * 60f + 30f
                    GoldFlake(
                        x = offset.x, y = offset.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed - 30f,
                        size = Random.nextFloat() * 8f + 4f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 180f,
                        alpha = 1f,
                        shimmerPhase = Random.nextFloat() * PI.toFloat() * 2,
                        color = roseGoldColors.random()
                    )
                }
                goldFlakes = (goldFlakes + newFlakes).takeLast(120)
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 玫瑰金渐变背景
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF0F5),
                    Color(0xFFFFE4E1),
                    Color(0xFFFFDAB9),
                    Color(0xFFFFE4C4)
                )
            )
        )
        
        // 金属光泽波纹
        drawMetallicWaves(width, height, wave, shimmer, tiltX)
        
        // 柔光效果
        softGlows.forEach { glow ->
            drawSoftGlowEnhanced(glow, shimmer)
        }
        
        // 金箔粒子
        goldFlakes.forEach { flake ->
            drawGoldFlakeEnhanced(flake, shimmer)
        }
        
        // 星点
        starPoints.forEach { star ->
            drawStarPointEnhanced(star, frame)
        }
        
        // 优雅涟漪
        elegantRipples.forEach { ripple ->
            drawElegantRippleEnhanced(ripple)
        }
        
        // 顶部柔光
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f + sin(shimmer) * 0.05f),
                    Color.Transparent
                ),
                endY = height * 0.25f
            )
        )
    }
}


// RoseGold 数据类
data class GoldFlake(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var rotation: Float,
    var rotationSpeed: Float, var alpha: Float,
    var shimmerPhase: Float, var color: Color
)

data class SoftGlow(
    var x: Float, var y: Float,
    var size: Float, var phase: Float,
    var alpha: Float, var color: Color
)

data class ElegantRipple(
    val x: Float, val y: Float,
    var radius: Float, var alpha: Float
)

data class StarPoint(
    var x: Float, var y: Float,
    var size: Float, var phase: Float,
    var speed: Float, var alpha: Float
)

private val roseGoldColors = listOf(
    Color(0xFFFFB6C1), Color(0xFFFFC0CB), Color(0xFFFFDAB9),
    Color(0xFFFFE4B5), Color(0xFFF5DEB3), Color(0xFFFFD700)
)

private fun createGoldFlake(canvasSize: Size): GoldFlake {
    return GoldFlake(
        x = Random.nextFloat() * canvasSize.width,
        y = -Random.nextFloat() * 50f - 10f,
        vx = (Random.nextFloat() - 0.5f) * 15f,
        vy = Random.nextFloat() * 25f + 15f,
        size = Random.nextFloat() * 6f + 3f,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 90f,
        alpha = Random.nextFloat() * 0.5f + 0.5f,
        shimmerPhase = Random.nextFloat() * PI.toFloat() * 2,
        color = roseGoldColors.random()
    )
}

private fun createSoftGlow(canvasSize: Size): SoftGlow {
    return SoftGlow(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        size = Random.nextFloat() * 150f + 80f,
        phase = Random.nextFloat() * PI.toFloat() * 2,
        alpha = Random.nextFloat() * 0.15f + 0.05f,
        color = roseGoldColors.random()
    )
}

private fun createStarPoint(canvasSize: Size): StarPoint {
    return StarPoint(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        size = Random.nextFloat() * 4f + 2f,
        phase = Random.nextFloat() * PI.toFloat() * 2,
        speed = Random.nextFloat() * 0.08f + 0.02f,
        alpha = Random.nextFloat() * 0.6f + 0.3f
    )
}

private fun updateGoldFlake(
    flake: GoldFlake, canvasSize: Size, tiltX: Float, tiltY: Float
): GoldFlake? {
    val gravity = 20f
    val sway = sin(flake.shimmerPhase) * 20f
    
    val newFlake = flake.copy(
        x = flake.x + flake.vx * 0.016f + sway * 0.016f + tiltX * 2f,
        y = flake.y + (flake.vy + gravity * 0.016f) * 0.016f,
        vx = flake.vx * 0.995f,
        vy = (flake.vy + gravity * 0.016f).coerceAtMost(60f),
        rotation = flake.rotation + flake.rotationSpeed * 0.016f,
        shimmerPhase = flake.shimmerPhase + 0.05f,
        alpha = flake.alpha - 0.001f
    )
    
    return if (newFlake.y > canvasSize.height + 30 || newFlake.alpha <= 0) null else newFlake
}

private fun updateSoftGlow(glow: SoftGlow, canvasSize: Size, frame: Long): SoftGlow {
    return glow.copy(
        phase = glow.phase + 0.01f,
        alpha = (glow.alpha + sin(frame * 0.01f + glow.phase) * 0.01f).coerceIn(0.03f, 0.2f)
    )
}

private fun updateStarPoint(star: StarPoint, frame: Long): StarPoint {
    return star.copy(
        phase = star.phase + star.speed,
        alpha = (star.alpha + sin(frame * 0.02f + star.phase) * 0.02f).coerceIn(0.2f, 0.9f)
    )
}


// RoseGold 绘制函数
private fun DrawScope.drawMetallicWaves(
    width: Float, height: Float, wave: Float, shimmer: Float, tiltX: Float
) {
    val waveCount = 5
    for (i in 0 until waveCount) {
        val baseY = height * (0.3f + i * 0.15f)
        val waveOffset = sin(wave * 2 * PI.toFloat() + i * 0.5f) * 30f
        val alpha = (0.06f - i * 0.008f + sin(shimmer + i) * 0.02f).coerceIn(0.02f, 0.1f)
        
        val path = Path().apply {
            moveTo(-50f, baseY + waveOffset)
            var x = -50f
            while (x < width + 100) {
                val y = baseY + sin((x + wave * width) * 0.02f + i) * 25f + waveOffset + tiltX * 5f
                lineTo(x, y)
                x += 20f
            }
            lineTo(width + 100, height)
            lineTo(-50f, height)
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = alpha),
                    Color(0xFFFFB6C1).copy(alpha = alpha * 0.5f),
                    Color.Transparent
                ),
                startY = baseY - 50,
                endY = baseY + 100
            )
        )
    }
}

private fun DrawScope.drawSoftGlowEnhanced(glow: SoftGlow, shimmer: Float) {
    val pulse = (sin(glow.phase) + 1) / 2
    val currentAlpha = (glow.alpha * (0.7f + pulse * 0.5f)).coerceIn(0f, 0.25f)
    val currentSize = glow.size * (0.9f + pulse * 0.2f)
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glow.color.copy(alpha = currentAlpha),
                glow.color.copy(alpha = currentAlpha * 0.3f),
                Color.Transparent
            )
        ),
        radius = currentSize,
        center = Offset(glow.x, glow.y)
    )
}

private fun DrawScope.drawGoldFlakeEnhanced(flake: GoldFlake, shimmer: Float) {
    val shimmerEffect = (sin(flake.shimmerPhase) + 1) / 2
    val currentAlpha = (flake.alpha * (0.6f + shimmerEffect * 0.4f)).coerceIn(0f, 1f)
    
    rotate(flake.rotation, pivot = Offset(flake.x, flake.y)) {
        // 金箔光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    flake.color.copy(alpha = currentAlpha * 0.4f),
                    Color.Transparent
                )
            ),
            radius = flake.size * 2.5f,
            center = Offset(flake.x, flake.y)
        )
        
        // 金箔主体（菱形）
        val path = Path().apply {
            moveTo(flake.x, flake.y - flake.size)
            lineTo(flake.x + flake.size * 0.6f, flake.y)
            lineTo(flake.x, flake.y + flake.size)
            lineTo(flake.x - flake.size * 0.6f, flake.y)
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = currentAlpha),
                    flake.color.copy(alpha = currentAlpha),
                    Color(0xFFFFE4B5).copy(alpha = currentAlpha)
                ),
                start = Offset(flake.x - flake.size, flake.y - flake.size),
                end = Offset(flake.x + flake.size, flake.y + flake.size)
            )
        )
        
        // 高光
        if (shimmerEffect > 0.6f) {
            drawCircle(
                color = Color.White.copy(alpha = currentAlpha * 0.6f),
                radius = flake.size * 0.2f,
                center = Offset(flake.x - flake.size * 0.15f, flake.y - flake.size * 0.3f)
            )
        }
    }
}

private fun DrawScope.drawStarPointEnhanced(star: StarPoint, frame: Long) {
    val twinkle = (sin(star.phase) + 1) / 2
    val currentAlpha = (star.alpha * twinkle).coerceIn(0f, 1f)
    val currentSize = star.size * (0.5f + twinkle * 1f)
    
    // 光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = currentAlpha * 0.4f),
                Color.Transparent
            )
        ),
        radius = currentSize * 4f,
        center = Offset(star.x, star.y)
    )
    
    // 十字光芒
    if (twinkle > 0.5f) {
        val rayLength = currentSize * 3f
        val rayAlpha = currentAlpha * 0.5f
        
        drawLine(
            color = Color.White.copy(alpha = rayAlpha),
            start = Offset(star.x - rayLength, star.y),
            end = Offset(star.x + rayLength, star.y),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.White.copy(alpha = rayAlpha),
            start = Offset(star.x, star.y - rayLength),
            end = Offset(star.x, star.y + rayLength),
            strokeWidth = 1f
        )
    }
    
    // 核心
    drawCircle(
        color = Color.White.copy(alpha = currentAlpha),
        radius = currentSize * 0.5f,
        center = Offset(star.x, star.y)
    )
}

private fun DrawScope.drawElegantRippleEnhanced(ripple: ElegantRipple) {
    // 多层涟漪
    for (i in 0 until 3) {
        val layerRadius = ripple.radius - i * 15f
        if (layerRadius > 0) {
            val layerAlpha = (ripple.alpha * (1f - i * 0.25f)).coerceIn(0f, 1f)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFD700).copy(alpha = layerAlpha * 0.3f),
                        Color(0xFFFFB6C1).copy(alpha = layerAlpha * 0.2f),
                        Color.Transparent
                    )
                ),
                radius = layerRadius,
                center = Offset(ripple.x, ripple.y)
            )
            
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = layerAlpha * 0.4f),
                radius = layerRadius,
                center = Offset(ripple.x, ripple.y),
                style = Stroke(width = 2f)
            )
        }
    }
}
