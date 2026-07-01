# Plan: Audit Skill Numbers for Accuracy

## Issue Addressed
- **Issue 11**: Many skills have incorrect numbers — some healing skills claim to heal 50% HP, but actually heal 0% HP. Audit skills to find discrepancies between stated effect and real effect. Ideally, there should only be one source of truth. Description flavor text can omit exact numbers since the skill card already mentions specifics.

---

## Root Cause Analysis

There are **two sources of truth** for skill effects:

1. **`skill.description`** — Free-text flavor text (e.g., "Heal target ally and cleanse 1 status effect.")
2. **`skill.getMechanicsDescription()`** — Auto-generated description from the skill's data fields (e.g., "Heals 35 HP. Cleanses negative effects.")

The `getMechanicsDescription()` method in `Skill.kt` uses the actual data fields to generate its output. The `description` field is static text that may become outdated as skill data changes.

### Specific issues found in `heroes.json`:

1. **Shanti's "Calming Presence" (shanti_skill2)**:
   - Description: "Grant target shield and SPD+."
   - Shield scaling: `{"baseShield": 0, "shieldPerLevel": 0, "isPercentage": true, "percentage": 0}`
   - **Bug**: `percentage: 0` means the shield is computed as `(0 * casterMaxHp).toInt()` = 0 shield! The description claims it grants a shield, but `getMechanicsDescription()` would output "Grants 0% Max HP Shield." — it grants nothing.
   - **Fix**: Set `percentage` to a reasonable value (e.g., `0.15` for 15% Max HP shield).

2. **Shanti's Ultimate "Calming Radiance" (shanti_ultimate)**:
   - Description: "Revive all fallen allies and heal 50% of max HP."
   - Heal scaling: `{"baseHeal": 0, "healPerLevel": 0, "isPercentage": true, "targetMissingHpBonus": 0}`
   - `baseHeal: 0` with `isPercentage: true` — `computeHeal()` returns `(0 * casterMaxHp / 100).coerceAtLeast(1)` = 0 HP.
   - **Bug**: Claims 50% heal but heals 0%. `baseHeal` should be `50` for 50% heal.

3. **Maitri's Ultimate "Universal Embrace" (maitri_ultimate)**:
   - Description: "Fully heal the party and deal air damage to all enemies."
   - Heal scaling: `{"baseHeal": 0, "healPerLevel": 0, "isPercentage": true, "targetMissingHpBonus": 0}`
   - Same bug: `baseHeal: 0` = 0% heal. Should be `100` for "Fully heal".

4. **Combo "Purifying Shelter" (combo_shanti_santosha)**:
   - `shieldScaling: {"baseShield": 30, "shieldPerLevel": 5, "isPercentage": true, "percentage": 0}`
   - `isPercentage: true` with `percentage: 0` means `(0 * avgHero.maxHp).toInt()` = 0. But `baseShield: 30` is ignored when `isPercentage` is true.
   - The description says "Party shield + cleanse all debuffs" but the shield is 0.
   - **Fix**: Either set `isPercentage: false` (to use `baseShield`) or set `percentage` to a non-zero value.

5. **Combo "Gentle Fortitude" (combo_santosha_maitri)**:
   - Shield scaling: same pattern — `isPercentage: true, percentage: 0` but with `baseShield: 25`. The shield from `computeShield` for combo would be 0.
   - **Fix**: Set `isPercentage: false` to use the flat value.

6. **Combo "Serene Eruption" (combo_shanti_santosha_virya)**:
   - Same shield issue: `{"baseShield": 40, "shieldPerLevel": 0, "isPercentage": true, "percentage": 0}`

7. **Combo "Kindled Courage" (combo_dhairya_maitri)**:
   - `healScaling: {"baseHeal": 100, "healPerLevel": 0, "isPercentage": true, "targetMissingHpBonus": 0}`
   - `baseHeal: 100` with `isPercentage: true` → `computeHeal` returns `(100 * avgHero.maxHp / 100)` which is 100% HP. This is correct.

8. **"Compassionate Radiance" (combo_shanti_dhairya_maitri)** and "Tranquil Bastion" (combo_shanti_santosha_maitri) and "Nurtured Strength" (combo_santosha_virya_maitri):
   - `healScaling: {"baseHeal": 100, ...}` with `isPercentage: true` — these are correct (100% HP heal).

---

## Changes Required

### 1. Fix data in `heroes.json`

| Skill ID | Field | Current Value | Correct Value |
|----------|-------|--------------|---------------|
| `shanti_skill2` | `shieldScaling.percentage` | `0` | `0.15` (15% Max HP) |
| `shanti_ultimate` | `healScaling.baseHeal` | `0` | `50` (50% Max HP) |
| `maitri_ultimate` | `healScaling.baseHeal` | `0` | `100` (100% Max HP, as described) |

### 2. Fix data in `combos.json`

| Combo ID | Field | Current Value | Correct Value |
|----------|-------|--------------|---------------|
| `combo_shanti_santosha` | `shieldScaling.isPercentage` | `true` | `false` (or add `percentage: 0.3`) |
| `combo_santosha_maitri` | `shieldScaling.isPercentage` | `true` | `false` |
| `combo_shanti_santosha_virya` | `shieldScaling.isPercentage` | `true` | `false` (with `baseShield: 40` it should work as flat) |

Also verify all `isPercentage: true` + `percentage: 0` cases across all combos — these should either:
- Use `isPercentage: false` with appropriate `baseShield`/`baseHeal` values, or
- Have a correct `percentage` value (e.g., `0.4` for 40% Max HP)

### 3. Remove numerical claims from `description` flavor text (optional)

The `getMechanicsDescription()` method on `SkillCard` already shows the mechanics with exact numbers. The `description` field (shown as italic flavor text on the card) should be free of specific numbers to avoid future discrepancies:

```
Current: "Revive all fallen allies and heal 50% of max HP."
Better:  "Revive all fallen allies with restorative energy."
```

This is optional but recommended as a preventative measure. Update all skill descriptions that contain specific percentages to use qualitative language instead.

### 4. Verify the fix

After fixing data files, verify in-game that:
- Shanti's "Calming Presence" actually grants a shield
- Shanti's ultimate actually heals on revive
- Maitri's ultimate fully heals the party
- Combo shields are non-zero
