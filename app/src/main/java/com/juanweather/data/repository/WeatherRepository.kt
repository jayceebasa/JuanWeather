package com.juanweather.data.repository

import com.juanweather.data.models.WeatherApiResponse
import com.juanweather.data.remote.WeatherApiService
import com.juanweather.ui.models.DailyForecastItem
import com.juanweather.ui.models.HourlyForecastItem
import com.juanweather.ui.models.Metric
import com.juanweather.utils.Constants
import java.util.Calendar
import java.util.Locale

class WeatherRepository(private val apiService: WeatherApiService) {

    // Fetch full forecast (current + hourly + 5-day) for a lat/lon
    suspend fun getWeatherForLocation(lat: Double, lon: Double): WeatherApiResponse {
        return apiService.getForecast(
            apiKey = Constants.WEATHER_API_KEY,
            query  = "$lat,$lon",
            days   = 5
        )
    }

    // Fetch by city name (for search)
    suspend fun getWeatherForCity(city: String): WeatherApiResponse {
        return apiService.getForecast(
            apiKey = Constants.WEATHER_API_KEY,
            query  = city,
            days   = 5
        )
    }

    // Map weatherapi.com condition text → your existing icon types ("sun","cloud","rain","drizzle")
    fun mapConditionToIcon(conditionCode: Int): String {
        return when (conditionCode) {
            1000 -> "sun"                          // Sunny / Clear
            1003, 1006 -> "cloud"                  // Partly/Mostly cloudy
            1009 -> "cloud"                        // Overcast
            1063, 1180, 1183, 1186, 1189,
            1192, 1195, 1198, 1201 -> "rain"       // Rain variants
            1150, 1153, 1168, 1171 -> "drizzle"    // Drizzle variants
            1066, 1069, 1072, 1114, 1117,
            1210, 1213, 1216, 1219, 1222,
            1225, 1237, 1240, 1243, 1246,
            1249, 1252, 1255, 1258, 1261,
            1264 -> "rain"                         // Snow/sleet → rain icon
            1087, 1273, 1276, 1279, 1282 -> "rain" // Thunder
            else -> "cloud"
        }
    }

    // Map today's hourly forecast to your HourlyForecastItem list
    fun mapHourlyForecast(response: WeatherApiResponse): List<HourlyForecastItem> {
        val hours = response.forecast?.forecastDay?.firstOrNull()?.hour ?: return emptyList()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return hours
            .filter { hourWeather ->
                val hourInt = hourWeather.time.split(" ").last().split(":").first().toIntOrNull() ?: 0
                hourInt >= currentHour
            }
            .take(7)
            .mapIndexed { index, hourWeather ->
                val hourInt = hourWeather.time.split(" ").last().split(":").first().toIntOrNull() ?: 0
                val label = if (index == 0) "NOW" else {
                    val suffix = if (hourInt < 12) "AM" else "PM"
                    val display = if (hourInt == 0) 12 else if (hourInt > 12) hourInt - 12 else hourInt
                    "${display}${suffix}"
                }
                HourlyForecastItem(
                    time = label,
                    iconType = mapConditionToIcon(hourWeather.condition.code),
                    temperature = "${hourWeather.tempC.toInt()}°"
                )
            }
    }

    // Map 5-day forecast to your DailyForecastItem list
    fun mapDailyForecast(response: WeatherApiResponse): List<DailyForecastItem> {
        val days = response.forecast?.forecastDay ?: return emptyList()
        val dayNames = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        return days.mapIndexed { index, forecastDay ->
            val label = if (index == 0) "TODAY" else {
                try {
                    // Parse "yyyy-MM-dd" manually to stay API 21 compatible
                    val parts = forecastDay.date.split("-")
                    val cal = Calendar.getInstance()
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                } catch (e: Exception) {
                    forecastDay.date
                }
            }
            DailyForecastItem(
                day = label,
                iconType = mapConditionToIcon(forecastDay.day.condition.code)
            )
        }
    }

    // Map current weather to Metric list for your metrics card
    fun mapMetrics(response: WeatherApiResponse): List<Metric> {
        val current = response.current
        return listOf(
            Metric("HUMIDITY",   "${current.humidity}%"),
            Metric("REAL FEEL",  "${current.feelsLikeC.toInt()}°C"),
            Metric("UV",         "${current.uv.toInt()}"),
            Metric("PRESSURE",   "${current.pressureMb.toInt()}mbar")
        )
    }
}
