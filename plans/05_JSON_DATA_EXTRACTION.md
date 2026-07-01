# Plan: Extract Hardcoded Game Data to JSON

**Dependencies:** None — independent data layer refactor. Can run in parallel with other plans.
**Files touched:**
- **New:** `app/src/main/assets/game/heroes.json`
- **New:** `app/src/main/assets/game/monsters.json`
- **New:** `app/src/main/assets/game/equipment.json`
- **New:** `app/src/main/assets/game/combos.json`
- **New:** `app/src/main/assets/game/trophies.json`
- **New:** `app/src/main/java/com/example/game/persistence/DataLoader.kt`
- **Modify:** `Hero.kt` — delete `HeroDefinitions` object, keep `Hero`/`HeroInstance` data classes
- **Modify:** `Monster.kt` — delete `MonsterDefinitions` object, keep data classes
- **Modify:** `Equipment.kt` — delete `EquipmentDefinitions` object, keep data classes
- **Modify:** `ComboSkill.kt` — delete `ComboSkillDefinitions` object, keep data classes
- **Modify:** `Trophy.kt` — delete `TrophyDefinitions` object, keep data classes
- **Modify:** All files referencing `*.Definitions.*` — update imports to use `DataLoader.*`
- **Modify:** `GameViewModel.kt` — call `DataLoader.init(context)` in `init{}`
- **Modify:** `GameApp.kt` — or pass context to DataLoader

**Priority:** Medium

---

## Guiding principle

**Clean break.** Remove the old `Definitions` objects entirely. Every call site switches to `DataLoader`. No backward-compatibility bridge.

---

## 1. Create JSON files under `assets/game/`

### 1.1 heroes.json

Contains `Hero` data (stats, skills, ultimate, theme colors) for all 5 heroes. Skills are embedded within each hero entry (not a separate file), since skills are tightly coupled to their hero.

**Schema:**
```json
[
  {
    "id": "shanti",
    "name": "Shanti",
    "description": "The tranquil healer who channels the calming waters of inner peace...",
    "element": "WATER",
    "role": "HEALER",
    "baseStats": { "maxHp": 800, "attack": 70, "defense": 65, "speed": 70 },
    "unlockLevel": 1,
    "skills": [
      {
        "id": "shanti_skill_1",
        "name": "Aqua Burst",
        "description": "A burst of healing water",
        "targetType": "SINGLE_ALLY",
        "damageComponents": [],
        "baseDamage": 0,
        "hits": 1,
        "healScaling": { "stat": "ATK", "ratio": 0.8, "baseHeal": 60 },
        "shieldScaling": null,
        "statusEffects": [],
        "buffs": [],
        "cleanse": false,
        "revive": false,
        "speed": "NORMAL",
        "cooldown": 0
      }
    ],
    "ultimate": {
      "id": "shanti_ultimate",
      "name": "Tranquil Cascade",
      "description": "Channel pure water energy to restore all allies",
      "targetType": "ALL_ALLIES",
      "healScaling": { "stat": "ATK", "ratio": 1.5, "baseHeal": 200 }
    },
    "colorTheme": { "primary": "0xFF4FC3F7", "secondary": "0xFF0288D1" },
    "quote": "Peace flows like water...",
    "uniqueItems": ["shanti_calm_waters"]
  }
]
```

**Important:** Enum values (`WATER`, `HEALER`, `SINGLE_ALLY`, `NORMAL`) must match the exact enum names in Kotlin. Use `@SerializedName` annotations in the data classes where JSON keys differ from Kotlin field names.

Also include `Hero.uniqueItems` (list of equipment IDs) and any other hero-specific fields from the data class.

### 1.2 monsters.json

All 16 monsters with stats, phases, AI behavior, boss flags, mechanics descriptions.

**Schema:**
```json
[
  {
    "id": "bhaya",
    "name": "Bhaya",
    "englishName": "Fear",
    "element": "SHADOW",
    "baseStats": { "maxHp": 500, "attack": 40, "defense": 30, "speed": 60 },
    "specialAttack": { "name": "Paralyzing Grip", "description": "...", "targetType": "SINGLE_ENEMY", "baseDamage": 1.2 },
    "mechanics": "Shadow tendrils that drain courage",
    "difficultyTier": "EASY",
    "phases": [],
    "isBoss": false,
    "aiBehavior": { "targetStrategy": "RANDOM", "actionPattern": ["BASIC", "BASIC", "SPECIAL"] }
  }
]
```

### 1.3 equipment.json

All ~35 equipment items + 3 set bonuses.

**Schema:**
```json
{
  "items": [
    {
      "id": "generic_weapon_1",
      "name": "Training Sword",
      "slot": "WEAPON",
      "tier": "GENERIC",
      "levelRequirement": 1,
      "description": "A basic training weapon",
      "effects": [{ "type": "ATK_PERCENT", "value": 5.0, "target": "SELF" }],
      "bonusDescriptions": ["ATK +5%"],
      "heroClassRequirement": null,
      "heroIdRequirement": null,
      "cost": { "sparks": 0 },
      "colorTheme": { "primary": "0xFF9E9E9E", "secondary": "0xFF616161" },
      "iconName": "sword"
    }
  ],
  "setBonuses": [
    {
      "id": "virya_set",
      "name": "Burning Vigor",
      "heroId": "virya",
      "piecesRequired": 2,
      "effects": [{ "type": "ATK_PERCENT", "value": 15.0, "target": "SELF" }],
      "description": "Virya's flame armor and blade set"
    }
  ]
}
```

### 1.4 combos.json

All 20 combos with required heroes and effects.

**Schema:**
```json
[
  {
    "id": "purifying_shelter",
    "name": "Purifying Shelter",
    "description": "Shanti and Santosha...",
    "requiredHeroes": ["shanti", "santosha"],
    "type": "TWO_HERO",
    "targetType": "ALL_ALLIES",
    "healComponents": [{ "stat": "ATK", "ratio": 1.2, "baseHeal": 150 }],
    "shieldComponents": [{ "stat": "ATK", "ratio": 0.8, "baseShield": 100 }],
    "damageComponents": [],
    "buffs": [{ "type": "DAMAGE_REDUCTION", "value": 20.0, "turns": 2 }],
    "statusEffects": [],
    "cleanse": true,
    "revive": false,
    "speed": "NORMAL",
    "cooldown": 3
  }
]
```

### 1.5 trophies.json

All 27 trophies (16 badges + 11 special achievements).

**Schema:**
```json
[
  {
    "id": "defeat_bhaya",
    "name": "Fear Vanquished",
    "description": "Defeat Bhaya, the manifestation of Fear",
    "rarity": "BRONZE",
    "category": "BADGE",
    "condition": { "type": "DEFEAT_MONSTER", "params": { "monsterId": "bhaya" } }
  }
]
```

---

## 2. Create DataLoader.kt

**Path:** `app/src/main/java/com/example/game/persistence/DataLoader.kt`

```kotlin
package com.example.game.persistence

import android.content.Context
import com.example.game.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DataLoader {

    private lateinit var context: Context
    private val gson = Gson()

    val heroes: List<Hero> by lazy { loadList("heroes.json") }
    val monsters: List<Monster> by lazy { loadList("monsters.json") }
    val equipment: List<Equipment> by lazy { loadList("equipment.json") }
    val combos: List<ComboSkill> by lazy { loadList("combos.json") }
    val trophies: List<Trophy> by lazy { loadList("trophies.json") }

    val equipmentSetBonuses: List<SetBonus> by lazy {
        val text = context.assets.open("game/equipment.json")
            .bufferedReader().use { it.readText() }
        val map = gson.fromJson(text, Map::class.java)
        val bonuses = map["setBonuses"] as? List<Map<String, Any>> ?: emptyList()
        gson.fromJson(gson.toJson(bonuses), object : TypeToken<List<SetBonus>>() {}.type)
    }

    fun init(appContext: Context) {
        context = appContext.applicationContext
        // Force initialization
        heroes
        monsters
        equipment
        combos
        trophies
        if (::equipmentSetBonuses.isInitialized) equipmentSetBonuses
    }

    fun getHero(id: String): Hero = heroes.first { it.id == id }

    fun getHeroForLevel(level: Int): Hero =
        heroes.filter { level >= it.unlockLevel }.maxByOrNull { it.unlockLevel }
            ?: heroes.first()

    fun getMonster(id: String): Monster = monsters.first { it.id == id }

    fun getEquipment(id: String): Equipment = equipment.first { it.id == id }

    fun getCombo(id: String): ComboSkill = combos.first { it.comboId == id }

    fun findCombo(heroIds: List<String>): ComboSkill? =
        combos.firstOrNull { it.requiredHeroes.toSet() == heroIds.toSet() }

    fun getTrophy(id: String): Trophy = trophies.first { it.id == id }

    fun getTrophiesByCategory(category: TrophyCategory): List<Trophy> =
        trophies.filter { it.category == category }

    private inline fun <reified T> loadList(file: String): List<T> {
        val json = context.assets.open("game/$file").bufferedReader().use { it.readText() }
        return gson.fromJson(json, object : TypeToken<List<T>>() {}.type)
    }
}
```

### Key decisions:
- **Lazy loading:** Each list loads on first access. `init()` forces eager load.
- **Gson re-use:** Single `Gson` instance.
- **Utility methods:** Mirror the existing API from `HeroDefinitions`, `MonsterDefinitions`, etc. so call sites can switch easily.

---

## 3. Delete Definitions objects

### Hero.kt — delete `HeroDefinitions` object

The `HeroDefinitions` object (with `heroes`, `getHero`, `getHeroForLevel`, `createInstance`) is deleted entirely. `createInstance` logic can move to a companion or extension function:

```kotlin
data class HeroInstance(
    val heroId: String,
    val hero: Hero,
    val level: Int,
    var currentHp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    var isAlive: Boolean = true,
    var shield: Int = 0,
    var ultimateGauge: Float = 0f,
    val equippedItems: MutableList<String> = mutableListOf()
)

// Move createInstance logic here
fun Hero.createInstance(level: Int): HeroInstance {
    val hpScale = 1.0 + (level - 1) * 0.1
    val statScale = 1.0 + (level - 1) * 0.05
    return HeroInstance(
        heroId = id,
        hero = this,
        level = level,
        currentHp = (baseStats.maxHp * hpScale).toInt(),
        maxHp = (baseStats.maxHp * hpScale).toInt(),
        attack = (baseStats.attack * statScale).toInt(),
        defense = (baseStats.defense * statScale).toInt(),
        speed = baseStats.speed
    )
}
```

### Monster.kt — delete `MonsterDefinitions` object

Delete the `MonsterDefinitions` object. Move `createInstance` to an extension function:

```kotlin
fun Monster.createInstance(level: Int): MonsterInstance {
    val hpScale = 1.0 + (level - 1) * 0.15
    val statScale = 1.0 + (level - 1) * 0.08
    return MonsterInstance(
        monsterId = id,
        monster = this,
        level = level,
        currentHp = (baseStats.maxHp * hpScale).toInt(),
        maxHp = (baseStats.maxHp * hpScale).toInt(),
        attack = (baseStats.attack * statScale).toInt(),
        defense = (baseStats.defense * statScale).toInt(),
        speed = baseStats.speed
    )
}
```

### Equipment.kt — delete `EquipmentDefinitions` object

Delete the `EquipmentDefinitions` object. Keep `Equipment`, `EquipmentEffect`, `EquipmentSlot`, `EquipmentTier`, `HeroClass`, `SetBonus`, etc.

### ComboSkill.kt — delete `ComboSkillDefinitions` object

Delete `ComboSkillDefinitions`. Keep `ComboSkill`, `ComboType`.

### Trophy.kt — delete `TrophyDefinitions` object

Delete `TrophyDefinitions`. Keep `Trophy`, `TrophyRarity`, `TrophyCategory`, `TrophyCondition`.

---

## 4. Update all call sites

Search the entire `com.example.game` package for references to:
- `HeroDefinitions.` → `DataLoader.`
- `MonsterDefinitions.` → `DataLoader.`
- `EquipmentDefinitions.` → `DataLoader.`
- `ComboSkillDefinitions.` → `DataLoader.`
- `TrophyDefinitions.` → `DataLoader.`

### Specific files to update:

| File | What to change |
|------|----------------|
| `GameViewModel.kt` | All references to Definitions objects |
| `BattleActions.kt` | ComboSkillDefinitions → DataLoader |
| `BattleScreen.kt` | Any monster/hero references |
| `BattleHUD.kt` | Any definitions references |
| `BattleCanvas.kt` | Any hero/monster lookups |
| `BattleResultScreen.kt` | Trophy definitions |
| `HubScreen.kt` | Monster/hero references |
| `MonsterRoadSelection.kt` | MonsterDefinitions |
| `PartyScreen.kt` | HeroDefinitions, EquipmentDefinitions |
| `ShopScreen.kt` | EquipmentDefinitions, HeroDefinitions |
| `TrophyScreen.kt` | TrophyDefinitions |
| `TrophyModal.kt` | TrophyDefinitions |
| `GameSaveManager.kt` | Any definitions references |

### Typical pattern change:

**Before:**
```kotlin
val hero = HeroDefinitions.getHero("shanti")
val monsters = MonsterDefinitions.monsters
```

**After:**
```kotlin
val hero = DataLoader.getHero("shanti")
val monsters = DataLoader.monsters
```

---

## 5. Initialize DataLoader

### In GameViewModel

```kotlin
class GameViewModel(application: Application) : AndroidViewModel(application) {
    init {
        DataLoader.init(application)
    }
}
```

Since `GameViewModel` is already an `AndroidViewModel` (has `Application` access), this is straightforward. Add the call before any data loading.

### Alternative: In GameApp or MainActivity

If any UI code needs data before the ViewModel is created, initialize earlier in `GameApp` or `MainActivity.onCreate`:

```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DataLoader.init(this)  // ensure data is ready
    // ...
}
```

**Recommendation:** Initialize in both `GameViewModel.init` AND `MainActivity.onCreate` for safety.

---

## 6. Gson annotations

Some data classes may need `@SerializedName` annotations if JSON keys differ from Kotlin field names. Review each data class and add annotations where needed.

**Example:**
```kotlin
data class Hero(
    val id: String,
    val name: String,
    val description: String,
    val element: Element,
    val role: HeroRole,
    @SerializedName("baseStats") val baseStats: HeroStats,
    @SerializedName("unlockLevel") val unlockLevel: Int,
    val skills: List<Skill>,
    val ultimate: Skill,
    @SerializedName("colorTheme") val colorTheme: HeroColorTheme,
    val quote: String,
    @SerializedName("uniqueItems") val uniqueItems: List<String>
)
```

**Types that need extra attention:**
- Enums (`Element`, `HeroRole`, `TargetType`, `DamageType`, etc.) — Gson maps string → enum by name, so JSON values must match Kotlin enum names exactly
- Nested objects (like `baseStats`, `colorTheme`, `healScaling`, `phases`) — may need nested data classes with their own `@SerializedName` annotations

---

## Verification

1. Build: `./gradlew :app:assembleDebug` — must compile cleanly with no references to deleted Definitions objects
2. Data loading: The app should start without crashes, and `DataLoader.heroes` should return all 5 heroes
3. Gameplay: Battle, shop, party, trophy screens should display data correctly (names, stats, descriptions, icons)
4. No runtime `FileNotFoundException` for any JSON file under `assets/game/`
