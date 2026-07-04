# Plan: Human-Readable Stats + Level-Up Details Pop-out

## Part A: Better Stats Display

**File:** `app/src/main/java/com/example/game/ui/components/PartyScreen.kt:211-216`

The current stats display shows HP/ATK/SPD as bare numbers with icons. Replace with a more readable format that includes the stat name, the base value, and any modifiers from equipment.

### Current
```
❤️ HP    ⚡ ATK    📈 SPD
320      45        14
```

### Proposed
```
┌─────────────────────────────────┐
│  Base Stats (Lv.3)              │
│                                 │
│  ❤️  HP      320  (+45 from gear)│
│  ⚔️  ATK      45  (+12% from gear)│
│  💨  SPD      14                │
│  ⚡  Crit      5%               │
│  🛡️  Status    10%              │
│     Resistance                  │
└─────────────────────────────────┘
```

The specific additional stats shown depend on what equipment is equipped. Iterate over `getEquippedItems(hero.id)` and collect unique effect types to show their contributions.

Simpler alternative: show a stat block with HP/ATK/SPD as bars rather than numbers, making it more visual:

```kotlin
@Composable
private fun StatBar(label: String, value: Int, maxValue: Int, color: Color, icon: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
        LinearProgressIndicator(
            progress = { value.toFloat() / maxValue.toFloat() },
            modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Spacer(Modifier.width(8.dp))
        Text("$value", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
```

This requires a `maxValue` for each stat. Use an arbitrary baseline (e.g., HP 500, ATK 80, SPD 25) or compute from the hero's own base stats at max level.

## Part B: Level-Up Details Pop-out

**File:** `PartyScreen.kt:219-229`

The current level-up button shows cost and immediately upgrades. Replace it with a button that opens a confirmation dialog showing what changes.

### New flow

1. "Level Up" button shows the cost as before
2. Tapping opens a **LevelUpDialog** composable
3. The dialog displays:

```
┌──────────────────────────────────┐
│     Level Up: Shanti?           │
│  ──────────────────────────────  │
│  Current (Lv.3)    →  Next (Lv.4)│
│  ──────────────────────────────  │
│  HP:  320         →  368(+48)   │
│  ATK: 45          →   52 (+7)   │
│  SPD: 14          →   16 (+2)   │
│  ──────────────────────────────  │
│  Skills:                         │
│  Gentle Strike: 8 → 10 damage   │
│  Pranayama: 35 → 40 heal        │
│  ──────────────────────────────  │
│  Cost: 300 sparks ✦              │
│  [Cancel]         [Confirm]     │
└──────────────────────────────────┘
```

### Implementation

```kotlin
@Composable
fun LevelUpDialog(
    hero: Hero,
    partyMember: PartyMemberData,
    heroColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentStats = computeHeroStats(hero, partyMember.level)
    val nextStats = computeHeroStats(hero, partyMember.level + 1)
    val currentSkills = hero.skills.map { it.baseDamage }
    val nextSkills = hero.skills.map { it.baseDamage + it.damagePerLevel }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title
                Text("Level Up ${hero.name.take(1)}?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // Stats comparison table
                Text("Stats", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                StatComparisonRow("HP", currentStats.maxHp, nextStats.maxHp, Color.Red)
                StatComparisonRow("ATK", currentStats.atk, nextStats.atk, Color(0xFFFFA500))
                StatComparisonRow("SPD", currentStats.spd, nextStats.spd, Color.Cyan)

                Spacer(Modifier.height(16.dp))

                // Skill improvements
                Text("Skills", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                hero.skills.forEachIndexed { index, skill ->
                    SkillComparisonRow(skill, partyMember.level)
                }

                Spacer(Modifier.height(20.dp))

                // Cost & buttons
                val cost = viewModel.getHeroLevelUpCost(hero.id)
                Text("Cost: $cost sparks ✦", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = heroColor)) { Text("Confirm") }
                }
            }
        }
    }
}

@Composable
private fun StatComparisonRow(label: String, current: Int, next: Int, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("$current", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(" → ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text("$next", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        Text(" (+${next - current})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun SkillComparisonRow(skill: Skill, currentLevel: Int) {
    val current = skill.baseDamage + skill.damagePerLevel * (currentLevel - 1)
    val next = skill.baseDamage + skill.damagePerLevel * currentLevel
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(skill.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (skill.damagePerLevel > 0) {
            Text("${current} → ${next} dmg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        } else if (skill.healScaling != null) {
            val curHeal = skill.healScaling.baseHeal + skill.healScaling.healPerLevel * (currentLevel - 1)
            val nxtHeal = skill.healScaling.baseHeal + skill.healScaling.healPerLevel * currentLevel
            if (skill.healScaling.healPerLevel > 0) {
                Text("${curHeal} → ${nxtHeal} heal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("No change", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            Text("No change", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
```

### Wiring

In `HeroDetailsDialog`, replace the level-up `Button` with:

```kotlin
var showLevelUpDialog by remember { mutableStateOf(false) }

Button(
    onClick = { showLevelUpDialog = true },
    enabled = canLevelUp,
    ...
) {
    Text("Level Up (${levelUpCost} sparks ✦)", fontWeight = FontWeight.Bold)
}

if (showLevelUpDialog) {
    LevelUpDialog(
        hero = hero,
        partyMember = partyMember,
        heroColor = heroColor,
        onConfirm = {
            viewModel.levelUpHero(hero.id)
            showLevelUpDialog = false
        },
        onDismiss = { showLevelUpDialog = false }
    )
}
```

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/java/com/example/game/ui/components/PartyScreen.kt` | Replace stats display with `StatBar` composables; replace level-up button with `LevelUpDialog` trigger |
