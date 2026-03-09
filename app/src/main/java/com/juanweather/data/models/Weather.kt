package com.juanweather.data.models

import com.google.gson.annotations.SerializedName

// ── weatherapi.com response models ──────────────────────────────────────────

data class WeatherApiResponse(
    @SerializedName("location") val location: WeatherLocation,
    @SerializedName("current")  val current: CurrentWeather,
    @SerializedName("forecast") val forecast: ForecastContainer?
)

data class WeatherLocation(
    @SerializedName("name")      val name: String,
    @SerializedName("region")    val region: String,
    @SerializedName("country")   val country: String,
    @SerializedName("lat")       val lat: Double,
    @SerializedName("lon")       val lon: Double,
    @SerializedName("tz_id")     val tzId: String,
    @SerializedName("localtime") val localtime: String
)

data class CurrentWeather(
    @SerializedName("temp_c")       val tempC: Float,
    @SerializedName("temp_f")       val tempF: Float,
    @SerializedName("feelslike_c")  val feelsLikeC: Float,
    @SerializedName("humidity")     val humidity: Int,
    @SerializedName("wind_kph")     val windKph: Float,
    @SerializedName("wind_degree")  val windDegree: Int,
    @SerializedName("pressure_mb")  val pressureMb: Float,
    @SerializedName("vis_km")       val visKm: Float,
    @SerializedName("uv")           val uv: Float,
    @SerializedName("cloud")        val cloud: Int,
    @SerializedName("condition")    val condition: WeatherCondition
)

data class WeatherCondition(
    @SerializedName("text") val text: String,
    @SerializedName("icon") val icon: String,
    @SerializedName("code") val code: Int
)

data class ForecastContainer(
    @SerializedName("forecastday") val forecastDay: List<ForecastDay>
)

data class ForecastDay(
    @SerializedName("date")  val date: String,
    @SerializedName("day")   val day: DaySummary,
    @SerializedName("hour")  val hour: List<HourWeather>
)

data class DaySummary(
    @SerializedName("maxtemp_c")       val maxTempC: Float,
    @SerializedName("mintemp_c")       val minTempC: Float,
    @SerializedName("daily_chance_of_rain") val chanceOfRain: Int,
    @SerializedName("condition")       val condition: WeatherCondition
)

data class HourWeather(
    @SerializedName("time")      val time: String,
    @SerializedName("temp_c")    val tempC: Float,
    @SerializedName("condition") val condition: WeatherCondition,
    @SerializedName("chance_of_rain") val chanceOfRain: Int
)

// ── Legacy models kept for WeatherRepository compatibility ───────────────────

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
