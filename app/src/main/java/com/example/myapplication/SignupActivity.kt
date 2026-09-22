package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

/**
 * SignupActivity - Handles new user registration.
 * Includes validation for name (letters only) and email format.
 */
class SignupActivity : BaseActivity() {

    private val TAG = "SignupActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        
        val db = AppDatabase.getDatabase(this)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Input Validation using ValidationUtils
            if (!ValidationUtils.isValidName(name)) {
                etName.error = "Name must contain only letters and be at least 2 characters"
                return@setOnClickListener
            }

            if (!ValidationUtils.isValidEmail(email)) {
                etEmail.error = "Please enter a valid email address (e.g. name@gmail.com)"
                return@setOnClickListener
            }

            if (!ValidationUtils.isValidPassword(password)) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                Log.d(TAG, "Starting signup process for: $email")
                
                // 1. Check local DB
                var existingUser = db.userDao().getUserByEmail(email)
                
                // 2. Check Firebase Cloud to prevent duplicate emails across devices
                try {
                    val apiService = WorkoutApiService.create()
                    val cloudUsersMap = apiService.getAllUsers()
                    if (cloudUsersMap != null) {
                        val cloudUserExists = cloudUsersMap.values.any { it.email.equals(email, ignoreCase = true) }
                        if (cloudUserExists) {
                            Log.w(TAG, "Signup failed: Email exists in cloud database")
                            Toast.makeText(this@SignupActivity, "Email already registered in cloud", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Cloud email check failed, proceeding with local check", e)
                }

                if (existingUser != null) {
                    Log.w(TAG, "Signup failed: Email exists in local database")
                    Toast.makeText(this@SignupActivity, "Email already registered locally", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 3. Register user locally
                val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                val newUser = User(fullName = name, email = email, password = hashedPassword)
                val userId = db.userDao().signup(newUser)
                
                // 4. Back up account to Firebase Realtime Database cloud via REST API
                try {
                    val apiService = WorkoutApiService.create()
                    val userToUpload = newUser.copy(id = userId.toInt())
                    apiService.uploadUser(userToUpload)
                    Log.i(TAG, "User profile successfully backed up to Firebase Cloud")
                } catch (e: Exception) {
                    Log.e(TAG, "Cloud backup failed, user saved locally only", e)
                }

                val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putInt("current_user_id", userId.toInt()).apply()

                Toast.makeText(this@SignupActivity, "Welcome to RepSync! Synced with Cloud.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                finishAffinity()
            }
        }

        findViewById<TextView>(R.id.tvLoginRedirectFooter).setOnClickListener {
            finish()
        }
    }
}
