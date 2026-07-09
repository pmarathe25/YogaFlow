# Progression & Rewards

## Badges

Earned by defeating each monster for the first time. Defined in `assets/game/trophies.json` with `category: BADGE`.

## Trophies

Earned by meeting special conditions. Defined in `assets/game/trophies.json` with `category: TROPHY`.

## Monster Unlock Progression

Beat all Easy (4) → unlock Medium (3) → Beat Medium → unlock Hard (5) → Beat Hard → unlock Bosses → Beat Klesh → unlock Samsara.

## Data Sources

| Data | Location |
|------|----------|
| Trophy/badge definitions | `assets/game/trophies.json` |
| Trophy model | `game/model/Trophy.kt` |
| Data loading | `game/persistence/DataLoader.kt` |

## Source of Truth

**All trophy/badge data is in `assets/game/trophies.json`.** Do not maintain trophy tables in this document — refer to the JSON file and `DataLoader.kt` for the current definitions.