package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

class SignupActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        applySystemBarInsets(R.id.signupMain)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        
        val db = AppDatabase.getDatabase(this)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.ui_please_fill_in_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!ValidationUtils.isValidName(name)) {
                etName.error = getString(R.string.validation_name)
                return@setOnClickListener
            }
            if (!ValidationUtils.isValidEmail(email)) {
                etEmail.error = getString(R.string.validation_email)
                return@setOnClickListener
            }
            if (!ValidationUtils.isValidPassword(password)) {
                etPassword.error = getString(R.string.ui_password_must_be_at_least_6_characters)
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = db.userDao().getUserByEmail(email)
                if (existingUser != null) {
                    Toast.makeText(this@SignupActivity, getString(R.string.ui_email_already_registered), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                val newUser = User(fullName = name, email = email, password = hashedPassword)
                val userId = db.userDao().signup(newUser)
                
                val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
                AuthSession.beginLocal(this@SignupActivity, userId.toInt())

                Toast.makeText(this@SignupActivity, getString(R.string.ui_welcome_to_repsync), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                finishAffinity()
            }
        }

        findViewById<TextView>(R.id.tvLoginRedirectFooter).setOnClickListener {
            finish()
        }
    }
}
