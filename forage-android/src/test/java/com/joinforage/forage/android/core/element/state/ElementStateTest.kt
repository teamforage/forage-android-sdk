package com.joinforage.forage.android.core.element.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ElementStateTest {

    @Test
    fun testInitialPinElementState() {
        assertThat(INITIAL_PIN_ELEMENT_STATE.isFocused).isFalse
        assertThat(INITIAL_PIN_ELEMENT_STATE.isBlurred).isTrue
        assertThat(INITIAL_PIN_ELEMENT_STATE.isEmpty).isTrue
        assertThat(INITIAL_PIN_ELEMENT_STATE.isValid).isFalse
        assertThat(INITIAL_PIN_ELEMENT_STATE.isComplete).isFalse
        assertThat(INITIAL_PIN_ELEMENT_STATE.validationError).isNull()

        // cast to Any? to avoid ambiguous overload issue that
        // is unique to PinDetails because it's an alias for `Nothing?`
        assertThat(INITIAL_PIN_ELEMENT_STATE.details as Any?).isNull()
    }

    @Test
    fun testInitialPanElementState() {
        assertThat(INITIAL_PAN_ELEMENT_STATE.isFocused).isFalse
        assertThat(INITIAL_PAN_ELEMENT_STATE.isBlurred).isTrue
        assertThat(INITIAL_PAN_ELEMENT_STATE.isEmpty).isTrue
        assertThat(INITIAL_PAN_ELEMENT_STATE.isValid).isTrue
        assertThat(INITIAL_PAN_ELEMENT_STATE.isComplete).isFalse
        assertThat(INITIAL_PAN_ELEMENT_STATE.validationError).isNull()
        assertThat(INITIAL_PAN_ELEMENT_STATE.details).isEqualTo(PanDetails(DerivedCardInfo()))
    }
}
