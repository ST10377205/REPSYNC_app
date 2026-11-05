package com.example.myapplication

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RepSyncMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushNotifications.saveToken(this, token)
        // Firebase maintains topic subscriptions across token rotations; connect retries on app startup.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Data-only messages use this path in both foreground and background.
        // Notification payloads use this path in foreground; Firebase displays them in background.
        val context = PushNotifications.localizedContext(this)
        val content = NotificationContent.from(
            message.data, PushNotifications.language(this),
            context.getString(R.string.push_default_title), context.getString(R.string.push_default_body),
            message.notification?.title, message.notification?.body
        )
        PushNotifications.recordReceipt(this, content)
        PushNotifications.show(this, content, message.messageId ?: java.util.UUID.randomUUID().toString())
    }
}
