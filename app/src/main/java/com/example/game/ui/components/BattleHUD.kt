package com.example.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*

private val hpGreen = Color(0xFF66BB6A)
private val hpYellow = Color(0xFFFFCA28)
private val hpRed = Color(0xFFEF5350)

fun hpColorFromPercent(pct: Float): Color {
    return if (pct > 0.6f) hpGreen
    else if (pct > 0.25f) hpYellow
    else hpRed
}

@Composable
fun FloatingHUD(
    name: String,
    hp: Int,
    maxHp: Int,
    modifier: Modifier = Modifier,
    shield: Int = 0,
    gauge: Int? = null, // null for monsters
    element: Element,
    statuses: List<BattleStatus> = emptyList(),
    isCurrentTurn: Boolean = false,
    width: Int = 80,
    hpBarColor: Color? = null
) {
    val accentColor = element.color
    val hpPercent = hp.toFloat() / maxHp.coerceAtLeast(1)
    
    val animatedHpPercent by animateFloatAsState(
        targetValue = hpPercent,
        animationSpec = tween(400, easing = FastOutSlowInEasing)
    )

    Column(
        modifier = modifier.requiredWidth(width.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Name and Turn Indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCurrentTurn) {
                Text("▶ ", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrentTurn) accentColor else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
        
        Spacer(Modifier.height(2.dp))

        // Shield Bar (if any)
        if (shield > 0) {
            val shieldPct = (shield.toFloat() / maxHp).coerceAtMost(1f)
            Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                val shieldWidth = size.width * shieldPct
                if (shieldWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF64B5F6),
                                Color(0xFF1E88E5)
                            )
                        ),
                        size = Size(shieldWidth, size.height),
                        cornerRadius = CornerRadius(1f)
                    )
                }
            }
            Spacer(Modifier.height(1.dp))
        }

        // HP Bar
        Box(modifier = Modifier.fillMaxWidth().height(10.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    cornerRadius = CornerRadius(3f)
                )
                // Fill with vertical gradient
                val baseColor = hpBarColor ?: hpColorFromPercent(animatedHpPercent)
                val fillWidth = size.width * animatedHpPercent
                if (fillWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.9f),
                                baseColor,
                                baseColor.copy(
                                    red = (baseColor.red * 0.6f).coerceAtMost(1f),
                                    green = (baseColor.green * 0.6f).coerceAtMost(1f),
                                    blue = (baseColor.blue * 0.6f).coerceAtMost(1f)
                                )
                            )
                        ),
                        size = Size(fillWidth, size.height),
                        cornerRadius = CornerRadius(3f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.15f),
                        size = Size(fillWidth.coerceAtLeast(1f), size.height * 0.3f),
                        cornerRadius = CornerRadius(1f)
                    )
                }
            }
            
            // Numerical HP
            Text(
                text = "$hp/$maxHp",
                color = Color.White,
                fontSize = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Ultimate Gauge (if present)
        gauge?.let { g ->
            Spacer(Modifier.height(2.dp))
            val gPct = g.toFloat() / 100f
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(2.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(Color.Black.copy(alpha = 0.3f), cornerRadius = CornerRadius(1f))
                    val gaugeWidth = size.width * gPct
                    if (gaugeWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFF176),
                                    Color(0xFFFFD700),
                                    Color(0xFFFF8F00)
                                )
                            ),
                            size = Size(gaugeWidth, size.height),
                            cornerRadius = CornerRadius(1f)
                        )
                    }
                }
            }
        }

        // Status Effects
        if (statuses.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                statuses.take(4).forEach { status ->
                    StatusIcon(status.statusType, iconSize = 10)
                }
            }
        }
    }
}

@Composable
fun StatusIcon(type: StatusEffectType, iconSize: Int = 12) {
    val (label, color) = when (type) {
        StatusEffectType.ATK_UP -> "ATK↑" to Color(0xFFEF5350)
        StatusEffectType.ATK_DOWN -> "ATK↓" to Color.Gray
        StatusEffectType.SPD_UP -> "SPD↑" to Color(0xFF66BB6A)
        StatusEffectType.SPD_DOWN -> "SPD↓" to Color.Gray
        StatusEffectType.BURN -> "BRN" to Color(0xFFFFA500)
        StatusEffectType.STUN -> "STN" to Color.Yellow
        StatusEffectType.TAUNT -> "TNT" to Color.Red
        StatusEffectType.CONFUSE -> "CNF" to Color.LightGray
        StatusEffectType.DAMAGE_REDUCTION -> "DRD" to Color.Blue
        StatusEffectType.DEF_DOWN -> "DEF↓" to Color.Gray
        StatusEffectType.SHIELD -> "SHD" to Color.Cyan
        StatusEffectType.REFLECT -> "RFL" to Color.Magenta
    }

    Box(
        modifier = Modifier.size(iconSize.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 1f
            drawCircle(color = color.copy(alpha = 0.25f), radius = r, center = Offset(cx, cy))
            drawCircle(color = color.copy(alpha = 0.5f), radius = r, center = Offset(cx, cy), style = Stroke(width = 1.5f))
        }
        Text(
            text = label,
            color = color,
            fontSize = (iconSize - 4).sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HeroHUD(hero: CombatantState, statuses: List<BattleStatus>, isCurrentTurn: Boolean, modifier: Modifier = Modifier) {
    FloatingHUD(hero.name, hero.hp, hero.maxHp, modifier, hero.shield, hero.gauge, hero.element, statuses, isCurrentTurn, width = 60)
}

@Composable
fun MonsterHUD(monster: CombatantState, statuses: List<BattleStatus>, modifier: Modifier = Modifier) {
    FloatingHUD(monster.name, monster.hp, monster.maxHp, modifier, monster.shield, null, monster.element, statuses, false, width = 100, hpBarColor = Color.Red)
}
