package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

/**
 * LoginActivity - Handles user authentication.
 * Validates email format before attempting to sign in.
 */
class LoginActivity : BaseActivity() {

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUpRedirect = findViewById<TextView>(R.id.tvSignUpRedirect)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        val db = AppDatabase.getDatabase(this)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Input Validation
            if (!ValidationUtils.isValidEmail(email)) {
                etEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password cannot be empty"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                Log.d(TAG, "Attempting login for: $email")
                
                // 1. Try local login first
                var user = db.userDao().getUserByEmail(email)
                
                // 2. If not found locally, try to find in Firebase Cloud (Sync for new devices)
                if (user == null) {
                    try {
                        Log.i(TAG, "User not found locally, checking Firebase Cloud...")
                        val apiService = WorkoutApiService.create()
                        val cloudUsersMap = apiService.getAllUsers()
                        if (cloudUsersMap != null) {
                            val cloudUser = cloudUsersMap.values.find { it.email.equals(email, ignoreCase = true) }
                            if (cloudUser != null) {
                                Log.i(TAG, "User found in cloud, synchronizing to local database")
                                // Save to local DB so future logins are faster/offline
                                db.userDao().signup(cloudUser)
                                user = cloudUser
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Cloud login check failed", e)
                    }
                }

                if (user != null && BCrypt.checkpw(password, user.password)) {
                    val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putInt("current_user_id", user.id).apply()

                    Log.i(TAG, "Login successful for user: ${user.email}")
                    Toast.makeText(this@LoginActivity, "Login Successful (Cloud Synced)", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.w(TAG, "Login failed: Invalid credentials for $email")
                    Toast.makeText(this@LoginActivity, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvSignUpRedirect.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}
