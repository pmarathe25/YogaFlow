# Plan: Set Bonus Display in GearDetailsDialog

## Change

**File:** `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` — `GearDetailsDialog` composable (lines 229-283)

For UNIQUE items, display the item's set information below the stats section: which other items are in the set, and the set bonus description.

## Implementation

After the stats `Surface` (after line 274), add:

```kotlin
// Set bonus section for unique items
if (item.tier == EquipmentTier.UNIQUE && item.heroId != null) {
    val heroDef = DataLoader.heroes.find { it.id == item.heroId }
    val setBonus = heroDef?.setBonusId?.let { sid ->
        DataLoader.setBonuses.find { it.name == sid }
    }
    
    if (setBonus != null) {
        Spacer(Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Set: ${setBonus.name}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(4.dp))
                val otherItems = setBonus.requiredItems.filter { it != item.id }
                Text(
                    "Other items: ${otherItems.joinToString(", ") { id ->
                        DataLoader.getEquipment(id)?.name ?: id
                    }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bonus: ${setBonus.description}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
```

## Lookup logic

1. Get the hero definition from `DataLoader.heroes` using `item.heroId`
2. Get the hero's `setBonusId` (e.g., `"Virya"`, `"Maitri"`, `"Shanti"`)
3. Find the matching set bonus in `DataLoader.setBonuses` by name
4. From the set bonus, get `requiredItems` to list other items in the set, and `description` for the bonus text

## Edge cases

- Items with no set bonus (no hero found, or hero has no `setBonusId`): nothing extra is shown
- Item is the only owned piece of the set: still shows the other required items as "locked"
- All pieces owned: set bonus description informs the player of the active effect

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` | Add set bonus section to `GearDetailsDialog` |
