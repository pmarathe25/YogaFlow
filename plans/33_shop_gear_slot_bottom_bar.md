# Plan 33: Replace Shop Gear Filters With Slot Pages + Bottom Bar

## Problem

The shop currently uses two rows of `FilterChip` — one for equipment slot (Weapon/Armor/Accessory/All) and one for tier. The slot filter takes up horizontal space and mixes categories with "All". A bottom navigation bar provides clearer separation between the three gear types and leaves more room for content.

## Changes

### 1. Replace slot filter row with a bottom bar

**`ShopScreen.kt`** — remove the slot filter `LazyRow` (lines 87-105). Add a `NavigationBar` at the bottom of the screen with three items: WEAPON, ARMOR, ACCESSORY.

Remove:
```kotlin
// Delete lines 87-105
var selectedCategory by remember { mutableStateOf<EquipmentSlot?>(null) }
LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(listOf(null) + EquipmentSlot.values()) { slot ->
        FilterChip(...)
    }
}
```

Add at the bottom of the scaffold/column (outside the `LazyColumn`):

```kotlin
@Composable
private fun SlotNavigationBar(
    selectedSlot: EquipmentSlot,
    onSlotSelected: (EquipmentSlot) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        EquipmentSlot.WEAPON.let { slot ->
            NavigationBarItem(
                icon = { Icon(Icons.Default.Shield, contentDescription = null) },
                label = { Text("Weapon") },
                selected = selectedSlot == slot,
                onClick = { onSlotSelected(slot) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFF44336),   // red
                    selectedTextColor = Color(0xFFF44336),
                    indicatorColor = Color(0xFFF44336).copy(alpha = 0.12f)
                )
            )
        }
        EquipmentSlot.ARMOR.let { slot ->
            NavigationBarItem(
                icon = { Icon(Icons.Default.Checkroom, contentDescription = null) },
                label = { Text("Armor") },
                selected = selectedSlot == slot,
                onClick = { onSlotSelected(slot) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF4CAF50),   // green
                    selectedTextColor = Color(0xFF4CAF50),
                    indicatorColor = Color(0xFF4CAF50).copy(alpha = 0.12f)
                )
            )
        }
        EquipmentSlot.ACCESSORY.let { slot ->
            NavigationBarItem(
                icon = { Icon(Icons.Default.Watch, contentDescription = null) },
                label = { Text("Accessory") },
                selected = selectedSlot == slot,
                onClick = { onSlotSelected(slot) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2196F3),   // blue
                    selectedTextColor = Color(0xFF2196F3),
                    indicatorColor = Color(0xFF2196F3).copy(alpha = 0.12f)
                )
            )
        }
    }
}
```

Remove the `"All"` option — the bottom bar always has exactly one slot selected. No `null` option.

### 2. Make `selectedCategory` non-nullable

```diff
- var selectedCategory by remember { mutableStateOf<EquipmentSlot?>(null) }
+ var selectedCategory by remember { mutableStateOf(EquipmentSlot.WEAPON) }
```

### 3. Update the item filter

Remove the old slot filter condition (which allowed `null` = all). The filter now always filters by the selected slot:

```kotlin
val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory && eq.id !in battleRewardItemIds &&
    (selectedTierFilter == null || eq.tier == selectedTierFilter)
}
```

### 4. Layout adjustment

Wrap the existing content in a `Scaffold` with the `SlotNavigationBar` as its bottom bar:

```kotlin
Scaffold(
    bottomBar = {
        SlotNavigationBar(
            selectedSlot = selectedCategory,
            onSlotSelected = { selectedCategory = it }
        )
    }
) { padding ->
    Column(modifier = Modifier.padding(padding)) {
        // Tier filter LazyRow (unchanged)
        TierFilterRow(...)

        // Item list LazyColumn (unchanged)
        LazyColumn(...) { ... }

        // Spark/gold costs footer (unchanged)
    }
}
```

If the shop already uses a `Scaffold`, just add the `bottomBar` parameter to it.

### 5. Icon availability note

The icons `Shield`, `Checkroom`, and `Watch` are available in `material-icons-extended`. If any are missing, fall back to:
- WEAPON: `Icons.Default.Shield` or emoji `"⚔️"`
- ARMOR: `Icons.Default.Checkroom` or emoji `"🛡️"`
- ACCESSORY: `Icons.Default.Watch` or emoji `"💍"`

## Files to modify

| File | Changes |
|---|---|
| `ShopScreen.kt` | Remove slot filter `LazyRow`; add `SlotNavigationBar` composable; wrap content in `Scaffold` with bottom bar; change `selectedCategory` to non-nullable with default `WEAPON` |

## Dependencies

- None. Standalone shop layout change.
