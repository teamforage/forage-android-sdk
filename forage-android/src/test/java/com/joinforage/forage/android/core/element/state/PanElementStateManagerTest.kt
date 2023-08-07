package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.element.StatefulElementListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PanSetIsValidTest {

    @Test
    fun `cardNumber as empty string`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("")
        val state = manager.getState()
        assertThat(state.isValid).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `cardNumber shorter than min 16 length`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("12345")
        val state = manager.getState()
        assertThat(state.isValid).isFalse
        assertThat(state.validationError).isEqualTo(IncompleteEbtPanError)
    }

    @Test
    fun `cardNumber is min length 16`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("1234567890123456")
        val state = manager.getState()
        assertThat(state.isValid).isTrue
        assertThat(state.validationError).isNull()
    }
}

class PanSetIsCompleteTest {
    @Test
    fun `cardNumber is less than min length 16`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("123456789012345")
        val state = manager.getState()
        assertThat(state.isComplete).isFalse
    }

    @Test
    fun `cardNumber is min length 16`() {
        val manager = PanElementStateManager.forEmptyInput()
        manager.handleChangeEvent("1234567890123456")
        val state = manager.getState()
        assertThat(state.isComplete).isTrue
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
        manager.handleChangeEvent("1234567890123456")

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