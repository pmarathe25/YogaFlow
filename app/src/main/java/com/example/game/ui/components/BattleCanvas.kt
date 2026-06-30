package com.example.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.model.Element
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

// ─── Sprite Animation State ───────────────────────────────────────────

enum class SpriteState { IDLE, ATTACKING, HIT, DYING, DEAD }

data class SpriteAnimState(
    val state: SpriteState = SpriteState.IDLE,
    val stateTime: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f
)

@Composable
fun rememberSpriteAnimState(): MutableState<SpriteAnimState> =
    remember { mutableStateOf(SpriteAnimState()) }

// ─── Background ────────────────────────────────────────────────────────

@Composable
fun BattleBackground(
    modifier: Modifier = Modifier,
    parallaxOffset: Float = 0f,
    bossFight: Boolean = false,
    monsterElement: Element = Element.NEUTRAL,
    elementTint: Color = Color.Transparent
) {
    val skyBase = if (bossFight) Color(0xFF1A0033) else Color(0xFF0D1B2A)
    val groundBase = if (bossFight) Color(0xFF2D1B4E) else Color(0xFF1B2838)

    val tintStrength = if (bossFight) 0.25f else 0.12f
    val skyColor = lerp(skyBase, elementTint, tintStrength)
    val groundColor = lerp(groundBase, elementTint, tintStrength * 0.5f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sky gradient
        drawRect(skyColor)
        for (i in 0..5) {
            val alpha = 0.05f * (1f - i / 6f)
            drawRect(
                color = elementTint.copy(alpha = alpha * 2f),
                topLeft = Offset(0f, h * i / 6f),
                size = Size(w, h / 6f)
            )
        }

        // Element-specific effects
        when (monsterElement) {
            Element.FIRE -> {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color(0xFFFF6B35).copy(alpha = 0.15f),
                        0.3f to Color.Transparent
                    ),
                    size = Size(w, h * 0.5f)
                )
            }
            Element.WATER -> {
                for (i in 0..2) {
                    val by = h * (0.2f + i * 0.15f) + sin(parallaxOffset + i * 2f) * 10f
                    drawRect(
                        Color(0xFF42A5F5).copy(alpha = 0.06f),
                        topLeft = Offset(0f, by),
                        size = Size(w, 4f)
                    )
                }
            }
            Element.DARK, Element.SHADOW -> {
                drawRect(Color(0xFF4A148C).copy(alpha = 0.08f))
            }
            Element.LIGHT -> {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFD740).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(w * 0.5f, 0f),
                        radius = w * 0.6f
                    )
                )
            }
            Element.ELECTRIC -> {
                if (sin(parallaxOffset * 17f) > 0.92f) {
                    drawRect(Color.White.copy(alpha = 0.15f))
                }
            }
            else -> {}
        }

        // Stars
        val starCount = if (bossFight) 50 else 30
        val starSeed = 137.5f
        repeat(starCount) {
            val sx = (it * starSeed + 50f * sin(it * 1.3f)) % w
            val sy = (it * 97.3f + parallaxOffset * 20f) % (h * 0.5f)
            val starSize = 1f + (it % 3)
            val twinkle = 0.3f + 0.3f * sin(it * 0.7f + parallaxOffset * 2f)
            drawCircle(
                color = Color.White.copy(alpha = twinkle),
                radius = starSize,
                center = Offset(sx, sy)
            )
        }

        // Clouds
        val cloudAlpha = if (bossFight) 0.08f else 0.12f
        for (i in 0..3) {
            val cx = (w * 0.2f * i + parallaxOffset * 30f * (1f + i * 0.3f)) % (w + 100f) - 50f
            val cy = h * (0.08f + i * 0.07f) + sin(parallaxOffset + i * 1.5f) * 8f
            val cw = 60f + i * 20f
            drawCircle(Color.White.copy(alpha = cloudAlpha), cw * 0.4f, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = cloudAlpha * 0.7f), cw * 0.3f, Offset(cx + cw * 0.3f, cy - 5f))
            drawCircle(Color.White.copy(alpha = cloudAlpha * 0.5f), cw * 0.25f, Offset(cx - cw * 0.25f, cy + 3f))
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

        // Mid-layer ruins
        val midX = parallaxOffset * 30f
        for (i in 0..3) {
            val rx = (w * 0.3f * (i + 1) + midX * 0.3f * (1f + i * 0.5f)) % w
            val rw = 12f + i * 4f
            val rh = 40f + i * 10f
            val rAlpha = 0.15f + i * 0.05f
            drawRect(
                groundColor.copy(alpha = rAlpha),
                Offset(rx - rw / 2f, groundY - rh),
                Size(rw, rh)
            )
            drawRect(
                groundColor.copy(alpha = rAlpha * 1.5f),
                Offset(rx - rw * 0.7f, groundY - rh - 4f),
                Size(rw * 1.4f, 5f)
            )
        }

        // Far mountain silhouette
        val mountPath = Path().apply {
            moveTo(0f, groundY)
            for (x in 0..(w.toInt()) step 30) {
                val my = groundY - 40f - 30f * sin(x * 0.008f + parallaxOffset * 0.3f)
                lineTo(x.toFloat(), my)
            }
            lineTo(w, groundY)
            close()
        }
        drawPath(mountPath, Color(0xFF162240).copy(alpha = 0.5f))

        // Decorative pillars
        drawPillar(w * 0.15f, groundY - 10f, 20f, 60f, elementTint.copy(alpha = 0.3f))
        drawPillar(w * 0.85f, groundY - 10f, 20f, 60f, elementTint.copy(alpha = 0.3f))

        // Boss upgrades
        if (bossFight) {
            drawPillar(w * 0.5f, groundY - 15f, 30f, 80f, Color(0xFFFF4444).copy(alpha = 0.4f))
            // Ground cracks with glow
            val crackPath = Path().apply {
                moveTo(0f, groundY + 5f)
                for (x in 0..(w.toInt()) step 10) {
                    val cy2 = groundY + 5f + 3f * sin(x * 0.05f + parallaxOffset * 2f)
                    lineTo(x.toFloat(), cy2)
                }
            }
            drawPath(crackPath, Color(0xFFFF4444).copy(alpha = 0.3f + 0.2f * sin(parallaxOffset * 3f)),
                style = Stroke(width = 2f))
            // Floating embers
            for (i in 0..20) {
                val ex = (i * 47f + parallaxOffset * 50f) % w
                val ey = groundY - (i * 13f + parallaxOffset * 20f * sin(i.toFloat())) % (h * 0.5f)
                drawCircle(
                    Color(0xFFFF6B35).copy(alpha = 0.3f + 0.2f * sin(parallaxOffset * 5f + i.toFloat())),
                    radius = 2f + sin(i.toFloat()) * 1f,
                    center = Offset(ex, ey)
                )
            }
        }

        // Fog near ground
        for (i in 0..2) {
            val fogX = parallaxOffset * 15f * (1f + i * 0.5f)
            drawRect(
                Color(0xFFB2EBF2).copy(alpha = 0.03f),
                topLeft = Offset(fogX % w - 50f, groundY - 10f + i * 15f),
                size = Size(100f + i * 40f, 8f)
            )
        }

        // Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x80000000)),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.8f
            )
        )
    }
}

private fun DrawScope.drawPillar(x: Float, baseY: Float, width: Float, height: Float, color: Color) {
    drawRect(color, Offset(x - width / 2f, baseY - height), Size(width, height))
    drawRect(
        color.copy(alpha = color.alpha * 1.5f),
        Offset(x - width * 0.7f, baseY - height - 5f),
        Size(width * 1.4f, 8f)
    )
}

// ─── Animated Hero Sprite ──────────────────────────────────────────────

@Composable
fun HeroSprite(
    modifier: Modifier = Modifier,
    heroName: String,
    elementColor: Color,
    isActive: Boolean = true,
    isDamaged: Boolean = false,
    flashAlpha: Float = 0f,
    animState: SpriteAnimState = SpriteAnimState()
) {
    val tint = if (!isActive) elementColor.copy(alpha = 0.4f) else elementColor
    val flash = if (isDamaged) Color.Red.copy(alpha = flashAlpha) else Color.Transparent

    Canvas(modifier = modifier) {
        val cx = size.width / 2f + animState.offsetX
        val cy = size.height * 0.6f + animState.offsetY
        val s = size.minDimension * 0.2f * animState.scale

        val idleBob = if (animState.state == SpriteState.IDLE) {
            sin(animState.stateTime * 1.5f + heroName.hashCode() * 0.1f) * 4f
        } else 0f

        val drawCx = cx
        val drawCy = cy + idleBob

        // Glow aura
        if (isActive) {
            drawCircle(
                color = elementColor.copy(alpha = 0.12f + 0.05f * sin(animState.stateTime * 1.2f)),
                radius = s * 1.6f,
                center = Offset(drawCx, drawCy)
            )
        }

        drawSilhouette(drawCx, drawCy, s, heroName, tint.copy(alpha = animState.alpha))

        if (isDamaged && flashAlpha > 0f) {
            drawRect(flash, alpha = flashAlpha * animState.alpha)
        }
    }
}

// ─── Animated Monster Sprite ───────────────────────────────────────────

@Composable
fun MonsterSprite(
    modifier: Modifier = Modifier,
    monsterName: String,
    elementColor: Color,
    isBoss: Boolean = false,
    isDamaged: Boolean = false,
    flashAlpha: Float = 0f,
    bossPulse: Float = 0f,
    animState: SpriteAnimState = SpriteAnimState()
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f + animState.offsetX
        val cy = size.height * 0.42f + animState.offsetY
        val s = size.minDimension * 0.3f * (if (isBoss) 1.3f else 1f) * animState.scale

        val idleBob = if (animState.state == SpriteState.IDLE) {
            sin(animState.stateTime * 1.5f + monsterName.hashCode() * 0.1f) * 3f
        } else 0f

        val drawCx = cx
        val drawCy = cy + idleBob

        // Boss aura
        if (isBoss && bossPulse > 0f) {
            val auraRadius = s * (1.5f + 0.3f * sin(bossPulse * PI.toFloat()))
            drawCircle(
                color = elementColor.copy(alpha = 0.15f),
                radius = auraRadius,
                center = Offset(drawCx, drawCy)
            )
            drawCircle(
                color = Color(0xFFFF4444).copy(alpha = 0.08f + 0.05f * sin(bossPulse * 2f)),
                radius = auraRadius * 1.4f,
                center = Offset(drawCx, drawCy)
            )
        }

        drawMonsterShape(drawCx, drawCy, s, monsterName, elementColor.copy(alpha = animState.alpha))

        if (isDamaged && flashAlpha > 0f) {
            drawRect(Color.Red.copy(alpha = flashAlpha * animState.alpha))
        }
    }
}

// ─── Silhouette Drawers (unchanged from original) ──────────────────────

private fun DrawScope.drawSilhouette(cx: Float, cy: Float, s: Float, name: String, tint: Color) {
    val path = Path()

    when (name) {
        "Shanti" -> {
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
            drawCircle(tint.copy(alpha = 0.3f), s * 0.15f, Offset(cx, cy - s * 0.3f))
        }
        "Santosha" -> {
            path.moveTo(cx - s * 0.5f, cy - s * 0.5f)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.9f, cx - s * 0.2f, cy - s * 0.9f)
            path.quadraticBezierTo(cx, cy - s * 1.0f, cx + s * 0.2f, cy - s * 0.9f)
            path.quadraticBezierTo(cx + s * 0.5f, cy - s * 0.9f, cx + s * 0.5f, cy - s * 0.5f)
            path.quadraticBezierTo(cx + s * 0.6f, cy, cx + s * 0.4f, cy + s * 0.5f)
            path.lineTo(cx - s * 0.4f, cy + s * 0.5f)
            path.quadraticBezierTo(cx - s * 0.6f, cy, cx - s * 0.5f, cy - s * 0.5f)
            path.close()
            drawPath(path, tint)
            drawCircle(tint.copy(alpha = 0.4f), s * 0.2f, Offset(cx, cy - s * 0.1f))
        }
        "Virya" -> {
            path.moveTo(cx - s * 0.3f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.1f, cy - s * 0.2f)
            path.quadraticBezierTo(cx, cy - s * 0.7f, cx + s * 0.15f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.3f, cy + s * 0.1f)
            path.quadraticBezierTo(cx + s * 0.4f, cy + s * 0.4f, cx + s * 0.2f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.3f, cy + s * 0.6f)
            path.close()
            drawPath(path, tint)
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
            path.moveTo(cx - s * 0.2f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.25f, cy - s * 0.3f)
            path.quadraticBezierTo(cx - s * 0.2f, cy - s * 0.8f, cx, cy - s * 0.9f)
            path.quadraticBezierTo(cx + s * 0.2f, cy - s * 0.8f, cx + s * 0.25f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.2f, cy + s * 0.6f)
            path.close()
            drawPath(path, tint)
            val lancePath = Path().apply {
                moveTo(cx, cy - s * 0.9f)
                lineTo(cx + s * 0.03f, cy - s * 1.2f)
                lineTo(cx - s * 0.03f, cy - s * 1.2f)
                close()
            }
            drawPath(lancePath, tint.copy(alpha = 0.6f))
        }
        "Maitri" -> {
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
            path.moveTo(cx - s * 0.4f, cy + s * 0.2f)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.1f, cx - s * 0.2f, cy - s * 0.2f)
            path.quadraticBezierTo(cx, cy - s * 0.6f, cx + s * 0.2f, cy - s * 0.2f)
            path.quadraticBezierTo(cx + s * 0.5f, cy - s * 0.1f, cx + s * 0.4f, cy + s * 0.2f)
            path.lineTo(cx - s * 0.4f, cy + s * 0.2f)
            path.close()
            drawPath(path, tint)
        }
        name.contains("Chinta") || name.contains("Anxiety") -> {
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
            drawCircle(tint, s * 0.4f, Offset(cx, cy + s * 0.1f))
            drawCircle(tint.copy(alpha = 0.6f), s * 0.3f, Offset(cx - s * 0.2f, cy - s * 0.1f))
            drawCircle(tint.copy(alpha = 0.6f), s * 0.3f, Offset(cx + s * 0.2f, cy - s * 0.1f))
        }
        name.contains("Matsarya") || name.contains("Envy") -> {
            path.moveTo(cx - s * 0.4f, cy + s * 0.3f)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.2f, cx - s * 0.2f, cy - s * 0.5f)
            path.quadraticBezierTo(cx, cy - s * 0.7f, cx + s * 0.2f, cy - s * 0.4f)
            path.quadraticBezierTo(cx + s * 0.5f, cy, cx + s * 0.3f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint)
        }
        name.contains("Krodha") || name.contains("Anger") -> {
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
            drawCircle(tint.copy(alpha = 0.3f), s * 0.2f, Offset(cx, cy - s * 0.1f))
        }
        name.contains("Ahankara") || name.contains("Ego") -> {
            path.moveTo(cx - s * 0.5f, cy + s * 0.4f)
            path.lineTo(cx - s * 0.45f, cy - s * 0.3f)
            path.quadraticBezierTo(cx - s * 0.3f, cy - s * 0.7f, cx, cy - s * 0.8f)
            path.quadraticBezierTo(cx + s * 0.3f, cy - s * 0.7f, cx + s * 0.45f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.5f, cy + s * 0.4f)
            path.close()
            drawPath(path, tint)
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
            drawCircle(tint, s * 0.5f, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = 0.2f), s * 0.3f, Offset(cx, cy))
            for (i in 0..7) {
                val angle = i * PI.toFloat() / 4f
                val rx = cx + s * 0.45f * cos(angle)
                val ry = cy + s * 0.45f * sin(angle)
                drawLine(tint.copy(alpha = 0.6f), Offset(cx, cy), Offset(rx, ry), 2f)
            }
            drawCircle(Color.White.copy(alpha = 0.3f), s * 0.1f, Offset(cx, cy))
        }
        else -> {
            path.moveTo(cx - s * 0.4f, cy + s * 0.3f)
            path.quadraticBezierTo(cx - s * 0.5f, cy - s * 0.3f, cx - s * 0.1f, cy - s * 0.5f)
            path.quadraticBezierTo(cx, cy - s * 0.7f, cx + s * 0.1f, cy - s * 0.5f)
            path.quadraticBezierTo(cx + s * 0.5f, cy - s * 0.3f, cx + s * 0.4f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint)
        }
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        a.red + (b.red - a.red) * clamped,
        a.green + (b.green - a.green) * clamped,
        a.blue + (b.blue - a.blue) * clamped,
        a.alpha + (b.alpha - a.alpha) * clamped
    )
}
