# Plan 67: Fix Combo Skills — Show in All Participating Heroes + Round-Based Gating

## Problem
Two issues with combo skill cards in battle:

### 1. Combos only shown in the current hero's hand
`BattleScene.kt:216-218` filters combos to only those where `currentHeroId in combo.requiredHeroes`. This means combo cards appear only in the hand of one participating hero. The user wants combo cards shown in ALL participating heroes' hands so any of them can trigger the combo.

### 2. No round-based gating — combos usable even if a participant already acted
There is no check preventing a combo from being used if one of the required heroes has already taken their turn this round. A combo requires all participants to act simultaneously, so if any participant has already acted this round, the combo should be blocked.

## Fix

### Fix 1: Show combos in all participating heroes
Remove the `currentHeroId in combo.requiredHeroes` filter from the `availableCombos` computation in `BattleScene.kt:210-221`. Every hero's action tray should show all combos for which all required heroes are alive:

```kotlin
val availableCombos = remember(state.heroes) {
    if (currentHero == null) emptyList()
    else {
        val aliveHeroes = state.heroes.filter { it.hp > 0 && !it.isDefeated }
        val aliveHeroIds = aliveHeroes.mapNotNull { it.id.toIntOrNull() }.toSet()
        DataLoader.combos.filter { combo ->
            combo.requiredHeroes.all { id -> id in aliveHeroIds }
        }
    }
}
```

This removes `currentHeroId in combo.requiredHeroes` while keeping the alive-check.

### Fix 2: Gate combo usage on round position
In `HandOfCards` (`ActionTray.kt`), when building the card list and checking usability, determine whether any required hero has already acted this round by comparing their turn order index against `currentTurnIndex`.

**Step 1: Pass turn order info to `ActionTray`**
Add two new parameters to `ActionTray`:
- `turnOrder: List<BattleActor>`
- `currentTurnIndex: Int`

Pass these from `BattleScene`:
```kotlin
ActionTray(
    currentHero = currentHero,
    turnOrder = state.turnOrder,
    currentTurnIndex = state.currentTurnIndex,
    ...
)
```

**Step 2: Determine which hero IDs have already acted**
In `HandOfCards`, compute a set of "already acted" hero IDs:
```kotlin
val actedHeroIds = remember(turnOrder, currentTurnIndex) {
    turnOrder
        .filterIndexed { idx, _ -> idx < currentTurnIndex }
        .mapNotNull { it.id.toIntOrNull() }
        .toSet()
}
```

**Step 3: Grey out / disable combos when any participant has acted**
In `ComboCard`, add a `disabled: Boolean` parameter. When disabled, grey out the card and block interaction.

In `HandOfCards`, when building cards:
```kotlin
is ComboSkill -> {
    val anyActed = item.requiredHeroes.any { it in actedHeroIds }
    ComboCard(
        combo = item,
        disabled = anyActed,
        ...
    )
}
```

In `ComboCard`, when disabled:
- Set `alpha = 0.5f`
- Skip the glow animation
- Use grey border instead of purple

**Step 4: Block tap and drag on disabled combo cards**
In the `dragMod` and `tapMod` pointer input handlers, add an early return for disabled combos. In `detectTapGestures`:
```kotlin
if (item is ComboSkill) {
    val anyActed = item.requiredHeroes.any { it in actedHeroIds }
    if (anyActed) return@detectTapGestures
}
```

### Fix 3: Add "Skip Turn" button
The user needs a way to end the current hero's turn without using a skill. This is useful when no skill is needed (e.g. all skills on cooldown, or the player wants to save skills for later).

**Step 1: Add `skipTurn` function to `GameViewModel`**
The existing `TurnManager.defend()` method already handles this by giving the hero a small shield + gauge boost and advancing the turn. Expose it through the ViewModel:

```kotlin
// In GameViewModel.kt:
fun skipTurn(heroId: String) {
    val state = _battleState.value ?: return
    if (state.phase != PLAYER_TURN || _isProcessingTurn.value) return
    val hero = state.heroes.find { it.id == heroId && !it.isDefeated } ?: return

    viewModelScope.launch {
        _isProcessingTurn.value = true
        _battleState.value = state.copy(pendingSkill = null)

        val result = turnManager.defend(_battleState.value ?: return@launch, heroId)
        _battleState.value = updateComboAvailability(result.newState)
        result.logMessages.forEach { addBattleLog(it) }

        delay(400)
        advanceToNextTurn()
        _isProcessingTurn.value = false
    }
}
```

**Step 2: Pass `onSkipTurn` callback through `ActionTray`**
Add an `onSkipTurn: (() -> Unit)?` parameter to `ActionTray`. Pass it from `BattleScene`:
```kotlin
ActionTray(
    currentHero = currentHero,
    ...
    onSkipTurn = { viewModel.skipTurn(state.currentActorId) },
    ...
)
```

**Step 3: Render "Skip Turn" button in `ActionTray`**
Add the button in `ActionTray.kt` near the targeting overlay, visible during the current hero's turn when not targeting:

```kotlin
// In ActionTray.kt composable, before HandOfCards:
if (!isTargeting && onSkipTurn != null) {
    TextButton(
        onClick = onSkipTurn,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 4.dp)
    ) {
        Text("SKIP TURN", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
```

Position: bottom-right corner of the ActionTray, above the hand, so it's accessible but not in the way of card interaction.

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/BattleScene.kt` — lines 210-221: remove `currentHeroId in combo.requiredHeroes` filter; pass `state.turnOrder` and `state.currentTurnIndex` to `ActionTray`; pass `onSkipTurn`
- `app/src/main/java/com/example/game/ui/components/ActionTray.kt` — add `turnOrder`/`currentTurnIndex` params to `ActionTray` and `HandOfCards`; compute `actedHeroIds`; add `disabled` param to `ComboCard`; grey out + block interaction when disabled; add `onSkipTurn` param; render "SKIP TURN" button
- `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt` — add `skipTurn(heroId)` function
- `com.example.game.model` — no change needed

## Dependencies
- None
