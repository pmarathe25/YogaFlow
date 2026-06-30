# Yoga Progression

XP, levels, achievements, and stats earned through practice.

## Karma XP

Every completed session earns Karma XP based on duration and difficulty:

```
XP = 150 (base) + 10 × durationMinutes + difficultyBonus
```

where `difficultyBonus` ranges from 30 (Beginner) to 150 (Advanced).

Each unique practice day also awards 1 **Zen Spark** with a +150 XP bonus.

## Yoga Levels

10 levels with XP thresholds:

| Level | Name | XP Required |
|-------|------|-------------|
| 1 | Prana Sprout | 0 |
| 2 | Breath Seeker | 500 |
| 3 | Flow Walker | 1,200 |
| 4 | Pose Weaver | 2,500 |
| 5 | Sun Saluter | 5,000 |
| 6 | Asana Holder | 10,000 |
| 7 | Inner Warrior | 20,000 |
| 8 | Zen Guardian | 40,000 |
| 9 | Bliss Seeker | 75,000 |
| 10 | Infinite Samadhi | 120,000 |

Level progression determines:
- Zen Battle hero unlocks (levels 1-5)
- Hero skin unlocks (level 6+)
- Equipment availability gates

## Achievements

| Badge | Name | Condition |
|-------|------|-----------|
| 🥇 | First Breath | Complete your first practice session |
| 🥈 | Zen Spark Collector | Earn your first Zen Spark (first unique practice day) |
| 🥈 | Tri-Fold Harmony | Practice 3 different flows |
| 🥇 | Yogi Adept | Reach Yoga Level 5 |
| 💎 | Deep Devotee | Practice on 7 different days |

## Stats Tracking

The `StatsViewModel` computes and exposes:

- Total sessions completed
- Total Karma XP earned
- Current level, level name, and progress to next level
- Total Zen Sparks (unique practice days)
- Achievement unlock status
- Daily quest: practice on the current calendar day

All session data is stored in the Room `yoga_sessions` table and viewable in the Practice History screen.
