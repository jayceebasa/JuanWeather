package com.juanweather.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.juanweather.data.models.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContact>)

    // READ - Get all contacts as Flow
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    // READ - Get single contact by id
    @Query("SELECT * FROM emergency_contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: String): EmergencyContact?

    // READ - Get all contacts (non-reactive, one-time fetch)
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    suspend fun getAllContactsOnce(): List<EmergencyContact>

    // UPDATE
    @Update
    suspend fun updateContact(contact: EmergencyContact)

    // DELETE
    @Delete
    suspend fun deleteContact(contact: EmergencyContact)

    @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: String)

    // DELETE all contacts
    @Query("DELETE FROM emergency_contacts")
    suspend fun deleteAllContacts()

    // COUNT
    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getContactCount(): Int
}
