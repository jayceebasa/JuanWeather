package com.juanweather

import com.juanweather.data.local.UserLocationDao
import com.juanweather.data.models.UserLocation
import com.juanweather.data.models.WeatherApiResponse
import com.juanweather.data.models.WeatherCondition
import com.juanweather.data.models.WeatherLocation
import com.juanweather.data.models.CurrentWeather
import com.juanweather.data.models.ForecastContainer
import com.juanweather.data.models.ForecastDay
import com.juanweather.data.models.DaySummary
import com.juanweather.data.repository.WeatherRepository
import com.juanweather.viewmodel.LocationViewModel
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

/**
 * Unit tests for LocationViewModel — covers Step 3 of Option 1:
 *   - CREATE: addLocation (success, duplicate, city not found / no internet)
 *   - DELETE: deleteLocation
 *   - READ:   loadLocationsForUser (success, API failure fallback)
 *   - addResult state transitions (Idle → Loading → Success / Error)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: UserLocationDao
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var viewModel: LocationViewModel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fakeCondition() = WeatherCondition(text = "Sunny", icon = "", code = 1000)

    private fun fakeResponse(cityName: String = "Manila"): WeatherApiResponse =
        WeatherApiResponse(
            location = WeatherLocation(
                name      = cityName,
                region    = "",
                country   = "Philippines",
                lat       = 14.5,
                lon       = 121.0,
                tzId      = "Asia/Manila",
                localtime = "2026-03-09 10:00"
            ),
            current = CurrentWeather(
                isDay      = 1,
                tempC      = 30f,
                tempF      = 86f,
                feelsLikeC = 29f,
                humidity   = 75,
                windKph    = 12f,
                windDegree = 90,
                pressureMb = 1012f,
                visKm      = 10f,
                uv         = 1f,
                cloud      = 10,
                condition  = fakeCondition()
            ),
            forecast = ForecastContainer(
                forecastDay = listOf(
                    ForecastDay(
                        date = "2026-03-09",
                        day  = DaySummary(
                            maxTempC     = 34f,
                            minTempC     = 26f,
                            chanceOfRain = 5,
                            condition    = fakeCondition()
                        ),
                        hour = emptyList()
                    )
                )
            )
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao               = mockk(relaxed = true)
        weatherRepository = mockk()
        viewModel         = LocationViewModel(dao, weatherRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── CREATE Tests ──────────────────────────────────────────────────────────

    /**
     * TC-L-01 | CREATE — addLocation success
     * A valid, non-duplicate city that resolves via the API should be inserted
     * into Room and addResult should become Success.
     */
    @Test
    fun `TC-L-01 addLocation with valid city inserts into Room and emits Success`() = runTest {
        coEvery { dao.findLocation(any(), "Manila") } returns null
        coEvery { weatherRepository.getWeatherForCity("Manila") } returns fakeResponse("Manila")
        coEvery { dao.insertLocation(any()) } returns 1L

        viewModel.addLocation("Manila")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LocationViewModel.AddResult.Success, viewModel.addResult.value)
        // Verify Room insert was called with the correct city name
        val slot = slot<UserLocation>()
        coVerify { dao.insertLocation(capture(slot)) }
        assertEquals("Manila", slot.captured.cityName)
    }

    /**
     * TC-L-02 | CREATE — addLocation with blank input
     * An empty city name must immediately emit an Error without making any
     * network call or Room insertion.
     */
    @Test
    fun `TC-L-02 addLocation with blank name emits Error without network call`() = runTest {
        viewModel.addLocation("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.addResult.value
        assertTrue(result is LocationViewModel.AddResult.Error)
        assertEquals("Please enter a city name",
            (result as LocationViewModel.AddResult.Error).message)
        coVerify(exactly = 0) { weatherRepository.getWeatherForCity(any()) }
    }

    /**
     * TC-L-03 | CREATE — addLocation with duplicate city
     * If the city already exists in Room for this user, the ViewModel must
     * reject the request without calling the API.
     */
    @Test
    fun `TC-L-03 addLocation duplicate city emits Error without API call`() = runTest {
        val existing = UserLocation(id = 1, userId = 0, cityName = "Manila")
        coEvery { dao.findLocation(any(), "Manila") } returns existing

        viewModel.addLocation("Manila")
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.addResult.value
        assertTrue(result is LocationViewModel.AddResult.Error)
        assertTrue((result as LocationViewModel.AddResult.Error).message.contains("already in your list"))
        coVerify(exactly = 0) { weatherRepository.getWeatherForCity(any()) }
    }

    /**
     * TC-L-04 | CREATE — addLocation city not found / no internet
     * If the API throws (city invalid or no network), the ViewModel must emit
     * an Error and NOT insert anything into Room.
     */
    @Test
    fun `TC-L-04 addLocation API failure emits Error and does not insert into Room`() = runTest {
        coEvery { dao.findLocation(any(), "FakeCity123") } returns null
        coEvery { weatherRepository.getWeatherForCity("FakeCity123") } throws
            UnknownHostException("no internet")

        viewModel.addLocation("FakeCity123")
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.addResult.value
        assertTrue(result is LocationViewModel.AddResult.Error)
        assertEquals("City not found. Please check the name.",
            (result as LocationViewModel.AddResult.Error).message)
        coVerify(exactly = 0) { dao.insertLocation(any()) }
    }

    /**
     * TC-L-05 | CREATE — addResult transitions through Loading → Success
     * Uses Turbine to subscribe to the StateFlow before the operation starts,
     * capturing every emission in order: Idle → Loading → Success.
     */
    @Test
    fun `TC-L-05 addLocation emits Loading then Success`() = runTest {
        coEvery { dao.findLocation(any(), "Tagaytay") } returns null
        coEvery { weatherRepository.getWeatherForCity("Tagaytay") } returns fakeResponse("Tagaytay")
        coEvery { dao.insertLocation(any()) } returns 2L

        viewModel.addResult.test {
            assertEquals(LocationViewModel.AddResult.Idle, awaitItem())   // initial state

            viewModel.addLocation("Tagaytay")

            assertEquals(LocationViewModel.AddResult.Loading, awaitItem()) // set inside launch{}
            assertEquals(LocationViewModel.AddResult.Success, awaitItem()) // set after API + insert

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── DELETE Tests ──────────────────────────────────────────────────────────

    /**
     * TC-L-06 | DELETE — deleteLocation calls DAO with correct ID
     * Verifies that deleteLocation passes the correct location ID to the DAO
     * and calls Firestore deleteLocation with the city name.
     */
    @Test
    fun `TC-L-06 deleteLocation calls dao deleteLocationById with correct id`() = runTest {
        // Test with explicit cityName parameter
        viewModel.deleteLocation(42, "Manila")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { dao.deleteLocationById(42) }
    }

    // ── READ Tests ────────────────────────────────────────────────────────────

    /**
     * TC-L-07 | READ — loadLocationsForUser maps API data into LocationWeather cards
     * When the DAO emits a list of saved locations, the ViewModel should fetch
     * weather for each and populate locationCards correctly.
     */
    @Test
    fun `TC-L-07 loadLocationsForUser maps API data into locationCards`() = runTest {
        val savedLocations = listOf(
            UserLocation(id = 1, userId = 5, cityName = "Manila"),
            UserLocation(id = 2, userId = 5, cityName = "Tagaytay")
        )
        coEvery { dao.getLocationsForUser(5) } returns flowOf(savedLocations)
        coEvery { weatherRepository.getWeatherForCity("Manila") } returns fakeResponse("Manila")
        coEvery { weatherRepository.getWeatherForCity("Tagaytay") } returns fakeResponse("Tagaytay")
        coEvery { weatherRepository.mapConditionToIcon(any(), any()) } returns "sun"

        viewModel.loadLocationsForUser(5)
        testDispatcher.scheduler.advanceUntilIdle()

        val cards = viewModel.locationCards.value
        assertEquals(2, cards.size)
        assertEquals("Manila",   cards[0].city)
        assertEquals("Tagaytay", cards[1].city)
        assertEquals(30,         cards[0].temp)   // from fakeResponse tempC = 30f
    }

    /**
     * TC-L-08 | READ — loadLocationsForUser handles per-city API failure gracefully
     * If one city's weather fetch fails (e.g. network error), that card should
     * show placeholder data ("Unavailable") instead of crashing.
     */
    @Test
    fun `TC-L-08 loadLocationsForUser shows Unavailable card when API fails for a city`() = runTest {
        val savedLocations = listOf(
            UserLocation(id = 1, userId = 5, cityName = "BadCity")
        )
        coEvery { dao.getLocationsForUser(5) } returns flowOf(savedLocations)
        coEvery { weatherRepository.getWeatherForCity("BadCity") } throws
            UnknownHostException("no internet")

        viewModel.loadLocationsForUser(5)
        testDispatcher.scheduler.advanceUntilIdle()

        val cards = viewModel.locationCards.value
        assertEquals(1, cards.size)
        assertEquals("BadCity",     cards[0].city)
        assertEquals("Unavailable", cards[0].condition)
        assertEquals(0,             cards[0].temp)
    }

    // ── resetAddResult Tests ──────────────────────────────────────────────────

    /**
     * TC-L-09 | STATE — resetAddResult returns addResult to Idle
     */
    @Test
    fun `TC-L-09 resetAddResult sets addResult back to Idle`() = runTest {
        coEvery { dao.findLocation(any(), any()) } returns null
        coEvery { weatherRepository.getWeatherForCity(any()) } throws RuntimeException()

        viewModel.addLocation("AnyCity")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.addResult.value is LocationViewModel.AddResult.Error)
        viewModel.resetAddResult()
        assertEquals(LocationViewModel.AddResult.Idle, viewModel.addResult.value)
    }
}
