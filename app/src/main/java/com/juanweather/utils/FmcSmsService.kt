package com.juanweather.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FmcSmsService {

    companion object {
        private const val TAG = "FmcSmsService"
        private const val MESSAGES_PATH = "web/FMCSMS/api/messages.php"
    }

    private val fmcsmsConfig = FmcSmsConfig()

    suspend fun sendSMS(
        toPhoneNumber: String,
        message: String,
        locationUrl: String? = null,
        userName: String = "User"
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!fmcsmsConfig.isConfigured()) {
                    Log.e(TAG, "FMCSMS is not configured")
                    return@withContext false
                }

                var messageToSend = "EMERGENCY ALERT FROM $userName\n\n$message\n\nSent from JuanWeather"
                if (locationUrl != null) {
                    messageToSend += "\n$locationUrl"
                }

                val payload = JSONObject().apply {
                    put("SenderName", fmcsmsConfig.getSenderName())
                    put("ToNumber", toPhoneNumber)
                    put("MessageBody", messageToSend)
                    put("FromNumber", fmcsmsConfig.getFromNumber())
                }

                postMessage(payload.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS", e)
                false
            }
        }
    }

    suspend fun sendSOSToMultipleContacts(
        phoneNumbers: List<String>,
        message: String,
        locationUrl: String? = null,
        userName: String = "User"
    ): Map<String, Boolean> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, Boolean>()
            if (phoneNumbers.isEmpty()) {
                return@withContext results
            }

            phoneNumbers.forEach { phoneNumber ->
                results[phoneNumber] = sendSMS(phoneNumber, message, locationUrl, userName)
            }

            results
        }
    }

    private fun postMessage(jsonBody: String): Boolean {
        val url = URL(resolveMessagesUrl())
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-API-Key", fmcsmsConfig.getApiKey())
            doOutput = true
            connectTimeout = 30000
            readTimeout = 30000
        }

        return try {
            connection.outputStream.use { output ->
                output.write(jsonBody.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val response = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            Log.d(TAG, "FMCSMS HTTP $code")
            if (code !in 200..299) {
                Log.e(TAG, "FMCSMS error response: $response")
            }

            code in 200..299
        } finally {
            connection.disconnect()
        }
    }

    private fun resolveMessagesUrl(): String {
        val baseUrl = fmcsmsConfig.getBaseUrl().trimEnd('/')
        return "$baseUrl/$MESSAGES_PATH"
    }
}
