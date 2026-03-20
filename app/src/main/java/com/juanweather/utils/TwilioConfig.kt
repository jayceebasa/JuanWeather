package com.juanweather.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure configuration manager for Twilio credentials
 * Uses EncryptedSharedPreferences for secure storage
 */
class TwilioConfig(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "twilio_config"
        private const val KEY_ACCOUNT_SID = "twilio_account_sid"
        private const val KEY_AUTH_TOKEN = "twilio_auth_token"
        private const val KEY_MESSAGING_SERVICE_SID = "twilio_messaging_service_sid"
        private const val KEY_PHONE_NUMBER = "twilio_phone_number"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save Twilio credentials securely
     * Phone number is needed as the "From" number for SMS sending
     */
    fun saveCredentials(accountSid: String, authToken: String, messagingServiceSid: String, phoneNumber: String = "") {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCOUNT_SID, accountSid)
            putString(KEY_AUTH_TOKEN, authToken)
            putString(KEY_MESSAGING_SERVICE_SID, messagingServiceSid)
            putString(KEY_PHONE_NUMBER, phoneNumber)
            apply()
        }
    }

    /**
     * Get Account SID
     */
    fun getAccountSid(): String {
        return encryptedPrefs.getString(KEY_ACCOUNT_SID, "") ?: ""
    }

    /**
     * Get Auth Token
     */
    fun getAuthToken(): String {
        return encryptedPrefs.getString(KEY_AUTH_TOKEN, "") ?: ""
    }

    /**
     * Get Messaging Service SID
     */
    fun getMessagingServiceSid(): String {
        return encryptedPrefs.getString(KEY_MESSAGING_SERVICE_SID, "") ?: ""
    }

    /**
     * Get Phone Number (From number for SMS)
     */
    fun getPhoneNumber(): String {
        return encryptedPrefs.getString(KEY_PHONE_NUMBER, "") ?: ""
    }

    /**
     * Check if credentials are configured
     */
    fun isConfigured(): Boolean {
        return getAccountSid().isNotEmpty() &&
               getAuthToken().isNotEmpty() &&
               getMessagingServiceSid().isNotEmpty() &&
               getPhoneNumber().isNotEmpty()
    }

    /**
     * Clear all credentials (e.g., on logout)
     */
    fun clearCredentials() {
        encryptedPrefs.edit().apply {
            remove(KEY_ACCOUNT_SID)
            remove(KEY_AUTH_TOKEN)
            remove(KEY_MESSAGING_SERVICE_SID)
            remove(KEY_PHONE_NUMBER)
            apply()
        }
    }
}
