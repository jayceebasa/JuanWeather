package com.juanweather.utils

/**
 * Utility for validating phone numbers (supports both Philippine and international numbers)
 */
object PhoneNumberValidator {

    /**
     * Validates if a phone number is valid (Philippine or international)
     *
     * Accepted formats:
     * - Philippine: +639XXXXXXXXX, 09XXXXXXXXX, or 9XXXXXXXXX
     * - International: +1 to +999 with at least 6 digits after country code
     *
     * @param phoneNumber The phone number to validate
     * @return true if valid phone number, false otherwise
     */
    fun isValidPhilippineNumber(phoneNumber: String): Boolean {
        // Remove spaces and hyphens for validation
        val cleanNumber = phoneNumber.trim().replace(" ", "").replace("-", "")

        // Check if it's a Philippine number
        return when {
            // Format: +639XXXXXXXXX (11 digits with country code)
            cleanNumber.startsWith("+63") -> {
                val digits = cleanNumber.removePrefix("+63")
                digits.length == 10 && digits.all { it.isDigit() } && digits.startsWith("9")
            }
            // Format: 09XXXXXXXXX (11 digits with local 0 prefix)
            cleanNumber.startsWith("09") -> {
                cleanNumber.length == 11 && cleanNumber.all { it.isDigit() }
            }
            // Format: 9XXXXXXXXX (10 digits, assumes +63)
            cleanNumber.startsWith("9") && !cleanNumber.startsWith("0") -> {
                cleanNumber.length == 10 && cleanNumber.all { it.isDigit() }
            }
            // International format: +X to +999 with at least 6 digits after country code
            cleanNumber.startsWith("+") -> {
                val phoneDigitsStart = cleanNumber.indexOfFirst { it.isDigit() }
                if (phoneDigitsStart != -1) {
                    val phoneDigits = cleanNumber.substring(phoneDigitsStart)
                    phoneDigits.all { it.isDigit() } && phoneDigits.length >= 6
                } else {
                    false
                }
            }
            else -> false
        }
    }

    /**
     * Formats a phone number for Twilio SMS (E.164 format)
     * If the number has a country code, use it as-is
     * If it's a Philippine number without +63, add it
     *
     * @param phoneNumber The phone number to format
     * @return Formatted number in E.164 format or original if it's already international
     */
    fun formatPhilippineNumber(phoneNumber: String): String {
        val cleanNumber = phoneNumber.trim().replace(" ", "").replace("-", "")

        return when {
            // Already in international format
            cleanNumber.startsWith("+") -> cleanNumber
            // Philippine format: 09XXXXXXXXX
            cleanNumber.startsWith("09") -> "+63" + cleanNumber.substring(1)
            // Philippine format: 9XXXXXXXXX
            cleanNumber.startsWith("9") -> "+63$cleanNumber"
            // Fallback: assume it's a number and return as-is
            else -> phoneNumber
        }
    }

    /**
     * Gets a user-friendly error message for invalid phone number
     *
     * @param phoneNumber The phone number that failed validation
     * @return Error message describing what's wrong
     */
    fun getValidationErrorMessage(phoneNumber: String): String {
        val cleanNumber = phoneNumber.trim()

        return when {
            cleanNumber.isEmpty() -> "Phone number cannot be empty"
            !cleanNumber.all { it.isDigit() || it == '+' || it == ' ' || it == '-' } ->
                "Phone number contains invalid characters"
            !isValidPhilippineNumber(phoneNumber) ->
                "Please enter a valid phone number (Philippine: 09XXXXXXXXX, +639XXXXXXXXX, or international: +1 to +999 with at least 6 digits)"
            else -> "Invalid phone number"
        }
    }
}
