package com.example.game.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.game.model.Monster
import com.example.game.model.MonsterDefinitions
import kotlin.math.sin

@Composable
fun MonsterRoadSelection(
    defeatedMonsterIds: Set<String>,
    onSelectMonster: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val monsters = MonsterDefinitions.allMonsters
    val listState = rememberLazyListState()
    
    // Auto-scroll to the first undefeated monster
    val sortedMonsters = remember { monsters.reversed() }
    val firstUndefeatedIndex = remember {
        val idx = sortedMonsters.indexOfLast { defeatedMonsterIds.contains(it.id) }
        if (idx == -1) sortedMonsters.size - 1 else (idx - 1).coerceAtLeast(0)
    }

    LaunchedEffect(Unit) {
        listState.animateScrollToItem(firstUndefeatedIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF388E3C)) // SOLID GRASS GREEN
    ) {
        // ─── Dirt Road Background ────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val w = size.width
            val h = size.height
            
            path.moveTo(w / 2f, h)
            for (y in h.toInt() downTo 0 step 10) {
                val xOffset = sin(y / 200f) * 80f
                path.lineTo(w / 2f + xOffset, y.toFloat())
            }
            
            drawPath(
                path = path,
                color = Color(0xFF8D6E63), // Dirt road color
                style = Stroke(width = 120f, cap = StrokeCap.Round)
            )
            
            // Road edges/texture
            drawPath(
                path = path,
                color = Color(0xFF795548),
                style = Stroke(width = 130f, cap = StrokeCap.Round)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "The Path of Zen",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                itemsIndexed(sortedMonsters) { index, monster ->
                    val isDefeated = defeatedMonsterIds.contains(monster.id)
                    val isUnlocked = index == sortedMonsters.size - 1 || 
                                     defeatedMonsterIds.contains(sortedMonsters[index + 1].id)
                    
                    val xOffset = sin((monsters.size - index) * 0.8f) * 60f
                    
                    MonsterNode(
                        monster = monster,
                        isUnlocked = isUnlocked,
                        isDefeated = isDefeated,
                        isBoss = monster.isBoss,
                        modifier = Modifier.offset(x = xOffset.dp),
                        onClick = { if (isUnlocked) onSelectMonster(monster.id) }
                    )
                }
            }
        }
        
        // Flowers & Decor Decoration Overlay
        Decorations()
    }
}

@Composable
fun Decorations() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val flowerColors = listOf(Color(0xFFFF80AB), Color(0xFFF48FB1), Color(0xFFCE93D8))
        val rockColor = Color(0xFFBDBDBD)
        val grassColor = Color(0xFF81C784)
        val random = java.util.Random(42)
        
        repeat(50) {
            val fx = random.nextFloat() * size.width
            val fy = random.nextFloat() * size.height
            val roadX = size.width / 2f + sin(fy / 200f) * 80f
            
            if (kotlin.math.abs(fx - roadX) > 100f) {
                val type = random.nextInt(3)
                when (type) {
                    0 -> { // Flower
                        drawCircle(flowerColors[random.nextInt(3)], 6f, Offset(fx, fy))
                        drawCircle(Color.Yellow, 2f, Offset(fx, fy))
                    }
                    1 -> { // Rock
                        drawCircle(rockColor, 4f + random.nextFloat() * 4f, Offset(fx, fy))
                    }
                    2 -> { // Grass tuft
                        drawLine(grassColor, Offset(fx, fy), Offset(fx - 4f, fy - 8f), strokeWidth = 2f)
                        drawLine(grassColor, Offset(fx, fy), Offset(fx + 4f, fy - 8f), strokeWidth = 2f)
                    }
                }
            }
        }
    }
}

@Composable
fun MonsterNode(
    monster: Monster,
    isUnlocked: Boolean,
    isDefeated: Boolean,
    isBoss: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isUnlocked) Color.White.copy(alpha = 0.9f) else Color.Gray.copy(alpha = 0.5f))
            .clickable(enabled = isUnlocked) { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isUnlocked) elementToColor(monster.element).copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.White)
            } else {
                Text(if (isBoss) "💀" else "👾", fontSize = 32.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        
        Text(
            text = monster.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) Color.Black else Color.DarkGray
        )
        Text(
            text = monster.englishName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        if (isUnlocked) {
            Spacer(Modifier.height(4.dp))
            if (isDefeated) {
                Text("✓ DEFEATED", color = Color(0xFF2E7D32), fontWeight = FontWeight.Black, fontSize = 10.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF44336))
                    Text("CHALLENGE", color = Color(0xFFF44336), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
    }
}
