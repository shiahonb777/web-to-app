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
 * 专业级实时物理主题背景
 * 
 * 特性：
 * - 真实物理引擎驱动
 * - 实时传感器交互
 * - 流体动力学模拟
 * - 群体智能行为
 * - 生态系统模拟
 * - 多点触控力场
 * ============================================================
 */

// ==================== 森林生态主题 ====================

/**
 * 森林生态主题 - 真实生态系统模拟
 * 
 * 特性：
 * - 真实物理引擎驱动的树叶飘落
 * - Boids算法驱动的萤火虫群
 * - 生态系统：植物、猎物、捕食者
 * - 实时风场影响
 * - 触摸产生力场交互
 * - 传感器驱动的环境变化
 */
@Composable
fun ForestEcosystemBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var lastFrameTime by remember { mutableLongStateOf(System.nanoTime()) }
    
    // 物理系统
    val physicsWorld = remember { mutableStateOf<ForestPhysicsWorld?>(null) }
    
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
        
        var lastShakeTime = 0L
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
                    
                    // 摇晃触发效果
                    if (shakeAccum > 0.5f && System.currentTimeMillis() - lastShakeTime > 500) {
                        lastShakeTime = System.currentTimeMillis()
                        haptic.performHaptic(HapticType.IMPACT_MEDIUM, shakeAccum)
                        
                        // 摇晃产生树叶
                        physicsWorld.value?.spawnLeavesFromShake(shakeAccum * 20)
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
        if (canvasSize.width > 0 && physicsWorld.value == null) {
            physicsWorld.value = ForestPhysicsWorld(
                bounds = PhysicsBounds(0f, canvasSize.width, 0f, canvasSize.height)
            ).apply {
                initialize()
            }
        }
    }
    
    // 主物理循环
    LaunchedEffect(physicsWorld.value) {
        physicsWorld.value?.let { world ->
            while (true) {
                val currentTime = System.nanoTime()
                val dt = ((currentTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrameTime = currentTime
                
                // Update风场（基于传感器）
                world.windField.baseDirection = Vec2(-sensorData.tilt.x, 0.3f).normalized()
                world.windField.baseStrength = 30f + abs(sensorData.tilt.x) * 50f
                
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
                        haptic.performHaptic(HapticType.SPARKLE)
                        onInteraction()
                        touchManager.forceFields.add(TouchForceField.Explosion(
                            position = pos,
                            radius = 250f,
                            strength = 400f
                        ))
                        physicsWorld.value?.spawnFirefliesAt(pos, 8)
                    }
                    EnhancedTouchEvent.TouchType.LONG_PRESS -> {
                        haptic.performHaptic(HapticType.LONG_PRESS)
                        touchManager.forceFields.add(TouchForceField.Attractor(
                            position = pos,
                            radius = 300f,
                            strength = 500f,
                            life = 3f,
                            decay = 0.008f
                        ))
                    }
                    EnhancedTouchEvent.TouchType.DRAG -> {
                        dragPosition?.let { oldPos ->
                            val velocity = pos - oldPos
                            if (velocity.length() > 5f) {
                                touchManager.forceFields.add(TouchForceField.Vortex(
                                    position = pos,
                                    radius = 120f,
                                    strength = 200f,
                                    life = 0.5f,
                                    decay = 0.1f,
                                    clockwise = velocity.x > 0
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
        
        physicsWorld.value?.let { world ->
            // 绘制背景渐变
            drawForestBackground(width, height, world.timeOfDay)
            
            // 绘制体积光
            drawVolumetricLight(width, height, world.sunPosition, sensorData.tilt)
            
            // 绘制远景森林
            drawForestLayers(width, height, sensorData.tilt, world.windField)
            
            // 绘制雾气
            drawFogLayers(width, height, world.time, world.windField)
            
            // 绘制生态系统实体
            drawEcosystemEntities(world.ecosystem)
            
            // 绘制树叶
            drawPhysicsLeaves(world.leaves, world.trailSystem)
            
            // 绘制萤火虫
            drawFireflies(world.fireflies, world.time)
            
            // 绘制触摸力场效果
            drawForceFieldEffects(touchManager.forceFields)
            
            // 绘制涟漪
            drawRipples(world.rippleSystem)
            
            // 绘制前景雾气
            drawForegroundFog(width, height, world.time)
        }
    }
}


/**
 * 森林物理世界
 */
class ForestPhysicsWorld(val bounds: PhysicsBounds) {
    // Time
    var time = 0f
    var timeOfDay = 0.3f // 0-1, 0.3 = 早晨
    var sunPosition = Vec2(0f, 0f)
    
    // 物理系统
    val windField = WindField(
        baseDirection = Vec2(1f, 0.2f).normalized(),
        baseStrength = 40f,
        gustStrength = 80f,
        turbulenceStrength = 25f
    )
    
    // 粒子系统
    val leaves = mutableListOf<PhysicsLeaf>()
    val fireflies = mutableListOf<Firefly>()
    
    // 生态系统
    val ecosystem = Ecosystem(bounds)
    
    // 视觉效果
    val trailSystem = TrailSystem(maxPoints = 15)
    val rippleSystem = RippleSystem()
    val glowEffects = mutableListOf<GlowEffect>()
    
    fun initialize() {
        // Initialize树叶 - 减少数量以提升性能
        repeat(15) {
            leaves.add(createLeaf())
        }
        
        // Initialize萤火虫（使用Boids）- 减少数量以提升性能
        repeat(20) {
            fireflies.add(createFirefly())
        }
        
        // Initialize生态系统 - 减少实体数量
        ecosystem.initialize(plantCount = 8, preyCount = 4, predatorCount = 1)
    }
    
    fun update(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        time += dt
        
        // Update时间（缓慢变化）
        timeOfDay = (timeOfDay + dt * 0.01f) % 1f
        sunPosition = Vec2(
            bounds.maxX * (0.2f + timeOfDay * 0.6f),
            bounds.maxY * (0.1f + sin(timeOfDay * PI.toFloat()) * 0.2f)
        )
        
        // Update风场
        windField.update(dt)
        
        // Update树叶物理
        updateLeaves(dt, sensorData, touchManager)
        
        // Update萤火虫（Boids行为）
        updateFireflies(dt, sensorData, touchManager)
        
        // Update生态系统
        ecosystem.update(dt)
        
        // Update视觉效果
        trailSystem.update(0.08f)
        rippleSystem.update(dt)
        glowEffects.forEach { it.update(dt) }
        
        // 补充树叶 - 降低上限和生成概率
        if (leaves.size < 12 && Random.nextFloat() < 0.02f) {
            leaves.add(createLeaf())
        }
    }
    
    private fun updateLeaves(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        val gravity = Vec2(sensorData.tilt.x * 50f, 150f + sensorData.tilt.y * 30f)
        
        leaves.forEach { leaf ->
            // 重力
            leaf.body.applyForce(gravity * leaf.body.mass)
            
            // 风力
            val wind = windField.getWindAt(leaf.body.position)
            leaf.body.applyForce(wind * leaf.windResistance)
            
            // 触摸力场
            val touchForce = touchManager.getTotalForceAt(leaf.body.position)
            leaf.body.applyForce(touchForce * 0.5f)
            
            // 摇摆效果
            val sway = sin(time * leaf.swayFrequency + leaf.swayPhase) * leaf.swayAmplitude
            leaf.body.applyForce(Vec2(sway, 0f))
            
            // 积分
            leaf.body.integrate(dt)
            leaf.body.constrainToBounds(bounds)
            
            // Update旋转
            leaf.rotation += leaf.rotationSpeed * dt + wind.x * 0.01f
            
            // 添加轨迹
            if (leaf.body.velocity.length() > 20f) {
                trailSystem.addPoint(leaf, leaf.body.position, leaf.size * 0.5f)
            }
            
            // 生命周期
            leaf.life -= dt * 0.1f
        }
        
        // 移除死亡树叶
        leaves.removeAll { it.life <= 0 || it.body.position.y > bounds.maxY + 50 }
    }
    
    private fun updateFireflies(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        // 收集吸引点和排斥点
        val attractors = mutableListOf<Vec2>()
        val repellers = mutableListOf<Vec2>()
        
        for (field in touchManager.forceFields) {
            when (field) {
                is TouchForceField.Attractor -> attractors.add(field.position)
                is TouchForceField.Repeller, is TouchForceField.Explosion -> repellers.add(field.position)
                else -> {}
            }
        }
        
        // Boids行为参数 - 简化距离减少计算
        val separationDist = 20f
        val alignmentDist = 40f
        val cohesionDist = 60f
        
        for (firefly in fireflies) {
            // 分离
            var separation = Vec2.ZERO
            var alignment = Vec2.ZERO
            var cohesion = Vec2.ZERO
            var neighborCount = 0
            
            for (other in fireflies) {
                if (other === firefly) continue
                val diff = firefly.position - other.position
                val dist = diff.length()
                
                if (dist < separationDist && dist > 0) {
                    separation = separation + diff.normalized() / dist * 50f
                }
                if (dist < alignmentDist) {
                    alignment = alignment + other.velocity
                    neighborCount++
                }
                if (dist < cohesionDist) {
                    cohesion = cohesion + other.position
                }
            }
            
            if (neighborCount > 0) {
                alignment = (alignment / neighborCount.toFloat()).normalized() * firefly.maxSpeed
                cohesion = ((cohesion / neighborCount.toFloat()) - firefly.position).normalized() * firefly.maxSpeed
            }
            
            // 边界约束
            var boundary = Vec2.ZERO
            val margin = 80f
            if (firefly.position.x < bounds.minX + margin) boundary = boundary + Vec2(firefly.maxForce, 0f)
            if (firefly.position.x > bounds.maxX - margin) boundary = boundary + Vec2(-firefly.maxForce, 0f)
            if (firefly.position.y < bounds.minY + margin) boundary = boundary + Vec2(0f, firefly.maxForce)
            if (firefly.position.y > bounds.maxY * 0.7f) boundary = boundary + Vec2(0f, -firefly.maxForce)
            
            // 吸引点
            var attraction = Vec2.ZERO
            for (attractor in attractors) {
                val diff = attractor - firefly.position
                val dist = diff.length()
                if (dist < 200f && dist > 10f) {
                    attraction = attraction + diff.normalized() * firefly.maxSpeed * 0.8f
                }
            }
            
            // 排斥点
            var repulsion = Vec2.ZERO
            for (repeller in repellers) {
                val diff = firefly.position - repeller
                val dist = diff.length()
                if (dist < 150f && dist > 10f) {
                    repulsion = repulsion + diff.normalized() * firefly.maxForce * 2f
                }
            }
            
            // Shuffle漫游
            firefly.wanderAngle += (Random.nextFloat() - 0.5f) * 3f * dt
            val wander = Vec2.fromAngle(firefly.wanderAngle, 30f)
            
            // 合并所有力
            val totalForce = separation * 1.5f + 
                            (alignment - firefly.velocity).normalized() * 0.8f +
                            (cohesion - firefly.velocity).normalized() * 0.6f +
                            boundary + attraction + repulsion + wander
            
            // App力
            firefly.velocity = firefly.velocity + totalForce * dt
            val speed = firefly.velocity.length()
            if (speed > firefly.maxSpeed) {
                firefly.velocity = firefly.velocity.normalized() * firefly.maxSpeed
            }
            
            // Update位置
            firefly.position = firefly.position + firefly.velocity * dt
            
            // Update发光相位
            firefly.glowPhase += firefly.glowSpeed * dt
            if (firefly.glowPhase > 2 * PI) firefly.glowPhase -= 2 * PI.toFloat()
            
            // 添加轨迹
            if (firefly.velocity.length() > 15f) {
                trailSystem.addPoint(firefly, firefly.position, firefly.size * 0.3f)
            }
        }
    }
    
    fun spawnLeavesFromShake(count: Float) {
        repeat(count.toInt()) {
            val leaf = createLeaf()
            leaf.body.position = Vec2(
                Random.nextFloat() * bounds.maxX,
                bounds.maxY * 0.2f + Random.nextFloat() * bounds.maxY * 0.3f
            )
            leaf.body.applyImpulse(Vec2(
                (Random.nextFloat() - 0.5f) * 100f,
                Random.nextFloat() * 50f
            ))
            leaves.add(leaf)
        }
        
        // 添加涟漪效果
        rippleSystem.addRipple(
            Vec2(bounds.maxX / 2, bounds.maxY * 0.3f),
            300f,
            Color(0xFF4CAF50).copy(alpha = 0.3f),
            2
        )
    }
    
    fun spawnFirefliesAt(position: Vec2, count: Int) {
        repeat(count) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val speed = Random.nextFloat() * 80f + 40f
            
            fireflies.add(Firefly(
                position = position + Vec2.fromAngle(angle, Random.nextFloat() * 20f),
                velocity = Vec2.fromAngle(angle, speed),
                size = Random.nextFloat() * 4f + 3f,
                color = fireflyColors.random(),
                glowPhase = Random.nextFloat() * 2 * PI.toFloat(),
                glowSpeed = Random.nextFloat() * 3f + 2f,
                maxSpeed = Random.nextFloat() * 60f + 80f,
                maxForce = Random.nextFloat() * 30f + 40f
            ))
        }
        
        // 限制数量
        while (fireflies.size > 80) {
            fireflies.removeAt(0)
        }
        
        // 添加涟漪
        rippleSystem.addRipple(position, 150f, Color(0xFFB7E4C7).copy(alpha = 0.5f), 3)
    }
    
    private fun createLeaf(): PhysicsLeaf {
        val startX = Random.nextFloat() * bounds.maxX
        val startY = -Random.nextFloat() * 50f - 20f
        
        return PhysicsLeaf(
            body = PhysicsBody(
                position = Vec2(startX, startY),
                previousPosition = Vec2(startX, startY),
                mass = Random.nextFloat() * 0.5f + 0.3f,
                radius = Random.nextFloat() * 8f + 6f,
                friction = 0.995f
            ),
            size = Random.nextFloat() * 12f + 8f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 120f,
            color = leafColors.random(),
            windResistance = Random.nextFloat() * 0.3f + 0.2f,
            swayFrequency = Random.nextFloat() * 2f + 1f,
            swayAmplitude = Random.nextFloat() * 15f + 10f,
            swayPhase = Random.nextFloat() * 2 * PI.toFloat(),
            life = 1f
        )
    }
    
    private fun createFirefly(): Firefly {
        return Firefly(
            position = Vec2(
                Random.nextFloat() * bounds.maxX,
                Random.nextFloat() * bounds.maxY * 0.6f
            ),
            velocity = Vec2.fromAngle(Random.nextFloat() * 2 * PI.toFloat(), Random.nextFloat() * 30f),
            size = Random.nextFloat() * 4f + 3f,
            color = fireflyColors.random(),
            glowPhase = Random.nextFloat() * 2 * PI.toFloat(),
            glowSpeed = Random.nextFloat() * 3f + 2f,
            maxSpeed = Random.nextFloat() * 40f + 60f,
            maxForce = Random.nextFloat() * 20f + 30f
        )
    }
    
    companion object {
        val leafColors = listOf(
            Color(0xFF2D6A4F), Color(0xFF40916C), Color(0xFF52B788),
            Color(0xFF74C69D), Color(0xFF95D5B2), Color(0xFF1B4332),
            Color(0xFFD4A373), Color(0xFFE9C46A) // 秋叶色
        )
        
        val fireflyColors = listOf(
            Color(0xFFB7E4C7), Color(0xFF74C69D), Color(0xFFD8F3DC),
            Color(0xFF95D5B2), Color(0xFFA7E8BD), Color(0xFFFFE66D)
        )
    }
}

/**
 * 物理树叶
 */
data class PhysicsLeaf(
    val body: PhysicsBody,
    var size: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var color: Color,
    var windResistance: Float,
    var swayFrequency: Float,
    var swayAmplitude: Float,
    var swayPhase: Float,
    var life: Float
)

/**
 * 萤火虫
 */
data class Firefly(
    var position: Vec2,
    var velocity: Vec2,
    var size: Float,
    var color: Color,
    var glowPhase: Float,
    var glowSpeed: Float,
    var maxSpeed: Float,
    var maxForce: Float,
    var wanderAngle: Float = Random.nextFloat() * 2 * PI.toFloat()
)


// ==================== 森林主题绘制函数 ====================

private fun DrawScope.drawForestBackground(width: Float, height: Float, timeOfDay: Float) {
    // 根据时间变化的天空颜色
    val skyColors = when {
        timeOfDay < 0.25f -> listOf( // 黎明
            Color(0xFF1A237E), Color(0xFF283593), Color(0xFF3949AB), Color(0xFF5C6BC0)
        )
        timeOfDay < 0.5f -> listOf( // 早晨
            Color(0xFF0A1A0F), Color(0xFF0F2818), Color(0xFF1A3D25), Color(0xFF2D5A3A)
        )
        timeOfDay < 0.75f -> listOf( // 下午
            Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF40916C), Color(0xFF52B788)
        )
        else -> listOf( // 黄昏
            Color(0xFF1A0A1A), Color(0xFF2D1B2D), Color(0xFF4A2C4A), Color(0xFF6B3D6B)
        )
    }
    
    drawRect(brush = Brush.verticalGradient(colors = skyColors))
}

private fun DrawScope.drawVolumetricLight(
    width: Float, height: Float, sunPosition: Vec2, tilt: Vec2
) {
    val rayCount = 6
    val sourceX = sunPosition.x + tilt.x * 30f
    val sourceY = sunPosition.y + tilt.y * 20f
    
    for (i in 0 until rayCount) {
        val baseAngle = (i.toFloat() / rayCount) * 0.5f + 0.25f
        val angle = baseAngle * PI.toFloat()
        val rayLength = height * 1.2f
        val rayWidth = 0.08f
        
        val path = Path().apply {
            moveTo(sourceX, sourceY)
            lineTo(sourceX + cos(angle - rayWidth) * rayLength, sourceY + sin(angle - rayWidth) * rayLength)
            lineTo(sourceX + cos(angle + rayWidth) * rayLength, sourceY + sin(angle + rayWidth) * rayLength)
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = 0.12f),
                    Color(0xFF95D5B2).copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(sourceX, sourceY),
                radius = rayLength
            )
        )
    }
}

private fun DrawScope.drawForestLayers(
    width: Float, height: Float, tilt: Vec2, windField: WindField
) {
    val layers = listOf(
        Triple(0.5f, Color(0xFF0A1A0F), 0.15f),
        Triple(0.58f, Color(0xFF0D1F14), 0.25f),
        Triple(0.65f, Color(0xFF102518), 0.4f),
        Triple(0.72f, Color(0xFF152D1E), 0.6f),
        Triple(0.78f, Color(0xFF1A3D25), 0.85f)
    )
    
    layers.forEachIndexed { index, (baseY, color, parallax) ->
        val offsetX = tilt.x * parallax * 25f
        val windSway = sin(windField.baseStrength * 0.01f) * parallax * 8f
        
        val path = Path().apply {
            moveTo(-50f + offsetX, height)
            
            var x = -50f + offsetX
            val seed = index * 1000
            while (x < width + 100) {
                val treeHeight = height * (0.25f + Random(seed + x.toInt()).nextFloat() * 0.25f)
                val treeWidth = 25f + Random(seed + x.toInt() + 1).nextFloat() * 35f
                
                lineTo(x, height - treeHeight * 0.1f)
                quadraticBezierTo(
                    x + treeWidth * 0.3f + windSway, height - treeHeight,
                    x + treeWidth * 0.5f + windSway * 0.5f, height - treeHeight * 0.3f
                )
                quadraticBezierTo(
                    x + treeWidth * 0.7f + windSway * 0.3f, height - treeHeight * 0.8f,
                    x + treeWidth, height - treeHeight * 0.1f
                )
                
                x += treeWidth + Random(seed + x.toInt() + 2).nextFloat() * 15f
            }
            
            lineTo(width + 100, height)
            close()
        }
        
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawFogLayers(
    width: Float, height: Float, time: Float, windField: WindField
) {
    val fogLayers = listOf(
        Triple(0.4f, 0.1f, 0.6f),
        Triple(0.55f, 0.07f, 0.4f),
        Triple(0.7f, 0.04f, 0.25f)
    )
    
    fogLayers.forEachIndexed { index, (yRatio, alpha, speed) ->
        val baseY = height * yRatio
        val offset = sin(time * speed + index) * 40f
        
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF1A3D25).copy(alpha = alpha),
                    Color(0xFF0F2818).copy(alpha = alpha * 0.5f),
                    Color.Transparent
                ),
                startY = baseY - 80 + offset,
                endY = baseY + 80 + offset
            )
        )
    }
}

private fun DrawScope.drawEcosystemEntities(ecosystem: Ecosystem) {
    for (entity in ecosystem.entities) {
        when (entity) {
            is PlantEntity -> {
                // 植物
                val growthScale = entity.growthStage
                val stemHeight = entity.size * 1.5f * growthScale
                
                // 茎
                drawLine(
                    color = Color(0xFF228B22).copy(alpha = 0.8f),
                    start = Offset(entity.position.x, entity.position.y),
                    end = Offset(entity.position.x, entity.position.y - stemHeight),
                    strokeWidth = 2f + growthScale * 2f,
                    cap = StrokeCap.Round
                )
                
                // 花/叶
                if (growthScale > 0.3f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                entity.color.copy(alpha = 0.9f),
                                entity.color.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        radius = entity.size * growthScale,
                        center = Offset(entity.position.x, entity.position.y - stemHeight)
                    )
                }
            }
            is PreyEntity -> {
                // 猎物（蝴蝶效果）
                val wingAngle = sin(entity.age * 8f) * 30f
                val fearColor = if (entity.fearLevel > 0.5f) Color.Red.copy(alpha = 0.3f) else Color.Transparent
                
                // 身体
                drawCircle(
                    color = entity.color,
                    radius = entity.size * 0.4f,
                    center = Offset(entity.position.x, entity.position.y)
                )
                
                // 翅膀
                rotate(wingAngle, pivot = Offset(entity.position.x, entity.position.y)) {
                    drawOval(
                        color = entity.color.copy(alpha = 0.8f),
                        topLeft = Offset(entity.position.x - entity.size, entity.position.y - entity.size * 0.5f),
                        size = Size(entity.size, entity.size * 0.6f)
                    )
                }
                rotate(-wingAngle, pivot = Offset(entity.position.x, entity.position.y)) {
                    drawOval(
                        color = entity.color.copy(alpha = 0.8f),
                        topLeft = Offset(entity.position.x, entity.position.y - entity.size * 0.5f),
                        size = Size(entity.size, entity.size * 0.6f)
                    )
                }
                
                // 恐惧指示
                if (entity.fearLevel > 0.3f) {
                    drawCircle(
                        color = fearColor,
                        radius = entity.size * 2f * entity.fearLevel,
                        center = Offset(entity.position.x, entity.position.y),
                        style = Stroke(width = 1f)
                    )
                }
            }
            is PredatorEntity -> {
                // 捕食者
                val huntingGlow = if (entity.targetPrey != null) 0.5f else 0f
                
                // 光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            entity.color.copy(alpha = 0.3f + huntingGlow * 0.3f),
                            Color.Transparent
                        )
                    ),
                    radius = entity.size * 3f,
                    center = Offset(entity.position.x, entity.position.y)
                )
                
                // 身体
                drawCircle(
                    color = entity.color,
                    radius = entity.size,
                    center = Offset(entity.position.x, entity.position.y)
                )
                
                // 眼睛
                val eyeOffset = entity.velocity.normalized() * entity.size * 0.4f
                drawCircle(
                    color = Color.White,
                    radius = entity.size * 0.2f,
                    center = Offset(entity.position.x + eyeOffset.x, entity.position.y + eyeOffset.y)
                )
            }
        }
    }
}

private fun DrawScope.drawPhysicsLeaves(leaves: List<PhysicsLeaf>, trailSystem: TrailSystem) {
    for (leaf in leaves) {
        // 绘制轨迹
        val trail = trailSystem.getTrail(leaf)
        if (trail.size > 1) {
            for (i in 1 until trail.size) {
                val alpha = (trail[i].alpha * leaf.life * 0.3f).coerceIn(0f, 1f)
                drawLine(
                    color = leaf.color.copy(alpha = alpha),
                    start = Offset(trail[i - 1].position.x, trail[i - 1].position.y),
                    end = Offset(trail[i].position.x, trail[i].position.y),
                    strokeWidth = trail[i].size,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // 绘制树叶
        rotate(leaf.rotation, pivot = Offset(leaf.body.position.x, leaf.body.position.y)) {
            val alpha = leaf.life.coerceIn(0f, 1f)
            
            // 阴影
            drawLeafShape(
                leaf.body.position.x + 2f,
                leaf.body.position.y + 2f,
                leaf.size,
                Color.Black.copy(alpha = 0.15f * alpha)
            )
            
            // 主体
            drawLeafShape(
                leaf.body.position.x,
                leaf.body.position.y,
                leaf.size,
                leaf.color.copy(alpha = alpha)
            )
            
            // 高光
            drawLeafShape(
                leaf.body.position.x - leaf.size * 0.1f,
                leaf.body.position.y - leaf.size * 0.1f,
                leaf.size * 0.4f,
                Color.White.copy(alpha = alpha * 0.25f)
            )
            
            // 叶脉
            drawLine(
                color = leaf.color.copy(alpha = alpha * 0.4f),
                start = Offset(leaf.body.position.x, leaf.body.position.y - leaf.size * 0.35f),
                end = Offset(leaf.body.position.x, leaf.body.position.y + leaf.size * 0.35f),
                strokeWidth = 1f
            )
        }
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

private fun DrawScope.drawFireflies(fireflies: List<Firefly>, time: Float) {
    for (firefly in fireflies) {
        val glow = (sin(firefly.glowPhase) + 1f) / 2f
        val alpha = (0.4f + glow * 0.6f).coerceIn(0f, 1f)
        val currentSize = firefly.size * (0.8f + glow * 0.4f)
        val center = Offset(firefly.position.x, firefly.position.y)
        
        // 简化光晕 - 合并为两层以提升性能
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    firefly.color.copy(alpha = alpha * 0.3f),
                    firefly.color.copy(alpha = alpha * 0.08f),
                    Color.Transparent
                )
            ),
            radius = currentSize * 6f,
            center = center
        )
        
        // 核心发光
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha),
                    firefly.color.copy(alpha = alpha * 0.4f),
                    Color.Transparent
                )
            ),
            radius = currentSize * 2f,
            center = center
        )
    }
}

private fun DrawScope.drawForceFieldEffects(forceFields: List<TouchForceField>) {
    for (field in forceFields) {
        val alpha = (field.life * 0.5f).coerceIn(0f, 1f)
        
        when (field) {
            is TouchForceField.Attractor -> {
                // 吸引力场 - 向内的圆环
                for (i in 0 until 3) {
                    val ringRadius = field.radius * (1f - field.life + i * 0.2f)
                    drawCircle(
                        color = Color(0xFF74C69D).copy(alpha = alpha * (1f - i * 0.3f)),
                        radius = ringRadius,
                        center = Offset(field.position.x, field.position.y),
                        style = Stroke(width = 2f)
                    )
                }
            }
            is TouchForceField.Repeller -> {
                // 排斥力场 - 向外的圆环
                for (i in 0 until 3) {
                    val ringRadius = field.radius * field.life * (0.3f + i * 0.25f)
                    drawCircle(
                        color = Color(0xFFFF6B6B).copy(alpha = alpha * (1f - i * 0.3f)),
                        radius = ringRadius,
                        center = Offset(field.position.x, field.position.y),
                        style = Stroke(width = 2f)
                    )
                }
            }
            is TouchForceField.Vortex -> {
                // 漩涡力场 - 螺旋
                val spiralPath = Path().apply {
                    var angle = 0f
                    var radius = 10f
                    moveTo(
                        field.position.x + cos(angle) * radius,
                        field.position.y + sin(angle) * radius
                    )
                    while (radius < field.radius * field.life) {
                        angle += if (field.clockwise) 0.3f else -0.3f
                        radius += 3f
                        lineTo(
                            field.position.x + cos(angle) * radius,
                            field.position.y + sin(angle) * radius
                        )
                    }
                }
                drawPath(
                    path = spiralPath,
                    color = Color(0xFF9370DB).copy(alpha = alpha),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }
            is TouchForceField.Explosion -> {
                // 爆炸力场 - 扩散环
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFFD700).copy(alpha = alpha * 0.5f),
                            Color(0xFFFF6B6B).copy(alpha = alpha * 0.3f),
                            Color.Transparent
                        )
                    ),
                    radius = field.currentRadius + 30f,
                    center = Offset(field.position.x, field.position.y)
                )
                
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = alpha),
                    radius = field.currentRadius,
                    center = Offset(field.position.x, field.position.y),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

private fun DrawScope.drawRipples(rippleSystem: RippleSystem) {
    for (ripple in rippleSystem.ripples) {
        val alpha = ripple.alpha.coerceIn(0f, 1f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    ripple.color.copy(alpha = alpha * 0.4f),
                    ripple.color.copy(alpha = alpha * 0.2f),
                    Color.Transparent
                )
            ),
            radius = ripple.radius,
            center = Offset(ripple.center.x, ripple.center.y)
        )
        
        drawCircle(
            color = ripple.color.copy(alpha = alpha * 0.6f),
            radius = ripple.radius,
            center = Offset(ripple.center.x, ripple.center.y),
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawForegroundFog(width: Float, height: Float, time: Float) {
    val fogY = height * 0.85f + sin(time * 0.3f) * 20f
    
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF1A3D25).copy(alpha = 0.25f),
                Color(0xFF0F2818).copy(alpha = 0.4f)
            ),
            startY = fogY - 100,
            endY = height
        )
    )
}
