# Plan 48: Shop Bottom Bar Icons

## Problem

The Shop screen's bottom navigation bar uses mismatched Material icons for equipment categories:
- **Weapon**: `Icons.Default.Shield` (a shield, not a weapon)
- **Armor**: `Icons.Default.Checkroom` (a coat hanger/closet)
- **Accessory**: `Icons.Default.Watch` (a wristwatch)

These don't clearly communicate the equipment slot type. The Party screen already uses clear emoji icons for the same slots (🗡️ for weapon, 🛡️ for armor, 💍 for accessory).

## Fix

### 1. Replace Material icons with emoji icons

**`ShopScreen.kt:210-248`** — change the `icon` parameter in each `NavigationBarItem` from Material icons to `Text` composables with emojis:

Before:
```kotlin
EquipmentSlot.WEAPON.let { slot ->
    NavigationBarItem(
        icon = { Icon(Icons.Default.Shield, contentDescription = null) },
        ...
    )
}
EquipmentSlot.ARMOR.let { slot ->
    NavigationBarItem(
        icon = { Icon(Icons.Default.Checkroom, contentDescription = null) },
        ...
    )
}
EquipmentSlot.ACCESSORY.let { slot ->
    NavigationBarItem(
        icon = { Icon(Icons.Default.Watch, contentDescription = null) },
        ...
    )
}
```

After:
```kotlin
EquipmentSlot.WEAPON.let { slot ->
    NavigationBarItem(
        icon = { Text("\uD83D\uDDE1\uFE0F", fontSize = 20.sp) },    // 🗡️ sword
        ...
    )
}
EquipmentSlot.ARMOR.let { slot ->
    NavigationBarItem(
        icon = { Text("\uD83D\uDEE1\uFE0F", fontSize = 20.sp) },    // 🛡️ shield
        ...
    )
}
EquipmentSlot.ACCESSORY.let { slot ->
    NavigationBarItem(
        icon = { Text("\uD83D\uDC8D", fontSize = 20.sp) },           // 💍 ring/amulet
        ...
    )
}
```

### 2. Remove unused icon imports

**`ShopScreen.kt:12-15`** — remove the unused Material icon imports:

```kotlin
// Remove these:
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Watch
```

Keep `Icons.Default.Lock` (still used at line 304 for the lock icon on level-locked items).

### 3. Update color scheme (optional)

The current colors per tab are: Weapon=red, Armor=green, Accessory=blue. These can remain as-is or be updated to match the element colors. No change needed unless desired.

## Files to modify

| File | Changes |
|---|---|
| `ShopScreen.kt` | Replace `Icon(Icons.Default.*)` with `Text(emoji)` in `SlotNavigationBar`; remove unused icon imports |

## Dependencies

- None. Standalone icon fix.
