package com.juanweather.utils

/**
 * Utility for validating Philippine phone numbers
 */
object PhoneNumberValidator {

    /**
     * Validates if a phone number is a valid Philippine SIM number
     *
     * Accepted formats:
     * - +639XXXXXXXXX (11 digits with +63 prefix)
     * - 09XXXXXXXXX (11 digits starting with 0)
     * - 9XXXXXXXXX (10 digits, assumes +63)
     *
     * @param phoneNumber The phone number to validate
     * @return true if valid Philippine phone number, false otherwise
     */
    fun isValidPhilippineNumber(phoneNumber: String): Boolean {
        // Remove spaces and hyphens
        val cleanNumber = phoneNumber.trim().replace(" ", "").replace("-", "")

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
            else -> false
        }
    }

    /**
     * Formats a Philippine phone number to standard format
     * Converts to 09XXXXXXXXX format
     *
     * @param phoneNumber The phone number to format
     * @return Formatted number or original if invalid
     */
    fun formatPhilippineNumber(phoneNumber: String): String {
        val cleanNumber = phoneNumber.trim().replace(" ", "").replace("-", "")

        return when {
            cleanNumber.startsWith("+63") -> {
                "0" + cleanNumber.removePrefix("+63")
            }
            cleanNumber.startsWith("0") -> cleanNumber
            cleanNumber.startsWith("9") -> "0$cleanNumber"
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
                "Please enter a valid Philippine phone number (09XXXXXXXXX, +639XXXXXXXXX, or 9XXXXXXXXX)"
            else -> "Invalid phone number"
        }
    }
}
