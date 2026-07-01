# Plan: Only Show Targeting Reticles on Valid Targets

## Issue Addressed
- **Issue 5**: Skills which don't allow targeting enemies should not show targeting reticles on enemies.

---

## Current Behavior

In `BattleScreen.kt`, targeting reticles (TargetCircle) are shown on monsters when:

```kotlin
val canTarget = state.pendingSkill?.let {
    it.targetType == TargetType.SINGLE_ENEMY || it.targetType == TargetType.ALL_ENEMIES || it.targetType == TargetType.ALL
} ?: false
```

And on heroes when:

```kotlin
val canTarget = state.pendingSkill?.let {
    it.targetType == TargetType.SINGLE_ALLY || it.targetType == TargetType.ALL_ALLIES || it.targetType == TargetType.ALL || it.targetType == TargetType.SELF
} ?: false
```

The problem occurs when `pendingSkill.targetType == TargetType.ALL` — it shows reticles on **both** heroes and monsters, which is correct.

But when `pendingSkill.targetType == TargetType.SELF`, it shows a reticle on **all** heroes (including other heroes) due to the `ALL_ALLIES` and `ALL` conditions. Only the caster should be targetable for SELF skills.

Similarly, for `SINGLE_ALLY`, only other allies (not the caster) should be targetable.

The most common bug: a skill like "Heal" (SINGLE_ALLY) correctly shows reticles on allies, but a skill with SELF target type also shows reticles on all allies. Additionally, skills like buffs that target ALL_ALLIES correctly show reticles on all heroes, which is fine.

However, the issue says "Skills which don't allow targeting enemies should not show targeting reticles on enemies." So the concern is specifically about enemy reticles showing up when they shouldn't.

---

## Changes Required

### 1. `BattleScreen.kt` — Monster targeting logic

Refine the `canTarget` check for monsters:

```kotlin
val canTarget = state.pendingSkill?.let { skill ->
    skill.targetType == TargetType.SINGLE_ENEMY || 
    skill.targetType == TargetType.ALL_ENEMIES || 
    skill.targetType == TargetType.ALL
} ?: false
```

This already correctly excludes SELF, SINGLE_ALLY, and ALL_ALLIES. If the issue persists, verify that `pendingSkill` is correctly set to `null` when it shouldn't be targetable, and that skills with `SINGLE_ENEMY` target type are auto-resolved before target selection.

In `GameViewModel.executeSkill()`:
```kotlin
when (skill.targetType) {
    SINGLE_ENEMY -> {
        val monsterId = state.aliveMonsters.firstOrNull()?.monsterId
        if (monsterId != null) {
            return executeSkill(heroId, skill, listOf(monsterId))
        }
    }
    SINGLE_ALLY, SELF -> {
        state.pendingSkill = skill
        emitBattleState(state)
        return
    }
    else -> {} // ALL_ENEMIES, ALL_ALLIES, ALL are resolved automatically
}
```

This shows that `SINGLE_ENEMY` skills auto-target and never set `pendingSkill`. `SINGLE_ALLY` and `SELF` do set `pendingSkill`. `ALL_ENEMIES`, `ALL_ALLIES`, `ALL` fall through and are auto-resolved in `resolveTargets`.

For `ALL_ENEMIES` and `ALL`, the target selection is done automatically and `pendingSkill` is not set. So these should never show targeting reticles. This looks correct.

**Potential issue**: `ALL` and `ALL_ENEMIES` in `executeSkill` fall through to `else -> {}` and never set `pendingSkill`. So they should not trigger targeting UI. If the bug is that they DO show targeting reticles, the issue is that `pendingSkill` is incorrectly set somewhere. Verify this.

### 2. `BattleScreen.kt` — Hero targeting logic

Refine the `canTarget` check for heroes to exclude skills that shouldn't target enemies or allies:

```kotlin
val canTarget = state.pendingSkill?.let { skill ->
    when (skill.targetType) {
        TargetType.SINGLE_ALLY -> hero.heroId != state.currentActorId // Caster cannot target self
        TargetType.ALL_ALLIES, TargetType.ALL -> true
        TargetType.SELF -> hero.heroId == state.currentActorId // Only caster
        else -> false
    }
} ?: false
```

### 3. Additional edge case: No reticles for auto-resolved skills

Confirm that skills with target type `ALL_ENEMIES`, `ALL_ALLIES`, `ALL` never set `pendingSkill`. In `executeSkill`:
- `ALL_ENEMIES` → falls to `else -> {}` → no pendingSkill → correct
- `ALL_ALLIES` → falls to `else -> {}` → no pendingSkill → correct
- `ALL` → falls to `else -> {}` → no pendingSkill → correct
- `SELF` → sets pendingSkill → correct (needs targeting but only for self)
- `SINGLE_ALLY` → sets pendingSkill → correct (needs player to choose which ally)
- `SINGLE_ENEMY` → auto-targets → no pendingSkill → correct

This looks correct for the auto-resolution cases. The fix is primarily in the hero targeting logic (#2 above).
