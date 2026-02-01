package com.juanweather.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.juanweather.data.models.EmergencyContact

class SharedPreferencesHelper(context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("juanweather_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Emergency Contacts
    fun getEmergencyContacts(): List<EmergencyContact> {
        val json = sharedPref.getString("emergency_contacts", "[]") ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<List<EmergencyContact>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveEmergencyContacts(contacts: List<EmergencyContact>) {
        val json = gson.toJson(contacts)
        sharedPref.edit().putString("emergency_contacts", json).apply()
    }

    // Favorite Locations
    fun saveFavoriteLocations(locations: List<String>) {
        val json = gson.toJson(locations)
        sharedPref.edit().putString("favorite_locations", json).apply()
    }

    fun getFavoriteLocations(): List<String> {
        val json = sharedPref.getString("favorite_locations", "[]") ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    // Current Location
    fun saveCurrentLocation(locationName: String) {
        sharedPref.edit().putString("current_location", locationName).apply()
    }

    fun getCurrentLocation(): String {
        return sharedPref.getString("current_location", "") ?: ""
    }
}
