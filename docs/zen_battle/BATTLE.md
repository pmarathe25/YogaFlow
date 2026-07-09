# Battle System

Speed-based turn queue with interleaved hero/monster turns. Canvas-drawn sprites, parallax backgrounds, and animated VFX.

## Data Sources

| Data | Location |
|------|----------|
| Combat logic | `game/battle/BattleReducer.kt` |
| Turn management | `game/battle/TurnManager.kt` |
| Random provider | `game/battle/RandomProvider.kt` |
| Sound effects | `game/battle/BattleSoundManager.kt` |
| Status effects | `game/model/StatusEffect.kt` |
| Element effectiveness | `game/model/Element.kt` |

## Turn System

1. **SPD** determines turn order. All units sorted by SPD at battle start.
2. After each action, the turn passes to the next unit in the queue.
3. Status effects tick at end of each full round (all living units have acted once).
4. Dead units are removed from the queue.

## Action Types

| Action | Ultimate Gauge | Description |
|--------|---------------|-------------|
| Skill | +20 | Use one of 3 class skills |
| Ultimate | 0 (costs 100) | Hero's ultimate ability |
| Defend | +30 | Skip turn, gain more gauge |
| Combo | +20 each | Team attack consuming all participants' turns |

## Combo Skills

Available when 2+ required heroes are alive. Any participant can trigger. Consumes all participants' turns. Defined in `assets/game/combos.json` and loaded via `DataLoader.kt`.

## Status Effects

See `game/model/StatusEffect.kt` for current implementation.

## Element Effectiveness

See `game/model/Element.kt` for the current effectiveness matrix.

## Battle Layout

```
┌──────────────────────────────────────────┐
│                                          │
│          [Monster Sprite]                 │
│          (large, centered)               │
│          HP bar                          │
│                                          │
│    ┌──────┐  ┌──────┐  ┌──────┐        │
│    │Hero 1│  │Hero 2│  │Hero 3│         │
│    │sprite│  │sprite│  │sprite│         │
│    │ HP   │  │ HP   │  │ HP   │         │
│    └──────┘  └──────┘  └──────┘         │
│                                          │
│  [Skill 1] [Skill 2] [Skill 3]          │
│  [ULTIMATE] [DEFEND] [COMBO]            │
└──────────────────────────────────────────┘
```

## Canvas Visual Features

- **Hero sprites**: Path-based abstract silhouettes with element-colored radial auras
- **Monster sprites**: Unique shadow shapes per monster
- **Backgrounds**: Deep night-sky gradient + floating light motes with parallax
- **Damage numbers**: Float up and fade; crits (10%, 1.5x) are yellow + larger
- **Health bars**: Gradient green→yellow→red, white damage flash, blue shield overlay
- **Screen shake**: On hit, proportional to damage
- **Boss aura**: Pulsing radial glow on boss monsters

## State Management

- `StateFlow` with snapshot pattern for battle state to trigger Compose recomposition
- `BattleState` data class holds all combat state
- `BattleReducer` processes actions and returns new state