# Monsters

16 total (12 normal + 4 bosses/superboss). Each monster has a unique mechanic and phase triggers.

## Data Sources

| Data | Location |
|------|----------|
| Monster definitions | `assets/game/monsters.json` |
| Monster model | `game/model/Monster.kt` |
| Data loading | `game/persistence/DataLoader.kt` |

## Monster Overview

| Monster | Element | Tier | Unlock |
|---------|---------|------|--------|
| Bhaya (Fear) | Shadow | Easy | Initial |
| Tandra (Fatigue) | Water | Easy | Initial |
| Chinta (Anxiety) | Electric | Easy | Initial |
| Alasya (Sloth) | Earth | Easy | Initial |
| Matsarya (Envy) | Dark | Medium | Beat Easy |
| Krodha (Anger) | Fire | Medium | Beat Easy |
| Dvesha (Aversion) | Dark | Medium | Beat Easy |
| Moha (Delusion) | Dark | Hard | Beat Medium |
| Lobha (Greed) | Earth | Hard | Beat Medium |
| Abhimana (Conceit) | Light | Hard | Beat Medium |
| Mada (Pride) | Light | Hard | Beat Medium |
| Irsya (Jealousy) | Electric | Hard | Beat Medium |
| Ahankara (Ego) | Light | Boss | Beat Hard |
| Maya (Illusion) | Dark | Boss | Beat Hard |
| Klesh (Turmoil) | Void | Boss | Beat Hard |
| Samsara (Cycle) | Void | Superboss | Beat Klesh |

## Mechanics

Each monster has:
- Base stats (HP, ATK, SPD)
- Special attack skill
- AI behavior (target strategy, special chance)
- Phase triggers (reflect, summon, shield, double actions, untargetable, nullify element, extra action)
- Optional first-defeat item reward

## Phase Triggers

Defined in `MonsterPhase` and `PhaseTriggerType` in `game/model/Monster.kt`:
- REFLECT_DAMAGE
- SUMMON_ADD
- GAIN_SHIELD
- DOUBLE_ACTIONS
- BECOME_UNTARGETABLE
- NULLIFY_ELEMENT
- EXTRA_ACTION

## Source of Truth

**All monster data is in `assets/game/monsters.json`.** Do not maintain monster stats/tables in this document — refer to the JSON file and `DataLoader.kt` for the current definitions.