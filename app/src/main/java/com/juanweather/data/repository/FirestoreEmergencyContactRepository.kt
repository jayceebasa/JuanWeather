package com.juanweather.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.juanweather.data.firebase.FirebaseAuthManager
import com.juanweather.data.models.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firestore-based repository for Emergency Contacts
 * Stores user-scoped emergency contacts in Firestore subcollection
 * Replaces SharedPreferencesHelper-based storage
 */
class FirestoreEmergencyContactRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Get all emergency contacts for current user as Flow
     */
    fun getAllContacts(): Flow<List<EmergencyContact>> = MutableStateFlow<List<EmergencyContact>>(emptyList()).apply {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("emergencyContacts")
                .whereNotEqualTo("id", "_init")  // Exclude init document
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        return@addSnapshotListener
                    }

                    val contacts = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(EmergencyContact::class.java)
                    } ?: emptyList()

                    value = contacts
                }
        }
    }.asStateFlow()

    /**
     * Add a new emergency contact
     */
    suspend fun addContact(contact: EmergencyContact) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            val contactId = UUID.randomUUID().toString()
            val contactData = contact.copy(id = contactId)

            firestore.collection("users").document(uid)
                .collection("emergencyContacts")
                .document(contactId)
                .set(contactData)
                .await()
        }
    }

    /**
     * Update an existing emergency contact
     */
    suspend fun updateContact(contact: EmergencyContact) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null && contact.id.isNotEmpty()) {
            firestore.collection("users").document(uid)
                .collection("emergencyContacts")
                .document(contact.id)
                .set(contact)
                .await()
        }
    }

    /**
     * Delete an emergency contact
     */
    suspend fun deleteContact(contactId: String) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("emergencyContacts")
                .document(contactId)
                .delete()
                .await()
        }
    }

    /**
     * Get all contacts as List (non-reactive, one-time fetch)
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

                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(EmergencyContact::class.java)
                }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
