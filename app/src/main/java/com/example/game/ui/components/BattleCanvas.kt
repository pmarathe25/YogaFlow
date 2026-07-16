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
import kotlin.math.*

// ─── Sprite Animation State ───────────────────────────────────────────

enum class SpriteState { IDLE, ATTACKING, HIT, DYING, DEAD }

data class SpriteAnimState(
    val state: SpriteState = SpriteState.IDLE,
    val stateTime: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f
)

// ─── Background ────────────────────────────────────────────────────────

@Composable
fun BattleBackground(
    modifier: Modifier = Modifier,
    parallaxOffset: Float = 0f,
    bossFight: Boolean = false,
    monsterElement: Element = Element.NEUTRAL,
    elementTint: Color = Color.Transparent
) {
    val skyBase = if (bossFight) Color(0xFF2A0055) else Color(0xFF1B263B) // BRIGHTER
    val groundBase = if (bossFight) Color(0xFF3D2B5E) else Color(0xFF243447) // BRIGHTER

    val tintStrength = if (bossFight) 0.3f else 0.18f
    val skyColor = lerp(skyBase, elementTint, tintStrength)
    val groundColor = lerp(groundBase, elementTint, tintStrength * 0.5f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sky gradient
        drawRect(skyColor)
        for (i in 0..5) {
            val alpha = 0.08f * (1f - i / 6f)
            drawRect(
                color = elementTint.copy(alpha = alpha * 2f),
                topLeft = Offset(0f, h * i / 6f),
                size = Size(w, h / 6f)
            )
        }

        // Element-specific effects
        when (monsterElement) {
            Element.FIRE -> {
                // Ember glow
                drawRect(
                    Brush.verticalGradient(
                        0f to Color(0xFFFF6B35).copy(alpha = 0.25f),
                        0.4f to Color.Transparent
                    ),
                    size = Size(w, h * 0.5f)
                )
                // Lava glow floor cracks
                for (i in 0..6) {
                    val cx = w * (0.1f + i * 0.15f) + sin(parallaxOffset + i * 1.3f) * 20f
                    val cy = h * 0.62f + 12f * sin(i * 2.1f + parallaxOffset * 0.5f)
                    drawCircle(Color(0xFFFF4500).copy(alpha = 0.2f + 0.1f * sin(parallaxOffset * 2f + i)), 8f + 4f * sin(parallaxOffset + i), Offset(cx, cy))
                }
                // Ember particles floating up
                val emberAlpha = 0.3f + 0.2f * sin(parallaxOffset * 1.5f)
                for (i in 0..8) {
                    val ex = (w * 0.05f + i * w * 0.11f + parallaxOffset * 15f * (1f + i * 0.2f)) % w
                    val ey = h * 0.6f - (i * 20f + parallaxOffset * 30f) % (h * 0.5f)
                    drawCircle(Color(0xFFFF9800).copy(alpha = emberAlpha * 0.5f), 2f + sin(parallaxOffset + i) * 1f, Offset(ex, ey))
                }
            }
            Element.WATER -> {
                // Rain effect
                val rainAlpha = 0.2f + 0.1f * sin(parallaxOffset)
                for (i in 0..12) {
                    val rx = (w * 0.08f * i + parallaxOffset * 40f * (1f + i * 0.15f)) % w
                    val ry = (i * 37f + parallaxOffset * 50f) % h
                    drawLine(Color(0xFF42A5F5).copy(alpha = rainAlpha * (0.5f + i * 0.04f)),
                        Offset(rx, ry), Offset(rx - 6f, ry + 18f), 2f)
                }
                // Rippling water bands
                for (i in 0..4) {
                    val by = h * (0.15f + i * 0.12f) + sin(parallaxOffset + i * 2f) * 15f
                    drawRect(
                        Color(0xFF42A5F5).copy(alpha = 0.1f),
                        topLeft = Offset(0f, by),
                        size = Size(w, 6f)
                    )
                    drawRect(
                        Color(0xFFB3E5FC).copy(alpha = 0.05f),
                        topLeft = Offset(0f, by + 10f + 5f * sin(parallaxOffset * 1.5f + i)),
                        size = Size(w, 3f)
                    )
                }
            }
            Element.EARTH -> {
                drawRect(
                    Brush.verticalGradient(
                        0.5f to Color(0xFF8D6E63).copy(alpha = 0.2f),
                        1f to Color.Transparent
                    ),
                    size = Size(w, h * 0.5f)
                )
                // Rocky terrain details
                for (i in 0..5) {
                    val rx = w * (0.1f + i * 0.18f) + sin(parallaxOffset * 0.3f + i) * 12f
                    val ry = h * 0.6f + 8f * sin(i * 1.7f)
                    drawCircle(Color(0xFF6D4C41).copy(alpha = 0.2f), 6f + i * 2f, Offset(rx, ry))
                    drawCircle(Color(0xFFA1887F).copy(alpha = 0.1f), 3f + i * 1f, Offset(rx + 5f, ry - 4f))
                }
                // Vine pillars
                for (i in 0..3) {
                    val vx = w * (0.15f + i * 0.28f)
                    for (j in 0..4) {
                        val theta = parallaxOffset * 0.5f + i + j * 0.3f
                        val vxo = 5f * sin(theta)
                        val vyo = j * 18f
                        drawCircle(Color(0xFF4CAF50).copy(alpha = 0.15f), 4f, Offset(vx + vxo, h * 0.6f - vyo))
                    }
                }
            }
            Element.AIR -> {
                val wispAlpha = 0.1f + 0.05f * sin(parallaxOffset * 0.5f)
                for (i in 0..6) {
                    val wx = (w * 0.1f * i + parallaxOffset * 25f * (1f + i * 0.3f)) % (w + 150f) - 75f
                    val wy = h * (0.05f + i * 0.07f) + sin(parallaxOffset + i * 1.2f) * 20f
                    drawRect(
                        Color.White.copy(alpha = wispAlpha * (0.6f + i * 0.1f)),
                        topLeft = Offset(wx, wy),
                        size = Size(80f + i * 20f, 3f)
                    )
                }
                // Wind swirl particles
                val swirlAlpha = 0.15f + 0.1f * sin(parallaxOffset * 0.7f)
                for (i in 0..5) {
                    val angle = parallaxOffset * 0.4f + i * 1.05f
                    val sx = w * 0.5f + w * 0.35f * cos(angle)
                    val sy = h * 0.3f + h * 0.2f * sin(angle * 0.7f)
                    drawCircle(Color.White.copy(alpha = swirlAlpha), 3f + 2f * sin(angle), Offset(sx, sy))
                }
                // Cloud platform silhouettes
                for (i in 0..2) {
                    val cx = (w * 0.2f + i * w * 0.35f + parallaxOffset * 20f * (i + 1)) % (w + 100f) - 50f
                    val cy = h * 0.48f + i * 15f + 10f * sin(parallaxOffset + i)
                    drawCircle(Color.White.copy(alpha = 0.08f), 30f + i * 10f, Offset(cx, cy))
                    drawCircle(Color.White.copy(alpha = 0.05f), 20f + i * 8f, Offset(cx + 25f, cy - 5f))
                }
            }
            Element.DARK, Element.SHADOW -> {
                drawRect(Color(0xFF4A148C).copy(alpha = 0.12f))
                // Purple mist
                for (i in 0..4) {
                    val mx = (w * 0.1f + i * w * 0.2f + parallaxOffset * 10f * (i + 1)) % (w + 100f) - 50f
                    val my = h * 0.5f + i * 20f + 15f * sin(parallaxOffset * 0.5f + i * 1.2f)
                    drawCircle(Color(0xFF7B1FA2).copy(alpha = 0.08f + 0.04f * sin(parallaxOffset + i)), 30f + i * 8f, Offset(mx, my))
                }
                // Floating void orbs
                for (i in 0..4) {
                    val ox = (w * 0.1f + i * w * 0.22f + parallaxOffset * 8f * (i + 1)) % w
                    val oy = h * 0.2f + 30f * sin(parallaxOffset * 0.6f + i * 1.5f)
                    val orbAlpha = 0.25f + 0.15f * sin(parallaxOffset * 1.5f + i * 0.8f)
                    drawCircle(Color(0xFFCE93D8).copy(alpha = orbAlpha), 4f + 2f * sin(parallaxOffset + i), Offset(ox, oy))
                    drawCircle(Color(0xFFE1BEE7).copy(alpha = orbAlpha * 0.5f), 6f + 3f * sin(parallaxOffset + i), Offset(ox, oy), style = Stroke(width = 1f))
                }
            }
            Element.LIGHT -> {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFD740).copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(w * 0.5f, -100f),
                        radius = w * 0.8f
                    )
                )
                // Radiant beams from above
                for (i in 0..4) {
                    val beamAngle = parallaxOffset * 0.3f + i * 0.8f
                    val bx = w * 0.5f + w * 0.4f * sin(beamAngle)
                    val bw = 20f + 10f * sin(beamAngle)
                    val beamAlpha = 0.06f + 0.04f * sin(beamAngle * 1.5f)
                    drawRect(
                        Color(0xFFFFF176).copy(alpha = beamAlpha),
                        topLeft = Offset(bx - bw / 2f, 0f),
                        size = Size(bw, h * 0.6f)
                    )
                }
                // Golden fog
                for (i in 0..3) {
                    val fogX = (parallaxOffset * 15f * (i + 1) + w * 0.1f * i) % (w + 100f) - 50f
                    val fogY = h * 0.55f + i * 12f
                    drawCircle(Color(0xFFFFF176).copy(alpha = 0.04f + 0.02f * sin(parallaxOffset + i)), 40f + i * 12f, Offset(fogX, fogY))
                }
            }
            Element.ELECTRIC -> {
                // Lightning bolts
                if (sin(parallaxOffset * 21f) > 0.88f) {
                    drawRect(Color.White.copy(alpha = 0.25f))
                    for (i in 0..2) {
                        val lx = w * (0.2f + i * 0.3f)
                        val path = Path().apply {
                            moveTo(lx, 0f)
                            var px = lx
                            var py = 0f
                            val seed = i * 7
                            repeat(6) {
                                px += 15f * sin(parallaxOffset * 5f + seed + it * 3f)
                                py += (h * 0.1f)
                                lineTo(px, py)
                            }
                        }
                        drawPath(path, Color(0xFFFFEB3B).copy(alpha = 0.4f), style = Stroke(width = 3f))
                    }
                }
                // Ambient electric crackle
                val crackleAlpha = 0.1f + 0.05f * sin(parallaxOffset * 13f)
                for (i in 0..3) {
                    val ex = (w * 0.1f + i * w * 0.28f) % w
                    val ey = h * 0.3f + 20f * sin(parallaxOffset * 2f + i * 2f)
                    drawLine(Color(0xFFFFEB3B).copy(alpha = crackleAlpha), Offset(ex, ey), Offset(ex + 10f * sin(parallaxOffset * 5f + i), ey - 15f), 2f)
                }
            }
            else -> {}
        }

        // Stars
        val starCount = if (bossFight) 60 else 40
        val starSeed = 137.5f
        repeat(starCount) {
            val sx = (it * starSeed + 60f * sin(it * 1.3f)) % w
            val sy = (it * 97.3f + parallaxOffset * 25f) % (h * 0.55f)
            val starSize = 1.2f + (it % 3)
            val twinkle = 0.4f + 0.4f * sin(it * 0.7f + parallaxOffset * 2.5f)
            drawCircle(
                color = Color.White.copy(alpha = twinkle),
                radius = starSize,
                center = Offset(sx, sy)
            )
        }

        // Clouds
        val cloudAlpha = if (bossFight) 0.12f else 0.18f
        for (i in 0..4) {
            val cx = (w * 0.15f * i + parallaxOffset * 35f * (1f + i * 0.3f)) % (w + 200f) - 100f
            val cy = h * (0.05f + i * 0.06f) + sin(parallaxOffset + i * 1.5f) * 12f
            val cw = 80f + i * 25f
            drawCircle(Color.White.copy(alpha = cloudAlpha), cw * 0.4f, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = cloudAlpha * 0.7f), cw * 0.3f, Offset(cx + cw * 0.3f, cy - 8f))
            drawCircle(Color.White.copy(alpha = cloudAlpha * 0.5f), cw * 0.25f, Offset(cx - cw * 0.25f, cy + 5f))
        }

        // Ground with parallax
        val groundY = h * 0.62f
        val groundPath = Path().apply {
            moveTo(0f, groundY)
            for (x in 0..(w.toInt()) step 15) {
                val y = groundY + 35f * sin(x * 0.015f + parallaxOffset)
                lineTo(x.toFloat(), y)
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(groundPath, groundColor)

        // Mid-layer ruins
        val midX = parallaxOffset * 35f
        for (i in 0..4) {
            val rx = (w * 0.25f * (i + 1) + midX * 0.4f * (1f + i * 0.5f)) % w
            val rw = 15f + i * 5f
            val rh = 50f + i * 15f
            val rAlpha = 0.2f + i * 0.06f
            drawRect(
                groundColor.copy(alpha = rAlpha),
                Offset(rx - rw / 2f, groundY - rh),
                Size(rw, rh)
            )
            drawRect(
                groundColor.copy(alpha = rAlpha * 1.6f),
                Offset(rx - rw * 0.75f, groundY - rh - 6f),
                Size(rw * 1.5f, 7f)
            )
        }

        // Far mountain silhouette
        val mountPath = Path().apply {
            moveTo(0f, groundY)
            for (x in 0..(w.toInt()) step 25) {
                val my = groundY - 60f - 40f * sin(x * 0.007f + parallaxOffset * 0.4f)
                lineTo(x.toFloat(), my)
            }
            lineTo(w, groundY)
            close()
        }
        drawPath(mountPath, Color(0xFF1E2F5A).copy(alpha = 0.6f))

        // Floating Runes
        repeat(8) {
            val rx = (it * 150f + parallaxOffset * 10f) % w
            val ry = groundY - 100f - 50f * sin(it * 1.5f + parallaxOffset * 0.5f)
            val rSize = 5f + sin(it.toFloat()) * 2f
            drawCircle(
                color = elementTint.copy(alpha = 0.4f + 0.2f * sin(parallaxOffset + it)),
                radius = rSize,
                center = Offset(rx, ry),
                style = Stroke(width = 1.5f)
            )
        }

        // Decorative pillars
        drawPillar(w * 0.1f, groundY - 15f, 25f, 80f, elementTint.copy(alpha = 0.4f))
        drawPillar(w * 0.9f, groundY - 15f, 25f, 80f, elementTint.copy(alpha = 0.4f))

        // Boss upgrades
        if (bossFight) {
            drawPillar(w * 0.5f, groundY - 20f, 40f, 110f, Color(0xFFFF5555).copy(alpha = 0.5f))
            // Ground cracks with glow
            val crackPath = Path().apply {
                moveTo(0f, groundY + 8f)
                for (x in 0..(w.toInt()) step 8) {
                    val cy2 = groundY + 8f + 5f * sin(x * 0.06f + parallaxOffset * 2.5f)
                    lineTo(x.toFloat(), cy2)
                }
            }
            drawPath(crackPath, Color(0xFFFF4444).copy(alpha = 0.4f + 0.25f * sin(parallaxOffset * 4f)),
                style = Stroke(width = 3f))
        }

        // Fog near ground
        for (i in 0..3) {
            val fogX = parallaxOffset * 20f * (1f + i * 0.5f)
            drawRect(
                Color(0xFFB2EBF2).copy(alpha = 0.05f),
                topLeft = Offset(fogX % w - 70f, groundY - 15f + i * 18f),
                size = Size(150f + i * 50f, 10f)
            )
        }

        // Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x66000000)), // LESS DARK
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.9f
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

// ─── Silhouette Drawers (unchanged from original) ──────────────────────

fun DrawScope.drawSilhouette(cx: Float, cy: Float, s: Float, heroId: Int, tint: Color, primaryColor: Color? = null, secondaryColor: Color? = null) {
    val path = Path()

    when (heroId) {
        1 -> {
            // 1. Watery Halo (Aura)
            drawCircle((primaryColor ?: tint).copy(alpha = 0.15f), s * 0.45f, Offset(cx, cy - s * 0.7f))
            drawCircle((primaryColor ?: tint).copy(alpha = 0.1f), s * 0.55f, Offset(cx, cy - s * 0.7f))

            // 2. Flowing Gown (Body)
            path.reset()
            path.moveTo(cx - s * 0.15f, cy - s * 0.5f)
            path.cubicTo(cx - s * 0.4f, cy - s * 0.1f, cx - s * 0.5f, cy + s * 0.4f, cx - s * 0.45f, cy + s * 0.7f)
            path.lineTo(cx + s * 0.45f, cy + s * 0.7f)
            path.cubicTo(cx + s * 0.5f, cy + s * 0.4f, cx + s * 0.4f, cy - s * 0.1f, cx + s * 0.15f, cy - s * 0.5f)
            path.close()
            drawPath(path, (primaryColor ?: tint).copy(alpha = 0.8f))

            // 3. Hair (Flowing Waves)
            val hairPath = Path()
            hairPath.moveTo(cx - s * 0.18f, cy - s * 0.85f)
            hairPath.cubicTo(cx - s * 0.35f, cy - s * 0.7f, cx - s * 0.3f, cy - s * 0.4f, cx - s * 0.4f, cy - s * 0.1f)
            hairPath.moveTo(cx + s * 0.18f, cy - s * 0.85f)
            hairPath.cubicTo(cx + s * 0.35f, cy - s * 0.7f, cx + s * 0.3f, cy - s * 0.4f, cx + s * 0.4f, cy - s * 0.1f)
            drawPath(hairPath, secondaryColor ?: tint, style = Stroke(width = 2f * s / 50f))

            // 4. Head & Face
            drawCircle((primaryColor ?: tint).copy(alpha = 0.9f), s * 0.22f, Offset(cx, cy - s * 0.72f))

            // 5. Hands in Prayer (Anjali Mudra)
            val handPath = Path()
            handPath.moveTo(cx, cy - s * 0.4f)
            handPath.lineTo(cx - s * 0.08f, cy - s * 0.25f)
            handPath.lineTo(cx, cy - s * 0.15f)
            handPath.lineTo(cx + s * 0.08f, cy - s * 0.25f)
            handPath.close()
            drawPath(handPath, secondaryColor ?: tint, style = Stroke(width = 1.5f * s / 50f))

            // 6. Prayer Beads (Mala)
            for (i in 0..5) {
                val bx = cx - s * 0.1f + i * s * 0.04f
                val by = cy - s * 0.35f + sin(i * 0.8f) * s * 0.02f
                drawCircle(Color.White.copy(alpha = 0.6f), s * 0.025f, Offset(bx, by))
            }
        }
        2 -> {
            // 1. Earthy Shield Base
            path.reset()
            path.moveTo(cx - s * 0.55f, cy - s * 0.4f)
            path.lineTo(cx - s * 0.45f, cy + s * 0.5f)
            path.quadraticTo(cx, cy + s * 0.75f, cx + s * 0.45f, cy + s * 0.5f)
            path.lineTo(cx + s * 0.55f, cy - s * 0.4f)
            path.quadraticTo(cx, cy - s * 0.55f, cx - s * 0.55f, cy - s * 0.4f)
            path.close()
            drawPath(path, (primaryColor ?: tint).copy(alpha = 0.7f))

            // 2. Inner Shield Decoration
            drawPath(path, secondaryColor ?: tint, style = Stroke(width = 3f * s / 50f))

            // 3. Sturdy Figure
            drawCircle((primaryColor ?: tint).copy(alpha = 0.9f), s * 0.25f, Offset(cx, cy - s * 0.7f))
            val bodyPath = Path()
            bodyPath.moveTo(cx - s * 0.25f, cy - s * 0.5f)
            bodyPath.lineTo(cx - s * 0.35f, cy + s * 0.2f)
            bodyPath.lineTo(cx + s * 0.35f, cy + s * 0.2f)
            bodyPath.lineTo(cx + s * 0.25f, cy - s * 0.5f)
            bodyPath.close()
            drawPath(bodyPath, (primaryColor ?: tint).copy(alpha = 0.85f))

            // 4. Grounding Cracks
            val crackPath = Path()
            crackPath.moveTo(cx - s * 0.4f, cy + s * 0.6f)
            crackPath.lineTo(cx - s * 0.6f, cy + s * 0.8f)
            crackPath.moveTo(cx + s * 0.4f, cy + s * 0.6f)
            crackPath.lineTo(cx + s * 0.6f, cy + s * 0.8f)
            drawPath(crackPath, (secondaryColor ?: tint).copy(alpha = 0.5f), style = Stroke(width = 2f))
        }
        3 -> {
            // 1. Flame Aura
            for (i in 0..12) {
                val angle = i * 2f * PI.toFloat() / 12f
                val fx = cx + s * 0.4f * cos(angle)
                val fy = cy + s * 0.4f * sin(angle) - s * 0.2f
                drawCircle((primaryColor ?: tint).copy(alpha = 0.2f), s * 0.15f, Offset(fx, fy))
            }

            // 2. Dynamic Pose
            path.reset()
            path.moveTo(cx - s * 0.1f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.3f, cy - s * 0.1f)
            path.lineTo(cx, cy - s * 0.5f)
            path.lineTo(cx + s * 0.4f, cy + s * 0.1f)
            path.lineTo(cx + s * 0.2f, cy + s * 0.6f)
            path.close()
            drawPath(path, (primaryColor ?: tint).copy(alpha = 0.9f))

            // 3. Flame Hair
            val firePath = Path()
            firePath.moveTo(cx - s * 0.15f, cy - s * 0.5f)
            firePath.quadraticTo(cx - s * 0.3f, cy - s * 0.9f, cx, cy - s * 1.1f)
            firePath.quadraticTo(cx + s * 0.3f, cy - s * 0.9f, cx + s * 0.15f, cy - s * 0.5f)
            drawPath(firePath, secondaryColor ?: tint)

            // 4. Glowing Core
            drawCircle(Color.White.copy(alpha = 0.7f), s * 0.08f, Offset(cx, cy - s * 0.1f))
        }
        4 -> {
            // 1. Radiant Cape
            path.reset()
            path.moveTo(cx - s * 0.1f, cy - s * 0.6f)
            path.lineTo(cx - s * 0.5f, cy + s * 0.5f)
            path.quadraticTo(cx, cy + s * 0.65f, cx + s * 0.5f, cy + s * 0.5f)
            path.lineTo(cx + s * 0.1f, cy - s * 0.6f)
            path.close()
            drawPath(path, (primaryColor ?: tint).copy(alpha = 0.4f))

            // 2. Noble Figure
            drawCircle((primaryColor ?: tint).copy(alpha = 0.95f), s * 0.2f, Offset(cx, cy - s * 0.75f))
            val armorPath = Path()
            armorPath.moveTo(cx - s * 0.25f, cy - s * 0.55f)
            armorPath.lineTo(cx - s * 0.2f, cy + s * 0.4f)
            armorPath.lineTo(cx + s * 0.2f, cy + s * 0.4f)
            armorPath.lineTo(cx + s * 0.25f, cy - s * 0.55f)
            armorPath.close()
            drawPath(armorPath, primaryColor ?: tint)

            // 3. Battle Standard (Lance)
            val lancePath = Path()
            lancePath.moveTo(cx + s * 0.3f, cy + s * 0.5f)
            lancePath.lineTo(cx + s * 0.3f, cy - s * 0.9f)
            drawPath(lancePath, secondaryColor ?: tint, style = Stroke(width = 3f))
            val flagPath = Path()
            flagPath.moveTo(cx + s * 0.3f, cy - s * 0.9f)
            flagPath.lineTo(cx + s * 0.6f, cy - s * 0.8f)
            flagPath.lineTo(cx + s * 0.3f, cy - s * 0.7f)
            drawPath(flagPath, (secondaryColor ?: tint).copy(alpha = 0.6f))
        }
        5 -> {
            // 1. Gentle Winds (Spinning paths)
            for (i in 0..2) {
                val windPath = Path()
                val offset = i * 40f
                windPath.moveTo(cx - s * 0.6f, cy - s * 0.3f + offset)
                windPath.quadraticTo(cx, cy - s * 0.8f + offset, cx + s * 0.6f, cy - s * 0.3f + offset)
                drawPath(windPath, (secondaryColor ?: tint).copy(alpha = 0.2f), style = Stroke(width = 4f, cap = StrokeCap.Round))
            }

            // 2. Ascended Form
            path.reset()
            path.moveTo(cx - s * 0.3f, cy)
            path.quadraticTo(cx, cy - s * 0.9f, cx + s * 0.3f, cy)
            path.lineTo(cx + s * 0.2f, cy + s * 0.5f)
            path.quadraticTo(cx, cy + s * 0.7f, cx - s * 0.2f, cy + s * 0.5f)
            path.close()
            drawPath(path, (primaryColor ?: tint).copy(alpha = 0.8f))

            // 3. Heart Emblem
            val heartPath = Path()
            heartPath.moveTo(cx, cy - s * 0.1f)
            heartPath.cubicTo(cx - s * 0.2f, cy - s * 0.3f, cx - s * 0.2f, cy + s * 0.1f, cx, cy + s * 0.3f)
            heartPath.cubicTo(cx + s * 0.2f, cy + s * 0.1f, cx + s * 0.2f, cy - s * 0.3f, cx, cy - s * 0.1f)
            drawPath(heartPath, Color.White.copy(alpha = 0.5f), style = Stroke(width = 2f))

            // 4. Wide Sleeves
            val sleevePath = Path()
            sleevePath.moveTo(cx - s * 0.3f, cy - s * 0.2f)
            sleevePath.lineTo(cx - s * 0.6f, cy + s * 0.3f)
            sleevePath.moveTo(cx + s * 0.3f, cy - s * 0.2f)
            sleevePath.lineTo(cx + s * 0.6f, cy + s * 0.3f)
            drawPath(sleevePath, secondaryColor ?: tint, style = Stroke(width = 5f * s / 50f))
        }
        else -> {
            path.moveTo(cx - s * 0.3f, cy + s * 0.5f)
            path.lineTo(cx - s * 0.2f, cy - s * 0.3f)
            path.quadraticTo(cx, cy - s * 0.8f, cx + s * 0.2f, cy - s * 0.3f)
            path.lineTo(cx + s * 0.3f, cy + s * 0.5f)
            path.close()
            drawPath(path, tint)
        }
    }
}

internal fun DrawScope.drawMonsterShape(cx: Float, cy: Float, s: Float, name: String, tint: Color) {
    val path = Path()

    when {
        name.contains("Bhaya") || name.contains("Fear") -> {
            // 1. Shifting Shadow Mass
            path.reset()
            path.moveTo(cx - s * 0.4f, cy + s * 0.3f)
            path.quadraticTo(cx - s * 0.6f, cy - s * 0.1f, cx - s * 0.3f, cy - s * 0.5f)
            path.quadraticTo(cx, cy - s * 0.8f, cx + s * 0.3f, cy - s * 0.5f)
            path.quadraticTo(cx + s * 0.6f, cy - s * 0.1f, cx + s * 0.4f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint.copy(alpha = 0.7f))

            // 2. Chaotic Tendrils
            val tendrilPath = Path()
            tendrilPath.moveTo(cx - s * 0.2f, cy - s * 0.6f)
            tendrilPath.quadraticTo(cx - s * 0.5f, cy - s * 0.9f, cx - s * 0.1f, cy - s * 1.1f)
            tendrilPath.moveTo(cx + s * 0.2f, cy - s * 0.6f)
            tendrilPath.quadraticTo(cx + s * 0.5f, cy - s * 0.9f, cx + s * 0.1f, cy - s * 1.1f)
            drawPath(tendrilPath, tint, style = Stroke(width = 2f))

            // 3. Glowing Eyes
            drawCircle(Color.Red, s * 0.05f, Offset(cx - s * 0.15f, cy - s * 0.4f))
            drawCircle(Color.Red, s * 0.05f, Offset(cx + s * 0.15f, cy - s * 0.4f))
        }
        name.contains("Tandra") || name.contains("Fatigue") -> {
            // 1. Slumped, Heavy Form
            path.reset()
            path.moveTo(cx - s * 0.6f, cy + s * 0.5f)
            path.quadraticTo(cx - s * 0.7f, cy - s * 0.1f, cx - s * 0.3f, cy - s * 0.3f)
            path.quadraticTo(cx, cy - s * 0.4f, cx + s * 0.3f, cy - s * 0.3f)
            path.quadraticTo(cx + s * 0.7f, cy - s * 0.1f, cx + s * 0.6f, cy + s * 0.5f)
            path.close()
            drawPath(path, tint.copy(alpha = 0.8f))

            // 2. Heavy Eyelids (Sleepy eyes)
            val eyePath = Path()
            eyePath.moveTo(cx - s * 0.25f, cy - s * 0.15f)
            eyePath.quadraticTo(cx - s * 0.15f, cy - s * 0.1f, cx - s * 0.05f, cy - s * 0.15f)
            eyePath.moveTo(cx + s * 0.05f, cy - s * 0.15f)
            eyePath.quadraticTo(cx + s * 0.15f, cy - s * 0.1f, cx + s * 0.25f, cy - s * 0.15f)
            drawPath(eyePath, Color.Black.copy(alpha = 0.5f), style = Stroke(width = 3f))
        }
        name.contains("Chinta") || name.contains("Anxiety") -> {
            // 1. Erratic, Jittery Shape
            path.reset()
            path.moveTo(cx - s * 0.4f, cy + s * 0.3f)
            for (i in 1..8) {
                val dx = (i * s * 0.1f) - s * 0.4f
                val dy = if (i % 2 == 0) -s * 0.5f else -s * 0.3f
                path.lineTo(cx + dx, cy + dy)
            }
            path.lineTo(cx + s * 0.4f, cy + s * 0.3f)
            path.close()
            drawPath(path, tint.copy(alpha = 0.75f))

            // 2. Electric Sparks
            val sparkPath = Path()
            sparkPath.moveTo(cx - s * 0.5f, cy - s * 0.2f)
            sparkPath.lineTo(cx - s * 0.6f, cy - s * 0.4f)
            sparkPath.lineTo(cx - s * 0.45f, cy - s * 0.35f)
            drawPath(sparkPath, Color.Yellow, style = Stroke(width = 2f))
        }
        name.contains("Alasya") || name.contains("Sloth") -> {
            // 1. Massive, Blob-like weight
            drawCircle(tint.copy(alpha = 0.8f), s * 0.5f, Offset(cx, cy + s * 0.2f))
            drawCircle(tint.copy(alpha = 0.6f), s * 0.4f, Offset(cx - s * 0.3f, cy + s * 0.1f))
            drawCircle(tint.copy(alpha = 0.6f), s * 0.4f, Offset(cx + s * 0.3f, cy + s * 0.1f))

            // 2. Heavy Mouth (Yawn)
            drawCircle(Color.Black.copy(alpha = 0.3f), s * 0.1f, Offset(cx, cy + s * 0.3f))
        }
        name.contains("Matsarya") || name.contains("Envy") -> {
            // 1. Serpentine, Covetous Form
            path.reset()
            path.moveTo(cx - s * 0.2f, cy + s * 0.6f)
            path.cubicTo(cx - s * 0.6f, cy + s * 0.2f, cx - s * 0.6f, cy - s * 0.4f, cx - s * 0.1f, cy - s * 0.6f)
            path.cubicTo(cx + s * 0.4f, cy - s * 0.8f, cx + s * 0.6f, cy - s * 0.2f, cx + s * 0.2f, cy + s * 0.6f)
            path.close()
            drawPath(path, tint.copy(alpha = 0.8f))

            // 2. Narrowed Eye
            val eyePath = Path()
            eyePath.moveTo(cx - s * 0.1f, cy - s * 0.3f)
            eyePath.quadraticTo(cx, cy - s * 0.4f, cx + s * 0.1f, cy - s * 0.3f)
            drawPath(eyePath, Color.Green.copy(alpha = 0.7f), style = Stroke(width = 4f))
        }
        name.contains("Krodha") || name.contains("Anger") -> {
            // 1. Spiky, Exploding Mass
            path.reset()
            for (i in 0..11) {
                val angle = i * 2f * PI.toFloat() / 12f
                val radius = if (i % 2 == 0) s * 0.6f else s * 0.3f
                val px = cx + radius * cos(angle)
                val py = cy + radius * sin(angle)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, tint.copy(alpha = 0.9f))

            // 2. Burning Center
            drawCircle(Color(0xFFFF4500).copy(alpha = 0.6f), s * 0.25f, Offset(cx, cy))
        }
        name.contains("Ahankara") || name.contains("Ego") -> {
            // 1. Overbearing, Tall Figure
            path.reset()
            path.moveTo(cx - s * 0.35f, cy + s * 0.6f)
            path.lineTo(cx - s * 0.45f, cy - s * 0.4f)
            path.quadraticTo(cx, cy - s * 1.1f, cx + s * 0.45f, cy - s * 0.4f)
            path.lineTo(cx + s * 0.35f, cy + s * 0.6f)
            path.close()
            drawPath(path, tint.copy(alpha = 0.85f))

            // 2. Jagged Crown
            val crownPath = Path()
            crownPath.moveTo(cx - s * 0.3f, cy - s * 0.7f)
            crownPath.lineTo(cx - s * 0.15f, cy - s * 1.0f)
            crownPath.lineTo(cx, cy - s * 0.8f)
            crownPath.lineTo(cx + s * 0.15f, cy - s * 1.0f)
            crownPath.lineTo(cx + s * 0.3f, cy - s * 0.7f)
            drawPath(crownPath, Color(0xFFFFD700), style = Stroke(width = 4f))
        }
        name.contains("Samsara") -> {
            // 1. Eternal Wheel
            drawCircle(tint, s * 0.55f, Offset(cx, cy), style = Stroke(width = 6f))
            drawCircle(tint.copy(alpha = 0.3f), s * 0.45f, Offset(cx, cy))

            // 2. 8 Spokes of Suffering
            for (i in 0..7) {
                val angle = i * PI.toFloat() / 4f
                val rx = cx + s * 0.55f * cos(angle)
                val ry = cy + s * 0.55f * sin(angle)
                drawLine(tint.copy(alpha = 0.8f), Offset(cx, cy), Offset(rx, ry), 3f)
            }

            // 3. Central Eye of the Cycle
            drawCircle(Color.White, s * 0.12f, Offset(cx, cy))
            drawCircle(Color.Black, s * 0.05f, Offset(cx, cy))
        }
        else -> {
            path.reset()
            path.moveTo(cx - s * 0.4f, cy + s * 0.3f)
            path.quadraticTo(cx - s * 0.5f, cy - s * 0.3f, cx - s * 0.1f, cy - s * 0.5f)
            path.quadraticTo(cx, cy - s * 0.7f, cx + s * 0.1f, cy - s * 0.5f)
            path.quadraticTo(cx + s * 0.5f, cy - s * 0.3f, cx + s * 0.4f, cy + s * 0.3f)
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

fun String.toComposeColor(): Color = Color(android.graphics.Color.parseColor(this))
