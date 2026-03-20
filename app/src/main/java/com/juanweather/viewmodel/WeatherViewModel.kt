package com.juanweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.models.WeatherApiResponse
import com.juanweather.data.repository.WeatherRepository
import com.juanweather.ui.models.DailyForecastItem
import com.juanweather.ui.models.HourlyForecastItem
import com.juanweather.ui.models.Metric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    // The raw city string currently displayed on the homepage
    private val _currentCity = MutableStateFlow("")
    val currentCity: StateFlow<String> = _currentCity.asStateFlow()

    // True once the user has explicitly set a home location
    private val _hasLocation = MutableStateFlow(false)
    val hasLocation: StateFlow<Boolean> = _hasLocation.asStateFlow()

    private val _locationName   = MutableStateFlow("--")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _temperature    = MutableStateFlow("--")
    val temperature: StateFlow<String> = _temperature.asStateFlow()

    private val _condition      = MutableStateFlow("--")
    val condition: StateFlow<String> = _condition.asStateFlow()

    private val _highLow        = MutableStateFlow("H:--° L:--°")
    val highLow: StateFlow<String> = _highLow.asStateFlow()

    private val _chanceOfRain   = MutableStateFlow("--")
    val chanceOfRain: StateFlow<String> = _chanceOfRain.asStateFlow()

    private val _hourlyForecast = MutableStateFlow<List<HourlyForecastItem>>(emptyList())
    val hourlyForecast: StateFlow<List<HourlyForecastItem>> = _hourlyForecast.asStateFlow()

    private val _dailyForecast  = MutableStateFlow<List<DailyForecastItem>>(emptyList())
    val dailyForecast: StateFlow<List<DailyForecastItem>> = _dailyForecast.asStateFlow()

    private val _metrics        = MutableStateFlow<List<Metric>>(emptyList())
    val metrics: StateFlow<List<Metric>> = _metrics.asStateFlow()

    private val _isLoading      = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage   = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Increment on each fetch to force LazyRow recomposition
    private val _fetchId        = MutableStateFlow(0)
    val fetchId: StateFlow<Int> = _fetchId.asStateFlow()

    // Called with device GPS coordinates
    fun fetchWeatherByLocation(lat: Double, lon: Double) {
        fetchWeather { repository.getWeatherForLocation(lat, lon) }
    }

    // Called with a city name string
    fun fetchWeatherByCity(city: String) {
        if (city.isBlank()) return
        _currentCity.value = city
        _hasLocation.value = true
        fetchWeather { repository.getWeatherForCity(city) }
    }

    private fun fetchWeather(apiCall: suspend () -> WeatherApiResponse) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            // Clear old forecast data before fetching new data
            _hourlyForecast.value = emptyList()
            _dailyForecast.value = emptyList()
            try {
                val response = apiCall()
                android.util.Log.d("WeatherViewModel", "Fetched data for: ${response.location.name}, Temp: ${response.current.tempC}°C")

                _locationName.value   = response.location.name
                _temperature.value    = "${response.current.tempC.toInt()}°C"
                _condition.value      = response.current.condition.text
                val today = response.forecast?.forecastDay?.firstOrNull()?.day
                _highLow.value        = "H:${today?.maxTempC?.toInt() ?: "--"}° L:${today?.minTempC?.toInt() ?: "--"}°"
                _chanceOfRain.value   = "${today?.chanceOfRain ?: "--"}%"

                val hourlyData = repository.mapHourlyForecast(response)
                android.util.Log.d("WeatherViewModel", "Hourly forecast items: ${hourlyData.size}, First temp: ${hourlyData.firstOrNull()?.temperature}")
                _hourlyForecast.value = hourlyData

                _dailyForecast.value  = repository.mapDailyForecast(response)
                _metrics.value        = repository.mapMetrics(response)

                // Increment fetchId to force LazyRow recomposition
                _fetchId.value = (_fetchId.value + 1) % Int.MAX_VALUE
            } catch (e: Exception) {
                android.util.Log.e("WeatherViewModel", "Error fetching weather: ${e.message}")
                _errorMessage.value = "Failed to load weather: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    /**
     * Clear all weather data when user logs out
     * Prevents showing previous user's weather data on new account
     */
    fun clearData() {
        _currentCity.value = ""
        _hasLocation.value = false
        _locationName.value = "--"
        _temperature.value = "--"
        _condition.value = "--"
        _highLow.value = "H:--° L:--°"
        _chanceOfRain.value = "--"
        _hourlyForecast.value = emptyList()
        _dailyForecast.value = emptyList()
        _metrics.value = emptyList()
        _isLoading.value = false
        _errorMessage.value = null
    }
}
