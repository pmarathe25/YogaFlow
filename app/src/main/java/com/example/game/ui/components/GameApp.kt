package com.example.game.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.example.game.viewmodel.GameScreen
import com.example.game.viewmodel.GameViewModel

@Composable
fun GameApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            when {
                targetState == GameScreen.BATTLE -> fadeIn(tween(500)) + scaleIn(initialScale = 0.9f, animationSpec = tween(500)) togetherWith fadeOut(tween(300))
                targetState == GameScreen.BATTLE_RESULT -> fadeIn(tween(400)) togetherWith fadeOut(tween(200))
                targetState == GameScreen.HUB -> fadeIn(tween(300)) + slideInVertically(animationSpec = tween(300)) { it } togetherWith fadeOut(tween(200))
                else -> slideInHorizontally(animationSpec = tween(300)) { it } togetherWith slideOutHorizontally(animationSpec = tween(300)) { -it }
            }
        },
        label = "GameScreenTransition"
    ) { screen ->
        when (screen) {
            GameScreen.HUB -> HubScreen(viewModel)
            GameScreen.BATTLE -> BattleScreen(viewModel)
            GameScreen.PARTY -> PartyScreen(viewModel)
            GameScreen.EQUIPMENT -> EquipmentScreen(viewModel)
            GameScreen.TROPHIES -> TrophyScreen(viewModel)
            GameScreen.SHOP -> ShopScreen(viewModel)
            GameScreen.BATTLE_RESULT -> BattleResultScreen(viewModel)
            GameScreen.SETTINGS -> HubScreen(viewModel)
        }
    }
}
