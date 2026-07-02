package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.game.viewmodel.GameViewModel

enum class HubView { DASHBOARD, PATH_OF_ZEN }

@Composable
fun HubScreen(
    onNavigateToBattle: (String) -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToParty: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrophies: () -> Unit,
    model: GameViewModel
) {
    val saveData by model.saveData.collectAsState()
    val error by model.error.collectAsState()

    var currentHubView by remember { mutableStateOf(HubView.DASHBOARD) }

    LaunchedEffect(Unit) { model.refreshSync() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentHubView) {
            HubView.DASHBOARD -> YourJourneyDashboard(
                onNavigateToPath = { currentHubView = HubView.PATH_OF_ZEN },
                onNavigateToShop = onNavigateToShop,
                onNavigateToParty = onNavigateToParty,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToTrophies = onNavigateToTrophies,
                model = model
            )
            HubView.PATH_OF_ZEN -> MonsterRoadSelection(
                defeatedMonsterIds = saveData.defeatedMonsterIds,
                onSelectMonster = onNavigateToBattle,
                onDismiss = { currentHubView = HubView.DASHBOARD }
            )
        }

        error?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = { model.clearError() }) {
                        Text("OK")
                    }
                }
            ) {
                Text(msg)
            }
        }
    }
}
