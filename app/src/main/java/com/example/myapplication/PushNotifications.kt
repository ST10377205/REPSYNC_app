package com.example.myapplication

import android.Manifest
import android.app.LocaleManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale
import java.util.UUID

object PushNotifications {
    const val CHANNEL_ID = "workout_updates"
    // Public broadcasts only. Personal/user-specific information must not use a public topic.
    const val UPDATES_TOPIC = "repsync_updates"
    private const val PREFS = "RepSyncPushPrefs"

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = runCatching { FirebaseApp.getInstance() }.isSuccess

    fun rememberLanguage(context: Context) {
        preferences(context).edit().putString("language", context.resources.configuration.locales[0].language).apply()
        createChannel(context)
    }

    fun language(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) return locales[0].language
        }
        return preferences(context).getString("language", null)
            ?: context.resources.configuration.locales[0].language
    }

    fun localizedContext(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language(context)))
        return context.createConfigurationContext(configuration)
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localized = localizedContext(context)
            val channel = NotificationChannel(
                CHANNEL_ID, localized.getString(R.string.push_channel_name), NotificationManager.IMPORTANCE_HIGH
            ).apply { description = localized.getString(R.string.push_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    fun connect(context: Context, complete: (Boolean) -> Unit = {}) {
        val appContext = context.applicationContext
        if (!isConfigured()) { complete(false); return }
        val messaging = FirebaseMessaging.getInstance()
        messaging.token.addOnCompleteListener { tokenTask ->
            if (!tokenTask.isSuccessful) { complete(false); return@addOnCompleteListener }
            saveToken(appContext, tokenTask.result)
            messaging.subscribeToTopic(UPDATES_TOPIC).addOnCompleteListener { subscription ->
                complete(subscription.isSuccessful)
            }
        }
    }

    fun saveToken(context: Context, token: String) {
        preferences(context).edit().putString("token", token).apply()
    }

    fun token(context: Context): String? = preferences(context).getString("token", null)

    fun recordReceipt(context: Context, content: NotificationContent) {
        preferences(context).edit()
            .putString("last_title", content.title)
            .putLong("last_received_at", System.currentTimeMillis()).apply()
    }

    fun lastReceivedTitle(context: Context): String? = preferences(context).getString("last_title", null)

    fun buildNotification(context: Context, content: NotificationContent, requestCode: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.orange_primary))
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(NotificationNavigation.pendingIntent(context, content.destination, requestCode))
            .build()

    fun show(context: Context, content: NotificationContent, messageId: String = UUID.randomUUID().toString()): Boolean {
        createChannel(context)
        if (!notificationsAllowed(context)) return false
        return try {
            context.getSystemService(NotificationManager::class.java)
                .notify(messageId, 0, buildNotification(context, content, messageId.hashCode()))
            true
        } catch (_: SecurityException) {
            // Permission can be revoked between checking it and posting the notification.
            false
        }
    }
}
