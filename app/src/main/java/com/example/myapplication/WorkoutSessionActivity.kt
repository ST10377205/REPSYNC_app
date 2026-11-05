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
import androidx.activity.addCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class WorkoutSessionActivity : BaseActivity() {
    private var sessionOwnerId: Int = -1
    private lateinit var llExerciseContainer: LinearLayout
    private var exerciseList = ArrayList<String>()
    private var categoryList = ArrayList<String>()
    private val exerciseViews = mutableListOf<View>()
    
    private var secondsElapsed = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionOwnerId = savedInstanceState?.getInt("session_owner_id")
            ?: getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getInt("current_user_id", -1)
        setContentView(R.layout.activity_workout_session)

        llExerciseContainer = findViewById(R.id.llExerciseContainer)

        applySystemBarInsets(R.id.workoutSessionMain)
        onBackPressedDispatcher.addCallback(this) { showExitConfirmation() }
        setupToolbarNavigation(contentDescriptionRes = R.string.exit_workout) {
            onBackPressedDispatcher.onBackPressed()
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
        findViewById<MaterialButton>(R.id.tvFinishWorkout).setOnClickListener {
            showFinishConfirmation()
        }

        findViewById<MaterialButton>(R.id.btnAddExercise).setOnClickListener {
            startActivity(Intent(this, ExerciseLibraryActivity::class.java))
        }

        findViewById<TextView>(R.id.tvRoutineTitle).text = localizedText(intent.getStringExtra("ROUTINE_TITLE") ?: "Home Workout")
        
        setupExerciseViews()
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                secondsElapsed++
                val hours = secondsElapsed / 3600
                val minutes = (secondsElapsed % 3600) / 60
                val secs = secondsElapsed % 60
                val timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
                findViewById<TextView>(R.id.tvTimer).text =
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
                findViewById<TextView>(R.id.tvControlTimer).text = timeString
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun showFinishConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.ui_finish_workout))
            .setMessage(getString(R.string.ui_are_you_sure_you_want_to_complete_your_session))
            .setPositiveButton(getString(R.string.ui_finish_4)) { _, _ ->
                finishAndSave()
            }
            .setNegativeButton(getString(R.string.ui_cancel), null)
            .show()
    }

    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.ui_exit_workout))
            .setMessage(getString(R.string.ui_your_progress_in_this_session_will_be_lost))
            .setPositiveButton(getString(R.string.ui_exit)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.ui_keep_training), null)
            .show()
    }

    private fun finishAndSave() {
        val result = collectWorkoutResult()
        val intent = Intent(this, WorkoutSummaryActivity::class.java).apply {
            putExtra("WORKOUT_OWNER_ID", sessionOwnerId)
            putExtra("WORKOUT_ID", java.util.UUID.randomUUID().toString())
            putExtra("SESSION_TIME_SECONDS", secondsElapsed)
            putExtra("ROUTINE_TITLE", intent.getStringExtra("ROUTINE_TITLE") ?: "Home Workout")
            putExtra("TOTAL_VOLUME", result.totalVolume)
            putExtra("COMPLETED_SETS", result.completedSets)
            putExtra("PERSONAL_RECORDS", result.personalRecords)
            putStringArrayListExtra("PR_EXERCISES", ArrayList(result.prExercises))
            putExtra("PR_WEIGHTS", result.prWeights.toFloatArray())
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
            exerciseView.findViewById<TextView>(R.id.tvExerciseName).text = localizedText(exerciseList[i])
            exerciseView.findViewById<TextView>(R.id.tvExerciseCategory).text = localizedText(categoryList[i])

            val container = exerciseView.findViewById<LinearLayout>(R.id.llSetsContainer)
            val btnAdd = exerciseView.findViewById<MaterialButton>(R.id.btnAddSet)

            addSetRow(container, inflater) // Initial set

            btnAdd.setOnClickListener {
                addSetRow(container, inflater)
            }

            llExerciseContainer.addView(exerciseView)
            exerciseView.tag = exerciseList[i]
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

    private fun collectWorkoutResult(): WorkoutResult {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        var totalVolume = 0f
        var completedSets = 0
        val prExercises = mutableListOf<String>()
        val prWeights = mutableListOf<Float>()

        for (view in exerciseViews) {
            val name = view.tag as String
            val setsContainer = view.findViewById<LinearLayout>(R.id.llSetsContainer)
            var maxWeight = 0f

            for (i in 0 until setsContainer.childCount) {
                val row = setsContainer.getChildAt(i)
                if (row.findViewById<CheckBox>(R.id.cbDone).isChecked) {
                    val weight = row.findViewById<EditText>(R.id.etWeight).text.toString().toFloatOrNull() ?: 0f
                    val reps = row.findViewById<EditText>(R.id.etReps).text.toString().toIntOrNull() ?: 0
                    completedSets++
                    totalVolume += weight * reps
                    if (weight > maxWeight) maxWeight = weight
                }
            }

            if (maxWeight > sharedPreferences.getFloat("user_pr_$name", 0f)) {
                prExercises.add(name)
                prWeights.add(maxWeight)
            }
        }

        return WorkoutResult(totalVolume, completedSets, prExercises, prWeights)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("session_owner_id", sessionOwnerId)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }

    private data class WorkoutResult(
        val totalVolume: Float,
        val completedSets: Int,
        val prExercises: List<String>,
        val prWeights: List<Float>
    ) {
        val personalRecords: Int
            get() = prExercises.size
    }
}
