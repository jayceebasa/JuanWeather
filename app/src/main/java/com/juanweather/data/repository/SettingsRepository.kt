package com.juanweather.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.juanweather.data.firebase.FirebaseAuthManager
import com.juanweather.data.local.AppSettingsDao
import com.juanweather.data.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Unified Settings Repository
 * Implements dual-source pattern with remote-first priority:
 * - Firebase Firestore: Cloud source of truth
 * - Room Database: Local offline cache
 *
 * Strategy:
 * 1. Try to fetch from Firebase first
 * 2. Save received data to Room for offline use
 * 3. If Firebase fails, fallback to Room
 * 4. All writes go to Firebase first, then synced to Room
 */
class SettingsRepository(private val settingsDao: AppSettingsDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "SettingsRepository"

    /**
     * Get user settings as Flow (reactive, remote-first)
     * Observes Firestore for real-time updates and syncs to Room
     */
    fun getSettings(userId: Int): Flow<AppSettings> = flow {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                // First, try to fetch from Firestore
                val snapshot = firestore.collection("users").document(uid)
                    .collection("settings")
                    .document("preferences")
                    .get()
                    .await()

                val settings = if (snapshot.exists()) {
                    val firestoreSettings = snapshot.toObject(AppSettings::class.java)?.copy(userId = userId)
                        ?: AppSettings(userId = userId)

                    // Sync to Room for offline access
                    try {
                        val existing = settingsDao.getSettingsForUserSync(userId)
                        if (existing != null) {
                            settingsDao.updateSettings(firestoreSettings)
                        } else {
                            settingsDao.insertSettings(firestoreSettings)
                        }
                        Log.d(tag, "Synced settings from Firebase to Room")
                    } catch (e: Exception) {
                        Log.e(tag, "Error syncing to Room: ${e.message}", e)
                    }

                    firestoreSettings
                } else {
                    // No settings in Firebase, use Room or defaults
                    Log.d(tag, "No settings in Firebase, checking Room")
                    settingsDao.getSettingsForUserSync(userId) ?: AppSettings(userId = userId)
                }

                emit(settings)
                Log.d(tag, "Emitted settings: $settings")

            } catch (e: Exception) {
                Log.e(tag, "Firestore fetch failed: ${e.message}", e)
                // Fallback to Room on error
                val roomSettings = settingsDao.getSettingsForUserSync(userId)
                    ?: AppSettings(userId = userId)
                emit(roomSettings)
                Log.d(tag, "Emitted Room settings after Firebase error: $roomSettings")
            }
        } else {
            Log.w(tag, "No Firebase UID found")
            // No UID, use Room or defaults
            val roomSettings = settingsDao.getSettingsForUserSync(userId)
                ?: AppSettings(userId = userId)
            emit(roomSettings)
        }
    }

    /**
     * Get settings once (non-reactive, one-time fetch, remote-first)
     */
    suspend fun getSettingsOnce(userId: Int): AppSettings {
        val uid = FirebaseAuthManager.getCurrentUid()

        return if (uid != null) {
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("settings")
                    .document("preferences")
                    .get()
                    .await()

                if (snapshot.exists()) {
                    val settings = snapshot.toObject(AppSettings::class.java)?.copy(userId = userId)
                        ?: AppSettings(userId = userId)

                    // Sync to Room
                    try {
                        val existing = settingsDao.getSettingsForUserSync(userId)
                        if (existing != null) {
                            settingsDao.updateSettings(settings)
                        } else {
                            settingsDao.insertSettings(settings)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error syncing to Room: ${e.message}", e)
                    }

                    settings
                } else {
                    // Create default settings if doesn't exist
                    AppSettings(userId = userId)
                }
            } catch (e: Exception) {
                Log.e(tag, "Firestore fetch failed: ${e.message}", e)
                // Fallback to Room
                settingsDao.getSettingsForUserSync(userId) ?: AppSettings(userId = userId)
            }
        } else {
            Log.w(tag, "No Firebase UID found")
            // Fallback to Room
            settingsDao.getSettingsForUserSync(userId) ?: AppSettings(userId = userId)
        }
    }

    /**
     * Save temperature unit (remote-first)
     */
    suspend fun saveTemperatureUnit(userId: Int, unit: String) {
        val uid = FirebaseAuthManager.getCurrentUid()

        // Try Firebase first
        if (uid != null) {
            try {
                // First get current settings
                val existing = settingsDao.getSettingsForUserSync(userId)
                if (existing != null) {
                    val updated = existing.copy(temperatureUnit = unit)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(updated)
                        .await()
                    Log.d(tag, "Saved temperature unit to Firebase: $unit")
                } else {
                    // Create new settings with this unit
                    val newSettings = AppSettings(userId = userId, temperatureUnit = unit)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(newSettings)
                        .await()
                    Log.d(tag, "Created and saved temperature unit to Firebase: $unit")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        // Always update Room as offline cache
        try {
            val existing = settingsDao.getSettingsForUserSync(userId)
            if (existing != null) {
                settingsDao.updateSettings(existing.copy(temperatureUnit = unit))
            } else {
                settingsDao.insertSettings(AppSettings(userId = userId, temperatureUnit = unit))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Save wind speed unit (remote-first)
     */
    suspend fun saveWindSpeedUnit(userId: Int, unit: String) {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                val existing = settingsDao.getSettingsForUserSync(userId)
                if (existing != null) {
                    val updated = existing.copy(windSpeedUnit = unit)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(updated)
                        .await()
                    Log.d(tag, "Saved wind speed unit to Firebase: $unit")
                } else {
                    val newSettings = AppSettings(userId = userId, windSpeedUnit = unit)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(newSettings)
                        .await()
                    Log.d(tag, "Created and saved wind speed unit to Firebase: $unit")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        try {
            val existing = settingsDao.getSettingsForUserSync(userId)
            if (existing != null) {
                settingsDao.updateSettings(existing.copy(windSpeedUnit = unit))
            } else {
                settingsDao.insertSettings(AppSettings(userId = userId, windSpeedUnit = unit))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Save pressure unit (remote-first)
     */
    suspend fun savePressureUnit(userId: Int, unit: String) {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                val existing = settingsDao.getSettingsForUserSync(userId)
                if (existing != null) {
                    val updated = existing.copy(pressureUnit = unit)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(updated)
                        .await()
                    Log.d(tag, "Saved pressure unit to Firebase: $unit")
                } else {
                    val newSettings = AppSettings(userId = userId, pressureUnit = unit)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(newSettings)
                        .await()
                    Log.d(tag, "Created and saved pressure unit to Firebase: $unit")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        try {
            val existing = settingsDao.getSettingsForUserSync(userId)
            if (existing != null) {
                settingsDao.updateSettings(existing.copy(pressureUnit = unit))
            } else {
                settingsDao.insertSettings(AppSettings(userId = userId, pressureUnit = unit))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Save visibility unit (remote-first)
     */
    suspend fun saveVisibilityUnit(userId: Int, unit: String) {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                val existing = settingsDao.getSettingsForUserSync(userId)
                if (existing != null) {
                    val updated = existing.copy(visibilityUnit = unit)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(updated)
                        .await()
                    Log.d(tag, "Saved visibility unit to Firebase: $unit")
                } else {
                    val newSettings = AppSettings(userId = userId, visibilityUnit = unit)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(newSettings)
                        .await()
                    Log.d(tag, "Created and saved visibility unit to Firebase: $unit")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        try {
            val existing = settingsDao.getSettingsForUserSync(userId)
            if (existing != null) {
                settingsDao.updateSettings(existing.copy(visibilityUnit = unit))
            } else {
                settingsDao.insertSettings(AppSettings(userId = userId, visibilityUnit = unit))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Save theme preference (remote-first)
     */
    suspend fun saveTheme(userId: Int, theme: String) {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                val existing = settingsDao.getSettingsForUserSync(userId)
                if (existing != null) {
                    val updated = existing.copy(theme = theme)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(updated)
                        .await()
                    Log.d(tag, "Saved theme to Firebase: $theme")
                } else {
                    val newSettings = AppSettings(userId = userId, theme = theme)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(newSettings)
                        .await()
                    Log.d(tag, "Created and saved theme to Firebase: $theme")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        try {
            val existing = settingsDao.getSettingsForUserSync(userId)
            if (existing != null) {
                settingsDao.updateSettings(existing.copy(theme = theme))
            } else {
                settingsDao.insertSettings(AppSettings(userId = userId, theme = theme))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Save notifications enabled state (remote-first)
     */
    suspend fun saveNotificationsEnabled(userId: Int, enabled: Boolean) {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                val existing = settingsDao.getSettingsForUserSync(userId)
                if (existing != null) {
                    val updated = existing.copy(notificationsEnabled = enabled)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(updated)
                        .await()
                    Log.d(tag, "Saved notifications enabled to Firebase: $enabled")
                } else {
                    val newSettings = AppSettings(userId = userId, notificationsEnabled = enabled)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(newSettings)
                        .await()
                    Log.d(tag, "Created and saved notifications enabled to Firebase: $enabled")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        try {
            val existing = settingsDao.getSettingsForUserSync(userId)
            if (existing != null) {
                settingsDao.updateSettings(existing.copy(notificationsEnabled = enabled))
            } else {
                settingsDao.insertSettings(AppSettings(userId = userId, notificationsEnabled = enabled))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Save language preference (remote-first)
     */
    suspend fun saveLanguage(userId: Int, language: String) {
        val uid = FirebaseAuthManager.getCurrentUid()

        if (uid != null) {
            try {
                val existing = settingsDao.getSettingsForUserSync(userId)
                if (existing != null) {
                    val updated = existing.copy(language = language)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(updated)
                        .await()
                    Log.d(tag, "Saved language to Firebase: $language")
                } else {
                    val newSettings = AppSettings(userId = userId, language = language)
                    firestore.collection("users").document(uid)
                        .collection("settings")
                        .document("preferences")
                        .set(newSettings)
                        .await()
                    Log.d(tag, "Created and saved language to Firebase: $language")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        try {
            val existing = settingsDao.getSettingsForUserSync(userId)
            if (existing != null) {
                settingsDao.updateSettings(existing.copy(language = language))
            } else {
                settingsDao.insertSettings(AppSettings(userId = userId, language = language))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Save all settings at once (remote-first)
     */
    suspend fun saveAllSettings(userId: Int, settings: AppSettings) {
        val uid = FirebaseAuthManager.getCurrentUid()

        // Prepare settings with correct userId
        val settingsWithUserId = settings.copy(userId = userId)

        // Try Firebase first
        if (uid != null) {
            try {
                firestore.collection("users").document(uid)
                    .collection("settings")
                    .document("preferences")
                    .set(settingsWithUserId)
                    .await()
                Log.d(tag, "Saved all settings to Firebase")
            } catch (e: Exception) {
                Log.e(tag, "Error saving to Firebase: ${e.message}", e)
            }
        }

        // Always update Room
        try {
            val existing = settingsDao.getSettingsForUserSync(userId)
            if (existing != null) {
                settingsDao.updateSettings(settingsWithUserId)
            } else {
                settingsDao.insertSettings(settingsWithUserId)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating Room: ${e.message}", e)
        }
    }

    /**
     * Sync settings from Firebase to Room (on login)
     * Called when user logs in to sync cloud settings to local DB
     */
    suspend fun syncFirestoreSettingsToRoom(userId: Int, firebaseUid: String? = null) {
        try {
            val uid = firebaseUid ?: FirebaseAuthManager.getCurrentUid()

            if (!uid.isNullOrBlank()) {
                Log.d(tag, "Syncing settings for user $userId from Firebase UID: $uid")

                val snapshot = firestore.collection("users").document(uid)
                    .collection("settings")
                    .document("preferences")
                    .get()
                    .await()

                if (snapshot.exists()) {
                    val firestoreSettings = snapshot.toObject(AppSettings::class.java)?.copy(userId = userId)
                        ?: AppSettings(userId = userId)

                    Log.d(tag, "Found settings in Firestore for user $userId")

                    // Check if settings exist in Room
                    val existing = settingsDao.getSettingsForUserSync(userId)
                    if (existing != null) {
                        settingsDao.updateSettings(firestoreSettings)
                        Log.d(tag, "Updated settings in Room")
                    } else {
                        settingsDao.insertSettings(firestoreSettings)
                        Log.d(tag, "Inserted settings into Room")
                    }
                } else {
                    Log.d(tag, "No settings found in Firestore for UID: $uid")
                    // Create default settings in Room if doesn't exist
                    val existing = settingsDao.getSettingsForUserSync(userId)
                    if (existing == null) {
                        settingsDao.insertSettings(AppSettings(userId = userId))
                    }
                }
            } else {
                Log.w(tag, "No Firebase UID available for sync")
            }
        } catch (e: Exception) {
            Log.e(tag, "Sync error: ${e.message}", e)
        }
    }
}
