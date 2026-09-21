package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ExerciseLibraryActivity : BaseActivity() {
    
    private val allExercises = listOf(
        Exercise(
            "Push-ups", 
            "Chest • Bodyweight", 
            "Bodyweight", 
            "Place hands shoulder-width apart, lower chest to the floor keeping your core tight and elbows at a 45-degree angle, then push back up fully."
        ),
        Exercise(
            "Bodyweight Squats", 
            "Legs • Bodyweight", 
            "Bodyweight", 
            "Keep feet shoulder-width apart, lower hips back and down like sitting in a chair, keep chest high, and push through your heels to return."
        ),
        Exercise(
            "Plank", 
            "Core • Bodyweight", 
            "Bodyweight", 
            "Place forearms on the ground, align elbows under shoulders, keep body in a perfect straight line from head to heels, and squeeze core tightly."
        ),
        Exercise(
            "Mountain Climbers", 
            "Cardio • Bodyweight", 
            "Bodyweight", 
            "Start in a top push-up position, rapidly drive your knees toward your chest one at a time in an alternating running motion."
        ),
        Exercise(
            "Lunges", 
            "Legs • Bodyweight", 
            "Bodyweight", 
            "Step forward with one leg, lower your hips until both knees are bent at a 90-degree angle, keep torso upright, then push back to start."
        ),
        Exercise(
            "Burpees", 
            "Full Body • Bodyweight", 
            "Bodyweight", 
            "Drop into a squat, jump feet back into a push-up position, do a push-up, jump feet back into a squat, and explosively jump up with hands overhead."
        ),
        Exercise(
            "Dumbbell Curls", 
            "Arms • Dumbbells", 
            "Dumbbell", 
            "Stand tall, keep elbows close to your torso, curl dumbbells up while contracting biceps, squeeze at the top, and lower with absolute control."
        ),
        Exercise(
            "Dumbbell Rows", 
            "Back • Dumbbells", 
            "Dumbbell", 
            "Hinge forward at the hips with flat back, pull dumbbells up toward your waist keeping elbows tucked close to your ribcage, then lower."
        ),
        Exercise(
            "Lateral Raises", 
            "Shoulders • Dumbbells", 
            "Dumbbell", 
            "Stand tall with dumbbells at sides, raise arms out horizontally with a slight bend in the elbows until parallel to the floor, then lower slowly."
        ),
        Exercise(
            "Cat-Cow Stretch", 
            "Back • Flexibility", 
            "Flexibility", 
            "On all fours, inhale to arch your back downward and look up (Cow), then exhale to round your spine up toward the ceiling (Cat)."
        ),
        Exercise(
            "Downward Dog", 
            "Full Body • Flexibility", 
            "Flexibility", 
            "From hands and knees, push hips up and back to form an inverted 'V' shape, press heels down toward the floor, and relax your head."
        ),
        Exercise(
            "Cobra Stretch", 
            "Abs • Flexibility", 
            "Flexibility", 
            "Lie flat face down, place hands under your shoulders, and gently press up to lift your chest off the ground while keeping hips on the floor."
        ),
        Exercise(
            "Bench Press", 
            "Chest • Barbell", 
            "Gym", 
            "Lie flat on the bench, grip the barbell slightly wider than shoulder-width, lower the bar to mid-chest with control, then press it explosively back up."
        ),
        Exercise(
            "Deadlift", 
            "Back • Barbell", 
            "Gym", 
            "Stand with feet mid-bar, hinge at hips to grip the bar, keep spine neutral and chest up, drive upward through your hips and legs to stand upright."
        )
    )

    private val selectedExercises = mutableSetOf<Exercise>()
    private var currentFilter = "All"
    private var currentSearch = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.exerciseLibraryMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        setupSearch()
        setupFilters()
        refreshList()

        findViewById<MaterialButton>(R.id.btnStartSelected).setOnClickListener {
            startWorkout()
        }

        findViewById<TextView>(R.id.tvCreateCustom).setOnClickListener {
            // Logic for custom exercise
        }

        setupBottomNavigation(R.id.nav_library)
    }

    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.etSearchExercise)
        etSearch.addTextChangedListener {
            currentSearch = it.toString().trim().lowercase()
            refreshList()
        }
    }

    private fun setupFilters() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupFilters)
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipBodyweight -> "Bodyweight"
                R.id.chipDumbbell -> "Dumbbell"
                R.id.chipYoga -> "Flexibility"
                else -> "All"
            }
            refreshList()
        }
    }

    private fun refreshList() {
        val container = findViewById<LinearLayout>(R.id.llLibraryContainer)
        val customBtn = findViewById<TextView>(R.id.tvCreateCustom)
        
        container.removeAllViews()
        container.addView(customBtn) // Always keep "Add Custom" at top or bottom

        val filtered = allExercises.filter {
            (currentFilter == "All" || it.type == currentFilter) &&
            (currentSearch.isEmpty() || it.name.lowercase().contains(currentSearch))
        }

        for (ex in filtered) {
            val card = createExerciseCard(ex)
            container.addView(card, container.childCount - 1)
        }
    }

    private fun createExerciseCard(ex: Exercise): MaterialCardView {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
            radius = 16f * resources.displayMetrics.density
            cardElevation = 0f
            strokeWidth = if (selectedExercises.contains(ex)) 
                (2f * resources.displayMetrics.density).toInt() else (1f * resources.displayMetrics.density).toInt()
            strokeColor = if (selectedExercises.contains(ex))
                getColor(R.color.orange_primary) else getColor(R.color.app_surface_variant)
            setCardBackgroundColor(getColor(R.color.app_surface))
            isClickable = true
            isFocusable = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val tvName = TextView(this).apply {
            text = ex.name
            setTextColor(getColor(R.color.text_primary))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val tvCat = TextView(this).apply {
            text = ex.category
            setTextColor(getColor(R.color.text_secondary))
            textSize = 13f
            setPadding(0, 4, 0, 0)
        }

        // Dedicated text button to teach the user how the exercise is done
        val tvGuideLink = TextView(this).apply {
            text = "→ View Guide & Instructions"
            setTextColor(getColor(R.color.orange_primary))
            textSize = 12f
            setPadding(0, 16, 0, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                MaterialAlertDialogBuilder(this@ExerciseLibraryActivity)
                    .setTitle("How to perform: ${ex.name}")
                    .setMessage(ex.instructions)
                    .setPositiveButton("Got it!", null)
                    .show()
            }
        }

        layout.addView(tvName)
        layout.addView(tvCat)
        layout.addView(tvGuideLink)
        card.addView(layout)

        card.setOnClickListener {
            if (selectedExercises.contains(ex)) {
                selectedExercises.remove(ex)
            } else {
                selectedExercises.add(ex)
            }
            updateStartButton()
            refreshList() // Redraw to show selection
        }

        return card
    }

    private fun updateStartButton() {
        val btn = findViewById<MaterialButton>(R.id.btnStartSelected)
        if (selectedExercises.isNotEmpty()) {
            btn.visibility = View.VISIBLE
            btn.text = "Start (${selectedExercises.size})"
        } else {
            btn.visibility = View.GONE
        }
    }

    private fun startWorkout() {
        val intent = Intent(this, WorkoutSessionActivity::class.java).apply {
            putStringArrayListExtra("EXERCISE_NAMES", ArrayList(selectedExercises.map { it.name }))
            putStringArrayListExtra("EXERCISE_CATEGORIES", ArrayList(selectedExercises.map { it.category }))
            putExtra("ROUTINE_TITLE", "Custom Routine")
        }
        startActivity(intent)
    }

    data class Exercise(val name: String, val category: String, val type: String, val instructions: String)
}
