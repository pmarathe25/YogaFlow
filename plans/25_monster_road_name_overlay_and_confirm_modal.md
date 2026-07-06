# Plan 25: Monster Road — Name Overlay & Confirmation Modal

## Problem

The Path of Zen map has two UX gaps:

1. **No monster names on the path** — the map shows only icon silhouettes (and an incomplete boss circle at line 625 that draws a dot but no text). Players must tap nodes to discover what they face.
2. **No confirmation before battle** — tapping a node immediately launches the battle. Players can't review the monster's stats, their party composition, or any description before committing.

## Changes

### 1. Monster name labels on path nodes

**`MonsterRoadSelection.kt:drawNodes()`** — before drawing each node's silhouette, draw the monster's `name` above the circle using native Canvas text:

```kotlin
// In drawNodes() before the silhouette draw (line 558):
if (!isLocked) {
    val namePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 11f * dpScale
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    val defeatedPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 10f * dpScale
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isStrikeThruText = isDefeated
    }
    val paint = if (isDefeated) defeatedPaint else namePaint
    val labelY = cy - nodeScale * 1.4f
    drawContext.canvas.nativeCanvas.drawText(
        monster.name, cx, labelY, paint
    )
}
```

This replaces the incomplete "Boss name label" section (lines 624-628) — remove that block since the name is now drawn for all unlocked nodes.

The existing "Active pulsing pointer" (line 611) stays in the same position (under the icon circle) since the name is now above. Change the arrow to point **up** toward the node instead of down:

```kotlin
// Arrow tip at the top, base at the bottom
val arrPath = Path().apply {
    moveTo(cx - 8f * dpScale, arrowY + 10f * dpScale)  // bottom-left
    lineTo(cx, arrowY)                                    // tip (up)
    lineTo(cx + 8f * dpScale, arrowY + 10f * dpScale)    // bottom-right
    close()
}
```

This flips the triangle so the tip points at the node instead of away from it.

### 2. Confirmation dialog on tap

**`MonsterRoadSelection.kt:41-155`** — replace instant `onMonsterSelected(monster)` with a dialog:

#### New parameter

```kotlin
@Composable
fun MonsterRoadSelection(
    monsters: List<Monster>,
    defeatedIds: Set<String>,
    partyMembers: List<PartyMemberData>,   // NEW
    onMonsterSelected: (Monster) -> Unit,
    onBack: () -> Unit
)
```

#### Internal state

```kotlin
var selectedMonster by remember { mutableStateOf<Monster?>(null) }
```

Change the click handler (line 149):
```diff
- ) { onMonsterSelected(monster) }
+ ) { selectedMonster = monster }
```

#### Dialog composable (add at end of file)

```kotlin
@Composable
private fun MonsterConfirmDialog(
    monster: Monster,
    partyMembers: List<PartyMemberData>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val elColor = elementToColor(monster.element)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Header: monster name + tier badge ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Element-colored circle
                    Box(Modifier.size(12.dp).background(elColor, CircleShape))

                    Text(
                        monster.englishName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Difficulty tier badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (monster.difficultyTier) {
                            DifficultyTier.BOSS, DifficultyTier.SUPERBOSS -> Color(0xFFFFD700).copy(alpha = 0.2f)
                            else -> elColor.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            monster.difficultyTier.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (monster.difficultyTier) {
                                DifficultyTier.BOSS, DifficultyTier.SUPERBOSS -> Color(0xFFFFD700)
                                else -> elColor
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // ── Flavor Text ──
                // Prefer flavorText (lore), fall back to mechanicDescription (tactical).
                val flavor = if (monster.flavorText.isNotBlank()) monster.flavorText else monster.mechanicDescription
                Text(
                    flavor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // ── Stats ──
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip("HP", "${monster.baseHp}", Color(0xFF4CAF50))
                    StatChip("ATK", "${monster.baseAtk}", Color(0xFFF44336))
                    StatChip("SPD", "${monster.baseSpd}", Color(0xFF2196F3))
                }

                // ── Party heroes ──
                Text(
                    "Active Party",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (partyMembers.isEmpty()) {
                    Text(
                        "No heroes in party!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    partyMembers.forEach { pm ->
                        val heroDef = DataLoader.heroes.find { it.id == pm.heroId }
                        if (heroDef != null) {
                            PartyHeroRow(heroDef = heroDef, pm = pm)
                        }
                    }
                }

                // ── Confirm / Cancel ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = partyMembers.isNotEmpty()
                    ) { Text("Enter Battle") }
                }
            }
        }
    }
}
```

#### Helper composables

```kotlin
@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun PartyHeroRow(heroDef: Hero, pm: PartyMemberData) {
    val heroColor = elementToColor(heroDef.element)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Small hero silhouette
        Canvas(Modifier.size(28.dp)) {
            val c = size.width / 2f
            drawCircle(heroColor.copy(alpha = 0.2f), c, Offset(c, c))
            drawMonsterShape(
                cx = c, cy = c * 0.9f, s = c * 0.8f,
                name = heroDef.name, tint = heroColor.copy(alpha = 0.8f)
            )
        }

        Column {
            Text(heroDef.name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Text("Lv.${pm.level} ${heroDef.element.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}
```

### 3. Update callers

**`ExpandedDashboardScreen` in JourneyScreen.kt:77-87** — add the party parameter; use `gameViewModel.party.collectAsState().value` or pass it from the existing `gameSaveData.party`:

```diff
 MonsterRoadSelection(
     monsters = DataLoader.monsters,
     defeatedIds = gameSaveData.defeatedMonsterIds,
+    partyMembers = gameSaveData.party,
     onMonsterSelected = { monster ->
         gameViewModel.startBattle(monster.id)
         onNavigateToBattle()
         showMonsterRoad = false
     },
     onBack = { showMonsterRoad = false }
 )
```

**`HubScreen.kt`** (if not removed by Plan 21) — similar change:

```diff
 MonsterRoadSelection(
     monsters = DataLoader.monsters,
     defeatedIds = saveData.defeatedMonsterIds,
+    partyMembers = saveData.party,
     onMonsterSelected = { onNavigateToBattle(it.id) },
     onBack = onExitHub
 )
```

### 4. Add `flavorText` to every monster

Add a `flavorText` field to each monster entry in **`monsters.json`**, and add the field to the **`Monster.kt`** data class. The dialog displays `flavorText` with `mechanicDescription` as fallback.

#### Monster.kt

```diff
 data class Monster(
     val id: String,
     val name: String,
     val englishName: String,
     val element: Element,
     val baseHp: Int,
     val baseAtk: Int,
     val baseSpd: Int,
     val specialAttack: Skill,
     val mechanicDescription: String,
+    val flavorText: String = "",
     val aiBehavior: AIBehavior,
```

#### Flavor text for each monster

| Monster | English Name | Tier | `flavorText` |
|---|---|---|---|
| Bhaya | Self-Doubt & Fear | EASY | "The first shadow on the path. It whispers that you are not ready." |
| Tandra | Fatigue & Lethargy | EASY | "A heavy fog that seeps into your bones, whispering that rest is all you need." |
| Chinta | Anxiety & Worry | EASY | "A restless swarm of what-ifs, each sting eroding your peace." |
| Alasya | Sloth & Laziness | EASY | "A comfortable trap that convinces you motion is pointless." |
| Matsarya | Envy & Jealousy | MEDIUM | "A serpent coiled around your heart, feeding on the success of others." |
| Krodha | Anger & Rage | MEDIUM | "A wildfire that burns all reason. It strikes hardest when wounded." |
| Dvesha | Aversion & Hatred | MEDIUM | "A relentless force that seeks what it despises and strikes without mercy." |
| Moha | Delusion & Attachment | HARD | "Reality bends in its presence. Trust nothing your eyes show you." |
| Lobha | Greed & Attachment | HARD | "An insatiable hunger that turns even kindness into a possession." |
| Abhimana | Conceit & Pride | HARD | "Tall and unyielding, it feeds on your own reflection." |
| Mada | Pride & Vanity | HARD | "A radiant lie that turns healing into harm." |
| Irsya | Jealousy & Resentment | HARD | "It watches, learns, and turns your own strength against you." |
| Ahankara | Ego & Vanity | BOSS | "The grand illusion of self. It guards the deepest truths with mirrored walls." |
| Maya | Illusion & Deception | BOSS | "Nothing here is real — least of all the enemy you see." |
| Klesh | Turmoil & Affliction | BOSS | "The root of all suffering given form. It fights with chaos itself." |
| Samsara | Cycle of Suffering | SUPERBOSS | "The endless wheel. Every victory is a beginning, every defeat a lesson." |

## Files to modify

| File | Changes |
|---|---|---|
| `MonsterRoadSelection.kt` | Draw monster names in `drawNodes()`; remove incomplete "Boss name label" block; add `partyMembers` parameter; add `selectedMonster` state; add dialog + helper composables; flip active arrow to point up |
| `JourneyScreen.kt` | Pass `partyMembers = gameSaveData.party` to `MonsterRoadSelection` |
| `HubScreen.kt` | Pass `partyMembers = saveData.party` to `MonsterRoadSelection` (only if HubScreen still exists after Plan 21) |
| `monsters.json` | Add `flavorText` field to all 16 monsters |
| `Monster.kt` | Add `val flavorText: String = ""` to data class |

## Dependencies

- None. Independent of other plans.
