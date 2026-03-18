package com.juanweather.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.juanweather.data.firebase.FirebaseAuthManager
import com.juanweather.data.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-based repository for App Settings
 * Stores user-scoped settings in Firestore subcollection
 * Replaces DataStore-based preferences with cloud-synced version
 * Enables settings to sync across user devices
 */
class FirestoreSettingsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Get user settings as Flow (reactive, auto-updates)
     */
    fun getSettings(): Flow<AppSettings> = MutableStateFlow(AppSettings()).apply {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot?.exists() == true) {
                        try {
                            val settings = snapshot.toObject(AppSettings::class.java) ?: AppSettings()
                            value = settings
                        } catch (e: Exception) {
                            // If deserialization fails, use defaults
                            value = AppSettings()
                        }
                    } else {
                        // Use default if doesn't exist
                        value = AppSettings()
                    }
                }
        }
    }.asStateFlow()

    /**
     * Save temperature unit
     */
    suspend fun saveTemperatureUnit(unit: String) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .update("temperatureUnit", unit)
                .await()
        }
    }

    /**
     * Save wind speed unit
     */
    suspend fun saveWindSpeedUnit(unit: String) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .update("windSpeedUnit", unit)
                .await()
        }
    }

    /**
     * Save pressure unit
     */
    suspend fun savePressureUnit(unit: String) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .update("pressureUnit", unit)
                .await()
        }
    }

    /**
     * Save visibility unit
     */
    suspend fun saveVisibilityUnit(unit: String) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .update("visibilityUnit", unit)
                .await()
        }
    }

    /**
     * Save theme preference
     */
    suspend fun saveTheme(theme: String) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .update("theme", theme)
                .await()
        }
    }

    /**
     * Save notifications enabled state
     */
    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .update("notificationsEnabled", enabled)
                .await()
        }
    }

    /**
     * Save language preference
     */
    suspend fun saveLanguage(language: String) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .update("language", language)
                .await()
        }
    }

    /**
     * Save all settings at once
     */
    suspend fun saveAllSettings(settings: AppSettings) {
        val uid = FirebaseAuthManager.getCurrentUid()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("settings")
                .document("preferences")
                .set(settings)
                .await()
        }
    }

    /**
     * Get settings once (non-reactive, one-time fetch)
     */
    suspend fun getSettingsOnce(): AppSettings {
        val uid = FirebaseAuthManager.getCurrentUid()
        return if (uid != null) {
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("settings")
                    .document("preferences")
                    .get()
                    .await()

                if (snapshot.exists()) {
                    snapshot.toObject(AppSettings::class.java) ?: AppSettings()
                } else {
                    AppSettings()
                }
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }
}
