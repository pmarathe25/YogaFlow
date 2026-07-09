package com.example.game.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.example.game.battle.BattleSoundManager
import com.example.game.model.*
import com.example.game.model.BattlePhase.*
import com.example.game.viewmodel.GameViewModel
import com.example.game.persistence.DataLoader
import kotlinx.coroutines.delay
import kotlin.math.*

val LocalBattleSoundManager = staticCompositionLocalOf<BattleSoundManager> {
    error("No BattleSoundManager provided")
}

@Composable
fun BattleScene(viewModel: GameViewModel) {
    val context = LocalContext.current
    val battleState by viewModel.battleState.collectAsState()
    val state = battleState ?: return
    val battleLog by viewModel.battleLog.collectAsState()

    val soundManager = remember { BattleSoundManager(context) }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val parallaxOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing))
    )

    val bossPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = FastOutSlowInEasing))
    )

    val dropGlowTransition = rememberInfiniteTransition()
    val dropOverlayAlpha by dropGlowTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse)
    )

    val monster = state.monsters.firstOrNull()
    val monsterColor = monster?.let { elementToColor(it.element) } ?: Color.Gray
    val isBoss = monster?.isBoss ?: false

    // ─── Battle UI States ──────────────────────────────────────────
    var showFullLog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var dragOverlayColor by remember { mutableStateOf<Color?>(null) }
    BackHandler(enabled = state.phase == PLAYER_TURN || state.phase == ENEMY_TURN) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Battle?") },
            text = { Text("Are you sure you want to forfeit the current battle?") },
            confirmButton = {
                TextButton(onClick = { viewModel.navigateBack(); showExitDialog = false }) {
                    Text("Forfeit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue Fighting")
                }
            }
        )
    }

    // ─── Targeting Logic ───────────────────────────────────────────
    val isTargeting = state.phase == PLAYER_TURN && state.pendingSkill != null

    // ─── Battle Start Animation ────────────────────────────────────
    var blackVisible by remember { mutableStateOf(true) }
    var monsterVisible by remember { mutableStateOf(false) }
    var monsterNameVisible by remember { mutableStateOf(false) }
    var battleTextVisible by remember { mutableStateOf(false) }
    val heroVisibilities = remember { mutableStateMapOf<String, Boolean>() }

    val blackAlpha by animateFloatAsState(
        targetValue = if (blackVisible) 1f else 0f,
        animationSpec = tween(300)
    )
    val monsterAppearScale by animateFloatAsState(
        targetValue = if (monsterVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f)
    )

    LaunchedEffect(Unit) {
        delay(100)
        blackVisible = false
        delay(300)
        monsterVisible = true
        delay(500)
        monsterNameVisible = true
        delay(1000)
        monsterNameVisible = false
        delay(200)
        state.aliveHeroes.forEachIndexed { _, hero ->
            delay(100)
            heroVisibilities[hero.id] = true
        }
        delay(50)
        battleTextVisible = true
        delay(1500)
        battleTextVisible = false
        viewModel.onIntroComplete()
    }

    // ─── Sprite Animations ─────────────────────────────────────────
    var monsterPos by remember { mutableStateOf(Offset.Zero) }
    var heroPositions by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    val (heroAnimStates, monsterAnimState) = rememberSpriteAnimations(state.eventLog, state, heroPositions, monsterPos)

    // Flash states
    var monsterFlashAlpha by remember { mutableStateOf(0f) }
    var monsterFlashColor by remember { mutableStateOf(Color.Red) }
    val heroFlashAlphas = remember { mutableStateMapOf<String, Float>() }
    val heroFlashColors = remember { mutableStateMapOf<String, Color>() }
    val lastFlashEventCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(state.eventLog.size) {
        if (state.eventLog.size <= lastFlashEventCount.intValue) return@LaunchedEffect
        lastFlashEventCount.intValue = state.eventLog.size
        val event = state.eventLog.lastOrNull() ?: return@LaunchedEffect
        when (event) {
            is BattleEvent.SkillUsed -> {
                val isAttack = event.skill.damageComponents.isNotEmpty() || event.skill.baseDamage > 0
                val flashColor = if (event.skill.healScaling != null) Color(0xFF66BB6A)
                                else if (isAttack) Color.Red
                                else Color(0xFF42A5F5)

                if (isAttack) {
                    val totalShieldDamage = event.outcomes.sumOf { outcome ->
                        outcome.perTargetResult.values.sumOf { it.shieldDamage }
                    }
                    val totalDamage = event.outcomes.sumOf { it.damageDealt }
                    monsterFlashColor = if (totalShieldDamage > 0 && totalShieldDamage >= totalDamage) {
                        Color(0xFF9C27B0)
                    } else {
                        Color.Red
                    }
                    monsterFlashAlpha = 1f
                    delay(150)
                    monsterFlashAlpha = 0f
                } else {
                    event.targets.forEach { targetId ->
                        heroFlashColors[targetId] = flashColor
                        heroFlashAlphas[targetId] = 1f
                    }
                    delay(300)
                    event.targets.forEach { targetId -> heroFlashAlphas[targetId] = 0f }
                }
            }
            is BattleEvent.MonsterTurn -> {
                event.targets.forEach { targetHeroId ->
                    heroFlashColors[targetHeroId] = Color.Red
                    heroFlashAlphas[targetHeroId] = 1f
                }
                delay(200)
                event.targets.forEach { targetHeroId -> heroFlashAlphas[targetHeroId] = 0f }
            }
            else -> {}
        }
    }

    // ─── Effect System ─────────────────────────────────────────────
    val shakeHandle = rememberShakeHandle()
    val pool = rememberParticlePool(300)

    // Current actor for action tray
    val currentHero = state.aliveHeroes.find { it.id == state.currentActorId }
    val availableCombos = remember(state.heroes) {
        if (currentHero == null) emptyList()
        else {
            val aliveHeroes = state.heroes.filter { it.hp > 0 && !it.isDefeated }
            val aliveHeroIds = aliveHeroes.mapNotNull { it.id.toIntOrNull() }.toSet()
            DataLoader.combos.filter { combo ->
                combo.requiredHeroes.all { id -> id in aliveHeroIds }
            }
        }
    }

    CompositionLocalProvider(LocalBattleSoundManager provides soundManager) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .shakeOffset(shakeHandle)
        ) {
        BattleBackground(
            parallaxOffset = sin(parallaxOffset),
            bossFight = isBoss,
            monsterElement = monster?.element ?: Element.NEUTRAL,
            elementTint = monsterColor,
            biomeIndex = state.turnsTaken % 4,
            modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 250.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    TurnOrderList(state = state)
                    IconButton(
                        onClick = { showFullLog = true },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Battle Log", tint = Color.White)
                    }
                }

                // Monster Zone (upper 55%)
                Box(
                    modifier = Modifier.fillMaxWidth().weight(0.55f).onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        monsterPos = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height * 0.4f)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (monster != null && !monster.isDefeated) {
                        val canTarget = state.pendingSkill?.let {
                            it.targetType == TargetType.SINGLE_ENEMY || it.targetType == TargetType.ALL_ENEMIES || it.targetType == TargetType.ALL
                        } ?: false
                        val isTargeted = isTargeting && canTarget

                        val monsterClickable = if (isTargeting && canTarget) {
                            Modifier.clickable {
                                if (isTargeting) {
                                    val skill = state.pendingSkill ?: return@clickable
                                    viewModel.executeSkill(currentHero?.id ?: return@clickable, skill, listOf(monster.id))
                                }
                            }
                        } else Modifier

                        Box(
                            modifier = Modifier.fillMaxSize().then(monsterClickable),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(monsterAppearScale)) {
                                MonsterHUD(
                                    monster = monster,
                                    statuses = state.getStatusesForTarget(monster.id),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                CombatantSprite(
                                    isMonster = true,
                                    name = monster.id,
                                    heroId = 0,
                                    elementColor = monsterColor,
                                    isActive = !monster.isDefeated,
                                    isBoss = isBoss,
                                    isFlashing = monsterFlashAlpha > 0f,
                                    flashColor = monsterFlashColor,
                                    flashAlpha = monsterFlashAlpha,
                                    bossPulse = sin(bossPulse),
                                    animState = monsterAnimState.value,
                                    isTargeted = isTargeted,
                                    isLowHp = monster.hpPercent < 0.3f && !monster.isDefeated,
                                    element = monster.element,
                                    modifier = Modifier.fillMaxWidth().weight(1f).graphicsLayer {
                                        scaleX = 1.5f
                                        scaleY = 1.5f
                                        if (isTargeted) { scaleX = 1.65f; scaleY = 1.65f }
                                    }
                                )
                            }
                        }
                    }
                }

                // Hero Zone (lower 45%)
                Box(
                    modifier = Modifier.fillMaxWidth().weight(0.45f).onGloballyPositioned { coords ->
                        val basePos = coords.positionInRoot()
                        val hCount = state.aliveHeroes.size.coerceAtLeast(1)
                        heroPositions = state.aliveHeroes.mapIndexed { idx, hero ->
                            val hw = coords.size.width / hCount
                            hero.id to Offset(basePos.x + hw * idx + hw / 2f, basePos.y + coords.size.height * 0.5f)
                        }.toMap()
                    },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        state.aliveHeroes.forEach { hero ->
                            val isTurn = state.currentActorId == hero.id
                            val animState = heroAnimStates[hero.id] ?: SpriteAnimState()
                            val heroFlash = heroFlashAlphas[hero.id] ?: 0f
                            val canTarget = state.pendingSkill?.let { skill ->
                                when (skill.targetType) {
                                    TargetType.SINGLE_ALLY -> hero.id != state.currentActorId
                                    TargetType.ALL_ALLIES, TargetType.ALL -> true
                                    TargetType.SELF -> hero.id == state.currentActorId
                                    else -> false
                                }
                            } ?: false
                            val isTargeted = isTargeting && canTarget
                            val heroEntry by animateFloatAsState(
                                targetValue = if (heroVisibilities[hero.id] == true) 0f else 150f,
                                animationSpec = spring(0.7f, 150f)
                            )
                            val density = LocalDensity.current

                            val heroClickable = if (isTargeting && canTarget) {
                                Modifier.clickable {
                                    val skill = state.pendingSkill ?: return@clickable
                                    viewModel.executeSkill(currentHero?.id ?: return@clickable, skill, listOf(hero.id))
                                }
                            } else Modifier

                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.graphicsLayer { translationY = with(density) { heroEntry.dp.toPx() } }
                                        .then(heroClickable)
                                ) {
                                    HeroHUD(
                                        hero = hero,
                                        statuses = state.getStatusesForTarget(hero.id),
                                        isCurrentTurn = isTurn,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Box(contentAlignment = Alignment.Center) {
                                        CombatantSprite(
                                            isMonster = false,
                                            name = hero.id,
                                            heroId = hero.id.toIntOrNull() ?: 0,
                                            elementColor = elementToColor(hero.element),
                                            isActive = !hero.isDefeated,
                                            isFlashing = heroFlash > 0f,
                                            flashColor = heroFlashColors[hero.id] ?: Color.Red,
                                            flashAlpha = heroFlash,
                                            animState = animState,
                                            isTargeted = isTargeted,
                                            isLowHp = hero.hpPercent < 0.3f && !hero.isDefeated,
                                            isCurrentTurn = isTurn,
                                            element = hero.element,
                                            modifier = Modifier.size(120.dp).graphicsLayer {
                                                if (isTargeted) { scaleX = 1.15f; scaleY = 1.15f }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Targeting prompt
            if (isTargeting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Select a target",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(16.dp))
                        TextButton(onClick = {
                            viewModel.cancelAction()
                        }) {
                            Text("CANCEL", color = Color.Red, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Drop zone overlay during card drag
            if (dragOverlayColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 250.dp)
                        .background(dragOverlayColor!!.copy(alpha = dropOverlayAlpha))
                )
            }

            // Action Tray at bottom with card-deal slide-up animation
            if (currentHero != null && state.phase == PLAYER_TURN) {
                key(state.currentActorId) {
                    var showHand by remember { mutableStateOf(false) }
                    val slideFraction by animateFloatAsState(
                        targetValue = if (showHand) 0f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f)
                    )

                    LaunchedEffect(Unit) {
                        showHand = false
                        delay(50)
                        showHand = true
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .graphicsLayer { translationY = slideFraction * 200.dp.toPx() }
                    ) {
                        ActionTray(
                            currentHero = currentHero,
                            turnOrder = state.turnOrder,
                            currentTurnIndex = state.currentTurnIndex,
                            skillCooldowns = state.skillCooldowns[currentHero.id] ?: emptyMap(),
                            availableCombos = availableCombos,
                            isTargeting = isTargeting,
                            onSkill = { skill ->
                                viewModel.executeSkill(currentHero.id, skill)
                            },
                            onComboById = { comboId ->
                                viewModel.executeComboById(comboId)
                            },
                            onCancelTargeting = {
                                viewModel.cancelAction()
                            },
                            onCardDragStart = { color -> dragOverlayColor = color },
                            onCardDragEnd = { dragOverlayColor = null },
                            onSkipTurn = { viewModel.skipTurn(state.currentActorId) },
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Battle Effects Layer
        BattleEffectsLayer(
            events = state.eventLog,
            heroPositions = heroPositions,
            monsterPosition = monsterPos,
            pool = pool,
            shakeHandle = shakeHandle,
            soundManager = soundManager,
            modifier = Modifier.fillMaxSize()
        )

        // Battle Start Text Overlay
        AnimatedVisibility(
            visible = battleTextVisible,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 1.5f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "BATTLE BEGINS!",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.displayLarge
            )
        }

        // Monster Name Banner
        AnimatedVisibility(
            visible = monsterNameVisible,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 1.5f) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
        ) {
            Text(
                text = monster?.englishName?.uppercase() ?: "???",
                color = monsterColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineLarge
            )
        }

        // Turn indicator banner (animated popup like intro text)
        TurnBanner(
            actorName = state.turnOrder.find { it.id == state.currentActorId }?.name,
            visible = state.phase != BattlePhase.INTRO,
            modifier = Modifier.align(Alignment.Center)
        )

        // Black overlay for intro
        Box(modifier = Modifier.fillMaxSize().alpha(blackAlpha).background(Color.Black))

        // Battle Log Dialog
        if (showFullLog) {
            BattleLogDialog(log = battleLog, onDismiss = { showFullLog = false })
        }
        }
    }
}

@Composable
fun BattleLogDialog(log: List<String>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Battle Log", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(log.reversed()) { message ->
                        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TurnOrderList(state: BattleState) {
    val infiniteTransition = rememberInfiniteTransition()
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val lastEvent = state.eventLog.lastOrNull()
    val comboParticipantIds = remember(lastEvent) {
        if (lastEvent is BattleEvent.ComboUsed) lastEvent.participants
        else emptySet()
    }

    LazyColumn(
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(state.turnOrder) { actor ->
            val isActive = actor.id == state.currentActorId
            val isComboParticipant = actor.id in comboParticipantIds
            val isCurrentlyActive = isActive || isComboParticipant
            val actorIndex = state.turnOrder.indexOf(actor)
            val hasActed = actorIndex >= 0 && actorIndex < state.currentTurnIndex
            val color = elementToColor(actor.element)

            Text(
                text = actor.name.uppercase(),
                color = if (isCurrentlyActive) color else Color.White.copy(alpha = if (hasActed) 0.35f else 0.6f),
                fontWeight = if (isCurrentlyActive) FontWeight.Black else if (hasActed) FontWeight.Light else FontWeight.Normal,
                fontSize = if (isCurrentlyActive) 12.sp else 10.sp,
                textDecoration = if (hasActed) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 0.dp)
                    .then(if (isCurrentlyActive) Modifier.graphicsLayer { translationY = bounceOffset } else Modifier)
            )
        }
    }
}
