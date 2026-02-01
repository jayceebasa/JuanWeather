package com.juanweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanweather.data.models.EmergencyContact
import com.juanweather.data.repository.EmergencyContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmergencyContactViewModel(
    private val repository: EmergencyContactRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            try {
                _contacts.value = repository.getAllContacts()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                repository.addContact(contact)
                loadContacts()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                repository.deleteContact(contactId)
                loadContacts()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun callContact(phoneNumber: String) {
        // Handle phone call
    }
}
