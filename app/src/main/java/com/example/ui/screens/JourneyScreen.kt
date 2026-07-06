package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LevelDefinitions
import com.example.ui.components.*
import com.example.viewmodel.YogaViewModel

import androidx.compose.ui.graphics.Color
import com.example.game.ui.components.TrophyModal
import com.example.game.ui.components.MonsterRoadSelection
import com.example.game.ui.components.elementToColor
import com.example.game.ui.components.drawMonsterShape
import com.example.game.model.Monster
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel
import com.example.game.viewmodel.GameScreen
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
    modifier: Modifier = Modifier
) {
    var showTrophies by remember { mutableStateOf(false) }
    val achievements by viewModel.achievements.collectAsState()
    val gameSaveData by gameViewModel.saveData.collectAsState()

    if (showTrophies) {
        TrophyModal(
            achievements = achievements,
            earnedTrophyIds = gameSaveData.earnedTrophyIds,
            onDismiss = { showTrophies = false }
        )
    }

    var showMonsterRoad by remember { mutableStateOf(false) }

    if (showMonsterRoad) {
        MonsterRoadSelection(
            monsters = DataLoader.monsters,
            defeatedIds = gameSaveData.defeatedMonsterIds,
            partyMembers = gameSaveData.party,
            onMonsterSelected = { monster ->
                gameViewModel.startBattle(monster.id)
                onNavigateToBattle()
                showMonsterRoad = false
            },
            onBack = { showMonsterRoad = false }
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Your Journey",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showTrophies = true }) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophies",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StatisticsPanel(
                viewModel = viewModel, 
                gameViewModel = gameViewModel,
                onNavigateToBattle = onNavigateToBattle,
                onEnterPath = { showMonsterRoad = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    }
}

@Composable
fun StatisticsPanel(
    viewModel: YogaViewModel,
    gameViewModel: GameViewModel,
    onNavigateToBattle: () -> Unit,
    onEnterPath: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentLevelName by viewModel.currentLevelName.collectAsState()
    val levelProgress by viewModel.levelProgress.collectAsState()
    val dailyQuestCompleted by viewModel.dailyQuestCompleted.collectAsState()
    val gameSaveData by gameViewModel.saveData.collectAsState()
    val party by gameViewModel.party.collectAsState()

    var showLevelsDialog by remember { mutableStateOf(false) }
    var activeDialogType by remember { mutableStateOf<String?>(null) }

    if (showLevelsDialog) {
        LevelsInfoDialog(
            currentLevel = currentLevel,
            totalXp = totalXp,
            onDismiss = { showLevelsDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { showLevelsDialog = true }
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )
                .padding(16.dp)
                .testTag("lvl_section_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LVL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "$currentLevel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = currentLevelName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total Sessions: $totalSessions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            text = "$totalXp XP",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = LevelDefinitions.remainingXpToNextLevel(totalXp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (dailyQuestCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (dailyQuestCompleted) "✨" else "🌸",
                    style = MaterialTheme.typography.titleLarge
                )
                Column {
                    Text(
                        text = if (dailyQuestCompleted) "Today's Quest Completed!" else "Today's Practice Quest",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (dailyQuestCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (dailyQuestCompleted) 
                            "You've earned today's Daily Zen Spark and +150 XP bonus!" 
                            else "Complete any yoga flow today to earn today's Daily Zen Spark and +150 XP bonus!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                value = "${gameSaveData.sparks}",
                label = "Zen Sparks",
                icon = Icons.Default.Favorite,
                color = MaterialTheme.colorScheme.tertiary,
                onClick = { activeDialogType = if (activeDialogType == "sparks") null else "sparks" },
                isSelected = activeDialogType == "sparks",
                modifier = Modifier.weight(1f)
            )
            val availableGold = gameSaveData.gold
            StatCard(
                value = "$availableGold",
                label = "Zen Gold",
                icon = Icons.Default.MonetizationOn,
                color = Color(0xFFFFD700),
                onClick = { activeDialogType = if (activeDialogType == "gold") null else "gold" },
                isSelected = activeDialogType == "gold",
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(
            visible = activeDialogType != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (activeDialogType != null) {
                val infoTitle = when (activeDialogType) {
                    "gold" -> "Zen Gold 🪙"
                    else -> "Zen Sparks ✨"
                }
                val infoDescription = when (activeDialogType) {
                    "gold" -> "Zen Gold is earned through Karma XP. Use it in the Shop to buy powerful gear for your heroes."
                    else -> "Zen Sparks represent daily mindfulness and consistent practice. You earn exactly 1 Zen Spark for each unique calendar day you practice. Each Zen Spark also grants a +150 XP bonus! Use Sparks to level up your heroes. Collect 3 Zen Sparks on different days to unlock the 'Zen Spark Collector' badge."
                }
                val infoIcon = when (activeDialogType) {
                    "gold" -> Icons.Default.MonetizationOn
                    else -> Icons.Default.Favorite
                }
                val infoColor = when (activeDialogType) {
                    "gold" -> Color(0xFFFFD700)
                    else -> MaterialTheme.colorScheme.tertiary
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = infoColor.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(1.dp, infoColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(infoColor.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = infoIcon,
                                contentDescription = null,
                                tint = infoColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = infoTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { activeDialogType = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss info",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = infoDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "ZEN BATTLE HUB",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavCard(
                title = "Party",
                icon = "👥",
                subtitle = "${party.size} Heroes",
                onClick = { 
                    gameViewModel.navigateTo(GameScreen.PARTY)
                    onNavigateToBattle()
                },
                modifier = Modifier.weight(1f)
            )
            NavCard(
                title = "Shop",
                icon = "🛒",
                subtitle = "New Items",
                onClick = { 
                    gameViewModel.navigateTo(GameScreen.SHOP)
                    onNavigateToBattle()
                },
                modifier = Modifier.weight(1f)
            )
        }

        // ── Inline Path of Zen Portal ──
        val infiniteTransition = rememberInfiniteTransition()
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse)
        )
        val glow by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse)
        )
        val previewMonsters = remember { DataLoader.monsters.take(3) }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0D1B2A))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                drawCircle(Color(0xFF4CAF50).copy(alpha = 0.08f + 0.04f * sin(pulse * 3f)), w * 0.5f, Offset(cx, cy))
                drawCircle(Color(0xFF81C784).copy(alpha = 0.05f + 0.03f * sin(glow * 2f)), w * 0.65f, Offset(cx, cy))
                for (i in 0..25) {
                    val sx = (i * 137.5f) % w
                    val sy = (i * 97.3f) % (h * 0.7f)
                    val twinkle = 0.3f + 0.4f * sin(i * 1.3f + glow * 4f)
                    drawCircle(Color.White.copy(alpha = twinkle * 0.4f), 1.2f, Offset(sx, sy))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val c = size.width / 2f
                        val outerR = size.width * 0.45f
                        val innerR = size.width * 0.25f
                        drawCircle(Color(0xFF4CAF50).copy(alpha = 0.2f + 0.1f * sin(pulse * 2f)), outerR * 1.2f, Offset(c, c))
                        drawCircle(Color(0xFF66BB6A).copy(alpha = 0.15f), outerR * 1.0f, Offset(c, c), style = Stroke(width = 4f))
                        drawCircle(Color(0xFF388E3C).copy(alpha = 0.2f), innerR, Offset(c, c), style = Stroke(width = 2f))
                        val path = Path()
                        for (i in 0..359) {
                            val a = i * 0.01745f
                            val r = innerR * (0.3f + 0.7f * (i / 360f))
                            val px = c + r * kotlin.math.cos(a)
                            val py = c + r * kotlin.math.sin(a)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, Color(0xFFA5D6A7).copy(alpha = 0.5f), style = Stroke(width = 2f))
                        val orbPulse = 0.6f + 0.4f * sin(pulse * 3f)
                        drawCircle(Color(0xFFE8F5E9).copy(alpha = orbPulse * 0.8f), innerR * 0.35f, Offset(c, c))
                        drawCircle(Color.White.copy(alpha = orbPulse * 0.3f), innerR * 0.5f, Offset(c, c))
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Path of Zen",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE8F5E9)
                )
                Text(
                    "Walk the path of enlightenment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA5D6A7),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    "Encounters ahead:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF81C784)
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewMonsters.forEach { monster ->
                        MonsterPreviewCircle(monster)
                    }
                    Text("...", color = Color(0xFF81C784), fontSize = 20.sp)
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onEnterPath,
                    modifier = Modifier
                        .width(220.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C).copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        "ENTER THE PATH",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
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
private fun MonsterPreviewCircle(monster: Monster) {
    val elColor = elementToColor(monster.element)
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = size.width / 2f
            drawCircle(elColor.copy(alpha = 0.2f), c, Offset(c, c))
            drawCircle(elColor.copy(alpha = 0.5f), c * 0.6f, Offset(c, c), style = Stroke(width = 2f))
            drawMonsterShape(c * 0.5f, c * 0.6f, c * 0.7f, monster.name, elColor)
        }
    }
}
