# Plan 15: Fix Shop Item Display & Hero Filters

## Problem 1: Hero Filter Chips Never Appear

In `ShopScreen.kt:106`, the hero filter chips are built from **unlocked heroes only**:

```kotlin
val unlockedHeroes = viewModel.getUnlockedHeroes()  // returns List<Hero>
// ...
items(listOf(null) + unlockedHeroes.map { it.id }) { heroId ->
```

`getUnlockedHeroes()` (GameViewModel.kt:147-149) does:
```kotlin
DataLoader.heroes.filter { it.id in unlockedHeroIds }
```

**Bug**: `it.id` is `Int`, `unlockedHeroIds` is `Set<String>`. `Int in Set<String>` is always false → returns **empty list** → no hero filter chips at all (only "All" shows).

**Fix 1a**: Show **all** heroes as filter options, not just unlocked ones. The purchase logic already disables Buy for locked/unowned heroes via the `partyHasHero` check (line 139).

```kotlin
// Replace lines 105-120
var selectedHeroFilter by remember { mutableStateOf<Int?>(null) }

LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(listOf(null) + DataLoader.heroes.map { it.id }) { heroId ->
        val hero = heroId?.let { DataLoader.heroes.find { h -> h.id == it } }
        val label = when {
            heroId == null -> "All"
            hero != null -> hero.name.split(" ").first()
            else -> "#$heroId"
        }
        FilterChip(
            selected = selectedHeroFilter == heroId,
            onClick = { selectedHeroFilter = heroId },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
```

**Fix 1b**: Also fix the item filter on line 131-133 so that when a locked hero is selected, their unique items are visible (not filtered out):

```kotlin
// Status quo — this already works correctly for locked heroes:
val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory && eq.id !in battleRewardItemIds &&
    (selectedHeroFilter == null ||
     eq.tier == EquipmentTier.GENERIC ||
     eq.heroId == selectedHeroFilter)
}
```

The filter `eq.heroId == selectedHeroFilter` uses `Int == Int?` which works. When a locked hero is selected, their items appear. The Buy button is correctly disabled by the `partyHasHero` check on line 139.

## Problem 2: Unique Items Not Showing (Diagnosis)

With Fix 1 in place, the hero filter chips will appear for all heroes. After that, check if unique items still fail to display:

### Step 1 — Verify data loading
Confirm all 20 items in `equipment.json` parse into `DataLoader.equipment`. Add a test log:

```kotlin
// In DataLoader.init() or a test
Log.d("DataLoader", "Loaded ${equipment.size} equipment items")
Log.d("DataLoader", "Items: ${equipment.map { "${it.id} (${it.tier})" }}")
```

Expected: 20 items, including 12 UNIQUE items.

### Step 2 — Verify battle-reward exclusion
Check the `battleRewardItemIds` set:
- `shanti_prayer_beads` (UNIQUE)
- `santosha_foundation_stone` (UNIQUE)
- `virya_ember_core` (UNIQUE)
- `dhairya_battle_standard` (UNIQUE)
- `maitri_universal_key` (UNIQUE)

These 5 are correctly excluded from the shop (they are first-defeat rewards).

Remaining 7 UNIQUE items that should always appear in shop:
- `shanti_tidal_staff`, `shanti_serene_mantle`
- `santosha_earthen_bulwark`, `santosha_contentment_beads`
- `virya_inferno_wrath`, `virya_blazing_mantle`
- `dhairya_light_vanguard`, `dhairya_courage_circlet`
- `maitri_wind_caress`, `maitri_heart_embrace`

### Step 3 — Fix if items are still missing

If unique items still don't display after Fix 1, the most likely remaining causes:

| Cause | Check |
|---|---|
| `DataLoader.equipment` silently fails to parse equipment.json | JSON field name mismatch with `Equipment` data class (e.g. `minYogaLevel` vs `yogaLevelRequired`) |
| Battle-reward exclusion set is too broad | Verify `battleRewardItemIds` only contains the 5 intended IDs |
| Slot tabs are confusing | User may be looking at WEAPON but item is in ARMOR — add an "All" slot tab |

### Fix 3a — Add "All" slot tab (optional)

To make it easier to browse, add an "All" option to the slot filter tabs:

```kotlin
var selectedCategory by remember { mutableStateOf<EquipmentSlot?>(null) }  // nullable

LazyRow {
    items(listOf(null) + EquipmentSlot.values()) { slot ->
        FilterChip(
            selected = selectedCategory == slot,
            onClick = { selectedCategory = slot },
            label = { Text(slot?.name ?: "All", ...) }
        )
    }
}
```

Then update the item filter:
```kotlin
(selectedCategory == null || eq.slot == selectedCategory) && ...
```

### Fix 3b — Fix JSON field mapping (if that's the bug)

The `Equipment` data class uses `minYogaLevel` but `equipment.json` also uses `minYogaLevel`. The computed property `yogaLevelRequired` is derived — not a JSON field. This should work correctly.

If items fail to parse, check:
- All required fields exist in each item
- No trailing commas (not valid in JSON)
- `effects` array syntax is correct

## Files to modify

| File | Changes |
|---|---|
| `ShopScreen.kt:105-120` | Replace `getUnlockedHeroes()` with `DataLoader.heroes` for filter chips |
| `ShopScreen.kt:84` | Optionally make `selectedCategory` nullable for an "All" slot tab |
| `DataLoader.kt` | Add debug logging for equipment loading |
