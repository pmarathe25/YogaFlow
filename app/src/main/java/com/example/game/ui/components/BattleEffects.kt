package com.example.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.model.*
import kotlinx.coroutines.delay
import kotlin.math.sin

data class EffectState(
    val damageNumbers: List<FloatingTextEntry> = emptyList(),
    val hitParticles: List<Pair<Offset, EmitterConfig>> = emptyList(),
    val shockwaveRadius: Float = 0f,
    val shockwaveAlpha: Float = 0f,
    val shockwaveCenter: Offset = Offset.Zero,
    val screenFlashAlpha: Float = 0f,
    val victoryFountain: Boolean = false,
    val defeatRain: Boolean = false
)

@Composable
fun rememberEffectState(): EffectState = remember { EffectState() }

@Composable
fun BattleEffectOverlay(
    events: List<BattleEvent>,
    heroPositions: Map<String, Offset>,
    monsterPosition: Offset,
    pool: ParticlePool,
    modifier: Modifier = Modifier
) {
    var damageNumbers by remember { mutableStateOf<List<FloatingTextEntry>>(emptyList()) }
    var shockwave by remember { mutableStateOf<ShockwaveState?>(null) }
    var screenFlash by remember { mutableStateOf(0f) }
    var victoryParticles by remember { mutableStateOf(false) }
    var defeatParticles by remember { mutableStateOf(false) }

    val lastEventCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(events.size) {
        if (events.size <= lastEventCount.intValue) return@LaunchedEffect
        lastEventCount.intValue = events.size

        val event = events.lastOrNull() ?: return@LaunchedEffect
        when (event) {
            is BattleEvent.SkillUsed -> {
                val heroPos = heroPositions[event.heroId] ?: return@LaunchedEffect
                val totalDmg = event.outcomes.sumOf { it.damageDealt }
                val totalHeal = event.outcomes.sumOf { it.healingDone }

                // Particle stream from hero toward monster (80ms)
                val streamConfig = EmitterConfig(
                    particlesPerSecond = 300,
                    spreadAngle = 25f,
                    force = 180f,
                    gravity = 0f,
                    colors = listOf(Color(0xFFFFD740), Color(0xFFFFA726)),
                    sizeRange = 2f..4f,
                    lifetimeRange = 10..20
                )
                repeat(5) {
                    pool.emit(streamConfig, heroPos, 8)
                    delay(16)
                }

                val dmgEntry = FloatingTextEntry(
                    id = System.nanoTime(),
                    text = if (totalDmg > 0) "$totalDmg" else if (totalHeal > 0) "HEAL +$totalHeal" else "0",
                    color = if (totalDmg > 0) Color(0xFFFF4444) else Color(0xFF66BB6A),
                    startX = monsterPosition.x - 20f + kotlin.random.Random.nextFloat() * 40f,
                    startY = monsterPosition.y - 30f
                )
                damageNumbers = damageNumbers + dmgEntry

                val impactConfig = EmitterConfig(
                    particlesPerSecond = 200,
                    spreadAngle = 180f,
                    force = 80f,
                    gravity = 100f,
                    colors = listOf(Color(0xFFFF6B35), Color(0xFFFFA726), Color(0xFFFFD740)),
                    sizeRange = 2f..5f,
                    lifetimeRange = 15..30
                )
                pool.emit(impactConfig, monsterPosition, 15)
            }

            is BattleEvent.ComboUsed -> {
                screenFlash = 0.5f
                shockwave = ShockwaveState(
                    center = monsterPosition,
                    maxRadius = 300f,
                    durationMs = 500
                )
                val comboConfig = EmitterConfig(
                    particlesPerSecond = 300,
                    spreadAngle = 360f,
                    force = 150f,
                    gravity = 50f,
                    colors = listOf(Color(0xFF9C27B0), Color(0xFFE040FB), Color(0xFFFFD740)),
                    sizeRange = 3f..8f,
                    lifetimeRange = 20..50
                )
                pool.emit(comboConfig, monsterPosition, 30)
            }

            is BattleEvent.MonsterTurn -> {
                monsterPositions@ for ((_, pos) in heroPositions) {
                    pool.emit(
                        EmitterConfig(
                            particlesPerSecond = 150,
                            spreadAngle = 120f,
                            force = 100f,
                            colors = listOf(Color(0xFFB71C1C), Color(0xFFE53935)),
                            sizeRange = 2f..4f,
                            lifetimeRange = 10..25
                        ),
                        pos, 10
                    )
                }
            }

            is BattleEvent.HeroDown -> {
                val pos = heroPositions[event.heroId] ?: return@LaunchedEffect
                pool.emit(
                    EmitterConfig(
                        particlesPerSecond = 200,
                        spreadAngle = 360f,
                        force = 60f,
                        gravity = 80f,
                        colors = listOf(Color.Gray, Color.White.copy(alpha = 0.5f)),
                        sizeRange = 1f..4f,
                        lifetimeRange = 30..60
                    ),
                    pos, 25
                )
            }

            is BattleEvent.MonsterDown -> {
                pool.emit(
                    EmitterConfig(
                        particlesPerSecond = 300,
                        spreadAngle = 360f,
                        force = 120f,
                        gravity = 60f,
                        colors = listOf(Color(0xFFFFD740), Color.White, Color(0xFFFF6B35)),
                        sizeRange = 3f..8f,
                        lifetimeRange = 20..50
                    ),
                    monsterPosition, 40
                )
            }

            is BattleEvent.PhaseTriggered -> {
                val intensity = 0.3f + event.phaseIndex * 0.2f
                screenFlash = intensity
                shockwave = ShockwaveState(
                    center = monsterPosition,
                    maxRadius = 200f + event.phaseIndex * 60f,
                    durationMs = 600
                )
                pool.emit(
                    EmitterConfig(
                        particlesPerSecond = 250,
                        spreadAngle = 360f,
                        force = 100f + event.phaseIndex * 30f,
                        gravity = 40f,
                        colors = listOf(Color(0xFFFF6B35), Color(0xFFFFD740), Color.White),
                        sizeRange = 3f..7f,
                        lifetimeRange = 25..50
                    ),
                    monsterPosition, 20 + event.phaseIndex * 10
                )
            }

            is BattleEvent.Victory -> {
                victoryParticles = true
            }

            is BattleEvent.Defeat -> {
                defeatParticles = true
            }
        }
    }

    // Victory fountain
    LaunchedEffect(victoryParticles) {
        if (!victoryParticles) return@LaunchedEffect
        while (true) {
            val fountainPos = Offset(
                monsterPosition.x + kotlin.random.Random.nextFloat() * 200f - 100f,
                monsterPosition.y
            )
            pool.emit(
                EmitterConfig(
                    particlesPerSecond = 50,
                    spreadAngle = 30f,
                    force = -80f,
                    gravity = 50f,
                    colors = listOf(Color(0xFFFFD740), Color(0xFFFFA726), Color.White),
                    sizeRange = 2f..5f,
                    lifetimeRange = 30..60
                ),
                fountainPos, 5
            )
            delay(50)
        }
    }

    LaunchedEffect(defeatParticles) {
        if (!defeatParticles) return@LaunchedEffect
        while (true) {
            val rainPos = Offset(
                kotlin.random.Random.nextFloat() * 400f,
                -10f
            )
            pool.emit(
                EmitterConfig(
                    particlesPerSecond = 30,
                    spreadAngle = 10f,
                    force = 100f,
                    gravity = 150f,
                    colors = listOf(Color(0xFFB71C1C).copy(alpha = 0.6f), Color(0xFFE53935).copy(alpha = 0.4f)),
                    sizeRange = 1f..3f,
                    lifetimeRange = 40..80
                ),
                rainPos, 2
            )
            delay(33)
        }
    }

    // Shockwave animation
    LaunchedEffect(shockwave) {
        val sw = shockwave ?: return@LaunchedEffect
        val steps = 30
        for (i in 0 until steps) {
            sw.currentRadius = sw.maxRadius * (i.toFloat() / steps)
            sw.currentAlpha = (1f - i.toFloat() / steps) * 0.6f
            delay(sw.durationMs / steps)
        }
        shockwave = null
    }

    // Screen flash decay
    LaunchedEffect(screenFlash) {
        if (screenFlash <= 0f) return@LaunchedEffect
        val steps = 10
        for (i in 0 until steps) {
            screenFlash = 0.5f * (1f - i.toFloat() / steps)
            delay(50)
        }
        screenFlash = 0f
    }

    // Render layer
    Canvas(modifier = modifier) {
        // Screen flash
        if (screenFlash > 0f) {
            drawRect(Color.White.copy(alpha = screenFlash))
        }

        // Shockwave ring
        val sw = shockwave
        if (sw != null && sw.currentAlpha > 0f) {
            drawCircle(
                color = Color.White.copy(alpha = sw.currentAlpha),
                radius = sw.currentRadius,
                center = sw.center,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color(0xFF9C27B0).copy(alpha = sw.currentAlpha * 0.5f),
                radius = sw.currentRadius * 0.9f,
                center = sw.center,
                style = Stroke(width = 6f)
            )
        }

        // Particles
        if (pool.hasActive) {
            pool.update()
            pool.draw(this)
        }
    }
}

data class ShockwaveState(
    val center: Offset,
    val maxRadius: Float,
    val durationMs: Long,
    var currentRadius: Float = 0f,
    var currentAlpha: Float = 0f
)
