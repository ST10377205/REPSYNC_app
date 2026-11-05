package com.example.myapplication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object NotificationNavigation {
    fun isNotificationIntent(intent: Intent): Boolean =
        intent.getBooleanExtra("from_push", false) || intent.hasExtra("google.message_id")

    fun destinationIntent(context: Context, source: Intent): Intent {
        val destination = if (isNotificationIntent(source)) source.getStringExtra("destination") else null
        val activity = when (destination) {
            "progress" -> ProgressActivity::class.java
            "workout" -> WorkoutGeneratorActivity::class.java
            else -> MainActivity::class.java
        }
        return Intent(context, activity)
    }

    fun pendingIntent(context: Context, destination: String, requestCode: Int): PendingIntent {
        // Always go through the login screen's session check; never expose private activities.
        val intent = Intent(context, LoginActivity::class.java)
            .putExtra("from_push", true)
            .putExtra("destination", destination)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
