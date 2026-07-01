package com.example.game.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.game.model.*
import com.example.game.persistence.DataLoader
import kotlin.math.*

@Composable
fun ActionPanel(
    currentHero: HeroInstance,
    heroes: List<HeroInstance>,
    monsters: List<MonsterInstance>,
    skillCooldowns: Map<String, Int>,
    onSkill: (Skill, List<String>) -> Unit,
    onUltimate: () -> Unit,
    onComboById: (String) -> Unit,
    isTargeting: Boolean,
    selectedTargets: List<String>,
    onCancelTargeting: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    
    // Available combos for current hero
    val availableCombos = remember(heroes) {
        val aliveHeroNames = heroes
            .filter { it.currentHp > 0 && !it.isDead }
            .map { it.name }
            .toSet()
        DataLoader.combos.filter { combo ->
            currentHero.name in combo.requiredHeroes &&
            combo.requiredHeroes.all { name -> name in aliveHeroNames } &&
            combo.requiredHeroes.mapNotNull { name ->
                heroes.find { it.name == name }
            }.all { it.ultimateGauge >= 100 }
        }
    }
    
    // Animation for laying the card down
    var isUsingSkill by remember { mutableStateOf(false) }
    val useAnimProgress = animateFloatAsState(
        targetValue = if (isUsingSkill) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        finishedListener = {
            if (it == 1f) {
                // Animation finished, execute skill or wait for target
                val skill = (currentHero.skills + currentHero.ultimate).find { it.id == selectedCardId }
                if (skill != null) {
                    onSkill(skill, emptyList()) // VM will handle if targeting is needed
                    if (skill.targetType != TargetType.SINGLE_ALLY && skill.targetType != TargetType.SELF && skill.targetType != TargetType.SINGLE_ENEMY) {
                        selectedCardId = null
                        isUsingSkill = false
                    }
                }
            }
        }
    )

    // Execute skill if targeting is complete
    LaunchedEffect(selectedTargets.size) {
        if (selectedTargets.isNotEmpty() && selectedCardId != null && isUsingSkill) {
            val skill = (currentHero.skills + currentHero.ultimate).find { it.id == selectedCardId }
            if (skill != null) {
                onSkill(skill, selectedTargets)
                selectedCardId = null
                isUsingSkill = false
            }
        }
    }
    
    // Clear selection if targeting was canceled from VM
    LaunchedEffect(isTargeting) {
        if (!isTargeting && isUsingSkill) {
            selectedCardId = null
            isUsingSkill = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Wooden Table Background
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter)) {
            val w = size.width
            val h = size.height
            // Table top
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF5D4037), Color(0xFF3E2723))
                )
            )
            // Wood grain
            for (i in 0..10) {
                val y = h * i / 10f
                drawLine(
                    color = Color(0xFF21100B).copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(w, y + (sin(i.toFloat()) * 20f)),
                    strokeWidth = 2f
                )
            }
            // Rim
            drawRect(
                color = Color(0xFF21100B).copy(alpha = 0.4f),
                size = Size(w, 4f),
                topLeft = Offset(0f, 0f)
            )
        }

        // 1. Hand of Cards in Arc
        if (!isUsingSkill && !isTargeting) {
            HandOfCards(
                currentHero = currentHero,
                skillCooldowns = skillCooldowns,
                availableCombos = availableCombos,
                selectedCardId = selectedCardId,
                onCardSelect = { selectedCardId = if (selectedCardId == it) null else it },
                onUse = { skill ->
                    isUsingSkill = true
                },
                onComboSelect = { comboId ->
                    onComboById(comboId)
                }
            )
        }

        // 2. Skill Card Overlay (Selected or Using)
        if (selectedCardId != null) {
            val skill = (currentHero.skills + currentHero.ultimate).find { it.id == selectedCardId }
            if (skill != null) {
                val cooldown = skillCooldowns[skill.id] ?: 0 // ADDED
                // Background Dim (only if not yet "laid down")
                if (!isUsingSkill) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { selectedCardId = null }
                    )
                }
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SkillCard(
                        skill = skill,
                        isUltimate = skill.ultimateGain == 0,
                        ultReady = currentHero.ultimateGauge >= 100,
                        baseCooldown = skill.cooldown,
                        cooldownRemaining = cooldown,
                        isSelected = true,
                        onClick = { if (!isUsingSkill) selectedCardId = null },
                        onUse = {
                            if (skill.ultimateGain == 0 && currentHero.ultimateGauge < 100) return@SkillCard
                            isUsingSkill = true
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                rotationZ = 0f // Upright
                                if (isUsingSkill) {
                                    // "Laying on table" animation
                                    // Move from center to a bit lower and tilt flat
                                    val targetTy = 180.dp.toPx() // Lower onto the table
                                    val targetRotationX = 45f
                                    val targetScale = 0.8f
                                    
                                    translationY = lerp(0f, targetTy, useAnimProgress.value)
                                    rotationX = lerp(0f, targetRotationX, useAnimProgress.value)
                                    scaleX = lerp(1.2f, targetScale, useAnimProgress.value)
                                    scaleY = lerp(1.2f, targetScale, useAnimProgress.value)
                                } else {
                                    scaleX = 1.2f
                                    scaleY = 1.2f
                                }
                            }
                            .zIndex(20f)
                    )
                }
            }
        }
        
        // 3. Targeting Instructions
        if (isTargeting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Select a target by clicking them", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(16.dp))
                    TextButton(onClick = { 
                        onCancelTargeting()
                        isUsingSkill = false
                        selectedCardId = null
                    }) {
                        Text("CANCEL", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }


    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    (1 - fraction) * start + fraction * stop

@Composable
fun HandOfCards(
    currentHero: HeroInstance,
    skillCooldowns: Map<String, Int>,
    availableCombos: List<ComboSkill>,
    selectedCardId: String?,
    onCardSelect: (String) -> Unit,
    onUse: (Skill) -> Unit,
    onComboSelect: (String) -> Unit
) {
    val allCards: List<Any> = buildList {
        currentHero.skills.forEach { add(it) }
        if (currentHero.ultimateGauge >= 100) {
            add(currentHero.ultimate)
        }
        availableCombos.forEach { add(it) }
    }
    val cardCount = allCards.size

    var scrollOffset by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        scrollOffset += delta
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        allCards.forEachIndexed { index, item ->
            val centerIndex = (cardCount - 1) / 2f
            val relativeIndex = index - centerIndex + (scrollOffset / 150f)
            val rotation = relativeIndex * 12f
            val ty = (relativeIndex.pow(2) * 10f)
            val tx = relativeIndex * 85f

            when (item) {
                is Skill -> {
                    val isUlt = item.ultimateGain == 0
                    val ultReady = currentHero.ultimateGauge >= 100
                    val cooldown = skillCooldowns[item.id] ?: 0
                    val isSelected = selectedCardId == item.id

                    SkillCard(
                        skill = item,
                        isUltimate = isUlt,
                        ultReady = ultReady,
                        baseCooldown = item.cooldown,
                        cooldownRemaining = cooldown,
                        isSelected = isSelected,
                        onClick = { onCardSelect(item.id) },
                        onUse = { onUse(item) },
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = tx.dp.toPx()
                                translationY = ty.dp.toPx()
                                rotationZ = rotation
                                alpha = if (selectedCardId != null && !isSelected) 0.3f else 1f
                                scaleX = if (selectedCardId != null && !isSelected) 0.8f else 1f
                                scaleY = if (selectedCardId != null && !isSelected) 0.8f else 1f
                            }
                            .zIndex(index.toFloat())
                    )
                }
                is ComboSkill -> {
                    val isSelected = selectedCardId == item.id
                    ComboCard(
                        combo = item,
                        isSelected = isSelected,
                        onClick = {
                            selectedCardId?.let { onCardSelect(it) }
                            onComboSelect(item.id)
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = tx.dp.toPx()
                                translationY = ty.dp.toPx() - 20f
                                rotationZ = rotation
                                alpha = if (selectedCardId != null && !isSelected) 0.3f else 1f
                                scaleX = if (selectedCardId != null && !isSelected) 0.8f else 1f
                                scaleY = if (selectedCardId != null && !isSelected) 0.8f else 1f
                            }
                            .zIndex(index.toFloat())
                    )
                }
            }
        }
    }
}

@Composable
private fun ComboCard(
    combo: ComboSkill,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Card(
        modifier = modifier
            .width(150.dp)
            .height(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4A148C).copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 16.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(
                    width = 3.dp,
                    color = Color(0xFF9C27B0).copy(alpha = if (isSelected) 1f else glowAlpha),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("\uD83D\uDCA5", fontSize = 28.sp)

                Spacer(Modifier.height(4.dp))

                Text(
                    text = combo.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFCE93D8),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = combo.description,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color(0xFFE1BEE7).copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    maxLines = 3
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = combo.requiredHeroes.joinToString(" + "),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = Color(0xFFCE93D8).copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            if (isSelected) {
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                        .border(4.dp, Color(0xFF9C27B0).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                )
            }
        }
    }
}

@Composable
fun SkillCard(
    skill: Skill,
    isUltimate: Boolean,
    ultReady: Boolean,
    baseCooldown: Int = 0,
    cooldownRemaining: Int = 0,
    isSelected: Boolean,
    onClick: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnCooldown = cooldownRemaining > 0
    val canUse = if (isUltimate) ultReady else !isOnCooldown

    val bgColor = when {
        isOnCooldown -> Color(0xFFE0E0E0)
        isUltimate -> if (ultReady) Color(0xFFFFF9C4) else Color(0xFFEEEEEE)
        skill.healScaling != null -> Color(0xFFF1F8E9)
        skill.damageComponents.isNotEmpty() -> Color(0xFFFFF1F0)
        else -> Color(0xFFE1F5FE)
    }
    
    val borderColor = when {
        isOnCooldown -> Color.Gray
        isUltimate -> if (ultReady) Color(0xFFFFD700) else Color.Gray
        skill.healScaling != null -> Color(0xFF689F38)
        skill.damageComponents.isNotEmpty() -> Color(0xFFD32F2F)
        else -> Color(0xFF0288D1)
    }

    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Card(
        modifier = modifier
            .width(150.dp)
            .height(220.dp)
            .alpha(if (isOnCooldown) 0.8f else 1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 16.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(
                    width = if (isUltimate) 4.dp else 1.5.dp,
                    color = if (isUltimate) Color(0xFFFFD700).copy(alpha = if (ultReady) glowAlpha else 0.4f) else borderColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            // Sparkling effect for ready ultimate
            if (isUltimate && ultReady) {
                val sparkTransition = rememberInfiniteTransition()
                repeat(5) { i ->
                    val sparkX by sparkTransition.animateFloat(
                        initialValue = 0f, targetValue = 150f,
                        animationSpec = infiniteRepeatable(tween(1000 + i * 200), RepeatMode.Restart)
                    )
                    val sparkY by sparkTransition.animateFloat(
                        initialValue = 0f, targetValue = 220f,
                        animationSpec = infiniteRepeatable(tween(1500 - i * 100), RepeatMode.Reverse)
                    )
                    Box(
                        modifier = Modifier
                            .offset(sparkX.dp, sparkY.dp)
                            .size(2.dp)
                            .background(Color.White, CircleShape)
                            .alpha(glowAlpha)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(getSkillIcon(skill), fontSize = 32.sp, modifier = Modifier.alpha(if (isOnCooldown) 0.5f else 1f))

                    if (baseCooldown > 0 || isOnCooldown) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(
                                    if (isOnCooldown) Color.Red.copy(alpha = 0.8f)
                                    else Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isOnCooldown) "$cooldownRemaining" else "CD:$baseCooldown",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = if (isOnCooldown) Color.Gray else Color.Black
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = skill.getMechanicsDescription(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isOnCooldown) Color.LightGray else Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = skill.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 9.sp
                    ),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
                
                if (isSelected) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onUse,
                        enabled = canUse,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isOnCooldown) Color.Gray else borderColor)
                    ) {
                        Text(if (isOnCooldown) "COOLDOWN" else "USE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            // Ultimate Golden Glow - Pulse
            if (isUltimate && ultReady) {
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
                )
                // Glow layers
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .border(6.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(2.dp)
                        .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(14.dp))
                )
            }
        }
    }
}

fun getSkillIcon(skill: Skill): String {
    return when {
        skill.ultimateGain == 0 -> "\uD83D\uDD25"
        skill.healScaling != null -> "\u2728"
        skill.shieldScaling != null -> "\uD83D\uDEE1\uFE0F"
        skill.damageComponents.any { it.element == Element.FIRE } -> "\uD83D\uDD25"
        skill.damageComponents.any { it.element == Element.WATER } -> "\uD83C\uDF0A"
        skill.damageComponents.any { it.element == Element.AIR } -> "\uD83C\uDF2C\uFE0F"
        skill.damageComponents.any { it.element == Element.EARTH } -> "\u26F0\uFE0F"
        skill.damageComponents.any { it.element == Element.LIGHT } -> "\u2600\uFE0F"
        skill.damageComponents.any { it.element == Element.DARK || it.element == Element.SHADOW } -> "\uD83D\uDC7B"
        else -> "\u2694\uFE0F"
    }
}


