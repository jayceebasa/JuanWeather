# FMCSMS Configuration Troubleshooting Guide

## Issue
"FMCSMS is not configured. Please contact administrator."

---

## Root Cause
The app was built **before** the FMCSMS credentials were added to `local.properties`. The `BuildConfig` is generated at build time, so it needs to be regenerated.

---

## ✅ Solution: Rebuild the Project

### Step 1: Verify `local.properties` is Complete
File: `local.properties`

```properties
sdk.dir=C\:\\Users\\JAYCEE\\AppData\\Local\\Android\\Sdk

# FMCSMS Configuration
FMCSMS_API_KEY=fmcsms_05034e905b42f54c3ca7977354f510091129f32c62cb65d4
FMCSMS_BASE_URL=https://fortmed.org/
FMCSMS_SENDER_NAME=JuanWeather
FMCSMS_FROM_NUMBER=+639189876543
```

**Status:** ✅ Verified

---

### Step 2: Clean and Rebuild

#### **Option A: Using Android Studio**
1. Click `Build` menu → `Clean Project`
2. Wait for completion (1-2 minutes)
3. Click `Build` menu → `Rebuild Project`
4. Wait for completion (2-5 minutes)
5. Run the app

#### **Option B: Using Terminal (PowerShell)**
```powershell
cd C:\Users\JAYCEE\AndroidStudioProjects\JuanWeather
.\gradlew.bat clean build
```

#### **Option C: Using Gradle Wrapper (Windows)**
```powershell
cd C:\Users\JAYCEE\AndroidStudioProjects\JuanWeather
.\gradlew.bat --stop
.\gradlew.bat clean
.\gradlew.bat build
```

---

## 📊 What Gets Generated

When you rebuild with the credentials in `local.properties`:

```
local.properties (input)
         ↓
gradle reads properties
         ↓
build.gradle.kts processes:
  buildConfigField("String", "FMCSMS_API_KEY", "\"$fmcsmsApiKey\"")
  buildConfigField("String", "FMCSMS_BASE_URL", "\"$fmcsmsBaseUrl\"")
  buildConfigField("String", "FMCSMS_SENDER_NAME", "\"$fmcsmsSenderName\"")
  buildConfigField("String", "FMCSMS_FROM_NUMBER", "\"$fmcsmsFromNumber\"")
         ↓
BuildConfig.java is generated with values:
  public static final String FMCSMS_API_KEY = "fmcsms_05034e905b42f54c3ca7977354f510091129f32c62cb65d4";
  public static final String FMCSMS_BASE_URL = "https://fortmed.org/";
  public static final String FMCSMS_SENDER_NAME = "JuanWeather";
  public static final String FMCSMS_FROM_NUMBER = "+639189876543";
         ↓
FmcSmsConfig reads from BuildConfig
         ↓
isConfigured() returns true ✓
```

---

## 🔍 Debug Logging

After rebuild, when you trigger the SOS alert, check Android Studio's **Logcat** for debug messages:

### Expected Output (Success)
```
D/FmcSmsConfig: API Key loaded: ✓ Present (64 chars)
D/FmcSmsConfig: Base URL loaded: https://fortmed.org/
D/FmcSmsConfig: Sender Name loaded: JuanWeather
D/FmcSmsConfig: From Number loaded: ✓ Present (+639189876543)
D/FmcSmsConfig: Configuration Status: ✓ CONFIGURED
  - API Key: ✓
  - Base URL: ✓
  - From Number: ✓
```

### Debug Output (Failure)
```
E/SOSViewModel: FMCSMS Configuration Check Failed!
E/SOSViewModel: API Key: EMPTY
E/SOSViewModel: Base URL: https://fortmed.org/
E/SOSViewModel: From Number: EMPTY
```

---

## ⚠️ Common Issues & Solutions

### Issue 1: BuildConfig Not Updated
**Symptom:** Still getting "not configured" error after rebuild

**Solution:**
- Close Android Studio completely
- Delete the `build/` folder in app directory
- Reopen Android Studio
- Rebuild the project

### Issue 2: BuildConfig.java Not Found
**Symptom:** Compile error about BuildConfig

**Solution:**
```powershell
cd app
.\gradlew.bat clean
.\gradlew.bat compileDebugKotlin
```

### Issue 3: Properties Not Read from local.properties
**Symptom:** Values are empty even after rebuild

**Solution:**
- Verify `local.properties` is in the **root** directory (not in `app/`)
- Check that no spaces are around the `=` sign
- Make sure file encoding is UTF-8

### Issue 4: Terminal Shows Gradle Daemon Issues
**Solution:**
```powershell
.\gradlew.bat --stop
.\gradlew.bat clean build
```

---

## 🧪 Testing After Rebuild

1. **Run the app**
2. **Navigate to SOS Settings**
3. **Add at least one emergency contact** (e.g., +639171234567)
4. **Click "Test SOS Alert"**
5. **Check for:**
   - No error message
   - SMS should be sent
   - Contact receives message

---

## 📝 Checklist

- [ ] `local.properties` exists in project root
- [ ] All 4 FMCSMS properties are present
- [ ] No empty values in properties
- [ ] `build.gradle.kts` has buildConfigField entries (verified ✓)
- [ ] Ran `gradle clean build`
- [ ] Restarted Android Studio
- [ ] BuildConfig shows correct values in logs
- [ ] SOS alert works without error

---

## 🔗 Related Files

- `local.properties` - Configuration file
- `app/build.gradle.kts` - Gradle build configuration
- `app/src/main/java/com/juanweather/utils/FmcSmsConfig.kt` - Config manager
- `app/src/main/java/com/juanweather/utils/FmcSmsService.kt` - SMS service
- `app/src/main/java/com/juanweather/viewmodel/SOSViewModel.kt` - SOS logic

---

## ✅ After Successful Rebuild

Once rebuilt successfully:
- ✅ `FmcSmsConfig.isConfigured()` returns `true`
- ✅ SMS can be sent to emergency contacts
- ✅ SOS alerts work properly
- ✅ No more "not configured" errors

