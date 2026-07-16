# Karma XP Currency & Skill Unlock System — Implementation Plan

## Overview

Add **Karma XP** as a second currency (earned alongside gold from battles and yoga XP sync), and gate skills behind individual **unlock purchases**. Skills are no longer automatically available at level 1; each hero starts with one free offensive skill, and the rest must be explicitly purchased with Karma XP.

---

## Dependency Order & Step Sequence

Perform steps in numeric order. Steps within a phase may be done in any order, but all steps in Phase N must complete before starting Phase N+1.

| Phase | Step | Description |
|-------|------|-------------|
| **1** | 1.1 | Add `karmaXpCost` field to `Skill` data class |
| **1** | 1.2 | Update `heroes.json` to add `karmaXpCost` to all skills |
| **2** | 2.1 | Add `karmaXp` and `unlockedSkillIds` to `GameProgress` |
| **2** | 2.2 | Update `default_save.json` |
| **3** | 3.1 | Add `karmaXp` and `unlockedSkillIds` to `GameSaveManager` migration + normalize + legacy loader |
| **4** | 4.1 | Update `GameSyncManager` to compute Karma XP from yoga XP delta |
| **4** | 4.2 | Update `BattleOrchestrator.onBattleWon()` to award Karma XP |
| **5** | 5.1 | Add `unlockSkill()` and `isSkillUnlocked()` to `PartyManager` |
| **5** | 5.2 | Expose `unlockSkill` and `isSkillUnlocked` in `GameViewModel` |
| **6** | 6.1 | Filter unlocked skills in `ActionTray` / `HandOfCards` |
| **6** | 6.2 | Filter unlocked skills in `Hero.toCombatantState()` |
| **7** | 7.1 | Update `PartyScreen` header to show Karma XP |
| **7** | 7.2 | Update `HeroDetailsDialog` to show lock/unlock state per skill |
| **8** | 8.1 | Update unit tests |

---

## Phase 1 — Skill Model Changes

### Step 1.1: Add `karmaXpCost` field to `Skill.kt`

**File:** `app/src/main/java/com/example/game/model/Skill.kt`

At line 47, the `Skill` data class begins. Add a new parameter after `cooldown` (line 64):

```kotlin
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val targetType: TargetType,
    val combinedTypes: List<SkillType> = listOf(SkillType.DAMAGE),
    val damageComponents: List<DamageComponent> = emptyList(),
    val baseDamage: Int = 0,
    val damagePerLevel: Int = 0,
    val hits: Int = 1,
    val healScaling: HealScaling? = null,
    val shieldScaling: ShieldScaling? = null,
    val statusEffects: List<StatusEffectInfliction> = emptyList(),
    val buffs: List<BuffApplication> = emptyList(),
    val cleanse: Boolean = false,
    val revive: Boolean = false,
    val ultimateGain: Int = 20,
    val cooldown: Int = 0,
    val karmaXpCost: Int = 0   // <-- ADD THIS LINE
)
```

**Logic:** Default value is `0` so existing JSON without the field deserializes as "already unlocked" (backward-compatible). The `getMechanicsDescription()` method (line 66) does NOT need changes — Karma XP cost is displayed separately in the UI.

---

### Step 1.2: Add `karmaXpCost` to every skill in `heroes.json`

**File:** `app/src/main/assets/game/heroes.json`

Add `"karmaXpCost": <value>` to every skill object in the JSON. The JSON file is ~687 lines. Every hero has a `skills` array (4–5 skills each) plus an `ultimate` object.

**Rules for assigning costs:**
- **First offensive skill** (DAMAGE type, usually named `_basic`): `karmaXpCost = 0` (starter, auto-unlocked)
- **Ultimate skills**: `karmaXpCost = 0` (always available when gauge is full — but the hero must be leveled up to use it, which is an existing mechanic; or alternatively, you can decide that ultimates should also require unlock via Karma XP — the plan defaults to 0 per existing behavior)
- **Other skills**: assign increasing cost values. Suggested scale:
  - Secondary skills: `50–100` Karma XP
  - Advanced skills (3rd/4th slot): `150–250` Karma XP
  - Powerful AOE/cleansing skills: `300–500` Karma XP

**Exact locations in `heroes.json`:**

| Hero | Skill ID | Suggested `karmaXpCost` |
|------|----------|-------------------------|
| Shanti (id=1) | `shanti_basic` | 0 |
| Shanti | `shanti_skill1` (Pranayama Breath) | 100 |
| Shanti | `shanti_skill2` (Calming Presence) | 150 |
| Shanti | `shanti_skill3` (Serene Renewal) | 300 |
| Shanti | `shanti_skill4` (Rippling Current) | 200 |
| Shanti | `shanti_ultimate` | 0 |
| Santosha (id=2) | `santosha_basic` | 0 |
| Santosha | `santosha_skill1` (Inner Sanctuary) | 100 |
| Santosha | `santosha_skill2` (Solid Foundation) | 200 |
| Santosha | `santosha_skill3` ... | see JSON for actual IDs, assign 150–300 |
| Santosha | `santosha_ultimate` | 0 |
| Virya (id=3) | `virya_basic` | 0 |
| Virya | all other skills | 100–500 |
| Virya | `virya_ultimate` | 0 |
| Dhairya (id=4) | `dhairya_basic` | 0 |
| Dhairya | all other skills | 100–500 |
| Dhairya | `dhairya_ultimate` | 0 |
| Maitri (id=5) | `maitri_basic` | 0 |
| Maitri | all other skills | 100–500 |
| Maitri | `maitri_ultimate` | 0 |

**JSON syntax:** Add `"karmaXpCost": N` as the last field before each skill object's closing `}`. For example, in `shanti_basic` (around line 32):

```json
"cooldown": 0,
"karmaXpCost": 0
```

And for `shanti_skill1` (around line 51):

```json
"cooldown": 2,
"karmaXpCost": 100
```

**IMPORTANT:** Read the full `heroes.json` file to determine exact line numbers and the exact skill IDs for heroes 3–5 before editing. The pattern shown for Shanti and Santosha applies to all heroes.

---

## Phase 2 — GameProgress Model Changes

### Step 2.1: Add `karmaXp` and `unlockedSkillIds` to `GameProgress.kt`

**File:** `app/src/main/java/com/example/game/model/GameProgress.kt`

Add two new fields to the `GameProgress` data class. Insert them after `gold` (line 12) and before `totalBattlesWon`:

```kotlin
data class GameProgress(
    val version: Int = 3,
    val party: List<PartyMemberData> = emptyList(),
    val unlockedHeroIds: Set<Int> = emptySet(),
    val defeatedMonsterIds: Set<String> = emptySet(),
    val inventory: List<String> = emptyList(),
    val sparks: Int = 0,
    val yogaLevel: Int = 1,
    val totalYogaXp: Int = 0,
    val gold: Int = 0,
    val karmaXp: Int = 0,                              // <-- ADD
    val unlockedSkillIds: Map<Int, Set<String>> = emptyMap(),  // <-- ADD (heroId -> set of skill IDs)
    val totalBattlesWon: Int = 0,
    val syncedYogaSparks: Int = 0,
    val earnedTrophyIds: Set<String> = emptySet(),
    val lastPlayedTimestamp: Long = 0L
)
```

**Type semantics:**
- `karmaXp: Int` — total Karma XP currency the player currently has. Default 0.
- `unlockedSkillIds: Map<Int, Set<String>>` — maps hero ID to set of skill ID strings that have been purchased/unlocked for that hero. Default `emptyMap()` (meaning no skills unlocked; the migration will populate starter skills).

Note: In the `GameProgress.normalized()` function in GameSaveManager (step 3.1), we will auto-populate starter skill unlocks for heroes in the party.

---

### Step 2.2: Update `default_save.json`

**File:** `app/src/main/assets/game/default_save.json`

Add `karmaXp` and `unlockedSkillIds` to the default save, matching the new defaults:

```json
{
  "battleState": null,
  "party": [{"heroId": 1}],
  "unlockedHeroIds": [1],
  "sparks": 0,
  "yogaLevel": 1,
  "earnedTrophyIds": [],
  "totalBattlesWon": 0,
  "inventory": [],
  "lastPlayedTimestamp": 0,
  "totalYogaXp": 0,
  "gold": 0,
  "karmaXp": 0,
  "unlockedSkillIds": {},
  "defeatedMonsterIds": []
}
```

The `syncYogaSparks` field is absent from this JSON (it uses the default value of 0 from the data class), which is fine.

---

## Phase 3 — Save/Load Migration

### Step 3.1: Update `GameSaveManager.kt`

**File:** `app/src/main/java/com/example/game/persistence/GameSaveManager.kt`

Three changes needed:

**(A) `normalized()` function (line 152):** Add normalization logic to auto-unlock starter offensive skills for heroes already in the party. This handles migration from old saves that lack `unlockedSkillIds`.

Replace the `normalized()` function with this expanded version:

```kotlin
private fun GameProgress.normalized(): GameProgress {
    var result = copy(
        version = 3,
        party = (party ?: emptyList()).map {
            it.copy(
                level = it.level ?: 1,
                equippedItemIds = it.equippedItemIds.orEmpty()
            )
        },
        unlockedHeroIds = unlockedHeroIds ?: emptySet(),
        defeatedMonsterIds = (defeatedMonsterIds ?: emptySet()).map(::normalizeMonsterId).toSet(),
        karmaXp = karmaXp ?: 0,
        unlockedSkillIds = (unlockedSkillIds ?: emptyMap()).mapValues { (_, v) ->
            (v ?: emptySet()).toSet()
        }.toMutableMap()
    )

    // Ensure hero 1 is always unlocked
    if (1 !in result.unlockedHeroIds) {
        result = result.copy(unlockedHeroIds = result.unlockedHeroIds + 1)
    }
    if (result.party.none { it.heroId == 1 }) {
        result = result.copy(party = result.party + PartyMemberData(heroId = 1))
    }

    // Auto-unlock starter offensive skills for all heroes in the party
    val mutableSkills = result.unlockedSkillIds.toMutableMap()
    result.party.forEach { pm ->
        val heroDef = DataLoader.heroes.find { it.id == pm.heroId }
        if (heroDef != null) {
            val existing = mutableSkills[pm.heroId] ?: emptySet()
            val starterSkills = heroDef.skills
                .filter { it.karmaXpCost == 0 && it.ultimateGain != 0 } // exclude ultimate
                .map { it.id }
                .toSet()
            mutableSkills[pm.heroId] = existing + starterSkills
        }
    }
    result = result.copy(unlockedSkillIds = mutableSkills)

    return result
}
```

**Note:** `DataLoader.init()` must be called before normalization. Currently `loadGame()` is called in `GameViewModel.loadGame()` which runs after `DataLoader.init()`. The `normalized()` function is called inside `loadGame()`, `saveGame()`, and `loadDefaultSave()`. Ensure `DataLoader` is initialized before any of these are called. If `DataLoader.heroes` is accessed via lazy init, the first call to `loadGame()` after `DataLoader.init()` will work — but `loadDefaultSave()` in `resetToDefault()` might fail if `DataLoader` is not initialized. We need to add a guard:

**At line 68, inside `loadDefaultSave()`**, wrap the `normalized()` call so it does NOT try to access `DataLoader.heroes` for migration if DataLoader is not yet initialized. A simple approach: check `DataLoader::context.isInitialized`.

**(B) `loadLegacySave()` function (line 78):** Add `karmaXp` and `unlockedSkillIds` defaults when loading legacy key-value saves:

```kotlin
return GameProgress(
    party = readJsonList(KEY_PARTY, emptyList<PartyMemberData>()),
    unlockedHeroIds = readJsonStringSet(KEY_UNLOCKED_HERO_IDS)
        .mapNotNull { id -> stringHeroIdToInt[id.trim().lowercase()] }.toSet(),
    sparks = prefs.getInt(KEY_SPARKS, 0),
    yogaLevel = prefs.getInt(KEY_YOGA_LEVEL, 1),
    earnedTrophyIds = readJsonStringSet(KEY_EARNED_TROPHY_IDS),
    totalBattlesWon = prefs.getInt(KEY_TOTAL_BATTLES_WON, 0),
    inventory = readJsonList(KEY_INVENTORY, emptyList<String>()),
    lastPlayedTimestamp = prefs.getLong(KEY_LAST_PLAYED_TIMESTAMP, 0L),
    totalYogaXp = xp,
    gold = xp / 10,
    karmaXp = 0,                    // <-- ADD (legacy saves start with 0)
    unlockedSkillIds = emptyMap(),  // <-- ADD (will be populated by normalized())
    defeatedMonsterIds = readJsonStringSet(KEY_DEFEATED_MONSTER_IDS)
)
```

**(C) No changes needed in `migrateBlobIfNeeded()`** — the JSON blob already uses Gson deserialization, and new fields will default to their Kotlin default values (`0` for `karmaXp`, `emptyMap()` for `unlockedSkillIds`).

**(D) Bump the save version?** No — we use the `normalized()` function to handle migration transparently. The version stays at 3 because the blob structure is backward-compatible (new fields just get default values during deserialization).

---

## Phase 4 — Award Karma XP

### Step 4.1: Update `GameSyncManager` to compute Karma XP

**File:** `app/src/main/java/com/example/game/viewmodel/GameSyncManager.kt`

In `syncWithMainApp()`, after the gold computation (lines 57–61), add Karma XP computation. Karma XP is awarded at **3x the gold rate**: `xp_delta / 3` (vs gold's `xp_delta / 10`).

Add this block after line 61 (inside the `if (xpSum > data.totalYogaXp)` block):

```kotlin
if (xpSum > data.totalYogaXp) {
    val newGoldEarned = (xpSum - data.totalYogaXp) / 10
    if (newGoldEarned > 0) {
        updated = updated.copy(gold = updated.gold + newGoldEarned)
    }
    val newKarmaXpEarned = (xpSum - data.totalYogaXp) / 3   // <-- ADD (3x gold rate)
    if (newKarmaXpEarned > 0) {
        updated = updated.copy(karmaXp = updated.karmaXp + newKarmaXpEarned)
    }
}
```

This means for every 10 XP difference from yoga practice:
- Gold = +1
- Karma XP = +3 (approximately 3x the gold rate)

---

### Step 4.2: Update `BattleOrchestrator.onBattleWon()` to award Karma XP

**File:** `app/src/main/java/com/example/game/viewmodel/BattleOrchestrator.kt`

In `onBattleWon()` (line 244), after the gold reward logic (lines 254–261), add Karma XP reward. Karma XP scales similarly to gold but at ~2x the rate:

Immediately after the `goldReward` val block (line 260), add:

```kotlin
val karmaXpReward = when (monster.difficultyTier) {
    DifficultyTier.EASY -> 20
    DifficultyTier.MEDIUM -> 50
    DifficultyTier.HARD -> 100
    DifficultyTier.BOSS -> 200
    DifficultyTier.SUPERBOSS -> 400
}
```

Then in the `_saveData.value = data.copy(...)` block at line 312, add `karmaXp` to the copy:

```kotlin
_saveData.value = data.copy(
    totalBattlesWon = data.totalBattlesWon + 1,
    defeatedMonsterIds = defeatedIds,
    gold = data.gold + goldReward,
    karmaXp = data.karmaXp + karmaXpReward,   // <-- ADD
    inventory = inventory,
    earnedTrophyIds = existing.toSet(),
    lastPlayedTimestamp = System.currentTimeMillis()
)
```

---

## Phase 5 — Business Logic: Unlock & Check Skills

### Step 5.1: Add `unlockSkill()` and `isSkillUnlocked()` to `PartyManager`

**File:** `app/src/main/java/com/example/game/viewmodel/PartyManager.kt`

Add two methods to the `PartyManager` class (after `getEquippedItems()` at line 101):

```kotlin
fun isSkillUnlocked(heroId: Int, skillId: String): Boolean {
    val unlockedForHero = _saveData.value.unlockedSkillIds[heroId] ?: emptySet()
    return skillId in unlockedForHero
}

fun unlockSkill(heroId: Int, skillId: String): Boolean {
    val data = _saveData.value
    val heroDef = DataLoader.heroes.find { it.id == heroId } ?: return false
    val skill = (heroDef.skills + heroDef.ultimate).find { it.id == skillId } ?: return false

    // Already unlocked?
    val currentUnlocked = data.unlockedSkillIds[heroId] ?: emptySet()
    if (skillId in currentUnlocked) return false

    // Can afford?
    if (data.karmaXp < skill.karmaXpCost) return false

    // Must own the hero
    if (heroId !in data.unlockedHeroIds) return false

    // Deduct and unlock
    val newUnlocked = currentUnlocked + skillId
    val newUnlockedMap = data.unlockedSkillIds.toMutableMap()
    newUnlockedMap[heroId] = newUnlocked

    _saveData.value = data.copy(
        karmaXp = data.karmaXp - skill.karmaXpCost,
        unlockedSkillIds = newUnlockedMap
    )
    saveManager.saveGame(_saveData.value)
    return true
}
```

---

### Step 5.2: Expose new methods in `GameViewModel`

**File:** `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt`

Add these delegations after line 97 (near other partyManager delegations):

```kotlin
fun isSkillUnlocked(heroId: Int, skillId: String): Boolean = partyManager.isSkillUnlocked(heroId, skillId)
fun unlockSkill(heroId: Int, skillId: String): Boolean = partyManager.unlockSkill(heroId, skillId)
```

---

## Phase 6 — Filter Skills in Battle

### Step 6.1: Filter unlocked skills in `HandOfCards`

**File:** `app/src/main/java/com/example/game/ui/components/ActionTray.kt`

The `HandOfCards` composable (line 110) builds the card list at lines 121–127:

```kotlin
val allCards: List<Any> = buildList {
    currentHero.skills.forEach { add(it) }
    if (currentHero.ultimate != null) add(currentHero.ultimate!!)
    availableCombos.filter { ... }.forEach { add(it) }
}
```

**Change:** Currently `currentHero.skills` is the full list from `CombatantState`. We will filter skills at the `CombatantState` level instead (see step 6.2). So we do NOT need to change `HandOfCards` — it will receive an already-filtered skill list.

However, `ActionTray` receives `currentHero: CombatantState` from `BattleScene`. The `CombatantState.skills` field (defined in `BattleState.kt` line 36) is populated from `Hero.toCombatantState()`. We change it at the source.

---

### Step 6.2: Filter unlocked skills in `Hero.toCombatantState()`

**File:** `app/src/main/java/com/example/game/model/Hero.kt`

The `toCombatantState()` function (line 30) creates a `CombatantState` with `skills = skills` (line 60). We need to filter skills to only include those unlocked for the hero.

**Problem:** `toCombatantState()` does not currently have access to the `unlockedSkillIds` map. We need to pass it as a parameter.

**Approach:** Add an `unlockedSkillIds: Map<Int, Set<String>>` parameter to `toCombatantState()`:

```kotlin
fun Hero.toCombatantState(
    partyMember: PartyMemberData,
    equippedEquipment: List<Equipment> = emptyList(),
    unlockedSkillIds: Map<Int, Set<String>> = emptyMap()   // <-- ADD parameter
): CombatantState {
    val atkPercent = ...
    // ... existing stat calculations ...

    val heroUnlockedSkills = unlockedSkillIds[partyMember.heroId] ?: emptySet()
    val availableSkills = skills.filter { it.id in heroUnlockedSkills }

    return CombatantState(
        id = id.toString(),
        side = CombatSide.HERO,
        name = name.split(" ").first(),
        element = element,
        maxHp = finalHp,
        hp = finalHp,
        attack = finalAtk,
        speed = finalSpd,
        shield = initialShield,
        level = partyMember.level,
        skills = availableSkills,   // <-- CHANGED from `skills` to `availableSkills`
        ultimate = ultimate
    )
}
```

**Update call sites:** `BattleOrchestrator.startBattle()` (line 37–42) calls `heroDef.toCombatantState(pm, equipped)`. Change to:

```kotlin
val battleHeroes = partyMembers.mapNotNull { pm ->
    val heroDef = DataLoader.heroes.find { it.id == pm.heroId } ?: return@mapNotNull null
    val equipped = pm.equippedItemIds.mapNotNull { DataLoader.getEquipment(it) }
    heroDef.toCombatantState(pm, equipped, _saveData.value.unlockedSkillIds)  // <-- pass map
}
```

---

## Phase 7 — UI Changes

### Step 7.1: Show Karma XP in PartyScreen header

**File:** `app/src/main/java/com/example/game/ui/components/PartyScreen.kt`

In the `PartyScreen` composable (line 36), the header displays Sparks and Gold (lines 65–80). Add Karma XP display after the gold row.

After line 79 (closing of the gold `Text`), add:

```kotlin
Spacer(Modifier.width(12.dp))
Text("\uD83D\uDD2E", fontSize = 14.sp)  // Crystal ball emoji for Karma
Spacer(Modifier.width(4.dp))
Text(
    "${saveData.karmaXp}",
    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
    color = Color(0xFFAB47BC)  // Purple color for Karma XP
)
```

Also add Karma XP to the `ShopScreen.kt` header if it shows currency (examine lines 40–80 of ShopScreen to insert similarly).

---

### Step 7.2: Update `HeroDetailsDialog` to show lock/unlock per skill

**File:** `app/src/main/java/com/example/game/ui/components/PartyScreen.kt`

The `HeroDetailsDialog` composable (line 249) shows skills at lines 434–445. Currently each skill is rendered unconditionally via `HeroDetailSkillCard`. We need to:

**(A)** Add a state variable for confirmation dialog (for unlocking):

```kotlin
var confirmUnlockSkillId by remember { mutableStateOf<String?>(null) }
```

**(B)** Show a confirmation dialog when `confirmUnlockSkillId != null`:

```kotlin
confirmUnlockSkillId?.let { skillId ->
    val skill = (hero.skills + hero.ultimate).find { it.id == skillId }
    if (skill != null) {
        AlertDialog(
            onDismissRequest = { confirmUnlockSkillId = null },
            title = { Text("Unlock ${skill.name}?") },
            text = { Text("Cost: ${skill.karmaXpCost} Karma XP\n\nYou have: ${saveData.karmaXp} Karma XP") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unlockSkill(hero.id, skillId)
                    confirmUnlockSkillId = null
                }) {
                    Text("Unlock", color = Color(0xFFAB47BC))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnlockSkillId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

**(C)** Change each skill card rendering (lines 434–445) to check unlock status. Replace the `hero.skills.forEach { skill -> ... }` block at line 434:

```kotlin
hero.skills.forEach { skill ->
    val isUnlocked = viewModel.isSkillUnlocked(hero.id, skill.id)
    HeroDetailSkillCard(
        skill = skill,
        hero = hero,
        level = partyMember.level,
        heroColor = heroColor,
        isLocked = !isUnlocked,
        karmaXpCost = skill.karmaXpCost,
        modifier = Modifier.clickable {
            if (isUnlocked) {
                selectedSkill = skill
            } else if (skill.karmaXpCost > 0) {
                confirmUnlockSkillId = skill.id
            }
        }
    )
}
```

**(D)** Update `HeroDetailSkillCard` signature (line 454) to accept new parameters:

```kotlin
@Composable
private fun HeroDetailSkillCard(
    skill: Skill,
    hero: Hero,
    level: Int,
    isUltimate: Boolean = false,
    heroColor: Color = Color.Gray,
    isLocked: Boolean = false,      // <-- ADD
    karmaXpCost: Int = 0,           // <-- ADD
    modifier: Modifier = Modifier
)
```

Inside the composable body, when `isLocked` is true, show a dimmed/greyed-out card with a lock icon and the Karma XP cost. The existing content (around lines 454–512) can be conditionally shown or greyed:

- If `isLocked`: set `alpha = 0.5f` on the card, overlay a lock icon, and replace the damage/type info with "Cost: N Karma XP".
- If unlocked: same as current behavior.

**Like the `HeroDetailSkillCard` composable at line 480:**

```kotlin
Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (isLocked) Color.Gray.copy(alpha = 0.1f) else typeColor.copy(alpha = 0.08f),
    border = BorderStroke(1.dp, if (isLocked) Color.Gray.copy(alpha = 0.3f) else typeColor.copy(alpha = 0.3f)),
    modifier = modifier.fillMaxWidth().padding(vertical = 3.dp).then(
        if (isLocked) Modifier.alpha(0.6f) else Modifier
    )
) {
    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(8.dp, 36.dp)
                .background(if (isLocked) Color.Gray else typeColor, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    skill.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) Color.Gray else typeColor
                )
                if (isLocked) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Lock, contentDescription = "Locked",
                        modifier = Modifier.size(16.dp), tint = Color.Gray
                    )
                }
                // ... existing typeLabel etc (only if unlocked) ...
            }
            if (isLocked) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cost: $karmaXpCost \uD83D\uDD2E",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAB47BC)
                )
            } else {
                // ... existing description, damage, heal, etc ...
            }
        }
        if (!isUltimate && !isLocked) {
            // ... existing ultimate gauge gain ...
        }
    }
}
```

---

## Phase 8 — Update Tests

### Step 8.1: Update unit tests for new fields

**(A) `GameProgressSerializationTest.kt`**

**File:** `app/src/test/java/com/example/game/model/GameProgressSerializationTest.kt`

The `default values on missing fields` test (line 45–57) expects a JSON `{}` to deserialize with defaults. Since `karmaXp` defaults to `0` and `unlockedSkillIds` defaults to `emptyMap()`, this test should still pass without changes (Gson uses Kotlin default parameter values).

The `serialize and deserialize` test (line 12) should be updated to include the new fields in the `original` data:

```kotlin
val original = GameProgress(
    version = 3,
    party = listOf(
        PartyMemberData(1, 3, listOf("training_blade")),
        PartyMemberData(3, 5, listOf("ember_pendant"))
    ),
    unlockedHeroIds = setOf(1, 3),
    sparks = 150,
    yogaLevel = 4,
    totalBattlesWon = 12,
    karmaXp = 250,
    unlockedSkillIds = mapOf(
        1 to setOf("shanti_basic", "shanti_skill1"),
        3 to setOf("virya_basic")
    )
)
```

And add assertions:

```kotlin
assertEquals(original.karmaXp, restored.karmaXp)
assertEquals(original.unlockedSkillIds, restored.unlockedSkillIds)
```

**(B) `GameSaveManagerTest.kt`**

**File:** `app/src/test/java/com/example/game/persistence/GameSaveManagerTest.kt`

**Default save test** (line 26–34): The default save from assets now has `karmaXp: 0` and `unlockedSkillIds: {}`. The normalization function will auto-unlock starter skills for hero 1 (Shanti). After `loadGame()`, `unlockedSkillIds` should contain `1 -> {"shanti_basic"}` (since Shanti's starter skill has `karmaXpCost: 0` and `ultimateGain != 0`).

**Roundtrip test** (line 38–73): Add `karmaXp` and `unlockedSkillIds` to the test data:

```kotlin
val original = GameProgress(
    version = 3,
    party = listOf(
        PartyMemberData(1, 3, listOf("training_blade")),
        PartyMemberData(3, 5, listOf("ember_pendant"))
    ),
    unlockedHeroIds = setOf(1, 2, 3),
    sparks = 150,
    yogaLevel = 4,
    earnedTrophyIds = setOf("badge_bhaya", "trophy_fearless"),
    totalBattlesWon = 12,
    inventory = listOf("training_blade", "crystal_sword"),
    lastPlayedTimestamp = 1000000L,
    totalYogaXp = 5200,
    gold = 400,
    karmaXp = 300,                                     // <-- ADD
    unlockedSkillIds = mapOf(1 to setOf("shanti_basic", "shanti_skill1")),  // <-- ADD
    defeatedMonsterIds = setOf("bhaya", "tandra", "chinta")
)
```

Add corresponding assertions after the gold assertion (line 73):

```kotlin
assertEquals(original.karmaXp, loaded.karmaXp)
assertEquals(original.unlockedSkillIds, loaded.unlockedSkillIds)
```

Also update the `versioned JSON blob roundtrip` test (line 212) and the `multiple save cycles` test (line 150) to either include the new fields or ensure backward compatibility.

**(C) `PartyManager` tests (if any):** Search for existing PartyManager tests. If none exist, this plan does not require creating new ones. However, verify that `purchaseHero` auto-unlocks the starter skill (may need to add logic to `purchaseHero` in `PartyManager.kt` — see Phase 9 below).

---

## Phase 9 — Auto-Unlock Starter Skill on Hero Purchase

### Additional Logic in `PartyManager.purchaseHero()`

**File:** `app/src/main/java/com/example/game/viewmodel/PartyManager.kt`

In `purchaseHero()` (line 13), after the hero is purchased, auto-unlock their starter offensive skill. Add the following after line 23 (`_saveData.value = data.copy(...)`):

Inside the `_saveData.value = data.copy(...)` block (lines 23–26), add `unlockedSkillIds` update:

```kotlin
val heroDef = DataLoader.getHero(heroId)
val starterSkills = heroDef.skills
    .filter { it.karmaXpCost == 0 && it.ultimateGain != 0 }
    .map { it.id }
    .toSet()
val newUnlockedMap = data.unlockedSkillIds.toMutableMap()
newUnlockedMap[heroId] = starterSkills

_saveData.value = data.copy(
    sparks = data.sparks - sparkCost,
    unlockedHeroIds = data.unlockedHeroIds + heroId,
    party = newParty,
    unlockedSkillIds = newUnlockedMap   // <-- ADD
)
```

---

## Summary of All Files Changed

| # | File | Phase | Changes |
|---|------|-------|---------|
| 1 | `app/src/main/java/com/example/game/model/Skill.kt` | 1.1 | Add `karmaXpCost: Int = 0` parameter |
| 2 | `app/src/main/assets/game/heroes.json` | 1.2 | Add `"karmaXpCost": N` to every skill object |
| 3 | `app/src/main/java/com/example/game/model/GameProgress.kt` | 2.1 | Add `karmaXp`, `unlockedSkillIds` fields |
| 4 | `app/src/main/assets/game/default_save.json` | 2.2 | Add `karmaXp: 0`, `unlockedSkillIds: {}` |
| 5 | `app/src/main/java/com/example/game/persistence/GameSaveManager.kt` | 3.1 | Update `normalized()`, `loadLegacySave()`, guard in `loadDefaultSave()` |
| 6 | `app/src/main/java/com/example/game/viewmodel/GameSyncManager.kt` | 4.1 | Compute Karma XP = xpDelta / 3 |
| 7 | `app/src/main/java/com/example/game/viewmodel/BattleOrchestrator.kt` | 4.2 | Award Karma XP in `onBattleWon()` |
| 8 | `app/src/main/java/com/example/game/viewmodel/PartyManager.kt` | 5.1 + 9 | Add `isSkillUnlocked()`, `unlockSkill()`; update `purchaseHero()` |
| 9 | `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt` | 5.2 | Expose `isSkillUnlocked()`, `unlockSkill()` |
| 10 | `app/src/main/java/com/example/game/model/Hero.kt` | 6.2 | Add `unlockedSkillIds` param to `toCombatantState()`, filter skills |
| 11 | `app/src/main/java/com/example/game/viewmodel/BattleOrchestrator.kt` | 6.2 | Pass `unlockedSkillIds` to `toCombatantState()` |
| 12 | `app/src/main/java/com/example/game/ui/components/ActionTray.kt` | 6.1 | (No changes needed if 6.2 is done) |
| 13 | `app/src/main/java/com/example/game/ui/components/PartyScreen.kt` | 7.1 + 7.2 | Add Karma XP to header; update `HeroDetailsDialog` skill cards with lock/unlock |
| 14 | `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` | 7.1 | Optionally add Karma XP display |
| 15 | `app/src/test/java/com/example/game/model/GameProgressSerializationTest.kt` | 8.1 | Add assertions for new fields |
| 16 | `app/src/test/java/com/example/game/persistence/GameSaveManagerTest.kt` | 8.1 | Add new fields to test data |

---

## Karma XP Earning Rates (Reference)

| Source | Karma XP Awarded |
|--------|-----------------|
| Yoga XP sync | `XP_delta / 3` (cf. gold: `XP_delta / 10`) |
| Battle — EASY | 20 |
| Battle — MEDIUM | 50 |
| Battle — HARD | 100 |
| Battle — BOSS | 200 |
| Battle — SUPERBOSS | 400 |

---

## Karma XP Cost Design Guidelines

| Skill Category | Typical Cost |
|---------------|-------------|
| Starter offensive skill (always 1 per hero) | 0 |
| Basic support/healing skill | 50–100 |
| Buff/debuff skill | 100–200 |
| AOE damage or strong healing | 200–300 |
| Mass cleanse/revive/advanced | 300–500 |
| Ultimate | 0 |

An EASY battle awards 20 Karma XP, so a 100-cost skill requires 5 easy battles. This keeps early progression feeling rewarding while gating later skills behind ~10–15 battles or 2–3 boss fights.

---

## Edge Cases & Gotchas

1. **DataLoader not initialized during `normalized()`:** If `loadDefaultSave()` or `resetToDefault()` is called before `DataLoader.init()`, the `normalized()` function's attempt to look up `DataLoader.heroes` will fail because `DataLoader.context` is not yet initialized. Solution: wrap the skill auto-unlock logic in `normalized()` with a `try-catch` or check `::context.isInitialized`:

   ```kotlin
   if (DataLoader::context.isInitialized) {
       // auto-unlock starter skills
   }
   ```

2. **Ultimates in skill list:** The current code treats `CombatantState.skills` and `CombatantState.ultimate` separately. Ultimates have `karmaXpCost = 0` by convention and are always available when gauge is full (no unlock needed). The `HandOfCards` separately adds the ultimate. Ensure the ultimate is NOT filtered out — only regular `skills` are filtered.

3. **Non-existent hero skills in migrate:** When migrating, if a hero in the party doesn't exist in `DataLoader.heroes`, we can't compute their starter skills. The code should skip such heroes gracefully (the `heroDef != null` check in the plan handles this).

4. **Battle replay / skill cooldowns:** `skillCooldowns` in `BattleState` are keyed by skill ID. Filtering skills from `CombatantState.skills` does NOT affect cooldown tracking. Unlocked skills that are on cooldown still show correctly.

5. **Combo skills:** These are not gated by unlock — they depend on hero presence, not individual skills. No changes needed for combos.

6. **Gson serialization of `Map<Int, Set<String>>`:** Gson handles this correctly. The JSON will look like `{"1":["shanti_basic","shanti_skill1"]}`.
