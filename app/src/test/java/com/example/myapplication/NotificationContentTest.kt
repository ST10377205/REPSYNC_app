package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationContentTest {
    @Test
    fun localizedDataOverridesGenericAndNotificationPayload() {
        val data = mapOf("title" to "Plan ready", "body" to "Open your plan", "title_zu" to "Uhlelo selulungile", "body_zu" to "Vula uhlelo lwakho", "destination" to "workout")
        val content = NotificationContent.from(data, "zu-ZA", "Default", "Default body", "Notification title", "Notification body")
        assertEquals("Uhlelo selulungile", content.title)
        assertEquals("Vula uhlelo lwakho", content.body)
        assertEquals("workout", content.destination)
    }

    @Test
    fun missingOrBlankTranslationFallsBackToGenericThenNotificationThenDefaults() {
        val generic = NotificationContent.from(mapOf("title_af" to " ", "title" to "Plan ready"), "af", "Default", "Default body", "SDK title", "SDK body")
        assertEquals("Plan ready", generic.title)
        assertEquals("SDK body", generic.body)
        val fallback = NotificationContent.from(emptyMap(), "zu", "Isibuyekezo", "Vula i-RepSync", "", null)
        assertEquals("Isibuyekezo", fallback.title)
        assertEquals("Vula i-RepSync", fallback.body)
    }

    @Test
    fun unrecognizedDestinationCannotChooseAnArbitraryActivityOrUrl() {
        for (value in listOf("https://example.invalid", "ProfileActivity", "intent://settings")) {
            assertEquals("home", NotificationContent.from(mapOf("destination" to value), "en", "Update", "Open app").destination)
        }
        assertEquals("progress", NotificationContent.from(mapOf("destination" to "progress"), "af", "Update", "Open app").destination)
    }
}
