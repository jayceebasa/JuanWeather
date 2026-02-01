package com.juanweather.utils

import java.text.SimpleDateFormat
import java.util.*

fun Long.toFormattedDate(): String {
    val date = Date(this * 1000)
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(date)
}

fun Float.toCelsius(): Int = this.toInt()

fun Float.toFahrenheit(): Int = (this * 9/5 + 32).toInt()

fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
