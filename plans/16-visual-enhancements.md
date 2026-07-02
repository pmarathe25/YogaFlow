# Plan 16: Visual Enhancement — Arena & Effects

## Summary
Upgrade the battle visuals to feel like a compact 2D RPG arena. Enhance the `BattleBackground` with per-encounter/element painted-style backgrounds. Add event-driven battle effects: camera shake, screen flash, cut-in banners for ultimates/combos, elemental burst particles.

## Changes

### 1. `BattleScene.kt` / `BattleBackground` — Arena background
- Already has a painted-style background with sky, ground, mountains, clouds, stars, pillars, runes.
- Enhance per-element backgrounds:
  - **Fire**: Lava glow floor cracks, ember particles
  - **Water**: Raining effect, rippling water floor
  - **Earth**: Rocky terrain with grass tufts, vine pillars
  - **Air**: Cloud platforms, wind swirl particles
  - **Light**: Radiant beams from above, golden fog
  - **Dark/Shadow**: Purple mist, floating void orbs
  - **Electric**: Crackling lightning bolts in background
- Add mid-ground layer with encounter-specific ruins/platforms.
- Keep the parallax scrolling for depth.

### 2. `BattleEffectsLayer.kt` — Enhanced effects
- **Damage numbers**: Better font, shadow outline, scale-up on appear.
- **Shield break**: Purple sparks burst, shield shatter icon.
- **Heal numbers**: Green with a small heart icon, gentle float up.
- **Elemental burst**: When a skill is used, emit element-colored particle burst from the caster toward the target.
- **Screen shake**: `ScreenShake.kt` already exists. Trigger on:
  - Any skill with `baseDamage > 200`
  - Any ultimate
  - Any combo
  - Monster phase triggers
  - Boss attacks
- **Screen tint/flash**: Brief full-screen color overlay matching skill element on use (100ms duration).
- **Cut-in banner**: For ultimates and combos, show a full-width banner:
  - Ultimate: "`HERO_NAME` unleashes `ULT_NAME`!" with hero silhouette
  - Combo: "`COMBO_NAME`!" with participant silhouettes
  - Banner slides in from right, holds 1s, slides out.

### 3. `CombatantSprite.kt` — Sprite polish
- **Idle bob**: Already implemented. Keep.
- **Attack lunge**: Sprite moves toward target, snaps back. Already implemented. Keep.
- **Hit reaction**: Flash red, brief knockback. Already implemented. Keep.
- **Low-HP pulse**: When `hp < 30% maxHp`, add a pulsing red glow around the sprite.
- **Defeated**: Sprite fades out, falls down. Already implemented. Keep.
- **Turn highlight**: Glowing ring/outline around the current actor's sprite.
- **Elemental aura**: Colored glow around sprite matching element (varies intensity with idle bob).

### 4. `ActionTray.kt` — Polish
- Glossy gradient backgrounds on skill cards.
- Element-colored accent borders.
- Cooldown shown as a sweeping radial overlay (like a clock) instead of just a number badge.
- Gauge shown as a mini progress bar at bottom of each card.
- Combo cards have a purple shimmer border animation.

### 5. `ParticleEngine.kt` — Enhancements
- Already functional with emitters, gravity, drag, blend modes.
- Add element-specific particle colors and behaviors in `emitterConfigForElement()`.
- Add burst mode (all particles emit in a single frame from a point).
- Add trail mode (particles leave fading trail).

### 6. Remove emoji usage
- Replace emoji in `getSkillIcon()` and combo cards with Canvas-drawn element icons or simple geometric shapes.
- Replace emoji in HUD status icons with colored circles with letter indicators (e.g., "ATK↑", "SPD↓", "BRN", "STN").

## Verification
- Manual: battle looks visually richer with element-themed backgrounds.
- Manual: ultimate/combo cut-in banners appear correctly.
- Manual: screen shake on heavy hits.
- Manual: low-HP pulse visible on heroes below 30%.
- Run `./gradlew assembleDebug` to verify compilation.
