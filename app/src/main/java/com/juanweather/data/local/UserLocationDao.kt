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

    // Get all locations for a specific user — reactive Flow
    @Query("SELECT * FROM user_locations WHERE userId = :userId ORDER BY addedAt DESC")
    fun getLocationsForUser(userId: Int): Flow<List<UserLocation>>

    @Delete
    suspend fun deleteLocation(location: UserLocation)

    @Query("DELETE FROM user_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)

    // Check for duplicate city per user
    @Query("SELECT * FROM user_locations WHERE userId = :userId AND cityName = :cityName LIMIT 1")
    suspend fun findLocation(userId: Int, cityName: String): UserLocation?
}
