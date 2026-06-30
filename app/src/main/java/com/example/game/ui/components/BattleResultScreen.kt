package com.example.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.model.*
import com.example.game.viewmodel.GameScreen
import com.example.game.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun BattleResultScreen(viewModel: GameViewModel) {
    val battleState by viewModel.battleState.collectAsState()
    val isVictory = battleState?.phase == BattlePhase.VICTORY
    val turnsTaken = battleState?.turnsTaken ?: 0

    var titleVisible by remember { mutableStateOf(false) }
    var statsVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }

    val pool = rememberParticlePool(200)

    LaunchedEffect(Unit) {
        titleVisible = true
        delay(400)
        statsVisible = true
        delay(400)
        buttonVisible = true
    }

    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
    )
    val titleScale by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f)
    )
    val statsAlpha by animateFloatAsState(
        targetValue = if (statsVisible) 1f else 0f,
        animationSpec = tween(400)
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (buttonVisible) 1f else 0f,
        animationSpec = tween(300)
    )

    // Victory / Defeat particle effects
    LaunchedEffect(isVictory) {
        if (isVictory) {
            while (true) {
                pool.emit(
                    EmitterConfig(
                        particlesPerSecond = 40,
                        spreadAngle = 45f,
                        force = -100f,
                        gravity = 80f,
                        colors = listOf(Color(0xFFFFD740), Color(0xFFFFA726), Color.White),
                        sizeRange = 2f..6f,
                        lifetimeRange = 30..60
                    ),
                    Offset(
                        200f + kotlin.random.Random.nextFloat() * 200f,
                        500f + kotlin.random.Random.nextFloat() * 200f
                    ),
                    4
                )
                delay(40)
            }
        } else {
            while (true) {
                pool.emit(
                    EmitterConfig(
                        particlesPerSecond = 20,
                        spreadAngle = 15f,
                        force = 120f,
                        gravity = 200f,
                        colors = listOf(Color(0xFFB71C1C).copy(alpha = 0.5f), Color(0xFFE53935).copy(alpha = 0.3f)),
                        sizeRange = 1f..4f,
                        lifetimeRange = 40..80
                    ),
                    Offset(
                        kotlin.random.Random.nextFloat() * 400f,
                        -20f
                    ),
                    3
                )
                delay(30)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                if (isVictory) Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B5E20), MaterialTheme.colorScheme.background)
                )
                else Brush.verticalGradient(
                    colors = listOf(Color(0xFFB71C1C), MaterialTheme.colorScheme.background)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (isVictory) "\u2605 VICTORY \u2605" else "\u2715 DEFEATED \u2715",
                color = if (isVictory) Color(0xFFFFD740) else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(titleAlpha).scale(titleScale)
            )

            Spacer(Modifier.height(12.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth().alpha(statsAlpha),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Turns: $turnsTaken",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.navigateTo(GameScreen.HUB) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp).alpha(buttonAlpha)
            ) {
                Text(
                    "Return to Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (pool.hasActive) {
                pool.update()
                pool.draw(this)
            }
        }
    }
}
