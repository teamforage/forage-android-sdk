package com.joinforage.forage.android.core.ui.element.state.pin

import com.joinforage.forage.android.core.ui.element.ElementValidationError
import com.joinforage.forage.android.core.ui.element.IncompleteEbtPinError
import com.joinforage.forage.android.core.ui.element.WrongEbtPinError

internal data class PinInputState(
    val isEmpty: Boolean,
    val isValid: Boolean,
    val isComplete: Boolean,
    val validationError: ElementValidationError?
) {
    // this function is used for after the submit event
    // happens and we learn that the PIN was not correct
    fun markPinAsWrong() = PinInputState(
        isEmpty = isEmpty,
        isValid = false,
        isComplete = isComplete,
        validationError = WrongEbtPinError
    )

    // this function is used within the on-change event
    // callback from our vault providers
    fun setPinIsComplete(isComplete: Boolean) = PinInputState(
        isEmpty = isEmpty,
        // for PINs, isValid and isComplete are the same thing
        // they differ for PANs
        isValid = isComplete,
        isComplete = isComplete,
        validationError = if (!isComplete) IncompleteEbtPinError else null
    )

    fun setIsEmpty(isEmpty: Boolean) = PinInputState(
        isEmpty = isEmpty,
        isValid = isValid,
        isComplete = isComplete,
        validationError = validationError
    )

    fun handleChangeEvent(isComplete: Boolean, isEmpty: Boolean): PinInputState =
        this.setPinIsComplete(isComplete).setIsEmpty(isEmpty)

    companion object {
        fun forEmptyInput() = PinInputState(
            isEmpty = true,
            isValid = false,
            isComplete = false,
            validationError = null
        )
    }
}
