# Plan 23: Equip Gear from Hero Details Dialog

## Problem

In `HeroDetailsDialog`, the three gear slot cards (`EquipmentSlotCard`) pass `onEquip = {}` (empty lambda) — clicking them does nothing. Only the unequip action works (via the `LinkOff` icon button inside each card).

The "Available Inventory" section below the gear layout provides an `EQUIP` button for each item, but this is crowded and doesn't show stat comparisons.

## Changes

### 1. Make gear slots clickable → show equip dialog

Replace the empty `onEquip = {}` with a dialog trigger:

```kotlin
var equipSlot by remember { mutableStateOf<EquipmentSlot?>(null) }

// In the gear row (lines 344-365):
EquipmentSlotCard(
    slot = EquipmentSlot.WEAPON,
    item = weaponItem,
    heroColor = heroColor,
    onUnequip = { viewModel.unequipItem(hero.id, it) },
    onEquip = { equipSlot = EquipmentSlot.WEAPON }   // was empty lambda
)
// ... same for ARMOR, ACCESSORY
```

### 2. Remove "Available Inventory" section

Delete lines 370-397 entirely — the inventory list below the gear layout:

```kotlin
// DELETE ALL:
// Spacer(Modifier.height(16.dp))
// Text("Available Inventory", ...)
// val available = ...
// LazyColumn(...) { ... }
```

### 3. Add equip item dialog

When `equipSlot != null`, show a dialog listing all equippable items for that slot:

```kotlin
equipSlot?.let { slot ->
    val availableItems = DataLoader.equipment.filter { eq ->
        eq.id in saveData.inventory && eq.slot == slot &&
        (eq.heroId == null || eq.heroId == hero.id)
    }

    EquipItemDialog(
        slot = slot,
        items = availableItems,
        currentlyEquipped = equipped.find { it.slot == slot },
        heroColor = heroColor,
        onEquip = { itemId ->
            viewModel.equipItem(hero.id, itemId)
            equipSlot = null
        },
        onDismiss = { equipSlot = null }
    )
}
```

### 4. Create EquipItemDialog composable

```kotlin
@Composable
private fun EquipItemDialog(
    slot: EquipmentSlot,
    items: List<Equipment>,
    currentlyEquipped: Equipment?,
    heroColor: Color,
    onEquip: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Select ${slot.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (currentlyEquipped != null) "Currently: ${currentlyEquipped.name}" else "No item equipped",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (items.isEmpty()) {
                    Text(
                        "No items available for this slot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    items.forEach { item ->
                        EquipItemRow(
                            item = item,
                            currentlyEquipped = currentlyEquipped,
                            heroColor = heroColor,
                            onEquip = { onEquip(item.id) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}
```

### 5. Create EquipItemRow with stat comparison

```kotlin
@Composable
private fun EquipItemRow(
    item: Equipment,
    currentlyEquipped: Equipment?,
    heroColor: Color,
    onEquip: () -> Unit
) {
    val isAlreadyEquipped = item.id == currentlyEquipped?.id

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (isAlreadyEquipped) 0.3f else 0.5f
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Item name + tier badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = item.getThemeColor()
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = item.getThemeColor().copy(alpha = 0.15f)
                    ) {
                        Text(
                            item.tier.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = item.getThemeColor(),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Stat comparison: current value → new value
                if (currentlyEquipped != null && !isAlreadyEquipped) {
                    StatComparison(
                        currentItem = currentlyEquipped,
                        newItem = item
                    )
                } else if (isAlreadyEquipped) {
                    Text(
                        "Currently equipped",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        item.bonusDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isAlreadyEquipped) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onEquip,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = heroColor.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Equip", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
```

### 6. StatComparison composable

Compare effects of the same type between current and new item:

```kotlin
@Composable
private fun StatComparison(currentItem: Equipment, newItem: Equipment) {
    // Collect all effect types from both items
    val allTypes = (currentItem.effects.map { it.type } + newItem.effects.map { it.type }).distinct()

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        allTypes.forEach { type ->
            val currentVal = currentItem.effects.find { it.type == type }?.value ?: 0f
            val newVal = newItem.effects.find { it.type == type }?.value ?: 0f

            if (currentVal != newVal) {
                val label = type.name.lowercase().replace("_", " ")
                val fmt = { v: Float -> if (v >= 1f) "+${v.toInt()}" else if (v > 0f) "+${(v * 100).toInt()}%" else "0" }
                val isBetter = newVal > currentVal

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${fmt(currentVal)} → ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        fmt(newVal),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBetter) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                    Text(
                        " $label",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
```

This shows each effect type where the current and new values differ, in format:

```
+5% → +10%  atk_percent   (green text when better)
+0% → +3%   crit_chance   (green)
```

## Files to modify

| File | Changes |
|---|---|
| `PartyScreen.kt` | Replace `onEquip = {}` with dialog trigger; add `equipSlot` state; remove "Available Inventory" block; add `EquipItemDialog`, `EquipItemRow`, `StatComparison` composables |

## Dependencies

- Apply after Plan 18 (dialog reorder + scrolling fix) — this plan replaces the inventory section that Plan 18 would have moved.
- Plan 22 tier colors are reused for the item name + badge in the equip dialog.
