package com.joinforage.forage.android.ui

import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinTextWatcherTest {
    @Test
    fun `ensure afterTextChanged event is fired correctly`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        val watcher = PinTextWatcher(editText)
        editText.addTextChangedListener(watcher)

        // mutable state to help us test the callback
        var callbackCount = 0
        var isCompleteAndValidTest = false
        var isEmptyTest = true
        watcher.onInputChangeEvent { isCompleteAndValid, isEmpty ->
            callbackCount++
            isCompleteAndValidTest = isCompleteAndValid
            isEmptyTest = isEmpty
        }

        // simulate an incomplete PIN
        editText.setText("123")

        assertThat(isCompleteAndValidTest).isFalse
        assertThat(isEmptyTest).isFalse
        assertThat(callbackCount).isEqualTo(1)

        // simulate an empty PIN
        editText.setText("")

        assertThat(isCompleteAndValidTest).isFalse
        assertThat(isEmptyTest).isTrue
        assertThat(callbackCount).isEqualTo(2)

        // simulate a complete PIN
        editText.setText("1234")
        assertThat(isCompleteAndValidTest).isTrue
        assertThat(isEmptyTest).isFalse
        assertThat(callbackCount).isEqualTo(3)
    }
}
