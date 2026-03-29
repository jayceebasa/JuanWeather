package com.juanweather.utils

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.Manifest
import com.google.android.gms.tasks.CancellationTokenSource

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "LocationManager"
    }

    fun getCurrentLocation(
        onSuccess: (latitude: Double, longitude: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Check permissions first
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                onError("Location permissions not granted")
                return
            }

            // Always request fresh location for accuracy
            requestFreshLocation(onSuccess, onError)
        } catch (e: SecurityException) {
            onError("Permission denied: ${e.message}")
        }
    }

    private fun requestFreshLocation(
        onSuccess: (latitude: Double, longitude: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Check permissions
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                onError("Location permissions not granted")
                return
            }

            // Use CurrentLocationRequest with HIGH_ACCURACY and 0 max age for fresh data
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0) // Force fresh location, ignore cache
                .setDurationMillis(10000) // Wait up to 10 seconds for location
                .build()

            // Create a CancellationTokenSource for proper cancellation support
            val cancellationTokenSource = CancellationTokenSource()

            Log.d(TAG, "Requesting fresh location with HIGH_ACCURACY priority")
            fusedLocationClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.d(TAG, "Location obtained: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m")
                        onSuccess(location.latitude, location.longitude)
                    } else {
                        Log.w(TAG, "getCurrentLocation returned null")
                        onError("Could not get current location")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Location request failed: ${exception.message}", exception)
                    onError(exception.message ?: "Failed to get location")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location: ${e.message}", e)
            onError("Error requesting location: ${e.message}")
        }
    }
}
