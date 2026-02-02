# Juan Weather 🌤️

A modern Android weather application built with **Jetpack Compose**, providing real-time weather information, location management, emergency contacts, and SOS functionality.

## 📱 Features

### Core Weather Features
- **Real-time Weather Dashboard** - Display current weather conditions with temperature, humidity, pressure, and "real feel"
- **Hourly Forecast** - 7-hour weather forecast with icons and temperature
- **Daily Forecast** - 5-day daily weather predictions
- **Weather Metrics** - Detailed metrics including humidity, real feel temperature, UV index, and atmospheric pressure
- **Multi-Location Support** - Add and manage multiple locations

### Authentication & Security
- **Secure Login** - Email-based authentication (demo: juan23@gmail.com / juan23)
- **Email Validation** - Real-time email format validation with visual feedback
- **Session Management** - Secure logout functionality with session clearing

### Settings & Preferences
- **Weather Preferences** - Customize temperature units (°C/°F), wind speed units, and update frequency
- **Emergency Contact Management** - Add and manage emergency contacts
- **SOS Settings** - Configure emergency alert systems
- **Theme Customization** - Adjust app appearance and layout preferences

### Safety Features
- **SOS Button** - Quick emergency alert system with confirmations
- **Emergency Contacts** - One-tap emergency contact access
- **Location Sharing** - Share current location during emergencies

## 🏗️ Project Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose 1.6.1
- **Architecture**: MVVM with Navigation Controller
- **Target SDK**: Android 14+ (Min SDK 21)
- **Networking**: Retrofit 2.9.0 + OkHttp 4.11.0
- **Local Storage**: Room Database 2.6.1
- **Image Loading**: Coil 2.5.0 & Glide 4.16.0
- **Location Services**: Google Play Services 21.0.1
- **Async**: Kotlin Coroutines

### Project Structure

```
JuanWeather/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/juanweather/
│   │   │   ├── JuanWeatherApp.kt          # Application entry point
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
│   │   │   ├── utils/                     # Utility functions & helpers
│   │   │   └── viewmodel/                 # ViewModels for MVVM
│   │   └── res/                           # Resources (drawables, strings, etc)
│   └── build.gradle.kts                   # Dependencies & build config
├── gradle/
│   └── libs.versions.toml                 # Gradle version catalog
└── README.md                              # This file
```

## 🛠️ Installation & Setup

### Prerequisites
- Android Studio (Giraffe or later)
- JDK 11+
- Android SDK 21+ (Min), 36+ (Target)
- Google Play Services for location functionality

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
   - Email: `juan23@gmail.com`
   - Password: `juan23`

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
                    │
                    ▼
              ┌──────────────┐
              │About/Support │
              └──────────────┘
```

## 🔐 Login Features

### Email Validation
The login screen implements robust email validation:

```kotlin
// Accepted email formats:
✓ juan23@gmail.com
✓ user.name@example.com
✓ user+tag@domain.co.uk

// Invalid formats:
✗ juan23 (missing @)
✗ user@.com (missing domain)
✗ user@domain (missing TLD)
```

### Real-time Validation Feedback
- **Green indicator**: Valid email format
- **Red indicator**: Invalid email format
- **Error messages**: Specific guidance on what's wrong

## 🎯 Key Screens

### 1. Login Screen
- Email-based authentication with format validation
- Password visibility toggle
- Demo credentials display
- Error handling with user-friendly messages

### 2. Weather Dashboard
- Current weather conditions with large temperature display
- Hourly forecast (7 hours ahead)
- Daily forecast (5 days ahead)
- Key metrics: Humidity, Real Feel, UV Index, Pressure
- Quick access to Settings and Add Location
- SOS emergency button

### 3. Settings Screen
- Navigate to sub-settings
- Logout functionality
- User profile section
- Quick access to all configuration screens

### 4. Weather Preferences
- Temperature unit selection (°C/°F)
- Wind speed unit configuration
- Update frequency settings
- Local persistence of preferences

### 5. Emergency Contact Management
- Add/edit emergency contacts
- Quick call functionality
- Contact list display
- Delete contact options

### 6. SOS Settings
- Configure emergency alerts
- Set alert recipients
- Customize alert messages
- Location sharing options

### 7. About & Support
- App information and version
- Support contact details
- Privacy policy and terms
- Feedback submission

## 📊 Data Management

### Local Storage (Room Database)
- Weather data caching
- Location history
- Emergency contacts
- User preferences
- Settings storage

### Remote API Integration
- Weather API calls via Retrofit
- Real-time data fetching
- Error handling & retry logic
- Offline support with cached data

### Data Flow
```
API ←→ Repository ←→ ViewModel ←→ UI (Compose)
        ↓
    Local Database
```

## 🔄 State Management

### Navigation State
- Managed via `NavigationController` class
- Backstack-based navigation system
- Deep link support capabilities

### UI State
- Composed using Jetpack Compose state management
- `mutableStateOf()` for screen-level state
- ViewModel-backed state for complex logic

### Data State
- ViewModel-managed data
- LiveData for reactive updates
- Coroutines for async operations

## 🎨 UI Components

### Custom Components
- Weather icons (Sun, Cloud, Cloud Rain)
- Custom app bar with branding
- Settings cards and toggles
- Emergency contact cards
- SOS alert dialog

### Material 3 Integration
- Material Design 3 components
- Dynamic color support (Android 12+)
- Proper elevation and shadows
- Responsive layouts

## 🔒 Security Features

### Authentication
- Email format validation
- Password masking in input fields
- Secure session management
- Logout clears all authentication data

### Location Services
- Runtime permission requests
- Graceful fallback without location
- Location caching for efficiency

### Data Storage
- Encrypted shared preferences
- Room database with proper entity relationships
- API key security (store in BuildConfig)

## 📝 Dependencies

### Core
- androidx.core:core-ktx:1.12.0
- androidx.appcompat:appcompat:1.6.1
- androidx.activity:activity-compose:1.8.1

### Compose
- androidx.compose.ui:ui:1.6.1
- androidx.compose.material3:material3:1.1.2
- androidx.compose.material:material-icons-extended:1.6.1

### Networking
- com.squareup.retrofit2:retrofit:2.9.0
- com.squareup.okhttp3:okhttp:4.11.0
- com.google.code.gson:gson:2.10.1

### Database
- androidx.room:room-runtime:2.6.1
- androidx.room:room-ktx:2.6.1

### Image Loading
- io.coil-kt:coil-compose:2.5.0
- com.github.bumptech.glide:glide:4.16.0

### Location & Async
- com.google.android.gms:play-services-location:21.0.1
- org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3

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

### ProGuard Minification
- Enabled for release builds
- Config in `proguard-rules.pro`
- Maintains API compatibility

## 📋 Build Configuration

- **Compile SDK**: 36 (Android 14)
- **Target SDK**: 36 (Android 14)
- **Min SDK**: 21 (Android 5.0)
- **Kotlin Version**: 1.9.x
- **Java Compatibility**: 11
- **Build Features**: ViewBinding, Compose

## 🐛 Troubleshooting

### Common Issues

**Login fails with invalid email**
- Ensure email follows format: `user@domain.com`
- Demo email: `juan23@gmail.com`

**Location services not working**
- Check LOCATION permission in AndroidManifest.xml
- Grant runtime location permissions
- Ensure device has location services enabled

**Gradle build fails**
- Run `./gradlew clean`
- Invalidate caches in Android Studio
- Sync gradle files

**UI not loading**
- Check Compose version compatibility
- Update Android Studio to latest version
- Clear build cache: `rm -rf app/build`

## 📚 Additional Resources

- [Android Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Retrofit Documentation](https://square.github.io/retrofit/)
- [Google Play Services](https://developers.google.com/android/guides/overview)

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Author

**Juan Weather Development Team**

## 📞 Support

For support, email: support@juanweather.com

---

**Version**: 1.0  
**Last Updated**: February 2026  
**Build Target**: Android 14+
