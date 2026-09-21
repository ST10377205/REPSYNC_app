package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme()
        super.onCreate(savedInstanceState)
    }

    private fun applyAppTheme() {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", true)
        val targetMode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode)
        }
    }

    protected fun setupBottomNavigation(currentTabId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav) ?: return
        bottomNav.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED
        
        // Update visual selection without triggering the listener
        if (currentTabId != -1) {
            bottomNav.menu.findItem(currentTabId)?.isChecked = true
        } else {
            clearBottomNavSelection(bottomNav)
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == currentTabId) return@setOnItemSelectedListener true
            
            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(this, MainActivity::class.java)
                R.id.nav_progress -> Intent(this, ProgressActivity::class.java)
                R.id.nav_achievements -> Intent(this, AchievementsActivity::class.java)
                R.id.nav_library -> Intent(this, ExerciseLibraryActivity::class.java)
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                else -> null
            }
            
            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(it)
                overridePendingTransition(0, 0)
            }
            true
        }
    }

    private fun clearBottomNavSelection(bottomNav: BottomNavigationView) {
        bottomNav.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNav.menu.size()) {
            bottomNav.menu.getItem(i).isChecked = false
        }
        bottomNav.menu.setGroupCheckable(0, true, true)
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
    }

    private fun updateBottomNavSelection() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav) ?: return
        val currentTabId = when (this) {
            is MainActivity -> R.id.nav_home
            is ProgressActivity -> R.id.nav_progress
            is AchievementsActivity -> R.id.nav_achievements
            is ExerciseLibraryActivity -> R.id.nav_library
            is ProfileActivity -> R.id.nav_profile
            else -> -1
        }
        
        if (currentTabId != -1) {
            bottomNav.menu.findItem(currentTabId)?.isChecked = true
        } else {
            clearBottomNavSelection(bottomNav)
        }
    }
}
