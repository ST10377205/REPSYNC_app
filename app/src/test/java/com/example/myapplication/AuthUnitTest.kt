package com.example.myapplication

import org.junit.Test
import org.junit.Assert.*
import org.mindrot.jbcrypt.BCrypt

class AuthUnitTest {

    @Test
    fun passwordEncryption_isCorrect() {
        val password = "securePassword123"
        
        // Hash the password
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        
        // Verify the hash is not the same as the plain text
        assertNotEquals(password, hashedPassword)
        
        // Verify the checkpw function works correctly
        assertTrue(BCrypt.checkpw(password, hashedPassword))
    }

    @Test
    fun passwordDecryption_failsForWrongPassword() {
        val password = "mySecretPassword"
        val wrongPassword = "wrongPassword"
        
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        
        // Verify it returns false for incorrect password
        assertFalse(BCrypt.checkpw(wrongPassword, hashedPassword))
    }
}
