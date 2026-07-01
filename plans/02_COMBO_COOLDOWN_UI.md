# Plan: Combo Cards in Hand + Cooldown Display

**Dependencies:** None — fully independent.
**Files touched:**
- `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/ui/components/BattleActions.kt` (primary — all changes)
- `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/viewmodel/GameViewModel.kt` (minor — add `executeComboById`)
- `/home/pranav/Code/YogaFlow/app/src/main/java/com/example/game/model/ComboSkill.kt` (unchanged — reading `requiredHeroes`)

**Data model note:** `ComboSkill.requiredHeroes` stores hero **names** (e.g., "Shanti", "Santosha"). `HeroInstance.heroId` stores hero **IDs** (e.g., "shanti"). `HeroInstance.name` stores the display name (e.g., "Shanti"). The plan handles this mapping.

---

## 1.5 Integrate combos into HandOfCards

### Current architecture (BattleActions.kt)

**ActionPanel** (lines 42-265):
- Line 103: `height(400.dp)` — fixed height
- Lines 135-145: HandOfCards displays current hero's skills + ultimate
- Lines 226-242: FAB button at `Alignment.TopEnd` when `comboAvailable && selectedCardId == null && !isTargeting`
- Lines 244-263: ComboSelector Surface modal
- Line 58: `var showComboSelector by remember { mutableStateOf(false) }`

**HandOfCards** (lines 270-329):
- Line 278: `val allCards = currentHero.skills + currentHero.ultimate`
- Renders cards in a fan layout

**ComboSelector** (lines 509-567):
- Full composable with hero avatar selection + "UNLEASH ULTIMATE COMBO" button
- Called from ActionPanel's Surface modal
- Sends hero IDs to `onCombo: (Set<String>) -> Unit`

### Changes

#### Step 1: Determine available combos

In `ActionPanel`, compute which combos are available for the current hero:

```kotlin
@Composable
fun ActionPanel(
    currentHero: HeroInstance,
    heroes: List<HeroInstance>,
    monsters: List<MonsterInstance>,
    skillCooldowns: Map<String, Int>,
    comboAvailable: Boolean,         // kept for backward compat but no longer used
    onSkill: (Skill, List<String>) -> Unit,
    onUltimate: () -> Unit,
    onCombo: (Set<String>) -> Unit,  // will be replaced by onComboById
    isTargeting: Boolean,
    selectedTargets: List<String>,
    onCancelTargeting: () -> Unit,
    modifier: Modifier = Modifier
)
```

Replace `comboAvailable` and `onCombo` with `onComboById: (String) -> Unit`:

```kotlin
@Composable
fun ActionPanel(
    currentHero: HeroInstance,
    heroes: List<HeroInstance>,
    monsters: List<MonsterInstance>,
    skillCooldowns: Map<String, Int>,
    onSkill: (Skill, List<String>) -> Unit,
    onUltimate: () -> Unit,
    onComboById: (String) -> Unit,       // NEW: takes combo ID, not Set<String>
    isTargeting: Boolean,
    selectedTargets: List<String>,
    onCancelTargeting: () -> Unit,
    modifier: Modifier = Modifier
)
```

Inside ActionPanel, compute available combos:

```kotlin
// Available combos for current hero
val availableCombos = remember(heroes) {
    val aliveHeroNames = heroes
        .filter { it.currentHp > 0 && !it.isDead }
        .map { it.name }
        .toSet()
    ComboSkillDefinitions.allCombos.filter { combo ->
        // Current hero's NAME must be in requiredHeroes
        currentHero.name in combo.requiredHeroes &&
        // ALL required heroes must be alive
        combo.requiredHeroes.all { name -> name in aliveHeroNames } &&
        // ALL partners must have full combo gauge
        combo.requiredHeroes.mapNotNull { name ->
            heroes.find { it.name == name }
        }.all { it.ultimateGauge >= 100 }
    }
}
```

**IMPORTANT:** The mapping uses hero **names** ("Shanti") not IDs ("shanti"), because `ComboSkill.requiredHeroes` stores names. See `HeroInstance.name` field.

#### Step 2: Add combo cards to HandOfCards

Change `HandOfCards` call in ActionPanel (lines 135-145):

**Before:**
```kotlin
if (!isUsingSkill && !isTargeting) {
    HandOfCards(
        currentHero = currentHero,
        skillCooldowns = skillCooldowns,
        selectedCardId = selectedCardId,
        onCardSelect = { selectedCardId = if (selectedCardId == it) null else it },
        onUse = { skill -> isUsingSkill = true }
    )
}
```

**After:**
```kotlin
if (!isUsingSkill && !isTargeting) {
    HandOfCards(
        currentHero = currentHero,
        skillCooldowns = skillCooldowns,
        availableCombos = availableCombos,     // NEW
        selectedCardId = selectedCardId,
        onCardSelect = { selectedCardId = if (selectedCardId == it) null else it },
        onUse = { skill -> isUsingSkill = true },
        onComboSelect = { comboId ->           // NEW: handle combo card tap
            onComboById(comboId)
        }
    )
}
```

#### Step 3: Update HandOfCards composable

**Before (line 270-329):**
```kotlin
@Composable
fun HandOfCards(
    currentHero: HeroInstance,
    skillCooldowns: Map<String, Int>,
    selectedCardId: String?,
    onCardSelect: (String) -> Unit,
    onUse: (Skill) -> Unit
) {
    val allCards = currentHero.skills + currentHero.ultimate
    // ... fan layout rendering SkillCard for each ...
    allCards.forEachIndexed { index, skill ->
        SkillCard(
            skill = skill,
            isUltimate = isUlt,
            ultReady = ultReady,
            cooldown = cooldown,
            isSelected = isSelected,
            onClick = { onCardSelect(skill.id) },
            onUse = { onUse(skill) },
            // ... graphicsLayer transformations ...
        )
    }
}
```

**After:**
```kotlin
@Composable
fun HandOfCards(
    currentHero: HeroInstance,
    skillCooldowns: Map<String, Int>,
    availableCombos: List<ComboSkill>,      // NEW
    selectedCardId: String?,
    onCardSelect: (String) -> Unit,
    onUse: (Skill) -> Unit,
    onComboSelect: (String) -> Unit         // NEW
) {
    val allCards: List<Any> = buildList {
        // Skill cards
        currentHero.skills.forEach { add(it) }
        // Ultimate card (only if gauge is full)
        if (currentHero.ultimateGauge >= 100) {
            add(currentHero.ultimate)
        }
        // Combo cards
        availableCombos.forEach { add(it) }
    }
    val cardCount = allCards.size

    var scrollOffset by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        scrollOffset += delta
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        allCards.forEachIndexed { index, item ->
            val centerIndex = (cardCount - 1) / 2f
            val relativeIndex = index - centerIndex + (scrollOffset / 150f)
            val rotation = relativeIndex * 12f
            val ty = (relativeIndex.pow(2) * 10f)
            val tx = relativeIndex * 85f

            when (item) {
                is Skill -> {
                    val isUlt = item.ultimateGain == 0
                    val ultReady = currentHero.ultimateGauge >= 100
                    val cooldown = skillCooldowns[item.id] ?: 0
                    val isSelected = selectedCardId == item.id

                    SkillCard(
                        skill = item,
                        isUltimate = isUlt,
                        ultReady = ultReady,
                        baseCooldown = item.cooldown,
                        cooldownRemaining = cooldown,
                        isSelected = isSelected,
                        onClick = { onCardSelect(item.id) },
                        onUse = { onUse(item) },
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = tx.dp.toPx()
                                translationY = ty.dp.toPx()
                                rotationZ = rotation
                                alpha = if (selectedCardId != null && !isSelected) 0.3f else 1f
                                scaleX = if (selectedCardId != null && !isSelected) 0.8f else 1f
                                scaleY = if (selectedCardId != null && !isSelected) 0.8f else 1f
                            }
                            .zIndex(index.toFloat())
                    )
                }
                is ComboSkill -> {
                    val isSelected = selectedCardId == item.id
                    ComboCard(
                        combo = item,
                        isSelected = isSelected,
                        onClick = {
                            selectedCardId?.let { onCardSelect(it) }
                            onComboSelect(item.id)
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = tx.dp.toPx()
                                translationY = ty.dp.toPx() - 20f // slightly higher
                                rotationZ = rotation
                                alpha = if (selectedCardId != null && !isSelected) 0.3f else 1f
                                scaleX = if (selectedCardId != null && !isSelected) 0.8f else 1f
                                scaleY = if (selectedCardId != null && !isSelected) 0.8f else 1f
                            }
                            .zIndex(index.toFloat())
                    )
                }
            }
        }
    }
}
```

#### Step 4: Create ComboCard composable

Add a new composable in BattleActions.kt for combo cards:

```kotlin
@Composable
private fun ComboCard(
    combo: ComboSkill,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse)
    )

    Card(
        modifier = modifier
            .width(150.dp)
            .height(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4A148C).copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 16.dp else 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(
                    width = 3.dp,
                    color = Color(0xFF9C27B0).copy(alpha = if (isSelected) 1f else glowAlpha),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Combo icon
                Text("\uD83D\uDCA5", fontSize = 28.sp) // explosion emoji

                Spacer(Modifier.height(4.dp))

                Text(
                    text = combo.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFCE93D8), // light purple
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = combo.description,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color(0xFFE1BEE7).copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    maxLines = 3
                )

                Spacer(Modifier.weight(1f))

                // Required heroes
                Text(
                    text = combo.requiredHeroes.joinToString(" + "),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = Color(0xFFCE93D8).copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Purple glow pulse
            if (isSelected) {
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                        .border(4.dp, Color(0xFF9C27B0).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                )
            }
        }
    }
}
```

#### Step 5: Remove old FAB and ComboSelector

From `ActionPanel` (lines 225-263), delete:

1. The `showComboSelector` state variable (line 57):
   ```
   var showComboSelector by remember { mutableStateOf(false) }
   ```

2. The FAB block (lines 225-242):
   ```kotlin
   // 4. Combo Button
   if (comboAvailable && selectedCardId == null && !isTargeting) {
       Box(
           modifier = Modifier
               .align(Alignment.TopEnd)
               .padding(end = 16.dp, top = 40.dp)
       ) {
           FloatingActionButton(
               onClick = { showComboSelector = !showComboSelector },
               containerColor = Color(0xFF9C27B0),
               contentColor = Color.White,
               shape = CircleShape,
               modifier = Modifier.size(56.dp)
           ) {
               Icon(Icons.Default.AutoAwesome, contentDescription = "Combo", modifier = Modifier.size(28.dp))
           }
       }
   }
   ```

3. The ComboSelector modal (lines 244-263):
   ```kotlin
   // 5. Combo Selector Modal
   if (showComboSelector) {
       Surface(
           modifier = Modifier
               .fillMaxWidth()
               .padding(16.dp)
               .align(Alignment.Center),
           shape = RoundedCornerShape(24.dp),
           tonalElevation = 12.dp
       ) {
           ComboSelector(
               heroes = heroes,
               onSelect = { participants ->
                   onCombo(participants)
                   showComboSelector = false
               },
               onDismiss = { showComboSelector = false }
           )
       }
   }
   ```

4. The entire `ComboSelector` composable function (lines 509-567):
   ```kotlin
   @Composable
   fun ComboSelector(
       heroes: List<HeroInstance>,
       onSelect: (Set<String>) -> Unit,
       onDismiss: () -> Unit
   ) {
       // ... delete entire function body ...
   }
   ```

5. Remove unused imports: `Icons.Default.AutoAwesome` (from line 18's `import androidx.compose.material.icons.filled.*` — since it's a wildcard import, it may still be used elsewhere; keep the import but verify no other `Icons` references were deleted).

#### Step 6: Add `executeComboById` to GameViewModel

**File:** `GameViewModel.kt`

Add a new function alongside the existing `executeCombo`:

```kotlin
fun executeComboById(comboId: String) {
    val combo = ComboSkillDefinitions.getCombo(comboId) ?: return
    // Map requiredHero names to hero IDs (hero.name -> hero.heroId)
    val participantIds = combo.requiredHeroes.mapNotNull { name ->
        _battleState.value?.heroes?.find { it.name == name && !it.isDead }?.heroId
    }.toSet()
    if (participantIds.size != combo.requiredHeroes.size) return
    executeCombo(participantIds)
}
```

**Update `ActionPanel` call in `BattleScreen.kt` (lines 401-426) to pass `onComboById`:**

Find the `ActionPanel` call in BattleScreen.kt and change the `onCombo` parameter:

**Before (line 416):**
```kotlin
onCombo = { viewModel.executeCombo(it) },
```

**After:**
```kotlin
onComboById = { comboId -> viewModel.executeComboById(comboId) },
```

**Also remove the `comboAvailable` parameter:**
**Before:**
```kotlin
ActionPanel(
    currentHero = currentHero,
    heroes = state.heroes,
    monsters = state.monsters,
    skillCooldowns = state.skillCooldowns[currentHero.heroId] ?: emptyMap(),
    comboAvailable = state.isComboAvailable,
    onSkill = { skill, targets -> ...
    onUltimate = { viewModel.executeUltimate(currentHero.heroId) },
    onCombo = { viewModel.executeCombo(it) },
    ...
)
```

**After:**
```kotlin
ActionPanel(
    currentHero = currentHero,
    heroes = state.heroes,
    monsters = state.monsters,
    skillCooldowns = state.skillCooldowns[currentHero.heroId] ?: emptyMap(),
    onSkill = { skill, targets -> ...
    onUltimate = { viewModel.executeUltimate(currentHero.heroId) },
    onComboById = { comboId -> viewModel.executeComboById(comboId) },
    ...
)
```

---

## 1.6 Show base cooldown on skill cards

### Current behavior (SkillCard, lines 331-491)

- Lines 342-343: `isOnCooldown = cooldown > 0`, `canUse = if (isUltimate) ultReady else !isOnCooldown`
- Lines 413-424: Only shows cooldown number when `isOnCooldown` is true via centered overlay

### Changes

#### Step 1: Add `baseCooldown` and `cooldownRemaining` parameters to SkillCard

**Before (line 332-341):**
```kotlin
@Composable
fun SkillCard(
    skill: Skill,
    isUltimate: Boolean,
    ultReady: Boolean,
    cooldown: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnCooldown = cooldown > 0
    val canUse = if (isUltimate) ultReady else !isOnCooldown
```

**After:**
```kotlin
@Composable
fun SkillCard(
    skill: Skill,
    isUltimate: Boolean,
    ultReady: Boolean,
    baseCooldown: Int = 0,          // NEW: skill.cooldown — always shown
    cooldownRemaining: Int = 0,     // NEW: remaining turns from state
    isSelected: Boolean,
    onClick: () -> Unit,
    onUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnCooldown = cooldownRemaining > 0
    val canUse = if (isUltimate) ultReady else !isOnCooldown
```

#### Step 2: Replace cooldown overlay with badge

**Before (lines 413-423):**
```kotlin
Box(contentAlignment = Alignment.Center) {
    Text(getSkillIcon(skill), fontSize = 32.sp, modifier = Modifier.alpha(if (isOnCooldown) 0.5f else 1f))
    if (isOnCooldown) {
        Text(
            text = cooldown.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).padding(horizontal = 8.dp)
        )
    }
}
```

**After:**
```kotlin
Box(contentAlignment = Alignment.Center) {
    Text(getSkillIcon(skill), fontSize = 32.sp, modifier = Modifier.alpha(if (isOnCooldown) 0.5f else 1f))

    // Cooldown badge — top-right corner
    if (baseCooldown > 0 || isOnCooldown) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    if (isOnCooldown) Color.Red.copy(alpha = 0.8f)
                    else Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isOnCooldown) "$cooldownRemaining" else "CD:$baseCooldown",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
```

#### Step 3: Update HandOfCards to pass new parameters

In HandOfCards (the `SkillCard` call within the `item is Skill` branch):

**Before (in the Skill branch of HandOfCards):**
```kotlin
SkillCard(
    skill = item,
    isUltimate = isUlt,
    ultReady = ultReady,
    cooldown = cooldown,
    isSelected = isSelected,
    onClick = { onCardSelect(item.id) },
    onUse = { onUse(item) },
    ...
)
```

**After:**
```kotlin
SkillCard(
    skill = item,
    isUltimate = isUlt,
    ultReady = ultReady,
    baseCooldown = item.cooldown,           // NEW
    cooldownRemaining = cooldown,           // NEW (previously just "cooldown")
    isSelected = isSelected,
    onClick = { onCardSelect(item.id) },
    onUse = { onUse(item) },
    ...
)
```

Also update the standalone SkillCard usage in ActionPanel (lines 166-198) where a single card is shown enlarged after selection:

**Before (line 170):**
```kotlin
SkillCard(
    skill = skill,
    isUltimate = skill.ultimateGain == 0,
    ultReady = currentHero.ultimateGauge >= 100,
    cooldown = cooldown,
    isSelected = true,
    ...
)
```

**After:**
```kotlin
SkillCard(
    skill = skill,
    isUltimate = skill.ultimateGain == 0,
    ultReady = currentHero.ultimateGauge >= 100,
    baseCooldown = skill.cooldown,
    cooldownRemaining = cooldown,
    isSelected = true,
    ...
)
```

---

## Verification

1. Available combos appear as purple-bordered cards in HandOfCards
2. Combo cards show combo name, description, required heroes, and purple glow
3. Tapping a combo card executes it immediately (auto-resolves partners)
4. Old FAB button and ComboSelector dialog no longer present in UI
5. `ComboSelector` function no longer exists in BattleActions.kt
6. Every skill card shows its base cooldown (e.g., "CD:2") when not on cooldown
7. Skills on cooldown show remaining turns in a red badge instead
8. No crash when using combos — `executeComboById` correctly maps names to IDs
