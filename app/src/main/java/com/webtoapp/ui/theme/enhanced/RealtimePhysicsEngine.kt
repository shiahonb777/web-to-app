package com.webtoapp.ui.theme.enhanced

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*
import kotlin.random.Random

/**
 * ============================================================
 * 实时物理引擎 - 专业级
 * 
 * 特性：
 * - 真实物理模拟（Verlet积分）
 * - 流体动力学（SPH）
 * - 群体智能（Boids算法）
 * - 软体物理（弹簧-质点系统）
 * - 碰撞检测（空间哈希）
 * - 实时传感器融合
 * - 触摸力场交互
 * ============================================================
 */

// ==================== 核心物理实体 ====================

/**
 * 物理实体 - 使用Verlet积分实现更稳定的物理模拟
 */
class PhysicsBody(
    var position: Vec2,
    var previousPosition: Vec2 = position,
    var acceleration: Vec2 = Vec2.ZERO,
    var mass: Float = 1f,
    var radius: Float = 5f,
    var friction: Float = 0.98f,
    var restitution: Float = 0.7f,
    var isStatic: Boolean = false,
    var userData: Any? = null
) {
    val velocity: Vec2 get() = position - previousPosition
    
    fun applyForce(force: Vec2) {
        if (!isStatic) {
            acceleration = acceleration + force / mass
        }
    }
    
    fun applyImpulse(impulse: Vec2) {
        if (!isStatic) {
            previousPosition = previousPosition - impulse / mass
        }
    }
    
    fun integrate(dt: Float) {
        if (isStatic) return
        
        val velocity = (position - previousPosition) * friction
        previousPosition = position
        position = position + velocity + acceleration * dt * dt
        acceleration = Vec2.ZERO
    }
    
    fun constrainToBounds(bounds: PhysicsBounds) {
        if (isStatic) return
        
        val vel = velocity
        
        if (position.x - radius < bounds.minX) {
            position = Vec2(bounds.minX + radius, position.y)
            previousPosition = Vec2(position.x + vel.x * restitution, previousPosition.y)
        }
        if (position.x + radius > bounds.maxX) {
            position = Vec2(bounds.maxX - radius, position.y)
            previousPosition = Vec2(position.x + vel.x * restitution, previousPosition.y)
        }
        if (position.y - radius < bounds.minY) {
            position = Vec2(position.x, bounds.minY + radius)
            previousPosition = Vec2(previousPosition.x, position.y + vel.y * restitution)
        }
        if (position.y + radius > bounds.maxY) {
            position = Vec2(position.x, bounds.maxY - radius)
            previousPosition = Vec2(previousPosition.x, position.y + vel.y * restitution)
        }
    }
}

data class PhysicsBounds(
    val minX: Float, val maxX: Float,
    val minY: Float, val maxY: Float
)

// ==================== 弹簧约束系统 ====================

/**
 * 弹簧约束 - 用于软体物理、布料模拟等
 */
class SpringConstraint(
    val bodyA: PhysicsBody,
    val bodyB: PhysicsBody,
    val restLength: Float,
    val stiffness: Float = 0.5f,
    val damping: Float = 0.1f
) {
    fun solve() {
        val delta = bodyB.position - bodyA.position
        val distance = delta.length()
        if (distance < 0.0001f) return
        
        val diff = (distance - restLength) / distance
        val correction = delta * diff * 0.5f * stiffness
        
        if (!bodyA.isStatic) {
            bodyA.position = bodyA.position + correction
        }
        if (!bodyB.isStatic) {
            bodyB.position = bodyB.position - correction
        }
    }
}

// ==================== 空间哈希碰撞检测 ====================

/**
 * 空间哈希网格 - 高效碰撞检测
 */
class SpatialHash(private val cellSize: Float) {
    private val cells = mutableMapOf<Long, MutableList<PhysicsBody>>()
    
    private fun hash(x: Int, y: Int): Long = (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)
    
    fun clear() = cells.clear()
    
    fun insert(body: PhysicsBody) {
        val minX = ((body.position.x - body.radius) / cellSize).toInt()
        val maxX = ((body.position.x + body.radius) / cellSize).toInt()
        val minY = ((body.position.y - body.radius) / cellSize).toInt()
        val maxY = ((body.position.y + body.radius) / cellSize).toInt()
        
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val key = hash(x, y)
                cells.getOrPut(key) { mutableListOf() }.add(body)
            }
        }
    }
    
    fun queryNearby(body: PhysicsBody): List<PhysicsBody> {
        val result = mutableSetOf<PhysicsBody>()
        val minX = ((body.position.x - body.radius) / cellSize).toInt()
        val maxX = ((body.position.x + body.radius) / cellSize).toInt()
        val minY = ((body.position.y - body.radius) / cellSize).toInt()
        val maxY = ((body.position.y + body.radius) / cellSize).toInt()
        
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                cells[hash(x, y)]?.let { result.addAll(it) }
            }
        }
        result.remove(body)
        return result.toList()
    }
}

// ==================== Boids群体智能 ====================

/**
 * Boid实体 - 群体智能行为
 */
class Boid(
    var position: Vec2,
    var velocity: Vec2,
    var maxSpeed: Float = 200f,
    var maxForce: Float = 50f,
    var size: Float = 8f,
    var color: Color = Color.White,
    var type: Int = 0
) {
    fun update(dt: Float) {
        position = position + velocity * dt
    }
    
    fun applyForce(force: Vec2) {
        velocity = velocity + force
        val speed = velocity.length()
        if (speed > maxSpeed) {
            velocity = velocity.normalized() * maxSpeed
        }
    }
    
    /**
     * 分离 - 避免拥挤
     */
    fun separate(boids: List<Boid>, desiredSeparation: Float): Vec2 {
        var steer = Vec2.ZERO
        var count = 0
        
        for (other in boids) {
            if (other === this) continue
            val d = (position - other.position).length()
            if (d > 0 && d < desiredSeparation) {
                val diff = (position - other.position).normalized() / d
                steer = steer + diff
                count++
            }
        }
        
        if (count > 0) {
            steer = steer / count.toFloat()
            if (steer.length() > 0) {
                steer = steer.normalized() * maxSpeed - velocity
                if (steer.length() > maxForce) {
                    steer = steer.normalized() * maxForce
                }
            }
        }
        return steer
    }
    
    /**
     * 对齐 - 与邻居保持相同方向
     */
    fun align(boids: List<Boid>, neighborDist: Float): Vec2 {
        var sum = Vec2.ZERO
        var count = 0
        
        for (other in boids) {
            if (other === this) continue
            val d = (position - other.position).length()
            if (d > 0 && d < neighborDist) {
                sum = sum + other.velocity
                count++
            }
        }
        
        if (count > 0) {
            sum = sum / count.toFloat()
            sum = sum.normalized() * maxSpeed
            val steer = sum - velocity
            return if (steer.length() > maxForce) steer.normalized() * maxForce else steer
        }
        return Vec2.ZERO
    }
    
    /**
     * 聚合 - 向邻居中心移动
     */
    fun cohesion(boids: List<Boid>, neighborDist: Float): Vec2 {
        var sum = Vec2.ZERO
        var count = 0
        
        for (other in boids) {
            if (other === this) continue
            val d = (position - other.position).length()
            if (d > 0 && d < neighborDist) {
                sum = sum + other.position
                count++
            }
        }
        
        if (count > 0) {
            sum = sum / count.toFloat()
            return seek(sum)
        }
        return Vec2.ZERO
    }
    
    /**
     * 寻找目标
     */
    fun seek(target: Vec2): Vec2 {
        val desired = (target - position).normalized() * maxSpeed
        val steer = desired - velocity
        return if (steer.length() > maxForce) steer.normalized() * maxForce else steer
    }
    
    /**
     * 逃离目标
     */
    fun flee(target: Vec2, fleeRadius: Float): Vec2 {
        val d = (position - target).length()
        if (d < fleeRadius) {
            val desired = (position - target).normalized() * maxSpeed
            val steer = desired - velocity
            return if (steer.length() > maxForce) steer.normalized() * maxForce else steer
        }
        return Vec2.ZERO
    }
    
    /**
     * 边界约束
     */
    fun constrainToBounds(bounds: PhysicsBounds, margin: Float = 50f): Vec2 {
        var steer = Vec2.ZERO
        
        if (position.x < bounds.minX + margin) {
            steer = steer + Vec2(maxForce, 0f)
        }
        if (position.x > bounds.maxX - margin) {
            steer = steer + Vec2(-maxForce, 0f)
        }
        if (position.y < bounds.minY + margin) {
            steer = steer + Vec2(0f, maxForce)
        }
        if (position.y > bounds.maxY - margin) {
            steer = steer + Vec2(0f, -maxForce)
        }
        
        return steer
    }
}

/**
 * Boids群体系统
 */
class BoidsSystem(
    val separationWeight: Float = 1.5f,
    val alignmentWeight: Float = 1.0f,
    val cohesionWeight: Float = 1.0f,
    val separationDist: Float = 30f,
    val neighborDist: Float = 80f
) {
    val boids = mutableListOf<Boid>()
    
    fun update(dt: Float, bounds: PhysicsBounds, attractors: List<Vec2> = emptyList(), repellers: List<Vec2> = emptyList()) {
        for (boid in boids) {
            // 基础群体行为
            val sep = boid.separate(boids, separationDist) * separationWeight
            val ali = boid.align(boids, neighborDist) * alignmentWeight
            val coh = boid.cohesion(boids, neighborDist) * cohesionWeight
            val boundary = boid.constrainToBounds(bounds)
            
            // 吸引点
            var attraction = Vec2.ZERO
            for (attractor in attractors) {
                attraction = attraction + boid.seek(attractor) * 0.5f
            }
            
            // 排斥点
            var repulsion = Vec2.ZERO
            for (repeller in repellers) {
                repulsion = repulsion + boid.flee(repeller, 150f) * 2f
            }
            
            boid.applyForce(sep + ali + coh + boundary + attraction + repulsion)
            boid.update(dt)
        }
    }
}


// ==================== SPH流体模拟 ====================

/**
 * SPH流体粒子
 */
class FluidParticle(
    var position: Vec2,
    var velocity: Vec2 = Vec2.ZERO,
    var density: Float = 0f,
    var pressure: Float = 0f,
    var mass: Float = 1f,
    var color: Color = Color.Cyan
) {
    var force: Vec2 = Vec2.ZERO
}

/**
 * SPH流体系统 - 简化版光滑粒子流体动力学
 */
class SPHFluidSystem(
    val restDensity: Float = 1000f,
    val gasConstant: Float = 2000f,
    val viscosity: Float = 250f,
    val smoothingRadius: Float = 16f,
    val gravity: Vec2 = Vec2(0f, 500f)
) {
    val particles = mutableListOf<FluidParticle>()
    
    private val poly6Coeff: Float = 315f / (64f * PI.toFloat() * smoothingRadius.pow(9))
    private val spikyGradCoeff: Float = -45f / (PI.toFloat() * smoothingRadius.pow(6))
    private val viscLaplaceCoeff: Float = 45f / (PI.toFloat() * smoothingRadius.pow(6))
    
    fun update(dt: Float, bounds: PhysicsBounds) {
        // 计算密度和压力
        for (p in particles) {
            p.density = 0f
            for (other in particles) {
                val r = (p.position - other.position).length()
                if (r < smoothingRadius) {
                    val q = smoothingRadius * smoothingRadius - r * r
                    p.density += p.mass * poly6Coeff * q * q * q
                }
            }
            p.pressure = gasConstant * (p.density - restDensity)
        }
        
        // 计算力
        for (p in particles) {
            var pressureForce = Vec2.ZERO
            var viscosityForce = Vec2.ZERO
            
            for (other in particles) {
                if (other === p) continue
                
                val diff = p.position - other.position
                val r = diff.length()
                
                if (r > 0 && r < smoothingRadius) {
                    val dir = diff.normalized()
                    
                    // 压力力
                    val pressureMag = -other.mass * (p.pressure + other.pressure) / (2f * other.density) *
                            spikyGradCoeff * (smoothingRadius - r).pow(2)
                    pressureForce = pressureForce + dir * pressureMag
                    
                    // 粘性力
                    val viscMag = viscosity * other.mass * (other.velocity - p.velocity).length() / other.density *
                            viscLaplaceCoeff * (smoothingRadius - r)
                    viscosityForce = viscosityForce + (other.velocity - p.velocity).normalized() * viscMag
                }
            }
            
            p.force = pressureForce + viscosityForce + gravity * p.density
        }
        
        // 积分
        for (p in particles) {
            if (p.density > 0) {
                p.velocity = p.velocity + p.force / p.density * dt
                p.position = p.position + p.velocity * dt
            }
            
            // 边界约束
            val damping = 0.3f
            if (p.position.x < bounds.minX) {
                p.position = Vec2(bounds.minX, p.position.y)
                p.velocity = Vec2(-p.velocity.x * damping, p.velocity.y)
            }
            if (p.position.x > bounds.maxX) {
                p.position = Vec2(bounds.maxX, p.position.y)
                p.velocity = Vec2(-p.velocity.x * damping, p.velocity.y)
            }
            if (p.position.y < bounds.minY) {
                p.position = Vec2(p.position.x, bounds.minY)
                p.velocity = Vec2(p.velocity.x, -p.velocity.y * damping)
            }
            if (p.position.y > bounds.maxY) {
                p.position = Vec2(p.position.x, bounds.maxY)
                p.velocity = Vec2(p.velocity.x, -p.velocity.y * damping)
            }
        }
    }
    
    fun applyForceAt(position: Vec2, force: Vec2, radius: Float) {
        for (p in particles) {
            val d = (p.position - position).length()
            if (d < radius) {
                val strength = 1f - d / radius
                p.velocity = p.velocity + force * strength
            }
        }
    }
}

// ==================== 实时传感器管理器 ====================

/**
 * 传感器数据
 */
data class SensorData(
    val gravity: Vec2 = Vec2.ZERO,
    val linearAcceleration: Vec2 = Vec2.ZERO,
    val rotation: Vec2 = Vec2.ZERO,
    val shake: Float = 0f,
    val tilt: Vec2 = Vec2.ZERO
)

/**
 * 实时传感器管理器
 */
class RealtimeSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    
    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData
    
    // 低通滤波器参数
    private val alpha = 0.15f
    private var filteredGravity = Vec2.ZERO
    private var filteredAccel = Vec2.ZERO
    private var filteredRotation = Vec2.ZERO
    
    // 摇晃检测
    private var lastAccelMagnitude = 0f
    private var shakeAccumulator = 0f
    
    fun start() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gravity?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        linearAccel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }
    
    fun stop() {
        sensorManager.unregisterListener(this)
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val rawAccel = Vec2(event.values[0], event.values[1])
                filteredAccel = filteredAccel * (1 - alpha) + rawAccel * alpha
                
                // 摇晃检测
                val magnitude = rawAccel.length()
                val delta = abs(magnitude - lastAccelMagnitude)
                lastAccelMagnitude = magnitude
                
                if (delta > 3f) {
                    shakeAccumulator = (shakeAccumulator + delta * 0.3f).coerceAtMost(1f)
                } else {
                    shakeAccumulator = (shakeAccumulator - 0.05f).coerceAtLeast(0f)
                }
                
                // 倾斜计算
                val tilt = Vec2(
                    (event.values[0] / 9.81f).coerceIn(-1f, 1f),
                    (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                )
                
                _sensorData.value = _sensorData.value.copy(
                    linearAcceleration = filteredAccel,
                    shake = shakeAccumulator,
                    tilt = tilt
                )
            }
            Sensor.TYPE_GRAVITY -> {
                val rawGravity = Vec2(event.values[0], event.values[1])
                filteredGravity = filteredGravity * (1 - alpha) + rawGravity * alpha
                _sensorData.value = _sensorData.value.copy(gravity = filteredGravity)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val rawRotation = Vec2(event.values[0], event.values[1])
                filteredRotation = filteredRotation * (1 - alpha) + rawRotation * alpha
                _sensorData.value = _sensorData.value.copy(rotation = filteredRotation)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val accel = Vec2(event.values[0], event.values[1])
                _sensorData.value = _sensorData.value.copy(linearAcceleration = accel)
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

// ==================== 触摸交互系统 ====================

/**
 * 触摸点数据
 */
data class TouchPoint(
    val id: Int,
    var position: Vec2,
    var previousPosition: Vec2,
    var startPosition: Vec2,
    var startTime: Long,
    var isActive: Boolean = true
) {
    val velocity: Vec2 get() = position - previousPosition
    val dragDistance: Float get() = (position - startPosition).length()
    val duration: Long get() = System.currentTimeMillis() - startTime
}

/**
 * 触摸力场
 */
sealed class TouchForceField {
    abstract val position: Vec2
    abstract val radius: Float
    abstract val strength: Float
    abstract val life: Float
    abstract fun update(dt: Float): TouchForceField?
    abstract fun getForceAt(point: Vec2): Vec2
    
    data class Attractor(
        override val position: Vec2,
        override val radius: Float,
        override val strength: Float,
        override var life: Float = 1f,
        val decay: Float = 0.02f
    ) : TouchForceField() {
        override fun update(dt: Float): TouchForceField? {
            val newLife = life - decay
            return if (newLife > 0) copy(life = newLife) else null
        }
        
        override fun getForceAt(point: Vec2): Vec2 {
            val diff = position - point
            val dist = diff.length()
            if (dist < radius && dist > 1f) {
                val force = strength * life * (1f - dist / radius)
                return diff.normalized() * force
            }
            return Vec2.ZERO
        }
    }
    
    data class Repeller(
        override val position: Vec2,
        override val radius: Float,
        override val strength: Float,
        override var life: Float = 1f,
        val decay: Float = 0.02f
    ) : TouchForceField() {
        override fun update(dt: Float): TouchForceField? {
            val newLife = life - decay
            return if (newLife > 0) copy(life = newLife) else null
        }
        
        override fun getForceAt(point: Vec2): Vec2 {
            val diff = point - position
            val dist = diff.length()
            if (dist < radius && dist > 1f) {
                val force = strength * life * (1f - dist / radius)
                return diff.normalized() * force
            }
            return Vec2.ZERO
        }
    }
    
    data class Vortex(
        override val position: Vec2,
        override val radius: Float,
        override val strength: Float,
        override var life: Float = 1f,
        val decay: Float = 0.015f,
        val clockwise: Boolean = true
    ) : TouchForceField() {
        override fun update(dt: Float): TouchForceField? {
            val newLife = life - decay
            return if (newLife > 0) copy(life = newLife) else null
        }
        
        override fun getForceAt(point: Vec2): Vec2 {
            val diff = point - position
            val dist = diff.length()
            if (dist < radius && dist > 1f) {
                val tangent = if (clockwise) Vec2(-diff.y, diff.x) else Vec2(diff.y, -diff.x)
                val force = strength * life * (1f - dist / radius)
                return tangent.normalized() * force
            }
            return Vec2.ZERO
        }
    }
    
    data class Explosion(
        override val position: Vec2,
        override val radius: Float,
        override val strength: Float,
        override var life: Float = 1f,
        val decay: Float = 0.05f,
        var currentRadius: Float = 0f,
        val expansionSpeed: Float = 500f
    ) : TouchForceField() {
        override fun update(dt: Float): TouchForceField? {
            val newLife = life - decay
            val newRadius = currentRadius + expansionSpeed * dt
            return if (newLife > 0 && newRadius < radius) {
                copy(life = newLife, currentRadius = newRadius)
            } else null
        }
        
        override fun getForceAt(point: Vec2): Vec2 {
            val diff = point - position
            val dist = diff.length()
            val ringWidth = 50f
            if (dist > currentRadius - ringWidth && dist < currentRadius + ringWidth && dist > 1f) {
                val force = strength * life
                return diff.normalized() * force
            }
            return Vec2.ZERO
        }
    }
}

/**
 * 触摸交互管理器
 */
class TouchInteractionManager {
    private val touchPoints = mutableMapOf<Int, TouchPoint>()
    val forceFields = mutableListOf<TouchForceField>()
    
    fun onTouchDown(id: Int, position: Vec2) {
        touchPoints[id] = TouchPoint(
            id = id,
            position = position,
            previousPosition = position,
            startPosition = position,
            startTime = System.currentTimeMillis()
        )
    }
    
    fun onTouchMove(id: Int, position: Vec2) {
        touchPoints[id]?.let {
            it.previousPosition = it.position
            it.position = position
        }
    }
    
    fun onTouchUp(id: Int, position: Vec2): TouchPoint? {
        val point = touchPoints.remove(id)
        point?.let {
            it.isActive = false
            it.position = position
        }
        return point
    }
    
    fun createForceFieldFromGesture(touchPoint: TouchPoint, type: GestureType) {
        val velocity = touchPoint.velocity
        val speed = velocity.length()
        
        when (type) {
            GestureType.TAP -> {
                forceFields.add(TouchForceField.Explosion(
                    position = touchPoint.position,
                    radius = 200f,
                    strength = 300f
                ))
            }
            GestureType.LONG_PRESS -> {
                forceFields.add(TouchForceField.Attractor(
                    position = touchPoint.position,
                    radius = 250f,
                    strength = 400f,
                    life = 2f,
                    decay = 0.01f
                ))
            }
            GestureType.SWIPE -> {
                val direction = velocity.normalized()
                forceFields.add(TouchForceField.Repeller(
                    position = touchPoint.position,
                    radius = 150f + speed * 0.5f,
                    strength = 200f + speed * 2f
                ))
            }
            GestureType.DRAG -> {
                forceFields.add(TouchForceField.Vortex(
                    position = touchPoint.position,
                    radius = 180f,
                    strength = 250f,
                    clockwise = velocity.x > 0
                ))
            }
        }
    }
    
    fun update(dt: Float) {
        forceFields.replaceAll { it.update(dt) ?: it }
        forceFields.removeAll { it.life <= 0 }
    }
    
    fun getActivePoints(): List<TouchPoint> = touchPoints.values.filter { it.isActive }
    
    fun getTotalForceAt(point: Vec2): Vec2 {
        var force = Vec2.ZERO
        for (field in forceFields) {
            force = force + field.getForceAt(point)
        }
        return force
    }
}

enum class GestureType {
    TAP, LONG_PRESS, SWIPE, DRAG
}


// ==================== 高级视觉效果 ====================

/**
 * 轨迹系统 - 用于绘制粒子拖尾
 */
class TrailSystem(val maxPoints: Int = 20) {
    private val trails = mutableMapOf<Any, MutableList<TrailPoint>>()
    
    data class TrailPoint(
        val position: Vec2,
        val time: Long,
        val alpha: Float = 1f,
        val size: Float = 1f
    )
    
    fun addPoint(id: Any, position: Vec2, size: Float = 1f) {
        val trail = trails.getOrPut(id) { mutableListOf() }
        trail.add(TrailPoint(position, System.currentTimeMillis(), 1f, size))
        if (trail.size > maxPoints) {
            trail.removeAt(0)
        }
    }
    
    fun getTrail(id: Any): List<TrailPoint> = trails[id] ?: emptyList()
    
    fun update(fadeSpeed: Float = 0.05f) {
        val toRemove = mutableListOf<Any>()
        for ((id, trail) in trails) {
            trail.forEachIndexed { index, point ->
                val newAlpha = point.alpha - fadeSpeed
                if (newAlpha > 0) {
                    trail[index] = point.copy(alpha = newAlpha)
                }
            }
            trail.removeAll { it.alpha <= 0 }
            if (trail.isEmpty()) {
                toRemove.add(id)
            }
        }
        toRemove.forEach { trails.remove(it) }
    }
    
    fun clear() = trails.clear()
}

/**
 * 涟漪效果
 */
data class PhysicsRipple(
    val center: Vec2,
    var radius: Float,
    var alpha: Float,
    val maxRadius: Float,
    val color: Color,
    val speed: Float = 200f,
    val fadeSpeed: Float = 0.02f
) {
    fun update(dt: Float): Boolean {
        radius += speed * dt
        alpha -= fadeSpeed
        return alpha > 0 && radius < maxRadius
    }
}

/**
 * 涟漪系统
 */
class RippleSystem {
    val ripples = mutableListOf<PhysicsRipple>()
    
    fun addRipple(center: Vec2, maxRadius: Float, color: Color, count: Int = 3) {
        repeat(count) { i ->
            ripples.add(PhysicsRipple(
                center = center,
                radius = i * 20f,
                alpha = 1f - i * 0.2f,
                maxRadius = maxRadius,
                color = color,
                speed = 150f + i * 30f,
                fadeSpeed = 0.015f + i * 0.005f
            ))
        }
    }
    
    fun update(dt: Float) {
        ripples.removeAll { !it.update(dt) }
    }
}

/**
 * 光晕效果
 */
data class GlowEffect(
    var position: Vec2,
    var radius: Float,
    var intensity: Float,
    var color: Color,
    var pulsePhase: Float = 0f,
    var pulseSpeed: Float = 2f,
    var pulseAmount: Float = 0.2f
) {
    fun update(dt: Float) {
        pulsePhase += pulseSpeed * dt
        if (pulsePhase > 2 * PI) pulsePhase -= 2 * PI.toFloat()
    }
    
    val currentIntensity: Float
        get() = intensity * (1f + sin(pulsePhase) * pulseAmount)
    
    val currentRadius: Float
        get() = radius * (1f + sin(pulsePhase) * pulseAmount * 0.5f)
}

// ==================== 环境系统 ====================

/**
 * 风场
 */
class WindField(
    var baseDirection: Vec2 = Vec2(1f, 0f),
    var baseStrength: Float = 50f,
    var gustStrength: Float = 100f,
    var gustFrequency: Float = 0.5f,
    var turbulenceScale: Float = 0.01f,
    var turbulenceStrength: Float = 30f
) {
    private var time = 0f
    private val noise = PerlinNoiseGenerator()
    
    fun update(dt: Float) {
        time += dt
    }
    
    fun getWindAt(position: Vec2): Vec2 {
        // 基础风向
        var wind = baseDirection * baseStrength
        
        // 阵风
        val gust = sin(time * gustFrequency * 2 * PI.toFloat()) * 0.5f + 0.5f
        wind = wind + baseDirection * gustStrength * gust
        
        // 湍流
        val turbX = noise.noise(position.x * turbulenceScale + time, position.y * turbulenceScale)
        val turbY = noise.noise(position.x * turbulenceScale, position.y * turbulenceScale + time)
        wind = wind + Vec2(turbX, turbY) * turbulenceStrength
        
        return wind
    }
}

/**
 * 天气系统
 */
enum class WeatherType {
    CLEAR, WINDY, STORMY, CALM
}

class WeatherSystem {
    var currentWeather = WeatherType.CLEAR
    var transitionProgress = 1f
    var targetWeather = WeatherType.CLEAR
    
    val windField = WindField()
    var rainIntensity = 0f
    var lightningChance = 0f
    
    fun setWeather(weather: WeatherType, instant: Boolean = false) {
        if (instant) {
            currentWeather = weather
            targetWeather = weather
            transitionProgress = 1f
            applyWeatherSettings(weather)
        } else {
            targetWeather = weather
            transitionProgress = 0f
        }
    }
    
    fun update(dt: Float) {
        if (transitionProgress < 1f) {
            transitionProgress = (transitionProgress + dt * 0.5f).coerceAtMost(1f)
            if (transitionProgress >= 1f) {
                currentWeather = targetWeather
            }
        }
        
        windField.update(dt)
    }
    
    private fun applyWeatherSettings(weather: WeatherType) {
        when (weather) {
            WeatherType.CLEAR -> {
                windField.baseStrength = 20f
                windField.gustStrength = 30f
                rainIntensity = 0f
                lightningChance = 0f
            }
            WeatherType.WINDY -> {
                windField.baseStrength = 80f
                windField.gustStrength = 150f
                rainIntensity = 0f
                lightningChance = 0f
            }
            WeatherType.STORMY -> {
                windField.baseStrength = 120f
                windField.gustStrength = 250f
                rainIntensity = 1f
                lightningChance = 0.01f
            }
            WeatherType.CALM -> {
                windField.baseStrength = 5f
                windField.gustStrength = 10f
                rainIntensity = 0f
                lightningChance = 0f
            }
        }
    }
}

// ==================== 生态系统 ====================

/**
 * 生态实体类型
 */
enum class EcosystemEntityType {
    PLANT, PREY, PREDATOR, PARTICLE
}

/**
 * 生态实体
 */
open class EcosystemEntity(
    var position: Vec2,
    var velocity: Vec2 = Vec2.ZERO,
    var energy: Float = 100f,
    var maxEnergy: Float = 100f,
    var size: Float = 10f,
    var age: Float = 0f,
    var type: EcosystemEntityType = EcosystemEntityType.PARTICLE,
    var color: Color = Color.White
) {
    var isAlive = true
    
    open fun update(dt: Float, ecosystem: Ecosystem) {
        age += dt
        energy -= dt * 0.5f // 基础能量消耗
        
        if (energy <= 0) {
            isAlive = false
        }
    }
}

/**
 * 植物实体
 */
class PlantEntity(
    position: Vec2,
    size: Float = 15f,
    color: Color = Color(0xFF4CAF50)
) : EcosystemEntity(position, Vec2.ZERO, 50f, 50f, size, 0f, EcosystemEntityType.PLANT, color) {
    var growthStage = 0f
    
    override fun update(dt: Float, ecosystem: Ecosystem) {
        super.update(dt, ecosystem)
        
        // 生长
        if (growthStage < 1f) {
            growthStage = (growthStage + dt * 0.1f).coerceAtMost(1f)
            size = 5f + growthStage * 15f
        }
        
        // 光合作用恢复能量
        energy = (energy + dt * 2f).coerceAtMost(maxEnergy)
    }
}

/**
 * 猎物实体（如蝴蝶、萤火虫）
 */
class PreyEntity(
    position: Vec2,
    velocity: Vec2 = Vec2.ZERO,
    size: Float = 8f,
    color: Color = Color(0xFFFFEB3B)
) : EcosystemEntity(position, velocity, 80f, 80f, size, 0f, EcosystemEntityType.PREY, color) {
    var wanderAngle = Random.nextFloat() * 2 * PI.toFloat()
    var fearLevel = 0f
    
    override fun update(dt: Float, ecosystem: Ecosystem) {
        super.update(dt, ecosystem)
        
        // 寻找食物（植物）
        var nearestPlant: PlantEntity? = null
        var nearestDist = Float.MAX_VALUE
        
        for (entity in ecosystem.entities) {
            if (entity is PlantEntity && entity.isAlive && entity.growthStage > 0.5f) {
                val dist = (entity.position - position).length()
                if (dist < nearestDist && dist < 200f) {
                    nearestDist = dist
                    nearestPlant = entity
                }
            }
        }
        
        // 检测捕食者
        var nearestPredator: PredatorEntity? = null
        var predatorDist = Float.MAX_VALUE
        
        for (entity in ecosystem.entities) {
            if (entity is PredatorEntity && entity.isAlive) {
                val dist = (entity.position - position).length()
                if (dist < predatorDist && dist < 150f) {
                    predatorDist = dist
                    nearestPredator = entity
                }
            }
        }
        
        // 行为决策
        var targetVelocity = Vec2.ZERO
        
        if (nearestPredator != null && predatorDist < 150f) {
            // 逃跑
            fearLevel = 1f
            val fleeDir = (position - nearestPredator.position).normalized()
            targetVelocity = fleeDir * 150f
            energy -= dt * 2f // 逃跑消耗额外能量
        } else if (nearestPlant != null && energy < maxEnergy * 0.7f) {
            // 寻找食物
            fearLevel = (fearLevel - dt).coerceAtLeast(0f)
            val seekDir = (nearestPlant.position - position).normalized()
            targetVelocity = seekDir * 80f
            
            // 吃食物
            if (nearestDist < 20f) {
                energy = (energy + 20f).coerceAtMost(maxEnergy)
                nearestPlant.energy -= 10f
            }
        } else {
            // 漫游
            fearLevel = (fearLevel - dt).coerceAtLeast(0f)
            wanderAngle += (Random.nextFloat() - 0.5f) * 2f * dt
            targetVelocity = Vec2.fromAngle(wanderAngle, 40f)
        }
        
        // 平滑速度变化
        velocity = velocity.lerp(targetVelocity, dt * 3f)
        position = position + velocity * dt
    }
}

/**
 * 捕食者实体
 */
class PredatorEntity(
    position: Vec2,
    velocity: Vec2 = Vec2.ZERO,
    size: Float = 12f,
    color: Color = Color(0xFFF44336)
) : EcosystemEntity(position, velocity, 120f, 120f, size, 0f, EcosystemEntityType.PREDATOR, color) {
    var huntCooldown = 0f
    var targetPrey: PreyEntity? = null
    
    override fun update(dt: Float, ecosystem: Ecosystem) {
        super.update(dt, ecosystem)
        
        huntCooldown = (huntCooldown - dt).coerceAtLeast(0f)
        
        // 寻找猎物
        if (targetPrey == null || !targetPrey!!.isAlive) {
            targetPrey = null
            var nearestDist = Float.MAX_VALUE
            
            for (entity in ecosystem.entities) {
                if (entity is PreyEntity && entity.isAlive) {
                    val dist = (entity.position - position).length()
                    if (dist < nearestDist && dist < 250f) {
                        nearestDist = dist
                        targetPrey = entity
                    }
                }
            }
        }
        
        var targetVelocity = Vec2.ZERO
        
        if (targetPrey != null && huntCooldown <= 0f) {
            // 追捕
            val chaseDir = (targetPrey!!.position - position).normalized()
            targetVelocity = chaseDir * 100f
            energy -= dt * 1.5f // 追捕消耗能量
            
            // 捕获
            val dist = (targetPrey!!.position - position).length()
            if (dist < 15f) {
                targetPrey!!.isAlive = false
                energy = (energy + 50f).coerceAtMost(maxEnergy)
                huntCooldown = 3f
                targetPrey = null
            }
        } else {
            // 休息/漫游
            val wanderAngle = sin(age * 0.5f) * PI.toFloat()
            targetVelocity = Vec2.fromAngle(wanderAngle, 30f)
        }
        
        velocity = velocity.lerp(targetVelocity, dt * 2f)
        position = position + velocity * dt
    }
}

/**
 * 生态系统
 */
class Ecosystem(val bounds: PhysicsBounds) {
    val entities = mutableListOf<EcosystemEntity>()
    var time = 0f
    
    fun update(dt: Float) {
        time += dt
        
        // Update所有实体
        for (entity in entities) {
            if (entity.isAlive) {
                entity.update(dt, this)
                
                // 边界约束
                entity.position = Vec2(
                    entity.position.x.coerceIn(bounds.minX + entity.size, bounds.maxX - entity.size),
                    entity.position.y.coerceIn(bounds.minY + entity.size, bounds.maxY - entity.size)
                )
            }
        }
        
        // 移除死亡实体
        entities.removeAll { !it.isAlive }
        
        // 自然繁殖
        if (Random.nextFloat() < 0.01f * dt) {
            spawnRandomEntity()
        }
    }
    
    fun spawnRandomEntity() {
        val x = Random.nextFloat() * (bounds.maxX - bounds.minX) + bounds.minX
        val y = Random.nextFloat() * (bounds.maxY - bounds.minY) + bounds.minY
        
        when (Random.nextInt(10)) {
            in 0..5 -> entities.add(PlantEntity(Vec2(x, y)))
            in 6..8 -> entities.add(PreyEntity(Vec2(x, y)))
            9 -> if (entities.count { it is PredatorEntity } < 3) {
                entities.add(PredatorEntity(Vec2(x, y)))
            }
        }
    }
    
    fun initialize(plantCount: Int = 20, preyCount: Int = 10, predatorCount: Int = 2) {
        entities.clear()
        
        repeat(plantCount) {
            val x = Random.nextFloat() * (bounds.maxX - bounds.minX) + bounds.minX
            val y = Random.nextFloat() * (bounds.maxY - bounds.minY) + bounds.minY
            entities.add(PlantEntity(Vec2(x, y)))
        }
        
        repeat(preyCount) {
            val x = Random.nextFloat() * (bounds.maxX - bounds.minX) + bounds.minX
            val y = Random.nextFloat() * (bounds.maxY - bounds.minY) + bounds.minY
            entities.add(PreyEntity(Vec2(x, y)))
        }
        
        repeat(predatorCount) {
            val x = Random.nextFloat() * (bounds.maxX - bounds.minX) + bounds.minX
            val y = Random.nextFloat() * (bounds.maxY - bounds.minY) + bounds.minY
            entities.add(PredatorEntity(Vec2(x, y)))
        }
    }
}
