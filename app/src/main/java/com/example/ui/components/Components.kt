package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioCueManager
import com.example.model.YogaFlow
import com.example.ui.theme.*
import com.example.viewmodel.Achievement
import com.example.viewmodel.YogaViewModel
import com.example.game.model.TrophyRarity
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.platform.testTag

@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val orbPurple = if (isDark) OrbPurpleDark else OrbPurpleLight
    val orbBlue = if (isDark) OrbBlueDark else OrbBlueLight
    val bg = if (isDark) DarkBackground else LightBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbPurple, Color.Transparent),
                        center = Offset(size.width * 0.25f, size.height * 0.25f),
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.25f, size.height * 0.25f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbBlue, Color.Transparent),
                        center = Offset(size.width * 0.75f, size.height * 0.75f),
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.75f, size.height * 0.75f)
                )
            }
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val borderColor = if (isDark) GlassBorderDark else GlassBorderLight
    
    Card(
        modifier = modifier
            .border(width = 1.dp, color = borderColor, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun FlowLibraryBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun FlowStatBadge(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun VolumeUpIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Box(
        modifier = modifier
            .drawBehind {
                val path = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.35f)
                    lineTo(size.width * 0.42f, size.height * 0.35f)
                    lineTo(size.width * 0.65f, size.height * 0.15f)
                    lineTo(size.width * 0.65f, size.height * 0.85f)
                    lineTo(size.width * 0.42f, size.height * 0.65f)
                    lineTo(size.width * 0.2f, size.height * 0.65f)
                    close()
                }
                drawPath(path = path, color = tint)
                
                drawArc(
                    color = tint,
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.35f, size.height * 0.2f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.6f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
    )
}

@Composable
fun SpeechStateBanner(
    speechState: AudioCueManager.SpeechState,
    onReplay: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = when (speechState) {
            is AudioCueManager.SpeechState.Loading -> MaterialTheme.colorScheme.surfaceVariant
            is AudioCueManager.SpeechState.Playing -> MaterialTheme.colorScheme.primaryContainer
            is AudioCueManager.SpeechState.Error -> MaterialTheme.colorScheme.errorContainer
            else -> Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                when (speechState) {
                    is AudioCueManager.SpeechState.Loading -> {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Loading Voice",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Synthesizing AI Spoken Guide...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha)
                        )
                    }
                    is AudioCueManager.SpeechState.Playing -> {
                        VolumeUpIcon(
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guided: Speaking via ${speechState.source}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    is AudioCueManager.SpeechState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Speech: Using fallback device TTS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    else -> {
                        // Silent state
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Audio cues ready",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Voice cue ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (speechState is AudioCueManager.SpeechState.Idle || speechState is AudioCueManager.SpeechState.Playing) {
                IconButton(
                    onClick = onReplay,
                    modifier = Modifier.size(24.dp)
                ) {
                    VolumeUpIcon(
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(
    title: String,
    subtitle: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp, 20.dp)
                    .background(color = badgeColor, shape = RoundedCornerShape(3.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 14.dp, top = 2.dp)
        )
    }
}

fun getColorForFlow(flowId: String): Color {
    return when (flowId) {
        "sun_salutation" -> Color(0xFFFFB74D)          // Amber/Orange
        "warrior_flow" -> Color(0xFFE57373)            // Light Red/Coral
        "restorative_yin" -> Color(0xFF64B5F6)          // Light Blue
        "morning_energizer" -> Color(0xFFFFF176)        // Yellow
        "bedtime_wind_down" -> Color(0xFF9575CD)        // Purple
        "core_balance" -> Color(0xFF4DB6AC)            // Teal
        "heart_opening_vinyasa" -> Color(0xFFF06292)   // Pink
        "power_vinyasa_ascent" -> Color(0xFFFF8A65)    // Deep Orange
        "ashtanga_core_power" -> Color(0xFFAED581)     // Light Green
        "advanced_balance_mastery" -> Color(0xFF4DD0E1) // Cyan
        else -> Color(0xFF81C784)                       // Default Green
    }
}

@Composable
fun FlowCard(
    flowItem: YogaFlow,
    isFavorite: Boolean,
    hasActiveReminders: Boolean = false,
    onToggleFavorite: () -> Unit,
    onViewFlowDetails: (YogaFlow) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewFlowDetails(flowItem) }
            .testTag("flow_library_card_${flowItem.id}"),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left color-coded vertical indicator bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        color = getColorForFlow(flowItem.id),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = flowItem.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("favorite_btn_${flowItem.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                if (hasActiveReminders) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Active Reminders",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reminders active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = flowItem.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowLibraryBadge(label = "${flowItem.poses.size} Poses")
                        FlowLibraryBadge(label = "${flowItem.totalDurationMinutes} Min")
                    }
                    
                    Button(
                        onClick = { onViewFlowDetails(flowItem) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.testTag("view_flow_btn_${flowItem.id}")
                    ) {
                        Text("Practice", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(if (isSelected) color.copy(alpha = 0.16f) else color.copy(alpha = 0.08f))
            .then(
                if (isSelected) Modifier.border(1.5.dp, color, RoundedCornerShape(16.dp))
                else Modifier
            )
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AchievementBadgeCard(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val alpha = if (achievement.isUnlocked) 1.0f else 0.4f
    
    val rarityColor = when (achievement.rarity) {
        TrophyRarity.BRONZE -> Color(0xFFCD7F32)
        TrophyRarity.SILVER -> Color(0xFFC0C0C0)
        TrophyRarity.GOLD -> Color(0xFFFFD700)
        TrophyRarity.PLATINUM -> Color(0xFFE5E4E2)
        null -> null
    }

    val cardBg = if (achievement.isUnlocked) {
        rarityColor?.copy(alpha = 0.15f) ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }
    
    val borderColor = if (achievement.isUnlocked) {
        rarityColor?.copy(alpha = 0.6f) ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    Box(
        modifier = modifier
            .width(135.dp)
            .background(color = cardBg, shape = RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, borderColor), shape = RoundedCornerShape(16.dp))
            .padding(10.dp)
            .alpha(alpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Badge Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (achievement.isUnlocked) {
                            rarityColor?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (achievement.id) {
                        "first_breath" -> "🌬️"
                        "zen_spark_collector" -> "✨"
                        "tri_fold_harmony" -> "🌿"
                        "yogi_adept" -> "🎯"
                        "deep_devotee" -> "🧘"
                        "practice_morning_light" -> "☀️"
                        "practice_hour_of_power" -> "⚡"
                        "practice_ten_sessions" -> "🔟"
                        else -> if (achievement.id.startsWith("badge_")) "🏅" else "🏆"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                minLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            Box(
                modifier = Modifier
                    .background(
                        color = if (achievement.isUnlocked) (rarityColor?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (achievement.isUnlocked) "UNLOCKED" else achievement.progressText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (achievement.isUnlocked) (rarityColor ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun DrawerStatisticsBadge(
    viewModel: YogaViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentLevelName by viewModel.currentLevelName.collectAsState()
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalSparks by viewModel.totalSparks.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mini Level Badge
        Box(
            modifier = Modifier
                .size(36.dp)
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
            Text(
                text = "$currentLevel",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Progress Summary info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$currentLevelName • $totalXp XP",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "🧘 $totalSessions • ✨ $totalSparks",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CompactStatisticsPanel(
    viewModel: YogaViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentLevelName by viewModel.currentLevelName.collectAsState()
    val levelProgress by viewModel.levelProgress.collectAsState()
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalSparks by viewModel.totalSparks.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mini Level Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                Text(
                    text = "$currentLevel",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Progress Summary info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$currentLevelName • $totalXp XP",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "🧘 $totalSessions • ✨ $totalSparks",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LinearProgressIndicator(
                    progress = { levelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }

            // Arrow to expand
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Expand Dashboard",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
