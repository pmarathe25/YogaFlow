# Plan 17: Fix Set Bonus Not Displayed

## Root Cause

In `ShopScreen.kt:GearDetailsDialog` (lines 296-301), the set bonus lookup uses `heroDef?.setBonusId` which is a string like `"Shanti"` from `heroes.json`:

```kotlin
val heroDef = DataLoader.heroes.find { it.id == item.heroId }
val setBonus = heroDef?.setBonusId?.let { sid ->
    DataLoader.setBonuses.find { it.name == sid }
}
```

But in `equipment.json`, the set bonuses are named differently:

| Hero | `setBonusId` (heroes.json) | Actual set bonus name (equipment.json) |
|---|---|---|
| Shanti | `"Shanti"` | `"Calming Current"` |
| Santosha | `"Santosha"` | `"Eternal Foundation"` |
| Virya | `"Virya"` | `"Raging Inferno"` |
| Dhairya | `"Dhairya"` | `"Inspiring Presence"` |
| Maitri | `"Maitri"` | `"Universal Love"` |

None of the hero `setBonusId` values match the actual set bonus names → `setBonus` is always `null` → the section never renders.

## Fix: Link sets to hero ID (Option A)

Add a `heroId` field to each set bonus in `equipment.json`, then look up by hero ID instead of name:

```json
{"name": "Calming Current", "heroId": 1, "requiredItems": ["shanti_prayer_beads", "shanti_tidal_staff"], "effects": [...]}
{"name": "Eternal Foundation", "heroId": 2, "requiredItems": ["santosha_foundation_stone", "santosha_earthen_bulwark"], "effects": [...]}
{"name": "Raging Inferno", "heroId": 3, "requiredItems": ["virya_ember_core", "virya_inferno_wrath"], "effects": [...]}
{"name": "Inspiring Presence", "heroId": 4, "requiredItems": ["dhairya_battle_standard", "dhairya_light_vanguard"], "effects": [...]}
{"name": "Universal Love", "heroId": 5, "requiredItems": ["maitri_universal_key", "maitri_wind_caress", "maitri_heart_embrace"], "effects": [...]}
```

Then in `GearDetailsDialog`:
```kotlin
val setBonus = item.heroId?.let { hid ->
    DataLoader.setBonuses.find { it.heroId == hid }
}
```

And update `SetBonus` data class:
```kotlin
data class SetBonus(
    val name: String,
    val heroId: Int? = null,
    val description: String,
    val requiredItems: List<String>,
    val effects: List<EquipmentEffect>
)
```

The lookup works because each set bonus is tied to a specific hero, and all items in that set belong to that same hero. If an item has no set (e.g. `shanti_serene_mantle`), no matching `heroId` exists in `setBonuses` → nothing renders — correct behavior.

## Additional issue: items not in any set

These unique items are intentionally set-less (no set bonus includes them):
- `shanti_serene_mantle` (only prayer_beads + tidal_staff are in "Calming Current")
- `santosha_contentment_beads` (only foundation_stone + earthen_bulwark are in "Eternal Foundation")
- `virya_blazing_mantle` (only ember_core + inferno_wrath are in "Raging Inferno")
- `dhairya_courage_circlet` (only battle_standard + light_vanguard are in "Inspiring Presence")

No fix needed — these legitimately have no set bonus and shouldn't show one.

## Files to modify

| File | Changes |
|---|---|
| `equipment.json` | Add `"heroId"` to each set bonus object |
| `Equipment.kt` | Add `heroId: Int? = null` to `SetBonus` |
| `ShopScreen.kt:296-301` | Change lookup to `DataLoader.setBonuses.find { it.heroId == item.heroId }` |
