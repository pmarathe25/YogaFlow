package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.game.model.BattleEvent
import com.example.game.model.BattleState
import com.example.game.ui.components.SpriteState
import com.example.game.ui.components.SpriteAnimState
import kotlinx.coroutines.delay

@Composable
fun TurnBanner(
    actorName: String?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    var showBanner by remember { mutableStateOf(false) }

    LaunchedEffect(actorName, visible) {
        if (visible && actorName != null) {
            showBanner = true
            delay(1500)
            showBanner = false
        }
    }

    AnimatedVisibility(
        visible = showBanner,
        enter = scaleIn(initialScale = 0.5f) + fadeIn(),
        exit = scaleOut(targetScale = 1.5f) + fadeOut(),
        modifier = modifier
    ) {
        Text(
            text = "${actorName?.uppercase() ?: "???"}'S TURN",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

@Composable
fun rememberSpriteAnimations(
    eventLog: List<BattleEvent>,
    state: BattleState
): Pair<Map<String, SpriteAnimState>, State<SpriteAnimState>> {
    val heroAnimStates = remember { mutableStateMapOf<String, SpriteAnimState>() }
    val monsterAnimState = remember { mutableStateOf(SpriteAnimState()) }
    val lastEventCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(eventLog.size) {
        if (eventLog.size <= lastEventCount.intValue) return@LaunchedEffect
        lastEventCount.intValue = eventLog.size

        val event = eventLog.lastOrNull() ?: return@LaunchedEffect
        when (event) {
            is BattleEvent.SkillUsed -> {
                val isAttack = event.skill.damageComponents.isNotEmpty() || event.skill.baseDamage > 0
                if (isAttack) {
                    heroAnimStates[event.heroId] = SpriteAnimState(
                        state = SpriteState.ATTACKING, stateTime = 0f,
                        offsetX = 80f, offsetY = -10f
                    )
                    delay(300)
                    heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                    
                    monsterAnimState.value = SpriteAnimState(
                        state = SpriteState.HIT, stateTime = 0f,
                        offsetX = -25f, offsetY = 5f
                    )
                    delay(200)
                    monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                } else {
                    heroAnimStates[event.heroId] = SpriteAnimState(
                        state = SpriteState.ATTACKING, stateTime = 0f,
                        offsetX = 0f, offsetY = -30f
                    )
                    delay(300)
                    heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                }
            }
            is BattleEvent.MonsterTurn -> {
                monsterAnimState.value = SpriteAnimState(
                    state = SpriteState.ATTACKING, stateTime = 0f,
                    offsetX = -80f, offsetY = 10f
                )
                delay(300)
                monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                event.targets.forEach { targetHeroId ->
                    heroAnimStates[targetHeroId] = SpriteAnimState(
                        state = SpriteState.HIT, stateTime = 0f,
                        offsetX = -15f, offsetY = 10f
                    )
                }
                delay(200)
                event.targets.forEach { targetHeroId ->
                    heroAnimStates[targetHeroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                }
            }
            is BattleEvent.MonsterDown -> {
                monsterAnimState.value = SpriteAnimState(state = SpriteState.DYING, stateTime = 0f, alpha = 0f, offsetY = 30f)
            }
            is BattleEvent.HeroDown -> {
                heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.DYING, stateTime = 0f, alpha = 0f, offsetY = 30f)
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            val dt = 0.016f
            state.heroes.forEach { hero ->
                val s = heroAnimStates[hero.id] ?: SpriteAnimState()
                if (s.state == SpriteState.IDLE) {
                    heroAnimStates[hero.id] = s.copy(stateTime = s.stateTime + dt)
                }
            }
            if (monsterAnimState.value.state == SpriteState.IDLE) {
                monsterAnimState.value = monsterAnimState.value.copy(
                    stateTime = monsterAnimState.value.stateTime + dt
                )
            }
        }
    }

    return heroAnimStates to monsterAnimState
}
