package com.example.game.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.model.Element
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CombatantSprite(
    modifier: Modifier = Modifier,
    isMonster: Boolean,
    name: String,
    elementColor: Color,
    heroId: Int = 0,
    isActive: Boolean = true,
    isBoss: Boolean = false,
    isFlashing: Boolean = false,
    flashColor: Color = Color.Red,
    flashAlpha: Float = 0f,
    bossPulse: Float = 0f,
    animState: SpriteAnimState = SpriteAnimState(),
    isTargeted: Boolean = false,
    isLowHp: Boolean = false,
    isCurrentTurn: Boolean = false,
    element: Element = Element.NEUTRAL
) {
    val smoothOffsetX by animateFloatAsState(
        targetValue = animState.offsetX,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f)
    )
    val smoothOffsetY by animateFloatAsState(
        targetValue = animState.offsetY,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f)
    )
    val smoothScale by animateFloatAsState(
        targetValue = animState.scale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )
    val smoothAlpha by animateFloatAsState(
        targetValue = animState.alpha,
        animationSpec = tween(400)
    )
    val smoothFlashAlpha by animateFloatAsState(
        targetValue = if (isFlashing) flashAlpha else 0f,
        animationSpec = tween(200)
    )

    val tint = if (!isActive) elementColor.copy(alpha = 0.4f) else elementColor

    Canvas(modifier = modifier) {
        val cx = size.width / 2f + smoothOffsetX
        val cy = if (isMonster) size.height * 0.42f + smoothOffsetY
                 else size.height * 0.6f + smoothOffsetY
        val phase = name.hashCode() * 0.1f
        val idleScalePulse = if (animState.state == SpriteState.IDLE) {
            1f + sin(animState.stateTime * 1.2f + phase) * 0.02f
        } else 1f
        val s = size.minDimension * (if (isMonster) 0.3f * (if (isBoss) 1.3f else 1f) else 0.2f) * smoothScale * idleScalePulse

        val idleBob = if (animState.state == SpriteState.IDLE) {
            sin(animState.stateTime * (if (isMonster) 1.5f else 1.5f) + phase) * (if (isMonster) 3f else 4f)
        } else 0f

        val drawCx = cx
        val drawCy = cy + idleBob

        // Elemental glow aura
        if (isActive) {
            drawCircle(
                color = elementColor.copy(alpha = 0.12f + 0.05f * sin(animState.stateTime * 1.2f)),
                radius = s * 1.6f,
                center = Offset(drawCx, drawCy)
            )
        }

        // Boss aura
        if (isMonster && isBoss && bossPulse > 0f) {
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

        // Low-HP pulse effect
        if (isLowHp && isActive) {
            val pulseAlpha = 0.15f + 0.1f * sin(animState.stateTime * 3f)
            drawCircle(
                color = Color.Red.copy(alpha = pulseAlpha),
                radius = s * 1.8f,
                center = Offset(drawCx, drawCy)
            )
        }

        // Draw sprite
        if (isMonster) {
            drawMonsterShape(drawCx, drawCy, s, name, elementColor.copy(alpha = smoothAlpha))
        } else {
            drawSilhouette(drawCx, drawCy, s, heroId, tint.copy(alpha = smoothAlpha))
        }

        // Flash overlay
        if (smoothFlashAlpha > 0f) {
            drawCircle(
                color = flashColor.copy(alpha = smoothFlashAlpha * 0.5f),
                radius = s * (if (isMonster) 1.1f else 1.2f),
                center = Offset(drawCx, drawCy)
            )
        }

        // Turn highlight pulse ring
        if (isCurrentTurn && isActive) {
            val ringPhase = sin(animState.stateTime * 2f) * 0.5f + 0.5f
            drawCircle(
                color = elementColor.copy(alpha = 0.3f + 0.2f * ringPhase),
                radius = s * 1.3f,
                center = Offset(drawCx, drawCy),
                style = Stroke(width = 3f)
            )
        }

        // Target highlight
        if (isTargeted) {
            val pulseScale = 1f + 0.15f * sin(animState.stateTime * 3f)
            drawCircle(
                color = if (isMonster) Color.Red.copy(alpha = 0.6f) else Color.Green.copy(alpha = 0.6f),
                radius = s * 1.1f * pulseScale,
                center = Offset(drawCx, drawCy),
                style = Stroke(width = 4f)
            )
        }
    }
}
