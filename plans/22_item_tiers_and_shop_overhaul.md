# Plan 22: Item Tier System & Shop Overhaul

## Changes

### 1. EquipmentTier enum — replace GENERIC with COMMON/UNCOMMON/RARE

**`Equipment.kt:9-11`** — remove `GENERIC` and `CLASS_SPECIFIC`, add `COMMON`, `UNCOMMON`, `RARE`:

```kotlin
enum class EquipmentTier {
    COMMON, UNCOMMON, RARE, UNIQUE
}
```

Update all references to `EquipmentTier.GENERIC`:
- `Equipment.goldCost` (line 33-35) — use tier-based multiplier
- `ShopScreen` item filter (line 132) — remove the `eq.tier == GENERIC` clause
- Any other references (grep for `GENERIC` and `CLASS_SPECIFIC`)

### 2. Expanded item list — more items per tier, items assigned by yoga level

**Existing items** get reassigned:

| Existing Item | Slot | Yoga Lv | Spark Cost | Tier | Effects |
|---|---|---|---|---|---|
| Training Blade | WEAPON | 1 | 2 | `COMMON` | ATK +5% |
| Traveler's Cloak | ARMOR | 1 | 2 | `COMMON` | HP +5% |
| Simple Beads | ACCESSORY | 1 | 3 | `COMMON` | Status Dur. Reduction +1 |
| Crystal Sword | WEAPON | 3 | 5 | `UNCOMMON` | ATK +10%, CRIT +3% |
| Enchanted Robe | ARMOR | 3 | 5 | `UNCOMMON` | HP +10%, Status Res. +5% |
| Ember Pendant | ACCESSORY | 3 | 5 | `UNCOMMON` | Bonus Fire on Strike +10% |
| Swift Boots | ACCESSORY | 4 | 6 | `UNCOMMON` | SPD +12% |
| Crystal Ward | ARMOR | 5 | 8 | `UNCOMMON` | Start Shield +15% |
| Mythril Edge | WEAPON | 6 | 10 | `RARE` | ATK +15%, CRIT +5%, CRIT DMG +10% |
| Ethereal Vestment | ARMOR | 6 | 10 | `RARE` | HP +15%, Status Res. +10%, Incoming Heal +5% |
| Force Amulet | ACCESSORY | 6 | 10 | `RARE` | DMG +8% |
| Sage's Tome | ACCESSORY | 7 | 12 | `RARE` | Ult Gain +15%, SPD +5% |

**New items to add** — fills gaps so each tier has 2-3 items per slot:

| New Item | Slot | Yoga Lv | Spark Cost | Tier | Effects |
|---|---|---|---|---|---|
| Practice Staff | WEAPON | 1 | 2 | `COMMON` | ATK +3% |
| Cotton Gloves | WEAPON | 2 | 3 | `COMMON` | ATK +2%, SPD +2% |
| Ripped Vest | ARMOR | 1 | 2 | `COMMON` | HP +3% |
| Wooden Bracers | ARMOR | 2 | 3 | `COMMON` | HP +3%, Status Res. +2% |
| Focus Charm | ACCESSORY | 1 | 2 | `COMMON` | Ult Gain +5% |
| Breath Counter | ACCESSORY | 2 | 3 | `COMMON` | SPD +3% |
| Runed Dagger | WEAPON | 4 | 6 | `UNCOMMON` | ATK +8%, CRIT +2% |
| Scale Mail | ARMOR | 4 | 5 | `UNCOMMON` | HP +8%, Incoming Heal +5% |
| Meditation Stone | ACCESSORY | 5 | 6 | `UNCOMMON` | Heal Amount +8% |
| Wind Whistle | ACCESSORY | 3 | 5 | `UNCOMMON` | SPD +8%, Stacks +1 |
| Shadow Blade | WEAPON | 7 | 10 | `RARE` | ATK +12%, CRIT +4%, CRIT DMG +5% |
| Phoenix Feather Robe | ARMOR | 8 | 10 | `RARE` | HP +12%, Incoming Heal +10%, Status Res. +5% |
| Dragon's Eye | ACCESSORY | 8 | 12 | `RARE` | All Stats +3%, Ult Gain +8% |

UNIQUE items (hero items) stay `UNIQUE` — no change.

### 3. goldCost per tier

**`Equipment.kt:33-35`** — update `goldCost` to use tier multipliers:

```kotlin
val goldCost: Int get() = sparkCost * when (tier) {
    EquipmentTier.COMMON -> 5
    EquipmentTier.UNCOMMON -> 7
    EquipmentTier.RARE -> 10
    EquipmentTier.UNIQUE -> 10
}
```

### 4. getThemeColor() per tier

**`Equipment.kt:74-89`** — color by tier:

```kotlin
fun getThemeColor(): Color {
    if (tier == EquipmentTier.UNIQUE && heroId != null) {
        return when (heroId) {  // hero-specific gold shades
            1 -> Color(0xFF2196F3)  // Shanti — blue
            2 -> Color(0xFF795548)  // Santosha — brown
            3 -> Color(0xFFF44336)  // Virya — red
            4 -> Color(0xFFFFD54F)  // Dhairya — yellow
            5 -> Color(0xFF81D4FA)  // Maitri — light blue
            else -> Color.Gray
        }
    }
    return when (tier) {
        EquipmentTier.COMMON -> Color.Gray
        EquipmentTier.UNCOMMON -> Color(0xFF4CAF50)  // green
        EquipmentTier.RARE -> Color(0xFF2196F3)       // blue
        EquipmentTier.UNIQUE -> Color(0xFFFFD700)     // gold
    }
}
```

### 5. Shop: replace hero filters with tier filters

**`ShopScreen.kt:104-120`** — remove hero filter `LazyRow`, add tier filter `LazyRow`:

```kotlin
var selectedTierFilter by remember { mutableStateOf<EquipmentTier?>(null) }

LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(listOf(null) + EquipmentTier.values().toList()) { tier ->
        val label = tier?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All"
        FilterChip(
            selected = selectedTierFilter == tier,
            onClick = { selectedTierFilter = tier },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
```

### 6. Shop: update item filter for tiers

**`ShopScreen.kt:129-134`** — replace the old filter:

```kotlin
val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory && eq.id !in battleRewardItemIds &&
    (selectedTierFilter == null || eq.tier == selectedTierFilter)
}
```

### 7. Shop: add tier color indicators to item cards

**`ShopScreen.kt:164-250` (ShopItemCard)** — add a colored left border or accent based on tier:

```kotlin
GlassCard(
    modifier = Modifier
        .fillMaxWidth().padding(vertical = 4.dp)
        .alpha(if (levelLocked) 0.6f else 1f)
        .clickable { onShowDetail() },
    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    elevation = 2.dp,
    useDefaultPadding = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp)
            .then(
                if (item.tier == EquipmentTier.UNIQUE && item.heroId != null)
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = item.getThemeColor().copy(alpha = 0.8f),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ... existing content
    }
}
```

Add a colored tier badge next to the item name:

```kotlin
Text(
    item.name,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.Bold,
    color = item.getThemeColor()
)
// Tier badge
Surface(
    shape = RoundedCornerShape(4.dp),
    color = item.getThemeColor().copy(alpha = 0.15f)
) {
    Text(
        item.tier.name.lowercase().replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.labelSmall,
        color = item.getThemeColor(),
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
    )
}
```

### 8. Shop: split gear list into tier sections

**`ShopScreen.kt:290-330`** — replace the flat `LazyColumn { items(available) { ... } }` with tier-grouped sections:

```kotlin
LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
    if (selectedTierFilter != null) {
        // Single-tier filter — flat list, no section headers needed
        items(available, key = { it.id }) { item ->
            ShopItemCard(item = item, onShowDetail = { selectedItem = item })
        }
    } else {
        // All tiers — group by tier with sticky headers
        val grouped = available.groupBy { it.tier }
        val tierOrder = listOf(
            EquipmentTier.COMMON,
            EquipmentTier.UNCOMMON,
            EquipmentTier.RARE,
            EquipmentTier.UNIQUE
        )
        for (tier in tierOrder) {
            val tierItems = grouped[tier].orEmpty()
            if (tierItems.isEmpty()) continue

            stickyHeader(key = "header_${tier.name}") {
                TierSectionHeader(tier)
            }
            items(tierItems, key = { it.id }) { item ->
                ShopItemCard(item = item, onShowDetail = { selectedItem = item })
            }
        }
    }
}
```

**Add the section header composable** (near the bottom of the file):

```kotlin
@Composable
fun TierSectionHeader(tier: EquipmentTier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = tier.getThemeColor().copy(alpha = 0.2f)
        ) {
            Text(
                tier.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tier.getThemeColor(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
```

> **Note:** If `stickyHeader` is not already imported, add `import androidx.compose.foundation.lazy.stickyHeader` to the imports block.

### 9. Shop: show concise stats summary on item cards

**`ShopScreen.kt:202-208`** — replace the "Yoga Lv.X" line with a stats summary line:

```kotlin
Text(
    item.bonusDescription.split("\n").first(),  // first effect only
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
)
```

For the full stats, keep the detail dialog (GearDetailsDialog) unchanged — it already shows the full `bonusDescription`.

## Files to modify

| File | Changes |
|---|---|
| `Equipment.kt` | Update `EquipmentTier` enum, `goldCost`, `getThemeColor()` |
| `equipment.json` | Replace `"GENERIC"` with `"COMMON"`, `"UNCOMMON"`, or `"RARE"` per tier table; add 13 new items |
| `ShopScreen.kt` | Replace hero filters with tier filters; update item filter; add tier color/border to `ShopItemCard`; add tier-section sticky headers; add `TierSectionHeader` composable; add stats summary line |
| Any other file referencing `EquipmentTier.GENERIC` or `.CLASS_SPECIFIC` | Update references |

## Dependencies

- Plan 15 (shop item display) — the tier filter replaces the hero filter from that plan, so apply Plan 22 after Plan 15.
