package com.example.myapplication

import android.content.Intent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import android.view.View
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matcher

@RunWith(AndroidJUnit4::class)
class OnlineExercisesUiTest {
    @Test fun searchGuideAndStartExerciseWorkWithRealCatalogue() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("live_fitness_api") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val page = OnlineExerciseRepository(context).loadPage(refresh = true)
        assertFalse(page.saved)
        val exercise = page.exercises.first()
        ActivityScenario.launch<OnlineExercisesActivity>(Intent(context, OnlineExercisesActivity::class.java)).use { scenario ->
            var loaded = false
            repeat(50) {
                if (!loaded) {
                    scenario.onActivity { activity ->
                        val list = activity.findViewById<LinearLayout>(R.id.llOnlineExercises)
                        loaded = list.childCount > 0 && list.getChildAt(0) is com.google.android.material.button.MaterialButton
                    }
                    if (!loaded) Thread.sleep(100)
                }
            }
            assertTrue("Cached exercise list should load", loaded)
            onView(withId(R.id.etOnlineSearch)).perform(replaceText("zz-no-such-exercise"), closeSoftKeyboard())
            onView(withText(R.string.online_no_results)).check(matches(isDisplayed()))
            onView(withId(R.id.etOnlineSearch)).perform(replaceText(exercise.name), closeSoftKeyboard())
            scenario.onActivity { activity ->
                val list = activity.findViewById<LinearLayout>(R.id.llOnlineExercises)
                assertTrue((list.getChildAt(0) as TextView).text.startsWith(exercise.name))
                list.getChildAt(0).performClick()
            }
            onView(withText(exercise.name)).inRoot(isDialog()).check(matches(isDisplayed()))
            var workout: WorkoutSessionActivity? = null
            try {
                onView(withId(android.R.id.button1)).inRoot(isDialog()).check(matches(withText(R.string.online_start)))
                // Dispatch the button's accessibility click without screen-coordinate timing.
                onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(object : ViewAction {
                    override fun getConstraints(): Matcher<View> = isEnabled()
                    override fun getDescription() = "Activate Start exercise"
                    override fun perform(controller: UiController, view: View) {
                        assertTrue(view.performClick())
                        controller.loopMainThreadUntilIdle()
                    }
                })
                repeat(50) {
                    if (workout == null) {
                        instrumentation.runOnMainSync {
                            workout = ActivityLifecycleMonitorRegistry.getInstance()
                                .getActivitiesInStage(Stage.RESUMED).filterIsInstance<WorkoutSessionActivity>().firstOrNull()
                        }
                        if (workout == null) Thread.sleep(100)
                    }
                }
                assertNotNull("Start should open the workout screen", workout)
                onView(withId(R.id.tvRoutineTitle)).check(matches(withText(exercise.name)))
                assertEquals(arrayListOf(exercise.name), workout!!.intent.getStringArrayListExtra("EXERCISE_NAMES"))
            } finally { instrumentation.runOnMainSync { workout?.finish() } }
        }
    }
}
