package com.juanweather.data.remote

import com.juanweather.data.models.WeatherApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    // Current + forecast in one call (days=1 gives hourly for today, days=5 gives 5-day forecast)
    @GET("forecast.json")
    suspend fun getForecast(
        @Query("key")  apiKey: String,
        @Query("q")    query: String,       // "lat,lon" or city name
        @Query("days") days: Int = 5,
        @Query("aqi")  aqi: String = "no",
        @Query("alerts") alerts: String = "no"
    ): WeatherApiResponse

    // City search for autocomplete
    @GET("search.json")
    suspend fun searchCity(
        @Query("key") apiKey: String,
        @Query("q")   query: String
    ): List<com.juanweather.data.models.WeatherLocation>
}
