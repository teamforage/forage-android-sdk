package com.joinforage.forage.android.core.element.state.pin

import com.joinforage.forage.android.core.ui.element.IncompleteEbtPinError
import com.joinforage.forage.android.core.ui.element.WrongEbtPinError
import com.joinforage.forage.android.core.ui.element.state.pin.PinInputState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MarkPinAsWrongTest {
    @Test
    fun `test input marked as invalid and validation error`() {
        val state = PinInputState.forEmptyInput().markPinAsWrong()
        assertThat(state.isValid).isFalse
        assertThat(state.validationError).isEqualTo(WrongEbtPinError)
    }
}

class SetPinIsCompleteTest {
    @Test
    fun `everything is valid when pin is complete`() {
        val state = PinInputState.forEmptyInput()
            // scramble the state so we know our method does something
            .markPinAsWrong()
            // perform the action we care about
            .handleChangeEvent(isComplete = true, false)

        // assert state is updated as we expect
        assertThat(state.isComplete).isTrue
        assertThat(state.isValid).isTrue
        assertThat(state.validationError).isNull()
    }

    @Test
    fun `everything is invalid when pin is complete`() {
        val state = PinInputState.forEmptyInput()
            // scramble the state so we know our method does something
            .markPinAsWrong()
            // perform the action we care about
            .handleChangeEvent(isComplete = false, false)

        // assert state is updated as we expect
        assertThat(state.isComplete).isFalse
        assertThat(state.isValid).isFalse
        assertThat(state.validationError).isEqualTo(IncompleteEbtPinError)
    }
}

class PinSetIsEmptyTest {
    @Test
    fun `sets the value correctly`() {
        val state = PinInputState.forEmptyInput()
            .handleChangeEvent(true, isEmpty = true)
        assertThat(state.isEmpty).isTrue
    }
}

class PinHandleChangeEventTest {
    @Test
    fun `completed pin passes correct state to callback`() {
        val state = PinInputState
            .forEmptyInput()
            .handleChangeEvent(isComplete = true, isEmpty = false)

        // TODO: its not clear that we should make any assertions
        //  about the resulting state of isFocus or isBlur. Should
        //  handle events update the focus / blurred state??

        assertThat(state.isEmpty).isFalse
        assertThat(state.isValid).isTrue
        assertThat(state.isComplete).isTrue
        assertThat(state.validationError).isNull()
    }
}
