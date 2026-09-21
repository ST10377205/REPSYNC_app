package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class WorkoutSummaryActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_summary)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workoutSummaryMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        displaySummaryData()

        findViewById<MaterialButton>(R.id.btnSaveWorkout).setOnClickListener {
            saveAndExit()
        }

        findViewById<MaterialButton>(R.id.btnDiscardWorkout).setOnClickListener {
            finish()
        }
    }

    private fun displaySummaryData() {
        val seconds = intent.getIntExtra("SESSION_TIME_SECONDS", 0)
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        findViewById<TextView>(R.id.tvSummaryTime).text = String.format("%02d:%02d", minutes, remainingSeconds)

        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)
        
        if (userId != -1) {
            val history = sharedPreferences.getString("user_history_$userId", "") ?: ""
            val lastSession = history.trim().split("\n").lastOrNull()
            if (lastSession != null) {
                val parts = lastSession.split(" • ")
                if (parts.size >= 2) {
                    findViewById<TextView>(R.id.tvSummaryVolume).text = parts[1]
                }
            }
        }
        
        // Placeholder values for sets and PRs (could be passed via intent)
        findViewById<TextView>(R.id.tvSummarySets).text = "12"
        findViewById<TextView>(R.id.tvSummaryPRs).text = "1"
    }

    private fun saveAndExit() {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)

        if (userId != -1) {
            val currentDuration = sharedPreferences.getInt("user_duration_$userId", 0)
            val sessionSeconds = intent.getIntExtra("SESSION_TIME_SECONDS", 0)
            
            sharedPreferences.edit().apply {
                putInt("user_duration_$userId", currentDuration + (sessionSeconds / 60))
                putInt("user_push_day_progress_$userId", (20..100).random()) // Random progress bump
                apply()
            }
            Toast.makeText(this, "Workout saved! Great job.", Toast.LENGTH_SHORT).show()
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
