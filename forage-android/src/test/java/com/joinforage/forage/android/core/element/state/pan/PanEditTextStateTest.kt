package com.joinforage.forage.android.core.element.state.pan

import com.joinforage.forage.android.core.ui.element.state.pan.PanEditTextState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PanEditTextStateTest {

    @Test
    fun testInitialPanElementState() {
        assertThat(PanEditTextState.forEmptyInput().isFocused).isFalse
        assertThat(PanEditTextState.forEmptyInput().isBlurred).isTrue
        assertThat(PanEditTextState.forEmptyInput().isEmpty).isTrue
        assertThat(PanEditTextState.forEmptyInput().isValid).isTrue
        assertThat(PanEditTextState.forEmptyInput().isComplete).isFalse
        assertThat(PanEditTextState.forEmptyInput().validationError).isNull()
        assertThat(PanEditTextState.forEmptyInput().derivedCardInfo.usState).isNull()
    }
}
