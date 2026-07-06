package com.example.game.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel

@Composable
fun HubScreen(
    onNavigateToBattle: (String) -> Unit,
    onExitHub: () -> Unit,
    model: GameViewModel
) {
    val saveData by model.saveData.collectAsState()
    val error by model.error.collectAsState()

    LaunchedEffect(Unit) { model.refreshSync() }

    Box(modifier = Modifier.fillMaxSize()) {
        MonsterRoadSelection(
            monsters = DataLoader.monsters,
            defeatedIds = saveData.defeatedMonsterIds,
            onMonsterSelected = { onNavigateToBattle(it.id) },
            onBack = onExitHub
        )

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
