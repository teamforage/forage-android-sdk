package com.joinforage.forage.android.core.ui.element.state

import com.joinforage.forage.android.core.ui.element.IncompleteEbtPinError
import com.joinforage.forage.android.core.ui.element.WrongEbtPinError

internal abstract class PinElementStateManager<T : PinElementState>(state: T) : ElementStateManager<T>(state) {
    // this function is used for after the submit event
    // happens and we learn that the PIN was not correct
    fun markPinAsWrong() {
        isValid = false
        validationError = WrongEbtPinError
    }

    // this function is used within the on-change event
    // callback from our vault providers
    private fun setPinIsComplete(isComplete: Boolean) {
        // for PINs, isValid and isComplete are the same thing
        // they differ for PANs
        isValid = isComplete
        this.isComplete = isComplete
        validationError = if (!isValid) IncompleteEbtPinError else null
    }

    private fun setIsEmpty(isEmpty: Boolean) {
        this.isEmpty = isEmpty
    }

    protected fun handleChangeEvent(isComplete: Boolean, isEmpty: Boolean) {
        setPinIsComplete(isComplete)
        setIsEmpty(isEmpty)
        onChangeEventListener?.invoke(getState())
    }
}
