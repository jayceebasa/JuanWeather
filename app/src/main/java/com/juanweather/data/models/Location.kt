package com.juanweather.data.models

data class Location(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String = "",
    val state: String = "",
    val timezone: String = "",
    val isFavorite: Boolean = false,
    val isSelected: Boolean = false
)
