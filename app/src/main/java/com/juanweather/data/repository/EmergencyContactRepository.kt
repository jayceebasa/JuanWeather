package com.juanweather.data.repository

import com.juanweather.data.models.EmergencyContact
import com.juanweather.data.local.SharedPreferencesHelper

class EmergencyContactRepository(
    private val preferencesHelper: SharedPreferencesHelper
) {

    fun getAllContacts(): List<EmergencyContact> {
        return preferencesHelper.getEmergencyContacts()
    }

    fun addContact(contact: EmergencyContact) {
        val contacts = getAllContacts().toMutableList()
        contacts.add(contact)
        preferencesHelper.saveEmergencyContacts(contacts)
    }

    fun deleteContact(contactId: String) {
        val contacts = getAllContacts().toMutableList()
        contacts.removeAll { it.id == contactId }
        preferencesHelper.saveEmergencyContacts(contacts)
    }

    fun updateContact(contact: EmergencyContact) {
        val contacts = getAllContacts().toMutableList()
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contacts[index] = contact
            preferencesHelper.saveEmergencyContacts(contacts)
        }
    }
}
