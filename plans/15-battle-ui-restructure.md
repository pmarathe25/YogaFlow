# Plan 15: Battle UI Composables Restructure

## Summary
Restructure the battle UI into the cleaner component set described in the vision: `BattleScene`, `CombatantSprite`, `ActionTray`, `BattleHud`, `BattleEffectsLayer`. Extract logic from the monolithic `BattleScreen.kt` and simplify the composable tree.

## Component Breakdown

### New files
- **`BattleScene.kt`** — Replaces `BattleScreen.kt`. Top-level battle composable. Owns the layout: full-screen background, monster zone (upper half), hero zone (lower half), action tray (bottom), and effects overlay.
- **`CombatantSprite.kt`** — Extracted from `BattleCanvas.kt`. Renamed `HeroSprite`/`MonsterSprite` to unified `CombatantSprite` composable that renders either side based on a `isMonster` flag. Keeps Canvas silhouette drawing functions.
- **`ActionTray.kt`** — Extracted from `BattleActions.kt`. Cleaner, more compact action bar with glossy skill cards, cooldown indicators, and gauge display. Removes the "hand of cards" fan layout in favor of a horizontal scrolling tray.
- **`BattleHud.kt`** — Rename existing `BattleHUD.kt` content. Keep `FloatingHUD`, `HeroHUD`, `MonsterHUD`, `StatusIcon`, `elementToColor()`.
- **`BattleEffectsLayer.kt`** — Extracted from `BattleEffects.kt`. Overlay composable that handles damage numbers, particles, screen shake, screen tint/flash, and cut-in banners.

### Removed/modified files
- **`BattleScreen.kt`** — Delete after extracting components.
- **`BattleActions.kt`** — Delete after extracting `ActionTray`.
- **`BattleCanvas.kt`** — Keep as sprite-drawing utility but rename `HeroSprite`/`MonsterSprite` composables to point to `CombatantSprite`.
- **`BattleAnimations.kt`** — Inline turn banner into `BattleScene`; keep `rememberSpriteAnimations` but move to `BattleScene`.
- **`BattleEffects.kt`** — Rename/move content to `BattleEffectsLayer.kt`.

### `GameApp.kt`
- Change import from `BattleScreen` to `BattleScene`.

## Detailed Changes

### `BattleScene.kt`
```
- Full-screen painted-style battlefield background (delegate to existing `BattleBackground` in BattleCanvas.kt)
- Monster displayed in upper 55% with large `CombatantSprite`, `MonsterHUD`
- Heroes displayed in lower 45% with small `CombatantSprite`, `HeroHUD`, turn highlight
- `ActionTray` fixed at bottom
- Turn indicator banner at top (from BattleAnimations)
- `BattleEffectsLayer` as overlay
- Targeting overlay when `pendingSkill` is set
- BackHandler for exit dialog
```

### `ActionTray.kt`
```
- Horizontal scrollable row of action cards
- Each card shows: skill icon (remove emoji, use Canvas element icon), name, cooldown/gauge state
- Ultimate card has golden glow when ready
- Combo cards appear as purple variants when available
- Compact: cards ~120dp wide x 160dp tall
- No "hand of cards" fan layout or card flip animation
- On card tap: if skill needs targeting, enter targeting mode; otherwise immediately execute
- Targeting mode overlay with hero/monster clickable highlights
```

### `CombatantSprite.kt`
```
- Unified composable: CombatantSprite(isMonster, ...)
- Renders same Canvas silhouettes as current HeroSprite/MonsterSprite
- Supports: idle bob, attack lunge, hit recoil, dying fade, dead gray
- Elemental glow aura
- Low-HP pulse effect (red glow at <30% HP)
- Turn highlight pulse ring
```

### `BattleHud.kt`
- Keep existing implementation but refactor to read `CombatantState` instead of `HeroInstance`/`MonsterInstance`.
- HP bar, shield bar, ultimate gauge, status icons.

### `BattleEffectsLayer.kt`
- Damage numbers (floating up and fading)
- Heal numbers (green float up)
- Shield break sparks (purple particle burst)
- Status application icons
- Elemental burst particles on skill use
- Screen shake on heavy hits
- Screen tint/flash by element
- Ultimate/combo cut-in banner (short banner with hero silhouettes)

## Verification
- `./gradlew assembleDebug` must compile.
- Manual: battle should render with new composable structure, all interactions work.
- Manual: verify on small phone screen (no overlapping elements).
