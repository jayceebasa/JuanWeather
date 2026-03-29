package com.juanweather.utils

import android.util.Log
import com.juanweather.BuildConfig

/**
 * Build-time FMCSMS configuration holder.
 * Values come from local.properties via BuildConfig fields.
 */
class FmcSmsConfig {

    companion object {
        private const val TAG = "FmcSmsConfig"
    }

    fun getApiKey(): String {
        val key = BuildConfig.FMCSMS_API_KEY
        Log.d(TAG, "API Key loaded: ${if (key.isNotBlank()) "✓ Present (${key.length} chars)" else "✗ Empty"}")
        return key
    }

    fun getBaseUrl(): String {
        val url = BuildConfig.FMCSMS_BASE_URL
        Log.d(TAG, "Base URL loaded: $url")
        return url
    }

    fun getSenderName(): String {
        val name = BuildConfig.FMCSMS_SENDER_NAME
        Log.d(TAG, "Sender Name loaded: $name")
        return name
    }

    fun getFromNumber(): String {
        val number = BuildConfig.FMCSMS_FROM_NUMBER
        Log.d(TAG, "From Number loaded: ${if (number.isNotBlank()) "✓ Present ($number)" else "✗ Empty"}")
        return number
    }

    fun isConfigured(): Boolean {
        val apiKey = getApiKey()
        val baseUrl = getBaseUrl()
        val fromNumber = getFromNumber()

        val isConfigured = apiKey.isNotBlank() &&
            baseUrl.isNotBlank() &&
            fromNumber.isNotBlank()

        Log.d(TAG, "Configuration Status: ${if (isConfigured) "✓ CONFIGURED" else "✗ NOT CONFIGURED"}")
        Log.d(TAG, "  - API Key: ${if (apiKey.isNotBlank()) "✓" else "✗"}")
        Log.d(TAG, "  - Base URL: ${if (baseUrl.isNotBlank()) "✓" else "✗"}")
        Log.d(TAG, "  - From Number: ${if (fromNumber.isNotBlank()) "✓" else "✗"}")

        return isConfigured
    }
}
