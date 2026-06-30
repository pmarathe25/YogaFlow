package com.example.game.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FloatingTextEntry(
    val id: Long,
    val text: String,
    val color: Color,
    val startX: Float,
    val startY: Float,
    val startTime: Long = 0L,
    val durationMs: Long = 800
)

@Composable
fun FloatingTextOverlay(
    entries: List<FloatingTextEntry>,
    onExpired: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        entries.forEach { entry ->
            FloatingTextItem(entry, onExpired)
        }
    }
}

@Composable
private fun FloatingTextItem(
    entry: FloatingTextEntry,
    onExpired: (Long) -> Unit
) {
    val elapsed = remember { mutableLongStateOf(0L) }
    LaunchedEffect(entry.id) {
        val stepMs = 16L
        var t = 0L
        while (t < entry.durationMs) {
            kotlinx.coroutines.delay(stepMs)
            t += stepMs
            elapsed.longValue = t
        }
        onExpired(entry.id)
    }

    val progress = (elapsed.longValue.toFloat() / entry.durationMs).coerceIn(0f, 1f)
    val alpha by animateFloatAsState(
        targetValue = (1f - progress).coerceIn(0f, 1f),
        animationSpec = tween(16)
    )
    val floatY = -progress * 40f
    val popScale = (1.3f - progress * 0.3f).coerceIn(1f, 1.3f)

    Text(
        text = entry.text,
        color = entry.color.copy(alpha = alpha),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .offset { IntOffset(entry.startX.toInt(), (entry.startY + floatY).toInt()) }
            .alpha(alpha)
            .scale(popScale)
    )
}
