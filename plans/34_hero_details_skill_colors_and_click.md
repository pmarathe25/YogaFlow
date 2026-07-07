# Plan 34: Hero Details — Skill Colors Match Battle Cards, Click Opens Skill Card

## Problem

Two issues in the hero details dialog's skill display:

1. **Color mismatch** — The party screen `SkillCard` computes its own colors using different logic than the battle `SkillCard`. For example, Shanti's "Gentle Strike" (a physical damage skill) appears white in the party screen but red in battle, because the party screen falls through to `MaterialTheme.colorScheme.onSurface` for physical damage while battle uses pale red backgrounds for damage skills.

2. **No detail popup** — Skills are static text in the hero details. Clicking a skill should open a skill card popup showing the full detail (icon, description, mechanics), reusing the battle `SkillCard` component.

## Changes

### 1. Align skill colors with battle card colors

**`PartyScreen.kt:408-416`** — update the `typeColor` computation to match the color scheme used in `ActionTray.kt:568-587` (`getCardColor()`):

```kotlin
val typeColor = when {
    isUltimate -> heroColor
    skill.healScaling != null -> Color(0xFF66BB6A)        // green
    skill.shieldScaling != null -> Color(0xFF42A5F5)      // blue
    skill.damageComponents.any { it.element == Element.FIRE } -> Color(0xFFE53935)
    skill.damageComponents.any { it.element == Element.WATER } -> Color(0xFF1E88E5)
    skill.damageComponents.any { it.element == Element.AIR } -> Color(0xFFB0BEC5)
    skill.damageComponents.any { it.element == Element.EARTH } -> Color(0xFF795548)
    skill.damageComponents.any { it.element == Element.LIGHT } -> Color(0xFFFFF176)
    skill.damageComponents.any { it.element == Element.DARK || it.element == Element.SHADOW } -> Color(0xFF7B1FA2)
    skill.damageComponents.any { it.type == DamageType.ELEMENTAL } -> elementToColor(
        skill.damageComponents.firstNotNullOfOrNull { it.element } ?: Element.NEUTRAL)
    skill.buffs.isNotEmpty() -> Color(0xFFFFD740)          // amber
    skill.cleanse -> Color(0xFF7E57C2)                     // purple
    skill.statusEffects.isNotEmpty() -> Color(0xFFFF7043)  // deep orange
    else -> MaterialTheme.colorScheme.onSurface
}
```

Also update the card background to match battle's approach — use a tinted surface instead of default card color:

```kotlin
Surface(
    color = typeColor.copy(alpha = 0.08f),   // pale tinted bg
    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.3f)),
    ...
)
```

### 2. Make skills clickable → show skill card popup

**`PartyScreen.kt:388-398`** — add click handlers to each skill. When clicked, show a dialog that reuses the battle `SkillCard` composable.

Add state:
```kotlin
var selectedSkill by remember { mutableStateOf<Skill?>(null) }
```

Wrap each skill in a clickable:

```kotlin
hero.skills.forEach { skill ->
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { selectedSkill = skill },
        ...
    ) {
        SkillCard(skill = skill, ...)
    }
}
```

Add dialog at top of dialog:
```kotlin
selectedSkill?.let { skill ->
    Dialog(onDismissRequest = { selectedSkill = null }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Reuse the battle SkillCard composable for full detail
                com.example.game.ui.components.ActionTray.SkillCard(
                    skill = skill,
                    isUltimate = false,
                    ultReady = true,
                    heroLevel = partyMember.level,
                    modifier = Modifier.size(250.dp, 320.dp)  // larger for detail view
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { selectedSkill = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Close") }
            }
        }
    }
}
```

For the ultimate skill, set `isUltimate = true` and `ultReady = true` in the dialog call.

> Note: The battle `SkillCard` is `internal`, so it's accessible within the same module. Verify the import path — `ActionTray.kt` and `PartyScreen.kt` are both in `com.example.game.ui.components`, so no visibility issues.

## Files to modify

| File | Changes |
|---|---|
| `PartyScreen.kt` | Update `typeColor` logic to match battle `getCardColor()`; add click handler on each skill card; add dialog showing battle-style `SkillCard` on click |

## Dependencies

- None. Standalone visual fix.
