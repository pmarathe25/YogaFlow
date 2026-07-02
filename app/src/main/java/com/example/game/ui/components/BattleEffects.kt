package com.example.game.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import kotlinx.coroutines.delay

@Composable
fun BattleEffectOverlay(
    events: List<BattleEvent>,
    heroPositions: Map<String, Offset>,
    monsterPosition: Offset,
    pool: ParticlePool,
    modifier: Modifier = Modifier
) {
    val damageNumbers = remember { mutableStateListOf<FloatingTextEntry>() }
    var lastProcessedEventCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(events.size) {
        if (events.size <= lastProcessedEventCount) return@LaunchedEffect
        
        for (i in lastProcessedEventCount until events.size) {
            val event = events[i]
            when (event) {
                is BattleEvent.SkillUsed -> {
                    event.outcomes.forEach { outcome ->
                        outcome.perTargetResult.forEach { (targetId, result) ->
                            val targetPos = heroPositions[targetId] ?: monsterPosition

                            if (result.shieldDamage > 0) {
                                damageNumbers.add(FloatingTextEntry(
                                    id = System.nanoTime(),
                                    text = "-${result.shieldDamage}",
                                    color = Color(0xFF9C27B0),
                                    startX = targetPos.x,
                                    startY = targetPos.y - 15f,
                                    startTime = System.currentTimeMillis()
                                ))
                                pool.emit(EmitterConfig(colors = listOf(Color(0xFF9C27B0), Color(0xFFCE93D8))), targetPos, 10)
                            }

                            val hpDmg = result.damage - result.shieldDamage
                            if (hpDmg > 0) {
                                damageNumbers.add(FloatingTextEntry(
                                    id = System.nanoTime() + 1,
                                    text = "-${hpDmg}",
                                    color = Color.Red,
                                    startX = targetPos.x,
                                    startY = targetPos.y,
                                    startTime = System.currentTimeMillis()
                                ))
                            }

                            if (result.heal > 0) {
                                damageNumbers.add(FloatingTextEntry(
                                    id = System.nanoTime() + 2,
                                    text = "+${result.heal}",
                                    color = Color.Green,
                                    startX = targetPos.x,
                                    startY = targetPos.y,
                                    startTime = System.currentTimeMillis()
                                ))
                                pool.emit(EmitterConfig(colors = listOf(Color.Green)), targetPos, 10)
                            }

                            if (result.shield > 0) {
                                damageNumbers.add(FloatingTextEntry(
                                    id = System.nanoTime() + 3,
                                    text = "🛡️ ${result.shield}",
                                    color = Color.Cyan,
                                    startX = targetPos.x,
                                    startY = targetPos.y,
                                    startTime = System.currentTimeMillis()
                                ))
                                pool.emit(EmitterConfig(colors = listOf(Color.Cyan)), targetPos, 8)
                            }
                        }
                    }
                }
                is BattleEvent.MonsterTurn -> {
                    event.outcome.perTargetResult.forEach { (targetId, result) ->
                        val targetPos = heroPositions[targetId] ?: Offset.Zero

                        val shieldDmg = result.shieldDamage
                        val hpDmg = result.damage - shieldDmg

                        if (shieldDmg > 0) {
                            damageNumbers.add(FloatingTextEntry(
                                id = System.nanoTime(),
                                text = "-${shieldDmg}",
                                color = Color(0xFF9C27B0),
                                startX = targetPos.x,
                                startY = targetPos.y - 15f,
                                startTime = System.currentTimeMillis()
                            ))
                        }
                        if (hpDmg > 0) {
                            damageNumbers.add(FloatingTextEntry(
                                id = System.nanoTime() + 1,
                                text = "-${hpDmg}",
                                color = Color.Red,
                                startX = targetPos.x,
                                startY = targetPos.y,
                                startTime = System.currentTimeMillis()
                            ))
                        }
                        pool.emit(EmitterConfig(colors = listOf(Color.Black, Color.Red), force = 12f), targetPos, 12)
                    }
                }
                else -> {}
            }
        }
        lastProcessedEventCount = events.size
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            pool.update()
            pool.draw(this)
        }
        
        FloatingTextOverlay(
            entries = damageNumbers,
            onExpired = { id -> damageNumbers.removeAll { it.id == id } }
        )
    }
}

fun emitterConfigForElement(element: Element): EmitterConfig {
    return when (element) {
        Element.FIRE -> EmitterConfig(colors = listOf(Color.Red, Color.Yellow), force = 10f)
        Element.WATER -> EmitterConfig(colors = listOf(Color.Blue, Color.Cyan), force = 6f)
        Element.AIR -> EmitterConfig(colors = listOf(Color.White, Color.LightGray), force = 8f)
        Element.EARTH -> EmitterConfig(colors = listOf(Color(0xFF795548), Color(0xFF5D4037)), force = 5f)
        Element.LIGHT -> EmitterConfig(colors = listOf(Color.Yellow, Color.White), force = 12f, blendMode = BlendMode.Screen)
        Element.DARK -> EmitterConfig(colors = listOf(Color.DarkGray, Color.Black), force = 7f)
        else -> EmitterConfig(colors = listOf(Color.Gray), force = 5f)
    }
}
