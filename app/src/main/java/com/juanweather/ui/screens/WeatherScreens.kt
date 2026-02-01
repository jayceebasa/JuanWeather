package com.juanweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
@Composable
fun WeatherDashboardScreen(
    onNavigateToAddLocation: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val showSosPopup = remember { mutableStateOf(false) }

    // Sample data
    val hourlyForecast = listOf(
        HourlyForecastItem("NOW", "sun", "19°"),
        HourlyForecastItem("12PM", "sun", "22°"),
        HourlyForecastItem("1PM", "sun", "23°"),
        HourlyForecastItem("2PM", "sun", "24°"),
        HourlyForecastItem("3PM", "cloud", "23°"),
        HourlyForecastItem("4PM", "cloud", "22°"),
        HourlyForecastItem("5PM", "rain", "20°")
    )

    val dailyForecast = listOf(
        DailyForecastItem("TODAY", "sun"),
        DailyForecastItem("TUE", "rain"),
        DailyForecastItem("WED", "drizzle"),
        DailyForecastItem("THUR", "cloud"),
        DailyForecastItem("FRI", "cloud")
    )

    val metrics = listOf(
        Metric("HUMIDITY", "91%"),
        Metric("REAL FEEL", "24°C"),
        Metric("UV", "0"),
        Metric("PRESSURE", "1008mbar")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                    Text(
                        text = "Imus",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "19°C",
                        color = Color.White,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Thin,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Mostly Clear",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "H:24° L:18°",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
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
                                items(hourlyForecast) { item ->
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

                                        Text(
                                            text = item.temperature,
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
                                .height(280.dp),
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
                                    text = "91%",
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
                                .height(280.dp),
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
    val city: String,
    val temp: Int,
    val condition: String,
    val highTemp: Int,
    val icon: String
)

/**
 * Add Location Screen
 */
@Composable
fun AddLocationScreen(
    onBack: () -> Unit
) {
    val locations = remember {
        listOf(
            LocationWeather(
                id = "1",
                city = "Imus",
                temp = 19,
                condition = "Mostly Clear",
                highTemp = 24,
                icon = "sun"
            ),
            LocationWeather(
                id = "2",
                city = "Manila",
                temp = 26,
                condition = "Light Rain",
                highTemp = 29,
                icon = "rain"
            ),
            LocationWeather(
                id = "3",
                city = "Tagaytay",
                temp = 22,
                condition = "Cloudy",
                highTemp = 25,
                icon = "cloud"
            )
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
                    modifier = Modifier
                        .size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw left arrow
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

            // Location cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                locations.forEach { location ->
                    LocationWeatherCard(location)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add Location button
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
                        // Draw circle background
                        drawCircle(
                            color = Color(0xFFB0BEC5),
                            radius = size.width / 2,
                            alpha = 0.5f
                        )
                        // Draw plus sign
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
                    text = "ADD LOCATION",
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
 * Location Weather Card Component
 */
@Composable
fun LocationWeatherCard(location: LocationWeather) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x2F2E2E).copy(alpha = 0.68f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Weather icon
            Box(
                modifier = Modifier
                    .size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                WeatherIconLarge(iconType = location.icon)
            }

            // Content section (city, temp, condition, high temp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = location.city,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${location.temp}°",
                    color = Color.White,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Thin,
                    lineHeight = 85.sp
                )

                Text(
                    text = location.condition,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 16.sp
                )

                Text(
                    text = "H:${location.highTemp}°",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }

            // Delete button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.6f)
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val inset = size.width / 6

                    // Draw trash can icon
                    // Top handle
                    drawLine(
                        color = Color.White,
                        start = Offset(centerX - inset * 1.2f, centerY - inset * 1.2f),
                        end = Offset(centerX + inset * 1.2f, centerY - inset * 1.2f),
                        strokeWidth = 1.5f
                    )
                    // Top line
                    drawLine(
                        color = Color.White,
                        start = Offset(centerX - inset * 1.2f, centerY - inset * 0.8f),
                        end = Offset(centerX + inset * 1.2f, centerY - inset * 0.8f),
                        strokeWidth = 1.5f
                    )
                    // Body outline
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(centerX - inset, centerY - inset * 0.5f),
                        size = Size(inset * 2, inset * 1.8f),
                        style = Stroke(width = 1.5f)
                    )
                    // Vertical lines in body
                    drawLine(
                        color = Color.White,
                        start = Offset(centerX - inset * 0.5f, centerY - inset * 0.5f),
                        end = Offset(centerX - inset * 0.5f, centerY + inset * 0.9f),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(centerX, centerY - inset * 0.5f),
                        end = Offset(centerX, centerY + inset * 0.9f),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(centerX + inset * 0.5f, centerY - inset * 0.5f),
                        end = Offset(centerX + inset * 0.5f, centerY + inset * 0.9f),
                        strokeWidth = 1.5f
                    )
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
 * Settings Screen
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B2F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back",
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(8.dp),
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A3E)
                )
            ) {
                Text(
                    text = "Temperature Unit",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A3E)
                )
            ) {
                Text(
                    text = "Notifications",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A3E)
                )
            ) {
                Text(
                    text = "About",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
