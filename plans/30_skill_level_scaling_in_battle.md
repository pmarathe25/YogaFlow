# Plan 30: Fix Skill Level Scaling in Battle Card Display

## Problem

Battle skill cards always display level-1 values because `getMechanicsDescription()` is called without a hero level argument, defaulting to `heroLevel = 1`. The actual damage computed during battle correctly uses `combatant.level`, creating a mismatch between displayed and actual numbers.

**`ActionTray.kt:485`**:
```kotlin
Text(text = skill.getMechanicsDescription(), ...)  // defaults to level 1
```

**`BattleReducer.kt:461`**:
```kotlin
val skillBase = skill.baseDamage + skill.damagePerLevel * combatant.level  // uses actual level
```

## Fix

Pass the hero's actual level to `getMechanicsDescription()` in the battle `SkillCard`.

**`ActionTray.kt`** — the `SkillCard` composable already receives cooldown and ultimate info. Add a `heroLevel` parameter:

```diff
 @Composable
 internal fun SkillCard(
     skill: com.example.game.model.Skill,
     isUltimate: Boolean,
     ultReady: Boolean,
+    heroLevel: Int,
     baseCooldown: Int = 0,
     cooldownRemaining: Int = 0,
     modifier: Modifier = Modifier
 )
```

Then at line 485:
```diff
- Text(skill.getMechanicsDescription(), ...)
+ Text(skill.getMechanicsDescription(heroLevel), ...)
```

**Update caller** in `HandOfCards` where `SkillCard` is created. The current hero is available in the loop — pass `currentHero.level`:

```kotlin
SkillCard(
    skill = item,
    isUltimate = isUlt,
    ultReady = ultReady,
    heroLevel = currentHero.level,   // NEW
    baseCooldown = skill.cooldown,
    cooldownRemaining = cooldown,
    modifier = Modifier.size(150.dp, 220.dp)
)
```

`currentHero` is a `CombatantState` which already has a `level` field set during `Hero.toCombatantState()`.

## Inconsistency note

The party screen (`PartyScreen.kt:430`) computes display values as `baseDamage + damagePerLevel * (level - 1)`, while the battle engine computes `baseDamage + damagePerLevel * combatant.level`. These differ by `damagePerLevel` at any given level. The party screen formula treats `baseDamage` as the level-1 value; the battle engine treats `baseDamage` as the level-0 value and adds 1 level's worth. 

Both should agree. Fix the party screen to match the battle engine:
```diff
- val damage = skill.baseDamage + skill.damagePerLevel * (level - 1)
+ val damage = skill.baseDamage + skill.damagePerLevel * level
```

Or fix the battle engine to match the party screen (treat `baseDamage` as level 1):
```diff
- val skillBase = skill.baseDamage + skill.damagePerLevel * combatant.level
+ val skillBase = skill.baseDamage + skill.damagePerLevel * (combatant.level - 1)
```

**Recommendation**: Fix the party screen to use `* level` (the battle engine formula is more intuitive: `baseDamage` at level 0 + `damagePerLevel * level` = value at level N). This also matches how `toCombatantState` scales stats with `1 + (level - 1) * 0.15`.

## Files to modify

| File | Changes |
|---|---|
| `ActionTray.kt` | Add `heroLevel` param to `SkillCard`; pass `currentHero.level` from `HandOfCards` caller |
| `PartyScreen.kt` | Change `* (level - 1)` to `* level` in skill display computation |

## Dependencies

- None. Standalone fix.
