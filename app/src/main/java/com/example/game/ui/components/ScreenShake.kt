package com.example.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin
import kotlin.random.Random

class ShakeHandle {
    private val _offsetX = Animatable(0f)
    private val _offsetY = Animatable(0f)
    private var seed = Random.nextFloat() * 100f

    val offsetX: Float get() = _offsetX.value
    val offsetY: Float get() = _offsetY.value

    suspend fun shake(intensity: Float = 8f, durationMs: Long = 300) {
        seed = Random.nextFloat() * 100f
        val freq = 30f
        val steps = 10
        val stepMs = (durationMs / steps).toInt().coerceAtLeast(16)
        for (i in 0 until steps) {
            val t = i.toFloat() / steps
            val decay = (1f - t) * intensity
            val tx = sin(seed + t * freq) * decay
            val ty = sin(seed * 2f + t * freq * 0.7f) * decay * 0.5f
            _offsetX.animateTo(tx, tween(stepMs))
            _offsetY.animateTo(ty, tween(stepMs))
        }
        _offsetX.animateTo(0f, tween(50))
        _offsetY.animateTo(0f, tween(50))
    }
}

@Composable
fun rememberShakeHandle(): ShakeHandle = remember { ShakeHandle() }

fun Modifier.shakeOffset(handle: ShakeHandle): Modifier = this.graphicsLayer {
    translationX = handle.offsetX
    translationY = handle.offsetY
}
