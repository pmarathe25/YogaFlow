# Plan: Color-Coded Skill List in Hero Details

## Problem

The hero detail dialog doesn't display the hero's skills at all. Users can't see what abilities a hero has without going into battle.

## Solution

Add a skills section to `HeroDetailsDialog` showing all skills + ultimate in a color-coded list. Use element colors for damage types and role-appropriate accent colors for healing/shielding/buffs.

## Color Scheme

| Type | Color |
|------|-------|
| Physical damage | `MaterialTheme.colorScheme.onSurface` (neutral) |
| Elemental damage | `elementToColor(skill.element)` (hero's element color) |
| Healing | Green (`Color(0xFF4CAF50)`) |
| Shielding | Blue (`Color(0xFF2196F3)`) |
| Buffs | Gold/Yellow (`Color(0xFFFFD740)`) |
| Debuffs/Status | Purple (`Color(0xFFAB47BC)`) |
| Ultimate | Special accent (hero's element color + gold border) |

## Layout

```
┌──────────────────────────────────┐
│ Skills                           │
│ ──────────────────────────────── │
│ ⚔️ Gentle Strike   (Physical)    │
│   8 dmg → single enemy           │
│   [████████░░░░] Ultimate +20    │
│                                 │
│ 💚 Pranayama Breath  (Heal)      │
│   Heal 35 + Cleanse 1 debuff     │
│   [████████░░░░] Ultimate +20    │
│   Cooldown: 2                    │
│                                 │
│ 💙 Calming Presence  (Shield)    │
│   Shield 15% + SPD+ 2 turns      │
│   [████████░░░░] Ultimate +20    │
│   Cooldown: 2                    │
│                                 │
│ 🌊 Rippling Current  (Water)     │
│   15 dmg → single enemy          │
│   [████████░░░░] Ultimate +20    │
│   Cooldown: 1                    │
│ ──────────────────────────────── │
│ ⭐ Calming Radiance  (Ultimate)  │
│   Revive all + heal 50% HP       │
│   Cost: 100 gauge                │
└──────────────────────────────────┘
```

## Implementation

**File:** `app/src/main/java/com/example/game/ui/components/PartyScreen.kt`

Add after the stats section and before the gear section (around line 220):

```kotlin
HorizontalDivider(Modifier.padding(vertical = 16.dp))

// Skills section
Text("Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
Spacer(Modifier.height(8.dp))

hero.skills.forEach { skill ->
    SkillCard(skill = skill, hero = hero, level = partyMember.level)
}

// Ultimate
Spacer(Modifier.height(12.dp))
SkillCard(skill = hero.ultimate, hero = hero, level = partyMember.level, isUltimate = true)
```

### SkillCard composable

```kotlin
@Composable
private fun SkillCard(skill: Skill, hero: Hero, level: Int, isUltimate: Boolean = false) {
    val typeColor = when {
        isUltimate -> heroColor // hero's element color
        skill.healScaling != null -> Color(0xFF4CAF50)
        skill.shieldScaling != null -> Color(0xFF2196F3)
        skill.buffs.isNotEmpty() -> Color(0xFFFFD740)
        skill.damageComponents.any { it.type == DamageType.PHYSICAL } -> MaterialTheme.colorScheme.onSurface
        skill.damageComponents.any { it.type == DamageType.ELEMENTAL } -> elementToColor(skill.damageComponents.firstNotNullOf { it.element })
        else -> MaterialTheme.colorScheme.onSurface
    }

    val typeLabel = when {
        isUltimate -> "Ultimate"
        skill.healScaling != null -> "Heal"
        skill.shieldScaling != null -> "Shield"
        skill.cleanse -> "Cleanse"
        skill.statusEffects.isNotEmpty() -> "Status"
        skill.buffs.isNotEmpty() -> "Buff"
        skill.damageComponents.any { it.type == DamageType.ELEMENTAL } -> 
            skill.damageComponents.firstNotNullOf { it.element?.name } ?: "Damage"
        else -> "Physical"
    }

    val damage = skill.baseDamage + skill.damagePerLevel * (level - 1)
    val healAmount = skill.healScaling?.let { 
        if (it.isPercentage) "${it.baseHeal}%" else "${it.baseHeal + it.healPerLevel * (level - 1)}"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = typeColor.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            // Type indicator
            Box(
                modifier = Modifier.size(8.dp, 36.dp)
                    .background(typeColor, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        skill.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                    if (isUltimate) {
                        Spacer(Modifier.width(4.dp))
                        Text("⭐", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    skill.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (damage > 0) {
                        Text("${damage} dmg", style = MaterialTheme.typography.labelSmall, color = typeColor)
                    }
                    if (healAmount != null) {
                        Text("Heal $healAmount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                    if (skill.cooldown > 0) {
                        Text("CD: ${skill.cooldown}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    if (skill.cleanse) {
                        Text("Cleanses", style = MaterialTheme.typography.labelSmall, color = Color(0xFFAB47BC))
                    }
                }
            }
            // Ultimate gauge gain
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

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/java/com/example/game/ui/components/PartyScreen.kt` | Add `SkillCard` composable and skills section to `HeroDetailsDialog` |
