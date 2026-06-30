package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.YogaViewModel

@Composable
fun YogaPlayerScreen(
    viewModel: YogaViewModel,
    onExit: () -> Unit
) {
    val flow by viewModel.flow.collectAsState()
    val currentPoseIndex by viewModel.currentPoseIndex.collectAsState()
    val currentPose by viewModel.currentPose.collectAsState()
    val remainingTimeSec by viewModel.remainingTimeSec.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val speechState by viewModel.speechState.collectAsState()

    if (currentPose == null) return

    val pose = currentPose!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Immersive Player Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.testTag("player_exit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit to Dashboard",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Pose ${currentPoseIndex + 1} of ${flow.poses.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            // Empty placeholder for centering balance
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Sanskrit and English Headers
        Text(
            text = pose.sanskritName,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = pose.englishName,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Center Content: Front and Side Angles Side-by-Side Sketchpads
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Front Angle Sketchpad
            YogaPoseVisual(
                poseId = pose.id,
                angle = PoseAngle.FRONT,
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
            )

            // Side Angle Sketchpad
            YogaPoseVisual(
                poseId = pose.id,
                angle = PoseAngle.SIDE,
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Timer and Player Navigation Panel (Combined for ergonomic efficiency)
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.primaryContainer
        val isDarkTheme = MaterialTheme.colorScheme.background == DarkBackground
        val glassBorder = if (isDarkTheme) GlassBorderDark else GlassBorderLight

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Pose Button
            FilledTonalIconButton(
                onClick = { viewModel.skipBackward() },
                modifier = Modifier
                    .size(56.dp)
                    .testTag("skip_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Pose",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Centered Circular Countdown Timer which is also the Play/Pause Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp) // slightly larger for excellent touch target
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = glassBorder,
                        shape = CircleShape
                    )
                    .clickable { viewModel.togglePlay() }
                    .drawBehind {
                        // Draw outer circular track
                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.4f),
                            radius = size.minDimension / 2 - 8.dp.toPx(),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Draw active countdown arc
                        val sweepAngle = (remainingTimeSec.toFloat() / 30f) * 360f
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                            size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    .testTag("countdown_circle")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${remainingTimeSec}s",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isPlaying) "HOLD POSE" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp
                        ),
                        color = if (isPlaying) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Next Pose Button
            FilledTonalIconButton(
                onClick = { viewModel.skipForward() },
                modifier = Modifier
                    .size(56.dp)
                    .testTag("skip_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Pose",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrolling Instructions and Benefits Card in high-fidelity GlassCard
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HOW TO HOLD IT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                pose.instructions.forEachIndexed { stepIdx, step ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "${stepIdx + 1}. ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "KEY BENEFITS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pose.benefits,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.restartSession() },
                modifier = Modifier.testTag("restart_session_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restart Practice", fontWeight = FontWeight.SemiBold)
            }

            val isMusicMuted by viewModel.isMusicMuted.collectAsState()
            val currentTrackIndex by viewModel.currentTrackIndex.collectAsState()
            val tracks = viewModel.ambientMusicManager.tracks
            val activeTrackName = if (currentTrackIndex in tracks.indices) tracks[currentTrackIndex].name else "Calm Sound"

            TextButton(
                onClick = { viewModel.setIsMusicMuted(!isMusicMuted) }
            ) {
                Icon(
                    imageVector = if (isMusicMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isMusicMuted) "Muted" else activeTrackName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SessionCompleteScreen(
    viewModel: YogaViewModel,
    onDone: () -> Unit
) {
    val flow by viewModel.flow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success check",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Namaste",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your practice is complete.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = flow.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "12",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Postures Held",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "360s",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Practice Time",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Text(
            text = "You have mindfully completed the 12 steps of the Sun Salutation flow. Carry this sense of calm, posture, and strength with you throughout your day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("complete_done_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Return to Dashboard",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CountdownStartScreen(
    viewModel: YogaViewModel,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countdownRemaining by viewModel.countdownRemaining.collectAsState()
    val preferredVoice by viewModel.preferredVoice.collectAsState()

    // Animate the countdown number whenever it changes for a pulsing effect
    val scale = remember(countdownRemaining) { Animatable(0.5f) }
    LaunchedEffect(countdownRemaining) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 200)
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Lotus flower icon
                Text(
                    text = "🌸",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = "Prepare Your Mind & Body",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                val firstPose = viewModel.flow.collectAsState().value.poses.firstOrNull()
                Text(
                    text = if (preferredVoice == "sa") {
                        "प्रथमं आसनं सिद्धं भवतु: ${firstPose?.sanskritName ?: ""}"
                    } else {
                        "Prepare for your first pose: ${firstPose?.englishName ?: ""}"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // The pulsing countdown circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = countdownRemaining.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 80.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value
                        )
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Beginning in a few moments...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Control actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel Practice")
                    }
                    Button(
                        onClick = { viewModel.skipCountdownAndStart() },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Start Now")
                    }
                }
            }
        }
    }
}
