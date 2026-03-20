# SOS Settings Integration with Twilio SMS

## Overview
This document describes the complete integration of Twilio SMS service for sending SOS alerts to emergency contacts in the JuanWeather app.

## Components Created

### 1. Data Models
- **SOSSettings.kt** - Room entity for storing SOS preferences
  - `enableLocationSharing`: Boolean flag to include location in SOS messages
  - `messageTemplate`: Custom SOS message template
  - `lastSentTime`: Timestamp of last sent SOS alert
  - Twilio credentials (Account SID, Auth Token, Phone Number)

### 2. Database Layer
- **SOSSettingsDao.kt** - Room DAO for SOS settings CRUD operations
  - `getSettings()`: Reactive Flow for observing settings changes
  - `getSettingsOnce()`: One-time fetch for settings
  - `updateSettings()`: Update SOS settings
  - `insertSettings()`: Insert new SOS settings

### 3. Repository Layer
- **SOSRepository.kt** - Data repository for SOS operations
  - `updateLocationSharing()`: Toggle location sharing
  - `updateMessageTemplate()`: Update SOS message
  - `updateLastSentTime()`: Track when SOS was last sent
  - `setTwilioCredentials()`: Store Twilio API credentials

### 4. Service Layer
- **TwilioSmsService.kt** - Service for sending SMS via Twilio API
  - `sendSMS()`: Send SMS to a single contact
  - `sendSOSToMultipleContacts()`: Send to multiple emergency contacts
  - Location URL attachment to SOS messages

### 5. ViewModel Layer
- **SOSViewModel.kt** - Business logic for SOS settings
  - `toggleLocation`: Track location sharing state
  - `messageTemplate`: Current SOS message
  - `sendSOS()`: Initiate SOS alert sending
  - Error and success message tracking
  - Location retrieval integration

### 6. UI Layer
- **SOSSettingsScreen** - Fully functional Compose UI
  - Location sharing toggle (Switch)
  - Message template editor (TextField)
  - Emergency contacts list display
  - Send SOS Alert button with loading state
  - Error and success message dialogs
  - Disabled state when no emergency contacts

## Setup Instructions

### Step 1: Add Twilio Credentials
Before using SOS functionality, you must configure your Twilio credentials:

```kotlin
// In your app initialization or settings configuration
sosViewModel.setTwilioCredentials(
    accountSid = "your_twilio_account_sid",
    authToken = "your_twilio_auth_token",
    phoneNumber = "+1234567890" // Your Twilio phone number
)
```

### Step 2: Emergency Contacts Setup
Users must add emergency contacts before SOS can be sent. These are managed in the Emergency Contacts screen.

### Step 3: Configure SOS Settings
Users can customize SOS behavior:
1. Toggle location sharing on/off
2. Edit the default SOS message template
3. View configured emergency contacts

## Features

### Location Sharing
When enabled, the SOS alert includes a Google Maps link with the user's current location:
```
Location: https://maps.google.com/?q=14.5995,120.9842
```

### Error Handling
- Validates emergency contacts exist before sending
- Provides clear error messages for failures
- Tracks success/failure for each contact
- Shows detailed error feedback to user

### SMS Service
- Uses Twilio REST API for reliable delivery
- HTTP Basic Authentication with account credentials
- URL encoding for message content
- Supports multi-contact bulk sending

## Database Migration
The app includes migration v5→v6 that creates the `sos_settings` table:

```sql
CREATE TABLE IF NOT EXISTS sos_settings (
    id TEXT PRIMARY KEY NOT NULL,
    enableLocationSharing INTEGER NOT NULL DEFAULT 1,
    messageTemplate TEXT NOT NULL DEFAULT 'I need help. This is an emergency SOS alert from JuanWeather.',
    lastSentTime INTEGER NOT NULL DEFAULT 0,
    twilioAccountSid TEXT NOT NULL DEFAULT '',
    twilioAuthToken TEXT NOT NULL DEFAULT '',
    twilioPhoneNumber TEXT NOT NULL DEFAULT ''
)
```

## Permissions Required
The following permissions are required in AndroidManifest.xml:
- `android.permission.INTERNET` - For Twilio API calls
- `android.permission.ACCESS_FINE_LOCATION` - For GPS location (optional)
- `android.permission.ACCESS_COARSE_LOCATION` - For network location (optional)
- `android.permission.SEND_SMS` - Already declared

## Dependencies
- Twilio SDK: `com.twilio.sdk:twilio:9.2.0`
- OkHttp3: For HTTP requests
- Coroutines: For async operations
- Room: For local storage

## Usage Example

```kotlin
// Observe emergency contacts
val contacts = emergencyContactViewModel.contacts.collectAsState(emptyList())

// Send SOS alert
sosViewModel.sendSOS(
    emergencyContacts = contacts.value.map { it.phoneNumber },
    includeLocation = true // Enable location sharing for this alert
)

// Observe for success/error
val successMessage = sosViewModel.successMessage.collectAsState(null)
val errorMessage = sosViewModel.errorMessage.collectAsState(null)
```

## Important Security Notes

⚠️ **Credentials Storage:**
- Current implementation stores Twilio credentials in unencrypted Room database
- **For production**, implement:
  - Encrypted SharedPreferences
  - Android KeyStore for credentials
  - Backend server for API calls (recommended)
  - Remove credentials from local storage when user logs out

⚠️ **Rate Limiting:**
- Consider implementing cooldown period between SOS alerts
- Prevent accidental multiple sends within short time frame
- Log all SOS activities for security audit

⚠️ **Compliance:**
- Ensure compliance with local emergency services regulations
- Verify with Twilio that account supports emergency numbers
- Get proper consent from users before sending emergency alerts

## Testing

To test SOS functionality:
1. Add test emergency contacts
2. Enable location sharing
3. Customize message template (optional)
4. Click "Send SOS Alert"
5. Verify SMS received at emergency contact numbers
6. Check success dialog appears
7. Verify location URL includes proper coordinates

## Troubleshooting

### SMS Not Sending
- Verify Twilio credentials are correctly set
- Check internet connection
- Ensure emergency contacts have valid phone numbers
- Check Twilio account has available SMS credits

### Location Not Included
- Verify location permissions are granted
- Check "Toggle location" is enabled in settings
- Ensure device has location services enabled

### Database Migration Issues
- Clear app data and reinstall if migration fails
- Check Room compiler generated code
- Verify version numbers match in AppDatabase

## Future Enhancements
- Push notification as alternative to SMS
- WhatsApp or Telegram integration
- Voice call support
- SMS delivery status tracking
- Scheduled check-in reminders
- SOS alert history/logs
- Geofencing-based triggers
