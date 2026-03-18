package com.juanweather.data.remote

import com.juanweather.data.firebase.FirebaseAuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor for attaching Firebase ID tokens to HTTP requests
 * This enables Retrofit calls to include authentication headers for backend API calls
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get Firebase ID token (blocking operation wrapped in runBlocking)
        val token = runBlocking {
            FirebaseAuthManager.getIdToken()
        }

        return if (token != null) {
            // Add Authorization header with Firebase ID token
            val authorizedRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = chain.proceed(authorizedRequest)

            // Handle token expiration (401 Unauthorized)
            if (response.code == 401) {
                // Log out user if token is invalid/expired
                FirebaseAuthManager.logout()
                response.close()

                // Retry the request with a new token
                val newToken = runBlocking {
                    FirebaseAuthManager.getIdToken()
                }

                if (newToken != null) {
                    val retryRequest = originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer $newToken")
                        .addHeader("Content-Type", "application/json")
                        .build()

                    chain.proceed(retryRequest)
                } else {
                    response
                }
            } else {
                response
            }
        } else {
            // No token available, proceed without auth header
            chain.proceed(originalRequest)
        }
    }
}
