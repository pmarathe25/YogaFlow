package com.example.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.game.model.*
import kotlin.math.pow

private val hpGreen = Color(0xFF66BB6A)
private val hpYellow = Color(0xFFFFA726)
private val hpRed = Color(0xFFFF4444)

private fun hpColorFromPercent(pct: Float): Color = when {
    pct < 0.5f -> lerp(hpRed, hpYellow, (pct / 0.5f).coerceIn(0f, 1f))
    else -> lerp(hpYellow, hpGreen, ((pct - 0.5f) / 0.5f).coerceIn(0f, 1f))
}

@Composable
fun HeroHUD(
    hero: HeroInstance,
    statuses: List<BattleStatus>,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = elementToColor(hero.element)

    val animatedHpPercent by animateFloatAsState(
        targetValue = hero.hpPercent,
        animationSpec = tween(300)
    )

    val hpColor = hpColorFromPercent(animatedHpPercent)

    val infiniteTransition = rememberInfiniteTransition()
    val turnGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val borderAlpha = if (isCurrentTurn) turnGlowAlpha else 0f

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val gaugePct = hero.ultimateGauge / 100f
    val isUltReady = gaugePct >= 1f
    val ultGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        border = if (isCurrentTurn) CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(accentColor.copy(alpha = borderAlpha))
        ) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = hero.name,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))

            // HP bar
            Box(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        surfaceVariant,
                        cornerRadius = CornerRadius(2f)
                    )
                    drawRoundRect(
                        color = hpColor,
                        size = Size(size.width * animatedHpPercent, size.height),
                        cornerRadius = CornerRadius(2f)
                    )
                    if (hero.shield > 0) {
                        drawRoundRect(
                            color = Color(0xFF42A5F5).copy(alpha = 0.5f),
                            size = Size(size.width * hero.shield.toFloat() / hero.maxHp, size.height),
                            cornerRadius = CornerRadius(2f),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            Text(
                text = "${hero.currentHp}/${hero.maxHp}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(2.dp))

            // Ultimate gauge
            Box(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val segments = 10
                    val gap = 1f
                    val segW = (w - gap * (segments - 1)) / segments
                    for (i in 0 until segments) {
                        val filled = i < (gaugePct * segments).toInt()
                        val x = i * (segW + gap)
                        val segColor = if (filled) {
                            if (isUltReady) Color(0xFFFFD700).copy(alpha = ultGlowAlpha)
                            else Color(0xFFFFD700)
                        } else Color(0x40000000)
                        drawRoundRect(
                            color = segColor,
                            topLeft = Offset(x, 0f),
                            size = Size(segW, h),
                            cornerRadius = CornerRadius(1f)
                        )
                    }
                    if (isUltReady) {
                        drawRoundRect(
                            color = Color(0xFFFFD700).copy(alpha = ultGlowAlpha * 0.3f),
                            size = size,
                            cornerRadius = CornerRadius(1f),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            // Status effects
            if (statuses.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    statuses.take(3).forEach { status ->
                        StatusIcon(status.statusType)
                    }
                }
            }
        }
    }
}

@Composable
fun MonsterHUD(
    monster: MonsterInstance,
    statuses: List<BattleStatus>,
    modifier: Modifier = Modifier
) {
    val accentColor = elementToColor(monster.element)

    val animatedHpPercent by animateFloatAsState(
        targetValue = monster.hpPercent,
        animationSpec = tween(300)
    )

    val hpColor = hpColorFromPercent(animatedHpPercent)

    val surfaceVariantMonster = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = monster.name,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            if (monster.isBoss) {
                Text(
                    text = "\u2605 BOSS \u2605",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))

            // HP bar
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        surfaceVariantMonster,
                        cornerRadius = CornerRadius(3f)
                    )
                    drawRoundRect(
                        color = hpColor,
                        size = Size(size.width * animatedHpPercent, size.height),
                        cornerRadius = CornerRadius(3f)
                    )
                    if (monster.shield > 0) {
                        drawRoundRect(
                            color = Color(0xFF42A5F5).copy(alpha = 0.5f),
                            size = Size(size.width * monster.shield.toFloat() / monster.maxHp, size.height),
                            cornerRadius = CornerRadius(3f),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }

            Text(
                text = "${monster.currentHp}/${monster.maxHp}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            if (statuses.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    statuses.forEach { status ->
                        StatusIcon(status.statusType, iconSize = 16)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIcon(type: StatusEffectType, iconSize: Int = 12) {
    val color = when (type) {
        StatusEffectType.BURN -> Color(0xFFFF4444)
        StatusEffectType.ATK_UP -> Color(0xFFFFA726)
        StatusEffectType.ATK_DOWN -> Color(0xFFEF5350)
        StatusEffectType.SPD_UP -> Color(0xFF42A5F5)
        StatusEffectType.SPD_DOWN -> Color(0xFF7E57C2)
        StatusEffectType.DAMAGE_REDUCTION -> Color(0xFF66BB6A)
        StatusEffectType.TAUNT -> Color(0xFFFF7043)
        StatusEffectType.STUN -> Color(0xFFBDBDBD)
        StatusEffectType.CONFUSE -> Color(0xFFAB47BC)
        StatusEffectType.SHIELD -> Color(0xFF42A5F5)
        else -> Color.Gray
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f)
    )

    Box(modifier = Modifier.scale(iconScale)) {
        Canvas(modifier = Modifier.size(iconSize.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f

            drawCircle(color.copy(alpha = 0.15f), r, c)
            drawCircle(color, r * 0.6f, c, style = Stroke(width = 1.5f))
            drawCircle(color.copy(alpha = 0.3f), r * 0.4f, c)

            when (type) {
                StatusEffectType.ATK_UP -> {
                    drawLine(Color.White, Offset(c.x, c.y - r * 0.3f), Offset(c.x, c.y + r * 0.3f), 1.5f)
                    drawLine(Color.White, Offset(c.x - r * 0.3f, c.y), Offset(c.x + r * 0.3f, c.y), 1.5f)
                }
                StatusEffectType.SPD_UP -> {
                    val arrowPath = Path().apply {
                        moveTo(c.x, c.y - r * 0.4f)
                        lineTo(c.x - r * 0.25f, c.y)
                        lineTo(c.x + r * 0.25f, c.y)
                        close()
                    }
                    drawPath(arrowPath, Color.White)
                }
                StatusEffectType.SHIELD -> {
                    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.3f, c)
                }
                else -> {}
            }
        }
    }
}

fun elementToColor(element: Element): Color = when (element) {
    Element.FIRE -> Color(0xFFFF6B35)
    Element.WATER -> Color(0xFF42A5F5)
    Element.AIR -> Color(0xFFB2EBF2)
    Element.EARTH -> Color(0xFF8D6E63)
    Element.LIGHT -> Color(0xFFFFD54F)
    Element.DARK -> Color(0xFF7E57C2)
    Element.SHADOW -> Color(0xFF4A148C)
    Element.ELECTRIC -> Color(0xFFFFD740)
    Element.VOID -> Color(0xFFCE93D8)
    Element.NEUTRAL -> Color(0xFFBDBDBD)
}
