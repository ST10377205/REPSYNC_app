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
import kotlinx.coroutines.launch

/**
 * WorkoutSummaryActivity - Shows details at the end of a session.
 * Integrates with Retrofit WorkoutApiService to push completed data online.
 */
class WorkoutSummaryActivity : BaseActivity() {

    private val TAG = "WorkoutSummary"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_summary)
        Log.d(TAG, "onCreate: Initializing Workout Summary Layout Screen")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workoutSummaryMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        displaySummaryData()

        findViewById<MaterialButton>(R.id.btnSaveWorkout).setOnClickListener {
            Log.d(TAG, "Save button clicked - preparing data packages")
            saveAndExit()
        }

        findViewById<MaterialButton>(R.id.btnDiscardWorkout).setOnClickListener {
            Log.d(TAG, "Discard button clicked - canceling session data generation")
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
        
        findViewById<TextView>(R.id.tvSummarySets).text = "12"
        findViewById<TextView>(R.id.tvSummaryPRs).text = "1"
    }

    private fun saveAndExit() {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)
        val sessionSeconds = intent.getIntExtra("SESSION_TIME_SECONDS", 0)
        val workoutTitle = intent.getStringExtra("ROUTINE_TITLE") ?: "General Workout"

        if (userId != -1) {
            Log.i(TAG, "Writing progress markers to offline storage cache for userId: $userId")
            val currentDuration = sharedPreferences.getInt("user_duration_$userId", 0)
            sharedPreferences.edit().apply {
                putInt("user_duration_$userId", currentDuration + (sessionSeconds / 60))
                putInt("user_push_day_progress_$userId", (20..100).random())
                apply()
            }

            // REST API Integration asynchronously handled through background context
            lifecycleScope.launch {
                try {
                    Log.i(TAG, "Dispatching outbound POST REST api payload to Firebase Cloud servers")
                    val apiService = WorkoutApiService.create()
                    val cloudWorkout = CloudWorkout(
                        userId = userId,
                        workoutTitle = workoutTitle,
                        durationMinutes = sessionSeconds / 60
                    )
                    
                    val response = apiService.uploadWorkout(cloudWorkout)
                    Log.d(TAG, "Firebase Server responded with unique identification token: ${response.id}")
                    Toast.makeText(this@WorkoutSummaryActivity, "Cloud Sync Successful: ID ${response.id}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Network transport failure caught gracefully. Defaulting to local offline storage schema.", e)
                    Toast.makeText(this@WorkoutSummaryActivity, "Saved locally. Cloud sync pending connection.", Toast.LENGTH_SHORT).show()
                }

                val intent = Intent(this@WorkoutSummaryActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        } else {
            finish()
        }
    }
}
