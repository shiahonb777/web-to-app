package com.webtoapp.ui.theme.enhanced

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * ============================================================
 * 强化版主题背景系统 - 深度定制版
 * 每个主题都有独特的沉浸式视觉效果、物理模拟和交互
 * ============================================================
 */

// ==================== 触觉反馈工具 ====================

private fun vibrate(context: Context, duration: Long = 20L) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    } catch (e: Exception) { /* 忽略 */ }
}

// ==================== 柏林噪声实现 ====================

private class PerlinNoise(seed: Long = System.currentTimeMillis()) {
    private val permutation = IntArray(512)
    
    init {
        val p = (0..255).shuffled(Random(seed)).toIntArray()
        for (i in 0..255) {
            permutation[i] = p[i]
            permutation[256 + i] = p[i]
        }
    }
    
    private fun fade(t: Float): Float = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)
    private fun grad(hash: Int, x: Float, y: Float): Float {
        val h = hash and 3
        val u = if (h < 2) x else y
        val v = if (h < 2) y else x
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }
    
    fun noise(x: Float, y: Float): Float {
        val xi = x.toInt() and 255
        val yi = y.toInt() and 255
        val xf = x - x.toInt()
        val yf = y - y.toInt()
        val u = fade(xf)
        val v = fade(yf)
        val aa = permutation[permutation[xi] + yi]
        val ab = permutation[permutation[xi] + yi + 1]
        val ba = permutation[permutation[xi + 1] + yi]
        val bb = permutation[permutation[xi + 1] + yi + 1]
        return lerp(v,
            lerp(u, grad(aa, xf, yf), grad(ba, xf - 1, yf)),
            lerp(u, grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1))
        )
    }
    
    fun fbm(x: Float, y: Float, octaves: Int = 4): Float {
        var value = 0f
        var amplitude = 0.5f
        var frequency = 1f
        for (i in 0 until octaves) {
            value += amplitude * noise(x * frequency, y * frequency)
            amplitude *= 0.5f
            frequency *= 2f
        }
        return value
    }
}

// ==================== 极光梦境 - Aurora ====================

/**
 * 极光流体背景 - 深度优化版
 * 多层极光流体 + 3D星空 + 流星 + 北极光粒子
 */
@Composable
fun AuroraEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var auroraParticles by remember { mutableStateOf(listOf<AuroraParticle>()) }
    var shootingStars by remember { mutableStateOf(listOf<ShootingStar>()) }
    
    // Listen从 EnhancedThemeWrapper 传递的触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // Response触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                touchPoint = event.position
                vibrate(context, 15)
                // 点击生成极光爆发
                auroraParticles = auroraParticles + List(8) {
                    AuroraParticle(
                        x = event.position.x, y = event.position.y,
                        vx = (Random.nextFloat() - 0.5f) * 8f,
                        vy = -Random.nextFloat() * 6f - 2f,
                        size = Random.nextFloat() * 6f + 3f,
                        alpha = 1f,
                        color = listOf(
                            Color(0xFF7B68EE), Color(0xFF00CED1),
                            Color(0xFFFF69B4), Color(0xFF00FF7F)
                        ).random(),
                        glowIntensity = Random.nextFloat() * 0.5f + 0.5f
                    )
                }
            }
        }
    }
    
    // 重力感应
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    
    // 传感器 - 使用 SENSOR_DELAY_UI 降低更新频率以提升性能
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = event.values[0] * 0.1f
                tiltY = event.values[1] * 0.1f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    // 多层极光相位
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase3"
    )
    
    // 动画循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            auroraParticles = List(35) { createAuroraParticle(canvasSize) }
            while (true) {
                delay(16)
                frame++
                
                // Update极光粒子
                auroraParticles = auroraParticles.map { p ->
                    updateAuroraParticle(p, canvasSize, tiltX, tiltY, touchPoint)
                }
                
                // Shuffle生成流星
                if (Random.nextFloat() < 0.008f && shootingStars.size < 3) {
                    shootingStars = shootingStars + createShootingStar(canvasSize)
                }
                
                // Update流星
                shootingStars = shootingStars.mapNotNull { star ->
                    val newStar = star.copy(
                        x = star.x + star.vx,
                        y = star.y + star.vy,
                        life = star.life - 0.015f,
                        trail = star.trail + Offset(star.x, star.y)
                    )
                    if (newStar.life > 0) newStar else null
                }
                
                // 清除触摸点
                if (touchPoint != null && frame % 60 == 0L) touchPoint = null
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 深空背景渐变
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF050510),
                    Color(0xFF0D0B1A),
                    Color(0xFF1A0A2E),
                    Color(0xFF0D1B2A)
                )
            )
        )
        
        // 绘制3D星空（多层视差）
        drawStarfield(width, height, frame, tiltX, tiltY)
        
        // 绘制多层极光
        drawAuroraLayers(width, height, phase1, phase2, phase3, tiltX)
        
        // 绘制极光粒子
        auroraParticles.forEach { particle ->
            drawAuroraParticle(particle)
        }
        
        // 绘制流星
        shootingStars.forEach { star ->
            drawShootingStarWithTrail(star)
        }
        
        // 绘制极光反射（底部）
        drawAuroraReflection(width, height, phase1)
    }
}

data class AuroraParticle(
    var x: Float, var y: Float,
    var vx: Float = 0f, var vy: Float = 0f,
    var size: Float, var alpha: Float = 1f,
    var color: Color = Color.White,
    var glowIntensity: Float = 1f
)

data class ShootingStar(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var length: Float, var life: Float,
    var trail: List<Offset> = emptyList()
)

private fun createAuroraParticle(canvasSize: Size): AuroraParticle {
    return AuroraParticle(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height * 0.6f,
        vx = (Random.nextFloat() - 0.5f) * 0.5f,
        vy = (Random.nextFloat() - 0.5f) * 0.3f,
        size = Random.nextFloat() * 4f + 2f,
        alpha = Random.nextFloat() * 0.5f + 0.3f,
        color = listOf(
            Color(0xFF7B68EE), Color(0xFF00CED1),
            Color(0xFFFF69B4), Color(0xFF00FF7F), Color(0xFF9370DB)
        ).random(),
        glowIntensity = Random.nextFloat() * 0.5f + 0.5f
    )
}

private fun updateAuroraParticle(
    p: AuroraParticle, canvasSize: Size,
    tiltX: Float, tiltY: Float, touchPoint: Offset?
): AuroraParticle {
    var newP = p.copy(
        x = p.x + p.vx + tiltX * 0.5f,
        y = p.y + p.vy + sin(p.x * 0.01f) * 0.3f,
        alpha = (p.alpha + (Random.nextFloat() - 0.5f) * 0.02f).coerceIn(0.2f, 0.8f)
    )
    
    // 触摸吸引
    touchPoint?.let { touch ->
        val dx = touch.x - newP.x
        val dy = touch.y - newP.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 200 && dist > 10) {
            newP = newP.copy(
                vx = newP.vx + dx / dist * 0.3f,
                vy = newP.vy + dy / dist * 0.3f
            )
        }
    }
    
    // 边界处理
    if (newP.x < -20) newP = newP.copy(x = canvasSize.width + 20)
    if (newP.x > canvasSize.width + 20) newP = newP.copy(x = -20f)
    if (newP.y < -20 || newP.y > canvasSize.height * 0.7f) {
        newP = createAuroraParticle(canvasSize)
    }
    
    return newP
}

private fun createShootingStar(canvasSize: Size): ShootingStar {
    val startX = Random.nextFloat() * canvasSize.width * 0.7f
    return ShootingStar(
        x = startX, y = Random.nextFloat() * canvasSize.height * 0.3f,
        vx = Random.nextFloat() * 12f + 8f,
        vy = Random.nextFloat() * 8f + 4f,
        length = Random.nextFloat() * 60f + 40f,
        life = 1f
    )
}

private fun DrawScope.drawStarfield(
    width: Float, height: Float, frame: Long,
    tiltX: Float, tiltY: Float
) {
    // 减少星星数量以提升性能
    val layers = listOf(
        Triple(60, 0.3f, 1.5f),   // 远景星星
        Triple(30, 0.6f, 2.5f),   // 中景星星
        Triple(15, 1f, 4f)        // 近景星星
    )
    
    layers.forEachIndexed { layerIndex, (count, parallax, maxSize) ->
        val random = Random(42 + layerIndex)
        repeat(count) {
            val baseX = random.nextFloat() * width * 1.2f - width * 0.1f
            val baseY = random.nextFloat() * height * 0.7f
            val starX = baseX + tiltX * parallax * 10
            val starY = baseY + tiltY * parallax * 10
            val starSize = random.nextFloat() * maxSize + 0.5f
            // 简化闪烁计算
            val twinkle = (sin(frame * 0.03f + layerIndex * 100 + it) + 1) / 2
            val alpha = (0.4f + twinkle * 0.6f) * parallax
            
            // 只为最大的星星绘制光晕
            if (starSize > 3f) {
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.25f),
                    radius = starSize * 2,
                    center = Offset(starX, starY)
                )
            }
            
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = starSize,
                center = Offset(starX, starY)
            )
        }
    }
}

private fun DrawScope.drawAuroraLayers(
    width: Float, height: Float,
    phase1: Float, phase2: Float, phase3: Float,
    tiltX: Float
) {
    val auroraConfigs = listOf(
        Triple(Color(0xFF7B68EE), 0.25f, phase1),
        Triple(Color(0xFF00CED1), 0.35f, phase2),
        Triple(Color(0xFFFF69B4), 0.3f, phase3),
        Triple(Color(0xFF00FF7F), 0.4f, phase1 * 0.7f)
    )
    
    auroraConfigs.forEachIndexed { index, (color, baseY, phase) ->
        val path = Path()
        val segments = 60
        val layerOffset = tiltX * (index + 1) * 5
        
        for (i in 0..segments) {
            val x = width * i / segments
            val noise1 = sin(x / width * 3 * PI.toFloat() + phase) * height * 0.08f
            val noise2 = sin(x / width * 7 * PI.toFloat() + phase * 1.3f) * height * 0.04f
            val noise3 = sin(x / width * 11 * PI.toFloat() + phase * 0.8f) * height * 0.02f
            val y = height * baseY + noise1 + noise2 + noise3 + layerOffset
            
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
        
        // 极光渐变
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.4f - index * 0.05f),
                    color.copy(alpha = 0.2f - index * 0.03f),
                    color.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                startY = height * baseY - 50,
                endY = height * 0.8f
            )
        )
        
        // 极光边缘发光
        drawPath(
            path = path,
            color = color.copy(alpha = 0.6f - index * 0.1f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawAuroraParticle(particle: AuroraParticle) {
    // 粒子光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                particle.color.copy(alpha = particle.alpha * particle.glowIntensity),
                particle.color.copy(alpha = particle.alpha * 0.3f),
                Color.Transparent
            )
        ),
        radius = particle.size * 4,
        center = Offset(particle.x, particle.y)
    )
    
    // 粒子核心
    drawCircle(
        color = Color.White.copy(alpha = particle.alpha),
        radius = particle.size * 0.5f,
        center = Offset(particle.x, particle.y)
    )
}

private fun DrawScope.drawShootingStarWithTrail(star: ShootingStar) {
    val angle = atan2(star.vy, star.vx)
    
    // 绘制尾迹
    if (star.trail.size > 1) {
        val trailPath = Path()
        star.trail.takeLast(15).forEachIndexed { index, point ->
            if (index == 0) trailPath.moveTo(point.x, point.y)
            else trailPath.lineTo(point.x, point.y)
        }
        
        drawPath(
            path = trailPath,
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = star.life * 0.5f)),
                start = star.trail.first(),
                end = star.trail.last()
            ),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
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

private fun DrawScope.drawAuroraReflection(width: Float, height: Float, phase: Float) {
    val reflectionY = height * 0.85f
    
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF7B68EE).copy(alpha = 0.1f + sin(phase) * 0.05f),
                Color(0xFF00CED1).copy(alpha = 0.08f),
                Color.Transparent
            ),
            startY = reflectionY,
            endY = height
        ),
        topLeft = Offset(0f, reflectionY),
        size = Size(width, height - reflectionY)
    )
}


// ==================== 樱花物语 - Sakura ====================

/**
 * 樱花物理背景 - 深度优化版
 * 真实物理樱花飘落 + 重力感应 + 风力模拟 + 樱花树 + 花瓣爆发 + 触觉反馈
 */
@Composable
fun SakuraEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    enableSound: Boolean = true,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    var petals by remember { mutableStateOf(listOf<SakuraPetal>()) }
    var fallingPetals by remember { mutableStateOf(listOf<FallingPetal>()) }
    var windForce by remember { mutableFloatStateOf(0f) }
    var windDirection by remember { mutableFloatStateOf(1f) }
    
    // Listen从 EnhancedThemeWrapper 传递的触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // Response触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                vibrate(context, 25)
                onInteraction()
                // 点击爆发花瓣
                val offset = event.position
                petals = petals.map { petal ->
                    val dx = petal.x - offset.x
                    val dy = petal.y - offset.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 180) {
                        val force = (180 - dist) / 180 * 18
                        val angle = atan2(dy, dx)
                        petal.copy(
                            vx = petal.vx + cos(angle) * force,
                            vy = petal.vy + sin(angle) * force - 5f,
                            rotationSpeed = petal.rotationSpeed + (Random.nextFloat() - 0.5f) * 15
                        )
                    } else petal
                }
                // 添加爆发花瓣
                petals = petals + List(15) {
                    val angle = Random.nextFloat() * 2 * PI.toFloat()
                    val speed = Random.nextFloat() * 10 + 5
                    SakuraPetal(
                        x = offset.x, y = offset.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed - 8f,
                        size = Random.nextFloat() * 14f + 8f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 12f,
                        swayPhase = Random.nextFloat() * PI.toFloat() * 2,
                        colorIndex = Random.nextInt(5),
                        alpha = 1f,
                        depth = Random.nextFloat()
                    )
                }
            }
        }
    }
    
    // 重力感应
    var gravityX by remember { mutableFloatStateOf(0f) }
    var gravityY by remember { mutableFloatStateOf(9.8f) }
    var shakeIntensity by remember { mutableFloatStateOf(0f) }
    
    // 传感器
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeTime = 0L
        var lastX = 0f; var lastY = 0f; var lastZ = 0f
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                gravityX = -event.values[0] * 0.8f
                gravityY = event.values[1] * 0.8f + 5f
                
                // 摇晃检测
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 100) {
                    val deltaX = abs(event.values[0] - lastX)
                    val deltaY = abs(event.values[1] - lastY)
                    val deltaZ = abs(event.values[2] - lastZ)
                    val acceleration = deltaX + deltaY + deltaZ
                    
                    if (acceleration > 15) {
                        shakeIntensity = (acceleration - 15).coerceAtMost(25f)
                        vibrate(context, 30)
                        // 摇晃时从树上掉落更多花瓣
                        if (canvasSize.width > 0) {
                            fallingPetals = fallingPetals + List((shakeIntensity * 2).toInt()) {
                                createTreePetal(canvasSize)
                            }
                        }
                    }
                    
                    lastX = event.values[0]
                    lastY = event.values[1]
                    lastZ = event.values[2]
                    lastShakeTime = currentTime
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    // 动画循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            petals = List(25) { createSakuraPetal(canvasSize) }
            fallingPetals = List(8) { createTreePetal(canvasSize) }
            
            while (true) {
                delay(16)
                frame++
                
                // 风力变化
                if (frame % 120 == 0L) {
                    windForce = Random.nextFloat() * 3f - 1f
                    if (Random.nextFloat() < 0.1f) windDirection = -windDirection
                }
                
                // Update花瓣
                petals = petals.map { petal ->
                    updateSakuraPetal(petal, canvasSize, gravityX, gravityY, windForce, windDirection, shakeIntensity)
                }
                
                // Update树上掉落的花瓣
                fallingPetals = fallingPetals.mapNotNull { petal ->
                    val updated = updateFallingPetal(petal, canvasSize, gravityX, gravityY, windForce)
                    if (updated.y < canvasSize.height + 50) updated else null
                }
                
                // 补充花瓣
                if (fallingPetals.size < 20 && Random.nextFloat() < 0.1f) {
                    fallingPetals = fallingPetals + createTreePetal(canvasSize)
                }
                
                // 衰减摇晃
                if (shakeIntensity > 0) shakeIntensity *= 0.92f
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 樱花背景渐变
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF5F8),
                    Color(0xFFFFE8ED),
                    Color(0xFFFFF0F5),
                    Color(0xFFFFE4EC)
                )
            )
        )
        
        // 绘制远景山脉
        drawDistantMountains(width, height)
        
        // 绘制樱花树
        drawSakuraTree(width, height, frame, windForce)
        
        // 绘制花瓣（按深度排序）
        val allPetals = (petals + fallingPetals.map { 
            SakuraPetal(it.x, it.y, it.vx, it.vy, it.size, it.rotation, 
                it.rotationSpeed, it.swayPhase, it.colorIndex, it.alpha, it.depth)
        }).sortedBy { it.depth }
        
        allPetals.forEach { petal ->
            drawSakuraPetalEnhanced(petal)
        }
        
        // 绘制地面花瓣堆积
        drawGroundPetals(width, height, frame)
    }
}

data class SakuraPetal(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var rotation: Float,
    var rotationSpeed: Float, var swayPhase: Float,
    var colorIndex: Int, var alpha: Float = 1f,
    var depth: Float = 0.5f
)

data class FallingPetal(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var rotation: Float,
    var rotationSpeed: Float, var swayPhase: Float,
    var colorIndex: Int, var alpha: Float = 1f,
    var depth: Float = 0.5f
)

private val sakuraColors = listOf(
    Color(0xFFFFB7C5), Color(0xFFFFC1E3), Color(0xFFFFE4E9),
    Color(0xFFF8BBD9), Color(0xFFFFCDD2)
)

private fun createSakuraPetal(canvasSize: Size): SakuraPetal {
    return SakuraPetal(
        x = Random.nextFloat() * canvasSize.width * 1.2f - canvasSize.width * 0.1f,
        y = -Random.nextFloat() * canvasSize.height * 0.5f - 50f,
        vx = (Random.nextFloat() - 0.5f) * 2f,
        vy = Random.nextFloat() * 1.5f + 0.5f,
        size = Random.nextFloat() * 12f + 6f,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 4f,
        swayPhase = Random.nextFloat() * PI.toFloat() * 2,
        colorIndex = Random.nextInt(5),
        alpha = Random.nextFloat() * 0.3f + 0.7f,
        depth = Random.nextFloat()
    )
}

private fun createTreePetal(canvasSize: Size): FallingPetal {
    // 从树的位置掉落
    val treeX = canvasSize.width * 0.15f
    val treeTop = canvasSize.height * 0.2f
    return FallingPetal(
        x = treeX + (Random.nextFloat() - 0.3f) * canvasSize.width * 0.25f,
        y = treeTop + Random.nextFloat() * canvasSize.height * 0.2f,
        vx = (Random.nextFloat() - 0.3f) * 3f,
        vy = Random.nextFloat() * 0.5f,
        size = Random.nextFloat() * 14f + 8f,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 5f,
        swayPhase = Random.nextFloat() * PI.toFloat() * 2,
        colorIndex = Random.nextInt(5),
        alpha = 1f,
        depth = Random.nextFloat() * 0.3f + 0.7f
    )
}

private fun updateSakuraPetal(
    petal: SakuraPetal, canvasSize: Size,
    gravityX: Float, gravityY: Float,
    windForce: Float, windDirection: Float,
    shakeIntensity: Float
): SakuraPetal {
    val sway = sin(petal.swayPhase) * 1.5f * (1 + petal.depth)
    var newPetal = petal.copy(
        x = petal.x + petal.vx + sway + windForce * windDirection * petal.depth,
        y = petal.y + petal.vy,
        vx = petal.vx * 0.98f + gravityX * 0.015f + (if (shakeIntensity > 0) (Random.nextFloat() - 0.5f) * shakeIntensity * 0.5f else 0f),
        vy = (petal.vy + gravityY * 0.008f * (0.5f + petal.depth)).coerceAtMost(6f),
        rotation = petal.rotation + petal.rotationSpeed,
        rotationSpeed = petal.rotationSpeed * 0.995f,
        swayPhase = petal.swayPhase + 0.06f * (1 + petal.depth)
    )
    
    // 边界重生
    if (newPetal.y > canvasSize.height + 50 || newPetal.x < -100 || newPetal.x > canvasSize.width + 100) {
        newPetal = createSakuraPetal(canvasSize)
    }
    
    return newPetal
}

private fun updateFallingPetal(
    petal: FallingPetal, canvasSize: Size,
    gravityX: Float, gravityY: Float, windForce: Float
): FallingPetal {
    val sway = sin(petal.swayPhase) * 2f
    return petal.copy(
        x = petal.x + petal.vx + sway + windForce * 0.5f,
        y = petal.y + petal.vy,
        vx = petal.vx * 0.99f + gravityX * 0.01f,
        vy = (petal.vy + gravityY * 0.012f).coerceAtMost(5f),
        rotation = petal.rotation + petal.rotationSpeed,
        swayPhase = petal.swayPhase + 0.08f
    )
}

private fun DrawScope.drawDistantMountains(width: Float, height: Float) {
    // 远山
    val mountainPath = Path().apply {
        moveTo(0f, height * 0.7f)
        quadraticBezierTo(width * 0.2f, height * 0.55f, width * 0.35f, height * 0.65f)
        quadraticBezierTo(width * 0.5f, height * 0.5f, width * 0.7f, height * 0.6f)
        quadraticBezierTo(width * 0.85f, height * 0.52f, width, height * 0.62f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    
    drawPath(
        path = mountainPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8D5E0).copy(alpha = 0.5f),
                Color(0xFFF5E6EB).copy(alpha = 0.3f)
            )
        )
    )
}

private fun DrawScope.drawSakuraTree(width: Float, height: Float, frame: Long, windForce: Float) {
    val treeBaseX = width * 0.12f
    val treeBaseY = height
    val windSway = sin(frame * 0.02f) * 5f + windForce * 3f
    
    // 树干
    val trunkPath = Path().apply {
        moveTo(treeBaseX - 15f, treeBaseY)
        quadraticBezierTo(treeBaseX - 20f, height * 0.7f, treeBaseX - 10f + windSway * 0.3f, height * 0.5f)
        quadraticBezierTo(treeBaseX + windSway * 0.5f, height * 0.35f, treeBaseX + 5f + windSway, height * 0.25f)
        lineTo(treeBaseX + 15f + windSway, height * 0.25f)
        quadraticBezierTo(treeBaseX + 20f + windSway * 0.5f, height * 0.35f, treeBaseX + 25f + windSway * 0.3f, height * 0.5f)
        quadraticBezierTo(treeBaseX + 30f, height * 0.7f, treeBaseX + 25f, treeBaseY)
        close()
    }
    
    drawPath(
        path = trunkPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723))
        )
    )
    
    // 树枝
    val branches = listOf(
        Triple(0.4f, -0.15f, 0.7f),
        Triple(0.35f, 0.2f, 0.6f),
        Triple(0.28f, -0.1f, 0.5f),
        Triple(0.32f, 0.15f, 0.4f)
    )
    
    branches.forEach { (yRatio, xOffset, length) ->
        val branchStartX = treeBaseX + 10f + windSway * (1 - yRatio)
        val branchStartY = height * yRatio
        val branchEndX = branchStartX + width * xOffset * length + windSway * 1.5f
        val branchEndY = branchStartY - height * 0.05f
        
        drawLine(
            color = Color(0xFF5D4037),
            start = Offset(branchStartX, branchStartY),
            end = Offset(branchEndX, branchEndY),
            strokeWidth = 6f * length,
            cap = StrokeCap.Round
        )
    }
    
    // 树冠（樱花团）
    val blossomCenters = listOf(
        Offset(treeBaseX + windSway, height * 0.22f),
        Offset(treeBaseX - 40f + windSway * 0.8f, height * 0.28f),
        Offset(treeBaseX + 50f + windSway * 1.2f, height * 0.26f),
        Offset(treeBaseX + windSway * 0.9f, height * 0.32f),
        Offset(treeBaseX - 60f + windSway * 0.7f, height * 0.35f),
        Offset(treeBaseX + 70f + windSway * 1.1f, height * 0.33f),
        Offset(treeBaseX + 20f + windSway, height * 0.18f)
    )
    
    blossomCenters.forEachIndexed { index, center ->
        val radius = 45f + index * 8f
        val alpha = 0.9f - index * 0.05f
        
        // 外层光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFB7C5).copy(alpha = alpha * 0.6f),
                    Color(0xFFFFC1E3).copy(alpha = alpha * 0.3f),
                    Color.Transparent
                )
            ),
            radius = radius * 1.5f,
            center = center
        )
        
        // 樱花团
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFCDD2).copy(alpha = alpha),
                    Color(0xFFFFB7C5).copy(alpha = alpha * 0.8f),
                    Color(0xFFF8BBD9).copy(alpha = alpha * 0.5f)
                )
            ),
            radius = radius,
            center = center
        )
    }
}

private fun DrawScope.drawSakuraPetalEnhanced(petal: SakuraPetal) {
    val color = sakuraColors[petal.colorIndex]
    val scale = 0.7f + petal.depth * 0.6f
    val adjustedSize = petal.size * scale
    
    rotate(petal.rotation, pivot = Offset(petal.x, petal.y)) {
        // 花瓣阴影
        val shadowOffset = 2f * scale
        drawPetalShape(
            petal.x + shadowOffset, petal.y + shadowOffset,
            adjustedSize, Color.Black.copy(alpha = 0.1f * petal.alpha)
        )
        
        // 花瓣主体
        drawPetalShape(petal.x, petal.y, adjustedSize, color.copy(alpha = petal.alpha))
        
        // 花瓣高光
        drawPetalShape(
            petal.x - adjustedSize * 0.1f, petal.y - adjustedSize * 0.1f,
            adjustedSize * 0.6f, Color.White.copy(alpha = petal.alpha * 0.4f)
        )
        
        // 花瓣纹理线
        drawLine(
            color = Color(0xFFD81B60).copy(alpha = petal.alpha * 0.3f),
            start = Offset(petal.x, petal.y - adjustedSize * 0.3f),
            end = Offset(petal.x, petal.y + adjustedSize * 0.2f),
            strokeWidth = 0.5f
        )
    }
}

private fun DrawScope.drawPetalShape(x: Float, y: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, y - size * 0.5f)
        cubicTo(
            x + size * 0.5f, y - size * 0.3f,
            x + size * 0.4f, y + size * 0.3f,
            x, y + size * 0.4f
        )
        cubicTo(
            x - size * 0.4f, y + size * 0.3f,
            x - size * 0.5f, y - size * 0.3f,
            x, y - size * 0.5f
        )
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawGroundPetals(width: Float, height: Float, frame: Long) {
    val groundY = height * 0.92f
    val random = Random(123)
    
    // 地面渐变
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xFFFFE4EC).copy(alpha = 0.5f)),
            startY = groundY - 30f,
            endY = height
        ),
        topLeft = Offset(0f, groundY - 30f),
        size = Size(width, height - groundY + 30f)
    )
    
    // 散落的花瓣
    repeat(40) {
        val petalX = random.nextFloat() * width
        val petalY = groundY + random.nextFloat() * (height - groundY) * 0.8f
        val petalSize = random.nextFloat() * 8f + 4f
        val rotation = random.nextFloat() * 360f
        val colorIndex = random.nextInt(5)
        val alpha = 0.4f + random.nextFloat() * 0.4f
        
        rotate(rotation, pivot = Offset(petalX, petalY)) {
            drawPetalShape(petalX, petalY, petalSize, sakuraColors[colorIndex].copy(alpha = alpha))
        }
    }
}


// ==================== 深海幽蓝 - Ocean ====================

/**
 * 深海物理水效果背景 - 深度优化版
 * 真实物理水波 + 重力控制水位 + 气泡物理 + 水母 + 光线折射 + 触摸涟漪
 */
@Composable
fun OceanEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    
    // 水波物理
    var wavePoints by remember { mutableStateOf(listOf<WavePoint>()) }
    var waterLevel by remember { mutableFloatStateOf(0.4f) }
    var waterVelocity by remember { mutableFloatStateOf(0f) }
    
    // 粒子系统
    var bubbles by remember { mutableStateOf(listOf<Bubble>()) }
    var jellyfish by remember { mutableStateOf(listOf<Jellyfish>()) }
    var ripples by remember { mutableStateOf(listOf<OceanRipple>()) }
    var caustics by remember { mutableStateOf(listOf<Caustic>()) }
    
    // Listen从 EnhancedThemeWrapper 传递的触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // Response触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0) {
                val offset = event.position
                when (event.type) {
                    EnhancedTouchEvent.TouchType.TAP -> {
                        vibrate(context, 20)
                        onInteraction()
                        // 添加涟漪
                        ripples = ripples + OceanRipple(offset.x, offset.y, 0f, 0.9f)
                        // 扰动波浪
                        wavePoints = wavePoints.map { point ->
                            val dist = abs(point.x - offset.x)
                            if (dist < 120) {
                                point.copy(velocity = point.velocity - (120 - dist) / 120 * 15)
                            } else point
                        }
                        // 添加气泡
                        bubbles = bubbles + List(4) {
                            Bubble(
                                x = offset.x + (Random.nextFloat() - 0.5f) * 60,
                                y = offset.y,
                                vx = (Random.nextFloat() - 0.5f) * 2f,
                                vy = -Random.nextFloat() * 4f - 2f,
                                size = Random.nextFloat() * 12f + 4f,
                                wobblePhase = Random.nextFloat() * PI.toFloat() * 2,
                                alpha = 0.8f
                            )
                        }
                    }
                    EnhancedTouchEvent.TouchType.DRAG -> {
                        // 拖动创建波浪
                        wavePoints = wavePoints.map { point ->
                            val dist = abs(point.x - offset.x)
                            if (dist < 60) {
                                point.copy(velocity = point.velocity + 3f)
                            } else point
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    // 重力感应
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    
    // 传感器
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = event.values[0] / 10f
                tiltY = event.values[1] / 10f
                // 根据倾斜更新水位
                waterVelocity += tiltY * 0.003f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    // 动画循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            // Initialize波浪点
            val pointCount = 60
            wavePoints = List(pointCount) { i ->
                WavePoint(
                    x = canvasSize.width * i / (pointCount - 1),
                    y = canvasSize.height * waterLevel,
                    velocity = 0f
                )
            }
            bubbles = List(18) { createBubble(canvasSize) }
            jellyfish = List(3) { createJellyfish(canvasSize) }
            caustics = List(10) { createCaustic(canvasSize) }
            
            while (true) {
                delay(16)
                frame++
                
                // Update水位
                waterVelocity *= 0.97f
                waterLevel = (waterLevel + waterVelocity).coerceIn(0.3f, 0.5f)
                
                // Update波浪
                wavePoints = updateWavePhysics(wavePoints, canvasSize, tiltX, frame, waterLevel)
                
                // Update气泡
                bubbles = bubbles.map { updateBubblePhysics(it, canvasSize, tiltX, wavePoints) }
                
                // Update水母
                jellyfish = jellyfish.map { updateJellyfish(it, canvasSize, frame) }
                
                // Update焦散
                caustics = caustics.map { updateCaustic(it, canvasSize, frame) }
                
                // Update涟漪
                ripples = ripples.mapNotNull { ripple ->
                    val newRipple = ripple.copy(
                        radius = ripple.radius + 4f,
                        alpha = ripple.alpha - 0.015f
                    )
                    if (newRipple.alpha > 0) newRipple else null
                }
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 深海背景渐变
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF001220),
                    Color(0xFF002040),
                    Color(0xFF003366),
                    Color(0xFF004080),
                    Color(0xFF0066AA)
                )
            )
        )
        
        // 绘制焦散光效
        caustics.forEach { caustic ->
            drawCaustic(caustic)
        }
        
        // 绘制光线
        drawLightRaysEnhanced(width, height, frame, tiltX)
        
        // 绘制水母
        jellyfish.forEach { jf ->
            drawJellyfish(jf, frame)
        }
        
        // 绘制气泡
        bubbles.forEach { bubble ->
            drawBubbleEnhanced(bubble)
        }
        
        // 绘制水面
        drawWaterSurfaceEnhanced(wavePoints, height, frame)
        
        // 绘制涟漪
        ripples.forEach { ripple ->
            drawRippleEnhanced(ripple)
        }
        
        // 绘制海底
        drawSeaFloorEnhanced(width, height, frame)
    }
}

data class WavePoint(val x: Float, var y: Float, var velocity: Float)
data class Bubble(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var size: Float, var wobblePhase: Float, var alpha: Float = 1f
)
data class Jellyfish(
    var x: Float, var y: Float, var size: Float,
    var pulsePhase: Float, var driftPhase: Float,
    var color: Color, var tentaclePhase: Float
)
data class OceanRipple(val x: Float, val y: Float, var radius: Float, var alpha: Float)
data class Caustic(var x: Float, var y: Float, var size: Float, var phase: Float, var speed: Float)

private fun createBubble(canvasSize: Size): Bubble {
    return Bubble(
        x = Random.nextFloat() * canvasSize.width,
        y = canvasSize.height + Random.nextFloat() * 100,
        vx = 0f,
        vy = -(Random.nextFloat() * 2.5f + 1f),
        size = Random.nextFloat() * 14f + 4f,
        wobblePhase = Random.nextFloat() * PI.toFloat() * 2,
        alpha = Random.nextFloat() * 0.4f + 0.4f
    )
}

private fun createJellyfish(canvasSize: Size): Jellyfish {
    return Jellyfish(
        x = Random.nextFloat() * canvasSize.width,
        y = canvasSize.height * (0.4f + Random.nextFloat() * 0.4f),
        size = Random.nextFloat() * 30f + 25f,
        pulsePhase = Random.nextFloat() * PI.toFloat() * 2,
        driftPhase = Random.nextFloat() * PI.toFloat() * 2,
        color = listOf(
            Color(0xFF80DEEA), Color(0xFFB2EBF2), Color(0xFFE0F7FA),
            Color(0xFFCE93D8), Color(0xFFF8BBD9)
        ).random(),
        tentaclePhase = Random.nextFloat() * PI.toFloat() * 2
    )
}

private fun createCaustic(canvasSize: Size): Caustic {
    return Caustic(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        size = Random.nextFloat() * 80f + 40f,
        phase = Random.nextFloat() * PI.toFloat() * 2,
        speed = Random.nextFloat() * 0.02f + 0.01f
    )
}

private fun updateWavePhysics(
    points: List<WavePoint>, canvasSize: Size,
    tiltX: Float, frame: Long, waterLevel: Float
): List<WavePoint> {
    val springConstant = 0.025f
    val damping = 0.97f
    val spread = 0.25f
    
    val newPoints = points.toMutableList()
    val baseY = canvasSize.height * waterLevel
    
    // Update每个点
    for (i in newPoints.indices) {
        val point = newPoints[i]
        val waveOffset = sin(frame * 0.03f + i * 0.15f) * 8 + sin(frame * 0.02f + i * 0.08f) * 5
        val targetY = baseY + waveOffset + tiltX * 15
        val displacement = point.y - targetY
        
        var newVelocity = point.velocity - displacement * springConstant
        newVelocity *= damping
        
        newPoints[i] = point.copy(y = point.y + newVelocity, velocity = newVelocity)
    }
    
    // 波浪传播
    repeat(3) {
        for (i in newPoints.indices) {
            if (i > 0) {
                val delta = newPoints[i].y - newPoints[i - 1].y
                newPoints[i - 1] = newPoints[i - 1].copy(velocity = newPoints[i - 1].velocity + delta * spread)
            }
            if (i < newPoints.size - 1) {
                val delta = newPoints[i].y - newPoints[i + 1].y
                newPoints[i + 1] = newPoints[i + 1].copy(velocity = newPoints[i + 1].velocity + delta * spread)
            }
        }
    }
    
    return newPoints
}

private fun updateBubblePhysics(bubble: Bubble, canvasSize: Size, tiltX: Float, wavePoints: List<WavePoint>): Bubble {
    var newBubble = bubble.copy(
        x = bubble.x + bubble.vx + sin(bubble.wobblePhase) * 0.8f + tiltX * 3,
        y = bubble.y + bubble.vy,
        vx = bubble.vx * 0.95f,
        vy = (bubble.vy - 0.02f).coerceAtLeast(-4f),
        wobblePhase = bubble.wobblePhase + 0.12f
    )
    
    // Check是否到达水面
    if (wavePoints.isNotEmpty()) {
        val nearestWave = wavePoints.minByOrNull { abs(it.x - newBubble.x) }
        if (nearestWave != null && newBubble.y < nearestWave.y) {
            // 气泡破裂，重生
            newBubble = createBubble(canvasSize)
        }
    }
    
    if (newBubble.y < -50) {
        newBubble = createBubble(canvasSize)
    }
    
    return newBubble
}

private fun updateJellyfish(jf: Jellyfish, canvasSize: Size, frame: Long): Jellyfish {
    val pulse = sin(jf.pulsePhase)
    val drift = sin(jf.driftPhase) * 0.5f
    
    var newJf = jf.copy(
        x = jf.x + drift,
        y = jf.y + pulse * 0.3f - 0.2f,
        pulsePhase = jf.pulsePhase + 0.05f,
        driftPhase = jf.driftPhase + 0.02f,
        tentaclePhase = jf.tentaclePhase + 0.08f
    )
    
    // 边界处理
    if (newJf.y < canvasSize.height * 0.3f) newJf = newJf.copy(y = canvasSize.height * 0.8f)
    if (newJf.x < -50) newJf = newJf.copy(x = canvasSize.width + 50)
    if (newJf.x > canvasSize.width + 50) newJf = newJf.copy(x = -50f)
    
    return newJf
}

private fun updateCaustic(caustic: Caustic, canvasSize: Size, frame: Long): Caustic {
    return caustic.copy(
        phase = caustic.phase + caustic.speed,
        x = (caustic.x + sin(caustic.phase) * 0.5f).let { 
            if (it < -caustic.size) canvasSize.width + caustic.size else it 
        }
    )
}

private fun DrawScope.drawLightRaysEnhanced(width: Float, height: Float, frame: Long, tiltX: Float) {
    val rayCount = 6
    for (i in 0 until rayCount) {
        val baseX = width * (0.15f + i * 0.14f)
        val startX = baseX + sin(frame * 0.008f + i * 0.5f) * 30 + tiltX * 20
        val spread = 60f + sin(frame * 0.01f + i) * 20
        
        val path = Path().apply {
            moveTo(startX, 0f)
            lineTo(startX - spread, height * 0.7f)
            lineTo(startX + spread + 40, height * 0.7f)
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x3090E0EF),
                    Color(0x1548CAE4),
                    Color(0x0500B4D8)
                )
            )
        )
    }
}

private fun DrawScope.drawCaustic(caustic: Caustic) {
    val wobble = sin(caustic.phase) * 0.3f + 1f
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x2090E0EF),
                Color(0x1048CAE4),
                Color.Transparent
            )
        ),
        radius = caustic.size * wobble,
        center = Offset(caustic.x, caustic.y)
    )
}

private fun DrawScope.drawJellyfish(jf: Jellyfish, frame: Long) {
    val pulse = (sin(jf.pulsePhase) + 1) / 2
    val bodyWidth = jf.size * (0.8f + pulse * 0.4f)
    val bodyHeight = jf.size * (0.6f + pulse * 0.2f)
    
    // 发光效果
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                jf.color.copy(alpha = 0.4f),
                jf.color.copy(alpha = 0.1f),
                Color.Transparent
            )
        ),
        radius = jf.size * 2,
        center = Offset(jf.x, jf.y)
    )
    
    // 身体
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                jf.color.copy(alpha = 0.8f),
                jf.color.copy(alpha = 0.5f),
                jf.color.copy(alpha = 0.2f)
            ),
            center = Offset(jf.x, jf.y - bodyHeight * 0.2f)
        ),
        topLeft = Offset(jf.x - bodyWidth / 2, jf.y - bodyHeight / 2),
        size = Size(bodyWidth, bodyHeight)
    )
    
    // 触须
    val tentacleCount = 6
    for (i in 0 until tentacleCount) {
        val startX = jf.x - bodyWidth * 0.3f + (bodyWidth * 0.6f) * i / (tentacleCount - 1)
        val startY = jf.y + bodyHeight * 0.3f
        
        val tentaclePath = Path().apply {
            moveTo(startX, startY)
            val segments = 5
            var currentX = startX
            var currentY = startY
            for (j in 1..segments) {
                val wave = sin(jf.tentaclePhase + i * 0.5f + j * 0.8f) * 8
                currentX += wave
                currentY += jf.size * 0.4f
                lineTo(currentX, currentY)
            }
        }
        
        drawPath(
            path = tentaclePath,
            color = jf.color.copy(alpha = 0.6f - i * 0.05f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawBubbleEnhanced(bubble: Bubble) {
    // 气泡光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x30FFFFFF),
                Color(0x1590E0EF),
                Color.Transparent
            ),
            center = Offset(bubble.x - bubble.size * 0.2f, bubble.y - bubble.size * 0.2f)
        ),
        radius = bubble.size * 1.5f,
        center = Offset(bubble.x, bubble.y)
    )
    
    // 气泡主体
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x2090E0EF),
                Color(0x4048CAE4)
            ),
            center = Offset(bubble.x + bubble.size * 0.3f, bubble.y + bubble.size * 0.3f)
        ),
        radius = bubble.size,
        center = Offset(bubble.x, bubble.y)
    )
    
    // 气泡边缘
    drawCircle(
        color = Color(0x50FFFFFF),
        radius = bubble.size,
        center = Offset(bubble.x, bubble.y),
        style = Stroke(width = 1.5f)
    )
    
    // 高光
    drawCircle(
        color = Color(0x80FFFFFF),
        radius = bubble.size * 0.25f,
        center = Offset(bubble.x - bubble.size * 0.3f, bubble.y - bubble.size * 0.3f)
    )
}

private fun DrawScope.drawWaterSurfaceEnhanced(points: List<WavePoint>, height: Float, frame: Long) {
    if (points.isEmpty()) return
    
    val path = Path().apply {
        moveTo(0f, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2
            val midY = (prev.y + curr.y) / 2
            quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
        lineTo(points.last().x, points.last().y)
        lineTo(size.width, 0f)
        lineTo(0f, 0f)
        close()
    }
    
    // 水面渐变
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x5090E0EF),
                Color(0x3048CAE4),
                Color(0x1500B4D8)
            )
        )
    )
    
    // 水面高光线
    val highlightPath = Path().apply {
        moveTo(0f, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2
            val midY = (prev.y + curr.y) / 2
            quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
    }
    
    drawPath(
        path = highlightPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0x60FFFFFF),
                Color(0x90FFFFFF),
                Color(0x60FFFFFF)
            )
        ),
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawRippleEnhanced(ripple: OceanRipple) {
    // 多层涟漪
    for (i in 0..2) {
        val radiusOffset = i * 15f
        val alphaOffset = i * 0.2f
        drawCircle(
            color = Color(0xFF90E0EF).copy(alpha = (ripple.alpha - alphaOffset).coerceAtLeast(0f)),
            radius = ripple.radius + radiusOffset,
            center = Offset(ripple.x, ripple.y),
            style = Stroke(width = 2f - i * 0.5f)
        )
    }
}

private fun DrawScope.drawSeaFloorEnhanced(width: Float, height: Float, frame: Long) {
    val floorY = height * 0.88f
    
    // 海底渐变
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF001830),
                Color(0xFF000D1A)
            ),
            startY = floorY - 50,
            endY = height
        ),
        topLeft = Offset(0f, floorY - 50),
        size = Size(width, height - floorY + 50)
    )
    
    // 海底地形
    val floorPath = Path().apply {
        moveTo(0f, height)
        lineTo(0f, floorY)
        var x = 0f
        val random = Random(42)
        while (x < width) {
            val nextX = x + random.nextFloat() * 40 + 20
            val peakY = floorY + random.nextFloat() * 20 - 10
            quadraticBezierTo(x + 10, peakY - 5, nextX, floorY + random.nextFloat() * 10)
            x = nextX
        }
        lineTo(width, height)
        close()
    }
    
    drawPath(
        path = floorPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0A2942), Color(0xFF051A24))
        )
    )
    
    // 海草
    val seaweedPositions = listOf(0.1f, 0.25f, 0.4f, 0.6f, 0.75f, 0.9f)
    seaweedPositions.forEach { xRatio ->
        val baseX = width * xRatio
        val seaweedHeight = height * (0.08f + Random(xRatio.hashCode()).nextFloat() * 0.06f)
        
        for (strand in 0..2) {
            val strandX = baseX + (strand - 1) * 8
            val seaweedPath = Path().apply {
                moveTo(strandX, floorY)
                var y = floorY
                var currentX = strandX
                while (y > floorY - seaweedHeight) {
                    val wave = sin(frame * 0.03f + y * 0.02f + strand) * 12
                    currentX = strandX + wave
                    y -= 15
                    lineTo(currentX, y)
                }
            }
            
            drawPath(
                path = seaweedPath,
                color = Color(0xFF2D6A4F).copy(alpha = 0.7f - strand * 0.15f),
                style = Stroke(width = 4f - strand, cap = StrokeCap.Round)
            )
        }
    }
}


// ==================== 星空银河 - Galaxy ====================

/**
 * 星空银河背景 - 深度优化版
 * 3D视差星空 + 旋转星云 + 流星雨 + 行星 + 星座连线
 */
@Composable
fun GalaxyEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "galaxy")
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    
    var stars by remember { mutableStateOf(listOf<Star3D>()) }
    var shootingStars by remember { mutableStateOf(listOf<GalaxyShootingStar>()) }
    var nebulaClouds by remember { mutableStateOf(listOf<NebulaCloud>()) }
    var touchStars by remember { mutableStateOf(listOf<TouchStar>()) }
    
    // Listen从 EnhancedThemeWrapper 传递的触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // Response触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                val offset = event.position
                vibrate(context, 15)
                onInteraction()
                // 点击创建星星爆发
                touchStars = touchStars + List(6) {
                    val angle = Random.nextFloat() * 2 * PI.toFloat()
                    val dist = Random.nextFloat() * 50 + 20
                    TouchStar(
                        x = offset.x + cos(angle) * dist,
                        y = offset.y + sin(angle) * dist,
                        size = Random.nextFloat() * 8 + 4,
                        alpha = 1f,
                        color = listOf(
                            Color(0xFFFFD700), Color(0xFF87CEEB),
                            Color(0xFFDDA0DD), Color.White
                        ).random()
                    )
                }
                // 添加流星
                shootingStars = shootingStars + GalaxyShootingStar(
                    x = offset.x, y = offset.y,
                    vx = Random.nextFloat() * 15 + 8,
                    vy = Random.nextFloat() * 10 + 3,
                    length = Random.nextFloat() * 80 + 50,
                    life = 1f,
                    color = Color.White
                )
            }
        }
    }
    
    // 视差
    var parallaxX by remember { mutableFloatStateOf(0f) }
    var parallaxY by remember { mutableFloatStateOf(0f) }
    
    // 星云旋转
    val nebulaRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(120000, easing = LinearEasing), RepeatMode.Restart),
        label = "nebulaRotation"
    )
    
    // 传感器
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                parallaxX = event.values[0] * 2f
                parallaxY = event.values[1] * 2f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    // 动画循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            stars = List(100) { createStar3D(canvasSize) }
            nebulaClouds = List(3) { createNebulaCloud(canvasSize) }
            
            while (true) {
                delay(16)
                frame++
                
                // Update星星
                stars = stars.map { updateStar3D(it, frame) }
                
                // Shuffle流星
                if (Random.nextFloat() < 0.015f && shootingStars.size < 4) {
                    shootingStars = shootingStars + createGalaxyShootingStar(canvasSize)
                }
                
                // Update流星
                shootingStars = shootingStars.mapNotNull { star ->
                    val newStar = star.copy(
                        x = star.x + star.vx,
                        y = star.y + star.vy,
                        life = star.life - 0.012f
                    )
                    if (newStar.life > 0 && newStar.x < canvasSize.width * 1.2f) newStar else null
                }
                
                // Update触摸星星
                touchStars = touchStars.mapNotNull { star ->
                    val newStar = star.copy(
                        size = star.size * 0.96f,
                        alpha = star.alpha * 0.95f
                    )
                    if (newStar.alpha > 0.05f) newStar else null
                }
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 深空背景
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A1A3E),
                    Color(0xFF12122A),
                    Color(0xFF0A0A18),
                    Color(0xFF050510)
                ),
                center = Offset(width * 0.3f, height * 0.3f),
                radius = maxOf(width, height) * 1.2f
            )
        )
        
        // 绘制星云
        rotate(nebulaRotation * 0.1f, pivot = Offset(width * 0.6f, height * 0.4f)) {
            nebulaClouds.forEach { cloud ->
                drawNebulaCloud(cloud, parallaxX, parallaxY)
            }
        }
        
        // 绘制银河带
        drawMilkyWayEnhanced(width, height, frame, parallaxX)
        
        // 绘制星星（按深度排序）
        stars.sortedBy { it.depth }.forEach { star ->
            drawStar3DEnhanced(star, parallaxX, parallaxY, frame)
        }
        
        // 绘制流星
        shootingStars.forEach { star ->
            drawGalaxyShootingStar(star)
        }
        
        // 绘制触摸星星
        touchStars.forEach { star ->
            drawTouchStar(star)
        }
        
        // 绘制行星
        drawPlanets(width, height, frame, parallaxX, parallaxY)
    }
}

data class Star3D(
    var x: Float, var y: Float, var depth: Float,
    var size: Float, var twinklePhase: Float, var twinkleSpeed: Float,
    var color: Color
)

data class GalaxyShootingStar(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var length: Float, var life: Float, var color: Color
)

data class NebulaCloud(
    var x: Float, var y: Float, var size: Float,
    var color: Color, var rotation: Float
)

data class TouchStar(
    var x: Float, var y: Float, var size: Float,
    var alpha: Float, var color: Color
)

private fun createStar3D(canvasSize: Size): Star3D {
    val depth = Random.nextFloat()
    return Star3D(
        x = Random.nextFloat() * canvasSize.width * 1.4f - canvasSize.width * 0.2f,
        y = Random.nextFloat() * canvasSize.height * 1.4f - canvasSize.height * 0.2f,
        depth = depth,
        size = (0.5f + depth * 3f) * (Random.nextFloat() * 0.5f + 0.5f),
        twinklePhase = Random.nextFloat() * PI.toFloat() * 2,
        twinkleSpeed = Random.nextFloat() * 0.08f + 0.02f,
        color = listOf(
            Color.White, Color(0xFFFFE4B5), Color(0xFFB0C4DE),
            Color(0xFFFFB6C1), Color(0xFFE6E6FA)
        ).random()
    )
}

private fun updateStar3D(star: Star3D, frame: Long): Star3D {
    return star.copy(twinklePhase = star.twinklePhase + star.twinkleSpeed)
}

private fun createGalaxyShootingStar(canvasSize: Size): GalaxyShootingStar {
    return GalaxyShootingStar(
        x = Random.nextFloat() * canvasSize.width * 0.5f - canvasSize.width * 0.1f,
        y = Random.nextFloat() * canvasSize.height * 0.4f,
        vx = Random.nextFloat() * 18f + 12f,
        vy = Random.nextFloat() * 12f + 6f,
        length = Random.nextFloat() * 100f + 60f,
        life = 1f,
        color = listOf(Color.White, Color(0xFF87CEEB), Color(0xFFFFD700)).random()
    )
}

private fun createNebulaCloud(canvasSize: Size): NebulaCloud {
    return NebulaCloud(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height * 0.7f,
        size = Random.nextFloat() * 200f + 150f,
        color = listOf(
            Color(0xFF5C4D7D), Color(0xFF7C5CBF), Color(0xFF4A3B6B),
            Color(0xFF6B4E8C), Color(0xFF3D2B5A)
        ).random(),
        rotation = Random.nextFloat() * 360f
    )
}

private fun DrawScope.drawNebulaCloud(cloud: NebulaCloud, parallaxX: Float, parallaxY: Float) {
    val adjustedX = cloud.x + parallaxX * 0.5f
    val adjustedY = cloud.y + parallaxY * 0.5f
    
    rotate(cloud.rotation, pivot = Offset(adjustedX, adjustedY)) {
        // 多层星云
        for (i in 0..2) {
            val layerSize = cloud.size * (1f + i * 0.3f)
            val layerAlpha = 0.15f - i * 0.04f
            
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cloud.color.copy(alpha = layerAlpha),
                        cloud.color.copy(alpha = layerAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(adjustedX, adjustedY)
                ),
                topLeft = Offset(adjustedX - layerSize, adjustedY - layerSize * 0.6f),
                size = Size(layerSize * 2, layerSize * 1.2f)
            )
        }
    }
}

private fun DrawScope.drawMilkyWayEnhanced(width: Float, height: Float, frame: Long, parallaxX: Float) {
    val milkyWayPath = Path()
    val startX = -width * 0.3f + parallaxX * 2
    val startY = height * 0.15f
    val endX = width * 1.3f + parallaxX * 2
    val endY = height * 0.85f
    
    milkyWayPath.moveTo(startX, startY)
    milkyWayPath.cubicTo(
        width * 0.3f, height * 0.3f,
        width * 0.7f, height * 0.5f,
        endX, endY
    )
    
    // 银河主体
    drawPath(
        path = milkyWayPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x15FFFFFF),
                Color(0x25E6E6FA),
                Color(0x20DDA0DD),
                Color(0x15FFFFFF)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY)
        ),
        style = Stroke(width = 120f, cap = StrokeCap.Round)
    )
    
    // 银河核心
    drawPath(
        path = milkyWayPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x30FFFFFF),
                Color(0x40FFE4B5),
                Color(0x30FFFFFF)
            )
        ),
        style = Stroke(width = 40f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawStar3DEnhanced(star: Star3D, parallaxX: Float, parallaxY: Float, frame: Long) {
    val parallaxFactor = star.depth
    val adjustedX = star.x + parallaxX * parallaxFactor * 8
    val adjustedY = star.y + parallaxY * parallaxFactor * 8
    
    val twinkle = (sin(star.twinklePhase) + 1) / 2
    val alpha = (0.4f + twinkle * 0.6f) * (0.3f + star.depth * 0.7f)
    val currentSize = star.size * (0.8f + twinkle * 0.4f)
    
    // 大星星的光晕
    if (star.depth > 0.6f && currentSize > 2f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    star.color.copy(alpha = alpha * 0.4f),
                    star.color.copy(alpha = alpha * 0.1f),
                    Color.Transparent
                )
            ),
            radius = currentSize * 5,
            center = Offset(adjustedX, adjustedY)
        )
        
        // 十字光芒
        val rayLength = currentSize * 4
        val rayAlpha = alpha * 0.5f
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

private fun DrawScope.drawGalaxyShootingStar(star: GalaxyShootingStar) {
    val angle = atan2(star.vy, star.vx)
    val tailX = star.x - cos(angle) * star.length * star.life
    val tailY = star.y - sin(angle) * star.length * star.life
    
    // 流星尾迹
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                star.color.copy(alpha = star.life),
                star.color.copy(alpha = star.life * 0.5f),
                Color.Transparent
            ),
            start = Offset(star.x, star.y),
            end = Offset(tailX, tailY)
        ),
        start = Offset(star.x, star.y),
        end = Offset(tailX, tailY),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    
    // 流星头部光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = star.life),
                star.color.copy(alpha = star.life * 0.5f),
                Color.Transparent
            )
        ),
        radius = 10f,
        center = Offset(star.x, star.y)
    )
}

private fun DrawScope.drawTouchStar(star: TouchStar) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                star.color.copy(alpha = star.alpha),
                star.color.copy(alpha = star.alpha * 0.3f),
                Color.Transparent
            )
        ),
        radius = star.size * 2,
        center = Offset(star.x, star.y)
    )
    
    drawCircle(
        color = Color.White.copy(alpha = star.alpha),
        radius = star.size * 0.5f,
        center = Offset(star.x, star.y)
    )
}

private fun DrawScope.drawPlanets(width: Float, height: Float, frame: Long, parallaxX: Float, parallaxY: Float) {
    // 远处的行星
    val planet1X = width * 0.85f + parallaxX * 0.3f
    val planet1Y = height * 0.2f + parallaxY * 0.3f
    
    // 行星光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x30DDA0DD),
                Color(0x15DDA0DD),
                Color.Transparent
            )
        ),
        radius = 60f,
        center = Offset(planet1X, planet1Y)
    )
    
    // 行星本体
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF9370DB),
                Color(0xFF6A5ACD),
                Color(0xFF483D8B)
            ),
            center = Offset(planet1X - 8f, planet1Y - 8f)
        ),
        radius = 25f,
        center = Offset(planet1X, planet1Y)
    )
    
    // 行星环
    rotate(20f, pivot = Offset(planet1X, planet1Y)) {
        drawOval(
            color = Color(0x60DDA0DD),
            topLeft = Offset(planet1X - 45f, planet1Y - 8f),
            size = Size(90f, 16f),
            style = Stroke(width = 3f)
        )
    }
}


// ==================== 赛博霓虹 - Cyberpunk ====================

/**
 * 赛博朋克霓虹背景 - 深度优化版
 * 透视网格 + 霓虹建筑 + 故障艺术 + 数据流 + 全息效果
 */
@Composable
fun CyberpunkEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "cyber")
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    var glitchLines by remember { mutableStateOf(listOf<GlitchLine>()) }
    var dataStreams by remember { mutableStateOf(listOf<DataStream>()) }
    var holoParticles by remember { mutableStateOf(listOf<HoloParticle>()) }
    
    // Listen从 EnhancedThemeWrapper 传递的触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // Response触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                val offset = event.position
                vibrate(context, 25)
                onInteraction()
                // 点击触发故障
                glitchLines = List(8) {
                    GlitchLine(
                        y = offset.y + (Random.nextFloat() - 0.5f) * 150,
                        height = Random.nextFloat() * 20 + 5,
                        offset = (Random.nextFloat() - 0.5f) * 60,
                        rgbSplit = Random.nextFloat() * 12 + 3
                    )
                }
                // 添加全息粒子爆发
                holoParticles = holoParticles + List(10) {
                    val angle = Random.nextFloat() * 2 * PI.toFloat()
                    val speed = Random.nextFloat() * 8 + 3
                    HoloParticle(
                        x = offset.x, y = offset.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        size = Random.nextFloat() * 6 + 2,
                        alpha = 1f,
                        color = listOf(
                            Color(0xFF00FFFF), Color(0xFFFF00FF), Color(0xFFFFFF00)
                        ).random()
                    )
                }
            }
        }
    }
    
    // 霹虹闪烁
    val neonFlicker by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse),
        label = "flicker"
    )
    
    // 网格滚动
    val gridScroll by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "gridScroll"
    )
    
    // 故障效果定时器
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(800, 3000))
            if (canvasSize.width > 0) {
                glitchLines = List(Random.nextInt(4, 10)) {
                    GlitchLine(
                        y = Random.nextFloat() * canvasSize.height,
                        height = Random.nextFloat() * 25 + 5,
                        offset = (Random.nextFloat() - 0.5f) * 40,
                        rgbSplit = Random.nextFloat() * 8 + 2
                    )
                }
            }
            delay(80)
            glitchLines = emptyList()
        }
    }
    
    // 动画循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            dataStreams = List(8) { createDataStream(canvasSize) }
            holoParticles = List(20) { createHoloParticle(canvasSize) }
            
            while (true) {
                delay(16)
                frame++
                
                // Update数据流
                dataStreams = dataStreams.map { updateDataStream(it, canvasSize) }
                
                // Update全息粒子
                holoParticles = holoParticles.map { updateHoloParticle(it, canvasSize, frame) }
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 深色背景
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF05050A),
                    Color(0xFF0A0A12),
                    Color(0xFF0F0F1A)
                )
            )
        )
        
        // 透视网格
        drawCyberGridEnhanced(width, height, gridScroll, frame)
        
        // 数据流
        dataStreams.forEach { stream ->
            drawDataStream(stream, frame)
        }
        
        // 霓虹建筑
        drawCyberBuildingsEnhanced(width, height, neonFlicker, frame)
        
        // 霓虹光效
        drawNeonGlowEnhanced(width, height, neonFlicker, frame)
        
        // 全息粒子
        holoParticles.forEach { particle ->
            drawHoloParticle(particle)
        }
        
        // 故障效果
        glitchLines.forEach { line ->
            drawGlitchLineEnhanced(line, width)
        }
        
        // 扫描线
        drawScanLinesEnhanced(width, height, frame)
        
        // 边框霓虹
        drawNeonBorder(width, height, neonFlicker, frame)
    }
}

data class GlitchLine(val y: Float, val height: Float, val offset: Float, val rgbSplit: Float)
data class DataStream(var x: Float, var y: Float, var speed: Float, var chars: List<Char>, var alpha: Float)
data class HoloParticle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var size: Float, var alpha: Float, var color: Color
)

private fun createDataStream(canvasSize: Size): DataStream {
    return DataStream(
        x = Random.nextFloat() * canvasSize.width,
        y = -Random.nextFloat() * canvasSize.height,
        speed = Random.nextFloat() * 4 + 2,
        chars = List(Random.nextInt(8, 20)) { 
            (0x30A0 + Random.nextInt(96)).toChar() // 日文片假名
        },
        alpha = Random.nextFloat() * 0.5f + 0.3f
    )
}

private fun updateDataStream(stream: DataStream, canvasSize: Size): DataStream {
    var newStream = stream.copy(y = stream.y + stream.speed)
    if (newStream.y > canvasSize.height + 200) {
        newStream = createDataStream(canvasSize)
    }
    return newStream
}

private fun createHoloParticle(canvasSize: Size): HoloParticle {
    return HoloParticle(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        vx = (Random.nextFloat() - 0.5f) * 2,
        vy = (Random.nextFloat() - 0.5f) * 2,
        size = Random.nextFloat() * 4 + 1,
        alpha = Random.nextFloat() * 0.5f + 0.2f,
        color = listOf(Color(0xFF00FFFF), Color(0xFFFF00FF), Color(0xFFFFFF00)).random()
    )
}

private fun updateHoloParticle(particle: HoloParticle, canvasSize: Size, frame: Long): HoloParticle {
    var newParticle = particle.copy(
        x = particle.x + particle.vx,
        y = particle.y + particle.vy,
        alpha = (particle.alpha + sin(frame * 0.1f + particle.x * 0.01f) * 0.1f).coerceIn(0.1f, 0.7f)
    )
    
    // 边界处理
    if (newParticle.x < 0 || newParticle.x > canvasSize.width) newParticle = newParticle.copy(vx = -newParticle.vx)
    if (newParticle.y < 0 || newParticle.y > canvasSize.height) newParticle = newParticle.copy(vy = -newParticle.vy)
    
    return newParticle
}

private fun DrawScope.drawCyberGridEnhanced(width: Float, height: Float, scroll: Float, frame: Long) {
    val gridStartY = height * 0.55f
    val horizonY = height * 0.35f
    val lineCount = 25
    
    // 水平线（透视）
    for (i in 0..lineCount) {
        val progress = (i.toFloat() / lineCount + scroll / lineCount) % 1f
        val y = gridStartY + (height - gridStartY) * progress
        val perspectiveScale = ((y - horizonY) / (height - horizonY)).coerceIn(0f, 1f)
        val alpha = perspectiveScale * 0.6f
        val lineWidth = 1f + perspectiveScale * 2f
        
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF00FFFF).copy(alpha = alpha * 0.3f),
                    Color(0xFF00FFFF).copy(alpha = alpha),
                    Color(0xFF00FFFF).copy(alpha = alpha * 0.3f)
                )
            ),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = lineWidth
        )
    }
    
    // 垂直线（透视）
    val verticalLines = 20
    for (i in 0..verticalLines) {
        val topX = width * i / verticalLines
        val bottomX = width * 0.5f + (topX - width * 0.5f) * 2.5f
        val alpha = 0.4f - abs(i - verticalLines / 2f) / verticalLines * 0.3f
        
        drawLine(
            color = Color(0xFF00FFFF).copy(alpha = alpha),
            start = Offset(topX, horizonY),
            end = Offset(bottomX, height),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawDataStream(stream: DataStream, frame: Long) {
    val charHeight = 14f
    stream.chars.forEachIndexed { index, char ->
        val charY = stream.y + index * charHeight
        if (charY > 0 && charY < size.height) {
            val charAlpha = stream.alpha * (1f - index.toFloat() / stream.chars.size)
            val isHead = index == 0
            
            // 使用简单的矩形代替文字
            drawRect(
                color = if (isHead) Color.White.copy(alpha = charAlpha) 
                       else Color(0xFF00FF00).copy(alpha = charAlpha),
                topLeft = Offset(stream.x, charY),
                size = Size(8f, 12f)
            )
        }
    }
}

private fun DrawScope.drawCyberBuildingsEnhanced(width: Float, height: Float, flicker: Float, frame: Long) {
    val buildingCount = 10
    val random = Random(42)
    
    for (i in 0 until buildingCount) {
        val buildingWidth = random.nextFloat() * 100 + 50
        val buildingHeight = random.nextFloat() * height * 0.35f + height * 0.1f
        val x = width * i / buildingCount + random.nextFloat() * 30 - 15
        val y = height * 0.45f - buildingHeight
        
        // 建筑剪影
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0A15), Color(0xFF151520))
            ),
            topLeft = Offset(x, y),
            size = Size(buildingWidth, buildingHeight + height * 0.15f)
        )
        
        // 建筑边缘霓虹
        drawRect(
            color = Color(0xFF00FFFF).copy(alpha = 0.3f * flicker),
            topLeft = Offset(x, y),
            size = Size(buildingWidth, buildingHeight + height * 0.15f),
            style = Stroke(width = 1f)
        )
        
        // 窗户
        val windowRows = (buildingHeight / 25).toInt()
        val windowCols = (buildingWidth / 18).toInt()
        for (row in 0 until windowRows) {
            for (col in 0 until windowCols) {
                if (random.nextFloat() > 0.25f) {
                    val windowColor = when {
                        random.nextFloat() > 0.85f -> Color(0xFFFF00FF)
                        random.nextFloat() > 0.7f -> Color(0xFF00FFFF)
                        random.nextFloat() > 0.5f -> Color(0xFFFFFF00)
                        else -> Color(0xFF00FF00)
                    }
                    val windowFlicker = if (random.nextFloat() > 0.92f) flicker else 1f
                    
                    drawRect(
                        color = windowColor.copy(alpha = 0.7f * windowFlicker),
                        topLeft = Offset(x + 6 + col * 18f, y + 8 + row * 25f),
                        size = Size(10f, 16f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNeonGlowEnhanced(width: Float, height: Float, flicker: Float, frame: Long) {
    val neonConfigs = listOf(
        Triple(Color(0xFFFF00FF), 40f, 0f),
        Triple(Color(0xFF00FFFF), 70f, 0.3f),
        Triple(Color(0xFFFFFF00), 100f, 0.6f)
    )
    
    neonConfigs.forEach { (color, y, phaseOffset) ->
        val pulse = (sin(frame * 0.05f + phaseOffset * PI.toFloat()) + 1) / 2 * 0.3f + 0.7f
        val actualFlicker = flicker * pulse
        
        // 发光效果
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.4f * actualFlicker),
                    color.copy(alpha = 0.1f * actualFlicker)
                ),
                startY = y - 15,
                endY = y + 15
            ),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 30f
        )
        
        // 核心线条
        drawLine(
            color = color.copy(alpha = 0.9f * actualFlicker),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawHoloParticle(particle: HoloParticle) {
    // 粒子光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                particle.color.copy(alpha = particle.alpha),
                particle.color.copy(alpha = particle.alpha * 0.3f),
                Color.Transparent
            )
        ),
        radius = particle.size * 3,
        center = Offset(particle.x, particle.y)
    )
    
    // 粒子核心
    drawCircle(
        color = Color.White.copy(alpha = particle.alpha),
        radius = particle.size * 0.5f,
        center = Offset(particle.x, particle.y)
    )
}

private fun DrawScope.drawGlitchLineEnhanced(line: GlitchLine, width: Float) {
    // RGB分离效果
    drawRect(
        color = Color.Red.copy(alpha = 0.6f),
        topLeft = Offset(line.offset - line.rgbSplit, line.y),
        size = Size(width, line.height)
    )
    drawRect(
        color = Color.Cyan.copy(alpha = 0.6f),
        topLeft = Offset(line.offset + line.rgbSplit, line.y),
        size = Size(width, line.height)
    )
    drawRect(
        color = Color.White.copy(alpha = 0.4f),
        topLeft = Offset(line.offset, line.y),
        size = Size(width, line.height)
    )
}

private fun DrawScope.drawScanLinesEnhanced(width: Float, height: Float, frame: Long) {
    // 静态扫描线
    var y = 0f
    while (y < height) {
        drawLine(
            color = Color.Black.copy(alpha = 0.08f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += 3f
    }
    
    // 移动扫描线
    val scanY = (frame * 3 % height.toLong()).toFloat()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF00FFFF).copy(alpha = 0.15f),
                Color.Transparent
            ),
            startY = scanY - 40,
            endY = scanY + 40
        ),
        topLeft = Offset(0f, scanY - 40),
        size = Size(width, 80f)
    )
}

private fun DrawScope.drawNeonBorder(width: Float, height: Float, flicker: Float, frame: Long) {
    val borderWidth = 3f
    val glowWidth = 15f
    val colors = listOf(Color(0xFF00FFFF), Color(0xFFFF00FF))
    val progress = (frame % 200) / 200f
    
    // 顶部边框
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(colors[0], colors[1], colors[0]),
            startX = width * progress - width * 0.5f,
            endX = width * progress + width * 0.5f
        ),
        start = Offset(0f, borderWidth / 2),
        end = Offset(width, borderWidth / 2),
        strokeWidth = borderWidth,
        alpha = flicker * 0.8f
    )
}


// ==================== 熔岩之心 - Volcano ====================

/**
 * 熔岩流动背景 - 深度优化版
 * 流动岩浆 + 火焰粒子 + 岩石纹理 + 火山喷发 + 热浪扭曲
 */
@Composable
fun VolcanoEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "volcano")
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    var fireParticles by remember { mutableStateOf(listOf<FireParticle>()) }
    var lavaBlobs by remember { mutableStateOf(listOf<LavaBlob>()) }
    var sparks by remember { mutableStateOf(listOf<Spark>()) }
    var erupting by remember { mutableStateOf(false) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // 熔岩流动
    val lavaFlow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "lavaFlow"
    )
    
    // 热浪
    val heatWave by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "heatWave"
    )
    
    // 动画循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            fireParticles = List(30) { createFireParticle(canvasSize) }
            lavaBlobs = List(8) { createLavaBlob(canvasSize) }
            
            while (true) {
                delay(16)
                frame++
                
                // Update火焰粒子
                fireParticles = fireParticles.map { updateFireParticle(it, canvasSize) }
                
                // Update熔岩块
                lavaBlobs = lavaBlobs.map { updateLavaBlob(it, canvasSize, lavaFlow) }
                
                // Update火花
                sparks = sparks.mapNotNull { spark ->
                    val newSpark = spark.copy(
                        x = spark.x + spark.vx,
                        y = spark.y + spark.vy,
                        vy = spark.vy + 0.15f,
                        life = spark.life - 0.02f
                    )
                    if (newSpark.life > 0) newSpark else null
                }
                
                // 喷发效果
                if (erupting && frame % 5 == 0L) {
                    sparks = sparks + List(3) {
                        Spark(
                            x = canvasSize.width * 0.5f + (Random.nextFloat() - 0.5f) * 50,
                            y = canvasSize.height * 0.3f,
                            vx = (Random.nextFloat() - 0.5f) * 12,
                            vy = -Random.nextFloat() * 15 - 8,
                            size = Random.nextFloat() * 6 + 2,
                            life = 1f,
                            color = listOf(
                                Color(0xFFFF6B35), Color(0xFFFFD700), Color(0xFFFF4500)
                            ).random()
                        )
                    }
                }
            }
        }
    }
    
    // 喷发定时器
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(5000, 12000))
            erupting = true
            vibrate(context, 100)
            delay(1500)
            erupting = false
        }
    }
    
    // Response增强触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                val offset = event.position
                vibrate(context, 40)
                onInteraction()
                // 点击喷发
                sparks = sparks + List(15) {
                    val angle = Random.nextFloat() * PI.toFloat() - PI.toFloat() / 2
                    val speed = Random.nextFloat() * 15 + 8
                    Spark(
                        x = offset.x, y = offset.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed - 5,
                        size = Random.nextFloat() * 8 + 3,
                        life = 1f,
                        color = listOf(
                            Color(0xFFFF6B35), Color(0xFFFFD700),
                            Color(0xFFFF4500), Color(0xFFFF8C00)
                        ).random()
                    )
                }
                fireParticles = fireParticles + List(8) {
                    FireParticle(
                        x = offset.x + (Random.nextFloat() - 0.5f) * 40,
                        y = offset.y,
                        vx = (Random.nextFloat() - 0.5f) * 6,
                        vy = -Random.nextFloat() * 10 - 5,
                        size = Random.nextFloat() * 12 + 6,
                        life = 1f,
                        color = listOf(
                            Color(0xFFFF6B35), Color(0xFFFFD700), Color(0xFFFF4500)
                        ).random()
                    )
                }
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 深色岩石背景
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1A0A08),
                    Color(0xFF2D1A16),
                    Color(0xFF3D2420),
                    Color(0xFF4A2A24)
                )
            )
        )
        
        // 火山轮廓
        drawVolcanoSilhouette(width, height)
        
        // 熔岩层
        drawLavaLayerEnhanced(width, height, lavaFlow, heatWave)
        
        // 熔岩块
        lavaBlobs.forEach { blob ->
            drawLavaBlobEnhanced(blob)
        }
        
        // 火焰粒子
        fireParticles.forEach { particle ->
            drawFireParticleEnhanced(particle)
        }
        
        // 火花
        sparks.forEach { spark ->
            drawSpark(spark)
        }
        
        // 热浪扭曲
        drawHeatDistortionEnhanced(width, height, heatWave, frame)
        
        // 岩石纹理
        drawRockTextureEnhanced(width, height)
        
        // 烟雾效果
        if (erupting) {
            drawVolcanoSmoke(width, height, frame)
        }
    }
}

data class FireParticle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var size: Float, var life: Float, var color: Color
)

data class LavaBlob(
    var x: Float, var y: Float, var size: Float,
    var phase: Float, var glowIntensity: Float
)

data class Spark(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var size: Float, var life: Float, var color: Color
)

private fun createFireParticle(canvasSize: Size): FireParticle {
    return FireParticle(
        x = Random.nextFloat() * canvasSize.width,
        y = canvasSize.height * (0.55f + Random.nextFloat() * 0.45f),
        vx = (Random.nextFloat() - 0.5f) * 2,
        vy = -Random.nextFloat() * 4 - 1,
        size = Random.nextFloat() * 10 + 4,
        life = Random.nextFloat() * 0.5f + 0.5f,
        color = listOf(
            Color(0xFFFF6B35), Color(0xFFFFD700),
            Color(0xFFFF4500), Color(0xFFFF8C00)
        ).random()
    )
}

private fun updateFireParticle(particle: FireParticle, canvasSize: Size): FireParticle {
    var newParticle = particle.copy(
        x = particle.x + particle.vx + (Random.nextFloat() - 0.5f) * 2,
        y = particle.y + particle.vy,
        vy = particle.vy - 0.08f,
        size = particle.size * 0.98f,
        life = particle.life - 0.008f
    )
    
    if (newParticle.life <= 0 || newParticle.y < canvasSize.height * 0.2f || newParticle.size < 1) {
        newParticle = createFireParticle(canvasSize)
    }
    
    return newParticle
}

private fun createLavaBlob(canvasSize: Size): LavaBlob {
    return LavaBlob(
        x = Random.nextFloat() * canvasSize.width,
        y = canvasSize.height * (0.65f + Random.nextFloat() * 0.25f),
        size = Random.nextFloat() * 50 + 25,
        phase = Random.nextFloat() * PI.toFloat() * 2,
        glowIntensity = Random.nextFloat() * 0.5f + 0.5f
    )
}

private fun updateLavaBlob(blob: LavaBlob, canvasSize: Size, lavaFlow: Float): LavaBlob {
    return blob.copy(
        x = blob.x + sin(lavaFlow + blob.phase) * 0.8f,
        y = blob.y + cos(lavaFlow * 0.5f + blob.phase) * 0.5f,
        glowIntensity = (blob.glowIntensity + sin(lavaFlow * 2 + blob.phase) * 0.1f).coerceIn(0.4f, 1f)
    )
}

private fun DrawScope.drawVolcanoSilhouette(width: Float, height: Float) {
    val volcanoPath = Path().apply {
        moveTo(0f, height)
        lineTo(0f, height * 0.5f)
        quadraticBezierTo(width * 0.15f, height * 0.4f, width * 0.35f, height * 0.35f)
        quadraticBezierTo(width * 0.45f, height * 0.28f, width * 0.5f, height * 0.3f)
        quadraticBezierTo(width * 0.55f, height * 0.28f, width * 0.65f, height * 0.35f)
        quadraticBezierTo(width * 0.85f, height * 0.4f, width, height * 0.5f)
        lineTo(width, height)
        close()
    }
    
    drawPath(
        path = volcanoPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2D1A16),
                Color(0xFF1A0A08)
            )
        )
    )
}

private fun DrawScope.drawLavaLayerEnhanced(width: Float, height: Float, flow: Float, wave: Float) {
    val lavaPath = Path()
    val baseY = height * 0.6f
    
    lavaPath.moveTo(0f, height)
    lavaPath.lineTo(0f, baseY)
    
    val segments = 40
    for (i in 0..segments) {
        val x = width * i / segments
        val noise1 = sin(x / width * 5 * PI + flow) * 25
        val noise2 = sin(x / width * 10 * PI + flow * 1.5f) * 12
        val noise3 = sin(wave + x / width * 3 * PI) * 8
        val y = baseY + noise1 + noise2 + noise3
        lavaPath.lineTo(x, y.toFloat())
    }
    
    lavaPath.lineTo(width, height)
    lavaPath.close()
    
    // 熔岩渐变
    drawPath(
        path = lavaPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFD700),
                Color(0xFFFF6B35),
                Color(0xFFFF4500),
                Color(0xFFD32F2F),
                Color(0xFF8B0000)
            ),
            startY = baseY - 40,
            endY = height
        )
    )
    
    // 熔岩表面高光
    val highlightPath = Path()
    highlightPath.moveTo(0f, baseY)
    for (i in 0..segments) {
        val x = width * i / segments
        val y = baseY + sin(x / width * 5 * PI + flow) * 25
        highlightPath.lineTo(x, y.toFloat())
    }
    
    drawPath(
        path = highlightPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = 0.6f),
                Color(0xFFFFFFFF).copy(alpha = 0.8f),
                Color(0xFFFFD700).copy(alpha = 0.6f)
            )
        ),
        style = Stroke(width = 5f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawLavaBlobEnhanced(blob: LavaBlob) {
    // 外层发光
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = blob.glowIntensity * 0.5f),
                Color(0xFFFF6B35).copy(alpha = blob.glowIntensity * 0.2f),
                Color.Transparent
            )
        ),
        radius = blob.size * 2,
        center = Offset(blob.x, blob.y)
    )
    
    // 熔岩块核心
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF).copy(alpha = blob.glowIntensity),
                Color(0xFFFFD700),
                Color(0xFFFF6B35),
                Color(0xFFFF4500)
            ),
            center = Offset(blob.x - blob.size * 0.2f, blob.y - blob.size * 0.2f)
        ),
        radius = blob.size,
        center = Offset(blob.x, blob.y)
    )
}

private fun DrawScope.drawFireParticleEnhanced(particle: FireParticle) {
    val alpha = particle.life.coerceIn(0f, 1f)
    
    // 粒子光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                particle.color.copy(alpha = alpha * 0.5f),
                particle.color.copy(alpha = alpha * 0.2f),
                Color.Transparent
            )
        ),
        radius = particle.size * 2.5f,
        center = Offset(particle.x, particle.y)
    )
    
    // 粒子核心
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha),
                particle.color.copy(alpha = alpha)
            )
        ),
        radius = particle.size,
        center = Offset(particle.x, particle.y)
    )
}

private fun DrawScope.drawSpark(spark: Spark) {
    val alpha = spark.life.coerceIn(0f, 1f)
    
    // 火花尾迹
    val tailLength = spark.size * 3
    val angle = atan2(spark.vy, spark.vx)
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(spark.color.copy(alpha = alpha), Color.Transparent),
            start = Offset(spark.x, spark.y),
            end = Offset(spark.x - cos(angle) * tailLength, spark.y - sin(angle) * tailLength)
        ),
        start = Offset(spark.x, spark.y),
        end = Offset(spark.x - cos(angle) * tailLength, spark.y - sin(angle) * tailLength),
        strokeWidth = spark.size * 0.5f,
        cap = StrokeCap.Round
    )
    
    // 火花核心
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = spark.size * 0.5f,
        center = Offset(spark.x, spark.y)
    )
}

private fun DrawScope.drawHeatDistortionEnhanced(width: Float, height: Float, wave: Float, frame: Long) {
    // 热浪效果
    val distortionLayers = 5
    for (i in 0 until distortionLayers) {
        val layerY = height * (0.35f + i * 0.05f)
        val layerAlpha = 0.08f - i * 0.01f
        val waveOffset = sin(wave + i * 0.5f) * 10
        
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFF6B35).copy(alpha = layerAlpha),
                    Color.Transparent
                ),
                startY = layerY - 30 + waveOffset,
                endY = layerY + 30 + waveOffset
            ),
            topLeft = Offset(0f, layerY - 30 + waveOffset),
            size = Size(width, 60f)
        )
    }
}

private fun DrawScope.drawRockTextureEnhanced(width: Float, height: Float) {
    val random = Random(456)
    
    // 岩石裂缝
    repeat(15) {
        val startX = random.nextFloat() * width
        val startY = random.nextFloat() * height * 0.4f
        
        val crackPath = Path().apply {
            moveTo(startX, startY)
            var x = startX
            var y = startY
            repeat(6) {
                x += (random.nextFloat() - 0.5f) * 60
                y += random.nextFloat() * 40
                lineTo(x, y)
            }
        }
        
        drawPath(
            path = crackPath,
            color = Color(0xFF0A0505).copy(alpha = 0.6f),
            style = Stroke(width = 2f + random.nextFloat() * 2)
        )
    }
}

private fun DrawScope.drawVolcanoSmoke(width: Float, height: Float, frame: Long) {
    val smokeCount = 8
    val random = Random(frame / 10)
    
    for (i in 0 until smokeCount) {
        val baseX = width * 0.5f + (random.nextFloat() - 0.5f) * 80
        val baseY = height * 0.28f - (frame % 100) * 2 - i * 30
        val size = 40f + i * 15f + random.nextFloat() * 20
        val alpha = (0.4f - i * 0.04f).coerceAtLeast(0f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4A4A4A).copy(alpha = alpha),
                    Color(0xFF3A3A3A).copy(alpha = alpha * 0.5f),
                    Color.Transparent
                )
            ),
            radius = size,
            center = Offset(baseX + sin(frame * 0.05f + i) * 20, baseY)
        )
    }
}


// ==================== 冰晶之境 - Frost ====================

/**
 * 冰晶效果背景 - 深度优化版
 * 冰晶生长 + 雪花物理 + 霜冻扩散 + 极光 + 触摸冰冻
 */
@Composable
fun FrostEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "frost")
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    var snowflakes by remember { mutableStateOf(listOf<Snowflake>()) }
    var iceCrystals by remember { mutableStateOf(listOf<IceCrystal>()) }
    var frostPatches by remember { mutableStateOf(listOf<FrostPatch>()) }
    var touchFrost by remember { mutableStateOf<TouchFrost?>(null) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // 重力感应
    var tiltX by remember { mutableFloatStateOf(0f) }
    
    // 闪烁
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer"
    )
    
    // 极光相位
    val auroraPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "aurora"
    )
    
    // 传感器
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = event.values[0] * 0.3f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    // 动画循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            snowflakes = List(40) { createSnowflake(canvasSize) }
            iceCrystals = List(6) { createIceCrystal(canvasSize) }
            frostPatches = List(5) { createFrostPatch(canvasSize) }
            
            while (true) {
                delay(16)
                frame++
                
                // Update雪花
                snowflakes = snowflakes.map { updateSnowflake(it, canvasSize, tiltX) }
                
                // Update冰晶生长
                iceCrystals = iceCrystals.map { crystal ->
                    if (crystal.size < crystal.targetSize) {
                        crystal.copy(size = crystal.size + (crystal.targetSize - crystal.size) * 0.02f)
                    } else crystal
                }
                
                // Update触摸霜冻
                touchFrost?.let { frost ->
                    if (frost.radius < frost.maxRadius) {
                        touchFrost = frost.copy(
                            radius = frost.radius + 5f,
                            alpha = (frost.alpha - 0.008f).coerceAtLeast(0f)
                        )
                    }
                    if (frost.alpha <= 0) touchFrost = null
                }
            }
        }
    }
    
    // Response增强触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0 && event.type == EnhancedTouchEvent.TouchType.TAP) {
                val offset = event.position
                vibrate(context, 20)
                onInteraction()
                // 触摸冰冻效果
                touchFrost = TouchFrost(
                    x = offset.x, y = offset.y,
                    radius = 0f, maxRadius = 200f, alpha = 1f
                )
                // 添加冰晶
                iceCrystals = iceCrystals + IceCrystal(
                    x = offset.x, y = offset.y,
                    size = 0f,
                    targetSize = Random.nextFloat() * 70 + 50,
                    rotation = Random.nextFloat() * 30 - 15,
                    branches = Random.nextInt(5, 9)
                )
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 冰蓝色背景
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF041520),
                    Color(0xFF082030),
                    Color(0xFF0C3045),
                    Color(0xFF104055)
                )
            )
        )
        
        // 极光效果
        drawFrostAuroraEnhanced(width, height, auroraPhase)
        
        // 霜冻斑块
        frostPatches.forEach { patch ->
            drawFrostPatch(patch, shimmer)
        }
        
        // 冰晶
        iceCrystals.forEach { crystal ->
            drawIceCrystalEnhanced(crystal, shimmer, frame)
        }
        
        // 雪花
        snowflakes.forEach { flake ->
            drawSnowflakeEnhanced(flake, frame)
        }
        
        // 触摸霜冻
        touchFrost?.let { frost ->
            drawTouchFrostEnhanced(frost, frame)
        }
        
        // 霜冻边缘
        drawFrostEdgeEnhanced(width, height, shimmer, frame)
        
        // 冰面反射
        drawIceReflection(width, height, shimmer)
    }
}

data class Snowflake(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var size: Float, var rotation: Float, var rotationSpeed: Float,
    var type: Int, var alpha: Float, var depth: Float
)

data class IceCrystal(
    var x: Float, var y: Float, var size: Float, var targetSize: Float,
    var rotation: Float, var branches: Int
)

data class FrostPatch(
    var x: Float, var y: Float, var size: Float, var alpha: Float
)

data class TouchFrost(
    var x: Float, var y: Float, var radius: Float,
    var maxRadius: Float, var alpha: Float
)

private fun createSnowflake(canvasSize: Size): Snowflake {
    val depth = Random.nextFloat()
    return Snowflake(
        x = Random.nextFloat() * canvasSize.width * 1.2f - canvasSize.width * 0.1f,
        y = -Random.nextFloat() * canvasSize.height * 0.5f - 30f,
        vx = (Random.nextFloat() - 0.5f) * 1.5f,
        vy = Random.nextFloat() * 2f + 0.5f,
        size = (Random.nextFloat() * 5f + 2f) * (0.5f + depth * 0.5f),
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 3f,
        type = Random.nextInt(4),
        alpha = 0.5f + depth * 0.5f,
        depth = depth
    )
}

private fun updateSnowflake(flake: Snowflake, canvasSize: Size, tiltX: Float): Snowflake {
    val sway = sin(flake.y * 0.02f + flake.x * 0.01f) * 0.8f
    var newFlake = flake.copy(
        x = flake.x + flake.vx + sway + tiltX * flake.depth,
        y = flake.y + flake.vy * (0.5f + flake.depth * 0.5f),
        rotation = flake.rotation + flake.rotationSpeed
    )
    
    if (newFlake.y > canvasSize.height + 30 || newFlake.x < -50 || newFlake.x > canvasSize.width + 50) {
        newFlake = createSnowflake(canvasSize)
    }
    
    return newFlake
}

private fun createIceCrystal(canvasSize: Size): IceCrystal {
    return IceCrystal(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        size = Random.nextFloat() * 40 + 30,
        targetSize = Random.nextFloat() * 60 + 40,
        rotation = Random.nextFloat() * 30 - 15,
        branches = Random.nextInt(5, 9)
    )
}

private fun createFrostPatch(canvasSize: Size): FrostPatch {
    return FrostPatch(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        size = Random.nextFloat() * 150 + 80,
        alpha = Random.nextFloat() * 0.15f + 0.05f
    )
}

private fun DrawScope.drawFrostAuroraEnhanced(width: Float, height: Float, phase: Float) {
    val auroraColors = listOf(
        Color(0xFF81D4FA),
        Color(0xFF4FC3F7),
        Color(0xFFB2EBF2),
        Color(0xFF80DEEA)
    )
    
    for (layer in 0..2) {
        val layerPhase = phase + layer * 0.5f
        val path = Path()
        val baseY = height * (0.15f + layer * 0.08f)
        
        path.moveTo(0f, 0f)
        path.lineTo(0f, baseY)
        
        for (i in 0..30) {
            val x = width * i / 30
            val y = baseY + sin(x / width * 4 * PI + layerPhase) * 40 +
                    sin(x / width * 2 * PI + layerPhase * 0.7f) * 20
            path.lineTo(x, y.toFloat())
        }
        
        path.lineTo(width, 0f)
        path.close()
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    auroraColors[layer].copy(alpha = 0.2f - layer * 0.05f),
                    auroraColors[layer].copy(alpha = 0.1f - layer * 0.02f),
                    Color.Transparent
                )
            )
        )
    }
}

private fun DrawScope.drawFrostPatch(patch: FrostPatch, shimmer: Float) {
    val adjustedAlpha = patch.alpha * (0.8f + shimmer * 0.4f)
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE1F5FE).copy(alpha = adjustedAlpha),
                Color(0xFFB3E5FC).copy(alpha = adjustedAlpha * 0.5f),
                Color.Transparent
            )
        ),
        radius = patch.size,
        center = Offset(patch.x, patch.y)
    )
}

private fun DrawScope.drawIceCrystalEnhanced(crystal: IceCrystal, shimmer: Float, frame: Long) {
    if (crystal.size < 5) return
    
    rotate(crystal.rotation, pivot = Offset(crystal.x, crystal.y)) {
        // 绘制冰晶分支
        for (i in 0 until crystal.branches) {
            val angle = 360f / crystal.branches * i
            rotate(angle, pivot = Offset(crystal.x, crystal.y)) {
                // 主分支
                val branchGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE1F5FE).copy(alpha = 0.9f),
                        Color(0xFFB3E5FC).copy(alpha = 0.6f),
                        Color(0xFF81D4FA).copy(alpha = 0.3f)
                    ),
                    start = Offset(crystal.x, crystal.y),
                    end = Offset(crystal.x, crystal.y - crystal.size)
                )
                
                drawLine(
                    brush = branchGradient,
                    start = Offset(crystal.x, crystal.y),
                    end = Offset(crystal.x, crystal.y - crystal.size),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                
                // 侧分支
                val sideLength = crystal.size * 0.4f
                val sideY = crystal.y - crystal.size * 0.5f
                
                drawLine(
                    color = Color(0xFFB3E5FC).copy(alpha = 0.7f),
                    start = Offset(crystal.x, sideY),
                    end = Offset(crystal.x - sideLength * 0.7f, sideY - sideLength * 0.5f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFB3E5FC).copy(alpha = 0.7f),
                    start = Offset(crystal.x, sideY),
                    end = Offset(crystal.x + sideLength * 0.7f, sideY - sideLength * 0.5f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                
                // 更小的分支
                val smallY = crystal.y - crystal.size * 0.7f
                val smallLength = crystal.size * 0.25f
                drawLine(
                    color = Color(0xFF81D4FA).copy(alpha = 0.5f),
                    start = Offset(crystal.x, smallY),
                    end = Offset(crystal.x - smallLength, smallY - smallLength * 0.3f),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF81D4FA).copy(alpha = 0.5f),
                    start = Offset(crystal.x, smallY),
                    end = Offset(crystal.x + smallLength, smallY - smallLength * 0.3f),
                    strokeWidth = 1.5f
                )
            }
        }
        
        // 中心发光
        val glowAlpha = 0.4f + shimmer * 0.3f + sin(frame * 0.05f) * 0.1f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = glowAlpha),
                    Color(0xFF81D4FA).copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                )
            ),
            radius = crystal.size * 0.35f,
            center = Offset(crystal.x, crystal.y)
        )
    }
}

private fun DrawScope.drawSnowflakeEnhanced(flake: Snowflake, frame: Long) {
    val twinkle = (sin(frame * 0.08f + flake.x * 0.01f) + 1) / 2 * 0.3f
    val alpha = (flake.alpha + twinkle).coerceIn(0f, 1f)
    
    rotate(flake.rotation, pivot = Offset(flake.x, flake.y)) {
        when (flake.type) {
            0 -> {
                // 简单圆形雪花
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha),
                            Color(0xFFE1F5FE).copy(alpha = alpha * 0.5f)
                        )
                    ),
                    radius = flake.size,
                    center = Offset(flake.x, flake.y)
                )
            }
            1 -> {
                // 六角形雪花
                for (i in 0..5) {
                    val angle = 60f * i
                    rotate(angle, pivot = Offset(flake.x, flake.y)) {
                        drawLine(
                            color = Color.White.copy(alpha = alpha),
                            start = Offset(flake.x, flake.y),
                            end = Offset(flake.x, flake.y - flake.size * 2.5f),
                            strokeWidth = 1.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.8f),
                    radius = flake.size * 0.4f,
                    center = Offset(flake.x, flake.y)
                )
            }
            2 -> {
                // 星形雪花
                for (i in 0..7) {
                    val angle = 45f * i
                    val length = if (i % 2 == 0) flake.size * 2 else flake.size * 1.3f
                    rotate(angle, pivot = Offset(flake.x, flake.y)) {
                        drawLine(
                            color = Color.White.copy(alpha = alpha * 0.9f),
                            start = Offset(flake.x, flake.y),
                            end = Offset(flake.x, flake.y - length),
                            strokeWidth = 1f
                        )
                    }
                }
            }
            else -> {
                // 复杂雪花
                for (i in 0..5) {
                    val angle = 60f * i
                    rotate(angle, pivot = Offset(flake.x, flake.y)) {
                        drawLine(
                            color = Color.White.copy(alpha = alpha),
                            start = Offset(flake.x, flake.y),
                            end = Offset(flake.x, flake.y - flake.size * 2),
                            strokeWidth = 1f
                        )
                        // 小分支
                        val branchY = flake.y - flake.size
                        drawLine(
                            color = Color.White.copy(alpha = alpha * 0.7f),
                            start = Offset(flake.x, branchY),
                            end = Offset(flake.x - flake.size * 0.5f, branchY - flake.size * 0.3f),
                            strokeWidth = 0.8f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = alpha * 0.7f),
                            start = Offset(flake.x, branchY),
                            end = Offset(flake.x + flake.size * 0.5f, branchY - flake.size * 0.3f),
                            strokeWidth = 0.8f
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawTouchFrostEnhanced(frost: TouchFrost, frame: Long) {
    // 霜冻扩散圈
    for (i in 0..3) {
        val ringRadius = frost.radius - i * 20
        if (ringRadius > 0) {
            val ringAlpha = frost.alpha * (1f - i * 0.2f)
            drawCircle(
                color = Color(0xFFE1F5FE).copy(alpha = ringAlpha * 0.5f),
                radius = ringRadius,
                center = Offset(frost.x, frost.y),
                style = Stroke(width = 3f - i * 0.5f)
            )
        }
    }
    
    // 冰晶纹理
    val crystalCount = (frost.radius / 20).toInt().coerceAtMost(12)
    for (i in 0 until crystalCount) {
        val angle = 360f / crystalCount * i + frame * 0.3f
        val length = frost.radius * 0.8f
        rotate(angle, pivot = Offset(frost.x, frost.y)) {
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = frost.alpha * 0.8f),
                        Color(0xFF81D4FA).copy(alpha = frost.alpha * 0.3f),
                        Color.Transparent
                    ),
                    start = Offset(frost.x, frost.y),
                    end = Offset(frost.x, frost.y - length)
                ),
                start = Offset(frost.x, frost.y),
                end = Offset(frost.x, frost.y - length),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
    
    // 中心光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = frost.alpha * 0.6f),
                Color(0xFFE1F5FE).copy(alpha = frost.alpha * 0.3f),
                Color.Transparent
            )
        ),
        radius = frost.radius * 0.3f,
        center = Offset(frost.x, frost.y)
    )
}

private fun DrawScope.drawFrostEdgeEnhanced(width: Float, height: Float, shimmer: Float, frame: Long) {
    val edgeWidth = 80f
    val alpha = 0.3f + shimmer * 0.15f
    
    // 左边缘
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFE1F5FE).copy(alpha = alpha),
                Color(0xFFB3E5FC).copy(alpha = alpha * 0.5f),
                Color.Transparent
            ),
            startX = 0f,
            endX = edgeWidth
        ),
        size = Size(edgeWidth, height)
    )
    
    // 右边缘
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFB3E5FC).copy(alpha = alpha * 0.5f),
                Color(0xFFE1F5FE).copy(alpha = alpha)
            ),
            startX = width - edgeWidth,
            endX = width
        ),
        topLeft = Offset(width - edgeWidth, 0f),
        size = Size(edgeWidth, height)
    )
    
    // 顶部边缘（更浓）
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE1F5FE).copy(alpha = alpha * 1.5f),
                Color(0xFFB3E5FC).copy(alpha = alpha),
                Color.Transparent
            ),
            startY = 0f,
            endY = edgeWidth * 1.5f
        ),
        size = Size(width, edgeWidth * 1.5f)
    )
    
    // 边缘冰晶装饰
    val random = Random(789)
    repeat(20) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * edgeWidth
        val crystalSize = random.nextFloat() * 15 + 5
        
        for (i in 0..5) {
            val angle = 60f * i
            rotate(angle, pivot = Offset(x, y)) {
                drawLine(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    start = Offset(x, y),
                    end = Offset(x, y - crystalSize),
                    strokeWidth = 1f
                )
            }
        }
    }
}

private fun DrawScope.drawIceReflection(width: Float, height: Float, shimmer: Float) {
    val reflectionY = height * 0.9f
    
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF81D4FA).copy(alpha = 0.1f + shimmer * 0.05f),
                Color(0xFFB3E5FC).copy(alpha = 0.15f + shimmer * 0.08f)
            ),
            startY = reflectionY,
            endY = height
        ),
        topLeft = Offset(0f, reflectionY),
        size = Size(width, height - reflectionY)
    )
}
