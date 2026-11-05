package com.example.myapplication

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PushNotificationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun notificationSettingsOpenWithOrWithoutFirebaseProvisioning() {
        ActivityScenario.launch(NotificationsActivity::class.java).use {
            onView(withId(R.id.tvNotificationPermission)).check(matches(isDisplayed()))
            onView(withId(R.id.tvPushConnection)).check(matches(isDisplayed()))
            onView(withId(R.id.btnNotificationSystemSettings)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun channelAndNotificationCarryContentAndAnImmutableAppIntent() {
        PushNotifications.createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertNotNull(context.getSystemService(NotificationManager::class.java).getNotificationChannel(PushNotifications.CHANNEL_ID))
        }
        val notification = PushNotifications.buildNotification(context, NotificationContent("New plan", "Your plan is ready", "workout"), 42)
        assertEquals("New plan", notification.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals("Your plan is ready", notification.extras.getCharSequence(Notification.EXTRA_TEXT))
        assertNotNull(notification.smallIcon)
        assertEquals(context.packageName, notification.contentIntent.creatorPackage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) assertTrue(notification.contentIntent.isImmutable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) assertEquals(PushNotifications.CHANNEL_ID, notification.channelId)
    }

    @Test
    fun notificationRoutingAllowsOnlyKnownDestinationsAndOrdinaryLoginStillOpensHome() {
        val ordinary = Intent().putExtra("destination", "progress")
        assertFalse(NotificationNavigation.isNotificationIntent(ordinary))
        assertEquals(MainActivity::class.java.name, NotificationNavigation.destinationIntent(context, ordinary).component?.className)
        val push = Intent().putExtra("from_push", true).putExtra("destination", "progress")
        assertEquals(ProgressActivity::class.java.name, NotificationNavigation.destinationIntent(context, push).component?.className)
        push.putExtra("destination", "https://example.invalid")
        assertEquals(MainActivity::class.java.name, NotificationNavigation.destinationIntent(context, push).component?.className)
        val firebaseConsole = Intent().putExtra("google.message_id", "test-id").putExtra("destination", "workout")
        assertEquals(WorkoutGeneratorActivity::class.java.name, NotificationNavigation.destinationIntent(context, firebaseConsole).component?.className)
    }
}
