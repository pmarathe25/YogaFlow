package com.example.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun BattleBackground(
    modifier: Modifier = Modifier,
    parallaxOffset: Float = 0f,
    bossFight: Boolean = false
) {
    val skyColor = if (bossFight) Color(0xFF1A0033) else Color(0xFF0D1B2A)
    val groundColor = if (bossFight) Color(0xFF2D1B4E) else Color(0xFF1B2838)
    val accentColor = if (bossFight) Color(0xFFFF4444) else Color(0xFF4488FF)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sky gradient
        drawRect(skyColor)
        for (i in 0..5) {
            val alpha = 0.05f * (1f - i / 6f)
            drawRect(
                color = accentColor.copy(alpha = alpha),
                topLeft = Offset(0f, h * i / 6f),
                size = androidx.compose.ui.geometry.Size(w, h / 6f)
            )
        }

        // Stars
        val starCount = if (bossFight) 40 else 25
        repeat(starCount) {
            val sx = (it * 137.5f + 50f * sin(it * 1.3f)) % w
            val sy = (it * 97.3f + parallaxOffset * 20f) % (h * 0.5f)
            val starSize = 1f + (it % 3)
            drawCircle(
                color = Color.White.copy(alpha = 0.3f + 0.3f * sin(it * 0.7f + parallaxOffset)),
                radius = starSize,
                center = Offset(sx, sy)
            )
        }

        // Ground with parallax
        val groundY = h * 0.65f
        val groundPath = Path().apply {
            moveTo(0f, groundY)
            for (x in 0..(w.toInt()) step 20) {
                val y = groundY + 30f * sin(x * 0.02f + parallaxOffset)
                lineTo(x.toFloat(), y)
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(groundPath, groundColor)

        // Decorative pillars/platforms
        drawPillar(w * 0.15f, groundY - 10f, 20f, 60f, accentColor.copy(alpha = 0.3f))
        drawPillar(w * 0.85f, groundY - 10f, 20f, 60f, accentColor.copy(alpha = 0.3f))
        if (bossFight) {
            drawPillar(w * 0.5f, groundY - 15f, 30f, 80f, Color(0xFFFF4444).copy(alpha = 0.4f))
        }

        // Vignette
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x80000000)),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.8f
            )
        )
    }
}

private fun DrawScope.drawPillar(x: Float, baseY: Float, width: Float, height: Float, color: Color) {
    drawRect(color, Offset(x - width / 2f, baseY - height), androidx.compose.ui.geometry.Size(width, height))
    // Cap
    drawRect(
        color.copy(alpha = color.alpha * 1.5f),
        Offset(x - width * 0.7f, baseY - height - 5f),
        androidx.compose.ui.geometry.Size(width * 1.4f, 8f)
    )
}

@Composable
fun HeroSprite(
    modifier: Modifier = Modifier,
    heroName: String,
    elementColor: Color,
    scale: Float = 1f,
    isActive: Boolean = true,
    isDamaged: Boolean = false,
    flashAlpha: Float = 0f
) {
    val tint = if (!isActive) elementColor.copy(alpha = 0.4f) else elementColor
    val flash = if (isDamaged) Color.Red.copy(alpha = flashAlpha) else Color.Transparent

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.6f
        val s = size.minDimension * 0.2f * scale

        drawSilhouette(cx, cy, s, heroName, tint)

        // Damage flash
        if (isDamaged && flashAlpha > 0f) {
            drawRect(flash, alpha = flashAlpha)
        }

        // Active indicator
        if (isActive) {
            drawCircle(
                color = elementColor.copy(alpha = 0.15f),
                radius = s * 1.5f,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
fun MonsterSprite(
    modifier: Modifier = Modifier,
    monsterName: String,
    elementColor: Color,
    isBoss: Boolean = false,
    isDamaged: Boolean = false,
    flashAlpha: Float = 0f,
    bossPulse: Float = 0f
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.42f
        val s = size.minDimension * 0.3f * (if (isBoss) 1.3f else 1f)

        // Boss aura
        if (isBoss && bossPulse > 0f) {
            val auraRadius = s * (1.5f + 0.3f * sin(bossPulse * PI.toFloat()))
            drawCircle(
                color = elementColor.copy(alpha = 0.15f),
                radius = auraRadius,
                center = Offset(cx, cy)
            )
        }

        drawMonsterShape(cx, cy, s, monsterName, elementColor)

        if (isDamaged && flashAlpha > 0f) {
            drawRect(Color.Red.copy(alpha = flashAlpha))
        }
    }
}

private fun DrawScope.drawSilhouette(cx: Float, cy: Float, s: Float, name: String, tint: Color) {
    val path = Path()

    when (name) {
        "Shanti" -> {
            // Flowing feminine silhouette with water-like curves
            path.moveTo(cx - s * 0.3f, cy - s * 0.8f)
            path.quadraticBezierTo(cx - s * 0.1f, cy - s * 1.1f, cx + s * 0.1f, cy - s * 0.9f)
            path.quadraticBezierTo(cx + s * 0.2f, cy - s * 0.7f, cx + s * 0.15f, cy - s * 0.4f)
            path.lineTo(cx + s * 0.35f, cy + s * 0.1f)
            path.quadraticBezierTo(cx + s * 0.4f, cy + s * 0.4f, cx + s * 0.2f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.2f, cy + s * 0.6f)
            path.quadraticBezierTo(cx - s * 0.4f, cy + s * 0.4f, cx - s * 0.35f, cy + s * 0.1f)
            path.lineTo(cx - s * 0.15f, cy - s * 0.4f)
            path.close()
            drawPath(path, tint)
            // Lotus detail
            drawCircle(tint.copy(alpha = 0.3f), s * 0.15f, Offset(cx, cy - s * 0.3f))
        }
        "Santosha" -> {
            // Broad, solid shape
            path.moveTo(cx - s * 0.5f, cy - s * 0.5f)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.9f, cx - s * 0.2f, cy - s * 0.9f)
            path.quadraticBezierTo(cx, cy - s * 1.0f, cx + s * 0.2f, cy - s * 0.9f)
            path.quadraticBezierTo(cx + s * 0.5f, cy - s * 0.9f, cx + s * 0.5f, cy - s * 0.5f)
            path.quadraticBezierTo(cx + s * 0.6f, cy, cx + s * 0.4f, cy + s * 0.5f)
            path.lineTo(cx - s * 0.4f, cy + s * 0.5f)
            path.quadraticBezierTo(cx - s * 0.6f, cy, cx - s * 0.5f, cy - s * 0.5f)
            path.close()
            drawPath(path, tint)
            // Shield emblem
            drawCircle(tint.copy(alpha = 0.4f), s * 0.2f, Offset(cx, cy - s * 0.1f))
        }
        "Virya" -> {
            // Dynamic angled pose
            path.moveTo(cx - s * 0.3f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.1f, cy - s * 0.2f)
            path.quadraticBezierTo(cx, cy - s * 0.7f, cx + s * 0.15f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.3f, cy + s * 0.1f)
            path.quadraticBezierTo(cx + s * 0.4f, cy + s * 0.4f, cx + s * 0.2f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.3f, cy + s * 0.6f)
            path.close()
            drawPath(path, tint)
            // Flame crest
            val flamePath = Path().apply {
                moveTo(cx - s * 0.08f, cy - s * 0.7f)
                quadraticBezierTo(cx, cy - s * 0.9f, cx + s * 0.08f, cy - s * 0.7f)
                quadraticBezierTo(cx + s * 0.05f, cy - s * 0.75f, cx, cy - s * 0.85f)
                quadraticBezierTo(cx - s * 0.05f, cy - s * 0.75f, cx - s * 0.08f, cy - s * 0.7f)
                close()
            }
            drawPath(flamePath, tint.copy(alpha = 0.5f))
        }
        "Dhairya" -> {
            // Tall upright figure
            path.moveTo(cx - s * 0.2f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.25f, cy - s * 0.3f)
            path.quadraticBezierTo(cx - s * 0.2f, cy - s * 0.8f, cx, cy - s * 0.9f)
            path.quadraticBezierTo(cx + s * 0.2f, cy - s * 0.8f, cx + s * 0.25f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.2f, cy + s * 0.6f)
            path.close()
            drawPath(path, tint)
            // Lance/spear tip
            val lancePath = Path().apply {
                moveTo(cx, cy - s * 0.9f)
                lineTo(cx + s * 0.03f, cy - s * 1.2f)
                lineTo(cx - s * 0.03f, cy - s * 1.2f)
                close()
            }
            drawPath(lancePath, tint.copy(alpha = 0.6f))
        }
        "Maitri" -> {
            // Open, embracing shape
            path.moveTo(cx - s * 0.4f, cy)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.5f, cx - s * 0.3f, cy - s * 0.7f)
            path.quadraticBezierTo(cx, cy - s * 0.9f, cx + s * 0.3f, cy - s * 0.7f)
            path.quadraticBezierTo(cx + s * 0.5f, cy - s * 0.5f, cx + s * 0.4f, cy)
            path.lineTo(cx + s * 0.35f, cy + s * 0.3f)
            path.quadraticBezierTo(cx + s * 0.2f, cy + s * 0.6f, cx + s * 0.1f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.1f, cy + s * 0.6f)
            path.quadraticBezierTo(cx - s * 0.2f, cy + s * 0.6f, cx - s * 0.35f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint)
            // Heart detail
            val heartPath = Path().apply {
                moveTo(cx, cy - s * 0.1f)
                quadraticBezierTo(cx - s * 0.12f, cy - s * 0.2f, cx - s * 0.08f, cy - s * 0.05f)
                quadraticBezierTo(cx, cy + s * 0.05f, cx, cy + s * 0.05f)
                quadraticBezierTo(cx, cy + s * 0.05f, cx + s * 0.08f, cy - s * 0.05f)
                quadraticBezierTo(cx + s * 0.12f, cy - s * 0.2f, cx, cy - s * 0.1f)
                close()
            }
            drawPath(heartPath, tint.copy(alpha = 0.4f), style = Stroke(width = 2f))
        }
        else -> {
            // Generic humanoid
            path.moveTo(cx - s * 0.3f, cy + s * 0.5f)
            path.lineTo(cx - s * 0.2f, cy - s * 0.3f)
            path.quadraticBezierTo(cx, cy - s * 0.8f, cx + s * 0.2f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.3f, cy + s * 0.5f)
            path.close()
            drawPath(path, tint)
        }
    }
}

private fun DrawScope.drawMonsterShape(cx: Float, cy: Float, s: Float, name: String, tint: Color) {
    val path = Path()

    when {
        name.contains("Bhaya") || name.contains("Fear") -> {
            // Jagged shadow
            path.moveTo(cx - s * 0.4f, cy + s * 0.3f)
            path.lineTo(cx - s * 0.3f, cy - s * 0.2f)
            path.lineTo(cx - s * 0.1f, cy - s * 0.5f)
            path.lineTo(cx, cy - s * 0.3f)
            path.lineTo(cx + s * 0.1f, cy - s * 0.6f)
            path.lineTo(cx + s * 0.25f, cy - s * 0.2f)
            path.lineTo(cx + s * 0.4f, cy + s * 0.1f)
            path.lineTo(cx + s * 0.35f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint)
        }
        name.contains("Tandra") || name.contains("Fatigue") -> {
            // Drooping form
            path.moveTo(cx - s * 0.4f, cy + s * 0.2f)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.1f, cx - s * 0.2f, cy - s * 0.2f)
            path.quadraticBezierTo(cx, cy - s * 0.6f, cx + s * 0.2f, cy - s * 0.2f)
            path.quadraticBezierTo(cx + s * 0.5f, cy - s * 0.1f, cx + s * 0.4f, cy + s * 0.2f)
            path.lineTo(cx - s * 0.4f, cy + s * 0.2f)
            path.close()
            drawPath(path, tint)
        }
        name.contains("Chinta") || name.contains("Anxiety") -> {
            // Spiky electric
            path.moveTo(cx - s * 0.3f, cy + s * 0.3f)
            path.lineTo(cx - s * 0.35f, cy)
            path.lineTo(cx - s * 0.2f, cy - s * 0.2f)
            path.lineTo(cx - s * 0.15f, cy - s * 0.5f)
            path.lineTo(cx, cy - s * 0.3f)
            path.lineTo(cx + s * 0.15f, cy - s * 0.6f)
            path.lineTo(cx + s * 0.2f, cy - s * 0.2f)
            path.lineTo(cx + s * 0.35f, cy)
            path.lineTo(cx + s * 0.3f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint)
        }
        name.contains("Alasya") || name.contains("Sloth") -> {
            // Heavy blob
            drawCircle(tint, s * 0.4f, Offset(cx, cy + s * 0.1f))
            drawCircle(tint.copy(alpha = 0.6f), s * 0.3f, Offset(cx - s * 0.2f, cy - s * 0.1f))
            drawCircle(tint.copy(alpha = 0.6f), s * 0.3f, Offset(cx + s * 0.2f, cy - s * 0.1f))
        }
        name.contains("Matsarya") || name.contains("Envy") -> {
            // Serpentine
            path.moveTo(cx - s * 0.4f, cy + s * 0.3f)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.2f, cx - s * 0.2f, cy - s * 0.5f)
            path.quadraticBezierTo(cx, cy - s * 0.7f, cx + s * 0.2f, cy - s * 0.4f)
            path.quadraticBezierTo(cx + s * 0.5f, cy, cx + s * 0.3f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint)
        }
        name.contains("Krodha") || name.contains("Anger") -> {
            // Jagged flame-like
            path.moveTo(cx, cy + s * 0.4f)
            path.lineTo(cx - s * 0.3f, cy - s * 0.1f)
            path.lineTo(cx - s * 0.1f, cy - s * 0.3f)
            path.lineTo(cx - s * 0.2f, cy - s * 0.6f)
            path.lineTo(cx, cy - s * 0.4f)
            path.lineTo(cx + s * 0.2f, cy - s * 0.6f)
            path.lineTo(cx + s * 0.1f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.3f, cy - s * 0.1f)
            path.close()
            drawPath(path, tint)
            // Inner glow
            drawCircle(tint.copy(alpha = 0.3f), s * 0.2f, Offset(cx, cy - s * 0.1f))
        }
        name.contains("Ahankara") || name.contains("Ego") -> {
            // Large imposing form
            path.moveTo(cx - s * 0.5f, cy + s * 0.4f)
            path.lineTo(cx - s * 0.45f, cy - s * 0.3f)
            path.quadraticBezierTo(cx - s * 0.3f, cy - s * 0.7f, cx, cy - s * 0.8f)
            path.quadraticBezierTo(cx + s * 0.3f, cy - s * 0.7f, cx + s * 0.45f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.5f, cy + s * 0.4f)
            path.close()
            drawPath(path, tint)
            // Crown
            val crownPath = Path().apply {
                moveTo(cx - s * 0.25f, cy - s * 0.65f)
                lineTo(cx - s * 0.15f, cy - s * 0.85f)
                lineTo(cx, cy - s * 0.7f)
                lineTo(cx + s * 0.15f, cy - s * 0.85f)
                lineTo(cx + s * 0.25f, cy - s * 0.65f)
                close()
            }
            drawPath(crownPath, Color.Yellow.copy(alpha = 0.5f))
        }
        name.contains("Samsara") -> {
            // Circular wheel
            drawCircle(tint, s * 0.5f, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = 0.2f), s * 0.3f, Offset(cx, cy))
            // Spokes
            for (i in 0..7) {
                val angle = i * PI.toFloat() / 4f
                val rx = cx + s * 0.45f * cos(angle)
                val ry = cy + s * 0.45f * sin(angle)
                drawLine(tint.copy(alpha = 0.6f), Offset(cx, cy), Offset(rx, ry), 2f)
            }
            drawCircle(Color.White.copy(alpha = 0.3f), s * 0.1f, Offset(cx, cy))
        }
        else -> {
            // Default monster silhouette
            path.moveTo(cx - s * 0.4f, cy + s * 0.3f)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.3f, cx - s * 0.1f, cy - s * 0.5f)
            path.quadraticBezierTo(cx, cy - s * 0.7f, cx + s * 0.1f, cy - s * 0.5f)
            path.quadraticBezierTo(cx + s * 0.5f, cy - s * 0.3f, cx + s * 0.4f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint)
        }
    }
}
