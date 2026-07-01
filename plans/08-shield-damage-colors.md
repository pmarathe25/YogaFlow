# Plan: Purple Damage Numbers for Shield Damage

## Issue Addressed
- **Issue 10**: Damage done to shields should use purple numbers/animations to distinguish from damage done to health (red numbers/animations).

---

## Current Behavior

In `BattleEffects.kt`, when damage is dealt, a single floating text entry is created:

```kotlin
if (outcome.damageDealt > 0) {
    val entry = FloatingTextEntry(
        id = System.nanoTime(),
        text = "-${outcome.damageDealt}",
        color = Color.Red,
        startX = targetPos.x,
        startY = targetPos.y,
        startTime = System.currentTimeMillis()
    )
    damageNumbers.add(entry)
    pool.emit(emitterConfigForElement(...), targetPos, 15)
}
```

This shows all damage in red, regardless of whether it was absorbed by shields or health.

---

## Root Cause

The `ActionOutcome` contains `perTargetResult` with `TargetResult(damage, heal, shield, ...)`. The damage value is the **total** damage dealt (sum of shield-absorbed and HP damage). There is no split between "damage to shield" and "damage to HP" at the outcome level.

The shield/HHP split happens in `BattleEngine.applyOutcome()`:
```kotlin
if (hero.shield >= dmg) {
    hero.shield -= dmg
} else {
    val remaining = dmg - hero.shield
    hero.shield = 0
    hero.currentHp = (hero.currentHp - remaining).coerceAtLeast(0)
}
```

But the floating text is generated from the event's outcome, which only stores the total damage.

---

## Critical Note — Current Code Structure

The `BattleEffects.kt` file currently does **not** iterate `perTargetResult`. It uses `outcome.damageDealt` (a single total per `ActionOutcome`) and applies the same floating text to every target in `outcome.targets`:

```kotlin
event.outcomes.forEach { outcome ->
    outcome.targets.forEach { targetId ->
        val targetPos = heroPositions[targetId] ?: monsterPosition
        if (outcome.damageDealt > 0) {
            // Shows total damage in red for EACH target
        }
    }
}
```

This is incorrect for multi-target skills — it shows the same total damage number on every target. To properly split shield vs HP damage per target, the code must:
1. Iterate `outcome.perTargetResult` instead of `outcome.targets`
2. Use per-target `shieldDamage` to create two separate floating text entries (purple for shield, red for HP)

Same issue exists for `BattleEvent.MonsterTurn` (lines 84-101), which also uses `outcome.damageDealt` and needs per-target iteration.

Also affects combo outcomes (`BattleEvent.ComboUsed`) and monster outcomes. Any event type that carries an `ActionOutcome` with `perTargetResult` needs this treatment.

## Changes Required

### 1. `TargetResult` (in `BattleState.kt`) — Add shield damage field

Add a `shieldDamage` field to `TargetResult` to track how much damage was absorbed by shields:

```kotlin
data class TargetResult(
    val damage: Int = 0,
    val heal: Int = 0,
    val shield: Int = 0,
    val shieldDamage: Int = 0,  // NEW: damage absorbed by shield
    val statuses: List<String> = emptyList(),
    val cleansed: Boolean = false
)
```

### 2. `BattleEngine.kt` — `applyOutcome()`

When applying damage, track how much was absorbed by shields and store it in `perTargetResult`:

```kotlin
if (result.damage > 0) {
    val dmg = result.damage
    if (hero != null) {
        if (hero.shield >= dmg) {
            hero.shield -= dmg
            result.shieldDamage = dmg  // NEW: all absorbed by shield
        } else {
            val remaining = dmg - hero.shield
            result.shieldDamage = hero.shield  // NEW: portion absorbed by shield
            hero.shield = 0
            hero.currentHp = (hero.currentHp - remaining).coerceAtLeast(0)
        }
    }
}
```

Note: `TargetResult` is a data class, so `shieldDamage` can be added to its constructor. `perTargetResult` is a `Map<String, TargetResult>` which gets reassigned after `applyOutcome`. The outcome object needs to carry the split information back to the UI.

### 3. `BattleEffects.kt` — Rewrite event processing to iterate `perTargetResult`

**Replace the entire `SkillUsed` handling block** (currently lines 38-83) and the **`MonsterTurn` handling block** (currently lines 84-101) to iterate `perTargetResult` instead of `outcome.targets`:

For `SkillUsed`:
```kotlin
is BattleEvent.SkillUsed -> {
    event.outcomes.forEach { outcome ->
        outcome.perTargetResult.forEach { (targetId, result) ->
            val targetPos = heroPositions[targetId] ?: monsterPosition
            
            // Shield damage (purple)
            if (result.shieldDamage > 0) {
                damageNumbers.add(FloatingTextEntry(
                    id = System.nanoTime(),
                    text = "-${result.shieldDamage}",
                    color = Color(0xFF9C27B0),  // Purple
                    startX = targetPos.x,
                    startY = targetPos.y - 15f,
                    startTime = System.currentTimeMillis()
                ))
                pool.emit(EmitterConfig(
                    colors = listOf(Color(0xFF9C27B0), Color(0xFFCE93D8))
                ), targetPos, 10)
            }
            
            // HP damage (red)
            val hpDmg = result.damage - result.shieldDamage
            if (hpDmg > 0) {
                damageNumbers.add(FloatingTextEntry(
                    id = System.nanoTime() + 1,
                    text = "-${hpDmg}",
                    color = Color.Red,
                    startX = targetPos.x,
                    startY = targetPos.y,
                    startTime = System.currentTimeMillis()
                ))
            }
            
            // Healing (green) — use result.heal instead of outcome.healingDone
            if (result.heal > 0) {
                damageNumbers.add(FloatingTextEntry(
                    id = System.nanoTime() + 2,
                    text = "+${result.heal}",
                    color = Color.Green,
                    startX = targetPos.x,
                    startY = targetPos.y,
                    startTime = System.currentTimeMillis()
                ))
                pool.emit(EmitterConfig(colors = listOf(Color.Green)), targetPos, 10)
            }
            
            // Shield applied (cyan)
            if (result.shield > 0) {
                damageNumbers.add(FloatingTextEntry(
                    id = System.nanoTime() + 3,
                    text = "🛡️ ${result.shield}",
                    color = Color.Cyan,
                    startX = targetPos.x,
                    startY = targetPos.y,
                    startTime = System.currentTimeMillis()
                ))
                pool.emit(EmitterConfig(colors = listOf(Color.Cyan)), targetPos, 8)
            }
        }
    }
}
```

For `MonsterTurn`:
```kotlin
is BattleEvent.MonsterTurn -> {
    event.outcome.perTargetResult.forEach { (targetId, result) ->
        val targetPos = heroPositions[targetId] ?: Offset.Zero
        
        val shieldDmg = result.shieldDamage
        val hpDmg = result.damage - shieldDmg
        
        if (shieldDmg > 0) {
            damageNumbers.add(FloatingTextEntry(
                id = System.nanoTime(),
                text = "-${shieldDmg}",
                color = Color(0xFF9C27B0),
                startX = targetPos.x,
                startY = targetPos.y - 15f,
                startTime = System.currentTimeMillis()
            ))
        }
        if (hpDmg > 0) {
            damageNumbers.add(FloatingTextEntry(
                id = System.nanoTime() + 1,
                text = "-${hpDmg}",
                color = Color.Red,
                startX = targetPos.x,
                startY = targetPos.y,
                startTime = System.currentTimeMillis()
            ))
        }
        pool.emit(EmitterConfig(colors = listOf(Color.Black, Color.Red), force = 12f), targetPos, 12)
    }
}
```

**Also handle `BattleEvent.ComboUsed`** if that event type exists in the event log processing. It uses the same outcome structure and should also iterate `perTargetResult`. Add a new `when` branch or extend the existing `SkillUsed`-like handling for combos.

**⚠️ Important:** The particle emission (`pool.emit`) should only happen once per target (not once per sub-type). Place it after all damage/heal/shield checks, or emit particles only for the main damage type.

### 4. Flash colors in `BattleScreen.kt`

Also update the flash effect to use purple for shield-only damage. The current flash logic (lines 118-151) uses `monsterFlashColor = Color.Red` unconditionally for attacks. Check if the attack's damage was entirely absorbed by shields and use purple instead:

```kotlin
is BattleEvent.SkillUsed -> {
    val isAttack = event.skill.damageComponents.isNotEmpty() || event.skill.baseDamage > 0
    val flashColor = if (event.skill.healScaling != null) Color(0xFF66BB6A) 
                     else if (isAttack) Color.Red 
                     else Color(0xFF42A5F5)
    
    if (isAttack) {
        // Check if all damage was absorbed by shields
        val totalShieldDamage = event.outcomes.sumOf { outcome ->
            outcome.perTargetResult.values.sumOf { it.shieldDamage }
        }
        val totalDamage = event.outcomes.sumOf { it.damageDealt }
        if (totalShieldDamage > 0 && totalShieldDamage >= totalDamage) {
            monsterFlashColor = Color(0xFF9C27B0)  // Purple for shield-only damage
        } else {
            monsterFlashColor = Color.Red
        }
        monsterFlashAlpha = 1f
        delay(150)
        monsterFlashAlpha = 0f
    }
}
```

This is a nice-to-have refinement but secondary to the floating text changes.
