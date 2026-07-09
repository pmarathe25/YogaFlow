# Plan 66: Fix Hero Detail Page Skill Display

## Problem
Two issues with skill display in `HeroDetailsDialog` (PartyScreen.kt):

### 1. Hero detail skill list uses a separate `HeroDetailSkillCard` composable instead of `SkillCard`
`HeroDetailSkillCard` (PartyScreen.kt:454-558) is a completely separate implementation from the battle `SkillCard` (ActionTray.kt:436-602). It has different layout, different color logic, and different rendering. The user wants the EXACT same `SkillCard` component used in battle.

The popup dialog that appears when tapping a skill (PartyScreen.kt:404-427) already uses `SkillCard` — that part is correct. But the list items (lines 433-445) use `HeroDetailSkillCard`.

### 2. "Rippling Current" and similar skills show wrong color
The `typeColor` logic in `HeroDetailSkillCard` (lines 455-471) uses explicit element checks (`it.element == Element.WATER`, etc.) but falls through to `else -> MaterialTheme.colorScheme.onSurface` (white) for any skill that doesn't match these exact conditions. The `SkillCard` border color logic (ActionTray.kt:457-465) uses a different classification: it checks `skill.damageComponents.isNotEmpty()` as a general damage indicator, then `healScaling`, `shieldScaling`, `buffs`, etc. The HeroDetailSkillCard's `typeColor` also doesn't have the same fallback structure.

For "Rippling Current" (WATER element with `damageComponents = [{type: ELEMENTAL, element: WATER}]`), the `typeColor` should match `it.element == Element.WATER` → `Color(0xFF1E88E5)` (blue). But if the JSON deserialization or any code path produces a match failure, it falls to white. Using the battle `SkillCard` component eliminates this divergence entirely.

### 3. Skill colors use heuristic detection instead of explicit type categories
The current `SkillCard` border color logic (ActionTray.kt:457-465) heuristically determines the skill type by checking `damageComponents`, `healScaling`, `shieldScaling`, and `buffs` in order. This is fragile:
- A skill with both `damageComponents` and `healScaling` (e.g. "Vampiric Strike") gets a damage border (red) because damage is checked first
- A skill with `baseDamage > 0` but empty `damageComponents` gets no damage border
- The user wants explicit typecasting: damage skills = red, heal skills = green, buff skills = blue, with combined colors for multi-type skills

## Fix

### Replace list item rendering with `SkillCard`
Change the skill list section in `HeroDetailsDialog` from using `HeroDetailSkillCard` to using `SkillCard` directly, sized appropriately for a mobile list:

**Before (lines 432-445)**:
```kotlin
Text("Skills", ...)
Spacer(Modifier.height(8.dp))

hero.skills.forEach { skill ->
    HeroDetailSkillCard(
        skill = skill, hero = hero, level = partyMember.level, heroColor = heroColor,
        modifier = Modifier.clickable { selectedSkill = skill }
    )
}

// Ultimate
Spacer(Modifier.height(12.dp))
HeroDetailSkillCard(
    skill = hero.ultimate, hero = hero, level = partyMember.level, isUltimate = true, heroColor = heroColor,
    modifier = Modifier.clickable { selectedSkill = hero.ultimate }
)
```

**After**:
```kotlin
Text("Skills", ...)
Spacer(Modifier.height(8.dp))

hero.skills.forEach { skill ->
    SkillCard(
        skill = skill,
        heroColor = heroColor,
        isUltimate = false,
        ultReady = true,
        heroLevel = partyMember.level,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 4.dp)
            .clickable { selectedSkill = skill }
    )
}

// Ultimate
Spacer(Modifier.height(12.dp))
SkillCard(
    skill = hero.ultimate,
    heroColor = heroColor,
    isUltimate = true,
    ultReady = true,
    heroLevel = partyMember.level,
    modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .padding(vertical = 4.dp)
        .clickable { selectedSkill = hero.ultimate }
)
```

### Remove `HeroDetailSkillCard`
Delete the `HeroDetailSkillCard` composable (PartyScreen.kt:454-558) entirely since it is no longer used.

### Add explicit `SkillType` field on `Skill` data class
Instead of heuristically determining skill type from component fields, add an explicit `SkillType` enum to the `Skill` model:

**`app/src/main/java/com/example/game/model/Skill.kt`** — Add enum:
```kotlin
enum class SkillType(val label: String) {
    DAMAGE("Damage"),
    HEAL("Heal"),
    BUFF("Buff")
}
```

Add field to `Skill`:
```kotlin
data class Skill(
    ...
    val type: SkillType = SkillType.DAMAGE,
    ...
)
```

Add `combinedTypes: List<SkillType>` for multi-type skills:
```kotlin
data class Skill(
    ...
    val type: SkillType = SkillType.DAMAGE,
    val combinedTypes: List<SkillType> = emptyList(),
    ...
)
```

### Update `SkillCard` border color logic
In `ActionTray.kt:457-465`, replace the heuristic chain with explicit type-based colors. For single-type skills use the primary color. For multi-type skills (when `combinedTypes` is non-empty) blend the colors:

```kotlin
private fun skillCardBorderColor(
    skill: Skill,
    isOnCooldown: Boolean,
    isUltimate: Boolean,
    ultReady: Boolean
): Color {
    if (isOnCooldown) return Color.Gray
    if (isUltimate) return Color(0xFFFFD700)

    val types = skill.combinedTypes.ifEmpty { listOf(skill.type) }
    val colorMap = mapOf(
        SkillType.DAMAGE to Color(0xFFD32F2F),  // red
        SkillType.HEAL to Color(0xFF689F38),     // green
        SkillType.BUFF to Color(0xFF0288D1),     // blue
    )
    return if (types.size == 1) {
        colorMap[types.first()] ?: Color(0xFF0288D1)
    } else {
        val colors = types.mapNotNull { colorMap[it] }
        val avgR = colors.sumOf { it.red * 255 }.toInt() / colors.size
        val avgG = colors.sumOf { it.green * 255 }.toInt() / colors.size
        val avgB = colors.sumOf { it.blue * 255 }.toInt() / colors.size
        Color(avgR, avgG, avgB)
    }
}
```

### Add missing `baseDamage` check for backward compat
For backward compatibility during the transition, also add `skill.baseDamage > 0` check to catch skills that haven't been annotated yet:

```kotlin
val effectiveType = if (skill.type == SkillType.DAMAGE || skill.baseDamage > 0 || skill.damageComponents.isNotEmpty())
    SkillType.DAMAGE else skill.type
```

## Files to Modify
- `app/src/main/java/com/example/game/model/Skill.kt` — add `SkillType` enum, add `type` and `combinedTypes` fields to `Skill`
- `app/src/main/java/com/example/game/ui/components/PartyScreen.kt` — replace `HeroDetailSkillCard` calls with `SkillCard`; delete `HeroDetailSkillCard` composable (lines 454-558)
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt` — replace heuristic border color logic with `skillCardBorderColor()` using `skill.type` / `skill.combinedTypes`
- (Optionally) JSON data source for skills — add `"type": "damage"|"heal"|"buff"` and `"combinedTypes": [...]` fields if skills are loaded from JSON

## Dependencies
- None
