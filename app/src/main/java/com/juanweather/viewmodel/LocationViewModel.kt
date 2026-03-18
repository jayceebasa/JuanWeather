package com.juanweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.local.UserLocationDao
import com.juanweather.data.models.UserLocation
import com.juanweather.data.repository.WeatherRepository
import com.juanweather.data.repository.FirestoreUserLocationRepository
import com.juanweather.ui.screens.LocationWeather
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationDao: UserLocationDao,
    private val weatherRepository: WeatherRepository,
    private val firestoreRepository: FirestoreUserLocationRepository = FirestoreUserLocationRepository()
) : ViewModel() {

    private val _locationCards = MutableStateFlow<List<LocationWeather>>(emptyList())
    val locationCards: StateFlow<List<LocationWeather>> = _locationCards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _addResult = MutableStateFlow<AddResult>(AddResult.Idle)
    val addResult: StateFlow<AddResult> = _addResult.asStateFlow()

    private var currentUserId: Int = 0

    // Load locations from Room and fetch weather for each
    fun loadLocationsForUser(userId: Int, firebaseUid: String? = null, onFirstLocationLoaded: ((String) -> Unit)? = null) {
        if (userId <= 0) return

        currentUserId = userId
        // Clear previous locations before loading new user's locations
        _locationCards.value = emptyList()
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // First, sync Firestore locations to Room (in case they were added on other device)
                syncFirestoreLocationsToRoom(userId, firebaseUid)

                // Then load from Room and display
                locationDao.getLocationsForUser(userId).collect { locations ->
                    fetchWeatherForLocations(locations)

                    // Auto-load the first location on the homepage
                    if (locations.isNotEmpty() && onFirstLocationLoaded != null) {
                        onFirstLocationLoaded(locations.first().cityName)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load locations: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Sync Firestore locations to Room database
     * Ensures locations saved in Firestore are available offline
     * Firestore is the source of truth - we sync from it to Room on login
     */
    private suspend fun syncFirestoreLocationsToRoom(userId: Int, firebaseUid: String? = null) {
        try {
            // Use provided Firebase UID or fallback to current uid
            val uid = firebaseUid ?: com.juanweather.data.firebase.FirebaseAuthManager.currentUid.value

            if (!uid.isNullOrBlank()) {
                android.util.Log.d("LocationViewModel", "Syncing locations for user $userId from Firebase UID: $uid")

                // Fetch locations from Firestore using Firebase UID
                val firestoreLocations = firestoreRepository.getAllLocationsByFirebaseUid(uid)

                if (firestoreLocations.isNotEmpty()) {
                    android.util.Log.d("LocationViewModel", "Found ${firestoreLocations.size} locations in Firestore for user $userId")

                    // For each Firestore location, ensure it exists in Room with correct userId
                    for (firestoreLocation in firestoreLocations) {
                        // Check if location already exists in Room by cityName
                        val existing = locationDao.findLocation(userId, firestoreLocation.cityName)

                        if (existing == null) {
                            // Add to Room with correct userId, keeping other fields from Firestore
                            val roomLocation = firestoreLocation.copy(userId = userId)
                            locationDao.insertLocation(roomLocation)
                            android.util.Log.d("LocationViewModel", "Synced location: ${firestoreLocation.cityName} to Room")
                        } else {
                            android.util.Log.d("LocationViewModel", "Location ${firestoreLocation.cityName} already exists in Room")
                        }
                    }
                } else {
                    android.util.Log.d("LocationViewModel", "No locations found in Firestore for UID: $uid")
                }
            } else {
                android.util.Log.d("LocationViewModel", "No Firebase UID available for sync")
            }
        } catch (e: Exception) {
            // Firestore sync failed, but we can still load from Room
            android.util.Log.e("LocationViewModel", "Sync error: ${e.message}", e)
        }
    }

    private suspend fun fetchWeatherForLocations(locations: List<UserLocation>) {
        _isLoading.value = true
        // Filter out locations with blank cityName or userId <= 0
        val validLocations = locations.filter { it.cityName.isNotBlank() && it.userId > 0 }
        val cards = validLocations.mapNotNull { loc ->
            try {
                val response = weatherRepository.getWeatherForCity(loc.cityName)
                val today = response.forecast?.forecastDay?.firstOrNull()?.day
                LocationWeather(
                    id        = loc.id.toString(),
                    city      = response.location.name,
                    temp      = response.current.tempC.toInt(),
                    condition = response.current.condition.text,
                    highTemp  = today?.maxTempC?.toInt() ?: response.current.tempC.toInt(),
                    icon      = weatherRepository.mapConditionToIcon(
                        response.current.condition.code,
                        response.current.isDay
                    ),
                    locationId = loc.id
                )
            } catch (e: Exception) {
                // If API fails for a city, show it with placeholder data
                LocationWeather(
                    id        = loc.id.toString(),
                    city      = loc.cityName,
                    temp      = 0,
                    condition = "Unavailable",
                    highTemp  = 0,
                    icon      = "cloud",
                    locationId = loc.id
                )
            }
        }
        _locationCards.value = cards
        _isLoading.value = false
    }

    // Add a new city — checks for duplicate first
    fun addLocation(cityName: String) {
        if (cityName.isBlank()) {
            _addResult.value = AddResult.Error("Please enter a city name")
            return
        }
        // Ensure userId is set
        if (currentUserId <= 0) {
            _addResult.value = AddResult.Error("User not loaded. Please refresh.")
            return
        }
        viewModelScope.launch {
            _addResult.value = AddResult.Loading
            // Check duplicate
            val existing = locationDao.findLocation(currentUserId, cityName.trim())
            if (existing != null) {
                _addResult.value = AddResult.Error("${cityName.trim()} is already in your list")
                return@launch
            }
            // Verify city exists via API before saving
            try {
                weatherRepository.getWeatherForCity(cityName.trim())
                val newLocation = UserLocation(userId = currentUserId, cityName = cityName.trim())

                // Save to Room (offline storage)
                locationDao.insertLocation(newLocation)

                // Save to Firestore (cloud sync)
                firestoreRepository.addLocation(newLocation)

                _addResult.value = AddResult.Success
            } catch (e: Exception) {
                _addResult.value = AddResult.Error("City not found. Please check the name.")
            }
        }
    }

    // Delete a location from Room and Firestore
    fun deleteLocation(locationId: Int) {
        viewModelScope.launch {
            // Delete from Room (offline)
            locationDao.deleteLocationById(locationId)

            // Delete from Firestore (cloud)
            firestoreRepository.deleteLocation(locationId)
        }
    }

    /**
     * Swap a saved location with the current homepage city.
     *
     * - The selected location is removed from the saved list.
     * - The previous homepage city is saved into the list (if not already there).
     * - [onSwitchCity] is called with the selected city so the WeatherViewModel
     *   can fetch new weather data for the homepage.
     */
    fun swapWithHomeLocation(
        selectedLocation: com.juanweather.ui.screens.LocationWeather,
        currentHomeCity: String,
        onSwitchCity: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Remove the selected location from the saved list
            locationDao.deleteLocationById(selectedLocation.locationId)
            // Also remove from Firestore
            firestoreRepository.deleteLocation(selectedLocation.locationId)

            // Add the current homepage city to the saved list (skip if duplicate)
            val existing = locationDao.findLocation(currentUserId, currentHomeCity.trim())
            if (existing == null && currentHomeCity.isNotBlank()) {
                val newLocation = UserLocation(userId = currentUserId, cityName = currentHomeCity.trim())
                locationDao.insertLocation(newLocation)
                // Also add to Firestore
                firestoreRepository.addLocation(newLocation)
            }

            // Switch the homepage to the selected city
            onSwitchCity(selectedLocation.city)
        }
    }

    fun resetAddResult() {
        _addResult.value = AddResult.Idle
    }

    /**
     * Clear all location data when user logs out
     * Only clears UI state - keeps data in Room database for next login
     */
    fun clearData() {
        _locationCards.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
        _addResult.value = AddResult.Idle
        currentUserId = 0
    }

    sealed class AddResult {
        object Idle    : AddResult()
        object Loading : AddResult()
        object Success : AddResult()
        data class Error(val message: String) : AddResult()
    }
}
