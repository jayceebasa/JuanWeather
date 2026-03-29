package com.juanweather.data.models

import com.google.gson.annotations.SerializedName

// ── weatherapi.com response models ──────────────────────────────────────────

data class WeatherApiResponse(
    @SerializedName("location") val location: WeatherLocation = WeatherLocation(),
    @SerializedName("current")  val current: CurrentWeather = CurrentWeather(),
    @SerializedName("forecast") val forecast: ForecastContainer? = null
)

data class WeatherLocation(
    @SerializedName("name")      val name: String = "",
    @SerializedName("region")    val region: String = "",
    @SerializedName("country")   val country: String = "",
    @SerializedName("lat")       val lat: Double = 0.0,
    @SerializedName("lon")       val lon: Double = 0.0,
    @SerializedName("tz_id")     val tzId: String = "",
    @SerializedName("localtime") val localtime: String = ""
)

data class CurrentWeather(
    @SerializedName("is_day")       val isDay: Int = 0,
    @SerializedName("temp_c")       val tempC: Float = 0f,
    @SerializedName("temp_f")       val tempF: Float = 0f,
    @SerializedName("feelslike_c")  val feelsLikeC: Float = 0f,
    @SerializedName("humidity")     val humidity: Int = 0,
    @SerializedName("wind_kph")     val windKph: Float = 0f,
    @SerializedName("wind_degree")  val windDegree: Int = 0,
    @SerializedName("pressure_mb")  val pressureMb: Float = 0f,
    @SerializedName("vis_km")       val visKm: Float = 0f,
    @SerializedName("uv")           val uv: Float = 0f,
    @SerializedName("cloud")        val cloud: Int = 0,
    @SerializedName("condition")    val condition: WeatherCondition = WeatherCondition()
)

data class WeatherCondition(
    @SerializedName("text") val text: String = "",
    @SerializedName("icon") val icon: String = "",
    @SerializedName("code") val code: Int = 0
)

data class ForecastContainer(
    @SerializedName("forecastday") val forecastDay: List<ForecastDay> = emptyList()
)

data class ForecastDay(
    @SerializedName("date")  val date: String = "",
    @SerializedName("day")   val day: DaySummary = DaySummary(),
    @SerializedName("hour")  val hour: List<HourWeather> = emptyList()
)

data class DaySummary(
    @SerializedName("maxtemp_c")       val maxTempC: Float = 0f,
    @SerializedName("mintemp_c")       val minTempC: Float = 0f,
    @SerializedName("daily_chance_of_rain") val chanceOfRain: Int = 0,
    @SerializedName("condition")       val condition: WeatherCondition = WeatherCondition()
)

data class HourWeather(
    @SerializedName("time")           val time: String = "",
    @SerializedName("temp_c")         val tempC: Float = 0f,
    @SerializedName("is_day")         val isDay: Int = 0,
    @SerializedName("condition")      val condition: WeatherCondition = WeatherCondition(),
    @SerializedName("chance_of_rain") val chanceOfRain: Int = 0
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
