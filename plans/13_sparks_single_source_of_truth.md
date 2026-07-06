# Plan 13: Sparks Single Source of Truth, Gold Economy Fix & Shanti Default-Unlock Bug

## Problem 1: Dual Spark Display

Two different spark values are shown to the user depending on the screen:

| Screen | Source | Semantics |
|---|---|---|
| `SessionCompleteScreen` (line 356) | `statsManager.totalSparks` | **Total unique practice days ever** — never decremented |
| `JourneyScreen` (line 281) | `gameSaveData.sparks` | **Spendable balance** — decremented by hero level-ups |
| `ShopScreen` (via VM) | `gameSaveData.sparks` | Spendable balance (correct) |

A user who levels up a hero sees "10 Sparks" on the session end screen, then "7 Sparks" on the Journey screen. This looks like a bug even though it's a semantic difference.

**Root cause**: The game's sync (`GameViewModel.syncWithMainApp()`, lines 100–103) correctly adds only *new* sparks (`delta = mainSparks - lastSyncedMainSparks`), so `GameProgress.sparks` is an accurate spendable balance. But `SessionCompleteScreen` bypasses the game save entirely and reads the raw main-app counter.

## Problem 2: Gold Infinite Refill

`syncWithMainApp()` (lines 108–112) sets gold to an upward-only ceiling:

```kotlin
val previousGold = data.gold
val expectedGold = xpSum / 10
if (expectedGold > previousGold) {
    updated = updated.copy(gold = expectedGold)
}
```

**Effect**: When the player spends gold in the shop (`purchaseItem` decrements `data.gold`), `previousGold` drops below `expectedGold` on the next sync, so gold is refilled back to `xpSum / 10`. **Spending gold has no permanent effect** — it's essentially infinite.

Additionally:
- `SessionCompleteScreen` (line 362) computes `gold = totalXp / 10` independently, not from the game save.
- `JourneyScreen` (line 289) reads `gameSaveData.gold`. These can disagree.

## Problem 3: Shanti Not Unlocked By Default

`default_save.json` has:
```json
{"unlockedHeroIds": [1], "party": [{"heroId": 1}]}
```

`GameProgress.unlockedHeroIds` is currently `Set<String>`. Gson deserializes `1` → `"1"`, so the set contains `"1"`.

But `purchaseHero(heroId: Int)` (line 480) checks `heroId in data.unlockedHeroIds` — an `Int in Set<String>` check that **always returns false** in Kotlin. So Shanti is never recognised as already unlocked, and the player can purchase her again (or sees her as locked).

Similarly, `PartyMemberData.heroId` is still `String`, so the JSON `{"heroId": 1}` is deserialized to `heroId = "1"` — a string that doesn't match `Hero.id` (`Int 1`) when the combat system does lookups.

**Root cause**: Stalled migration to `Int` IDs (Plan 11). `GameViewModel.purchaseHero()` was changed to accept `Int`, but `GameProgress` and `PartyMemberData` fields remain `String`.

---

## Solution

### 3A — Sparks: Single Display Source

1. **`GameProgress.sparks` becomes the exclusive display source for sparks everywhere.** Remove all direct reads of `StatsManager.totalSparks` from UI code.

2. **`SessionCompleteScreen`**: Replace the `statsManager.totalSparks` read with `gameViewModel.saveData.collectAsState().value.sparks`. Add a `LaunchedEffect(Unit)` that calls `gameViewModel.refreshSync()` to ensure the game save is up to date before displaying.

3. **Add a session delta indicator** next to the spark count on `SessionCompleteScreen`:
   - After sync, compute `newSparkCount = saveData.value.sparks`
   - Store `previousSparkCount` before sync (or track `lastSyncedMainSparks` diff)
   - Display `"+1 new"` if this session contributed a new unique day

4. **Remove `lastSyncedMainSparks` from `GameProgress`** (and the associated serialization) since it's only used internally by `syncWithMainApp()`. Store it as a plain `var` on `GameViewModel` instead.

### 3B — Gold: Fix Infinite Refill

Replace the upward-only ceiling with an **additive model**:

```kotlin
// Old (buggy)
val previousGold = data.gold
val expectedGold = xpSum / 10
if (expectedGold > previousGold) {
    updated = updated.copy(gold = expectedGold)
}

// New (additive)
if (xpSum > data.totalYogaXp) {
    val newGoldEarned = (xpSum - data.totalYogaXp) / 10
    if (newGoldEarned > 0) {
        updated = updated.copy(gold = updated.gold + newGoldEarned)
    }
}
```

This ensures:
- Gold earned from XP cumulates over time
- Spending gold in the shop permanently reduces the balance
- No auto-refill

Also:
- `SessionCompleteScreen`: Replace `gold = totalXp / 10` (line 362) with `gameViewModel.saveData.collectAsState().value.gold` (same source as JourneyScreen).
- Keep `gold` in `GameProgress` — it's a genuine balance now.

### 3C — Shanti: Complete Int ID Migration

**`GameProgress.kt`**:
- Change `unlockedHeroIds: Set<String>` → `Set<Int>`
- Change `PartyMemberData.heroId: String` → `Int`

**`GameSaveManager.kt`**:
- Remove `normalizeId()` for hero IDs (heroes use Int — no case/normalization needed)
- `normalizeId()` can be kept for item/monster IDs (still strings)
- Update `normalized()` to skip string normalization for `heroId` and `unlockedHeroIds`
- Update `loadLegacySave()` to convert legacy string IDs to Int

**`GameViewModel.kt`** — already uses `Int` for `purchaseHero()` but fix remaining string references:
- Line 395: `fun equipItem(heroId: String, ...)` → `heroId: Int`
- Line 418: `fun unequipItem(heroId: String, ...)` → `heroId: Int`
- Line 435: `fun getEquippedItems(heroId: String, ...)` → `heroId: Int`
- Line 444: `fun getHeroLevelUpCost(heroId: String)` → `heroId: Int`
- Line 449: `fun levelUpHero(heroId: String)` → `heroId: Int`
- Line 147–154: `getAvailableHeroes()` — fix `h.id !in unlockedHeroIds` check (now both Int)

**`default_save.json`** — already uses `1` (Int), no change needed.

**Other files using `heroId: String`** — update signatures and all call sites:
- `BattleCanvas.kt`, `CombatantSprite.kt` (match `heroId: Int` for `drawSilhouette`)
- `PartyScreen.kt`, `ShopScreen.kt` (all hero ID references)
- `ActionTray.kt`, `BattleScene.kt`
- `equipment.json`, `combos.json` (hero references like `"shanti"` → `1` if any exist)

**Remove `normalizeId()` for heroes entirely** after the migration — case-insensitive matching is no longer needed for hero IDs.

---

## Files to Modify

| File | Changes |
|---|---|
| `GameProgress.kt` | `unlockedHeroIds: Set<Int>`, `PartyMemberData.heroId: Int`, remove `lastSyncedMainSparks` |
| `GameViewModel.kt` | Additive gold model; all `heroId: String` params → `Int`; `getAvailableHeroes()` Int check; store `lastSyncedMainSparks` as local var |
| `GameSaveManager.kt` | Remove hero normalization from `normalized()`; update `loadLegacySave()` for Int IDs |
| `SessionCompleteScreen` | Read `gameVM.saveData.sparks` and `gameVM.saveData.gold`; add `LaunchedEffect` to `refreshSync()`; show spark delta |
| `JourneyScreen.kt` | Already reads from `gameSaveData` — verify no changes needed (should just work) |
| `HubScreen.kt` | Already calls `refreshSync()` — no change needed |
| `ShopScreen.kt` | Verify `purchaseItem()` works with Int hero filters (from Plan 05) |
| `PartyScreen.kt` | All `heroId: String` → `Int` |
| `BattleCanvas.kt` | `drawSilhouette` match on `heroId: Int` |
| `CombatantSprite.kt` | Pass `hero.id` as Int |
| `ActionTray.kt`, `BattleScene.kt` | All `heroId` references |
| `DataLoader.kt` | Hero lookup should key on `Int` |

## Dependencies

- **After** Plan 11 (numerical hero IDs) — Int ID migration is a prerequisite for 3C.
- The additive gold model (3B) and spark display fix (3A) are independent of Int IDs and can be done immediately.
