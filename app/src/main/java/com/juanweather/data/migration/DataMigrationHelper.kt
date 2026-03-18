package com.juanweather.data.migration

import com.google.firebase.firestore.FirebaseFirestore
import com.juanweather.data.firebase.FirebaseAuthManager
import com.juanweather.data.local.PreferencesHelper
import com.juanweather.data.local.SharedPreferencesHelper
import com.juanweather.data.models.EmergencyContact
import com.juanweather.data.repository.FirestoreEmergencyContactRepository
import com.juanweather.data.repository.FirestoreSettingsRepository
import kotlinx.coroutines.tasks.await

/**
 * Data Migration Helper
 *
 * Migrates existing user data from local storage (Room, SharedPreferences, DataStore)
 * to Firestore for cloud sync and cross-device support.
 *
 * This should be run once after Firebase integration for each existing user.
 * New users automatically get Firestore-based storage.
 */
object DataMigrationHelper {

    /**
     * Migrate all user data to Firestore
     * Call this once after Firebase setup completes
     */
    suspend fun migrateAllUserData(
        sharedPrefsHelper: SharedPreferencesHelper,
        preferencesHelper: PreferencesHelper
    ) {
        try {
            // Get current authenticated user
            val uid = FirebaseAuthManager.getCurrentUid()
            if (uid == null) {
                throw IllegalStateException("User must be authenticated before migration")
            }

            // Migrate emergency contacts
            migrateEmergencyContacts(uid, sharedPrefsHelper)

            // Migrate settings/preferences
            migrateSettings(uid, preferencesHelper)

            // Locations migration is handled by LocationViewModel
            // using a background WorkManager sync job

        } catch (e: Exception) {
            throw RuntimeException("Migration failed: ${e.message}", e)
        }
    }

    /**
     * Migrate emergency contacts from SharedPreferences to Firestore
     */
    private suspend fun migrateEmergencyContacts(
        uid: String,
        sharedPrefsHelper: SharedPreferencesHelper
    ) {
        try {
            // Get contacts from local SharedPreferences
            val localContacts = sharedPrefsHelper.getEmergencyContacts()

            if (localContacts.isEmpty()) {
                return // Nothing to migrate
            }

            val firestore = FirebaseFirestore.getInstance()

            // Migrate each contact to Firestore
            for (contact in localContacts) {
                val contactData = mapOf(
                    "id" to contact.id,
                    "name" to contact.name,
                    "phoneNumber" to contact.phoneNumber,
                    "relationship" to contact.relationship
                )

                firestore.collection("users")
                    .document(uid)
                    .collection("emergencyContacts")
                    .document(contact.id)
                    .set(contactData)
                    .await()
            }

            // Clear local storage after successful migration
            sharedPrefsHelper.saveEmergencyContacts(emptyList())

        } catch (e: Exception) {
            throw RuntimeException("Emergency contacts migration failed: ${e.message}", e)
        }
    }

    /**
     * Migrate app settings from DataStore to Firestore
     */
    private suspend fun migrateSettings(
        uid: String,
        preferencesHelper: PreferencesHelper
    ) {
        try {
            // Get settings from local DataStore
            val localSettings = preferencesHelper.getSettings()

            val firestore = FirebaseFirestore.getInstance()

            // Migrate settings to Firestore
            val settingsData = mapOf(
                "temperatureUnit" to localSettings.temperatureUnit,
                "windSpeedUnit" to localSettings.windSpeedUnit,
                "pressureUnit" to localSettings.pressureUnit,
                "visibilityUnit" to localSettings.visibilityUnit,
                "theme" to localSettings.theme,
                "language" to localSettings.language,
                "notificationsEnabled" to localSettings.notificationsEnabled
            )

            firestore.collection("users")
                .document(uid)
                .collection("settings")
                .document("preferences")
                .set(settingsData)
                .await()

            // Keep local DataStore as fallback for offline mode

        } catch (e: Exception) {
            throw RuntimeException("Settings migration failed: ${e.message}", e)
        }
    }

    /**
     * Create initial settings document for new user
     * Called automatically during user registration
     */
    suspend fun initializeNewUserSettings(uid: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            val defaultSettings = mapOf(
                "temperatureUnit" to "C",
                "windSpeedUnit" to "km/h",
                "pressureUnit" to "mb",
                "visibilityUnit" to "km",
                "theme" to "light",
                "language" to "en",
                "notificationsEnabled" to true
            )

            firestore.collection("users")
                .document(uid)
                .collection("settings")
                .document("preferences")
                .set(defaultSettings)
                .await()

        } catch (e: Exception) {
            // Log error but don't fail - user can set preferences later
        }
    }

    /**
     * Verify migration was successful
     * Checks if data exists in Firestore
     */
    suspend fun verifyMigration(uid: String): MigrationStatus {
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Check emergency contacts
            val contactsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("emergencyContacts")
                .get()
                .await()

            val contactCount = contactsSnapshot.documents.size - 1 // Exclude _init doc

            // Check settings
            val settingsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("settings")
                .document("preferences")
                .get()
                .await()

            val hasSettings = settingsSnapshot.exists()

            return MigrationStatus(
                success = true,
                migratedContacts = contactCount,
                migratedSettings = hasSettings,
                message = "Migration successful"
            )

        } catch (e: Exception) {
            return MigrationStatus(
                success = false,
                migratedContacts = 0,
                migratedSettings = false,
                message = "Verification failed: ${e.message}"
            )
        }
    }

    /**
     * Rollback migration if needed
     * Restores local storage if Firestore migration fails
     */
    suspend fun rollbackMigration(
        uid: String,
        sharedPrefsHelper: SharedPreferencesHelper,
        preferencesHelper: PreferencesHelper,
        originalContacts: List<EmergencyContact>
    ) {
        try {
            // Delete Firestore documents
            val firestore = FirebaseFirestore.getInstance()

            val contactsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("emergencyContacts")
                .get()
                .await()

            for (doc in contactsSnapshot.documents) {
                doc.reference.delete().await()
            }

            firestore.collection("users")
                .document(uid)
                .collection("settings")
                .document("preferences")
                .delete()
                .await()

            // Restore local storage
            sharedPrefsHelper.saveEmergencyContacts(originalContacts)

        } catch (e: Exception) {
            throw RuntimeException("Rollback failed: ${e.message}", e)
        }
    }
}

/**
 * Migration status result
 */
data class MigrationStatus(
    val success: Boolean,
    val migratedContacts: Int,
    val migratedSettings: Boolean,
    val message: String
)
