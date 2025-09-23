package com.joinforage.forage.android.ecom.ui.element

import android.graphics.Typeface
import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.ui.element.CardExpiredError
import com.joinforage.forage.android.ecom.ui.element.ExpirationElementState.Companion.secondDayOfFollowingMonth
import com.joinforage.forage.android.ecom.ui.element.ExpirationField.Companion.formatExpirationValue
import com.joinforage.forage.android.mock.TestForagePaymentSheet
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.MOCK_SESSION_TOKEN
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.TEST_HSA_FSA_CARD
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.TEST_MERCHANT_ID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class ForagePaymentSheetTest {

    val foragePaymentSheet: ForagePaymentSheet by lazy { TestForagePaymentSheet().create() }

    @Before
    fun before() = foragePaymentSheet.clearText()

    @Test
    fun `Verify cardholder name empty`() {
        assertThat(foragePaymentSheet.cardholderNameField.value).isEqualTo("")
        with(foragePaymentSheet.getElementState().cardholderNameState) {
            assertThat(isEmpty).isTrue
            assertThat(isValid).isFalse
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify cardholder name complete`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardholderNameEditText))
            .perform(ViewActions.typeText("J"))
        assertThat(foragePaymentSheet.cardholderNameField.value).isEqualTo("J")
        with(foragePaymentSheet.getElementState().cardholderNameState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify card number empty`() {
        assertThat(foragePaymentSheet.cardNumberField.value).isEqualTo("")
        with(foragePaymentSheet.getElementState().cardNumberState) {
            assertThat(isEmpty).isTrue
            assertThat(isValid).isFalse
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify card number short`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(forceTypeText("4"))
        assertThat(foragePaymentSheet.cardNumberField.value).isEqualTo("4")
        with(foragePaymentSheet.getElementState().cardNumberState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify card number failed Luhn`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(forceTypeText("4111111111111112"))
        assertThat(foragePaymentSheet.cardNumberField.value).isEqualTo("4111111111111112")
        with(foragePaymentSheet.getElementState().cardNumberState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify card number complete`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(forceTypeText("4111111111111111"))
        assertThat(foragePaymentSheet.cardNumberField.value).isEqualTo("4111111111111111")
        with(foragePaymentSheet.getElementState().cardNumberState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify expiration empty`() {
        assertThat(foragePaymentSheet.expirationField.expirationValueAsString).isEqualTo("")
        assertThatThrownBy { foragePaymentSheet.expirationField.value }
            .isInstanceOf(IllegalStateException::class.java)
        with(foragePaymentSheet.getElementState().expirationState) {
            assertThat(isEmpty).isTrue
            assertThat(isValid).isFalse
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify expiration short`() {
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(forceTypeText("1"))
        assertThatThrownBy { foragePaymentSheet.expirationField.value }
            .isInstanceOf(IllegalStateException::class.java)
        with(foragePaymentSheet.getElementState().expirationState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify expiration complete`() {
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(forceTypeText("1234"))
        assertThat(foragePaymentSheet.expirationField.value).isEqualTo(Pair(12, 2034))
        with(foragePaymentSheet.getElementState().expirationState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify firstDayOfFollowingMonth`() {
        assertThat(secondDayOfFollowingMonth(Pair(1, 2025))).isEqualTo(secondDayOfGivenMonth(2, 2025))
        assertThat(secondDayOfFollowingMonth(Pair(11, 2025))).isEqualTo(secondDayOfGivenMonth(12, 2025))
        assertThat(secondDayOfFollowingMonth(Pair(12, 2025))).isEqualTo(secondDayOfGivenMonth(1, 2026))
    }

    @Test
    fun `Verify expired card`() {
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(forceTypeText("0125"))
        assertThat(foragePaymentSheet.expirationField.value).isEqualTo(Pair(1, 2025))
        with(foragePaymentSheet.getElementState().expirationState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isFalse
            assertThat(validationError).isEqualTo(CardExpiredError)
        }
    }

    @Test
    fun `Verify unexpired card`() {
        val nextYear = Calendar.getInstance().get(Calendar.YEAR) + 1
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(forceTypeText(formatExpirationValue(Pair(1, nextYear))))
        assertThat(foragePaymentSheet.expirationField.value).isEqualTo(Pair(1, nextYear))
        with(foragePaymentSheet.getElementState().expirationState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    private fun secondDayOfGivenMonth(month: Int, year: Int): Calendar {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        calendar.set(year, month - 1, 2, 0, 0, 0)
        return calendar
    }

    @Test
    fun `Verify security code empty`() {
        assertThat(foragePaymentSheet.securityCodeField.value).isEqualTo("")
        with(foragePaymentSheet.getElementState().securityCodeState) {
            assertThat(isEmpty).isTrue
            assertThat(isValid).isFalse
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify security code short`() {
        Espresso.onView(ViewMatchers.withId(R.id.securityCodeEditText))
            .perform(forceTypeText("1"))
        assertThat(foragePaymentSheet.securityCodeField.value).isEqualTo("1")
        with(foragePaymentSheet.getElementState().securityCodeState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify security code complete`() {
        Espresso.onView(ViewMatchers.withId(R.id.securityCodeEditText))
            .perform(forceTypeText("123"))
        assertThat(foragePaymentSheet.securityCodeField.value).isEqualTo("123")
        with(foragePaymentSheet.getElementState().securityCodeState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue()
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify zip code empty`() {
        assertThat(foragePaymentSheet.zipCodeField.value).isEqualTo("")
        with(foragePaymentSheet.getElementState().zipCodeState) {
            assertThat(isEmpty).isTrue
            assertThat(isValid).isFalse
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify zip code short`() {
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(forceTypeText("1"))
        assertThat(foragePaymentSheet.zipCodeField.value).isEqualTo("1")
        with(foragePaymentSheet.getElementState().zipCodeState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify zip code complete`() {
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(forceTypeText("12345"))
        assertThat(foragePaymentSheet.zipCodeField.value).isEqualTo("12345")
        with(foragePaymentSheet.getElementState().zipCodeState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify zip code complete 9 digits`() {
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(forceTypeText("12345-6789"))
        assertThat(foragePaymentSheet.zipCodeField.value).isEqualTo("12345-6789")
        with(foragePaymentSheet.getElementState().zipCodeState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify combined state empty`() {
        with(foragePaymentSheet.getElementState()) {
            assertThat(isEmpty).isTrue
            assertThat(isValid).isFalse
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify combined state non-empty`() {
        foragePaymentSheet.cardholderNameField.value = "John Doe"
        with(foragePaymentSheet.getElementState()) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isFalse
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify combined state complete`() {
        foragePaymentSheet.cardholderNameField.value = "John Doe"
        foragePaymentSheet.cardNumberField.value = TEST_HSA_FSA_CARD
        foragePaymentSheet.expirationField.value = Pair(12, 34)
        foragePaymentSheet.securityCodeField.value = "123"
        foragePaymentSheet.zipCodeField.value = "12345"
        with(foragePaymentSheet.getElementState()) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify ForageConfig accessors`() {
        val forageConfig = ForageConfig(TEST_MERCHANT_ID, MOCK_SESSION_TOKEN)
        foragePaymentSheet.setForageConfig(forageConfig)
        assertThat(foragePaymentSheet.getForageConfig()).isEqualTo(forageConfig)
    }

    @Test
    fun `Verify clearText`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardholderNameEditText))
            .perform(ViewActions.typeText("John Doe"))
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(forceTypeText(TEST_HSA_FSA_CARD))
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(forceTypeText("1234"))
        Espresso.onView(ViewMatchers.withId(R.id.securityCodeEditText))
            .perform(forceTypeText("123"))
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(forceTypeText("12345"))
        with(foragePaymentSheet) {
            assertThat(cardholderNameField.value).isNotEmpty()
            assertThat(cardNumberField.value).isNotEmpty()
            assertThat(expirationField.expirationValueAsString).isNotEmpty()
            assertThat(securityCodeField.value).isNotEmpty()
            assertThat(zipCodeField.value).isNotEmpty()
            clearText()
            assertThat(cardholderNameField.value).isEmpty()
            assertThat(cardNumberField.value).isEmpty()
            assertThat(expirationField.expirationValueAsString).isEmpty()
            assertThat(securityCodeField.value).isEmpty()
            assertThat(zipCodeField.value).isEmpty()
        }
    }

    @Test
    fun `Verify typeface`() {
        foragePaymentSheet.typeface = Typeface.DEFAULT
        assertThat(foragePaymentSheet.typeface).isEqualTo(Typeface.DEFAULT)
        foragePaymentSheet.typeface = Typeface.MONOSPACE
        assertThat(foragePaymentSheet.typeface).isEqualTo(Typeface.MONOSPACE)
    }

    @Test
    fun `Verify setTextColor`() {
        val specialColor = 0x808080
        with(foragePaymentSheet) {
            setTextColor(specialColor)
            assertThat(cardholderNameField.editText.textColors.defaultColor).isEqualTo(specialColor)
            assertThat(cardNumberField.editText.textColors.defaultColor).isEqualTo(specialColor)
            assertThat(expirationField.editText.textColors.defaultColor).isEqualTo(specialColor)
            assertThat(securityCodeField.editText.textColors.defaultColor).isEqualTo(specialColor)
            assertThat(zipCodeField.editText.textColors.defaultColor).isEqualTo(specialColor)
        }
    }

    @Test
    fun `Verify setTextSize`() {
        val specialTextSize = 2.0f
        with(foragePaymentSheet) {
            setTextSize(specialTextSize)
            assertThat(cardholderNameField.editText.textSize).isEqualTo(specialTextSize)
            assertThat(cardNumberField.editText.textSize).isEqualTo(specialTextSize)
            assertThat(expirationField.editText.textSize).isEqualTo(specialTextSize)
            assertThat(securityCodeField.editText.textSize).isEqualTo(specialTextSize)
            assertThat(zipCodeField.editText.textSize).isEqualTo(specialTextSize)
        }
    }

    @Test
    fun `Verify setOnChangeEventListener`() {
        var changeCount = 0
        foragePaymentSheet.setOnChangeEventListener { ++changeCount }
        Espresso.onView(ViewMatchers.withId(R.id.cardholderNameEditText))
            .perform(ViewActions.typeText("John Doe"))
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(forceTypeText(TEST_HSA_FSA_CARD))
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(forceTypeText("1234"))
        Espresso.onView(ViewMatchers.withId(R.id.securityCodeEditText))
            .perform(forceTypeText("123"))
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(forceTypeText("12345"))
        assertThat(changeCount).isEqualTo(36)
    }

    @Test
    fun `Verify ElementStateAdapter toString`() {
        val fourValueString = foragePaymentSheet.getElementState().toString()
        val fourValueList = fourValueString.split(' ')
        assertThat(fourValueList).hasSizeGreaterThanOrEqualTo(4)
        for (booleanExpected in fourValueList.subList(0, 3))
            assertThat(booleanExpected.toBoolean().toString()).isEqualTo(booleanExpected)
    }

    @Test
    fun `Verify cardholderNameField setHint`() {
        val randomHint = "random-hint"
        assertThat(foragePaymentSheet.cardholderNameField.editText.hint).isNotEqualTo(randomHint)
        foragePaymentSheet.cardholderNameField.setHint(randomHint)
        assertThat(foragePaymentSheet.cardholderNameField.editText.hint).isEqualTo(randomHint)
    }

    @Test
    fun `Verify cardholderNameField setHintTextColor`() {
        val randomColor = 0x01234567
        assertThat(foragePaymentSheet.cardholderNameField.layout.defaultHintTextColor?.defaultColor).isNotEqualTo(randomColor)
        foragePaymentSheet.cardholderNameField.setHintTextColor(randomColor)
        assertThat(foragePaymentSheet.cardholderNameField.layout.defaultHintTextColor?.defaultColor).isEqualTo(randomColor)
    }

    @Test
    fun `Verify cardholderNameField setBoxStrokeColor`() {
        val randomColor = 0x01234567
        assertThat(foragePaymentSheet.cardholderNameField.boxStrokeColor).isNotEqualTo(randomColor)
        foragePaymentSheet.cardholderNameField.setBoxStrokeColor(randomColor)
        assertThat(foragePaymentSheet.cardholderNameField.boxStrokeColor).isEqualTo(randomColor)
    }

    @Test
    fun `Verify cardholderNameField setBoxStrokeWidth`() {
        val randomWidth = 4321
        assertThat(foragePaymentSheet.cardholderNameField.boxStrokeWidth).isNotEqualTo(randomWidth)
        foragePaymentSheet.cardholderNameField.setBoxStrokeWidth(randomWidth)
        assertThat(foragePaymentSheet.cardholderNameField.boxStrokeWidth).isEqualTo(randomWidth)
    }

    @Test
    fun `Verify setOnFocusEventListener`() {
        foragePaymentSheet.cardNumberField.requestFocus()
        assertThat(foragePaymentSheet.cardholderNameField.editText.isFocused).isFalse()
        var gotFocusEvent = false
        foragePaymentSheet.cardholderNameField.setOnFocusEventListener { gotFocusEvent = true }
        foragePaymentSheet.cardholderNameField.requestFocus()
        assertThat(foragePaymentSheet.cardholderNameField.editText.isFocused).isTrue()
        assertThat(gotFocusEvent).isTrue()
    }

    @Test
    fun `Verify setOnBlurEventListener`() {
        foragePaymentSheet.cardholderNameField.requestFocus()
        assertThat(foragePaymentSheet.cardholderNameField.editText.isFocused).isTrue()
        var gotBlurEvent = false
        foragePaymentSheet.cardholderNameField.setOnBlurEventListener { gotBlurEvent = true }
        foragePaymentSheet.cardNumberField.requestFocus()
        assertThat(foragePaymentSheet.cardholderNameField.editText.isFocused).isFalse()
        assertThat(gotBlurEvent).isTrue()
    }

    //
    // For an EditText with inputType="number", Espresso's typeText() doesn't have any impact
    // when run on Robolectric.
    //
    // https://github.com/robolectric/robolectric/issues/5110#issuecomment-501744719
    //
    private fun forceTypeText(text: String): ViewAction {
        return object : ViewAction {
            override fun getDescription() = "force type text"

            override fun getConstraints() = ViewMatchers.isEnabled()

            override fun perform(uiController: UiController?, view: View?) {
                val editText = view as EditText
                text.forEach { c -> editText.append(c.toString()) }
                uiController?.loopMainThreadUntilIdle()
            }
        }
    }
}
