package com.example.game.ui.components

import androidx.compose.runtime.*
import com.example.game.viewmodel.GameScreen
import com.example.game.viewmodel.GameViewModel

@Composable
fun GameApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        GameScreen.HUB -> HubScreen(viewModel)
        GameScreen.BATTLE -> BattleScreen(viewModel)
        GameScreen.PARTY -> PartyScreen(viewModel)
        GameScreen.EQUIPMENT -> EquipmentScreen(viewModel)
        GameScreen.TROPHIES -> TrophyScreen(viewModel)
        GameScreen.SHOP -> ShopScreen(viewModel)
        GameScreen.BATTLE_RESULT -> BattleResultScreen(viewModel)
        GameScreen.SETTINGS -> HubScreen(viewModel) // Fallback
    }
}
