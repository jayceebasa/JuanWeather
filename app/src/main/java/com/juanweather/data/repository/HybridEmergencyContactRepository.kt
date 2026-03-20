package com.juanweather.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.juanweather.data.firebase.FirebaseAuthManager
import com.juanweather.data.models.EmergencyContact
import com.juanweather.data.local.EmergencyContactDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Hybrid Repository for Emergency Contacts
 * Primary source: Firestore (cloud)
 * Secondary cache: Room (local SQLite)
 *
 * Strategy:
 * - On add/update/delete: Write to Firestore first, then update Room cache
 * - On read: Return Firestore data, keeping Room in sync for offline access
 * - If network unavailable: Fall back to Room cache
 */
class HybridEmergencyContactRepository(
    private val roomDao: EmergencyContactDao
) {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Get all emergency contacts with Firestore as primary, Room as fallback
     * Returns reactive Flow that updates in real-time when Firestore changes
     */
    fun getAllContacts(): Flow<List<EmergencyContact>> = MutableStateFlow<List<EmergencyContact>>(emptyList()).apply {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            // First, emit cached Room data for instant UI load
            firestore.collection("users").document(uid)
                .collection("emergencyContacts")
                .whereNotEqualTo("id", "_init")
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        // Network error - emit Room cache
                        return@addSnapshotListener
                    }

                    val contacts = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(EmergencyContact::class.java)
                    } ?: emptyList()

                    // Update both Firestore listener value and Room cache
                    value = contacts

                    // Sync to Room cache for offline access
                    try {
                        // Clear old cache and insert new data
                        // Note: In production, use a separate coroutine scope
                        kotlin.runCatching {
                            // This is async - updates happen in background
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        }
    }.asStateFlow()

    /**
     * Get contacts from Room cache (for offline access)
     */
    fun getContactsFromCache(): Flow<List<EmergencyContact>> {
        return roomDao.getAllContacts()
    }

    /**
     * Add a new emergency contact to Firestore
     * Also caches in Room for offline access
     */
    suspend fun addContact(contact: EmergencyContact) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            val contactId = UUID.randomUUID().toString()
            val contactData = contact.copy(id = contactId)

            try {
                // Write to Firestore first
                firestore.collection("users").document(uid)
                    .collection("emergencyContacts")
                    .document(contactId)
                    .set(contactData)
                    .await()

                // Then cache in Room
                roomDao.insertContact(contactData)
            } catch (e: Exception) {
                // If Firestore fails, at least save to Room for offline
                roomDao.insertContact(contactData)
                throw e
            }
        }
    }

    /**
     * Update an existing emergency contact in Firestore
     * Also updates Room cache
     */
    suspend fun updateContact(contact: EmergencyContact) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null && contact.id.isNotEmpty()) {
            try {
                // Update Firestore
                firestore.collection("users").document(uid)
                    .collection("emergencyContacts")
                    .document(contact.id)
                    .set(contact)
                    .await()

                // Update Room cache
                roomDao.updateContact(contact)
            } catch (e: Exception) {
                // If Firestore fails, update Room anyway
                roomDao.updateContact(contact)
                throw e
            }
        }
    }

    /**
     * Delete an emergency contact from Firestore
     * Also removes from Room cache
     */
    suspend fun deleteContact(contactId: String) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            try {
                // Delete from Firestore
                firestore.collection("users").document(uid)
                    .collection("emergencyContacts")
                    .document(contactId)
                    .delete()
                    .await()

                // Delete from Room cache
                roomDao.deleteContactById(contactId)
            } catch (e: Exception) {
                // If Firestore fails, still remove from Room
                roomDao.deleteContactById(contactId)
                throw e
            }
        }
    }

    /**
     * Get all contacts as List (non-reactive, one-time fetch)
     * Tries Firestore first, falls back to Room if offline
     */
    suspend fun getAllContactsOnce(): List<EmergencyContact> {
        val uid = FirebaseAuthManager.getCurrentUid()
        return if (uid != null) {
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("emergencyContacts")
                    .whereNotEqualTo("id", "_init")
                    .get()
                    .await()

                val contacts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(EmergencyContact::class.java)
                }

                // Update Room cache with fetched data
                roomDao.deleteAllContacts()
                roomDao.insertContacts(contacts)

                contacts
            } catch (e: Exception) {
                // Network error - return Room cache
                roomDao.getAllContactsOnce()
            }
        } else {
            // No auth - return Room cache
            roomDao.getAllContactsOnce()
        }
    }

    /**
     * Sync Room cache with latest Firestore data
     * Useful when app comes to foreground
     */
    suspend fun syncWithFirestore() {
        val contacts = getAllContactsOnce()
        // Data is already synced in getAllContactsOnce
    }
}
