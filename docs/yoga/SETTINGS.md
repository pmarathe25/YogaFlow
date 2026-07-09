# Settings & Customization

## App Settings

Configured via `SettingsScreen.kt` and persisted by `SettingsViewModel` (backed by `SettingsManager.kt` / DataStore).

| Setting | Options | Default |
|---------|---------|---------|
| Theme | System, Light, Dark | System |
| Keep Screen Awake | On/Off | Off |
| Background Audio | On/Off | On |
| Voice Guide | On/Off | On |
| Voice Language | English, Sanskrit | English |
| Music Mute | On/Off | Off |
| Active Track | Track 0-5 | Track 0 |

## Practice Reminders

Managed by `ReminderViewModel` with Android alarm notifications.

- Per-flow reminders with time and day-of-week selection
- Tapping a reminder notification navigates directly to the flow detail screen
- Reminders stored in Room `reminders` table (`ReminderEntity`)
- Duplicate detection prevents multiple reminders for the same flow+time
- Alarms scheduled via `AlarmManager` with `setAlarmClock` for exact timing

## Data Management

- **Reset All Stats**: Clears all yoga sessions, resets XP/level/achievements
- Confirmation dialog prevents accidental data loss
- Game save data (Zen Battle) is stored separately in SharedPreferences and is NOT affected

## Implementation Details

- Settings: `SettingsManager.kt` (DataStore Preferences)
- Reminders: `ReminderManager.kt` (AlarmManager), `ReminderDao.kt`, `ReminderEntity.kt`
- ViewModels: `SettingsViewModel`, `ReminderViewModel`
- Screens: `SettingsScreen.kt`