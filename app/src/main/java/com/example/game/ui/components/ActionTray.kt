package com.example.game.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.game.model.*
import kotlin.math.pow

private sealed class CardEntry {
    data class Skill(val skill: com.example.game.model.Skill) : CardEntry()
    data class Ultimate(val skill: com.example.game.model.Skill) : CardEntry()
    data class Combo(val combo: ComboSkill) : CardEntry()
}

@Composable
fun ActionTray(
    currentHero: CombatantState,
    skillCooldowns: Map<String, Int>,
    availableCombos: List<ComboSkill>,
    isTargeting: Boolean,
    onSkill: (com.example.game.model.Skill) -> Unit,
    onComboById: (String) -> Unit,
    onCancelTargeting: () -> Unit,
    onCardDragStart: ((Color) -> Unit)? = null,
    onCardDragEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allCards = remember(currentHero, availableCombos) {
        buildList {
            currentHero.skills.forEach { add(CardEntry.Skill(it)) }
            if (currentHero.ultimate != null) add(CardEntry.Ultimate(currentHero.ultimate!!))
            availableCombos.forEach { add(CardEntry.Combo(it)) }
        }
    }
    Box(
        modifier = modifier.fillMaxWidth().height(400.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Wooden table background
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter)) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF5D4037), Color(0xFF3E2723))
                )
            )
            for (i in 0..10) {
                val y = h * i / 10f
                drawLine(
                    color = Color(0xFF21100B).copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(w, y + (kotlin.math.sin(i.toFloat()) * 20f)),
                    strokeWidth = 2f
                )
            }
            drawRect(
                color = Color(0xFF21100B).copy(alpha = 0.4f),
                size = Size(w, 4f),
                topLeft = Offset(0f, 0f)
            )
        }

        // Hand of Cards (when not in targeting mode)
        if (!isTargeting) {
            HandOfCards(
                currentHero = currentHero,
                skillCooldowns = skillCooldowns,
                availableCombos = availableCombos,
                onSkill = onSkill,
                onComboSelect = onComboById,
                onCardDragStart = onCardDragStart,
                onCardDragEnd = onCardDragEnd,
            )
        }

        // Targeting instruction overlay
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
                    TextButton(onClick = { onCancelTargeting() }) {
                        Text("CANCEL", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HandOfCards(
    currentHero: CombatantState,
    skillCooldowns: Map<String, Int>,
    availableCombos: List<ComboSkill>,
    onSkill: (com.example.game.model.Skill) -> Unit,
    onComboSelect: (String) -> Unit,
    onCardDragStart: ((Color) -> Unit)? = null,
    onCardDragEnd: (() -> Unit)? = null,
) {
    val allCards: List<Any> = buildList {
        currentHero.skills.forEach { add(it) }
        if (currentHero.ultimate != null) add(currentHero.ultimate!!)
        availableCombos.forEach { add(it) }
    }
    val cardCount = allCards.size
    val density = LocalDensity.current
    val thresholdPx = with(density) { 200.dp.toPx() }
    val tapPopPositionPx = with(density) { 130.dp.toPx() }
    var scrollOffset by remember { mutableStateOf(0f) }
    val (minScrollOffset, maxScrollOffset) = remember(cardCount) {
        val center = (cardCount - 1) / 2f
        val maxOff = center * 150f
        val minOff = -(cardCount - 1 - center) * 150f
        minOff to maxOff
    }
    var poppedCardIndex by remember { mutableIntStateOf(-1) }
    var dragActiveIndex by remember { mutableIntStateOf(-1) }
    var rawDragY by remember { mutableStateOf(0f) }
    var rawDragX by remember { mutableStateOf(0f) }
    var isPopped by remember { mutableStateOf(false) }
    var lastDragX by remember { mutableStateOf(0f) }
    val popThresholdPx = with(density) { 30.dp.toPx() }

    val isDragged = dragActiveIndex >= 0

    val displayDragY by animateFloatAsState(
        targetValue = if (isDragged) rawDragY else 0f,
        animationSpec = if (isDragged)
            snap()
        else
            spring(dampingRatio = 0.5f, stiffness = 500f)
    )

    val displayDragX by animateFloatAsState(
        targetValue = if (isDragged && isPopped) rawDragX else 0f,
        animationSpec = if (isDragged)
            snap()
        else
            spring(dampingRatio = 0.5f, stiffness = 500f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val draggableState = rememberDraggableState { delta ->
            if (poppedCardIndex >= 0) {
                poppedCardIndex = -1
                dragActiveIndex = -1
                isPopped = false
                rawDragY = 0f
            }
            scrollOffset = (scrollOffset + delta).coerceIn(minScrollOffset, maxScrollOffset)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .draggable(state = draggableState, orientation = Orientation.Horizontal),
            contentAlignment = Alignment.BottomCenter
        ) {
            allCards.forEachIndexed { index, item ->
                val centerIndex = (cardCount - 1) / 2f
                val relativeIndex = index - centerIndex + (scrollOffset / 150f)
                val rotation = relativeIndex * 12f
                val ty = (relativeIndex.pow(2) * 10f)
                val tx = relativeIndex * 85f

                val isDragged = dragActiveIndex == index

                val cardMod = Modifier
                    .graphicsLayer {
                        val dy = if (isDragged) displayDragY else 0f
                        val dx = if (isDragged && isPopped) displayDragX else 0f
                        translationX = tx.dp.toPx() + dx
                        translationY = ty.dp.toPx() + dy - (if (item is ComboSkill) 20f else 0f)
                        rotationZ = if (isDragged) 0f else rotation
                        if (isDragged) { scaleX = 1.15f; scaleY = 1.15f }
                    }
                    .zIndex(if (isDragged) 999f else index.toFloat())

                val dragMod = if (item is com.example.game.model.Skill || item is ComboSkill) {
                    val cardColor = getCardColor(item)
                    Modifier.pointerInput(index) {
                        detectVerticalDragGestures(
                            onDragStart = { startPos ->
                                val usable = if (item is com.example.game.model.Skill) {
                                    val s = item
                                    val isUlt = s.ultimateGain == 0
                                    if (isUlt) currentHero.gauge >= 100
                                    else (skillCooldowns[s.id] ?: 0) <= 0
                                } else true
                                if (usable) {
                                    if (dragActiveIndex == index && isPopped) {
                                        poppedCardIndex = -1
                                        lastDragX = startPos.x
                                    } else {
                                        poppedCardIndex = -1
                                        rawDragX = 0f
                                        rawDragY = 0f
                                        isPopped = false
                                        lastDragX = startPos.x
                                    }
                                    dragActiveIndex = index
                                    onCardDragStart?.invoke(cardColor)
                                }
                            },
                            onVerticalDrag = { change: PointerInputChange, dragAmountY: Float ->
                                if (poppedCardIndex < 0 || poppedCardIndex == index) {
                                    rawDragY += dragAmountY
                                    if (!isPopped) {
                                        if (rawDragY < -popThresholdPx) {
                                            isPopped = true
                                            lastDragX = change.position.x
                                        } else {
                                            lastDragX = change.position.x
                                        }
                                    } else {
                                        val currentX = change.position.x
                                        rawDragX += currentX - lastDragX
                                        lastDragX = currentX
                                        change.consume()
                                    }
                                }
                            },
                            onDragEnd = {
                                if (rawDragY < -thresholdPx) {
                                    rawDragY = 0f
                                    when (item) {
                                        is com.example.game.model.Skill -> {
                                            val skill = item
                                            val isUlt = skill.ultimateGain == 0
                                            val canUse = if (isUlt) currentHero.gauge >= 100
                                                else (skillCooldowns[skill.id] ?: 0) <= 0
                                            if (canUse) onSkill(skill)
                                        }
                                        is ComboSkill -> onComboSelect(item.id)
                                    }
                                }
                                dragActiveIndex = -1
                                poppedCardIndex = -1
                                isPopped = false
                                onCardDragEnd?.invoke()
                            },
                            onDragCancel = {
                                dragActiveIndex = -1
                                poppedCardIndex = -1
                                isPopped = false
                                onCardDragEnd?.invoke()
                            }
                        )
                    }
                } else Modifier

                val tapMod = if (item is com.example.game.model.Skill || item is ComboSkill) {
                    Modifier.pointerInput(index) {
                        detectTapGestures {
                            if (item is com.example.game.model.Skill) {
                                val skill = item
                                val isUlt = skill.ultimateGain == 0
                                val isUsable = if (isUlt) currentHero.gauge >= 100
                                    else (skillCooldowns[skill.id] ?: 0) <= 0
                                if (!isUsable) return@detectTapGestures
                            }
                            if (poppedCardIndex == index) {
                                poppedCardIndex = -1
                                dragActiveIndex = -1
                                isPopped = false
                                rawDragY = 0f
                            } else {
                                poppedCardIndex = index
                                dragActiveIndex = index
                                isPopped = true
                                rawDragY = -tapPopPositionPx
                                rawDragX = 0f
                            }
                        }
                    }
                } else Modifier

                when (item) {
                    is com.example.game.model.Skill -> {
                        val isUlt = item.ultimateGain == 0
                        val ultReady = currentHero.gauge >= 100
                        val cooldown = skillCooldowns[item.id] ?: 0

                        SkillCard(
                            skill = item,
                            heroColor = elementToColor(currentHero.element),
                            isUltimate = isUlt,
                            ultReady = ultReady,
                            heroLevel = currentHero.level,
                            baseCooldown = item.cooldown,
                            cooldownRemaining = cooldown,
                            modifier = Modifier.width(150.dp).height(220.dp).then(cardMod).then(dragMod).then(tapMod)
                        )
                    }
                    is ComboSkill -> {
                        ComboCard(
                            combo = item,
                            modifier = Modifier.width(150.dp).height(220.dp).then(cardMod).then(dragMod).then(tapMod)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ComboCard(
    combo: ComboSkill,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A148C).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(
                    width = 3.dp,
                    color = Color(0xFF9C27B0).copy(alpha = glowAlpha),
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
        }
    }
}

@Composable
internal fun SkillCard(
    skill: com.example.game.model.Skill,
    heroColor: Color,
    isUltimate: Boolean,
    ultReady: Boolean,
    heroLevel: Int,
    baseCooldown: Int = 0,
    cooldownRemaining: Int = 0,
    modifier: Modifier = Modifier
) {
    val isOnCooldown = cooldownRemaining > 0 || (isUltimate && !ultReady)
    val showCooldown = baseCooldown > 1
    val displayText = if (isOnCooldown) "$cooldownRemaining" else "$baseCooldown"

    val bgColor = when {
        isOnCooldown -> Color(0xFFE0E0E0)
        isUltimate   -> Color(0xFFFFF9C4)
        else         -> heroColor.copy(alpha = 0.25f)
    }

    val borderColor = when {
        isOnCooldown                     -> Color.Gray
        isUltimate                       -> Color(0xFFFFD700)
        skill.healScaling != null        -> Color(0xFF689F38)
        skill.damageComponents.isNotEmpty() -> Color(0xFFD32F2F)
        skill.shieldScaling != null      -> Color(0xFF0288D1)
        skill.buffs.isNotEmpty()         -> Color(0xFFFFA000)
        else                             -> Color(0xFF0288D1)
    }

    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Card(
        modifier = modifier
            .alpha(if (isOnCooldown) 0.8f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
            Box(modifier = Modifier.fillMaxSize()) {
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(getSkillIcon(skill), fontSize = 32.sp, modifier = Modifier.alpha(if (isOnCooldown) 0.5f else 1f))
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
                        text = skill.getMechanicsDescription(heroLevel),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isOnCooldown) Color.LightGray else Color.DarkGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp,
                        // natural height
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
                }

                // Ultimate Golden Glow - Pulse
                if (isUltimate && ultReady) {
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
                    )
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

                // Cooldown badge (top-left)
                if (showCooldown) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(24.dp)
                            .background(
                                if (isOnCooldown) Color(0xFFFFCDD2)
                                else Color.Black.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnCooldown) Color(0xFFE53935)
                                    else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

internal fun getSkillIcon(skill: com.example.game.model.Skill): String {
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

private fun getCardColor(item: Any): Color {
    return when (item) {
        is com.example.game.model.Skill -> {
            if (item.ultimateGain == 0) Color(0xFFFFD700)
            else when {
                item.healScaling != null -> Color(0xFF66BB6A)
                item.shieldScaling != null -> Color(0xFF42A5F5)
                item.damageComponents.any { it.element == Element.FIRE } -> Color(0xFFE53935)
                item.damageComponents.any { it.element == Element.WATER } -> Color(0xFF1E88E5)
                item.damageComponents.any { it.element == Element.AIR } -> Color(0xFFB0BEC5)
                item.damageComponents.any { it.element == Element.EARTH } -> Color(0xFF795548)
                item.damageComponents.any { it.element == Element.LIGHT } -> Color(0xFFFFF176)
                item.damageComponents.any { it.element == Element.DARK || it.element == Element.SHADOW } -> Color(0xFF7B1FA2)
                else -> Color(0xFF9E9E9E)
            }
        }
        is ComboSkill -> Color(0xFF9C27B0)
        else -> Color.Gray
    }
}


