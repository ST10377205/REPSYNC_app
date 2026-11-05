package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class WorkoutGeneratorActivity : BaseActivity() {
    private var selectedGoal = "Build Muscle"
    private var selectedEquipment = "Home (No Equipment)"
    private var selectedDays = "3 Days / Week"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_generator)

        applySystemBarInsets(R.id.workoutGeneratorMain)
        setupToolbarNavigation()

        setupSelectionListeners()
        
        findViewById<MaterialButton>(R.id.btnGenerateWorkout).setOnClickListener {
            generateWorkout()
        }

        setupBottomNavigation(R.id.nav_home)
    }

    private fun setupSelectionListeners() {
        findViewById<MaterialCardView>(R.id.cvGoal).setOnClickListener {
            val goals = arrayOf("Build Muscle", "Lose Weight", "Improve Endurance", "Flexibility")
            showSelectionDialog(getString(R.string.ui_select_goal), goals) { selection ->
                selectedGoal = selection
                findViewById<TextView>(R.id.tvSelectedGoal).text = localizedText(selection)
            }
        }

        findViewById<MaterialCardView>(R.id.cvEquipment).setOnClickListener {
            val equipments = arrayOf("Home (No Equipment)", "Dumbbells Only", "Full Gym", "Resistance Bands")
            showSelectionDialog(getString(R.string.ui_select_equipment), equipments) { selection ->
                selectedEquipment = selection
                findViewById<TextView>(R.id.tvSelectedEquipment).text = localizedText(selection)
            }
        }

        findViewById<MaterialCardView>(R.id.cvDays).setOnClickListener {
            val days = arrayOf("2 Days / Week", "3 Days / Week", "4 Days / Week", "5+ Days / Week")
            showSelectionDialog(getString(R.string.ui_select_frequency), days) { selection ->
                selectedDays = selection
                findViewById<TextView>(R.id.tvSelectedDays).text = localizedText(selection)
            }
        }
    }

    private fun showSelectionDialog(title: String, items: Array<String>, onSelected: (String) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(items.map { localizedText(it) }.toTypedArray()) { _, which ->
                onSelected(items[which])
            }
            .show()
    }

    private fun generateWorkout() {
        val intent = Intent(this, WorkoutSessionActivity::class.java)
        val exercises = ArrayList<String>()
        val categories = ArrayList<String>()

        // Enhanced home-focused generation logic
        when (selectedEquipment) {
            "Home (No Equipment)" -> {
                exercises.add("Push-ups")
                categories.add("Chest • Bodyweight")
                exercises.add("Bodyweight Squats")
                categories.add("Legs • Bodyweight")
                exercises.add("Plank")
                categories.add("Core • Bodyweight")
                exercises.add("Mountain Climbers")
                categories.add("Cardio • Bodyweight")
                exercises.add("Glute Bridges")
                categories.add("Glutes • Bodyweight")
            }
            "Dumbbells Only" -> {
                exercises.add("Dumbbell Goblet Squats")
                categories.add("Legs • Dumbbells")
                exercises.add("Dumbbell Bench Press")
                categories.add("Chest • Dumbbells")
                exercises.add("Dumbbell Rows")
                categories.add("Back • Dumbbells")
                exercises.add("Lateral Raises")
                categories.add("Shoulders • Dumbbells")
            }
            else -> {
                exercises.add("Barbell Squats")
                categories.add("Legs • Barbell")
                exercises.add("Bench Press")
                categories.add("Chest • Barbell")
                exercises.add("Deadlift")
                categories.add("Back • Barbell")
            }
        }

        intent.putStringArrayListExtra("EXERCISE_NAMES", exercises)
        intent.putStringArrayListExtra("EXERCISE_CATEGORIES", categories)
        intent.putExtra("ROUTINE_TITLE", "$selectedGoal - $selectedEquipment")
        
        Toast.makeText(this, getString(R.string.ui_home_workout_generated), Toast.LENGTH_SHORT).show()
        startActivity(intent)
    }
}
