# Heroes

Five heroes, each with 1 basic attack + 4 class skills + 1 Ultimate.

## Data Sources

| Data | Location |
|------|----------|
| Hero definitions | `assets/game/heroes.json` |
| Skill definitions | embedded in `assets/game/heroes.json` (`skills` array) |
| Hero model | `game/model/Hero.kt` |
| Data loading | `game/persistence/DataLoader.kt` |

## Hero Overview

| Hero | Role | Element | Unlock Yoga Level |
|------|------|---------|-------------------|
| Shanti (Calm) | Healer/Support | Water | 1 |
| Santosha (Content) | Tank | Earth | 2 |
| Virya (Vigor) | DPS/Brawler | Fire | 3 |
| Dhairya (Courage) | Paladin/Buffer | Light | 4 |
| Maitri (Loving-Kindness) | Mage/Healer | Air | 5 |

## Hero Stats

Base stats defined in `heroes.json`. Level multiplier: +15% all stats per hero level.

- HP, ATK, SPD scale with hero level
- Ultimate gauge: +20 on skill/Strike, +30 on Defend, costs 100

## Skills

Each hero has:
- 1 basic attack (Strike)
- 4 class skills
- 1 Ultimate

Skills defined in JSON with damage type (PHY/ELE), element, power scaling, and effects.

## Source of Truth

**All hero data is in `assets/game/heroes.json`.** Do not maintain hero stats/tables in this document — refer to the JSON file and `DataLoader.kt` for the current definitions.