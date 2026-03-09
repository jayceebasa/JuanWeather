package com.juanweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.local.UserLocationDao
import com.juanweather.data.models.UserLocation
import com.juanweather.data.repository.WeatherRepository
import com.juanweather.ui.screens.LocationWeather
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationDao: UserLocationDao,
    private val weatherRepository: WeatherRepository
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
    fun loadLocationsForUser(userId: Int) {
        currentUserId = userId
        viewModelScope.launch {
            locationDao.getLocationsForUser(userId).collect { locations ->
                fetchWeatherForLocations(locations)
            }
        }
    }

    private suspend fun fetchWeatherForLocations(locations: List<UserLocation>) {
        _isLoading.value = true
        val cards = locations.mapNotNull { loc ->
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
                locationDao.insertLocation(
                    UserLocation(userId = currentUserId, cityName = cityName.trim())
                )
                _addResult.value = AddResult.Success
            } catch (e: Exception) {
                _addResult.value = AddResult.Error("City not found. Please check the name.")
            }
        }
    }

    // Delete a location from Room
    fun deleteLocation(locationId: Int) {
        viewModelScope.launch {
            locationDao.deleteLocationById(locationId)
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

            // Add the current homepage city to the saved list (skip if duplicate)
            val existing = locationDao.findLocation(currentUserId, currentHomeCity.trim())
            if (existing == null && currentHomeCity.isNotBlank()) {
                locationDao.insertLocation(
                    UserLocation(userId = currentUserId, cityName = currentHomeCity.trim())
                )
            }

            // Switch the homepage to the selected city
            onSwitchCity(selectedLocation.city)
        }
    }

    fun resetAddResult() {
        _addResult.value = AddResult.Idle
    }

    sealed class AddResult {
        object Idle    : AddResult()
        object Loading : AddResult()
        object Success : AddResult()
        data class Error(val message: String) : AddResult()
    }
}
