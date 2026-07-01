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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
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
    val accentColor = elementToColor(element)
    val hpPercent = hp.toFloat() / maxHp.coerceAtLeast(1)
    
    val animatedHpPercent by animateFloatAsState(
        targetValue = hpPercent,
        animationSpec = tween(400, easing = FastOutSlowInEasing)
    )

    Column(
        modifier = modifier.width(width.dp),
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
                drawRoundRect(
                    color = Color(0xFF42A5F5),
                    size = Size(size.width * shieldPct, size.height),
                    cornerRadius = CornerRadius(1f)
                )
            }
            Spacer(Modifier.height(1.dp))
        }

        // HP Bar
        Box(modifier = Modifier.fillMaxWidth().height(10.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background
                drawRoundRect(
                    Color.Black.copy(alpha = 0.6f),
                    cornerRadius = CornerRadius(2f)
                )
                // Fill
                drawRoundRect(
                    color = hpBarColor ?: hpColorFromPercent(animatedHpPercent),
                    size = Size(size.width * animatedHpPercent, size.height),
                    cornerRadius = CornerRadius(2f)
                )
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
                    drawRoundRect(
                        color = Color(0xFFFFD700),
                        size = Size(size.width * gPct, size.height),
                        cornerRadius = CornerRadius(1f)
                    )
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
    val (icon, color) = when (type) {
        StatusEffectType.ATK_UP -> "⚔️" to Color(0xFFEF5350)
        StatusEffectType.ATK_DOWN -> "⚔️" to Color.Gray
        StatusEffectType.SPD_UP -> "👟" to Color(0xFF66BB6A)
        StatusEffectType.SPD_DOWN -> "👟" to Color.Gray
        StatusEffectType.BURN -> "🔥" to Color(0xFFFFA500)
        StatusEffectType.STUN -> "💫" to Color.Yellow
        StatusEffectType.TAUNT -> "🎯" to Color.Red
        StatusEffectType.CONFUSE -> "🌀" to Color.LightGray
        StatusEffectType.DAMAGE_REDUCTION -> "🛡️" to Color.Blue
        StatusEffectType.DEF_DOWN -> "🛡️" to Color.Gray
        StatusEffectType.SHIELD -> "🛡️" to Color.Cyan
    }

    Surface(
        shape = RoundedCornerShape(2.dp),
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.size(iconSize.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = icon, fontSize = (iconSize - 2).sp)
        }
    }
}

fun elementToColor(element: Element): Color {
    return when (element) {
        Element.FIRE -> Color(0xFFF44336)
        Element.WATER -> Color(0xFF2196F3)
        Element.AIR -> Color(0xFFE1F5FE)
        Element.EARTH -> Color(0xFF795548)
        Element.LIGHT -> Color(0xFFFFF176)
        Element.DARK -> Color(0xFF3F51B5)
        Element.SHADOW -> Color(0xFF455A64)
        Element.ELECTRIC -> Color(0xFFFFEB3B)
        Element.VOID -> Color(0xFF9C27B0)
        Element.NEUTRAL -> Color(0xFF9E9E9E)
    }
}

@Composable
fun HeroHUD(hero: HeroInstance, statuses: List<BattleStatus>, isCurrentTurn: Boolean, modifier: Modifier = Modifier) {
    FloatingHUD(hero.name, hero.currentHp, hero.maxHp, modifier, hero.shield, hero.ultimateGauge, hero.element, statuses, isCurrentTurn, width = 60)
}

@Composable
fun MonsterHUD(monster: MonsterInstance, statuses: List<BattleStatus>, modifier: Modifier = Modifier) {
    FloatingHUD(monster.name, monster.currentHp, monster.maxHp, modifier, monster.shield, null, monster.element, statuses, false, width = 100, hpBarColor = Color.Red)
}
