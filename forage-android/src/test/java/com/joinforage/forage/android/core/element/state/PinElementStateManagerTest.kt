package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.IncompleteEbtPinError
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.WrongEbtPinError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MarkPinAsWrongTest {
    @Test
    fun `test input marked as invalid and validation error`() {
        val manager = PinElementStateManager.forEmptyInput()
        manager.markPinAsWrong()
        val state = manager.getState()
        assertThat(state.isValid).isFalse
        assertThat(state.validationError).isEqualTo(WrongEbtPinError)
    }
}

class SetPinIsCompleteTest {
    @Test
    fun `everything is valid when pin is complete`() {
        val manager = PinElementStateManager.forEmptyInput()
        // scramble the state so we know our method does something
        manager.markPinAsWrong()

        // perform the action we care about
        manager.handleChangeEvent(isComplete = true, false)

        // assert state is updated as we expect
        val state = manager.getState()
        assertThat(state.isComplete).isTrue
        assertThat(state.isValid).isTrue
        assertThat(state.validationError).isNull()
        assertThat(state.details as Any?).isNull()
    }

    @Test
    fun `everything is invalid when pin is complete`() {
        val manager = PinElementStateManager.forEmptyInput()
        // scramble the state so we know our method does something
        manager.markPinAsWrong()

        // perform the action we care about
        manager.handleChangeEvent(isComplete = false, false)

        // assert state is updated as we expect
        val state = manager.getState()
        assertThat(state.isComplete).isFalse
        assertThat(state.isValid).isFalse
        assertThat(state.validationError).isEqualTo(IncompleteEbtPinError)

        // cast to Any? to avoid ambiguous overload issue that
        // is unique to PinDetails because it's an alias for `Nothing?`
        assertThat(state.details as Any?).isNull()
    }
}

class PinSetIsEmptyTest {
    @Test
    fun `sets the value correctly`() {
        val manager = PinElementStateManager.forEmptyInput()
        manager.handleChangeEvent(true, isEmpty = true)
        val state = manager.getState()
        assertThat(state.isEmpty).isTrue
    }
}

class PinHandleChangeEventTest {
    @Test
    fun `completed pin passes correct state to callback`() {
        val manager = PinElementStateManager.forEmptyInput()
        var state: ElementState<PinDetails> = manager.getState()
        val callback: StatefulElementListener<PinDetails> = { newState -> state = newState }
        manager.setOnChangeEventListener(callback)
        manager.handleChangeEvent(isComplete = true, isEmpty = false)

        // TODO: its not clear that we should make any assertions
        //  about the resulting state of isFocus or isBlur. Should
        //  handle events update the focus / blurred state??

        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()

        // cast to Any? to avoid ambiguous overload issue that
        // is unique to PinDetails because it's an alias for `Nothing?`
        assertThat(state.details as Any?).isNull()
    }

    @Test
    fun `setting multiple callbacks only invokes most recently set callback`() {
        val manager = PinElementStateManager.forEmptyInput()
        var callbackAInvoked = false
        var callbackBInvoked = false
        val callbackA: StatefulElementListener<PinDetails> = { callbackAInvoked = true }
        val callbackB: StatefulElementListener<PinDetails> = { callbackBInvoked = true }

        manager.setOnChangeEventListener(callbackA)
        manager.setOnChangeEventListener(callbackB)
        manager.handleChangeEvent(isComplete = true, isEmpty = false)

        // only the current callback should be invoked
        assertThat(callbackAInvoked).isFalse
        assertThat(callbackBInvoked).isTrue
    }
}
