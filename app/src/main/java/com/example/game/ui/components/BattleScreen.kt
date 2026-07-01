package com.example.game.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.*
import com.example.game.model.BattlePhase.*
import com.example.game.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun BattleScreen(viewModel: GameViewModel) {
    val battleState by viewModel.battleState.collectAsState()
    val state = battleState ?: return
    val battleLog by viewModel.battleLog.collectAsState()

    val infiniteTransition = rememberInfiniteTransition()
    val parallaxOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing))
    )

    val bossPulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = FastOutSlowInEasing))
    )

    val monster = state.monsters.firstOrNull()
    val monsterColor = monster?.let { elementToColor(it.element) } ?: Color.Gray
    val isBoss = monster?.isBoss ?: false

    // ─── Battle UI States ──────────────────────────────────────────
    var showFullLog by remember { mutableStateOf(false) }
    var currentTurnActorName by remember { mutableStateOf<String?>(null) }
    var showTurnBanner by remember { mutableStateOf(false) }

    // ─── Targeting Logic ───────────────────────────────────────────
    val selectedTargets = remember { mutableStateListOf<String>() }
    val isTargeting = state.phase == PLAYER_TURN && state.pendingSkill != null

    // ─── Battle Start Animation ────────────────────────────────────
    var blackVisible by remember { mutableStateOf(true) }
    var monsterVisible by remember { mutableStateOf(false) }
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
        state.aliveHeroes.forEachIndexed { _, hero ->
            delay(100)
            heroVisibilities[hero.heroId] = true
        }
        delay(50)
        battleTextVisible = true
        delay(1500)
        battleTextVisible = false
    }

    // Turn Banner Logic
    LaunchedEffect(state.currentActorId) {
        val actor = state.turnOrder.find { it.id == state.currentActorId }
        if (actor != null) {
            currentTurnActorName = actor.name
            showTurnBanner = true
            delay(1000)
            showTurnBanner = false
        }
    }

    // ─── Sprite Animations ─────────────────────────────────────────
    val (heroAnimStates, monsterAnimState) = rememberSpriteAnimations(state.eventLog, state)

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
                    monsterFlashColor = Color.Red
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
    var monsterPos by remember { mutableStateOf(Offset.Zero) }
    var heroPositions by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).shakeOffset(shakeHandle)
    ) {
        BattleBackground(
            parallaxOffset = sin(parallaxOffset),
            bossFight = isBoss,
            monsterElement = monster?.element ?: Element.NEUTRAL,
            elementTint = monsterColor,
            modifier = Modifier.fillMaxSize()
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 400.dp)) {
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

                // Monster Area
                Box(
                    modifier = Modifier.fillMaxWidth().weight(0.55f).onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        monsterPos = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height * 0.4f)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (monster != null && !monster.isDead) {
                        val isTargeted = selectedTargets.contains(monster.monsterId)
                        val canTarget = state.pendingSkill?.let { it.targetType == TargetType.SINGLE_ENEMY || it.targetType == TargetType.ALL_ENEMIES || it.targetType == TargetType.ALL } ?: false
                        
                        val monsterClickable = if (isTargeting && canTarget) {
                            Modifier.clickable {
                                if (selectedTargets.contains(monster.monsterId)) selectedTargets.remove(monster.monsterId)
                                else selectedTargets.add(monster.monsterId)
                            }
                        } else Modifier

                        Box(
                            modifier = Modifier.fillMaxSize().then(monsterClickable),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(monsterAppearScale)) {
                                MonsterHUD(monster = monster, statuses = state.getStatusesForTarget(monster.monsterId), modifier = Modifier.padding(bottom = 8.dp))
                                MonsterSprite(
                                    monsterName = monster.monsterId,
                                    elementColor = monsterColor,
                                    isBoss = isBoss,
                                    isFlashing = monsterFlashAlpha > 0f,
                                    flashColor = monsterFlashColor,
                                    flashAlpha = monsterFlashAlpha,
                                    bossPulse = sin(bossPulse),
                                    animState = monsterAnimState.value,
                                    modifier = Modifier.fillMaxWidth().weight(1f).scale(1.5f).graphicsLayer {
                                        if (isTargeted) { scaleX = 1.1f; scaleY = 1.1f }
                                    }
                                )
                            }
                            if (isTargeting) TargetCircle(color = Color.Red, isSelected = isTargeted)
                        }
                    }
                }

                // Hero Area
                Box(
                    modifier = Modifier.fillMaxWidth().weight(0.45f).onGloballyPositioned { coords ->
                        val basePos = coords.positionInRoot()
                        val hCount = state.aliveHeroes.size.coerceAtLeast(1)
                        heroPositions = state.aliveHeroes.mapIndexed { idx, hero ->
                            val hw = coords.size.width / hCount
                            hero.heroId to Offset(basePos.x + hw * idx + hw / 2f, basePos.y + coords.size.height * 0.5f)
                        }.toMap()
                    },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        state.aliveHeroes.forEach { hero ->
                            val isTurn = state.currentActorId == hero.heroId
                            val animState = heroAnimStates[hero.heroId] ?: SpriteAnimState()
                            val heroFlash = heroFlashAlphas[hero.heroId] ?: 0f
                            val isTargeted = selectedTargets.contains(hero.heroId)
                            val canTarget = state.pendingSkill?.let { it.targetType == TargetType.SINGLE_ALLY || it.targetType == TargetType.ALL_ALLIES || it.targetType == TargetType.ALL || it.targetType == TargetType.SELF } ?: false
                            val heroEntry by animateFloatAsState(targetValue = if (heroVisibilities[hero.heroId] == true) 0f else 150f, animationSpec = spring(0.7f, 150f))
                            val density = LocalDensity.current
                            
                            val heroClickable = if (isTargeting && canTarget) {
                                Modifier.clickable {
                                    if (selectedTargets.contains(hero.heroId)) selectedTargets.remove(hero.heroId)
                                    else selectedTargets.add(hero.heroId)
                                }
                            } else Modifier

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f).graphicsLayer { translationY = with(density) { heroEntry.dp.toPx() } }
                                    .then(heroClickable)
                            ) {
                                HeroHUD(hero = hero, statuses = state.getStatusesForTarget(hero.heroId), isCurrentTurn = isTurn, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
                                Box(contentAlignment = Alignment.Center) {
                                    HeroSprite(
                                        heroName = hero.heroId,
                                        elementColor = elementToColor(hero.element),
                                        isActive = !hero.isDead,
                                        isFlashing = heroFlash > 0f,
                                        flashColor = heroFlashColors[hero.heroId] ?: Color.Red,
                                        flashAlpha = heroFlash,
                                        animState = animState,
                                        modifier = Modifier.size(120.dp).graphicsLayer {
                                            if (isTargeted) { scaleX = 1.15f; scaleY = 1.15f }
                                        }
                                    )
                                    if (isTargeting) TargetCircle(color = Color.Green, isSelected = isTargeted)
                                }
                            }
                        }
                    }
                }
            }

            // Action Panel
            val currentHero = state.aliveHeroes.find { it.heroId == state.currentActorId }
            if (currentHero != null && state.phase == PLAYER_TURN) {
                key(state.currentActorId) {
                    ActionPanel(
                        currentHero = currentHero,
                        heroes = state.heroes,
                        monsters = state.monsters,
                        skillCooldowns = state.skillCooldowns[currentHero.heroId] ?: emptyMap(),
                        onSkill = { skill, targets -> 
                            viewModel.executeSkill(currentHero.heroId, skill, targets.ifEmpty { null })
                            selectedTargets.clear()
                        },
                        onUltimate = { viewModel.executeUltimate(currentHero.heroId) },
                        onComboById = { comboId -> viewModel.executeComboById(comboId) },
                        isTargeting = isTargeting,
                        selectedTargets = selectedTargets.toList(),
                        onCancelTargeting = { viewModel.cancelAction(); selectedTargets.clear() },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    )
                }
            }
        }

        BattleEffectOverlay(events = state.eventLog, heroPositions = heroPositions, monsterPosition = monsterPos, pool = pool, modifier = Modifier.fillMaxSize())
        
        // Turn Banner Overlay
        TurnBanner(actorName = currentTurnActorName, visible = showTurnBanner, modifier = Modifier.align(Alignment.Center))

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

        Box(modifier = Modifier.fillMaxSize().alpha(blackAlpha).background(Color.Black))

        if (showFullLog) {
            BattleLogDialog(log = battleLog, onDismiss = { showFullLog = false })
        }
    }
}

@Composable
fun BattleLogDialog(log: List<String>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
    LazyColumn(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        items(state.turnOrder) { actor ->
            val isActive = actor.id == state.currentActorId
            val color = elementToColor(actor.element)
            
            Text(
                text = (if (isActive) "▶ " else "") + actor.name.uppercase(),
                color = if (isActive) color else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun TargetCircle(color: Color, isSelected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(if (isSelected) 4.dp else 2.dp, if (isSelected) color else color.copy(alpha = 0.5f), CircleShape)
    )
}
