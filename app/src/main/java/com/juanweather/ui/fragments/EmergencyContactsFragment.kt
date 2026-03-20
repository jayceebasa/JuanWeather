package com.juanweather.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.juanweather.JuanWeatherApp
import com.juanweather.data.models.EmergencyContact
import com.juanweather.databinding.FragmentEmergencyContactsBinding
import com.juanweather.utils.PhoneNumberValidator
import com.juanweather.viewmodel.EmergencyContactViewModel
import kotlinx.coroutines.launch
import java.util.UUID

class EmergencyContactsFragment : Fragment() {

    private var _binding: FragmentEmergencyContactsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EmergencyContactViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentEmergencyContactsBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
        setupUI()
        observeContacts()
    }

    private fun initializeViewModel() {
        try {
            val app = requireActivity().application as JuanWeatherApp
            viewModel = ViewModelProvider(
                this,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return EmergencyContactViewModel(
                            app.hybridEmergencyContactRepository
                        ) as T
                    }
                }
            )[EmergencyContactViewModel::class.java]
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupUI() {
        try {
            binding.addContactButton.setOnClickListener {
                showAddContactDialog()
            }

            binding.sosButton.setOnClickListener {
                // TODO: Trigger SOS alert
            }

            // Handle contact list item clicks
            binding.contactsListView.setOnItemClickListener { _, _, position, _ ->
                val contacts = (binding.contactsListView.adapter as? ArrayAdapter<*>)?.let { adapter ->
                    (0 until adapter.count).map { adapter.getItem(it) as? String }
                }?.filterNotNull() ?: emptyList()

                if (position < contacts.size) {
                    val contactName = contacts[position]
                    showContactOptionsDialog(contactName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contacts.collect { contacts ->
                updateContactsList(contacts)
            }
        }
    }

    private fun updateContactsList(contacts: List<EmergencyContact>) {
        val contactNames = contacts.map { "${it.name} - ${it.phoneNumber}" }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            contactNames
        )
        binding.contactsListView.adapter = adapter
    }

    private fun showAddContactDialog() {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Add Emergency Contact")

        // Create layout for dialog
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        // Name field
        val nameInput = EditText(context).apply {
            hint = "Contact Name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Phone field
        val phoneInput = EditText(context).apply {
            hint = "Phone Number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Relationship field
        val relationshipInput = EditText(context).apply {
            hint = "Relationship (e.g., Mother, Brother)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(nameInput)
        layout.addView(phoneInput)
        layout.addView(relationshipInput)

        builder.setView(layout)

        builder.setPositiveButton("Add") { dialog, _ ->
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val relationship = relationshipInput.text.toString().trim()

            // Validate name and phone
            if (name.isEmpty()) {
                showValidationError("Name cannot be empty")
                return@setPositiveButton
            }

            if (phone.isEmpty()) {
                showValidationError("Phone number cannot be empty")
                return@setPositiveButton
            }

            if (!PhoneNumberValidator.isValidPhilippineNumber(phone)) {
                showValidationError(PhoneNumberValidator.getValidationErrorMessage(phone))
                return@setPositiveButton
            }

            val formattedPhone = PhoneNumberValidator.formatPhilippineNumber(phone)
            val newContact = EmergencyContact(
                id = UUID.randomUUID().toString(),
                name = name,
                phoneNumber = formattedPhone,
                relationship = relationship
            )
            viewModel.addContact(newContact)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showContactOptionsDialog(contactName: String) {
        val context = requireContext()
        val options = arrayOf("Edit", "Delete", "Cancel")

        AlertDialog.Builder(context)
            .setTitle("Contact Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Find the contact and open edit dialog
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.contacts.collect { contacts ->
                                val contact = contacts.find { it.name == contactName.split(" - ")[0] }
                                if (contact != null) {
                                    showEditContactDialog(contact)
                                }
                            }
                        }
                    }
                    1 -> {
                        // Delete the contact
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.contacts.collect { contacts ->
                                val contact = contacts.find { it.name == contactName.split(" - ")[0] }
                                if (contact != null) {
                                    AlertDialog.Builder(context)
                                        .setTitle("Confirm Delete")
                                        .setMessage("Are you sure you want to delete ${contact.name}?")
                                        .setPositiveButton("Delete") { _, _ ->
                                            viewModel.deleteContact(contact.id)
                                        }
                                        .setNegativeButton("Cancel", null)
                                        .show()
                                }
                            }
                        }
                    }
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun showEditContactDialog(contact: EmergencyContact) {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Edit Emergency Contact")

        // Create layout for dialog
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        // Name field
        val nameInput = EditText(context).apply {
            setText(contact.name)
            hint = "Contact Name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Phone field
        val phoneInput = EditText(context).apply {
            setText(contact.phoneNumber)
            hint = "Phone Number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Relationship field
        val relationshipInput = EditText(context).apply {
            setText(contact.relationship)
            hint = "Relationship (e.g., Mother, Brother)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(nameInput)
        layout.addView(phoneInput)
        layout.addView(relationshipInput)

        builder.setView(layout)

        builder.setPositiveButton("Save") { dialog, _ ->
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val relationship = relationshipInput.text.toString().trim()

            // Validate name and phone
            if (name.isEmpty()) {
                showValidationError("Name cannot be empty")
                return@setPositiveButton
            }

            if (phone.isEmpty()) {
                showValidationError("Phone number cannot be empty")
                return@setPositiveButton
            }

            if (!PhoneNumberValidator.isValidPhilippineNumber(phone)) {
                showValidationError(PhoneNumberValidator.getValidationErrorMessage(phone))
                return@setPositiveButton
            }

            val formattedPhone = PhoneNumberValidator.formatPhilippineNumber(phone)
            val updatedContact = contact.copy(
                name = name,
                phoneNumber = formattedPhone,
                relationship = relationship
            )
            viewModel.updateContact(updatedContact)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showValidationError(errorMessage: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Validation Error")
            .setMessage(errorMessage)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


