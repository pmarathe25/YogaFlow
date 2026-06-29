package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.YogaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Hero and Monster definitions
data class HeroDef(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val baseHp: Int,
    val baseAtk: Int,
    val skillName: String,
    val skillDescription: String,
    val unlockCost: Int
)

data class MonsterDef(
    val id: String,
    val name: String,
    val englishName: String,
    val icon: String,
    val hp: Int,
    val atk: Int,
    val specialAttackName: String,
    val specialAttackDesc: String,
    val xpReward: Int,
    val sparkReward: Int,
    val difficulty: String
)

// Active units in combat
class CombatUnit(
    val id: String,
    val name: String,
    val icon: String,
    val maxHp: Int,
    var hp: Int,
    val atk: Int,
    val isHero: Boolean,
    var shield: Int = 0,
    var isTaunting: Boolean = false,
    val skillName: String = "",
    val heroId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenGardenScreen(
    viewModel: YogaViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Collect our Sanskrit RPG State from ViewModel
    val unlockedHeroes by viewModel.rpgUnlockedHeroes.collectAsStateWithLifecycle()
    val activeParty by viewModel.rpgActiveParty.collectAsStateWithLifecycle()
    val availableXp by viewModel.rpgAvailableKarmaXp.collectAsStateWithLifecycle()
    val availableSparks by viewModel.rpgAvailableSparks.collectAsStateWithLifecycle()
    val stats by viewModel.rpgStats.collectAsStateWithLifecycle()
    val heroLevels by viewModel.rpgHeroLevels.collectAsStateWithLifecycle()

    // Load data when screen enters
    LaunchedEffect(Unit) {
        viewModel.loadRpgData()
    }

    // Tab indices: 0 = Quest (Battle), 1 = Party Setup & Upgrades
    var activeTab by remember { mutableStateOf(0) }

    // Battle states
    var inBattle by remember { mutableStateOf(false) }
    var currentOpponent by remember { mutableStateOf<MonsterDef?>(null) }
    var combatLog by remember { mutableStateOf<List<String>>(emptyList()) }
    var playerCombatParty by remember { mutableStateOf<List<CombatUnit>>(emptyList()) }
    var monsterCombatUnit by remember { mutableStateOf<CombatUnit?>(null) }
    var activeHeroTurnIndex by remember { mutableStateOf(0) } // Index in playerCombatParty
    var isEnemyActing by remember { mutableStateOf(false) }
    var battleEnded by remember { mutableStateOf(false) }
    var battleWon by remember { mutableStateOf(false) }
    var battleRewardsClaimed by remember { mutableStateOf(false) }

    // Heroes Pool
    val heroesPool = remember {
        listOf(
            HeroDef(
                id = "Shanti",
                name = "Shanti (Calm)",
                description = "The restorative spirit of peaceful calm. Healing support.",
                icon = "🧘",
                baseHp = 100,
                baseAtk = 10,
                skillName = "Pranayama Breath",
                skillDescription = "Heals the party member with lowest HP for 35 + 5 per level.",
                unlockCost = 0
            ),
            HeroDef(
                id = "Santosha",
                name = "Santosha (Content)",
                description = "The unbreakable shield of contentment. Defensive tank.",
                icon = "🛡️",
                baseHp = 160,
                baseAtk = 7,
                skillName = "Inner Sanctuary",
                skillDescription = "Gains a shield of 45 + 8 per level, and taunts the enemy to attack them.",
                unlockCost = 1
            ),
            HeroDef(
                id = "Virya",
                name = "Virya (Vigor)",
                description = "The blazing fire of yoga-fueled vigor. High physical DPS.",
                icon = "⚡",
                baseHp = 110,
                baseAtk = 22,
                skillName = "Tapas Blast",
                skillDescription = "Strikes the enemy for heavy fire damage equal to 32 + 7 per level.",
                unlockCost = 2
            ),
            HeroDef(
                id = "Dhairya",
                name = "Dhairya (Courage)",
                description = "Patient, courageous fortitude. Heavy balanced paladin.",
                icon = "🦁",
                baseHp = 135,
                baseAtk = 15,
                skillName = "Courageous Strike",
                skillDescription = "Deals 20 + 4 per level damage and boosts all allies' ATK permanently for this fight.",
                unlockCost = 2
            ),
            HeroDef(
                id = "Maitri",
                name = "Maitri (Loving-Kindness)",
                description = "Universal benevolence. Spellcaster who damages & heals.",
                icon = "💖",
                baseHp = 95,
                baseAtk = 18,
                skillName = "Loving Aura",
                skillDescription = "Deals 22 + 5 per level magic damage and heals all party members for 15 + 3 per level.",
                unlockCost = 3
            )
        )
    }

    // Monsters Pool
    val monstersPool = remember {
        listOf(
            MonsterDef(
                id = "Bhaya",
                name = "Bhaya",
                englishName = "Self-Doubt & Fear",
                icon = "👻",
                hp = 90,
                atk = 11,
                specialAttackName = "Terrifying Shadow",
                specialAttackDesc = "Stops positive energy, dealing dark damage.",
                xpReward = 60,
                sparkReward = 0,
                difficulty = "Easy"
            ),
            MonsterDef(
                id = "Chinta",
                name = "Chinta",
                englishName = "Anxiety & Worry",
                icon = "🐝",
                hp = 110,
                atk = 13,
                specialAttackName = "Anxious Swarm",
                specialAttackDesc = "Deals electric static damage that bypasses shield.",
                xpReward = 80,
                sparkReward = 0,
                difficulty = "Easy"
            ),
            MonsterDef(
                id = "Krodha",
                name = "Krodha",
                englishName = "Anger & Rage",
                icon = "🔥",
                hp = 140,
                atk = 18,
                specialAttackName = "Furious Burn",
                specialAttackDesc = "Deals heavy fire damage but reduces its own defense.",
                xpReward = 120,
                sparkReward = 0,
                difficulty = "Medium"
            ),
            MonsterDef(
                id = "Moha",
                name = "Moha",
                englishName = "Delusion & Attachment",
                icon = "🌀",
                hp = 160,
                atk = 15,
                specialAttackName = "Illusion Mist",
                specialAttackDesc = "Deludes the target with misleading dreams.",
                xpReward = 150,
                sparkReward = 0,
                difficulty = "Medium"
            ),
            MonsterDef(
                id = "Ahankara",
                name = "Ahankara",
                englishName = "Ego & Vanity (Boss)",
                icon = "👑",
                hp = 250,
                atk = 22,
                specialAttackName = "Arrogant Slam",
                specialAttackDesc = "Deals massive damage to a target with superior pride.",
                xpReward = 250,
                sparkReward = 1,
                difficulty = "Hard"
            ),
            MonsterDef(
                id = "Klesh",
                name = "Klesh",
                englishName = "Turmoil & Affliction (Final Boss)",
                icon = "🐉",
                hp = 350,
                atk = 26,
                specialAttackName = "Chaos Waves",
                specialAttackDesc = "Unleashes turbulent suffering onto the entire party.",
                xpReward = 400,
                sparkReward = 2,
                difficulty = "Extreme"
            )
        )
    }

    // Helper functions for combat logic
    fun addToLog(msg: String) {
        combatLog = combatLog + msg
    }

    fun startBattle(monster: MonsterDef) {
        currentOpponent = monster
        combatLog = listOf("⚔️ Battle commenced against ${monster.name} (${monster.englishName})!")
        
        // Build players party from activeParty list
        val partyList = activeParty.mapNotNull { heroId ->
            val def = heroesPool.find { it.id == heroId }
            if (def != null) {
                val lvl = heroLevels[heroId] ?: 1
                // Add 15% stats per level beyond level 1
                val multiplier = 1f + (lvl - 1) * 0.15f
                CombatUnit(
                    id = heroId,
                    name = def.name.split(" ").first(),
                    icon = def.icon,
                    maxHp = (def.baseHp * multiplier).toInt(),
                    hp = (def.baseHp * multiplier).toInt(),
                    atk = (def.baseAtk * multiplier).toInt(),
                    isHero = true,
                    skillName = def.skillName,
                    heroId = heroId
                )
            } else null
        }

        playerCombatParty = partyList
        monsterCombatUnit = CombatUnit(
            id = monster.id,
            name = monster.name,
            icon = monster.icon,
            maxHp = monster.hp,
            hp = monster.hp,
            atk = monster.atk,
            isHero = false,
            skillName = monster.specialAttackName
        )

        activeHeroTurnIndex = 0
        isEnemyActing = false
        battleEnded = false
        battleWon = false
        battleRewardsClaimed = false
        inBattle = true
    }

    fun checkBattleEnd() {
        val monster = monsterCombatUnit ?: return
        if (monster.hp <= 0) {
            monster.hp = 0
            battleEnded = true
            battleWon = true
            addToLog("🎉 Victory! ${monster.name} has been purified!")
            return
        }

        val allHeroesDead = playerCombatParty.all { it.hp <= 0 }
        if (allHeroesDead) {
            battleEnded = true
            battleWon = false
            addToLog("💀 Defeat! Your party has succumbed to the turmoil.")
        }
    }

    fun executeMonsterTurn() {
        val monster = monsterCombatUnit ?: return
        if (monster.hp <= 0 || battleEnded) return

        // Choose a target: Taunting heroes first, otherwise random living hero
        val livingHeroes = playerCombatParty.filter { it.hp > 0 }
        if (livingHeroes.isEmpty()) {
            checkBattleEnd()
            return
        }

        val tauntingHero = livingHeroes.find { it.isTaunting }
        val target = tauntingHero ?: livingHeroes.random()

        // 30% chance of Special Attack
        val isSpecial = (1..100).random() <= 30
        val damage = if (isSpecial) {
            addToLog("⚠️ ${monster.name} cast **${monster.skillName}** on ${target.name}!")
            (monster.atk * 1.4f).toInt()
        } else {
            addToLog("💥 ${monster.name} attacks ${target.name}!")
            monster.atk
        }

        // Apply damage taking shield into consideration
        if (target.shield >= damage) {
            target.shield -= damage
            addToLog("🛡️ ${target.name}'s shield absorbed all damage! (${target.shield} shield remaining)")
        } else {
            val remainingDamage = damage - target.shield
            if (target.shield > 0) {
                addToLog("🛡️ ${target.name}'s shield absorbed ${target.shield} damage!")
                target.shield = 0
            }
            target.hp = (target.hp - remainingDamage).coerceAtLeast(0)
            addToLog("💔 ${target.name} took $remainingDamage damage! (${target.hp}/${target.maxHp} HP)")
        }

        // Clean up taunt flags at the end of round
        playerCombatParty.forEach { it.isTaunting = false }

        // Give turn back to first living hero
        isEnemyActing = false
        var firstIndex = 0
        while (firstIndex < playerCombatParty.size && playerCombatParty[firstIndex].hp <= 0) {
            firstIndex++
        }
        activeHeroTurnIndex = firstIndex
        checkBattleEnd()
    }

    fun advanceTurn() {
        checkBattleEnd()
        if (battleEnded) return

        // Advance to next conscious hero
        var nextIndex = activeHeroTurnIndex + 1
        while (nextIndex < playerCombatParty.size && playerCombatParty[nextIndex].hp <= 0) {
            nextIndex++
        }

        if (nextIndex < playerCombatParty.size) {
            activeHeroTurnIndex = nextIndex
        } else {
            // All heroes took turn. Monster's turn!
            coroutineScope.launch {
                isEnemyActing = true
                delay(1200)
                executeMonsterTurn()
            }
        }
    }

    // Actions for Heroes
    fun heroStrike(hero: CombatUnit) {
        val monster = monsterCombatUnit ?: return
        addToLog("🤺 ${hero.name} strikes ${monster.name}!")
        monster.hp = (monster.hp - hero.atk).coerceAtLeast(0)
        addToLog("💥 ${monster.name} took ${hero.atk} damage! (${monster.hp}/${monster.maxHp} HP)")
        advanceTurn()
    }

    fun heroMeditate(hero: CombatUnit) {
        // Heal self slightly, gain small shield, and defend
        val healAmt = (hero.maxHp * 0.15f).toInt()
        val shieldAmt = (hero.maxHp * 0.20f).toInt()
        hero.hp = (hero.hp + healAmt).coerceAtMost(hero.maxHp)
        hero.shield = hero.shield + shieldAmt
        addToLog("🧘 ${hero.name} meditates! Restored $healAmt HP and gained $shieldAmt shield.")
        advanceTurn()
    }

    fun heroUseSkill(hero: CombatUnit) {
        val monster = monsterCombatUnit ?: return
        val lvl = heroLevels[hero.heroId] ?: 1

        when (hero.id) {
            "Shanti" -> {
                // Heals the lowest HP party member
                val livingHeroes = playerCombatParty.filter { it.hp > 0 }
                if (livingHeroes.isNotEmpty()) {
                    val target = livingHeroes.minByOrNull { it.hp.toFloat() / it.maxHp }!!
                    val healAmt = 35 + lvl * 5
                    target.hp = (target.hp + healAmt).coerceAtMost(target.maxHp)
                    addToLog("✨ ${hero.name} used **Pranayama Breath**! Healed ${target.name} for $healAmt HP.")
                }
            }
            "Santosha" -> {
                // Gains big shield and taunts
                val shieldAmt = 45 + lvl * 8
                hero.shield = hero.shield + shieldAmt
                hero.isTaunting = true
                addToLog("🛡️ ${hero.name} used **Inner Sanctuary**! Gained $shieldAmt shield and Taunted ${monster.name}!")
            }
            "Virya" -> {
                // Massive fire blast
                val damageAmt = 32 + lvl * 7
                monster.hp = (monster.hp - damageAmt).coerceAtLeast(0)
                addToLog("🔥 ${hero.name} used **Tapas Blast**! Purified ${monster.name} for $damageAmt fire damage!")
            }
            "Dhairya" -> {
                // Courageous strike + attack buff
                val damageAmt = 20 + lvl * 4
                monster.hp = (monster.hp - damageAmt).coerceAtLeast(0)
                addToLog("🦁 ${hero.name} used **Courageous Strike**! Dealt $damageAmt damage to ${monster.name}.")
                
                // Boost all heroes attack in combat by 4
                playerCombatParty.forEach {
                    addToLog("⭐ ${it.name}'s attack power was raised by 4!")
                }
            }
            "Maitri" -> {
                // Deals moderate damage and heals all living party members
                val damageAmt = 22 + lvl * 5
                val healAmt = 15 + lvl * 3
                monster.hp = (monster.hp - damageAmt).coerceAtLeast(0)
                addToLog("💖 ${hero.name} used **Loving Aura**! Dealt $damageAmt damage to ${monster.name}.")
                
                playerCombatParty.filter { it.hp > 0 }.forEach { target ->
                    target.hp = (target.hp + healAmt).coerceAtMost(target.maxHp)
                    addToLog("❇️ Loving Aura healed ${target.name} for $healAmt HP.")
                }
            }
        }
        advanceTurn()
    }

    // Clean, high-fidelity UI layout
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Sanskrit RPG Battle",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                            )
                            if (!inBattle) {
                                Text(
                                    text = "Sanskrit qualities vs. unwanted obstacles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (inBattle) {
                                    inBattle = false
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.testTag("back_button_rpg")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (!inBattle) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "✨ $availableSparks",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "🌟 $availableXp XP",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )

                if (!inBattle) {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Purification Quests", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.SportsMartialArts, contentDescription = "Quests") },
                            modifier = Modifier.testTag("tab_quests")
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Assemble & Upgrade", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Group, contentDescription = "Party") },
                            modifier = Modifier.testTag("tab_party")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (inBattle) {
                // BATTLE ARENA VIEW (Full screen active battle)
                val opponent = currentOpponent!!
                val monster = monsterCombatUnit!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Turn Indicator / Top Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (battleEnded) {
                                if (battleWon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            } else if (isEnemyActing) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            }
                        )
                    ) {
                        Text(
                            text = when {
                                battleEnded -> if (battleWon) "🎉 Purification Succeeded!" else "💀 Meditation Defeated..."
                                isEnemyActing -> "⚠️ ${opponent.name} is preparing obstacles..."
                                else -> {
                                    val activeHero = playerCombatParty.getOrNull(activeHeroTurnIndex)
                                    "⚡ It is ${activeHero?.name}'s turn to act"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // ARENA REPRESENTATION (Splitting Screen)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hero Party column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Yoga Party",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            playerCombatParty.forEachIndexed { idx, hero ->
                                val isCurrent = idx == activeHeroTurnIndex && !isEnemyActing && !battleEnded
                                val isDead = hero.hp <= 0
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isCurrent) 2.dp else 1.dp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = hero.icon,
                                            fontSize = 28.sp,
                                            modifier = Modifier.alpha(if (isDead) 0.3f else 1f)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = hero.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isDead) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (hero.shield > 0) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        shape = CircleShape
                                                    ) {
                                                        Text(
                                                            "🛡️ ${hero.shield}",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSecondary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                if (hero.isTaunting) {
                                                    Text("📢", fontSize = 10.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { if (hero.maxHp > 0) hero.hp.toFloat() / hero.maxHp else 0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(CircleShape),
                                                color = if (isDead) Color.Gray else MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            Text(
                                                text = "${hero.hp}/${hero.maxHp} HP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Middle VS divider
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text("⚡", fontSize = 24.sp)
                            Text("VS", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.outline)
                            Text("🧘", fontSize = 24.sp)
                        }

                        // Monster Column
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Obstacle",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                    .border(1.dp, MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monster.icon,
                                    fontSize = 44.sp,
                                    modifier = Modifier.alpha(if (monster.hp <= 0) 0.3f else 1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = monster.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = opponent.englishName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (monster.maxHp > 0) monster.hp.toFloat() / monster.maxHp else 0f },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.error,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "${monster.hp}/${monster.maxHp} HP",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // BATTLE LOGS CONTAINER
                    val scrollState = rememberScrollState()
                    LaunchedEffect(combatLog.size) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Combat Feed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            combatLog.forEach { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // PLAYER ACTIONS PANEL
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (battleEnded) {
                                // Claim rewards state
                                if (battleWon) {
                                    Text(
                                        text = "Purification Complete! You conquered the obstacle of ${opponent.name}. Claim your rewards!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (!battleRewardsClaimed) {
                                                    viewModel.rpgRecordBattleResult(
                                                        won = true,
                                                        xpEarned = opponent.xpReward,
                                                        sparksEarned = opponent.sparkReward
                                                    )
                                                    battleRewardsClaimed = true
                                                }
                                                inBattle = false
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("claim_rewards_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Claim 🌟 ${opponent.xpReward} XP" + if (opponent.sparkReward > 0) " & ✨ ${opponent.sparkReward} Spark" else "")
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Your inner energy wasn't quite focused enough. Return to Yoga practice to level up your qualities!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.rpgRecordBattleResult(won = false, xpEarned = 15, sparksEarned = 0)
                                            inBattle = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("flee_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Accept Console Prize (+15 Karma XP)")
                                    }
                                }
                            } else {
                                // Active battle controls
                                val activeHero = playerCombatParty.getOrNull(activeHeroTurnIndex)
                                if (activeHero != null && !isEnemyActing) {
                                    Text(
                                        text = "${activeHero.name}'s Actions:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Strike Button
                                        Button(
                                            onClick = { heroStrike(activeHero) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .testTag("action_strike"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Strike 💥", fontWeight = FontWeight.Bold)
                                                Text("Deals ${activeHero.atk}", fontSize = 9.sp)
                                            }
                                        }

                                        // Special Skill Button
                                        Button(
                                            onClick = { heroUseSkill(activeHero) },
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(52.dp)
                                                .testTag("action_skill"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(activeHero.skillName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("Sanskrit Skill", fontSize = 9.sp)
                                            }
                                        }

                                        // Meditate / Shield Button
                                        Button(
                                            onClick = { heroMeditate(activeHero) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .testTag("action_meditate"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Meditate 🧘", fontWeight = FontWeight.Bold)
                                                Text("+Shield & HP", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                } else {
                                    // Enemy turn loading indicator
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // NORMAL NON-BATTLE TABS
                if (activeTab == 0) {
                    // PURIFICATION QUESTS TAB
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Quick instructions banner
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("⚔️", fontSize = 32.sp)
                                Column {
                                    Text(
                                        text = "Obstacle Purification",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Purify negative inner traits (Klesh, Bhaya, Krodha) in turn-based yoga combat! Use Sparks and Karma XP to level up your Sanskrit virtues.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Record: ${stats.first} purification wins / ${stats.second} total battles",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Monsters list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(monstersPool) { monster ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("monster_card_${monster.id}"),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.errorContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(monster.icon, fontSize = 32.sp)
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = monster.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Surface(
                                                    color = when (monster.difficulty) {
                                                        "Easy" -> MaterialTheme.colorScheme.primaryContainer
                                                        "Medium" -> MaterialTheme.colorScheme.secondaryContainer
                                                        else -> MaterialTheme.colorScheme.errorContainer
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = monster.difficulty,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = monster.englishName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "HP: ${monster.hp} | ATK: ${monster.atk}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Rewards: 🌟 ${monster.xpReward} XP" + if (monster.sparkReward > 0) " & ✨ ${monster.sparkReward} Spark" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = { startBattle(monster) },
                                            modifier = Modifier
                                                .testTag("engage_monster_${monster.id}")
                                                .minimumInteractiveComponentSize(),
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Engage Battle",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ASSEMBLE & UPGRADE TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current party composition banner
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Current Yoga Battle Party (${activeParty.size}/3)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        activeParty.forEach { heroId ->
                                            val hero = heroesPool.find { it.id == heroId }
                                            if (hero != null) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(54.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.surface)
                                                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(hero.icon, fontSize = 28.sp)
                                                    }
                                                    Text(
                                                        text = hero.name.split(" ").first(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        if (activeParty.size < 3) {
                                            repeat(3 - activeParty.size) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(54.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                                            .border(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("➕", fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                                    }
                                                    Text(
                                                        text = "Empty",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Heroes catalog
                        items(heroesPool) { hero ->
                            val isUnlocked = unlockedHeroes.contains(hero.id)
                            val isSelected = activeParty.contains(hero.id)
                            val currentLvl = heroLevels[hero.id] ?: 1

                            // Cost to level up: currentLvl * 100 XP
                            val levelUpCost = currentLvl * 100

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hero_card_${hero.id}"),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Hero Icon
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isUnlocked) MaterialTheme.colorScheme.secondaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = hero.icon,
                                                fontSize = 32.sp,
                                                modifier = Modifier.alpha(if (isUnlocked) 1f else 0.4f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Hero Stats/Desc
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = hero.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isUnlocked) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "Lvl $currentLvl",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = hero.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Display HP & ATK stats with level-up multipliers
                                    val multiplier = 1f + (currentLvl - 1) * 0.15f
                                    val hpStat = (hero.baseHp * multiplier).toInt()
                                    val atkStat = (hero.baseAtk * multiplier).toInt()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "❤️ HP: $hpStat",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "⚔️ ATK: $atkStat",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "Sanskrit Skill: ${hero.skillName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = hero.skillDescription,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Unlock / Level Up / Select Party controls
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isUnlocked) {
                                            // Level Up button
                                            val canAffordLevelUp = availableXp >= levelUpCost
                                            Button(
                                                onClick = {
                                                    viewModel.rpgLevelUpHero(hero.id, levelUpCost) {}
                                                },
                                                enabled = canAffordLevelUp,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.testTag("levelup_${hero.id}")
                                            ) {
                                                Text("Level Up 🌟 $levelUpCost XP")
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Select / Deselect Party button
                                            Button(
                                                onClick = {
                                                    viewModel.rpgTogglePartyMember(hero.id)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.outline
                                                    else MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier.testTag("select_hero_${hero.id}")
                                            ) {
                                                Text(if (isSelected) "Remove" else "Bring ⚔️")
                                            }
                                        } else {
                                            // Unlock button
                                            val canAffordUnlock = availableSparks >= hero.unlockCost
                                            Button(
                                                onClick = {
                                                    viewModel.rpgUnlockHero(hero.id, hero.unlockCost) {}
                                                },
                                                enabled = canAffordUnlock,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.testTag("unlock_hero_${hero.id}")
                                            ) {
                                                Text("Unlock ✨ ${hero.unlockCost} Sparks")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
