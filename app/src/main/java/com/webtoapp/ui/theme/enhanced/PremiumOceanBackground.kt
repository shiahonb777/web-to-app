package com.webtoapp.ui.theme.enhanced

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
 * 专业级深海物理主题
 * 
 * 特性：
 * - SPH流体动力学模拟
 * - 真实物理气泡上升
 * - Boids算法驱动的鱼群
 * - 水母物理模拟
 * - 海流场影响
 * - 触摸产生涟漪和力场
 * - 传感器驱动的水流方向
 * ============================================================
 */

@Composable
fun OceanPhysicsBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var lastFrameTime by remember { mutableLongStateOf(System.nanoTime()) }

    // 物理系统
    val oceanWorld = remember { mutableStateOf<OceanPhysicsWorld?>(null) }
    
    // 传感器数据
    var sensorData by remember { mutableStateOf(SensorData()) }
    
    // 触摸交互
    val touchManager = remember { TouchInteractionManager() }
    var dragPosition by remember { mutableStateOf<Vec2?>(null) }
    
    // Listen增强触摸事件
    val enhancedTouchEvent = LocalEnhancedTouchEvent.current
    
    // 传感器监听
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        var lastAccel = Vec2.ZERO
        var shakeAccum = 0f
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val accel = Vec2(event.values[0], event.values[1])
                val tilt = Vec2(
                    (event.values[0] / 9.81f).coerceIn(-1f, 1f),
                    (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                )
                
                // 摇晃检测
                val delta = (accel - lastAccel).length()
                lastAccel = accel
                
                if (delta > 3f) {
                    shakeAccum = (shakeAccum + delta * 0.2f).coerceAtMost(1f)
                    
                    if (shakeAccum > 0.5f) {
                        haptic.performHaptic(HapticType.WATER_DROP, shakeAccum)
                        oceanWorld.value?.createWaveFromShake(shakeAccum)
                    }
                } else {
                    shakeAccum = (shakeAccum - 0.02f).coerceAtLeast(0f)
                }
                
                sensorData = SensorData(
                    gravity = accel,
                    tilt = tilt,
                    shake = shakeAccum
                )
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Initialize物理世界
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && oceanWorld.value == null) {
            oceanWorld.value = OceanPhysicsWorld(
                bounds = PhysicsBounds(0f, canvasSize.width, 0f, canvasSize.height)
            ).apply {
                initialize()
            }
        }
    }
    
    // 主物理循环
    LaunchedEffect(oceanWorld.value) {
        oceanWorld.value?.let { world ->
            while (true) {
                val currentTime = System.nanoTime()
                val dt = ((currentTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrameTime = currentTime
                
                // Update海流（基于传感器）
                world.currentField.baseDirection = Vec2(-sensorData.tilt.x, 0.2f).normalized()
                world.currentField.baseStrength = 20f + abs(sensorData.tilt.x) * 40f
                
                // Update触摸力场
                touchManager.update(dt)
                
                // Update物理世界
                world.update(dt, sensorData, touchManager)
                
                delay(16)
            }
        }
    }
    
    // Response增强触摸事件
    LaunchedEffect(enhancedTouchEvent.value) {
        enhancedTouchEvent.value?.let { event ->
            if (canvasSize.width > 0) {
                val pos = Vec2(event.position.x, event.position.y)
                when (event.type) {
                    EnhancedTouchEvent.TouchType.TAP -> {
                        haptic.performHaptic(HapticType.WATER_DROP)
                        onInteraction()
                        touchManager.forceFields.add(TouchForceField.Explosion(
                            position = pos,
                            radius = 200f,
                            strength = 300f
                        ))
                        oceanWorld.value?.spawnBubblesAt(pos, 12)
                        oceanWorld.value?.rippleSystem?.addRipple(pos, 180f, Color(0xFF00CED1).copy(alpha = 0.5f), 3)
                    }
                    EnhancedTouchEvent.TouchType.LONG_PRESS -> {
                        haptic.performHaptic(HapticType.LONG_PRESS)
                        touchManager.forceFields.add(TouchForceField.Vortex(
                            position = pos,
                            radius = 250f,
                            strength = 400f,
                            life = 3f,
                            decay = 0.008f,
                            clockwise = true
                        ))
                    }
                    EnhancedTouchEvent.TouchType.DRAG -> {
                        dragPosition?.let { oldPos ->
                            val velocity = pos - oldPos
                            if (velocity.length() > 5f) {
                                touchManager.forceFields.add(TouchForceField.Repeller(
                                    position = pos,
                                    radius = 100f,
                                    strength = 150f,
                                    life = 0.3f,
                                    decay = 0.15f
                                ))
                            }
                        }
                        dragPosition = pos
                    }
                    EnhancedTouchEvent.TouchType.DRAG_END -> {
                        dragPosition = null
                    }
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
        
        oceanWorld.value?.let { world ->
            // 绘制深海背景
            drawOceanBackground(width, height, world.depth)
            
            // 绘制光线
            drawUnderwaterLightRays(width, height, world.time, sensorData.tilt)
            
            // 绘制海流可视化
            drawCurrentVisualization(world.currentField, width, height, world.time)

            // 绘制鱼群
            drawFishSchool(world.fishSchool, world.time)
            
            // 绘制水母
            drawJellyfishPhysics(world.jellyfish, world.time)
            
            // 绘制气泡
            drawPhysicsBubbles(world.bubbles, world.trailSystem)
            
            // 绘制浮游生物
            drawPlankton(world.plankton, world.time)
            
            // 绘制触摸力场效果
            drawOceanForceFieldEffects(touchManager.forceFields)
            
            // 绘制涟漪
            drawOceanRipples(world.rippleSystem)
            
            // 绘制焦散光效
            drawCausticLights(width, height, world.time)
        }
    }
}


/**
 * 深海物理世界
 */
class OceanPhysicsWorld(val bounds: PhysicsBounds) {
    var time = 0f
    var depth = 0.5f // 深度 0-1
    
    // 海流场
    val currentField = WindField(
        baseDirection = Vec2(1f, 0.1f).normalized(),
        baseStrength = 30f,
        gustStrength = 50f,
        turbulenceStrength = 20f
    )
    
    // 物理实体
    val bubbles = mutableListOf<PhysicsBubble>()
    val fishSchool = BoidsSystem(
        separationWeight = 1.8f,
        alignmentWeight = 1.2f,
        cohesionWeight = 1.0f,
        separationDist = 25f,
        neighborDist = 70f
    )
    val jellyfish = mutableListOf<PhysicsJellyfish>()
    val plankton = mutableListOf<Plankton>()
    
    // 视觉效果
    val trailSystem = TrailSystem(maxPoints = 12)
    val rippleSystem = RippleSystem()

    fun initialize() {
        // Initialize气泡
        repeat(25) {
            bubbles.add(createBubble())
        }
        
        // Initialize鱼群
        repeat(30) {
            fishSchool.boids.add(createFish())
        }
        
        // Initialize水母
        repeat(5) {
            jellyfish.add(createJellyfish())
        }
        
        // Initialize浮游生物
        repeat(60) {
            plankton.add(createPlankton())
        }
    }
    
    fun update(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        time += dt
        
        // Update海流
        currentField.update(dt)
        
        // Update气泡物理
        updateBubbles(dt, sensorData, touchManager)
        
        // Update鱼群（Boids）
        val attractors = mutableListOf<Vec2>()
        val repellers = mutableListOf<Vec2>()
        
        for (field in touchManager.forceFields) {
            when (field) {
                is TouchForceField.Attractor -> attractors.add(field.position)
                is TouchForceField.Repeller, is TouchForceField.Explosion -> repellers.add(field.position)
                else -> {}
            }
        }
        
        fishSchool.update(dt, bounds, attractors, repellers)
        
        // Update水母
        updateJellyfish(dt, sensorData, touchManager)
        
        // Update浮游生物
        updatePlankton(dt, sensorData)
        
        // Update视觉效果
        trailSystem.update(0.1f)
        rippleSystem.update(dt)
        
        // 补充气泡
        if (bubbles.size < 20 && Random.nextFloat() < 0.05f) {
            bubbles.add(createBubble())
        }
    }

    private fun updateBubbles(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        val buoyancy = Vec2(0f, -200f) // 浮力
        val gravity = Vec2(sensorData.tilt.x * 30f, 0f)
        
        bubbles.forEach { bubble ->
            // 浮力
            bubble.body.applyForce(buoyancy * bubble.body.mass)
            
            // 重力感应
            bubble.body.applyForce(gravity)
            
            // 海流
            val current = currentField.getWindAt(bubble.body.position)
            bubble.body.applyForce(current * 0.3f)
            
            // 触摸力场
            val touchForce = touchManager.getTotalForceAt(bubble.body.position)
            bubble.body.applyForce(touchForce * 0.4f)
            
            // 摆动
            val wobble = sin(time * bubble.wobbleSpeed + bubble.wobblePhase) * 20f
            bubble.body.applyForce(Vec2(wobble, 0f))
            
            // 积分
            bubble.body.integrate(dt)
            
            // 添加轨迹
            if (bubble.body.velocity.length() > 30f) {
                trailSystem.addPoint(bubble, bubble.body.position, bubble.size * 0.3f)
            }
            
            // 生命周期
            bubble.life -= dt * 0.05f
        }
        
        // 移除死亡气泡
        bubbles.removeAll { it.life <= 0 || it.body.position.y < -50 }
    }
    
    private fun updateJellyfish(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        jellyfish.forEach { jf ->
            // 脉冲推进
            jf.pulsePhase += jf.pulseSpeed * dt
            if (jf.pulsePhase > 2 * PI) jf.pulsePhase -= 2 * PI.toFloat()
            
            val pulseForce = sin(jf.pulsePhase) * 80f
            if (pulseForce > 0) {
                jf.body.applyForce(Vec2(0f, -pulseForce))
            }
            
            // 海流影响
            val current = currentField.getWindAt(jf.body.position)
            jf.body.applyForce(current * 0.2f)
            
            // 触摸力场
            val touchForce = touchManager.getTotalForceAt(jf.body.position)
            jf.body.applyForce(touchForce * 0.3f)

            // 重力
            jf.body.applyForce(Vec2(sensorData.tilt.x * 20f, 30f))
            
            // 积分
            jf.body.integrate(dt)
            jf.body.constrainToBounds(bounds)
            
            // 触须相位
            jf.tentaclePhase += dt * 2f
        }
    }
    
    private fun updatePlankton(dt: Float, sensorData: SensorData) {
        plankton.forEach { p ->
            // 海流
            val current = currentField.getWindAt(Vec2(p.x, p.y))
            p.vx += current.x * dt * 0.5f
            p.vy += current.y * dt * 0.5f
            
            // 重力感应
            p.vx += sensorData.tilt.x * 10f * dt
            p.vy += sensorData.tilt.y * 5f * dt
            
            // Shuffle漂移
            p.vx += (Random.nextFloat() - 0.5f) * 20f * dt
            p.vy += (Random.nextFloat() - 0.5f) * 20f * dt
            
            // 阻尼
            p.vx *= 0.98f
            p.vy *= 0.98f
            
            // Update位置
            p.x += p.vx * dt
            p.y += p.vy * dt
            
            // 边界处理
            if (p.x < 0) p.x = bounds.maxX
            if (p.x > bounds.maxX) p.x = 0f
            if (p.y < 0) p.y = bounds.maxY
            if (p.y > bounds.maxY) p.y = 0f
            
            // 发光相位
            p.glowPhase += p.glowSpeed * dt
        }
    }
    
    fun spawnBubblesAt(position: Vec2, count: Int) {
        repeat(count) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val speed = Random.nextFloat() * 50f + 30f
            
            bubbles.add(PhysicsBubble(
                body = PhysicsBody(
                    position = position + Vec2.fromAngle(angle, Random.nextFloat() * 20f),
                    previousPosition = position,
                    mass = Random.nextFloat() * 0.3f + 0.1f,
                    radius = Random.nextFloat() * 8f + 4f,
                    friction = 0.99f
                ).apply {
                    applyImpulse(Vec2.fromAngle(angle - PI.toFloat() / 2, speed))
                },
                size = Random.nextFloat() * 10f + 5f,
                wobblePhase = Random.nextFloat() * 2 * PI.toFloat(),
                wobbleSpeed = Random.nextFloat() * 3f + 2f,
                life = 1f
            ))
        }

        // 限制数量
        while (bubbles.size > 60) {
            bubbles.removeAt(0)
        }
    }
    
    fun createWaveFromShake(intensity: Float) {
        // Create多个涟漪
        repeat((intensity * 5).toInt()) {
            val x = Random.nextFloat() * bounds.maxX
            val y = Random.nextFloat() * bounds.maxY * 0.5f
            rippleSystem.addRipple(Vec2(x, y), 150f + intensity * 50f, Color(0xFF00CED1).copy(alpha = 0.3f), 2)
        }
        
        // 产生气泡
        repeat((intensity * 10).toInt()) {
            bubbles.add(createBubble())
        }
    }
    
    private fun createBubble(): PhysicsBubble {
        val x = Random.nextFloat() * bounds.maxX
        val y = bounds.maxY + Random.nextFloat() * 50f
        
        return PhysicsBubble(
            body = PhysicsBody(
                position = Vec2(x, y),
                previousPosition = Vec2(x, y + 1f),
                mass = Random.nextFloat() * 0.3f + 0.1f,
                radius = Random.nextFloat() * 8f + 4f,
                friction = 0.995f
            ),
            size = Random.nextFloat() * 12f + 4f,
            wobblePhase = Random.nextFloat() * 2 * PI.toFloat(),
            wobbleSpeed = Random.nextFloat() * 3f + 2f,
            life = 1f
        )
    }
    
    private fun createFish(): Boid {
        return Boid(
            position = Vec2(
                Random.nextFloat() * bounds.maxX,
                Random.nextFloat() * bounds.maxY * 0.7f + bounds.maxY * 0.15f
            ),
            velocity = Vec2.fromAngle(Random.nextFloat() * 2 * PI.toFloat(), Random.nextFloat() * 50f + 30f),
            maxSpeed = Random.nextFloat() * 80f + 100f,
            maxForce = Random.nextFloat() * 30f + 40f,
            size = Random.nextFloat() * 8f + 6f,
            color = fishColors.random(),
            type = Random.nextInt(3)
        )
    }
    
    private fun createJellyfish(): PhysicsJellyfish {
        return PhysicsJellyfish(
            body = PhysicsBody(
                position = Vec2(
                    Random.nextFloat() * bounds.maxX,
                    Random.nextFloat() * bounds.maxY * 0.6f + bounds.maxY * 0.2f
                ),
                mass = Random.nextFloat() * 0.5f + 0.3f,
                friction = 0.98f
            ),
            size = Random.nextFloat() * 30f + 20f,
            pulsePhase = Random.nextFloat() * 2 * PI.toFloat(),
            pulseSpeed = Random.nextFloat() * 2f + 1f,
            color = jellyfishColors.random(),
            tentaclePhase = Random.nextFloat() * 2 * PI.toFloat()
        )
    }

    private fun createPlankton(): Plankton {
        return Plankton(
            x = Random.nextFloat() * bounds.maxX,
            y = Random.nextFloat() * bounds.maxY,
            vx = (Random.nextFloat() - 0.5f) * 20f,
            vy = (Random.nextFloat() - 0.5f) * 20f,
            size = Random.nextFloat() * 3f + 1f,
            color = planktonColors.random(),
            glowPhase = Random.nextFloat() * 2 * PI.toFloat(),
            glowSpeed = Random.nextFloat() * 2f + 1f
        )
    }
    
    companion object {
        val fishColors = listOf(
            Color(0xFF00CED1), Color(0xFF20B2AA), Color(0xFF48D1CC),
            Color(0xFF40E0D0), Color(0xFF7FFFD4), Color(0xFFFF6B6B),
            Color(0xFFFFD93D), Color(0xFF6BCB77)
        )
        
        val jellyfishColors = listOf(
            Color(0xFFE040FB), Color(0xFF7C4DFF), Color(0xFF536DFE),
            Color(0xFF448AFF), Color(0xFF40C4FF), Color(0xFFFF80AB)
        )
        
        val planktonColors = listOf(
            Color(0xFF00E5FF), Color(0xFF18FFFF), Color(0xFF64FFDA),
            Color(0xFFA7FFEB), Color(0xFFB2FF59)
        )
    }
}

// 数据类
data class PhysicsBubble(
    val body: PhysicsBody,
    var size: Float,
    var wobblePhase: Float,
    var wobbleSpeed: Float,
    var life: Float
)

data class PhysicsJellyfish(
    val body: PhysicsBody,
    var size: Float,
    var pulsePhase: Float,
    var pulseSpeed: Float,
    var color: Color,
    var tentaclePhase: Float
)

data class Plankton(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var color: Color,
    var glowPhase: Float,
    var glowSpeed: Float
)


// ==================== 绘制函数 ====================

private fun DrawScope.drawOceanBackground(width: Float, height: Float, depth: Float) {
    val colors = listOf(
        Color(0xFF001830),
        Color(0xFF002850),
        Color(0xFF003870),
        Color(0xFF004890),
        Color(0xFF0058A0)
    )
    
    drawRect(brush = Brush.verticalGradient(colors = colors))
}

private fun DrawScope.drawUnderwaterLightRays(width: Float, height: Float, time: Float, tilt: Vec2) {
    val rayCount = 8
    
    for (i in 0 until rayCount) {
        val baseX = width * (i + 0.5f) / rayCount + tilt.x * 30f
        val sway = sin(time * 0.5f + i * 0.5f) * 30f
        
        val path = Path().apply {
            moveTo(baseX + sway - 40f, -20f)
            lineTo(baseX + sway + 40f, -20f)
            lineTo(baseX + sway * 2 + 80f, height * 0.7f)
            lineTo(baseX + sway * 2 - 80f, height * 0.7f)
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF87CEEB).copy(alpha = 0.15f),
                    Color(0xFF00CED1).copy(alpha = 0.05f),
                    Color.Transparent
                )
            )
        )
    }
}

private fun DrawScope.drawCurrentVisualization(currentField: WindField, width: Float, height: Float, time: Float) {
    val gridSize = 80f
    val cols = (width / gridSize).toInt() + 1
    val rows = (height / gridSize).toInt() + 1
    
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val x = col * gridSize + gridSize / 2
            val y = row * gridSize + gridSize / 2
            
            val current = currentField.getWindAt(Vec2(x, y))
            val strength = current.length() / 100f
            val alpha = (strength * 0.15f).coerceIn(0f, 0.2f)
            
            if (alpha > 0.02f) {
                val angle = atan2(current.y, current.x)
                val lineLength = 15f + strength * 10f

                val endX = x + cos(angle) * lineLength
                val endY = y + sin(angle) * lineLength
                
                drawLine(
                    color = Color(0xFF00CED1).copy(alpha = alpha),
                    start = Offset(x, y),
                    end = Offset(endX, endY),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawFishSchool(fishSchool: BoidsSystem, time: Float) {
    for (fish in fishSchool.boids) {
        val angle = atan2(fish.velocity.y, fish.velocity.x)
        val speed = fish.velocity.length()
        val tailWag = sin(time * 10f + fish.position.x * 0.1f) * 0.3f
        
        rotate(degrees = angle * 180f / PI.toFloat(), pivot = Offset(fish.position.x, fish.position.y)) {
            // 身体
            val bodyPath = Path().apply {
                moveTo(fish.position.x + fish.size, fish.position.y)
                quadraticBezierTo(
                    fish.position.x + fish.size * 0.3f, fish.position.y - fish.size * 0.4f,
                    fish.position.x - fish.size * 0.5f, fish.position.y
                )
                quadraticBezierTo(
                    fish.position.x + fish.size * 0.3f, fish.position.y + fish.size * 0.4f,
                    fish.position.x + fish.size, fish.position.y
                )
                close()
            }
            
            drawPath(
                path = bodyPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        fish.color.copy(alpha = 0.9f),
                        fish.color.copy(alpha = 0.6f)
                    ),
                    startX = fish.position.x + fish.size,
                    endX = fish.position.x - fish.size
                )
            )
            
            // 尾巴
            val tailPath = Path().apply {
                moveTo(fish.position.x - fish.size * 0.5f, fish.position.y)
                lineTo(fish.position.x - fish.size * 1.2f, fish.position.y - fish.size * 0.4f + tailWag * fish.size)
                lineTo(fish.position.x - fish.size * 1.2f, fish.position.y + fish.size * 0.4f + tailWag * fish.size)
                close()
            }
            
            drawPath(path = tailPath, color = fish.color.copy(alpha = 0.7f))
            
            // 眼睛
            drawCircle(
                color = Color.White,
                radius = fish.size * 0.15f,
                center = Offset(fish.position.x + fish.size * 0.5f, fish.position.y - fish.size * 0.1f)
            )
            drawCircle(
                color = Color.Black,
                radius = fish.size * 0.08f,
                center = Offset(fish.position.x + fish.size * 0.55f, fish.position.y - fish.size * 0.1f)
            )
        }
    }
}


private fun DrawScope.drawJellyfishPhysics(jellyfish: List<PhysicsJellyfish>, time: Float) {
    for (jf in jellyfish) {
        val pulse = (sin(jf.pulsePhase) + 1f) / 2f
        val currentSize = jf.size * (0.8f + pulse * 0.4f)
        
        // 外层光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    jf.color.copy(alpha = 0.2f),
                    jf.color.copy(alpha = 0.05f),
                    Color.Transparent
                )
            ),
            radius = currentSize * 2.5f,
            center = Offset(jf.body.position.x, jf.body.position.y)
        )
        
        // 伞体
        val umbrellaPath = Path().apply {
            moveTo(jf.body.position.x - currentSize, jf.body.position.y)
            quadraticBezierTo(
                jf.body.position.x - currentSize * 0.8f, jf.body.position.y - currentSize * (0.6f + pulse * 0.2f),
                jf.body.position.x, jf.body.position.y - currentSize * (0.8f + pulse * 0.2f)
            )
            quadraticBezierTo(
                jf.body.position.x + currentSize * 0.8f, jf.body.position.y - currentSize * (0.6f + pulse * 0.2f),
                jf.body.position.x + currentSize, jf.body.position.y
            )
            quadraticBezierTo(
                jf.body.position.x + currentSize * 0.5f, jf.body.position.y + currentSize * 0.2f,
                jf.body.position.x, jf.body.position.y + currentSize * 0.1f
            )
            quadraticBezierTo(
                jf.body.position.x - currentSize * 0.5f, jf.body.position.y + currentSize * 0.2f,
                jf.body.position.x - currentSize, jf.body.position.y
            )
            close()
        }
        
        drawPath(
            path = umbrellaPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    jf.color.copy(alpha = 0.8f),
                    jf.color.copy(alpha = 0.5f),
                    jf.color.copy(alpha = 0.3f)
                ),
                center = Offset(jf.body.position.x, jf.body.position.y - currentSize * 0.3f)
            )
        )
        
        // 触须
        val tentacleCount = 8
        for (i in 0 until tentacleCount) {
            val baseX = jf.body.position.x + (i - tentacleCount / 2f + 0.5f) * currentSize * 0.2f
            val baseY = jf.body.position.y + currentSize * 0.1f
            
            val tentaclePath = Path().apply {
                moveTo(baseX, baseY)
                
                var x = baseX
                var y = baseY
                val segments = 6
                
                for (s in 0 until segments) {
                    val wave = sin(jf.tentaclePhase + i * 0.5f + s * 0.8f) * 8f
                    x += wave
                    y += currentSize * 0.15f
                    lineTo(x, y)
                }
            }
            
            drawPath(
                path = tentaclePath,
                color = jf.color.copy(alpha = 0.5f - i * 0.03f),
                style = Stroke(width = 2f - i * 0.1f, cap = StrokeCap.Round)
            )
        }
    }
}


private fun DrawScope.drawPhysicsBubbles(bubbles: List<PhysicsBubble>, trailSystem: TrailSystem) {
    for (bubble in bubbles) {
        // 绘制轨迹
        val trail = trailSystem.getTrail(bubble)
        if (trail.size > 1) {
            for (i in 1 until trail.size) {
                val alpha = (trail[i].alpha * bubble.life * 0.2f).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(0xFF87CEEB).copy(alpha = alpha),
                    radius = trail[i].size,
                    center = Offset(trail[i].position.x, trail[i].position.y)
                )
            }
        }
        
        val alpha = bubble.life.coerceIn(0f, 1f)
        
        // 外层光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF87CEEB).copy(alpha = alpha * 0.3f),
                    Color.Transparent
                )
            ),
            radius = bubble.size * 2f,
            center = Offset(bubble.body.position.x, bubble.body.position.y)
        )
        
        // 气泡主体
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha * 0.1f),
                    Color(0xFF87CEEB).copy(alpha = alpha * 0.3f),
                    Color(0xFF00CED1).copy(alpha = alpha * 0.2f)
                ),
                center = Offset(
                    bubble.body.position.x - bubble.size * 0.2f,
                    bubble.body.position.y - bubble.size * 0.2f
                )
            ),
            radius = bubble.size,
            center = Offset(bubble.body.position.x, bubble.body.position.y)
        )
        
        // 高光
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.6f),
            radius = bubble.size * 0.25f,
            center = Offset(
                bubble.body.position.x - bubble.size * 0.3f,
                bubble.body.position.y - bubble.size * 0.3f
            )
        )
        
        // 边缘
        drawCircle(
            color = Color(0xFF87CEEB).copy(alpha = alpha * 0.4f),
            radius = bubble.size,
            center = Offset(bubble.body.position.x, bubble.body.position.y),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawPlankton(plankton: List<Plankton>, time: Float) {
    for (p in plankton) {
        val glow = (sin(p.glowPhase) + 1f) / 2f
        val alpha = (0.3f + glow * 0.5f).coerceIn(0f, 1f)
        
        // 光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    p.color.copy(alpha = alpha * 0.4f),
                    Color.Transparent
                )
            ),
            radius = p.size * 4f,
            center = Offset(p.x, p.y)
        )
        
        // 核心
        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = p.size,
            center = Offset(p.x, p.y)
        )
    }
}


private fun DrawScope.drawOceanForceFieldEffects(forceFields: List<TouchForceField>) {
    for (field in forceFields) {
        val alpha = (field.life * 0.5f).coerceIn(0f, 1f)
        
        when (field) {
            is TouchForceField.Vortex -> {
                // 漩涡 - 水流螺旋
                val spiralPath = Path().apply {
                    var angle = 0f
                    var radius = 10f
                    moveTo(
                        field.position.x + cos(angle) * radius,
                        field.position.y + sin(angle) * radius
                    )
                    while (radius < field.radius * field.life) {
                        angle += if (field.clockwise) 0.25f else -0.25f
                        radius += 4f
                        lineTo(
                            field.position.x + cos(angle) * radius,
                            field.position.y + sin(angle) * radius
                        )
                    }
                }
                drawPath(
                    path = spiralPath,
                    color = Color(0xFF00CED1).copy(alpha = alpha),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
            is TouchForceField.Explosion -> {
                // 爆炸 - 水波扩散
                for (i in 0 until 3) {
                    val ringRadius = field.currentRadius * (0.6f + i * 0.2f)
                    drawCircle(
                        color = Color(0xFF87CEEB).copy(alpha = alpha * (1f - i * 0.3f)),
                        radius = ringRadius,
                        center = Offset(field.position.x, field.position.y),
                        style = Stroke(width = 3f - i)
                    )
                }
            }
            is TouchForceField.Attractor -> {
                // 吸引 - 向内的圆环
                for (i in 0 until 3) {
                    val ringRadius = field.radius * (field.life * 0.3f + i * 0.25f)
                    drawCircle(
                        color = Color(0xFF00CED1).copy(alpha = alpha * (1f - i * 0.25f)),
                        radius = ringRadius,
                        center = Offset(field.position.x, field.position.y),
                        style = Stroke(width = 2f)
                    )
                }
            }
            is TouchForceField.Repeller -> {
                // 排斥 - 向外的圆环
                for (i in 0 until 2) {
                    val ringRadius = field.radius * field.life * (0.5f + i * 0.3f)
                    drawCircle(
                        color = Color(0xFF48D1CC).copy(alpha = alpha * (1f - i * 0.4f)),
                        radius = ringRadius,
                        center = Offset(field.position.x, field.position.y),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawOceanRipples(rippleSystem: RippleSystem) {
    for (ripple in rippleSystem.ripples) {
        val alpha = ripple.alpha.coerceIn(0f, 1f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    ripple.color.copy(alpha = alpha * 0.3f),
                    ripple.color.copy(alpha = alpha * 0.15f),
                    Color.Transparent
                )
            ),
            radius = ripple.radius,
            center = Offset(ripple.center.x, ripple.center.y)
        )
        
        drawCircle(
            color = ripple.color.copy(alpha = alpha * 0.5f),
            radius = ripple.radius,
            center = Offset(ripple.center.x, ripple.center.y),
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawCausticLights(width: Float, height: Float, time: Float) {
    val causticCount = 12
    
    for (i in 0 until causticCount) {
        val baseX = (i + 0.5f) * width / causticCount
        val baseY = height * 0.3f + sin(time * 0.8f + i * 0.7f) * 50f
        val size = 40f + sin(time * 1.2f + i) * 15f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF87CEEB).copy(alpha = 0.08f),
                    Color.Transparent
                )
            ),
            radius = size,
            center = Offset(baseX + sin(time + i) * 20f, baseY)
        )
    }
}
