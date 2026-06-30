package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel
import com.example.game.viewmodel.GameScreen

@Composable
fun BattleResultScreen(viewModel: GameViewModel) {
    val battleState by viewModel.battleState.collectAsState()
    val isVictory = battleState?.phase == BattlePhase.VICTORY
    val turnsTaken = battleState?.turnsTaken ?: 0

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                if (isVictory) androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF1B5E20), Color(0xFF0D1B2A))
                )
                else androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFFB71C1C), Color(0xFF0D1B2A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (isVictory) "★ VICTORY ★" else "✕ DEFEAT ✕",
                color = if (isVictory) Color(0xFFFFD740) else Color(0xFFFF4444),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Turns: $turnsTaken",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.navigateTo(GameScreen.HUB)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Return to Hub", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
