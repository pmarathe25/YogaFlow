# Plan: Visual Gear Layout with Hero Body

## Problem

The hero detail dialog currently shows equipped gear as a simple text list (slot name + item name). This doesn't give a visual sense of where gear goes on the hero.

## Solution

Rework the gear section of `HeroDetailsDialog` to show the hero's body silhouette on the left with 3 gear slot positions arranged on the right, inspired by RPG equipment screens (Diablo, Witcher).

## Layout Design

```
┌──────────────────────────────────┐
│ Hero name + level      [Close]  │
│ Flavor quote                     │
│ ──────────────────────────────── │
│ HP: XXX   ATK: XX   SPD: XX     │
│ ──────────────────────────────── │
│        Equipment                 │
│ ┌──────────┐  ┌──────────────┐  │
│ │          │  │ Weapon:      │  │
│ │  Hero    │  │ [Item Name]  │  │
│ │ Portrait │  │ HP+10% ATK+5%│  │
│ │          │  ├──────────────┤  │
│ │          │  │ Armor:       │  │
│ │          │  │ [Item Name]  │  │
│ │          │  │ DEF+15%      │  │
│ │          │  ├──────────────┤  │
│ │          │  │ Accessory:   │  │
│ │          │  │ [Item Name]  │  │
│ │          │  │ SPD+12%      │  │
│ └──────────┘  └──────────────┘  │
│                                 │
│ [Level Up button]               │
│                                 │
│ Available Inventory             │
│ - Item 1              [EQUIP]  │
│ - Item 2              [EQUIP]  │
└──────────────────────────────────┘
```

Each slot card should:
- Show the slot icon (from the item if equipped, or a generic slot icon if empty)
- Show the item name (or "Empty" in gray/italic)
- Show the item's `bonusDescription` (first line, truncated)
- Be clickable: tapping an empty slot could open an equip picker; tapping an equipped item could offer unequip

Empty slots should render as a dashed/disabled outline to communicate "there's space here".

## Implementation

**File:** `app/src/main/java/com/example/game/ui/components/PartyScreen.kt:233-265`

Replace the current loop over `slots.forEach { slot -> ... }` with a layout using `Row` containing:

### Left Column: Hero Portrait
```kotlin
Box(
    modifier = Modifier.size(120.dp),
    contentAlignment = Alignment.Center
) {
    HeroPortrait(hero.id, heroColor, 100.dp)
}
```

### Right Column: Gear Slots
```kotlin
Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    EquipmentSlotCard(slot = EquipmentSlot.WEAPON, item = weaponItem, heroColor, onUnequip, onEquip)
    EquipmentSlotCard(slot = EquipmentSlot.ARMOR, item = armorItem, heroColor, onUnequip, onEquip)
    EquipmentSlotCard(slot = EquipmentSlot.ACCESSORY, item = accessoryItem, heroColor, onUnequip, onEquip)
}
```

### Slot Card composable
```kotlin
@Composable
private fun EquipmentSlotCard(
    slot: EquipmentSlot,
    item: Equipment?,
    heroColor: Color,
    onUnequip: (String) -> Unit,
    onEquip: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (item != null) 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth().clickable { if (item != null) onUnequip(item.id) else onEquip() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot icon
            Text(
                item?.getIcon() ?: when (slot) {
                    EquipmentSlot.WEAPON -> "🗡️"
                    EquipmentSlot.ARMOR -> "🛡️"
                    EquipmentSlot.ACCESSORY -> "💍"
                },
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item?.name ?: "Empty ${slot.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (item != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (item != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                if (item != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.bonusDescription.split("\n").first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (item != null) {
                IconButton(onClick = { onUnequip(item.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.LinkOff, contentDescription = "Unequip", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
```

### Click-to-equip on empty slots

If an empty slot is tapped, show an equip picker sheet (or navigate to the existing inventory list below). The simplest approach: when an empty slot is tapped, scroll to / highlight the available inventory section.

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/java/com/example/game/ui/components/PartyScreen.kt` | Replace the gear slot list with visual layout using `HeroPortrait` + `EquipmentSlotCard` composable |
