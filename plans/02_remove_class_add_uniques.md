# Plan: Remove Class-Specific Gear, Add More Unique Items

## 3a. Remove CLASS_SPECIFIC items from equipment.json

**File:** `app/src/main/assets/game/equipment.json`

Delete these 12 items entirely (their full JSON objects):

| id | Class |
|----|-------|
| staff_of_life | Healer |
| healers_vestments | Healer |
| pacifiers_charm | Healer |
| bulwark_shield | Tank |
| vitality_mantle | Tank |
| defenders_crest | Tank |
| fury_blade | DPS |
| assault_plate | DPS |
| berserkers_band | DPS |
| guiding_lance | Buffer |
| commanders_plate | Buffer |
| inspirers_crown | Buffer |
| prism_staff | Mage |
| flowing_robes | Mage |
| empath_ring | Mage |

## 3b. Add 6 new unique items

**File:** `app/src/main/assets/game/equipment.json`

Insert into the `"items"` array. Use `"tier": "UNIQUE"` and `"heroClass": null` for all.

### Shanti — Tidal Staff
```json
{"id": "shanti_tidal_staff", "name": "Shanti's Tidal Staff", "slot": "WEAPON", "tier": "UNIQUE", "heroClass": null, "heroId": "Shanti", "minYogaLevel": 3, "minHeroLevel": 2, "sparkCost": 10, "icon": "🌊", "description": "A staff channeling the gentle flow of water.", "effects": [{"type": "HEAL_AMOUNT", "value": 0.15, "target": "SELF"}, {"type": "HEAL_SHIELD_PERCENT", "value": 0.1, "target": "SELF"}]}
```

### Shanti — Serene Mantle
```json
{"id": "shanti_serene_mantle", "name": "Shanti's Serene Mantle", "slot": "ARMOR", "tier": "UNIQUE", "heroClass": null, "heroId": "Shanti", "minYogaLevel": 7, "minHeroLevel": 5, "sparkCost": 18, "icon": "💙", "description": "A mantle woven from the calmest waters.", "effects": [{"type": "HP_PERCENT", "value": 0.15, "target": "SELF"}, {"type": "INCOMING_HEALING", "value": 0.15, "target": "SELF"}]}
```

### Santosha — Earthen Bulwark
```json
{"id": "santosha_earthen_bulwark", "name": "Santosha's Earthen Bulwark", "slot": "WEAPON", "tier": "UNIQUE", "heroClass": null, "heroId": "Santosha", "minYogaLevel": 3, "minHeroLevel": 2, "sparkCost": 10, "icon": "🏔️", "description": "An unyielding bulwark forged from the earth itself.", "effects": [{"type": "SHIELD_GAIN", "value": 0.2, "target": "SELF"}, {"type": "HP_PERCENT", "value": 0.08, "target": "SELF"}]}
```

### Santosha — Contentment Beads
```json
{"id": "santosha_contentment_beads", "name": "Santosha's Contentment Beads", "slot": "ACCESSORY", "tier": "UNIQUE", "heroClass": null, "heroId": "Santosha", "minYogaLevel": 7, "minHeroLevel": 5, "sparkCost": 18, "icon": "🧘", "description": "Beads that remind the wearer of life's true riches.", "effects": [{"type": "PARTY_DAMAGE_REDUCTION", "value": 0.05, "target": "SELF"}, {"type": "START_SHIELD_PERCENT", "value": 0.1, "target": "SELF"}]}
```

### Virya — Blazing Mantle
```json
{"id": "virya_blazing_mantle", "name": "Virya's Blazing Mantle", "slot": "ARMOR", "tier": "UNIQUE", "heroClass": null, "heroId": "Virya", "minYogaLevel": 7, "minHeroLevel": 4, "sparkCost": 18, "icon": "🔥", "description": "A mantle that burns with unfettered vigor.", "effects": [{"type": "ATK_PERCENT", "value": 0.12, "target": "SELF"}, {"type": "CRIT_CHANCE", "value": 0.05, "target": "SELF"}]}
```

### Dhairya — Courage Circlet
```json
{"id": "dhairya_courage_circlet", "name": "Dhairya's Courage Circlet", "slot": "ACCESSORY", "tier": "UNIQUE", "heroClass": null, "heroId": "Dhairya", "minYogaLevel": 7, "minHeroLevel": 4, "sparkCost": 18, "icon": "👑", "description": "A circlet that channels boundless courage.", "effects": [{"type": "ULTIMATE_GAIN_RATE", "value": 0.15, "target": "SELF"}, {"type": "DAMAGE_PERCENT", "value": 0.05, "target": "SELF"}]}
```

## 3c. Update setBonuses

**File:** `app/src/main/assets/game/equipment.json`

Keep the 3 existing bonuses. Add 2 new ones:

```json
{"name": "Calming Current", "description": "Pranayama Breath also grants a shield equal to 10% of target's max HP", "requiredItems": ["shanti_prayer_beads", "shanti_tidal_staff"], "effects": [{"type": "SHIELD_GAIN", "value": 0.1, "target": "SELF"}]},
{"name": "Eternal Foundation", "description": "Battle start: gain an additional 15% max HP as shield", "requiredItems": ["santosha_foundation_stone", "santosha_earthen_bulwark"], "effects": [{"type": "START_SHIELD_PERCENT", "value": 0.15, "target": "SELF"}]}
```

## 3d. Update heroes.json

**File:** `app/src/main/assets/game/heroes.json`

Update `uniqueItemIds` arrays and `setBonusId`:

| Hero | uniqueItemIds | setBonusId |
|------|---------------|------------|
| Shanti | `["shanti_prayer_beads", "shanti_tidal_staff", "shanti_serene_mantle"]` | `"Shanti"` |
| Santosha | `["santosha_foundation_stone", "santosha_earthen_bulwark", "santosha_contentment_beads"]` | `"Santosha"` |
| Virya | `["virya_ember_core", "virya_inferno_wrath", "virya_blazing_mantle"]` | `"Virya"` (unchanged) |
| Dhairya | `["dhairya_battle_standard", "dhairya_light_vanguard", "dhairya_courage_circlet"]` | `"Dhairya"` (unchanged) |
| Maitri | (unchanged) | `"Maitri"` (unchanged) |

## 3e. Handle orphaned CLASS_SPECIFIC references in Kotlin

**File:** `app/src/main/java/com/example/game/model/Equipment.kt`

- `goldCost`: Remove `EquipmentTier.CLASS_SPECIFIC -> 8` branch. Only `GENERIC -> 5` and `UNIQUE -> 10` remain.
- `getThemeColor()`: Remove `EquipmentTier.CLASS_SPECIFIC -> Color(0xFFB388FF)` branch.
- `getIcon()`: This will be replaced by the per-item icon approach from Plan 02, so no separate change needed here.

**File:** `app/src/main/java/com/example/game/ui/components/ShopScreen.kt`

The `CLASS_SPECIFIC` branches in name-color `when` blocks (lines 172-176, 244-248) will be removed as part of Plan 02's switch to `getThemeColor()`.

Note: The `EquipmentTier.CLASS_SPECIFIC` enum value and `HeroClass` enum can remain in code for potential future use. They're not hurting anything.

## 3f. Update EQUIPMENT.md

**File:** `docs/zen_battle/EQUIPMENT.md`

Replace the entire doc content:

```markdown
# Equipment System

3 slots per hero: **Weapon**, **Armor**, **Accessory**. Each item has a minimum Yoga Level requirement + Sparks cost.

## Tiers

| Tier | Availability | Copies | Gated By |
|------|-------------|--------|----------|
| Generic | Any hero | Multi-copy | Yoga Level + Sparks |
| Unique | One hero | Single copy | Yoga Level + Hero Level + Sparks (or Battle Reward) |

Heroes with 2+ unique items gain a **Set Bonus** when all their available unique items are equipped simultaneously.

## Generic Items (any hero)

| Item | Slot | Yoga Lvl | Cost | Effect |
|------|------|----------|------|--------|
| Training Blade | Weapon | 1 | 2 | +5% ATK, physical damage +3% |
| Crystal Sword | Weapon | 3 | 5 | +10% ATK, +3% crit chance |
| Mythril Edge | Weapon | 6 | 10 | +15% ATK, +5% crit chance, +10% crit damage |
| Traveler's Cloak | Armor | 1 | 2 | +5% max HP |
| Enchanted Robe | Armor | 3 | 5 | +10% max HP, +5% status resistance |
| Ethereal Vestment | Armor | 6 | 10 | +15% max HP, +10% status resistance, +5% incoming healing |
| Simple Beads | Accessory | 1 | 3 | Reduce status duration by 1 turn |
| Swift Boots | Accessory | 4 | 6 | +12% SPD |
| Sage's Tome | Accessory | 7 | 12 | +15% ultimate gauge gain, +5% SPD |
| Ember Pendant | Accessory | 3 | 5 | 10% chance bonus fire damage on Strike |
| Crystal Ward | Armor | 5 | 8 | Start battle with 15% max HP as shield |
| Force Amulet | Accessory | 6 | 10 | All skill damage +8% |

## Unique Items (one hero, single copy)

Require both Yoga Level (account-wide) AND Hero Level (specific hero).
Some unique items are obtained as first-defeat battle rewards instead of being sold in the shop.

### Shanti (Calm) — Healer

| Item | Slot | Yoga Lvl | Hero Lvl | Cost | Effect | Acquisition |
|------|------|----------|----------|------|--------|-------------|
| Shanti's Prayer Beads | Accessory | 5 | 3 | 15 | Calming Radiance also grants party SPD+ 3 turns | Reward: defeat Bhaya |
| Shanti's Tidal Staff | Weapon | 3 | 2 | 10 | +15% heal amount, heals also grant 10% as shield | Shop |
| Shanti's Serene Mantle | Armor | 7 | 5 | 18 | +15% max HP, +15% incoming healing | Shop |

**Set Bonus (2): Calming Current** — Pranayama Breath also grants a shield equal to 10% of target's max HP

### Santosha (Content) — Tank

| Item | Slot | Yoga Lvl | Hero Lvl | Cost | Effect | Acquisition |
|------|------|----------|----------|------|--------|-------------|
| Santosha's Foundation Stone | Armor | 5 | 3 | 15 | Battle start: gain 30% max HP as shield | Reward: defeat Chinta |
| Santosha's Earthen Bulwark | Weapon | 3 | 2 | 10 | +20% shield strength, +8% max HP | Shop |
| Santosha's Contentment Beads | Accessory | 7 | 5 | 18 | Party takes 5% less damage, start with 10% shield | Shop |

**Set Bonus (2): Eternal Foundation** — Battle start: gain an additional 15% max HP as shield

### Virya (Vigor) — DPS

| Item | Slot | Yoga Lvl | Hero Lvl | Cost | Effect | Acquisition |
|------|------|----------|----------|------|--------|-------------|
| Virya's Ember Core | Accessory | 5 | 3 | 15 | Each Blazing Ascension hit has 30% burn chance | Reward: defeat Matsarya |
| Virya's Inferno Wrath | Weapon | 8 | 6 | 20 | Tapas Blast +30% damage to burning targets | Shop |
| Virya's Blazing Mantle | Armor | 7 | 4 | 18 | +12% ATK, +5% crit chance | Shop |

**Set Bonus (2): Raging Inferno** — Tapas Blast burn chance +20%

### Dhairya (Courage) — Buffer

| Item | Slot | Yoga Lvl | Hero Lvl | Cost | Effect | Acquisition |
|------|------|----------|----------|------|--------|-------------|
| Dhairya's Battle Standard | Weapon | 5 | 3 | 15 | Rallying Cry buffs last +2 turns | Reward: defeat Dvesha |
| Dhairya's Light Vanguard | Armor | 8 | 6 | 20 | Courageous Strike shields all allies 10% max HP | Shop |
| Dhairya's Courage Circlet | Accessory | 7 | 4 | 18 | +15% ultimate gauge gain, +5% all damage | Shop |

**Set Bonus (2): Inspiring Presence** — Battle start: party ATK+ 3 turns

### Maitri (Loving-Kindness) — Mage

| Item | Slot | Yoga Lvl | Hero Lvl | Cost | Effect | Acquisition |
|------|------|----------|----------|------|--------|-------------|
| Maitri's Universal Key | Accessory | 5 | 3 | 15 | Universal Embrace revives one fallen ally | Reward: defeat Lobha |
| Maitri's Wind Caress | Weapon | 8 | 6 | 20 | Loving Aura damage +30% | Shop |
| Maitri's Heart Embrace | Armor | 10 | 8 | 25 | Compassion's Touch also grants SPD+ 3 turns | Shop |

**Set Bonus (3): Universal Love** — Loving Aura cleanses statuses from all allies

## Set Bonuses Summary

| Hero | Items | Bonus |
|------|-------|-------|
| Shanti | Prayer Beads + Tidal Staff | **Calming Current**: Pranayama Breath also grants 10% shield |
| Santosha | Foundation Stone + Earthen Bulwark | **Eternal Foundation**: Battle start: additional 15% shield |
| Virya | Ember Core + Inferno's Wrath | **Raging Inferno**: Tapas Blast burn chance +20% |
| Dhairya | Battle Standard + Light's Vanguard | **Inspiring Presence**: Battle start: party ATK+ 3 turns |
| Maitri | Universal Key + Wind's Caress + Heart's Embrace | **Universal Love**: Loving Aura cleanses statuses from all allies |
```

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/assets/game/equipment.json` | Remove 12 class items, add 6 new unique items, add 2 set bonuses |
| `app/src/main/assets/game/heroes.json` | Update `uniqueItemIds` and `setBonusId` for Shanti, Santosha, Virya, Dhairya |
| `app/src/main/java/com/example/game/model/Equipment.kt` | Remove CLASS_SPECIFIC branches from `goldCost` and `getThemeColor()` |
| `docs/zen_battle/EQUIPMENT.md` | Full rewrite removing class section, adding new items + acquisition column |
