package com.example.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel

@Composable
fun BattleScreen(viewModel: GameViewModel) {
    val battleState by viewModel.battleState.collectAsState()
    val state = battleState ?: return

    // Animation states
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
    val monsterDef = monster?.let { MonsterDefinitions.getMonster(it.monsterId) }
    val monsterColor = monster?.let { elementToColor(it.element) } ?: Color.Gray
    val isBoss = monster?.isBoss ?: false

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Background layer
        BattleBackground(
            parallaxOffset = kotlin.math.sin(parallaxOffset),
            bossFight = isBoss,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Monster area (top 40%)
            Box(
                modifier = Modifier.fillMaxWidth().weight(0.4f),
                contentAlignment = Alignment.Center
            ) {
                if (monster != null && !monster.isDead) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MonsterSprite(
                            monsterName = monster.monsterId,
                            elementColor = monsterColor,
                            isBoss = isBoss,
                            bossPulse = kotlin.math.sin(bossPulse),
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

            // Battle log (compact)
            BattleLog(
                log = viewModel.battleLog.value.takeLast(3),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Hero area (bottom ~35%)
            Box(
                modifier = Modifier.fillMaxWidth().weight(0.35f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    state.aliveHeroes.forEach { hero ->
                        val isTurn = state.currentActorId == hero.heroId
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            HeroSprite(
                                heroName = hero.heroId,
                                elementColor = elementToColor(hero.element),
                                isActive = !hero.isDead,
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

            // Action panel (bottom 25%)
            val currentHero = state.aliveHeroes.find { it.heroId == state.currentActorId }
            if (currentHero != null && state.phase == BattlePhase.PLAYER_TURN) {
                ActionPanel(
                    currentHero = currentHero,
                    heroes = state.heroes,
                    comboAvailable = state.isComboAvailable,
                    currentCombo = state.currentCombo,
                    onSkill = { skill -> viewModel.executeSkill(currentHero.heroId, skill) },
                    onUltimate = { viewModel.executeUltimate(currentHero.heroId) },
                    onDefend = { viewModel.executeDefend(currentHero.heroId) },
                    onCombo = { viewModel.executeCombo(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
