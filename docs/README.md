# YogaFlow Developer Documentation

YogaFlow is a guided yoga practice app for Android with an optional turn-based battle minigame.

## Documentation Structure

| Path | Description |
|------|-------------|
| `yoga/PRACTICE.md` | Yoga flows, poses, session player, voice guidance, ambient music |
| `yoga/PROGRESSION.md` | Karma XP, yoga levels, achievements, stats |
| `yoga/SETTINGS.md` | Settings, practice reminders, data management |
| `zen_battle/OVERVIEW.md` | Turn-based battle minigame — theme, progression, hero unlocks |
| `zen_battle/HEROES.md` | Hero roster, roles, elements, unlock levels |
| `zen_battle/MONSTERS.md` | Monster roster, mechanics, boss phases |
| `zen_battle/EQUIPMENT.md` | Equipment tiers, slots, effect types |
| `zen_battle/BATTLE.md` | Battle system, turn order, combos, status effects, elements |
| `zen_battle/REWARDS.md` | Badges, trophies, monster unlock progression |
| `ARCHITECTURE.md` | Full app architecture, module structure, data flow |

## Source of Truth

**JSON assets in `app/src/main/assets/` and Kotlin source files are the authoritative sources.** Documentation files summarize key concepts and point to the source files — they do not duplicate data tables that are already in code/assets.