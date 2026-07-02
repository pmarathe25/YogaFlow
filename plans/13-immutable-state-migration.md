# Plan 13: Adopt Immutable CombatantState & GameProgress

## Summary
Replace mutable `HeroInstance`/`MonsterInstance` with the already-defined (but unused) `CombatantState` throughout `BattleState`. Replace the persistence model `GameSaveData` with `GameProgress`. Remove `BattleSaveData` and active-battle save/restore. All state becomes immutable — only the reducer produces new state.

## Changes

### 1. `BattleState.kt` — Make CombatantState the primary combatant type
- Remove `HeroInstance`/`MonsterInstance` references from `BattleState`.
- Change `BattleState.heroes: List<CombatantState>` and `BattleState.monsters: List<CombatantState>`.
- Add fields to `CombatantState` needed by the battle system that are currently only on `HeroInstance`/`MonsterInstance`:
  - `skills: List<Skill>`, `ultimate: Skill`, `level: Int`, `maxHp: Int`, `atk: Int`, `speed: Int`
  - `phases: List<MonsterPhase>`, `activePhase: Int`, `aiBehavior: AIBehavior`, `extraActionsThisRound: Int`, `turnsSinceLastSpecial: Int`
  - `englishName: String?`, `specialAttack: Skill?`, `isBoss: Boolean`
- Move convenience properties (`aliveHeroes`, `aliveMonsters`, `isBattleOver`, `getActor`, `getStatusesForTarget`, etc.) to use `CombatantState`.
- Remove `BattleSaveData`, `HeroSaveData`, `MonsterSaveData`, `StatusSaveData` (no more active battle save/restore).
- Keep `BattleActor` for turn order (it is lightweight and already immutable).

### 2. `GameProgress.kt` (formerly part of `BattleState.kt`)
- Promote `GameProgress` to its own file (`GameProgress.kt`).
- Add missing fields from `GameSaveData`:
  - `consumables: Map<String, Int>`, `equippedSkins: Map<String, String>`, `unlockedSkinIds: Set<String>`
  - `totalPlayTimeMs: Long`, `highestComboHits: Int`, `fastestBattleTurns: Int`
- Keep `version: Int = 2`.
- Rename `defeatedEncounterIds` -> `defeatedMonsterIds` for backward compat with the rest of the codebase.

### 3. `GameSaveManager.kt` — Switch to GameProgress
- Replace all uses of `GameSaveData` with `GameProgress`.
- Update `loadGame()`, `saveGame()`, `loadDefaultSave()`, `loadLegacySave()`, `normalized()` accordingly.
- Keep the SharedPreferences `KEY_PROGRESS_BLOB = "progress_blob_v2"` key.
- Remove `HeroSaveData` references (use a simplified inline serialization format inside progress blob).
- Remove `normalizeKnownHeroBoundId()` — IDs are now normalized at load time only.

### 4. `GameViewModel.kt` — Update state references
- Change `_saveData: StateFlow<GameSaveData>` to `_saveData: StateFlow<GameProgress>`.
- Change `_party: StateFlow<List<HeroInstance>>` to derive from `_saveData.value.party`.
- Update `syncWithMainApp()`, `restoreParty()`, `startBattle()`, `onBattleWon()`, `resetAllProgress()`, `purchaseItem()`, `equipItem()`, `levelUpHero()`, `purchaseHero()` to use `GameProgress`.
- In `startBattle()`, create `List<CombatantState>` from the party `GameProgress` data instead of mutating `HeroInstance` objects.
- All mutating `var` fields on hero/monster instances (`currentHp`, `shield`, `ultimateGauge`, `isDead`) are gone — the reducer owns all combatant state.

### 5. `Hero.kt` / `Monster.kt` — Keep definitions, remove Instance classes
- Keep `Hero` and `Monster` as immutable definition data classes.
- Remove `HeroInstance` and `MonsterInstance` — they are replaced by `CombatantState`.
- Remove `Hero.createInstance()` and `Monster.createInstance()`.
- Move `HeroRole`, `MonsterPhase`, `PhaseTrigger`, `AIBehavior`, `TargetStrategy`, `DifficultyTier` to their own files or keep as-is.

### 6. Update all imports and call sites
- `BattleReducer.kt`, `TurnManager.kt`, `BattleEngine.kt`: change all `HeroInstance`/`MonsterInstance` parameters to `CombatantState`.
- UI files (`BattleScreen.kt`, `BattleHUD.kt`, `BattleCanvas.kt`, `BattleActions.kt`, `BattleAnimations.kt`, `BattleEffects.kt`): update to read `CombatantState` fields.
- `DataLoader.kt`: keep as-is (provides definitions, no changes needed).
- `MonsterRoadSelection.kt`: uses `Monster` definitions, no change.
- `HubScreen.kt`, `PartyScreen.kt`, `ShopScreen.kt`, `TrophyScreen.kt`: update to use new `_saveData` type.

## Verification
- `./gradlew assembleDebug` must compile.
- Manual test: start a battle, use skills, ultimates, combos, verify no crashes.
- Manual test: save/load progress via hub.
