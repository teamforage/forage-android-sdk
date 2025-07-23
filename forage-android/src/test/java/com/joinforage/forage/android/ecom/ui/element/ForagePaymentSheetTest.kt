package com.joinforage.forage.android.ecom.ui.element

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.mock.MockForagePaymentSheet.Companion.TEST_HSA_FSA_CARD
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForagePaymentSheetTest {

    class SimpleFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = ForagePaymentSheet(inflater.context)
    }

    val foragePaymentSheet: ForagePaymentSheet by lazy {
        val fragment = SimpleFragment()
        launchFragmentInContainer(
            themeResId = com.google.android.material.R.style.Theme_AppCompat,
            instantiate = { fragment }
        )
        fragment.view as ForagePaymentSheet
    }

    @Before
    fun before() {
        foragePaymentSheet.clearText()
    }

    @Test
    fun `Verify cardholder name empty`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardholderNameEditText))
            .perform(ViewActions.replaceText(""))
        assertThat(foragePaymentSheet.cardholderNameValue).isEqualTo("")
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
            .perform(ViewActions.replaceText("J"))
        assertThat(foragePaymentSheet.cardholderNameValue).isEqualTo("J")
        with(foragePaymentSheet.getElementState().cardholderNameState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify card number empty`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(ViewActions.replaceText(""))
        assertThat(foragePaymentSheet.cardNumberValue).isEqualTo("")
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
            .perform(ViewActions.replaceText("4"))
        assertThat(foragePaymentSheet.cardNumberValue).isEqualTo("4")
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
            .perform(ViewActions.replaceText("4111111111111112"))
        assertThat(foragePaymentSheet.cardNumberValue).isEqualTo("4111111111111112")
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
            .perform(ViewActions.replaceText("4111111111111111"))
        assertThat(foragePaymentSheet.cardNumberValue).isEqualTo("4111111111111111")
        with(foragePaymentSheet.getElementState().cardNumberState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify expiration empty`() {
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(ViewActions.replaceText(""))
        assertThatThrownBy { foragePaymentSheet.expirationValue }
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
            .perform(ViewActions.replaceText("1"))
        assertThatThrownBy { foragePaymentSheet.expirationValue }
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
            .perform(ViewActions.replaceText("1234"))
        assertThat(foragePaymentSheet.expirationValue).isEqualTo(Pair(12, 2034))
        with(foragePaymentSheet.getElementState().expirationState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify security code empty`() {
        Espresso.onView(ViewMatchers.withId(R.id.securityCodeEditText))
            .perform(ViewActions.replaceText(""))
        assertThat(foragePaymentSheet.securityCodeValue).isEqualTo("")
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
            .perform(ViewActions.replaceText("1"))
        assertThat(foragePaymentSheet.securityCodeValue).isEqualTo("1")
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
            .perform(ViewActions.replaceText("123"))
        assertThat(foragePaymentSheet.securityCodeValue).isEqualTo("123")
        with(foragePaymentSheet.getElementState().securityCodeState) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue()
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify zip code empty`() {
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(ViewActions.replaceText(""))
        assertThat(foragePaymentSheet.zipCodeValue).isEqualTo("")
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
            .perform(ViewActions.replaceText("1"))
        assertThat(foragePaymentSheet.zipCodeValue).isEqualTo("1")
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
            .perform(ViewActions.replaceText("12345"))
        assertThat(foragePaymentSheet.zipCodeValue).isEqualTo("12345")
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
            .perform(ViewActions.replaceText("12345-6789"))
        assertThat(foragePaymentSheet.zipCodeValue).isEqualTo("12345-6789")
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
        Espresso.onView(ViewMatchers.withId(R.id.cardholderNameEditText))
            .perform(ViewActions.replaceText("J"))
        with(foragePaymentSheet.getElementState()) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isFalse
            assertThat(isComplete).isFalse
            assertThat(validationError).isNotNull
        }
    }

    @Test
    fun `Verify combined state complete`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardholderNameEditText))
            .perform(ViewActions.replaceText("J"))
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(ViewActions.replaceText(TEST_HSA_FSA_CARD))
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(ViewActions.replaceText("1234"))
        Espresso.onView(ViewMatchers.withId(R.id.securityCodeEditText))
            .perform(ViewActions.replaceText("123"))
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(ViewActions.replaceText("12345"))
        with(foragePaymentSheet.getElementState()) {
            assertThat(isEmpty).isFalse
            assertThat(isValid).isTrue
            assertThat(isComplete).isTrue
            assertThat(validationError).isNull()
        }
    }

    @Test
    fun `Verify get+set ForageConfig`() {
        val forageConfig = ForageConfig("merchant-id", "session-token")
        foragePaymentSheet.setForageConfig(forageConfig)
        assertThat(foragePaymentSheet.getForageConfig()).isEqualTo(forageConfig)
    }

    @Test
    fun `Verify clearText`() {
        Espresso.onView(ViewMatchers.withId(R.id.cardholderNameEditText))
            .perform(ViewActions.replaceText("J"))
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(ViewActions.replaceText(TEST_HSA_FSA_CARD))
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(ViewActions.replaceText("1234"))
        Espresso.onView(ViewMatchers.withId(R.id.securityCodeEditText))
            .perform(ViewActions.replaceText("123"))
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(ViewActions.replaceText("12345"))
        with(foragePaymentSheet) {
            assertThat(cardholderNameValue).isNotEmpty()
            assertThat(cardNumberValue).isNotEmpty()
            assertThat(expirationValueAsString).isNotEmpty()
            assertThat(securityCodeValue).isNotEmpty()
            assertThat(zipCodeValue).isNotEmpty()
            clearText()
            assertThat(cardholderNameValue).isEmpty()
            assertThat(cardNumberValue).isEmpty()
            assertThat(expirationValueAsString).isEmpty()
            assertThat(securityCodeValue).isEmpty()
            assertThat(zipCodeValue).isEmpty()
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
            assertThat(cardholderNameEditText.textColors.defaultColor).isEqualTo(specialColor)
            assertThat(cardNumberEditText.textColors.defaultColor).isEqualTo(specialColor)
            assertThat(expirationEditText.textColors.defaultColor).isEqualTo(specialColor)
            assertThat(securityCodeEditText.textColors.defaultColor).isEqualTo(specialColor)
            assertThat(zipCodeEditText.textColors.defaultColor).isEqualTo(specialColor)
        }
    }

    @Test
    fun `Verify setTextSize`() {
        val specialTextSize = 2.0f
        with(foragePaymentSheet) {
            setTextSize(specialTextSize)
            assertThat(cardholderNameEditText.textSize).isEqualTo(specialTextSize)
            assertThat(cardNumberEditText.textSize).isEqualTo(specialTextSize)
            assertThat(expirationEditText.textSize).isEqualTo(specialTextSize)
            assertThat(securityCodeEditText.textSize).isEqualTo(specialTextSize)
            assertThat(zipCodeEditText.textSize).isEqualTo(specialTextSize)
        }
    }

    @Test
    fun `Verify setOnChangeEventListener`() {
        var changeCount = 0
        foragePaymentSheet.setOnChangeEventListener { ++changeCount }
        Espresso.onView(ViewMatchers.withId(R.id.cardholderNameEditText))
            .perform(ViewActions.replaceText("J"))
        Espresso.onView(ViewMatchers.withId(R.id.cardNumberEditText))
            .perform(ViewActions.replaceText(TEST_HSA_FSA_CARD))
        Espresso.onView(ViewMatchers.withId(R.id.expirationEditText))
            .perform(ViewActions.replaceText("1234"))
        Espresso.onView(ViewMatchers.withId(R.id.securityCodeEditText))
            .perform(ViewActions.replaceText("123"))
        Espresso.onView(ViewMatchers.withId(R.id.zipCodeEditText))
            .perform(ViewActions.replaceText("12345"))
        assertThat(changeCount).isEqualTo(5)
    }
}
