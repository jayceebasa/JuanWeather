package com.juanweather.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Base64
import android.util.Log

class TwilioSmsService(private val context: Context) {

    companion object {
        private const val TAG = "TwilioSmsService"
    }

    private val twilioConfig = TwilioConfig(context)

    /**
     * Send an SOS SMS to a phone number using Twilio Messaging Service
     * This bypasses A2P 10DLC registration requirement
     *
     * @param toPhoneNumber Recipient phone number (should be in E.164 format)
     * @param message Message content
     * @param locationUrl Optional location URL to include
     * @param messagingServiceSid Your Twilio Messaging Service SID (MG...)
     * @return true if successful, false otherwise
     */
    suspend fun sendSMS(
        toPhoneNumber: String,
        message: String,
        locationUrl: String? = null,
        messagingServiceSid: String = ""
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val accountSid = twilioConfig.getAccountSid()
                val authToken = twilioConfig.getAuthToken()
                val phoneNumber = twilioConfig.getPhoneNumber()

                if (accountSid.isEmpty() || authToken.isEmpty()) {
                    Log.e(TAG, "Twilio credentials not configured (Account SID or Auth Token missing)")
                    return@withContext false
                }

                if (messagingServiceSid.isEmpty()) {
                    Log.e(TAG, "Messaging Service SID not provided")
                    return@withContext false
                }

                if (phoneNumber.isEmpty()) {
                    Log.e(TAG, "Twilio phone number not configured. Please restart the app after configuration.")
                    return@withContext false
                }

                Log.d(TAG, "Sending SMS - From: $phoneNumber, To: $toPhoneNumber, Service: $messagingServiceSid")

                val messageToSend = if (locationUrl != null) {
                    "$message\n\nLocation: $locationUrl"
                } else {
                    message
                }

                // Use Messaging Service endpoint instead of direct SMS
                val url = URL("https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json")
                val urlConnection = url.openConnection() as java.net.HttpURLConnection

                // Set up authentication
                val auth = "$accountSid:$authToken"
                val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())

                urlConnection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Basic $encodedAuth")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                    doOutput = true
                }

                // Prepare request body - use MessagingServiceSid AND From number
                val postData = "MessagingServiceSid=${messagingServiceSid.encodeURLComponent()}" +
                        "&From=${phoneNumber.encodeURLComponent()}" +
                        "&To=${toPhoneNumber.encodeURLComponent()}" +
                        "&Body=${messageToSend.encodeURLComponent()}"

                urlConnection.outputStream.use { os ->
                    os.write(postData.toByteArray(Charsets.UTF_8))
                }

                // Read response
                val responseCode = urlConnection.responseCode
                val response = if (responseCode == 201) {
                    urlConnection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    urlConnection.errorStream.bufferedReader().use { it.readText() }
                }

                Log.d(TAG, "Response code: $responseCode")
                Log.d(TAG, "Response: $response")

                return@withContext responseCode == 201
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS", e)
                return@withContext false
            }
        }
    }

    /**
     * Send SOS to multiple contacts using Messaging Service
     * @param phoneNumbers List of recipient phone numbers
     * @param message Message content
     * @param locationUrl Optional location URL
     * @param messagingServiceSid Your Twilio Messaging Service SID
     * @return Map of phone number to success status
     */
    suspend fun sendSOSToMultipleContacts(
        phoneNumbers: List<String>,
        message: String,
        locationUrl: String? = null,
        messagingServiceSid: String = ""
    ): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()

        phoneNumbers.forEach { phoneNumber ->
            results[phoneNumber] = sendSMS(phoneNumber, message, locationUrl, messagingServiceSid)
        }

        return results
    }
}

private fun String.encodeURLComponent(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}
