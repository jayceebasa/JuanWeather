package com.juanweather.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.models.SOSSettings
import com.juanweather.data.repository.HybridSOSRepository
import com.juanweather.utils.FmcSmsConfig
import com.juanweather.utils.FmcSmsService
import com.juanweather.utils.LocationManager
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class SOSViewModel(
    private val repository: HybridSOSRepository,
    private val smsService: FmcSmsService,
    private val fmcSmsConfig: FmcSmsConfig,
    private val locationManager: LocationManager? = null
) : ViewModel() {

    private val _settings = MutableStateFlow<SOSSettings?>(null)
    val settings: StateFlow<SOSSettings?> = _settings.asStateFlow()

    private val _toggleLocation = MutableStateFlow(false)
    val toggleLocation: StateFlow<Boolean> = _toggleLocation.asStateFlow()

    private val _messageTemplate = MutableStateFlow("I need help. This is an emergency SOS alert from JuanWeather.")
    val messageTemplate: StateFlow<String> = _messageTemplate.asStateFlow()

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

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

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
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

                // Check if Semaphore is configured
                if (!fmcSmsConfig.isConfigured()) {
                    Log.e("SOSViewModel", "FMCSMS Configuration Check Failed!")
                    Log.e("SOSViewModel", "API Key: ${fmcSmsConfig.getApiKey().ifEmpty { "EMPTY" }}")
                    Log.e("SOSViewModel", "Base URL: ${fmcSmsConfig.getBaseUrl().ifEmpty { "EMPTY" }}")
                    Log.e("SOSViewModel", "From Number: ${fmcSmsConfig.getFromNumber().ifEmpty { "EMPTY" }}")
                    _errorMessage.value = "FMCSMS is not configured. Please contact administrator."
                    return@launch
                }

                // Send SOS to all emergency contacts
                val results = smsService.sendSOSToMultipleContacts(
                    emergencyContacts,
                    _messageTemplate.value,
                    locationUrl,
                    _userName.value
                )

                _sosResults.value = results

                // Update last sent time
                repository.updateLastSentTime()

                // Check if any messages were sent successfully
                val successCount = results.values.count { it }
                if (successCount > 0) {
                    _successMessage.value = "SOS alert sent to $successCount contact(s)"
                } else {
                    _errorMessage.value = "Failed to send SOS alerts to any contacts. Please check your FMCSMS configuration."
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
     * Timeout: 10 seconds to prevent hanging
     */
    private suspend fun getCurrentLocationUrl(): String? {
        return try {
            if (locationManager == null) {
                return null
            }

            withTimeoutOrNull(10000L) {
                suspendCancellableCoroutine { continuation ->
                    locationManager.getCurrentLocation(
                        onSuccess = { lat, lon ->
                            // Format coordinates with proper precision (6 decimal places = ~0.1m accuracy)
                            val formattedLat = String.format("%.6f", lat)
                            val formattedLon = String.format("%.6f", lon)
                            val locationText = "Location: $formattedLat, $formattedLon"
                            Log.d("SOSViewModel", "Location obtained: $locationText")
                            continuation.resume(locationText)
                        },
                        onError = { error ->
                            // Continue without location if retrieval fails
                            Log.e("SOSViewModel", "Location retrieval failed: $error")
                            _errorMessage.value = "Could not get location: $error (sending SOS without location)"
                            continuation.resume(null)
                        }
                    )
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w("SOSViewModel", "Location timeout after 10 seconds - sending SOS without location")
            _errorMessage.value = "Location retrieval timeout - sending SOS without location"
            null
        } catch (e: Exception) {
            Log.e("SOSViewModel", "Location error: ${e.message}", e)
            _errorMessage.value = "Location error: ${e.message}"
            null
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
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
