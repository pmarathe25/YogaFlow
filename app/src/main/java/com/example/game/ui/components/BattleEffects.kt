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
                        outcome.targets.forEach { targetId ->
                            val targetPos = heroPositions[targetId] ?: monsterPosition
                            
                            if (outcome.damageDealt > 0) {
                                val entry = FloatingTextEntry(
                                    id = System.nanoTime(),
                                    text = "-${outcome.damageDealt}",
                                    color = Color.Red,
                                    startX = targetPos.x,
                                    startY = targetPos.y,
                                    startTime = System.currentTimeMillis()
                                )
                                damageNumbers.add(entry)
                                pool.emit(emitterConfigForElement(event.skill.damageComponents.firstOrNull()?.element ?: Element.NEUTRAL), targetPos, 15)
                            }
                            
                            if (outcome.healingDone > 0) {
                                val entry = FloatingTextEntry(
                                    id = System.nanoTime(),
                                    text = "+${outcome.healingDone}",
                                    color = Color.Green,
                                    startX = targetPos.x,
                                    startY = targetPos.y,
                                    startTime = System.currentTimeMillis()
                                )
                                damageNumbers.add(entry)
                                pool.emit(EmitterConfig(colors = listOf(Color.Green)), targetPos, 10)
                            }
                            
                            if (outcome.shieldApplied > 0) {
                                val entry = FloatingTextEntry(
                                    id = System.nanoTime(),
                                    text = "🛡️ ${outcome.shieldApplied}",
                                    color = Color.Cyan,
                                    startX = targetPos.x,
                                    startY = targetPos.y,
                                    startTime = System.currentTimeMillis()
                                )
                                damageNumbers.add(entry)
                                pool.emit(EmitterConfig(colors = listOf(Color.Cyan)), targetPos, 8)
                            }
                            if (outcome.damageDealt > 0 || outcome.healingDone > 0 || outcome.shieldApplied > 0) {
                                // Add a generic emission if specific ones fail to trigger
                                pool.emit(EmitterConfig(colors = listOf(Color.White)), targetPos, 5)
                            }
                        }
                    }
                }
                is BattleEvent.MonsterTurn -> {
                    val outcome = event.outcome
                    outcome.targets.forEach { targetId ->
                        val targetPos = heroPositions[targetId] ?: Offset.Zero
                        if (outcome.damageDealt > 0) {
                            val entry = FloatingTextEntry(
                                id = System.nanoTime(),
                                text = "-${outcome.damageDealt}",
                                color = Color.Red,
                                startX = targetPos.x,
                                startY = targetPos.y,
                                startTime = System.currentTimeMillis()
                            )
                            damageNumbers.add(entry)
                            pool.emit(EmitterConfig(colors = listOf(Color.Black, Color.Red), force = 12f), targetPos, 12)
                        }
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
