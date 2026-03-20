package com.juanweather.utils

import android.content.Context
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.Manifest
import com.google.android.gms.tasks.CancellationTokenSource

class LocationManager(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val context = context

    fun getCurrentLocation(
        onSuccess: (latitude: Double, longitude: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // First try lastLocation (faster, if available)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    // If lastLocation is null, request a fresh location
                    requestFreshLocation(onSuccess, onError)
                }
            }.addOnFailureListener { e ->
                // If lastLocation fails, request a fresh location
                requestFreshLocation(onSuccess, onError)
            }
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

            // Use CurrentLocationRequest for the newer API
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0)
                .build()

            // Create a CancellationTokenSource for proper cancellation support
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        onSuccess(location.latitude, location.longitude)
                    } else {
                        onError("Could not get current location")
                    }
                }
                .addOnFailureListener { exception ->
                    onError(exception.message ?: "Failed to get location")
                }
        } catch (e: Exception) {
            onError("Error requesting location: ${e.message}")
        }
    }
}
