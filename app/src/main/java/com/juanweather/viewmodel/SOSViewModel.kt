package com.juanweather.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.models.SOSSettings
import com.juanweather.data.repository.HybridSOSRepository
import com.juanweather.utils.LocationManager
import com.juanweather.utils.TwilioConfig
import com.juanweather.utils.TwilioSmsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SOSViewModel(
    private val repository: HybridSOSRepository,
    private val smsService: TwilioSmsService,
    private val twilioConfig: TwilioConfig,
    private val locationManager: LocationManager? = null
) : ViewModel() {

    private val _settings = MutableStateFlow<SOSSettings?>(null)
    val settings: StateFlow<SOSSettings?> = _settings.asStateFlow()

    private val _toggleLocation = MutableStateFlow(true)
    val toggleLocation: StateFlow<Boolean> = _toggleLocation.asStateFlow()

    private val _messageTemplate = MutableStateFlow("I need help. This is an emergency SOS alert from JuanWeather.")
    val messageTemplate: StateFlow<String> = _messageTemplate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Track which contacts were successfully contacted
    private val _sosResults = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val sosResults: StateFlow<Map<String, Boolean>> = _sosResults.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val currentSettings = repository.getSettingsOnce() ?: SOSSettings()
                _settings.value = currentSettings
                _toggleLocation.value = currentSettings.enableLocationSharing
                _messageTemplate.value = currentSettings.messageTemplate
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load SOS settings: ${e.message}"
            }
        }
    }

    fun updateLocationSharing(enabled: Boolean) {
        // Update UI immediately
        _toggleLocation.value = enabled

        // Save to database asynchronously
        viewModelScope.launch {
            try {
                repository.updateLocationSharing(enabled)
                val updated = repository.getSettingsOnce()
                _settings.value = updated
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update location sharing: ${e.message}"
            }
        }
    }

    fun updateMessageTemplate(message: String) {
        // Update UI immediately
        _messageTemplate.value = message

        // Save to database asynchronously
        viewModelScope.launch {
            try {
                repository.updateMessageTemplate(message)
                val updated = repository.getSettingsOnce()
                _settings.value = updated
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save message template: ${e.message}"
            }
        }
    }

    /**
     * Send SOS alert to emergency contacts
     * @param emergencyContacts List of phone numbers to send to
     * @param includeLocation Whether to include current location
     */
    fun sendSOS(
        emergencyContacts: List<String>,
        includeLocation: Boolean = true
    ) {
        if (emergencyContacts.isEmpty()) {
            _errorMessage.value = "No emergency contacts configured. Please add emergency contacts first."
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                var locationUrl: String? = null

                // Get current location if enabled
                if (includeLocation && _toggleLocation.value) {
                    locationUrl = getCurrentLocationUrl()
                }

                // Get Messaging Service SID from TwilioConfig
                val messagingServiceSid = twilioConfig.getMessagingServiceSid()

                if (messagingServiceSid.isEmpty()) {
                    _errorMessage.value = "Twilio Messaging Service not configured. Please contact administrator."
                    return@launch
                }

                // Send SOS to all emergency contacts
                val results = smsService.sendSOSToMultipleContacts(
                    emergencyContacts,
                    _messageTemplate.value,
                    locationUrl,
                    messagingServiceSid
                )

                _sosResults.value = results

                // Update last sent time
                repository.updateLastSentTime()

                // Check if any messages were sent successfully
                val successCount = results.values.count { it }
                if (successCount > 0) {
                    _successMessage.value = "SOS alert sent to $successCount contact(s)"
                } else {
                    _errorMessage.value = "Failed to send SOS alerts to any contacts. Please check your Twilio configuration."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error sending SOS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get current location as a URL using suspendCancellableCoroutine
     * This properly waits for the location callback before returning
     */
    private suspend fun getCurrentLocationUrl(): String? {
        return try {
            if (locationManager == null) {
                return null
            }

            suspendCancellableCoroutine { continuation ->
                locationManager.getCurrentLocation(
                    onSuccess = { lat, lon ->
                        val url = "https://maps.google.com/?q=$lat,$lon"
                        continuation.resume(url)
                    },
                    onError = { error ->
                        // Continue without location if retrieval fails
                        _errorMessage.value = "Could not get location: $error (sending SOS without location)"
                        continuation.resume(null)
                    }
                )
            }
        } catch (e: Exception) {
            _errorMessage.value = "Location error: ${e.message}"
            null
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun setTwilioCredentials(accountSid: String, authToken: String, phoneNumber: String) {
        viewModelScope.launch {
            try {
                repository.setTwilioCredentials(accountSid, authToken, phoneNumber)
                val updated = repository.getSettingsOnce()
                _settings.value = updated
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set Twilio credentials: ${e.message}"
            }
        }
    }

    fun syncSOSSettingsOnLogin(firebaseUid: String? = null) {
        viewModelScope.launch {
            try {
                val uid = firebaseUid ?: return@launch
                // Sync from Firestore to Room
                repository.syncFirestoreSettingsToRoom(uid)
                // Then reload the synced settings
                loadSettings()
                Log.d("SOSViewModel", "SOS settings synced and reloaded on login")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to sync SOS settings on login: ${e.message}"
                Log.e("SOSViewModel", "Error syncing on login: ${e.message}", e)
                // Still load what we have in case of error
                loadSettings()
            }
        }
    }
}

