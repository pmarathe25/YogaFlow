package com.example.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.Monster
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel
import kotlin.math.sin

@Composable
fun HubScreen(
    onNavigateToBattle: (String) -> Unit,
    onExitHub: () -> Unit,
    model: GameViewModel
) {
    val saveData by model.saveData.collectAsState()
    val error by model.error.collectAsState()
    var showMap by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { model.refreshSync() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showMap) {
            MonsterRoadSelection(
                monsters = DataLoader.monsters,
                defeatedIds = saveData.defeatedMonsterIds,
                onMonsterSelected = { onNavigateToBattle(it.id) },
                onBack = { showMap = false }
            )
        } else {
            PortalEntrance(
                onEnter = { showMap = true },
                onExit = onExitHub
            )
        }

        error?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = { model.clearError() }) {
                        Text("OK")
                    }
                }
            ) {
                Text(msg)
            }
        }
    }
}

@Composable
private fun PortalEntrance(onEnter: () -> Unit, onExit: () -> Unit) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
    ) {
        // Animated background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Radial glow behind portal
            drawCircle(Color(0xFF4CAF50).copy(alpha = 0.08f + 0.04f * sin(pulse * 3f)), w * 0.5f, Offset(cx, cy))
            drawCircle(Color(0xFF81C784).copy(alpha = 0.05f + 0.03f * sin(glow * 2f)), w * 0.65f, Offset(cx, cy))

            // Stars
            for (i in 0..25) {
                val sx = (i * 137.5f) % w
                val sy = (i * 97.3f) % (h * 0.7f)
                val twinkle = 0.3f + 0.4f * sin(i * 1.3f + glow * 4f)
                drawCircle(Color.White.copy(alpha = twinkle * 0.4f), 1.2f, Offset(sx, sy))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(0.5f))

            // Portal icon (outer ring)
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val c = size.width / 2f
                    val outerR = size.width * 0.45f
                    val innerR = size.width * 0.25f

                    drawCircle(Color(0xFF4CAF50).copy(alpha = 0.2f + 0.1f * sin(pulse * 2f)), outerR * 1.2f, Offset(c, c))
                    drawCircle(Color(0xFF66BB6A).copy(alpha = 0.15f), outerR * 1.0f, Offset(c, c), style = Stroke(width = 4f))
                    drawCircle(Color(0xFF388E3C).copy(alpha = 0.2f), innerR, Offset(c, c), style = Stroke(width = 2f))

                    // Lotus / spiral
                    val path = Path()
                    for (i in 0..359) {
                        val a = i * 0.01745f
                        val r = innerR * (0.3f + 0.7f * (i / 360f))
                        val px = c + r * kotlin.math.cos(a)
                        val py = c + r * kotlin.math.sin(a)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(path, Color(0xFFA5D6A7).copy(alpha = 0.5f), style = Stroke(width = 2f))

                    // Center orb
                    val orbPulse = 0.6f + 0.4f * sin(pulse * 3f)
                    drawCircle(Color(0xFFE8F5E9).copy(alpha = orbPulse * 0.8f), innerR * 0.35f, Offset(c, c))
                    drawCircle(Color.White.copy(alpha = orbPulse * 0.3f), innerR * 0.5f, Offset(c, c))
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Path of Zen",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8F5E9)
            )
            Text(
                "Walk the path of enlightenment",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFA5D6A7),
                modifier = Modifier.padding(top = 4.dp)
            )

            // Preview of first monsters
            Spacer(Modifier.height(32.dp))
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
                    PreviewMonsterCircle(monster)
                }
                Text("...", color = Color(0xFF81C784), fontSize = 20.sp)
            }

            Spacer(Modifier.height(48.dp))

            // Enter button
            Button(
                onClick = onEnter,
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

            Spacer(Modifier.weight(0.5f))

            TextButton(onClick = onExit) {
                Text("Return to main menu", color = Color(0xFF81C784))
            }
        }
    }
}

@Composable
private fun PreviewMonsterCircle(monster: Monster) {
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
