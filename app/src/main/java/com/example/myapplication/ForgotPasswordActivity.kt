package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

class ForgotPasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotPasswordMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<TextInputEditText>(R.id.etForgotEmail)
        val etNewPassword = findViewById<TextInputEditText>(R.id.etNewPassword)
        val btnReset = findViewById<MaterialButton>(R.id.btnResetPassword)
        val tvBack = findViewById<TextView>(R.id.tvBackToLogin)

        val db = AppDatabase.getDatabase(this)

        btnReset.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()

            if (email.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = db.userDao().getUserByEmail(email)
                if (user != null) {
                    // Encrypt the new password
                    val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
                    val updatedUser = user.copy(password = hashedPassword)
                    
                    db.userDao().updateUser(updatedUser)
                    
                    Toast.makeText(this@ForgotPasswordActivity, "Password updated successfully!", Toast.LENGTH_LONG).show()
                    finish() // Return to Login
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, "Email not found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvBack.setOnClickListener {
            finish()
        }
    }
}
