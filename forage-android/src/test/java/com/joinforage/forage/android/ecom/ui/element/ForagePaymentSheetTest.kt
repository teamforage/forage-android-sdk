package com.joinforage.forage.android.ecom.ui.element

import com.joinforage.forage.android.mock.MockForagePaymentSheet.Companion.TEST_HSA_FSA_CARD
import com.joinforage.forage.android.mock.MockForagePaymentSheet.Companion.createMockForagePaymentSheet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForagePaymentSheetTest {
    @Test
    fun `Verify CardholderNameElementState empty`() {
        val paymentSheet = createMockForagePaymentSheet(cardholderName = "")
        val state = paymentSheet.CardholderNameElementState()
        assertThat(state.isEmpty).isTrue
        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify CardholderNameElementState complete`() {
        val paymentSheet = createMockForagePaymentSheet(cardholderName = "J")
        val state = paymentSheet.CardholderNameElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `Verify CardNumberElementState empty`() {
        val paymentSheet = createMockForagePaymentSheet(cardNumber = "")
        val state = paymentSheet.CardNumberElementState()
        assertThat(state.isEmpty).isTrue
        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify CardNumberElementState short`() {
        val paymentSheet = createMockForagePaymentSheet(cardNumber = "4")
        val state = paymentSheet.CardNumberElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify CardNumberElementState failed Luhn`() {
        val paymentSheet = createMockForagePaymentSheet(cardNumber = "4111111111111112")
        val state = paymentSheet.CardNumberElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify CardNumberElementState complete`() {
        val paymentSheet = createMockForagePaymentSheet(cardNumber = "4111111111111111")
        val state = paymentSheet.CardNumberElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `Verify ExpirationElementState empty`() {
        val paymentSheet = createMockForagePaymentSheet(expiration = "")
        val state = paymentSheet.ExpirationElementState()
        assertThat(state.isEmpty).isTrue
        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify ExpirationElementState short`() {
        val paymentSheet = createMockForagePaymentSheet(expiration = "1")
        val state = paymentSheet.ExpirationElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify ExpirationElementState complete`() {
        val paymentSheet = createMockForagePaymentSheet(expiration = "12/34")
        val state = paymentSheet.ExpirationElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `Verify SecurityCodeElementState empty`() {
        val paymentSheet = createMockForagePaymentSheet(securityCode = "")
        val state = paymentSheet.SecurityCodeElementState()
        assertThat(state.isEmpty).isTrue
        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify SecurityCodeElementState short`() {
        val paymentSheet = createMockForagePaymentSheet(securityCode = "1")
        val state = paymentSheet.SecurityCodeElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify SecurityCodeElementState complete`() {
        val paymentSheet = createMockForagePaymentSheet(securityCode = "111")
        val state = paymentSheet.SecurityCodeElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `Verify ZipCodeElementState empty`() {
        val paymentSheet = createMockForagePaymentSheet(zipCode = "")
        val state = paymentSheet.ZipCodeElementState()
        assertThat(state.isEmpty).isTrue
        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify ZipCodeElementState short`() {
        val paymentSheet = createMockForagePaymentSheet(zipCode = "1")
        val state = paymentSheet.ZipCodeElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify ZipCodeElementState complete`() {
        val paymentSheet = createMockForagePaymentSheet(zipCode = "10001")
        val state = paymentSheet.ZipCodeElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `Verify ZipCodeElementState complete 9 digits`() {
        val paymentSheet = createMockForagePaymentSheet(zipCode = "10001-1234")
        val state = paymentSheet.ZipCodeElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `Verify CombinedElementState empty`() {
        val paymentSheet = createMockForagePaymentSheet()
        val state = paymentSheet.getElementState()
        assertThat(state.isEmpty).isTrue
        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify CombinedElementState non-empty`() {
        val paymentSheet = createMockForagePaymentSheet(cardholderName = "X")
        val state = paymentSheet.getElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNotNull
    }

    @Test
    fun `Verify CombinedElementState complete`() {
        val paymentSheet = createMockForagePaymentSheet(
            cardholderName = "X",
            cardNumber = TEST_HSA_FSA_CARD,
            expiration = "12/34",
            securityCode = "111",
            zipCode = "10001"
        )
        val state = paymentSheet.getElementState()
        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }
}
