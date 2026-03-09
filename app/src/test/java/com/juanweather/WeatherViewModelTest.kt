package com.juanweather

import app.cash.turbine.test
import com.juanweather.data.models.CurrentWeather
import com.juanweather.data.models.DaySummary
import com.juanweather.data.models.ForecastContainer
import com.juanweather.data.models.ForecastDay
import com.juanweather.data.models.WeatherApiResponse
import com.juanweather.data.models.WeatherCondition
import com.juanweather.data.models.WeatherLocation
import com.juanweather.data.repository.WeatherRepository
import com.juanweather.viewmodel.WeatherViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for WeatherViewModel — covers Step 3 of Option 1:
 *   - Error handling (no internet, timeout, generic error)
 *   - CRUD READ operations (fetch by city, fetch by coordinates)
 *   - isLoading state transitions
 *   - currentCity tracking
 *   - clearError() behaviour
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: WeatherRepository
    private lateinit var viewModel: WeatherViewModel

    // ── Test data helper ─────────────────────────────────────────────────────

    private fun fakeCondition(text: String = "Sunny", code: Int = 1000) =
        WeatherCondition(text = text, icon = "", code = code)

    private fun fakeResponse(
        cityName: String      = "Imus",
        tempC: Float          = 28f,
        conditionText: String = "Sunny",
        conditionCode: Int    = 1000,
        maxTempC: Float       = 32f,
        minTempC: Float       = 24f,
        chanceOfRain: Int     = 10
    ): WeatherApiResponse = WeatherApiResponse(
        location = WeatherLocation(
            name      = cityName,
            region    = "Cavite",
            country   = "Philippines",
            lat       = 14.42,
            lon       = 120.93,
            tzId      = "Asia/Manila",
            localtime = "2026-03-09 10:00"
        ),
        current = CurrentWeather(
            isDay      = 1,
            tempC      = tempC,
            tempF      = tempC * 9f / 5f + 32f,
            feelsLikeC = tempC - 1f,
            humidity   = 80,
            windKph    = 10f,
            windDegree = 180,
            pressureMb = 1010f,
            visKm      = 10f,
            uv         = 0f,
            cloud      = 20,
            condition  = fakeCondition(conditionText, conditionCode)
        ),
        forecast = ForecastContainer(
            forecastDay = listOf(
                ForecastDay(
                    date = "2026-03-09",
                    day  = DaySummary(
                        maxTempC     = maxTempC,
                        minTempC     = minTempC,
                        chanceOfRain = chanceOfRain,
                        condition    = fakeCondition(conditionText, conditionCode)
                    ),
                    hour = emptyList()
                )
            )
        )
    )

    // ── Setup / Teardown ─────────────────────────────────────────────────────

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel  = WeatherViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── READ Tests ───────────────────────────────────────────────────────────

    /**
     * TC-W-01 | READ — Fetch weather by city name (success)
     * Verifies temperature, condition, highLow, and locationName
     * are correctly mapped from the API response.
     */
    @Test
    fun `TC-W-01 fetchWeatherByCity success updates all weather states correctly`() = runTest {
        val response = fakeResponse(
            cityName = "Imus", tempC = 28f,
            conditionText = "Sunny", maxTempC = 32f,
            minTempC = 24f, chanceOfRain = 10
        )
        coEvery { repository.getWeatherForCity("Imus, Cavite") } returns response
        coEvery { repository.mapHourlyForecast(response) } returns emptyList()
        coEvery { repository.mapDailyForecast(response) } returns emptyList()
        coEvery { repository.mapMetrics(response) } returns emptyList()

        viewModel.fetchWeatherByCity("Imus, Cavite")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Imus",        viewModel.locationName.value)
        assertEquals("28°C",        viewModel.temperature.value)
        assertEquals("Sunny",       viewModel.condition.value)
        assertEquals("H:32° L:24°", viewModel.highLow.value)
        assertEquals("10%",         viewModel.chanceOfRain.value)
        assertNull(viewModel.errorMessage.value)
    }

    /**
     * TC-W-02 | READ — fetchWeatherByCity updates currentCity
     * Ensures currentCity always reflects the last-requested city string.
     */
    @Test
    fun `TC-W-02 fetchWeatherByCity updates currentCity before fetch`() = runTest {
        val response = fakeResponse()
        coEvery { repository.getWeatherForCity("Manila") } returns response
        coEvery { repository.mapHourlyForecast(any()) } returns emptyList()
        coEvery { repository.mapDailyForecast(any()) } returns emptyList()
        coEvery { repository.mapMetrics(any()) } returns emptyList()

        viewModel.fetchWeatherByCity("Manila")
        // currentCity is set synchronously before the coroutine suspends
        assertEquals("Manila", viewModel.currentCity.value)
    }

    /**
     * TC-W-03 | READ — Fetch weather by GPS coordinates (success)
     * Verifies fetchWeatherByLocation calls the repository with the exact lat/lon.
     */
    @Test
    fun `TC-W-03 fetchWeatherByLocation calls repository with correct coordinates`() = runTest {
        val lat = 14.4296; val lon = 120.9367
        val response = fakeResponse(cityName = "Imus")
        coEvery { repository.getWeatherForLocation(lat, lon) } returns response
        coEvery { repository.mapHourlyForecast(response) } returns emptyList()
        coEvery { repository.mapDailyForecast(response) } returns emptyList()
        coEvery { repository.mapMetrics(response) } returns emptyList()

        viewModel.fetchWeatherByLocation(lat, lon)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getWeatherForLocation(lat, lon) }
        assertEquals("Imus", viewModel.locationName.value)
        assertNull(viewModel.errorMessage.value)
    }

    // ── Error Handling Tests ─────────────────────────────────────────────────

    /**
     * TC-W-04 | ERROR — No internet connection (UnknownHostException)
     * When the device has no network the app must NOT crash; it must show an
     * error message and leave weather values at their initial "--" defaults.
     */
    @Test
    fun `TC-W-04 no internet connection sets errorMessage and keeps default values`() = runTest {
        coEvery { repository.getWeatherForCity(any()) } throws
            UnknownHostException("Unable to resolve host — no internet")

        viewModel.fetchWeatherByCity("Imus, Cavite")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Failed to load weather"))
        // Weather fields must remain at their "--" defaults
        assertEquals("--", viewModel.temperature.value)
        assertEquals("--", viewModel.condition.value)
        assertEquals("H:--° L:--°", viewModel.highLow.value)
    }

    /**
     * TC-W-05 | ERROR — Request timeout (SocketTimeoutException)
     * Simulates a slow or unreachable server; the app handles it gracefully.
     */
    @Test
    fun `TC-W-05 timeout exception sets errorMessage and clears isLoading`() = runTest {
        coEvery { repository.getWeatherForCity(any()) } throws
            SocketTimeoutException("connect timed out")

        viewModel.fetchWeatherByCity("Imus, Cavite")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Failed to load weather"))
        assertEquals(false, viewModel.isLoading.value)
    }

    /**
     * TC-W-06 | ERROR — Generic server / API error
     * Any unexpected exception (e.g. HTTP 403, 429) must be caught and
     * surfaced as a human-readable error message — never a crash.
     */
    @Test
    fun `TC-W-06 generic API error sets errorMessage`() = runTest {
        coEvery { repository.getWeatherForCity(any()) } throws
            RuntimeException("HTTP 403 Forbidden")

        viewModel.fetchWeatherByCity("Imus, Cavite")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.errorMessage.value!!.startsWith("Failed to load weather"))
    }

    /**
     * TC-W-07 | LOADING — isLoading transitions are correct
     * isLoading must go: false → true (fetch starts) → false (fetch ends).
     * Tested for the success path.
     */
    @Test
    fun `TC-W-07 isLoading transitions false-true-false on successful fetch`() = runTest {
        val response = fakeResponse()
        coEvery { repository.getWeatherForCity(any()) } returns response
        coEvery { repository.mapHourlyForecast(any()) } returns emptyList()
        coEvery { repository.mapDailyForecast(any()) } returns emptyList()
        coEvery { repository.mapMetrics(any()) } returns emptyList()

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())         // initial: not loading
            viewModel.fetchWeatherByCity("Imus, Cavite")
            assertEquals(true, awaitItem())          // fetch started → loading
            assertEquals(false, awaitItem())         // fetch done   → not loading
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * TC-W-07b | LOADING — isLoading is false after a network failure
     * The finally block in fetchWeather() must always clear isLoading,
     * even when an exception is thrown.
     */
    @Test
    fun `TC-W-07b isLoading is false after network failure`() = runTest {
        coEvery { repository.getWeatherForCity(any()) } throws UnknownHostException()

        viewModel.fetchWeatherByCity("Imus, Cavite")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
    }

    /**
     * TC-W-08 | ERROR — clearError() resets errorMessage to null
     * After an error is acknowledged, clearError() must null out the error state
     * so the UI can dismiss the error banner.
     */
    @Test
    fun `TC-W-08 clearError resets errorMessage to null`() = runTest {
        coEvery { repository.getWeatherForCity(any()) } throws RuntimeException("some failure")

        viewModel.fetchWeatherByCity("Imus, Cavite")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)  // error is present after failure
        viewModel.clearError()
        assertNull(viewModel.errorMessage.value)     // cleared after calling clearError()
    }
}
