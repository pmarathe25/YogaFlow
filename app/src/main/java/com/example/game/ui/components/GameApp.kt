package com.example.game.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.example.game.viewmodel.GameScreen
import com.example.game.viewmodel.GameViewModel

@Composable
fun GameApp(
    viewModel: GameViewModel,
    onExitHub: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    BackHandler(enabled = currentScreen != GameScreen.HUB) {
        viewModel.navigateBack()
    }

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
            GameScreen.HUB -> HubScreen(
                viewModel = viewModel,
                onExitHub = onExitHub
            )
            GameScreen.BATTLE -> BattleScreen(viewModel = viewModel)
            GameScreen.PARTY -> PartyScreen(viewModel = viewModel)
            GameScreen.EQUIPMENT -> PartyScreen(viewModel = viewModel)
            GameScreen.TROPHIES -> TrophyScreen(viewModel = viewModel)
            GameScreen.SHOP -> ShopScreen(viewModel = viewModel)
            GameScreen.BATTLE_RESULT -> BattleResultScreen(viewModel = viewModel)
            GameScreen.SETTINGS -> HubScreen(
                viewModel = viewModel,
                onExitHub = onExitHub
            )
        }
    }
}
