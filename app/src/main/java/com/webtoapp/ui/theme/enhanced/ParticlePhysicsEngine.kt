package com.webtoapp.ui.theme.enhanced

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.*
import kotlin.random.Random

/**
 * ============================================================
 * 高级粒子物理引擎
 * 提供真实的物理模拟、碰撞检测、力场效果
 * ============================================================
 */

/**
 * 2D 向量工具类
 */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vec2(x / scalar, y / scalar)
    
    fun length() = sqrt(x * x + y * y)
    fun lengthSquared() = x * x + y * y
    fun normalized(): Vec2 {
        val len = length()
        return if (len > 0.0001f) Vec2(x / len, y / len) else Vec2(0f, 0f)
    }
    fun dot(other: Vec2) = x * other.x + y * other.y
    fun cross(other: Vec2) = x * other.y - y * other.x
    fun rotate(angle: Float): Vec2 {
        val cos = cos(angle)
        val sin = sin(angle)
        return Vec2(x * cos - y * sin, x * sin + y * cos)
    }
    fun lerp(target: Vec2, t: Float) = Vec2(
        x + (target.x - x) * t,
        y + (target.y - y) * t
    )
    fun toOffset() = Offset(x, y)
    
    companion object {
        val ZERO = Vec2(0f, 0f)
        val UP = Vec2(0f, -1f)
        val DOWN = Vec2(0f, 1f)
        val LEFT = Vec2(-1f, 0f)
        val RIGHT = Vec2(1f, 0f)
        
        fun fromAngle(angle: Float, length: Float = 1f) = Vec2(
            cos(angle) * length,
            sin(angle) * length
        )
        
        fun random(minX: Float, maxX: Float, minY: Float, maxY: Float) = Vec2(
            Random.nextFloat() * (maxX - minX) + minX,
            Random.nextFloat() * (maxY - minY) + minY
        )
        
        fun fromOffset(offset: Offset) = Vec2(offset.x, offset.y)
    }
}

/**
 * 高级粒子类
 */
data class AdvancedParticle(
    var position: Vec2,
    var velocity: Vec2,
    var acceleration: Vec2 = Vec2.ZERO,
    var mass: Float = 1f,
    var size: Float = 10f,
    var rotation: Float = 0f,
    var angularVelocity: Float = 0f,
    var life: Float = 1f,
    var maxLife: Float = 1f,
    var color: Color = Color.White,
    var alpha: Float = 1f,
    var scale: Float = 1f,
    var drag: Float = 0.98f,
    var bounce: Float = 0.6f,
    var gravity: Float = 0f,
    var type: Int = 0,
    var data: Any? = null
) {
    val lifeRatio: Float get() = (life / maxLife).coerceIn(0f, 1f)
    val isDead: Boolean get() = life <= 0f
    
    fun update(deltaTime: Float, worldGravity: Vec2 = Vec2.ZERO) {
        // App重力
        val gravityForce = worldGravity * gravity
        acceleration = acceleration + gravityForce
        
        // Update速度
        velocity = velocity + acceleration * deltaTime
        velocity = velocity * drag
        
        // Update位置
        position = position + velocity * deltaTime
        
        // Update旋转
        rotation += angularVelocity * deltaTime
        
        // Reset加速度
        acceleration = Vec2.ZERO
        
        // Update生命
        life -= deltaTime
    }
    
    fun applyForce(force: Vec2) {
        acceleration = acceleration + force / mass
    }
    
    fun applyImpulse(impulse: Vec2) {
        velocity = velocity + impulse / mass
    }
}

/**
 * 力场类型
 */
sealed class ForceField {
    abstract fun apply(particle: AdvancedParticle, deltaTime: Float)
    
    /**
     * 点吸引力场
     */
    data class PointAttractor(
        val position: Vec2,
        val strength: Float,
        val radius: Float,
        val falloff: Float = 2f // 衰减指数
    ) : ForceField() {
        override fun apply(particle: AdvancedParticle, deltaTime: Float) {
            val diff = position - particle.position
            val dist = diff.length()
            if (dist < radius && dist > 0.1f) {
                val normalizedDist = dist / radius
                val force = strength * (1f - normalizedDist.pow(falloff))
                particle.applyForce(diff.normalized() * force)
            }
        }
    }
    
    /**
     * 点排斥力场
     */
    data class PointRepeller(
        val position: Vec2,
        val strength: Float,
        val radius: Float,
        val falloff: Float = 2f
    ) : ForceField() {
        override fun apply(particle: AdvancedParticle, deltaTime: Float) {
            val diff = particle.position - position
            val dist = diff.length()
            if (dist < radius && dist > 0.1f) {
                val normalizedDist = dist / radius
                val force = strength * (1f - normalizedDist.pow(falloff))
                particle.applyForce(diff.normalized() * force)
            }
        }
    }
    
    /**
     * 漩涡力场
     */
    data class Vortex(
        val position: Vec2,
        val strength: Float,
        val radius: Float,
        val inwardPull: Float = 0.3f
    ) : ForceField() {
        override fun apply(particle: AdvancedParticle, deltaTime: Float) {
            val diff = particle.position - position
            val dist = diff.length()
            if (dist < radius && dist > 0.1f) {
                val normalizedDist = dist / radius
                val tangent = Vec2(-diff.y, diff.x).normalized()
                val inward = diff.normalized() * -1f
                val force = (tangent + inward * inwardPull) * strength * (1f - normalizedDist)
                particle.applyForce(force)
            }
        }
    }
    
    /**
     * 风力场
     */
    data class Wind(
        val direction: Vec2,
        val strength: Float,
        val turbulence: Float = 0f,
        var time: Float = 0f
    ) : ForceField() {
        override fun apply(particle: AdvancedParticle, deltaTime: Float) {
            time += deltaTime
            val turbulenceOffset = if (turbulence > 0) {
                Vec2(
                    sin(time * 3f + particle.position.x * 0.01f) * turbulence,
                    cos(time * 2f + particle.position.y * 0.01f) * turbulence
                )
            } else Vec2.ZERO
            particle.applyForce((direction + turbulenceOffset) * strength)
        }
    }
    
    /**
     * 噪声力场
     */
    data class NoiseField(
        val strength: Float,
        val scale: Float = 0.01f,
        var time: Float = 0f
    ) : ForceField() {
        override fun apply(particle: AdvancedParticle, deltaTime: Float) {
            time += deltaTime
            val noiseX = sin(particle.position.x * scale + time) * cos(particle.position.y * scale * 0.7f)
            val noiseY = cos(particle.position.x * scale * 0.8f + time * 0.5f) * sin(particle.position.y * scale)
            particle.applyForce(Vec2(noiseX, noiseY) * strength)
        }
    }
    
    /**
     * 边界反弹力场
     */
    data class BoundaryBounce(
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float,
        val bounciness: Float = 0.8f,
        val padding: Float = 0f
    ) : ForceField() {
        override fun apply(particle: AdvancedParticle, deltaTime: Float) {
            val p = particle.position
            val v = particle.velocity
            val pad = padding + particle.size / 2
            
            if (p.x < minX + pad) {
                particle.position = Vec2(minX + pad, p.y)
                particle.velocity = Vec2(-v.x * bounciness, v.y)
            }
            if (p.x > maxX - pad) {
                particle.position = Vec2(maxX - pad, p.y)
                particle.velocity = Vec2(-v.x * bounciness, v.y)
            }
            if (p.y < minY + pad) {
                particle.position = Vec2(p.x, minY + pad)
                particle.velocity = Vec2(v.x, -v.y * bounciness)
            }
            if (p.y > maxY - pad) {
                particle.position = Vec2(p.x, maxY - pad)
                particle.velocity = Vec2(v.x, -v.y * bounciness)
            }
        }
    }
}

/**
 * 粒子发射器配置
 */
data class EmitterConfig(
    val position: Vec2,
    val emissionRate: Float = 10f,          // 每秒发射数量
    val emissionAngle: Float = 0f,          // 发射角度
    val emissionSpread: Float = PI.toFloat(), // 发射扩散角度
    val particleSpeed: ClosedFloatingPointRange<Float> = 50f..100f,
    val particleSize: ClosedFloatingPointRange<Float> = 5f..15f,
    val particleLife: ClosedFloatingPointRange<Float> = 1f..3f,
    val particleColor: List<Color> = listOf(Color.White),
    val particleDrag: Float = 0.98f,
    val particleGravity: Float = 0f,
    val particleBounce: Float = 0.6f,
    val burstCount: Int = 0,                // 爆发数量，0表示持续发射
    val initialRotation: ClosedFloatingPointRange<Float> = 0f..360f,
    val angularVelocity: ClosedFloatingPointRange<Float> = -90f..90f
)

/**
 * 粒子发射器
 */
class ParticleEmitter(
    var config: EmitterConfig,
    var enabled: Boolean = true
) {
    private var accumulator: Float = 0f
    
    fun emit(deltaTime: Float): List<AdvancedParticle> {
        if (!enabled) return emptyList()
        
        val particles = mutableListOf<AdvancedParticle>()
        
        if (config.burstCount > 0) {
            // 爆发模式
            repeat(config.burstCount) {
                particles.add(createParticle())
            }
            enabled = false
        } else {
            // 持续发射模式
            accumulator += deltaTime * config.emissionRate
            while (accumulator >= 1f) {
                particles.add(createParticle())
                accumulator -= 1f
            }
        }
        
        return particles
    }
    
    private fun createParticle(): AdvancedParticle {
        val angle = config.emissionAngle + (Random.nextFloat() - 0.5f) * config.emissionSpread
        val speed = Random.nextFloat() * (config.particleSpeed.endInclusive - config.particleSpeed.start) + config.particleSpeed.start
        val size = Random.nextFloat() * (config.particleSize.endInclusive - config.particleSize.start) + config.particleSize.start
        val life = Random.nextFloat() * (config.particleLife.endInclusive - config.particleLife.start) + config.particleLife.start
        val rotation = Random.nextFloat() * (config.initialRotation.endInclusive - config.initialRotation.start) + config.initialRotation.start
        val angVel = Random.nextFloat() * (config.angularVelocity.endInclusive - config.angularVelocity.start) + config.angularVelocity.start
        
        return AdvancedParticle(
            position = config.position,
            velocity = Vec2.fromAngle(angle, speed),
            size = size,
            rotation = rotation,
            angularVelocity = angVel,
            life = life,
            maxLife = life,
            color = config.particleColor.random(),
            drag = config.particleDrag,
            gravity = config.particleGravity,
            bounce = config.particleBounce
        )
    }
}

/**
 * 粒子系统
 */
class ParticleSystem(
    val maxParticles: Int = 1000,
    var worldGravity: Vec2 = Vec2(0f, 200f)
) {
    private val particles = mutableListOf<AdvancedParticle>()
    private val emitters = mutableListOf<ParticleEmitter>()
    private val forceFields = mutableListOf<ForceField>()
    
    val particleCount: Int get() = particles.size
    val activeParticles: List<AdvancedParticle> get() = particles.toList()
    
    fun addEmitter(emitter: ParticleEmitter) {
        emitters.add(emitter)
    }
    
    fun removeEmitter(emitter: ParticleEmitter) {
        emitters.remove(emitter)
    }
    
    fun addForceField(field: ForceField) {
        forceFields.add(field)
    }
    
    fun removeForceField(field: ForceField) {
        forceFields.remove(field)
    }
    
    fun clearForceFields() {
        forceFields.clear()
    }
    
    fun addParticle(particle: AdvancedParticle) {
        if (particles.size < maxParticles) {
            particles.add(particle)
        }
    }
    
    fun addParticles(newParticles: List<AdvancedParticle>) {
        val available = maxParticles - particles.size
        particles.addAll(newParticles.take(available))
    }
    
    fun update(deltaTime: Float) {
        // 从发射器生成新粒子
        emitters.forEach { emitter ->
            val newParticles = emitter.emit(deltaTime)
            addParticles(newParticles)
        }
        
        // Update所有粒子
        particles.forEach { particle ->
            // App力场
            forceFields.forEach { field ->
                field.apply(particle, deltaTime)
            }
            
            // Update粒子物理
            particle.update(deltaTime, worldGravity)
        }
        
        // 移除死亡粒子
        particles.removeAll { it.isDead }
    }
    
    fun clear() {
        particles.clear()
    }
    
    /**
     * 在指定位置爆发粒子
     */
    fun burst(
        position: Vec2,
        count: Int,
        config: EmitterConfig
    ) {
        val emitter = ParticleEmitter(
            config = config.copy(position = position, burstCount = count),
            enabled = true
        )
        addParticles(emitter.emit(0f))
    }
}

/**
 * 柏林噪声生成器
 */
class PerlinNoiseGenerator(seed: Long = System.currentTimeMillis()) {
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
    
    fun fbm(x: Float, y: Float, octaves: Int = 4, persistence: Float = 0.5f): Float {
        var value = 0f
        var amplitude = 1f
        var frequency = 1f
        var maxValue = 0f
        
        for (i in 0 until octaves) {
            value += amplitude * noise(x * frequency, y * frequency)
            maxValue += amplitude
            amplitude *= persistence
            frequency *= 2f
        }
        
        return value / maxValue
    }
    
    fun turbulence(x: Float, y: Float, octaves: Int = 4): Float {
        var value = 0f
        var amplitude = 1f
        var frequency = 1f
        
        for (i in 0 until octaves) {
            value += amplitude * abs(noise(x * frequency, y * frequency))
            amplitude *= 0.5f
            frequency *= 2f
        }
        
        return value
    }
}

/**
 * 颜色工具
 */
object ColorUtils {
    fun lerp(start: Color, end: Color, fraction: Float): Color {
        val f = fraction.coerceIn(0f, 1f)
        return Color(
            red = start.red + (end.red - start.red) * f,
            green = start.green + (end.green - start.green) * f,
            blue = start.blue + (end.blue - start.blue) * f,
            alpha = start.alpha + (end.alpha - start.alpha) * f
        )
    }
    
    fun gradient(colors: List<Color>, t: Float): Color {
        if (colors.isEmpty()) return Color.White
        if (colors.size == 1) return colors[0]
        
        val scaledT = t.coerceIn(0f, 1f) * (colors.size - 1)
        val index = scaledT.toInt().coerceIn(0, colors.size - 2)
        val localT = scaledT - index
        
        return lerp(colors[index], colors[index + 1], localT)
    }
    
    fun withAlpha(color: Color, alpha: Float): Color {
        return color.copy(alpha = alpha.coerceIn(0f, 1f))
    }
    
    fun brighten(color: Color, amount: Float): Color {
        return Color(
            red = (color.red + amount).coerceIn(0f, 1f),
            green = (color.green + amount).coerceIn(0f, 1f),
            blue = (color.blue + amount).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }
    
    fun darken(color: Color, amount: Float): Color {
        return brighten(color, -amount)
    }
}

/**
 * 缓动函数
 */
object Easing {
    fun linear(t: Float) = t
    fun easeInQuad(t: Float) = t * t
    fun easeOutQuad(t: Float) = t * (2 - t)
    fun easeInOutQuad(t: Float) = if (t < 0.5f) 2 * t * t else -1 + (4 - 2 * t) * t
    fun easeInCubic(t: Float) = t * t * t
    fun easeOutCubic(t: Float) = (t - 1).let { it * it * it + 1 }
    fun easeInOutCubic(t: Float) = if (t < 0.5f) 4 * t * t * t else (t - 1).let { (2 * t - 2) * it * it + 1 }
    fun easeInExpo(t: Float) = if (t == 0f) 0f else 2f.pow(10 * (t - 1))
    fun easeOutExpo(t: Float) = if (t == 1f) 1f else 1 - 2f.pow(-10 * t)
    fun easeInOutExpo(t: Float) = when {
        t == 0f -> 0f
        t == 1f -> 1f
        t < 0.5f -> 2f.pow(20 * t - 10) / 2
        else -> (2 - 2f.pow(-20 * t + 10)) / 2
    }
    fun easeInElastic(t: Float): Float {
        val c4 = (2 * PI / 3).toFloat()
        return when {
            t == 0f -> 0f
            t == 1f -> 1f
            else -> -2f.pow(10 * t - 10) * sin((t * 10 - 10.75f) * c4)
        }
    }
    fun easeOutElastic(t: Float): Float {
        val c4 = (2 * PI / 3).toFloat()
        return when {
            t == 0f -> 0f
            t == 1f -> 1f
            else -> 2f.pow(-10 * t) * sin((t * 10 - 0.75f) * c4) + 1
        }
    }
    fun easeOutBounce(t: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            t < 1 / d1 -> n1 * t * t
            t < 2 / d1 -> n1 * (t - 1.5f / d1).let { it * it } + 0.75f
            t < 2.5 / d1 -> n1 * (t - 2.25f / d1).let { it * it } + 0.9375f
            else -> n1 * (t - 2.625f / d1).let { it * it } + 0.984375f
        }
    }
    fun easeInBounce(t: Float) = 1 - easeOutBounce(1 - t)
}
