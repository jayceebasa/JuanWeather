package com.juanweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.juanweather.data.models.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    /**
     * Insert or ignore if settings already exist
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSettings(settings: AppSettings): Long

    /**
     * Get user's settings as Flow (reactive, auto-updates)
     * Settings are stored per userId for multi-user support
     */
    @Query("SELECT * FROM app_settings WHERE userId = :userId LIMIT 1")
    fun getSettingsForUser(userId: Int): Flow<AppSettings?>

    /**
     * Get user's settings synchronously (one-time fetch)
     */
    @Query("SELECT * FROM app_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettingsForUserSync(userId: Int): AppSettings?

    /**
     * Update existing settings
     */
    @Update
    suspend fun updateSettings(settings: AppSettings)

    /**
     * Delete settings for a specific user
     */
    @Query("DELETE FROM app_settings WHERE userId = :userId")
    suspend fun deleteSettingsForUser(userId: Int)

    /**
     * Get all settings (for admin purposes)
     */
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettings>>
}
