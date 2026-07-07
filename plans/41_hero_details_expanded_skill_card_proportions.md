# Plan 41: Fix Hero Details Expanded Skill Card Proportions

## Problem

The expanded skill card dialog (`SkillCard` inside a `Dialog`) has a fixed size of `250.dp x 320.dp`. The mechanics description text uses `Modifier.weight(1f)`, which forces it to fill all remaining vertical space inside the card. This creates two issues:

1. The card body is too large relative to its sparse contents (an icon, a name, a short description).
2. The contents appear vertically offset because the mechanics text stretches to fill the gap, pushing the flavor text to the bottom.

## Fix

### 1. Remove fixed height / use wrapContentSize

**`PartyScreen.kt:408`** — change from a fixed `320.dp` height to `wrapContentSize()` or remove height constraint entirely, letting the card size to its content:

Before:
```kotlin
SkillCard(
    ...
    modifier = Modifier.size(250.dp, 320.dp)
)
```

After:
```kotlin
SkillCard(
    ...
    modifier = Modifier.widthIn(min = 250.dp, max = 300.dp)   // constrain width only
)
```

### 2. Remove `weight(1f)` from mechanics description

**`ActionTray.kt:513`** (or wherever the `SkillCard` used in the dialog sets `weight(1f)` on the mechanics text) — remove `Modifier.weight(1f)` so the text only takes its natural height:

Before:
```kotlin
Text(
    text = skill.getMechanicsDescription(heroLevel),
    style = MaterialTheme.typography.labelSmall,
    ...
    modifier = Modifier.weight(1f)  // fills remaining space
)
```

After:
```kotlin
Text(
    text = skill.getMechanicsDescription(heroLevel),
    style = MaterialTheme.typography.labelSmall,
    ...
    // no weight modifier — natural height
)
```

### 3. Adjust column arrangement

If the icon, name, mechanics, and flavor text should be evenly distributed rather than top-aligned, use `Arrangement.SpaceEvenly` or `Arrangement.spacedBy()` on the `Column`:

```kotlin
Column(
    modifier = Modifier.padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)  // even spacing between items
) {
    // icon, name, mechanics, flavor text...
}
```

## Files to modify

| File | Changes |
|---|---|
| `PartyScreen.kt` | Change `SkillCard` modifier from fixed size to width-constrained only |
| `ActionTray.kt` | Remove `Modifier.weight(1f)` from mechanics description; add `Arrangement.spacedBy` for even layout |

## Dependencies

- None. Standalone layout fix.
