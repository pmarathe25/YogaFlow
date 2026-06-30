# Monsters

16 total (12 normal + 4 bosses/superboss). Aggressive difficulty scaling. Each monster has a unique mechanic.

## Difficulty Tiers

| Tier | Levels | HP Range | Monsters |
|------|--------|----------|----------|
| Easy | 1-3 | 80-130 | Bhaya, Tandra, Chinta, Alasya |
| Medium | 4-7 | 180-250 | Matsarya, Krodha, Dvesha |
| Hard | 8-12 | 350-420 | Moha, Lobha, Abhimana, Mada, Irsya |
| Boss | 13+ | 600-900 | Ahankara, Maya, Klesh |
| Superboss | 15+ | 1500 | Samsara |

## Easy (starter, levels 1-3)

| Monster | Element | HP/ATK/SPD | Special | Mechanic |
|---------|---------|-----------|---------|----------|
| **Bhaya** (Fear) | Shadow | 80/8/16 | Terrifying Shadow | ATK- debuff 30% per hit. Fast but fragile. |
| **Tandra** (Fatigue) | Water | 100/9/10 | Drowsing Mist | AOE SPD- debuff. Slow and tanky. |
| **Chinta** (Anxiety) | Electric | 110/10/14 | Anxious Swarm | Multi-hit 2-3 random targets. Small SPD- chance. |
| **Alasya** (Sloth) | Earth | 130/11/6 | Crushing Weight (2.0x) | Skips every other turn to build up, then big hit. Predictable. |

## Medium (levels 4-7)

| Monster | Element | HP/ATK/SPD | Special | Mechanic |
|---------|---------|-----------|---------|----------|
| **Matsarya** (Envy) | Dark | 180/16/12 | Covetous Gaze | Steals one random buff from target. |
| **Krodha** (Anger) | Fire | 220/20/8 | Furious Burn | Below 50% HP: Berserk (ATK×2.5, DEF×0.5). |
| **Dvesha** (Aversion) | Dark | 250/18/10 | Vicious Strike | Targets highest-HP hero, bonus 15% max HP damage. |

## Hard (levels 8-12)

| Monster | Element | HP/ATK/SPD | Special | Mechanic |
|---------|---------|-----------|---------|----------|
| **Moha** (Delusion) | Dark | 350/22/11 | Illusion Mist | 30% confuse. Gets one extra action per round. |
| **Lobha** (Greed) | Earth | 400/20/9 | Grasping Hunger | Shield = 40% damage dealt. Extremely tanky. |
| **Abhimana** (Conceit) | Light† | 380/26/11 | Arrogant Beam | Consumes buffs → ATK+ per buff. Punishes buffers. |
| **Mada** (Pride) | Light† | 420/28/12 | Radiant Slam | Heals 20% max HP when heroes heal. Anti-healer. |
| **Irsya** (Jealousy) | Electric | 360/24/15 | Mirror Strike | Copies last hero action. +5 SPD per copy. |

†Mada and Abhimana use corrupted Light (sickly yellow/gold).

## Bosses

| Monster | Element | HP/ATK/SPD | Special | Mechanic |
|---------|---------|-----------|---------|----------|
| **Ahankara** (Ego) | Light | 600/30/12 | Arrogant Slam | P1: Reflect 35% damage. P2 (<50%): Summon mirror images. |
| **Maya** (Illusion) | Dark | 700/28/13 | Deceptive Shadows | P1: Shadow copies (50% HP, 70% dmg). P2 (<50%): Untargetable while copies remain. |
| **Klesh** (Turmoil) | Void | 900/35/14 | Chaos Waves | P1: Random status. P2 (<66%): Shield 60% + summon Bhaya. P3 (<33%): Double actions. |

## Superboss

| Monster | Element | HP/ATK/SPD | Special | Mechanic |
|---------|---------|-----------|---------|----------|
| **Samsara** (Cycle) | Void | 1500/40/16 | Karmic Retribution | 5 phases: P1 null Fire, P2 null Light, P3 null Air, P4 null Earth, P5 null all elements. Physical damage components always work. |

## Phase Triggers

| Trigger | Effect |
|---------|--------|
| REFLECT_DAMAGE | Return % damage to attacker |
| SUMMON_ADD | Spawn additional monster(s) |
| GAIN_SHIELD | Gain % max HP as shield |
| DOUBLE_ACTIONS | Act multiple times per round |
| BECOME_UNTARGETABLE | Cannot be targeted while adds remain |
| NULLIFY_ELEMENT | Specific element(s) deal no damage |
| EXTRA_ACTION | One additional action per round |

## Unlock Progression

Beat Easy (4) → unlock Medium (3) → Beat Medium → unlock Hard (5) → Beat Hard → unlock Bosses → Beat Klesh → unlock Samsara.
