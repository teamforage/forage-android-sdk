package com.joinforage.forage.android.pos.ui.element.state.pin

import com.joinforage.forage.android.core.ui.element.ElementValidationError
import com.joinforage.forage.android.core.ui.element.state.EditTextState
import com.joinforage.forage.android.core.ui.element.state.ElementState
import com.joinforage.forage.android.core.ui.element.state.pin.PinInputState
import com.joinforage.forage.android.pos.ui.element.PinText

/**
 * An interface that represents the state of a
 * **ForagePINEditText** Element.
 * @see [EditTextState][com.joinforage.forage.android.core.ui.element.state.EditTextState]
 */
interface PinPadState : ElementState {
    val length: Int

    companion object {
        internal fun from(pinTextManager: PinText, inputState: PinInputState) = object :
            PinPadState {
            override val length: Int = pinTextManager.rawText.length
            override val isEmpty: Boolean = inputState.isEmpty
            override val isValid: Boolean = inputState.isValid
            override val isComplete: Boolean = inputState.isComplete
            override val validationError: ElementValidationError? = inputState.validationError
        }

        internal fun forEmptyInput() = from(
            PinText.forEmptyInput(),
            PinInputState.forEmptyInput()
        )
    }
}
