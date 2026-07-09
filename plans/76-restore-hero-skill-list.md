# Plan 76: Restore Old HeroDetailSkillCard for Skill List

## Problem
Commit `560207a` replaced `HeroDetailSkillCard` (compact list item) with `SkillCard` (large battle card at 180dp height) for the skill list in `HeroDetailsDialog`. The battle `SkillCard` is designed for ActionTray (full color, large stats) and looks wrong in a dialog list — too large, visually overwhelming, and doesn't fit well with the dialog layout.

The `SkillCard` popup (on tap) is correct and should remain.

## Fix
Restore the original `HeroDetailSkillCard` composable for the list items (skills + ultimate). Keep `SkillCard` only for the popup dialog.

Restore the old `HeroDetailSkillCard` composable with:
- Compact `Surface` with colored border/background (12dp rounded, alpha 0.08f background, 0.3f border)
- Left colored indicator bar (8dp × 36dp, rounded)
- Skill name + type label chip + star if ultimate, description, stats row (damage, heal, CD, cleanse)
- Right-side ultimate gauge gain column
- `Modifier.fillMaxWidth().padding(vertical = 3.dp)` — no fixed height

## Files to Modify
- `app/src/main/java/com/example/game/ui/components/PartyScreen.kt`

## Changes

### 1. Restore HeroDetailSkillCard composable
Insert before `EquipmentSlotCard` (line ~466). Implementation from git commit `2f27ba2`:

```kotlin
@Composable
private fun HeroDetailSkillCard(skill: Skill, hero: Hero, level: Int, isUltimate: Boolean = false, heroColor: Color = Color.Gray, modifier: Modifier = Modifier) {
    val typeColor = heroColor

    val typeLabel = when {
        isUltimate -> "Ultimate"
        skill.healScaling != null -> "Heal"
        skill.shieldScaling != null -> "Shield"
        skill.cleanse -> "Cleanse"
        skill.statusEffects.isNotEmpty() -> "Status"
        skill.buffs.isNotEmpty() -> "Buff"
        skill.damageComponents.any { it.type == DamageType.ELEMENTAL } ->
            skill.damageComponents.firstNotNullOfOrNull { it.element?.name } ?: "Damage"
        else -> "Physical"
    }

    val damage = skill.baseDamage + skill.damagePerLevel * level
    val healAmount = skill.healScaling?.let {
        if (it.isPercentage) "${it.baseHeal}%" else "${it.baseHeal + it.healPerLevel * level}"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = typeColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(8.dp, 36.dp)
                    .background(typeColor, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(skill.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = typeColor)
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = typeColor.copy(alpha = 0.15f)) {
                        Text(typeLabel, style = MaterialTheme.typography.labelSmall, color = typeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    if (isUltimate) { Spacer(Modifier.width(4.dp)); Text("\u2B50", fontSize = 12.sp) }
                }
                Spacer(Modifier.height(4.dp))
                Text(skill.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (damage > 0) Text("${damage} dmg", style = MaterialTheme.typography.labelSmall, color = typeColor)
                    if (healAmount != null) Text("Heal $healAmount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    if (skill.cooldown > 0) Text("CD: ${skill.cooldown}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    if (skill.cleanse) Text("Cleanses", style = MaterialTheme.typography.labelSmall, color = Color(0xFFAB47BC))
                }
            }
            if (!isUltimate) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("+${skill.ultimateGain}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("ult gauge", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}
```

### 2. Replace list items (lines 433-459)
Replace both `SkillCard(...)` calls for skills and ultimate with `HeroDetailSkillCard(...)`:

```kotlin
hero.skills.forEach { skill ->
    HeroDetailSkillCard(
        skill = skill, hero = hero, level = partyMember.level, heroColor = heroColor,
        modifier = Modifier.clickable { selectedSkill = skill }
    )
}

Spacer(Modifier.height(12.dp))
HeroDetailSkillCard(
    skill = hero.ultimate, hero = hero, level = partyMember.level, isUltimate = true, heroColor = heroColor,
    modifier = Modifier.clickable { selectedSkill = hero.ultimate }
)
```

### 3. Keep popup dialog (lines 404-427) unchanged
The `Dialog` with `SkillCard` remains as-is.

## Verification
- Open Party screen → tap any hero → skills render as compact list items with colored bar, type label, stats
- Tap any skill → popup shows full `SkillCard` style
- No visual regressions in dialog layout

## Dependencies
- Plan 75 (Skill model cleanup) should be applied first or concurrently — `HeroDetailSkillCard` reads `skill.healScaling`, `skill.shieldScaling`, etc. which are unchanged by Plan 75
