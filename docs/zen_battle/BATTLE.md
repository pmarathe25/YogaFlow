# Battle System

Speed-based turn queue with interleaved hero/monster turns. Canvas-drawn sprites, parallax backgrounds, and animated VFX.

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

Available when 2+ required heroes are alive. Any participant can trigger. Consumes all participants' turns.

### 2-Hero Combos

| Combo | Heroes | Effect |
|-------|--------|--------|
| Purifying Shelter | Shanti + Santosha | Party shield (30% max HP) + cleanse all debuffs |
| Meditative Fury | Shanti + Virya | Fire damage all + party heal |
| Calm Resolve | Shanti + Dhairya | Party ATK/SPD up + heal |
| Tranquil Embrace | Shanti + Maitri | Full party revive at 40% HP |
| Unyielding Flame | Santosha + Virya | Massive single PHY+FIRE hybrid |
| Fortress of Light | Santosha + Dhairya | Party shield + dmg reduction + ATK buff |
| Gentle Fortitude | Santosha + Maitri | Party shield + heal over time |
| Inspiring Blaze | Virya + Dhairya | FIRE+LIGHT damage + party ATK buff |
| Wild Compassion | Virya + Maitri | AIR+FIRE damage all + party heal |
| Kindled Courage | Dhairya + Maitri | Party full heal + overshield 25% |

### 3-Hero Combos

| Combo | Heroes | Effect |
|-------|--------|--------|
| Serene Eruption | Shanti + Santosha + Virya | Party overshield 40% + heavy fire damage all |
| Unyielding Radiance | Shanti + Santosha + Dhairya | Party invincibility 2 turns + full heal + cleanse |
| Tranquil Bastion | Shanti + Santosha + Maitri | Party full HP + shield + ATK/SPD buff |
| Blazing Conviction | Shanti + Virya + Dhairya | Massive FIRE+LIGHT all + party heal + ATK buff |
| Universal Flame | Shanti + Virya + Maitri | 4-element hit all + party revive 50% |
| Compassionate Radiance | Shanti + Dhairya + Maitri | Party max HP + all buffs + cleanse |
| Fortified Valor | Santosha + Virya + Dhairya | Party invincibility 2 turns + massive damage all |
| Nurtured Strength | Santosha + Virya + Maitri | Party full HP + overshield + AIR+FIRE damage |
| Eternal Guardian | Santosha + Dhairya + Maitri | Party overshield 60% + all stats up + cleanse |
| Radiant Vigor | Virya + Dhairya + Maitri | 4-hit 3-element + party revive 50% |

## Status Effects

| Effect | Effect | Duration |
|--------|--------|----------|
| Burn | Take 10% ATK as damage/turn | 3 turns |
| Shield | Absorbs damage before HP | Until consumed |
| ATK+ | +25% attack power | 3 turns |
| ATK- | -25% attack power | 3 turns |
| SPD+ | +25% speed | 3 turns |
| SPD- | -25% speed | 3 turns |
| Damage Reduction | -20% incoming damage | 1-2 turns |
| Taunt | Forces enemies to target this unit | 1-2 turns |
| Stun | Skip next turn | 1 turn |
| Confuse | 25% chance to attack own party | 2 turns |

## Element Effectiveness

| Attacker\Defender | Fire | Water | Air | Earth | Light | Dark | Shadow | Electric | Void |
|-------------------|------|-------|-----|-------|-------|------|--------|----------|------|
| Fire | 1.0 | 0.5 | 1.5 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| Water | 1.5 | 1.0 | 1.0 | 0.5 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| Air | 0.5 | 1.0 | 1.0 | 1.5 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| Earth | 1.0 | 1.5 | 0.5 | 1.0 | 1.0 | 1.0 | 1.0 | 0.5 | 1.0 |
| Light | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.5 | 1.5 | 1.0 | 0.5 |
| Dark | 1.0 | 1.0 | 1.0 | 1.0 | 1.5 | 1.0 | 1.0 | 1.0 | 0.5 |
| Shadow | 1.0 | 1.0 | 1.0 | 1.0 | 1.5 | 1.0 | 1.0 | 1.0 | 1.0 |
| Electric | 1.0 | 1.5 | 1.0 | 0.5 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| Void | 1.0 | 1.0 | 1.0 | 1.0 | 1.5 | 1.5 | 1.0 | 1.0 | 1.0 |

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
