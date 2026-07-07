# Plan 31: Equipped Gear Click Opens Equip Dialog

## Problem

Clicking an occupied gear slot in the hero details dialog immediately unequips the item. This is destructive — the user may click accidentally and lose their equipped item. It should instead open the equip dialog (same as clicking an empty slot), allowing the user to swap or re-select.

The small `LinkOff` icon button provided as the only unequip path is inconsistent with the pattern — unequipping should happen through the dialog like everything else.

## Fix

### 1. Remove the `LinkOff` icon button

**`PartyScreen.kt:553-556`** — delete the `LinkOff` icon button block entirely from `EquipmentSlotCard`:

```kotlin
// DELETE THIS:
if (item != null) {
    IconButton(onClick = { onUnequip(item.id) }, modifier = Modifier.size(24.dp)) {
        Icon(Icons.Default.LinkOff, contentDescription = "Unequip", ...)
    }
}
```

### 2. Change click handler to always open dialog

**`PartyScreen.kt:520`** — change the `Surface` clickable so it always opens the equip dialog, regardless of whether a slot is occupied:

Before:
```kotlin
.clickable { if (item != null) onUnequip(item.id) else onEquip() }
```

After:
```kotlin
.clickable { onEquip() }   // always opens equip dialog
```

### 3. Remove `onUnequip` callback from `EquipmentSlotCard`

Since `LinkOff` is removed and the click handler no longer calls `onUnequip`, the `onUnequip` parameter is no longer used. Remove it from the `EquipmentSlotCard` signature and all call sites.

### 4. Add "None" (empty slot) option in `EquipItemDialog`

**`PartyScreen.kt:665-718`** — add a "None" row at the top of the items list in `EquipItemDialog` that unequips the current item in that slot.

Change the dialog's `onEquip` callback signature from `(String) -> Unit` to `(String?) -> Unit`, where `null` means "unequip current item":

```kotlin
private fun EquipItemDialog(
    slot: EquipmentSlot,
    items: List<Equipment>,
    currentlyEquipped: Equipment?,
    heroColor: Color,
    onEquip: (String?) -> Unit,     // null = unequip
    onDismiss: () -> Unit
)
```

Add a "None" row before `items.forEach`:

```kotlin
// None option (unequip current item)
EquipItemRow(
    item = null,
    currentlyEquipped = currentlyEquipped,
    heroColor = heroColor,
    onEquip = { onEquip(null) }
)

items.forEach { item ->
    ...
}
```

**`EquipItemRow` (lines 721-799)** — accept `item: Equipment?` instead of `item: Equipment`. When `item` is null, show a "None (empty slot)" label with an "Equip" button that fires `onEquip`.

### 5. Update call site in party screen

**`PartyScreen.kt:371-386`** — update the dialog invocation to handle the nullable `onEquip`:

```kotlin
EquipItemDialog(
    slot = slot,
    items = availableItems,
    currentlyEquipped = equipped.find { it.slot == slot },
    heroColor = heroColor,
    onEquip = { itemId ->
        if (itemId != null) {
            viewModel.equipItem(hero.id, itemId)
        } else {
            currentlyEquipped?.let { viewModel.unequipItem(hero.id, it.id) }
        }
        equipSlot = null
    },
    onDismiss = { equipSlot = null }
)
```

## Files to modify

| File | Changes |
|---|---|
| `PartyScreen.kt` | Remove `LinkOff` button; change click handler to always call `onEquip()`; remove `onUnequip` param from `EquipmentSlotCard`; add "None" option in `EquipItemDialog`; make `onEquip` nullable; update call site |

## Dependencies

- None. Standalone fix.
