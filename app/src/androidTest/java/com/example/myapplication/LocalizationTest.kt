package com.example.myapplication

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    @Test
    fun profilePickerSwitchesLanguagesAndKeepsSelectionAfterRecreation() {
        ActivityScenario.launch(ProfileActivity::class.java).use { scenario ->
            val original = AppCompatDelegate.getApplicationLocales()
            try {
                for ((name, label) in listOf("isiZulu" to "Ulimi", "Afrikaans" to "Taal", "English" to "Language")) {
                    onView(withId(R.id.btnLanguage)).perform(scrollTo(), click())
                    onView(withText(name)).inRoot(isDialog()).perform(click())
                    onView(withId(R.id.tvLanguageLabel)).check(matches(withText(label)))
                    onView(withId(R.id.tvLanguageValue)).check(matches(withText(name)))
                    scenario.recreate()
                    onView(withId(R.id.tvLanguageLabel)).check(matches(withText(label)))
                    onView(withId(R.id.tvLanguageValue)).check(matches(withText(name)))
                }
            } finally {
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    AppCompatDelegate.setApplicationLocales(original)
                }
            }
        }
    }

    private fun contextFor(tag: String) =
        InstrumentationRegistry.getInstrumentation().targetContext.let { context ->
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(Locale.forLanguageTag(tag))
            context.createConfigurationContext(configuration)
        }

    @Test
    fun languageResourcesAndFormattedMessagesUseSelectedLocale() {
        for ((tag, language) in listOf("en" to "Language", "zu" to "Ulimi", "af" to "Taal")) {
            val context = contextFor(tag)
            assertEquals(language, context.getString(R.string.language))
            assertEquals(2, Regex("7|9").findAll(context.getString(R.string.next_level, 7, 9)).count())
            if (tag != "en") {
                assertNotEquals("Start (3)", context.getString(R.string.start_count, 3))
                assertNotEquals("Push-ups", context.localizedText("Push-ups"))
                assertNotEquals("Chest • Bodyweight", context.localizedText("Chest • Bodyweight"))
            }
        }
    }

    @Test
    fun translatingDisplayLabelsPreservesCanonicalAndUserProvidedText() {
        val canonical = listOf("Push-ups", "Home Full Body", "Build Muscle - Home (No Equipment)")
        val before = canonical.toList()
        for (tag in listOf("zu", "af", "en")) {
            val context = contextFor(tag)
            canonical.forEach { context.localizedText(it) }
            assertEquals(before, canonical)
            assertEquals("My own workout", context.localizedText("My own workout"))
        }
    }
}
