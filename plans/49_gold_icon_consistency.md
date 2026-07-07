# Plan 49: Gold Icon Consistency Across the Game

## Problem

The gold currency uses different icons and colors across screens:

| Screen | Icon/Emoji | Color |
|---|---|---|
| Path of Zen (JourneyScreen) | `Icons.Default.MonetizationOn` (Material icon) | `0xFFFFD700` |
| Shop (ShopScreen) | `🪙` coin emoji as Text | `0xFFFFD740` |
| Rewards (PlayerScreens) | No icon — plain text | `0xFFFFD600` |
| Party (PartyScreen) | Not displayed at all | — |
| Battle Result (BattleResultScreen) | Not displayed at all | — |

This inconsistency is confusing. Gold should use the same icon and color everywhere it appears.

## Fix

### 1. Standardize on `🪙` coin emoji with gold `0xFFFFD700`

Use the coin emoji `🪙` (`\uD83E\uDE99`) as a `Text` composable with `Color(0xFFFFD700)` across all screens. The coin emoji is more universally recognizable as currency than the `MonetizationOn` Material icon.

### 2. Update JourneyScreen

**`JourneyScreen.kt:149-150`** — replace `Icon(Icons.Default.MonetizationOn)` with `Text("🪙")`:

Before:
```kotlin
Icon(Icons.Default.MonetizationOn, contentDescription = null,
    tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
```

After:
```kotlin
Text("\uD83E\uDE99", fontSize = 16.sp)
```

### 3. Update ShopScreen header

**`ShopScreen.kt:89-93`** — update the gold coin color to match `0xFFFFD700`:

Before:
```kotlin
Text("\uD83E\uDE99", color = Color(0xFFFFD740), ...)
...
Text("$availableGold", color = Color(0xFFFFD740), ...)
```

After:
```kotlin
Text("\uD83E\uDE99", color = Color(0xFFFFD700), ...)
...
Text("$availableGold", color = Color(0xFFFFD700), ...)
```

Also update the gold cost display in `ShopItemCard` (line 353, 356):
```kotlin
Text("${item.goldCost} \uD83E\uDE99", ..., color = if (canAfford && !levelLocked) Color(0xFFFFD700) else ...)
```

### 4. Add gold to PlayerScreens rewards

**`PlayerScreens.kt:487`** — add the coin emoji next to the gold value:

Before:
```kotlin
RewardItem(value = "${gameSaveData.gold}", label = "Gold", color = Color(0xFFFFD600))
```

After:
```kotlin
RewardItem(value = "${gameSaveData.gold} \uD83E\uDE99", label = "Gold", color = Color(0xFFFFD700))
```

### 5. Add gold to Party page header

**`PartyScreen.kt`** — add gold display in the header row alongside sparks, matching the Path of Zen layout:

In the header `Row`, after the sparks display:
```kotlin
Spacer(Modifier.width(12.dp))
Text("\uD83E\uDE99", fontSize = 14.sp)
Spacer(Modifier.width(4.dp))
Text(
    "${saveData.gold}",
    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
    color = Color(0xFFFFD700)
)
```

### 6. Add gold reward to BattleResultScreen

**`BattleResultScreen.kt`** — display the gold earned from the battle. The `GameViewModel` already calculates `newGoldEarned` but it's never shown. Pass the gold earned to `BattleResultScreen` and display it alongside turns taken and damage dealt:

```kotlin
// In the stats section:
Row {
    Text("Gold earned: ${goldEarned} \uD83E\uDE99", ...)
}
```

### 7. Remove unused `MonetizationOn` import

**`JourneyScreen.kt`** — remove `import androidx.compose.material.icons.filled.MonetizationOn` if no longer used.

## Files to modify

| File | Changes |
|---|---|
| `JourneyScreen.kt` | Replace `MonetizationOn` Icon with `🪙` Text; update color to `0xFFFFD700` |
| `ShopScreen.kt` | Update gold color from `0xFFFFD740` to `0xFFFFD700` in header and item cards |
| `PlayerScreens.kt` | Add coin emoji to gold reward; update color to `0xFFFFD700` |
| `PartyScreen.kt` | Add gold display to header row with 🪙 emoji |
| `BattleResultScreen.kt` | Add gold earned display in stats section |

## Dependencies

- May need to pass `goldEarned` to `BattleResultScreen` from the ViewModel.
