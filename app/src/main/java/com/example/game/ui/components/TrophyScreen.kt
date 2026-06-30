package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun TrophyScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val earnedTrophies = saveData.earnedTrophyIds

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B2838))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "TROPHIES",
                color = Color(0xFF4488FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${earnedTrophies.size} / ${TrophyDefinitions.allTrophies.size} earned",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(TrophyDefinitions.allTrophies) { trophy ->
                    val earned = trophy.id in earnedTrophies
                    TrophyCard(trophy = trophy, earned = earned)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Badges
            Text(
                "Badges: ${earnedTrophies.size}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.navigateBack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun TrophyCard(trophy: Trophy, earned: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (earned) Color(0x402E7D32) else Color(0x40000000)
        ),
        border = if (earned) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF66BB6A).copy(alpha = 0.5f))
        ) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (earned) "★" else "☆",
                color = if (earned) Color(0xFFFFD740) else Color.Gray,
                fontSize = 18.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trophy.name,
                    color = if (earned) Color.White else Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = trophy.description,
                    color = Color.White.copy(alpha = if (earned) 0.6f else 0.3f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
