package com.juanweather.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import com.juanweather.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentSettingsBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        try {
            // Set up spinner listener for temperature unit selection
            binding.tempUnitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val unit = if (position == 0) "C" else "F"
                    // TODO: Update settings
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }
            }

            binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
                val theme = if (isChecked) "dark" else "light"
                // TODO: Update theme
            }

            binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
                // TODO: Update notifications
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
