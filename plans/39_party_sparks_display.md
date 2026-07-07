# Plan 39: Party Page Sparks Display

## Problem

The Party page currently shows sparks as inline text in the subtitle: `"Yoga Level: ${saveData.yogaLevel} | Sparks: ${saveData.sparks} ✦"`. This doesn't match the Path of Zen page, which displays sparks as an icon in the top-right corner of the header row with a dedicated icon (`⚡`) and the numeric count.

The spark display should be consistent across screens.

## Fix

### 1. Move sparks to header row

**`PartyScreen.kt:48-62`** — the current header has only a back button and "Your Heroes" title. Move the sparks (and gold) into this row, aligned to the right, matching the Path of Zen pattern.

Before:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    IconButton(onClick = onBack) { ... }
    Text("Your Heroes", ...)
}

Spacer(Modifier.height(8.dp))
Text(
    "Yoga Level: ${saveData.yogaLevel} | Sparks: ${saveData.sparks} \u2726",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    modifier = Modifier.padding(start = 12.dp)
)
```

After:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    IconButton(onClick = onBack) { ... }
    Text("Your Heroes", ...)
    Spacer(Modifier.weight(1f))

    // Sparks (no yoga level — removed per user request)
    Text("⚡", fontSize = 14.sp)
    Spacer(Modifier.width(4.dp))
    Text(
        "${saveData.sparks}",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}
```

Remove the old subtitle line entirely (`"Yoga Level: ${saveData.yogaLevel} | Sparks: ${saveData.sparks} \u2726"` and surrounding `Spacer`/`Text`).

### 2. Use same icon style as Path of Zen

Use `⚡` (lightning bolt) emoji to match the Path of Zen page, instead of `✦` (white diamond star).

## Files to modify

| File | Changes |
|---|---|
| `PartyScreen.kt` | Move sparks/yoga level into header Row with ⚡ icon; match Path of Zen layout |

## Dependencies

- None. Standalone UI consistency fix.
