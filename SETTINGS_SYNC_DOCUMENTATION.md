# Settings Synchronization Implementation

## Overview
Settings are now saved in both **Firebase Firestore** (cloud) and **Room Database** (offline cache) with **remote-first priority**. This dual-source pattern ensures settings are always available, whether online or offline.

## Architecture

### Key Components

1. **AppSettings Entity** (`data/models/AppSettings.kt`)
   - Room entity with `userId` field for multi-user support
   - Fields: temperatureUnit, windSpeedUnit, pressureUnit, visibilityUnit, notificationsEnabled, theme, language

2. **AppSettingsDao** (`data/local/AppSettingsDao.kt`)
   - Room DAO for local persistence
   - Methods for CRUD operations on AppSettings
   - Reactive Flow-based queries for real-time updates

3. **SettingsRepository** (`data/repository/SettingsRepository.kt`)
   - Unified repository implementing remote-first sync pattern
   - Fetches from Firebase first, falls back to Room on failure
   - Syncs Firebase data to Room automatically for offline access
   - All writes go to Firebase first, then to Room

4. **SettingsViewModel** (`viewmodel/SettingsViewModel.kt`)
   - Manages settings state and user interactions
   - Uses SettingsRepository for all data operations
   - Tracks current user ID for per-user settings management

### Database Schema

#### Migration v3 → v4
Creates the `app_settings` table:
```sql
CREATE TABLE app_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    userId INTEGER NOT NULL,
    temperatureUnit TEXT NOT NULL DEFAULT 'C',
    windSpeedUnit TEXT NOT NULL DEFAULT 'km/h',
    pressureUnit TEXT NOT NULL DEFAULT 'mb',
    visibilityUnit TEXT NOT NULL DEFAULT 'km',
    notificationsEnabled INTEGER NOT NULL DEFAULT 1,
    theme TEXT NOT NULL DEFAULT 'light',
    language TEXT NOT NULL DEFAULT 'en'
)
```

## Synchronization Flow

### On Login
1. User logs in via AuthViewModel
2. `settingsViewModel.syncSettingsOnLogin(userId, firebaseUid)` is called
3. Fetches settings from Firestore using Firebase UID
4. Syncs data to Room for offline access
5. `loadSettingsForUser(userId)` loads settings reactively

### Reading Settings
**Remote-first flow:**
1. Try to fetch from Firebase Firestore
2. Sync received data to Room automatically
3. If Firebase fails, fallback to Room
4. Emit settings to UI via Flow

### Writing Settings
**Remote-first flow:**
1. Write to Firebase Firestore first
2. On success, write to Room
3. If Firebase fails, still write to Room as offline cache
4. Update UI state optimistically

### Methods in SettingsRepository

#### Reading
- `getSettings(userId): Flow<AppSettings>` - Reactive, remote-first with auto-sync
- `getSettingsOnce(userId): AppSettings` - One-time fetch, remote-first
- `syncFirestoreSettingsToRoom(userId, firebaseUid)` - Manual sync on login

#### Writing
- `saveTemperatureUnit(userId, unit)` - Remote-first
- `saveWindSpeedUnit(userId, unit)` - Remote-first
- `savePressureUnit(userId, unit)` - Remote-first
- `saveVisibilityUnit(userId, unit)` - Remote-first
- `saveTheme(userId, theme)` - Remote-first
- `saveNotificationsEnabled(userId, enabled)` - Remote-first
- `saveLanguage(userId, language)` - Remote-first
- `saveAllSettings(userId, settings)` - Remote-first bulk save

## Usage in ViewModels

### SettingsViewModel
```kotlin
// Initialize with repository injection
val settingsViewModel = SettingsViewModel(preferencesHelper, settingsRepository)

// Load settings on login
settingsViewModel.syncSettingsOnLogin(userId, firebaseUid)

// Load settings reactively
settingsViewModel.loadSettingsForUser(userId)

// Update individual settings
settingsViewModel.updateTemperatureUnit("F")
settingsViewModel.updateTheme("dark")
settingsViewModel.updateNotifications(true)

// Subscribe to settings
settingsViewModel.settings.collect { appSettings ->
    // React to settings changes
}
```

## Integration with WeatherApp

### SettingsViewModel Initialization
```kotlin
val settingsViewModel: SettingsViewModel = viewModel(
    factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                app.preferencesHelper,
                app.settingsRepository
            ) as T
        }
    }
)
```

### Login Flow
```kotlin
onLoginSuccess = {
    val userId = authViewModel.loggedInUser.value?.id ?: 0
    val firebaseUid = authViewModel.firebaseUid.value
    
    // Sync settings from Firebase to Room
    settingsViewModel.syncSettingsOnLogin(userId, firebaseUid)
    
    // Load locations
    locationViewModel.loadLocationsForUser(userId, firebaseUid) { ... }
    
    // Navigate to dashboard
    navigationController.navigate(AppScreen.Dashboard)
}
```

### Register Flow
```kotlin
onRegisterSuccess = {
    val userId = authViewModel.loggedInUser.value?.id ?: 0
    if (userId > 0) {
        // Initialize default settings for new user
        settingsViewModel.loadSettingsForUser(userId)
    }
    navigationController.navigateBack()
}
```

## Firebase Structure

Settings are stored in Firestore under:
```
users/{firebaseUid}/settings/preferences
```

Example document:
```json
{
    "temperatureUnit": "C",
    "windSpeedUnit": "km/h",
    "pressureUnit": "mb",
    "visibilityUnit": "km",
    "notificationsEnabled": true,
    "theme": "light",
    "language": "en"
}
```

## Offline Behavior

- **If offline and settings exist locally**: Use Room data
- **If offline and settings not in Room**: Use defaults
- **When back online**: Automatically sync to/from Firebase
- **Firebase is source of truth**: Remote data always takes priority when available

## Error Handling

- **Firestore errors**: Logged but don't block; falls back to Room
- **Room errors**: Logged; Firebase still updated
- **No network**: Gracefully falls back to local cache
- **Network restored**: Automatic re-sync on next operation

## Testing Scenarios

### Scenario 1: Online Login
1. User logs in with internet
2. Settings fetched from Firebase
3. Synced to Room
4. Settings available offline after sync

### Scenario 2: Offline Setting Change
1. User changes setting while offline
2. Written to Room immediately
3. On reconnect, synced to Firebase

### Scenario 3: Cross-Device Sync
1. Settings changed on Device A
2. Firebase updated
3. Device B fetches on next read
4. Automatically synced to Room on Device B

## Migration Notes

If users have existing settings in DataStore/SharedPreferences:
- Old PreferencesHelper is still available for backward compatibility
- Can migrate old data to Room/Firebase separately if needed
- New settings flow through unified SettingsRepository

## Performance Considerations

- **Remote-first reads**: Async, non-blocking via Firestore listeners
- **Room as cache**: Immediate offline access without network
- **Batching**: `saveAllSettings()` for bulk updates to reduce Firebase writes
- **Logging**: All operations logged for debugging

## Future Enhancements

1. Settings sync conflict resolution (last-write-wins vs. merge strategies)
2. Settings versioning and rollback
3. Bulk migration tools for old preferences
4. Settings backup/restore functionality
5. Settings analytics and usage tracking
