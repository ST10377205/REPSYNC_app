package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect

class MainActivity : BaseActivity() {
    private var workoutObserver: Job? = null
    private var exerciseRequest: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        applySystemBarInsets(R.id.main)

        setupButtons()
        setupBottomNavigation(R.id.nav_home)
        loadUserData()
        fetchExerciseSpotlight()
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

        // Refresh the fitness database spotlight.
        findViewById<MaterialCardView>(R.id.btnRefreshTips).setOnClickListener {
            fetchExerciseSpotlight(refresh = true)
        }
    }

    override fun onResume() {
        super.onResume()
        observeWorkoutChanges()
        loadUserData()
    }


    override fun onPause() {
        workoutObserver?.cancel()
        super.onPause()
    }

    private fun observeWorkoutChanges() {
        workoutObserver?.cancel()
        val userId = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        if (userId < 0) return
        workoutObserver = lifecycleScope.launch {
            val repository = WorkoutRepository(this@MainActivity)
            repository.ensureLegacyImported(userId)
            repository.observe(userId).collect {
                repository.refreshMetrics(userId)
                loadUserData()
            }
        }
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
                getString(R.string.weekly_sessions, workoutsThisWeek)
                else getString(R.string.first_session)
        }
    }

    private fun fetchExerciseSpotlight(refresh: Boolean = false) {
        exerciseRequest?.cancel()
        val text = findViewById<TextView>(R.id.tvDailyTip)
        text.text = getString(R.string.online_loading)
        text.setOnClickListener { startActivity(Intent(this, OnlineExercisesActivity::class.java)) }
        exerciseRequest = lifecycleScope.launch {
            try {
                val page = OnlineExerciseRepository(this@MainActivity).loadPage(refresh = refresh)
                val exercise = page.exercises.randomOrNull()
                text.text = if (exercise == null) getString(R.string.online_open)
                    else getString(R.string.online_preview, exercise.name, exercise.category) +
                        "\n" + getString(if (page.saved) R.string.online_preview_saved else R.string.online_preview_live)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                text.text = getString(R.string.online_open)
            }
        }
    }
}
