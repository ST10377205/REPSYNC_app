package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test
import org.mindrot.jbcrypt.BCrypt

class PasswordVerifierTest {
    @Test
    fun googleOnlyAndMalformedHashesDoNotAllowPasswordLoginOrCrash() {
        assertFalse(PasswordVerifier.matches("", ""))
        assertFalse(PasswordVerifier.matches("any password", ""))
        assertFalse(PasswordVerifier.matches("any password", "not-a-bcrypt-hash"))
    }

    @Test
    fun anExistingPasswordHashStillVerifiesOnlyItsOriginalPassword() {
        val hash = BCrypt.hashpw("ExistingPassword123", BCrypt.gensalt(4))
        assertTrue(PasswordVerifier.matches("ExistingPassword123", hash))
        assertFalse(PasswordVerifier.matches("wrong", hash))
    }
}
