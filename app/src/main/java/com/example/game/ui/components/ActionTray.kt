package com.example.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*

private sealed class CardEntry {
    data class Skill(val skill: com.example.game.model.Skill) : CardEntry()
    data class Ultimate(val skill: com.example.game.model.Skill) : CardEntry()
    data class Combo(val combo: ComboSkill) : CardEntry()
}

@Composable
fun ActionTray(
    currentHero: CombatantState,
    skillCooldowns: Map<String, Int>,
    availableCombos: List<ComboSkill>,
    isTargeting: Boolean,
    onSkill: (com.example.game.model.Skill) -> Unit,
    onComboById: (String) -> Unit,
    onCancelTargeting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCards = remember(currentHero, availableCombos) {
        buildList {
            currentHero.skills.forEach { add(CardEntry.Skill(it)) }
            if (currentHero.ultimate != null) add(CardEntry.Ultimate(currentHero.ultimate!!))
            availableCombos.forEach { add(CardEntry.Combo(it)) }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Wooden table background
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).align(Alignment.BottomCenter)) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF5D4037), Color(0xFF3E2723))
                )
            )
            for (i in 0..10) {
                val y = h * i / 10f
                drawLine(
                    color = Color(0xFF21100B).copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(w, y + (kotlin.math.sin(i.toFloat()) * 20f)),
                    strokeWidth = 2f
                )
            }
            drawRect(
                color = Color(0xFF21100B).copy(alpha = 0.4f),
                size = Size(w, 4f),
                topLeft = Offset(0f, 0f)
            )
        }

        if (!isTargeting) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(allCards, key = { it.hashCode() }) { card ->
                    when (card) {
                        is CardEntry.Skill -> ActionSkillCard(
                            skill = card.skill,
                            element = currentHero.element,
                            cooldownRemaining = skillCooldowns[card.skill.id] ?: 0,
                            baseCooldown = card.skill.cooldown,
                            onClick = { onSkill(card.skill) }
                        )
                        is CardEntry.Ultimate -> ActionUltimateCard(
                            skill = card.skill,
                            isReady = currentHero.gauge >= 100,
                            gauge = currentHero.gauge,
                            element = currentHero.element,
                            onClick = { if (currentHero.gauge >= 100) onSkill(card.skill) }
                        )
                        is CardEntry.Combo -> ActionComboCard(
                            combo = card.combo,
                            onClick = { onComboById(card.combo.id) }
                        )
                    }
                }
            }
        }

        // Targeting instruction overlay
        if (isTargeting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Select a target", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(16.dp))
                    TextButton(onClick = onCancelTargeting) {
                        Text("CANCEL", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSkillCard(
    skill: com.example.game.model.Skill,
    element: Element,
    cooldownRemaining: Int,
    baseCooldown: Int,
    onClick: () -> Unit
) {
    val isOnCooldown = cooldownRemaining > 0
    val elementColor = elementToColor(
        skill.damageComponents.firstOrNull()?.element ?: element
    )

    val infiniteTransition = rememberInfiniteTransition()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
    )

    Card(
        modifier = Modifier
            .width(120.dp)
            .height(160.dp)
            .alpha(if (isOnCooldown) 0.6f else 1f)
            .clickable(enabled = !isOnCooldown) { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Glossy gradient background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.verticalGradient(
                    colors = if (isOnCooldown) {
                        listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD))
                    } else {
                        listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.7f),
                            elementColor.copy(alpha = 0.05f),
                            elementColor.copy(alpha = 0.12f)
                        )
                    }
                )
                drawRect(gradientBrush)
            }

            // Element-colored accent border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        color = if (isOnCooldown) Color.Gray.copy(alpha = 0.3f) else elementColor.copy(alpha = shimmerAlpha),
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Element icon
                Box(modifier = Modifier.size(40.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawElementIcon(
                            element = skill.damageComponents.firstOrNull()?.element ?: element,
                            color = if (isOnCooldown) elementColor.copy(alpha = 0.4f) else elementColor,
                            cx = size.width / 2f,
                            cy = size.height / 2f,
                            size = size.minDimension * 0.7f
                        )
                    }
                }

                // Skill name
                Text(
                    text = skill.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = if (isOnCooldown) Color.Gray else Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Cooldown indicator with radial overlay
                if (baseCooldown > 1) {
                    Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sweepAngle = (cooldownRemaining.toFloat() / baseCooldown) * 360f
                            // Clock ticks
                            for (j in 0..11) {
                                val ta = j * 30f - 90f
                                val tr = size.minDimension * 0.35f
                                val tickLen = if (j % 3 == 0) 4f else 2f
                                val rad = ta * kotlin.math.PI.toFloat() / 180f
                                drawLine(
                                    color = elementColor.copy(alpha = 0.3f),
                                    start = Offset(
                                        size.width / 2f + tr * kotlin.math.cos(rad),
                                        size.height / 2f + tr * kotlin.math.sin(rad)
                                    ),
                                    end = Offset(
                                        size.width / 2f + (tr + tickLen) * kotlin.math.cos(rad),
                                        size.height / 2f + (tr + tickLen) * kotlin.math.sin(rad)
                                    ),
                                    strokeWidth = 1f
                                )
                            }
                            // Cooldown arc overlay
                            if (isOnCooldown) {
                                drawArc(
                                    color = Color(0xFFE53935).copy(alpha = 0.2f),
                                    startAngle = -90f,
                                    sweepAngle = -sweepAngle,
                                    useCenter = true,
                                    style = Fill
                                )
                                drawArc(
                                    color = Color(0xFFE53935).copy(alpha = 0.5f),
                                    startAngle = -90f,
                                    sweepAngle = -sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                        Text(
                            text = if (isOnCooldown) "$cooldownRemaining" else " ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnCooldown) Color(0xFFE53935) else Color.Transparent
                        )
                    }
                } else {
                    Spacer(Modifier.height(26.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionUltimateCard(
    skill: com.example.game.model.Skill,
    isReady: Boolean,
    gauge: Int,
    element: Element,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    val elementColor = elementToColor(element)

    Card(
        modifier = Modifier
            .width(120.dp)
            .height(160.dp)
            .alpha(if (isReady) 1f else 0.6f)
            .clickable(enabled = isReady) { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Glossy gradient background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.verticalGradient(
                    colors = if (isReady) {
                        listOf(Color(0xFFFFF9C4), Color(0xFFFFF176), elementColor.copy(alpha = 0.1f))
                    } else {
                        listOf(Color(0xFFEEEEEE), Color(0xFFE0E0E0))
                    }
                )
                drawRect(gradientBrush)
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Ultimate icon
                Box(modifier = Modifier.size(40.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawElementIcon(
                            element = element,
                            color = if (isReady) Color(0xFFFFD700) else Color.Gray,
                            cx = size.width / 2f,
                            cy = size.height / 2f,
                            size = size.minDimension * 0.7f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = if (isReady) 0.6f else 0.2f),
                            radius = size.minDimension * 0.3f,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = Stroke(width = 2f)
                        )
                    }
                }

                Text(
                    text = skill.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = if (isReady) Color(0xFFBF6B00) else Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Gauge mini progress bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.85f).height(6.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(Color.Black.copy(alpha = 0.15f))
                            val barWidth = size.width * (gauge.toFloat() / 100f)
                            if (barWidth > 0f) {
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFFF176), Color(0xFFFFD700), Color(0xFFFF8F00))
                                    ),
                                    size = Size(barWidth, size.height)
                                )
                            }
                        }
                    }
                    Text(
                        text = "$gauge/100",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) Color(0xFFFFD700) else Color.Gray
                    )
                }
            }

            // Golden glow border when ready
            if (isReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(3.dp, Color(0xFFFFD700).copy(alpha = glowAlpha), RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
private fun ActionComboCard(
    combo: ComboSkill,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )

    Card(
        modifier = Modifier
            .width(120.dp)
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Glossy gradient background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6A1B9A).copy(alpha = 0.2f),
                        Color(0xFF4A148C).copy(alpha = 0.3f),
                        Color(0xFF6A1B9A).copy(alpha = 0.15f)
                    )
                )
                drawRect(gradientBrush)
            }

            // Purple shimmer border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFCE93D8).copy(alpha = glowAlpha),
                                Color(0xFF9C27B0).copy(alpha = shimmerOffset * 0.5f + 0.3f),
                                Color(0xFFCE93D8).copy(alpha = glowAlpha),
                                Color(0xFFE1BEE7).copy(alpha = (1f - shimmerOffset) * 0.5f + 0.3f)
                            ),
                            start = Offset(0f, shimmerOffset * 200f),
                            end = Offset(200f * (1f - shimmerOffset), 200f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Combo icon
                Canvas(modifier = Modifier.size(40.dp)) {
                    val c = Color(0xFFCE93D8)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.3f
                    for (i in 0..7) {
                        val angle = i * kotlin.math.PI.toFloat() / 4f
                        val ex = cx + r * kotlin.math.cos(angle)
                        val ey = cy + r * kotlin.math.sin(angle)
                        drawLine(c, Offset(cx, cy), Offset(ex, ey), strokeWidth = 3f)
                    }
                    drawCircle(c, r * 0.4f, Offset(cx, cy))
                }

                Text(
                    text = combo.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFCE93D8),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = combo.requiredHeroes.joinToString(" + "),
                    fontSize = 8.sp,
                    color = Color(0xFFCE93D8).copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

private fun DrawScope.drawElementIcon(element: Element, color: Color, cx: Float, cy: Float, size: Float) {
    val half = size / 2f
    when (element) {
        Element.FIRE -> {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - half)
                lineTo(cx - half, cy + half)
                lineTo(cx + half, cy + half)
                close()
            }
            drawPath(path, color)
            drawCircle(Color.White.copy(alpha = 0.4f), size * 0.15f, Offset(cx, cy - size * 0.1f))
        }
        Element.WATER -> {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - half, cy + size * 0.15f)
                cubicTo(cx - half, cy - half, cx + half, cy - half, cx + half, cy + size * 0.15f)
                close()
            }
            drawPath(path, color)
        }
        Element.AIR -> {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - half, cy)
                cubicTo(cx - half, cy - half, cx + half, cy - half, cx + half, cy)
                moveTo(cx - half * 0.6f, cy + size * 0.15f)
                cubicTo(cx - half * 0.6f, cy, cx + half * 0.6f, cy, cx + half * 0.6f, cy + size * 0.15f)
            }
            drawPath(path, color, style = Stroke(width = 3f))
        }
        Element.EARTH -> {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - half)
                lineTo(cx + half, cy)
                lineTo(cx, cy + half)
                lineTo(cx - half, cy)
                close()
            }
            drawPath(path, color)
            drawCircle(Color.White.copy(alpha = 0.3f), size * 0.1f, Offset(cx, cy))
        }
        Element.LIGHT -> {
            for (i in 0..7) {
                val angle = i * kotlin.math.PI.toFloat() / 4f - kotlin.math.PI.toFloat() / 2f
                val ex = cx + half * kotlin.math.cos(angle)
                val ey = cy + half * kotlin.math.sin(angle)
                drawLine(color, Offset(cx, cy), Offset(ex, ey), strokeWidth = 3f)
            }
            drawCircle(Color.White.copy(alpha = 0.5f), size * 0.2f, Offset(cx, cy))
        }
        Element.DARK, Element.SHADOW -> {
            drawCircle(color, half, Offset(cx, cy))
            drawCircle(
                Color.Black.copy(alpha = 0.5f),
                half,
                Offset(cx + size * 0.1f, cy - size * 0.1f)
            )
        }
        Element.ELECTRIC -> {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - half)
                lineTo(cx - size * 0.25f, cy)
                lineTo(cx + size * 0.1f, cy)
                lineTo(cx, cy + half)
                lineTo(cx + size * 0.25f, cy - size * 0.05f)
                lineTo(cx - size * 0.1f, cy - size * 0.05f)
                close()
            }
            drawPath(path, color)
        }
        else -> {
            drawCircle(color, half, Offset(cx, cy))
        }
    }
}
