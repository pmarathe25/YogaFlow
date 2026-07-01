package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.game.viewmodel.GameViewModel
import com.example.game.ui.components.MonsterRoadSelection

@Composable
fun HubScreen(
    viewModel: GameViewModel,
    onExitHub: () -> Unit
) {
    val saveData by viewModel.saveData.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshSync() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Monster selection Road Overlay (Always shown now as Hub is just the road)
        MonsterRoadSelection(
            defeatedMonsterIds = saveData.defeatedMonsterIds,
            onSelectMonster = { monsterId ->
                viewModel.startBattle(monsterId)
            },
            onDismiss = onExitHub
        )

        // Error snackbar
        error?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            ) {
                Text(msg)
            }
        }
    }
}
