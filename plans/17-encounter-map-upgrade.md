# Plan 17: Encounter Map Upgrade — Path of Zen

## Summary
Redesign the `MonsterRoadSelection` (Path of Zen) as a proper game map with biome bands, boss gates, glowing current node, cleared medals, locked fog. Richer monster node art and difficulty markers.

## Changes

### 1. `MonsterRoadSelection.kt` — Map redesign
- Replace the dirt road with a winding path that has distinct biome bands:
  - Early encounters: Green forest biome (grass, trees)
  - Mid encounters: Desert/canyon biome (sand, rocks)
  - Late encounters: Dark/mountain biome (snow, dark sky)
  - Boss encounters: Volcanic/storm biome (lava, lightning)
- Biome color transitions based on encounter index.
- Remove the `Decorations` composable in favor of biome-specific decorations drawn on the canvas.

### 2. `MonsterNode.kt` — Node upgrades
- Replace the simple circle + emoji with richer node art:
  - Each node is a floating platform/island on the path.
  - Undefeated / unlocked nodes glow with the monster's element color.
  - Defeated nodes show a medal/star icon and are slightly grayed out.
  - Locked nodes are covered in fog/mist effect and show a lock icon.
  - Boss nodes are larger, with spikes/golden border, pulsing glow.
- Add difficulty tier visual indicator:
  - Easy: small, simple shape
  - Medium: medium, with spikes
  - Hard: large, with glowing runes
  - Boss: largest, with crown/ornate border
- Show monster element icon or mini element-colored orb on the node.

### 3. Path visual
- The path is a curved line connecting nodes, drawn with glowing trail for completed segments.
- Uncompleted segments are dark/faded.
- The current "active" node (first undefeated) has a pulsing arrow/pointer.
- Fog of war covers nodes beyond the current active node (dark overlay).

### 4. Map decorations
- Biome-specific decorations (trees in forest, cacti in desert, crystals in mountains).
- Animated elements (flowing water, floating embers, drifting clouds) based on biome.
- Stars/moon in sky area, varying by biome.

### 5. `HubScreen.kt`
- Update the entrance to the Path of Zen to show a preview of the map (first few nodes) or a portal icon.
- Keep existing "Enter the Path" button but make it feel more like a game menu option.

### 6. Remove emoji from map
- Replace `👾` (normal monster) and `💀` (boss) icons with Canvas-drawn silhouettes matching the actual monster shape from `drawMonsterShape()`.
- Replace `✓ DEFEATED` with a star/medal icon drawn on Canvas.

## Verification
- Manual: scroll through Path of Zen, verify biome transitions.
- Manual: defeated nodes show medals, undefeated glow, locked nodes have fog.
- Manual: boss nodes are visually distinct.
- `./gradlew assembleDebug` must compile.
