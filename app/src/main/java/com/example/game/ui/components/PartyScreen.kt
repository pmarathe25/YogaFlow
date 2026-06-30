package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel

@Composable
fun PartyScreen(viewModel: GameViewModel) {
    val party by viewModel.party.collectAsState()
    val unlockedHeroes = viewModel.getUnlockedHeroes()
    val availableHeroes = viewModel.getAvailableHeroes()

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Party",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${party.size} / 5 heroes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))

            // Current party
            Text(
                "Current Party",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            if (party.isEmpty()) {
                Text(
                    "No heroes in party.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
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
                Text(
                    "Available Heroes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
private fun PartyHeroCard(hero: HeroInstance, onRemove: () -> Unit) {
    val heroColor = elementToColor(hero.element)

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(heroColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(heroColor)
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    hero.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = heroColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip("Lv", hero.level.toString())
                    StatChip("HP", "${hero.currentHp}/${hero.maxHp}")
                    StatChip("ATK", hero.atk.toString())
                    StatChip("SPD", hero.spd.toString())
                }
            }
            OutlinedButton(
                onClick = onRemove,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun AvailableHeroCard(hero: Hero, onAdd: () -> Unit) {
    val heroColor = elementToColor(hero.element)

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(heroColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(heroColor.copy(alpha = 0.6f))
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    hero.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = heroColor
                )
                Text(
                    hero.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            FilledTonalButton(
                onClick = onAdd,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    "+",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
