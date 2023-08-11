package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.element.InvalidEbtPanError
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.TooLongEbtPanError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PanSetIsValidTest {

    @Test
    fun `cardNumber as empty string`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("")
        val state = manager.getState()
        assertThat(state.isValid).isTrue
    }

    @Test
    fun `cardNumber too short to contain  IIN`() {
        val tooShortNoIIN: String = "420"
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(tooShortNoIIN)
        val state = manager.getState()
        assertThat(state.isValid).isFalse
    }

    @Test
    fun `cardNumber has non-existent IIN`() {
        val invalidIIN: String = "420420420"
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(invalidIIN)
        val state = manager.getState()
        assertThat(state.isValid).isFalse
    }

    @Test
    fun `cardNumber has valid state IIN but is shorter than expected length`() {
        val tooShortMaineNumber: String = "507703111" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(tooShortMaineNumber)
        val state = manager.getState()
        assertThat(state.isValid).isTrue
    }

    @Test
    fun `cardNumber has valid state IIN and is correct length`() {
        val okMaineNumber: String = "5077031111111111111" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(okMaineNumber)
        val state = manager.getState()
        assertThat(state.isValid).isTrue
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
    }
}

class PanSetIsCompleteTest {
    @Test
    fun `cardNumber as empty string`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("")
        val state = manager.getState()
        assertThat(state.isComplete).isFalse
    }

    @Test
    fun `cardNumber too short to contain  IIN`() {
        val tooShortNoIIN: String = "420"
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(tooShortNoIIN)
        val state = manager.getState()
        assertThat(state.isComplete).isFalse
    }

    @Test
    fun `cardNumber has non-existent IIN`() {
        val invalidIIN: String = "420420420"
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(invalidIIN)
        val state = manager.getState()
        assertThat(state.isComplete).isFalse
    }

    @Test
    fun `cardNumber has valid state IIN but is shorter than expected length`() {
        val tooShortMaineNumber: String = "507703111" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(tooShortMaineNumber)
        val state = manager.getState()
        assertThat(state.isComplete).isFalse
    }

    @Test
    fun `cardNumber has valid state IIN and is correct length`() {
        val okMaineNumber: String = "5077031111111111111" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(okMaineNumber)
        val state = manager.getState()
        assertThat(state.isComplete).isTrue
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
        assertThat(state.isComplete).isFalse
    }
}

class PanSetValidationErrorTest {
    @Test
    fun `cardNumber as empty string`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("")
        val state = manager.getState()
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `cardNumber too short to contain  IIN`() {
        val tooShortNoIIN: String = "420"
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(tooShortNoIIN)
        val state = manager.getState()
        assertThat(state.validationError).isEqualTo(IncompleteEbtPanError)
    }

    @Test
    fun `cardNumber has non-existent IIN`() {
        val invalidIIN: String = "420420420"
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(invalidIIN)
        val state = manager.getState()
        assertThat(state.validationError).isEqualTo(InvalidEbtPanError)
    }

    @Test
    fun `cardNumber has valid state IIN but is shorter than expected length`() {
        val tooShortMaineNumber: String = "507703111" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(tooShortMaineNumber)
        val state = manager.getState()
        assertThat(state.validationError).isEqualTo(IncompleteEbtPanError)
    }

    @Test
    fun `cardNumber has valid state IIN and is correct length`() {
        val okMaineNumber: String = "5077031111111111111" // Maine is 507703
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent(okMaineNumber)
        val state = manager.getState()
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
        assertThat(state.validationError).isEqualTo(TooLongEbtPanError)
    }
}

class PanSetIsEmptyTest {
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
}

class CanTokenizePanElementValueTest {
    @Test
    fun `alwaysAllow = true trumps complete = false`() {
        val notCompleteElementState = ElementState(
            isFocused = false,
            isEmpty = false,
            isBlurred = false,
            isValid = false,
            validationError = null,
            isComplete = false
        )
        val manager = PanElementStateManager(notCompleteElementState)
        val canTokenize = manager.canTokenizePanElementValue(alwaysAllow = true)
        assertThat(canTokenize).isTrue
    }

    @Test
    fun `can tokenize when complete = true && alwaysAllow = false`() {
        val completeElementState = ElementState(
            isFocused = false,
            isEmpty = false,
            isBlurred = false,
            isValid = false,
            validationError = null,
            isComplete = true
        )
        val manager = PanElementStateManager(completeElementState)
        val canTokenize = manager.canTokenizePanElementValue(alwaysAllow = false)
        assertThat(canTokenize).isTrue
    }

    @Test
    fun `canNOT tokenize when complete = false && alwaysAllow = false`() {
        val notCompleteElementState = ElementState(
            isFocused = false,
            isEmpty = false,
            isBlurred = false,
            isValid = false,
            validationError = null,
            isComplete = false
        )
        val manager = PanElementStateManager(notCompleteElementState)
        val canTokenize = manager.canTokenizePanElementValue(alwaysAllow = false)
        assertThat(canTokenize).isFalse
    }
}
