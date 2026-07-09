# Plan 73: Fix Party Screen Hero Detail Crash

## Problem
Opening the hero detail dialog from the Party screen crashes the app. The `HeroDetailsDialog` (PartyScreen.kt:247-465) calls `SkillCard` (ActionTray.kt:448-607) for each hero skill (lines 433-445) and the ultimate (lines 447-459).

Potential causes:

### 1. `SkillCard` called without `baseCooldown`/`cooldownRemaining`
PartyScreen calls `SkillCard(skill, heroColor, isUltimate=false, ultReady=true, heroLevel=partyMember.level, ...)` without passing `baseCooldown` or `cooldownRemaining`. These default to `0`, which means `showCooldown = baseCooldown > 1` is `false` for cooldown=0 or cooldown=1 — likely correct. No crash expected here.

### 2. `skillCardBorderColor` function failure
The `skillCardBorderColor` function (ActionTray.kt:624-651) accesses `skill.type` and `skill.combinedTypes` — new fields added to `Skill` by Plan 66. If any skill data (loaded from JSON or constructed) has a `type` value that Moshi cannot deserialize, or if the `combinedTypes` list contains unknown enum values, the deserialization could fail at the point when the `Skill` object is accessed.

Check: Is there a missing `SkillType` entry for a particular skill's data? If Moshi throws during deserialization of `Skill` objects in `DataLoader.heroes`, any access to that hero's skills would fail.

### 3. `Color(Int, Int, Int)` constructor compatibility
The `skillCardBorderColor` function uses `Color(avgR, avgG, avgB)` where all args are `Int`. This constructor signature was added in Compose 1.6. With BOM 2024.09.00 this should be available, but if there's a mismatch between the compile-time and runtime Compose versions, a `NoSuchMethodError` could occur.

### 4. Dialog + SkillCard composition issue
The `SkillCard` in the popup dialog (lines 404-427) uses `Modifier.widthIn(min = 250.dp, max = 300.dp)` without explicit height. Inside a `Dialog` with default content wrapper, the card might not have enough height constraint, causing the `Column` inside `SkillCard` to measure infinitely and throw an `IllegalStateException` ("Vertically scrollable component was measured with zero constraints...").

Check: The `SkillCard`'s content Column at ActionTray.kt:521 has `verticalArrangement = Arrangement.spacedBy(8.dp)` but does NOT have `Modifier.heightIn(max=...)` or `Modifier.weight(1f)`. Combined with `Spacer(Modifier.weight(1f))` at line 432 in `ComboCard` is not in `SkillCard`. However, `SkillCard` doesn't use `Modifier.weight` so infinite height shouldn't be an issue.

### 5. Most Likely: `Spacer(Modifier.weight(1f))` inside a non-weight-parent
Actually, `SkillCard` doesn't have `Spacer(Modifier.weight(1f))` — that's only in `ComboCard`. But looking at `SkillCard`'s Column (lines 521-556), there's no `weight` modifier used, so no crash from that.

**Most likely cause**: Unknown — needs crash log inspection. The most productive fix approach is to add defensive nullability and error handling around the new `SkillType` references and ensure the `SkillCard` is properly constrained in the PartyScreen's Dialog context.

## Fix
Since the exact crash is unknown without a stack trace, apply defensive fixes:

### Fix A: Add explicit height to the popup `SkillCard`
In `PartyScreen.kt:414`, add an explicit `height` to the popup SkillCard modifier:
```kotlin
SkillCard(
    skill = skill,
    heroColor = elementToColor(hero.element),
    isUltimate = isUltimateSkill,
    ultReady = true,
    heroLevel = partyMember.level,
    modifier = Modifier.widthIn(min = 250.dp, max = 300.dp).height(220.dp)
)
```

### Fix B: Ensure `skillCardBorderColor` handles edge cases
In `ActionTray.kt:624-651`, add a try-catch or defensive defaults:
```kotlin
private fun skillCardBorderColor(...): Color {
    if (isOnCooldown) return Color.Gray
    if (isUltimate) return Color(0xFFFFD700)

    val effectiveType = try {
        if (skill.type == SkillType.DAMAGE || skill.baseDamage > 0 || skill.damageComponents.isNotEmpty())
            SkillType.DAMAGE else skill.type
    } catch (e: Exception) {
        SkillType.DAMAGE
    }

    val types = try {
        skill.combinedTypes.ifEmpty { listOf(effectiveType) }
    } catch (e: Exception) {
        listOf(SkillType.DAMAGE)
    }
    // ... rest unchanged
}
```

### Fix C: Verify `DataLoader` skill data has valid `SkillType` values
Check `DataLoader.kt` to ensure all skills are properly defined with valid `type` fields. If skills are loaded from JSON, ensure the JSON includes `"type": "damage"` for any skill that previously relied on heuristic detection.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/PartyScreen.kt` — add explicit `height(220.dp)` to popup SkillCard
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt` — add defensive error handling in `skillCardBorderColor`
- `app/src/main/java/com/example/game/persistence/DataLoader.kt` — verify all skill data has valid `type` and `combinedTypes` values

## Dependencies
- Investigate crash log to narrow down exact cause before implementation
