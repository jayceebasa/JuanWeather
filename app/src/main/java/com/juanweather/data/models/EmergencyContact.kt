package com.juanweather.data.models

    import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = ""
)
