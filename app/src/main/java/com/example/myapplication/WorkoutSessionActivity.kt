package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.TimeUnit

/**
 * WorkoutSessionActivity - Manages the active training session.
 * Features a live timer and dynamic set tracking.
 */
class WorkoutSessionActivity : BaseActivity() {
    private val TAG = "WorkoutSession"
    private lateinit var llExerciseContainer: LinearLayout
    private var exerciseList = ArrayList<String>()
    private var categoryList = ArrayList<String>()
    private val exerciseViews = mutableListOf<View>()
    
    private var secondsElapsed = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_session)
        Log.d(TAG, "onCreate: Session started")

        llExerciseContainer = findViewById(R.id.llExerciseContainer)

        // Handle System Insets (Status bar and Navigation bar)
        // Fixed: Added bottom inset padding to ensure the "Finish" button isn't hidden behind the phone's navigation buttons
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workoutSessionMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        loadExercisesFromIntent()
        setupUI()
        startTimer()
        
        setupBottomNavigation(-1)
    }

    private fun loadExercisesFromIntent() {
        val names = intent.getStringArrayListExtra("EXERCISE_NAMES")
        val categories = intent.getStringArrayListExtra("EXERCISE_CATEGORIES")
        val workoutType = intent.getStringExtra("WORKOUT_TYPE")

        if (names != null && categories != null) {
            exerciseList.addAll(names)
            categoryList.addAll(categories)
        } else if (workoutType == "BODYWEIGHT") {
            exerciseList.addAll(listOf("Push-ups", "Bodyweight Squats", "Plank", "Lunges", "Crunches"))
            categoryList.addAll(listOf("Chest", "Legs", "Core", "Legs", "Core").map { "$it • Bodyweight" })
        } else if (workoutType == "YOGA") {
            exerciseList.addAll(listOf("Child's Pose", "Downward Dog", "Cobra Stretch", "Cat-Cow", "Warrior II"))
            categoryList.addAll(List(5) { "Flexibility • Yoga" })
        } else {
            exerciseList.addAll(listOf("Push-ups", "Air Squats", "Burpees", "Mountain Climbers"))
            categoryList.addAll(List(4) { "Home • Bodyweight" })
        }
    }

    private fun setupUI() {
        val btnFinish = findViewById<MaterialButton>(R.id.tvFinishWorkout)
        btnFinish.setOnClickListener {
            Log.d(TAG, "Finish button clicked")
            showFinishConfirmation()
        }

        findViewById<MaterialButton>(R.id.btnAddExercise).setOnClickListener {
            startActivity(Intent(this, ExerciseLibraryActivity::class.java))
        }

        val routineTitle = intent.getStringExtra("ROUTINE_TITLE") ?: "Home Workout"
        findViewById<TextView>(R.id.tvRoutineTitle).text = routineTitle
        
        setupExerciseViews()
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                secondsElapsed++
                val minutes = (secondsElapsed % 3600) / 60
                val secs = secondsElapsed % 60
                findViewById<TextView>(R.id.tvTimer).text = String.format("%02d:%02d", minutes, secs)
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun showFinishConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Finish Workout?")
            .setMessage("Are you sure you want to save your progress and complete the session?")
            .setPositiveButton("Finish") { _, _ ->
                Log.d(TAG, "User confirmed finish")
                finishAndSave()
            }
            .setNegativeButton("Keep Going", null)
            .show()
    }

    private fun finishAndSave() {
        Log.d(TAG, "Executing finishAndSave process")
        saveWorkoutData()
        val routineTitle = findViewById<TextView>(R.id.tvRoutineTitle).text.toString()
        val intent = Intent(this, WorkoutSummaryActivity::class.java).apply {
            putExtra("SESSION_TIME_SECONDS", secondsElapsed)
            putExtra("ROUTINE_TITLE", routineTitle)
        }
        startActivity(intent)
        finish()
    }

    private fun setupExerciseViews() {
        llExerciseContainer.removeAllViews()
        exerciseViews.clear()
        val inflater = LayoutInflater.from(this)

        for (i in exerciseList.indices) {
            val exerciseView = inflater.inflate(R.layout.item_workout_exercise, llExerciseContainer, false)
            exerciseView.findViewById<TextView>(R.id.tvExerciseName).text = exerciseList[i]
            exerciseView.findViewById<TextView>(R.id.tvExerciseCategory).text = categoryList[i]

            val container = exerciseView.findViewById<LinearLayout>(R.id.llSetsContainer)
            val btnAdd = exerciseView.findViewById<MaterialButton>(R.id.btnAddSet)

            addSetRow(container, inflater)

            btnAdd.setOnClickListener {
                addSetRow(container, inflater)
            }

            llExerciseContainer.addView(exerciseView)
            exerciseViews.add(exerciseView)
        }
    }

    private fun addSetRow(container: LinearLayout, inflater: LayoutInflater) {
        val setView = inflater.inflate(R.layout.item_workout_set, container, false)
        setView.findViewById<TextView>(R.id.tvSetNumber).text = (container.childCount + 1).toString()
        
        setView.findViewById<CheckBox>(R.id.cbDone).setOnCheckedChangeListener { _, isChecked ->
            setView.alpha = if (isChecked) 0.6f else 1.0f
        }
        
        container.addView(setView)
    }

    private fun saveWorkoutData() {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)
        
        if (userId != -1) {
            Log.d(TAG, "Saving workout data for user: $userId")
            val editor = sharedPreferences.edit()
            var totalVolume = 0f
            
            for (view in exerciseViews) {
                val name = view.findViewById<TextView>(R.id.tvExerciseName).text.toString()
                val setsContainer = view.findViewById<LinearLayout>(R.id.llSetsContainer)
                var maxWeight = 0f
                
                for (i in 0 until setsContainer.childCount) {
                    val row = setsContainer.getChildAt(i)
                    val weightStr = row.findViewById<EditText>(R.id.etWeight).text.toString()
                    val repsStr = row.findViewById<EditText>(R.id.etReps).text.toString()
                    
                    val weight = weightStr.toFloatOrNull() ?: 0f
                    val reps = repsStr.toIntOrNull() ?: 0
                    
                    if (row.findViewById<CheckBox>(R.id.cbDone).isChecked) {
                        totalVolume += (weight * reps)
                        if (weight > maxWeight) maxWeight = weight
                    }
                }
                
                if (maxWeight > 0) {
                    val pr = sharedPreferences.getFloat("user_pr_$name", 0f)
                    if (maxWeight > pr) {
                        Log.i(TAG, "New PR for $name: $maxWeight")
                        editor.putFloat("user_pr_$name", maxWeight)
                    }
                }
            }

            val history = sharedPreferences.getString("user_history_$userId", "") ?: ""
            val title = findViewById<TextView>(R.id.tvRoutineTitle).text.toString()
            val now = System.currentTimeMillis()
            val newRecord = "$title • ${totalVolume.toInt()} kg • $now\n"
            editor.putString("user_history_$userId", history + newRecord)

            // Update stats: Workouts this week
            val workoutsThisWeek = sharedPreferences.getInt("user_workouts_this_week_$userId", 0)
            editor.putInt("user_workouts_this_week_$userId", workoutsThisWeek + 1)

            // Update stats: Streak logic
            val lastWorkoutTime = sharedPreferences.getLong("user_last_workout_time_$userId", 0L)
            var currentStreak = sharedPreferences.getInt("user_streak_$userId", 0)

            if (lastWorkoutTime == 0L) {
                // First session ever starts the streak at 1
                currentStreak = 1
            } else {
                val lastDay = TimeUnit.MILLISECONDS.toDays(lastWorkoutTime)
                val today = TimeUnit.MILLISECONDS.toDays(now)

                if (today == lastDay + 1) {
                    // Consecutive day
                    currentStreak += 1
                } else if (today > lastDay) {
                    // Streak broken (missed at least one day)
                    currentStreak = 1
                }
                // If today == lastDay, currentStreak remains the same
            }
            
            editor.putInt("user_streak_$userId", currentStreak)
            editor.putLong("user_last_workout_time_$userId", now)

            editor.apply()
            Log.d(TAG, "Local storage updated: Streak=$currentStreak, Weekly Count=${workoutsThisWeek + 1}")
        } else {
            Log.e(TAG, "Failed to save: User not logged in")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
