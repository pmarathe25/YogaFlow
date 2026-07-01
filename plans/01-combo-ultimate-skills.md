# Plan: Make Combo and Ultimate Skill Cards Always Visible

## Issues Addressed
- **Issue 1**: Combo skills are missing — the skill card should be visible if all required heroes are present.
- **Issue 9**: Ultimate skills are missing — the card should always be visible, grayed out when not usable.

---

## Changes Required

### 1. `BattleActions.kt` — `ActionPanel` composable

**Current behavior (combo):**
`availableCombos` requires every required hero to have `ultimateGauge >= 100`. The combo card is only shown when this condition is met, making it effectively invisible most of the time.

**Current behavior (ultimate):**
The ultimate card is only added to `allCards` when `currentHero.ultimateGauge >= 100`. Otherwise it's absent.

**Fix:**

- **Combo availability**: Remove the `ultimateGauge >= 100` gate from `availableCombos`. A combo should be considered "available" for display whenever all required heroes are alive (present). The 100-ult-gauge requirement should only gate execution, not visibility.

  Change the `availableCombos` `remember` block in `ActionPanel` to:
  ```kotlin
  val availableCombos = remember(heroes) {
      val aliveHeroNames = heroes
          .filter { it.currentHp > 0 && !it.isDead }
          .map { it.name }
          .toSet()
      DataLoader.combos.filter { combo ->
          currentHero.name in combo.requiredHeroes &&
          combo.requiredHeroes.all { name -> name in aliveHeroNames }
      }
  }
  ```

- **Ultimate visibility**: In `HandOfCards`, always add the ultimate card to `allCards`, regardless of gauge level:
  ```kotlin
  val allCards: List<Any> = buildList {
      currentHero.skills.forEach { add(it) }
      add(currentHero.ultimate)  // always visible
      availableCombos.forEach { add(it) }
  }
  ```

### 2. `BattleActions.kt` — `SkillCard` composable

- When `isUltimate` is true and `ultReady` is false, the card is already "grayed out" via `isOnCooldown`-like styling. Verify that the disabled visual (gray background, dimmed alpha) applies correctly when `ultReady = false`.
- The `canUse` check already handles this: `val canUse = if (isUltimate) ultReady else !isOnCooldown`.

### 3. `BattleScreen.kt` — Combo availability check

- `updateComboAvailability` in `GameViewModel.kt` already checks only for alive heroes (no ult gauge requirement). This function is used elsewhere but does not affect UI card visibility. No change needed there.

### 4. Combo execution validation

- When the user taps a combo card to execute it, `executeCombo` and `executeComboById` in `GameViewModel.kt` should verify that all participants still have `ultimateGauge >= 100` before executing. This prevents executing an unready combo that appears visible but shouldn't actually fire.

  In `GameViewModel.executeCombo()`:
  ```kotlin
  // Before executing, check ult gauges
  if (participants.any { it.ultimateGauge < 100 }) return
  ```
