package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class WorkoutSummaryActivity : BaseActivity() {
    private lateinit var workoutId: String
    private var completedAt: Long = 0
    private var ownerId: Int = -1
    private var saving = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workoutId = savedInstanceState?.getString("workout_id") ?: intent.getStringExtra("WORKOUT_ID") ?: java.util.UUID.randomUUID().toString()
        completedAt = savedInstanceState?.getLong("completed_at") ?: System.currentTimeMillis()
        ownerId = savedInstanceState?.getInt("workout_owner") ?: intent.getIntExtra("WORKOUT_OWNER_ID",
            getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getInt("current_user_id", -1))
        setContentView(R.layout.activity_workout_summary)

        applySystemBarInsets(R.id.workoutSummaryMain)
        onBackPressedDispatcher.addCallback(this) {
            if (!saving) showDiscardConfirmation()
        }
        setupToolbarNavigation { onBackPressedDispatcher.onBackPressed() }

        displaySummaryData()

        findViewById<MaterialButton>(R.id.btnSaveWorkout).setOnClickListener {
            saveAndExit()
        }

        findViewById<MaterialButton>(R.id.btnDiscardWorkout).setOnClickListener {
            showDiscardConfirmation()
        }
    }

    private fun displaySummaryData() {
        val seconds = intent.getIntExtra("SESSION_TIME_SECONDS", 0)
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        findViewById<TextView>(R.id.tvSummaryTime).text =
            String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
        findViewById<TextView>(R.id.tvSummaryVolume).text =
            "${intent.getFloatExtra("TOTAL_VOLUME", 0f).toInt()} kg"
        findViewById<TextView>(R.id.tvSummarySets).text =
            intent.getIntExtra("COMPLETED_SETS", 0).toString()
        findViewById<TextView>(R.id.tvSummaryPRs).text =
            intent.getIntExtra("PERSONAL_RECORDS", 0).toString()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("workout_id", workoutId)
        outState.putLong("completed_at", completedAt)
        outState.putInt("workout_owner", ownerId)
        super.onSaveInstanceState(outState)
    }

    private fun saveAndExit() {
        if (saving) return
        val preferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        if (ownerId < 1 || preferences.getInt("current_user_id", -1) != ownerId) {
            Toast.makeText(this, R.string.workout_account_changed, Toast.LENGTH_LONG).show()
            return
        }
        saving = true
        findViewById<MaterialButton>(R.id.btnSaveWorkout).isEnabled = false
        findViewById<MaterialButton>(R.id.btnDiscardWorkout).isEnabled = false
        val notes = findViewById<TextInputEditText>(R.id.etWorkoutNotes).text?.toString()?.trim().orEmpty()
        lifecycleScope.launch {
            try {
                WorkoutRepository(this@WorkoutSummaryActivity).save(ownerId, CompletedWorkout(
                    workoutId, intent.getStringExtra("ROUTINE_TITLE") ?: "Workout", completedAt,
                    intent.getIntExtra("SESSION_TIME_SECONDS", 0).coerceIn(0, 604800),
                    intent.getFloatExtra("TOTAL_VOLUME", 0f).toDouble(),
                    intent.getIntExtra("COMPLETED_SETS", 0).coerceIn(0, 10000), notes
                ))
                val exercises = intent.getStringArrayListExtra("PR_EXERCISES").orEmpty()
                val weights = intent.getFloatArrayExtra("PR_WEIGHTS") ?: floatArrayOf()
                preferences.edit().apply {
                    if (notes.isNotEmpty()) putString("user_last_workout_notes_" + ownerId, notes)
                    for (index in 0 until minOf(exercises.size, weights.size)) {
                        val key = "user_pr_" + exercises[index]
                        putFloat(key, maxOf(preferences.getFloat(key, 0f), weights[index]))
                    }
                    apply()
                }
                Toast.makeText(this@WorkoutSummaryActivity, R.string.ui_workout_saved_great_job, Toast.LENGTH_SHORT).show()
                goHome()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Toast.makeText(this@WorkoutSummaryActivity, R.string.workout_save_error, Toast.LENGTH_LONG).show()
            } finally {
                saving = false
                if (!isFinishing && !isDestroyed) {
                    findViewById<MaterialButton>(R.id.btnSaveWorkout).isEnabled = true
                    findViewById<MaterialButton>(R.id.btnDiscardWorkout).isEnabled = true
                }
            }
        }
    }

    private fun showDiscardConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.ui_discard_session_5))
            .setMessage(getString(R.string.ui_this_completed_workout_will_not_be_added_to_your_progress))
            .setPositiveButton(getString(R.string.ui_discard)) { _, _ -> goHome() }
            .setNegativeButton(getString(R.string.ui_keep_editing), null)
            .show()
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }
}
