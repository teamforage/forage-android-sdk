package com.joinforage.forage.android.core.element.state.pin

import com.joinforage.forage.android.core.ui.element.state.pin.PinEditTextState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PinEditTextStateTest {

    @Test
    fun testInitialPinElementState() {
        assertThat(PinEditTextState.forEmptyInput().isFocused).isFalse
        assertThat(PinEditTextState.forEmptyInput().isBlurred).isTrue
        assertThat(PinEditTextState.forEmptyInput().isEmpty).isTrue
        assertThat(PinEditTextState.forEmptyInput().isValid).isFalse
        assertThat(PinEditTextState.forEmptyInput().isComplete).isFalse
        assertThat(PinEditTextState.forEmptyInput().validationError).isNull()
    }
}
