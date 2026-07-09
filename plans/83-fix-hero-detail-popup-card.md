# Plan 83: Match Hero Detail Popup SkillCard to Battle Dimensions

## Problem
In the Party screen's hero detail dialog, tapping a skill opens a `Dialog` containing a battle-style `SkillCard`. The card uses different dimensions than the actual battle `SkillCard`:

| Location | Width | Height |
|---|---|---|
| **Battle** (ActionTray.kt:346) | `width(150.dp)` | `height(220.dp)` |
| **Hero detail popup** (PartyScreen.kt:414) | `widthIn(min = 250.dp, max = 300.dp)` | `height(220.dp)` |

The popup card is **significantly wider** (250-300dp vs 150dp), which changes the proportions — text is wider, icon sizes don't scale, and the overall appearance doesn't match the battle card exactly.

The user wants the popup to use the EXACT same component including dimensions, so the card looks identical to the battle view.

## Fix
Change the popup SkillCard modifier to match the battle dimensions exactly:

```kotlin
SkillCard(
    ...
    modifier = Modifier.width(150.dp).height(220.dp)
)
```

Also add `Modifier.wrapContentWidth().wrapContentHeight()` or just `Modifier.size(150.dp, 220.dp)` to ensure no stretching.

Keep the `Dialog` wrapper — that's the only difference (popup shows in Dialog, battle shows in ActionTray).

## File to Modify
- `app/src/main/java/com/example/game/ui/components/PartyScreen.kt`

## Changes

### PartyScreen.kt — line 414
**Before:**
```kotlin
SkillCard(
    skill = skill,
    heroColor = elementToColor(hero.element),
    isUltimate = isUltimateSkill,
    ultReady = true,
    heroLevel = partyMember.level,
    modifier = Modifier.widthIn(min = 250.dp, max = 300.dp).height(220.dp)
)
```

**After:**
```kotlin
SkillCard(
    skill = skill,
    heroColor = elementToColor(hero.element),
    isUltimate = isUltimateSkill,
    ultReady = true,
    heroLevel = partyMember.level,
    modifier = Modifier.width(150.dp).height(220.dp)
)
```

## Verification
- Open Party screen → tap any hero → tap a skill card
- Popup shows a SkillCard identical in proportions to the battle card (150×220dp)
- Text, icons, border, and glow effects are proportionally identical to battle
- Close button at top-right is still visible and clickable

## Dependencies
- None (isolated dimension change)
