package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.model.Trophy
import com.example.game.model.TrophyDefinitions
import com.example.game.model.TrophyRarity
import com.example.ui.components.AchievementBadgeCard
import com.example.viewmodel.Achievement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrophyModal(
    achievements: List<Achievement>,
    earnedTrophyIds: Set<String>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your Achievements",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TabRow(
                    selectedTabIndex = 0, // Simplified for now
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[0]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(selected = true, onClick = {}, text = { Text("Badges & Trophies") })
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // 1. Harmony Badges (Yoga Achievements)
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        SectionHeader("Harmony Badges")
                    }
                    items(achievements) { achievement ->
                        AchievementBadgeCard(achievement = achievement, modifier = Modifier.fillMaxWidth())
                    }

                    // 2. Zen Battle Trophies
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Zen Battle Trophies")
                    }
                    items(TrophyDefinitions.all()) { trophy ->
                        val isEarned = earnedTrophyIds.contains(trophy.id)
                        BattleTrophyCard(trophy = trophy, isEarned = isEarned)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun BattleTrophyCard(trophy: Trophy, isEarned: Boolean) {
    val alpha = if (isEarned) 1.0f else 0.4f
    val rarityColor = when (trophy.rarity) {
        TrophyRarity.BRONZE -> Color(0xFFCD7F32)
        TrophyRarity.SILVER -> Color(0xFFC0C0C0)
        TrophyRarity.GOLD -> Color(0xFFFFD700)
        TrophyRarity.PLATINUM -> Color(0xFFE5E4E2)
    }

    val cardBg = if (isEarned) rarityColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
    val borderColor = if (isEarned) rarityColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .alpha(alpha)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(rarityColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (trophy.category == com.example.game.model.TrophyCategory.BADGE) "🏅" else "🏆",
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = trophy.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = trophy.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
