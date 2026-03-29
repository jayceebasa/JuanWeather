package com.juanweather.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.juanweather.data.models.UserLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface UserLocationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(location: UserLocation): Long

    // Get all locations for a specific user — reactive Flow, sorted by lastViewedAt (most recent first)
    @Query("SELECT * FROM user_locations WHERE userId = :userId ORDER BY lastViewedAt DESC")
    fun getLocationsForUser(userId: Int): Flow<List<UserLocation>>

    // Get all locations for a specific user — synchronous (for batch operations)
    @Query("SELECT * FROM user_locations WHERE userId = :userId ORDER BY lastViewedAt DESC")
    suspend fun getLocationsForUserSync(userId: Int): List<UserLocation>

    @Delete
    suspend fun deleteLocation(location: UserLocation)

    @Query("DELETE FROM user_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)

    // Get a location by ID
    @Query("SELECT * FROM user_locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: Int): UserLocation?

    // Update lastViewedAt timestamp when location is viewed
    @Query("UPDATE user_locations SET lastViewedAt = :timestamp WHERE id = :id")
    suspend fun updateLastViewedAt(id: Int, timestamp: Long)

    // Check for duplicate city per user
    @Query("SELECT * FROM user_locations WHERE userId = :userId AND cityName = :cityName LIMIT 1")
    suspend fun findLocation(userId: Int, cityName: String): UserLocation?
}
