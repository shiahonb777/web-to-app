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
 * 高端主题背景系统 - 终极版
 * 每个主题都具有：
 * - 多层视差效果
 * - 物理粒子系统
 * - 高级触觉反馈
 * - 传感器交互
 * - 流畅动画
 * - 精致视觉效果
 * ============================================================
 */


// ==================== 森林晨曦 - Forest ====================

/**
 * 森林主题 - 终极版
 * 特性：
 * - 多层视差森林剪影（5层深度）
 * - 体积光/丁达尔效应
 * - 物理萤火虫群（群聚行为）
 * - 飘落树叶（真实物理）
 * - 雾气流动（柏林噪声）
 * - 光斑闪烁
 * - 触摸产生萤火虫爆发
 * - 摇晃触发树叶飘落
 * - 陀螺仪控制视差
 */
@Composable
fun ForestEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "forest")
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    var lastFrameTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // 粒子系统
    var fireflies by remember { mutableStateOf(listOf<ForestFirefly>()) }
    var leaves by remember { mutableStateOf(listOf<ForestLeaf>()) }
    var lightSpeckles by remember { mutableStateOf(listOf<LightSpeckle>()) }
    var touchRipples by remember { mutableStateOf(listOf<ForestRipple>()) }
    
    // 传感器数据
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var shakeIntensity by remember { mutableFloatStateOf(0f) }
    
    // 风力
    var windForce by remember { mutableFloatStateOf(0f) }
    var windTarget by remember { mutableFloatStateOf(0f) }
    
    // 柏林噪声
    val noise = remember { PerlinNoiseGenerator() }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // 传感器监听
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastX = 0f; var lastY = 0f; var lastZ = 0f
        var lastShakeTime = 0L
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // 平滑倾斜
                tiltX = tiltX * 0.9f + event.values[0] * 0.1f
                tiltY = tiltY * 0.9f + event.values[1] * 0.1f
                
                // 摇晃检测
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 100) {
                    val deltaX = abs(event.values[0] - lastX)
                    val deltaY = abs(event.values[1] - lastY)
                    val deltaZ = abs(event.values[2] - lastZ)
                    val acceleration = deltaX + deltaY + deltaZ
                    
                    if (acceleration > 12) {
                        shakeIntensity = (acceleration - 12).coerceAtMost(20f)
                        haptic.performHaptic(HapticType.IMPACT_MEDIUM, shakeIntensity / 20f)
                        
                        // 摇晃产生树叶
                        if (canvasSize.width > 0) {
                            val newLeaves = List((shakeIntensity * 1.5f).toInt()) {
                                createForestLeaf(canvasSize, fromTree = true)
                            }
                            leaves = (leaves + newLeaves).takeLast(100)
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
    
    // 动画相位
    val sunRayPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunRay"
    )
    
    val fogPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Restart),
        label = "fog"
    )
    
    // 主循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            // Initialize粒子 - 减少数量以提升性能
            fireflies = List(25) { createForestFirefly(canvasSize) }
            leaves = List(12) { createForestLeaf(canvasSize) }
            lightSpeckles = List(20) { createLightSpeckle(canvasSize) }
            
            while (true) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = ((currentTime - lastFrameTime) / 1000f).coerceAtMost(0.05f)
                lastFrameTime = currentTime
                frame++
                
                // Update风力
                if (frame % 120 == 0L) {
                    windTarget = (Random.nextFloat() - 0.5f) * 3f
                }
                windForce = windForce * 0.98f + windTarget * 0.02f
                
                // Update萤火虫（群聚行为）
                fireflies = updateFirefliesWithFlocking(fireflies, canvasSize, tiltX, frame, touchRipples)
                
                // Update树叶
                leaves = leaves.mapNotNull { leaf ->
                    updateForestLeaf(leaf, canvasSize, windForce, tiltX, deltaTime)
                }
                
                // 补充树叶 - 降低上限
                if (leaves.size < 10 && Random.nextFloat() < 0.03f) {
                    leaves = leaves + createForestLeaf(canvasSize)
                }
                
                // Update光斑
                lightSpeckles = lightSpeckles.map { updateLightSpeckle(it, frame) }
                
                // Update涟漪
                touchRipples = touchRipples.mapNotNull { ripple ->
                    val newRipple = ripple.copy(
                        radius = ripple.radius + 3f,
                        alpha = ripple.alpha - 0.015f
                    )
                    if (newRipple.alpha > 0) newRipple else null
                }
                
                // 衰减摇晃
                if (shakeIntensity > 0) shakeIntensity *= 0.95f
                
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
                
                // 添加涟漪
                touchRipples = touchRipples + ForestRipple(offset.x, offset.y, 0f, 1f)
                
                // 萤火虫爆发
                val burstFireflies = List(15) {
                    val angle = Random.nextFloat() * 2 * PI.toFloat()
                    val speed = Random.nextFloat() * 80f + 40f
                    ForestFirefly(
                        x = offset.x,
                        y = offset.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        targetX = offset.x + (Random.nextFloat() - 0.5f) * 200f,
                        targetY = offset.y + (Random.nextFloat() - 0.5f) * 200f,
                        glowPhase = Random.nextFloat() * PI.toFloat() * 2,
                        size = Random.nextFloat() * 5f + 4f,
                        brightness = 1f,
                        color = forestFireflyColors.random()
                    )
                }
                fireflies = (fireflies + burstFireflies).takeLast(100)
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // === 背景渐变 ===
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A1A0F),
                    Color(0xFF0F2818),
                    Color(0xFF1A3D25),
                    Color(0xFF2D5A3A),
                    Color(0xFF3D7A4A)
                )
            )
        )
        
        // === 多层雾气 ===
        drawForestFogLayers(width, height, fogPhase, noise, frame)
        
        // === 体积光/丁达尔效应 ===
        drawVolumetricLightRays(width, height, sunRayPhase, tiltX, tiltY)
        
        // === 多层森林剪影（视差） ===
        drawForestSilhouetteLayers(width, height, frame, tiltX, windForce)
        
        // === 光斑 ===
        lightSpeckles.forEach { speckle ->
            drawForestLightSpeckle(speckle, tiltX, tiltY)
        }
        
        // === 飘落树叶 ===
        leaves.sortedBy { it.depth }.forEach { leaf ->
            drawForestLeafEnhanced(leaf)
        }
        
        // === 萤火虫 ===
        fireflies.forEach { firefly ->
            drawForestFireflyEnhanced(firefly, frame)
        }
        
        // === 触摸涟漪 ===
        touchRipples.forEach { ripple ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB7E4C7).copy(alpha = ripple.alpha * 0.4f),
                        Color(0xFF74C69D).copy(alpha = ripple.alpha * 0.2f),
                        Color.Transparent
                    )
                ),
                radius = ripple.radius,
                center = Offset(ripple.x, ripple.y)
            )
        }
        
        // === 前景雾气 ===
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF1A3D25).copy(alpha = 0.3f)
                ),
                startY = height * 0.7f,
                endY = height
            )
        )
    }
}

// 森林主题数据类
data class ForestFirefly(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var targetX: Float, var targetY: Float,
    var glowPhase: Float, var size: Float,
    var brightness: Float, var color: Color
)

data class ForestLeaf(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var rotation: Float, var rotationSpeed: Float,
    var size: Float, var swayPhase: Float,
    var depth: Float, var color: Color, var alpha: Float
)

data class LightSpeckle(
    var x: Float, var y: Float,
    var size: Float, var alpha: Float,
    var phase: Float, var flickerSpeed: Float
)

data class ForestRipple(
    val x: Float, val y: Float,
    var radius: Float, var alpha: Float
)

private val forestFireflyColors = listOf(
    Color(0xFFB7E4C7), Color(0xFF74C69D), Color(0xFFD8F3DC),
    Color(0xFF95D5B2), Color(0xFFA7E8BD)
)

private val forestLeafColors = listOf(
    Color(0xFF2D6A4F), Color(0xFF40916C), Color(0xFF52B788),
    Color(0xFF74C69D), Color(0xFF95D5B2), Color(0xFF1B4332)
)

private fun createForestFirefly(canvasSize: Size): ForestFirefly {
    val x = Random.nextFloat() * canvasSize.width
    val y = Random.nextFloat() * canvasSize.height * 0.7f
    return ForestFirefly(
        x = x, y = y,
        vx = (Random.nextFloat() - 0.5f) * 30f,
        vy = (Random.nextFloat() - 0.5f) * 20f,
        targetX = x + (Random.nextFloat() - 0.5f) * 150f,
        targetY = y + (Random.nextFloat() - 0.5f) * 100f,
        glowPhase = Random.nextFloat() * PI.toFloat() * 2,
        size = Random.nextFloat() * 4f + 2f,
        brightness = Random.nextFloat() * 0.5f + 0.5f,
        color = forestFireflyColors.random()
    )
}

private fun createForestLeaf(canvasSize: Size, fromTree: Boolean = false): ForestLeaf {
    val depth = Random.nextFloat()
    return ForestLeaf(
        x = if (fromTree) canvasSize.width * 0.15f + (Random.nextFloat() - 0.5f) * canvasSize.width * 0.3f
            else Random.nextFloat() * canvasSize.width * 1.2f - canvasSize.width * 0.1f,
        y = if (fromTree) canvasSize.height * 0.2f + Random.nextFloat() * canvasSize.height * 0.2f
            else -Random.nextFloat() * 50f - 20f,
        vx = (Random.nextFloat() - 0.5f) * 20f,
        vy = Random.nextFloat() * 30f + 20f,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 120f,
        size = (Random.nextFloat() * 12f + 8f) * (0.6f + depth * 0.4f),
        swayPhase = Random.nextFloat() * PI.toFloat() * 2,
        depth = depth,
        color = forestLeafColors.random(),
        alpha = 0.7f + depth * 0.3f
    )
}

private fun createLightSpeckle(canvasSize: Size): LightSpeckle {
    return LightSpeckle(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height * 0.6f,
        size = Random.nextFloat() * 25f + 10f,
        alpha = Random.nextFloat() * 0.25f + 0.1f,
        phase = Random.nextFloat() * PI.toFloat() * 2,
        flickerSpeed = Random.nextFloat() * 0.03f + 0.01f
    )
}


// 萤火虫群聚行为更新
private fun updateFirefliesWithFlocking(
    fireflies: List<ForestFirefly>,
    canvasSize: Size,
    tiltX: Float,
    frame: Long,
    ripples: List<ForestRipple>
): List<ForestFirefly> {
    return fireflies.map { firefly ->
        var newFirefly = firefly.copy()
        
        // 群聚行为参数
        val separationRadius = 30f
        val alignmentRadius = 60f
        val cohesionRadius = 100f
        
        var separation = Vec2.ZERO
        var alignment = Vec2.ZERO
        var cohesion = Vec2.ZERO
        var neighborCount = 0
        
        // 计算群聚力
        fireflies.forEach { other ->
            if (other !== firefly) {
                val dx = other.x - firefly.x
                val dy = other.y - firefly.y
                val dist = sqrt(dx * dx + dy * dy)
                
                if (dist < separationRadius && dist > 0) {
                    separation = separation - Vec2(dx / dist, dy / dist) * (separationRadius - dist)
                }
                if (dist < alignmentRadius) {
                    alignment = alignment + Vec2(other.vx, other.vy)
                    neighborCount++
                }
                if (dist < cohesionRadius) {
                    cohesion = cohesion + Vec2(other.x, other.y)
                }
            }
        }
        
        if (neighborCount > 0) {
            alignment = alignment / neighborCount.toFloat()
            cohesion = cohesion / neighborCount.toFloat() - Vec2(firefly.x, firefly.y)
        }
        
        // 向目标点移动
        val toTarget = Vec2(firefly.targetX - firefly.x, firefly.targetY - firefly.y)
        val targetForce = toTarget.normalized() * 15f
        
        // 涟漪排斥
        var rippleForce = Vec2.ZERO
        ripples.forEach { ripple ->
            val dx = firefly.x - ripple.x
            val dy = firefly.y - ripple.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < ripple.radius + 50 && dist > ripple.radius - 20 && dist > 0) {
                rippleForce = rippleForce + Vec2(dx / dist, dy / dist) * 100f * ripple.alpha
            }
        }
        
        // 合并所有力
        val totalForce = separation * 2f + alignment * 0.5f + cohesion.normalized() * 0.3f + 
                        targetForce + rippleForce + Vec2(-tiltX * 5f, 0f)
        
        // Update速度和位置
        newFirefly = newFirefly.copy(
            vx = (firefly.vx + totalForce.x * 0.1f) * 0.95f,
            vy = (firefly.vy + totalForce.y * 0.1f) * 0.95f
        )
        
        newFirefly = newFirefly.copy(
            x = firefly.x + newFirefly.vx * 0.016f,
            y = firefly.y + newFirefly.vy * 0.016f + sin(frame * 0.03f + firefly.glowPhase) * 0.3f,
            glowPhase = firefly.glowPhase + 0.08f
        )
        
        // Update目标点
        if (abs(newFirefly.x - newFirefly.targetX) < 30 && abs(newFirefly.y - newFirefly.targetY) < 30) {
            newFirefly = newFirefly.copy(
                targetX = Random.nextFloat() * canvasSize.width,
                targetY = Random.nextFloat() * canvasSize.height * 0.6f
            )
        }
        
        // 边界处理
        if (newFirefly.x < -30) newFirefly = newFirefly.copy(x = canvasSize.width + 30)
        if (newFirefly.x > canvasSize.width + 30) newFirefly = newFirefly.copy(x = -30f)
        if (newFirefly.y < -30) newFirefly = newFirefly.copy(y = canvasSize.height * 0.6f)
        if (newFirefly.y > canvasSize.height * 0.7f) newFirefly = newFirefly.copy(y = -30f)
        
        newFirefly
    }
}

private fun updateForestLeaf(
    leaf: ForestLeaf, canvasSize: Size,
    windForce: Float, tiltX: Float, deltaTime: Float
): ForestLeaf? {
    val gravity = 50f + leaf.depth * 30f
    val sway = sin(leaf.swayPhase) * 40f * (1f - leaf.depth * 0.3f)
    val windEffect = windForce * 30f * (1f - leaf.depth * 0.5f)
    
    val newLeaf = leaf.copy(
        x = leaf.x + (leaf.vx + sway + windEffect - tiltX * 10f) * deltaTime,
        y = leaf.y + (leaf.vy + gravity * deltaTime) * deltaTime,
        vx = leaf.vx * 0.99f,
        vy = (leaf.vy + gravity * deltaTime).coerceAtMost(150f),
        rotation = leaf.rotation + leaf.rotationSpeed * deltaTime,
        swayPhase = leaf.swayPhase + 0.05f + leaf.depth * 0.02f
    )
    
    return if (newLeaf.y > canvasSize.height + 50 || newLeaf.x < -100 || newLeaf.x > canvasSize.width + 100) {
        null
    } else newLeaf
}

private fun updateLightSpeckle(speckle: LightSpeckle, frame: Long): LightSpeckle {
    val flicker = sin(frame * speckle.flickerSpeed + speckle.phase)
    return speckle.copy(
        alpha = (0.15f + flicker * 0.15f).coerceIn(0.05f, 0.4f)
    )
}

// 绘制函数
private fun DrawScope.drawForestFogLayers(
    width: Float, height: Float, phase: Float,
    noise: PerlinNoiseGenerator, frame: Long
) {
    val fogLayers = listOf(
        Triple(0.4f, 0.12f, 0.8f),
        Triple(0.55f, 0.08f, 0.6f),
        Triple(0.7f, 0.05f, 0.4f)
    )
    
    fogLayers.forEachIndexed { index, (yRatio, alpha, speed) ->
        val baseY = height * yRatio
        val noiseOffset = noise.fbm(phase * speed + index, frame * 0.0005f) * 50f
        
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF1A3D25).copy(alpha = alpha),
                    Color(0xFF0F2818).copy(alpha = alpha * 0.5f),
                    Color.Transparent
                ),
                startY = baseY - 100 + noiseOffset,
                endY = baseY + 100 + noiseOffset
            )
        )
    }
}

private fun DrawScope.drawVolumetricLightRays(
    width: Float, height: Float, phase: Float,
    tiltX: Float, tiltY: Float
) {
    val rayCount = 8
    val sourceX = width * 0.85f + tiltX * 30f
    val sourceY = -height * 0.1f + tiltY * 20f
    
    for (i in 0 until rayCount) {
        val baseAngle = (i.toFloat() / rayCount) * 0.6f + 0.2f
        val angleVariation = sin(phase + i * 0.5f) * 0.05f
        val angle = (baseAngle + angleVariation) * PI.toFloat()
        
        val rayLength = height * 1.5f
        val rayWidth = 0.06f + sin(phase * 0.5f + i) * 0.02f
        
        val path = Path().apply {
            moveTo(sourceX, sourceY)
            lineTo(
                sourceX + cos(angle - rayWidth) * rayLength,
                sourceY + sin(angle - rayWidth) * rayLength
            )
            lineTo(
                sourceX + cos(angle + rayWidth) * rayLength,
                sourceY + sin(angle + rayWidth) * rayLength
            )
            close()
        }
        
        val intensity = 0.08f + sin(phase * 2 + i * 0.7f) * 0.03f
        
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = intensity),
                    Color(0xFF95D5B2).copy(alpha = intensity * 0.5f),
                    Color.Transparent
                ),
                center = Offset(sourceX, sourceY),
                radius = rayLength
            )
        )
    }
}

private fun DrawScope.drawForestSilhouetteLayers(
    width: Float, height: Float, frame: Long,
    tiltX: Float, windForce: Float
) {
    // 5层森林剪影，从远到近
    val layers = listOf(
        ForestLayer(0.5f, 0.15f, Color(0xFF0A1A0F), 0.2f),
        ForestLayer(0.55f, 0.25f, Color(0xFF0D1F14), 0.35f),
        ForestLayer(0.6f, 0.35f, Color(0xFF102518), 0.5f),
        ForestLayer(0.65f, 0.5f, Color(0xFF152D1E), 0.7f),
        ForestLayer(0.7f, 0.7f, Color(0xFF1A3D25), 1f)
    )
    
    layers.forEach { layer ->
        val parallaxOffset = tiltX * layer.parallax * 30f
        val windSway = sin(frame * 0.01f * layer.parallax) * windForce * 10f * layer.parallax
        
        val treePath = Path().apply {
            moveTo(-50f + parallaxOffset, height)
            
            var x = -50f + parallaxOffset
            while (x < width + 100) {
                val treeHeight = height * (0.3f + layer.heightVariation * Random(x.toInt()).nextFloat() * 0.3f)
                val treeWidth = 30f + Random(x.toInt() + 1).nextFloat() * 40f
                
                // 树的形状
                lineTo(x, height - treeHeight * 0.1f)
                quadraticBezierTo(
                    x + treeWidth * 0.3f + windSway, height - treeHeight,
                    x + treeWidth * 0.5f + windSway * 0.5f, height - treeHeight * 0.3f
                )
                quadraticBezierTo(
                    x + treeWidth * 0.7f + windSway * 0.3f, height - treeHeight * 0.8f,
                    x + treeWidth, height - treeHeight * 0.1f
                )
                
                x += treeWidth + Random(x.toInt() + 2).nextFloat() * 20f
            }
            
            lineTo(width + 100, height)
            close()
        }
        
        drawPath(path = treePath, color = layer.color)
    }
}

private data class ForestLayer(
    val baseY: Float, val heightVariation: Float,
    val color: Color, val parallax: Float
)

private fun DrawScope.drawForestLightSpeckle(speckle: LightSpeckle, tiltX: Float, tiltY: Float) {
    val offsetX = tiltX * 5f
    val offsetY = tiltY * 3f
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = speckle.alpha),
                Color(0xFFFFD700).copy(alpha = speckle.alpha * 0.3f),
                Color.Transparent
            )
        ),
        radius = speckle.size,
        center = Offset(speckle.x + offsetX, speckle.y + offsetY)
    )
}

private fun DrawScope.drawForestLeafEnhanced(leaf: ForestLeaf) {
    val scale = 0.6f + leaf.depth * 0.4f
    
    rotate(leaf.rotation, pivot = Offset(leaf.x, leaf.y)) {
        // 叶子阴影
        val shadowOffset = 2f * scale
        drawLeafShape(
            leaf.x + shadowOffset, leaf.y + shadowOffset,
            leaf.size, Color.Black.copy(alpha = 0.15f * leaf.alpha)
        )
        
        // 叶子主体
        drawLeafShape(leaf.x, leaf.y, leaf.size, leaf.color.copy(alpha = leaf.alpha))
        
        // 叶子高光
        drawLeafShape(
            leaf.x - leaf.size * 0.1f, leaf.y - leaf.size * 0.1f,
            leaf.size * 0.5f, Color.White.copy(alpha = leaf.alpha * 0.2f)
        )
        
        // 叶脉
        drawLine(
            color = leaf.color.copy(alpha = leaf.alpha * 0.5f),
            start = Offset(leaf.x, leaf.y - leaf.size * 0.4f),
            end = Offset(leaf.x, leaf.y + leaf.size * 0.4f),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawLeafShape(x: Float, y: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, y - size * 0.5f)
        cubicTo(
            x + size * 0.5f, y - size * 0.3f,
            x + size * 0.4f, y + size * 0.3f,
            x, y + size * 0.5f
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

private fun DrawScope.drawForestFireflyEnhanced(firefly: ForestFirefly, frame: Long) {
    val glow = (sin(firefly.glowPhase) + 1f) / 2f
    val alpha = 0.4f + glow * 0.6f
    val currentSize = firefly.size * (0.8f + glow * 0.4f)
    
    // 外层大光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                firefly.color.copy(alpha = alpha * 0.2f * firefly.brightness),
                firefly.color.copy(alpha = alpha * 0.05f * firefly.brightness),
                Color.Transparent
            )
        ),
        radius = currentSize * 8f,
        center = Offset(firefly.x, firefly.y)
    )
    
    // 中层光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                firefly.color.copy(alpha = alpha * 0.5f * firefly.brightness),
                firefly.color.copy(alpha = alpha * 0.2f * firefly.brightness),
                Color.Transparent
            )
        ),
        radius = currentSize * 4f,
        center = Offset(firefly.x, firefly.y)
    )
    
    // 内层光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.8f * firefly.brightness),
                firefly.color.copy(alpha = alpha * 0.6f * firefly.brightness),
                Color.Transparent
            )
        ),
        radius = currentSize * 2f,
        center = Offset(firefly.x, firefly.y)
    )
    
    // 核心
    drawCircle(
        color = Color.White.copy(alpha = alpha * firefly.brightness),
        radius = currentSize * 0.8f,
        center = Offset(firefly.x, firefly.y)
    )
}


// ==================== 日落余晖 - Sunset ====================

/**
 * 日落主题 - 终极版
 * 特性：
 * - 动态天空渐变（日落进程）
 * - 太阳光晕 + 脉冲效果
 * - 多层云彩（体积云效果）
 * - 山脉剪影（多层视差）
 * - 飞鸟群（群聚算法）
 * - 光粒子系统
 * - 水面反射
 * - 触摸产生光芒爆发
 */
@Composable
fun SunsetEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "sunset")
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    
    // 粒子系统
    var clouds by remember { mutableStateOf(listOf<SunsetCloud>()) }
    var birds by remember { mutableStateOf(listOf<SunsetBird>()) }
    var sunParticles by remember { mutableStateOf(listOf<SunParticle>()) }
    var lightBursts by remember { mutableStateOf(listOf<LightBurst>()) }
    
    // 传感器
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // 日落进程 (0-1)
    val sunsetProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Reverse),
        label = "sunsetProgress"
    )
    
    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "sunPulse"
    )
    
    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudDrift"
    )
    
    // 传感器
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
    
    // 主循环
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0) {
            clouds = List(12) { createSunsetCloud(canvasSize, it) }
            birds = List(20) { createSunsetBird(canvasSize) }
            sunParticles = List(60) { createSunParticle(canvasSize) }
            
            while (true) {
                frame++
                
                // Update云彩
                clouds = clouds.map { updateSunsetCloud(it, canvasSize, tiltX, cloudDrift) }
                
                // Update飞鸟（群聚行为）
                birds = updateBirdsWithFlocking(birds, canvasSize, frame)
                
                // Update光粒子
                sunParticles = sunParticles.map { updateSunParticle(it, canvasSize, frame) }
                
                // Update光芒爆发
                lightBursts = lightBursts.mapNotNull { burst ->
                    val newBurst = burst.copy(
                        radius = burst.radius + burst.speed,
                        alpha = burst.alpha - 0.02f,
                        speed = burst.speed * 0.98f
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
                haptic.performHaptic(HapticType.SOFT_LANDING)
                onInteraction()
                
                // 光芒爆发
                lightBursts = lightBursts + LightBurst(
                    x = offset.x, y = offset.y,
                    radius = 0f, alpha = 1f, speed = 8f
                )
                
                // 粒子爆发
                val newParticles = List(20) {
                    val angle = Random.nextFloat() * 2 * PI.toFloat()
                    val speed = Random.nextFloat() * 100f + 50f
                    SunParticle(
                        x = offset.x, y = offset.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        size = Random.nextFloat() * 6f + 3f,
                        alpha = 1f,
                        color = sunsetParticleColors.random(),
                        life = 1f
                    )
                }
                sunParticles = (sunParticles + newParticles).takeLast(100)
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // === 动态天空渐变 ===
        val skyColors = getSunsetSkyColors(sunsetProgress)
        drawRect(brush = Brush.verticalGradient(colors = skyColors))
        
        // === 太阳 ===
        val sunY = height * (0.35f + sunsetProgress * 0.25f)
        val sunX = width * 0.7f + tiltX * 20f
        val sunRadius = min(width, height) * 0.1f
        drawSunWithGlow(sunX, sunY, sunRadius, sunPulse, sunsetProgress)
        
        // === 云彩 ===
        clouds.sortedBy { it.depth }.forEach { cloud ->
            drawVolumetricCloud(cloud, sunX, sunY, sunsetProgress)
        }
        
        // === 光粒子 ===
        sunParticles.forEach { particle ->
            drawSunParticleEnhanced(particle)
        }
        
        // === 山脉剪影 ===
        drawMountainLayers(width, height, tiltX, sunsetProgress)
        
        // === 水面反射 ===
        drawWaterReflection(width, height, sunX, sunY, sunRadius, sunsetProgress, frame)
        
        // === 飞鸟 ===
        birds.forEach { bird ->
            drawSunsetBird(bird, frame)
        }
        
        // === 光芒爆发 ===
        lightBursts.forEach { burst ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = burst.alpha * 0.6f),
                        Color(0xFFFF8C00).copy(alpha = burst.alpha * 0.3f),
                        Color.Transparent
                    )
                ),
                radius = burst.radius,
                center = Offset(burst.x, burst.y)
            )
        }
    }
}

// 日落主题数据类
data class SunsetCloud(
    var x: Float, var y: Float,
    var width: Float, var height: Float,
    var speed: Float, var depth: Float,
    var segments: List<CloudSegment>
)

data class CloudSegment(
    val offsetX: Float, val offsetY: Float,
    val radiusX: Float, val radiusY: Float
)

data class SunsetBird(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var wingPhase: Float, var size: Float
)

data class SunParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var alpha: Float,
    var color: Color, var life: Float
)

data class LightBurst(
    val x: Float, val y: Float,
    var radius: Float, var alpha: Float, var speed: Float
)

private val sunsetParticleColors = listOf(
    Color(0xFFFFD700), Color(0xFFFF8C00), Color(0xFFFF6347),
    Color(0xFFFFB6C1), Color(0xFFFFA07A), Color(0xFFFFE4B5)
)

private fun getSunsetSkyColors(progress: Float): List<Color> {
    val earlyColors = listOf(
        Color(0xFF87CEEB), Color(0xFFFFE4B5), Color(0xFFFFD700),
        Color(0xFFFF8C00), Color(0xFFFF6347)
    )
    val lateColors = listOf(
        Color(0xFF1A0533), Color(0xFF4A1942), Color(0xFFB33951),
        Color(0xFFE85D04), Color(0xFFFFAA00)
    )
    
    return earlyColors.mapIndexed { index, earlyColor ->
        ColorUtils.lerp(earlyColor, lateColors[index], progress)
    }
}

private fun createSunsetCloud(canvasSize: Size, index: Int): SunsetCloud {
    val depth = (index % 4) / 4f + 0.1f
    val segments = List(Random.nextInt(4) + 3) {
        CloudSegment(
            offsetX = (Random.nextFloat() - 0.5f) * 100f,
            offsetY = (Random.nextFloat() - 0.5f) * 30f,
            radiusX = Random.nextFloat() * 60f + 40f,
            radiusY = Random.nextFloat() * 25f + 15f
        )
    }
    
    return SunsetCloud(
        x = Random.nextFloat() * canvasSize.width * 1.5f - canvasSize.width * 0.25f,
        y = canvasSize.height * (0.1f + index * 0.05f),
        width = Random.nextFloat() * 150f + 100f,
        height = Random.nextFloat() * 40f + 25f,
        speed = (0.3f + depth * 0.5f) * (if (Random.nextBoolean()) 1f else -1f),
        depth = depth,
        segments = segments
    )
}

private fun updateSunsetCloud(cloud: SunsetCloud, canvasSize: Size, tiltX: Float, drift: Float): SunsetCloud {
    var newX = cloud.x + cloud.speed + tiltX * cloud.depth * 3f
    
    if (newX > canvasSize.width + cloud.width) newX = -cloud.width
    if (newX < -cloud.width * 2) newX = canvasSize.width + cloud.width
    
    return cloud.copy(x = newX)
}

private fun createSunsetBird(canvasSize: Size): SunsetBird {
    return SunsetBird(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height * 0.4f + canvasSize.height * 0.1f,
        vx = Random.nextFloat() * 60f + 30f,
        vy = (Random.nextFloat() - 0.5f) * 20f,
        wingPhase = Random.nextFloat() * PI.toFloat() * 2,
        size = Random.nextFloat() * 6f + 4f
    )
}

private fun updateBirdsWithFlocking(birds: List<SunsetBird>, canvasSize: Size, frame: Long): List<SunsetBird> {
    return birds.map { bird ->
        // 简化的群聚行为
        var avgVx = 0f
        var avgVy = 0f
        var count = 0
        
        birds.forEach { other ->
            if (other !== bird) {
                val dx = other.x - bird.x
                val dy = other.y - bird.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 100f) {
                    avgVx += other.vx
                    avgVy += other.vy
                    count++
                }
            }
        }
        
        var newVx = bird.vx
        var newVy = bird.vy
        
        if (count > 0) {
            avgVx /= count
            avgVy /= count
            newVx = bird.vx * 0.95f + avgVx * 0.05f
            newVy = bird.vy * 0.95f + avgVy * 0.05f
        }
        
        // 添加随机扰动
        newVx += (Random.nextFloat() - 0.5f) * 2f
        newVy += (Random.nextFloat() - 0.5f) * 1f
        
        var newX = bird.x + newVx * 0.016f
        var newY = bird.y + newVy * 0.016f + sin(frame * 0.05f + bird.wingPhase) * 0.5f
        
        // 边界处理
        if (newX > canvasSize.width + 50) {
            newX = -30f
            newY = Random.nextFloat() * canvasSize.height * 0.4f + canvasSize.height * 0.1f
        }
        
        bird.copy(
            x = newX, y = newY,
            vx = newVx.coerceIn(20f, 100f),
            vy = newVy.coerceIn(-30f, 30f),
            wingPhase = bird.wingPhase + 0.3f
        )
    }
}

private fun createSunParticle(canvasSize: Size): SunParticle {
    val sunX = canvasSize.width * 0.7f
    val sunY = canvasSize.height * 0.4f
    val angle = Random.nextFloat() * 2 * PI.toFloat()
    val dist = Random.nextFloat() * canvasSize.width * 0.4f
    
    return SunParticle(
        x = sunX + cos(angle) * dist,
        y = sunY + sin(angle) * dist * 0.5f,
        vx = (Random.nextFloat() - 0.5f) * 10f,
        vy = -Random.nextFloat() * 15f - 5f,
        size = Random.nextFloat() * 4f + 2f,
        alpha = Random.nextFloat() * 0.5f + 0.3f,
        color = sunsetParticleColors.random(),
        life = 1f
    )
}

private fun updateSunParticle(particle: SunParticle, canvasSize: Size, frame: Long): SunParticle {
    var newParticle = particle.copy(
        x = particle.x + particle.vx * 0.016f + sin(frame * 0.02f) * 0.5f,
        y = particle.y + particle.vy * 0.016f,
        alpha = particle.alpha - 0.003f,
        life = particle.life - 0.005f
    )
    
    if (newParticle.alpha <= 0 || newParticle.life <= 0) {
        newParticle = createSunParticle(canvasSize)
    }
    
    return newParticle
}


// 日落绘制函数
private fun DrawScope.drawSunWithGlow(
    sunX: Float, sunY: Float, radius: Float,
    pulse: Float, progress: Float
) {
    val pulseScale = 1f + sin(pulse) * 0.03f
    val glowIntensity = 1f - progress * 0.3f
    
    // 最外层光晕
    for (i in 6 downTo 1) {
        val layerRadius = radius * (2f + i * 1.2f) * pulseScale
        val layerAlpha = (0.08f / i) * glowIntensity
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = layerAlpha),
                    Color(0xFFFF8C00).copy(alpha = layerAlpha * 0.5f),
                    Color.Transparent
                )
            ),
            radius = layerRadius,
            center = Offset(sunX, sunY)
        )
    }
    
    // 太阳光芒
    val rayCount = 12
    for (i in 0 until rayCount) {
        val angle = (i.toFloat() / rayCount) * 2 * PI.toFloat() + pulse * 0.1f
        val rayLength = radius * (2.5f + sin(pulse * 2 + i) * 0.5f)
        
        val rayPath = Path().apply {
            moveTo(sunX, sunY)
            lineTo(
                sunX + cos(angle - 0.05f) * rayLength,
                sunY + sin(angle - 0.05f) * rayLength
            )
            lineTo(
                sunX + cos(angle + 0.05f) * rayLength,
                sunY + sin(angle + 0.05f) * rayLength
            )
            close()
        }
        
        drawPath(
            path = rayPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = 0.3f * glowIntensity),
                    Color.Transparent
                ),
                center = Offset(sunX, sunY),
                radius = rayLength
            )
        )
    }
    
    // 太阳主体
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFACD),
                Color(0xFFFFD700),
                Color(0xFFFF8C00),
                Color(0xFFFF6347).copy(alpha = 0.8f)
            )
        ),
        radius = radius * pulseScale,
        center = Offset(sunX, sunY)
    )
}

private fun DrawScope.drawVolumetricCloud(
    cloud: SunsetCloud, sunX: Float, sunY: Float, progress: Float
) {
    val baseColor = ColorUtils.lerp(
        Color(0xFFFFE4B5),
        Color(0xFFFF8C00),
        progress
    )
    val highlightColor = ColorUtils.lerp(
        Color(0xFFFFFFFF),
        Color(0xFFFFD700),
        progress
    )
    
    cloud.segments.forEach { segment ->
        val segX = cloud.x + segment.offsetX
        val segY = cloud.y + segment.offsetY
        
        // 云朵阴影
        drawOval(
            color = Color.Black.copy(alpha = 0.1f * cloud.depth),
            topLeft = Offset(segX - segment.radiusX + 3f, segY - segment.radiusY + 3f),
            size = Size(segment.radiusX * 2, segment.radiusY * 2)
        )
        
        // 云朵主体
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(highlightColor.copy(alpha = 0.9f), baseColor.copy(alpha = 0.7f))
            ),
            topLeft = Offset(segX - segment.radiusX, segY - segment.radiusY),
            size = Size(segment.radiusX * 2, segment.radiusY * 2)
        )
        
        // 云朵高光
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = Offset(segX - segment.radiusX * 0.3f, segY - segment.radiusY * 0.3f)
            ),
            topLeft = Offset(segX - segment.radiusX * 0.8f, segY - segment.radiusY * 0.8f),
            size = Size(segment.radiusX * 1.2f, segment.radiusY * 1.2f)
        )
    }
}

private fun DrawScope.drawMountainLayers(
    width: Float, height: Float, tiltX: Float, progress: Float
) {
    val layers = listOf(
        MountainLayer(0.6f, 0.15f, Color(0xFF2D1B4E).copy(alpha = 0.6f + progress * 0.2f)),
        MountainLayer(0.68f, 0.25f, Color(0xFF1A0533).copy(alpha = 0.7f + progress * 0.2f)),
        MountainLayer(0.75f, 0.4f, Color(0xFF0D0015).copy(alpha = 0.85f + progress * 0.1f))
    )
    
    layers.forEachIndexed { index, layer ->
        val parallax = tiltX * (index + 1) * 5f
        
        val path = Path().apply {
            moveTo(-50f + parallax, height)
            
            var x = -50f + parallax
            val seed = index * 1000
            while (x < width + 100) {
                val peakHeight = height * layer.heightRatio * (0.7f + Random(seed + x.toInt()).nextFloat() * 0.6f)
                val peakWidth = Random(seed + x.toInt() + 1).nextFloat() * 150f + 100f
                
                lineTo(x, height - peakHeight * 0.1f)
                quadraticBezierTo(
                    x + peakWidth * 0.5f, height - peakHeight,
                    x + peakWidth, height - peakHeight * 0.1f
                )
                
                x += peakWidth * 0.8f
            }
            
            lineTo(width + 100, height)
            close()
        }
        
        drawPath(path = path, color = layer.color)
    }
}

private data class MountainLayer(
    val baseY: Float, val heightRatio: Float, val color: Color
)

private fun DrawScope.drawWaterReflection(
    width: Float, height: Float,
    sunX: Float, sunY: Float, sunRadius: Float,
    progress: Float, frame: Long
) {
    val waterY = height * 0.82f
    
    // 水面基础
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A0533).copy(alpha = 0.3f),
                Color(0xFF0D0015).copy(alpha = 0.6f)
            ),
            startY = waterY,
            endY = height
        ),
        topLeft = Offset(0f, waterY),
        size = Size(width, height - waterY)
    )
    
    // 太阳反射
    val reflectionY = waterY + (height - waterY) * 0.3f
    val waveOffset = sin(frame * 0.03f) * 5f
    
    for (i in 0 until 8) {
        val segmentY = reflectionY + i * 8f + waveOffset * (i * 0.2f)
        val segmentWidth = sunRadius * (2f - i * 0.15f)
        val segmentAlpha = (0.4f - i * 0.04f) * (1f - progress * 0.3f)
        
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFD700).copy(alpha = segmentAlpha),
                    Color(0xFFFF8C00).copy(alpha = segmentAlpha),
                    Color.Transparent
                )
            ),
            topLeft = Offset(sunX - segmentWidth, segmentY - 3f),
            size = Size(segmentWidth * 2, 6f)
        )
    }
    
    // 水面波纹
    for (i in 0 until 5) {
        val waveY = waterY + 10f + i * 15f
        val waveAlpha = 0.1f - i * 0.015f
        
        drawLine(
            color = Color.White.copy(alpha = waveAlpha),
            start = Offset(0f, waveY + sin(frame * 0.02f + i) * 3f),
            end = Offset(width, waveY + sin(frame * 0.02f + i + 2f) * 3f),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawSunsetBird(bird: SunsetBird, frame: Long) {
    val wingAngle = sin(bird.wingPhase) * 35f
    val color = Color(0xFF1A0533)
    
    // 身体
    drawLine(
        color = color,
        start = Offset(bird.x - bird.size, bird.y),
        end = Offset(bird.x + bird.size, bird.y),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    
    // 翅膀
    val wingY = bird.y - bird.size * 0.4f * sin((wingAngle + 45) * PI.toFloat() / 180f)
    
    drawLine(
        color = color,
        start = Offset(bird.x - bird.size * 0.2f, bird.y),
        end = Offset(bird.x - bird.size, wingY),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    
    drawLine(
        color = color,
        start = Offset(bird.x + bird.size * 0.2f, bird.y),
        end = Offset(bird.x + bird.size, wingY),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSunParticleEnhanced(particle: SunParticle) {
    // 光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                particle.color.copy(alpha = particle.alpha * 0.6f),
                particle.color.copy(alpha = particle.alpha * 0.2f),
                Color.Transparent
            )
        ),
        radius = particle.size * 4f,
        center = Offset(particle.x, particle.y)
    )
    
    // 核心
    drawCircle(
        color = Color.White.copy(alpha = particle.alpha * 0.9f),
        radius = particle.size * 0.6f,
        center = Offset(particle.x, particle.y)
    )
}


// ==================== 极简主义 - Minimal ====================

/**
 * 极简主题 - 终极版
 * 特性：
 * - 呼吸式网格（波纹扩散）
 * - 中心光晕（脉冲呼吸）
 * - 几何粒子（圆/方/三角）
 * - 连线效果（粒子间连接）
 * - 触摸涟漪（多层扩散）
 * - 极致简约美学
 */
@Composable
fun MinimalEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "minimal")
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    
    var geometricParticles by remember { mutableStateOf(listOf<MinimalParticle>()) }
    var pulseRings by remember { mutableStateOf(listOf<MinimalRing>()) }
    var touchPoints by remember { mutableStateOf(listOf<MinimalTouch>()) }
    
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "breathe"
    )
    
    val gridPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "gridPulse"
    )
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = tiltX * 0.92f + event.values[0] * 0.08f
                tiltY = tiltY * 0.92f + event.values[1] * 0.08f
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
            geometricParticles = List(25) { createMinimalParticle(canvasSize) }
            
            while (true) {
                frame++
                
                geometricParticles = geometricParticles.map { 
                    updateMinimalParticle(it, canvasSize, tiltX, tiltY, frame) 
                }
                
                pulseRings = pulseRings.mapNotNull { ring ->
                    val newRing = ring.copy(
                        radius = ring.radius + 2.5f,
                        alpha = ring.alpha - 0.008f
                    )
                    if (newRing.alpha > 0) newRing else null
                }
                
                touchPoints = touchPoints.mapNotNull { touch ->
                    val newTouch = touch.copy(life = touch.life - 0.015f)
                    if (newTouch.life > 0) newTouch else null
                }
                
                // 定期产生脉冲环
                if (frame % 200 == 0L) {
                    pulseRings = pulseRings + MinimalRing(
                        canvasSize.width / 2 + tiltX * 20f,
                        canvasSize.height / 2 + tiltY * 15f,
                        0f, 0.5f
                    )
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
                haptic.performHaptic(HapticType.SELECTION)
                onInteraction()
                
                // 多层涟漪
                pulseRings = pulseRings + listOf(
                    MinimalRing(offset.x, offset.y, 0f, 0.8f),
                    MinimalRing(offset.x, offset.y, 0f, 0.6f),
                    MinimalRing(offset.x, offset.y, 0f, 0.4f)
                )
                
                touchPoints = touchPoints + MinimalTouch(offset.x, offset.y, 1f)
                
                // 粒子爆发
                val newParticles = List(6) {
                    val angle = Random.nextFloat() * 2 * PI.toFloat()
                    val speed = Random.nextFloat() * 40f + 20f
                    MinimalParticle(
                        x = offset.x, y = offset.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        size = Random.nextFloat() * 10f + 6f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 60f,
                        shape = Random.nextInt(3),
                        alpha = 0.8f
                    )
                }
                geometricParticles = (geometricParticles + newParticles).takeLast(40)
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        val centerX = width / 2 + tiltX * 25f
        val centerY = height / 2 + tiltY * 20f
        
        // 纯净背景
        drawRect(color = Color(0xFFFAFAFA))
        
        // 呼吸网格
        drawBreathingGridEnhanced(width, height, breathe, gridPulse, centerX, centerY)
        
        // 中心光晕
        val glowSize = min(width, height) * 0.35f * (1f + sin(breathe) * 0.08f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE0E0E0).copy(alpha = 0.35f + sin(breathe) * 0.1f),
                    Color(0xFFF0F0F0).copy(alpha = 0.15f),
                    Color.Transparent
                )
            ),
            radius = glowSize,
            center = Offset(centerX, centerY)
        )
        
        // 粒子连线
        drawParticleConnections(geometricParticles)
        
        // 脉冲环
        pulseRings.forEach { ring ->
            drawCircle(
                color = Color(0xFFBDBDBD).copy(alpha = ring.alpha),
                radius = ring.radius,
                center = Offset(ring.x, ring.y),
                style = Stroke(width = 1.5f)
            )
        }
        
        // 几何粒子
        geometricParticles.forEach { particle ->
            drawMinimalParticle(particle)
        }
        
        // 触摸点效果
        touchPoints.forEach { touch ->
            val scale = 1f - touch.life
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9E9E9E).copy(alpha = touch.life * 0.4f),
                        Color.Transparent
                    )
                ),
                radius = 60f * scale + 15f,
                center = Offset(touch.x, touch.y)
            )
        }
    }
}

data class MinimalParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var rotation: Float,
    var rotationSpeed: Float, var shape: Int,
    var alpha: Float
)

data class MinimalRing(val x: Float, val y: Float, var radius: Float, var alpha: Float)
data class MinimalTouch(val x: Float, val y: Float, var life: Float)

private fun createMinimalParticle(canvasSize: Size): MinimalParticle {
    return MinimalParticle(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        vx = (Random.nextFloat() - 0.5f) * 15f,
        vy = (Random.nextFloat() - 0.5f) * 15f,
        size = Random.nextFloat() * 12f + 8f,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 30f,
        shape = Random.nextInt(3),
        alpha = Random.nextFloat() * 0.25f + 0.15f
    )
}

private fun updateMinimalParticle(
    particle: MinimalParticle, canvasSize: Size,
    tiltX: Float, tiltY: Float, frame: Long
): MinimalParticle {
    var newParticle = particle.copy(
        x = particle.x + particle.vx * 0.016f + tiltX * 0.3f,
        y = particle.y + particle.vy * 0.016f + tiltY * 0.3f,
        rotation = particle.rotation + particle.rotationSpeed * 0.016f,
        alpha = (particle.alpha + sin(frame * 0.015f + particle.x * 0.005f) * 0.005f).coerceIn(0.1f, 0.4f)
    )
    
    // 边界处理（平滑环绕）
    if (newParticle.x < -30) newParticle = newParticle.copy(x = canvasSize.width + 30)
    if (newParticle.x > canvasSize.width + 30) newParticle = newParticle.copy(x = -30f)
    if (newParticle.y < -30) newParticle = newParticle.copy(y = canvasSize.height + 30)
    if (newParticle.y > canvasSize.height + 30) newParticle = newParticle.copy(y = -30f)
    
    return newParticle
}

private fun DrawScope.drawBreathingGridEnhanced(
    width: Float, height: Float, breathe: Float, gridPulse: Float,
    centerX: Float, centerY: Float
) {
    val gridSize = 45f
    val cols = (width / gridSize).toInt() + 2
    val rows = (height / gridSize).toInt() + 2
    val maxDist = sqrt(width * width + height * height) / 2
    
    // 网格点
    for (i in 0 until cols) {
        for (j in 0 until rows) {
            val x = i * gridSize
            val y = j * gridSize
            val distFromCenter = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
            val normalizedDist = distFromCenter / maxDist
            
            // 波纹效果
            val wave = sin(normalizedDist * 6 * PI.toFloat() - gridPulse * 2 * PI.toFloat())
            val breatheEffect = sin(breathe + normalizedDist * 3)
            val alpha = (0.04f + wave * 0.025f + breatheEffect * 0.015f).coerceIn(0.015f, 0.12f)
            val pointSize = 2f + wave * 1.2f + breatheEffect * 0.5f
            
            drawCircle(
                color = Color(0xFFBDBDBD).copy(alpha = alpha),
                radius = pointSize.coerceAtLeast(1f),
                center = Offset(x, y)
            )
        }
    }
    
    // 网格线
    val lineAlpha = 0.025f + sin(breathe) * 0.008f
    for (i in 0..cols) {
        drawLine(
            color = Color(0xFFE0E0E0).copy(alpha = lineAlpha),
            start = Offset(i * gridSize, 0f),
            end = Offset(i * gridSize, height),
            strokeWidth = 0.5f
        )
    }
    for (j in 0..rows) {
        drawLine(
            color = Color(0xFFE0E0E0).copy(alpha = lineAlpha),
            start = Offset(0f, j * gridSize),
            end = Offset(width, j * gridSize),
            strokeWidth = 0.5f
        )
    }
}

private fun DrawScope.drawParticleConnections(particles: List<MinimalParticle>) {
    val connectionDistance = 120f
    
    particles.forEachIndexed { i, p1 ->
        particles.drop(i + 1).forEach { p2 ->
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val dist = sqrt(dx * dx + dy * dy)
            
            if (dist < connectionDistance) {
                val alpha = (1f - dist / connectionDistance) * 0.15f * min(p1.alpha, p2.alpha) * 5f
                drawLine(
                    color = Color(0xFF9E9E9E).copy(alpha = alpha),
                    start = Offset(p1.x, p1.y),
                    end = Offset(p2.x, p2.y),
                    strokeWidth = 1f
                )
            }
        }
    }
}

private fun DrawScope.drawMinimalParticle(particle: MinimalParticle) {
    val color = Color(0xFF757575).copy(alpha = particle.alpha)
    
    rotate(particle.rotation, pivot = Offset(particle.x, particle.y)) {
        when (particle.shape) {
            0 -> { // 圆形
                drawCircle(
                    color = color,
                    radius = particle.size / 2,
                    center = Offset(particle.x, particle.y),
                    style = Stroke(width = 1.5f)
                )
                // 内圆
                drawCircle(
                    color = color.copy(alpha = particle.alpha * 0.3f),
                    radius = particle.size / 4,
                    center = Offset(particle.x, particle.y)
                )
            }
            1 -> { // 方形
                drawRect(
                    color = color,
                    topLeft = Offset(particle.x - particle.size / 2, particle.y - particle.size / 2),
                    size = Size(particle.size, particle.size),
                    style = Stroke(width = 1.5f)
                )
                // 内方
                drawRect(
                    color = color.copy(alpha = particle.alpha * 0.3f),
                    topLeft = Offset(particle.x - particle.size / 4, particle.y - particle.size / 4),
                    size = Size(particle.size / 2, particle.size / 2)
                )
            }
            2 -> { // 三角形
                val path = Path().apply {
                    moveTo(particle.x, particle.y - particle.size / 2)
                    lineTo(particle.x + particle.size / 2, particle.y + particle.size / 2)
                    lineTo(particle.x - particle.size / 2, particle.y + particle.size / 2)
                    close()
                }
                drawPath(path = path, color = color, style = Stroke(width = 1.5f))
                
                // 内三角
                val innerPath = Path().apply {
                    moveTo(particle.x, particle.y - particle.size / 4)
                    lineTo(particle.x + particle.size / 4, particle.y + particle.size / 4)
                    lineTo(particle.x - particle.size / 4, particle.y + particle.size / 4)
                    close()
                }
                drawPath(path = innerPath, color = color.copy(alpha = particle.alpha * 0.3f))
            }
        }
    }
}


// ==================== 霓虹东京 - NeonTokyo ====================

/**
 * 霓虹东京主题 - 终极版
 * 特性：
 * - 赛博雨滴（物理模拟）
 * - 霓虹招牌（闪烁+故障）
 * - 城市天际线（多层视差）
 * - 湿地反射（动态波纹）
 * - 全息粒子
 * - 扫描线效果
 * - 故障艺术
 * - 触摸产生电弧效果
 */
@Composable
fun NeonTokyoEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "neonTokyo")
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    
    var rainDrops by remember { mutableStateOf(listOf<NeonRain>()) }
    var neonSigns by remember { mutableStateOf(listOf<NeonSignData>()) }
    var holoParticles by remember { mutableStateOf(listOf<HoloParticleData>()) }
    var glitchEffects by remember { mutableStateOf(listOf<GlitchEffect>()) }
    var electricArcs by remember { mutableStateOf(listOf<ElectricArc>()) }
    
    var tiltX by remember { mutableFloatStateOf(0f) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    val scanLine by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "scanLine"
    )
    
    val neonFlicker by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "neonFlicker"
    )
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                tiltX = tiltX * 0.9f + event.values[0] * 0.1f
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
            rainDrops = List(200) { createNeonRain(canvasSize) }
            neonSigns = createNeonSignsData(canvasSize)
            holoParticles = List(50) { createHoloParticleData(canvasSize) }
            
            while (true) {
                frame++
                
                rainDrops = rainDrops.map { updateNeonRain(it, canvasSize, tiltX) }
                holoParticles = holoParticles.map { updateHoloParticleData(it, canvasSize, frame) }
                
                electricArcs = electricArcs.mapNotNull { arc ->
                    val newArc = arc.copy(life = arc.life - 0.05f)
                    if (newArc.life > 0) newArc else null
                }
                
                // Shuffle故障效果
                if (Random.nextFloat() < 0.015f) {
                    glitchEffects = List(Random.nextInt(4) + 2) {
                        GlitchEffect(
                            y = Random.nextFloat() * canvasSize.height,
                            height = Random.nextFloat() * 20f + 5f,
                            offset = (Random.nextFloat() - 0.5f) * 30f,
                            life = Random.nextFloat() * 0.3f + 0.1f,
                            color = neonColors.random()
                        )
                    }
                }
                
                glitchEffects = glitchEffects.mapNotNull { glitch ->
                    val newGlitch = glitch.copy(life = glitch.life - 0.04f)
                    if (newGlitch.life > 0) newGlitch else null
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
                haptic.performHaptic(HapticType.IMPACT_HEAVY, 0.8f)
                onInteraction()
                
                // 电弧效果
                electricArcs = electricArcs + List(5) {
                    ElectricArc(
                        startX = offset.x,
                        startY = offset.y,
                        endX = offset.x + (Random.nextFloat() - 0.5f) * 150f,
                        endY = offset.y + (Random.nextFloat() - 0.5f) * 150f,
                        life = 1f,
                        color = neonColors.random()
                    )
                }
                
                // 故障爆发
                glitchEffects = glitchEffects + List(8) {
                    GlitchEffect(
                        y = offset.y + (Random.nextFloat() - 0.5f) * 100f,
                        height = Random.nextFloat() * 15f + 5f,
                        offset = (Random.nextFloat() - 0.5f) * 40f,
                        life = 0.6f,
                        color = neonColors.random()
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
        
        // 深色城市背景
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF05000A),
                    Color(0xFF0A0015),
                    Color(0xFF100020),
                    Color(0xFF0A0012)
                )
            )
        )
        
        // 城市天际线
        drawCyberpunkSkyline(width, height, frame, tiltX)
        
        // 霓虹招牌
        neonSigns.forEach { sign ->
            drawNeonSignEnhanced(sign, neonFlicker, frame)
        }
        
        // 雨滴
        rainDrops.forEach { drop ->
            drawNeonRainEnhanced(drop)
        }
        
        // 湿地反射
        drawWetStreetReflection(width, height, frame, neonFlicker)
        
        // 全息粒子
        holoParticles.forEach { particle ->
            drawHoloParticleEnhanced(particle, frame)
        }
        
        // 电弧效果
        electricArcs.forEach { arc ->
            drawElectricArc(arc)
        }
        
        // 故障效果
        glitchEffects.forEach { glitch ->
            drawGlitchEffect(glitch, width)
        }
        
        // 扫描线
        val scanY = height * scanLine
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF00FFFF).copy(alpha = 0.08f),
                    Color(0xFF00FFFF).copy(alpha = 0.15f),
                    Color(0xFF00FFFF).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = scanY - 40,
                endY = scanY + 40
            ),
            topLeft = Offset(0f, scanY - 40),
            size = Size(width, 80f)
        )
        
        // CRT扫描线纹理
        for (i in 0 until (height / 2.5f).toInt()) {
            drawLine(
                color = Color.Black.copy(alpha = 0.08f),
                start = Offset(0f, i * 2.5f),
                end = Offset(width, i * 2.5f),
                strokeWidth = 1f
            )
        }
        
        // 边缘暗角
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                center = Offset(width / 2, height / 2),
                radius = max(width, height) * 0.8f
            )
        )
    }
}

// 霓虹东京数据类
data class NeonRain(
    var x: Float, var y: Float,
    var speed: Float, var length: Float,
    var color: Color, var alpha: Float
)

data class NeonSignData(
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val color: Color, val flickerPhase: Float,
    val glitchProbability: Float
)

data class HoloParticleData(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var color: Color,
    var phase: Float, var alpha: Float
)

data class GlitchEffect(
    val y: Float, val height: Float,
    val offset: Float, var life: Float,
    val color: Color
)

data class ElectricArc(
    val startX: Float, val startY: Float,
    val endX: Float, val endY: Float,
    var life: Float, val color: Color
)

private val neonColors = listOf(
    Color(0xFFFF00FF), Color(0xFF00FFFF), Color(0xFFFF0080),
    Color(0xFF00FF80), Color(0xFFFFFF00), Color(0xFF8000FF)
)

private fun createNeonRain(canvasSize: Size): NeonRain {
    return NeonRain(
        x = Random.nextFloat() * canvasSize.width,
        y = -Random.nextFloat() * canvasSize.height,
        speed = Random.nextFloat() * 18f + 12f,
        length = Random.nextFloat() * 35f + 20f,
        color = listOf(
            Color(0xFF00FFFF), Color(0xFFFF00FF), Color(0xFF8080FF)
        ).random(),
        alpha = Random.nextFloat() * 0.4f + 0.3f
    )
}

private fun updateNeonRain(drop: NeonRain, canvasSize: Size, tiltX: Float): NeonRain {
    var newDrop = drop.copy(
        x = drop.x + tiltX * 2.5f,
        y = drop.y + drop.speed
    )
    
    if (newDrop.y > canvasSize.height + drop.length) {
        newDrop = createNeonRain(canvasSize).copy(y = -drop.length)
    }
    
    return newDrop
}

private fun createNeonSignsData(canvasSize: Size): List<NeonSignData> {
    return List(8) { i ->
        NeonSignData(
            x = Random.nextFloat() * canvasSize.width * 0.8f + canvasSize.width * 0.1f,
            y = Random.nextFloat() * canvasSize.height * 0.35f + canvasSize.height * 0.08f,
            width = Random.nextFloat() * 80f + 50f,
            height = Random.nextFloat() * 35f + 25f,
            color = neonColors.random(),
            flickerPhase = Random.nextFloat() * PI.toFloat() * 2,
            glitchProbability = Random.nextFloat() * 0.1f
        )
    }
}

private fun createHoloParticleData(canvasSize: Size): HoloParticleData {
    return HoloParticleData(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height,
        vx = (Random.nextFloat() - 0.5f) * 20f,
        vy = -Random.nextFloat() * 15f - 5f,
        size = Random.nextFloat() * 5f + 3f,
        color = neonColors.random(),
        phase = Random.nextFloat() * PI.toFloat() * 2,
        alpha = Random.nextFloat() * 0.5f + 0.3f
    )
}

private fun updateHoloParticleData(particle: HoloParticleData, canvasSize: Size, frame: Long): HoloParticleData {
    var newParticle = particle.copy(
        x = particle.x + particle.vx * 0.016f + sin(frame * 0.03f + particle.phase) * 0.8f,
        y = particle.y + particle.vy * 0.016f,
        phase = particle.phase + 0.05f,
        alpha = (particle.alpha + sin(frame * 0.02f + particle.phase) * 0.02f).coerceIn(0.2f, 0.7f)
    )
    
    if (newParticle.y < -20 || newParticle.x < -20 || newParticle.x > canvasSize.width + 20) {
        newParticle = createHoloParticleData(canvasSize).copy(y = canvasSize.height + 20)
    }
    
    return newParticle
}


// 霓虹东京绘制函数
private fun DrawScope.drawCyberpunkSkyline(
    width: Float, height: Float, frame: Long, tiltX: Float
) {
    val buildingCount = 20
    val random = Random(42)
    
    // 远景建筑
    for (i in 0 until buildingCount) {
        val buildingX = width * i / buildingCount + tiltX * 5f
        val buildingWidth = width / buildingCount * 0.95f
        val buildingHeight = random.nextFloat() * height * 0.35f + height * 0.15f
        val buildingY = height * 0.55f - buildingHeight
        
        // 建筑主体
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A0015),
                    Color(0xFF050008)
                )
            ),
            topLeft = Offset(buildingX, buildingY),
            size = Size(buildingWidth, buildingHeight + height * 0.45f)
        )
        
        // 建筑边缘光
        drawLine(
            color = Color(0xFF00FFFF).copy(alpha = 0.3f),
            start = Offset(buildingX, buildingY),
            end = Offset(buildingX, buildingY + buildingHeight),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFFFF00FF).copy(alpha = 0.3f),
            start = Offset(buildingX + buildingWidth, buildingY),
            end = Offset(buildingX + buildingWidth, buildingY + buildingHeight),
            strokeWidth = 1f
        )
        
        // 窗户
        val windowRows = (buildingHeight / 18).toInt()
        val windowCols = (buildingWidth / 12).toInt()
        
        for (row in 0 until windowRows) {
            for (col in 0 until windowCols) {
                val windowOn = random.nextFloat() > 0.35f
                if (windowOn) {
                    val flicker = if (random.nextFloat() < 0.08f) {
                        sin(frame * 0.15f + row + col) * 0.4f
                    } else 0f
                    
                    val windowColor = listOf(
                        Color(0xFFFFFF80), Color(0xFF80FFFF), Color(0xFFFF80FF),
                        Color(0xFF80FF80), Color(0xFFFFFFFF)
                    ).random()
                    
                    drawRect(
                        color = windowColor.copy(alpha = (0.5f + flicker).coerceIn(0.2f, 0.9f)),
                        topLeft = Offset(buildingX + col * 12 + 2, buildingY + row * 18 + 4),
                        size = Size(7f, 11f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNeonSignEnhanced(sign: NeonSignData, flicker: Float, frame: Long) {
    val flickerIntensity = (sin(flicker * 3 + sign.flickerPhase) + 1) / 2
    val isGlitching = Random.nextFloat() < sign.glitchProbability
    val glitchOffset = if (isGlitching) (Random.nextFloat() - 0.5f) * 8f else 0f
    
    // 外发光（多层）
    for (i in 3 downTo 1) {
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    sign.color.copy(alpha = 0.25f * flickerIntensity / i),
                    sign.color.copy(alpha = 0.08f * flickerIntensity / i),
                    Color.Transparent
                ),
                center = Offset(sign.x + sign.width / 2, sign.y + sign.height / 2),
                radius = sign.width * (0.8f + i * 0.3f)
            ),
            topLeft = Offset(sign.x - 25f * i + glitchOffset, sign.y - 25f * i),
            size = Size(sign.width + 50f * i, sign.height + 50f * i),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
        )
    }
    
    // 招牌背景
    drawRoundRect(
        color = Color(0xFF0A0015).copy(alpha = 0.8f),
        topLeft = Offset(sign.x + glitchOffset, sign.y),
        size = Size(sign.width, sign.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
    )
    
    // 招牌边框
    drawRoundRect(
        color = sign.color.copy(alpha = 0.9f * flickerIntensity),
        topLeft = Offset(sign.x + glitchOffset, sign.y),
        size = Size(sign.width, sign.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f),
        style = Stroke(width = 2.5f)
    )
    
    // 内部装饰线
    drawLine(
        color = sign.color.copy(alpha = 0.6f * flickerIntensity),
        start = Offset(sign.x + 8f + glitchOffset, sign.y + sign.height / 2),
        end = Offset(sign.x + sign.width - 8f + glitchOffset, sign.y + sign.height / 2),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawNeonRainEnhanced(drop: NeonRain) {
    // 雨滴光晕
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                drop.color.copy(alpha = drop.alpha * 0.3f),
                drop.color.copy(alpha = drop.alpha),
                drop.color.copy(alpha = drop.alpha * 0.5f)
            ),
            startY = drop.y - drop.length,
            endY = drop.y
        ),
        start = Offset(drop.x, drop.y - drop.length),
        end = Offset(drop.x, drop.y),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    
    // 雨滴核心
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = drop.alpha)),
            startY = drop.y - drop.length * 0.3f,
            endY = drop.y
        ),
        start = Offset(drop.x, drop.y - drop.length * 0.3f),
        end = Offset(drop.x, drop.y),
        strokeWidth = 1f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawWetStreetReflection(
    width: Float, height: Float, frame: Long, flicker: Float
) {
    val groundY = height * 0.72f
    
    // 湿地基础
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF050008).copy(alpha = 0.7f),
                Color(0xFF0A0012).copy(alpha = 0.9f)
            ),
            startY = groundY,
            endY = height
        ),
        topLeft = Offset(0f, groundY),
        size = Size(width, height - groundY)
    )
    
    // 霓虹反射
    neonColors.forEachIndexed { i, color ->
        val reflectX = width * (i + 0.5f) / neonColors.size
        val reflectWidth = 25f + sin(frame * 0.025f + i) * 12f
        val alpha = 0.18f + sin(flicker + i * 0.5f) * 0.08f
        
        // 反射条纹
        for (j in 0 until 6) {
            val stripeY = groundY + j * 20f + sin(frame * 0.02f + i + j) * 3f
            val stripeAlpha = alpha * (1f - j * 0.12f)
            
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = stripeAlpha),
                        color.copy(alpha = stripeAlpha * 1.2f),
                        color.copy(alpha = stripeAlpha),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(reflectX - reflectWidth, stripeY),
                size = Size(reflectWidth * 2, 8f)
            )
        }
    }
    
    // 水面波纹
    for (i in 0 until 4) {
        val waveY = groundY + 8f + i * 25f
        val waveAlpha = 0.08f - i * 0.015f
        
        drawLine(
            color = Color.White.copy(alpha = waveAlpha),
            start = Offset(0f, waveY + sin(frame * 0.015f + i) * 4f),
            end = Offset(width, waveY + sin(frame * 0.015f + i + 3f) * 4f),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawHoloParticleEnhanced(particle: HoloParticleData, frame: Long) {
    val pulse = (sin(particle.phase) + 1) / 2
    
    // 外层光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                particle.color.copy(alpha = particle.alpha * 0.4f * pulse),
                particle.color.copy(alpha = particle.alpha * 0.1f * pulse),
                Color.Transparent
            )
        ),
        radius = particle.size * 5f,
        center = Offset(particle.x, particle.y)
    )
    
    // 中层
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                particle.color.copy(alpha = particle.alpha * 0.7f * pulse),
                Color.Transparent
            )
        ),
        radius = particle.size * 2.5f,
        center = Offset(particle.x, particle.y)
    )
    
    // 核心
    drawCircle(
        color = Color.White.copy(alpha = particle.alpha * pulse),
        radius = particle.size * 0.6f,
        center = Offset(particle.x, particle.y)
    )
}

private fun DrawScope.drawElectricArc(arc: ElectricArc) {
    val segments = 8
    val points = mutableListOf<Offset>()
    points.add(Offset(arc.startX, arc.startY))
    
    for (i in 1 until segments) {
        val t = i.toFloat() / segments
        val baseX = arc.startX + (arc.endX - arc.startX) * t
        val baseY = arc.startY + (arc.endY - arc.startY) * t
        val offset = (Random.nextFloat() - 0.5f) * 30f * arc.life
        points.add(Offset(baseX + offset, baseY + offset))
    }
    points.add(Offset(arc.endX, arc.endY))
    
    // 外层光晕
    for (i in 0 until points.size - 1) {
        drawLine(
            color = arc.color.copy(alpha = arc.life * 0.3f),
            start = points[i],
            end = points[i + 1],
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
    }
    
    // 核心
    for (i in 0 until points.size - 1) {
        drawLine(
            color = Color.White.copy(alpha = arc.life * 0.9f),
            start = points[i],
            end = points[i + 1],
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawGlitchEffect(glitch: GlitchEffect, width: Float) {
    // RGB分离效果
    drawRect(
        color = Color.Red.copy(alpha = glitch.life * 0.4f),
        topLeft = Offset(glitch.offset - 3f, glitch.y),
        size = Size(width * 0.4f, glitch.height)
    )
    drawRect(
        color = Color.Green.copy(alpha = glitch.life * 0.4f),
        topLeft = Offset(glitch.offset, glitch.y),
        size = Size(width * 0.4f, glitch.height)
    )
    drawRect(
        color = Color.Blue.copy(alpha = glitch.life * 0.4f),
        topLeft = Offset(glitch.offset + 3f, glitch.y),
        size = Size(width * 0.4f, glitch.height)
    )
    
    // 主色块
    drawRect(
        color = glitch.color.copy(alpha = glitch.life * 0.6f),
        topLeft = Offset(glitch.offset, glitch.y),
        size = Size(Random.nextFloat() * width * 0.5f + 50f, glitch.height)
    )
}


// ==================== 薰衣草梦境 - Lavender ====================

/**
 * 薰衣草主题 - 终极版
 * 特性：
 * - 摇曳薰衣草田（物理风力）
 * - 蝴蝶群（群聚+追踪行为）
 * - 飘落花瓣
 * - 柔光粒子
 * - 梦幻光晕
 * - 远景山丘
 * - 触摸吸引蝴蝶
 * - 摇晃释放花瓣
 */
@Composable
fun LavenderEnhancedBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val infiniteTransition = rememberInfiniteTransition(label = "lavender")
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frame by remember { mutableLongStateOf(0L) }
    
    var lavenderStalks by remember { mutableStateOf(listOf<LavenderStalkData>()) }
    var butterflies by remember { mutableStateOf(listOf<ButterflyData>()) }
    var floatingPetals by remember { mutableStateOf(listOf<LavenderPetalData>()) }
    var lightOrbs by remember { mutableStateOf(listOf<LightOrbData>()) }
    var attractionPoint by remember { mutableStateOf<Offset?>(null) }
    
    var windForce by remember { mutableFloatStateOf(0f) }
    var shakeDetected by remember { mutableStateOf(false) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    val windPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "wind"
    )
    
    val dreamGlow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "dreamGlow"
    )
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastX = 0f
        var lastShakeTime = 0L
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                windForce = windForce * 0.95f + event.values[0] * 0.05f
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 400) {
                    val deltaX = abs(event.values[0] - lastX)
                    if (deltaX > 10) {
                        shakeDetected = true
                        haptic.performHaptic(HapticType.SOFT_LANDING)
                        lastShakeTime = currentTime
                    }
                    lastX = event.values[0]
                }
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
            lavenderStalks = createLavenderFieldData(canvasSize)
            butterflies = List(10) { createButterflyData(canvasSize) }
            floatingPetals = List(30) { createLavenderPetalData(canvasSize) }
            lightOrbs = List(20) { createLightOrbData(canvasSize) }
            
            while (true) {
                frame++
                
                butterflies = updateButterfliesWithBehavior(butterflies, canvasSize, frame, windForce, attractionPoint)
                floatingPetals = floatingPetals.mapNotNull { updateLavenderPetalData(it, canvasSize, windForce) }
                lightOrbs = lightOrbs.map { updateLightOrbData(it, canvasSize, frame) }
                
                // 补充花瓣
                if (floatingPetals.size < 25 && Random.nextFloat() < 0.08f) {
                    floatingPetals = floatingPetals + createLavenderPetalData(canvasSize)
                }
                
                // 摇晃释放花瓣
                if (shakeDetected) {
                    shakeDetected = false
                    floatingPetals = floatingPetals + List(20) { createLavenderPetalData(canvasSize) }
                    if (butterflies.size < 15) {
                        butterflies = butterflies + List(3) { createButterflyData(canvasSize) }
                    }
                }
                
                // 清除吸引点
                if (attractionPoint != null && frame % 180 == 0L) {
                    attractionPoint = null
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
                haptic.performHaptic(HapticType.LIGHT_TAP)
                onInteraction()
                
                attractionPoint = offset
                
                // 花瓣爆发
                val newPetals = List(15) {
                    val angle = Random.nextFloat() * 2 * PI.toFloat()
                    val speed = Random.nextFloat() * 60f + 30f
                    LavenderPetalData(
                        x = offset.x, y = offset.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed - 20f,
                        size = Random.nextFloat() * 8f + 5f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 180f,
                        alpha = 1f,
                        color = lavenderPetalColors.random()
                    )
                }
                floatingPetals = (floatingPetals + newPetals).takeLast(80)
            }
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        canvasSize = size
        val width = size.width
        val height = size.height
        
        // 梦幻天空渐变
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8F0FF),
                    Color(0xFFEDE0F5),
                    Color(0xFFE5D5EE),
                    Color(0xFFDDCCE8)
                )
            )
        )
        
        // 梦幻光晕
        val glowAlpha = 0.25f + sin(dreamGlow) * 0.1f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE6E6FA).copy(alpha = glowAlpha),
                    Color(0xFFDDA0DD).copy(alpha = glowAlpha * 0.4f),
                    Color.Transparent
                )
            ),
            radius = min(width, height) * 0.55f,
            center = Offset(width * 0.3f, height * 0.2f)
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFDA70D6).copy(alpha = glowAlpha * 0.6f),
                    Color.Transparent
                )
            ),
            radius = min(width, height) * 0.35f,
            center = Offset(width * 0.8f, height * 0.25f)
        )
        
        // 远景山丘
        drawLavenderHillsEnhanced(width, height)
        
        // 薰衣草田
        lavenderStalks.forEach { stalk ->
            drawLavenderStalkEnhanced(stalk, windPhase, windForce, frame)
        }
        
        // 光球
        lightOrbs.forEach { orb ->
            drawLightOrbEnhanced(orb, frame)
        }
        
        // 飘落花瓣
        floatingPetals.forEach { petal ->
            drawLavenderPetalEnhanced(petal)
        }
        
        // 蝴蝶
        butterflies.forEach { butterfly ->
            drawButterflyEnhanced(butterfly, frame)
        }
        
        // 吸引点效果
        attractionPoint?.let { point ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE6E6FA).copy(alpha = 0.3f),
                        Color(0xFFDDA0DD).copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                radius = 80f,
                center = point
            )
        }
        
        // 底部雾气
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFE6E6FA).copy(alpha = 0.35f)
                ),
                startY = height * 0.75f,
                endY = height
            )
        )
    }
}

// 薰衣草主题数据类
data class LavenderStalkData(
    val x: Float, val baseY: Float,
    val height: Float, val phase: Float,
    val flowerCount: Int, val depth: Float
)

data class ButterflyData(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var wingPhase: Float, var size: Float,
    var color: Color, var targetX: Float, var targetY: Float
)

data class LavenderPetalData(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var rotation: Float,
    var rotationSpeed: Float, var alpha: Float,
    var color: Color
)

data class LightOrbData(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var size: Float, var phase: Float,
    var color: Color, var alpha: Float
)

private val lavenderPetalColors = listOf(
    Color(0xFFE6E6FA), Color(0xFFDDA0DD), Color(0xFFDA70D6),
    Color(0xFFBA55D3), Color(0xFF9370DB)
)

private val butterflyColors = listOf(
    Color(0xFFE6E6FA), Color(0xFFDDA0DD), Color(0xFFDA70D6),
    Color(0xFFFFB6C1), Color(0xFFFFF0F5), Color(0xFFE0B0FF)
)

private fun createLavenderFieldData(canvasSize: Size): List<LavenderStalkData> {
    val stalks = mutableListOf<LavenderStalkData>()
    val rows = 10
    
    for (row in 0 until rows) {
        val depth = row.toFloat() / rows
        val baseY = canvasSize.height * (0.45f + depth * 0.45f)
        val count = 25 + row * 4
        
        for (i in 0 until count) {
            stalks.add(LavenderStalkData(
                x = canvasSize.width * i / count + (Random.nextFloat() - 0.5f) * 15f,
                baseY = baseY,
                height = (55f + Random.nextFloat() * 35f) * (1f - depth * 0.25f),
                phase = Random.nextFloat() * PI.toFloat() * 2,
                flowerCount = Random.nextInt(5) + 4,
                depth = depth
            ))
        }
    }
    
    return stalks.sortedBy { it.depth }
}

private fun createButterflyData(canvasSize: Size): ButterflyData {
    val x = Random.nextFloat() * canvasSize.width
    val y = Random.nextFloat() * canvasSize.height * 0.5f + canvasSize.height * 0.1f
    return ButterflyData(
        x = x, y = y,
        vx = (Random.nextFloat() - 0.5f) * 40f,
        vy = (Random.nextFloat() - 0.5f) * 25f,
        wingPhase = Random.nextFloat() * PI.toFloat() * 2,
        size = Random.nextFloat() * 14f + 10f,
        color = butterflyColors.random(),
        targetX = x + (Random.nextFloat() - 0.5f) * 200f,
        targetY = y + (Random.nextFloat() - 0.5f) * 150f
    )
}

private fun createLavenderPetalData(canvasSize: Size): LavenderPetalData {
    return LavenderPetalData(
        x = Random.nextFloat() * canvasSize.width,
        y = -Random.nextFloat() * 40f - 10f,
        vx = (Random.nextFloat() - 0.5f) * 20f,
        vy = Random.nextFloat() * 35f + 20f,
        size = Random.nextFloat() * 7f + 4f,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 120f,
        alpha = Random.nextFloat() * 0.4f + 0.6f,
        color = lavenderPetalColors.random()
    )
}

private fun createLightOrbData(canvasSize: Size): LightOrbData {
    return LightOrbData(
        x = Random.nextFloat() * canvasSize.width,
        y = Random.nextFloat() * canvasSize.height * 0.65f,
        vx = (Random.nextFloat() - 0.5f) * 8f,
        vy = -Random.nextFloat() * 6f - 2f,
        size = Random.nextFloat() * 18f + 10f,
        phase = Random.nextFloat() * PI.toFloat() * 2,
        color = listOf(Color(0xFFE6E6FA), Color(0xFFFFF0F5), Color(0xFFFFE4E1)).random(),
        alpha = Random.nextFloat() * 0.3f + 0.2f
    )
}


// 薰衣草更新和绘制函数
private fun updateButterfliesWithBehavior(
    butterflies: List<ButterflyData>,
    canvasSize: Size,
    frame: Long,
    windForce: Float,
    attractionPoint: Offset?
): List<ButterflyData> {
    return butterflies.map { butterfly ->
        var newButterfly = butterfly.copy(wingPhase = butterfly.wingPhase + 0.35f)
        
        // 吸引点追踪
        val targetX: Float
        val targetY: Float
        
        if (attractionPoint != null) {
            targetX = attractionPoint.x
            targetY = attractionPoint.y
        } else {
            targetX = butterfly.targetX
            targetY = butterfly.targetY
        }
        
        // 向目标移动
        val dx = targetX - butterfly.x
        val dy = targetY - butterfly.y
        val dist = sqrt(dx * dx + dy * dy)
        
        if (dist > 20) {
            newButterfly = newButterfly.copy(
                vx = butterfly.vx * 0.92f + dx / dist * 8f,
                vy = butterfly.vy * 0.92f + dy / dist * 6f
            )
        } else if (attractionPoint == null) {
            // 到达目标，设置新目标
            newButterfly = newButterfly.copy(
                targetX = Random.nextFloat() * canvasSize.width,
                targetY = Random.nextFloat() * canvasSize.height * 0.5f + canvasSize.height * 0.1f
            )
        }
        
        // 添加随机扰动和风力
        val wobbleX = sin(frame * 0.04f + butterfly.wingPhase) * 1.2f
        val wobbleY = cos(frame * 0.03f + butterfly.wingPhase) * 0.8f
        
        newButterfly = newButterfly.copy(
            x = butterfly.x + newButterfly.vx * 0.016f + wobbleX + windForce * 0.8f,
            y = butterfly.y + newButterfly.vy * 0.016f + wobbleY,
            vx = newButterfly.vx.coerceIn(-60f, 60f),
            vy = newButterfly.vy.coerceIn(-40f, 40f)
        )
        
        // 边界处理
        if (newButterfly.x < -30) newButterfly = newButterfly.copy(x = canvasSize.width + 30)
        if (newButterfly.x > canvasSize.width + 30) newButterfly = newButterfly.copy(x = -30f)
        if (newButterfly.y < 0) newButterfly = newButterfly.copy(vy = abs(newButterfly.vy))
        if (newButterfly.y > canvasSize.height * 0.65f) newButterfly = newButterfly.copy(vy = -abs(newButterfly.vy))
        
        newButterfly
    }
}

private fun updateLavenderPetalData(
    petal: LavenderPetalData, canvasSize: Size, windForce: Float
): LavenderPetalData? {
    val gravity = 25f
    val sway = sin(petal.rotation * PI.toFloat() / 180f) * 15f
    
    val newPetal = petal.copy(
        x = petal.x + petal.vx * 0.016f + sway * 0.016f + windForce * 1.5f,
        y = petal.y + petal.vy * 0.016f,
        vx = petal.vx * 0.995f,
        vy = (petal.vy + gravity * 0.016f).coerceAtMost(80f),
        rotation = petal.rotation + petal.rotationSpeed * 0.016f,
        alpha = petal.alpha - 0.001f
    )
    
    return if (newPetal.y > canvasSize.height + 30 || newPetal.alpha <= 0) null else newPetal
}

private fun updateLightOrbData(orb: LightOrbData, canvasSize: Size, frame: Long): LightOrbData {
    var newOrb = orb.copy(
        x = orb.x + orb.vx * 0.016f + sin(frame * 0.008f + orb.phase) * 0.6f,
        y = orb.y + orb.vy * 0.016f,
        phase = orb.phase + 0.02f,
        alpha = (orb.alpha + sin(frame * 0.015f + orb.phase) * 0.01f).coerceIn(0.15f, 0.45f)
    )
    
    if (newOrb.y < -30 || newOrb.x < -30 || newOrb.x > canvasSize.width + 30) {
        newOrb = createLightOrbData(canvasSize).copy(y = canvasSize.height * 0.7f)
    }
    
    return newOrb
}

private fun DrawScope.drawLavenderHillsEnhanced(width: Float, height: Float) {
    // 远山
    val farHillPath = Path().apply {
        moveTo(0f, height)
        quadraticBezierTo(width * 0.2f, height * 0.55f, width * 0.4f, height * 0.62f)
        quadraticBezierTo(width * 0.6f, height * 0.5f, width * 0.8f, height * 0.58f)
        quadraticBezierTo(width * 0.95f, height * 0.52f, width, height * 0.56f)
        lineTo(width, height)
        close()
    }
    
    drawPath(
        path = farHillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFD8BFD8).copy(alpha = 0.35f),
                Color(0xFFE6E6FA).copy(alpha = 0.25f)
            )
        )
    )
    
    // 近山
    val nearHillPath = Path().apply {
        moveTo(0f, height)
        quadraticBezierTo(width * 0.15f, height * 0.65f, width * 0.35f, height * 0.7f)
        quadraticBezierTo(width * 0.55f, height * 0.6f, width * 0.75f, height * 0.68f)
        quadraticBezierTo(width * 0.9f, height * 0.62f, width, height * 0.66f)
        lineTo(width, height)
        close()
    }
    
    drawPath(
        path = nearHillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFDDA0DD).copy(alpha = 0.25f),
                Color(0xFFE6E6FA).copy(alpha = 0.15f)
            )
        )
    )
}

private fun DrawScope.drawLavenderStalkEnhanced(
    stalk: LavenderStalkData, windPhase: Float,
    windForce: Float, frame: Long
) {
    val sway = sin(windPhase + stalk.phase) * 12f * (1f - stalk.depth * 0.4f) + windForce * 18f
    val alpha = 0.65f + stalk.depth * 0.35f
    
    // 茎
    val stemPath = Path().apply {
        moveTo(stalk.x, stalk.baseY)
        quadraticBezierTo(
            stalk.x + sway * 0.4f, stalk.baseY - stalk.height * 0.5f,
            stalk.x + sway, stalk.baseY - stalk.height
        )
    }
    
    drawPath(
        path = stemPath,
        color = Color(0xFF228B22).copy(alpha = alpha * 0.75f),
        style = Stroke(width = 2.5f * (1f - stalk.depth * 0.25f), cap = StrokeCap.Round)
    )
    
    // 花朵
    val flowerColors = listOf(
        Color(0xFF9370DB), Color(0xFF8A2BE2), Color(0xFF9932CC),
        Color(0xFFBA55D3), Color(0xFF8B008B)
    )
    
    for (i in 0 until stalk.flowerCount) {
        val t = i.toFloat() / stalk.flowerCount
        val flowerX = stalk.x + sway * (1f - t * 0.25f)
        val flowerY = stalk.baseY - stalk.height * (0.55f + t * 0.4f)
        val flowerSize = (5f + Random(stalk.x.toInt() + i).nextFloat() * 4f) * (1f - stalk.depth * 0.25f)
        val flowerColor = flowerColors[i % flowerColors.size]
        
        // 花瓣光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    flowerColor.copy(alpha = alpha * 0.35f),
                    Color.Transparent
                )
            ),
            radius = flowerSize * 2.5f,
            center = Offset(flowerX, flowerY)
        )
        
        // 花瓣
        drawCircle(
            color = flowerColor.copy(alpha = alpha),
            radius = flowerSize,
            center = Offset(flowerX, flowerY)
        )
        
        // 花心
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.5f),
            radius = flowerSize * 0.3f,
            center = Offset(flowerX, flowerY)
        )
    }
}

private fun DrawScope.drawButterflyEnhanced(butterfly: ButterflyData, frame: Long) {
    val wingAngle = sin(butterfly.wingPhase) * 45f
    val bodyLength = butterfly.size * 0.7f
    
    // 身体阴影
    drawLine(
        color = Color.Black.copy(alpha = 0.15f),
        start = Offset(butterfly.x + 2f, butterfly.y - bodyLength / 2 + 2f),
        end = Offset(butterfly.x + 2f, butterfly.y + bodyLength / 2 + 2f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    
    // 身体
    drawLine(
        color = Color(0xFF4B0082),
        start = Offset(butterfly.x, butterfly.y - bodyLength / 2),
        end = Offset(butterfly.x, butterfly.y + bodyLength / 2),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    
    // 翅膀
    val wingSize = butterfly.size
    val wingY = butterfly.y
    
    // 上翅膀
    rotate(wingAngle, pivot = Offset(butterfly.x, wingY)) {
        // 左上翅
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    butterfly.color,
                    butterfly.color.copy(alpha = 0.7f),
                    butterfly.color.copy(alpha = 0.3f)
                ),
                center = Offset(butterfly.x - wingSize * 0.4f, wingY - wingSize * 0.2f)
            ),
            topLeft = Offset(butterfly.x - wingSize, wingY - wingSize * 0.7f),
            size = Size(wingSize, wingSize * 0.65f)
        )
        
        // 翅膀花纹
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = wingSize * 0.15f,
            center = Offset(butterfly.x - wingSize * 0.5f, wingY - wingSize * 0.3f)
        )
    }
    
    rotate(-wingAngle, pivot = Offset(butterfly.x, wingY)) {
        // 右上翅
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    butterfly.color,
                    butterfly.color.copy(alpha = 0.7f),
                    butterfly.color.copy(alpha = 0.3f)
                ),
                center = Offset(butterfly.x + wingSize * 0.4f, wingY - wingSize * 0.2f)
            ),
            topLeft = Offset(butterfly.x, wingY - wingSize * 0.7f),
            size = Size(wingSize, wingSize * 0.65f)
        )
        
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = wingSize * 0.15f,
            center = Offset(butterfly.x + wingSize * 0.5f, wingY - wingSize * 0.3f)
        )
    }
    
    // 下翅膀
    rotate(wingAngle * 0.6f, pivot = Offset(butterfly.x, wingY)) {
        drawOval(
            color = butterfly.color.copy(alpha = 0.75f),
            topLeft = Offset(butterfly.x - wingSize * 0.7f, wingY),
            size = Size(wingSize * 0.7f, wingSize * 0.45f)
        )
    }
    
    rotate(-wingAngle * 0.6f, pivot = Offset(butterfly.x, wingY)) {
        drawOval(
            color = butterfly.color.copy(alpha = 0.75f),
            topLeft = Offset(butterfly.x, wingY),
            size = Size(wingSize * 0.7f, wingSize * 0.45f)
        )
    }
    
    // 触角
    drawLine(
        color = Color(0xFF4B0082),
        start = Offset(butterfly.x, butterfly.y - bodyLength / 2),
        end = Offset(butterfly.x - 5f, butterfly.y - bodyLength / 2 - 8f),
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF4B0082),
        start = Offset(butterfly.x, butterfly.y - bodyLength / 2),
        end = Offset(butterfly.x + 5f, butterfly.y - bodyLength / 2 - 8f),
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )
    
    // 触角尖端
    drawCircle(
        color = Color(0xFF4B0082),
        radius = 1.5f,
        center = Offset(butterfly.x - 5f, butterfly.y - bodyLength / 2 - 8f)
    )
    drawCircle(
        color = Color(0xFF4B0082),
        radius = 1.5f,
        center = Offset(butterfly.x + 5f, butterfly.y - bodyLength / 2 - 8f)
    )
}

private fun DrawScope.drawLavenderPetalEnhanced(petal: LavenderPetalData) {
    rotate(petal.rotation, pivot = Offset(petal.x, petal.y)) {
        // 花瓣阴影
        drawOval(
            color = Color.Black.copy(alpha = petal.alpha * 0.1f),
            topLeft = Offset(petal.x - petal.size / 2 + 2f, petal.y - petal.size / 3 + 2f),
            size = Size(petal.size, petal.size * 0.6f)
        )
        
        // 花瓣主体
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    petal.color.copy(alpha = petal.alpha),
                    petal.color.copy(alpha = petal.alpha * 0.7f)
                ),
                center = Offset(petal.x, petal.y)
            ),
            topLeft = Offset(petal.x - petal.size / 2, petal.y - petal.size / 3),
            size = Size(petal.size, petal.size * 0.6f)
        )
        
        // 花瓣高光
        drawOval(
            color = Color.White.copy(alpha = petal.alpha * 0.3f),
            topLeft = Offset(petal.x - petal.size / 4, petal.y - petal.size / 5),
            size = Size(petal.size * 0.4f, petal.size * 0.25f)
        )
    }
}

private fun DrawScope.drawLightOrbEnhanced(orb: LightOrbData, frame: Long) {
    val pulse = (sin(orb.phase) + 1) / 2
    val currentAlpha = orb.alpha * (0.7f + pulse * 0.3f)
    val currentSize = orb.size * (0.9f + pulse * 0.2f)
    
    // 外层光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                orb.color.copy(alpha = currentAlpha * 0.4f),
                orb.color.copy(alpha = currentAlpha * 0.1f),
                Color.Transparent
            )
        ),
        radius = currentSize * 2f,
        center = Offset(orb.x, orb.y)
    )
    
    // 核心
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = currentAlpha * 0.8f),
                orb.color.copy(alpha = currentAlpha * 0.5f),
                Color.Transparent
            )
        ),
        radius = currentSize * 0.8f,
        center = Offset(orb.x, orb.y)
    )
}
