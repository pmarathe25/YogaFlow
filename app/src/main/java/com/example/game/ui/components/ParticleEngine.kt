package com.example.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var color: Color,
    var alpha: Float,
    var size: Float,
    var lifetime: Int,
    var maxLifetime: Int,
    var gravity: Float = 0f,
    var drag: Float = 0.98f
) {
    val isDead: Boolean get() = lifetime <= 0 || alpha <= 0f
}

data class EmitterConfig(
    val particlesPerSecond: Int = 50,
    val spreadAngle: Float = 360f,
    val force: Float = 100f,
    val gravity: Float = 0f,
    val colors: List<Color>,
    val sizeRange: ClosedFloatingPointRange<Float> = 2f..6f,
    val lifetimeRange: IntRange = 20..60,
    val blendMode: BlendMode = BlendMode.SrcOver
)

data class EmitterState(
    val position: Offset,
    val active: Boolean,
    val config: EmitterConfig
)

class ParticlePool(capacity: Int) {
    private val particles = Array(capacity) {
        Particle(0f, 0f, 0f, 0f, Color.White, 1f, 2f, 0, 1)
    }
    private var activeCount = 0

    fun emit(config: EmitterConfig, position: Offset, count: Int) {
        val angleRad = config.spreadAngle * kotlin.math.PI.toFloat() / 180f
        for (i in 0 until count) {
            val p = getDeadOrNull() ?: return
            val angle = Random.nextFloat() * angleRad - angleRad / 2f
            val speed = Random.nextFloat() * config.force
            p.x = position.x
            p.y = position.y
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.color = config.colors.random()
            p.alpha = 1f
            p.size = Random.nextFloat() * (config.sizeRange.endInclusive - config.sizeRange.start) + config.sizeRange.start
            p.maxLifetime = config.lifetimeRange.random()
            p.lifetime = p.maxLifetime
            p.gravity = config.gravity
            p.drag = 0.98f
            activeCount++
        }
    }

    fun update() {
        var i = 0
        while (i < activeCount) {
            val p = particles[i]
            p.vx *= p.drag
            p.vy += p.gravity
            p.x += p.vx * 0.016f
            p.y += p.vy * 0.016f
            p.lifetime--
            p.alpha = (p.lifetime.toFloat() / p.maxLifetime).coerceAtLeast(0f)

            if (p.isDead) {
                removeAt(i)
            } else {
                i++
            }
        }
    }

    fun draw(drawScope: DrawScope) {
        for (i in 0 until activeCount) {
            val p = particles[i]
            drawScope.drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x, p.y),
                blendMode = BlendMode.SrcOver
            )
        }
    }

    val hasActive: Boolean get() = activeCount > 0

    private fun getDeadOrNull(): Particle? {
        // Try from the end where dead particles are moved
        if (activeCount < particles.size) return particles[activeCount]
        return null
    }

    private fun removeAt(index: Int) {
        val lastIdx = activeCount - 1
        if (index != lastIdx) {
            val temp = particles[index]
            particles[index] = particles[lastIdx]
            particles[lastIdx] = temp
        }
        activeCount--
    }
}

@Composable
fun rememberParticlePool(capacity: Int = 200): ParticlePool {
    return remember { ParticlePool(capacity) }
}

@Composable
fun ParticleEffect(
    config: EmitterConfig,
    active: Boolean,
    position: Offset,
    modifier: Modifier = Modifier,
    pool: ParticlePool = remember { ParticlePool(200) }
) {
    LaunchedEffect(active, position) {
        if (!active) return@LaunchedEffect
        while (true) {
            val count = (config.particlesPerSecond / 60).coerceAtLeast(1)
            pool.emit(config, position, count)
            pool.update()
            kotlinx.coroutines.delay(16)
        }
    }

    if (pool.hasActive) {
        Canvas(modifier = modifier) {
            pool.update()
            pool.draw(this)
        }
    }
}
