# Plan 37: Play Full Defeat Animation Before Battle End

## Problem

When a skill reduces the last monster's HP to 0, the battle transitions to the result screen before the skill animation and defeat animation finish playing. The `MonsterDown` event is emitted instantly at the same time as damage, and the current timing has a 1000ms delay in `executeSkill` + another 1000ms in `advanceToNextTurn`, but these delays run concurrently with any animation work rather than waiting for it.

## Root cause

**`BattleReducer.kt`** — `applyOutcome()` (around line 788) sets the monster's HP to 0 and emits `MonsterDown` in the same state update as the damage application. The `useSkill()` function then calls `terminalResult()` which immediately detects victory.

**`BattleAnimations.kt:61-118`** — sprite animations are driven by a `LaunchedEffect` keyed on `eventLog.size`. The `MonsterDown` animation (line 110) transitions to `DYING` state instantly with no delay.

**`GameViewModel.kt:245-256`** — `executeSkill()` delays 1000ms then calls `advanceToNextTurn()`. Another 1000ms delay before switching to `BATTLE_RESULT`. But these are fixed delays, not animation-aware.

## Fix

Instead of fixed delays, wait for the actual defeat animation to complete before transitioning to the result screen.

### 1. Add a `defeatAnimationPlaying` state

**`GameViewModel.kt`** — add a state flow to track whether the defeat animation is still playing:

```kotlin
private val _defeatAnimationPlaying = MutableStateFlow(false)
val defeatAnimationPlaying: StateFlow<Boolean> = _defeatAnimationPlaying.asStateFlow()
```

Set it to `true` when a monster dies and `false` when the animation duration has elapsed.

### 2. Delay `MonsterDown` in BattleReducer

Modify `applyOutcome()` in `BattleReducer.kt` to not immediately remove dead monsters. Instead, add a `dyingMonsterIds` field to `BattleState`:

```kotlin
data class BattleState(
    ...
    val dyingMonsterIds: Set<String> = emptySet(),  // monsters in defeat animation
)
```

In `applyOutcome()`, instead of immediately setting HP to 0 and removing from `aliveMonsters`, move the monster to `dyingMonsterIds`:

```kotlin
// When HP <= 0, add to dyingMonsterIds instead of immediately removing
newMonsters = newMonsters.map { m ->
    if (m.id == target.id && m.hp <= 0)
        m.copy(hp = 0, spriteState = SpriteState.DYING)
    else m
}
```

`terminalResult()` should not count `dyingMonsterIds` as defeated yet:

```kotlin
private fun terminalResult(...): TurnResult? {
    return when {
        state.aliveMonsters.none { it.id !in state.dyingMonsterIds } -> { /* not yet victory */ }
        ...
    }
}
```

**Alternative (simpler):** Instead of modifying the state machine, add a delay in `GameViewModel` before the victory check that matches the animation duration:

```kotlin
// In executeSkill(), after applying outcome:
val hadFatal = result.events.any { it is BattleEvent.MonsterDown }  // or check HP
if (hadFatal && result.victory) {
    delay(1500)  // wait for defeat animation (300ms skill + 200ms hit + 1000ms death)
}
```

This is simpler but less precise.

### 3. Extend defeat animation duration in BattleAnimations

**`BattleAnimations.kt:110`** — currently `MonsterDown` sets state to `DYING` with no delay. Add a meaningful animation:

```kotlin
is BattleEvent.MonsterDown -> {
    setSpriteState(targetId, SpriteState.DYING)
    delay(1200)  // play death animation for 1.2 seconds
    // After delay, allow removal / result transition
}
```

The `DYING` state could animate a fade-out or collapse effect in `CombatantSprite.kt`.

### 4. Sequence: trigger → animate → result

The combined flow after a killing blow:

1. `executeSkill` computes outcome → monster drops to 0 HP → `MonsterDown` event emitted
2. `LaunchedEffect` in `BattleAnimations` picks up `MonsterDown` → sets `DYING` → delays 1200ms
3. ViewModel's `executeSkill` delay (1000ms) + animation delay run concurrently
4. `advanceToNextTurn` checks victory → if monsters still in death anim, delay longer
5. Only transition to `BATTLE_RESULT` after death animation completes + a brief pause

### Simpler approach (recommended)

Instead of modifying the state machine (risky), just sequence the delays properly:

1. Keep the existing 1000ms delay after skill execution (for hit flash etc.)
2. In `advanceToNextTurn`, when victory is detected, add a `delay(1500)` before switching to `BATTLE_RESULT` (replacing the existing 1000ms). The total wait becomes ~2500ms from the killing blow, which gives enough time for:
   - 300ms skill animation (hero attacking)
   - 200ms hit reaction (monster hit flash)
   - ~1500ms death animation (monster fading/collapsing)
   - 500ms buffer

```kotlin
// GameViewModel.kt line 315-318
if (result.victory) {
    delay(2500)  // was 1000 — wait for full death animation
    _currentScreen.value = GameScreen.BATTLE_RESULT
    onBattleWon()
    return
}
```

And increase `executeSkill`'s delay to let the hit animation play before the death sequence:

```kotlin
// GameViewModel.kt line 253
delay(1300)  // was 1000 — let hit animation finish
```

## Files to modify

| File | Changes |
|---|---|
| `GameViewModel.kt` | Increase victory/defeat delay in `advanceToNextTurn` from 1000ms to 2500ms; increase `executeSkill`/`executeUltimate`/`executeCombo` delays slightly |
| `BattleAnimations.kt` | Add `delay()` after `MonsterDown` events to play death animation |
| `CombatantSprite.kt` | (Optional) Add a fade-out or collapse animation for `DYING` state |

## Dependencies

- None. Standalone timing fix.
