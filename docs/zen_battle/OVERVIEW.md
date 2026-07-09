# Zen Battle — Minigame Overview

A turn-based battle minigame within YogaFlow. Players guide five yoga-virtue heroes through battles against inner-demon monsters using Canvas-drawn graphics, party combat, and elemental strategy.

## Data Sources

| Data | Location |
|------|----------|
| Heroes | `assets/game/heroes.json` |
| Monsters | `assets/game/monsters.json` |
| Equipment | `assets/game/equipment.json` |
| Combos | `assets/game/combos.json` |
| Trophies | `assets/game/trophies.json` |
| Data loading | `game/persistence/DataLoader.kt` |
| Models | `game/model/` |

## Theme

- **Heroes**: Sanskrit yoga virtues — Shanti (Calm/Water), Santosha (Content/Earth), Virya (Vigor/Fire), Dhairya (Courage/Light), Maitri (Loving-Kindness/Air)
- **Monsters**: Inner demons — each a unique abstract shadow form with distinct shape language
- **Visual**: Dark backgrounds (deep blue/purple/black) with floating light motes. Heroes radiate warm light; monsters are shadowy.

## Progression

| Resource | Source | Used For |
|----------|--------|----------|
| Yoga practice | Main app sessions | Karma XP, Sparks |
| Karma XP | Yoga practice | Level up heroes |
| Yoga Level | Total XP milestones | Unlock heroes, skins, equipment gates |
| Sparks | Yoga streaks | Purchase equipment |
| Badges | Defeat monsters | Cosmetic only |
| Trophies | Special conditions | Cosmetic only |

### Hero Unlock Table

| Yoga Level | Unlock |
|------------|--------|
| 1 | Shanti (always unlocked) |
| 2 | Santosha |
| 3 | Virya |
| 4 | Dhairya |
| 5 | Maitri |
| 6+ | Hero skins (cosmetic, one per level) |

## Key Components

- **GameViewModel** — Party management, battle engine, equipment, save/load
- **BattleReducer** — Pure function battle state transitions
- **TurnManager** — Speed-based turn queue
- **BattleCanvas** — Canvas rendering for sprites, backgrounds, effects
- **GameSaveManager** — SharedPreferences + Gson persistence

See `docs/zen_battle/` for detailed docs on battle system, heroes, monsters, equipment, and rewards.