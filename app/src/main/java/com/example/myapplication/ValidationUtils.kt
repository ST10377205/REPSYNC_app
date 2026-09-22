package com.example.myapplication

import android.util.Patterns

/**
 * ValidationUtils - Utility class for validating user input fields.
 */
object ValidationUtils {

    /**
     * Validates if the email is a proper email format.
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validates if the password meets security requirements (min 6 characters).
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Validates if the name contains only letters and spaces, and is at least 2 characters.
     */
    fun isValidName(name: String): Boolean {
        val nameRegex = "^[a-zA-Z\\s]+$".toRegex()
        return name.trim().length >= 2 && nameRegex.matches(name)
    }
}
