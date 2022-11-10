package com.joinforage.android.example

import android.os.SystemClock
import android.view.KeyEvent
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joinforage.android.example.ui.catalog.CatalogFragment
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ForagePANEditTextTest {
    @Test
    fun oneDigit_ShouldShowInvalidPan() {
        launchFragmentInContainer<CatalogFragment>(themeResId = R.style.Theme_ForageAndroid)

        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_1))

        onView(withText("Invalid PAN")).check(matches(isDisplayed()))
    }

    @Test
    fun shouldAcceptValidPan() {
        launchFragmentInContainer<CatalogFragment>(themeResId = R.style.Theme_ForageAndroid)

        // TODO make a helper function to this
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_5))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_0))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_7))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_6))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_8))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_0))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_1))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_2))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_3))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_4))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_5))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_6))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_7))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_8))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_4))
        onView(withId(R.id.firstForageEditText)).perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_4))

        // TODO improve this
        SystemClock.sleep(1000)

        onView(withText("Invalid PAN")).check(doesNotExist())
    }
}
