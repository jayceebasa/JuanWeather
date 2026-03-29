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
    private var currentFirebaseUid: String? = null

    // Load locations from Room and fetch weather for each
    fun loadLocationsForUser(userId: Int, firebaseUid: String? = null, onFirstLocationLoaded: ((String) -> Unit)? = null) {
        if (userId <= 0) {
            android.util.Log.e("LocationViewModel", "Invalid userId: $userId")
            _errorMessage.value = "Invalid user ID"
            return
        }

        currentUserId = userId
        currentFirebaseUid = firebaseUid
        android.util.Log.d("LocationViewModel", "Loading locations for user $userId (Firebase: $firebaseUid)")

        // Clear previous locations before loading new user's locations
        _locationCards.value = emptyList()
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // First, migrate old locations if they exist (for backward compatibility)
                migrateOldLocationsIfNeeded(userId, firebaseUid)

                // Then sync Firestore locations to Room (in case they were added on other device)
                syncFirestoreLocationsToRoom(userId, firebaseUid)

                // Then load from Room and display
                locationDao.getLocationsForUser(userId).collect { locations ->
                    android.util.Log.d("LocationViewModel", "Loaded ${locations.size} locations for user $userId")
                    fetchWeatherForLocations(locations)

                    // Auto-load the first location on the homepage
                    if (locations.isNotEmpty() && onFirstLocationLoaded != null) {
                        onFirstLocationLoaded(locations.first().cityName)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Error loading locations: ${e.message}", e)
                _errorMessage.value = "Failed to load locations: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Sync Firestore locations to Room database
     * Ensures locations saved in Firestore are available offline
     * Firestore is the source of truth - we sync from it to Room on login
     * Also deletes from Room any locations that no longer exist in Firestore
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

                    // Get current Room locations for this user
                    val roomLocations = locationDao.getLocationsForUserSync(userId)

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

                    // Delete from Room any locations that are no longer in Firestore
                    val firestoreCityNames = firestoreLocations.map { it.cityName }.toSet()
                    for (roomLocation in roomLocations) {
                        if (roomLocation.cityName !in firestoreCityNames) {
                            locationDao.deleteLocationById(roomLocation.id)
                            android.util.Log.d("LocationViewModel", "Deleted stale location from Room: ${roomLocation.cityName}")
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

    /**
     * Migrate old locations from previous userId to new userId
     * This fixes accounts created before the cross-device sync update
     */
    private suspend fun migrateOldLocationsIfNeeded(userId: Int, firebaseUid: String?) {
        if (firebaseUid.isNullOrBlank()) return

        try {
            // Get all locations from Firestore for this user
            val firestoreLocations = firestoreRepository.getAllLocationsByFirebaseUid(firebaseUid)

            if (firestoreLocations.isNotEmpty()) {
                android.util.Log.d("LocationViewModel", "Found ${firestoreLocations.size} locations in Firestore, updating to userId=$userId")

                // Update each location to use the new userId
                for (location in firestoreLocations) {
                    if (location.userId != userId) {
                        val updatedLocation = location.copy(userId = userId)

                        // Save updated location to Room
                        locationDao.insertLocation(updatedLocation)
                        android.util.Log.d("LocationViewModel", "Migrated location: ${location.cityName} from userId=${location.userId} to userId=$userId in Room")

                        // Update in Firestore
                        firestoreRepository.addLocation(updatedLocation)
                        android.util.Log.d("LocationViewModel", "Updated location in Firestore: ${location.cityName}")
                    } else {
                        // Location already has correct userId, just ensure it's in Room
                        locationDao.insertLocation(location)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationViewModel", "Error during migration: ${e.message}", e)
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
                    locationId = loc.id,
                    cityName = loc.cityName  // Store the original city name for Firestore deletion
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
                    locationId = loc.id,
                    cityName = loc.cityName
                )
            }
        }
        _locationCards.value = cards
        _isLoading.value = false
    }

    // Add a new city — checks for duplicate first (case-insensitive)
    fun addLocation(cityName: String) {
        if (cityName.isBlank()) {
            _addResult.value = AddResult.Error("Please enter a city name")
            return
        }

        // Ensure userId is set - this should always be set after loadLocationsForUser
        if (currentUserId <= 0) {
            android.util.Log.e("LocationViewModel", "Cannot add location: currentUserId=$currentUserId, firebaseUid=$currentFirebaseUid")
            _addResult.value = AddResult.Error("User not enabled. Please log in again.")
            return
        }

        viewModelScope.launch {
            _addResult.value = AddResult.Loading
            val trimmedCity = cityName.trim()

            android.util.Log.d("LocationViewModel", "Adding location '$trimmedCity' for user $currentUserId (Firebase: $currentFirebaseUid)")

            // Check duplicate in Room (case-insensitive)
            val existing = locationDao.findLocation(currentUserId, trimmedCity)
            if (existing != null) {
                android.util.Log.d("LocationViewModel", "Location already exists in Room: $trimmedCity")
                _addResult.value = AddResult.Error("${trimmedCity} is already in your list")
                return@launch
            }

            // Also check current locationCards for duplicates (handles real-time cases)
            if (_locationCards.value.any { it.cityName.equals(trimmedCity, ignoreCase = true) || it.city.equals(trimmedCity, ignoreCase = true) }) {
                android.util.Log.d("LocationViewModel", "Location already exists in UI: $trimmedCity")
                _addResult.value = AddResult.Error("${trimmedCity} is already in your list")
                return@launch
            }

            // Verify city exists via API before saving
            try {
                weatherRepository.getWeatherForCity(trimmedCity)
                val newLocation = UserLocation(userId = currentUserId, cityName = trimmedCity)

                android.util.Log.d("LocationViewModel", "Creating location: $newLocation")

                // Save to Room (offline storage)
                locationDao.insertLocation(newLocation)
                android.util.Log.d("LocationViewModel", "Saved to Room successfully")

                // Save to Firestore (cloud sync)
                firestoreRepository.addLocation(newLocation)
                android.util.Log.d("LocationViewModel", "Saved to Firestore successfully")

                _addResult.value = AddResult.Success
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Error adding location: ${e.message}", e)
                _addResult.value = AddResult.Error("City not found. Please check the name.")
            }
        }
    }

    // Delete a location from Room and Firestore
    fun deleteLocation(locationId: Int, cityName: String = "") {
        viewModelScope.launch {
            try {
                // First try to get the actual city name from Room if not provided
                val cityToDelete = if (cityName.isNotBlank()) {
                    cityName
                } else {
                    // Fetch from Room if we need it
                    val location = locationDao.getLocationById(locationId)
                    location?.cityName ?: ""
                }

                // Delete from Firestore first (cloud) using cityName as query key
                if (cityToDelete.isNotBlank()) {
                    firestoreRepository.deleteLocation(cityToDelete)
                }

                // Then delete from Room (offline)
                locationDao.deleteLocationById(locationId)

                android.util.Log.d("LocationViewModel", "Deleted location: $cityToDelete (ID: $locationId)")
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Error deleting location: ${e.message}", e)
            }
        }
    }

    /**
     * Set a location as the current homepage/dashboard city.
     *
     * - The selected location becomes the new dashboard city
     * - The selected location is removed from the saved locations list (it's now the main one)
     * - If currentHomeCity is different, it's added to saved locations (if not already there)
     * - [onSwitchCity] is called to update the WeatherViewModel
     */
    fun setLocationAsDashboard(
        selectedLocation: com.juanweather.ui.screens.LocationWeather,
        currentHomeCity: String,
        onSwitchCity: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Use the original cityName (not API-normalized) for deletion
                val cityToDelete = selectedLocation.cityName.ifBlank { selectedLocation.city }

                // Remove the selected location from the saved list (it will become the dashboard city)
                locationDao.deleteLocationById(selectedLocation.locationId)
                firestoreRepository.deleteLocation(cityToDelete)

                // Add the current homepage city to the saved list (skip if duplicate or blank)
                // Note: currentHomeCity is the API-normalized name from the previous dashboard
                if (currentHomeCity.isNotBlank() && currentHomeCity.trim() != cityToDelete.trim()) {
                    val existing = locationDao.findLocation(currentUserId, currentHomeCity.trim())
                    if (existing == null) {
                        val newLocation = UserLocation(userId = currentUserId, cityName = currentHomeCity.trim())
                        locationDao.insertLocation(newLocation)
                        firestoreRepository.addLocation(newLocation)
                        android.util.Log.d("LocationViewModel", "Added previous dashboard city to saved: $currentHomeCity")
                    }
                }

                // Switch the dashboard to the selected city (use the API-normalized name)
                onSwitchCity(selectedLocation.city)
                android.util.Log.d("LocationViewModel", "Set dashboard to: ${selectedLocation.city}")
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "Error setting dashboard location: ${e.message}", e)
            }
        }
    }

    /**
     * Swap a saved location with the current homepage city.
     * Delegates to [setLocationAsDashboard] for backward compatibility.
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
        setLocationAsDashboard(selectedLocation, currentHomeCity, onSwitchCity)
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
