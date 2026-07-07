# Plan 50: Show All Unique Items in the Shop

## Problem

Some unique (hero-specific) items are hidden from the shop because they are marked as `firstDefeatItemReward` on monsters. The shop filters them out:

```kotlin
val battleRewardItemIds = DataLoader.monsters
    .mapNotNull { it.firstDefeatItemReward }
    .toSet()

val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory && eq.id !in battleRewardItemIds && ...
}
```

5 items are currently hidden:
- `shanti_prayer_beads` (Shanti accessory) — reward from Bhaya
- `santosha_foundation_stone` (Santosha armor) — reward from Chinta
- `virya_ember_core` (Virya accessory) — reward from Matsarya
- `dhairya_battle_standard` (Dhairya weapon) — reward from Dvesha
- `maitri_universal_key` (Maitri accessory) — reward from Lobha

All unique items should be visible in the shop, regardless of whether they're also obtainable as monster defeat rewards. Players should be able to purchase them with gold if they don't want to wait for the monster drop.

## Fix

### 1. Remove the `battleRewardItemIds` filter

**`ShopScreen.kt:126-133`** — remove the `battleRewardItemIds` exclusion:

Before:
```kotlin
val battleRewardItemIds = DataLoader.monsters
    .mapNotNull { it.firstDefeatItemReward }
    .toSet()

val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory && eq.id !in battleRewardItemIds &&
    (selectedTierFilter == null || eq.tier == selectedTierFilter)
}
```

After:
```kotlin
val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory &&
    (selectedTierFilter == null || eq.tier == selectedTierFilter)
}
```

### 2. Prevent duplicate purchases

The `purchaseItem()` function in `GameViewModel.kt:389-402` already checks `if (itemId in data.inventory) return false`, so players cannot buy an item they already own (whether acquired from shop or as a monster drop). No additional dedup logic needed.

### 3. Keep "Owned" badge

Items already in the player's inventory (from monster drops or previous purchases) will show the "Owned" badge (ShopScreen.kt line 338-350) — no change needed.

## Files to modify

| File | Changes |
|---|---|
| `ShopScreen.kt` | Remove `battleRewardItemIds` computation and the `eq.id !in battleRewardItemIds` filter condition |

## Dependencies

- None. Standalone fix.
