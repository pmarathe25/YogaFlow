# Plan 21: Inline Path of Zen Portal on Journey Screen

## Goal

Replace the plain "Enter Battle" button on the Journey screen with the full "Path of Zen" portal UI (currently in HubScreen's PortalEntrance), and remove the separate HubScreen step. The journey screen becomes a single destination that includes both the yoga dashboard AND the battle entrance portal.

## Current Architecture

```
JourneyScreen
  ├── Stats (Sparks, Gold)
  ├── Info cards
  ├── "ZEN BATTLE HUB" section
  │   ├── Party card
  │   ├── Shop card
  │   └── Enter Battle button
  │
  └── [Enter Battle] → Nav to ZenBattle route → GameApp → HubScreen
                                                           ├── PortalEntrance (Path of Zen portal)
                                                           │   └── [Enter the Path] → MonsterRoadSelection
                                                           └── MonsterRoadSelection → Battle
```

## Target Architecture

```
JourneyScreen
  ├── Stats (Sparks, Gold)
  ├── Info cards
  ├── "ZEN BATTLE HUB" section
  │   ├── Party card
  │   └── Shop card
  │
  ├── Path of Zen Portal (inline, replaces Enter Battle button)
  │   ├── Portal icon/visual
  │   ├── "Path of Zen" title
  │   ├── "Walk the path of enlightenment" subtitle
  │   ├── Monster preview row
  │   └── Enter the Path button → MonsterRoadSelection (full screen)
  │
  └── [Enter the Path] → MonsterRoadSelection → Battle
```

## Changes Required

### 1. Extract `PortalEntrance` as a reusable composable (or inline content)

Move the visual portal content from `HubScreen.kt:PortalEntrance` (lines 68-207) into `JourneyScreen.kt`, replacing lines 428-446 (the "Enter Battle" button block).

### 2. Add MonsterRoadSelection navigation from JourneyScreen

Currently the flow is:
- `JourneyScreen` → navigate to `Screen.ZenBattle.route` → `GameApp` → `HubScreen` → `PortalEntrance` → `MonsterRoadSelection`

New flow:
- `JourneyScreen` → inline portal → show `MonsterRoadSelection` (either inline or as a full-screen overlay/dialog on the journey screen)

Two approaches:

**Approach A (recommended): Make MonsterRoadSelection a dialog on JourneyScreen**
- Import and use `MonsterRoadSelection` directly in `JourneyScreen`
- Show/hide based on a `showMonsterRoad` state variable
- When a monster is selected, navigate to the battle screen directly

**Approach B: Keep HubScreen but skip PortalEntrance**
- Remove `PortalEntrance` from `HubScreen`
- Rename `HubScreen` to `MonsterRoadScreen` 
- JourneyScreen's portal button navigates directly to `MonsterRoadSelection`

### 3. Remove or simplify HubScreen

If using Approach A:
- `HubScreen` can be removed entirely from `GameApp`
- `GameScreen.HUB` and `GameScreen.SETTINGS` cases in `GameApp` can be removed or simplified
- `navigateTo(GameScreen.HUB)` calls become direct navigations to the battle

If using Approach B:
- Rename `HubScreen` → `MonsterRoadSelectionWrapper` or similar
- Remove `PortalEntrance`

### 4. Update navigation

**JourneyScreen** changes:
- Replace lines 428-446 (Enter Battle button) with portal content from `PortalEntrance`
- Add `showMonsterRoad` state
- Show `MonsterRoadSelection` when state is true
- On monster selected: navigate to battle (via existing `onNavigateToBattle` callback or new one)

### 5. Portal content to copy (from HubScreen.kt:68-207)

The portal section to inline includes:
- Animated green radial glow + stars (Canvas, lines 88-105)
- Portal icon: outer ring, spiral, center orb (Canvas, lines 117-142)
- "Path of Zen" headline (lines 147-158)
- Monster preview row (lines 160-177)
- "ENTER THE PATH" button (lines 182-198)
- "Return to main menu" text button (omit — not needed on Journey)

### 6. Integration with existing JourneyScreen layout

Insert the portal content between the existing battle hub row (Party/Shop cards) and the bottom of the Column. The portal already has its own dark-themed background, so wrap it in a `Box` or `Card` with the dark background.

```kotlin
// After Party/Shop Row (line 426)
Spacer(Modifier.height(16.dp))

// Inline portal
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF0D1B2A), RoundedCornerShape(16.dp))
        .clipToBounds()
) {
    // Portal content from HubScreen PortalEntrance
    // ... Canvas background, portal icon, text, monster preview, button
}
```

## Files to modify

| File | Changes |
|---|---|
| `JourneyScreen.kt` | Replace "Enter Battle" button with inline portal; add `showMonsterRoad` state; show `MonsterRoadSelection` |
| `HubScreen.kt` | Remove `PortalEntrance`; simplify or remove the screen |
| `GameApp.kt` | Update `GameScreen.HUB` case (or remove if HubScreen is removed) |
| `GameViewModel.kt` | May need to update `navigateTo(GameScreen.HUB)` → direct battle navigation |

## Dependencies

This plan removes the separate Path of Zen hub. The `HubScreen.kt` file can be deleted fully if no other references exist (check `GameApp.kt` line 37 and line 48).
