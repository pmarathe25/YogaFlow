package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.model.DifficultyTier.*
import com.example.game.viewmodel.GameScreen
import com.example.game.viewmodel.GameViewModel

@Composable
fun HubScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val party by viewModel.party.collectAsState()
    val error by viewModel.error.collectAsState()
    var showMonsterSelection by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B2838))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "ZEN BATTLE",
                color = Color(0xFF4488FF),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(
                text = "A Yoga Journey",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(24.dp))

            // Stats row
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x40000000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Yoga Lv", saveData.yogaLevel.toString())
                    StatItem("Sparks", saveData.sparks.toString())
                    StatItem("Wins", saveData.totalBattlesWon.toString())
                }
            }

            Spacer(Modifier.height(24.dp))

            // Continue Battle / Start Battle button
            Button(
                onClick = {
                    if (party.isNotEmpty()) {
                        showMonsterSelection = true
                    }
                },
                enabled = party.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0),
                    disabledContainerColor = Color(0xFF333333)
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    if (party.isNotEmpty()) "ENTER BATTLE" else "ADD HEROES TO PARTY",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (party.isNotEmpty()) Color.White else Color.Gray
                )
            }
            Spacer(Modifier.height(16.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavButton("PARTY", Color(0xFF4A148C)) { viewModel.navigateTo(GameScreen.PARTY) }
                NavButton("GEAR", Color(0xFF1B5E20)) { viewModel.navigateTo(GameScreen.EQUIPMENT) }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavButton("TROPHIES", Color(0xFFE65100)) { viewModel.navigateTo(GameScreen.TROPHIES) }
                NavButton("SHOP", Color(0xFF311B92)) { viewModel.navigateTo(GameScreen.SHOP) }
            }

            Spacer(Modifier.weight(1f))

            // Party preview
            if (party.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x40000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Party", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        party.forEach { hero ->
                            Text(
                                text = "${hero.name}  Lv.${hero.level}  HP:${hero.currentHp}/${hero.maxHp}",
                                color = elementToColor(hero.element),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

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

        // Monster selection dialog
        if (showMonsterSelection) {
            MonsterSelectionDialog(
                onSelectMonster = { monsterId ->
                    showMonsterSelection = false
                    viewModel.startBattle(monsterId)
                },
                onDismiss = { showMonsterSelection = false }
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color(0xFF4488FF),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MonsterSelectionDialog(
    onSelectMonster: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val monstersByTier = MonsterDefinitions.allMonsters.groupBy { it.difficultyTier }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Choose Opponent", fontWeight = FontWeight.Bold, color = Color(0xFF4488FF))
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                monstersByTier.entries.forEach { (tier, monsters) ->
                    item {
                        val tierColor = when (tier) {
                            EASY -> Color(0xFF4CAF50)
                            MEDIUM -> Color(0xFFFFC107)
                            HARD -> Color(0xFFFF9800)
                            BOSS -> Color(0xFFF44336)
                            SUPERBOSS -> Color(0xFF9C27B0)
                        }
                        Text(
                            text = tier.name,
                            color = tierColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    items(monsters) { monster ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectMonster(monster.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1B2838)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            elementToColor(monster.element),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = monster.englishName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = monster.name,
                                        color = elementToColor(monster.element),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "HP ${monster.baseHp}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RowScope.NavButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.weight(1f).height(48.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
