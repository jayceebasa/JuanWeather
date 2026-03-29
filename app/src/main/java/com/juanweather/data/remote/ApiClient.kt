package com.juanweather.data.remote

import android.util.Log
import com.juanweather.utils.Constants
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

object ApiClient {

    private const val TAG = "ApiClient"
    private const val ENABLE_SSL_BYPASS = true  // Set to false for production

    private fun getOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor()

        // Configure TLS specifications for better SSL/TLS support
        val modernTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
            .build()

        val compatibleTlsSpec = ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
            .build()

        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)  // Firebase Auth token injection
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionSpecs(listOf(modernTlsSpec, compatibleTlsSpec))

        // Development SSL bypass for certificate validation issues
        if (ENABLE_SSL_BYPASS) {
            try {
                val trustAllCerts = arrayOf<X509TrustManager>(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )

                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, trustAllCerts, SecureRandom())
                }

                val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
                builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0])
                    .hostnameVerifier { _, _ -> true }

                Log.d(TAG, "SSL bypass enabled for development (trust all certificates)")
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring SSL bypass: ${e.message}", e)
            }
        }

        return builder.build()
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.WEATHER_BASE_URL)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getWeatherService(): WeatherApiService {
        return getRetrofit().create(WeatherApiService::class.java)
    }
}
