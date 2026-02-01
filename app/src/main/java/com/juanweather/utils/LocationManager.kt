package com.juanweather.utils

import android.content.Context
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationManager(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getCurrentLocation(
        onSuccess: (latitude: Double, longitude: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    onError("Location not available")
                }
            }.addOnFailureListener { e ->
                onError(e.message ?: "Unknown error")
            }
        } catch (e: SecurityException) {
            onError("Permission denied: ${e.message}")
        }
    }
}
