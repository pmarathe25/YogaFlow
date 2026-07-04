# Plan: Battle Rewards for Unique Items

One unique item per hero is moved from the shop to become a first-time-defeat reward:

| Monster | Position | Hero | Item |
|---------|----------|------|------|
| Bhaya (Self-Doubt & Fear) | #1 | Shanti | Shanti's Prayer Beads |
| Chinta (Anxiety & Worry) | #3 | Santosha | Santosha's Foundation Stone |
| Matsarya (Envy & Jealousy) | #5 | Virya | Virya's Ember Core |
| Dvesha (Aversion & Hatred) | #7 | Dhairya | Dhairya's Battle Standard |
| Lobha (Greed & Attachment) | #9 | Maitri | Maitri's Universal Key |

## 4a. Add reward field to Monster model

**File:** Need to locate the Monster model (likely `app/src/main/java/com/example/game/model/Monster.kt` — read this file during implementation).

Add an optional field:

```kotlin
val firstDefeatItemReward: String? = null
```

## 4b. Add reward field to monsters.json

**File:** `app/src/main/assets/game/monsters.json`

Add `"firstDefeatItemReward"` to the 5 monsters above. Example:

```json
{"id": "Bhaya", ..., "firstDefeatItemReward": "shanti_prayer_beads"}
```

The field goes alongside other monster fields like `isBoss`, `difficultyTier`, etc.

## 4c. Update onBattleWon in GameViewModel

**File:** `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt:353-363`

Modify `onBattleWon()` to check for first-defeat rewards:

```kotlin
private fun onBattleWon() {
    val monster = _currentMonster.value ?: return
    val data = _saveData.value
    val isFirstDefeat = monster.id.lowercase() !in data.defeatedMonsterIds

    var inventory = data.inventory
    if (isFirstDefeat && monster.firstDefeatItemReward != null) {
        inventory = inventory + monster.firstDefeatItemReward
    }

    _saveData.value = data.copy(
        totalBattlesWon = data.totalBattlesWon + 1,
        defeatedMonsterIds = data.defeatedMonsterIds + monster.id.lowercase(),
        inventory = inventory,
        lastPlayedTimestamp = System.currentTimeMillis()
    )
    saveGame()
}
```

## 4d. Hide reward items from the shop

**File:** `app/src/main/java/com/example/game/ui/components/ShopScreen.kt:105-107`

Filter out battle reward items from the shop listing:

```kotlin
val battleRewardItemIds = DataLoader.monsters
    .mapNotNull { it.firstDefeatItemReward }
    .toSet()

val available = DataLoader.equipment.filter { eq ->
    eq.slot == selectedCategory && eq.id !in battleRewardItemIds
}
```

This requires that the `Monster` model is accessible from the ShopScreen. `DataLoader.monsters` is already available.

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/java/com/example/game/model/Monster.kt` | Add `firstDefeatItemReward: String?` field |
| `app/src/main/assets/game/monsters.json` | Add `firstDefeatItemReward` to Bhaya, Chinta, Matsarya, Dvesha, Lobha |
| `app/src/main/java/com/example/game/viewmodel/GameViewModel.kt` | Update `onBattleWon()` to grant items on first defeat |
| `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` | Exclude reward items from the shop listing |
