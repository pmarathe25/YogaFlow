# Plan: Hero-Specific Filtering in Shop

## Change

**File:** `app/src/main/java/com/example/game/ui/components/ShopScreen.kt`

Add a second row of filter chips below the existing slot (WEAPON/ARMOR/ACCESSORY) row. When a hero is selected, the shop shows all generic items plus that hero's unique items.

## 5a. Add hero filter state and UI

After the slot filter row (after line 100), add:

```kotlin
Spacer(Modifier.height(8.dp))

// Hero filter
var selectedHeroFilter by remember { mutableStateOf<String?>(null) } // null = "All"
val unlockedHeroes = viewModel.getUnlockedHeroes()

LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(listOf(null) + unlockedHeroes.map { it.id }) { heroId ->
        val label = heroId ?: "All"
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

## 5b. Update item filter logic

Replace the current simple slot filter (line 105-107) with one that also considers hero selection:

```kotlin
val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory && eq.id !in battleRewardItemIds &&
    (selectedHeroFilter == null ||
     eq.tier == EquipmentTier.GENERIC ||
     eq.heroId == selectedHeroFilter)
}
```

When `selectedHeroFilter == null` ("All"): show all items for the slot (minus battle rewards).
When a specific hero is selected: show GENERIC items (usable by anyone) + UNIQUE items for that hero.

## Behavior

- User can filter by both slot AND hero simultaneously
- "All" hero filter = original unfiltered view (shows all non-reward items)
- Selecting a specific hero filters down to generics + their uniques
- Only heroes the player has unlocked appear in the hero filter chips

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` | Add hero filter state, LazyRow, and updated filter predicate |
