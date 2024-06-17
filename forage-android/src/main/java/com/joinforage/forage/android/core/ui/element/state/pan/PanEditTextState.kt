package com.joinforage.forage.android.core.ui.element.state.pan

import com.joinforage.forage.android.core.ui.element.state.EditTextState
import com.joinforage.forage.android.core.ui.element.state.FocusState

/**
 * An interface that represents information that Forage gets from the card.
 * This includes the [USState] that issued the card.
 */
interface DerivedCardInfo {
    val usState: USState?
}

/**
 * An interface that represents the state of a
 * [ForagePANEditText][com.joinforage.forage.android.ui.ForagePANEditText] Element.
 * @property derivedCardInfo The [DerivedCardInfo] for the card number submitted to the Element.
 */
interface PanEditTextState : EditTextState {
    val derivedCardInfo: DerivedCardInfo // the interface not the DTO

    companion object {
        internal fun from(focusState: FocusState, inputState: PanInputState) = object :
            PanEditTextState {
            override val isFocused = focusState.isFocused
            override val isBlurred = focusState.isBlurred
            override val isEmpty = inputState.isEmpty
            override val isValid = inputState.isValid
            override val isComplete = inputState.isComplete
            override val validationError = inputState.validationError
            override val derivedCardInfo = object : DerivedCardInfo {
                override val usState: USState? = inputState.usState
            }
        }

        internal fun forEmptyInput() = from(
            FocusState.forEmptyInput(),
            PanInputState.NON_PROD_forEmptyInput()
        )
    }
}
