package com.joinforage.forage.android.core.element.state.pan

import com.joinforage.forage.android.core.ui.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.ui.element.InvalidEbtPanError
import com.joinforage.forage.android.core.ui.element.TooLongEbtPanError
import com.joinforage.forage.android.core.ui.element.state.pan.PanInputState
import com.joinforage.forage.android.core.ui.element.state.pan.USState
import com.joinforage.forage.android.core.ui.element.state.pan.WhitelistedCardsValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StrictForEmptyInputTest {

    @Test
    fun `cardNumber as empty string`() {
        val state = PanInputState.forEmptyInput().handleChangeEvent("")

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNull()
        assertThat(state.usState).isNull()
    }

    @Test
    fun `cardNumber too short to contain  IIN`() {
        val tooShortNoIIN: String = "420"
        val state = PanInputState.forEmptyInput().handleChangeEvent(tooShortNoIIN)

        // we want to consider a card valid as long as we don't have
        // a definitive reason to think it is invalid. If we don't
        // have enough digits to know if its a valid StateIIN, then
        // we'll consider it valid for now.
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(IncompleteEbtPanError)
        assertThat(state.usState).isNull()
    }

    @Test
    fun `cardNumber has non-existent IIN`() {
        val invalidIIN: String = "420420420"
        val state = PanInputState.forEmptyInput().handleChangeEvent(invalidIIN)

        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(InvalidEbtPanError)
        assertThat(state.usState).isNull()
    }

    @Test
    fun `cardNumber has valid state IIN but is shorter than expected length`() {
        val tooShortMaineNumber: String = "507703111" // Maine is 507703
        val state = PanInputState.forEmptyInput().handleChangeEvent(tooShortMaineNumber)

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(IncompleteEbtPanError)
        assertThat(state.usState).isEqualTo(USState.MAINE)
    }

    @Test
    fun `cardNumber has valid state IIN and is correct length`() {
        val okMaineNumber: String = "5077031111111111111" // Maine is 507703
        val state = PanInputState.forEmptyInput().handleChangeEvent(okMaineNumber)

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
        assertThat(state.usState).isEqualTo(USState.MAINE)
    }

    @Test
    fun `cardNumber has valid state IIN and is too long`() {
        // NOTE: we expect the view to enforce max length based on IIN
        // but for good measure we'll make sure validation knows to handle
        // this case
        val longMaineNumber: String = "50770311111111111110" // Maine is 507703
        val state = PanInputState.forEmptyInput().handleChangeEvent(longMaineNumber)

        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(TooLongEbtPanError)
        assertThat(state.usState).isEqualTo(USState.MAINE)
    }

    @Test
    fun `cardNumber is the shared ND-SD card number`() {
        val longMaineNumber: String = "5081321111111111" // North Dakota/South Dakota is 508132
        val state = PanInputState.forEmptyInput().handleChangeEvent(longMaineNumber)

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
        assertThat(state.usState).isEqualTo(USState.SOUTH_DAKOTA)
    }
}

class DEV_ONLY_IntegrationTests {
    @Test
    fun `StrictEbtValidator - correctly flags valid`() {
        val okMaineNumber: String = "5077031111111111111" // Maine is 507703
        val state = PanInputState.NON_PROD_forEmptyInput().handleChangeEvent(okMaineNumber)

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
        assertThat(state.usState).isEqualTo(USState.MAINE)
    }

    @Test
    fun `PaymentCaptureErrorCard - correctly flags valid`() {
        val whitelistedPAN: String = "4444444444444412345"
        val state = PanInputState.NON_PROD_forEmptyInput().handleChangeEvent(whitelistedPAN)

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.usState).isNull()
    }

    @Test
    fun `BalanceCheckErrorCard - correctly flags valid`() {
        val whitelistedPAN: String = "5555555555555512345"
        val state = PanInputState.NON_PROD_forEmptyInput().handleChangeEvent(whitelistedPAN)

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.usState).isNull()
    }

    @Test
    fun `NonProdValidEbtCard - correctly flags valid`() {
        val whitelistedPAN: String = "9999420420420420420"
        val state = PanInputState.NON_PROD_forEmptyInput().handleChangeEvent(whitelistedPAN)

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.usState).isNull()
    }

    @Test
    fun `EmptyEbtCashBalanceCard - correctly flags valid`() {
        val whitelistedPAN: String = "6543210000000000000"
        val state = PanInputState.NON_PROD_forEmptyInput().handleChangeEvent(whitelistedPAN)

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.usState).isNull()
    }

    @Test
    fun `empty string is valid`() {
        val state = PanInputState.NON_PROD_forEmptyInput().handleChangeEvent("")

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNull()
        assertThat(state.usState).isNull()
    }

    @Test
    fun `non-whitelisted invalid Ebt Pan should be invalid`() {
        val state = PanInputState.NON_PROD_forEmptyInput().handleChangeEvent("4204204204204204204")

        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(InvalidEbtPanError)
        assertThat(state.usState).isNull()
    }
}

class TestWhitelistedCards {
    @Test
    fun `prefix can be repeated arbitrary times`() {
        val whitelistValidator = WhitelistedCardsValidator("hello", 2)
        val validStr = "hellohello"
        assertThat(whitelistValidator.checkIfValid(validStr)).isTrue
    }

    @Test
    fun `complete accepts length 16 card number`() {
        val whitelistValidator = WhitelistedCardsValidator("hello", 2)
        val completeStr = "hellohello******"
        assertThat(whitelistValidator.checkIfComplete(completeStr)).isTrue
    }

    @Test
    fun `complete accepts length 19 card number`() {
        val whitelistValidator = WhitelistedCardsValidator("hello", 2)
        val completeStr = "hellohello*********"
        assertThat(whitelistValidator.checkIfComplete(completeStr)).isTrue
    }

    @Test
    fun `complete rejects too short and too long numbers`() {
        val whitelistValidator = WhitelistedCardsValidator("hello", 2)

        val tooShort = "hellohello"
        assertThat(whitelistValidator.checkIfValid(tooShort)).isTrue
        assertThat(whitelistValidator.checkIfComplete(tooShort)).isFalse

        val tooLong = "hellohello****************"
        assertThat(whitelistValidator.checkIfValid(tooLong)).isTrue
        assertThat(whitelistValidator.checkIfComplete(tooLong)).isFalse
    }

    @Test
    fun `complete and valid require matching prefix`() {
        val whitelistValidator = WhitelistedCardsValidator("hello", 2)

        val invalidCompleteStr = "hello12345*********"
        assertThat(whitelistValidator.checkIfValid(invalidCompleteStr)).isFalse
        assertThat(whitelistValidator.checkIfComplete(invalidCompleteStr)).isFalse

        val completeStr = "hellohello*********"
        assertThat(whitelistValidator.checkIfValid(completeStr)).isTrue
        assertThat(whitelistValidator.checkIfComplete(completeStr)).isTrue
    }

    @Test
    fun `validationError is always null`() {
        val whitelistValidator = WhitelistedCardsValidator("hello", 2)

        val emptyStr = ""
        assertThat(whitelistValidator.checkForValidationError(emptyStr)).isNull()

        val shortAndDoesNotMatchPrefixStr = "hello12345"
        assertThat(whitelistValidator.checkForValidationError(shortAndDoesNotMatchPrefixStr)).isNull()

        val correctLengthAndDoesNotMatchPrefixStr = "hello12345*********"
        assertThat(whitelistValidator.checkForValidationError(correctLengthAndDoesNotMatchPrefixStr)).isNull()

        val validIncompleteStr = "hellohello"
        assertThat(whitelistValidator.checkForValidationError(validIncompleteStr)).isNull()

        val completeStr = "hellohello*********"
        assertThat(whitelistValidator.checkForValidationError(completeStr)).isNull()
    }
}

class PanIsEmptyTest {
    @Test
    fun `cardNumber is length 0`() {
        val state = PanInputState.forEmptyInput().handleChangeEvent("")
        assertThat(state.isEmpty).isTrue
    }

    @Test
    fun `cardNumber is not empty`() {
        val state = PanInputState.forEmptyInput().handleChangeEvent("1")
        assertThat(state.isEmpty).isFalse
    }
}

class PanHandleChangeEventTest {
    @Test
    fun `valid card 16-digit card number passes correct state to callback`() {
        val state = PanInputState.forEmptyInput().handleChangeEvent("5076807890123456")

        // TODO: its not clear that we should make any assertions
        //  about the resulting state of isFocus or isBlur. Should
        //  handle events update the focus / blurred state??

        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
        assertThat(state.usState).isEqualTo(USState.ALABAMA)
    }

    @Test
    fun `strips all non-digit characters before processing`() {
        val validStringContaminatedByOtherChars = "!@# $%^ &*()_+<>? abcd5076807890123456"
        val state = PanInputState.forEmptyInput().handleChangeEvent(validStringContaminatedByOtherChars)

        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
        assertThat(state.usState).isEqualTo(USState.ALABAMA)
    }
}
