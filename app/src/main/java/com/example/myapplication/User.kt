package com.example.myapplication

import androidx.room.Index
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["googleUid"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val password: String, // BCrypt hash for password accounts; empty for Google-only accounts.
    val googleUid: String? = null
)