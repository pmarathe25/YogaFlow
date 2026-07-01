package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.BattleEvent
import com.example.game.model.BattleState
import com.example.game.model.SpriteState
import kotlinx.coroutines.delay

@Composable
fun TurnBanner(
    actorName: String?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { -it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${actorName ?: "Unknown"}'s Turn!",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}

data class SpriteAnimState(
    val state: SpriteState = SpriteState.IDLE,
    val stateTime: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val alpha: Float = 1f
)

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
                heroAnimStates[event.heroId] = SpriteAnimState(
                    state = SpriteState.ATTACKING, stateTime = 0f, offsetX = if (isAttack) 40f else 0f, offsetY = if (!isAttack) -20f else 0f
                )
                delay(200)
                heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                
                if (isAttack) {
                    monsterAnimState.value = SpriteAnimState(state = SpriteState.HIT, stateTime = 0f, offsetX = -20f)
                    delay(150)
                    monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                }
            }
            is BattleEvent.MonsterTurn -> {
                monsterAnimState.value = SpriteAnimState(state = SpriteState.ATTACKING, stateTime = 0f, offsetY = 30f)
                delay(200)
                monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
                event.targets.forEach { targetHeroId ->
                    heroAnimStates[targetHeroId] = SpriteAnimState(state = SpriteState.HIT, stateTime = 0f, offsetY = 15f)
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
                val s = heroAnimStates[hero.heroId] ?: SpriteAnimState()
                if (s.state == SpriteState.IDLE) {
                    heroAnimStates[hero.heroId] = s.copy(stateTime = s.stateTime + dt)
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
