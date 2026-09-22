package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * ProgressActivity - Displays user fitness progress and cloud-synced workout history.
 * Optimized to fetch only data belonging to the logged-in user.
 */
class ProgressActivity : BaseActivity() {

    private val TAG = "ProgressActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        loadProgressData()
        loadCloudFeedData()
        setupBottomNavigation(R.id.nav_progress)
    }

    override fun onResume() {
        super.onResume()
        loadProgressData()
        loadCloudFeedData()
    }

    private fun loadCloudFeedData() {
        val tvCloudHistoryStatus = findViewById<TextView>(R.id.tvCloudHistoryStatus) ?: return
        val llCloudHistoryContainer = findViewById<LinearLayout>(R.id.llCloudHistoryContainer) ?: return
        val cvNoCloudHistory = findViewById<MaterialCardView>(R.id.cvNoCloudHistory) ?: return

        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)

        if (userId == -1) {
            tvCloudHistoryStatus.text = "Please log in to view cloud history."
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching cloud workouts for userId: $userId")
                val apiService = WorkoutApiService.create()
                // Optimized fetch: Only retrieves workouts for this specific user
                val cloudWorkoutsMap = apiService.getUserCloudWorkouts(userId)

                // Clear previous entries while preserving the empty-state template card
                for (i in llCloudHistoryContainer.childCount - 1 downTo 0) {
                    val child = llCloudHistoryContainer.getChildAt(i)
                    if (child.id != R.id.cvNoCloudHistory) {
                        llCloudHistoryContainer.removeViewAt(i)
                    }
                }

                if (cloudWorkoutsMap != null && cloudWorkoutsMap.isNotEmpty()) {
                    cvNoCloudHistory.visibility = View.GONE
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
                    val cloudWorkoutsList = cloudWorkoutsMap.values.toList()

                    // Show top 5 cloud synced entries for this user
                    for (workout in cloudWorkoutsList.takeLast(5).reversed()) {
                        val historyCard = MaterialCardView(this@ProgressActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(0, 0, 0, 16) }
                            radius = 16f * resources.displayMetrics.density
                            cardElevation = 2f
                            setCardBackgroundColor(getColorStateList(R.color.app_surface))
                            strokeWidth = 0
                        }

                        val innerLayout = LinearLayout(this@ProgressActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(48, 32, 48, 32)
                        }

                        innerLayout.addView(TextView(this@ProgressActivity).apply {
                            text = workout.workoutTitle
                            setTextColor(getColor(R.color.orange_primary))
                            textSize = 15f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                        })

                        innerLayout.addView(TextView(this@ProgressActivity).apply {
                            text = "Duration: ${workout.durationMinutes} mins • Synced via Firebase Cloud"
                            setTextColor(getColor(R.color.text_primary))
                            textSize = 13f
                            setPadding(0, 4, 0, 0)
                        })

                        innerLayout.addView(TextView(this@ProgressActivity).apply {
                            text = dateFormat.format(Date(workout.timestamp))
                            setTextColor(getColor(R.color.text_secondary))
                            textSize = 11f
                            setPadding(0, 4, 0, 0)
                        })

                        historyCard.addView(innerLayout)
                        llCloudHistoryContainer.addView(historyCard)
                    }
                } else {
                    cvNoCloudHistory.visibility = View.VISIBLE
                    tvCloudHistoryStatus.text = "No cloud entries for your account yet."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch cloud history", e)
                cvNoCloudHistory.visibility = View.VISIBLE
                tvCloudHistoryStatus.text = "Cloud feed unavailable. Check connection."
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
            findViewById<TextView>(R.id.tvWorkoutsThisWeek).text = if (workoutsThisWeek == 1) "1 Session" else "$workoutsThisWeek Sessions"
            findViewById<TextView>(R.id.tvStreakBadge).text = "$streak Days"

            // Extract metrics for the Regression Line Stats Chart
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
            chartView.setData(realVolumePoints)

            // Saved Plans Helper
            findViewById<TextView>(R.id.tvNoSavedPlans).text = "Your generated plans are active! Reuse them anytime from the Home screen."

            // Update History List
            val llHistoryContainer = findViewById<LinearLayout>(R.id.llHistoryContainer)
            val cvNoHistory = findViewById<MaterialCardView>(R.id.cvNoHistory)

            if (historyLog.trim().isNotEmpty()) {
                cvNoHistory.visibility = View.GONE
                
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
                            text = title
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
                        llHistoryContainer.addView(historyCard)
                    }
                }
            } else {
                cvNoHistory.visibility = View.VISIBLE
            }
            
            // PR Logic
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
                        layoutParams = LinearLayout.LayoutParams(380, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 16, 8) }
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
            tvNoPRs.visibility = if (hasPRs) View.GONE else View.VISIBLE
            llPRContainer.visibility = if (hasPRs) View.VISIBLE else View.GONE
        }
    }
}
