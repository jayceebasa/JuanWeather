package com.juanweather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Improved weather icons using Canvas - cleaner, crisper rendering
 */

private val STROKE_WIDTH = 2.5f

@Composable
fun SunIcon(
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = Color(0xFFFCD34D)
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val innerRadius = size.width / 6.5f
        val outerRadius = size.width / 2.8f

        drawCircle(
            color = color,
            radius = innerRadius,
            center = center,
            style = Stroke(width = STROKE_WIDTH)
        )

        for (i in 0..7) {
            val angle = (i * 45f) * (Math.PI / 180.0)
            val startX = center.x + (innerRadius * kotlin.math.cos(angle)).toFloat()
            val startY = center.y + (innerRadius * kotlin.math.sin(angle)).toFloat()
            val endX = center.x + (outerRadius * kotlin.math.cos(angle)).toFloat()
            val endY = center.y + (outerRadius * kotlin.math.sin(angle)).toFloat()

            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = STROKE_WIDTH
            )
        }
    }
}

@Composable
fun CloudIcon(
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = Color(0xFFE5E7EB)
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2f

        // Filled cloud: 3 circles arranged to form a cloud shape
        drawCircle(
            color = color,
            radius = size.width / 5.5f,
            center = Offset(centerX - size.width / 3.5f, centerY)
        )

        drawCircle(
            color = color,
            radius = size.width / 3.8f,
            center = Offset(centerX, centerY - size.height / 6f)
        )

        drawCircle(
            color = color,
            radius = size.width / 5.5f,
            center = Offset(centerX + size.width / 3.5f, centerY)
        )
    }
}

@Composable
fun CloudRainIcon(
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = Color(0xFF93C5FD)
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 3.5f

        // Filled cloud part
        drawCircle(
            color = color,
            radius = size.width / 6.5f,
            center = Offset(centerX - size.width / 3.5f, centerY)
        )

        drawCircle(
            color = color,
            radius = size.width / 4f,
            center = Offset(centerX, centerY - size.height / 8f)
        )

        drawCircle(
            color = color,
            radius = size.width / 6.5f,
            center = Offset(centerX + size.width / 3.5f, centerY)
        )

        // Rain drops - 3 vertical lines
        val dropStartY = centerY + size.height / 2.5f
        val dropLen = size.height / 3.5f

        drawLine(
            color = color,
            start = Offset(centerX - size.width / 4.5f, dropStartY),
            end = Offset(centerX - size.width / 4.5f, dropStartY + dropLen),
            strokeWidth = STROKE_WIDTH
        )

        drawLine(
            color = color,
            start = Offset(centerX, dropStartY + dropLen * 0.2f),
            end = Offset(centerX, dropStartY + dropLen * 1.2f),
            strokeWidth = STROKE_WIDTH
        )

        drawLine(
            color = color,
            start = Offset(centerX + size.width / 4.5f, dropStartY),
            end = Offset(centerX + size.width / 4.5f, dropStartY + dropLen),
            strokeWidth = STROKE_WIDTH
        )
    }
}

@Composable
fun CloudDrizzleIcon(
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = Color(0xFF93C5FD)
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 3.5f

        // Filled cloud part
        drawCircle(
            color = color,
            radius = size.width / 6.5f,
            center = Offset(centerX - size.width / 3.5f, centerY)
        )

        drawCircle(
            color = color,
            radius = size.width / 4f,
            center = Offset(centerX, centerY - size.height / 8f)
        )

        drawCircle(
            color = color,
            radius = size.width / 6.5f,
            center = Offset(centerX + size.width / 3.5f, centerY)
        )

        // Drizzle - small dashes in alternating pattern
        val drizzleStartY = centerY + size.height / 2.5f
        val drizzleLen = size.height / 4.5f

        // Left column
        drawLine(
            color = color,
            start = Offset(centerX - size.width / 4.5f, drizzleStartY),
            end = Offset(centerX - size.width / 4.5f, drizzleStartY + drizzleLen),
            strokeWidth = STROKE_WIDTH
        )
        drawLine(
            color = color,
            start = Offset(centerX - size.width / 4.5f, drizzleStartY + drizzleLen * 1.3f),
            end = Offset(centerX - size.width / 4.5f, drizzleStartY + drizzleLen * 2f),
            strokeWidth = STROKE_WIDTH
        )

        // Center column
        drawLine(
            color = color,
            start = Offset(centerX, drizzleStartY + drizzleLen * 0.1f),
            end = Offset(centerX, drizzleStartY + drizzleLen * 0.9f),
            strokeWidth = STROKE_WIDTH
        )
        drawLine(
            color = color,
            start = Offset(centerX, drizzleStartY + drizzleLen * 1.4f),
            end = Offset(centerX, drizzleStartY + drizzleLen * 2.1f),
            strokeWidth = STROKE_WIDTH
        )

        // Right column
        drawLine(
            color = color,
            start = Offset(centerX + size.width / 4.5f, drizzleStartY),
            end = Offset(centerX + size.width / 4.5f, drizzleStartY + drizzleLen),
            strokeWidth = STROKE_WIDTH
        )
        drawLine(
            color = color,
            start = Offset(centerX + size.width / 4.5f, drizzleStartY + drizzleLen * 1.3f),
            end = Offset(centerX + size.width / 4.5f, drizzleStartY + drizzleLen * 2f),
            strokeWidth = STROKE_WIDTH
        )
    }
}

@Composable
fun SettingsIcon(
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val centerR = size.width / 10f

        // 12 gear teeth around the perimeter
        val outerRadius = size.width / 2.3f
        val toothRadius = size.width / 11f

        for (i in 0..11) {
            val angle = (i * 30f) * (Math.PI / 180.0)
            val x = center.x + (outerRadius * kotlin.math.cos(angle)).toFloat()
            val y = center.y + (outerRadius * kotlin.math.sin(angle)).toFloat()

            // Draw each tooth as a circle
            drawCircle(
                color = color,
                radius = toothRadius,
                center = Offset(x, y),
                style = Stroke(width = STROKE_WIDTH)
            )
        }

        // Draw center filled circle
        drawCircle(
            color = color,
            radius = centerR,
            center = center
        )
    }
}

@Composable
fun MoonIcon(
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = Color(0xFFE2E8F0)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Crescent moon: large circle minus an offset smaller circle (drawn as arc trick)
        // Draw a filled arc for the outer circle
        val outerRadius = w / 2.4f
        val center = Offset(w / 2f + w * 0.05f, h / 2f)

        // Outer filled circle
        drawCircle(
            color = color,
            radius = outerRadius,
            center = center
        )

        // Inner "cutout" circle shifted to create crescent
        drawCircle(
            color = Color(0xFF1B1B2F), // matches dark background
            radius = outerRadius * 0.82f,
            center = Offset(center.x + outerRadius * 0.45f, center.y - outerRadius * 0.1f)
        )

        // Small star to the top-right
        val starCenter = Offset(w * 0.82f, h * 0.18f)
        val starR = w / 14f
        drawCircle(color = color, radius = starR, center = starCenter)

        // Tiny star below
        val star2 = Offset(w * 0.75f, h * 0.38f)
        drawCircle(color = color, radius = starR * 0.6f, center = star2)
    }
}

@Composable
fun WeatherIcon(
    iconType: String,
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = Color.White
) {
    when (iconType) {
        "sun"     -> SunIcon(modifier, color = Color(0xFFFCD34D))
        "cloud"   -> CloudIcon(modifier, color = Color(0xFFE5E7EB))
        "rain"    -> CloudRainIcon(modifier, color = Color(0xFF93C5FD))
        "drizzle" -> CloudDrizzleIcon(modifier, color = Color(0xFF93C5FD))
        "night"   -> MoonIcon(modifier)
        else      -> SunIcon(modifier, color = Color(0xFFFCD34D))
    }
}
