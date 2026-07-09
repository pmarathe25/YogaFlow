# Equipment System

3 slots per hero: **Weapon**, **Armor**, **Accessory**. Each item has a minimum Yoga Level requirement + Sparks cost.

## Data Sources

| Data | Location |
|------|----------|
| Equipment items | `assets/game/equipment.json` |
| Set bonuses | `assets/game/equipment.json` (setBonuses array) |
| Equipment model | `game/model/Equipment.kt` |
| Data loading | `game/persistence/DataLoader.kt` |

## Tiers

| Tier | Availability | Copies | Gated By |
|------|-------------|--------|----------|
| Generic | Any hero | Multi-copy | Yoga Level + Sparks |
| Unique | One hero | Single copy | Yoga Level + Hero Level + Sparks (or Battle Reward) |

Heroes with 2+ unique items gain a **Set Bonus** when all their available unique items are equipped simultaneously.

## Equipment Effects

Effects defined in `EquipmentEffectType` enum (`game/model/Equipment.kt`). Each effect has:
- `type`: Effect type (ATK_PERCENT, HP_PERCENT, SPD_PERCENT, etc.)
- `value`: Float value (percentage as decimal, e.g., 0.15 = 15%)
- `target`: SELF, PARTY, or ALL_ENEMIES

## Source of Truth

**All equipment data is in `assets/game/equipment.json`.** Do not maintain item tables in this document — refer to the JSON file and `DataLoader.kt` for current definitions.