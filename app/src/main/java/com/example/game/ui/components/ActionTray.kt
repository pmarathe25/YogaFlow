package com.example.game.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.lerp
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
import com.example.game.persistence.DataLoader
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow


@Composable
fun ActionTray(
    currentHero: CombatantState,
    turnOrder: List<BattleActor>,
    currentTurnIndex: Int,
    skillCooldowns: Map<String, Int>,
    availableCombos: List<ComboSkill>,
    isTargeting: Boolean,
    onSkill: (com.example.game.model.Skill) -> Unit,
    onComboById: (String) -> Unit,
    onCardDragStart: ((Color) -> Unit)? = null,
    onCardDragEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
                turnOrder = turnOrder,
                currentTurnIndex = currentTurnIndex,
                skillCooldowns = skillCooldowns,
                availableCombos = availableCombos,
                onSkill = onSkill,
                onComboSelect = onComboById,
                onCardDragStart = onCardDragStart,
                onCardDragEnd = onCardDragEnd,
            )
        }

        // Targeting instruction overlay removed — now in BattleScene
    }
}

@Composable
private fun HandOfCards(
    currentHero: CombatantState,
    turnOrder: List<BattleActor>,
    currentTurnIndex: Int,
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
    if (cardCount == 0) return

    val density = LocalDensity.current
    val thresholdPx = with(density) { 200.dp.toPx() }
    val popPositionPx = with(density) { 130.dp.toPx() }
    val popThresholdPx = with(density) { 30.dp.toPx() }
    val cardWidthDp = 150.dp
    val cardHeightDp = 220.dp
    val cardSpacingPx = with(density) { 85.dp.toPx() }
    val arcHeightFactorPx = with(density) { 10.dp.toPx() }
    val cardWidthPx = with(density) { cardWidthDp.toPx() }
    val cardHeightPx = with(density) { cardHeightDp.toPx() }

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    val (minScrollOffset, maxScrollOffset) = remember(cardCount, cardSpacingPx) {
        val center = (cardCount - 1) / 2f
        val maxOff = center * cardSpacingPx
        val minOff = -(cardCount - 1 - center) * cardSpacingPx
        minOff to maxOff
    }

    var selectedCardIndex by remember { mutableIntStateOf(-1) }
    var dragCardIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragPopped by remember { mutableStateOf(false) }
    var popAnchorX by remember { mutableFloatStateOf(0f) }
    var dragFromSelected by remember { mutableStateOf(false) }
    var dragStartCenterX by remember { mutableFloatStateOf(0f) }

    val actedHeroIds = remember(turnOrder, currentTurnIndex) {
        turnOrder
            .filterIndexed { idx, _ -> idx < currentTurnIndex }
            .mapNotNull { it.id.toIntOrNull() }
            .toSet()
    }

    fun isCardUsable(item: Any): Boolean = when (item) {
        is com.example.game.model.Skill -> {
            val isUlt = item.ultimateGain == 0
            if (isUlt) currentHero.gauge >= 100
            else (skillCooldowns[item.id] ?: 0) <= 0
        }
        is ComboSkill -> !item.requiredHeroes.any { it in actedHeroIds }
        else -> false
    }

    fun arcTx(index: Int): Float {
        val centerIndex = (cardCount - 1) / 2f
        val relIdx = index - centerIndex + (scrollOffset / cardSpacingPx)
        return relIdx * 85f
    }

    fun arcTy(index: Int): Float {
        val centerIndex = (cardCount - 1) / 2f
        val relIdx = index - centerIndex + (scrollOffset / cardSpacingPx)
        return relIdx.pow(2) * 10f
    }

    fun arcRotation(index: Int): Float {
        val centerIndex = (cardCount - 1) / 2f
        val relIdx = index - centerIndex + (scrollOffset / cardSpacingPx)
        return relIdx * 12f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .pointerInput(cardCount) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val downPos = down.position
                        val boxWidth = size.width
                        val boxHeight = size.height
                        val density = density

                        val centerIndex = (cardCount - 1) / 2f

                        fun hitTestCard(touchX: Float, touchY: Float): Int {
                            if (selectedCardIndex >= 0) {
                                val selCenterX = boxWidth / 2f
                                val selCenterY = boxHeight - cardHeightPx / 2f - popPositionPx
                                if (abs(touchX - selCenterX) < cardWidthPx / 2f &&
                                    abs(touchY - selCenterY) < cardHeightPx / 2f) {
                                    return selectedCardIndex
                                }
                            }
                            for (i in allCards.indices.reversed()) {
                                if (i == selectedCardIndex) continue
                                val relIdx = i - centerIndex + (scrollOffset / cardSpacingPx)
                                val cardCenterX = boxWidth / 2f + relIdx * cardSpacingPx
                                val arcY = relIdx * relIdx * arcHeightFactorPx
                                val cardCenterY = boxHeight - cardHeightPx / 2f - arcY
                                if (abs(touchX - cardCenterX) < cardWidthPx / 2f &&
                                    abs(touchY - cardCenterY) < cardHeightPx / 2f) {
                                    return i
                                }
                            }
                            return -1
                        }

                        val touchedIdx = hitTestCard(downPos.x, downPos.y)

                        var hasMoved = false
                        var gestureAction = ""
                        var lastPos = downPos

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()
                            val curPos = change.position
                            val delta = curPos - lastPos

                            if (!hasMoved && (curPos - downPos).getDistance() > viewConfiguration.touchSlop) {
                                hasMoved = true
                                val absDx = abs(delta.x)
                                val absDy = abs(delta.y)

                                if (touchedIdx >= 0 &&
                                    isCardUsable(allCards[touchedIdx]) &&
                                    (touchedIdx == selectedCardIndex || absDy > absDx)
                                ) {
                                    val alreadyPopped = touchedIdx == selectedCardIndex
                                    if (selectedCardIndex >= 0 && !alreadyPopped) {
                                        selectedCardIndex = -1
                                    }
                                    gestureAction = "cardDrag"
                                    dragCardIndex = touchedIdx
                                    isDragPopped = alreadyPopped
                                    dragFromSelected = alreadyPopped
                                    dragStartCenterX = boxWidth / 2f + (touchedIdx - centerIndex) * cardSpacingPx
                                    if (alreadyPopped) {
                                        popAnchorX = downPos.x
                                    }
                                    dragOffsetY = 0f
                                    dragOffsetX = 0f
                                    onCardDragStart?.invoke(getCardColor(allCards[touchedIdx]))
                                } else {
                                    gestureAction = "scroll"
                                    if (selectedCardIndex >= 0) {
                                        selectedCardIndex = -1
                                    }
                                }
                            }

                            if (hasMoved) {
                                when (gestureAction) {
                                    "cardDrag" -> {
                                        dragOffsetY = curPos.y - downPos.y
                                        if (!isDragPopped && dragOffsetY < -popThresholdPx) {
                                            isDragPopped = true
                                            popAnchorX = curPos.x
                                        }
                                        if (isDragPopped) {
                                            dragOffsetX = curPos.x - popAnchorX
                                        }
                                    }
                                    "scroll" -> {
                                        scrollOffset = (scrollOffset + delta.x)
                                            .coerceIn(minScrollOffset, maxScrollOffset)
                                    }
                                }
                            }

                            lastPos = curPos
                        } while (event.changes.any { it.pressed })

                        if (!hasMoved && touchedIdx >= 0) {
                            if (selectedCardIndex == touchedIdx) {
                                selectedCardIndex = -1
                            } else if (isCardUsable(allCards[touchedIdx])) {
                                selectedCardIndex = touchedIdx
                            }
                        } else if (gestureAction == "cardDrag") {
                            if (isDragPopped && dragOffsetY < -thresholdPx) {
                                val item = allCards[dragCardIndex]
                                when (item) {
                                    is com.example.game.model.Skill -> {
                                        val isUlt = item.ultimateGain == 0
                                        val canUse = if (isUlt) currentHero.gauge >= 100
                                            else (skillCooldowns[item.id] ?: 0) <= 0
                                        if (canUse) onSkill(item)
                                    }
                                    is ComboSkill -> {
                                        if (!item.requiredHeroes.any { it in actedHeroIds }) {
                                            onComboSelect(item.id)
                                        }
                                    }
                                }
                            }
                            dragCardIndex = -1
                            dragOffsetY = 0f
                            dragOffsetX = 0f
                            popAnchorX = 0f
                            isDragPopped = false
                            dragFromSelected = false
                            dragStartCenterX = 0f
                            selectedCardIndex = -1
                            onCardDragEnd?.invoke()
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            allCards.forEachIndexed { index, item ->
                val isSelected = selectedCardIndex == index
                val isDragged = dragCardIndex == index

                val cardAlpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(400, delayMillis = 100 + index * 80),
                    label = "cardAlpha_$index"
                )

                val popProgress by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = if (isDragged) snap() else tween(250),
                    label = "popProgress_$index"
                )

                val cardMod = Modifier
                    .alpha(cardAlpha)
                    .graphicsLayer {
                        when {
                            isDragged -> {
                                val startTx = arcTx(index).dp.toPx()
                                val startTy = arcTy(index).dp.toPx()
                                if (dragFromSelected) {
                                    translationX = dragOffsetX
                                    translationY = -popPositionPx + dragOffsetY
                                    rotationZ = 0f
                                    scaleX = 1.15f; scaleY = 1.15f
                                } else if (isDragPopped) {
                                    val progress = (-dragOffsetY - popThresholdPx)
                                        .coerceIn(0f, popThresholdPx) / popThresholdPx
                                    translationX = dragStartCenterX * (1f - progress) + dragOffsetX
                                    translationY = startTy + dragOffsetY
                                    rotationZ = 0f
                                    scaleX = 1f + 0.15f * progress
                                    scaleY = 1f + 0.15f * progress
                                } else {
                                    val dragProgress = (-dragOffsetY / popThresholdPx).coerceIn(0f, 1f)
                                    translationX = startTx * (1f - dragProgress)
                                    translationY = startTy + min(dragOffsetY, 0f)
                                    rotationZ = arcRotation(index) * (1f - dragProgress)
                                    scaleX = 1f + 0.15f * dragProgress
                                    scaleY = 1f + 0.15f * dragProgress
                                }
                            }
                            isSelected -> {
                                val startTx = arcTx(index).dp.toPx()
                                val startTy = arcTy(index).dp.toPx()
                                val comboOff = if (item is ComboSkill) 20f else 0f
                                translationX = startTx * (1f - popProgress)
                                translationY = startTy * (1f - popProgress) + (-popPositionPx) * popProgress - comboOff * (1f - popProgress)
                                rotationZ = arcRotation(index) * (1f - popProgress)
                                scaleX = 1f + 0.15f * popProgress
                                scaleY = 1f + 0.15f * popProgress
                            }
                            else -> {
                                translationX = arcTx(index).dp.toPx()
                                translationY = arcTy(index).dp.toPx() - (if (item is ComboSkill) 20f else 0f)
                                rotationZ = arcRotation(index)
                            }
                        }
                    }
                    .zIndex(
                        when {
                            isSelected || isDragged -> 999f
                            else -> index.toFloat()
                        }
                    )

                when (item) {
                    is com.example.game.model.Skill -> {
                        val isUlt = item.ultimateGain == 0
                        val ultReady = currentHero.gauge >= 100
                        val cooldown = skillCooldowns[item.id] ?: 0

                        SkillCard(
                            skill = item,
                            heroColor = currentHero.element.color,
                            isUltimate = isUlt,
                            ultReady = ultReady,
                            heroLevel = currentHero.level,
                            baseCooldown = item.cooldown,
                            cooldownRemaining = cooldown,
                            suspendAnimations = isDragged,
                            modifier = Modifier.width(cardWidthDp).height(cardHeightDp).then(cardMod)
                        )
                    }
                    is ComboSkill -> {
                        val anyActed = item.requiredHeroes.any { it in actedHeroIds }
                        ComboCard(
                            combo = item,
                            disabled = anyActed,
                            suspendAnimations = isDragged,
                            modifier = Modifier.width(cardWidthDp).height(cardHeightDp).then(cardMod)
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
    disabled: Boolean = false,
    suspendAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = if (suspendAnimations || disabled)
            infiniteRepeatable(tween<Float>(0, easing = LinearEasing), RepeatMode.Reverse)
        else
            infiniteRepeatable(tween<Float>(1000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Card(
        modifier = modifier
            .alpha(if (disabled) 0.5f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = lerp(Color(0xFFF5EEDC), Color(0xFF4A148C), 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (suspendAnimations) 0.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(
                    width = 3.dp,
                    color = if (disabled) Color.Gray.copy(alpha = 0.4f) else Color(0xFF9C27B0).copy(alpha = glowAlpha),
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
                    text = combo.requiredHeroes.joinToString(" + ") { id ->
                        DataLoader.heroes.find { it.id == id }?.name ?: "Hero $id"
                    },
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
    suspendAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isOnCooldown = cooldownRemaining > 0 || (isUltimate && !ultReady)
    val showCooldown = baseCooldown > 1
    val displayText = if (isOnCooldown) "$cooldownRemaining" else "$baseCooldown"

    val eggshell = Color(0xFFF5EEDC)

    val bgColor = when {
        isOnCooldown -> Color(0xFFE0E0E0)
        isUltimate   -> Color(0xFFFFF9C4)
        else         -> lerp(eggshell, heroColor, 0.15f)
    }

    val borderColor = skillCardBorderColor(skill, isOnCooldown, isUltimate, ultReady)

    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = if (suspendAnimations)
            infiniteRepeatable(tween<Float>(0, easing = LinearEasing), RepeatMode.Reverse)
        else
            infiniteRepeatable(tween<Float>(1000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Card(
        modifier = modifier
            .alpha(if (isOnCooldown) 0.8f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (suspendAnimations) 0.dp else 4.dp)
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
                if (isUltimate && ultReady && !suspendAnimations) {
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
                if (isUltimate && ultReady && !suspendAnimations) {
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

private fun skillCardBorderColor(
    skill: com.example.game.model.Skill,
    isOnCooldown: Boolean,
    isUltimate: Boolean,
    ultReady: Boolean
): Color {
    if (isOnCooldown) return Color.Gray
    if (isUltimate) return Color(0xFFFFD700)

    val types = skill.combinedTypes
    val colorMap = mapOf(
        SkillType.DAMAGE to Color(0xFFD32F2F),
        SkillType.HEAL to Color(0xFF689F38),
        SkillType.BUFF to Color(0xFF0288D1),
    )
    return if (types.size == 1) {
        colorMap[types.first()] ?: Color(0xFF0288D1)
    } else {
        val colors = types.mapNotNull { colorMap[it] }
        val avgR = colors.map { (it.red * 255).toInt() }.average().toInt()
        val avgG = colors.map { (it.green * 255).toInt() }.average().toInt()
        val avgB = colors.map { (it.blue * 255).toInt() }.average().toInt()
        Color(avgR, avgG, avgB)
    }
}

private fun getCardColor(item: Any): Color {
    return when (item) {
        is com.example.game.model.Skill -> {
            if (item.ultimateGain == 0) Color(0xFFFFD700)
            else when {
                item.healScaling != null -> Color(0xFF66BB6A)
                item.shieldScaling != null -> Color(0xFF42A5F5)
                item.damageComponents.any { it.element == Element.DARK || it.element == Element.SHADOW } -> Element.DARK.color
                else -> item.damageComponents.firstNotNullOfOrNull { it.element }?.color ?: Element.NEUTRAL.color
            }
        }
        is ComboSkill -> Color(0xFF9C27B0)
        else -> Color.Gray
    }
}


