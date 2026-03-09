package com.juanweather.data.repository

import com.juanweather.data.models.WeatherApiResponse
import com.juanweather.data.remote.WeatherApiService
import com.juanweather.ui.models.DailyForecastItem
import com.juanweather.ui.models.HourlyForecastItem
import com.juanweather.ui.models.Metric
import com.juanweather.utils.Constants
import java.util.Calendar

class WeatherRepository(private val apiService: WeatherApiService) {

    // Fetch full forecast (current + hourly + 5-day) for a lat/lon
    suspend fun getWeatherForLocation(lat: Double, lon: Double): WeatherApiResponse {
        return apiService.getForecast(
            apiKey = Constants.WEATHER_API_KEY,
            query  = "$lat,$lon",
            days   = 6
        )
    }

    // Fetch by city name (for search)
    suspend fun getWeatherForCity(city: String): WeatherApiResponse {
        return apiService.getForecast(
            apiKey = Constants.WEATHER_API_KEY,
            query  = city,
            days   = 6
        )
    }

    // isDay: 1 = daytime, 0 = night — returns "night" for clear/partly cloudy at night
    fun mapConditionToIcon(conditionCode: Int, isDay: Int = 1): String {
        return when (conditionCode) {
            1000 -> if (isDay == 1) "sun" else "night"   // Clear sky
            1003 -> if (isDay == 1) "sun" else "night"   // Partly cloudy
            1006, 1009 -> "cloud"                         // Cloudy / Overcast
            1063, 1180, 1183, 1186, 1189,
            1192, 1195, 1198, 1201 -> "rain"
            1150, 1153, 1168, 1171 -> "drizzle"
            1066, 1069, 1072, 1114, 1117,
            1210, 1213, 1216, 1219, 1222,
            1225, 1237, 1240, 1243, 1246,
            1249, 1252, 1255, 1258, 1261,
            1264 -> "rain"
            1087, 1273, 1276, 1279, 1282 -> "rain"
            else -> "cloud"
        }
    }

    // All hours from current hour through tomorrow — each with correct day/night icon
    fun mapHourlyForecast(response: WeatherApiResponse): List<HourlyForecastItem> {
        val forecastDays = response.forecast?.forecastDay ?: return emptyList()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val allHours = forecastDays.take(2).flatMap { it.hour }

        var firstEntry = true
        return allHours
            .filter { hw ->
                val hourInt = hw.time.split(" ").last().split(":").first().toIntOrNull() ?: 0
                val isToday = hw.time.startsWith(forecastDays[0].date)
                (!isToday) || (hourInt >= currentHour)
            }
            .map { hw ->
                val hourInt = hw.time.split(" ").last().split(":").first().toIntOrNull() ?: 0
                val label = if (firstEntry) {
                    firstEntry = false
                    "NOW"
                } else {
                    val suffix = if (hourInt < 12) "AM" else "PM"
                    val display = when (hourInt) {
                        0         -> 12
                        in 13..23 -> hourInt - 12
                        else      -> hourInt
                    }
                    "${display}${suffix}"
                }
                HourlyForecastItem(
                    time        = label,
                    iconType    = mapConditionToIcon(hw.condition.code, hw.isDay),
                    temperature = "${hw.tempC.toInt()}°"
                )
            }
    }

    // 5-day forecast with real day names from API, always daytime icon for daily summary
    fun mapDailyForecast(response: WeatherApiResponse): List<DailyForecastItem> {
        val days = response.forecast?.forecastDay ?: return emptyList()
        val dayNames = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        return days.take(5).mapIndexed { index, forecastDay ->
            val label = if (index == 0) "TODAY" else {
                try {
                    val parts = forecastDay.date.split("-")
                    val cal = Calendar.getInstance()
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                } catch (e: Exception) {
                    forecastDay.date
                }
            }
            DailyForecastItem(
                day      = label,
                iconType = mapConditionToIcon(forecastDay.day.condition.code, isDay = 1)
            )
        }
    }

    // Map current weather to Metric list for your metrics card
    fun mapMetrics(response: WeatherApiResponse): List<Metric> {
        val current = response.current
        return listOf(
            Metric("HUMIDITY",  "${current.humidity}%"),
            Metric("REAL FEEL", "${current.feelsLikeC.toInt()}°C"),
            Metric("UV",        "${current.uv.toInt()}"),
            Metric("PRESSURE",  "${current.pressureMb.toInt()}mbar")
        )
    }
}
