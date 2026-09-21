package com.example.myapplication

import android.content.Context
import android.content.Intent
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

class SignupActivity : BaseActivity() {
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

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = db.userDao().getUserByEmail(email)
                if (existingUser != null) {
                    Toast.makeText(this@SignupActivity, "Email already registered", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                val newUser = User(fullName = name, email = email, password = hashedPassword)
                val userId = db.userDao().signup(newUser)
                
                val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putInt("current_user_id", userId.toInt()).apply()

                Toast.makeText(this@SignupActivity, "Welcome to RepSync!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                finishAffinity()
            }
        }

        findViewById<TextView>(R.id.tvLoginRedirectFooter).setOnClickListener {
            finish()
        }
    }
}
