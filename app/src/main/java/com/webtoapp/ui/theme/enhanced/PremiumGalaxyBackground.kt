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
 * 专业级星系物理主题
 * 
 * 特性：
 * - N体引力模拟
 * - 真实轨道力学
 * - 星云粒子系统
 * - 黑洞引力场
 * - 流星雨物理
 * - 触摸产生引力波
 * - 传感器驱动的视角变化
 * ============================================================
 */

@Composable
fun GalaxyPhysicsBackground(
    modifier: Modifier = Modifier,
    theme: AppTheme = LocalAppTheme.current,
    onInteraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var lastFrameTime by remember { mutableLongStateOf(System.nanoTime()) }

    // 物理系统
    val galaxyWorld = remember { mutableStateOf<GalaxyPhysicsWorld?>(null) }
    
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
                        haptic.performHaptic(HapticType.EXPLOSION, shakeAccum)
                        galaxyWorld.value?.triggerSupernova(shakeAccum)
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
        if (canvasSize.width > 0 && galaxyWorld.value == null) {
            galaxyWorld.value = GalaxyPhysicsWorld(
                bounds = PhysicsBounds(0f, canvasSize.width, 0f, canvasSize.height)
            ).apply {
                initialize()
            }
        }
    }

    // 主物理循环
    LaunchedEffect(galaxyWorld.value) {
        galaxyWorld.value?.let { world ->
            while (true) {
                val currentTime = System.nanoTime()
                val dt = ((currentTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrameTime = currentTime
                
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
                            radius = 300f,
                            strength = 500f
                        ))
                        galaxyWorld.value?.spawnStardustAt(pos, 20)
                        galaxyWorld.value?.rippleSystem?.addRipple(pos, 200f, Color(0xFF9370DB).copy(alpha = 0.4f), 3)
                    }
                    EnhancedTouchEvent.TouchType.LONG_PRESS -> {
                        haptic.performHaptic(HapticType.LONG_PRESS)
                        touchManager.forceFields.add(TouchForceField.Attractor(
                            position = pos,
                            radius = 350f,
                            strength = 800f,
                            life = 4f,
                            decay = 0.006f
                        ))
                        galaxyWorld.value?.createTemporaryBlackHole(pos)
                    }
                    EnhancedTouchEvent.TouchType.DRAG -> {
                        dragPosition?.let { oldPos ->
                            val velocity = pos - oldPos
                            if (velocity.length() > 5f) {
                                galaxyWorld.value?.spawnMeteorAt(oldPos, velocity.normalized() * 300f)
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

        galaxyWorld.value?.let { world ->
            // 绘制深空背景
            drawDeepSpaceBackground(width, height)
            
            // 绘制星云
            drawNebulae(world.nebulae, world.time, sensorData.tilt)
            
            // 绘制远景星星（视差）
            drawParallaxStars(world.backgroundStars, sensorData.tilt, world.time)
            
            // 绘制星系旋臂
            drawGalaxyArms(width, height, world.time, sensorData.tilt)
            
            // 绘制轨道星体
            drawOrbitalBodies(world.orbitalBodies, world.trailSystem)
            
            // 绘制星尘
            drawStardust(world.stardust, world.time)
            
            // 绘制流星
            drawMeteors(world.meteors)
            
            // 绘制黑洞效果
            drawBlackHoles(world.blackHoles, world.time)
            
            // 绘制触摸力场效果
            drawGalaxyForceFieldEffects(touchManager.forceFields)
            
            // 绘制引力波涟漪
            drawGravityWaves(world.rippleSystem)
            
            // 绘制超新星效果
            drawSupernovaEffects(world.supernovaEffects)
        }
    }
}


/**
 * 星系物理世界
 */
class GalaxyPhysicsWorld(val bounds: PhysicsBounds) {
    var time = 0f
    val center = Vec2(bounds.maxX / 2, bounds.maxY / 2)
    
    // 物理实体
    val orbitalBodies = mutableListOf<OrbitalBody>()
    val stardust = mutableListOf<Stardust>()
    val meteors = mutableListOf<Meteor>()
    val blackHoles = mutableListOf<BlackHole>()
    val backgroundStars = mutableListOf<BackgroundStar>()
    val nebulae = mutableListOf<Nebula>()
    
    // 视觉效果
    val trailSystem = TrailSystem(maxPoints = 20)
    val rippleSystem = RippleSystem()
    val supernovaEffects = mutableListOf<SupernovaEffect>()
    
    // 中心黑洞
    val centralBlackHole = BlackHole(
        position = center,
        mass = 5000f,
        radius = 30f,
        isPermanent = true
    )

    fun initialize() {
        // Initialize轨道星体
        repeat(40) {
            orbitalBodies.add(createOrbitalBody())
        }
        
        // Initialize星尘
        repeat(100) {
            stardust.add(createStardust())
        }
        
        // Initialize背景星星
        repeat(200) {
            backgroundStars.add(createBackgroundStar())
        }
        
        // Initialize星云
        repeat(5) {
            nebulae.add(createNebula())
        }
        
        // 添加中心黑洞
        blackHoles.add(centralBlackHole)
    }
    
    fun update(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        time += dt
        
        // Update轨道星体（N体引力）
        updateOrbitalBodies(dt, sensorData, touchManager)
        
        // Update星尘
        updateStardust(dt, sensorData, touchManager)
        
        // Update流星
        updateMeteors(dt)
        
        // Update黑洞
        updateBlackHoles(dt)
        
        // Update视觉效果
        trailSystem.update(0.06f)
        rippleSystem.update(dt)
        updateSupernovaEffects(dt)
        
        // 补充星尘
        if (stardust.size < 80 && Random.nextFloat() < 0.1f) {
            stardust.add(createStardust())
        }
    }
    
    private fun updateOrbitalBodies(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        for (body in orbitalBodies) {
            // 中心黑洞引力
            val toCenter = centralBlackHole.position - body.position
            val distToCenter = toCenter.length()
            if (distToCenter > centralBlackHole.radius * 2) {
                val gravityStrength = centralBlackHole.mass / (distToCenter * distToCenter) * 50f
                body.velocity = body.velocity + toCenter.normalized() * gravityStrength * dt
            }
            
            // 其他黑洞引力
            for (bh in blackHoles) {
                if (bh === centralBlackHole) continue
                val toBH = bh.position - body.position
                val distToBH = toBH.length()
                if (distToBH > bh.radius && distToBH < 300f) {
                    val bhGravity = bh.mass / (distToBH * distToBH) * 30f
                    body.velocity = body.velocity + toBH.normalized() * bhGravity * dt
                }
            }
            
            // 触摸力场
            val touchForce = touchManager.getTotalForceAt(body.position)
            body.velocity = body.velocity + touchForce * dt * 0.3f
            
            // 传感器影响
            body.velocity = body.velocity + Vec2(sensorData.tilt.x * 20f, sensorData.tilt.y * 10f) * dt
            
            // 速度限制
            val speed = body.velocity.length()
            if (speed > body.maxSpeed) {
                body.velocity = body.velocity.normalized() * body.maxSpeed
            }
            
            // Update位置
            body.position = body.position + body.velocity * dt
            
            // 添加轨迹
            if (speed > 30f) {
                trailSystem.addPoint(body, body.position, body.size * 0.3f)
            }
            
            // 边界处理（环绕）
            if (body.position.x < -50) body.position = Vec2(bounds.maxX + 50, body.position.y)
            if (body.position.x > bounds.maxX + 50) body.position = Vec2(-50f, body.position.y)
            if (body.position.y < -50) body.position = Vec2(body.position.x, bounds.maxY + 50)
            if (body.position.y > bounds.maxY + 50) body.position = Vec2(body.position.x, -50f)
        }
    }

    private fun updateStardust(dt: Float, sensorData: SensorData, touchManager: TouchInteractionManager) {
        stardust.forEach { dust ->
            // 中心引力（弱）
            val toCenter = center - dust.position
            val distToCenter = toCenter.length()
            if (distToCenter > 50f) {
                dust.velocity = dust.velocity + toCenter.normalized() * 5f * dt
            }
            
            // 触摸力场
            val touchForce = touchManager.getTotalForceAt(dust.position)
            dust.velocity = dust.velocity + touchForce * dt * 0.2f
            
            // 传感器
            dust.velocity = dust.velocity + Vec2(sensorData.tilt.x * 10f, sensorData.tilt.y * 5f) * dt
            
            // 阻尼
            dust.velocity = dust.velocity * 0.99f
            
            // Update位置
            dust.position = dust.position + dust.velocity * dt
            
            // 发光相位
            dust.glowPhase += dust.glowSpeed * dt
            
            // 生命周期
            dust.life -= dt * 0.02f
        }
        
        // 移除死亡星尘
        stardust.removeAll { it.life <= 0 }
    }
    
    private fun updateMeteors(dt: Float) {
        meteors.forEach { meteor ->
            meteor.position = meteor.position + meteor.velocity * dt
            meteor.trail.add(meteor.position)
            if (meteor.trail.size > 15) meteor.trail.removeAt(0)
            meteor.life -= dt * 0.5f
        }
        
        meteors.removeAll { it.life <= 0 || 
            it.position.x < -100 || it.position.x > bounds.maxX + 100 ||
            it.position.y < -100 || it.position.y > bounds.maxY + 100 }
    }
    
    private fun updateBlackHoles(dt: Float) {
        blackHoles.forEach { bh ->
            if (!bh.isPermanent) {
                bh.life -= dt * 0.1f
            }
            bh.rotationPhase += dt * 2f
        }
        
        blackHoles.removeAll { !it.isPermanent && it.life <= 0 }
    }
    
    private fun updateSupernovaEffects(dt: Float) {
        supernovaEffects.forEach { effect ->
            effect.radius += effect.expansionSpeed * dt
            effect.alpha -= dt * 0.3f
        }
        
        supernovaEffects.removeAll { it.alpha <= 0 }
    }
    
    fun spawnStardustAt(position: Vec2, count: Int) {
        repeat(count) {
            val angle = Random.nextFloat() * 2 * PI.toFloat()
            val speed = Random.nextFloat() * 100f + 50f
            
            stardust.add(Stardust(
                position = position + Vec2.fromAngle(angle, Random.nextFloat() * 20f),
                velocity = Vec2.fromAngle(angle, speed),
                size = Random.nextFloat() * 3f + 1f,
                color = stardustColors.random(),
                glowPhase = Random.nextFloat() * 2 * PI.toFloat(),
                glowSpeed = Random.nextFloat() * 3f + 2f,
                life = 1f
            ))
        }
        
        while (stardust.size > 150) {
            stardust.removeAt(0)
        }
    }
    
    fun spawnMeteorAt(position: Vec2, velocity: Vec2) {
        meteors.add(Meteor(
            position = position,
            velocity = velocity,
            size = Random.nextFloat() * 4f + 2f,
            color = meteorColors.random(),
            life = 1f
        ))
        
        while (meteors.size > 10) {
            meteors.removeAt(0)
        }
    }

    fun createTemporaryBlackHole(position: Vec2) {
        blackHoles.add(BlackHole(
            position = position,
            mass = 2000f,
            radius = 20f,
            isPermanent = false,
            life = 1f
        ))
        
        while (blackHoles.size > 4) {
            val toRemove = blackHoles.find { !it.isPermanent }
            toRemove?.let { blackHoles.remove(it) }
        }
    }
    
    fun triggerSupernova(intensity: Float) {
        val pos = Vec2(
            Random.nextFloat() * bounds.maxX,
            Random.nextFloat() * bounds.maxY
        )
        
        supernovaEffects.add(SupernovaEffect(
            position = pos,
            radius = 0f,
            maxRadius = 200f + intensity * 100f,
            expansionSpeed = 300f + intensity * 200f,
            alpha = 1f,
            color = supernovaColors.random()
        ))
        
        // 产生星尘
        spawnStardustAt(pos, (intensity * 30).toInt())
        
        // 产生引力波
        rippleSystem.addRipple(pos, 300f, Color(0xFFFFD700).copy(alpha = 0.3f), 4)
    }
    
    private fun createOrbitalBody(): OrbitalBody {
        val angle = Random.nextFloat() * 2 * PI.toFloat()
        val distance = Random.nextFloat() * min(bounds.maxX, bounds.maxY) * 0.4f + 80f
        val position = center + Vec2.fromAngle(angle, distance)
        
        // 轨道速度（垂直于半径方向）
        val orbitalSpeed = sqrt(centralBlackHole.mass / distance) * 3f
        val velocityAngle = angle + PI.toFloat() / 2
        
        return OrbitalBody(
            position = position,
            velocity = Vec2.fromAngle(velocityAngle, orbitalSpeed),
            size = Random.nextFloat() * 6f + 3f,
            color = starColors.random(),
            maxSpeed = orbitalSpeed * 2f
        )
    }
    
    private fun createStardust(): Stardust {
        val angle = Random.nextFloat() * 2 * PI.toFloat()
        val distance = Random.nextFloat() * min(bounds.maxX, bounds.maxY) * 0.45f + 50f
        
        return Stardust(
            position = center + Vec2.fromAngle(angle, distance),
            velocity = Vec2.fromAngle(angle + PI.toFloat() / 2, Random.nextFloat() * 20f + 10f),
            size = Random.nextFloat() * 2f + 0.5f,
            color = stardustColors.random(),
            glowPhase = Random.nextFloat() * 2 * PI.toFloat(),
            glowSpeed = Random.nextFloat() * 2f + 1f,
            life = 1f
        )
    }
    
    private fun createBackgroundStar(): BackgroundStar {
        return BackgroundStar(
            x = Random.nextFloat() * bounds.maxX,
            y = Random.nextFloat() * bounds.maxY,
            size = Random.nextFloat() * 2f + 0.5f,
            brightness = Random.nextFloat() * 0.5f + 0.3f,
            twinklePhase = Random.nextFloat() * 2 * PI.toFloat(),
            twinkleSpeed = Random.nextFloat() * 2f + 1f,
            parallaxFactor = Random.nextFloat() * 0.3f + 0.1f
        )
    }
    
    private fun createNebula(): Nebula {
        return Nebula(
            position = Vec2(
                Random.nextFloat() * bounds.maxX,
                Random.nextFloat() * bounds.maxY
            ),
            size = Random.nextFloat() * 200f + 100f,
            color = nebulaColors.random(),
            alpha = Random.nextFloat() * 0.15f + 0.05f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * 0.1f
        )
    }

    companion object {
        val starColors = listOf(
            Color(0xFFFFFFFF), Color(0xFFFFE4B5), Color(0xFFADD8E6),
            Color(0xFFFFB6C1), Color(0xFFE6E6FA), Color(0xFFFFFACD)
        )
        
        val stardustColors = listOf(
            Color(0xFF9370DB), Color(0xFF8A2BE2), Color(0xFF9400D3),
            Color(0xFFBA55D3), Color(0xFFDA70D6), Color(0xFFEE82EE)
        )
        
        val meteorColors = listOf(
            Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF6347),
            Color(0xFFFF4500), Color(0xFFFFFFFF)
        )
        
        val nebulaColors = listOf(
            Color(0xFF9370DB), Color(0xFF8B008B), Color(0xFF4B0082),
            Color(0xFF483D8B), Color(0xFF6A5ACD), Color(0xFF7B68EE)
        )
        
        val supernovaColors = listOf(
            Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF6347),
            Color(0xFFFFFFFF), Color(0xFF00FFFF)
        )
    }
}

// 数据类
data class OrbitalBody(
    var position: Vec2,
    var velocity: Vec2,
    var size: Float,
    var color: Color,
    var maxSpeed: Float
)

data class Stardust(
    var position: Vec2,
    var velocity: Vec2,
    var size: Float,
    var color: Color,
    var glowPhase: Float,
    var glowSpeed: Float,
    var life: Float
)

data class Meteor(
    var position: Vec2,
    var velocity: Vec2,
    var size: Float,
    var color: Color,
    var life: Float,
    val trail: MutableList<Vec2> = mutableListOf()
)

data class BlackHole(
    var position: Vec2,
    var mass: Float,
    var radius: Float,
    var isPermanent: Boolean,
    var life: Float = 1f,
    var rotationPhase: Float = 0f
)

data class BackgroundStar(
    var x: Float,
    var y: Float,
    var size: Float,
    var brightness: Float,
    var twinklePhase: Float,
    var twinkleSpeed: Float,
    var parallaxFactor: Float
)

data class Nebula(
    var position: Vec2,
    var size: Float,
    var color: Color,
    var alpha: Float,
    var rotationSpeed: Float,
    var rotation: Float = 0f
)

data class SupernovaEffect(
    var position: Vec2,
    var radius: Float,
    var maxRadius: Float,
    var expansionSpeed: Float,
    var alpha: Float,
    var color: Color
)


// ==================== 绘制函数 ====================

private fun DrawScope.drawDeepSpaceBackground(width: Float, height: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0D0015),
                Color(0xFF050008),
                Color(0xFF000000)
            ),
            center = Offset(width / 2, height / 2),
            radius = max(width, height) * 0.8f
        )
    )
}

private fun DrawScope.drawNebulae(nebulae: List<Nebula>, time: Float, tilt: Vec2) {
    for (nebula in nebulae) {
        val offsetX = tilt.x * nebula.size * 0.1f
        val offsetY = tilt.y * nebula.size * 0.1f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    nebula.color.copy(alpha = nebula.alpha),
                    nebula.color.copy(alpha = nebula.alpha * 0.5f),
                    nebula.color.copy(alpha = nebula.alpha * 0.2f),
                    Color.Transparent
                )
            ),
            radius = nebula.size,
            center = Offset(nebula.position.x + offsetX, nebula.position.y + offsetY)
        )
    }
}

private fun DrawScope.drawParallaxStars(stars: List<BackgroundStar>, tilt: Vec2, time: Float) {
    for (star in stars) {
        val offsetX = tilt.x * star.parallaxFactor * 30f
        val offsetY = tilt.y * star.parallaxFactor * 30f
        val twinkle = (sin(star.twinklePhase + time * star.twinkleSpeed) + 1f) / 2f
        val alpha = (star.brightness * (0.5f + twinkle * 0.5f)).coerceIn(0f, 1f)
        
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = star.size,
            center = Offset(star.x + offsetX, star.y + offsetY)
        )
        
        // 大星星有光晕
        if (star.size > 1.5f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha * 0.3f),
                        Color.Transparent
                    )
                ),
                radius = star.size * 4f,
                center = Offset(star.x + offsetX, star.y + offsetY)
            )
        }
    }
}

private fun DrawScope.drawGalaxyArms(width: Float, height: Float, time: Float, tilt: Vec2) {
    val centerX = width / 2 + tilt.x * 20f
    val centerY = height / 2 + tilt.y * 20f
    val armCount = 3
    
    for (arm in 0 until armCount) {
        val baseAngle = arm * 2 * PI.toFloat() / armCount + time * 0.05f
        
        val armPath = Path()
        var isFirst = true
        
        for (i in 0..60) {
            val t = i / 60f
            val angle = baseAngle + t * 2.5f * PI.toFloat()
            val radius = 30f + t * min(width, height) * 0.4f
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius
            
            if (isFirst) {
                armPath.moveTo(x, y)
                isFirst = false
            } else {
                armPath.lineTo(x, y)
            }
        }
        
        drawPath(
            path = armPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF9370DB).copy(alpha = 0.3f),
                    Color(0xFF8A2BE2).copy(alpha = 0.15f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 40f, cap = StrokeCap.Round)
        )
    }
}


private fun DrawScope.drawOrbitalBodies(bodies: List<OrbitalBody>, trailSystem: TrailSystem) {
    for (body in bodies) {
        // 绘制轨迹
        val trail = trailSystem.getTrail(body)
        if (trail.size > 1) {
            for (i in 1 until trail.size) {
                val alpha = (trail[i].alpha * 0.4f).coerceIn(0f, 1f)
                drawLine(
                    color = body.color.copy(alpha = alpha),
                    start = Offset(trail[i - 1].position.x, trail[i - 1].position.y),
                    end = Offset(trail[i].position.x, trail[i].position.y),
                    strokeWidth = trail[i].size,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // 光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    body.color.copy(alpha = 0.4f),
                    body.color.copy(alpha = 0.1f),
                    Color.Transparent
                )
            ),
            radius = body.size * 4f,
            center = Offset(body.position.x, body.position.y)
        )
        
        // 星体
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    body.color,
                    body.color.copy(alpha = 0.8f)
                )
            ),
            radius = body.size,
            center = Offset(body.position.x, body.position.y)
        )
    }
}

private fun DrawScope.drawStardust(stardust: List<Stardust>, time: Float) {
    for (dust in stardust) {
        val glow = (sin(dust.glowPhase) + 1f) / 2f
        val alpha = (dust.life * (0.4f + glow * 0.4f)).coerceIn(0f, 1f)
        
        // 光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    dust.color.copy(alpha = alpha * 0.5f),
                    Color.Transparent
                )
            ),
            radius = dust.size * 5f,
            center = Offset(dust.position.x, dust.position.y)
        )
        
        // 核心
        drawCircle(
            color = dust.color.copy(alpha = alpha),
            radius = dust.size,
            center = Offset(dust.position.x, dust.position.y)
        )
    }
}

private fun DrawScope.drawMeteors(meteors: List<Meteor>) {
    for (meteor in meteors) {
        // 尾迹
        if (meteor.trail.size > 1) {
            for (i in 1 until meteor.trail.size) {
                val progress = i.toFloat() / meteor.trail.size
                val alpha = (progress * meteor.life * 0.6f).coerceIn(0f, 1f)
                val width = meteor.size * progress
                
                drawLine(
                    color = meteor.color.copy(alpha = alpha),
                    start = Offset(meteor.trail[i - 1].x, meteor.trail[i - 1].y),
                    end = Offset(meteor.trail[i].x, meteor.trail[i].y),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // 头部
        val alpha = meteor.life.coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha),
                    meteor.color.copy(alpha = alpha * 0.6f),
                    Color.Transparent
                )
            ),
            radius = meteor.size * 3f,
            center = Offset(meteor.position.x, meteor.position.y)
        )
    }
}


private fun DrawScope.drawBlackHoles(blackHoles: List<BlackHole>, time: Float) {
    for (bh in blackHoles) {
        val alpha = if (bh.isPermanent) 1f else bh.life.coerceIn(0f, 1f)
        
        // 吸积盘
        for (i in 0 until 3) {
            val ringRadius = bh.radius * (2f + i * 0.8f)
            val ringAlpha = (alpha * (0.3f - i * 0.08f)).coerceIn(0f, 1f)
            
            rotate(degrees = bh.rotationPhase * (30f - i * 5f), pivot = Offset(bh.position.x, bh.position.y)) {
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = ringAlpha),
                            Color(0xFFFFA500).copy(alpha = ringAlpha * 0.5f),
                            Color(0xFFFF4500).copy(alpha = ringAlpha * 0.3f),
                            Color(0xFFFFD700).copy(alpha = ringAlpha)
                        ),
                        center = Offset(bh.position.x, bh.position.y)
                    ),
                    topLeft = Offset(bh.position.x - ringRadius, bh.position.y - ringRadius * 0.3f),
                    size = Size(ringRadius * 2, ringRadius * 0.6f)
                )
            }
        }
        
        // 事件视界
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black,
                    Color(0xFF1A0033).copy(alpha = alpha),
                    Color(0xFF330066).copy(alpha = alpha * 0.5f)
                )
            ),
            radius = bh.radius,
            center = Offset(bh.position.x, bh.position.y)
        )
        
        // 引力透镜效果
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF9370DB).copy(alpha = alpha * 0.2f),
                    Color.Transparent
                )
            ),
            radius = bh.radius * 3f,
            center = Offset(bh.position.x, bh.position.y)
        )
    }
}

private fun DrawScope.drawGalaxyForceFieldEffects(forceFields: List<TouchForceField>) {
    for (field in forceFields) {
        val alpha = (field.life * 0.5f).coerceIn(0f, 1f)
        
        when (field) {
            is TouchForceField.Explosion -> {
                // 引力波扩散
                for (i in 0 until 3) {
                    val ringRadius = field.currentRadius * (0.7f + i * 0.2f)
                    drawCircle(
                        color = Color(0xFF9370DB).copy(alpha = alpha * (1f - i * 0.3f)),
                        radius = ringRadius,
                        center = Offset(field.position.x, field.position.y),
                        style = Stroke(width = 3f - i)
                    )
                }
            }
            is TouchForceField.Attractor -> {
                // 黑洞吸引效果
                for (i in 0 until 4) {
                    val ringRadius = field.radius * (1f - field.life * 0.3f + i * 0.2f)
                    drawCircle(
                        color = Color(0xFF8A2BE2).copy(alpha = alpha * (1f - i * 0.2f)),
                        radius = ringRadius,
                        center = Offset(field.position.x, field.position.y),
                        style = Stroke(width = 2f)
                    )
                }
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawGravityWaves(rippleSystem: RippleSystem) {
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

private fun DrawScope.drawSupernovaEffects(effects: List<SupernovaEffect>) {
    for (effect in effects) {
        val alpha = effect.alpha.coerceIn(0f, 1f)
        
        // 核心爆发
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha),
                    effect.color.copy(alpha = alpha * 0.6f),
                    effect.color.copy(alpha = alpha * 0.2f),
                    Color.Transparent
                )
            ),
            radius = effect.radius,
            center = Offset(effect.position.x, effect.position.y)
        )
        
        // 冲击波
        drawCircle(
            color = effect.color.copy(alpha = alpha * 0.8f),
            radius = effect.radius,
            center = Offset(effect.position.x, effect.position.y),
            style = Stroke(width = 4f)
        )
    }
}
