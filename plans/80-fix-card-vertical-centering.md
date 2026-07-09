# Plan 80: Fix Skill Cards Vertically Centered in Battle

## Problem
The ActionTray (skill card hand) appears vertically centered in the battle UI instead of anchored to the bottom of the screen. The fix from Plan 78 changed the `Box` to use `contentAlignment = Alignment.BottomCenter` and removed `Modifier.align(Alignment.BottomCenter)` from `ActionTray`, but the hand is still mid-screen.

## Root Cause
The `Box` wrapping `ActionTray` (BattleScene.kt:443-471) uses `contentAlignment = Alignment.BottomCenter` but has **no fixed height** — it wraps to the height of its content (ActionTray → HandOfCards at 280dp). `contentAlignment = BottomCenter` within a wrap-content Box does nothing because there's no extra space to distribute.

The `HandOfCards` has `Modifier.height(280.dp)` and `contentAlignment = Alignment.BottomCenter` — this ALIGNS the card collection to the bottom of its 280dp box, which centers cards vertically inside that 280dp space. But the outer `Box` isn't pinned to the screen bottom.

Meanwhile, parent `Column(modifier = Modifier.fillMaxSize().padding(bottom = 250.dp))` at line 236 divides space with weights (0.55f monster, 0.45f hero). The ActionTray is outside this Column (at the same level in the `Box(modifier = Modifier.fillMaxSize())` parent), so it overlays from the top of the screen with `zIndex(1f)` and `graphicsLayer { translationY = slideFraction * 200.dp }`. When `slideFraction` goes from 1 → 0, the Box slides up by 200dp, but it starts from `y=0` (screen top), not from the bottom.

The Box has no `Modifier.align(Alignment.BottomCenter)` on itself — it just has `contentAlignment` on its **content**. The Box's position within the `Box(Modifier.fillMaxSize())` parent defaults to top-start.

## Fix
Add `Modifier.align(Alignment.BottomCenter)` to the animated Box that wraps ActionTray (line 443). This pins the entire card hand to the bottom of the screen.

Also add `Modifier.windowInsetsBottom(WindowInsets.navigationBars)` to prevent the system navigation bar from obscuring the cards — since the parent Scaffold's bottom padding is not applied to ZenBattle content (see Plan 84).

## File to Modify
- `app/src/main/java/com/example/game/ui/components/BattleScene.kt`

## Changes

### BattleScene.kt — line 443
Add `Modifier.align(Alignment.BottomCenter)` and remove `contentAlignment` from the Box (since contentAlignment on a wrap-content Box is redundant):

**Before:**
```kotlin
Box(
    contentAlignment = Alignment.BottomCenter,
    modifier = Modifier.fillMaxWidth().zIndex(1f)
        .graphicsLayer { translationY = slideFraction * 200.dp.toPx() }
) {
    ActionTray(...)
}
```

**After:**
```kotlin
Box(
    modifier = Modifier.fillMaxWidth().zIndex(1f)
        .align(Alignment.BottomCenter)
        .windowInsetsBottom(WindowInsets.navigationBars)
        .graphicsLayer { translationY = slideFraction * 200.dp.toPx() }
) {
    ActionTray(...)
}
```

Remove `Alignment.BottomCenter` from the ActionTray line if present (already done in Plan 78).

## Verification
- Open battle → skill card hand is flush with the bottom of the screen
- Card deal slide-up animation plays correctly from bottom
- Cards are not covered by system navigation bar (on 3-button nav phones)
- Monster/Hero zones above the hand are fully visible
