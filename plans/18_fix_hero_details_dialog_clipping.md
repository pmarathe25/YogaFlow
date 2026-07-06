# Plan 18: Fix HeroDetailsDialog Content Clipping

## Problem

The `HeroDetailsDialog` (PartyScreen.kt lines 246-411) has a fixed `heightIn(max = 650.dp)` constraint on its outer `Surface` but uses a plain `Column` **without `.verticalScroll()`**. The dialog contains too much content to fit:

1. Header (hero portrait + name + close) ≈ 80dp
2. Flavor quote ≈ 40dp
3. Divider ≈ 4dp
4. Stat bars (HP, ATK, SPD) ≈ 80dp
5. Divider ≈ 4dp
6. Skills heading ≈ 20dp
7. 4-5 skill cards × 80dp each ≈ 320-400dp
8. Ultimate card ≈ 80dp
9. Spacer ≈ 20dp
10. Level Up button ≈ 50dp
11. (LevelUpDialog — shown on demand)
12. Spacer ≈ 24dp
13. Gear section heading ≈ 20dp
14. Gear layout (portrait + 3 slot cards) ≈ 150dp
15. Spacer ≈ 16dp
16. Inventory heading ≈ 20dp
17. Inventory list (variable)

**Estimated total: ~860-940dp+, far exceeding 650dp.**

### Items clipped (below the fold):
- **Level Up button** (#10) — the user thinks it was "removed"
- **Gear section** (#13-14) — the visual gear layout from Plan 08 is in the code but invisible
- **Inventory** (#16-17)

The gear layout code EXISTS at lines 330-377 (hero portrait + 3 `EquipmentSlotCard`s) but is never seen because everything below skill cards is clipped.

## Fix

### Step 1: Add scrolling

Make the dialog's Column scrollable:

```kotlin
// line 252, add .verticalScroll()
Column(
    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())  // ADD .verticalScroll()
) {
    // ... existing content unchanged
}
```

Add imports:
```kotlin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
```

### Step 2: Reorder — push skills + ultimate to the bottom

Move skill cards and ultimate to the **bottom** of the dialog. The primary actions/readouts (stats → level up → gear → inventory) should come first; skills are reference info.

```
Before:                        After:
1. Header                      1. Header
2. Quote                       2. Quote
3. Stats bars                  3. Stats bars
4. Skills                      4. Level Up button (immediate action)
5. Ultimate                    5. Gear layout (equip/unequip)
6. Level Up button (hidden!)   6. Inventory
7. Gear layout (hidden!)       7. Skills (pushed to bottom)
8. Inventory (hidden!)          8. Ultimate (pushed to bottom)
```

This means reordering the dialog's Column from this:

```
Header → Quote → Stats → Skills → Ultimate → Level Up → Gear → Inventory
```

To this:

```
Header → Quote → Stats → Level Up → Gear → Inventory → Skills → Ultimate
```

Move the Level Up block (lines 302-326) **before** Skills (line 289). Move the Gear heading + layout + Inventory (lines 330-408) **before** Skills as well. Keep Skills (lines 289-298) and Ultimate (lines 296-298) at the very end.

### Step 3: Verify gear layout renders

After adding scrolling, the existing gear layout code at lines 330-377 will become visible. It already has:
- Hero portrait on the left
- 3 `EquipmentSlotCard`s (Weapon/Armor/Accessory) on the right
- Each card shows icon + name + bonus description + unequip button
- Below: inventory list with "EQUIP" buttons

No additional code changes needed for the gear layout itself.

## Files to modify

| File | Changes |
|---|---|
| `PartyScreen.kt:252` | Add `.verticalScroll(rememberScrollState())` |
| `PartyScreen.kt:302-326` | Move Level Up block before Skills section (line 289) |
| Add import for `rememberScrollState` and `verticalScroll` if missing |
