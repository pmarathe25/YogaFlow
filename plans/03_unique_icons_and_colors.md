# Plan: Unique Icons + Hero Color Coding

## 2a. Add per-item icon field to equipment.json

**File:** `app/src/main/assets/game/equipment.json`

Add an `"icon"` field to every item object. Each item gets a unique emoji:

### Generic Items

| id | Icon |
|----|------|
| training_blade | 🗡️ |
| crystal_sword | ⚔️ |
| mythril_edge | 🔪 |
| traveler_cloak | 🧥 |
| enchanted_robe | 👘 |
| ethereal_vestment | ✨ |
| simple_beads | 📿 |
| swift_boots | 👢 |
| sages_tome | 📖 |
| ember_pendant | 🔥 |
| crystal_ward | 💎 |
| force_amulet | 🌀 |

### Unique Items

| id | Icon |
|----|------|
| shanti_prayer_beads | 🕊️ |
| shanti_tidal_staff | 🌊 |
| shanti_serene_mantle | 💙 |
| santosha_foundation_stone | 🪨 |
| santosha_earthen_bulwark | 🏔️ |
| santosha_contentment_beads | 🧘 |
| virya_ember_core | ❤️‍🔥 |
| virya_inferno_wrath | ⚡ |
| virya_blazing_mantle | 🔥 |
| dhairya_battle_standard | 🚩 |
| dhairya_light_vanguard | 🛡️ |
| dhairya_courage_circlet | 👑 |
| maitri_universal_key | 🔑 |
| maitri_wind_caress | 🍃 |
| maitri_heart_embrace | 💚 |

## 2b. Update Equipment model to read icon from JSON

**File:** `app/src/main/java/com/example/game/model/Equipment.kt`

- Add `val icon: String = "❓"` field to the `Equipment` data class
- Simplify `getIcon()` to return `icon` directly instead of the slot/tier emoji switch

```kotlin
fun getIcon(): String = icon
```

The JSON field `"icon"` maps automatically via Gson's field-name matching.

## 2c. Color-code unique items per hero

**File:** `app/src/main/java/com/example/game/ui/components/ShopScreen.kt`

In both `ShopItemCard` (line 172) and `GearDetailsDialog` (line 244), replace the `when`-on-tier block with a direct call to `item.getThemeColor()`:

```kotlin
// Before (ShopItemCard line 172):
color = when (item.tier) {
    EquipmentTier.UNIQUE -> Color(0xFFFFD740)
    EquipmentTier.CLASS_SPECIFIC -> Color(0xFFB388FF)
    EquipmentTier.GENERIC -> MaterialTheme.colorScheme.onSurface
}

// After:
color = item.getThemeColor()
```

Same change in `GearDetailsDialog` line 244.

`Equipment.getThemeColor()` already returns hero-specific colors for UNIQUE items:
- Shanti → Blue `#2196F3`
- Santosha → Brown `#795548`
- Virya → Red `#F44336`
- Dhairya → Yellow `#FFD54F` (adjust from `#FFEB3B` for readability)
- Maitri → Light Blue `#81D4FA` (adjust from `#E1F5FE` which was too pale)

GENERIC items will continue to return `Color.Gray` from the `else` branch, which is fine. Consider changing to `MaterialTheme.colorScheme.onSurface` for better visibility.

## Affected files

| File | Changes |
|------|---------|
| `app/src/main/assets/game/equipment.json` | Add `"icon"` field to all 27 items |
| `app/src/main/java/com/example/game/model/Equipment.kt` | Add `icon` field, simplify `getIcon()` |
| `app/src/main/java/com/example/game/ui/components/ShopScreen.kt` | Use `getThemeColor()` in both composables |
