# Plan: Battle Animation Improvements

## Issue Addressed
- **Issue 8**: Monster intro animation is missing (immediately starts monster's turn). Attack animations are incorrect — the attacker should move toward the target, not randomly. Movement should be further (attacker gets very close to target before attacking).

---

## Part 1: Monster Introduction Animation

### Current behavior
`BattleScreen.kt` has a battle start animation sequence (lines 83-96):
```kotlin
LaunchedEffect(Unit) {
    delay(100)
    blackVisible = false
    delay(300)
    monsterVisible = true
    state.aliveHeroes.forEachIndexed { _, hero ->
        delay(100)
        heroVisibilities[hero.heroId] = true
    }
    delay(50)
    battleTextVisible = true
    delay(1500)
    battleTextVisible = false
}
```

After this animation, `startBattle` in `GameViewModel.kt` immediately checks if the first actor is a monster and starts its turn:

```kotlin
val firstActor = state.turnOrder.firstOrNull()
if (firstActor != null) {
    if (firstActor.isHero) {
        state.phase = PLAYER_TURN
        updateComboAvailability(state)
    } else {
        state.phase = ENEMY_TURN
        executeMonsterTurn(state, firstActor) // Starts immediately!
    }
}
```

The problem: The monster appears and immediately starts attacking, giving the player no time to see the monster.

### Changes Required

#### 1. `GameViewModel.kt` — `startBattle()` method

**Decision: Introduce a `BattlePhase.INTRO` phase** to explicitly gate the first action. This is more robust than a hardcoded delay because it works regardless of animation timing variations.

Add to `BattlePhase` enum in `BattleState.kt`:
```kotlin
enum class BattlePhase { INTRO, START_OF_BATTLE, PLAYER_TURN, ENEMY_TURN, VICTORY, DEFEAT, PHASE_TRIGGER }
```

In `GameViewModel.startBattle()`, set the phase to `INTRO` instead of immediately starting a turn:
```kotlin
val firstActor = state.turnOrder.firstOrNull()
if (firstActor != null) {
    state.phase = BattlePhase.INTRO
    state.currentActorId = firstActor.id
    // Don't start the first turn yet — BattleScreen will signal when ready
}
emitBattleState(state)
```

In `BattleScreen.kt`, after the intro animation completes (at the end of the existing `LaunchedEffect(Unit)`), call a new ViewModel method to transition from INTRO to the appropriate phase:
```kotlin
LaunchedEffect(Unit) {
    delay(100)
    blackVisible = false
    delay(300)
    monsterVisible = true
    // ... existing animation sequence ...
    delay(1500)
    battleTextVisible = false
    
    // Signal that intro is done
    viewModel.onIntroComplete()
}
```

In `GameViewModel`:
```kotlin
fun onIntroComplete() {
    val state = _battleState.value ?: return
    val firstActor = state.turnOrder.firstOrNull() ?: return
    if (firstActor.isHero) {
        state.phase = PLAYER_TURN
        updateComboAvailability(state)
    } else {
        state.phase = ENEMY_TURN
        viewModelScope.launch {
            // No delay needed — intro animation is already complete
            if (state.aliveMonsters.isNotEmpty()) {
                executeMonsterTurn(state, firstActor)
            }
        }
    }
    emitBattleState(state)
}
```

Also add a guard in the turn banner `LaunchedEffect(state.currentActorId)` to skip during INTRO phase:
```kotlin
LaunchedEffect(state.currentActorId) {
    if (state.phase == BattlePhase.INTRO) return@LaunchedEffect
    // ... existing banner logic ...
}
```

**Fallback approach** (if adding a new enum value is risky): Use `delay(3500)` instead of `delay(2500)` in a `viewModelScope.launch`. This is less reliable but simpler.

#### 2. `BattleScreen.kt` — Battle start animation

Enhance the existing animation to include a monster-specific intro (e.g., the monster appears with a dramatic effect, a name banner is shown):

```kotlin
LaunchedEffect(Unit) {
    delay(100)
    blackVisible = false   // fade from black
    delay(300)
    monsterVisible = true   // monster scales in
    delay(500)              // pause to show monster
    // Show monster name banner
    monsterNameVisible = true
    delay(1000)
    monsterNameVisible = false
    delay(200)
    // Then heroes arrive
    state.aliveHeroes.forEachIndexed { _, hero ->
        delay(100)
        heroVisibilities[hero.heroId] = true
    }
    delay(50)
    battleTextVisible = true
    delay(1500)
    battleTextVisible = false
}
```

**⚠️ Timeline coordination (for reference):** The intro animation takes ~2950ms for 5 heroes. With `BattlePhase.INTRO`, timing is non-critical since the phase explicitly gates execution.

---

## Part 2: Attack Movement Direction

### Current behavior

In `BattleAnimations.kt` (lines 70-82), when a hero attacks:

```kotlin
is BattleEvent.SkillUsed -> {
    val isAttack = event.skill.damageComponents.isNotEmpty() || event.skill.baseDamage > 0
    heroAnimStates[event.heroId] = SpriteAnimState(
        state = SpriteState.ATTACKING, stateTime = 0f,
        offsetX = if (isAttack) 40f else 0f,
        offsetY = if (!isAttack) -20f else 0f
    )
    delay(200)
    heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    
    if (isAttack) {
        monsterAnimState.value = SpriteAnimState(state = SpriteState.HIT, stateTime = 0f, offsetX = -20f)
        delay(150)
        monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    }
}
```

For monster attacks (lines 84-94):
```kotlin
is BattleEvent.MonsterTurn -> {
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.ATTACKING, stateTime = 0f, offsetY = 30f
    )
    delay(200)
    monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    event.targets.forEach { targetHeroId ->
        heroAnimStates[targetHeroId] = SpriteAnimState(state = SpriteState.HIT, stateTime = 0f, offsetY = 15f)
    }
    delay(200)
    event.targets.forEach { targetHeroId ->
        heroAnimStates[targetHeroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    }
}
```

Problems:
- Heroes attack by moving right (`offsetX = 40f`), regardless of target position (monster is typically on the right, so this is accidentally correct)
- Monsters attack by moving down (`offsetY = 30f`), which is arbitrary and doesn't move toward heroes
- Movement distance is small (40f for heroes, 30f for monsters)

### Changes Required

#### 1. `BattleAnimations.kt` — Hero attack movement

When a hero attacks the monster:
- Move the hero **rightward and slightly downward** toward the monster (positive X, small positive Y)
- Use a larger offset (e.g., `offsetX = 80f` to get closer to the monster in the center)
- The monster reacts with a HIT animation (larger backward offset, e.g., `offsetX = -30f`)

```kotlin
is BattleEvent.SkillUsed -> {
    val isAttack = event.skill.damageComponents.isNotEmpty() || event.skill.baseDamage > 0
    if (isAttack) {
        // Hero moves toward monster (right, forward)
        heroAnimStates[event.heroId] = SpriteAnimState(
            state = SpriteState.ATTACKING, stateTime = 0f,
            offsetX = 80f, offsetY = -10f  // move right (toward monster) and slightly up
        )
        delay(300)  // longer movement animation
        heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
        
        // Monster reaction
        monsterAnimState.value = SpriteAnimState(
            state = SpriteState.HIT, stateTime = 0f,
            offsetX = -25f, offsetY = 5f  // knocked back
        )
        delay(200)
        monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    } else {
        // Healing/buff animation (stay in place, move up slightly)
        heroAnimStates[event.heroId] = SpriteAnimState(
            state = SpriteState.ATTACKING, stateTime = 0f,
            offsetX = 0f, offsetY = -30f  // rise up
        )
        delay(300)
        heroAnimStates[event.heroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    }
}
```

#### 2. `BattleAnimations.kt` — Monster attack movement

When a monster attacks a hero:
- Move the monster **leftward and slightly forward** toward the party (negative X, small positive Y)
- Use a larger offset (e.g., `offsetX = -80f`)

```kotlin
is BattleEvent.MonsterTurn -> {
    // Monster moves toward heroes (left, forward)
    monsterAnimState.value = SpriteAnimState(
        state = SpriteState.ATTACKING, stateTime = 0f,
        offsetX = -80f, offsetY = 10f  // move left (toward heroes)
    )
    delay(300)
    monsterAnimState.value = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    
    // Targeted heroes react (knocked back)
    event.targets.forEach { targetHeroId ->
        heroAnimStates[targetHeroId] = SpriteAnimState(
            state = SpriteState.HIT, stateTime = 0f,
            offsetX = -15f, offsetY = 10f
        )
    }
    delay(200)
    event.targets.forEach { targetHeroId ->
        heroAnimStates[targetHeroId] = SpriteAnimState(state = SpriteState.IDLE, stateTime = 0f)
    }
}
```

#### 3. `BattleCanvas.kt` — Smooth animation transitions

The `HeroSprite` and `MonsterSprite` composables already use `spring()` for position animation. The increased offsets should smoothly animate to the new positions and back.

Verify that the `spring()` stiffness/damping values allow the larger offsets to animate smoothly:
```kotlin
val smoothOffsetX by animateFloatAsState(
    targetValue = animState.offsetX,
    animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f)
)
```

Consider reducing stiffness slightly for attack movements to make them appear more natural (slower, weightier).

#### 4. Animation timing coordination

Ensure the timing in `GameViewModel` (`delay(1000)` after skill execution, `delay(1200)` for turn banner) allows enough time for the longer attack animations (300ms movement + 200ms hit reaction + return).
