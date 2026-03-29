package com.juanweather.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,
    val password: String,
    val role: String = "user",          // RBAC: "admin" | "user"
    val createdAt: Long = System.currentTimeMillis(),
    val lastDashboardLocation: String = ""  // Persists the current dashboard city name
)
