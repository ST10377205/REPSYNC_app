package com.example.myapplication

import org.mindrot.jbcrypt.BCrypt

object PasswordVerifier {
    fun matches(password: String, hash: String): Boolean =
        hash.isNotBlank() && runCatching { BCrypt.checkpw(password, hash) }.getOrDefault(false)
}
