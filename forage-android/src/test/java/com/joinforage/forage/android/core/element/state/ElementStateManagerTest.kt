package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.SimpleElementListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ElementStateManagerTest {

    @Test
    fun testGetState() {
        val manager = PanElementStateManager.forEmptyInput()
        val state = manager.getState()
        assertThat(state.isFocused).isEqualTo(INITIAL_ELEMENT_STATE.isFocused)
        assertThat(state.isBlurred).isEqualTo(INITIAL_ELEMENT_STATE.isBlurred)
        assertThat(state.isEmpty).isEqualTo(INITIAL_ELEMENT_STATE.isEmpty)
        assertThat(state.isValid).isEqualTo(INITIAL_ELEMENT_STATE.isValid)
        assertThat(state.isComplete).isEqualTo(INITIAL_ELEMENT_STATE.isComplete)
        assertThat(state.validationError).isEqualTo(INITIAL_ELEMENT_STATE.validationError)
    }

    @Test
    fun testSetOnFocusEventListener() {
        val manager = PanElementStateManager.forEmptyInput()
        var callbackAInvoked = false
        var callbackBInvoked = false

        val callbackA: SimpleElementListener = { callbackAInvoked = true }
        val callbackB: SimpleElementListener = { callbackBInvoked = true }

        manager.setOnFocusEventListener(callbackA)
        manager.setOnFocusEventListener(callbackB)

        manager.focus()

        // only the current callback should be invoked
        assertThat(callbackAInvoked).isFalse
        assertThat(callbackBInvoked).isTrue
    }

    @Test
    fun testSetOnBlurEventListener() {
        val manager = PinElementStateManager.forEmptyInput()
        var callbackAInvoked = false
        var callbackBInvoked = false

        val callbackA: SimpleElementListener = { callbackAInvoked = true }
        val callbackB: SimpleElementListener = { callbackBInvoked = true }

        manager.setOnBlurEventListener(callbackA)
        manager.setOnBlurEventListener(callbackB)

        manager.blur()

        // only the current callback should be invoked
        assertThat(callbackAInvoked).isFalse
        assertThat(callbackBInvoked).isTrue
    }

    @Test
    fun testChangeFocus() {
        val manager = PinElementStateManager.forEmptyInput()
        var state = manager.getState()

        // focus

        // only focus and blur should change
        manager.changeFocus(true)
        state = manager.getState()
        assertThat(state.isFocused).isTrue
        assertThat(state.isBlurred).isFalse
        assertThat(state.isEmpty).isEqualTo(INITIAL_ELEMENT_STATE.isEmpty)
        assertThat(state.isValid).isEqualTo(INITIAL_ELEMENT_STATE.isValid)
        assertThat(state.isComplete).isEqualTo(INITIAL_ELEMENT_STATE.isComplete)

        // unfocus
        manager.changeFocus(false)
        state = manager.getState()
        assertThat(state.isFocused).isFalse
        assertThat(state.isBlurred).isTrue
        assertThat(state.isEmpty).isEqualTo(INITIAL_ELEMENT_STATE.isEmpty)
        assertThat(state.isValid).isEqualTo(INITIAL_ELEMENT_STATE.isValid)
        assertThat(state.isComplete).isEqualTo(INITIAL_ELEMENT_STATE.isComplete)
    }
}
