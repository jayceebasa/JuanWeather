package com.juanweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.models.EmergencyContact
import com.juanweather.data.repository.HybridEmergencyContactRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmergencyContactViewModel(
    private val repository: HybridEmergencyContactRepository
) : ViewModel() {

    val contacts: StateFlow<List<EmergencyContact>> = repository.getAllContacts() as StateFlow<List<EmergencyContact>>

    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                repository.addContact(contact)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                repository.deleteContact(contactId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                repository.updateContact(contact)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun callContact(phoneNumber: String) {
        // Handle phone call
    }
}
