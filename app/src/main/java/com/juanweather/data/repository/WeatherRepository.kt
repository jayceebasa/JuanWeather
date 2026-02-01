package com.juanweather.data.repository

import com.juanweather.data.models.WeatherData
import com.juanweather.data.models.WeatherResponse
import com.juanweather.data.remote.WeatherApiService

class WeatherRepository(private val apiService: WeatherApiService) {

    suspend fun getWeather(latitude: Double, longitude: Double): WeatherResponse {
        return try {
            // Call your weather API
            val response = apiService.getCurrentWeather(latitude, longitude)
            response
        } catch (e: Exception) {
            throw Exception("Failed to fetch weather: ${e.message}")
        }
    }

    suspend fun getForecast(latitude: Double, longitude: Double, days: Int = 5): List<WeatherData> {
        return try {
            val response = apiService.getForecast(latitude, longitude, count = days * 8)
            response.forecast ?: emptyList()
        } catch (e: Exception) {
            throw Exception("Failed to fetch forecast: ${e.message}")
        }
    }
}
