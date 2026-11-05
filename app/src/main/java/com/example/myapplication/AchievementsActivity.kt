package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

class AchievementsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        applySystemBarInsets(R.id.achievementsMain)
        setupToolbarNavigation()

        loadAchievements()
        setupBottomNavigation(R.id.nav_achievements)
    }

    override fun onResume() {
        super.onResume()
        loadAchievements()
    }

    private fun loadAchievements() {
        val sharedPreferences = getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("current_user_id", -1)

        if (userId != -1) {
            val totalWorkouts = sharedPreferences.getInt("user_total_workouts_$userId", 0)
            val streak = sharedPreferences.getInt("user_streak_$userId", 0)
            
            // Level Logic: 100 XP per workout
            val totalXp = totalWorkouts * 100
            val level = (totalXp / 500)
            val xpInLevel = totalXp % 500
            
            findViewById<TextView>(R.id.tvCurrentRank).text = if (level == 0) getString(R.string.ui_level_0_beginner) else getString(R.string.level_athlete, level)
            findViewById<TextView>(R.id.tvTotalXp).text = "$totalXp XP"
            findViewById<ProgressBar>(R.id.progressBarXp).progress = (xpInLevel * 100) / 500
            findViewById<TextView>(R.id.tvXpToNextLevel).text = getString(R.string.next_level, 500 - xpInLevel, level + 1)
            
            // Achievement 1: Consistency Master (10 day streak)
            val pb1 = findViewById<ProgressBar>(R.id.pbAchievement1)
            val tv1Status = findViewById<TextView>(R.id.tvAchievement1Status)
            val ivBadge1 = findViewById<ImageView>(R.id.ivBadge1)
            
            pb1.progress = (streak * 100) / 10
            if (streak >= 10) {
                tv1Status.text = getString(R.string.ui_unlocked)
                tv1Status.setTextColor(getColor(R.color.orange_primary))
                ivBadge1.setImageResource(R.drawable.ic_trophy)
                ivBadge1.imageTintList = getColorStateList(R.color.orange_primary)
            } else {
                tv1Status.text = getString(R.string.streak_target, streak)
            }

            // Achievement 2: Volume Crusher
            // Placeholder logic for now, using a simple check
            val tv2Status = findViewById<TextView>(R.id.tvAchievement2Status)
            val ivBadge2 = findViewById<ImageView>(R.id.ivBadge2)
            if (totalWorkouts >= 5) {
                tv2Status.text = getString(R.string.ui_unlocked)
                tv2Status.setTextColor(getColor(R.color.orange_primary))
                ivBadge2.imageTintList = getColorStateList(R.color.orange_primary)
            }
        }
    }
}
