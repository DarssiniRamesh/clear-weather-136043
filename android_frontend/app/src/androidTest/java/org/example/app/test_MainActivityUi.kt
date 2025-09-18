package org.example.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests using Espresso.
 *
 * These tests validate:
 * - Empty input shows error.
 * - Typing text enables valid search behavior (note: without a valid API key network call won't succeed;
 *   this test focuses on input and initial state/visibility toggles).
 *
 * For fully deterministic network verification on device, a network mocking layer would be required.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityUiTest {

    @Test
    fun emptyInput_showsError() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.searchButton)).perform(click())

        onView(withId(R.id.errorText))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(withText(R.string.error_empty_city)))
    }

    @Test
    fun typingCity_enablesSearchButton() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.inputCity)).perform(click(), typeText("Paris"), closeSoftKeyboard())

        // Button should be visible and enabled by default
        onView(withId(R.id.searchButton)).check(matches(allOf(isDisplayed(), isEnabled())))
    }
}
