package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.ui.element.state.DerivedCardInfoDto
import com.joinforage.forage.android.core.ui.element.state.INITIAL_PAN_ELEMENT_STATE
import com.joinforage.forage.android.core.ui.element.state.INITIAL_PIN_ELEMENT_STATE
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
    }

    @Test
    fun testInitialPanElementState() {
        assertThat(INITIAL_PAN_ELEMENT_STATE.isFocused).isFalse
        assertThat(INITIAL_PAN_ELEMENT_STATE.isBlurred).isTrue
        assertThat(INITIAL_PAN_ELEMENT_STATE.isEmpty).isTrue
        assertThat(INITIAL_PAN_ELEMENT_STATE.isValid).isTrue
        assertThat(INITIAL_PAN_ELEMENT_STATE.isComplete).isFalse
        assertThat(INITIAL_PAN_ELEMENT_STATE.validationError).isNull()
        assertThat(INITIAL_PAN_ELEMENT_STATE.derivedCardInfo).isEqualTo(DerivedCardInfoDto())
    }
}
