package com.juanweather.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_locations")
data class UserLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,            // FK to User.id — each location belongs to a user
    val cityName: String,
    val addedAt: Long = System.currentTimeMillis()
)
