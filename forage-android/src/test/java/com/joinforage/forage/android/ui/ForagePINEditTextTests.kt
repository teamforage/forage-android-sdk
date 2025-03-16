package com.joinforage.forage.android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.joinforage.forage.android.core.ui.textwatcher.PinTextWatcher
import com.joinforage.forage.android.ecom.ui.element.ForagePINEditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ForagePINEditTextTests {

    private lateinit var context: Context
    private lateinit var foragePINEditText: ForagePINEditText

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        foragePINEditText = ForagePINEditText(context)
    }

    @Test
    fun testShowKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        foragePINEditText.showKeyboard()
        val shadowImm = Shadows.shadowOf(imm)
        assertTrue(shadowImm.isSoftInputVisible)
    }

    @Test
    fun testClearText() {
        foragePINEditText._editText.setText("1234")
        assertEquals("1234", foragePINEditText._editText.text.toString())

        foragePINEditText.clearText()
        assertEquals("", foragePINEditText._editText.text.toString())
    }

    @Test
    fun testGetTextElement() {
        assertTrue(foragePINEditText._editText is EditText)
    }

    @Test
    fun testTypeface() {
        val typeface = Typeface.DEFAULT_BOLD
        foragePINEditText.typeface = typeface
        assertEquals(typeface, foragePINEditText.typeface)
    }

    @Test
    fun testSetTextColor() {
        val color = Color.RED
        foragePINEditText.setTextColor(color)
        assertEquals(color, (foragePINEditText._editText).currentTextColor)
    }

    @Test
    fun testSetTextSize() {
        val textSize = 20f
        foragePINEditText.setTextSize(textSize)
        assertEquals(textSize, (foragePINEditText._editText).textSize)
    }

    @Test
    fun `ensure onFocusChange`() {
        val editText = foragePINEditText._editText
        val focusChangeListener = editText.onFocusChangeListener

        focusChangeListener.onFocusChange(editText, true)

        assertTrue(foragePINEditText.pinEditTextState.isFocused)
        assertFalse(foragePINEditText.pinEditTextState.isBlurred)

        focusChangeListener.onFocusChange(editText, false)
        assertFalse(foragePINEditText.pinEditTextState.isFocused)
        assertTrue(foragePINEditText.pinEditTextState.isBlurred)
    }

    /**
     * Integration test of ForagePINEditText and PinTextWatcher,
     * The [PinTextWatcher] is covered in [PinTextWatcherTest]
     */
    @Test
    fun `ensure PinTextWatcher is attached`() {
        val editText = foragePINEditText._editText

        // Use reflection to get the private mListeners field in the TextView class
        val field = TextView::class.java.getDeclaredField("mListeners")
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val textWatchers = field.get(editText) as ArrayList<TextWatcher>
        val pinTextWatcher = textWatchers.find { it is PinTextWatcher }
        assertNotNull(pinTextWatcher)
    }

    @Test
    fun `ensure  input type is TYPE_CLASS_NUMBER with TYPE_NUMBER_VARIATION_PASSWORD`() {
        val editText = foragePINEditText._editText
        val expectedInputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        assertEquals(expectedInputType, editText.inputType)
    }

    @Test
    fun `test EditText max length is set to 4`() {
        val editText = foragePINEditText._editText
        val lengthFilter = editText.filters.find { it is InputFilter.LengthFilter } as InputFilter.LengthFilter
        val maxLengthField = InputFilter.LengthFilter::class.java.getDeclaredField("mMax")
        maxLengthField.isAccessible = true
        val maxLength = maxLengthField.get(lengthFilter) as Int
        assertEquals(4, maxLength)
    }

    @Test
    fun `test EditText gravity is center`() {
        val editText = foragePINEditText._editText
        assertEquals("Gravity should be center", Gravity.CENTER, editText.gravity)
    }

    @Test
    fun `test EditText background has sharp corners by default`() {
        val editText = foragePINEditText._editText
        val background = editText.background
        assertTrue("Background should be an instance of GradientDrawable", background is GradientDrawable)

        val gradientDrawable = background as GradientDrawable
        val cornerRadii = gradientDrawable.cornerRadii

        assertNotNull("Corner radii should not be null", cornerRadii)
        assertTrue("Corner radii should have 8 values", cornerRadii!!.size == 8)
        assertTrue(cornerRadii.all { it == 0f })
    }

    @Test
    fun `has padding of 0 by default`() {
        val editText = foragePINEditText._editText
        assertEquals(0, editText.paddingLeft)
        assertEquals(0, editText.paddingTop)
        assertEquals(0, editText.paddingRight)
        assertEquals(0, editText.paddingBottom)
    }

    @Test
    fun `has transparent background by default`() {
        val editText = foragePINEditText._editText
        val background = editText.background as GradientDrawable
        assertFalse(background.color!!.isOpaque)
    }

    @Test
    fun `has 14 text size by default`() {
        val editText = foragePINEditText._editText
        assertEquals(14.0f, editText.textSize)
    }
}
