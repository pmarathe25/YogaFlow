# Implementation Plan: Unique Items Rework & Shop Overhaul

## Overview
Unique (tier=UNIQUE) items become exclusive battle rewards from boss monster defeats. They are removed entirely from the shop. Set bonuses are updated to reference `Hero.uniqueItemIds` so they automatically cover all unique items for a given hero.

---

## Dependency Order (execute in this sequence)

```
Step 1 (JSON assets) → Step 2 (EconomyManager) → Step 3 (ShopScreen) → Step 4 (verify)
                                                          ↓
                                                   Step 5 (SetBonus — optional/future)
```

Steps 1–3 are non-overlapping and can be done in any order, but the JSON changes should be done first since they define the source of truth that other steps reference.

---

## Step 1: Remap `firstDefeatItemReward` in `monsters.json`

**File**: `app/src/main/assets/game/heroes.json`
- Contains `uniqueItemIds` for each hero. Read these to know the canonical set of unique item IDs (already populated).

**File**: `app/src/main/assets/game/monsters.json`

### What to change
The 15 unique items from `heroes.json` must be distributed across monsters in hero-unlock order (Shanti → Santosha → Virya → Dhairya → Maitri). Assign them to monsters in the order monsters appear in `monsters.json`, skipping at most 1 monster to keep mapping clean. Boss monsters (isBoss=true) MUST each carry ONE `firstDefeatItemReward`.

### Current `firstDefeatItemReward` assignments (5 monsters):

| Monster | Current Item |
|---------|-------------|
| Bhaya (EASY) | `shanti_prayer_beads` |
| Chinta (EASY) | `santosha_foundation_stone` |
| Matsarya (MEDIUM) | `virya_ember_core` |
| Dvesha (MEDIUM) | `dhairya_battle_standard` |
| Lobha (HARD) | `maitri_universal_key` |

### Target mapping (assign all 15 unique items):

All unique item IDs in hero-unlock order:
```
shanti_prayer_beads, shanti_tidal_staff, shanti_serene_mantle,
santosha_foundation_stone, santosha_earthen_bulwark, santosha_contentment_beads,
virya_ember_core, virya_inferno_wrath, virya_blazing_mantle,
dhairya_battle_standard, dhairya_light_vanguard, dhairya_courage_circlet,
maitri_universal_key, maitri_wind_caress, maitri_heart_embrace
```

Assign to monsters in file order. The 4 bosses (isBoss=true) get the last 4 items of the sequence so that boss wins feel rewarding (they are fought later):

| # | Monster | Difficulty | isBoss | New `firstDefeatItemReward` |
|---|---------|-----------|--------|---------------------------|
| 1 | Bhaya | EASY | false | `shanti_prayer_beads` |
| 2 | Tandra | EASY | false | `shanti_tidal_staff` |
| 3 | Chinta | EASY | false | `shanti_serene_mantle` |
| 4 | Alasya | EASY | false | `santosha_foundation_stone` |
| 5 | Matsarya | MEDIUM | false | `santosha_earthen_bulwark` |
| 6 | Krodha | MEDIUM | false | `santosha_contentment_beads` |
| 7 | Dvesha | MEDIUM | false | `virya_ember_core` |
| 8 | Moha | HARD | false | `virya_inferno_wrath` |
| 9 | Lobha | HARD | false | `virya_blazing_mantle` |
| 10 | Abhimana | HARD | false | `dhairya_battle_standard` |
| 11 | Mada | HARD | false | `dhairya_light_vanguard` |
| 12 | Irsya | HARD | false | `dhairya_courage_circlet` |
| 13 | Ahankara | BOSS | **true** | `maitri_universal_key` |
| 14 | Maya | BOSS | **true** | `maitri_wind_caress` |
| 15 | Klesh | BOSS | **true** | `maitri_heart_embrace` |
| 16 | Samsara | SUPERBOSS | **true** | *(leave null — superboss reward is prestige)* |

### Exact changes to `monsters.json`

1. **Bhaya** (approx. line 34-35): Set `"firstDefeatItemReward": "shanti_prayer_beads"` (unchanged)

2. **Tandra** (approx. line 68-69): Currently has no `firstDefeatItemReward`. Add:
   ```json
   "firstDefeatItemReward": "shanti_tidal_staff"
   ```
   Insert after `"isBoss": false` but before the closing `}` of the object.

3. **Chinta** (approx. line 103-104): Change from `"santosha_foundation_stone"` to:
   ```json
   "firstDefeatItemReward": "shanti_serene_mantle"
   ```

4. **Alasya** (approx. line 138-139): Currently has no `firstDefeatItemReward`. Add:
   ```json
   "firstDefeatItemReward": "santosha_foundation_stone"
   ```

5. **Matsarya** (approx. line 172-173): Change from `"virya_ember_core"` to:
   ```json
   "firstDefeatItemReward": "santosha_earthen_bulwark"
   ```

6. **Krodha** (approx. line 210-211): Currently has no `firstDefeatItemReward`. Add:
   ```json
   "firstDefeatItemReward": "santosha_contentment_beads"
   ```

7. **Dvesha** (approx. line 244-245): Change from `"dhairya_battle_standard"` to:
   ```json
   "firstDefeatItemReward": "virya_ember_core"
   ```

8. **Moha** (approx. line 278-279): Currently has no `firstDefeatItemReward`. Add:
   ```json
   "firstDefeatItemReward": "virya_inferno_wrath"
   ```

9. **Lobha** (approx. line 313-314): Change from `"maitri_universal_key"` to:
   ```json
   "firstDefeatItemReward": "virya_blazing_mantle"
   ```

10. **Abhimana** (approx. line 348-349): Currently has no `firstDefeatItemReward`. Add:
    ```json
    "firstDefeatItemReward": "dhairya_battle_standard"
    ```

11. **Mada** (approx. line 382-383): Currently has no `firstDefeatItemReward`. Add:
    ```json
    "firstDefeatItemReward": "dhairya_light_vanguard"
    ```

12. **Irsya** (approx. line 416-417): Currently has no `firstDefeatItemReward`. Add:
    ```json
    "firstDefeatItemReward": "dhairya_courage_circlet"
    ```

13. **Ahankara** (approx. line 453-454): Currently has no `firstDefeatItemReward`. Add:
    ```json
    "firstDefeatItemReward": "maitri_universal_key"
    ```

14. **Maya** (approx. line 490-491): Currently has no `firstDefeatItemReward`. Add:
    ```json
    "firstDefeatItemReward": "maitri_wind_caress"
    ```

15. **Klesh** (approx. line 528-529): Currently has no `firstDefeatItemReward`. Add:
    ```json
    "firstDefeatItemReward": "maitri_heart_embrace"
    ```

16. **Samsara** (approx. line 568-569): Leave without `firstDefeatItemReward`. Superboss is prestige content; no unique item needed.

### Notes for JSON editing
- Each `firstDefeatItemReward` line must be placed inside the monster object, after `"isBoss"` and before the closing `}`. Follow the existing pattern (e.g., Bhaya at line 34-35).
- Ensure proper trailing commas: if `firstDefeatItemReward` is the last field in an object, it needs a comma if another field follows but NOT if it's the last field. Follow the existing JSON conventions in the file.
- For monsters that already have a `firstDefeatItemReward`, just change the value string.
- For monsters adding it for the first time, insert the line before the closing `}`.

---

## Step 2: Block unique item purchases in `EconomyManager.kt`

**File**: `app/src/main/java/com/example/game/viewmodel/EconomyManager.kt`
**Lines**: 16–29

### What to change
Add a tier check so unique items cannot be purchased with gold. They can only enter inventory via boss defeat rewards (`BattleOrchestrator.onBattleWon()` at `BattleOrchestrator.kt:249-252`).

### Exact code change

In `purchaseItem()`, add a third early-return check after the two existing checks (lines 18-20). Insert after line 20 (`if (itemId in data.inventory) return false`):

```kotlin
if (item.tier == EquipmentTier.UNIQUE) return false
```

The full `purchaseItem()` method becomes (replace lines 16-29):

```kotlin
fun purchaseItem(itemId: String): Boolean {
    val item = DataLoader.getEquipment(itemId)
    val data = _saveData.value
    if (data.yogaLevel < item.yogaLevelRequired) return false
    if (data.gold < item.goldCost) return false
    if (itemId in data.inventory) return false
    if (item.tier == EquipmentTier.UNIQUE) return false

    _saveData.value = data.copy(
        gold = data.gold - item.goldCost,
        inventory = data.inventory + itemId
    )
    saveManager.saveGame(_saveData.value)
    return true
}
```

### Why this is safe
`BattleOrchestrator.onBattleWon()` at line 249-252 directly modifies `_saveData.value` without calling `purchaseItem()`, so unique items from boss rewards are unaffected.

---

## Step 3: Remove unique items from `ShopScreen.kt`

**File**: `app/src/main/java/com/example/game/ui/components/ShopScreen.kt`

### 3a. Remove UNIQUE from tier filter chips (lines 97-109)

Change line 98:
```kotlin
// Before:
items(listOf(null) + EquipmentTier.values().toList()) { tier ->
// After:
items(listOf(null, EquipmentTier.COMMON, EquipmentTier.UNCOMMON, EquipmentTier.RARE)) { tier ->
```

This removes `UNIQUE` from the tier filter row so the player cannot filter by it. The `null` entry renders as "All".

### 3b. Exclude unique items from the filtered list (lines 113-116)

Change the filter to also exclude UNIQUE tier items:

```kotlin
// Before:
val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory &&
    (selectedTierFilter == null || eq.tier == selectedTierFilter)
}
// After:
val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory &&
    eq.tier != EquipmentTier.UNIQUE &&
    (selectedTierFilter == null || eq.tier == selectedTierFilter)
}
```

### 3c. Remove UNIQUE from tier section header group (lines 136-169)

In the `else` branch (the grouped-by-tier view), change line 139-144 from:

```kotlin
val tierOrder = listOf(
    EquipmentTier.COMMON,
    EquipmentTier.UNCOMMON,
    EquipmentTier.RARE,
    EquipmentTier.UNIQUE
)
```

To:

```kotlin
val tierOrder = listOf(
    EquipmentTier.COMMON,
    EquipmentTier.UNCOMMON,
    EquipmentTier.RARE
)
```

Since step 3b already filters out UNIQUE items, the UNIQUE header would never render anyway. But removing it from the list is cleaner and prevents empty-section edge cases.

### 3d. Remove the set bonus section from GearDetailsDialog (lines 409-446) — OPTIONAL

The dialog currently shows set bonus info for unique items. Since unique items can no longer be seen in the shop, this code path is unreachable from the shop. However, if `GearDetailsDialog` is used elsewhere (e.g., inventory view), keep it. For now, leave it as-is — it does no harm and may be useful if a future inventory screen calls `GearDetailsDialog`.

Search the codebase for other usages of `GearDetailsDialog`:
```bash
rg "GearDetailsDialog" app/src/
```
If only called from `ShopScreen`, the set bonus panel lines 409-446 is dead code but harmless to leave.

---

## Step 4: Verify the flow

After completing steps 1–3, verify:

1. **Build**: `./gradlew assembleDebug` — must succeed.
2. **Shop screen**: Open app, navigate to Shop. Confirm no unique items appear in any slot (Weapon/Armor/Accessory) or any tier filter. Confirm "Unique" filter chip is gone.
3. **Boss rewards**: Defeat a boss monster (e.g., Ahankara). Confirm the correct `firstDefeatItemReward` item appears in the inventory (check save data). Confirm it only drops on first defeat (defeat same boss twice, gold only second time).
4. **Purchase block**: Try to directly call `purchaseItem()` with a unique item ID (via test or debug). Confirm it returns `false`.
5. **Existing save compatibility**: If a player already owns unique items from before this change, `saveData.inventory` will still contain those IDs. This is fine — `purchaseItem` checks `itemId in data.inventory` before the new UNIQUE check, so already-owned items don't cause false negatives. The `ShopItemCard` correctly shows "Owned" for any item already in inventory, regardless of tier.

---

## Step 5: Set Bonus Rework (future / separate task)

### Current state
- `SetBonus.requiredItems` in `equipment.json` hardcodes specific item IDs (e.g., `["virya_ember_core", "virya_inferno_wrath"]`).
- `Hero.setBonusId` is a `String?` field populated in `heroes.json` (e.g., `"Shanti"`, `"Santosha"`) but is **never read** in any Kotlin logic.
- `Hero.uniqueItemIds` is populated for all heroes but is **never read** in any Kotlin logic.
- Set bonuses are **not applied in combat**. The `Hero.toCombatantState()` extension (`Hero.kt:30-63`) only reads equipped equipment effects — it does not look up `SetBonus` or `uniqueItemIds`.

### What should change (plan for a follow-up PR)
1. Update `SetBonus.requiredItems` in `equipment.json` to reference `Hero.uniqueItemIds` instead of hardcoding. In practice this means: each `setBonus` object should have its `requiredItems` list contain ALL items from the hero's `uniqueItemIds`. Currently some set bonuses only list 2 of 3 items (e.g., Shanti's "Calming Current" lists `["shanti_prayer_beads", "shanti_tidal_staff"]` but misses `shanti_serene_mantle`). A set bonus should require ALL unique items for that hero to be equipped.

2. **New JSON structure** for `setBonuses` — change `requiredItems` for each entry to match the full `uniqueItemIds` from the hero:
   ```json
   // Shanti (heroId=1): all 3 items
   {"name": "Calming Current", "heroId": 1, "description": "...", "requiredItems": ["shanti_prayer_beads", "shanti_tidal_staff", "shanti_serene_mantle"], "effects": [...]},
   // Santosha (heroId=2): all 3 items
   {"name": "Eternal Foundation", "heroId": 2, "description": "...", "requiredItems": ["santosha_foundation_stone", "santosha_earthen_bulwark", "santosha_contentment_beads"], "effects": [...]},
   // Virya (heroId=3): all 3 items
   {"name": "Raging Inferno", "heroId": 3, "description": "...", "requiredItems": ["virya_ember_core", "virya_inferno_wrath", "virya_blazing_mantle"], "effects": [...]},
   // Dhairya (heroId=4): all 3 items
   {"name": "Inspiring Presence", "heroId": 4, "description": "...", "requiredItems": ["dhairya_battle_standard", "dhairya_light_vanguard", "dhairya_courage_circlet"], "effects": [...]},
   // Maitri (heroId=5): all 3 items
   {"name": "Universal Love", "heroId": 5, "description": "...", "requiredItems": ["maitri_universal_key", "maitri_wind_caress", "maitri_heart_embrace"], "effects": [...]}
   ```

3. **Apply set bonuses in combat**: In `Hero.toCombatantState()` (`Hero.kt:30-63`), after computing stats from equipment effects, also check if the hero has all `uniqueItemIds` equipped. If so, look up the set bonus and apply its effects to the combatant state. Implementation sketch:
   - Accept a `List<SetBonus>` parameter (or read from `DataLoader.setBonuses`).
   - For the current hero, check if `DataLoader.setBonuses.find { it.heroId == this.id }?.requiredItems?.all { it in equippedItems } == true`.
   - If yes, apply the bonus effects (same way equipment effects are applied — sum up ATK_PERCENT, HP_PERCENT, SPD_PERCENT, ALL_STATS_PERCENT, START_SHIELD_PERCENT).

### Note on `setBonusId`
The `setBonusId` field on `Hero` is currently unused. After the rework, `setBonusId` could be removed or repurposed. For now leave it untouched — it causes no bugs.

---

## Step 6: Optional cleanup — `sparkCost` on unique items

**File**: `app/src/main/assets/game/equipment.json`

Unique items still have `sparkCost` fields (15, 10, 18, 20, 25) which are only used for `goldCost` calculation. Since unique items can no longer be purchased, these fields are dead data. Consider setting them to 0 or removing them, but this is purely cosmetic. If you change `sparkCost`, verify no code reads it from unique items for any purpose other than purchase cost.

---

## Files Modified Summary

| File | Action |
|------|--------|
| `app/src/main/assets/game/monsters.json` | Add/change `firstDefeatItemReward` on 16 monsters |
| `app/src/main/java/com/example/game/viewmodel/EconomyManager.kt` | Add UNIQUE tier check in `purchaseItem()` (line ~21) |
| `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` | Filter out UNIQUE items (line ~114), remove UNIQUE chip (line ~98), remove UNIQUE tier header (line ~143) |
| `app/src/main/assets/game/equipment.json` | *(Optional)* Update `setBonuses.requiredItems` to 3 items each; *(Optional)* zero out `sparkCost` on unique items |

### Files NOT modified (no changes needed)

| File | Reason |
|------|--------|
| `DataLoader.kt` | `equipment` list and `setBonuses` are loaded as-is; no logic changes needed |
| `Equipment.kt` | No structural changes to data classes |
| `Hero.kt` | `uniqueItemIds` field already exists, no changes needed |
| `GameProgress.kt` | `inventory` field unchanged |
| `BattleOrchestrator.kt` | Already handles `firstDefeatItemReward` correctly (lines 249-252) |
| `GameViewModel.kt` | Delegates to `EconomyManager`, no changes needed |
| `heroes.json` | `uniqueItemIds` already populated, no changes needed |
| `default_save.json` | Existing saves are compatible; no migration needed |
