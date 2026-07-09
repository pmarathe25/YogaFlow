# Yoga Progression

XP, levels, achievements, and stats earned through practice.

## Data Sources

| Data | Location |
|------|----------|
| Level definitions | `model/LevelDefinitions.kt` |
| XP calculation formula | `model/XpCalculator.kt` |
| Session logging | `db/YogaSession.kt`, `db/YogaSessionDao.kt` |
| Stats computation | `db/StatsManager.kt`, `viewmodel/StatsViewModel.kt` |
| Achievements | `model/YogaModels.kt` (Achievement data class) |

## Karma XP

Formula in `XpCalculator.kt`:
```
XP = 150 (base) + 10 × durationMinutes + difficultyBonus
```

Difficulty bonus varies by flow ID (see `XpCalculator.kt` for current mapping).

Each unique practice day also awards 1 **Zen Spark** with a +150 XP bonus.

## Yoga Levels

10 levels defined in `LevelDefinitions.kt` with XP thresholds. See that file for current level names and XP ranges.

Level progression determines:
- Zen Battle hero unlocks (levels 1-5)
- Hero skin unlocks (level 6+)
- Equipment availability gates

## Achievements

Defined in `YogaModels.kt`. Current achievements include:
- First Breath — complete first session
- Zen Spark Collector — earn first Zen Spark
- Tri-Fold Harmony — practice 3 different flows
- Yogi Adept — reach Yoga Level 5
- Deep Devotee — practice on 7 different days

## Stats Tracking

`StatsViewModel` computes and exposes:
- Total sessions completed
- Total Karma XP earned
- Current level, level name, and progress to next level
- Total Zen Sparks (unique practice days)
- Achievement unlock status
- Daily quest: practice on the current calendar day

All session data is stored in the Room `yoga_sessions` table and viewable in the Practice History screen.