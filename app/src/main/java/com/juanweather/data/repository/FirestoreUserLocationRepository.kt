package com.juanweather.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.juanweather.data.firebase.FirebaseAuthManager
import com.juanweather.data.models.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firestore-based repository for User Locations
 * Stores user-scoped locations in Firestore subcollection
 * Works alongside Room database for offline caching (dual-source pattern)
 * Enables location sync across devices via Firestore
 */
class FirestoreUserLocationRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Get all user locations as Flow (reactive, auto-updates)
     */
    fun getAllLocations(): Flow<List<UserLocation>> = MutableStateFlow<List<UserLocation>>(emptyList()).apply {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("locations")
                .whereNotEqualTo("_init", "_init")  // Exclude init document
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        return@addSnapshotListener
                    }

                    val locations = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(UserLocation::class.java)
                    } ?: emptyList()

                    value = locations
                }
        }
    }.asStateFlow()

    /**
     * Add a new user location
     */
    suspend fun addLocation(location: UserLocation) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            val locationId = UUID.randomUUID().toString()
            try {
                // Store location in Firestore without modifying userId
                // It will be stored as-is from Room, and synced back as-is
                firestore.collection("users").document(uid)
                    .collection("locations")
                    .document(locationId)
                    .set(location.copy(addedAt = System.currentTimeMillis()))
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("FirestoreLocationRepo", "Error adding location: ${e.message}", e)
                throw e
            }
        } else {
            android.util.Log.e("FirestoreLocationRepo", "No Firebase UID found")
        }
    }

    /**
     * Update an existing location
     */
    suspend fun updateLocation(location: UserLocation) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("locations")
                .document(location.id.toString())
                .set(location)
                .await()
        }
    }

    /**
     * Delete a location
     */
    suspend fun deleteLocation(locationId: Int) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("locations")
                .document(locationId.toString())
                .delete()
                .await()
        }
    }

    /**
     * Get all locations once (non-reactive, one-time fetch)
     */
    suspend fun getAllLocationsOnce(): List<UserLocation> {
        val uid = FirebaseAuthManager.getCurrentUid()
        return if (uid != null) {
            getAllLocationsByFirebaseUid(uid)
        } else {
            emptyList()
        }
    }

    /**
     * Get all locations for a specific Firebase UID
     * Used for syncing Firestore to Room on login
     */
    suspend fun getAllLocationsByFirebaseUid(firebaseUid: String): List<UserLocation> {
        return try {
            val snapshot = firestore.collection("users").document(firebaseUid)
                .collection("locations")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserLocation::class.java)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreLocationRepo", "Error fetching locations: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Check if a location city already exists for current user
     */
    suspend fun locationExists(cityName: String): Boolean {
        val uid = FirebaseAuthManager.getCurrentUid()
        return if (uid != null) {
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("locations")
                    .whereEqualTo("cityName", cityName)
                    .get()
                    .await()

                !snapshot.isEmpty
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Get home location (if needed - Room model doesn't have isHome flag)
     */
    suspend fun getHomeLocation(): UserLocation? {
        val uid = FirebaseAuthManager.getCurrentUid()
        return if (uid != null) {
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("locations")
                    .get()
                    .await()

                snapshot.documents.firstOrNull()?.toObject(UserLocation::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Set a location as home location (if needed - Room model doesn't support this)
     */
    suspend fun setHomeLocation(locationId: Int) {
        // Room UserLocation model doesn't have isHome flag
        // This is a placeholder for future enhancement
    }
}
