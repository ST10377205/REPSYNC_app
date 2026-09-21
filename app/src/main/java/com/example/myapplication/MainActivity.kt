package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        setupButtons()
        setupBottomNavigation(R.id.nav_home)
        loadUserData()
        fetchDailyTip()
    }

    private fun setupButtons() {
        findViewById<View>(R.id.btnStartWorkout).setOnClickListener {
            startActivity(Intent(this, WorkoutSessionActivity::class.java).apply {
                putExtra("ROUTINE_TITLE", "Home Full Body")
            })
        }

        findViewById<MaterialCardView>(R.id.btnExerciseLibrary).setOnClickListener {
            startActivity(Intent(this, ExerciseLibraryActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.btnGeneratePlan).setOnClickListener {
            startActivity(Intent(this, WorkoutGeneratorActivity::class.java))
        }

        findViewById<View>(R.id.tvWeeklyActivityDetails).setOnClickListener {
            startActivity(Intent(this, ProgressActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardBodyweight).setOnClickListener {
            startActivity(Intent(this, WorkoutSessionActivity::class.java).apply {
                putExtra("WORKOUT_TYPE", "BODYWEIGHT")
                putExtra("ROUTINE_TITLE", "Bodyweight Power")
            })
        }

        findViewById<MaterialCardView>(R.id.cardYoga).setOnClickListener {
            startActivity(Intent(this, WorkoutSessionActivity::class.java).apply {
                putExtra("WORKOUT_TYPE", "YOGA")
                putExtra("ROUTINE_TITLE", "Morning Flow")
            })
        }

        // Action listener for refreshing tips instantly via REST API
        findViewById<MaterialCardView>(R.id.btnRefreshTips).setOnClickListener {
            fetchDailyTip()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)

        if (userId != -1) {
            val db = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                val user = db.userDao().getUserById(userId)
                user?.let {
                    findViewById<TextView>(R.id.tvUserName).text = it.fullName
                }
            }

            val streak = sharedPreferences.getInt("user_streak_$userId", 0)
            val workoutsThisWeek = sharedPreferences.getInt("user_workouts_this_week_$userId", 0)
            val totalDuration = sharedPreferences.getInt("user_duration_$userId", 0)
            val progress = sharedPreferences.getInt("user_push_day_progress_$userId", 0)

            findViewById<TextView>(R.id.tvStreak).text = streak.toString()
            findViewById<TextView>(R.id.tvThisWeek).text = workoutsThisWeek.toString()
            findViewById<TextView>(R.id.tvDuration).text = "${totalDuration}m"
            
            findViewById<ProgressBar>(R.id.pbPushDayProgress).progress = progress
            findViewById<TextView>(R.id.tvPushDayPercent).text = "$progress%"
            
            findViewById<TextView>(R.id.tvWeeklySubtext).text = if (workoutsThisWeek > 0) 
                "You've crushed $workoutsThisWeek sessions this week!" 
                else "Ready for your first session of the week?"
        }
    }

    private fun fetchDailyTip() {
        val tvDailyTip = findViewById<TextView>(R.id.tvDailyTip)
        tvDailyTip.text = "Loading fresh advice..."
        
        // Connect the active Retrofit service to load real-time tips from the live REST API
        lifecycleScope.launch {
            try {
                val apiService = FitnessApiService.create()
                val response = apiService.getDailyTip()
                tvDailyTip.text = "\"${response.slip.advice}\""
            } catch (e: Exception) {
                // Fallback elegant message if offline
                tvDailyTip.text = "\"Consistency is the secret ingredient to seeing results at home.\""
            }
        }
    }
}
