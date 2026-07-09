# Plan 69: Fix Combo Animation — Heroes Not Animating When Combo Fires

## Problem
When a combo skill is used, `BattleEvent.ComboUsed` is posted to the event log (`BattleReducer.kt:199`), and the `BattleEffectsLayer` handles it for the cut-in text ("COMBO!") and particle burst (`BattleEffectsLayer.kt:195-210`). However, the hero sprite animations in `rememberSpriteAnimations` (`BattleAnimations.kt:55-167`) do NOT handle `BattleEvent.ComboUsed`. The `when (event)` block at line 70 only handles `SkillUsed`, `MonsterTurn`, `MonsterDown`, and `HeroDown`. This means:

1. Combo participants never perform the lunge/attack animation
2. The target (monster) never receives the hit/recoil animation
3. The battle feels lifeless when a combo is executed — only the text overlay and particles appear

## Fix

### Add `is BattleEvent.ComboUsed ->` handler in `BattleAnimations.kt`
Insert a new branch in the `when` block (line 70, after `SkillUsed`):

```kotlin
is BattleEvent.ComboUsed -> {
    // All participants lunge toward the monster
    event.participants.forEach { heroId ->
        val attackerPos = heroPositions[heroId] ?: return@forEach
        val targetPos = monsterPos
        val dx = targetPos.x - attackerPos.x
        val dy = targetPos.y - attackerPos.y
        val distance = sqrt(dx * dx + dy * dy)
        val normalizedDx = dx / distance
        val normalizedDy = dy / distance
        val lungeDistance = 80f
        heroAnimStates[heroId] = SpriteAnimState(
            state = SpriteState.ATTACKING, stateTime = 0f,
            offsetX = normalizedDx * lungeDistance,
            offsetY = normalizedDy * lungeDistance
        )
    }
    delay(300)
    // Reset all participants
    event.participants.forEach { heroId ->
        heroAnimStates[heroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    }

    // Monster hit recoil
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.HIT, stateTime = 0f,
        offsetX = 0f, offsetY = 0f  // No directional knockback for combo (combined force)
    )
    delay(200)
    monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
}
```

Key design decisions:
- All participants lunge simultaneously (combo is a combined action)
- Monster recoils with no directional bias (the combo force is from multiple directions, so no single knockback direction makes sense)
- Lunge distance 80f matches the `SkillUsed` attack animation (line 81)
- Delay 300ms for lunge hold and 200ms for hit recoil matches existing animation timing

### Optional: Enhanced combo visuals
For extra impact, stagger the lunge animation slightly so participants don't move in perfect lockstep:
```kotlin
event.participants.forEachIndexed { i, heroId ->
    launch {
        delay(i * 50L) // Stagger by 50ms per participant
        val attackerPos = heroPositions[heroId] ?: return@launch
        // ... lunge logic ...
    }
}
```

This requires wrapping the body in a `coroutineScope { }` or using separate `launch` blocks because we're already inside a `LaunchedEffect`.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/BattleAnimations.kt` — add `is BattleEvent.ComboUsed ->` branch after line 105 (after `SkillUsed` handler)

## Dependencies
- Plan 69 must be implemented before or alongside any other battle animation work
