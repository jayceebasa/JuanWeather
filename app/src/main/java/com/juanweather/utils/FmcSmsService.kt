package com.juanweather.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

                Log.d(TAG, "Sending SMS to $toPhoneNumber: ${payload.toString().take(100)}...")
                return@withContext postMessage(payload.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS to $toPhoneNumber", e)
                return@withContext false
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
                Log.d(TAG, "No phone numbers to send to")
                return@withContext results
            }

            Log.d(TAG, "Starting to send SOS to ${phoneNumbers.size} contact(s)")

            phoneNumbers.forEachIndexed { index, phoneNumber ->
                Log.d(TAG, "Processing contact ${index + 1}/${phoneNumbers.size}: $phoneNumber")
                val result = sendSMS(phoneNumber, message, locationUrl, userName)
                results[phoneNumber] = result
                Log.d(TAG, "Result for $phoneNumber: $result (${index + 1}/${phoneNumbers.size})")

                // Add small delay between messages to ensure proper sequencing
                if (index < phoneNumbers.size - 1) {
                    delay(500)  // 500ms delay between messages
                }
            }

            Log.d(TAG, "Completed sending to all contacts. Results: ${results.size} processed")
            results
        }
    }

    private suspend fun postMessage(jsonBody: String): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(resolveMessagesUrl())
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-API-Key", fmcsmsConfig.getApiKey())
                    doOutput = true
                    connectTimeout = 30000
                    readTimeout = 30000
                }

                connection.outputStream.use { output ->
                    output.write(jsonBody.toByteArray(Charsets.UTF_8))
                    output.flush()  // Ensure data is flushed
                }

                val code = connection.responseCode
                val response = if (code in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                Log.d(TAG, "FMCSMS HTTP $code - Response: $response")
                if (code !in 200..299) {
                    Log.e(TAG, "FMCSMS error response: $response")
                }

                return@withContext code in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "Error posting message to FMCSMS", e)
                return@withContext false
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun resolveMessagesUrl(): String {
        val baseUrl = fmcsmsConfig.getBaseUrl().trimEnd('/')
        return "$baseUrl/$MESSAGES_PATH"
    }
}
