package com.example.myapplication

object ValidationUtils {

    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && EMAIL_REGEX.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        // At least 6 characters for basic security
        return password.length >= 6
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }
}
