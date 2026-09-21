package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WorkoutSessionActivity : BaseActivity() {
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

        llExerciseContainer = findViewById(R.id.llExerciseContainer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workoutSessionMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
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
            // Default Home Workout
            exerciseList.addAll(listOf("Push-ups", "Air Squats", "Burpees", "Mountain Climbers"))
            categoryList.addAll(List(4) { "Home • Bodyweight" })
        }
    }

    private fun setupUI() {
        findViewById<View>(R.id.toolbar).setOnClickListener {
            // Optional click logic
        }

        findViewById<MaterialButton>(R.id.tvFinishWorkout).setOnClickListener {
            showFinishConfirmation()
        }

        findViewById<MaterialButton>(R.id.btnAddExercise).setOnClickListener {
            startActivity(Intent(this, ExerciseLibraryActivity::class.java))
        }

        findViewById<TextView>(R.id.tvRoutineTitle).text = intent.getStringExtra("ROUTINE_TITLE") ?: "Home Workout"
        
        setupExerciseViews()
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                secondsElapsed++
                val hours = secondsElapsed / 3600
                val minutes = (secondsElapsed % 3600) / 60
                val secs = secondsElapsed % 60
                val timeString = String.format("%02d:%02d:%02d", hours, minutes, secs)
                findViewById<TextView>(R.id.tvTimer).text = String.format("%02d:%02d", minutes, secs)
                findViewById<TextView>(R.id.tvControlTimer).text = timeString
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun showFinishConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Finish Workout?")
            .setMessage("Are you sure you want to complete your session?")
            .setPositiveButton("Finish") { _, _ ->
                finishAndSave()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun finishAndSave() {
        saveWorkoutData()
        val intent = Intent(this, WorkoutSummaryActivity::class.java).apply {
            putExtra("SESSION_TIME_SECONDS", secondsElapsed)
            // Could add more metrics here
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

            addSetRow(container, inflater) // Initial set

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
            val editor = sharedPreferences.edit()
            var totalVolume = 0f
            
            for (view in exerciseViews) {
                val name = view.findViewById<TextView>(R.id.tvExerciseName).text.toString()
                val setsContainer = view.findViewById<LinearLayout>(R.id.llSetsContainer)
                var maxWeight = 0f
                
                for (i in 0 until setsContainer.childCount) {
                    val row = setsContainer.getChildAt(i)
                    val weight = row.findViewById<EditText>(R.id.etWeight).text.toString().toFloatOrNull() ?: 0f
                    val reps = row.findViewById<EditText>(R.id.etReps).text.toString().toIntOrNull() ?: 0
                    if (row.findViewById<CheckBox>(R.id.cbDone).isChecked) {
                        totalVolume += (weight * reps)
                        if (weight > maxWeight) maxWeight = weight
                    }
                }
                
                if (maxWeight > 0) {
                    val pr = sharedPreferences.getFloat("user_pr_$name", 0f)
                    if (maxWeight > pr) editor.putFloat("user_pr_$name", maxWeight)
                }
            }

            // Stats update logic...
            val currentWorkouts = sharedPreferences.getInt("user_workouts_this_week_$userId", 0)
            editor.putInt("user_workouts_this_week_$userId", currentWorkouts + 1)
            
            val history = sharedPreferences.getString("user_history_$userId", "")
            val title = findViewById<TextView>(R.id.tvRoutineTitle).text.toString()
            val newRecord = "$title • ${totalVolume.toInt()} kg • ${System.currentTimeMillis()}\n"
            editor.putString("user_history_$userId", history + newRecord)

            editor.apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
