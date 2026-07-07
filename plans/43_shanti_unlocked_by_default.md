# Plan 43: Ensure Shanti Is Unlocked by Default

## Problem

Despite `default_save.json` containing `"unlockedHeroIds": [1]` and `"party": [{"heroId": 1}]`, Shanti is still not showing as unlocked for some users. This likely occurs because:

1. Existing save files from before this default was set do not get migrated.
2. The `normalized()` function in `GameProgress` may not ensure Shanti is always in the party/unlocked set.

## Diagnosis

**`GameProgress.kt`** — check the `normalized()` function (likely on the `GameProgress` data class) to see if it adds Shanti when missing.

**`GameSaveManager.kt:68-76`** — the `loadDefaultSave()` function loads `default_save.json`, but this only applies to brand-new installs with no existing save.

**`GameViewModel.kt`** — the save data init/load path may not re-apply defaults if the save file exists but has no heroes.

## Fix

### 1. Add Shanti in `normalized()` if missing

**`GameProgress.kt`** — in the `normalized()` function (or create one), ensure Shanti (heroId = 1) is always present in both `unlockedHeroIds` and `party`:

```kotlin
fun normalized(): GameProgress {
    var result = this
    if (1 !in result.unlockedHeroIds) {
        result = result.copy(
            unlockedHeroIds = result.unlockedHeroIds + 1
        )
    }
    if (result.party.none { it.heroId == 1 }) {
        result = result.copy(
            party = result.party + PartyMemberData(heroId = 1)
        )
    }
    return result
}
```

Call `normalized()` at the end of the save-load process in `GameSaveManager` or `GameViewModel` when loading save data.

### 2. Ensure `normalized()` is called on every load

**`GameSaveManager.kt`** or **`GameViewModel.kt`** — verify that `normalized()` is called on the loaded save data, not just on `loadDefaultSave()`:

```kotlin
private fun loadSave(): GameProgress {
    return try {
        val json = readFromFile(...)
        gson.fromJson(json, GameProgress::class.java).normalized()
    } catch (e: Exception) {
        loadDefaultSave()
    }
}
```

### 3. Verify `default_save.json`

**`default_save.json`** — confirm the file already includes `"unlockedHeroIds": [1]` and `"party": [{"heroId": 1}]`. This should be correct already; the fix is in the migration/normalization layer.

## Files to modify

| File | Changes |
|---|---|
| `GameProgress.kt` | Add `normalized()` function (or extend existing) to ensure heroId=1 is always in `unlockedHeroIds` and `party` |
| `GameSaveManager.kt` | Call `normalized()` on every save load, not just default |

## Dependencies

- None. Standalone fix.
