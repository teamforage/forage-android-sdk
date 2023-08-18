package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.element.InvalidEbtPanError
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.TooLongEbtPanError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StrictForEmptyInputTest {

    @Test
    fun `cardNumber as empty string`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("")
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `cardNumber too short to contain  IIN`() {
        val tooShortNoIIN: String = "420"
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(tooShortNoIIN)
        val state = manager.getState()

        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(IncompleteEbtPanError)
    }

    @Test
    fun `cardNumber has non-existent IIN`() {
        val invalidIIN: String = "420420420"
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(invalidIIN)
        val state = manager.getState()

        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(InvalidEbtPanError)
    }

    @Test
    fun `cardNumber has valid state IIN but is shorter than expected length`() {
        val tooShortMaineNumber: String = "507703111" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(tooShortMaineNumber)
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(IncompleteEbtPanError)
    }

    @Test
    fun `cardNumber has valid state IIN and is correct length`() {
        val okMaineNumber: String = "5077031111111111111" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(okMaineNumber)
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `cardNumber has valid state IIN and is too long`() {
        // NOTE: we expect the view to enforce max length based on IIN
        // but for good measure we'll make sure validation knows to handle
        // this case
        val longMaineNumber: String = "50770311111111111110" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(longMaineNumber)
        val state = manager.getState()

        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(TooLongEbtPanError)
    }
}

class DEV_ONLY_IntegrationTests {
    @Test
    fun `StrictEbtValidator - correctly flags valid`() {
        val okMaineNumber: String = "5077031111111111111" // Maine is 507703
        val manager = PanElementStateManager.DEV_ONLY_forEmptyInput()
        manager.handleChangeEvent(okMaineNumber)
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `PaymentCaptureErrorCard - correctly flags valid`() {
        val whitelistedPAN: String = "4444444444444412345"
        val manager = PanElementStateManager.DEV_ONLY_forEmptyInput()
        manager.handleChangeEvent(whitelistedPAN)
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
    }

    @Test
    fun `BalanceCheckErrorCard - correctly flags valid`() {
        val whitelistedPAN: String = "5555555555555512345"
        val manager = PanElementStateManager.DEV_ONLY_forEmptyInput()
        manager.handleChangeEvent(whitelistedPAN)
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
    }

    @Test
    fun `NonProdValidEbtCard - correctly flags valid`() {
        val whitelistedPAN: String = "9999420420420420420"
        val manager = PanElementStateManager.DEV_ONLY_forEmptyInput()
        manager.handleChangeEvent(whitelistedPAN)
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
    }

    @Test
    fun `EmptyEbtCashBalanceCard - correctly flags valid`() {
        val whitelistedPAN: String = "6543210000000000000"
        val manager = PanElementStateManager.DEV_ONLY_forEmptyInput()
        manager.handleChangeEvent(whitelistedPAN)
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
    }

    @Test
    fun `empty string is valid`() {
        val manager = PanElementStateManager.DEV_ONLY_forEmptyInput()
        manager.handleChangeEvent("")
        val state = manager.getState()

        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `non-whitelisted invalid Ebt Pan should be invalid`() {
        val manager = PanElementStateManager.DEV_ONLY_forEmptyInput()
        manager.handleChangeEvent("4204204204204204204")
        val state = manager.getState()

        assertThat(state.isValid).isFalse
        assertThat(state.isComplete).isFalse
        assertThat(state.validationError).isEqualTo(InvalidEbtPanError)
    }
}

class TestWhitelistedCards {
    @Test
    fun `prefix can be repeated arbitrary times`() {
        val whitelistValidator = WhitelistedCards("hello", 2)
        val validStr = "hellohello"
        assertThat(whitelistValidator.checkIfValid(validStr)).isTrue
    }

    @Test
    fun `complete requires length 19 card number`() {
        val whitelistValidator = WhitelistedCards("hello", 2)

        val incompleteStr = "hellohello"
        assertThat(whitelistValidator.checkIfValid(incompleteStr)).isTrue
        assertThat(whitelistValidator.checkIfComplete(incompleteStr)).isFalse

        val completeStr = "hellohello*********"
        assertThat(whitelistValidator.checkIfComplete(completeStr)).isTrue
    }

    @Test
    fun `complete and valid require matching prefix`() {
        val whitelistValidator = WhitelistedCards("hello", 2)

        val invalidCompleteStr = "hello12345*********"
        assertThat(whitelistValidator.checkIfValid(invalidCompleteStr)).isFalse
        assertThat(whitelistValidator.checkIfComplete(invalidCompleteStr)).isFalse

        val completeStr = "hellohello*********"
        assertThat(whitelistValidator.checkIfValid(completeStr)).isTrue
        assertThat(whitelistValidator.checkIfComplete(completeStr)).isTrue
    }

    @Test
    fun `validationError is always null`() {
        val whitelistValidator = WhitelistedCards("hello", 2)

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
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("")
        val state = manager.getState()
        assertThat(state.isEmpty).isTrue
    }

    @Test
    fun `cardNumber is not empty`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("1")
        val state = manager.getState()
        assertThat(state.isEmpty).isFalse
    }
}

class PanHandleChangeEventTest {
    @Test
    fun `valid card 16-digit card number passes correct state to callback`() {
        val manager = PanElementStateManager.forEmptyInput()
        var state: ElementState = manager.getState()
        val callback: StatefulElementListener = { newState -> state = newState }
        manager.setOnChangeEventListener(callback)
        manager.handleChangeEvent("5076807890123456")

        // TODO: its not clear that we should make any assertions
        //  about the resulting state of isFocus or isBlur. Should
        //  handle events update the focus / blurred state??

        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `setting multiple callbacks only invokes most recently set callback`() {
        val manager = PanElementStateManager.forEmptyInput()
        var callbackAInvoked = false
        var callbackBInvoked = false
        val callbackA: StatefulElementListener = { callbackAInvoked = true }
        val callbackB: StatefulElementListener = { callbackBInvoked = true }

        manager.setOnChangeEventListener(callbackA)
        manager.setOnChangeEventListener(callbackB)
        manager.handleChangeEvent("1234567890123456")

        // only the current callback should be invoked
        assertThat(callbackAInvoked).isFalse
        assertThat(callbackBInvoked).isTrue
    }

    @Test
    fun `strips all non-digit characters before processing`() {
        val manager = PanElementStateManager.forEmptyInput()
        var state: ElementState = manager.getState()
        val callback: StatefulElementListener = { newState -> state = newState }

        val validStringContaminatedByOtherChars = "!@# $%^ &*()_+<>? abcd5076807890123456"
        manager.setOnChangeEventListener(callback)
        manager.handleChangeEvent(validStringContaminatedByOtherChars)

        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }
}
