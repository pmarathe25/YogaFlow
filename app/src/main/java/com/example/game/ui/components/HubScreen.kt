package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.model.DifficultyTier.*
import com.example.game.viewmodel.GameScreen
import com.example.game.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(viewModel: GameViewModel) {
    val saveData by viewModel.saveData.collectAsState()
    val party by viewModel.party.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshSync() }
    var showMonsterSelection by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "ZEN BATTLE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    letterSpacing = 4.sp
                )
            )
            Text(
                text = "A Yoga Journey",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(24.dp))

            // Stats row
            var statsVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { statsVisible = true }

            AnimatedVisibility(
                visible = statsVisible,
                enter = fadeIn(animationSpec = spring(stiffness = 200f)) +
                        slideInVertically(animationSpec = spring(stiffness = 200f)) { it / 2 }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Yoga Lv", saveData.yogaLevel.toString())
                        StatItem("Sparks", saveData.sparks.toString())
                        StatItem("Wins", saveData.totalBattlesWon.toString())
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Party preview
            if (party.isNotEmpty()) {
                var partyVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { partyVisible = true }

                AnimatedVisibility(
                    visible = partyVisible,
                    enter = fadeIn(animationSpec = spring(stiffness = 150f)) +
                            scaleIn(animationSpec = spring(stiffness = 150f, dampingRatio = 0.6f))
                ) {
                    Column {
                        Text(
                            "Your Party",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(party) { hero ->
                                HeroAvatar(hero)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Enter Battle button (gradient)
            Button(
                onClick = {
                    if (party.isNotEmpty()) {
                        showMonsterSelection = true
                    }
                },
                enabled = party.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (party.isNotEmpty()) "Enter Battle" else "Add Heroes to Party",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Navigation grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavTile(
                    icon = "\uD83D\uDC65",
                    label = "Party",
                    onClick = { viewModel.navigateTo(GameScreen.PARTY) },
                    modifier = Modifier.weight(1f)
                )
                NavTile(
                    icon = "\u2699\uFE0F",
                    label = "Gear",
                    onClick = { viewModel.navigateTo(GameScreen.EQUIPMENT) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavTile(
                    icon = "\uD83C\uDFC6",
                    label = "Trophies",
                    onClick = { viewModel.navigateTo(GameScreen.TROPHIES) },
                    modifier = Modifier.weight(1f)
                )
                NavTile(
                    icon = "\uD83D\uDED2",
                    label = "Shop",
                    onClick = { viewModel.navigateTo(GameScreen.SHOP) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))
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

        // Monster selection bottom sheet
        if (showMonsterSelection) {
            ModalBottomSheet(
                onDismissRequest = { showMonsterSelection = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = MaterialTheme.shapes.large
            ) {
                MonsterSelectionSheet(
                    onSelectMonster = { monsterId ->
                        showMonsterSelection = false
                        viewModel.startBattle(monsterId)
                    },
                    onDismiss = { showMonsterSelection = false }
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HeroAvatar(hero: HeroInstance) {
    val color = elementToColor(hero.element)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = hero.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun NavTile(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MonsterSelectionSheet(
    onSelectMonster: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val monstersByTier = MonsterDefinitions.allMonsters.groupBy { it.difficultyTier }

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            "Choose Opponent",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
                items(monsters) { monster ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectMonster(monster.id) },
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(elementToColor(monster.element))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = monster.englishName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = monster.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = elementToColor(monster.element)
                                )
                            }
                            Text(
                                text = "HP ${monster.baseHp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
