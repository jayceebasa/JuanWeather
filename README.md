# Juan Weather 🌤️

A modern Android weather application built with **Jetpack Compose**, providing real-time weather information, location management, emergency contacts, and SOS functionality.

## 📅 Sprint Highlights

### Sprint 3 – Local Persistence & User Management
- Added the full Room stack (`AppDatabase`, `UserDao`, `UserLocationDao`, `UserRepository`) with schema migrations from v1→v2 (adds the `role` column) and v2→v3 (creates `user_locations`).
- Implemented `AuthViewModel` and `LocationViewModel` logic for registration, login, RBAC, duplicate-city checks, and the swap-with-home workflow seen in `WeatherScreens.kt`.
- Validated functionality through nine `WeatherViewModelTest` cases and nine `LocationViewModelTest` cases under `app/src/test/java/com/juanweather/`, covering CRUD flows, error handling, and loading states.

### Sprint 4 – Remote Data Integration
- Standardized on a REST-only architecture using WeatherAPI.com via Retrofit/OkHttp (`data/remote/ApiClient.kt`, `WeatherApiService.kt`) and Gson-mapped models in `data/models/Weather.kt`.
- `WeatherRepository` now exposes forecast fetchers plus mapping helpers (`mapHourlyForecast`, `mapDailyForecast`, `mapMetrics`) consumed by `WeatherViewModel` and `LocationViewModel` for UI-ready data.
- Hardened error handling: both ViewModels wrap API calls in try/catch/finally blocks, surface user-friendly errors, validate city names before inserts, and fall back to placeholder cards when a single city fails to load.

## 📱 Features

### Core Weather Features
- **Real-time Weather Dashboard** - Display current weather conditions with temperature, humidity, pressure, and "real feel"
- **Hourly Forecast** - 7-hour weather forecast with icons and temperature
- **Daily Forecast** - 5-day daily weather predictions
- **Weather Metrics** - Detailed metrics including humidity, real feel temperature, UV index, and atmospheric pressure
- **Multi-Location Support** - Add and manage multiple locations

### Authentication & Security
- **Secure Login** - Authentication via Firebase with a local Room cache for user profile/role and session restoration
- **Email Validation** - Real-time email format validation with visual feedback (the special username `admin` is supported)
- **Session Management** - Logout clears the local session state
- **Admin Account Seeded on First Run** - Default admin is auto-created locally if missing:
  - Email/Username: `admin`
  - Password: `admin123`

### Settings & Preferences
- **Weather Preferences** - Customize temperature units (°C/°F), wind speed units, pressure units, and visibility units
- **Emergency Contact Management** - Add/edit/delete emergency contacts
- **SOS Settings** - Configure emergency message template + optional location sharing

### Safety Features
- **SOS Button** - Quick emergency alert system with confirmation
- **Multi-Contact SOS Delivery** - Sends alerts to *each* configured emergency contact (one SMS per contact)
- **Location Sharing** - Optional location URL inclusion when enabled in SOS Settings (requires location permission)
- **Validation on SOS** - If no emergency contacts exist, the dashboard blocks sending and prompts the user to add contacts

## 🏗️ Project Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with a custom `NavigationController`
- **Target SDK**: Android 14 (API 34) *(verify in Gradle if you change this)*
- **Min SDK**: 21
- **Networking**: Retrofit + OkHttp
- **Local Storage**: Room Database
- **Image Loading**: Coil & Glide
- **Location Services**: Google Play Services Location
- **Async**: Kotlin Coroutines

### Project Structure

```
JuanWeather/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/juanweather/
│   │   │   ├── JuanWeatherApp.kt          # Application entry point (seeds admin)
│   │   │   ├── data/
│   │   │   │   ├── local/                 # Room Database DAOs
│   │   │   │   ├── models/                # Data models & entities
│   │   │   │   ├── remote/                # API services
│   │   │   │   └── repository/            # Data repositories
│   │   │   ├── ui/
│   │   │   │   ├── WeatherApp.kt          # Main navigation & app structure
│   │   │   │   ├── activities/            # Activity components
│   │   │   │   ├── screens/               # Composable screens
│   │   │   │   ├── components/            # Reusable UI components
│   │   │   │   ├── fragments/             # Fragment implementations
│   │   │   │   └── models/                # UI models & states
│   │   │   ├── utils/                     # Utility functions & helpers (SMS, phone validation)
│   │   │   └── viewmodel/                 # ViewModels for MVVM
│   │   └── res/                           # Resources
│   └── build.gradle.kts                   # Dependencies & build config
├── gradle/
│   └── libs.versions.toml                 # Gradle version catalog
└── README.md
```

## 🛠️ Installation & Setup

### Prerequisites
- Android Studio (Giraffe or later)
- JDK 11+
- Android SDK (Min 21)
- Google Play Services (for location)

### Steps

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd JuanWeather
   ```

2. **Open in Android Studio**
   - File → Open → Select JuanWeather folder

3. **Build the Project**
   ```bash
   ./gradlew build
   ```

4. **Run on Device/Emulator**
   ```bash
   ./gradlew installDebug
   ```

5. **Login**
   - Regular demo user (if present in your environment): `juan23@gmail.com` / `juan23`
   - Admin (auto-seeded locally): `admin` / `admin123`

## 📱 Screen Navigation

```
┌─────────────────────────────────────┐
│         Login Screen                │
│  (Email & Password Authentication)  │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│    Weather Dashboard                │
│  (Main weather display & forecast)  │
└────────┬──────────────────────┬─────┘
         │                      │
         ▼                      ▼
    ┌─────────────┐      ┌──────────────┐
    │ Add Location│      │Settings Menu │
    └─────────────┘      └──────┬───────┘
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
              ┌──────────┐ ┌──────────┐ ┌──────────┐
              │ Weather  │ │Emergency │ │   SOS    │
              │Preferences│ │ Contacts │ │ Settings │
              └──────────┘ └──────────┘ └──────────┘
```

## 🆘 SOS / Emergency System

### Sending SOS from the Dashboard
1. Tap the red **SOS** button.
2. If you **don’t have emergency contacts**, the app will block sending and prompt you to add contacts in Settings.
3. Confirm the alert.
4. The app sends **one SMS per contact** using the integrated SMS gateway (**FMCSMS**).

### Message Format
SOS messages follow this pattern:

```
EMERGENCY ALERT FROM <User Name>

<Message Template>

Sent from JuanWeather
<Location URL if enabled>
```

### Location Sharing
- Enable/disable in **SOS Settings**.
- Requires runtime permissions (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`).

## 📞 Emergency Contact Validation

Phone numbers are validated and formatted by `PhoneNumberValidator`.

Supported formats:
- Philippine: `+639XXXXXXXXX`, `09XXXXXXXXX`, or `9XXXXXXXXX`
- International: `+<countrycode>` with at least 6 digits after the country code

## 📊 Data Management

### Local Storage (Room Database)
- Users and roles (RBAC)
- Saved locations
- SOS settings
- Emergency contacts

### Remote Services
- Weather forecast data from WeatherAPI.com
- Authentication + user-scoped data synchronization via Firebase (where configured)

## 🔒 Notes on Security Claims
- This repo clears local session state on logout.
- If you need encrypted storage for local secrets/session info, add AndroidX Security Crypto (not currently enforced everywhere).

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests (Android Device/Emulator)
```bash
./gradlew connectedAndroidTest
```

## 🚀 Build & Release

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

## 🐛 Troubleshooting

**SOS doesn’t send messages**
- Verify FMCSMS configuration is present (API key, base URL, sender/from number)
- Ensure your device/emulator has internet access

**Location sharing doesn’t work**
- Grant runtime permissions
- Ensure location services are enabled on the device/emulator

---

**Version**: 1.0  
**Last Updated**: March 2026
