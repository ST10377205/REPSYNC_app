package com.example.myapplication

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleLoginUiTest {
    @Test
    fun loginOffersConfiguredGoogleSignInAlongsidePasswordLogin() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(GoogleSignInCoordinator.webClientId(context))
        ActivityScenario.launch<LoginActivity>(android.content.Intent(context, LoginActivity::class.java).putExtra("show_login", true)).use {
            onView(withId(R.id.btnGoogleSignIn)).perform(scrollTo()).check(matches(isDisplayed())).check(matches(isEnabled()))
            onView(withId(R.id.btnGoogleSignIn)).check(matches(withText(R.string.google_sign_in)))
            onView(withId(R.id.btnLogin)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }
}
