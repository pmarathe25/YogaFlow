package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BattleEffectsLayer(
    events: List<BattleEvent>,
    heroPositions: Map<String, Offset>,
    monsterPosition: Offset,
    pool: ParticlePool,
    shakeHandle: ShakeHandle,
    modifier: Modifier = Modifier
) {
    val damageNumbers = remember { mutableStateListOf<FloatingTextEntry>() }
    var lastProcessedEventCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Screen tint state
    var screenTintColor by remember { mutableStateOf(Color.Transparent) }
    var screenTintAlpha by remember { mutableStateOf(0f) }

    // Cut-in banner state
    var cutInText by remember { mutableStateOf("") }
    var cutInColor by remember { mutableStateOf(Color.White) }
    var showCutIn by remember { mutableStateOf(false) }

    LaunchedEffect(events.size) {
        if (events.size <= lastProcessedEventCount) return@LaunchedEffect

        for (i in lastProcessedEventCount until events.size) {
            val event = events[i]
            when (event) {
                is BattleEvent.SkillUsed -> {
                    val isAttack = event.skill.damageComponents.isNotEmpty() || event.skill.baseDamage > 0
                    val isHeal = event.skill.healScaling != null
                    var totalDamage = 0

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
                                totalDamage += hpDmg
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
                                    text = "${result.shield}",
                                    color = Color.Cyan,
                                    startX = targetPos.x,
                                    startY = targetPos.y,
                                    startTime = System.currentTimeMillis()
                                ))
                                pool.emit(EmitterConfig(colors = listOf(Color.Cyan)), targetPos, 8)
                            }
                        }
                    }

                    // Screen tint/flash by element
                    if (isAttack) {
                        val flashElement = event.skill.damageComponents.firstOrNull()?.element ?: Element.NEUTRAL
                        val flashColor = when {
                            event.skill.healScaling != null -> Color(0xFF66BB6A)
                            else -> elementToColor(flashElement)
                        }
                        screenTintColor = flashColor
                        screenTintAlpha = 0.2f
                        scope.launch {
                            delay(200)
                            screenTintAlpha = 0f
                        }

                        // Screen shake on heavy hits
                        if (totalDamage > 20) {
                            val intensity = (totalDamage.coerceAtMost(50) / 50f) * 12f
                            scope.launch {
                                shakeHandle.shake(intensity = intensity, durationMs = 300)
                            }
                        }

                        // Elemental burst particles
                        val element = flashElement ?: Element.NEUTRAL
                        pool.emit(emitterConfigForElement(element), monsterPosition, 15)
                    } else if (isHeal) {
                        screenTintColor = Color(0xFF66BB6A)
                        screenTintAlpha = 0.15f
                        scope.launch {
                            delay(300)
                            screenTintAlpha = 0f
                        }
                    }

                    // Ultimate/combo cut-in
                    if (event.skill.ultimateGain == 0) {
                        val heroName = stateHeroName(events, event.heroId)
                        cutInText = "${heroName.uppercase()}\nULTIMATE!"
                        cutInColor = elementToColor(
                            event.skill.damageComponents.firstOrNull()?.element ?: Element.NEUTRAL
                        )
                        showCutIn = true
                        scope.launch {
                            delay(1500)
                            showCutIn = false
                        }
                    }
                }
                is BattleEvent.ComboUsed -> {
                    cutInText = "${event.combo.name.uppercase()}\nCOMBO!"
                    cutInColor = Color(0xFF9C27B0)
                    showCutIn = true
                    scope.launch {
                        delay(1500)
                        showCutIn = false
                    }
                    pool.emit(
                        EmitterConfig(colors = listOf(Color(0xFF9C27B0), Color(0xFFCE93D8), Color.White), force = 15f),
                        monsterPosition, 25
                    )
                    scope.launch {
                        shakeHandle.shake(intensity = 10f, durationMs = 400)
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
                            if (hpDmg > 15) {
                                scope.launch {
                                    shakeHandle.shake(intensity = 6f, durationMs = 250)
                                }
                            }
                        }
                        pool.emit(
                            EmitterConfig(colors = listOf(Color.Black, Color.Red), force = 12f),
                            targetPos, 12
                        )
                    }
                }
                else -> {}
            }
        }
        lastProcessedEventCount = events.size
    }

    Box(modifier = modifier) {
        // Screen tint overlay
        if (screenTintAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenTintColor.copy(alpha = screenTintAlpha))
            )
        }

        // Cut-in banner
        AnimatedVisibility(
            visible = showCutIn,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cutInText,
                    color = cutInColor,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            pool.update()
            pool.draw(this)
        }

        // Floating damage/heal numbers
        FloatingTextOverlay(
            entries = damageNumbers,
            onExpired = { id -> damageNumbers.removeAll { it.id == id } }
        )
    }
}

private fun stateHeroName(events: List<BattleEvent>, heroId: String): String {
    // This is just a fallback; the actual rendering uses the current state
    return heroId
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
