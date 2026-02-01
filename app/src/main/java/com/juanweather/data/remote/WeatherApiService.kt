package com.juanweather.data.remote

import com.juanweather.data.models.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = "YOUR_API_KEY",
        @Query("units") units: String = "metric"
    ): WeatherResponse

    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = "YOUR_API_KEY",
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 40
    ): WeatherResponse

    @GET("find")
    suspend fun searchCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String = "YOUR_API_KEY"
    ): WeatherResponse
}
