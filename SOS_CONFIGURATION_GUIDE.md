u# SOS Settings - Configuration Examples

## Getting Twilio Credentials

### Step 1: Sign Up for Twilio
1. Visit https://www.twilio.com/try-twilio
2. Sign up with your email
3. Verify your email and phone number
4. Create a Twilio account

### Step 2: Get Your Credentials
1. Go to https://www.twilio.com/console
2. You'll see your **Account SID** (looks like: ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx)
3. Click on the eye icon to reveal **Auth Token** (looks like: your_auth_token_here)
4. Copy both values - you'll need them in the app

### Step 3: Get a Phone Number
1. In Twilio Console, go to Phone Numbers
2. Click "Get Started" or "Buy a Number"
3. Choose your country (Philippines recommended: +63xxx)
4. Select a number
5. Purchase/Activate it
6. Copy the phone number (with + prefix)

## Configuring in JuanWeather

### Option A: Configure in Settings Screen (Recommended)

```kotlin
// This would be done via UI when user configures SOS
// In a future enhancement, add a Settings page for Twilio config

sosViewModel.setTwilioCredentials(
    accountSid = "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    authToken = "your_auth_token_here",
    phoneNumber = "+63917123456"  // Your Twilio number
)
```

### Option B: Configure in App Initialization

Edit `MainActivity.kt`:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // After app initialization, set Twilio credentials
    val app = application as JuanWeatherApp
    val sosRepository = app.sosRepository
    
    lifecycleScope.launch {
        sosRepository.setTwilioCredentials(
            accountSid = "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
            authToken = "your_auth_token_here",
            phoneNumber = "+63917123456"
        )
    }
}
```

### Option C: Configure in WeatherApp Composable

Edit `ui/WeatherApp.kt`:
```kotlin
@Composable
fun WeatherApp() {
    val context = LocalContext.current
    val app = context.applicationContext as JuanWeatherApp
    
    // ... existing code ...
    
    // Initialize Twilio credentials once on app launch
    LaunchedEffect(Unit) {
        try {
            val settings = app.sosRepository.getSettingsOnce()
            if (settings?.twilioAccountSid.isNullOrEmpty()) {
                // First time setup
                app.sosRepository.setTwilioCredentials(
                    accountSid = "ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                    authToken = "your_auth_token_here",
                    phoneNumber = "+63917123456"
                )
            }
        } catch (e: Exception) {
            Log.e("WeatherApp", "Failed to initialize Twilio", e)
        }
    }
    
    // ... rest of code ...
}
```

## Example SOS Flow

### Complete User Journey

```
1. USER SETUP
   └─ Settings → Emergency Contact
      ├─ Add Contact: "Mom" → "+63917111111"
      ├─ Add Contact: "Brother" → "+63917222222"
      └─ Add Contact: "Hospital" → "+63917333333"

2. SOS CONFIGURATION
   └─ Settings → SOS Settings
      ├─ Toggle Location: ON (default)
      ├─ Message Template: "I'm in danger, need help! - John"
      └─ Show Contacts: 3 emergency contacts listed

3. EMERGENCY SITUATION
   └─ Click "Send SOS Alert"
      ├─ Get current GPS location (if enabled)
      ├─ Open Twilio REST API connection
      ├─ Send SMS to 3 contacts:
      │  ├─ Mom: "I'm in danger, need help! - John\n\nLocation: https://maps.google.com/?q=14.5995,120.9842"
      │  ├─ Brother: (same message)
      │  └─ Hospital: (same message)
      └─ Show: "SOS alert sent to 3 contact(s)"

4. EMERGENCY CONTACTS RECEIVE
   └─ SMS arrives in seconds
      ├─ Message includes location URL
      └─ Can click link to see exact location on Google Maps
```

## Code Example: Using SOSViewModel

```kotlin
@Composable
fun ExampleSOSUsage() {
    val context = LocalContext.current
    val app = (context.applicationContext as? JuanWeatherApp) ?: return
    
    // Create ViewModel
    val sosViewModel: SOSViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SOSViewModel(
                    repository = app.sosRepository,
                    smsService = TwilioSmsService(context),
                    locationManager = LocationManager(context)
                ) as T
            }
        }
    )
    
    // Get emergency contacts from another ViewModel
    val emergencyContacts by emergencyContactViewModel.contacts.collectAsState(emptyList())
    
    // Trigger SOS
    Button(
        onClick = {
            sosViewModel.sendSOS(
                emergencyContacts = emergencyContacts.map { it.phoneNumber },
                includeLocation = true
            )
        }
    ) {
        Text("Send SOS")
    }
    
    // Handle success
    val successMessage by sosViewModel.successMessage.collectAsState()
    if (successMessage != null) {
        AlertDialog(
            title = { Text("Success") },
            text = { Text(successMessage) },
            onDismissRequest = { sosViewModel.clearMessages() }
        )
    }
}
```

## Twilio SMS Cost

### Pricing Example (As of 2024)
- **Outbound SMS**: ~$0.0075 per SMS (varies by country)
- **Philippine SMS**: ~$0.0075-0.015 per SMS
- **Free Trial**: Usually $15 credit for new accounts

### Cost Calculation
- Sending to 3 contacts: ~$0.02 per SOS alert
- 10 SOS alerts/month: ~$0.20/month
- Typical monthly cost: Very low

**Note**: Contact Twilio for current pricing in your region

## Troubleshooting Configuration

### Issue: "Invalid Account SID"
```
Solution: Verify Account SID is copied correctly (ACxxxxxx...)
- Check no extra spaces
- Account SID should be 34 characters
- Starts with "AC"
```

### Issue: "Authentication failed"
```
Solution: Verify Auth Token is correct
- Copy from Twilio console again
- No spaces or special characters
- Should be alphanumeric string
```

### Issue: "Invalid phone number"
```
Solution: Use E.164 format
- Correct: +63917123456 (Philippines)
- Correct: +12025551234 (USA)
- Wrong: 0917123456 (missing +, country code)
- Wrong: 63917123456 (missing +)
```

### Issue: "No SMS credits"
```
Solution: Add billing method and purchase SMS credits
- Go to Twilio Console → Billing
- Add credit card or bank account
- Purchase SMS credits (minimum usually $5-10)
- Wait 15 minutes for account upgrade
```

### Issue: "SMS not delivered"
```
Solution: Check multiple factors
1. Verify phone number is correct (user can test call)
2. Check Twilio console → Logs for delivery status
3. Ensure emergency contact number is in E.164 format
4. Verify Twilio account is active (not suspended)
5. Check phone number permissions in Twilio
```

## Testing Scenario

### Test 1: Self-Send
```kotlin
// Test SMS to your own number first
sosViewModel.sendSOS(
    emergencyContacts = listOf("+63917123456"), // Your own number
    includeLocation = true
)
// Should receive SMS within 5 seconds
```

### Test 2: Multiple Contacts
```kotlin
sosViewModel.sendSOS(
    emergencyContacts = listOf(
        "+63917111111",  // Contact 1
        "+63917222222",  // Contact 2
        "+63917333333"   // Contact 3
    ),
    includeLocation = false  // Test without location first
)
// All three should receive within 10 seconds
```

### Test 3: With Location
```kotlin
sosViewModel.sendSOS(
    emergencyContacts = listOf("+63917123456"),
    includeLocation = true
)
// Message should include Google Maps URL
// Example: https://maps.google.com/?q=14.5995,120.9842
```

## Security Checklist

Before Going Live:

- [ ] Twilio credentials stored securely (not hardcoded)
- [ ] Location permission requested before sharing
- [ ] SMS rate limiting implemented (prevent spam)
- [ ] SOS activity logging enabled
- [ ] User consent obtained for emergency alerts
- [ ] Tested with actual emergency contacts
- [ ] Verified compliance with local regulations
- [ ] Backup contact methods in place
- [ ] Account monitoring enabled
- [ ] Incident response plan documented

## Support Contacts

**Twilio Support**: https://www.twilio.com/console/support
**Twilio Docs**: https://www.twilio.com/docs

**JuanWeather Issues**: Check GitHub or project documentation

## Philippines-Specific Setup

### Local SMS Rates
- Globe/TM: Generally supported
- Smart: Generally supported
- Sun Cellular: Generally supported

### Twilio Numbers for Philippines
1. Go to Phone Numbers in Twilio Console
2. Select Country: Philippines
3. Available numbers shown (+63 prefix)
4. Select and purchase

### Example Configuration for Philippines
```kotlin
sosViewModel.setTwilioCredentials(
    accountSid = "AC..." ,  // Your Account SID
    authToken = "...",      // Your Auth Token
    phoneNumber = "+63917123456"  // Philippine Twilio number
)
```

### Testing with Local Numbers
```kotlin
val testContacts = listOf(
    "+63917111111",  // Globe number
    "+63955222222",  // Smart number
    "+63905333333"   // Sun Cellular number
)

sosViewModel.sendSOS(testContacts, includeLocation = true)
```

---

That's everything you need to configure and test the SOS functionality!
