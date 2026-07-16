# Element Effectiveness — Implementation Plan

## Summary

Add an element effectiveness system to the YogaFlow Android app. Three goals:
1. **Fill element chart gaps** — ensure every monster element has at least one hero element that deals 1.5× damage against it.
2. **Add utility lookup** — derive weakness/strength lists from the element chart so the UI can query them without duplicating data.
3. **Surface effectiveness in UI** — show element counters in battle and on the monster-info dialog.

## Current State Audit

### Hero elements (the only available attacker elements for players)
| Hero | Element |
|------|---------|
| Shanti (id=1) | WATER |
| Santosha (id=2) | EARTH |
| Virya (id=3) | FIRE |
| Dhairya (id=4) | LIGHT |
| Maitri (id=5) | AIR |

### Monster elements found in `monsters.json`
SHADOW, WATER, ELECTRIC, EARTH, DARK, FIRE, LIGHT, VOID (8 elements total).

### Element chart gaps
The current `elementChart` in `DamageCalculator.kt:18-27` leaves two monster elements **uncountered** (no hero element deals 1.5× vs them):

| Monster element | Current counters from hero elements | Gap? |
|-----------------|--------------------------------------|------|
| SHADOW | LIGHT (1.5×) — Dhairya | Covered |
| WATER | EARTH (1.5×) — Santosha | Covered |
| ELECTRIC | EARTH (0.5× to attacker)* — but EARTH > ELECTRIC not in chart. No hero counters ELECTRIC. | Add EARTH > ELECTRIC |
| EARTH | AIR (1.5×) — Maitri | Covered |
| DARK | LIGHT (1.5×) — Dhairya | Covered |
| FIRE | WATER (1.5×) — Shanti | Covered |
| **LIGHT** | DARK (1.5×) — no hero has DARK. VOID (1.5×) — no hero has VOID. **No hero counter exists.** | Add counter |
| **VOID** | Nothing listed. VOID has no weakness in the chart. **No hero counter exists.** | Add counter |

NOTE: ELECTRIC < EARTH (0.5×) means ELECTRIC attacks deal 0.5× vs EARTH defenders, NOT that EARTH deals bonus vs ELECTRIC. To counter ELECTRIC monsters we need `EARTH > ELECTRIC (1.5×)`.

### Fixes needed in element chart
1. **VOID weakness**: Add `AIR > VOID (1.5×)` — Maitri counters VOID monsters (Klesh, Samsara). Thematically: breath fills the void.
2. **VOID weakness reciprocal**: Add `VOID < AIR (0.5×)` — consistency with chart conventions.
3. **LIGHT weakness**: Add `EARTH > LIGHT (1.5×)` — Santosha counters LIGHT monsters (Abhimana, Mada, Ahankara). Thematically: earth blocks/dims light.
4. **LIGHT weakness reciprocal**: Add `LIGHT < EARTH (0.5×)` — consistency.
5. **ELECTRIC weakness**: Add `EARTH > ELECTRIC (1.5×)` — Santosha also counters ELECTRIC (Chinta, Irsya). Thematically: earth grounds electricity.
6. **ELECTRIC weakness reciprocal**: Add `ELECTRIC < EARTH (0.5×)` already exists. Only need EARTH's attacker-side entry.
7. **SHADOW weakness** (cosmetic): Add `SHADOW < LIGHT (0.5×)` — already implied by LIGHT > SHADOW, but adding here makes the utility function derivable symmetrically.

### Dependency order
Steps must be implemented in this order:
1. DamageCalculator.kt (element chart update) — foundational
2. Element.kt (utility extension functions) — depends on the chart logic being complete
3. BattleHUD.kt (monster element badge + counter icons in battle) — depends on utilities
4. MonsterRoadSelection.kt (effectiveness info in MonsterConfirmDialog) — depends on utilities

---

## Step 1 — Update Element Chart

**File**: `app/src/main/java/com/example/game/battle/DamageCalculator.kt`

**Lines 18–27** — The `elementChart` map inside `DamageCalculator`.

### Changes

Replace the current `elementChart` block with the following (adds the new entries):

```kotlin
    private val elementChart: Map<Element, Map<Element, Float>> = mapOf(
        Element.FIRE to mapOf(Element.AIR to 1.5f, Element.WATER to 0.5f),
        Element.WATER to mapOf(Element.FIRE to 1.5f, Element.EARTH to 0.5f),
        Element.AIR to mapOf(Element.EARTH to 1.5f, Element.FIRE to 0.5f, Element.VOID to 1.5f),
        Element.EARTH to mapOf(Element.WATER to 1.5f, Element.AIR to 0.5f, Element.LIGHT to 1.5f, Element.ELECTRIC to 1.5f),
        Element.LIGHT to mapOf(Element.DARK to 1.5f, Element.SHADOW to 1.5f, Element.VOID to 0.5f, Element.EARTH to 0.5f),
        Element.DARK to mapOf(Element.LIGHT to 1.5f, Element.VOID to 0.5f),
        Element.SHADOW to mapOf(Element.LIGHT to 1.5f, Element.LIGHT to 0.5f),
        Element.ELECTRIC to mapOf(Element.WATER to 1.5f, Element.EARTH to 0.5f),
        Element.VOID to mapOf(Element.LIGHT to 1.5f, Element.DARK to 1.5f, Element.AIR to 0.5f)
    )
```

Wait — the SHADOW line is wrong: you cannot have two `Element.LIGHT` keys in a map literal. The SHADOW entry needs both `LIGHT to 1.5f` (SHADOW strong vs LIGHT) and `LIGHT to 0.5f` (SHADOW weak vs LIGHT). You can't have both keys. So we skip adding the self-weakness for SHADOW. The existing `LIGHT > SHADOW (1.5×)` already covers the counter. SHADOW's own entry stays as `mapOf(Element.LIGHT to 1.5f)`.

**Final correct chart** — replace lines 18–27 with:

```kotlin
    private val elementChart: Map<Element, Map<Element, Float>> = mapOf(
        Element.FIRE to mapOf(Element.AIR to 1.5f, Element.WATER to 0.5f),
        Element.WATER to mapOf(Element.FIRE to 1.5f, Element.EARTH to 0.5f),
        Element.AIR to mapOf(Element.EARTH to 1.5f, Element.FIRE to 0.5f, Element.VOID to 1.5f),
        Element.EARTH to mapOf(Element.WATER to 1.5f, Element.AIR to 0.5f, Element.LIGHT to 1.5f, Element.ELECTRIC to 1.5f),
        Element.LIGHT to mapOf(Element.DARK to 1.5f, Element.SHADOW to 1.5f, Element.VOID to 0.5f, Element.EARTH to 0.5f),
        Element.DARK to mapOf(Element.LIGHT to 1.5f, Element.VOID to 0.5f),
        Element.SHADOW to mapOf(Element.LIGHT to 1.5f),
        Element.ELECTRIC to mapOf(Element.WATER to 1.5f, Element.EARTH to 0.5f),
        Element.VOID to mapOf(Element.LIGHT to 1.5f, Element.DARK to 1.5f, Element.AIR to 0.5f)
    )
```

**Summary of what was added**:
- `Element.AIR` map: `Element.VOID to 1.5f`
- `Element.EARTH` map: `Element.LIGHT to 1.5f`, `Element.ELECTRIC to 1.5f`
- `Element.LIGHT` map: `Element.EARTH to 0.5f`
- `Element.VOID` map: `Element.AIR to 0.5f`

### Verification
After this change, every monster element is countered (has a hero element that deals 1.5× against it):

| Monster element | Counter hero | Element |
|-----------------|-------------|---------|
| SHADOW | Dhairya | LIGHT |
| WATER | Santosha | EARTH |
| ELECTRIC | Santosha | EARTH |
| EARTH | Maitri | AIR |
| DARK | Dhairya | LIGHT |
| FIRE | Shanti | WATER |
| LIGHT | Santosha | EARTH |
| VOID | Maitri | AIR |

All 8 monster elements now have at least one hero counter. No hero elements were added or changed.

---

## Step 2 — Add Element Utility Functions

**File**: `app/src/main/java/com/example/game/model/Skill.kt`

**Add after line 7** (after the `Element` enum declaration ends at `}` on line 7, but before the `val Element.color` extension on line 9):

Insert a companion object block with utility methods on `Element`:

```kotlin
enum class Element {
    FIRE, WATER, AIR, EARTH, LIGHT, DARK, SHADOW, ELECTRIC, VOID, NEUTRAL;

    companion object {
        /**
         * Map of (defender element) -> list of (attacker element) that deal 1.5x damage.
         * Derived from DamageCalculator.elementChart.
         * Each entry means: if you are fighting an X defender, these Y attackers deal bonus damage.
         */
        val weaknesses: Map<Element, List<Element>> = mapOf(
            FIRE to listOf(WATER),
            WATER to listOf(EARTH),
            AIR to listOf(FIRE),
            EARTH to listOf(AIR),
            LIGHT to listOf(DARK, SHADOW, VOID), // wait, defending LIGHT: DARK > LIGHT (1.5), SHADOW > LIGHT (1.5). But VOID > LIGHT (1.5) checks out. No — wait.
            // Actually, for getElementMultiplier(attacker, defender), we return elementChart[attacker]?.get(defender). So to find "what attacks a defender for bonus":
            // We need to look at all attackers and check if their chart maps to this defender with 1.5f.
        )
    }
}
```

Wait — I need to think about this more carefully. The `elementChart` is `Map<attackerElement, Map<defenderElement, Float>>`. We want a function like: given a defender element, what attacker elements deal bonus damage (1.5x)?

Similarly: given an attacker element, what defender elements do they deal bonus damage against?

And importantly: we want to filter to only HERO elements (available attacker pool) when showing counters to the player.

Rather than adding a companion object with a duplicate map (which could get out of sync), we should make the element chart accessible and derive the utilities from it. But the chart is `private` inside `DamageCalculator`. Options:
A. Move the chart to a top-level file or companion object on `Element`.
B. Keep the chart in DamageCalculator but expose a public utility object that references it.
C. Put the derived functions in DamageCalculator as public methods.

**Recommended approach (Option C)**: Keep the chart as the single source of truth inside `DamageCalculator`. Add public companion functions for weakness/strength lookups. This avoids duplicating data.

But DamageCalculator is `internal class`, and the companion utilities should be accessible from UI composables.

**Alternative (simpler)**: Create a new file `app/src/main/java/com/example/game/battle/ElementRelations.kt` that contains a top-level `elementChart` val (or copy it from DamageCalculator) plus the utility functions. Then import it in both DamageCalculator and the UI.

Actually, the simplest approach that avoids duplication: Make `elementChart` a package-private (internal) top-level val in `DamageCalculator.kt`, outside the class, and add extension functions on `Element` in the same file. The UI code imports from the `battle` package.

**Final recommended approach**:

In `DamageCalculator.kt`, after the imports and before the `BattleTuning` object (around line 5-6), add:

```kotlin
/**
 * Single source of truth for elemental effectiveness.
 * Outer key = attacker element. Inner key = defender element. Value = damage multiplier.
 */
internal val elementChart: Map<Element, Map<Element, Float>> = mapOf(
    // ... same as in DamageCalculator
)

/**
 * Returns all elements that deal 1.5x damage to the given [defenderElement].
 * Call this to find which hero elements counter a monster.
 */
fun Element.countersFor(defenderElement: Element): List<Element> {
    return Element.entries.filter { attacker ->
        (elementChart[attacker]?.get(defenderElement) ?: 1f) > 1f
    }
}

/**
 * Returns all elements that receive 1.5x damage from the given [attackerElement].
 */
fun Element.targetsFor(attackerElement: Element): List<Element> {
    return Element.entries.filter { defender ->
        (elementChart[attackerElement]?.get(defender) ?: 1f) > 1f
    }
}

/**
 * Returns the multiplier for a specific matchup: (heroAttacker, monsterDefender) -> multiplier.
 */
fun getMatchupMultiplier(attacker: Element, defender: Element): Float {
    return elementChart[attacker]?.get(defender) ?: 1f
}
```

Then update the `DamageCalculator.elementChart` field on line 18 to simply reference the top-level val:

```kotlin
internal class DamageCalculator(private val rng: RandomProvider) {

    private val elementChart = com.example.game.battle.elementChart  // re-use top-level
```

Actually, since the field name is the same, Kotlin would shadow. Simpler: remove the `private val elementChart` from `DamageCalculator` and instead use `getElementMultiplier` to delegate to the top-level map. Or just reference the top-level map directly.

Wait — the current `getElementMultiplier` uses `elementChart[attacker]?.get(defender)`. If we rename the top-level val to avoid collision:

```kotlin
internal val elementalChart: Map<Element, Map<Element, Float>> = mapOf( ... )
```

Then in `DamageCalculator`, delete lines 18–27 (the `private val elementChart` field) and update `getElementMultiplier` (line 30–32) to:

```kotlin
    fun getElementMultiplier(attacker: Element, defender: Element): Float {
        return elementalChart[attacker]?.get(defender) ?: 1f
    }
```

### Changes to `DamageCalculator.kt` (lines 5–7, 18–32):

**Insert after line 4** (`import com.example.game.model.*`):

```kotlin

/**
 * Elemental effectiveness chart — single source of truth.
 * Outer key = attacker element. Inner key = defender element. Value = damage multiplier.
 * 1.5f = super effective, 0.5f = not very effective, 1f = neutral (not listed).
 */
internal val elementalChart: Map<Element, Map<Element, Float>> = mapOf(
    Element.FIRE to mapOf(Element.AIR to 1.5f, Element.WATER to 0.5f),
    Element.WATER to mapOf(Element.FIRE to 1.5f, Element.EARTH to 0.5f),
    Element.AIR to mapOf(Element.EARTH to 1.5f, Element.FIRE to 0.5f, Element.VOID to 1.5f),
    Element.EARTH to mapOf(Element.WATER to 1.5f, Element.AIR to 0.5f, Element.LIGHT to 1.5f, Element.ELECTRIC to 1.5f),
    Element.LIGHT to mapOf(Element.DARK to 1.5f, Element.SHADOW to 1.5f, Element.VOID to 0.5f, Element.EARTH to 0.5f),
    Element.DARK to mapOf(Element.LIGHT to 1.5f, Element.VOID to 0.5f),
    Element.SHADOW to mapOf(Element.LIGHT to 1.5f),
    Element.ELECTRIC to mapOf(Element.WATER to 1.5f, Element.EARTH to 0.5f),
    Element.VOID to mapOf(Element.LIGHT to 1.5f, Element.DARK to 1.5f, Element.AIR to 0.5f)
)

/**
 * Returns all elements that deal 1.5x damage to the given [defender].
 * Use to find which hero elements counter a monster element.
 */
fun countersFor(defender: Element): List<Element> {
    return Element.entries.filter { attacker ->
        (elementalChart[attacker]?.get(defender) ?: 1f) > 1f
    }
}

/**
 * Returns all elements that the given attacker deals 1.5x damage to.
 */
fun strongAgainst(attacker: Element): List<Element> {
    return elementalChart[attacker]?.filter { it.value > 1f }?.keys?.toList() ?: emptyList()
}

/**
 * Convenience: multiplier for a specific attacker-vs-defender matchup.
 */
fun matchupMultiplier(attacker: Element, defender: Element): Float {
    return elementalChart[attacker]?.get(defender) ?: 1f
}
```

**Delete lines 18–27** (the `private val elementChart` field inside `DamageCalculator`).

**Update lines 30–32** (`getElementMultiplier`) to:

```kotlin
    fun getElementMultiplier(attacker: Element, defender: Element): Float {
        return elementalChart[attacker]?.get(defender) ?: 1f
    }
```

### Also add a helper extension on `Element` in `Skill.kt`:

**In `app/src/main/java/com/example/game/model/Skill.kt`**, after the `val Element.color` block (line 21), add:

```kotlin
/**
 * Human-readable icon/name for the element, suitable for UI labels.
 */
val Element.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }
```

This is optional but nice for the UI — gives "Fire", "Water", etc.

---

## Step 3 — Show Element Counters in Battle HUD

**File**: `app/src/main/java/com/example/game/ui/components/BattleHUD.kt`

### Changes

**Add import at line 23** (after `import com.example.game.model.*`):

```kotlin
import com.example.game.battle.countersFor
```

### Modify `MonsterHUD` composable (line 224–226):

Currently:
```kotlin
@Composable
fun MonsterHUD(monster: CombatantState, statuses: List<BattleStatus>, modifier: Modifier = Modifier) {
    FloatingHUD(monster.name, monster.hp, monster.maxHp, modifier, monster.shield, null, monster.element, statuses, false, width = 100, hpBarColor = Color.Red)
}
```

Replace with:
```kotlin
@Composable
fun MonsterHUD(
    monster: CombatantState,
    statuses: List<BattleStatus>,
    modifier: Modifier = Modifier,
    heroElements: List<Element> = emptyList()  // New parameter
) {
    val counters = remember(monster.element, heroElements) {
        countersFor(monster.element).filter { it in heroElements }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingHUD(monster.name, monster.hp, monster.maxHp, Modifier, monster.shield, null, monster.element, statuses, false, width = 100, hpBarColor = Color.Red)

        // Element badge with counter icons
        ElementBadge(
            element = monster.element,
            counters = counters,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
```

### Add a new `ElementBadge` composable to `BattleHUD.kt`

Add after the `MonsterHUD` function (around line 226), before the closing of the file:

```kotlin
@Composable
fun ElementBadge(
    element: Element,
    counters: List<Element> = emptyList(),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Element indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(element.color, CircleShape)
        )

        // Counter elements shown as small colored dots
        if (counters.isNotEmpty()) {
            Text(
                text = "←",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Light
            )
            counters.forEach { counterElement ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(counterElement.color, CircleShape)
                )
            }
        }
    }
}
```

Requires adding these imports to BattleHUD.kt if not already present:
- `import androidx.compose.foundation.background`
- `import androidx.compose.foundation.shape.CircleShape`
- `import androidx.compose.ui.unit.sp`
- `import androidx.compose.ui.text.font.FontWeight`

These are already present in BattleHUD.kt.

### Update `BattleScene.kt` to pass hero elements to `MonsterHUD`

**File**: `app/src/main/java/com/example/game/ui/components/BattleScene.kt`

**Around line 274** — where `MonsterHUD` is called:

```kotlin
MonsterHUD(
    monster = monster,
    statuses = state.getStatusesForTarget(monster.id),
    modifier = Modifier.padding(bottom = 8.dp)
)
```

Change to:
```kotlin
MonsterHUD(
    monster = monster,
    statuses = state.getStatusesForTarget(monster.id),
    modifier = Modifier.padding(bottom = 8.dp),
    heroElements = state.heroes.map { it.element }.distinct()
)
```

---

## Step 4 — Show Element Effectiveness in Monster Confirm Dialog

**File**: `app/src/main/java/com/example/game/ui/components/MonsterRoadSelection.kt`

### Changes

**Add import** after line 31 (`import com.example.game.persistence.DataLoader`):

```kotlin
import com.example.game.battle.countersFor
```

### Modify `MonsterConfirmDialog` composable

**Location**: lines 687–817 in `MonsterRoadSelection.kt`.

After the stats block (lines 768–773 — the `StatChip` Row), add an element effectiveness section.

**Insert between line 773** (closing `}` of the Stats Row) **and line 775** (the blank line before the "Active Party" heading):

```kotlin
                // ── Element Effectiveness ──
                val monsterCounters = remember(monster.element) {
                    val allCounters = countersFor(monster.element)
                    allCounters.filter { counter ->
                        DataLoader.heroes.any { it.element == counter }
                    }
                }

                if (monsterCounters.isNotEmpty()) {
                    Text(
                        "Element Effectiveness",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(12.dp).background(monster.element.color, CircleShape))
                        Text(
                            monster.element.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = monster.element.color
                        )
                        Text("is weak to", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }

                    monsterCounters.forEach { counterElement ->
                        val counterHeroes = DataLoader.heroes.filter { it.element == counterElement }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).background(counterElement.color, CircleShape))
                            Text(
                                counterElement.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = counterElement.color
                            )
                            Text(
                                "◎",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                counterHeroes.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Also show what the monster is strong against (heroes weak to monster)
                    val strongAgainstHeroes = DataLoader.heroes.filter { hero ->
                        val mult = com.example.game.battle.matchupMultiplier(monster.element, hero.element)
                        mult > 1f
                    }
                    if (strongAgainstHeroes.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).background(monster.element.color, CircleShape))
                            Text(
                                "Deals bonus damage to:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                strongAgainstHeroes.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEF5350)
                            )
                        }
                    }
                }
```

**Note**: Requires `import kotlinx.coroutines.remember` (already imported) and `import com.example.game.battle.matchupMultiplier`.

Also need to add to the imports block (around line 3-33):
```kotlin
import com.example.game.battle.matchupMultiplier
```

---

## Step 5 — Verify with Tests

Run existing tests to ensure no regressions:

```bash
./gradlew test
```

The element chart change should be transparent to existing tests since the multipliers are only accessed through `getElementMultiplier` which still works identically (only new entries added; old entries unchanged).

If there are specific `DamageCalculator` tests, verify they pass:

```bash
./gradlew test --tests "*DamageCalculator*"
```

---

## Implementation Order & Files Summary

| Order | File | Change |
|-------|------|--------|
| 1 | `app/src/main/java/com/example/game/battle/DamageCalculator.kt` | Move `elementChart` to top-level `elementalChart` val; add 4 new relationships (AIR>VOID, EARTH>LIGHT, EARTH>ELECTRIC, LIGHT<EARTH, VOID<AIR); add `countersFor`, `strongAgainst`, `matchupMultiplier` top-level functions |
| 2 | `app/src/main/java/com/example/game/model/Skill.kt` | Add `val Element.displayName` extension (optional but nice) |
| 3 | `app/src/main/java/com/example/game/ui/components/BattleHUD.kt` | Add `ElementBadge` composable; update `MonsterHUD` to accept `heroElements` parameter and show counter dots |
| 4 | `app/src/main/java/com/example/game/ui/components/BattleScene.kt` | Pass `heroElements` to `MonsterHUD` call |
| 5 | `app/src/main/java/com/example/game/ui/components/MonsterRoadSelection.kt` | Add element effectiveness section to `MonsterConfirmDialog` |

### Risk Notes
- `DamageCalculator` is `internal`; moving the chart to top-level is safe since Kotlin `internal` means visible within the same module. The UI composables are in the same module.
- The `countersFor` function only considers multipliers > 1.0f (i.e., 1.5×). It ignores neutral (1.0×) and resistances (0.5×).
- None of the changes modify hero or monster data files — only the element relationships and UI.
