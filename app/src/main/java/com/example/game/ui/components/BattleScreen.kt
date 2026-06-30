package com.example.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun BattleScreen(viewModel: GameViewModel) {
    val battleState by viewModel.battleState.collectAsState()
    val state = battleState ?: return

    val infiniteTransition = rememberInfiniteTransition()
    val parallaxOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing))
    )
    val bossPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = FastOutSlowInEasing))
    )

    val monster = state.monsters.firstOrNull()
    val monsterColor = monster?.let { elementToColor(it.element) } ?: Color.Gray
    val isBoss = monster?.isBoss ?: false

    // ─── Sprite Animation States ────────────────────────────────────
    val heroAnimStates = remember { mutableStateMapOf<String, SpriteAnimState>() }
    val monsterAnimState = remember { mutableStateOf(SpriteAnimState()) }

    // Update idle timers
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            val dt = 0.016f
            for ((id, state) in heroAnimStates) {
                if (state.state == SpriteState.IDLE) {
                    heroAnimStates[id] = state.copy(stateTime = state.stateTime + dt)
                }
            }
            if (monsterAnimState.value.state == SpriteState.IDLE) {
                monsterAnimState.value = monsterAnimState.value.copy(
                    stateTime = monsterAnimState.value.stateTime + dt
                )
            }
        }
    }

    // ─── Battle Event → Sprite Animations ──────────────────────────
    val eventLog = state.eventLog
    val lastEventCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(eventLog.size) {
        if (eventLog.size <= lastEventCount.intValue) return@LaunchedEffect
        lastEventCount.intValue = eventLog.size

        val event = eventLog.lastOrNull() ?: return@LaunchedEffect
        when (event) {
            is BattleEvent.SkillUsed -> {
                heroAnimStates[event.heroId] = SpriteAnimState(
                    state = SpriteState.ATTACKING, stateTime = 0f, offsetX = 30f
                )
                delay(100)
                heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                monsterAnimState.value = SpriteAnimState(
                    state = SpriteState.HIT, stateTime = 0f, offsetX = -15f
                )
                delay(150)
                monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
            }
            is BattleEvent.MonsterTurn -> {
                monsterAnimState.value = SpriteAnimState(
                    state = SpriteState.ATTACKING, stateTime = 0f, offsetY = -20f
                )
                delay(100)
                monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                // Hit effect on random hero
                val targetHeroId = event.targets.firstOrNull()
                if (targetHeroId != null) {
                    heroAnimStates[targetHeroId] = SpriteAnimState(
                        state = SpriteState.HIT, stateTime = 0f, offsetX = -10f
                    )
                    delay(150)
                    heroAnimStates[targetHeroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                }
            }
            is BattleEvent.MonsterDown -> {
                monsterAnimState.value = SpriteAnimState(
                    state = SpriteState.DYING, stateTime = 0f, alpha = 0f
                )
            }
            is BattleEvent.HeroDown -> {
                heroAnimStates[event.heroId] = SpriteAnimState(
                    state = SpriteState.DYING, stateTime = 0f, alpha = 0f
                )
            }
            else -> {}
        }
    }

    // ─── Effect System ─────────────────────────────────────────────
    val shakeHandle = rememberShakeHandle()
    val pool = rememberParticlePool(300)

    // Track positions
    var monsterPos by remember { mutableStateOf(Offset.Zero) }
    var heroPositions by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }

    // Shake launcher
    LaunchedEffect(eventLog.size) {
        if (eventLog.size <= 1) return@LaunchedEffect
        val event = eventLog.lastOrNull() ?: return@LaunchedEffect
        when (event) {
            is BattleEvent.SkillUsed -> {
                val dmg = event.outcomes.sumOf { it.damageDealt }
                shakeHandle.shake(if (dmg > 50) 10f else 5f, 250)
            }
            is BattleEvent.ComboUsed -> shakeHandle.shake(14f, 400)
            is BattleEvent.MonsterTurn -> shakeHandle.shake(7f, 250)
            is BattleEvent.MonsterDown -> shakeHandle.shake(12f, 350)
            is BattleEvent.HeroDown -> shakeHandle.shake(8f, 300)
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .shakeOffset(shakeHandle)
    ) {
        // Background
        BattleBackground(
            parallaxOffset = kotlin.math.sin(parallaxOffset),
            bossFight = isBoss,
            monsterElement = monster?.element ?: Element.NEUTRAL,
            elementTint = monsterColor,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Monster area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        monsterPos = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height * 0.4f)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (monster != null && !monster.isDead) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MonsterSprite(
                            monsterName = monster.monsterId,
                            elementColor = monsterColor,
                            isBoss = isBoss,
                            bossPulse = kotlin.math.sin(bossPulse),
                            animState = monsterAnimState.value,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                        MonsterHUD(
                            monster = monster,
                            statuses = state.getStatusesForTarget(monster.monsterId),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Battle log
            BattleLog(
                log = viewModel.battleLog.value.takeLast(3),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Hero area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .onGloballyPositioned { coords ->
                        val basePos = coords.positionInRoot()
                        val hCount = state.aliveHeroes.size.coerceAtLeast(1)
                        heroPositions = state.aliveHeroes.mapIndexed { idx, hero ->
                            val hw = coords.size.width / hCount
                            hero.heroId to Offset(
                                basePos.x + hw * idx + hw / 2f,
                                basePos.y + coords.size.height * 0.5f
                            )
                        }.toMap()
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    state.aliveHeroes.forEach { hero ->
                        val isTurn = state.currentActorId == hero.heroId
                        val animState = heroAnimStates[hero.heroId] ?: SpriteAnimState()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            HeroSprite(
                                heroName = hero.heroId,
                                elementColor = elementToColor(hero.element),
                                isActive = !hero.isDead,
                                animState = animState,
                                modifier = Modifier.size(60.dp)
                            )
                            HeroHUD(
                                hero = hero,
                                statuses = state.getStatusesForTarget(hero.heroId),
                                isCurrentTurn = isTurn,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            // Action panel
            val currentHero = state.aliveHeroes.find { it.heroId == state.currentActorId }
            if (currentHero != null && state.phase == BattlePhase.PLAYER_TURN) {
                ActionPanel(
                    currentHero = currentHero,
                    heroes = state.heroes,
                    comboAvailable = state.isComboAvailable,
                    currentCombo = state.currentCombo,
                    onSkill = { skill -> viewModel.executeSkill(currentHero.heroId, skill) },
                    onUltimate = { viewModel.executeUltimate(currentHero.heroId) },
                    onCombo = { viewModel.executeCombo(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Effect overlay
        BattleEffectOverlay(
            events = eventLog,
            heroPositions = heroPositions,
            monsterPosition = monsterPos,
            pool = pool,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BattleLog(
    log: List<String>,
    modifier: Modifier = Modifier
) {
    if (log.isEmpty()) return
    Column(modifier = modifier) {
        log.forEach { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
