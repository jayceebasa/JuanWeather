package com.juanweather.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import android.widget.EditText
import android.widget.LinearLayout
import com.juanweather.JuanWeatherApp
import com.juanweather.data.models.EmergencyContact
import com.juanweather.databinding.FragmentEmergencyContactsBinding
import com.juanweather.viewmodel.EmergencyContactViewModel
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                val newContact = EmergencyContact(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phoneNumber = phone,
                    relationship = relationship
                )
                viewModel.addContact(newContact)
                dialog.dismiss()
            } else {
                // Show validation error
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Validation Error")
                    .setMessage("Please enter both name and phone number")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
