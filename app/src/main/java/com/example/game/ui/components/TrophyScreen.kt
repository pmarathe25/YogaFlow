package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel

@Composable
fun TrophyScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val earnedTrophies = saveData.earnedTrophyIds
    val totalTrophies = TrophyDefinitions.allTrophies.size
    val earnedCount = earnedTrophies.size

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Trophies",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            // Progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "$earnedCount / $totalTrophies",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LinearProgressIndicator(
                    progress = { if (totalTrophies > 0) earnedCount.toFloat() / totalTrophies else 0f },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = Color(0xFFFFD740),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(TrophyDefinitions.allTrophies) { trophy ->
                    val earned = trophy.id in earnedTrophies
                    TrophyCard(trophy = trophy, earned = earned)
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.navigateBack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Back",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TrophyCard(trophy: Trophy, earned: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (earned)
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = if (earned) CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(Color(0xFFFFD740).copy(alpha = 0.5f))
        ) else CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (earned) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (earned) "\u2605" else "\u2606",
                color = if (earned) Color(0xFFFFD740) else Color.Gray,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trophy.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (earned) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = trophy.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (earned) 0.6f else 0.3f
                    )
                )
            }
        }
    }
}
