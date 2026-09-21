package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Handle system bar insets for Edge-to-Edge look
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupToolbar()
        setupThemeSwitch()
        setupLogout()
        setupBottomNavigation(R.id.nav_profile)
        loadUserData()
    }

    private fun setupToolbar() {
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)

        if (userId != -1) {
            val db = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                try {
                    val user = db.userDao().getUserById(userId)
                    user?.let {
                        findViewById<TextView>(R.id.tvProfileName).text = it.fullName
                        findViewById<TextView>(R.id.tvProfileEmail).text = it.email
                    }
                } catch (e: Exception) {
                    // Log error or show default
                }
            }
        }
    }

    private fun setupThemeSwitch() {
        val switchTheme = findViewById<SwitchCompat>(R.id.switchTheme)
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", true)
        switchTheme.isChecked = isDarkMode

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (sharedPreferences.getBoolean("dark_mode", true) != isChecked) {
                sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
                
                val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    private fun setupLogout() {
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().remove("current_user_id").apply()
            
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
