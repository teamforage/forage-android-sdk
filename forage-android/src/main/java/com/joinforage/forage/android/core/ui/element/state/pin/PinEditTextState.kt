package com.joinforage.forage.android.core.ui.element.state.pin

import com.joinforage.forage.android.core.ui.element.ElementValidationError
import com.joinforage.forage.android.core.ui.element.state.EditTextState
import com.joinforage.forage.android.core.ui.element.state.FocusState

/**
 * An interface that represents the state of a
 * [ForagePINEditText][com.joinforage.forage.android.core.ui.ForagePINEditText] Element.
 * @see [EditTextState][com.joinforage.forage.android.core.ui.element.state.EditTextState]
 */
interface PinEditTextState : EditTextState {
    companion object {
        internal fun from(focusState: FocusState, inputState: PinInputState) = object :
            PinEditTextState {
            override val isFocused: Boolean = focusState.isFocused
            override val isBlurred: Boolean = focusState.isBlurred
            override val isEmpty: Boolean = inputState.isEmpty
            override val isValid: Boolean = inputState.isValid
            override val isComplete: Boolean = inputState.isComplete
            override val validationError: ElementValidationError? = inputState.validationError
        }

        internal fun forEmptyInput() = from(
            FocusState.forEmptyInput(),
            PinInputState.forEmptyInput()
        )
    }
}
