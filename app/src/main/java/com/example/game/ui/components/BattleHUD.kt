package com.example.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.ui.theme.*

@Composable
fun HeroHUD(
    hero: HeroInstance,
    statuses: List<BattleStatus>,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = elementToColor(hero.element)
    val hpColor = if (hero.hpPercent < 0.3f) Color(0xFFFF4444)
    else if (hero.hpPercent < 0.6f) Color(0xFFFFA726)
    else Color(0xFF66BB6A)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0x80000000)
        ),
        border = if (isCurrentTurn) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(accentColor)
        ) else null
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = hero.name,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))

            // HP bar
            Box(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(Color(0x40000000), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f))
                    drawRoundRect(
                        color = hpColor,
                        size = Size(size.width * hero.hpPercent, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                    )
                    if (hero.shield > 0) {
                        drawRoundRect(
                            color = Color(0xFF42A5F5).copy(alpha = 0.5f),
                            size = Size(size.width * hero.shield.toFloat() / hero.maxHp, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            Text(
                text = "${hero.currentHp}/${hero.maxHp}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 9.sp
            )
            Spacer(Modifier.height(2.dp))

            // Ultimate gauge
            val gaugePct = hero.ultimateGauge / 100f
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(Color(0x40000000), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f))
                    drawRoundRect(
                        color = Color(0xFFFFD700),
                        size = Size(size.width * gaugePct, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f)
                    )
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
    val hpColor = if (monster.hpPercent < 0.3f) Color(0xFFFF4444)
    else if (monster.hpPercent < 0.6f) Color(0xFFFFA726)
    else Color(0xFF66BB6A)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0x80000000)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = monster.name,
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (monster.isBoss) {
                Text(
                    text = "★ BOSS ★",
                    color = Color(0xFFFF4444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))

            // HP bar
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(Color(0x40000000), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
                    drawRoundRect(
                        color = hpColor,
                        size = Size(size.width * monster.hpPercent, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                    )
                    if (monster.shield > 0) {
                        drawRoundRect(
                            color = Color(0xFF42A5F5).copy(alpha = 0.5f),
                            size = Size(size.width * monster.shield.toFloat() / monster.maxHp, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }

            Text(
                text = "${monster.currentHp}/${monster.maxHp}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
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

    Canvas(modifier = Modifier.size(iconSize.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f

        drawCircle(color.copy(alpha = 0.2f), r, c)
        drawCircle(color, r * 0.6f, c)

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
