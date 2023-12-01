package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.IncompleteEbtPinError
import com.joinforage.forage.android.core.element.WrongEbtPinError

internal class PinElementStateManager(state: ElementState<PinDetails>) : ElementStateManager<PinDetails>(state) {

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

    private fun setDetails() {
        // right now there is no extra info we want to supply along
        // side the existing element state so we set details = null
        this.details = null
    }

    fun handleChangeEvent(isComplete: Boolean, isEmpty: Boolean) {
        setPinIsComplete(isComplete)
        setIsEmpty(isEmpty)
        setDetails()
        onChangeEventListener?.invoke(getState())
    }

    companion object {
        fun forEmptyInput(): PinElementStateManager {
            return PinElementStateManager(INITIAL_PIN_ELEMENT_STATE)
        }
    }
}
