# Plan: Numerical Hero IDs (Bug Fix)

## Root Cause

Hero IDs are case-sensitive strings ("Shanti", "Santosha", etc.) in `heroes.json`. The `GameSaveManager.normalizeId()` lowercases all IDs when saving/loading (`"shanti"`, etc.). This causes every `==` comparison between DataLoader IDs (capitalized) and stored IDs (lowercase) to **fail**, breaking party member lookups, equipment purchases, level-up, and battle initiation.

Full bug inventory: **15+ comparison sites** across `GameViewModel.kt`, `PartyScreen.kt`, and `ShopScreen.kt`.

## Solution: Numerical IDs

Replace the string-based hero IDs with small integers throughout the entire codebase.

## Step-by-step

### Step 1: Update heroes.json

**File:** `app/src/main/assets/game/heroes.json`

Replace the existing `"id"` string values with integers. Keep the `"name"` field for display.

```json
[
  {
    "id": 1,
    "name": "Shanti (Calm)",
    ...
  },
  {
    "id": 2,
    "name": "Santosha (Content)",
    ...
  },
  ...
]
```

### Step 2: Update Hero model

**File:** `app/src/main/java/com/example/game/model/Hero.kt`

Change `val id: String` to `val id: Int`:

```kotlin
data class Hero(
    val id: Int,           // was String
    val name: String,
    ...
)
```

### Step 3: Update Equipment model

**File:** `app/src/main/java/com/example/game/model/Equipment.kt`

Change `val heroId: String?` to `val heroId: Int?`:

```kotlin
data class Equipment(
    ...
    val heroId: Int? = null,   // was String?
    ...
)
```

Update `getThemeColor()` to match on Int instead of String:

```kotlin
fun getThemeColor(): Color {
    if (tier == EquipmentTier.UNIQUE && heroId != null) {
        return when (heroId) {
            1 -> Color(0xFF2196F3)   // Shanti
            2 -> Color(0xFF795548)   // Santosha
            3 -> Color(0xFFF44336)   // Virya
            4 -> Color(0xFFFFD54F)   // Dhairya
            5 -> Color(0xFF81D4FA)   // Maitri
            else -> Color.Gray
        }
    }
    ...
}
```

### Step 4: Update PartyMemberData

**File:** `app/src/main/java/com/example/game/model/GameProgress.kt`

```kotlin
data class PartyMemberData(
    val heroId: Int,         // was String
    val level: Int = 1,
    val equippedItemIds: List<String> = emptyList()
)
```

And `GameProgress.unlockedHeroIds`:
```kotlin
data class GameProgress(
    ...
    val unlockedHeroIds: Set<Int> = emptySet(),   // was Set<String>
    ...
)
```

### Step 5: Update equipment.json

**File:** `app/src/main/assets/game/equipment.json`

Change all `"heroId"` values from strings to integers:

```json
{"id": "shanti_prayer_beads", ..., "heroId": 1, ...}
{"id": "santosha_foundation_stone", ..., "heroId": 2, ...}
{"id": "virya_ember_core", ..., "heroId": 3, ...}
...
```

### Step 6: Update monsters.json

**File:** `app/src/main/assets/game/monsters.json` (if `firstDefeatItemReward` was added from plan 04)

No change needed — reward items reference equipment IDs (strings), not hero IDs.

### Step 7: Update combos.json

**File:** `app/src/main/assets/game/combos.json`

Change `requiredHeroes` from string names to integer IDs:

```json
{"id": "combo_shanti_santosha", "requiredHeroes": [1, 2], ...}
{"id": "combo_shanti_virya", "requiredHeroes": [1, 3], ...}
...
```

### Step 8: Update GameSaveManager.kt

**File:** `app/src/main/java/com/example/game/persistence/GameSaveManager.kt`

Remove the `normalizeId()` function entirely. Update `normalized()`:

```kotlin
private fun GameProgress.normalized(): GameProgress =
    copy(
        version = 2,
        party = party.map { it.copy(equippedItemIds = it.equippedItemIds.map(::normalizeItemId)) },
        defeatedMonsterIds = defeatedMonsterIds.map(::normalizeMonsterId).toSet()
    )

// Equipment IDs and monster IDs still need normalization (lowercase)
private fun normalizeItemId(id: String): String = id.trim().lowercase()
private fun normalizeMonsterId(id: String): String = id.trim().lowercase()
```

Hero IDs are now `Int`, so no normalization needed for them.

Also update `unlockedHeroIds` persistence — needs to change from string-based to int-based storage. Use `Set<Int>` directly with Gson.

### Step 9: Update GameViewModel.kt

**File:** `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt`

All `heroId` parameters and comparisons need to change from `String` to `Int`:

- `getEquippedItems(heroId: String)` → `getEquippedItems(heroId: Int)`
- `levelUpHero(heroId: String)` → `levelUpHero(heroId: Int)`
- `purchaseHero(heroId: String)` → `purchaseHero(heroId: Int)`
- `equipItem(heroId: String, ...)` → `equipItem(heroId: Int, ...)`
- `unequipItem(heroId: String, ...)` → `unequipItem(heroId: Int, ...)`
- `getHeroLevelUpCost(heroId: String)` → `getHeroLevelUpCost(heroId: Int)`

Remove all `.lowercase()` calls on hero IDs. The `it.id == pm.heroId` comparisons will now work because both are `Int`.

Update the sync/save code that handles `unlockedHeroIds` — the `Set<Int>` comparison just works with `in`/`contains`.

Update `restoreParty()` — filtering by `unlockYogaLevel` is unchanged. The `PartyMemberData.heroId` will be `Int`, matching `Hero.id` (also `Int`).

### Step 10: Update PartyScreen.kt

**File:** `app/src/main/java/com/example/game/ui/components/PartyScreen.kt`

All hero ID comparisons use `==` between `Int` values. Remove any `.lowercase()` calls on hero IDs. Update `detailHeroId` state from `String?` to `Int?`:

```kotlin
var detailHeroId by remember { mutableStateOf<Int?>(null) }  // was String?
```

### Step 11: Update ShopScreen.kt

**File:** `app/src/main/java/com/example/game/ui/components/ShopScreen.kt`

The hero filter `selectedHeroFilter` changes from `String?` to `Int?`. The item filter compares `eq.heroId == selectedHeroFilter` — both are `Int?` so this works.

The `val partyHasHero` check changes to:

```kotlin
val partyHasHero = item.heroId == null || party.any { it.heroId == item.heroId }
```

Both are `Int?` so `==` works correctly.

### Step 12: Update BattleScene.kt / BattleCanvas.kt / CombatantSprite.kt

**File:** `app/src/main/java/com/example/game/ui/components/BattleScene.kt`

`CombatantState.id` is currently set to `Hero.id` (now `Int`). The battle system uses hero IDs internally. The `drawSilhouette()` function currently matches on a `name: String` parameter. This needs to change to accept a hero ID or name separately.

**Option A:** Pass both `heroId: Int` and `displayName: String` to `drawSilhouette()`. The `when` block matches on `displayName` (or `heroId`).

**Option B:** Change `drawSilhouette()` to match on `heroId: Int`:

```kotlin
internal fun DrawScope.drawSilhouette(cx: Float, cy: Float, s: Float, heroId: Int, tint: Color) {
    when (heroId) {
        1 -> { /* Shanti */ }
        2 -> { /* Santosha */ }
        3 -> { /* Virya */ }
        4 -> { /* Dhairya */ }
        5 -> { /* Maitri */ }
        else -> { /* generic fallback */ }
    }
}
```

Option B is cleaner since the `when` is now type-safe and doesn't depend on string matching.

Update `CombatantSprite.kt` to pass `hero.id` (now `Int`) instead of `hero.id` (was `String`).

Update `HeroPortrait` (from plan 07) to accept `heroId: Int` instead of `heroId: String`.

### Step 13: Update DataLoader.kt

**File:** `app/src/main/java/com/example/game/persistence/DataLoader.kt`

Change `getHero(id: String)` to `getHero(id: Int)`:

```kotlin
fun getHero(id: Int): Hero = heroes.first { it.id == id }
```

### Step 14: Update default_save.json

**File:** `app/src/main/assets/game/default_save.json`

Change `"unlockedHeroIds": []` — the array elements are now integers. Gson will handle this if the Kotlin type is `Set<Int>`.

## Bug Fixes Achieved

All 15+ comparison mismatches are resolved:
- `GameViewModel.kt:184` — `DataLoader.heroes.find { it.id == pm.heroId }` now works (Int == Int)
- `PartyScreen.kt:69` — `party.find { it.heroId == heroDef.id }` now works
- `PartyScreen.kt:94` — same fix
- `ShopScreen.kt:112` — `party.any { it.heroId == item.heroId }` now works
- All `GameViewModel` find/filter operations on hero IDs now work

## Migration Note

Since `GameSaveManager.normalized()` previously lowercased IDs, players with existing save data will have `"shanti"` strings in their saved party. After switching to `Int`, old saves will fail to load (Gson will try to parse `"shanti"` as an `Int`). 

**Mitigation:** Add a version check in `GameSaveManager.loadGame()` — if the loaded data has `String` hero IDs (version < 3), map them to integers using a lookup and bump the version to 3. Or simply reset save data for existing players (acceptable since the game is in development).

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/assets/game/heroes.json` | Change `id` values to integers |
| `app/src/main/assets/game/equipment.json` | Change `heroId` to integers |
| `app/src/main/assets/game/combos.json` | Change `requiredHeroes` to integer array |
| `app/src/main/java/com/example/game/model/Hero.kt` | `id: Int`, change `toCombatantState()` to match |
| `app/src/main/java/com/example/game/model/Equipment.kt` | `heroId: Int?`, update `getThemeColor()` |
| `app/src/main/java/com/example/game/model/GameProgress.kt` | `PartyMemberData.heroId: Int`, `unlockedHeroIds: Set<Int>` |
| `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt` | All heroId parameters and comparisons to `Int` |
| `app/src/main/java/com/example/game/ui/components/PartyScreen.kt` | `detailHeroId: Int?`, all heroId references to `Int` |
| `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` | `selectedHeroFilter: Int?`, heroId comparisons |
| `app/src/main/java/com/example/game/ui/components/BattleScene.kt` | Pass `hero.id` as Int to drawSilhouette |
| `app/src/main/java/com/example/game/ui/components/BattleCanvas.kt` | `drawSilhouette` parameter `heroId: Int`, `when` on Int |
| `app/src/main/java/com/example/game/ui/components/CombatantSprite.kt` | Pass numeric ID to drawSilhouette |
| `app/src/main/java/com/example/game/persistence/DataLoader.kt` | `getHero(id: Int)` |
| `app/src/main/java/com/example/game/persistence/GameSaveManager.kt` | Remove string normalizeId for heroes, update persistence types |
| `app/src/main/assets/game/default_save.json` | Hero IDs as integers |
