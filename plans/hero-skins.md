# Hero Skins Implementation Plan

## Summary

Build a hero skins system from scratch. Heroes currently have no skins. The system needs:
1. A `HeroSkin` data model and skin definitions for all 5 heroes
2. Persistence (equipped skin per hero + unlocked skin tracking)
3. Color override plumbing through the drawing pipeline (`drawSilhouette` → `CombatantSprite` → `HeroPortrait`)
4. UI in the `HeroDetailsDialog` to view, select, and unlock skins

**Simplest viable approach:** Alternate skins are different color palettes applied to the same silhouette. The `drawSilhouette()` function already accepts a single `tint` color. We extend it to accept optional `primaryColor` and `secondaryColor` overrides, then apply those colors to different portions of each hero's silhouette. No new bitmaps or artwork needed.

---

## Implementation Order & Dependencies

The work must proceed in this order because later steps depend on earlier ones:

```
Step 1: Data model          (Hero.kt)           ──┐
Step 2: Skin definitions    (skins.json)        ──┤ foundation
Step 3: DataLoader          (DataLoader.kt)     ──┘
Step 4: Persistence model   (GameProgress.kt)   ──┐
Step 5: Save/load           (GameSaveManager.kt)──┘ storage
Step 6: Drawing updates     (BattleCanvas.kt, CombatantSprite.kt, HeroPortrait.kt) ── visual
Step 7: BattleState passthrough (Hero.kt, BattleState.kt) ── plumbing
Step 8: Manager logic       (PartyManager.kt) ── business logic
Step 9: ViewModel exposure  (GameViewModel.kt) ── wiring
Step 10: UI                 (PartyScreen.kt)   ── user-facing
Step 11: Tests              (GameProgressSerializationTest.kt) ── verify
```

---

## Step 1: Define `HeroSkin` data model

**File:** `app/src/main/java/com/example/game/model/Hero.kt`

**Add after line 19** (after the `Hero` data class closing `)` and before `HeroColorTheme`):

```kotlin
enum class SkinUnlockMethod {
    DEFAULT,        // unlocked automatically (each hero's default skin)
    PURCHASE,       // buy with gold
    ACHIEVEMENT,    // tied to trophy/badge unlock
    YOGA_LEVEL      // unlocked when yoga level reaches threshold
}

data class HeroSkin(
    val skinId: String,           // e.g. "virya_inferno"
    val heroId: Int,              // which hero (1-5)
    val name: String,             // display name
    val description: String,
    val primaryColor: String,     // hex color, e.g. "#FF4500"
    val secondaryColor: String,   // hex color, e.g. "#FFD700"
    val unlockMethod: SkinUnlockMethod,
    val unlockCost: Int = 0,      // gold cost (for PURCHASE)
    val unlockYogaLevel: Int = 0, // yoga level required (for YOGA_LEVEL)
    val unlockAchievementId: String? = null  // trophy ID (for ACHIEVEMENT)
)
```

**Update `toCombatantState()` (lines 30-63 of Hero.kt):**

The function signature at line 31 currently takes `(partyMember: PartyMemberData, equippedEquipment: List<Equipment> = emptyList())`. Add a third parameter `heroSkin: HeroSkin? = null`.

At the `CombatantState(...)` construction (lines 49-62), add two new fields (which will be added to `CombatantState` in Step 7):

```kotlin
skinPrimaryColor = heroSkin?.primaryColor,
skinSecondaryColor = heroSkin?.secondaryColor
```

---

## Step 2: Create skin definitions JSON

**File:** `app/src/main/assets/game/skins.json` (new file)

Create a JSON array of `HeroSkin` objects. Each hero gets 1 default skin + 1-2 alternate skins.

Default skins use the hero's existing element colors. Alternate skins use different color combinations applied to the same silhouette.

**Skin definitions:**

| skinId | heroId | name | primaryColor | secondaryColor | unlockMethod | cost |
|---|---|---|---|---|---|---|
| shanti_default | 1 | Serene Tide | #1E88E5 | #B3E5FC | DEFAULT | 0 |
| shanti_night | 1 | Moonlit Calm | #311B92 | #CE93D8 | PURCHASE | 200 |
| shanti_radiant | 1 | Radiant Grace | #FFF176 | #FFAB91 | YOGA_LEVEL | lv 10 |
| santosha_default | 2 | Earth's Embrace | #795548 | #A1887F | DEFAULT | 0 |
| santosha_iron | 2 | Iron Resolve | #607D8B | #CFD8DC | PURCHASE | 200 |
| santosha_wild | 2 | Wild Root | #2E7D32 | #A5D6A7 | YOGA_LEVEL | lv 12 |
| virya_default | 3 | Blazing Core | #E53935 | #FF9800 | DEFAULT | 0 |
| virya_inferno | 3 | Inferno Soul | #B71C1C | #FFEB3B | PURCHASE | 250 |
| virya_azure | 3 | Azure Flame | #01579B | #40C4FF | PURCHASE | 300 |
| dhairya_default | 4 | Golden Courage | #FFD54F | #FFA000 | DEFAULT | 0 |
| dhairya_onyx | 4 | Onyx Knight | #424242 | #BDBDBD | PURCHASE | 200 |
| dhairya_royal | 4 | Royal Standard | #4A148C | #FFD700 | YOGA_LEVEL | lv 15 |
| maitri_default | 5 | Whispering Wind | #B0BEC5 | #E1F5FE | DEFAULT | 0 |
| maitri_spring | 5 | Spring Bloom | #4CAF50 | #C8E6C9 | PURCHASE | 200 |
| maitri_celestial | 5 | Celestial Light | #7C4DFF | #B388FF | PURCHASE | 300 |

Each JSON object has all fields from the `HeroSkin` data class. Example for one:

```json
{
  "skinId": "shanti_default",
  "heroId": 1,
  "name": "Serene Tide",
  "description": "Shanti's classic serene form, flowing with water energy.",
  "primaryColor": "#1E88E5",
  "secondaryColor": "#B3E5FC",
  "unlockMethod": "DEFAULT",
  "unlockCost": 0,
  "unlockYogaLevel": 0
}
```

---

## Step 3: Add skin loading to DataLoader

**File:** `app/src/main/java/com/example/game/persistence/DataLoader.kt`

**Add `skins` lazy property after line 14** (after the `heroes` lazy initialization):

```kotlin
val skins: List<HeroSkin> by lazy { loadList("skins.json") }
```

**Add `getSkin(skinId: String)` lookup method after existing lookup methods (around line 51):**

```kotlin
fun getSkin(skinId: String): HeroSkin? = skins.firstOrNull { it.skinId == skinId }

fun getSkinsForHero(heroId: Int): List<HeroSkin> = skins.filter { it.heroId == heroId }

fun getDefaultSkin(heroId: Int): HeroSkin? = skins.firstOrNull { it.heroId == heroId && it.unlockMethod == SkinUnlockMethod.DEFAULT }
```

---

## Step 4: Update persistence model (GameProgress)

**File:** `app/src/main/java/com/example/game/model/GameProgress.kt`

### 4a: Add fields to `GameProgress`

Add two new fields to the `GameProgress` data class (after `totalYogaXp` line 11, before `gold` line 12):

```kotlin
val heroSkins: Map<Int, String> = emptyMap(),        // heroId -> equipped skinId
val unlockedSkinIds: Set<String> = emptySet()        // all unlocked skin IDs
```

Bump `version` from 3 to 4:

```kotlin
val version: Int = 4,
```

### 4b: Add field to `PartyMemberData`

**File:** `app/src/main/java/com/example/game/model/GameProgress.kt`, lines 19-23

Add `skinId: String? = null` to `PartyMemberData`:

```kotlin
data class PartyMemberData(
    val heroId: Int,
    val level: Int = 1,
    val equippedItemIds: List<String> = emptyList(),
    val skinId: String? = null
)
```

---

## Step 5: Update save/load persistence

**File:** `app/src/main/java/com/example/game/persistence/GameSaveManager.kt`

### 5a: Update version constant (line 16)

Change:
```kotlin
const val KEY_PROGRESS_BLOB = "progress_blob_v3"
```
to:
```kotlin
const val KEY_PROGRESS_BLOB = "progress_blob_v4"
```

### 5b: Update `saveGame()` (line 52-58)

Change `version = 3` to `version = 4`:
```kotlin
putString(KEY_PROGRESS_BLOB, gson.toJson(normalized.copy(version = 4)))
```

### 5c: Update `migrateBlobIfNeeded()` (line 95-138)

After the existing v3 migration logic (around line 133, before `root.addProperty("version", 3)`), add v4 migration for skin fields:

In the migration function, change the version check at line 99:
```kotlin
if (version >= 4) return blob
```

Then add the v4 migration block. After the `unlockedHeroIds` migration (around line 131 `root.add("unlockedHeroIds", newArray)`), before setting `version = 3`:
- Add empty `heroSkins: {}` (empty JSON object)
- Add empty `unlockedSkinIds: []`
- For each hero in the party, check if it has `skinId`; if not, look up the default skin ID for that hero and set `skinId` on the party member

Then change the version stamp to `4`:
```kotlin
root.addProperty("version", 4)
```

### 5d: Update `normalized()` (line 152-171)

In the `normalized()` function, add defaults for the new fields:

After line 163 (`defeatedMonsterIds = ...`), add:
```kotlin
heroSkins = heroSkins ?: emptyMap(),
unlockedSkinIds = unlockedSkinIds ?: emptySet()
```

Also check: if any hero in party doesn't have a skinId in heroSkins, auto-equip the default skin. And automatically unlock all DEFAULT skins:

```kotlin
// Auto-unlock all DEFAULT skins
val defaultSkinIds = skins.filter { it.unlockMethod == SkinUnlockMethod.DEFAULT }.map { it.skinId }.toSet()
if (defaultSkinIds.any { it !in unlockedSkinIds }) {
    result = result.copy(unlockedSkinIds = unlockedSkinIds + defaultSkinIds)
}
// Auto-equip default skin for any hero missing one
result = result.copy(party = result.party.map { pm ->
    if (pm.skinId == null) {
        val defaultSkin = getDefaultSkin(pm.heroId)
        pm.copy(skinId = defaultSkin?.skinId)
    } else pm
})
```

This function now needs access to skin data. Import `DataLoader`'s skin methods or pass them as parameters. The cleanest approach: add a private helper or use `DataLoader.skins` / `DataLoader.getDefaultSkin()` directly (DataLoader is an `object`, accessible anywhere after init).

**Important:** `normalized()` is called during `loadGame()` which runs before `DataLoader` has skins loaded (DataLoader.init runs in GameViewModel init). To avoid this ordering issue, do the skin normalization inside `GameViewModel.loadGame()` after DataLoader is initialized, OR use `runCatching` to handle the case where DataLoader isn't initialized yet.

**Simplest fix:** Inside `normalized()`, wrap the skin-related logic in a try/catch that silently skips if `DataLoader::skins.isInitialized` is false. Or move skin normalization to `PartyManager` (see Step 8).

### 5e: Add import

Add import at top of `GameSaveManager.kt`:
```kotlin
import com.example.game.model.HeroSkin
import com.example.game.model.SkinUnlockMethod
```

---

## Step 6: Update drawing to support skin colors

### 6a: Modify `drawSilhouette()` signature

**File:** `app/src/main/java/com/example/game/ui/components/BattleCanvas.kt`, line 375

Currently:
```kotlin
fun DrawScope.drawSilhouette(cx: Float, cy: Float, s: Float, heroId: Int, tint: Color)
```

Add `primaryColor` and `secondaryColor` optional overrides:
```kotlin
fun DrawScope.drawSilhouette(
    cx: Float, cy: Float, s: Float,
    heroId: Int,
    tint: Color,
    primaryColor: Color? = null,
    secondaryColor: Color? = null
)
```

The new colors are nullable; when `null`, fall back to existing behavior using `tint`. When provided, use `primaryColor` for the main body fill and `secondaryColor` for accent details.

**Implementation note:** Color conversion from the hex strings stored in `HeroSkin` is needed. Add a utility function:

```kotlin
fun String.toComposeColor(): Color = Color(android.graphics.Color.parseColor(this))
```

### 6b: Apply skin colors inside `drawSilhouette()`

For each hero's silhouette drawing (lines 379-554), replace uses of `tint` with the appropriate skin color where applicable:
- Main body/fill paths use `primaryColor ?: tint`
- Details (halos, auras, lines, strokes) use `secondaryColor ?: tint`
- White/neutral elements (prayer beads, heart emblem, glowing core) keep their original colors unchanged

This is a large change since each hero branch (hero 1-5) needs to be updated individually. Example for Hero 1 (Shanti, line 379-418):
- Line 381 (halo): `primaryColor?.copy(alpha = 0.15f) ?: tint.copy(alpha = 0.15f)`
- Line 391 (gown body): `primaryColor?.copy(alpha = 0.8f) ?: tint.copy(alpha = 0.8f)`
- Line 399 (hair stroke): `secondaryColor ?: tint`
- Line 402 (head): `primaryColor?.copy(alpha = 0.9f) ?: tint.copy(alpha = 0.9f)`
- Line 411 (hands): `secondaryColor ?: tint`
- Line 417 (prayer beads): keep as `Color.White.copy(alpha = 0.6f)` (unchanged)

Apply similar logic to heroes 2-5.

**Add import** at top of `BattleCanvas.kt`:
```kotlin
import kotlin.math.*
```

### 6c: Update `CombatantSprite.kt` to pass skin colors

**File:** `app/src/main/java/com/example/game/ui/components/CombatantSprite.kt`

Add parameters to `CombatantSprite` composable signature (lines 18-35):

```kotlin
@Composable
fun CombatantSprite(
    ...
    primaryColor: String? = null,
    secondaryColor: String? = null,
    ...
)
```

Convert the hex strings to `Color` inside the composable:
```kotlin
val skinPrimary = remember(primaryColor) { primaryColor?.toComposeColor() }
val skinSecondary = remember(secondaryColor) { secondaryColor?.toComposeColor() }
```

Then update line 119 where `drawSilhouette` is called:
```kotlin
drawSilhouette(drawCx, drawCy, s, heroId, tint.copy(alpha = smoothAlpha), skinPrimary, skinSecondary)
```

Add import:
```kotlin
import androidx.compose.runtime.remember
```

### 6d: Update `HeroPortrait.kt` to pass skin colors

**File:** `app/src/main/java/com/example/game/ui/components/HeroPortrait.kt`

Add `primaryColor` and `secondaryColor` parameters:

```kotlin
@Composable
fun HeroPortrait(
    heroId: Int,
    elementColor: Color,
    modifier: Modifier = Modifier,
    primaryColor: Color? = null,
    secondaryColor: Color? = null
)
```

Update the `drawSilhouette` call on line 17:
```kotlin
drawSilhouette(cx, cy, s, heroId, elementColor, primaryColor, secondaryColor)
```

---

## Step 7: Pass skin colors through BattleState pipeline

### 7a: Add skin fields to `CombatantState`

**File:** `app/src/main/java/com/example/game/model/BattleState.kt`, lines 23-50

Add two fields to `CombatantState` (after `level` line 35, before `skills`):
```kotlin
val skinPrimaryColor: String? = null,
val skinSecondaryColor: String? = null,
```

### 7b: Update `Hero.toCombatantState()`

**File:** `app/src/main/java/com/example/game/model/Hero.kt`, lines 30-63

Already described in Step 1. The `toCombatantState()` function now:
1. Accepts optional `heroSkin: HeroSkin?` parameter
2. Passes `heroSkin?.primaryColor` and `heroSkin?.secondaryColor` to `CombatantState()`

### 7c: Update `BattleOrchestrator.startBattle()` to pass skin data

**File:** `app/src/main/java/com/example/game/viewmodel/BattleOrchestrator.kt`, lines 28-55

In `startBattle()`, where `battleHeroes` is constructed (lines 37-41), look up each hero's equipped skin and pass it to `toCombatantState()`:

Replace:
```kotlin
val battleHeroes = partyMembers.mapNotNull { pm ->
    val heroDef = DataLoader.heroes.find { it.id == pm.heroId } ?: return@mapNotNull null
    val equipped = pm.equippedItemIds.mapNotNull { DataLoader.getEquipment(it) }
    heroDef.toCombatantState(pm, equipped)
}
```

With:
```kotlin
val battleHeroes = partyMembers.mapNotNull { pm ->
    val heroDef = DataLoader.heroes.find { it.id == pm.heroId } ?: return@mapNotNull null
    val equipped = pm.equippedItemIds.mapNotNull { DataLoader.getEquipment(it) }
    val skin = pm.skinId?.let { DataLoader.getSkin(it) }
    heroDef.toCombatantState(pm, equipped, skin)
}
```

### 7d: Update `BattleScreen.kt` (if applicable) to read skin colors from CombatantState

Where `CombatantSprite` is called for heroes, read `skinPrimaryColor` and `skinSecondaryColor` from the `CombatantState` and pass them to the composable. This is likely in `BattleScreen.kt` or a similar battle rendering composable. Search for `CombatantSprite(` calls for heroes and add the two skin color parameters from the hero's `CombatantState`.

---

## Step 8: Add skin business logic to PartyManager

**File:** `app/src/main/java/com/example/game/viewmodel/PartyManager.kt`

### 8a: Add skin initialization to PartyManager

Add method to ensure all heroes have default skins unlocked and equipped after game load. This is called once from `GameViewModel.loadGame()`:

```kotlin
fun initializeDefaultSkins() {
    val data = _saveData.value
    var updated = data
    var changed = false

    // Unlock all DEFAULT skins
    val defaultSkins = DataLoader.skins.filter { it.unlockMethod == SkinUnlockMethod.DEFAULT }
    for (skin in defaultSkins) {
        if (skin.skinId !in updated.unlockedSkinIds) {
            updated = updated.copy(unlockedSkinIds = updated.unlockedSkinIds + skin.skinId)
            changed = true
        }
    }

    // Equip default skin for any hero missing one
    val newParty = updated.party.map { pm ->
        if (pm.skinId == null || DataLoader.getSkin(pm.skinId) == null) {
            val defaultSkin = DataLoader.getDefaultSkin(pm.heroId)
            pm.copy(skinId = defaultSkin?.skinId)
        } else pm
    }

    // Update heroSkins map
    val newHeroSkins = updated.heroSkins.toMutableMap()
    for (pm in newParty) {
        val skinId = pm.skinId
        if (skinId != null && updated.heroSkins[pm.heroId] != skinId) {
            newHeroSkins[pm.heroId] = skinId
            changed = true
        }
    }

    if (newParty != updated.party) changed = true

    if (changed) {
        updated = updated.copy(party = newParty, heroSkins = newHeroSkins)
        _saveData.value = updated
        _party.value = newParty
        saveManager.saveGame(updated)
    }
}
```

Add imports at top:
```kotlin
import com.example.game.model.SkinUnlockMethod
```

### 8b: Add `equipSkin()` method

Add after `getEquippedItems()` (around line 100):

```kotlin
fun equipSkin(heroId: Int, skinId: String): Boolean {
    val skin = DataLoader.getSkin(skinId) ?: return false
    if (skin.heroId != heroId) return false
    val data = _saveData.value
    if (skinId !in data.unlockedSkinIds) return false

    val newParty = _party.value.map {
        if (it.heroId == heroId) it.copy(skinId = skinId) else it
    }
    val newHeroSkins = data.heroSkins + (heroId to skinId)

    _party.value = newParty
    _saveData.value = data.copy(party = newParty, heroSkins = newHeroSkins)
    saveManager.saveGame(_saveData.value)
    return true
}
```

### 8c: Add `unlockSkin()` method

```kotlin
fun unlockSkin(skinId: String): Boolean {
    val skin = DataLoader.getSkin(skinId) ?: return false
    val data = _saveData.value
    if (skinId in data.unlockedSkinIds) return false

    val canUnlock = when (skin.unlockMethod) {
        SkinUnlockMethod.DEFAULT -> true
        SkinUnlockMethod.PURCHASE -> data.gold >= skin.unlockCost
        SkinUnlockMethod.YOGA_LEVEL -> data.yogaLevel >= skin.unlockYogaLevel
        SkinUnlockMethod.ACHIEVEMENT -> skin.unlockAchievementId?.let { it in data.earnedTrophyIds } ?: false
    }
    if (!canUnlock) return false

    val cost = if (skin.unlockMethod == SkinUnlockMethod.PURCHASE) skin.unlockCost else 0
    val newData = data.copy(
        gold = data.gold - cost,
        unlockedSkinIds = data.unlockedSkinIds + skinId
    )
    _saveData.value = newData
    saveManager.saveGame(newData)
    return true
}
```

### 8d: Add `getEquippedSkin()` helper

```kotlin
fun getEquippedSkin(heroId: Int): HeroSkin? {
    val pm = _party.value.find { it.heroId == heroId }
    val skinId = pm?.skinId ?: _saveData.value.heroSkins[heroId]
    return skinId?.let { DataLoader.getSkin(it) }
}
```

### 8e: Add `getUnlockedSkinsForHero()` helper

```kotlin
fun getUnlockedSkinsForHero(heroId: Int): List<HeroSkin> {
    val unlocked = _saveData.value.unlockedSkinIds
    return DataLoader.getSkinsForHero(heroId).filter { it.skinId in unlocked }
}
```

---

## Step 9: Expose skin methods in GameViewModel

**File:** `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt`

### 9a: Call skin initialization in `loadGame()` (line 76-80)

After `_party.value = data.party` (line 79), add:
```kotlin
partyManager.initializeDefaultSkins()
```

### 9b: Add public delegate methods (after line 101)

```kotlin
fun equipSkin(heroId: Int, skinId: String): Boolean = partyManager.equipSkin(heroId, skinId)
fun unlockSkin(skinId: String): Boolean = partyManager.unlockSkin(skinId)
fun getEquippedSkin(heroId: Int): HeroSkin? = partyManager.getEquippedSkin(heroId)
fun getUnlockedSkinsForHero(heroId: Int): List<HeroSkin> = partyManager.getUnlockedSkinsForHero(heroId)
```

Add import at top:
```kotlin
import com.example.game.model.HeroSkin
```

---

## Step 10: Add skin UI to HeroDetailsDialog

**File:** `app/src/main/java/com/example/game/ui/components/PartyScreen.kt`

### 10a: Update `PartyScreen` composable (lines 109-119)

In the `detailHeroId?.let { id ->` block, add a `selectedSkinDialog` flag and pass it plus skin data to `HeroDetailsDialog`:

Add a state variable at the top of `PartyScreen` (after `detailHeroId`):
```kotlin
var showSkinSelect by remember { mutableStateOf(false) }
```

When calling `HeroDetailsDialog`, pass `showSkinSelect` and `onSkinSelectClick`:
```kotlin
HeroDetailsDialog(
    hero = heroDef,
    partyMember = partyMember,
    saveData = saveData,
    viewModel = viewModel,
    onDismiss = { detailHeroId = null },
    onOpenSkinSelect = { showSkinSelect = true }
)
```

After the `HeroDetailsDialog` call block, add the skin select dialog:
```kotlin
if (showSkinSelect && detailHeroId != null && partyMember != null) {
    SkinSelectDialog(
        heroId = detailHeroId!!,
        currentSkinId = partyMember!!.skinId,
        viewModel = viewModel,
        onDismiss = { showSkinSelect = false }
    )
}
```

### 10b: Update `HeroDetailsDialog` signature (line 249)

Add parameters:
```kotlin
@Composable
fun HeroDetailsDialog(
    hero: Hero,
    partyMember: PartyMemberData,
    saveData: GameProgress,
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onOpenSkinSelect: () -> Unit = {}
)
```

### 10c: Add skin section to `HeroDetailsDialog` body

Insert a new section between the stats bar (after line 299, before the HorizontalDivider on line 301) and the level-up button. This shows the current skin with a click-to-change action:

```kotlin
// Skin section
val equippedSkin = viewModel.getEquippedSkin(hero.id)
if (equippedSkin != null) {
    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Skin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().clickable { onOpenSkinSelect() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small colored preview
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(equippedSkin.primaryColor))
                        } catch (_: Exception) { heroColor }
                    ),
                contentAlignment = Alignment.Center
            ) {
                HeroPortrait(
                    hero.id,
                    heroColor,
                    Modifier.size(36.dp),
                    primaryColor = try { Color(android.graphics.Color.parseColor(equippedSkin.primaryColor)) } catch (_: Exception) { null },
                    secondaryColor = try { Color(android.graphics.Color.parseColor(equippedSkin.secondaryColor)) } catch (_: Exception) { null }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(equippedSkin.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(equippedSkin.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Change", tint = Color.Gray)
        }
    }
}
```

Add imports needed at the top of the file:
```kotlin
import com.example.game.model.HeroSkin
import com.example.game.model.SkinUnlockMethod
```

### 10d: Create `SkinSelectDialog` composable

Add a new composable at the end of `PartyScreen.kt` (after line 873):

```kotlin
@Composable
fun SkinSelectDialog(
    heroId: Int,
    currentSkinId: String?,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val allSkinsForHero = remember { DataLoader.getSkinsForHero(heroId) }
    val unlockedSkinIds = viewModel.saveData.collectAsState().value.unlockedSkinIds
    val saveData = viewModel.saveData.collectAsState().value

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Select Skin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                allSkinsForHero.forEach { skin ->
                    val isUnlocked = skin.skinId in unlockedSkinIds
                    val isEquipped = skin.skinId == currentSkinId
                    val heroColor = try {
                        Color(android.graphics.Color.parseColor(skin.primaryColor))
                    } catch (_: Exception) { Color.Gray }
                    val secondaryColor = try {
                        Color(android.graphics.Color.parseColor(skin.secondaryColor))
                    } catch (_: Exception) { Color.Gray }

                    SkinCard(
                        skin = skin,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        heroColor = heroColor,
                        secondaryColor = secondaryColor,
                        saveData = saveData,
                        onSelect = {
                            if (isUnlocked) {
                                viewModel.equipSkin(heroId, skin.skinId)
                                onDismiss()
                            }
                        },
                        onUnlock = {
                            if (!isUnlocked) {
                                viewModel.unlockSkin(skin.skinId)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun SkinCard(
    skin: HeroSkin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    heroColor: Color,
    secondaryColor: Color,
    saveData: GameProgress,
    onSelect: () -> Unit,
    onUnlock: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isEquipped)
            heroColor.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isEquipped) BorderStroke(2.dp, heroColor) else null,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .then(if (isUnlocked) Modifier.clickable { onSelect() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color preview circle
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(heroColor)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(secondaryColor.copy(alpha = 0.5f))
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        skin.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isEquipped) FontWeight.Bold else FontWeight.Medium,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                    if (isEquipped) {
                        Spacer(Modifier.width(8.dp))
                        Text("(Equipped)", style = MaterialTheme.typography.labelSmall, color = heroColor, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    skin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isUnlocked) {
                    Spacer(Modifier.height(4.dp))
                    UnlockRequirementLabel(skin, saveData)
                }
            }

            if (!isUnlocked) {
                val canAfford = when (skin.unlockMethod) {
                    SkinUnlockMethod.PURCHASE -> saveData.gold >= skin.unlockCost
                    SkinUnlockMethod.YOGA_LEVEL -> saveData.yogaLevel >= skin.unlockYogaLevel
                    SkinUnlockMethod.ACHIEVEMENT -> skin.unlockAchievementId?.let { it in saveData.earnedTrophyIds } ?: false
                    else -> false
                }
                FilledTonalButton(
                    onClick = onUnlock,
                    enabled = canAfford,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = heroColor.copy(alpha = 0.3f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        if (canAfford) "Unlock" else "Locked",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockRequirementLabel(skin: HeroSkin, saveData: GameProgress) {
    val label = when (skin.unlockMethod) {
        SkinUnlockMethod.PURCHASE -> "\uD83E\uDE99 ${skin.unlockCost} Gold"
        SkinUnlockMethod.YOGA_LEVEL -> "Yoga Lv.${skin.unlockYogaLevel} required"
        SkinUnlockMethod.ACHIEVEMENT -> "Achievement required"
        SkinUnlockMethod.DEFAULT -> "Default"
    }
    val met = when (skin.unlockMethod) {
        SkinUnlockMethod.PURCHASE -> saveData.gold >= skin.unlockCost
        SkinUnlockMethod.YOGA_LEVEL -> saveData.yogaLevel >= skin.unlockYogaLevel
        SkinUnlockMethod.ACHIEVEMENT -> skin.unlockAchievementId?.let { it in saveData.earnedTrophyIds } ?: false
        SkinUnlockMethod.DEFAULT -> true
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = if (met) MaterialTheme.colorScheme.primary else Color.Gray
    )
}
```

---

## Step 11: Update tests

**File:** `app/src/test/java/com/example/game/model/GameProgressSerializationTest.kt`

### 11a: Update `version` assertions

In `serialize and deserialize GameProgress` test (lines 12-36), update:
- `version = 3` → `version = 4` (line 14)
- `assertEquals(3, restored.version)` → `assertEquals(4, restored.version)` (line 27)

### 11b: Update `version field is respected` test (lines 38-43)

```kotlin
val json = """{"version":4,"sparks":100}"""
// ...
assertEquals(4, restored.version)
```

### 11c: Update `default values on missing fields` test (lines 46-58)

`assertEquals(3, restored.version)` → `assertEquals(4, restored.version)` (line 49)

Add assertions for new fields:
```kotlin
assertTrue(restored.heroSkins.isEmpty())
assertTrue(restored.unlockedSkinIds.isEmpty())
```

### 11d: Add skin serialization test

```kotlin
@Test
fun `skin fields serialize and deserialize`() {
    val original = GameProgress(
        version = 4,
        heroSkins = mapOf(1 to "shanti_default", 3 to "virya_inferno"),
        unlockedSkinIds = setOf("shanti_default", "virya_inferno", "virya_azure"),
        party = listOf(
            PartyMemberData(1, 2, emptyList(), "shanti_default"),
            PartyMemberData(3, 4, emptyList(), "virya_inferno")
        )
    )
    val json = gson.toJson(original)
    val restored = gson.fromJson(json, GameProgress::class.java)
    assertEquals(original.heroSkins, restored.heroSkins)
    assertEquals(original.unlockedSkinIds, restored.unlockedSkinIds)
    assertEquals(original.party[0].skinId, restored.party[0].skinId)
    assertEquals("shanti_default", restored.party[0].skinId)
    assertEquals("virya_inferno", restored.party[1].skinId)
}
```

---

## Step 12: Update `default_save.json` (game asset)

**File:** `app/src/main/assets/game/default_save.json`

Update `version` from 3 to 4. Add empty arrays/objects for the new fields:
```json
{
  "version": 4,
  "party": [...],
  "unlockedHeroIds": [...],
  "heroSkins": {},
  "unlockedSkinIds": []
}
```

The `PartyMemberData` items in the party array should include `"skinId": "shanti_default"` (etc.) matching each hero's default skin.

---

## Step 13: Update `PartyScreen.kt` to pass skin colors to `HeroPortrait` calls

In `PartyScreen.kt`, every call to `HeroPortrait` currently passes only `hero.id` and `heroColor`. Update each call site to also look up the equipped skin and pass colors:

- Line 160: `HeroListItem` — get equipped skin from party member's `skinId` and pass colors
- Line 274: `HeroDetailsDialog` portrait — already partially covered by the skin section addition
- Line 346: Equipment section portrait — pass skin colors

Each call site pattern:
```kotlin
val equippedSkin = viewModel.getEquippedSkin(hero.id)
HeroPortrait(
    hero.id,
    heroColor,
    Modifier.size(48.dp),
    primaryColor = equippedSkin?.primaryColor?.let { Color(android.graphics.Color.parseColor(it)) },
    secondaryColor = equippedSkin?.secondaryColor?.let { Color(android.graphics.Color.parseColor(it)) }
)
```

---

## File Change Summary

| # | File | Action | Lines |
|---|---|---|---|
| 1 | `game/model/Hero.kt` | Add `SkinUnlockMethod` enum, `HeroSkin` data class, update `toCombatantState()` | after 19, update 30-63 |
| 2 | `app/src/main/assets/game/skins.json` | **New file** — 14 skin definitions | — |
| 3 | `game/persistence/DataLoader.kt` | Add `skins` lazy list + `getSkin()`, `getSkinsForHero()`, `getDefaultSkin()` | after 14, after 51 |
| 4 | `game/model/GameProgress.kt` | Bump version to 4, add `heroSkins`, `unlockedSkinIds` to `GameProgress`; add `skinId` to `PartyMemberData` | 3, 11-23 |
| 5 | `game/persistence/GameSaveManager.kt` | Update blob key, save version, migrate v3→v4 (skin defaults), `normalized()` updates | 16, 56, 95-138, 152-171 |
| 6 | `game/ui/components/BattleCanvas.kt` | Update `drawSilhouette()` signature + body to use `primaryColor`/`secondaryColor` overrides | 375 (signature), 379-554 (body) |
| 7 | `game/ui/components/CombatantSprite.kt` | Add `primaryColor`/`secondaryColor` params, pass to `drawSilhouette()` | 18-35 (sig), 61, 119 |
| 8 | `game/ui/components/HeroPortrait.kt` | Add `primaryColor`/`secondaryColor` params, pass to `drawSilhouette()` | 8-18 |
| 9 | `game/model/BattleState.kt` | Add `skinPrimaryColor`/`skinSecondaryColor` to `CombatantState` | 23-50 |
| 10 | `game/viewmodel/BattleOrchestrator.kt` | Look up skin in `startBattle()`, pass to `toCombatantState()` | 37-41 |
| 11 | `game/viewmodel/PartyManager.kt` | Add `initializeDefaultSkins()`, `equipSkin()`, `unlockSkin()`, `getEquippedSkin()`, `getUnlockedSkinsForHero()` | after 100 |
| 12 | `game/viewmodel/GameViewModel.kt` | Call `initializeDefaultSkins()` in `loadGame()`, expose `equipSkin`/`unlockSkin`/`getEquippedSkin`/`getUnlockedSkinsForHero` | 79, after 101 |
| 13 | `game/ui/components/PartyScreen.kt` | Add skin section to `HeroDetailsDialog`, create `SkinSelectDialog`, `SkinCard`, `UnlockRequirementLabel`, update `HeroPortrait` calls | 109-119, 299+, 873+ |
| 14 | `app/src/main/assets/game/default_save.json` | Bump version to 4, add `heroSkins: {}`, `unlockedSkinIds: []`, add `skinId` to party members | — |
| 15 | `test/.../GameProgressSerializationTest.kt` | Update version=4 assertions, add skin fields test | 14, 27, 39, 49, 56-58 |

---

## Edge Cases & Gotchas

1. **DataLoader initialization order:** `DataLoader.init()` is called in `GameViewModel.init {}`. The `PartyManager.initializeDefaultSkins()` is called in `GameViewModel.loadGame()` which runs AFTER `init {}`. This is safe because `DataLoader` is initialized first.

2. **Save migration:** Old saves have no `heroSkins` or `unlockedSkinIds`. The migration in `GameSaveManager.migrateBlobIfNeeded()` adds empty defaults. The `initializeDefaultSkins()` call in `loadGame()` then populates them with default skins.

3. **Color parsing:** The `String.toComposeColor()` utility must handle invalid hex strings gracefully (use `runCatching` or try/catch, falling back to the element color).

4. **Heroes not found:** If a hero definition isn't found, `getDefaultSkin()` returns null. The `norm()` and `equipSkin()` logic should handle null skin gracefully by skipping.

5. **Skin un-equipping:** If a previously equipped skin is NOT in unlocked set (shouldn't happen, but can after migration bugs), auto-fallback to default skin.

6. **Performance:** Skin colors are hex strings in the model to keep JSON serialization simple. Convert to `Color` once per composable call site (not inside draw loops).

7. **Battle screen:** Confirm the exact file/call site where `CombatantSprite` is rendered for heroes in battle (likely `BattleScreen.kt`). If it differs from the file names used here, adjust file paths accordingly.

8. **Kotlin `Color` import:** The `Color` class comes from `androidx.compose.ui.graphics.Color`. The `android.graphics.Color.parseColor()` is needed for hex → Compose Color conversion. Add both imports where needed.
