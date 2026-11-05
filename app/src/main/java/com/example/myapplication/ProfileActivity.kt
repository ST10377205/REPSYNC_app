package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        applySystemBarInsets(R.id.profileMain)
        setupToolbarNavigation()
        findViewById<View>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<View>(R.id.btnWorkoutSync).setOnClickListener {
            startActivity(Intent(this, WorkoutSyncActivity::class.java))
        }
        setupLanguagePicker()
        setupThemeSwitch()
        setupLogout()
        setupBottomNavigation(R.id.nav_profile)
        loadUserData()
    }

    private fun setupLanguagePicker() {
        val codes = arrayOf("en", "zu", "af")
        val names = arrayOf("English", "isiZulu", "Afrikaans")
        val current = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: resources.configuration.locales[0].language
        val selected = codes.indexOf(current).coerceAtLeast(0)
        findViewById<TextView>(R.id.tvLanguageValue).text = names[selected]
        findViewById<View>(R.id.btnLanguage).apply {
            setOnClickListener {
                MaterialAlertDialogBuilder(this@ProfileActivity)
                    .setTitle(R.string.choose_language)
                    .setSingleChoiceItems(names, selected) { dialog, which ->
                        dialog.dismiss()
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codes[which]))
                    }
                    .setNegativeButton(R.string.ui_cancel, null)
                    .show()
            }
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
        findViewById<View>(R.id.rowTheme).setOnClickListener { switchTheme.toggle() }

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
            findViewById<MaterialButton>(R.id.btnLogout).isEnabled = false
            AuthSession.clearLocalSession(this)
            lifecycleScope.launch {
                AuthSession.clearGoogleSession(this@ProfileActivity)
                val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
