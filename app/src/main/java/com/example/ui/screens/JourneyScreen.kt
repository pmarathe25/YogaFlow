package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.viewmodel.YogaViewModel

import androidx.compose.ui.graphics.Color
import com.example.game.ui.components.MonsterRoadSelection
import com.example.game.ui.components.elementToColor
import com.example.game.ui.components.drawMonsterShape
import com.example.game.ui.components.TrophyModal
import com.example.db.Achievement
import com.example.game.model.Monster
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

@Composable
fun ExpandedDashboardScreen(
    viewModel: YogaViewModel,
    gameViewModel: GameViewModel,
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToBattle: () -> Unit,
    onNavigateToParty: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToTrophies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentLevelName by viewModel.currentLevelName.collectAsState()
    val levelProgress by viewModel.levelProgress.collectAsState()
    val gameSaveData by gameViewModel.saveData.collectAsState()
    val party by gameViewModel.party.collectAsState()
    val sparks = gameSaveData.sparks
    val gold = gameSaveData.gold

    var showMonsterRoad by remember { mutableStateOf(false) }
    var showLevelsDialog by remember { mutableStateOf(false) }
    var showTrophies by remember { mutableStateOf(false) }

    if (showLevelsDialog) {
        LevelsInfoDialog(
            currentLevel = currentLevel,
            totalXp = totalXp,
            onDismiss = { showLevelsDialog = false }
        )
    }

    if (showTrophies) {
        val achievements by viewModel.achievements.collectAsState()
        TrophyModal(
            achievements = achievements,
            earnedTrophyIds = gameSaveData.earnedTrophyIds,
            onDismiss = { showTrophies = false }
        )
    }

    if (showMonsterRoad) {
        MonsterRoadSelection(
            monsters = DataLoader.monsters,
            defeatedIds = gameSaveData.defeatedMonsterIds,
            partyMembers = party,
            onMonsterSelected = { monster ->
                gameViewModel.startBattle(monster.id)
                onNavigateToBattle()
                showMonsterRoad = false
            }
        )
    } else {
        val infiniteTransition = rememberInfiniteTransition()
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
        )
        val glow by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse)
        )
        val listState = rememberLazyListState()
        val normDefeated = remember(gameSaveData.defeatedMonsterIds) {
            gameSaveData.defeatedMonsterIds.map { it.lowercase() }.toSet()
        }

        LaunchedEffect(Unit) {
            val defeatedIds = gameSaveData.defeatedMonsterIds
            val furthestIdx = DataLoader.monsters.indexOfLast { m ->
                m.id.lowercase() in defeatedIds.map { it.lowercase() }
            }
            val targetIdx = (furthestIdx + 1).coerceAtMost(DataLoader.monsters.lastIndex)
            if (targetIdx > 0) {
                listState.animateScrollToItem(targetIdx)
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D1B2A))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val cx = w / 2f; val cy = h / 2f
                drawCircle(Color(0xFF4CAF50).copy(alpha = 0.08f + 0.04f * sin(pulse * 3f)), w * 0.5f, Offset(cx, cy))
                drawCircle(Color(0xFF81C784).copy(alpha = 0.05f + 0.03f * sin(glow * 2f)), w * 0.65f, Offset(cx, cy))
                for (i in 0..25) {
                    val sx = (i * 137.5f) % w; val sy = (i * 97.3f) % (h * 0.7f)
                    val twinkle = 0.3f + 0.4f * sin(i * 1.3f + glow * 4f)
                    drawCircle(Color.White.copy(alpha = twinkle * 0.4f), 1.2f, Offset(sx, sy))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Path of Zen",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE8F5E9)
                    )
                    Spacer(Modifier.weight(1f))
                    Text("⚡", fontSize = 16.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("$sparks",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("\uD83E\uDE99", fontSize = 16.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("$gold",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFD700))
                }

                Spacer(Modifier.height(24.dp))

                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        val c = size.width / 2f; val r = size.width * 0.45f
                        val innerR = size.width * 0.25f
                        drawCircle(Color(0xFF4CAF50).copy(alpha = 0.2f + 0.1f * sin(pulse * 2f)), r * 1.2f, Offset(c, c))
                        drawCircle(Color(0xFF66BB6A).copy(alpha = 0.15f), r * 1.0f, Offset(c, c), style = Stroke(width = 4f))
                        drawCircle(Color(0xFF388E3C).copy(alpha = 0.2f), innerR, Offset(c, c), style = Stroke(width = 2f))
                        val path = Path()
                        for (i in 0..359) {
                            val a = i * 0.01745f
                            val sr = innerR * (0.3f + 0.7f * (i / 360f))
                            val px = c + sr * kotlin.math.cos(a)
                            val py = c + sr * kotlin.math.sin(a)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, Color(0xFFA5D6A7).copy(alpha = 0.5f), style = Stroke(width = 2f))
                        val orbPulse = 0.6f + 0.4f * sin(pulse * 3f)
                        drawCircle(Color(0xFFE8F5E9).copy(alpha = orbPulse * 0.8f), innerR * 0.35f, Offset(c, c))
                        drawCircle(Color.White.copy(alpha = orbPulse * 0.3f), innerR * 0.5f, Offset(c, c))
                    }
                }

                Spacer(Modifier.height(24.dp))

                LevelCardCompact(
                    currentLevel = currentLevel,
                    currentLevelName = currentLevelName,
                    totalXp = totalXp,
                    levelProgress = levelProgress,
                    onClick = { showLevelsDialog = true }
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    "Encounters ahead:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF81C784)
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(DataLoader.monsters) { monster ->
                        val idx = DataLoader.monsters.indexOf(monster)
                        val isUnlocked = idx == 0 || normDefeated.contains(
                            DataLoader.monsters[idx - 1].id.lowercase()
                        )
                        val isDefeated = normDefeated.contains(monster.id.lowercase())
                        MonsterPreviewCircle(
                            monster = monster,
                            size = 52.dp,
                            isLocked = !isUnlocked,
                            isDefeated = isDefeated
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                Button(
                    onClick = { showMonsterRoad = true },
                    modifier = Modifier.width(240.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C).copy(alpha = 0.9f))
                ) {
                    Text("ENTER THE PATH", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp)
                }

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavCard(
                        title = "Party", icon = "👥",
                        subtitle = "${party.size} Heroes",
                        onClick = onNavigateToParty,
                        modifier = Modifier.weight(1f)
                    )
                    NavCard(
                        title = "Shop", icon = "🛒",
                        subtitle = "New Items",
                        onClick = onNavigateToShop,
                        modifier = Modifier.weight(1f)
                    )
                    NavCard(
                        title = "Trophies", icon = "🏆",
                        subtitle = "Achievements",
                        onClick = { showTrophies = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LevelCardCompact(
    currentLevel: Int,
    currentLevelName: String,
    totalXp: Int,
    levelProgress: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LVL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary)
                    Text("$currentLevel", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(currentLevelName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface)
                Text("$totalXp XP", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { levelProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }

            Text(
                "→",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun NavCard(
    title: String,
    icon: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MonsterPreviewCircle(monster: Monster, size: Dp = 44.dp, isLocked: Boolean = false, isDefeated: Boolean = false) {
    val elColor = elementToColor(monster.element)
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = this.size.width / 2f
            val alpha = if (isLocked) 0.3f else if (isDefeated) 0.5f else 1f
            drawCircle(elColor.copy(alpha = 0.2f * alpha), c, Offset(c, c))
            drawCircle(elColor.copy(alpha = 0.5f * alpha), c * 0.6f, Offset(c, c), style = Stroke(width = 2f))
            drawMonsterShape(c * 0.5f, c * 0.6f, c * 0.7f, monster.name,
                tint = if (isDefeated) Color.Gray else elColor.copy(alpha = alpha))
            if (isLocked) {
                drawCircle(Color(0xFF37474F).copy(alpha = 0.6f), c * 0.7f, Offset(c, c))
                drawCircle(Color(0xFF455A64).copy(alpha = 0.3f), c * 0.8f, Offset(c, c))
            }
        }
        if (isLocked) {
            Text("\uD83D\uDD12", fontSize = 24.sp, color = Color.White)
        }
    }
}
