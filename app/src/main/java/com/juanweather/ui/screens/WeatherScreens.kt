package com.juanweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.juanweather.R
import com.juanweather.ui.components.CloudIcon
import com.juanweather.ui.components.CloudRainIcon
import com.juanweather.ui.components.SettingsIcon
import com.juanweather.ui.components.SunIcon
import com.juanweather.ui.components.WeatherIcon
import com.juanweather.ui.models.DailyForecastItem
import com.juanweather.ui.models.HourlyForecastItem
import com.juanweather.ui.models.Metric
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas

/**
 * Main weather dashboard screen composable
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WeatherDashboardScreen(
    onNavigateToAddLocation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToUserManagement: () -> Unit = {},
    isAdmin: Boolean = false,
    weatherViewModel: com.juanweather.viewmodel.WeatherViewModel? = null,
    settingsViewModel: com.juanweather.viewmodel.SettingsViewModel? = null
) {
    val showSosPopup = remember { mutableStateOf(false) }

    // Collect settings for unit conversions
    val settingsState = settingsViewModel?.settings?.collectAsState()
    val settings = settingsState?.value

    // Collect real weather data from ViewModel
    val locationName  = weatherViewModel?.locationName?.collectAsState()?.value  ?: ""
    val temperature   = weatherViewModel?.temperature?.collectAsState()?.value   ?: "19°C"
    val condition     = weatherViewModel?.condition?.collectAsState()?.value     ?: "Mostly Clear"
    val highLow       = weatherViewModel?.highLow?.collectAsState()?.value       ?: "H:24° L:18°"
    val chanceOfRain  = weatherViewModel?.chanceOfRain?.collectAsState()?.value  ?: "91%"
    val isLoading     = weatherViewModel?.isLoading?.collectAsState()?.value     ?: false
    val errorMessage  = weatherViewModel?.errorMessage?.collectAsState()?.value
    val fetchId       = weatherViewModel?.fetchId?.collectAsState()?.value       ?: 0

    val hourlyForecast = weatherViewModel?.hourlyForecast?.collectAsState()?.value ?: listOf(
        HourlyForecastItem("NOW",  "sun",   "19°"),
        HourlyForecastItem("12PM", "sun",   "22°"),
        HourlyForecastItem("1PM",  "sun",   "23°"),
        HourlyForecastItem("2PM",  "sun",   "24°"),
        HourlyForecastItem("3PM",  "cloud", "23°"),
        HourlyForecastItem("4PM",  "cloud", "22°"),
        HourlyForecastItem("5PM",  "rain",  "20°")
    )

    val dailyForecast = weatherViewModel?.dailyForecast?.collectAsState()?.value ?: listOf(
        DailyForecastItem("TODAY", "sun"),
        DailyForecastItem("TUE",   "rain"),
        DailyForecastItem("WED",   "drizzle"),
        DailyForecastItem("THUR",  "cloud"),
        DailyForecastItem("FRI",   "cloud")
    )

    val rawMetrics = weatherViewModel?.metrics?.collectAsState()?.value ?: listOf(
        Metric("HUMIDITY",   "91%"),
        Metric("REAL FEEL",  "24°C"),
        Metric("UV",         "0"),
        Metric("PRESSURE",   "1008mbar")
    )

    // Convert metrics based on user settings
    val metrics = convertMetricsToUserUnits(rawMetrics, settings)

    val currentCity  = weatherViewModel?.currentCity?.collectAsState()?.value  ?: ""
    val hasLocation  = weatherViewModel?.hasLocation?.collectAsState()?.value  ?: false

    // Only fetch when a city has been set
    LaunchedEffect(currentCity) {
        if (currentCity.isNotBlank()) {
            weatherViewModel?.fetchWeatherByCity(currentCity)
        }
    }

    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh  = { weatherViewModel?.fetchWeatherByCity(currentCity) }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pullRefresh(pullRefreshState)
    ) {
        // Background image (volcano & rice fields)
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Weather background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header with SOS and Settings buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SOS Button
                Card(
                    modifier = Modifier
                        .clickable {
                            showSosPopup.value = true
                        }
                        .padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFBA1E1E).copy(alpha = 0.79f)
                    )
                ) {
                    Text(
                        text = "SOS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

                // Users button — visible to admins only (RBAC)
                if (isAdmin) {
                    Text(
                        text = "USERS",
                        color = Color(0xFF81C784),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigateToUserManagement() }
                            .padding(8.dp)
                    )
                }

                // Settings button
                Text(
                    text = "SETTINGS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigateToSettings() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main weather card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .clickable { onNavigateToAddLocation() },
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF515151).copy(alpha = 0.42f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(30.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    } else if (!hasLocation) {
                        // No location set yet — prompt the user to add one
                        Spacer(modifier = Modifier.height(8.dp))
                        Canvas(modifier = Modifier.size(48.dp)) {
                            val cx = size.width / 2; val cy = size.height / 2
                            val r  = size.width / 2 - 3.dp.toPx()
                            drawCircle(color = Color.White, radius = r,
                                style = Stroke(width = 2.5f))
                            val arm = r * 0.45f
                            drawLine(Color.White, Offset(cx, cy - arm), Offset(cx, cy + arm), 2.5f)
                            drawLine(Color.White, Offset(cx - arm, cy), Offset(cx + arm, cy), 2.5f)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No location set",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap here to add a location",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = locationName,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Convert main temperature to user's preferred unit
                        val tempValue = temperature.replace("°C", "").replace("°F", "").trim().toDoubleOrNull() ?: 19.0
                        val convertedTemp = if (settings?.temperatureUnit == "F") {
                            val fahrenheit = (tempValue * 9/5) + 32
                            "${fahrenheit.toInt()}°F"
                        } else {
                            "${tempValue.toInt()}°C"
                        }

                        Text(
                            text = convertedTemp,
                            color = Color.White,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Thin,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = condition,
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        // Convert high/low temps to user's preferred unit
                        val highLowConverted = if (settings?.temperatureUnit == "F") {
                            val parts = highLow.split(" ")
                            val hStr = parts.getOrNull(0)?.replace("H:", "")?.replace("°", "")?.trim()?.toDoubleOrNull() ?: 24.0
                            val lStr = parts.getOrNull(1)?.replace("L:", "")?.replace("°", "")?.trim()?.toDoubleOrNull() ?: 18.0
                            val hF = (hStr * 9/5) + 32
                            val lF = (lStr * 9/5) + 32
                            "H:${hF.toInt()}° L:${lF.toInt()}°"
                        } else {
                            highLow
                        }

                        Text(
                            text = highLowConverted,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFEF5350),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Container for forecasts and metrics
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF515151).copy(alpha = 0.65f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    // 24-Hour Forecast
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1B1B1B).copy(alpha = 0.77f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "24-HOUR FORECAST",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(hourlyForecast, key = { "${fetchId}-${it.time}" }) { item ->
                                    Column(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .width(50.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = item.time,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        WeatherIcon(
                                            iconType = item.iconType,
                                            modifier = Modifier.size(28.dp)
                                        )

                                        // Convert hourly temperature to user's preferred unit
                                        val tempStr = item.temperature.replace("°C", "").replace("°F", "").trim()
                                        val tempC = tempStr.toDoubleOrNull() ?: 19.0
                                        val convertedHourlyTemp = if (settings?.temperatureUnit == "F") {
                                            val fahrenheit = (tempC * 9/5) + 32
                                            "${fahrenheit.toInt()}°F"
                                        } else {
                                            "${tempC.toInt()}°C"
                                        }

                                        Text(
                                            text = convertedHourlyTemp,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5-day Forecast
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1B1B1B).copy(alpha = 0.77f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "5-DAY FORECAST",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                dailyForecast.forEach { item ->
                                    Column(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = item.day,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        WeatherIcon(
                                            iconType = item.iconType,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chance of Rain and Metrics side by side
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Chance of Rain card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1B1B1B).copy(alpha = 0.77f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CHANCE OF RAIN",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = chanceOfRain,
                                    color = Color.White,
                                    fontSize = 60.sp,
                                    fontWeight = FontWeight.Thin,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                CloudRainIcon(
                                    modifier = Modifier.size(60.dp),
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }

                        // Metrics card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1B1B1B).copy(alpha = 0.77f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = "METRICS",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                )

                                metrics.forEach { metric ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = metric.label,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp
                                        )

                                        Text(
                                            text = metric.value,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Pull-to-refresh spinner shown at the top while refreshing
        PullRefreshIndicator(
            refreshing = isLoading,
            state      = pullRefreshState,
            modifier   = Modifier.align(Alignment.TopCenter),
            contentColor = Color.White,
            backgroundColor = Color(0xFF2E2E2E)
        )
    }

    // SOS Success Dialog
    if (showSosPopup.value) {
        SosSuccessDialog(
            onDismiss = { showSosPopup.value = false }
        )

        LaunchedEffect(Unit) {
            delay(2000)
            showSosPopup.value = false
        }
    }
}

/**
 * Emergency Contact data model
 */
data class EmergencyContactItem(
    val id: String,
    val name: String,
    val phone: String
)

/**
 * Emergency Contact Screen
 */
@Composable
fun EmergencyContactScreen(
    onBack: () -> Unit
) {
    val contacts = remember {
        listOf(
            EmergencyContactItem("1", "Mj Bautista", "09165543123"),
            EmergencyContactItem("2", "Juan Carlos Basa", "09165543123"),
            EmergencyContactItem("3", "Charles Medel", "09165543123"),
            EmergencyContactItem("4", "Frances Balgos", "09165543123")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Weather background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x51515199))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 12.dp)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        val centerX = size.width / 2
                        val length = size.width / 2.5f

                        drawLine(
                            color = Color.White,
                            start = Offset(centerX + length / 2, centerY - length / 2),
                            end = Offset(centerX - length / 2, centerY),
                            strokeWidth = 2.2f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - length / 2, centerY),
                            end = Offset(centerX + length / 2, centerY + length / 2),
                            strokeWidth = 2.2f
                        )
                    }
                }

                Text(
                    text = "Previous",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Emergency Contact",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                contacts.forEach { contact ->
                    ContactItemCard(contact)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFFB0BEC5),
                            radius = size.width / 2,
                            alpha = 0.5f
                        )
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val len = size.width / 4

                        drawLine(
                            color = Color.White,
                            start = Offset(centerX, centerY - len),
                            end = Offset(centerX, centerY + len),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - len, centerY),
                            end = Offset(centerX + len, centerY),
                            strokeWidth = 2.5f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ADD CONTACT",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * Individual contact card
 */
@Composable
fun ContactItemCard(contact: EmergencyContactItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val phoneWidth = size.width / 2.5f
                        val phoneHeight = size.height / 1.5f

                        drawRect(
                            color = Color.White,
                            topLeft = Offset(centerX - phoneWidth / 2, centerY - phoneHeight / 2),
                            size = Size(phoneWidth, phoneHeight),
                            style = Stroke(width = 1.2f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 0.8f,
                            center = Offset(centerX, centerY + phoneHeight / 3)
                        )
                    }
                }

                Text(
                    text = contact.phone,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )

                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val length = size.width / 2.5f

                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - length / 2, centerY - length / 2.5f),
                            end = Offset(centerX + length / 2, centerY),
                            strokeWidth = 1.5f,
                            alpha = 0.5f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX + length / 2, centerY),
                            end = Offset(centerX - length / 2, centerY + length / 2.5f),
                            strokeWidth = 1.5f,
                            alpha = 0.5f
                        )
                    }
                }
            }
        }
    }
}

/**
 * SOS Settings Screen
 */
@Composable
fun SOSSettingsScreen(
    onBack: () -> Unit
) {
    val toggleLocation = remember { mutableStateOf(true) }
    val messageTemplate = remember { mutableStateOf("My name is...") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Weather background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x51515199))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 12.dp)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        val centerX = size.width / 2
                        val length = size.width / 2.5f

                        drawLine(
                            color = Color.White,
                            start = Offset(centerX + length / 2, centerY - length / 2),
                            end = Offset(centerX - length / 2, centerY),
                            strokeWidth = 2.2f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - length / 2, centerY),
                            end = Offset(centerX + length / 2, centerY + length / 2),
                            strokeWidth = 2.2f
                        )
                    }
                }

                Text(
                    text = "Previous",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SOS Settings",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggle Location
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0x2F2E2E).copy(alpha = 0.68f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Toggle location",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Switch(
                            checked = toggleLocation.value,
                            onCheckedChange = { toggleLocation.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF81C784),
                                uncheckedThumbColor = Color(0xFFF4F3F4),
                                uncheckedTrackColor = Color(0xFF767577)
                            ),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                // Message Template Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0x2F2E2E).copy(alpha = 0.68f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Message Template",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = messageTemplate.value,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Message Input
                androidx.compose.material3.TextField(
                    value = messageTemplate.value,
                    onValueChange = { messageTemplate.value = it },
                    placeholder = {
                        Text(
                            "Enter message template",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0x2F2E2E).copy(alpha = 0.68f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x2F2E2E).copy(alpha = 0.68f),
                        unfocusedContainerColor = Color(0x2F2E2E).copy(alpha = 0.68f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    singleLine = true
                )

                // Send Text SOS Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFBA1E1E).copy(alpha = 0.79f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Send Text SOS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * About & Support Screen
 */
@Composable
fun AboutSupportScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Weather background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x51515199))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 12.dp)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        val centerX = size.width / 2
                        val length = size.width / 2.5f

                        drawLine(
                            color = Color.White,
                            start = Offset(centerX + length / 2, centerY - length / 2),
                            end = Offset(centerX - length / 2, centerY),
                            strokeWidth = 2.2f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - length / 2, centerY),
                            end = Offset(centerX + length / 2, centerY + length / 2),
                            strokeWidth = 2.2f
                        )
                    }
                }

                Text(
                    text = "Previous",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "About & Support",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // About Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawCircle(
                                color = Color.White,
                                radius = size.width / 2,
                                style = Stroke(width = 2f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.5f,
                                center = Offset(size.width / 2, size.height / 2.5f)
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(size.width / 2, size.height / 2.5f + 1.5f),
                                end = Offset(size.width / 2, size.height * 0.65f),
                                strokeWidth = 1.5f
                            )
                        }

                        Text(
                            text = "About JuanWeather",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "App Version",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "1.0.0",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Build Number",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "2024.01.001",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "JuanWeather is your personal weather companion, providing real-time weather updates, emergency alerts, and location-based weather information to keep you safe and informed.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Support Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(size.width / 4, size.height / 3),
                                size = Size(size.width / 2, size.height / 3),
                                style = Stroke(width = 1.5f)
                            )
                        }

                        Text(
                            text = "Get Support",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = Color(0xFF81C784).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(20.dp)) {
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(size.width / 5, size.height / 3),
                                            size = Size(size.width * 0.6f, size.height / 3),
                                            style = Stroke(width = 1.5f)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Email Support",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "support@juanweather.com",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Canvas(modifier = Modifier.size(20.dp)) {
                                val centerX = size.width / 2
                                val centerY = size.height / 2
                                val length = size.width / 3

                                drawLine(
                                    color = Color(0xFF81C784),
                                    start = Offset(centerX - length / 2, centerY - length / 2.5f),
                                    end = Offset(centerX + length / 2, centerY),
                                    strokeWidth = 2f
                                )
                                drawLine(
                                    color = Color(0xFF81C784),
                                    start = Offset(centerX + length / 2, centerY),
                                    end = Offset(centerX - length / 2, centerY + length / 2.5f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = Color(0xFF81C784).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(20.dp)) {
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(size.width / 3.5f, size.height / 4),
                                            size = Size(size.width / 3.5f, size.height / 2),
                                            style = Stroke(width = 1.5f)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Phone Support",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "+1 (234) 567-890",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Canvas(modifier = Modifier.size(20.dp)) {
                                val centerX = size.width / 2
                                val centerY = size.height / 2
                                val length = size.width / 3

                                drawLine(
                                    color = Color(0xFF81C784),
                                    start = Offset(centerX - length / 2, centerY - length / 2.5f),
                                    end = Offset(centerX + length / 2, centerY),
                                    strokeWidth = 2f
                                )
                                drawLine(
                                    color = Color(0xFF81C784),
                                    start = Offset(centerX + length / 2, centerY),
                                    end = Offset(centerX - length / 2, centerY + length / 2.5f),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                }

                // Legal Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawCircle(
                                color = Color.White,
                                radius = size.width / 2,
                                style = Stroke(width = 2f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.5f,
                                center = Offset(size.width / 2, size.height / 2.5f)
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(size.width / 2, size.height / 2.5f + 1.5f),
                                end = Offset(size.width / 2, size.height * 0.65f),
                                strokeWidth = 1.5f
                            )
                        }

                        Text(
                            text = "Legal",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Privacy Policy",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Canvas(modifier = Modifier.size(16.dp)) {
                                val centerX = size.width / 2
                                val centerY = size.height / 2
                                val length = size.width / 2.5f

                                drawLine(
                                    color = Color.White.copy(alpha = 0.5f),
                                    start = Offset(centerX - length / 2, centerY - length / 2.5f),
                                    end = Offset(centerX + length / 2, centerY),
                                    strokeWidth = 1.5f
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.5f),
                                    start = Offset(centerX + length / 2, centerY),
                                    end = Offset(centerX - length / 2, centerY + length / 2.5f),
                                    strokeWidth = 1.5f
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Terms of Service",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Canvas(modifier = Modifier.size(16.dp)) {
                                val centerX = size.width / 2
                                val centerY = size.height / 2
                                val length = size.width / 2.5f

                                drawLine(
                                    color = Color.White.copy(alpha = 0.5f),
                                    start = Offset(centerX - length / 2, centerY - length / 2.5f),
                                    end = Offset(centerX + length / 2, centerY),
                                    strokeWidth = 1.5f
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.5f),
                                    start = Offset(centerX + length / 2, centerY),
                                    end = Offset(centerX - length / 2, centerY + length / 2.5f),
                                    strokeWidth = 1.5f
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "© 2024 JuanWeather. All rights reserved.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SosSuccessDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF515151).copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Checkmark container
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(35.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 40.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Message Sent!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Your SOS alert has been sent to emergency contacts",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Location weather data model
 */
data class LocationWeather(
    val id: String,
    val city: String,           // API-normalized city name for display
    val temp: Int,
    val condition: String,
    val highTemp: Int,
    val icon: String,
    val locationId: Int = 0,    // Room PK — used for delete
    val cityName: String = ""   // Original city name from user input — used for Firestore delete
)

/**
 * Add Location Screen
 */
@Composable
fun AddLocationScreen(
    onBack: () -> Unit,
    locationViewModel: com.juanweather.viewmodel.LocationViewModel? = null,
    onLocationSelected: ((LocationWeather) -> Unit)? = null,
    userId: Int = 0,
    temperatureUnit: String = "C" // <-- Add this parameter
) {
    val locationCards = locationViewModel?.locationCards?.collectAsState()?.value ?: emptyList()
    val isLoading     = locationViewModel?.isLoading?.collectAsState()?.value    ?: false
    val addResult     = locationViewModel?.addResult?.collectAsState()?.value
        ?: com.juanweather.viewmodel.LocationViewModel.AddResult.Idle

    val showAddDialog = remember { mutableStateOf(false) }
    val cityInput     = remember { mutableStateOf("") }
    val inputError    = remember { mutableStateOf<String?>(null) }

    // Load locations when screen is shown
    LaunchedEffect(userId) {
        if (userId > 0) {
            locationViewModel?.loadLocationsForUser(userId)
        }
    }

    // React to add result
    LaunchedEffect(addResult) {
        when (addResult) {
            is com.juanweather.viewmodel.LocationViewModel.AddResult.Success -> {
                showAddDialog.value = false
                cityInput.value = ""
                inputError.value = null
                locationViewModel?.resetAddResult()
            }
            is com.juanweather.viewmodel.LocationViewModel.AddResult.Error -> {
                inputError.value = addResult.message
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background image
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Weather background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x51515199))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        val centerX = size.width / 2
                        val length  = size.width / 2.5f
                        drawLine(color = Color.White, start = Offset(centerX + length / 2, centerY - length / 2), end = Offset(centerX - length / 2, centerY), strokeWidth = 2.2f)
                        drawLine(color = Color.White, start = Offset(centerX - length / 2, centerY), end = Offset(centerX + length / 2, centerY + length / 2), strokeWidth = 2.2f)
                    }
                }
                Text(
                    text = "Previous",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                }
            }

            // Location cards from Room
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (locationCards.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No locations saved yet.\nTap + to add your first location.",
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
                locationCards.forEach { location ->
                    LocationWeatherCard(
                        location = location,
                        onDelete = { locationViewModel?.deleteLocation(location.locationId, location.cityName.ifBlank { location.city }) },
                        onClick  = if (onLocationSelected != null) {
                            { onLocationSelected(location) }
                        } else null,
                        temperatureUnit = temperatureUnit // <-- Use the parameter here
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ADD LOCATION button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { showAddDialog.value = true },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color(0xFFB0BEC5), radius = size.width / 2, alpha = 0.5f)
                        val cx = size.width / 2; val cy = size.height / 2; val len = size.width / 4
                        drawLine(color = Color.White, start = Offset(cx, cy - len), end = Offset(cx, cy + len), strokeWidth = 2.5f)
                        drawLine(color = Color.White, start = Offset(cx - len, cy), end = Offset(cx + len, cy), strokeWidth = 2.5f)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ADD LOCATION",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.clickable { showAddDialog.value = true }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // ── Add Location Dialog ──────────────────────────────────────
        if (showAddDialog.value) {
            Dialog(onDismissRequest = {
                showAddDialog.value = false
                cityInput.value = ""
                inputError.value = null
                locationViewModel?.resetAddResult()
            }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Add Location", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Enter a city name", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        TextField(
                            value = cityInput.value,
                            onValueChange = {
                                cityInput.value = it
                                inputError.value = null
                                locationViewModel?.resetAddResult()
                            },
                            placeholder = { Text("e.g. Manila, Tagaytay", color = Color.White.copy(alpha = 0.4f)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            isError = inputError.value != null,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor        = Color.White,
                                unfocusedTextColor      = Color.White,
                                focusedLabelColor       = Color.White.copy(alpha = 0.7f),
                                unfocusedLabelColor     = Color.White.copy(alpha = 0.7f),
                                focusedIndicatorColor   = Color(0xFF81C784),
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        if (inputError.value != null) {
                            Text(inputError.value!!, color = Color(0xFFEF5350), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        val isAdding = addResult is com.juanweather.viewmodel.LocationViewModel.AddResult.Loading

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showAddDialog.value = false
                                    cityInput.value = ""
                                    inputError.value = null
                                    locationViewModel?.resetAddResult()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Cancel", color = Color.White) }

                            Button(
                                onClick = { if (!isAdding) locationViewModel?.addLocation(cityInput.value) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isAdding
                            ) {
                                if (isAdding) androidx.compose.material3.CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                                else Text("Add", color = Color.Black, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Location Weather Card Component
 */
@Composable
fun LocationWeatherCard(
    location: LocationWeather,
    onDelete: () -> Unit = {},
    onClick: (() -> Unit)? = null,
    temperatureUnit: String = "C" // Default to Celsius if not provided
) {
    val isFahrenheit = temperatureUnit == "F"
    val displayTemp = try { convertTemperature(location.temp.toDouble(), isFahrenheit) } catch (_: Exception) { "${location.temp}°" }
    val displayHighTemp = try { convertTemperature(location.highTemp.toDouble(), isFahrenheit) } catch (_: Exception) { "H:${location.highTemp}°" }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0x2F2E2E).copy(alpha = 0.68f), shape = RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                WeatherIconLarge(iconType = location.icon)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .padding(start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = location.city,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayTemp,
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Thin,
                    lineHeight = 60.sp
                )
                Text(
                    text = location.condition,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayHighTemp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            // Delete button — wired to onDelete callback
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onDelete() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().alpha(0.6f)) {
                    val centerX = size.width / 2; val centerY = size.height / 2; val inset = size.width / 6
                    drawLine(color = Color.White, start = Offset(centerX - inset * 1.2f, centerY - inset * 1.2f), end = Offset(centerX + inset * 1.2f, centerY - inset * 1.2f), strokeWidth = 1.5f)
                    drawLine(color = Color.White, start = Offset(centerX - inset * 1.2f, centerY - inset * 0.8f), end = Offset(centerX + inset * 1.2f, centerY - inset * 0.8f), strokeWidth = 1.5f)
                    drawRect(color = Color.White, topLeft = Offset(centerX - inset, centerY - inset * 0.5f), size = Size(inset * 2, inset * 1.8f), style = Stroke(width = 1.5f))
                    drawLine(color = Color.White, start = Offset(centerX - inset * 0.5f, centerY - inset * 0.5f), end = Offset(centerX - inset * 0.5f, centerY + inset * 0.9f), strokeWidth = 1.5f)
                    drawLine(color = Color.White, start = Offset(centerX, centerY - inset * 0.5f), end = Offset(centerX, centerY + inset * 0.9f), strokeWidth = 1.5f)
                    drawLine(color = Color.White, start = Offset(centerX + inset * 0.5f, centerY - inset * 0.5f), end = Offset(centerX + inset * 0.5f, centerY + inset * 0.9f), strokeWidth = 1.5f)
                }
            }
        }
    }
}

/**
 * Large weather icon component
 */
@Composable
fun WeatherIconLarge(iconType: String) {
    when (iconType) {
        "sun" -> SunIcon(
            modifier = Modifier.size(100.dp),
            color = Color(0xFFFCD34D)
        )

        "cloud" -> CloudIcon(
            modifier = Modifier.size(100.dp),
            color = Color(0xFFE5E7EB)
        )

        "rain" -> CloudRainIcon(
            modifier = Modifier.size(100.dp),
            color = Color(0xFF93C5FD)
        )

        else -> SunIcon(
            modifier = Modifier.size(100.dp),
            color = Color(0xFFFCD34D)
        )
    }
}

/**
 * Settings item data model
 */
data class SettingsItem(
    val id: String,
    val title: String,
    val icon: String  // "settings", "phone", "alert", "info"
)

/**
 * Settings Screen
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToWeatherPreferences: () -> Unit = {},
    onNavigateToEmergencyContact: () -> Unit = {},
    onNavigateToSOSSettings: () -> Unit = {},
    onNavigateToAboutSupport: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val settingsItems = remember {
        listOf(
            SettingsItem("1", "Weather Preferences", "settings"),
            SettingsItem("2", "Emergency Contact", "phone"),
            SettingsItem("3", "SOS Settings", "alert"),
            SettingsItem("4", "About & Support", "info")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background image
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Weather background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x51515199))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 12.dp)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back arrow icon
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        val centerX = size.width / 2
                        val length = size.width / 2.5f

                        drawLine(
                            color = Color.White,
                            start = Offset(centerX + length / 2, centerY - length / 2),
                            end = Offset(centerX - length / 2, centerY),
                            strokeWidth = 2.2f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - length / 2, centerY),
                            end = Offset(centerX + length / 2, centerY + length / 2),
                            strokeWidth = 2.2f
                        )
                    }
                }

                Text(
                    text = "Previous",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Settings title card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                settingsItems.forEach { item ->
                    SettingsItemCard(
                        item = item,
                        onClick = {
                            when (item.id) {
                                "1" -> onNavigateToWeatherPreferences()
                                "2" -> onNavigateToEmergencyContact()
                                "3" -> onNavigateToSOSSettings()
                                "4" -> onNavigateToAboutSupport()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Logout button
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBA1E1E),
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Logout",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * Individual settings item card
 */
@Composable
fun SettingsItemCard(
    item: SettingsItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon and title
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SettingsIcon(iconType = item.icon)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Title
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Arrow icon
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val length = size.width / 3

                    drawLine(
                        color = Color.White,
                        start = Offset(centerX - length / 2, centerY - length / 2),
                        end = Offset(centerX + length / 2, centerY),
                        strokeWidth = 2f,
                        alpha = 0.5f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(centerX + length / 2, centerY),
                        end = Offset(centerX - length / 2, centerY + length / 2),
                        strokeWidth = 2f,
                        alpha = 0.5f
                    )
                }
            }
        }
    }
}

/**
 * Settings icon component - draws different icons based on type
 */
@Composable
fun SettingsIcon(iconType: String) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        when (iconType) {
            "settings" -> {
                // Gear/Settings icon
                drawCircle(
                    color = Color.White,
                    radius = size.width / 5,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5f)
                )
                // Gear teeth
                for (i in 0..5) {
                    val angle = (i * 60f) * (Math.PI / 180.0)
                    val x1 = centerX + (size.width / 3.5f * kotlin.math.cos(angle)).toFloat()
                    val y1 = centerY + (size.width / 3.5f * kotlin.math.sin(angle)).toFloat()
                    val x2 = centerX + (size.width / 2.2f * kotlin.math.cos(angle)).toFloat()
                    val y2 = centerY + (size.width / 2.2f * kotlin.math.sin(angle)).toFloat()

                    drawLine(
                        color = Color.White,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1.5f
                    )
                }
            }

            "phone" -> {
                // Phone icon
                drawRect(
                    color = Color.White,
                    topLeft = Offset(centerX - size.width / 3.5f, centerY - size.height / 2.5f),
                    size = Size(size.width / 1.75f, size.height / 1.25f),
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.5f,
                    center = Offset(centerX, centerY + size.height / 4)
                )
            }

            "alert" -> {
                // Triangle alert icon
                val topX = centerX
                val topY = centerY - size.height / 2.5f
                val bottomLeftX = centerX - size.width / 2.5f
                val bottomLeftY = centerY + size.height / 3
                val bottomRightX = centerX + size.width / 2.5f
                val bottomRightY = centerY + size.height / 3

                drawLine(
                    color = Color.White,
                    start = Offset(topX, topY),
                    end = Offset(bottomLeftX, bottomLeftY),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(bottomLeftX, bottomLeftY),
                    end = Offset(bottomRightX, bottomRightY),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(bottomRightX, bottomRightY),
                    end = Offset(topX, topY),
                    strokeWidth = 1.5f
                )
                // Exclamation mark
                drawCircle(
                    color = Color.White,
                    radius = 1.2f,
                    center = Offset(centerX, centerY - 2.dp.toPx())
                )
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY + 1.dp.toPx()),
                    end = Offset(centerX, centerY + 5.dp.toPx()),
                    strokeWidth = 1.5f
                )
            }

            "info" -> {
                // Info icon (circle with 'i')
                drawCircle(
                    color = Color.White,
                    radius = size.width / 2,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5f)
                )
                // Dot
                drawCircle(
                    color = Color.White,
                    radius = 1.2f,
                    center = Offset(centerX, centerY - 4.dp.toPx())
                )
                // Line
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY - 1.dp.toPx()),
                    end = Offset(centerX, centerY + 5.dp.toPx()),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

/**
 * Placeholder Screen for upcoming settings sub-screens
 */
@Composable
fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background image
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Weather background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x51515199))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 12.dp)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back arrow icon
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        val centerX = size.width / 2
                        val length = size.width / 2.5f

                        drawLine(
                            color = Color.White,
                            start = Offset(centerX + length / 2, centerY - length / 2),
                            end = Offset(centerX - length / 2, centerY),
                            strokeWidth = 2.2f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - length / 2, centerY),
                            end = Offset(centerX + length / 2, centerY + length / 2),
                            strokeWidth = 2.2f
                        )
                    }
                }

                Text(
                    text = "Previous",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Title card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Coming soon message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Coming Soon",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Weather Preferences Screen
 */
@Composable
fun WeatherPreferencesScreen(
    settingsViewModel: com.juanweather.viewmodel.SettingsViewModel,
    loggedInUser: com.juanweather.data.models.User?,
    onBack: () -> Unit
) {
    val settingsState = settingsViewModel.settings.collectAsState()
    val settings = settingsState.value

    // Load settings on first appearance
    LaunchedEffect(loggedInUser?.id) {
        if (loggedInUser != null && loggedInUser.id > 0) {
            settingsViewModel.loadSettingsForUser(loggedInUser.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background image
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Weather background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x51515199))
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 12.dp)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2
                        val centerX = size.width / 2
                        val length = size.width / 2.5f

                        drawLine(
                            color = Color.White,
                            start = Offset(centerX + length / 2, centerY - length / 2),
                            end = Offset(centerX - length / 2, centerY),
                            strokeWidth = 2.2f
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(centerX - length / 2, centerY),
                            end = Offset(centerX + length / 2, centerY + length / 2),
                            strokeWidth = 2.2f
                        )
                    }
                }

                Text(
                    text = "Previous",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Title card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Weather Preferences",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preferences container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Temperature Unit Selector
                SettingsTileWithOptions(
                    title = "Temperature Unit",
                    selectedValue = if (settings?.temperatureUnit == "C") "Celsius" else "Fahrenheit",
                    options = listOf("Celsius", "Fahrenheit"),
                    onOptionSelected = { selected ->
                        val newUnit = if (selected == "Celsius") "C" else "F"
                        settingsViewModel.updateTemperatureUnit(newUnit)
                    }
                )

                // Wind Speed Unit Selector
                SettingsTileWithOptions(
                    title = "Wind Speed Unit",
                    selectedValue = if (settings?.windSpeedUnit == "km/h") "km/h" else "mph",
                    options = listOf("km/h", "mph"),
                    onOptionSelected = { selected ->
                        settingsViewModel.updateWindSpeedUnit(selected)
                    }
                )

                // Pressure Unit Selector
                SettingsTileWithOptions(
                    title = "Pressure Unit",
                    selectedValue = if (settings?.pressureUnit == "mb") "Millibar (mb)" else "Inches of Mercury (inHg)",
                    options = listOf("Millibar (mb)", "Inches of Mercury (inHg)"),
                    onOptionSelected = { selected ->
                        val newUnit = if (selected.contains("mb")) "mb" else "inHg"
                        settingsViewModel.updatePressureUnit(newUnit)
                    }
                )

                // Visibility Unit Selector
                SettingsTileWithOptions(
                    title = "Visibility Unit",
                    selectedValue = if (settings?.visibilityUnit == "km") "Kilometers" else "Miles",
                    options = listOf("Kilometers", "Miles"),
                    onOptionSelected = { selected ->
                        val newUnit = if (selected == "Kilometers") "km" else "mi"
                        settingsViewModel.updateVisibilityUnit(newUnit)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notifications Toggle
                SettingsTileWithOptions(
                    title = "Notifications",
                    selectedValue = if (settings?.notificationsEnabled == true) "Enabled" else "Disabled",
                    options = listOf("Enabled", "Disabled"),
                    onOptionSelected = { selected ->
                        settingsViewModel.updateNotifications(selected == "Enabled")
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * Settings Tile with Options - shows a dropdown/popup with selectable options
 */
@Composable
fun SettingsTileWithOptions(
    title: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = selectedValue,
                        color = Color(0xFF81C784),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Dropdown indicator
                Box(
                    modifier = Modifier
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val length = size.width / 3

                        if (expanded) {
                            // Chevron up
                            drawLine(
                                color = Color(0xFF81C784),
                                start = Offset(centerX - length / 2, centerY + length / 2),
                                end = Offset(centerX, centerY - length / 2),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = Color(0xFF81C784),
                                start = Offset(centerX, centerY - length / 2),
                                end = Offset(centerX + length / 2, centerY + length / 2),
                                strokeWidth = 2f
                            )
                        } else {
                            // Chevron down
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = Offset(centerX - length / 2, centerY - length / 2),
                                end = Offset(centerX + length / 2, centerY),
                                strokeWidth = 2f,
                                alpha = 0.5f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = Offset(centerX + length / 2, centerY),
                                end = Offset(centerX - length / 2, centerY + length / 2),
                                strokeWidth = 2f,
                                alpha = 0.5f
                            )
                        }
                    }
                }
            }

            // Options dropdown
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0x1F1F1F).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(bottom = 8.dp)
                ) {
                    options.forEach { option ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected(option)
                                    expanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = option,
                                    color = if (option == selectedValue) Color(0xFF81C784) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (option == selectedValue) FontWeight.SemiBold else FontWeight.Normal
                                )

                                // Checkmark for selected option
                                if (option == selectedValue) {
                                    Box(
                                        modifier = Modifier.size(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✓",
                                            color = Color(0xFF81C784),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Divider between options
                        if (option != options.last()) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Login Screen Composable
 * Authenticates users against the Room database.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    authViewModel: com.juanweather.viewmodel.AuthViewModel? = null
) {
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val showPassword = remember { mutableStateOf(false) }
    val showError = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    val emailError = remember { mutableStateOf(false) }

    val authState = authViewModel?.authState?.collectAsState()

    // Observe ViewModel auth state
    LaunchedEffect(authState?.value) {
        when (val state = authState?.value) {
            is com.juanweather.viewmodel.AuthViewModel.AuthState.LoginSuccess -> {
                onLoginSuccess()
                authViewModel.resetState()
            }
            is com.juanweather.viewmodel.AuthViewModel.AuthState.Error -> {
                showError.value = true
                errorMessage.value = state.message
            }
            else -> {}
        }
    }

    val isLoading = authState?.value is com.juanweather.viewmodel.AuthViewModel.AuthState.Loading

    // Email/username validation — 'admin' bypasses email format check
    fun isValidEmail(email: String): Boolean {
        if (email == "admin") return true
        val emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$"
        return email.matches(emailPattern.toRegex()) && email.contains("@") && email.contains(".")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background image
        AsyncImage(
            model = R.drawable.background,
            contentDescription = "Login background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x51515199))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Juan Weather",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Login Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0x2F2E2E).copy(alpha = 0.68f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Login",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Field
                    TextField(
                        value = username.value,
                        onValueChange = {
                            username.value = it
                            showError.value = false
                            // Validate email in real-time
                            emailError.value = username.value.isNotEmpty() && !isValidEmail(username.value)
                        },
                        label = { Text("Email", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.White.copy(alpha = 0.7f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedIndicatorColor = if (emailError.value) Color(0xFFEF5350) else Color(0xFF81C784),
                            unfocusedIndicatorColor = if (emailError.value) Color(0xFFEF5350) else Color.White.copy(alpha = 0.3f)
                        )
                    )

                    // Email validation error
                    if (emailError.value) {
                        Text(
                            text = "Please enter a valid email address",
                            color = Color(0xFFEF5350),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    TextField(
                        value = password.value,
                        onValueChange = { password.value = it; showError.value = false },
                        label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword.value) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .clickable { showPassword.value = !showPassword.value }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (showPassword.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showPassword.value) "Hide password" else "Show password",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.White.copy(alpha = 0.7f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedIndicatorColor = Color(0xFF81C784),
                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error message
                    if (showError.value) {
                        Text(
                            text = errorMessage.value,
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Login Button
                    Button(
                        onClick = {
                            showError.value = false
                            if (authViewModel != null) {
                                // Room-backed login via AuthViewModel
                                authViewModel.login(username.value.trim(), password.value)
                            } else {
                                // Fallback demo credentials
                                when {
                                    username.value.isEmpty() -> {
                                        showError.value = true
                                        errorMessage.value = "Email is required"
                                    }
                                    !isValidEmail(username.value) -> {
                                        showError.value = true
                                        errorMessage.value = "Please enter a valid email address"
                                    }
                                    password.value.isEmpty() -> {
                                        showError.value = true
                                        errorMessage.value = "Password is required"
                                    }
                                    else -> {
                                        showError.value = true
                                        errorMessage.value = "Invalid email or password"
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF81C784),
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Login",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigate to Register
                    Text(
                        text = "Don't have an account? Register",
                        color = Color(0xFF81C784),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToRegister() },
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * Convert metrics from Celsius/mbar to user's preferred units
 */
fun convertMetricsToUserUnits(
    metrics: List<Metric>,
    settings: com.juanweather.data.models.AppSettings?
): List<Metric> {
    if (settings == null) return metrics

    return metrics.map { metric ->
        when (metric.label) {
            "REAL FEEL" -> {
                // Extract temperature value (assuming format "24°C")
                val tempStr = metric.value.replace("°C", "").trim()
                val tempC = tempStr.toDoubleOrNull() ?: 24.0

                val convertedValue = if (settings.temperatureUnit == "F") {
                    val tempF = (tempC * 9/5) + 32
                    "${tempF.toInt()}°F"
                } else {
                    "${tempC.toInt()}°C"
                }
                metric.copy(value = convertedValue)
            }
            "PRESSURE" -> {
                // Extract pressure value (assuming format "1008mbar")
                val pressureStr = metric.value.replace("mbar", "").trim()
                val pressureMb = pressureStr.toDoubleOrNull() ?: 1008.0

                val convertedValue = if (settings.pressureUnit == "inHg") {
                    val pressureInHg = pressureMb * 0.02953
                    String.format("%.2f inHg", pressureInHg)
                } else {
                    "${pressureMb.toInt()} mb"
                }
                metric.copy(value = convertedValue)
            }
            else -> metric
        }
    }
}

/**
 * Convert temperature from Celsius to user's preferred unit
 */
fun convertTemperature(celsius: Double, isFahrenheit: Boolean): String {
    return if (isFahrenheit) {
        val fahrenheit = (celsius * 9/5) + 32
        "${fahrenheit.toInt()}°F"
    } else {
        "${celsius.toInt()}°C"
    }
}

/**
 * Convert wind speed from km/h to user's preferred unit
 */
fun convertWindSpeed(kmh: Double, toMph: Boolean): String {
    return if (toMph) {
        val mph = kmh * 0.621371
        String.format("%.1f mph", mph)
    } else {
        "${kmh.toInt()} km/h"
    }
}

/**
 * Convert pressure from mb to user's preferred unit
 */
fun convertPressure(mb: Double, toInHg: Boolean): String {
    return if (toInHg) {
        val inHg = mb * 0.02953
        String.format("%.2f inHg", inHg)
    } else {
        "${mb.toInt()} mb"
    }
}

/**
 * Convert visibility from km to user's preferred unit
 */
fun convertVisibility(km: Double, toMiles: Boolean): String {
    return if (toMiles) {
        val miles = km * 0.621371
        String.format("%.1f mi", miles)
    } else {
        "${km.toInt()} km"
    }
}

