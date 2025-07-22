package com.joinforage.forage.android.core.ui.textwatcher

import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpirationTextWatcherTest {
    @Test
    fun `verify watcher adds slash`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        val watcher = ExpirationTextWatcher(editText)
        editText.addTextChangedListener(watcher)

        var changeCount = 0
        watcher.onFormattedChangeEvent { ++changeCount }

        changeCount = 0
        editText.setText("")
        assertThat(editText.text.toString()).isEqualTo("")
        assertThat(changeCount).isEqualTo(1)

        changeCount = 0
        editText.append("1")
        assertThat(editText.text.toString()).isEqualTo("1")
        assertThat(changeCount).isEqualTo(1)

        changeCount = 0
        editText.append("2")
        assertThat(editText.text.toString()).isEqualTo("12")
        assertThat(changeCount).isEqualTo(1)

        changeCount = 0
        editText.append("3")
        assertThat(editText.text.toString()).isEqualTo("12/3")
        assertThat(changeCount).isEqualTo(1)

        changeCount = 0
        editText.append("4")
        assertThat(editText.text.toString()).isEqualTo("12/34")
        assertThat(changeCount).isEqualTo(1)
    }
}
