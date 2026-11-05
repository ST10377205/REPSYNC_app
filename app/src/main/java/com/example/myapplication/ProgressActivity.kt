package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressActivity : BaseActivity() {
    private var workoutObserver: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        applySystemBarInsets(R.id.progressMain)
        setupToolbarNavigation()
        loadProgressData()
        setupBottomNavigation(R.id.nav_progress)
    }

    override fun onResume() {
        super.onResume()
        observeWorkoutChanges()
        loadProgressData()
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
            val repository = WorkoutRepository(this@ProgressActivity)
            repository.ensureLegacyImported(userId)
            repository.observe(userId).collect {
                repository.refreshMetrics(userId)
                loadProgressData()
            }
        }
    }

    private fun loadProgressData() {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)

        if (userId != -1) {
            val workoutsThisWeek = sharedPreferences.getInt("user_workouts_this_week_$userId", 0)
            val streak = sharedPreferences.getInt("user_streak_$userId", 0)
            val historyLog = sharedPreferences.getString("user_history_$userId", "") ?: ""

            // Update Header summary text
            findViewById<TextView>(R.id.tvWorkoutsThisWeek).text = if (workoutsThisWeek == 1) getString(R.string.session_one) else getString(R.string.sessions_count, workoutsThisWeek)
            findViewById<TextView>(R.id.tvStreakBadge).text = getString(R.string.days_count, streak)

            // Extract metrics for the Regression Line Stats Chart - completely zero-based initialization
            val chartView = findViewById<StatsChartView>(R.id.statsChartView)
            val realVolumePoints = mutableListOf<Float>()

            if (historyLog.trim().isNotEmpty()) {
                val individualSessions = historyLog.trim().split("\n")
                for (session in individualSessions) {
                    val parts = session.split(" • ")
                    if (parts.size >= 2) {
                        val volumeStr = parts[1].replace(" kg", "").trim()
                        val volumeFloat = volumeStr.toFloatOrNull()
                        if (volumeFloat != null && volumeFloat > 0f) {
                            realVolumePoints.add(volumeFloat)
                        }
                    }
                }
            }
            // Populate the chart view with live computed metrics (will display safe empty/zero helper text if empty)
            chartView.setData(realVolumePoints)

            // Update Saved Plans section to tell users where generated plans can be reused
            val llSavedPlansContainer = findViewById<LinearLayout>(R.id.llSavedPlansContainer)
            val tvNoSavedPlans = findViewById<TextView>(R.id.tvNoSavedPlans)

            // Show the newly generated or active plan in the dashboard list
            tvNoSavedPlans.text = getString(R.string.ui_your_generated_home_plan_is_saved_and_active_you_can_reuse_it_any)

            // Update History List with premium Material cards
            val llHistoryContainer = findViewById<LinearLayout>(R.id.llHistoryContainer)
            val cvNoHistory = findViewById<MaterialCardView>(R.id.cvNoHistory)

            if (historyLog.trim().isNotEmpty()) {
                cvNoHistory.visibility = View.GONE

                // Clear previous entries while preserving the empty-state template
                for (i in llHistoryContainer.childCount - 1 downTo 0) {
                    val child = llHistoryContainer.getChildAt(i)
                    if (child.id != R.id.cvNoHistory) {
                        llHistoryContainer.removeViewAt(i)
                    }
                }

                val sessions = historyLog.trim().split("\n").reversed()
                val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())

                for (session in sessions.take(10)) {
                    val parts = session.split(" • ")
                    if (parts.size >= 3) {
                        val title = parts[0]
                        val volume = parts[1]
                        val timestamp = parts[2].toLongOrNull() ?: 0L
                        val date = if (timestamp > 0) Date(timestamp) else Date()

                        // Create modern Material Card
                        val historyCard = MaterialCardView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 16) }
                            radius = 16f * resources.displayMetrics.density
                            cardElevation = 2f
                            setCardBackgroundColor(getColorStateList(R.color.app_surface))
                            strokeWidth = 0
                        }

                        val innerLayout = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(48, 32, 48, 32)
                        }

                        innerLayout.addView(TextView(this).apply {
                            text = localizedText(title)
                            setTextColor(getColor(R.color.text_primary))
                            textSize = 16f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                        })

                        innerLayout.addView(TextView(this).apply {
                            text = "${dateFormat.format(date)} • $volume"
                            setTextColor(getColor(R.color.text_secondary))
                            textSize = 13f
                            setPadding(0, 4, 0, 0)
                        })

                        historyCard.addView(innerLayout)
                        llHistoryContainer.addView(historyCard, 0) // Reverse order for feed feel
                    }
                }
            } else {
                cvNoHistory.visibility = View.VISIBLE
            }

            // PR Logic - Horizontal scroller for records
            val llPRContainer = findViewById<LinearLayout>(R.id.llPRContainer)
            val tvNoPRs = findViewById<TextView>(R.id.tvNoPRs)

            val commonExercises = listOf("Push-ups", "Plank", "Squats", "Dumbbell Rows", "Bench Press")
            var hasPRs = false

            llPRContainer.removeAllViews()

            for (ex in commonExercises) {
                val pr = sharedPreferences.getFloat("user_pr_$ex", 0f)
                if (pr > 0) {
                    hasPRs = true

                    val prCard = MaterialCardView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            380,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 16, 8) }
                        radius = 16f * resources.displayMetrics.density
                        cardElevation = 4f
                        setCardBackgroundColor(getColorStateList(R.color.app_surface))
                        strokeWidth = 0
                    }

                    val prLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(32, 32, 32, 32)
                        gravity = Gravity.CENTER
                    }

                    prLayout.addView(TextView(this).apply {
                        text = ex
                        setTextColor(getColor(R.color.text_secondary))
                        textSize = 11f
                        gravity = Gravity.CENTER
                    })

                    prLayout.addView(TextView(this).apply {
                        text = if (ex == "Plank") "${pr.toInt()}s" else "${pr.toInt()}kg"
                        setTextColor(getColor(R.color.orange_primary))
                        textSize = 18f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(0, 4, 0, 0)
                    })

                    prCard.addView(prLayout)
                    llPRContainer.addView(prCard)
                }
            }

            if (hasPRs) {
                tvNoPRs.visibility = View.GONE
                llPRContainer.visibility = View.VISIBLE
            } else {
                tvNoPRs.visibility = View.VISIBLE
                llPRContainer.visibility = View.GONE
            }
        }
    }
}
