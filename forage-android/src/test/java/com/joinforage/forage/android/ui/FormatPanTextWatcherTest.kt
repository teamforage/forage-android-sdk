package com.joinforage.forage.android.ui

import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InputSanitizationTest {
    @Test
    fun `static - non-whitespace, non-digit characters get stripped`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("!@#$%^&*()_+1234567890-=")
        assertThat(editText.text.toString()).isEqualTo("1234567890")
    }

    @Test
    fun `dynamic - can enter a digit`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("123")
        editText.text.insert(1, "7")
        assertThat(editText.text.toString()).isEqualTo("1723")
    }

    @Test
    fun `dynamic - cannot enter whitespace`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("123")
        editText.text.insert(1, " ")
        assertThat(editText.text.toString()).isEqualTo("123")
    }

    @Test
    fun `dynamic - cannot enter non-digits`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("123")
        editText.text.insert(1, "a")
        assertThat(editText.text.toString()).isEqualTo("123")
    }
}

@RunWith(RobolectricTestRunner::class)
class StaticPANFormattingTest {
    @Test
    fun `empty input maps to empty output`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("")
        assertThat(editText.text.toString()).isEqualTo("")
    }

    @Test
    fun `16 digit PAN gets formatted correctly`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("5053490000000000")
        assertThat(editText.text.toString()).isEqualTo("5053 4900 0000 0000")
    }

    @Test
    fun `18 digit PAN gets formatted correctly`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("6008900000000000000")
        assertThat(editText.text.toString()).isEqualTo("600890 0000 00000 00 0")
    }

    @Test
    fun `19 digit PAN gets formatted correctly`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("6274850000000000000")
        assertThat(editText.text.toString()).isEqualTo("627485 0000 0000 000 00")
    }

    @Test
    fun `input with invalid StateIIN does not get transformed`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))
        editText.setText("420420000000")
        assertThat(editText.text.toString()).isEqualTo("420420000000")
    }
}

class ExpandDigitsToFormatTest {
    @Test
    fun `digits beyond pattern length get dropped`() {
        val testFormat = expandDigitsToFormat("## #")
        val formattedText = testFormat("1234")
        assertThat(formattedText).isEqualTo("12 3") // 4 is dropped
    }

    @Test
    fun `incomplete - start of number group`() {
        val testFormat = expandDigitsToFormat("## #")
        val formattedText = testFormat("1")
        assertThat(formattedText).isEqualTo("1")
    }

    @Test
    fun `incomplete - within number group`() {
        val testFormat = expandDigitsToFormat("### #")
        val formattedText = testFormat("12")
        assertThat(formattedText).isEqualTo("12")
    }

    @Test
    fun `incomplete - end of number group`() {
        val testFormat = expandDigitsToFormat("## #")
        val formattedText = testFormat("12")
        assertThat(formattedText).isEqualTo("12")
    }
}

@RunWith(RobolectricTestRunner::class)
class TypingCharactersTest {
    @Test
    fun `input is max-length - typing at start of string`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 042 04") // to help us visualize

        // simulate typing a "1" at start of input
        editText.setSelection(0)
        editText.text.insert(0, "1")

        // 1 gets inserted and trailing 4 gets dropped.
        // NOTE: that this becomes an invalid StateIIN so the format is lost too
        assertThat(editText.text.toString()).isEqualTo("1627485420420420420")

        // a standard insert should move the cursor to the next position
        assertThat(editText.selectionStart).isEqualTo(1)
        assertThat(editText.selectionEnd).isEqualTo(1)
    }

    @Test
    fun `input is max-length - typing before delimiter`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 042 04") // to help us visualize

        // simulate typing a "1" between "85" and " 42"
        editText.setSelection(6)
        editText.text.insert(editText.selectionStart, "1")

        // 1 gets inserted and trailing 4 gets dropped.
        assertThat(editText.text.toString()).isEqualTo("627485 1420 4204 204 20")

        // inserting before a delimiter means the cursor needs
        // to jump across the delimiter to be after the next number
        assertThat(editText.selectionStart).isEqualTo(8)
        assertThat(editText.selectionEnd).isEqualTo(8)
    }

    @Test
    fun `input is max-length - typing after delimiter`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 042 04") // to help us visualize

        // simulate typing a "1" between "85 " and "42"
        editText.setSelection(7)
        editText.text.insert(editText.selectionStart, "1")

        // 1 gets inserted and trailing 4 gets dropped.
        assertThat(editText.text.toString()).isEqualTo("627485 1420 4204 204 20")

        // a standard insert should move the cursor to the next position
        assertThat(editText.selectionStart).isEqualTo(8)
        assertThat(editText.selectionEnd).isEqualTo(8)
    }

    @Test
    fun `input is max-length - typing at end of string`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 042 04") // to help us visualize

        // simulate typing a "1" after trailing "04"
        editText.setSelection(23)
        editText.text.insert(editText.selectionStart, "1")

        // no-op. Can't add more characters at the end of the string
        assertThat(editText.text.toString()).isEqualTo("627485 4204 2042 042 04")

        // the cursor should remain at the end of the string
        assertThat(editText.selectionStart).isEqualTo(23)
        assertThat(editText.selectionEnd).isEqualTo(23)
    }

    @Test
    fun `input NOT max-length - typing at start of string`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204") // to help us visualize

        // simulate typing a "1" at beginning of string
        editText.setSelection(0)
        editText.text.insert(editText.selectionStart, "1")

        // "1" gets inserted. everything else gets pushed
        // NOTE: this breaks the StateIIN so format collapses
        assertThat(editText.text.toString()).isEqualTo("16274854204")

        // the cursor should remain at the end of the string
        assertThat(editText.selectionStart).isEqualTo(1)
        assertThat(editText.selectionEnd).isEqualTo(1)
    }

    @Test
    fun `input NOT max-length - typing before delimiter`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204") // to help us visualize

        // simulate typing a "1" between "85" and " 42"
        editText.setSelection(6)
        editText.text.insert(editText.selectionStart, "1")

        // 1 gets inserted and spill into new group
        assertThat(editText.text.toString()).isEqualTo("627485 1420 4")

        // inserting before a delimiter means the cursor needs
        // to jump across the delimiter to be after the next number
        assertThat(editText.selectionStart).isEqualTo(8)
        assertThat(editText.selectionEnd).isEqualTo(8)
    }

    @Test
    fun `input NOT max-length - typing after delimiter`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204") // to help us visualize

        // simulate typing a "1" between "85 " and "42"
        editText.setSelection(7)
        editText.text.insert(editText.selectionStart, "1")

        // 1 gets inserted and spill into new group
        assertThat(editText.text.toString()).isEqualTo("627485 1420 4")

        // a standard insert should move the cursor to the next position
        assertThat(editText.selectionStart).isEqualTo(8)
        assertThat(editText.selectionEnd).isEqualTo(8)
    }

    @Test
    fun `input NOT max-length - typing at end of string`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204") // to help us visualize

        // simulate typing a "1" after trailing "04"
        editText.setSelection(11)
        editText.text.insert(editText.selectionStart, "1")

        // 1 gets inserted and spill into new group
        assertThat(editText.text.toString()).isEqualTo("627485 4204 1")

        // inserting before a delimiter means the cursor needs
        // to jump across the delimiter to be after the next number
        assertThat(editText.selectionStart).isEqualTo(13)
        assertThat(editText.selectionEnd).isEqualTo(13)
    }
}

@RunWith(RobolectricTestRunner::class)
class PastingOverSelectionsTest {
    @Test
    fun `pasting into empty input`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("") // to help us visualize

        // simulate pasting a "1" in empty input
        editText.setSelection(0)
        editText.text.replace(editText.selectionStart, editText.selectionEnd, "1")

        // 1 gets inserted
        assertThat(editText.text.toString()).isEqualTo("1")

        // pasting in this way should move the cursor to the next position
        assertThat(editText.selectionStart).isEqualTo(1)
        assertThat(editText.selectionEnd).isEqualTo(1)
    }

    @Test
    fun `pasting over entire input`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 042 04") // to help us visualize

        // simulate pasting a "627485 4204" across whole input
        editText.setSelection(0, formattedText.length)
        editText.text.replace(editText.selectionStart, editText.selectionEnd, "6274854204")

        // "627485 4204" gets inserted
        assertThat(editText.text.toString()).isEqualTo("627485 4204")

        // pasting in this way should move the cursor to the next position
        assertThat(editText.selectionStart).isEqualTo(11)
        assertThat(editText.selectionEnd).isEqualTo(11)
    }

    @Test
    fun `pasting across group boundaries`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 042 04") // to help us visualize

        // simulate pasting across "204 204"
        editText.setSelection(8, 15)
        editText.text.replace(editText.selectionStart, editText.selectionEnd, "6274854204")

        // "627485 4204" gets inserted
        assertThat(editText.text.toString()).isEqualTo("627485 4627 4854 204 20")

        // pasting in this way should move the cursor to the next position
        assertThat(editText.selectionStart).isEqualTo(20)
        assertThat(editText.selectionEnd).isEqualTo(20)
    }

    @Test
    fun `pasting before group delimiter`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 042 04") // to help us visualize

        // simulate pasting across "4204" and " 2042"
        editText.setSelection(11)
        editText.text.replace(editText.selectionStart, editText.selectionEnd, "77")

        // "77" gets inserted and pushes text back
        assertThat(editText.text.toString()).isEqualTo("627485 4204 7720 420 42")

        // pasting moves the cursor to the end of the pasted text
        assertThat(editText.selectionStart).isEqualTo(14)
        assertThat(editText.selectionEnd).isEqualTo(14)
    }

    @Test
    fun `pasting after group delimiter`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 042 04") // to help us visualize

        // simulate pasting between "4204 " and "2042"
        editText.setSelection(12)
        editText.text.replace(editText.selectionStart, editText.selectionEnd, "77")

        // "77" gets inserted and pushes text back
        assertThat(editText.text.toString()).isEqualTo("627485 4204 7720 420 42")

        // pasting moves the cursor to the end of the pasted text
        assertThat(editText.selectionStart).isEqualTo(14)
        assertThat(editText.selectionEnd).isEqualTo(14)
    }

    @Test
    fun `pasting at start of input`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("627485420")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 420") // to help us visualize

        // simulate pasting at start of input
        editText.setSelection(0)
        editText.text.replace(editText.selectionStart, editText.selectionEnd, "77")

        // "77" gets inserted and pushes text back
        // NOTE: that format is lost since StateIIN becomes invalid
        assertThat(editText.text.toString()).isEqualTo("77627485420")

        // pasting moves the cursor to the end of the pasted text
        assertThat(editText.selectionStart).isEqualTo(2)
        assertThat(editText.selectionEnd).isEqualTo(2)
    }

    @Test
    fun `pasting at end of input`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("6274854204204204")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("627485 4204 2042 04") // to help us visualize

        // simulate pasting "7777" at end of the input
        editText.setSelection(formattedText.length)
        editText.text.replace(editText.selectionStart, editText.selectionEnd, "7777")

        // "77" gets inserted, correctly grouped and the last "7" gets dropped
        assertThat(editText.text.toString()).isEqualTo("627485 4204 2042 047 77")

        // pasting moves the cursor to the end of the pasted text
        // which in this case is the end of the input
        assertThat(editText.selectionStart).isEqualTo(23)
        assertThat(editText.selectionEnd).isEqualTo(23)
    }
}

@RunWith(RobolectricTestRunner::class)
class BackspaceInteractionsTest {
    @Test
    fun `backspace on whitespace delimiter does not affect text but moves cursor left`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("505349012")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("5053 4901 2") // to help us visualize

        // simulate backspace on trailing " " before the 2
        editText.setSelection(editText.text.length - 1)
        val cursorPos = editText.selectionStart
        editText.text.delete(cursorPos - 1, cursorPos)

        // trying to delete a whitespace should not affect text content
        assertThat(editText.text.toString()).isEqualTo("5053 4901 2")

        // trying to delete a whitespace should move cursor
        // to left, placing it between 1 and " "
        assertThat(editText.selectionStart).isEqualTo(editText.text.length - 2)
        assertThat(editText.selectionEnd).isEqualTo(editText.text.length - 2)
    }

    @Test
    fun `backspace on only digit in group deletes digit and cursor stays at end`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("505349012")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("5053 4901 2") // to help us visualize

        // simulate backspace on trailing 2
        editText.setSelection(editText.text.length)
        val cursorPos = editText.selectionStart
        editText.text.delete(cursorPos - 1, cursorPos)

        // deleting the trailing two should also delete the whitespace
        assertThat(editText.text.toString()).isEqualTo("5053 4901")

        // cursor should remain at end of string
        assertThat(editText.selectionStart).isEqualTo(editText.text.length)
        assertThat(editText.selectionEnd).isEqualTo(editText.text.length)
    }

    @Test
    fun `backspace on last digit in group deletes digit and collapses group`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("505349012")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("5053 4901 2") // to help us visualize

        // simulate backspace on the 1
        editText.setSelection(editText.text.length - 2)
        val cursorPos = editText.selectionStart
        editText.text.delete(cursorPos - 1, cursorPos)

        // deleting the trailing 1 should collapse into two groups
        assertThat(editText.text.toString()).isEqualTo("5053 4902")

        // cursor should remain at end of string
        assertThat(editText.selectionStart).isEqualTo(editText.text.length - 1)
        assertThat(editText.selectionEnd).isEqualTo(editText.text.length - 1)
    }

    @Test
    fun `backspace on span across groups deletes digits and preserves groups`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        editText.addTextChangedListener(FormatPanTextWatcher(editText))

        // initialize editText
        editText.setText("505349012345")
        val formattedText = editText.text.toString()
        assertThat(formattedText).isEqualTo("5053 4901 2345") // to help us visualize

        // simulate backspace on the "1 2"
        editText.setSelection(8, 11)
        editText.text.delete(editText.selectionStart, editText.selectionEnd)

        // make sure groups are still preserved
        assertThat(editText.text.toString()).isEqualTo("5053 4903 45")

        // cursor should lie between "03" and " 45"
        assertThat(editText.selectionStart).isEqualTo(8)
        assertThat(editText.selectionEnd).isEqualTo(8)
    }
}

@RunWith(RobolectricTestRunner::class)
class OnFormattedChangeEventTest {
    @Test
    fun `correctly passes the formatted text to the callback`() {
        val editText = EditText(ApplicationProvider.getApplicationContext())
        val watcher = FormatPanTextWatcher(editText)
        editText.addTextChangedListener(watcher)

        // mutable state to help us test the callback
        var callbackCount = 0
        var actualPAN = ""
        watcher.onFormattedChangeEvent { formattedCardNumber ->
            callbackCount++
            actualPAN = formattedCardNumber
        }

        // simulate a paste event
        editText.setText("505349012345")

        // we expect the callback to be passed the correctly
        // formatted PAN exactly once
        val expectedFormattedPAN = "5053 4901 2345"
        assertThat(editText.text.toString()).isEqualTo(expectedFormattedPAN)
        assertThat(actualPAN).isEqualTo(expectedFormattedPAN)
        assertThat(callbackCount).isEqualTo(1)
    }
}
