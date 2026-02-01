package com.juanweather.data.models

data class WeatherData(
    val temperature: Float,
    val feelsLike: Float,
    val minTemp: Float,
    val maxTemp: Float,
    val pressure: Int,
    val humidity: Int,
    val windSpeed: Float,
    val windDegree: Int,
    val cloudiness: Int,
    val uvIndex: Float,
    val visibility: Int,
    val condition: String,
    val description: String,
    val icon: String,
    val sunrise: Long,
    val sunset: Long,
    val timezone: String,
    val timestamp: Long
)

data class WeatherResponse(
    val current: WeatherData?,
    val forecast: List<WeatherData>?
)
