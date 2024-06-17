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
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.ui.textwatcher.PinTextWatcher
import com.joinforage.forage.android.ecom.ui.vault.forage.RosettaPinElement
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
class RosettaPinElementTests {

    private lateinit var context: Context
    private lateinit var rosettaPinElement: RosettaPinElement

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        rosettaPinElement = RosettaPinElement(context)
    }

    @Test
    fun testVaultType() {
        assertEquals(VaultType.FORAGE_VAULT_TYPE, rosettaPinElement.vaultType)
    }

    @Test
    fun testShowKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        rosettaPinElement.showKeyboard()
        val shadowImm = Shadows.shadowOf(imm)
        assertTrue(shadowImm.isSoftInputVisible)
    }

    @Test
    fun testClearText() {
        rosettaPinElement.getTextElement().setText("1234")
        assertEquals("1234", rosettaPinElement.getTextElement().text.toString())

        rosettaPinElement.clearText()
        assertEquals("", rosettaPinElement.getTextElement().text.toString())
    }

    @Test
    fun testGetTextElement() {
        assertTrue(rosettaPinElement.getTextElement() is EditText)
    }

    @Test
    fun testTypeface() {
        val typeface = Typeface.DEFAULT_BOLD
        rosettaPinElement.typeface = typeface
        assertEquals(typeface, rosettaPinElement.typeface)
    }

    @Test
    fun testSetTextColor() {
        val color = Color.RED
        rosettaPinElement.setTextColor(color)
        assertEquals(color, (rosettaPinElement.getTextElement()).currentTextColor)
    }

    @Test
    fun testSetTextSize() {
        val textSize = 20f
        rosettaPinElement.setTextSize(textSize)
        assertEquals(textSize, (rosettaPinElement.getTextElement()).textSize)
    }

    @Test
    fun `ensure onFocusChange`() {
        val editText = rosettaPinElement.getTextElement()
        val focusChangeListener = editText.onFocusChangeListener

        focusChangeListener.onFocusChange(editText, true)

        assertTrue(rosettaPinElement.pinEditTextState.isFocused)
        assertFalse(rosettaPinElement.pinEditTextState.isBlurred)

        focusChangeListener.onFocusChange(editText, false)
        assertFalse(rosettaPinElement.pinEditTextState.isFocused)
        assertTrue(rosettaPinElement.pinEditTextState.isBlurred)
    }

    /**
     * Integration test of RosettaPinElement and PinTextWatcher,
     * The [PinTextWatcher] is covered in [PinTextWatcherTest]
     */
    @Test
    fun `ensure PinTextWatcher is attached`() {
        val editText = rosettaPinElement.getTextElement()

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
        val editText = rosettaPinElement.getTextElement()
        val expectedInputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        assertEquals(expectedInputType, editText.inputType)
    }

    @Test
    fun `test EditText max length is set to 4`() {
        val editText = rosettaPinElement.getTextElement()
        val lengthFilter = editText.filters.find { it is InputFilter.LengthFilter } as InputFilter.LengthFilter
        val maxLengthField = InputFilter.LengthFilter::class.java.getDeclaredField("mMax")
        maxLengthField.isAccessible = true
        val maxLength = maxLengthField.get(lengthFilter) as Int
        assertEquals(4, maxLength)
    }

    @Test
    fun `test EditText gravity is center`() {
        val editText = rosettaPinElement.getTextElement()
        assertEquals("Gravity should be center", Gravity.CENTER, editText.gravity)
    }

    @Test
    fun `test EditText background has sharp corners by default`() {
        val editText = rosettaPinElement.getTextElement()
        val background = editText.background
        assertTrue("Background should be an instance of GradientDrawable", background is GradientDrawable)

        val gradientDrawable = background as GradientDrawable
        val cornerRadii = gradientDrawable.cornerRadii

        assertNotNull("Corner radii should not be null", cornerRadii)
        assertTrue("Corner radii should have 8 values", cornerRadii!!.size == 8)
        assertTrue(cornerRadii.all { it == 0f })
    }

    @Test
    fun `has padding of 20 by default`() {
        val editText = rosettaPinElement.getTextElement()
        assertEquals(20, editText.paddingLeft)
        assertEquals(20, editText.paddingTop)
        assertEquals(20, editText.paddingRight)
        assertEquals(20, editText.paddingBottom)
    }

    @Test
    fun `has transparent background by default`() {
        val editText = rosettaPinElement.getTextElement()
        val background = editText.background as GradientDrawable
        assertFalse(background.color!!.isOpaque)
    }

    @Test
    fun `has 14 text size by default`() {
        val editText = rosettaPinElement.getTextElement()
        assertEquals(14.0f, editText.textSize)
    }
}
