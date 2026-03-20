package com.juanweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.juanweather.data.models.SOSSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SOSSettingsDao {

    // CREATE/UPDATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SOSSettings)

    // READ - Get settings as Flow
    @Query("SELECT * FROM sos_settings WHERE id = 'sos_settings'")
    fun getSettings(): Flow<SOSSettings?>

    // READ - Get settings once
    @Query("SELECT * FROM sos_settings WHERE id = 'sos_settings'")
    suspend fun getSettingsOnce(): SOSSettings?

    // UPDATE
    @Update
    suspend fun updateSettings(settings: SOSSettings)

    // DELETE
    @Query("DELETE FROM sos_settings")
    suspend fun deleteAllSettings()
}
