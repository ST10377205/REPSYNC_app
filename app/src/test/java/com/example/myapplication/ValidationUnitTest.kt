package com.example.myapplication

import org.junit.Test
import org.junit.Assert.*

class ValidationUnitTest {

    @Test
    fun emailValidation_isCorrect() {
        assertTrue(ValidationUtils.isValidEmail("test@example.com"))
        assertTrue(ValidationUtils.isValidEmail("user.name@domain.co.za"))
        assertFalse(ValidationUtils.isValidEmail("invalid-email"))
        assertFalse(ValidationUtils.isValidEmail("test@"))
        assertFalse(ValidationUtils.isValidEmail("@example.com"))
        assertFalse(ValidationUtils.isValidEmail(""))
    }

    @Test
    fun passwordValidation_isCorrect() {
        assertTrue(ValidationUtils.isValidPassword("123456"))
        assertTrue(ValidationUtils.isValidPassword("strongPassword!"))
        assertFalse(ValidationUtils.isValidPassword("12345"))
        assertFalse(ValidationUtils.isValidPassword(""))
    }

    @Test
    fun nameValidation_isCorrect() {
        assertTrue(ValidationUtils.isValidName("John"))
        assertTrue(ValidationUtils.isValidName("Al"))
        assertFalse(ValidationUtils.isValidName("A"))
        assertFalse(ValidationUtils.isValidName(""))
        assertFalse(ValidationUtils.isValidName("   "))
    }
}
