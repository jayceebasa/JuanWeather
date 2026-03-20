package com.juanweather.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.juanweather.data.firebase.FirebaseAuthManager
import com.juanweather.data.local.SOSSettingsDao
import com.juanweather.data.models.SOSSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * Hybrid SOS Repository
 * Implements dual-source pattern with remote-first priority:
 * - Firebase Firestore: Cloud source of truth (synced across devices)
 * - Room Database: Local offline cache
 *
 * Strategy:
 * 1. Try to fetch from Firebase first
 * 2. Save received data to Room for offline use
 * 3. If Firebase fails, fallback to Room
 * 4. All writes go to Firebase first, then synced to Room
 */
class HybridSOSRepository(
    private val sosSettingsDao: SOSSettingsDao
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "HybridSOSRepository"

    /**
     * Get SOS settings as Flow (reactive, remote-first)
     * Observes Firestore for real-time updates and syncs to Room
     */
    fun getSettings(): Flow<SOSSettings?> = flow {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                // First, try to fetch from Firestore
                val snapshot = firestore.collection("users").document(uid)
                    .collection("settings")
                    .document("sos")
                    .get()
                    .await()

                val settings = if (snapshot.exists()) {
                    val firestoreSettings = snapshot.toObject(SOSSettings::class.java)
                        ?: SOSSettings()

                    // Sync to Room for offline access
                    try {
                        val existing = sosSettingsDao.getSettingsOnce()
                        if (existing != null) {
                            sosSettingsDao.updateSettings(firestoreSettings)
                        } else {
                            sosSettingsDao.insertSettings(firestoreSettings)
                        }
                        Log.d(tag, "Synced SOS settings from Firebase to Room")
                    } catch (e: Exception) {
                        Log.e(tag, "Error syncing to Room: ${e.message}", e)
                    }

                    firestoreSettings
                } else {
                    // Firestore doc doesn't exist, try Room
                    sosSettingsDao.getSettingsOnce()
                }

                emit(settings)
            } catch (e: Exception) {
                Log.e(tag, "Error fetching from Firebase: ${e.message}", e)
                // Fallback to Room
                try {
                    emit(sosSettingsDao.getSettingsOnce())
                } catch (roomError: Exception) {
                    Log.e(tag, "Error fetching from Room: ${roomError.message}", roomError)
                    emit(null)
                }
            }
        } else {
            // Not logged in, use Room
            try {
                emit(sosSettingsDao.getSettingsOnce())
            } catch (e: Exception) {
                Log.e(tag, "Error fetching from Room: ${e.message}", e)
                emit(null)
            }
        }
    }

    /**
     * Get SOS settings once (non-reactive, one-time fetch)
     */
    suspend fun getSettingsOnce(): SOSSettings? {
        val uid = FirebaseAuthManager.getCurrentUid()

        return if (uid != null) {
            try {
                // Try Firebase first
                val snapshot = firestore.collection("users").document(uid)
                    .collection("settings")
                    .document("sos")
                    .get()
                    .await()

                val settings = if (snapshot.exists()) {
                    snapshot.toObject(SOSSettings::class.java) ?: SOSSettings()
                } else {
                    SOSSettings()
                }

                // Sync to Room
                try {
                    val existing = sosSettingsDao.getSettingsOnce()
                    if (existing != null) {
                        sosSettingsDao.updateSettings(settings)
                    } else {
                        sosSettingsDao.insertSettings(settings)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error syncing to Room: ${e.message}", e)
                }

                settings
            } catch (e: Exception) {
                Log.e(tag, "Error fetching from Firebase: ${e.message}", e)
                // Fallback to Room
                try {
                    sosSettingsDao.getSettingsOnce()
                } catch (roomError: Exception) {
                    Log.e(tag, "Error fetching from Room: ${roomError.message}", roomError)
                    null
                }
            }
        } else {
            // Not logged in, use Room
            try {
                sosSettingsDao.getSettingsOnce()
            } catch (e: Exception) {
                Log.e(tag, "Error fetching from Room: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Update SOS settings (remote-first)
     */
    suspend fun updateSettings(settings: SOSSettings) {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                // Save to Firebase first
                firestore.collection("users").document(uid)
                    .collection("settings")
                    .document("sos")
                    .set(settings)
                    .await()
                Log.d(tag, "Saved SOS settings to Firebase")
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        // Always save to Room for offline access
        try {
            val existing = sosSettingsDao.getSettingsOnce()
            if (existing != null) {
                sosSettingsDao.updateSettings(settings)
            } else {
                sosSettingsDao.insertSettings(settings)
            }
            Log.d(tag, "Saved SOS settings to Room")
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Update location sharing setting
     */
    suspend fun updateLocationSharing(enabled: Boolean) {
        val currentSettings = getSettingsOnce() ?: SOSSettings()
        updateSettings(currentSettings.copy(enableLocationSharing = enabled))
    }

    /**
     * Update message template
     */
    suspend fun updateMessageTemplate(message: String) {
        val currentSettings = getSettingsOnce() ?: SOSSettings()
        updateSettings(currentSettings.copy(messageTemplate = message))
    }

    /**
     * Update last sent time
     */
    suspend fun updateLastSentTime() {
        val currentSettings = getSettingsOnce() ?: SOSSettings()
        updateSettings(currentSettings.copy(lastSentTime = System.currentTimeMillis()))
    }

    /**
     * Set Twilio credentials
     */
    suspend fun setTwilioCredentials(accountSid: String, authToken: String, phoneNumber: String) {
        val currentSettings = getSettingsOnce() ?: SOSSettings()
        updateSettings(
            currentSettings.copy(
                twilioAccountSid = accountSid,
                twilioAuthToken = authToken,
                twilioPhoneNumber = phoneNumber
            )
        )
    }

    /**
     * Sync SOS settings from Firestore to Room on login
     * Called after user logs in to ensure cloud settings are available offline
     */
    suspend fun syncFirestoreSettingsToRoom(firebaseUid: String) {
        try {
            val snapshot = firestore.collection("users").document(firebaseUid)
                .collection("settings")
                .document("sos")
                .get()
                .await()

            if (snapshot.exists()) {
                val firestoreSettings = snapshot.toObject(SOSSettings::class.java) ?: SOSSettings()

                // Sync to Room
                val existing = sosSettingsDao.getSettingsOnce()
                if (existing != null) {
                    sosSettingsDao.updateSettings(firestoreSettings)
                } else {
                    sosSettingsDao.insertSettings(firestoreSettings)
                }
                Log.d(tag, "Synced SOS settings from Firebase to Room on login")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing on login: ${e.message}", e)
        }
    }
}
