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
fun PartyScreen(viewModel: GameViewModel) {
    val party by viewModel.party.collectAsState()
    val unlockedHeroes = viewModel.getUnlockedHeroes()
    val availableHeroes = viewModel.getAvailableHeroes()

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
                "PARTY",
                color = Color(0xFF4488FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Limit: 5 heroes",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(16.dp))

            // Current party
            Text("Current Party", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (party.isEmpty()) {
                Text(
                    "No heroes in party.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(party) { hero ->
                        PartyHeroCard(
                            hero = hero,
                            onRemove = { viewModel.removeHeroFromParty(hero.heroId) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Available heroes
            if (availableHeroes.isNotEmpty() && party.size < 5) {
                Text("Available Heroes", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(availableHeroes) { hero ->
                        AvailableHeroCard(
                            hero = hero,
                            onAdd = { viewModel.addHeroToParty(hero.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
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
private fun PartyHeroCard(hero: HeroInstance, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x40000000))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(hero.name, color = elementToColor(hero.element), fontWeight = FontWeight.Bold)
                Text(
                    "Lv.${hero.level}  HP:${hero.currentHp}/${hero.maxHp}  ATK:${hero.atk}  SPD:${hero.spd}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            OutlinedButton(
                onClick = onRemove,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4444))
            ) {
                Text("Remove", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun AvailableHeroCard(hero: Hero, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x40000000))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(hero.name, color = elementToColor(hero.element), fontWeight = FontWeight.Bold)
                Text(
                    hero.description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.height(32.dp)
            ) {
                Text("+", fontSize = 14.sp)
            }
        }
    }
}
